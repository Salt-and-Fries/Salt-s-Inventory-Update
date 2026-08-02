package com.salts_inventory_update.compat.rei;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

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
import com.salts_inventory_update.compat.recipebrowser.RecipeBrowserView;
import com.salts_inventory_update.platform.loader.api.FabricLoader;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.config.ConfigManager;
import me.shedaniel.rei.api.client.config.ConfigObject;
import me.shedaniel.rei.api.client.favorites.FavoriteEntry;
import me.shedaniel.rei.api.client.favorites.FavoriteEntryType;
import me.shedaniel.rei.api.client.gui.widgets.Slot;
import me.shedaniel.rei.api.client.gui.widgets.Tooltip;
import me.shedaniel.rei.api.client.gui.widgets.TooltipContext;
import me.shedaniel.rei.api.client.gui.widgets.Widget;
import me.shedaniel.rei.api.client.gui.widgets.WidgetWithBounds;
import me.shedaniel.rei.api.client.gui.widgets.Widgets;
import me.shedaniel.rei.api.client.registry.category.CategoryRegistry;
import me.shedaniel.rei.api.client.registry.display.DisplayCategory;
import me.shedaniel.rei.api.client.registry.display.DisplayCategoryView;
import me.shedaniel.rei.api.client.registry.entry.EntryRegistry;
import me.shedaniel.rei.api.client.search.SearchFilter;
import me.shedaniel.rei.api.client.search.SearchProvider;
import me.shedaniel.rei.api.client.view.ViewSearchBuilder;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.Display;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.entry.EntryStack;
import me.shedaniel.rei.api.common.entry.InputIngredient;
import me.shedaniel.rei.api.common.entry.type.VanillaEntryTypes;
import me.shedaniel.rei.api.common.util.EntryStacks;
import me.shedaniel.rei.impl.display.DisplaySpec;
import me.shedaniel.rei.plugin.client.categories.tag.TagTreeWidget;
import me.shedaniel.rei.plugin.common.displays.crafting.CraftingDisplay;
import me.shedaniel.rei.plugin.common.displays.tag.DefaultTagDisplay;
import me.shedaniel.rei.plugin.common.displays.tag.TagNode;
import me.shedaniel.rei.plugin.common.displays.tag.TagNodes;
import net.minecraft.client.Minecraft;
import com.salts_inventory_update.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CrafterMenu;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.joml.Matrix3x2f;
import org.jspecify.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

final class RuntimeReiRecipeBrowserAccess implements RecipeBrowserAccess {
    private static final String FAVORITES_TAB_UID = "salts_inventory_update:rei_favorites";
    private static final String RECENT_TAB_UID = "salts_inventory_update:rei_recent";
    private static final int RECENT_HISTORY_LIMIT = 64;
    private static final int MAX_TRANSFER_ALTERNATIVES = 128;
    private static final int TRANSFER_BUTTON_SIZE = 13;
    private static final String REI_MOD_ID = "roughlyenoughitems";
    private final List<RecipeBrowserEntry> recentEntries = new ArrayList<>();
    private String filterText = "";

    @Override
    public RecipeBrowserSource source() {
        return RecipeBrowserSource.REI;
    }

    @Override
    public boolean isAvailable() {
        if (!FabricLoader.getInstance().isModLoaded(REI_MOD_ID)) {
            return false;
        }
        try {
            EntryRegistry.getInstance();
            CategoryRegistry.getInstance();
            return true;
        } catch (RuntimeException | LinkageError ignored) {
            return false;
        }
    }

    @Override
    public String filterText() {
        return this.filterText;
    }

    @Override
    public void setFilterText(String text) {
        this.filterText = text == null ? "" : text;
    }

    @Override
    public List<RecipeBrowserTab> tabs() {
        Map<String, RecipeBrowserTab> tabs = new LinkedHashMap<>();
        tabs.put(FAVORITES_TAB_UID, new RecipeBrowserTab(FAVORITES_TAB_UID, Component.literal("Favorites"), null, RecipeBrowserTabKind.FAVORITES));
        tabs.put(RECENT_TAB_UID, new RecipeBrowserTab(RECENT_TAB_UID, Component.literal("Recent"), null, RecipeBrowserTabKind.RECENT));
        for (EntryStack<?> stack : this.entryStacks()) {
            if (stack == null || stack.isEmpty()) {
                continue;
            }
            String uid = typeUid(stack);
            tabs.computeIfAbsent(uid, key -> new RecipeBrowserTab(key, Component.literal(titleFor(stack.getType().getId())), this.wrap(stack)));
        }
        return List.copyOf(tabs.values());
    }

