# Controlify integration validation

Generated from local evidence at 2026-10-19T15:55:07+00:00. Refresh with `python tools/report-controlify-validation.py --feature-parity --artifacts dist --output controlify/functional-tests/all-versions-verification.md`.

The complete target inventory contains **12 Minecraft/loader combinations**. **12/12 matching Core and companion artifact pairs** are collected. Packaged scoped verification is complete for **12/12 targets**. The covered scopes and artifact lineage are reported below.

Legacy 1.20.1/1.21.1/1.21.11 runs exercise the ported recipe, creative, scrolling, keyboard and custom-window features. Modern targets use the main feature suite plus focused retries where needed. These scopes are not described as a complete functional test of every target.

Focused reports must match all three final Salt JARs exactly. Reuse of earlier modern main-suite evidence permits only four corrected bullet/ellipsis UTF-8 constants in one wheel class, requiring all remaining class and resource bytes to be identical and a successful wheel retry on the final JAR. A main report that stopped only in the independent post-suite API setup also requires a successful final-JAR API retry. The original reports and their setup failures remain recorded; failures in the completed main phase are never accepted.

For legacy Fabric development launches, the original staged Salt inputs remain byte-identical to final dist artifacts and original upstream inputs retain their published SHA-512 pins. The actual loader creates named-namespace runtime copies in the owned profile’s `.fabric/processedMods/` directory. The focused verifier invokes that same native `RuntimeModRemapper` with the recorded mapping JAR, classpath and selected original/nested inputs, then compares every generated member name and byte with the loaded copies (16 selected JARs per Fabric profile). Forge records the official `SRG_TO_MCP` tool, mapping hashes and named derivatives of the five preserved original Salt/Controlify/YACL JARs. The native code sources must resolve to their verified same-profile runtime copies. Reproducing this evidence requires the retained native mapping/classpath cache and a local JDK.

## Automated tests

The 13 scoped JUnit XML directories contain **1435 tests**, with 0 failures, 0 errors and 0 skips. All suites passed.

| Suite | Tests | Status | Report |
| --- | ---: | --- | --- |
| Controller model/input/render | 216 | Passed | [HTML](../model/build/reports/tests/test/index.html) |
| Core 1.20.1 | 151 | Passed | [HTML](../../versions/1.20.1/fabric/build/reports/tests/test/index.html) |
| Core 1.21.1 | 154 | Passed | [HTML](../../versions/1.21.1/fabric/build/reports/tests/test/index.html) |
| Core 1.21.11 | 154 | Passed | [HTML](../../versions/1.21.11/fabric/build/reports/tests/test/index.html) |
| Core 26.1.2 | 171 | Passed | [HTML](../../versions/26.1.2/fabric/build/reports/tests/test/index.html) |
| Core 26.2 | 171 | Passed | [HTML](../../versions/26.2/fabric/build/reports/tests/test/index.html) |
| Core 26.3 | 171 | Passed | [HTML](../../versions/26.3/fabric/build/reports/tests/test/index.html) |
| Extended 1.20.1 providers | 66 | Passed | [HTML](../../compatibility/versions/1.20.1/fabric/build/reports/tests/test/index.html) |
| Extended 1.21.1 providers | 37 | Passed | [HTML](../../compatibility/versions/1.21.1/fabric/build/reports/tests/test/index.html) |
| Extended 1.21.11 providers | 37 | Passed | [HTML](../../compatibility/versions/1.21.11/fabric/build/reports/tests/test/index.html) |
| Extended 26.1.2 providers | 35 | Passed | [HTML](../../compatibility/versions/26.1.2/fabric/build/reports/tests/test/index.html) |
| Extended 26.2 providers | 35 | Passed | [HTML](../../compatibility/versions/26.2/fabric/build/reports/tests/test/index.html) |
| Extended 26.3 providers | 37 | Passed | [HTML](../../compatibility/versions/26.3/fabric/build/reports/tests/test/index.html) |

Core tests run once per Minecraft version through its Fabric test source set. Forge and NeoForge share the ported Core implementation; their native compilation, archive checks and individual gameplay runs are separate evidence. Extended provider tests are included for each version in this parity pass.

