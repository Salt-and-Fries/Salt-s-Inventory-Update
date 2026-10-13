package com.salts_inventory_update.compat.ae2.nativeview;

import com.salts_inventory_update.api.client.gui.GuiGraphicsExtractor;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/** Desktop painting for AE2 15's ordinary Minecraft buttons, retaining native input and text layout. */
public final class LegacyButtons extends AbstractWidget {
    private static final Set<Button> DESKTOP = Collections.newSetFromMap(new WeakHashMap<>());
    private LegacyButtons() { super(0, 0, 0, 0, Component.empty()); }
    @Override protected void renderWidget(GuiGraphics graphics, int x, int y, float delta) { }
    @Override protected void updateWidgetNarration(NarrationElementOutput output) { }

    public static void setDesktop(Button button, boolean hosted) {
        if (hosted) DESKTOP.add(button);
        else DESKTOP.remove(button);
    }

    public static void paint(Button button, GuiGraphicsExtractor graphics) {
        if (!DESKTOP.contains(button) || !button.visible
                || button instanceof appeng.client.gui.widgets.IconButton
                || button instanceof appeng.client.gui.widgets.TabButton) return;
        NativeDesktopChrome.button(graphics, new Rect2i(button.getX(), button.getY(), button.getWidth(), button.getHeight()),
                button.isHovered(), button.isFocused(), button.active);
        renderButtonText(button, graphics, Minecraft.getInstance().font, 2, button.active ? -1 : 0xFFA0A0A0, 0);
    }

    public static void renderButtonText(Button button, GuiGraphicsExtractor graphics, Font font,
            int margin, int color, int verticalOffset) {
        renderScrollingString(new NoShadowGraphics(graphics.unwrap()), font, button.getMessage(),
                button.getX() + margin, button.getY() + verticalOffset,
                button.getX() + button.getWidth() - margin, button.getY() + button.getHeight() + verticalOffset, color);
    }

    /** The native scrolling/clipping algorithm draws through the fork's shadow-free text contract. */
    private static final class NoShadowGraphics extends GuiGraphics {
        private final GuiGraphics delegate;
        NoShadowGraphics(GuiGraphics delegate) {
            super(Minecraft.getInstance(), Minecraft.getInstance().renderBuffers().bufferSource());
            this.delegate = delegate;
        }
        @Override public com.mojang.blaze3d.vertex.PoseStack pose() { return delegate.pose(); }
        @Override public void enableScissor(int x0, int y0, int x1, int y1) { delegate.enableScissor(x0, y0, x1, y1); }
        @Override public void disableScissor() { delegate.disableScissor(); }
        @Override public int drawString(Font font, Component text, int x, int y, int color) {
            return delegate.drawString(font, text, x, y, color, false);
        }
        @Override public int drawString(Font font, Component text, int x, int y, int color, boolean shadow) {
            return delegate.drawString(font, text, x, y, color, false);
        }
        @Override public int drawString(Font font, FormattedCharSequence text, int x, int y, int color) {
            return delegate.drawString(font, text, x, y, color, false);
        }
        @Override public int drawString(Font font, FormattedCharSequence text, int x, int y, int color, boolean shadow) {
            return delegate.drawString(font, text, x, y, color, false);
        }
    }
}
