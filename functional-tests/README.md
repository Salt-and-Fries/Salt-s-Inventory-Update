# Functional Tests

This directory contains the shared functional-test setup for Salt's Inventory Update across every supported Minecraft version and loader.

Normal builds do not include this harness. It is only compiled when Gradle is run with:

```powershell
-PincludeFunctionalTests=true
```

## Version Matrix

The current runtime matrix covers every supported version and loader pair:

| Minecraft version | Loaders tested |
| --- | --- |
| `1.20.1` | `fabric`, `forge` |
| `1.21.1` | `fabric`, `neoforge` |
| `1.21.11` | `fabric`, `neoforge` |
| `26.1.2` | `fabric`, `neoforge` |
| `26.2` | `fabric`, `neoforge` |

## Test Types

There are three layers:

1. Static source parity checks in `scripts/Test-SourceFeatureParity.ps1`.
2. Gated compile checks through the Gradle task `functionalTestCompile`.
3. Optional in-client runtime checks in `FunctionalTestHarness`.

The first two are fast and do not open Minecraft. The third launches Minecraft clients and waits for `SIU_FUNCTIONAL_TEST_SUMMARY`.

## Static Source Parity Tests

File:

```text
functional-tests/scripts/Test-SourceFeatureParity.ps1
```

This script checks that every version keeps the same declared feature surface, then checks every loader entrypoint and metadata file in the ten-pair matrix.

### Desktop Menu Coverage

For each version, the script verifies that both client and server code reference each supported `MenuType`.

Checked in client:

```text
versions/<version>/fabric/src/main/java/com/salts_inventory_update/client/InventoryDesktopScreen.java
```

Checked in server:

```text
versions/<version>/fabric/src/main/java/com/salts_inventory_update/server/DesktopContainerSessions.java
```

Menus checked for all versions:

| Test label pattern | Required menu |
| --- | --- |
| `<version> client menu GENERIC_9x1` / `<version> server menu GENERIC_9x1` | `MenuType.GENERIC_9x1` |
| `<version> client menu GENERIC_9x2` / `<version> server menu GENERIC_9x2` | `MenuType.GENERIC_9x2` |
| `<version> client menu GENERIC_9x3` / `<version> server menu GENERIC_9x3` | `MenuType.GENERIC_9x3` |
| `<version> client menu GENERIC_9x4` / `<version> server menu GENERIC_9x4` | `MenuType.GENERIC_9x4` |
| `<version> client menu GENERIC_9x5` / `<version> server menu GENERIC_9x5` | `MenuType.GENERIC_9x5` |
| `<version> client menu GENERIC_9x6` / `<version> server menu GENERIC_9x6` | `MenuType.GENERIC_9x6` |
| `<version> client menu GENERIC_3x3` / `<version> server menu GENERIC_3x3` | `MenuType.GENERIC_3x3` |
| `<version> client menu ANVIL` / `<version> server menu ANVIL` | `MenuType.ANVIL` |
| `<version> client menu BEACON` / `<version> server menu BEACON` | `MenuType.BEACON` |
| `<version> client menu BLAST_FURNACE` / `<version> server menu BLAST_FURNACE` | `MenuType.BLAST_FURNACE` |
| `<version> client menu BREWING_STAND` / `<version> server menu BREWING_STAND` | `MenuType.BREWING_STAND` |
| `<version> client menu CRAFTING` / `<version> server menu CRAFTING` | `MenuType.CRAFTING` |
| `<version> client menu ENCHANTMENT` / `<version> server menu ENCHANTMENT` | `MenuType.ENCHANTMENT` |
| `<version> client menu FURNACE` / `<version> server menu FURNACE` | `MenuType.FURNACE` |
| `<version> client menu GRINDSTONE` / `<version> server menu GRINDSTONE` | `MenuType.GRINDSTONE` |
| `<version> client menu HOPPER` / `<version> server menu HOPPER` | `MenuType.HOPPER` |
| `<version> client menu LOOM` / `<version> server menu LOOM` | `MenuType.LOOM` |
| `<version> client menu MERCHANT` / `<version> server menu MERCHANT` | `MenuType.MERCHANT` |
| `<version> client menu SHULKER_BOX` / `<version> server menu SHULKER_BOX` | `MenuType.SHULKER_BOX` |
| `<version> client menu SMITHING` / `<version> server menu SMITHING` | `MenuType.SMITHING` |
| `<version> client menu SMOKER` / `<version> server menu SMOKER` | `MenuType.SMOKER` |
| `<version> client menu CARTOGRAPHY_TABLE` / `<version> server menu CARTOGRAPHY_TABLE` | `MenuType.CARTOGRAPHY_TABLE` |
| `<version> client menu STONECUTTER` / `<version> server menu STONECUTTER` | `MenuType.STONECUTTER` |

