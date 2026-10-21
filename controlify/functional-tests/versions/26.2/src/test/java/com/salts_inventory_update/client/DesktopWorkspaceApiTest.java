package com.salts_inventory_update.client;

import com.salts_inventory_update.api.client.desktop.DesktopClientApi;
import com.salts_inventory_update.api.client.desktop.DesktopWindowOptions;
import com.salts_inventory_update.api.client.desktop.DesktopWindowDefinition;
import com.salts_inventory_update.api.client.desktop.DesktopWindowSurface;
import com.salts_inventory_update.api.client.desktop.DesktopWindowRegion;
import com.salts_inventory_update.api.client.desktop.DesktopWindowControl;
import com.salts_inventory_update.api.client.desktop.DesktopSurfaceControl;
import com.salts_inventory_update.api.client.desktop.DesktopWindowBounds;
import com.salts_inventory_update.api.client.desktop.DesktopWindowState;
import com.salts_inventory_update.api.client.desktop.DesktopWindowContext;
import com.salts_inventory_update.api.client.desktop.DesktopResizePolicy;
import com.salts_inventory_update.api.client.desktop.DesktopWindowSize;
import com.salts_inventory_update.api.client.desktop.DesktopHitRole;
import com.salts_inventory_update.api.client.desktop.DesktopPopup;
import com.salts_inventory_update.api.client.desktop.DesktopSlotHit;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.KeyEvent;
import com.salts_inventory_update.api.client.desktop.DesktopWorkspace;
import com.salts_inventory_update.api.client.desktop.DesktopWorkspaceHost;
import com.salts_inventory_update.api.client.desktop.DesktopWorkspaceWindow;
import com.salts_inventory_update.network.DesktopPackets;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/** Exercises the real core presentation API and slot routing, without replacing either with mocks. */
final class DesktopWorkspaceApiTest {
    private static sun.misc.Unsafe unsafe;
    private DesktopWorkspaceHost previousHost;
    private Object previousSingleton;
    private boolean previousAllowResizing;

    @BeforeAll static void bootstrap() throws Exception {
        Class<?> loaderType = Class.forName("net.fabricmc.loader.impl.FabricLoaderImpl");
        Object loader = loaderType.getField("INSTANCE").get(null);
        Field configDir = loaderType.getDeclaredField("configDir");
        configDir.setAccessible(true);
        var configPath = java.nio.file.Path.of(System.getProperty("user.dir"), "controller-workspace-config");
        java.nio.file.Files.createDirectories(configPath);
        configDir.set(loader, configPath);
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        Field access = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        access.setAccessible(true);
        unsafe = (sun.misc.Unsafe) access.get(null);
    }

    @BeforeEach void installHost() throws Exception {
        previousHost = DesktopClientApi.workspaceHost();
        previousSingleton = field(null, InventoryDesktopScreen.class, "singleton");
        previousAllowResizing = SaltsInventoryConfig.get().allowResizing;
        DesktopClientApi.setWorkspaceHost(new DesktopWorkspaceHost() {
            @Override public boolean enabled() { return true; }
            @Override public void opened(DesktopWorkspace workspace) { }
        });
    }

    @AfterEach void restoreHost() throws Exception {
        DesktopClientApi.setWorkspaceHost(previousHost);
        set(null, InventoryDesktopScreen.class, "singleton", previousSingleton);
        SaltsInventoryConfig.get().allowResizing = previousAllowResizing;
    }

    @Test void workspaceSnapshotRetainsExistingWindowIdsAndModelsWithoutRecreation() throws Exception {
        InventoryDesktopScreen screen = screen();
        Object player = window(screen, "INVENTORY", "Player");
        Object character = window(screen, "CHARACTER", "Character");
        Object panel = window(screen, "CLIENT_API", "Custom panel");
        Object model = new Object();
        set(panel, "apiModel", model);
        DesktopWorkspace workspace = screen.workspaceAccess();
        var first = workspace.windows();
        assertEquals(List.of(DesktopWorkspaceWindow.Kind.PLAYER_INVENTORY, DesktopWorkspaceWindow.Kind.CHARACTER,
                DesktopWorkspaceWindow.Kind.OTHER), first.stream().map(DesktopWorkspaceWindow::kind).toList());
        assertEquals(List.of(id(player), id(character), id(panel)), first.stream().map(w -> w.handle().instanceId()).toList());
        assertTrue(first.stream().allMatch(w -> w.handle().isOpen()));
        assertThrows(UnsupportedOperationException.class, () -> first.clear());
        assertEquals(first.stream().map(w -> w.handle().instanceId()).toList(), workspace.windows().stream()
                .map(w -> w.handle().instanceId()).toList());
        assertSame(model, field(panel, "apiModel"));
        assertSame(panel, windows(screen).get(2));
    }

    @Test void disposedAndOwnerHiddenCompanionsAreNotExposedAsIndependentWorkspaceWindows() throws Exception {
        InventoryDesktopScreen screen = screen();
        Object visible = window(screen, "INVENTORY", "Player");
        Object disposed = window(screen, "CLIENT_API", "Disposed");
        Object hidden = window(screen, "CONTAINER", "Companion");
        set(disposed, "apiDisposed", true);
        set(hidden, "companionHidden", true);
        var snapshot = screen.workspaceAccess().windows();
        assertEquals(1, snapshot.size());
        assertEquals(id(visible), snapshot.getFirst().handle().instanceId());
    }

    @Test void protectedPlayerAndCharacterRejectEvenForcedCoreClosePaths() throws Exception {
        InventoryDesktopScreen screen = screen();
        Object player = window(screen, "INVENTORY", "Player");
        Object character = window(screen, "CHARACTER", "Character");
        DesktopWorkspace workspace = screen.workspaceAccess();
        workspace.protectWindow(id(player));
        workspace.protectWindow(id(character));
        assertTrue(workspace.windows().stream().noneMatch(DesktopWorkspaceWindow::closable));
        invoke(screen, "closeWindow", player, "controller-test", true);
        invoke(screen, "closeWindow", character, "controller-test", true);
        assertEquals(List.of(player, character), windows(screen));
        assertTrue(workspace.windows().stream().allMatch(w -> w.handle().isOpen()));
        assertFalse((Boolean) field(player, "apiDisposed"));
        assertFalse((Boolean) field(character, "apiDisposed"));
    }

