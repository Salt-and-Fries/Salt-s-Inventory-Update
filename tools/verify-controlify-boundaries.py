"""Verify the third mod has its own implementation and only consumes Salt's API."""
from pathlib import Path
import argparse
import importlib.util
import hashlib
import json
import re
import tomllib
from urllib.parse import urlparse
import zipfile
from controlify_classfile import ClassIndex, read_class

ROOT = Path(__file__).resolve().parents[1]
PREFIX = "com/salts_inventory_update/"
ALLOWED = (PREFIX + "api/", PREFIX + "controlify/")
spec = importlib.util.spec_from_file_location("module_boundaries", ROOT / "tools/verify-module-boundaries.py")
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)
VERSIONS = ("1.20.1", "1.21.1", "1.21.11", "26.1.2", "26.2", "26.3")
EXPECTED = {(version, loader) for version in VERSIONS
            for loader in ("fabric", "forge" if version == "1.20.1" else "neoforge")}
BOOTSTRAPS = {"fabric": "SaltsControlifyFabricClient", "forge": "SaltsControlifyForge",
              "neoforge": "SaltsControlifyNeoForge"}


def matrix(path=ROOT / "controlify/targets.json"):
    data = json.loads(Path(path).read_text(encoding="utf-8-sig"))
    assert data["schema_version"] == 1, f"{path}: unsupported pin schema"
    targets = data["targets"]
    keys = [(target["minecraft"], target["loader"]) for target in targets]
    assert len(keys) == len(set(keys)) == 12 and set(keys) == EXPECTED, \
        f"{path}: require all 12 exact supported targets; found {keys}"
    for target in targets:
        forge = target["loader"] == "forge"
        assert target.get("native_forge_port") is forge, f"{path}: incorrect native Forge port marker"
        assert target["controller"]["project"] == ("G2K5sLls" if forge else "controlify"), \
            f"{path}: incorrect controller project for {target['minecraft']}/{target['loader']}"
        for role in ("controller", "yacl"):
            pin = target[role]
            url = urlparse(pin["url"])
            assert url.scheme == "https" and url.hostname == "cdn.modrinth.com" \
                and f"/versions/{pin['version_id']}/" in url.path, f"{path}: unpinned {role} URL"
            assert re.fullmatch(r"[a-f0-9]{128}", pin["sha512"]), f"{path}: invalid {role} SHA512"
            assert pin["version"] and pin["version_id"], f"{path}: missing {role} version"
        runtime = target.get("runtime_dependencies", [])
        assert len(runtime) == (1 if forge else 0), f"{path}: incorrect loader shim inventory"
        if forge:
            shim = runtime[0]
            assert (shim["provider"], shim["project_id"], shim["version_id"], shim["mod_id"], shim["version"]) == \
                ("curseforge", "1257105", "6488404", "mixin_args", "1.0"), f"{path}: unverified Forge Args shim"
            assert shim["coordinate"] == "curse.maven:mixin-args-1257105:6488404", f"{path}: wrong shim coordinate"
            assert shim["url"] == "https://mediafilez.forgecdn.net/files/6488/404/mixin_args-1.0.jar", \
                f"{path}: unpinned Forge shim URL"
            assert shim["filename"] == "mixin_args-1.0.jar" and shim["license"] == "All Rights Reserved", \
                f"{path}: wrong published shim metadata"
            assert re.fullmatch(r"[a-f0-9]{128}", shim["sha512"]), f"{path}: invalid shim SHA512"
    print("Controller upstream matrix verified: 12 immutable targets, including the native Forge port")
    return {key: target for key, target in zip(keys, targets)}


def artifact_target(path):
    match = re.fullmatch(r"salts_inventory_controlify-(.+?)-(Fabric|NeoForge|Forge)-(.+)\.jar", Path(path).name)
    assert match, f"{path}: unexpected controller artifact name"
    key = match[1], match[2].lower()
    assert key in EXPECTED, f"{path}: unsupported target {key}"
    return key, match[3]


def compiler_target(path):
    path = Path(path)
    key = path.parent.parent.parent.name, path.parent.parent.name
    assert key in EXPECTED, f"{path}: compiler manifest must identify its exact target"
    return key