Additional menu checked for all versions except `1.20.1`:

| Test label pattern | Required menu |
| --- | --- |
| `<version> client menu CRAFTER_3x3` / `<version> server menu CRAFTER_3x3` | `MenuType.CRAFTER_3x3` |

### Packet Payload Coverage

For each version, the script verifies that this file declares every desktop networking payload:

```text
versions/<version>/fabric/src/main/java/com/salts_inventory_update/network/DesktopPackets.java
```

Payload tests:

| Test label pattern | Required payload/text |
| --- | --- |
| `<version> packet InventorySlotPurchasePayload` | `InventorySlotPurchasePayload` |
| `<version> packet InventoryExpansionSyncPayload` | `InventoryExpansionSyncPayload` |
| `<version> packet DesktopReadyPayload` | `DesktopReadyPayload` |
| `<version> packet DesktopClickPayload` | `DesktopClickPayload` |
| `<version> packet DesktopQuickMovePayload` | `DesktopQuickMovePayload` |
| `<version> packet DesktopButtonPayload` | `DesktopButtonPayload` |
| `<version> packet DesktopPlaceRecipePayload` | `DesktopPlaceRecipePayload` |
| `<version> packet DesktopRenamePayload` | `DesktopRenamePayload` |
| `<version> packet DesktopCustomPayload` | `DesktopCustomPayload` |
| `<version> packet DesktopCloseSessionPayload` | `DesktopCloseSessionPayload` |
| `<version> packet DesktopSessionPinPayload` | `DesktopSessionPinPayload` |
| `<version> packet DesktopSessionVisibilityPayload` | `DesktopSessionVisibilityPayload` |
| `<version> packet DesktopOpenSessionPayload` | `DesktopOpenSessionPayload` |
| `<version> packet DesktopSlotPayload` | `DesktopSlotPayload` |
| `<version> packet DesktopDataPayload` | `DesktopDataPayload` |
| `<version> packet DesktopCarriedPayload` | `DesktopCarriedPayload` |
| `<version> packet DesktopGhostRecipePayload` | `DesktopGhostRecipePayload` |
| `<version> packet DesktopSessionClosedPayload` | `DesktopSessionClosedPayload` |
| `<version> packet DesktopMerchantOffersPayload` | `DesktopMerchantOffersPayload` |
| `<version> packet pin modes` | `PIN_MODE_GHOST_PINNED` |
| `<version> quick move target` | `QUICK_TARGET_HOTBAR` |

### Client Entry Coverage

For each version, the script verifies client-facing entry points in:

```text
versions/<version>/fabric/src/main/java/com/salts_inventory_update/client/WindowedInventoryClient.java
```

Tests:

| Test label pattern | Required text |
| --- | --- |
| `<version> character keybind` | `GLFW_KEY_C` |
| `<version> client command root` | `"salts_inventory"` |
| `<version> config command` | `"config"` |
| `<version> functional hook` | `FunctionalTestHarness` |

### Config Field Coverage

For each version, the script verifies key config fields in:

```text
versions/<version>/fabric/src/main/java/com/salts_inventory_update/client/SaltsInventoryConfig.java
```

Tests:

| Test label pattern | Required text |
| --- | --- |
| `<version> config enableMod` | `enableMod` |
| `<version> config expandableInventory` | `expandableInventory` |
| `<version> config ghost pins` | `enableGhostPins` |

### Loader Bootstrap Coverage

For each loader, the script verifies loader bootstrap calls in:

```text
versions/<version>/<loader>/src/main/java/com/salts_inventory_update/SaltsInventoryUpdateFabric.java
versions/<version>/<loader>/src/main/java/com/salts_inventory_update/SaltsInventoryUpdateForge.java
versions/<version>/<loader>/src/main/java/com/salts_inventory_update/SaltsInventoryUpdateNeoForge.java
```

Tests:

| Test label pattern | Required text |
| --- | --- |
| `<version> <loader> payload registration` | `DesktopPackets.registerPayloadTypes();` |
| `<version> <loader> server session init` | `DesktopContainerSessions.initialize();` |

Fabric also has a separate client initializer:

```text
versions/<version>/fabric/src/main/java/com/salts_inventory_update/SaltsInventoryUpdateFabricClient.java
```

| Test label pattern | Required text |
| --- | --- |
| `<version> fabric client init` | `WindowedInventoryClient.initialize();` |

### Loader Metadata Coverage

For each Fabric loader, the script verifies entrypoint and mixin metadata in:

