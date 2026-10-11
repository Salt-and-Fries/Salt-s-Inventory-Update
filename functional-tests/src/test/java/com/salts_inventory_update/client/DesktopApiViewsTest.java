package com.salts_inventory_update.client;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.salts_inventory_update.api.client.desktop.DesktopInputContext;
import com.salts_inventory_update.api.client.desktop.DesktopWindowContext;
import com.salts_inventory_update.api.client.desktop.DesktopWindowDefinition;
import com.salts_inventory_update.api.client.desktop.DesktopWindowOptions;
import com.salts_inventory_update.api.client.desktop.DesktopWindowPresentation;
import com.salts_inventory_update.api.client.desktop.DesktopWindowRegion;
import com.salts_inventory_update.api.client.desktop.DesktopWindowSurface;
import com.salts_inventory_update.api.client.desktop.DesktopHitRole;
import com.salts_inventory_update.api.client.desktop.DesktopInsets;
import com.salts_inventory_update.api.client.desktop.DesktopSurfaceMode;
import com.salts_inventory_update.api.client.desktop.DesktopViewportPolicy;
import com.salts_inventory_update.api.client.desktop.DesktopChromeMetrics;
import com.salts_inventory_update.api.client.desktop.DesktopWindowDecoration;
import com.salts_inventory_update.api.client.desktop.DesktopWindowAction;
import com.salts_inventory_update.api.client.desktop.DesktopWindowSize;
import com.salts_inventory_update.api.client.desktop.DesktopPopup;
import com.salts_inventory_update.api.client.desktop.widget.DesktopWidgetHost;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Runs the actual desktop view contexts and routing without a game window or a network connection. */
final class DesktopApiViewsTest {
    private static sun.misc.Unsafe unsafe;

    @BeforeAll
    static void bootstrap() throws Exception {
        // These tests run without a loader launch, but use the real window constructor.
        Class<?> loaderType = Class.forName("net.fabricmc.loader.impl.FabricLoaderImpl");
        Object loader = loaderType.getField("INSTANCE").get(null);
        Field configDir = loaderType.getDeclaredField("configDir");
        configDir.setAccessible(true);
        java.nio.file.Path configPath = java.nio.file.Path.of(System.getProperty("user.dir"), "api-view-config");
        java.nio.file.Files.createDirectories(configPath);
        configDir.set(loader, configPath);
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        Field field = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        unsafe = (sun.misc.Unsafe) field.get(null);
    }

    @Test
    void clientViewRetainsItsOwnModelAndCannotPerformInventoryActions() throws Exception {
        InventoryDesktopScreen screen = screen();
        Object model = new Object();
        Object window = view(screen, model, definition(false, new AtomicInteger(), new AtomicInteger()));
        DesktopInputContext<?, ?> context = context(screen, window);
        assertSame(model, context.model());
        assertFalse(context.inventoryAvailable());
        assertEquals(Integer.MIN_VALUE, context.sessionId());
        assertTrue(context.containerSlots().isEmpty());
        assertNull(context.menuSlot(0));
        assertNull(context.containerSlot(0));
        assertTrue(context.carriedStack().isEmpty());
        assertFalse(context.sendMenuButton(12));
        assertFalse(context.sendRename("should never reach networking"));
        assertFalse(context.quickMoveSlot(0));
        assertFalse(context.toggleRecipeBook());
        Method rawPayload = List.of(DesktopInputContext.class.getMethods()).stream()
            .filter(method -> method.getName().equals("sendPayload") && method.getParameterTypes().length == 2)
            .findFirst().orElseThrow();
        assertEquals(false, rawPayload.invoke(context, null, new byte[]{1}), "No network send is attempted");
        assertTrue(context.windowHandle().isOpen());
    }

    @Test
    void clientViewReceivesTickWithoutAContainerMenu() throws Exception {
        InventoryDesktopScreen screen = screen();
        AtomicInteger ticks = new AtomicInteger();
        Object window = view(screen, new Object(), definition(false, ticks, new AtomicInteger()));
        assertEquals(true, invoke(screen, "apiTick", window));
        assertEquals(1, ticks.get());
    }

    @Test
    void keyboardInputGoesOnlyToItsOwnerRegardlessOfWindowStacking() throws Exception {
        InventoryDesktopScreen screen = screen();
        AtomicInteger firstKeys = new AtomicInteger();
        AtomicInteger secondKeys = new AtomicInteger();
        Object first = view(screen, new Object(), definition(false, new AtomicInteger(), firstKeys));
        view(screen, new Object(), definition(false, new AtomicInteger(), secondKeys));
        set(screen, "apiKeyboardFocusWindow", first);
        assertEquals(true, invoke(screen, "apiKeyPressed", (Object) null));
        assertEquals(1, firstKeys.get());
        assertEquals(0, secondKeys.get());
        assertTrue(context(screen, first).keyboardFocused());
        assertFalse(context(screen, first).focused(), "Keyboard focus does not select a transfer target");
    }

    @Test
    void focusingClientViewLeavesTheInventoryTransferTargetSelected() throws Exception {
        InventoryDesktopScreen screen = screen();
        Object target = view(screen, new Object(), definition(false, new AtomicInteger(), new AtomicInteger()));
        set(target, "focused", true);
        Object clientView = view(screen, new Object(), definition(false, new AtomicInteger(), new AtomicInteger()));
        invoke(screen, "setFocusedWindow", clientView);
        assertTrue(context(screen, target).focused());
        assertFalse(context(screen, clientView).focused());
        assertTrue(context(screen, clientView).keyboardFocused());
    }

    @Test
    void modalClientViewBlocksSlotsAndPointerTargetsBehindItsRectangle() throws Exception {
        InventoryDesktopScreen screen = screen();
        Object modal = view(screen, new Object(), definition(true, new AtomicInteger(), new AtomicInteger()));
        assertSame(modal, invoke(screen, "windowAt", 1000.0D, 1000.0D));
        assertNull(invoke(screen, "slotAt", 1000.0D, 1000.0D), "A modal view cannot fall through to hotbar/player slots");
        set(modal, "ghosted", true);
        assertNull(invoke(screen, "apiModalWindow"), "Ghost previews cannot hold desktop modality");
    }

