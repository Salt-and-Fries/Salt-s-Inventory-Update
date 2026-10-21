"""Audit saved server snapshots from the focused Windows Computer Use playthrough.

This reads evidence only. It does not drive Minecraft or replace real mouse input.
"""
import hashlib
import json
from collections import Counter
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
EVIDENCE = ROOT / "build/mouse-tweaks/gameplay"
FIXED = [
    "1.20.1-forge", "1.20.1-fabric", "1.21.11-fabric",
    "26.3-fabric", "26.3-neoforge",
]
BROAD = ["26.3-fabric-computer-use-01", "1.20.1-forge-computer-use-02"]


def read(path):
    return json.loads(path.read_text(encoding="utf-8-sig"))


def slots(snapshot, inventory):
    result = {}
    for entry in snapshot[inventory]:
        index, stack = entry.split(":", 1)
        item, count = stack.rsplit("=", 1)
        result[int(index)] = (item.removeprefix("minecraft:"), int(count))
    return result


def cursor(snapshot):
    count, item = snapshot["cursor"].split(" ", 1)
    return item.removeprefix("minecraft:"), int(count)


def count(snapshot, inventory, index):
    return slots(snapshot, inventory).get(index, ("air", 0))[1]


def state(snapshot):
    return {k: v for k, v in snapshot.items() if k != "time"}


def check_run(name, expected_stone, fixed):
    folder = EVIDENCE / name
    report = read(folder / "results.json")
    assert report["failures"] == 0, name
    assert report["scope"] == ("mouse-tweaks-computer-use-fixture" if fixed else "mouse-tweaks-core-only"), name
    assert any(record["name"] == "manual-layout" for record in report["results"]), name
    assert "mousetweaks" in report["loadedMods"], name
    assert not any("compatibility" in mod or "controlify" in mod for mod in report["loadedMods"]), name
    checkpoints = {}
    for path in sorted(folder.glob("*.json")):
        snapshot = read(path)
        if path.name == "manual-state.json" or "container0" not in snapshot:
            continue
        total = Counter()
        for inventory in ("player", "container0", "container1", "container2"):
            for item, quantity in slots(snapshot, inventory).values():
                assert 0 < quantity <= 64, (path, item, quantity)
                total[item] += quantity
        item, quantity = cursor(snapshot)
        assert 0 <= quantity <= 64, path
        total[item] += quantity
        assert total["stone"] == expected_stone, (path, total)
        assert total["dirt"] == 42 and total["iron_ingot"] == 3, (path, total)
        assert count(snapshot, "container2", 2) == 10, (path, "virtual dirt slot moved")
        checkpoints[path.stem] = snapshot
    assert checkpoints, name
    manifest = read(folder / "staged-artifacts.json")
    if fixed:
        core = next(a for a in manifest["artifacts"] if a["family"] == "core")
        assert hashlib.sha256(Path(core["file"]).read_bytes()).hexdigest() == core["sha256"], name
        craft = checkpoints["crafting-wheel-fixed"]
        assert cursor(craft)[1] == 0, name
        assert slots(craft, "player")[9] == ("stone", 23 if name.startswith("26.3-neoforge") else 20), name
        assert sum(q for item, q in slots(craft, "player").values() if item == "crafting_table") == 1, name
        assert sum(q for item, q in slots(craft, "player").values() if item == "oak_planks") == 12, name
        drag = checkpoints["post-fix-native-api-drag"]
        assert cursor(drag) == ("stone", 32), name
        assert count(drag, "container1", 1) == count(drag, "container2", 1) == 0, name
        final = next(value for key, value in checkpoints.items() if key.endswith("-final"))
        assert cursor(final)[1] == 0, name
    return checkpoints, {"run": name, "checkpoints": len(checkpoints), "current_core_hash": fixed,
                         "minecraft": report["minecraft"], "loader": report["loader"],
                         "mouse_tweaks": report["loadedMods"]["mousetweaks"]}


summaries = []
modern, summary = check_run(BROAD[0], 164, False)
summaries.append(summary)
legacy, summary = check_run(BROAD[1], 164, False)
summaries.append(summary)
assert cursor(modern["cross-native-collection"]) == ("stone", 56)
assert cursor(legacy["cross-native-collection"]) == ("stone", 52)
assert cursor(modern["native-api-drag"]) == ("stone", 30)
assert cursor(legacy["native-api-drag"]) == ("stone", 32)
assert cursor(modern["right-split"]) == ("stone", 28)
assert cursor(legacy["right-split"]) == ("stone", 16)
assert cursor(modern["double-click"]) == ("stone", 64)
assert cursor(modern["right-final-item"])[1] == 0
assert cursor(legacy["crafting-wheel"]) == ("stone", 20), "missing original bug evidence"
for snapshots, before, after in [
    (modern, "wheel-player-closed", "overlap-wheel-blocked"),
    (modern, "crafting-wheel", "popup-wheel-blocked"),
    (legacy, "player-closed-wheel", "overlap-wheel-blocked"),
    (legacy, "wheel-source-exhausted", "exhausted-source-noop"),
]:
    assert state(snapshots[before]) == state(snapshots[after]), (before, after)
for target in FIXED:
    _, summary = check_run(target + "-computer-use-craft-r2", 152, True)
    summaries.append(summary)
output = ROOT / "build/mouse-tweaks/computer-use-audit.json"
output.write_text(json.dumps({"status": "PASS", "scope": "saved Computer Use server snapshots",
                              "checkpoint_count": sum(s["checkpoints"] for s in summaries),
                              "runs": summaries}, indent=2) + "\n", encoding="utf-8")
print(f"Verified {sum(s['checkpoints'] for s in summaries)} checkpoints across {len(summaries)} playthroughs.")
print("Item conservation, stack limits, original failure, five fixed regressions and current Core hashes verified.")
