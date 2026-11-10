from pathlib import Path
import sys,json
import focused as f
if len(sys.argv)!=3:raise SystemExit('Usage: '+sys.argv[0]+' fabric|forge <disposable-production-observer-output>')
loader=sys.argv[1];f.RUN=Path(sys.argv[2]).resolve()
def press(s,name):
 a=next(a for a in f.win(s)['nativeWidgets'] if a.get('visible') and a.get('message','').split('\n')[0]==name);return f.tap(a['x']+a['width']/2,a['y']+a['height']/2)
s=f.S('wait',ticks=10)
if not any(w['kind']=='INVENTORY' for w in s['workspaceWindows']):s=f.S('inventory',ticks=30)
f.cmd('item replace entity @a inventory.0 with ae2wtlib:magnet_card',12);s=f.S('wait',ticks=12);s=f.itap(s,9);s=f.slot(f.win(s),5);s=f.S('wait',ticks=15);assert f.win(s)['slots'][5]['item']=='ae2wtlib:magnet_card';f.save(loader+'-crafting',s)

s=press(s,'Trash');assert f.win(s)['menuId']=='ae2:trash';f.save(loader+'-trash',s);s=press(s,'Wireless Crafting Terminal')
s=press(s,'Magnet Filter');assert f.win(s)['menuId']=='ae2:magnet';f.save(loader+'-magnet-filter',s);s=press(s,'Wireless Crafting Terminal')
if loader=='forge':
 s=press(s,'Wireless Terminal Settings');f.save(loader+'-wireless-settings',s);s=press(s,'Wireless Crafting Terminal');assert f.win(s)['nativeScreen'].endswith('WCTScreen')
f.S('close',ticks=10);f.S('held',item='ae2wtlib:wireless_pattern_encoding_terminal',snbt='{accessPoint:{dimension:"minecraft:overworld",pos:[I;8,65,8]},internalCurrentPower:1000000.0d}',ticks=15);s=f.S('use-item',ticks=25);assert f.win(s)['nativeScreen'].endswith('WETScreen');f.save(loader+'-pattern-encoding',s)
(f.OUT/(loader+'-final-views-log.json')).write_text(json.dumps(f.LOG,indent=2));print(loader,'ALL FIVE FINAL ARTIFACT WINDOWS OPENED AND CAPTURED')
