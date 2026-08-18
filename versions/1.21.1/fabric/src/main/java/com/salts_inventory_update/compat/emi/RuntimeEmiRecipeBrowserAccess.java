package com.salts_inventory_update.compat.emi;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.salts_inventory_update.client.gui.GuiGraphicsExtractor;
import com.salts_inventory_update.client.input.MouseButtonEvent;
import com.salts_inventory_update.compat.recipebrowser.RecipeBrowserAccess;
import com.salts_inventory_update.compat.recipebrowser.RecipeBrowserCategory;
import com.salts_inventory_update.compat.recipebrowser.RecipeBrowserEntry;
import com.salts_inventory_update.compat.recipebrowser.RecipeBrowserMode;
import com.salts_inventory_update.compat.recipebrowser.RecipeBrowserRecipe;
import com.salts_inventory_update.compat.recipebrowser.RecipeBrowserSortStage;
import com.salts_inventory_update.compat.recipebrowser.RecipeBrowserSource;
import com.salts_inventory_update.compat.recipebrowser.RecipeBrowserTab;
import com.salts_inventory_update.compat.recipebrowser.RecipeBrowserTabKind;
import com.salts_inventory_update.compat.recipebrowser.RecipeBrowserTransferPlan;
import com.salts_inventory_update.compat.recipebrowser.RecipeBrowserTransferRect;
import com.salts_inventory_update.compat.recipebrowser.RecipeBrowserTransferSlot;
import com.salts_inventory_update.platform.loader.api.FabricLoader;
import dev.emi.emi.api.EmiApi;
import dev.emi.emi.api.recipe.EmiCraftingRecipe;
import dev.emi.emi.api.recipe.EmiIngredientRecipe;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.recipe.EmiRecipeManager;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.Bounds;
import dev.emi.emi.api.widget.SlotWidget;
import dev.emi.emi.api.widget.Widget;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CrafterMenu;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import org.jspecify.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

final class RuntimeEmiRecipeBrowserAccess implements RecipeBrowserAccess {
    private static final String EMI_MOD_ID = "emi";
    private static final String FAVORITES_TAB_UID = "salts_inventory_update:emi_favorites";
    private static final String RECENT_TAB_UID = "salts_inventory_update:emi_recent";
    private static final String EMI_FAVORITES_CLASS = "dev.emi.emi.runtime.EmiFavorites";
    private static final String EMI_PERSISTENT_DATA_CLASS = "dev.emi.emi.runtime.EmiPersistentData";
    private static final String EMI_SIDEBARS_CLASS = "dev.emi.emi.runtime.EmiSidebars";
    private static final String EMI_COMPILED_QUERY_CLASS = "dev.emi.emi.search.EmiSearch$CompiledQuery";
    private static final String JEMI_RECIPE_CLASS = "dev.emi.emi.jemi.JemiRecipe";
    private static final String JEI_TAG_CATEGORY_CLASS = "mezz.jei.library.plugins.jei.tags.TagInfoRecipeCategory";
    private static final int RECENT_HISTORY_LIMIT = 64;
    private static final int MAX_TRANSFER_ALTERNATIVES = 128;
    private static final int TRANSFER_BUTTON_SIZE = 13;
    private static final int EMI_INGREDIENT_RECIPE_MAX_HEIGHT = 78;
    private static final int JEMI_TAG_RECIPE_HEIGHT = 42;
    private static final int JEMI_TAG_MEMBER_ROW_Y = 24;
    private static final int EMI_SLOT_HOVER_COLOR = 0x80FFFFFF;
    private static final int OFFSCREEN_MOUSE_COORDINATE = -1_000_000;

    private final List<RecipeBrowserEntry> recentEntries = new ArrayList<>();
    private final Set<RecipeBrowserSortStage> enabledSortStages = EnumSet.noneOf(RecipeBrowserSortStage.class);
    private final Map<EmiRecipe, List<Widget>> recipeWidgetCache = new IdentityHashMap<>();
    private final Map<String, List<RecipeBrowserEntry>> filteredIndexEntryCache = new LinkedHashMap<>();
    private @Nullable List<EmiStack> cachedIndexSource;
    private List<RecipeBrowserTab> cachedIndexTabs = List.of();
    private Map<String, List<RecipeBrowserEntry>> cachedIndexEntriesByType = Map.of();
    private String filterText = "";
    private String compiledSearchText = "";
    private @Nullable Object compiledSearchQuery;

    @Override
    public RecipeBrowserSource source() {
        return RecipeBrowserSource.EMI;
    }

    @Override
    public boolean isAvailable() {
        return FabricLoader.getInstance().isModLoaded(EMI_MOD_ID);
    }

    @Override
    public String filterText() {
        return this.filterText;
    }

    @Override
    public synchronized void setFilterText(String text) {
        String nextText = text == null ? "" : text;
        if (!Objects.equals(this.filterText, nextText)) {
            this.filterText = nextText;
            this.filteredIndexEntryCache.clear();
        }
        if (!Objects.equals(nextText, this.compiledSearchText)) {
            this.compiledSearchText = "";
            this.compiledSearchQuery = null;
        }
    }

    @Override
    public synchronized List<RecipeBrowserTab> tabs() {
        this.ensureIndexCache();
        return this.cachedIndexTabs;
    }

