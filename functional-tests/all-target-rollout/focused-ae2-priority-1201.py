"""Real priority-tab entry, native +/- updates, return and reopening checks."""
from pathlib import Path
import argparse,json,re,sys
sys.path.insert(0,str(Path(__file__).parent))
from evidence import send
p=argparse.ArgumentParser();p.add_argument('run',type=Path);a=p.parse_args();checks=[]
def state():return send(a.run,'wait',ticks=2)['client']
def current():return state()['windows'][0]
def check(label,ok):
 checks.append({'check':label,'passed':bool(ok)});(a.run/'ae2-priority-focused-checks.json').write_text(json.dumps({'scope':'Priority tab, three native increment actions, return and reopening only','checks':checks},indent=2));assert ok,label;print('PASS',label,flush=True)
def button(message):
 b=next(w for w in current()['nativeWidgets'] if w.get('visible') and w.get('message')==message)
 send(a.run,'xy-click',x=b['x']+b['width']/2,y=b['y']+b['height']/2,ticks=16)
check('actual-priority-menu-open',current()['menuId']=='ae2:priority')
for label in ['+100','+10','+1']:button(label)
send(a.run,'reviewed-block-probe',ticks=2);nbt=json.loads((a.run/'reviewed-block-probe.json').read_text())['nbt']
check('native-increments-update-real-storage-bus-priority',re.search(r'priority:111(?:[,}])',nbt) is not None)
button('ME Storage Bus');check('native-return-tab-restores-owning-bus',current()['menuId']=='ae2:storagebus')
button('Priority');check('priority-reopens-through-original-tab',current()['menuId']=='ae2:priority')
send(a.run,'reviewed-block-probe',ticks=2);nbt=json.loads((a.run/'reviewed-block-probe.json').read_text())['nbt']
check('priority-value-survives-native-round-trip',re.search(r'priority:111(?:[,}])',nbt) is not None)
