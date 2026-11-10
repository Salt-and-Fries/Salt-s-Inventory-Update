"""Independently rederive the real same-turn Requester checks from sealed native states."""
from pathlib import Path
import argparse,copy,datetime,json,sys,types
R=Path(__file__).resolve().parents[1]
sys.path[:0]=[str(R/'tools'),str(R/'functional-tests/release-specialized')]
from focused_release_evidence import checked,reference,verify
from ae2_crafting_features import Crafting,fixture_pattern,ordinary,stored_matches,CELL
from ae2_settings_features import item_cell,stack,marker_item
from ae2_quantum_features import expected_item
from requester_features import synchronized,matches,no_buffered_items,TEMPLATE

LABELS=['original-provider','original-pattern-inserted','original-drive','real-cell-inserted','original-repository','real-logs-deposited','original-requester','empty-row-disabled','typed-ghost-selected','template-restored','same-turn-native-numbers','burst-window-moved','moved-same-turn-native-numbers','burst-native-reopened']
NAMES=['genuine recipe and exact declared typed requester prerequisites precede measured input','native provider receives real pattern while all requester template items stay intact','real native cell is inserted and the independent marked shell is preserved','normal repository input stores sixteen real logs for later native requests','original block exposes synchronized default native request and visible ghost row','original empty-row StateBox disables request before selection','native ghost selection copies full typed key without consuming held three items','whole physical player inventory returns intact after ghost selection','both ordinary same-turn Enter submissions reach the actual native manager','native Requester burst source moves with exact typed ownership','both moved same-turn Enter submissions reach the same native manager','ordinary reopening retains both final numbers and the full typed ghost key']


def validate(target,journal,frames,probes,seed,driver_hash):
    assert target in ['1.21.1/neoforge','26.1.2/neoforge'];mc=target.split('/')[0]
    assert journal['complete']is True and journal['cases']==['requester-block']and journal['driver_sha256']==driver_hash
    assert [(v['case'],v['name'],v['passed'])for v in journal['checks']]==[('requester-block',name,True)for name in NAMES]
    assert [label for label,_ in frames]==LABELS and len(probes)==2 and len(journal['original_entries'])==1
    assert fixture_pattern(seed['decoded'],mc)
    inventory=frames[0][1]['ledger']['inventory'];assert len(inventory)>=36 and all(v.get('index')==i for i,v in enumerate(inventory))
    pattern=seed['pattern'];assert item_cell(inventory[9])==item_cell(pattern)and pattern['item']=='ae2:crafting_pattern'and pattern['count']==1
    assert stack(inventory[10]['signature'],inventory[10]['count'])==stack(expected_item(mc,'ae2:item_storage_cell_64k',1,CELL))
    assert stack(inventory[11]['signature'],inventory[11]['count'])==stack(ordinary(mc,'minecraft:oak_log',16))
    assert stack(inventory[12]['signature'],inventory[12]['count'])==stack(marker_item(mc))
    assert stack(inventory[14]['signature'],inventory[14]['count'])==stack(expected_item(mc,'minecraft:stone',3,TEMPLATE))
    assert stack(inventory[15]['signature'],inventory[15]['count'])==stack(ordinary(mc,'minecraft:oak_planks',4))
    template=inventory[14];clients=[]
    for i,(_,frame)in enumerate(frames):
        native=frame['native']if i<6 else frame['network'];ledger=frame['ledger'];wanted=copy.deepcopy(inventory)
        for index,from_frame in [(9,1),(10,3),(11,5),(14,8)]:
            if i>=from_frame and(index!=14 or i==8):wanted[index]['count']=0
        owner=types.SimpleNamespace(mc=mc,native=native,pattern=pattern,cell=inventory[10])
        assert Crafting.owned(owner,ledger,wanted,cursor=template if i==8 else None,provider=i>=1,cell=i>=3),'Complete typed native ownership mismatch'
        assert native['powered']is True and stored_matches(native['stored'],mc,16 if i>=5 else 0,0)
        assert len(native['cpus'])==1 and native['cpus'][0]['busy']is False and native['cpus'][0]['active']is False and native['cpus'][0]['bytes']>=65536
        if i<6:continue
        state=frame['requester'];rows=[v for v in state['client']if v['menu']=='com.almostreliable.merequester.requester.RequesterMenu'];assert len(rows)==1;client=rows[0]
        assert client['screen']=='com.almostreliable.merequester.client.RequesterScreen'
        server=[v for v in state['server']if(v['session'],v['menu'])==(client['session'],client['menu'])];assert len(server)==1 and synchronized(state,client,server[0])and no_buffered_items(state)
        amount,batch=(0,1)if i<=7 else(3,1)if i<=9 else(37,8)if i<=11 else(41,9)
        assert matches(state,None if i<=7 else template,amount,batch,i==6)
        widgets=[v for v in client['rows']if v['request'].get('empty')is False and v['request']['index']==0 and v['controls']['stateBox']['visible']is True];assert len(widgets)==1
        controls=widgets[0]['controls'];assert controls['stateBox']['selected']is(i==6)
        for name,value in [('amountField',amount),('batchField',batch)]:assert controls[name]['value']==str(value)and controls[name]['visible']is True and controls[name]['active']is True
        ghost=[v for v in client['slots']if v['index']==0 and v['requesterId']==client['references'][0]['id']and v['visible']is True];assert len(ghost)==1
        assert item_cell(ghost[0]['item'])==((0,None)if i<=7 else item_cell(dict(template,count=1)))
        clients.append(client)
    before,after=clients[4],clients[5]
    assert(after['bounds']['x'],after['bounds']['y'])==(before['bounds']['x']+16,before['bounds']['y']+12)
    assert clients[-1]['session']!=clients[-2]['session']
    seen=[]
    for probe in probes:
        values=[e['text']for e in probe['events']if e['type']=='text'];assert values in [['37','8'],['41','9']];seen.append(values)
        source=clients[3]if values==['37','8']else clients[5];controls=next(v for v in source['rows']if v['request'].get('empty')is False and v['request']['index']==0)['controls'];events=[]
        for name,text in zip(['amountField','batchField'],values):
            box=controls[name];events.append(dict(type='regression-tap',session=source['session'],x=box['x']+box['width']/2,y=box['y']+box['height']/2));events.append(dict(type='key',key=269));events.extend(dict(type='key',key=259)for _ in box['value']);events.extend([dict(type='text',text=text),dict(type='key',key=257)])
        assert probe['events']==events and len(probe['observed'])==len(events)
        assert all(v['event']==event['type']and type(v['pending'])is int and v['pending']>=0 and type(v['busy'])is bool for v,event in zip(probe['observed'],events))
        lane=probe['afterBurst'];assert type(lane['pending'])is int and lane['pending']>=1 and lane['busy']is True and lane['managerCloseAvailable']is False
    assert sorted(seen)==[['37','8'],['41','9']]
    return dict(checks=12,states=16,real_bursts=2,scope='Actual disabled Requester numeric events, full typed resource ownership, movement, protected pending lane and ordinary persistence; no terminal search or recipe production claim')