    @Override
    public synchronized List<RecipeBrowserEntry> filteredEntries(RecipeBrowserTab tab) {
        if (tab != null && tab.kind() == RecipeBrowserTabKind.FAVORITES) {
            return this.filterEntries(this.favoriteEntries());
        }
        if (tab != null && tab.kind() == RecipeBrowserTabKind.RECENT) {
            return this.filterEntries(this.recentEntries());
        }

        this.ensureIndexCache();
        String typeUid = tab == null ? "" : tab.uid();
        List<RecipeBrowserEntry> entries = this.cachedIndexEntriesByType.getOrDefault(typeUid, List.of());
        if (this.filterText.trim().isEmpty()) {
            return entries;
        }
        return this.filteredIndexEntryCache.computeIfAbsent(typeUid, ignored -> this.filterEntries(entries));
    }

    private void ensureIndexCache() {
        List<EmiStack> source = this.entryStacks();
        if (source == this.cachedIndexSource) {
            return;
        }

        Map<String, RecipeBrowserTab> tabs = new LinkedHashMap<>();
        Map<String, List<RecipeBrowserEntry>> entriesByType = new LinkedHashMap<>();
        tabs.put(FAVORITES_TAB_UID, new RecipeBrowserTab(
            FAVORITES_TAB_UID,
            Component.literal("Favorites"),
            null,
            RecipeBrowserTabKind.FAVORITES
        ));
        tabs.put(RECENT_TAB_UID, new RecipeBrowserTab(
            RECENT_TAB_UID,
            Component.literal("Recent"),
            null,
            RecipeBrowserTabKind.RECENT
        ));
        for (EmiStack stack : this.entryStacks()) {
            if (stack == null || stack.isEmpty()) {
                continue;
            }
            String uid = typeUid(stack);
            RecipeBrowserEntry entry = this.wrap(stack);
            entriesByType.computeIfAbsent(uid, ignored -> new ArrayList<>()).add(entry);
            tabs.computeIfAbsent(uid, key -> new RecipeBrowserTab(key, Component.literal(typeTitle(stack)), entry));
        }
        for (Map.Entry<String, List<RecipeBrowserEntry>> entry : entriesByType.entrySet()) {
            entry.setValue(List.copyOf(entry.getValue()));
        }
        this.cachedIndexSource = source;
        this.cachedIndexTabs = List.copyOf(tabs.values());
        this.cachedIndexEntriesByType = entriesByType;
        this.filteredIndexEntryCache.clear();
    }

    @Override
    public void renderTabIcon(GuiGraphicsExtractor graphics, RecipeBrowserTab tab, int x, int y) {
        RecipeBrowserEntry icon = tab == null ? null : tab.icon();
        if (icon != null) {
            this.render(graphics, icon, x, y);
        }
    }

    @Override
    public void render(GuiGraphicsExtractor graphics, RecipeBrowserEntry entry, int x, int y) {
        EmiIngredient ingredient = this.ingredient(entry);
        if (ingredient != null && !ingredient.isEmpty()) {
            ingredient.render(graphics.unwrap(), x, y, 0.0F);
        }
    }

    @Override
    public List<Component> tooltip(RecipeBrowserEntry entry) {
        EmiStack stack = this.entryStack(entry);
        if (stack == null || stack.isEmpty()) {
            return List.of();
        }
        try {
            return List.copyOf(stack.getTooltipText());
        } catch (RuntimeException | LinkageError ignored) {
            return List.of(stack.getName());
        }
    }

