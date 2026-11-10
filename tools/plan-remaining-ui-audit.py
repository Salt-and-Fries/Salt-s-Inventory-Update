"""Inventory the remaining audit entries without claiming gameplay coverage.

Uses the retained, hash-pinned audit binaries. Screen inheritance and abstract
flags distinguish capture candidates from helper classes; only a real runtime
observation can establish that a candidate is reachable or covered.
"""
from __future__ import annotations

import collections
import csv
import io
import json
from pathlib import Path
import re
import struct
import zipfile

ROOT = Path(__file__).resolve().parents[1]
AUDIT = ROOT / "build/ui-modpack-audit-20261025"
OUT = ROOT / "output/remaining-ui-audit"
# Exact audit rows, not mod IDs: Tierify and TieredZ deliberately remain distinct.
DELIVERED = {19, 33, 34, 42, 70, 74, 83, 99, 110, 130, 168, 217,
             222, 238, 244, 245, 276, 295, 307, 317, 382, 413, 430,
             499, 505, 524, 532, 579, 583}
SCREEN_ROOTS = {"net/minecraft/client/gui/screens/Screen", "net/minecraft/class_437",
                "net/minecraft/client/gui/screen/Screen"}
CONTAINER_ROOTS = {"net/minecraft/client/gui/screens/inventory/AbstractContainerScreen",
                   "net/minecraft/class_465", "net/minecraft/client/gui/screen/ingame/HandledScreen"}
MENU_ROOTS = {"net/minecraft/world/inventory/AbstractContainerMenu", "net/minecraft/class_1703",
              "net/minecraft/screen/ScreenHandler"}


def read_json(path):
    return json.loads(path.read_text(encoding="utf-8-sig"))


def class_header(data, include_strings=False):
    if data[:4] != b"\xca\xfe\xba\xbe":
        return None
    count = struct.unpack_from(">H", data, 8)[0]
    pool = [None] * count
    i, offset = 1, 10
    while i < count:
        tag = data[offset]
        offset += 1
        if tag == 1:
            size = struct.unpack_from(">H", data, offset)[0]
            offset += 2
            pool[i] = data[offset:offset + size].decode("utf-8", "replace")
            offset += size
        elif tag in (7, 8, 16, 19, 20):
            pool[i] = struct.unpack_from(">H", data, offset)[0]
            offset += 2
        elif tag in (3, 4, 9, 10, 11, 12, 17, 18):
            offset += 4
        elif tag in (5, 6):
            offset += 8
            i += 1
        elif tag == 15:
            offset += 3
        else:
            raise ValueError(f"Unknown constant pool tag {tag}")
        i += 1
    flags, this, parent = struct.unpack_from(">HHH", data, offset)
    result = {"name": pool[pool[this]], "super": pool[pool[parent]] if parent else "",
              "abstract": bool(flags & 0x0400), "interface": bool(flags & 0x0200),
              "mixin": "Lorg/spongepowered/asm/mixin/Mixin;" in pool}
    if include_strings:
        result["strings"] = [value for value in pool if isinstance(value, str)]
    return result


def classify(name, graph):
    seen, chain = set(), []
    current = name
    while current and current not in seen:
        seen.add(current)
        chain.append(current)
        if current in CONTAINER_ROOTS:
            return "container-screen", chain
        if current in SCREEN_ROOTS:
            return "standalone-screen", chain
        if current in MENU_ROOTS:
            return "menu", chain
        current = graph.get(current, {}).get("super", "")
    return "unresolved", chain


def retained_binary(source, path_to_sha):
    direct = AUDIT / "jars" / (source["sha1"] + ".jar")
    if direct.exists():
        return direct
    components = source["path"].split("!/")
    if len(components) < 2 or components[0] not in path_to_sha:
        return None
    parent = AUDIT / "jars" / (path_to_sha[components[0]] + ".jar")
    if not parent.exists():
        return None
    data = parent.read_bytes()
    for member in components[1:]:
        with zipfile.ZipFile(io.BytesIO(data)) as archive:
            data = archive.read(member)
    import hashlib
    expected_key = "nested-" + hashlib.sha1((path_to_sha[components[0]] + "!" + source["path"]).encode()).hexdigest()
    assert source["sha1"] == expected_key, source["path"]
    digest = hashlib.sha1(data).hexdigest()
    target = ROOT / "build/remaining-ui-audit/nested" / (digest + ".jar")
    target.parent.mkdir(parents=True, exist_ok=True)
    if not target.exists():
        target.write_bytes(data)
    return target


