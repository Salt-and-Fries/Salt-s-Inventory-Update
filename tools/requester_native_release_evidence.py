"""Validate sealed original Requester workflows and independently check final typed resource recovery."""
from pathlib import Path
import ast,copy,json
from focused_release_evidence import checked,reference,verify
from ae2_crafting_features import cellshell,fixture_pattern,key_item,stored_matches,physical_counter
from ae2_settings_features import item_cell
from requester_features import matches,synchronized,no_buffered_items

def derive(root,seal_path,expected):
    root=Path(root).resolve();seal=json.loads(Path(seal_path).read_text());verify(root,seal,expected)
    summary=json.loads(checked(root,seal['summary']).read_text());row=summary['results'][0]
    assert summary['status']==row['status']=='passed'and row['exit_code']==0
    result=row['results'];assert result['requester_features_checked']is True and result['cases']==2 and result['checks']==32 and result['failed']==0
    acceptance=checked(root,seal['acceptance']);journal=json.loads(acceptance.read_text())
    assert journal['complete']is True and journal['cases']==['requester-block','requester-terminal']
    assert len(journal['checks'])==32 and all(v['passed']is True for v in journal['checks'])
    source=root/'functional-tests/release-specialized/requester_features.py'
    names=[v.args[0].value for v in ast.walk(ast.parse(source.read_text()))if isinstance(v,ast.Call)and isinstance(v.func,ast.Attribute)and v.func.attr=='check'and v.args and isinstance(v.args[0],ast.Constant)]
    names=[s.replace('impossible native search hides request controls while retaining original summary header and models','impossible native search hides all unmatched rows and retains exact models')for s in names]
    assert len(names)==32 and {v['name']for v in journal['checks']}==set(names)
    frames=[];refs=[]
    for i,p in enumerate(sorted(acceptance.parent.glob('[0-9][0-9][0-9]-*.json')),1):
        assert p.name.startswith(str(i).zfill(3)+'-');ref=reference(root,p);assert ref in seal['full_states'];frames.append((p.name[4:-5],json.loads(p.read_text())));refs.append(ref)
    assert frames[0][0]=='original-provider'and frames[-1][0]=='native-cell-recovered'
    baseline=frames[0][1]['ledger']['inventory'];mc=seal['target'].split('/')[0]
    seed=(root/row['profile']/'release-ae2-crafting-fixture.json').resolve();assert seed.is_relative_to(root)
    seed_data=json.loads(seed.read_text());assert fixture_pattern(seed_data['decoded'],mc)
    closed=acceptance.parent/'closed.json';closed_ref=reference(root,closed);assert closed_ref in seal['full_states'];refs.append(closed_ref);final=json.loads(closed.read_text())
    expected_inventory=copy.deepcopy(baseline);expected_inventory[9]['count']=0
    actual_cell=frames[-1][1]['ledger']['inventory'][10];assert cellshell(actual_cell,mc)==cellshell(baseline[10],mc);expected_inventory[10]=actual_cell
    expected_inventory[11]=dict(baseline[11],count=13);expected_inventory[13]=dict(baseline[13],item='minecraft:oak_planks',count=12,signature=key_item(mc,'minecraft:oak_planks')['signature'])
    assert len(final['inventory'])==len(expected_inventory)and not final['menus']and final['cursor']['count']==0
    assert all(v.get('index',i)==expected_inventory[i].get('index',i)and item_cell(v)==item_cell(expected_inventory[i])for i,v in enumerate(final['inventory']))
    assert not physical_counter(final['physical'].values())
    last=frames[-1][1]['native'];assert not physical_counter(last['drive'])and physical_counter(last['provider'])==physical_counter([seed_data['pattern']])and stored_matches(last['stored'],mc,0,0)
    generated=[f for label,f in frames if label.startswith('genuine-request-production-')and stored_matches(f['network']['stored'],mc,13,12)]
    assert generated and all(no_buffered_items(f['requester'])for f in generated[-1:])
    filtered=next(f for label,f in frames if label=='native-impossible-search');client=next(v for v in filtered['requester']['client']if v['menu'].endswith('.RequesterTerminalMenu'))
    assert(client['search'],client['matches'],client['lines'])==('siu_no_match_347',0,0)and not any(v['controls']['stateBox']['visible']for v in client['rows'])
    cleared=next(f for label,f in frames if label=='native-final-config-cleared');client=next(v for v in cleared['requester']['client']if v['menu'].endswith('.RequesterMenu'));server=next(v for v in cleared['requester']['server']if v['session']==client['session'])
    assert synchronized(cleared['requester'],client,server)and matches(cleared['requester'],None,0,1,False)and no_buffered_items(cleared['requester'])
    # Remaining sealed observations include actual numeric-field clearing/filter prerequisites.
    for ref in seal['full_states']:checked(root,ref)
    return dict(status='actual-native-Requester32checks-and-independent-typed-recovery-qualified',target=seal['target'],subjects={k:list(v)for k,v in expected.items()},seal=reference(root,seal_path),acceptance=reference(root,acceptance),original_states=refs,native_seed=reference(root,seed),checks=32,states=len(seal['full_states']),scope='Original block and terminal native32interaction assertions, ghost/number/Submit/state/search/moved/reopen behavior, real12plank production from3logs, independent exact final full typed inventory/cell shell/provider/storage recovery. No arbitrary automation or wireless claim.')
