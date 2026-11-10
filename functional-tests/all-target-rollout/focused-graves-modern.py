"""Real death/list/storage entry, permission, Info and withdrawal checks for SGUI2."""
from pathlib import Path
import argparse,json,sys
sys.path.insert(0,str(Path(__file__).parent))
from evidence import send,capture
p=argparse.ArgumentParser();p.add_argument('run',type=Path);p.add_argument('--side',choices=['native','siu'],required=True)
p.add_argument('--minecraft',required=True);p.add_argument('--loader',default='fabric');p.add_argument('--resume-storage',action='store_true');a=p.parse_args()
checks=[];cmd='server-command' if a.side=='siu' else 'command'
def act(kind,**kw):return send(a.run,kind,**kw)
def command(s,ticks=6):return act(cmd,command=s,ticks=ticks)
def check(label,ok):
 checks.append({'check':label,'passed':bool(ok)})
 (a.run/'graves-modern-focused-checks.json').write_text(json.dumps({'scope':'Real death, native selection list, read-only storage, one Info compass and seven-item withdrawal only','checks':checks},indent=2))
 assert ok,label
 print('PASS',label,flush=True)
def win(s):return next(w for w in s.get('windows',[]) if w.get('definition','').endswith('.GraveWindow'))
def native_entry(s):
 b=s.get('nativeBounds',s.get('bounds'));assert b,'Native list bounds unavailable'
 if a.side=='siu':return act('native-click',x=b['x']+16,y=b['y']+26,ticks=16)
 act('click',x=b['x']+16,y=b['y']+26,ticks=1)
 return act('release',x=b['x']+16,y=b['y']+26,ticks=16)
if a.resume_storage:s=act('inspect',ticks=2)['client']
else:
 act('close',ticks=3);act('fixture',block='minecraft:stone',ticks=16)
 act('catalog',ticks=1);catalog=json.loads((a.run/'catalog.json').read_text())
 x,y,z=map(int,catalog.get('origin','8,65,8').split(','))
 command('gamerule minecraft:immediate_respawn true');command('gamerule minecraft:keep_inventory false')
 command('gamemode survival @a');command(f'spawnpoint @a {x} {y} {z+3}');command(f'tp @a {x+.5} {y} {z+3.5} 90 0')
 command('clear @a');command('item replace entity @a inventory.0 with minecraft:diamond 7')
 command('item replace entity @a inventory.1 with minecraft:gold_ingot 16');command('kill @a',ticks=50)
 s=command('execute as @a run graves',ticks=16)['client']
 check('selection-list-retains-native-screen',s.get('screen','').endswith('ContainerScreen') and not s.get('windows'))
 act('capture',capture=f'{a.side}-graves-list-native-retained-{a.minecraft.replace(".", "")}-{a.loader}',ticks=16)
 r=native_entry(s);s=r['client']
if a.side=='native':
 check('real-native-grave-storage',len(s.get('slots',[]))>=45)
 capture(a.run,a.side,a.minecraft,a.loader,'universal-grave-storage');raise SystemExit
w=win(s);b=w['bounds']
check('real-seven-diamonds-in-storage',w['slots'][0]['item']=='minecraft:diamond' and w['slots'][0]['count']==7)
r=act('click',slot=0,ticks=6)
check('read-only-preview-cannot-withdraw',win(r['client'])['slots'][0]['count']==7 and r['server']['cursor']['count']==0)
act('seed-player',items=[{'slot':9,'item':'minecraft:compass','count':1}],ticks=6)
before=act('player-click',slot=9,ticks=6)
compasses=sum(v['count'] for v in before['server']['playerInventory'] if v['item']=='universal_graves:grave_compass')
r=act('xy-click',x=b['x']+23,y=b['y']+b['height']-13,ticks=10)
check('Info-converts-exact-carried-compass',sum(v['count'] for v in r['server']['playerInventory'] if v['item']=='universal_graves:grave_compass')==compasses+1 and r['server']['cursor']['count']==0)
for window in r['client'].get('workspaceWindows',[]):
 if window.get('kind')=='INVENTORY':
  ib=window['bounds'];act('xy-click',x=ib['x']+ib['width']-9,y=ib['y']+9,ticks=5)
command('time set day');act('hover',x=639,y=449,ticks=2)
capture(a.run,'siu',a.minecraft,a.loader,'universal-grave-storage')
act('close',ticks=4);s=command('execute as @a run graves modify',ticks=16)['client']
check('modifying-list-retains-native-screen',s.get('screen','').endswith('ContainerScreen') and not s.get('windows'))
native_entry(s);r=act('click',slot=0,ticks=8)
check('editable-storage-withdraws-exact-seven-diamonds',win(r['client'])['slots'][0]['count']==0 and r['server']['cursor']['item']=='minecraft:diamond' and r['server']['cursor']['count']==7)
r=act('store',ticks=8)
check('separate-inventory-stores-seven-without-duplication',r['server']['cursor']['count']==0 and sum(v['count'] for v in r['server']['playerInventory'] if v['item']=='minecraft:diamond')==7)
