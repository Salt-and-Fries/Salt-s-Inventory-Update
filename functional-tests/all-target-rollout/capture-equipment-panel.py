"""Reach Curios/Accessories through the actual inventory or Character button."""
from pathlib import Path
import argparse,json,sys
sys.path.insert(0,str(Path(__file__).parent))
from evidence import send,capture
p=argparse.ArgumentParser();p.add_argument('run',type=Path);p.add_argument('--side',choices=['native','siu'],required=True);p.add_argument('--minecraft',required=True);p.add_argument('--loader',required=True);p.add_argument('--mod',choices=['accessories','curios'],required=True);p.add_argument('--variant',choices=['original','experimental','legacy'],default='original');a=p.parse_args()
def act(kind,**kw):return send(a.run,kind,**kw)['client']
def tap(x,y):
 act('move',x=x,y=y,ticks=3)
 physical=a.side=='native' and a.mod=='accessories' and a.minecraft=='1.21.1'
 act('mouse-press' if physical else 'click',x=x,y=y,ticks=1)
 return act('mouse-release' if physical else 'release',x=x,y=y,ticks=16)
act('close',ticks=3);act('command',command='gamemode survival @a',ticks=4)
act('command',command='effect clear @a',ticks=3)
s=act('inventory',ticks=10)
if a.side=='native':
 if a.mod=='curios':v=next(v for v in s['widgets'] if v['type'].endswith('.CuriosButton'))
 else:
  v=next((v for v in s['widgets'] if v.get('visible') and v['width']==8 and v['height']==8),None)
  if v is None and a.minecraft=='1.21.1':
   # Accessories 1.1 renders this launcher in Owo's layer tree rather
   # than the screen's AbstractWidget children. Its publisher defaults
   # place the same eight-pixel button at inventory-relative (66,8).
   b=s['bounds'];v={'x':b['x']+66,'y':b['y']+8,'width':8,'height':8}
  assert v is not None,'Original inventory launcher not found'
 s=tap(v['x']+v['width']/2,v['y']+v['height']/2)
 if s.get('screen','').endswith('.ScreenVariantSelectionScreen'):
  # A first-run native preference chooser, not an inventory window.
  act('capture',capture='accessories-native-variant-chooser',ticks=16)
  s=tap(s['guiWidth']/2+(-65 if a.variant=='original' else 65),s['guiHeight']/2)
 assert a.mod.lower() in s['screen'].lower(),s.get('screen')
 if a.variant=='experimental':assert 'Experimental' in s['screen'],s['screen']
 if a.variant=='legacy':assert a.mod=='curios' and s['screen'].endswith('.CuriosScreen'),s['screen']
else:
 if not any(w['kind']=='CHARACTER' for w in s['workspaceWindows']):s=act('key',key=67,ticks=12)
 w=next(w for w in s['workspaceWindows'] if w['kind']=='CHARACTER');b=w['bounds'];s=tap(b['x']+32,b['y']+16+14)
 assert any(a.mod in w.get('menuId','') for w in s['windows']),s.get('windows')
act('move',x=5,y=5,ticks=4)
capture(a.run,a.side,a.minecraft,a.loader,a.mod+('-'+a.variant if a.variant in ['experimental','legacy'] else ''),'Original inventory launcher or SIU Character attachment used normally. Equipment/cosmetic layout pictured; other interactions are tracked separately.')
