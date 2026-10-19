"""Rebuild the 26.3 Controlify UI palette and native open/close event aliases.

Requires Python 3, numpy and soundfile (libsndfile with OGG/Vorbis support).
No normalisation, resampling, synthesis, pitch shift or randomisation is used.
Unchanged, checksum-verified derived recordings are retained byte-for-byte;
--reencode explicitly regenerates them. Minecraft audio is never copied.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import math
from pathlib import Path
import shutil

import numpy as np
import soundfile as sf


ROOT = Path(__file__).resolve().parent
ASSETS = ROOT.parent / "src/main/resources/assets/salts_inventory_controlify"


def sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def dbfs(value: float) -> float | None:
    return round(20 * math.log10(value), 6) if value > 0 else None


def build(reencode: bool = False) -> None:
    source = json.loads((ROOT / "sources.json").read_text(encoding="utf-8"))
    expected_recordings = {"navigate", "window_change", "tuck", "restore", "confirm", "craft_complete", "unavailable"}
    if len(source["assets"]) != 7 or {entry["name"] for entry in source["assets"]} != expected_recordings:
        raise ValueError("Exactly the seven retained CC0 recordings may be generated; open/close use native event aliases")
    previous = ROOT / "palette-manifest.json"
    old_records = {entry["name"]: entry for entry in json.loads(previous.read_text(encoding="utf-8"))["assets"]} if previous.is_file() else {}
    output = ASSETS / "sounds/ui"
    output.mkdir(parents=True, exist_ok=True)
    records = []
    sounds = {}
    for entry in source["assets"]:
        original = ROOT / entry["source_file"]
        if sha256(original) != entry["source_sha256"]:
            raise ValueError(f"Source checksum mismatch: {original}")
        data, rate = sf.read(original, dtype="float64", always_2d=True)
        if rate != 44100:
            raise ValueError(f"Unexpected source rate: {original}: {rate}")
        original_channels = data.shape[1]
        # Arithmetic downmix preserves mono compatibility; never pick one ear.
        data = np.mean(data, axis=1)
        if entry.get("keep_first_seconds") is not None:
            data = data[:round(entry["keep_first_seconds"] * rate)].copy()
        data *= 10 ** (entry["source_gain_db"] / 20)
        fade_in = min(round(entry["fade_in_seconds"] * rate), len(data))
        fade_out = min(round(entry["fade_out_seconds"] * rate), len(data))
        if fade_in > 1:
            data[:fade_in] *= np.sin(np.linspace(0, np.pi / 2, fade_in)) ** 2
        if fade_out > 1:
            data[-fade_out:] *= np.cos(np.linspace(0, np.pi / 2, fade_out)) ** 2
        target = output / f"{entry['name']}.ogg"
        old = old_records.get(entry["name"], {})
        if reencode or not target.is_file() or not entry.items() <= old.items() or sha256(target) != old.get("derived_sha256"):
            sf.write(target, data, rate, format="OGG", subtype="VORBIS")
        decoded, decoded_rate = sf.read(target, dtype="float64", always_2d=True)
        if decoded_rate != rate or decoded.shape != (len(data), 1):
            raise ValueError(f"Unexpected encoded geometry: {target}")
        if not np.all(np.isfinite(decoded)) or np.max(np.abs(decoded)) >= 1:
            raise ValueError(f"Invalid/clipped decoded audio: {target}")
        event = f"ui.{entry['name']}"
        sounds[event] = {
            "sounds": [{
                "name": f"salts_inventory_controlify:ui/{entry['name']}",
                "stream": False,
            }],
        }
        records.append({
            **entry,
            "event_id": f"salts_inventory_controlify:{event}",
            "derived_resource": f"assets/salts_inventory_controlify/sounds/ui/{target.name}",
            "derived_sha256": sha256(target),
            "derived_size_bytes": target.stat().st_size,
            "original_channels": original_channels,
            "sample_rate_hz": rate,
            "channels": 1,
            "frames": len(decoded),
            "duration_seconds": round(len(decoded) / rate, 6),
            "decoded_peak_dbfs": dbfs(float(np.max(np.abs(decoded)))),
            "decoded_rms_dbfs": dbfs(float(np.sqrt(np.mean(decoded * decoded)))),
            "last_1ms_peak_dbfs": dbfs(float(np.max(np.abs(decoded[-44:])))),
        })
    aliases = source.get("event_aliases", [])
    if {entry["name"] for entry in aliases} != {"open", "close"} or len(aliases) != 2:
        raise ValueError("The open and close events must be native aliases")
    for entry in aliases:
        if entry["type"] != "event" or entry["target_event"] != "minecraft:ui.button.click" or entry["redistributed_audio"] is not False:
            raise ValueError(f"Unexpected native UI alias: {entry}")
        sounds[f"ui.{entry['name']}"] = {
            "sounds": [{"name": entry["target_event"], "type": "event"}],
        }
        # Only these two exact obsolete runtime derivatives may be removed.
        # The CC0 authoring originals remain outside production resources.
        retired = output / f"{entry['name']}.ogg"
        if retired.name not in {"open.ogg", "close.ogg"} or retired.resolve().parent != output.resolve():
            raise ValueError(f"Unexpected retired asset path: {retired}")
        if retired.is_file():
            retired.unlink()
    (ASSETS / "sounds.json").write_text(json.dumps(sounds, indent=2) + "\n", encoding="utf-8")
    notices = ASSETS / "licenses/ui-sounds"
    notices.mkdir(parents=True, exist_ok=True)
    license_records = []
    for pack in source["packs"]:
        path = ROOT / pack["license_file"]
        if sha256(path) != pack["license_sha256"]:
            raise ValueError(f"License checksum mismatch: {path}")
        shutil.copyfile(path, notices / path.name)
        license_records.append({**pack, "packaged_notice": f"assets/salts_inventory_controlify/licenses/ui-sounds/{path.name}"})
    manifest = {
        "title": "Salt's Inventory Update Controlify 26.3 UI sound palette",
        "scope": "26.3 Controlify companion JAR only. No vanilla/core/extended event overrides.",
        "license": "CC0-1.0",
        "license_scope": "Seven redistributed derivative recordings only. event_aliases refer to Minecraft-installed recordings; no Minecraft recording is redistributed or licensed here.",
        "license_url": "https://creativecommons.org/publicdomain/zero/1.0/",
        "auditioned_by_researcher": False,
        "measurement_note": "Decoded sample peak and whole-file RMS; neither LUFS nor perceived loudness. Final engine family gains are additional.",
        "transformations": "Arithmetic stereo-to-mono downmix where needed; fixed attenuation; short equal-power edge fades; only craft_complete is shortened to a 140 ms prefix with a 24 ms end fade. No normalisation, resampling or pitch changes.",
        "tool_versions": {"numpy": np.__version__, "soundfile": sf.__version__, "libsndfile": sf.__libsndfile_version__},
        "packs": license_records,
        "assets": records,
        "event_aliases": aliases,
    }
    (ROOT / "palette-manifest.json").write_text(json.dumps(manifest, indent=2) + "\n", encoding="utf-8")
    # Keep provenance available to resource-pack authors inspecting the JAR.
    (notices / "palette-manifest.json").write_text(json.dumps(manifest, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({"events": len(sounds), "native_event_aliases": aliases, "files": [{"id": r["event_id"], "seconds": r["duration_seconds"], "peak_dbfs": r["decoded_peak_dbfs"], "tail_1ms_peak_dbfs": r["last_1ms_peak_dbfs"]} for r in records]}, indent=2))


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--reencode", action="store_true", help="Re-encode even unchanged verified derivatives instead of preserving their bytes")
    args = parser.parse_args()
    build(args.reencode)
