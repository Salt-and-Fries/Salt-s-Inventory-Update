# AE2 desktop integration

The optional compatibility extension consumes unmodified upstream Applied
Energistics 2 and Salt's public API. The sibling reference fork is read-only.
Core contains no AE2 dependency, classes, assets or integration hooks.

## Version-specific UI baseline

Each adapter uses the installed AE2 release's native artwork, controls and
features, with the fork's desktop integration layered on top. Minecraft 1.21.1
uses AE2 19's design; 26.3 uses AE2 26.3's design. Native changes between upstream
releases are expected. Older adapters do not acquire later ordinary AE2 features
solely to match screenshots from 26.3.

The reference fork changed none of upstream 26.3's 654 native PNG textures or
54 ordinary screen documents. Its 42 added desktop documents are packaged
byte-identically and include the installed release's ordinary documents. The
extension retains native rendering, input, menus and packet codecs. Resource
pack overrides remain supported.

AE2 15 keeps its native knife field and condenser control positions and its
97-pixel catalog footer. Its 36-slot sky chest receives enough desktop height
for every native slot. Its combined chest menu and absence of key-type
selection are native version differences. Desktop management controls, frame
contours, detached menu lifetime, toolbox companions and authenticated actions
remain integration responsibilities. The priority tab receives the desktop
navigation geometry, without changing ordinary styles.

## Reviewed upstream targets

| Minecraft / loader | Unmodified AE2 input |
| --- | --- |
| 1.20.1 Fabric | 15.4.11 |
| 1.20.1 Forge | 15.4.11 |
| 1.21.1 NeoForge | 19.2.18 |
| 26.1.2 NeoForge | 26.1.13-beta |
| 26.2 NeoForge | 26.2.1-alpha |
| 26.3 NeoForge | 26.3.1-alpha |

All twelve extension JARs contain the shared additions. The official download
page and release API were checked on 2026-10-12. No upstream input was available
for 1.21.11 on either loader or for Fabric 1.21.1/26.1.2/26.2/26.3. Those six
combinations have no verified native AE2 adapter. They safely leave unrelated
integrations available; packaging shared code does not establish AE2 support.

The native Mixin plugin inspects bytecode without defining optional AE2 classes.
It verifies the entire relevant contract before applying desktop additions.
Missing AE2 or an unsupported native ABI leaves ordinary AE2 behavior available.
GuideME and other upstream dependencies are supplied by the installed AE2
release and are never embedded in this extension.

## API boundary and native hooks

All communication with Salt uses `com.salts_inventory_update.api`. Compatibility
compiles against a declaration-only public API JAR; the Core implementation is
absent from its compiler classpath. No hook targets Core, and no reflection or
access widener bypasses the Core API.

Fork-added helpers have extension-owned packages. Changes to existing AE2
classes are verified instruction deltas under `ae2-native-patches`, applied to
the installed native classes. Native bodies are retained, and no `appeng`
implementation classes or native texture overrides are bundled. Compile-only
AE2 ABI declarations discard bodies and resources and never enter release JARs.
Named and production namespaces are verified separately on Fabric/Forge 1.20.1.

Server actions run through authenticated public desktop callbacks, read and
commit the public session cursor, and borrow the native menu only within the
callback. Menu transitions use `DesktopSessions`. Detached builders delegate
to native factory and opening serializers, restoring their scope on failure.
Toolbox companions share the original native inventory by exact stack identity.
Weak lifetime ownership and disconnect cleanup release menus and queued updates.

Packet adapters retain native integer registries, encoders, decoders and action
allowlists. Lifetime UUIDs select the intended desktop menu. An extension-owned
bounded update channel is independent of the Core's authenticated action limit.
Owned buffers are released on success and failure. Ordinary AE2 transport and
its GUI synchronization schema remain intact.

Four desktop CPU fields travel in a five-byte public session payload instead of
adding `GuiSync` entries to ordinary AE2 updates. Initial state and changes are
sent by the server; the client never invents job state. This preserves decoding
by unmodified clients on the normal AE2 menu path.

## Imported additions

* All 42 desktop screen JSON profiles are copied byte for byte. They continue to
  include AE2's original screen documents and use its original art and geometry.
