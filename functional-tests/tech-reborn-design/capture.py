"""Real native openings and source-blueprint-built multiblocks; no SIU loaded."""
from pathlib import Path
import hashlib, json, sys

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0,str(ROOT/'functional-tests/project-everything-ten'))
from send import send

RUN = ROOT/'build/remaining-ui-audit/gameplay/1.20.1-fabric-native-tech-reborn-design-02'
OUT = ROOT/'output/tech-reborn-window-design'
OPENINGS = json.loads((ROOT/'build/tech-reborn-window-design/openings.json').read_text())
OLD = json.loads((OUT/'native-references.json').read_text())['cases']
RECORDS = []


def call(kind, **args):
    return send(RUN, kind, **args)['client']


def cmd(command, ticks=3):
    return call('command',command=command,ticks=ticks)


def capture(key, expected=None, **extra):
    call('move',x=2,y=2,ticks=2)
    s = call('capture',capture=key,ticks=5)
    if expected:
        assert s['screen'] == expected,(key,s['screen'])
    assert s['siu_loaded'] is False
    p = RUN/'screenshots'/(key+'.png')
    r = dict(id=key,capture=str(p.relative_to(ROOT)),sha256=hashlib.sha256(p.read_bytes()).hexdigest(),state=s,**extra)
    RECORDS.append(r)
    (OUT/'fresh-native.json').write_text(json.dumps(RECORDS,indent=2))
    print('CAPTURED',key,s['screen'].split('.')[-1],s.get('trSelectedTab'),s.get('trValid'),flush=True)
    return s


def tap(x,y):
    call('move',x=x,y=y,ticks=1)
    call('click',x=x,y=y,button=0,ticks=1)
    return call('release',x=x,y=y,button=0,ticks=8)


def open_block(block):
    call('close')
    cmd('fill -10 65 -10 26 79 26 minecraft:air')
    call('fixture',block=block,ticks=8)
    cmd('tp @p 8.5 65 12.5 180 8')
    return call('open',ticks=15)


def form_structure(key):
    call('close')
    call('tr-blueprint',ticks=3)
    blueprint=json.loads((RUN/'blueprint.json').read_text())
    assert blueprint['blocks']
    runtime=Path(json.loads((RUN/'launch.json').read_text())['cwd'])
    save=next((runtime/'saves').glob('native-reference-*'))
    pack=save/'datapacks/siu-reference-blueprint'
    (pack/'data/siu_reference/functions').mkdir(exist_ok=True,parents=True)
    (pack/'pack.mcmeta').write_text(json.dumps({'pack':{'pack_format':15,'description':'Disposable native structure reference'}}))
    lines=[f'setblock {8+b["x"]} {65+b["y"]} {8+b["z"]} {b["state"]}'for b in blueprint['blocks']if (b['x'],b['y'],b['z'])!=(0,0,0)]
    (pack/'data/siu_reference/functions/structure.mcfunction').write_text('\n'.join(lines)+'\n')
    cmd('reload',ticks=30)
    cmd('function siu_reference:structure',ticks=45)
    call('tr-blueprint',ticks=3)
    result=json.loads((RUN/'blueprint.json').read_text())
    (OUT/(key+'-blueprint.json')).write_text(json.dumps(result,indent=2))
    assert result['valid'],(key,'Native multiblock validation failed')
    cmd('tp @p 8.5 65 12.5 180 8')
    return call('open',ticks=16)


def main():
    call('catalog',namespaces=['techreborn'],ticks=1)
    cmd('gamerule doWeatherCycle false')
    for r in OLD:
        key=r['id']
        block=next(a['block']for a in OPENINGS[key]['native_actions']if a['type']=='fixture')
        s=open_block(block)
        assert s['screen']==r['native']['screen'],(key,s['screen'])
        is_multiblock=any(name in s['screen']for name in ['GuiBlastFurnace','GuiDistillationTower','GuiFluidReplicator','GuiFusionReactor','GuiImplosionCompressor','GuiIndustrialGrinder','GuiIndustrialSawmill','GuiVacuumFreezer'])
        if is_multiblock:
            capture(key+'-unformed',r['native']['screen'],block=block,variant='unformed')
            s=form_structure(key)
        capture(key,r['native']['screen'],block=block,variant='formed-idle'if is_multiblock else'idle')
    call('close')


if __name__=='__main__':
    main()
