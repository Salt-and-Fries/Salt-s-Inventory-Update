"""Pigment editor native bucket/dye/result flow and cursor recovery only."""
from pathlib import Path
import argparse,json,re,sys
sys.path.insert(0,str(Path(__file__).parent))
from evidence import send,capture
p=argparse.ArgumentParser();p.add_argument('run',type=Path);a=p.parse_args();checks=[]
def act(kind,**kw):return send(a.run,kind,**kw)['client']
def check(name,ok):
 checks.append({'check':name,'passed':bool(ok)});(a.run/'pigment-focused-checks.json').write_text(json.dumps({'scope':'Native pigment bucket input, dye, generated result and exact recovery only; no full Spawn gameplay suite','checks':checks},indent=2));assert ok,name;print('PASS',name,flush=True)
def window(s):return next(w for w in s['windows'] if w.get('nativeScreen','').endswith('.PigmentShifterScreen'))
def point(s,index):
 w=window(s);v=next(v for v in w['slots'] if v['index']==index);x=v['x']+8;y=v['y']+8;r=next(r for r in w['template']['regions'] if r['source']['x']<=x<r['source']['x']+r['source']['width'] and r['source']['y']<=y<r['source']['y']+r['source']['height']);b=w['bounds'];return b['x']+6+r['x']+x-r['source']['x'],b['y']+20+r['y']+y-r['source']['y']
def cell(s,index):
 w=next(w for w in s['workspaceWindows'] if w['kind']=='INVENTORY');v=next(v for v in w['inventoryCells'] if v['inventorySlot']==index);return v['x'],v['y']
def tap(x,y):return act('xy-click',x=x,y=y,ticks=12)
def snapshot():act('snapshot-server',ticks=3);return json.loads((a.run/'server-state.json').read_text())
def native(s):return next(m for m in s['menus'] if m['class'].endswith('.PigmentShifterMenu'))
def count(v):
 if v=='{}' or 'minecraft:air' in v:return 0
 m=re.search(r'(?:Count|count):\s*(\d+)',v);return int(m[1]) if m else 0
act('close',ticks=2);act('sneak',down=False,ticks=2);act('command',command='gamemode survival @a',ticks=2);act('held',item='minecraft:air',ticks=2);act('fixture',block='spawn:pigment_shifter',ticks=4);act('command',command='setblock 8 65 8 spawn:pigment_shifter[waterlogged=true]',ticks=3);s=act('client-use-block',ticks=15)
check('original-menu-and-reviewed-ghost-slot-layout',isinstance(window(s).get('template'),dict) and not window(s).get('layoutError'))
capture(a.run,'siu','1.21.1','neoforge','spawn-pigment-shifter','Original waterlogged pigment editor, complete native slot icons, preview and rotation affordance in its integrated window.')
# The native editor requires a caught mob's bucket data. A data-less creative
# bucket is accepted as input but intentionally produces no native result.
for c in ['clear @a','item replace entity @a inventory.0 with minecraft:tropical_fish_bucket[minecraft:bucket_entity_data={BucketVariantTag:0}]','item replace entity @a inventory.1 with minecraft:red_dye 3']:act('command',command=c,ticks=3)
s=act('inventory',ticks=8);s=tap(*cell(s,9));s=tap(*point(s,0));t=snapshot();check('native-bucket-input-accepts-real-item','minecraft:tropical_fish_bucket' in native(t)['slots'][0]['item'])
s=tap(*cell(s,10));s=tap(*point(s,1));t=snapshot();result=native(t)['slots'][5]['item'];check('native-dye-callback-creates-one-result','minecraft:tropical_fish_bucket' in result and count(result)==1)
capture(a.run,'siu','1.21.1','neoforge','spawn-pigment-shifter-populated','Native tropical fish bucket and red dye produce the original result and live preview. Result withdrawal is checked separately.')
s=tap(*point(s,5));s=tap(*cell(s,11));t=snapshot();n=native(t);stored=next(v['item'] for v in t['inventory'] if v['index']==11)
check('one-result-and-one-dye-consumed-with-clear-cursor','minecraft:tropical_fish_bucket' in stored and count(stored)==1 and count(n['slots'][0]['item'])==0 and count(n['slots'][1]['item'])==2 and all(count(m['carried'])==0 for m in t['menus']))
s=act('close',ticks=8);check('pigment-window-closes-normally',s.get('worldLoaded') and not s['windows'])
