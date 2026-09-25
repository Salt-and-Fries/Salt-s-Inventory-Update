# Native container lifecycle validation

Date: 2026-09-08

Related report: https://github.com/Salt-and-Fries/Salt-s-Inventory-Update/issues/7

## Changes

- Extend native chest opener ownership to barrels and ender chests. A detached Salt menu must count during native opener rechecks even though it is not `player.containerMenu`.
- Bind each detached ender-chest menu to its physical block for ownership, validity, and exactly-once close. The shared ender inventory must not redirect an older window's close to the most recently opened block.
- Preserve native start/stop methods, sound generation, and animations instead of manually playing sounds. Native/disabled menu paths remain native.
- Apply to shared sources and mixin registrations for 1.20.1, 1.21.1, 1.21.11, 26.1.2, and 26.2, covering both loaders for each version. Newer stop methods use ContainerUser; older versions use Player.

## Completed checks

- `gradlew.bat verifyReleaseBundle sourceFeatureParity --no-daemon --console=plain`: successful; all ten release targets compiled and packaging verified.
- Reran `sourceFeatureParity` after adding assertions for all three opener classes, per-menu ender ownership, disabled gating, and mixin registration: passed.
- In-game Fabric 1.21.1 with Salt enabled:
  - Barrel opens, remains visibly open beyond native rechecks, closes, and reopens with the expected opening and closing sound events.
  - Ender chest opens, stays open, and closes with the expected lid animation and sound events.
  - Two different physical ender chests open simultaneously. Closing the first closes only its physical chest; the second remains open until its own window closes. Shared items were not modified.
  - Trapped chest opens, remains open across rechecks, and closes with sound events.
  - Shulker box opens and closes with its native animation and sound events.
- Restarted Fabric 1.21.1 with `enableMod: false`: native barrel UI opens, closes, and reopens with sound events.

Sound events were observed through Minecraft subtitles, not audio waveform capture. Computer-use testing inspected actual block animations and menus. No claim is made that the original reported released binary was reproduced; the current build passes the reported disabled/enabled scenarios.

## Remaining coverage

Interactive runtime testing in this pass was limited to Fabric 1.21.1. Other loader/version targets passed compilation, packaging, and source assertions, but have not all been interactively retested for this change. Multiplayer opener counts, double chests, trapped-chest redstone output, disconnect/chunk-unload timing, and arbitrary third-party container implementations remain additional runtime cases. Existing Sophisticated-specific ownership integration was preserved, not exhaustively retested here.

## Test environment

Used the existing development world only after copying it to `functional-tests/results/backup-barrel-1.21.1-fabric-20260908`. Fixtures at (160,71,-51) and (159,71,-51) remain in the test world; its original state is recoverable from that backup. Restored Salt enablement and the original subtitles-off setting after testing and closed the test client.
