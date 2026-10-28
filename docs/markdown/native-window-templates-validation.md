# Native window templates: focused validation

Date: 2026-10-26. Scope: **Minecraft 1.20.1 Fabric and Forge**, Core 0.1.5 and matching Compatibility 0.1.3. These are focused automated and gameplay checks for the native-template work and representative existing API windows. A complete functional/gameplay suite and a twelve-target rebuild were not run.

## Implemented boundary

- Added `DesktopNativeTemplate`, context/view snapshots, immutable region/layout composition and the versioned JSON codec. Added explicit native matcher registration without removing existing public members or changing API version 6.
- The existing native host is selected automatically only for eligible unregistered modded containers. An API definition enters the new renderer only when it explicitly implements `DesktopNativeTemplate`; JSON profiles apply below API definitions in lookup priority.
- The host retains one original screen/menu, renders native artwork once per frame into an isolated target, composites its regions/cutouts and real slots, and maps input through the same plan. It restores the caller's actual framebuffer. Player ownership bindings, real slot IDs, shared cursor and normal server mutation validation remain authoritative.
- The inspector supports source selection, base replacement, adding/hiding/moving regions, padding, reset, preview and JSON export. Static resource/config profiles and Java layout callbacks use the same geometry engine. Resource reload and an explicit reload command refresh profile discovery.
- The separate optional native-template capability channel advertises described menu families; the original automatic-ready packet and ordinary API packet encodings are unchanged.

## Builds and automated checks

The following command passed:

```powershell
.\gradlew.bat :1.20.1:fabric:build :1.20.1:forge:build `
  :compatibility:1.20.1:fabric:build :compatibility:1.20.1:forge:build `
  :common:test --tests '*AutomaticContainerLayoutTest' `
  verifyNativeTemplateExample `
  :controlify:1.20.1:fabric:compileJava :controlify:1.20.1:forge:compileJava
```

- Core's configured Fabric test task: **178 tests, zero failures/errors/skips**. This includes eight new native composition tests and three new capability packet tests, plus the existing API window, input and session checks.
- Compatibility's configured Fabric test task: **70 tests, zero failures/errors/skips**.
- Focused shared automatic geometry task: **6 tests passed**.
- Forge's ordinary JUnit tasks are `NO-SOURCE`; they are not counted as duplicate test passes. Its configured Curios checks passed **448 placements plus overflow/full-grid checks and 1,305 visibility-toggle layouts**.
- Existing coverage-verifier Python checks: **6 passed**. Coverage metadata validation is source validation, not a gameplay test of those integrations.
- The native-template example compiled against Compatibility's API-only classpath for both loaders. Existing Controlify consumers compiled on both loaders; no Controlify sources or distributable JARs changed for this feature.
- Source boundary verification passed for **1,021 Compatibility classes, 485 native patches and Core sources**.
- Constant-pool checks passed on the four staged JARs: **523 Fabric Core, 525 Forge Core, 277 Fabric Compatibility and 288 Forge Compatibility classes**. Both compiled example outputs also reference only the supported SIU API.
- Artifact inspection found **154 Core API class files per loader**, including 11 added class files. Every previously public/protected field/method descriptor and every previous API class remained present. Production JARs contain no example/QA sources or test classes, and Compatibility embeds no Core/API implementation.
- `git diff --check` passed. Build logs retain ordinary existing deprecation warnings.

An existing resource-environment regression test caught profile loading before a Minecraft client/resource manager exists. Loading now safely defers in that state; the final automated run passed. The initial Forge gameplay fixture also registered its example before Forge populated the menu registry. Moving that **excluded test fixture's** registration to client startup resolved the fixture failure before the successful Forge run.

## Gameplay checks executed

Tests ran in disposable integrated-server worlds with production Core JARs, matching upstream mod fixtures and an excluded command/observation driver. Computer Use supplied visual inspection and real pointer actions for inspector selection/export, the added example button, the moved recipe button, Forge's native selector and the scrollbar. The historical task name `runFullGameplayQa` is reused by this focused driver; it does not mean the complete suite was run.