    @Test
    void clientViewExternalRegionsParticipateInDesktopHitTesting() throws Exception {
        InventoryDesktopScreen screen = screen();
        Object window = view(screen, new Object(), definition(false, new AtomicInteger(), new AtomicInteger(), true));
        assertSame(window, invoke(screen, "windowAt", -10.0D, 20.0D));
        assertNull(invoke(screen, "windowAt", -40.0D, 20.0D));
        set(window, "minimized", true);
        assertNull(invoke(screen, "windowAt", -10.0D, 20.0D), "Minimized panels must not intercept external controls");
    }

    @Test
    void eachViewInstanceHasAnIndependentHandle() throws Exception {
        InventoryDesktopScreen screen = screen();
        Object first = view(screen, new Object(), definition(false, new AtomicInteger(), new AtomicInteger()));
        Object second = view(screen, new Object(), definition(false, new AtomicInteger(), new AtomicInteger()));
        var firstHandle = context(screen, first).windowHandle();
        var secondHandle = context(screen, second).windowHandle();
        assertNotEquals(firstHandle.instanceId(), secondHandle.instanceId());
        windows(screen).remove(first);
        assertFalse(firstHandle.isOpen());
        assertTrue(secondHandle.isOpen());
    }

    @Test
    void disposalInvalidatesTheHandleBeforeCallingClosedAndIsIdempotent() throws Exception {
        InventoryDesktopScreen screen = screen();
        AtomicInteger closes = new AtomicInteger();
        Object definition = Proxy.newProxyInstance(DesktopWindowDefinition.class.getClassLoader(),
            new Class<?>[]{DesktopWindowDefinition.class}, (proxy, method, arguments) -> {
                if (method.getName().equals("surface")) return DesktopWindowSurface.SALT;
                if (method.getName().equals("closed")) {
                    DesktopWindowContext<?, ?> context = (DesktopWindowContext<?, ?>) arguments[0];
                    assertFalse(context.windowHandle().isOpen());
                    assertFalse(context.inventoryAvailable());
                    closes.incrementAndGet();
                }
                return method.getReturnType() == boolean.class ? false : null;
            });
        Object window = view(screen, new Object(), definition);
        invoke(screen, "apiClosed", window);
        invoke(screen, "apiClosed", window);
        assertEquals(1, closes.get());
        assertFalse(context(screen, window).windowHandle().isOpen());
    }

    @Test
    void modOwnedSurfaceHasNoReservedInsetsOrInvisibleNativeControls() throws Exception {
        InventoryDesktopScreen screen = screen();
        Object window = view(screen, new Object(), surfaceDefinition(DesktopWindowSurface.modOwned(List.of(
            DesktopWindowRegion.visualAndInteractive(0, 0, 120, 100),
            DesktopWindowRegion.interactive(0, 0, 120, 16).withRole(DesktopHitRole.MOVE)))));
        var context = context(screen, window);
        assertEquals(context.windowX(), context.contentX());
        assertEquals(context.windowY(), context.contentY());
        assertEquals(context.windowWidth(), context.contentWidth());
        assertEquals(context.windowHeight(), context.contentHeight());
        assertEquals(false, invoke(window, "isTopBar", 50.0, 5.0));
        assertEquals(false, invoke(window, "resizeGripAt", 115.0, 95.0));
        assertEquals(DesktopHitRole.MOVE, context.surface().hitRole(0, 0, 120, 100, 50, 5));
    }

    @Test
    void cutoutRoutesToUnderlyingWindowWhileModalStillBlocksIt() throws Exception {
        InventoryDesktopScreen screen = screen();
        Object background = view(screen, new Object(), definition(false, new AtomicInteger(), new AtomicInteger()));
        Object foreground = view(screen, new Object(), surfaceDefinition(DesktopWindowSurface.modOwned(List.of(
            DesktopWindowRegion.visualAndInteractive(0, 0, 120, 50),
            DesktopWindowRegion.visualAndInteractive(0, 50, 70, 50)))));
        assertSame(foreground, invoke(screen, "windowAt", 80.0, 20.0));
        assertSame(background, invoke(screen, "windowAt", 90.0, 70.0));
        set(foreground, "apiDefinition", definition(true, new AtomicInteger(), new AtomicInteger()));
        assertSame(foreground, invoke(screen, "windowAt", 90.0, 70.0));
        assertNull(invoke(screen, "slotAt", 90.0, 70.0));
    }

    @Test
    void surfaceQueriesAreCommittedRatherThanRepeatedDuringHitTesting() throws Exception {
        InventoryDesktopScreen screen = screen();
        AtomicInteger queries = new AtomicInteger();
        Object definition = Proxy.newProxyInstance(DesktopWindowDefinition.class.getClassLoader(),
            new Class<?>[]{DesktopWindowDefinition.class}, (proxy, method, arguments) -> {
                if (method.getName().equals("surface")) {
                    queries.incrementAndGet();
                    return new DesktopWindowSurface(DesktopSurfaceMode.MOD_OWNED, new DesktopInsets(2, 3, 4, 5),
                        true, List.of(), DesktopViewportPolicy.PRESERVE);
                }
                return method.getReturnType() == boolean.class ? false : null;
            });
        Object window = view(screen, new Object(), definition);
        assertEquals(2, context(screen, window).contentX());
        assertEquals(3, context(screen, window).contentY());
        int committedQueries = queries.get();
        assertTrue(committedQueries > 0);
        for (int i = 0; i < 5; i++) invoke(screen, "windowAt", 10.0, 10.0);
        assertEquals(committedQueries, queries.get());
    }

