# Window-hosted hotbar HUD visibility

Core 0.1.5, checked on 2026-10-21. These are focused checks of the **Return Hotbar to Inventory** option, not a complete gameplay suite.

When Core is enabled, this option is enabled, and an interactive desktop window is open, the native hotbar and its health, armor, food, air, experience, mount and selected-item decorations are suppressed together. Closing the windows or disabling the option restores normal HUD rendering. The inventory window continues to display its hotbar slots.

The original checks below predate the subsequent movement-control exception: activating Window Movement Control now temporarily restores the HUD and permits hotbar selection. Current coverage is recorded in [HOTBAR-MOVEMENT.md](HOTBAR-MOVEMENT.md).

Vanilla uses the version's hotbar/decorations rendering or extraction methods; 1.20.1 has separate decoration methods. Forge overlays and NeoForge layers also receive their own cancellation hooks because their status indicators render independently. These hooks reuse Core's existing hotbar visibility predicate. The visibility mixins run before scoped HUD decorators, and native loader events allow canceled-overlay cleanup to run normally.

## Executed checks

- All twelve Core targets compiled and assembled successfully.
- `./gradlew.bat writeModuleChecksums -x test --console=plain` passed, including production packaging, module boundaries, API compile-classpath checks, integration coverage checks and six existing coverage-audit regression tests. The general JUnit and functional suites were not run.
- All twelve production Core JARs contain the new visibility injection and updated option tooltip. Their distribution checksums were verified again after gameplay.
- Two disposable-world gameplay profiles passed with Core 0.1.5, Compatibility 0.1.2 and Raised 6.0.2 installed. Each produced 19 passing assertion/stage records, five framebuffer captures and zero failures.

| Profile | Core runtime | Result |
| --- | --- | --- |
| Minecraft 26.3 Fabric | Production JAR copied from `dist/core`, with its hash logged by the packaged launcher | PASS |
| Minecraft 1.20.1 Forge | Development outputs through the native loader launcher | PASS |

Both profiles checked the following sequence:

1. Windows open, option disabled: hotbar and indicators visible.
2. Enable the option and refresh the inventory layout: indicators hidden, window hotbar visible.
3. Disable the option: normal HUD restored immediately.
4. Enable the option and close all windows: normal HUD visible.
5. Reopen the inventory window with the option still enabled: indicators hidden again.

Framebuffer analysis confirmed health and experience pixels present in all six expected visible frames and absent in all four expected hidden frames. Representative captures were also visually inspected for armor, hunger, inventory slots and restored HUD placement.

## Reproduction and evidence

After building distribution JARs:

```powershell
.\functional-tests\scripts\Run-RaisedHudQa.ps1 -Minecraft 26.3 -Loader fabric -Mode raised -HudVisibilityOnly -RunId hud-visibility-modern-r1
.\functional-tests\scripts\Run-RaisedHudQa.ps1 -Minecraft 1.20.1 -Loader forge -Mode raised -HudVisibilityOnly -RunId hud-visibility-legacy-r1
```

- Modern report and five screenshots: `build/gameplay-qa/packaged-extension-empty/26.3-fabric-hud-visibility-modern-r1/`
- Legacy report and five screenshots: `build/raised-qa/runtime/1.20.1-forge-hud-visibility-legacy-r1/`
- Pixel counts and framebuffer hashes: `build/hud-visibility-qa/framebuffer-checks.json`
- Build logs: `build/hud-visibility-build.log` and `build/hud-visibility-distribution.log`
- Release JARs and hashes: `dist/core/` and `dist/core/SHA256SUMS.txt`

## Coverage limits

The other ten targets were compiled and packaged, but were not launched for this focused change. Mounted jump/health bars, underwater air bubbles, selected-item labels and spectator presentation were not exercised in these two gameplay profiles. Forge gameplay used development outputs; its distributable JAR was built and inspected separately. A new Core-only gameplay run and the full functional suite were not performed.
