# Controlify UI sound source

This is the editable source for **all supported Controlify companions'** UI palette. Its canonical location remains under `versions/26.3`; the build shares the generated companion assets with every target. It is outside `src/main/resources`, so the source originals and Python tool are not packaged in any production JAR. The generated assets live exclusively in `assets/salts_inventory_controlify`; they override no Minecraft, Core, or Extended events.

The palette follows the UI sound research in `output/pdf/ui-sound-research/salts-inventory-ui-sound-research.pdf`, with later user listening feedback: keep repeated movement tiny, restart the selection cue during fast browsing, reserve a separate completion cue for a completed action, and use refusal feedback only for an attempted unavailable action. Opening and closing now reference Minecraft's classic button click. Seven other recordings retain their approved bytes. **No controlled listening study is claimed.**

| Stable event suffix | Source | Duration | Intended role |
| --- | --- | ---: | --- |
| `ui.navigate` | Interface Sounds / `tick_001.ogg` | 23.220 ms | Changed recipe, slot, or category; quiet repeated movement |
| `ui.window_change` | Interface Sounds / `select_001.ogg` | 43.288 ms | Committed focus/window/tab selection |
| `ui.open` | Native event `minecraft:ui.button.click` | Game-provided | Enter a wheel or panel once |
| `ui.close` | Native event `minecraft:ui.button.click` | Game-provided | Leave a wheel or panel once |
| `ui.tuck` | Interface Sounds / `minimize_003.ogg` | 211.859 ms | Enter the tucked state once |
| `ui.restore` | Interface Sounds / `maximize_003.ogg` | 211.859 ms | Restore the expanded state once |
| `ui.confirm` | UI Audio / `click1.ogg` | 93.832 ms | A neutral committed UI action, not proof a craft completed |
| `ui.craft_complete` | Interface Sounds / `confirmation_001.ogg` | 140.000 ms | One observed completed craft transaction, including a stack |
| `ui.unavailable` | Interface Sounds / `error_004.ogg` | 102.698 ms | One refused press; never missing-material hover/idle feedback |

All companion IDs use the `salts_inventory_controlify:` namespace. The two open/close definitions reference a Minecraft event with `type: event`; no Minecraft recording is copied. The runtime owns admission, deduplication, event priority, engine/family gain and the native UI sound bus. Fast recipe/scroll changes stop and restart the 23 ms selection tick without a movement cooldown. Ordinary navigation retains its 5 Hz limit.

## Source rights and transformations

Both packs are by Kenney and distributed under **Creative Commons Zero (CC0-1.0)**. Source pages are <https://kenney.nl/assets/interface-sounds> and <https://kenney.nl/assets/ui-audio>. Their original `License.txt` bytes are retained in `licenses/`, with archive/member/checksum details in `sources.json`. Original OGG bytes are retained unchanged in `originals/`, including the two retired open/close originals for history. CC0 applies to the seven redistributed derivatives; it does not license Minecraft's game-provided audio. The Python tool follows the repository's code license. No Minecraft, Bedrock, legacy console or other mod recording is redistributed.

Each output is a 44.1 kHz mono Vorbis OGG. Stereo sources use an arithmetic channel average. All receive fixed -3 dB source attenuation, except the denser craft cue, which receives -6 dB. Short sine-squared/cosine-squared edge fades reduce cut-edge transients. The craft cue retains the first 140 ms of its 290 ms source and uses a 24 ms fade out; all other outputs retain the source frame count. There is no resampling, pitch shift, normalisation or fabricated spatial/stereo spread. Exact fade durations, source hashes, output hashes and decoded measurements are in `palette-manifest.json`.

Peak/RMS figures are digital sample measurements, not perceived loudness, LUFS, safe device settings, or an assertion that all sounds are equally loud. The runtime applies additional gain. Compare the chosen mix in a real 5 Hz sweep, with narration and normal world ambience, before making a subjective audio-quality claim.

## Rebuild

Install Python packages `numpy` and `soundfile` with a libsndfile build supporting OGG/Vorbis, then run from this directory:

```text
python build_palette.py
```

The tool verifies every retained original and license checksum before using it. It preserves the seven verified OGG bytes by default and writes `sounds.json` with the two native event aliases, unchanged packaged license notices, and both copies of the provenance manifest. It decodes each result and rejects invalid channels, rate, frame count, nonfinite samples or clipping. Use `--reencode` only when intentionally regenerating recordings: Vorbis container serial numbers or encoder versions can change byte hashes. The regenerated manifest records the resulting bytes.

## Replace sounds with a resource pack

The easiest replacement preserves the same asset path, for example:

```text
assets/salts_inventory_controlify/sounds/ui/navigate.ogg
```

Use the normal resource-pack metadata for the installed Minecraft version. Supply an OGG Vorbis recording, preferably mono and short for a repeated event. The fixed event IDs remain usable by per-event sound mixers. To point an event at a differently named file, provide your own `assets/salts_inventory_controlify/sounds.json` entry:

```json
{
  "ui.navigate": {
    "replace": true,
    "sounds": [{ "name": "my_pack:ui/navigation", "stream": false }]
  }
}
```

Place that recording at `assets/my_pack/sounds/ui/navigation.ogg`. Replacing a recording does not change runtime frequency limits, gains, visual feedback or transaction semantics. Do not turn a navigation replacement into a long fanfare that overlaps on every detent.
