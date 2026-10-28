# Animal Feeding Trough

Compatibility supports Animal Feeding Trough on all twelve supported Minecraft/loader targets. Install Core **0.1.6**, Compatibility **0.1.3** and the matching upstream dependencies. Reviewed releases are 1.1.0 on 1.20.1, 1.1.2 on 1.21.1, 1.2.0 on 1.21.11 and 1.2.1 on 26.x. See [the all-target matrix and validation record](../docs/markdown/native-containers-all-targets.md).

The native one-slot menu opens automatically as a 200x50 desktop window. The slot is centered beneath Salt's normal title bar, replacing the unnecessarily tall generic forced-container layout. Window size is fixed to its content; movement, pinning, locking, closing, tooltips, in-container sorting and transfers use the existing window framework. Sorting within one slot has no ordering effect, but normal transfer-to-other-inventories controls remain useful.

Native Slot instances and the original menu are retained. The integration adds no food whitelist: items accepted by the upstream inventory remain accepted. The upstream mod owns animal AI, consumption/breeding, the world's fill-level display, XP storage and crouch-empty-hand collection. Native block use runs before the ordinary menu is captured, so XP collection does not become an inventory-opening action.

## Implementation boundary

The providers live in each version's `fabric/src/main/java/com/salts_inventory_update/compat/feedingtrough` source root; Forge/NeoForge compile the shared version sources. Optional initialization is gated on `animal_feeding_trough`, and registration waits until native registries are ready. Missing targets never resolve these providers. Unknown menu layouts keep their native screen.

The providers consume only the existing Core public API: `DesktopEnvironment`, `DesktopServerApi`, `DesktopMenuBindings`, `DesktopClientApi`, and the window render/slot contexts. Only storage slot 0 is declared eligible for sorting and transfer. Player inventory slots are excluded. No Core implementation access, API changes, access wideners, custom networking or copied upstream classes are needed.

One optional mixin targets the upstream menu's always-true validity method. It uses `DesktopSessions.hasOpenSessionMatching` to identify ownership of this exact menu, then applies Minecraft's standard block-entity validity check. Destroyed/replaced sources and sources outside the normal eight-block radius close their desktop session. Native screens outside the desktop keep their upstream behavior. The plugin checks target bytecode without defining optional classes. Version-specific targets follow the upstream menu rename from 1.21.11 onward; legacy builds also handle their intermediary/SRG selectors.

## Original 1.20.1 validation

The production Fabric and Forge JARs each passed 17 focused checks using released Core 0.1.5:

- Automatic capture without a force override; centered 200x50 layout, fitted restored bounds and GUI scale 3 containment.
- Pickup, splitting, single placement, merging, quick transfer and close/reopen with matching client/server contents. Native acceptance of non-food items is preserved.
- Outer-sort transfer of 32 wheat into an open chest containing 10, and shared-cursor transfer of all 42 back into the trough. Closing with a carried stack keeps that stack accessible through Core's player window.
- Real native cow AI consumed trough wheat and produced a calf while the desktop stayed open. The changing inventory stayed synchronized.
- The native trough gathered seven XP while the player was away. Crouch-empty-hand collection released those seven XP to the player, cleared stored XP and opened no menu.
- Source destruction and distance invalidation closed the desktop. Before the guard, destruction reproduced a stale open session on Fabric. Disabling Core still opened the ordinary upstream screen.

Both loaders also passed startup, vanilla-chest opening, sorting and transfer checks with Animal Feeding Trough absent. Production remapping, optional dependency metadata, native mixin registration, API-only references and absence of bundled Core/upstream/test classes passed artifact inspection. Core and Controlify sources and release JARs were verified unchanged.

Detailed observations, screenshots and the before/fixed source-removal case are recorded under `build/feeding-trough-compatibility/`. These are focused integration checks, not a full functional suite. Dedicated multiplayer, full modpacks, every supported animal species and physical controller navigation were not exercised.
