package com.salts_inventory_update.compat.jei;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import com.mojang.blaze3d.platform.InputConstants;
import com.salts_inventory_update.debug.DesktopDebug;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.IRecipeLayoutDrawable;
import mezz.jei.api.gui.inputs.IJeiInputHandler;
import mezz.jei.api.gui.inputs.IJeiUserInput;
import mezz.jei.api.gui.ingredient.IRecipeSlotDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.ingredients.IIngredientRenderer;
import mezz.jei.api.ingredients.IIngredientType;
import mezz.jei.api.recipe.IFocus;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.IRecipeManager;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.api.recipe.transfer.IRecipeTransferInfo;
import mezz.jei.api.runtime.IJeiKeyMapping;
import mezz.jei.api.runtime.IIngredientFilter;
import mezz.jei.api.runtime.IIngredientManager;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import com.salts_inventory_update.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CrafterMenu;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jspecify.annotations.Nullable;

final class RuntimeJeiDesktopAccess implements JeiDesktopAccess {
    private static final String FAVORITES_TAB_UID = "salts_inventory_update:jei_favorites";
    private static final String RECENT_TAB_UID = "salts_inventory_update:jei_recent";
    private static final boolean JEI_TRANSFER_DEBUG = Boolean.getBoolean("salts_inventory_update.jeiTransferDebug");
    private final IJeiRuntime runtime;
    private String lastTransferDebugKey = "";
    private long lastTransferDebugAt;

    RuntimeJeiDesktopAccess(IJeiRuntime runtime) {
        this.runtime = runtime;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String filterText() {
        return this.filter().getFilterText();
    }

    @Override
    public void setFilterText(String text) {
        this.filter().setFilterText(text == null ? "" : text);
    }

    @Override
    public List<JeiDesktopTab> tabs() {
        List<IIngredientType<?>> types = new ArrayList<>(this.manager().getRegisteredIngredientTypes());
        types.sort(Comparator.comparing(IIngredientType::getUid));
        List<JeiDesktopTab> tabs = new ArrayList<>();
        tabs.add(new JeiDesktopTab(FAVORITES_TAB_UID, Component.literal("Favorites"), null, JeiDesktopTabKind.FAVORITES));
        tabs.add(new JeiDesktopTab(RECENT_TAB_UID, Component.literal("Recent"), null, JeiDesktopTabKind.RECENT));
        for (IIngredientType<?> type : types) {
            if (!this.hasIngredients(type)) {
                continue;
            }
            String uid = type.getUid();
            tabs.add(new JeiDesktopTab(uid, Component.literal(this.titleFor(type)), this.tabIcon(type)));
        }
        return tabs;
    }

    @Override
    public List<JeiDesktopEntry> filteredEntries(JeiDesktopTab tab) {
        if (tab != null && tab.kind() == JeiDesktopTabKind.FAVORITES) {
            return this.elementEntries(this.bookmarkList());
        }
        if (tab != null && tab.kind() == JeiDesktopTabKind.RECENT) {
            return this.elementEntries(this.lookupHistory());
        }
        IIngredientType<?> type = this.type(tab);
        if (type == null) {
            return List.of();
        }
        return this.filteredEntries(type);
    }

    @Override
    public void renderTabIcon(GuiGraphicsExtractor graphics, JeiDesktopTab tab, int x, int y) {
        JeiDesktopEntry icon = tab == null ? null : tab.icon();
        if (icon != null) {
            this.renderUnchecked(graphics, icon, x, y);
        }
    }

    @Override
    public void render(GuiGraphicsExtractor graphics, JeiDesktopEntry entry, int x, int y) {
        this.renderUnchecked(graphics, entry, x, y);
    }

    @Override
    public List<Component> tooltip(JeiDesktopEntry entry) {
        return this.tooltipUnchecked(entry);
    }

    @Override
    public JeiDesktopEntry entryForItemStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        return new JeiDesktopEntry(VanillaTypes.ITEM_STACK.getUid(), VanillaTypes.ITEM_STACK, stack.copy());
    }

    @Override
    public boolean matchesRecipeKey(int key) {
        return this.matchesJeiKey(this.runtime.getKeyMappings().getShowRecipe(), key);
    }

    @Override
    public boolean matchesUsesKey(int key) {
        return this.matchesJeiKey(this.runtime.getKeyMappings().getShowUses(), key);
    }

    @Override
    public void addLookupHistory(JeiDesktopEntry entry) {
        Object lookupHistory = this.lookupHistory();
        Object bookmark = this.createIngredientBookmark(entry);
        if (lookupHistory != null && bookmark != null) {
            this.invoke(lookupHistory, "add", new Class<?>[] { this.classForName("mezz.jei.gui.bookmarks.IBookmark") }, bookmark);
        }
    }

    @Override
    public List<JeiRecipeCategory> recipeCategories(JeiDesktopEntry entry, JeiRecipeMode mode) {
        IFocus<?> focus = this.focus(entry, mode);
        if (focus == null) {
            return List.of();
        }
        return this.recipeManager()
            .createRecipeCategoryLookup()
            .limitFocus(List.of(focus))
            .get()
            .map(this::wrapCategory)
            .toList();
    }

