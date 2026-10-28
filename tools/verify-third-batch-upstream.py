"""Check the exact optional-menu ABI used by the later third-batch adapters.

Pass the directory of downloaded, unmodified upstream JARs named <version_id>.jar.
This is a focused binary contract check, not runtime or gameplay validation.
"""
from pathlib import Path
import argparse
import hashlib
import json
import struct
import zipfile
from controlify_classfile import read_class

ROOT = Path(__file__).resolve().parents[1]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("directory", type=Path)
    args = parser.parse_args()
    manifest = json.loads((ROOT / "compatibility/third-batch-all-targets.json").read_text(encoding="utf-8"))
    count = 0
    for target in manifest["targets"]:
        mc, loader = target["minecraft"], target["loader"]
        if mc == "1.20.1":
            continue
        for row in target["integrations"]:
            path = args.directory / (row["version_id"] + ".jar")
            assert hashlib.sha512(path.read_bytes()).hexdigest() == row["sha512"], path
            with zipfile.ZipFile(path) as jar:
                def clazz(name):
                    return read_class(jar.read(name.replace(".", "/") + ".class"))

                def methods(name, *names):
                    found = {n for n, _ in clazz(name).methods}
                    assert set(names) <= found, (mc, loader, name, set(names) - found)

                def constructor(name, tail):
                    assert ("<init>", tail) in clazz(name).methods, (mc, loader, name, tail)

                inv = "Lnet/minecraft/class_1661;" if loader == "fabric" and mc == "1.21.1" else "Lnet/minecraft/world/entity/player/Inventory;"
                mod = row["mod_id"]
                if loader == "fabric":
                    # Some upstream descriptions contain literal newlines; Loader accepts them.
                    metadata = json.loads(jar.read("fabric.mod.json"), strict=False)
                    assert metadata["id"] == mod, (mc, row["project"], metadata["id"], mod)
                textures = {
                    "runes": ["textures/gui/crafting_altar.png"],
                    "tiered": ["textures/gui/reforging_screen.png"],
                    "smitherz": ["textures/gui/smither_screen.png"],
                    "chipped": ["textures/gui/container/workbench.png", "textures/gui/sprites/button.png",
                                "textures/gui/sprites/slot.png", "textures/gui/sprites/single_block_button.png",
                                "textures/gui/sprites/horizontal_blocks_button.png", "textures/gui/sprites/vertical_blocks_button.png",
                                "textures/gui/sprites/two_by_two_button.png"],
                    "simplyswords": ["textures/gui/container/runic_forge.png"],
                }
                for texture in textures.get(mod, []):
                    assert "assets/" + mod + "/" + texture in jar.namelist(), (mc, loader, mod, texture)
                if mod == "chipped":
                    name = "earth.terrarium.chipped.common.menus.WorkbenchMenu"
                    constructor(name, "(I" + inv + ")V")
                    methods(name, "selectStack", "reset", "updateResults", "results", "chosenStack", "setChosenStack", "craft", "level")
                    constructor("earth.terrarium.chipped.client.screens.RenderWindowWidget",
                                "(IIIILjava/util/function/Supplier;Ljava/util/function/Supplier;)V")
                    # Chipped 4 moved hover states into separate PNGs. Treating these
                    # as Chipped 3's double-height sheets stretches/clips every button.
                    for sprite, width in [("button", 72), ("single_block_button", 18),
                                          ("horizontal_blocks_button", 18), ("vertical_blocks_button", 18),
                                          ("two_by_two_button", 18)]:
                        for state in ("", "_highlighted"):
                            png = jar.read(f"assets/chipped/textures/gui/sprites/{sprite}{state}.png")
                            assert png[:8] == b"\x89PNG\r\n\x1a\n"
                            assert struct.unpack(">II", png[16:24]) == (width, 18), (mc, loader, sprite, state)
                    methods("earth.terrarium.chipped.common.blocks.WorkbenchBlock", "method_55766" if loader == "fabric" else "useWithoutItem")
                elif mod == "simplyswords":
                    pos = "Lnet/minecraft/class_2338;" if loader == "fabric" else "Lnet/minecraft/core/BlockPos;"
                    constructor("net.sweenus.simplyswords.screen.RunicForgeScreenHandler", "(I" + inv + pos + ")V")
                    methods("net.sweenus.simplyswords.screen.RunicForgeScreenHandler", "getPreviewStack")
                    methods("net.sweenus.simplyswords.block.RunicForgeBlock", "method_55766" if loader == "fabric" else "useWithoutItem")
                elif mod == "extendeddrawersaddon":
                    constructor("net.extendeddrawersaddon.screen.DrawerScreenHandler", "(I" + inv + "I)V")
                elif mod == "smitherz":
                    constructor("net.smitherz.screen.SmitherScreenHandler", "(I" + inv + "Lnet/minecraft/class_3914;I)V")
                    methods("net.smitherz.screen.SmitherScreenHandler", "getUnlickedGem", "canLinkSameGem", "smith", "updateGemSlots", "getPos", "setPos")
                    methods("net.smitherz.screen.widget.GemSlot", "setEnabled", "setLinked", "isLinked")
                elif mod == "tiered":
                    methods("draylar.tiered.reforge.ReforgeScreenHandler", "isReforgeReady", "reforge", "getPos")
                elif mod == "graveyard" and mc != "1.21.1":
                    methods("com.lion.graveyard.gui.OssuaryScreenHandler", "getAvailableResults", "setAvailableResults")
            count += 1
    assert count == 31
    print(f"Third-batch upstream ABI: {count} SHA-512 verified release JARs; changed constructors, actions and mixin entrypoints verified")


if __name__ == "__main__":
    main()
