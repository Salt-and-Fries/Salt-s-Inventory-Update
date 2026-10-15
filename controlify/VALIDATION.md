# Controlify integration validation

## Current 26.3 verification status

The live recipe-wheel overhaul is implemented on `controllify-jar-test`. The
complete root build and artifact checks passed: all 36 separate production JARs
are current, and both 26.3 companions contain the current source textures.
Both Fabric and NeoForge test harnesses compile successfully.

Final fresh 26.3 gameplay verification remains pending. Windows reports both
graphics devices stopped with Code 43, and the Minecraft SDL video backend reports
"No displays available" before the game window initializes. The implementation
goal remains unfinished until the remaining native checks pass on both loaders.
No driver settings were changed, and no commit or merge was performed.

The [current build archive](../dist/salts-inventory-all-latest.zip) contains all
36 JARs, checksums and build verification, with an explicit pending runtime notice.
Replace both matching 26.3 Core and Controlify companion JARs when testing.


Generated from local evidence at 2026-10-14T16:16:24+00:00. Refresh with `python tools/report-controlify-validation.py`.

The complete target inventory contains **12 Minecraft/loader combinations**. **12/12 matching Core and companion artifact pairs** are collected. Packaged gameplay is verified for **10/12 targets** against those exact artifacts.

Full runtime matrix verification remains incomplete. Pending or stale/failed reports below do not establish working gameplay for that target.

## Automated tests

The seven scoped JUnit XML directories contain **901 tests**, with 0 failures, 0 errors and 0 skips. All suites passed.

| Suite | Tests | Status | Report |
| --- | ---: | --- | --- |
| Controller model/input/render | 140 | Passed | [HTML](model/build/reports/tests/test/index.html) |
| Core 1.20.1 | 119 | Passed | [HTML](../versions/1.20.1/fabric/build/reports/tests/test/index.html) |
| Core 1.21.1 | 121 | Passed | [HTML](../versions/1.21.1/fabric/build/reports/tests/test/index.html) |
| Core 1.21.11 | 121 | Passed | [HTML](../versions/1.21.11/fabric/build/reports/tests/test/index.html) |
| Core 26.1.2 | 121 | Passed | [HTML](../versions/26.1.2/fabric/build/reports/tests/test/index.html) |
| Core 26.2 | 121 | Passed | [HTML](../versions/26.2/fabric/build/reports/tests/test/index.html) |
| Core 26.3 | 158 | Passed | [HTML](../versions/26.3/fabric/build/reports/tests/test/index.html) |

Core tests run once per Minecraft version through its Fabric test source set. Forge and NeoForge share the ported Core implementation; their native compilation, archive checks and individual gameplay runs are separate evidence. Extended compatibility test counts are outside this controller matrix total.

The regressions cover workspace lifetimes, mandatory-window protection, restoration, scoped HUD settings, native/external slot geometry, modal ownership and outside dismissal, exact displayed-pair destinations, server authorization and transaction rollback, native crafting/Loom/furnace result callbacks and replenishment. Modern API fixtures also check supplementary Unicode character events. The controller suite covers three-mode state, input routing, bounded layouts, animation, transformed clipping and queued-render opacity capture.

## Artifacts and isolation

Independent source, all twelve compiler manifests, collected companion bytecode and matching Core peer archives: **Passed**. The reporter calls the existing [boundary verifier](../tools/verify-controlify-boundaries.py) for these checks. Dedicated companions and installation-bundle copies must be byte-identical.

`buildControlify` also checks native Controlify/Minecraft member references and mixin selectors against each target's actual compiler inputs, and verifies the published controller JAR against its SHA-512 pin. These are build gates; gameplay status is determined independently below.

The third JAR contains the controller layout, bindings, animation and render adapters. Core supplies generic public workspace/transfer APIs. Upstream Controlify/YACL, Core and the optional extended compatibility mod remain separate archives. QA fixtures are excluded from production JARs.

