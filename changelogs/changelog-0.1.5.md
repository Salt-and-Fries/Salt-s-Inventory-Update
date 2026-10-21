# Salt's Inventory Update Core 0.1.5

- Fixes NeoForge startup by loading vanilla grouped HUD hooks only on Fabric. Forge and NeoForge use their native overlay/layer visibility hooks.

- While **Return Hotbar to Inventory** is enabled, Window Movement Control temporarily restores the native hotbar and its indicators and allows held-slot selection. When movement control is inactive, wheel and hotbar-key selection stay locked to the current slot. Uses the configured binding and Hold/Toggle mode across all twelve Core targets.

- Hides health, armor, hunger, air, experience, mount indicators and the selected-item label with the hotbar while windows are open and **Return Hotbar to Inventory** is enabled, across all versions and loaders. The HUD returns during Window Movement Control, when windows close, or when the option is disabled.

- Adds public HUD layout providers for live whole-layout and per-slot placement, scaling, visibility and interaction control. Detached hotbar/offhand rendering and mouse targets share the resolved geometry, including Mouse Tweaks slot lookup. Window-hosted inventory, creative and Controlify layouts remain independent.

This is the shared development release for all twelve supported Minecraft and loader targets. These notes cover the changes implemented so far.

- Adds Mouse Tweaks support directly to Core on all supported Minecraft versions and loaders. Matching Core + Mouse Tweaks JARs enable left-drag collection, Shift-drag transfers, right-drag placement and wheel transfers without the Compatibility extension. Public-API windows automatically participate through their real slot geometry and menu bindings; virtual/filter slots remain excluded from item transfers. Mouse Tweaks' configuration remains authoritative.
- Fixes Mouse Tweaks wheel bursts continuing after a crafting/result slot runs out and moving an unrelated player stack onto the cursor.
- Fixes physical chest lids remaining open when Persistent Windows hides their windows. Restoring a hidden window reopens its viewer, and disposing it releases that viewer once.
- Fixes recipe-book buttons needing two clicks after closing a window with its recipe book open and reopening it.
- Adds a shared **Hold / Toggle** choice for Window Movement Control in both the Salt's Inventory settings and Minecraft's Controls screen. Hold remains the default; Toggle clears when leaving the window workspace.
- Names the existing rebindable action **Window Movement Control**, keeps Left Alt as its default, preserves existing custom bindings, and groups it with the mod's character and recipe-browser bindings in a **Salt's Inventory Update** section of Minecraft Key Binds. The mod settings retain a Key Binds shortcut; the Controls screen contains only the shared Hold/Toggle selector.
- Makes hold-to-close follow the vanilla Inventory binding for both keyboard and mouse remappings. Help reflects the selected binding, and the hold-duration setting uses Inventory wording instead of a fixed E label. Persistent Windows behavior is preserved.
- Registers the Core bindings during Forge/NeoForge's native key-mapping collection, so they appear in Key Binds and participate in vanilla binding settings instead of being created too late.
- Removes window minimization, ghost pins and ghost opacity, including their controls, settings, rendering, server state and public API.
- Moves the public integration API to version 6. Integrations must recompile for the reduced window options/state and update their provider descriptors. Normal pinning, saved positions and Persistent Windows remain available.

- Uses **0.1.5** for every Core JAR, including Minecraft 1.20.1 Fabric and Forge.
- Prevents the regular-window tutorial from opening automatically when an alternative workspace presentation is registered, including Salt's Inventory Controlify. This also applies before a controller is connected.
- Keeps manual help available and preserves the normal automatic tutorial when no alternative workspace presentation is registered.
- Leaves the tutorial preference unseen when the automatic popup is suppressed.
- Uses the existing public workspace-host API for onboarding ownership, without requiring Core to depend on Controlify or Compatibility.

Core works independently. Optional **Compatibility 0.1.2** and **Controlify 0.1.1** builds require Core **0.1.5** for the same Minecraft version and loader. Fabric also requires the matching Fabric API.

Use this same changelog for every Core 0.1.5 Minecraft/loader artifact. Add further implemented 0.1.5 changes here as development continues.
