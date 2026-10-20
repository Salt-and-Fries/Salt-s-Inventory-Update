package com.salts_inventory_update.controlify;

import com.salts_inventory_update.api.client.desktop.DesktopWindowBounds;
import com.salts_inventory_update.controlify.model.StatusEffectTrayLayout;
import com.salts_inventory_update.controlify.model.WorkspaceLayout;
import java.util.ArrayList;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.effect.MobEffectInstance;

/** Controller-only status information, separate from Character's measured window bounds. */
final class WorkspaceEffectsTray {
    static final int COUNTDOWN_COLOR = 0xFF404040;
    static final boolean COUNTDOWN_SHADOW = false;

    static void render(SaltsControlifyClient owner, WorkspaceLayout<UUID> placements,
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, int contentHeight) {
        var mc = Minecraft.getInstance();
        if (mc.player == null || placements == null || owner.radialActive() || owner.tuckedAway()) return;
        UUID character = owner.model().character();
        if (!character.equals(owner.model().focused()) && !character.equals(owner.model().selected())) return;
        var pane = placements.windows().get(character);
        if (pane == null || pane.opacity() < .01) return;
        var effects = new ArrayList<>(mc.player.getActiveEffects());
        if (effects.isEmpty()) return;
        effects.sort(null);
        var bounds = owner.desktop().presentationBounds(character);
        int cellWidth = 30;
        for (var effect : effects) cellWidth = Math.max(cellWidth, mc.font.width(duration(effect)) + 8);
        var tray = StatusEffectTrayLayout.place(graphics.guiWidth(), contentHeight,
                pane.x(), pane.y(), bounds.width() * pane.scale(), bounds.height() * pane.scale(),
                effects.size(), cellWidth);
        if (tray == null) return;
        graphics.nextStratum();
        owner.desktop().renderWindowFrame(graphics,
                new DesktopWindowBounds(tray.x(), tray.y(), tray.width(), tray.height()), Component.empty());
        boolean overflow = effects.size() > tray.capacity();
        int visible = Math.min(effects.size(), tray.capacity() - (overflow ? 1 : 0));
        for (int i = 0; i < visible; i++) {
            var effect = effects.get(i);
            int x = tray.cellX(i), y = tray.cellY(i), center = x + tray.cellWidth() / 2;
            graphics.blitSprite(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, net.minecraft.client.gui.Gui.getMobEffectSprite(effect.getEffect()), center - 9, y, 18, 18);
            String remaining = duration(effect);
            graphics.text(mc.font, remaining, center - mc.font.width(remaining) / 2,
                    y + 20, COUNTDOWN_COLOR, COUNTDOWN_SHADOW);
            if (contains(mouseX, mouseY, x, y, tray.cellWidth(), StatusEffectTrayLayout.ROW_HEIGHT))
                graphics.setTooltipForNextFrame(mc.font.split(description(effect), Math.min(220, graphics.guiWidth() - 16)), mouseX, mouseY);
        }
        if (overflow) {
            int x = tray.cellX(visible), y = tray.cellY(visible);
            String more = "+" + (effects.size() - visible);
            graphics.text(mc.font, more, x + (tray.cellWidth() - mc.font.width(more)) / 2,
                    y + 12, COUNTDOWN_COLOR, COUNTDOWN_SHADOW);
            if (contains(mouseX, mouseY, x, y, tray.cellWidth(), StatusEffectTrayLayout.ROW_HEIGHT)) {
                var lines = new ArrayList<FormattedCharSequence>();
                for (int i = visible; i < effects.size(); i++)
                    lines.addAll(mc.font.split(description(effects.get(i)), Math.min(220, graphics.guiWidth() - 16)));
                graphics.setTooltipForNextFrame(lines, mouseX, mouseY);
            }
        }
    }

    private static boolean contains(int mx, int my, int x, int y, int width, int height) {
        return mx >= x && my >= y && mx < x + width && my < y + height;
    }

    private static Component description(MobEffectInstance effect) {
        var name = Component.translatable(effect.getDescriptionId());
        if (effect.getAmplifier() > 0)
            name = name.append(" ").append(Component.translatable("enchantment.level." + (effect.getAmplifier() + 1)));
        return name.append(" - " + duration(effect));
    }

    private static String duration(MobEffectInstance effect) {
        if (effect.isInfiniteDuration()) return "\u221e";
        int seconds = Math.max(0, effect.getDuration() / 20);
        return seconds / 60 + ":" + String.format(java.util.Locale.ROOT, "%02d", seconds % 60);
    }
}
