# Compatibility changelog

## 0.1.2

- Adds optional Raised 6.0.2 compatibility to every version/loader build using only Core's public HUD layout API. Follows horizontal/vertical offsets and live configuration while preserving native Raised HUD behavior.

- Fixes REI desktop searches crashing on registry-backed tooltips such as researched Ancient Remnants Monolith Codices. Local entry copies supply the current world registry context without hiding entries or changing REI's search syntax. The shared fix covers Fabric and NeoForge from Minecraft 1.21.1 onward; Minecraft 1.20.1 already uses the player-backed tooltip path.

Standalone notes: [Compatibility 0.1.2](../changelogs/changelog-compatibility-0.1.2.md).

- Fixes Minecraft 1.20.1 Tom's Storage client registration when Forge client-startup ordering would otherwise leave terminals outside the desktop. Both 1.20.1 loader builds include the shared fix.
- Updates AE2 controls and all companion artifacts for Core API 6, removing calls to deleted minimize/ghost APIs.

- Requires Core 0.1.5 across all twelve Minecraft/loader targets, matching the Core tutorial fix and Controlify companion 0.1.1.
- Retains the integration implementations and optional upstream dependencies from 0.1.1. The Controlify companion remains optional.

## 0.1.1

- Publishes the rebuilt compatibility coverage update under a new version for all twelve Minecraft/loader targets.
- Includes the Minecraft 1.20.1 EMI integrations, maintained Sophisticated Fabric ports on 1.20.1 and 1.21.1, repaired legacy Tom's Storage integration, native Sophisticated opening-data fixes, and Tom's Storage window bounds and text visibility fixes.
- Requires Core 0.1.4.1 on Minecraft 1.20.1 Fabric and Forge, and Core 0.1.4 on the other supported targets. Third-party integration targets remain optional; Fabric also requires Fabric API.
- Keeps the same validated integration implementations as the rebuilt 0.1.0 artifacts; this publication update changes release numbers and dependency metadata.

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
