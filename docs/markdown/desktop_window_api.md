# Salt Desktop Window API v2

Targets: Fabric 1.20.1, 1.21.1, 1.21.11, 26.1.2, and 26.2; Forge 1.20.1; NeoForge 1.21.1, 1.21.11, 26.1.2, and 26.2.

Salt's Inventory Update can host mod containers inside its movable desktop windows. The desktop engine still owns sessions, carried stacks, hotbar interaction, placement, lock/pin/ghost pin, resizing, and multi-window behavior. Mods provide a window definition and, when needed, server-side opt-in/payload handlers.

Future API feature requests and larger helper ideas are tracked in [API ToDo](api_todo.md).

## Supported Versions And API Bands

The examples in this document use the 26.x API spelling. Most API concepts are shared across every supported version, but a few Minecraft type names and typed payload helpers differ by version.

| Version | Loaders | Resource id type | Slot click type | Input event types | Typed payload helpers |
| --- | --- | --- | --- | --- | --- |
| 26.1.2 | Fabric, NeoForge | `Identifier` | `ContainerInput` | `net.minecraft.client.input.*` | Yes |
| 26.2 | Fabric, NeoForge | `Identifier` | `ContainerInput` | `net.minecraft.client.input.*` | Yes |
| 1.21.11 | Fabric, NeoForge | `Identifier` | `ClickType` | `net.minecraft.client.input.*` | Yes |
| 1.21.1 | Fabric, NeoForge | `ResourceLocation` | `ClickType` | `com.salts_inventory_update.client.input.*` | Yes |
| 1.20.1 | Fabric, Forge | `ResourceLocation` | `ClickType` | `com.salts_inventory_update.client.input.*` | Raw byte payloads only |

Loader differences do not create a separate Salt desktop API. The Forge and NeoForge builds compile the same per-version API sources as Fabric, then add a loader shim for entrypoints, events, key bindings, commands, and networking.

When porting an example between API bands:

- On `1.20.1` and `1.21.1`, replace `Identifier` with `ResourceLocation`.
- On `1.20.1`, `1.21.1`, and `1.21.11`, replace `ContainerInput` with `ClickType`.
- On `1.20.1` and `1.21.1`, import `CharacterEvent`, `KeyEvent`, and `MouseButtonEvent` from `com.salts_inventory_update.client.input`.
- On `1.20.1`, use raw `byte[]` payloads; `DesktopPayloadCodecs`, typed client `sendPayload`, typed server `registerServerPayload`, and typed `sendToClient` are not available.
- On `1.20.1` and `1.21.1`, `createRecipeBook` returns raw `RecipeBookComponent` instead of `RecipeBookComponent<?>`.

## Registration Model

Client rendering and server desktop support are separate on purpose.

```java
public final class MyCompat {
    public static void initClient() {
        MenuType<MyMenu> menu = findMenu("other_mod:my_menu");
        if (menu != null) {
            SaltsInventoryDesktopApi.registerClientWindow(menu, new MyWindow());
        }
    }

    public static void initServer() {
        MenuType<MyMenu> menu = findMenu("other_mod:my_menu");
        if (menu != null) {
            SaltsInventoryDesktopApi.registerServerWindow(menu, new MyServerWindow());
        }
    }
}
```

Use `replaceClientWindow(...)` when intentionally replacing an existing definition. Use `registerClientWindowPredicate(...)` for dynamic matching when a direct `MenuType` is not enough.

Important fallback rule: unknown modded containers keep opening their normal vanilla/mod screen. A menu becomes Salt desktop-managed only when it is a vanilla Salt-supported menu or it registers server window support. Registering a server payload handler also implies server desktop support.

## Compatibility Limits And Showstoppers

Salt can host normal `AbstractContainerMenu`-backed screens well, but some menus need extra integration before they are safe desktop windows.

Treat these as showstoppers unless the integration handles them explicitly:

- The screen keeps important state only on the client `Screen` instead of in the menu, synced data slots, or Salt payloads.
- The UI shows fake item entries that are not real `Slot`s, but expects normal slot clicks to work.
- Custom packets assume there is one currently open screen instead of a specific Salt desktop session.
- Buttons, text fields, tabs, scroll positions, or selected recipes mutate server state without a session-scoped packet or menu button.
- The original renderer hardcodes player inventory, hotbar, or vanilla screen coordinates into the same draw path as the machine UI.
- The menu has custom drag, quick-move, double-click, or carried-stack behavior that bypasses vanilla menu click handling.
- The container is client-only or not backed by a live server menu that Salt can validate.

When one of these applies, leave the menu on its original screen until the compat layer has a client window definition and any required server handler or payloads.

## Existing Menu Rework Checklist

When adapting an existing GUI to Salt, split the original screen into the parts Salt owns and the parts your integration owns.

