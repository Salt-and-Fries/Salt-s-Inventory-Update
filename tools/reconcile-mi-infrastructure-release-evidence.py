"""Review actual native pump cycles and formed-tank status on two MI releases."""
from pathlib import Path
import argparse, datetime, importlib.util, json, math, re, sys
import nbtlib

ROOT = Path(__file__).resolve().parents[1]
sys.path[:0] = [str(ROOT/'tools'), str(ROOT/'functional-tests/release-specialized')]
spec = importlib.util.spec_from_file_location('mi_storage_evidence_shared', ROOT/'tools/reconcile-mi-storage-release-evidence.py')
storage = importlib.util.module_from_spec(spec); spec.loader.exec_module(storage)
from focused_release_evidence import checked, reference, verify as verify_sealed
from mi_infrastructure_features import PUMPS, tank_matches, tank_approach
from mi_storage_features import cursor_is, physical, physical_item, resources, policies, typed
stack=storage.stack

TARGETS = storage.TARGETS
PREFIX = storage.PREFIX
SERVER = storage.SERVER
CASES = [PREFIX+name for name in PUMPS+['large_tank']]
DRIVER = 'functional-tests/release-specialized/mi_infrastructure_features.py'
SCHEMA = 'mi-native-pump-production-and-formed-tank-status-v1'
PUMP_BE = 'aztech.modern_industrialization.machines.blockentities.SteamWaterPumpBlockEntity'
TANK_BE = 'aztech.modern_industrialization.machines.blockentities.multiblocks.LargeTankMultiblockBlockEntity'
PUMP_NAMES = ['exact foreign typed fixture and empty cursor',
    'real native pumping consumes steam and exposes active progress',
    'one real cycle consumes exact steam and generates exact water',
    'native bucket withdraws genuinely generated water exactly once',
    'original pump output refuses water reinsertion',
    'generated fluid and full physical items persist after reopening']
TANK_NAMES = ['exact foreign typed fixture and empty cursor',
    'original unformed blueprint is genuine and complete',
    'ordinary native structure computes its real empty capacity',
    'populated native gauge retains real formation and capacity',
    'formed populated native status persists on ordinary reopening']
LIMITS = ('Two exact pinned native releases only. Pump steam is a declared pre-entry prerequisite; '
    'water production is witnessed through the original native cycle. Tank water is separately '
    'declared stored content: tank-fluid transfer and production are not qualified. No long '
    'production-chain, automatic piping, whole-mod acceptance or invented retained close snapshot.')


def native_classes(target):
    return storage.native_classes(target)+[PUMP_BE,TANK_BE]


def specification(target, case, classes):
    native_classes(target)
    assert case in CASES and classes == [SERVER], 'Unreviewed native infrastructure variant/class'
    return 'read-only-status-pass' if case == CASES[-1] else 'functional-pass'


def source_tag(text, case):
    tag = nbtlib.parse_nbt(text)
    assert tag.get('id') == nbtlib.String('modern_industrialization:'+case.removeprefix(PREFIX))
    assert all(isinstance(tag.get(key),nbtlib.Int) and int(tag[key])==value
        for key,value in [('x',8),('y',65),('z',8)])
    return tag


def native_probe(probe, target, case, entity):
    assert probe['blockEntity']==entity and probe['client']['menu']==storage.native_classes(target)[1]
    return source_tag(probe['fixture'],case)


