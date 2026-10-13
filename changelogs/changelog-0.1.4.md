# Salt's Inventory Update 0.1.4 Changelog

This update separates Salt's Inventory Update into an independent inventory framework and an optional Salt's Inventory Compatibility mod. It also overhauls the public API so other mods can integrate intricate interfaces while retaining their original artwork, widgets and inventory behavior, adds shared real-inventory companion windows and manager-routed controls, and brings the AE2 desktop integration into the optional extension.

The final public integration API is **version 5** and the desktop wire protocol is **version 4**. Salt clients and servers must use matching protocol versions. External integrations must rebuild for the matching Minecraft/loader artifact and declare `apiVersion=5`; older public API entrypoints and compatibility aliases are not retained.

## Core And Compatibility Separation

- Preserved the existing Prism multi-version structure: Core remains under `versions/<Minecraft>/<loader>`, and the extension lives under `compatibility/versions/<Minecraft>/<loader>` with independent metadata, entrypoints, conditional mixins and production packaging.
- Core keeps `salts_inventory_update` and owns inventory expansion, vanilla desktop windows, configuration, persistence, networking/security, public API and external-provider discovery. It has no dependency on the extension or its third-party integration targets.
- Added the optional `salts_inventory_compat` mod, requiring the exact matching Core version. Install matching artifacts for the same Minecraft version and loader to activate supported integrations; third-party mods remain optional.
- Migrated Tom's Simple Storage, JEI, REI, EMI, Fabric Mod Menu and the supported Forge/NeoForge Sophisticated Core, Backpacks and Storage implementations into separate integration packages. Their plugins, conditional mixins, event handlers and integration-specific resources now belong to the extension.
- Preserved configuration files, generic recipe-window persistence keys and desktop protocol identifiers. The provider-neutral recipe browser contract and no-provider fallback stay in Core, while third-party runtime implementations live in the extension.
- Extension compilers receive only the matching Core public API JAR, tracked as a Gradle input so API changes trigger recompilation. Core implementation classes are absent from that compiler classpath. Core is a separate development-runtime mod and is never shaded, nested or copied into the extension.
- Added build gates for imports, fully qualified calls, reflective names, encoded native patches, class constant pools, metadata and archive contents. Integration hooks cannot target private Core classes or bypass the API through reflection, access wideners or mixins.
- Isolated optional initialization by target presence and supported ABI, with lazy class resolution, transactional rollback and failure isolation so one unavailable integration does not prevent unrelated integrations from registering.

## Public API Additions For Extension Consumers

- Promoted recipe-browser models and `RecipeBrowserAccess`/`RecipeBrowserBridge` into `api.client.recipebrowser`. External providers can install and clear their backend with a namespaced source and priority; Core handles provider selection and the empty fallback.
- Added the generalized native player-inventory quick-move policy and storage-slot registration so integrations can retain native transfer behavior without importing Core menu helpers.
- Added `DesktopSessions` for authenticated native menu transitions, serialized opening-data capture, session-targeted payloads, active routing sessions, source identities and native ownership queries.
- Added guarded session-context `carriedStack`/`setCarriedStack` access for the canonical player cursor. Native action consumers commit the shared cursor before broadcasting; detached cursor copies are cleared to prevent duplicate settlement.
- Added `DesktopItemSources` for stable item-backed inventory identity and lifetime-scoped protection, plus `DesktopSynchronization` for retaining the active desktop synchronizer during native installation.
- Added `DesktopEnvironment` lifecycle, mod-presence, diagnostics and transactional registration hooks; client mutation readiness, configuration-screen factories and integration help registration; and supported payload-limit/theme-asset constants.
- Moved legacy graphics/input wrappers into supported `api.client.gui` and `api.client.input` packages and exposed native mouse-button normalization. Legacy consumers must update the moved imports and recompile; API version 5 and wire version 4 remain unchanged by the split.
- Expanded the generated signature reference to 86 public types and documented the new contracts as reusable third-party extension points rather than privileged bundled-integration access.

## Applied Energistics 2 Desktop Integration