```text
versions/<version>/fabric/src/main/resources/fabric.mod.json
```

Fabric metadata tests:

| Test label pattern | Required text |
| --- | --- |
| `<version> fabric entrypoint metadata` | `"entrypoints"` |
| `<version> fabric main entrypoint metadata` | `SaltsInventoryUpdateFabric` |
| `<version> fabric client entrypoint metadata` | `SaltsInventoryUpdateFabricClient` |
| `<version> fabric mixin metadata block` | `"mixins"` |
| `<version> fabric mixin metadata config` | `${mod_id}.mixins.json` |

For Forge and NeoForge loaders, the script verifies mixin metadata in:

```text
versions/<version>/<loader>/src/main/resources/META-INF/mods.toml
versions/<version>/<loader>/src/main/resources/META-INF/neoforge.mods.toml
```

Forge also requires a manifest-level Mixin config for the 1.20.1 Forge dev/runtime path:

```text
versions/<version>/forge/src/main/resources/META-INF/MANIFEST.MF
```

The Forge 1.20.1 dev client also needs the Mixin config passed as a launch argument because its exploded classpath
manifest is visible to the mod after startup but is not consumed by Mixin's early platform scan:

```text
--mixin.config salts_inventory_update.mixins.json
```

Forge/NeoForge metadata tests:

| Test label pattern | Required text |
| --- | --- |
| `<version> <loader> mixin metadata block` | `[[mixins]]` |
| `<version> <loader> mixin metadata config` | `config = "${mod_id}.mixins.json"` |
| `<version> forge mixin manifest config` | `MixinConfigs: salts_inventory_update.mixins.json` |
| `<version> forge dev run mixin launch arg` | `"--mixin.config", "salts_inventory_update.mixins.json"` |

## Gradle Compile Test

Task:

```text
functionalTestCompile
```

Command:

```powershell
.\gradlew.bat --no-daemon -PincludeFunctionalTests=true functionalTestCompile --console=plain
```

This compiles every fabric/forge/neoforge loader project with:

```text
functional-tests/src/main/java
```

added to the main source set. It catches Java/API incompatibilities between the shared harness and each Minecraft version.

## Runtime Harness Tests

File:

```text
functional-tests/src/main/java/com/salts_inventory_update/functionaltest/FunctionalTestHarness.java
```

The runtime harness is enabled with either:

```powershell
$env:SIU_FUNCTIONAL_TESTS = 'true'
```

or:

```text
-Dsalts_inventory_update.functionalTests=true
```

It logs every test as:

```text
SIU_FUNCTIONAL_TEST test=<name> status=PASS
SIU_FUNCTIONAL_TEST test=<name> status=FAIL reason=<reason>
```

and finishes with:

```text
SIU_FUNCTIONAL_TEST_SUMMARY status=PASS passed=<count> failed=0 mc=<version> loader=<loader>
```

### Runtime Suite: `runtime-and-keybinds`

| Exact test name | What it verifies |
| --- | --- |
| `runtime.configured_enabled` | The configured runtime flag can be true. |
| `runtime.server_desktop_available` | The server desktop availability flag can be true. |
| `runtime.enabled` | The combined runtime enabled state is true when both runtime flags are true. |
| `keybind.character_window_registered` | The character-window keybind was registered by `WindowedInventoryClient`. |

### Runtime Suite: `config-normalization`

The harness saves the current config, writes deliberately extreme/invalid values, reloads the config, checks normalization, then restores the original config.

| Exact test name | What it verifies |
| --- | --- |
| `config.enable_mod_updates_runtime` | Setting `enableMod = false` updates `SaltsInventoryRuntime` to disabled. |
| `config.window_opening_style_normalizes` | Invalid `windowOpeningStyle` normalizes to the parser fallback. |
| `config.ghost_opacity_clamps_high` | `ghostWindowOpacity = 42.0` clamps to `0.90`. |
| `config.e_hold_seconds_clamps_low` | `eHoldCloseAllSeconds = 0.10` clamps to `0.50`. |

### Runtime Suite: `desktop-menu-screens`

The harness reads `MenuScreens` and verifies that the desktop screen constructor is present for each supported menu.

Exact tests for all versions:

