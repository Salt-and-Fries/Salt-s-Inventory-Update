"""Real AE2 local preference controls, native child/parent return and persistence."""
from pathlib import Path
import copy,hashlib,json
from ae2_settings_features import Settings,ENTRIES,isolate,translate,pair,ledger,stack,MARKER,marker_item

FIELDS={
    'pinAutoCraftedItemsCheckbox':'isPinAutoCraftedItems',
    'notifyForFinishedCraftingJobsCheckbox':'isNotifyForFinishedCraftingJobs',
    'clearGridOnCloseCheckbox':'isClearGridOnClose',
    'rememberCheckbox':'isRememberLastSearch',
    'autoFocusCheckbox':'isAutoFocusSearch',
    'syncWithExternalCheckbox':'isSyncWithExternalSearch',
    'clearExternalCheckbox':'isClearExternalSearchOnOpen',
}
INTERNAL=list(FIELDS)[:6]


def values(native):
    raw=native['values'];assert set(raw)==set(FIELDS.values())|{'isUseExternalSearch'}
    assert all(type(v['value'])is bool and v['javaType']=='java.lang.Boolean'for v in raw.values())
    return{k:v['value']for k,v in raw.items()}


def controls_match(native):
    wanted=values(native);controls=native['controls'];external=wanted['isUseExternalSearch']
    if set(controls)!=set(FIELDS)|{'useInternalSearchRadio','useExternalSearchRadio'}or type(native['browserEnabled'])is not bool:return False
    for name,getter in FIELDS.items():
        if type(controls[name]['selected'])is not bool or controls[name]['selected']!=wanted[getter]:return False
    if controls['useInternalSearchRadio']['selected']is not(not external)or controls['useExternalSearchRadio']['selected']is not external:return False
    if controls['useExternalSearchRadio']['active']is not native['browserEnabled']:return False
    for name in ['rememberCheckbox','autoFocusCheckbox','syncWithExternalCheckbox']:
        if controls[name]['visible']is not(not external):return False
    return controls['clearExternalCheckbox']['visible']is external


