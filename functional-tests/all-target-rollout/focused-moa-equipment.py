"""One natural Moa opening, saddle insertion/recovery and normal close only."""
from pathlib import Path
import argparse,json,re,sys
sys.path.insert(0,str(Path(__file__).parent))
from evidence import send
p=argparse.ArgumentParser();p.add_argument('run',type=Path);a=p.parse_args();checks=[]
def act(kind,**kw):return send(a.run,kind,**kw)['client']
def check(label,ok):
    checks.append({'check':label,'passed':bool(ok)})
    (a.run/'moa-equipment-focused-checks.json').write_text(json.dumps({'scope':'One ordinary Moa use, original saddle slot, exact recovery and close; no full mount gameplay suite','checks':checks},indent=2))
    assert ok,label
    print('PASS',label,flush=True)
def moa(s):return next(w for w in s['windows'] if w.get('menuId')=='salts_inventory_compat:moa')
def snapshot():
    act('snapshot-server',ticks=3);return json.loads((a.run/'server-state.json').read_text())
def native_menu(s):return next(m for m in s['menus'] if m['class'].endswith('.MoaInventoryMenu'))
def count(nbt):
    if nbt=='{}' or 'minecraft:air' in nbt:return 0
    match=re.search(r'(?:Count|count):\s*(\d+)',nbt);return int(match[1]) if match else 0
def tap(x,y):return act('xy-click',x=x,y=y,ticks=12)
def saddle_point(s):
    w=moa(s);v=next(v for v in w['slots'] if v['index']==0);x=v['x']+8;y=v['y']+8
    r=next(r for r in w['template']['regions'] if r['source']['x']<=x<r['source']['x']+r['source']['width'] and r['source']['y']<=y<r['source']['y']+r['source']['height'])
    b=w['bounds'];padding=w['template']['padding']
    return b['x']+6+padding+r['x']+x-r['source']['x'],b['y']+20+padding+r['y']+y-r['source']['y']
s=act('wait',ticks=2)
check('one-normal-use-produces-one-original-moa-window',sum(w.get('menuId')=='salts_inventory_compat:moa' for w in s['windows'])==1 and moa(s)['nativeScreen'].endswith('.MoaInventoryScreen') and not moa(s).get('layoutError'))
act('command',command='gamemode survival @a',ticks=4);act('command',command='item replace entity @a inventory.0 with minecraft:saddle',ticks=4)
if not any(w['kind']=='INVENTORY' for w in s['workspaceWindows']):s=act('inventory',ticks=8)
inv=next(w for w in s['workspaceWindows'] if w['kind']=='INVENTORY');v=next(v for v in inv['inventoryCells'] if v['inventorySlot']==9);s=tap(v['x'],v['y']);s=tap(*saddle_point(s));server=snapshot()
stored=native_menu(server)['slots'][0]['item'];check('original-moa-slot-accepts-one-saddle','minecraft:saddle' in stored and count(stored)==1)
s=tap(*saddle_point(s));inv=next(w for w in s['workspaceWindows'] if w['kind']=='INVENTORY');v=next(v for v in inv['inventoryCells'] if v['inventorySlot']==10);tap(v['x'],v['y']);server=snapshot();stored=next(v['item'] for v in server['inventory'] if v['index']==10)
check('saddle-recovers-once-with-clear-cursor','minecraft:saddle' in stored and count(stored)==1 and count(native_menu(server)['slots'][0]['item'])==0 and all(count(m['carried'])==0 for m in server['menus']))
s=act('close',ticks=8);check('moa-window-closes-without-disconnect',s.get('worldLoaded') and not s['windows'])
