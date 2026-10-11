package com.salts_inventory_update.api.client.desktop;

import java.util.List;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public interface DesktopRenderContext<T, S> extends DesktopWindowContext<T, S>, DesktopNativeRenderContext {

    /** Native graphics for existing Minecraft/mod renderers. Valid only during this callback. */
    net.minecraft.client.gui.GuiGraphics nativeGraphics();

    /** Nested scissor scope using the current native render pose; always restores the parent clip. */
    default void withClip(int x, int y, int width, int height, Runnable draw) {
        if (width < 0 || height < 0) throw new IllegalArgumentException("Negative clip dimensions");
        rawGraphics().enableScissor(x, y, Math.addExact(x, width), Math.addExact(y, height));
        try { draw.run(); } finally { rawGraphics().disableScissor(); }
    }

    /** Scoped translation/scale for nested layouts. Hit testing must use corresponding coordinates. */
    default void withTransform(float x, float y, float scale, Runnable draw) {
        if (!Float.isFinite(x) || !Float.isFinite(y) || !Float.isFinite(scale) || scale <= 0) {
            throw new IllegalArgumentException("Transform must be finite with positive scale");
        }
        var pose = rawGraphics().pose();
        pose.pushMatrix();
        try {
            pose.translate(x, y);
            pose.scale(scale, scale);
            draw.run();
        } finally { pose.popMatrix(); }
    }

    int mouseX();

    int mouseY();

    void fill(int x1, int y1, int x2, int y2, int color);

    void text(String text, int x, int y, int color, boolean shadow);

    void scaledText(String text, int x, int y, int color, boolean shadow, float scale);

    void text(Component text, int x, int y, int color, boolean shadow);

    void sprite(ResourceLocation sprite, int x, int y, int width, int height);

    void sprite(ResourceLocation sprite, int sourceWidth, int sourceHeight, int sourceX, int sourceY, int x, int y, int width, int height);

    void texture(ResourceLocation texture, int x, int y, int sourceX, int sourceY, int width, int height, int sourceWidth, int sourceHeight, int textureWidth, int textureHeight);

    void windowNineSlice(ResourceLocation texture, int x, int y, int width, int height);

    void onePixelNineSlice(ResourceLocation texture, int x, int y, int width, int height);

    void item(ItemStack stack, int x, int y);

    void item(ItemStack stack, int x, int y, int seed);

    void virtualItem(ItemStack stack, long count, int x, int y);

    void slot(int menuSlotId, int x, int y);

    void slot(Slot slot, int x, int y);

    void texturelessSlot(int menuSlotId, int x, int y);

    void slotBackground(int x, int y);

    void slotHighlight(int x, int y);

    void entityPreview(LivingEntity entity, int x0, int y0, int x1, int y1, int scale, float mouseScale);

    void tooltip(ItemStack stack, int mouseX, int mouseY);

    void tooltip(Component text, int mouseX, int mouseY);

    void tooltip(List<Component> lines, int mouseX, int mouseY);
}