| Area | Executed result |
| --- | --- |
| Fabric native capture | Oxidized's kiln retained native processing artwork, real slots and recipe-book control inside the composed window. |
| Java API composition | Public-API-only furnace-like example inherited the automatic body, moved its output frame, moved the native recipe button when present, filled removed areas and added a functional Details button with consumer-owned state. |
| Input mapping | Clicking the output's old cutout did not pick up an item. Clicking the moved real output picked up the expected stack and cleared that same server slot. Placement/pickup through the original input retained the same server slot ID. Fuel restrictions still rejected an invalid item. |
| Recipe button | A real pointer click on the relocated native button opened Core's recipe panel. |
| Layout changes | Moving the templated window and changing GUI scale from 2 to 3 and back retained valid composition. Slot/widget anchor changes also have focused geometry coverage. |
| Inspector | Source selection, Use, Add, Hide, Move, Padding, Export, Reset and Exit were exercised. Selection/export did not pick up inventory items. Native slot/widget outlines and the external toolbar were visually inspected. |
| Export/reopen | Exported an Oxidized profile from the inspector, reopened it through profile discovery, and picked up items through its retained layout with matching client/server contents. |
| Multiple regions and scroll | A two-region Nether Chested profile included a noninteractive visual mirror and a second real storage region far down an 800-unit canvas. Wheel scrolling and a real pointer scrollbar drag revealed the lower region; clicking its slots picked up/returned the expected server stack. The window/inspector stayed bounded at the tested GUI size. |
| Safe profile fallback | A profile intentionally selecting half a real storage cell was rejected with a diagnostic. The inferred fallback remained usable and transferred the expected stack. |
| Forge native template | Brick & Mortar's native kiln opened through the public example, retained its native selector controls and original menu, and supported pickup from its moved output and placement into its real input. The inspector rendered correctly; a real pointer click changed its native selector. |
| Existing API definitions | Iron Chests: Restocked and Animal Feeding Trough kept their existing API definitions on **both loaders**. Opening and real slot pickup worked; neither entered the native-template renderer. |
| Core without Compatibility | A separate Fabric run staged Core and upstream fixtures with **no Compatibility JAR**. The API example, inspector edits/export, real slot transfers and invalid-profile fallback worked. |

Local evidence is under `build/native-template-qa/`: `release-build.log`, `artifact-audit.json`, `production-boundary.json`, and `gameplay/1.20.1-*` observations/screenshots. Successful focused runs are Fabric `template-fabric-01` (initial inspector/export), Fabric `template-fabric-02` (API/scroll/regressions), Forge `template-forge-02`, and Fabric `template-core-only-03`. These files are local build output and will be removed by a clean build.

## Produced artifacts

Only the affected 1.20.1 production artifacts were refreshed in distribution folders. Versions remain unchanged for this implementation task.

| Artifact | Path |
| --- | --- |
| Fabric Core 0.1.5 | `dist/core/salts_inventory_update-1.20.1-Fabric-0.1.5.jar` |
| Forge Core 0.1.5 | `dist/core/salts_inventory_update-1.20.1-Forge-0.1.5.jar` |
| Fabric Compatibility 0.1.3 | `dist/compatibility/salts_inventory_compat-1.20.1-Fabric-0.1.3.jar` |
| Forge Compatibility 0.1.3 | `dist/compatibility/salts_inventory_compat-1.20.1-Forge-0.1.3.jar` |

The two Core JARs contain the new functionality. Matching Compatibility artifacts were rebuilt against the API. Only remapped production archives were copied; no new stale version variants were left for these targets. Distribution checksum files were refreshed and copied bytes matched their module artifacts.

## Coverage limits

- No port to other Minecraft versions was attempted, and their existing distribution artifacts were not rebuilt.
- Dedicated remote multiplayer, Spatial GUI/Controlify runtime combinations, every mod configuration/resource pack/language, every native custom renderer, every GUI scale and arbitrary user-authored profiles were not exhaustively tested.
- Native text-field focus bounds and key-release forwarding, custom player-slot ownership descriptors and resource-pack replacement profiles have implementation/compile coverage but were not each exercised in gameplay. Final small hardening changes for ownership-aware descriptors, focused key-release forwarding and pre-client resource reload were compiled/automated-checked after the gameplay passes.
- Profiles use static geometry. Java anchors cover dynamic layouts, but they cannot infer the semantics of arbitrary tabs, virtual inventories, custom framebuffer code, menu-specific packets or screen-global side effects. Native source regions are not an automatic network adapter or universal UI interpreter.
- Fully custom API windows remain available for interfaces that need more than native composition. See [the API/profile/inspector guide](native-window-templates-1.20.1.md).
