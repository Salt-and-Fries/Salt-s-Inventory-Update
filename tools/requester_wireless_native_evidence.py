"""Qualify original Survival wireless input with native energy and typed recovery."""
from pathlib import Path
import ast,copy,json
from focused_release_evidence import checked,reference,verify
from ae2_settings_features import item_cell,stack
from ae2_crafting_features import cellshell,fixture_pattern,stored_matches,physical_counter
from requester_features import matches,synchronized,no_buffered_items
from requester_wireless_features import ENERGY,SOURCE_TAG,native_energy,power_matches
from requester_wireless_observer import SCREEN,MENU

def original_names(root):
    names=[]
    for name,cls,method in [('requester_features.py','Requester','setup'),('requester_wireless_features.py','Wireless','run')]:
        tree=ast.parse((root/'functional-tests/release-specialized'/name).read_text());owner=next(v for v in tree.body if isinstance(v,ast.ClassDef)and v.name==cls)
        body=next(v for v in owner.body if isinstance(v,ast.FunctionDef)and v.name==method)
        names += [v.args[0].value for v in ast.walk(body)if isinstance(v,ast.Call)and isinstance(v.func,ast.Attribute)and v.func.attr=='check'and v.args and isinstance(v.args[0],ast.Constant)]
    names=[v.replace('wireless impossible search retains original summary but hides row controls only','wireless impossible search hides all unmatched rows and retains exact native models')for v in names]
    assert len(names)==27;return names

