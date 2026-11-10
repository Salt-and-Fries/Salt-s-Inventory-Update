"""Verify screenshot provenance and account for every remaining audit entry.

This is a coverage ledger, not a functional-test certification. A captured
interface never implies that every state or every other interface is covered.
"""
from __future__ import annotations

import collections
import csv
import hashlib
import io
import json
from pathlib import Path
import zipfile

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "output/remaining-ui-audit"


def read(path):
    return json.loads(path.read_text(encoding="utf-8-sig"))


def resolve(path):
    result = Path(path)
    return result if result.is_absolute() else ROOT / result


def main():
    inventory = read(OUT / "inventory.json")
    entries = {entry["entry"]: entry for entry in inventory["entries"]}
    reviews_path = OUT / "entry-reviews.json"
    reviews = {row["entry"]: row for row in read(reviews_path)["entries"]} if reviews_path.exists() else {}
    workflow_path = OUT / "additional-inventory-workflows.json"
    workflows = {row["entry"]:row for row in read(workflow_path)["entries"]} if workflow_path.exists() else {}
    release_hashes = {}
    nested_hashes = {}
    def staged_source_hashes(stage):
        result = set()
        def descend(data, label):
            digest = hashlib.sha256(data).hexdigest()
            if digest in nested_hashes:
                return nested_hashes[digest]
            hashes = {digest}
            with zipfile.ZipFile(io.BytesIO(data)) as archive:
                names = set(archive.namelist())
                members = []
                if "fabric.mod.json" in names:
                    meta = json.loads(archive.read("fabric.mod.json"), strict=False)
                    members += [row["file"] for row in meta.get("jars", [])]
                if "META-INF/jarjar/metadata.json" in names:
                    meta = json.loads(archive.read("META-INF/jarjar/metadata.json"))
                    members += [row["path"] for row in meta.get("jars", [])]
                for member in set(members):
                    hashes.update(descend(archive.read(member), label + "!/" + member))
            nested_hashes[digest] = hashes
            return hashes
        for artifact in stage["artifacts"]:
            if artifact.get("family") in ("core", "qa"):
                continue
            digest = artifact["sha256"]
            result.add(digest)
            if digest in nested_hashes:
                result.update(nested_hashes[digest])
                continue
            path = artifact.get("file") or artifact.get("source") or artifact.get("path")
            if not path or not resolve(path).is_file():
                continue
            data = resolve(path).read_bytes()
            if hashlib.sha256(data).hexdigest() != digest:
                raise ValueError("Staged provenance source has changed: " + path)
            result.update(descend(data, path))
        return result
    def original_hashes(entry):
        result = set()
        for release in entry["releases"]:
            path = release.get("path")
            if not path:
                continue
            if path not in release_hashes:
                release_hashes[path] = hashlib.sha256(resolve(path).read_bytes()).hexdigest()
            result.add(release_hashes[path])
        return result
    cases, checks, observed, seen = [], [], collections.defaultdict(list), set()
    staged_screen_candidates = collections.defaultdict(set)
    observed_releases = collections.defaultdict(dict)
    for source in sorted(OUT.glob("*-pairs.json")):
        for pair in read(source)["cases"]:
            key = pair["minecraft"], pair["loader"], pair["id"]
            if key in seen:
                raise ValueError("Duplicate case: " + str(key))
            seen.add(key)
            entry = entries[pair["entry"]]
            if entry["delivered"]:
                raise ValueError("Completed integration included in remaining comparison: " + entry["title"])
            stage_sets = {}
            capture_sides = []
            for side in ("native", "core"):
                panel = pair[side]
                if panel.get("capture"):
                    capture = resolve(panel["capture"])
                    actual = hashlib.sha256(capture.read_bytes()).hexdigest()
                    if actual != panel.get("sha256"):
                        raise ValueError("Screenshot changed: " + str(capture))
                    capture_sides.append(side)
                    stage_path = capture.parent.parent / "staged-artifacts.json"
                elif panel.get("staged_artifacts"):
                    stage_path = resolve(panel["staged_artifacts"])
                elif panel.get("runtime_output"):
                    stage_path = resolve(panel["runtime_output"]) / "staged-artifacts.json"
                else:
                    raise ValueError("Missing capture needs explicit runtime provenance: " + pair["id"] + " / " + side)
                stage = read(stage_path)
                if (stage["minecraft"],stage["loader"]) != (pair["minecraft"],pair["loader"]):
                    raise ValueError("Runtime target mismatch: " + str(stage_path))
                stage_sets[side] = {a["sha256"] for a in stage["artifacts"]
                                    if a.get("family") not in ("core", "qa")
                                    and not a["filename"].startswith(("salts_inventory_update-", "chest-close-qa-", "native-ui-reference"))}
            if stage_sets["native"] != stage_sets["core"]:
                raise ValueError("Paired upstream/dependency hashes differ: " + pair["id"])
            if not capture_sides:
                raise ValueError("Comparison needs at least one genuine screenshot: " + pair["id"])
            if len(capture_sides) != 2 and pair["outcome"] in ("automatic", "builtin", "native preserved"):
                raise ValueError("Missing screenshot cannot be a successful paired outcome: " + pair["id"])
            if pair["outcome"] in ("automatic", "builtin", "native preserved"):
                for side in ("native", "core"):
                    screen = pair[side].get("screen") or "none"
                    if screen == "none" or any(name in screen for name in ("DeathScreen", "TitleScreen", "DisconnectedScreen")):
                        raise ValueError("Non-target screen cannot be a successful paired outcome: " + pair["id"] + ": " + screen)
            source_hashes = staged_source_hashes(stage)
            if not original_hashes(entry).intersection(source_hashes):
                raise ValueError("Screenshot assigned to an audit entry whose release is not staged: " + pair["id"])
            def record_release(number):
                for release in entries[number]["releases"]:
                    if release_hashes.get(release.get("path")) not in source_hashes:
                        continue
                    observed_releases[number][release["path"]] = {
                        key:release.get(key) for key in ("version", "path", "minecraft", "loaders")}
                    staged_screen_candidates[number].update(c["class"] for c in release["ui_candidates"]
                                                            if c["kind"] == "container-screen")
            record_release(pair["entry"])
            for window in pair["core"].get("windows", []):
                definition = window.get("definition", "")
                if definition.endswith("NativeTemplateWindow") or ".compat." in definition:
                    raise ValueError("Custom adapter contaminates baseline: " + pair["id"])
            if pair["native"].get("menu_class") != pair["core"].get("menu_class"):
                # Retain a failed/different opening as an audit finding, not a
                # successful same-interface conversion.
                checks.append({"id":pair["id"],"check":"same-native-menu-class","passed":False,
                               "native":pair["native"].get("menu_class"),"core":pair["core"].get("menu_class")})
            else:
                checks.append({"id":pair["id"],"check":"same-native-menu-class","passed":True})
            cases.append({**pair,"title":entry["title"],"source_manifest":str(source.relative_to(ROOT)),
                          "screenshot_hashes_verified":len(capture_sides) == 2,
                          "verified_screenshot_sides":capture_sides,"identical_upstream_artifacts_verified":True,
                          "original_audit_release_binary_verified":True})
            observed[pair["entry"]].append(pair)
            for number in pair.get("related_entries", []):
                if number not in entries or entries[number]["delivered"]:
                    raise ValueError("Invalid related audit entry: " + str(number))
                if not original_hashes(entries[number]).intersection(source_hashes):
                    raise ValueError("Related entry release is not staged: " + str(number))
                record_release(number)
                # A shared screen can demonstrate an add-on's changed workflow,
                # but it must be declared explicitly, not inferred from a
                # matching screen class alone across different releases.
                observed[number].append(pair)
    ledger = []
    for number, entry in entries.items():
        if entry["delivered"]:
            continue
        captures = observed[number]
        candidate = bool(entry["container_screens"] or entry["menus"] or number in workflows)
        screens = {(p["native"].get("screen") or "").replace(".","/") for p in captures} - {""}
        menus = {(p["native"].get("menu_class") or "").replace(".","/") for p in captures} - {""}
        row = {"entry":number,"title":entry["title"],"audit_kind":entry["audit_kind"],
               "source_url":entry["source_url"],"pairs":len(captures),
               "outcomes":dict(collections.Counter(p["outcome"] for p in captures)),
               "observed_screen_classes":sorted(screens),"observed_menu_classes":sorted(menus),
               "unobserved_class_candidates":sorted(set(entry["container_screens"]) - screens),
               "class_inventory_is_not_proof_of_reachable_ui":True}
        review = reviews.get(number, {})
        row["source_review"] = review
        row["additional_inventory_workflows"] = workflows.get(number, {}).get("workflows", [])
        observed_workflows = {p["workflow_id"] for p in captures if p.get("workflow_id")}
        row["unobserved_inventory_workflows"] = [w for w in row["additional_inventory_workflows"] if w["id"] not in observed_workflows]
        dispositions = review.get("class_dispositions", [])
        resolved = {item["class"] for item in dispositions
                    if item.get("disposition") in ("shared infrastructure", "outside container scope", "no independent visible UI", "not active in audited cohort", "conditional variant not selected")}
        row["unresolved_class_candidates"] = sorted(set(row["unobserved_class_candidates"]) - resolved)
        row["observed_original_releases"] = list(observed_releases[number].values())
        row["unobserved_screen_candidates_in_staged_releases"] = sorted(staged_screen_candidates[number] - screens - resolved)
        row["other_retained_release_screen_candidates"] = sorted(set(entry["container_screens"]) - staged_screen_candidates[number]) if captures else []
        if captures:
            row["status"] = "captures-recorded; whole-mod coverage not certified"
            row["appendix_reason"] = "See comparison pages and per-interface findings. Captures are visual baselines only."
        elif candidate:
            row["status"] = "container candidate; capture pending"
            row["appendix_reason"] = "Retained binary has container/menu classes or reviewed vanilla/shared inventory openers. Runtime reachability and captures remain pending."
        else:
            row["status"] = "outside requested container scope by retained audit"
            row["appendix_reason"] = ("The retained class inventory found settings, standalone, HUD, tooltip, recipe or shared-library surfaces rather than an owned container candidate. "
                                       "No absence-of-UI or compatibility claim is made.")
        if review.get("appendix_reason"):
            row["appendix_reason"] += " " + review["appendix_reason"]
        if not captures and review.get("status_override"):
            row["status"] = review["status_override"]
            if review.get("appendix_reason"):
                row["appendix_reason"] = review["appendix_reason"]
        ledger.append(row)
    assert len(ledger) == 618
    summary = {"original_audit_entries":647,"original_report_pages":199,"excluded_delivered_audit_rows":29,
               "remaining_entries_accounted_for":len(ledger),"verified_screenshot_pairs":sum(c["screenshot_hashes_verified"] for c in cases),
               "recorded_comparisons":len(cases),"comparisons_with_missing_screenshot":sum(not c["screenshot_hashes_verified"] for c in cases),
               "reviewed_vanilla_or_shared_inventory_entries":len(workflows),
               "entries_with_pairs":sum(bool(value) for value in observed.values()),"outcomes":dict(collections.Counter(p["outcome"] for p in cases)),
               "pending_container_candidate_entries":sum(row["status"].startswith("container candidate") for row in ledger),
               "outside_container_scope_entries":sum(row["status"].startswith("outside") for row in ledger),
               "source_reviewed_shared_or_non_ui_entries":sum(row["status"].startswith(("shared infrastructure", "source-reviewed")) for row in ledger),
               "same_menu_class_discrepancies":sum(not check["passed"] for check in checks),
               "coverage_limit":"Opening, captured appearance and provenance only; no complete functional suite or every-state certification. Pending entries are not completed."}
    result = {"summary":summary,"cases":cases,"checks":checks,"appendix":ledger}
    (OUT / "coverage.json").write_text(json.dumps(result,indent=2)+"\n",encoding="utf-8")
    gaps = [{key:row[key] for key in ("entry", "title", "status", "observed_original_releases",
                                     "unobserved_screen_candidates_in_staged_releases", "unobserved_inventory_workflows", "source_review")}
            for row in ledger if row["status"].startswith("container candidate")
            or row["unobserved_screen_candidates_in_staged_releases"] or row["unobserved_inventory_workflows"]]
    (OUT / "capture-gaps.json").write_text(json.dumps({
        "scope":"Capture queue only. A retained class is not proof of a distinct reachable UI. Source-reviewed helpers and other-release classes are kept separate.",
        "entries":gaps},indent=2)+"\n",encoding="utf-8")
    with (OUT / "coverage.csv").open("w",encoding="utf-8",newline="") as stream:
        writer=csv.DictWriter(stream,fieldnames=["entry","title","audit_kind","pairs","status","appendix_reason"])
        writer.writeheader()
        for row in ledger:
            writer.writerow({k:row[k] for k in writer.fieldnames})
    print(json.dumps(summary,indent=2))


if __name__ == "__main__":
    main()
