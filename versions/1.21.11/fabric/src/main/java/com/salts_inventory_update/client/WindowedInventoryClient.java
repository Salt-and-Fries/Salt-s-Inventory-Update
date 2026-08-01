package com.salts_inventory_update.client;

import com.mojang.blaze3d.platform.InputConstants;
import java.lang.reflect.Field;
import com.salts_inventory_update.platform.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import com.salts_inventory_update.platform.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import com.salts_inventory_update.platform.fabric.api.client.command.v2.ClientCommandManager;
import com.salts_inventory_update.platform.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.Options;
import com.salts_inventory_update.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

import com.salts_inventory_update.SaltsInventoryUpdate;
import com.salts_inventory_update.SaltsInventoryRuntime;
import com.salts_inventory_update.compat.toms_storage.client.TomsStorageClientCompat;
import com.salts_inventory_update.mixin.client.MouseHandlerAccessor;

public final class WindowedInventoryClient {
    private static KeyMapping characterWindowKey;
    private static KeyMapping jeiWindowKey;
    private static KeyMapping mouseFocusKey;
    private static boolean customMouseGrab;
    private static Field mouseGrabbedField;
    private static Field mouseXposField;
    private static Field mouseYposField;
    private static int pendingInstructionsWindowOpenTicks;

    private WindowedInventoryClient() {
    }

