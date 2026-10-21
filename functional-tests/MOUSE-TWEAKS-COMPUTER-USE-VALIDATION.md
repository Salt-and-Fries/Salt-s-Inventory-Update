# Mouse Tweaks: Windows Computer Use validation

Date: 2026-10-21. Scope: Mouse Tweaks gestures in Core 0.1.5. This was focused gameplay validation, not the complete functional suite.

## Result and fix

Real mouse input reproduced a bug on Minecraft 1.20.1 Forge. With one crafting-table recipe remaining and an occupied player inventory, a three-tick wheel burst crafted the table but then picked up an unrelated stack of 20 stone. The items were conserved, but the cursor and inventory were wrong.

Core now ends the remaining clicks in that gesture when a crafting output is empty and nothing was picked up. The guard resets for the next gesture. It was applied to all six shared version source trees, covering all twelve loader targets. No public API signatures, protocol version, or companion code changed.

The corrected crafting burst passed in five real clients. Each produced exactly one crafting table, retained the unrelated occupied player stack, and left the cursor empty. A subsequent drag collected 16 stone from a native barrel and 16 from a public-API window, proving that the guard did not block later gestures.

## Method

The `computer-use:computer-use` skill and `@oai/sky` controlled Minecraft through Windows mouse and keyboard input. Each action was followed by an observed screenshot. The test fixture created a disposable integrated-server world, seeded inventories, opened windows, and wrote read-only server snapshots. It did not invoke Mouse Tweaks callbacks during the manual profile.

Five windows were open together: a native chest, native barrel, custom public-API chest, crafting table, and player inventory. The API chest included a virtual slot that must not participate in inventory gestures. Compatibility and Controlify were absent; the actual upstream Mouse Tweaks release and production Core artifact were loaded. Forge's launcher records the production-to-development remapping lineage.

Saved checkpoints include player inventory, shared cursor, and all three physical containers. `tools/verify-mouse-tweaks-computer-use.py` audited 54 checkpoints across seven playthroughs for conserved stone, dirt and iron, stack limits, untouched virtual-slot contents, selected gesture outcomes, the original failure, and the five corrected crafting regressions. It also matched all five final runtime inputs to the current `dist/core` hashes. The audit output is `build/mouse-tweaks/computer-use-audit.json`.

Fixture `results.json` records are setup and lifecycle checks. They are not a count of manually tested gestures and do not by themselves establish that gameplay passed.

## Runtime coverage

| Minecraft / loader | Mouse Tweaks | Broad playthrough before fix | Retest with final Core |
| --- | --- | --- | --- |
| 1.20.1 Forge | 2.25.1 | 15 checkpoints; crafting failure reproduced | Crafting exhaustion, native/API collection, placement, ordinary wheel: pass |
| 1.20.1 Fabric | 2.26 | — | Crafting exhaustion, native/API collection, right-click split and return: pass |
| 1.21.11 Fabric | 2.30 | — | Crafting exhaustion, native/API collection, placement, API wheel: pass |
| 26.3 Fabric | 2.31 | 23 checkpoints | Crafting exhaustion, native/API collection, placement, ordinary wheel: pass |
| 26.3 NeoForge | 2.31 | — | Ordinary wheel, crafting exhaustion, native/API collection and placement: pass |

The broad playthroughs exercised these cases with multiple windows open:

| Behavior | Observed result |
| --- | --- |
| Left-drag collection across native windows | Matching items collected into one shared cursor stack |
| Left-drag collection between native and API windows | Matching items collected across both inventories |
| Distribution of a carried stack across two windows | Stack split between both destinations; empty cursor afterward |
| Drag past a different item type | Different item remained unchanged |
| Right-click split, one-item placement, final carried item | Correct halves/single placements; last item placed without stale cursor |
| Double-click collection across windows | Collection stopped at the 64-item stack limit |
| Merge into a full stack and wheel toward a full stack | No overflow; remaining items conserved |
| Native and API wheel transfers in both directions | Expected per-notch transfers, with server counts checked |
| Player-slot wheel transfers and a 100-notch source exhaustion burst | Source drained without overdraw; additional scrolling was a no-op |
| Empty slot and virtual API slot | No unintended transfer; virtual dirt remained at 10 |
| Player inventory closed while other windows remain open | Wheel transfer continued; reopening showed synchronized inventory |
| An empty player window covering a populated API slot | Covered slot did not receive wheel input |
| Window options popup | Wheel input was blocked while open and resumed after closing |
| Moved windows and resized native chest | Input followed the resulting slot layout |
| Crafting output with fewer recipes than wheel ticks | Original Forge failure fixed and retested on five targets |

