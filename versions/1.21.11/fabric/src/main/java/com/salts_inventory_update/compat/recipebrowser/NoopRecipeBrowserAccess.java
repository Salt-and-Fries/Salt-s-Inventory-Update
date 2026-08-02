package com.salts_inventory_update.compat.recipebrowser;

import java.util.List;

import com.salts_inventory_update.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

final class NoopRecipeBrowserAccess implements RecipeBrowserAccess {
    @Override
    public RecipeBrowserSource source() {
        return RecipeBrowserSource.JEI;
    }

    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public String filterText() {
        return "";
    }

    @Override
    public void setFilterText(String text) {
    }

    @Override
    public List<RecipeBrowserTab> tabs() {
        return List.of();
    }

    @Override
    public List<RecipeBrowserEntry> filteredEntries(RecipeBrowserTab tab) {
        return List.of();
    }

    @Override
    public void renderTabIcon(GuiGraphicsExtractor graphics, RecipeBrowserTab tab, int x, int y) {
    }

    @Override
    public void render(GuiGraphicsExtractor graphics, RecipeBrowserEntry entry, int x, int y) {
    }

    @Override
    public List<Component> tooltip(RecipeBrowserEntry entry) {
        return List.of();
    }

    @Override
    public RecipeBrowserEntry entryForItemStack(ItemStack stack) {
        return null;
    }

    @Override
    public boolean matchesRecipeKey(int key) {
        return false;
    }

    @Override
    public boolean matchesUsesKey(int key) {
        return false;
    }

    @Override
    public void addLookupHistory(RecipeBrowserEntry entry) {
    }

    @Override
    public List<RecipeBrowserCategory> recipeCategories(RecipeBrowserEntry entry, RecipeBrowserMode mode) {
        return List.of();
    }

    @Override
    public List<RecipeBrowserRecipe> recipes(RecipeBrowserEntry entry, RecipeBrowserMode mode, RecipeBrowserCategory category) {
        return List.of();
    }

    @Override
    public List<RecipeBrowserEntry> craftingStations(RecipeBrowserCategory category) {
        return List.of();
    }

    @Override
    public void renderRecipeCategoryIcon(GuiGraphicsExtractor graphics, RecipeBrowserCategory category, int x, int y) {
    }

    @Override
    public void renderRecipe(GuiGraphicsExtractor graphics, RecipeBrowserRecipe recipe, int x, int y, int mouseX, int mouseY) {
    }

    @Override
    public void renderRecipeOverlays(GuiGraphicsExtractor graphics, RecipeBrowserRecipe recipe, int x, int y, int mouseX, int mouseY) {
    }

    @Override
    public void renderRecipeSlotHighlights(GuiGraphicsExtractor graphics, RecipeBrowserRecipe recipe, int x, int y, List<Integer> inputIndexes, int color) {
    }

    @Override
    public void tickRecipe(RecipeBrowserRecipe recipe) {
    }

    @Override
    public boolean canBookmarkRecipe(RecipeBrowserRecipe recipe) {
        return false;
    }

    @Override
    public boolean isRecipeBookmarked(RecipeBrowserRecipe recipe) {
        return false;
    }

    @Override
    public void toggleRecipeBookmark(RecipeBrowserRecipe recipe) {
    }

    @Override
    public boolean isRecipeSortStageEnabled(RecipeBrowserSortStage stage) {
        return false;
    }

    @Override
    public void toggleRecipeSortStage(RecipeBrowserSortStage stage) {
    }

    @Override
    public RecipeBrowserTransferPlan recipeTransferPlan(RecipeBrowserRecipe recipe, AbstractContainerMenu menu) {
        return null;
    }

    @Override
    public RecipeBrowserEntry recipeIngredientAt(RecipeBrowserRecipe recipe, int x, int y, int mouseX, int mouseY) {
        return null;
    }

    @Override
    public boolean handleRecipeMouseScrolled(RecipeBrowserRecipe recipe, int x, int y, double mouseX, double mouseY, double scrollX, double scrollY) {
        return false;
    }

    @Override
    public boolean handleRecipeMouseClicked(RecipeBrowserRecipe recipe, int x, int y, MouseButtonEvent event, boolean doubleClick) {
        return false;
    }

    @Override
    public boolean handleRecipeMouseReleased(RecipeBrowserRecipe recipe, int x, int y, MouseButtonEvent event) {
        return false;
    }

    @Override
    public boolean handleRecipeMouseDragged(RecipeBrowserRecipe recipe, int x, int y, MouseButtonEvent event, double dragX, double dragY) {
        return false;
    }
}
