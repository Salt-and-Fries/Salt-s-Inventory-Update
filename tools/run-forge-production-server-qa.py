"""Boot original 1.20.1 Forge release JARs with the official production server."""
import argparse
import hashlib
import importlib.util
import json
import os
from pathlib import Path
import re
import shutil
import socket
import subprocess
import sys
import time
import urllib.request

ROOT = Path(__file__).resolve().parents[1]
MC, FORGE = "1.20.1", "47.4.0"
INSTALL = ROOT / f"build/release-server-production/forge-{MC}-{FORGE}"
INSTALLER_SHA1 = "b73203963ea70aee0c38dc58de1f6d16a766605b"
spec = importlib.util.spec_from_file_location("rcon", ROOT / "tools/verify-spatial-server.py")
rcon_module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(rcon_module)


def digest(path, algorithm="sha256"):
    return hashlib.new(algorithm, path.read_bytes()).hexdigest()


def java17():
    candidates = []
    if os.environ.get("JAVA17_HOME"):
        candidates.append(Path(os.environ["JAVA17_HOME"]) / "bin/java.exe")
    candidates += sorted((Path.home() / ".gradle/jdks").glob("*17*/bin/java.exe"))
    candidates += sorted((Path.home() / ".jdks").glob("*17*/bin/java.exe"))
    for candidate in candidates:
        if candidate.is_file():
            version = subprocess.run([str(candidate), "-version"], capture_output=True, text=True)
            if re.search(r'version "17[.]', version.stderr):
                return candidate
    raise RuntimeError("A Java 17 toolchain is required for this production Forge fixture")


