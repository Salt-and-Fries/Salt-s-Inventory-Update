package com.salts_inventory_update.controlify.render;

import com.salts_inventory_update.api.client.desktop.DesktopWindowBounds;
import com.salts_inventory_update.controlify.model.WindowPlacement;
import com.salts_inventory_update.controlify.model.WindowSize;
import com.salts_inventory_update.controlify.model.WorkspacePreviewGeometry;
import java.util.UUID;
import com.salts_inventory_update.api.client.gui.GuiGraphicsExtractor;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** Inactive surfaces use native item models; the tuck option uses an editable two-frame PNG. */
public final class WorkspacePreviewRenderer {
    public static final ResourceLocation TUCK_TEXTURE = new ResourceLocation(
            "salts_inventory_controlify", "textures/gui/workspace/tuck_away.png");
    private WorkspacePreviewRenderer() { }

    public static void renderWindowIcon(UUID windowId, ItemStack item, WindowPlacement placement,
                                         DesktopWindowBounds bounds, GuiGraphicsExtractor graphics) {
        if (item.isEmpty()) return;
        var square = WorkspacePreviewGeometry.icon(placement, new WindowSize(bounds.width(), bounds.height()));
        graphics.pose().pushMatrix();
        try (var opacity = WorkspaceImmediateOpacity.enter(graphics, (float) placement.opacity())) {

            graphics.pose().translate((float) square.x(), (float) square.y());
            graphics.pose().scale((float) square.size() / 16, (float) square.size() / 16);
            graphics.item(item, 0, 0);
        } finally { graphics.pose().popMatrix(); }
    }

    public static void renderTuck(GuiGraphicsExtractor graphics, WindowPlacement placement) {
        var square = WorkspacePreviewGeometry.tuck(placement);
        int frame = placement.highlight() >= .5 ? 1 : 0;
        graphics.pose().pushMatrix();
        try (var opacity = WorkspaceImmediateOpacity.enter(graphics, (float) placement.opacity())) {

            graphics.pose().translate((float) square.x(), (float) square.y());
            graphics.pose().scale((float) placement.scale(), (float) placement.scale());
            graphics.blit(null, TUCK_TEXTURE, 0, 0, frame * 32, 0,
                    32, 32, 32, 32, 64, 32, 0xFFFFFFFF);
        } finally { graphics.pose().popMatrix(); }
    }
}
