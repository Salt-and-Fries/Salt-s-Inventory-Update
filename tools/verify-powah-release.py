"""Assemble focused Powah evidence and distribute only the tested release bytes."""
from pathlib import Path
import hashlib
import json
import shutil
import zipfile
from PIL import Image

ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'output/powah-implemented'
RUNS={
    'fabric':['1.20.1-fabric-siu-powah-implemented-02','1.20.1-fabric-siu-powah-advanced-03','1.20.1-fabric-siu-powah-final-01','1.20.1-fabric-siu-powah-fluid-final-01'],
    'forge':['1.20.1-forge-siu-powah-implemented-01','1.20.1-forge-siu-powah-advanced-02','1.20.1-forge-siu-powah-final-01','1.20.1-forge-siu-powah-fluid-final-01'],
}
def digest(path):return hashlib.sha256(path.read_bytes()).hexdigest()
def read(path):return json.loads(path.read_text())
def relative(path):return path.relative_to(ROOT).as_posix()


def fluid_checks(loader,names):
    base=ROOT/'build/remaining-ui-audit/gameplay'
    before=read(base/names[0]/'powah-checks/journal.json')
    after=read(base/names[-1]/'powah-checks/journal.json')
    checks=[];pixels=[]
    cases=[('magmator-fluid-active','magmator',False),('thermo_generator-fluid-active','thermo_generator',False),
           ('reactor-fluid-active','reactor',False),('magmator-full-tank','magmator',True),
           ('magmator-full-tank-moved','magmator',True)]
    def working_pixels(row):
        im=Image.open(ROOT/row['screenshot']).convert('RGB').resize((640,450),Image.Resampling.NEAREST)
        b=row['bounds'];body=next(r for r in row['composition']['regions']if r['id']=='native-machine')
        return im,b['x']+7+body['x'],b['y']+25+body['y']
    for name,block,full in cases:
        row=next(r for r in after['captures']if r['id']==name)
        old=next(r for r in before['captures']if r['id']==block)
        state=read(base/names[-1]/'powah-checks'/(name+'-client.json'))
        assert not next(w for w in state['allWindows']if w.get('menuId')=='powah:'+row['menu'])['layoutError']
        im,dx,dy=working_pixels(row);ref,rx,ry=working_pixels(old)
        def colored(x,y):
            r,g,b=im.getpixel((dx+x,dy+y))
            return r>g+25 and r>b+50 if block=='magmator'else b>r+20
        count=sum(colored(x,y)for x in range(157,171)for y in range(5,70))
        bottom=sum(colored(x,69)for x in range(157,171))
        rim=sum(im.getpixel((dx+x,dy+y))!=ref.getpixel((rx+x,ry+y))
            for x in range(154,174)for y in range(75)if not(157<=x<171 and 5<=y<70))
        values={'visible-native-fluid':count>=112 and bottom==14,'unchanged-complete-tank-rim':rim==0}
        if full:values['full-tank-entire-65-row-fill']=count==14*65
        for suffix,passed in values.items():
            checks.append(dict(id=name+'-'+suffix,passed=passed,journal='output/powah-implemented/fluid-verification.json'))
        pixels.append(dict(loader=loader,capture=name,colored_pixels=count,bottom_colored_pixels=bottom,
            tank_rim_mismatches=rim,screenshot=row['screenshot'],sha256=digest(ROOT/row['screenshot'])))
    assert all(c['passed']for c in checks),[c for c in checks if not c['passed']]
    return checks,pixels