| Exact test name |
| --- |
| `menu_screen.generic_9x1` |
| `menu_screen.generic_9x2` |
| `menu_screen.generic_9x3` |
| `menu_screen.generic_9x4` |
| `menu_screen.generic_9x5` |
| `menu_screen.generic_9x6` |
| `menu_screen.generic_3x3` |
| `menu_screen.anvil` |
| `menu_screen.beacon` |
| `menu_screen.blast_furnace` |
| `menu_screen.brewing_stand` |
| `menu_screen.crafting` |
| `menu_screen.enchantment` |
| `menu_screen.furnace` |
| `menu_screen.grindstone` |
| `menu_screen.hopper` |
| `menu_screen.loom` |
| `menu_screen.merchant` |
| `menu_screen.shulker_box` |
| `menu_screen.smithing` |
| `menu_screen.smoker` |
| `menu_screen.cartography_table` |
| `menu_screen.stonecutter` |

Additional runtime test when the Minecraft version exposes `MenuType.CRAFTER_3x3`:

| Exact test name |
| --- |
| `menu_screen.crafter_3x3` |

### Runtime Suite: `desktop-api-definitions`

These verify that built-in desktop API definitions were registered for furnace-style menus.

| Exact test name | What it verifies |
| --- | --- |
| `api_definition.furnace` | `SaltsInventoryDesktopApi.findDefinition(...)` returns a definition for `MenuType.FURNACE`. |
| `api_definition.blast_furnace` | `SaltsInventoryDesktopApi.findDefinition(...)` returns a definition for `MenuType.BLAST_FURNACE`. |
| `api_definition.smoker` | `SaltsInventoryDesktopApi.findDefinition(...)` returns a definition for `MenuType.SMOKER`. |

### Runtime Suite: `desktop-packets`

| Exact test name | What it verifies |
| --- | --- |
| `packets.menu_type_round_trip.furnace` | `DesktopPackets.menuTypeId(MenuType.FURNACE)` resolves back to `MenuType.FURNACE`. |
| `packets.menu_type_round_trip.crafter` | Same round-trip for `MenuType.CRAFTER_3x3`, when present. |
| `packets.pin_mode_order` | Pin constants are `UNPINNED = 0`, `PINNED = 1`, `GHOST_PINNED = 2`. |
| `packets.quick_target_order` | Quick-move target constants are `DEFAULT = 0`, `SESSION = 1`, `HOTBAR = 2`. |
| `packets.special_kinds` | Special-kind constants are `GENERIC = 0`, `HORSE = 1`. |

### Runtime Suite: `inventory-expansion`

| Exact test name | What it verifies |
| --- | --- |
| `inventory_expansion.clamp_low` | Negative extra slot counts clamp to `0`. |
| `inventory_expansion.clamp_high` | Counts above `HARD_MAX_EXTRA_SLOTS` clamp to `HARD_MAX_EXTRA_SLOTS`. |
| `inventory_expansion.first_cost` | The first extra inventory slot costs `1` level. |
| `inventory_expansion.max_cost` | The cost at `HARD_MAX_EXTRA_SLOTS` is `Integer.MAX_VALUE`. |

## Commands

Run static parity plus compile all versions/loaders with the harness:

```powershell
powershell -ExecutionPolicy Bypass -File .\functional-tests\scripts\Run-FunctionalMatrix.ps1
```

Run only static parity:

```powershell
powershell -ExecutionPolicy Bypass -File .\functional-tests\scripts\Test-SourceFeatureParity.ps1
```

Compile only through Gradle:

```powershell
.\gradlew.bat --no-daemon -PincludeFunctionalTests=true functionalTestCompile --console=plain
```

Launch every client in the full Fabric + Forge/NeoForge matrix and wait for the in-client harness summary:

```powershell
powershell -ExecutionPolicy Bypass -File .\functional-tests\scripts\Run-FunctionalMatrix.ps1 -Runtime -TimeoutSeconds 240
```

Run one client directly:

```powershell
$env:SIU_FUNCTIONAL_TESTS = 'true'
$env:SIU_FUNCTIONAL_TEST_EXIT = 'true'
$env:SIU_FUNCTIONAL_TEST_LOADER = 'neoforge'
.\gradlew.bat --no-daemon -PincludeFunctionalTests=true :1.21.1:neoforge:runClient --console=plain
```

## Known Boundary

This is not a pixel-perfect UI automation suite. It does not replace a small visual pass for rendered layout, mouse dragging, resizing, ghost pins, and manual item movement. It is meant to catch common cross-version and cross-loader breakages quickly before doing any heavier manual pass.

## Adding Coverage

Add version-neutral runtime checks in:

```text
functional-tests/src/main/java/com/salts_inventory_update/functionaltest/FunctionalTestHarness.java
```

Add static source parity checks in:

```text
functional-tests/scripts/Test-SourceFeatureParity.ps1
```

Prefer tests that use public mod APIs, stable Minecraft classes, or reflection over version-specific implementation details. If a feature only exists in newer versions, guard it the same way `CRAFTER_3x3` is guarded.
