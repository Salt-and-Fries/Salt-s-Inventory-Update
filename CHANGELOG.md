# Changelog

## 0.1.5

This release applies to all twelve supported Minecraft/loader targets: Fabric and Forge on 1.20.1, and Fabric and NeoForge on 1.21.1, 1.21.11, 26.1.2, 26.2 and 26.3. Every Core JAR uses version **0.1.5**.

### Inventory windows and controls

- Adds **Sort this inventory** beside **Sort into open containers**, using the former minimize sprite location. Sorting merges compatible stacks and groups valuables, equipment, supplies, crafting/utility items and building materials, with bulk terrain last. Existing tags, item properties and creative-menu order cover modded items; datapacks can override categories. Stack components/NBT are preserved, and hotbar, equipment, crafting and machine slots are excluded. Older servers hide unsupported sorting through channel/capability negotiation, including the separate 1.20.1 capability check.
- Keeps the entire window inside all four desktop edges during dragging, resizing and resolution/GUI-scale changes. Windows can no longer hang halfway beyond the bottom or right edge.
- Matches Character's slot, player-preview and recipe-book positions to vanilla inventory across all targets. Removes the Health/Hunger/XP text and unused height, keeps a small bottom gap, and adds one editable 16 x 16 compact crafting-arrow sprite. Offhand visibility follows **Return Hotbar to Inventory** unless a public attachment requests that it remain visible.
- Fixes the recipe-book button requiring two clicks after closing and reopening a window with its recipe book open.
- Fixes physical chest lids staying open when Persistent Windows hides their windows. Restoring reopens the viewer; disposal releases it once. Persistent Windows, normal pins and saved positions remain supported.
- Removes Minimize, Ghost Pins and ghost opacity completely, including controls, settings, rendering, server state and public API members.
- Adds a shared **Hold / Toggle** setting for Window Movement Control in both the mod settings and Minecraft Controls. Hold remains the default; toggle state clears when leaving the workspace.
- Makes **Window Movement Control** rebindable in Minecraft Key Binds under **Salt's Inventory Update**, alongside the character and recipe-browser bindings. Left Alt remains the default, existing custom bindings are preserved, and the mod settings retain a Key Binds shortcut. The Controls screen keeps the Hold/Toggle selector without a duplicate movement-key button.
- Makes hold-to-close follow the player's vanilla Inventory binding, including keyboard and mouse remappings. Help and hold-duration labels reflect that binding instead of hardcoding E.
- Registers Core bindings during Forge/NeoForge's native key-mapping collection so they appear correctly in Minecraft Key Binds.

### Hotbar, HUD and movement

- With **Return Hotbar to Inventory** enabled and windows open, hides the native hotbar, health, armor, hunger, air, experience, mount indicators and selected-item label. Window Movement Control temporarily restores the hotbar and indicators and allows held-item selection; closing windows or disabling the option also restores them.
- Locks wheel and number-key hotbar selection while that hotbar is hidden and movement control is inactive. The behavior follows the configured movement binding and Hold/Toggle mode.
- Fixes NeoForge startup by applying vanilla grouped HUD mixins only on Fabric; Forge/NeoForge use their native overlay/layer hooks.
- Corrects NeoForge screen detection when no screen is open, avoiding repeated failed reflection lookups during ordinary movement.

### Optional Mouse Tweaks and Spatial GUI support

- Adds Mouse Tweaks interoperability directly to Core on all twelve targets. Core plus the matching upstream Mouse Tweaks JAR enables left-drag collection, Shift-drag transfers, right-drag placement and wheel transfers without the Compatibility extension. Mouse Tweaks' own settings remain authoritative.
- Includes real inventory slots exposed by public-API windows in Mouse Tweaks interactions across multiple windows. Virtual/filter, protected and covered slots retain their interaction rules. A wheel burst stops when a crafting/result slot runs out, preventing later clicks from picking up unrelated player items.
- Adds experimental optional **Spatial GUI 1.8** support on every supported target. The ordinary desktop becomes one 3D panel with the same fixed canvas, aspect ratio, window coordinates and four walls. Dragging cannot enlarge the canvas or progressively shrink windows.
- Coordinates projected UI input, physical-pointer world targeting, player reach/obstruction checks and Window Movement Control. Movement control smoothly returns to the normal first-person desktop and native camera input; releasing it returns to the spatial panel. Avatar clearance, detached hotbar/HUD placement and carried-item/count feedback follow the panel.
- Smooths inventory opening, movement-control release and closing all windows. Interrupted transitions, rapid reversals and reopening during closing continue from the visible pose; closing restores the saved gameplay perspective.
- Keeps Spatial GUI client-only, optional and inactive when absent. Servers need only the matching Core; no Compatibility extension or server-side Spatial GUI installation is required, and the existing server protocol is unchanged.

### Public API and integration infrastructure

