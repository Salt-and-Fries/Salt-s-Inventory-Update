"""Focused processing, controls and multiwindow evidence; not the full gameplay suite."""
import argparse
import re
from run_cases import Review


def items(*values):
    return [{"slot": slot,"item": item,"count": count} for slot,item,count in values]


def recipe_toggle(result):
    """Use the native template's committed region, rather than a historic button offset."""
    b=result['client']['window'];layout=result['client']['allWindows'][0]['templateLayout']
    match=re.search(r'id=recipe-toggle, source=DesktopWindowBounds\[x=-?\d+, y=-?\d+, width=(\d+), height=(\d+)\], x=(-?\d+), y=(-?\d+), interactive=true, cutouts=\[\], exterior=(true|false)',layout)
    if not match:raise AssertionError('No visible recipe-toggle region')
    w,h,x,y=map(int,match.groups()[:4]);exterior=match.group(5)=='true'
    return dict(x=b['x']+x+w/2+(0 if exterior else 8),y=b['y']+y+h/2+(0 if exterior else 24))


class Processing(Review):
    def below(self, block):
        x,y,z=self.origin
        self.command("command",command=f"setblock {x} {y-1} {z} {block}")

    def take_output(self, slot, expected, nbt=None):
        result=self.command("inspect")
        self.expect(result["server"]["slots"][slot]["item"]==expected and result["server"]["slots"][slot]["count"]>0,
                    "native processing produces the expected result")
        self.command("click",slot=slot)
        result=self.command("inspect")
        self.expect(result["server"]["cursor"]["item"]==expected and result["server"]["cursor"]["count"]>0,
                    "native result pickup preserves the shared cursor")
        self.command("store-in-player")
        result=self.command("inspect")
        self.expect(result["server"]["cursor"]["count"]==0,"processed output transfers to the player window")
        if nbt is not None:
            # A vanilla server command checks the actual conserved inventory NBT.
            self.command("command",command="item replace entity @a hotbar.8 with minecraft:air")
            self.command("command",command='execute as @a if data entity @s Inventory[{id:"'+expected+'",tag:'+nbt+'}] run item replace entity @s hotbar.8 with minecraft:lime_dye')
            result=self.command("inspect")
            self.expect(any(item["item"]=="minecraft:lime_dye" for item in result["server"]["slots"]),
                        "native result metadata survives pickup and cross-window transfer")
            self.command("command",command="item replace entity @a hotbar.8 with minecraft:air")
        self.capture("output-transferred")

    def cooking(self, copper=False):
        self.case="copper-pot" if copper else "cooking-pot"
        self.fixture("miners_delight:copper_pot" if copper else "farmersdelight:cooking_pot")
        self.below("minecraft:campfire[lit=true]")
        self.command("command",command="recipe give @a *")
        if copper:
            seed=items((0,"minecraft:carrot",8),(1,"minecraft:potato",8),(2,"minecraft:beetroot",8),
                       (3,"farmersdelight:cabbage_leaf",8),(5,"miners_delight:copper_cup",8))
        else:
            seed=items((0,"farmersdelight:rice",8),(7,"minecraft:bowl",8))
        self.command("seed",items=seed)
        self.capture("cooking-early")
        self.command("wait",ticks=40)
        result=self.capture("cooking-active")
        b=result["client"]["window"]
        self.command("xy-click",**recipe_toggle(result))
        self.capture("recipe-book")
        self.command("xy-click",**recipe_toggle(result))
        self.command("wait",ticks=160)
        self.capture("cooked")
        self.take_output(6 if copper else 8,"miners_delight:vegetable_soup_cup" if copper else "farmersdelight:cooked_rice")

    def identifying(self):
        self.case="identifying"
        self.fixture("betterarcheology:archeology_table")
        offset=36 if self.loader=="forge" else 0
        self.command("seed",items=items((offset,"minecraft:brush",1),(offset+1,"betterarcheology:unidentified_artifact",4)))
        self.capture("identifying-early")
        self.command("wait",ticks=55)
        self.capture("identifying-active")
        self.command("wait",ticks=220)
        self.capture("identified")
        self.take_output(offset+2,"minecraft:enchanted_book")

    def imbuing(self):
        self.case="imbuing"
        self.fixture("illagerinvasion:imbuing_table")
        ingredient=items((0,"minecraft:enchanted_book",1),(1,"minecraft:diamond_pickaxe",1),(2,"illagerinvasion:hallowed_gem",4))
        ingredient[0]["nbt"]='{StoredEnchantments:[{id:"minecraft:unbreaking",lvl:3s}]}'
        ingredient[1]["nbt"]='{Enchantments:[{id:"minecraft:unbreaking",lvl:3s}]}'
        self.command("seed",items=ingredient)
        self.capture("valid-upgrade")
        self.take_output(3,"minecraft:diamond_pickaxe",'{Enchantments:[{id:"minecraft:unbreaking",lvl:4s}]}')

    def combustion(self):
        self.case="combustion"
        self.fixture("galosphere:combustion_table")
        self.command("seed",items=items((0,"galosphere:palladium_bomb",4),(1,"minecraft:slime_ball",4),
                                      (2,"minecraft:string",4),(3,"minecraft:gunpowder",4)))
        self.capture("configured-bomb")
        self.take_output(4,"galosphere:palladium_bomb",'{Bouncy:1,Duration:1,Explosion:1}')

    def sawmill(self):
        self.case="sawmill"
        self.fixture("sawmill:sawmill")
        self.command("seed",items=items((0,"minecraft:oak_log",32)))
        result=self.capture("recipes")
        b=result["client"]["window"]
        body_x=int(re.search(r'source=DesktopWindowBounds\[x=(-?\d+)',result["client"]["allWindows"][0]["templateLayout"]).group(1))
        controls=result["client"]["allWindows"][0]["nativeControls"]
        search=next((control for control in controls if control["visible"] and "EditBox" in control["type"]),None)
        source_y=33 if search else 20
        self.command("xy-click",x=b["x"]+8+60-body_x,y=b["y"]+24+source_y-13)
        result=self.capture("recipe-selected")
        self.expect(result["server"]["slots"][1]["count"]>0,"native recipe selector sends the correct menu button")
        item=result["server"]["slots"][1]["item"]
        self.take_output(1,item)

    def alloy(self):
        self.case="alloy-forge"
        self.fixture("alloy_forgery:bricks_forge_controller")
        self.command("seed",items=items((0,"minecraft:coal",4),(2,"minecraft:raw_iron",8)))
        self.capture("smelting-early")
        self.command("wait",ticks=55)
        self.capture("smelting-active")
        self.command("wait",ticks=240)
        self.capture("smelted")
        self.take_output(1,"minecraft:iron_ingot")


if __name__=="__main__":
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument("--loader",required=True,choices=["fabric","forge"])
    p.add_argument("--run",required=True)
    p.add_argument("--case",required=True,choices=["cooking","copper","identifying","imbuing","combustion","sawmill","alloy"])
    a=p.parse_args()
    review=Processing(a.loader,a.run,"processing-"+a.case)
    if a.case=="copper":review.cooking(True)
    else:getattr(review,a.case)()
    print(f"{a.loader}/{a.case}: {len(review.checks)} focused checks passed; {len(review.images)} screenshots.")
