package com.salts_inventory_update.controlify;

import com.salts_inventory_update.api.client.desktop.DesktopContextualMenu;
import com.salts_inventory_update.api.client.desktop.DesktopWindowBounds;
import com.salts_inventory_update.controlify.model.ContextualWheelSelection;
import com.salts_inventory_update.controlify.model.AlphabeticalRecipeIndex;
import com.salts_inventory_update.controlify.model.AlphabeticalRecipeCache;
import com.salts_inventory_update.controlify.model.AlphabeticalMarkerLayout;
import com.salts_inventory_update.controlify.model.ContextualPresentationLayout;
import com.salts_inventory_update.controlify.model.ContextualPresentationTransition;
import com.salts_inventory_update.controlify.model.CreativeTabLayers;
import com.salts_inventory_update.controlify.model.RecipeFilter;
import com.salts_inventory_update.controlify.WorkspaceUiSounds.Action;
import com.salts_inventory_update.controlify.render.ContextualRadialAssets;
import com.salts_inventory_update.controlify.render.ContextualRadialAssets.Element;
import com.salts_inventory_update.controlify.render.WorkspaceOpacity;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import com.salts_inventory_update.api.client.gui.GuiGraphicsExtractor;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.display.RecipeDisplayId;
import net.minecraft.world.item.ItemStack;

