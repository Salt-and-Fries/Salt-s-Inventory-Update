# Controlify integration validation

Validation date: 2026-10-12. Built targets: Minecraft 1.21.1, Fabric and
NeoForge, Salt's Inventory Update / Controlify companion 0.1.4, Controlify
3.0.1+lts. Use the rebuilt matching core from this branch.

## Build and isolation

The final serialized refresh completed successfully:

```powershell
.\gradlew.bat buildControlify collectControlifyJars collectControlifyTestJars :compatibility:1.21.1:fabric:build :compatibility:1.21.1:neoforge:build --console=plain --no-parallel
```

The complete JUnit XML reports contain 204 tests, with zero failures, errors or
skips:

| Suite | Tests | Report |
| --- | ---: | --- |
| Controller model, input routing, animation and clipping | 35 | [Controller report](model/build/reports/tests/test/index.html) |
| Core API and inventory behavior, including controller workspace fixtures | 121 | [Core report](../versions/1.21.1/fabric/build/reports/tests/test/index.html) |
| Extended compatibility, Fabric | 35 | [Fabric compatibility report](../compatibility/versions/1.21.1/fabric/build/reports/tests/test/index.html) |
| Extended compatibility, NeoForge | 13 | [NeoForge compatibility report](../compatibility/versions/1.21.1/neoforge/build/reports/tests/test/index.html) |

The core total includes 19 workspace API and 13 authenticated quick-move
regressions. These cover permanent-window protection, exact displayed-pair
destinations, native modal ownership and dismissal, external controls and slot
snapping, recipe replenishment, partial result transfers, Loom result slot 3,
and furnace extraction accounting. The build also passed 16 AE2 merge-tool
tests and resolved 9,315 native desktop instruction member references.

[The isolation verifier](../tools/verify-controlify-boundaries.py) passed for
28 controller Java source files, both compiler classpaths (114 Fabric / 90
NeoForge entries), both companion JARs and all four core/extended peer JARs.
Each companion contains 23 classes exclusively in
`com.salts_inventory_update.controlify`, no bundled core/extended or upstream
dependency classes, no nested dependency JARs, and no QA harness. Its compiler
receives the core's public API archive. Core and extended bytecode and metadata
have no dependency on Controlify or the companion implementation.

The new core hooks are generic workspace and transfer APIs; controller layouts,
bindings, animations and transformed clipping live in the third JAR.
The extended compatibility JAR remains optional.

## Packaged gameplay verification

The final exact-artifact Fabric launch exited successfully with zero failures
in 60.747 seconds. Its [report](../build/controlify-qa/1.21.1-fabric/results.json)
contains **305 records: 286 PASS, 18 CAPTURED and one OBSERVED**. These are
scenario steps, actions, assertions, captures and an observation. The
[screenshots](../build/controlify-qa/1.21.1-fabric/screenshots) show the three
modes, Character rendering, native crafting/Loom output and client-only UI.

```powershell
.\gradlew.bat -I controlify/functional-tests/controlify-qa.init.gradle :controlify:1.21.1:fabric:runPackagedControlifyQa
```

The recorded runtime uses Java 21, Minecraft 1.21.1, Fabric Loader 0.18.2,
Fabric API 0.116.1+1.21.1, Controlify 3.0.1+lts and YACL 3.8.2+1.21.1-fabric.
The expanded Loom and modal-dismissal cases passed against the same final
production files; production build/remap tasks remained up to date.

The opt-in fixture uses a synthetic Controlify driver, actual ControllerEntity
and input bindings, and a disposable integrated-server world. It uses
Controlify's supported mixed input mode to prevent native operating-system
mouse events from interrupting the automated sequence. Production input code
is unchanged. This verifies actual game runtime behavior without claiming
physical controller hardware coverage.

The launcher removes project implementation directories from the runtime
classpath and stages the original published Controlify/YACL JARs with their
nested libraries. The report records core, companion and Controlify code
sources from Fabric's processed production mod JARs. The staged core and
companion hashes match the collected artifacts below. The separate test
initializer is excluded from distributable JARs.

