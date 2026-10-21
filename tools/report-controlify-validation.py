"""Refresh Controlify documentation from actual tests, archives and packaged runtime evidence."""
import argparse
import contextlib
from datetime import datetime, timezone
import importlib.util
import io
import json
import os
from pathlib import Path
import xml.etree.ElementTree as ET
import zipfile

ROOT = Path(__file__).resolve().parents[1]
VERSIONS = ("1.20.1", "1.21.1", "1.21.11", "26.1.2", "26.2", "26.3")
LOADERS = {"fabric": "Fabric", "forge": "Forge", "neoforge": "NeoForge"}


def load_tool(filename):
    spec = importlib.util.spec_from_file_location(filename.replace("-", "_"), ROOT / "tools" / filename)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def relative(path):
    return Path(path).resolve().relative_to(ROOT).as_posix()


def link(path, label, document):
    destination = os.path.relpath(Path(path).resolve(), document.parent.resolve()).replace("\\", "/")
    return f"[{label}]({destination})"


def junit(name, folder):
    files = sorted(folder.glob("TEST-*.xml"))
    row = {"suite": name, "tests": 0, "failures": 0, "errors": 0, "skipped": 0,
           "xml_files": len(files), "report": relative(folder.parent.parent / "reports/tests/test/index.html")}
    for path in files:
        element = ET.parse(path).getroot()
        suites = [element] if element.tag == "testsuite" else element.findall(".//testsuite")
        for suite in suites:
            for key in ("tests", "failures", "errors", "skipped"):
                row[key] += int(suite.get(key, "0"))
    row["status"] = "Passed" if files and row["tests"] and not any(
        row[key] for key in ("failures", "errors", "skipped")) else "Missing" if not files else "Needs attention"
    return row


def archive(folder, prefix, game, loader, runtime):
    candidates = list(folder.glob(f"{prefix}-{game}-{LOADERS[loader]}-*.jar"))
    assert len(candidates) == 1, f"{game}-{loader}: expected one collected {prefix} JAR, found {len(candidates)}"
    path = candidates[0]
    with zipfile.ZipFile(path) as jar:
        classes = sum(name.endswith(".class") for name in jar.namelist())
        assert classes, f"{path.name}: no implementation classes"
    return {"path": relative(path), "bytes": path.stat().st_size,
            "sha256": runtime.digest(path), "classes": classes}


def inventory(targets, artifacts, companions, runtime):
    rows = []
    for target in targets:
        game, loader = target["minecraft"], target["loader"]
        row = {"minecraft": game, "loader": loader, "status": "Present"}
        try:
            row["core"] = archive(artifacts / "core", "salts_inventory_update", game, loader, runtime)
            row["controlify"] = archive(artifacts / "controlify", "salts_inventory_controlify", game, loader, runtime)
            dedicated = companions / Path(row["controlify"]["path"]).name
            assert dedicated.is_file() and runtime.digest(dedicated) == row["controlify"]["sha256"], \
                f"{game}-{loader}: dedicated companion differs from installation bundle"
            core_version = Path(row["core"]["path"]).stem.split(f"-{LOADERS[loader]}-", 1)[1]
            companion_version = Path(row["controlify"]["path"]).stem.split(f"-{LOADERS[loader]}-", 1)[1]
            runtime.companion_versions(ROOT / row["controlify"]["path"], loader, core_version, companion_version)
        except (AssertionError, OSError, zipfile.BadZipFile) as error:
            row["status"], row["details"] = "Needs attention", str(error)
        rows.append(row)
    return rows


def boundaries(targets, rows, tool):
    output = io.StringIO()
    try:
        with contextlib.redirect_stdout(output):
            tool.sources()
            tool.matrix_sources([(target["minecraft"], target["loader"]) for target in targets])
            for row in rows:
                assert row["status"] == "Present", row.get("details", "Missing artifacts")
                core_version = Path(row["core"]["path"]).stem.split(f"-{LOADERS[row['loader']]}-", 1)[1]
                tool.jar(ROOT / row["controlify"]["path"], core_version)
                tool.peer_jar(ROOT / row["core"]["path"])
                tool.classpath(ROOT / f"controlify/versions/{row['minecraft']}/{row['loader']}/build/controlify-compile-classpath.txt")
        return {"status": "Passed", "details": output.getvalue().splitlines()}
    except (AssertionError, OSError, KeyError, zipfile.BadZipFile) as error:
        return {"status": "Needs attention", "details": str(error)}


