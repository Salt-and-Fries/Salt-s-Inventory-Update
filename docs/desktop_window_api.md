# Salt's Inventory Update Desktop Window API

Target: Fabric 26.1.2.

This API lets another mod replace Salt's generated container window for a `MenuType` with a custom desktop window while keeping Salt's existing desktop behavior: multi-window sessions, carried-stack sync, hotbar interaction, shift-click, lock, pin, ghost pin, close/minimize/focus, placement styles, and server-authoritative slot clicks.

## Quick Start

Register client window definitions from client init:

```java
import com.salts_inventory_update.api.client.desktop.*;
import com.salts_inventory_update.api.desktop.SaltsInventoryDesktopApi;

public final class MyModClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        SaltsInventoryDesktopApi.register(MyMenus.MY_MENU, new MyDesktopWindow());
    }
}
```

Examples below assume the usual Minecraft imports such as `Slot`, `Component`, `Identifier`, and `MouseButtonEvent`.

Register server custom-payload handlers from server/common init:

```java
import com.salts_inventory_update.api.server.desktop.DesktopServerApi;

DesktopServerApi.registerPayload(MyMenus.MY_MENU, MyDesktopWindow.CHANNEL, context -> {
    MyMenu menu = (MyMenu) context.menu();
    menu.setMode(context.data()[0]);
    context.broadcastChanges();
});
```

Use `SaltsInventoryDesktopApi.replace(...)` instead of `register(...)` when intentionally replacing an existing definition.

## Window Definitions

A desktop window definition owns the client-side layout and local state for one menu type:

```java
public final class MyDesktopWindow implements DesktopWindowDefinition<MyMenu, MyDesktopWindow.State> {
    static final Identifier CHANNEL = Identifier.fromNamespaceAndPath("my_mod", "my_window");

    public static final class State {
        int selectedTab;
        int scrollRow;
        int animationTicks;
    }

    @Override
    public State createState(DesktopWindowSetupContext<MyMenu> context) {
        return new State();
    }

    @Override
    public DesktopWindowSize defaultSize(DesktopWindowSetupContext<MyMenu> context) {
        return DesktopWindowSize.of(160, 96);
    }

    @Override
    public Component title(DesktopWindowContext<MyMenu, State> context) {
        return Component.translatable("container.my_mod.my_menu");
    }

    @Override
    public void tick(DesktopWindowContext<MyMenu, State> context) {
        context.state().animationTicks++;
    }
}
```

The state object is client-local and lives as long as that desktop window instance. Use it for tabs, scroll position, animation counters, focused text fields, and similar UI state.

## Slots And Rendering

Use menu slot ids for item interaction. If you have a container-slot index instead, use `containerSlotHit(...)` or `context.containerSlot(index)` so the API converts to the correct menu slot id.

```java
public final class SimpleSlotWindow implements DesktopWindowDefinition<MyMenu, Void> {
    private static final int INPUT = 0;
    private static final int FUEL = 1;
    private static final int OUTPUT = 2;

    @Override
    public void render(DesktopRenderContext<MyMenu, Void> context) {
        int x = context.windowX() + 8;
        int y = context.windowY() + 24;

        Slot input = context.containerSlot(INPUT);
        if (input != null) context.slot(input, x, y);

        Slot fuel = context.containerSlot(FUEL);
        if (fuel != null) context.slot(fuel, x, y + 20);

        context.sprite(MyTextures.ARROW, x + 38, y + 20, 24, 16);

        Slot output = context.containerSlot(OUTPUT);
        if (output != null) context.slot(output, x + 72, y + 20);
    }

    @Override
    public DesktopSlotHit slotAt(DesktopSlotContext<MyMenu, Void> context, double mouseX, double mouseY) {
        int x = context.windowX() + 8;
        int y = context.windowY() + 24;

        DesktopSlotHit input = context.containerSlotHit(INPUT, x, y, mouseX, mouseY);
        if (input != null) return input;

        DesktopSlotHit fuel = context.containerSlotHit(FUEL, x, y + 20, mouseX, mouseY);
        if (fuel != null) return fuel;

        return context.containerSlotHit(OUTPUT, x + 72, y + 20, mouseX, mouseY);
    }
}
```

Rendering helpers automatically respect ghost-window translucency where possible:

