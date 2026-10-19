package com.salts_inventory_update.controlify.qa.mixin;

import com.salts_inventory_update.controlify.qa.QaFeatureProbe;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** NeoForge's deferred callback invokes its item-stack overload rather than the vanilla wrapper. */
@Mixin(GuiGraphicsExtractor.class)
public abstract class FeatureNeoForgeTooltipProbeMixin {
    @Inject(method = "tooltip(Lnet/minecraft/client/gui/Font;Ljava/util/List;IILnet/minecraft/client/gui/screens/inventory/tooltip/ClientTooltipPositioner;Lnet/minecraft/resources/Identifier;Lnet/minecraft/world/item/ItemStack;)V", at = @At("HEAD"))
    private void observeConsumedTooltip(Font font, List<ClientTooltipComponent> lines, int x, int y,
                                        ClientTooltipPositioner positioner, Identifier background,
                                        ItemStack stack, CallbackInfo ci) {
        QaFeatureProbe.tooltip(x, y);
    }
}
