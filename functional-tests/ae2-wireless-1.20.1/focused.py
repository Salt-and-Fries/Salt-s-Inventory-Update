from pathlib import Path
import json,sys
ROOT=Path(__file__).resolve().parents[2]
sys.path.insert(0,str(ROOT/'functional-tests/all-target-rollout'))
from evidence import send
RUN=ROOT/'build/remaining-ui-audit/gameplay/1.20.1-fabric-integrated-wireless02'
OUT=ROOT/'build/ae2-wireless-implementation';OUT.mkdir(parents=True,exist_ok=True);LOG=[]
def S(kind,**args):
 reply=send(RUN,kind,**args);s=reply['client'];LOG.append({'command':kind,'args':args,'client':s})
 (OUT/'latest-observations.json').write_text(json.dumps(LOG,indent=2));return s
def cmd(t,ticks=8):return S('command',command=t,ticks=ticks)
def tap(x,y,b=0):
 S('move',x=x,y=y,ticks=2);S('click',x=x,y=y,button=b,ticks=1);return S('release',x=x,y=y,button=b,ticks=7)
def win(s,id=None):return next(w for w in s['allWindows'] if id is None or w['menuId']==id)
def native(w,x,y,b=0):a=w['bounds'];return tap(a['x']+x,a['y']+y,b)
def slot(w,n,b=0):s=w['slots'][n];return native(w,s['x']+7,s['y']+7,b)
def inv(s,n=9):return next(c for w in s['workspaceWindows'] if w['kind']=='INVENTORY' for c in w['inventoryCells'] if c['inventorySlot']==n)
def itap(s,n=9,b=0):c=inv(s,n);return tap(c['x'],c['y'],b)
def save(name,s):
 S('move',x=2,y=2,ticks=3);S('capture',capture=name,ticks=3)
 (OUT/(name+'.json')).write_text(json.dumps({'state':s,'run':RUN.name,'capture':str(RUN/'screenshots'/(name+'.png'))},indent=2))
