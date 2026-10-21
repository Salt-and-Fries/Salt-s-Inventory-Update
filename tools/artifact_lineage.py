"""Narrow evidence comparison for the final window-feature description correction."""
import hashlib
from zipfile import ZipFile


def core_description_match(release, staged):
    """Permit only deleting the obsolete ghost-pin claim; require all other bytes."""
    old = b"Move, pin, ghost-pin, resize, and snap"
    new = b"Move, pin, resize, and snap"
    mod_metadata = {"fabric.mod.json", "META-INF/mods.toml", "META-INF/neoforge.mods.toml"}
    metadata = mod_metadata | {"pack.mcmeta"}
    with ZipFile(staged) as before, ZipFile(release) as after:
        assert before.testzip() is None and after.testzip() is None
        assert set(before.namelist()) == set(after.namelist()), "Core archive inventory changed"
        changed = [name for name in before.namelist() if before.read(name) != after.read(name)]
        assert 1 <= len(changed) <= 2 and set(changed) <= metadata and len(set(changed) & mod_metadata) == 1, \
            f"Core changes beyond its description: {changed}"
        for member in changed:
            original, final = before.read(member), after.read(member)
            assert original.count(old) == 1 and original.replace(old, new, 1) == final, \
                "Core metadata changed beyond removal of the obsolete ghost-pin description"
    return {"mode": "core-description-only", "runtime_sha256": hashlib.sha256(staged.read_bytes()).hexdigest(),
        "final_sha256": hashlib.sha256(release.read_bytes()).hexdigest(), "changed_members": changed,
        "all_class_bytes_identical": True, "all_other_resource_bytes_identical": True}
