"""Polymer's native creative browser is an action selector, not owned storage."""
from pathlib import Path
import hashlib,json
SOURCE_SHA256=hashlib.sha256(Path(__file__).read_bytes()).hexdigest()
MENU='eu.pb4.polymer.core.impl.ui.MicroUi$InternalScreenHandler'

def exercise(row,profile,act,expect,snapshot,state):
    row['mod_id']='polymer-core'
    row.setdefault('test_helper_sha256',{})[__name__]=SOURCE_SHA256
    def browser():
        act('regression-native-browser',ticks=2)
        return json.loads((profile/'regression-native-browser.json').read_text())
    def identity(value):return [(v['index'],v['item'],v['count'],v['signature']) for v in value['slots']]
    original=browser();before=snapshot()
    expect('Polymer browser retains its real native action handler',original['menuClass']==MENU)
    expect('creative category selector remains native instead of owned storage',not before['menus'] and not state.get('windows') and state.get('screen') not in [None,'none','com.salts_inventory_update.client.InventoryDesktopScreen'])
    category=next(v for v in original['slots'] if v['item']=='minecraft:bricks' and 'buildingBlocks' in v['signature'])
    act('regression-native-slot-tap',slot=category['index'],ticks=12)
    selected=browser()
    row['native_browser_navigation']={'handler':selected['menuClass'],'category_slot':category['index'],
        'category_grid_sha256':hashlib.sha256(json.dumps(identity(original)).encode()).hexdigest(),
        'item_grid_sha256':hashlib.sha256(json.dumps(identity(selected)).encode()).hexdigest(),
        'item_sample':[v['item'] for v in selected['slots'] if v['count']][:12]}
    # The native 1.21.1 building category starts with wood; stone is on a
    # later page. Require real building entries rather than inventing its order.
    expect('native category click replaces the category grid with its item list',selected['menuClass']==MENU and identity(original)!=identity(selected)
           and {'minecraft:oak_log','minecraft:oak_planks'}.issubset({v['item'] for v in selected['slots']}))
    after=snapshot()
    expect('category navigation grants no physical items and keeps the cursor',after['inventory']==before['inventory'] and after['cursor']==before['cursor'] and not after['menus'])
    act('regression-native-key',key=256,ticks=5)
    expect('native creative browser closes without leaving a desktop source',not snapshot()['menus'] and act('wait',ticks=3)['client'].get('screen')=='none')
    command=next(a for a in reversed(row['actions']) if a['type']=='command' and a['command'].endswith('run polymer creative'))
    act('command',command=command['command'],ticks=8)
    expect('public Polymer command reopens the same category browser',identity(browser())==identity(original) and not snapshot()['menus'])
    act('regression-native-key',key=256,ticks=5)
    act('fixture',block='minecraft:barrel',ticks=5);act('client-use-block',ticks=10)
    physical=snapshot();menu=next((m for m in physical['menus'] if m.get('menuType')=='minecraft:generic_9x3'),None)
    expect('native selector veto preserves ordinary vanilla barrel windows',menu is not None)
    sid=menu['session'];slot=next(v['index'] for v in menu['slots'] if not v['player'] and v['mayPlace'] and not v['count'])
    act('regression-seed-player',items=[{'slot':10,'item':'minecraft:diamond','count':7,'snbt':'{siu_regression_marker:42}'}],ticks=4)
    signature=snapshot()['inventory'][10]['signature']
    act('regression-player-click',slot=10,ticks=4);act('regression-slot-click',session=sid,slot=slot,ticks=4)
    stored=snapshot();cell=next(m for m in stored['menus'] if m['session']==sid)['slots'][slot]
    expect('ordinary barrel still accepts the exact tagged seven-item stack',cell['count']==7 and cell['signature']==signature and not stored['cursor']['count'])
    act('regression-slot-click',session=sid,slot=slot,ticks=4);act('regression-player-click',slot=11,ticks=4)
    restored=snapshot()
    expect('vanilla-shaped storage still returns items once with their metadata',restored['inventory'][11]['count']==7 and restored['inventory'][11]['signature']==signature and not restored['cursor']['count'])
    row.update(status='passed',menu_ids=['minecraft:generic_9x6'],native_screen_roundtrip={'screen':state.get('screen'),'menu':MENU},
               polymer_browser_checked=True,coverage='Native category navigation, inventory/cursor conservation, Escape/public-command reopening and tagged vanilla barrel transfers; creative item granting not certified.')