def matrix_sources(targets):
    for game, loader in targets:
        folder = ROOT / f"controlify/versions/{game}"
        runtime = folder / "common/src/main/java/com/salts_inventory_update/controlify"
        assert (runtime / "SaltsControlifyClient.java").is_file(), f"{folder}: missing actual runtime"
        bootstrap = folder / f"{loader}/src/main/java/com/salts_inventory_update/controlify/{BOOTSTRAPS[loader]}.java"
        assert bootstrap.is_file(), f"{bootstrap}: missing loader entrypoint"
        resource = "fabric.mod.json" if loader == "fabric" else \
            "META-INF/mods.toml" if loader == "forge" else "META-INF/neoforge.mods.toml"
        assert (folder / loader / "src/main/resources" / resource).is_file(), f"{folder}/{loader}: missing metadata"
    print("Controller runtime/entrypoint source inventory verified for all 12 targets")


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
    public_workspace = False
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
        public_workspace |= PREFIX + "api/client/desktop/DesktopWorkspace.class" in names
    assert entries, f"{manifest}: empty compiler manifest"
    assert public_workspace, f"{manifest}: missing public workspace API in compiler inputs"
    print(f"Controller compiler isolation verified: {len(entries)} entries")
    return entries


def jar(path, core_version=None):
    path = Path(path)
    (game, loader), version = artifact_target(path)
    if core_version is None:
        # Resolve the target's Core hotfix independently of the companion release label.
        declaration = (ROOT / "build.gradle.kts").read_text(encoding="utf-8")
        core_version = re.search(r'^version = "([^"]+)"$', declaration, re.MULTILINE)[1]
        properties = (ROOT / 'gradle.properties').read_text(encoding='utf-8')
        override = re.search(r'^' + re.escape('coreVersion' + game.replace('.', '_')) + r'=(\S+)\s*$',
                             properties, re.MULTILINE)
        if override:
            core_version = override[1]
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
        if loader == "fabric":
            metadata = json.loads(archive.read("fabric.mod.json"))
            assert metadata["id"] == "salts_inventory_controlify", path
            assert metadata["environment"] == "client", path
            assert "salts_inventory_controlify.mixins.json" in metadata["mixins"], path
            assert "salts_inventory_update" in metadata["depends"] and "controlify" in metadata["depends"], path
            assert "salts_inventory_compat" not in metadata["depends"], path
            assert metadata["version"] == version and metadata["depends"]["minecraft"] == game, \
                f"{path}: metadata targets a different game/mod version"
            assert metadata["depends"]["salts_inventory_update"] == "=" + core_version, \
                f"{path}: requires an unmatched core version"
            for entrypoints in metadata["entrypoints"].values():
                for entry in entrypoints:
                    entry = entry if isinstance(entry, str) else entry["value"]
                    entry = entry.split("::", 1)[0]
                    assert entry.replace(".", "/") + ".class" in names, f"{path}: missing entrypoint {entry}"
        else:
            resource = "META-INF/mods.toml" if loader == "forge" else "META-INF/neoforge.mods.toml"
            metadata = tomllib.loads(archive.read(resource).decode())
            assert len(metadata["mods"]) == 1 and metadata["mods"][0]["modId"] == "salts_inventory_controlify", path
            assert metadata["mods"][0]["version"] == version, f"{path}: incorrect mod version"
            dependencies = {dep["modId"]: dep for dep in metadata["dependencies"]["salts_inventory_controlify"]}
            assert {"minecraft", "salts_inventory_update", "controlify"}.issubset(dependencies), path
            assert "salts_inventory_compat" not in dependencies, path
            assert dependencies["minecraft"]["versionRange"] == f"[{game}]", f"{path}: incorrect Minecraft target"
            assert dependencies["salts_inventory_update"]["versionRange"] == f"[{core_version}]", f"{path}: unmatched core version"
            for required in ("salts_inventory_update", "controlify"):
                dep = dependencies[required]
                assert dep.get("mandatory") is True if loader == "forge" else dep.get("type") == "required", \
                    f"{path}: {required} must be required"
                assert dep.get("side") == "CLIENT", f"{path}: {required} dependency must be client-only"
            if loader == "forge":
                shim = dependencies.get("mixin_args", {})
                assert shim.get("mandatory") is True and shim.get("side") == "CLIENT" \
                    and shim.get("versionRange") == "[1.0,)", f"{path}: missing required Forge Args shim"
                manifest = archive.read("META-INF/MANIFEST.MF").decode().replace("\r\n ", "").replace("\n ", "")
                assert re.search(r"(?m)^MixinConfigs:.*salts_inventory_controlify\.mixins\.json", manifest), \
                    f"{path}: Forge requires the controller mixins in the JAR manifest"
            else:
                assert any(entry["config"] == "salts_inventory_controlify.mixins.json"
                           for entry in metadata["mixins"]), f"{path}: missing mixin registration"
        assert PREFIX + f"controlify/{BOOTSTRAPS[loader]}.class" in names, f"{path}: missing actual loader bootstrap"
        assert PREFIX + "controlify/SaltsControlifyClient.class" in names, f"{path}: missing controller runtime"
        assert PREFIX + "controlify/input/ControlifyInputBridge.class" in names, f"{path}: missing typed controller input"
    print(f"Third production JAR isolation verified: {path.name} ({len(classes)} controller classes)")
    return game, loader


