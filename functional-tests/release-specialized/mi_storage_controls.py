"""Extend original MI transfers with real native lock/eject controls per panel.

The original storage driver, transfers and assertions remain unchanged. These
additional inputs use native widget/cell coordinates and restore the original
empty cells, option state, complete physical inventory and cursor ownership.
"""
from pathlib import Path
import hashlib,json
import nbtlib
from mi_storage_features import Storage,FLUIDS,OUTPUTS,cell_state,cursor_is,physical,resources,typed,original

def capabilities(observed,mc,kind):
    assert mc in {'1.20.1','1.21.1'}and kind in {'items','fluids'}
    prefix='aztech.modern_industrialization.'+(''if mc=='1.20.1'else'client.')+'machines.'
    state=observed['controls'];options=state['autoExtract']
    assert type(state['lockingMode'])is bool and type(state['hasLockButton'])is bool
    assert isinstance(options,list)and len(options)<=1
    eject=False
    if options:
        option=options[0];assert option['type']==prefix+'guicomponents.AutoExtractClient'
        assert all(type(option[name])is bool for name in ['hasExtractItems','hasExtractFluids','extractItems','extractFluids'])
        assert option['hasExtractFluids'if kind=='items'else'hasExtractItems']is False
        eject=option['hasExtractItems'if kind=='items'else'hasExtractFluids']
    return dict(lock=state['hasLockButton'],eject=eject,kind=kind)

def expected_counts(caps):
    assert set(caps)=={'lock','eject','kind'}and caps['kind']in{'items','fluids'}
    assert type(caps['lock'])is bool and type(caps['eject'])is bool
    return 3+4*int(caps['lock'])+2*int(caps['eject']),3+5*int(caps['lock'])+2*int(caps['eject'])

def native_controls(client,mc,kind,observed):
    caps=capabilities(observed,mc,kind);window,_=original.geometry.check(client)
    prefix='aztech.modern_industrialization.'+(''if mc=='1.20.1'else'client.')+'machines.'
    native=[w for w in window['nativeWidgets']if w['type']==prefix+'gui.MachineScreen$MachineButton']
    names=[name for name in ['lock','eject']if caps[name]]
    assert len(native)==len(names)and len({w['x']for w in native})==len(native),'Native buttons disagree with native declared capabilities'
    native.sort(key=lambda w:w['x'],reverse=True)
    assert all(w['visible']is True and w['active']is True for w in native)
    return window,dict(zip(names,native)),caps

def button(client,mc,kind,control,enabled,observed):
    assert type(enabled)is bool
    window,roles,caps=native_controls(client,mc,kind,observed)
    assert control in roles,'Requested control is absent from this native release'
    if control=='lock':assert observed['controls']['lockingMode']is enabled
    else:assert observed['controls']['autoExtract'][0]['extractItems'if kind=='items'else'extractFluids']is enabled
    return window,roles[control]


def extracting(server,kind):
    assert kind in {'items','fluids'}
    value=nbtlib.parse_nbt(server['fixture']).get('extractItems'if kind=='items'else'extractFluids',nbtlib.Byte(0))
    assert isinstance(value,nbtlib.Byte)and int(value)in{0,1}
    return bool(value)

def lock_change(before,after,index):
    assert type(index)is int and 0<=index<len(before)and len(before)==len(after)
    if not bool(before[index].get('playerLockable',False)):
        return typed(before)==typed(after)
    value=after[index].get('playerLocked');assert isinstance(value,nbtlib.Byte)and int(value)==1
    for i,(initial,changed)in enumerate(zip(before,after)):
        a=nbtlib.Compound(dict(initial));b=nbtlib.Compound(dict(changed))
        if i==index:
            for name in ['playerLocked','locked']:a.pop(name,None);b.pop(name,None)
        if typed(a)!=typed(b):return False
    return True

