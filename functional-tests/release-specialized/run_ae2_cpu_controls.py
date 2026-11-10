"""Separate native CPU control qualification; original complete-suite QA is intact."""
from pathlib import Path
import argparse,datetime,hashlib,importlib.util,json,msvcrt,os,re,shutil,subprocess,sys,traceback,zipfile

ROOT=Path(__file__).resolve().parents[2]
sys.path[:0]=[str(ROOT/'tools'),str(ROOT/'functional-tests/project-everything-regression')]
from ae2_cpu_controls_source import BASE,TARGETS,enhance,preserves_base,validate_controls
from subject_artifacts import current_subjects
from focused_release_evidence import reference
import full_suite_provenance as provenance


def sha(path):return hashlib.sha256(Path(path).read_bytes()).hexdigest()
def read(path):return json.loads(Path(path).read_text())


def prepare(folder,mc):
    generated=folder/'generated-shared';generated.mkdir()
    original=ROOT/'functional-tests/ae2/shared/src/main/java'
    source_hashes={}
    for source in sorted(original.rglob('*.java')):
        relative=source.relative_to(original);target=generated/relative;target.parent.mkdir(parents=True,exist_ok=True)
        if source==ROOT/BASE:
            base=source.read_text();value=enhance(base,mc);preserves_base(base,value,mc);target.write_text(value)
        else:shutil.copyfile(source,target)
        source_hashes[source.relative_to(ROOT).as_posix()]=sha(source)
    init=folder/'cpu-controls.init.gradle'
    init.write_text('''// Separate derived observation source set; never modifies shipping or original QA sources.
gradle.projectsEvaluated {
    def root=gradle.rootProject
    def mc=root.providers.gradleProperty('qaMinecraft').get()
    def loader=root.providers.gradleProperty('qaLoader').get()
    def target=root.project(":${mc}:${loader}")
    def qa=target.sourceSets.getByName('chestCloseQa')
    def original=root.file('functional-tests/ae2/shared/src/main/java').canonicalFile
    def sources=qa.java.srcDirs.findAll { it.canonicalFile!=original }
    qa.java.setSrcDirs(sources+[root.file('''+json.dumps(generated.as_posix())+''')])
    target.tasks.named('runFullGameplayQa').configure {
        // Retain native staging as the first action; inspect the finalized
        // classpath immediately afterward and before JavaExec starts.
        def nativeSetup=actions.remove(0)
        doFirst {
            def prefix=root.rootDir.canonicalPath+File.separator
            println('CPU_CONTROLS_QA_PROJECT_CLASSPATH='+classpath.files.findAll {
                it.isDirectory() && it.canonicalPath.startsWith(prefix) &&
                    !it.canonicalPath.contains(File.separator+'.gradle'+File.separator)
            })
        }
        doFirst(nativeSetup)
    }
}
''')
    base=ROOT/'functional-tests/scripts/Run-Ae2ReleaseQa.ps1';value=base.read_text()
    old="$repo=(Resolve-Path (Join-Path $PSScriptRoot '../..')).Path"
    assert value.count(old)==1;value=value.replace(old,"$repo='"+str(ROOT).replace("'","''")+"'",1)
    old="'-I','functional-tests/ae2-release-qa.init.gradle',"
    assert value.count(old)==1;value=value.replace(old,old+"\n    '-I','"+init.relative_to(ROOT).as_posix().replace("'","''")+"',",1)
    script=folder/'Run-CpuControls.ps1';script.write_text(value)
    return dict(script=script,inputs=[*sorted(generated.rglob('*.java')),init,script],original_shared=source_hashes)


def capture_subject(runtime,folder,name,expected,loader):
    paths=[p for p in [runtime/'mods'/name,runtime/'qa-launch-mods'/name]if p.is_file()]
    assert len(paths)==1,'Missing or duplicate actual production subject'
    actual=paths[0];digest=sha(actual)
    if digest==expected:return dict(file=provenance.relative(ROOT,actual),sha256=digest,subject_sha256=expected,mode='exact-packaged-copy')
    assert loader=='forge','Unexpected transformed production subject'
    lineage=folder/'forge-remap-lineage.json';data=read(lineage)
    assert data['schema_version']==1 and data['operation']=='SRG_TO_MCP'
    original=(runtime/'original-mods'/name).resolve();assert sha(original)==expected
    rows=[v for v in data['artifacts']if Path(v['original']).resolve()==original and
        Path(v['mapped']).resolve()==(runtime/'mods'/name).resolve()]
    assert len(rows)==1 and rows[0]['original_sha256']==expected and rows[0]['mapped_sha256']==digest
    for key in ['installer_tools','mappings']:assert sha(data[key])==data[key+'_sha256']
    spec=importlib.util.spec_from_file_location('cpu_controls_forge_resource_contract',ROOT/'tools/remap-controlify-forge-qa.py')
    module=importlib.util.module_from_spec(spec);spec.loader.exec_module(module);module.verify_resources(original,actual)
    return dict(file=provenance.relative(ROOT,actual),sha256=digest,subject_sha256=expected,
        mode='declared-Forge-SRG_TO_MCP-with-recorded-optional-QA-group-relocation',
        original=reference(ROOT,original),lineage=reference(ROOT,lineage),
        mapped_before_optional_group=str((runtime/'mods'/name).resolve()),
        transformation=dict(operation=data['operation'],**{k:data[k]for k in ['installer_tools','installer_tools_sha256','mappings','mappings_sha256']}))


