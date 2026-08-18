package com.salts_inventory_update.mixin.client;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.resources.ResourceLocation;

@Mixin(GuiGraphics.class)
public interface GuiGraphicsAccessor {
    @Invoker("innerBlit")
    void salts_inventory_update$invokeInnerBlit(
        ResourceLocation texture,
        int minX,
        int maxX,
        int minY,
        int maxY,
        int z,
        float minU,
        float maxU,
        float minV,
        float maxV
    );

    @Invoker("renderTooltipInternal")
    void salts_inventory_update$invokeRenderTooltipInternal(
        Font font,
        List<ClientTooltipComponent> components,
        int mouseX,
        int mouseY,
        ClientTooltipPositioner positioner
    );
}
