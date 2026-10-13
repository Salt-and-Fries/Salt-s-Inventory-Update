# Changelog

## 0.1.4

The complete update record is [changelogs/changelog-0.1.4.md](changelogs/changelog-0.1.4.md).

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
