"""Focused real-client checks and screenshots for the second 1.20.1 integration batch."""
from pathlib import Path
import argparse
import json
import re
import sys
from send import send, read

CASES = [
    ("bartering", "barteringstation:bartering_station", 0, "minecraft:gold_ingot", {"fabric", "forge"}),
    ("alloy-forge", "alloy_forgery:bricks_forge_controller", 2, "minecraft:raw_iron", {"fabric"}),
    ("heater", "seasonsextras:heater", 0, "minecraft:coal", {"fabric"}),
    ("chiller", "seasonsextras:chiller", 0, "minecraft:ice", {"fabric"}),
    ("cooking-pot", "farmersdelight:cooking_pot", 0, "farmersdelight:rice", {"fabric", "forge"}),
    ("copper-pot", "miners_delight:copper_pot", 0, "farmersdelight:rice", {"forge"}),
    ("identifying", "betterarcheology:archeology_table", 1, "betterarcheology:unidentified_artifact", {"fabric", "forge"}),
    ("fossil", "betterarcheology:villager_fossil", 0, "minecraft:torch", {"fabric", "forge"}),
    ("imbuing", "illagerinvasion:imbuing_table", 2, "illagerinvasion:hallowed_gem", {"fabric", "forge"}),
    ("combustion", "galosphere:combustion_table", 0, "galosphere:palladium_bomb", {"fabric", "forge"}),
    ("sawmill", "sawmill:sawmill", 0, "minecraft:oak_log", {"fabric", "forge"}),
]


class Review:
    def __init__(self, loader, run, report="containers"):
        self.report = report
        self.loader = loader
        self.output = Path("build/project-everything-ten-1.20.1/gameplay") / f"1.20.1-{loader}-{run}"
        self.checks = []
        self.images = []
        self.case = "setup"
        send(self.output, "catalog")
        self.origin = tuple(map(int, read(self.output / "catalog.json")["origin"].split(", ")))

    def command(self, kind, **args):
        return send(self.output, kind, **args)

    def expect(self, condition, name):
        self.checks.append({"case": self.case, "name": name, "passed": bool(condition)})
        self.save()
        if not condition:
            raise AssertionError(f"{self.case}: {name}")

    def save(self):
        (self.output / f"checks-{self.report}.json").write_text(json.dumps(self.checks, indent=2))
        (self.output / f"images-{self.report}.json").write_text(json.dumps(self.images, indent=2))

    def capture(self, state):
        name = f"{self.loader}-{self.case}-{state}"
        result = self.command("capture", capture=name)
        self.images.append({"case": self.case, "state": state, "file": result["screenshot"],
                            "sequence": result["sequence"], "client": result["client"], "server": result["server"]})
        self.save()
        return result

    def fixture(self, block):
        self.command("fixture", block=block)
        if block.startswith("alloy_forgery:"):
            x, y, z = self.origin
            material = "minecraft:"+block.split(":",1)[1].removesuffix("_forge_controller")
            self.command("command", command=f"fill {x-1} {y-1} {z} {x+1} {y+1} {z+2} {material}")
            self.command("command", command=f"fill {x} {y} {z+1} {x} {y+1} {z+1} minecraft:air")
            self.command("command", command=f"setblock {x} {y} {z} {block}[facing=north]")
        result = self.command("open", ticks=16)
        self.command("dismiss-help")
        return result

    def window(self, result):
        windows = result["client"]["allWindows"]
        self.expect(len(windows) == 1, "one hosted native window")
        window = windows[0]
        self.expect(window.get("definition", "").endswith("NativeTemplateWindow"), "public native template selected")
        self.expect(not window.get("layoutError"), "valid native regions")
        return window["bounds"]

    def containers(self, include):
        for case, block, slot, item, loaders in CASES:
            if self.loader not in loaders or include and case not in include:
                continue
            self.case = case
            if case == "fossil" and self.loader == "forge":
                slot = 36  # Forge appends the real socket after the player inventory.
            if case == "identifying" and self.loader == "forge":
                slot = 37
            result = self.fixture(block)
            if case in {"heater", "chiller"}:
                # Pause fuel use through the actual visible valve before testing
                # pickup/return; production must retain the native fuel filter.
                b = result["client"]["window"]
                self.command("xy-click", x=b["x"]+8+80, y=b["y"]+24+46)
            self.command("seed", items=[{"slot": slot, "item": item, "count": 1}])
            result = self.capture("ready")
            self.window(result)
            self.expect(result["client"]["slots"][slot]["count"] == result["server"]["slots"][slot]["count"] == 1,
                        "initial authoritative item snapshot")
            result = self.command("click", slot=slot)
            self.expect(result["server"]["cursor"]["count"] == 1, "real slot pickup")
            result = self.command("click", slot=slot)
            self.expect(result["server"]["slots"][slot]["count"] == 1 and result["server"]["cursor"]["count"] == 0,
                        "real slot return")
            result = self.command("move", dx=-75, dy=40)
            self.window(result)
            result = self.capture("moved")
            self.command("close")
            result = self.command("open", ticks=12)
            self.window(result)
            if case in {"imbuing", "combustion", "sawmill"}:
                self.expect(sum(s["count"] for s in result["server"]["slots"] if s["item"] == item) == 1,
                            "native temporary workbench returns its input to the player on close")
            else:
                self.expect(result["server"]["slots"][slot]["count"] == 1, "same source keeps items across reopen")
            if case in {"heater", "chiller"}:
                before = result["server"]["sourceNbt"]
                b = result["client"]["window"]
                for control in range(3):
                    result = self.command("xy-click", x=b["x"]+8+80+18*control, y=b["y"]+24+46)
                    native = result["server"]["sourceNbt"]
                    old = re.search(fr'module_{control}:\{{[^}}]*enabled:(\d)b', before).group(1)
                    new = re.search(fr'module_{control}:\{{[^}}]*enabled:(\d)b', native).group(1)
                    self.expect(old != new, f"valve {control} updates the authoritative block")
                    before = native
                result = self.command("xy-click", x=b["x"]+8+125, y=b["y"]+24+3)
                self.expect(re.search(r'particles:(\d)b', before).group(1) !=
                            re.search(r'particles:(\d)b', result["server"]["sourceNbt"]).group(1),
                            "particle control updates the authoritative block")
                self.capture("controls")
            print(f"{self.loader} {case}: focused container checks passed", flush=True)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--loader", choices=["fabric", "forge"], required=True)
    parser.add_argument("--run", required=True)
    parser.add_argument("--include", help="Comma-separated focused UI cases")
    args = parser.parse_args()
    review = Review(args.loader, args.run)
    review.containers(set(args.include.split(",")) if args.include else set())
    print(f"Passed {len(review.checks)} focused checks; captured {len(review.images)} images.")
