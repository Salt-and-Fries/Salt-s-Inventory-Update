# API5 companion and original-surface controls: implementation report

**Status: complete, verified on the final artifacts.** Implementation, documentation, all-target builds, 559 automated tests and all six native acceptance runs pass. This file replaces the earlier checkpoint plan; older failed/intermediate runs are diagnostic history.

## Public changes

The public integration API is **5** and the desktop wire protocol is **4**. Consumers must rebuild for their matching Minecraft/loader artifact and set `apiVersion=5` in their optional integration descriptor. Client/server Salt protocol versions must match. No adapter for older public API consumers or mixed wire protocols is supplied. Existing built-in integrations retain their ordinary presentation and five-argument options behavior.

| Contract | Behavior |
| --- | --- |
| `DesktopInventoryIdentity.reference(Object)` | Stable actual backing source/inventory identity by reference, independent of menu type/title. |
| `DesktopInventoryIdentity.persistent(String, UUID)` | Namespaced, server-assigned identity persisted on a real source. Copies/new tools require new IDs; identity alone never authorizes access. |
| `DesktopCompanionSpec(identity, provider, authorization)` | Creates a genuine registered server menu over the actual inventory, with an independent source permission guard. |
| `DesktopServerSessionContext.acquireCompanion(spec)` | Returns an optional server-thread `DesktopCompanionLease`, idempotent per parent lifetime and identity. Multiple parents share one companion menu/token. |
| `DesktopCompanionLease.sessionId/isOpen/close` | Parent-scoped reference. Releasing one leaves the others intact; automatic final release disposes the companion. Repeated release/disposal is harmless. |
| `DesktopWindowContext.companion/companionOwnerSessionIds/visible` | Public companion ownership and eligibility snapshots. `DesktopWindowState.visible` includes companion hiding. |
| `DesktopWindowOptions.DRAG_ONLY` | Movable; independent close, pin, minimize, ghost, lock, menu and resize denied. The companion manager enforces the policy throughout actions, handles, shortcuts and menus. |
| `DesktopSurfaceControl` and `managementControls/renderManagementControls` | Opt-in Close, Pin/Unpin, Minimize/Restore and More at consumer-defined rectangles inside original artwork. No added frame, fill, title or padding. |
| `DesktopControlState.keyboardFocused` | Complements hover, pressed, enabled and selected states. Manager tooltips/narration and Alt+F10/Tab/Enter/Space access use committed control geometry. |
| Seven-field `DesktopWindowOptions` | Independent lock and management-menu permissions; the existing five-argument constructor preserves ordinary integration defaults. |

The first acquisition supplies the companion's menu/title/opening data. Later owners reuse its state and independently draggable position. Acquisition cannot evict a parent at the sixteen-session cap; nested companion ownership is unsupported. Owners sharing an identity must describe the same inventory and compatible slot rules. Guards, native menu validity and actual physical slot identity remain authoritative.

## Inventory, visibility and teardown

The companion uses its own authenticated server session and actual slots, including pickup, placement, Shift-click and quick-craft dragging. Its inventory is not copied or represented through another parent's slot proxy. There is one canonical player cursor. Removing a detached menu clears only its copied cursor before native removal, avoiding duplicate settlement. Already authorized mutations remain committed when a pending gesture is cancelled.

Closing either parent releases its reference before consumer teardown callbacks; closing the final parent invalidates the companion's handle and menu once. Source removal/replacement, permission loss, dimension travel and disconnect expire the applicable lifetimes. World changes clean up even while the desktop is hidden, preserving connection negotiation and mutation sequencing during travel. Recipe-transfer source discovery checks companion guards before the next lifecycle tick, including when native menu validity remains true.

Initial placement tries right, left, below and above the first eligible owner with a four-GUI-pixel gap. Later associations do not move/promote the companion or steal keyboard/inventory-transfer focus. It enters ordinary window stacking, respects foreground surfaces and modal blocking, and hides when all owners are minimized, ghosted or persistently hidden. Restoring an eligible owner restores visibility without selecting it. Owner closure cancels a pending inventory gesture; losing the final eligible owner cancels a companion move as well.

