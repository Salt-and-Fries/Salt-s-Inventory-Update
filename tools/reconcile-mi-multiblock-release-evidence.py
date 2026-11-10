"""Review original formed MI controller workflows without inventing production."""
from pathlib import Path
import argparse,datetime,importlib.util,json,re,sys
import nbtlib

ROOT=Path(__file__).resolve().parents[1]
sys.path[:0]=[str(ROOT/'tools'),str(ROOT/'functional-tests/release-specialized')]
spec=importlib.util.spec_from_file_location('mi_infrastructure_evidence_shared',ROOT/'tools/reconcile-mi-infrastructure-release-evidence.py')
infra=importlib.util.module_from_spec(spec);spec.loader.exec_module(infra)
storage=infra.storage
from focused_release_evidence import checked,reference,verify as verify_sealed
from mi_multiblock_features import MACHINES,HATCHES,active_status
from mi_storage_contracts import hatch_kinds,select_native_hatches
from mi_storage_features import resources,physical,cursor_is,typed

TARGETS=storage.TARGETS;PREFIX=storage.PREFIX;SERVER=storage.SERVER
CASES=[PREFIX+v for v in MACHINES]
DRIVER='functional-tests/release-specialized/mi_multiblock_features.py'
SCHEMA='mi-native-formed-controller-recipe-and-invalidation-v1'
NAMES=['exact foreign typed fixture and empty cursor','original controller reports actual missing structure',
    'native fixed-shape controller has no editable shape tray',
    'ordinary native casing and allowed hatches form an idle machine',
    'real native recipe consumes steam and synchronizes active progress',
    'native single recipe yields exact guaranteed output without item creation elsewhere',
    'native structure removal reaches the original client status',
    'invalid status and every native hatch resource persist on ordinary reopening']
LIMITS=('Exact native release/controller workflows only. Inputs and steam are declared pre-entry '
    'prerequisites; native formation, progress and coke/blast completion are observed. Coke creosote '
    'retains its original bounded probabilistic outcome. Quarry has active-recipe and invalidation '
    'acceptance, not completed probabilistic output. Hatch transfers, long production chains and '
    'whole-mod gameplay are separate. Final close retains its original assertion only.')


def native_classes(target):
    classes=storage.native_classes(target)
    return classes+[classes[2].removesuffix('gui.MachineScreen')+'guicomponents.CraftingMultiblockGuiClient']


def specification(target,case,classes):
    native_classes(target)
    assert case in CASES and classes==[SERVER],'Unreviewed native controller/alias/class'
    return case.removeprefix(PREFIX)


def blueprint_contract(blueprint,case):
    assert isinstance(blueprint,list)and len(blueprint)>=24
    assert all(type(v[k])is int for v in blueprint for k in ['x','y','z'])
    assert len({(v['x'],v['y'],v['z'])for v in blueprint})==len(blueprint)
    assert all(isinstance(v['block'],str)and re.fullmatch(r'[a-z0-9_.-]+:[a-z0-9_/.-]+',v['block'])
        and isinstance(v['allows'],list)and all(isinstance(k,str)for k in v['allows'])for v in blueprint)
    selected=select_native_hatches(blueprint,case)
    assert len(selected)==len(hatch_kinds(case)) and [v['kind']for v in selected]==list(hatch_kinds(case))
    return selected,sorted((v['x'],v['y'],v['z'],v['block'],tuple(sorted(v['allows'])))for v in blueprint)