def gameplay(targets, args, runtime):
    rows = []
    focused = load_tool('verify-controlify-focused-runtime.py') if args.feature_parity else None
    for target in targets:
        name = f"{target['minecraft']}-{target['loader']}"
        legacy_parity = args.feature_parity and target['minecraft'] in ('1.20.1','1.21.1','1.21.11')
        extended = args.feature_parity or args.extended_26_3 and target['minecraft'] == '26.3'
        suffix = '-feature-parity-extended' if legacy_parity else '-extended' if extended else ''
        report = args.results_root / (name + suffix) / "results.json"
        row = {"minecraft": target["minecraft"], "loader": target["loader"], "status": "Pending"}
        if report.is_file():
            row["report"] = relative(report)
            try:
                with contextlib.redirect_stdout(io.StringIO()):
                    if legacy_parity:
                        row.update(focused.validate(report.parent,args.artifacts))
                        row['validated_scope'] = 'ported features and custom-window APIs'
                    else:
                        profile = 'packaged-controlify-' + name + suffix if extended else args.run_profile
                        row.update(runtime.validate(target, args.results_root, args.artifacts, profile, suffix, extended,
                            feature_parity=args.feature_parity,allow_completed_main_suite=args.feature_parity,
                            allow_ui_text_correction=args.feature_parity))
                    if args.feature_parity:
                        proof_reports = [report]
                        raw = json.loads(report.read_text(encoding='utf-8-sig'))
                        passed = {r['name'] for r in raw['results'] if r['status']=='PASS'}
                        retries = []
                        needed = []
                        if not legacy_parity:
                            if row.get('requires_successful_final_artifact_api_retry') or not focused.compat_runtime.API_CHECKS <= passed:
                                needed.append('public-api')
                            if any(proof.get('requires_final_artifact_wheel_retry') for proof in row['artifact_lineage'].values()):
                                needed.append('wheel-polish')
                        for scope in needed:
                            folder = args.results_root / f'{name}-{scope}-extended'
                            retry = focused.validate(folder,args.artifacts)
                            retries.append(retry);proof_reports.append(folder/'results.json')
                            row['passes'] += retry['passes'];row['screenshots'] += retry['screenshots']
                            row['elapsed_ms'] += retry['elapsed_ms']
                        if needed: row['validated_scope'] = 'main suite + ' + ', '.join(needed)
                        row['focused_retries'] = retries
                        row['unique_checks'] = len({r['name'] for path in proof_reports
                            for r in json.loads(path.read_text(encoding='utf-8-sig'))['results'] if r['status']=='PASS'})
                row["status"] = "Passed"
            except (AssertionError, OSError, ValueError, KeyError, TypeError, zipfile.BadZipFile) as error:
                row["status"], row["details"] = "Needs rerun", str(error)
        else:
            row["details"] = "No completed packaged gameplay report"
        rows.append(row)
    return rows


