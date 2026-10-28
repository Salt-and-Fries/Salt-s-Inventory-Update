"""Focused fresh 1.21.1 visual checks against one already-running QA client."""
from pathlib import Path
import argparse, importlib.util, json
ROOT=Path(__file__).resolve().parents[2]
spec=importlib.util.spec_from_file_location('sender',ROOT/'functional-tests/project-everything-ten/send.py')
sender=importlib.util.module_from_spec(spec);spec.loader.exec_module(sender)
p=argparse.ArgumentParser();p.add_argument('--loader',choices=['fabric','neoforge'],required=True);p.add_argument('--run',required=True);a=p.parse_args()
out=ROOT/'build/third-batch-ports/gameplay'/f'1.21.1-{a.loader}-{a.run}';checks=[]
def command(name,**kw):return sender.send(out,name,**kw)
def record(name,ok,detail=None):
 checks.append({'name':name,'passed':bool(ok),'detail':detail});(out/'visual-fix-checks.json').write_text(json.dumps(checks,indent=2),encoding='utf-8')
 if not ok:raise AssertionError(name+': '+str(detail))
if a.loader=='fabric':
 for name,block in [('single','extended_drawers:single_drawer'),('double','extended_drawers:double_drawer'),('quad','extended_drawers:quad_drawer')]:
  command('fixture',block=block);command('crouch-open',ticks=20);r=command('capture',capture='fresh-drawer-'+name,ticks=12)
  record('drawer '+name,len(r['client']['windows'])==1 and r['client']['windows'][0]['definition'].endswith('NativeTemplateWindow'))
  if name=='single':
   removed=command('remove-source',ticks=30);record('drawer closes when source removed',removed['client']['sessions']==0)
  command('close')
 for name,block,first,second in [('anvil','minecraft:anvil','AnvilMenu','ReforgeScreenHandler'),('smith','minecraft:smithing_table','SmithingMenu','SmitherScreenHandler'),('grind','minecraft:grindstone','GrindstoneMenu','GrinderScreenHandler')]:
  command('fixture',block=block);r=command('open',realUse=True,ticks=20);r=command('capture',capture='fresh-gear-'+name+'-first');record(name+' first',r['server']['menu'].endswith(first),r['server']['menu']);b=r['client']['windows'][0]['bounds']
  r=command('xy-click',x=b['x']+36,y=b['y']-10,ticks=25);record(name+' second',r['server']['menu'].endswith(second),r['server']['menu']);r=command('capture',capture='fresh-gear-'+name+'-second');b=r['client']['windows'][0]['bounds']
  r=command('xy-click',x=b['x']+12,y=b['y']-10,ticks=25);record(name+' returns',r['server']['menu'].endswith(first),r['server']['menu']);command('close')
 for name,block in [('rune-altar','runes:crafting_altar'),('fletching','minecraft:fletching_table')]:
  command('fixture',block=block);r=command('open',realUse=True,ticks=20);r=command('capture',capture='fresh-'+name)
  record(name+' native template',len(r['client']['windows'])==1 and r['client']['windows'][0]['definition'].endswith('NativeTemplateWindow'));command('close')
else:
 for name,block in [('ossuary','graveyard:ossuary'),('rune-altar','runes:crafting_altar')]:
  command('fixture',block=block);r=command('open',realUse=True,ticks=20);r=command('capture',capture='fresh-'+name)
  record(name+' native template',len(r['client']['windows'])==1 and r['client']['windows'][0]['definition'].endswith('NativeTemplateWindow'));command('close')
command('fixture',block='chipped:mason_table');command('seed-player',items=[{'slot':9,'item':'minecraft:stone','count':16}]);r=command('open',realUse=True,ticks=25);r=command('capture',capture='fresh-chipped-empty');record('Chipped definition',r['client']['windows'][0]['definition'].endswith('ChippedWindow'));b=r['client']['windows'][0]['bounds']
command('xy-click',x=b['x']+232,y=b['y']+32,ticks=12);command('xy-click',x=b['x']+125,y=b['y']+67,ticks=25);r=command('capture',capture='fresh-chipped-selected')
command('xy-click',x=b['x']+118,y=b['y']+73,ticks=15)
command('hover',x=10,y=10,ticks=8,capture='fresh-chipped-craft-normal')
command('hover',x=b['x']+45,y=b['y']+136,ticks=8,capture='fresh-chipped-craft-hover')
for i in range(4):
 command('xy-click',x=b['x']+19+18*i,y=b['y']+156,ticks=12)
 command('hover',x=b['x']+19+18*i,y=b['y']+156,ticks=8,capture='fresh-chipped-mode-'+str(i)+'-hover')
 command('hover',x=10,y=10,ticks=8,capture='fresh-chipped-mode-'+str(i))
r=command('xy-click',x=b['x']+45,y=b['y']+136,ticks=25,capture='fresh-chipped-crafted');stack=r['server']['playerInventory'][9]
record('Chipped craft preserves selected stack count',stack['count']==16 and stack['item']!='minecraft:stone',{'item':stack['item'],'count':stack['count']})
command('close')
if a.loader=='neoforge':
 command('capture',capture='fresh-world-control')
 r=command('native-inventory-control',ticks=20,capture='fresh-native-inventory-control')
 record('ordinary native screen remains outside capture scope',r['client'].get('currentScreen','').endswith('InventoryScreen') and not r['client'].get('nativeCaptureScope',True),r['client'].get('currentScreen'))
(out/'finish-requested').touch();print('focused checks',len(checks),'all passed')
