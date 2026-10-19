package com.salts_inventory_update.controlify.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.salts_inventory_update.controlify.SaltsControlifyClient;
import com.salts_inventory_update.controlify.WorkspaceActionGuide;
import dev.isxander.controlify.api.contextual.GuideInstance;
import dev.isxander.controlify.gui.guide.InGameButtonGuide;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Move only native top-aligned gameplay guides; native visibility and style gates remain intact. */
@Mixin(value = InGameButtonGuide.class, remap = false)
public abstract class TuckedInGameGuideMixin {
    @WrapOperation(method = "extractRenderState", at = @At(value = "INVOKE",
            target = "Ldev/isxander/controlify/api/contextual/GuideInstance;extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;ZZI)V"), remap = false)
    private void salts$clearTuckedWorkspace(GuideInstance<?> guide, GuiGraphicsExtractor graphics,
            boolean bottomAligned, boolean textContrast, int guiScale, Operation<Void> original) {
        int inset = bottomAligned ? 0 : SaltsControlifyClient.tuckedGuideInset(graphics.guiWidth(), graphics.guiHeight());
        if (inset <= 0) {
            if (!WorkspaceActionGuide.renderTucked(guide, graphics, bottomAligned, textContrast, guiScale))
                original.call(guide, graphics, bottomAligned, textContrast, guiScale);
            return;
        }
        graphics.pose().pushMatrix();
        try {
            graphics.pose().translate(0, inset);
            if (!WorkspaceActionGuide.renderTucked(guide, graphics, bottomAligned, textContrast, guiScale))
                original.call(guide, graphics, bottomAligned, textContrast, guiScale);
        } finally {
            graphics.pose().popMatrix();
        }
    }
}
