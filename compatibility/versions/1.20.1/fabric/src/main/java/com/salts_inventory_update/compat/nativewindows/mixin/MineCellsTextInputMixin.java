package com.salts_inventory_update.compat.nativewindows.mixin;

import com.salts_inventory_update.api.client.desktop.DesktopNativeTextInput;
import com.salts_inventory_update.compat.minecells.MineCellsClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

/** Owo nests this field below the root rather than Screen.getFocused(). */
@Pseudo
@Mixin(targets="com.github.mim1q.minecells.screen.cellcrafter.CellCrafterScreen",remap=false)
public abstract class MineCellsTextInputMixin implements DesktopNativeTextInput {
    @Override public boolean desktopNativeTextInputFocused(){return MineCellsClient.textFocused(this);}
}