    @Override
    public @Nullable RecipeBrowserEntry entryForItemStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        return this.wrap(EmiStack.of(stack.copy()));
    }

    @Override
    public boolean matchesRecipeKey(int key) {
        return key == GLFW.GLFW_KEY_R;
    }

    @Override
    public boolean matchesUsesKey(int key) {
        return key == GLFW.GLFW_KEY_U;
    }

    @Override
    public void addLookupHistory(RecipeBrowserEntry entry) {
        EmiIngredient ingredient = this.ingredient(entry);
        if (ingredient == null || ingredient.isEmpty()) {
            return;
        }
        this.recentEntries.removeIf(recent -> this.sameEntry(recent, entry));
        this.recentEntries.add(0, entry);
        while (this.recentEntries.size() > RECENT_HISTORY_LIMIT) {
            this.recentEntries.remove(this.recentEntries.size() - 1);
        }
        invokeStatic(EMI_SIDEBARS_CLASS, "lookup", new Class<?>[]{EmiIngredient.class}, ingredient);
    }

    @Override
    public List<RecipeBrowserCategory> recipeCategories(RecipeBrowserEntry entry, RecipeBrowserMode mode) {
        Map<String, RecipeBrowserCategory> categories = new LinkedHashMap<>();
        for (EmiRecipe recipe : this.matchingRecipes(entry, mode)) {
            EmiRecipeCategory category = recipe.getCategory();
            if (category == null) {
                continue;
            }
            String uid = category.getId().toString();
            RecipeBrowserCategory previous = categories.get(uid);
            int width = Math.max(1, recipe.getDisplayWidth());
            int height = recipeDisplayHeight(recipe);
            if (previous == null) {
                categories.put(uid, new RecipeBrowserCategory(uid, category.getName(), width, height, category));
            } else if (width > previous.width() || height > previous.height()) {
                categories.put(uid, new RecipeBrowserCategory(
                    uid,
                    previous.title(),
                    Math.max(width, previous.width()),
                    Math.max(height, previous.height()),
                    category
                ));
            }
        }
        return List.copyOf(categories.values());
    }

    @Override
    public List<RecipeBrowserRecipe> recipes(RecipeBrowserEntry entry, RecipeBrowserMode mode, RecipeBrowserCategory category) {
        List<RecipeBrowserRecipe> recipes = new ArrayList<>();
        int index = 0;
        for (EmiRecipe recipe : this.matchingRecipes(entry, mode)) {
            EmiRecipeCategory recipeCategory = recipe.getCategory();
            if (recipeCategory == null || (category != null && !Objects.equals(category.uid(), recipeCategory.getId().toString()))) {
                continue;
            }
            ResourceLocation id = recipe.getId();
            String categoryUid = recipeCategory.getId().toString();
            String recipeId = id == null ? null : id.toString();
            recipes.add(new RecipeBrowserRecipe(
                categoryUid + ":" + (recipeId == null ? index : recipeId),
                categoryUid,
                recipeId,
                Math.max(1, recipe.getDisplayWidth()),
                recipeDisplayHeight(recipe),
                recipe,
                recipeCategory
            ));
            index++;
        }
        this.sortRecipes(recipes);
        return recipes;
    }

    @Override
    public boolean usesCompactRecipeSpacing(RecipeBrowserCategory category) {
        return category != null && ("emi:tag".equals(category.uid()) || isJemiTagCategory(this.category(category)));
    }

    @Override
    public List<RecipeBrowserEntry> craftingStations(RecipeBrowserCategory category) {
        EmiRecipeCategory emiCategory = this.category(category);
        if (emiCategory == null) {
            return List.of();
        }
        List<RecipeBrowserEntry> stations = new ArrayList<>();
        try {
            for (EmiIngredient ingredient : this.recipeManager().getWorkstations(emiCategory)) {
                this.addIngredientEntries(stations, ingredient);
            }
        } catch (RuntimeException | LinkageError ignored) {
            return List.of();
        }
        return this.distinctEntries(stations);
    }

    @Override
    public void renderRecipeCategoryIcon(GuiGraphicsExtractor graphics, RecipeBrowserCategory category, int x, int y) {
        EmiRecipeCategory emiCategory = this.category(category);
        if (emiCategory != null) {
            emiCategory.render(graphics.unwrap(), x, y, 0.0F);
        }
    }

    @Override
    public void renderRecipe(GuiGraphicsExtractor graphics, RecipeBrowserRecipe recipe, int x, int y, int mouseX, int mouseY) {
        List<Widget> widgets = this.widgets(recipe);
        if (widgets.isEmpty()) {
            return;
        }
        int localMouseX = mouseX - x;
        int localMouseY = mouseY - y;
        graphics.pose().pushMatrix();
        graphics.pose().translate(x, y);
        try {
            for (Widget widget : widgets) {
                if (widget instanceof SlotWidget slot
                    && !slot.getStack().isEmpty()
                    && slot.getBounds().contains(localMouseX, localMouseY)) {
                    this.renderHoveredSlot(graphics, slot);
                } else {
                    widget.render(graphics.unwrap(), localMouseX, localMouseY, 0.0F);
                }
            }
        } finally {
            graphics.pose().popMatrix();
        }
    }

    private void renderHoveredSlot(GuiGraphicsExtractor graphics, SlotWidget slot) {
        Bounds bounds = slot.getBounds();
        slot.drawBackground(graphics.unwrap(), OFFSCREEN_MOUSE_COORDINATE, OFFSCREEN_MOUSE_COORDINATE, 0.0F);
        graphics.fill(
            bounds.left() + 1,
            bounds.top() + 1,
            bounds.right() - 1,
            bounds.bottom() - 1,
            EMI_SLOT_HOVER_COLOR
        );
        slot.drawStack(graphics.unwrap(), OFFSCREEN_MOUSE_COORDINATE, OFFSCREEN_MOUSE_COORDINATE, 0.0F);
        slot.drawOverlay(graphics.unwrap(), OFFSCREEN_MOUSE_COORDINATE, OFFSCREEN_MOUSE_COORDINATE, 0.0F);
    }

    @Override
    public void renderRecipeOverlays(GuiGraphicsExtractor graphics, RecipeBrowserRecipe recipe, int x, int y, int mouseX, int mouseY) {
    }

    @Override
    public boolean renderRecipeTooltip(
        GuiGraphicsExtractor graphics,
        RecipeBrowserRecipe recipe,
        int x,
        int y,
        int mouseX,
        int mouseY
    ) {
        int localX = mouseX - x;
        int localY = mouseY - y;
        List<Widget> widgets = this.widgets(recipe);
        for (int index = widgets.size() - 1; index >= 0; index--) {
            Widget widget = widgets.get(index);
            if (!widget.getBounds().contains(localX, localY)) {
                continue;
            }
            List<ClientTooltipComponent> tooltip = widget.getTooltip(localX, localY);
            if (!tooltip.isEmpty()) {
                graphics.setClientTooltipForNextFrame(Minecraft.getInstance().font, tooltip, mouseX, mouseY);
                return true;
            }
        }
        return false;
    }

    @Override
    public void renderRecipeSlotHighlights(
        GuiGraphicsExtractor graphics,
        RecipeBrowserRecipe recipe,
        int x,
        int y,
        List<Integer> inputIndexes,
        int color
    ) {
        int inputIndex = 0;
        for (Widget widget : this.widgets(recipe)) {
            if (!(widget instanceof SlotWidget slot) || slot.getRecipe() != null) {
                continue;
            }
            if (inputIndexes.contains(inputIndex)) {
                Bounds bounds = slot.getBounds();
                graphics.fill(
                    x + bounds.left() + 1,
                    y + bounds.top() + 1,
                    x + bounds.right() - 1,
                    y + bounds.bottom() - 1,
                    color
                );
            }
            inputIndex++;
        }
    }

    @Override
    public void tickRecipe(RecipeBrowserRecipe recipe) {
    }

    @Override
    public boolean canBookmarkRecipe(RecipeBrowserRecipe recipe) {
        return this.recipeFavoriteOutput(this.emiRecipe(recipe)) != null;
    }

    @Override
    public boolean isRecipeBookmarked(RecipeBrowserRecipe recipe) {
        EmiStack output = this.recipeFavoriteOutput(this.emiRecipe(recipe));
        return output != null && !this.matchingFavorites(output).isEmpty();
    }

    @Override
    public void toggleRecipeBookmark(RecipeBrowserRecipe recipe) {
        EmiStack output = this.recipeFavoriteOutput(this.emiRecipe(recipe));
        if (output == null) {
            return;
        }
        List<Object> existing = this.matchingFavorites(output);
        if (!existing.isEmpty()) {
            for (Object favorite : existing) {
                invokeStatic(EMI_FAVORITES_CLASS, "removeFavorite", new Class<?>[]{EmiIngredient.class}, favorite);
            }
            invokeStatic(EMI_PERSISTENT_DATA_CLASS, "save", new Class<?>[0]);
        } else {
            invokeStatic(EMI_FAVORITES_CLASS, "addFavorite", new Class<?>[]{EmiIngredient.class}, output.copy());
        }
    }

    @Override
    public boolean isRecipeSortStageEnabled(RecipeBrowserSortStage stage) {
        return stage != null && this.enabledSortStages.contains(stage);
    }

    @Override
    public void toggleRecipeSortStage(RecipeBrowserSortStage stage) {
        if (stage == null || !this.enabledSortStages.remove(stage)) {
            if (stage != null) {
                this.enabledSortStages.add(stage);
            }
        }
    }

    @Override
    public @Nullable RecipeBrowserTransferPlan recipeTransferPlan(RecipeBrowserRecipe recipe, AbstractContainerMenu menu) {
        EmiRecipe emiRecipe = this.emiRecipe(recipe);
        if (!(emiRecipe instanceof EmiCraftingRecipe craftingRecipe)) {
            return null;
        }

        EmiCraftingGrid grid = EmiCraftingGrid.forMenu(menu);
        if (grid == null || !craftingRecipe.canFit(grid.width(), grid.height()) || grid.slotStart() + grid.slotCount() > menu.slots.size()) {
            return null;
        }

        List<EmiIngredient> inputs = craftingRecipe.getInputs();
        if (inputs.isEmpty() || inputs.size() > 9) {
            return null;
        }

        List<Integer> recipeSlotIds = new ArrayList<>(grid.slotCount());
        for (int index = 0; index < grid.slotCount(); index++) {
            net.minecraft.world.inventory.Slot slot = menu.getSlot(grid.slotStart() + index);
            if (!slot.isActive() || slot.isFake()) {
                return null;
            }
            recipeSlotIds.add(slot.index);
        }

        List<RecipeBrowserTransferSlot> requirements = new ArrayList<>();
        int packedShapelessIndex = 0;
        for (int inputIndex = 0; inputIndex < inputs.size(); inputIndex++) {
            EmiIngredient input = inputs.get(inputIndex);
            if (input == null || input.isEmpty()) {
                continue;
            }
            int gridIndex = craftingRecipe.shapeless
                ? packedShapelessIndex++
                : inputIndex % 3 + inputIndex / 3 * grid.width();
            if (gridIndex < 0 || gridIndex >= grid.slotCount()) {
                return null;
            }
            List<ItemStack> alternatives = itemAlternatives(input);
            if (alternatives.isEmpty()) {
                return null;
            }
            requirements.add(new RecipeBrowserTransferSlot(
                craftingDisplaySlot(craftingRecipe, inputIndex),
                menu.getSlot(grid.slotStart() + gridIndex).index,
                alternatives
            ));
        }
        if (requirements.isEmpty()) {
            return null;
        }
        return new RecipeBrowserTransferPlan(
            new RecipeBrowserTransferRect(0, 0, TRANSFER_BUTTON_SIZE, TRANSFER_BUTTON_SIZE),
            recipeSlotIds,
            requirements
        );
    }

    @Override
    public @Nullable RecipeBrowserEntry recipeIngredientAt(
        RecipeBrowserRecipe recipe,
        int x,
        int y,
        int mouseX,
        int mouseY
    ) {
        int localX = mouseX - x;
        int localY = mouseY - y;
        List<Widget> widgets = this.widgets(recipe);
        for (int index = widgets.size() - 1; index >= 0; index--) {
            Widget widget = widgets.get(index);
            if (!(widget instanceof SlotWidget slot) || !slot.getBounds().contains(localX, localY)) {
                continue;
            }
            EmiStack stack = firstStack(slot.getStack());
            return stack == null || stack.isEmpty() ? null : this.wrap(stack);
        }
        return null;
    }

    @Override
    public boolean handleRecipeMouseScrolled(
        RecipeBrowserRecipe recipe,
        int x,
        int y,
        double mouseX,
        double mouseY,
        double scrollX,
        double scrollY
    ) {
        return false;
    }

    @Override
    public boolean handleRecipeMouseClicked(
        RecipeBrowserRecipe recipe,
        int x,
        int y,
        MouseButtonEvent event,
        boolean doubleClick
    ) {
        int localX = (int) Math.floor(event.x() - x);
        int localY = (int) Math.floor(event.y() - y);
        List<Widget> widgets = this.widgets(recipe);
        for (int index = widgets.size() - 1; index >= 0; index--) {
            Widget widget = widgets.get(index);
            if (widget.getBounds().contains(localX, localY) && widget.mouseClicked(localX, localY, event.button())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean handleRecipeMouseReleased(RecipeBrowserRecipe recipe, int x, int y, MouseButtonEvent event) {
        return false;
    }

    @Override
    public boolean handleRecipeMouseDragged(
        RecipeBrowserRecipe recipe,
        int x,
        int y,
        MouseButtonEvent event,
        double dragX,
        double dragY
    ) {
        return false;
    }

    private EmiRecipeManager recipeManager() {
        return EmiApi.getRecipeManager();
    }

    private List<EmiStack> entryStacks() {
        try {
            List<EmiStack> stacks = EmiApi.getIndexStacks();
            return stacks == null ? List.of() : stacks;
        } catch (RuntimeException | LinkageError ignored) {
            return List.of();
        }
    }

    private List<RecipeBrowserEntry> favoriteEntries() {
        List<RecipeBrowserEntry> entries = new ArrayList<>();
        for (Object favorite : staticList(EMI_FAVORITES_CLASS, "favorites")) {
            Object value = invoke(favorite, "getStack");
            if (value instanceof EmiIngredient ingredient) {
                this.addIngredientEntries(entries, ingredient);
            }
        }
        return this.distinctEntries(entries);
    }

    private List<RecipeBrowserEntry> recentEntries() {
        List<RecipeBrowserEntry> entries = new ArrayList<>();
        for (Object value : staticList(EMI_SIDEBARS_CLASS, "lookupHistory")) {
            if (value instanceof EmiIngredient ingredient) {
                this.addIngredientEntries(entries, ingredient);
            }
        }
        entries.addAll(this.recentEntries);
        return this.distinctEntries(entries);
    }

    private List<RecipeBrowserEntry> filterEntries(List<RecipeBrowserEntry> entries) {
        String query = this.filterText.trim();
        if (query.isEmpty()) {
            return entries;
        }
        Object compiled = this.compiledSearchQuery(query);
        List<RecipeBrowserEntry> filtered = new ArrayList<>();
        for (RecipeBrowserEntry entry : entries) {
            EmiStack stack = this.entryStack(entry);
            Boolean compiledMatch = stack == null ? null : matchesCompiledQuery(compiled, stack);
            if (stack != null && (Boolean.TRUE.equals(compiledMatch) || (compiledMatch == null && fallbackSearch(stack, query)))) {
                filtered.add(entry);
            }
        }
        return filtered;
    }

    private @Nullable Object compiledSearchQuery(String query) {
        if (Objects.equals(query, this.compiledSearchText)) {
            return this.compiledSearchQuery;
        }
        this.compiledSearchText = query;
        this.compiledSearchQuery = null;
        try {
            Class<?> type = Class.forName(EMI_COMPILED_QUERY_CLASS);
            Constructor<?> constructor = type.getDeclaredConstructor(String.class);
            constructor.setAccessible(true);
            this.compiledSearchQuery = constructor.newInstance(query);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
        }
        return this.compiledSearchQuery;
    }

    private static @Nullable Boolean matchesCompiledQuery(@Nullable Object compiled, EmiStack stack) {
        if (compiled == null) {
            return null;
        }
        try {
            Method test = compiled.getClass().getDeclaredMethod("test", EmiStack.class);
            test.setAccessible(true);
            return Boolean.TRUE.equals(test.invoke(compiled, stack));
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
            return null;
        }
    }

    private static boolean fallbackSearch(EmiStack stack, String query) {
        String needle = query.toLowerCase(Locale.ROOT);
        if (stack.getId().toString().toLowerCase(Locale.ROOT).contains(needle)
            || stack.getName().getString().toLowerCase(Locale.ROOT).contains(needle)) {
            return true;
        }
        try {
            return stack.getTooltipText().stream()
                .map(Component::getString)
                .map(text -> text.toLowerCase(Locale.ROOT))
                .anyMatch(text -> text.contains(needle));
        } catch (RuntimeException | LinkageError ignored) {
            return false;
        }
    }

    private List<EmiRecipe> matchingRecipes(RecipeBrowserEntry entry, RecipeBrowserMode mode) {
        EmiStack focus = this.entryStack(entry);
        if (focus == null || focus.isEmpty() || mode == null || mode == RecipeBrowserMode.INGREDIENTS) {
            return List.of();
        }

        EmiRecipeManager manager = this.recipeManager();
        LinkedHashSet<EmiRecipe> recipes = new LinkedHashSet<>();
        if (mode == RecipeBrowserMode.RECIPES) {
            for (EmiRecipe recipe : manager.getRecipesByOutput(focus)) {
                if (recipe != null && recipe.getOutputs().stream().anyMatch(output -> output != null && output.isEqual(focus))) {
                    recipes.add(recipe);
                }
            }
        } else {
            for (EmiRecipe recipe : manager.getRecipesByInput(focus)) {
                if (recipe != null && this.recipeUses(recipe, focus)) {
                    recipes.add(recipe);
                }
            }
            for (EmiRecipeCategory category : manager.getCategories()) {
                boolean workstationMatches = manager.getWorkstations(category).stream()
                    .anyMatch(workstation -> ingredientContains(workstation, focus));
                if (workstationMatches) {
                    recipes.addAll(manager.getRecipes(category));
                }
            }
        }
        return List.copyOf(recipes);
    }

    private boolean recipeUses(EmiRecipe recipe, EmiStack focus) {
        return recipe.getInputs().stream().anyMatch(input -> ingredientContains(input, focus))
            || recipe.getCatalysts().stream().anyMatch(catalyst -> ingredientContains(catalyst, focus));
    }

    private static boolean ingredientContains(@Nullable EmiIngredient ingredient, EmiStack focus) {
        if (ingredient == null || ingredient.isEmpty()) {
            return false;
        }
        for (EmiStack stack : ingredient.getEmiStacks()) {
            if (stack != null && !stack.isEmpty() && stack.isEqual(focus)) {
                return true;
            }
        }
        return false;
    }

    private synchronized List<Widget> widgets(RecipeBrowserRecipe recipe) {
        EmiRecipe emiRecipe = this.emiRecipe(recipe);
        if (emiRecipe == null) {
            return List.of();
        }
        List<Widget> cached = this.recipeWidgetCache.get(emiRecipe);
        if (cached != null) {
            return cached;
        }
        if (isJemiTagRecipe(emiRecipe)) {
            List<Widget> widgets = compactJemiTagWidgets(
                emiRecipe,
                Math.max(1, recipe.width()),
                Math.max(1, recipe.height())
            );
            this.recipeWidgetCache.put(emiRecipe, widgets);
            return widgets;
        }
        EmiWidgetHolder holder = new EmiWidgetHolder(
            Math.max(1, recipe.width()),
            Math.max(1, recipe.height())
        );
        try {
            emiRecipe.addWidgets(holder);
            List<Widget> widgets = List.copyOf(holder.widgets);
            this.recipeWidgetCache.put(emiRecipe, widgets);
            return widgets;
        } catch (RuntimeException | LinkageError ignored) {
            this.recipeWidgetCache.put(emiRecipe, List.of());
            return List.of();
        }
    }

    private static int recipeDisplayHeight(EmiRecipe recipe) {
        int height = Math.max(1, recipe.getDisplayHeight());
        if (isJemiTagRecipe(recipe)) {
            return Math.min(height, JEMI_TAG_RECIPE_HEIGHT);
        }
        return recipe instanceof EmiIngredientRecipe
            ? Math.min(height, EMI_INGREDIENT_RECIPE_MAX_HEIGHT)
            : height;
    }

    private static List<Widget> compactJemiTagWidgets(EmiRecipe recipe, int width, int height) {
        List<Widget> widgets = new ArrayList<>();
        EmiIngredient tag = firstIngredient(recipe.getInputs());
        if (tag != null) {
            SlotWidget tagSlot = new SlotWidget(tag, Math.max(0, (width - 18) / 2), 0);
            Object originalId = publicField(recipe, "originalId");
            if (originalId instanceof ResourceLocation id) {
                tagSlot.appendTooltip(Component.literal("#" + id));
            }
            widgets.add(tagSlot);
        }

        List<EmiStack> members = recipe.getOutputs();
        int visibleMembers = Math.min(members.size(), Math.max(1, width / 18));
        int memberX = Math.max(0, (width - visibleMembers * 18) / 2);
        int memberY = Math.max(0, Math.min(JEMI_TAG_MEMBER_ROW_Y, height - 18));
        for (int index = 0; index < visibleMembers; index++) {
            EmiStack member = members.get(index);
            if (member != null && !member.isEmpty()) {
                widgets.add(new SlotWidget(member, memberX + index * 18, memberY).recipeContext(recipe));
            }
        }
        return List.copyOf(widgets);
    }

    private static @Nullable EmiIngredient firstIngredient(List<EmiIngredient> ingredients) {
        for (EmiIngredient ingredient : ingredients) {
            if (ingredient != null && !ingredient.isEmpty()) {
                return ingredient;
            }
        }
        return null;
    }

    private static boolean isJemiTagRecipe(@Nullable EmiRecipe recipe) {
        if (recipe == null || !JEMI_RECIPE_CLASS.equals(recipe.getClass().getName())) {
            return false;
        }
        return isJeiTagCategoryObject(publicField(recipe, "category"));
    }

    private static boolean isJemiTagCategory(@Nullable EmiRecipeCategory category) {
        return category != null && isJeiTagCategoryObject(publicField(category, "category"));
    }

    private static boolean isJeiTagCategoryObject(@Nullable Object category) {
        return category != null && JEI_TAG_CATEGORY_CLASS.equals(category.getClass().getName());
    }

    synchronized void invalidateCaches() {
        this.cachedIndexSource = null;
        this.cachedIndexTabs = List.of();
        this.cachedIndexEntriesByType = Map.of();
        this.filteredIndexEntryCache.clear();
        this.recipeWidgetCache.clear();
    }

    private void sortRecipes(List<RecipeBrowserRecipe> recipes) {
        boolean bookmarkedFirst = this.isRecipeSortStageEnabled(RecipeBrowserSortStage.BOOKMARKED);
        boolean craftableFirst = this.isRecipeSortStageEnabled(RecipeBrowserSortStage.CRAFTABLE);
        if (!bookmarkedFirst && !craftableFirst) {
            return;
        }
        Comparator<RecipeBrowserRecipe> comparator = (left, right) -> 0;
        if (bookmarkedFirst) {
            comparator = comparator.thenComparing(recipe -> !this.isRecipeBookmarked(recipe));
        }
        if (craftableFirst) {
            comparator = comparator.thenComparing(recipe -> !this.isRecipeCraftable(recipe));
        }
        recipes.sort(comparator);
    }

    private boolean isRecipeCraftable(RecipeBrowserRecipe recipe) {
        EmiRecipe emiRecipe = this.emiRecipe(recipe);
        Minecraft minecraft = Minecraft.getInstance();
        if (emiRecipe == null || minecraft.player == null) {
            return false;
        }
        Inventory inventory = minecraft.player.getInventory();
        List<ItemStack> available = new ArrayList<>();
        for (int index = 0; index < inventory.getContainerSize(); index++) {
            ItemStack stack = inventory.getItem(index);
            if (!stack.isEmpty()) {
                available.add(stack.copy());
            }
        }
        boolean hasInput = false;
        for (EmiIngredient input : emiRecipe.getInputs()) {
            if (input == null || input.isEmpty()) {
                continue;
            }
            hasInput = true;
            if (!consumeAnyMatchingIngredient(available, itemAlternatives(input))) {
                return false;
            }
        }
        return hasInput;
    }

    private static boolean consumeAnyMatchingIngredient(List<ItemStack> available, List<ItemStack> options) {
        for (ItemStack option : options) {
            List<ItemStack> candidate = available.stream().map(ItemStack::copy).toList();
            if (consumeIngredient(candidate, option)) {
                available.clear();
                available.addAll(candidate);
                return true;
            }
        }
        return false;
    }

    private static boolean consumeIngredient(List<ItemStack> available, ItemStack ingredient) {
        int remaining = Math.max(1, ingredient.getCount());
        for (ItemStack stack : available) {
            if (!ItemStack.isSameItemSameComponents(stack, ingredient)) {
                continue;
            }
            int consumed = Math.min(stack.getCount(), remaining);
            stack.shrink(consumed);
            remaining -= consumed;
            if (remaining <= 0) {
                return true;
            }
        }
        return false;
    }

    private static List<ItemStack> itemAlternatives(EmiIngredient ingredient) {
        List<ItemStack> alternatives = new ArrayList<>();
        long requestedAmount = Math.max(1L, ingredient.getAmount());
        for (EmiStack stack : ingredient.getEmiStacks()) {
            if (stack == null || stack.isEmpty()) {
                continue;
            }
            ItemStack item = stack.getItemStack();
            if (item == null || item.isEmpty()) {
                continue;
            }
            int count = (int) Math.min(Integer.MAX_VALUE, Math.max(requestedAmount, stack.getAmount()));
            ItemStack alternative = item.copyWithCount(Math.max(1, count));
            boolean duplicate = alternatives.stream().anyMatch(existing ->
                existing.getCount() == alternative.getCount() && ItemStack.isSameItemSameComponents(existing, alternative)
            );
            if (!duplicate) {
                alternatives.add(alternative);
            }
            if (alternatives.size() >= MAX_TRANSFER_ALTERNATIVES) {
                break;
            }
        }
        return alternatives;
    }

    private static int craftingDisplaySlot(EmiCraftingRecipe recipe, int inputIndex) {
        if (recipe.shapeless) {
            return inputIndex;
        }
        int sourceOffset = 0;
        if (recipe.canFit(1, 3)) {
            sourceOffset--;
        }
        if (recipe.canFit(3, 1)) {
            sourceOffset -= 3;
        }
        return inputIndex - sourceOffset;
    }

    private @Nullable EmiStack recipeFavoriteOutput(@Nullable EmiRecipe recipe) {
        if (recipe == null) {
            return null;
        }
        for (EmiStack output : recipe.getOutputs()) {
            if (output != null && !output.isEmpty()) {
                return output.copy().setAmount(1L);
            }
        }
        return null;
    }

    private List<Object> matchingFavorites(EmiStack output) {
        List<Object> matching = new ArrayList<>();
        for (Object favorite : staticList(EMI_FAVORITES_CLASS, "favorites")) {
            Object value = invoke(favorite, "getStack");
            if (value instanceof EmiIngredient ingredient && sameSingleStack(ingredient, output)) {
                matching.add(favorite);
            }
        }
        return matching;
    }

    private static boolean sameSingleStack(EmiIngredient ingredient, EmiStack output) {
        List<EmiStack> stacks = ingredient.getEmiStacks();
        return stacks.size() == 1 && stacks.get(0) != null && stacks.get(0).isEqual(output);
    }

    private void addIngredientEntries(List<RecipeBrowserEntry> entries, @Nullable EmiIngredient ingredient) {
        if (ingredient == null || ingredient.isEmpty()) {
            return;
        }
        for (EmiStack stack : ingredient.getEmiStacks()) {
            if (stack != null && !stack.isEmpty()) {
                entries.add(this.wrap(stack));
            }
        }
    }

    private List<RecipeBrowserEntry> distinctEntries(List<RecipeBrowserEntry> entries) {
        List<RecipeBrowserEntry> distinct = new ArrayList<>();
        for (RecipeBrowserEntry entry : entries) {
            if (entry != null && distinct.stream().noneMatch(existing -> this.sameEntry(existing, entry))) {
                distinct.add(entry);
            }
        }
        return distinct;
    }

    private RecipeBrowserEntry wrap(EmiStack stack) {
        EmiStack copy = stack.copy();
        return new RecipeBrowserEntry(typeUid(copy), copy.getKey().getClass(), copy, stack);
    }

    private boolean sameEntry(RecipeBrowserEntry left, RecipeBrowserEntry right) {
        EmiStack leftStack = this.entryStack(left);
        EmiStack rightStack = this.entryStack(right);
        return leftStack != null && rightStack != null && leftStack.isEqual(rightStack);
    }

    private @Nullable EmiIngredient ingredient(RecipeBrowserEntry entry) {
        return entry != null && entry.ingredient() instanceof EmiIngredient ingredient ? ingredient : null;
    }

    private @Nullable EmiStack entryStack(RecipeBrowserEntry entry) {
        return firstStack(this.ingredient(entry));
    }

    private static @Nullable EmiStack firstStack(@Nullable EmiIngredient ingredient) {
        if (ingredient == null || ingredient.isEmpty()) {
            return null;
        }
        for (EmiStack stack : ingredient.getEmiStacks()) {
            if (stack != null && !stack.isEmpty()) {
                return stack;
            }
        }
        return null;
    }

    private @Nullable EmiRecipe emiRecipe(RecipeBrowserRecipe recipe) {
        return recipe != null && recipe.recipe() instanceof EmiRecipe emiRecipe ? emiRecipe : null;
    }

    private @Nullable EmiRecipeCategory category(RecipeBrowserCategory category) {
        return category != null && category.category() instanceof EmiRecipeCategory emiCategory ? emiCategory : null;
    }

    private static String typeUid(EmiStack stack) {
        Object key = stack.getKey();
        if (key instanceof Item) {
            return "minecraft:item";
        }
        if (key instanceof Fluid) {
            return "minecraft:fluid";
        }
        return "emi:" + key.getClass().getName().toLowerCase(Locale.ROOT).replace('$', '.');
    }

    private static String typeTitle(EmiStack stack) {
        Object key = stack.getKey();
        if (key instanceof Item) {
            return "Items";
        }
        if (key instanceof Fluid) {
            return "Fluids";
        }
        String simpleName = key.getClass().getSimpleName().replace('_', ' ');
        if (simpleName.isEmpty()) {
            return "Ingredients";
        }
        return Character.toUpperCase(simpleName.charAt(0)) + simpleName.substring(1);
    }

    private static List<?> staticList(String className, String fieldName) {
        try {
            Class<?> type = Class.forName(className);
            Field field = type.getField(fieldName);
            Object value = field.get(null);
            return value instanceof List<?> list ? List.copyOf(list) : List.of();
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
            return List.of();
        }
    }

    private static @Nullable Object invoke(@Nullable Object target, String methodName) {
        if (target == null) {
            return null;
        }
        try {
            return target.getClass().getMethod(methodName).invoke(target);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
            return null;
        }
    }

    private static @Nullable Object publicField(@Nullable Object target, String fieldName) {
        if (target == null) {
            return null;
        }
        try {
            return target.getClass().getField(fieldName).get(target);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
            return null;
        }
    }

    private static @Nullable Object invokeStatic(String className, String methodName, Class<?>[] parameterTypes, Object... arguments) {
        try {
            Class<?> type = Class.forName(className);
            return type.getMethod(methodName, parameterTypes).invoke(null, arguments);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
            return null;
        }
    }

    private static final class EmiWidgetHolder implements WidgetHolder {
        private final int width;
        private final int height;
        private final List<Widget> widgets = new ArrayList<>();

        private EmiWidgetHolder(int width, int height) {
            this.width = width;
            this.height = height;
        }

        @Override
        public int getWidth() {
            return this.width;
        }

        @Override
        public int getHeight() {
            return this.height;
        }

        @Override
        public <T extends Widget> T add(T widget) {
            if (widget != null) {
                this.widgets.add(widget);
            }
            return widget;
        }
    }

    private record EmiCraftingGrid(int width, int height, int slotStart, int slotCount) {
        private static @Nullable EmiCraftingGrid forMenu(AbstractContainerMenu menu) {
            if (menu instanceof CraftingMenu) {
                return new EmiCraftingGrid(3, 3, 1, 9);
            }
            if (menu instanceof InventoryMenu) {
                return new EmiCraftingGrid(2, 2, 1, 4);
            }
            if (menu instanceof CrafterMenu) {
                return new EmiCraftingGrid(3, 3, 0, 9);
            }
            return null;
        }
    }
}
