"""Verify the third mod has its own implementation and only consumes Salt's API."""
from pathlib import Path
import argparse
import importlib.util
import json
import re
import zipfile

ROOT = Path(__file__).resolve().parents[1]
PREFIX = "com/salts_inventory_update/"
ALLOWED = (PREFIX + "api/", PREFIX + "controlify/")
spec = importlib.util.spec_from_file_location("module_boundaries", ROOT / "tools/verify-module-boundaries.py")
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)


def forbidden(text):
    names = re.findall(r"com[./]salts_inventory_update[./][\w.$/]+", text)
    return sorted({name for name in names
                   if name.replace(".", "/") != PREFIX + "controlify"
                   and not name.replace(".", "/").startswith(ALLOWED)})


def sources():
    files = [p for p in (ROOT / "controlify").rglob("*.java") if "build" not in p.parts
             and "functional-tests" not in p.parts]
    assert files, "No controller implementation sources found"
    for path in files:
        bad = forbidden(path.read_text(encoding="utf-8"))
        assert not bad, f"{path.relative_to(ROOT)}: non-public Salt references {bad}"
    for folder in (ROOT / "versions", ROOT / "compatibility"):
        for path in folder.rglob("*.java"):
            if "build" in path.parts or "tests" in path.parts:
                continue
            assert not re.search(r"com\.salts_inventory_update\.controlify\.", path.read_text(encoding="utf-8")), \
                f"{path.relative_to(ROOT)}: other mod depends on controller implementation"
    print(f"Controller source isolation verified: {len(files)} Java files")


def classpath(manifest):
    entries = [Path(line) for line in Path(manifest).read_text(encoding="utf-8").splitlines() if line]
    for path in entries:
        if path.is_dir():
            names = [p.relative_to(path).as_posix() for p in (path / "com/salts_inventory_update").rglob("*.class")]
        elif path.is_file() and path.suffix == ".jar":
            with zipfile.ZipFile(path) as archive:
                names = archive.namelist()
        else:
            continue
        bad = [name for name in names if name.endswith(".class") and name.startswith(PREFIX)
               and not name.startswith(ALLOWED)]
        assert not bad, f"{path}: controller compiler can access core/extended implementation {bad[:10]}"
    assert entries, f"{manifest}: empty compiler manifest"
    print(f"Controller compiler isolation verified: {len(entries)} entries")


def jar(path):
    path = Path(path)
    assert not any(marker in path.name for marker in ("-dev", "-sources", "-api")), path
    with zipfile.ZipFile(path) as archive:
        names = set(archive.namelist())
        classes = [name for name in names if name.endswith(".class")]
        assert classes, f"{path}: empty controller artifact"
        assert all(name.startswith(PREFIX + "controlify/") for name in classes), \
            f"{path}: bundled core, extended, or dependency code"
        assert not any("/qa/" in name or "ControlifyQa" in name or "GameplayHarness" in name for name in names), \
            f"{path}: bundled controller QA instrumentation"
        assert not any(name.endswith(".jar") for name in names), f"{path}: bundled nested dependency"
        assert "salts_inventory_controlify.mixins.json" in names, f"{path}: missing controller render mixin configuration"
        mixins = json.loads(archive.read("salts_inventory_controlify.mixins.json"))
        for mixin in mixins.get("client", []):
            entry = (mixins["package"] + "." + mixin).replace(".", "/") + ".class"
            assert entry in names, f"{path}: missing mixin class {entry}"
        for name in classes:
            for constant in module.utf8_constants(archive.read(name)):
                bad = forbidden(constant)
                assert not bad, f"{path.name}!{name}: non-public Salt bytecode references {bad}"
        if "fabric.mod.json" in names:
            metadata = json.loads(archive.read("fabric.mod.json"))
            assert metadata["id"] == "salts_inventory_controlify", path
            assert metadata["environment"] == "client", path
            assert "salts_inventory_controlify.mixins.json" in metadata["mixins"], path
            assert "salts_inventory_update" in metadata["depends"] and "controlify" in metadata["depends"], path
            assert "salts_inventory_compat" not in metadata["depends"], path
            for entrypoints in metadata["entrypoints"].values():
                for entry in entrypoints:
                    assert entry.replace(".", "/") + ".class" in names, f"{path}: missing entrypoint {entry}"
        else:
            metadata = archive.read("META-INF/neoforge.mods.toml").decode()
            assert 'modId = "salts_inventory_controlify"' in metadata, path
            assert 'modId = "salts_inventory_update"' in metadata and 'modId = "controlify"' in metadata, path
            assert 'modId = "salts_inventory_compat"' not in metadata, path
            assert 'config = "salts_inventory_controlify.mixins.json"' in metadata, path
            assert PREFIX + "controlify/SaltsControlifyNeoForge.class" in names, path
    print(f"Third production JAR isolation verified: {path.name} ({len(classes)} controller classes)")


def peer_jar(path):
    with zipfile.ZipFile(path) as archive:
        names = archive.namelist()
        assert not any(name.startswith(PREFIX + "controlify/") for name in names), \
            f"{path}: controller implementation was packaged into another Salt mod"
        assert not any(name.startswith("dev/isxander/controlify/") for name in names), \
            f"{path}: Controlify implementation was packaged into another Salt mod"
        for name in names:
            if not name.endswith(".class"):
                continue
            for constant in module.utf8_constants(archive.read(name)):
                assert not re.search(r"com[./]salts_inventory_update[./]controlify[./]|dev[./]isxander[./]controlify[./]", constant), \
                    f"{path}!{name}: another Salt mod depends on the controller implementation"
        if "fabric.mod.json" in names:
            metadata = json.loads(archive.read("fabric.mod.json"))
            assert not {"controlify", "salts_inventory_controlify"}.intersection(metadata.get("depends", {})), \
                f"{path}: another Salt mod requires the controller integration"
        else:
            entry = "META-INF/mods.toml" if "META-INF/mods.toml" in names else "META-INF/neoforge.mods.toml"
            metadata = archive.read(entry).decode()
            assert not re.search(r'modId\s*=\s*"(?:controlify|salts_inventory_controlify)"', metadata), \
                f"{path}: another Salt mod requires the controller integration"
    print(f"Peer JAR remains separate from controller integration: {Path(path).name}")


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--sources", action="store_true")
    parser.add_argument("--jar", action="append", default=[])
    parser.add_argument("--classpath", action="append", default=[])
    parser.add_argument("--peer-jar", action="append", default=[])
    args = parser.parse_args()
    if args.sources:
        sources()
    for item in args.classpath:
        classpath(item)
    for item in args.jar:
        jar(item)
    for item in args.peer_jar:
        peer_jar(item)
