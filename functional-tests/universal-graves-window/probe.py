"""Focused ordinary-input checks in the disposable Universal Graves world.

Only fixture creation uses commands. Grave menus and actions use real clicks.
"""
import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / 'functional-tests/project-everything-ten'))
from send import send

RUN = ROOT / 'build/remaining-ui-audit/gameplay/1.20.1-fabric-integrated-graves02'
OUT = ROOT / 'build/universal-graves-window/evidence-final'
OUT.mkdir(parents=True, exist_ok=True)

def act(kind='capture', **kw):
    return send(RUN, kind, **kw)['client']

def click(x, y, button=0):
    act('click', x=x, y=y, button=button)
    return act('release', x=x, y=y, button=button)

def window(s=None):
    return next(w for w in (s or act())['allWindows'] if w['menuId'].endswith(':universal_grave'))

def slot(i, button=0):
    b = window()['bounds']
    return click(b['x']+23+(i%9)*18, b['y']+(33+(i//9)*18 if i<36 else 119), button)

def server(name=None):
    act('snapshot-server', ticks=1)
    d = json.loads((RUN/'server-state.json').read_text())
    if name: (OUT/(name+'.json')).write_text(json.dumps({'client':act(), 'server':d}, indent=2))
    return d

def menu(d=None):
    return next(m for m in (d or server())['menus'] if m['class'].endswith('.VirtualScreenHandler'))

def inventory_slot(i):
    s=act()
    if i<9:
        return click(s['guiWidth']//2-90+9+i*20,s['guiHeight']-11)
    w=next((w for w in s['workspaceWindows'] if w['kind']=='INVENTORY'),None)
    if w is None:
        s=act('inventory');w=next(w for w in s['workspaceWindows'] if w['kind']=='INVENTORY')
    p=next(c for c in w['inventoryCells'] if c['inventorySlot']==i)
    return click(p['x'],p['y'])

def check(name, passed, details=None):
    dest=OUT/'checks.json'
    rows=json.loads(dest.read_text()) if dest.exists() else []
    rows.append(dict(name=name,passed=bool(passed),details=details))
    dest.write_text(json.dumps(rows,indent=2))
    print(('PASS ' if passed else 'FAIL ')+name,flush=True)
    assert passed, (name,details)

def list_open(modify=False, index=0):
    s=act('command',command='execute as @a run graves'+(' modify' if modify else ''))
    b=s['bounds'];return click(b['x']+16+(index%9)*18,b['y']+26+(index//9)*18)

def move_window(dx,dy):
    b=window()['bounds'];x,y=b['x']+20,b['y']+7
    act('click',x=x,y=y,button=0)
    act('drag',x=x+dx,y=y+dy,button=0,dx=dx,dy=dy)
    return act('release',x=x+dx,y=y+dy,button=0)

def command(text,**kw):return act('command',command=text,**kw)
