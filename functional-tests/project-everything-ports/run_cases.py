"""Focused real-client native-window checks; run against one isolated port fixture."""
from pathlib import Path
import argparse
import importlib.util
import json

spec = importlib.util.spec_from_file_location("qa_send", Path(__file__).parents[1] / "project-everything-ten/send.py")
sender = importlib.util.module_from_spec(spec)
spec.loader.exec_module(sender)

CASES = [
    ("bartering", "barteringstation:bartering_station", "minecraft:gold_ingot", (11, 17)),
    ("alloy-forge", "alloy_forgery:bricks_forge_controller", "minecraft:raw_iron", (44, 43)),
    ("alloy-forge", "alloy-forgery:bricks_forge_controller", "minecraft:raw_iron", (44, 43)),
    ("heater", "seasonsextras:heater", "minecraft:coal", (26, 17)),
    ("chiller", "seasonsextras:chiller", "minecraft:ice", (26, 17)),
    ("alloyed", "alloyed:forge", "minecraft:iron_ingot", (30, 17)),
    ("alloyed", "alloyed:oven", "minecraft:iron_ingot", (30, 17)),
    ("cooking-pot", "farmersdelight:cooking_pot", "farmersdelight:rice", (30, 17)),
    ("copper-pot", "miners_delight:copper_pot", "farmersdelight:rice", (40, 18)),
    ("copper-pot", "minersdelight:copper_pot", "farmersdelight:rice", (40, 18)),
    ("identifying", "betterarcheology:archeology_table", "betterarcheology:unidentified_artifact", (26, 48)),
    ("fossil", "betterarcheology:villager_fossil", "minecraft:torch", (80, 22)),
    ("imbuing", "illagerinvasion:imbuing_table", "illagerinvasion:hallowed_gem", (134, 54)),
    ("combustion", "galosphere:combustion_table", "galosphere:palladium_bomb", (44, 29)),
    ("sawmill", "sawmill:sawmill", "minecraft:oak_log", None),
]


