# Changelog

## 0.1.5

- Fixes NeoForge startup by loading vanilla grouped HUD hooks only on Fabric. Forge and NeoForge use their native overlay/layer visibility hooks.

- While **Return Hotbar to Inventory** is enabled, Window Movement Control temporarily restores the native hotbar and its indicators and allows held-slot selection. When movement control is inactive, wheel and hotbar-key selection stay locked to the current slot. Uses the configured binding and Hold/Toggle mode across all twelve Core targets.

- Hides health, armor, hunger, air, experience, mount indicators and the selected-item label with the hotbar while windows are open and **Return Hotbar to Inventory** is enabled, across all versions and loaders. The HUD returns during Window Movement Control, when windows close, or when the option is disabled.

- Adds the public HUD layout API for advanced HUD positioning mods, keeping detached hotbar/offhand overlays and interaction geometry synchronized across all versions/loaders.

The standalone release notes are [changelogs/changelog-0.1.5.md](changelogs/changelog-0.1.5.md).

- Adds optional Mouse Tweaks interoperability directly to Core across all twelve targets, including public-API inventory windows. Preserves upstream drag/wheel settings, authenticated desktop actions and standalone startup without Mouse Tweaks; no Compatibility extension is required.
- Stops a Mouse Tweaks wheel burst when a crafting or other result slot runs out, preventing later destination clicks from picking up unrelated player items.
- Fixes the first recipe-book click after closing and reopening its window. Adds a shared Hold/Toggle mode in the mod settings and Minecraft Controls, plus rebindable Window Movement Control (default Left Alt), across all twelve Core builds.
- Groups Core key bindings under Salt's Inventory Update in Minecraft Key Binds and keeps only Hold/Toggle in the main Controls menu. Hold-to-close and its help follow the vanilla Inventory binding, including keyboard and mouse remappings.
- Fixes Forge/NeoForge registration timing so all three Core bindings appear in Minecraft's normal Key Binds list.

- Fixes persistent hidden chest viewers and removes Minimize/Ghost Pins throughout Core, settings and the public API. API 6 requires integrations to recompile; normal pins and Persistent Windows remain supported.

- Unifies all twelve Core Minecraft/loader targets under version 0.1.5.
- Suppresses the automatic regular-window tutorial when an alternative workspace presentation is registered, including the Controlify companion. This applies even before a controller is connected. Manual help remains available, and the tutorial preference stays unseen until the user actually opens help.
- Preserves the normal automatic tutorial when no alternative workspace presentation is registered.
- Releases this fix across all twelve Minecraft/loader targets. Compatibility 0.1.2 and the Controlify companion 0.1.1 require this Core release.

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
