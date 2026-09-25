# Sophisticated compatibility validation — 2026-09-08

Status: in progress. Build/source checks are not a claim of complete gameplay coverage.

## Verified

- All ten loader/version artifacts assembled; `verifyReleaseBundle` and `sourceFeatureParity` passed after the first fixes.
- Fabric artifacts retain the Sophisticated exclusion.
- 1.20.1 Forge, both Sophisticated mods installed: launched and loaded an existing development world (backed up first).
- Opened item backpack without a crash; opened player creative inventory alongside it.
- Dragged 59 oak planks into two backpack slots: 29 + 29, with 1 remaining.
- Reproduced double-click collection failing to include that backpack. Fixed dispatch to use the authenticated multi-menu operation; restarted and verified 1 + 29 + 29 = 59 in player inventory, backpack empty.
- Opened backpack settings; reproduced invisible right-tab icons. Added legacy rendering boundaries; restarted and verified all five settings icons visible.
- 1.21.1 NeoForge: after the hook corrections below, launched with both mods, loaded a backed-up development world, created/opened an item backpack, opened settings with all tab icons visible, and shut down with world-save completion confirmed in the log.
- 1.21.11, 26.1.2, and 26.2 NeoForge: launched with both mods, created disposable creative test worlds, opened an item backpack and its settings with visible tab icons, then closed the clients normally. The final 1.21.11 crafting Tuple fix still needs a fresh runtime upgrade check.
- Rebuilt all ten artifacts and reran packaging/parity after the ordinary-slot identity fix: `BUILD SUCCESSFUL`, 72 tasks; all new double-click source assertions passed.
- 1.20.1 ordinary backpack-slot double-click retest: distributed 59 planks as 29 + 29 + 1, double-clicked a backpack slot, returned all 59 to player inventory; backpack empty.
- 1.20.1 crafting upgrade: reproduced server rejection when inserting into the upgrade rail. After correcting logical-slot validation, restarted and verified one-click installation, immediate tab appearance, and panel opening without a crash.
- Dragged 59 planks over two crafting-panel slots (29 + 29, one remainder); double-clicked the player remainder and collected all 59 from the external crafting slots. Confirmed 59 in the HUD after closing the desktop.
- Final post-logical-slot build: `verifyReleaseBundle sourceFeatureParity` passed in 41 seconds, 72 tasks. This verifies ten artifacts, not ten runtime gameplay suites.

## Fixes identified

- 1.21.1 search-box mixin used `extractWidget`, absent from its pinned Core jar. Changed to verified `renderBg`.
- 1.20.1 and 1.21.11 double-click branches bypassed `dispatchPickupAll`. Restored multi-menu dispatch and pending carried-sync handling.
- 1.20.1 ordinary slot-click call sites discarded previous-slot identity, unlike its creative call sites. Passed the captured key through both ordinary call sites; added cross-target regression assertions.
- 1.20.1 server click validation, quick-move source resolution, and diagnostic slot snapshots still used vanilla `menu.slots`. Changed these to `DesktopMenuSlots.slot` so authenticated upgrade slots are accepted; added cross-target click/snapshot assertions. The newer targets already used logical slot resolution.
- 1.20.1 native tab icons needed a rendering boundary after Salt tab plates and before the main frame.
- 1.21.1 launch rejected a manually installed JEI 19.27.0.346 (Core requires at least 19.32.0.359). Preserved the old jar in that client's `mods.disabled` directory.
- 1.21.1: corrected immediate-tooltip method, predicate-based opener counting, legacy pick-item packet, two-argument slot rendering/hover access, and crafting popup Tuple field. The initial bad adapters produced runtime mixin failures despite successful compilation.
- 1.21.11: verified SearchBox positioning lives in `renderWidget`; corrected its selector. Corrected JavaFML language range to `[10,11)` (runtime advertises 10.0, not FML implementation version 10.0.36).
- Disabled manually installed JEI jars in 1.21.11 and 26.1.2 test runs with a recoverable `.jar.disabled` suffix to isolate Sophisticated compatibility testing.

## Still required

- Full container-type and interaction checks on all five Forge/NeoForge targets (basic item-backpack/settings opening is verified, not the full matrix).
- No-mod and each-mod-alone launch combinations.
- Multiple independent storage sessions, cross-window drag previews, right-drag, shift-click/double-click, source locking, upgrades, limited barrels, settings/back, and reconnect cleanup across every target.
- Packaged-jar runtime checks (development launches alone do not test remapping).
- Follow up on the short main backpack retaining the taller settings-view height after navigation/reopening. Also observed a missing `fabric:textures/gui/creative_buttons.png` warning on Forge; this did not crash the tested interactions but is not a clean visual result.

The computer-use API exposes left-button endpoint dragging but not held right-button multi-point dragging. Right-drag preview validation needs an in-client test harness or a manual check; it must not be marked passed based on left-drag results.
