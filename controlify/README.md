# Salt's Inventory Controlify

This client mod is the third, independent Salt's Inventory Update artifact. Install
the matching rebuilt core JAR, the native Controlify build and YACL dependency
listed below, and this JAR. Fabric also requires the matching Fabric API and
Fabric Loader version declared by the artifact. The existing extended
compatibility JAR is optional.

The [illustrated feature guide](docs/salts-inventory-controlify-feature-guide.pdf)
documents the original controller presentation, control reference and installation
steps using verified Minecraft 1.21.1 Fabric images. The current 26.3 features and
screenshots are described below. See [current validation](VALIDATION.md) for the
expanded matrix and each loader's packaged gameplay verification.

Controller code lives exclusively in `controlify/`. The integration compiles only
against the core's public `com.salts_inventory_update.api` classes. Core runtime
classes and the Controlify/YACL dependencies remain separate mods; none are copied
into the controller JAR.

The matching rebuilt core for each Minecraft version supplies generic workspace API hooks for enumerating,
rendering and routing input to existing windows. It contains no controller layouts,
inputs or animations. Older core 0.1.4 archives without these hooks are insufficient;
use the core JAR produced by this branch's build together with the controller JAR.

## Controls

Opening a supported container or UI focuses player inventory on the left and places
the opened window on the right. Player inventory and Character remain available and
cannot be closed individually. Controlify's usual cursor, scrolling, select,
quick-move and take-half actions retain their configured bindings. The 26.3
overrides, including immediate side-by-side B close and right-stick scrolling,
are described in the feature section below.

| Input | Action |
| --- | --- |
| LB / RB | Select the previous / next window on the right |
| Hold LT | Show other open windows around the focused center window |
| Left stick while holding LT | Highlight the nearest radial window; bottom selects Tuck Away |
| Release LT | Put the highlighted window on the right, or tuck the workspace upward |
| A while holding LT | Swap the highlighted window into focus |
| Configured close-inventory button (normally B), in the 26.3 LT wheel | Immediately close only the highlighted closable window; keep the wheel and other windows open |
| Configured close-inventory button, while holding LT on older versions | Close the highlighted closable window on release |
| Configured close-inventory button (normally B), in 26.3 side-by-side view | Immediately close the workspace and all closable windows |
| Hold the configured close-inventory button, on older versions | Close all closable windows using Core's existing hold duration |
| Configured inventory button (normally Y), while tucked | Restore the same windows and sessions |

While either inventory mode is visible, the screen owns character and world input.
Tucking immediately restores normal gameplay. New supported containers join the same
workspace while tucked and remain out of view until inventory is restored. Quick-move
routes to the other displayed window; a UI without storage cannot receive a stack.

`config/salts_inventory_controlify.properties` contains `animations=true` by default.
Set `animations=false` and restart the client to make all presentation transitions
immediate. The controller presentation forces Core's inventory HUD/offhand arrangement
while active without overwriting the keyboard/mouse preference on disk.

## 26.3 feature test

The current feature pass applies only to Minecraft **26.3**, for Fabric and NeoForge.
Other versions retain their previous controller behavior. Replace both the matching
Core and Salt's Controlify companion when installing this pass.

The inventory fits its occupied rows and hotbar. Item tooltips use screen coordinates,
and the native crafting guide participates in pane sizing and positioning when its
presentation is enabled. Controller recipe-book windows use the contextual wheel
described below. The native Controlify cursor scales with the hovered pane's slots.

Tucked gameplay keeps a small Core-textured tab for every retained window, including
Inventory, Character, containers and client UI views. Tabs spread across the top and
compress as more windows join the workspace; they persist with animations disabled.

While holding LT, the configured inventory button (normally Y) sorts the focused
middle window into all eligible open storage containers through Core's regular sort
action. A second tap within **350 ms** uses the same force rules as Shift + sort:
matching items, shared item tags, the focused destination, then empty containers.
In side-by-side view, pressing the configured close button (normally B) immediately
closes the workspace and its closable container sessions. In the LT window radial,
a press closes only the highlighted closable window and leaves the wheel open.
Holding the close button has no extra action or progress timer in either 26.3 view.

