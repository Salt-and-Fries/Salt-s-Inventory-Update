package com.salts_inventory_update.compat.sophisticated.client;

import com.salts_inventory_update.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.p3pp3rf1y.sophisticatedstorage.client.gui.LimitedBarrelScreen;
import net.p3pp3rf1y.sophisticatedstorage.common.gui.StorageContainerMenu;

/** Isolates Sophisticated Storage linkage so Backpacks can run without Storage installed. */
final class SophisticatedLimitedBarrelRendering {
    private SophisticatedLimitedBarrelRendering() {
    }

    static void drawSlotBackground(
        AbstractContainerScreen<?> screen,
        GuiGraphicsExtractor graphics,
        int bodyX,
        int bodyY,
        int slotCount
    ) {
        LimitedBarrelScreen.drawSlotBg(screen, graphics.unwrap(), bodyX, bodyY, slotCount);
    }

    static void renderCapacityBars(
        AbstractContainerScreen<?> screen,
        AbstractContainerMenu menu,
        GuiGraphicsExtractor graphics,
        int bodyX,
        int bodyY
    ) {
        if (!(menu instanceof StorageContainerMenu storageMenu)) {
            return;
        }
        graphics.pose().pushMatrix();
        graphics.pose().translate(bodyX, bodyY);
        try {
            LimitedBarrelScreen.renderBars(
                screen.getMinecraft().font,
                ((com.salts_inventory_update.compat.sophisticated.mixin.AbstractContainerScreenAccessor) (Object) screen)
                    .salts_inventory_update$imageWidth(),
                storageMenu,
                graphics.unwrap(),
                storageMenu::getSlotFillPercentage
            );
        } finally {
            graphics.pose().popMatrix();
        }
    }
}
