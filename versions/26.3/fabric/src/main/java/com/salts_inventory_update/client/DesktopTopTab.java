package com.salts_inventory_update.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/** One screen-edge action shared by the desktop and its full native view. */
public final class DesktopTopTab {
    private static final Identifier TEXTURE = WindowedInventoryClient.id("textures/gui/desktop_top_tab.png");
    private static final DesktopTabState STATE = new DesktopTabState();
    private static boolean pressed;
    private static final java.util.Set<Integer> capturedButtons = new java.util.HashSet<>();

    private DesktopTopTab() { }

    public static boolean available(Minecraft mc) {
        boolean nativeView = NativeContainerView.isActive(mc);
        if (!nativeView && !(mc.gui.screen() instanceof InventoryDesktopScreen desktop
                && desktop.hasTopTabWindows())) return false;
        return DesktopTabState.shouldShow(SaltsInventoryConfig.get().showTopDropdown,
            InventoryKeyHoldController.isHolding(), nativeView);
    }

    public static void reset() { STATE.reset(); pressed = false; capturedButtons.clear(); }

    public static boolean covers(Minecraft mc, double x, double y) {
        return available(mc) && STATE.contains(mc.getWindow().getGuiScaledWidth(), x, y);
    }

    public static boolean mouseButton(Minecraft mc, int button, int action) {
        double x = mc.mouseHandler.getScaledXPos(mc.getWindow());
        double y = mc.mouseHandler.getScaledYPos(mc.getWindow());
        boolean hovered = covers(mc, x, y);
        if (action == InputConstants.PRESS && hovered) {
            capturedButtons.add(button);
            if (button == InputConstants.MOUSE_BUTTON_LEFT) pressed = true;
            return true;
        }
        if (action == InputConstants.RELEASE && capturedButtons.remove(button)) {
            if (button != InputConstants.MOUSE_BUTTON_LEFT) return true;
            pressed = false;
            if (hovered) {
                if (NativeContainerView.isActive(mc) && !InventoryKeyHoldController.isHolding()) NativeContainerView.restore(mc);
                else closeAll(mc);
            }
            return true;
        }
        return false;
    }

    public static void closeAll(Minecraft mc) {
        NativeContainerView.restore(mc);
        InventoryDesktopScreen.permanentlyCloseAllOpenWindows(mc);
    }

    public static void extract(Minecraft mc, GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (!available(mc)) { reset(); return; }
        STATE.update(System.currentTimeMillis(), graphics.guiWidth(), mouseX, mouseY, InventoryKeyHoldController.isHolding());
        int x = STATE.x(graphics.guiWidth()), y = STATE.y();
        boolean hovered = STATE.contains(graphics.guiWidth(), mouseX, mouseY);
        int row = pressed && hovered ? 2 : hovered ? 1 : 0;
        graphics.nextStratum();
        InventoryDesktopScreen.blitRegion(graphics, TEXTURE, x, y, 0, row * DesktopTabState.HEIGHT,
            DesktopTabState.WIDTH, DesktopTabState.HEIGHT, DesktopTabState.WIDTH, DesktopTabState.HEIGHT,
            DesktopTabState.WIDTH, DesktopTabState.HEIGHT * 3);
        Component label = Component.translatable(NativeContainerView.isActive(mc) && !InventoryKeyHoldController.isHolding()
            ? "gui.salts_inventory_update.desktop_tab.return" : "gui.salts_inventory_update.desktop_tab.close_all");
        graphics.text(mc.font, label, x + (DesktopTabState.WIDTH - mc.font.width(label)) / 2, y + 12,
            0xFF202020, false);
        if (InventoryKeyHoldController.isHolding()) InventoryKeyHoldController.drawWrappingProgress(
            graphics, x, y, DesktopTabState.WIDTH, DesktopTabState.HEIGHT, InventoryKeyHoldController.holdProgress());
    }
}