    @Test
    void nativePreparationFinishesBeforeHitRegionsAreCommitted() throws Exception {
        InventoryDesktopScreen screen = screen();
        java.util.concurrent.atomic.AtomicBoolean prepared = new java.util.concurrent.atomic.AtomicBoolean();
        Object definition = Proxy.newProxyInstance(DesktopWindowDefinition.class.getClassLoader(),
            new Class<?>[]{DesktopWindowDefinition.class}, (proxy, method, arguments) -> {
                if (method.getName().equals("prepareLayout")) {
                    DesktopWindowContext<?, ?> candidate = (DesktopWindowContext<?, ?>) arguments[0];
                    assertEquals(0, candidate.contentX());
                    assertEquals(0, candidate.contentY());
                    prepared.set(true);
                    return null;
                }
                if (method.getName().equals("surface")) {
                    return DesktopWindowSurface.modOwned(List.of(
                        DesktopWindowRegion.interactive(prepared.get() ? 80 : 0, 0, 10, 10)));
                }
                return method.getReturnType() == boolean.class ? false : null;
            });
        Object window = view(screen, new Object(), definition);
        assertSame(window, invoke(screen, "windowAt", 85.0, 5.0));
        assertNull(invoke(screen, "windowAt", 5.0, 5.0), "The prior widget position must not remain interactive");
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void customNativeChromeUsesItsPublishedMetricsForHitTesting() throws Exception {
        InventoryDesktopScreen screen = screen();
        DesktopWindowDecoration decoration = new DesktopWindowDecoration() {
            public DesktopChromeMetrics metrics() { return new DesktopChromeMetrics(30, 3, 4, 20, 16, 4, 2, 8, 7); }
            public void render(com.salts_inventory_update.api.client.desktop.DesktopRenderContext context, List controls) { }
        };
        Object definition = Proxy.newProxyInstance(DesktopWindowDefinition.class.getClassLoader(),
            new Class<?>[]{DesktopWindowDefinition.class}, (proxy, method, arguments) -> switch (method.getName()) {
                case "surface" -> new DesktopWindowSurface(DesktopSurfaceMode.SALT, new DesktopInsets(3, 34, 3, 3),
                    true, List.of(), DesktopViewportPolicy.CLAMP);
                case "decoration" -> decoration;
                case "options" -> DesktopWindowOptions.DEFAULT;
                default -> method.getReturnType() == boolean.class ? false : null;
            });
        Object window = view(screen, new Object(), definition);
        assertEquals(34, context(screen, window).contentY());
        assertEquals(true, invoke(window, "isTopBar", 50.0, 25.0));
        assertEquals(false, invoke(window, "isTopBar", 50.0, 31.0));
        Class<?> controls = Class.forName(InventoryDesktopScreen.class.getName() + "$WindowControl");
        Object close = Enum.valueOf((Class) controls, "CLOSE");
        List<?> rectangles = (List<?>) invoke(screen, "controlRects", window, List.of(close));
        Object rectangle = rectangles.get(0);
        int x = (int) invoke(rectangle, "x");
        int y = (int) invoke(rectangle, "y");
        assertEquals(20, invoke(rectangle, "width"));
        assertEquals(16, invoke(rectangle, "height"));
        assertEquals(true, invoke(rectangle, "contains", x + 18.0, y + 14.0));
        assertEquals(false, invoke(rectangle, "contains", x + 20.0, y + 14.0));
    }

    @Test
    void closingFromOpenedDoesNotPrepareOrNotifyAfterClosed() throws Exception {
        InventoryDesktopScreen screen = screen();
        List<String> events = new ArrayList<>();
        java.util.concurrent.atomic.AtomicBoolean closed = new java.util.concurrent.atomic.AtomicBoolean();
        Object[] owner = new Object[1];
        Object definition = Proxy.newProxyInstance(DesktopWindowDefinition.class.getClassLoader(),
            new Class<?>[]{DesktopWindowDefinition.class}, (proxy, method, arguments) -> {
                assertFalse(closed.get(), "No callback may run after closed: " + method.getName());
                switch (method.getName()) {
                    case "surface": return DesktopWindowSurface.SALT;
                    case "options": return DesktopWindowOptions.DEFAULT;
                    case "opened": invoke(screen, "apiClosed", owner[0]); return null;
                    case "windowStateChanged":
                        if (!((com.salts_inventory_update.api.client.desktop.DesktopWindowState) arguments[1]).open()) events.add("disposed");
                        return null;
                    case "closed": events.add("closed"); closed.set(true); return null;
                    default: return method.getReturnType() == boolean.class ? false : null;
                }
            });
        owner[0] = view(screen, new Object(), definition);
        invoke(screen, "apiOpened", owner[0]);
        assertEquals(List.of("disposed", "closed"), events);
        assertFalse(context(screen, owner[0]).windowHandle().isOpen());
    }

    @Test
    void closingFromKeyboardInputDoesNotCommitAnotherLayout() throws Exception {
        InventoryDesktopScreen screen = screen();
        java.util.concurrent.atomic.AtomicBoolean closed = new java.util.concurrent.atomic.AtomicBoolean();
        AtomicInteger postCloseCallbacks = new AtomicInteger();
        Object[] owner = new Object[1];
        Object definition = Proxy.newProxyInstance(DesktopWindowDefinition.class.getClassLoader(),
            new Class<?>[]{DesktopWindowDefinition.class}, (proxy, method, arguments) -> {
                if (closed.get()) postCloseCallbacks.incrementAndGet();
                if (method.getName().equals("surface")) return DesktopWindowSurface.SALT;
                if (method.getName().equals("keyPressed")) { invoke(screen, "apiClosed", owner[0]); return true; }
                if (method.getName().equals("closed")) closed.set(true);
                return method.getReturnType() == boolean.class ? false : null;
            });
        owner[0] = view(screen, new Object(), definition);
        set(screen, "apiKeyboardFocusWindow", owner[0]);
        assertEquals(true, invoke(screen, "apiKeyPressed", (Object) null));
        assertTrue(closed.get());
        assertEquals(0, postCloseCallbacks.get());
    }

    @Test
    void changingLayoutRevisionDiscardsOldPreferencesAndGeometry() throws Exception {
        InventoryDesktopScreen screen = screen();
        java.util.concurrent.atomic.AtomicReference<CompoundTag> loaded = new java.util.concurrent.atomic.AtomicReference<>();
        Object definition = Proxy.newProxyInstance(DesktopWindowDefinition.class.getClassLoader(),
            new Class<?>[]{DesktopWindowDefinition.class}, (proxy, method, arguments) -> switch (method.getName()) {
                case "surface" -> DesktopWindowSurface.SALT;
                case "layoutRevision" -> "layout-2";
                case "loadLocalState" -> { loaded.set((CompoundTag) arguments[1]); yield null; }
                default -> method.getReturnType() == boolean.class ? false : null;
            });
        Object window = view(screen, new Object(), definition);
        set(window, "apiInitialSize", DesktopWindowSize.of(120, 100));
        set(window, "width", 260);
        var old = new CompoundTag();
        old.putString("salts:layout_revision", "layout-1");
        old.putString("old-position-dependent-filter", "obsolete");
        invoke(screen, "apiLoadLocalState", window, old);
        assertTrue(loaded.get().isEmpty());
        assertEquals(120, context(screen, window).windowWidth());
        CompoundTag saved = (CompoundTag) invoke(screen, "apiSaveLocalState", window);
        Object revision = invoke(saved, "getString", "salts:layout_revision");
        if (revision instanceof java.util.Optional<?> optional) revision = optional.orElse(null);
        assertEquals("layout-2", revision);
    }

    @Test
    void viewportAndResourceChangesNotifyOncePerEnvironment() throws Exception {
        InventoryDesktopScreen screen = screen();
        AtomicInteger notifications = new AtomicInteger();
        Object definition = Proxy.newProxyInstance(DesktopWindowDefinition.class.getClassLoader(),
            new Class<?>[]{DesktopWindowDefinition.class}, (proxy, method, arguments) -> {
                if (method.getName().equals("surface")) return DesktopWindowSurface.SALT;
                if (method.getName().equals("environmentChanged")) notifications.incrementAndGet();
                return method.getReturnType() == boolean.class ? false : null;
            });
        Object window = view(screen, new Object(), definition);
        invoke(screen, "notifyApiEnvironment", window);
        invoke(screen, "notifyApiEnvironment", window);
        assertEquals(1, notifications.get());
        Field width = net.minecraft.client.gui.screens.Screen.class.getDeclaredField("width");
        width.setAccessible(true);
        width.setInt(screen, 400);
        invoke(screen, "notifyApiEnvironment", window);
        assertEquals(2, notifications.get());
        InventoryDesktopScreen.resourcesReloaded();
        invoke(screen, "notifyApiEnvironment", window);
        assertEquals(3, notifications.get());
        assertEquals(400, context(screen, window).environment().viewportWidth());
    }

    @Test
    void modOwnedWindowCanOpenManagerControlsWithoutNativeChrome() throws Exception {
        InventoryDesktopScreen screen = screen();
        Object window = view(screen, new Object(), surfaceDefinition(DesktopWindowSurface.modOwned(List.of(
            DesktopWindowRegion.interactive(0, 0, 120, 100)))));
        assertEquals(true, invoke(screen, "requestApiWindowAction", window, DesktopWindowAction.MANAGEMENT_MENU));
        Field popup = InventoryDesktopScreen.class.getDeclaredField("popupWindow");
        popup.setAccessible(true);
        assertSame(window, popup.get(screen));
        assertEquals(false, invoke(window, "isTopBar", 50.0, 5.0));
        screen.width = 320; screen.height = 240;
        Object rect = invoke(screen, "popupRect", window);
        assertNotNull(rect, "An accepted management request must produce visible, interactive controls");
        int x = (int) invoke(rect, "x");
        int y = (int) invoke(rect, "y");
        assertTrue(x >= 0 && y >= 0);
        assertNotNull(invoke(screen, "popupControlAt", window, (double) x + 5, (double) y + 5));
        Method keyPressed = List.of(InventoryDesktopScreen.class.getDeclaredMethods()).stream()
            .filter(method -> method.getName().equals("keyPressed") && method.getParameterCount() == 1).findFirst().orElseThrow();
        int escapeKey = 256;
        try { escapeKey = com.mojang.blaze3d.platform.InputConstants.class.getField("KEY_ESCAPE").getInt(null); }
        catch (NoSuchFieldException preSdl) { }
        Object escape = keyPressed.getParameterTypes()[0].getDeclaredConstructor(int.class, int.class, int.class)
            .newInstance(escapeKey, 0, 0);
        assertEquals(true, keyPressed.invoke(screen, escape));
        assertNull(popup.get(screen), "Escape dismisses management before reaching the owner");
        assertTrue(context(screen, window).windowHandle().isOpen());
    }

    @Test
    void authorFallbackKeepsPixelsAndNotifiesOncePerUnavailableViewport() throws Exception {
        InventoryDesktopScreen screen = screen();
        screen.width = 64; screen.height = 64;
        AtomicInteger unavailable = new AtomicInteger();
        Object provider = Proxy.newProxyInstance(DesktopWindowDefinition.class.getClassLoader(),
            new Class<?>[]{DesktopWindowDefinition.class}, (proxy, method, arguments) -> switch (method.getName()) {
                case "surface" -> new DesktopWindowSurface(DesktopSurfaceMode.MOD_OWNED, DesktopInsets.ZERO,
                    true, List.of(), DesktopViewportPolicy.ADAPT);
                case "surfaceUnavailable" -> { unavailable.incrementAndGet(); yield null; }
                case "options" -> DesktopWindowOptions.DEFAULT;
                default -> method.getReturnType() == boolean.class ? false : null;
            });
        Object window = view(screen, new Object(), provider);
        assertEquals(1, unavailable.get());
        invoke(screen, "commitApiSurface", window);
        assertEquals(1, unavailable.get());
        assertEquals(120, context(screen, window).windowWidth());
        screen.width = 60;
        invoke(screen, "commitApiSurface", window);
        assertEquals(2, unavailable.get());
        screen.width = 400; screen.height = 300;
        invoke(screen, "commitApiSurface", window);
        screen.width = 64; screen.height = 64;
        invoke(screen, "commitApiSurface", window);
        assertEquals(3, unavailable.get());
    }

    @Test
    void popupDismissalRemovesItsModalityAndCannotDeliverTwice() throws Exception {
        InventoryDesktopScreen screen = screen();
        var popup = new DesktopPopup("menu", List.of(DesktopWindowRegion.visualAndInteractive(20, 20, 40, 30)),
            true, true, true, true);
        AtomicInteger dismissed = new AtomicInteger();
        Object provider = Proxy.newProxyInstance(DesktopWindowDefinition.class.getClassLoader(),
            new Class<?>[]{DesktopWindowDefinition.class}, (proxy, method, arguments) -> switch (method.getName()) {
                case "surface" -> DesktopWindowSurface.modOwned(List.of(DesktopWindowRegion.visualAndInteractive(0, 0, 120, 100)));
                case "popups" -> dismissed.get() == 0 ? List.of(popup) : List.of();
                case "dismissPopup" -> { dismissed.incrementAndGet(); yield true; }
                case "options" -> DesktopWindowOptions.DEFAULT;
                default -> method.getReturnType() == boolean.class ? false : null;
            });
        Object window = view(screen, new Object(), provider);
        assertSame(window, invoke(screen, "apiModalWindow"));
        assertEquals(DesktopHitRole.POPUP, context(screen, window).surface().hitRole(0, 0, 120, 100, 25, 25));
        set(window, "persistentHidden", true);
        assertNull(invoke(screen, "apiModalWindow"));
        assertEquals(false, invoke(screen, "dismissApiOutsidePress", 1000.0, 1000.0),
            "A retained hidden popup cannot swallow background input");
        assertEquals(0, dismissed.get());
        set(window, "persistentHidden", false);
        assertEquals(true, invoke(screen, "dismissApiEscape", window));
        assertEquals(1, dismissed.get());
        assertNull(invoke(screen, "apiModalWindow"));
        assertTrue(context(screen, window).popups().isEmpty());
        assertEquals(false, invoke(screen, "dismissApiEscape", window));
        assertEquals(1, dismissed.get());
    }

    @Test
    void closingFromDecorationQuerySkipsMetricsAndFinalCommit() throws Exception {
        InventoryDesktopScreen screen = screen();
        AtomicInteger metrics = new AtomicInteger();
        Object provider = Proxy.newProxyInstance(DesktopWindowDefinition.class.getClassLoader(),
            new Class<?>[]{DesktopWindowDefinition.class}, (proxy, method, arguments) -> switch (method.getName()) {
                case "surface" -> DesktopWindowSurface.SALT;
                case "decoration" -> {
                    invoke(screen, "apiClosed", windows(screen).get(0));
                    yield new DesktopWindowDecoration<Object, Object>() {
                        @Override public DesktopChromeMetrics metrics() { metrics.incrementAndGet(); return DesktopChromeMetrics.SALT; }
                        @Override public void render(com.salts_inventory_update.api.client.desktop.DesktopRenderContext<Object, Object> context,
                                List<com.salts_inventory_update.api.client.desktop.DesktopControlState> controls) { }
                    };
                }
                case "options" -> DesktopWindowOptions.DEFAULT;
                default -> method.getReturnType() == boolean.class ? false : null;
            });
        Object window = view(screen, new Object(), provider);
        assertFalse(context(screen, window).windowHandle().isOpen());
        assertEquals(0, metrics.get());
    }

    @Test
    void nativeWidgetTraversalSkipsUnavailableWidgetsAndRetainsSemanticNarration() {
        var first = net.minecraft.client.gui.components.Button.builder(Component.literal("First"), button -> { }).bounds(0, 0, 80, 20).build();
        var disabled = net.minecraft.client.gui.components.Button.builder(Component.literal("Disabled"), button -> { }).bounds(0, 20, 80, 20).build();
        var last = net.minecraft.client.gui.components.Button.builder(Component.literal("Last"), button -> { }).bounds(0, 40, 80, 20).build();
        disabled.active = false;
        var host = new DesktopWidgetHost().add("first", first, Component.literal("First action"))
            .add("disabled", disabled, Component.literal("Unavailable action"))
            .add("last", last, Component.literal("Last action"));
        assertTrue(host.focusNext(true));
        assertEquals("first", host.focusedId());
        assertTrue(first.isFocused());
        assertTrue(host.focusNext(true));
        assertEquals("last", host.focusedId());
        assertFalse(first.isFocused());
        List<Object> narration = new ArrayList<>();
        net.minecraft.client.gui.narration.NarrationElementOutput output = (net.minecraft.client.gui.narration.NarrationElementOutput)
            Proxy.newProxyInstance(DesktopWindowDefinition.class.getClassLoader(),
                new Class<?>[]{net.minecraft.client.gui.narration.NarrationElementOutput.class}, (proxy, method, arguments) -> {
                    if (method.getName().equals("nest")) return proxy;
                    if (method.getName().equals("add")) narration.add(arguments[0]);
                    return null;
                });
        host.narrate(output);
        assertTrue(narration.contains(net.minecraft.client.gui.narration.NarratedElementType.TITLE));
        assertTrue(narration.contains(net.minecraft.client.gui.narration.NarratedElementType.HINT));
        last.visible = false;
        host.focusNext(false);
        assertEquals("first", host.focusedId());
        host.clearFocus();
        assertNull(host.focusedId());
    }

    @Test
    void environmentChangeCancelsWidgetCaptureAndClosedIsTheLastCallback() throws Exception {
        InventoryDesktopScreen screen = screen();
        screen.width = 400; screen.height = 300;
        List<String> events = new ArrayList<>();
        var button = net.minecraft.client.gui.components.Button.builder(Component.literal("Drag"), value -> { }).build();
        var host = new DesktopWidgetHost().add("drag", button, Component.literal("Drag widget"), () -> events.add("cancel"));
        Field entries = DesktopWidgetHost.class.getDeclaredField("entries"); entries.setAccessible(true);
        Field captured = DesktopWidgetHost.class.getDeclaredField("captured"); captured.setAccessible(true);
        captured.set(host, ((List<?>) entries.get(host)).get(0));
        Object provider = Proxy.newProxyInstance(DesktopWindowDefinition.class.getClassLoader(),
            new Class<?>[]{DesktopWindowDefinition.class, DesktopWindowPresentation.class}, (proxy, method, arguments) -> switch (method.getName()) {
                case "surface" -> DesktopWindowSurface.SALT;
                case "options" -> DesktopWindowOptions.DEFAULT;
                case "cancelPointer" -> { host.cancelPointer(); yield null; }
                case "windowStateChanged" -> { events.add("state"); yield null; }
                case "closed" -> { events.add("closed"); yield null; }
                case "render", "moved", "environmentChanged" -> { events.add(method.getName()); yield null; }
                default -> method.getReturnType() == boolean.class ? false : null;
            });
        Object window = view(screen, host, provider);
        invoke(screen, "notifyApiEnvironment", window);
        events.clear();
        set(screen, "apiPointerCaptureWindow", window);
        set(screen, "apiPointerCaptureButton", 0);
        screen.width = 320;
        invoke(screen, "notifyApiEnvironment", window);
        assertEquals(List.of("cancel", "environmentChanged"), events);
        assertNull(captured.get(host));
        invoke(screen, "apiClosed", window);
        List<String> terminal = List.copyOf(events);
        assertEquals("closed", terminal.get(terminal.size() - 1));
        set(window, "x", 22);
        invoke(screen, "notifyApiWindowState", window);
        invoke(screen, "apiMoved", window);
        invoke(screen, "renderApiWindow", null, window, 0, 0);
        assertEquals(terminal, events);
    }

    @Test
    void subviewCancellationDuringPressDoesNotCaptureItsReplacement() throws Exception {
        InventoryDesktopScreen screen = screen();
        Object provider = Proxy.newProxyInstance(DesktopWindowDefinition.class.getClassLoader(),
            new Class<?>[]{DesktopWindowDefinition.class}, (proxy, method, arguments) -> switch (method.getName()) {
                case "surface" -> DesktopWindowSurface.SALT;
                case "options" -> DesktopWindowOptions.DEFAULT;
                case "mouseClicked" -> { invoke(screen, "cancelApiInput", windows(screen).get(0)); yield true; }
                default -> method.getReturnType() == boolean.class ? false : null;
            });
        Object window = view(screen, new Object(), provider);
        Method clicked = List.of(InventoryDesktopScreen.class.getDeclaredMethods()).stream()
            .filter(method -> method.getName().equals("apiMouseClicked")).findFirst().orElseThrow();
        Class<?> eventType = clicked.getParameterTypes()[1];
        Constructor<?> constructor = eventType.getDeclaredConstructors()[0];
        Class<?> buttonType = constructor.getParameterTypes()[2];
        Object button = buttonType == int.class ? 0 : buttonType.getDeclaredConstructor(int.class, int.class).newInstance(1, 0);
        Object event = constructor.newInstance(50.0, 30.0, button);
        assertEquals(true, invoke(screen, "apiMouseClicked", window, event, false));
        invoke(screen, "captureApiPointer", window, event);
        assertFalse(context(screen, window).pointerCaptured());
        assertTrue(context(screen, window).windowHandle().isOpen());
    }

    @Test
    void hiddenNativeWidgetCannotKeepKeyboardOrPointerOwnership() throws Exception {
        AtomicInteger activated = new AtomicInteger();
        AtomicInteger cancelled = new AtomicInteger();
        var button = net.minecraft.client.gui.components.Button.builder(Component.literal("Action"), value -> activated.incrementAndGet()).build();
        var host = new DesktopWidgetHost().add("action", button, Component.literal("Action"), cancelled::incrementAndGet);
        assertThrows(IllegalArgumentException.class, () -> host.add("same-widget", button, Component.literal("Alias")));
        host.focusNext(true);
        Field entries = DesktopWidgetHost.class.getDeclaredField("entries"); entries.setAccessible(true);
        Field captured = DesktopWidgetHost.class.getDeclaredField("captured"); captured.setAccessible(true);
        captured.set(host, ((List<?>) entries.get(host)).get(0));
        button.visible = false;
        Method keyPressed = List.of(DesktopWidgetHost.class.getMethods()).stream()
            .filter(method -> method.getName().equals("keyPressed")).findFirst().orElseThrow();
        Object key = keyPressed.getParameterTypes()[0].getDeclaredConstructor(int.class, int.class, int.class).newInstance(32, 0, 0);
        assertEquals(false, keyPressed.invoke(host, key));
        assertEquals(0, activated.get());
        assertEquals(1, cancelled.get());
        assertNull(host.focusedId());
        assertNull(captured.get(host));
        host.cancelPointer();
        assertEquals(1, cancelled.get());
    }

    @Test
    void publicKeyboardEntryPointRoutesToTheFocusedApiOwner() throws Exception {
        InventoryDesktopScreen screen = screen();
        AtomicInteger keys = new AtomicInteger();
        Object window = view(screen, new Object(), definition(false, new AtomicInteger(), keys));
        set(screen, "apiKeyboardFocusWindow", window);
        Method keyPressed = List.of(InventoryDesktopScreen.class.getDeclaredMethods()).stream()
            .filter(method -> method.getName().equals("keyPressed") && method.getParameterCount() == 1).findFirst().orElseThrow();
        Object key = keyPressed.getParameterTypes()[0].getDeclaredConstructor(int.class, int.class, int.class)
            .newInstance(65, 0, 0);
        assertEquals(true, keyPressed.invoke(screen, key));
        assertEquals(1, keys.get());
    }

    private static Object surfaceDefinition(DesktopWindowSurface surface) {
        return Proxy.newProxyInstance(DesktopWindowDefinition.class.getClassLoader(),
            new Class<?>[]{DesktopWindowDefinition.class}, (proxy, method, arguments) -> switch (method.getName()) {
                case "surface" -> surface;
                case "options" -> DesktopWindowOptions.DEFAULT;
                default -> method.getReturnType() == boolean.class ? false : null;
            });
    }

    @Test
    void surfaceManagementControlsReserveCommittedGeometryAheadOfTitleDragging() throws Exception {
        InventoryDesktopScreen screen = screen();
        Object window = view(screen, new Object(), controlsDefinition(DesktopWindowOptions.DEFAULT));
        var surface = context(screen, window).surface();
        assertEquals(DesktopHitRole.MOVE, surface.hitRole(0, 0, 120, 100, 40, 8));
        assertEquals(DesktopHitRole.CONTROL, surface.hitRole(0, 0, 120, 100, 103, 8));
        assertEquals("CLOSE", invoke(screen, "titleBarControlAt", window, 103.0, 8.0).toString());
        assertNull(invoke(screen, "titleBarControlAt", window, 95.0, 8.0));
        set(window, "x", 70);
        assertNull(invoke(screen, "titleBarControlAt", window, 103.0, 8.0));
        assertEquals("CLOSE", invoke(screen, "titleBarControlAt", window, 173.0, 8.0).toString());
        set(window, "minimized", true);
        assertSame(window, invoke(screen, "windowAt", 173.0, 8.0), "Restore/management controls remain reachable when contents are minimized");
        assertNull(invoke(screen, "windowAt", 100.0, 60.0));
    }

    @Test
    void dragOnlyPolicyDisablesManagementAndLockRequestsWhileRetainingMovement() throws Exception {
        InventoryDesktopScreen screen = screen();
        Object window = view(screen, new Object(), controlsDefinition(DesktopWindowOptions.DRAG_ONLY));
        invoke(screen, "enforceApiOptions", window);
        for (DesktopWindowAction action : List.of(DesktopWindowAction.USER_CLOSE, DesktopWindowAction.PIN,
            DesktopWindowAction.UNPIN, DesktopWindowAction.MINIMIZE, DesktopWindowAction.GHOST,
            DesktopWindowAction.LOCK, DesktopWindowAction.UNLOCK, DesktopWindowAction.MANAGEMENT_MENU)) {
            assertEquals(false, invoke(screen, "apiActionAvailable", window, action), action.toString());
            assertEquals(false, invoke(screen, "requestApiWindowAction", window, action), action.toString());
        }
        assertEquals(true, invoke(screen, "apiActionAvailable", window, DesktopWindowAction.MOVE));
        assertTrue(((List<?>) invoke(screen, "popupControls", window)).isEmpty(), "Forbidden commands cannot survive as menu buttons");
        windows(screen).remove(window);
        assertEquals(false, invoke(screen, "apiActionAvailable", window, DesktopWindowAction.MOVE));
    }

    @Test
    void managerControlsExposePaintAndKeyboardFocusStatesAndHonorDisabledActions() throws Exception {
        InventoryDesktopScreen screen = screen();
        Object window = view(screen, new Object(), controlsDefinition(DesktopWindowOptions.DEFAULT));
        set(screen, "apiKeyboardFocusWindow", window);
        Method keyPressed = List.of(InventoryDesktopScreen.class.getDeclaredMethods()).stream()
            .filter(method -> method.getName().equals("apiKeyPressed")).findFirst().orElseThrow();
        keyPressed.setAccessible(true);
        int tabKey = com.mojang.blaze3d.platform.InputConstants.KEY_TAB;
        Object tab = keyPressed.getParameterTypes()[0].getDeclaredConstructor(int.class, int.class, int.class).newInstance(tabKey, 0, 0);
        assertEquals(true, keyPressed.invoke(screen, tab));
        assertEquals(com.salts_inventory_update.api.client.desktop.DesktopWindowControl.CLOSE,
            field(window, "apiControlFocus"));
        assertEquals(false, keyPressed.invoke(screen, tab), "Traversal exits management controls instead of trapping native widgets");
        set(window, "apiDefinition", controlsDefinition(DesktopWindowOptions.DRAG_ONLY));
        invoke(screen, "commitApiSurface", window);
        assertEquals(false, keyPressed.invoke(screen, tab), "Disabled controls cannot acquire keyboard focus");
    }

    private static Object field(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name); field.setAccessible(true); return field.get(target);
    }

