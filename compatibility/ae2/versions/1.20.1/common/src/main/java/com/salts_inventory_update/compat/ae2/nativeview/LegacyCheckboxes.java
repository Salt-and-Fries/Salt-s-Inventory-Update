package com.salts_inventory_update.compat.ae2.nativeview;

import appeng.client.gui.style.PaletteColor;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.AECheckbox;
import com.salts_inventory_update.api.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.Rect2i;

/** Fork-owned hosted checkbox drawing; ordinary AE2 15 painting remains in its native renderer. */
public final class LegacyCheckboxes {
    private LegacyCheckboxes() { }
    public static void render(AECheckbox widget, ScreenStyle style, GuiGraphics graphics, int mouseX, int mouseY) {
        var font = Minecraft.getInstance().font;
        var palette = widget.active ? PaletteColor.DEFAULT_TEXT_COLOR : PaletteColor.MUTED_TEXT_COLOR;
        boolean hovered = widget.isMouseOver(mouseX, mouseY);
        float opacity = widget.active ? 1 : 0.5f;
        if (style.isDesktopRestyled()) {
            var context = GuiGraphicsExtractor.wrap(graphics);
            NativeDesktopChrome.button(context, new Rect2i(widget.getX(), widget.getY(), 14, 14), hovered, widget.isFocused(), widget.active);
            if (widget.isSelected()) {
                int color = widget.active ? 0xFF404040 : 0xFF777777;
                if (widget.isRadio()) context.fill(widget.getX()+4, widget.getY()+4, widget.getX()+10, widget.getY()+10, color);
                else {
                    context.fill(widget.getX()+3,widget.getY()+6,widget.getX()+6,widget.getY()+10,color);
                    context.fill(widget.getX()+6,widget.getY()+8,widget.getX()+8,widget.getY()+11,color);
                    for (int i=0;i<5;i++) context.fill(widget.getX()+7+i,widget.getY()+8-i,widget.getX()+8+i,widget.getY()+11-i,color);
                }
            }
        } else {
            widget.siu$desktopIcon(widget.active && (hovered || widget.isFocused()))
                    .dest(widget.getX(),widget.getY()).opacity(opacity).blit(graphics);
        }
        var label = widget.displayLabel().copy().withStyle(s -> s.withColor(style.getColor(palette).toARGB()));
        int inset = widget.isRadio() ? 16 : 26;
        var lines = font.split(label, widget.getWidth()-inset);
        int y = widget.getY()+(lines.size() <= 1 ? 4 : 1);
        for (var line : lines) {
            graphics.drawString(font,line,widget.getX()+inset,y,style.getColor(palette).toARGB(),false);
            y += font.lineHeight;
        }
    }
}
