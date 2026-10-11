# Salt Desktop API — 0.1.4

Salt's Inventory Update hosts mod UIs as independent desktop windows. API version **5** supports menu-backed windows and client-only views. Salt remains an optional installation: your normal mod and UI run without it, while Salt discovers your integration providers when installed.

There are no adapters for the earlier public API. Build your integration for the exact Minecraft version, loader and mappings you support. The supported matrix is Fabric on 1.20.1, 1.21.1, 1.21.11, 26.1.2, 26.2 and 26.3; Forge on 1.20.1; NeoForge on the other five versions.

## Public API and 0.1.4 changes

All packages below begin with `com.salts_inventory_update`. The shared contracts have no native Minecraft dependency; the other contracts are compiled for each Minecraft target. Source links use 26.3 as the current reference. For an older target, read the same package under that version's `fabric/src/main/java` directory; Forge/NeoForge builds share those API sources.

| Package | Public entry points and purpose |
| --- | --- |
| [`api.integration`](../../versions/common/src/main/java/com/salts_inventory_update/api/integration) | `DesktopIntegration`, `DesktopIntegrationContext`, `DesktopDiagnostics`: optional discovery, phase selection and structured diagnostics. |
| [`api.desktop`](../../versions/26.3/fabric/src/main/java/com/salts_inventory_update/api/desktop) | `DesktopMenuBindings`: common slot/source/gesture registration. `DesktopPayloadCodecs`: typed payload encoding/decoding on 1.21.1 and newer. |
| [`api.server.desktop`](../../versions/26.3/fabric/src/main/java/com/salts_inventory_update/api/server/desktop) | `DesktopServerApi`, session/payload contexts and handlers, and recipe-transfer requests/decisions/validators. Safe to reference from the common provider. |
| [`api.client.desktop`](../../versions/26.3/fabric/src/main/java/com/salts_inventory_update/api/client/desktop) | `DesktopClientApi`, `DesktopMenuFactories`, window definitions/contexts, native rendering, presentation regions, resizing and matcher contracts. Load through the client provider. |
| [`api.client.desktop.widget`](../../versions/26.3/fabric/src/main/java/com/salts_inventory_update/api/client/desktop/widget) | Text editing, buttons, grids, virtual items and scrollbar helpers. Shared `DesktopScrollState` supplies scrolling/virtualization calculations. |
| [Shared window contracts](../../versions/common/src/main/java/com/salts_inventory_update/api/client/desktop) | `DesktopWindowHandle` and `DesktopWindowOptions`: instance lifetime and permitted window controls. Keep their use inside the optional integration even though these two contracts have no native GUI dependency. |

The 0.1.4 public surface changes are:

- The combined `SaltsInventoryDesktopApi` was removed. Register common/server behavior with `DesktopServerApi` and client behavior with `DesktopClientApi`.
- `DesktopWindowDefinition<T, S>` now accepts any model type. Contexts expose `model()` in place of `menu()`, with `inventoryAvailable()` separating menu-backed windows from client-only views.
- Former internal helpers are public contracts: `DesktopMenuBindings`, `DesktopMenuFactories`, `DesktopWindowPresentation`, `DesktopWindowRegion` and `DesktopNativeRenderContext`. The old internal types were removed.
- Use `sendPayload` in place of `sendCustomPayload`. Matchers must declare their supported menu types for capture negotiation. Duplicate registration now fails; window replacement is explicit.
- New optional discovery, client views/handles, keyboard ownership, modal routing, pointer capture/cancellation, overlay/external-region rendering, clips/transforms, free resizing and storage ownership hooks are described below. There are no old API aliases.

## Start with the examples

The [example sources](../../api-examples/ui/src/main/java/example/desktop) demonstrate:

- `SimpleMachineWindow`: one original machine frame, zero host insets, custom real-slot artwork, computed gesture previews, synchronized progress, session-scoped buttons and a management menu. `MachineTransferHandler` implements server-authorized smelting recipe transfer.
- `TerminalWindow`: a stepped surface with transparent top corners, protruding tabs, a 10,000-row virtual catalog, search, independent scroll state, nested clips, custom slot previews, resizing, tooltips, selection dragging, an attached inspector and a declared modal popup. Virtual entries use a custom server action, rather than invented slots.
- `ClientOnlyWindow`: native `EditBox`/`Button` widgets, instance-owned routing, Tab/Shift-Tab traversal, semantic labels, narration, inactive hover and capture cancellation. `StyledPreferencesWindow` reuses those widgets with custom manager-routed decoration.

The example backends contain no Salt references. Register their menu types through your usual loader setup and call `ExampleMenus.bind(...)` there. Keep your original screens registered. The provider classes supply the optional desktop presentation. The sample catalog selection demonstrates authenticated server actions; connect actual withdrawals to your own storage and permission checks.

Run `./gradlew verifyApiExamples` to compile these integrations against all twelve actual loader/version classpaths. Generated sources under each loader's `build/generated/api-examples` account for native Minecraft name, graphics and input differences. They are excluded from the shipped mod jars. See [the examples README](../../api-examples/README.md) for setup.

## Compile against Salt without requiring it

Use Salt as a **compile-only** mod dependency. Do not bundle its jar, copy its API classes into your mod, or list it as a required runtime dependency. The full matching Salt jar provides the public API; there is no separate runtime API mod.

For a local build, use the matching artifact from `build/release/0.1.4`, or the matching project's development output. Keep Minecraft mappings consistent. Pre-26 Fabric consumers should let Loom remap the production mod dependency; ForgeGradle consumers should use their usual deobfuscated mod dependency configuration. Modern ModDev consumers must also use the appropriate development mappings. A plain unmapped production jar is not interchangeable with a development jar.

A Fabric project using Loom can use:

```kotlin
dependencies {
    modCompileOnly(files("libs/salts_inventory_update-<minecraft>-Fabric-0.1.4.jar"))
    // Add the same matching jar to a local runtime only when testing Salt-present behavior.
}
```

For a project using named development output, `compileOnly(files(...))` can reference the matching loader, version-common and shared-common class directories. See this repository's `compileApiExamples` tasks for an executable example. The loader's remapping/deobfuscation configuration remains part of your own build; Salt does not publish an invented Maven coordinate.

### Fabric

Leave Salt out of `fabric.mod.json`'s `depends`. An optional `suggests` entry is allowed. Do not put the Salt providers in `main` or `client` entrypoints; those run even when Salt is absent. Salt reads the descriptor below from the owning `ModContainer` after your registries exist.

### Forge 1.20.1

Leave Salt out of mandatory dependencies in `META-INF/mods.toml`. If you declare it for ordering/version information, use `mandatory=false` with `modId="salts_inventory_update"`. Do not annotate the provider with `@Mod` or subscribe it as an ordinary automatic event subscriber. Salt scans the loaded mod files and runs common registration before server startup, or during client initialization.

### NeoForge

Leave Salt out of required dependencies in `META-INF/neoforge.mods.toml`. Optional metadata can use `type="optional"` and `modId="salts_inventory_update"`. Do not register the provider as a normal mod entrypoint/event subscriber. Salt reads each owning mod file's contents, using the supported NeoForge file SPI for that version. Client registration runs on the loader's queued client setup; dedicated servers select only the common provider.

## Minecraft-specific types

The following native types differ between targets. `CharacterEvent`, `KeyEvent` and `MouseButtonEvent` use the package shown in the event column.

| Minecraft | Identifier type | `nativeGraphics()` | Event package | `clickSlot` action type | Payload codecs |
| --- | --- | --- | --- | --- | --- |
| 1.20.1 | `ResourceLocation` | `GuiGraphics` | `com.salts_inventory_update.client.input` | `ClickType` | Raw bytes |
| 1.21.1 | `ResourceLocation` | `GuiGraphics` | `com.salts_inventory_update.client.input` | `ClickType` | Raw bytes or `StreamCodec` |
| 1.21.11 | `Identifier` | `GuiGraphics` | `net.minecraft.client.input` | `ClickType` | Raw bytes or `StreamCodec` |
| 26.1.2 / 26.2 / 26.3 | `Identifier` | `GuiGraphicsExtractor` | `net.minecraft.client.input` | `ContainerInput` | Raw bytes or `StreamCodec` |

