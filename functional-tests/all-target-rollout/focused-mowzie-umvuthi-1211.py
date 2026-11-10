"""Native Umvuthi payment and blessing through the window's typed transport."""
from pathlib import Path
import argparse,json,re,sys
sys.path.insert(0,str(Path(__file__).parent))
from evidence import send
p=argparse.ArgumentParser();p.add_argument('run',type=Path);a=p.parse_args();checks=[]
def act(k,**kw):return send(a.run,k,**kw)['client']
def check(name,ok):
 checks.append({'check':name,'passed':bool(ok)});(a.run/'mowzie-typed-trade-focused-checks.json').write_text(json.dumps({'scope':'Umvuthi original desired-item payment, exact native cost/remainder and the original blessing action. Other mob abilities are outside this focused check.','checks':checks},indent=2));assert ok,name;print('PASS',name,flush=True)
def window(s):return next(w for w in s['windows']if w.get('menuId')=='mowziesmobs:umvuthi_trade')
def source_point(w,x,y):
 r=next(r for r in w['template']['regions']if r['source']['x']<=x<r['source']['x']+r['source']['width']and r['source']['y']<=y<r['source']['y']+r['source']['height']);b=w['bounds'];pad=w['template']['padding'];return b['x']+6+pad+r['x']+x-r['source']['x'],b['y']+20+pad+r['y']+y-r['source']['y']
def tap(x,y):return act('xy-click',x=x,y=y,ticks=16)
def snap():
 act('snapshot-server',ticks=3);return json.loads((a.run/'server-state.json').read_text())
def count(nbt):
 m=re.search(r'(?:Count|count):\s*(\d+)',nbt);return int(m[1])if m else 0
s=act('wait',ticks=2);check('native-umvuthi-window-open',window(s)['nativeScreen'].endswith('.GuiUmvuthiTrade')and not window(s).get('layoutError'))
act('reviewed-entity-probe',ticks=3);probe=json.loads((a.run/'reviewed-entity-probe.json').read_text());v=next(v for v in probe['entities']if v.get('type')=='mowziesmobs:umvuthi');match=re.fullmatch(r'(\d+) (minecraft:\w+)',v['getDesires']);assert match,v
cost=int(match[1]);item=match[2];assert cost+5<=64
# Keep the native friendly mask equipped; clearing armor revokes permission
# to trade and correctly closes Umvuthi's inventory.
act('command',command='gamemode survival @a',ticks=3);act('command',command=f'item replace entity @a inventory.0 with {item} {cost+5}',ticks=5)
if not any(w['kind']=='INVENTORY'for w in s['workspaceWindows']):s=act('inventory',ticks=8)
inv=next(w for w in s['workspaceWindows']if w['kind']=='INVENTORY');v=next(v for v in inv['inventoryCells']if v['inventorySlot']==9);s=tap(v['x'],v['y']);w=window(s);slot=w['slots'][0];s=tap(*source_point(w,slot['x']+8,slot['y']+8));server=snap();m=next(m for m in server['menus']if m['class'].endswith('.ContainerUmvuthiTrade'))
check('native-payment-slot-receives-exact-stack',item in m['slots'][0]['item']and count(m['slots'][0]['item'])==cost+5)
def trade_button(s):
 w=window(s);v=next(v for v in w['nativeWidgets']if v.get('visible')and v.get('active'));b=w['nativeBounds'];return source_point(w,v['x']+v['width']/2-b['x'],v['y']+v['height']/2-b['y'])
s=tap(*trade_button(s));server=snap();m=next(m for m in server['menus']if m['class'].endswith('.ContainerUmvuthiTrade'))
check('native-handler-consumes-price-and-returns-five-once',sum(count(v['item'])for v in server['inventory']if item in v['item'])==5 and count(m['slots'][0]['item'])==0 and all(count(m['carried'])==0 for m in server['menus']))
s=tap(*trade_button(s));act('command',command='setblock 8 65 8 minecraft:white_wool',ticks=3);act('command',command='execute if entity @a[nbt={active_effects:[{id:"mowziesmobs:suns_blessing"}]}] run setblock 8 65 8 minecraft:lime_wool',ticks=3);act('reviewed-block-probe',ticks=3);marker=json.loads((a.run/'reviewed-block-probe.json').read_text())
check('original-grant-button-applies-server-blessing','minecraft:lime_wool'in marker['block'])
