"""Focused return-tab regressions; no full functional/gameplay suite."""
from pathlib import Path
import sys,json,time,shutil
ROOT=Path(__file__).resolve().parents[2]
sys.path.insert(0,str(ROOT/'functional-tests/ae2-wireless-1.20.1'))
import focused as f
version,run=sys.argv[1:3]
f.RUN=Path(run).resolve();f.OUT=ROOT/'build/ae2-tab-join-fix'/version;f.OUT.mkdir(parents=True,exist_ok=True);f.LOG=[]
checks=[]
def S(kind,**args):
    response=f.send(f.RUN,kind,**args);f.LOG.append({'command':kind,'args':args,'client':response['client']});return response['client']
f.S=S
def check(name,value):
    assert value,name
    checks.append({'name':name,'pass':True});(f.OUT/'checks.json').write_text(json.dumps(checks,indent=2));print('PASS',name,flush=True)
def w(s):return next(w for w in s['allWindows'] if w.get('definition','').endswith('.AEWindow'))
def observe():return S('wait',ticks=10)
def button(s,label):return next(a for a in w(s)['nativeWidgets'] if a['visible'] and a['message'].split('\n')[0]==label)
def press(s,label):
    b=button(s,label);return f.tap(b['x']+b['width']/2,b['y']+b['height']/2)
def picture(name,s):
    f.save(name,s);p=f.RUN/'screenshots'/(name+'.png')
    for _ in range(100):
        if p.exists() and p.stat().st_size:break
        time.sleep(.05)
    # The shipping observer also retains the complete, uncropped frame.
def return_tab(s,name):
    b=next(b for b in w(s)['nativeWidgets'] if b.get('visible') and b.get('tabIcon')=='BACK')
    check(name+' uses native framed return tab',b['tabStyle']=='BOX')
    if name in ['trash','magnet-filter','wireless-settings']:
        check(name+' return remains top-right',b['x']>=w(s)['bounds']['x']+w(s)['bounds']['width']-28 and b['y']<w(s)['bounds']['y']+8)
    else:
        check(name+' native header anchor retained',b['y']<w(s)['bounds']['y']+8)
    picture(name,s)
    s=f.tap(b['x']+b['width']/2,b['y']+b['height']/2)
    return s

S('close',ticks=3);f.cmd('gamemode survival @a');f.cmd('clear @a')
f.cmd('fill 4 64 4 14 64 14 minecraft:stone');f.cmd('fill 6 65 6 11 68 10 minecraft:air')
f.cmd('setblock 8 65 8 ae2:wireless_access_point');f.cmd('setblock 8 65 9 ae2:creative_energy_cell',30)
f.cmd('tp @a 8.5 65 12.5 180 8',20)
f.cmd('item replace entity @a weapon.mainhand with ae2:wireless_crafting_terminal[ae2:stored_energy=1000000.0d,ae2:wireless_link_target={dimension:"minecraft:overworld",pos:[I;8,65,8]}]',12)
s=S('use-item',ticks=25);check('Native WCT opens',w(s)['nativeScreen'].endswith('WCTScreen'))
s=S('inventory',ticks=30);f.cmd('item replace entity @a inventory.0 with ae2wtlib:magnet_card',12)
s=observe();s=f.itap(s,9);upgrade=next(a['index'] for a in w(s)['slotVisibility'] if a['visible'] and 'id=UPGRADE,' in a['semantic']);s=f.slot(w(s),upgrade);s=observe()
check('Native magnet control remains available',button(s,'Magnet Filter')['active'])
# Remove the separate Inventory panel from these focused pictures.
inventory=next(a for a in s['workspaceWindows'] if a['kind']=='INVENTORY')['bounds'];s=f.tap(inventory['x']+inventory['width']-9,inventory['y']+8)
for label,screen,name in [('Trash','TrashScreen','trash'),('Magnet Filter','MagnetScreen','magnet-filter'),('Wireless Terminal Settings','WirelessTerminalSettingsScreen','wireless-settings'),('Terminal Settings','TerminalSettingsScreen','terminal-settings')]:
    s=press(s,label);check(name+' opens native view',w(s)['nativeScreen'].endswith(screen))
    s=return_tab(s,name);check(name+' returns to WCT',w(s)['nativeScreen'].endswith('WCTScreen'))
# A second parent exercises the same shared fix outside AE2WTLib.
S('close',ticks=10);f.cmd('setblock 10 65 8 ae2:pattern_provider',12);S('close',ticks=10);f.cmd('tp @a 10.5 65 5.5 0 20',25)
s=S('use-block-ray',ticks=20);check('Native provider opens',w(s)['nativeScreen'].endswith('PatternProviderScreen'))
s=press(s,'Priority');check('Native priority opens',w(s)['nativeScreen'].endswith('PriorityScreen'))
s=return_tab(s,'priority');check('Priority returns to provider',w(s)['nativeScreen'].endswith('PatternProviderScreen'))
(f.OUT/'observations.json').write_text(json.dumps(f.LOG,indent=2))
print(version,'PASS',len(checks),'focused checks',flush=True)
