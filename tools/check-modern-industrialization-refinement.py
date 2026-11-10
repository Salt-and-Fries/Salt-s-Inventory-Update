"""Focused inputs after MI crop/padding/tray changes; no production simulation."""
from pathlib import Path
import argparse,json,sys
from importlib.machinery import SourceFileLoader
ROOT=Path(__file__).resolve().parents[1]
exercise=SourceFileLoader('mi_actions',str(ROOT/'tools/exercise-modern-industrialization.py')).load_module()
Ui=exercise.Ui

def main():
    p=argparse.ArgumentParser();p.add_argument('run');p.add_argument('--resume-machine',action='store_true');args=p.parse_args();ui=Ui(args.run)
    records=[]
    for case in ['shape','tank','item-pipe','fluid-pipe']:
        if not args.resume_machine:ui.execute(case)
        else:
            tag={'shape':'shape-changed','tank':'tank-bucket-transfer','item-pipe':'item-pipe-reopened','fluid-pipe':'fluid-pipe-reopened'}[case]
            assert (ui.out/(tag+'-server.json')).exists(),tag
        records.append(dict(check=case,status='pass'))
    ui.send('close',ticks=2);ui.send('command',command='gamemode survival @a',ticks=2)
    ui.block('electric_macerator')
    cell=next(s for s in ui.window()['nativeSlots'] if not s['player'] and s['active'] and s['bounds']['x']<176)['bounds']
    ui.seed('minecraft:stone',8);ui.native(cell['x']+8,cell['y']+8)
    ui.native(162,-14);ui.native(cell['x']+8,cell['y']+8);ui.native(140,-14)
    s,d=ui.save('final-macerator-with-inventory');exercise.geometry.check(s)
    assert 'extractItems:1b' in d['fixture'] and 'playerLocked:1b' in d['fixture'] and 'minecraft:stone' in d['fixture'],d['fixture']
    records.append(dict(check='compact machine slots, lock and auto-eject',status='pass'))
    ui.send('gui-scale',scale=3,ticks=8);s=ui.state();exercise.geometry.check(s)
    ui.native(140,-14);s,d=ui.save('macerator-resized-actions');exercise.geometry.check(s)
    assert 'extractItems:1b' not in d['fixture'],d['fixture'] # Newer MI omits false defaults.
    ui.send('gui-scale',scale=2,ticks=8)
    s=ui.state();workspace=next(w for w in s['workspaceWindows'] if w['kind']=='CONTAINER')
    if workspace['locked'] in [True,'true']:
        if 'LOCK' in workspace['controls']:
            control=workspace['controls']['LOCK'];ui.click(control['x'],control['y'])
        else:
            control=workspace['controls']['ELLIPSIS'];ui.click(control['x'],control['y']);ui.save('compact-management-menu')
            # Native SIU popup: four 11px controls, 3px gaps and 4px padding.
            ui.click(control['x']-18,control['y']+17)
    assert next(w for w in ui.state()['workspaceWindows'] if w['kind']=='CONTAINER')['locked'] in [False,'false']
    b=ui.window()['bounds'];x,y=b['x']+24,b['y']+8
    ui.send('click',x=x,y=y,ticks=1);ui.send('drag',x=x-30,y=y+18,dx=-30,dy=18,ticks=4);ui.send('release',x=x-30,y=y+18,ticks=5)
    s,d=ui.save('macerator-moved');w,_=exercise.geometry.check(s)
    assert w['bounds']['x']==b['x']-30 and w['bounds']['y']==b['y']+18,(b,w['bounds'])
    ui.native(140,-14);s,d=ui.save('macerator-moved-action');assert 'extractItems:1b' in d['fixture'],d['fixture']
    records.append(dict(check='GUI scale and parent/tray movement with native input',status='pass'))
    ui.send('close',ticks=2);ui.block('lv_energy_input_hatch');ui.save('energy-padding')
    ui.send('close',ticks=2);ui.send('command',command='data merge block 8 65 8 {storedEu:1000L}',ticks=4)
    ui.send('open-ray',ticks=8);s,d=ui.save('energy-charged');exercise.geometry.check(s)
    assert 'storedEu:1000L' in d['fixture'],d['fixture']
    records.append(dict(check='native energy icon empty and charged',status='pass'))
    ui.send('close',ticks=2);ui.block('bronze_boiler');ui.save('full-thermometer')
    ui.send('close',ticks=2);ui.block('large_tank');ui.save('full-large-tank')
    (ui.out/'refinement-actions.json').write_text(json.dumps(records,indent=2))
    print(args.run,'REFINEMENT ACTIONS PASS',flush=True)

if __name__=='__main__':main()