def minecraft_graph(graph):
    # Include vanilla intermediate parents such as FurnaceScreen; mod classes
    # often inherit those rather than AbstractContainerScreen directly.
    for version in ("1.20.1", "1.21.1"):
        cache = Path("C:/Users/texca/.gradle/caches/fabric-loom") / version
        tiny = cache / "intermediary-v2.tiny"
        client = cache / "minecraft-client.jar"
        if not tiny.exists() or not client.exists():
            continue
        names = {}
        for line in tiny.read_text(encoding="utf-8").splitlines():
            if line.startswith("c\t"):
                _, official, intermediary = line.split("\t")
                names[official] = intermediary
        with zipfile.ZipFile(client) as jar:
            for name in jar.namelist():
                if not name.endswith(".class") or name[:-6] not in names:
                    continue
                header = class_header(jar.read(name))
                header["name"] = names.get(header["name"], header["name"])
                header["super"] = names.get(header["super"], header["super"])
                graph.setdefault(header["name"], header)
    # Mojang-named vanilla parents used by the Forge audit binaries.
    for name in ("AbstractFurnaceScreen", "AbstractRepairScreen", "AnvilScreen", "BeaconScreen",
                 "BrewingStandScreen", "CartographyTableScreen", "ContainerScreen", "CraftingScreen",
                 "DispenserScreen", "EnchantmentScreen", "FurnaceScreen", "GrindstoneScreen", "HopperScreen",
                 "HorseInventoryScreen", "InventoryScreen", "LoomScreen", "MerchantScreen", "ShulkerBoxScreen",
                 "SmithingScreen", "SmokerScreen", "StonecutterScreen"):
        qualified = "net/minecraft/client/gui/screens/inventory/" + name
        graph.setdefault(qualified, {"name": qualified, "super": "net/minecraft/client/gui/screens/inventory/AbstractContainerScreen"})


def library_graph(graph):
    """Include abstract UI parents from dependencies omitted by the old audit.

    For example AEBaseScreen is an abstract third-party API class: it is not
    itself an owned screen in the selected audit, but descendants in add-ons
    still need to be classified as real containers. Cache exact release-byte
    headers; only UI-shaped classes are read from the full retained pack.
    """
    cache_path = ROOT / "build/remaining-ui-audit/ui-parent-headers.json"
    cache = read_json(cache_path) if cache_path.exists() else {}
    paths = list((AUDIT / "jars").glob("*.jar"))
    paths += list((ROOT / "build/remaining-ui-audit/nested").glob("*.jar"))
    for path in paths:
        key = path.stem
        if key not in cache:
            rows = []
            with zipfile.ZipFile(path) as jar:
                for name in jar.namelist():
                    if name.endswith(".class") and any(part in name.lower() for part in ("screen", "menu", "/gui/", "container")):
                        rows.append(class_header(jar.read(name)))
            cache[key] = rows
        for header in cache[key]:
            graph[header["name"]] = header
    cache_path.parent.mkdir(parents=True, exist_ok=True)
    cache_path.write_text(json.dumps(cache, separators=(",", ":")) + "\n", encoding="utf-8")


