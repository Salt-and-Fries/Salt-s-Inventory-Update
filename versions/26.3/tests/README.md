# Minecraft 26.3 validation

Run the version-local sorting regression suite and build from the repository root:

```powershell
$env:CI = 'true'
./gradlew.bat -I versions/26.3/sort-tests.gradle :26.3:fabric:test :26.3:fabric:build :26.3:neoforge:build
```

The headless Fabric tests cover exact and tag routing, focus and empty-window fallback, partial and full destinations, duplicate storage views, output callbacks, excluded player slots, and malformed requests. The NeoForge target compiles and packages the shared implementation. Its build uses ModDevGradle's binary patch pipeline because the published NeoForm source currently fails to recompile `HolderSet.emptyNamed`.

For in-game QA, copy `qa-pack` into a disposable world's `datapacks` directory, run `/reload`, then `/function siu_sort_qa:setup`. This replaces blocks near Y=200. Open the containers and player inventory in separate Salt windows; check normal and Shift sorting, then confirm the hotbar and processing inputs stay untouched.

Recorded: 19 sorting regression tests, the SDL-to-container mouse button regression test, and the Fabric build passed. With Java 25.0.3, the client loaded an integrated world, opened the Salt inventory window, and displayed the sort control with the correct normal and Shift tooltips. The optional JEI 31.7.0.37 runtime also opened the recipe browser and rendered fluid and item tabs. Full live multi-container routing remains a manual check; the headless suite covers its transfer rules.

The SDL button fix was also checked in a disposable creative world on Java 26: left-click picked up a full stack of 16, right-click split it into two stacks of 8, right-drag placed one item per slot, and left-drag distributed the remainder evenly. The test world saved and the client exited normally.

NeoForge 26.3.0.22-beta was smoke-tested in a disposable integrated world. The first-time help window opened, the survival inventory window rendered with the sorting control and its normal tooltip, and 16 dirt moved from the hotbar into the main inventory. Right-click split the stack into two stacks of 8. The world saved, the client exited normally, and `runClient` completed successfully.