def capture_fml_group(folder,runtime,subjects,copies,loaded):
    from focused_release_evidence_ae2_drive_gutter import atlas_group
    group=atlas_group(ROOT,folder,runtime,copies,loaded)
    core=[v for v in copies if Path(v['file']).name==subjects['core'][0]]
    assert len(core)==1
    assert loaded['coreCodeSource']['file']==core[0]['file']and loaded['coreCodeSource']['sha256']==core[0]['sha256']
    return group


def capture_runtime(folder,target,subjects):
    report=read(folder/'results.json');mc,loader=target.split('/')
    assert (report['minecraft'],report['loader'],report['scenario'])==(mc,loader,'ae2-atlas')
    assert type(report['failures'])is int and report['failures']==0 and report['results']
    assert any(v['status']=='PASS'for v in report['results'])and not any(v['status']in {'FAIL','FAILED','ERROR'}for v in report['results'])
    runtime=Path(report['runDirectory']).resolve();assert runtime.is_relative_to(ROOT)
    copies=[capture_subject(runtime,folder,name,digest,loader)for name,digest in subjects.values()]
    loaded={key:provenance.loaded_code_source(ROOT,runtime,report[key])for key in ['coreCodeSource','compatibilityCodeSource']}
    fabric_lineage=None;fml_group=None
    if loader=='fabric':
        from ae2_cpu_fabric_lineage import derive
        fabric_lineage=derive(ROOT,folder,subjects,copies,loaded)
    else:fml_group=capture_fml_group(folder,runtime,subjects,copies,loaded)
    dependencies=provenance.ordinary_dependencies(ROOT,folder,runtime,dict(minecraft=mc,loader=loader),'ae2-atlas')
    binaries={};logs=(folder/'launch.log').read_text(errors='replace')
    if loader=='fabric':
        rows=re.findall(r'^CPU_CONTROLS_QA_PROJECT_CLASSPATH=\[(.*)\]\s*$',logs,re.MULTILINE);assert len(rows)==1
        paths=[Path(v.strip()).resolve()for v in rows[0].split(',')]
        assert len(paths)>=2 and all(p.is_relative_to(ROOT)and p.is_dir()for p in paths)
        for path in paths:
            for parent,dirs,files in os.walk(path):
                for name in files:
                    p=Path(parent)/name;binaries[provenance.relative(ROOT,p)]=sha(p)
        lifecycle=[ROOT/p for p in binaries if p.endswith('/com/salts_inventory_update/compat/ae2/qa/NativeCpuLifecycleQa.class')]
        assert len(lifecycle)==1 and b'verifyAdditionalControls'in lifecycle[0].read_bytes()
    else:
        group=read(folder/'qa-runtime-group.json');archives=group['archives']
        assert len(archives)==2
        for value in archives:assert sha(value['file'])==value['sha256']
        observed=[]
        for value in archives:
            with zipfile.ZipFile(value['file'])as jar:
                member='com/salts_inventory_update/compat/ae2/qa/NativeCpuLifecycleQa.class'
                if member in jar.namelist():
                    assert b'verifyAdditionalControls'in jar.read(member);observed.append(value)
        assert len(observed)==1
        binary=Path(observed[0]['file']);binaries[provenance.relative(ROOT,binary)]=sha(binary)
        assert any(v['file']==provenance.relative(ROOT,Path(a['file']))for v in copies for a in archives)
    return dict(report=reference(ROOT,folder/'results.json'),runtime_copies=copies,loaded_code_sources=loaded,
        fabric_lineage=fabric_lineage,FML_group=fml_group,dependency_evidence=dependencies,qa_binaries=binaries,qa_binary_archive=provenance.bundle(ROOT,binaries,'qa-binaries'))