    @Test void quickMoveAlwaysTargetsOtherDisplayedContainerEvenWhenCoreFocusDiffers() throws Exception {
        InventoryDesktopScreen screen = screen();
        Object first = window(screen, "CONTAINER", "First");
        Object second = window(screen, "CONTAINER", "Second");
        Object unrelated = window(screen, "CONTAINER", "Undisplayed");
        attachSession(first, 11);
        attachSession(second, 22);
        attachSession(unrelated, 33);
        set(unrelated, "focused", true);
        screen.workspaceAccess().transferPair(id(first), id(second));
        set(screen, "workspacePointerWindow", first);
        Object target = invoke(screen, "quickMoveTarget", hit(11));
        assertEquals(DesktopPackets.QUICK_TARGET_SESSION, invoke(target, "kind"));
        assertEquals(22, invoke(target, "sessionId"));
        set(screen, "workspacePointerWindow", second);
        target = invoke(screen, "quickMoveTarget", hit(22));
        assertEquals(11, invoke(target, "sessionId"));
    }

    @Test void quickMoveUsesCurrentPairAndDoesNotFallBackWhenDestinationClosesOrHasNoInventory() throws Exception {
        InventoryDesktopScreen screen = screen();
        Object player = window(screen, "INVENTORY", "Player");
        Object first = window(screen, "CONTAINER", "First");
        Object second = window(screen, "CONTAINER", "Second");
        Object panel = window(screen, "CLIENT_API", "Inventory-less UI");
        attachSession(first, 11);
        attachSession(second, 22);
        DesktopWorkspace workspace = screen.workspaceAccess();
        set(screen, "workspaceLastPointerWindow", player);
        workspace.transferPair(id(player), id(first));
        assertEquals(11, invoke(invoke(screen, "quickMoveTarget", hit(DesktopPackets.PLAYER_MENU_SESSION)), "sessionId"));
        workspace.transferPair(id(player), id(second));
        assertEquals(22, invoke(invoke(screen, "quickMoveTarget", hit(DesktopPackets.PLAYER_MENU_SESSION)), "sessionId"));
        windows(screen).remove(second);
        assertRejected(screen, DesktopPackets.PLAYER_MENU_SESSION);
        workspace.transferPair(id(player), id(panel));
        assertRejected(screen, DesktopPackets.PLAYER_MENU_SESSION);
        set(screen, "workspaceLastPointerWindow", first);
        workspace.transferPair(id(player), id(panel));
        assertRejected(screen, 11);
    }

    @Test void containerToPlayerQuickMoveSelectsPlayerInventoryAndSameSessionTargetIsRejected() throws Exception {
        InventoryDesktopScreen screen = screen();
        Object player = window(screen, "INVENTORY", "Player");
        Object chest = window(screen, "CONTAINER", "Chest");
        Object duplicate = window(screen, "CONTAINER", "Duplicate session");
        attachSession(chest, 11);
        attachSession(duplicate, 11);
        set(screen, "workspacePointerWindow", chest);
        screen.workspaceAccess().transferPair(id(chest), id(player));
        var target = invoke(screen, "quickMoveTarget", hit(11));
        assertEquals(DesktopPackets.QUICK_TARGET_PLAYER_STORAGE, invoke(target, "kind"));
        assertEquals(DesktopPackets.PLAYER_MENU_SESSION, invoke(target, "sessionId"));
        screen.workspaceAccess().transferPair(id(chest), id(duplicate));
        assertRejected(screen, 11);
    }

    @Test void forcedInventoryHudIsScopedToEnabledExternalPresentationAndUsesCoreHoldThreshold() throws Exception {
        InventoryDesktopScreen screen = screen();
        boolean saved = SaltsInventoryConfig.get().returnHotbarToInventory;
        try {
            SaltsInventoryConfig.get().returnHotbarToInventory = false;
            assertEquals(false, invoke(screen, "workspaceInventoryHud"));
            screen.workspaceAccess().forceInventoryHud(true);
            assertEquals(true, invoke(screen, "workspaceInventoryHud"));
            window(screen, "CHARACTER", "Character");
            assertTrue(screen.usesInventoryWindowHotbar());
            DesktopClientApi.setWorkspaceHost(null);
            assertEquals(false, invoke(screen, "workspaceInventoryHud"));
            assertFalse(screen.usesInventoryWindowHotbar());
            assertEquals(SaltsInventoryConfig.get().eHoldCloseAllMs(), screen.workspaceAccess().closeAllHoldMillis());
            assertFalse(SaltsInventoryConfig.get().returnHotbarToInventory, "Presentation override must not rewrite the saved config");
        } finally {
            SaltsInventoryConfig.get().returnHotbarToInventory = saved;
        }
    }

