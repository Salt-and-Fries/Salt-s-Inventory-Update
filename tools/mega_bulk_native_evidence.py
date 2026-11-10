"""Reconstruct native compression-cutoff transitions and complete physical ownership."""
from pathlib import Path
import copy,inspect,json,textwrap
from focused_release_evidence import checked,reference,verify
import mega_bulk_features as native
from ae2_settings_features import item_cell,marker_item,stack
from ae2_quantum_features import expected_item

def validate(journal,frames,closed,driver_hash):
    assert journal['complete']is True and journal['cases']==['bulk-cell-compression-cutoff']and journal['driver_sha256']==driver_hash
    labels=['ordinary-native-handheld-entry','actual-empty-bulk-cell','actual-nugget-ghost','real-compression-card-reveals-native-control',
            'left-decrement-to-ingot','left-decrement-to-nugget','actual-cutoff-moved','right-increment-to-ingot','right-increment-to-block',
            'right-wrap-to-nugget','left-wrap-with-native-negative-sentinel','source-protection-after-native-cutoff-change',
            'ordinary-source-reopened','real-card-recovered','actual-filtered-cell-recovered']
    assert [label for label,_ in frames]==labels
    names=['real original handheld entry preserves exact declared full typed prerequisites','actual empty bulk cell has one native partition and no invented stored contents',
           'native single ghost copies ordinary nugget key while all five real nuggets stay held','actual compression card reveals original item-backed cutoff with default native block variant']
    cycle=lambda n:'original '+n+' persists exact native cutoff and item icon with all resources'
    names += [cycle(labels[4]),cycle(labels[5]),'native cutoff and ghost follow actual window movement']+[cycle(n)for n in labels[7:11]]
    names += ['complete marked source stays protected after legitimate nested native changes','ordinary reopening retains original cutoff sentinel filter and physical compression card',
              'native card removal hides conditional control and recovers exact marked upgrade','exact fully configured bulk cell recovers and native clear mode releases source ghost ownership',
              'last close leaves complete exact final resources no physical duplicates and no shared cursor']
    assert len(names)==16 and [(v['case'],v['name'],v['passed'])for v in journal['checks']]==[('bulk-cell-compression-cutoff',n,True)for n in names]
    # The pinned BulkCellItem deliberately returns null; preserve the ownership
    # algorithm while correcting that single original test assumption locally.
    source=textwrap.dedent(inspect.getsource(native.owned));old="native['fuzzyMode']!='IGNORE_ALL'";assert source.count(old)==1
    namespace=dict(native.__dict__);exec(compile(source.replace(old,"native['fuzzyMode']!='null'",1),__file__,'exec'),namespace);owned=namespace['owned']
    initial=copy.deepcopy(frames[0][1]['ledger']['inventory']);inventory=copy.deepcopy(initial)
    assert len(initial)>=36 and all(v['index']==i for i,v in enumerate(initial))
    for i,item,count,tag in [(0,'megacells:portable_cell_workbench',1,native.SOURCE),(9,'megacells:bulk_item_cell',1,native.CELL),(11,'megacells:compression_card',1,native.UPGRADE)]:
        assert stack(initial[i]['signature'],initial[i]['count'])==stack(expected_item('1.21.1',item,count,tag))
    assert stack(initial[10]['signature'],initial[10]['count'])==stack(native.plain('iron_nugget',5))and stack(initial[12]['signature'],initial[12]['count'])==stack(marker_item('1.21.1'))
    assert all(v['count']==0 for i,v in enumerate(initial)if i not in [0,9,10,11,12])
    cutoffs=[None,None,None,None,1,0,0,1,2,0,-1,-1,-1,-1,None];clients=[]
    for i,(_,frame)in enumerate(frames):
        assert set(frame)=={'native','ledger'}and len(frame['native']['client'])==len(frame['native']['server'])==1
        c=frame['native']['client'][0];s=frame['native']['server'][0];a=c['native'];b=s['native']
        assert c['session']==s['session']and c['menu']==s['menu']and c['screen']=='gripe._90.megacells.client.screen.PortableCellWorkbenchScreen'
        assert a['copyMode']==b['copyMode']=='CLEAR_ON_REMOVE'and a['fuzzyMode']==b['fuzzyMode']and a['bulk']==b['bulk']
        inv=copy.deepcopy(initial);cell=initial[9]if 1<=i<=13 else None;key=initial[10]if 2<=i<=13 else None;upgrades=[initial[11]]if 3<=i<=12 else []
        if 1<=i<=13:inv[9]['count']=0
        if upgrades:inv[11]['count']=0
        if i==2:inv[10]['count']=0
        if i==14:
            recovered=frames[13][1]['native']['server'][0]['native']['cell'];assert native.shell(recovered,native.CELL_FIELDS)==native.shell(initial[9],native.CELL_FIELDS);inv[9]=recovered
        assert native.configuration(a['config'],key)and owned(frame['ledger'],b,inv,initial[0],cell,upgrades,key,cutoffs[i],initial[10]if i==2 else None)
        buttons=[v for v in c['widgets']if v['type']==native.BUTTON];assert len(buttons)==1;button=buttons[0]
        assert button['visible']is(3<=i<=12)
        if button['visible']:
            assert button['active']is True;logical=2 if cutoffs[i]is None or cutoffs[i]<0 else min(cutoffs[i],2)
            assert item_cell(button['itemIcon'])==item_cell(b['bulk']['variants'][logical]['item'])
        clients.append(c)
    assert(clients[6]['bounds']['x'],clients[6]['bounds']['y'])==(clients[5]['bounds']['x']+16,clients[5]['bounds']['y']+12)
    assert clients[12]['session']!=clients[11]['session']
    assert not closed['menus']and not native.counter(closed['physical'].values())and closed['cursor']['count']==0
    assert [item_cell(v)for v in closed['inventory']]==[item_cell(v)for v in frames[-1][1]['ledger']['inventory']]
    return dict(assertions=16,states=len(frames)+1,native_cutoffs=[None,1,0,1,2,0,-1],contract='native-compression-cutoff-both-buttons-wraps-null-fuzzy-and-typed-ownership')