/** The held-trigger recipe/tab presentation. Actual actions remain in the native Core session API. */
public final class ContextualRadialOverlay {
    private final SaltsControlifyClient owner;
    private final ContextualWheelSelection selection = new ContextualWheelSelection();
    private final com.salts_inventory_update.controlify.model.StableRecipeOrder<DesktopContextualMenu.Recipe> recipeOrder = new com.salts_inventory_update.controlify.model.StableRecipeOrder<>();
    private UUID source, destination;
    private DesktopContextualMenu snapshot = DesktopContextualMenu.NONE;
    private int category, creativeIndex;
    private RecipeFilter filter = RecipeFilter.ALL;
    private long refreshedAt;
    private String status = "";
    private RecipeDisplayId hoverRecipe;
    private AlphabeticalRecipeIndex.Catalog<DesktopContextualMenu.Recipe> alphabetCatalog = AlphabeticalRecipeIndex.Catalog.empty();
    private final AlphabeticalRecipeCache alphabetCache = new AlphabeticalRecipeCache();
    private List<DesktopContextualMenu.Recipe> indexedEntries = List.of();
    private DesktopContextualMenu indexedSnapshot;
    private int indexedCategory = -1;
    private RecipeFilter indexedFilter;
    private String indexedLanguage = "";
    private Language indexedLanguageInstance;
    private final ContextualPresentationTransition transition = new ContextualPresentationTransition();
    private boolean inputActive;
    private ContextualPresentationLayout.Rect originalSource;
    private ContextualPresentationLayout.Rect originalDestination;
    private ContextualPresentationLayout.Frame presentationFrame;
    private ContextualPresentationLayout.Frame closingFrom, reopeningFrom;
    private double retargetProgress;
    public ContextualRadialOverlay(SaltsControlifyClient owner) { this.owner = owner; }
    public boolean active() { return inputActive; }
    public boolean presenting() { return source != null; }
    public UUID presentationSource() { return source; }
    public UUID presentationDestination() { return destination; }
    /** Pure observation of the cached frame shared by artwork and the live native window. */
    public ContextualPresentationLayout.Frame presentationFrame() { return presentationFrame; }
    public ContextualPresentationLayout.Rect originalSourceRect() { return originalSource; }
    public ContextualPresentationLayout.Rect originalDestinationRect() { return originalDestination; }
    public void preparePresentation(int width, int height, ContextualPresentationLayout.Rect sourceSide,
            ContextualPresentationLayout.Rect destinationSide) {
        if (!presenting()) return;
        var window = owner.windows().get(source);
        var destinationWindow = owner.windows().get(destination);
        if (window == null || !window.handle().isOpen() || destinationWindow == null || !destinationWindow.handle().isOpen()) { finishImmediately(); return; }
        long now = System.nanoTime();
        if (!owner.config().animationsEnabled()) transition.target(active(), now, false);
        if (!active() && transition.finished(now)) { finishImmediately(); return; }
        if (originalSource == null) originalSource = sourceSide;
        if (originalDestination == null) originalDestination = destinationSide;
        double progress = transition.value(now);
        var bounds = owner.desktop().presentationBounds(source);
        var destinationBounds = owner.desktop().presentationBounds(destination);
        if (!active() && closingFrom != null)
            presentationFrame = ContextualPresentationLayout.closing(width, height, sourceSide, destinationSide, closingFrom,
                retargetProgress <= 0 ? 0 : progress / retargetProgress);
        else if (active() && reopeningFrom != null)
            presentationFrame = ContextualPresentationLayout.reopening(width, height, originalSource, originalDestination,
                bounds.width(), bounds.height(), destinationBounds.width(), destinationBounds.height(),
                reopeningFrom, retargetProgress >= 1 ? 1 : (progress - retargetProgress) / (1 - retargetProgress));
        else presentationFrame = ContextualPresentationLayout.frame(width, height, originalSource, originalDestination, progress,
            bounds.width(), bounds.height(), destinationBounds.width(), destinationBounds.height());
    }
    public UUID source() { return source; }
    public DesktopContextualMenu.Kind kind() { return snapshot.kind(); }
    /** Availability shown by the native guide uses the same displayed card as confirmation. */
    public boolean canConfirm() {
        if (!active()) return false;
        if (snapshot.kind() == DesktopContextualMenu.Kind.CREATIVE) return !snapshot.tabs().isEmpty();
        var entries = filtered();
        int index = selection.displayedSelected(entries.size());
        return index >= 0 && entries.get(index).craftable();
    }
    public boolean open(UUID source, UUID destination) {
        if (source == null || destination == null || source.equals(destination)) return false;
        DesktopContextualMenu content = owner.desktop().contextualMenu(source);
        if (content.kind() == DesktopContextualMenu.Kind.NONE) return false;
        long now = System.nanoTime();
        boolean samePair = source.equals(this.source) && destination.equals(this.destination);
        boolean resuming = samePair && !active() && presentationFrame != null;
        if (!samePair) {
            if (this.source != null) owner.desktop().endRecipeHover(this.source);
            transition.reset(); originalSource = originalDestination = null; presentationFrame = null;
        }
        if (content.kind() != DesktopContextualMenu.Kind.CREATIVE && !owner.desktop().beginRecipeHover(source)) {
            finishImmediately(); WorkspaceUiSounds.play(Action.REJECT); return false;
        }
        reopeningFrom = resuming ? presentationFrame : null; closingFrom = null;
        retargetProgress = transition.value(now);
        this.source = source; this.destination = destination; snapshot = content;
        inputActive = true;
        transition.target(true, now, owner.config().animationsEnabled());
        category = 0; creativeIndex = 0; filter = owner.recipeFilter(); selection.reset(); status = "";
        invalidateCatalog();
        hoverRecipe = null;
        for (int i = 0; i < content.tabs().size(); i++) if (content.tabs().get(i).selected()) creativeIndex = i;
        refreshedAt = System.nanoTime();
        owner.desktop().cancelInput();
        WorkspaceUiSounds.play(Action.OPEN);
        return true;
    }
    public void close() {
        boolean wasActive = active();
        inputActive = false;
        if (wasActive) {
            owner.desktop().endRecipeHover(source); hoverRecipe = null;
            long now = System.nanoTime();
            closingFrom = presentationFrame; reopeningFrom = null; retargetProgress = transition.value(now);
            transition.target(false, now, owner.config().animationsEnabled());
        }
        if (!owner.config().animationsEnabled()) finishImmediately();
        if (wasActive) WorkspaceUiSounds.play(Action.CLOSE);
    }
    /** Screen/session removal cannot leave a retained visual pointing at a stale native menu. */
    public void finishImmediately() {
        if (source != null) owner.desktop().endRecipeHover(source);
        hoverRecipe = null;
        inputActive = false; source = null; destination = null; originalSource = originalDestination = null;
        snapshot = DesktopContextualMenu.NONE; status = ""; presentationFrame = null; closingFrom = reopeningFrom = null;
        transition.reset(); invalidateCatalog();
    }
    public void input(float rightX, float rightY, float leftX, float leftY,
                      boolean categoryLeft, boolean categoryRight, boolean toggleCraftable, boolean confirm) {
        input(rightX, rightY, leftX, leftY, categoryLeft, categoryRight, toggleCraftable, confirm, false, false, false);
    }
    public void input(float rightX, float rightY, float leftX, float leftY,
                      boolean categoryLeft, boolean categoryRight, boolean toggleCraftable, boolean confirm,
                      boolean rotateLeftHeld, boolean rotateRightHeld, boolean craftStack) {
        if (!active()) return;
        var sourceWindow = owner.windows().get(source);
        var destinationWindow = owner.windows().get(destination);
        if (sourceWindow == null || destinationWindow == null || !sourceWindow.handle().isOpen() || !destinationWindow.handle().isOpen()) { finishImmediately(); return; }
        if (snapshot.kind() != DesktopContextualMenu.Kind.CREATIVE && owner.desktop().recipeHoverRejected(source)) {
            close(); WorkspaceUiSounds.play(Action.REJECT); return;
        }
        long now = System.nanoTime();
        if (now - refreshedAt >= 150_000_000L) {
            snapshot = owner.desktop().contextualMenu(source); refreshedAt = now;
            if (snapshot.kind() == DesktopContextualMenu.Kind.NONE) { finishImmediately(); return; }
        }
        if (snapshot.kind() == DesktopContextualMenu.Kind.CREATIVE) {
            int size = snapshot.tabs().size();
            if (size == 0) return;
            int before = creativeIndex;
            Action sound = null;
            if (Math.hypot(rightX, rightY) >= 0.45) creativeIndex = ContextualWheelSelection.sector(rightX, rightY, size);
            else if (Math.hypot(leftX, leftY) >= 0.45) creativeIndex = ContextualWheelSelection.sector(leftX, leftY, size);
            if (categoryLeft || categoryRight) creativeIndex = Math.floorMod(creativeIndex + (categoryRight ? 1 : -1), size);
            creativeIndex = Math.min(size - 1, creativeIndex);
            if (creativeIndex != before) sound = Action.NAVIGATE;
            if (confirm) {
                var tab = snapshot.tabs().get(creativeIndex);
                if (!tab.selected()) {
                    String previousStatus = status;
                    boolean accepted = owner.desktop().selectContextualTab(source, tab.id());
                    status = accepted ? "" : "Tab unavailable";
                    if (accepted) sound = Action.TAB;
                    else if (!status.equals(previousStatus)) sound = Action.REJECT;
                }
                snapshot = owner.desktop().contextualMenu(source); refreshedAt = now;
            }
            if (sound != null) WorkspaceUiSounds.play(sound);
            return;
        }
        Action sound = null;
        if (categoryLeft || categoryRight) {
            int next = Math.floorMod(category + (categoryRight ? 1 : -1), Math.max(1, snapshot.categories().size()));
            if (next != category) { category = next; selection.reset(); status = ""; sound = Action.CATEGORY; }
        }
        if (toggleCraftable) { filter = owner.cycleRecipeFilter(); selection.reset(); status = ""; sound = Action.FILTER; }
        List<DesktopContextualMenu.Recipe> entries = filtered();
        int before = selection.selected(entries.size());
        selection.update(entries.size(), rightX, rightY, leftX, leftY, rotateLeftHeld, rotateRightHeld, now);
        selection.advancePresentation(now, owner.config().animationsEnabled());
        int selected = selection.displayedSelected(entries.size());
        if (selected != before) status = "";
        if (sound == null && selection.selected(entries.size()) != before && selected >= 0) sound = Action.CAROUSEL;
        if ((confirm || craftStack && snapshot.kind() == DesktopContextualMenu.Kind.CRAFTING) && selected >= 0) {
            var recipe = entries.get(selected);
            String previousStatus = status;
            boolean accepted = false;
            WorkspaceCraftFeedback.Attempt craftAttempt = null;
            if (!recipe.craftable()) status = "Missing ingredients";
            else if (snapshot.kind() == DesktopContextualMenu.Kind.COOKING) {
                accepted = owner.desktop().prepareCookingRecipe(source, recipe.id());
                status = accepted ? "" : "Ingredients unavailable";
            } else {
                craftAttempt = owner.craftFeedback().before(owner.desktop(), source, destination, recipe.output());
                accepted = craftStack && !confirm ? owner.desktop().craftStackToWindow(source, destination, recipe.id())
                    : owner.desktop().craftToWindow(source, destination, recipe.id());
                status = accepted ? "" : "Destination unavailable";
            }
            if (accepted) {
                if (snapshot.kind() == DesktopContextualMenu.Kind.COOKING) sound = Action.REQUEST;
                else { owner.craftFeedback().accepted(craftAttempt); sound = null; }
            }
            else if (!status.equals(previousStatus)) sound = Action.REJECT;
            refreshedAt = 0;
            if (accepted) hoverRecipe = null;
        }
        if (selected >= 0) {
            var hovered = entries.get(selected).id();
            if (!hovered.equals(hoverRecipe)) {
                owner.desktop().updateRecipeHover(source, hovered); hoverRecipe = hovered;
            }
        } else if (hoverRecipe != null) {
            owner.desktop().updateRecipeHover(source, null); hoverRecipe = null;
        }
        if (sound != null) WorkspaceUiSounds.play(sound);
    }
    private List<DesktopContextualMenu.Recipe> filtered() {
        if (snapshot.categories().isEmpty()) { invalidateCatalog(); return List.of(); }
        category = Math.min(category, snapshot.categories().size() - 1);
        String language = Minecraft.getInstance().getLanguageManager().getSelected();
        Language languageInstance = Language.getInstance();
        if (indexedSnapshot == snapshot && indexedCategory == category && indexedFilter == filter
                && indexedLanguage.equals(language) && indexedLanguageInstance == languageInstance)
            return indexedEntries;
        var id = snapshot.categories().get(category).id();
        var available = snapshot.recipes().stream()
            .filter(recipe -> category == 0 || id.equals(recipe.category())).toList();
        var eligible = available.stream().filter(recipe -> filter.includes(recipe.craftable(),
            recipe.grid().stream().anyMatch(ingredient -> ingredient.available() > 0))).toList();
        boolean sameContext = indexedCategory == category && indexedFilter == filter
            && indexedLanguage.equals(language) && indexedLanguageInstance == languageInstance;
        if (!sameContext) recipeOrder.reset();
        // Refresh availability and counts without moving cards when crafting consumes ingredients.
        // A new opening, category or filter takes a fresh membership snapshot.
        var pinned = recipeOrder.update(available, eligible, recipe -> recipe.id().toString());
        alphabetCatalog = alphabetCache.index(pinned.stream().map(recipe -> new AlphabeticalRecipeIndex.Entry<>(
            recipe, recipe.output().getHoverName().getString(), recipe.id().toString())).toList(), language, languageInstance);
        var entries = alphabetCatalog.entries();
        indexedSnapshot = snapshot; indexedCategory = category; indexedFilter = filter;
        indexedLanguage = language; indexedLanguageInstance = languageInstance;
        indexedEntries = entries.stream().map(AlphabeticalRecipeIndex.Entry::value).toList();
        return indexedEntries;
    }
    private void invalidateCatalog() {
        recipeOrder.reset();
        indexedSnapshot = null; indexedCategory = -1; alphabetCatalog = AlphabeticalRecipeIndex.Catalog.empty(); indexedEntries = List.of();
    }
    public List<AlphabeticalRecipeIndex.Group> alphabetGroups() { filtered(); return alphabetCatalog.groups(); }
    private String filterLabel() {
        return Component.translatable("salts_inventory_controlify.recipe_filter." + filter.key()).getString();
    }
    public void render(GuiGraphicsExtractor graphics, int width, int height) {
        if (!presenting()) return;
        if (presentationFrame == null) return;
        var frame = presentationFrame;
        graphics.nextStratum();
        try (var opacity = WorkspaceOpacity.enter((float) frame.progress())) {
            ContextualRadialAssets.texture(graphics, Element.BACKDROP, 0, 0, width, height);
        }
        graphics.enableScissor(0, 0, width, height);
        try {
            renderEmergingPanel(graphics, frame.wheel(), ContextualPresentationLayout.WHEEL, (float) frame.progress(), true);
            renderEmergingPanel(graphics, frame.materials(), ContextualPresentationLayout.MATERIALS, (float) frame.progress(), false);
        } finally { graphics.disableScissor(); }
    }
    private void renderEmergingPanel(GuiGraphicsExtractor graphics, ContextualPresentationLayout.Rect screen,
            ContextualPresentationLayout.Rect virtual, float opacity, boolean wheel) {
        graphics.pose().pushMatrix();
        double scale = screen.width() / virtual.width();
        graphics.pose().translate((float) screen.x(), (float) screen.y());
        graphics.pose().scale((float) scale, (float) scale);
        graphics.pose().translate((float) -virtual.x(), (float) -virtual.y());
        try (var scope = WorkspaceOpacity.enter(opacity)) {
            renderNativeFrame(graphics, virtual, wheel);
            if (wheel) {
                if (snapshot.kind() == DesktopContextualMenu.Kind.CREATIVE) renderTabs(graphics);
                else renderRecipes(graphics);
            } else renderMaterials(graphics);
        } finally { graphics.pose().popMatrix(); }
    }
    /** Only Core owns the dynamic window skin and native title drawing. */
    private void renderNativeFrame(GuiGraphicsExtractor graphics, ContextualPresentationLayout.Rect virtual, boolean wheel) {
        String title;
        if (snapshot.kind() == DesktopContextualMenu.Kind.CREATIVE) title = wheel ? "Creative tabs" : "Creative tab";
        else if (!wheel) title = "Ingredients  •  Have / Need";
        else {
            int count = filtered().size();
            String categoryTitle = snapshot.categories().isEmpty() ? "Crafting" : snapshot.categories().get(category).title();
            title = categoryTitle + "  •  " + count + " recipes";
        }
        owner.desktop().renderWindowFrame(graphics,
            new DesktopWindowBounds((int)virtual.x(), (int)virtual.y(), (int)virtual.width(), (int)virtual.height()), Component.literal(title));
    }
    private void renderMaterials(GuiGraphicsExtractor graphics) {
        if (snapshot.kind() == DesktopContextualMenu.Kind.CREATIVE) {
            if (!snapshot.tabs().isEmpty()) {
                var tab = snapshot.tabs().get(Math.min(creativeIndex, snapshot.tabs().size() - 1));
                int center = (int)ContextualPresentationLayout.MATERIALS.centerX(), top = (int)ContextualPresentationLayout.MATERIALS.y();
                itemIcon(graphics, tab.icon(), center, top + 56, 1.6f);
                text(graphics, truncate(tab.title(), 170), center, top + 94, 0xFF404040);
            }
            return;
        }
        var entries = filtered();
        int selected = selection.displayedSelected(entries.size());
        if (selected >= 0) renderRequirements(graphics, entries.get(selected));
        else text(graphics, "No recipe selected", (int)ContextualPresentationLayout.MATERIALS.centerX(),
            (int)ContextualPresentationLayout.MATERIALS.y() + 31, 0xFF404040);
    }
    private void renderTabs(GuiGraphicsExtractor graphics) {
        int size = snapshot.tabs().size();
        creativeIndex = Math.min(creativeIndex, Math.max(0, size - 1));
        for (int i = 0; i < size; i++) {
            var tab = snapshot.tabs().get(i);
            int x = radialX(i, size, 132), y = radialY(i, size, 132);
            tabTile(graphics, tab.icon(), x, y, i == creativeIndex, tab.selected());
        }
        if (size > 0) {
            var tab = snapshot.tabs().get(creativeIndex);
            text(graphics, truncate(tab.title(), 220), 380, 210, ContextualRadialAssets.theme().text());
        }
        text(graphics, status, 380, 382, 0xFF8F1E19);
    }
    private void renderRecipes(GuiGraphicsExtractor graphics) {
        List<DesktopContextualMenu.Recipe> entries = filtered();
        long presentationNow = System.nanoTime();
        selection.advancePresentation(presentationNow, owner.config().animationsEnabled());
        renderCategories(graphics);
        var theme = ContextualRadialAssets.theme();
        int selected = selection.displayedSelected(entries.size());
        String selectedLetter = selected < 0 ? "" : AlphabeticalRecipeIndex.initialLetter(alphabetCatalog.entries().get(selected).displayName());
        var font = Minecraft.getInstance().font;
        int labelWidth = Math.max(10, alphabetCatalog.groups().stream().mapToInt(group -> font.width(group.letter()) + 4).max().orElse(10));
        var placements = AlphabeticalMarkerLayout.layout(alphabetCatalog.groups(), entries.size(), labelWidth,
            Math.max(12, font.lineHeight + 3), alphabetObstacles(entries.size()));
        for (var card : selection.cards(entries.size(), presentationNow, owner.config().animationsEnabled())) {
            var recipe = entries.get(card.recipeIndex());
            int x = 380 + (int) Math.round(Math.sin(card.angle()) * 108), y = 221 - (int) Math.round(Math.cos(card.angle()) * 108);
            graphics.pose().pushMatrix();
            try (var opacity = WorkspaceOpacity.enter((float) card.opacity())) {
                graphics.pose().translate(x, y); graphics.pose().scale((float) card.scale(), (float) card.scale());
                tile(graphics, recipe.output(), 0, 0, card.recipeIndex() == selected, recipe.craftable());
            } finally { graphics.pose().popMatrix(); }
        }
        // The native card surfaces enter behind the foreground doorway artwork.
        graphics.nextStratum();
        ContextualRadialAssets.texture(graphics, Element.CAROUSEL_OPENING, 300, 280, 160, 90);
        // Navigation stays legible over the doorway, while only cards pass behind it.
        graphics.nextStratum();
        ContextualRadialAssets.texture(graphics, Element.WHEEL_RING, 236, 77, 288, 288);
        if (!entries.isEmpty()) ContextualRadialAssets.drawRingHighlight(graphics, 236, 77,
            selection.positionAngle(entries.size()), selection.highlightHalfAngle(entries.size()));
        // Leaders precede every label, so no connector can erase a letter's native text.
        for (var placement : placements) renderAlphabetLeader(graphics, placement);
        for (int i = 0; i < alphabetCatalog.groups().size(); i++) {
            var group = alphabetCatalog.groups().get(i);
            renderAlphabetMarker(graphics, group, entries.size(), group.letter().equals(selectedLetter), placements.get(i));
        }
        // The blank bottom wedge lets fading cards enter and leave instead of appearing as pages.
        text(graphics, entries.isEmpty() ? "0 recipes" : "Recipe " + (selected + 1) + " / " + entries.size(), 380, 205, theme.hover());
        if (selected >= 0) {
            var recipe = entries.get(selected);
            renderSelectedRecipe(graphics, recipe);
        } else text(graphics, "No recipes in this category", 380, 229, theme.text());
        text(graphics, filterLabel(), 380, 394, theme.mutedText());
        text(graphics, status, 380, 410, 0xFF8F1E19);
    }
    private List<AlphabeticalMarkerLayout.Rect> alphabetObstacles(int recipeCount) {
        var obstacles = new java.util.ArrayList<AlphabeticalMarkerLayout.Rect>();
        obstacles.add(new AlphabeticalMarkerLayout.Rect(12, 21, 195, 240));
        obstacles.add(new AlphabeticalMarkerLayout.Rect(12, 275, 195, 154));
        obstacles.add(new AlphabeticalMarkerLayout.Rect(553, 87, 195, 267));
        int count = snapshot.categories().size(), spacing = Math.min(44, 270 / Math.max(1, count));
        for (int i = 0; i < count; i++) {
            int half = i == category ? 17 : 12;
            int x = 380 + (2 * i - count + 1) * spacing / 2;
            obstacles.add(new AlphabeticalMarkerLayout.Rect(x - half - 1, 59 - half - 1, half * 2 + 2, half * 2 + 2));
        }
        var font = Minecraft.getInstance().font;
        String summary = filterLabel();
        obstacles.add(new AlphabeticalMarkerLayout.Rect(380 - font.width(summary) / 2, 394, font.width(summary), font.lineHeight));
        obstacles.add(new AlphabeticalMarkerLayout.Rect(380 - font.width(status) / 2, 410, font.width(status), font.lineHeight));
        return obstacles;
    }
    private static void renderAlphabetMarker(GuiGraphicsExtractor graphics, AlphabeticalRecipeIndex.Group group, int recipeCount,
            boolean highlighted, AlphabeticalMarkerLayout.Placement placement) {
        var theme = ContextualRadialAssets.theme();
        renderAlphabetAnchor(graphics, group, recipeCount, highlighted, placement.anchorX(), placement.anchorY());
        var bounds = placement.backgroundBounds();
        ContextualRadialAssets.texture(graphics, highlighted ? Element.HIGHLIGHTED_ALPHABET_LABEL : Element.ALPHABET_LABEL,
            bounds.x(), bounds.y(), bounds.width(), bounds.height());
        text(graphics, group.letter(), placement.labelX(), placement.labelY() - Minecraft.getInstance().font.lineHeight / 2,
            highlighted ? theme.hover() : theme.mutedText());
    }
    private static void renderAlphabetAnchor(GuiGraphicsExtractor graphics, AlphabeticalRecipeIndex.Group group, int recipeCount,
            boolean highlighted, int x, int y) {
        var theme = ContextualRadialAssets.theme();
        ContextualRadialAssets.draw(graphics, Element.ALPHABET_ANCHOR, x - 1, y - 1, 2, 2, highlighted ? theme.hover() : theme.ring());
    }
    private static void renderAlphabetLeader(GuiGraphicsExtractor graphics, AlphabeticalMarkerLayout.Placement placement) {
        var bounds = placement.backgroundBounds();
        int x = placement.anchorX(), y = placement.anchorY();
        int endX = Math.max(bounds.x(), Math.min(x, bounds.right())), endY = Math.max(bounds.y(), Math.min(y, bounds.bottom()));
        int length = (int)Math.round(Math.hypot(endX - x, endY - y));
        if (length == 0) return;
        graphics.pose().pushMatrix();
        try {
            graphics.pose().translate(x, y);
            graphics.pose().rotate((float)Math.atan2(endY - y, endX - x));
            ContextualRadialAssets.draw(graphics, Element.ALPHABET_LEADER, 0, 0, length, 1,
                ContextualRadialAssets.theme().ring() & 0x00FFFFFF | 0xCC000000);
        } finally { graphics.pose().popMatrix(); }
    }
    private void renderRequirements(GuiGraphicsExtractor graphics, DesktopContextualMenu.Recipe recipe) {
        int left = (int)ContextualPresentationLayout.MATERIALS.x(), top = (int)ContextualPresentationLayout.MATERIALS.y();
        int y = top + 31;
        for (var ingredient : recipe.requirements()) {
            if (y + 19 > top + ContextualPresentationLayout.MATERIALS.height() - 6) break;
            ItemStack icon = ingredient.alternatives().isEmpty() ? ItemStack.EMPTY : ingredient.alternatives().getFirst();
            graphics.item(icon, left + 10, y);
            graphics.text(Minecraft.getInstance().font, truncate(icon.getHoverName().getString(), 145), left + 31, y, 0xFF29251D, false);
            graphics.text(Minecraft.getInstance().font, ingredient.available() + " / " + ingredient.required(), left + 31, y + 10,
                ingredient.available() >= ingredient.required() ? 0xFF1D5120 : 0xFF8F1E19, false);
            y += 22;
        }
    }
    private void renderSelectedRecipe(GuiGraphicsExtractor graphics, DesktopContextualMenu.Recipe recipe) {
        text(graphics, truncate(recipe.output().getHoverName().getString(), 170), 380, 235, ContextualRadialAssets.theme().text());
        text(graphics, recipe.craftable() ? "Ready" : "Missing items", 380, 253,
            recipe.craftable() ? 0xFF1D5120 : 0xFF8F1E19);
    }
    private void renderCategories(GuiGraphicsExtractor graphics) {
        int count = snapshot.categories().size();
        int spacing = Math.min(44, 270 / Math.max(1, count));
        for (int i = 0; i < count; i++) {
            boolean selected = i == category;
            int x = 380 + (2 * i - count + 1) * spacing / 2;
            categoryTile(graphics, snapshot.categories().get(i).icon(), x, 59, selected);
        }
    }
    private static void categoryTile(GuiGraphicsExtractor graphics, ItemStack item, int x, int y, boolean selected) {
        var theme = ContextualRadialAssets.theme();
        int half = selected ? 17 : 12;
        ContextualRadialAssets.surface(graphics, selected ? Element.SELECTED_CATEGORY : Element.CATEGORY,
                x - half, y - half, half * 2, half * 2, selected ? theme.hover() : theme.border(), theme.card());
        itemIcon(graphics, item, x, y, selected ? 1.3f : 1);
    }
    private static void tabTile(GuiGraphicsExtractor graphics, ItemStack item, int x, int y, boolean hovered, boolean active) {
        int half = hovered || active ? 16 : 12;
        for (var layer : CreativeTabLayers.ordered(hovered, active)) {
            if (layer == CreativeTabLayers.Layer.HOVER) ContextualRadialAssets.texture(graphics, Element.HOVERED_CREATIVE_TAB,
                x - half - 1, y - half - 1, half * 2 + 2, half * 2 + 2);
            else ContextualRadialAssets.texture(graphics, layer == CreativeTabLayers.Layer.ACTIVE ? Element.ACTIVE_CREATIVE_TAB : Element.CREATIVE_TAB,
                x - half, y - half, half * 2, half * 2);
        }
        itemIcon(graphics, item, x, y - (active ? 1 : 0), hovered || active ? 1.2f : 1);
    }
    private static void itemIcon(GuiGraphicsExtractor graphics, ItemStack item, int x, int y, float scale) {
        graphics.pose().pushMatrix();
        try {
            graphics.pose().translate(x, y); graphics.pose().scale(scale, scale);
            graphics.item(item, -8, -8);
            graphics.itemDecorations(Minecraft.getInstance().font, item, -8, -8);
        } finally { graphics.pose().popMatrix(); }
    }
    private static int radialX(int index, int size, int radius) { return 380 + (int) Math.round(Math.sin((index + 0.5) * Math.PI * 2 / Math.max(1, size)) * radius); }
    private static int radialY(int index, int size, int radius) { return 221 - (int) Math.round(Math.cos((index + 0.5) * Math.PI * 2 / Math.max(1, size)) * radius); }
    private static void tile(GuiGraphicsExtractor graphics, ItemStack item, int x, int y, boolean selected, boolean craftable) {
        int half = selected ? 17 : 13;
        ContextualRadialAssets.texture(graphics, craftable ? Element.RECIPE_CARD : Element.UNAVAILABLE_RECIPE_CARD,
            x - half, y - half, half * 2, half * 2);
        if (selected) ContextualRadialAssets.texture(graphics, Element.HOVERED_CARD, x - half, y - half, half * 2, half * 2);
        itemIcon(graphics, item, x, y, selected ? 1.2f : 1);
    }
    private static void text(GuiGraphicsExtractor graphics, String value, int x, int y, int color) {
        var font = Minecraft.getInstance().font;
        graphics.text(font, value, x - font.width(value) / 2, y, color, false);
    }
    private static String truncate(String value, int width) {
        var font = Minecraft.getInstance().font;
        return font.width(value) <= width ? value : font.plainSubstrByWidth(value, Math.max(1, width - font.width("…"))) + "…";
    }
}