Opening a container with LT keeps the workspace in side-by-side view until LT is
released. A fresh LT press then opens the window radial. Its focused center and
highlighted choice are substantially larger than the other previews. Side-by-side
view shows up to two faint neighboring windows above and below the selected pane;
LB/RB animate these previews into the selected position. Inactive previews use the
associated block's native item model, with suitable item fallbacks for Character,
inventory and client views. The focused and selected panes retain their full UI.
The LT wheel's Tuck option uses an editable two-state pixel sprite and grows or
shrinks with its animated selection.

D-pad navigation follows authored logical rows in native workstations, including
the three staggered brewing bottle slots, and covers creative tabs and item grids.
At a scrollable grid's visible edge, D-pad up/down scrolls one row before leaving
the grid. The **right stick** scrolls the list under the cursor with a deadzone and
controlled repeat speed. The left stick remains the native cursor control.

In the creative catalog, A adds one item directly to an available hotbar slot and
Y adds a legal full stack. Compatible stacks and empty slots are used without
replacing unrelated items; a full hotbar leaves the catalog item unchanged.

Hold the quick-action binding (RT by default) over a supported crafting, cooking,
or creative window to open its wheel. Controlify's native action lists show the
binding and its current action when that window is hovered. Container windows no
longer draw the recipe-book icon or its binding glyph. Core's normal mouse recipe
book remains available.

The native action guides include workspace selection, focus, close, sort,
navigation, scrolling, creative hotbar actions, and every wheel control. Short
one- or two-word labels fill centered horizontal rows along the bottom, wrapping
into two rows and scaling when necessary to preserve the inventory area. Stick
controls use one plain LS/RS glyph rather than four directional glyphs.
Hints change with the active mode and hovered window, use the actual configured
bindings, and respect Controlify's screen-guide visibility and scale preferences.
While tucked, the native inventory action becomes Restore; top-aligned
gameplay guides sit below the retained tabs and departing previews. Their normal
placement returns when leaving tucked mode.

D-pad left/right changes category; D-pad up toggles craftable recipes. Category icons
appear above the wheel with the selected category enlarged. The right stick moves
continuously through the outer recipe list, feeding individual cards through the
inner ring's bottom opening. The left stick chooses a visible recipe. Hold LB/RB to
rotate counterclockwise/clockwise at five recipes per second; release to stop.
Both recipe sticks use a larger dead zone (0.78 activation, 0.64 release) and ignore
inward spring movement, keeping the selected recipe stable when the stick is released.
Recipes are sorted by their displayed localized names. The outer ring marks the
start of each available A–Z group, skipping empty groups; categories and the
craftable filter rebuild these markers without changing continuous browsing.
Letters alternate inside and outside the ring with small connecting lines. Aiming
at a letter centers its first recipe at the top of the carousel. The highlighted
arc covers the visible range: it shrinks for larger catalogs and grows for smaller
ones. The separate outer marker is removed.

Ingredient counts and recipe availability include the player and all eligible open
containers. Holding the quick-action binding moves the actual supported container
to the right. The materials window and framed wheel emerge beneath it during the
same transition, settling left and center. The opposite paired window moves below
the ingredients on the left and stays live, so crafted output is visible in its
actual destination. Releasing the binding reverses both native windows and restores
ordinary inventory input immediately. Their displayed positions also govern input
and tooltips. The darkened world background remains constant through both wheels.

Crafting tables and the Character window retain their live 3x3 or 2x2 native slots
on the right. Opening a recipe wheel safely moves existing inputs and real furnace
output into eligible storage, leaving fuel in place. Hovering a recipe physically
places available ingredients into its native slots; missing ingredients appear as
translucent red previews. Changing recipes, releasing the wheel or interrupting it
returns unused ingredients to their original containers. A requests one native
craft into the opposite displayed storage window, or supplies needed fuel for
cooking. Native processing, recipe remainders, statistics and XP remain intact;
consumed ingredients are never restored. If storage cannot hold the existing
items, opening is rejected without changing them.

