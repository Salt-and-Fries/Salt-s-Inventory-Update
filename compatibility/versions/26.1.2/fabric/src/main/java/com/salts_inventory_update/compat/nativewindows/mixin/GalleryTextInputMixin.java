package com.salts_inventory_update.compat.nativewindows.mixin;

import com.salts_inventory_update.api.client.desktop.DesktopNativeTextInput;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

/** Gallery refreshes its painting list in the screen's input handlers, after
 * editing the field. Forward to those handlers instead of only the EditBox. */
@Pseudo
@Mixin(targets="com.teamabnormals.gallery.client.gui.screens.inventory.PaintingSelectorScreen",remap=false)
public abstract class GalleryTextInputMixin implements DesktopNativeTextInput {
    @Override public boolean desktopNativeTextInputFocused() {
        return ((Screen)(Object)this).getFocused() instanceof EditBox box&&box.visible&&box.isFocused();
    }
}