    @Override
    public List<RecipeBrowserEntry> filteredEntries(RecipeBrowserTab tab) {
        if (tab != null && tab.kind() == RecipeBrowserTabKind.FAVORITES) {
            return this.filterEntries(this.favoriteEntries());
        }
        if (tab != null && tab.kind() == RecipeBrowserTabKind.RECENT) {
            return this.filterEntries(this.recentEntries());
        }
        String typeUid = tab == null ? "" : tab.uid();
        List<RecipeBrowserEntry> entries = new ArrayList<>();
        for (EntryStack<?> stack : this.entryStacks()) {
            if (stack != null && !stack.isEmpty() && Objects.equals(typeUid(stack), typeUid)) {
                entries.add(this.wrap(stack));
            }
        }
        return this.filterEntries(entries);
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
        EntryStack<?> stack = this.entryStack(entry);
        if (stack != null && !stack.isEmpty()) {
            stack.render(graphics.unwrap(), new Rectangle(x, y, 16, 16), x, y, 0.0F);
        }
    }

    @Override
    public List<Component> tooltip(RecipeBrowserEntry entry) {
        EntryStack<?> stack = this.entryStack(entry);
        if (stack == null || stack.isEmpty()) {
            return List.of();
        }
        Minecraft minecraft = Minecraft.getInstance();
        Tooltip tooltip = stack.getTooltip(
            TooltipContext.of(
                new me.shedaniel.math.Point(0, 0),
                net.minecraft.world.item.Item.TooltipContext.of(minecraft.level),
                TooltipFlag.Default.NORMAL
            )
        );
        if (tooltip == null) {
            return List.of();
        }
        List<Component> components = new ArrayList<>();
        for (Tooltip.Entry tooltipEntry : tooltip.entries()) {
            if (tooltipEntry.isText()) {
                components.add(tooltipEntry.getAsText());
            }
        }
        return components;
    }