The wheel and ingredients panels use Core's ordinary dynamic window frame and
title renderer. The remaining right-trigger wheel artwork uses editable PNGs:
letter boxes, outer ring and a fixed-lighting highlighted section, recipe-card
availability/hover states, category/creative-tab states and the carousel opening.
Recipe cards pass behind the opening artwork; the ring, highlighted arc and letters
render above it. The separate arc boundary lines and texture are removed. Wheel
text uses dark colors against the window skin, and redundant success captions are
removed.
Native text and item models render separately. The
[asset guide](versions/26.3/common/CONTEXTUAL_RADIAL_ASSETS.md) documents dimensions
and alignment. Runtime sprites ship inside the companion JAR; generated originals,
the prompt set and exact-pixel exporter are available in the repository's
[wheel art sources](versions/26.3/common/art-source/wheel/).

Holding the same action over creative inventory opens its tab wheel. The active tab
remains highlighted separately from the hovered choice. Release the action to return
to side-by-side view. The carousel rendering keeps separate texture hooks for future
artwork. Its control instructions appear in the native guide lists.
Subtle native UI sounds accompany window selection and switching, tucking/restoring,
slot navigation, scrolling, carousel/category/filter changes and appropriate
confirm/close actions. Held input is rate limited, unchanged selections stay quiet,
and the game's Master/UI volume settings apply.
The [texture asset guide](versions/26.3/common/CONTEXTUAL_RADIAL_ASSETS.md) lists the
runtime PNG paths, source sizes and editable tuck sprite.

The root `build` refreshes production JARs for all twelve targets in
[`dist/core`](../dist/core/), [`dist/compatibility`](../dist/compatibility/) and
[`dist/controlify`](../dist/controlify/), with checksums in each directory.
The [combined latest archive](../dist/salts-inventory-all-latest.zip) groups all
36 production JARs by Minecraft version and loader. For this 26.3 feature pass,
use the matching 26.3 Core and controller companion; the compatibility extension
remains optional. Install the pinned upstream dependencies listed below separately.
The combined archive does not include gameplay screenshots.
See [validation](VALIDATION.md) for native game evidence and exact artifact hashes.

The input adapters use Controlify's native container screen processors and bindings.
Version-specific adapters handle Minecraft's native screen, input and rendering
APIs and the Controlify 2.x, 3.0 LTS and 3.5 families. Build checks resolve their
member references and mixin targets against the actual target compiler inputs
and pinned upstream JARs.

## Minecraft and loader targets

The build produces a distinct companion and matching Core JAR for all twelve
targets below. Build completion and successful gameplay verification are tracked
separately in [VALIDATION.md](VALIDATION.md); pending runtime rows are not claims
of completed gameplay testing.

