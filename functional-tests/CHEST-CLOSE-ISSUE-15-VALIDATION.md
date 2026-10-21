# Chest closing reproduction: GitHub issue 15

Date: 2026-10-20

Report: https://github.com/Salt-and-Fries/Salt-s-Inventory-Update/issues/15

## Result

**Reproduced in standalone Core 0.1.5 on Fabric 1.21.1 and Fabric 26.3.**

| Persistent Windows | Gesture | Window dismissed | Server chest viewers afterward | Client lid afterward |
| --- | --- | --- | --- | --- |
| Disabled | Hold E | Yes | 0 | Closed |
| Disabled | Hold Esc | Yes | 0 | Closed |
| Disabled | Tap Esc | Yes | 0 | Closed |
| Enabled | Hold E | Yes | **1** | **Fully open** |
| Enabled | Hold Esc | Yes | 0 | Closed |
| Enabled | Tap Esc | Yes | **1** | **Fully open** |

Both targets produced this same six-case result. The exact reported hold-E
behavior remains reproducible when Persistent Windows is enabled. Holding Esc
closes the chest correctly in these builds; tapping Esc reproduces the open lid
with Persistent Windows enabled. The report does not specify that setting.

All cases started with an actual open chest, one native viewer and lid openness
1.0. After input, the test waited another 45 client ticks for opener rechecks,
block events and animation. The failing cases still had one viewer and lid
openness 1.0; successful cases had zero viewers and openness 0.0. All 32 dirt
items remained in the chest in every case. Persistent windows could be restored.

## Runtime method and artifacts

- Actual rendered Minecraft clients and integrated singleplayer servers, each
  using a fresh disposable world.
- Shipping Core JARs from `dist/core`, plus Fabric API and a separate test mod.
  Compatibility and Controlify were absent. Project implementation class
  directories were removed from the game classpath.
- Synthetic key press/release events entered Core's unmodified
  `InventoryKeyHoldController.handleInventoryKeyAction`, with its normal screen
  callback fallback. Holds used 900 ms of real elapsed time against the 500 ms
  setting. This was an automated in-game test, not a physical keyboard test.
- The test observed the actual native screen, server chest opener counter,
  client chest lid animation value and framebuffer. Screenshots for an open lid
  after dismissal and a normally closed lid were visually inspected.
- Two final profiles, six cases each, 130 report records and 15 screenshots per
  target. **Each profile has four failing assertions:** the native viewer count
  and lid-closed assertions for persistent hold E and persistent tap Esc. These
  are reproductions, not passing regression suites.
- No production code or distributable JAR changed during this investigation.

| Minecraft / loader | Core JAR SHA-256 |
| --- | --- |
| 1.21.1 Fabric | `6b01b4a650403e99bbef30054459efcfd80b880a9d25e4c61a5912d0bb17620d` |
| 26.3 Fabric | `75b21d340c104f1ad167d254d513da9fb722440513dfdf259db5e34b6380e62d` |

The staged inputs match these distribution hashes. Fabric 1.21.1 applied its
normal development namespace processing; its report records the processed JAR
as the code source. Fabric 26.3 loaded the staged JAR directly.

## Evidence and reproduction

Final reports and captures:

- `build/gameplay-qa/packaged-core/1.21.1-fabric-issue15-confirm/`
- `build/gameplay-qa/packaged-core/26.3-fabric-issue15-confirm/`
- `build/issue-15-chest-close/summary.json`
- `build/issue-15-chest-close/1.21.1-fabric-confirm.log`
- `build/issue-15-chest-close/26.3-fabric-confirm.log`

The earlier `26.3-fabric-issue15-repro` exploration is retained separately. Its
screen-dismissal assertions initially inspected the retained desktop singleton
instead of the active native screen. The final profiles corrected that test
observation and contain only the four product bug assertions listed above.

From the repository root, after building the Core distribution, run either
supported target with the standalone packaged launcher:

```powershell
$qaMinecraft = '26.3' # Also tested with '1.21.1'.
$qaArguments = @(
    '-I', 'functional-tests/packaged-qa.init.gradle',
    "-PqaMinecraft=$qaMinecraft", '-PqaScenario=core',
    '-PqaRunId=issue15-confirm', '-PqaChestCloseOnly=true',
    '-PqaExtension=false', '-PqaUseDistribution=true',
    '-PincludeFunctionalTests=false',
    '-PsaltsDesktopRunDiagnostics=false', '-PsaltsDesktopRunTrace=false',
    ":${qaMinecraft}:fabric:runPackagedGameplay",
    '--no-parallel', '--max-workers=2', '--console=plain'
)
.\gradlew.bat @qaArguments
```

Inspect `results.json` and `SIU_GAMEPLAY_SUMMARY` for the gameplay result. The
Java launch can exit normally while gameplay assertions report `FAIL`; a
successful Gradle launch alone does not mean this regression passed.

## Suspected cause and limits

Persistent dismissal calls `hideWindowPersistently`, which sends a visibility
change while retaining the server menu session. Core's native chest ownership
check still recognizes that retained session, and the vanilla menu has no
integration visibility callback that stops its native opener. This explains the
observed difference between hiding a window and permanently closing it. This
is source analysis consistent with the reproduction, not an implemented fix.

This pass covers single vanilla chests in singleplayer on the two Fabric
targets above. Forge, NeoForge, other Minecraft versions, dedicated multiplayer,
double chests and third-party container implementations were not tested here.
