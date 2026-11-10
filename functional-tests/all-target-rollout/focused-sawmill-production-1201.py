"""One actual Sawmill input/recipe/result transfer through the public desktop."""
from pathlib import Path
import argparse,json,re,sys
sys.path.insert(0,str(Path(__file__).parent))
from evidence import send
p=argparse.ArgumentParser();p.add_argument('run',type=Path);a=p.parse_args();checks=[]
def act(kind,**kw):return send(a.run,kind,**kw)
def check(name,ok):
 checks.append({'check':name,'passed':bool(ok)})
 (a.run/'sawmill-opening-recipe-focused-checks.json').write_text(json.dumps({'scope':'Normal Sawmill opening and one native recipe/result transfer only','checks':checks},indent=2))
 assert ok,name
 print('PASS',name,flush=True)
def window(s):return next(w for w in s['windows'] if w.get('menuId')=='sawmill:sawmill')
def point(s,x,y):
 w=window(s);b=w['bounds']
 r=next(r for r in w['template']['regions'] if r['interactive'] and r['source']['x']<=x<r['source']['x']+r['source']['width'] and r['source']['y']<=y<r['source']['y']+r['source']['height'])
 return b['x']+8+r['x']+x-r['source']['x'],b['y']+24+r['y']+y-r['source']['y']
def tap(x,y):return act('xy-click',x=x,y=y,ticks=12)['client']
def snapshot():
 act('snapshot-server',ticks=3);return json.loads((a.run/'server-state.json').read_text())
def nbt_count(item):
 match=re.search(r'Count:(\d+)b',item);return int(match[1]) if match else 0
def input_menu(s):return next(m for m in s['menus'] if 'SawmillMenu' in m['class'])
act('close');act('fixture',block='sawmill:sawmill',ticks=16)
act('server-command',command='gamemode survival @a',ticks=5)
act('server-command',command='clear @a',ticks=4)
act('server-command',command='item replace entity @a inventory.0 with minecraft:oak_log 32',ticks=5)
s=act('client-use-block',ticks=20)['client']
if not s.get('windows'):s=act('client-use-block',ticks=20)['client']
check('normal-Sawmill-opening-keeps-the-connection',s.get('worldLoaded') and window(s)['definition'].endswith('.NativeTemplateWindow'))
s=act('inventory',ticks=12)['client']
inventory=next(w for w in s['workspaceWindows'] if w['kind']=='INVENTORY')
cell=next(c for c in inventory['inventoryCells'] if c['inventorySlot']==9)
s=tap(cell['x'],cell['y']);s=tap(*point(s,29,41))
before=snapshot();native=input_menu(before)
check('actual-player-input-reaches-native-slot',nbt_count(native['slots'][0]['item'])==32 and 'minecraft:oak_log' in native['slots'][0]['item'])
s=tap(*point(s,60,33));ready=snapshot();native=input_menu(ready);output=native['slots'][1]['item']
check('original-recipe-grid-selects-a-real-result',nbt_count(output)>0 and 'minecraft:air' not in output)
act('capture',capture='sawmill-native-recipe-ready',ticks=16)
s=tap(*point(s,151,41))
inventory=next(w for w in s['workspaceWindows'] if w['kind']=='INVENTORY')
cell=next(c for c in inventory['inventoryCells'] if c['inventorySlot']==10)
tap(cell['x'],cell['y']);after=snapshot()
stored=next(v['item'] for v in after['inventory'] if v['index']==10)
check('native-result-stores-with-its-original-count',stored==output)
check('native-recipe-consumes-the-input',nbt_count(input_menu(after)['slots'][0]['item'])<32)
