package com.salts_inventory_update.client;

import com.salts_inventory_update.api.client.desktop.DesktopInputContext;
import com.salts_inventory_update.api.client.desktop.DesktopClientApi;
import com.salts_inventory_update.client.automatic.AutomaticNativeScope;
import com.salts_inventory_update.internal.desktop.DesktopItemSourceLocks;
import com.salts_inventory_update.internal.desktop.PhysicalPlayerInventory;
import com.salts_inventory_update.network.DesktopPackets;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import org.jspecify.annotations.Nullable;

/** Full native presentation of a live desktop menu. The authenticated cursor lane
 * stays in charge; native clicks never target an unrelated vanilla server menu. */
public final class NativeContainerView {
    public record Input(AbstractContainerScreen<?> screen, DesktopInputContext<?, ?> context) {
        public int sessionId() {
            var player = Minecraft.getInstance().player;
            return player != null && screen.getMenu() == player.inventoryMenu ? DesktopPackets.PLAYER_MENU_SESSION : context.sessionId();
        }
    }
    private static @Nullable NativeContainerView active;
    private static @Nullable Screen retiring;
    private static @Nullable NativeContainerView retiringView;
    private static boolean switching;
    private final InventoryDesktopScreen desktop;
    private final AbstractContainerScreen<?> screen;
    private final AbstractContainerMenu previousMenu;
    private final DesktopInputContext<?, ?> context;

    private NativeContainerView(InventoryDesktopScreen desktop, AbstractContainerScreen<?> screen,
            AbstractContainerMenu previousMenu, DesktopInputContext<?, ?> context) {
        this.desktop = desktop; this.screen = screen; this.previousMenu = previousMenu; this.context = context;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static @Nullable AbstractContainerScreen<?> create(Minecraft mc, AbstractContainerMenu menu, Component title,
            @Nullable DesktopContainerSession session) {
        if (mc.player == null) return null;
        if (menu == mc.player.inventoryMenu) return new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player);
        var inventory = PhysicalPlayerInventory.get(mc.player);
        if (session != null && menu instanceof net.minecraft.world.inventory.HorseInventoryMenu horse
                && session.mountEntity(mc) instanceof net.minecraft.world.entity.animal.equine.AbstractHorse entity)
            return new net.minecraft.client.gui.screens.inventory.HorseInventoryScreen(horse, inventory, entity, session.columns());
        if (session != null && menu instanceof net.minecraft.world.inventory.NautilusInventoryMenu nautilus
                && session.mountEntity(mc) instanceof net.minecraft.world.entity.animal.nautilus.AbstractNautilus entity)
            return new net.minecraft.client.gui.screens.inventory.NautilusInventoryScreen(nautilus, inventory, entity, session.columns());
        var registered = DesktopClientApi.createNativeScreen(menu, inventory, title);
        if (registered != null) return registered;
        var factory = InventoryDesktopScreen.nativeContainerConstructor(menu.getType());
        if (factory == null) return null;
        Object screen = ((net.minecraft.client.gui.screens.MenuScreens.ScreenConstructor)factory).create(menu, inventory, title);
        return screen instanceof AbstractContainerScreen<?> container ? container : null;
    }

    public static void open(Minecraft mc, InventoryDesktopScreen desktop, AbstractContainerScreen<?> screen,
            DesktopInputContext<?, ?> context) {
        if (mc.player == null || active != null) return;
        active = new NativeContainerView(desktop, screen, mc.player.containerMenu, context);
        mc.player.containerMenu = screen.getMenu();
        switching = true;
        try { mc.setScreen(screen); }
        catch (RuntimeException failure) {
            var previous = active.previousMenu;
            retiring = screen; retiringView = active; active = null;
            mc.player.containerMenu = previous;
            try { mc.setScreen(desktop); }
            finally { retiring = null; retiringView = null; }
            throw failure;
        }
        finally { switching = false; }
    }

    public static boolean isActive(Minecraft mc) { return active != null && mc.screen == active.screen; }
    public static boolean owns(Object screen) { return active != null && active.screen == screen || retiring != null && retiring == screen; }

    /** Player and mount menus may have no registered type. Their ordinary packets
     * must not enter a type-keyed native-template handler lookup. */
    public static @Nullable MenuType<?> registeredMenuType(AbstractContainerMenu menu) {
        try { return menu.getType(); }
        catch (UnsupportedOperationException untypedMenu) { return null; }
    }

    public static @Nullable Input input() {
        var scope = AutomaticNativeScope.current();
        if (scope != null && scope.input() != null) return new Input(scope.screen(), scope.input());
        var mc = Minecraft.getInstance();
        var view = retiringView != null ? retiringView : isActive(mc) ? active : null;
        var input = view != null ? new Input(view.screen, view.context) : null;
        return input != null && input.sessionId() >= 0 ? input : null;
    }

    public static void restore(Minecraft mc) {
        NativeContainerView view = active;
        if (view == null) return;
        active = null;
        retiring = view.screen;
        retiringView = view;
        switching = true;
        if (mc.player != null) mc.player.containerMenu = view.previousMenu;
        try {
            if (mc.player != null && InventoryDesktopScreen.current(mc) == view.desktop)
                mc.setScreen(view.desktop.hasTopTabWindows() ? view.desktop : null);
        } finally { retiring = null; retiringView = null; switching = false; }
    }

    /** Gui changes from a native back/close button return to the saved workspace. */
    public static boolean changingScreen(Minecraft mc, @Nullable Screen incoming) {
        if (active == null || switching) return false;
        if (incoming == null && isActive(mc)) { restore(mc); return true; }
        if (incoming == active.desktop) {
            if (mc.player != null) mc.player.containerMenu = active.previousMenu;
            retiring = active.screen;
            retiringView = active;
            active = null;
        }
        return false;
    }

    public static void changedScreen() { if (!switching) { retiring = null; retiringView = null; } }

    public static void tick(Minecraft mc) {
        if (active == null) return;
        if (mc.player == null || mc.level == null || InventoryDesktopScreen.current(mc) != active.desktop) { reset(mc); return; }
        if (active.context.sessionId() > DesktopPackets.PLAYER_MENU_SESSION
                && InventoryDesktopScreen.sessionForNetworking(mc, active.context.sessionId()) == null) restore(mc);
    }

    public static void reset(Minecraft mc) {
        var view = active;
        boolean visible = view != null && mc.screen == view.screen;
        if (view != null && mc.player != null && mc.player.containerMenu == view.screen.getMenu())
            mc.player.containerMenu = mc.player.inventoryMenu;
        active = null;
        retiring = view == null ? null : view.screen;
        retiringView = view;
        switching = true;
        try { if (visible) mc.setScreen(null); }
        finally { retiring = null; retiringView = null; switching = false; DesktopTopTab.reset(); }
    }

    /** Cancel vanilla prediction and use the existing serialized, server-validated native click. */
    public static boolean click(Minecraft mc, int menuId, int slotId, int button, ClickType input) {
        if (!isActive(mc) || active.screen.getMenu().containerId != menuId || mc.player == null) return false;
        AbstractContainerMenu menu = active.screen.getMenu();
        var target = input();
        if (target == null) return false; // Legacy menus already use the ordinary native wire protocol.
        if (!DesktopItemSourceLocks.shouldRejectClick(mc.player, menu, slotId, button, input, menu.getCarried()))
            DesktopContainerClient.clickSlot(0, target.sessionId(), slotId, button, input, menu.getCarried());
        return true;
    }
}