    @Test
    void managerFocusedControlsSupplyTheirSemanticNarrationAndKeyboardUsage() throws Exception {
        InventoryDesktopScreen screen = screen();
        Object window = view(screen, new Object(), controlsDefinition(DesktopWindowOptions.DEFAULT));
        set(window, "apiControlFocus", com.salts_inventory_update.api.client.desktop.DesktopWindowControl.CLOSE);
        List<String> narration = new ArrayList<>();
        Object output = Proxy.newProxyInstance(net.minecraft.client.gui.narration.NarrationElementOutput.class.getClassLoader(),
            new Class<?>[]{net.minecraft.client.gui.narration.NarrationElementOutput.class}, (proxy, method, arguments) -> {
                if (method.getName().equals("add")) narration.add(java.util.Arrays.deepToString(arguments));
                return method.getReturnType().isInstance(proxy) ? proxy : null;
            });
        invoke(screen, "apiNarrate", window, output);
        assertTrue(narration.stream().anyMatch(text -> text.contains("window_control.close")), narration.toString());
        assertTrue(narration.stream().anyMatch(text -> text.contains("management_control")), narration.toString());
    }

    @Test
    void slotShiftRoutingUsesCapturedModernModifiersWithoutGlobalKeyboardPolling() throws Exception {
        InventoryDesktopScreen screen = screen();
        Method routing = List.of(InventoryDesktopScreen.class.getDeclaredMethods()).stream()
            .filter(method -> method.getName().equals("slotShiftHeld")).findFirst().orElseThrow();
        routing.setAccessible(true);
        Class<?> eventType = routing.getParameterTypes()[0];
        Constructor<?> eventConstructor = eventType.getDeclaredConstructors()[0];
        Class<?> buttonType = eventConstructor.getParameterTypes()[2];
        if (buttonType == int.class) {
            assertEquals(false, routing.invoke(screen, eventConstructor.newInstance(10.0, 20.0, 0)),
                "Legacy native mouse callbacks have no captured modifier argument");
        } else {
            Object button = buttonType.getConstructor(int.class, int.class).newInstance(0, 1);
            Object event = eventConstructor.newInstance(10.0, 20.0, button);
            assertEquals(true, routing.invoke(screen, event), "Explicit Shift works without an active physical keyboard");
        }
    }

