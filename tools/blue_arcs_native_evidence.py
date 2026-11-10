"""Reconstruct native Arc capability transitions from sealed full states."""
from pathlib import Path
import copy,json
from focused_release_evidence import checked,reference,verify
from blue_arcs_features import ARCS,owned,counter
from ae2_settings_features import item_cell,stack,marker_item
from ae2_quantum_features import expected_item

NAMES=['actual Character attachment opens original native Arcs screen and empty capability cells',
       'original restricted arc slot refuses full typed unrelated diamonds',
       'actual arc cell0 owns exact marked native capability item',
       'native duplicate rule rejects same arc item even with different complete metadata']
NAMES += ['actual arc cell'+str(i)+' owns exact marked native capability item' for i in range(1,4)]
NAMES += ['original four-cell window moves while retaining exact capability ownership',
          'moved last native slot returns original full typed arc',
          'moved last native slot accepts its real item again',
          'ordinary Character launcher reopening retains all four exact actual capability cells']
NAMES += ['actual capability cell'+str(i)+' empties with full metadata and exact inventory recovery' for i in range(4)]
NAMES += ['Arcs attachment remains reachable after actual Character movement with exact restored inventory',
          'all native Arcs sessions close with exact full inventory and clear cursor']

def validate(journal,frames,closed,driver_hash):
    assert journal['complete']is True and journal['cases']==['blue-skies-arcs']and journal['driver_sha256']==driver_hash
    assert [(v['case'],v['name'],v['passed'])for v in journal['checks']]==[('blue-skies-arcs',name,True)for name in NAMES]and len(NAMES)==17
    first=frames[0][1];inventory=copy.deepcopy(first['ledger']['inventory']);initial=copy.deepcopy(inventory)
    assert len(inventory)>=36 and all(v['index']==i for i,v in enumerate(inventory))
    for i,name in enumerate(ARCS):
        assert stack(inventory[9+i]['signature'],inventory[9+i]['count'])==stack(expected_item('1.20.1','blue_skies:'+name,1,'{ArcLevel:1,release_blue_arc:'+str(i+131)+',typed:[B;2b,9b]}'))
    assert stack(inventory[14]['signature'],inventory[14]['count'])==stack(expected_item('1.20.1','blue_skies:aquatic_arc',1,'{ArcLevel:1,release_blue_duplicate:137L,typed:[B;5b,4b]}'))
    assert stack(inventory[16]['signature'],inventory[16]['count'])==stack(marker_item('1.20.1'))
    assert all(v['count']==0 for i,v in enumerate(inventory)if i not in [9,10,11,12,14,16])
    empty=lambda:dict(count=0,signature='{}')
    equipped=[empty()for _ in range(4)];expected=[]
    def frame(label,cursor=None):expected.append((label,copy.deepcopy(inventory),copy.deepcopy(equipped),copy.deepcopy(cursor)))
    frame('original-launcher-and-screen')
    inventory[16]['count']=0;frame('native-wrong-item-refused',initial[16]);inventory[16]=copy.deepcopy(initial[16])
    for i in range(4):
        inventory[9+i]['count']=0;equipped[i]=copy.deepcopy(initial[9+i]);frame('native-arc-equipped-'+str(i))
        if i==0:
            inventory[14]['count']=0;frame('same-item-different-data-refused',initial[14]);inventory[14]=copy.deepcopy(initial[14])
    frame('native-arcs-window-moved')
    inventory[12]=copy.deepcopy(initial[12]);equipped[3]=empty();frame('moved-final-cell-recovered')
    inventory[12]['count']=0;equipped[3]=copy.deepcopy(initial[12]);frame('moved-final-cell-restored');frame('ordinary-arcs-reopened')
    for i in range(4):
        inventory[9+i]=copy.deepcopy(initial[9+i]);equipped[i]=empty();frame('native-arc-recovered-'+str(i))
    frame('moved-character-launcher')
    assert [label for label,_ in frames]==[row[0]for row in expected]
    clients={}
    for (label,value),(wanted_label,inv,eq,cursor)in zip(frames,expected):
        assert label==wanted_label and set(value)=={'native','ledger'}
        assert owned(value['ledger'],value['native'],inv,eq,cursor)
        c=value['native']['client'][0]
        assert c['screen']=='com.legacy.blue_skies.client.gui.screen.ArcsScreen'and c['menu']=='com.legacy.blue_skies.client.gui.menu.ArcsMenu'
        clients[label]=c
    before=clients['native-arc-equipped-3'];after=clients['native-arcs-window-moved']
    assert after['session']==before['session']and(after['bounds']['x'],after['bounds']['y'])==(before['bounds']['x']+16,before['bounds']['y']+12)
    assert(after['bounds']['width'],after['bounds']['height'])==(before['bounds']['width'],before['bounds']['height'])
    assert clients['ordinary-arcs-reopened']['session']!=before['session']
    assert clients['moved-character-launcher']['session']!=clients['ordinary-arcs-reopened']['session']
    assert not closed['menus']and not counter(closed['physical'].values())and closed['cursor']['count']==0
    assert [item_cell(v)for v in closed['inventory']]==[item_cell(v)for v in initial]
    return dict(assertions=17,states=len(frames)+1,capability_cells=4,contract='native-capability-typed-refusal-duplicate-movement-reopen-recovery')

def derive(root,seal_path,expected):
    root=Path(root).resolve();seal_path=Path(seal_path);sealed=json.loads(seal_path.read_text());verify(root,sealed,expected)
    summary=json.loads(checked(root,sealed['summary']).read_text());row=summary['results'][0]
    assert summary['target']=='1.20.1/forge'and summary['status']==row['status']=='passed'and row['exit_code']==0
    assert row['results']['blue_arcs_features_checked']is True and(row['results']['checks'],row['results']['cases'],row['results']['failed'])==(17,1,0)
    path=checked(root,sealed['acceptance']);assert path==(root/row['profile']/'blue-arcs-release/summary.json').resolve()
    refs=[];frames=[]
    for i,p in enumerate(sorted(path.parent.glob('[0-9][0-9][0-9]-*.json')),1):
        assert p.name.startswith(str(i).zfill(3)+'-');ref=reference(root,p);assert ref in sealed['full_states'];refs.append(ref);frames.append((p.name[4:-5],json.loads(p.read_text())))
    p=path.parent/'closed.json';ref=reference(root,p);assert ref in sealed['full_states'];refs.append(ref)
    result=validate(json.loads(path.read_text()),frames,json.loads(p.read_text()),sealed['archived_inputs']['functional-tests/release-specialized/blue_arcs_features.py'])
    assert len(refs)==len(sealed['full_states'])==result['states']
    return dict(status='native-four-capability-cells17checks-and-typed-ledgers-qualified',target=sealed['target'],subjects={k:list(v)for k,v in expected.items()},seal=reference(root,seal_path),acceptance=reference(root,path),native_states=refs,validation=result,scope='Actual Character launcher and original Blue Skies screen/menu, four capability-backed slots, wrong-item and same-item/different-data refusal, moved input, ordinary reopening and exact recovery. No dungeon progression or Arc gameplay-effect claim.')
