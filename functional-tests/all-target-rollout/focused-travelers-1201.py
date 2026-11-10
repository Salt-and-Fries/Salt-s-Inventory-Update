"""Affected 1.20.1 packet actions, settings and exact storage recovery only."""
from pathlib import Path
import argparse,json,re,sys
sys.path.insert(0,str(Path(__file__).parent))
from evidence import send
p=argparse.ArgumentParser();p.add_argument('run',type=Path);a=p.parse_args();checks=[]
def act(kind,**kw):return send(a.run,kind,**kw)['client']
def check(name,ok):
 checks.append({'check':name,'passed':bool(ok)});(a.run/'travelers-packets-focused-checks.json').write_text(json.dumps({'scope':'Held backpack storage, tool action, settings/unsortables and upgrade filter/tank actions; not a complete gameplay suite','checks':checks},indent=2));assert ok,name;print('PASS',name,flush=True)
def window(s):return next(w for w in s['windows'] if w.get('menuId','').startswith('travelersbackpack:'))
def snapshot():
 act('snapshot-server',ticks=3);return json.loads((a.run/'server-state.json').read_text())
def held(server):return next(v['item'] for v in server['inventory'] if v['index']==server['selected'])
def count(nbt):
 if 'minecraft:air' in nbt:return 0
 m=re.search(r'(?:Count|count):\s*(\d+)',nbt);return int(m[1]) if m else 0
def native_point(s,x,y):
 w=window(s);r=next(r for r in w['template']['regions'] if r['source']['x']<=x<r['source']['x']+r['source']['width'] and r['source']['y']<=y<r['source']['y']+r['source']['height']);b=w['bounds'];src=r['source']
 return b['x']+(0 if r['exterior'] else 8)+r['x']+x-src['x'],b['y']+(0 if r['exterior'] else 24)+r['y']+y-src['y']
def click_native(s,x,y):return act('xy-click',x=native_point(s,x,y)[0],y=native_point(s,x,y)[1],ticks=12)
def widget(s,name,dx=5,dy=5):
 w=window(s);v=next(v for v in w['nativeValues']['customWidgets'] if v['type']==name);b=w['nativeBounds'];return click_native(s,v['bounds'][0]-b['x']+dx,v['bounds'][1]-b['y']+dy)
def slot(s,index):
 v=next(v for v in window(s)['slots'] if v['index']==index);return click_native(s,v['x']+8,v['y']+8)
def menu(server):return next(m for m in server['menus'] if m['class'].startswith('com.tiviacz.travelersbackpack.') and 'Menu' in m['class'])
def open_item(upgrade=None):
 act('close',ticks=3);act('fixture',block='minecraft:stone',ticks=8);act('command',command='gamemode survival @a',ticks=4)
 tag='' if not upgrade else '{Tier:0,StorageSlots:27,UpgradeSlots:2,ToolSlots:2,Upgrades:{Size:2,Items:[{Slot:0,id:"travelersbackpack:'+upgrade+'_upgrade",Count:1b,tag:{TabOpen:0b}}]}}'
 act('command',command='item replace entity @a weapon.mainhand with travelersbackpack:standard'+tag,ticks=5)
 return act('client-use-item',ticks=20)
s=open_item();check('native-template-active',window(s)['templateClass'].endswith('TravelersWindows$Template'))
s=widget(s,'ToolSlotsWidget');check('tool-action-opens-drawer',any(r['id']=='tool-drawer' for r in window(s)['template']['regions']))
s=widget(s,'ToolSlotsWidget');check('tool-action-closes-drawer',not any(r['id']=='tool-drawer' for r in window(s)['template']['regions']))
act('command',command='item replace entity @a inventory.0 with minecraft:diamond 7',ticks=4)
if not any(w['kind']=='INVENTORY' for w in s['workspaceWindows']):s=act('inventory',ticks=8)
inv=next(w for w in s['workspaceWindows'] if w['kind']=='INVENTORY');v=next(v for v in inv['inventoryCells'] if v['inventorySlot']==9);s=act('xy-click',x=v['x'],y=v['y'],ticks=10)
s=slot(s,0);server=snapshot();stored=menu(server)['slots'][0]['item'];check('exact-seven-diamonds-enter-storage','minecraft:diamond' in stored and count(stored)==7 and all(count(m['carried'])==0 for m in server['menus']))
s=slot(s,0);inv=next(w for w in s['workspaceWindows'] if w['kind']=='INVENTORY');v=next(v for v in inv['inventoryCells'] if v['inventorySlot']==10);s=act('xy-click',x=v['x'],y=v['y'],ticks=10);server=snapshot();stored=next(v['item'] for v in server['inventory'] if v['index']==10);check('exact-seven-return-to-separate-inventory','minecraft:diamond' in stored and count(stored)==7 and count(menu(server)['slots'][0]['item'])==0)
s=widget(s,'SettingsWidget');check('original-settings-opens',window(s)['menuId']=='travelersbackpack:backpack_settings')
s=widget(s,'UnsortablesWidget');s=slot(s,0);server=snapshot();check('settings-slot-action-persists','UnsortableSlots:[0]' in held(server))
s=widget(s,'SettingsWidget');check('settings-returns-to-backpack',window(s)['menuId']=='travelersbackpack:backpack_item')
s=open_item('pickup');s=widget(s,'AutoPickupWidget');check('pickup-upgrade-opens-native-panel',next(v for v in window(s)['nativeValues']['customWidgets'] if v['type']=='AutoPickupWidget').get('open'))
before=held(snapshot());s=widget(s,'AutoPickupWidget',12,30);after=held(snapshot());check('filter-settings-packet-persists',before!=after)
s=open_item('tanks');s=widget(s,'TankWidget');check('tank-upgrade-opens-native-panel',next(v for v in window(s)['nativeValues']['customWidgets'] if v['type']=='TankWidget').get('open'))
print('Affected backpack checks complete.',flush=True)
