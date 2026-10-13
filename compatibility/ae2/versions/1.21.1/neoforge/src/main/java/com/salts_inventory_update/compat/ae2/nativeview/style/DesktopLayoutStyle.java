package com.salts_inventory_update.compat.ae2.nativeview.style;

import appeng.client.gui.style.*;
/** Resource-pack-controlled geometry for the optional desktop presentation. */
public final class DesktopLayoutStyle {
    private int width;
    private int height;
    private String resize = "fixed";
    private boolean showTitle;
    private Color backgroundColor;
    private String fitSlots;
    private int minWidth;
    private int minHeight;
    private boolean preserveAeLayout;
    private int inventoryCut;

    public boolean preserveAeLayout() {
        return preserveAeLayout;
    }

    public int inventoryCut() {
        return inventoryCut;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public String resize() {
        return resize;
    }

    public boolean showTitle() {
        return showTitle;
    }

    public String fitSlots() {
        return fitSlots;
    }

    public int minWidth() {
        return minWidth > 0 ? minWidth : width;
    }

    public int minHeight() {
        return minHeight > 0 ? minHeight : height;
    }

    public int fittedHeight(appeng.menu.AEBaseMenu menu, ScreenStyle style) {
        if (preserveAeLayout) {
            return height;
        }
        if (fitSlots == null) {
            return style.getGeneratedBackground() == null ? height : style.getGeneratedBackground().getHeight();
        }
        int result = height;
        var semantic = appeng.menu.SlotSemantics.getOrThrow(fitSlots);
        var position = style.getSlots().get(fitSlots);
        var origin = position.resolve(new net.minecraft.client.renderer.Rect2i(0, 0, width, height));
        var slots = menu.getSlots(semantic);
        for (int i = 0; i < slots.size(); i++) {
            var slot = slots.get(i);
            if (slot.isActive() && (!(slot instanceof appeng.menu.slot.AppEngSlot ae) || ae.isSlotEnabled())) {
                var point = position.getGrid() == null ? origin
                        : position.getGrid().getPosition(origin.getX(), origin.getY(), i);
                result = Math.max(result, point.getY() + 20);
            }
        }
        return result;
    }

    public int backgroundColor() {
        return backgroundColor == null ? 0xFFC6C6C6 : backgroundColor.toARGB();
    }
}
