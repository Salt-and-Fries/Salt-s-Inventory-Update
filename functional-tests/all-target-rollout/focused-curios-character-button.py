"""Character attachment origin: normal Curios button toggles one attached panel."""
from pathlib import Path
import argparse,json,sys
sys.path.insert(0,str(Path(__file__).parent))
from evidence import send
p=argparse.ArgumentParser();p.add_argument('run',type=Path);a=p.parse_args();checks=[]
def act(kind,**kw):return send(a.run,kind,**kw)['client']
def panels(s):return [w for w in s['windows'] if w.get('menuId')=='salts_inventory_compat:curios_accessories']
def check(label,ok):
 checks.append({'check':label,'passed':bool(ok)});(a.run/'curios-character-origin-focused-checks.json').write_text(json.dumps({'scope':'Character launcher hit area, attached-panel placement and close/reopen only; not a full equipment suite','checks':checks},indent=2));assert ok,label;print('PASS',label,flush=True)
def tap(s):
 b=next(w['bounds'] for w in s['workspaceWindows'] if w['kind']=='CHARACTER')
 return act('xy-click',x=b['x']+32,y=b['y']+30,ticks=16)
s=act('wait',ticks=2);check('normal-launcher-opens-exactly-one-Curios-panel',len(panels(s))==1)
b=panels(s)[0]['bounds'];parent=next(w['bounds'] for w in s['workspaceWindows'] if w['kind']=='CHARACTER')
check('panel-attaches-above-Character-within-viewport',b['x']==parent['x'] and b['y']>=0 and b['y']+b['height']<=parent['y'])
s=tap(s);check('same-native-launcher-closes-only-its-panel',not panels(s) and any(w['kind']=='CHARACTER' for w in s['workspaceWindows']))
s=tap(s);check('launcher-reopens-one-panel-with-live-world',len(panels(s))==1 and s.get('worldLoaded'))
