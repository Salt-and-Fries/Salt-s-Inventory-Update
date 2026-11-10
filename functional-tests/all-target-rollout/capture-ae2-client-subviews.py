"""AE2 19 key types and processing quantity through original client controls."""
from pathlib import Path
import argparse,subprocess,sys
sys.path.insert(0,str(Path(__file__).parent))
from evidence import send,capture
p=argparse.ArgumentParser();p.add_argument('run',type=Path);p.add_argument('--side',choices=['native','siu'],required=True);p.add_argument('--loader',required=True);p.add_argument('--minecraft',default='1.21.1');a=p.parse_args()
subprocess.run([sys.executable,'-X','utf8',str(Path(__file__).with_name('capture-ae2-legacy-workflows.py')),str(a.run),'--side',a.side,'--loader',a.loader,'--minecraft',a.minecraft,'--only','pattern-processing-patterns'],check=True)
def act(k,**kw):return send(a.run,k,**kw)['client']
def widgets(s):return [v for w in s['windows'] for v in w['nativeWidgets']] if a.side=='siu' else s['widgets']
def screen(s):return s['windows'][0].get('nativeScreen','') if a.side=='siu' else s['screen']
def tap(v):
 x=v['x']+v['width']/2;y=v['y']+v['height']/2
 act('click',x=x,y=y,ticks=1);return act('release',x=x,y=y,ticks=16)
def picture(s,name,suffix):
 assert screen(s).endswith(suffix),screen(s)
 act('move',x=5,y=5,ticks=3);capture(a.run,a.side,a.minecraft,a.loader,'ae2-'+name,'Original pattern-terminal toolbar or middle-click action opens this client-only subview. Native ghost-slot configuration content is seeded for the quantity editor; no menu provider is forced.')
s=act('wait',ticks=3)
if a.minecraft!='1.20.1':
 s=tap(next(v for v in widgets(s) if v.get('visible') and v['type'].endswith('.KeyTypeSelectionButton')));picture(s,'key-type-selection','.KeyTypeSelectionScreen')
 s=tap(next(v for v in widgets(s) if v.get('visible') and v.get('message')=='ME Pattern Encoding Terminal'))
kw={'menu':'appeng.menu.me.items.PatternEncodingTermMenu'} if a.side=='siu' else {}
s=act('reviewed-session-seed' if a.side=='siu' else 'seed',slot=51,item='minecraft:iron_ingot',count=64,ticks=10,**kw)
w=s['windows'][0] if a.side=='siu' else s;v=next(v for v in w['slots'] if v['index']==51);b=w['nativeBounds'] if a.side=='siu' else s['bounds'];x=b['x']+v['x']+8;y=b['y']+v['y']+8
act('click',x=x,y=y,button=2,ticks=1);s=act('release',x=x,y=y,button=2,ticks=16);picture(s,'processing-pattern-amount','.SetProcessingPatternAmountScreen')
print('PASS native processing-amount client subview'+(' and key-type selection' if a.minecraft!='1.20.1' else ''),flush=True)
