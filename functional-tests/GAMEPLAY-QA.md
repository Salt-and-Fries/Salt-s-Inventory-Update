# In-world gameplay QA

`InWorldGameplayHarness` creates disposable flat worlds in isolated run directories.
It opens real native menus, invokes the desktop's mouse callbacks, waits for authenticated
network mutations, checks authoritative integrated-server state, and saves native framebuffer
captures. It is opt-in and excluded from production JARs.

Run from the repository root on Windows:

```powershell
.\functional-tests\scripts\Run-GameplayQa.ps1 -Scenario core -Minecraft 1.21.1 -Loader fabric
.\functional-tests\scripts\Run-GameplayQa.ps1 -Scenario extension-empty
.\functional-tests\scripts\Run-GameplayQa.ps1 -Scenario browser-jei
.\functional-tests\scripts\Run-GameplayQa.ps1 -Scenario browser-emi
.\functional-tests\scripts\Run-GameplayQa.ps1 -Scenario browser-rei
.\functional-tests\scripts\Run-GameplayQa.ps1 -Scenario toms
.\functional-tests\scripts\Run-GameplayQa.ps1 -Scenario sophisticated -Loader neoforge
.\functional-tests\scripts\Run-GameplayQa.ps1 -Scenario ae2 -Minecraft 26.2 -Loader neoforge
.\functional-tests\scripts\Run-GameplayQa.ps1 -Scenario ae2-atlas -Minecraft 26.3 -Loader neoforge
```

Use the target's supported loader. Optional runtime dependency versions come from the
normal Gradle configuration. Tom's Storage currently requires the verified Fabric 1.21.1
fixture downloaded into the `toms` smoke profile; the script checks for that exact fixture.
The REI 1.21.1 profile also uses cached Mod Menu 11.0.4. Run Gradle profiles sequentially:
concurrent Loom cache rebuilds can interfere with one another.

Each launch writes `build/gameplay-qa/<scenario>/<minecraft>-<loader>/launch.log`,
`results.json`, and `screenshots/`. A pass requires both the JSON result and an explicit
`SIU_GAMEPLAY_SUMMARY status=PASS` marker, not just a successful launcher exit.
Creating `abort.request` in that output directory requests a controlled stop. The script
clears the previous result and its owned screenshot/atlas directories at launch so
stale evidence cannot pass a failed launch. It preserves disposable worlds.

## Production archive checks

This separate QA mod removes project implementation classes from the runtime classpath
and loads the actual release archives from its own mods directory:

```powershell
.\gradlew.bat -I functional-tests/packaged-qa.init.gradle :1.21.1:fabric:runPackagedGameplay
.\gradlew.bat -I functional-tests/packaged-qa.init.gradle -PqaExtension=true :1.21.1:fabric:runPackagedGameplay
```

Omit `includeFunctionalTests` for these launches. The QA initializer is compiled into its
own source set. Both production JARs retain their own metadata and code. Fabric's development
launcher may remap installed production archives; this is not a consumer-launcher test.

## Coverage and interpretation

Core coverage includes local windows, inventory expansion and XP cost, extra-slot item
movement, chest click/split/merge, both drag modes with item conservation, window controls,
all listed vanilla block menus, crafting, smelting, and four mount menus.
The final packaged and Sophisticated profiles also reload resource packs and
render the inventory again afterwards.
Integration coverage includes the eight Tom's menu families and terminal withdrawal/return;
Sophisticated backpacks, source-item locking, crafting upgrade, settings tabs and three
storage variants; recipe-browser item/search/recipe/bookmark contracts; and Mod Menu config.
The `ae2` profile checks four native menu families plus pickup through the public shared
cursor API. The `ae2-atlas` profile ports the reference fork's 40-container/90-presentation
fixtures and geometry checks, with populated inventories and the reference window settings.
It uses unmodified upstream AE2. Its test mixin observes the extension's own window and
public API handles; native AE2 reflection is confined to the excluded atlas sources.
The atlas flags and sources never belong in a distributable build. Compare rendered
captures separately before claiming identical UI.

A stage completion, a checked assertion, and a saved image are different evidence types.
Opening/rendering a menu does not test every native operation. Dedicated-server networking,
all upgrades/recipes, arbitrary target versions/mod combinations, persistence across world
reloads, and long-term gameplay require further scenarios. Test-driver reflection into core
classes is confined to the excluded QA harness; the compatibility mod still compiles against
only the public API.

After the selected profiles finish, run the normal root build to regenerate clean production
artifacts and execute all regression and boundary checks. Generate the evidence report with
`tools/generate-gameplay-report.py`; review every rendered PDF page before distribution.
