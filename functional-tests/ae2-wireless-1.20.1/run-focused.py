import sys,json
from pathlib import Path
import focused as f
if len(sys.argv)!=3:raise SystemExit('Usage: '+sys.argv[0]+' fabric|forge <disposable-production-observer-output>')
loader=sys.argv[1];f.RUN=Path(sys.argv[2]).resolve()
REPORT=[]
def observe():return f.S('wait',ticks=12)
def picture(n,s):f.save(loader+'-'+n,s)
def record(n,s):
 REPORT.append({'check':n,'pass':True,'window':f.win(s)['menuId'],'bounds':f.win(s)['bounds']});(f.OUT/(loader+'-final-checks.json')).write_text(json.dumps(REPORT,indent=2));(f.OUT/(loader+'-final-log.json')).write_text(json.dumps(f.LOG,indent=2))
def press(s,name):
 a=next(a for a in f.win(s)['nativeWidgets'] if a.get('visible') and a.get('message','').split('\n')[0]==name);return f.tap(a['x']+a['width']/2,a['y']+a['height']/2)
def inventory(s):return s if any(w['kind']=='INVENTORY' for w in s['workspaceWindows']) else f.S('inventory',ticks=30)
def open_item(name,tag=''):
 f.S('close',ticks=12);f.S('held',item=name,snbt='{accessPoint:{dimension:"minecraft:overworld",pos:[I;8,65,8]},internalCurrentPower:1000000.0d'+tag+'}',ticks=15);return f.S('use-item',ticks=30)
s=observe();assert f.win(s)['nativeScreen'].endswith('WCTScreen');s=inventory(s)
f.cmd('item replace entity @a inventory.0 with minecraft:oak_log 2',15);s=observe();s=f.itap(s,9);s=f.slot(f.win(s),43)
if f.win(s)['slots'][43]['item']=='minecraft:air':s=f.itap(s,9);s=f.slot(f.win(s),43)
assert f.win(s)['slots'][52]['item']=='minecraft:oak_planks';s=f.slot(f.win(s),52);s=f.itap(s,10);s=observe();assert 'oak_planks' in f.inv(s,10)['item'];record('Craft recipe and transfer native output',s);picture('crafting',s)
s=press(s,'Trash');assert f.win(s)['menuId']=='ae2:trash';assert len([a for a in f.win(s)['slots'] if not a['player']])==27;picture('trash',s)
a=next(a for a in f.win(s)['nativeWidgets'] if a.get('message')=='Wireless Crafting Terminal');assert a['x']>f.win(s)['bounds']['x']+140;s=press(s,'Wireless Crafting Terminal');assert f.win(s)['nativeScreen'].endswith('WCTScreen');record('27 Trash slots and native top-right return',s)
s=open_item('ae2:wireless_crafting_terminal',',upgrades:[{Slot:0b,id:"ae2wtlib:magnet_card",Count:1b}]');s=inventory(s)
if loader=='forge':
 s=press(s,'Wireless Terminal Settings');picture('wireless-settings',s);s=press(s,'Wireless Crafting Terminal');assert f.win(s)['nativeScreen'].endswith('WCTScreen');record('Native wireless settings and same-instance return',s)
s=press(s,'Magnet Filter');assert f.win(s)['menuId']=='ae2:magnet';assert len([a for a in f.win(s)['slots'] if not a['player']])==54;picture('magnet-filter',s);s=press(s,'Wireless Crafting Terminal');record('54 filter slots and native return',s)
s=open_item('ae2wtlib:wireless_pattern_encoding_terminal');s=inventory(s);assert f.win(s)['nativeScreen'].endswith('WETScreen')
f.cmd('item replace entity @a inventory.1 with ae2:blank_pattern 4',15);s=observe();s=f.itap(s,10);s=f.slot(f.win(s),165);assert f.win(s)['slots'][165]['item']=='ae2:blank_pattern'
f.cmd('item replace entity @a inventory.1 with minecraft:oak_log 1',15);s=observe();s=f.itap(s,10);s=f.slot(f.win(s),43);s=f.itap(s,10);assert f.win(s)['slots'][52]['item']=='minecraft:oak_planks';s=press(s,'Encode Pattern');assert f.win(s)['slots'][166]['item']=='ae2:crafting_pattern';picture('pattern-encoding',s)
s=f.slot(f.win(s),166);s=f.itap(s,11);assert 'ae2:crafting_pattern' in f.inv(s,11)['item'];record('Native blank pattern consumed and encoded recipe transferred',s)
for label in ['Processing Patterns','Smithing Table Patterns','Stonecutting Patterns','Crafting Patterns']:
 s=press(s,label);picture('encoding-'+label.split()[0].lower(),s)
record('Four native encoding modes',s)
f.S('close',ticks=12);f.cmd('setblock 10 65 9 ae2:cable_bus{cable:{id:"ae2:fluix_glass_cable"}}',6);f.cmd('setblock 10 65 8 ae2:pattern_provider',12);f.cmd('tp @a 10.5 65 11.5 180 20',20);s=f.S('use-block-ray',ticks=30);assert f.win(s)['menuId']=='ae2:pattern_provider';s=inventory(s);s=f.itap(s,11);s=f.slot(f.win(s),36);assert f.win(s)['slots'][36]['item']=='ae2:crafting_pattern'
s=open_item('ae2wtlib:wireless_pattern_access_terminal');s=inventory(s);assert f.win(s)['nativeScreen'].endswith('WATScreen');s=observe();w=f.win(s);item=next(a for a in w['slots'] if a['item']=='ae2:crafting_pattern' and not a['player']);index=item['index'];picture('pattern-access',s)
s=f.slot(w,index);s=f.itap(s,12);s=observe()
if 'ae2:crafting_pattern' not in f.inv(s,12)['item']:s=f.itap(s,12);s=observe()
assert 'ae2:crafting_pattern' in f.inv(s,12)['item'],f.inv(s,12);record('Pattern Access withdraws through native provider action',s)
s=f.itap(s,12);s=f.slot(f.win(s),index);s=observe();assert f.win(s)['slots'][index]['item']=='ae2:crafting_pattern';record('Pattern Access reinserts through native provider action',s);picture('pattern-access',s)
print(loader,'FINAL PASS',len(REPORT),'checks')
