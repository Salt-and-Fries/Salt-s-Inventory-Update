package com.salts_inventory_update.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import com.salts_inventory_update.platform.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import org.jspecify.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import com.salts_inventory_update.SaltsInventoryRuntime;
import com.salts_inventory_update.debug.DesktopDebug;

public final class InventoryKeyHoldController {
    private static final int PROGRESS_COLOR = 0xFFFFD84A;

    private static @Nullable HoldAction holdAction;
    private static boolean closedByHold;
    private static boolean suppressEscapeUntilRelease;
    private static long holdStartedMs;

    private InventoryKeyHoldController() {
    }

    public static boolean handleInventoryKeyAction(Minecraft minecraft, int action, KeyEvent event) {
        if (consumeSuppressedEscapeAction(action, event)) {
            return true;
        }

        return handleAction(minecraft, action, holdAction(minecraft, event));
    }

    public static boolean handleInventoryMouseAction(Minecraft minecraft, int action, int button) {
        InputConstants.Key binding = KeyMappingHelper.getBoundKeyOf(minecraft.options.keyInventory);
        if (binding.getType() != InputConstants.Type.MOUSE || binding.getValue() != button
            || !KeyMappingHelper.isKeyModifierActive(minecraft.options.keyInventory)) return false;
        return handleAction(minecraft, action, HoldAction.INVENTORY);
    }

    private static boolean handleAction(Minecraft minecraft, int action, @Nullable HoldAction requestedAction) {
        if (requestedAction == null || !canHandleHoldAction(minecraft, requestedAction)) {
            return false;
        }

        if (action == GLFW.GLFW_PRESS) {
            beginHold(requestedAction);
            return true;
        }

        if (action == GLFW.GLFW_REPEAT) {
            return holdAction == requestedAction;
        }

        if (action == GLFW.GLFW_RELEASE) {
            finishHold(minecraft);
            return true;
        }

        return false;
    }

    public static void tick(Minecraft minecraft) {
        if (holdAction == null) {
            return;
        }

        if (!canHandleHoldAction(minecraft, holdAction)) {
            resetHoldState();
            return;
        }

        if (!closedByHold && elapsedMs() >= closeAllMs()) {
            closedByHold = true;
            DesktopDebug.log("client {} hold elapsedMs={}", holdAction.debugName(), elapsedMs());
            if (holdAction == HoldAction.ESCAPE) {
                suppressEscapeUntilRelease = true;
                InventoryDesktopScreen.permanentlyCloseAllOpenWindows(minecraft);
            } else {
                DesktopTopTab.closeAll(minecraft);
            }
        }
    }

    public static void reset() {
        resetHoldState();
        suppressEscapeUntilRelease = false;
    }

    private static void resetHoldState() {
        holdAction = null;
        closedByHold = false;
        holdStartedMs = 0L;
    }

    public static boolean isHolding() { return holdAction != null && !closedByHold; }

    public static float holdProgress() {
        return (float)Math.min(1.0, Math.max(0.0, (elapsedMs() - overlayDelayMs())
            / (double)Math.max(1L, closeAllMs() - overlayDelayMs())));
    }

    private static void beginHold(HoldAction action) {
        if (holdAction != null) {
            return;
        }

        holdAction = action;
        closedByHold = false;
        holdStartedMs = System.currentTimeMillis();
        DesktopDebug.trace("client {} hold begin", action.debugName());
    }

    private static boolean consumeSuppressedEscapeAction(int action, KeyEvent event) {
        if (!suppressEscapeUntilRelease || !event.isEscape()) {
            return false;
        }

        if (action == GLFW.GLFW_RELEASE) {
            DesktopDebug.trace("client Esc hold suppression released");
            reset();
        }
        return true;
    }

    private static void finishHold(Minecraft minecraft) {
        HoldAction action = holdAction;
        if (action == null) {
            return;
        }

        long elapsed = elapsedMs();
        boolean shouldToggle = !closedByHold && elapsed < closeAllMs();
        DesktopDebug.trace("client {} hold finish elapsedMs={} activate={}", action.debugName(), elapsed, shouldToggle);
        reset();

        if (shouldToggle && action == HoldAction.INVENTORY) {
            openInventoryFromRelease(minecraft);
        } else if (shouldToggle) {
            InventoryDesktopScreen.closeAllOpenWindows(minecraft);
        }
    }

