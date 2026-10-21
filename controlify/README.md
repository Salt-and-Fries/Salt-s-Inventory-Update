# Salt's Inventory Controlify

This client mod is the third, independent Salt's Inventory Update artifact. Install
the matching rebuilt core JAR, the native Controlify build and YACL dependency
listed below, and this JAR. Fabric also requires the matching Fabric API and
Fabric Loader version declared by the artifact. The existing extended
compatibility JAR is optional.

The [illustrated feature guide](docs/salts-inventory-controlify-feature-guide.pdf)
documents the original controller presentation, control reference and installation
steps using verified Minecraft 1.21.1 Fabric images. The current shared controller features are described below; screenshots
from 26.3 illustrate the presentation used across the supported versions. See [current validation](VALIDATION.md) for the
expanded matrix and each loader's packaged gameplay verification.

Controller code lives exclusively in `controlify/`. The integration compiles only
against the core's public `com.salts_inventory_update.api` classes. Core runtime
classes and the Controlify/YACL dependencies remain separate mods; none are copied
into the controller JAR.

The matching rebuilt core for each Minecraft version supplies generic workspace API hooks for enumerating,
rendering and routing input to existing windows. It contains no controller layouts,
inputs or animations. Older core 0.1.4 archives without these hooks are insufficient;
use the core JAR produced by this branch's build together with the controller JAR.

## Release version

The companion has its own release version, set by `controlifyVersion` in the root
`gradle.properties`. The current version is **0.1.1**. Production filenames end in
`-0.1.1.jar`, for example `salts_inventory_controlify-26.3-Fabric-0.1.1.jar`.
The embedded mod version uses the same label on every supported game and loader.

The companion requires **Core 0.1.5** on every target. The optional Compatibility
extension is **0.1.2** and requires the same Core release. The companion's Core
dependency is generated independently of its own release label. Use matching
Minecraft/loader targets. Older published companions with an exact Core 0.1.4
dependency do not accept the newer Core releases.

Core's regular-window tutorial does not automatically open while this alternative
workspace presentation is installed, including before a controller is connected.
Manual help remains available, and removing the companion restores Core's normal
first-run tutorial behavior if help has never been opened.

## Playtest controls and cleanup

**Controls > Inventory controls** offers independent Hold/Toggle modes for the window
wheel and quick actions. Both default to Hold. In Toggle mode, press the trigger again
to finish selecting a window or close quick actions; releasing it keeps the wheel open.

Closing the whole controller workspace returns the carried stack and Character crafting
inputs to player storage, using vanilla ground drops when storage is full. Tucking the
workspace or closing one container keeps the shared cursor intact. Quick-moving fuel
into a paired vanilla furnace uses its native fuel slot and does not spill into the
input slot when the fuel slot is full.

A recipe wheel keeps its recipe positions throughout that opening, while refreshing
availability and ingredient counts. Changing category/filter or reopening takes a fresh
filtered list. This prevents crafting from moving the ring underneath the cursor.

Active status effects appear in a compact tray below the displayed Character window.
Countdowns use dark-grey text without a shadow; hovering an icon shows its name,
level and remaining time. The tray does not reduce Character's size. It wraps or
uses an overflow tooltip when space is limited, and can use the space above the
window when a complete row cannot fit below. It hides with Character's icon preview.

These builds use desktop protocol **5**. Replace Core on both client and server with the
matching rebuilt JAR; older protocol builds are rejected by the version handshake.

## Controls

Opening a supported container or UI focuses player inventory on the left and places
the opened window on the right. Player inventory and Character remain available and
cannot be closed individually. Controlify's usual cursor, scrolling, select,
quick-move and take-half actions retain their configured bindings. The workspace
overrides, including immediate side-by-side B close and right-stick scrolling,
are described in the feature section below.

| Input | Action |
| --- | --- |
| LB / RB | Select the previous / next window on the right |
| Hold LT | Show other open windows around the focused center window |
| Left stick while holding LT | Highlight the nearest radial window; bottom selects Tuck Away |
| Release LT | Put the highlighted window on the right, or tuck the workspace upward |
| A while holding LT | Swap the highlighted window into focus |
| Configured close-inventory button (normally B), in the LT wheel | Immediately close only the highlighted closable window; keep the wheel and other windows open |
| Configured close-inventory button (normally B), in side-by-side view | Immediately close the workspace and all closable windows |
| Configured inventory button (normally Y), while tucked | Restore the same windows and sessions |

