package com.salts_inventory_update.mixin.client;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.WidgetSprites;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(ImageButton.class)
public interface AutomaticRecipeButtonAccessor {
    @Accessor("sprites") WidgetSprites salts_inventory_update$nativeButtonSprites();
}
