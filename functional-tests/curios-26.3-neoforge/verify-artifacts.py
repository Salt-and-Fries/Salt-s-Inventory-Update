"""Focused production checks for the two experimental 26.3 NeoForge artifacts."""
from pathlib import Path
import importlib.util
import tomllib
import zipfile

ROOT = Path(__file__).resolve().parents[2]
spec = importlib.util.spec_from_file_location("boundaries", ROOT / "tools/verify-module-boundaries.py")
boundaries = importlib.util.module_from_spec(spec)
spec.loader.exec_module(boundaries)
PREFIX = "com/salts_inventory_update/"
TARGETS = (
    ("core", "salts_inventory_update", "0.1.5"),
    ("compatibility", "salts_inventory_compat", "0.1.2"),
)

for kind, mod, version in TARGETS:
    artifact = ROOT / "dist" / kind / f"{mod}-26.3-NeoForge-{version}.jar"
    with zipfile.ZipFile(artifact) as jar:
        names = set(jar.namelist())
        classes = [name for name in names if name.endswith(".class")]
        assert classes, f"Empty artifact: {artifact}"
        assert not any(name.startswith(boundaries.NATIVE_INTEGRATION_PREFIXES) for name in classes), "Bundled native integration"
        assert not any(name.startswith(PREFIX + "qa/") for name in names), "Bundled QA classes"
        metadata = tomllib.loads(jar.read("META-INF/neoforge.mods.toml").decode())
        assert metadata["mods"][0]["modId"] == mod
        assert metadata["mods"][0]["version"] == version
        assert "salts_curios_qa" not in str(metadata)
        deps = {dep["modId"]: dep for dep in metadata["dependencies"][mod]}
        assert deps["minecraft"]["versionRange"] == "[26.3]"
        if kind == "core":
            assert "salts_inventory_compat" not in deps and "curios" not in deps
            for api in ["DesktopBuiltinAttachment", "DesktopWindowAttachment"]:
                assert PREFIX + f"api/client/desktop/{api}.class" in names
            for name in classes:
                for value in boundaries.utf8_constants(jar.read(name)):
                    assert "top/theillusivec4/curios/" not in value and "top.theillusivec4.curios." not in value
                    assert PREFIX + "compat/" not in value
        else:
            assert deps["salts_inventory_update"]["type"] == "required"
            assert deps["salts_inventory_update"]["versionRange"] == "[0.1.5]"
            assert deps["curios"]["type"] == "optional" and deps["curios"]["side"] == "BOTH"
            assert PREFIX + "compat/curios/CuriosDesktopMenu.class" in names
            assert not any(name.startswith("assets/curios/") for name in names), "Copied native artwork"
            assert not any(name.endswith(".jar") for name in names), "Nested JAR"
            for name in classes:
                assert not name.startswith(PREFIX) or name.startswith(PREFIX + "compat/"), "Bundled Core class"
                for value in boundaries.utf8_constants(jar.read(name)):
                    assert not boundaries.forbidden_references(value), f"Core implementation reference in {name}"
        print(f"Verified {artifact.relative_to(ROOT)}: metadata, ownership, optional dependency, API boundary, no QA")
