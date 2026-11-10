"""Use the publisher's real settings button to reach its modifier inventory."""
from pathlib import Path
import argparse,json,sys
sys.path.insert(0,str(Path(__file__).parent))
from evidence import send,capture

p=argparse.ArgumentParser();p.add_argument('run',type=Path)
p.add_argument('--side',choices=['native','siu'],required=True)
p.add_argument('--minecraft',required=True);p.add_argument('--loader',required=True)
p.add_argument('--reference',type=Path,help='Native entry geometry when the Core observer has no native-widget enumeration')
a=p.parse_args()
send(a.run,'catalog',ticks=1)
catalog=json.loads((a.run/'catalog.json').read_text())
if 'waystones:waystone_modifiers' not in catalog['menus']:
    print('NOT APPLICABLE: this original release has no modifier inventory.',flush=True)
    raise SystemExit(0)
send(a.run,'close',ticks=3)
state=send(a.run,'fixture',block='waystones:waystone',ticks=20)['client']
if 'WaystoneEditScreen' not in state.get('screen',''):
    state=send(a.run,'client-use-block' if a.side=='siu' else 'open',ticks=20)['client']
assert 'WaystoneEditScreen' in state.get('screen',''),state.get('screen')
assert not state.get('windows',state.get('allWindows',[])), 'Waystone settings should remain native'
entry=state
widgets=state.get('widgets',[])
if not widgets and a.reference:
    reference=json.loads((a.reference/'waystones-modifier-settings-entry.json').read_text())
    assert reference['screen']==state['screen']
    dx=state['bounds']['x']-reference['bounds']['x'];dy=state['bounds']['y']-reference['bounds']['y']
    widgets=[{**w,'x':w['x']+dx,'y':w['y']+dy} for w in reference['widgets']]
button=next(w for w in widgets if w.get('visible',True) and ('manage_modifiers' in w.get('message','').lower() or 'manage modifiers' in w.get('message','').lower()))
(a.run/'waystones-modifier-settings-entry.json').write_text(json.dumps(entry,indent=2))
x=button['x']+button['width']/2;y=button['y']+button['height']/2
if a.side=='siu' and a.run.parent.parent.name=='all-target-rollout':
    # This first screen intentionally remains native; route the normal button
    # event to that screen, not the desktop's window-input dispatcher.
    state=send(a.run,'native-click',x=x,y=y,button=0,ticks=20)['client']
else:
    state=send(a.run,'click',x=x,y=y,ticks=20)['client']
    send(a.run,'release',x=x,y=y,ticks=3)
if a.side=='native':
    assert 'WaystoneModifierScreen' in state.get('screen',''),state.get('screen')
    assert len([s for s in state['slots'] if not s.get('player',False)])>=5
else:
    windows=state.get('windows',state.get('allWindows',[]))
    assert any(w.get('menuId')=='waystones:waystone_modifiers' and 'NativeTemplateWindow' in w.get('definition','') for w in windows), windows
capture(a.run,a.side,a.minecraft,a.loader,'waystones-modifiers',
        'Five-slot inventory reached through the original Manage Modifiers settings button. Non-inventory settings and destination selection remain native. Entry and pictured layout only.')
(a.run/'waystones-modifier-entry-checks.json').write_text(json.dumps({
    'native_settings_retained':True,'real_manage_modifiers_transition':True,
    'inventory_captured':True,'scope':'Natural entry and pictured state; no modifier consumption assertion'},indent=2))
