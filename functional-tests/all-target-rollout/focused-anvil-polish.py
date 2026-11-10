"""Natural Anvil entry, complete field mapping, guide hover and one real rename."""
from pathlib import Path
import argparse,json,sys
sys.path.insert(0,str(Path(__file__).parent))
from evidence import send,capture
p=argparse.ArgumentParser();p.add_argument('run',type=Path);p.add_argument('--minecraft',required=True);p.add_argument('--loader',required=True);a=p.parse_args()
checks=[]
def act(kind,**kw):return send(a.run,kind,**kw)
def check(name,ok):
 checks.append({'check':name,'passed':bool(ok)});(a.run/'anvil-polish-focused-checks.json').write_text(json.dumps({'scope':'Natural opening, name-field mapping, guide hover and one rename only','checks':checks},indent=2))
 assert ok,name
 print('PASS',name,flush=True)
def window(s):return next(w for w in s['windows'] if 'anvil' in w.get('nativeScreen','').lower())
act('close',ticks=3);act('fixture',block='minecraft:anvil',ticks=16)
act('server-command',command='gamemode survival @a',ticks=6);act('server-command',command='xp set @a 30 levels',ticks=6)
s=act('client-use-block',ticks=20)['client']
if not s.get('windows'):s=act('client-use-block',ticks=20)['client']
w=window(s);b=w['bounds'];regions=w['template']['regions']
check('integrated-native-Anvil',w['definition'].endswith('.NativeTemplateWindow'))
check('name-field-stays-with-its-border',not any(r['id'].startswith('widget/') for r in regions))
check('complete-native-formatting-guide-region',any(r['id']=='formatting-guide' for r in regions))
capture(a.run,'siu',a.minecraft,a.loader,'easy-anvils','The complete name field stays in its original frame; the native formatting guide is retained beside it. Pictured layout and the separately recorded one-item rename are focused checks.')
guide=next(r for r in regions if r['id']=='formatting-guide')
act('hover',x=b['x']+6+guide['x']+7,y=b['y']+20+guide['y']+6,ticks=16)
act('capture',capture='siu-anvil-formatting-guide-hover',ticks=16)
act('seed-player',items=[{'slot':9,'item':'minecraft:diamond_sword','count':1}],ticks=6)
act('player-click',slot=9,ticks=6);r=act('click',slot=0,ticks=8)
check('actual-sword-in-native-input',r['server']['slots'][0]['item']=='minecraft:diamond_sword')
for inv in r['client'].get('workspaceWindows',[]):
 if inv.get('kind')=='INVENTORY':
  ib=inv['bounds'];act('xy-click',x=ib['x']+ib['width']-9,y=ib['y']+9,ticks=5)
act('xy-click',x=b['x']+6+13,y=b['y']+20+11,ticks=4)
act('key',key=65,modifiers=2,ticks=3)
r=act('text',text='SIU Check',ticks=12)
result=r['server']['slots'][2]
check('native-rename-prepares-exact-complete-name',result['item']=='minecraft:diamond_sword' and result['count']==1 and 'SIU Check' in result.get('components',''))
r=act('click',slot=2,ticks=8)
check('takes-exact-one-renamed-sword',r['server']['cursor']['item']=='minecraft:diamond_sword' and r['server']['cursor']['count']==1)
r=act('store',ticks=8)
items=[v for v in r['server']['playerInventory'] if v['item']=='minecraft:diamond_sword']
check('stored-sword-has-native-custom-name',sum(v['count'] for v in items)==1 and any('SIU Check' in v.get('components','') for v in items) and r['server']['cursor']['count']==0)