On **26.3**, native mouse events and `mouseButtonDown(...)` use SDL mouse button ids, such as `InputConstants.MOUSE_BUTTON_LEFT`. `clickSlot(...)` retains container button conventions: for a pickup, primary is `0` and secondary is `1`. Translate the event button before sending a slot click; do not forward the SDL id directly. Other container actions have their own button meanings. Earlier targets use their native pre-SDL mouse constants.

See the version adaptations in the example compilation tasks for native event construction and widget calls. These source differences require a build for each target.

## Optional provider discovery

Ship a UTF-8 properties file at exactly:

```properties
# META-INF/salts_inventory_update.integration.properties
modId=my_mod
apiVersion=5
common=my.mod.salt.MyCommonIntegration
client=my.mod.salt.MyClientIntegration
# Optional prerequisites, evaluated before resolving either provider class:
# requires=another_mod,optional_ui_library
```

`modId` must match the descriptor's owning loader mod. `common` and `client` can be omitted independently. Each specified class implements `DesktopIntegration`, with an accessible no-argument constructor:

```java
public final class MyCommonIntegration implements DesktopIntegration {
    @Override
    public void register(DesktopIntegrationContext context) {
        DesktopServerApi.registerWindow(MyMenus.MACHINE, new MyMachineServerHandler());
    }
}
```

The client provider registers `DesktopClientApi` definitions. Keep every Salt type reference inside these lazily discovered classes or classes they load. Your normal entrypoint must not instantiate the provider, store a Salt type in a field, inherit from a Salt class, or use a provider class literal. A client annotation alone does not make Salt optional.

The descriptor is the bridge: without Salt, nobody resolves the provider classes. The JVM can load your ordinary mod while their bytecode remains in its jar. On a dedicated server, Salt never resolves the `client` class. Put native GUI classes and client API imports exclusively in the client provider's dependency tree.

Common registration runs once per process, after mod registries exist, at client initialization or the first server-starting event. Client registration follows common registration. World restarts do not repeat registration. Providers run in deterministic mod-id order; use `requires` for prerequisites, not an assumed provider ordering. `DesktopIntegrationContext.phase()` identifies the selected phase and `isModLoaded(...)` can gate additional integrations.

Salt refuses an incompatible `apiVersion`, malformed/oversized descriptor, unknown descriptor key or absent prerequisite before class resolution. The descriptor is limited to 16 KiB and accepts only `modId`, `apiVersion`, `common`, `client` and `requires`. A failed common provider also prevents its client provider from running. Registered API entries are rolled back if the provider fails, including restoring deliberate window replacements. Keep other changes to your mod's hooks explicit: registration rollback covers Salt's registries, not arbitrary side effects in your code.

## Register a menu-backed window

Register a common/server handler and a client definition for the same menu type:

```java
// Common provider
DesktopServerApi.registerWindow(MyMenus.MACHINE, new MyServerHandler());

// Client provider
DesktopClientApi.registerWindow(MyMenus.MACHINE, new MyWindow());
```

`DesktopWindowDefinition<T, S>` supplies UI behavior. `T` is your menu/model; `S` is per-window UI state returned by `createState`. A definition may be reused by many windows. Put mutable selection, native widgets, scroll and drag state in `S`, not in the shared definition or static fields. `DesktopServerWindowHandler<T, S>` supplies independent server state and lifecycle callbacks per session.

Duplicate `registerWindow` calls are errors with diagnostics. Use `replaceWindow` to deliberately override an existing definition/handler. Payload channels, view ids, factory registrations and menu bindings also reject duplicate registrations.

A registered renderer is announced to the server in the bounded handshake/mode capability list. Automatic capture of a modded menu requires common support and client renderer support. If a provider is missing, fails or has incompatible metadata, the menu stays on its normal UI. Vanilla desktop menus keep their existing handling. An explicit user force-enable override remains an intentional generic capture override.

For conditional support, override the server handler's pure `supports(player, menu)` query. Returning false preserves the original UI for that instance. The client can use `registerMatcher(id, priority, menuTypes, matcher, factory)` for families of layouts. Declare the exact menu types; the matcher/factory must cover every automatically captured instance of those types. Mirror unsupported variants in the server eligibility check. Direct definitions take precedence; higher-priority matchers run first.

### Menus with opening data

If your client menu constructor needs additional data, register both sides:

```java
// Common provider: called when Salt captures normal menu opening.
DesktopServerApi.registerOpeningData(MyMenus.TERMINAL,
    (player, menu, buffer) -> buffer.writeVarInt(menu.catalogSeed()));

// Client provider: reconstruct the same logical menu and slot topology.
DesktopMenuFactories.register(MyMenus.TERMINAL,
    (minecraft, player, containerId, bytes) -> MyMenus.decodeTerminal(containerId, player, bytes));
```

The opening-data transport works on Fabric, Forge and NeoForge. Data is session-authenticated, defensively copied and limited to 32 KiB. Release any buffer you create. Reject invalid/trailing data. Mark menus that require these bytes through `DesktopMenuBindings.register(..., true)` as shown below. Registered opening writers must produce the data their factories require; capture falls back when required opening transport/data is unavailable. Preserve original non-Salt menu opening too. Salt's snapshot validation requires the reconstructed slot count to agree with the server; a factory cannot substitute a different inventory.

### Nonstandard slots and storage

`DesktopMenuBindings` is a common registration contract for menus with dynamic slots, transfer-backed slots or custom storage ownership. Its `SlotAccess` identifies logical slot ids and can override `isPlayerInventorySlot(menu, slot, inventory)` for inventory wrappers. Salt can then identify container content without assuming player inventory coordinates or a 4x9 rectangle.

For an otherwise conventional menu that requires opening data, register this once from the common provider:

```java
DesktopMenuBindings.register(MyMenus.TERMINAL,
    menu -> menu.slots.size(), true);
```

The registration overloads add `SourceKeyResolver`, `SnapshotPreparer`, then `GesturePolicy`; omit unused hooks or pass `null`. A custom `SlotAccess` implements `size(menu)` and can override `slot(menu, logicalId)` and player-inventory ownership. Its defaults use the native menu slots and `slot.container == inventory`.

| Binding hook | Responsibility |
| --- | --- |
| `SourceKeyResolver.resolve(player, menu, openingData)` | Return a stable source key for the real container/storage. The supplied opening bytes are copied. Returning `null` leaves source selection to Salt. |
| `SnapshotPreparer.prepare(menu, items)` | Materialize dynamic logical slots before snapshot validation. |
| `GesturePolicy.canQuickCraft`, `quickCraftPlaceCount`, `quickCraftMaxStackSize` | Describe drag eligibility, distribution and storage limits. |
| `allowsPickupAll`, `allowsPickupAllAnchor` | Control which slots supply items and which visible slot can start the gesture. |
| `repeatedQuickMoveLimit`, `useNativeQuickMove`, `allowsTargetedQuickMoveSource` | Bound repeated transfers and select menu-specific quick-move behavior. |
| `physicalOwner`, `physicalIndex` | Identify the backing storage object and index, even when different menus use different slot wrappers. Owner comparison uses object identity. |

Both sides must use the same logical topology. Return the same stable physical owner object/index for slots backed by the same storage to prevent duplicate transfers across windows. These hooks supplement menu/slot restrictions and server validity checks. A binding is shared by menu type, so inspect the supplied menu when behavior varies by instance.

Virtual catalog entries are presentation data. Render them with `virtualItem` or `DesktopVirtualItem`; handle them with your own hit testing and server actions. Do not create real slots merely to represent counts, recipes or remote entries.

## Open a client-only view

Client-only views use ordinary mod models and do not create a menu:

```java
DesktopClientApi.registerView(PREFERENCES, PreferencesModel.class, new PreferencesWindow());
var handle = DesktopClientApi.openView(PREFERENCES, "main", model, Component.literal("Preferences"));
```

