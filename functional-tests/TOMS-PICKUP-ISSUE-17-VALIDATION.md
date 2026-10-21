# Tom's Storage pickup report: focused reproduction

The current production JARs did **not reproduce** [issue 17](https://github.com/Salt-and-Fries/Salt-s-Inventory-Update/issues/17), which reports terminal stock decreasing without an item appearing on the cursor on Fabric 26.3.

## Initial Minecraft 26.3 Fabric check

One disposable, rendered Minecraft 26.3 Fabric profile loaded the actual distribution JARs:

- Core **0.1.5**.
- Compatibility **0.1.2**.
- Official Tom's Storage **2.12.2** and its required libraries, selected from the existing hash-pinned compatibility fixture manifest.
- Fabric API and a separate opt-in QA mod. Controlify, recipe browsers and unrelated integration targets were absent.

Only the **storage terminal** and **crafting terminal** were exercised. A native connector linked each terminal to the same physical chest, seeded with 32 dirt and 32 planks. Mouse callbacks went through the real desktop window and authenticated payload handler. No server inventory or cursor was assigned to simulate a successful pickup.

For each terminal, a normal left click withdrew 32 items. The client shared cursor contained 32 items, the canonical server cursor contained 32, and 32 remained in storage. Depositing the carried stack restored the storage total to 64 and emptied the cursor. Both menus passed these checks. Eight native screenshots were captured; the storage-terminal pickup image also visibly shows the 32-item carried stack.

The profile completed with **50 report records and zero failures**. The Gradle invocation finished successfully in 1m16s. Runtime Core and Compatibility JARs match the current distribution bytes exactly. The source review found the same public shared-cursor commit pattern in all six version source trees, which supply their respective loader builds; this is source inspection, not gameplay verification of those other targets.

## Evidence

- `build/toms-pickup-issue17/summary.json`: conclusion, exact artifact hashes and scope.
- `build/toms-pickup-issue17/26.3-fabric-before.log`: complete launch and gameplay log.
- `build/toms-pickup-issue17/26.3-fabric-fixtures.json`: upstream fixture inventory and hashes.
- `build/gameplay-qa/packaged-toms/26.3-fabric-issue17-before/results.json`: assertions and production code sources.
- The adjacent `screenshots/` folder contains each terminal before pickup, while carrying the stack and after depositing it.

The `before` run ID identifies an unmodified 26.3 production baseline. The subsequent 1.20.1 initialization patch is recorded below.

## Minecraft 1.20.1 follow-up

Both Fabric and Forge were checked with Core **0.1.5**, Compatibility **0.1.2** and the official loader-specific Tom's Storage **1.7.1** JAR. The scope remained limited to the storage and crafting terminal withdrawal/deposit interaction above.

The initial Fabric run passed all 50 records. The initial Forge run exposed a different problem: the adapter waited for Core's client-started notification, but Forge had already dispatched it before the extension's queued client initialization. No Tom client window definitions were registered, so the native Tom screens opened instead of desktop sessions. The resulting eight report failures were the opening timeout and subsequent checks without a desktop session; that run did not establish pickup loss.

The 1.20.1 adapter now calls its existing `registerAll("initializer")` during initialization, matching the other five version implementations. Its client-started registration remains available. This uses the existing public window registration API; no Core internals, reflection bypass, Core changes or API additions are involved. The shared source supplies both the 1.20.1 Fabric and Forge Compatibility JARs.

After rebuilding only those two production JARs, the focused checks passed:

| Minecraft | Loader | Tom's Storage | Report records | Failures | Screenshots |
| --- | --- | --- | ---: | ---: | ---: |
| 1.20.1 | Fabric | 1.7.1 | 50 | 0 | 8 |
| 1.20.1 | Forge | 1.7.1 | 50 | 0 | 8 |

Both terminal types held 32 items on the visible client cursor and canonical server cursor after pickup, with 32 remaining in storage. Depositing restored all 64 items to storage and emptied the cursor. Forge's pickup screenshot also visibly shows the carried stack. The original pickup-loss symptom was not reproduced on either final loader build.

Only `TomsStorageClientCompat.class` differs in each changed archive. All other entries and all other 34 release JARs are unchanged. Both rebuilt Compatibility files retain version **0.1.2**. Distribution checksums and the 36-JAR release ZIP were refreshed and verified. Core and Controlify distribution artifacts are unchanged.

Evidence:

- `build/toms-pickup-issue17/summary-1201.json`: both final profiles, artifact hashes, changed archive members and scope.
- `build/toms-pickup-issue17/1.20.1-fabric.log` and `1.20.1-forge.log`: initial baseline logs, including the Forge initialization failure.
- `build/toms-pickup-issue17/1.20.1-fabric-fixed.log` and `1.20.1-forge-fixed.log`: final passing gameplay logs.
- `build/gameplay-qa/packaged-toms/1.20.1-fabric-issue17-1201-fix/`: Fabric results and eight screenshots. Staged Core and Compatibility bytes match the distribution exactly.
- `build/toms-pickup-issue17/gameplay/1.20.1-forge-issue17-1201-fix/`: Forge results, eight screenshots, staged inventory and `forge-remap-lineage.json`. The development runtime uses the same genuine ModDev SRG-to-MCP transform as other packaged Forge QA; original distribution/upstream hashes, transformed hashes and unchanged resources are verified separately.
- `build/toms-pickup-issue17/before-1201/`: preserved baseline artifacts and source.

The two production archive tasks compiled successfully. No complete build/test suite was run. The final two gameplay invocations took 1m20s on Fabric and 1m3s on Forge. A focused bytecode check on the two rebuilt JARs also confirmed no references to the removed window API calls.

## Reproduce the focused check

Use a fresh run ID to retain earlier evidence:

```powershell
python tools/prepare-compatibility-qa.py --minecraft 26.3 --loader fabric --scenario toms `
    --output build/toms-pickup-issue17/26.3-fabric-fixtures.json
$qaArgs = @('-I', 'functional-tests/packaged-qa.init.gradle',
    '-I', 'functional-tests/compatibility-coverage.init.gradle',
    '-PqaMinecraft=26.3', '-PqaRunId=issue17-another-run',
    '-PqaExtension=true', '-PqaScenario=toms', '-PqaTomsPickupOnly=true',
    '-PqaUseDistribution=true', '-PcompatibilityQaOriginalJars=true',
    '-PcompatibilityQaProject=:26.3:fabric',
    '-PcompatibilityQaManifest=build/toms-pickup-issue17/26.3-fabric-fixtures.json',
    '-PincludeFunctionalTests=false', '-PsaltsDesktopRunDiagnostics=false',
    '-PsaltsDesktopRunTrace=false', ':26.3:fabric:runPackagedGameplay',
    '--no-parallel', '--max-workers=2', '--console=plain')
& .\gradlew.bat @qaArgs
```

Check `results.json` and `SIU_GAMEPLAY_SUMMARY`; successful process exit alone does not establish a passing gameplay result. `qaTomsPickupOnly` excludes unrelated menus and resource reload checks. On a failed withdrawal observation it stops before additional item actions, retaining the failure and framebuffer evidence.

The shared native container launcher also supports the focused Forge check. Prepare a matching `toms` manifest for `1.20.1/forge`, then use:

```powershell
$forgeArgs = @('-I', 'functional-tests/chest-close-qa.init.gradle',
    '-PqaMinecraft=1.20.1', '-PqaLoader=forge', '-PqaRunId=issue17-another-run',
    '-PqaTomsPickupOnly=true', '-PincludeFunctionalTests=false',
    '-PcompatibilityQaManifest=build/toms-pickup-issue17/1.20.1-forge-fixtures.json',
    '-PsaltsDesktopRunDiagnostics=false', '-PsaltsDesktopRunTrace=false',
    ':1.20.1:forge:runTomsPickupQa', '--no-parallel', '--max-workers=2', '--console=plain')
& .\gradlew.bat @forgeArgs
```

Despite the historical init-script filename, `qaTomsPickupOnly` selects the two-terminal scenario and excludes the chest/window-cleanup regression. Omitting that property preserves the original standalone chest profile.

## Outcome and limits

The pickup-loss report did not reproduce on 26.3 Fabric or the final 1.20.1 Fabric/Forge builds. The separate 1.20.1 initialization bug was fixed and its two Compatibility JARs rebuilt. The narrow opt-in reproduction remains available for future reports.

Other game/loader combinations beyond the three listed, dedicated multiplayer, other Salt/Tom release sets, alternative terminal control modes, rapid repeated clicks and third-party mod combinations were not tested here. These results establish these specific setups, not the absence of the bug in every possible environment.
