# Current window cleanup validation

Core 0.1.5 now includes production lifecycle fixes and API 6 removal of minimize and ghost pins. The [window cleanup validation](../functional-tests/WINDOW-CLEANUP-VALIDATION.md) records the current build, focused gameplay coverage and companion API audit. The earlier metadata-only comparison below is historical and does not establish coverage of these new source changes.

# Controlify integration validation

## Historical API5 Core 0.1.5 release alignment

The earlier API5 builds used **Core 0.1.5**, **Compatibility 0.1.2** and **Controlify 0.1.1**
for all twelve targets. Both companions require matching Core 0.1.5. This changes
release metadata and dependency pins; it adds no further gameplay changes.

The 42-profile evidence below belongs to the preserved Core 0.1.4.2 artifacts,
including the original companion dependency metadata. It is not a fresh gameplay
run of the Core 0.1.5 release set.

The normal root build passed: **408 tasks**, including release checks and the
applicable automated tests. All **36 production JARs** have the expected independent
versions and Core pins; module checksums, bundle bytes and stale-output checks pass.
A full archive-member comparison with the preserved tested set confirms **8,722
compiled classes and all non-metadata resources are byte-identical**. Every changed
archive entry differs only by replacing the Core version/dependency label
`0.1.4.2` with `0.1.5`. No new gameplay run was performed for this metadata change.

Evidence: `build/core-0.1.5/root-build.log` and
`build/core-0.1.5/artifact-version-validation.json`.

## Prior Core 0.1.4.2 / Controlify 0.1.1 release validation

The preceding tested release set was **Core 0.1.4.2**, **Compatibility 0.1.2** and **Controlify
companion 0.1.1** across all twelve Minecraft/loader targets. Both companions require
the matching Core 0.1.4.2 artifact. The normal root build passed and produced all
36 independent production JARs, with verified metadata, dependency pins, checksums
and matching bundle bytes.

All **42 packaged gameplay profiles passed strict final-artifact verification**:
twelve baselines, twenty-six focused profiles and four native JEI/REI profiles.
They recorded **16,616 distinct profile/check pairs**, **18,464 PASS records** and
**778 native screenshots**. Repeated checks across profiles count once per profile.
The retained JUnit XML contains **1,447 tests**, with zero failures, errors or skips;
Gradle reused unchanged tasks where applicable. The release-version Python
regression also passed.

Core now skips automatic regular-window onboarding when an alternative workspace
host is registered. All twelve baselines start with help unseen and verify
suppression before connecting the synthetic controller, an unchanged preference,
automatic help while the host is temporarily unregistered, and explicit manual
help after restoring it. This uses the existing public workspace-host contract.

Selected native captures were visually reviewed for window frames, slot spacing,
recipe wheels, native keyboard, the selected status tray, AE2 and Sophisticated
presentation. Physical controller hardware, installed-launcher profiles,
dedicated/remote multiplayer and arbitrary modpacks remain unverified. The native
mod profiles cover their listed 26.3 combinations, not every upstream version.
Temporarily unregistering a host is not a separate Core-only installation test.

See the [complete current release report](docs/release-0.1.1-validation.md) for the
42-profile table, evidence paths, exact artifact checks and coverage limits.
Machine-readable totals and artifact hashes are in
`build/controlify-release-0.1.1/final-validation.json`; the matrix is
`build/controlify-release-0.1.1/controlify011-all/results.json`.

The following dated sections describe earlier releases and their exact validation
scope; they do not certify the current release set.

## Historical independent companion release label (2026-10-20)

All twelve production companions now use **0.1.0** in both the filename suffix and
embedded mod version. Core and Extended retain **0.1.4**. Each companion still
requires Core 0.1.4, generated independently from the root build version.
`controlifyVersion` in the root `gradle.properties` controls future companion labels.

`bundleLatestMods -x test --no-daemon --no-parallel --max-workers=2` passed for the
complete target matrix, including production isolation and native symbol checks.
Focused package checks verified all twelve companion versions/dependencies, all
36 distribution JARs and their bundle bytes/checksums, and rejection of 24
incorrect Core/companion version combinations. The updated artifact inventory
reporter and feature/asset inventory also passed across all twelve targets.

Evidence: `build/controlify-version-label-validation.json` and
`build/controlify-version-label-feature-inventory.json`.
No JUnit or gameplay suite was rerun for this version-label change. The gameplay
records below describe the earlier artifacts and retain their original filenames
and hashes; they are not new gameplay evidence for the relabeled JARs.

## Playtest follow-up (2026-10-19)

The playtest changes are on `controlify-playtest-fixes`. All twelve matching Core,
Extended and Controlify targets build, and `dist/salts-inventory-all-latest.zip`
contains 36 production JARs with verified module checksums. The all-target build
used `bundleLatestMods -x test`: it ran package isolation/native symbol checks,
without running the full functional or JUnit suites.

**Selected status-effects design: tray below Character.** The user chose the tray
and requested dark-grey countdowns without shadows. It is now implemented in all
twelve companion targets, using Core's dynamic frame through the public workspace
API. Character's measured size and scale do not change. Icon tooltips provide names,
levels and remaining time; constrained layouts wrap or expose an overflow tooltip.
The tray hides with Character's icon preview and may use the space above when no
complete row fits underneath.

**Final tray checks:** four focused layout tests passed. Packaged 26.3 Fabric and
NeoForge each passed **47 distinct checks** and retained **two native screenshots**,
at 1280x900 and 960x720. Actual rendering probes verified six dark-grey countdowns
with no shadows, below-Character placement, screen bounds, unchanged Character
scale, suppression/restoration of the old attached list through API ownership, and
no tray after effects clear. The strict verifier matched the current JARs in `dist`,
pinned upstream dependencies, loaded code sources and PNG captures. See
`build/controlify-status-tray-validation.json` and
`build/controlify-qa/26.3-{fabric,neoforge}-status-tray-extended/`.
The complete functional suite was not run for this rendering change.

The following seven-fix checks preceded the tray addition. Their exact tested JARs
and headless reports are preserved under `tmp/controlify-pre-tray-artifacts/`;
the eight-profile verifier was rerun against that snapshot. They establish the
functional fixes before the isolated tray/API rendering change, not a fresh
all-menu certification of the final tray builds.

**Earlier focused headless checks: 61 passed.** These cover the changed controller
state/animation/recipe models, the protocol handshake and the authenticated
workspace-finish packet.

**Earlier packaged focused gameplay: 26.3 Fabric and NeoForge passed**, with Core,
Extended and Controlify loaded together. Each loader recorded **139 distinct PASS
checks and 14 native screenshots**. The strict verifier checked the exact
tested JARs in the preserved snapshot, native loaded code sources, pinned upstream
artifacts and complete PNG captures. Evidence is retained under
`build/controlify-qa/26.3-{fabric,neoforge}-playtest-fixes-extended/`, with the
combined verification in `build/controlify-playtest-validation.json`.

