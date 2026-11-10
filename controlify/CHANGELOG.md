# Controlify companion changelog

## 0.1.2

- Requires Core 0.1.6 on every supported Minecraft and loader target. Works with the optional Compatibility 0.1.3 extension.
- Marks the controller workspace as a screen with its own recipe interface so compatible recipe browsers can suppress automatic overlays while keeping recipes and search available.
- Fixes resource-pack metadata on Minecraft 1.21.11 Fabric and NeoForge.
- Declares a minimum Mixin version for the companion's hooks, resolving a missing-version startup diagnostic on Forge.

[Shared Modrinth release notes for all twelve jars](../changelogs/changelog-controlify-0.1.2.md).

## 0.1.1

This release requires **Core 0.1.5** on all twelve supported Minecraft and loader targets and works with the optional Compatibility 0.1.2 extension.

### Changes

- Uses Core's alternative-workspace onboarding behavior so the ordinary desktop tutorial no longer interrupts first-world entry with the companion installed, including before a controller connects. Manual help remains available.
- Rebuilds all twelve artifacts against **Core API 6**, without calls to removed Minimize/Ghost Pins members. Keeps the existing controller workspace, inventory interactions, crafting/recipe wheels, native keyboard, preferences, sounds and status-effects tray.

### Validation and installation

The original Core 0.1.5 validation passed **54 controller profiles**: twelve standalone, twelve extension baselines, 26 focused and four native-mod/browser combinations. Real-game checks cover navigation, workstations, recipe wheels, native keyboards, API controls, layouts/animation, first-world onboarding and selected Tom's/AE2/Sophisticated and JEI/REI combinations. The final full-project build passed all **1,637 fresh unit tests**, and those original companion JARs matched their runtime evidence.

Controller gameplay checks use virtual controller input in real Minecraft worlds with server assertions; physical gamepads/haptics and every possible integration combination are not covered. The [full validation report](../functional-tests/FULL-VALIDATION-20261023.md) supersedes earlier metadata-only and focused acceptance records for those original release artifacts and documents exact coverage and diagnostics.

Install the matching Core, this companion, upstream Controlify and its required dependencies. Fabric also requires Fabric API. Core and upstream dependencies are separate downloads; Compatibility remains optional.

Use this same changelog for every Controlify 0.1.1 artifact.

Standalone release notes: [Controlify 0.1.1](../changelogs/changelog-controlify-0.1.1.md).

## 0.1.0

- Initial independently versioned controller companion for all twelve Minecraft/loader targets.
- Provides the controller workspace, native input and keyboard adapters, crafting and recipe wheels, controller preferences, sounds and status-effects tray through Core's public API.