- Ported the reviewed sibling fork's desktop additions into the extension while preserving unmodified installed upstream AE2 implementations. The reference repository and its built JAR remained read-only throughout the session.
- Added reviewed native adapters for AE2 15.4.11 on Fabric/Forge 1.20.1, AE2 19.2.18 on NeoForge 1.21.1, AE2 26.1.13-beta on NeoForge 26.1.2, AE2 26.2.1-alpha on NeoForge 26.2 and AE2 26.3.1-alpha on NeoForge 26.3.
- All twelve extension artifacts include the shared desktop additions. Native AE2 support is not claimed for Fabric 1.21.1/1.21.11/26.1.2/26.2/26.3 or NeoForge 1.21.11 because no reviewed upstream input was available for those targets. Other integrations remain available.
- Retained native screens, repositories, slot rendering/input, menu factories, opening serializers, packet registries/codecs and action allowlists. Verified instruction deltas attach desktop behavior to AE2 only; release JARs contain no native `appeng` implementation copies, upstream AE2 JARs or Core implementation classes.
- Imported the fork's 42 desktop screen profiles byte-for-byte and its 59 added translation keys, with provenance and LGPL notices. Ordinary screen documents and native textures come from the installed AE2 release, preserving resource-pack overrides.
- Added lifetime-addressed native updates and early-update queues, authenticated desktop actions, scoped menu reconstruction/navigation, detached menu cleanup, desktop management controls and shared toolbox companions tied to the original inventory's stack identity.
- Preserved native packet validation and bounded transport, including action/update allowlists, buffer ownership, trailing-byte checks and ordinary AE2 GUI synchronization. Crafting CPU desktop state uses a separate five-byte public session payload rather than changing ordinary AE2 `GuiSync` decoding.
- Added initial server-authoritative CPU state delivery and job/cancel updates. Disconnect cleanup releases receivers, queued updates and discarded menu/toolbox lifetimes.
- Each target uses its own upstream UI design: Minecraft 1.21.1 follows AE2 19, and 26.3 follows AE2 26.3. Older targets retain their native features and artwork instead of acquiring later ordinary AE2 behavior merely for screenshot parity.
- Preserved AE2 15's native knife/condenser geometry, larger 36-slot sky chest, combined chest menu and absence of newer key-type subviews. Added only the documented desktop adaptations needed for window controls, navigation, frame contours and the older CPU presentation.

## Runtime And Legacy UI Fixes

- Restored missing legacy 1.21.1 expansion capability state, corrected Tom's terminal cursor commits and fixed late NeoForge resource-listener registration found by in-world testing.
- Resent serialized opening data for every full 1.20.1 menu snapshot so reconstructed client menus retain their required native host/context; session replacement metadata is applied only on the initial opening.
- Corrected duplicate Sophisticated bindings, repeated-world Tom's registration and the Forge extension refmap identity. Forge development launches explicitly load the separate Core mixin configuration.
- Corrected live cursor-session validation to use the existing active session owner, preserving guarded native cursor actions on Forge 1.20.1.
- Fixed AE2 15 ME Chest and terminal bottom corners by leaving the final native footer rows for the footer pass. Plain terminals recognize their 97-pixel source footer and extend the native 176-pixel footer caps/center across the actual terminal width, removing the scrollbar-side border seam.
- Matched legacy pattern mode-tab bounds to the native 22-by-22 artwork, with 21-pixel shared-border spacing and the final tab aligned to the footer. Ordinary styles and custom bounds are preserved.
- Removed the crafting CPU selector's protruding lower join by finishing its shared rail after attachment and buffered-widget rendering. These visual corrections affect only the shared 1.20.1 compatibility adapter and its mapped native contracts; modern UI artwork is unchanged.

## Independent Builds, Reports And Session Verification

