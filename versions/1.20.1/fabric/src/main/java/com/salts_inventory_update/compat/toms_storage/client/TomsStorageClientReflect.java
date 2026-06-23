package com.salts_inventory_update.compat.toms_storage.client;

import java.lang.reflect.Constructor;

import org.jspecify.annotations.Nullable;

import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.world.inventory.AbstractContainerMenu;

import com.salts_inventory_update.debug.DesktopDebug;

final class TomsStorageClientReflect {
    private TomsStorageClientReflect() {
    }

    static @Nullable RecipeBookComponent createCraftingTerminalRecipeBook(AbstractContainerMenu menu) {
        try {
            Class<?> menuClass = Class.forName("com.tom.storagemod.menu.CraftingTerminalMenu");
            if (!menuClass.isInstance(menu)) {
                return null;
            }

            Class<?> widgetClass = Class.forName("com.tom.storagemod.screen.widget.CraftingTerminalRecipeBookWidget");
            Constructor<?> constructor = widgetClass.getConstructor(menuClass);
            Object widget = constructor.newInstance(menu);
            return widget instanceof RecipeBookComponent recipeBook ? recipeBook : null;
        } catch (ReflectiveOperationException exception) {
            DesktopDebug.warn("Tom's Storage compat failed recipe book create menu={} reason={}", menu.getClass().getName(), exception.toString());
            return null;
        }
    }
}
