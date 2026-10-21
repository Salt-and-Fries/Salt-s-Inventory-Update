"""Verify selected focused controller runs against exact final production artifacts.

This validates the supplied scopes only. It does not replace a full gameplay run
or imply that a focused run covers every feature on its target.
"""
import argparse
from collections import Counter
import importlib.util
import io
import os
import json
from pathlib import Path
import re
import struct
import shutil
import subprocess
import tempfile
from urllib.parse import unquote, urlsplit
from zipfile import ZipFile
import zlib


ROOT = Path(__file__).resolve().parents[1]
LEGACY = {"1.20.1", "1.21.1", "1.21.11"}
MODERN = {"26.1.2", "26.2", "26.3"}
SALT_MODULES = {
    "core": "salts_inventory_update",
    "compatibility": "salts_inventory_compat",
    "controlify": "salts_inventory_controlify",
}


def load_tool(name, filename):
    spec = importlib.util.spec_from_file_location(name, ROOT / "tools" / filename)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


runtime = load_tool("focused_controller_runtime", "verify-controlify-runtime.py")
compat_runtime = load_tool("focused_controller_compat", "verify-controlify-compat-runtime.py")


def read_json(path):
    return json.loads(path.read_text(encoding="utf-8-sig"))


def pinned_matrix():
    targets = read_json(ROOT / "controlify/targets.json")["targets"]
    expected = {(game, loader) for game in LEGACY | MODERN
                for loader in ("fabric", "forge" if game == "1.20.1" else "neoforge")}
    keys = [(target["minecraft"], target["loader"]) for target in targets]
    assert len(keys) == len(set(keys)) == 12 and set(keys) == expected, \
        "Pinned controller inventory must describe the exact twelve supported targets"
    return dict(zip(keys, targets))


def plain_filename(value):
    return isinstance(value, str) and bool(value) and value not in {".", ".."} \
        and not any(character in value for character in "/\\:")


def code_source_path(value):
    """Decode native file/Forge union CodeSources without accepting sibling paths."""
    assert isinstance(value, str), "Missing native JAR CodeSource"
    if value.startswith("jar:"):
        value = value[4:]
    parsed = urlsplit(value)
    assert parsed.scheme in {"file", "union"} and not parsed.netloc \
        and not parsed.query and not parsed.fragment, f"Unexpected native CodeSource: {value}"
    path = unquote(parsed.path).replace("\\", "/")
    match = re.fullmatch(r"(.+\.jar)(?:#\d+)?(?:!/)?", path, flags=re.IGNORECASE)
    assert match, f"CodeSource is not an actual top-level JAR: {value}"
    path = match.group(1)
    if re.match(r"^/[A-Za-z]:/", path):
        path = path[1:]
    return Path(path).resolve()


def validate_png(path):
    """Check dimensions, complete PNG chunks, checksums and compressed image data."""
    data = path.read_bytes()
    assert data.startswith(b"\x89PNG\r\n\x1a\n"), f"Invalid screenshot PNG: {path}"
    offset, chunks, compressed = 8, [], bytearray()
    width = height = 0
    while offset < len(data):
        assert offset + 12 <= len(data), f"Truncated screenshot PNG: {path}"
        length = struct.unpack_from(">I", data, offset)[0]
        kind = data[offset + 4:offset + 8]
        end = offset + 12 + length
        assert end <= len(data), f"Truncated screenshot chunk: {path}"
        payload = data[offset + 8:offset + 8 + length]
        crc = struct.unpack_from(">I", data, offset + 8 + length)[0]
        assert zlib.crc32(kind + payload) & 0xFFFFFFFF == crc, f"Screenshot checksum mismatch: {path}"
        if not chunks:
            assert kind == b"IHDR" and length == 13, f"Missing screenshot dimensions: {path}"
            width, height = struct.unpack_from(">II", payload)
            assert width > 0 and height > 0, f"Empty screenshot: {path}"
        if kind == b"IDAT":
            compressed.extend(payload)
        chunks.append(kind)
        offset = end
        if kind == b"IEND":
            assert length == 0 and offset == len(data), f"Trailing screenshot data: {path}"
            break
    assert chunks[-1:] == [b"IEND"] and compressed, f"Incomplete screenshot: {path}"
    image = zlib.decompressobj()
    decoded = image.decompress(compressed) + image.flush()
    assert image.eof and not image.unused_data and decoded, f"Invalid screenshot image data: {path}"
    return {"file": path.name, "width": width, "height": height, "sha256": runtime.digest(path)}