- The normal root `./gradlew build` or `.\gradlew.bat build` automatically builds both mods for all twelve targets and collects 24 correctly remapped production JARs. Individual Core and compatibility module builds remain available.
- Release paths are `dist/core/salts_inventory_update-<Minecraft>-<Loader>-0.1.4.jar` and `dist/compatibility/salts_inventory_compat-<Minecraft>-<Loader>-0.1.4.jar`, with `SHA256SUMS.txt` in both directories. Root distribution preparation and exact-output synchronization prevent stale, development, sources, intermediate, API/ABI and QA artifacts from being distributed. The legacy `build/release/0.1.4` bundle remains Core-only.
- Retained Prism's loader toolchains and separate publishing configuration. Modern NeoForge builds use the supported binary-patch pipeline without requiring `CI=true`, and prevent cross-loader artifact-cache reuse on affected targets.
- Added separation/extension guides, native version/ABI audits, reproducible resource/delta tools, portable merge regressions and a disposable-world gameplay harness. Atlas coverage verifies labels, geometry, controls, native menu counts and actual version-specific subview availability; missing supported views fail validation. QA classes and observer mixins are excluded from production packaging.
- Final normal root build passed: **849 JUnit tests across 114 suite reports, zero failures, errors or skipped tests**, plus **16 portable instruction-merge regressions**. API examples, source parity, compiler isolation, native symbol/stack validation and all 24 production archive boundary checks passed.
- Core-only in-world gameplay passed all twelve targets. Extension-without-targets and JEI, EMI, REI/Mod Menu, Tom's Storage and Sophisticated gameplay profiles passed their recorded scenarios. Separate production-artifact Core-only and paired Fabric 1.21.1 launches also passed using an independent QA mod.
- All six available native AE2 targets passed the four-menu gameplay profile: 312 records per AE2 15 loader and 319 per newer target. All six passed initial CPU delivery and two real crafting job/cancel cycles, including server cancellation and client idle resynchronization.
- Full native UI atlases passed: 76 presentations across 39 native menus/40 fixtures on each AE2 15 loader, and 90 presentations across 40 native menus on each AE2 19/26 target. Fresh final Fabric/Forge legacy runs passed after the four highlighted visual fixes.
- Produced a Core/Compatibility QA report and two grouped design reviews instead of duplicating a PDF for each Minecraft release: a 96-page modern review, including native version variations, and a 78-page legacy review with a clickable 40-container index. Regenerated the legacy review from fresh runtime captures after correcting the highlighted areas, rendered every page and checked source hashes, margins and links. The approved modern review remained unchanged.
- The final legacy correction changed only the two 1.20.1 compatibility production JARs; the other 22 release JAR hashes remained unchanged. Integrity checks also confirmed the reference HEAD, clean status, all 4,862 tracked-file hashes and reference JAR hash were unchanged.

These session totals supersede the earlier build counts below. Detailed architecture, executed scenarios and coverage limits are documented in the [separation guide](../docs/markdown/jar-separation.md), [separation validation report](../docs/markdown/jar-separation-validation.md), [AE2 guide](../compatibility/ae2/README.md) and [AE2 validation report](../docs/markdown/ae2-extension-validation.md). Generated review PDFs are local artifacts under `output/pdf`, including `ae2-legacy-design-review-fixed.pdf` as the corrected legacy review.

The following sections retain the earlier API and companion work included in the same release.

## Public API And Optional Integration

- Separated the common/server and client registries into `DesktopServerApi` and `DesktopClientApi`, replacing the old combined public entrypoint.
- Added optional integration discovery through `META-INF/salts_inventory_update.integration.properties` on Fabric, Forge and NeoForge. Providers register after mod registries exist, with separate common and client phases.
- Kept ordinary mod entrypoints independent of Salt API classes. Dedicated servers can register common integration behavior without resolving client providers or GUI classes.
- Added provider prerequisites, deterministic discovery, registration conflict handling, rollback of partial registrations and structured diagnostics recorded in the mod log.
- Added client-renderer capability negotiation and per-menu server eligibility. Menus without a complete eligible integration retain their normal screens.
- Promoted menu reconstruction factories, opening-data serialization, native render access, window presentation and custom slot bindings into documented public contracts.
- Added server-session callbacks and session-targeted custom payload routing, including the documented game-specific identifier, input and graphics differences across supported versions.