def derive(root,seal_path,expected):
    root=Path(root).resolve();seal_path=Path(seal_path);seal=json.loads(seal_path.read_text());verify(root,seal,expected)
    summary=json.loads(checked(root,seal['summary']).read_text());row=summary['results'][0]
    assert seal['target']=='1.21.1/neoforge'and summary['status']==row['status']=='passed'and row['exit_code']==0
    assert row['results']['mega_bulk_features_checked']is True and(row['results']['checks'],row['results']['cases'],row['results']['failed'])==(16,1,0)
    path=checked(root,seal['acceptance']);assert path==(root/row['profile']/'mega-bulk-release/summary.json').resolve();refs=[];frames=[]
    for i,p in enumerate(sorted(path.parent.glob('[0-9][0-9][0-9]-*.json')),1):
        assert p.name.startswith(str(i).zfill(3)+'-');ref=reference(root,p);assert ref in seal['full_states'];refs.append(ref);frames.append((p.name[4:-5],json.loads(p.read_text())))
    p=path.parent/'closed.json';ref=reference(root,p);assert ref in seal['full_states'];refs.append(ref)
    result=validate(json.loads(path.read_text()),frames,json.loads(p.read_text()),seal['archived_inputs']['functional-tests/release-specialized/mega_bulk_features.py']);assert len(refs)==len(seal['full_states'])==result['states']
    return dict(status='native-bulk16checks-and-typed-ledgers-qualified',target=seal['target'],subjects={k:list(v)for k,v in expected.items()},seal=reference(root,seal_path),acceptance=reference(root,path),native_states=refs,validation=result,scope='Actual MEGA4.11 workbench bulk cell and compression card, native conditional icon, both mouse directions/wraps, exact null unsupported fuzzy mode, source protection, movement/reopening and full resource recovery. No bulk throughput or every-upgrade claim.')
