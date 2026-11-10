from pathlib import Path
import importlib.util,json,hashlib,sys
ROOT=Path(__file__).resolve().parents[2]
spec=importlib.util.spec_from_file_location('native_driver',ROOT/'functional-tests/tech-reborn-design/capture.py');d=importlib.util.module_from_spec(spec);spec.loader.exec_module(d)
RUN=ROOT/'build/remaining-ui-audit/gameplay/1.20.1-fabric-integrated-tech-reborn-implemented-10';OUT=ROOT/'output/tech-reborn-implementation';OUT.mkdir(exist_ok=True,parents=True)
d.RUN=RUN;d.OUT=OUT
RECORDS=[]
call=d.call;cmd=d.cmd;tap=d.tap

def capture(key,**extra):
    call('move',x=2,y=2,ticks=2)
    s=call('capture',capture=key,ticks=5)
    assert s.get('siu_loaded') is True and s.get('sessions')==1,(key,s)
    w=s['allWindows'][-1]
    assert 'TechRebornClient' in w.get('template',''),(key,w)
    assert w.get('sameModel') is True,(key,'native/menu ownership mismatch',w)
    assert w.get('composition',{}).get('regions',[{}])[0].get('id')=='native-machine',(key,w)
    p=RUN/'screenshots'/(key+'.png')
    RECORDS.append(dict(id=key,capture=str(p.relative_to(ROOT)),sha256=hashlib.sha256(p.read_bytes()).hexdigest(),state=s,**extra))
    (OUT/'captures.json').write_text(json.dumps(RECORDS,indent=2));print('CAPTURED',key,w.get('trSelectedTab'),w.get('trValid'),flush=True)
    return s

def main():
    call('catalog',namespaces=['techreborn'],ticks=1)
    cmd('gamerule doWeatherCycle false');cmd('gamerule doDaylightCycle false');cmd('time set noon');cmd('weather clear')
    for r in d.OLD:
        key=r['id'];block=next(a['block']for a in d.OPENINGS[key]['native_actions']if a['type']=='fixture')
        s=d.open_block(block)
        multi=any(name in r['native']['screen']for name in ['GuiBlastFurnace','GuiDistillationTower','GuiFluidReplicator','GuiFusionReactor','GuiImplosionCompressor','GuiIndustrialGrinder','GuiIndustrialSawmill','GuiVacuumFreezer'])
        if multi:
            capture(key+'-unformed',block=block,variant='unformed');d.form_structure(key)
        capture(key,block=block,variant='formed-idle'if multi else'idle')
    call('close')
if __name__=='__main__':main()