def pump(target, case, frames, probes, multiplier):
    assert [v[0]for v in frames]==['empty','active-native-cycle','completed-native-cycle',
        'native-generated-water-withdrawn','native-pump-output-refuses-reinsertion','native-pump-reopened']
    assert [v[0]for v in probes]==['active-native-cycle','completed-native-cycle']
    mc=target.split('/')[0];unit=81000 if mc=='1.20.1'else 1000
    key='fluid'if mc=='1.20.1'else'id'
    water=nbtlib.parse_nbt('{'+key+':"minecraft:water"}')
    steam=nbtlib.parse_nbt('{'+key+':"modern_industrialization:steam"}')
    initial,_,cells=storage.common(frames[0][1],target,case,'fluids')
    assert len(cells)==2 and resources(cells)==() and cursor_is(initial)
    assert not bool(cells[1].get('playerInsert',False)) and bool(cells[1].get('playerExtract',False))
    baseline=physical(initial,exclude=(9,));policy=policies(cells)
    assert stack(initial['inventory'][9]['item'])==stack('{}')
    produced=((1,multiplier*unit,typed(water)),)
    remaining=()if multiplier==1 else((1,(multiplier-1)*unit,typed(water)),)
    for stage,(_,frame)in enumerate(frames):
        data,_,actual=storage.common(frame,target,case,'fluids',active_pump=stage==1)
        assert len(actual)==2 and policies(actual)==policy and physical(data,exclude=(9,))==baseline
        if stage==1:
            res=resources(actual)
            assert len(res)==1 and res[0][0]==0 and 0<res[0][1]<unit//10 and res[0][2]==typed(steam)
        else:
            assert resources(actual)==(()if stage==0 else produced if stage==2 else remaining)
        if stage in [3,4]:
            assert cursor_is(data,physical_item(mc,'minecraft:water_bucket',1)) and stack(data['inventory'][9]['item'])==stack('{}')
        else:
            assert cursor_is(data)
            expected='{}'if stage==0 else physical_item(mc,'minecraft:water_bucket'if stage==5 else'minecraft:bucket',1)
            assert stack(data['inventory'][9]['item'])==stack(expected)
    active=probes[0][1];tag=native_probe(active,target,case,PUMP_BE)
    progress=active['client']['components']['progress']
    assert type(progress)in{int,float}and math.isfinite(progress)and 0<progress<1
    assert 0<int(tag['pumpingTicks'])<100 and 0<int(tag['fluids'][0]['amount'])<unit//10
    res=resources(tag['fluids'])
    assert len(res)==1 and res[0][0]==0 and res[0][2]==typed(steam)and policies(tag['fluids'])==policy
    completed=probes[1][1];tag=native_probe(completed,target,case,PUMP_BE)
    assert int(tag['pumpingTicks'])==0 and resources(tag['fluids'])==produced and policies(tag['fluids'])==policy


def tank(target, case, frames, probes):
    labels=['empty','native-formed-empty-tank','native-populated-formed-tank','native-populated-tank-reopened']
    assert [v[0]for v in frames]==labels
    assert [v[0]for v in probes]==['original-unformed-blueprint',*labels[1:]]
    mc=target.split('/')[0];unit=81000 if mc=='1.20.1'else 1000
    baseline=None;blueprint=None
    for stage,((_,frame),(_,probe))in enumerate(zip(frames,probes)):
        data,_,_=storage.common(frame,target,case,None)
        assert cursor_is(data)
        if baseline is None:baseline=physical(data)
        assert physical(data)==baseline
        components=frame['client']['windows'][0].get('nativeValues',{}).get('miComponentBounds',[])
        assert sum(v['component']=='LargeTankFluidDisplayClient'for v in components)==1,'Missing native tank gauge geometry'
        tag=native_probe(probe,target,case,TANK_BE)
        actual=probe['blueprint'];assert isinstance(actual,list)and len(actual)==25
        assert all(type(v[k])is int for v in actual for k in ['x','y','z'])
        assert all(isinstance(v['block'],str)and re.fullmatch(r'[a-z0-9_.-]+:[a-z0-9_/.-]+',v['block'])for v in actual)
        assert len({(v['x'],v['y'],v['z'])for v in actual})==25
        assert not any((v['x'],v['y'],v['z'])==(8,65,8)for v in actual)
        ordered=sorted((v['x'],v['y'],v['z'],v['block'])for v in actual)
        if blueprint is None:blueprint=ordered;tank_approach(actual)
        assert ordered==blueprint,'Original native blueprint changed during formation/status workflow'
        if stage==0:
            assert probe['shapeValid']is False
        else:
            expected=0 if stage==1 else unit
            assert tank_matches(probe,unit,expected)
            server_tag=source_tag(data['fixture'],case)
            assert typed(server_tag['amount'])==typed(tag['amount'])and typed(server_tag['capacity'])==typed(tag['capacity'])
            if expected:
                fluid=nbtlib.parse_nbt('{'+('fluid'if mc=='1.20.1'else'id')+':"minecraft:water"}')
                assert typed(tag['fluid'])==typed(fluid)and typed(server_tag['fluid'])==typed(fluid)


