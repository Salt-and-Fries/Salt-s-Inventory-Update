package com.salts_inventory_update.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import org.lwjgl.glfw.GLFW;

import com.salts_inventory_update.SaltsInventoryRuntime;
import com.salts_inventory_update.debug.DesktopDebug;

public final class InventoryKeyHoldController {
    private static final int BOX_BACKGROUND = 0xCC101010;
    private static final int BOX_OUTLINE = 0xFF303030;
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int PROGRESS_COLOR = 0xFFFFD84A;

    private static boolean holdingInventoryKey;
    private static boolean closedByHold;
    private static long holdStartedMs;

    private InventoryKeyHoldController() {
    }

    public static boolean handleInventoryKeyAction(Minecraft minecraft, int action, KeyEvent event) {
        if (!minecraft.options.keyInventory.matches(event) || !canHandleInventoryKey(minecraft)) {
            return false;
        }

        if (action == GLFW.GLFW_PRESS) {
            beginHold();
            return true;
        }

        if (action == GLFW.GLFW_REPEAT) {
            return holdingInventoryKey;
        }

        if (action == GLFW.GLFW_RELEASE) {
            finishHold(minecraft);
            return true;
        }

        return false;
    }

    public static void tick(Minecraft minecraft) {
        if (!holdingInventoryKey) {
            return;
        }

        if (!canHandleInventoryKey(minecraft)) {
            reset();
            return;
        }

        if (!closedByHold && elapsedMs() >= closeAllMs()) {
            closedByHold = true;
            DesktopDebug.log("client E hold close-all elapsedMs={}", elapsedMs());
            InventoryDesktopScreen.closeAllOpenWindows(minecraft);
        }
    }

    public static void reset() {
        holdingInventoryKey = false;
        closedByHold = false;
        holdStartedMs = 0L;
    }

    public static void extractOverlay(Minecraft minecraft, GuiGraphicsExtractor graphics) {
        long overlayDelayMs = overlayDelayMs();
        long closeAllMs = closeAllMs();
        if (!holdingInventoryKey || closedByHold || elapsedMs() < overlayDelayMs || !canHandleInventoryKey(minecraft)) {
            return;
        }

        Font font = minecraft.font;
        String label = "Closing All";
        int textWidth = font.width(label);
        int width = Math.max(104, textWidth + 24);
        int height = 24;
        int x = (graphics.guiWidth() - width) / 2;
        int y = 8;
        int textX = x + (width - textWidth) / 2;
        int textY = y + 8;
        float progress = (float) Math.min(1.0, Math.max(0.0, (elapsedMs() - overlayDelayMs) / (double) Math.max(1L, closeAllMs - overlayDelayMs)));

        graphics.fill(x, y, x + width, y + height, BOX_BACKGROUND);
        graphics.outline(x, y, width, height, BOX_OUTLINE);
        graphics.text(font, label, textX, textY, TEXT_COLOR, false);
        drawWrappingProgress(graphics, x, y, width, height, progress);
    }

    private static void beginHold() {
        if (holdingInventoryKey) {
            return;
        }

        holdingInventoryKey = true;
        closedByHold = false;
        holdStartedMs = System.currentTimeMillis();
        DesktopDebug.trace("client E hold begin");
    }

    private static void finishHold(Minecraft minecraft) {
        if (!holdingInventoryKey) {
            return;
        }

        long elapsed = elapsedMs();
        boolean shouldToggle = !closedByHold && elapsed < closeAllMs();
        DesktopDebug.trace("client E hold finish elapsedMs={} toggle={}", elapsed, shouldToggle);
        reset();

        if (shouldToggle) {
            openInventoryFromRelease(minecraft);
        }
    }

    private static void openInventoryFromRelease(Minecraft minecraft) {
        if (minecraft.player == null || minecraft.gameMode == null) {
            return;
        }

        if (minecraft.gameMode.isServerControlledInventory()) {
            minecraft.player.sendOpenInventory();
        } else {
            InventoryDesktopScreen.openOrToggleInventory(minecraft);
        }
    }

    private static boolean canHandleInventoryKey(Minecraft minecraft) {
        if (!SaltsInventoryRuntime.isEnabled()) {
            return false;
        }

        if (minecraft.player == null || minecraft.gameMode == null) {
            return false;
        }

        Screen screen = minecraft.screen;
        if (screen instanceof InventoryDesktopScreen desktop && desktop.isTextInputActive()) {
            return false;
        }

        return screen == null || screen instanceof InventoryDesktopScreen;
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

    private static void drawWrappingProgress(GuiGraphicsExtractor graphics, int x, int y, int width, int height, float progress) {
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
}
