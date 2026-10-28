package com.salts_inventory_update.compat.chipped;

import com.salts_inventory_update.compat.nativewindows.ForeignMenuAccess;
import java.lang.reflect.Field;
import java.util.List;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

/** Optional upstream access only; Salt communication uses its public API. */
public final class ChippedNative {
    static final String MENU="earth.terrarium.chipped.common.menus.WorkbenchMenu";
    private static Field recipeField;
    private ChippedNative() { }
    public static int menuSlot(int inventoryIndex) {
        if(inventoryIndex<0||inventoryIndex>=36)throw new IllegalArgumentException("Invalid material inventory index");
        return inventoryIndex<9?inventoryIndex+27:inventoryIndex-9;
    }
    public static void select(AbstractContainerMenu menu,int index) {
        ForeignMenuAccess.invoke(menu,"selectStack",new Class<?>[]{int.class},menuSlot(index));
    }
    public static void reset(AbstractContainerMenu menu) { ForeignMenuAccess.invoke(menu,"reset"); }
    public static void filter(AbstractContainerMenu menu,String text) {
        ForeignMenuAccess.invoke(menu,"updateResults",new Class<?>[]{String.class},text);
    }
    @SuppressWarnings("unchecked")
    public static List<ItemStack> results(AbstractContainerMenu menu) { return (List<ItemStack>)ForeignMenuAccess.invoke(menu,"results"); }
    public static ItemStack chosen(AbstractContainerMenu menu) { return (ItemStack)ForeignMenuAccess.invoke(menu,"chosenStack"); }
    public static void choose(AbstractContainerMenu menu,ItemStack stack) {
        ForeignMenuAccess.invoke(menu,"setChosenStack",new Class<?>[]{ItemStack.class},stack);
    }
    public static void craft(AbstractContainerMenu menu,ItemStack stack,boolean all) {
        ForeignMenuAccess.invoke(menu,"craft",new Class<?>[]{ItemStack.class,boolean.class},stack,all);
    }
    @SuppressWarnings({"rawtypes","unchecked"})
    public static boolean eligible(AbstractContainerMenu menu,ItemStack stack) {
        if(stack.isEmpty())return false;
        var level=(Level)ForeignMenuAccess.invoke(menu,"level");
        var key=net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("chipped","workbench");
        var type=(RecipeType)net.minecraft.core.registries.BuiltInRegistries.RECIPE_TYPE.get(key);
        return type!=null&&level.getRecipeManager().getRecipeFor(type,
            net.minecraft.world.item.crafting.CraftingInput.of(1,1,List.of(stack)),level).isPresent();
    }
}
