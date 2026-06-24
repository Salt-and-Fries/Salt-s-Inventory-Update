package com.salts_inventory_update.client;

import com.mojang.blaze3d.platform.InputConstants;
import java.lang.reflect.Field;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.Options;
import com.salts_inventory_update.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.glfw.GLFW;

import com.salts_inventory_update.SaltsInventoryUpdate;
import com.salts_inventory_update.SaltsInventoryRuntime;
import com.salts_inventory_update.compat.toms_storage.client.TomsStorageClientCompat;
import com.salts_inventory_update.mixin.client.MouseHandlerAccessor;

public final class WindowedInventoryClient {
    private static KeyMapping characterWindowKey;
    private static boolean customMouseGrab;
    private static Field mouseGrabbedField;
    private static Field mouseXposField;
    private static Field mouseYposField;
    private static int pendingConfigScreenOpenTicks;

    private WindowedInventoryClient() {
    }

    public static void initialize() {
        SaltsInventoryConfig.load();
        characterWindowKey = KeyBindingHelper.registerKeyBinding(
            new KeyMapping(
                "key.salts_inventory_update.character_window",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_C,
                KeyMapping.CATEGORY_INVENTORY
            )
        );

        InventoryDesktopScreen.registerInternalApiDefinitions();
        TomsStorageClientCompat.initialize();
        InventoryDesktopScreen.registerContainerScreens();
        DesktopContainerClient.initializeNetworking();
        registerClientCommands();
        initializeFunctionalTests();
        ClientTickEvents.START_CLIENT_TICK.register(WindowedInventoryClient::syncDesktopMovementKeys);
        ClientTickEvents.END_CLIENT_TICK.register(WindowedInventoryClient::onClientTick);
    }

    public static KeyMapping characterWindowKey() {
        return characterWindowKey;
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(SaltsInventoryUpdate.MOD_ID, path);
    }