    private static void openInventoryFromRelease(Minecraft minecraft) {
        if (NativeContainerView.isActive(minecraft)) { NativeContainerView.restore(minecraft); return; }
        if (minecraft.player == null || minecraft.gameMode == null) {
            return;
        }

        if (InventoryDesktopScreen.openOrToggleRiddenInventory(minecraft)) {
            return;
        }

        if (minecraft.gameMode.isServerControlledInventory()) {
            minecraft.player.sendOpenInventory();
        } else {
            InventoryDesktopScreen.openOrToggleInventory(minecraft);
        }
    }

    private static boolean canHandleHoldAction(Minecraft minecraft, HoldAction action) {
        if (!SaltsInventoryRuntime.isEnabled()) {
            return false;
        }

        if (minecraft.player == null || minecraft.gameMode == null) {
            return false;
        }

        Screen screen = minecraft.screen;
        if (NativeContainerView.isActive(minecraft)
                && screen.getFocused() instanceof net.minecraft.client.gui.components.EditBox edit && edit.isFocused()) return false;
        if (screen instanceof InventoryDesktopScreen desktop && desktop.isTextInputActive()) {
            return false;
        }

        if (action == HoldAction.ESCAPE) {
            return SaltsInventoryConfig.get().persistentWindows && screen instanceof InventoryDesktopScreen;
        }

        return screen == null || screen instanceof InventoryDesktopScreen || NativeContainerView.isActive(minecraft);
    }

    private static @Nullable HoldAction holdAction(Minecraft minecraft, KeyEvent event) {
        if (minecraft.options.keyInventory.matches(event)) {
            return HoldAction.INVENTORY;
        }
        if (event.isEscape()) {
            return HoldAction.ESCAPE;
        }
        return null;
    }

    private static long elapsedMs() {
        return holdStartedMs == 0L ? 0L : System.currentTimeMillis() - holdStartedMs;
    }

    private static long closeAllMs() {
        return SaltsInventoryConfig.get().eHoldCloseAllMs();
    }

    private static long overlayDelayMs() {
        return SaltsInventoryConfig.get().eHoldOverlayDelayMs();
    }

    static void drawWrappingProgress(GuiGraphicsExtractor graphics, int x, int y, int width, int height, float progress) {
        int thickness = 2;
        int halfWidth = width / 2;
        int remaining = Math.round((width * 2 + height * 2) * progress);

        remaining = drawTopRight(graphics, x + halfWidth, y, width - halfWidth, thickness, remaining);
        remaining = drawRight(graphics, x + width - thickness, y, height, thickness, remaining);
        remaining = drawBottom(graphics, x, y + height - thickness, width, thickness, remaining);
        remaining = drawLeft(graphics, x, y, height, thickness, remaining);
        drawTopLeft(graphics, x, y, halfWidth, thickness, remaining);
    }

    private static int drawTopRight(GuiGraphicsExtractor graphics, int x, int y, int length, int thickness, int remaining) {
        int drawn = Math.min(length, remaining);
        if (drawn > 0) {
            graphics.fill(x, y, x + drawn, y + thickness, PROGRESS_COLOR);
        }
        return remaining - drawn;
    }

    private static int drawRight(GuiGraphicsExtractor graphics, int x, int y, int length, int thickness, int remaining) {
        int drawn = Math.min(length, remaining);
        if (drawn > 0) {
            graphics.fill(x, y, x + thickness, y + drawn, PROGRESS_COLOR);
        }
        return remaining - drawn;
    }

    private static int drawBottom(GuiGraphicsExtractor graphics, int x, int y, int length, int thickness, int remaining) {
        int drawn = Math.min(length, remaining);
        if (drawn > 0) {
            graphics.fill(x + length - drawn, y, x + length, y + thickness, PROGRESS_COLOR);
        }
        return remaining - drawn;
    }

    private static int drawLeft(GuiGraphicsExtractor graphics, int x, int y, int length, int thickness, int remaining) {
        int drawn = Math.min(length, remaining);
        if (drawn > 0) {
            graphics.fill(x, y + length - drawn, x + thickness, y + length, PROGRESS_COLOR);
        }
        return remaining - drawn;
    }

    private static void drawTopLeft(GuiGraphicsExtractor graphics, int x, int y, int length, int thickness, int remaining) {
        int drawn = Math.min(length, remaining);
        if (drawn > 0) {
            graphics.fill(x, y, x + drawn, y + thickness, PROGRESS_COLOR);
        }
    }

    private enum HoldAction {
        INVENTORY("Inventory"),
        ESCAPE("Esc");

        private final String debugName;

        HoldAction(String debugName) {
            this.debugName = debugName;
        }

        private String debugName() {
            return this.debugName;
        }
    }
}