def main():
    targets=[];fluid_pixels=[]
    for loader,names in RUNS.items():
        latest={};records=[];old_jars=[]
        for name in names:
            run=ROOT/'build/remaining-ui-audit/gameplay'/name
            journal=read(run/'powah-checks/journal.json')
            assert journal['checks'] and all(c['passed']for c in journal['checks']),name
            stage=read(run/'staged-artifacts.json')
            compat=next(r for r in stage['artifacts']if r['family']=='compatibility')
            core=next(r for r in stage['artifacts']if r['family']=='core')
            runtime=Path(read(run/'launch.json')['cwd'])
            jar=runtime/'mods'/compat['filename'];assert digest(jar)==compat['sha256']
            assert digest(Path(core['file']))==core['sha256'],'Core changed after live checks'
            old_jars.append(jar)
            for c in journal['checks']:latest[c['id']]={**c,'journal':relative(run/'powah-checks/journal.json')}
            records.append(dict(run=relative(run),journal=relative(run/'powah-checks/journal.json'),
                recorded_assertions=len(journal['checks']),captures=len(journal['captures']),
                compatibility_sha256=compat['sha256'],core_sha256=core['sha256']))
        final=old_jars[-1]
        extra,pixels=fluid_checks(loader,names);fluid_pixels.extend(pixels)
        for c in extra:latest[c['id']]=c
        build=ROOT/f'compatibility/versions/1.20.1/{loader}/build/libs'/final.name
        assert digest(build)==digest(final),'Release build differs from final live runtime'
        # Earlier geometry remains relevant: only payload validation and the
        # final-surface fluid overlay changed after that evidence was captured.
        revisions=[]
        with zipfile.ZipFile(final)as current:
            entries=set(current.namelist())
            assert not any('NativeUiReference'in p or 'native_ui_reference'in p for p in entries)
            required={'com/salts_inventory_update/compat/powah/'+n+'.class'for n in ['PowahWindows','PowahClient','PowahChrome']}
            assert required<=entries
            for old in old_jars[:-1]:
                with zipfile.ZipFile(old)as prior:
                    assert entries-set(prior.namelist())=={'com/salts_inventory_update/compat/powah/PowahFluids.class'}
                    assert not set(prior.namelist())-entries
                    changed=[p for p in prior.namelist()if prior.read(p)!=current.read(p)]
                    assert set(changed)<={'com/salts_inventory_update/compat/powah/PowahWindows.class',
                        'com/salts_inventory_update/compat/powah/PowahClient$Template.class'},changed
                    revisions.append(dict(sha256=digest(old),changed_entries=sorted(changed)))
            if loader=='fabric':
                metadata=json.loads(current.read('fabric.mod.json'))
                assert 'powah'not in metadata.get('depends',{})
            else:
                assert 'modId="powah"'not in current.read('META-INF/mods.toml').decode()
        dest=ROOT/'dist/compatibility'/final.name;shutil.copy2(build,dest)
        assert digest(dest)==digest(final)
        targets.append(dict(minecraft='1.20.1',loader=loader,powah='5.0.11',jar=relative(dest),
            sha256=digest(dest),live_assertions=len(latest),checks=list(latest.values()),runs=records,
            earlier_evidence_revision= revisions,qa_excluded=True,hard_powah_dependency=False,
            core_unchanged=True))
    artwork=read(OUT/'artwork-verification.json');assert artwork['mismatches']==0
    (OUT/'fluid-verification.json').write_text(json.dumps(dict(status='pass',records=fluid_pixels),indent=2)+'\n')
    report=dict(status='pass',scope='Approved 1.20.1 Powah windows on Fabric and Forge only',
        live_assertions=sum(t['live_assertions']for t in targets),
        assertion_counting='Distinct affected-window assertion IDs per loader; repeated final-build checks replace prior records.',
        targets=targets,artwork={k:v for k,v in artwork.items()if k!='crops'},
        advanced_runs={l:'build/remaining-ui-audit/gameplay/'+names[1]for l,names in RUNS.items()},
        fluid_runs={l:'build/remaining-ui-audit/gameplay/'+names[-1]for l,names in RUNS.items()},
        presentation_runs={l:'build/remaining-ui-audit/gameplay/'+names[-1]for l,names in RUNS.items()},
        scope_types=11,opening_routes=12,
        source_revision='b7343d065eec0c57c4bd99dcde7bbdbccccf1307',
        build_log='tmp/powah-build-fluid-final.log',
        limits=['Focused affected-window checks, not a complete functional or gameplay suite.',
            'Representative tiers and states; not every automation or optional-mod combination.',
            'No dedicated multiplayer, alternate resource packs or newer Minecraft ports exercised.',
            'Energy fill fixtures and disposable creative item setup are observation helpers, not shipping behavior.'])
    (OUT/'verification.json').write_text(json.dumps(report,indent=2)+'\n')
    print(json.dumps(dict(status=report['status'],distinct_assertions=report['live_assertions'],
        targets=[{k:t[k]for k in ['loader','jar','live_assertions','sha256']}for t in targets]),indent=2))


if __name__=='__main__':main()
