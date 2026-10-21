# Raised compatibility analysis

Analysis date: 2026-10-21. This is a source and published-artifact audit, not an implemented fix or a gameplay validation report.

Implementation follow-up: see the [public HUD API guide](hud_interoperability.md) and [executed focused validation](../../functional-tests/RAISED-HUD-VALIDATION.md). The original analysis below is retained as the pre-implementation record.

## Finding

Raised moves Minecraft's native hotbar HUD. Salt's desktop screen separately paints hotbar interaction overlays and an offhand slot, and calculates their mouse targets from the unmodified screen size. These two coordinate systems disagree when Raised offsets the hotbar.

This explains the hotbar alignment problem described in [issue #12](https://github.com/Salt-and-Fries/Salt-s-Inventory-Update/issues/12). The mismatch is confirmed in source across all twelve Core targets; its precise appearance and interaction effects have not been reproduced in game during this audit.

## Evidence reviewed

- Raised's [README](https://github.com/yurisuika/Raised), [compatibility documentation](https://github.com/yurisuika/Raised/wiki/Compatibility), [configuration documentation](https://github.com/yurisuika/Raised/wiki/Config), [commands](https://github.com/yurisuika/Raised/wiki/Commands), and [keybindings](https://github.com/yurisuika/Raised/wiki/Keybindings).
- Raised source at commit `50208b7a05472f9df2c370df1fd03991dc57cc64`, identifying itself as 6.0.2; wiki at `76b27dad92750e51ca7a141b99b997680dd1b989`.
- The twelve corresponding official Raised 6.0.2 releases from [Modrinth](https://modrinth.com/mod/raised/versions). Each downloaded JAR's SHA-512 matched the published metadata. `javap` confirmed the public String-based X/Y methods in every binary. Binary API presence does not establish gameplay compatibility.
- Core's six version-specific desktop implementations, HUD mixins, hotbar visibility policy, pointer ownership, Mouse Tweaks bridge, public window/workspace APIs, shared geometry, and Gradle source sharing.
- Compatibility's optional initialization and API boundary, plus Controlify's workspace presentation and `forceInventoryHud` usage.

Reproducible audit inputs are under ignored `build/raised-analysis/`: upstream and wiki checkouts, official release metadata, `inputs.json`, downloaded JARs, per-JAR `javap` output, `binary-api-checks.json`, and `published-metadata.json`. These downloaded Raised JARs were inspected, not launched or distributed.

## Supported target matrix

Raised 6.0.2 is published for every current Salt's target:

| Minecraft | Salt's / Raised loaders |
| --- | --- |
| 1.20.1 | Fabric, Forge |
| 1.21.1 | Fabric, NeoForge |
| 1.21.11 | Fabric, NeoForge |
| 26.1.2 | Fabric, NeoForge |
| 26.2 | Fabric, NeoForge |
| 26.3 | Fabric, NeoForge |

Core's Forge and NeoForge builds reuse `versions/<Minecraft>/fabric/src/main/java` through `SourceSet.addLoaderSourceDirs` in the root build. Correcting a version's shared desktop implementation therefore applies to both loaders. Loader-specific initialization, optional dependencies, metadata and runtime behavior still need to be checked for each artifact.

## Raised's contract

Raised's public API provides:

```java
RaisedApi.getX("minecraft:hotbar");
RaisedApi.getY("minecraft:hotbar");
```

Both methods have the descriptor `(Ljava/lang/String;)I` in all twelve inspected releases. They return final, signed offsets in GUI coordinates, summed across every configured group containing the layer. Position settings determine the signs. The default group's Y value is 2 with a BOTTOM position, so the actual API result is **-2**, moving the hotbar upward.

Consequences for the adapter:

1. Consume both X and Y. A fixed two-pixel correction only handles the default configuration.
2. Use the API result directly; do not negate it, multiply it by the GUI scale, parse Raised's JSON, or read private config fields.
3. Resolve current values during rendering and input handling. Raised's screen, commands and config reload can change offsets, membership and positions while running. A value cached only at startup becomes wrong.
4. Resolve one placement snapshot for an operation, then use it for all of that operation's slot rectangles and drawing.
5. Follow the existing `minecraft:hotbar` layer. Registering a separately configurable Salt's overlay layer would let interaction visuals drift away from the native hotbar they describe.

The relevant upstream implementations are `api/RaisedApi.java`, `util/Translate.java`, `option/Options.java`, `client/gui/layer/Layer.java`, and `client/gui/layer/Layers.java` under each target's `src/main/java/dev/yurisuika/raised/`.

## Exact Core adaptation points

All rows below refer to `versions/<Minecraft>/fabric/src/main/java/com/salts_inventory_update/client/InventoryDesktopScreen.java`. Line numbers are the working tree at analysis time.

| Minecraft | `hotbarSlotX` / `hotbarY` | `renderDesktopHotbarAffordances` | `hotbarSlotAt` / `offhandSlotAt` |
| --- | --- | --- | --- |
| 1.20.1 | 18180 / 18205 | 17861 | 17921 / 17937 |
| 1.21.1 | 18240 / 18265 | 17921 | 17981 / 17997 |
| 1.21.11 | 18221 / 18246 | 17885 | 17945 / 17961 |
| 26.1.2 | 18315 / 18340 | 17979 | 18039 / 18055 |
| 26.2 | 18315 / 18340 | 17979 | 18039 / 18055 |
| 26.3 | 18315 / 18340 | 17979 | 18039 / 18055 |

The listed coordinate, hit-test and overlay methods have identical normalized bodies across the six sources, allowing for the modern graphics type name.

### 1. Detached hotbar placement

Current item origins are:

```text
x = guiWidth / 2 - 88 + slotIndex * 20
y = guiHeight - 19
```

The required placement when Raised is active is:

```text
x = baseX + RaisedApi.getX("minecraft:hotbar")
y = baseY + RaisedApi.getY("minecraft:hotbar")
```

The detached offhand origin is derived from the first hotbar slot and must receive the same translation exactly once. Keep the existing spacing and slot dimensions; Raised translates this layout rather than changing its spacing or scale.

### 2. Salt's hotbar/offhand drawing

`renderDesktopHotbarAffordances` and `renderHotbarOverlaySlot` paint the offhand frame and item, hover highlights, drag-distribution previews, item counts and hovered hotbar item redraws. Every element must use the resolved placement. Otherwise Raised can move the underlying item while Salt's redraw remains at its former location, creating apparent duplicate or displaced items.

These are screen-rendered overlays, outside Raised's native HUD scopes. Translating only the vanilla HUD is insufficient. Translating the entire desktop screen would incorrectly move all ordinary windows and menus.

### 3. Mouse targets and input routing

Use the same resolved geometry in:

- `hotbarSlotAt` and `offhandSlotAt`, including their one-pixel hitbox inset and 18-unit target size;
- `slotAt` when it constructs the detached hotbar/offhand `SlotHit`, so the returned origin agrees with the hit test;
- `isPointOverInteractiveUi`, through those corrected hit tests, so cursor/world interaction ownership follows the displayed HUD;
- item hover/tooltips, pending clicks, drag previews, quick moves and other existing consumers of `slotAt`;
- Mouse Tweaks' `mouseTweaksSlotAt` and its slot proxies, which already route through Core's slot lookup. Check that the bridge retains the corrected visible coordinates throughout a gesture.

Changing render matrices alone cannot fix input coordinates. Conversely, changing only hitboxes leaves incorrect highlights and previews. Existing window/modal occlusion must continue to take precedence over a hotbar positioned behind a window.

### 4. Live changes and coordinate units

Use scaled GUI coordinates consistently for render and pointer input. Changes to GUI scale, window size or Raised's config must affect the next resolved placement. Handle arbitrary signed X/Y values and combinations of groups. Preserve Raised's supplied location rather than independently clamping Salt's overlays to a different screen position.

## Rendering that Raised already handles

On Fabric 1.20.1, Raised brackets individual native HUD calls inside `Gui.render`. On Fabric 1.21.1 and 1.21.11 it brackets `renderHotbarAndDecorations` (and additional HUD methods where necessary). The 26.x code uses the extraction equivalents; 26.2/26.3 target `Hud` instead of `Gui`.

On Forge 1.20.1 and NeoForge, Raised curates native overlay/layer identifiers into `minecraft:hotbar` and other logical layers. Its event handlers push/translate on Pre and pop on Post or canceled Pre. The mapped hotbar group includes native health, armor, food, air, mount/XP decorations and selected-item text as appropriate to the version.

Do not apply a second offset to these native HUD calls. Salt's existing hotbar cancellation hooks must remain compatible with Raised's balanced translation scopes when the hotbar is hosted in an inventory window.

Raised also owns the native selection-indicator repair:

- 1.20.1: expands the existing selector draw height to 24.
- 1.21.1 and later supported targets: AUTO, REPLACE, PATCH or NONE, including resource-pack detection and reload.

Salt's detached overlays do not draw that selector. Reuse the already-rendered native selector and leave Raised's texture choice and repair modes intact; no copied Raised assets or replacement selector implementation are required for this fix.

## Window-hosted content and Controlify

`inventoryHotbarX/Y` and `creativeHotbarX/Y` are relative to their owning windows. Character slots, stats and effects are also window content. They should not inherit a `minecraft:hotbar` HUD offset.

`usesInventoryWindowHotbar` already separates detached interaction from a window-hosted hotbar. The `returnHotbarToInventory` setting and Controlify's `DesktopWorkspace.forceInventoryHud(true)` use this separate path. Keep the offset scoped to detached HUD placement; do not apply it to workspace transforms, controller cursor snapping or previews of window-hosted slots.

No Raised-specific Controlify code change is indicated by the reviewed call paths. Controller mode, keyboard mode and transitions between them still need focused runtime verification.

The fixed left-side detached offhand frame is an existing Salt's layout choice, not behavior introduced by Raised. The adapter must translate it consistently. Dominant-hand differences and native offhand visibility should be included in visual checks rather than assumed to be new Raised defects.

## API boundary and packaging recommendation

The geometry correction belongs in Core, which owns rendering and hit testing. The existing public APIs describe windows and alternate workspace presentation; they do not provide a supported way to adjust Core's detached HUD anchor. `DesktopWindowSurface`, `DesktopTransform` and `DesktopWorkspaceHost` do not supply that missing hook.

Following the current Core/Compatibility split, add a small, general public client API for named HUD-placement providers, with a player-hotbar anchor shared by detached hotbar/offhand drawing and input. A provider supplies a live GUI-coordinate offset; identity placement is the default. Registration should follow existing owner/conflict/rollback conventions. This would also support other HUD-positioning mods without exposing `InventoryDesktopScreen` or its private methods.

Put the Raised-specific consumer of that API in Compatibility, in its own `compat.raised` package:

- initialize only on the client and only when mod ID `raised` is present;
- call Raised's public `RaisedApi`, then register through Salt's public API;
- compile against the matching Raised artifact without embedding it;
- keep Raised optional in loader metadata and keep failure of this integration isolated;
- do not use reflection, access wideners or mixins to change Core internals.

This is a proposed API addition, not an existing method or an implemented integration. If a direct Core + Raised pairing is desired without Compatibility, the Raised-specific bridge would instead need an explicit Core interoperability exception like the previously authorized Mouse Tweaks bridge. The shared geometry requirements are the same in either packaging choice.

## Focused validation needed after implementation

No build or gameplay suite was run for this analysis. Published-JAR hash, API-descriptor and source comparisons were the only checks executed.

When implementing, focus checks on:

1. Core alone; Core + Compatibility without Raised; Core + Compatibility + Raised, across all twelve supported loader/version pairs.
2. Raised's default upward offset, zero offset, horizontal movement, negative values, changed position signs and multiple groups.
3. Live config/command changes, GUI scaling and resizing, including changes while the desktop is open.
4. All nine hotbar slots and offhand: empty/populated, hover, tooltip, pickup/place, quick move, left/right drag and item-count previews.
5. Mouse Tweaks wheel and drag operations between the moved hotbar and multiple native/API windows, when Mouse Tweaks is installed.
6. A window covering the moved hotbar, pointer/world ownership at the old and new positions, hotbar-only mode and movement/camera control.
7. Return Hotbar to Inventory, creative inventory, Controlify's window-hosted hotbar and keyboard/controller transitions.
8. Selection repair and resource-pack reload: legacy 24-unit draw and modern AUTO/REPLACE/PATCH/NONE. Check native HUD decorations for double translation and cancellation-related matrix problems.

These are pending checks, not passed scenarios. A complete unrelated gameplay suite is outside this audit's scope.
