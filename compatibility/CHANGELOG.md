# Compatibility changelog

## 0.1.0

- Packages third-party inventory and recipe-browser integrations as an optional companion to Salt's Inventory Update Core 0.1.4.
- Integrates supported releases of Applied Energistics 2, Tom's Simple Storage, Sophisticated Backpacks and Storage, JEI, REI, EMI and Fabric Mod Menu through the public Core API.
- Adds EMI support for Minecraft 1.20.1 on Fabric and Forge, alongside the existing 1.21.1 Fabric and NeoForge adapters.
- Adds Sophisticated's maintained Fabric ports on Minecraft 1.20.1 and 1.21.1, including native slot behavior, upgrade tabs, settings navigation, menu opening data and synchronization.
- Checks remapped Fabric slot-rendering hooks so the legacy native override remains active in production JARs.
- Repairs Tom's Storage 1.7.x support on Minecraft 1.20.1, including its native registry IDs, terminal actions and settings, seven available menus, filtered connectors, emitter and link state, recipe book and legacy texture atlases.
- Preserves the full native Sophisticated Storage opening payload, including newer linked-storage state, while deriving source identity from the authenticated server menu.
- Keeps emitter controls, inventory-link lists and tag-filter panels within their window frames, including minimum bounds for restored windows, across every target.
- Uses explicit opaque text colors for Tom's Storage labels, counts and controls so newer renderers do not make them transparent.
- Audits upstream availability across all twelve Minecraft/loader targets and checks that eligible adapters are present in source and production artifacts.
- Keeps third-party integrations optional and the Core framework independent.

Use the rebuilt Core 0.1.4 and Compatibility 0.1.0 pair for the same Minecraft version and loader; the 1.20.1 EMI adapter uses the added public Core rendering hooks. Fabric also requires Fabric API. Install third-party mods and their required dependencies separately. Availability follows the upstream release matrix; an extension JAR does not imply that upstream mods exist for every loader and Minecraft version.
