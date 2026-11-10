"""Build minimal capture cohorts from the original audit's local release pins.

No upstream release is upgraded and no production artifact is altered. Required
dependencies are resolved from the same audited pack before considering another
pack on the identical Minecraft/loader target. Missing/conflicting dependencies
remain explicit planning errors, not silently removed mods.
"""
from __future__ import annotations

import collections
import argparse
import hashlib
import io
import json
from pathlib import Path
import tomllib
import zipfile

ROOT = Path(__file__).resolve().parents[1]
AUDIT = ROOT / "build/ui-modpack-audit-20261025"
OUT = ROOT / "build/remaining-ui-audit"
PACK_TARGET = {"EGs3lC8D": ("1.20.1", "fabric"), "i4XHCd7Q": ("1.20.1", "fabric"),
               "shFhR8Vx": ("1.20.1", "fabric"), "4BV47HRn": ("1.20.1", "forge"),
               "t1tOiUHZ": ("1.21.1", "neoforge")}
PACK_ORDER = list(PACK_TARGET)
PROVIDED = {"minecraft", "java", "fabricloader", "fabric", "fabric-api", "forge", "neoforge"}


def load(path):
    return json.loads(path.read_text(encoding="utf-8-sig"))


def metadata(data, loader):
    ids, required, nested = set(), set(), []
    primary_ids = set()
    with zipfile.ZipFile(io.BytesIO(data)) as jar:
        names = set(jar.namelist())
        if loader == "fabric":
            if "fabric.mod.json" not in names:
                return ids, required, nested
            doc = json.loads(jar.read("fabric.mod.json"), strict=False)
            ids.add(doc["id"])
            ids.update(doc.get("provides", []))
            primary_ids.update(ids)
            required.update(doc.get("depends", {}))
            for entry in doc.get("jars", []):
                member = entry["file"]
                nested_data = jar.read(member)
                child_ids, child_required, grandchildren = metadata(nested_data, loader)
                ids.update(child_ids)
                required.update(child_required)
                nested.append({"member": member, "ids": sorted(child_ids),
                               "sha512": hashlib.sha512(nested_data).hexdigest()})
                nested.extend(grandchildren)
        else:
            resource = "META-INF/neoforge.mods.toml" if "META-INF/neoforge.mods.toml" in names else "META-INF/mods.toml"
            if resource not in names:
                return ids, required, nested
            doc = tomllib.loads(jar.read(resource).decode("utf-8-sig"))
            ids.update(mod["modId"] for mod in doc.get("mods", []))
            primary_ids.update(ids)
            for rows in doc.get("dependencies", {}).values():
                if not isinstance(rows, list):
                    continue
                for dep in rows:
                    if not isinstance(dep, dict):
                        continue
                    if dep.get("mandatory") or dep.get("type") == "required":
                        required.add(dep["modId"])
            if "META-INF/jarjar/metadata.json" in names:
                for entry in json.loads(jar.read("META-INF/jarjar/metadata.json")).get("jars", []):
                    member = entry["path"]
                    child_ids, child_required, grandchildren = metadata(jar.read(member), loader)
                    ids.update(child_ids)
                    required.update(child_required)
                    nested.append({"member": member, "ids": sorted(child_ids),
                                   "sha512": hashlib.sha512(jar.read(member)).hexdigest()})
                    nested.extend(grandchildren)
    # Keep dependencies even when a nested placeholder claims to provide them
    # (e.g. owo-sentinel). The native pack's real standalone library must still
    # be staged; an embedded older copy can also have an insufficient version.
    return ids, required - primary_ids, nested


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--preserve-cohort", action="append", default=[],
                        help="Do not rewrite a fixture already used for gameplay captures")
    parser.add_argument("--shared-inventory-only", action="store_true",
                        help="Prepare separate cohorts for reviewed consumers of vanilla/shared inventory menus")
    args = parser.parse_args()
    inventory = load(ROOT / "output/remaining-ui-audit/inventory.json")
    workflow_path = ROOT / "output/remaining-ui-audit/additional-inventory-workflows.json"
    shared_workflows = {e["entry"]:e for e in load(workflow_path)["entries"]} if workflow_path.exists() else {}
    packs = load(AUDIT / "packs.json")
    files = load(AUDIT / "files.json")
    # files.json also includes modpack override JARs, not just index downloads.
    OUT.mkdir(parents=True, exist_ok=True)
    cached_metadata_path = OUT / "binary-metadata.json"
    previous = load(cached_metadata_path) if cached_metadata_path.exists() else {}
    retained_metadata = previous.get("artifacts",{}) if previous.get("schema")==2 else {}
    cache, metadata_errors = {}, []
    choices = collections.defaultdict(list)
    for sha, source in files.items():
        if not source["path"].endswith(".jar") or "/.connector/" in source["path"]:
            continue
        path = AUDIT / "jars" / (sha + ".jar")
        for pack_id in source["packs"]:
            if pack_id not in PACK_TARGET:
                continue
            mc, loader = PACK_TARGET[pack_id]
            key = sha, loader
            if key not in cache:
                if not path.exists():
                    continue
                cached_key = sha + ":" + loader
                if cached_key in retained_metadata:
                    cache[key] = retained_metadata[cached_key]
                    for mod_id in cache[key]["mod_ids"]:
                        choices[mc, loader, mod_id].append((pack_id, cache[key]))
                    continue
                data = path.read_bytes()
                assert hashlib.sha1(data).hexdigest() == sha, path
                try:
                    ids, required, nested = metadata(data, loader)
                except (tomllib.TOMLDecodeError, ValueError, KeyError) as error:
                    metadata_errors.append({"sha1":sha, "path":source["path"], "loader":loader, "error":str(error)})
                    ids, required, nested = set(), set(), []
                cache[key] = {"sha1": sha, "sha512": hashlib.sha512(data).hexdigest(),
                              "path": str(path.relative_to(ROOT)), "filename": Path(source["path"]).name,
                              "original_path": source["path"], "mod_ids": sorted(ids),
                              "required_mod_ids": sorted(required), "nested": nested}
                retained_metadata[cached_key] = cache[key]
            artifact = cache[key]
            for mod_id in artifact["mod_ids"]:
                choices[mc, loader, mod_id].append((pack_id, artifact))
    cached_metadata_path.write_text(json.dumps({"schema":2,"artifacts":retained_metadata}, indent=2) + "\n", encoding="utf-8")
    (OUT / "metadata-errors.json").write_text(json.dumps(metadata_errors, indent=2) + "\n", encoding="utf-8")
    for pin_path in (OUT / "extra-dependencies").glob("*.json"):
        pin = load(pin_path)
        data = (ROOT / pin["path"]).read_bytes()
        assert hashlib.sha512(data).hexdigest() == pin["sha512"], pin_path
        ids, required, nested = metadata(data, pin["loader"])
        artifact = {**pin, "mod_ids":sorted(ids), "required_mod_ids":sorted(required), "nested":nested}
        cache[pin["sha1"], pin["loader"]] = artifact
        for mod_id in ids:
            choices[pin["minecraft"], pin["loader"], mod_id].append(("additional-required-dependency",artifact))
    roots = []
    for entry in inventory["entries"]:
        if entry["delivered"] or not (entry["container_screens"] or entry["menus"] or entry["entry"] in shared_workflows):
            continue
        if args.shared_inventory_only and entry["entry"] not in shared_workflows:
            continue
        # Lowest supported audited target first; preserve additional UI designs in
        # the inventory for a subsequent cross-version difference review.
        candidates = []
        for release in entry["releases"]:
            for pack in release["pack_ids"]:
                if pack in PACK_TARGET:
                    candidates.append((PACK_ORDER.index(pack), pack, release))
        if not candidates:
            raise ValueError("No audited runtime for " + entry["title"])
        _, pack, release = min(candidates, key=lambda x:x[0])
        mc, loader = PACK_TARGET[pack]
        artifact = cache.get((release["sha1"], loader))
        if artifact is None:  # Nested API mod: install its original enclosing JAR.
            root_path = release["original_path"].split("!/")[0]
            artifact = next((a for (s,l),a in cache.items() if l == loader and a["original_path"] == root_path), None)
        roots.append({"entry": entry["entry"], "title": entry["title"], "minecraft": mc, "loader": loader,
                      "pack_id": pack, "release": release, "artifact": artifact})
    cohorts = []
    for pack in PACK_ORDER:
        selected_roots = [root for root in roots if root["pack_id"] == pack]
        if not selected_roots:
            continue
        mc, loader = PACK_TARGET[pack]
        selected, errors = {}, []
        queue = [r["artifact"] for r in selected_roots if r["artifact"]]
        for r in selected_roots:
            if r["artifact"] is None:
                errors.append({"entry": r["entry"], "reason": "Root JAR could not be resolved"})
        while queue:
            artifact = queue.pop(0)
            if artifact["sha1"] in selected:
                continue
            selected[artifact["sha1"]] = artifact
            for dep in artifact["required_mod_ids"]:
                if dep in PROVIDED or (loader == "fabric" and dep.startswith("fabric-") and dep != "fabric-language-kotlin"):
                    continue
                def is_primary(a):
                    nested_ids = {mod for child in a.get("nested", []) for mod in child["ids"]}
                    return dep in set(a["mod_ids"]) - nested_ids
                if any(is_primary(a) for a in selected.values()) or any(is_primary(a) for a in queue):
                    continue
                candidates = choices.get((mc, loader, dep), [])
                # A feature mod can bundle a library. Installing that whole
                # feature mod merely to obtain the library changes the cohort.
                # Prefer the pack's actual standalone dependency artifact.
                primary_same_pack = [a for p,a in candidates if p == pack and is_primary(a)]
                primary_elsewhere = [a for p,a in candidates if is_primary(a)]
                same_pack = [a for p,a in candidates if p == pack]
                available = primary_same_pack or primary_elsewhere or same_pack or [a for p,a in candidates]
                # If a library exists only nested, let the native loader resolve
                # it. If the audited pack has a standalone release as well,
                # stage that too: its newer version may satisfy another root's
                # minimum even when an older embedded copy is already present.
                embedded_present = any(dep in a["mod_ids"] for a in selected.values()) or any(dep in a["mod_ids"] for a in queue)
                if embedded_present and not (primary_same_pack or primary_elsewhere):
                    continue
                if not available:
                    errors.append({"from": artifact["filename"], "missing_required_mod_id": dep})
                    continue
                queue.append(available[0])
        provided = collections.defaultdict(list)
        for a in selected.values():
            for mod in a["mod_ids"]:
                provided[mod].append(a["filename"])
        duplicates = {mod: names for mod,names in provided.items() if len(names)>1}
        # Duplicate embedded libraries can be resolved by the native loader; root
        # duplicate detection is still recorded for runtime review, not hidden.
        name = f"{mc}-{loader}-{pack.lower()}"
        if args.shared_inventory_only:
            name += "-shared-inventory"
        cohort = {"name": name, "minecraft": mc, "loader": loader, "scenario": "extension-empty",
                  "pack_id": pack, "scope": "Native versus shipping Core only; Compatibility and Controlify absent",
                  "roots": [{k:v for k,v in r.items() if k != "artifact"} for r in selected_roots],
                  "artifacts": [{**a, "coordinate": "audited-local:" + a["sha1"]} for a in selected.values()],
                  "planning_errors": errors, "duplicate_provided_ids": duplicates}
        path = OUT / "fixtures" / (name + ".json")
        path.parent.mkdir(parents=True, exist_ok=True)
        if name in args.preserve_cohort and path.exists():
            cohort = load(path)
            selected_roots = cohort["roots"]
            selected = {a["sha1"]:a for a in cohort["artifacts"]}
            errors = cohort["planning_errors"]
            duplicates = cohort["duplicate_provided_ids"]
        path.write_text(json.dumps(cohort, indent=2) + "\n", encoding="utf-8")
        cohorts.append({"name": name, "path": str(path.relative_to(ROOT)), "roots": len(selected_roots),
                        "artifacts": len(selected), "planning_errors": errors, "duplicate_id_count":len(duplicates)})
    (OUT / "cohorts.json").write_text(json.dumps(cohorts, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(cohorts, indent=2))


if __name__ == "__main__":
    main()