- Do not render the player inventory or hotbar inside the Salt window. Salt already provides independent player inventory, hotbar, and offhand interaction.
- Map every real item slot to the correct menu slot id. Use `containerSlot(...)`, `containerSlotHit(...)`, and `menuSlotId(...)` when the menu's container slot indexes are easier to reason about.
- Use `renderSlot(...)` or `slot(...)` when Salt should draw the slot background and item. Use `texturelessSlot(...)` when your cropped background already contains the slot art.
- Move screen-only UI state into the desktop state object, synced menu data, or session-scoped payloads.
- Replace custom screen packets with Salt payload handlers when the action belongs to a specific desktop session.
- Recreate non-slot controls such as buttons, text fields, progress bars, tabs, and scrollbars through `render`, input hooks, widgets, menu buttons, or payloads.
- Keep optional mod compatibility guarded by registry lookups, reflection, or optional compat entrypoints.
- Test normal click, shift-click, drag distribution, double-click collection, carried-stack sync, recipe book behavior, ghost pin, close/reopen, and any custom payload actions.

## Client Window Definition

Implement `DesktopWindowDefinition<T extends AbstractContainerMenu, S>`.

The generic `S` is your per-window client state: selected tab, scroll row, search text, animation frame, focused text field, etc.

Main hooks:

- `createState(setup)`
- `title(context)`
- `defaultSize(setup)`, `minSize(context)`, `resizePolicy(context)`, `snapSize(context)`
- `opened`, `closed`, `moved`, `resized`, `focusChanged`
- `ghosted`, `unghosted`, `tick`, `render`
- `slotAt`
- `mouseClicked`, `mouseReleased`, `mouseDragged`, `mouseScrolled`
- `keyPressed`, `charTyped`, `wantsTextInput`
- `appendTooltip`
- `customPayload`
- `saveLocalState` / `loadLocalState`

Minimal slot window:

```java
public final class MyWindow implements DesktopWindowDefinition<MyMenu, MyWindow.State> {
    public static final class State {
        int scroll;
    }

    @Override
    public State createState(DesktopWindowSetupContext<MyMenu> context) {
        return new State();
    }

    @Override
    public DesktopWindowSize defaultSize(DesktopWindowSetupContext<MyMenu> context) {
        return DesktopWindowSize.of(8 + 9 * 18 + 8, 16 + 8 + 3 * 18 + 8);
    }

    @Override
    public void render(DesktopRenderContext<MyMenu, State> context) {
        int x = context.contentX();
        int y = context.contentY();
        for (int i = 0; i < 27; i++) {
            context.renderSlot(i, x + i % 9 * 18, y + i / 9 * 18);
        }
    }

    @Override
    public DesktopSlotHit slotAt(DesktopSlotContext<MyMenu, State> context, double mouseX, double mouseY) {
        int x = context.contentX();
        int y = context.contentY();
        for (int i = 0; i < 27; i++) {
            DesktopSlotHit hit = context.hitSlot(i, x + i % 9 * 18, y + i / 9 * 18, mouseX, mouseY);
            if (hit != null) return hit;
        }
        return null;
    }
}
```

## Cropped Backgrounds

Many normal container textures include the machine UI at the top and the 4x9 player inventory at the bottom. In a Salt desktop window, draw only the machine region and omit the duplicated player inventory.

Use `DesktopRenderContext.texture(...)` to draw a source rectangle from the original PNG. If that cropped region already includes slot backgrounds, use `texturelessSlot(...)` so Salt renders only the item, hover highlight, drag preview, and no-item icon on top of your texture. `defaultSize(...)` is the full Salt window size, so include the title bar and content padding around the cropped region.

```java
private static final Identifier MY_SCREEN_TEXTURE =
    Identifier.fromNamespaceAndPath("my_mod", "textures/gui/my_machine.png");
private static final int TEXTURE_WIDTH = 256;
private static final int TEXTURE_HEIGHT = 256;
private static final int MACHINE_WIDTH = 176;
private static final int MACHINE_HEIGHT = 84;

@Override
public DesktopWindowSize defaultSize(DesktopWindowSetupContext<MyMenu> context) {
    return DesktopWindowSize.of(8 + MACHINE_WIDTH + 8, 16 + 8 + MACHINE_HEIGHT + 8);
}

@Override
public void render(DesktopRenderContext<MyMenu, State> context) {
    int x = context.contentX();
    int y = context.contentY();

    context.texture(
        MY_SCREEN_TEXTURE,
        x,
        y,
        0,
        0,
        MACHINE_WIDTH,
        MACHINE_HEIGHT,
        MACHINE_WIDTH,
        MACHINE_HEIGHT,
        TEXTURE_WIDTH,
        TEXTURE_HEIGHT
    );

    context.texturelessSlot(0, x + 56, y + 17);
    context.texturelessSlot(1, x + 56, y + 53);
    context.texturelessSlot(2, x + 116, y + 35);
}

@Override
public DesktopSlotHit slotAt(DesktopSlotContext<MyMenu, State> context, double mouseX, double mouseY) {
    int x = context.contentX();
    int y = context.contentY();

    DesktopSlotHit input = context.hitSlot(0, x + 56, y + 17, mouseX, mouseY);
    if (input != null) return input;

    DesktopSlotHit fuel = context.hitSlot(1, x + 56, y + 53, mouseX, mouseY);
    return fuel != null ? fuel : context.hitSlot(2, x + 116, y + 35, mouseX, mouseY);
}
```