def native_symbols(companion, libraries, minecraft=False):
    prefixes = ("dev/isxander/controlify/",)
    if minecraft:
        prefixes += ("net/minecraft/", "com/mojang/blaze3d/", "com/mojang/renderpearl/")
    index = ClassIndex([companion, *libraries])
    count, selectors, obligations = 0, 0, 0
    try:
        with zipfile.ZipFile(companion) as archive:
            for entry in archive.namelist():
                if not entry.endswith(".class"):
                    continue
                clazz = read_class(archive.read(entry))
                if not clazz.access & 0x0400:  # Concrete adapters must implement current native contracts.
                    # Forge reobfuscates vanilla override names in production archives.
                    # Third-party API contracts keep their published names on every loader.
                    for owner, member in index.native_obligations(clazz, ("dev/isxander/controlify/",)):
                        assert index.concrete_method(clazz.name, member), \
                            f"{companion}!{entry}: missing native abstract implementation {owner}.{member[0]}{member[1]}"
                        obligations += 1
                for name in clazz.classes:
                    if name.startswith(prefixes):
                        assert index.get(name) is not None, f"{companion}!{entry}: missing native class {name}"
                for kind, owner, name, descriptor in clazz.references:
                    if not owner.startswith(prefixes):
                        continue
                    count += 1
                    assert index.member(owner, kind, name, descriptor), \
                        f"{companion}!{entry}: unresolved native {kind} {owner}.{name}{descriptor}"
                for target in clazz.mixin_targets:
                    if not target.startswith(prefixes):
                        continue
                    assert index.get(target) is not None, f"{companion}!{entry}: missing mixin target {target}"
                    for selector in clazz.injections:
                        name, _, tail = selector.partition("(")
                        descriptor = "(" + tail if tail else None
                        if name.endswith("*"):
                            assert any(member[0].startswith(name[:-1]) for member in index.get(target).methods), \
                                f"{companion}!{entry}: no mixin target matches {target}.{selector}"
                        else:
                            assert index.member(target, "method", name, descriptor), \
                                f"{companion}!{entry}: missing native mixin method {target}.{selector}"
                        selectors += 1
        assert count, f"{companion}: no real native controller calls to verify"
        print(f"Native symbols verified: {Path(companion).name} ({count} member references, {selectors} mixin selectors, {obligations} native contracts)")
    finally:
        index.close()


