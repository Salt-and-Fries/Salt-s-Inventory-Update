"""Audit all release JARs for removed window APIs, callers, config keys and wire types."""
import argparse
import io
import json
from pathlib import Path
import re
import struct
import zipfile

ROOT = Path(__file__).resolve().parents[1]
API = "com/salts_inventory_update/api/"
WINDOW = API + "client/desktop/"
REMOVED_METHODS = {"minimized", "minimizable", "ghosted", "unghosted", "ghostable", "ghostPinned", "pinChanged"}
REMOVED_ENUMS = {"MINIMIZE", "RESTORE", "GHOST", "UNGHOST"}
OPTIONS_CTORS = {"(ZZZ)V", "(ZZZZZ)V"}
STATE_CTOR = "(Ljava/util/UUID;ZIIIIZZZZZ)V"


def class_info(data):
    assert data[:4] == b"\xca\xfe\xba\xbe"
    position = 8

    def u2():
        nonlocal position
        value = struct.unpack_from(">H", data, position)[0]
        position += 2
        return value

    count = u2()
    pool = [None] * count
    index = 1
    while index < count:
        tag = data[position]
        position += 1
        if tag == 1:
            length = u2()
            value = data[position:position + length].decode("utf-8", errors="replace")
            position += length
        elif tag in (7, 8, 16, 19, 20):
            value = u2()
        elif tag in (9, 10, 11, 12, 17, 18):
            value = (u2(), u2())
        elif tag == 3:
            value = struct.unpack_from(">i", data, position)[0]
            position += 4
        elif tag == 4:
            value = None
            position += 4
        elif tag in (5, 6):
            value = None
            position += 8
            pool[index] = (tag, value)
            index += 2
            continue
        elif tag == 15:
            value = None
            position += 3
        else:
            raise AssertionError(f"Unknown class constant tag {tag}")
        pool[index] = (tag, value)
        index += 1

    def utf8(index):
        assert pool[index][0] == 1
        return pool[index][1]

    def class_name(index):
        assert pool[index][0] == 7
        return utf8(pool[index][1])

    references = []
    for item in pool[1:]:
        if item and item[0] in (9, 10, 11):
            owner, member = item[1]
            name, descriptor = pool[member][1]
            references.append((class_name(owner), utf8(name), utf8(descriptor)))
    u2()  # access flags
    owner = class_name(u2())
    u2()  # superclass
    interfaces = u2()
    position += 2 * interfaces

    def members():
        nonlocal position
        result = []
        for _ in range(u2()):
            u2()
            name, descriptor = utf8(u2()), utf8(u2())
            constant = None
            for _ in range(u2()):
                attribute = utf8(u2())
                length = struct.unpack_from(">I", data, position)[0]
                position += 4
                if attribute == "ConstantValue":
                    constant = pool[struct.unpack_from(">H", data, position)[0]][1]
                position += length
            result.append((name, descriptor, constant))
        return result

    return owner, references, members(), members()


def check_reference(owner, name, descriptor, location):
    if owner.startswith(API):
        assert name not in REMOVED_METHODS, f"{location}: removed API member {owner}.{name}"
        if owner in {WINDOW + "DesktopWindowAction", WINDOW + "DesktopWindowControl"}:
            assert name not in REMOVED_ENUMS, f"{location}: removed API command {name}"
        if name == "<init>" and owner == WINDOW + "DesktopWindowOptions":
            assert descriptor in OPTIONS_CTORS, f"{location}: old options constructor {descriptor}"
        if name == "<init>" and owner == WINDOW + "DesktopWindowState":
            assert descriptor == STATE_CTOR, f"{location}: old state constructor {descriptor}"
    assert not owner.endswith("DesktopPackets$DesktopSessionPinPayload"), f"{location}: removed ghost pin packet"


def archive_entries(archive, prefix=""):
    for name in archive.namelist():
        if name.endswith(".jar"):
            with zipfile.ZipFile(io.BytesIO(archive.read(name))) as nested:
                yield from archive_entries(nested, prefix + name + "!")
        elif name.endswith(".class") or name.endswith(".json"):
            yield prefix + name, archive.read(name)


def audit(path, kind):
    count = references = 0
    checked = set()
    with zipfile.ZipFile(path) as jar:
        for filename, payload in archive_entries(jar):
            if filename.endswith(".class"):
                owner, calls, fields, methods = class_info(payload)
                count += 1
                references += len(calls)
                for target, name, descriptor in calls:
                    check_reference(target, name, descriptor, f"{path.name}!{filename}")
                if owner.startswith(API):
                    assert kind == "core", f"{path}: embeds Core API {owner}"
                    checked.add(owner)
                    for name, descriptor, _ in fields + methods:
                        check_reference(owner, name, descriptor, f"{path.name}!{filename}")
                if owner.endswith("client/InventoryDesktopScreen$InventoryWindow"):
                    assert not {"minimized", "ghosted"} & {name for name, _, _ in fields}
                if owner.endswith("client/SaltsInventoryConfig$ConfigFile"):
                    assert not {"minimizableWindows", "enableGhostPins", "ghostWindowOpacity"} & {name for name, _, _ in fields}
                if owner == API + "integration/DesktopIntegrationContext":
                    assert next(value for name, _, value in fields if name == "API_VERSION") == 6
                assert not owner.endswith("DesktopPackets$DesktopSessionPinPayload")
            elif kind == "core" and re.match(r"assets/salts_inventory_update/lang/[^/]+\.json$", filename):
                labels = json.loads(payload)
                assert not any(re.search(r"minimiz|ghost_pin|ghost_window", key, re.I) for key in labels), f"{path}: retired UI labels"
            elif kind == "core" and filename in {"fabric.mod.json", "META-INF/mods.toml", "META-INF/neoforge.mods.toml", "pack.mcmeta"}:
                assert b"ghost-pin" not in payload.lower(), f"{path}: obsolete feature in mod description"
        if kind == "core":
            assert {WINDOW + "DesktopWindowOptions", WINDOW + "DesktopWindowState", WINDOW + "DesktopWindowAction",
                WINDOW + "DesktopWindowControl", API + "integration/DesktopIntegrationContext"} <= checked
    return {"jar": str(path), "kind": kind, "classes": count, "memberReferences": references}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--dist", type=Path, default=ROOT / "dist")
    parser.add_argument("--summary", type=Path)
    args = parser.parse_args()
    results = []
    for kind in ("core", "compatibility", "controlify"):
        jars = sorted((args.dist / kind).glob("*.jar"))
        assert len(jars) == 12, f"Expected twelve {kind} production JARs, found {len(jars)}"
        results.extend(audit(jar, kind) for jar in jars)
    report = {"status": "PASS", "apiVersion": 6, "jars": results}
    if args.summary:
        args.summary.parent.mkdir(parents=True, exist_ok=True)
        args.summary.write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
    print(f"PASS: {len(results)} production JARs; {sum(result['classes'] for result in results):,} classes; "
        f"{sum(result['memberReferences'] for result in results):,} member references; no removed window API callers.")


if __name__ == "__main__":
    main()