While either inventory mode is visible, the screen owns character and world input.
Tucking immediately restores normal gameplay. New supported containers join the same
workspace while tucked and remain out of view until inventory is restored. Quick-move
routes to the other displayed window; a UI without storage cannot receive a stack.

`config/salts_inventory_controlify.properties` contains `animations=true` by default.
Set `animations=false` and restart the client to make all presentation transitions
immediate. The controller presentation forces Core's inventory HUD/offhand arrangement
while active without overwriting the keyboard/mouse preference on disk.

## Controller features

These features are implemented for all twelve version/loader targets in the matrix
below: Minecraft 1.20.1 Fabric/Forge and 1.21.1, 1.21.11, 26.1.2, 26.2 and 26.3
Fabric/NeoForge. The same workspace and wheel models are shared, with native
adapters for each game and Controlify version. Replace both the matching Core
and Salt's Controlify companion when installing this pass; replace Extended as
well when using its mod integrations. Compilation, unit checks and packaged
gameplay results are recorded separately in [VALIDATION.md](VALIDATION.md).

The inventory fits its occupied rows and hotbar. Item tooltips use screen coordinates,
and the native crafting guide participates in pane sizing and positioning when its
presentation is enabled. Controller recipe-book windows use the contextual wheel
described below. The native Controlify cursor scales with the hovered pane's slots.

Tucked gameplay keeps a small Core-textured tab for every retained window, including
Inventory, Character, containers and client UI views. Each tab hangs down from the
top edge and shows the same item icon as the window wheel. Tabs spread across the
top and shrink together when crowded; they persist with animations disabled.

While holding LT, the configured inventory button (normally Y) sorts the focused
middle window into all eligible open storage containers through Core's regular sort
action. A second tap within **350 ms** uses the same force rules as Shift + sort:
matching items, shared item tags, the focused destination, then empty containers.
In side-by-side view, pressing the configured close button (normally B) immediately
closes the workspace and its closable container sessions. In the LT window radial,
a press closes only the highlighted closable window and leaves the wheel open.
Holding the close button has no extra action or progress timer in either view.

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
the grid. The **right stick** scrolls the list under the cursor at a steady analog
rate, increasing from 4 to 24 rows per second as the stick moves farther. Scrolling
starts immediately without a repeat delay and stops on release. A deadzone prevents
drift; elapsed-time updates preserve the speed across frame rates without queuing
a jump after a pause. The left stick remains the native cursor control.

When player Inventory is paired with Character or a crafting table, Quick Move
(Y by default) moves inventory stacks between the hotbar and main inventory.
Crafted output sent to player Inventory fills compatible or empty hotbar slots
first, then uses main inventory space. Character crafting inputs are returned to
the player's inventory on close, with overflow dropped using vanilla's rules.
Tucking keeps those windows open. These changes require the updated matching **Core**
JAR; the cleanup also applies to the ordinary keyboard/mouse character window.

In the creative catalog, A adds one item directly to an available hotbar slot and
Y adds a legal full stack. Compatible stacks and empty slots are used without
replacing unrelated items; a full hotbar leaves the catalog item unchanged.
If the cursor already carries a stack, a catalog grab clears it without granting
the catalog item. In regular creative tabs, Quick Move (Y by default) deletes only
the hovered hotbar stack, preserving a carried stack. Other windows and the
creative Inventory tab retain normal Quick Move transfers.

With the creative **Search** tab selected, hovering its window exposes **Search**
(X by default). This rebindable action focuses the real search field and opens
Controlify's native on-screen keyboard. Selecting the field with A opens the same
keyboard. While it is open, controller input, clicks and scrolling belong only
to the keyboard. Closing it restores the same workspace; held opening/closing
inputs must return to neutral before inventory controls resume. X retains its
ordinary action outside this context. Rebind **Creative search** in Controlify's
Salt's Inventory binding category.

