package com.salts_inventory_update.mixin.client;

import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ImageButton.class)
public interface AutomaticRecipeButtonAccessor {
    @Accessor("resourceLocation") ResourceLocation salts_inventory_update$nativeButtonTexture();
}
