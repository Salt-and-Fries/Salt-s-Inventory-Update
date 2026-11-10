"""Review original AE2 priority/emitter controls and native server-backed anchor overlay.

Read actual sealed frames and complete typed item ledgers. Native overlay is a server-backed config setting; toggling it is not chunk-loading endurance.
"""
from pathlib import Path
import argparse,datetime,hashlib,json,re,sys
import nbtlib

ROOT=Path(__file__).resolve().parents[1]
sys.path[:0]=[str(ROOT/'tools'),str(ROOT/'functional-tests/release-specialized')]
from focused_release_evidence import checked,reference,verify as verify_sealed
from native_publisher_index import index,matches
from focused_release_evidence_ae2_variants import additional_jobs,combine,expected_subjects
from ae2_settings_features import CASES,STATS,ledger,marker_item,pair,number
from mowzie_features import stack

TARGETS=['1.20.1/fabric','1.20.1/forge','1.21.1/neoforge','26.1.2/neoforge','26.2/neoforge','26.3/neoforge']
ALIASES={'ae2-priority':'priority','atlas-ae2-spatial-anchor':'spatial-anchor','atlas-ae2-energy-level-emitter':'energy-level-emitter'}
MENUS={name:'appeng.menu.implementations.'+simple for name,simple in [('priority','PriorityMenu'),('spatial-anchor','SpatialAnchorMenu'),('energy-level-emitter','EnergyLevelEmitterMenu')]}
PARENTS=dict(MENUS,priority='appeng.menu.implementations.StorageBusMenu')
DRIVER='functional-tests/release-specialized/ae2_settings_features.py'
SCHEMA='ae2-native-settings-and-server-overlay-v2'
FRAMES={
    'priority':['native-parent','priority-child','priority41','priority-moved','priority37','storage-bus-reopened','priority-reopened'],
    'spatial-anchor':['native-parent','anchor-overlay','anchor-moved','anchor-overlay-return','anchor-reopened'],
    'energy-level-emitter':['native-parent','emitter41','emitter-mode','emitter-moved','emitter42','emitter-reopened']}
NAMES={
    'priority':['native priority child observes its actual host','ordinary numeric input sets exact native priority41',
        'priority moves through original drag input','moved numeric input sets exact backing host priority37',
        'native priority persists after ordinary parent reopening'],
    'spatial-anchor':['actual populated native anchor statistics synchronize',
        'original overlay toggle updates and synchronizes the native server setting','anchor moves before native overlay input',
        'moved overlay input restores the original native server value','ordinary anchor reopening preserves restored server setting and real statistics'],
    'energy-level-emitter':['native emitter initial threshold and mode synchronize',
        'ordinary numeric input sets exact native energy threshold41','native redstone-mode button changes the original server setting',
        'emitter moves through original drag input','moved emitter input retains native mode and exact threshold42',
        'native emitter threshold and mode persist after ordinary reopening']}
LIMITS=('Exact native target and staged publisher only. Numeric settings/host persistence and native server-backed overlay '
    'are UI contracts. No arbitrary redstone circuit, chunk-loading endurance, crafting, unrelated addon or whole-AE2 '
    'qualification. Final close retains its original assertion rather than an invented retained closing snapshot.')


def specification(target,case,classes):
    assert target in TARGETS and case in ALIASES
    name=ALIASES[case]
    assert classes==(['appeng.client.gui.implementations.PriorityScreen']if name=='priority'else[MENUS[name]]),'Unreviewed original native alias/class'
    return name,'functional-pass'


def enum(row,key,java_type):
    value=row['values'][key]
    assert value['javaType']==java_type and isinstance(value['value'],str)and value['value']
    return value


def controls(client,name,wanted):
    widgets=client['widgets']
    assert all(isinstance(w.get('type'),str)and type(w.get('visible'))is bool and type(w.get('active'))is bool for w in widgets)
    visible=[w for w in widgets if w['visible']and w['active']]
    if wanted==PARENTS['priority']:
        assert sum(w['type'].endswith('.TabButton')and(w.get('message')=='Priority'or'Priority'in w.get('tooltips',[]))for w in visible)==1
    elif name in{'priority','energy-level-emitter'}:
        assert sum('value'in w and isinstance(w['value'],str)for w in visible)==1,'Missing actual native numeric input'
    if name in{'spatial-anchor','energy-level-emitter'}:
        key='getOverlayMode'if name=='spatial-anchor'else'getRedStoneMode'
        assert sum(w.get('currentValue')==client['values'][key]['value']for w in visible)==1,'Missing original native mode control'