Selecting a modded text field with A also opens Controlify's native
keyboard. This uses the window's public API editor bounds and focus state, including
search fields in the recipe browser, Tom's terminals, Sophisticated containers and
AE2. The field continues to own its text, caret and filtering. Inventory actions
stay captured by the keyboard until it closes and held inputs return to neutral.
Clicking an item beside an automatically focused search field keeps normal item
interaction.

Custom API windows expose their current controls, virtual entries, scroll viewport
and editor bounds through Core's desktop API. Controlify consumes those queries
without per-mod checks. Extended supplies the native adapters for supported mods;
an arbitrary unregistered mod screen still needs an API integration. See the
[controller API contract](../docs/markdown/desktop_window_api.md#controller-navigation-and-editors).
When installing this compatibility pass, replace the matching **Core and
Controlify** JARs and **Extended** if you use it. Only integrations available for
the selected Minecraft version and loader are included; the companion does not
add upstream mod releases to targets where those mods are unavailable.

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
Four-direction navigation uses one complete D-pad icon. Multi-button actions show
adjacent icons with ordinary spacing outside the controller bitmap font. Craft
and Craft Stack remain visible throughout crafting-wheel browsing, even when a
recipe is unavailable, keeping the action rows stable.
Hints change with the active mode and hovered window, use the actual configured
bindings and respect Controlify's screen-guide visibility preference. Controlify
3.5 targets also use its screen-guide scale setting; the 2.x and 3.0 LTS adapters
use Minecraft's GUI scale because those upstream versions lack that setting.
While tucked, the native inventory action becomes Restore; top-aligned
gameplay guides sit below the retained tabs and departing previews. Their normal
placement returns when leaving tucked mode.

D-pad left/right changes category; D-pad up cycles **All**, **Contains Parts** and
**Craftable**, retaining the filter when the wheel is reopened. Contains Parts
requires at least one available ingredient for that recipe. Category icons
appear above the wheel with the selected category enlarged. The right stick moves
continuously through the outer recipe list, feeding individual cards through the
inner ring's bottom opening. The left stick chooses a visible recipe. Hold LB/RB to
rotate counterclockwise/clockwise at five recipes per second; release to stop.
Both recipe sticks use a larger dead zone (0.78 activation, 0.64 release) and ignore
inward spring movement, keeping the selected recipe stable when the stick is released.
Recipes are sorted by their displayed localized names. The outer ring marks the
start of each available A–Z group, skipping empty groups; categories and the
recipe filter rebuild these markers without changing continuous browsing.
Letters use two fixed inner/outer tracks with consistent spacing and short
connecting lines, staying clear of the category tabs. Aiming
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
cooking. In crafting wheels, Quick Move (Y by default) is **Craft Stack**: one
press crafts complete batches up to the result's native stack limit (64, 16 or 1,
for example), stopping earlier when ingredients or destination space run out.
Multi-output recipes never exceed that limit; six-item batches stop at 60 of 64.
Native processing, recipe remainders, statistics and XP remain intact;
consumed ingredients are never restored. If storage cannot hold the existing
items, opening is rejected without changing them.

In 26.3, **D-pad Up** cycles **All → Contains Parts → Craftable → All**.
Contains Parts includes recipes for which at least one matching ingredient is
available across the open containers, even if there is not enough to craft it.
Furnace fuel alone does not count as a recipe ingredient. The filter is shared by
the crafting and cooking wheels and survives closing windows and restarting the
game, in `config/salts_inventory_controlify_recipes.properties`.
The LT wheel keeps its center preview inside the ring of opaque window icons,
including during transitions. Its size limit reserves the largest icon and stays
fixed when the highlighted choice changes. Hover checks use the cheap window-kind hint before
requesting creative tabs, so hovering crafting/furnace windows does not build a
recipe catalog.
Recipe wheels reuse alphabetical sorting and letter positions across unchanged
catalog refreshes and reopening. Recipe values and ingredient availability always
come from the fresh native snapshot; language/resource changes invalidate the index.

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
Every companion target uses nine dedicated UI events
for navigation, window changes, opening, closing, tucking, restoring, confirmation,
observed crafting output and unavailable actions. Opening and closing use Minecraft's
classic button click; the other seven recordings retain their CC0-derived palette.
**Inventory sounds are off by default.** New installs and Restore defaults use the
Off preset; selecting Quiet, Standard or Minimal opts in. Saved audible preferences
are preserved. When enabled, recipe-wheel and scroll selection interrupt the previous
selection cue and restart immediately on the next update, with no browsing cooldown.
Other navigation stays limited to five cues per second. Unchanged selections and
idle wheels stay silent, and cursor-hover audio is off by default. One cue wins each client tick,
with action feedback taking priority over transitions and navigation.
Open **Options → Music & Sounds → Controller sounds** for presets, companion volume,
separate Navigation/Windows/Actions levels, and one-cue preview, Stop and reset.
Minecraft's Master level also applies. Targets with a native UI sound category
(26.x) additionally respect UI volume; older targets use Minecraft's native
Master-routed UI playback. Craft completion follows an observed
increase in matching output in the destination; sending a request alone does not
play success, and uncertain results remain silent. This client observation is not
a Core server acknowledgment.
These optional sound changes live only in the companion JARs. Core and Extended
receive no controller sound assets or playback hooks. The [sound guide](versions/26.3/common/UI_SOUNDS.md)
documents all event IDs, settings, resource-pack replacement and editable sources.
The [texture asset guide](versions/26.3/common/CONTEXTUAL_RADIAL_ASSETS.md) lists the
runtime PNG paths, source sizes and editable tuck sprite.

