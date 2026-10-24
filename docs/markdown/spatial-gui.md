# Spatial GUI integration

Core 0.1.5 includes the optional client integration for **Spatial GUI 1.8** across every supported Core Minecraft/loader target. The Compatibility and Controlify JARs do not provide or require this integration.

| Minecraft | Loaders |
| --- | --- |
| 1.20.1 | Fabric, Forge |
| 1.21.1 | Fabric, NeoForge |
| 1.21.11 | Fabric, NeoForge |
| 26.1.2 | Fabric, NeoForge |
| 26.2 | Fabric, NeoForge |
| 26.3 | Fabric, NeoForge |

Install the matching Spatial GUI file and its declared dependencies. Spatial GUI 1.8 requires Fabric Loader 0.19.3 or newer for its Fabric files, and NeoForge 21.1.240 or newer on Minecraft 1.21.1. Core's own loader requirements still apply. The immutable release IDs and upstream SHA-512 hashes are recorded in [the target manifest](../../gradle/spatial-gui-targets.json).

Install matching Core and Spatial GUI files and their dependencies on the client. The Compatibility extension is not required. Servers need the matching Core and its ordinary loader dependencies, including Fabric API on Fabric. Spatial GUI and its client dependencies are unnecessary on a dedicated server.

## Presentation and controls

The existing Salt's desktop becomes one Spatial GUI plane, or its optional curved first-person surface. All windows share it, retaining their dragging, resizing, focus, slots, item transfers and server sessions. The desktop keeps one coordinate system and its aspect ratio across camera modes. The whole panel is uniformly fitted to the visible camera viewport, with margins and an expanded avatar exclusion column in third person. This fitting can reduce the requested Spatial GUI scale and shift the panel; it does not edit upstream configuration or rearrange individual windows.

The fully open panel footprint is fitted before applying Spatial GUI's opening/closing scale animation. The flat corners or all curved-mesh vertices and the corresponding mouse-hit basis use the same fitted transform. Spatial GUI still owns ordinary screen-opening animation, spatial camera targets, parallax and cosmetic avatar pose. The integration owns movement-control transitions between that presentation and the normal desktop. At extreme aspect ratios, camera positions or GUI settings, fitting a complete desktop can make it smaller; it does not promise a minimum physical text size.

The logical desktop always has the normal GUI viewport's dimensions and aspect ratio. It never grows in response to window dragging, and there is no content-driven capture scale. All four sides are inner walls. Core clamps the entire window during dragging/resizing and recovers its bounds on resolution or GUI-scale changes; that baseline change applies to every Core version and loader. Alt transitions do not move or resize individual windows. A window against a spatial desktop wall therefore meets the corresponding physical screen edge in the settled normal presentation.

A workspace containing container or other non-player windows uses Spatial GUI's container settings; a player/character-only workspace uses its inventory settings. Explicit exclusions and perspective exclusions are respected. The screen identifier is `com.salts_inventory_update.client.InventoryDesktopScreen`.

Window input uses the projected cursor once. Moving off the panel keeps it open. Out-of-panel input cannot accidentally click a slot/control at the texture's clamped edge. Captured geometry drags can finish at the panel boundary.

Exposed world space uses the physical pointer and displayed camera angles/FOV, while respecting player reach and an unobstructed eye-to-target trace. Spatial GUI changes the camera angles after vanilla caches its near-plane vectors, so the integration computes its world ray from the displayed angles. Seeing around an obstacle does not grant interaction through it. Carried-item outside-drop handling takes precedence over world actions.

**Window Movement Control** keeps its configured binding (Left Alt by default) and Hold/Toggle setting. In this Spatial GUI experiment it transitions to the normal first-person camera. The mouse turns the player, and attack/use operate through the crosshair. The desktop remains open with interaction paused, expanding into the same screen-aligned rectangle as the normal desktop. Camera and curvature use reversible elapsed-time transitions; the panel smoothly follows its projected position and size, including while the camera is moving. Returning restores the spatial presentation around the player's current facing. Native relative mouse callbacks bypass Spatial GUI's projected-coordinate mapping during movement control.

With **Return Hotbar to Inventory** enabled, Core's window-hosted hotbar, HUD hiding and hidden-slot selection lock remain intact; movement control restores the native HUD and held-slot selection. With the option disabled, the detached hotbar and its indicators use a compact dock below the window cluster, bounded by the logical desktop. Rendering and slot targets receive the same dock offset and canvas scale. Windows retain their existing paint priority over the dock. During movement control the native HUD returns to its normal screen position. Spatial GUI's explicit HUD/hotbar hiding options remain effective in desktop mode.

The shared carried stack and count are drawn once in the main screen HUD at the physical cursor, inset from the screen edges. They remain readable when the pointer leaves the 3D panel, rather than being cut by the captured texture boundary. Core still owns all carried-item interactions, server synchronization and outside-drop decisions.

## Isolation and server behavior

`core-integrations/spatial-gui/` supplies the shared implementation. `tools/generate-spatial-sources.py` adapts Minecraft method names, input signatures, graphics wrappers and loader-specific HUD composition into each Core target's build directory. Projection and transition algorithms remain shared. Each target compiles against its pinned upstream release; the dependency is compile-only and is not bundled. Optional client mixins activate for the tested Spatial GUI 1.8 series. Other upstream versions produce a warning and do not apply these version-specific hooks.

There are no public Core API or network-protocol changes. Core-only and Spatial GUI clients use the existing Core server protocol. Dedicated servers never load the integration's client classes. Absent/disabled Spatial GUI retains the normal Core presentation and input paths. Fabric checks the loader's mod metadata; Forge and NeoForge check their early loading metadata. All hooks stay in client mixin lists. Native HUD capture uses Forge overlays or NeoForge layers, with a scoped filter that captures the hotbar group and leaves other HUD layers in their ordinary pass.

The integration does not edit Spatial GUI's configuration or require a fork. It targets the published JAR, rather than APIs added only on the upstream development branch. Alternative Controlify presentations, shaders, other camera mods, and future Spatial GUI versions require separate validation.

## Focused QA

The original 26.3 Fabric [transition validation](../../functional-tests/SPATIAL-TRANSITIONS-VALIDATION.md) covers opening, movement-control release, closing and interrupted transitions. The panel retains its last displayed pose through these changes, follows fitted screen position and size with critically damped motion, and keeps the same placement while the captured texture closes. Projection normalization prevents camera depth/FOV changes from accelerating the panel across the screen. Closing camera interpolation returns to the saved gameplay perspective. These adjustments are shared by all twelve optional integration targets. The GLFW mouse adapter wraps outside Spatial GUI's coordinate remapping, and movement control prevents spatial frame updates from replacing native mouse positions.

See [the port validation report](../../functional-tests/SPATIAL-PORTS-VALIDATION.md) for the target matrix, executed checks and limits, and [the original report](../../functional-tests/SPATIAL-GUI-VALIDATION.md) for the initial 26.3 Fabric experiment. `functional-tests/scripts/Run-SpatialGuiQa.ps1` loads production Core JARs with a separate disposable QA mod on Fabric. Forge/NeoForge use the source-classpath gameplay launcher with QA-only instrumentation. Both use unmodified upstream files. The dedicated-server fixture contains only production Core and the normal Fabric runtime and binds to localhost. These checks are specific to the affected features, not a complete gameplay suite.
