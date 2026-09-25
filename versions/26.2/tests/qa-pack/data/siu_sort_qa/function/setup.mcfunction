fill -5 200 -2 5 200 5 minecraft:stone
fill -5 201 -2 5 204 5 minecraft:air
setblock -3 201 0 minecraft:chest{Items:[{Slot:0b,id:"minecraft:dirt",count:1},{Slot:1b,id:"minecraft:sand",count:1}]}
setblock -1 201 0 minecraft:chest{Items:[{Slot:0b,id:"minecraft:iron_ingot",count:1},{Slot:1b,id:"minecraft:diamond",count:1}]}
setblock 1 201 0 minecraft:chest{Items:[{Slot:0b,id:"minecraft:rotten_flesh",count:1},{Slot:1b,id:"minecraft:bone",count:1}]}
setblock 3 201 0 minecraft:barrel
item replace entity @s inventory.0 with minecraft:bone 5
item replace entity @s inventory.1 with minecraft:iron_ingot 6
item replace entity @s inventory.2 with minecraft:sand 7
item replace entity @s inventory.3 with minecraft:gravel 8
item replace entity @s inventory.4 with minecraft:gold_ingot 9
item replace entity @s hotbar.0 with minecraft:bone 10
tp @s 0.5 201 3.5 135 15
