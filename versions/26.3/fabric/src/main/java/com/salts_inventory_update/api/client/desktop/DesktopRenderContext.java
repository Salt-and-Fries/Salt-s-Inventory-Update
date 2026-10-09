package com.salts_inventory_update.api.client.desktop;

import java.util.List;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public interface DesktopRenderContext<T extends AbstractContainerMenu, S> extends DesktopWindowContext<T, S> {
    int mouseX();

    int mouseY();

    void fill(int x1, int y1, int x2, int y2, int color);

    void text(String text, int x, int y, int color, boolean shadow);

    void scaledText(String text, int x, int y, int color, boolean shadow, float scale);

    void text(Component text, int x, int y, int color, boolean shadow);

    void sprite(Identifier sprite, int x, int y, int width, int height);

    void sprite(Identifier sprite, int sourceWidth, int sourceHeight, int sourceX, int sourceY, int x, int y, int width, int height);

    void texture(Identifier texture, int x, int y, int sourceX, int sourceY, int width, int height, int sourceWidth, int sourceHeight, int textureWidth, int textureHeight);

    void windowNineSlice(Identifier texture, int x, int y, int width, int height);

    void onePixelNineSlice(Identifier texture, int x, int y, int width, int height);

    void item(ItemStack stack, int x, int y);

    void item(ItemStack stack, int x, int y, int seed);

    void virtualItem(ItemStack stack, long count, int x, int y);

    void slot(int menuSlotId, int x, int y);

    default void renderSlot(int menuSlotId, int x, int y) {
        this.slot(menuSlotId, x, y);
    }

    void slot(Slot slot, int x, int y);

    void texturelessSlot(int menuSlotId, int x, int y);

    void slotBackground(int x, int y);

    void slotHighlight(int x, int y);

    void entityPreview(LivingEntity entity, int x0, int y0, int x1, int y1, int scale, float mouseScale);

    void tooltip(ItemStack stack, int mouseX, int mouseY);

    void tooltip(Component text, int mouseX, int mouseY);

    void tooltip(List<Component> lines, int mouseX, int mouseY);
}