    @Test void compactControllerInventoryPreservesSavedKeyboardHeightAfterHostDisables() throws Exception {
        boolean savedReturnHotbar = SaltsInventoryConfig.get().returnHotbarToInventory;
        boolean savedExpandable = SaltsInventoryConfig.get().expandableInventory;
        boolean savedGlobalPins = SaltsInventoryConfig.get().globalPins;
        try {
            SaltsInventoryConfig.get().expandableInventory = false;
            SaltsInventoryConfig.get().globalPins = true;
            for (boolean keyboardHotbar : List.of(false, true)) {
                SaltsInventoryConfig.get().returnHotbarToInventory = keyboardHotbar;
                var enabled = new java.util.concurrent.atomic.AtomicBoolean(true);
                DesktopClientApi.setWorkspaceHost(new DesktopWorkspaceHost() {
                    @Override public boolean enabled() { return enabled.get(); }
                    @Override public void opened(DesktopWorkspace workspace) { }
                });
                InventoryDesktopScreen screen = screen();
                set(screen, net.minecraft.client.gui.screens.Screen.class, "width", 640);
                set(screen, net.minecraft.client.gui.screens.Screen.class, "height", 480);
                var font = (net.minecraft.client.gui.Font) unsafe.allocateInstance(net.minecraft.client.gui.Font.class);
                set(font, "splitter", new net.minecraft.client.StringSplitter((codepoint, style) -> 6));
                set(screen, net.minecraft.client.gui.screens.Screen.class, "font", font);
                var localPlayer = (net.minecraft.client.player.LocalPlayer) unsafe.allocateInstance(net.minecraft.client.player.LocalPlayer.class);
                var inventory = new net.minecraft.world.entity.player.Inventory(localPlayer, new net.minecraft.world.entity.EntityEquipment());
                set(localPlayer, Player.class, "inventory", inventory);
                var menu = (net.minecraft.world.inventory.InventoryMenu) unsafe.allocateInstance(net.minecraft.world.inventory.InventoryMenu.class);
                var slots = net.minecraft.core.NonNullList.<Slot>create();
                for (int index = 0; index < 9; index++) slots.add(new Slot(new SimpleContainer(9), index, 0, 0));
                for (int index = 9; index < 36; index++) slots.add(new Slot(inventory, index, 0, 0));
                for (int index = 0; index < 9; index++) slots.add(new Slot(inventory, index, 0, 0));
                for (int index = 0; index < slots.size(); index++) slots.get(index).index = index;
                set(menu, AbstractContainerMenu.class, "slots", slots);
                set(localPlayer, Player.class, "inventoryMenu", menu);
                set(screen, "owner", localPlayer);
                Object playerWindow = window(screen, "INVENTORY", "Inventory");
                set(playerWindow, "width", 178);
                set(playerWindow, "height", 314);

                DesktopWorkspace workspace = screen.workspaceAccess();
                workspace.transferPair(id(playerWindow), null);
                workspace.forceInventoryHud(true);
                assertEquals(108, workspace.presentationBounds(id(playerWindow)).height(),
                    "Controller presentation must discard blank rows in a previously tall keyboard window");
                var visibleSlots = workspace.visibleSlots(id(playerWindow));
                assertEquals(36, visibleSlots.size());
                int lastInventoryRow = visibleSlots.stream().filter(slot -> slot.menuSlotId() >= 9 && slot.menuSlotId() < 36)
                    .mapToInt(DesktopSlotHit::y).max().orElseThrow();
                int hotbar = visibleSlots.stream().filter(slot -> slot.menuSlotId() >= 36)
                    .mapToInt(DesktopSlotHit::y).min().orElseThrow();
                assertEquals(22, hotbar - lastInventoryRow, "Hotbar follows the occupied row with the normal 4px gap");
                invoke(screen, "saveWindowState", playerWindow, false);
                assertEquals(314, DesktopWindowStateStore.load(null, "local:inventory").orElseThrow().height,
                    "Saving during controller use must retain the keyboard user's height");

                enabled.set(false); // Actual host ordering: input mode changes before the addon releases ownership.
                workspace.forceInventoryHud(false);
                assertEquals(314, field(playerWindow, "height"));
                assertEquals(keyboardHotbar, screen.usesInventoryWindowHotbar());
                assertEquals(keyboardHotbar, SaltsInventoryConfig.get().returnHotbarToInventory);
                workspace.forceInventoryHud(false);
                assertEquals(314, field(playerWindow, "height"), "Repeated keyboard ticks must not subtract the forced row again");
            }
        } finally {
            SaltsInventoryConfig.get().returnHotbarToInventory = savedReturnHotbar;
            SaltsInventoryConfig.get().expandableInventory = savedExpandable;
            SaltsInventoryConfig.get().globalPins = savedGlobalPins;
        }
    }

    @Test void characterDestinationUsesItsExactEquipmentSurface() throws Exception {
        InventoryDesktopScreen screen = screen();
        Object player = window(screen, "INVENTORY", "Player");
        Object character = window(screen, "CHARACTER", "Character");
        screen.workspaceAccess().transferPair(id(player), id(character));
        set(screen, "workspacePointerWindow", player);
        Object target = invoke(screen, "quickMoveTarget", hit(DesktopPackets.PLAYER_MENU_SESSION));
        assertEquals(DesktopPackets.QUICK_TARGET_PLAYER_EQUIPMENT, invoke(target, "kind"));
        set(screen, "workspacePointerWindow", character);
        target = invoke(screen, "quickMoveTarget", hit(DesktopPackets.PLAYER_MENU_SESSION));
        assertEquals(DesktopPackets.QUICK_TARGET_PLAYER_STORAGE, invoke(target, "kind"));
    }

    @Test void ensuringExistingCharacterRevivesSameObjectWithoutFocusOrDisposalChanges() throws Exception {
        InventoryDesktopScreen screen = screen();
        Object character = window(screen, "CHARACTER", "Character");
        set(character, "persistentHidden", true);
        Object model = new Object();
        set(character, "apiModel", model);
        var handle = screen.workspaceAccess().ensureWindow(DesktopWorkspaceWindow.Kind.CHARACTER);
        assertEquals(id(character), handle.instanceId());
        assertTrue(handle.isOpen());
        assertSame(character, windows(screen).getFirst());
        assertSame(model, field(character, "apiModel"));
        assertEquals(false, field(character, "persistentHidden"));
        assertEquals(false, field(character, "focused"));
    }

