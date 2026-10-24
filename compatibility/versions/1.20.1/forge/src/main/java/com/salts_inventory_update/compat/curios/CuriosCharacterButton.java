package com.salts_inventory_update.compat.curios;

import java.util.List;
import com.salts_inventory_update.api.client.desktop.*;
import com.salts_inventory_update.api.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.AbstractContainerMenu;

/** Native Curios launcher at the vanilla player-preview anchor in Character. */
public final class CuriosCharacterButton implements DesktopBuiltinAttachment {
    private static final int X = 27, Y = 9, SIZE = 10;
    private static boolean contains(DesktopWindowContext<?, ?> c, double x, double y) {
        return x >= c.contentX() + X && x < c.contentX() + X + SIZE
            && y >= c.contentY() + Y && y < c.contentY() + Y + SIZE;
    }
    @Override public void render(DesktopRenderContext<AbstractContainerMenu, Void> c) {
        boolean hover = contains(c, c.mouseX(), c.mouseY());
        c.texture(CuriosClientIntegration.CONTROLS, c.contentX() + X, c.contentY() + Y,
            50, hover ? 14 : 0, SIZE, SIZE, 14, 14, 256, 256);
    }
    @Override public boolean mouseClicked(DesktopInputContext<AbstractContainerMenu, Void> c, MouseButtonEvent event, boolean doubleClick) {
        if (!contains(c, event.x(), event.y())) return false;
        if (event.button() == 0) CuriosClientIntegration.toggle(c.windowHandle());
        return true;
    }
    @Override public boolean appendTooltip(DesktopRenderContext<AbstractContainerMenu, Void> c, int x, int y) {
        if (!contains(c, x, y)) return false;
        c.tooltip(Component.translatable("key.curios.open.desc"), x, y); return true;
    }
    @Override public List<DesktopWindowRegion> regions(DesktopWindowContext<AbstractContainerMenu, Void> c) {
        return List.of(DesktopWindowRegion.interactive(c.contentX() - c.windowX() + X,
            c.contentY() - c.windowY() + Y, SIZE, SIZE));
    }
}