def pieces(probe,selected):
    actual=probe['pieces'];assert isinstance(actual,list)and len(actual)==len(selected)
    assert all(type(v[k])is int for v in actual for k in ['x','y','z'])
    by_position={(v['x'],v['y'],v['z']):v for v in actual}
    assert len(by_position)==len(selected) and set(by_position)=={(v['x'],v['y'],v['z'])for v in selected}
    result={}
    for wanted in selected:
        row=by_position[wanted['x'],wanted['y'],wanted['z']]
        tag=nbtlib.parse_nbt(row['nbt'])
        assert tag.get('id')==nbtlib.String(wanted['block'])
        assert all(isinstance(tag.get(k),nbtlib.Int)and int(tag[k])==wanted[k]for k in ['x','y','z'])
        assert isinstance(tag.get('items',[]),(list,nbtlib.List))and isinstance(tag.get('fluids',[]),(list,nbtlib.List))
        # Every nonempty native resource retains its exact Long/key signature.
        for kind in ['items','fluids']:resources(tag.get(kind,[]))
        result[wanted['kind']]=tag
    return result


def status(probe,case):
    infra.source_tag(probe['controller'],case)
    assert all(type(v)is bool for v in [probe['shapeValid'],probe['active'],probe['client']['shapeValid'],probe['client']['active']])
    assert isinstance(probe['operatingState'],str)and probe['operatingState']