| Objective requirement | Evidence |
| --- | --- |
| 1. Permanent inventory/Character and three modes | Ordinary inventory open, mandatory windows, protected close attempts, side/radial/tucked mode assertions; model lifecycle tests |
| 2. Side-by-side pair, native controls and bumpers | Initial pair screenshot; native cursor and D-pad slot snapping; exact chest-to-player and chest-to-barrel quick-move; RB/LB selection assertions |
| 3. LT radial navigation and focus swap | Joystick Character highlight, A focus swap, LT release selection, cursor visibility assertions and screenshots |
| 4. Tuck, gameplay and restoration | Same window/model/session identities across tuck; real walking and native world USE opening a barrel while tucked; configured Y restoration; client-only UI opened while tucked |
| 5. New window defaults | Player-focused chest and client-only UI opening assertions |
| 6. Individual close and permanent protection | Highlighted barrel/client UI closure, close callback once, player and Character protection |
| 7. Configured hold-to-close-all | Rebound close action, core hold-duration threshold, both modes, server-session disposal and return to gameplay |
| 8. HUD/offhand arrangement | Runtime forced-HUD/offhand assertion; scoped API preference regression |
| 9. Smooth/configurable animation | Five interpolation/lifecycle animation tests; radial/tuck/restore screenshots; disabled-animation immediate-target assertion |
| 10. Independent third artifact/public API | Source, compiler and six-JAR isolation checks; recorded separate production code sources |
| 11. Native UI and inventory functionality | Default A click on native/protruding client-only UI, modal outside dismissal, native crafting/Loom/furnace result callbacks, conservation and furnace XP assertions |

Nine pure clipping tests cover all four transformed rectangle corners,
rotation/reflection, outward rounding, empty clips, nested scopes and cleanup.
The captured Character avatar renders under the transformed native clipping.

This run validates packaged Fabric gameplay. NeoForge passed compilation,
packaging, matching core and extended builds, and artifact isolation; it has
not undergone the packaged in-world scenario. Other Minecraft releases listed
in [README](README.md#other-minecraft-versions) are upstream availability, not
built integration targets.

## Collected artifacts

Install the matching loader's core and companion together with Controlify and
its required dependencies. The following SHA-256 values identify the final
files; sizes are bytes.

| Artifact | Bytes | SHA-256 |
| --- | ---: | --- |
| [Fabric core](../dist/controlify-test/core/salts_inventory_update-1.21.1-Fabric-0.1.4.jar) | 992441 | `7d03bae92a58106beccac6c26149e0a51b10ad41285ee8b431e4dcf634cf9711` |
| [NeoForge core](../dist/controlify-test/core/salts_inventory_update-1.21.1-NeoForge-0.1.4.jar) | 1001441 | `67920e21288b782ca06b1312673390acdf91f664b456442b8910c6c20b465731` |
| [Fabric companion](../dist/controlify/salts_inventory_controlify-1.21.1-Fabric-0.1.4.jar) | 53719 | `88cde137cc41eb41a5a256fe7ccc4b42a5567786ff0b602891d35622803622cb` |
| [NeoForge companion](../dist/controlify/salts_inventory_controlify-1.21.1-NeoForge-0.1.4.jar) | 53208 | `4a75b7c69344716e959c87269617a5881ab02072e412ac92a2126ba193af30e9` |
| [Optional Fabric extended](../compatibility/versions/1.21.1/fabric/build/libs/salts_inventory_compat-1.21.1-Fabric-0.1.4.jar) | 260319 | `202110987f55909383791b6a975978999784146f696de119a586ed8ab19c366a` |
| [Optional NeoForge extended](../compatibility/versions/1.21.1/neoforge/build/libs/salts_inventory_compat-1.21.1-NeoForge-0.1.4.jar) | 642804 | `46b41330c46cec2c8a3e0efc739d734e288c2a1f0ed99c5760d3cb6d15726cba` |

The installation bundle under `dist/controlify-test/` keeps `core/` and
`controlify/` separate. The dedicated companion copies under
`dist/controlify/` are byte-identical to their installation-bundle counterparts.
