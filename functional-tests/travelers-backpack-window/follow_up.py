"""Focused follow-up for dynamic upgrades, settings transitions and SIU controls."""
import json
import probe as p

p.start()
p.check('Final build opens and protects the item backpack',p.window()['menuId']=='travelersbackpack:backpack_item' and p.server()['selectedLocked'])
p.seed(0,'travelersbackpack:crafting_upgrade');p.slot(0);p.slot(29)
p.check('Installing an upgrade rebuilds both server and client menus',len(p.backpack_menu()['slots'])==77 and len(p.window()['slots'])==77)
p.widget('CraftingWidget')
p.check('Dynamically installed upgrade opens immediately',next(v for v in p.window()['nativeValues']['customWidgets'] if v['type']=='CraftingWidget')['open'])
p.seed(0,'minecraft:oak_log');p.slot(0);p.slot(67)
p.check('Newly installed crafting upgrade shows its recipe result',p.window()['slots'][76]['item']=='minecraft:oak_planks')
p.slot(76);p.slot(1)
p.check('Newly installed upgrade crafts through the original recipe','oak_planks' in p.backpack_menu()['slots'][1]['item'])
p.widget('CraftingWidget',54,12)
p.check('Native remove-upgrade action rebuilds both menus',len(p.backpack_menu()['slots'])==67 and len(p.window()['slots'])==67)
p.check('Removed upgrade is returned to the player',any('travelersbackpack:crafting_upgrade' in cell['item'] for cell in p.server()['inventory']))
p.save('dynamic-upgrade-removed')

# Placed settings must return on the first click, even across repeated replacements.
p.act('fixture',block='minecraft:stone')
p.act('command',command='setblock 8 65 8 travelersbackpack:standard{Backpack:{id:"travelersbackpack:standard",Count:1b,tag:{Tier:0,StorageSlots:27,UpgradeSlots:2,ToolSlots:2}}}')
p.act('command',command='item replace entity @a weapon.mainhand with minecraft:air')
p.act('open',ticks=15)
for cycle in range(2):
    p.widget('SettingsWidget')
    p.check('Placed settings opens on cycle '+str(cycle+1),p.window()['menuId']=='travelersbackpack:backpack_settings')
    p.widget('SettingsWidget')
    p.check('Placed settings returns to the block backpack' if cycle==0 else 'Placed settings returns on the first click after repeated transitions',p.window()['menuId']=='travelersbackpack:backpack_block')
p.save('placed-window')

p.start(True)
p.seed(0,'minecraft:diamond',7);p.seed(7,'minecraft:stone',3);p.seed(8,'minecraft:diamond',5)
before=p.backpack_menu();controls=p.act()['workspaceWindows'][0]['controls']
p.clickxy(**controls['SORT_INVENTORY']);after=p.backpack_menu()
diamonds=[s for s in after['slots'][:99] if 'minecraft:diamond' in s['item']]
p.check('SIU sorting merges storage stacks',len(diamonds)==1 and 'Count:12b' in diamonds[0]['item'])
p.check('SIU sorting leaves tools and upgrades in place',before['slots'][99:111]==after['slots'][99:111])
p.act('inventory')
p.act('command',command='item replace entity @a inventory.0 with minecraft:stone 4')
inv=next(w for w in p.act()['workspaceWindows'] if w['kind']=='INVENTORY')
p.clickxy(**inv['controls']['SORT_WINDOWS']);after=p.backpack_menu()
stones=[s for s in after['slots'][:99] if 'minecraft:stone' in s['item']]
p.check('SIU sorts matching items into the backpack from the player inventory',len(stones)==1 and 'Count:7b' in stones[0]['item'] and 'Count:0b' in p.server()['inventory'][9]['item'])
p.save('workspace-final')
p.act('gui-scale',scale=3,width=960,height=720)
w=p.window();p.check('Compact GUI keeps a native storage scrollbar',any(r['id']=='native-scroll' for r in w['composition']['regions']))
x,y=p.point(80,40);p.act('scroll',x=x,y=y,amount=-4)
w=p.window();p.check('Compact GUI can reach the final storage row',w['nativeValues']['scrollAmount']>0 and w['slots'][98]['y']>=0)
p.save('compact-scrolled')
p.act('gui-scale',scale=2,width=1280,height=900)
p.save('workspace-final')
print('Follow-up focused checks complete.',flush=True)
