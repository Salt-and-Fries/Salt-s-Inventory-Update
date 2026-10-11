package example.companion.demo;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import example.companion.*;
import com.salts_inventory_update.api.client.desktop.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.concurrent.CompletableFuture;

/** Opt-in native acceptance. Uses only public Salt contracts and ordinary Minecraft UI events. */
public final class CompanionNativeAcceptance implements CompanionUiHooks.Observer {
    private record Window(DesktopWindowHandle handle, Object model, int session, List<Integer> owners,
                          boolean visible, boolean keyboard, boolean transfer, long resourceRevision) { }
    private final Map<UUID, Window> windows = new LinkedHashMap<>();
    private int ticks;
    private int stage;
    private UUID companionId;
    private CompletableFuture<CompanionDemoMod.InventorySnapshot> pending;
    private CompanionDemoMod.InventorySnapshot serverState;
    private boolean finished;
    private long reloadRevision;
    private CompletableFuture<Void> reload;
    private int previousWidth;
    private int previousHeight;
    private UUID activeSharedId;
    private final Map<UUID, List<DesktopControlState>> controls = new LinkedHashMap<>();
    private DesktopWindowHandle modal;
    private DesktopWindowHandle expired;
    private List<DesktopWindowHandle> worldHandles;
    private CompletableFuture<Integer> leaseAudit;
    public static void initialize() {
        CompanionUiHooks.observer = new CompanionNativeAcceptance();
        DesktopClientApi.registerView(net.minecraft.resources.Identifier.parse("companion_example:qa_modal"), Object.class,
            new DesktopWindowDefinition<Object, Void>() {
                @Override public DesktopWindowSize defaultSize(DesktopWindowSetupContext<Object> context) { return DesktopWindowSize.of(140, 72); }
                @Override public boolean modal(DesktopWindowContext<Object, Void> context) { return true; }
                @Override public DesktopWindowSurface surface(DesktopWindowContext<Object, Void> context) {
                    return DesktopWindowSurface.modOwned(List.of(DesktopWindowRegion.visualAndInteractive(0, 0, 140, 72)));
                }
                @Override public void render(DesktopRenderContext<Object, Void> context) {
                    context.fill(context.windowX(), context.windowY(), context.windowX() + 140, context.windowY() + 72, 0xFF24577C);
                    context.text("Modal acceptance", context.windowX() + 8, context.windowY() + 10, 0xFFFFFFFF, false);
                }
            });
    }
    @Override public void opened(Object value) { snapshot((DesktopWindowContext<?, ?>) value); }
    @Override public void closed(Object value) {
        var context = (DesktopWindowContext<?, ?>) value;
        windows.remove(context.windowHandle().instanceId());
    }
    @Override public void tick(Object value) {
        var context = (DesktopWindowContext<?, ?>) value;
        snapshot(context);
    }
    @Override public void frame() {
        var client = Minecraft.getInstance();
        if (finished || ++ticks % 20 != 0) return;
        if (stage == 55 && client.player == null) {
            require(windows.isEmpty() && worldHandles.stream().noneMatch(DesktopWindowHandle::isOpen), "disconnect disposes every consumer window");
            System.out.println("COMPANION NATIVE ACCEPTANCE COMPLETE"); finished = true; client.stop(); return;
        }
        if (client.player == null || client.level == null || client.gui.overlay() != null) return;
        if (pending == null) {
            pending = new CompletableFuture<>();
            var result = pending;
            var server = client.getSingleplayerServer(); var id = client.player.getUUID();
            server.execute(() -> {
                try { result.complete(CompanionDemoMod.inspect(server.getPlayerList().getPlayer(id))); }
                catch (RuntimeException failure) { result.completeExceptionally(failure); }
            });
            return;
        }
        if (!pending.isDone()) return;
        try { serverState = pending.join(); pending = null; advance(); }
        catch (RuntimeException failure) {
            System.out.println("COMPANION NATIVE FAILED stage=" + stage + ": " + failure);
            finished = true; failure.printStackTrace(); client.stop();
        }
    }
    private void snapshot(DesktopWindowContext<?, ?> context) {
        var handle = context.windowHandle();
        windows.put(handle.instanceId(), new Window(handle, context.model(), context.sessionId(),
            context.companionOwnerSessionIds(), context.visible(), context.keyboardFocused(), context.focused(), context.environment().resourceRevision()));
    }
    @Override public void controls(Object value, List<?> states) {
        var context = (DesktopWindowContext<?, ?>) value;
        controls.put(context.windowHandle().instanceId(), states.stream().map(DesktopControlState.class::cast).toList());
    }
    private void advance() {
        var client = Minecraft.getInstance();
        long mains = windows.values().stream().filter(w -> w.model() instanceof CompanionMainMenu).count();
        var companions = windows.values().stream().filter(w -> w.model() instanceof ToolInventoryMenu).toList();
        Window companion = companions.isEmpty() ? null : companions.get(0);
        DesktopWindowHandle handle = companion == null ? null : companion.handle();
        require(serverState.total() == 28, "server inventory and cursor conservation (28 cards)");
        if (stage == 0) {
            if (companion == null) return;
            require(companions.size() == 1, "first inventory has one companion");
            require(!companion.owners().isEmpty(), "real authenticated companion ownership");
            companionId = handle.instanceId();
            require(windows.values().stream().filter(w -> w.model() instanceof CompanionMainMenu).anyMatch(w -> w.keyboard() && w.transfer()),
                "automatic companion opening preserves main keyboard and transfer focus");
            stage = 1;
        } else if (stage == 1 && mains == 2) {
            require(companions.size() == 1 && companion.owners().size() == 2, "two parents share one companion");
            require(companionId.equals(handle.instanceId()), "ownership acquisition preserves the window lifetime");
            for (var action : List.of(DesktopWindowAction.USER_CLOSE, DesktopWindowAction.MINIMIZE, DesktopWindowAction.PIN,
                DesktopWindowAction.GHOST, DesktopWindowAction.LOCK, DesktopWindowAction.MANAGEMENT_MENU, DesktopWindowAction.RESIZE)) {
                require(!handle.available(action) && !handle.request(action), "companion denies " + action);
            }
            require(!handle.close(), "independent handle disposal denied");
            require(handle.moveTo(24, 200), "independent companion position");
            dragTitle(companion, 8, 4);
            stage = 2;
        } else if (stage == 2) {
            require(handle.state().x() == 32 && handle.state().y() == 204, "native title dragging and repeated ownership acquisition preserve independent position");
            Screenshot.grab(client, false);
            System.out.println("COMPANION NATIVE BASELINE COMPLETE");
            stage = 3;
        } else if (stage == 3) {
            require(handle.focus(), "explicit user focus can select the companion");
            clickSlot(companion, 0, InputConstants.MOUSE_BUTTON_LEFT, 0); stage = 4;
        } else if (stage == 4) {
            require(serverState.cursor() == 16 && serverState.first()[0] == 0, "real server pickup");
            clickSlot(companion, 1, InputConstants.MOUSE_BUTTON_RIGHT, 0); stage = 5;
        } else if (stage == 5) {
            require(serverState.cursor() == 15 && serverState.first()[1] == 1, "real server placement");
            clickSlot(companion, 7, InputConstants.MOUSE_BUTTON_LEFT, 0); stage = 6;
        } else if (stage == 6) {
            require(serverState.cursor() == 14 && serverState.first()[7] == 1, "slot capacity restriction");
            clickSlot(companion, 8, InputConstants.MOUSE_BUTTON_LEFT, 0); stage = 7;
        } else if (stage == 7) {
            require(serverState.cursor() == 14 && serverState.first()[8] == 0, "read-only slot placement denied");
            clickSlot(companion, 1, InputConstants.MOUSE_BUTTON_LEFT, 1); stage = 8;
        } else if (stage == 8) {
            require(serverState.first()[1] == 0 && serverState.playerCards() == 1 && serverState.cursor() == 14,
                "Shift-click to the actual player inventory preserves the shared cursor");
            drag(companion, 2, 3); stage = 9;
        } else if (stage == 9) {
            require(serverState.cursor() == 0 && serverState.first()[2] == 7 && serverState.first()[3] == 7, "real server quick-craft drag");
            clickSlot(companion, 2, InputConstants.MOUSE_BUTTON_LEFT, 0); stage = 10;
        } else if (stage == 10) {
            require(serverState.cursor() == 7, "cursor prepared for ownership transition");
            beginSlotDrag(companion, 4, 5);
            require(main(1).handle().close(), "first parent closes during a pending gesture"); stage = 11;
        } else if (stage == 11) {
            endDrag();
            require(mains == 1 && companions.size() == 1 && companion.owners().size() == 1, "remaining parent retains the companion");
            require(serverState.cursor() == 7 && serverState.first()[4] == 0 && serverState.first()[5] == 0,
                "owner closure cancels the pending drag and preserves cursor contents");
            clickSlot(companion, 4, InputConstants.MOUSE_BUTTON_LEFT, 0); stage = 12;
        } else if (stage == 12) {
            require(serverState.cursor() == 0 && serverState.first()[4] == 7, "companion remains usable after first-parent closure");
            clickSlot(companion, 4, InputConstants.MOUSE_BUTTON_LEFT, 0); stage = 13;
        } else if (stage == 13) {
            require(serverState.cursor() == 7, "cursor held during final teardown");
            require(main(2).handle().close(), "last parent closes"); stage = 14;
        } else if (stage == 14) {
            require(mains == 0 && companions.isEmpty(), "last-owner disposal removes the companion");
            require(serverState.cursor() == 7 && serverState.first()[4] == 0, "final teardown preserves real cursor and backing inventory");
            CompanionDemoClient.open(client, 1); stage = 15;
        } else if (stage == 15 && companion != null) {
            require(!handle.instanceId().equals(companionId), "reopening creates a new lifetime over retained storage");
            require(handle.focus() && handle.moveTo(24, 200), "restored real inventory can be focused and dragged");
            clickSlot(companion, 4, InputConstants.MOUSE_BUTTON_LEFT, 0); stage = 16;
        } else if (stage == 16) {
            require(serverState.cursor() == 0 && serverState.first()[4] == 7, "reopened companion accepts the retained cursor");
            Screenshot.grab(client, false); System.out.println("COMPANION NATIVE INVENTORY COMPLETE"); stage = 17;
        } else if (stage == 17) {
            CompanionDemoClient.open(client, 2); CompanionDemoClient.open(client, 3); stage = 18;
        } else if (stage == 18 && mains == 3 && companions.size() == 2) {
            activeSharedId = handle.instanceId();
            require(companions.get(0).owners().size() == 2 && companions.get(1).owners().size() == 1,
                "different real tools produce separate companions while shared owners deduplicate");
            require(!((ToolInventoryMenu) companions.get(0).model()).tool.id.equals(((ToolInventoryMenu) companions.get(1).model()).tool.id),
                "separate source inventories retain separate identities");
            require(main(3).keyboard() && main(3).transfer(), "second automatic companion does not steal owner focus");
            require(main(1).handle().focus(), "main management controls can receive user focus");
            control(main(1), DesktopWindowControl.PIN); stage = 19;
        } else if (stage == 19) {
            require(main(1).handle().state().pinned(), "original-artwork Pin control works");
            control(main(1), DesktopWindowControl.PIN); stage = 20;
        } else if (stage == 20) {
            require(!main(1).handle().state().pinned(), "original-artwork Unpin control works");
            control(main(1), DesktopWindowControl.MINIMIZE); stage = 21;
        } else if (stage == 21) {
            require(main(1).handle().state().minimized() && handle.state().visible(), "one minimized owner leaves shared companion visible");
            require(main(2).handle().request(DesktopWindowAction.MINIMIZE), "second owner minimizes"); stage = 22;
        } else if (stage == 22) {
            require(!handle.state().visible() && handle.isOpen(), "all unavailable owners hide the companion but retain its lifetime");
            control(main(1), DesktopWindowControl.MINIMIZE); stage = 23;
        } else if (stage == 23) {
            require(!main(1).handle().state().minimized() && handle.state().visible(), "original-artwork Restore control restores the shared companion");
            require(handle.state().x() == 24 && handle.state().y() == 200, "hide/restore retains independent companion position");
            require(main(1).handle().focus(), "management-menu owner focused");
            control(main(1), DesktopWindowControl.ELLIPSIS); stage = 24;
        } else if (stage == 24) {
            Screenshot.grab(client, false);
            client.gui.screen().keyPressed(new net.minecraft.client.input.KeyEvent(InputConstants.KEY_ESCAPE, 0, 0));
            require(main(1).handle().isOpen(), "management-menu Escape retains the original owner");
            reloadRevision = companion.resourceRevision(); reload = client.reloadResourcePacks(); stage = 25;
        } else if (stage == 25 && reload.isDone() && client.gui.overlay() == null) {
            reload.join();
            require(companion.resourceRevision() > reloadRevision, "resource reload refreshes retained companion presentation");
            require(serverState.first()[4] == 7 && handle.isOpen(), "resource reload preserves real slots and window lifetime");
            Screenshot.grab(client, false);
            previousWidth = client.getWindow().getScreenWidth(); previousHeight = client.getWindow().getScreenHeight();
            client.getWindow().setWindowed(640, 480); stage = 26;
        } else if (stage == 26) {
            require(handle.state().width() == 176 && handle.state().height() == 52, "constrained viewport preserves original companion GUI pixels");
            Screenshot.grab(client, false);
            client.getWindow().setWindowed(previousWidth, previousHeight); stage = 27;
        } else if (stage == 27) {
            require(main(2).handle().close(), "either parent may close first in a fresh ownership group"); stage = 28;
        } else if (stage == 28) {
            require(companion.owners().size() == 1 && handle.isOpen(), "closing the second parent leaves the first owner's companion usable");
            require(handle.focus(), "remaining companion accepts focus");
            clickSlot(companion, 4, InputConstants.MOUSE_BUTTON_LEFT, 0); stage = 29;
        } else if (stage == 29) {
            require(serverState.cursor() == 7 && serverState.first()[4] == 0, "inventory remains usable after opposite parent closure");
            clickSlot(companion, 4, InputConstants.MOUSE_BUTTON_LEFT, 0); stage = 30;
        } else if (stage == 30) {
            require(serverState.cursor() == 0 && serverState.first()[4] == 7, "cursor placement after opposite closure");
            require(main(1).handle().focus(), "Close button owner focused");
            control(main(1), DesktopWindowControl.CLOSE); stage = 31;
        } else if (stage == 31) {
            require(mains == 1 && companions.size() == 1 && !windows.containsKey(activeSharedId),
                "original-artwork Close disposes the last shared owner and only its companion");
            Screenshot.grab(client, false);
            System.out.println("COMPANION NATIVE LIFECYCLE COMPLETE"); stage = 32;
        } else if (stage == 32) {
            require(main(3).handle().focus(), "keyboard manager owner focused");
            key(InputConstants.KEY_F10, 256); stage = 33;
        } else if (stage == 33) {
            require(controlState(main(3), DesktopWindowControl.PIN).keyboardFocused(), "Alt+F10 exposes public control keyboard focus");
            key(InputConstants.KEY_RETURN, 0); stage = 34;
        } else if (stage == 34) {
            require(main(3).handle().state().pinned() && controlState(main(3), DesktopWindowControl.PIN).selected(), "Enter activates Pin and public selected state");
            key(InputConstants.KEY_RETURN, 0); key(InputConstants.KEY_TAB, 0); stage = 35;
        } else if (stage == 35) {
            require(!main(3).handle().state().pinned() && controlState(main(3), DesktopWindowControl.MINIMIZE).keyboardFocused(), "Tab traverses controls and Enter unpins");
            key(InputConstants.KEY_ESCAPE, 0);
            require(handle.moveTo(40, 200), "companion positioned for foreground overlap");
            require(main(3).handle().moveTo(44, 200) && main(3).handle().focus(), "foreground main window covers companion in normal stacking"); stage = 36;
        } else if (stage == 36) {
            Screenshot.grab(client, false);
            clickSlot(companion, 0, InputConstants.MOUSE_BUTTON_LEFT, 0); stage = 37;
        } else if (stage == 37) {
            require(serverState.cursor() == 0 && serverState.second()[0] == 12, "foreground window blocks companion slot hits");
            require(main(3).handle().moveTo(260, 80), "owner moved away from companion");
            modal = DesktopClientApi.openView(net.minecraft.resources.Identifier.parse("companion_example:qa_modal"), "modal", new Object(),
                net.minecraft.network.chat.Component.literal("Modal acceptance")).orElseThrow(); stage = 38;
        } else if (stage == 38) {
            require(!handle.focus() && !handle.moveTo(50, 200), "modal blocks companion focus and movement");
            clickSlot(companion, 0, InputConstants.MOUSE_BUTTON_LEFT, 0); Screenshot.grab(client, false); stage = 39;
        } else if (stage == 39) {
            require(serverState.cursor() == 0 && serverState.second()[0] == 12, "modal blocks background inventory mutations");
            require(modal.close() && !modal.isOpen() && !modal.request(DesktopWindowAction.PIN), "disposed modal handle cannot issue stale actions");
            require(handle.focus(), "companion usable after modal dismissal");
            clickSlot(companion, 0, InputConstants.MOUSE_BUTTON_LEFT, 0); stage = 40;
        } else if (stage == 40) {
            require(serverState.cursor() == 12 && serverState.second()[0] == 0, "second inventory has its own real contents");
            expired = handle;
            beginSlotDrag(companion, 1, 2); server(player -> CompanionDemoMod.invalidate(player, 1)); stage = 41;
        } else if (stage == 41) {
            endDrag();
            require(companions.isEmpty() && !expired.isOpen() && !expired.request(DesktopWindowAction.PIN), "invalid source disposes companion and stale handle");
            require(serverState.cursor() == 12 && serverState.second()[1] == 0 && serverState.second()[2] == 0,
                "invalid-source cancellation retains cursor and rejects unfinished drag");
            hotbar(3); CompanionDemoClient.open(client, 1); stage = 42;
        } else if (stage == 42 && companion != null) {
            require(serverState.cursor() == 0 && serverState.playerCards() == 13, "held cursor remains usable after source teardown");
            require(handle.focus() && handle.moveTo(40, 200), "first source reopened for replacement check");
            clickSlot(companion, 3, InputConstants.MOUSE_BUTTON_LEFT, 0); stage = 43;
        } else if (stage == 43) {
            require(serverState.cursor() == 7, "replacement test holds a real cursor"); expired = handle;
            server(CompanionDemoMod::replace); stage = 44;
        } else if (stage == 44) {
            require(companions.isEmpty() && !expired.isOpen() && serverState.cursor() == 7, "source item replacement disposes old authorization and preserves cursor");
            hotbar(4); require(main(1).handle().close(), "old replacement owner closes");
            server(CompanionDemoMod::rebind); CompanionDemoClient.open(client, 1); stage = 45;
        } else if (stage == 45 && companion != null) {
            require(serverState.cursor() == 0 && serverState.playerCards() == 20, "replacement cursor returns to actual player inventory");
            require(!handle.instanceId().equals(expired.instanceId()), "replacement source has a fresh companion lifetime");
            require(handle.focus() && handle.moveTo(40, 200), "valid replacement inventory reopened");
            clickSlot(companion, 4, InputConstants.MOUSE_BUTTON_LEFT, 0); stage = 46;
        } else if (stage == 46) {
            require(serverState.cursor() == 7, "world-change test holds the shared cursor");
            worldHandles = windows.values().stream().map(Window::handle).toList();
            server(player -> {
                player.setNoGravity(true);
                player.teleportTo(player.level().getServer().getLevel(net.minecraft.world.level.Level.NETHER), 0, 100, 0, java.util.Set.of(), 0, 0, true);
            }); stage = 47;
        } else if (stage == 47 && client.level.dimension().equals(net.minecraft.world.level.Level.NETHER)) {
            require(worldHandles.stream().noneMatch(DesktopWindowHandle::isOpen), "world change invalidates original window lifetimes");
            require(serverState.total() == 28 && serverState.cursor() + serverState.playerCards() == 27,
                "world teardown conserves cursor contents through normal Minecraft settlement");
            leaseAudit = new CompletableFuture<>();
            server(player -> leaseAudit.complete(CompanionCommonIntegration.openQaLeases())); stage = 48;
        } else if (stage == 48 && leaseAudit.isDone()) {
            require(leaseAudit.join() == 0, "world teardown releases all server ownership references");
            if (serverState.cursor() != 0) hotbar(5);
            CompanionDemoClient.open(client, 1); stage = 49;
        } else if (stage == 49 && companion != null) {
            require(serverState.cursor() == 0 && serverState.total() == 28, "world-transition cursor remains placeable");
            require(!worldHandles.stream().anyMatch(h -> h.instanceId().equals(handle.instanceId())), "new world opens new companion lifetime");
            Screenshot.grab(client, false);
            worldHandles = windows.values().stream().map(Window::handle).toList();
            stage = 55; client.disconnect(new net.minecraft.client.gui.screens.TitleScreen(), false);
        }
        else if (ticks > 5000) throw new IllegalStateException("Timed out waiting for companion acceptance");
    }
    private Window main(int number) {
        return windows.values().stream().filter(w -> w.model() instanceof CompanionMainMenu menu && menu.machineNumber == number).findFirst().orElseThrow();
    }
    private DesktopControlState controlState(Window window, DesktopWindowControl kind) {
        return controls.get(window.handle().instanceId()).stream().filter(state -> state.control() == kind).findFirst().orElseThrow();
    }
    private void key(int key, int modifiers) { Minecraft.getInstance().gui.screen().keyPressed(new net.minecraft.client.input.KeyEvent(key, 0, modifiers)); }
    private void server(java.util.function.Consumer<net.minecraft.server.level.ServerPlayer> action) {
        var client = Minecraft.getInstance(); var server = client.getSingleplayerServer(); var id = client.player.getUUID();
        server.execute(() -> action.accept(server.getPlayerList().getPlayer(id)));
    }
    private void hotbar(int index) {
        var client = Minecraft.getInstance();
        click(client.getWindow().getGuiScaledWidth() / 2.0 - 90 + index * 18 + 9,
            client.getWindow().getGuiScaledHeight() - 12, InputConstants.MOUSE_BUTTON_LEFT, 0);
    }
    private void control(Window window, DesktopWindowControl control) {
        var state = window.handle().state();
        int x = switch (control) { case PIN -> 118; case MINIMIZE -> 132; case ELLIPSIS -> 146; case CLOSE -> 160; default -> throw new IllegalArgumentException(); };
        click(state.x() + x + 5, state.y() + 10, InputConstants.MOUSE_BUTTON_LEFT, 0);
    }
    private void clickSlot(Window window, int slot, int button, int modifiers) {
        var s = window.handle().state();
        click(s.x() + 8 + slot * 18 + 8, s.y() + 27 + 8, button, modifiers);
    }
    private void click(double x, double y, int button, int modifiers) {
        var screen = Minecraft.getInstance().gui.screen();
        var event = new MouseButtonEvent(x, y, new MouseButtonInfo(button, modifiers));
        screen.mouseClicked(event, false); screen.mouseReleased(event);
    }
    private void drag(Window window, int first, int second) { beginSlotDrag(window, first, second); endDrag(); }
    private void dragTitle(Window window, int dx, int dy) {
        var client = Minecraft.getInstance(); var w = client.getWindow(); var s = window.handle().state();
        double sx = (double) w.getScreenWidth() / w.getGuiScaledWidth(), sy = (double) w.getScreenHeight() / w.getGuiScaledHeight();
        double x = s.x() + 40, y = s.y() + 10;
        focusWindow(true);
        client.mouseHandler.onMove(w.handle(), x * sx, y * sy, 0, 0); client.mouseHandler.handleAccumulatedMovement();
        client.mouseHandler.onButton(w.handle(), new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, 0), InputConstants.PRESS);
        client.mouseHandler.onMove(w.handle(), (x + dx) * sx, (y + dy) * sy, dx * sx, dy * sy); client.mouseHandler.handleAccumulatedMovement();
        endDrag();
    }
    private void beginSlotDrag(Window window, int first, int second) {
        var client = Minecraft.getInstance(); var w = client.getWindow(); var s = window.handle().state();
        double sx = (double) w.getScreenWidth() / w.getGuiScaledWidth(), sy = (double) w.getScreenHeight() / w.getGuiScaledHeight();
        double x = s.x() + 8 + first * 18 + 8, y = s.y() + 35;
        focusWindow(true);
        client.mouseHandler.onMove(w.handle(), x * sx, y * sy, 0, 0); client.mouseHandler.handleAccumulatedMovement();
        client.mouseHandler.onButton(w.handle(), new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, 0), InputConstants.PRESS);
        double end = s.x() + 8 + second * 18 + 8;
        client.mouseHandler.onMove(w.handle(), end * sx, y * sy, (end - x) * sx, 0); client.mouseHandler.handleAccumulatedMovement();
    }
    private void endDrag() {
        var client = Minecraft.getInstance();
        client.mouseHandler.onButton(client.getWindow().handle(), new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, 0), InputConstants.RELEASE);
        focusWindow(false);
    }
    private void focusWindow(boolean focused) {
        try {
            var window = Minecraft.getInstance().getWindow();
            var method = window.getClass().getDeclaredMethod("onFocus", boolean.class); method.setAccessible(true); method.invoke(window, focused);
        } catch (ReflectiveOperationException failure) { throw new IllegalStateException(failure); }
    }
    private static void require(boolean condition, String invariant) {
        if (!condition) throw new IllegalStateException(invariant);
        System.out.println("COMPANION NATIVE PASS: " + invariant);
    }
}