    @Override
    public List<JeiRecipeEntry> recipes(JeiDesktopEntry entry, JeiRecipeMode mode, JeiRecipeCategory category) {
        IFocus<?> focus = this.focus(entry, mode);
        if (focus == null) {
            return List.of();
        }
        IFocusGroup focusGroup = this.runtime.getJeiHelpers().getFocusFactory().createFocusGroup(List.of(focus));
        return this.recipesUnchecked(focus, focusGroup, category);
    }

    @Override
    @SuppressWarnings("removal")
    public List<JeiDesktopEntry> craftingStations(JeiRecipeCategory category) {
        IRecipeCategory<?> recipeCategory = this.castCategory(category.category());
        List<JeiDesktopEntry> stations = this.recipeManager()
            .createCraftingStationLookup(recipeCategory.getRecipeType())
            .get()
            .map(this::wrapTypedIngredient)
            .toList();
        if (!stations.isEmpty()) {
            return stations;
        }
        return this.recipeManager()
            .createRecipeCatalystLookup(recipeCategory.getRecipeType())
            .get()
            .map(this::wrapTypedIngredient)
            .toList();
    }

    @Override
    public void renderRecipeCategoryIcon(GuiGraphicsExtractor graphics, JeiRecipeCategory category, int x, int y) {
        this.castCategory(category.category()).getIcon().draw(graphics.unwrap(), x, y);
    }

    @Override
    public void renderRecipe(GuiGraphicsExtractor graphics, JeiRecipeEntry recipe, int x, int y, int mouseX, int mouseY) {
        IRecipeLayoutDrawable<?> layout = this.castRecipeLayout(recipe.layout());
        layout.setPosition(x, y);
        layout.drawRecipe(graphics.unwrap(), mouseX, mouseY);
    }

    @Override
    public void renderRecipeOverlays(GuiGraphicsExtractor graphics, JeiRecipeEntry recipe, int x, int y, int mouseX, int mouseY) {
        IRecipeLayoutDrawable<?> layout = this.castRecipeLayout(recipe.layout());
        layout.setPosition(x, y);
        layout.drawOverlays(graphics.unwrap(), mouseX, mouseY);
    }

    @Override
    public void renderRecipeSlotHighlights(GuiGraphicsExtractor graphics, JeiRecipeEntry recipe, int x, int y, List<Integer> inputIndexes, int color) {
        if (inputIndexes.isEmpty()) {
            return;
        }
        IRecipeLayoutDrawable<?> layout = this.castRecipeLayout(recipe.layout());
        layout.setPosition(x, y);
        List<IRecipeSlotView> inputSlots = layout.getRecipeSlotsView().getSlotViews(RecipeIngredientRole.INPUT);
        for (int inputIndex : inputIndexes) {
            if (inputIndex >= 0 && inputIndex < inputSlots.size()) {
                inputSlots.get(inputIndex).drawHighlight(graphics.unwrap(), color);
            }
        }
    }

    @Override
    public void tickRecipe(JeiRecipeEntry recipe) {
        this.castRecipeLayout(recipe.layout()).tick();
    }

    @Override
    public boolean canBookmarkRecipe(JeiRecipeEntry recipe) {
        return this.createRecipeBookmark(recipe) != null;
    }

    @Override
    public boolean isRecipeBookmarked(JeiRecipeEntry recipe) {
        Object bookmarkList = this.bookmarkList();
        Object bookmark = this.createRecipeBookmark(recipe);
        return bookmarkList != null && bookmark != null && this.invokeBoolean(bookmarkList, "contains", false, this.classForName("mezz.jei.gui.bookmarks.IBookmark"), bookmark);
    }

    @Override
    public void toggleRecipeBookmark(JeiRecipeEntry recipe) {
        Object bookmarkList = this.bookmarkList();
        Object bookmark = this.createRecipeBookmark(recipe);
        if (bookmarkList != null && bookmark != null) {
            this.invoke(bookmarkList, "toggleBookmark", new Class<?>[] { this.classForName("mezz.jei.gui.bookmarks.IBookmark") }, bookmark);
        }
    }

    @Override
    public boolean isRecipeSortStageEnabled(JeiRecipeSortStage stage) {
        Object recipeSorterStage = this.recipeSorterStage(stage);
        Object sorterStages = this.invoke(this.clientConfig(), "getRecipeSorterStages");
        return recipeSorterStage != null && sorterStages instanceof Collection<?> collection && collection.contains(recipeSorterStage);
    }

    @Override
    public void toggleRecipeSortStage(JeiRecipeSortStage stage) {
        Object recipeSorterStage = this.recipeSorterStage(stage);
        if (recipeSorterStage == null) {
            return;
        }

        Object clientConfig = this.clientConfig();
        Class<?> stageClass = recipeSorterStage.getClass();
        if (this.isRecipeSortStageEnabled(stage)) {
            this.invoke(clientConfig, "disableRecipeSorterStage", new Class<?>[] { stageClass }, recipeSorterStage);
        } else {
            this.invoke(clientConfig, "enableRecipeSorterStage", new Class<?>[] { stageClass }, recipeSorterStage);
        }
    }