* The language document contains the fork's 59 new keys. Existing AE2 language
  entries are supplied by AE2 itself.
* `DesktopContentRegistry` retains the fork's lifetime-addressed update receivers,
  early-update queues, and limits. The native packet type is supplied by each
  adapter. Disconnect cleanup releases every receiver and queued update.
* `MenuPacketScope` retains nested widget ownership and restores the previous
  menu even if a callback throws. Ordinary widgets use native AE2 transport.
* `DesktopMenuState` retains the fork's detached-menu identity, toolbox flag,
  action queue, and action limits outside the native implementation.
* `DesktopChrome` retains the fork's drawing coordinates and colors. Native
  sprite drawing is delegated to AE2's native blitter by the window adapter.
* `DesktopSlotPresentation` retains defensive copies for slot art. The generic
  `DesktopContentActions` contract supplies the fork's per-input slot, close and
  subview callbacks without tying shared code to a single Minecraft input type.
* `DesktopManagementControls` draws the fork's manager-button pixels using
  public Salt render context and control state. The modern native window adapter
  also includes the fork's minimized-frame rendering and keyboard activation.
* Optional mixins catalog native menu builders and attach extension-owned state
  to AE2 menus. The older `build(String)` and newer `buildUnregistered` paths are
  supported. The mixin plugin inspects bytecode without defining optional AE2
  classes; it skips missing targets and older methods.
* The NeoForge 26.3 `DesktopMenuPackets` adapter retains the fork's explicit
  action/update allowlists, bounds, trailing-byte checks, and authenticated menu
  ID replacement. It compiles against unmodified upstream AE2 26.3.1-alpha and
  delegates encoding, decoding, and packet construction to that mod. This
  compile-only dependency is confined to the compatibility module. NeoForge
  26.1.2 and 26.2 have corresponding native codec adapters. AE2 19 and AE2 15
  use their respective native codec adapters described above.
* NeoForge 26.3 `DesktopUpdates` retains the fork's lifetime-addressed update
  application to native menus, repositories, crafting views and pattern views.
  It calls AE2's existing setters and update methods. The native handler hooks
  and client window adapter connect transport and lifetime receiver registration.
* The NeoForge 26.3 desktop builder bridge delegates menu reconstruction and
  initial data to AE2's existing factory and serializer strategies. It retains
  the fork's detached-opening rule: a missing host cannot send a native close
  packet for another container. Ordinary AE2 opening methods remain unchanged.
* NeoForge 26.3 toolbox hooks expose the original toolbox host and share its
  native inventory by exact item-stack identity, as in the fork. Weak keys and
  values release discarded tools. Native inventory implementation remains AE2's.
* The NeoForge 26.3 server registration adapter retains menu bindings, source
  identities, slot gesture ownership, companion leases and native action
  allowlists. Shared cursor access and native GUI transitions use the public
  Salt API. Registration is deferred until native menus and client screens have
  finished registering, and uses the public integration transaction boundary.

## Build and validation

See the [executed validation report](../../docs/markdown/ae2-extension-validation.md)
for final build totals, artifact paths, native support and remaining limits.

Run `./gradlew build` or `.\gradlew.bat build` from the project root. The normal
build tests and packages all twelve Core and twelve compatibility targets,
clears owned distribution outputs, and copies only remapped production JARs to
`dist/core` and `dist/compatibility`. Individual target builds remain available.

Build audits reject Core-to-AE2/compatibility references, compatibility-to-Core
implementation references, native AE2 implementation classes, shading and QA
instrumentation. They validate exact imported resources, annotations, native
member kinds, method stacks, branch labels and bootstrap handles.
`VerifyAe2NativeDeltas --session-cpu-state` also proves all 61 native 26.3 deltas
against the fork, accounting only for the documented CPU transport metadata.

Release activation is enabled on all six reviewed native adapters. Fresh Fabric
and Forge AE2 15 full atlases pass all 76 native presentations.

All six native targets have passed the four-menu gameplay profile: 312 records
on each AE2 15 loader and 319 on the four newer targets, with zero failures.
All six have also passed initial CPU delivery and two real job/cancel cycles,
including authoritative server cancellation and client idle resynchronization.
Forge 1.20.1 Core alone passed 271 records; its extension without AE2 passed 281.
These tests establish their recorded scenarios, not every possible modpack.

