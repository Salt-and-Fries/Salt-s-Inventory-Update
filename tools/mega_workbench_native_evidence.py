"""Reconstruct actual portable-workbench ownership and native setting transitions."""
from pathlib import Path
import ast,copy,json
from focused_release_evidence import checked,reference,verify
import mega_workbench_features as native
from ae2_settings_features import item_cell,marker_item,stack
from ae2_quantum_features import expected_item

def validate(journal,frames,closed,driver_hash):
    source=Path(native.__file__);tree=ast.parse(source.read_text())
    names=[v.args[0].value for v in ast.walk(tree)if isinstance(v,ast.Call)and isinstance(v.func,ast.Attribute)and v.func.attr=='check'and isinstance(v.args[0],ast.Constant)]
    assert len(names)==19
    assert journal['complete']is True and journal['cases']==['portable-cell-workbench']and journal['driver_sha256']==driver_hash
    assert [(v['case'],v['name'],v['passed'])for v in journal['checks']]==[('portable-cell-workbench',n,True)for n in names]
    first=frames[0][1];initial=copy.deepcopy(first['ledger']['inventory']);inventory=copy.deepcopy(initial)
    assert len(inventory)>=36 and all(v['index']==i for i,v in enumerate(inventory))
    for i,item,count,tag in [(0,'megacells:portable_cell_workbench',1,native.SOURCE),(9,'ae2:item_storage_cell_1k',1,native.CELL),(10,'minecraft:stone',3,native.TEMPLATE),(11,'ae2:fuzzy_card',1,native.UPGRADE)]:
        assert stack(initial[i]['signature'],initial[i]['count'])==stack(expected_item('1.21.1',item,count,tag))
    assert stack(initial[12]['signature'],initial[12]['count'])==stack(marker_item('1.21.1'))
    assert all(v['count']==0 for i,v in enumerate(initial)if i not in [0,9,10,11,12])
    cell=None;upgrades=[];key=None;expected=[]
    def frame(label,cursor=None,mode='CLEAR_ON_REMOVE'):
        expected.append((label,copy.deepcopy(inventory),copy.deepcopy(cell),copy.deepcopy(upgrades),copy.deepcopy(key),copy.deepcopy(cursor),mode))
    frame('original-handheld-entry');inventory[12]['count']=0;frame('refused-real-diamond',initial[12]);inventory[12]=copy.deepcopy(initial[12])
    inventory[9]['count']=0;cell=initial[9];frame('real-marked-cell-inserted');key=initial[10]
    inventory[10]['count']=0;frame('typed-ghost-copied',initial[10]);inventory[10]=copy.deepcopy(initial[10])
    for name in ['template-restored','source-pickup-refused','workbench-moved']:frame(name)
    frame('native-copy-keep',mode='KEEP_ON_REMOVE');frame('native-copy-clear')
    inventory[11]['count']=0;upgrades=[initial[11]];frame('physical-fuzzy-upgrade');frame('native-fuzzy-changed')
    for i in range(4):frame('native-fuzzy-cycle-'+str(i))
    frame('original-source-reopened');key=None;frame('native-filter-clear');key=initial[10];frame('partition-template-restored');key=None;frame('native-empty-cell-partition')
    inventory[11]=copy.deepcopy(initial[11]);upgrades=[];frame('actual-upgrade-recovered')
    actual=dict(frames)['actual-cell-recovered']['native']['server'][0]['native']['cell']
    # The just-removed physical cell is supplied by its preceding authoritative state.
    before=dict(frames)['actual-upgrade-recovered']['native']['server'][0]['native']['cell']
    assert native.shell(before,native.CELL_FIELDS)==native.shell(initial[9],native.CELL_FIELDS)
    inventory[9]=copy.deepcopy(before);cell=None;frame('actual-cell-recovered')
    assert actual['count']==0 and [label for label,_ in frames]==[v[0]for v in expected]
    clients={};fuzzy={}
    for (label,value),(_,inv,wanted_cell,wanted_upgrades,wanted_key,cursor,mode)in zip(frames,expected):
        assert set(value)=={'native','ledger'}and len(value['native']['client'])==len(value['native']['server'])==1
        c=value['native']['client'][0];s=value['native']['server'][0];a=c['native'];b=s['native']
        assert c['session']==s['session']and c['menu']==s['menu']and c['screen']=='gripe._90.megacells.client.screen.PortableCellWorkbenchScreen'and len(c['slots'])==72
        assert a['copyMode']==b['copyMode']==mode and a['fuzzyMode']==b['fuzzyMode']and native.configuration(a['config'],wanted_key)
        assert native.owned(value['ledger'],b,inv,initial[0],wanted_cell,wanted_upgrades,wanted_key,cursor)
        clients[label]=c;fuzzy[label]=b['fuzzyMode']
    before=clients['source-pickup-refused'];after=clients['workbench-moved']
    assert(after['bounds']['x'],after['bounds']['y'])==(before['bounds']['x']+16,before['bounds']['y']+12)and after['session']==before['session']
    assert clients['original-source-reopened']['session']!=before['session']
    labels=['physical-fuzzy-upgrade','native-fuzzy-changed']+['native-fuzzy-cycle-'+str(i)for i in range(3)]
    assert {fuzzy[n]for n in labels}=={'IGNORE_ALL','PERCENT_99','PERCENT_75','PERCENT_50','PERCENT_25'}
    assert fuzzy['native-fuzzy-cycle-3']==fuzzy['original-source-reopened']==fuzzy['physical-fuzzy-upgrade']
    final=frames[-1][1]['ledger'];assert not closed['menus']and not native.counter(closed['physical'].values())and closed['cursor']['count']==0
    assert [item_cell(v)for v in closed['inventory']]==[item_cell(v)for v in final['inventory']]
    return dict(assertions=19,states=len(frames)+1,ghost_cells=63,fuzzy_states=5,contract='native-workbench-typed-components-callbacks-source-lock-and-resource-recovery')

