package com.salts_inventory_update.compat.nativewindows.mixin;

import com.salts_inventory_update.api.client.desktop.DesktopNativeTextInput;
import com.salts_inventory_update.compat.nativewindows.ForeignMenuAccess;
import net.minecraft.client.gui.components.EditBox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

/** DoAPI owns its search field inside a recipe component rather than Screen.getFocused(). */
@Pseudo
@Mixin(targets="de.cristelknight.doapi.client.recipebook.screen.AbstractRecipeBookGUIScreen",remap=false)
public abstract class DoApiTextInputMixin implements DesktopNativeTextInput {
    @Override public boolean desktopNativeTextInputFocused() {
        try {
            Object book=getClass().getField("recipeBook").get(this);
            if(!Boolean.TRUE.equals(ForeignMenuAccess.invoke(book,"isOpen")))return false;
            for(Class<?> type=book.getClass();type!=null;type=type.getSuperclass())try {
                var field=type.getDeclaredField("searchField");field.setAccessible(true);
                return field.get(book) instanceof EditBox box&&box.isFocused();
            } catch(NoSuchFieldException absent) { }
            return false;
        }catch(ReflectiveOperationException failure){throw new IllegalStateException("Unsupported DoAPI text focus",failure);}
    }
}