    @Override
    public JeiRecipeTransferPlan recipeTransferPlan(JeiRecipeEntry recipe, AbstractContainerMenu menu) {
        if (recipe == null || menu == null) {
            this.debugTransfer("plan-null-input", "plan skipped reason=null-input recipe={} menu={}", recipe == null ? "null" : recipe.uid(), this.describeMenu(menu));
            return null;
        }

        IRecipeLayoutDrawable<?> layout = this.castRecipeLayout(recipe.layout());
        IRecipeCategory<?> category = layout.getRecipeCategory();
        IRecipeTransferHandler<AbstractContainerMenu, Object> handler = this.transferHandler(menu, category).orElse(null);
        if (handler == null) {
            this.debugTransfer(
                "no-handler|" + recipe.uid() + "|" + this.describeMenu(menu) + "|" + this.describeCategory(category),
                "plan skipped reason=no-handler recipe={} menu={} category={}",
                recipe.uid(),
                this.describeMenu(menu),
                this.describeCategory(category)
            );
            return null;
        }

        Object rawRecipe = layout.getRecipe();
        List<IRecipeSlotView> inputSlots = layout.getRecipeSlotsView().getSlotViews(RecipeIngredientRole.INPUT);
        TransferPlanSlots transferSlots = this.transferPlanSlots(handler, menu, rawRecipe, inputSlots);
        if (transferSlots.recipeSlots().isEmpty() || transferSlots.mappings().isEmpty()) {
            this.debugTransfer(
                "no-slots|" + recipe.uid() + "|" + this.describeMenu(menu) + "|" + handler.getClass().getName(),
                "plan skipped reason=no-transfer-slots recipe={} menu={} handler={} inputSlots={} recipeSlots={} mappings={}",
                recipe.uid(),
                this.describeMenu(menu),
                handler.getClass().getName(),
                inputSlots.size(),
                transferSlots.recipeSlots().size(),
                transferSlots.mappings().size()
            );
            return null;
        }

        Rect2i button = layout.getRecipeTransferButtonArea();
        if (button.getWidth() <= 0 || button.getHeight() <= 0) {
            this.debugTransfer(
                "bad-button|" + recipe.uid() + "|" + this.describeMenu(menu) + "|" + button.getWidth() + "x" + button.getHeight(),
                "plan skipped reason=bad-button-rect recipe={} menu={} rect={}x{}+{},{}",
                recipe.uid(),
                this.describeMenu(menu),
                button.getWidth(),
                button.getHeight(),
                button.getX(),
                button.getY()
            );
            return null;
        }

        List<Integer> recipeSlotIds = transferSlots.recipeSlots()
            .stream()
            .map(slot -> slot.index)
            .distinct()
            .toList();
        List<JeiRecipeTransferSlot> requirements = new ArrayList<>();
        for (TransferSlotMapping mapping : transferSlots.mappings()) {
            IRecipeSlotView inputSlot = inputSlots.get(mapping.inputIndex());
            if (inputSlot.isEmpty()) {
                continue;
            }

            List<ItemStack> alternatives = inputSlot.getItemStacks()
                .filter(stack -> !stack.isEmpty())
                .map(stack -> stack.copyWithCount(Math.max(1, stack.getCount())))
                .toList();
            if (alternatives.isEmpty()) {
                this.debugTransfer(
                    "empty-alternatives|" + recipe.uid() + "|" + this.describeMenu(menu) + "|" + mapping.inputIndex(),
                    "plan skipped reason=empty-alternatives recipe={} menu={} inputIndex={} targetSlot={}",
                    recipe.uid(),
                    this.describeMenu(menu),
                    mapping.inputIndex(),
                    mapping.targetSlot().index
                );
                return null;
            }

            requirements.add(new JeiRecipeTransferSlot(mapping.inputIndex(), mapping.targetSlot().index, alternatives));
        }

        if (requirements.isEmpty()) {
            this.debugTransfer(
                "empty-requirements|" + recipe.uid() + "|" + this.describeMenu(menu),
                "plan skipped reason=empty-requirements recipe={} menu={} inputSlots={} mappings={}",
                recipe.uid(),
                this.describeMenu(menu),
                inputSlots.size(),
                transferSlots.mappings().size()
            );
            return null;
        }

        return new JeiRecipeTransferPlan(
            new JeiRecipeTransferRect(button.getX(), button.getY(), button.getWidth(), button.getHeight()),
            recipeSlotIds,
            requirements
        );
    }

    @Override
    public JeiDesktopEntry recipeIngredientAt(JeiRecipeEntry recipe, int x, int y, int mouseX, int mouseY) {
        IRecipeLayoutDrawable<?> layout = this.castRecipeLayout(recipe.layout());
        layout.setPosition(x, y);
        Optional<IRecipeSlotDrawable> slot = layout
            .getSlotUnderMouse(mouseX, mouseY)
            .map(slotUnderMouse -> slotUnderMouse.slot());
        if (slot.isEmpty()) {
            return null;
        }

        Optional<ITypedIngredient<?>> displayed = slot.get().getDisplayedIngredient();
        if (displayed.isPresent()) {
            return this.wrapTypedIngredient(displayed.get());
        }

        return slot.get()
            .getAllIngredients()
            .filter(ingredient -> ingredient != null)
            .findFirst()
            .map(this::wrapTypedIngredient)
            .orElse(null);
    }