| Minecraft | Loader | Matching artifact pair | Packaged gameplay |
| --- | --- | --- | --- |
| 1.20.1 | Fabric | Present | Passed |
| 1.20.1 | Forge | Present | Passed |
| 1.21.1 | Fabric | Present | Passed |
| 1.21.1 | NeoForge | Present | Passed |
| 1.21.11 | Fabric | Present | Passed |
| 1.21.11 | NeoForge | Present | Passed |
| 26.1.2 | Fabric | Present | Passed |
| 26.1.2 | NeoForge | Present | Passed |
| 26.2 | Fabric | Present | Passed |
| 26.2 | NeoForge | Present | Passed |
| 26.3 | Fabric | Present | Pending |
| 26.3 | NeoForge | Present | Needs rerun |

The following additional upstream runtime dependency is installed separately. Its original published bytes and declared version must also pass the packaged runtime verifier.

| Minecraft | Loader | Required runtime dependency | Pin |
| --- | --- | --- | --- |
| 1.20.1 | Forge | LexManos's Mixin Args 1.0 | [Original release](https://www.curseforge.com/minecraft/mc-mods/mixin-args/files/6488404); `mixin_args-1.0.jar`; SHA-512 in targets.json |

## Packaged gameplay evidence

The [runtime verifier](../tools/verify-controlify-runtime.py) requires successful scenario records for all eleven baseline objective areas, plus 37 feature areas for 26.3, actual matching Minecraft/Controlify/YACL versions, original dependency SHA-512 pins, separate production JAR code sources, final collected/staged Core and companion SHA-256 equality, fresh reports and actual PNG captures. Every existing report is revalidated when this file is generated.

| Minecraft | Loader | Status | Evidence |
| --- | --- | --- | --- |
| 1.20.1 | Fabric | Passed | 290 PASS records; 18 screenshots; 63.118 s; [report](../build/controlify-qa/1.20.1-fabric/results.json) |
| 1.20.1 | Forge | Passed | 290 PASS records; 18 screenshots; 60.803 s; [report](../build/controlify-qa/1.20.1-forge/results.json) |
| 1.21.1 | Fabric | Passed | 291 PASS records; 18 screenshots; 63.167 s; [report](../build/controlify-qa/1.21.1-fabric/results.json) |
| 1.21.1 | NeoForge | Passed | 291 PASS records; 18 screenshots; 58.879 s; [report](../build/controlify-qa/1.21.1-neoforge/results.json) |
| 1.21.11 | Fabric | Passed | 295 PASS records; 18 screenshots; 61.621 s; [report](../build/controlify-qa/1.21.11-fabric/results.json) |
| 1.21.11 | NeoForge | Passed | 295 PASS records; 18 screenshots; 58.876 s; [report](../build/controlify-qa/1.21.11-neoforge/results.json) |
| 26.1.2 | Fabric | Passed | 293 PASS records; 18 screenshots; 60.687 s; [report](../build/controlify-qa/26.1.2-fabric/results.json) |
| 26.1.2 | NeoForge | Passed | 291 PASS records; 18 screenshots; 57.550 s; [report](../build/controlify-qa/26.1.2-neoforge/results.json) |
| 26.2 | Fabric | Passed | 299 PASS records; 18 screenshots; 60.663 s; [report](../build/controlify-qa/26.2-fabric/results.json) |
| 26.2 | NeoForge | Passed | 299 PASS records; 18 screenshots; 58.360 s; [report](../build/controlify-qa/26.2-neoforge/results.json) |
| 26.3 | Fabric | Pending | No completed packaged gameplay report |
| 26.3 | NeoForge | Needs rerun | 26.3-neoforge: missing 26.3 continuous recipe carousel proof: ['contextual-held-LB-five-recipes-per-second', 'contextual-held-RB-five-recipes-per-second', 'contextual-left-stick-inward-return-does-not-shift-selection', 'contextual-recipe-large-deadzone-retains-outer-and-inner-selection', 'contextual-right-stick-inward-return-does-not-shift-wheel']; [report](../build/controlify-qa/26.3-neoforge/results.json) |

