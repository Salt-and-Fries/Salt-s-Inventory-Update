"""Confirm the live Guard model remains inside its preview when the window moves."""
from pathlib import Path
import argparse,json,sys,time
from PIL import Image
sys.path.insert(0,str(Path(__file__).parent))
from evidence import send
p=argparse.ArgumentParser();p.add_argument('run',type=Path);a=p.parse_args()
checks=[]
def act(kind,**kw):return send(a.run,kind,**kw)
def guard(state):return next(w for w in state['windows'] if w.get('menuId')=='guardvillagers:guard_screen')
def portrait_count(state,stem):
 w=guard(state);b=w['bounds']
 region=next(r for r in w['template']['regions'] if r['source']['x']<=26 and r['source']['y']<=8 and r['source']['x']+r['source']['width']>=76)
 sx=b['x']+6+region['x']+26-region['source']['x'];sy=b['y']+20+region['y']+8-region['source']['y']
 path=a.run/'screenshots'/(stem+'.png');deadline=time.monotonic()+5
 while True:
  try:image=Image.open(path).convert('RGB');break
  except (FileNotFoundError,OSError):
   if time.monotonic()>=deadline:raise
   time.sleep(.05)
 scale=image.width/state['guiWidth']
 box=tuple(round(v*scale) for v in (sx+3,sy+3,sx+47,sy+69))
 return sum(1 for r,g,b in image.crop(box).get_flattened_data() if r>40 and r>g*1.15 and g>b*1.1)
state=act('wait',ticks=1)['client']
workspace=next(w for w in state['workspaceWindows'] if w['kind']=='CONTAINER')
if workspace.get('locked')=='true':
 control=workspace['controls']['LOCK'];act('xy-click',x=control['x'],y=control['y'],ticks=6)
before=act('capture',capture='guard-model-before-move',ticks=16)['client'];b=guard(before)['bounds']
count=portrait_count(before,'guard-model-before-move');assert count>100,count
checks.append({'check':'visible-native-model-before-move','foreground_pixels':count,'passed':True})
x=b['x']+25;y=b['y']+5;dx=36;dy=18
act('click',x=x,y=y,ticks=3);act('drag',x=x+dx,y=y+dy,dx=dx,dy=dy,ticks=3);act('release',x=x+dx,y=y+dy,ticks=8)
after=act('capture',capture='guard-model-after-move',ticks=16)['client'];moved=guard(after)['bounds']
assert (moved['x'],moved['y'])==(b['x']+dx,b['y']+dy),(b,moved)
checks.append({'check':'ordinary-title-drag-moves-window','passed':True})
count=portrait_count(after,'guard-model-after-move');assert count>100,count
checks.append({'check':'native-model-stays-inside-moved-preview','foreground_pixels':count,'passed':True})
(a.run/'native-preview-move-focused-checks.json').write_text(json.dumps({'scope':'Guard live model and one ordinary title drag only','checks':checks},indent=2))
print('PASS',json.dumps(checks))