def verify_run(root,summary,expected):
    """Recheck immutable sources/binaries, exact subjects and native observations."""
    root=Path(root).resolve();assert root==ROOT
    def checked(ref):
        p=(root/ref['file']).resolve();assert p.is_relative_to(root)and sha(p)==ref['sha256'];return p
    assert summary['status']=='passed'and type(summary['exit_code'])is int and summary['exit_code']==0
    assert summary['subjects']=={k:list(v)for k,v in expected.items()}
    target=summary['target'];assert target in TARGETS;mc,loader=target.split('/')
    inputs=summary['qa_inputs'];provenance.verify_bundle(root,summary['qa_source_archive'],inputs)
    with zipfile.ZipFile(checked(summary['qa_source_archive']))as jar:
        original=jar.read(BASE).decode()
        candidates=[n for n in jar.namelist()if '/generated-shared/'in n and n.endswith('/NativeCpuLifecycleQa.java')]
        assert len(candidates)==1 and preserves_base(original,jar.read(candidates[0]).decode(),mc)
    controls=read(checked(summary['native_controls']));lifecycle=read(checked(summary['native_lifecycle']))
    checks=validate_controls(target,controls,lifecycle,checked(summary['original_inner_log']).read_text(errors='replace'))
    acceptance=read(checked(summary['acceptance']));assert acceptance['complete']is True and acceptance['checks']==checks
    assert acceptance['actual_observations']==[summary['native_controls'],summary['native_lifecycle']]
    assert len(summary['results'])==1;row=summary['results'][0]
    assert row['target']==target and row['status']=='passed'and type(row['exit_code'])is int and row['exit_code']==0
    assert row['results']['ae2_cpu_controls_checked']is True
    for key,wanted in [('cases',1),('failed',0),('checks',len(checks)),('native_job_cancellations',2)]:
        assert type(row['results'][key])is int and row['results'][key]==wanted
    runtime=summary['runtime_evidence'];report_path=checked(runtime['report']);report=read(report_path)
    assert report_path==root/row['profile']/'results.json'
    assert (report['minecraft'],report['loader'],report['scenario'])==(mc,loader,'ae2-atlas')and report['failures']==0
    assert report['results']and any(v['status']=='PASS'for v in report['results'])and not any(v['status']in {'FAIL','FAILED','ERROR'}for v in report['results'])
    actual=Path(report['runDirectory']).resolve();assert actual.is_relative_to(root)
    copies=[capture_subject(actual,report_path.parent,name,digest,loader)for name,digest in expected.values()]
    assert copies==runtime['runtime_copies']
    assert runtime['loaded_code_sources']=={key:provenance.loaded_code_source(root,actual,report[key])for key in ['coreCodeSource','compatibilityCodeSource']}
    if loader=='fabric':
        from ae2_cpu_fabric_lineage import derive
        assert runtime['fabric_lineage']==derive(root,report_path.parent,expected,copies,runtime['loaded_code_sources'])
    else:
        assert runtime.get('fabric_lineage')is None
        assert runtime['FML_group']==capture_fml_group(report_path.parent,actual,expected,copies,runtime['loaded_code_sources'])
    if 'evidence_repair'in summary:
        repair=summary['evidence_repair'];assert repair['schema']=='native-CPU-archival-followup-v1'and repair['no_new_native_execution']is True
        original=read(checked(repair['original_failed_summary']))
        assert original['status']=='failed'and original['exit_code']==0 and original['target']==target and original['subjects']==summary['subjects']
        assert original['qa_inputs']==summary['qa_inputs']and original['qa_source_archive']==summary['qa_source_archive']
        assert original['original_shared_source_sha256']==summary['original_shared_source_sha256']and original['command']==summary['command']
        original_run=original['command'][original['command'].index('-RunId')+1]
        assert report_path.parent.name==mc+'-'+loader+'-'+original_run and repair['actual_report']==runtime['report']
        provenance.verify_bundle(root,repair['verifier_source_archive'],repair['verifier_inputs'])
    provenance.verify_bundle(root,runtime['qa_binary_archive'],runtime['qa_binaries'])
    deps=runtime['dependency_evidence'];checked(deps['manifest'])
    for name,digest in deps['runtime_mod_archives'].items():assert sha(root/name)==digest
    for value in deps['upstream']:
        raw=checked(value).read_bytes();assert hashlib.sha512(raw).hexdigest()==value['sha512']
        if value.get('runtime_copy'):checked(value['runtime_copy'])
    checked(summary['driver_log'])
    return True


