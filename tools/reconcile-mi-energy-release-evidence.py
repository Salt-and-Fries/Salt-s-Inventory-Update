"""Bind real MI energy-hatch status evidence to twenty exact backlog records.

Read-only synchronization is the UI contract here. A declared stored-energy
prerequisite is not natural power generation or formed-multiblock acceptance.
No gameplay, fixtures, original assertions or publisher bytes are changed.
"""
from pathlib import Path
from collections import Counter
import argparse,datetime,hashlib,json,re,sys
import nbtlib

ROOT=Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT/'tools'))
sys.path.insert(0,str(ROOT/'functional-tests/release-specialized'))
from focused_release_evidence import checked,reference,verify as verify_sealed
from mi_owned_geometry import check_owned
from native_publisher_index import index,matches
from mi_energy_features import HATCHES,inventory,no_cursor,seeded,synchronized,original

TARGETS=['1.20.1/fabric','1.21.1/neoforge']
SERVER='aztech.modern_industrialization.machines.gui.MachineMenuServer'
DRIVER='functional-tests/release-specialized/mi_energy_features.py'
SCHEMA='mi-native-energy-status-observations-v1'
STATUS='read-only-status-pass'
PREFIX='machine-modern_industrialization-'
LABELS=['empty','charged','reopened']
NAMES=['original zero-item panel and empty synchronized status','charged native component matches authoritative prerequisite','native status and full physical ledger persist on reopening']
REQUIRED=Counter([(PREFIX+hatch,name)for hatch in HATCHES for name in NAMES]+[(PREFIX+HATCHES[0],'declared typed foreign inventory fixture exists'),('all-energy-hatches','closing leaves no owned desktop session')])
SCOPE='All ten original MI energy-hatch UI variants: empty and declared1000EU status synchronized between native server NBT and client component, complete geometry, zero physical slots, full typed inventory/cursor conservation and ordinary reopening.'
LIMITS='Read-only status UI acceptance on the exact native target/publisher. Charge is a declared pre-entry prerequisite. No natural power generation, cable transmission, formed multiblock, recipe production or whole-mod gameplay acceptance. Ten variants sharing a generic menu remain explicitly identified by native block ID.'

def native_classes(target):
    assert target in TARGETS
    prefix='aztech.modern_industrialization.'+(''if target=='1.20.1/fabric'else'client.')+'machines.'
    return [SERVER,prefix+'gui.MachineMenuClient',prefix+'gui.MachineScreen',prefix+'guicomponents.EnergyBarClient']

def specification(target,case,classes):
    native_classes(target)
    assert case in {PREFIX+v for v in HATCHES}and classes==[SERVER],'Unreviewed native machine/alias/class'
    return case.removeprefix(PREFIX)

def native_empty_cursor(text,mc):
    value=nbtlib.parse_nbt(text)
    if not isinstance(value,nbtlib.Compound):return False
    if not value:return True
    return (mc=='1.20.1'and set(value)=={'id','Count'}and isinstance(value['id'],nbtlib.String)
        and str(value['id'])=='minecraft:air'and isinstance(value['Count'],nbtlib.Byte)and int(value['Count'])==0)


def validate(target,journal,states,driver_hash):
    classes=native_classes(target);mc=target.split('/')[0]
    assert journal['complete']is True and journal['driver_sha256']==driver_hash
    assert journal['cases']==[PREFIX+h for h in HATCHES]
    assert Counter((v['case'],v['name'])for v in journal['checks'])==REQUIRED
    assert all(v.get('passed')is True for v in journal['checks'])
    assert set(states)==set(HATCHES)
    baseline=None
    for hatch in HATCHES:
        frames=states[hatch];assert isinstance(frames,list)and len(frames)==3
        for frame,expected in zip(frames,[0,1000,1000]):
            assert set(frame)=={'client','server','observed'}
            client=frame['client'];server=frame['server'];observed=frame['observed']
            window,count=check_owned(original.geometry.check,client)
            assert count==0 and window['menuId']=='modern_industrialization:machine'
            assert len([v for v in window.get('nativeValues',{}).get('miComponentBounds',[])if v['component']=='EnergyBarClient'])==1
            assert observed['menu']==classes[1]and observed['screen']==classes[2]
            assert len(observed['energy'])==1 and observed['energy'][0]['type']==classes[3]
            tag=nbtlib.parse_nbt(server['fixture'])
            assert isinstance(tag,nbtlib.Compound)and tag.get('id')==nbtlib.String('modern_industrialization:'+hatch)
            assert all(isinstance(tag.get(key),nbtlib.Int)and int(tag[key])==value for key,value in [('x',8),('y',65),('z',8)])
            assert synchronized(server,observed,expected),'Native energy status does not match declared prerequisite'
            assert seeded(server,mc),'Missing exact typed foreign inventory seed'
            items=server['inventory'];assert isinstance(items,list)and len(items)>=36
            assert all(type(v['index'])is int and v['index']==i for i,v in enumerate(items))
            actual=inventory(server)
            if baseline is None:baseline=actual
            assert actual==baseline and no_cursor(server),'Full physical inventory or cursor changed'
            menus=server['menus'];assert sum(v.get('class')==SERVER for v in menus)==1
            assert all(isinstance(v.get('class'),str)and native_empty_cursor(v['carried'],mc)for v in menus)
    return dict(assertions=32,states=30,native_variants=10,contract='read-only-status-not-power-generation')

