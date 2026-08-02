package com.salts_inventory_update.compat.recipebrowser;

import java.util.List;

import com.salts_inventory_update.client.gui.GuiGraphicsExtractor;
import com.salts_inventory_update.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public interface RecipeBrowserAccess {
    RecipeBrowserSource source();

    boolean isAvailable();

    String filterText();

    void setFilterText(String text);

    List<RecipeBrowserTab> tabs();

    List<RecipeBrowserEntry> filteredEntries(RecipeBrowserTab tab);

    void renderTabIcon(GuiGraphicsExtractor graphics, RecipeBrowserTab tab, int x, int y);

    void render(GuiGraphicsExtractor graphics, RecipeBrowserEntry entry, int x, int y);

    List<Component> tooltip(RecipeBrowserEntry entry);

    RecipeBrowserEntry entryForItemStack(ItemStack stack);

    boolean matchesRecipeKey(int key);

    boolean matchesUsesKey(int key);

    void addLookupHistory(RecipeBrowserEntry entry);

    List<RecipeBrowserCategory> recipeCategories(RecipeBrowserEntry entry, RecipeBrowserMode mode);

    List<RecipeBrowserRecipe> recipes(RecipeBrowserEntry entry, RecipeBrowserMode mode, RecipeBrowserCategory category);

    List<RecipeBrowserEntry> craftingStations(RecipeBrowserCategory category);

    void renderRecipeCategoryIcon(GuiGraphicsExtractor graphics, RecipeBrowserCategory category, int x, int y);

    void renderRecipe(GuiGraphicsExtractor graphics, RecipeBrowserRecipe recipe, int x, int y, int mouseX, int mouseY);

    void renderRecipeOverlays(GuiGraphicsExtractor graphics, RecipeBrowserRecipe recipe, int x, int y, int mouseX, int mouseY);

    void renderRecipeSlotHighlights(GuiGraphicsExtractor graphics, RecipeBrowserRecipe recipe, int x, int y, List<Integer> inputIndexes, int color);

    void tickRecipe(RecipeBrowserRecipe recipe);

    boolean canBookmarkRecipe(RecipeBrowserRecipe recipe);

    boolean isRecipeBookmarked(RecipeBrowserRecipe recipe);

    void toggleRecipeBookmark(RecipeBrowserRecipe recipe);

    default @Nullable RecipeBrowserView expandedViewAt(RecipeBrowserRecipe recipe, int x, int y, double mouseX, double mouseY) {
        return null;
    }

    boolean isRecipeSortStageEnabled(RecipeBrowserSortStage stage);

    void toggleRecipeSortStage(RecipeBrowserSortStage stage);

    RecipeBrowserTransferPlan recipeTransferPlan(RecipeBrowserRecipe recipe, AbstractContainerMenu menu);

    RecipeBrowserEntry recipeIngredientAt(RecipeBrowserRecipe recipe, int x, int y, int mouseX, int mouseY);

    boolean handleRecipeMouseScrolled(RecipeBrowserRecipe recipe, int x, int y, double mouseX, double mouseY, double scrollX, double scrollY);

    boolean handleRecipeMouseClicked(RecipeBrowserRecipe recipe, int x, int y, MouseButtonEvent event, boolean doubleClick);

    boolean handleRecipeMouseReleased(RecipeBrowserRecipe recipe, int x, int y, MouseButtonEvent event);

    boolean handleRecipeMouseDragged(RecipeBrowserRecipe recipe, int x, int y, MouseButtonEvent event, double dragX, double dragY);
}
