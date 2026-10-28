"""Run only affected native-window checks, one disposable game client at a time."""
from pathlib import Path
import argparse
import importlib.util
import json
import subprocess
import sys
import time

ROOT = Path(__file__).resolve().parents[2]
WORK = ROOT / "build/third-batch-ports"
spec = importlib.util.spec_from_file_location("port_checks", Path(__file__).with_name("run_cases.py"))
checks = importlib.util.module_from_spec(spec)
spec.loader.exec_module(checks)


def launch(mc, loader, run, cases=(), hold=False, manifest=None, transparent_probe=False):
    key = f"{mc}-{loader}-{run}"
    output = WORK / "gameplay" / key
    if output.exists():
        raise ValueError(f"Preserve old evidence: {output}")
    args = ["cmd", "/c", "gradlew.bat", "-I", "functional-tests/third-batch-ports/qa.init.gradle",
            f":{mc}:{loader}:runNativePortQa", f"-PqaMinecraft={mc}", f"-PqaLoader={loader}",
            "-PqaFullGameplay=true", "-PqaScenario=extension-empty", "-PqaIntegrationOnly=true",
            f"-PqaRunId={run}", f"-PcompatibilityQaManifest={manifest or f'build/third-batch-ports/fixture-{mc}-{loader}.json'}",
            "--console=plain"]
    if transparent_probe: args.append('-PqaTransparentProbe=true')
    log_path = WORK / ("runtime-" + key + ".log")
    with log_path.open("w", encoding="utf-8") as log:
        process = subprocess.Popen(args, cwd=ROOT, stdout=log, stderr=subprocess.STDOUT)
        try:
            deadline = time.monotonic() + 360
            while not (output / "ready.json").exists():
                if process.poll() is not None:
                    raise RuntimeError(f"Launch exited {process.returncode}: {log_path}")
                if time.monotonic() > deadline:
                    raise TimeoutError(f"No ready marker: {log_path}")
                time.sleep(1)
            results = checks.check(output, cases)
            if hold:
                print('READY FOR INTERACTION', output, flush=True)
                while not (output / 'finish-requested').exists():
                    if process.poll() is not None:
                        raise RuntimeError(f'Client exited while held: {log_path}')
                    time.sleep(1)
            checks.sender.send(output, "finish")
            if process.wait(timeout=100) != 0:
                raise RuntimeError(f"Game checks passed but launch failed: {log_path}")
            return {"minecraft": mc, "loader": loader, "run": run, "opening_observations": True,
                    "assertions": len(results), "output": str(output.relative_to(ROOT))}
        finally:
            if process.poll() is None:
                try:
                    if (output / "ready.json").exists():
                        checks.sender.send(output, "finish")
                        process.wait(timeout=35)
                except Exception:
                    pass
            if process.poll() is None:
                # Only descendants of this disposable launch are terminated.
                subprocess.run(["taskkill", "/PID", str(process.pid), "/T", "/F"],
                               stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, check=False)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--run", required=True)
    parser.add_argument("--target", action="append", help="Exact Minecraft/loader; repeat to select a subset")
    parser.add_argument("--case", action="append", default=[], help="Focus a follow-up on named affected interfaces")
    parser.add_argument("--hold", action="store_true", help="Keep the client open for focused follow-up commands")
    parser.add_argument("--manifest", help="Use a separately resolved cohort fixture")
    parser.add_argument("--transparent-probe", action="store_true", help="Trace native background capture without altering rendering")
    args = parser.parse_args()
    targets = args.target or [f"{v}/{loader}" for v in ("1.21.1", "1.21.11", "26.1.2", "26.2", "26.3")
                              for loader in ("fabric", "neoforge")]
    rows = []
    for target in targets:
        print("START", target, flush=True)
        mc, loader = target.split("/")
        rows.append(launch(mc, loader, args.run, args.case, args.hold, args.manifest, args.transparent_probe))
        print("OBSERVED", target, rows[-1]["assertions"], flush=True)
        (WORK / ("matrix-" + args.run + ".json")).write_text(json.dumps(rows, indent=2), encoding="utf-8")