Different nonblank instance keys create independent windows. Reusing a key with the same model object focuses that instance, preserving its state. Reusing it with a different object is an error. A non-null model must match the class registered by `registerView`. `openView` returns an empty optional when no view is registered or the desktop is unavailable; your mod can open its original UI then. Install an ordinary mod-owned callback from the client provider, as `ExampleUiHooks` demonstrates, so normal code contains no Salt references.

The context exposes `model()`, `state()`, geometry and `windowHandle()`. The handle's UUID identifies this particular window lifetime. `focus()` reopens/unminimizes an existing instance when another modal does not own focus; `close()` disposes it permanently. Stale handles return false. Handles are invalidated before `closed`, preventing recursive close actions. Opening a replacement with the same key gives a new lifetime/UUID.

For client-only views, `inventoryAvailable()` is false, the session id is `Integer.MIN_VALUE`, slot lists are empty and the carried stack is empty. Menu buttons, rename, slot/quick-move actions and payload sends return false; typed payloads are denied before encoding. These views do not select an inventory transfer target.

## Render intricate interfaces

`DesktopRenderContext` supplies text, fills, textures, sprites, item/virtual-item rendering, real slots, tooltips and entity previews. Sizes describe the complete surface, including its declared insets; `contentX/Y/Width/Height` derive from those insets. Salt's default presentation uses its frame. A mod-owned surface can use zero insets and draw the whole interface. Default callbacks use absolute scaled GUI coordinates. Region coordinates are window-relative.

The surface overhaul is in progress. The implemented surface contracts are described here; resource-reload/accessibility hooks, complete gesture presentation, consumer examples and native acceptance are still tracked in [the implementation record](../../API-SURFACE-0.1.4-PLAN.md).

Override `surface(context)` to return a `DesktopWindowSurface`. `DesktopWindowSurface.SALT` supplies the normal appearance. `modOwned(regions)` supplies zero insets, explicit hit regions and preserved dimensions. Salt then adds no frame, backing fill, title, controls or resize decoration, and disables its native title/grip hit targets. Window title metadata remains available. Normal callback contexts expose the committed snapshot through `surface()`.

`prepareLayout(context)` receives a candidate surface whose insets already apply to its content geometry. Position native widgets there. Salt then samples the final pure `surface(...)`, advanced regions and decoration queries and commits their immutable results together. Queries may run in both layout stages; rendering/hit testing use the completed snapshot rather than rerunning those queries. Keep painting and input based on that prepared geometry.

Regions independently declare visual, interactive and placement participation. `withRole(...)` assigns `CONTENT`, `MOVE`, a directional resize role, `SLOT`, `CONTROL` or `POPUP`. Controls, slots and popups take priority over movement/resizing; background content has lower priority. Among declared regions of equal priority, the later region wins. Actual slots also prevent move/resize interception. An undeclared cutout can expose an underlying window; modality still blocks background interaction independently of shape. Image alpha alone never selects a hit target.

For styled Salt-managed chrome, return a `DesktopWindowDecoration` from `decoration(context)`. Its `metrics()` defines title/control/grip geometry, and its `render(context, controls)` paints all chrome using your assets or draw calls. Each immutable `DesktopControlState` supplies its kind, bounds, hover/press/enable/selection state. Salt routes the controls using those same bounds and adds no default decoration when a renderer is supplied. Use `DesktopChromeMetrics.contentInsets(padding)` and `DesktopInsets.surfaceSize(...)` to keep surface and content sizes consistent.

Use `nativeGraphics()` to delegate appropriate parts of existing renderers and Minecraft/mod widgets. `rawGraphics()` exposes Salt's version bridge and `context.gesture()` supplies immutable computed slot/cursor presentation data. Native types differ by Minecraft version: `GuiGraphics` on 1.20.1/1.21.1/1.21.11 and `GuiGraphicsExtractor` on the 26.x targets. Compile against the matching target instead of assuming one native binary fits all versions.

`withClip(x, y, width, height, draw)` and `withTransform(x, y, scale, draw)` restore the parent scissor/pose in `finally`, including nested scopes. Clip dimensions must be nonnegative; transform positions must be finite and scale finite/positive. Clip coordinates follow the current native render pose. Apply the matching inverse transform in hit testing. `DesktopScrollState` provides bounded pixel scrolling, uniform-row virtualization, visible ranges/overscan and the corresponding row hit test. It does not own your model, variable-height layout or fetching policy. Existing helpers include text editing, buttons, slot/virtual grids and scrollbars; arbitrary trees, tabs and animations may use your own UI framework.

An optional `DesktopWindowPresentation` adds:

- `renderBehindFrame`: attached decorations beneath the Salt frame.
- `desiredWindowSize`: native layout-driven size updates.
- `externalRegions`: visual, interactive and position-constraining areas independently. Transient popups can remain interactive without changing window placement bounds.
- `updateTooltipState`: hover activity notification, including inactive notifications for obscured native controls.
- `cancelPointer`: cancellation without a release/commit when capture is lost.

`renderOverlay` belongs to the main definition and runs above that window's content/frame, below windows later in the stack. External controls and popups must report their hit regions. Return real slots from `slotAt` only when the visible area represents those slots; arbitrary virtual hit targets stay in your own input callbacks. Avoid duplicate native and Salt slot-click handling.

Implement `DesktopWindowPresentation` alongside your `DesktopWindowDefinition`. Its `externalRegions` query must be side-effect free. `DesktopWindowRegion` accepts negative window-relative positions, nonnegative dimensions and independent `visual`, `interactive` and `constrainsWindow` flags. The `visual`, `interactive` and `visualAndInteractive` factories constrain placement; `transientVisualAndInteractive` creates a popup that does not. Use `updateTooltipState(..., active)` to clear native hover state when the window loses tooltip ownership.

Reuse your content/widgets, adapting position, focus, rendering and input ownership. Do not replace the game's active screen from a hosted render callback or blindly delegate a whole container screen that assumes `player.containerMenu`, one global cursor or native player-inventory placement. Per-version native behavior outside these hooks requires an explicit adapter. The examples and existing Tom's/Sophisticated integrations demonstrate supported patterns; they are evidence for those patterns, not proof that every UI framework can be hosted unchanged.

## Geometry and viewport fallback

All surface/content sizes and regions use scaled GUI pixels, not framebuffer pixels. `windowX/Y` are screen-space origins; regions are relative to that origin. `contentX/Y` add the committed left/top insets. Content dimensions subtract both opposite insets and clamp at zero. `DesktopInsets.ZERO` imposes no padding. Mod-owned surfaces may deliberately declare their own insets. Default/minimum/maximum/snap sizes all describe the whole surface.

`DesktopInsets.surfaceSize(contentWidth, contentHeight)` adds insets with checked arithmetic. `contentWidth(surfaceWidth)` and `contentHeight(surfaceHeight)` subtract them and allow a zero-size interior. `surfaceTransform()` and `contentTransform()` provide translation/scale helpers: `screenX/Y`, exact `localX/Y` inverses and `pixelX/Y` rounded with `Math.round`. Native drawing uses its target's float transform; use integer translation and integral positive scale for crisp pixel art. Region rectangles are half-open on right/bottom. Negative region origins support attachments; negative dimensions and arithmetic overflow are rejected.

The surface's alpha, declared visual regions, pointer regions and placement constraints are independent. Salt does not sample image alpha. Declare a transparent cutout as absent from the visual/hit shape when clicks should reach another desktop window. A transparent visual corner may still have an interactive region when the owner must block input there. Shape never bypasses modal or world-input rules. Declaring a region does not draw or scissor it; your renderer owns its actual pixels.

`CLAMP` fits the surface to the viewport and requires your renderer to adapt to the resulting size; it may reduce below an author minimum when the viewport is smaller. `PRESERVE` retains unscaled dimensions and may overflow; it preserves access to the base window's top edge where possible. `ADAPT` also preserves pixels and calls `surfaceUnavailable(context)` once per unavailable surface/viewport size, including placement-constraining attachments. Select a compact internal layout, new desired size or another policy there. The default callback leaves the dimensions preserved. A newly fitting layout resets that notification. No policy scales textures automatically or silently hands a detached authenticated session to an unrelated native screen.