The regressions cover workspace lifetimes, mandatory-window protection, restoration, scoped HUD settings, native/external slot geometry, modal ownership and outside dismissal, exact displayed-pair destinations, server authorization and transaction rollback, native crafting/Loom/furnace result callbacks and replenishment. Modern API fixtures also check supplementary Unicode character events. The controller suite covers three-mode state, input routing, bounded layouts, animation, transformed clipping and queued-render opacity capture.

## Artifacts and isolation

Independent source, all twelve compiler manifests, collected companion bytecode and matching Core peer archives: **Passed**. The reporter calls the existing [boundary verifier](../../tools/verify-controlify-boundaries.py) for these checks. Dedicated companions and installation-bundle copies must be byte-identical.

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
| 26.3 | Fabric | Present | Passed |
| 26.3 | NeoForge | Present | Passed |

The following additional upstream runtime dependency is installed separately. Its original published bytes and declared version must also pass the packaged runtime verifier.

| Minecraft | Loader | Required runtime dependency | Pin |
| --- | --- | --- | --- |
| 1.20.1 | Forge | LexManos's Mixin Args 1.0 | [Original release](https://www.curseforge.com/minecraft/mc-mods/mixin-args/files/6488404); `mixin_args-1.0.jar`; SHA-512 in targets.json |

## Packaged gameplay evidence

The [runtime verifier](../../tools/verify-controlify-runtime.py) checks modern main-suite evidence for all eleven baseline objective areas, plus 49 feature areas for modern targets, actual matching Minecraft/Controlify/YACL versions, original dependency SHA-512 pins, separate production JAR code sources, verified final collected/staged artifact lineage, fresh reports and actual PNG captures. Legacy ported-feature scopes and modern focused retries are checked independently by the focused verifier. Every report used by this document is revalidated when the file is generated.

| Minecraft | Loader | Status | Evidence |
| --- | --- | --- | --- |
| 1.20.1 | Fabric | Passed | 158 distinct checks; 5 screenshots; 49.086 s; ported features and custom-window APIs; [report](../../build/controlify-qa/1.20.1-fabric-feature-parity-extended/results.json) |
| 1.20.1 | Forge | Passed | 158 distinct checks; 5 screenshots; 46.895 s; ported features and custom-window APIs; [report](../../build/controlify-qa/1.20.1-forge-feature-parity-extended/results.json) |
| 1.21.1 | Fabric | Passed | 161 distinct checks; 5 screenshots; 47.676 s; ported features and custom-window APIs; [report](../../build/controlify-qa/1.21.1-fabric-feature-parity-extended/results.json) |
| 1.21.1 | NeoForge | Passed | 161 distinct checks; 5 screenshots; 45.383 s; ported features and custom-window APIs; [report](../../build/controlify-qa/1.21.1-neoforge-feature-parity-extended/results.json) |
| 1.21.11 | Fabric | Passed | 161 distinct checks; 5 screenshots; 47.967 s; ported features and custom-window APIs; [report](../../build/controlify-qa/1.21.11-fabric-feature-parity-extended/results.json) |
| 1.21.11 | NeoForge | Passed | 161 distinct checks; 5 screenshots; 46.400 s; ported features and custom-window APIs; [report](../../build/controlify-qa/1.21.11-neoforge-feature-parity-extended/results.json) |
| 26.1.2 | Fabric | Passed | 2065 distinct checks; 93 screenshots; 360.065 s; main suite + public-api, wheel-polish; original post-suite API setup failed; final API retry passed; [report](../../build/controlify-qa/26.1.2-fabric-extended/results.json); [public-api](../../build/controlify-qa/26.1.2-fabric-public-api-extended/results.json); [wheel-polish](../../build/controlify-qa/26.1.2-fabric-wheel-polish-extended/results.json) |
| 26.1.2 | NeoForge | Passed | 2065 distinct checks; 93 screenshots; 353.362 s; main suite + public-api, wheel-polish; original post-suite API setup failed; final API retry passed; [report](../../build/controlify-qa/26.1.2-neoforge-extended/results.json); [public-api](../../build/controlify-qa/26.1.2-neoforge-public-api-extended/results.json); [wheel-polish](../../build/controlify-qa/26.1.2-neoforge-wheel-polish-extended/results.json) |
| 26.2 | Fabric | Passed | 2065 distinct checks; 93 screenshots; 303.745 s; main suite + wheel-polish; [report](../../build/controlify-qa/26.2-fabric-extended/results.json); [wheel-polish](../../build/controlify-qa/26.2-fabric-wheel-polish-extended/results.json) |
| 26.2 | NeoForge | Passed | 1970 distinct checks; 91 screenshots; 271.556 s; all recorded scenarios; [report](../../build/controlify-qa/26.2-neoforge-extended/results.json) |
| 26.3 | Fabric | Passed | 1970 distinct checks; 91 screenshots; 286.847 s; main suite + public-api; [report](../../build/controlify-qa/26.3-fabric-extended/results.json); [public-api](../../build/controlify-qa/26.3-fabric-public-api-extended/results.json) |
| 26.3 | NeoForge | Passed | 1970 distinct checks; 91 screenshots; 281.990 s; main suite + public-api; [report](../../build/controlify-qa/26.3-neoforge-extended/results.json); [public-api](../../build/controlify-qa/26.3-neoforge-public-api-extended/results.json) |