## Original Artwork And Intricate Interfaces

- Separated window management from presentation. Integrations can choose default Salt decoration, custom-rendered Salt-managed decoration or completely mod-owned surfaces.
- Mod-owned surfaces can retain their own frame, title, borders, padding, tabs and textures with zero host insets. Salt adds no mandatory outer frame, backing fill, title bar, controls or resize decoration.
- Added arbitrary-model client-only views with per-instance state and handles. These views do not receive inventory mutation privileges.
- Added coherent layout preparation and immutable committed surface, region, popup and decoration geometry shared by painting and hit testing.
- Added independent visual, interaction and placement regions for shaped/transparent interfaces and protruding attachments, with explicit control, slot, move and resize roles.
- Added GUI-pixel geometry, insets, transforms, scoped clipping and native graphics access. Original artwork dimensions can remain unchanged at different GUI scales.
- Added per-window rendering groups and overlays that respect foreground windows, rather than painting integration content above the entire desktop.
- Added explicit minimum/maximum sizes, snapping, fixed/free/storage-grid resize policies and author-selected `PRESERVE` or `ADAPT` viewport behavior.
- Added bounded virtual-row scrolling for large catalogs and terminal interfaces.

## Input, Accessibility And Lifecycle

- Added pointer capture, cancellation, external interaction regions and modal routing for native widgets and complex subviews.
- Separated keyboard ownership from inventory-transfer focus, with public focus state and lifecycle notifications.
- Added an instance-owned native widget host with traversal, narration, semantic labels and cleanup of hidden/disabled widget ownership. Duplicate widget instances are rejected.
- Added owned popups with outside-click/Escape dismissal and modal blocking. Hidden retained popups no longer consume outside input or narration.
- Added resource/viewport notifications, resource revisions, preference layout revisions and lifetime-owned asynchronous updates.
- Added stale-handle rejection, reentrant disposal cleanup and cancellation of captured interaction when a window or subview becomes unavailable.
- Fixed the legacy 1.20.1/1.21.1 public keyboard entry points to use the same integration focus/modal dispatch as newer versions.
- Fixed legacy key-event modifier handling to use captured event flags. Modern Shift-click routing now also uses the captured mouse-event modifiers instead of polling the physical keyboard.

## Shared Real-Inventory Companion Windows

- Added `DesktopInventoryIdentity`, `DesktopCompanionSpec`, `DesktopCompanionLease` and `DesktopServerSessionContext.acquireCompanion`.
- Multiple independent main windows can acquire the same companion through parent-lifetime ownership references. Repeated acquisition and release are idempotent.
- Companions deduplicate by the actual backing inventory/source identity, using a stable object reference or a namespaced, server-assigned persistent UUID. Window type, item type and title do not identify an inventory; different tools remain separate sources.
- Reusing a companion preserves its genuine menu/session, state, position and independently draggable lifetime. Closing one parent releases only that parent's association; closing the last automatically disposes the companion.
- Companion menus bind actual server-authorized inventory slots rather than copied inventories, client-only displays or proxies of a parent's slots. They remain usable after either associated parent closes.
- Preserved normal pickup, placement, Shift-click, quick-craft dragging/cancellation, native slot restrictions and the shared player cursor.
- Preserved the canonical cursor during parent/companion disposal by clearing only a detached menu's copied cursor before native removal, avoiding duplicate cursor settlement.
- Added source/permission guards checked on acquisition, lifecycle ticks and inventory authorization. Source removal, replacement and loss of authorization release invalid ownerships and dispose companions when no valid owner remains.
- Recipe-transfer source discovery now rejects revoked companion permissions before the next lifecycle tick, even if native menu validity still returns true. Another valid owner can retain authorization; stale owners cannot.
- Added ownership metadata updates that retain the existing client menu and input lifetime instead of reconstructing the companion.
- Added initial placement beside the first eligible owner, trying right, left, below and above with a four-GUI-pixel gap before viewport fallback. Later associations retain the dragged position.
- Automatic opening and ownership updates preserve the main window's keyboard and inventory-transfer focus and use ordinary window stacking.
- Companions hide when all owners are minimized, ghosted or persistently hidden. Restoring an eligible owner restores visibility without stealing focus or resetting position.
- Closing a parent cancels pending inventory gestures while preserving cursor contents. Losing the last eligible owner cancels companion movement/capture as well.
- Added cleanup for world changes and disconnects, including hidden desktops and portable menus whose native validity checks remain true. Dimension travel ends old-world window lifetimes while preserving connection negotiation and mutation sequencing.

