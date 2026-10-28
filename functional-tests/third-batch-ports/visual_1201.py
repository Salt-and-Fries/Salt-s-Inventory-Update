"""Run one disposable 1.20.1 visual-fix cohort; preserves existing evidence."""
from pathlib import Path
import argparse, importlib.util, json, subprocess, time

ROOT=Path(__file__).resolve().parents[2]
spec=importlib.util.spec_from_file_location('sender',ROOT/'functional-tests/project-everything-ten/send.py')
sender=importlib.util.module_from_spec(spec);spec.loader.exec_module(sender)

def run(kind,loader,run_id):
 folder='next-eight-implemented' if kind=='gear' else 'chipped-implemented'
 base='project-everything-eight-implemented' if kind=='gear' else 'project-everything-chipped-implemented'
 if kind=='gear' and loader!='fabric':raise ValueError('Drawer and gear integrations are Fabric-only')
 out=ROOT/'build'/base/'gameplay'/f'1.20.1-{loader}-{run_id}'
 if out.exists():raise ValueError(f'Preserve prior evidence: {out}')
 log=ROOT/'build/third-batch-ports'/f'visual-1.20.1-{loader}-{kind}-{run_id}.log'
 fixture=ROOT/'functional-tests'/folder/f'fixture-{loader}.json'
 args=['cmd','/c','gradlew.bat','-I',f'functional-tests/{folder}/qa.init.gradle',
       f':1.20.1:{loader}:runFullGameplayQa','-PqaMinecraft=1.20.1',f'-PqaLoader={loader}',
       '-PqaFullGameplay=true','-PqaScenario=extension-empty','-PqaIntegrationOnly=true',
       f'-PqaRunId={run_id}',f'-PcompatibilityQaManifest={fixture.relative_to(ROOT)}','--console=plain']
 checks=[]
 def command(name,**kw):return sender.send(out,name,**kw)
 def record(name,ok,detail=None):
  checks.append({'name':name,'passed':bool(ok),'detail':detail})
  (out/'visual-fix-checks.json').write_text(json.dumps(checks,indent=2),encoding='utf-8')
  if not ok:raise AssertionError(name+': '+str(detail))
 with log.open('w',encoding='utf-8') as f:
  proc=subprocess.Popen(args,cwd=ROOT,stdout=f,stderr=subprocess.STDOUT)
  try:
   deadline=time.monotonic()+420
   while not (out/'ready.json').exists():
    if proc.poll() is not None:raise RuntimeError(f'Launch exited {proc.returncode}: {log}')
    if time.monotonic()>deadline:raise TimeoutError(f'No ready marker: {log}')
    time.sleep(1)
   if kind=='gear':
    for name,block in [('single','extended_drawers:single_drawer'),('double','extended_drawers:double_drawer'),('quad','extended_drawers:quad_drawer')]:
     command('fixture',block=block);r=command('crouch-open',ticks=20);command('dismiss-help');r=command('capture',capture='fresh-drawer-'+name,ticks=12)
     record('drawer '+name,len(r['client']['allWindows'])==1 and r['client']['allWindows'][0].get('definition','').endswith('NativeTemplateWindow'))
     command('close')
    for name,block,first,second in [('anvil','minecraft:anvil','AnvilMenu','ReforgeScreenHandler'),('smith','minecraft:smithing_table','SmithingMenu','SmitherScreenHandler'),('grind','minecraft:grindstone','GrindstoneMenu','GrinderScreenHandler')]:
     command('fixture',block=block);r=command('open',ticks=20);command('dismiss-help');r=command('capture',capture='fresh-gear-'+name+'-first',ticks=12);b=r['client']['window']
     record(name+' first',r['client']['menu'].endswith(first),r['client']['menu'])
     r=command('xy-click',x=b['x']+39,y=b['y']-10,ticks=25);record(name+' second',r['client']['menu'].endswith(second),r['client']['menu']);r=command('capture',capture='fresh-gear-'+name+'-second',ticks=12)
     b=r['client']['window'];r=command('xy-click',x=b['x']+16,y=b['y']-10,ticks=25);record(name+' returns',r['client']['menu'].endswith(first),r['client']['menu']);command('close')
   else:
    command('fixture',block='chipped:mason_table');command('seed-player',items=[{'slot':9,'item':'minecraft:stone','count':16}]);r=command('open',ticks=25);command('dismiss-help');r=command('capture',capture='fresh-chipped-empty',ticks=12)
    record('Chipped definition',len(r['client']['allWindows'])==1 and r['client']['allWindows'][0].get('definition','').endswith('ChippedWindow'))
    b=r['client']['window'];command('xy-click',x=b['x']+232,y=b['y']+32,ticks=15);command('xy-click',x=b['x']+125,y=b['y']+67,ticks=25)
    r=command('capture',capture='fresh-chipped-selected',ticks=12)
    command('xy-click',x=b['x']+118,y=b['y']+73,ticks=15)
    command('hover',x=10,y=10,ticks=8,capture='fresh-chipped-craft-normal')
    command('hover',x=b['x']+45,y=b['y']+136,ticks=8,capture='fresh-chipped-craft-hover')
    for i in range(4):
     command('xy-click',x=b['x']+19+18*i,y=b['y']+156,ticks=12)
     command('hover',x=b['x']+19+18*i,y=b['y']+156,ticks=8,capture='fresh-chipped-mode-'+str(i)+'-hover')
     command('hover',x=10,y=10,ticks=8,capture='fresh-chipped-mode-'+str(i))
    r=command('xy-click',x=b['x']+45,y=b['y']+136,ticks=25,capture='fresh-chipped-crafted')
    stack=r['server']['playerInventory'][9]
    record('Chipped craft preserves selected stack count',stack['count']==16 and stack['item']!='minecraft:stone',{'item':stack['item'],'count':stack['count']});command('close')
   command('finish');proc.wait(timeout=100)
   if proc.returncode:raise RuntimeError(f'Client exit {proc.returncode}: {log}')
  finally:
   if proc.poll() is None:
    try:
     if (out/'ready.json').exists():command('finish');proc.wait(timeout=35)
    except Exception:pass
   if proc.poll() is None:subprocess.run(['taskkill','/PID',str(proc.pid),'/T','/F'],stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL)

if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('--kind',choices=['gear','chipped'],required=True);p.add_argument('--loader',choices=['fabric','forge'],required=True);p.add_argument('--run',required=True);a=p.parse_args();run(a.kind,a.loader,a.run)