The disposable integrated-server harness installs a synthetic driver in the real Controlify manager and uses actual bindings, native cursor/screen processing and server packets. Supported mixed input mode prevents operating-system cursor events from interrupting automation. This provides game runtime evidence; physical controller hardware has not been tested.

The modern main-suite runs cover permanent/default windows; side-by-side native pointer, D-pad snapping and exact transfers; radial navigation/focus/bumpers; gameplay gating; tuck/add/restore identities; protected and individual close; immediate side/radial close on all targets; HUD/animation settings; native result callbacks and conservation/XP; protruding custom UI and modal dismissal; and the genuine controller/server handshake.

```powershell
.\gradlew.bat buildControlify collectControlifyJars collectControlifyTestJars --console=plain --no-parallel
.\gradlew.bat -I controlify/functional-tests/controlify-qa.init.gradle :controlify:1.21.1:fabric:runPackagedControlifyQa
python tools/report-controlify-validation.py
python tools/verify-controlify-runtime.py --summary build/controlify-runtime-verified.json
```

Use the corresponding `:controlify:<minecraft>:<loader>:runPackagedControlifyQa` task for each target. The full runtime verification command rejects an incomplete matrix.

## Collected file hashes

Install each companion with the matching rebuilt Core and the pinned native dependencies listed in [README](../README.md). The installation bundle keeps `core/` and `controlify/` separate; dedicated companion copies are in `dist/controlify/`.

