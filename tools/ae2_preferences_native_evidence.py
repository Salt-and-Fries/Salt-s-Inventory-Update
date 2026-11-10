"""Independently validate actual local preferences, native callbacks and full owned-item conservation."""
from pathlib import Path
import copy,json
from focused_release_evidence import checked,reference,verify
from ae2_settings_features import ledger,marker_item,pair,stack
from ae2_preferences_features import FIELDS,INTERNAL,values,controls_match

PARENT='appeng.client.gui.me.common.MEStorageScreen'
CHILD='appeng.client.gui.me.common.TerminalSettingsScreen'
MENU='appeng.menu.me.common.MEStorageMenu'
TARGETS={'1.20.1/fabric','1.20.1/forge','1.21.1/neoforge','26.1.2/neoforge','26.2/neoforge','26.3/neoforge'}
NAMES=['original powered terminal opens with exact declared full typed unrelated inventory','original settings button opens actual local child sharing unchanged server menu','original internal-search radio preserves all other native local settings','local preference controls move with original window and no owned mutation']
for name in INTERNAL:NAMES+=['original moved '+name+' changes exactly its actual native config boolean','original '+name+' restores exact native value']
NAMES+=['native browser availability and conditional external controls preserve their actual contract','original Back returns to real original terminal and identical local values','ordinary terminal reopening preserves all native local settings and complete owned ledger','all original local and server sessions close without full-item or cursor changes']

def validate(target,journal,frames,closed,driver_hash):
    assert target in TARGETS and journal['complete']is True and journal['cases']==['terminal-preferences']and journal['driver_sha256']==driver_hash
    assert [(v['case'],v['name'],v['passed'])for v in journal['checks']]==[('terminal-preferences',name,True)for name in NAMES]and len(NAMES)==20
    assert frames[0][0]=='original-terminal'
    first=frames[0][1];assert len(first['native']['client'])==1
    initial=values(first['native']['client'][0]);internal=copy.deepcopy(initial);internal['isUseExternalSearch']=False
    available=first['native']['client'][0]['browserEnabled'];assert type(available)is bool
    expected=[('original-terminal',False,initial),('original-local-preferences',True,initial),('original-internal-search',True,internal),('preferences-moved',True,internal)]
    for name in INTERNAL:
        wanted=copy.deepcopy(internal);wanted[FIELDS[name]]=not wanted[FIELDS[name]]
        expected.extend([('toggle-'+name,True,wanted),('restore-'+name,True,internal)])
    if available:
        external=copy.deepcopy(internal);external['isUseExternalSearch']=True
        changed=copy.deepcopy(external);changed[FIELDS['clearExternalCheckbox']]=not changed[FIELDS['clearExternalCheckbox']]
        expected.extend([('external-radio',True,external),('external-clear-toggle',True,changed),('external-clear-restored',True,external),('native-internal-restored',True,internal)])
    if initial['isUseExternalSearch']:
        assert available;expected.append(('initial-radio-restored',True,initial))
    expected.extend([('original-parent-returned',False,initial),('ordinary-parent-reopened',False,initial),('ordinary-preferences-reopened',True,initial)])
    assert [label for label,_ in frames]==[label for label,_,_ in expected]
    baseline=ledger(first['ledger']);inventory=first['ledger']['inventory'];assert len(inventory)>=36 and all(v['index']==i for i,v in enumerate(inventory))
    assert stack(inventory[12]['signature'],inventory[12]['count'])==stack(marker_item(target.split('/')[0]))
    assert all(v['count']==0 for i,v in enumerate(inventory)if i!=12)and first['ledger']['cursor']['count']==0
    clients={};native={}
    for (label,frame),(wanted_label,child,wanted)in zip(frames,expected):
        assert label==wanted_label and set(frame)=={'native','widgets','ledger'}
        c,s=pair(frame['widgets'],'MEStorageMenu');assert c['menu']==s['menu']==MENU and c['screen']==(CHILD if child else PARENT)
        rows=frame['native']['client'];assert len(rows)==1;n=rows[0]
        assert n['session']==c['session']and n['menu']==MENU and n['screen']==c['screen']and n['sameParentMenu']is True and n['parentScreen']==PARENT
        assert values(n)==wanted and n['browserEnabled']is available
        if child:assert controls_match(n)
        else:assert n['controls']=={}
        assert ledger(frame['ledger'])==baseline
        assert len([v for v in frame['ledger']['menus']if v['type']==MENU and v['session']==c['session']])==1
        clients[label]=c;native[label]=n
    original=clients['original-terminal']['session'];reopened=clients['ordinary-parent-reopened']['session'];assert original!=reopened
    assert all(c['session']==(reopened if label.startswith('ordinary-')else original)for label,c in clients.items())
    before=clients['original-internal-search']['bounds'];after=clients['preferences-moved']['bounds']
    assert(after['x'],after['y'])==(before['x']+16,before['y']+12)and(after['width'],after['height'])==(before['width'],before['height'])
    for name,a in native['original-internal-search']['controls'].items():
        b=native['preferences-moved']['controls'][name]
        if a['visible']and b['visible']:assert(b['x'],b['y'],b['width'],b['height'])==(a['x']+16,a['y']+12,a['width'],a['height'])
    assert not closed['menus']and ledger(closed)==baseline
    entries=journal['original_entries'];assert len(entries)==1 and entries[0]['original']['id']=='atlas-ae2-terminal'and entries[0]['external_browser_available']is available
    return dict(assertions=20,states=len(frames)+1,external_browser_available=available,contract='real-native-local-preferences-moved-controls-return-reopen-and-typed-conservation')

def derive(root,seal_path,expected):
    root=Path(root).resolve();sealed=json.loads(Path(seal_path).read_text());verify(root,sealed,expected)
    summary=json.loads(checked(root,sealed['summary']).read_text());row=summary['results'][0]
    assert summary['status']==row['status']=='passed'and row['exit_code']==0
    result=row['results'];assert result['ae2_preferences_features_checked']is True and(result['checks'],result['cases'],result['failed'])==(20,1,0)
    path=checked(root,sealed['acceptance']);assert path==(root/row['profile']/'ae2-preferences-release/summary.json').resolve()
    refs=[];frames=[]
    for index,p in enumerate(sorted(path.parent.glob('[0-9][0-9][0-9]-*.json')),1):
        assert p.name.startswith(str(index).zfill(3)+'-');ref=reference(root,p);assert ref in sealed['full_states'];refs.append(ref);frames.append((p.name[4:-5],json.loads(p.read_text())))
    p=path.parent/'closed.json';ref=reference(root,p);assert ref in sealed['full_states'];refs.append(ref)
    validation=validate(sealed['target'],json.loads(path.read_text()),frames,json.loads(p.read_text()),sealed['archived_inputs']['functional-tests/release-specialized/ae2_preferences_features.py'])
    assert len(refs)==len(sealed['full_states'])==validation['states']
    return dict(status='native-local-preferences20checks-and-full-typed-ledgers-qualified',target=sealed['target'],subjects={k:list(v)for k,v in expected.items()},seal=reference(root,seal_path),acceptance=reference(root,path),native_states=refs,validation=validation,scope='Actual native TerminalSettingsScreen/MEStorageScreen with the same server menu; all applicable native AEConfig checkbox/radio values, original moved controls, native return tab, ordinary reopening and complete typed owned-item/cursor conservation. Local config only; no browser-query integration, notification delivery or crafting-grid side-effect claim.')
