package com.salts_inventory_update.compat.ae2.nativeview;

import appeng.api.client.AEKeyRendering;
import appeng.api.config.CpuSelectionMode;
import appeng.api.stacks.AmountFormat;
import appeng.client.gui.style.Color;
import appeng.client.gui.widgets.CPUSelectionList;
import appeng.client.gui.widgets.Scrollbar;
import appeng.menu.me.crafting.CraftingStatusMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/** Fork desktop CPU drawing over AE2 15's native menu, keys and formatting. */
public final class LegacyCpuSelection {
    private LegacyCpuSelection() { }

    public static void render(CPUSelectionList widget, GuiGraphics graphics, Rect2i parent,
            Rect2i bounds, CraftingStatusMenu menu, Scrollbar scrollbar, Color textColor,
            int selectedColor, int rowHeight, int buttonWidth, boolean narrow) {
        int x = parent.getX() + bounds.getX(), y = parent.getY() + bounds.getY();
        if (!narrow) NativeDesktopChrome.recessed(graphics,
                new Rect2i(x, y, bounds.getWidth(), bounds.getHeight()));
        x += narrow ? 1 : 4;
        y += narrow ? 0 : 4;
        var font = Minecraft.getInstance().font;
        var entries = menu.cpuList.cpus();
        int start = Mth.clamp(scrollbar.getCurrentScroll(), 0, entries.size());
        int end = Mth.clamp(scrollbar.getCurrentScroll() + widget.visibleRowCount(), 0, entries.size());
        for (var cpu : entries.subList(start, end)) {
            boolean selected = cpu.serial() == menu.getSelectedCpuSerial();
            if (narrow) LegacyCpuArtwork.button(graphics::fill, x, y, bounds.getWidth() - 20, selected);
            else {
                NativeDesktopChrome.button(graphics, new Rect2i(x, y, bounds.getWidth() - 20, rowHeight),
                        false, false, true);
                if (selected) graphics.fill(x + 1, y + 1, x + bounds.getWidth() - 21,
                        y + rowHeight - 1, selectedColor);
            }
            var name = widget.siu$desktopCpuName(cpu);
            text(graphics, Component.literal(font.plainSubstrByWidth(name.getString(), bounds.getWidth() - 26)),
                    textColor.toARGB(), 1, x + 3, y);
            var currentJob = cpu.currentJob();
            if (currentJob != null) {
                LegacyCpuArtwork.paint("craft", graphics::fill, x + 2, y + 9);
                text(graphics, Component.literal(currentJob.what().formatAmount(currentJob.amount(), AmountFormat.SLOT)),
                        textColor.toARGB(), 0.666f, x + 14, y + 13);
                var pose = graphics.pose();
                pose.pushPose();
                try {
                    pose.translate(x + 55, y + 9, 0);
                    pose.scale(0.666f, 0.666f, 1);
                    AEKeyRendering.drawInGui(Minecraft.getInstance(), graphics, 0, 0, currentJob.what());
                } finally { pose.popPose(); }
                int progress = (int) (cpu.progress() * (narrow ? bounds.getWidth() - 21 : buttonWidth - 1));
                pose.pushPose();
                try {
                    pose.translate(1, -1, 0);
                    graphics.fill(x, y + rowHeight - 2, x + progress, y + rowHeight - 1,
                            selected ? 0xFF7da9d2 : selectedColor);
                } finally { pose.popPose(); }
            } else {
                LegacyCpuArtwork.paint("storage", graphics::fill, x + 27, y + 9);
                text(graphics, Component.literal(widget.siu$desktopCpuStorage(cpu)), textColor.toARGB(),
                        0.666f, x + 39, y + 13);
                if (cpu.coProcessors() > 0) {
                    LegacyCpuArtwork.paint("processor", graphics::fill, x + 2, y + 9);
                    text(graphics, Component.literal(String.valueOf(cpu.coProcessors())), textColor.toARGB(),
                            0.666f, x + 14, y + 13);
                }
                if (cpu.mode() == CpuSelectionMode.PLAYER_ONLY)
                    LegacyCpuArtwork.paint("terminal", graphics::fill, x + 55, y + 9);
                else if (cpu.mode() == CpuSelectionMode.MACHINE_ONLY)
                    LegacyCpuArtwork.paint("machine", graphics::fill, x + 55, y + 9);
            }
            y += rowHeight + 1;
        }
    }

    private static void text(GuiGraphics graphics, Component text, int color, float scale, int x, int y) {
        var pose = graphics.pose();
        pose.pushPose();
        try {
            pose.translate(x, y, 0);
            pose.scale(scale, scale, 1);
            graphics.drawString(Minecraft.getInstance().font, text, 0, 0, color, false);
        } finally { pose.popPose(); }
    }
}
