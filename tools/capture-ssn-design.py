"""Capture actual SSN menus in disposable native clients, SIU absent."""
from pathlib import Path
import sys,json,hashlib,argparse
ROOT=Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT/'functional-tests/project-everything-ten'))
from send import send
p=argparse.ArgumentParser();p.add_argument('--neo',action='store_true');a=p.parse_args()
version,loader=('1.21.1','neoforge') if a.neo else ('1.20.1','forge')
RUN=ROOT/f'build/remaining-ui-audit/gameplay/{version}-{loader}-native-ssn-design-01'
OUT=ROOT/'output/ssn-window-design';OUT.mkdir(parents=True,exist_ok=True)
rows=[];state={}
def call(kind='wait',**args):
    global state
    state=send(RUN,kind,**args)['client'];return state
def cmd(s):return call('command',command=s,ticks=4)
def put(item,destination):
    source=next(s['index']for s in state['slots']if s['player']and s['item']==item)
    call('slot',slot=source,ticks=2);call('slot',slot=destination,ticks=4)
def capture(id,label):
    call('move',x=2,y=2,ticks=3);call('capture',capture='ssn-'+id,ticks=3)
    path=RUN/'screenshots'/('ssn-'+id+'.png')
    assert state['screen'].startswith('com.lothrazar.storagenetwork.'),state
    assert not state['siu_loaded']
    rows.append(dict(id=id,label=label,capture=str(path.relative_to(ROOT)),sha256=hashlib.sha256(path.read_bytes()).hexdigest(),minecraft=version,loader=loader,release='1.13.2',state=state))
    (OUT/f'native-references-{version}.json').write_text(json.dumps(dict(minecraft=version,loader=loader,siu_loaded=False,cases=rows),indent=2));print('CAPTURE',id,state['bounds'],flush=True)

if a.neo:
    for id,block,title in [('processing-cable','process_kabel','Processing Cable'),('remote-cradle','cradle','Remote Cradle'),('network-receiver','receiver','Network Receiver')]:
        call('close');call('fixture',block='storagenetwork:'+block,ticks=15);call('open',ticks=15);capture(id,title)
else:
    for id,block,title in [('import-cable','import_filter_kabel','Filtered Import Cable'),('export-cable','export_kabel','Export Cable'),('link-cable','filter_kabel','Filtered Link Cable'),('collection','collector','Network Collection Interface')]:
        call('close');call('fixture',block='storagenetwork:'+block,ticks=10);call('open',ticks=10);capture(id,title)
    for id,block,title in [('inventory','inventory','Storage Inventory'),('request','request','Storage Request Table'),('expanded-inventory','request_expanded','Expanded Storage Request Table')]:
        call('close');call('gui-scale',scale=1 if 'expanded' in id else 2,ticks=5)
        call('ssn-setup',block='storagenetwork:'+block,ticks=40)
        if id!='inventory':cmd('item replace entity @a inventory.0 with minecraft:oak_log 8')
        call('open',ticks=30)
        if id!='inventory':put('minecraft:oak_log',1)
        capture(id,title)
    for id,item,title in [('storage-remote','inventory_remote','Storage Remote'),('crafting-remote','crafting_remote','Crafting Remote'),('expanded-remote','expanded_remote','Expanded Remote')]:
        call('close');call('gui-scale',scale=1 if 'expanded' in id else 2,ticks=5)
        call('ssn-setup',block='storagenetwork:master',ticks=40)
        call('held',item='storagenetwork:'+item,ticks=5);call('open',ticks=8)
        if id!='storage-remote':cmd('item replace entity @a inventory.0 with minecraft:oak_log 8')
        call('use-item',ticks=20)
        if id!='storage-remote':put('minecraft:oak_log',1)
        capture(id,title)
call('close')