    @Override
    public boolean handleRecipeMouseScrolled(JeiRecipeEntry recipe, int x, int y, double mouseX, double mouseY, double scrollX, double scrollY) {
        IRecipeLayoutDrawable<?> layout = this.castRecipeLayout(recipe.layout());
        layout.setPosition(x, y);
        IJeiInputHandler inputHandler = layout.getInputHandler();
        return inputHandler.handleMouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean handleRecipeMouseClicked(JeiRecipeEntry recipe, int x, int y, MouseButtonEvent event, boolean doubleClick) {
        IRecipeLayoutDrawable<?> layout = this.castRecipeLayout(recipe.layout());
        layout.setPosition(x, y);
        IJeiInputHandler inputHandler = layout.getInputHandler();
        return inputHandler.handleInput(event.x(), event.y(), new RecipeMouseInput(event, true));
    }

    @Override
    public boolean handleRecipeMouseReleased(JeiRecipeEntry recipe, int x, int y, MouseButtonEvent event) {
        IRecipeLayoutDrawable<?> layout = this.castRecipeLayout(recipe.layout());
        layout.setPosition(x, y);
        IJeiInputHandler inputHandler = layout.getInputHandler();
        return inputHandler.handleInput(event.x(), event.y(), new RecipeMouseInput(event, false));
    }

    @Override
    public boolean handleRecipeMouseDragged(JeiRecipeEntry recipe, int x, int y, MouseButtonEvent event, double dragX, double dragY) {
        IRecipeLayoutDrawable<?> layout = this.castRecipeLayout(recipe.layout());
        layout.setPosition(x, y);
        IJeiInputHandler inputHandler = layout.getInputHandler();
        InputConstants.Key key = InputConstants.Type.MOUSE.getOrCreate(event.input());
        return inputHandler.handleMouseDragged(event.x(), event.y(), key, dragX, dragY);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private Optional<IRecipeTransferHandler<AbstractContainerMenu, Object>> transferHandler(AbstractContainerMenu menu, IRecipeCategory<?> category) {
        return (Optional) this.runtime.getRecipeTransferManager().getRecipeTransferHandler(menu, (IRecipeCategory) category);
    }

    private TransferPlanSlots transferPlanSlots(
        IRecipeTransferHandler<AbstractContainerMenu, Object> handler,
        AbstractContainerMenu menu,
        Object recipe,
        List<IRecipeSlotView> inputSlots
    ) {
        IRecipeTransferInfo<AbstractContainerMenu, Object> transferInfo = this.transferInfo(handler);
        if (transferInfo != null) {
            if (!this.canHandleTransfer(transferInfo, menu, recipe)) {
                this.debugTransfer(
                    "can-handle-false|" + this.describeMenu(menu) + "|" + handler.getClass().getName(),
                    "slot lookup failed reason=can-handle-false menu={} handler={} transferInfo={} recipeClass={}",
                    this.describeMenu(menu),
                    handler.getClass().getName(),
                    transferInfo.getClass().getName(),
                    recipe == null ? "null" : recipe.getClass().getName()
                );
                return TransferPlanSlots.EMPTY;
            }

            List<Slot> recipeSlots = this.transferRecipeSlots(transferInfo, menu, recipe);
            if (recipeSlots.isEmpty() || inputSlots.size() > recipeSlots.size()) {
                this.debugTransfer(
                    "slot-mismatch|" + this.describeMenu(menu) + "|" + handler.getClass().getName() + "|" + inputSlots.size() + "|" + recipeSlots.size(),
                    "slot lookup failed reason=slot-mismatch menu={} handler={} transferInfo={} inputSlots={} recipeSlots={}",
                    this.describeMenu(menu),
                    handler.getClass().getName(),
                    transferInfo.getClass().getName(),
                    inputSlots.size(),
                    recipeSlots.size()
                );
                return TransferPlanSlots.EMPTY;
            }

            List<TransferSlotMapping> mappings = new ArrayList<>();
            for (int i = 0; i < inputSlots.size(); i++) {
                mappings.add(new TransferSlotMapping(i, recipeSlots.get(i)));
            }
            return new TransferPlanSlots(recipeSlots, mappings);
        }

        TransferPlanSlots fallback = this.fallbackVanillaTransferPlanSlots(menu, inputSlots);
        if (fallback == TransferPlanSlots.EMPTY || fallback.recipeSlots().isEmpty() || fallback.mappings().isEmpty()) {
            this.debugTransfer(
                "fallback-empty|" + this.describeMenu(menu) + "|" + handler.getClass().getName() + "|" + inputSlots.size(),
                "slot lookup failed reason=fallback-empty menu={} handler={} inputSlots={} menuSlots={}",
                this.describeMenu(menu),
                handler.getClass().getName(),
                inputSlots.size(),
                menu.slots.size()
            );
        }
        return fallback;
    }

    private TransferPlanSlots fallbackVanillaTransferPlanSlots(AbstractContainerMenu menu, List<IRecipeSlotView> inputSlots) {
        if (menu instanceof CraftingMenu) {
            return this.rangeTransferPlanSlots(menu, inputSlots, 1, 9);
        }
        if (menu instanceof CrafterMenu) {
            return this.rangeTransferPlanSlots(menu, inputSlots, 0, 9);
        }
        if (menu instanceof InventoryMenu) {
            return this.playerInventoryCraftingTransferPlanSlots(menu, inputSlots);
        }
        return TransferPlanSlots.EMPTY;
    }

    private TransferPlanSlots rangeTransferPlanSlots(AbstractContainerMenu menu, List<IRecipeSlotView> inputSlots, int slotStart, int slotCount) {
        List<Slot> recipeSlots = this.menuSlotRange(menu, slotStart, slotCount);
        if (recipeSlots.size() != slotCount || inputSlots.size() > recipeSlots.size()) {
            return TransferPlanSlots.EMPTY;
        }

        List<TransferSlotMapping> mappings = new ArrayList<>();
        for (int i = 0; i < inputSlots.size(); i++) {
            mappings.add(new TransferSlotMapping(i, recipeSlots.get(i)));
        }
        return new TransferPlanSlots(recipeSlots, mappings);
    }

    private TransferPlanSlots playerInventoryCraftingTransferPlanSlots(AbstractContainerMenu menu, List<IRecipeSlotView> inputSlots) {
        List<Slot> recipeSlots = this.menuSlotRange(menu, 1, 4);
        if (recipeSlots.size() != 4) {
            return TransferPlanSlots.EMPTY;
        }

        int[] playerGridInputIndexes = { 0, 1, 3, 4 };
        Set<Integer> mappedInputIndexes = new LinkedHashSet<>();
        for (int inputIndex : playerGridInputIndexes) {
            mappedInputIndexes.add(inputIndex);
        }

        for (int i = 0; i < inputSlots.size(); i++) {
            if (!mappedInputIndexes.contains(i) && !inputSlots.get(i).isEmpty()) {
                return TransferPlanSlots.EMPTY;
            }
        }

        List<TransferSlotMapping> mappings = new ArrayList<>();
        for (int i = 0; i < playerGridInputIndexes.length; i++) {
            int inputIndex = playerGridInputIndexes[i];
            if (inputIndex < inputSlots.size()) {
                mappings.add(new TransferSlotMapping(inputIndex, recipeSlots.get(i)));
            }
        }
        return new TransferPlanSlots(recipeSlots, mappings);
    }

    private List<Slot> menuSlotRange(AbstractContainerMenu menu, int slotStart, int slotCount) {
        if (slotStart < 0 || slotCount <= 0 || slotStart + slotCount > menu.slots.size()) {
            return List.of();
        }
        return List.copyOf(menu.slots.subList(slotStart, slotStart + slotCount));
    }

    @SuppressWarnings("unchecked")
    private IRecipeTransferInfo<AbstractContainerMenu, Object> transferInfo(Object handler) {
        return this.transferInfo(handler, Collections.newSetFromMap(new IdentityHashMap<>()));
    }

    @SuppressWarnings("unchecked")
    private IRecipeTransferInfo<AbstractContainerMenu, Object> transferInfo(Object handler, Set<Object> seen) {
        if (handler == null || !seen.add(handler)) {
            return null;
        }

        Object transferInfo = this.field(handler, "transferInfo");
        if (transferInfo instanceof IRecipeTransferInfo<?, ?> info) {
            return (IRecipeTransferInfo<AbstractContainerMenu, Object>) info;
        }

        Object assignableTransferInfo = this.fieldAssignable(handler, IRecipeTransferInfo.class);
        if (assignableTransferInfo instanceof IRecipeTransferInfo<?, ?> info) {
            return (IRecipeTransferInfo<AbstractContainerMenu, Object>) info;
        }

        Object nestedHandler = this.field(handler, "handler");
        if (nestedHandler != null && nestedHandler != handler) {
            IRecipeTransferInfo<AbstractContainerMenu, Object> nestedInfo = this.transferInfo(nestedHandler, seen);
            if (nestedInfo != null) {
                return nestedInfo;
            }
        }

        for (Object nested : this.fieldsAssignable(handler, IRecipeTransferHandler.class)) {
            IRecipeTransferInfo<AbstractContainerMenu, Object> nestedInfo = this.transferInfo(nested, seen);
            if (nestedInfo != null) {
                return nestedInfo;
            }
        }

        return null;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private boolean canHandleTransfer(IRecipeTransferInfo<AbstractContainerMenu, Object> transferInfo, AbstractContainerMenu menu, Object recipe) {
        return ((IRecipeTransferInfo) transferInfo).canHandle(menu, recipe);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private List<Slot> transferRecipeSlots(IRecipeTransferInfo<AbstractContainerMenu, Object> transferInfo, AbstractContainerMenu menu, Object recipe) {
        return ((IRecipeTransferInfo) transferInfo).getRecipeSlots(menu, recipe);
    }

    private IIngredientManager manager() {
        return this.runtime.getIngredientManager();
    }

    private IIngredientFilter filter() {
        return this.runtime.getIngredientFilter();
    }

    private IRecipeManager recipeManager() {
        return this.runtime.getRecipeManager();
    }

    private boolean matchesJeiKey(IJeiKeyMapping mapping, int key) {
        if (mapping == null || mapping.isUnbound() || key == InputConstants.UNKNOWN.getValue()) {
            return false;
        }
        return mapping.isActiveAndMatches(InputConstants.Type.KEYSYM.getOrCreate(key));
    }

    private boolean hasIngredients(IIngredientType<?> type) {
        return !this.manager().getAllIngredients(type).isEmpty();
    }

    private <T> JeiDesktopEntry tabIcon(IIngredientType<T> type) {
        return this.manager()
            .getAllIngredients(type)
            .stream()
            .findFirst()
            .map(ingredient -> new JeiDesktopEntry(type.getUid(), type, ingredient))
            .orElse(null);
    }

    private IIngredientType<?> type(JeiDesktopTab tab) {
        if (tab == null) {
            return null;
        }
        return this.manager().getIngredientTypeForUid(tab.uid()).orElse(null);
    }

    private <T> List<JeiDesktopEntry> filteredEntries(IIngredientType<T> type) {
        List<T> ingredients = this.filter().getFilteredIngredients(type);
        List<JeiDesktopEntry> entries = new ArrayList<>(ingredients.size());
        String uid = type.getUid();
        for (T ingredient : ingredients) {
            entries.add(new JeiDesktopEntry(uid, type, ingredient));
        }
        return entries;
    }

    private List<JeiDesktopEntry> elementEntries(Object source) {
        Object elements = this.invoke(source, "getElements");
        if (!(elements instanceof Iterable<?> iterable)) {
            return List.of();
        }

        List<JeiDesktopEntry> entries = new ArrayList<>();
        for (Object element : iterable) {
            Object typedIngredient = this.invoke(element, "getTypedIngredient");
            if (!(typedIngredient instanceof ITypedIngredient<?> ingredient)) {
                continue;
            }
            Object bookmark = this.invoke(element, "getBookmark");
            Object opaque = bookmark instanceof Optional<?> optional ? optional.orElse(null) : null;
            entries.add(this.wrapTypedIngredient(ingredient, opaque));
        }
        return entries;
    }

    private JeiRecipeCategory wrapCategory(IRecipeCategory<?> category) {
        return new JeiRecipeCategory(
            category.getRecipeType().getUid().toString(),
            category.getTitle(),
            category.getWidth(),
            category.getHeight(),
            category
        );
    }

    private <R> List<JeiRecipeEntry> recipesUnchecked(IFocus<?> focus, IFocusGroup focusGroup, JeiRecipeCategory category) {
        IRecipeCategory<R> recipeCategory = this.castCategory(category.category());
        List<R> recipes = this.recipeManager()
            .createRecipeLookup(recipeCategory.getRecipeType())
            .limitFocus(List.of(focus))
            .get()
            .toList();
        List<JeiRecipeEntry> entries = new ArrayList<>(recipes.size());
        for (int i = 0; i < recipes.size(); i++) {
            R recipe = recipes.get(i);
            Optional<IRecipeLayoutDrawable<R>> layout = this.recipeManager().createRecipeLayoutDrawable(recipeCategory, recipe, focusGroup);
            if (layout.isEmpty()) {
                continue;
            }
            String uid = category.uid() + ":" + i;
            Rect2i rect = layout.get().getRectWithBorder();
            entries.add(new JeiRecipeEntry(uid, category.uid(), rect.getWidth(), rect.getHeight(), recipe, layout.get()));
        }
        this.sortRecipes(entries);
        return entries;
    }

    private void sortRecipes(List<JeiRecipeEntry> entries) {
        boolean bookmarkedFirst = this.isRecipeSortStageEnabled(JeiRecipeSortStage.BOOKMARKED);
        boolean craftableFirst = this.isRecipeSortStageEnabled(JeiRecipeSortStage.CRAFTABLE);
        if (!bookmarkedFirst && !craftableFirst) {
            return;
        }

        Comparator<JeiRecipeEntry> comparator = (left, right) -> 0;
        if (bookmarkedFirst) {
            comparator = comparator.thenComparing(entry -> !this.isRecipeBookmarked(entry));
        }
        if (craftableFirst) {
            comparator = comparator.thenComparing(entry -> !this.isRecipeCraftable(entry));
        }
        entries.sort(comparator);
    }

    private boolean isRecipeCraftable(JeiRecipeEntry entry) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return false;
        }

        Inventory inventory = minecraft.player.getInventory();
        List<ItemStack> available = new ArrayList<>();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty()) {
                available.add(stack.copy());
            }
        }

        IRecipeLayoutDrawable<?> layout = this.castRecipeLayout(entry.layout());
        List<IRecipeSlotView> inputSlots = layout.getRecipeSlotsView().getSlotViews(RecipeIngredientRole.INPUT);
        if (inputSlots.isEmpty()) {
            return false;
        }

        for (IRecipeSlotView slot : inputSlots) {
            List<ItemStack> options = slot.getItemStacks()
                .filter(stack -> !stack.isEmpty())
                .toList();
            if (options.isEmpty()) {
                continue;
            }
            if (!this.consumeAnyMatchingIngredient(available, options)) {
                return false;
            }
        }
        return true;
    }