class Preferences(Settings):
    def __init__(self,profile,mc,loader):
        self.profile=Path(profile);self.mc=mc;self.loader=loader;self.command='server-command'if loader=='neoforge'else'command';self.n=0;self.checks=[];self.cases=[];self.case='terminal-preferences';self.baseline=None;self.entry_records=[]
        self.out=self.profile/'ae2-preferences-release';self.out.mkdir();self.act('regression-catalog',ticks=2);self.origin=json.loads((self.profile/'regression-catalog.json').read_text())['origin']
    def save(self,complete=False):
        (self.out/'summary.json').write_text(json.dumps(dict(complete=complete,cases=self.cases,checks=self.checks,original_entries=self.entry_records,driver_sha256=hashlib.sha256(Path(__file__).read_bytes()).hexdigest(),scope='Actual Terminal Settings local AESubScreen shares original server menu. Real checkbox/radio callbacks change exactly their native AEConfig booleans; moved input, original Back, ordinary reopening and full typed inventory/cursor/physical preservation. Browser-dependent controls are tested only when native ItemListMod reports them available; otherwise actual disabled availability is asserted. No claimed server-side preference, external browser search behavior, notification delivery or crafting-grid side effect.'),indent=2)+'\n')
    def frame(self,label,child=True):
        self.act('regression-ae2-child-state',ticks=3);p=json.loads((self.profile/'release-ae2-child-state.json').read_text());self.client,self.server=pair(p,'MEStorageMenu')
        self.act('regression-ae2-preferences-state',ticks=3);probe=json.loads((self.profile/'release-ae2-preferences-state.json').read_text());assert len(probe['client'])==1;self.native=probe['client'][0]
        assert self.native['session']==self.client['session']and self.native['menu']==self.client['menu']and self.native['sameParentMenu']is True
        assert self.native['screen']==('appeng.client.gui.me.common.TerminalSettingsScreen'if child else'appeng.client.gui.me.items.TerminalScreen')
        self.act('regression-state',ticks=3);data=json.loads((self.profile/'regression-state.json').read_text());self.n+=1
        (self.out/(str(self.n).zfill(3)+'-'+label+'.json')).write_text(json.dumps(dict(native=probe,widgets=p,ledger=data),indent=2)+'\n')
        if self.baseline is not None:assert ledger(data)==self.baseline,'Local preference input changed full owned items/cursor'
        return data
    def open_settings(self):
        options=[v for v in self.client['widgets']if v['visible']and v['active']and v.get('icon')=='COG'and any('Terminal'in t and'Settings'in t for t in v.get('tooltips',[]))];assert len(options)==1;self.tap(self.client,options[0])
    def valid(self,wanted):return values(self.native)==wanted and controls_match(self.native)
    def press(self,name):self.tap(self.client,self.native['controls'][name])
    def run(self):
        self.act('close',ticks=3)
        for cmd in isolate(self.origin):self.cmd(cmd)
        self.cmd('clear @a');entries=[v for v in json.loads(ENTRIES.read_text())['cases']if v['id']=='atlas-ae2-terminal'and(v['minecraft'],v['loader'])==(self.mc,self.loader)];assert len(entries)==1
        entry=entries[0];old=[int(v)for v in entry['captured_origin'].split(',')];actions=[]
        for original in entry['actions']:
            action=copy.deepcopy(original);kind=action.pop('type');assert kind in {'fixture','command','use-block-ray'}
            if kind=='use-block-ray':continue
            if kind=='command':action['command']=translate(action['command'],old,self.origin);kind=self.command
            self.act(kind,**action);actions.append(dict(type=kind,**action))
        self.cmd('gamemode survival @a');suffix=MARKER if self.mc=='1.20.1'else'[minecraft:custom_data='+MARKER+']';self.cmd('item replace entity @a container.12 with minecraft:diamond'+suffix+' 7');self.act('use-block-ray',ticks=24)
        data=self.frame('original-terminal',False);self.baseline=ledger(data);session=self.client['session'];initial=values(self.native)
        self.check('original powered terminal opens with exact declared full typed unrelated inventory',stack(data['inventory'][12]['signature'],data['inventory'][12]['count'])==stack(marker_item(self.mc))and data['cursor']['count']==0)
        self.open_settings();data=self.frame('original-local-preferences');self.check('original settings button opens actual local child sharing unchanged server menu',self.client['session']==session and self.native['parentScreen']=='appeng.client.gui.me.items.TerminalScreen'and self.valid(initial))
        self.press('useInternalSearchRadio');self.frame('original-internal-search');baseline=copy.deepcopy(initial);baseline['isUseExternalSearch']=False
        self.check('original internal-search radio preserves all other native local settings',self.valid(baseline))
        before=self.client['bounds'];self.move(self.client);self.frame('preferences-moved');self.check('local preference controls move with original window and no owned mutation',self.client['bounds']['x']==before['x']+16 and self.client['bounds']['y']==before['y']+12 and self.valid(baseline))
        for field in INTERNAL:
            getter=FIELDS[field];wanted=copy.deepcopy(baseline);wanted[getter]=not wanted[getter];self.press(field);self.frame('toggle-'+field)
            self.check('original moved '+field+' changes exactly its actual native config boolean',self.valid(wanted))
            self.press(field);self.frame('restore-'+field);self.check('original '+field+' restores exact native value',self.valid(baseline))
        available=self.native['browserEnabled']
        if available:
            external=copy.deepcopy(baseline);external['isUseExternalSearch']=True;self.press('useExternalSearchRadio');self.frame('external-radio');assert self.valid(external)
            field='clearExternalCheckbox';wanted=copy.deepcopy(external);wanted[FIELDS[field]]=not wanted[FIELDS[field]];self.press(field);self.frame('external-clear-toggle');assert self.valid(wanted)
            self.press(field);self.frame('external-clear-restored');assert self.valid(external);self.press('useInternalSearchRadio');self.frame('native-internal-restored')
        else:assert self.native['controls']['useExternalSearchRadio']['active']is False
        self.check('native browser availability and conditional external controls preserve their actual contract',self.valid(baseline)and self.native['browserEnabled']is available)
        if initial['isUseExternalSearch']:
            assert available;self.press('useExternalSearchRadio');self.frame('initial-radio-restored');assert self.valid(initial)
        back=[v for v in self.client['widgets']if v['visible']and v['active']and v.get('icon')=='BACK'];assert len(back)==1;self.tap(self.client,back[0]);self.frame('original-parent-returned',False)
        self.check('original Back returns to real original terminal and identical local values',self.client['session']==session and values(self.native)==initial)
        self.act('close',ticks=4);self.act('use-block-ray',ticks=24);self.frame('ordinary-parent-reopened',False);self.open_settings();self.frame('ordinary-preferences-reopened')
        self.check('ordinary terminal reopening preserves all native local settings and complete owned ledger',self.valid(initial))
        self.act('close',ticks=5);self.act('regression-state',ticks=3);closed=json.loads((self.profile/'regression-state.json').read_text());(self.out/'closed.json').write_text(json.dumps(closed,indent=2)+'\n')
        self.check('all original local and server sessions close without full-item or cursor changes',not closed['menus']and ledger(closed)==self.baseline)
        self.entry_records.append(dict(original=entry,executed_world_actions=actions,entry='Original terminal Settings action, not a constructed local screen',external_browser_available=available,scope='Actual client configuration only'))
        self.cases.append(self.case);self.save(True);return dict(cases=1,failed=0,checks=len(self.checks),ae2_preferences_features_checked=True)


def run(profile,mc,loader,*args,**kwargs):return Preferences(profile,mc,loader).run()