Use `environmentChanged(context, environment)` to react to GUI scale or viewport changes. Client-only ordinary UI hooks can choose their normal screen when `openView` is unavailable. For menu-backed content, implement a compact view or deliberate overflow in the same authenticated model. A fallback that opens a new native menu must follow your mod's normal server-authorized opening flow. Do not assign `minecraft.screen` or `player.containerMenu` to bypass it.

`TerminalWindow` demonstrates `ADAPT`: a small viewport reduces the virtual catalog's dimensions and hides the inspector attachment while retaining its state. It keeps a readable 160×120 minimum and reserves room for the protruding tabs. Below that minimum it deliberately retains unscaled dimensions. Its inventory/menu topology never changes when selecting the compact layout.

Author minimum sizes take precedence over an inconsistent author maximum; provide `maxSize >= minSize` to avoid that normalization. Salt applies author limits on initial/restored geometry, desired-size updates, handle resize, dragging and snapping. Preservation policies retain dimensions rather than shrinking to make attachments fit. `FIXED` disallows user resize; a provider may still update its desired size for an internal mode change.

## Definition defaults and callback failures

A definition can override only the capabilities it needs. `createState` returns null by default; `title` returns `originalTitle`; `defaultSize` uses the setup context's dimensions; `minSize` is 64×48; `maxSize`, `snapSize`, `decoration`, `createRecipeBook` and `slotAt` return null. `resizePolicy` is `FIXED`, `options` enables all seven permissions, `surface` is Salt's normal surface, `popups` is empty and `layoutRevision` is an empty string. `modal` and `wantsTextInput` are false. Input, tooltip, traversal and dismissal callbacks return false; other notification/render/persistence callbacks do nothing. Default narration supplies the metadata title.

Setup defaults for a client-only view are a 240×160 surface and 256×192 nominal content hints. Return your own surface dimensions when those hints do not describe your UI. Setup hints are not mandatory padding or an author minimum. Menu-backed setup dimensions depend on the captured menu; choose explicit dimensions when preserving original artwork.

`surface` and custom decoration `metrics` must be non-null; null popup/attachment lists are treated as empty. Prefer the documented defaults rather than relying on defensive normalization of malformed queries. Definitions and their queries run on the client thread. Callback exceptions produce structured diagnostics; an initialization failure rejects that window, input failures are treated as unhandled and rendering failures skip that callback. A failure before a layout commits retains its previous surface/region snapshot; Salt cannot undo native widget changes made in `prepareLayout`. Salt does not roll back mutations you made in your model or server handler. Validate before mutation and handle operation-specific errors in your own UI. Closed lifetimes suppress subsequent rendering/input/model callbacks; `closed` is the final definition notification.

## Computed inventory gesture presentation

`DesktopGestureSnapshot` contains `id`, `phase` (`NONE`, `PENDING`, `DISTRIBUTING`), native pointer `button`, container `quickCraftType`, nullable `ownerInstanceId`, immutable `slots`, computed `carriedPreview`, nullable `carriedCountText`, and `pointerCaptured`. It belongs to the current callback, not a mutable menu transaction. Stack accessors return copies. `slot(sessionId, menuSlotId)` resolves a preview by authenticated session and logical id; null means no affected preview for that slot.

Each `DesktopSlotPreview` contains `sessionId`, `menuSlotId`, a computed `displayStack`, nullable `countText`, `eligible`, `selected` and `hovered`. Distribution counts include current target contents, capacity, Salt's configured slot policies and global selected target count. Targets may belong to several windows/sessions. A non-participating window sees phase `NONE`; it must not paint another owner's targets as its own. Use the actual slot stack when no preview exists. `OriginalSlotArtwork` is the complete custom-rendering example; it never mutates a slot or the shared carried stack.

`gestureChanged(context, DesktopGestureEvent)` reports a stable gesture id and `STARTED`, `UPDATED`, `COMPLETED` or `CANCELLED` with a reason. A terminal callback observes cleared pending presentation. Completion means the pointer gesture ended; server authorization/reconciliation still determines the inventory result. Cancellation covers discarded pending clicks, invalid targets, owner disposal/hiding/minimization/ghosting and surface-mode changes. Salt handles releases consumed by its inventory routing. Clear your own transient native previews from terminal callbacks or read the new snapshot; global mouse polling is unnecessary. Custom native widget capture is independently exposed by `pointerCaptured()` and the presentation's `cancelPointer` hook.

Keep the original menu slot topology and backing storage when omitting player inventory/hotbar visuals. Bind ownership through `DesktopMenuBindings`; hidden player-owned slots remain in the menu and are presented through Salt's shared inventory. A logical id need not equal a physical container index. Dynamic wrappers supply a current resolver and slot snapshot. Hidden real slots cannot be returned as clickable targets. Virtual entries have no real logical slot; use authenticated mod payloads with server-owned quantity and permission checks instead.

## Native widgets, popups and accessibility

`DesktopWidgetHost` is an optional per-instance router for existing `AbstractWidget`s. `add(id, widget, semanticLabel[, cancellation])` rejects duplicate/blank ids, reused widget instances and null arguments. Position widgets in `prepareLayout`; paint with `host.render(context)` and delegate mouse/key/character methods. `focusNext(forward)` cycles visible active widgets; wire it from the definition's `focusNext` callback, used for unconsumed Tab/Shift-Tab. Hidden or disabled widgets lose focus and captured input before routing/rendering/narration; cancellation is delivered once. `clearFocus`, nullable `focusedId`, `hoverActive`, `cancelPointer` and `narrate(output)` manage their corresponding local state. The cancellation hook must clear custom drag state without executing a release action. Cancel capture before replacing an internal widget set. Native widgets still own caret, selection, button actions and focus artwork.

Override `narrate(context, NarrationElementOutput)` to describe the hosted view and its focused controls. The default narrates `title(context)` even without visible host chrome. Salt delegates narration to the modal/keyboard owner. `host.narrate` preserves the native widget's title, role and value, then adds your semantic label as a hint. Supply meaningful titles/labels and visible focus cues for custom controls. `ClientOnlyWindow` demonstrates both keyboard routing and narration; mod-owned surfaces retain the same accessibility hooks.

`popups(context)` returns immutable descriptors in bottom-to-top order within this owner. `DesktopPopup(id, regions, modal, dismissOnOutside, dismissOnEscape, dismissOnFocusLoss)` rejects blank ids; duplicate ids in one layout are errors. Regions become transient `POPUP` roles and do not constrain window placement. The integration still paints and routes the popup's widgets in its owner group. `context.popups()` exposes the committed list. Popup modality is desktop-wide while its owner is active; hidden/minimized/ghosted owners cannot block input.

`dismissPopup(context, id, reason)` receives `OUTSIDE_PRESS`, `ESCAPE` or `FOCUS_LOST`. Return true after removing it from the model and next paint; Salt cancels pointer capture and commits the new hit layout. A modal popup consumes an outside press even if dismissal is declined. A nonmodal dismissal lets normal routing continue. Escape checks the last declared popup first, before owner key input. Dismiss flags select requests, not forced mutation of your model. Close nested subview tasks/subscriptions yourself when dismissing them. Persistent panels use placement-constraining surface/external regions; popup descriptors are transient but remain owned by that window.

Before replacing an internal view, tab layout or set of real slot targets, call `windowHandle().cancelInput()` on the client thread. It cancels that owner's pointer capture, window drag/resize and any slot gesture involving it; other participating owners receive the same terminal cancellation. An unrelated owner's slot gesture remains intact. If called during a handled press, Salt does not capture the replacement view for that old press. It returns false for a closed lifetime and does not undo actions already sent to the server. Cancel or close the old subview's own asynchronous subscriptions separately.

## Input, focus and window constraints

Mouse presses go to the topmost hovered window or modal owner. A handled API press captures subsequent drag/release events for that owner, even outside its rectangle. Mouse motion reaches the capture owner or hovered window. Capture is cancelled on hide, ghost, minimize, removal and modality changes; `cancelPointer` must clear pending gestures without committing them.