def validate(target,journal,states,driver_hash):
    native_classes(target)
    assert journal['complete']is True and journal['driver_sha256']==driver_hash and journal['cases']==CASES
    assert set(states)==set(CASES)
    expected=[]
    for case,multiplier in zip(CASES[:2],[1,2]):
        assert set(states[case])=={'frames','probes'}
        pump(target,case,states[case]['frames'],states[case]['probes'],multiplier)
        expected.extend((case,name)for name in PUMP_NAMES)
    assert set(states[CASES[-1]])=={'frames','probes'}
    tank(target,CASES[-1],states[CASES[-1]]['frames'],states[CASES[-1]]['probes'])
    expected.extend((CASES[-1],name)for name in TANK_NAMES)
    expected.append((CASES[-1],'native infrastructure closes all owned sessions'))
    assert [(v['case'],v['name'])for v in journal['checks']]==expected and len(expected)==18
    assert all(v.get('passed')is True for v in journal['checks'])
    return dict(assertions=18,states=24,native_variants=3,
        contract='two-real-pump-cycles-and-formed-tank-status-not-tank-production')


def derive(root,sealed_path,expected,case,original_classes):
    root=Path(root).resolve();sealed=json.loads(Path(sealed_path).read_text());target=sealed['target']
    disposition=specification(target,case,original_classes);verify_sealed(root,sealed,expected)
    summary=json.loads(checked(root,sealed['summary']).read_text());row=summary['results'][0];result=row['results']
    assert result.get('mi_infrastructure_features_checked')is True
    assert type(result['cases'])is int and result['cases']==3 and type(result['failed'])is int and result['failed']==0
    assert type(result['checks'])is int and result['checks']==18
    profile=(root/row['profile']).resolve();journal_path=checked(root,sealed['acceptance'])
    assert journal_path==profile/'mi-infrastructure-release/summary.json'
    states={c:dict(frames=[],probes=[])for c in CASES};refs=[]
    for number,path in enumerate(sorted(journal_path.parent.glob('[0-9][0-9][0-9]-*.json')),1):
        match=re.fullmatch(r'([0-9]{3})-(machine-modern_industrialization-[a-z_]+)-([a-z0-9-]+)\.json',path.name)
        assert match and int(match[1])==number and match[2]in states
        ref=reference(root,path);assert ref in sealed['full_states'];refs.append(ref)
        label=match[3];probe=label.endswith('-infrastructure')
        states[match[2]]['probes'if probe else'frames'].append((label.removesuffix('-infrastructure')if probe else label,json.loads(path.read_text())))
    assert len(refs)==24
    validation=validate(target,json.loads(journal_path.read_text()),states,sealed['archived_inputs'][DRIVER])
    classes=native_classes(target);publishers=storage.native_publishers(root,sealed,classes)
    scope=('Native large tank built from original blueprint: genuine empty formation/capacity, declared stored-water populated gauge, complete geometry and full inventory/cursor conservation through reopening.'
        if disposition=='read-only-status-pass'else'Native steam water pump: real active progress/steam consumption, completed exact generated water, native bucket extraction and output-only refusal, full ledgers and reopening.')
    return dict(schema=SCHEMA,target=target,case=case,disposition=disposition,original_native_classes=original_classes,native_classes=classes,
        subjects={k:list(v)for k,v in expected.items()},sealed_evidence=reference(root,sealed_path),acceptance=reference(root,journal_path),
        native_states=refs,validation=validation,publishers=publishers,scope=scope,limits=LIMITS)