If the cropped texture does not include slot backgrounds, use `renderSlot(...)` or `slotBackground(...)` plus `texturelessSlot(...)` instead.

## Context Helpers

`DesktopWindowContext` gives safe access to:

- `minecraft()`, `menu()`, `originalTitle()`
- `sessionId()`, `sourceKey()`, `state()`
- `windowX/Y/Width/Height`, `contentX/Y/Width/Height`
- `focused()`, `minimized()`, `ghosted()`
- `carriedStack()`
- `fontWidth(...)`, `trimToWidth(...)`
- `menuSlot(...)`, `containerSlot(...)`, `menuSlotId(...)`

`DesktopRenderContext` adds:

- `fill`, `text`, `scaledText`
- `sprite`, `texture`, `windowNineSlice`, `onePixelNineSlice`
- `item`, `virtualItem`
- `slot`, `renderSlot`, `texturelessSlot`, `slotBackground`, `slotHighlight`
- `entityPreview`
- `tooltip`

`DesktopInputContext` adds:

- `shiftDown()`, `ctrlDown()`, `altDown()`, `mouseButtonDown(button)`
- `sendMenuButton(buttonId)`
- `sendRename(name)`
- `clickSlot(menuSlotId, button, ContainerInput)` on `26.1.2` and `26.2`
- `clickSlot(menuSlotId, button, ClickType)` on `1.20.1`, `1.21.1`, and `1.21.11`
- `quickMoveSlot(menuSlotId)`
- `toggleRecipeBook()`, `setRecipeBookSearch(search)`
- `sendPayload(channel, byte[])`
- typed `sendPayload(channel, payload, codec)` on every version except `1.20.1`

Do not manually mutate carried stacks for normal slots. Let Salt route slot clicks to the server session.

## Widgets

Reusable helpers live in `com.salts_inventory_update.api.client.desktop.widget`.

Available building blocks:

- `DesktopTextBoxState`
- `DesktopWidgets.renderTextBox`, `clickTextBox`, `keyPressedTextBox`, `charTypedTextBox`, `wantsTextInput`
- `DesktopWidgets.renderIconButton`, `renderTextButton`
- `DesktopWidgets.renderScrollbar`, `scrollByWheel`
- `DesktopWidgets.renderDropdown`
- `DesktopWidgets.renderVirtualItemGrid`, `virtualItemIndexAt`
- `DesktopVirtualItem`
- `DesktopWidgetRect`

Search box example:

```java
public static final class State {
    final DesktopTextBoxState search = new DesktopTextBoxState();
}

@Override
public void render(DesktopRenderContext<MyMenu, State> context) {
    DesktopWidgets.renderTextBox(context, context.state().search, context.contentX(), context.contentY(), 120);
}

@Override
public boolean mouseClicked(DesktopInputContext<MyMenu, State> context, MouseButtonEvent event, boolean doubleClick) {
    return DesktopWidgets.clickTextBox(context.state().search, event, context.contentX(), context.contentY(), 120);
}

@Override
public boolean keyPressed(DesktopInputContext<MyMenu, State> context, KeyEvent event) {
    return DesktopWidgets.keyPressedTextBox(context.state().search, event);
}

@Override
public boolean charTyped(DesktopInputContext<MyMenu, State> context, CharacterEvent event) {
    return DesktopWidgets.charTypedTextBox(context.state().search, event);
}

@Override
public boolean wantsTextInput(DesktopWindowContext<MyMenu, State> context) {
    return DesktopWidgets.wantsTextInput(context.state().search);
}
```

When `wantsTextInput` returns true, Salt suppresses normal movement/UI hotkeys while the desktop is active.

## Server Windows

Register server support when the menu should be captured into a Salt session.

```java
public final class MyServerWindow implements DesktopServerWindowHandler<MyMenu, MyServerWindow.State> {
    public static final class State {
        long lastHash;
    }

    @Override
    public State createState(DesktopServerSessionContext<MyMenu, State> context) {
        return new State();
    }

    @Override
    public void tick(DesktopServerSessionContext<MyMenu, State> context) {
        context.broadcastChanges();
    }
}
```