Keyboard and character input go to one active API keyboard owner. `keyboardFocusChanged` reports ownership transitions. `keyboardFocused()` differs from `focused()`: the latter is the selected inventory transfer target. Opening/clicking a client-only view does not redirect Shift-click transfers. A focused editor returning `wantsTextInput=true` suppresses desktop shortcuts and movement. Forward key press/release and character events to the appropriate native widget; it still owns its internal caret/selection. Release events clear movement key state.

`modal(context)` is a pure query. An active modal stays on top and blocks pointer, keyboard, hotbar/world and inventory interaction behind it. It also takes keyboard ownership. Minimized, hidden and ghost windows cannot hold modality. Consume Escape in your callback to dismiss an internal modal panel; otherwise Escape closes a closable modal owner.

Use `DesktopWindowOptions` to allow moving, minimizing, pinning, ghosting, user closing, locking and management menus independently. Unsupported controls are hidden. Constraints apply when restoring saved state and during desktop updates. An owner's permanent handle close and world/session teardown can dispose a view even if user closing is disabled.

`DesktopResizePolicy.FIXED` disables interactive resize, `FREE` permits arbitrary resizing and `STORAGE_GRID` uses grid-oriented resizing. Sizes describe the surface including its declared insets. Supply `defaultSize`, `minSize`, optional `maxSize` and `snapSize` for your layout. Defaults are a 64×48 minimum, no integration-specific maximum and all seven window options enabled. Your layout must honor these dimensions and update child geometry after moving/resizing; side-region placement may constrain what fits on a small desktop.

Handles expose `state()`, `available(action)`, `request(action)`, `moveTo(x, y)` and `resizeTo(width, height)`. Requests use `DesktopWindowAction` for focus, user close, minimize/restore, pin/unpin, ghost/unghost, lock/unlock and `MANAGEMENT_MENU`. Right-click an explicit `MOVE` region to open the desktop management menu. A custom button may call `request(MANAGEMENT_MENU)`. Salt anchors the menu to its ellipsis control when present, otherwise to the surface's upper-right edge, and keeps it in the viewport without reserving host padding. Escape dismisses this menu before reaching the window's key callback or closing its owner. Parameterized movement/resizing use their own methods; `request(MOVE)` and `request(RESIZE)` return false. `windowStateChanged(context, snapshot)` observes changed state. User actions honor permissions, modal ownership and stale handles. Permanent owner `close()` remains separate from user-close permission.

## Lifecycle, persistence and threads

Client render/input/tick/lifecycle callbacks and view handle operations run on the client main thread. Common provider registration runs on the selected client/server initialization thread. Server session/payload callbacks run on the server thread. Marshal asynchronous model results to the appropriate main thread before touching a menu, window or widget. Do not retain a callback context or native graphics object beyond that callback.

`windowHandle().schedule(action)` may be called from a background thread; it queues an update and suppresses it if the window has closed before execution. `context.tasks()` supplies a lifetime-owned `DesktopWindowTasks` scope. `watch(stage, success, failure)` observes shared work without cancelling the shared stage; closing the returned subscription suppresses its results. `submit(executor, work, success, failure)` owns its work and supplies a cooperative cancellation predicate. Completion callbacks run through the owner on the client thread. Disposal closes the scope and suppresses queued/late results. Do not capture a live callback context in a worker; capture your mod-owned model or immutable request data instead.

Start lifetime-scoped asynchronous observations in `opened` or later. Initial environment/preparation callbacks run before the window is open; updates scheduled for a not-yet-open lifetime may be suppressed. A subview replacement should close its own subscriptions while retaining the parent window's task scope.

`createState` runs for each instance; the initial `environmentChanged` notification precedes surface preparation; `opened` follows creation/restoration. `moved`, `resized`, transfer-focus and keyboard-focus callbacks track desktop changes. Tick/animation callbacks continue per visible, non-minimized window. Ghosting retains the instance/state and emits `ghosted`/`unghosted`; it disables interaction and inventory actions. `closed` runs once on permanent disposal, including owner/world changes. The final closed-state notification precedes `closed`; preparation/render/input observers stop after disposal, including reentrant close from a callback. Release native resources and pending jobs there.

`loadLocalState`/`saveLocalState` use a per-window `CompoundTag` for UI preferences. Geometry/pins use Salt's existing window-state partitioning. Client view identity includes view id and instance key. Menu-backed source keys identify real sources; avoid assigning identical source keys to distinct containers. Persist filters, selected tabs and layout preferences, not authoritative stacks or server authorization tokens. Reopening the same retained instance preserves its state; reconstructing one creates new state then loads local preferences.

`layoutRevision(context)` defaults to the empty string and is limited to 256 characters. Change it when saved geometry/local preferences no longer fit your UI schema. Salt stores its revision separately from your preferences; mismatches discard the old local tag and reset to the instance's initial size before loading fresh preferences. Keep this revision per integration/layout, not tied to arbitrary runtime data.

`DesktopWindowEnvironment(viewportWidth, viewportHeight, guiScale, resourceRevision)` is delivered initially and when it changes. Resource reload increments `resourceRevision` and re-prepares retained windows on the client thread. Re-resolve your cached assets/styles when it changes; native texture ids use the reloaded resources. Salt cannot reconstruct arbitrary mod-specific cached renderer objects. Layout queries must be side-effect free except `prepareLayout`, and one completed layout supplies the surface, regions, popup descriptors and decoration metrics used by both paint and hit tests.

Within a window group the order is underlays, surface/chrome, content/slots/widgets and its overlay/attachments. Later window groups cover earlier groups. Native render strata separate groups. Desktop management popups, shared cursor and tooltips are final desktop stages; a per-window overlay does not paint over later owners. Render callbacks may not replace the active screen or retain native graphics beyond the callback.

## Server actions and authority

Use context helpers rather than sending another window's vanilla container packet. Menu buttons, slot clicks and custom payloads include Salt's connection/session authorization, state and ordered mutation handling. Each same-type menu has its own session and state. Salt checks capabilities, menu validity, visibility, state freshness, player state and rate/size bounds before dispatching authorized custom actions.

Register a channel with `DesktopServerApi.registerPayload` and send using `DesktopInputContext.sendPayload`. Raw bytes work on every target. Newer targets also provide registry-aware typed codecs through `DesktopPayloadCodecs`; decoding rejects oversized/trailing input. Server payload contexts expose the authenticated player, menu, per-session state and source key, plus `sendToClient` and `broadcastChanges`.

Client replies arrive in the definition's `customPayload(context, channel, data)` callback for that session. Server window handlers have `createState`, `opened`, `tick`, `closed`, `visibilityChanged` and `pinChanged` callbacks. Recipe-browser transfers stay disabled for custom menus until `validateTransfer` returns an allowed `DesktopTransferDecision` based on the server-resolved recipe and real destination slots. The [recipe-transfer reference](recipe_transfer_api.md) gives signatures, request/session fields, construction rules, limits and a consumer pattern.

Initialize every field used by your menu's full synchronization before that first write. Server `createState` runs before the synchronizer is installed; `opened` follows installation and its initial full synchronization. Menu construction or `createState` can initialize those values. A first `tick` or `opened` callback is too late for values serialized during opening. This matters for native mod synchronization as well as Salt's slot/data snapshot.

Your handler must validate operation-specific permissions, indexes, quantities and storage state. Treat virtual-entry ids/counts as requests, never as authoritative stacks. Preserve backing inventory rules, `mayPlace`/`mayPickup`, source locks and cursor reconciliation. Use `reject(reason)` for a diagnostic on a mod-specific refusal, then return from your handler. Its reason is limited to 512 characters. It records a warning; it does not abort execution, undo mutations or send an error reply by itself. Send your own response if your UI needs one. If a native handler needs a temporary active-menu assumption, adapt it explicitly; a context does not make the player's global menu point at this detached session.

### Transport and capture bounds

