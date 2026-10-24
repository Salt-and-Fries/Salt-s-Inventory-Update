# Salt's Inventory Controlify 0.1.1

This release provides **Controlify companion 0.1.1** for all twelve supported Minecraft/loader targets and requires the matching **Core 0.1.5**, including both 1.20.1 loaders.

## Changes

- Uses Core's alternative-workspace onboarding behavior so the ordinary desktop tutorial no longer interrupts first-world entry with the companion installed, including before a controller connects. Manual help remains available.
- Rebuilds all twelve artifacts against **Core API 6**, without calls to removed Minimize/Ghost Pins members. Keeps the existing controller workspace, inventory interactions, crafting/recipe wheels, native keyboard, preferences, sounds and status-effects tray.
- Aligns the exact Core dependency across every target and supports the optional **Compatibility 0.1.2** extension for the same Minecraft version and loader.
- Centralizes editable controller PNG/PDN artwork under `artwork/shared-resources/controlify`. All targets consume the same resources, the export tools use the shared folder, and existing artwork/resource identifiers are preserved. Editor originals are excluded from runtime JARs.
- Declares the owned mixin hooks' minimum Mixin version, removing the companion's missing-version startup diagnostic on Forge.
- Fixes rejected resource-pack metadata on **1.21.11 Fabric and NeoForge**. Fabric uses current explicit bounds; NeoForge supplies its native client-resource/server-data metadata. Controller gameplay classes and artwork are unchanged by these metadata corrections.
- Produces twelve independent release JARs in `dist/controlify`, with checksums and inclusion in the combined 36-JAR release bundle.

## Validation and installation

Passed **54 current-artifact controller profiles**: twelve standalone, twelve extension baselines, 26 focused and four native-mod/browser combinations. Real-game checks cover navigation, workstations, recipe wheels, native keyboards, API controls, layouts/animation, first-world onboarding and selected Tom's/AE2/Sophisticated and JEI/REI combinations. The final full-project build passed all **1,637 fresh unit tests**, and every final companion JAR matches its runtime evidence.

Controller gameplay checks use virtual controller input in real Minecraft worlds with server assertions; physical gamepads/haptics and every possible integration combination are not covered. The [full validation report](../functional-tests/FULL-VALIDATION-20261023.md) supersedes earlier metadata-only and focused acceptance records for the current release artifacts and documents exact coverage and diagnostics.

Install the matching Core, this companion, upstream Controlify and its required dependencies. Fabric also requires Fabric API. Core and upstream dependencies are separate downloads; Compatibility remains optional.

Use this same changelog for every Controlify 0.1.1 artifact.