def check(output, only=()):
    checks = []
    if only and (output / "focused-checks.json").exists():
        checks = json.loads((output / "focused-checks.json").read_text(encoding="utf-8"))
    def cmd(kind, **args):
        return sender.send(output, kind, **args)
    def expect(test, case, label):
        checks.append({"case": case, "check": label, "passed": bool(test)})
        (output / "focused-checks.json").write_text(json.dumps(checks, indent=2), encoding="utf-8")
        if not test:
            raise AssertionError(f"{case}: {label}")
    cmd("catalog")
    catalog = sender.read(output / "catalog.json")
    mc, loader = output.name.split("-")[:2]
    fixture = sender.read(output.parents[1] / f"fixture-{mc}-{loader}.json")
    expected = {
        "bartering-station": "barteringstation:bartering_station",
        "alloy-forgery": ("alloy_forgery:" if mc == "1.21.1" else "alloy-forgery:") + "bricks_forge_controller",
        "fabric-seasons-extras": "seasonsextras:heater",
        "create-alloyed": ("alloyed:forge", "alloyed:oven"),
        "farmers-delight": "farmersdelight:cooking_pot",
        "farmers-delight-refabricated": "farmersdelight:cooking_pot",
        "miners-delight": "minersdelight:copper_pot",
        "better-archeology": "betterarcheology:archeology_table",
        "illager-invasion": "illagerinvasion:imbuing_table",
        "galosphere": "galosphere:combustion_table",
        "universal-sawmill": "sawmill:sawmill",
    }
    for family in fixture.get("families", {}):
        choices=expected[family] if isinstance(expected[family],tuple) else (expected[family],)
        expect(any(block in catalog["blocks"] for block in choices), family, "installed family appears in fixture catalog")
    origin = tuple(map(int, catalog["origin"].split(", ")))
    for name, block, item, xy in CASES:
        if block not in catalog["blocks"] or only and name not in only:
            continue
        print("CHECK", name, block, flush=True)
        cmd("fixture", block=block, campfire="pot" in name)
        if "forge_controller" in block:
            x, y, z = origin
            cmd("server-command", command=f"fill {x-1} {y-1} {z} {x+1} {y+1} {z+2} minecraft:bricks")
            cmd("server-command", command=f"fill {x} {y} {z+1} {x} {y+1} {z+1} minecraft:air")
            cmd("server-command", command=f"setblock {x} {y} {z} {block}[facing=north]")
        result = cmd("open", realUse=True, ticks=30, capture=name + "-open")
        windows = result["client"]["windows"]
        expect(len(windows) == 1, name, "one detached window")
        window = windows[0]
        expect(window["definition"].endswith("NativeTemplateWindow"), name, "public native template selected")
        expect(not window.get("layoutError"), name, "no layout error")
        slots = [s for s in result["server"]["slots"] if not s["player"]]
        slot = next((s["index"] for s in slots if xy is None or (s["x"], s["y"]) == xy), None)
        expect(slot is not None, name, "expected native input present")
        if name in ("heater", "chiller"):
            # Disable burning before the stable pickup/return check.
            b = window["bounds"]
            cmd("xy-click", x=b["x"]+8+80, y=b["y"]+24+46)
        cmd("seed", slot=slot, item=item, count=1)
        result = cmd("click", slot=slot)
        expect(result["server"]["cursor"]["count"] == 1, name, "authoritative pickup")
        expect(result["client"]["cursor"]["count"] == 1, name, "client cursor agrees")
        result = cmd("click", slot=slot, capture=name + "-returned")
        expect(result["server"]["cursor"]["count"] == 0, name, "authoritative placement")
        expect(result["server"]["slots"][slot]["count"] == 1, name, "native input retained")
        if name == "identifying":
            expect("BetterArcheologyDecoration" in window["decoration"], name, "parchment decoration selected")
        if name in ("cooking-pot", "copper-pot"):
            toggle = next((r for r in window["template"]["regions"] if r["id"] == "recipe-toggle"), None)
            expect(toggle is not None and not toggle["exterior"], name, "recipe toggle inside frame")
            b = window["bounds"]
            args = {"x": b["x"] + 8 + toggle["x"] + 10, "y": b["y"] + 24 + toggle["y"] + 9}
            result = cmd("xy-click", **args, capture=name + "-book")
            expect(result["client"]["windows"][0]["recipeVisible"], name, "native button opens recipe book")
            result = cmd("xy-click", **args)
            expect(not result["client"]["windows"][0]["recipeVisible"], name, "second click closes book")
        result = cmd("move", dx=60, dy=35, capture=name + "-moved")
        expect(result["client"]["windows"][0]["bounds"] != window["bounds"], name, "window moves")
        cmd("close")
        result = cmd("open", realUse=True, capture=name + "-reopened")
        expect(len(result["client"]["windows"]) == 1, name, "reopens as window")
        if name in ("cooking-pot", "copper-pot"):
            w = result["client"]["windows"][0]
            toggle = next(r for r in w["template"]["regions"] if r["id"] == "recipe-toggle")
            args = {"x": w["bounds"]["x"]+8+toggle["x"]+10,
                    "y": w["bounds"]["y"]+24+toggle["y"]+9}
            if not w["recipeVisible"]:
                cmd("xy-click", **args)
            cmd("close")
            result = cmd("open", realUse=True)
            w = result["client"]["windows"][0]
            args["x"] = w["bounds"]["x"]+8+toggle["x"]+10
            args["y"] = w["bounds"]["y"]+24+toggle["y"]+9
            result = cmd("xy-click", **args, capture=name+"-book-after-reopen")
            expect(result["client"]["windows"][0]["recipeVisible"] != w["recipeVisible"], name,
                   "one click toggles after closing with book open")
        if name == "alloy-forge" and "disabledNativeSlots" in result["server"]:
            b = result["client"]["windows"][0]["bounds"]
            cmd("xy-click", x=b["x"]+8+148, y=b["y"]+24+63)
            result = cmd("click", slot=3, capture="alloy-forge-slot-disabled")
            expect(3 in result["server"]["disabledNativeSlots"], name, "native slot-lock action reaches server")
            if "nativeValues" in result["client"]["windows"][0]:
                expect(result["client"]["windows"][0]["nativeValues"]["disabled"] == [3], name, "native client lock appearance follows server state")
            result = cmd("click", slot=3, capture="alloy-forge-slot-enabled")
            expect(3 not in result["server"]["disabledNativeSlots"], name, "native slot unlock reaches server")
            if "nativeValues" in result["client"]["windows"][0]:
                cmd("click", slot=3)
                cmd("close")
                result=cmd("open", realUse=True, capture="alloy-forge-reopened-locked")
                expect(result["client"]["windows"][0]["nativeValues"]["disabled"] == [3],
                       name, "initial property snapshot survives reopening")
                b=result["client"]["windows"][0]["bounds"]
                cmd("xy-click", x=b["x"]+8+148, y=b["y"]+24+63)
                cmd("click", slot=3)
                cmd("seed", slot=0, item="minecraft:coal", count=1)
                result=cmd("wait", ticks=35, capture="alloy-forge-processing")
                expect(result["client"]["windows"][0]["nativeValues"]["getFuelProgress"] > 0,
                       name, "native fuel indicator receives live property updates")
        if name == "alloyed":
            # Exercise the newly standalone forge's native recipe and output slot.
            for index, ingredient in enumerate(("minecraft:copper_ingot", "minecraft:iron_nugget",
                                                 "minecraft:iron_nugget", "minecraft:iron_nugget", "minecraft:fire_charge")):
                cmd("seed", slot=index, item=ingredient, count=1)
            cmd("seed", slot=9, item="minecraft:coal", count=1)
            cmd("wait", ticks=40, capture="alloyed-processing")
            for attempt in range(40):
                result = cmd("wait", ticks=20)
                if result["server"]["slots"][10]["count"]:
                    break
            expect(result["server"]["slots"][10]["item"] == "alloyed:bronze_ingot", name, "native forging recipe completes")
            result = cmd("click", slot=10, capture="alloyed-result-picked")
            expect(result["server"]["cursor"]["item"] == "alloyed:bronze_ingot", name, "native output pickup retained")
            result = cmd("store", capture="alloyed-result-stored")
            expect(result["server"]["cursor"]["count"] == 0, name, "forge result transfers into player inventory")
        if name == "bartering" and not output.name.startswith("1.21.1-"):
            x, y, z = origin
            cmd("server-command", command=f'summon minecraft:piglin {x+2} {y} {z+2} {{IsImmuneToZombification:1b,NoAI:1b,PersistenceRequired:1b}}', ticks=45)
            result = cmd("capture", capture="bartering-head-active")
            b = result["client"]["windows"][0]["bounds"]
            result = cmd("xy-click", x=b["x"]+8+43+8, y=b["y"]+24+4+8, capture="bartering-head-closed")
            expect(result["client"]["sessions"] == 0, name, "head closes its detached window")
            result = cmd("server-command", command='execute if entity @e[type=minecraft:piglin,nbt={active_effects:[{id:"minecraft:glowing"}]}]')
            expect(result["server"]["commandResult"] > 0, name, "native highlight action reached server")
        cmd("close")
    return checks


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("output", type=Path)
    parser.add_argument("--only", nargs="*", default=[])
    args = parser.parse_args()
    results = check(args.output, args.only)
    print(f"Passed {sum(bool(r['passed']) for r in results)} focused assertions; {sum(not r['passed'] for r in results)} failures recorded")