def main():
    evidence = read_json(AUDIT / "ui-audit-evidence.json")
    versions = read_json(AUDIT / "versions.json")
    files = read_json(AUDIT / "files.json")
    path_to_sha = {row["path"]: sha for sha, row in files.items()}
    for source in evidence["per_file_evidence"]:
        if source.get("sha1"):
            path_to_sha[source["path"]] = source["sha1"]
    with (AUDIT / "ui-audit-inventory.csv").open(encoding="utf-8-sig", newline="") as stream:
        rows = list(csv.DictReader(stream))
    by_title = collections.defaultdict(list)
    for entry in evidence["entries"]:
        by_title[entry["title"]].append(entry)
    assert len(rows) == 647
    selected = []
    for row in rows:
        matches = [e for e in by_title[row["title"]]
                   if set(e["modids"]) == set(filter(None, re.split(r"[;, ]+", row["mod_ids"])))]
        if len(matches) > 1:
            matches = [e for e in matches if (e.get("url") or "") == row["source"]]
        assert len(matches) == 1, (row["entry"], row["title"], len(matches))
        selected.append(matches[0])

    graph = {}
    for source in evidence["per_file_evidence"]:
        def register(item):
            for cls in item.get("ui_classes", []):
                graph.setdefault(cls["name"], {"name": cls["name"], "super": cls.get("super", "")})
            for nested in item.get("nested", []):
                register(nested)
        register(source)
    minecraft_graph(graph)
    library_graph(graph)
    entries, missing = [], []
    for row, entry in zip(rows, selected):
        number = int(row["entry"])
        record = {"entry": number, "title": row["title"], "source_url": entry.get("url") or entry.get("source_url"),
                  "audit_kind": row["kind"], "priority": row["priority"], "mod_ids": entry["modids"],
                  "delivered": number in DELIVERED, "releases": [], "coverage": "excluded-delivered" if number in DELIVERED else "not-captured"}
        candidates = set(entry["all_ui_classes"] + entry["surfaces"] + entry["screens"] + entry["gamescreens"] +
                         entry["configscreens"] + entry["menus"] + entry["native_ui_reference_classes"])
        for source in entry["files"]:
            path = retained_binary(source, path_to_sha)
            version = versions.get(source["sha1"]) or {}
            release = {"sha1": source["sha1"], "version": source["version"], "version_id": source.get("version_id"),
                       "path": str(path.relative_to(ROOT)) if path else None, "original_path": source["path"],
                       "minecraft": version.get("game_versions", []), "loaders": version.get("loaders", []),
                       "pack_ids": source["packs"], "ui_candidates": []}
            if path is None:
                release["binary_error"] = "Retained upstream JAR is missing"
                missing.append(source["sha1"])
                record["releases"].append(release)
                continue
            # Do not rescan classes unrelated to UI. Read exact class headers for flags.
            with zipfile.ZipFile(path) as jar:
                names = set(jar.namelist())
                # The original report's class list omitted several add-on
                # screens because their abstract API parents were unresolved.
                candidates.update(name[:-6] for name in names if name.endswith(".class")
                                  and any(part in name.lower() for part in ("screen", "menu", "/gui/", "container")))
                for name in candidates:
                    member = name + ".class"
                    if member not in names:
                        continue
                    header = class_header(jar.read(member))
                    if header:
                        graph[name] = header
                for name in sorted(candidates):
                    if name + ".class" not in names:
                        continue
                    header = graph.get(name, {})
                    kind, chain = classify(name, graph)
                    if kind == "unresolved" or header.get("abstract") or header.get("interface") or header.get("mixin"):
                        continue
                    release["ui_candidates"].append({"class": name, "kind": kind,
                                                      "inheritance": chain, "coverage": "not-captured"})
            release["sha512"] = files.get(source["sha1"], {}).get("hashes", {}).get("sha512")
            record["releases"].append(release)
        record["container_screens"] = sorted({c["class"] for r in record["releases"] for c in r["ui_candidates"] if c["kind"] == "container-screen"})
        record["standalone_screens"] = sorted({c["class"] for r in record["releases"] for c in r["ui_candidates"] if c["kind"] == "standalone-screen"})
        record["menus"] = sorted({c["class"] for r in record["releases"] for c in r["ui_candidates"] if c["kind"] == "menu"})
        if not record["delivered"] and not (record["container_screens"] or record["menus"]):
            record["coverage"] = "no-container-class-observed"  # Static evidence, not proof of absence.
        entries.append(record)
    remaining = [e for e in entries if not e["delivered"]]
    summary = {"source_report_pages": 199, "original_audit_entries": len(entries),
               "delivered_families": 28, "delivered_audit_rows": len(DELIVERED), "remaining_entries": len(remaining),
               "remaining_kind_counts": dict(collections.Counter(e["audit_kind"] for e in remaining)),
               "remaining_entries_with_container_candidates": sum(bool(e["container_screens"] or e["menus"]) for e in remaining),
               "remaining_concrete_container_screen_classes": sum(len(e["container_screens"]) for e in remaining),
               "remaining_standalone_screen_classes": sum(len(e["standalone_screens"]) for e in remaining),
               "missing_retained_binaries": sorted(set(missing)), "actual_gameplay_pairs": 0,
               "scope_note": "Static capture inventory only. Classes may need special state, represent variants, or be unreachable. Every screenshot pair must be backed by runtime observations. No gameplay completion is inferred."}
    OUT.mkdir(parents=True, exist_ok=True)
    (OUT / "inventory.json").write_text(json.dumps({"summary": summary, "entries": entries}, indent=2) + "\n", encoding="utf-8")
    with (OUT / "inventory.csv").open("w", encoding="utf-8", newline="") as stream:
        writer = csv.DictWriter(stream, fieldnames=["entry", "title", "audit_kind", "delivered", "container_screen_count", "standalone_screen_count", "menu_count", "coverage"])
        writer.writeheader()
        for e in entries:
            writer.writerow({**{k:e[k] for k in ("entry", "title", "audit_kind", "delivered", "coverage")},
                             "container_screen_count": len(e["container_screens"]), "standalone_screen_count": len(e["standalone_screens"]), "menu_count": len(e["menus"])})
    print(json.dumps({**summary, "missing_retained_binaries": len(summary["missing_retained_binaries"])}, indent=2))


if __name__ == "__main__":
    main()
