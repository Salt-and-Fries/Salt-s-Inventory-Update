"""Verify saved native-client search regressions and the limited production artifact delta."""
import hashlib
import json
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "build/ancient-remnants-issue16"
TARGETS = [(mc, loader, browser) for mc, loaders in [
    ("1.20.1", ["fabric", "forge"]),
    ("1.21.1", ["fabric", "neoforge"]),
    ("26.1.2", ["fabric", "neoforge"]),
] for loader in loaders for browser in ["rei"]] + [
    ("26.1.2", "neoforge", "jei"), ("1.21.1", "fabric", "jei"),
]


def read(path):
    return json.loads(path.read_text(encoding="utf-8-sig"))


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


before = read(OUT / "before/artifacts.json")
changed = []
for name, digest in before.items():
    path = ROOT / name
    if sha(path) == digest:
        continue
    assert path.parent.name == "compatibility" and "1.20.1-" not in path.name, path
    with zipfile.ZipFile(OUT / "before" / path.name) as old, zipfile.ZipFile(path) as new:
        delta = [entry for entry in set(old.namelist()) | set(new.namelist())
                 if entry not in old.namelist() or entry not in new.namelist() or old.read(entry) != new.read(entry)]
        assert delta and all(entry.startswith("com/salts_inventory_update/compat/rei/RuntimeReiRecipeBrowserAccess") for entry in delta), (path, delta)
    changed.append(path.name)
assert len(before) == 36 and len(changed) == 10, changed

original = ROOT / "versions/26.1.2/neoforge/build/runtime-smoke/browser-search-rei-before/crash-reports"
crash = next(original.glob("crash-*-client.txt")).read_text(encoding="utf-8")
assert 'Failed to test search filter: "r"' in crash
assert "MonolithCodexItem.appendHoverText" in crash and "registries()\" is null" in crash
baseline = read(OUT / "gameplay/26.1.2-neoforge-rei-before/staged-artifacts.json")
original_compat = next(a for a in baseline["artifacts"] if a["family"] == "compatibility")
assert sha(OUT / "before" / original_compat["filename"]) == original_compat["sha256"]

runs = []
for mc, loader, browser in TARGETS:
    name = f"{mc}-{loader}-{browser}-fixed" + ("3" if browser == "jei" else "")
    folder = OUT / "gameplay" / name
    report = read(folder / "results.json")
    assert report["failures"] == 0 and all(r["status"] in {"PASS", "CAPTURED"} for r in report["results"]), name
    checks = {r["name"] for r in report["results"]}
    assert {"ancient-remnants-loaded", "compatibility-loaded", "search-baseline-codex-present",
            "search-keeps-codex-monolith", "search-keeps-codex-" + ("$pages" if browser == "jei" else "#codex"),
            "search-keeps-codex-@ancient_remnants", "search-no-results", "search-clear-restores-entries"} <= checks, name
    assert report["minecraft"] == mc and report["loader"] == loader, name
    assert report["loadedMods"]["salts_inventory_update"] == "0.1.5", name
    assert report["loadedMods"]["salts_inventory_compat"] == "0.1.2", name
    assert report["loadedMods"]["ancient_remnants"] == ("1.3.1" if mc == "26.1.2" else "1.5.2"), name
    assert "salts_inventory_controlify" not in report["loadedMods"], name
    for key in ("coreCodeSource", "compatibilityCodeSource"):
        assert ".jar" in report[key] and "/classes/" not in report[key].replace("\\", "/"), (name, key)
    staged = read(folder / "staged-artifacts.json")
    assert staged["scope"] == "ancient-remnants-browser-search", name
    for artifact in staged["artifacts"]:
        assert sha(Path(artifact["file"])) == artifact["sha256"], (name, artifact)
    fixture = read(OUT / f"{mc}-{loader}-{browser}-fixtures.json")
    for artifact in fixture["artifacts"]:
        assert hashlib.sha512(Path(artifact["path"]).read_bytes()).hexdigest() == artifact["sha512"], (name, artifact)
    images = list((folder / "screenshots").glob("*.png"))
    assert len(images) == 9 and all(p.stat().st_size > 1000 for p in images), name
    runs.append({"minecraft": mc, "loader": loader, "browser": browser,
                 "records": len(report["results"]), "failures": report["failures"], "screenshots": len(images)})

summary = {"status": "PASS", "scope": "Ancient Remnants search only", "original_crash_reproduced": True,
           "changed_compatibility_jars": sorted(changed), "unchanged_other_jars": 26, "runs": runs,
           "records": sum(run["records"] for run in runs), "screenshots": sum(run["screenshots"] for run in runs)}
(OUT / "summary.json").write_text(json.dumps(summary, indent=2) + "\n", encoding="utf-8")
print(f"Verified original crash, {len(runs)} focused search profiles and ten isolated REI artifact changes.")
print("All Core/Controlify and both 1.20.1 Compatibility artifacts remain unchanged.")