def frame(target,name,label,value,entry):
    assert set(value)=={'native','ledger'}
    native,data=value['native'],value['ledger']
    wanted=PARENTS[name]if label in{'native-parent','storage-bus-reopened'}else MENUS[name]
    client,server=pair(native,wanted.rsplit('.',1)[-1])
    assert client['menu']==server['menu']==wanted
    assert type(client['session'])is int and client['session']>0 and server['session']==client['session']
    assert client['screen']==('appeng.client.gui.implementations.PriorityScreen'if wanted==MENUS['priority']else entry['original_capture']['expected_native_screen'])
    bounds=client['bounds'];assert all(type(bounds[k])is int for k in ['x','y','width','height'])and bounds['width']>0 and bounds['height']>0
    assert type(client['locked'])is bool and isinstance(client['widgets'],list)
    controls(client,name,wanted)
    inventory=data['inventory'];assert isinstance(inventory,list)and len(inventory)>=36
    assert all(type(v['index'])is int and v['index']==i for i,v in enumerate(inventory))
    marker=inventory[12];assert stack(marker['signature'],marker['count'])==stack(marker_item(target.split('/')[0]))
    assert data['cursor']['count']==0 and type(data['cursor']['count'])is int
    menus=[v for v in data['menus']if v['session']==client['session']and v['type']==wanted]
    assert len(menus)==1,'Actual owned native menu missing from the full ledger'
    return client,server,ledger(data)


def moved(before,after):
    assert after['bounds']['x']==before['bounds']['x']+16 and after['bounds']['y']==before['bounds']['y']+12


def numeric(client,server,key,wanted,java_type,host=False):
    assert number(client,key,wanted,java_type) and number(server,'nativeHostReportingValue'if key=='getReportingValue'else key,wanted,java_type)
    if host:assert number(server,'nativeHostPriority',wanted,java_type)
    assert [w['value']for w in client['widgets']if 'value'in w and w['visible']and w['active']]==[str(wanted)]


def validate(target,journal,states,driver_hash):
    assert target in TARGETS and journal['complete']is True and journal['driver_sha256']==driver_hash
    assert journal['cases']==CASES and set(states)==set(CASES)
    entries=journal['original_entries'];assert isinstance(entries,list)and [v['case']for v in entries]==CASES
    expected=[];native_classes=set()
    for name,entry in zip(CASES,entries):
        original=entry['original_capture']
        assert (original['minecraft'],original['loader'])==tuple(target.split('/'))and original['expected_native_menu']==PARENTS[name]
        assert isinstance(original['expected_native_screen'],str)and original['expected_native_screen'].startswith('appeng.client.gui.')
        actions=entry['executed_actions'];assert isinstance(actions,list)and actions
        assert not any(v['type']in{'provider','profile','native-template-probe'}for v in actions),'Forced native entry prohibited'
        assert [v[0]for v in states[name]]==FRAMES[name]
        observed=[frame(target,name,label,v,entry)for label,v in states[name]]
        assert all(row[2]==observed[0][2]for row in observed),'Full physical/typed inventory or cursor changed'
        expected.append((name,'exact native parent and typed pre-entry inventory'))
        expected.extend((name,label)for label in NAMES[name])
        native_classes.add(original['expected_native_screen']);native_classes.update([PARENTS[name],MENUS[name]])
        if name=='priority':
            native_classes.add('appeng.client.gui.implementations.PriorityScreen')
            c,s,_=observed[1];assert c['values']['getPriorityValue']==s['values']['getPriorityValue']==s['values']['nativeHostPriority']
            assert number(c,'getPriorityValue',c['values']['getPriorityValue']['value'],'java.lang.Integer')
            for i,value in [(2,41),(3,41),(4,37),(6,37)]:numeric(*observed[i][:2],'getPriorityValue',value,'java.lang.Integer',True)
            moved(observed[2][0],observed[3][0])
        elif name=='spatial-anchor':
            c,s,_=observed[0];original_client=enum(c,'getOverlayMode','appeng.api.config.YesNo');original_server=enum(s,'getOverlayMode','appeng.api.config.YesNo')
            assert original_client['value']in{'YES','NO'}and original_server==original_client
            for c,s,_ in observed:
                assert all(c['values'][key]==s['values'][key]and number(c,key,c['values'][key]['value'],'java.lang.Long'if key=='powerConsumption'else'java.lang.Integer')
                    and c['values'][key]['value']>=0 for key in STATS)
                assert c['values']['allChunks']['value']>0
                assert enum(s,'getOverlayMode','appeng.api.config.YesNo')==enum(c,'getOverlayMode','appeng.api.config.YesNo')
            changed=enum(observed[1][0],'getOverlayMode','appeng.api.config.YesNo')
            assert changed['value']in{'YES','NO'}and changed!=original_client
            assert enum(observed[2][0],'getOverlayMode','appeng.api.config.YesNo')==changed
            assert all(enum(observed[i][0],'getOverlayMode','appeng.api.config.YesNo')==original_client for i in [3,4])
            moved(observed[1][0],observed[2][0])
        else:
            c,s,_=observed[0]
            assert c['values']['getReportingValue']==s['values']['nativeHostReportingValue'] and c['values']['getRedStoneMode']==s['values']['getRedStoneMode']
            original_mode=enum(s,'getRedStoneMode','appeng.api.config.RedstoneMode')
            numeric(*observed[1][:2],'getReportingValue',41,'java.lang.Long')
            changed=enum(observed[2][1],'getRedStoneMode','appeng.api.config.RedstoneMode');assert changed!=original_mode
            for i,(c,s,_)in enumerate(observed):
                if i>=2:assert enum(c,'getRedStoneMode','appeng.api.config.RedstoneMode')==enum(s,'getRedStoneMode','appeng.api.config.RedstoneMode')==changed
            moved(observed[2][0],observed[3][0])
            for i in [4,5]:numeric(*observed[i][:2],'getReportingValue',42,'java.lang.Long')
    expected.append((CASES[-1],'all native setting sessions close without changing owned items'))
    assert [(v['case'],v['name'])for v in journal['checks']]==expected and len(expected)==20
    assert all(v.get('passed')is True for v in journal['checks'])
    return dict(assertions=20,states=18,native_variants=3,contract='native-settings-and-server-overlay-not-redstone-or-chunk-endurance'),sorted(native_classes)