## Management Controls Inside Mod-Owned Surfaces

- Added opt-in `DesktopSurfaceControl` geometry and `managementControls`/`renderManagementControls` callbacks for Close, Pin/Unpin, Minimize/Restore and More.
- Consumers define positions, dimensions, artwork and spacing inside their existing title area. Declaring controls does not add Salt framing or padding.
- Added public hover, pressed, enabled, selected and keyboard-focused paint states based on the same committed rectangles used for input.
- Controls take priority over title dragging. Actions recheck permissions, modality and lifetime on activation; stale handles cannot revive windows.
- Added translated tooltips, keyboard access and narration. Alt+F10 enters enabled controls before native widgets consume keys; Tab/Shift-Tab traverse them, Enter/Space activate them and Escape leaves control focus.
- Management menus now anchor to the consumer's More rectangle or the surface edge when native chrome is absent. Escape dismisses the menu before reaching owner input/closure.
- Minimized mod-owned windows retain their declared management controls so consumers can render a collapsed original title with Restore access.
- Added independent lock and management-menu permissions to the seven-field `DesktopWindowOptions`; retained ordinary built-in integration behavior through the existing five-argument constructor.
- Added `DesktopWindowOptions.DRAG_ONLY` and manager enforcement for companions. Independent close, pin, minimize, ghost, lock, management menu and resize are denied through visible controls, context menus, shortcuts, action requests and handle disposal. Owner release and world/session teardown still dispose the companion.
- Added public companion ownership/visibility snapshots through `DesktopWindowContext` and `DesktopWindowState`.

## Inventory Bindings And Server Authority

- Exposed stable logical slot IDs, physical inventory owner/index identity, custom slot ownership and gesture policies for nonstandard storage backends.
- Preserved native placement/pickup restrictions, capacity, quick-move bookkeeping and authentication while preventing duplicate transfers through multiple representations of the same physical slots.
- Added computed read-only slot/cursor gesture snapshots, previews and terminal notifications so original slot artwork can represent Salt gestures without changing inventory topology or creating extra cursors.
- Added documented server-validated recipe-transfer requests, normalized ingredient/destination decisions and bounded quantities. The integrating mod retains recipe interpretation, backend permissions and transaction validation.
- Migrated built-in Tom's Simple Storage and Sophisticated integrations from private presentation/menu helpers to the public contracts, preserving their existing integration behavior where possible.
- Updated Sophisticated hosted gesture previews to use the computed cross-window snapshot and selected target count while restoring native render state afterward.

## Documentation, Examples And Control Layout Rules

- Updated the Markdown guide, HTML documentation, README, API follow-up intake and generated public signature reference together. The current reference covers 86 public types after the separation extensions.
- Documented constructors/methods, defaults, identity and ownership rules, thread requirements, inventory routing, focus, teardown, optional loading, version differences and integration responsibilities.
- Added complete compilable machine, virtual terminal, native preferences, styled-decoration and shared-companion examples for all twelve supported targets.
- Added a standalone optional NeoForge 26.3 companion consumer with original textures, ordinary fallback screens, two main windows sharing one real nine-slot tool inventory and another distinct tool inventory. It bundles no Salt classes and does not require Salt.
- Added runnable native acceptance instructions and deterministic example-artwork generation.
- Clarified the standard native expanded button order: Sort when eligible, Focus, Pin, Lock, Link, Minimize when enabled, Close. If the full permitted list and full title cannot fit, the entire row compresses to **More (`…`) and Close (`X`)**. More is not an extra button in the standard expanded row; there are no progressive button-removal stages or shrinking icons.
- Documented 11×11-GUI-pixel buttons, 3-pixel gaps, a 2-pixel extra right inset, a 4-pixel top inset and the 6-pixel title/control separation. The trailing gap makes the total right-edge space 5 pixels.
- Documented the exact full-title fit threshold, control rectangle formulas, compact title budget, minimum title text, native-widget reservations and recalculation after width/title/font/resource changes.
- Corrected the guide so the fixed Pin/Minimize/More/Close example is not presented as the standard expanded native row. Public-reference links retain this clarification when documentation is regenerated.
- Documented that original bottom-border joins, protruding tabs and native artwork/layout alignment remain the integration's responsibility; Salt does not force replacement textures or tab scaling.

