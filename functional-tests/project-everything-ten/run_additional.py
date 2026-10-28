"""Focused animation, native-editor and cross-window checks for the ten-mod batch."""
import argparse
import re
from run_cases import Review
from run_processing import items


class Additional(Review):
    def seasons(self):
        for case,block,fuel in [("heater","seasonsextras:heater","minecraft:coal"),
                                ("chiller","seasonsextras:chiller","minecraft:ice")]:
            self.case=case
            self.fixture(block)
            self.command("seed",items=items((0,fuel,16)))
            self.capture("fuel-early")
            self.command("wait",ticks=45)
            result=self.capture("fuel-active")
            self.expect(bool(re.search(r'burnTime:[1-9]\d*',result["server"]["sourceNbt"])),"native fuel burn and progress run")
            b=result["client"]["window"]
            for button in [1,2]:self.command("xy-click",x=b["x"]+8+80+18*button,y=b["y"]+24+46)
            self.command("wait",ticks=40)
            self.capture("three-modules")
        self.case="seasons-two-windows"
        self.fixture("seasonsextras:heater")
        self.command("seed",items=items((0,"minecraft:coal",16)))
        r=self.command("inspect");b=r["client"]["window"]
        self.command("xy-click",x=b["x"]+88,y=b["y"]+70) # disable first heater's first module
        self.command("secondary-block",block="seasonsextras:heater")
        self.command("seed",items=items((0,"minecraft:coal",16)))
        self.command("move",dx=190,dy=40)
        r=self.capture("independent-before");b=r["client"]["window"]
        self.expect(len(r["client"]["allWindows"])==2,"two native instances stay open")
        r=self.command("xy-click",x=b["x"]+88,y=b["y"]+70)
        self.expect("module_0:{burnTime:0,burnTimeTotal:0,enabled:0b}" in r["server"]["sourceNbt"] or
                    bool(re.search(r'module_0:\{[^}]*enabled:0b',r["server"]["sourceNbt"])),
                    "second window control does not toggle the first source")
        self.command("window-click",window=0,slot=0)
        r=self.command("window-click",window=1,slot=1)
        self.expect(r["server"]["cursor"]["count"]==0 and r["server"]["slots"][1]["count"]>0,
                    "fuel transfers between independent native windows")
        self.capture("fuel-transferred")

    def bartering(self):
        self.case="bartering"
        self.fixture("barteringstation:bartering_station")
        x,y,z=self.origin
        self.command("command",command="gamerule doMobSpawning false")
        self.command("command",command="difficulty normal")
        self.command("command",command="item replace entity @a armor.head with minecraft:golden_helmet")
        self.command("command",command=f'summon minecraft:piglin {x+3} {y} {z+1} {{IsImmuneToZombification:1b,PersistenceRequired:1b}}')
        self.command("seed",items=items((0,"minecraft:gold_ingot",16)))
        self.capture("bartering-start")
        self.command("wait",ticks=100)
        self.capture("bartering-active")
        self.command("wait",ticks=400)
        result=self.capture("bartered-loot")
        self.expect(any(slot["count"]>0 for slot in result["server"]["slots"][6:21]),"piglin bartering delivers native output loot")
        self.command("command",command="difficulty peaceful")

    def bounds(self):
        self.case="alloy-forge"
        self.fixture("alloy_forgery:bricks_forge_controller")
        for name,dx,dy in [("left",-5000,0),("right",5000,0),("bottom",0,5000),("top",0,-5000)]:
            result=self.command("move",dx=dx,dy=dy);b=result["client"]["window"]
            self.expect(b["x"]>=0 and b["x"]+b["width"]<=result["client"]["guiWidth"] and b["y"]>=16
                        and b["y"]+b["height"]<=result["client"]["guiHeight"],"reservoir and body fit the "+name+" wall")
        self.command("block-nbt",nbt='{FuelFluidInput:{Amount:81000L,Variant:{fluid:"minecraft:lava"}}}')
        self.command("wait",ticks=10)
        self.capture("lava-reservoir")
        self.command("gui-scale",scale=3)
        self.capture("scale-three")
        self.command("gui-scale",scale=2)

    def editors(self):
        self.case="farmers-cabinet"
        self.fixture("farmersdelight:oak_cabinet")
        self.command("seed",items=items((0,"minecraft:apple",16),(26,"minecraft:carrot",8)))
        self.capture("native-storage")
        self.command("click",slot=26)
        r=self.command("click",slot=1)
        self.expect(r["server"]["slots"][1]["count"]==8,"cabinet uses normal storage slot behavior")
        for key,block in [("canvas-sign","farmersdelight:canvas_sign"),("hanging-canvas-sign","farmersdelight:hanging_canvas_sign")]:
            self.case=key
            self.fixture("minecraft:stone")
            x,y,z=self.origin
            self.command("command",command=f"setblock {x} {y+1} {z} minecraft:stone")
            self.command("command",command=f"setblock {x} {y} {z} {block}")
            r=self.command("open")
            r=self.capture("native-editor")
            self.expect("SignEditScreen" in r["client"]["screen"] and r["client"]["sessions"]==0,
                        "standalone sign editor retains its native screen")
            self.command("key",key=256)

    def alloyed(self):
        self.case="alloyed"
        self.fixture("minecraft:chest")
        self.command("seed",items=items((0,"alloyed:bronze_ingot",16),(1,"alloyed:steel_ingot",16)))
        self.capture("items-in-storage")
        self.command("click",slot=0)
        self.command("store-in-player")
        r=self.capture("items-transferred")
        self.expect(r["server"]["cursor"]["count"]==0,"Alloyed items transfer through normal inventory windows")
        self.expect(any(s["item"]=="alloyed:bronze_ingot" and s["count"]==16 for s in r["server"]["slots"]),
                    "Alloyed bronze is conserved in the player inventory")


if __name__=="__main__":
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument("--loader",required=True,choices=["fabric","forge"])
    p.add_argument("--run",required=True)
    p.add_argument("--case",required=True,choices=["seasons","bartering","bounds","editors","alloyed"])
    a=p.parse_args()
    review=Additional(a.loader,a.run,"additional-"+a.case)
    getattr(review,a.case)()
    print(f"{a.loader}/{a.case}: {len(review.checks)} focused checks passed; {len(review.images)} screenshots.")