    @Test
    void managementKeyboardEntryWorksEvenWhenNativeWidgetsConsumeTraversal() throws Exception {
        InventoryDesktopScreen screen = screen();
        AtomicInteger nativeKeys = new AtomicInteger();
        Object delegate = controlsDefinition(DesktopWindowOptions.DEFAULT);
        Object nativeDefinition = Proxy.newProxyInstance(DesktopWindowDefinition.class.getClassLoader(), new Class<?>[]{DesktopWindowDefinition.class},
            (proxy, method, arguments) -> {
                if (method.getName().equals("keyPressed") || method.getName().equals("focusNext")) {
                    nativeKeys.incrementAndGet(); return true;
                }
                return method.invoke(delegate, arguments);
            });
        Object window = view(screen, new Object(), nativeDefinition);
        set(screen, "apiKeyboardFocusWindow", window);
        Method keyPressed = List.of(InventoryDesktopScreen.class.getDeclaredMethods()).stream()
            .filter(method -> method.getName().equals("apiKeyPressed")).findFirst().orElseThrow();
        keyPressed.setAccessible(true);
        Object key = keyPressed.getParameterTypes()[0].getDeclaredConstructor(int.class, int.class, int.class)
            .newInstance(com.mojang.blaze3d.platform.InputConstants.KEY_F10, 0, 0xFFFF);
        assertEquals(true, keyPressed.invoke(screen, key));
        assertEquals(com.salts_inventory_update.api.client.desktop.DesktopWindowControl.CLOSE, field(window, "apiControlFocus"));
        assertEquals(0, nativeKeys.get(), "The explicit manager entry precedes consuming native widgets");
    }