Full native atlas coverage is version-specific: 90 named presentations across
40 native menus on AE2 19/26, and 76 across 39 native menus on AE2 15 (40 fixtures
exercise its combined chest twice). `NativeAtlasCoverage` verifies expected
labels, uniqueness, image and geometry files, native menu count, and the actual
presence or absence of the two upstream subview types. Missing a supported view
fails. Regression checks reject duplicate labels, missing views or geometry,
and an incorrect upstream feature profile.

```powershell
.\functional-tests\scripts\Run-GameplayQa.ps1 -Minecraft 1.20.1 -Loader fabric -Scenario ae2-atlas
.\functional-tests\scripts\Run-GameplayQa.ps1 -Minecraft 26.3 -Loader neoforge -Scenario ae2-atlas -AtlasMenu craftingcpu -CpuLifecycle
```

Focused runs use separate outputs and cannot count as a full atlas. The catalog
focus ID is `patternaccessterminal`. Deferred fixture errors write `failed.txt`
and fail immediately. All atlas classes and observer mixins are excluded from
production packaging; building release JARs with atlas instrumentation is rejected.

Cross-version screenshot comparisons are diagnostics. Native art, renderer
color rounding, tooltip capitalization and absent upstream features can differ.
Raw rectangles also include changing world background and animated/live content;
complete raw pixel identity is not claimed. The 26.x full runs match all 90
geometry and control snapshots. The AE2 19 run matches all 90 control snapshots
and all rectangles, with eight tooltip capitalization differences.

The source coverage audit classifies all 89 changed Java files in the reference:
60 native source edits map to the verified deltas, 16 helpers are rehomed, 12 QA
sources/edits remain excluded, and the localization generator's added keys are
imported resources. The only `AppEngClient` edit calls the reference QA tick;
it is intentionally absent from production integration hooks.

## Legacy panel-border corrections

The AE2 15 adapter leaves the last four background rows for the native footer
pass. This prevents transparent rounded corners from revealing previously
painted vertical borders. Plain terminals recognize the native 97-pixel footer
and close across their full width, using the 176-pixel native footer's caps and
repeated center. Crafting and encoding retain their stepped contours.

Legacy pattern-mode tabs use their native 22-by-22 sprite dimensions and a
21-pixel pitch, with the last tab aligned to the cut footer. Only unchanged
native desktop defaults are adjusted; ordinary styles and custom bounds remain
available. The CPU selector's overlapping rail is stitched with a native
background sample after the attachment pass and buffered widget drawing, so
its border does not protrude into the status footer. These changes are confined
to the shared 1.20.1 compatibility adapter and its two mapped native contracts.

## Reproduction and provenance

`META-INF/ae2-desktop-reference.json` records the reference commit, imported
style hashes and translation keys. LGPL notices accompany derived additions
and the small desktop-only CPU artwork fallback needed by AE2 15. Native names,
storage formatting, item artwork and ordinary CPU rendering delegate to AE2.

Resource imports read the sibling fork without changing it:

```powershell
python tools/import-ae2-desktop-resources.py --reference '..\Applied-Energistics-2-Salts-inventory-api-test'
```

`tools/transfer-ae2-native-deltas.py` rebases the reviewed deltas onto 26.1.2
and 26.2. `tools/transfer-ae2-legacy-common-deltas.py` handles AE2 19's native
signatures and codecs. For AE2 15, run the style/menu transfers, then
`transfer-ae2-15-screen-deltas.py --include-specialized --include-catalog --include-common`,
`transfer-ae2-15-packet-views.py` and `generate-ae2-15-hook-config.py`. Generate
both named and runtime namespaces using the loader's mappings and named
Minecraft JAR. `--catalog-only` updates that contract in isolation. Regenerated
contracts require renewed review and runtime checks before release activation.

`tools/report-ae2-port-validation.py --output <workspace-report>` records current
JUnit results, JAR hashes and scoped gameplay results. It checks the reference
HEAD, clean status, all 4,862 tracked-file hashes and its built JAR hash. The
reference is never built, patched or rewritten by this project.
