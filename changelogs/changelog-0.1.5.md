# Salt's Inventory Update Core 0.1.5

This release applies to all twelve supported Minecraft/loader targets: Fabric and Forge on 1.20.1, and Fabric and NeoForge on 1.21.1, 1.21.11, 26.1.2, 26.2 and 26.3. Every Core JAR uses version **0.1.5**.

## Inventory windows and controls

- Adds **Sort this inventory** beside **Sort into open containers**, using the former minimize sprite location. Sorting merges compatible stacks and groups valuables, equipment, supplies, crafting/utility items and building materials, with bulk terrain last. Existing tags, item properties and creative-menu order cover modded items; datapacks can override categories. Stack components/NBT are preserved, and hotbar, equipment, crafting and machine slots are excluded. Older servers hide unsupported sorting through channel/capability negotiation, including the separate 1.20.1 capability check.
- Keeps the entire window inside all four desktop edges during dragging, resizing and resolution/GUI-scale changes. Windows can no longer hang halfway beyond the bottom or right edge.
- Matches Character's slot, player-preview and recipe-book positions to vanilla inventory across all targets. Removes the Health/Hunger/XP text and unused height, keeps a small bottom gap, and adds one editable 16 x 16 compact crafting-arrow sprite. Offhand visibility follows **Return Hotbar to Inventory** unless a public attachment requests that it remain visible.
- Fixes the recipe-book button requiring two clicks after closing and reopening a window with its recipe book open.
- Fixes physical chest lids staying open when Persistent Windows hides their windows. Restoring reopens the viewer; disposal releases it once. Persistent Windows, normal pins and saved positions remain supported.
- Removes Minimize, Ghost Pins and ghost opacity completely, including controls, settings, rendering, server state and public API members.
- Adds a shared **Hold / Toggle** setting for Window Movement Control in both the mod settings and Minecraft Controls. Hold remains the default; toggle state clears when leaving the workspace.
- Makes **Window Movement Control** rebindable in Minecraft Key Binds under **Salt's Inventory Update**, alongside the character and recipe-browser bindings. Left Alt remains the default, existing custom bindings are preserved, and the mod settings retain a Key Binds shortcut. The Controls screen keeps the Hold/Toggle selector without a duplicate movement-key button.
- Makes hold-to-close follow the player's vanilla Inventory binding, including keyboard and mouse remappings. Help and hold-duration labels reflect that binding instead of hardcoding E.
- Registers Core bindings during Forge/NeoForge's native key-mapping collection so they appear correctly in Minecraft Key Binds.

## Hotbar, HUD and movement

- With **Return Hotbar to Inventory** enabled and windows open, hides the native hotbar, health, armor, hunger, air, experience, mount indicators and selected-item label. Window Movement Control temporarily restores the hotbar and indicators and allows held-item selection; closing windows or disabling the option also restores them.
- Locks wheel and number-key hotbar selection while that hotbar is hidden and movement control is inactive. The behavior follows the configured movement binding and Hold/Toggle mode.
- Fixes NeoForge startup by applying vanilla grouped HUD mixins only on Fabric; Forge/NeoForge use their native overlay/layer hooks.
- Corrects NeoForge screen detection when no screen is open, avoiding repeated failed reflection lookups during ordinary movement.

## Optional Mouse Tweaks and Spatial GUI support

- Adds Mouse Tweaks interoperability directly to Core on all twelve targets. Core plus the matching upstream Mouse Tweaks JAR enables left-drag collection, Shift-drag transfers, right-drag placement and wheel transfers without the Compatibility extension. Mouse Tweaks' own settings remain authoritative.
- Includes real inventory slots exposed by public-API windows in Mouse Tweaks interactions across multiple windows. Virtual/filter, protected and covered slots retain their interaction rules. A wheel burst stops when a crafting/result slot runs out, preventing later clicks from picking up unrelated player items.
- Adds experimental optional **Spatial GUI 1.8** support on every supported target. The ordinary desktop becomes one 3D panel with the same fixed canvas, aspect ratio, window coordinates and four walls. Dragging cannot enlarge the canvas or progressively shrink windows.
- Coordinates projected UI input, physical-pointer world targeting, player reach/obstruction checks and Window Movement Control. Movement control smoothly returns to the normal first-person desktop and native camera input; releasing it returns to the spatial panel. Avatar clearance, detached hotbar/HUD placement and carried-item/count feedback follow the panel.
- Smooths inventory opening, movement-control release and closing all windows. Interrupted transitions, rapid reversals and reopening during closing continue from the visible pose; closing restores the saved gameplay perspective.
- Keeps Spatial GUI client-only, optional and inactive when absent. Servers need only the matching Core; no Compatibility extension or server-side Spatial GUI installation is required, and the existing server protocol is unchanged.

## Public API and integration infrastructure

- Moves the public API to **version 6** after removing Minimize/Ghost Pins. Consumers must recompile and update their provider descriptors for the reduced window options/state.
- Adds reusable HUD layout providers for live whole-layout and per-slot placement, scaling, visibility and interaction control. Detached hotbar/offhand rendering and pointer targets share the resolved geometry, including Mouse Tweaks lookup. Window-hosted, creative and Controlify layouts remain independent.
- Adds built-in Character attachments, foreground popup passes above the player preview, real offhand visibility requests and native equipment transfer/topology hooks. Dynamic slot snapshots reconcile capacity changes, preserve expanded storage and cancel stale gestures; the legacy click transport discards gestures from obsolete layouts.
- Adds built-in control contributions and equal-width joined upper menu windows with shared movement, bounds and lifetime, plus viewport fitting and scrolling. Joined layouts, desktop-availability and menu-capacity queries are available across all supported mappings. Existing legacy Trinkets attachment callbacks remain supported.
- Keeps native Trinkets, Curios and Raised implementations in Compatibility; these integrations consume the same supported public API as other mods.

## Configuration, artwork and packaging

- Makes Fabric Mod Menu's Core Config button work independently of Compatibility on every supported version. Forge and NeoForge retain their native config factories; Mod Menu remains optional.
- Suppresses the regular-window tutorial when an alternative workspace presentation is registered, including Controlify before a controller connects. Manual help remains available, the unseen tutorial preference is preserved, and ordinary Core retains its automatic tutorial.
- Centralizes Core textures under `artwork/shared-resources/core` and provides separate shared artwork inputs for Compatibility and Controlify. Builds track edits across versions/loaders without changing existing resource identifiers. Native third-party designs remain version-specific.
- Fixes rejected Core pack metadata on 1.21.11: Fabric uses current explicit format bounds, and NeoForge synthesizes client-resource/server-data metadata. Artifact checks enforce the loader-specific policies.
- Produces twelve Core JARs in `dist/core`, alongside the independently versioned companion outputs, checksum files and the 36-JAR bundle. Production checks exclude QA, editor and intermediate artifacts.

## Validation and installation

The final full-project build passed **1,637 fresh unit tests in 224 suites**, with zero failures, errors or skips. Across all three mods, validation passed **220 client gameplay/UI profiles, 58 dedicated-server profiles and twelve source-bootstrap smoke profiles**, plus three independent physical-input/multiplayer profiles. All 36 final JAR hashes match their accepted runtime evidence. The [full validation report](../functional-tests/FULL-VALIDATION-20261023.md) records fixes, executed scenarios, reviewed diagnostics and coverage limits.

Core works independently. Optional **Compatibility 0.1.2** and **Controlify 0.1.1** require Core **0.1.5** for the same Minecraft version and loader. Fabric also requires the matching Fabric API.

Use this same changelog for every Core 0.1.5 artifact.