def loaded_mods(report, name):
    mods = report["loadedMods"]
    if isinstance(mods, list):
        assert len(mods) == len({row["id"] for row in mods}), f"{name}: duplicate loaded mod IDs"
        mods = {row["id"]: row["version"] for row in mods}
    assert isinstance(mods, dict), f"{name}: missing native loaded-mod inventory"
    return mods


_FABRIC_PROOFS = {}


def java_tools():
    candidates = []
    if os.environ.get("JAVA_HOME"):
        candidates.append(Path(os.environ["JAVA_HOME"]) / "bin")
    if shutil.which("javac"):
        candidates.append(Path(shutil.which("javac")).resolve().parent)
    candidates.extend(sorted((Path.home() / ".jdks").glob("*/bin"), reverse=True))
    for folder in candidates:
        java, javac = folder / ("java.exe" if os.name == "nt" else "java"), folder / ("javac.exe" if os.name == "nt" else "javac")
        if java.is_file() and javac.is_file():
            return java, javac
    raise AssertionError("A local JDK is required to reproduce native Fabric remapping")


def fabric_named_lineage(profile, game, staged, mods, report_path):
    """Reproduce the actual loader's namespace remap; compare every generated JAR member."""
    name = profile.name
    processed = (profile / ".fabric/processedMods").resolve()
    assert processed.parent == (profile / ".fabric").resolve() and processed.is_dir(), f"{name}: missing native processed-mod directory"
    debug = profile / "logs/debug.log"
    log = debug.read_text(encoding="utf-8", errors="strict")
    versions = set(re.findall(r"Loading Minecraft " + re.escape(game) + r" with Fabric Loader ([^\s]+)", log))
    assert len(versions) == 1, f"{name}: ambiguous native Fabric loader version"
    loader_version = versions.pop()
    mapping_paths = {Path(value).resolve() for value in re.findall(r"Adding (.+mappings\.jar) to classpath\.", log)}
    assert len(mapping_paths) == 1, f"{name}: native mapping JAR was not recorded uniquely"
    mappings = mapping_paths.pop()
    assert mappings.is_file() and game in mappings.parts, f"{name}: wrong or missing native game mapping JAR"
    target = ROOT / f"controlify/versions/{game}/fabric"
    remap_classpath = target / ".gradle/loom-cache/remapClasspath.txt"
    compile_classpath = target / "build/controlify-compile-classpath.txt"
    paths = [Path(value).resolve() for value in compile_classpath.read_text(encoding="utf-8").splitlines() if value]
    loader_jars = [path for path in paths if path.name == f"fabric-loader-{loader_version}.jar"]
    assert len(loader_jars) == 1, f"{name}: native Fabric loader JAR is missing or ambiguous"
    loader = loader_jars[0]
    libraries = [path for path in paths if any(group in str(path).replace("\\", "/") for group in
                 ("/org.ow2.asm/", "/org.apache.logging.log4j/", "/org.slf4j/"))]
    assert libraries and loader.is_file() and remap_classpath.is_file(), f"{name}: missing native remapper dependencies"
    runtime_paths = [Path(value).resolve() for value in remap_classpath.read_text(encoding="utf-8").strip().split(os.pathsep) if value]
    assert runtime_paths and all(path.is_file() for path in runtime_paths), f"{name}: missing native remapping classpath entry"
    actuals = {}
    for path in sorted(processed.glob("*.jar")):
        assert path.resolve().parent == processed and path.stat().st_mtime_ns <= report_path.stat().st_mtime_ns, f"{name}: processed JAR escaped the launch profile or postdates gameplay"
        with ZipFile(path) as jar:
            assert len(jar.namelist()) == len(set(jar.namelist())) and jar.testzip() is None, f"{name}: corrupt processed native JAR"
            metadata = json.loads(jar.read("fabric.mod.json"))
        identity = (metadata["id"], str(metadata["version"]))
        assert identity not in actuals and mods.get(identity[0]) == identity[1], f"{name}: stale, duplicate or unloaded native processed mod {identity}"
        actuals[identity] = path
    assert actuals, f"{name}: no native processed JARs"
    candidates = {}
    roots = {}
    def discover(data, provenance, depth=0):
        assert depth < 12, f"{name}: invalid recursive mod input"
        with ZipFile(io.BytesIO(data)) as jar:
            assert len(jar.namelist()) == len(set(jar.namelist())) and jar.testzip() is None, f"{name}: corrupt original/nested JAR"
            if "fabric.mod.json" not in jar.namelist():
                return None
            metadata = json.loads(jar.read("fabric.mod.json"))
            identity = (metadata["id"], str(metadata["version"]))
            candidates.setdefault(identity, []).append((data, provenance))
            for nested in metadata.get("jars", []):
                member = nested["file"]
                assert isinstance(member, str) and not member.startswith(("/", "\\")) and ".." not in member.replace("\\", "/").split("/"), f"{name}: invalid declared nested mod path"
                discover(jar.read(member), provenance + "!" + member, depth + 1)
            return identity
    for filename, path in sorted(staged.items()):
        identity = discover(path.read_bytes(), filename)
        if identity:
            roots[filename] = identity
    assert all(identity in actuals for identity in roots.values()), f"{name}: not every original root mod was natively remapped"
    sources = []
    import hashlib
    for identity, actual in sorted(actuals.items()):
        found = candidates.get(identity, [])
        assert found and len({hashlib.sha256(data).hexdigest() for data, _ in found}) == 1, f"{name}: absent or ambiguous original for native processed {identity}"
        data, provenance = found[0]
        sources.append((identity, actual, data, provenance, hashlib.sha256(data).hexdigest()))
    java, javac = java_tools()
    helper = ROOT / "tools/VerifyFabricRuntimeRemap.java"
    assert helper.is_file(), "Native Fabric remap reproduction helper is missing"
    tools = [loader, mappings, remap_classpath, helper, *libraries]
    tool_hashes = {str(path): runtime.digest(path) for path in tools}
    classpath_hashes = {str(path): runtime.digest(path) for path in runtime_paths}
    actual_hashes = {str(actual): runtime.digest(actual) for _, actual, _, _, _ in sources}
    key = (tuple(sorted(tool_hashes.items())), tuple(sorted(classpath_hashes.items())), tuple(sorted(actual_hashes.items())), tuple((identity, digest) for identity, _, _, _, digest in sources))
    if key not in _FABRIC_PROOFS:
        scratch_root = ROOT / "tmp/controlify-fabric-lineage"
        scratch_root.mkdir(parents=True, exist_ok=True)
        with tempfile.TemporaryDirectory(prefix=game + "-", dir=scratch_root) as directory:
            scratch = Path(directory)
            classes = scratch / "classes"
            classes.mkdir()
            classpath = os.pathsep.join(str(path) for path in [loader, *libraries, mappings])
            compile_run = subprocess.run([str(javac), "-encoding", "UTF-8", "-cp", classpath, "-d", str(classes), str(helper)], capture_output=True, text=True, encoding="utf-8", errors="replace")
            assert compile_run.returncode == 0, f"{name}: cannot compile native remap proof: {compile_run.stdout} {compile_run.stderr}"
            args = [str(java), "-cp", str(classes) + os.pathsep + classpath, "VerifyFabricRuntimeRemap", str(mappings), str(remap_classpath), str(scratch / "native")]
            for index, (_, actual, data, _, _) in enumerate(sources):
                original = scratch / f"original-{index:03d}.jar"
                original.write_bytes(data)
                args += [str(original), str(actual)]
            run = subprocess.run(args, capture_output=True, text=True, encoding="utf-8", errors="replace")
            assert run.returncode == 0, f"{name}: native Fabric remap reproduction failed: {run.stdout[-6000:]} {run.stderr[-6000:]}"
            proof = re.findall(r"FABRIC_LINEAGE_VERIFIED count=(\d+) members=(\d+)", run.stdout)
            assert len(proof) == 1 and int(proof[0][0]) == len(sources) and int(proof[0][1]) > 0, f"{name}: native remap proof did not compare all selected JARs"
            _FABRIC_PROOFS[key] = {"operation": "Fabric RuntimeModRemapper intermediary-to-named", "loader_version": loader_version,
                "tool_sha256": tool_hashes, "classpath_sha256": classpath_hashes, "jar_count": len(sources), "member_count": int(proof[0][1]),
                "artifacts": [{"mod_id": identity[0], "version": identity[1], "original": provenance, "original_sha256": digest,
                    "mapped": str(actual), "mapped_sha256": actual_hashes[str(actual)]} for identity, actual, _, provenance, digest in sources]}
    mapping = {filename: actuals[identity].resolve() for filename, identity in roots.items()}
    return mapping, _FABRIC_PROOFS[key]