class StorageWithControls(Storage):
    def __init__(self,*args,**kw):
        self.control_cases=[];self.control_frames=0;self.control_capabilities={};self.control_counts={}
        super().__init__(*args,**kw)
        self.control_out=self.out/'native-controls';self.control_out.mkdir()
    def save(self,complete=False):
        super().save(complete)
        path=self.out/'summary.json';journal=json.loads(path.read_text())
        journal.update(control_cases=self.control_cases,control_frames=self.control_frames,control_capabilities=self.control_capabilities,control_counts=self.control_counts,
            controls_driver_sha256=hashlib.sha256(Path(__file__).read_bytes()).hexdigest())
        journal['scope']+=' Additional native-provided lock-editor/empty-first-cell policy and auto-eject checks, persistence and restoration on all seventeen panels; absent controls are recorded from actual GUI/component capabilities, never invented. Original transfers/assertions retained. Options and physical items restored. Native state/button geometry identifies controls including legacy blank labels; no adjacent-container automation or power/recipe production claim.'
        path.write_text(json.dumps(journal,indent=2)+'\n')
    def frame(self,label):
        self.control_frames+=1;tag=str(self.control_frames).zfill(3)+'-'+self.case+'-'+label
        client,server=self.ui.save('native-controls-'+tag);original.geometry.check(client)
        self.ui.send('regression-mi-storage-state',ticks=5)
        observed=json.loads((self.profile/'release-mi-storage-state.json').read_text())
        (self.control_out/(tag+'.json')).write_text(json.dumps(dict(client=client,server=server,observed=observed),indent=2)+'\n')
        self.control_client=client;self.control_observed=observed;return server,observed
    def tap(self,kind,control,enabled):
        window,widget=button(self.control_client,self.mc,kind,control,enabled,self.control_observed);native=window['nativeBounds']
        self.ui.native(widget['x']+widget['width']/2-native['x'],widget['y']+widget['height']/2-native['y'])
    def controls(self,kind):
        checks_before=len(self.checks);frames_before=self.control_frames
        data,observed=self.frame('initial');cells=cell_state(data,observed,kind);baseline=physical(data)
        assert cells and resources(cells)==()and cursor_is(data)
        _,roles,caps=native_controls(self.control_client,self.mc,kind,observed)
        self.control_capabilities[self.case]=caps
        if caps['lock']:assert not bool(cells[0].get('playerLocked',False))
        initial_extract=extracting(data,kind);initial=typed(cells)
        def conserved(data):return physical(data)==baseline and cursor_is(data)
        def unchanged(data,observed):return typed(cell_state(data,observed,kind))==initial and conserved(data)
        def states(lock,eject):
            _,_,actual=native_controls(self.control_client,self.mc,kind,self.control_observed);assert actual==caps
            if caps['lock']:button(self.control_client,self.mc,kind,'lock',lock,self.control_observed)
            if caps['eject']:button(self.control_client,self.mc,kind,'eject',eject,self.control_observed)
        states(False,initial_extract)
        self.check('native storage exposes exactly its declared controls',conserved(data))
        locked=cells
        if caps['lock']:
            self.tap(kind,'lock',False);data,observed=self.frame('lock-editor-enabled');states(True,initial_extract)
            self.check('native lock editor enables without changing resources',unchanged(data,observed))
            self.click(observed,kind,0);data,observed=self.frame('first-cell-lock-policy')
            locked=cell_state(data,observed,kind)
            self.check('native first storage cell respects its player-lock policy',lock_change(cells,locked,0)and resources(locked)==()and conserved(data))
        active_extract=initial_extract
        if caps['eject']:
            self.tap(kind,'eject',initial_extract);data,observed=self.frame('auto-eject-toggled');active_extract=not initial_extract
            states(caps['lock'],active_extract)
            self.check('native auto-eject changes exact server option conservatively',extracting(data,kind)is active_extract and typed(cell_state(data,observed,kind))==typed(locked)and conserved(data))
        self.reopen();data,observed=self.frame('locked-option-reopened');states(False,active_extract)
        self.check('native available storage controls persist on reopening',extracting(data,kind)is active_extract and typed(cell_state(data,observed,kind))==typed(locked)and conserved(data))
        if caps['lock']:
            self.tap(kind,'lock',False);data,observed=self.frame('restoring-lock-editor-enabled');states(True,active_extract)
            self.click(observed,kind,0);data,observed=self.frame('first-cell-lock-restored')
            self.check('native lock editor restores only the selected empty cell',unchanged(data,observed))
            self.tap(kind,'lock',True);data,observed=self.frame('lock-editor-disabled');states(False,active_extract)
            self.check('native lock editor disables without changing storage',unchanged(data,observed))
        if caps['eject']:
            self.tap(kind,'eject',active_extract);data,observed=self.frame('auto-eject-restored');states(False,initial_extract)
            self.check('native auto-eject restores original option and full item ledger',extracting(data,kind)is initial_extract and unchanged(data,observed))
        self.reopen();data,observed=self.frame('controls-restored-reopened');states(False,initial_extract)
        self.check('native storage controls remain restored after reopening',extracting(data,kind)is initial_extract and unchanged(data,observed))
        counts=(len(self.checks)-checks_before,self.control_frames-frames_before)
        assert counts==expected_counts(caps),'Each native capability group must complete its own actual checks and frames'
        self.control_counts[self.case]=dict(checks=counts[0],frames=counts[1])
        self.control_cases.append(self.case);self.save()
    def fluid(self,name):super().fluid(name);self.controls('fluids')
    def output(self,name):super().output(name);self.controls('items')

def run(profile,mc,loader,*args,**kw):
    scenario=StorageWithControls(profile,mc,loader);result=scenario.run()
    assert scenario.control_cases==['machine-modern_industrialization-'+v for v in FLUIDS+OUTPUTS]
    assert set(scenario.control_capabilities)==set(scenario.control_cases)
    assert scenario.control_frames==sum(expected_counts(v)[1]for v in scenario.control_capabilities.values())
    return dict(**result,mi_storage_controls_checked=True)
