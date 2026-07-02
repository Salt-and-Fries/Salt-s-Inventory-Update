package com.salts_inventory_update.compat.jei;

import java.util.List;

import com.salts_inventory_update.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

final class NoopJeiDesktopAccess implements JeiDesktopAccess {
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
    public List<JeiDesktopTab> tabs() {
        return List.of();
    }

    @Override
    public List<JeiDesktopEntry> filteredEntries(JeiDesktopTab tab) {
        return List.of();
    }

    @Override
    public void renderTabIcon(GuiGraphicsExtractor graphics, JeiDesktopTab tab, int x, int y) {
    }

    @Override
    public void render(GuiGraphicsExtractor graphics, JeiDesktopEntry entry, int x, int y) {
    }

    @Override
    public List<Component> tooltip(JeiDesktopEntry entry) {
        return List.of();
    }

    @Override
    public JeiDesktopEntry entryForItemStack(ItemStack stack) {
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
    public void addLookupHistory(JeiDesktopEntry entry) {
    }

    @Override
    public List<JeiRecipeCategory> recipeCategories(JeiDesktopEntry entry, JeiRecipeMode mode) {
        return List.of();
    }

    @Override
    public List<JeiRecipeEntry> recipes(JeiDesktopEntry entry, JeiRecipeMode mode, JeiRecipeCategory category) {
        return List.of();
    }

    @Override
    public List<JeiDesktopEntry> craftingStations(JeiRecipeCategory category) {
        return List.of();
    }

    @Override
    public void renderRecipeCategoryIcon(GuiGraphicsExtractor graphics, JeiRecipeCategory category, int x, int y) {
    }

    @Override
    public void renderRecipe(GuiGraphicsExtractor graphics, JeiRecipeEntry recipe, int x, int y, int mouseX, int mouseY) {
    }

    @Override
    public void renderRecipeOverlays(GuiGraphicsExtractor graphics, JeiRecipeEntry recipe, int x, int y, int mouseX, int mouseY) {
    }

    @Override
    public void renderRecipeSlotHighlights(GuiGraphicsExtractor graphics, JeiRecipeEntry recipe, int x, int y, List<Integer> inputIndexes, int color) {
    }

    @Override
    public void tickRecipe(JeiRecipeEntry recipe) {
    }

    @Override
    public boolean canBookmarkRecipe(JeiRecipeEntry recipe) {
        return false;
    }

    @Override
    public boolean isRecipeBookmarked(JeiRecipeEntry recipe) {
        return false;
    }

    @Override
    public void toggleRecipeBookmark(JeiRecipeEntry recipe) {
    }

    @Override
    public boolean isRecipeSortStageEnabled(JeiRecipeSortStage stage) {
        return false;
    }

    @Override
    public void toggleRecipeSortStage(JeiRecipeSortStage stage) {
    }

    @Override
    public JeiRecipeTransferPlan recipeTransferPlan(JeiRecipeEntry recipe, AbstractContainerMenu menu) {
        return null;
    }

    @Override
    public JeiDesktopEntry recipeIngredientAt(JeiRecipeEntry recipe, int x, int y, int mouseX, int mouseY) {
        return null;
    }

    @Override
    public boolean handleRecipeMouseScrolled(JeiRecipeEntry recipe, int x, int y, double mouseX, double mouseY, double scrollX, double scrollY) {
        return false;
    }

    @Override
    public boolean handleRecipeMouseClicked(JeiRecipeEntry recipe, int x, int y, MouseButtonEvent event, boolean doubleClick) {
        return false;
    }

    @Override
    public boolean handleRecipeMouseReleased(JeiRecipeEntry recipe, int x, int y, MouseButtonEvent event) {
        return false;
    }

    @Override
    public boolean handleRecipeMouseDragged(JeiRecipeEntry recipe, int x, int y, MouseButtonEvent event, double dragX, double dragY) {
        return false;
    }
}