    @Test void restoringRememberedViewRetainsIdentityModelAndPreferencesWithoutFocusOrOpeningCallbacks() throws Exception {
        for (String[] flags : new String[][]{{"persistentHidden"}}) {
            InventoryDesktopScreen screen = screen();
            Object restored = window(screen, "CLIENT_API", "Remembered view");
            Object other = window(screen, "CLIENT_API", "Keyboard owner");
            Object model = new Object();
            Object state = new Object();
            set(restored, "apiModel", model);
            set(restored, "apiState", state);
            set(restored, "apiViewKey", "remembered-view");
            AtomicInteger callbacks = new AtomicInteger();

            AtomicReference<DesktopWindowState> published = new AtomicReference<>();
            set(restored, "apiDefinition", Proxy.newProxyInstance(DesktopWindowDefinition.class.getClassLoader(),
                    new Class<?>[]{DesktopWindowDefinition.class}, (proxy, method, args) -> {
                        if (List.of("opened", "closed", "saveLocalState", "loadLocalState", "moved", "resized",
                                "focusChanged", "keyboardFocusChanged").contains(method.getName())) {
                            callbacks.incrementAndGet();
                        }
                        if (method.getName().equals("surface")) return DesktopWindowSurface.SALT;
                        if (method.getName().equals("options")) return DesktopWindowOptions.DEFAULT;
                        if (method.getName().equals("managementControls") || method.getName().equals("popups")) return List.of();
                        if (method.getName().equals("windowStateChanged")) published.set((DesktopWindowState) args[1]);

                        return method.getReturnType() == boolean.class ? false : null;
                    }));
            for (String flag : flags) set(restored, flag, true);
            set(other, "focused", true);
            set(screen, "apiKeyboardFocusWindow", other);
            UUID originalId = id(restored);
            screen.workspaceAccess().restoreWindow(originalId);
            assertEquals(false, field(restored, "persistentHidden"));
            assertEquals(false, field(restored, "apiDisposed"));
            assertEquals(false, field(restored, "focused"));
            assertEquals(true, field(other, "focused"));
            assertSame(other, field(screen, "apiKeyboardFocusWindow"));
            assertSame(restored, windows(screen).getFirst());
            assertEquals(originalId, id(restored));
            assertSame(model, field(restored, "apiModel"));
            assertSame(state, field(restored, "apiState"));
            assertEquals("remembered-view", field(restored, "apiViewKey"));
            assertEquals(0, callbacks.get(), "Presentation restoration must not reopen, refocus or save user preferences");
            assertNotNull(published.get(), "The same view must receive its restored visibility state");
            assertTrue(published.get().visible());


            screen.workspaceAccess().restoreWindow(originalId);

            assertEquals(0, callbacks.get());
        }
    }

    @Test void restorationDoesNotReviveOwnerHiddenCompanionOrRecreateMissingWindow() throws Exception {
        InventoryDesktopScreen screen = screen();
        Object companion = window(screen, "CLIENT_API", "Owner-hidden companion");
        set(companion, "companionHidden", true);
        set(companion, "persistentHidden", true);
        screen.workspaceAccess().restoreWindow(id(companion));
        assertEquals(true, field(companion, "persistentHidden"));
        screen.workspaceAccess().restoreWindow(UUID.randomUUID());
        assertEquals(List.of(companion), windows(screen));
    }

    @Test void externalPresentationConsumesNativeMoveResizeChromeWithoutMovingOrOpeningManagement() throws Exception {
        SaltsInventoryConfig.get().allowResizing = true;
        InventoryDesktopScreen screen = screen();
        Object view = window(screen, "CLIENT_API", "Native artwork");
        set(view, "locked", false);
        AtomicInteger nativeClicks = new AtomicInteger();
        for (DesktopHitRole role : DesktopHitRole.values()) {
            if (role != DesktopHitRole.MOVE && !role.resizing()) continue;
            var surface = DesktopWindowSurface.modOwned(List.of(
                    DesktopWindowRegion.visualAndInteractive(0, 0, 176, 166).withRole(role)));
            set(view, "apiSurface", surface);
            set(view, "apiDefinition", nativeDefinition(surface, nativeClicks, new AtomicInteger(), List.of()));
            assertEquals(true, invoke(screen, "apiActionAvailable", view,
                    com.salts_inventory_update.api.client.desktop.DesktopWindowAction.MOVE));
            assertEquals(true, invoke(screen, "canResizeWindow", view), "The resize guard must be tested on a normally resizable view");
            for (int button : new int[]{org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT, org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_RIGHT}) {
                assertEquals(true, invoke(screen, "beginSurfaceInteraction", view, new MouseButtonEvent(30, 40, new net.minecraft.client.input.MouseButtonInfo(button, 0)), false));
                assertNull(field(screen, "movingWindow"));
                assertNull(field(screen, "resizingWindow"));
                assertNull(field(screen, "popupWindow"));
                assertEquals(10, field(view, "x"));
                assertEquals(20, field(view, "y"));
                assertEquals(176, field(view, "width"));
                assertEquals(166, field(view, "height"));
            }
        }
        assertEquals(0, nativeClicks.get(), "Desktop chrome presses must not leak into native UI actions");
    }

    @Test void externalPresentationSuppressesManagementPaintHitAndKeyboardButKeepsNativeBodyControls() throws Exception {
        InventoryDesktopScreen screen = screen();
        Object view = window(screen, "CLIENT_API", "Native artwork");
        set(view, "locked", false);
        AtomicInteger nativeClicks = new AtomicInteger();
        AtomicInteger managementPaints = new AtomicInteger();
        var controls = List.of(new DesktopSurfaceControl(DesktopWindowControl.CLOSE, new DesktopWindowBounds(0, 0, 12, 12)));
        var surface = DesktopWindowSurface.modOwned(List.of(
                DesktopWindowRegion.visualAndInteractive(0, 0, 176, 166).withRole(DesktopHitRole.CONTROL)));
        set(view, "apiSurface", surface);
        set(view, "apiSurfaceControls", controls);
        set(view, "apiDefinition", nativeDefinition(surface, nativeClicks, managementPaints, controls));
        assertNull(invoke(screen, "surfaceControlAt", view, 15.0, 25.0));
        assertEquals(false, invoke(screen, "surfaceControlKeyboard", view,
                new KeyEvent(com.mojang.blaze3d.platform.InputConstants.KEY_F10, 0, com.mojang.blaze3d.platform.InputConstants.MOD_ALT)));
        assertNull(field(view, "apiControlFocus"));
        invoke(screen, "renderSurfaceControls", null, view, 30, 40);
        assertEquals(0, managementPaints.get());
        assertEquals(true, invoke(screen, "beginSurfaceInteraction", view, new MouseButtonEvent(40, 50, new net.minecraft.client.input.MouseButtonInfo(org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT, 0)), false));
        assertEquals(1, nativeClicks.get(), "The native body control must still receive its normal click");
        assertSame(view, field(screen, "apiPointerCaptureWindow"));
        assertNull(field(screen, "movingWindow"));
        assertNull(field(screen, "resizingWindow"));
    }