| Artifact | Bytes | SHA-256 |
| --- | ---: | --- |
| [1.20.1 Fabric core](../../dist/core/salts_inventory_update-1.20.1-Fabric-0.1.4.jar) | 1041117 | `4e8deaf281058bd1c891e0cde5e2c5aa07a02a6e34bfb0323ff355f24800c0b6` |
| [1.20.1 Fabric controlify](../../dist/controlify/salts_inventory_controlify-1.20.1-Fabric-0.1.4.jar) | 333947 | `1b5de43cd060ddbe477c2bb7006edffe166a3899e73264584f324f35a790d3b7` |
| [1.20.1 Forge core](../../dist/core/salts_inventory_update-1.20.1-Forge-0.1.4.jar) | 1053013 | `9c37333ad21a22f6df9c0b2c84e6e2caa5c2bdd8ac2d3e9350090a1065ed93ce` |
| [1.20.1 Forge controlify](../../dist/controlify/salts_inventory_controlify-1.20.1-Forge-0.1.4.jar) | 330697 | `ef85e0250005355191c092b1bfb416719eb58a664302415e03748ece06f8c195` |
| [1.21.1 Fabric core](../../dist/core/salts_inventory_update-1.21.1-Fabric-0.1.4.jar) | 1094997 | `c6397adcca4ee9d6600a9334033643961dce823bf9855a15cdf0c1446218317a` |
| [1.21.1 Fabric controlify](../../dist/controlify/salts_inventory_controlify-1.21.1-Fabric-0.1.4.jar) | 337567 | `e61ed5061d6a5a8ad270d55aad4cfe9aad9ca8cfd4ffd5579f4d7437675ad6c3` |
| [1.21.1 NeoForge core](../../dist/core/salts_inventory_update-1.21.1-NeoForge-0.1.4.jar) | 1103680 | `789ced3c03de9869a25ded9e8950393ebc80d04abac245b20883c98cc66915e8` |
| [1.21.1 NeoForge controlify](../../dist/controlify/salts_inventory_controlify-1.21.1-NeoForge-0.1.4.jar) | 332929 | `65e4a9fafc006714cc82c9ba0d5edf0c4a5d25e1ad5811ec60a79a9c99770c83` |
| [1.21.11 Fabric core](../../dist/core/salts_inventory_update-1.21.11-Fabric-0.1.4.jar) | 1087776 | `6fa605ec2e396c5e06c81b721aeabe793f19c186161f39371e80f5f37c1e26a4` |
| [1.21.11 Fabric controlify](../../dist/controlify/salts_inventory_controlify-1.21.11-Fabric-0.1.4.jar) | 340015 | `8db0949fdf663d8240bc394c1b86cc23befd8f1fb169729575e6efdadfcb480b` |
| [1.21.11 NeoForge core](../../dist/core/salts_inventory_update-1.21.11-NeoForge-0.1.4.jar) | 1097303 | `9e9fccfcc6e228bfa61faa4a80039ceaf9f1c18c75f4cfc71bc4024e52823671` |
| [1.21.11 NeoForge controlify](../../dist/controlify/salts_inventory_controlify-1.21.11-NeoForge-0.1.4.jar) | 335853 | `67a35ddd13497be4479909f783995ba31b106bbe256b2050ce33c636979164b2` |
| [26.1.2 Fabric core](../../dist/core/salts_inventory_update-26.1.2-Fabric-0.1.4.jar) | 1094830 | `fe90e52dcb2ada924c33dc7531069efaa63b84cd7af61eaf7c97dd8d29bde04f` |
| [26.1.2 Fabric controlify](../../dist/controlify/salts_inventory_controlify-26.1.2-Fabric-0.1.4.jar) | 344605 | `704887a5551d74a0414c8bf69db99885f65d6c20deb8bf6b8a81c9edca9072af` |
| [26.1.2 NeoForge core](../../dist/core/salts_inventory_update-26.1.2-NeoForge-0.1.4.jar) | 1091536 | `7663155db14f1c2f5576b02859a7b9876ebb56267a5359b0ffab32fc465bff50` |
| [26.1.2 NeoForge controlify](../../dist/controlify/salts_inventory_controlify-26.1.2-NeoForge-0.1.4.jar) | 338049 | `066f246e505a53cfa68afdeb59acbfd43360d78187d386f0af76b9e1d566fdfd` |
| [26.2 Fabric core](../../dist/core/salts_inventory_update-26.2-Fabric-0.1.4.jar) | 1097146 | `161f74f7964548021d27d5cf7eeea33ccaafc6c9dfa4ff407828ddd26e4bf8f4` |
| [26.2 Fabric controlify](../../dist/controlify/salts_inventory_controlify-26.2-Fabric-0.1.4.jar) | 344762 | `81433a0d0be7d877596f4bc21a4a1bb0c7c38f68d96a59c87333a1041bf6ad26` |
| [26.2 NeoForge core](../../dist/core/salts_inventory_update-26.2-NeoForge-0.1.4.jar) | 1093816 | `b171f53d29d3f6ba1d6b4bf4369da345c95f418567495aeba2f7fd961b78d364` |
| [26.2 NeoForge controlify](../../dist/controlify/salts_inventory_controlify-26.2-NeoForge-0.1.4.jar) | 338203 | `41404fc5d41158fc4431dd00857561c3ba9f4fdfe00c9113a020bf6e4037f7e3` |
| [26.3 Fabric core](../../dist/core/salts_inventory_update-26.3-Fabric-0.1.4.jar) | 1097540 | `5234b477fdcf0e96ca4d10ff003f724d6cc001e7a25df09effe86f835068d095` |
| [26.3 Fabric controlify](../../dist/controlify/salts_inventory_controlify-26.3-Fabric-0.1.4.jar) | 344204 | `e306fd6f3ee224d77d598a4dd13a2eb547ca8ebfaf713938f1e0d1073394386a` |
| [26.3 NeoForge core](../../dist/core/salts_inventory_update-26.3-NeoForge-0.1.4.jar) | 1094218 | `7a07fe7729168dc05bb8a32aa8f68fb4d230f0d402ba77bc2901e952fd07098f` |
| [26.3 NeoForge controlify](../../dist/controlify/salts_inventory_controlify-26.3-NeoForge-0.1.4.jar) | 337684 | `19a7d13b5cb16e1933f8a2546f60d153d4f175f73802392c3366ef5d824e938f` |
