# Automatic native containers: focused validation

Date: 2026-10-26. Scope: **Minecraft 1.20.1 Fabric and Forge**, Core 0.1.5. This is a focused validation of the new automatic host and representative API-window regressions, not a complete functional or gameplay suite.

This records the automatic-host stage **before** the later additive native-template API. Its API byte-identity figures below are historical. The current API additions, rebuilt artifacts and later focused checks are recorded in [native-window-templates-validation.md](native-window-templates-validation.md).

## Architecture checks

- Automatic admission and rendering are separate from public API registration. Registered server handlers exclude automatic admission; matching client definitions are selected first.
- The six target integrations have no container-specific renderer, texture copy or public API registration in this implementation. Core reuses their native menu and screen instances.
- All **143 public API class files** in each rebuilt Core JAR are byte-for-byte identical to the previous 1.20.1 distribution JAR for that loader.
- Source boundary verification passed: 1,021 Compatibility classes, 485 native patches and all Core sources. No new public API signatures or Controlify sources changed.
- Production JAR inspection verified metadata, the automatic host in Core, absence of QA/source classes, absence of Core/API implementation copies in Compatibility, and absence of Fabric classes/metadata in the Forge artifacts.

## Native gameplay checks

| Mod and pinned fixture | Loader tested | Focused observations |
|---|---|---|
| Oxidized 1.8.2 | Fabric | Copper kiln artwork, fuel/progress animation, native processing result, pickup into the separate player inventory, close/reopen and window movement. Physical pointer checks opened/closed the original recipe book, searched it and selected a recipe. |
| Brick & Mortar 1.2.1 / 1.2.2 | Fabric / Forge | Native kiln artwork, fuel/progress animation, processing result, transfer, reopen and movement. Physical clicks exercised all three hatch controls. Expanded preview retained all four item images, contracted when closed and continued rendering and accepting controls after moving its window. |
| Expanded Delight 0.3.2 | Fabric | Juicer upper-panel artwork, native processing to apple juice, result pickup/transfer, close/reopen and movement. The driver also attempted the locked juice cell, but that attempt alone does not establish every populated buffer restriction. |
| Crabber's Delight 1.2.3 | Forge | Crab trap native artwork and slots; pickup, split, place, merge, reopen, movement and storage sorting. Stone was rejected by the bait slot; cod was accepted. Advancing the native trap timer in the disposable fixture produced real loot while hosted. Sorting excluded the bait slot. Destroying the trap closed its session. Palm Cabinet retained the existing vanilla-container route. |
| Nether Chested 8.0.4 | Fabric / Forge | Original full grid artwork and exact counts 64, 127, 128, 256 and 512; pickup, split, placement, merge and reopening. Two automatic windows shared a 512-item transfer without client/server count disagreement. Destroying one source left the other window open; leaving reach closed the remaining source. |
| Inmis 2.7.2 | Fabric | Frayed and Endless native layouts; storage interactions, close/reopen and movement. A same-item reopen retained contents. The held backpack could not be picked up from its source slot; replacing it closed the session. Multi-item smart sorting merged dirt stacks, put diamonds first and persisted the sorted contents on reopening. Ender Pouch retained its existing vanilla-container route. |

The Crabber's Delight timer was accelerated using the game's block-data command; this verifies the native processing path while hosted, not the upstream mod's elapsed real-time catch rate.

Client and server slot/cursor snapshots were compared throughout the applicable interaction sequences. A dedicated shared-cursor assertion confirmed that one 512-item stack moved from the first automatic chest to the second, leaving an empty cursor.

## API and fallback checks

- Iron Chests: Restocked and Animal Feeding Trough retained their **existing API definitions on both loaders**. Their focused opening, item interaction, reopening, movement and sorting checks ran with Compatibility installed.
- A separate Forge run loaded **Core plus the upstream fixtures without Compatibility or Controlify**. The crab trap, Nether Chest and kiln were hosted automatically, including native preview clipping and controls after movement.
- The decline request reopened the original native screen on both loaders and kept that menu type native for the connection.
- A one-shot invalid-layout fixture exercised the actual inference-failure path on both loaders. It reopened the original native Nether Chest screen rather than showing an incomplete generic grid or disconnecting.
- GUI-scale changes were exercised with a hosted backpack. The host reinitialized its native screen and retained its contents.

## Corrections verified during implementation

Earlier passes exposed missing inferred storage controls, duplicated native chrome/lower labels, incorrect absolute clipping, an animated preview's nested clipping coordinates, and a backpack sorting rollback caused by its native backing-item save callback. These were corrected and their affected paths rerun. Backpack sorting and persistence have explicit passing assertions in the final Fabric check.

Some early harness commands targeted hidden native player slots rather than SIU's separate player window. Those assertions were repeated through the visible player inventory or physical hotbar. One driver timeout was resumed after restoring the active gameplay client. Earlier files are retained as development evidence; they are not described as passing tests.

## Build and automated checks

Executed successfully:

```powershell
.\gradlew.bat :1.20.1:fabric:build :1.20.1:forge:build :compatibility:1.20.1:fabric:build :compatibility:1.20.1:forge:build
.\gradlew.bat :common:test --tests '*AutomaticContainerLayoutTest' --tests '*DesktopConnectionStateTest'
python tools/verify-module-boundaries.py --sources
git diff --check
```

The selected common tests passed: **6 geometry tests + 6 connection-state tests**. The affected module builds also ran their normal checks, including the existing Compatibility layout and native-symbol checks. No complete cross-version gameplay suite was run.

The local runtime task is named `runFullGameplayQa` and prints that name in its log, but its excluded manual driver runs only the focused commands recorded here. Its name is not a claim of full functional coverage.

## Production artifacts

Updated the following production outputs and their distribution checksums:

- `dist/core/salts_inventory_update-1.20.1-Fabric-0.1.5.jar`
- `dist/core/salts_inventory_update-1.20.1-Forge-0.1.5.jar`
- `dist/compatibility/salts_inventory_compat-1.20.1-Fabric-0.1.3.jar`
- `dist/compatibility/salts_inventory_compat-1.20.1-Forge-0.1.3.jar`

The Compatibility builds include the preceding Iron Chests and Feeding Trough work already present in this workspace. The six automatic targets use Core's generic host; they do not add six new extension registrations. Only the affected 1.20.1 distributions were refreshed. Other Minecraft targets and Controlify JARs were not rebuilt in this task.

Detailed local evidence is retained under `build/native-container-autohost/`: `artifact-audit.json`, `release-build.log`, Fabric `a2`–`a5` and Forge `a1`–`a3` gameplay observations, screenshots and focused assertion records. These are local build artifacts and are removed by a clean build. The final regression records are Fabric `a5` (backpack sorting/API priority/fallback) and Forge `a3` (Core-only native hosting and visual controls); the wider target coverage is recorded in the preceding focused passes.

## Coverage limits

Dedicated remote multiplayer, Controlify combinations, every mod configuration/resource pack/language, every backpack tier and every GUI scale were not exhaustively gameplay-tested. Native panel layouts are retained rather than universally reflowed. Tabbed, interleaved, virtual or otherwise unconventional UIs and custom packets that assume an active native `player.containerMenu` may need a dedicated API adapter. See [the architecture guide](automatic-native-containers-1.20.1.md) for the supported shape and fallback behavior.
