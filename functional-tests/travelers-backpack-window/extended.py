"""Additional affected controls and native upgrade families. No general gameplay suite."""
import json
import probe as p

def item(upgrades, tier=0):
    cells=','.join('{Slot:%d,id:"travelersbackpack:%s_upgrade",Count:1b,tag:{TabOpen:0b}}'%(i,n) for i,n in enumerate(upgrades))
    return 'travelersbackpack:standard{Tier:%d,StorageSlots:%d,UpgradeSlots:%d,ToolSlots:2,Upgrades:{Size:%d,Items:[%s]}}'%(tier,27 if tier==0 else 99,2 if tier==0 else 6,2 if tier==0 else 6,cells)

def open_item(stack):
    p.act('fixture',block='minecraft:stone')
    p.act('command',command='item replace entity @a weapon.mainhand with '+stack)
    p.act('use-item',ticks=15)

# Fill and drain both tanks using the real native tank action and shared SIU cursor.
open_item(item(['tanks']))
p.seed(0,'minecraft:water_bucket');p.slot(0);p.click(16,40)
m=p.backpack_menu();s=p.act()
p.check('Tank fills from the shared cursor and returns an empty bucket','minecraft:bucket' in m['carried'] and 'amount:81000L' in s['held'] and 'minecraft:water' in s['held'])
p.click(16,40);m=p.backpack_menu();s=p.save('tank-fill-drain')
p.check('Tank drains back into a bucket without duplicating fluid','minecraft:water_bucket' in m['carried'] and 'amount:81000L' not in s['held'])
p.slot(0)
p.seed(1,'minecraft:lava_bucket');p.slot(1)
# Nine-column right tank begins at native x=195.
p.click(204,40);m=p.backpack_menu();s=p.save('tanks-fluid')
p.check('Right tank accepts lava independently','minecraft:bucket' in m['carried'] and 'minecraft:lava' in s['held'])
p.slot(1)

# Every remaining upgrade UI is built by its original upstream widget.
for name,cls in [('feeding','FeedingWidget'),('refill','UpgradeWidgetBase'),('jukebox','JukeboxWidget'),('lantern','LanternWidget'),('blast_furnace','AbstractSmeltingWidget'),('smoker','AbstractSmeltingWidget')]:
    capture='upgrade-'+name.replace('_','-')
    open_item(item([name]))
    w=p.window();widgets=w['nativeValues']['customWidgets']
    p.check(name+' upgrade retains its native widget',any(v['type']==cls for v in widgets))
    if name=='lantern':
        # This upgrade intentionally has no expanded panel, only native enable/remove controls.
        p.save(capture)
    else:
        p.widget(cls);p.save(capture)
        p.check(name+' upgrade opens its original panel',next(v for v in p.window()['nativeValues']['customWidgets'] if v['type']==cls)['open'])

# Filter ghosts and state buttons must stay virtual and server authoritative.
open_item(item(['pickup']))
p.widget('AutoPickupWidget')
w=p.window();filters=[c for c in w['slots'] if c['index']>=w['nativeValues']['UPGRADE_END']+36 and c['y']>=0]
p.check('Pickup panel exposes its original filter cells',len(filters)==9)
p.seed(0,'minecraft:stone',5);p.slot(0);p.slot(filters[0]['index'])
m=p.backpack_menu()
p.check('Filter click copies one ghost without consuming the carried stack','Count:5b' in m['carried'] and 'Count:1b' in m['slots'][filters[0]['index']]['item'])
p.slot(0)
before=p.act()['held'];p.widget('AutoPickupWidget',12,30);after=p.act()['held']
p.check('Pickup filter mode button persists its setting',before!=after)
p.save('filter-settings')
p.widget('AutoPickupWidget')
before=p.act()['held'];p.widget('AutoPickupWidget',25,12);after=p.act()['held']
p.check('Upgrade enable switch persists its state',before!=after)

# Equipment actions remain native. Open the worn backpack with its real key binding.
open_item(item(['crafting']))
w=p.window();a=next(r for r in w['composition']['regions'] if r['id']=='action/0')['source']
p.click(a['x']+5,a['y']+5);p.act('wait',ticks=12)
if not p.act()['allWindows']:
    raise SystemExit('Equip succeeded. Press the real B key, then run modes.py; key-down flags do not generate the native consumeClick event.')
w=p.window()
p.check('Equip action and native backpack key open the worn inventory',w['nativeValues']['screenID']==2)
p.save('worn-backpack')
# More actions is the native small arrow on worn backpacks.
for r in w['composition']['regions']:
    if r['id'].startswith('action/'):
        p.click(r['source']['x']+5,r['source']['y']+5);break
p.save('worn-actions')
p.check('Worn backpack exposes native extra actions',len([r for r in p.window()['composition']['regions'] if r['id'].startswith('action/')])>=3)

# Placed backpack: use its real block opener, then run an actual furnace recipe.
p.act('fixture',block='minecraft:stone')
stack=item(['furnace'])
tag=stack[stack.index('{'):]
p.act('command',command='setblock 8 65 8 travelersbackpack:standard{Backpack:{id:"travelersbackpack:standard",Count:1b,tag:'+tag+'}}')
p.act('command',command='item replace entity @a weapon.mainhand with minecraft:air')
p.act('open',ticks=15)
w=p.window()
p.check('Placed backpack opens its native block menu',w['menuId']=='travelersbackpack:backpack_block' and w['nativeValues']['screenID']==3)
p.widget('AbstractSmeltingWidget')
w=p.window();cells=[c for c in w['slots'] if c['index']>=w['nativeValues']['UPGRADE_END']+36 and c['y']>=0]
p.check('Placed furnace exposes all three native cells',len(cells)==3)
p.seed(0,'minecraft:iron_ore');p.seed(1,'minecraft:coal')
p.slot(0);p.slot(cells[0]['index']);p.slot(1);p.slot(cells[1]['index'])
p.act('wait',ticks=25);p.save('placed-smelting-active')
p.act('wait',ticks=220);m=p.backpack_menu();w=p.window()
p.check('Placed backpack furnace actually smelts and synchronizes its result','iron_ingot' in m['slots'][cells[2]['index']]['item'] and w['slots'][cells[2]['index']]['item']=='minecraft:iron_ingot')
p.slot(cells[2]['index']);p.slot(2)
p.check('Furnace output can be taken through SIU','iron_ingot' in p.backpack_menu()['slots'][2]['item'])
p.save('placed-smelting-done')
p.widget('SettingsWidget');p.check('Placed backpack settings opens',p.window()['menuId']=='travelersbackpack:backpack_settings')
p.widget('SettingsWidget');p.check('Placed settings returns to the block backpack',p.window()['menuId']=='travelersbackpack:backpack_block')
print('Extended focused backpack checks complete.',flush=True)
