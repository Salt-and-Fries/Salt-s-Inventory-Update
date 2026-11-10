"""Focused production-world Accessories probes. All item actions use real UI input."""
import json,sys,math,os
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
sys.path.insert(0,str(ROOT/'functional-tests/project-everything-ten'))
from send import send
RUN=ROOT/'build/remaining-ui-audit/gameplay'/os.environ.get('ACCESSORIES_RUN','1.20.1-fabric-integrated-accessories-window04')
OUT=ROOT/'build/accessories-window/evidence';OUT.mkdir(parents=True,exist_ok=True)
def act(kind='capture',**kw):return send(RUN,kind,**kw)['client']
def click(x,y,button=0):
    act('click',x=x,y=y,button=button);return act('release',x=x,y=y,button=button)
def character(s=None):return next(w for w in (s or act())['workspaceWindows'] if w['kind']=='CHARACTER')
def panel(s=None):return next(w for w in (s or act())['allWindows'] if w['menuId'].endswith(':accessories_equipment'))
def button():
    b=character()['bounds'];return click(b['x']+32,b['y']+30)
def command(text,**kw):return act('command',command=text,**kw)
def server(name=None):
    act('snapshot-server',ticks=1);d=json.loads((RUN/'server-state.json').read_text())
    act('accessories-state',ticks=1);d['accessories']=json.loads((RUN/'accessories-state.json').read_text())
    if name:(OUT/(name+'.json')).write_text(json.dumps({'client':act(),'server':d},indent=2))
    return d
def cursor():return server()['menus'][0]['carried']
def slotpos(name,index=0,cosmetic=False):
    w=panel();cell=next(c for c in w['accessoriesCells'] if c['name']==name and c['index']==index and c['cosmetic']==cosmetic)
    b=w['bounds'];pairs=len(w['slots'])//2;visible=max(1,(b['height']-14)//20);cols=max(1,(b['width']-7)//42)
    if math.ceil(pairs/cols)>visible:cols=max(1,(b['width']-18)//42)
    pair=cell['slot']//2;row=pair//cols;scroll=w['accessoriesScroll']
    if row<scroll or row>=scroll+visible:
        act('scroll',x=b['x']+15,y=b['y']+15,amount=scroll-row);return slotpos(name,index,cosmetic)
    return b['x']+15+(pair%cols)*42+(19 if cosmetic else 0),b['y']+16+(row-scroll)*20
def slot(name,index=0,cosmetic=False,mouse=0):return click(*slotpos(name,index,cosmetic),button=mouse)
def inventory(index):
    s=act()
    if index<9:return click(s['guiWidth']//2-90+9+index*20,s['guiHeight']-11)
    w=next(w for w in s['workspaceWindows'] if w['kind']=='INVENTORY');cell=next(c for c in w['inventoryCells'] if c['inventorySlot']==index);return click(cell['x'],cell['y'])
def check(name,passed,details=None):
    path=OUT/'checks.json';rows=json.loads(path.read_text()) if path.exists() else [];rows.append(dict(name=name,passed=bool(passed),details=details));path.write_text(json.dumps(rows,indent=2));print(('PASS ' if passed else 'FAIL ')+name,flush=True);assert passed,(name,details)
def capture(name):
    s=act(capture=name);(OUT/(name+'.json')).write_text(json.dumps(s,indent=2));return s
def fixture_item(item,slot_name,nbt=''):
    command('item replace entity @a weapon.mainhand with minecraft:'+item+nbt)
    command('execute as @a run accessories slot add valid '+slot_name)