def one(target,case,frames,probes):
    machine=specification(target,case,[SERVER]);mc=target.split('/')[0]
    unit=81000 if mc=='1.20.1'else 1000;key='item'if mc=='1.20.1'else'id';fluid_key='fluid'if mc=='1.20.1'else'id'
    labels=['native-controller-controls-observed','native-formed-idle','native-active-recipe']
    if machine!='steam_quarry':labels.append('native-recipe-completed')
    labels+=['native-structure-invalidated','native-invalid-controller-reopened']
    assert [v[0]for v in frames]==['empty',*labels]
    assert [v[0]for v in probes]==['native-unformed',*labels]
    selected,blueprint=blueprint_contract(probes[0][1]['blueprint'],case)
    baseline=None;parsed={};previous_invalid=None
    for stage,((label,frame),(_,probe))in enumerate(zip(frames,probes)):
        data,_,_=storage.common(frame,target,case,None)
        if baseline is None:baseline=physical(data)
        assert physical(data)==baseline and cursor_is(data)
        components=frame['client']['windows'][0].get('nativeValues',{}).get('miComponentBounds',[])
        assert sum(v['component']=='CraftingMultiblockGuiClient'for v in components)==1,'Missing native multiblock status geometry'
        status(probe,case)
        _,actual_blueprint=blueprint_contract(probe['blueprint'],case)
        assert actual_blueprint==blueprint,'Original native structure blueprint changed'
        if stage<2:
            assert probe['pieces']==[] and probe['shapeValid']is False and probe['client']['shapeValid']is False and probe['active']is False
            if stage==1:
                window=frame['client']['windows'][0]
                assert [v['component']for v in window.get('nativeValues',{}).get('miComponentBounds',[])]==['CraftingMultiblockGuiClient']
                assert not any(v['id']=='native-shape-tray'for v in window['template']['regions'])
            continue
        tags=pieces(probe,selected);parsed[label]=tags
        if label=='native-formed-idle':
            assert probe['shapeValid']is True and probe['client']['shapeValid']is True and probe['active']is False
            assert all(resources(tag.get(kind,[]))==()for tag in tags.values()for kind in ['items','fluids'])
        elif label=='native-active-recipe':
            assert active_status(probe)
            steam=nbtlib.parse_nbt('{'+fluid_key+':"modern_industrialization:steam"}')
            remaining=resources(tags['FLUID_INPUT']['fluids'])
            assert sum(v[1]for v in remaining)<4*unit and all(0<=v[1]and v[2]==typed(steam)for v in remaining)
        elif label=='native-recipe-completed':
            assert probe['active']is False and probe['client']['active']is False
            output_id='modern_industrialization:'+('coke'if machine=='coke_oven'else'steel_ingot')
            variant=nbtlib.parse_nbt('{'+key+':"'+output_id+'"}')
            assert resources(tags['ITEM_OUTPUT']['items'])==((0,1,typed(variant)),)and resources(tags['ITEM_INPUT']['items'])==()
            fluid=resources(tags['FLUID_OUTPUT']['fluids'])
            creosote=nbtlib.parse_nbt('{'+fluid_key+':"modern_industrialization:creosote"}')
            assert fluid==()or(machine=='coke_oven'and fluid==((0,unit//2,typed(creosote)),))
        else:
            assert label in {'native-structure-invalidated','native-invalid-controller-reopened'}
            assert probe['shapeValid']is False and probe['client']['shapeValid']is False
            if label=='native-structure-invalidated':previous_invalid=typed(tags)
            else:assert previous_invalid is not None and typed(tags)==previous_invalid,'Native hatch resource/metadata changed on reopening'
    return [name for name in NAMES if machine!='steam_quarry'or name!=NAMES[5]]


def validate(target,journal,states,driver_hash):
    native_classes(target)
    assert journal['complete']is True and journal['cases']==CASES and journal['driver_sha256']==driver_hash
    assert set(states)==set(CASES)
    expected=[]
    for case in CASES:
        assert set(states[case])=={'frames','probes'}
        names=one(target,case,states[case]['frames'],states[case]['probes'])
        expected.extend((case,name)for name in names)
    expected.append((CASES[-1],'native multiblock windows close all owned sessions'))
    assert [(v['case'],v['name'])for v in journal['checks']]==expected and len(expected)==24
    assert all(v.get('passed')is True for v in journal['checks'])
    return dict(assertions=24,states=40,native_variants=3,completed_guaranteed_recipes=2,
        contract='native-formed-active-invalidation-and-two-guaranteed-recipes-not-quarry-completion')


def derive(root,sealed_path,expected,case,original_classes):
    root=Path(root).resolve();sealed=json.loads(Path(sealed_path).read_text());target=sealed['target']
    machine=specification(target,case,original_classes);verify_sealed(root,sealed,expected)
    summary=json.loads(checked(root,sealed['summary']).read_text());row=summary['results'][0];result=row['results']
    assert result.get('mi_multiblock_features_checked')is True
    assert type(result['cases'])is int and result['cases']==3 and type(result['failed'])is int and result['failed']==0
    assert type(result['checks'])is int and result['checks']==24
    profile=(root/row['profile']).resolve();journal_path=checked(root,sealed['acceptance'])
    assert journal_path==profile/'mi-multiblock-release/summary.json'
    states={c:dict(frames=[],probes=[])for c in CASES};refs=[]
    for number,path in enumerate(sorted(journal_path.parent.glob('[0-9][0-9][0-9]-*.json')),1):
        match=re.fullmatch(r'([0-9]{3})-(machine-modern_industrialization-[a-z_]+)-([a-z0-9-]+)\.json',path.name)
        assert match and int(match[1])==number and match[2]in states
        ref=reference(root,path);assert ref in sealed['full_states'];refs.append(ref)
        data=json.loads(path.read_text());frame=set(data)=={'client','server','observed'}
        states[match[2]]['frames'if frame else'probes'].append((match[3],data))
    assert len(refs)==40
    validation=validate(target,json.loads(journal_path.read_text()),states,sealed['archived_inputs'][DRIVER])
    classes=native_classes(target);publishers=storage.native_publishers(root,sealed,classes)
    scope=('Original controller: unformed/formed/active/missing-structure states, explicit native absence of editable shape controls on these pins, complete geometry, typed physical/cursor conservation and exact backing-hatch persistence after reopening. '+
        ('Quarry witnesses genuine active recipe only, then structural invalidation.'if machine=='steam_quarry'else'One genuine native recipe completes with exact guaranteed item output; coke retains native probabilistic creosote bounds.'))
    return dict(schema=SCHEMA,target=target,case=case,disposition='functional-pass',original_native_classes=original_classes,native_classes=classes,
        subjects={k:list(v)for k,v in expected.items()},sealed_evidence=reference(root,sealed_path),acceptance=reference(root,journal_path),native_states=refs,
        validation=validation,publishers=publishers,scope=scope,limits=LIMITS)


def verify(root,record,expected):
    assert record['schema']==SCHEMA and record['disposition']=='functional-pass'
    assert derive(root,checked(root,record['sealed_evidence']),expected,record['case'],record['original_native_classes'])==record
    return True


def main():
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('--generation',required=True);parser.add_argument('--plan-only',action='store_true');args=parser.parse_args()
    assert re.fullmatch(r'build[0-9]+',args.generation)
    out=ROOT/'output/release-readiness';backlog=json.loads((out/'coverage-backlog.json').read_text())
    plan=json.loads((ROOT/'build/release-readiness/focused-native-plan-02.json').read_text());jobs=[v for v in plan['jobs']if v['family']=='mi_multiblock']
    assert len(jobs)==2 and {v['target']for v in jobs}==set(TARGETS);jobs={v['target']:v for v in jobs}
    rows=[r for r in backlog['records']if r['case']in CASES]
    assert len(rows)==6 and {(r['target'],r['case'])for r in rows}=={(t,c)for t in TARGETS for c in CASES}
    if args.plan_only:
        for row in rows:print(row['target'],row['case'],'requires actual sealed',jobs[row['target']]['run'])
        print('Plan only; no multiblock/recipe or quarry-output acceptance.');return
    sys.path.insert(0,str(ROOT/'functional-tests/project-everything-regression'))
    import hashlib,importlib.util
    helper=ROOT/'build/release-readiness/release_queue_history_455.py'
    assert hashlib.sha256(helper.read_bytes()).hexdigest()=='fb00a5e47131d9ca30263154439a17f13ea3661eef2651febe9a9fa75fa3affc'
    sp=importlib.util.spec_from_file_location('MI_original_native_history',helper);history=importlib.util.module_from_spec(sp);sp.loader.exec_module(history)
    resolved=history.completed(plan,json.loads((out/'focused-native-campaign-02.json').read_text()));assert len(resolved)==174
    jobs={v['target']:v for v in resolved if v['family']=='mi_multiblock'};assert set(jobs)==set(TARGETS)
    from subject_artifacts import current_subjects
    from release_candidate_identity import current
    from focused_release_evidence_core_log_consumer import verified
    candidate=current(ROOT,verify_worktree=True);records=[]
    consumer=verified(ROOT,args.generation,candidate)
    for row in rows:
        record=derive(ROOT,out/('focused-evidence-'+jobs[row['target']]['run']+'.json'),consumer.expected(row['target'],json.loads((out/('focused-evidence-'+jobs[row['target']]['run']+'.json')).read_text()),current_subjects(row['target'])),row['case'],row.get('historical_native_classes',row['native_classes']))
        assert row['current_exact_pins']and {v['sha512']for v in row['current_exact_pins']}<={v['sha512']for v in record['publishers']},'Unqualified native MI publisher patch'
        records.append(record)
    destination=out/f'coverage-mi-multiblock-{args.generation}-01.json';assert not destination.exists()
    destination.write_text(json.dumps(dict(status='reviewed-native-mi-multiblock-workflows-reconciled',created=datetime.datetime.now(datetime.timezone.utc).isoformat(),
        candidate_identity=candidate,artifact_equivalence=consumer.report(),auditor_inputs=[reference(ROOT,ROOT/'tools/mi_owned_geometry.py'),reference(ROOT,helper),reference(ROOT,Path(__file__)),reference(ROOT,ROOT/'tools/reconcile-mi-storage-release-evidence.py'),reference(ROOT,ROOT/'tools/reconcile-mi-infrastructure-release-evidence.py')],records=records,limits=LIMITS),indent=2)+'\n')
    print('Reconciled6controllerUIrecords from two actual workflows; quarry completion is not qualified.')


if __name__=='__main__':main()