    @Test void ordinaryDesktopMoveRoleStillWorksWhenExternalPresentationIsDisabled() throws Exception {
        InventoryDesktopScreen screen = screen();
        Object view = window(screen, "CLIENT_API", "Native artwork");
        set(view, "locked", false);
        var surface = DesktopWindowSurface.modOwned(List.of(
                DesktopWindowRegion.visualAndInteractive(0, 0, 176, 166).withRole(DesktopHitRole.MOVE)));
        set(view, "apiSurface", surface);
        set(view, "apiDefinition", nativeDefinition(surface, new AtomicInteger(), new AtomicInteger(), List.of()));
        DesktopClientApi.setWorkspaceHost(null);
        assertEquals(true, invoke(screen, "beginSurfaceInteraction", view, new MouseButtonEvent(30, 40, new net.minecraft.client.input.MouseButtonInfo(org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT, 0)), false));
        assertSame(view, field(screen, "movingWindow"));
        assertNull(field(screen, "resizingWindow"));
    }

    @Test void pointerExtentUsesNativeShapeExternalControlsAndPopupRegions() throws Exception {
        InventoryDesktopScreen screen = screen();
        Object view = window(screen, "CLIENT_API", "Native controls");
        Object other = window(screen, "CLIENT_API", "Other");
        AtomicInteger clicks = new AtomicInteger();
        var popup = new DesktopPopup("outside", List.of(DesktopWindowRegion.visualAndInteractive(180, 10, 24, 24)),
                false, true, true, false);
        var surface = DesktopWindowSurface.modOwned(List.of(
                DesktopWindowRegion.visualAndInteractive(0, 0, 40, 40),
                DesktopWindowRegion.visualAndInteractive(-20, 0, 16, 16).withRole(DesktopHitRole.CONTROL),
                DesktopWindowRegion.visual(-50, 0, 16, 16))).withRegions(popup.regions());
        set(view, "apiSurface", surface);
        set(view, "apiDefinition", nativeDefinition(surface, clicks, new AtomicInteger(), List.of()));
        set(view, "apiPopups", List.of(popup));
        DesktopWorkspace workspace = screen.workspaceAccess();
        workspace.transferPair(id(view), id(other));
        assertTrue(workspace.containsPointer(id(view), 20, 30));
        assertFalse(workspace.containsPointer(id(view), 100, 60), "Mod-owned cutouts must retain their native hit shape");
        assertTrue(workspace.containsPointer(id(view), -5, 25), "A control outside the base bounds must be reachable");
        assertFalse(workspace.containsPointer(id(view), -35, 25), "Visual-only protrusions do not own input");
        assertTrue(workspace.containsPointer(id(view), 195, 35), "Committed native popup geometry must own input");
        assertFalse(workspace.containsPointer(id(view), 220, 35));
        assertEquals(true, invoke(screen, "beginSurfaceInteraction", view, new MouseButtonEvent(-5, 25, new net.minecraft.client.input.MouseButtonInfo(org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT, 0)), false));
        assertEquals(1, clicks.get(), "A protruding native control keeps its existing click callback");
        assertSame(view, field(screen, "apiPointerCaptureWindow"));
        set(other, "apiSurface", DesktopWindowSurface.SALT);
        assertTrue(workspace.containsPointer(id(other), 100, 60), "Ordinary native body geometry remains usable");
    }

    @Test void pointerExtentRejectsUnavailableWindowsAndNonFiniteCoordinates() throws Exception {
        InventoryDesktopScreen screen = screen();
        Object view = window(screen, "CLIENT_API", "Native control");
        Object other = window(screen, "CLIENT_API", "Other");
        DesktopWorkspace workspace = screen.workspaceAccess();
        workspace.transferPair(id(view), id(other));
        assertTrue(workspace.containsPointer(id(view), 20, 30));
        for (String flag : List.of("persistentHidden", "companionHidden", "apiDisposed")) {
            set(view, flag, true);
            assertFalse(workspace.containsPointer(id(view), 20, 30), flag + " windows must not own pointer input");
            set(view, flag, false);
        }
        assertFalse(workspace.containsPointer(UUID.randomUUID(), 20, 30));
        assertFalse(workspace.containsPointer(id(view), Double.NaN, 30));
        assertFalse(workspace.containsPointer(id(view), 20, Double.POSITIVE_INFINITY));
        assertFalse(workspace.containsPointer(id(view), Double.NEGATIVE_INFINITY, 30));
    }