- `slot(...)`, `texturelessSlot(...)`, `item(...)`
- `sprite(...)`, `texture(...)`
- `windowNineSlice(...)`, `onePixelNineSlice(...)`
- `fill(...)`, `text(...)`
- `slotBackground(...)`, `slotHighlight(...)`
- `entityPreview(...)`
- `tooltip(...)`

## Buttons, Tabs, Scroll, And Input

Return `true` from an input hook when the custom UI consumed the event. Return `false` to let Salt continue with normal slot handling and vanilla fallback behavior.

```java
@Override
public boolean mouseClicked(DesktopInputContext<MyMenu, State> context, MouseButtonEvent event, boolean doubleClick) {
    int tabY = context.windowY() + 18;
    if (context.contains(event.x(), event.y(), context.windowX() + 8, tabY, 48, 14)) {
        context.state().selectedTab = 0;
        return true;
    }
    if (context.contains(event.x(), event.y(), context.windowX() + 60, tabY, 48, 14)) {
        context.state().selectedTab = 1;
        return true;
    }
    return false;
}

@Override
public boolean mouseScrolled(DesktopInputContext<MyMenu, State> context, double mouseX, double mouseY, double scrollX, double scrollY) {
    if (!context.contains(mouseX, mouseY, context.windowX() + 120, context.windowY() + 28, 12, 60)) {
        return false;
    }
    context.state().scrollRow = Math.max(0, context.state().scrollRow + (scrollY < 0 ? 1 : -1));
    return true;
}
```

Useful input helpers:

- `sendMenuButton(buttonId)` calls the server menu's normal button path.
- `sendRename(name)` sends Salt's existing rename packet.
- `clickSlot(menuSlotId, button, ContainerInput)` sends a normal slot click.
- `quickMoveSlot(menuSlotId)` uses Salt's focused/default shift-move behavior.
- `sendCustomPayload(channel, data)` sends addon data for this session.

## Custom Client/Server Payloads

Custom packets are scoped to a live desktop session. Payloads are capped at `32 KiB`.

Client:

```java
static final Identifier MODE_CHANNEL = Identifier.fromNamespaceAndPath("my_mod", "mode");

@Override
public boolean mouseClicked(DesktopInputContext<MyMenu, State> context, MouseButtonEvent event, boolean doubleClick) {
    if (context.contains(event.x(), event.y(), context.windowX() + 8, context.windowY() + 80, 32, 12)) {
        context.sendCustomPayload(MODE_CHANNEL, new byte[] {1});
        return true;
    }
    return false;
}

@Override
public void customPayload(DesktopWindowContext<MyMenu, State> context, Identifier channel, byte[] data) {
    if (MODE_CHANNEL.equals(channel) && data.length > 0) {
        context.state().selectedTab = data[0];
    }
}
```

Server:

```java
DesktopServerApi.registerPayload(MyMenus.MY_MENU, MODE_CHANNEL, context -> {
    MyMenu menu = (MyMenu) context.menu();
    if (context.data().length == 0) {
        return;
    }

    menu.setMode(context.data()[0]);
    context.sendToClient(MODE_CHANNEL, new byte[] {(byte) menu.getMode()});
    context.broadcastChanges();
});
```

The server drops custom payloads when the session is missing, hidden, invalid, or has no registered handler.

## Resizing

Definitions are fixed-size by default. Opt into resize handles with:

```java
@Override
public DesktopResizePolicy resizePolicy(DesktopWindowContext<MyMenu, State> context) {
    return DesktopResizePolicy.STORAGE_GRID;
}

@Override
public DesktopWindowSize minSize(DesktopWindowContext<MyMenu, State> context) {
    return DesktopWindowSize.of(96, 72);
}
```

Locked windows still cannot move or resize. Global config `allowResizing=false` disables resize handles for API windows too.

## Common Mistakes

- Do not mutate inventories client-side. Use Salt slot helpers, menu buttons, or custom server payloads.
- Do not pass container-slot indexes as menu slot ids. Use `containerSlotHit(...)` or `context.menuSlotId(slot)`.
- Do not manage the carried stack yourself. Salt syncs the shared carried stack across every desktop window.
- Do not draw item slots without implementing matching `slotAt(...)` hit testing.
- Do not register server handlers in client-only code.
- Do not assume your window is always opaque; ghost-pinned windows render translucent.
- Do not use the root client window API from a dedicated-server-only class. Use `DesktopServerApi` for server payload handlers.
