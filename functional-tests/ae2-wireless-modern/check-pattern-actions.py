"""Focused WET encoding and WAT provider-ID actions through real mouse input."""
from pathlib import Path
import sys, json, shutil, time
ROOT=Path(__file__).resolve().parents[2]
sys.path.insert(0,str(ROOT/'functional-tests/ae2-wireless-1.20.1'))
import focused as f
version,run=sys.argv[1:3]
f.RUN=Path(run).resolve();f.OUT=ROOT/'build/ae2-wireless-all-targets'/version;f.LOG=[]
def observed_send(kind, **args):
    response=f.send(f.RUN,kind,**args)
    f.LOG.append({'command':kind,'args':args,'client':response['client']})
    return response['client']
f.S=observed_send
checks=[]
def check(name,value):
    assert value,name
    checks.append({'name':name,'pass':True});(f.OUT/'pattern-checks.json').write_text(json.dumps(checks,indent=2));print('PASS',name,flush=True)
def state():return f.S('wait',ticks=12)
def w(s):return next(x for x in s['allWindows'] if x.get('definition','').endswith('.AEWindow'))
def slots(s,semantic):return [w(s)['slots'][x['index']] for x in w(s)['slotVisibility'] if x['visible'] and 'id='+semantic+',' in x['semantic']]
def slot(s,semantic):return f.slot(w(s),slots(s,semantic)[0]['index'])
def press(s,label):
    a=next(x for x in w(s)['nativeWidgets'] if x['visible'] and x['message'].split('\n')[0]==label)
    return f.tap(a['x']+a['width']/2,a['y']+a['height']/2)
def invitem(n):return next(x for x in json.loads((f.RUN/'reply.json').read_text())['server']['playerInventory'] if x['index']==n)['item']
def inv(s):return s if any(x['kind']=='INVENTORY' for x in s['workspaceWindows']) else f.S('inventory',ticks=30)
def openitem(item):
    f.S('close',ticks=5);f.cmd('item replace entity @a weapon.mainhand with '+item+'[ae2:stored_energy=1000000.0d,ae2:wireless_link_target={dimension:"minecraft:overworld",pos:[I;8,65,8]}]',12)
    return f.S('use-item',ticks=30)
def picture(name,s):
    f.save(name,s)
    source=f.RUN/'screenshots'/(name+'.png')
    for attempt in range(100):
        if source.exists():break
        time.sleep(.05)
    shutil.copyfile(source,f.OUT/(name+'.png'))

s=openitem('ae2wtlib:wireless_pattern_encoding_terminal');s=inv(s)
f.cmd('item replace entity @a inventory.1 with ae2:blank_pattern 4',12)
s=state();s=f.itap(s,10);s=slot(s,'BLANK_PATTERN')
check('Blank patterns enter native encoder',slots(s,'BLANK_PATTERN')[0]['item']=='ae2:blank_pattern')
f.cmd('item replace entity @a inventory.1 with minecraft:oak_log',12)
s=state();s=f.itap(s,10);s=slot(s,'CRAFTING_GRID');s=f.itap(s,10)
check('Native encoding preview recipe',slots(s,'CRAFTING_RESULT')[0]['item']=='minecraft:oak_planks')
s=press(s,'Encode Pattern')
check('Native Encode action creates crafting pattern',slots(s,'ENCODED_PATTERN')[0]['item']=='ae2:crafting_pattern')
s=slot(s,'ENCODED_PATTERN');s=f.itap(s,12)
check('Encoded pattern transfers to player',invitem(12)=='ae2:crafting_pattern')
picture('encoded-pattern',s)
f.S('close',ticks=5)
f.cmd('setblock 9 65 9 ae2:controller')
f.cmd('setblock 10 65 9 ae2:controller')
f.cmd('setblock 10 65 8 ae2:pattern_provider',20)
f.S('close',ticks=12)
f.cmd('tp @a 10.5 65 5.5 0 20',30)
s=f.S('use-block-ray',ticks=25);s=inv(s)
check('Provider opens through native block use',w(s)['nativeScreen'].endswith('PatternProviderScreen'))
providerSlot=next(x for x in w(s)['slots'] if not x['player'] and x['x']>=0 and x['y']>=0 and 'RestrictedInputSlot' in x['type'])['index']
s=f.itap(s,12);s=f.slot(w(s),providerSlot)
check('Pattern installed in live provider',w(s)['slots'][providerSlot]['item']=='ae2:crafting_pattern')
s=openitem('ae2wtlib:wireless_pattern_access_terminal');s=inv(s);s=state()
point=w(s)['wirelessGeometry']['slot/PatternSlot/1']
picture('pattern-access-populated',s)
s=f.native(w(s),point[0]+7,point[1]+7);s=f.itap(s,13);s=state()
check('Provider-ID action withdraws pattern',invitem(13)=='ae2:crafting_pattern')
s=f.itap(s,13);s=f.native(w(s),point[0]+7,point[1]+7);s=state()
s=f.native(w(s),point[0]+7,point[1]+7);s=f.itap(s,13);s=state()
check('Provider-ID action reinserts and withdraws pattern',invitem(13)=='ae2:crafting_pattern')
s=f.itap(s,13);s=f.native(w(s),point[0]+7,point[1]+7);s=state()
(f.OUT/'pattern-observations.json').write_text(json.dumps(f.LOG,indent=2))
print(version,'PASS',len(checks),'pattern actions',flush=True)