All client rendering/layout/input/handle mutation occurs on the client main thread; server leases, guards, menu creation and callbacks use the server main thread. The guide documents safe lifetime-scheduled asynchronous updates and prohibits retaining callback contexts.

## Documentation and runnable artifacts

- [Markdown API guide](docs/markdown/desktop_window_api.md), including the complete companion/control contract, identity/guard rules, constructors, threading, focus, transitions, teardown, native GUI pixels and original artwork.
- [Public signature reference](docs/markdown/api_reference.md): **64 public types**, synchronized with current declarations and game-specific naming notes.
- [HTML documentation](docs/index.html): synchronized with the Markdown guide; 17 chapters, four tables, nine unchanged code samples and validated repository links.
- [Companion guide entry](docs/markdown/companion_windows.md) and [complete consumer sources](api-examples/ui/src/main/java/example/companion).
- [Runnable consumer instructions](api-examples/companion-demo/README.md), normal fallback screens, common/client registration, actual nine-slot backend, original textures and opt-in native acceptance driver.
- [Standalone NeoForge 26.3 consumer jar](build/api-examples/companion-example-26.3-NeoForge-api5.jar).
- [Twelve release jars](build/release/0.1.4) and [release checksums](build/release/0.1.4/SHA256SUMS.txt).

The consumer jar contains no Salt classes and requires no Salt dependency. Ordinary entrypoints, menus and source models have no Salt class references. Common providers contain no client references. The exact same consumer is exercised with Salt present and absent. AE fork source and artifacts were not changed by this goal; the local launcher borrowed only cached native dependencies and removed AE classes/old Salt jars from the launch.

Final consumer SHA-256:
`E18F7C1929AF8F4B14783111E63C519C690E3B5284A3FD0C5FEB40E4C7EB9B71`

Final tested NeoForge 26.3 Salt SHA-256:
`F3F4C7A64A040ED18E86653F68FCE445F3E8FA8945C36D7A3509423966DD390A`

The tested Salt jar is byte-identical to its collected release artifact. [Jar audit](build/companion-api5-jar-audit.json) verifies every expected public type, API5/protocol4 constants, removed-type exclusions, absence of example/test-harness classes in release jars and optional consumer dependency/class separation.

## Verification

`CI=true ./gradlew functionalTestCompile -PincludeFunctionalTests=true` passed on all twelve targets. Then the normal release build ran sequentially with `build :26.3:neoforge:companionDemoJar -I functional-tests/sort-tests.gradle`. It passed source feature parity, all-target examples, release hygiene/bundle checks and checksums. [Functional compilation log](build/companion-api5-verified-functional-compile.log); [release/build log](build/companion-api5-verified-release.log).

**559 automated tests passed**, zero failures/errors: common 65; 1.20.1 82; 1.21.1 83; 1.21.11 82; 26.1.2 82; 26.2 82; 26.3 83. Coverage includes source identity, ownership metadata codec/copies/bounds, placement, drag-only/action policy, geometry/control priority, keyboard/narration routing, captured Shift modifiers, duplicate physical slot transfers and recipe-source permission revocation independently of native menu validity. The latter verifies that another authorized owner can keep the companion usable while a stale owner cannot retain access.

