"""Real inventory rendering and exact item recovery with Transmog installed."""
from pathlib import Path
import argparse,json,re,sys,time
from PIL import Image
sys.path.insert(0,str(Path(__file__).parent))
from evidence import send
p=argparse.ArgumentParser();p.add_argument('run',type=Path);a=p.parse_args();checks=[]
qa_mode=a.run.parent.parent.name=='all-target-rollout'
def act(kind,**kw):return send(a.run,'server-command' if kind=='command' else 'hover' if qa_mode and kind=='move' else kind,**kw)['client']
def check(label,ok):
    checks.append({'check':label,'passed':bool(ok)})
    (a.run/'transmog-inventory-focused-checks.json').write_text(json.dumps({'scope':'Transmog installed: 27 visible inventory cells, normal seven-diamond transfer and exact recovery; no full gameplay suite','checks':checks},indent=2))
    assert ok,label
    print('PASS',label,flush=True)
def snapshot():
    if qa_mode:
        s=send(a.run,'wait',ticks=3)['server']
        return {'inventory':[{'index':v['index'],'item':json.dumps(v)} for v in s['playerInventory']],
                'menus':[{'carried':json.dumps(s['cursor'])}]}
    act('snapshot-server',ticks=3);return json.loads((a.run/'server-state.json').read_text())
def count(nbt):
    if nbt=='{}' or 'minecraft:air' in nbt:return 0
    m=re.search(r'(?:Count|count)"?\s*:\s*(\d+)',nbt);return int(m[1]) if m else 0
def inventory(s):return next(w for w in s['workspaceWindows'] if w['kind']=='INVENTORY')
def tap(s,slot):
    v=next(v for v in inventory(s)['inventoryCells'] if v['inventorySlot']==slot)
    return act('xy-click',x=v['x'],y=v['y'],ticks=10)
act('close',ticks=3);act('command',command='gamemode survival @a',ticks=3)
act('command',command='clear @a',ticks=3)
s=act('inventory',ticks=8);act('move',x=5,y=5,ticks=3)
stem='transmog-inventory-visible-cells-'+str(time.monotonic_ns())
s=act('capture',capture=stem,ticks=16)
deadline=time.monotonic()+5
while True:
    try:
        image=Image.open(a.run/'screenshots'/(stem+'.png')).convert('RGB');break
    except OSError:
        if time.monotonic()>deadline:raise
        time.sleep(.05)
scale=image.width/s['guiWidth']
cells=inventory(s)['inventoryCells'];visible=sum(image.getpixel((round((v['x']-6)*scale),round((v['y']-6)*scale))) in {(139,139,139),(192,192,192)} for v in cells)
# Slot fill is #8b8b8b, or #c0c0c0 under the real OS cursor's hover overlay.
# mouseMoved() alone cannot relocate that cursor. The broken blank body was
# uniformly #c6c6c6, which is deliberately not accepted.
check('all-27-physical-inventory-cells-render',len(cells)==27 and visible==27)
s=act('command',command='item replace entity @a inventory.0 with minecraft:diamond 7',ticks=8)
s=tap(s,9);s=tap(s,10);server=snapshot();stored=next(v['item'] for v in server['inventory'] if v['index']==10)
check('ordinary-clicks-transfer-exact-seven-diamonds','minecraft:diamond' in stored and count(stored)==7 and count(next(v['item'] for v in server['inventory'] if v['index']==9))==0)
check('transfer-leaves-no-shared-cursor-stack',all(count(m['carried'])==0 for m in server['menus']))
s=tap(s,10);s=tap(s,9);server=snapshot();stored=next(v['item'] for v in server['inventory'] if v['index']==9)
check('exact-stack-recovers-once-in-original-cell','minecraft:diamond' in stored and count(stored)==7 and count(next(v['item'] for v in server['inventory'] if v['index']==10))==0 and all(count(m['carried'])==0 for m in server['menus']))
act('capture',capture='transmog-inventory-recovered-stack',ticks=16)