    @Override
    public @Nullable RecipeBrowserEntry entryForItemStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        return this.wrap(EntryStacks.of(stack.copy()));
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
        if (entry == null) {
            return;
        }
        this.recentEntries.removeIf(recent -> this.sameEntry(recent, entry));
        this.recentEntries.add(0, entry);
        while (this.recentEntries.size() > RECENT_HISTORY_LIMIT) {
            this.recentEntries.remove(this.recentEntries.size() - 1);
        }
    }

    @Override
    public List<RecipeBrowserCategory> recipeCategories(RecipeBrowserEntry entry, RecipeBrowserMode mode) {
        Map<String, RecipeBrowserCategory> categories = new LinkedHashMap<>();
        for (DisplaySpec spec : this.displays(entry, mode)) {
            Display display = spec.provideInternalDisplay();
            Optional<CategoryRegistry.CategoryConfiguration<Display>> config = this.categoryConfig(display.getCategoryIdentifier());
            if (config.isEmpty()) {
                continue;
            }
            DisplayCategory<Display> category = config.get().getCategory();
            if (!CategoryRegistry.getInstance().isCategoryVisible(category)) {
                continue;
            }
            String uid = category.getCategoryIdentifier().getIdentifier().toString();
            categories.computeIfAbsent(
                uid,
                key -> new RecipeBrowserCategory(
                    key,
                    category.getTitle(),
                    Math.max(1, category.getDisplayWidth(display)),
                    Math.max(1, category.getDisplayHeight()),
                    new ReiCategory(config.get())
                )
            );
        }
        return List.copyOf(categories.values());
    }

    @Override
    public List<RecipeBrowserRecipe> recipes(RecipeBrowserEntry entry, RecipeBrowserMode mode, RecipeBrowserCategory category) {
        List<RecipeBrowserRecipe> recipes = new ArrayList<>();
        int index = 0;
        for (DisplaySpec spec : this.displays(entry, mode)) {
            Display display = spec.provideInternalDisplay();
            if (category != null && !Objects.equals(category.uid(), display.getCategoryIdentifier().getIdentifier().toString())) {
                continue;
            }
            Optional<CategoryRegistry.CategoryConfiguration<Display>> config = this.categoryConfig(display.getCategoryIdentifier());
            if (config.isEmpty()) {
                continue;
            }
            DisplayCategory<Display> displayCategory = config.get().getCategory();
            recipes.add(new RecipeBrowserRecipe(
                this.recipeUid(spec, display, index++),
                display.getCategoryIdentifier().getIdentifier().toString(),
                Math.max(1, displayCategory.getDisplayWidth(display)),
                Math.max(1, displayCategory.getDisplayHeight()),
                display,
                new ReiRecipe(config.get())
            ));
        }
        return recipes;
    }

    @Override
    public List<RecipeBrowserEntry> craftingStations(RecipeBrowserCategory category) {
        ReiCategory reiCategory = this.reiCategory(category);
        if (reiCategory == null) {
            return List.of();
        }
        List<RecipeBrowserEntry> stations = new ArrayList<>();
        for (EntryIngredient ingredient : reiCategory.config().getWorkstations()) {
            for (EntryStack<?> stack : ingredient) {
                if (stack != null && !stack.isEmpty()) {
                    stations.add(this.wrap(stack));
                }
            }
        }
        return stations;
    }

    @Override
    public void renderRecipeCategoryIcon(GuiGraphicsExtractor graphics, RecipeBrowserCategory category, int x, int y) {
        ReiCategory reiCategory = this.reiCategory(category);
        if (reiCategory != null) {
            reiCategory.config().getCategory().getIcon().render(graphics.unwrap(), new Rectangle(x, y, 16, 16), x, y, 0.0F);
        }
    }

    @Override
    public void renderRecipe(GuiGraphicsExtractor graphics, RecipeBrowserRecipe recipe, int x, int y, int mouseX, int mouseY) {
        for (Widget widget : this.widgets(recipe, x, y)) {
            Rectangle bounds = widget instanceof WidgetWithBounds bounded
                ? bounded.getBounds()
                : new Rectangle(x, y, Math.max(1, recipe.width()), Math.max(1, recipe.height()));
            widget.render(graphics.unwrap(), bounds, mouseX, mouseY, 0.0F);
        }
    }

    @Override
    public void renderRecipeOverlays(GuiGraphicsExtractor graphics, RecipeBrowserRecipe recipe, int x, int y, int mouseX, int mouseY) {
    }

    @Override
    public void renderRecipeSlotHighlights(GuiGraphicsExtractor graphics, RecipeBrowserRecipe recipe, int x, int y, List<Integer> inputIndexes, int color) {
        int inputIndex = 0;
        for (Widget widget : this.widgets(recipe, x, y)) {
            if (!(widget instanceof Slot slot) || slot.getNoticeMark() != Slot.INPUT) {
                continue;
            }
            if (inputIndexes.contains(inputIndex)) {
                Rectangle bounds = slot.getInnerBounds();
                graphics.fill(bounds.x, bounds.y, bounds.getMaxX(), bounds.getMaxY(), color);
            }
            inputIndex++;
        }
    }

    @Override
    public void tickRecipe(RecipeBrowserRecipe recipe) {
    }

    @Override
    public boolean canBookmarkRecipe(RecipeBrowserRecipe recipe) {
        return this.recipeFavoriteOutput(this.display(recipe)) != null;
    }

    @Override
    public boolean isRecipeBookmarked(RecipeBrowserRecipe recipe) {
        EntryStack<?> output = this.recipeFavoriteOutput(this.display(recipe));
        return output != null && this.findEntryFavorite(output) != null;
    }

    @Override
    public void toggleRecipeBookmark(RecipeBrowserRecipe recipe) {
        Display display = this.display(recipe);
        EntryStack<?> output = this.recipeFavoriteOutput(display);
        if (display == null || output == null) {
            return;
        }

        List<FavoriteEntry> favorites = ConfigObject.getInstance().getFavoriteEntries();
        FavoriteEntry existing = this.findEntryFavorite(output);
        FavoriteEntry legacyDisplayFavorite = this.findDisplayFavorite(display);
        boolean changed = false;
        if (existing != null) {
            changed = favorites.remove(existing);
        } else {
            FavoriteEntry favorite = FavoriteEntry.fromEntryStack(output);
            if (!favorite.isInvalid()) {
                changed = favorites.add(favorite);
            }
        }
        if (legacyDisplayFavorite != null) {
            changed |= favorites.remove(legacyDisplayFavorite);
        }
        if (changed) {
            ConfigManager.getInstance().saveConfig();
        }
    }

    @Override
    public @Nullable RecipeBrowserView expandedViewAt(RecipeBrowserRecipe recipe, int x, int y, double mouseX, double mouseY) {
        Display display = this.display(recipe);
        if (!(display instanceof DefaultTagDisplay<?, ?> tagDisplay)
            || !new Rectangle(x + 5, y + 6, 13, 13).contains(mouseX, mouseY)) {
            return null;
        }
        return new ReiTagTreeView(tagDisplay);
    }

    @Override
    public boolean isRecipeSortStageEnabled(RecipeBrowserSortStage stage) {
        return false;
    }

    @Override
    public void toggleRecipeSortStage(RecipeBrowserSortStage stage) {
    }

    @Override
    public @Nullable RecipeBrowserTransferPlan recipeTransferPlan(RecipeBrowserRecipe recipe, AbstractContainerMenu menu) {
        Display display = this.display(recipe);
        if (!(display instanceof CraftingDisplay craftingDisplay)) {
            return null;
        }

        ReiCraftingGrid grid = ReiCraftingGrid.forMenu(menu);
        if (grid == null || craftingDisplay.getWidth() > grid.width() || craftingDisplay.getHeight() > grid.height()) {
            return null;
        }

        List<InputIngredient<EntryStack<?>>> inputs = craftingDisplay.getInputIngredients(3, 3);
        if (inputs.size() != 9 || grid.slotStart() + grid.slotCount() > menu.slots.size()) {
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
        for (int displayIndex = 0; displayIndex < inputs.size(); displayIndex++) {
            InputIngredient<EntryStack<?>> input = inputs.get(displayIndex);
            if (input.get().isEmpty()) {
                continue;
            }
            int gridIndex;
            if (craftingDisplay.isShapeless()) {
                gridIndex = packedShapelessIndex++;
            } else {
                gridIndex = displayIndex % 3 + displayIndex / 3 * grid.width();
            }
            if (gridIndex < 0 || gridIndex >= grid.slotCount()) {
                return null;
            }
            List<ItemStack> alternatives = itemAlternatives(input);
            if (alternatives.isEmpty()) {
                return null;
            }
            int targetSlotId = menu.getSlot(grid.slotStart() + gridIndex).index;
            requirements.add(new RecipeBrowserTransferSlot(displayIndex, targetSlotId, alternatives));
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

    private static List<ItemStack> itemAlternatives(InputIngredient<EntryStack<?>> input) {
        List<ItemStack> alternatives = new ArrayList<>();
        for (EntryStack<?> entry : input.get()) {
            if (entry == null || entry.isEmpty() || !VanillaEntryTypes.ITEM.equals(entry.getType())) {
                continue;
            }
            ItemStack stack = entry.castValue();
            if (stack == null || stack.isEmpty()) {
                continue;
            }
            ItemStack alternative = stack.copyWithCount(Math.max(1, stack.getCount()));
            boolean duplicate = alternatives.stream().anyMatch(existing ->
                existing.getCount() == alternative.getCount() && ItemStack.isSameItemSameComponents(existing, alternative)
            );
            if (!duplicate) {
                alternatives.add(alternative);
                if (alternatives.size() >= MAX_TRANSFER_ALTERNATIVES) {
                    break;
                }
            }
        }
        return alternatives;
    }

    @Override
    public @Nullable RecipeBrowserEntry recipeIngredientAt(RecipeBrowserRecipe recipe, int x, int y, int mouseX, int mouseY) {
        for (Widget widget : this.widgets(recipe, x, y)) {
            if (widget instanceof Slot slot && slot.getInnerBounds().contains(mouseX, mouseY)) {
                EntryStack<?> stack = slot.getCurrentEntry();
                if (stack != null && !stack.isEmpty()) {
                    return this.wrap(stack);
                }
            }
        }
        return null;
    }

    @Override
    public boolean handleRecipeMouseScrolled(RecipeBrowserRecipe recipe, int x, int y, double mouseX, double mouseY, double scrollX, double scrollY) {
        for (Widget widget : this.widgets(recipe, x, y)) {
            if (widget.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean handleRecipeMouseClicked(RecipeBrowserRecipe recipe, int x, int y, MouseButtonEvent event, boolean doubleClick) {
        for (Widget widget : this.widgets(recipe, x, y)) {
            if (widget.mouseClicked(event, doubleClick)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean handleRecipeMouseReleased(RecipeBrowserRecipe recipe, int x, int y, MouseButtonEvent event) {
        for (Widget widget : this.widgets(recipe, x, y)) {
            if (widget.mouseReleased(event)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean handleRecipeMouseDragged(RecipeBrowserRecipe recipe, int x, int y, MouseButtonEvent event, double dragX, double dragY) {
        for (Widget widget : this.widgets(recipe, x, y)) {
            if (widget.mouseDragged(event, dragX, dragY)) {
                return true;
            }
        }
        return false;
    }

    private List<EntryStack<?>> entryStacks() {
        try {
            return EntryRegistry.getInstance().getPreFilteredList();
        } catch (RuntimeException | LinkageError ignored) {
            try {
                return EntryRegistry.getInstance().getEntryStacks().toList();
            } catch (RuntimeException | LinkageError ignoredAgain) {
                return List.of();
            }
        }
    }

    private List<RecipeBrowserEntry> favoriteEntries() {
        List<RecipeBrowserEntry> entries = new ArrayList<>();
        try {
            for (FavoriteEntry favorite : ConfigObject.getInstance().getFavoriteEntries()) {
                if (favorite == null || favorite.isInvalid() || isDisplayFavorite(favorite)) {
                    continue;
                }
                EntryStack<?> stack = favorite.toStack();
                if (stack != null && !stack.isEmpty()) {
                    entries.add(this.wrap(stack));
                }
            }
        } catch (RuntimeException | LinkageError ignored) {
        }
        return entries;
    }

    private List<RecipeBrowserEntry> recentEntries() {
        return List.copyOf(this.recentEntries);
    }

    private List<RecipeBrowserEntry> filterEntries(List<RecipeBrowserEntry> entries) {
        SearchFilter filter = this.searchFilter();
        List<RecipeBrowserEntry> filtered = new ArrayList<>(entries.size());
        for (RecipeBrowserEntry entry : entries) {
            EntryStack<?> stack = this.entryStack(entry);
            if (stack != null && filter.test(stack)) {
                filtered.add(entry);
            }
        }
        return filtered;
    }

    private SearchFilter searchFilter() {
        String text = this.filterText == null ? "" : this.filterText.trim();
        if (text.isEmpty()) {
            return SearchFilter.matchAll();
        }
        try {
            return SearchProvider.getInstance().createFilter(text);
        } catch (RuntimeException | LinkageError ignored) {
            return SearchFilter.matchAll();
        }
    }

    private List<DisplaySpec> displays(RecipeBrowserEntry entry, RecipeBrowserMode mode) {
        EntryStack<?> stack = this.entryStack(entry);
        if (stack == null || stack.isEmpty() || mode == null || mode == RecipeBrowserMode.INGREDIENTS) {
            return List.of();
        }
        ViewSearchBuilder builder = ViewSearchBuilder.builder()
            .processingVisibilityHandlers(true)
            .mergingDisplays(true);
        if (mode == RecipeBrowserMode.RECIPES) {
            builder.addRecipesFor(stack);
        } else {
            builder.addUsagesFor(stack);
        }
        return builder.streamDisplays()
            .filter(spec -> this.displayMatches(spec.provideInternalDisplay(), stack, mode))
            .sorted(Comparator.comparing(spec -> spec.provideInternalDisplay().getCategoryIdentifier().getIdentifier().toString()))
            .toList();
    }

    private boolean displayMatches(Display display, EntryStack<?> focus, RecipeBrowserMode mode) {
        List<EntryIngredient> ingredients = mode == RecipeBrowserMode.RECIPES ? display.getOutputEntries() : display.getInputEntries();
        for (EntryIngredient ingredient : ingredients) {
            for (EntryStack<?> candidate : ingredient) {
                if (this.sameStack(candidate, focus)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean sameStack(EntryStack<?> left, EntryStack<?> right) {
        if (left == null || right == null || left.isEmpty() || right.isEmpty()) {
            return false;
        }
        try {
            return EntryStacks.equalsFuzzy(left, right) || EntryStacks.equalsExact(left, right);
        } catch (RuntimeException | LinkageError ignored) {
            return Objects.equals(left.getIdentifier(), right.getIdentifier())
                && Objects.equals(left.getType(), right.getType());
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Optional<CategoryRegistry.CategoryConfiguration<Display>> categoryConfig(CategoryIdentifier<?> category) {
        return CategoryRegistry.getInstance()
            .tryGet((CategoryIdentifier) category)
            .map(config -> (CategoryRegistry.CategoryConfiguration<Display>) config);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private List<Widget> widgets(RecipeBrowserRecipe recipe, int x, int y) {
        if (recipe == null || !(recipe.recipe() instanceof Display display)) {
            return List.of();
        }
        CategoryRegistry.CategoryConfiguration<Display> config = recipe.layout() instanceof ReiRecipe reiRecipe
            ? reiRecipe.config()
            : this.categoryConfig(display.getCategoryIdentifier()).orElse(null);
        if (config == null) {
            return List.of();
        }
        DisplayCategoryView<Display> view = config.getView(display);
        return view.setupDisplay(display, new Rectangle(x, y, Math.max(1, recipe.width()), Math.max(1, recipe.height())));
    }

    private @Nullable EntryStack<?> entryStack(RecipeBrowserEntry entry) {
        return entry != null && entry.ingredient() instanceof EntryStack<?> stack ? stack : null;
    }

    private @Nullable Display display(RecipeBrowserRecipe recipe) {
        return recipe != null && recipe.recipe() instanceof Display display ? display : null;
    }

    private @Nullable EntryStack<?> recipeFavoriteOutput(@Nullable Display display) {
        if (display == null) {
            return null;
        }

        EntryStack<?> fallback = null;
        for (EntryIngredient ingredient : display.getOutputEntries()) {
            for (EntryStack<?> stack : ingredient) {
                if (stack == null || stack.isEmpty() || !stack.supportSerialization()) {
                    continue;
                }
                if (VanillaEntryTypes.ITEM.equals(stack.getType())) {
                    return stack.copy();
                }
                if (fallback == null) {
                    fallback = stack.copy();
                }
            }
        }
        return fallback;
    }

    private @Nullable FavoriteEntry findEntryFavorite(EntryStack<?> stack) {
        try {
            FavoriteEntry candidate = FavoriteEntry.fromEntryStack(stack).getUnwrapped();
            if (candidate == null || candidate.isInvalid() || !FavoriteEntryType.ENTRY_STACK.equals(candidate.getType())) {
                return null;
            }

            for (FavoriteEntry favorite : ConfigObject.getInstance().getFavoriteEntries()) {
                FavoriteEntry unwrapped = favorite == null ? null : favorite.getUnwrapped();
                if (unwrapped != null
                    && !unwrapped.isInvalid()
                    && FavoriteEntryType.ENTRY_STACK.equals(unwrapped.getType())
                    && candidate.isSame(unwrapped)) {
                    return favorite;
                }
            }
        } catch (RuntimeException | LinkageError ignored) {
        }
        return null;
    }

    private @Nullable FavoriteEntry findDisplayFavorite(Display display) {
        FavoriteEntry candidate;
        CompoundTag candidateFingerprint;
        try {
            candidate = FavoriteEntry.fromDisplay(display);
            candidateFingerprint = favoriteFingerprint(candidate);
        } catch (RuntimeException | LinkageError exception) {
            return null;
        }
        if (candidateFingerprint == null) {
            return null;
        }

        for (FavoriteEntry favorite : ConfigObject.getInstance().getFavoriteEntries()) {
            CompoundTag fingerprint = favoriteFingerprint(favorite);
            if (candidateFingerprint.equals(fingerprint)) {
                return favorite;
            }
        }
        return null;
    }

    private static boolean isDisplayFavorite(FavoriteEntry favorite) {
        try {
            FavoriteEntry unwrapped = favorite.getUnwrapped();
            return unwrapped != null
                && !unwrapped.isInvalid()
                && FavoriteEntryType.DISPLAY.equals(unwrapped.getType());
        } catch (RuntimeException | LinkageError exception) {
            return false;
        }
    }

    private static @Nullable CompoundTag favoriteFingerprint(FavoriteEntry favorite) {
        try {
            FavoriteEntry unwrapped = favorite == null ? null : favorite.getUnwrapped();
            if (unwrapped == null || unwrapped.isInvalid() || !FavoriteEntryType.DISPLAY.equals(unwrapped.getType())) {
                return null;
            }
            CompoundTag tag = unwrapped.save(new CompoundTag());
            tag.remove("UUID");
            return tag;
        } catch (RuntimeException | LinkageError exception) {
            return null;
        }
    }

    private RecipeBrowserEntry wrap(EntryStack<?> stack) {
        EntryStack<?> copy = stack.copy();
        return new RecipeBrowserEntry(typeUid(copy), copy.getType(), copy, stack);
    }

    private boolean sameEntry(RecipeBrowserEntry left, RecipeBrowserEntry right) {
        EntryStack<?> leftStack = this.entryStack(left);
        EntryStack<?> rightStack = this.entryStack(right);
        if (leftStack == null || rightStack == null) {
            return false;
        }
        try {
            return EntryStacks.equalsExact(leftStack, rightStack);
        } catch (RuntimeException | LinkageError ignored) {
            return Objects.equals(leftStack.getIdentifier(), rightStack.getIdentifier())
                && Objects.equals(leftStack.getType(), rightStack.getType());
        }
    }

    private @Nullable ReiCategory reiCategory(RecipeBrowserCategory category) {
        return category != null && category.category() instanceof ReiCategory reiCategory ? reiCategory : null;
    }

    private String recipeUid(DisplaySpec spec, Display display, int index) {
        return spec.provideInternalDisplayIds().stream()
            .findFirst()
            .map(Identifier::toString)
            .orElse(display.getCategoryIdentifier().getIdentifier() + "#" + index);
    }

    private static String typeUid(EntryStack<?> stack) {
        return stack.getType().getId().toString();
    }

    private static String titleFor(Identifier id) {
        String path = id.getPath().replace('_', ' ');
        StringBuilder builder = new StringBuilder(path.length());
        boolean uppercase = true;
        for (int i = 0; i < path.length(); i++) {
            char c = path.charAt(i);
            if (Character.isWhitespace(c) || c == '/' || c == '.') {
                builder.append(' ');
                uppercase = true;
            } else if (uppercase) {
                builder.append(String.valueOf(c).toUpperCase(Locale.ROOT));
                uppercase = false;
            } else {
                builder.append(c);
            }
        }
        return builder.toString();
    }

    private record ReiCategory(CategoryRegistry.CategoryConfiguration<Display> config) {
    }

    private record ReiRecipe(CategoryRegistry.CategoryConfiguration<Display> config) {
    }

    private record ReiCraftingGrid(int width, int height, int slotStart, int slotCount) {
        private static @Nullable ReiCraftingGrid forMenu(AbstractContainerMenu menu) {
            if (menu instanceof CraftingMenu) {
                return new ReiCraftingGrid(3, 3, 1, 9);
            }
            if (menu instanceof InventoryMenu) {
                return new ReiCraftingGrid(2, 2, 1, 4);
            }
            if (menu instanceof CrafterMenu) {
                return new ReiCraftingGrid(3, 3, 0, 9);
            }
            return null;
        }
    }

    private record ReiTreeHit(@Nullable RecipeBrowserEntry entry, List<Component> tooltip) {
    }

    private record ReiTreeLayout(
        TagNode<Object> node,
        List<Holder<Object>> values,
        List<ReiTreeLayout> children,
        int columns,
        int rootWidth,
        int rootHeight,
        int width,
        int height
    ) {
        private static ReiTreeLayout create(TagNode<Object> node) {
            List<Holder<Object>> values = node.getValue() == null ? List.of() : node.getValue().stream().toList();
            List<ReiTreeLayout> children = node.children().stream().map(ReiTreeLayout::create).toList();
            boolean reference = node.getReference() != null;
            int columns = reference ? 0 : Math.max(1, Math.min(4, values.size()));
            int rows = reference ? 0 : Math.max(1, (values.size() + columns - 1) / columns);
            int rootWidth = reference ? 24 : columns * 16 + 12;
            int rootHeight = reference ? 23 : rows * 16 + 12;
            int childrenWidth = children.isEmpty() ? 0 : children.stream().mapToInt(child -> child.width + 6).sum() - 6;
            int width = Math.max(rootWidth, childrenWidth);
            int height = children.isEmpty()
                ? rootHeight
                : rootHeight + 16 + children.stream().mapToInt(ReiTreeLayout::height).max().orElse(0);
            return new ReiTreeLayout(node, values, children, columns, rootWidth, rootHeight, width, height);
        }

        private int childrenWidth() {
            return this.children.isEmpty() ? 0 : this.children.stream().mapToInt(child -> child.width + 6).sum() - 6;
        }
    }

    private final class ReiTagTreeView implements RecipeBrowserView {
        private static final double MIN_ZOOM = 0.15D;
        private static final double MAX_ZOOM = 4.0D;
        private static final double ZOOM_FACTOR = 1.2D;
        private static final double MIN_VISIBLE_TREE_SIZE = 16.0D;
        private static final int TREE_PADDING = 16;
        private final Rectangle bounds = new Rectangle();
        private final Function<Holder<Object>, EntryStack<Object>> mapper;
        private volatile @Nullable TagNode<Object> node;
        private volatile @Nullable Component error;
        private @Nullable ReiTreeLayout layout;
        private @Nullable WidgetWithBounds content;
        private @Nullable WidgetWithBounds widget;
        private double zoom = 1.0D;
        private double panX;
        private double panY;
        private double lastMouseX;
        private double lastMouseY;
        private boolean panning;

        @SuppressWarnings({"unchecked", "rawtypes"})
        private ReiTagTreeView(DefaultTagDisplay<?, ?> display) {
            this.mapper = (Function) display.getMapper();
            TagNodes.create(display.getKey(), result -> {
                if (result.error().isPresent()) {
                    this.error = Component.literal(result.error().get().message());
                } else {
                    this.node = (TagNode) result.result().orElse(null);
                }
            });
        }

        @Override
        public Component title() {
            return Component.literal("Tag Tree");
        }

        @Override
        public void render(GuiGraphicsExtractor graphics, int x, int y, int width, int height, int mouseX, int mouseY, float tickProgress) {
            this.bounds.setBounds(x, y, Math.max(1, width), Math.max(1, height));
            this.ensureWidget();
            if (this.widget != null) {
                this.clampPan();
                this.widget.render(graphics.unwrap(), this.widget.getBounds(), mouseX, mouseY, tickProgress);
            } else {
                Component message = this.error == null ? Component.literal("Loading tree...") : this.error;
                graphics.text(Minecraft.getInstance().font, message, x + 4, y + 4, 0xFFAAAAAA, false);
            }
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
            if (this.widget == null
                || event.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT
                || !this.bounds.contains(event.x(), event.y())) {
                return false;
            }
            this.panning = true;
            this.lastMouseX = event.x();
            this.lastMouseY = event.y();
            return true;
        }

        @Override
        public boolean mouseReleased(MouseButtonEvent event) {
            boolean wasPanning = this.panning;
            this.panning = false;
            return wasPanning;
        }

        @Override
        public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
            if (!this.panning || this.widget == null) {
                return false;
            }
            this.panX += event.x() - this.lastMouseX;
            this.panY += event.y() - this.lastMouseY;
            this.lastMouseX = event.x();
            this.lastMouseY = event.y();
            this.clampPan();
            return true;
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
            if (this.widget == null || scrollY == 0.0D || !this.bounds.contains(mouseX, mouseY)) {
                return false;
            }

            double previousZoom = this.zoom;
            double nextZoom = clamp(previousZoom * Math.pow(ZOOM_FACTOR, scrollY), MIN_ZOOM, MAX_ZOOM);
            if (nextZoom == previousZoom) {
                return true;
            }

            double ratio = nextZoom / previousZoom;
            double relativeMouseX = mouseX - this.bounds.getCenterX();
            double relativeMouseY = mouseY - this.bounds.getCenterY();
            this.panX = relativeMouseX - ratio * (relativeMouseX - this.panX);
            this.panY = relativeMouseY - ratio * (relativeMouseY - this.panY);
            this.zoom = nextZoom;
            this.clampPan();
            return true;
        }

        @Override
        public @Nullable RecipeBrowserEntry entryAt(double mouseX, double mouseY) {
            ReiTreeHit hit = this.hitAt(mouseX, mouseY);
            return hit == null ? null : hit.entry();
        }

        @Override
        public List<Component> tooltipAt(double mouseX, double mouseY) {
            ReiTreeHit hit = this.hitAt(mouseX, mouseY);
            return hit == null ? List.of() : hit.tooltip();
        }

        private void ensureWidget() {
            TagNode<Object> resolvedNode = this.node;
            if (this.widget == null && resolvedNode != null && this.bounds.width > 0 && this.bounds.height > 0) {
                TagTreeWidget<Object, Object> tree = new TagTreeWidget<>(resolvedNode, this.mapper, this.bounds);
                this.layout = ReiTreeLayout.create(resolvedNode);
                this.content = Widgets.padded(TREE_PADDING, tree);
                this.zoom = this.fittedZoom();
                this.widget = Widgets.scissored(this.bounds, Widgets.withTranslate(this.content, this::viewTransform));
            }
        }

        private Matrix3x2f viewTransform() {
            Rectangle contentBounds = this.contentBounds();
            return new Matrix3x2f()
                .translate((float) (this.bounds.getCenterX() + this.panX), (float) (this.bounds.getCenterY() + this.panY))
                .scale((float) this.zoom)
                .translate(-contentBounds.getCenterX(), -contentBounds.getCenterY());
        }

        private double fittedZoom() {
            Rectangle contentBounds = this.contentBounds();
            double widthScale = this.bounds.width / (double) Math.max(1, contentBounds.width);
            double heightScale = this.bounds.height / (double) Math.max(1, contentBounds.height);
            return clamp(Math.min(1.0D, Math.min(widthScale, heightScale)), MIN_ZOOM, MAX_ZOOM);
        }

        private void clampPan() {
            Rectangle contentBounds = this.contentBounds();
            double maxPanX = Math.max(0.0D, this.bounds.width / 2.0D + contentBounds.width * this.zoom / 2.0D - MIN_VISIBLE_TREE_SIZE);
            double maxPanY = Math.max(0.0D, this.bounds.height / 2.0D + contentBounds.height * this.zoom / 2.0D - MIN_VISIBLE_TREE_SIZE);
            this.panX = clamp(this.panX, -maxPanX, maxPanX);
            this.panY = clamp(this.panY, -maxPanY, maxPanY);
        }

        private Rectangle contentBounds() {
            return this.content == null ? new Rectangle() : this.content.getBounds();
        }

        private @Nullable ReiTreeHit hitAt(double mouseX, double mouseY) {
            ReiTreeLayout resolvedLayout = this.layout;
            if (this.widget == null || resolvedLayout == null || !this.bounds.contains(mouseX, mouseY)) {
                return null;
            }

            Rectangle contentBounds = this.contentBounds();
            double localX = contentBounds.getCenterX() + (mouseX - this.bounds.getCenterX() - this.panX) / this.zoom;
            double localY = contentBounds.getCenterY() + (mouseY - this.bounds.getCenterY() - this.panY) / this.zoom;
            return this.hitNode(
                resolvedLayout,
                localX,
                localY,
                TREE_PADDING,
                TREE_PADDING
            );
        }

        private @Nullable ReiTreeHit hitNode(ReiTreeLayout tree, double mouseX, double mouseY, int x, int y) {
            if (!contains(mouseX, mouseY, x, y, tree.width(), tree.height())) {
                return null;
            }
            int rootX = x + tree.width() / 2 - tree.rootWidth() / 2;
            if (tree.node().getReference() != null && contains(mouseX, mouseY, rootX, y, tree.rootWidth(), tree.rootHeight())) {
                return new ReiTreeHit(null, List.of(Component.literal("#" + tree.node().getReference().location())));
            }

            for (int i = 0; i < tree.values().size(); i++) {
                int itemX = rootX + i % tree.columns() * 16 + 6;
                int itemY = y + i / tree.columns() * 16 + 6;
                if (!contains(mouseX, mouseY, itemX, itemY, 16, 16)) {
                    continue;
                }
                EntryStack<?> stack = this.mapper.apply(tree.values().get(i));
                if (stack == null || stack.isEmpty()) {
                    return null;
                }
                RecipeBrowserEntry entry = RuntimeReiRecipeBrowserAccess.this.wrap(stack);
                return new ReiTreeHit(entry, RuntimeReiRecipeBrowserAccess.this.tooltip(entry));
            }

            int childrenX = x + tree.width() / 2 - tree.childrenWidth() / 2;
            int childY = y + tree.rootHeight() + 16;
            for (ReiTreeLayout child : tree.children()) {
                ReiTreeHit hit = this.hitNode(child, mouseX, mouseY, childrenX, childY);
                if (hit != null) {
                    return hit;
                }
                childrenX += child.width() + 6;
            }
            return null;
        }

        private static boolean contains(double x, double y, int left, int top, int width, int height) {
            return x >= left && x < left + width && y >= top && y < top + height;
        }

        private static double clamp(double value, double minimum, double maximum) {
            return Math.max(minimum, Math.min(maximum, value));
        }
    }
}