def validate(folder, artifacts, pins=None):
    folder, artifacts = Path(folder).resolve(), Path(artifacts).resolve()
    if folder.is_file():
        assert folder.name == "results.json", "--report must name a report directory or results.json"
        folder = folder.parent
    name = folder.name
    assert folder.parent == (ROOT / "build/controlify-qa").resolve(), \
        f"{name}: focused reports must remain in the retained gameplay evidence directory"
    report_path, staging_path = folder / "results.json", folder / "staged-artifacts.json"
    report, staging = read_json(report_path), read_json(staging_path)
    game, loader, scope = staging["minecraft"], staging["loader"], staging["scope"]
    pins = pins if pins is not None else pinned_matrix()
    assert (game, loader) in pins, f"{name}: unsupported target"
    pin = pins[game, loader]
    allowed_scopes = {"feature-parity", "playtest-adapters"} if game in LEGACY else {"public-api", "wheel-polish"}
    if game == "26.3": allowed_scopes = allowed_scopes | {"playtest-fixes", "status-tray"}
    assert scope in allowed_scopes, f"{name}: unsupported focused scope {scope}"
    expected_scenario = "controlify-controller-world" if game in LEGACY else "controlify-" + scope
    assert report.get("scenario") == expected_scenario, f"{name}: unexpected gameplay harness"
    assert report.get("scope", scope) == scope, f"{name}: report/launcher scope mismatch"
    run_id = staging.get('runId', '')
    assert not run_id or re.fullmatch(r'[a-z0-9-]+', run_id), f'{name}: invalid run ID'
    expected_name = f"{game}-{loader}-{scope}-extended" + ('-' + run_id if run_id else '')
    assert name == expected_name, f"{name}: target/scope/Extended path mismatch"
    assert staging.get("withExtension") is True and not staging.get("fixtures") \
        and not (folder / "optional-mods.json").exists(), f"{name}: expected the isolated matching Extended profile"
    records = report["results"]
    assert type(report.get("failures")) is int and report["failures"] == 0 and records, \
        f"{name}: focused gameplay run has failures or no recorded checks"
    assert all(row.get("status") in {"PASS", "CAPTURED", "OBSERVED"} for row in records), \
        f"{name}: unfinished or failed gameplay evidence"
    elapsed = report["elapsedMs"]
    assert type(elapsed) is int and elapsed > 0 \
        and all(type(row.get("elapsedMs")) is int and 0 <= row["elapsedMs"] <= elapsed for row in records), \
        f"{name}: invalid native scenario timestamps"
    passed = {row["name"] for row in records if row["status"] == "PASS"}
    assert {"actual-controlify-driver", "real-input-bindings-created", "salt-server-handshake"} <= passed, \
        f"{name}: missing actual native controller/server setup"
    if scope == "feature-parity":
        runtime.legacy_feature_parity(passed, name)
    elif scope == "playtest-adapters":
        required = {
            "controls-entry-visible", "native-settings-open", "LT-stays-open-after-release",
            "RT-stays-open-after-release", "LT-toggle-off", "RT-toggle-off", "crafts-four-planks",
            "catalog-does-not-move", "wheel-angle-does-not-move", "selected-recipe-does-not-move",
            "recipe-availability-refreshes", "native-cursor-holds-seven", "close-settles-cursor-and-grid",
            "fuel-reaches-fuel-slot", "full-fuel-preserves-source", "swap-keeps-held-direction",
        }
        compat_runtime.require(passed, {"parity-playtest-" + check for check in required}, name)
    elif scope == "public-api":
        compat_runtime.require(passed, compat_runtime.API_CHECKS, name)
    elif scope == "status-tray":
        required = {"tray-" + check + "-" + str(size)
            for size in (1280, 960) for check in ("six-countdowns", "dark-grey", "no-shadow", "keeps-character-scale",
                "below-character", "fits-screen", "replaces-old-side-list")}
        required |= {"tray-native-list-opt-out-restores", "tray-api-ownership-restored", "tray-hidden-without-effects"}
        compat_runtime.require(passed, required, name)
    elif scope == "playtest-fixes":
        required = {
            "LT-toggle-remains-open-after-release", "RT-toggle-remains-open-after-release",
            "playtest-LT-toggle-off", "playtest-RT-toggle-off",
            "playtest-crafting-keeps-catalog-membership", "playtest-crafting-keeps-wheel-angle",
            "playtest-crafting-keeps-selected-recipe", "playtest-depleted-recipe-updates-availability",
            "playtest-craft-produced-exactly-four-planks", "playtest-carried-item-uses-cursor-scale",
            "playtest-cursor-and-character-grid-returned", "playtest-coal-routes-only-to-fuel",
            "playtest-full-fuel-does-not-spill-into-input", "playtest-cursor-overflow-drops-exactly-once",
            "playtest-focus-swap-preserves-held-direction", "playtest-focus-swap-preserves-other-ring-positions",
            "playtest-cycle-keeps-outgoing-native-pane-during-transition",
            "playtest-cycle-fades-in-incoming-native-pane", "playtest-cycle-finishes-with-one-selected-native-pane",
            "playtest-main-menu-physical-text-path", "playtest-main-menu-native-keyboard-types",
            "playtest-main-menu-overlay-accepts-physical-character", "playtest-main-menu-overlay-accepts-physical-backspace",
            "playtest-main-menu-typing-resumes-after-keyboard", "playtest-controls-entry-visible-and-active",
            "playtest-native-controls-settings",
        }
        required |= {f"effects-option-{option}-{check}-scale-{scale}"
            for option in range(1, 5) for check in ("fits", "preserves-character") for scale in (2, 3)}
        compat_runtime.require(passed, required, name)
    else:
        compat_runtime.require(passed, compat_runtime.FOCUSED_CHECKS[scope], name)
    isolation = runtime.host_motion_isolation(report, name)

    profile_name = staging["runtimeProfile"]
    assert plain_filename(profile_name) and profile_name == "packaged-controlify-" + name, \
        f"{name}: invalid disposable runtime profile or mismatched scope"
    base = (ROOT / f"controlify/versions/{game}/{loader}/build/runtime-smoke").resolve()
    profile = (base / profile_name).resolve()
    assert profile.parent == base, f"{name}: runtime profile escapes its target directory"
    mods_folder = (profile / "mods").resolve()
    assert mods_folder.parent == profile, f"{name}: staged mods escape the disposable profile"
    inventory = staging["artifacts"]
    assert inventory and all(plain_filename(row["file"]) and row["file"].endswith(".jar") for row in inventory), \
        f"{name}: invalid launch inventory filename"
    filenames = [row["file"] for row in inventory]
    assert len(filenames) == len(set(filenames)) == len({filename.casefold() for filename in filenames}), \
        f"{name}: duplicate launch inventory entries"
    staged = {path.name: path for path in mods_folder.glob("*.jar")}
    assert set(staged) == set(filenames), f"{name}: stale, missing or unrecorded staged JAR"
    for row in inventory:
        path = staged[row["file"]]
        assert path.resolve().parent == mods_folder and type(row["bytes"]) is int \
            and path.stat().st_size == row["bytes"] and runtime.digest(path) == row["sha256"], \
            f"{name}: staged JAR differs from its launch evidence: {path.name}"
        assert report_path.stat().st_mtime_ns >= path.stat().st_mtime_ns, f"{name}: report predates staged {path.name}"
    assert report_path.stat().st_mtime_ns >= staging_path.stat().st_mtime_ns, \
        f"{name}: report predates its launcher inventory"
    outputs = [Path(path).resolve() for path in staging["projectClasspath"]]
    allowed_outputs = {(ROOT / f"controlify/versions/{game}/{loader}/build/{kind}/controlifyQa").resolve()
                       for kind in ("classes/java", "resources")} if loader == "fabric" else set()
    assert len(outputs) == len(set(outputs)) and set(outputs) <= allowed_outputs, \
        f"{name}: project implementation outputs were present on the gameplay classpath"
    if loader == "fabric":
        assert (ROOT / f"controlify/versions/{game}/{loader}/build/classes/java/controlifyQa").resolve() in outputs, \
            f"{name}: missing isolated test-only QA output"

    originals, lineage = runtime.forge_lineage(folder, mods_folder, report_path, with_extension=True) \
        if loader == "forge" else ({}, None)
    preserved = {filename: originals.get(filename, path) for filename, path in staged.items()}
    required_files, upstream = set(), {}
    for role in ("controller", "yacl"):
        matches = [filename for filename, path in preserved.items() if runtime.digest(path, "sha512") == pin[role]["sha512"]]
        assert len(matches) == 1, f"{name}: original pinned {role} JAR absent, duplicated or altered"
        required_files.add(matches[0])
        upstream[role] = {"file": matches[0], "sha512": pin[role]["sha512"]}
    for dependency in pin.get("runtime_dependencies", []):
        filename = dependency["filename"]
        assert filename in staged and filename not in originals \
            and runtime.digest(staged[filename], "sha512") == dependency["sha512"], \
            f"{name}: required runtime shim must be staged unchanged"
        required_files.add(filename)
    if loader != "fabric":
        qa = f"controller-qa-{game}-{loader}.jar"
        assert qa in staged, f"{name}: missing isolated test-only QA JAR"
        with ZipFile(staged[qa]) as jar:
            assert jar.testzip() is None and any(member.endswith("/ControlifyGameplayHarness.class") for member in jar.namelist()), \
                f"{name}: invalid native test-only QA JAR"
        required_files.add(qa)
    mods = loaded_mods(report, name)
    assert mods.get("minecraft") == game and {"controlify", "yet_another_config_lib_v3", *SALT_MODULES.values()} <= mods.keys(), \
        f"{name}: matching native Minecraft, Core, Extended, companion or dependency did not load"
    version = mods["salts_inventory_update"]
    expected_controller = pin["controller"]["version"].split("-mc", 1)[0] if pin.get("native_forge_port") else pin["controller"]["version"]
    assert mods["controlify"] == expected_controller or mods["controlify"].startswith(expected_controller + "+"), \
        f"{name}: wrong native controller version"
    assert mods["yet_another_config_lib_v3"] == pin["yacl"]["version"], f"{name}: wrong native YACL version"
    for dependency in pin.get("runtime_dependencies", []):
        assert mods.get(dependency["mod_id"]) == dependency["version"], f"{name}: required runtime shim did not load"
    artifact_hashes, artifact_lineage, salt_files = {}, {}, set()
    for family, mod_id in SALT_MODULES.items():
        filename = f"{mod_id}-{game}-{runtime.LOADER_NAMES[loader]}-{mods[mod_id]}.jar"
        release = artifacts / family / filename
        assert release.is_file() and filename in staged, f"{name}: missing final or staged {family}"
        artifact_lineage[family] = runtime.artifact_match(family, release, preserved[filename])
        assert artifact_lineage[family]["mode"] == "exact", f"{name}: focused runs require exact final artifact bytes"
        if family in ('controlify', 'compatibility'):
            runtime.companion_versions(release, loader, version, mods[mod_id], mod_id)
        artifact_hashes[family] = runtime.digest(release)
        required_files.add(filename)
        salt_files.add(filename)
        with ZipFile(release) as jar:
            members = jar.namelist()
            assert len(members) == len(set(members)) and jar.testzip() is None, f"{name}: invalid final {family} archive"
            assert not any("/qa/" in member or "/functionaltest/" in member or member.endswith((".pdn", ".rej", ".pending.txt"))
                           for member in members), f"{name}: test/authoring files leaked into final {family}"
    assert required_files == set(staged), f"{name}: unexpected JAR outside exact production/upstream/QA inventory"
    if lineage:
        assert set(originals) == salt_files | {upstream["controller"]["file"], upstream["yacl"]["file"]}, \
            f"{name}: Forge remapped something outside the five original production/upstream JARs"
    source_files = {"coreCodeSource": next(filename for filename in salt_files if filename.startswith("salts_inventory_update-")),
                    "companionCodeSource": next(filename for filename in salt_files if filename.startswith("salts_inventory_controlify-")),
                    "controlifyCodeSource": upstream["controller"]["file"]}
    if "extensionCodeSource" in report:
        source_files["extensionCodeSource"] = next(filename for filename in salt_files if filename.startswith("salts_inventory_compat-"))
    code_sources = {filename: path.resolve() for filename, path in staged.items()}
    fabric_lineage = None
    if loader == "fabric" and game in LEGACY and any(code_source_path(report[field]) != code_sources[filename] for field, filename in source_files.items()):
        code_sources, fabric_lineage = fabric_named_lineage(profile, game, staged, mods, report_path)
    for field, filename in source_files.items():
        assert code_source_path(report[field]) == code_sources[filename], \
            f"{name}: {field} did not load its actual preserved profile JAR or exactly reproduced native derivative"

    captures = [row for row in records if row["status"] == "CAPTURED"]
    capture_names = [row["name"] for row in captures]
    assert captures and len(capture_names) == len(set(capture_names)) \
        and all(re.fullmatch(r"[A-Za-z0-9_.-]+", label) and label not in {".", ".."} for label in capture_names), \
        f"{name}: missing or ambiguous native screenshots"
    assert {path.stem for path in (folder / "screenshots").glob("*.png")} == set(capture_names), \
        f"{name}: screenshots do not match the recorded captures"
    images = []
    for capture in captures:
        path = folder / "screenshots" / (capture["name"] + ".png")
        assert path.resolve().parent == (folder / "screenshots").resolve() \
            and staging_path.stat().st_mtime_ns <= path.stat().st_mtime_ns <= report_path.stat().st_mtime_ns, \
            f"{name}: screenshot predates launch evidence or postdates its gameplay report"
        images.append(validate_png(path))
    result = {"minecraft": game, "loader": loader, "scope": scope, "failures": 0,
              "records": len(records), "passes": sum(row["status"] == "PASS" for row in records),
              "unique_checks": len(passed), "screenshots": len(images), "elapsed_ms": elapsed,
              "artifact_sha256": artifact_hashes, "artifact_lineage": artifact_lineage, "fabric_remap_lineage": fabric_lineage,
              "upstream": upstream, "captures": images, "runtime_remapping": lineage,
              "report": str(report_path.relative_to(ROOT)), "runtime_profile": str(profile.relative_to(ROOT)),
              "host_sdl_motion_isolation": isolation, "validated_scope": scope}
    print(f"Focused runtime verified: {name}; {len(passed)} distinct PASS checks, {len(images)} screenshots, exact final JARs")
    return result


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--report", action="append", required=True, type=Path)
    parser.add_argument("--artifacts", type=Path, default=ROOT / "dist")
    parser.add_argument("--summary", required=True, type=Path)
    parser.add_argument("--require-matrix", action="store_true", help="Require supplied focused reports to cover all twelve targets")
    args = parser.parse_args()
    pins = pinned_matrix()
    reports = [path.resolve() for path in args.report]
    assert len(reports) == len(set(reports)), "Duplicate --report inputs"
    rows = [validate(path, args.artifacts.resolve(), pins) for path in reports]
    identities = [(row["minecraft"], row["loader"], row["scope"]) for row in rows]
    assert all(count == 1 for count in Counter(identities).values()), "Duplicate target/scope evidence"
    if args.require_matrix:
        assert {(row["minecraft"], row["loader"]) for row in rows} == set(pins), \
            "Focused evidence does not cover all twelve supported targets"
    args.summary.parent.mkdir(parents=True, exist_ok=True)
    args.summary.write_text(json.dumps({"schema_version": 1, "status": "passed", "targets": rows}, indent=2) + "\n", encoding="utf-8")


if __name__ == "__main__":
    main()