The root `build` refreshes production JARs for all twelve targets in
[`dist/core`](../dist/core/), [`dist/compatibility`](../dist/compatibility/) and
[`dist/controlify`](../dist/controlify/), with checksums in each directory.
The [combined latest archive](../dist/salts-inventory-all-latest.zip) groups all
36 production JARs by Minecraft version and loader. Use the matching Core and
controller companion for the chosen target; the compatibility extension remains
optional. Install the pinned upstream dependencies listed below separately.
The combined archive does not include gameplay screenshots.
See [validation](VALIDATION.md) for native game evidence and exact artifact hashes.

The input adapters use Controlify's native container screen processors and bindings.
Version-specific adapters handle Minecraft's native screen, input and rendering
APIs and the Controlify 2.x, 3.0 LTS and 3.5 families. Controlify 3.5 supplies its
contextual action-guide engine. Earlier versions render the same compact action
facts with their native bindings and controller glyphs. Controlify 3.0 LTS uses
its native keyboard overlay; the 1.20.1 adapter hosts Controlify 2.x's native
keyboard widget in a modal screen with the same input capture and neutral-release
rules. Minecraft's native recipe IDs, recipe matching, slot restrictions,
remainders and furnace behavior remain version-specific.

Editable wheel PNG/PDN originals stay in
`controlify/versions/26.3/common/src/main/resources/assets/salts_inventory_controlify/`.
The build shares that asset namespace with every companion target, so editing
those files and rebuilding updates the corresponding sprites across the matrix.
Do not edit copies inside build output or `dist`. Build checks resolve their
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
use the separate test-only initializer and packaged launch profile. For ordinary
changes, run only an affected-menu scope. The 26.3 crafting/cooking hover, recipe
filters and LT window wheel have a focused scope:

```powershell
.\gradlew.bat -I controlify/functional-tests/controlify-qa.init.gradle :controlify:26.3:fabric:runPackagedControlifyQa -PcontrolifyQaScope=wheel-polish
```

Replace `fabric` with `neoforge` for that loader. Its reports and screenshots go
to `build/controlify-qa/26.3-<loader>-wheel-polish`, preserving the full-scope
reports separately. The 1.20.1, 1.21.1 and 1.21.11 profiles also support
`-PcontrolifyQaScope=feature-parity` to exercise the added API navigation, modded
editor keyboard, recipe/cooking wheels, filters and creative actions after their
native world/controller/server fixtures, without rerunning the earlier baseline
scenarios. This is a focused scope, not complete gameplay coverage.
For an explicitly requested release-wide pass, `tools/Run-ControlifyReleaseQa.ps1`
plans twelve baselines, twenty-six focused profiles and four optional-mod profiles.
Run `-Phase all -RunId controlify011 -Resume` after the root build. The run ID
preserves historical reports; resume strictly rechecks the current production
JARs before accepting an existing result. Failed attempts and their evidence are
retained separately. Use `-PlanOnly` to inspect the scope without launching games.

Run a complete functional scope only on explicit request:

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