| Field | Limit |
| --- | --- |
| Opening data or custom payload body | 32 KiB |
| Client-advertised supported menu ids | 1,024 |
| Explicit force-enabled menu ids | 256 |
| Wire identifier text | 256 characters |
| Source text | 512 characters |
| Desktop container sessions | 16 |
| Items in an opening snapshot | 8,192 |
| Menu data values | 4,096 |
| Protocol envelope | 1 MiB |

The session/slot limits govern real menus, not the length of your virtual catalog. Keep large catalogs in your mod's model and use bounded paging/synchronization through your own actions. Salt's capability lists are separate from force overrides; do not use force capture as a substitute for registering a renderer.

## Shared inventory companions and original-surface controls

Use a real server menu for an inventory companion. A client-only view cannot supply server authorization, a backing inventory, slot permissions or a shared inventory cursor. The [complete consumer sources](../../api-examples/ui/src/main/java/example/companion) implement a main machine window and a nine-slot tool inventory. Their ordinary menus and source model contain no Salt references. The [standalone demonstration](../../api-examples/companion-demo) supplies normal loader registration and fallback screens.

### Source identity

`DesktopInventoryIdentity.reference(Object inventory)` requires a non-null, stable backing inventory or source object. Equality uses `==`, including when two objects have identical `equals()` values. Reconstructing the identity around that same object deduplicates it. Creating a new wrapper object on every acquisition does not. For capability/resource-handler wrappers, use the stable underlying inventory owner, not the transient wrapper.

`DesktopInventoryIdentity.persistent(String namespace, UUID sourceId)` requires a nonblank namespace of at most 128 characters and a non-null server-assigned UUID identifying the actual source. Equality compares namespace and UUID values. Use this when a mod reconstructs wrappers around a persistent inventory. Persist the UUID on the source; a copied/new tool must receive a new UUID. Neither the item type, menu type, display title, inventory position nor player identity alone identifies a tool. `equals(Object)` and `hashCode()` follow these identity rules. Keys are scoped to one player's current desktop connection, so authorization is never inherited from another player or connection.

Two main windows using the same actual Network Tool acquire one companion. Two different tools acquire two companions. A persistent identity does not grant permission or prove that a replacement object is the original inventory: the source authorization guard must independently verify that.

### Construct and acquire a real inventory session

`DesktopCompanionSpec(DesktopInventoryIdentity identity, MenuProvider provider, Predicate<ServerPlayer> authorization)` rejects null arguments. Its record accessors return those three values. The provider must construct a registered `AbstractContainerMenu` whose slots directly access the real inventory and retain its native restrictions and bookkeeping. It must not create a copied server `SimpleContainer`, proxy another window's slots or depend on another parent menu remaining open. A synchronized client menu replica is normal and required; it is not authoritative storage.

Register the companion's server handler using `DesktopServerApi.registerWindow`, its renderer using `DesktopClientApi.registerWindow`, and its client factory using `DesktopMenuFactories.register` when opening data is needed. Register `DesktopServerApi.registerOpeningData` to serialize the actual source information your client factory needs. Both sides must advertise support for the companion menu. Salt does not use force capture as permission, manufacture a menu type or register a vanilla fallback screen for you.

Acquire from the parent's `DesktopServerWindowHandler.opened` or `tick` callback:

```java
// In a registered DesktopServerWindowHandler<CompanionMainMenu, ...> callback:
ToolInventorySource source = parent.menu().tool;
var lease = parent.acquireCompanion(new DesktopCompanionSpec(
    DesktopInventoryIdentity.reference(source.inventory),
    new SimpleMenuProvider((id, inventory, player) ->
        new ToolInventoryMenu(id, inventory, source),
        Component.literal("Network Tool upgrades")),
    player -> source.authorized(player)));
```

The distributed [common provider](../../api-examples/ui/src/main/java/example/companion/CompanionCommonIntegration.java) includes imports, registration, opening serialization and a complete callback implementation. The [client provider](../../api-examples/ui/src/main/java/example/companion/CompanionClientIntegration.java) includes strict decoding and both window definitions. All associated source files are compiled by `verifyApiExamples` on every supported target.

`DesktopServerSessionContext.acquireCompanion(DesktopCompanionSpec)` returns `Optional<DesktopCompanionLease>`. It is available only for a live independent parent after its first snapshot has been sent. It returns empty before `opened`, for unsupported menus, inactive desktops, invalid parents/sources or the sixteen-session cap. Acquisition cannot evict a parent to make room. A companion cannot itself acquire another companion; cyclic/nested companion ownership is intentionally unsupported. Invalid opening data or a throwing provider is an integration error and can throw; the partially opened companion is cleaned up. Keep your normal tool UI available when Salt is absent or acquisition is unavailable.

Acquisition is idempotent for a parent lifetime plus inventory identity: an existing association returns the same live ownership object. A new parent reuses the existing companion session without reconstructing its menu, invoking its provider again, resetting its state or changing its position. An association released while another parent retains the companion can be acquired again, creating a new lease to the same companion. After final disposal, acquisition creates a new menu/window lifetime.

The first acquisition supplies that lifetime's menu, title and opening data. All owners using the identity must describe the same backing inventory and compatible slot rules. Repeated acquisition of an existing parent association retains its original specification/guard; it is not a way to replace them. Release and reacquire an association when its captured authorization requirements change. Each owner's guard is checked independently, and at least one valid owner must remain; the companion menu's own `stillValid` must also remain true.

The source guard runs on acquisition, server ticks and inventory authorization. Capture the exact source incarnation, backing inventory and required permissions; verify that the tool still exists at the authorized location or through your own stable locator. Checking only a title, item type or UUID of a replaceable object is insufficient. A false or throwing guard releases that parent's authorization. Permission loss, removal or replacement that invalidates all guards disposes the companion. A world change closes the old world's desktop sessions and expires their ownership references, including portable menus whose own validity check would otherwise remain true. A source moved or replaced by another operation can invalidate a gesture before its next authorized mutation. Your custom server payload handlers still validate their own quantities, operation permissions and backend transactions.

### Ownership and teardown

Every operation on `DesktopCompanionLease`, including its getters, uses the server main thread. `sessionId()` returns the companion's genuine authenticated desktop session ID. IDs are connection-local routing addresses, not persistent identity and not authorization tokens. `isOpen()` reports whether this parent association and companion lifetime are still registered; an external invalidation is reflected when the guard is checked, no later than the next server tick. `close()` releases only this parent's association. Repeated release is harmless. Do not call it from a client callback or a worker thread.

Closing the parent automatically releases its lease before integration teardown callbacks; consumers do not need a mandatory `closed` cleanup hook for this. When a second owner remains, the companion's own menu/token stays valid and receives a small ownership metadata update. Closing the last owner closes the companion once, invalidates all leases and its client handle, cancels interaction and delivers its normal `closed` callback. Disconnect/desktop session teardown clears the ownership registry. World changes and invalid menu/source authorization also dispose it. Release leases early if a parent changes its associated tool; acquisition alone does not release a lease for another inventory identity.

Dimension travel invalidates old-world client handles and cancels queued inventory intents, including while the desktop is hidden. It preserves the existing connection negotiation and mutation sequence; the new world may open fresh sessions through the same connection. Disconnect ends that connection as well. A late old-window action cannot restore a disposed lifetime.

`DesktopWindowContext.companion()` identifies an authenticated companion. `companionOwnerSessionIds()` returns an immutable current list of parent session IDs, or an empty list on independent windows/views. `visible()` and `DesktopWindowState.visible()` report eligible contents: disposed, hidden, minimized and ghosted windows return false. Retain handles or immutable snapshots; do not retain callback contexts, server session contexts, live slot previews or authorization tokens.

### Inventory routing and cursor conservation

Use the companion's own `DesktopSlotContext.menuSlotHit`/`containerSlotHit` and `DesktopInputContext` slot/transfer methods. Its `containerSlots`, `menuSlot`, `menuSlotId`, `sessionId`, computed gesture preview and `carriedStack` refer to its own menu. Pickup/place, Shift-click, quick-craft dragging, cancellation, pickup-all and permitted custom actions use the existing authenticated session routing. Its lifetime is independent of whichever parent opened first.