The API fixture declares a fixed size; trying to resize it correctly left it unchanged. That is not evidence of resizable API-window coverage.

## Evidence and reproduction

All evidence folders below are under `build/mouse-tweaks/gameplay/`:

- Broad modern run: `26.3-fabric-computer-use-01/`.
- Broad legacy run and original bug: `1.20.1-forge-computer-use-02/`, especially `crafting-wheel.json`.
- Final regressions: `<minecraft>-<loader>-computer-use-craft-r2/` for the five targets in the table, especially `crafting-wheel-fixed.json`, `post-fix-native-api-drag.json`, and the final snapshot.

The broad runs intentionally retain their pre-fix artifact provenance. They were not relabeled as complete retests of the final code. The final regression directories contain `staged-artifacts.json`, `manual-ready.json`, server checkpoints, and normal completion reports.

In-game F2 captures include:

- Original failure: `versions/1.20.1/forge/build/runtime-smoke/window-controls-computer-use-02/screenshots/2026-10-21_12.10.20.png`.
- Fixed Forge run: `versions/1.20.1/forge/build/runtime-smoke/window-controls-computer-use-craft-r2/screenshots/2026-10-21_12.17.19.png`.
- Fixed NeoForge run: `versions/26.3/neoforge/build/runtime-smoke/window-controls-computer-use-craft-r2/screenshots/2026-10-21_12.27.15.png`.
- Fixed 1.21.11 Fabric run: `versions/1.21.11/fabric/build/runtime-smoke/window-controls-computer-use-craft-r2/screenshots/2026-10-21_12.28.58.png`.

To launch the interactive fixture, use a fresh run ID and change the target as needed:

```powershell
$qaArgs = @(
  '-I', 'functional-tests/chest-close-qa.init.gradle',
  '-PqaMouseTweaksOnly=true', '-PqaManualMouseTweaks=true',
  '-PqaManualCraftingReady=true',
  '-PqaMinecraft=26.3', '-PqaLoader=fabric', '-PqaRunId=my-manual-run',
  ':26.3:fabric:runMouseTweaksQa', '--console=plain'
)
& .\gradlew.bat @qaArgs
```

After `manual-ready.json` appears, perform the gestures through Computer Use, observe the screenshots, and save settled `manual-state.json` checkpoints. Write `manual-finish.request` in the evidence folder to end the disposable client normally. Omit `qaManualCraftingReady` for the broader initial inventory layout. The manual task has a 120-minute limit.

Discarded setup attempts remain separate: the first Forge fixture placed crafting out of reach, and the first `computer-use-craft-fixed` fixtures seeded the wrong captured menu. Neither is counted as passing gameplay evidence. Both fixture problems were corrected before the final runs.

## Builds and artifact checks

These commands completed successfully after the production fix:

```powershell
.\gradlew.bat -I functional-tests/core-artifacts-qa.init.gradle stageCoreQaArtifacts --console=plain
.\gradlew.bat -I functional-tests/core-artifacts-qa.init.gradle bundleLatestMods --console=plain
python tools/verify-mouse-tweaks-computer-use.py
python tools/verify-mouse-tweaks-artifacts.py --baseline build/mouse-tweaks/before/artifacts.json
python tools/verify-module-boundaries.py --sources --jars
git -c core.safecrlf=false diff --check
```

All twelve Core 0.1.5 production JARs were rebuilt in `dist/core/` and copied to `build/release/0.1.5/`. `dist/salts-inventory-all-latest.zip` contains the updated twelve Core JARs plus the existing twelve Compatibility 0.1.2 and twelve Controlify 0.1.1 JARs. Metadata, checksums, production packaging, absence of bundled Mouse Tweaks/QA code, unchanged public API class bytes, unchanged 24 companion hashes, and module boundaries passed verification.

## Coverage limits

- Five of twelve final artifacts received direct Computer Use gameplay checks. The remaining seven were compiled and packaged, not launched for this fix.
- The available Computer Use API does not expose right-button dragging or a held Shift modifier during a mouse gesture. Right-click splitting/placement was exercised, but physical RMB-drag and Shift-drag were not. Earlier synthetic callback coverage is documented separately; it is not manual coverage of this final build.
- Some early wheel attempts had no effect while changing pointer targets. Settled-hover attempts and server snapshots were used for assertions; first-event timing was not quantified.
- The synthetic crafting test was strengthened to cover an occupied player stack and excess wheel ticks, but that updated synthetic case was not run during this playthrough.
- High-latency/dedicated-server sessions, every external API consumer, all Mouse Tweaks configuration combinations, and unrelated gameplay features were not tested. These results do not establish that the mod is bug-free.