- Moves the public API to **version 6** after removing Minimize/Ghost Pins. Consumers must recompile and update their provider descriptors for the reduced window options/state.
- Adds reusable HUD layout providers for live whole-layout and per-slot placement, scaling, visibility and interaction control. Detached hotbar/offhand rendering and pointer targets share the resolved geometry, including Mouse Tweaks lookup. Window-hosted, creative and Controlify layouts remain independent.
- Adds built-in Character attachments, foreground popup passes above the player preview, real offhand visibility requests and native equipment transfer/topology hooks. Dynamic slot snapshots reconcile capacity changes, preserve expanded storage and cancel stale gestures; the legacy click transport discards gestures from obsolete layouts.
- Adds built-in control contributions and equal-width joined upper menu windows with shared movement, bounds and lifetime, plus viewport fitting and scrolling. Joined layouts, desktop-availability and menu-capacity queries are available across all supported mappings. Existing legacy Trinkets attachment callbacks remain supported.
- Keeps native Trinkets, Curios and Raised implementations in Compatibility; these integrations consume the same supported public API as other mods.

### Configuration, artwork and packaging

- Makes Fabric Mod Menu's Core Config button work independently of Compatibility on every supported version. Forge and NeoForge retain their native config factories; Mod Menu remains optional.
- Suppresses the regular-window tutorial when an alternative workspace presentation is registered, including Controlify before a controller connects. Manual help remains available, the unseen tutorial preference is preserved, and ordinary Core retains its automatic tutorial.
- Centralizes Core textures under `artwork/shared-resources/core` and provides separate shared artwork inputs for Compatibility and Controlify. Builds track edits across versions/loaders without changing existing resource identifiers. Native third-party designs remain version-specific.
- Fixes rejected Core pack metadata on 1.21.11: Fabric uses current explicit format bounds, and NeoForge synthesizes client-resource/server-data metadata. Artifact checks enforce the loader-specific policies.
- Produces twelve Core JARs in `dist/core`, alongside the independently versioned companion outputs, checksum files and the 36-JAR bundle. Production checks exclude QA, editor and intermediate artifacts.

### Validation and installation

The final full-project build passed **1,637 fresh unit tests in 224 suites**, with zero failures, errors or skips. Across all three mods, validation passed **220 client gameplay/UI profiles, 58 dedicated-server profiles and twelve source-bootstrap smoke profiles**, plus three independent physical-input/multiplayer profiles. All 36 final JAR hashes match their accepted runtime evidence. The [full validation report](functional-tests/FULL-VALIDATION-20261023.md) records fixes, executed scenarios, reviewed diagnostics and coverage limits.

Core works independently. Optional **Compatibility 0.1.2** and **Controlify 0.1.1** require Core **0.1.5** for the same Minecraft version and loader. Fabric also requires the matching Fabric API.

Use this same changelog for every Core 0.1.5 artifact.

Standalone release notes: [Update Core 0.1.5](changelogs/changelog-0.1.5.md).

## 0.1.4.1 — Minecraft 1.20.1 only

- Publishes the updated Fabric and Forge 1.20.1 Core builds under a distinct hotfix version, including the public compact recipe-layout and native-tooltip hooks required by the updated Compatibility integration.
- Compatibility 0.1.1 requires Core 0.1.4.1 on Minecraft 1.20.1. All other Core builds remain at 0.1.4. This release-number change adds no further gameplay changes.

## 0.1.4

The complete update record is [changelogs/changelog-0.1.4.md](changelogs/changelog-0.1.4.md).

The subsequent [Compatibility 0.1.0 coverage audit](docs/markdown/compatibility-coverage.md) records the additional EMI and Sophisticated Fabric targets, legacy Tom's repairs and fresh validation. The [companion changelog](compatibility/CHANGELOG.md) tracks its independent version.

- Added the supported compact recipe-layout and native-component tooltip hooks to the Core 1.20.1 public API, matching the 1.21.1 provider contract. Use the rebuilt matching Core with the updated 1.20.1 companion.