def markdown(summary, document):
    passed_tests = all(row["status"] == "Passed" for row in summary["junit"])
    total = summary["junit_total"]
    artifacts = summary["artifacts"]
    runs = summary["runtime"]
    verified = sum(row["status"] == "Passed" for row in runs)
    present = sum(row["status"] == "Present" for row in artifacts)
    refresh = 'python tools/report-controlify-validation.py'
    if summary.get('feature_parity'): refresh += ' --feature-parity --artifacts dist'
    elif summary.get('extended_26_3'): refresh += ' --extended-26-3 --artifacts dist'
    if document.resolve() != ROOT / 'controlify/VALIDATION.md': refresh += ' --output ' + relative(document)
    lines = ["# Controlify integration validation", "",
             f"Generated from local evidence at {summary['generated_at']}. Refresh with " +
             f"`{refresh}`.", "",
             f"The complete target inventory contains **12 Minecraft/loader combinations**. "
             f"**{present}/12 matching Core and companion artifact pairs** are collected. "
             f"Packaged scoped verification is complete for **{verified}/12 targets**. "
             "The covered scopes and artifact lineage are reported below.", ""]
    if summary.get('feature_parity'):
        lines += ['Legacy 1.20.1/1.21.1/1.21.11 runs exercise the ported recipe, creative, scrolling, '
                  'keyboard and custom-window features. Modern targets use the main feature suite plus '
                  'focused retries where needed. These scopes are not described as a complete functional test of every target.', '',
                  'Focused reports must match all three final Salt JARs exactly. Reuse of earlier modern main-suite evidence '
                  'permits only four corrected bullet/ellipsis UTF-8 constants in one wheel class, requiring all remaining '
                  'class and resource bytes to be identical and a successful wheel retry on the final JAR. A main report '
                  'that stopped only in the independent post-suite API setup also requires a successful final-JAR API retry. '
                  'The original reports and their setup failures remain recorded; failures in the completed main phase are never accepted.', '']
    if summary.get('feature_parity'):
        lines += ['For legacy Fabric development launches, the original staged Salt inputs remain byte-identical '
                  'to final dist artifacts and original upstream inputs retain their published SHA-512 pins. '
                  'The actual loader creates named-namespace runtime copies in the owned profile’s '
                  '`.fabric/processedMods/` directory. The focused verifier invokes that same native '
                  '`RuntimeModRemapper` with the recorded mapping JAR, classpath and selected original/nested '
                  'inputs, then compares every generated member name and byte with the loaded copies '
                  '(16 selected JARs per Fabric profile). Forge records the official `SRG_TO_MCP` tool, '
                  'mapping hashes and named derivatives of the five preserved original Salt/Controlify/YACL JARs. '
                  'The native code sources must resolve to their verified same-profile runtime copies. '
                  'Reproducing this evidence requires the retained native mapping/classpath cache and a local JDK.', '']
    if verified < 12:
        lines += ["Full runtime matrix verification remains incomplete. Pending or stale/failed reports "
                  "below do not establish working gameplay for that target.", ""]
    lines += ["## Automated tests", "",
              f"The {len(summary['junit'])} scoped JUnit XML directories contain **{total['tests']} tests**, "
              f"with {total['failures']} failures, {total['errors']} errors and {total['skipped']} skips. "
              f"All suites {'passed' if passed_tests else 'have not passed'}.", "",
              "| Suite | Tests | Status | Report |", "| --- | ---: | --- | --- |"]
    for row in summary["junit"]:
        lines.append(f"| {row['suite']} | {row['tests']} | {row['status']} | "
                     f"{link(ROOT / row['report'], 'HTML', document)} |")
    lines += ["", "Core tests run once per Minecraft version through its Fabric test source set. "
              "Forge and NeoForge share the ported Core implementation; their native compilation, "
              "archive checks and individual gameplay runs are separate evidence. " +
              ("Extended provider tests are included for each version in this parity pass."
               if summary.get("feature_parity") else "Extended compatibility test counts are outside this controller matrix total."), "",
              "The regressions cover workspace lifetimes, mandatory-window protection, restoration, "
              "scoped HUD settings, native/external slot geometry, modal ownership and outside dismissal, "
              "exact displayed-pair destinations, server authorization and transaction rollback, "
              "native crafting/Loom/furnace result callbacks and replenishment. Modern API fixtures "
              "also check supplementary Unicode character events. The controller suite covers "
              "three-mode state, input routing, bounded layouts, animation, transformed clipping and "
              "queued-render opacity capture.", "", "## Artifacts and isolation", "",
              f"Independent source, all twelve compiler manifests, collected companion bytecode and "
              f"matching Core peer archives: **{summary['boundaries']['status']}**. "
              "The reporter calls the existing " + link(ROOT / "tools/verify-controlify-boundaries.py", "boundary verifier", document) +
              " for these checks. Dedicated companions and installation-bundle copies must be byte-identical.", "",
              "`buildControlify` also checks native Controlify/Minecraft member references and mixin "
              "selectors against each target's actual compiler inputs, and verifies the published "
              "controller JAR against its SHA-512 pin. These are build gates; gameplay status is "
              "determined independently below.", "",
              "The third JAR contains the controller layout, bindings, animation and render adapters. "
              "Core supplies generic public workspace/transfer APIs. Upstream Controlify/YACL, Core "
              "and the optional extended compatibility mod remain separate archives. QA fixtures "
              "are excluded from production JARs.", "",
              "| Minecraft | Loader | Matching artifact pair | Packaged gameplay |", "| --- | --- | --- | --- |"]
    for pair, run in zip(artifacts, runs):
        lines.append(f"| {pair['minecraft']} | {LOADERS[pair['loader']]} | {pair['status']} | {run['status']} |")
    if summary["boundaries"]["status"] != "Passed":
        lines += ["", "Boundary validation detail: " + str(summary["boundaries"]["details"]).replace("\n", " ")]
    dependencies = summary.get("runtime_dependencies", [])
    if dependencies:
        lines += ["", "The following additional upstream runtime dependency is installed separately. "
                  "Its original published bytes and declared version must also pass the packaged runtime verifier.", "",
                  "| Minecraft | Loader | Required runtime dependency | Pin |", "| --- | --- | --- | --- |"]
        for pin in dependencies:
            name = "LexManos's Mixin Args" if pin["mod_id"] == "mixin_args" else pin["mod_id"]
            lines.append(f"| {pin['minecraft']} | {LOADERS[pin['loader']]} | {name} {pin['version']} | "
                         f"[Original release]({pin['source']}); `{pin['filename']}`; SHA-512 in targets.json |")
    lines += ["", "## Packaged gameplay evidence", "",
              "The " + link(ROOT / "tools/verify-controlify-runtime.py", "runtime verifier", document) +
              (" checks modern main-suite evidence for all eleven baseline objective areas, "
               if summary.get("feature_parity") else " requires successful scenario records for all eleven baseline objective areas, ") +
              f"plus {summary['runtime_feature_areas_26_3']} feature areas for {'modern targets' if summary.get('feature_parity') else '26.3'}, actual matching "
              "Minecraft/Controlify/YACL versions, original dependency SHA-512 pins, separate production "
              "JAR code sources, verified final collected/staged artifact lineage, fresh "
              "reports and actual PNG captures. " +
              ("Legacy ported-feature scopes and modern focused retries are checked independently by the focused verifier. "
               if summary.get("feature_parity") else "") +
              "Every report used by this document is revalidated when the file is generated.", "",
              "| Minecraft | Loader | Status | Evidence |", "| --- | --- | --- | --- |"]
    for row in runs:
        if row["status"] == "Passed":
            details = f"{row.get('unique_checks',row['passes'])} distinct checks; {row['screenshots']} screenshots; {row['elapsed_ms'] / 1000:.3f} s"
            if summary.get('feature_parity'): details += '; ' + row.get('validated_scope','scoped evidence')
            if row.get('report_failures'):
                details += '; original post-suite API setup failed; final API retry passed'
        else:
            details = row["details"].replace("|", "\\|").replace("\n", " ")
        if "report" in row:
            details += "; " + link(ROOT / row["report"], "report", document)
        for retry in row.get('focused_retries',[]):
            details += '; ' + link(ROOT / retry['report'],retry['scope'],document)
        lines.append(f"| {row['minecraft']} | {LOADERS[row['loader']]} | {row['status']} | {details} |")
    lines += ["", "The disposable integrated-server harness installs a synthetic driver in the real "
              "Controlify manager and uses actual bindings, native cursor/screen processing and server "
              "packets. Supported mixed input mode prevents operating-system cursor events from "
              "interrupting automation. This provides game runtime evidence; physical controller "
              "hardware has not been tested.", "",
              ("The modern main-suite runs cover permanent/default windows; side-by-side native pointer, "
               if summary.get("feature_parity") else "Each completed run must cover permanent/default windows; side-by-side native pointer, ") +
              "D-pad snapping and exact transfers; radial navigation/focus/bumpers; gameplay gating; "
              "tuck/add/restore identities; protected and individual close; immediate side/radial close " +
              ("on all targets; " if summary.get("feature_parity") else "on 26.3 or the configured hold threshold on older versions; ") +
              "HUD/animation settings; native result callbacks and conservation/XP; protruding custom "
              "UI and modal dismissal; and the genuine controller/server handshake.", "",
              "```powershell", ".\\gradlew.bat buildControlify collectControlifyJars collectControlifyTestJars --console=plain --no-parallel",
              ".\\gradlew.bat -I controlify/functional-tests/controlify-qa.init.gradle :controlify:1.21.1:fabric:runPackagedControlifyQa",
              "python tools/report-controlify-validation.py",
              "python tools/verify-controlify-runtime.py --summary build/controlify-runtime-verified.json", "```", "",
              "Use the corresponding `:controlify:<minecraft>:<loader>:runPackagedControlifyQa` task "
              "for each target. The full runtime verification command rejects an incomplete matrix.", "",
              "## Collected file hashes", "",
              "Install each companion with the matching rebuilt Core and the pinned native dependencies "
              "listed in " + link(ROOT / "controlify/README.md", "README", document) +
              ". The installation bundle keeps `core/` and `controlify/` separate; dedicated companion "
              "copies are in `dist/controlify/`.", "",
              "| Artifact | Bytes | SHA-256 |", "| --- | ---: | --- |"]
    for row in artifacts:
        for module in ("core", "controlify"):
            if module in row:
                item = row[module]
                label = f"{row['minecraft']} {LOADERS[row['loader']]} {module}"
                lines.append(f"| {link(ROOT / item['path'], label, document)} | {item['bytes']} | `{item['sha256']}` |")
    return "\n".join(lines) + "\n"


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--targets", type=Path, default=ROOT / "controlify/targets.json")
    parser.add_argument("--results-root", type=Path, default=ROOT / "build/controlify-qa")
    parser.add_argument("--artifacts", type=Path, default=ROOT / "dist/controlify-test")
    parser.add_argument("--companions", type=Path, default=ROOT / "dist/controlify")
    parser.add_argument("--run-profile", default="packaged-controlify")
    parser.add_argument("--extended-26-3", action="store_true", help="Use the retained full 26.3 runs with the Extended module")
    parser.add_argument("--feature-parity", action="store_true", help="Require the new feature-parity runs with Extended on all twelve targets")
    parser.add_argument("--output", type=Path, default=ROOT / "controlify/VALIDATION.md")
    parser.add_argument("--summary", type=Path, default=ROOT / "build/controlify-validation.json")
    parser.add_argument("--require-runtime", action="store_true", help="Reject any missing, stale or failed gameplay target")
    args = parser.parse_args()
    for name in ("targets", "results_root", "artifacts", "companions", "output", "summary"):
        setattr(args, name, getattr(args, name).resolve())
    boundary = load_tool("verify-controlify-boundaries.py")
    runtime = load_tool("verify-controlify-runtime.py")
    targets = list(boundary.matrix(args.targets).values())
    suites = [junit("Controller model/input/render", ROOT / "controlify/model/build/test-results/test")]
    suites += [junit(f"Core {game}", ROOT / f"versions/{game}/fabric/build/test-results/test") for game in VERSIONS]
    if args.feature_parity:
        suites += [junit(f"Extended {game} providers", ROOT / f"compatibility/versions/{game}/fabric/build/test-results/test") for game in VERSIONS]
    elif args.extended_26_3:
        suites += [junit("Extended 26.3 providers", ROOT / "compatibility/versions/26.3/fabric/build/test-results/test")]
    artifacts = inventory(targets, args.artifacts, args.companions, runtime)
    summary = {"schema_version": 1, "generated_at": datetime.now(timezone.utc).isoformat(timespec="seconds"), "extended_26_3": args.extended_26_3,
               "feature_parity": args.feature_parity,
               "runtime_feature_areas_26_3": len(runtime.FEATURE_COVERAGE_26_3),
               "junit": suites, "junit_total": {key: sum(row[key] for row in suites)
                                               for key in ("tests", "failures", "errors", "skipped")},
               "artifacts": artifacts, "boundaries": boundaries(targets, artifacts, boundary),
               "runtime_dependencies": [{"minecraft": target["minecraft"], "loader": target["loader"], **pin}
                                        for target in targets for pin in target.get("runtime_dependencies", [])],
               "runtime": gameplay(targets, args, runtime)}
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(markdown(summary, args.output), encoding="utf-8")
    args.summary.parent.mkdir(parents=True, exist_ok=True)
    args.summary.write_text(json.dumps(summary, indent=2) + "\n", encoding="utf-8")
    complete = sum(row["status"] == "Passed" for row in summary["runtime"])
    print(f"Validation report refreshed: {summary['junit_total']['tests']} JUnit tests, {complete}/12 packaged runtime scopes verified")
    failed = any(row["status"] != "Passed" for row in suites) or summary["boundaries"]["status"] != "Passed"
    return 1 if failed or args.require_runtime and complete != 12 else 0


if __name__ == "__main__":
    raise SystemExit(main())