def derive(root,sealed_path,expected,case,original_classes):
    root=Path(root).resolve();sealed=json.loads(Path(sealed_path).read_text());target=sealed['target']
    specification(target,case,original_classes);verify_sealed(root,sealed,expected)
    summary=json.loads(checked(root,sealed['summary']).read_text());row=summary['results'][0];result=row['results']
    assert result.get('mi_energy_features_checked')is True
    assert type(result['cases'])is int and result['cases']==10 and type(result['failed'])is int and result['failed']==0
    assert type(result['checks'])is int and result['checks']==32
    profile=(root/row['profile']).resolve();journal_path=checked(root,sealed['acceptance'])
    assert journal_path==profile/'mi-energy-release/summary.json'
    states={};refs=[]
    for hatch in HATCHES:
        states[hatch]=[]
        for label in LABELS:
            path=journal_path.parent/(hatch+'-'+label+'.json');ref=reference(root,path)
            assert ref in sealed['full_states'];refs.append(ref);states[hatch].append(json.loads(path.read_text()))
    journal=json.loads(journal_path.read_text());validation=validate(target,journal,states,sealed['archived_inputs'][DRIVER])
    staged=json.loads(checked(root,sealed['staged']).read_text())['artifacts'];publishers=[];classes=native_classes(target)
    for artifact in staged:
        if artifact['family']!='upstream'or not artifact['filename'].lower().startswith('modern-industrialization-'):continue
        path=Path(artifact['file']);path=path if path.is_absolute()else root/path;raw=path.read_bytes()
        assert hashlib.sha256(raw).hexdigest()==artifact['sha256'];digest=hashlib.sha512(raw).hexdigest()
        if 'sha512'in artifact:assert artifact['sha512']==digest
        locations=matches(index(path,digest,target.split('/')[1]),classes)
        assert {name[:-6].replace('/','.')for loc in locations for name in loc['classes']}==set(classes)
        publishers.append(dict(filename=artifact['filename'],sha256=artifact['sha256'],sha512=digest,class_locations=locations))
    assert len(publishers)==1,'Exactly one actual native MI publisher required'
    return dict(schema=SCHEMA,target=target,case=case,disposition=STATUS,original_native_classes=original_classes,native_classes=classes,
        subjects={k:list(v)for k,v in expected.items()},sealed_evidence=reference(root,sealed_path),acceptance=reference(root,journal_path),
        native_states=refs,validation=validation,publishers=publishers,scope=SCOPE,limits=LIMITS)

def verify(root,record,expected):
    assert record['schema']==SCHEMA and record['disposition']==STATUS
    assert derive(root,checked(root,record['sealed_evidence']),expected,record['case'],record['original_native_classes'])==record
    return True

def main():
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('--generation',required=True);parser.add_argument('--plan-only',action='store_true');args=parser.parse_args()
    assert re.fullmatch(r'build[0-9]+',args.generation)
    out=ROOT/'output/release-readiness';backlog=json.loads((out/'coverage-backlog.json').read_text())
    plan=json.loads((ROOT/'build/release-readiness/focused-native-plan-02.json').read_text());jobs=[v for v in plan['jobs']if v['family']=='mi_energy']
    assert len(jobs)==2 and {v['target']for v in jobs}==set(TARGETS);jobs={v['target']:v for v in jobs}
    rows=[r for r in backlog['records']if r['case']in{PREFIX+h for h in HATCHES}]
    assert len(rows)==20 and {(r['target'],r['case'])for r in rows}=={(t,PREFIX+h)for t in TARGETS for h in HATCHES}
    if args.plan_only:
        for row in rows:print(row['target'],row['case'],'requires actual sealed',jobs[row['target']]['run'])
        print('Read-only status plan only; no coverage, generated energy or gameplay acceptance.');return
    sys.path.insert(0,str(ROOT/'functional-tests/project-everything-regression'))
    import hashlib,importlib.util
    helper=ROOT/'build/release-readiness/release_queue_history_455.py'
    assert hashlib.sha256(helper.read_bytes()).hexdigest()=='fb00a5e47131d9ca30263154439a17f13ea3661eef2651febe9a9fa75fa3affc'
    sp=importlib.util.spec_from_file_location('MI_original_native_history',helper);history=importlib.util.module_from_spec(sp);sp.loader.exec_module(history)
    resolved=history.completed(plan,json.loads((out/'focused-native-campaign-02.json').read_text()));assert len(resolved)==174
    jobs={v['target']:v for v in resolved if v['family']=='mi_energy'};assert set(jobs)==set(TARGETS)
    from subject_artifacts import current_subjects
    from release_candidate_identity import current
    from focused_release_evidence_core_log_consumer import verified
    candidate=current(ROOT,verify_worktree=True);records=[]
    consumer=verified(ROOT,args.generation,candidate)
    for row in rows:
        sealed=out/('focused-evidence-'+jobs[row['target']]['run']+'.json')
        record=derive(ROOT,sealed,consumer.expected(row['target'],json.loads((out/('focused-evidence-'+jobs[row['target']]['run']+'.json')).read_text()),current_subjects(row['target'])),row['case'],row.get('historical_native_classes',row['native_classes']))
        assert row['current_exact_pins']and {v['sha512']for v in row['current_exact_pins']}<={v['sha512']for v in record['publishers']},'Unqualified native MI publisher patch'
        records.append(record)
    destination=out/f'coverage-mi-energy-{args.generation}-01.json';assert not destination.exists()
    destination.write_text(json.dumps(dict(status='reviewed-native-mi-energy-status-workflows-reconciled',created=datetime.datetime.now(datetime.timezone.utc).isoformat(),candidate_identity=candidate,artifact_equivalence=consumer.report(),auditor_inputs=[reference(ROOT,ROOT/'tools/mi_owned_geometry.py'),reference(ROOT,helper),reference(ROOT,Path(__file__))],records=records,limits=LIMITS),indent=2)+'\n')
    print('Reconciled20read-only UI records from two actual ten-variant workflows; no new gameplay or power-generation claim.')

if __name__=='__main__':main()