def peer_jar(path):
    with zipfile.ZipFile(path) as archive:
        names = archive.namelist()
        assert not any(name.startswith(PREFIX + "controlify/") for name in names), \
            f"{path}: controller implementation was packaged into another Salt mod"
        assert not any(name.startswith("dev/isxander/controlify/") for name in names), \
            f"{path}: Controlify implementation was packaged into another Salt mod"
        assert not any(name.startswith("org/spongepowered/asm/synthetic/args/") for name in names), \
            f"{path}: Forge runtime shim was packaged into another Salt mod"
        for name in names:
            if not name.endswith(".class"):
                continue
            for constant in module.utf8_constants(archive.read(name)):
                assert not re.search(r"com[./]salts_inventory_update[./]controlify[./]|dev[./]isxander[./]controlify[./]", constant), \
                    f"{path}!{name}: another Salt mod depends on the controller implementation"
        if "fabric.mod.json" in names:
            metadata = json.loads(archive.read("fabric.mod.json"))
            assert not {"controlify", "salts_inventory_controlify", "mixin_args"}.intersection(metadata.get("depends", {})), \
                f"{path}: another Salt mod requires the controller integration"
        else:
            entry = "META-INF/mods.toml" if "META-INF/mods.toml" in names else "META-INF/neoforge.mods.toml"
            metadata = archive.read(entry).decode()
            assert not re.search(r'modId\s*=\s*"(?:controlify|salts_inventory_controlify|mixin_args)"', metadata), \
                f"{path}: another Salt mod requires the controller integration"
    print(f"Peer JAR remains separate from controller integration: {Path(path).name}")
    match = re.fullmatch(r"salts_inventory_(update|compat)-(.+?)-(Fabric|NeoForge|Forge)-(.+)\.jar", Path(path).name)
    assert match, f"{path}: unexpected peer artifact name"
    return match[1], (match[2], match[3].lower())


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--core-version", help="Required Core release (defaults to the root build version)")
    parser.add_argument("--sources", action="store_true")
    parser.add_argument("--jar", action="append", default=[])
    parser.add_argument("--classpath", action="append", default=[])
    parser.add_argument("--peer-jar", action="append", default=[])
    parser.add_argument("--matrix", action="store_true", help="Require the complete 12-target pin/source/artifact inventory")
    parser.add_argument("--targets", type=Path, default=ROOT / "controlify/targets.json")
    parser.add_argument("--native-symbols", nargs=2, action="append", default=[], metavar=("JAR", "LIBRARY"),
                        help="Check packaged controller references against the actual published Controlify JAR")
    parser.add_argument("--native-classpath", nargs=2, action="append", default=[], metavar=("DEV_JAR", "MANIFEST"),
                        help="Check named adapter/Minecraft symbols and mixin selectors against its compiler inputs")
    args = parser.parse_args()
    targets = matrix(args.targets) if args.matrix else None
    if args.sources:
        sources()
        if targets:
            matrix_sources(targets)
    for item in args.classpath:
        classpath(item)
    found = [jar(item, args.core_version) for item in args.jar]
    if targets and found:
        assert len(found) == len(set(found)) == 12 and set(found) == set(targets), \
            f"Require exactly one actual companion per target; found {found}"
        compilers = [compiler_target(path) for path in args.classpath]
        assert len(compilers) == len(set(compilers)) == 12 and set(compilers) == set(targets), \
            "Require one correct controller compiler manifest for every target"
        native = [compiler_target(manifest) for _, manifest in args.native_classpath]
        assert len(native) == len(set(native)) == 12 and set(native) == set(targets), \
            "Require named adapter/Minecraft native-symbol checks for all 12 targets"
    peers = [peer_jar(item) for item in args.peer_jar]
    if targets and found:
        core = [key for kind, key in peers if kind == "update"]
        assert len(core) == len(set(core)) == 12 and set(core) == set(targets), \
            "Require one matching core peer JAR for each controller target"
        extended = [key for kind, key in peers if kind == "compat"]
        if extended:
            assert len(extended) == len(set(extended)) == 12 and set(extended) == set(targets), \
                "Extended peer checks must cover the complete 12-target matrix"
    for companion, library in args.native_symbols:
        if targets:
            key, _ = artifact_target(companion)
            expected = targets[key]["controller"]["sha512"]
            assert hashlib.sha512(Path(library).read_bytes()).hexdigest() == expected, \
                f"{library}: differs from the immutable published controller pin for {key}"
        native_symbols(companion, [library])
    for companion, manifest in args.native_classpath:
        entries = [Path(line) for line in Path(manifest).read_text(encoding="utf-8").splitlines() if line]
        native_symbols(companion, entries, minecraft=True)