- Split the project into independent Core (`salts_inventory_update`) and optional Compatibility (`salts_inventory_compat`) mods across all twelve existing Minecraft/loader targets. Core retains the inventory framework and works without the extension; the extension requires matching Core and keeps third-party targets optional.
- Moved Tom's Simple Storage, JEI, REI, EMI, Fabric Mod Menu and the supported Sophisticated Core/Backpacks/Storage integrations into the extension, including their plugins, conditional mixins, assets and lifecycle hooks.
- Enforced public-API-only extension compilation and source/production-bytecode checks. Compatibility cannot access Core implementation classes, and neither Core nor upstream integrations are embedded in the extension.
- Added public recipe-provider registration, native quick-move/session transitions, shared cursor access, item-source protection, synchronizer preservation, lifecycle/diagnostics, configuration/help factories, theme assets and legacy rendering/input wrappers.
- Added AE2 desktop integration using unmodified upstream AE2: 15.4.11 on Fabric/Forge 1.20.1; 19.2.18 on NeoForge 1.21.1; and the reviewed 26.x releases on NeoForge 26.1.2, 26.2 and 26.3. Native support is not claimed for the six targets without available upstream AE2 inputs.
- Preserved each installed AE2 version's own design, native behavior and resource-pack overrides. Added desktop menus, authenticated actions, native packet adapters, toolbox companions, navigation, controls and server-authoritative crafting CPU updates through the public API.
- Fixed runtime issues found during the split and port: legacy expansion capability state, Tom's cursor commits, repeated full-snapshot opening data, late resource listeners, duplicate Sophisticated registrations and live cursor-session lookup.
- Corrected legacy AE2 chest/terminal bottom corners and scrollbar border joins, pattern mode-tab sizing/alignment and the crafting CPU selector join. Modern artwork remains unchanged.
- Made ordinary root builds produce 12 remapped Core JARs in `dist/core` and 12 remapped extension JARs in `dist/compatibility`, with checksums, stale-output prevention and exclusion of development, sources, ABI and QA artifacts. Individual module builds remain available.
- Added separation/AE2 architecture guides, reproducible native-delta and boundary audits, gameplay/atlas regression tooling, a Core/Compatibility QA PDF and grouped legacy/modern design PDFs. Refreshed the 78-page legacy review after the texture fixes.
- Final session validation passed the root build, 849 JUnit tests and 16 portable delta regressions. Core-only gameplay passed all twelve targets; all six available native AE2 targets passed their gameplay, full UI atlas and real crafting job/cancel scenarios. Reports distinguish development-launcher coverage from unverified installed-launcher, dedicated-server and multiplayer AE2 scenarios.

The earlier API and companion work included in this release is recorded below; its 559-test acceptance record predates the final session totals above.

- Redesigned the public API as version 5 with separate common/server and client registries. Removed the old combined entrypoint and migrated internal integrations; no adapters for earlier public API consumers.
- Added shared, authenticated inventory companion sessions with source identity, parent-scoped ownership leases, automatic last-owner disposal and a manager-enforced drag-only policy.
- Added opt-in management controls inside original mod-owned artwork, committed consumer geometry, public hover/press/selection/keyboard states, native tooltips/narration and title-space examples. Added independent lock/menu permissions and visible-content snapshots.
- Updated the desktop wire protocol to version 4 for authenticated companion ownership metadata; documented explicit consumer API5 rebuild/descriptor requirements.
- Added optional descriptor discovery on Fabric, Forge and NeoForge, delayed phase registration, dedicated-server client separation, registration rollback and structured diagnostics.
- Added arbitrary-model client-only views, per-instance handles and lifecycle cleanup, with inventory actions explicitly denied.
- Exposed native render access, scoped clipping/transforms, overlay and external-region hosting, pointer capture/cancellation, keyboard ownership, modal input routing, window options and min/max sizing.
- Added bounded virtual-row scrolling and explicit custom slot ownership/gesture bindings. Promoted menu reconstruction factories and opening-data transport for all loaders.
- Added client renderer negotiation and per-menu server eligibility so incomplete automatic integrations retain their normal screens.
- Added compilable machine, virtual terminal and native preferences examples for all twelve supported targets, optional-loading fixtures and API contract tests. Replaced the API documentation.

- Separated window management from presentation: original mod-owned surfaces have no mandatory frame, fill, title controls or padding; styled Salt decoration shares committed paint/input geometry.
- Added shape/placement regions, explicit hit roles, validated management actions/state, preserved artwork dimensions and author-selected viewport adaptation.
- Added computed read-only slot/cursor gesture snapshots and terminal notifications, native widget focus/narration helpers, popup ownership/dismissal, resource/viewport notifications and preference layout revisions.
- Added lifetime-owned asynchronous updates and expanded compiling examples/reference documentation, including a standalone optional nine-slot companion consumer.
- Fixed captured modifier routing, hidden popup/widget ownership, companion cursor teardown, world-transition cleanup without repeated handshakes and recipe-source permission revocation before the next lifecycle tick.
- Documented the complete native expanded button list and its all-at-once compression to More/Close, including exact title-fit thresholds, spacing and placement formulas. Clarified that Pin/Minimize/More/Close is a limited mod-owned example, not the standard native row; exposing every native button through that mechanism remains an API limit.
- Verified 559 automated tests, all twelve builds/examples and six exact-artifact native runs, including GUI scales 2/3/4, optional loading and both dedicated-server modes. The per-update record describes verification scope and remaining integration limits.

Verification evidence for companions is tracked in `API-COMPANIONS-0.1.4-PLAN.md`; the earlier surface overhaul is recorded in `API-SURFACE-0.1.4-PLAN.md`. Session architecture and runtime evidence are recorded in the [separation report](docs/markdown/jar-separation-validation.md) and [AE2 report](docs/markdown/ae2-extension-validation.md). The supported Minecraft/loader matrix is unchanged.
