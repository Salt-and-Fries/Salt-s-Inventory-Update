package com.salts_inventory_update.mixin.client;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.Screen;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
/** Render-only identity swap; does not invoke screen lifecycle callbacks. */
@Mixin(Gui.class)
public interface NativeGuiScreenAccessor {
    @Accessor("screen") void salts_inventory_update$nativeScreen(@Nullable Screen screen);
}
