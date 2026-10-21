# Core 0.1.5 recipe book and movement controls

Validated on 2026-10-21. This is focused feature validation, not a full gameplay or integration suite.

The subsequent [Key Binds follow-up](KEYBINDS-VALIDATION.md) removes the extra Controls shortcut, introduces the dedicated mod category and extends inventory-hold handling to mouse bindings. Menu-placement statements below describe the earlier validation snapshot.

## Changes

- Recipe-book buttons use the state displayed before lazy native initialization. Reopening a window while the player's saved recipe-book preference is open no longer makes the first click close an unseen book.
- Window Movement Control retains its existing binding identifier and defaults to Left Alt, preserving saved remappings. Minecraft Key Binds supports rebinding it; both settings screens provide a shortcut there.
- Salt's Inventory settings and Minecraft Controls share a saved Hold/Toggle preference. Existing configurations default to Hold. Keyboard repeat does not repeatedly toggle, and quick press/release pairs are retained between ticks.
- Toggle clears on leaving the desktop, opening another menu, losing game focus, entering text or disabling the mod. A registered but inactive alternative workspace leaves normal window controls available. An active alternative presentation continues to own its input.
- All six shared version source trees and all twelve Core loader targets contain the changes. Core remains 0.1.5; public API 6 is unchanged.

## Executed checks

| Check | Result |
| --- | --- |
| Production compilation and packaging for all twelve Core targets | Passed |
| `:common:test --tests com.salts_inventory_update.client.MovementControlStateTest` | 3 tests passed |
| Packaged Core gameplay, 1.20.1 Forge | Passed, 0 failures; 92 PASS records and 10 framebuffer capture records |
| Packaged Core gameplay, 26.3 Fabric | Passed, 0 failures; 92 PASS records and 10 framebuffer capture records |
| Distribution metadata/resources, API bytes, checksums and combined ZIP | Passed |
| `git diff --check` | Passed |

The gameplay checks use rendered Minecraft clients, disposable integrated-server worlds, native recipe-book button callbacks, native keyboard/mouse handler callbacks, and native settings button callbacks. They cover character, crafting-table and furnace recipe books: open, close while open, recreate the window, open with one click, close with the next click, and open again. They also cover both settings menus, saving/reloading the shared preference, Key Binds navigation, default Alt, a rebound keyboard key, a mouse binding, short taps, ignored repeats, a second tap restoring cursor control, closing/reopening the workspace, and returning from another menu. The movement checks include a registered but inactive public workspace-host fixture.

Framebuffer captures of the two settings menus were visually inspected on both targets. The legacy and modern reopened crafting-book views were also inspected. The native Forge QA launcher transforms the original production archive using Forge's SRG-to-named mappings and records its original and transformed hashes; the Fabric launcher loads the shipping archive directly.

Earlier development runs exposed harness assumptions about retained desktop singletons, fixture auto-closing and the modern native button signature. Those harness issues were corrected before the final runs recorded above.

## Artifacts and evidence

- `dist/core/`: twelve updated production Core 0.1.5 JARs and `SHA256SUMS.txt`.
- `build/release/0.1.5/`: matching Core archives and checksums.
- `dist/salts-inventory-all-latest.zip`: refreshed; all 36 bundled archives match their distribution files.
- `build/window-controls/artifact-audit.json`: verifies all twelve Core archives changed, their exported API entries remain byte-identical to the previous artifacts, and all 24 Compatibility/Controlify archives remain byte-identical.
- `build/window-controls/package-final.log`: final production packaging result.
- `versions/common/build/test-results/test/TEST-com.salts_inventory_update.client.MovementControlStateTest.xml`: focused unit result.
- `build/window-controls/gameplay/1.20.1-forge-window-controls-verified/`: final legacy results, screenshots, shipping hash and Forge mapping lineage.
- `build/window-controls/gameplay/26.3-fabric-window-controls-verified/`: final modern results, screenshots and shipping hash.

Reproduce one focused profile from the root, after refreshing `dist/core`:

```powershell
.\gradlew.bat -I functional-tests/chest-close-qa.init.gradle :1.20.1:forge:runWindowControlsQa "-PqaMinecraft=1.20.1" "-PqaLoader=forge" "-PqaRunId=window-controls-new-run" "-PqaWindowControlsOnly=true" "-PincludeFunctionalTests=false"
```

Use a fresh run ID to preserve earlier results. For modern input, select `:26.3:fabric:runWindowControlsQa`, `qaMinecraft=26.3` and `qaLoader=fabric`.

## Coverage limits

Gameplay was run on the two targets listed above, not every version/loader combination. All twelve targets compiled, packaged and passed the artifact audit. Hold-mode state transitions were tested directly; physical OS keyboard/mouse polling, unusual modifier combinations, dedicated servers, real controller hardware, every GUI scale, resource packs and the full optional-integration matrix were not newly exercised. Focus/text-entry reset paths were inspected and share the tested eligibility reset; they were not separately driven through physical input. The complete root `build` test graph was deliberately not dispatched for this change, following the requested limit on testing.
