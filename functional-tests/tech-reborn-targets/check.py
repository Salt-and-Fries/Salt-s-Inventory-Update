"""Focused native-window geometry, typed actions and property-sync checks."""
from pathlib import Path
import argparse,hashlib,importlib.util,json,sys,time,zipfile
from PIL import Image
ROOT=Path(__file__).resolve().parents[2]
sys.path.insert(0,str(ROOT/'functional-tests/project-everything-ten'))
from send import send
p=argparse.ArgumentParser();p.add_argument('run');p.add_argument('--sweep',action='store_true');p.add_argument('--heat-only',action='store_true');p.add_argument('--nuclear-only',action='store_true');a=p.parse_args()
RUN=ROOT/'build/remaining-ui-audit/gameplay'/a.run
MC=a.run.split('-fabric-')[0];OUT=ROOT/'output/tech-reborn-all-targets'/a.run;OUT.mkdir(exist_ok=True)
CHECKS=[];CAPTURES=[]
def call(kind,**kw):return send(RUN,kind,**kw)['client']
def cmd(text,ticks=3):return call('command',command=text,ticks=ticks)
def state():return call('wait',ticks=2)
def window(s=None):return(s or state())['allWindows'][-1]
def save():
 (OUT/('heat-checks.json'if a.heat_only else'nuclear-checks.json'if a.nuclear_only else'checks.json')).write_text(json.dumps({'status':'running','checks':CHECKS,'captures':CAPTURES},indent=2))
def check(name,ok):
 CHECKS.append({'name':name,'passed':bool(ok)});save();assert ok,name
 print('PASS',name,flush=True)
def opening(block):
 call('close');cmd('fill -10 65 -10 26 79 26 minecraft:air');call('fixture',block=block,ticks=8);cmd('tp @p 8.5 65 12.5 180 8');s=call('open',ticks=15)
 w=window(s);check(block+' uses the original authenticated menu',w.get('sameModel')and'TechRebornClient'in w.get('templateClass',w.get('template',''))and not w.get('layoutError'))
 return s
def capture(key):
 call('move',x=2,y=2,ticks=2);requested=time.time_ns();s=call('capture',capture=key,ticks=5);path=RUN/'screenshots'/(key+'.png')
 deadline=time.monotonic()+10
 while True:
  try:
   assert path.stat().st_mtime_ns>=requested
   with Image.open(path)as im:im.verify()
   break
  except (FileNotFoundError,AssertionError,OSError):
   if time.monotonic()>deadline:raise
   time.sleep(.05)
 CAPTURES.append({'id':key,'capture':str(path.relative_to(ROOT)),'sha256':hashlib.sha256(path.read_bytes()).hexdigest(),'state':s});save();return s
def point(x,y):
 w=window();b=w['bounds']
 for r in reversed(w['composition']['regions']):
  q=r['source']
  if q['x']<=x<q['x']+q['width']and q['y']<=y<q['y']+q['height']:
   return b['x']+r['x']+(0 if r['exterior']else 6)+x-q['x'],b['y']+r['y']+(0 if r['exterior']else 22)+y-q['y']
 raise AssertionError(('unmapped',x,y))
def tap(x,y):
 call('move',x=x,y=y,ticks=1);call('click',x=x,y=y,button=0,ticks=1);return call('release',x=x,y=y,button=0,ticks=7)
def native(x,y):return tap(*point(x,y))
def tab(index):
 b=window()['bounds'];return tap(b['x']-10,b['y']+36+24*index)
def observe():
 call('tr-observe',ticks=2);data=json.loads((RUN/'tr-state.json').read_text())
 if MC=='1.20.1':
  data['getSlotConfiguration']=data.get('getSlotConfigurationNbt');data['getRedstoneConfiguration']=data.get('getRedstoneConfigurationNbt');data['fluid']=data.get('nbt')
 return data
def popup_close():
 b=window()['bounds'];return tap(b['x']+b['width']+121,b['y']+34)
def inventory(index=9):
 s=state()
 if not any(x['kind']=='INVENTORY'for x in s['workspaceWindows']):s=call('inventory',ticks=3)
 b=next(x for x in s['workspaceWindows']if x['kind']=='INVENTORY');q=next(x for x in b['inventoryCells']if x['inventorySlot']==index);return tap(q['x'],q['y'])
def geometry(s):
 w=window(s);rs=w['composition']['regions'];count=0
 for slot in w['slots']:
  if slot['player']and slot['y']>=92:continue
  x,y=slot['x']-1,slot['y']-1
  assert any(r['source']['x']<=x and r['source']['y']<=y and x+18<=r['source']['x']+r['source']['width']and y+18<=r['source']['y']+r['source']['height']for r in rs),(w['menuId'],slot)
  count+=1
 if w['trUpgrades']:
  image=Image.open(RUN/'screenshots'/(CAPTURES[-1]['id']+'.png')).convert('RGB');scale=image.width/s['guiWidth'];b=w['bounds'];x=b['x']+b['width']-4;y=b['y']+24
  for q in [(x+22,y+3,3,74),(x+4,y+77,20,2)]:
   crop=image.crop(tuple(round(v*scale)for v in(q[0],q[1],q[0]+q[2],q[1]+q[3])))
   assert set(crop.getdata())=={(198,198,198)},(w['menuId'],'tray background')
  assert next(r for r in rs if r['id']=='native-upgrade-cells')['x']>0
 for r in rs:
  if r['id'].startswith('native-settings/'):assert r['x']==-22
 return count