    private static void registerClientCommands() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(
            ClientCommandManager.literal("salts_inventory")
                .then(ClientCommandManager.literal("config").executes(context -> {
                    Minecraft minecraft = Minecraft.getInstance();
                    minecraft.execute(() -> pendingConfigScreenOpenTicks = 2);
                    return 1;
                }))
        ));
    }

    private static void initializeFunctionalTests() {
        if (!functionalTestsRequested()) {
            return;
        }

        try {
            Class.forName("com.salts_inventory_update.functionaltest.FunctionalTestHarness")
                .getMethod("tryInitialize")
                .invoke(null);
        } catch (ClassNotFoundException exception) {
            SaltsInventoryUpdate.LOGGER.warn("Functional tests requested, but test sources were not included. Re-run with -PincludeFunctionalTests=true.");
        } catch (ReflectiveOperationException exception) {
            SaltsInventoryUpdate.LOGGER.error("Functional test harness failed to initialize", exception);
        }
    }

    private static boolean functionalTestsRequested() {
        return Boolean.getBoolean("salts_inventory_update.functionalTests")
            || isTruthy(System.getenv("SIU_FUNCTIONAL_TESTS"));
    }

    private static boolean isTruthy(String value) {
        return value != null && (
            value.equalsIgnoreCase("true")
                || value.equalsIgnoreCase("1")
                || value.equalsIgnoreCase("yes")
                || value.equalsIgnoreCase("on")
        );
    }

    public static boolean isAltDown(Minecraft minecraft) {
        return InputConstants.isKeyDown(minecraft.getWindow().getWindow(), GLFW.GLFW_KEY_LEFT_ALT)
            || InputConstants.isKeyDown(minecraft.getWindow().getWindow(), GLFW.GLFW_KEY_RIGHT_ALT);
    }

    private static void onClientTick(Minecraft minecraft) {
        DesktopContainerClient.tick(minecraft);
        openPendingConfigScreen(minecraft);
        if (!SaltsInventoryRuntime.isEnabled()) {
            InventoryKeyHoldController.reset();
            InventoryDesktopScreen.reset(minecraft);
            setCameraMouseGrab(minecraft, false);
            if (minecraft.screen instanceof InventoryDesktopScreen) {
                minecraft.setScreen(null);
            }
            return;
        }

        InventoryKeyHoldController.tick(minecraft);
        if (minecraft.player == null || minecraft.level == null) {
            InventoryKeyHoldController.reset();
            InventoryDesktopScreen.reset(minecraft);
            setCameraMouseGrab(minecraft, false);
            return;
        }

        Screen screen = minecraft.screen;
        boolean desktopTextInput = screen instanceof InventoryDesktopScreen inventoryScreen && inventoryScreen.isTextInputActive();
        while (characterWindowKey.consumeClick()) {
            if (!desktopTextInput) {
                InventoryDesktopScreen.openOrToggleCharacter(minecraft);
            }
        }

        if (screen instanceof InventoryDesktopScreen inventoryScreen) {
            boolean altDown = isAltDown(minecraft);
            boolean hasWindows = inventoryScreen.hasWindows();
            boolean desktopActive = hasWindows || inventoryScreen.isHotbarOnly();
            inventoryScreen.setCameraControl(altDown && hasWindows);
            syncMovementKeys(minecraft, desktopActive && !inventoryScreen.isTextInputActive(), !hasWindows || inventoryScreen.isCameraControlActive());
            setCameraMouseGrab(minecraft, inventoryScreen.isCameraControlActive());

            if (inventoryScreen.isHotbarOnly() && !altDown && inventoryScreen.canCloseHotbarOnly()) {
                inventoryScreen.onClose();
            }
        } else {
            setCameraMouseGrab(minecraft, false);
            InventoryDesktopScreen.tickPassiveGhostWindows(minecraft);
            if (screen == null && isAltDown(minecraft)) {
                InventoryDesktopScreen.openHotbarOnly(minecraft);
            }
        }
    }

    private static void openPendingConfigScreen(Minecraft minecraft) {
        if (pendingConfigScreenOpenTicks <= 0) {
            return;
        }

        pendingConfigScreenOpenTicks--;
        if (pendingConfigScreenOpenTicks > 0) {
            return;
        }

        SaltsInventoryConfig.reload();
        Screen previousScreen = minecraft.screen instanceof ChatScreen || minecraft.screen instanceof SaltsInventoryConfigScreen ? null : minecraft.screen;
        minecraft.setScreen(new SaltsInventoryConfigScreen(previousScreen));
    }

    public static void extractPassiveGhostWindows(GuiGraphicsExtractor graphics) {
        if (!SaltsInventoryRuntime.isEnabled()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        InventoryDesktopScreen.extractPassiveGhostWindows(minecraft, graphics);
        if (!(minecraft.screen instanceof InventoryDesktopScreen)) {
            InventoryKeyHoldController.extractOverlay(minecraft, graphics);
        }
    }

    public static boolean shouldHideCrosshair() {
        if (!SaltsInventoryRuntime.isEnabled()) {
            return false;
        }

        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.screen instanceof InventoryDesktopScreen screen
            && screen.hasWindows()
            && !screen.isCameraControlActive();
    }

    private static void syncDesktopMovementKeys(Minecraft minecraft) {
        if (!SaltsInventoryRuntime.isEnabled()) {
            return;
        }

        if (minecraft.screen instanceof InventoryDesktopScreen screen && (screen.hasWindows() || screen.isHotbarOnly())) {
            syncMovementKeys(minecraft, !screen.isTextInputActive(), !screen.hasWindows() || screen.isCameraControlActive());
        }
    }

    public static void syncMovementKeys(Minecraft minecraft, boolean enabled) {
        syncMovementKeys(minecraft, enabled, true);
    }

    public static void syncMovementKeys(Minecraft minecraft, boolean enabled, boolean allowShift) {
        Options options = minecraft.options;
        syncKey(minecraft, options.keyUp, enabled);
        syncKey(minecraft, options.keyLeft, enabled);
        syncKey(minecraft, options.keyDown, enabled);
        syncKey(minecraft, options.keyRight, enabled);
        syncKey(minecraft, options.keyJump, enabled);
        syncKey(minecraft, options.keyShift, enabled && allowShift);
        syncKey(minecraft, options.keySprint, enabled);
    }

    private static void syncKey(Minecraft minecraft, KeyMapping keyMapping, boolean enabled) {
        if (!enabled) {
            keyMapping.setDown(false);
            return;
        }

        InputConstants.Key key = KeyBindingHelper.getBoundKeyOf(keyMapping);
        if (key.getType() == InputConstants.Type.KEYSYM && key.getValue() != InputConstants.UNKNOWN.getValue()) {
            keyMapping.setDown(InputConstants.isKeyDown(minecraft.getWindow().getWindow(), key.getValue()));
        }
    }

    public static void setCameraMouseGrab(Minecraft minecraft, boolean grabbed) {
        MouseHandler mouseHandler = minecraft.mouseHandler;
        if (grabbed) {
            if (!customMouseGrab) {
                double x = minecraft.getWindow().getScreenWidth() / 2.0;
                double y = minecraft.getWindow().getScreenHeight() / 2.0;
                setMouseGrabState(mouseHandler, x, y, true);
                InputConstants.grabOrReleaseMouse(minecraft.getWindow().getWindow(), GLFW.GLFW_CURSOR_DISABLED, x, y);
                mouseHandler.setIgnoreFirstMove();
                customMouseGrab = true;
            }
            return;
        }

        if (customMouseGrab) {
            double x = minecraft.getWindow().getScreenWidth() / 2.0;
            double y = minecraft.getWindow().getScreenHeight() / 2.0;
            setMouseGrabState(mouseHandler, x, y, false);
            InputConstants.grabOrReleaseMouse(minecraft.getWindow().getWindow(), GLFW.GLFW_CURSOR_NORMAL, x, y);
            mouseHandler.setIgnoreFirstMove();
            customMouseGrab = false;
        }
    }

    private static void setMouseGrabState(MouseHandler mouseHandler, double x, double y, boolean grabbed) {
        if (mouseHandler instanceof MouseHandlerAccessor accessor) {
            accessor.salts_inventory_update$setXpos(x);
            accessor.salts_inventory_update$setYpos(y);
            accessor.salts_inventory_update$setMouseGrabbed(grabbed);
            return;
        }

        try {
            mouseXposField().setDouble(mouseHandler, x);
            mouseYposField().setDouble(mouseHandler, y);
            mouseGrabbedField().setBoolean(mouseHandler, grabbed);
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException("Unable to update mouse grab state", exception);
        }
    }

    private static Field mouseGrabbedField() {
        if (mouseGrabbedField == null) {
            mouseGrabbedField = mouseHandlerField("mouseGrabbed");
        }
        return mouseGrabbedField;
    }

    private static Field mouseXposField() {
        if (mouseXposField == null) {
            mouseXposField = mouseHandlerField("xpos");
        }
        return mouseXposField;
    }

    private static Field mouseYposField() {
        if (mouseYposField == null) {
            mouseYposField = mouseHandlerField("ypos");
        }
        return mouseYposField;
    }

    private static Field mouseHandlerField(String name) {
        try {
            Field field = MouseHandler.class.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            throw new IllegalStateException("Unable to access MouseHandler." + name, exception);
        }
    }
}
