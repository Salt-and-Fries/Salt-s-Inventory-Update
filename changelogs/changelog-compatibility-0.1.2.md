# Salt's Inventory Compatibility 0.1.2

- Adds optional Raised 6.0.2 HUD compatibility on all twelve targets. Hotbar/offhand overlays and interaction follow both axes, combined groups and live changes through Core's public HUD API. Raised's native selection fixes and window-hosted layouts are preserved.

- Fixes REI searches crashing on registry-backed item tooltips, including researched Ancient Remnants Monolith Codices. Desktop entries use the current world's tooltip registry context while preserving REI search syntax, custom renderers and item visibility.
- Fixes Tom's Storage desktop-window registration on Minecraft 1.20.1 Forge when the client startup notification has already fired. The shared initialization fix is included in both 1.20.1 loader builds.
- Updates AE2 window management and artwork rendering for Core API 6, removing Minimize/Restore controls and collapsed-window rendering.
- Rebuilds every version/loader artifact against the reduced Core window API.

- Requires **Salt's Inventory Update Core 0.1.5** on every supported Minecraft and loader target.
- Aligns with Core's first-world tutorial fix and the optional Controlify 0.1.1 companion.
- Retains the integrations and fixes from Compatibility 0.1.1, including supported AE2, Tom's Simple Storage, Sophisticated, JEI, REI, EMI and Fabric Mod Menu adapters.
- Keeps third-party integrations optional and communicates with Core through its public API. The extension does not include Core or third-party mods.

Install Core and Compatibility for the same Minecraft version and loader. Install the third-party mods you use and their dependencies separately. Fabric also requires the matching Fabric API. Controlify remains optional.

Use this same changelog for every Compatibility 0.1.2 Minecraft/loader artifact. Integration availability follows the upstream mod's supported versions and loaders.