    private static Object controlsDefinition(DesktopWindowOptions options) {
        return Proxy.newProxyInstance(DesktopWindowDefinition.class.getClassLoader(), new Class<?>[]{DesktopWindowDefinition.class},
            (proxy, method, arguments) -> switch (method.getName()) {
                case "surface" -> DesktopWindowSurface.modOwned(List.of(
                    DesktopWindowRegion.visualAndInteractive(0, 0, 120, 100),
                    DesktopWindowRegion.interactive(4, 4, 112, 14).withRole(DesktopHitRole.MOVE)));
                case "managementControls" -> List.of(new com.salts_inventory_update.api.client.desktop.DesktopSurfaceControl(
                    com.salts_inventory_update.api.client.desktop.DesktopWindowControl.CLOSE,
                    new com.salts_inventory_update.api.client.desktop.DesktopWindowBounds(100, 4, 12, 12)));
                case "options" -> options;
                default -> method.getReturnType() == boolean.class ? false : null;
            });
    }

    private static Object definition(boolean modal, AtomicInteger ticks, AtomicInteger keys) {
        return definition(modal, ticks, keys, false);
    }

    private static Object definition(boolean modal, AtomicInteger ticks, AtomicInteger keys, boolean presentation) {
        return Proxy.newProxyInstance(DesktopWindowDefinition.class.getClassLoader(),
            presentation ? new Class<?>[]{DesktopWindowDefinition.class, DesktopWindowPresentation.class}
                : new Class<?>[]{DesktopWindowDefinition.class}, (proxy, method, arguments) -> {
                return switch (method.getName()) {
                    case "tick" -> { ticks.incrementAndGet(); yield null; }
                    case "keyPressed" -> { keys.incrementAndGet(); yield true; }
                    case "modal" -> modal;
                    case "options" -> DesktopWindowOptions.DEFAULT;
                    case "surface" -> DesktopWindowSurface.SALT;
                    case "externalRegions" -> List.of(DesktopWindowRegion.visualAndInteractive(-20, 0, 20, 80));
                    case "toString" -> "Test view definition";
                    default -> method.getReturnType() == boolean.class ? false : null;
                };
            });
    }

