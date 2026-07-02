package com.salts_inventory_update.compat.jei;

import java.util.List;

import com.salts_inventory_update.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

public interface JeiDesktopAccess {
    boolean isAvailable();

    String filterText();

    void setFilterText(String text);

    List<JeiDesktopTab> tabs();

    List<JeiDesktopEntry> filteredEntries(JeiDesktopTab tab);

    void renderTabIcon(GuiGraphicsExtractor graphics, JeiDesktopTab tab, int x, int y);

    void render(GuiGraphicsExtractor graphics, JeiDesktopEntry entry, int x, int y);

    List<Component> tooltip(JeiDesktopEntry entry);

    JeiDesktopEntry entryForItemStack(ItemStack stack);

    boolean matchesRecipeKey(int key);

    boolean matchesUsesKey(int key);

    void addLookupHistory(JeiDesktopEntry entry);

    List<JeiRecipeCategory> recipeCategories(JeiDesktopEntry entry, JeiRecipeMode mode);

    List<JeiRecipeEntry> recipes(JeiDesktopEntry entry, JeiRecipeMode mode, JeiRecipeCategory category);

    List<JeiDesktopEntry> craftingStations(JeiRecipeCategory category);

    void renderRecipeCategoryIcon(GuiGraphicsExtractor graphics, JeiRecipeCategory category, int x, int y);

    void renderRecipe(GuiGraphicsExtractor graphics, JeiRecipeEntry recipe, int x, int y, int mouseX, int mouseY);

    void renderRecipeOverlays(GuiGraphicsExtractor graphics, JeiRecipeEntry recipe, int x, int y, int mouseX, int mouseY);

    void renderRecipeSlotHighlights(GuiGraphicsExtractor graphics, JeiRecipeEntry recipe, int x, int y, List<Integer> inputIndexes, int color);

    void tickRecipe(JeiRecipeEntry recipe);

    boolean canBookmarkRecipe(JeiRecipeEntry recipe);

    boolean isRecipeBookmarked(JeiRecipeEntry recipe);

    void toggleRecipeBookmark(JeiRecipeEntry recipe);

    boolean isRecipeSortStageEnabled(JeiRecipeSortStage stage);

    void toggleRecipeSortStage(JeiRecipeSortStage stage);

    JeiRecipeTransferPlan recipeTransferPlan(JeiRecipeEntry recipe, AbstractContainerMenu menu);

    JeiDesktopEntry recipeIngredientAt(JeiRecipeEntry recipe, int x, int y, int mouseX, int mouseY);

    boolean handleRecipeMouseScrolled(JeiRecipeEntry recipe, int x, int y, double mouseX, double mouseY, double scrollX, double scrollY);

    boolean handleRecipeMouseClicked(JeiRecipeEntry recipe, int x, int y, MouseButtonEvent event, boolean doubleClick);

    boolean handleRecipeMouseReleased(JeiRecipeEntry recipe, int x, int y, MouseButtonEvent event);

    boolean handleRecipeMouseDragged(JeiRecipeEntry recipe, int x, int y, MouseButtonEvent event, double dragX, double dragY);
}
