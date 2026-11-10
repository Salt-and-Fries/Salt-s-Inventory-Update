"""Focused original-release forge UI checks through ordinary desktop input.

The excluded observer only reads menu/component state. Operator commands create
the test materials and progression; upgrade/craft actions use native buttons.
"""
from pathlib import Path
import argparse
import json
import re
import sys
import time

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / "functional-tests/project-everything-ten"))
from send import send


class Check:
    def __init__(self, run, phase):
        self.run = run
        self.out = run / "focused-evidence" / phase
        self.out.mkdir(parents=True, exist_ok=True)
        self.results = []
        self.sequence = 0

    def act(self, kind="capture", **kw):
        r = send(self.run, kind, **kw)
        self.sequence += 1
        (self.out / f"{self.sequence:03}-{kind}.json").write_text(json.dumps(r, indent=2))
        assert not r["client"].get("observerError"), r["client"].get("observerError")
        return r["client"]

    def click(self, x, y, ticks=5):
        self.act("pointer", x=x, y=y, ticks=3)
        self.act("click", x=x, y=y, button=0, ticks=1)
        return self.act("release", x=x, y=y, button=0, ticks=ticks)

    def command(self, command, ticks=5):
        return self.act("command", command=command, ticks=ticks)

    def check(self, name, success, detail=None):
        self.results.append(dict(name=name, passed=bool(success), detail=detail))
        (self.out / "results.json").write_text(json.dumps(self.results, indent=2))
        assert success, (name, detail)
        print("PASS:", name, flush=True)

    def forge(self, state):
        return next(w for w in state["allWindows"] if "forgeContent" in w)

    def read_server(self):
        self.act("prominent-state", ticks=3)
        state = json.loads((self.run / "prominent-state.json").read_text())
        (self.out / f"{self.sequence:03}-components.json").write_text(json.dumps(state, indent=2))
        return state

    def setup(self, stage):
        self.act("fixture", block="prominent:primordial_forge", ticks=8)
        state=self.read_server()
        timer=re.search(r"endTime:(\d+)L",state["primordialforge.PrimordialForgeProgressComponent"])
        # The native stage command does not cancel a running upgrade. Let the
        # real timer elapse before preparing the next menu; never bypass it.
        while timer and int(timer[1])/1000 > time.time():
            self.act(ticks=160)
        for command in ["gamerule sendCommandFeedback false", "gamerule announceAdvancements false", "gamemode survival @p",
                        "prominent:set_primordial_forge_unlocked @p true",
                        f"prominent:set_primordial_forge_stage @p {stage}",
                        "prominent:cinders @p set 0", "tp @p 8.5 65 12.5 90 0"]:
            self.command(command)
        return self.act("open", ticks=20)

    def move(self, state, kind, x, y):
        w = next(w for w in state["workspaceWindows"] if w["kind"] == kind)
        if w["locked"] == "true":
            self.click(**w["controls"]["LOCK"])
        b = w["bounds"]
        if kind=="INVENTORY":
            # A newly opened inventory may sit behind the native window. Its
            # exposed empty left rim brings it forward without touching items.
            self.click(b["x"]+3,b["y"]+b["height"]-6)
        sx, sy = b["x"] + 55, b["y"] + 8
        tx, ty = x + 55, y + 8
        self.act("click", x=sx, y=sy, button=0, ticks=1)
        self.act("drag", x=tx, y=ty, button=0, dx=tx-sx, dy=ty-sy, ticks=2)
        return self.act("release", x=tx, y=ty, button=0, ticks=4)

    def upgrade(self):
        s = self.setup(0)
        w = self.forge(s)
        self.check("Upgrade opens with the dedicated original-surface template",
                   w["menuId"] == "prominent:primordial_forge_upgrade" and "ProminentWindows" in w["template"])
        s = self.move(s, "CONTAINER", 230, 65)
        self.check("Original forge header drags its entire native body",
                   self.forge(s)["bounds"]["x"] == 230 and self.forge(s)["bounds"]["y"] == 65)
        s = self.act("inventory", ticks=8)
        self.move(s, "INVENTORY", 230, 240)
        w = self.forge(self.act())
        cx, cy = w["forgeContent"]["x"], w["forgeContent"]["y"]
        self.click(cx+89, cy+92)
        state = self.read_server()
        self.check("Missing upgrade materials cannot start progression",
                   "endTime:0L" in state["primordialforge.PrimordialForgeProgressComponent"])
        materials = {"minecraft:iron_bars":16, "betterend:aeternium_forged_plate":4,
                     **{"archon:"+e+"_essence":7 for e in ["fire","water","earth","sky","end"]},
                     "archon:glisteel_block":4, "chipped:massive_deepslate_bricks":32}
        for i,(item,count) in enumerate(materials.items()):
            self.command(f"item replace entity @p inventory.{i} with {item} {count}", ticks=3)
        self.act("pointer",x=25,y=25,ticks=110,capture="forge-upgrade-with-inventory")
        self.click(cx+89, cy+92, ticks=12)
        state = self.read_server()
        progress = state["primordialforge.PrimordialForgeProgressComponent"]
        self.check("Native upgrade button starts the original 60-second timer",
                   "totalTime:60000L" in progress and "endTime:0L" not in progress, progress)
        self.check("Upgrade consumes its actual original ingredients",
                   all('id:"minecraft:air"' in cell["item"] for cell in state["inventory"]), state["inventory"])
        self.check("Native upgrade close ends its owning SIU session",
                   not self.act()["allWindows"])

    def armor(self):
        s = self.setup(2)
        w = self.forge(s)
        self.check("Armor selection opens with the dedicated native template",
                   w["menuId"] == "prominent:primordial_forge" and "ProminentWindows" in w["template"])
        s = self.move(s,"CONTAINER",196,60)
        self.check("Added armor header drags the nameplate, carousel and Forge button",
                   self.forge(s)["bounds"]["y"] == 60)
        w = self.forge(s);cx,cy=w["forgeContent"]["x"],w["forgeContent"]["y"]
        for y,slot in [(39,"HEAD"),(68,"CHEST"),(91,"LEGS"),(114,"FEET")]:
            s=self.click(cx+124,cy+y)
            self.check(f"Original {slot.lower()} armor selection works", self.forge(s)["forgeValues"]["selectedSlot"]==slot)
        self.click(cx+124,cy+145)
        state=self.read_server()
        self.check("Missing armor requirements do not spend Cinders or create items",
                   state["cinders.CindersComponent"]=="{cinders:0}" and all('id:"minecraft:air"' in c["item"] for c in state["inventory"]))
        s=self.click(cx+82,cy+58,ticks=25)
        self.check("Clicking a side set rotates the native carousel",abs(float(self.forge(s)["forgeValues"]["targetOffset"])+1)<.001)
        s=self.act("scroll",x=cx+124,y=cy+70,amount=1,ticks=25)
        self.check("Mouse wheel changes the native armor set",abs(float(self.forge(s)["forgeValues"]["targetOffset"])+2)<.001)
        s=self.act("scroll",x=cx+124,y=cy+70,amount=-2,ticks=30)
        self.check("Carousel returns to Lightbringer",self.forge(s)["forgeValues"]["armorName"]=="paladin")
        self.click(cx+124,cy+39)
        self.command("prominent:cinders @p set 1234")
        for i,item in enumerate(["mythicmounts:firebird_feather","paladins:netherite_crusader_armor_head",
                     "betternether:cincinnasite_block","mcda:fabric_bolt_yellow","mcda:upgrade_core_golden"]):
            self.command(f"item replace entity @p inventory.{i} with {item} 1",ticks=3)
        # The original screen recalculates its recipe when the piece changes.
        self.click(cx+124,cy+68)
        self.click(cx+124,cy+39)
        s=self.act(ticks=20)
        self.check("Original armor recipe becomes craftable with real materials and Cinders",
                   "canCraft=true" in self.forge(s)["forgeValues"]["craftingConditions"],self.forge(s)["forgeValues"])
        s=self.act("inventory",ticks=5)
        s=self.move(s,"INVENTORY",230,265)
        self.act("pointer",x=25,y=25,ticks=110,capture="forge-armor-with-inventory")
        self.click(cx+124,cy+145,ticks=20)
        state=self.read_server()
        self.check("Native Forge action spends the original 150 Cinders",
                   state["cinders.CindersComponent"]=="{cinders:1084}",state["cinders.CindersComponent"])
        self.check("Native Forge action consumes all five original materials",
                   all('id:"minecraft:air"' in c["item"] for c in state["inventory"]),state["inventory"])
        self.act(ticks=110)
        state=self.read_server()
        self.check("Original delayed forging animation delivers the selected armor",
                   sum('id:"prominent:armor/paladin_helmet"' in c["item"] for c in state["inventory"])==1,state["inventory"])

    def visuals(self):
        s=self.setup(2)
        self.command("prominent:cinders @p set 1234")
        s=self.act("inventory",ticks=5)
        s=self.move(s,"CONTAINER",196,60)
        s=self.move(s,"INVENTORY",230,265)
        self.act("pointer",x=25,y=25,ticks=100,capture="armor-selection-final-gui2")
        s=self.act("gui-scale",scale=3,ticks=15)
        s=self.move(s,"CONTAINER",90,5)
        s=self.move(s,"INVENTORY",125,200)
        w=self.forge(s);cx,cy=w["forgeContent"]["x"],w["forgeContent"]["y"]
        s=self.click(cx+124,cy+114)
        self.check("Armor selection maps correctly after GUI scale and position changes",
                   self.forge(s)["forgeValues"]["selectedSlot"]=="FEET")
        self.act("pointer",x=25,y=25,ticks=5,capture="armor-selection-final-gui3")
        s=self.act();w=next(w for w in s["workspaceWindows"] if w["kind"]=="CONTAINER")
        self.click(**w["controls"]["CLOSE"])
        s=self.act(ticks=8)
        self.check("Armor close control leaves separate Inventory open",
                   not s["allWindows"] and any(w["kind"]=="INVENTORY" for w in s["workspaceWindows"]))
        s=self.setup(0)
        s=self.act("inventory",ticks=5)
        s=self.move(s,"CONTAINER",125,25)
        s=self.move(s,"INVENTORY",125,200)
        forge=self.forge(s)["bounds"]
        inv=next(w["bounds"] for w in s["workspaceWindows"] if w["kind"]=="INVENTORY")
        self.check("GUI scale 3 fits the complete forge and separate Inventory without overlap",
                   all(b["x"]>=0 and b["y"]>=0 and b["x"]+b["width"]<=s["guiWidth"] and b["y"]+b["height"]<=s["guiHeight"] for b in [forge,inv])
                   and forge["y"]+forge["height"]<=inv["y"],dict(forge=forge,inventory=inv))
        self.act("pointer",x=25,y=25,ticks=90,capture="forge-upgrade-final-gui3")
        s=self.act("gui-scale",scale=2,ticks=15)
        s=self.move(s,"CONTAINER",230,65)
        s=self.move(s,"INVENTORY",230,240)
        self.act("pointer",x=25,y=25,ticks=8,capture="forge-upgrade-final-gui2")
        s=self.act();w=next(w for w in s["workspaceWindows"] if w["kind"]=="CONTAINER")
        s=self.click(**w["controls"]["LOCK"])
        # Core's existing resetLockedWindows preference may restore its saved
        # position when locking. Test dragging from that resulting position.
        w=next(w for w in s["workspaceWindows"] if w["kind"]=="CONTAINER")
        s=self.move_locked(w,300,80)
        self.check("Forge lock control prevents movement of the native panel",
                   self.forge(s)["bounds"]==w["bounds"])
        w=next(w for w in s["workspaceWindows"] if w["kind"]=="CONTAINER")
        self.click(**w["controls"]["CLOSE"])
        s=self.act(ticks=8)
        self.check("Upgrade close control leaves separate Inventory open",
                   not s["allWindows"] and any(w["kind"]=="INVENTORY" for w in s["workspaceWindows"]))

    def move_locked(self,w,x,y):
        sx,sy=w["bounds"]["x"]+55,w["bounds"]["y"]+8
        self.act("pointer",x=sx,y=sy,ticks=2)
        self.act("click",x=sx,y=sy,button=0,ticks=1)
        self.act("drag",x=x+55,y=y+8,button=0,dx=x+55-sx,dy=y+8-sy,ticks=2)
        return self.act("release",x=x+55,y=y+8,button=0,ticks=3)


if __name__ == "__main__":
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument("run",type=Path)
    parser.add_argument("phase",choices=["upgrade","armor","visuals"])
    args=parser.parse_args()
    check=Check(args.run,args.phase)
    getattr(check,args.phase)()