    @Test void pointerExtentRespectsDisplayedPairAndNativeModalOwner() throws Exception {
        InventoryDesktopScreen screen = screen();
        Object first = window(screen, "CLIENT_API", "First");
        Object second = window(screen, "CLIENT_API", "Second");
        Object undisplayed = window(screen, "CLIENT_API", "Undisplayed modal");
        var surface = DesktopWindowSurface.SALT;
        var modalPopup = new DesktopPopup("modal", List.of(DesktopWindowRegion.visualAndInteractive(180, 0, 24, 24)),
                true, true, false, false);
        for (Object view : List.of(first, second, undisplayed))
            set(view, "apiDefinition", nativeDefinition(surface, new AtomicInteger(), new AtomicInteger(), List.of()));
        set(undisplayed, "apiPopups", List.of(modalPopup));
        DesktopWorkspace workspace = screen.workspaceAccess();
        workspace.transferPair(id(first), id(second));
        assertTrue(workspace.containsPointer(id(first), 20, 30), "An undisplayed modal must not capture the displayed pair");
        assertFalse(workspace.containsPointer(id(undisplayed), 20, 30));
        set(second, "apiPopups", List.of(modalPopup));
        set(second, "apiSurface", surface.withRegions(modalPopup.regions()));
        assertFalse(workspace.containsPointer(id(first), 20, 30), "The displayed modal owner blocks the other window");
        assertTrue(workspace.containsPointer(id(second), 195, 25), "The modal owner keeps its native popup hit region");
        assertTrue(workspace.containsPointer(id(second), -200, -200), "Native modal ownership includes outside presses");
        assertFalse(workspace.containsPointer(id(first), -200, -200));
        AtomicInteger outsidePresses = new AtomicInteger();
        Object nativeDefinition = field(second, "apiDefinition");
        set(second, "apiDefinition", Proxy.newProxyInstance(DesktopWindowDefinition.class.getClassLoader(),
                new Class<?>[]{DesktopWindowDefinition.class}, (proxy, method, args) -> {
                    if (method.getName().equals("dismissPopup")) {
                        assertEquals("modal", args[1]);
                        assertEquals(DesktopPopup.Dismissal.OUTSIDE_PRESS, args[2]);
                        outsidePresses.incrementAndGet();
                        return false;
                    }
                    return method.invoke(nativeDefinition, args);
                }));
        set(screen, "apiLastModalWindow", second);
        set(screen, "apiKeyboardFocusWindow", second);
        assertTrue(workspace.pointer(id(second), DesktopWorkspace.PointerAction.PRESS, -200, -200, 0, 0, 0));
        assertEquals(1, outsidePresses.get(), "A modal outside press must reach the unchanged native dismissal callback");
        set(second, "persistentHidden", true);
        assertTrue(workspace.containsPointer(id(first), 20, 30), "A hidden modal cannot capture a visible window");
        DesktopClientApi.setWorkspaceHost(null);
        assertTrue(workspace.containsPointer(id(undisplayed), 20, 30), "Ordinary desktop modality still uses the native owner");
        assertFalse(workspace.containsPointer(id(first), 20, 30));
    }

    @Test void slotSnappingIncludesInteractiveExternalGeometryAndDeduplicatesNativeHits() throws Exception {
        InventoryDesktopScreen screen = screen();
        Object view = window(screen, "CONTAINER", "External slot");
        Object other = window(screen, "CLIENT_API", "Other");
        attachSession(view, 11);
        TestMenu menu = new TestMenu();
        Object session = field(view, "session");
        set(session, "menu", menu);
        set(session, "containerSlots", List.copyOf(menu.slots));
        var region = DesktopWindowRegion.interactive(-20, 0, 16, 16).withRole(DesktopHitRole.SLOT);
        var surface = DesktopWindowSurface.modOwned(List.of(region, region, DesktopWindowRegion.visual(-60, 0, 16, 16)));
        set(view, "apiSurface", surface);
        set(view, "apiDefinition", Proxy.newProxyInstance(DesktopWindowDefinition.class.getClassLoader(),
                new Class<?>[]{DesktopWindowDefinition.class}, (proxy, method, args) -> {
                    if (method.getName().equals("surface")) return surface;
                    if (method.getName().equals("options")) return DesktopWindowOptions.DEFAULT;
                    if (method.getName().equals("slotAt")) {
                        double x = (double) args[1], y = (double) args[2];
                        if (x >= -10 && x < 6 && y >= 20 && y < 36) return new DesktopSlotHit(0, -10, 20);
                        if (x >= -50 && x < -34 && y >= 20 && y < 36) return new DesktopSlotHit(0, -50, 20);
                    }
                    return method.getReturnType() == boolean.class ? false : null;
                }));
        DesktopWorkspace workspace = screen.workspaceAccess();
        workspace.transferPair(id(view), id(other));
        assertEquals(List.of(new DesktopSlotHit(0, -10, 20)), workspace.visibleSlots(id(view)),
                "Snapping must reach the actual external slot without adding visual-only or duplicate regions");
        assertSame(menu.slots.getFirst(), workspace.hoveredSlot(id(view), -5, 25));
        set(view, "persistentHidden", true);
        assertTrue(workspace.visibleSlots(id(view)).isEmpty());
        set(view, "persistentHidden", false);
        workspace.transferPair(id(other), UUID.randomUUID());
        assertTrue(workspace.visibleSlots(id(view)).isEmpty(), "Undisplayed native slots must not appear as snap targets");
    }

