package com.salts_inventory_update.controlify.qa;

import com.salts_inventory_update.api.client.desktop.DesktopWindowBounds;
import com.salts_inventory_update.controlify.model.WorkspaceLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;

/** Disposable visual experiments. No status-effect design is enabled in production. */
public final class QaStatusEffectLayouts {
    public static int mode;
    private static QaWorkspaceOwner owner;
    public static long frames;
    public static boolean fits = true;
    public static void install(QaWorkspaceOwner value) { owner = value; }
    public static boolean active() { return mode > 0 && owner != null; }

    @SuppressWarnings("unchecked")
    public static void render(GuiGraphicsExtractor graphics) {
        if (!active()) return;
        var mc = Minecraft.getInstance();
        if (mc.player == null || mc.gui.screen() == null) return;
        WorkspaceLayout<UUID> layout;
        try {
            var field = mc.gui.screen().getClass().getDeclaredField("placements");
            field.setAccessible(true);
            layout = (WorkspaceLayout<UUID>) field.get(mc.gui.screen());
        } catch (ReflectiveOperationException failure) { throw new IllegalStateException(failure); }
        if (layout == null) return;
        var pane = layout.windows().get(owner.model().character());
        var bounds = owner.desktop().presentationBounds(owner.model().character());
        if (pane == null) return;
        List<MobEffectInstance> effects = new ArrayList<>(mc.player.getActiveEffects());
        effects.sort(null);
        int available = graphics.guiWidth() - 12;
        int cells = Math.max(1, Math.min(effects.size(), available / 30));
        int rowHeight = 34;
        int width = cells * 30 + 8, height = ((effects.size() + cells - 1) / cells) * rowHeight + 8;
        int x = (graphics.guiWidth() - width) / 2, y = 8;
        graphics.nextStratum();
        if (mode == 2) {
            x = Math.max(6, Math.min(graphics.guiWidth() - width - 6, (int) pane.x()));
            y = Math.min(graphics.guiHeight() - height - 30,
                    (int) (pane.y() + bounds.height() * pane.scale()) + 6);
        }
        if (mode >= 3) {
            int badgeX = (int) (pane.x() + bounds.width() * pane.scale()) - 73;
            int badgeY = (int) pane.y() + 5;
            graphics.fill(badgeX, badgeY, badgeX + 65, badgeY + 16, 0xFF4A4A4A);
            graphics.text(mc.font, "Effects " + effects.size(), badgeX + 5, badgeY + 4, 0xFFFFFFFF, true);
            if (mode == 3) { frames++; return; }
            width = Math.min(170, graphics.guiWidth() - 12);
            height = Math.min(20 + effects.size() * 28, graphics.guiHeight() - 48);
            x = Math.max(6, Math.min(graphics.guiWidth() - width - 6,
                    (int) (pane.x() + bounds.width() * pane.scale()) + 7));
            y = Math.max(6, Math.min(graphics.guiHeight() - height - 30, (int) pane.y()));
        }
        fits &= x >= 0 && y >= 0 && x + width <= graphics.guiWidth()
                && y + height <= graphics.guiHeight() - 24;
        owner.desktop().renderWindowFrame(graphics, new DesktopWindowBounds(x, y, width, height),
                mode >= 3 ? Component.literal("Effects") : Component.empty());
        for (int i = 0; i < effects.size(); i++) {
            var effect = effects.get(i);
            int ix = mode >= 3 ? x + 7 : x + 10 + (i % cells) * 30;
            int iy = mode >= 3 ? y + 22 + i * 28 : y + 5 + (i / cells) * rowHeight;
            if (iy + 25 > y + height) break;
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, Hud.getMobEffectSprite(effect.getEffect()), ix, iy, 18, 18);
            String duration = duration(effect);
            if (mode >= 3) {
                String name = Component.translatable(effect.getDescriptionId()).getString();
                if (effect.getAmplifier() > 0) name += " " + (effect.getAmplifier() + 1);
                graphics.text(mc.font, name, ix + 25, iy + 2, 0xFFFFFFFF, true);
                graphics.text(mc.font, duration, ix + 25, iy + 13, 0xFFDDDDDD, true);
            } else graphics.text(mc.font, duration, ix + 9 - mc.font.width(duration) / 2, iy + 20, 0xFFFFFFFF, true);
        }
        frames++;
    }

    private static String duration(MobEffectInstance effect) {
        if (effect.isInfiniteDuration()) return "∞";
        int seconds = Math.max(0, effect.getDuration() / 20);
        return seconds / 60 + ":" + String.format(java.util.Locale.ROOT, "%02d", seconds % 60);
    }
}
