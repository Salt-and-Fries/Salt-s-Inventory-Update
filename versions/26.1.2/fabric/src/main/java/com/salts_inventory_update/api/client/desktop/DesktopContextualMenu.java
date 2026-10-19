package com.salts_inventory_update.api.client.desktop;

import java.util.List;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.display.RecipeDisplayId;

/** Read-only native content for an alternate recipe/tab presentation. No presentation owns a menu. */
public record DesktopContextualMenu(Kind kind, List<Category> categories, List<Recipe> recipes, List<Tab> tabs, Station station) {
    public static final DesktopContextualMenu NONE = new DesktopContextualMenu(Kind.NONE, List.of(), List.of(), List.of());
    public DesktopContextualMenu(Kind kind, List<Category> categories, List<Recipe> recipes, List<Tab> tabs) {
        this(kind, categories, recipes, tabs, Station.NONE);
    }
    public DesktopContextualMenu {
        categories = List.copyOf(categories); recipes = List.copyOf(recipes); tabs = List.copyOf(tabs);
        station = station == null ? Station.NONE : station;
    }
    public enum Kind { NONE, CRAFTING, COOKING, CREATIVE }
    public record Station(int gridWidth, int gridHeight, ItemStack input, ItemStack fuel, ItemStack output, float burnProgress, float litProgress) {
        public static final Station NONE = new Station(0, 0, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, 0, 0);
        public Station { input = input.copy(); fuel = fuel.copy(); output = output.copy(); }
        @Override public ItemStack input() { return input.copy(); }
        @Override public ItemStack fuel() { return fuel.copy(); }
        @Override public ItemStack output() { return output.copy(); }
    }
    public record Category(Identifier id, String title, ItemStack icon) {
        public Category { icon = icon.copy(); }
        @Override public ItemStack icon() { return icon.copy(); }
    }
    public record Ingredient(List<ItemStack> alternatives, int required, int available) {
        public Ingredient { alternatives = alternatives.stream().map(ItemStack::copy).toList(); }
        @Override public List<ItemStack> alternatives() { return alternatives.stream().map(ItemStack::copy).toList(); }
    }
    public record Recipe(RecipeDisplayId id, Identifier category, ItemStack output, int gridWidth, int gridHeight,
                         List<Ingredient> grid, List<Ingredient> requirements, boolean craftable) {
        public Recipe { output = output.copy(); grid = List.copyOf(grid); requirements = List.copyOf(requirements); }
        @Override public ItemStack output() { return output.copy(); }
    }
    public record Tab(Identifier id, String title, ItemStack icon, boolean selected) {
        public Tab { icon = icon.copy(); }
        @Override public ItemStack icon() { return icon.copy(); }
    }
}
