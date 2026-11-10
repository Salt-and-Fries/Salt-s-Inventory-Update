"""Reach AE2's actual submit-error view by changing a calculated network."""
from pathlib import Path
import argparse,json,subprocess,sys
sys.path.insert(0,str(Path(__file__).parent))
from evidence import send,capture
p=argparse.ArgumentParser();p.add_argument('run',type=Path);p.add_argument('--side',choices=['native','siu'],required=True);p.add_argument('--minecraft',required=True);p.add_argument('--loader',required=True);a=p.parse_args()
driver='capture-ae2-legacy-crafting.py' if a.minecraft=='1.20.1' else 'capture-ae2-modern-crafting.py'
args=[str(a.run),'--side',a.side,'--loader',a.loader]
if a.minecraft!='1.20.1':args+=['--minecraft',a.minecraft]
subprocess.run([sys.executable,'-X','utf8',str(Path(__file__).parent/driver),*args],check=True)
def act(kind,**kw):return send(a.run,kind,**kw)['client']
s=act('wait',ticks=2)
widgets=s['windows'][0]['nativeWidgets'] if a.side=='siu' else s['widgets']
button=next(w for w in widgets if w.get('visible') and w.get('active',True) and w.get('message')=='Start')
# Remove the real storage drive after the original plan has been calculated.
# The ordinary Start action must report the original resource extraction error.
act('command',command='setblock 8 65 7 minecraft:air',ticks=1)
x=button['x']+button['width']/2;y=button['y']+button['height']/2
act('click',x=x,y=y,ticks=1);s=act('release',x=x,y=y,ticks=24)
screen=(s['windows'][0].get('nativeScreen','') if a.side=='siu' and s.get('windows') else s.get('screen',''))
assert screen.endswith('.CraftErrorScreen'),('Expected publisher submit-error dialog',screen)
capture(a.run,a.side,a.minecraft,a.loader,'ae2-craft-error','Real calculated oak-planks request, followed by removal of its actual storage drive and the native Start button. The publisher handles the resulting submit failure. No error dialog or menu was constructed by the observer; this checks only the pictured error path.')
print('PASS original AE2 calculated-request submit-error entry',flush=True)