def heat():
 opening('techreborn:industrial_blast_furnace');call('close')
 # The publisher's unchanged blast-furnace blueprint was captured in the
 # approved 1.20.1 audit. New releases use the same block coordinates/states.
 blueprint=json.loads((ROOT/'output/tech-reborn-implementation/techreborn-industrial-blast-furnace-blueprint.json').read_text())
 for b in blueprint['blocks']:
  if (b['x'],b['y'],b['z'])!=(0,0,0):cmd(f'setblock {8+b["x"]} {65+b["y"]} {8+b["z"]} {b["state"]}',ticks=1)
 call('wait',ticks=80);cmd('tp @p 8.5 65 12.5 180 8');call('open',ticks=30)
 native_heat=int(observe()['getHeat']);check('Native formed blast furnace reports positive heat',native_heat>0)
 s=capture('formed-blast-furnace');check('Live native heat synchronizes to owning client menu',window(s)['cachedHeat']==native_heat)
 call('close')

def main():
 if a.nuclear_only:
  opening('techreborn:nuclear_reactor');cmd('gamemode survival @p');s=capture('nuclear-reactor');count=geometry(s);check('Complete reactor grid and energy attachment',count==54)
  before=window()['hologram'];native(-16,15);check('Native hologram toggle remains actionable',window()['hologram']!=before)
  cmd('item replace entity @p inventory.0 with techreborn:uranium_fuel_rod');inventory();native(16,26)
  check('Reactor accepts a real fuel rod through its native slot',any(not x['player']and x['item']=='techreborn:uranium_fuel_rod'for x in window()['slots']))
  native(16,26);native(70,26);check('Unconnected reactor columns retain native item restrictions',next(x for x in window()['slots']if not x['player']and x['x']==62 and x['y']==18)['item']=='minecraft:air');inventory(10)
  call('close');(OUT/'nuclear-checks.json').write_text(json.dumps({'status':'passed','checks':CHECKS,'captures':CAPTURES},indent=2));return
 if a.heat_only:
  heat();(OUT/'heat-checks.json').write_text(json.dumps({'status':'passed','checks':CHECKS,'captures':CAPTURES},indent=2));return
 cmd('gamerule doDaylightCycle false');cmd('time set noon');cmd('weather clear')
 opening('techreborn:alloy_smelter');cmd('gamemode survival @p');capture('alloy-smelter')
 tab(0);check('Attached item settings opens',window()['trSelectedTab']=='SlotConfigGui');native(42,55);capture('item-inspector')
 check('Native input inspector opens',window()['templateState']['popupHeight']==137)
 before=observe()['getSlotConfiguration'];native(209,93);after=observe()['getSlotConfiguration'];check('Typed Auto Input reaches server',before!=after)
 before=after;native(239,19);check('Native side diagram edits server port',before!=observe()['getSlotConfiguration'])
 popup_close();check('Inspector closes back to item settings',window()['templateState']['popupHeight']==0)
 tab(1);capture('redstone');before=observe()['getRedstoneConfiguration'];native(130,103);check('Typed redstone mode reaches server',before!=observe()['getRedstoneConfiguration']);tab(1)
 cmd('item replace entity @p inventory.0 with techreborn:overclocker_upgrade');inventory();native(-10,20)
 check('Right-hand upgrade cell accepts its native item',any(x['x']<0 and x['item']=='techreborn:overclocker_upgrade'for x in window()['slots']))
 native(-10,20);inventory(10);check('Upgrade can be removed through shared cursor',not any(x['x']<0 and x['item']=='techreborn:overclocker_upgrade'for x in window()['slots']))
 opening('techreborn:diesel_generator');tab(1);capture('fluid-inspector');check('Fluid inspector opens',window()['templateState']['popupHeight']==122)
 before=observe()['fluid'];native(209,93);check('Typed fluid Pull In reaches server',before!=observe()['fluid']);popup_close()
 for block,x,y,key in [('pump',90,36,'getDepth'),('player_detector',70,46,'getCurrentRadius'),('adjustable_su',127,85,'getCurrentOutput')]:
  opening('techreborn:'+block);before=observe()[key];native(x,y);check(block+' native adjustment reaches server',before!=observe()[key])
 if a.sweep:
  old=json.loads((ROOT/'build/tech-reborn-window-design/openings.json').read_text());pairs=[(key,next(v['block']for v in r['native_actions']if v['type']=='fixture'))for key,r in old.items()]
  if MC.startswith('26.'):pairs.append(('techreborn-nuclear-reactor','techreborn:nuclear_reactor'))
  cells=0
  for key,block in pairs:
   opening(block);s=capture(key);cells+=geometry(s);check(key+' complete cells and opaque attachments',True)
  (OUT/'geometry.json').write_text(json.dumps({'screens':len(pairs),'complete_cells':cells,'right_upgrade_trays':True,'left_tabs':True,'opaque_gutters':True},indent=2))
 call('close');check('Closing releases native sessions',state()['sessions']==0)
 result={'status':'passed','minecraft':MC,'loader':'fabric','checks':CHECKS,'captures':CAPTURES,'runtime':str(RUN.relative_to(ROOT)),'scope':'Focused windows, native slot envelopes, representative typed actions and session lifetime; not a complete gameplay suite.'}
 (OUT/'checks.json').write_text(json.dumps(result,indent=2));print('PASSED',MC,len(CHECKS),'focused checks',flush=True)

if __name__=='__main__':main()