def main():
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('--target',choices=TARGETS,required=True)
    parser.add_argument('--run',required=True);parser.add_argument('--plan-only',action='store_true');args=parser.parse_args()
    assert args.run and all(c in 'abcdefghijklmnopqrstuvwxyz0123456789-'for c in args.run)
    mc,loader=args.target.split('/')
    if args.plan_only:
        print(json.dumps(dict(target=args.target,original_source=BASE,original_cycles=2,
            additional_modes=['ANY','PLAYER_ONLY','MACHINE_ONLY'],suspend_resume=mc!='1.20.1',
            scope='Additional native widget checks on real held jobs through a separate derived QA copy. No native execution or acceptance by this plan.'),indent=2));return
    with (ROOT/'build/project-everything-regression/runtime.lock').open('a+b')as lock:
        lock.seek(0);msvcrt.locking(lock.fileno(),msvcrt.LK_NBLCK,1)
        try:
            out=ROOT/'output/release-readiness'/args.run;assert not out.exists();out.mkdir()
            record=dict(status='preparing',target=args.target,subjects=current_subjects(args.target),
                started=datetime.datetime.now(datetime.timezone.utc).isoformat(),
                scope='Supplemental native CPU scheduling selector and applicable Suspend/Resume checks on both genuine jobs. Original two desktop cancellation/lifecycle cycles are preserved. Modern original suspended hold is observed, then native Resume runs while a declared empty-assembler removal holds the job; that prerequisite restores after original Cancel. No client status assignment or input retry. Presentation-only atlas variants remain separate.')
            def save():(out/'summary.json').write_text(json.dumps(record,indent=2)+'\n')
            save()
            try:
                prepared=prepare(out,mc)
                inputs=provenance.sources(ROOT,dict(script='functional-tests/scripts/Run-Ae2ReleaseQa.ps1'))
                for p in [Path(__file__),Path(__file__).with_name('ae2_cpu_controls_source.py'),ROOT/'tools/ae2_cpu_fabric_lineage.py',ROOT/'tools/verify-controlify-focused-runtime.py',ROOT/'tools/VerifyFabricRuntimeRemap.java',ROOT/'tools/focused_release_evidence_ae2_drive_gutter.py',*prepared['inputs']]:inputs[p.relative_to(ROOT).as_posix()]=sha(p)
                record.update(status='running',qa_inputs=inputs,qa_source_archive=provenance.bundle(ROOT,inputs),
                    original_shared_source_sha256=prepared['original_shared'])
                command=['pwsh','-NoProfile','-File',str(prepared['script']),'-Minecraft',mc,'-Loader',loader,'-CpuLifecycle','-RunId',args.run]
                record['command']=command;save()
                log=out/'driver.log'
                with log.open('x')as stream:r=subprocess.run(command,cwd=ROOT,stdout=stream,stderr=subprocess.STDOUT,creationflags=subprocess.CREATE_NO_WINDOW if sys.platform=='win32'else 0)
                record['exit_code']=r.returncode;assert r.returncode==0,'Supplemental native CPU run failed'
                folder=ROOT/'build/gameplay-qa/packaged-native'/(mc+'-'+loader+'-'+args.run)
                atlas=folder/'atlas';controls=read(atlas/'cpu-controls.json');lifecycle=read(atlas/'cpu-lifecycle.json')
                checks=validate_controls(args.target,controls,lifecycle,(folder/'launch.log').read_text(errors='replace'))
                runtime=capture_runtime(folder,args.target,record['subjects'])
                assert inputs=={name:sha(ROOT/name)for name in inputs},'CPU source/generated inputs changed during run'
                assert current_subjects(args.target)==record['subjects'],'Production subjects changed during run'
                acceptance=out/'native-controls.json'
                acceptance.write_text(json.dumps(dict(complete=True,checks=checks,
                    actual_observations=[reference(ROOT,atlas/'cpu-controls.json'),reference(ROOT,atlas/'cpu-lifecycle.json')],
                    scope=record['scope']),indent=2)+'\n')
                record.update(status='passed',acceptance=reference(ROOT,acceptance),native_controls=reference(ROOT,atlas/'cpu-controls.json'),
                    native_lifecycle=reference(ROOT,atlas/'cpu-lifecycle.json'),runtime_evidence=runtime,
                    original_inner_log=reference(ROOT,folder/'launch.log'),driver_log=reference(ROOT,log),
                    results=[dict(target=args.target,status='passed',exit_code=0,profile=str(folder.relative_to(ROOT)),
                        results=dict(cases=1,failed=0,checks=len(checks),ae2_cpu_controls_checked=True,
                            original_gameplay_records=len(read(folder/'results.json')['results']),native_job_cancellations=2))])
                # The same public record is serialized before revalidation,
                # matching the representation future evidence readers consume.
                verify_run(ROOT,json.loads(json.dumps(record)),current_subjects(args.target))
            except BaseException as error:
                record.update(status='failed',error=repr(error),trace=traceback.format_exc());raise
            finally:
                record['finished']=datetime.datetime.now(datetime.timezone.utc).isoformat();save()
        finally:lock.seek(0);msvcrt.locking(lock.fileno(),msvcrt.LK_UNLCK,1)


if __name__=='__main__':main()