The disposable integrated-server harness installs a synthetic driver in the real Controlify manager and uses actual bindings, native cursor/screen processing and server packets. Supported mixed input mode prevents operating-system cursor events from interrupting automation. This provides game runtime evidence; physical controller hardware has not been tested.

Each completed run must cover permanent/default windows; side-by-side native pointer, D-pad snapping and exact transfers; radial navigation/focus/bumpers; gameplay gating; tuck/add/restore identities; protected and individual close; immediate side/radial close on 26.3 or the configured hold threshold on older versions; HUD/animation settings; native result callbacks and conservation/XP; protruding custom UI and modal dismissal; and the genuine controller/server handshake.

```powershell
.\gradlew.bat buildControlify collectControlifyJars collectControlifyTestJars --console=plain --no-parallel
.\gradlew.bat -I controlify/functional-tests/controlify-qa.init.gradle :controlify:1.21.1:fabric:runPackagedControlifyQa
python tools/report-controlify-validation.py
python tools/verify-controlify-runtime.py --summary build/controlify-runtime-verified.json
```

Use the corresponding `:controlify:<minecraft>:<loader>:runPackagedControlifyQa` task for each target. The full runtime verification command rejects an incomplete matrix.

## Collected file hashes

Install each companion with the matching rebuilt Core and the pinned native dependencies listed in [README](README.md). The installation bundle keeps `core/` and `controlify/` separate; dedicated companion copies are in `dist/controlify/`.

