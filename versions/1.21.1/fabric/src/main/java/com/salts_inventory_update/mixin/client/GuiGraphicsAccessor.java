package com.salts_inventory_update.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.client.gui.GuiGraphics;
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
}