This pass exercised trigger modes, native crafting and catalog stability, cursor
pickup/scaling/return/overflow, furnace fuel routing (including a full fuel slot),
focus swaps with a held stick, pane/icon transitions, main-menu native keyboard
text entry and physical character/backspace callbacks, and the native controls
settings entry.

**Additional focused adapter checks passed on all six older loader targets:**
1.20.1 Fabric/Forge recorded 74 distinct PASS checks each; 1.21.1 and 1.21.11
Fabric/NeoForge recorded 72 each. Every profile retained four native screenshots.
These `playtest-adapters` scenarios exercise the native controls entry, LT/RT
toggle and hold behavior, crafting selection stability and availability refresh,
cursor and Character grid cleanup, fuel routing/full-slot behavior, and focus
swaps with a held stick. They do not run the broader feature-parity suite. The
strict verifier checked the preserved tested JARs and pinned native dependencies for
all eight profiles; their combined result is in
`build/controlify-playtest-validation.json`. The older evidence is retained under
`build/controlify-qa/{game}-{loader}-playtest-adapters-extended/`.

26.1.2 and 26.2 were compiled and checked for native API compatibility, but were
not freshly playtested in this follow-up. The runtime fixtures inject synthetic
controller input into native Controlify; they do not substitute for physical
controller or long-session playtesting. Legacy Controlify versions do not have
the native main-menu keyboard overlay that the 26.x typing fix targets.

The original ribbon, tray and expandable-list comparisons remain QA-only historical
experiments. The selected production tray and its updated text are verified above.

The all-version section below records the earlier port baseline. Its full-suite
counts and gameplay evidence are historical, not a fresh full-suite certification
of this follow-up.

## Historical baseline: all-version controller port (2026-10-19)

The current port implements the controller feature set across **all twelve pinned
Minecraft/loader targets**. Core, Extended and the Controlify companion are
separate matching JARs. Install the newly rebuilt artifacts together; older JARs
with the same `0.1.4` filename may lack the added API methods. The
[port notes](functional-tests/all-versions-port-notes.md) describe native
Controlify, keyboard, guide, rendering and recipe differences, and the shared
editable assets that remain in their original 26.3 source directory.

**Headless checks: 1,435 tests passed, with zero failures, errors or skips.** The
baseline JUnit XML contained 216 shared controller model/input/render tests, 972 Core
tests across the six game versions, and 247 Extended provider tests. Core tests
run once per game version through the Fabric test source set; native compilation,
packaged JAR checks and gameplay on the other loaders are separate evidence.

**Packaged scoped verification passed for all 12 targets.** The
[all-version verification report](functional-tests/all-versions-verification.md)
records the final artifact hashes, native gameplay checks and screenshots. All six
legacy targets passed their native feature-parity scope; all six modern targets
passed aggregation of their main feature suites and seven required focused retries.
The scopes below document what was exercised on each target.

| Minecraft | Loader | Current runtime verification scope | Status |
| --- | --- | --- | --- |
| 1.20.1 | Fabric | Native feature parity and public API editors | Passed scoped verification |
| 1.20.1 | Forge | Native feature parity and public API editors | Passed scoped verification |
| 1.21.1 | Fabric | Native feature parity and public API editors | Passed scoped verification |
| 1.21.1 | NeoForge | Native feature parity and public API editors | Passed scoped verification |
| 1.21.11 | Fabric | Native feature parity and public API editors | Passed scoped verification |
| 1.21.11 | NeoForge | Native feature parity and public API editors | Passed scoped verification |
| 26.1.2 | Fabric | Main feature suite with focused retries where needed | Passed scoped verification |
| 26.1.2 | NeoForge | Main feature suite with focused retries where needed | Passed scoped verification |
| 26.2 | Fabric | Main feature suite with focused retries where needed | Passed scoped verification |
| 26.2 | NeoForge | Main feature suite with focused retries where needed | Passed scoped verification |
| 26.3 | Fabric | Main feature suite with focused retries where needed | Passed scoped verification |
| 26.3 | NeoForge | Main feature suite with focused retries where needed | Passed scoped verification |

Legacy `feature-parity` profiles exercise native crafting/cooking and creative
wheels, all-open ingredient counts, filters, scrolling, generic controls and
modded editor keyboard ownership. Modern profiles use the main feature suite;
`public-api` retries cover the independent custom-window/editor cases, and
`wheel-polish` retries cover wheel text/filter behavior when needed. These scopes
are reported individually and do not constitute a complete functional test of
every target or every optional mod combination. Synthetic native controller input
and an integrated server are used; physical controllers and a separate multiplayer
server are outside this fixture.

Every focused retry must pass with **all three final Salt JARs byte-identical** to
its staged production inputs, along with the pinned original upstream dependencies.
Legacy Fabric development launches load named-namespace copies from the owned
profile's `.fabric/processedMods/` directory. The verifier invokes that target's
actual Fabric Loader `RuntimeModRemapper` with its recorded mapping JAR, native
classpath and selected original/nested inputs, then compares every generated
archive member name and byte with the loaded copies. Each of the three Fabric
profiles reproduces all 16 selected JARs. Forge preserves the same five original
Salt/Controlify/YACL inputs and records the official `SRG_TO_MCP` tool, mapping
hashes and named runtime derivatives. Original Salt inputs match final `dist/`
bytes; original upstream inputs match their published SHA-512 pins. Reproducing
these checks requires the retained native mapping/classpath cache and a local JDK.

For modern main-suite evidence only, the verifier can accept a narrowly proven
change to four UTF-8 bullet/ellipsis constants in one wheel class: every other
class and resource byte must remain identical, and the final companion must pass
a fresh wheel retry. A run that completed the main suite but stopped only during
the independent API setup also requires a successful API retry on final artifacts.
Original failures remain recorded. Arbitrary code changes, an earlier feature
failure or a stale focused report cannot use either exception.

Final aggregation uses:

```powershell
python tools/report-controlify-validation.py --feature-parity --artifacts dist --output controlify/functional-tests/all-versions-verification.md --require-runtime
```

The dated results below are retained history. Their test totals, artifact hashes,
version limits and statements about which JARs changed describe those earlier
passes; they do not certify the current all-version artifacts. Work remains on
`controllify-jar-test`; no merge is part of this port.

## Historical 26.3 full feature and mod compatibility pass (2026-10-19)

Verified at 2026-10-19T05:15:57+00:00. All **12 required 26.3 packaged profiles**
passed: the full suite on both loaders,
all three recent feature scopes on both loaders, and separate JEI/REI compatibility
profiles on both loaders. There were no gameplay assertion failures.

