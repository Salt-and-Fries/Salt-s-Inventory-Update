"""Capture the native pouch variants and trinket box through their real item use."""
from pathlib import Path
import argparse,json,sys
ROOT=Path(__file__).resolve().parents[2]
sys.path.insert(0,str(ROOT/'functional-tests/project-everything-ten'))
from send import send
p=argparse.ArgumentParser();p.add_argument('run',type=Path);p.add_argument('--side',required=True,choices=['native','siu']);a=p.parse_args()
rows=[]
for name,item in [('flower-pouch','botania:flower_bag'),('petal-pouch','botania:petal_pouch'),('bauble-box','botania:bauble_box')]:
    actions=[('close',{}),('seed-player',{'items':[{'slot':0,'item':item,'count':1}],'ticks':16}),('client-use-item',{'ticks':16})] if a.side=='siu' else [('close',{}),('held',{'item':item,'count':1,'ticks':16}),('use-item',{'ticks':16})]
    for kind,kw in actions:result=send(a.run,kind,**kw)
    state=result['client'];assert state.get('sessions',0)>0 if a.side=='siu' else state.get('menuClass','').startswith('vazkii.botania.'),state
    stem=a.side+'-botania-'+name+'-454-1211-neoforge'
    result=send(a.run,'capture',capture=stem,ticks=16)
    rows.append({'id':name,'side':a.side,'screenshot':str(a.run/'screenshots'/(stem+'.png')),'state':result['client']})
    (a.run/'botania-pouches-capture-journal.json').write_text(json.dumps(rows,indent=2))
    print('CAPTURE',stem,flush=True)
