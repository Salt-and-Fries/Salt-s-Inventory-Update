import sys,json
from pathlib import Path
import focused as f
if len(sys.argv)!=3:raise SystemExit('Usage: '+sys.argv[0]+' fabric|forge <disposable-production-observer-output>')
loader=sys.argv[1];f.RUN=Path(sys.argv[2]).resolve()
f.S('close',ticks=12);f.cmd('setblock 10 65 9 ae2:cable_bus{cable:{id:"ae2:fluix_glass_cable"}}',6)
f.cmd('setblock 10 65 8 ae2:pattern_provider{patterns:[{Count:1b,Slot:0,id:"ae2:crafting_pattern",tag:{in:[{Count:1b,id:"minecraft:oak_log"},{},{},{},{},{},{},{},{}],out:{Count:4b,id:"minecraft:oak_planks"},recipe:"minecraft:oak_planks",substitute:0b,substituteFluids:0b}}]}',15)
f.S('held',item='ae2wtlib:wireless_pattern_access_terminal',snbt='{accessPoint:{dimension:"minecraft:overworld",pos:[I;8,65,8]},internalCurrentPower:1000000.0d}',ticks=15);s=f.S('use-item',ticks=30)
if not any(w['kind']=='INVENTORY' for w in s['workspaceWindows']):s=f.S('inventory',ticks=30)
w=f.win(s);assert w['nativeScreen'].endswith('WATScreen');index=next(a['index'] for a in w['slots'] if not a['player'] and a['item']=='ae2:crafting_pattern');f.save(loader+'-pattern-access',s)
s=f.slot(w,index);s=f.S('wait',ticks=15);print(loader,'provider after native click',f.win(s)['slots'][index]);assert f.win(s)['slots'][index]['item']=='minecraft:air'
s=f.itap(s,12);s=f.S('wait',ticks=15)
if 'ae2:crafting_pattern' not in f.inv(s,12)['item']:s=f.itap(s,12);s=f.S('wait',ticks=15)
assert 'ae2:crafting_pattern' in f.inv(s,12)['item'],f.inv(s,12)
s=f.itap(s,12);s=f.slot(f.win(s),index);s=f.S('wait',ticks=15);assert f.win(s)['slots'][index]['item']=='ae2:crafting_pattern'
f.save(loader+'-pattern-access',s);(f.OUT/(loader+'-provider-regression-log.json')).write_text(json.dumps(f.LOG,indent=2));(f.OUT/(loader+'-provider-regression.json')).write_text(json.dumps({'pass':True,'checks':['provider shown from live network','native pickup removes real provider pattern','pattern reaches real Inventory','native set-down reinserts the real pattern'],'run':f.RUN.name},indent=2));print(loader,'PROVIDER TRANSFERS PASS')
