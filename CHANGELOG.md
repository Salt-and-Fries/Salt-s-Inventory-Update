# Changelog

## 0.1.4

The complete update record is [changelogs/changelog-0.1.4.md](changelogs/changelog-0.1.4.md).

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

Verification evidence for companions is tracked in `API-COMPANIONS-0.1.4-PLAN.md`; the earlier surface overhaul is recorded in `API-SURFACE-0.1.4-PLAN.md`. The supported Minecraft/loader matrix is unchanged.