def ensure_installation(java):
    INSTALL.mkdir(parents=True, exist_ok=True)
    installer = INSTALL / f"forge-{MC}-{FORGE}-installer.jar"
    if not installer.exists():
        url = f"https://maven.minecraftforge.net/net/minecraftforge/forge/{MC}-{FORGE}/{installer.name}"
        request = urllib.request.Request(url, headers={"User-Agent": "Gradle/9.7.0"})
        with urllib.request.urlopen(request, timeout=120) as response:
            installer.write_bytes(response.read())
    assert digest(installer, "sha1") == INSTALLER_SHA1, "Official installer checksum mismatch"
    arguments = INSTALL / f"libraries/net/minecraftforge/forge/{MC}-{FORGE}/win_args.txt"
    if not arguments.exists():
        with (INSTALL / "install.log").open("w", encoding="utf-8") as stream:
            subprocess.run([str(java), "-jar", str(installer), "--installServer", "."],
                           cwd=INSTALL, stdout=stream, stderr=subprocess.STDOUT,
                           creationflags=subprocess.CREATE_NO_WINDOW, timeout=900, check=True)
    text = arguments.read_text()
    assert "--launchTarget forgeserver" in text and "userdev" not in text
    return installer, arguments


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--scenario", choices=["core", "extension-empty", "ae2", "toms", "sophisticated"], required=True)
    parser.add_argument("--run-id", required=True)
    parser.add_argument("--accessory", choices=["trinkets", "curios"])
    parser.add_argument("--upstream-only", action="store_true")
    args = parser.parse_args()
    assert re.fullmatch("[a-z0-9-]+", args.run_id)
    assert not args.upstream_only or args.scenario != "core"
    with socket.socket() as probe:
        probe.bind(("127.0.0.1", 25589))
    output = ROOT / f"build/gameplay-qa/packaged-native/{MC}-forge-{args.run_id}"
    output.mkdir(parents=True, exist_ok=True)
    assert not (output / "server-results.json").exists(), "Preserve old results; use a fresh run ID"
    fixture_path = output / "upstream-fixtures.json"
    prepare = [sys.executable, str(ROOT / "tools/prepare-compatibility-qa.py"),
               "--minecraft", MC, "--loader", "forge", "--scenario", args.scenario,
               "--output", str(fixture_path)]
    if args.accessory:
        prepare += ["--extra-project", args.accessory]
    subprocess.run(prepare, cwd=ROOT, check=True)
    fixture = json.loads(fixture_path.read_text())
    java = java17()
    installer, arguments = ensure_installation(java)
    inputs = []
    if not args.upstream_only:
        for family in ["core"] + ([] if args.scenario == "core" else ["compatibility"]):
            candidates = list((ROOT / "dist" / family).glob(f"*-{MC}-Forge-*.jar"))
            assert len(candidates) == 1
            inputs.append((family, candidates[0], candidates[0].name))
    for entry in fixture["artifacts"]:
        if entry.get("coordinate"):
            path = ROOT / entry["path"]
            assert digest(path, "sha512") == entry["sha512"]
            assert Path(entry["filename"]).name == entry["filename"]
            inputs.append(("upstream", path, entry["filename"]))
    mods = (INSTALL / "mods").resolve()
    assert mods.is_relative_to(ROOT) and mods.parent == INSTALL.resolve()
    mods.mkdir(exist_ok=True)
    ledger = INSTALL / "owned-mods.json"
    owned = set(json.loads(ledger.read_text())) if ledger.exists() else set()
    existing = list(mods.glob("*.jar"))
    assert {p.name for p in existing} <= owned, "Unrecognized files in the isolated fixture mods directory"
    for path in existing:
        assert path.resolve().parent == mods and not path.is_symlink()
        path.unlink()
    preserved = output / "staged-mods"
    preserved.mkdir(exist_ok=True)
    inventory = []
    for family, source, filename in inputs:
        assert filename.endswith(".jar") and Path(filename).name == filename
        archived = preserved / filename
        shutil.copy2(source, archived)
        shutil.copy2(archived, mods / filename)
        assert digest(source) == digest(archived) == digest(mods / filename)
        inventory.append(dict(family=family, source=str(source), filename=filename,
                              sha256=digest(source), sha512=digest(source, "sha512")))
    ledger.write_text(json.dumps([entry["filename"] for entry in inventory], indent=2) + "\n")
    (output / "staged-artifacts.json").write_text(json.dumps(dict(
        minecraft=MC, loader="forge", forge_version=FORGE, runtime="production",
        upstream_only=args.upstream_only, java=str(java), installer_sha1=digest(installer, "sha1"),
        argument_file=str(arguments), argument_file_sha256=digest(arguments), artifacts=inventory), indent=2) + "\n")
    (INSTALL / "eula.txt").write_text("eula=true\n")
    flat = json.dumps(dict(layers=[dict(height=1, block="minecraft:bedrock"),
                                  dict(height=2, block="minecraft:dirt"),
                                  dict(height=1, block="minecraft:grass_block")], biome="minecraft:plains"))
    (INSTALL / "server.properties").write_text(
        "server-ip=127.0.0.1\nserver-port=0\nonline-mode=false\nview-distance=2\nsimulation-distance=3\n"
        f"level-name=worlds/{args.run_id}\nlevel-type=minecraft:flat\ngenerator-settings={flat}\n"
        "generate-structures=false\nenable-rcon=true\nrcon.port=25589\n"
        "rcon.password=release-local-qa-only\nbroadcast-rcon-to-ops=false\n")
    environment = {k: v for k, v in os.environ.items() if not k.startswith("SIU_") and k != "MOD_CLASSES"}
    command = [str(java), "-Xms512m", "-Xmx3g", "-Dforge.logging.console.level=debug",
               "-Dsalts_inventory_update.desktopDebug=true",
               "@" + str(INSTALL / "user_jvm_args.txt"), "@" + str(arguments), "nogui"]
    (output / "launch-command.json").write_text(json.dumps(command, indent=2) + "\n")
    log_path = output / "launch.log"
    with log_path.open("w", encoding="utf-8") as stream:
        child = subprocess.Popen(command, cwd=INSTALL, env=environment, stdin=subprocess.DEVNULL,
                                 stdout=stream, stderr=subprocess.STDOUT,
                                 creationflags=subprocess.CREATE_NO_WINDOW)
        try:
            deadline = time.monotonic() + 600
            while True:
                text = log_path.read_text(encoding="utf-8", errors="replace")
                if re.search(r"Done \([^)]+\)!", text):
                    break
                if child.poll() is not None:
                    raise RuntimeError(f"Production server exited before ready (exit {child.returncode}): {output}")
                if time.monotonic() > deadline:
                    raise TimeoutError(f"Production server readiness: {output}")
                time.sleep(1)
            remote = rcon_module.Rcon()
            remote.port, remote.password, remote.command_timeout = 25589, "release-local-qa-only", 120
            with remote as rcon:
                commands = {name: rcon.command(value) for name, value in
                            [("list", "list"), ("save", "save-all flush"), ("stop", "stop")]}
            assert child.wait(timeout=180) == 0, "Production server failed during shutdown"
            final = log_path.read_text(encoding="utf-8", errors="replace")
            selected_launch = next(line for line in final.splitlines() if "ModLauncher running: args" in line)
            assert "userdev" not in selected_launch and re.search(r"launchTarget[, ]+forgeserver\b", selected_launch)
            assert "Current naming domain is 'srg'" in final
            assert "Failed to start the minecraft server" not in final
            assert "Attempted to load class" not in final
            assert "Unable to initialize optional integration" not in final
            assert "Mixin apply failed salts_inventory" not in final
            assert not re.search(r"Couldn.t load (mod/)?salts_inventory_(update|compat|controlify) pack metadata", final)
            assert "Exception in server tick loop" not in final
            if not args.upstream_only:
                assert "Salt's Inventory Update initialized for Forge 1.20.1" in final
            if args.scenario == "ae2" and not args.upstream_only:
                assert re.search(r"Verified \d+ (?:native )?AE2 desktop hooks", final)
            if args.scenario == "sophisticated" and not args.upstream_only:
                assert "Sophisticated desktop integration initialized client=false backpacks=true storage=true" in final
            assert {p.name for p in mods.glob("*.jar")} == {entry["filename"] for entry in inventory}
            for entry in inventory:
                assert digest(mods / entry["filename"]) == entry["sha256"]
            (output / "server-results.json").write_text(json.dumps(dict(
                passed=True, minecraft=MC, loader="forge", forge_version=FORGE, runtime="production",
                scenario=args.scenario, accessory=args.accessory, upstream_only=args.upstream_only,
                mods=inventory, commands=commands, exit_code=child.returncode), indent=2) + "\n")
            print("PRODUCTION FORGE SERVER PASS", args.scenario, "upstream_only=" + str(args.upstream_only), output)
        finally:
            if child.poll() is None:
                try:
                    remote = rcon_module.Rcon()
                    remote.port, remote.password, remote.command_timeout = 25589, "release-local-qa-only", 120
                    with remote as rcon:
                        rcon.command("stop")
                    child.wait(timeout=60)
                except Exception:
                    child.terminate()
                    child.wait(timeout=30)
            for filename in ("latest.log", "debug.log"):
                source = INSTALL / "logs" / filename
                if source.exists():
                    shutil.copy2(source, output / filename)


if __name__ == "__main__":
    main()
