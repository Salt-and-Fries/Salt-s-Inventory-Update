# Core 0.1.5 Key Binds follow-up

This follow-up updates Controls placement and confirms that inventory hold-to-close follows the vanilla Inventory binding. It supersedes the menu-placement details in [WINDOW-CONTROLS-VALIDATION.md](WINDOW-CONTROLS-VALIDATION.md).

## Implementation

- Minecraft Controls keeps the shared Hold/Toggle selector and removes the extra Window Movement Key button. Salt's Inventory settings retain their Key Binds shortcut.
- The normal Key Binds list has a **Salt's Inventory Update** section containing Character Window, Recipe Browser Window and Window Movement Control. Binding identifiers and defaults remain unchanged, preserving the existing settings keys. The vanilla Inventory binding remains in its own vanilla section.
- Forge and NeoForge create the Core bindings during `RegisterKeyMappingsEvent`, before the loader collects them into Minecraft Options. Client setup reuses those same instances. A focused Forge run initially exposed four failures: the three bindings and their category were absent from the actual native list. All four pass after the registration fix.
- Keyboard hold handling already matched `Options.keyInventory`; it was not hardcoded to E. Mouse bindings now enter the same hold controller through the native mouse callback. Short taps open inventory, holds close the workspace, and releasing after a completed hold does not reopen it.
- Hold-to-close help resolves the selected vanilla binding dynamically. The duration label and description say Inventory rather than E. Existing saved duration fields and Persistent Windows behavior are preserved.

## Validation

Validated on 2026-10-21 using isolated production-JAR launches, disposable integrated-server worlds and native settings/input callbacks.

| Focused profile | Result |
| --- | --- |
| 1.20.1 Forge | Passed: 23 assertions, 0 failures |
| 26.3 Fabric | Passed: 23 assertions, 0 failures |
| 26.3 NeoForge | Passed: 23 assertions, 0 failures |

Each profile checks the selector remains usable, the extra Controls button is absent, all three mappings are in Minecraft Options, and the native Key Binds list contains the translated mod category. It then remaps vanilla Inventory to I, verifies E no longer closes the windows, holds I to close them, and confirms release leaves them closed. The same close/release checks run with a mouse binding, followed by a short mouse tap opening inventory. Hold-help labels are checked against both remapped bindings.

All twelve Core production targets compiled and packaged successfully. The distribution audit verifies the new category translations and handlers, the absence of the removed shortcut method, matching checksums and release copies, and all 36 JARs inside the combined ZIP. Exported API entries remain byte-identical to the previous Core JARs; all 24 Compatibility and Controlify JARs remain byte-identical.

Evidence is stored under `build/keybinds-followup/`: `package-final.log`, `artifact-audit.json`, per-loader logs and `gameplay/<minecraft>-<loader>-keybinds-fixed/`. Each gameplay folder contains `results.json`, shipping archive hashes and two native framebuffer images. Forge also records the original production archive and its required SRG-to-named QA transformation.

Run this focused profile after refreshing `dist/core`, with a fresh run ID:

```powershell
.\gradlew.bat -I functional-tests/chest-close-qa.init.gradle :1.20.1:forge:runWindowControlsQa "-PqaMinecraft=1.20.1" "-PqaLoader=forge" "-PqaRunId=keybinds-new-run" "-PqaKeybindsOnly=true" "-PincludeFunctionalTests=false"
```

Select `:26.3:fabric:runWindowControlsQa` or `:26.3:neoforge:runWindowControlsQa` and the corresponding properties for modern targets.

These are focused controls checks, not a full gameplay suite. Other version/loader combinations received compilation and artifact checks. Physical OS input, unusual modifier combinations, custom-binding persistence across a complete game restart, real controller hardware, dedicated servers and optional integrations were not newly tested. Persistent Windows behavior was preserved by reusing its existing close path, rather than changing the hide/restore policy.
