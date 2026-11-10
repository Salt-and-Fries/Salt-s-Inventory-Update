import json
import probe as p
def item(upgrades, tier=0):
    cells=','.join('{Slot:%d,id:"travelersbackpack:%s_upgrade",Count:1b,tag:{TabOpen:0b}}'%(i,n) for i,n in enumerate(upgrades))
    return 'travelersbackpack:standard{Tier:%d,StorageSlots:%d,UpgradeSlots:%d,ToolSlots:2,Upgrades:{Size:%d,Items:[%s]}}'%(tier,27 if tier==0 else 99,2 if tier==0 else 6,2 if tier==0 else 6,cells)

def open_item(stack):
    p.act('fixture',block='minecraft:stone')
    p.act('command',command='item replace entity @a weapon.mainhand with '+stack)
    p.act('use-item',ticks=15)

w=p.window()
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