Server hooks:

- `createState`
- `opened`
- `tick`
- `closed`
- `visibilityChanged`
- `pinChanged`

Hidden ghost sessions still tick and validate through the desktop session manager. `closed` means the live session is actually ending, not just becoming a ghost preview.

## Payloads

Payload channels use `Identifier` on `26.1.2`, `26.2`, and `1.21.11`; use `ResourceLocation` on `1.21.1` and `1.20.1`.

Raw fallback:

```java
SaltsInventoryDesktopApi.registerServerPayload(MyMenus.MY_MENU, MY_CHANNEL, context -> {
    byte[] data = context.data();
    context.broadcastChanges();
});
```

Typed payload:

Typed payload helpers are available on `1.21.1`, `1.21.11`, `26.1.2`, and `26.2`. On `1.20.1`, register a raw byte payload and encode/decode the bytes yourself.

```java
public record ModePayload(int mode) {
    public static final StreamCodec<RegistryFriendlyByteBuf, ModePayload> CODEC =
        StreamCodec.composite(ByteBufCodecs.INT, ModePayload::mode, ModePayload::new);
}

SaltsInventoryDesktopApi.registerServerPayload(MyMenus.MY_MENU, MY_CHANNEL, ModePayload.CODEC, (context, payload) -> {
    context.menu().setMode(payload.mode());
    context.broadcastChanges();
});
```

Client send:

```java
context.sendPayload(MY_CHANNEL, new ModePayload(2), ModePayload.CODEC);
```

On `1.20.1`, send the encoded bytes instead:

```java
context.sendPayload(MY_CHANNEL, encodedBytes);
```

Payload data is capped by Salt's networking layer at 32 KiB. Keep payloads small and session-scoped.

## Virtual Item Grids

Terminals that display items not backed by `Slot`s should use virtual item rendering and their own mouse action packets.

```java
List<DesktopVirtualItem> visible = entries.stream()
    .map(entry -> new DesktopVirtualItem(entry.stack(), entry.count()))
    .toList();

DesktopWidgets.renderVirtualItemGrid(context, visible, scrollRow * columns, x, y, columns, rows);
int index = DesktopWidgets.virtualItemIndexAt(mouseX, mouseY, x, y, columns, rows, scrollRow * columns, visible.size());
```

Only return `DesktopSlotHit` for real menu slots. Virtual entries should send custom payloads.

## Resizing And Snapping

Use `DesktopResizePolicy.FIXED` for functional windows. Use `DesktopResizePolicy.STORAGE_GRID` for grid-like windows that should resize with full-size 18x18 slots.

Return `snapSize(context)` when a resizable custom window should remove leftover empty pixels after drag release.

## Ghost, Lock, Pin, Placement

API windows automatically inherit:

- desktop singleton lifecycle
- multi-window container sessions
- carried stack and normal slot handling
- hotbar/offhand interaction
- shift-click and vanilla mouse behavior
- lock, pin, ghost pin
- placement styles and saved geometry

Use `ghosted()`/`unghosted()` for animation state or preview-specific effects. Ghost previews should not process item movement; Salt already blocks normal slot mutation while ghosted.

## Common Mistakes

- Returning container slot order instead of menu slot id from `slotAt`.
- Handling carried stacks manually for real slots.
- Sending global mod packets that assume one active screen instead of session-scoped desktop payloads.
- Registering only a client definition for a server menu and expecting it to be captured.
- Forgetting `wantsTextInput` for search fields, which lets WASD move the player while typing.
- Rendering player inventory/hotbar duplicates inside a desktop container window.
- Drawing a full vanilla/mod screen texture instead of cropping out the player inventory area.

## Proof Cases

Salt's furnace window uses the internal API definition for a simple animated vanilla menu. Tom's Simple Storage compat uses v2 client definitions and server window handlers for a larger terminal-style integration with virtual entries, search, controls, custom packets, and server snapshots.

## Version-Specific Summary

`26.1.2` and `26.2` have the same public Salt desktop API. Their implementation differs internally because Minecraft moved some client screen and HUD APIs in `26.2`, but mod integrations that depend only on `com.salts_inventory_update.api` do not need source changes between those two versions.

`1.21.11` keeps the `Identifier` and `net.minecraft.client.input.*` style used by 26.x, but slot clicks use `ClickType` instead of `ContainerInput`.

`1.21.1` uses Mojang-style `ResourceLocation`, the Salt compatibility input event package, raw `RecipeBookComponent`, and `ClickType`, while still supporting typed payload codecs.

`1.20.1` uses the same broad shape as `1.21.1`, but without the typed payload convenience layer. Use raw byte payload registration and sends there.