Slot `mayPlace`, `mayPickup`, activity, maximum stack size and the integration's `DesktopMenuBindings.GesturePolicy` remain authoritative. Native quick-move bookkeeping can be retained through that policy. For custom resource-handler slots, register `physicalOwner(menu, slot)` and `physicalIndex(menu, slot)` for the actual backing storage. Equivalent representations must return the same owner object by reference and index. This lets Salt deduplicate physical slots across transfer/drag scans; a dummy vanilla container or unique wrapper per representation cannot safely identify physical storage. The [inventory routing chapter](desktop_window_api.md#nonstandard-slots-and-storage) describes the other binding methods.

Recipe-transfer source discovery also checks companion authorization before using its inventory, even when the native menu's `stillValid` still returns true and the next lifecycle tick has not run. A revoked owner's guard cannot contribute items through another window's recipe transfer; another valid owner can keep that same companion usable.

There is one canonical server cursor on the player inventory menu. Companion acquisition and metadata updates do not settle it, replace it or create a second cursor. When a desktop menu is removed, Salt clears only that menu's copied cursor before invoking native `removed`, preserving the canonical cursor and avoiding double settlement. Parent closure cancels a pending desktop inventory gesture; already authorized mutations remain committed and the remaining cursor can be placed normally. Final companion disposal cancels its input and retains the canonical cursor. Minecraft's ordinary disconnect/world/player teardown retains its own normal cursor settlement behavior.

### Drag-only presentation, visibility and focus

Supply `DesktopWindowSurface.modOwned(...)` and draw your own frame/title. `DesktopWindowOptions.DRAG_ONLY` is `(true, false, false, false, false, false, false)` for movable, minimizable, pinnable, ghostable, closable, lockable and management-menu permissions. A companion's manager policy enforces this even if a renderer accidentally returns broader options. Resize is also denied. There are no native title controls, title menu, independent close/pin/minimize/ghost/lock actions or keyboard shortcuts that bypass the policy. `available`/`request` and `moveTo` validate the same policy and current modal/lifetime state. `DesktopWindowHandle.close()` returns false for a companion: release a server lease, close its parents or let teardown dispose it. Ordinary independent window handles retain permanent owner-driven close.

On initial opening, the companion tries the right, left, below and above sides of the first eligible owner, in that order, with a four-GUI-pixel gap. It selects a side that fits the viewport before falling back to the declared viewport policy. It enters the normal stack just below that owner and does not open an extra player inventory, select an inventory-transfer target or take keyboard focus. Subsequent associations do not reposition or promote it. A user click may focus/promote it normally. Its overlay and artwork belong to that window group and respect foreground windows and modal blocking.

An owner is eligible when its lifetime is open and its contents are neither minimized, ghosted nor persistently hidden. At least one eligible owner keeps the companion visible. If all owners become unavailable, the companion hides, loses focus/capture and cancels its gestures while keeping its menu and position. Restoring an eligible owner restores the companion without selecting it. An independent owner can be minimized while another still keeps the companion visible. A companion cannot independently restore itself while all parents are unavailable. Desktop-modal windows/popups block companion input just as they block other windows.

If a parent closes during an inventory gesture, Salt cancels the pending gesture and preserves the shared cursor. A companion's window drag may continue when another visible owner remains; losing the last eligible owner cancels that drag too. Server final release/invalidation cancels all companion input and invalidates its handle. Auto-opening or ownership updates do not call `focus`; consumers should also avoid doing so from their `opened` callback.

### Manager controls inside original artwork

`DesktopSurfaceControl(DesktopWindowControl control, DesktopWindowBounds bounds)` accepts `CLOSE`, `PIN`, `MINIMIZE` and `ELLIPSIS`. Its accessors return the kind and immutable surface-relative rectangle. `DesktopWindowBounds(x, y, width, height)` requires positive dimensions and checked coordinates. The manager rejects duplicate kinds and overlapping rectangles during layout preparation, retaining the prior committed geometry on a failed preparation. Declare controls through `DesktopWindowDefinition.managementControls(context)`; the default is an empty list. They are opt-in and used only on mod-owned surfaces.

`renderManagementControls(DesktopRenderContext<T,S>, List<DesktopControlState>)` paints the controls after that window's original content and before the next window. It is also called for a minimized mod-owned window so the consumer can paint its original collapsed title and Restore button. The default does nothing; declaring controls requires drawing them. Salt adds no backing fill, title, frame, shadow, padding or control textures. The [main-window example](../../api-examples/ui/src/main/java/example/companion/CompanionMainWindow.java) defines 11×11 controls, original texture dimensions, gaps, title-space measurement, all rendering states and native slot hits.

`DesktopControlState(control, bounds, hovered, pressed, enabled, selected, keyboardFocused)` is an immutable paint snapshot; its bounds are absolute GUI pixels, already translated from the committed surface geometry. The six-argument constructor defaults `keyboardFocused` to false for ordinary decoration renderers. `hovered` respects topmost routing; `pressed` tracks the owning press until release/cancellation; `enabled` reflects live action permissions/modality; `selected` means pinned, minimized or the management menu open; `keyboardFocused` exposes manager keyboard traversal. Reading/mutating a detached snapshot cannot change permissions. A stale handle returns false for actions; it cannot revive a window.

Close routes `USER_CLOSE`, Pin routes `PIN`/`UNPIN`, Minimize routes `MINIMIZE`/`RESTORE`, and More routes `MANAGEMENT_MENU`. Action permissions are checked again on release/activation. Title controls outrank MOVE regions. The manager supplies translated hover tooltips, including Unpin/Restore labels, anchors More to its declared rectangle and supplies narration for keyboard-focused controls. Alt+F10 enters enabled management controls directly, before native widgets consume keys, so controls remain accessible even when a native widget host wraps its own traversal. Escape leaves control focus. Unconsumed Tab/Shift-Tab also traverses enabled controls in declaration order after the integration's native widget traversal. Enter/Space activates the focused control; traversal leaves the control set at its ends, allowing your `focusNext` hook to continue. Modal blocking and disposal cancel access.

`DesktopWindowOptions(movable, minimizable, pinnable, ghostable, closable, lockable, managementMenu)` independently restricts those behaviors, including keyboard/actions/context menus. `DEFAULT` enables all seven; the five-argument constructor retains ordinary-window defaults for lock/menu behavior. Forbidden native commands are removed from the management menu. Minimized mod-owned contents are not rendered, but their declared controls remain clickable so Restore can work. Rendering a collapsed original title is the integration's responsibility; Salt does not draw one for you.

Reserve the actual control rectangles before laying out your original title, search box or tabs. Compute the first control's x-coordinate, subtract the title's starting x and desired gap, and use `fontWidth`/`trimToWidth`; the complete example demonstrates this. Alternatively reposition controls or an original widget at layout preparation. Retain your original GUI-pixel tab width/height, visible borders, padding and hit rectangles. Draw the existing texture at its original source/destination dimensions, declare its real visual/hit regions and choose PRESERVE or ADAPT for small viewports. GUI scale multiplies GUI pixels at the framebuffer; Salt does not force a tab's scaling or replace its texture.

### Matching the controls on Salts vanilla windows

These are the standard Salts controls used on vanilla Minecraft container windows. **All permitted controls should either be stretched out into the complete expanded row, or compressed to the ellipsis (`…`, More) and `X` (Close).** Expanded means separate buttons at their original size; do not stretch the button artwork. This is a two-state decision. There are no intermediate three/four/five-button compression stages, shrinking icons, reduced gaps or controls removed one at a time. Window permissions still apply: omit a forbidden More or Close, and show no management controls on a drag-only companion.

