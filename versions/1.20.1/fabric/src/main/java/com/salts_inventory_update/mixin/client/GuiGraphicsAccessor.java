package com.salts_inventory_update.mixin.client;

import java.util.List;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;

@Mixin(GuiGraphics.class)
public interface GuiGraphicsAccessor {
    @Invoker("renderTooltipInternal")
    void salts_inventory_update$invokeRenderTooltipInternal(Font font, List<ClientTooltipComponent> components,
        int mouseX, int mouseY, ClientTooltipPositioner positioner);
}
