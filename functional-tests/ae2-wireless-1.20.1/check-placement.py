from pathlib import Path
import sys,json
import focused as f
if len(sys.argv)!=3:raise SystemExit('Usage: '+sys.argv[0]+' fabric|forge <disposable-production-observer-output>')
loader=sys.argv[1];f.RUN=Path(sys.argv[2]).resolve();f.OUT=f.ROOT/'build/ae2-wireless-button-placement';f.OUT.mkdir(exist_ok=True);f.LOG=[]
checks=[]
def check(name,condition):
 assert condition,name;checks.append({'name':name,'pass':True});(f.OUT/(loader+'-checks.json')).write_text(json.dumps(checks,indent=2))
def button(s,name):return next(a for a in f.win(s)['nativeWidgets'] if a.get('visible') and a.get('message','').split('\n')[0]==name)
def press(s,name):
 a=button(s,name);return f.tap(a['x']+a['width']/2,a['y']+a['height']/2)
def observe():return f.S('wait',ticks=12)
f.S('fixture',block='ae2:wireless_access_point',ticks=10);f.cmd('gamemode survival @a');f.cmd('setblock 8 65 9 ae2:creative_energy_cell',20)
f.S('held',item='ae2:wireless_crafting_terminal',snbt='{accessPoint:{dimension:"minecraft:overworld",pos:[I;8,65,8]},internalCurrentPower:1000000.0d}',ticks=15);s=f.S('use-item',ticks=25)
check('Dedicated WCT window',f.win(s)['nativeScreen'].endswith('WCTScreen'))
check('Magnet Filter hidden without card',not any(a.get('visible') and a.get('message')=='Magnet Filter' for a in f.win(s)['nativeWidgets']))
if not any(w['kind']=='INVENTORY' for w in s['workspaceWindows']):s=f.S('inventory',ticks=30)
f.cmd('item replace entity @a inventory.0 with ae2wtlib:magnet_card',12);s=observe();s=f.itap(s,9);s=f.slot(f.win(s),5);s=observe();check('Native card installation',f.win(s)['slots'][5]['item']=='ae2wtlib:magnet_card')
w=f.win(s);b=w['bounds'];original=json.loads((f.ROOT/'output/ae2-wireless-implemented'/('fabric-original-magnet-buttons.json' if loader=='fabric' else 'forge-original-wireless-crafting.json')).read_text())['state']
if loader=='fabric':
 labels=['Magnet Card','Magnet Filter','Trash'];native=[button(s,n) for n in labels];ref=[next(x for x in original['widgets'] if x.get('visible') and x.get('message','').split('\n')[0]==n) for n in labels]
 check('Native left toolbar placement',all(a['x']<b['x'] for a in native));check('Native vertical button order',list(sorted(native,key=lambda a:a['y']))==native);check('Single toolbar column',len({a['x'] for a in w['nativeWidgets'] if a.get('visible') and a['x']<b['x']})==1)
 check('Toolbar fully visible',all(a['y']+a['height']<=b['y']+b['height'] for a in native))
 for mode in ['PICKUP_INVENTORY','PICKUP_ME','OFF']:
  s=press(s,'Magnet Card');check('Native mode '+mode,f.win(s)['nativeValues']['magnetMode']==mode)
else:
 labels=['Wireless Terminal Settings','Magnet Filter','Trash'];native=[button(s,n) for n in labels];ref=[next(x for x in original['widgets'] if x.get('visible') and x.get('message','').split('\n')[0]==n) for n in labels]
 check('Native footer button order',list(sorted(native,key=lambda a:a['x']))==native);check('Native horizontal spacing',[native[i+1]['x']-native[i]['x'] for i in range(2)]==[ref[i+1]['x']-ref[i]['x'] for i in range(2)])
 result=w['slots'][52];check('Buttons below output',all(a['y']>=b['y']+result['y']+16 for a in native));check('Buttons fit compact footer',all(a['x']+a['width']<=b['x']+172 and a['y']+a['height']<=b['y']+b['height']-4 for a in native))
 s=press(s,'Wireless Terminal Settings');check('Wireless settings opens',f.win(s)['nativeScreen'].endswith('WirelessTerminalSettingsScreen'));s=press(s,'Wireless Crafting Terminal');check('Wireless settings return',f.win(s)['nativeScreen'].endswith('WCTScreen'))
s=press(s,'Magnet Filter');check('Filter opens',f.win(s)['menuId']=='ae2:magnet');s=press(s,'Wireless Crafting Terminal');check('Filter returns',f.win(s)['nativeScreen'].endswith('WCTScreen'))
s=press(s,'Trash');check('Trash opens',f.win(s)['menuId']=='ae2:trash');s=press(s,'Wireless Crafting Terminal');check('Trash returns',f.win(s)['nativeScreen'].endswith('WCTScreen'))
# Close only the local Inventory panel to expose the entire crafting footer.
inv=next((w for w in s['workspaceWindows'] if w['kind']=='INVENTORY'),None)
if inv:
 a=inv['controls']['CLOSE'];s=f.tap(a['x'],a['y'])
f.save(loader+'-native-button-placement',s)
(f.OUT/(loader+'-observations.json')).write_text(json.dumps(f.LOG,indent=2));print(loader,'PASS',len(checks),'focused checks',f.win(s)['bounds'])