| Minecraft | Loader | Pinned native controller | Pinned YACL |
| --- | --- | --- | --- |
| 1.20.1 | Fabric | [Controlify 2.0.4](https://modrinth.com/mod/controlify/version/sDAshEPv) | [3.6.6+1.20.1-fabric](https://modrinth.com/mod/1eAoo2KR/version/dvS5DjUA) |
| 1.20.1 | Forge | [Controlify: Forgified 2.1.9](https://modrinth.com/mod/G2K5sLls/version/Tzzphrug) | [3.6.6+1.20.1-forge](https://modrinth.com/mod/1eAoo2KR/version/sCWgXDYQ) |
| 1.21.1 | Fabric | [Controlify 3.0.1+lts](https://modrinth.com/mod/controlify/version/y9bu5RxH) | [3.8.2+1.21.1-fabric](https://modrinth.com/mod/1eAoo2KR/version/o3cDn8Vp) |
| 1.21.1 | NeoForge | [Controlify 3.0.1+lts](https://modrinth.com/mod/controlify/version/RqsNKKLK) | [3.8.2+1.21.1-neoforge](https://modrinth.com/mod/1eAoo2KR/version/7TVdVtxF) |
| 1.21.11 | Fabric | [Controlify 3.0.1+lts](https://modrinth.com/mod/controlify/version/qZW7FPjm) | [3.8.2+1.21.11-fabric](https://modrinth.com/mod/1eAoo2KR/version/pHWDw3Vc) |
| 1.21.11 | NeoForge | [Controlify 3.0.1+lts](https://modrinth.com/mod/controlify/version/rrwWAirw) | [3.8.2+1.21.11-neoforge](https://modrinth.com/mod/1eAoo2KR/version/HZs9mrWM) |
| 26.1.2 | Fabric | [Controlify 3.5.3+mc26.1](https://modrinth.com/mod/controlify/version/CCtUQSV4) | [3.9.7+26.1-fabric](https://modrinth.com/mod/1eAoo2KR/version/IODousKT) |
| 26.1.2 | NeoForge | [Controlify 3.5.3+mc26.1](https://modrinth.com/mod/controlify/version/CCtUQSV4) | [3.9.7+26.1-neoforge](https://modrinth.com/mod/1eAoo2KR/version/B1mfgQmD) |
| 26.2 | Fabric | [Controlify 3.5.3+mc26.2](https://modrinth.com/mod/controlify/version/VDw4mGkG) | [3.9.7+26.2-fabric](https://modrinth.com/mod/1eAoo2KR/version/DoR7RYgi) |
| 26.2 | NeoForge | [Controlify 3.5.3+mc26.2](https://modrinth.com/mod/controlify/version/VDw4mGkG) | [3.9.7+26.2-neoforge](https://modrinth.com/mod/1eAoo2KR/version/FfvwIZmI) |
| 26.3 | Fabric | [Controlify 3.5.3+mc26.3](https://modrinth.com/mod/controlify/version/fo76vL20) | [3.9.7+26.3-fabric](https://modrinth.com/mod/1eAoo2KR/version/s9SjoFu1) |
| 26.3 | NeoForge | [Controlify 3.5.3+mc26.3](https://modrinth.com/mod/controlify/version/fo76vL20) | [3.9.7+26.3-neoforge](https://modrinth.com/mod/1eAoo2KR/version/GL24RgOP) |

The Forge target uses the published native Forge port, Controlify: Forgified,
with Forge YACL. It also requires LexManos's [Mixin Args 1.0](https://www.curseforge.com/minecraft/mc-mods/mixin-args/files/6488404)
as a separate client mod. That unchanged dependency supplies the synthetic Args
package needed by the older Forge module layer; see [Mixin issue 584](https://github.com/SpongePowered/Mixin/issues/584).
The 26.x controller pins are universal upstream JARs that
declare native Fabric and NeoForge entrypoints. Each Salt companion remains
specific to its Minecraft version and loader. [targets.json](targets.json)
records immutable download URLs, version IDs and SHA-512 hashes for every pin.

Java minimums are 17 for 1.20.1, 21 for 1.21.1/1.21.11, and 25 for 26.x.
Fabric Loader minimums are 0.18.2 for 1.20.1/1.21.1, 0.19.2 for 1.21.11,
and **0.19.5 for every 26.x target**. [Loader 0.19.5](https://github.com/FabricMC/fabric-loader/releases/tag/0.19.5)
includes [MixinExtras 0.5.5](https://github.com/LlamaLad7/MixinExtras/releases/tag/0.5.5),
whose redirect-annotation compatibility fix is required by the original upstream
mods in these 26.x Fabric profiles.

The 1.20.1 Fabric artifact requires **exactly Controlify 2.0.4**. Inspection and
startup verification found an invalid MerchantScreen scroll-local injection in
the published 2.1.0–2.1.2 Fabric builds; 2.0.4 avoids that injection. The native
Forge port has its own corrected 2.1.9 implementation.

Minecraft 1.20.1 retains Core's existing blank large Loom banner preview. Native
pattern selection, result slots and authenticated output transfers work; the
rendered banner preview is verified on the modern targets.

## Build and development

Run these commands from the repository root:

```powershell
.\gradlew.bat :controlify:model:test
.\gradlew.bat build
.\gradlew.bat buildControlify
.\gradlew.bat collectControlifyJars
.\gradlew.bat collectControlifyTestJars
.\gradlew.bat :controlify:1.21.1:fabric:runClient
.\gradlew.bat :controlify:1.21.1:neoforge:runClient
```

`buildControlify` builds all twelve supported companion JARs and matching Core
archives, tests the controller workspace model and all six versions' real Core
workspace/transfer hooks, and checks source, compiler classpath, production
bytecode and native member/mixin boundaries.
`collectControlifyJars` places verified artifacts in `dist/controlify/`. The normal
root `build` produces all 36 Core, extension and controller JARs and keeps
`dist/core`, `dist/compatibility`, and `dist/controlify` separate. Development
client tasks launch the core and Controlify as separate mods.
See [validation evidence](VALIDATION.md) for the final test totals, packaged
gameplay scenarios, artifact isolation checks and SHA-256 hashes.

`collectControlifyTestJars` gathers the rebuilt matching core and controller JARs
in separate folders under `dist/controlify-test/`. The public workspace API added
for this integration requires this rebuilt core. For disposable in-world QA,
use the separate test-only initializer and packaged launch profile:

```powershell
.\gradlew.bat -I controlify/functional-tests/controlify-qa.init.gradle :controlify:1.21.1:fabric:runPackagedControlifyQa
python tools/report-controlify-validation.py
python tools/verify-controlify-runtime.py --summary build/controlify-runtime-verified.json
```

That profile removes project implementation directories from the game's classpath
and loads the production core/controller JARs from its isolated `mods` folder.
The harness compiles in its own source set and is excluded from all release JARs.
Use the corresponding `:controlify:<minecraft>:<loader>:runPackagedControlifyQa`
task for each target. Fabric and NeoForge profiles stage the original published
Controlify and YACL JARs with their nested dependencies. Each QA launch clears only its previous
report and PNG screenshots; preserved attempt folders remain available.

Legacy Forge QA uses [ModDev's standard SRG-to-named mapping](https://github.com/neoforged/ModDevGradle/blob/main/LEGACY.md#remapping-mod-dependencies)
on copies of the core, companion and upstream JARs for its development Minecraft.
`original-mods/` preserves the original inputs; `forge-remap-lineage.json` records
input/output hashes and the tool/mapping hashes. Validation checks resource and
nested-library lineage after conversion. Release installations use the original
release JARs and pinned upstream downloads. The separately installed Mixin Args
JAR is staged unchanged after QA remapping and is excluded from Salt's artifacts.

The disposable fixture uses a synthetic Controlify driver, real input bindings
and Controlify's supported mixed input mode. A QA-only hook prepares the cursor
from the last rendered layout immediately before native binding consumption;
Controlify's native processor and real server transfers perform the actions.
Physical controller hardware is not exercised by this fixture.

`WorkspaceScissorMixin` adapts Minecraft 1.20.1 and 1.21.1's native clipping while a controller
window is transformed. It maps all four rectangle corners through the current GUI
pose and rounds the clipping bounds outward. A nested render scope limits the
adaptation to controller windows; other screens retain their native clipping.
The pure tests cover transformed bounds, rotation, reflection, fractional edges,
empty clips, nested scopes, and exception cleanup. Minecraft 1.21.11 and 26.x use
native pose-aware clipping and queued GUI rendering. Companion render adapters
capture each window's opacity for its queued native draw states; pure tests cover
scope cleanup and independent per-state alpha.
