"""Focused, ordinary-input probes for the disposable Traveler's Backpack world."""
import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / 'functional-tests/project-everything-ten'))
from send import send

RUN = ROOT / 'build/remaining-ui-audit/gameplay/1.20.1-fabric-integrated-travelers11'
OUT = ROOT / 'build/travelers-backpack-window'

def act(kind='capture', **kw):
    return send(RUN, kind, **kw)['client']

def save(name, state=None):
    state = act(capture=name) if state is None else state
    (OUT / (name + '.json')).write_text(json.dumps(state, indent=2))
    return state

def window(state=None):
    s = state or act()
    return next(w for w in reversed(s['allWindows']) if w.get('menuId','').startswith('travelersbackpack:'))

def point(sx, sy, state=None):
    w = window(state)
    for r in w['composition']['regions']:
        src = r['source']
        if src['x'] <= sx < src['x']+src['width'] and src['y'] <= sy < src['y']+src['height']:
            b=w['bounds']; inset=(0,0) if r.get('exterior',False) else (8,24)
            return (b['x']+inset[0]+r['x']+sx-src['x'], b['y']+inset[1]+r['y']+sy-src['y'])
    raise ValueError(('Unmapped point', sx, sy))

def clickxy(x,y,button=0):
    act('click',x=x,y=y,button=button)
    return act('release',x=x,y=y,button=button)

def click(sx,sy,button=0):
    return clickxy(*point(sx,sy),button=button)

def slot(index,button=0):
    w=window(); c=w['slots'][index]
    return click(c['x']+8,c['y']+8,button)

def server():
    act('snapshot-server')
    return json.loads((RUN/'server-state.json').read_text())

def backpack_menu():
    return next(m for m in server()['menus'] if 'travelersbackpack' in m['class'])

def seed(index, item, count=1, nbt=None):
    args=dict(menu=window()['menu'],slot=index,item=item,count=count)
    if nbt:args['nbt']=nbt
    return act('reviewed-session-seed',**args)

def start(upgraded=False):
    act('fixture',block='minecraft:stone')
    for cmd in ['gamemode survival @a','time set noon','gamerule doDaylightCycle false','weather clear']:
        act('command',command=cmd)
    fixtures=json.loads((ROOT/'functional-tests/travelers-backpack-window/fixtures.json').read_text())
    act('command',command='item replace entity @a weapon.mainhand with '+fixtures['upgraded' if upgraded else 'default'])
    return act('use-item',ticks=15)

def widget(cls, dx=12, dy=12, button=0):
    w=window(); native=w['nativeBounds']
    v=next(v for v in w['nativeValues']['customWidgets'] if v['type']==cls)
    return click(v['bounds'][0]-native['x']+dx,v['bounds'][1]-native['y']+dy,button)

def check(name, condition, details=None):
    result=dict(name=name,passed=bool(condition),details=details)
    dest=OUT/'focused-checks.json'
    old=json.loads(dest.read_text()) if dest.exists() else []
    old.append(result);dest.write_text(json.dumps(old,indent=2))
    print(('PASS ' if condition else 'FAIL ')+name,flush=True)
    assert condition, result

if __name__=='__main__':
    s=start('--upgraded' in sys.argv)
    save('upgraded-window' if '--upgraded' in sys.argv else 'base-window',s)
    print(json.dumps(s,indent=2))
