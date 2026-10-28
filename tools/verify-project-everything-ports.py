"""Check exact release coverage, optional bootstrap wiring and packaged second-batch ports."""
from pathlib import Path
import argparse
import json
import zipfile

ROOT = Path(__file__).resolve().parents[1]
RECIPE = "data/alloyed/recipe/mechanical_crafting/steel_knife.json"


def recipe_conditions(data):
    fabric = data["fabric:load_conditions"]
    assert len(fabric) == 1 and fabric[0]["condition"] == "fabric:all_mods_loaded"
    assert set(fabric[0]["values"]) == {"alloyed", "farmersdelight", "create"}
    assert {c["modid"] for c in data["neoforge:conditions"]} == {"alloyed", "farmersdelight", "create"}
    assert data["type"] == "create:mechanical_crafting"
    assert data["result"]["id"] == "alloyed:steel_knife"


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--jars", action="store_true")
    args = parser.parse_args()
    manifest = json.loads((ROOT / "compatibility/native-windows-all-targets.json").read_text(encoding="utf-8"))
    targets = manifest["targets"]
    assert len(targets) == 10
    assert len({(t["minecraft"], t["loader"]) for t in targets}) == 10
    assert sum(len(t["integrations"]) for t in targets) == 50
    for target in targets:
        mc, loader = target["minecraft"], target["loader"]
        base = ROOT / f"compatibility/versions/{mc}"
        source = base / "fabric/src/main/java"
        bootstrap = (source / "com/salts_inventory_update/compat/CompatibilityBootstrap.java").read_text(encoding="utf-8")
        artifact = None
        if args.jars:
            label = {"fabric": "Fabric", "neoforge": "NeoForge"}[loader]
            matches = list((ROOT / "dist/compatibility").glob(f"salts_inventory_compat-{mc}-{label}-*.jar"))
            assert len(matches) == 1, (mc, loader, matches)
            artifact = zipfile.ZipFile(matches[0])
        try:
            for integration in target["integrations"]:
                assert len(integration["sha512"]) == 128
                cls = integration["integration_class"]
                assert f'initialize("{integration["mod_id"]}", "{cls}",' in bootstrap, (mc, loader, cls)
                assert (source / (cls.replace(".", "/") + ".java")).is_file(), cls
                if artifact:
                    assert cls.replace(".", "/") + ".class" in artifact.namelist(), (mc, loader, cls)
            if mc in {"26.1.2", "26.2", "26.3"} and (loader == "fabric" or mc == "26.1.2"):
                data = json.loads((base / loader / "src/main/resources" / RECIPE).read_text(encoding="utf-8"))
                recipe_conditions(data)
                if artifact:
                    assert json.loads(artifact.read(RECIPE)) == data
                    assert "pack.mcmeta" in artifact.namelist()
        finally:
            if artifact:
                artifact.close()
    print("Second-batch coverage verified: 50 exact upstream combinations, ten targets" + (", packaged integrations and conditional recipes" if args.jars else ""))


if __name__ == "__main__":
    main()
