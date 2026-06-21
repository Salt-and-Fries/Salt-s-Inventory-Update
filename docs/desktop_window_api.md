# Salt Desktop Window API v2

Target: Fabric 26.1.2.

Salt's Inventory Update can host mod containers inside its movable desktop windows. The desktop engine still owns sessions, carried stacks, hotbar interaction, placement, lock/pin/ghost pin, resizing, and multi-window behavior. Mods provide a window definition and, when needed, server-side opt-in/payload handlers.

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
- `clickSlot(menuSlotId, button, ContainerInput)`
- `quickMoveSlot(menuSlotId)`
- `toggleRecipeBook()`, `setRecipeBookSearch(search)`
- `sendPayload(channel, byte[])`
- typed `sendPayload(channel, payload, codec)`

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

Raw fallback:

```java
SaltsInventoryDesktopApi.registerServerPayload(MyMenus.MY_MENU, MY_CHANNEL, context -> {
    byte[] data = context.data();
    context.broadcastChanges();
});
```

Typed payload:

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

## Proof Cases

Salt's furnace window uses the internal API definition for a simple animated vanilla menu. Tom's Simple Storage compat uses v2 client definitions and server window handlers for a larger terminal-style integration with virtual entries, search, controls, custom packets, and server snapshots.