def verify(root,record,expected):
    assert record['schema']==SCHEMA and record['disposition']==specification(record['target'],record['case'],record['original_native_classes'])
    assert derive(root,checked(root,record['sealed_evidence']),expected,record['case'],record['original_native_classes'])==record
    return True


def main():
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('--generation',required=True);parser.add_argument('--plan-only',action='store_true');args=parser.parse_args()
    assert re.fullmatch(r'build[0-9]+',args.generation)
    out=ROOT/'output/release-readiness';backlog=json.loads((out/'coverage-backlog.json').read_text())
    plan=json.loads((ROOT/'build/release-readiness/focused-native-plan-02.json').read_text());jobs=[v for v in plan['jobs']if v['family']=='mi_infrastructure']
    assert len(jobs)==2 and {v['target']for v in jobs}==set(TARGETS);jobs={v['target']:v for v in jobs}
    rows=[r for r in backlog['records']if r['case']in CASES]
    assert len(rows)==6 and {(r['target'],r['case'])for r in rows}=={(t,c)for t in TARGETS for c in CASES}
    if args.plan_only:
        for row in rows:print(row['target'],row['case'],'requires actual sealed',jobs[row['target']]['run'])
        print('Plan only; no pump/tank acceptance.');return
    sys.path.insert(0,str(ROOT/'functional-tests/project-everything-regression'))
    import hashlib,importlib.util
    helper=ROOT/'build/release-readiness/release_queue_history_455.py'
    assert hashlib.sha256(helper.read_bytes()).hexdigest()=='fb00a5e47131d9ca30263154439a17f13ea3661eef2651febe9a9fa75fa3affc'
    sp=importlib.util.spec_from_file_location('MI_original_native_history',helper);history=importlib.util.module_from_spec(sp);sp.loader.exec_module(history)
    resolved=history.completed(plan,json.loads((out/'focused-native-campaign-02.json').read_text()));assert len(resolved)==174
    jobs={v['target']:v for v in resolved if v['family']=='mi_infrastructure'};assert set(jobs)==set(TARGETS)
    from subject_artifacts import current_subjects
    from release_candidate_identity import current
    from focused_release_evidence_core_log_consumer import verified
    candidate=current(ROOT,verify_worktree=True);records=[]
    consumer=verified(ROOT,args.generation,candidate)
    for row in rows:
        record=derive(ROOT,out/('focused-evidence-'+jobs[row['target']]['run']+'.json'),consumer.expected(row['target'],json.loads((out/('focused-evidence-'+jobs[row['target']]['run']+'.json')).read_text()),current_subjects(row['target'])),row['case'],row.get('historical_native_classes',row['native_classes']))
        assert row['current_exact_pins']and {v['sha512']for v in row['current_exact_pins']}<={v['sha512']for v in record['publishers']},'Unqualified native MI publisher patch'
        records.append(record)
    destination=out/f'coverage-mi-infrastructure-{args.generation}-01.json';assert not destination.exists()
    destination.write_text(json.dumps(dict(status='reviewed-native-mi-infrastructure-workflows-reconciled',created=datetime.datetime.now(datetime.timezone.utc).isoformat(),
        candidate_identity=candidate,artifact_equivalence=consumer.report(),auditor_inputs=[reference(ROOT,ROOT/'tools/mi_owned_geometry.py'),reference(ROOT,helper),reference(ROOT,Path(__file__)),reference(ROOT,ROOT/'tools/reconcile-mi-storage-release-evidence.py')],records=records,limits=LIMITS),indent=2)+'\n')
    print('Reconciled6UIrecords from two actual native workflows; pump production and tank read-only status retain separate scopes.')


if __name__=='__main__':main()
