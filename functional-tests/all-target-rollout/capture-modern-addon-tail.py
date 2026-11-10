"""Natural entries for current Botania, AE2 addons, Alloyed and Spawn."""
from pathlib import Path
import argparse,json,subprocess,sys
sys.path.insert(0,str(Path(__file__).parent))
from evidence import capture,send
p=argparse.ArgumentParser();p.add_argument('run',type=Path);p.add_argument('--side',choices=['native','siu'],required=True);a=p.parse_args()
common=['--side',a.side,'--minecraft','1.21.1','--loader','neoforge']
selected=['ad-astra-giselle-addon-automation-nasa-workbench','ad-astra-giselle-addon-fuel-loader',
 'ad-astra-giselle-addon-gravity-normalizer','ad-astra-giselle-addon-rocket-sensor',
 'own-ae-part-requester-terminal','merequester-requester','own-ae-part-cell-dock',
 'megacells-mega-interface','megacells-mega-pattern-provider',
 'item-megacells-portable-item-cell-1m','item-megacells-portable-fluid-cell-1m']
args=[sys.executable,'-X','utf8',str(Path(__file__).parent/'capture-reviewed-manifests.py'),str(a.run),*common,
 '--manifest','build/all-target-rollout/themed-reviewed-cases-1211-neoforge.json','--record','current-addon-reviewed-captures']
for name in selected:args+=['--only-case',name]
subprocess.run(args,check=True)
subprocess.run([sys.executable,'-X','utf8',str(Path(__file__).parent/'capture-container-batch.py'),
 '--siu',str(a.run),'--native',str(a.run),*common,'--only','alloyed-forge','--only','alloyed-oven'],check=True)
send(a.run,'catalog',ticks=1);items=set(json.loads((a.run/'catalog.json').read_text())['items'])
for ui,candidates in [('flower-pouch',['botania:flower_pouch','botania:flower_bag']),('petal-pouch',['botania:petal_pouch']),
 ('bauble-box',['botania:trinket_case','botania:bauble_box']),('botania-hand-of-ender',['botania:hand_of_ender','botania:ender_hand'])]:
 item=next((v for v in candidates if v in items),None)
 if item is None:print('ABSENT',ui,candidates,flush=True);continue
 send(a.run,'close',ticks=2);send(a.run,'sneak',down=False,ticks=2)
 send(a.run,'command',command='gamemode survival @a',ticks=2)
 send(a.run,'command',command='item replace entity @a inventory.0 with botania:mana_tablet[botania:mana=500000]',ticks=2)
 send(a.run,'held',item=item,ticks=4);send(a.run,'use-item',ticks=16)
 capture(a.run,a.side,'1.21.1','neoforge',ui,'Original Botania 457 item and native renderer. Native mana source supplied where required; pictured default state only.')
send(a.run,'close',ticks=2);send(a.run,'sneak',down=False,ticks=2);send(a.run,'command',command='gamemode creative @a',ticks=2)
send(a.run,'held',item='minecraft:air',ticks=2);send(a.run,'fixture',block='spawn:pigment_shifter',ticks=3)
send(a.run,'command',command='setblock 8 65 8 spawn:pigment_shifter[waterlogged=true]',ticks=3)
s=send(a.run,'client-use-block',ticks=15)['client']
if a.side=='siu':assert any(w.get('nativeScreen','').endswith('.PigmentShifterScreen') and isinstance(w.get('template'),dict) for w in s.get('windows',[])),'Expected reviewed pigment layout'
capture(a.run,a.side,'1.21.1','neoforge','spawn-pigment-shifter','Native waterlogged pigment editor. Original ghost-slot icons, preview and rotation affordance retained.')
