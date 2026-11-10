"""Focused native Aircraft slot, cargo and window-lifetime checks."""
from pathlib import Path
import sys,json,os
ROOT=Path(__file__).resolve().parents[2]
sys.path.insert(0,str(ROOT/'functional-tests/project-everything-ten'))
from send import send
RUN=ROOT/'build/remaining-ui-audit/gameplay'/os.environ.get('AIRCRAFT_RUN','1.20.1-fabric-integrated-aircraft-window03')
OUT=ROOT/os.environ.get('AIRCRAFT_EVIDENCE','build/aircraft-window/evidence');OUT.mkdir(parents=True,exist_ok=True)
def act(kind='capture',**kw):return send(RUN,kind,ticks=kw.pop('ticks',4),**kw)['client']
def command(s,**kw):return act('command',command=s,**kw)
def click(x,y,button=0):
    act('click',x=x,y=y,button=button,ticks=1);return act('release',x=x,y=y,button=button)
def window(zone='MAIN',state=None):return next(w for w in (state or act())['allWindows'] if w.get('aircraftZone')==zone)
def cell(zone='MAIN',source=None,kind=None,last=False):
    w=window(zone);cells=w['aircraftCells'];cells=[c for c in cells if (source is None or c['sourceIndex']==source) and (kind is None or c['kind']==kind)]
    return cells[-1 if last else 0]
def position(zone='MAIN',**kw):
    w=window(zone);c=cell(zone,**kw);b=w['bounds'];return b['x']+(b['width']-w['aircraftBodyWidth'])//2+c['x']+8,w['aircraftContentY']+c['y']+8
def slot(zone='MAIN',**kw):return click(*position(zone,**kw))
def inventory(index=9):
    s=act();w=next(w for w in s['workspaceWindows'] if w['kind']=='INVENTORY');c=next(c for c in w['inventoryCells'] if c['inventorySlot']==index);return click(c['x'],c['y'])
def item(name,count=1,index=9):return command(f'item replace entity @a inventory.{index-9} with {name} {count}')
def show_inventory():
    act('inventory');w=next(w for w in act()['workspaceWindows'] if w['kind']=='INVENTORY')
    if w['locked']=='true':click(**w['controls']['LOCK'])
    w=next(w for w in act()['workspaceWindows'] if w['kind']=='INVENTORY');b=w['bounds'];s=act();x=b['x']+20;y=b['y']+7;dx=s['guiWidth']//2-b['width']//2-b['x'];dy=min(320,s['guiHeight']-b['height']-8)-b['y']
    act('click',x=x,y=y,button=0,ticks=1);act('drag',x=x+dx,y=y+dy,dx=dx,dy=dy,button=0,ticks=1);return act('release',x=x+dx,y=y+dy,button=0)
def server(name=None):
    act('snapshot-server',ticks=1);d=json.loads((RUN/'server-state.json').read_text());act('aircraft-state',ticks=1);d['aircraft']=json.loads((RUN/'aircraft-state.json').read_text())
    if name:(OUT/(name+'.json')).write_text(json.dumps({'client':act(),'server':d},indent=2))
    return d
def check(name,passed,details=None):
    f=OUT/'checks.json';r=json.loads(f.read_text()) if f.exists() else [];r.append({'name':name,'passed':bool(passed),'details':details});f.write_text(json.dumps(r,indent=2));print(('PASS ' if passed else 'FAIL ')+name,flush=True);assert passed,(name,details)
def capture(name):
    name=name.replace('_','-');act('move',x=630,y=440);s=act(capture=name);(OUT/(name+'.json')).write_text(json.dumps(s,indent=2));return s
def open_aircraft(kind='cargo_airship'):
    act('close');command('kill @e[type=!minecraft:player]');command(f'summon immersive_aircraft:{kind} 8.5 65 8.5 {{NoGravity:1b,Invulnerable:1b}}');command('tp @a 8.5 65 10.5 180 8');act('aim-entity',entity='immersive_aircraft:'+kind);s=act('use-entity-ray',entity='immersive_aircraft:'+kind,shift=True,ticks=18);act('sneak',down=False);return s
