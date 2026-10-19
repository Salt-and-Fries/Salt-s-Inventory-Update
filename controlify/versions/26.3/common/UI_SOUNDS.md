# Controller inventory sounds — 26.3

Every supported **Controlify companion** target owns this sound palette, its
settings and its playback rules. Native adapters cover Minecraft 1.20.1, 1.21.1,
1.21.11, 26.1.2, 26.2 and 26.3 and their supported loaders. The sound overhaul adds
no audio implementation or resources to Core or Extended. The optional Extended
JAR is not needed for these sounds. Editable assets remain in this 26.3 source
directory and are shared into all companion builds.

**Inventory sounds are off by default.** New installations and Restore defaults
use the Off preset. Choose an audible preset below to opt in. Existing saved
audible presets and Custom mixes are preserved.

The palette applies the recommendations in the
[UI sound research PDF](../../../../output/pdf/ui-sound-research/salts-inventory-ui-sound-research.pdf):
small movement cues, distinct state changes, restrained action feedback and useful
silence. Opening and closing reference Minecraft's existing button-click event;
no Minecraft recording is copied into the companion. The other seven recordings
derive from Kenney's CC0 packs.
Build and native gameplay evidence are tracked separately in
[VALIDATION.md](../../../VALIDATION.md).

## Adjust the mix

Open **Options → Music & Sounds → Controller sounds**. These are ordinary native
Minecraft controls in a scrolling options list, with focus, tooltips and narration.

| Control | Behavior |
| --- | --- |
| Preset | **Off** is the default and mutes the whole palette. Quiet, Standard and Minimal opt in to sounds. Minimal keeps only Actions. Manual adjustments select Custom. |
| Inventory volume | Changes this companion's UI sound level; Off mutes the whole palette. |
| Navigation | Changes soft browsing, slot navigation, scrolling and optional cursor-hover feedback. |
| Windows | Changes window/wheel opening and closing, window switching, category/filter/tab changes, tucking and restoring. |
| Actions | Changes confirmation, observed crafting output and unavailable-action feedback. |
| Cursor hover | Optional controller-cursor feedback; off by default. All feedback requires inventory sounds to be enabled. |
| Cue / Play / Stop | Auditions one selected cue at the current mix. Starting another replaces the previous preview; Stop stops that preview. Play and Stop add no simultaneous vanilla button click. Muted cues remain silent. |
| Restore defaults | Returns to Off with inventory sounds and cursor-hover audio silent. |

Minecraft's **Master** volume governs every cue. The 26.x native **UI** level
also applies; older targets use Minecraft's Master-routed native UI playback.
This mixer does not alter other mods' audio or the world's sound categories.
Controls take effect immediately; Done saves the mix to
`config/salts_inventory_controlify_sounds.properties` in the instance's game
directory. The separate companion animation configuration remains independent.
Missing, unknown or Off presets remain muted even if the file contains a stale
positive inventory volume. An unreadable file also falls back to silence.

The preset multipliers are:

| Preset | Inventory | Navigation | Windows | Actions | Hover |
| --- | ---: | ---: | ---: | ---: | --- |
| Off (default) | Off | 30% | 65% | 80% | Off |
| Quiet | 70% | 30% | 65% | 80% | Off |
| Standard | 100% | 55% | 80% | 100% | Off |
| Minimal | 60% | Off | Off | 65% | Off |

These values multiply each cue's base gain as well as Minecraft's native volume
settings; they are not a measurement of perceived loudness or device volume.

## Event palette

All event IDs use the companion namespace. Opening and closing alias
`minecraft:ui.button.click`. The other seven events use short mono OGG files.
All nine play without looping through Minecraft's native UI playback, relative to
the listener; its category is UI on 26.x and Master on older targets.

| Event ID | Meaning |
| --- | --- |
| `salts_inventory_controlify:ui.navigate` | A changed controller selection, slot, scroll position or recipe; optional cursor hover. |
| `salts_inventory_controlify:ui.window_change` | A committed window/tab/category/filter change, sort request or cooking-load request. |
| `salts_inventory_controlify:ui.open` | Enter the controller workspace or a wheel; uses Minecraft's button click. |
| `salts_inventory_controlify:ui.close` | Close a window/workspace or leave a contextual wheel; uses Minecraft's button click. |
| `salts_inventory_controlify:ui.tuck` | Tuck the workspace. |
| `salts_inventory_controlify:ui.restore` | Restore tucked windows. |
| `salts_inventory_controlify:ui.confirm` | A committed UI action, such as a creative grant/clear/delete or focus swap. |
| `salts_inventory_controlify:ui.craft_complete` | Matching crafted output has been observed in the intended destination after an accepted craft request. |
| `salts_inventory_controlify:ui.unavailable` | An attempted action is unavailable; a red recipe or missing material on hover is silent. |

Recipe-wheel and scroll selection have **no movement cooldown**. Each changed
selection update stops the previous selection sound and starts the new one from
its beginning, even when the previous recording is still playing. Multiple changes
within one client tick coalesce to the latest selection; no old clicks queue up.
Ordinary slot/window navigation retains its shared **200 ms interval**.
A held control at the end of a scroll range, an unchanged selection, a neutral
stick and an idle wheel produce no repeat cue.
The gameplay carousel still rotates at five recipes per second when a bumper is
held; the audio policy does not change that control speed.

For gameplay feedback, each client tick retains one fresh candidate. Action feedback takes priority over
window transitions, which take priority over browsing. Replaced or expired cues
are discarded rather than played later. An action result also stops a lingering
navigation cue. A result or structural cue retains a brief 90 ms priority gap before
more selection sounds. Closing the workspace, changing worlds or disconnecting the
controller clears pending feedback. Visual highlights, quantities and error text
remain available with audio muted.

## What a crafting completion cue means

Before dispatching a craft request, the companion records the destination's count
of the selected output item, including its components, through the public
workspace API. After an accepted request, it waits for the same destination's
matching count to increase by at least one recipe output batch. A detected increase
produces one completion cue, including for Craft Stack; it does not play a fanfare
for each item or native batch.

This is a **client observation, not a Core server acknowledgment**. Other concurrent
changes can affect counts, and unsupported or ambiguous destinations cannot always
be observed. If no reliable increase arrives, the window identity changes, or the
observation expires, the companion stays silent. It does not invent a failure or
turn a locally accepted network request into a success claim. Preparing a furnace
uses a neutral request cue; it does not announce that cooking has finished.

## Replace or edit recordings

Runtime definitions are in
[`src/main/resources/assets/salts_inventory_controlify/sounds.json`](src/main/resources/assets/salts_inventory_controlify/sounds.json),
with recordings under the same asset root in `sounds/ui/`. A resource pack can
replace a file at the same path, for example:

```text
assets/salts_inventory_controlify/sounds/ui/navigate.ogg
```

To change the referenced filename instead, supply a normal resource-pack
`assets/salts_inventory_controlify/sounds.json` entry with `"replace": true` and
your own namespaced OGG path. Keep navigation replacements short so a five-Hz
sweep remains clear. Replacing audio does not change the companion's timing,
priority, gains or crafting observation rules.

The [editable audio sources](audio-source/README.md) retain the original OGG bytes,
licenses, source/archive hashes, transformation script and measured output
manifest. Those source originals stay outside production resources. The generated
CC0 notices and provenance manifest are included in the companion JAR under
`assets/salts_inventory_controlify/licenses/ui-sounds/`.

No subjective listening study is claimed by these implementation notes. The
retained source identities and numerical checks make the palette reproducible;
judging the mix with normal ambience, repeated browsing and narration remains a
listening task.
