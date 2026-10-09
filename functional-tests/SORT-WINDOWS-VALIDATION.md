# Sort into open containers validation

The 26.2 feature is ported to Minecraft 1.20.1, 1.21.1, 1.21.11, 26.1.2, and 26.3. Fabric is supported on all six versions, Forge on 1.20.1, and NeoForge on the five versions from 1.21.1 through 26.3.

## Automated checks

Each version has headless tests calling its production sorting implementation with Minecraft inventories, slots, and item stacks. Coverage includes the five-window example, exact and tag routing, focused-before-empty fallback, partial/full destinations, no destination, closed destinations, duplicate physical slots, snapshot stability, extraction callbacks, large storage, processor slot selection, main-inventory exclusions, stack metadata preservation, and duplicate network destination rejection.

Run from the repository root:

```powershell
./gradlew.bat -I functional-tests/sort-tests.gradle :1.20.1:fabric:test :1.21.1:fabric:test :1.21.11:fabric:test :26.1.2:fabric:test :26.2:fabric:test :26.3:fabric:test --console=plain
./functional-tests/scripts/Test-SourceFeatureParity.ps1
```

The source parity checks cover capability negotiation, packet registration, transfer callbacks, slot policies, dynamic Shift tooltips, atlas columns, identical textures, and Sophisticated storage registration on each Forge/NeoForge target.

Build all twelve targets. Set `CI=true` for the current shell so 26.3 NeoForge uses ModDevGradle's binary patch pipeline:

```powershell
$env:CI = 'true'
./gradlew.bat -I functional-tests/sort-tests.gradle :1.20.1:fabric:build :1.20.1:forge:build :1.21.1:fabric:build :1.21.1:neoforge:build :1.21.11:fabric:build :1.21.11:neoforge:build :26.1.2:fabric:build :26.1.2:neoforge:build :26.2:fabric:build :26.2:neoforge:build :26.3:fabric:build :26.3:neoforge:build --console=plain --continue
```

Tests run on each version's headless Fabric Minecraft runtime. Forge/NeoForge compile and package the shared implementation and their compatibility adapters; the headless tests do not launch those loaders.

## Drag placement regression

The same test harness also checks left, right, and creative-clone drag placement against Minecraft's preview calculation on all six versions. With 32 items and three slots, left drag must place 10 per slot, right drag must place one, and creative clone must place 64. Both the ordinary-slot calculation and the default registered gesture policy are checked.

These tests reproduced a reversed left/right calculation on 1.20.1, 1.21.1, and 1.21.11 before the fix. Their fallback helper had swapped the meanings of Minecraft's `CHARITABLE` (even distribution) and `GREEDY` (one per slot) constants. All three 26.x versions already delegate to Minecraft and passed unchanged. Sophisticated's native placement calculation also has the correct mapping; the fix concerns ordinary/player slots, including drags between those slots and Sophisticated windows.

## In-game scope

The original 26.2 work included NeoForge testing with Sophisticated Storage, normal transfers of sand and bones, and hotbar exclusion. The 26.3 Fabric port also loaded an integrated world and displayed the sort control with the correct normal and Shift tooltips. The 26.3 NeoForge client opened an integrated world and its inventory, displayed the normal sort tooltip, and correctly split a stack with right-click. Full live multi-container behavior on 26.3 remains a manual check; automated regression covers its routing and slot rules.

## Editable control texture

Each version's `common/src/main/resources/assets/salts_inventory_update/textures/gui/window_controls.png` is 132 by 33 pixels. Cells are 11 by 11 pixels. Normal sorting occupies zero-based column 10 (x=110); Shift sorting occupies column 11 (x=121). Rows retain normal, hovered, and pressed states. All six copies currently match.

The tooltips are exactly `Sort into open containers` and, while Shift is held, `Force sort into open containers`.