def derive(root,sealed_path,expected,case,original_classes):
    root=Path(root).resolve();sealed=json.loads(Path(sealed_path).read_text());target=sealed['target']
    name,disposition=specification(target,case,original_classes);verify_sealed(root,sealed,expected)
    summary=json.loads(checked(root,sealed['summary']).read_text());row=summary['results'][0];result=row['results']
    assert result.get('ae2_settings_features_checked')is True
    assert type(result['cases'])is int and result['cases']==3 and type(result['failed'])is int and result['failed']==0
    assert type(result['checks'])is int and result['checks']==20
    profile=(root/row['profile']).resolve();journal_path=checked(root,sealed['acceptance']);assert journal_path==profile/'ae2-settings-release/summary.json'
    states={v:[]for v in CASES};refs=[]
    for number,path in enumerate(sorted(journal_path.parent.glob('[0-9][0-9][0-9]-*.json')),1):
        match=re.fullmatch(r'([0-9]{3})-(priority|spatial-anchor|energy-level-emitter)-([a-z0-9-]+)\.json',path.name)
        assert match and int(match[1])==number
        ref=reference(root,path);assert ref in sealed['full_states'];refs.append(ref)
        states[match[2]].append((match[3],json.loads(path.read_text())))
    assert len(refs)==18
    validation,classes=validate(target,json.loads(journal_path.read_text()),states,sealed['archived_inputs'][DRIVER])
    publishers=[]
    for artifact in json.loads(checked(root,sealed['staged']).read_text())['artifacts']:
        if artifact['family']!='upstream':continue
        path=Path(artifact['file']);path=path if path.is_absolute()else root/path;raw=path.read_bytes()
        assert hashlib.sha256(raw).hexdigest()==artifact['sha256'];digest=hashlib.sha512(raw).hexdigest()
        if 'sha512'in artifact:assert artifact['sha512']==digest
        locations=matches(index(path,digest,target.split('/')[1]),classes)
        if not locations:continue
        assert {v[:-6].replace('/','.')for loc in locations for v in loc['classes']}==set(classes),'Split or incomplete native AE2 publisher'
        publishers.append(dict(filename=artifact['filename'],sha256=artifact['sha256'],sha512=digest,class_locations=locations))
    assert len(publishers)==1,'One exact original AE2 publisher required'
    scope={'priority':'Original priority child numeric41/37, native host backing value, moved input and ordinary parent/child reopening with full typed item conservation.',
        'energy-level-emitter':'Original emitter Long threshold41/42 and native redstone-mode control, moved input and persisted actual native host threshold plus menu mode with full typed item conservation.',
        'spatial-anchor':'Populated real native anchor statistics, original server-backed overlay toggle/restore, moved input and restored native server value after reopening; actual client/server values must synchronize.'}[name]
    return dict(schema=SCHEMA,target=target,case=case,disposition=disposition,original_native_classes=original_classes,native_classes=classes,
        subjects={k:list(v)for k,v in expected.items()},sealed_evidence=reference(root,sealed_path),acceptance=reference(root,journal_path),native_states=refs,
        validation=validation,publishers=publishers,scope=scope,limits=LIMITS)


