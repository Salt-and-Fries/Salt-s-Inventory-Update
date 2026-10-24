# In-place inventory sorting: all Core targets

## Scope and result

Core 0.1.5 provides **Sort this inventory** beside the existing transfer-sort control on all twelve supported targets: Fabric/Forge 1.20.1 and Fabric/NeoForge 1.21.1, 1.21.11, 26.1.2, 26.2 and 26.3. The focused sorting tests and production artifact build passed. The former 26.3 Fabric-only gates have been removed. Older versions use their native item, tag, slot and networking APIs.

## Automated regression

```powershell
.\gradlew.bat -I functional-tests/inventory-sort-tests.init.gradle `
  -I functional-tests/core-artifacts-qa.init.gradle `
  :1.20.1:fabric:test :1.21.1:fabric:test :1.21.11:fabric:test `
  :26.1.2:fabric:test :26.2:fabric:test :26.3:fabric:test `
  stageCoreQaArtifacts bundleLatestMods --console=plain
```

- `InventorySortTest`: 13 cases for category order, common tags on an otherwise unrelated item, datapack category overrides, material families, partial-stack consolidation, custom names and damaged equipment, per-slot limits, impossible layouts, idempotence/conservation, protected slots, rollback on a custom callback changing a protected neighbour, held cursor refusal and malformed packet rejection.
- `SortWindowsTest`: 20 existing transfer-sort/routing cases, including aliases, destination priorities, callbacks, large destination storage, components and player inventory boundaries.

Each of the six Minecraft mappings passed **13 in-place sorting cases and 20 existing transfer-sort cases: 198 tests, zero failures or skipped cases**. Headless fixtures bind the relevant item components and tags explicitly where required by that game version. The common-tag test simulates a modded classification contract; it does not launch a third-party content mod. Forge/NeoForge reuse the matching mapped implementation, and all six native-loader artifacts compiled and packaged successfully.

The init script filters every requested test task. An earlier command placed `--tests` only after the last task, which unintentionally ran additional tests on 1.20.1 and 1.21.1. That command reported 14 failures on 1.20.1 (session resynchronization and slot gestures) and 13 slot-gesture failures on 1.21.1. Those failures are outside the two sorting test classes and have not been diagnosed in this change; they are not claimed to be resolved or proven pre-existing. The final filtered run executed exactly 33 sorting tests per Minecraft target and passed. Logs remain in `build/sort-all-targets/legacy-tests.log` and `build/sort-all-targets/release-build.log`.

## Focused native-loader in-game check

```powershell
.\gradlew.bat -I functional-tests/inventory-sort-native-qa.init.gradle `
  '-PincludeFunctionalTests=true' '-PqaMinecraft=26.3' '-PqaLoader=neoforge' `
  '-PqaRunId=all-targets' '-PsaltsSmokeRun=true' '-PsaltsSmokeProfile=sort-all-targets' `
  '-PsaltsDesktopRunDiagnostics=false' '-PsaltsDesktopRunTrace=false' `
  :26.3:neoforge:runClient --console=plain
```

A rendered Minecraft 26.3 NeoForge client launched Core's development classes with only Minecraft, NeoForge and Core installed. The disposable integrated-server world sent real authenticated sorting packets through title/menu mouse callbacks and checked authoritative server inventories. This was a source-runtime check; the twelve production artifacts were subsequently rebuilt without QA sources and inspected separately.

The run recorded **58 passing assertions, 22 completed stages and eight framebuffer captures, zero failures**. It checked:

- Adjacent outer/inner controls on chest and player windows; texture column 1 for the inner control.
- Compact title row and adjacent sorting actions in More Controls, including activation and dismissal.
- Gems first, consolidated ingots, equipment/supplies/building order, dirt/gravel last.
- Custom item name, sword damage and Sharpness III preserved.
- All 159 seeded chest items conserved; repeat sorting leaves the result unchanged.
- Player inventory ordering and consolidation; hotbar, armor, offhand and the other open container unchanged.
- The existing outer sort still moves player items into matching open storage.
- Furnace has no in-place sort action; it retains its existing transfer control.

Machine-readable evidence and framebuffer captures:

```
build/gameplay-qa/inventory-sort/26.3-neoforge-all-targets/results.json
build/gameplay-qa/inventory-sort/26.3-neoforge-all-targets/screenshots/
build/sort-all-targets/native-gameplay.log
build/sort-all-targets/release-build.log
```

The sorted chest capture was visually inspected. Earlier packaged 26.3 Fabric evidence remains at `build/gameplay-qa/packaged-core/26.3-fabric-smart-sort-final/`; that historical run is not a new all-target gameplay test.

## Packaging and boundaries

All twelve production Core 0.1.5 artifacts are refreshed in `dist/core/` and mirrored in `build/release/0.1.5/`, with updated SHA-256 manifests and matching entries in `dist/salts-inventory-all-latest.zip`. All 24 existing Compatibility and Controlify binaries retain their original hashes. API 6 and desktop protocol 5 remain unchanged. Modern clients detect the additional sorting packet channel; 1.20.1 negotiates the additive `CAP_SORT_INVENTORY` capability through its existing authenticated transport.

`python functional-tests/verify-inventory-sort-artifacts.py` passed for all twelve JARs. It checks the sort packet, sorter, placement planner, server implementation and UI control; translated tooltip; correct loader metadata/version; absence of QA classes; SHA-256 manifests; matching release copies; and matching bundle entries. The artifact report is `build/sort-all-targets/artifacts.json`.

## Coverage limits

Fresh gameplay validation was limited to 26.3 NeoForge's integrated server, vanilla storage and player inventories. The other eleven production target combinations were not freshly gameplay-tested in this rollout. A separate dedicated-server multiplayer pairing, an older-Core server, creative inventory, purchased expansion slots, real third-party storage/modded content and datapack reload were not exercised in this profile. Their routing/classification follows the existing APIs and metadata contracts, but this report does not claim those gameplay scenarios passed. Exception/slot-restriction behavior was tested with headless custom-slot fixtures. Unusually large layouts can be refused by the bounded snapshot-verification budget. No complete gameplay suite was run.
