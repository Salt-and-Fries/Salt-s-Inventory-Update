"""Focused desktop/native switching smoke test, serial with other game runs."""
import argparse
import json
import msvcrt
import os
from pathlib import Path
import subprocess

ROOT = Path(__file__).resolve().parents[1]
parser = argparse.ArgumentParser()
parser.add_argument("--loader", choices=("fabric", "forge", "neoforge"), required=True)
parser.add_argument("--minecraft", choices=("1.20.1","1.21.1","1.21.11","26.1.2","26.2","26.3"), default="26.3")
parser.add_argument("--run", required=True)
parser.add_argument("--player-inventory-only", action="store_true")
args = parser.parse_args()
if not args.run.replace("-", "").isalnum() or args.run.lower() != args.run:
    raise SystemExit("Choose a lowercase run ID")
output = ROOT / "output/native-view" / args.run
output.mkdir(parents=True, exist_ok=False)
if args.loader not in ("fabric", "forge" if args.minecraft == "1.20.1" else "neoforge"):
    raise SystemExit("Unsupported Minecraft/loader pair")
label = {"fabric":"Fabric", "forge":"Forge", "neoforge":"NeoForge"}[args.loader]
artifact = f"versions/{args.minecraft}/{args.loader}/build/libs/salts_inventory_update-{args.minecraft}-{label}-0.1.6.jar"
env = dict(os.environ, JAVA_HOME="C:/Program Files/Java/jdk-25.0.3", SIU_QA_NATIVE_VIEW="true")
if args.player_inventory_only:
    env["SIU_QA_NATIVE_VIEW_PLAYER_ONLY"] = "true"
command = [str(ROOT / "gradlew.bat"), "-I", "functional-tests/chest-close-qa.init.gradle",
           "-PqaWindowControlsOnly=true", f"-PqaMinecraft={args.minecraft}", f"-PqaLoader={args.loader}",
           f"-PqaRunId={args.run}", f"-PqaCoreArtifact={artifact}", "-PsaltsSmokeRun=true",
           f"-PsaltsSmokeProfile={args.run}", "-PincludeFunctionalTests=false", "--console=plain",
           f":{args.minecraft}:{args.loader}:runWindowControlsQa"]
with (ROOT / "build/project-everything-regression/runtime.lock").open("a+b") as lock:
    lock.seek(0); lock.write(b"0"); lock.flush(); lock.seek(0)
    msvcrt.locking(lock.fileno(), msvcrt.LK_NBLCK, 1)
    try:
        with (output / "launch.log").open("w", encoding="utf-8") as log:
            result = subprocess.run(command, cwd=ROOT, env=env, stdout=log, stderr=subprocess.STDOUT)
        report_path = ROOT / f"build/window-controls/gameplay/{args.minecraft}-{args.loader}-{args.run}/results.json"
        summary = {"scope": f"{args.minecraft}-native-player-inventory-periodic-sync" if args.player_inventory_only else f"{args.minecraft}-native-view-and-top-tab", "minecraft": args.minecraft, "loader": args.loader,
                   "exit_code": result.returncode, "report": str(report_path)}
        if report_path.exists():
            report = json.loads(report_path.read_text(encoding="utf-8"))
            summary["failures"] = report["failures"]
            summary["records"] = len(report.get("results", []))
        (output / "summary.json").write_text(json.dumps(summary, indent=2) + "\n")
        print(json.dumps(summary))
        raise SystemExit(result.returncode)
    finally:
        lock.seek(0); msvcrt.locking(lock.fileno(), msvcrt.LK_UNLCK, 1)