def derive(root,seal_path,expected):
    root=Path(root).resolve();seal_path=Path(seal_path);sealed=json.loads(seal_path.read_text());verify(root,sealed,expected)
    summary=json.loads(checked(root,sealed['summary']).read_text());row=summary['results'][0]
    assert summary['target']=='1.21.1/neoforge'and summary['status']==row['status']=='passed'and row['exit_code']==0
    assert row['results']['mega_workbench_features_checked']is True and(row['results']['checks'],row['results']['cases'],row['results']['failed'])==(19,1,0)
    path=checked(root,sealed['acceptance']);assert path==(root/row['profile']/'mega-workbench-release/summary.json').resolve()
    refs=[];frames=[]
    for i,p in enumerate(sorted(path.parent.glob('[0-9][0-9][0-9]-*.json')),1):
        assert p.name.startswith(str(i).zfill(3)+'-');ref=reference(root,p);assert ref in sealed['full_states'];refs.append(ref);frames.append((p.name[4:-5],json.loads(p.read_text())))
    p=path.parent/'closed.json';ref=reference(root,p);assert ref in sealed['full_states'];refs.append(ref)
    result=validate(json.loads(path.read_text()),frames,json.loads(p.read_text()),sealed['archived_inputs']['functional-tests/release-specialized/mega_workbench_features.py'])
    assert len(refs)==len(sealed['full_states'])==result['states']
    return dict(status='native-workbench19checks-and-typed-ledgers-qualified',target=sealed['target'],subjects={k:list(v)for k,v in expected.items()},seal=reference(root,seal_path),acceptance=reference(root,path),native_states=refs,validation=result,scope='Actual handheld workbench, physical cell/fuzzy upgrade,63ghost cells, copy/clear/partition/fuzzy controls, source protection, movement/reopening and complete resource recovery. No bulk compression or every-cell/upgrade claim.')