    public static void initialize() {
        SaltsInventoryConfig.load();
        characterWindowKey = KeyBindingHelper.registerKeyBinding(
            new KeyMapping(
                "key.salts_inventory_update.character_window",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_C,
                KeyMapping.Category.INVENTORY
            )
        );
        jeiWindowKey = KeyBindingHelper.registerKeyBinding(
            new KeyMapping(
                "key.salts_inventory_update.jei_window",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_H,
                KeyMapping.Category.INVENTORY
            )
        );
        mouseFocusKey = KeyBindingHelper.registerKeyBinding(
            new KeyMapping(
                "key.salts_inventory_update.mouse_focus",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_LEFT_ALT,
                KeyMapping.Category.INVENTORY
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

    public static KeyMapping jeiWindowKey() {
        return jeiWindowKey;
    }

    public static KeyMapping mouseFocusKey() {
        return mouseFocusKey;
    }

    public static String mouseFocusKeyName() {
        return mouseFocusKey == null ? "Left Alt" : mouseFocusKey.getTranslatedKeyMessage().getString();
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(SaltsInventoryUpdate.MOD_ID, path);
    }

    private static void registerClientCommands() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("saltsinventory")
                .then(ClientCommandManager.literal("config").executes(context -> {
                    Minecraft minecraft = Minecraft.getInstance();
                    minecraft.execute(() -> minecraft.setScreen(createConfigScreen(minecraft.screen)));
                    return 1;
                }))
                .then(ClientCommandManager.literal("help").executes(context -> {
                    scheduleInstructionsWindowOpen();
                    return 1;
                })));
        });
    }

    public static void scheduleInstructionsWindowOpen() {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.execute(() -> pendingInstructionsWindowOpenTicks = 2);
    }

    public static Screen createConfigScreen(Screen previousScreen) {
        SaltsInventoryConfig.reload();
        InventoryDesktopScreen.syncForcedContainerScreens();
        return new SaltsInventoryConfigScreen(previousScreen);
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
        return InputConstants.isKeyDown(minecraft.getWindow(), GLFW.GLFW_KEY_LEFT_ALT)
            || InputConstants.isKeyDown(minecraft.getWindow(), GLFW.GLFW_KEY_RIGHT_ALT);
    }

    public static boolean isMouseFocusKeyDown(Minecraft minecraft) {
        if (mouseFocusKey == null) {
            return false;
        }
        if (!KeyBindingHelper.isKeyModifierActive(mouseFocusKey)) {
            return false;
        }
        if (mouseFocusKey.isDefault() && isAltDown(minecraft)) {
            return true;
        }
        InputConstants.Key key = KeyBindingHelper.getBoundKeyOf(mouseFocusKey);
        if (key.getValue() == InputConstants.UNKNOWN.getValue()) {
            return false;
        }
        if (key.getType() == InputConstants.Type.KEYSYM) {
            return InputConstants.isKeyDown(minecraft.getWindow(), key.getValue());
        }
        if (key.getType() == InputConstants.Type.MOUSE) {
            return GLFW.glfwGetMouseButton(minecraft.getWindow().handle(), key.getValue()) == GLFW.GLFW_PRESS;
        }
        return false;
    }

    private static void onClientTick(Minecraft minecraft) {
        DesktopContainerClient.tick(minecraft);
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

        openPendingInstructionsWindow(minecraft);
        maybeOpenFirstRunInstructions(minecraft);

        Screen screen = minecraft.screen;
        boolean desktopTextInput = screen instanceof InventoryDesktopScreen inventoryScreen && inventoryScreen.isTextInputActive();
        while (characterWindowKey.consumeClick()) {
            if (!desktopTextInput) {
                InventoryDesktopScreen.openOrToggleCharacter(minecraft);
            }
        }
        while (jeiWindowKey.consumeClick()) {
            if (!desktopTextInput) {
                InventoryDesktopScreen.openOrToggleJei(minecraft);
            }
        }

        if (screen instanceof InventoryDesktopScreen inventoryScreen) {
            boolean mouseFocusDown = isMouseFocusKeyDown(minecraft);
            boolean hasWindows = inventoryScreen.hasWindows();
            boolean desktopActive = hasWindows || inventoryScreen.isHotbarOnly();
            inventoryScreen.setCameraControl(mouseFocusDown && hasWindows);
            syncMovementKeys(minecraft, desktopActive && !inventoryScreen.isTextInputActive(), !hasWindows || inventoryScreen.isCameraControlActive());
            setCameraMouseGrab(minecraft, inventoryScreen.isCameraControlActive());

            if (inventoryScreen.isHotbarOnly() && !mouseFocusDown && inventoryScreen.canCloseHotbarOnly()) {
                inventoryScreen.onClose();
            }
        } else {
            setCameraMouseGrab(minecraft, false);
            InventoryDesktopScreen.tickPassiveGhostWindows(minecraft);
            if (screen == null && isMouseFocusKeyDown(minecraft)) {
                InventoryDesktopScreen.openHotbarOnly(minecraft);
            }
        }
    }

    private static void openPendingInstructionsWindow(Minecraft minecraft) {
        if (pendingInstructionsWindowOpenTicks <= 0) {
            return;
        }

        pendingInstructionsWindowOpenTicks--;
        if (pendingInstructionsWindowOpenTicks > 0) {
            return;
        }

        if (!openInstructionsWindow(minecraft)) {
            pendingInstructionsWindowOpenTicks = 1;
        }
    }

    private static void maybeOpenFirstRunInstructions(Minecraft minecraft) {
        if (!SaltsInventoryConfig.get().hasSeenInstructionsWindow) {
            openInstructionsWindow(minecraft);
        }
    }

    private static boolean openInstructionsWindow(Minecraft minecraft) {
        if (!InventoryDesktopScreen.openInstructions(minecraft)) {
            return false;
        }

        if (!SaltsInventoryConfig.get().hasSeenInstructionsWindow) {
            SaltsInventoryConfig.update(config -> config.hasSeenInstructionsWindow = true);
        }
        return true;
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
            keyMapping.setDown(InputConstants.isKeyDown(minecraft.getWindow(), key.getValue()));
        }
    }

    public static void setCameraMouseGrab(Minecraft minecraft, boolean grabbed) {
        MouseHandler mouseHandler = minecraft.mouseHandler;
        if (grabbed) {
            if (!customMouseGrab) {
                double x = minecraft.getWindow().getScreenWidth() / 2.0;
                double y = minecraft.getWindow().getScreenHeight() / 2.0;
                setMouseGrabState(mouseHandler, x, y, true);
                InputConstants.grabOrReleaseMouse(minecraft.getWindow(), GLFW.GLFW_CURSOR_DISABLED, x, y);
                mouseHandler.setIgnoreFirstMove();
                customMouseGrab = true;
            }
            return;
        }

        if (customMouseGrab) {
            double x = minecraft.getWindow().getScreenWidth() / 2.0;
            double y = minecraft.getWindow().getScreenHeight() / 2.0;
            setMouseGrabState(mouseHandler, x, y, false);
            InputConstants.grabOrReleaseMouse(minecraft.getWindow(), GLFW.GLFW_CURSOR_NORMAL, x, y);
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
