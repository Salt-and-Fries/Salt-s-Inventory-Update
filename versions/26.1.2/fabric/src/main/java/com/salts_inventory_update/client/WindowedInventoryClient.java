package com.salts_inventory_update.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.Options;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

import com.salts_inventory_update.SaltsInventoryUpdate;
import com.salts_inventory_update.compat.toms_storage.client.TomsStorageClientCompat;
import com.salts_inventory_update.mixin.client.MouseHandlerAccessor;

public final class WindowedInventoryClient {
    private static KeyMapping characterWindowKey;
    private static boolean customMouseGrab;

    private WindowedInventoryClient() {
    }

    public static void initialize() {
        SaltsInventoryConfig.load();
        characterWindowKey = KeyMappingHelper.registerKeyMapping(
            new KeyMapping(
                "key.salts_inventory_update.character_window",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_C,
                KeyMapping.Category.INVENTORY
            )
        );

        InventoryDesktopScreen.registerInternalApiDefinitions();
        TomsStorageClientCompat.initialize();
        InventoryDesktopScreen.registerContainerScreens();
        DesktopContainerClient.initializeNetworking();
        registerClientCommands();
        ClientTickEvents.START_CLIENT_TICK.register(WindowedInventoryClient::syncDesktopMovementKeys);
        ClientTickEvents.END_CLIENT_TICK.register(WindowedInventoryClient::onClientTick);
    }

    public static KeyMapping characterWindowKey() {
        return characterWindowKey;
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(SaltsInventoryUpdate.MOD_ID, path);
    }

    private static void registerClientCommands() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(
            ClientCommands.literal("salts_inventory")
                .then(ClientCommands.literal("config").executes(context -> {
                    Minecraft minecraft = context.getSource().getClient();
                    minecraft.execute(() -> {
                        SaltsInventoryConfig.reload();
                        minecraft.setScreen(new SaltsInventoryConfigScreen(minecraft.screen));
                    });
                    return 1;
                }))
        ));
    }

    public static boolean isAltDown(Minecraft minecraft) {
        return InputConstants.isKeyDown(minecraft.getWindow(), GLFW.GLFW_KEY_LEFT_ALT)
            || InputConstants.isKeyDown(minecraft.getWindow(), GLFW.GLFW_KEY_RIGHT_ALT);
    }

    private static void onClientTick(Minecraft minecraft) {
        InventoryKeyHoldController.tick(minecraft);
        DesktopContainerClient.tick(minecraft);
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

    public static void extractPassiveGhostWindows(GuiGraphicsExtractor graphics) {
        Minecraft minecraft = Minecraft.getInstance();
        InventoryDesktopScreen.extractPassiveGhostWindows(minecraft, graphics);
        if (!(minecraft.screen instanceof InventoryDesktopScreen)) {
            InventoryKeyHoldController.extractOverlay(minecraft, graphics);
        }
    }

    public static boolean shouldHideCrosshair() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.screen instanceof InventoryDesktopScreen screen
            && screen.hasWindows()
            && !screen.isCameraControlActive();
    }

    private static void syncDesktopMovementKeys(Minecraft minecraft) {
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

        InputConstants.Key key = KeyMappingHelper.getBoundKeyOf(keyMapping);
        if (key.getType() == InputConstants.Type.KEYSYM && key.getValue() != InputConstants.UNKNOWN.getValue()) {
            keyMapping.setDown(InputConstants.isKeyDown(minecraft.getWindow(), key.getValue()));
        }
    }

    public static void setCameraMouseGrab(Minecraft minecraft, boolean grabbed) {
        MouseHandler mouseHandler = minecraft.mouseHandler;
        MouseHandlerAccessor accessor = (MouseHandlerAccessor) mouseHandler;
        if (grabbed) {
            if (!customMouseGrab) {
                double x = minecraft.getWindow().getScreenWidth() / 2.0;
                double y = minecraft.getWindow().getScreenHeight() / 2.0;
                accessor.salts_inventory_update$setXpos(x);
                accessor.salts_inventory_update$setYpos(y);
                accessor.salts_inventory_update$setMouseGrabbed(true);
                InputConstants.grabOrReleaseMouse(minecraft.getWindow(), GLFW.GLFW_CURSOR_DISABLED, x, y);
                mouseHandler.setIgnoreFirstMove();
                customMouseGrab = true;
            }
            return;
        }

        if (customMouseGrab) {
            double x = minecraft.getWindow().getScreenWidth() / 2.0;
            double y = minecraft.getWindow().getScreenHeight() / 2.0;
            accessor.salts_inventory_update$setXpos(x);
            accessor.salts_inventory_update$setYpos(y);
            accessor.salts_inventory_update$setMouseGrabbed(false);
            InputConstants.grabOrReleaseMouse(minecraft.getWindow(), GLFW.GLFW_CURSOR_NORMAL, x, y);
            mouseHandler.setIgnoreFirstMove();
            customMouseGrab = false;
        }
    }
}
