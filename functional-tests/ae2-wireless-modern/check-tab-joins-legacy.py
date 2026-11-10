"""Focused 1.20.1 item-return tabs; keep their original colored icons intact."""
from pathlib import Path
import json,sys
ROOT=Path(__file__).resolve().parents[2]
sys.path.insert(0,str(ROOT/'functional-tests/ae2-wireless-1.20.1'))
import focused as f
loader,run=sys.argv[1:3];f.RUN=Path(run).resolve();f.OUT=ROOT/'build/ae2-tab-join-fix'/('1.20.1-'+loader);f.OUT.mkdir(exist_ok=True);f.LOG=[]
checks=[]
def S(kind,**args):
    response=f.send(f.RUN,kind,**args);f.LOG.append({'command':kind,'args':args,'client':response['client']});return response['client']
f.S=S
def check(name,condition):
    assert condition,name
    checks.append({'name':name,'pass':True});(f.OUT/'checks.json').write_text(json.dumps(checks,indent=2));print('PASS',name,flush=True)
def w(s):return next(x for x in s['allWindows'] if x.get('definition','').endswith('.AEWindow'))
def button(s,label):return next(x for x in w(s)['nativeWidgets'] if x.get('visible') and x.get('message','').split('\n')[0]==label)
def press(s,label):
    b=button(s,label);return f.tap(b['x']+b['width']/2,b['y']+b['height']/2)
S('fixture',block='ae2:wireless_access_point',ticks=10);f.cmd('gamemode survival @a');f.cmd('setblock 8 65 9 ae2:creative_energy_cell',20)
S('held',item='ae2:wireless_crafting_terminal',snbt='{accessPoint:{dimension:"minecraft:overworld",pos:[I;8,65,8]},internalCurrentPower:1000000.0d}',ticks=15);s=S('use-item',ticks=25)
check('Native crafting parent',w(s)['nativeScreen'].endswith('WCTScreen'))
s=S('inventory',ticks=30);f.cmd('item replace entity @a inventory.0 with ae2wtlib:magnet_card',12);s=S('wait',ticks=12);s=f.itap(s,9);s=f.slot(w(s),5);s=S('wait',ticks=12)
check('Native card installation',w(s)['slots'][5]['item']=='ae2wtlib:magnet_card')
views=[('Trash','TrashScreen','trash'),('Magnet Filter','MagnetScreen','magnet-filter'),('Terminal Settings','TerminalSettingsScreen','terminal-settings')]
if loader=='forge':views.append(('Wireless Terminal Settings','WirelessTerminalSettingsScreen','wireless-settings'))
for label,suffix,name in views:
    s=press(s,label);check(name+' native view',w(s)['nativeScreen'].endswith(suffix));b=button(s,'Wireless Crafting Terminal')
    check(name+' return remains visible',b['active'] and b['width']>=20 and b['height']>=20)
    f.save(name,s);s=press(s,'Wireless Crafting Terminal');check(name+' returns to parent',w(s)['nativeScreen'].endswith('WCTScreen'))
(f.OUT/'observations.json').write_text(json.dumps(f.LOG,indent=2));print(loader,'PASS',len(checks),'focused checks',flush=True)
