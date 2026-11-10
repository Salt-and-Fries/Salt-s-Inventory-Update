"""Run only the native-view/dropdown scenario, serially, against built Core jars."""
import argparse
import json
import msvcrt
import os
from pathlib import Path
import subprocess

ROOT = Path(__file__).resolve().parents[1]
TARGETS = [(v, loader) for v in ("1.20.1", "1.21.1", "1.21.11", "26.1.2", "26.2", "26.3")
           for loader in ("fabric", "forge" if v == "1.20.1" else "neoforge")]
parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument("--run", required=True)
parser.add_argument("--build-compatibility", action="store_true")
parser.add_argument("--targets", nargs="+", choices=[f"{v}-{loader}" for v, loader in TARGETS])
args = parser.parse_args()
if not args.run.replace("-", "").isalnum() or args.run.lower() != args.run:
    raise SystemExit("Choose a lowercase run ID")
selected = [(v, loader) for v, loader in TARGETS
            if not args.targets or f"{v}-{loader}" in args.targets]
out = ROOT / "output/native-view-all-targets" / args.run
out.mkdir(parents=True, exist_ok=False)
env = dict(os.environ, JAVA_HOME="C:/Program Files/Java/jdk-25.0.3", SIU_QA_NATIVE_VIEW="true")
command = [str(ROOT / "gradlew.bat"), "-I", "functional-tests/chest-close-qa.init.gradle",
           "-PqaMatrix=true", "-PqaWindowControlsOnly=true", "-PqaUseBuiltCore=true",
           f"-PqaRunId={args.run}", "-PsaltsSmokeRun=true", f"-PsaltsSmokeProfile={args.run}",
           "-PincludeFunctionalTests=false", "--no-parallel", "--max-workers=2", "--continue", "--console=plain"]
if args.build_compatibility:
    command += [f":compatibility:{v}:{loader}:assemble" for v, loader in TARGETS
                if (v == "1.20.1" or loader == "neoforge" and v != "1.21.11")]
command += [f":{v}:{loader}:runWindowControlsQa" for v, loader in selected]
with (ROOT / "build/project-everything-regression/runtime.lock").open("a+b") as lock:
    lock.seek(0)
    lock.write(b"0")
    lock.flush()
    lock.seek(0)
    msvcrt.locking(lock.fileno(), msvcrt.LK_NBLCK, 1)
    try:
        with (out / "launch.log").open("w", encoding="utf-8") as log:
            run = subprocess.run(command, cwd=ROOT, env=env, stdout=log, stderr=subprocess.STDOUT)
    finally:
        lock.seek(0)
        msvcrt.locking(lock.fileno(), msvcrt.LK_UNLCK, 1)
summary = {"scope": "native-view-and-top-dropdown-only", "exit_code": run.returncode, "targets": []}
for version, loader in selected:
    report = ROOT / f"build/window-controls/gameplay/{version}-{loader}-{args.run}/results.json"
    result = {"minecraft": version, "loader": loader, "report": str(report), "status": "missing-report"}
    if report.exists():
        data = json.loads(report.read_text(encoding="utf-8"))
        result.update(failures=data["failures"], records=len(data.get("results", [])),
                      status="PASS" if data["failures"] == 0 else "FAIL")
    summary["targets"].append(result)
(out / "summary.json").write_text(json.dumps(summary, indent=2) + "\n")
print(json.dumps(summary))
raise SystemExit(run.returncode or (0 if all(t["status"] == "PASS" for t in summary["targets"]) else 1))