def derive(root,seal_path,expected):
    root=Path(root).resolve();seal=json.loads(Path(seal_path).read_text());verify(root,seal,expected)
    summary=json.loads(checked(root,seal['summary']).read_text());assert summary['status']=='passed'and summary['results'][0]['results']['requester_burst_features_checked']is True
    acceptance=checked(root,seal['acceptance']);journal=json.loads(acceptance.read_text());folder=acceptance.parent;frames=[];refs=[]
    for number,p in enumerate(sorted(folder.glob('[0-9][0-9][0-9]-*.json')),1):
        assert p.name.startswith(str(number).zfill(3)+'-');ref=reference(root,p);assert ref in seal['full_states'];refs.append(ref);frames.append((p.name[4:-5],json.loads(p.read_text())))
    probes=[]
    for p in sorted(folder.glob('transport-burst-*.json')):
        ref=reference(root,p);assert ref in seal['full_states'];refs.append(ref);probes.append(json.loads(p.read_text()))
    assert len(refs)==len(seal['full_states'])==16
    seed=folder.parent/'release-ae2-crafting-fixture.json';validation=validate(seal['target'],journal,frames,probes,json.loads(seed.read_text()),seal['archived_inputs']['functional-tests/release-specialized/requester_features.py'])
    return dict(status='actual-native-Requester-owned-burst-strictly-qualified',target=seal['target'],subjects={k:list(v)for k,v in expected.items()},seal=reference(root,seal_path),acceptance=reference(root,acceptance),states=refs,native_seed=reference(root,seed),validation=validation)


if __name__=='__main__':
    parser=argparse.ArgumentParser();parser.add_argument('--run',action='append',required=True);parser.add_argument('--id',required=True);args=parser.parse_args();rows=[]
    for run in args.run:
        seal=R/'output/release-readiness'/('focused-evidence-'+run+'.json');data=json.loads(seal.read_text());rows.append(derive(R,seal,data['subjects']))
    out=R/'output/release-readiness'/args.id;out.mkdir();(out/'manifest.json').write_text(json.dumps(dict(status='actual-native-Requester-bursts-strictly-rederived',created=datetime.datetime.now(datetime.timezone.utc).isoformat(),reader=reference(R,Path(__file__)),runs=rows,checks=12*len(rows),full_states=16*len(rows)),indent=2)+'\n');print('PASS strict native Requester:',len(rows),'runs,',12*len(rows),'checks')
