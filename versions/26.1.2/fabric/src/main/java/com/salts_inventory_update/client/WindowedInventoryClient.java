package com.salts_inventory_update.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.Options;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

import com.salts_inventory_update.SaltsInventoryUpdate;
import com.salts_inventory_update.mixin.client.MouseHandlerAccessor;

public final class WindowedInventoryClient {
    private static KeyMapping characterWindowKey;
    private static boolean customMouseGrab;

    private WindowedInventoryClient() {
    }

    public static void initialize() {
        characterWindowKey = KeyMappingHelper.registerKeyMapping(
            new KeyMapping(
                "key.salts_inventory_update.character_window",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_C,
                KeyMapping.Category.INVENTORY
            )
        );

        InventoryDesktopScreen.registerContainerScreens();
        DesktopContainerClient.initializeNetworking();
        ClientTickEvents.START_CLIENT_TICK.register(WindowedInventoryClient::syncDesktopMovementKeys);
        ClientTickEvents.END_CLIENT_TICK.register(WindowedInventoryClient::onClientTick);
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> registerExternalHotbar(screen));
    }

    public static KeyMapping characterWindowKey() {
        return characterWindowKey;
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(SaltsInventoryUpdate.MOD_ID, path);
    }

    public static boolean isAltDown(Minecraft minecraft) {
        return InputConstants.isKeyDown(minecraft.getWindow(), GLFW.GLFW_KEY_LEFT_ALT)
            || InputConstants.isKeyDown(minecraft.getWindow(), GLFW.GLFW_KEY_RIGHT_ALT);
    }

    private static void onClientTick(Minecraft minecraft) {
        DesktopContainerClient.tick(minecraft);
        if (minecraft.player == null || minecraft.level == null) {
            InventoryDesktopScreen.reset(minecraft);
            setCameraMouseGrab(minecraft, false);
            return;
        }

        while (characterWindowKey.consumeClick()) {
            InventoryDesktopScreen.openOrToggleCharacter(minecraft);
        }

        Screen screen = minecraft.screen;
        if (screen instanceof InventoryDesktopScreen inventoryScreen) {
            boolean altDown = isAltDown(minecraft);
            inventoryScreen.setCameraControl(altDown && inventoryScreen.hasWindows());
            syncMovementKeys(minecraft, inventoryScreen.hasWindows());
            setCameraMouseGrab(minecraft, false);

            if (inventoryScreen.isHotbarOnly() && !altDown && inventoryScreen.canCloseHotbarOnly()) {
                inventoryScreen.onClose();
            }
        } else {
            setCameraMouseGrab(minecraft, false);
            if (screen == null && isAltDown(minecraft)) {
                InventoryDesktopScreen.openHotbarOnly(minecraft);
            }
        }
    }

    public static boolean shouldHideCrosshair() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.screen instanceof InventoryDesktopScreen screen
            && screen.hasWindows()
            && !screen.isCameraControlActive();
    }

    private static void registerExternalHotbar(Screen screen) {
        if (screen instanceof InventoryDesktopScreen) {
            return;
        }

        ScreenEvents.afterExtract(screen).register((activeScreen, graphics, mouseX, mouseY, tickProgress) ->
            InventoryDesktopScreen.extractExternalHotbarOverlay(graphics, mouseX, mouseY)
        );
        ScreenMouseEvents.afterMouseClick(screen).register((activeScreen, event, consumed) ->
            consumed || InventoryDesktopScreen.handleExternalHotbarClick(event)
        );
        ScreenMouseEvents.afterMouseRelease(screen).register((activeScreen, event, consumed) ->
            consumed || InventoryDesktopScreen.handleExternalHotbarRelease(event)
        );
    }

    private static void syncDesktopMovementKeys(Minecraft minecraft) {
        if (minecraft.screen instanceof InventoryDesktopScreen screen && screen.hasWindows()) {
            syncMovementKeys(minecraft, true);
        }
    }

    public static void syncMovementKeys(Minecraft minecraft, boolean enabled) {
        Options options = minecraft.options;
        syncKey(minecraft, options.keyUp, enabled);
        syncKey(minecraft, options.keyLeft, enabled);
        syncKey(minecraft, options.keyDown, enabled);
        syncKey(minecraft, options.keyRight, enabled);
        syncKey(minecraft, options.keyJump, enabled);
        syncKey(minecraft, options.keyShift, enabled);
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
