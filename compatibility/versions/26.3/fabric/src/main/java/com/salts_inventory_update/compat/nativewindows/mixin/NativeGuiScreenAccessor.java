package com.salts_inventory_update.compat.nativewindows.mixin;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(Gui.class) public interface NativeGuiScreenAccessor {
    @Accessor("screen") void salts_inventory_compat$nativeScreen(Screen screen);
}
