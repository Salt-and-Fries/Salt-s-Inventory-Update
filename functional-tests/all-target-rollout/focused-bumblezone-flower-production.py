"""Exercise the original flower packet, XP action and item recovery via actual UI clicks."""
from pathlib import Path
import argparse,json,re,sys
sys.path.insert(0,str(Path(__file__).parent))
from evidence import send,capture
p=argparse.ArgumentParser();p.add_argument('run',type=Path);p.add_argument('--minecraft',required=True);p.add_argument('--loader',required=True);a=p.parse_args();checks=[]
def act(kind,**kw):return send(a.run,kind,**kw)['client']
def command(text):return act('command',command=text,ticks=6)
def check(label,ok):
    checks.append({'check':label,'passed':bool(ok)})
    (a.run/'flower-packet-production-focused-checks.json').write_text(json.dumps({'scope':'Native flower book, XP feeding, enchantment packet and exact result recovery only; no complete gameplay suite','checks':checks},indent=2))
    assert ok,label
    print('PASS',label,flush=True)
def window(s):return next(w for w in s['windows'] if w.get('menuId')=='the_bumblezone:crystalline_flower')
def tap(x,y):return act('xy-click',x=x,y=y,ticks=12)
def native(s,x,y):
    w=window(s);layout=w['template'];b=w['bounds']
    r=next(r for r in layout['regions'] if r['source']['x']<=x<r['source']['x']+r['source']['width'] and r['source']['y']<=y<r['source']['y']+r['source']['height'])
    return tap(b['x']+7+layout['padding']+r['x']+x-r['source']['x'],b['y']+22+layout['padding']+r['y']+y-r['source']['y'])
def snapshot():
    act('snapshot-server',ticks=3);return json.loads((a.run/'server-state.json').read_text())
def menu(s):return next(m for m in s['menus'] if m['class'].endswith('.CrystallineFlowerMenu'))
def count(nbt):
    if 'minecraft:air' in nbt or nbt=='{}':return 0
    m=re.search(r'(?:Count|count):\s*(\d+)',nbt);return int(m[1]) if m else 0
act('close',ticks=3);act('fixture',block='the_bumblezone:crystalline_flower',ticks=16)
for text in ['kill @e[type=!minecraft:player]','gamemode survival @a','time set day','xp set @a 100 levels',
             'fill 7 66 7 9 73 9 minecraft:air',
             'setblock 8 64 8 minecraft:amethyst_block','item replace entity @a inventory.0 with minecraft:book']:
    command(text)
s=act('client-use-block',ticks=20);check('original-flower-hosted',bool(window(s)['template']) and not window(s).get('layoutError'))
if not any(w['kind']=='INVENTORY' for w in s['workspaceWindows']):s=act('inventory',ticks=8)
inv=next(w for w in s['workspaceWindows'] if w['kind']=='INVENTORY');cell=next(c for c in inv['inventoryCells'] if c['inventorySlot']==9)
s=tap(cell['x'],cell['y']);s=native(s,82,25)
stored=menu(snapshot())['slots'][1]['item'];check('book-in-original-input','minecraft:book' in stored and count(stored)==1)
s=native(s,54,21);s=native(s,95,55)
stored=menu(snapshot())['slots'][2]['item'];check('xp-and-native-enchantment-packet-produce-book','minecraft:enchanted_book' in stored and count(stored)==1)
capture(a.run,'siu',a.minecraft,a.loader,'bumblezone-flower-enchanting','Original book input, player XP feed, enchantment selection and exact result recovery verified through normal UI input.')
v=next(v for v in window(s)['slots'] if v['index']==2);s=native(s,v['x']+8,v['y']+8)
inv=next(w for w in s['workspaceWindows'] if w['kind']=='INVENTORY');cell=next(c for c in inv['inventoryCells'] if c['inventorySlot']==10);tap(cell['x'],cell['y'])
server=snapshot();stored=next(v['item'] for v in server['inventory'] if v['index']==10)
check('exact-result-recovered-once-with-clear-cursor','minecraft:enchanted_book' in stored and count(stored)==1 and all(count(m['carried'])==0 for m in server['menus']))
