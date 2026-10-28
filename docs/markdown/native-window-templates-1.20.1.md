# Native window templates — Core 0.1.6

This additive API is available on **all twelve supported Minecraft/loader targets**: 1.20.1 Fabric/Forge and 1.21.1, 1.21.11, 26.1.2, 26.2 and 26.3 Fabric/NeoForge. It lets an integration start with the original mod screen, keep selected pieces, rearrange them, and add ordinary SIU API controls. No extracted textures or copied native renderer are needed. The historical filename is retained for existing documentation links.

Use the matching Core 0.1.6 API artifact for your Minecraft version and loader. Input events use SIU wrappers on 1.20.1/1.21.1 and Minecraft's native event types on later versions. `verifyNativeTemplateExample` compiles the example on all twelve targets.

## Choosing an integration style

- Leave conventional screens unregistered to use automatic upper-container hosting.
- Implement `DesktopNativeTemplate<Menu, State>` when the native screen is mostly suitable but needs cropping, cutouts, rearrangement, padding or extra controls.
- Continue using `DesktopWindowDefinition` for a fully custom interface. Existing definitions keep priority and never enter the native capture path unless they explicitly implement `DesktopNativeTemplate`.

Core owns the screen host and geometry engine. Compatibility and other mods consume only `com.salts_inventory_update.api.*`. Native templates are client presentation code; normal menu construction, server permissions, opening data and custom-packet requirements still apply. A template does not make an unsafe custom network protocol work automatically.

## A small template

Register your original menu and screen normally. Register the template from your optional SIU client provider, after your menu types exist:

```java
DesktopClientApi.registerWindow(MY_MENU, new MyTemplate());

final class MyTemplate implements DesktopNativeTemplate<MyMenu, MyState> {
    @Override
    public MyState createState(DesktopWindowSetupContext<MyMenu> context) {
        return new MyState();
    }

    @Override
    public DesktopNativeLayout nativeLayout(DesktopNativeContext<MyMenu, MyState> context) {
        var view = context.nativeView();
        var output = view.slotBounds(2).orElseThrow();
        return DesktopNativeLayout.builder()
            .inherit(view.automaticBounds())
            .padding(4)
            .move("output", output, 170, 0)
            .canvas(200, 100)
            .build();
    }
}
```

Include the surrounding slot artwork in the moved source rectangle when the native screen uses a larger frame. `slotBounds` supplies the 18 × 18 slot cell, not an arbitrary surrounding decorative frame. The [compiled example](../../api-examples/native-template/src/main/java/example/nativewindow/FurnaceNativeTemplate.java) demonstrates this distinction, a moved native recipe button, an added button and consumer-owned state. Its menu parameter is a furnace-like **modded** menu; vanilla furnace windows continue using Core's built-in implementation.

For a family of menus, use `DesktopClientApi.registerNativeMatcher(id, priority, declaredMenuTypes, predicate, factory)`. This reuses the normal matcher ordering and registration rollback, while advertising the explicitly described menu family. Use this helper for native templates returned by matchers; ordinary `registerMatcher` declarations do not announce that a nonstandard layout has a native template. A direct registered definition takes precedence over matchers, which take precedence over JSON profiles.

## Geometry and anchors

`DesktopNativeView` is an immutable snapshot with native image bounds, inferred automatic bounds, real menu slot IDs/cells, and public widget descriptors. Widget descriptors expose a local ID, class name, label, bounds and visibility/activity. They expose no native screen or widget object.

- Source rectangles use GUI pixels relative to the original menu's `leftPos` / `topPos`, including negative coordinates for outboard controls.
- Ordinary region destinations use nonnegative coordinates relative to the template canvas. Explicit template padding is additional to the surface's frame/header insets; use zero for the standard compact inset. There is no independent per-region scaling.
- `exterior(id, source, windowX, windowY)` retains a native fragment outside the frame. Its destination is relative to the **outer window**, can be negative, does not scroll and does not enlarge the body canvas. Exterior fragments paint behind the frame and advertise visual/input regions automatically. Pointer capture, tooltips and real slots use the same inverse mapping. Existing region constructors continue to create ordinary canvas regions.
- Regions paint in list order; later regions are on top. Pointer lookup uses the same rectangles and cutouts in reverse order. A region with `interactive=false` is a visual mirror and does not intercept input beneath it.
- `inherit(bounds)` keeps the inferred upper region. `keep` adds another native rectangle. `hide` subtracts native rectangles from existing regions. `move` subtracts the original location before adding its replacement. `moveRegion` relocates a complete named region.
- `DesktopNativeRegion.cut(...)` supports nonrectangular compositions by subtracting rectangular holes. Several kept rectangles can form a more intricate shape.
- `canvas(width, height)` reserves room for additions; `padding(pixels)` adds outer space. The canvas expands to contain ordinary destinations; exterior destinations are excluded. `Placement.exterior()` distinguishes window-local exterior placements from canvas-local ones.
- `view.slotBounds(ids...)` and `view.widgetBounds(ids...)` return unions for anchors. Slot IDs remain the original menu IDs. Widget IDs are local to the current screen build; resolve them again after resize, using type/label information when appropriate.

The host evaluates `nativeLayout` during layout preparation. Derive anchors from the current snapshot instead of mutating original slot coordinates. Changes to the committed plan invalidate incompatible pointer gestures. Native screen ticks and renders are not repeated for each region: the screen renders once into an isolated framebuffer, then its fragments and real slots are composed in order.

Core maps slot hits, native control input, captured native drags, hover coordinates, text focus and tooltips through this geometry. A partly cut real slot is rejected: include its complete cell or hide the complete cell. Invalid geometry falls back to the inferred layout and reports a diagnostic. Hidden native widgets lose keyboard focus.