    private static InventoryDesktopScreen screen() throws Exception {
        InventoryDesktopScreen screen = (InventoryDesktopScreen) unsafe.allocateInstance(InventoryDesktopScreen.class);
        set(screen, "windows", new ArrayList<>());
        set(screen, "sharedCarried", ItemStack.EMPTY);
        return screen;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object view(InventoryDesktopScreen screen, Object model, Object definition) throws Exception {
        Class<?> type = Class.forName(InventoryDesktopScreen.class.getName() + "$InventoryWindow");
        Class<?> kind = Class.forName(InventoryDesktopScreen.class.getName() + "$WindowKind");
        Constructor<?> constructor = type.getDeclaredConstructor(kind, Component.class, int.class, int.class, int.class, int.class);
        constructor.setAccessible(true);
        Object window = constructor.newInstance(Enum.valueOf((Class) kind, "CLIENT_API"), Component.literal("Example view"), 0, 0, 120, 100);
        set(window, "apiDefinition", definition);
        set(window, "apiModel", model);
        windows(screen).add(window);
        invoke(screen, "commitApiSurface", window);
        return window;
    }

    private static DesktopInputContext<?, ?> context(InventoryDesktopScreen screen, Object window) throws Exception {
        return (DesktopInputContext<?, ?>) invoke(screen, "apiContext", window, null, 0, 0);
    }

    @SuppressWarnings("unchecked")
    private static List<Object> windows(InventoryDesktopScreen screen) throws Exception {
        Field field = InventoryDesktopScreen.class.getDeclaredField("windows");
        field.setAccessible(true);
        return (List<Object>) field.get(screen);
    }

    private static Object invoke(Object target, String name, Object... arguments) throws Exception {
        Method method = List.of(target.getClass().getDeclaredMethods()).stream()
            .filter(candidate -> candidate.getName().equals(name) && candidate.getParameterCount() == arguments.length)
            .findFirst().orElseThrow();
        method.setAccessible(true);
        return method.invoke(target, arguments);
    }

    private static void set(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}