## Earlier API And Companion Verification

- Kept the supported matrix unchanged: Fabric on 1.20.1, 1.21.1, 1.21.11, 26.1.2, 26.2 and 26.3; Forge on 1.20.1; NeoForge on the other five versions.
- Verified all twelve target builds, shared consumer examples, functional harness compilation, source feature parity, release packaging and checksums.
- Passed 559 automated tests with zero failures/errors, covering identity, metadata bounds/copies, geometry, policy, keyboard/narration routing, inventory gestures, duplicate physical slots and source authorization.
- Passed six native acceptance runs on the final artifact hashes: complete Salt-present client suites at GUI scales 2/3/4, a Salt-absent original-screen fallback, and dedicated servers with Salt present/absent using the same consumer jar.
- Native client acceptance demonstrated real inventory/cursor conservation, both parent-closing orders, last-owner disposal, separate sources, title dragging, controls, hiding/restoration, modal/foreground blocking, resource reload, constrained viewports, source replacement/invalidation, dimension travel and disconnect cleanup.
- Inspected rendered captures and verified exact original border corners and framebuffer dimensions for 176×52 companion and 176×64 main GUI-pixel artwork at scales 2/3/4.
- Audited release jars for API5/protocol4 constants, intended public types and absence of removed aliases, example classes and functional harnesses. Audited the consumer's optional dependency and common/client class separation.

## Current Limits And Integration Notes

- The mod-owned `DesktopSurfaceControl` mechanism currently accepts only Pin, Minimize, More and Close. It does not expose every native button directly and has no public automatic control-layout helper. Complete native ordering/automatic compression is available through Salt-managed decoration and its public control states; the four-button demonstration is not full native layout parity.
- Companion ownership is scoped to a player's desktop connection, with a sixteen-session cap and no nested/cyclic companion graph. Persistent identity never substitutes for source authorization.
- Consumers own their real persisted inventory, stable source locator, permissions and native screen/menu assumptions. The demonstration's source inventories are intentionally ephemeral.
- `PRESERVE` retains GUI pixels and can clip multiple large windows in a small viewport. Consumers choose an adapted layout when needed.
- Earlier companion visual/network acceptance used NeoForge 26.3. Dedicated acceptance covered common-only startup and actual menu/backend behavior with a FakePlayer; it did not connect a real remote client. Subsequent separation/AE2 evidence is recorded above and in the linked reports.
- AE2 native gameplay checks used development game launchers with unmodified upstream AE2. Production archives passed remapping/bytecode/resource audits, but fresh installed-launcher AE2 gameplay, dedicated servers, multiplayer, latency/reconnect/dimension scenarios and arbitrary modpacks remain unverified.
- The extension ships the reviewed API5 AE2 integration on the six listed native targets; it does not claim automatic adaptation of every mod screen or native AE2 support where no upstream input was available. Native version artwork/feature differences and live screenshot content do not imply raw cross-version pixel identity.

See the [API guide](../docs/markdown/desktop_window_api.md), [public signature reference](../docs/markdown/api_reference.md), [recipe-transfer reference](../docs/markdown/recipe_transfer_api.md), [runnable companion example](../api-examples/companion-demo/README.md) and [final companion verification report](../API-COMPANIONS-0.1.4-PLAN.md) for the complete contracts and evidence.