The standard expanded order, from left to right, is Sort (when eligible), Focus, Pin, Lock, Link, Minimize, Close. Minimize appears when the desktop's minimizable-windows setting and the window's permission allow it. Sort appears only for an eligible inventory/storage source with negotiated sorting support. Filter unsupported controls before measuring the row. Pin/Unpin, Lock/Unlock and Minimize/Restore retain their positions when their state changes. The expanded native row does not include More; the compact row is More followed by Close. During compression, Sort, Focus, Pin, Lock, Link and Minimize move into More together, in that same order, subject to their permissions. Close remains in the title row and is not duplicated inside More. Temporarily disabled actions, such as modal-blocked controls, retain their committed geometry and use the public enabled state; they are not a new compression stage.

For `DesktopChromeMetrics.SALT`, use 11×11-GUI-pixel buttons, a 3-pixel gap, a 2-pixel **extra** right inset and a 4-pixel top inset. The title starts at x=6, its text y=5, and its separation from the control row is 6 pixels. The default title bar is 16 pixels high. Measure the current full, untruncated title with the current font, including the component's styling, before deciding whether to compress. Recompute on width, title, font/resource, options or eligible-control changes; do not use a fixed title character count or framebuffer pixels.

Let `W` be the window width, `T` the measured full title width, and `n` the number of permitted expanded buttons. The reserved control width is `C(n) = n * (11 + 3) + 2`, which is the public `DesktopChromeMetrics.SALT.controlsWidth(n)`. Keep the expanded row when `W >= 6 + T + 6 + C(n)`; compress the whole row when `W < 6 + T + 6 + C(n)`. Exact equality stays expanded. There is no separate fixed minimum title-space threshold before compression: preserving the **whole title** with the whole expanded row determines the threshold. For five buttons the required width is `T + 84`; for six it is `T + 98`; with Sort making seven it is `T + 112`. For example, a measured 60-pixel title and six buttons stay expanded at width 158, and switch to More/Close at width 157.

After choosing the row, let `k` be its actual button count and `i` its zero-based left-to-right index. The window-relative bounds are `x = W - (k - i) * 14 - 2`, `y = 4`, `width = 11`, `height = 11`. Draw and hit-test these same bounds. The extra right inset includes a separate trailing 3-pixel control gap: the rightmost Close starts at `W - 16` and ends at `W - 5`, leaving **5 pixels total** to the window edge, not 2. Compact More starts at `W - 30`. At width 176, the six-button expanded x positions are 90, 104, 118, 132, 146, 160; the compact positions are 146 and 160. These formulas preserve Close's location when switching modes.

The remaining title width is `max(0, W - 6 - 6 - C(k))`. With both compact buttons, `C(2)=30`, so the title gets `max(0, W - 42)`. Salts then keeps the full title if it fits, otherwise shortens it with the literal `...` suffix. Its minimum title text is the first character plus `...`, or just `...` for an empty title. Standard vanilla-window sizing reserves at least `42 + fontWidth(minimumTitleText)` for that compact title/row, together with the window's content minimum. A mod-owned surface must supply a suitable `minSize` or adapted layout itself. Do not let the minimum suffix overlap controls when a custom title band is narrower than that minimum; clip it to the measured title area or adapt the layout.

For an integration-owned title with native widgets occupying space, apply the same calculation to the **usable title band**. Let `L` be the actual title-text start and `R` its available right boundary after reserving native widget/tab/slot hit areas. Expanded fits when `R - L >= T + 6 + C(n)`; control x becomes `R - (k - i) * 14 - 2`. Use the original title area's y origin plus 4 for the buttons and reserve the title/control separation before painting text. Never overlap a native slot or widget to manufacture space. Preserve the original frame, bottom-border joins, tab dimensions and artwork; those remain consumer layout/artwork responsibilities. Reposition the row or choose a compact/adapted layout when the usable band is too small.

Automatic selection and complete native control ordering are supplied for Salt-managed chrome, including custom `DesktopWindowDecoration` renderers, through their committed `DesktopControlState` rectangles. The standard is the complete available button list, or More and Close when that list cannot fit. **Pin, Minimize, More, Close is not the standard expanded row.** More replaces the other management buttons during compression; it is not an additional button in the native expanded row.

A mod-owned surface supplies its own row selection and `DesktopSurfaceControl` bounds; there is currently no public automatic control-layout helper. The current API only accepts Pin, Minimize, More and Close as manager-routed surface controls. This is a capability limit, not an alternate standard layout: it prevents that mechanism alone from displaying the full native expanded button list. Do not attempt to register Focus, Lock, Link or Sort as `DesktopSurfaceControl` values. The distributed `CompanionMainWindow` is a fixed, consumer-positioned demonstration of the supported subset, not an example of full native button ordering or responsive compression. Consumers requiring the complete standard row can use Salt-managed decoration and its public control states; the mod-owned surface-control mechanism would need additional API support to expose every native button directly.

### Versioning, threading and verification

The public integration descriptor now uses `apiVersion=5`; the wire protocol is version 4 because authenticated opening/ownership metadata changed. Recompile consumers against the matching game/loader API5 artifact and update the descriptor explicitly. No mixed-protocol compatibility layer is supplied. Salt remains compile-only and optional; common providers must not resolve client classes on a dedicated server, and ordinary loader entrypoints must not reference either Salt provider. The standalone jar has no required Salt dependency and includes no Salt classes.

Client layout, rendering, input, handles and widget mutation use the client main thread. Server acquisition, release, guards, menu creation and server callbacks use the server main thread. `DesktopWindowHandle.schedule` remains the thread-safe way to enqueue a lifetime-checked client update. Do asynchronous backend work using immutable inputs and marshal validated results onto the appropriate main thread before inventory mutation.

Resource reload re-prepares retained surfaces and controls and delivers the new resource revision; re-resolve consumer caches there. The demonstration retains original 176-GUI-pixel texture/slot dimensions through GUI scales 2/3/4. Constrained viewports may require the consumer to select a compact layout through ADAPT; preserving native pixels does not guarantee that several full windows fit beside one another. Native/runtime acceptance evidence is maintained in [the companion implementation audit](../../API-COMPANIONS-0.1.4-PLAN.md). Compilation and headless routing tests are separate from actual inventory/network/lifecycle and rendered-image proof.

## Diagnostics and verification

`DesktopDiagnostics.snapshot()` returns an immutable list of recent structured events with level (`INFO`, `WARNING`, `ERROR`), owner, operation, message and optional cause. `listen(...)` subscribes and returns an `AutoCloseable` subscription; close it when your debug view closes. Listeners run on the reporting thread, so marshal updates before touching client UI. Salt also logs these events. History is bounded to 256 entries. Check discovery/API mismatch, registration conflicts, eligibility/opening-data failures, callback exceptions and action refusals when a window does not work.

Automated proof in this repository covers an external-style mod compiled against the API and run with Salt removed from its runtime classpath, Salt-present phased discovery, dedicated-server client-class resolution traps, registration rollback, client-only action denial, unique/stale handles, focus/modality, external regions, custom inventory ownership, capture negotiation and packet bounds. Existing sort, transfer, gesture and protocol tests remain part of verification.

API5 companion acceptance gates are tracked in [the companion verification record](../../API-COMPANIONS-0.1.4-PLAN.md). The [surface API verification record](../../API-SURFACE-0.1.4-PLAN.md) covers the earlier completed API4 work. Earlier API test totals and old screenshots are historical evidence. Compilation, assertions, clean process exit and visual fidelity are assessed separately. The [public signature reference](api_reference.md) lists the current consumer declarations; this guide specifies behavior and ownership.

```powershell
$env:CI = 'true'  # supported NeoForge binary patch pipeline
./gradlew verifyApiExamples :common:test
./gradlew -I functional-tests/sort-tests.gradle :1.20.1:fabric:test :1.21.1:fabric:test :1.21.11:fabric:test :26.1.2:fabric:test :26.2:fabric:test :26.3:fabric:test
./gradlew -PincludeFunctionalTests=true functionalTestCompile
./gradlew build
```

Run your own mod with Salt absent and present, and on a dedicated server, before shipping. Test multiple instances, cursor/slot restrictions, persistence, resize, clips, modal input, native widgets and your mod's real permission/storage behavior. Headless tests establish routing and contract behavior; visual/native-framework behavior still needs an in-game pass for that integration.