    @Test void emptyPointerDismissesOnlyItsNativeNonmodalPopupWithoutBodyClickOrCapture() throws Exception {
        InventoryDesktopScreen screen = screen();
        Object first = window(screen, "CLIENT_API", "First popup");
        Object second = window(screen, "CLIENT_API", "Second popup");
        Object undisplayed = window(screen, "CLIENT_API", "Undisplayed popup");
        int[] dismissals = {0, 0, 0};
        AtomicInteger bodyClicks = new AtomicInteger();
        var popup = new DesktopPopup("outside", List.of(DesktopWindowRegion.visualAndInteractive(180, 0, 24, 24)),
                false, true, false, false);
        var undisplayedModal = new DesktopPopup("hidden-modal", popup.regions(), true, true, false, false);
        List<Object> views = List.of(first, second, undisplayed);
        for (int index = 0; index < views.size(); index++) {
            Object view = views.get(index);
            int owner = index;
            set(view, "apiDefinition", Proxy.newProxyInstance(DesktopWindowDefinition.class.getClassLoader(),
                    new Class<?>[]{DesktopWindowDefinition.class}, (proxy, method, args) -> {
                        if (method.getName().equals("surface")) return DesktopWindowSurface.SALT;
                        if (method.getName().equals("options")) return DesktopWindowOptions.DEFAULT;
                        if (method.getName().equals("managementControls")) return List.of();
                        if (method.getName().equals("popups")) return dismissals[owner] == 0
                                ? List.of(owner == 2 ? undisplayedModal : popup) : List.of();
                        if (method.getName().equals("dismissPopup")) {
                            assertEquals("outside", args[1]);
                            assertEquals(DesktopPopup.Dismissal.OUTSIDE_PRESS, args[2]);
                            dismissals[owner]++;
                            return true;
                        }
                        if (method.getName().equals("mouseClicked")) { bodyClicks.incrementAndGet(); return true; }
                        return method.getReturnType() == boolean.class ? false : null;
                    }));
            invoke(screen, "commitApiSurface", view);
        }
        DesktopWorkspace workspace = screen.workspaceAccess();
        workspace.transferPair(id(first), id(second));
        assertFalse(workspace.containsPointer(id(first), -200, -200));
        assertFalse(workspace.dismissOutsidePointer(id(first), 195, 25), "An actual popup hit is not an outside press");
        assertFalse(workspace.dismissOutsidePointer(id(first), Double.NaN, 25));
        assertFalse(workspace.dismissOutsidePointer(id(undisplayed), -200, -200));
        set(first, "persistentHidden", true);
        assertFalse(workspace.dismissOutsidePointer(id(first), -200, -200));
        set(first, "persistentHidden", false);
        assertTrue(workspace.dismissOutsidePointer(id(first), -200, -200));
        assertArrayEquals(new int[]{1, 0, 0}, dismissals, "The supplied owner's coordinates must never be applied to another popup");
        assertFalse(workspace.dismissOutsidePointer(id(first), -200, -200), "The native dismissal removes the popup exactly once");
        set(screen, "workspacePointerWindow", first);
        assertEquals(false, invoke(screen, "dismissApiOutsidePress", -300.0, -300.0));
        set(screen, "workspacePointerWindow", null);
        assertFalse(workspace.dismissOutsidePointer(id(second), -300, -300));
        assertArrayEquals(new int[]{1, 1, 0}, dismissals);
        assertEquals(0, bodyClicks.get());
        assertNull(field(screen, "apiPointerCaptureWindow"));
        assertNull(field(screen, "movingWindow"));
        assertNull(field(screen, "resizingWindow"));
    }

    @Test void nativeTextInputPreservesSupplementaryUnicodeCodepoints() throws Exception {
        InventoryDesktopScreen screen = screen();
        Object view = window(screen, "CLIENT_API", "Native text");
        Object other = window(screen, "CLIENT_API", "Other");
        AtomicInteger received = new AtomicInteger();
        set(view, "apiDefinition", Proxy.newProxyInstance(DesktopWindowDefinition.class.getClassLoader(),
                new Class<?>[]{DesktopWindowDefinition.class}, (proxy, method, args) -> {
                    if (method.getName().equals("surface")) return DesktopWindowSurface.SALT;
                    if (method.getName().equals("options")) return DesktopWindowOptions.DEFAULT;
                    if (method.getName().equals("managementControls") || method.getName().equals("popups")) return List.of();
                    if (method.getName().equals("charTyped")) {
                        received.set(((net.minecraft.client.input.CharacterEvent) args[1]).codepoint());
                        return true;
                    }
                    return method.getReturnType() == boolean.class ? false : null;
                }));
        DesktopWorkspace workspace = screen.workspaceAccess();
        workspace.transferPair(id(view), id(other));
        set(screen, "apiKeyboardFocusWindow", view);
        assertTrue(workspace.charTyped(0x1F600, 0));
        assertEquals(0x1F600, received.get(), "Native text events must not truncate a supplementary character to a UTF-16 char");
    }

    @Test void dynamicApiControlsAndVirtualEntriesFollowCommittedSurfaceWithoutCreatingSlots() throws Exception {
        InventoryDesktopScreen screen=screen();Object view=window(screen,"CLIENT_API","Unknown controls");
        DesktopWindowSurface surface=DesktopWindowSurface.SALT.withRegions(List.of(
            DesktopWindowRegion.interactive(12,24,32,16).withRole(DesktopHitRole.CONTROL),
            DesktopWindowRegion.interactive(12,64,16,16).withRole(DesktopHitRole.SLOT),
            DesktopWindowRegion.visual(80,20,32,16),DesktopWindowRegion.interactive(0,0,100,100)));
        set(view,"apiSurface",surface);set(view,"apiDefinition",nativeDefinition(surface,new AtomicInteger(),new AtomicInteger(),List.of()));
        DesktopWorkspace workspace=screen.workspaceAccess();
        workspace.transferPair(id(view),id(view));
        var first=workspace.visibleControls(id(view));assertEquals(2,first.size());
        int x=(Integer)field(view,"x"),y=(Integer)field(view,"y");
        assertEquals(new DesktopWindowBounds(x+12,y+24,32,16),first.getFirst());
        assertNull(workspace.hoveredSlot(id(view),x+20,y+72),"A virtual API entry must retain its native handler, not invent an inventory slot");
        set(view,"x",x+37);set(view,"y",y+13);
        assertEquals(new DesktopWindowBounds(x+49,y+37,32,16),workspace.visibleControls(id(view)).getFirst());
        set(view,"persistentHidden",true);assertTrue(workspace.visibleControls(id(view)).isEmpty());
    }

    @Test void adjacentVirtualEntriesKeepDistinctColumnsAtNativeSpacing() throws Exception {
        InventoryDesktopScreen screen=screen();Object view=window(screen,"CLIENT_API","Virtual grid");
        var emptyMenu=new TestMenu();emptyMenu.slots.clear();set(view,"legacyMenu",emptyMenu);
        var regions=java.util.stream.IntStream.range(0,5).mapToObj(i ->
            DesktopWindowRegion.interactive(7+i*18,24,18,18).withRole(DesktopHitRole.SLOT)).toList();
        var surface=DesktopWindowSurface.SALT.withRegions(regions);
        set(view,"apiSurface",surface);set(view,"apiDefinition",nativeDefinition(surface,new AtomicInteger(),new AtomicInteger(),List.of()));
        var workspace=screen.workspaceAccess();workspace.transferPair(id(view),id(view));
        var targets=workspace.controllerTargets(id(view));
        assertEquals(5,targets.size());
        assertEquals(5,targets.stream().map(t -> t.column()).distinct().count(),"Native spacing must not merge adjacent controls");
        assertEquals(1,targets.stream().map(t -> t.row()).distinct().count());
    }

