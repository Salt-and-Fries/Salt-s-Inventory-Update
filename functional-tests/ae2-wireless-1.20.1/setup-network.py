import sys,json
from pathlib import Path
import focused as f
if len(sys.argv)!=3:raise SystemExit('Usage: '+sys.argv[0]+' fabric|forge <disposable-production-observer-output>')
loader=sys.argv[1];f.RUN=Path(sys.argv[2]).resolve()
s=f.S('fixture',block='ae2:wireless_access_point',ticks=10);f.cmd('gamemode survival @a')
for p in ['8 64 8','8 65 9','7 65 8','8 66 8','8 65 7']:f.cmd('setblock '+p+' ae2:creative_energy_cell',4)
f.cmd('setblock 9 65 9 ae2:cable_bus{cable:{id:"ae2:fluix_glass_cable"}}',4);f.cmd('setblock 9 65 8 ae2:drive',4);f.cmd('data merge block 9 65 8 {inv:{item0:{id:"ae2:item_storage_cell_1k",Count:1b}}}',5)
f.S('held',item='ae2:wireless_crafting_terminal',snbt='{accessPoint:{dimension:"minecraft:overworld",pos:[I;8,65,8]},internalCurrentPower:1000000.0d}',ticks=10)
s=f.S('use-item',ticks=20);w=f.win(s);assert w['nativeScreen'].endswith('.WCTScreen');assert not w.get('layoutError');assert not any('PlayerEntityWidget' in a['type'] for a in w['nativeWidgets']);f.save(loader+'-crafting03',s)
print(loader,[(w['menuId'],w['bounds']) for w in s['allWindows']]);print([(v['index'],v['x'],v['y']) for v in w['slots'][43:53]]);print([(a.get('message'),a['x'],a['y']) for a in w['nativeWidgets'] if 'Trash' in a.get('message','') or 'Settings' in a.get('message','')])
(f.OUT/(loader+'-setup-log.json')).write_text(json.dumps(f.LOG,indent=2))