| Artifact | Bytes | SHA-256 |
| --- | ---: | --- |
| [1.20.1 Fabric core](../dist/core/salts_inventory_update-1.20.1-Fabric-0.1.4.jar) | 950943 | `82fe0dc21e6ea0b88b7bcd7240c84bab6305ed17808d04a33a522dda39d18830` |
| [1.20.1 Fabric controlify](../dist/controlify/salts_inventory_controlify-1.20.1-Fabric-0.1.4.jar) | 60297 | `026af89bfda700323e7ac3456afebcdf38620c2b1ad866135b35aa1699d0b5d5` |
| [1.20.1 Forge core](../dist/core/salts_inventory_update-1.20.1-Forge-0.1.4.jar) | 962785 | `e3dfa7476522995e855194e6d6b215449d10192152de366b011885c1069a9535` |
| [1.20.1 Forge controlify](../dist/controlify/salts_inventory_controlify-1.20.1-Forge-0.1.4.jar) | 60750 | `6950724d0fa0cac4074e079c61672e9ab8aa258b72be5436617a019fda17fc70` |
| [1.21.1 Fabric core](../dist/core/salts_inventory_update-1.21.1-Fabric-0.1.4.jar) | 992441 | `7d03bae92a58106beccac6c26149e0a51b10ad41285ee8b431e4dcf634cf9711` |
| [1.21.1 Fabric controlify](../dist/controlify/salts_inventory_controlify-1.21.1-Fabric-0.1.4.jar) | 59977 | `d6bd2df1bc566cfeecc4555414017f21e9a21b5635717c3cea8aa20ecb82a273` |
| [1.21.1 NeoForge core](../dist/core/salts_inventory_update-1.21.1-NeoForge-0.1.4.jar) | 1001441 | `67920e21288b782ca06b1312673390acdf91f664b456442b8910c6c20b465731` |
| [1.21.1 NeoForge controlify](../dist/controlify/salts_inventory_controlify-1.21.1-NeoForge-0.1.4.jar) | 59284 | `3f1f2d31215bcfb3eee33db252287ec00d1f26e85c011e876d8b1fd17416f348` |
| [1.21.11 Fabric core](../dist/core/salts_inventory_update-1.21.11-Fabric-0.1.4.jar) | 986008 | `0c5f82a467923db6767466f64ced99d0817f79de4f713153e18c99278e104079` |
| [1.21.11 Fabric controlify](../dist/controlify/salts_inventory_controlify-1.21.11-Fabric-0.1.4.jar) | 67248 | `f5c875dc38341d7a09e2453145459a0fde425f3d3128cdea7287d24aa19efbbe` |
| [1.21.11 NeoForge core](../dist/core/salts_inventory_update-1.21.11-NeoForge-0.1.4.jar) | 995887 | `997725f829cc05f901532c10ce1bfa287a5113bcf217e3caa565d2233bbb666d` |
| [1.21.11 NeoForge controlify](../dist/controlify/salts_inventory_controlify-1.21.11-NeoForge-0.1.4.jar) | 66739 | `02c3295337c8dab3c4d4f2ea487fde6c3ebd93cb893f988a1dd55cb940069f3d` |
| [26.1.2 Fabric core](../dist/core/salts_inventory_update-26.1.2-Fabric-0.1.4.jar) | 990600 | `9c3373f313d2fbd0d728750dd3bd008c30b4907d0a0fe8701a5dbc7564c5fba6` |
| [26.1.2 Fabric controlify](../dist/controlify/salts_inventory_controlify-26.1.2-Fabric-0.1.4.jar) | 68390 | `59465ef0205d3cb79cc8a1fdace57a3843ce2236629506db367389ec10b9edd5` |
| [26.1.2 NeoForge core](../dist/core/salts_inventory_update-26.1.2-NeoForge-0.1.4.jar) | 988782 | `32efeae1b945af90faeeff43f92c7969b5870811a260868d170b584ec3b330b6` |
| [26.1.2 NeoForge controlify](../dist/controlify/salts_inventory_controlify-26.1.2-NeoForge-0.1.4.jar) | 67018 | `5a3a53213cbeca04b613d68b1f650fe03b7fdb846b60c399624f55c1a7897953` |
| [26.2 Fabric core](../dist/core/salts_inventory_update-26.2-Fabric-0.1.4.jar) | 992861 | `2466a7d95ec5e4dc88168ff7e74038a2b6b0089cfaa1722703705ae4f3b22e4f` |
| [26.2 Fabric controlify](../dist/controlify/salts_inventory_controlify-26.2-Fabric-0.1.4.jar) | 68367 | `6d8d1bae3c561e5d1dc8590b6bdabc6bbc10bc8b78b78087ec9955b48704c8e0` |
| [26.2 NeoForge core](../dist/core/salts_inventory_update-26.2-NeoForge-0.1.4.jar) | 991007 | `792be81c5a208e3f519dd3e6570db7ed6224927ccb3c56200718b4b9515c960b` |
| [26.2 NeoForge controlify](../dist/controlify/salts_inventory_controlify-26.2-NeoForge-0.1.4.jar) | 66992 | `9b2114c5ebec865254d4f506d389fdd59f8e29bf82cd40739328d89bce12609a` |
| [26.3 Fabric core](../dist/core/salts_inventory_update-26.3-Fabric-0.1.4.jar) | 1086000 | `18bb32bef4823223500c98f06cbe97b003f1951d68341f74789b88eb76b87bf6` |
| [26.3 Fabric controlify](../dist/controlify/salts_inventory_controlify-26.3-Fabric-0.1.4.jar) | 234701 | `1b2d1cf8e4ec098c42d1a658667e913bfe108a38a1535f1f40b90fbfa7f18ebd` |
| [26.3 NeoForge core](../dist/core/salts_inventory_update-26.3-NeoForge-0.1.4.jar) | 1082786 | `d4fa8b2a99851a0b71bdbb3ed89cbc94fa69adb4e1b149bc07bb9559e7a99e9b` |
| [26.3 NeoForge controlify](../dist/controlify/salts_inventory_controlify-26.3-NeoForge-0.1.4.jar) | 229837 | `887f5b5fcdc691814a8bb7fedfe6a4251376dbd4a3f2aee822a09416eb55070f` |
