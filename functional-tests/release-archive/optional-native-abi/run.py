"""Exercise actual shipping optional-AE2 guard bytecode in isolated unit fixtures."""
from pathlib import Path
import argparse,datetime,hashlib,json,re,shutil,subprocess,sys,traceback,zipfile
ROOT=Path(__file__).resolve().parents[3];sys.path.insert(0,str(ROOT/'tools'))
from release_candidate_identity import current
parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('--run',required=True);parser.add_argument('--java-root',type=Path)
args=parser.parse_args();assert re.fullmatch('[a-z0-9-]+',args.run)
STAGE=ROOT/'build/release-readiness'/args.run;OUT=ROOT/'output/release-readiness'/args.run
assert not STAGE.exists()and not OUT.exists(),'Preserve original attempts; use a new run ID'
STAGE.mkdir();OUT.mkdir();SOURCE=Path(__file__).with_name('source')
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def ref(p):return dict(file=str(p.relative_to(ROOT)),sha256=sha(p))
def now():return datetime.datetime.now(datetime.timezone.utc).isoformat()
state=dict(status='preparing-process-isolated-unit-probe',started=now(),scope=__doc__,scenarios=[],limits='Synthetic Mixin environment and bytecode-provider seams around the actual shipping plugin class. No Minecraft/Gradle process or fallback-rendering/native gameplay acceptance.');manifest=OUT/'manifest.json'
def save():manifest.write_text(json.dumps(state,indent=2)+'\n')
save()
try:
 candidate=current(ROOT,verify_worktree=True);state['candidate_identity']=candidate
 native=json.loads((ROOT/'build/native-container-symbol-inputs.json').read_text());libs=[Path(p)for p in native[0]['libraries']]
 selected=[]
 for prefix in ['asm-9.7.1','asm-analysis-9.7.1','asm-commons-9.7.1','asm-tree-9.7.1','asm-util-9.7.1','sponge-mixin-0.15.4','gson-2.10.','slf4j-api-2.0.1']:
  found=[p for p in libs if p.name.startswith(prefix)];assert len(found)==1,(prefix,found);selected.append(found[0])
 state['libraries']=[dict(file=str(p),sha256=sha(p))for p in selected]
 jdk=(args.java_root or Path(next(v['jdk']for v in native if v['minecraft']=='26.3'))).resolve();java=jdk/'bin/java.exe';javac=jdk/'bin/javac.exe';state['jdk']=dict(java=str(java),java_sha256=sha(java),javac=str(javac),javac_sha256=sha(javac),release=(jdk/'release').read_text())
 canonical_source=sorted(SOURCE.rglob('*.java'));assert len(canonical_source)==4
 state['canonical_source_inputs']=[ref(p)for p in [Path(__file__),*canonical_source]]
 shutil.copytree(SOURCE,STAGE/'source')
 source=sorted((STAGE/'source').rglob('*.java'));assert len(source)==4
 state['source_inputs']=[ref(p)for p in source];classes=STAGE/'classes';classes.mkdir()
 args=[str(javac),'-proc:none','--release','17','-cp',';'.join(map(str,selected)),'-d',str(classes),*map(str,source)]
 r=subprocess.run(args,cwd=ROOT,text=True,capture_output=True);(OUT/'compile.log').write_text(r.stdout+r.stderr);state['compile']=dict(arguments=args[1:],exit_code=r.returncode,log=ref(OUT/'compile.log'));save();assert r.returncode==0
 service=classes/'META-INF/services/org.slf4j.spi.SLF4JServiceProvider';service.parent.mkdir(parents=True);service.write_text('probe.logging.ProbeProvider\n')
 state['compiled_inputs']=[ref(p)for p in sorted(classes.rglob('*'))if p.is_file()]
 artifacts=json.loads((ROOT/'output/release-readiness/candidate-integrity.json').read_text())['artifacts'];subjects=[]
 for artifact in artifacts:
  if artifact['family']!='compatibility':continue
  p=ROOT/artifact['file']
  with zipfile.ZipFile(p)as z:
   name='com/salts_inventory_update/compat/ae2/mixin/NativeDesktopPatchPlugin.class'
   if name not in z.namelist():continue
   subjects.append(dict(artifact,plugin_class_sha256=hashlib.sha256(z.read(name)).hexdigest()))
 assert len(subjects)==6
 state['subjects']=subjects;state['status']='running-isolated-unit-scenarios';save()
 for artifact in subjects:
  jar=ROOT/artifact['file'];assert sha(jar)==artifact['sha256']
  target=artifact['minecraft']+'/'+artifact['loader'];classpath=';'.join(map(str,[classes,jar,*selected]))
  for side in ['client','server']:
   for mode in ['absent-cnf','absent-io','contract-cnf','contract-io','contract-shape']:
    slug=target.replace('/','-').replace('.','')+'-'+side+'-'+mode
    args=[str(java),'-cp',classpath,'probe.OptionalAbiMain',str(jar),mode,side]
    row=dict(target=target,side=side,mode=mode,status='running',arguments=args[1:]);state['scenarios'].append(row);save()
    r=subprocess.run(args,cwd=ROOT,text=True,capture_output=True);log=OUT/(slug+'.log');log.write_text(r.stdout+r.stderr);row.update(exit_code=r.returncode,log=ref(log))
    if r.returncode!=0:row['status']='failed';save();raise RuntimeError('Preserved unit-harness scenario failure '+slug)
    data=json.loads(r.stdout.splitlines()[-1]);assert data['status']=='passed'and data['hooks_active']is False
    assert data['mode']==mode and data['side']==side and Path(data['plugin_jar'])==jar
    row.update(status='passed',observations=data);save();print('PASS',slug,flush=True)
  assert sha(jar)==artifact['sha256']
 assert len(state['scenarios'])==60 and all(v['status']=='passed'for v in state['scenarios'])
 assert current(ROOT,verify_worktree=True)==candidate
 archive=OUT/'actual-probe-inputs.zip';assert not archive.exists()
 with zipfile.ZipFile(archive,'x',zipfile.ZIP_DEFLATED)as z:
  for p in dict.fromkeys([*canonical_source,*source,*[p for p in sorted(classes.rglob('*'))if p.is_file()],Path(__file__)]):z.write(p,p.relative_to(ROOT).as_posix())
 with zipfile.ZipFile(archive)as z:assert z.testzip()is None and len(z.namelist())==len(set(z.namelist()))
 state.update(status='passed-isolated-shipping-plugin-guard-scenarios-not-gameplay',finished=now(),qa_inputs_archive=ref(archive),original_shipping_bytes_unchanged=True);save()
 print('60isolated guard scenarios passed across6actual shipping plugins; no gameplay or fallback-rendering coverage assigned.')
except BaseException as error:
 state.update(status='unit-probe-failed-requires-classification',error=repr(error),trace=traceback.format_exc(),finished=now());save();raise
