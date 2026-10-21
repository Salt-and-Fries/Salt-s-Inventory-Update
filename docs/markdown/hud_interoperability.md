# HUD interoperability â€” Core 0.1.5 / API 6

HUD mods can adapt Salt's detached player-hotbar and offhand overlays through `com.salts_inventory_update.api.client.desktop.DesktopHudApi`. This is an additive public API available in every Core version/loader build. No Compatibility implementation or Core internals are needed to consume it.

## What the API controls

`DesktopHudLayout` contains the nine hotbar slots and offhand, each exactly once. Each `DesktopHudSlot` has a stable kind/index, a `DesktopTransform` (translation and positive uniform scale), and independent visibility and interaction flags. Providers can translate a whole layout, rearrange individual slots into rows or columns, scale slots independently, reorder their paint priority, or hide/disable individual targets. Hidden slots are excluded from pointer hit testing. Visibility and interaction flags do not lock the underlying inventory, prevent automatic transfers, or undo already dispatched actions.

Core uses the same immutable geometry for its offhand frame/item, item redraws, hover highlights, drag previews and pointer hit testing. The inverse of each transform determines its 18-by-18 GUI-unit target, from local coordinate -1 inclusive to 17 exclusive. The item itself occupies local 0 through 16. List order determines painting; hit testing visits slots in reverse. Desktop windows and modal owners retain their existing input precedence.

The API adapts Salt's overlays to another mod's HUD. It does **not** move or replace Minecraft's own HUD, render a new vanilla hotbar background, alter inventory contents, suppress vanilla keyboard shortcuts, or provide arbitrary rotations/shaders. Your HUD renderer remains responsible for its own native HUD changes. A visual-only slot can remain visible with interaction disabled.

Hotbars hosted in inventory or creative windows, character slots and Controlify's alternative workspace use their own window-relative geometry. This API does not move them.

## Registration and live state

Register on the client thread during client initialization, or in a public `DesktopIntegration` CLIENT provider. The returned `RegistrationHandle` can be closed without checked exceptions. Registration inside `DesktopEnvironment.registerIntegration` or the descriptor-based integration lifecycle participates in rollback.

```java
var registration = DesktopHudApi.register("example:my_hud", 0, (context, layout) ->
    layout.translated(myHud.currentXOffset(), myHud.currentYOffset()));

// On removal/reconfiguration of this provider:
registration.close();
```

Owners must be unique namespaced identifiers. Providers run in ascending priority and then lexical owner order; later providers receive the preceding immutable result. Choose a higher priority only when intentionally adapting a previous provider's placement. Register a provider once, rather than on every frame or screen opening.

`DesktopHudContext` supplies the current scaled viewport dimensions and GUI scale. Transform values are already in scaled GUI units: do not multiply offsets by the GUI scale. Core resolves a new layout for each drawing or pointer operation, so read current configuration rather than capturing startup offsets. Return deterministically for the same state and avoid game-state mutations in the callback.

Layouts must retain all ten unique slot identities; use `withVisibility(false)` instead of removing a slot. Transforms must be finite, have a positive scale representable by native graphics, and fit the supported GUI coordinate range. Invalid construction throws an exception. A null result or provider exception preserves the preceding layout, reports a structured `hud-layout` diagnostic once per consecutive failure streak, and allows other providers to continue. The failing provider may recover on its next call.

See the compilable [per-slot example](../../api-examples/hud/src/main/java/example/hud/RearrangedHud.java) for scaling and independent placement.

## Raised adapter

Compatibility 0.1.2 contains a client-only adapter for Raised 6.0.2 and later with the inspected API. It registers `salts_inventory_compat:raised` at priority 0 and reads:

```java
RaisedApi.getX("minecraft:hotbar");
RaisedApi.getY("minecraft:hotbar");
```

Raised calculates the signed total across its configured groups. Its default result is `(0, -2)`. Both axes, multiple groups, position signs, GUI resizing and live configuration changes are respected. There is no separately configurable Salt's Raised layer that could drift away from the native hotbar.

Install Core, Compatibility and Raised for the same Minecraft version/loader. Raised is optional and compile-only for the adapter; no copy of Raised or Core is embedded in Compatibility. Without Raised the adapter is not initialized. Core itself has no Raised dependency. Native Raised selection repair, resource-pack support and other HUD layers remain owned by Raised.

Supported pairs: Fabric/Forge 1.20.1, and Fabric/NeoForge 1.21.1, 1.21.11, 26.1.2, 26.2 and 26.3. Use `-PincludeRaisedRuntime=true` with a Compatibility `runClient` task to opt into the pinned Raised development runtime. Ordinary builds do not install it at runtime.
