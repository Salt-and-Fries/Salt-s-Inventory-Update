"""Verify focused chest gameplay reports against the exact distributed Core inputs."""
import argparse
import hashlib
import json
from pathlib import Path
import struct
from artifact_lineage import core_description_match

ROOT = Path(__file__).resolve().parents[1]


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def verify(folder, minecraft, loader, dist, allow_description_correction=False):
    report = json.loads((folder / "results.json").read_text(encoding="utf-8-sig"))
    staging = json.loads((folder / "staged-artifacts.json").read_text(encoding="utf-8-sig"))
    assert report["minecraft"] == minecraft and report["loader"] == loader
    assert report["scope"] == staging["scope"] == "chest-close-issue-15"
    failures = [result for result in report["results"] if result["status"] == "FAIL"]
    assert report["failures"] == 0 and not failures, (folder, failures)
    assert report["loadedMods"]["salts_inventory_update"] == "0.1.5"
    assert "salts_inventory_compat" not in report["loadedMods"]
    assert "salts_inventory_controlify" not in report["loadedMods"]
    assert ".jar" in report["coreCodeSource"], "Core must load from an actual production mod"
    artifact = dist / "core" / Path(staging["artifact"]).name
    assert artifact.is_file(), artifact
    run = Path(report["runDirectory"])
    if loader == "forge":
        lineage = json.loads((folder / "forge-remap-lineage.json").read_text(encoding="utf-8-sig"))
        assert lineage["operation"] == "SRG_TO_MCP" and len(lineage["artifacts"]) == 1
        original = lineage["artifacts"][0]
        assert original["original_sha256"] == staging["sha256"] == digest(Path(original["original"]))
        assert original["mapped_sha256"] == digest(Path(original["mapped"]))
        tested_artifact = Path(original["original"])
    else:
        tested_artifact = run / "mods" / artifact.name
        assert digest(tested_artifact) == staging["sha256"]
    if digest(artifact) == staging["sha256"]:
        match = {"mode": "exact", "runtime_sha256": staging["sha256"], "final_sha256": digest(artifact)}
    else:
        assert allow_description_correction, f"Changed shipping input: {artifact}"
        match = core_description_match(artifact, tested_artifact)
    passed = {result["name"] for result in report["results"] if result["status"] == "PASS"}
    required = {"integrated-server-connected", "authenticated-desktop-ready", "config-keeps-persistent-windows",
        "config-removes-obsolete-controls", "native-opener-hide-restore-final-close-balanced"}
    required.update("removed-api-" + name for name in ("DesktopWindowContext", "DesktopWindowDefinition", "DesktopWindowState", "DesktopWindowOptions"))
    required.update("removed-setting-" + name for name in ("enableGhostPins", "minimizableWindows", "ghostWindowOpacity"))
    for state in ("ordinary", "persistent"):
        for gesture in ("hold-e", "hold-escape", "tap-escape"):
            prefix = f"chest-{state}-{gesture}-"
            required.update(prefix + check for check in ("native-openers-before", "native-lid-before", "desktop-dismissed",
                "native-openers-zero", "native-lid-closed", "contents-preserved"))
            if gesture.startswith("hold"):
                required.add(prefix + "closed-before-key-release")
            if state == "persistent" and gesture != "hold-escape":
                required.update(prefix + check for check in ("window-restored", "restored-native-viewer",
                    "restored-native-lid", "restored-contents-preserved"))
    assert required <= passed, (folder, "Missing checks", sorted(required - passed))
    captures = list((folder / "screenshots").glob("*.png"))
    assert len(captures) >= 15
    for capture in captures:
        data = capture.read_bytes()
        assert data[:8] == b"\x89PNG\r\n\x1a\n" and min(struct.unpack_from(">II", data, 16)) > 0
    return {"minecraft": minecraft, "loader": loader, "status": "PASS", "coreVersion": "0.1.5", "sha256": digest(artifact),
        "runtimeSha256": staging["sha256"],
        "artifact": str(artifact), "report": str(folder / "results.json"), "scenarios": 6,
        "records": len(report["results"]), "requiredChecks": len(required), "screenshots": len(captures),
        "coreCodeSource": report["coreCodeSource"], "artifactLineage": match}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--run-id", default="api6-final1")
    parser.add_argument("--target-run-id", action="append", default=[], metavar="MINECRAFT:LOADER=RUN_ID",
        help="Use a later focused rerun for a specific target, retaining other targets' evidence.")
    parser.add_argument("--reports", type=Path, default=ROOT / "build/core-window-cleanup/gameplay")
    parser.add_argument("--dist", type=Path, default=ROOT / "dist")
    parser.add_argument("--summary", type=Path)
    parser.add_argument("--allow-partial", action="store_true")
    parser.add_argument("--allow-description-correction", action="store_true",
        help="Permit only the verified metadata correction removing the obsolete ghost-pin claim.")
    args = parser.parse_args()
    matrix = json.loads((ROOT / "controlify/targets.json").read_text(encoding="utf-8-sig"))["targets"]
    overrides = {}
    targets = {f"{target['minecraft']}:{target['loader']}" for target in matrix}
    for override in args.target_run_id:
        target, run_id = override.split("=", 1)
        assert target in targets and target not in overrides and run_id and all(
            character in "abcdefghijklmnopqrstuvwxyz0123456789-" for character in run_id), override
        overrides[target] = run_id
    verified, pending = [], []
    for target in matrix:
        minecraft, loader = target["minecraft"], target["loader"]
        run_id = overrides.get(f"{minecraft}:{loader}", args.run_id)
        folder = args.reports / f"{minecraft}-{loader}-{run_id}"
        if not (folder / "results.json").is_file():
            pending.append(f"{minecraft}:{loader}")
            continue
        verified.append(verify(folder, minecraft, loader, args.dist, args.allow_description_correction))
    if not args.allow_partial:
        assert not pending, f"Incomplete focused matrix: {pending}"
    report = {"status": "PENDING" if pending else "PASS", "apiVersion": 6, "profiles": verified, "pending": pending,
        "scenarios": sum(result["scenarios"] for result in verified),
        "records": sum(result["records"] for result in verified),
        "screenshots": sum(result["screenshots"] for result in verified)}
    if args.summary:
        args.summary.parent.mkdir(parents=True, exist_ok=True)
        args.summary.write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
    print(f"{report['status']}: verified {len(verified)}/{len(matrix)} Core gameplay profiles; "
        f"{report['scenarios']} chest scenarios; {report['screenshots']} native screenshots.")


if __name__ == "__main__":
    main()