def derive(root,seal_path,expected):
    root=Path(root).resolve();seal_path=Path(seal_path);seal=json.loads(seal_path.read_text());verify(root,seal,expected)
    summary=json.loads(checked(root,seal['summary']).read_text());row=summary['results'][0]
    assert seal['target']in {'1.21.1/neoforge','26.1.2/neoforge'}and summary['status']==row['status']=='passed'and row['exit_code']==0
    result=row['results'];assert result['requester_wireless_features_checked']is True and(result['cases'],result['checks'],result['failed'])==(1,27,0)
    path=checked(root,seal['acceptance']);assert path==(root/row['profile']/'requester-wireless-release/summary.json').resolve()
    journal=json.loads(path.read_text());assert journal['complete']is True and journal['cases']==['wireless-requester']
    assert journal['driver_sha256']==seal['archived_inputs']['functional-tests/release-specialized/requester_wireless_features.py']
    assert [(v['case'],v['name'],v['passed'])for v in journal['checks']]==[('wireless-requester',name,True)for name in original_names(root)]
    refs=[];frames=[]
    for i,p in enumerate(sorted(path.parent.glob('[0-9][0-9][0-9]-*.json')),1):
        assert p.name.startswith(str(i).zfill(3)+'-');ref=reference(root,p);assert ref in seal['full_states'];refs.append(ref);frames.append((p.name[4:-5],json.loads(p.read_text())))
    assert frames[0][0]=='original-provider'and frames[-1][0]=='actual-empty-cell-recovered';named=dict(frames)
    baseline=copy.deepcopy(frames[0][1]['ledger']['inventory']);mc=seal['target'].split('/')[0]
    linked=named['declared-real-wireless-prerequisite']['ledger']['inventory'][0]
    import nbtlib
    raw=nbtlib.parse_nbt(linked['signature']);point=list(map(int,raw['components']['ae2:wireless_link_target']['pos']))
    wanted='{id:"merequester:wireless_requester_terminal",count:1,components:{"minecraft:custom_data":'+SOURCE_TAG+',"ae2wtlib:singularity":{},"ae2:stored_energy":100000.0d,"ae2:wireless_link_target":{dimension:"minecraft:overworld",pos:[I;'+','.join(map(str,point))+']}}}'
    assert linked['count']==1 and stack(linked['signature'])==stack(wanted)
    position=[point[0]+1.5,point[1]+1.0,point[2]+3.5];previous=ENERGY;unit=None;wireless_count=0;sessions=[]
    after_wireless={n for n,_ in frames[next(i for i,(n,_)in enumerate(frames)if n=='original-survival-wireless'):]}
    for label,f in frames:
        if label=='declared-real-wireless-prerequisite':continue
        if label not in after_wireless:continue
        energy=native_energy(f['ledger']['inventory'][0],linked,previous,unit or 1.0)
        if 'power'in f:
            power=f['power']['server'];assert len(power)==1;p=power[0]
            if unit is None:unit=10*p['drain'];energy=native_energy(f['ledger']['inventory'][0],linked,previous,unit)
            assert power_matches(p,linked,previous,unit,point,position)and energy is not None and energy<=p['stored']
            client=next(v for v in f['requester']['client']if v['menu']==MENU);server=next(v for v in f['requester']['server']if v['session']==client['session']and v['menu']==MENU)
            assert client['screen']==SCREEN and client['session']==p['session'];sessions.append(client['session']);wireless_count+=1
            assert synchronized(f['requester'],client,server)
        else:assert energy is not None
        previous=energy
    assert unit is not None and wireless_count>=15 and len(set(sessions))==2 and 0<previous<ENERGY
    filtered=named['wireless-impossible-search']['requester']['client'][0]
    assert(filtered['search'],filtered['matches'],filtered['lines'])==('siu_no_match_419',0,0)and not any(v['controls']['stateBox']['visible']for v in filtered['rows'])
    before=named['wireless-batch8']['requester']['client'][0];after=named['wireless-window-moved']['requester']['client'][0]
    assert(after['bounds']['x'],after['bounds']['y'])==(before['bounds']['x']+16,before['bounds']['y']+12)and after['session']==before['session']
    final_block=named['block-after-real-wireless-edit']['requester'];c=next(v for v in final_block['client']if v['menu'].endswith('.RequesterMenu'));s=next(v for v in final_block['server']if v['session']==c['session'])
    assert synchronized(final_block,c,s)and matches(final_block,None,0,1,False)and no_buffered_items(final_block)
    seed=root/row['profile']/'release-ae2-crafting-fixture.json';seed_data=json.loads(seed.read_text());assert fixture_pattern(seed_data['decoded'],mc)
    final_frame=frames[-1][1];recovered=final_frame['ledger']['inventory'][10];assert cellshell(recovered,mc)==cellshell(baseline[10],mc)
    wanted_inventory=copy.deepcopy(baseline);wanted_inventory[0]=final_frame['ledger']['inventory'][0];wanted_inventory[9]['count']=0;wanted_inventory[10]=recovered
    closed=path.parent/'closed.json';ref=reference(root,closed);assert ref in seal['full_states'];refs.append(ref);final=json.loads(closed.read_text())
    assert not final['menus']and final['cursor']['count']==0 and not physical_counter(final['physical'].values())
    assert len(final['inventory'])==len(wanted_inventory)and all(v.get('index',i)==wanted_inventory[i].get('index',i)and item_cell(v)==item_cell(wanted_inventory[i])for i,v in enumerate(final['inventory']))
    assert native_energy(final['inventory'][0],linked,previous,unit)==previous
    n=final_frame['native'];assert not physical_counter(n['drive'])and physical_counter(n['provider'])==physical_counter([seed_data['pattern']])and stored_matches(n['stored'],mc,0,0)
    for extra in seal['full_states']:
        if extra in refs:continue
        p=checked(root,extra);assert p.parent==path.parent and p.name.startswith('item-key-type-selection-')
        value=json.loads(p.read_text());a=value['before_ledger'];b=value['after_ledger']
        assert [item_cell(v)for v in a['inventory']]==[item_cell(v)for v in b['inventory']]and item_cell(a['cursor'])==item_cell(b['cursor'])and physical_counter(a['physical'].values())==physical_counter(b['physical'].values())
        refs.append(extra)
    assert len(refs)==len(seal['full_states'])
    return dict(status='native-wireless27checks-passive-power-and-typed-recovery-qualified',target=seal['target'],subjects={k:list(v)for k,v in expected.items()},seal=reference(root,seal_path),acceptance=reference(root,path),native_states=refs,native_seed=reference(root,seed),validation=dict(assertions=27,states=len(refs),wireless_power_states=wireless_count,source_energy_final=previous,native_debit_batch=unit),scope='Actual Survival wireless Requester, same world manager, original row/search/number/Submit/state/moved/reopen/source-lock input and complete resource recovery under actual monotonic native Double energy debits. Exact predeclared empty quantum component; no quantum transport/recharge or every-upgrade claim.')
