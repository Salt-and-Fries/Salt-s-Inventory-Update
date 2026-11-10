"""Record the currently visible, normally opened UI without changing it."""
import argparse
import hashlib
import importlib.util
import json
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location("sender", ROOT / "functional-tests/project-everything-ten/send.py")
sender = importlib.util.module_from_spec(spec)
spec.loader.exec_module(sender)

if __name__ == "__main__":
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument("output", type=Path)
    p.add_argument("side", choices=("native", "core"))
    p.add_argument("id")
    a = p.parse_args()
    if not re.fullmatch("[a-z0-9-]+", a.id):
        raise ValueError("Invalid capture id")
    name = a.side + "-" + a.id
    image = a.output / "screenshots" / (name + ".png")
    if image.exists():
        raise ValueError("Preserve existing evidence; choose a new capture id")
    reply = sender.send(a.output, "capture", ticks=12, capture=name)
    row = {"id": a.id, "side": a.side, "runtime_output": str(a.output),
           "capture": str(image), "sha256": hashlib.sha256(image.read_bytes()).hexdigest(),
           "observation": reply}
    dest = a.output / "manual-ui-records.json"
    rows = json.loads(dest.read_text()) if dest.exists() else []
    rows.append(row)
    dest.write_text(json.dumps(rows, indent=2) + "\n", encoding="utf-8")
    c = reply["client"]
    print(json.dumps({"id": a.id, "screen": c.get("screen"), "menu": c.get("menuClass") or c.get("menu"),
                      "windows": [{k: w.get(k) for k in ("definition", "nativeScreen", "menu")} for w in c.get("allWindows", [])],
                      "record": str(dest)}))
