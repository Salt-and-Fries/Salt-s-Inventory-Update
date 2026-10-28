"""Verify exact upstream release coverage and optional, API-only third-batch packaging."""
from pathlib import Path
import argparse
import json
import zipfile

ROOT = Path(__file__).resolve().parents[1]
PREFIX = "com/salts_inventory_update/compat/"


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--jars", action="store_true")
    args = parser.parse_args()
    manifest = json.loads((ROOT / "compatibility/third-batch-all-targets.json").read_text(encoding="utf-8"))
    targets = manifest["targets"]
    assert len(targets) == 12
    assert len({(t["minecraft"], t["loader"]) for t in targets}) == 12
    assert sum(len(t["integrations"]) for t in targets if t["minecraft"] != "1.20.1") == 31
    assert sum(len(t["integrations"]) for t in targets) == 45
    for target in targets:
        mc, loader = target["minecraft"], target["loader"]
        base = ROOT / f"compatibility/versions/{mc}"
        source = base / "fabric/src/main/java"
        bootstrap = (source / PREFIX / "CompatibilityBootstrap.java").read_text(encoding="utf-8")
        resource = base / loader / "src/main/resources"
        if loader == "fabric":
            metadata = json.loads((resource / "fabric.mod.json").read_text(encoding="utf-8"))
            configured_mixins = metadata["mixins"]
        else:
            metadata = (resource / "META-INF" / ("mods.toml" if loader == "forge" else "neoforge.mods.toml")).read_text(encoding="utf-8")
            configured_mixins = metadata
        artifact = None
        if args.jars:
            label = {"fabric": "Fabric", "forge": "Forge", "neoforge": "NeoForge"}[loader]
            paths = list((ROOT / "dist/compatibility").glob(f"salts_inventory_compat-{mc}-{label}-*.jar"))
            assert len(paths) == 1, (mc, loader, paths)
            artifact = zipfile.ZipFile(paths[0])
        try:
            for integration in target["integrations"]:
                mod, cls = integration["mod_id"], integration["integration_class"]
                assert len(integration["sha512"]) == 128
                assert f'initialize("{mod}", "{cls}",' in bootstrap, (mc, loader, cls)
                implementation = source / (cls.replace(".", "/") + ".java")
                assert implementation.is_file(), cls
                # IDs come from the audited native registry, not from the Java class name.
                # AdditionZ moved its fletching ID in 1.21.1; a typo otherwise silently
                # falls back to the automatic host while a bounds-only check still passes.
                for menu in integration.get("native_menu_ids", []):
                    text = implementation.read_text(encoding="utf-8")
                    assert f'NativeWindowPolish.installTemplate("{mod}","{menu}",' in text, (mc, loader, menu)
                if artifact:
                    entry = cls.replace(".", "/") + ".class"
                    assert entry in artifact.namelist(), (mc, loader, cls)
                    for menu in integration.get("native_menu_ids", []):
                        assert menu.encode("utf-8") in artifact.read(entry), (mc, loader, menu)
                if mc != "1.20.1":
                    if loader == "fabric":
                        assert mod in metadata["suggests"] and mod not in metadata["depends"], (mc, mod)
                    else:
                        assert f'modId = "{mod}"\ntype = "optional"' in metadata, (mc, mod)
            if mc == "1.21.1":
                for name in ("chipped", "simplyswords"):
                    config = f"salts_inventory_compat.{name}.mixins.json"
                    assert config in configured_mixins
                    data = json.loads((base / "common/src/main/resources" / config).read_text(encoding="utf-8"))
                    if artifact:
                        assert json.loads(artifact.read(config)) == data
                        for mixin in data["mixins"]:
                            assert data["package"].replace(".", "/") + "/" + mixin + ".class" in artifact.namelist()
            if mc not in {"1.20.1", "1.21.1"}:
                assert 'graveyard.OssuarySync", "initializeCommon"' in bootstrap
                if artifact:
                    assert PREFIX + "graveyard/OssuarySync$Template.class" in artifact.namelist()
        finally:
            if artifact:
                artifact.close()
    print("Third-batch coverage: 45 exact release combinations across all 12 targets; 31 later ports" +
          ("; production classes and optional mixins verified" if args.jars else "; optional source wiring verified"))


if __name__ == "__main__":
    main()