    private boolean consumeAnyMatchingIngredient(List<ItemStack> available, List<ItemStack> options) {
        for (ItemStack option : options) {
            List<ItemStack> candidate = available.stream()
                .map(ItemStack::copy)
                .toList();
            if (this.consumeIngredient(candidate, option)) {
                available.clear();
                available.addAll(candidate);
                return true;
            }
        }
        return false;
    }

    private boolean consumeIngredient(List<ItemStack> available, ItemStack ingredient) {
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

    private JeiDesktopEntry wrapTypedIngredient(ITypedIngredient<?> ingredient) {
        return this.wrapTypedIngredient(ingredient, null);
    }

    private JeiDesktopEntry wrapTypedIngredient(ITypedIngredient<?> ingredient, Object opaque) {
        return new JeiDesktopEntry(ingredient.getType().getUid(), ingredient.getType(), ingredient.getIngredient(), opaque);
    }

    private <T> void renderUnchecked(GuiGraphicsExtractor graphics, JeiDesktopEntry entry, int x, int y) {
        IIngredientType<T> type = this.castType(entry.type());
        T ingredient = this.castIngredient(entry.ingredient());
        IIngredientRenderer<T> renderer = this.manager().getIngredientRenderer(type);
        int renderX = x + Math.max(0, (16 - renderer.getWidth()) / 2);
        int renderY = y + Math.max(0, (16 - renderer.getHeight()) / 2);
        renderer.render(graphics.unwrap(), ingredient, renderX, renderY);
    }

    private <T> List<Component> tooltipUnchecked(JeiDesktopEntry entry) {
        IIngredientType<T> type = this.castType(entry.type());
        T ingredient = this.castIngredient(entry.ingredient());
        IIngredientRenderer<T> renderer = this.manager().getIngredientRenderer(type);
        TooltipFlag flag = Minecraft.getInstance().options.advancedItemTooltips ? TooltipFlag.Default.ADVANCED : TooltipFlag.Default.NORMAL;
        return renderer.getTooltip(ingredient, flag);
    }

    private <T> IFocus<T> focus(JeiDesktopEntry entry, JeiRecipeMode mode) {
        if (entry == null || mode == null || mode == JeiRecipeMode.INGREDIENTS) {
            return null;
        }
        IIngredientType<T> type = this.castType(entry.type());
        T ingredient = this.castIngredient(entry.ingredient());
        RecipeIngredientRole role = mode == JeiRecipeMode.RECIPES ? RecipeIngredientRole.OUTPUT : RecipeIngredientRole.INPUT;
        return this.runtime.getJeiHelpers().getFocusFactory().createFocus(role, type, ingredient);
    }

    private Object bookmarkList() {
        return this.field(this.runtime.getBookmarkOverlay(), "bookmarkList");
    }

    private Object lookupHistory() {
        Object overlay = this.runtime.getBookmarkOverlay();
        Object lookupHistoryOverlay = this.field(overlay, "lookupHistoryOverlay");
        return this.invoke(lookupHistoryOverlay, "getLookupHistory");
    }

    private Object clientConfig() {
        Object configs = this.invokeStatic("mezz.jei.common.Internal", "getJeiClientConfigs");
        return this.invoke(configs, "getClientConfig");
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Object recipeSorterStage(JeiRecipeSortStage stage) {
        Class<?> stageClass = this.classForName("mezz.jei.common.config.RecipeSorterStage");
        if (stageClass == null || !stageClass.isEnum()) {
            return null;
        }
        try {
            return Enum.valueOf((Class<? extends Enum>) stageClass.asSubclass(Enum.class), stage.name());
        } catch (IllegalArgumentException | ClassCastException ignored) {
            return null;
        }
    }

    private Object createIngredientBookmark(JeiDesktopEntry entry) {
        Optional<? extends ITypedIngredient<?>> typedIngredient = this.typedIngredient(entry);
        if (typedIngredient.isEmpty()) {
            return null;
        }
        return this.invokeStatic(
            "mezz.jei.gui.bookmarks.IngredientBookmark",
            "create",
            new Class<?>[] { this.classForName("mezz.jei.api.ingredients.ITypedIngredient"), this.classForName("mezz.jei.api.runtime.IIngredientManager") },
            typedIngredient.get(),
            this.manager()
        );
    }

    private Object createRecipeBookmark(JeiRecipeEntry recipe) {
        if (recipe == null || recipe.layout() == null) {
            return null;
        }
        return this.invokeStatic(
            "mezz.jei.gui.bookmarks.RecipeBookmark",
            "create",
            new Class<?>[] { this.classForName("mezz.jei.api.gui.IRecipeLayoutDrawable"), this.classForName("mezz.jei.api.runtime.IIngredientManager") },
            recipe.layout(),
            this.manager()
        );
    }

    private <T> Optional<ITypedIngredient<T>> typedIngredient(JeiDesktopEntry entry) {
        if (entry == null || entry.type() == null || entry.ingredient() == null) {
            return Optional.empty();
        }
        IIngredientType<T> type = this.castType(entry.type());
        T ingredient = this.castIngredient(entry.ingredient());
        return this.manager().createTypedIngredient(type, ingredient, true);
    }

    private Object field(Object target, String fieldName) {
        if (target == null) {
            return null;
        }
        for (Class<?> owner = target.getClass(); owner != null && owner != Object.class; owner = owner.getSuperclass()) {
            try {
                Field field = owner.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field.get(target);
            } catch (ReflectiveOperationException | RuntimeException ignored) {
                // Try the next class in the hierarchy.
            }
        }
        return null;
    }

    private Object fieldAssignable(Object target, Class<?> type) {
        List<Object> values = this.fieldsAssignable(target, type);
        return values.isEmpty() ? null : values.getFirst();
    }

    private List<Object> fieldsAssignable(Object target, Class<?> type) {
        if (target == null || type == null) {
            return List.of();
        }

        List<Object> values = new ArrayList<>();
        for (Class<?> owner = target.getClass(); owner != null && owner != Object.class; owner = owner.getSuperclass()) {
            for (Field field : owner.getDeclaredFields()) {
                if (!type.isAssignableFrom(field.getType())) {
                    continue;
                }
                try {
                    field.setAccessible(true);
                    Object value = field.get(target);
                    if (value != null && value != target) {
                        values.add(value);
                    }
                } catch (ReflectiveOperationException | RuntimeException ignored) {
                    // Ignore inaccessible implementation details.
                }
            }
        }
        return values;
    }

    private Object invoke(Object target, String methodName) {
        if (target == null) {
            return null;
        }
        try {
            Method method = target.getClass().getMethod(methodName);
            return method.invoke(target);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return null;
        }
    }

    private Object invoke(Object target, String methodName, Class<?>[] parameterTypes, Object... args) {
        if (target == null || hasNull(parameterTypes)) {
            return null;
        }
        try {
            Method method = target.getClass().getMethod(methodName, parameterTypes);
            return method.invoke(target, args);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return null;
        }
    }

    private boolean invokeBoolean(Object target, String methodName, boolean fallback) {
        Object value = this.invoke(target, methodName);
        return value instanceof Boolean bool ? bool : fallback;
    }

    private boolean invokeBoolean(Object target, String methodName, boolean fallback, Class<?> parameterType, Object arg) {
        Object value = this.invoke(target, methodName, new Class<?>[] { parameterType }, arg);
        return value instanceof Boolean bool ? bool : fallback;
    }

    private Object invokeStatic(String className, String methodName) {
        return this.invokeStatic(className, methodName, new Class<?>[0]);
    }

    private Object invokeStatic(String className, String methodName, Class<?>[] parameterTypes, Object... args) {
        if (hasNull(parameterTypes)) {
            return null;
        }
        try {
            Class<?> owner = Class.forName(className);
            Method method = owner.getMethod(methodName, parameterTypes);
            return method.invoke(null, args);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return null;
        }
    }

    private Class<?> classForName(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException | RuntimeException ignored) {
            return null;
        }
    }

    private static boolean hasNull(Class<?>[] parameterTypes) {
        for (Class<?> parameterType : parameterTypes) {
            if (parameterType == null) {
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private <T> IIngredientType<T> castType(Object type) {
        return (IIngredientType<T>) type;
    }

    @SuppressWarnings("unchecked")
    private <T> IRecipeCategory<T> castCategory(Object category) {
        return (IRecipeCategory<T>) category;
    }

    @SuppressWarnings("unchecked")
    private <T> IRecipeLayoutDrawable<T> castRecipeLayout(Object layout) {
        return (IRecipeLayoutDrawable<T>) layout;
    }

    @SuppressWarnings("unchecked")
    private <T> T castIngredient(Object ingredient) {
        return (T) ingredient;
    }

    private String titleFor(IIngredientType<?> type) {
        if (type == VanillaTypes.ITEM_STACK) {
            return "Items";
        }
        String uid = type.getUid();
        int separator = uid.indexOf(':');
        if (separator >= 0 && separator + 1 < uid.length()) {
            uid = uid.substring(separator + 1);
        }
        if (uid.contains("fluid")) {
            return "Fluids";
        }
        String normalized = uid.replace('_', ' ').replace('-', ' ').trim();
        if (normalized.isEmpty()) {
            normalized = type.getUid();
        }
        String[] words = normalized.toLowerCase(Locale.ROOT).split(" ");
        StringBuilder title = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) {
                continue;
            }
            if (!title.isEmpty()) {
                title.append(' ');
            }
            title.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return title.isEmpty() ? type.getUid() : title.toString();
    }

    private void debugTransfer(String key, String message, Object... args) {
        if (!JEI_TRANSFER_DEBUG) {
            return;
        }

        long now = System.currentTimeMillis();
        if (key.equals(this.lastTransferDebugKey) && now - this.lastTransferDebugAt < 2_000L) {
            return;
        }
        this.lastTransferDebugKey = key;
        this.lastTransferDebugAt = now;
        DesktopDebug.warn("JEI_TRANSFER_DIAG " + message, args);
    }

    private String describeCategory(IRecipeCategory<?> category) {
        if (category == null) {
            return "none";
        }
        return category.getClass().getName()
            + "/type="
            + category.getRecipeType().getUid()
            + "/recipeClass="
            + category.getRecipeType().getRecipeClass().getName();
    }

    private String describeMenu(@Nullable AbstractContainerMenu menu) {
        if (menu == null) {
            return "none";
        }

        return menu.getClass().getName()
            + "/type="
            + this.safeMenuKey(menu)
            + "/containerId="
            + menu.containerId
            + "/slots="
            + menu.slots.size();
    }

    private String safeMenuKey(AbstractContainerMenu menu) {
        try {
            MenuType<?> menuType = menu.getType();
            Identifier key = BuiltInRegistries.MENU.getKey(menuType);
            return key == null ? "unknown" : key.toString();
        } catch (RuntimeException ignored) {
            return "unknown";
        }
    }

    private record TransferPlanSlots(List<Slot> recipeSlots, List<TransferSlotMapping> mappings) {
        private static final TransferPlanSlots EMPTY = new TransferPlanSlots(List.of(), List.of());
    }

    private record TransferSlotMapping(int inputIndex, Slot targetSlot) {
    }

    private record RecipeMouseInput(MouseButtonEvent event, boolean simulate) implements IJeiUserInput {
        @Override
        public InputConstants.Key getKey() {
            return InputConstants.Type.MOUSE.getOrCreate(this.event.input());
        }

        @Override
        public int getModifiers() {
            return this.event.modifiers();
        }

        @Override
        public InputWithModifiers getInputWithModifiers() {
            return this.event;
        }

        @Override
        public boolean isSimulate() {
            return this.simulate;
        }

        @Override
        public boolean is(KeyMapping keyMapping) {
            return keyMapping.matchesMouse(this.event);
        }

        @Override
        public boolean is(IJeiKeyMapping keyMapping) {
            return keyMapping.isActiveAndMatches(this.getKey());
        }
    }
}