def verify(root,record,expected,consumer=None,current=None):
    assert record['schema']==SCHEMA and record['disposition']==specification(record['target'],record['case'],record['original_native_classes'])[1]
    actual=[derive(root,checked(root,v['sealed_evidence']),expected_subjects(record,v,expected,consumer,current),record['case'],record['original_native_classes'])for v in[record,*record.get('additional_workflows',[])]]
    assert combine(actual,consumer=consumer,current=current)==record
    return True


def main():
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('--generation',required=True);parser.add_argument('--plan-only',action='store_true');args=parser.parse_args()
    assert re.fullmatch(r'build[0-9]+',args.generation)
    out=ROOT/'output/release-readiness';backlog=json.loads((out/'coverage-backlog.json').read_text())
    plan=json.loads((ROOT/'build/release-readiness/focused-native-plan-02.json').read_text());jobs=[v for v in plan['jobs']if v['family']=='ae2_settings']
    assert len(jobs)==6 and {v['target']for v in jobs}==set(TARGETS);jobs={v['target']:v for v in jobs}
    rows=[r for r in backlog['records']if r['case']in ALIASES]
    assert len(rows)==13 and len({(r['target'],r['case'])for r in rows})==13
    assert all(sum(r['case']==case for r in rows)==(1 if case=='ae2-priority'else 6)for case in ALIASES)
    if args.plan_only:
        for row in rows:print(row['target'],row['case'],'requires actual sealed',jobs[row['target']]['run'])
        print('Plan only; no native settings/overlay acceptance.');return
    sys.path.insert(0,str(ROOT/'functional-tests/project-everything-regression'))
    import importlib.util
    history_source=ROOT/'build/release-readiness/release_queue_history_455.py'
    assert hashlib.sha256(history_source.read_bytes()).hexdigest()=='fb00a5e47131d9ca30263154439a17f13ea3661eef2651febe9a9fa75fa3affc'
    assert hashlib.sha256((ROOT/'build/release-readiness/focused-native-plan-02.json').read_bytes()).hexdigest()=='9cc23717142dc649b56b8e445e86d1b900652bb580401c8de0a1750f0eb467b9'
    spec=importlib.util.spec_from_file_location('AE2_original_native_history',history_source);history=importlib.util.module_from_spec(spec);spec.loader.exec_module(history)
    resolved=history.completed(plan,json.loads((out/'focused-native-campaign-02.json').read_text()));assert len(resolved)==174
    jobs={v['target']:v for v in resolved if v['family']=='ae2_settings'};assert set(jobs)==set(TARGETS)

    from subject_artifacts import current_subjects
    from release_candidate_identity import current
    from focused_release_evidence_core_log_consumer import verified
    candidate=current(ROOT,verify_worktree=True);records=[]
    consumer=verified(ROOT,args.generation,candidate)
    def accepted(job,row):
        sealed=out/('focused-evidence-'+job['run']+'.json')
        expected=consumer.expected(row['target'],json.loads(sealed.read_text()),current_subjects(row['target']))
        return derive(ROOT,sealed,expected,row['case'],row.get('historical_native_classes',row['native_classes']))

    for row in rows:
        executions=[jobs[row['target']],*additional_jobs(ROOT,'settings',row['target'])]
        workflows=[accepted(job,row)for job in executions]
        record=combine(workflows,consumer=consumer,current=current_subjects(row['target']))
        assert row['current_exact_pins']and {v['sha512']for v in row['current_exact_pins']}<={v['sha512']for v in record['publishers']},'Unqualified native AE2 publisher patch'
        records.append(record)
    destination=out/f'coverage-ae2-settings-{args.generation}-01.json';assert not destination.exists()
    destination.write_text(json.dumps(dict(status='reviewed-native-ae2-settings-workflows-reconciled',created=datetime.datetime.now(datetime.timezone.utc).isoformat(),
        candidate_identity=candidate,artifact_equivalence=consumer.report(),auditor_inputs=[reference(ROOT,history_source),reference(ROOT,Path(__file__)),reference(ROOT,ROOT/'tools/focused_release_evidence_ae2_variants.py')],records=records,limits=LIMITS),indent=2)+'\n')
    print('Reconciled13nativeUIrecords from six actual3-case workflows; native server overlay and actual emitter-host thresholds are checked; no chunk-loading endurance.')


if __name__=='__main__':main()
