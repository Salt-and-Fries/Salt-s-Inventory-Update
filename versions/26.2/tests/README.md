# 26.2 sort-into-windows validation

Run the version-local regression suite and both loader builds from the repository root:

```powershell
./gradlew.bat -I versions/26.2/sort-tests.gradle :26.2:fabric:test :26.2:fabric:build :26.2:neoforge:build
```

The 16 headless tests execute the production server routing and transfer callbacks using real Minecraft stacks and slots. They cover the five-window example, exact matching, tag matching, partial/full storage, focus precedence, fixed destination snapshots, callback-introduced source changes, component preservation, duplicate physical slots, closed destinations, large storage, output callbacks, processor slot roles, excluded hotbar/equipment slots, and malformed requests. The fixtures initialize only the stack components they need and allocate a minimal player without starting a world.

Headless tests run against Fabric's Minecraft runtime. NeoForge's patched runtime requires FML startup and is validated with its full client instead of plain JUnit.

## In-game fixture

Use a newly created disposable world with commands enabled. Copy `qa-pack` into that world's `datapacks` directory, run `/reload`, then `/function siu_sort_qa:setup`. This replaces blocks in the test area at Y=200–204 and fills five main inventory slots plus one hotbar slot. Do not run it in a valued world.

`view1` through `view4` orient the player toward each container. Open containers with right-click, hide the desktop with Escape while changing view, and press E to open the main inventory in survival mode. In creative mode, use the player-inventory tab.

Normal click should route only matching items. Shift-click should continue through tags, focused storage, then initially empty storage. Test with more than one matching chest, full chests, and a non-storage window focused. Verify minimized/hidden windows are excluded.

For processing containers, confirm only furnace/smoker/blast-furnace result slots and the three brewing bottle slots can move. Check brewing during an active brew. For Sophisticated Storage, include upgrade slots and ensure only the storage grid moves.

## Recorded validation

- 16 regression tests passed on the 26.2 Fabric runtime.
- 26.2 Fabric and NeoForge builds passed.
- Full NeoForge client with Sophisticated Storage: title-bar button rendered on vanilla chests, player inventory, and Sophisticated barrels.
- Vanilla chest: seven sand transferred on normal click; unmatched main-inventory items stayed in place.
- Sophisticated barrel seeded with one bone: four additional bones transferred on normal click; the separate ten-bone hotbar stack stayed untouched.
- Both live sort requests received successful server acknowledgments, without rollback or timeout.
- Visual QA found an overlong tooltip; it was changed to wrap the title and usage instructions, then rechecked successfully in the rebuilt NeoForge client.

The full live five-container, held-Shift, and machine-experience scenarios remain manual checks; the automated suite covers their routing/slot rules and extraction callback counts.