    @Test void onlyTheFocusedApiEditorRequestsAKeyboard() throws Exception {
        InventoryDesktopScreen screen=screen();Object view=window(screen,"CLIENT_API","Editor");
        Object other=window(screen,"CLIENT_API","Modal buttons");
        var editing=new java.util.concurrent.atomic.AtomicBoolean();
        set(view,"apiDefinition",Proxy.newProxyInstance(DesktopWindowDefinition.class.getClassLoader(),new Class<?>[]{DesktopWindowDefinition.class},
            (proxy,method,args) -> method.getName().equals("wantsTextInput") ? editing.get() : method.getReturnType()==boolean.class?false:null));
        set(screen,"apiKeyboardFocusWindow",view);DesktopWorkspace workspace=screen.workspaceAccess();
        workspace.transferPair(id(view),id(other));
        assertFalse(workspace.textInputActive(id(view)));editing.set(true);
        assertTrue(workspace.textInputActive(id(view)));assertFalse(workspace.textInputActive(id(other)));
        set(screen,"apiKeyboardFocusWindow",other);assertFalse(workspace.textInputActive(id(view)));
    }

    private static Object nativeDefinition(DesktopWindowSurface surface, AtomicInteger clicks, AtomicInteger paints,
                                            List<DesktopSurfaceControl> controls) {
        return Proxy.newProxyInstance(DesktopWindowDefinition.class.getClassLoader(), new Class<?>[]{DesktopWindowDefinition.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "surface" -> surface;
                    case "options" -> DesktopWindowOptions.DEFAULT;
                    case "resizePolicy" -> DesktopResizePolicy.FREE;
                    case "minSize" -> DesktopWindowSize.of(64, 48);
                    case "managementControls" -> controls;
                    case "popups", "controllerScrollRegions", "textInputBounds" -> List.of();
                    case "mouseClicked" -> { clicks.incrementAndGet(); yield true; }
                    case "renderManagementControls" -> { paints.incrementAndGet(); yield null; }
                    default -> method.getReturnType() == boolean.class ? false : null;
                });
    }

    private static void assertRejected(InventoryDesktopScreen screen, int session) throws Exception {
        assertTrue((Integer) invoke(invoke(screen, "quickMoveTarget", hit(session)), "kind") < 0,
                "An absent/invalid exact destination must reject rather than transfer to an unrelated inventory");
    }

    private static InventoryDesktopScreen screen() throws Exception {
        InventoryDesktopScreen screen = (InventoryDesktopScreen) unsafe.allocateInstance(InventoryDesktopScreen.class);
        set(screen, "windows", new ArrayList<>());
        set(screen, "sessions", new ArrayList<>());
        set(screen, "sharedCarried", ItemStack.EMPTY);
        set(screen, "workspaceProtectedWindows", new HashSet<UUID>());
        set(null, InventoryDesktopScreen.class, "singleton", screen);
        return screen;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Object window(InventoryDesktopScreen screen, String name, String title) throws Exception {
        Class<?> type = Class.forName(InventoryDesktopScreen.class.getName() + "$InventoryWindow");
        Class<?> kind = Class.forName(InventoryDesktopScreen.class.getName() + "$WindowKind");
        Constructor<?> constructor = type.getDeclaredConstructor(kind, Component.class, int.class, int.class, int.class, int.class);
        constructor.setAccessible(true);
        Object window = constructor.newInstance(Enum.valueOf((Class) kind, name), Component.literal(title), 10, 20, 176, 166);
        windows(screen).add(window);
        return window;
    }

    private static void attachSession(Object window, int id) throws Exception {
        DesktopContainerSession session = (DesktopContainerSession) unsafe.allocateInstance(DesktopContainerSession.class);
        set(session, "sessionId", id);
        set(session, "companionOwnerIds", new int[0]);
        set(window, "session", session);
    }

    private static Object hit(int sessionId) throws Exception {
        TestMenu menu = new TestMenu();
        Class<?> hit = Class.forName(InventoryDesktopScreen.class.getName() + "$SlotHit");
        Constructor<?> constructor = hit.getDeclaredConstructor(Slot.class, int.class, int.class, int.class, AbstractContainerMenu.class, int.class);
        constructor.setAccessible(true);
        return constructor.newInstance(menu.slots.getFirst(), 0, 0, 0, menu, sessionId);
    }

    private static final class TestMenu extends AbstractContainerMenu {
        TestMenu() { super(null, 0); addSlot(new Slot(new SimpleContainer(1), 0, 0, 0)); }
        @Override public boolean stillValid(Player player) { return true; }
        @Override public ItemStack quickMoveStack(Player player, int slot) { return ItemStack.EMPTY; }
    }

    private static UUID id(Object window) throws Exception { return (UUID) field(window, "apiInstanceId"); }
    @SuppressWarnings("unchecked")
    private static List<Object> windows(InventoryDesktopScreen screen) throws Exception { return (List<Object>) field(screen, "windows"); }
    private static Object field(Object target, String name) throws Exception { return field(target, target.getClass(), name); }
    private static Object field(Object target, Class<?> type, String name) throws Exception {
        Field field = type.getDeclaredField(name); field.setAccessible(true); return field.get(target);
    }
    private static void set(Object target, String name, Object value) throws Exception { set(target, target.getClass(), name, value); }
    private static void set(Object target, Class<?> type, String name, Object value) throws Exception {
        Field field = type.getDeclaredField(name); field.setAccessible(true); field.set(target, value);
    }
    private static Object invoke(Object target, String name, Object... args) throws Exception {
        Method method = List.of(target.getClass().getDeclaredMethods()).stream()
                .filter(candidate -> candidate.getName().equals(name) && candidate.getParameterCount() == args.length)
                .findFirst().orElseThrow();
        method.setAccessible(true); return method.invoke(target, args);
    }
}