All three Salt JARs match the final release bytes in every run. REI profiles also
assert native item/recipe transforms and clipping. Browser controls must fit
inside their measured visual bounds, and both displayed panes must be separate.

Fresh headless verification: **413 tests** (208 Controller model, 168 26.3 Core, 37 26.3 Extended providers),
zero failures, errors or skips. The other five Core test directories in the broader
generated table are retained historical evidence and were not rerun in this pass.

| Loader | Profile | Browser | Distinct passing checks | Screenshots |
| --- | --- | --- | ---: | ---: |
| fabric | all | none | 1907 | 90 |
| fabric | wheel-polish | none | 105 | 2 |
| fabric | wheel-stability | none | 107 | 3 |
| fabric | crafting-qol | none | 86 | 0 |
| fabric | mod-compat | jei | 220 | 6 |
| fabric | mod-compat | rei | 227 | 6 |
| neoforge | all | none | 1907 | 90 |
| neoforge | wheel-polish | none | 105 | 2 |
| neoforge | wheel-stability | none | 107 | 3 |
| neoforge | crafting-qol | none | 86 | 0 |
| neoforge | mod-compat | jei | 430 | 14 |
| neoforge | mod-compat | rei | 437 | 14 |

Full-run evidence (fabric: 2,387 result records and 90 screenshots; neoforge: 2,386 result records and 90 screenshots). Distinct checks are reported above to avoid
counting repeated observations as different cases.
Each optional profile includes all three Salt JARs and the applicable pinned native
mods. NeoForge adds AE2, GuideME and Sophisticated Storage/Backpacks; both loaders
include Tom's Storage. **EMI had no official 26.3 release to test.**

The six current 26.3 JARs are in `dist/core`, `dist/compatibility` and `dist/controlify`.
Install the matching **Core and Controlify**, plus **Extended** when used, from this
build together. Thirty other-version production JARs are unchanged. All 18 source
PNGs, existing audio assets and user configuration files were preserved. Inventory
sounds remain off by default. Work remains on `controllify-jar-test`; no merge was
performed. Physical controllers and a separate multiplayer server were not tested.