## Additions and replacements

All ordinary definition hooks remain available, with your own `State` in the callback context:

- `renderNativeUnderlay` draws behind the native fragments, for example to fill a removed area.
- `nativeSlotBackground(context, menuSlotId)` defaults to `true`. Returning `false` suppresses only that complete captured cell background and uses the standard SIU slot frame and existing item/count renderer. It does not change slot identity, permissions or synchronization. Native artwork surrounding the cell remains untouched.
- `render` adds API controls or artwork above the composed fragments and their real slot items.
- Existing input, tooltip, lifecycle, persistence, overlay, attachment, popup, management-control, custom payload and presentation-region hooks remain available.

Hide a native control's complete source area before drawing and handling its replacement through ordinary API callbacks. Add custom controls in reserved canvas space. Server-changing actions still use the established authorized API/server handler path. A presentation template cannot grant a click permission or change a slot's ownership.

The host computes native canvas sizing and uses a fixed outer window, with a bounded viewport and horizontal/vertical scrollbars only when needed. There are no hidden scrollbar strips on a fitting canvas. Cross-axis overflow is resolved together, and the committed viewport is shared by clipping, native input, slots, tooltips and controller scroll regions. Wheel scrolls vertically; Shift-wheel scrolls horizontally. Native controls get their own scroll event first. `scrollable(false)` requests the complete canvas without host viewport scrolling; use it only when your layout is guaranteed to fit. Templates do not reflow arbitrary native content automatically.

Animated exterior panels can reserve their maximum placement extent through `DesktopWindowPresentation.externalRegions`. A `DesktopWindowRegion` with `constrainsWindow=true`, `visual=false`, `interactive=false` reserves space without creating an invisible occluder. This keeps a parent stable while an attached native panel grows upward; the current exterior fragment supplies only the actually visible/clickable area. Keep these queries side-effect free.

## JSON profiles

Static geometry can be supplied without Java registration:

- Resource pack/mod resource: `assets/<namespace>/siu/native_windows/<name>.json`.
- Local development override: `<game directory>/config/salts_inventory_update/native-windows/<name>.json`.

```json
{
  "menu": "your_mod:machine",
  "minecraft": "1.20.1",
  "layout": {
    "format": 1,
    "padding": 4,
    "minimumWidth": 180,
    "minimumHeight": 100,
    "scrollable": true,
    "regions": [
      {
        "id": "body",
        "source": {"x": 7, "y": 16, "width": 162, "height": 56},
        "x": 0,
        "y": 0,
        "interactive": true,
        "cutouts": []
      }
    ]
  }
}
```

`screenType` optionally restricts a profile to an exact native screen class. `priority` adjusts selection within a source tier by −100…100. Local overrides take priority over resources; equal-priority conflicts keep the deterministic first file and warn. Unknown menu IDs and profiles for other Minecraft versions are ignored. Registered API definitions always win. Resource reload refreshes profile discovery; close and reopen an existing container to apply a replacement profile. `/saltsinventory nativeui reload` does the same for files edited on disk.

Profiles are versioned geometry only. They cannot contain Java, change menu permissions or implement custom network behavior. Limits include 64 regions, 64 cutouts per region, 4096 GUI-unit layout extents and bounded profile sizes/counts. `DesktopNativeProfiles.encode/decode` round-trips the layout object. Java callbacks remain preferable for dynamic slot/widget anchors and state-dependent controls.

An exterior region adds `"exterior": true` to its format-1 region object; old profiles omit it and retain canvas coordinates. Signed destinations are accepted only for exterior regions.

## In-game inspector

1. Open an automatically hosted or explicitly templated native container.
2. Run `/saltsinventory nativeui inspect`. The topmost eligible native window opens the inspector; ordinary API windows are not converted.
3. **Source** shows the original screen. Drag a rectangle to select native pixels. Editing is read-only: selections do not pick up items or activate native controls.
4. **Use** makes the selection the base region. **Add** appends a selected region beneath the existing pieces. **Hide** cuts the selection from existing pieces.
5. **Move** enables dragging a region to a new destination. **Padding +** adds two GUI pixels. **Reset** returns to the registered/native layout.
6. **Export** writes a local JSON override with the menu and screen class. Invalid edits must be corrected before export. **Exit** or Escape returns to normal interaction while keeping the preview for that window's lifetime.
7. Reopen the container to test the saved profile. Copy it into your mod's resource path for distribution, or translate the geometry into a Java template for dynamic anchors.

The inspector visualizes native slot/widget boundaries and includes its toolbar in the window's bounds. At smaller GUI sizes the host scrolls the native canvas. Exported files are local overrides; exporting does not overwrite source code or publish an integration.

## Scope and validation

For a library that sends custom menu state, `DesktopSessions.activeNetworkMenu(player)` returns the actual menu model during that player's scoped desktop server callback. It is empty outside that callback, including on other threads or for another player. Use it with `activeNetworkSessionId(player)` and `sendSessionPayload(...)` to route state through the public transport; receive it in the template's `customPayload` callback. This observation does not claim the menu as an explicit API window, and is available on all supported Core targets.

An integration must preserve the upstream message schema and validate its own target types. This hook does not provide access to Core's internal session objects. Do not assume that a menu ID or container ID is the same as an SIU session ID.

This API preserves the automatic host's admission, menu construction, shared cursor and original server slot behavior. Native rendering with custom framebuffer assumptions, screen-global side effects or packets tied to the active vanilla menu may still need dedicated integration work. A template selects presentation; it cannot infer every mod's semantics.

See [focused validation](native-window-templates-validation.md) for executed checks and limits. Compile the public-API-only example with `./gradlew verifyNativeTemplateExample`. It is excluded from production JARs and is also checked by root `check` / `build`.
