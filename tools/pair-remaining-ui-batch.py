"""Index actual capture batches; never treat an unopened screen as a UI pair.

This is disposable audit tooling. Production files and game behavior are not
modified. Raw screenshots remain untouched; crop rectangles are report layout.
"""
import argparse
import hashlib
import json
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[1]


def read(path):
    return json.loads(path.read_text(encoding="utf-8-sig"))


def side_record(row):
    state = row["observation"]["client"]
    windows = state.get("allWindows", [])
    window = windows[-1] if windows else {}
    bounds = window.get("bounds") or state.get("bounds")
    path = ROOT / row["capture"]
    digest = hashlib.sha256(path.read_bytes()).hexdigest()
    if digest != row["sha256"]:
        raise ValueError("Raw screenshot changed: " + str(path))
    with Image.open(path) as image:
        width, height = image.size
    if bounds:
        sx, sy = width / state["guiWidth"], height / state["guiHeight"]
        x, y = bounds["x"], bounds["y"]
        right, bottom = x + bounds["width"], y + bounds["height"]
        for widget in state.get("widgets", []):
            if not widget.get("visible", True):
                continue
            x, y = min(x, widget["x"]), min(y, widget["y"])
            right = max(right, widget["x"] + widget["width"])
            bottom = max(bottom, widget["y"] + widget["height"])
        crop = [max(0, round((x - 8) * sx)), max(0, round((y - 8) * sy)),
                min(width, round((right + 8) * sx)), min(height, round((bottom + 8) * sy))]
    else:
        crop = [0, 0, width, height]
    return {"capture": row["capture"], "sha256": digest,
            "screen": state["screen"],
            "menu_class": state.get("menuClass") or state.get("menu"),
            "title": state.get("title", ""), "bounds": bounds,
            "crop_rect": crop, "slots": state.get("slots", []), "windows": windows}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("manifest", type=Path)
    parser.add_argument("native_records", type=Path)
    parser.add_argument("core_records", type=Path)
    parser.add_argument("output", type=Path)
    args = parser.parse_args()
    manifest = read(args.manifest)
    native = {x["id"]: x for x in read(args.native_records)}
    core = {x["id"]: x for x in read(args.core_records)}
    cases, omitted = [], []
    for case in manifest["cases"]:
        name = case["id"]
        n, c = native.get(name, {}), core.get(name, {})
        ns = n.get("observation", {}).get("client", {})
        cs = c.get("observation", {}).get("client", {})
        if not n.get("capture") or not c.get("capture") or ns.get("screen", "none") == "none" or cs.get("screen", "none") == "none":
            omitted.append({"id": name, "reason": "A side has no captured visible UI", "native_error": n.get("error"), "core_error": c.get("error")})
            continue
        if case.get("expected_screen") and ns.get("screen") != case["expected_screen"]:
            omitted.append({"id": name, "reason": "Native capture did not reach the expected distinct screen", "observed": ns.get("screen"), "expected": case["expected_screen"]})
            continue
        windows = cs.get("allWindows", [])
        definition = windows[-1].get("definition", "") if windows else ""
        if definition == "builtin":
            outcome = "builtin"
        elif definition.endswith("AutomaticNativeWindow"):
            outcome = "automatic"
        elif not windows and ns["screen"] == cs["screen"]:
            outcome = "native preserved"
        else:
            outcome = "different current screen"
        row = {k: case[k] for k in ("id", "entry", "family", "workflow_id") if k in case}
        row.update(minecraft=manifest["minecraft"], loader=manifest["loader"], outcome=outcome,
                   setup_note=case.get("setup_note", manifest.get("scope", "")),
                   native=side_record(n), core=side_record(c))
        cases.append(row)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps({"scope": "Actual UI opening and appearance; crop/visual review required. No full functionality certification.", "cases": cases}, indent=2) + "\n", encoding="utf-8")
    args.output.with_suffix(".omitted.json").write_text(json.dumps(omitted, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({"paired": len(cases), "omitted": omitted}, indent=2))


if __name__ == "__main__":
    main()