See [the detailed scope and reproduction commands](functional-tests/26.3-compatibility-verification.md)
and [the public controller/editor API contract](../docs/markdown/desktop_window_api.md#controller-navigation-and-editors).

The dated notes below describe their individual earlier passes. Their statements
about which JARs changed apply to those passes; this current compatibility update
rebuilds all six matching 26.3 artifacts.

## Crafting inventory transfers and character-grid cleanup (2026-10-18)

This **26.3 Fabric/NeoForge** change updates Core's workspace routing and server
inventory handling. The existing companion uses those public workspace operations;
its production bytes are unchanged. **Install the updated matching Core**, on the
client and server when playing multiplayer, to get these behaviors.

When player Inventory is paired with Character or a crafting table, Quick Move
(Y by default) moves its hovered main-inventory stack into the hotbar, or its
hovered hotbar stack into main inventory. Crafting inputs and equipment are not
insertion destinations for this action. Other container pairs retain their routing.

Crafted output targeting player Inventory fills hotbar capacity first, including
empty hotbar slots before an existing partial stack in main inventory. Main and
expanded inventory receive the remainder. Both the recipe-wheel capacity reservation
and the actual transfer use this priority. Native recipe consumption, output stack
limits, slot locks and the existing transaction conservation checks remain in use.

Closing the character window sends an authenticated player-menu close request over
the existing close packet. Core ends reversible recipe hover first, then invokes
vanilla's crafting-grid removal/return behavior while preserving the separately
owned shared cursor. Inputs return to available inventory (including expansion
space through Core's existing hook); overflow drops normally. Ordinary character
close/hide/ghost-close and controller workspace close all use this cleanup. Tucking
does not close the windows.

**Only the affected inventory/crafting behavior was tested; no complete functional
suite ran.** All **31 focused server transfer tests** passed, covering both swap
directions, empty-hotbar priority over main-stack merges, split overflow, wheel
capacity planning and existing transaction/result/crafting constraints. The native
**crafting-qol** scope passed **86 checks on Fabric** and
**86 on NeoForge** through actual Controlify input and
an integrated server. It verifies Y in both paired menus and directions, 2x2 and
table recipe-wheel output placement, full-hotbar fallback, B-close returning grid
inputs, exact dropped overflow with a full inventory, cursor conservation and the
ordinary character-window close after switching to keyboard/mouse mode.
The [focused evidence](../build/crafting-qol-focused-verified.json) matches each
loaded Core/companion JAR to its collected hash. Older test/model reports are
retained history and were not rerun or presented as current full-suite evidence.

Both Core/companion builds and companion native-symbol/mixin checks passed.
The [change-scope audit](../build/crafting-qol-change-scope.json) permits exactly
two Core source changes and two 26.3 Core artifact changes; all other 2,538 protected
Core/Extended sources, all 12 Extended artifacts, all ten older Core artifacts,
and all 12 companion artifacts retain their prior bytes. The
[sound/resource isolation audit](../build/crafting-qol-isolation.json) records the
explicit authorized Core baseline and keeps sounds confined to the companion.
All 18 user PNGs remain byte-identical in both companion JARs; sounds remain off
by default. The retained full 26.3 gameplay reports have earlier Core hashes and
are correctly stale for this update.

Updated Core downloads: [Fabric](../dist/core/salts_inventory_update-26.3-Fabric-0.1.4.jar)
and [NeoForge](../dist/core/salts_inventory_update-26.3-NeoForge-0.1.4.jar).
The [complete archive](../dist/salts-inventory-all-latest.zip) includes the matching
current companions. The branch remains `controllify-jar-test`; no commit, push or
merge was performed.

The following sections preserve earlier validation history and its original scope.

## Stable window-wheel center and hanging icon tabs (2026-10-18)

The **26.3 Fabric and NeoForge companions** reserve the largest possible icon
footprint when limiting the LT wheel's center preview. The limit no longer follows
the animated size of a previously highlighted window, eliminating highlight-change
size pulses while retaining the entrance transition and icon clearance.

Tucked windows now appear as **icon-only tabs hanging from the top edge**. Their
existing Core frame extends above the viewport, hiding its upper border; lower
corners remain visible. Icons use the same public API as the LT window wheel.
Crowded rows shrink the complete tab and icon together, and native top-aligned
button guides reserve the visible tab height.

Recipe wheels retain a small metadata index containing alphabetical order and
letter positions. Unchanged snapshots and reopening reuse that index, avoiding
repeated locale collation and letter-group rebuilding. Actual recipe values and
ingredient availability always come from the current Core snapshot. Membership,
name, language or resource changes rebuild the index. The first opening still
requires a fresh native recipe snapshot; no FPS benchmark or complete elimination
of the small entrance dip is claimed.

**Only affected menus were tested; no complete functional suite was run.**
Seventeen focused model tests cover rapid highlight retargeting, existing preview
geometry, clipped/crowded icon tabs, guide reservation and metadata-cache freshness
and invalidation. The packaged **wheel-stability** scope passed
**107 checks on Fabric** and
**107 on NeoForge** through real Controlify input and
an integrated server. Eighteen rapid highlight changes preserve the center's
position/scale and opaque icons. Recipe refresh/reopen retain letter-order objects
while every presented recipe comes from the current snapshot. Tucked native draw
observations verify one matching item icon per window, no text labels, clipped
upper edges, visible icons, guide clearance and successful restoration. Three
screenshots per loader record the LT ring, recipe wheel and tucked icon tabs.
The [focused verification](../build/window-stability-focused-verified.json) matches
the loaded production JARs to their collected SHA-256 values.

Both production builds and native symbol/mixin checks passed. The
[isolation audit](../build/window-stability-isolation.json) preserves all 2,540
Core/Extended source files, all 24 Core/Extended production JARs, all ten older
companions, sound assets and all 18 user textures. Sounds remain off by default.
Retained full 26.3 gameplay reports have earlier hashes and are correctly stale;
the focused results above are the current evidence for these changes.

Updated [Fabric](../dist/controlify/salts_inventory_controlify-26.3-Fabric-0.1.4.jar),
[NeoForge](../dist/controlify/salts_inventory_controlify-26.3-NeoForge-0.1.4.jar), and
the [complete archive](../dist/salts-inventory-all-latest.zip) contain the changes.
No commit, push or merge was performed.

The following sections preserve earlier validation history and its original scope.

## Hover performance, bounded window wheel and saved recipe filters (2026-10-18)

The **26.3 Fabric and NeoForge companions** on `controllify-jar-test` avoid a full
recipe-book snapshot in creative-search availability checks over non-creative
windows. The check first uses Core's existing cheap window-kind hint. Previously,
hover/guide updates over crafting and furnace windows could enumerate every known
recipe repeatedly per rendered frame.

The LT window wheel caps the complete center preview inside the icon ring, including
sampled transition frames. Ordinary window icons occupy their ring positions and
remain opaque; the center preview grows within its limit, and icons render above
the live surfaces. Side-view ghost previews retain their existing presentation.

**D-pad Up** now cycles **All ÃƒÂ¢Ã¢â‚¬Â Ã¢â‚¬â„¢ Contains Parts ÃƒÂ¢Ã¢â‚¬Â Ã¢â‚¬â„¢ Craftable ÃƒÂ¢Ã¢â‚¬Â Ã¢â‚¬â„¢ All**. Contains Parts
requires at least one available matching recipe ingredient across the open
containers, even when its required quantity is incomplete. Fuel alone does not
qualify a furnace recipe. The filter is shared across crafting/cooking wheels and
saved in `config/salts_inventory_controlify_recipes.properties`, surviving wheel
closure, window recreation and game restarts.

**Only the affected menus were tested. No complete functional suite was run for
this change.** Eleven focused tests cover saved-state reload/recovery, filter
semantics, radial geometry/animation and the existing side-view layout contracts.
The packaged **wheel-polish** scope passed **105 checks
on Fabric** and **105 on NeoForge**. Both use the real
Controlify input path and integrated server. An observing public-API proxy recorded
**zero recipe-catalog calls** during stationary hover over crafting-table, furnace
and character windows. These are operation-count regressions, not a claimed FPS
benchmark. Native D-pad presses verify all filter states, incomplete recipe
membership, fuel-only exclusion, resource refresh and close/reopen persistence.
Two screenshots per loader record the Craftable filter and opaque LT icon ring.
The [focused verification](../build/wheel-polish-focused-verified.json) requires all
these checks and matches each loaded production JAR to its collected SHA-256.

Both production builds and exact native symbol/mixin checks passed. The
[isolation audit](../build/wheel-polish-isolation.json) preserves all 2,540
Core/Extended source files, all 24 Core/Extended production JARs, all ten older
companions, the sound assets and all 18 current user textures. No Core update is
required, and inventory sounds remain off by default.

The full 26.3 gameplay reports retained below refer to the previous companion
hashes and are correctly stale for these new JARs. Their counts are historical;
they were not relabeled as current full-suite evidence. Older Core test reports
are also retained rather than rerun for these companion-only changes.

Updated [Fabric](../dist/controlify/salts_inventory_controlify-26.3-Fabric-0.1.4.jar),
[NeoForge](../dist/controlify/salts_inventory_controlify-26.3-NeoForge-0.1.4.jar), and
the [complete archive](../dist/salts-inventory-all-latest.zip) contain the fixes.
No commit, push or merge was performed.

The following sections preserve earlier validation history and its original scope.

## Creative search with the native keyboard (2026-10-18)

The **26.3 Fabric and NeoForge companions** on `controllify-jar-test` now offer a
rebindable **Creative search** action, default **X / West**. It is available only
while hovering the creative window with the Search tab selected. Pressing it
focuses Core's actual search field and opens Controlify's native on-screen keyboard.
**A** directly on the search field opens the same keyboard. The native bottom guide
shows **Search**, replacing conflicting X hints only in this context.

The overlay uses Controlify's keyboard layout and native key navigation while
typing through Core's existing public search handlers. Existing query text is
preserved. Inventory pointer, scrolling, slot actions and wheel inputs are blocked
while it is open. Opening and closing wait for neutral input; physical held keys
and mouse buttons are consumed through release. B, Escape and Enter close only the
keyboard, restoring the same inventory. Same-frame native virtual-mouse A+X on
Enter cannot trap the parent in held-mouse quarantine or retain Minecraft's native
mouse-drag marker. Generated virtual clicks receive their native releases after
the entire dispatch; physical held buttons remain owned by their real releases.
Rebinding, non-search tabs,
other windows and ordinary navigation after close are covered by native QA.

Both production builds passed, including **196 controller model tests**. Packaged
Minecraft gameplay passed **2174 assertions on Fabric**
and **2174 on NeoForge**, with zero failures. The suite
uses the actual native KeyboardWidget and key functions, a synthetic controller,
and the integrated server, and records the native keyboard screenshot in each
loader's [runtime evidence](../build/latest-texture-runtime-verified.json). The
mandatory verifier checks require the new search, modal-input and release regressions.
Native symbol checks resolve 545 member references, 33 mixin selectors and 40
contracts against each loader's pinned compiler dependencies.

The complete gameplay runs also verify the prior change to **Off by default**
sounds: fresh settings emit no inventory cues, audible tests opt in explicitly,
and resetting defaults restores Off. This supersedes the narrower default-only
validation recorded below; its older reports remain historical evidence.

The [isolation audit](../build/creative-search-keyboard-isolation.json) preserves
all 2,540 Core/Extended source files, all 24 Core/Extended production JARs, all ten
older companions, seven recordings and two native sound aliases. No Core update
is required for this feature. All 18 current user textures are included byte for
byte, and no source art was edited. The user's installed sound config remains Off.

Updated [Fabric](../dist/controlify/salts_inventory_controlify-26.3-Fabric-0.1.4.jar),
[NeoForge](../dist/controlify/salts_inventory_controlify-26.3-NeoForge-0.1.4.jar), and
the [complete archive](../dist/salts-inventory-all-latest.zip) contain the change.
No commit, push or merge was performed for this request.

The following sections preserve earlier validation history and its original scope.

## Inventory sounds off by default (2026-10-18)

The **26.3 Fabric and NeoForge companions** on `controllify-jar-test` now use an
**Off** preset for new settings, Restore defaults and unreadable-file recovery.
Inventory volume is zero and cursor hover is disabled. Quiet, Standard, Minimal
and Custom remain available for opting in. Valid saved audible preferences are
preserved. Missing, blank, unknown and explicitly Off presets ignore a stale
positive volume; Custom without a valid saved volume also remains silent.

Both builds and **196 controller model tests** passed. Nine sound-settings
tests cover persistence, reset, opt-in preservation and malformed values. A separate
[packaged settings check](../build/sound-default-off-packaged-settings.json) loads
the settings class from each actual production JAR and verifies fresh-file Off,
saved audible opt-in, reset-to-Off and stale-volume protection. Native symbol/mixin
checks passed against both loaders' exact compiler dependencies.

The gameplay QA fixture was updated and compiled to check fresh-default silence,
explicit opt-in before audible tests, and Off after reset. **The complete Minecraft
gameplay suite was not rerun for this default-only change.** October 17 runtime
reports refer to earlier artifact hashes and are correctly shown as stale for the
two newly rebuilt companions. No earlier runtime report was rewritten as new proof.

The [isolation audit](../build/sound-default-off-isolation.json) preserves all 2,540
Core/Extended source files, all 24 Core/Extended production JARs, and all ten
older-version companions. Seven recordings and both native sound aliases remain
unchanged. Both new companions include all 18 current source PNGs, including the
user's newer creative-tab, ring and ring-highlight textures; no source art was edited.

Updated [Fabric](../dist/controlify/salts_inventory_controlify-26.3-Fabric-0.1.4.jar),
[NeoForge](../dist/controlify/salts_inventory_controlify-26.3-NeoForge-0.1.4.jar), and
the [complete archive](../dist/salts-inventory-all-latest.zip) contain the new
default. See the [sound settings guide](versions/26.3/common/UI_SOUNDS.md) to opt in.
No merge was performed.

The following sections retain October 17 history. Their gameplay counts describe
earlier production bytes, rather than the October 18 rebuild.

## Faster analog right-stick scrolling (2026-10-17)

The **26.3 Fabric and NeoForge companions** on `controllify-jar-test` replace the
350 ms first-repeat delay and slow fixed repeats with elapsed-time analog scrolling.
Scrolling starts with one row immediately. Continued deflection controls a smooth
4 to 24 rows-per-second rate. The existing one-row Core API is called separately
for each due row, so faster scrolling works with the existing Core JAR.

A deliberate activation threshold and release hysteresis reject stick drift.
Release and direction changes discard fractional motion immediately. Long frames
can advance at most three rows, with no queued catchup after a pause. Native scroll
handling remains suppressed while the companion owns scrolling, avoiding doubled
input. Screen changes and virtual-mouse disable clear the accumulator. D-pad edge
navigation still scrolls exactly one row, and contextual wheels retain their stick
controls.

Both builds and **193 controller model tests** passed, including nine
scrolling regressions for speed, frame-rate independence, drift, release, reversal,
stalls and clock rollback. Packaged native tests require faster full-stick scrolling
over 450 ms, slower gentle scrolling over the same interval, and no stale movement
after neutral release. They also retain exact one-row D-pad behavior and wheel
isolation checks.

The first Fabric attempt stopped at an older tucked-window check that read the
previous render snapshot after a new view opened. The QA fixture now waits for a
completed newer tab frame containing that view before running the unchanged exact
count/name assertions. The initial failed evidence is retained in
`tmp/scroll-refinements/fabric-first-results.json`; this fixture adjustment changes
no production behavior.

| Packaged target | Passed checks | Screenshots |
| --- | ---: | ---: |
| Fabric 26.3 | 2056 | 89 |
| NeoForge 26.3 | 2056 | 89 |

Source and artifact audits retain all 2,540 protected Core/Extended source files,
all 24 Core/Extended production JARs, and all ten older-version companions. All
18 current PNGs and the approved seven recordings plus two native sound aliases
are preserved. This adjustment changes only the two 26.3 companions. See the
[packaged runtime evidence](../build/latest-texture-runtime-verified.json) and
[isolation report](../build/scroll-refinements-isolation.json).

Updated [Fabric](../dist/controlify/salts_inventory_controlify-26.3-Fabric-0.1.4.jar),
[NeoForge](../dist/controlify/salts_inventory_controlify-26.3-NeoForge-0.1.4.jar),
and the [complete archive](../dist/salts-inventory-all-latest.zip) contain this
change. No merge was performed.

## Native open/close and fast selection follow-up (2026-10-17)

On `controllify-jar-test`, **26.3 Fabric and NeoForge** now use Minecraft's existing
`minecraft:ui.button.click` for opening and closing. The stable companion event IDs
remain `ui.open` and `ui.close`, implemented as native event references. No
Minecraft recordings are copied. The other **seven CC0 recordings and their
metadata are byte-for-byte unchanged** from the approved sound palette.

Fast recipe-wheel and scroll selection no longer wait for the 200 ms navigation
interval. A changed selection update stops the previous navigation sound before
playing a fresh instance from its beginning. Multiple changes within one client
tick coalesce to the latest selection, with no delayed click backlog. Ordinary
navigation retains its previous limits, and result/structural feedback retains
its brief priority gap. Presets, gains, Master/UI mute and silent idle behavior
remain in place. See the [sound guide](versions/26.3/common/UI_SOUNDS.md).

Both builds passed. The controller model suite passed **186 tests**, including
six additional fast-selection regressions. The unchanged Core test evidence below
is retained from the earlier pass. Native symbol checks and artifact isolation
passed for both new companions. Source and SHA-256 audits preserve all 2,540
protected Core/Extended source files, all 24 Core/Extended production JARs and the
ten companions for older Minecraft versions. All 18 current PNGs remain exact.

| Packaged target | Passed checks | Screenshots | Stable sound events |
| --- | ---: | ---: | ---: |
| Fabric 26.3 | 2044 | 89 | 9 |
| NeoForge 26.3 | 2044 | 89 | 9 |

Each native run requires eight rapid alternating scroll/carousel updates, a fresh
native sound for all eight, and at least three cues within 200 ms. Actual
SoundManager calls identify the previous sound instance and require its stop
strictly before the next play. The tests retain result priority, mute, idle,
preview, settings persistence, controller inputs and native item conservation.
The final JAR verifier requires seven exact recordings plus the two native aliases,
with no retired open/close OGGs or copied Minecraft sound data.

The [complete archive](../dist/salts-inventory-all-latest.zip) and individual
[Fabric](../dist/controlify/salts_inventory_controlify-26.3-Fabric-0.1.4.jar) and
[NeoForge](../dist/controlify/salts_inventory_controlify-26.3-NeoForge-0.1.4.jar)
companions contain this update. Core and Extended need no update for this follow-up.
The [sound isolation report](../build/sound-refinements-isolation.json) and
[packaged runtime evidence](../build/latest-texture-runtime-verified.json) bind
the checks to their exact production bytes. No merge was performed.

## Earlier UI sound overhaul (2026-10-17)

The [sound guide](versions/26.3/common/UI_SOUNDS.md) implements the research PDF's
restrained semantic palette for **26.3 Fabric and NeoForge**. All sound code,
configuration, nine CC0-derived recordings and notices are owned by the companion.
The repository remains on `controllify-jar-test`; no merge was performed.

Quiet is the default. Browsing shares a five-cues-per-second limit; stale and
replaced cues are discarded. Actions take priority over transitions and movement.
Unchanged selections, idle wheels and default cursor hover stay silent. The native
Music & Sounds screen opens Controller sounds, with presets, four volume controls,
optional cursor-hover audio, single-cue preview, Stop and reset. Minecraft's native
Master and UI volume settings continue to apply. Craft completion is an observed
matching destination increase, not a server acknowledgment; dispatch alone is
silent, and ambiguous or expired observations remain silent.

Both companion builds passed. A fresh controller model run passed **180 tests**,
including **35 new sound policy, observation and settings tests**. The unchanged
Core test results below are retained from the earlier pass; this sound pass did
not require a Core rebuild. Native symbol checks passed on each loader's actual
compiler inputs: 475 member references, 32 mixin selectors and 39 native contracts.

| Packaged target | Passed checks | Screenshots | Actual sound events |
| --- | ---: | ---: | ---: |
| Fabric 26.3 | 2028 | 89 | 9 |
| NeoForge 26.3 | 2028 | 89 | 9 |

The strict verifier checks real native sound dispatches, event identities, gains,
UI category, relative/non-looping playback, fixed pitch and rate limits separately
from the single settings preview. Native controls verify Minimal mode, mute,
default hover silence, same-tick result priority, persisted custom volume, small
viewport scrolling, preview Stop and reset. All original navigation, transfer,
crafting and conservation checks remain required. Closing is verified during the
first processed configured Back edge and before the native handler returns;
diagnostics retain queued-input, processing and handler timings separately.

The sound isolation audit compared **2,540 protected Core/Extended source files**
and **24 collected Core/Extended production JARs** against their pre-pass SHA-256
baseline. Every byte and source inventory matched. All nine packaged OGGs decoded
as short mono 44.1 kHz audio, with exact source/manifest/license hashes; authoring
originals and PCM files are excluded. Both companions also retain all 18 current
source PNGs byte-for-byte. The full 36-JAR inventory, checksums and archive CRCs
were verified. Earlier Minecraft versions retain their existing companion bytes.

For this audio update, replace only the matching 26.3 companion if the matching
Core from the earlier controller pass is already installed. Extended is optional.
The [complete archive](../dist/salts-inventory-all-latest.zip) retains separate
version/loader folders and includes both updated companions.

Native screenshots: [Fabric settings](../build/controlify-qa/26.3-fabric/screenshots/45-controller-sound-settings.png),
[Fabric constrained settings](../build/controlify-qa/26.3-fabric/screenshots/45b-controller-sound-settings-constrained.png),
[NeoForge settings](../build/controlify-qa/26.3-neoforge/screenshots/45-controller-sound-settings.png).
Machine-readable [sound isolation](../build/sound-overhaul-isolation.json) and
[packaged runtime evidence](../build/latest-texture-runtime-verified.json) identify
the exact collected artifacts. The remaining older-target status below is separate
from this completed 26.3 sound pass.

## Earlier controller refinements (2026-10-17)

Production changes are restricted to Minecraft 26.3 Fabric and NeoForge on
`controllify-jar-test`. Creative catalog selection clears an occupied cursor;
Quick Move deletes only the hovered hotbar stack in regular creative tabs.
Other windows and the creative Inventory tab retain normal transfers.

The recipe guide keeps Craft visible, adds the rebindable Craft Stack action,
composes adjacent button glyphs without a missing separator, and uses one full
D-pad icon. The footer fits common recipe controls on one compact row and keeps
two readable rows for narrow viewports. Alphabet labels follow two consistent
tracks with short leaders and category-tab clearance.

Craft Stack uses complete native recipe batches, available ingredients and
destination capacity, capped at the result's native stack size. Both loaders
passed packaged gameplay checks for 64-, 16- and 1-item results, a multi-output
recipe that stops at 60/64, limited materials and limited destination space,
rebinding, live preview restoration, creative cursor clearing and hotbar-only
deletion. These checks use real native processing with a synthetic controller.

Before the sound overhaul, the root build passed all 909 scoped JUnit tests and collected all 36 Core,
Extended and Controlify production JARs. Checksums and archive integrity passed;
both 26.3 companions match all 18 current source PNGs byte-for-byte. The
[complete archive](../dist/salts-inventory-all-latest.zip) groups JARs by version
and loader. Replace both matching Core and Controlify JARs when updating.

The repository remains at `G:/Codex Projects/Salts Inventory Update`. Earlier
C-drive runtime reports remain historical evidence: their recorded launcher
and mapping paths may require reruns after relocation. Only the current target
statuses below count as verified. No merge was performed.


## Automated tests

The 8 scoped JUnit XML directories contain **1016 tests**, with 0 failures, 0 errors and 0 skips. All suites passed.

| Suite | Tests | Status | Report |
| --- | ---: | --- | --- |
| Controller model/input/render | 208 | Passed | [HTML](../../controlify/model/build/reports/tests/test/index.html) |
| Core 1.20.1 | 119 | Passed | [HTML](../../versions/1.20.1/fabric/build/reports/tests/test/index.html) |
| Core 1.21.1 | 121 | Passed | [HTML](../../versions/1.21.1/fabric/build/reports/tests/test/index.html) |
| Core 1.21.11 | 121 | Passed | [HTML](../../versions/1.21.11/fabric/build/reports/tests/test/index.html) |
| Core 26.1.2 | 121 | Passed | [HTML](../../versions/26.1.2/fabric/build/reports/tests/test/index.html) |
| Core 26.2 | 121 | Passed | [HTML](../../versions/26.2/fabric/build/reports/tests/test/index.html) |
| Core 26.3 | 168 | Passed | [HTML](../../versions/26.3/fabric/build/reports/tests/test/index.html) |
| Extended 26.3 providers | 37 | Passed | [HTML](../../compatibility/versions/26.3/fabric/build/reports/tests/test/index.html) |

Core tests run once per Minecraft version through its Fabric test source set. Forge and NeoForge share the ported Core implementation; their native compilation, archive checks and individual gameplay runs are separate evidence. Extended compatibility test counts are outside this controller matrix total.

The regressions cover workspace lifetimes, mandatory-window protection, restoration, scoped HUD settings, native/external slot geometry, modal ownership and outside dismissal, exact displayed-pair destinations, server authorization and transaction rollback, native crafting/Loom/furnace result callbacks and replenishment. Modern API fixtures also check supplementary Unicode character events. The controller suite covers three-mode state, input routing, bounded layouts, animation, transformed clipping and queued-render opacity capture.

## Artifacts and isolation

Independent source, all twelve compiler manifests, collected companion bytecode and matching Core peer archives: **Passed**. The reporter calls the existing [boundary verifier](../../tools/verify-controlify-boundaries.py) for these checks. Dedicated companions and installation-bundle copies must be byte-identical.

`buildControlify` also checks native Controlify/Minecraft member references and mixin selectors against each target's actual compiler inputs, and verifies the published controller JAR against its SHA-512 pin. These are build gates; gameplay status is determined independently below.

The third JAR contains the controller layout, bindings, animation and render adapters. Core supplies generic public workspace/transfer APIs. Upstream Controlify/YACL, Core and the optional extended compatibility mod remain separate archives. QA fixtures are excluded from production JARs.

| Minecraft | Loader | Matching artifact pair | Packaged gameplay |
| --- | --- | --- | --- |
| 1.20.1 | Fabric | Present | Needs rerun |
| 1.20.1 | Forge | Present | Needs rerun |
| 1.21.1 | Fabric | Present | Needs rerun |
| 1.21.1 | NeoForge | Present | Passed |
| 1.21.11 | Fabric | Present | Needs rerun |
| 1.21.11 | NeoForge | Present | Passed |
| 26.1.2 | Fabric | Present | Needs rerun |
| 26.1.2 | NeoForge | Present | Passed |
| 26.2 | Fabric | Present | Needs rerun |
| 26.2 | NeoForge | Present | Passed |
| 26.3 | Fabric | Present | Passed |
| 26.3 | NeoForge | Present | Passed |

The following additional upstream runtime dependency is installed separately. Its original published bytes and declared version must also pass the packaged runtime verifier.

| Minecraft | Loader | Required runtime dependency | Pin |
| --- | --- | --- | --- |
| 1.20.1 | Forge | LexManos's Mixin Args 1.0 | [Original release](https://www.curseforge.com/minecraft/mc-mods/mixin-args/files/6488404); `mixin_args-1.0.jar`; SHA-512 in targets.json |

## Packaged gameplay evidence

The [runtime verifier](../../tools/verify-controlify-runtime.py) requires successful scenario records for all eleven baseline objective areas, plus 49 feature areas for 26.3, actual matching Minecraft/Controlify/YACL versions, original dependency SHA-512 pins, separate production JAR code sources, final collected/staged Core and companion SHA-256 equality, fresh reports and actual PNG captures. Every existing report is revalidated when this file is generated.

| Minecraft | Loader | Status | Evidence |
| --- | --- | --- | --- |
| 1.20.1 | Fabric | Needs rerun | 1.20.1-fabric: runtime loaded implementation directories instead of production JARs; [report](../../build/controlify-qa/1.20.1-fabric/results.json) |
| 1.20.1 | Forge | Needs rerun | Forge remapping used different Minecraft mappings; [report](../../build/controlify-qa/1.20.1-forge/results.json) |
| 1.21.1 | Fabric | Needs rerun | 1.21.1-fabric: runtime loaded implementation directories instead of production JARs; [report](../../build/controlify-qa/1.21.1-fabric/results.json) |
| 1.21.1 | NeoForge | Passed | 291 PASS records; 18 screenshots; 58.879 s; [report](../../build/controlify-qa/1.21.1-neoforge/results.json) |
| 1.21.11 | Fabric | Needs rerun | 1.21.11-fabric: runtime loaded implementation directories instead of production JARs; [report](../../build/controlify-qa/1.21.11-fabric/results.json) |
| 1.21.11 | NeoForge | Passed | 295 PASS records; 18 screenshots; 58.876 s; [report](../../build/controlify-qa/1.21.11-neoforge/results.json) |
| 26.1.2 | Fabric | Needs rerun | 26.1.2-fabric: runtime loaded implementation directories instead of production JARs; [report](../../build/controlify-qa/26.1.2-fabric/results.json) |
| 26.1.2 | NeoForge | Passed | 291 PASS records; 18 screenshots; 57.550 s; [report](../../build/controlify-qa/26.1.2-neoforge/results.json) |
| 26.2 | Fabric | Needs rerun | 26.2-fabric: runtime loaded implementation directories instead of production JARs; [report](../../build/controlify-qa/26.2-fabric/results.json) |
| 26.2 | NeoForge | Passed | 299 PASS records; 18 screenshots; 58.360 s; [report](../../build/controlify-qa/26.2-neoforge/results.json) |
| 26.3 | Fabric | Passed | 2184 PASS records; 90 screenshots; 264.995 s; [report](../../build/controlify-qa/26.3-fabric-extended/results.json) |
| 26.3 | NeoForge | Passed | 2183 PASS records; 90 screenshots; 263.409 s; [report](../../build/controlify-qa/26.3-neoforge-extended/results.json) |

The disposable integrated-server harness installs a synthetic driver in the real Controlify manager and uses actual bindings, native cursor/screen processing and server packets. Supported mixed input mode prevents operating-system cursor events from interrupting automation. This provides game runtime evidence; physical controller hardware has not been tested.

Each completed run must cover permanent/default windows; side-by-side native pointer, D-pad snapping and exact transfers; radial navigation/focus/bumpers; gameplay gating; tuck/add/restore identities; protected and individual close; immediate side/radial close on 26.3 or the configured hold threshold on older versions; HUD/animation settings; native result callbacks and conservation/XP; protruding custom UI and modal dismissal; and the genuine controller/server handshake.

```powershell
.\gradlew.bat buildControlify collectControlifyJars collectControlifyTestJars --console=plain --no-parallel
.\gradlew.bat -I controlify/functional-tests/controlify-qa.init.gradle :controlify:1.21.1:fabric:runPackagedControlifyQa
python tools/report-controlify-validation.py
python tools/verify-controlify-runtime.py --summary build/controlify-runtime-verified.json
```

Use the corresponding `:controlify:<minecraft>:<loader>:runPackagedControlifyQa` task for each target. The full runtime verification command rejects an incomplete matrix.

## Collected file hashes

Install each companion with the matching rebuilt Core and the pinned native dependencies listed in [README](../../controlify/README.md). The installation bundle keeps `core/` and `controlify/` separate; dedicated companion copies are in `dist/controlify/`.

| Artifact | Bytes | SHA-256 |
| --- | ---: | --- |
| [1.20.1 Fabric core](../../dist/core/salts_inventory_update-1.20.1-Fabric-0.1.4.jar) | 950943 | `82fe0dc21e6ea0b88b7bcd7240c84bab6305ed17808d04a33a522dda39d18830` |
| [1.20.1 Fabric controlify](../../dist/controlify/salts_inventory_controlify-1.20.1-Fabric-0.1.4.jar) | 60297 | `026af89bfda700323e7ac3456afebcdf38620c2b1ad866135b35aa1699d0b5d5` |
| [1.20.1 Forge core](../../dist/core/salts_inventory_update-1.20.1-Forge-0.1.4.jar) | 962785 | `e3dfa7476522995e855194e6d6b215449d10192152de366b011885c1069a9535` |
| [1.20.1 Forge controlify](../../dist/controlify/salts_inventory_controlify-1.20.1-Forge-0.1.4.jar) | 60750 | `6950724d0fa0cac4074e079c61672e9ab8aa258b72be5436617a019fda17fc70` |
| [1.21.1 Fabric core](../../dist/core/salts_inventory_update-1.21.1-Fabric-0.1.4.jar) | 992441 | `7d03bae92a58106beccac6c26149e0a51b10ad41285ee8b431e4dcf634cf9711` |
| [1.21.1 Fabric controlify](../../dist/controlify/salts_inventory_controlify-1.21.1-Fabric-0.1.4.jar) | 59977 | `d6bd2df1bc566cfeecc4555414017f21e9a21b5635717c3cea8aa20ecb82a273` |
| [1.21.1 NeoForge core](../../dist/core/salts_inventory_update-1.21.1-NeoForge-0.1.4.jar) | 1001441 | `67920e21288b782ca06b1312673390acdf91f664b456442b8910c6c20b465731` |
| [1.21.1 NeoForge controlify](../../dist/controlify/salts_inventory_controlify-1.21.1-NeoForge-0.1.4.jar) | 59284 | `3f1f2d31215bcfb3eee33db252287ec00d1f26e85c011e876d8b1fd17416f348` |
| [1.21.11 Fabric core](../../dist/core/salts_inventory_update-1.21.11-Fabric-0.1.4.jar) | 986008 | `0c5f82a467923db6767466f64ced99d0817f79de4f713153e18c99278e104079` |
| [1.21.11 Fabric controlify](../../dist/controlify/salts_inventory_controlify-1.21.11-Fabric-0.1.4.jar) | 67248 | `f5c875dc38341d7a09e2453145459a0fde425f3d3128cdea7287d24aa19efbbe` |
| [1.21.11 NeoForge core](../../dist/core/salts_inventory_update-1.21.11-NeoForge-0.1.4.jar) | 995887 | `997725f829cc05f901532c10ce1bfa287a5113bcf217e3caa565d2233bbb666d` |
| [1.21.11 NeoForge controlify](../../dist/controlify/salts_inventory_controlify-1.21.11-NeoForge-0.1.4.jar) | 66739 | `02c3295337c8dab3c4d4f2ea487fde6c3ebd93cb893f988a1dd55cb940069f3d` |
| [26.1.2 Fabric core](../../dist/core/salts_inventory_update-26.1.2-Fabric-0.1.4.jar) | 990600 | `9c3373f313d2fbd0d728750dd3bd008c30b4907d0a0fe8701a5dbc7564c5fba6` |
| [26.1.2 Fabric controlify](../../dist/controlify/salts_inventory_controlify-26.1.2-Fabric-0.1.4.jar) | 68390 | `59465ef0205d3cb79cc8a1fdace57a3843ce2236629506db367389ec10b9edd5` |
| [26.1.2 NeoForge core](../../dist/core/salts_inventory_update-26.1.2-NeoForge-0.1.4.jar) | 988782 | `32efeae1b945af90faeeff43f92c7969b5870811a260868d170b584ec3b330b6` |
| [26.1.2 NeoForge controlify](../../dist/controlify/salts_inventory_controlify-26.1.2-NeoForge-0.1.4.jar) | 67018 | `5a3a53213cbeca04b613d68b1f650fe03b7fdb846b60c399624f55c1a7897953` |
| [26.2 Fabric core](../../dist/core/salts_inventory_update-26.2-Fabric-0.1.4.jar) | 992861 | `2466a7d95ec5e4dc88168ff7e74038a2b6b0089cfaa1722703705ae4f3b22e4f` |
| [26.2 Fabric controlify](../../dist/controlify/salts_inventory_controlify-26.2-Fabric-0.1.4.jar) | 68367 | `6d8d1bae3c561e5d1dc8590b6bdabc6bbc10bc8b78b78087ec9955b48704c8e0` |
| [26.2 NeoForge core](../../dist/core/salts_inventory_update-26.2-NeoForge-0.1.4.jar) | 991007 | `792be81c5a208e3f519dd3e6570db7ed6224927ccb3c56200718b4b9515c960b` |
| [26.2 NeoForge controlify](../../dist/controlify/salts_inventory_controlify-26.2-NeoForge-0.1.4.jar) | 66992 | `9b2114c5ebec865254d4f506d389fdd59f8e29bf82cd40739328d89bce12609a` |
| [26.3 Fabric core](../../dist/core/salts_inventory_update-26.3-Fabric-0.1.4.jar) | 1095361 | `4b7d38445c3b7f5e40a8249e8ffd90dc791219f10f0a8a055ea5735f8b6a9fac` |
| [26.3 Fabric controlify](../../dist/controlify/salts_inventory_controlify-26.3-Fabric-0.1.4.jar) | 344220 | `e934d4c01982f814edc97a6efecc98777e5b86f057c19f445db20a9cc05523cc` |
| [26.3 NeoForge core](../../dist/core/salts_inventory_update-26.3-NeoForge-0.1.4.jar) | 1092075 | `382843d304794622e3c512649cb7ddec0c99790bcc08ee0d2d175aeb4a34b5b1` |
| [26.3 NeoForge controlify](../../dist/controlify/salts_inventory_controlify-26.3-NeoForge-0.1.4.jar) | 337700 | `fcae2b0b365265a635484143f04ec9658f93edbdf0551458198871f9321d9bf0` |