| Native run, final hashes | Result | Evidence |
| --- | --- | --- |
| Salt present, GUI scale 2 | PASS, exit 0 | [Log](build/companion-native-verified2-present/client.log), [exit/hash record](build/companion-native-verified2-present/exit.json), [screenshots](build/companion-native-verified2-present/screenshots). |
| Salt present, GUI scale 3 | PASS, exit 0 | [Log](build/companion-native-verified3-present/client.log), [exit/hash record](build/companion-native-verified3-present/exit.json), [screenshots](build/companion-native-verified3-present/screenshots). |
| Salt present, GUI scale 4 | PASS, exit 0 | [Log](build/companion-native-verified4-present/client.log), [exit/hash record](build/companion-native-verified4-present/exit.json), [screenshots](build/companion-native-verified4-present/screenshots). |
| Salt absent, same consumer client | PASS, exit 0; original fallback | [Log](build/companion-native-verified-absent/client.log), [exit/hash record](build/companion-native-verified-absent/exit.json), [screenshot](build/companion-native-verified-absent/screenshots). |
| Dedicated server, Salt present | PASS, exit 0 | [Log](build/companion-native-verified-present-server/server.log), [exit/hash record](build/companion-native-verified-present-server/exit.json). |
| Dedicated server, Salt absent | PASS, exit 0 | [Log](build/companion-native-verified-absent-server/server.log), [exit/hash record](build/companion-native-verified-absent-server/exit.json). |

The full native client suite asserts actual integrated-server counts and conservation of 28 items throughout:

1. Two main sessions sharing one genuine nine-slot companion; repeated acquisition returns the same ownership/lifetime and retains its dragged position.
2. All forbidden companion actions and independent handle disposal denied; native title-pointer dragging works.
3. Real pickup/place, read-only/capacity restrictions, Shift-click with a held cursor, quick-craft dragging and cancellation when a parent closes.
4. Continued inventory use after either parent closes, final-owner disposal with a nonempty cursor, reopening actual retained storage in a new lifetime.
5. Separate tools producing separate inventories/companions; all-owner hiding/restoration retaining position and lifetime, without automatic focus theft.
6. Original-artwork Close, Pin/Unpin, Minimize/Restore and More; keyboard access, stale handles, foreground hit blocking and modal blocking.
7. Actual resource reload, revised resource state and retained inventory; smaller viewport preserving native dimensions.
8. Source invalidation/replacement during interaction, preserved cursor items, world handle invalidation, zero remaining server leases, opening a new-world lifetime and disconnect disposing every window.

Dedicated runs exercise actual menus/backing slots, pickup, restrictions, native quick move, revocation and native teardown using a server FakePlayer. They prove common-only class loading with and without Salt. Authenticated desktop packet routing is demonstrated by the actual integrated native clients.

Screenshots are inspected for original frames, control spacing, manager-menu anchoring, normal stacking/modal presentation and constrained viewports. Pixel auditing checks the original border corners and exact framebuffer sizes corresponding to 176×52 companion and 176×64 main GUI pixels. The original textures are consumer-owned and unchanged by Salt.

The [final evidence manifest](build/companion-api5-verification.json) validates every test count, completion marker, zero exit code, consumer/Salt hash and screenshot list against the current release artifacts. [Pixel evidence](build/companion-api5-pixels.json) confirms the companion is 352×104, 528×156 and 704×208 framebuffer pixels at actual scales 2/3/4, with its original border corners intact; the main remains 176×64 GUI pixels too. All native processes have exited.

Documentation generators/checks and normal `git diff --check` pass. No commits or pushes were made.

## Remaining limits

- Native visual/network acceptance uses **NeoForge 26.3**. All twelve targets compile; mapped automated suites run on all six Minecraft versions. This does not claim native visual acceptance on every loader/version combination.
- Dedicated startup/backend acceptance uses a FakePlayer; a real remote-client/dedicated-server pair was not exercised. Integrated clients demonstrate the actual authenticated session protocol and inventory behavior.
- The fixture inventories are deliberately ephemeral. Production integrations supply their real persisted inventory and stable locator/authorization; they must keep menus independent of any particular parent screen and obey native backend bookkeeping.
- `PRESERVE` retains original GUI pixels and can clip multiple large windows in a small viewport. Consumers choose a compact `ADAPT` layout when needed; Salt does not resize AE tabs or replace textures automatically.
- There is a sixteen-session cap and no nested/cyclic companion graph. Persistent identities must distinguish copied tools. These are documented contracts.
- The AE fork was not redesigned or rebuilt for API5 as part of this goal. Its integration must adopt the documented public contracts; this report does not claim that AE itself has been converted.
