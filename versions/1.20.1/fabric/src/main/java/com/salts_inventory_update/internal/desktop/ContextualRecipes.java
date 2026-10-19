package com.salts_inventory_update.internal.desktop;

import com.salts_inventory_update.api.client.desktop.DesktopContextualMenu;
import java.util.*;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;

/** Native pre-display-recipe adapter. IDs stay server recipe identifiers on 1.20.1. */
public final class ContextualRecipes {
    private ContextualRecipes() { }
    public static boolean isCrafting(AbstractContainerMenu menu) { return menu instanceof CraftingMenu || menu instanceof InventoryMenu; }
    public static int width(AbstractContainerMenu menu) { return menu instanceof CraftingMenu ? 3 : menu instanceof InventoryMenu ? 2 : 1; }
    public static int height(AbstractContainerMenu menu) { return width(menu); }
    public static List<Slot> inputs(AbstractContainerMenu menu) {
        if (!isCrafting(menu)) return menu instanceof AbstractFurnaceMenu ? List.of(menu.getSlot(0)) : List.of();
        return List.copyOf(menu.slots.subList(1, 1 + width(menu) * height(menu)));
    }
    public static Slot result(AbstractContainerMenu menu) { return menu.getSlot(menu instanceof AbstractFurnaceMenu ? 2 : 0); }
    public static boolean fuel(ItemStack stack) { return net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity.isFuel(stack); }
    public static boolean cookingType(AbstractContainerMenu menu, Recipe<?> recipe) {
        return recipe.getType() == (menu instanceof SmokerMenu ? RecipeType.SMOKING : menu instanceof BlastFurnaceMenu ? RecipeType.BLASTING : RecipeType.SMELTING);
    }
    public static ResourceLocation category(Recipe<?> recipe) {
        String group;
        if (recipe instanceof CraftingRecipe crafting) group = switch (crafting.category()) {
            case BUILDING -> "crafting_building_blocks"; case REDSTONE -> "crafting_redstone";
            case EQUIPMENT -> "crafting_equipment"; case MISC -> "crafting_misc";
        };
        else if (recipe instanceof AbstractCookingRecipe cooking) group = switch (cooking.category()) {
            case FOOD -> "cooking_food"; case BLOCKS -> "cooking_blocks"; case MISC -> "cooking_misc";
        };
        else group = "crafting_misc";
        return new ResourceLocation("minecraft", group);
    }
    private static DesktopContextualMenu.Category category(String id, String title, Item item) {
        return new DesktopContextualMenu.Category(new ResourceLocation("minecraft", id), title, new ItemStack(item));
    }
    /** Slot order matches the native centered shaped-recipe placement. */
    public static List<Ingredient> grid(AbstractContainerMenu menu, Recipe<?> recipe) {
        int width = width(menu), height = height(menu);
        if (!recipe.canCraftInDimensions(width, height)) return List.of();
        List<Ingredient> grid = new ArrayList<>(Collections.nCopies(width * height, Ingredient.EMPTY));
        if (recipe instanceof ShapedRecipe shaped) {
            int offsetX = shaped.getWidth() < width / 2.0 ? (int)Math.floor(width / 2.0 - shaped.getWidth() / 2.0) : 0;
            int offsetY = shaped.getHeight() < height / 2.0 ? (int)Math.floor(height / 2.0 - shaped.getHeight() / 2.0) : 0;
            for (int y = 0; y < shaped.getHeight(); y++) for (int x = 0; x < shaped.getWidth(); x++)
                grid.set((y + offsetY) * width + x + offsetX, shaped.getIngredients().get(y * shaped.getWidth() + x));
        } else {
            int index = 0;
            for (Ingredient ingredient : recipe.getIngredients()) if (!ingredient.isEmpty()) {
                if (index >= grid.size()) return List.of();
                grid.set(index++, ingredient);
            }
        }
        return List.copyOf(grid);
    }
    private static DesktopContextualMenu.Ingredient ingredient(Ingredient ingredient, int required, List<ItemStack> available) {
        List<ItemStack> variants = Arrays.stream(ingredient.getItems()).map(ItemStack::copy).toList();
        int count = available.stream().filter(ingredient::test).mapToInt(ItemStack::getCount).sum();
        return new DesktopContextualMenu.Ingredient(variants, required, count);
    }
    public static DesktopContextualMenu snapshot(Player player, net.minecraft.stats.RecipeBook book, AbstractContainerMenu menu, List<Slot> sourceSlots) {
        if (menu == null || !(isCrafting(menu) || menu instanceof AbstractFurnaceMenu)) return DesktopContextualMenu.NONE;
        boolean cooking = menu instanceof AbstractFurnaceMenu;
        List<DesktopContextualMenu.Category> categories = cooking ? List.of(
            category("cooking_all", "All recipes", menu instanceof SmokerMenu ? Items.SMOKER : menu instanceof BlastFurnaceMenu ? Items.BLAST_FURNACE : Items.FURNACE),
            category("cooking_food", "Food", Items.PORKCHOP), category("cooking_blocks", "Blocks", Items.STONE), category("cooking_misc", "Miscellaneous", Items.IRON_INGOT)) : List.of(
            category("crafting_all", "All recipes", Items.CRAFTING_TABLE), category("crafting_building_blocks", "Building blocks", Items.BRICKS),
            category("crafting_redstone", "Redstone", Items.REDSTONE), category("crafting_equipment", "Equipment", Items.IRON_PICKAXE), category("crafting_misc", "Miscellaneous", Items.LAVA_BUCKET));
        List<ItemStack> available = new ArrayList<>(sourceSlots.stream().map(Slot::getItem).toList());
        // Include staged inputs after deduplicating native aliases: hover staging must not make its recipe disappear.
        for (Slot slot : inputs(menu)) if (sourceSlots.stream().noneMatch(source -> source.container == slot.container && source.getContainerSlot() == slot.getContainerSlot()) && slot.hasItem()) available.add(slot.getItem());
        StackedContents contents = new StackedContents();
        for (ItemStack stack : available) contents.accountStack(stack);
        List<ItemStack> fuels = cooking ? net.minecraft.core.registries.BuiltInRegistries.ITEM.stream()
            .map(ItemStack::new).filter(ContextualRecipes::fuel).toList() : List.of();
        List<DesktopContextualMenu.Recipe> recipes = new ArrayList<>();
        for (Recipe<?> recipe : player.level().getRecipeManager().getRecipes()) {
            if (!book.contains(recipe) || recipe.isSpecial() || (cooking ? !(recipe instanceof AbstractCookingRecipe) || !cookingType(menu, recipe) : !(recipe instanceof CraftingRecipe))) continue;
            List<Ingredient> grid = grid(menu, recipe);
            if (grid.isEmpty()) continue;
            ItemStack output = recipe.getResultItem(player.level().registryAccess());
            if (output.isEmpty()) continue;
            List<DesktopContextualMenu.Ingredient> requirements = new ArrayList<>();
            for (Ingredient ingredient : recipe.getIngredients()) if (!ingredient.isEmpty()) {
                int found = -1;
                for (int i=0;i<requirements.size();i++) {
                    var old = requirements.get(i).alternatives();
                    ItemStack[] next = ingredient.getItems();
                    if (old.size()==next.length && old.stream().allMatch(item -> Arrays.stream(next).anyMatch(other -> ItemStack.isSameItemSameTags(item, other)))) { found=i; break; }
                }
                if (found < 0) requirements.add(ingredient(ingredient, 1, available));
                else { var old=requirements.get(found); requirements.set(found, new DesktopContextualMenu.Ingredient(old.alternatives(), old.required()+1, old.available())); }
            }
            boolean craftable = contents.canCraft(recipe, null);
            if (cooking) {
                AbstractFurnaceMenu furnace=(AbstractFurnaceMenu)menu;
                ItemStack currentFuel=furnace.getSlot(1).getItem();
                var allocation=ContextualCookingAvailability.allocate(available, recipe.getIngredients().get(0)::test, ContextualRecipes::fuel, !furnace.isLit() && currentFuel.isEmpty());
                if (!furnace.isLit()) requirements.add(new DesktopContextualMenu.Ingredient(fuels, 1, allocation.fuelAvailable()+currentFuel.getCount()));
                ItemStack present=furnace.getSlot(2).getItem();
                craftable &= allocation.ready() && (currentFuel.isEmpty() || fuel(currentFuel)) && (present.isEmpty() || ItemStack.isSameItemSameTags(output,present)) && output.getCount()+present.getCount()<=output.getMaxStackSize();
            }
            recipes.add(new DesktopContextualMenu.Recipe(recipe.getId(), category(recipe), output, width(menu), height(menu), grid.stream().map(ingredient -> ingredient(ingredient, ingredient.isEmpty()?0:1, available)).toList(), requirements, craftable));
        }
        recipes.sort(Comparator.comparing((DesktopContextualMenu.Recipe recipe)->recipe.output().getHoverName().getString()).thenComparing(recipe->recipe.id().toString()));
        AbstractFurnaceMenu furnace=menu instanceof AbstractFurnaceMenu f?f:null;
        return new DesktopContextualMenu(cooking?DesktopContextualMenu.Kind.COOKING:DesktopContextualMenu.Kind.CRAFTING,categories,recipes,List.of(),new DesktopContextualMenu.Station(width(menu),height(menu),furnace==null?ItemStack.EMPTY:furnace.getSlot(0).getItem(),furnace==null?ItemStack.EMPTY:furnace.getSlot(1).getItem(),result(menu).getItem(),furnace==null?0:furnace.getBurnProgress(),furnace==null?0:furnace.getLitProgress()));
    }
}
