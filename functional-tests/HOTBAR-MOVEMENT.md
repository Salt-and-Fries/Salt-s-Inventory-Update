# Window Movement Control and the window-hosted hotbar

Core 0.1.5, checked on 2026-10-21. These checks cover the affected HUD and hotbar input behavior; they are not a complete gameplay suite.

With **Return Hotbar to Inventory** enabled and interactive desktop windows open:

- Cursor control hides the native hotbar and its status indicators. Wheel and hotbar-key selection cannot change the selected slot.
- Window Movement Control temporarily restores the native HUD and permits selection. Number keys target the player's selected slot even when the grabbed cursor lies over an inventory slot.
- Returning to cursor control locks the newly selected slot, without reverting it to the earlier selection.
- The behavior uses the existing movement-control state, including its configured binding and Hold/Toggle preference. Window scrolling, inventory slot operations and text input retain their existing routes.
- Disabling the option or closing the windows restores normal selection behavior.

The same predicate controls HUD suppression and the final hotbar-selection method. No new public API or saved setting is required.

## Builds and packaging

`./gradlew.bat writeModuleChecksums -x test --console=plain` passed after the loader fix. All twelve Core production JARs were regenerated in `dist/core/`, with matching `SHA256SUMS.txt`. The root packaging checks also passed module boundaries, API compile-classpath isolation, integration coverage and six existing coverage-audit regression tests. The general JUnit and functional suites were excluded.

Package inspection verified the Fabric-only HUD mixin configuration and class in all six Fabric JARs, registered by their own Fabric metadata. Neither appears in the six Forge/NeoForge JARs. The shared native mixins no longer contain the grouped decoration injection. Native loaders use their existing overlay/layer events.

## Focused tests

The three existing `MovementControlStateTest` unit tests passed: Hold follows the supplied held-key state, Toggle latches until the next press, and leaving the workspace or changing mode clears Toggle.

| Final gameplay profile | Core runtime | Result |
| --- | --- | --- |
| Minecraft 1.20.1 Fabric, Core alone | Production JAR copied from `dist/core`, with its hash recorded by the packaged launcher | PASS: 68 passing assertion/stage records, nine captures, zero failures |
| Minecraft 26.3 NeoForge, Core + Compatibility + Raised + Mouse Tweaks | Native development outputs | PASS: 68 passing assertion/stage records, nine captures, zero failures |

Framebuffer analysis confirmed health and experience pixels present in the ten expected visible frames and absent in the eight expected hidden frames. Representative captures were visually inspected. All twelve distribution checksums were verified again after gameplay. The two final profiles contain 136 passing assertion/stage records and 18 captures; these counts include stage-completion records, rather than 136 unique test cases.

The focused profile exercises:

1. HUD visibility with the option off, on, switched back, windows closed, and windows reopened.
2. Both wheel directions and all nine hotbar keys while the HUD is hidden.
3. Default Alt Toggle activation, visible HUD, wheel selection, and number-key selection.
4. A grabbed cursor placed over a real window slot: a number key selects the hotbar slot and preserves both item stacks.
5. Toggle release: the HUD hides and the current selection stays locked.
6. Movement binding changed to P: Alt is ignored, P activates movement control, and hotbar selection works.
7. Switching to Hold with no held key: the HUD hides and selection locks again.
8. Disabling Return Hotbar to Inventory: normal wheel and number-key selection resumes.
9. Closing and reopening the workspace: Toggle clears and hidden selection is locked.
10. Authoritative server selection after blocked input, enabled selection, mode changes and option changes.

These gameplay checks dispatch keyboard events through the native keyboard handler and wheel events through the native mouse handler. Pointer coordinates are controlled by the QA fixture to exercise window occlusion. No OS-level physical key hold was synthesized; Hold activation/release is covered by the state unit tests, and gameplay checks cover the mode transition and unheld state.

## Reproduction and evidence

```powershell
.\gradlew.bat :common:test --tests '*MovementControlStateTest'
.\gradlew.bat writeModuleChecksums -x test --console=plain
.\functional-tests\scripts\Run-RaisedHudQa.ps1 -Minecraft 1.20.1 -Loader fabric -Mode core -HudMovementOnly -RunId hud-movement-core-r2
.\functional-tests\scripts\Run-RaisedHudQa.ps1 -Minecraft 26.3 -Loader neoforge -Mode raised -MouseTweaks -HudMovementOnly -RunId hud-movement-raised-r2
```

- Production Fabric report, hashes and nine screenshots: `build/gameplay-qa/packaged-core/1.20.1-fabric-hud-movement-core-r2/`
- Native NeoForge report and screenshots: `build/raised-qa/runtime/26.3-neoforge-hud-movement-raised-r2/`
- Unit output: `versions/common/build/test-results/test/TEST-com.salts_inventory_update.client.MovementControlStateTest.xml`
- Final distribution build: `build/hud-movement-distribution-fixed.log`
- Required input assertions, framebuffer pixel counts and hashes, runtime mod lists, and final Core artifact hashes: `build/hud-movement-qa/verified-results.json`

An initial NeoForge launch failed before world creation because NeoForge removes the vanilla grouped HUD method introduced into the shared mixin by the preceding visibility change. The fix moves that injection into a required Fabric-only mixin configuration and excludes its source from native-loader compilation. The original failed launch remains in `build/raised-qa/runtime/26.3-neoforge-hud-movement-raised-r1/`. The final runs use the corrected loader separation. An earlier build also caught the legacy key-event signature difference; the 1.20.1 and 1.21.1 variants use their existing two-argument matching form.

## Coverage limits

The other ten Minecraft/loader targets were compiled and packaged, but were not launched for this change. The NeoForge profile uses development outputs; its production JAR is separately built and inspected. Physical OS Hold input, arbitrary third-party HUDs, mounted/underwater indicators and the complete gameplay suite were not exercised here.
