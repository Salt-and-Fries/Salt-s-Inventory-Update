package com.salts_inventory_update.controlify.mixin;

import com.salts_inventory_update.controlify.model.WorkspaceTooltipCoordinates;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** All item, recipe and UI tooltip overloads converge here before Minecraft stores the deferred callback. */
@Mixin(GuiGraphicsExtractor.class)
public abstract class WorkspaceTooltipMixin {
    @ModifyVariable(method = "setTooltipForNextFrameInternal(Lnet/minecraft/client/gui/Font;Ljava/util/List;IILnet/minecraft/client/gui/screens/inventory/tooltip/ClientTooltipPositioner;Lnet/minecraft/resources/Identifier;ZZ)V",
            at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private int saltsControlify$tooltipX(int x) { return WorkspaceTooltipCoordinates.screenX(x); }

    @ModifyVariable(method = "setTooltipForNextFrameInternal(Lnet/minecraft/client/gui/Font;Ljava/util/List;IILnet/minecraft/client/gui/screens/inventory/tooltip/ClientTooltipPositioner;Lnet/minecraft/resources/Identifier;ZZ)V",
            at = @At("HEAD"), argsOnly = true, ordinal = 1)
    private int saltsControlify$tooltipY(int y) { return WorkspaceTooltipCoordinates.screenY(y); }
}
