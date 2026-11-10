"""Original Sculptor payment and Begin action, without playing the challenge."""
from pathlib import Path
import argparse,json,sys
sys.path.insert(0,str(Path(__file__).parent))
from evidence import send
p=argparse.ArgumentParser();p.add_argument('run',type=Path);a=p.parse_args();checks=[]
def act(k,**kw):return send(a.run,k,**kw)['client']
def check(label,ok):
 checks.append({'check':label,'passed':bool(ok)});(a.run/'mowzie-sculptor-focused-checks.json').write_text(json.dumps({'scope':'Original payment and Begin action only; complete platform challenge not tested','checks':checks},indent=2));assert ok,label;print('PASS',label,flush=True)
def window(s):return next(w for w in s['windows']if w.get('menuId')=='mowziesmobs:sculptor_trade')
def point(w,x,y):
 r=next(r for r in w['template']['regions']if r['source']['x']<=x<r['source']['x']+r['source']['width']and r['source']['y']<=y<r['source']['y']+r['source']['height']);b=w['bounds'];pad=w['template']['padding'];return b['x']+6+pad+r['x']+x-r['source']['x'],b['y']+20+pad+r['y']+y-r['source']['y']
def tap(x,y):return act('xy-click',x=x,y=y,ticks=16)
s=act('wait',ticks=2);check('original-sculptor-window-stays-open',window(s)['nativeScreen'].endswith('.GuiSculptorTrade')and not window(s).get('layoutError'))
if any(w['kind']=='CREATIVE'for w in s['workspaceWindows']):s=act('inventory',ticks=5)
act('command',command='gamemode survival @a',ticks=4);act('command',command='item replace entity @a inventory.0 with mowziesmobs:bluff_rod',ticks=4)
if not any(w['kind']=='INVENTORY'for w in s['workspaceWindows']):s=act('inventory',ticks=8)
v=next(v for w in s['workspaceWindows']if w['kind']=='INVENTORY'for v in w['inventoryCells']if v['inventorySlot']==9);s=tap(v['x'],v['y']);w=window(s);v=w['slots'][0];s=tap(*point(w,v['x']+8,v['y']+8));act('snapshot-server',ticks=3);server=json.loads((a.run/'server-state.json').read_text());m=next(m for m in server['menus']if m['class'].endswith('.ContainerSculptorTrade'));check('native-payment-slot-receives-original-request','mowziesmobs:bluff_rod'in m['slots'][0]['item'])
w=window(s);v=next(v for v in w['nativeWidgets']if v.get('visible')and v.get('active'));b=w['nativeBounds'];tap(*point(w,v['x']+v['width']/2-b['x'],v['y']+v['height']/2-b['y']));act('reviewed-entity-probe',ticks=3);probe=json.loads((a.run/'reviewed-entity-probe.json').read_text());e=next(e for e in probe['entities']if e.get('type')=='mowziesmobs:sculptor');act('snapshot-server',ticks=3);server=json.loads((a.run/'server-state.json').read_text());check('native-begin-starts-test-and-consumes-payment',e['isTesting']=='true'and not any('mowziesmobs:bluff_rod'in v['item']for v in server['inventory'])and all('mowziesmobs:bluff_rod'not in v['item']for m in server['menus']for v in m['slots'][:1]))
