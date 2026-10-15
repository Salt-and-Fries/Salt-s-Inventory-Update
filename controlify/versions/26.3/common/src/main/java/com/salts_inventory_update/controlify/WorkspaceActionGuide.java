package com.salts_inventory_update.controlify;

import com.salts_inventory_update.api.client.desktop.DesktopContextualMenu;
import com.salts_inventory_update.controlify.input.WorkspaceContainerScreen;
import com.salts_inventory_update.controlify.input.ControlifyInputBridge;
import com.salts_inventory_update.controlify.model.CompactGuideLayout;
import dev.isxander.controlify.api.ControlifyApi;
import dev.isxander.controlify.api.bind.InputBinding;
import dev.isxander.controlify.api.bind.InputBindingSupplier;
import dev.isxander.controlify.api.contextual.*;
import dev.isxander.controlify.bindings.ControlifyBindings;
import dev.isxander.controlify.bindings.InputBindingImpl;
import dev.isxander.controlify.bindings.input.Input;
import dev.isxander.controlify.bindings.input.InputType;
import dev.isxander.controlify.contextual.*;
import dev.isxander.controlify.controller.ControllerEntity;
import dev.isxander.controlify.controller.input.ControllerStateView;
import dev.isxander.controlify.controller.input.GamepadInputs;
import dev.isxander.controlify.font.BindingFontHelper;
import dev.isxander.controlify.gui.guide.GuideInstanceImpl;
import dev.isxander.controlify.gui.guide.PrecomputedLines;
import java.util.*;
import java.util.function.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** A workspace-only native Controlify guide instance. Other screens retain their own guides. */
public final class WorkspaceActionGuide {
    public static final Identifier DOMAIN_ID = id("workspace");
    private static final Identifier CONTAINER_DOMAIN = Identifier.fromNamespaceAndPath("controlify", "container");
    private static final Identifier IN_GAME_DOMAIN = Identifier.fromNamespaceAndPath("controlify", "in_game");
    private static final Identifier ORDINARY_SIDE = id("guide/ordinary_side");
    private static ContextualDomainImpl<GuideContext> domain;
    private static final Map<WorkspaceContainerScreen, Session> sessions = new WeakHashMap<>();
    private static final Map<GuideInstance<?>, TuckedSession> tuckedSessions = new WeakHashMap<>();
    private static final Map<WorkspaceContainerScreen, Reservation> reservations = new WeakHashMap<>();

    public enum Mode { SIDE, LT_RADIAL, RECIPE, COOKING, CREATIVE_TABS }
    public record State(Mode mode, DesktopContextualMenu.Kind quickAction, boolean highlightedWindow, boolean tuckHighlighted,
                        boolean closableHighlight, boolean sortableFocus, boolean creativeCatalogItem, boolean scrollableHover,
                        boolean canConfirmContextual, Slot hoveredSlot, ItemStack carried) {
        public State { carried = carried == null ? ItemStack.EMPTY : carried; }
    }
    public record GuideContext(State state, ControllerEntity controller, GuideVerbosity verbosity) implements Context { }
    private record Session(ControllerEntity controller, Font font, GuideInstanceImpl<GuideContext> instance) { }
    private record TuckedSession(ControllerEntity controller, Font font, GuideInstanceImpl<InGameContext> instance) { }
    private record Definition(String fact, InputBindingSupplier binding, GuideLocation location, String text) { }
    private record CompactEntry(Component glyph, Component label, int width, int height) { }
    private record Reservation(Mode mode, int width, int height, double scale, int band) { }

    private static final InputBindingSupplier WINDOW_WHEEL = fixed("window_wheel", GamepadInputs.LEFT_TRIGGER_AXIS);
    private static final InputBindingSupplier WINDOW_CYCLE = fixed("window_cycle", GamepadInputs.LEFT_SHOULDER_BUTTON, GamepadInputs.RIGHT_SHOULDER_BUTTON);
    private static final InputBindingSupplier LEFT_STICK = fixed("left_stick", GamepadInputs.LEFT_STICK_AXIS_UP, GamepadInputs.LEFT_STICK_AXIS_RIGHT,
        GamepadInputs.LEFT_STICK_AXIS_DOWN, GamepadInputs.LEFT_STICK_AXIS_LEFT);
    private static final InputBindingSupplier RIGHT_STICK = fixed("right_stick", GamepadInputs.RIGHT_STICK_AXIS_UP, GamepadInputs.RIGHT_STICK_AXIS_RIGHT,
        GamepadInputs.RIGHT_STICK_AXIS_DOWN, GamepadInputs.RIGHT_STICK_AXIS_LEFT);
    private static final InputBindingSupplier SCROLL = fixed("scroll", GamepadInputs.RIGHT_STICK_AXIS_UP, GamepadInputs.RIGHT_STICK_AXIS_DOWN);
    private static final InputBindingSupplier CATEGORY = fixed("category", GamepadInputs.DPAD_LEFT_BUTTON, GamepadInputs.DPAD_RIGHT_BUTTON);
    private static final InputBindingSupplier FILTER = fixed("filter", GamepadInputs.DPAD_UP_BUTTON);
    private static InputBindingSupplier NAVIGATION;
    private static InputBindingSupplier CURSOR;
    private static List<Definition> DEFINITIONS;

    /** Native bindings read Minecraft options during class initialization; wait for a live workspace. */
    private static void ensureBindings() {
        if (DEFINITIONS != null) return;
        NAVIGATION = composite("navigation", ControlifyBindings.VMOUSE_SNAP_UP,
            ControlifyBindings.VMOUSE_SNAP_RIGHT, ControlifyBindings.VMOUSE_SNAP_DOWN, ControlifyBindings.VMOUSE_SNAP_LEFT);
        CURSOR = composite("cursor", ControlifyBindings.VMOUSE_MOVE_UP, ControlifyBindings.VMOUSE_MOVE_RIGHT,
            ControlifyBindings.VMOUSE_MOVE_DOWN, ControlifyBindings.VMOUSE_MOVE_LEFT);
        DEFINITIONS = List.of(
        row("close_all", ControlifyBindings.GUI_BACK, GuideLocation.LEFT, "Close all"),
        row("close_window", ControlifyBindings.GUI_BACK, GuideLocation.LEFT, "Close"),
        row("hold_windows", WINDOW_WHEEL, GuideLocation.LEFT, "Windows"),
        row("release_opening", WINDOW_WHEEL, GuideLocation.LEFT, "Release"),
        row("release_window", WINDOW_WHEEL, GuideLocation.LEFT, "Show"),
        row("release_tuck", WINDOW_WHEEL, GuideLocation.LEFT, "Tuck"),
        row("release_empty", WINDOW_WHEEL, GuideLocation.LEFT, "Return"),
        row("focus_window", ControlifyBindings.INV_SELECT, GuideLocation.LEFT, "Focus"),
        row("choose_window", LEFT_STICK, GuideLocation.RIGHT, "Choose"),
        row("cycle_windows", WINDOW_CYCLE, GuideLocation.RIGHT, "Cycle"),
        row("sort_focus", ControlifyBindings.INVENTORY, GuideLocation.RIGHT, "Sort/Force"),
        row("move_cursor", CURSOR, GuideLocation.RIGHT, "Cursor"),
        row("navigate", NAVIGATION, GuideLocation.RIGHT, "Navigate"),
        row("navigate_scroll", NAVIGATION, GuideLocation.RIGHT, "Navigate"),
        row("scroll", SCROLL, GuideLocation.RIGHT, "Scroll"),
        row("hold_recipe", ContextualRecipeBinding.OPEN_WHEEL, GuideLocation.LEFT, "Recipes"),
        row("hold_cooking", ContextualRecipeBinding.OPEN_WHEEL, GuideLocation.LEFT, "Cooking"),
        row("hold_tabs", ContextualRecipeBinding.OPEN_WHEEL, GuideLocation.LEFT, "Tabs"),
        row("return_pair", ContextualRecipeBinding.OPEN_WHEEL, GuideLocation.LEFT, "Return"),
        row("craft", ControlifyBindings.INV_SELECT, GuideLocation.LEFT, "Craft"),
        row("prepare_cooking", ControlifyBindings.INV_SELECT, GuideLocation.LEFT, "Load"),
        row("select_tab", ControlifyBindings.INV_SELECT, GuideLocation.LEFT, "Select"),
        row("creative_one", ControlifyBindings.INV_SELECT, GuideLocation.LEFT, "Add one"),
        row("creative_stack", ControlifyBindings.INVENTORY, GuideLocation.LEFT, "Add stack"),
        row("browse_recipes", RIGHT_STICK, GuideLocation.RIGHT, "Browse"),
        row("choose_recipe", LEFT_STICK, GuideLocation.RIGHT, "Choose"),
        row("rotate_recipes", WINDOW_CYCLE, GuideLocation.RIGHT, "Rotate"),
        row("categories", CATEGORY, GuideLocation.RIGHT, "Category"),
        row("craftable", FILTER, GuideLocation.RIGHT, "Craftable"),
        row("tab_stick", LEFT_STICK, GuideLocation.RIGHT, "Choose"),
        row("tab_right_stick", RIGHT_STICK, GuideLocation.RIGHT, "Choose"),
        row("tab_dpad", CATEGORY, GuideLocation.RIGHT, "Cycle")
        );
    }

    private WorkspaceActionGuide() { }

    /** Register from Controlify's pre-init entrypoint, after its platform is ready. */
    @SuppressWarnings("unchecked") public static void initialize(ContextualDomainRegistry registry) {
        if (domain != null) return;
        domain = (ContextualDomainImpl<GuideContext>) (Object) registry.<GuideContext>register(DOMAIN_ID);
        domain.registerContributor(WorkspaceActionGuide::contribute);
    }

    public static void update(WorkspaceContainerScreen screen, ControllerEntity controller) {
        var minecraft = Minecraft.getInstance();
        if (minecraft.gui.screen() != screen || !ControlifyApi.get().currentInputMode().isController()) return;
        if (domain == null) throw new IllegalStateException("Workspace guide must register through Controlify pre-init before use");
        ensureBindings();
        State state = screen.controllerGuideState();
        if (state == null) return;
        Session session = sessions.get(screen);
        if (session == null || session.controller() != controller || session.font() != minecraft.font) {
            var instance = new GuideInstanceImpl<>(domain, () -> rules(controller), minecraft.font);
            session = new Session(controller, minecraft.font, instance); sessions.put(screen, session);
            reservations.remove(screen);
        }
        session.instance().update(new GuideContext(state, controller, controller.settings().generic.guide.verbosity));
    }

    public static void render(WorkspaceContainerScreen screen, GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        ControllerEntity controller = ControlifyApi.get().getCurrentController().orElse(null);
        if (controller == null || !ControlifyApi.get().currentInputMode().isController() || !controller.settings().generic.guide.showScreenGuides) return;
        update(screen, controller);
        Session session = sessions.get(screen);
        if (session != null) renderCompact(graphics, session.instance(), false, controller.settings().generic.guide.screenGuiScale);
    }

    /** Actual compact row clearance; retain its high-water mark within one mode to avoid hover oscillation. */
    public static int reservedBottomSpace(WorkspaceContainerScreen screen) {
        var minecraft = Minecraft.getInstance();
        ControllerEntity controller = ControlifyApi.get().getCurrentController().orElse(null);
        if (controller == null || minecraft.gui.screen() != screen || !ControlifyApi.get().currentInputMode().isController()
                || !controller.settings().generic.guide.showScreenGuides) return 0;
        State state = screen.controllerGuideState();
        if (state == null) return 0;
        update(screen, controller);
        Session session = sessions.get(screen);
        if (session == null) return 0;
        var window = minecraft.getWindow();
        int width = window.getGuiScaledWidth(), height = window.getGuiScaledHeight();
        int guideScale = controller.settings().generic.guide.screenGuiScale;
        var layout = compactLayout(session.instance(), width, height, guideScale);
        double scale = guideScaleRatio(guideScale);
        Reservation previous = reservations.get(screen);
        int band = layout.reservedHeight();
        if (previous != null && previous.mode() == state.mode() && previous.width() == width
                && previous.height() == height && previous.scale() == scale) band = Math.max(band, previous.band());
        reservations.put(screen, new Reservation(state.mode(), width, height, scale, band));
        return band;
    }

    /** Native preferences choose the starting scale; compact wrapping may reduce it to fit. */
    private static double guideScaleRatio(int guideScale) {
        var minecraft = Minecraft.getInstance();
        var window = minecraft.getWindow();
        int currentScale = window.getGuiScale();
        if (guideScale == -1) guideScale = currentScale;
        else if (guideScale == 0) guideScale = Math.min(window.calculateScale(0, minecraft.isEnforceUnicode()) - 1, currentScale);
        else guideScale = window.calculateScale(guideScale, minecraft.isEnforceUnicode());
        return guideScale > 0 && guideScale != currentScale ? (double) guideScale / currentScale : 1;
    }

    private static CompactGuideLayout.Layout compactLayout(GuideInstanceImpl<?> nativeGuide, int width, int height, int guiScale) {
        List<CompactEntry> entries = compactEntries(nativeGuide);
        return CompactGuideLayout.compute(entries.stream().map(entry -> new CompactGuideLayout.Size(entry.width(), entry.height())).toList(),
            Math.max(1, width), Math.max(1, height), guideScaleRatio(guiScale));
    }

    /** Render the resolved native components horizontally instead of accumulating corner columns. */
    private static void renderCompact(GuiGraphicsExtractor graphics, GuideInstanceImpl<?> nativeGuide, boolean contrast, int guiScale) {
        List<CompactEntry> entries = compactEntries(nativeGuide);
        var layout = compactLayout(nativeGuide, graphics.guiWidth(), graphics.guiHeight(), guiScale);
        var font = Minecraft.getInstance().font;
        graphics.nextStratum();
        for (var placement : layout.entries()) {
            CompactEntry entry = entries.get(placement.index());
            if (contrast) graphics.fill((int) placement.x() - 1, (int) placement.y() - 1,
                (int) Math.ceil(placement.x() + placement.width()) + 1,
                (int) Math.ceil(placement.y() + placement.height()) + 1, 0x80000000);
            graphics.pose().pushMatrix();
            try {
                graphics.pose().translate((float) placement.x(), (float) placement.y());
                graphics.pose().scale((float) layout.scale(), (float) layout.scale());
                compactEntry(graphics, font, entry.glyph(), entry.label(), 0, 0, entry.width(), entry.height());
            } finally { graphics.pose().popMatrix(); }
        }
    }

    private static void compactEntry(GuiGraphicsExtractor graphics, Font font, Component glyph, Component label,
            int x, int y, int width, int height) {
        graphics.text(font, Component.empty().append(glyph).append(" ").append(label), x, y, 0xFFFFFFFF, true);
    }

    private static List<CompactEntry> compactEntries(GuideInstanceImpl<?> nativeGuide) {
        var font = Minecraft.getInstance().font;
        List<CompactEntry> entries = new ArrayList<>();
        appendCompactEntries(entries, nativeGuide.leftGuides(), !font.isBidirectional(), font);
        appendCompactEntries(entries, nativeGuide.rightGuides(), font.isBidirectional(), font);
        return entries;
    }

    private static void appendCompactEntries(List<CompactEntry> target, PrecomputedLines lines, boolean glyphFirst, Font font) {
        // Pinned GuideInstanceImpl builds each line as glyph/space/label or its bidi-aware reverse.
        for (var line : lines.lines()) {
            var parts = line.text().getSiblings();
            if (parts.size() != 3) throw new IllegalStateException("Unexpected native guide component structure");
            Component glyph = parts.get(glyphFirst ? 0 : 2), label = parts.get(glyphFirst ? 2 : 0);
            Component text = Component.empty().append(glyph).append(" ").append(label);
            target.add(new CompactEntry(glyph, label, Math.max(1, font.width(text)), Math.max(9, line.height())));
        }
    }

    /** Called within the native gameplay guide's own HUD visibility, placement and style gates. */
    @SuppressWarnings("unchecked")
    public static boolean renderTucked(GuideInstance<?> original, GuiGraphicsExtractor graphics,
            boolean bottomAligned, boolean textContrast, int guiScale) {
        var minecraft = Minecraft.getInstance();
        if (!SaltsControlifyClient.tuckedWindowsAvailable() || minecraft.player == null || minecraft.level == null
                || !ControlifyApi.get().currentInputMode().isController()) return false;
        ControllerEntity controller = ControlifyApi.get().getCurrentController().orElse(null);
        if (controller == null) return false;
        TuckedSession session = tuckedSessions.get(original);
        if (session == null || session.controller() != controller || session.font() != minecraft.font) {
            var nativeDomain = (ContextualDomainImpl<InGameContext>) (Object) ContextualDomains.INSTANCE.inGame();
            session = new TuckedSession(controller, minecraft.font, new GuideInstanceImpl<>(nativeDomain,
                WorkspaceActionGuide::tuckedRules, minecraft.font));
            tuckedSessions.put(original, session);
        }
        session.instance().update(InGameContext.create(minecraft, controller));
        session.instance().extractRenderState(graphics, bottomAligned, textContrast, guiScale);
        return true;
    }

    private static RuleEngine<GuideRule.Key, GuideRule> tuckedRules() {
        List<GuideRule> rules = new ArrayList<>();
        GuideLocation inventoryLocation = GuideLocation.LEFT;
        var nativeRules = RuleSetManager.GUIDE_RULES.getRuleEngine(Minecraft.getInstance().level, IN_GAME_DOMAIN)
            .orElseGet(() -> new RuleEngine<>(List.of()));
        for (GuideRule rule : nativeRules.rules()) {
            if (Objects.equals(rule.binding().bindId(), ControlifyBindings.INVENTORY.bindId())) {
                inventoryLocation = rule.location();
            } else rules.add(rule);
        }
        rules.add(new GuideRule(ControlifyBindings.INVENTORY, inventoryLocation, new ContextualPredicate.Static(true),
            Component.translatableWithFallback("salts_inventory_controlify.guide.restore_windows", "Restore")));
        return new RuleEngine<>(rules);
    }

    private static void contribute(GuideContext context, ContextualStateSink sink) {
        State state = context.state();
        var controller = context.controller();
        boolean side = state.mode() == Mode.SIDE, windowWheel = state.mode() == Mode.LT_RADIAL;
        boolean recipes = state.mode() == Mode.RECIPE || state.mode() == Mode.COOKING;
        boolean tabs = state.mode() == Mode.CREATIVE_TABS;
        boolean select = !conflictsWithClose(controller, ControlifyBindings.INV_SELECT);
        boolean inventory = !conflictsWithClose(controller, ControlifyBindings.INVENTORY);
        boolean contextual = !conflictsWithClose(controller, ContextualRecipeBinding.OPEN_WHEEL);
        boolean opening = ControlifyInputBridge.awaitingOpeningTriggerRelease();
        boolean contextualOwnsWindowTrigger = state.quickAction() != DesktopContextualMenu.Kind.NONE && contextual
            && sameBinding(controller, ContextualRecipeBinding.OPEN_WHEEL, WINDOW_WHEEL);
        Map<String, Boolean> facts = new HashMap<>();
        facts.put("close_all", !windowWheel);
        facts.put("close_window", windowWheel && state.closableHighlight());
        facts.put("hold_windows", side && !opening && !conflictsWithClose(controller, WINDOW_WHEEL) && !contextualOwnsWindowTrigger);
        facts.put("release_opening", side && opening);
        facts.put("release_window", windowWheel && state.highlightedWindow());
        facts.put("release_tuck", windowWheel && state.tuckHighlighted());
        facts.put("release_empty", windowWheel && !state.highlightedWindow() && !state.tuckHighlighted());
        facts.put("focus_window", windowWheel && state.highlightedWindow() && select);
        facts.put("choose_window", windowWheel);
        facts.put("cycle_windows", side && !conflictsWithClose(controller, WINDOW_CYCLE));
        facts.put("sort_focus", windowWheel && state.sortableFocus() && inventory);
        facts.put("move_cursor", side);
        facts.put("navigate", side && !state.scrollableHover());
        facts.put("navigate_scroll", side && state.scrollableHover());
        facts.put("scroll", side && state.scrollableHover());
        InputBinding contextualBinding = ContextualRecipeBinding.OPEN_WHEEL.onOrNull(controller);
        boolean openingBlocksContextual = opening && contextualBinding != null
            && contextualBinding.boundInput().getRelevantInputs().contains(GamepadInputs.LEFT_TRIGGER_AXIS);
        boolean quickActionReady = contextual && !openingBlocksContextual;
        facts.put("hold_recipe", side && state.quickAction() == DesktopContextualMenu.Kind.CRAFTING && quickActionReady);
        facts.put("hold_cooking", side && state.quickAction() == DesktopContextualMenu.Kind.COOKING && quickActionReady);
        facts.put("hold_tabs", side && state.quickAction() == DesktopContextualMenu.Kind.CREATIVE && quickActionReady);
        facts.put("return_pair", (recipes || tabs) && contextual);
        facts.put("craft", state.mode() == Mode.RECIPE && state.canConfirmContextual() && select);
        facts.put("prepare_cooking", state.mode() == Mode.COOKING && state.canConfirmContextual() && select);
        facts.put("select_tab", tabs && state.canConfirmContextual() && select);
        facts.put("creative_one", side && state.creativeCatalogItem() && select && !sameBinding(controller, ControlifyBindings.INV_SELECT, ControlifyBindings.INVENTORY));
        facts.put("creative_stack", side && state.creativeCatalogItem() && inventory);
        facts.put("browse_recipes", recipes);
        facts.put("choose_recipe", recipes);
        facts.put("rotate_recipes", recipes);
        facts.put("categories", recipes);
        facts.put("craftable", recipes);
        facts.put("tab_stick", tabs);
        facts.put("tab_right_stick", tabs);
        facts.put("tab_dpad", tabs);
        for (Definition definition : DEFINITIONS) sink.contributeFact(id("guide/" + definition.fact()), facts.getOrDefault(definition.fact(), false));
        sink.contributeFact(ORDINARY_SIDE, side && !state.creativeCatalogItem());

        // Resolve native item facts and resource-pack rules through the unchanged container domain.
        // Its guide instance/renderables are never mutated by this workspace domain.
        var nativeContext = new ContainerContext(Minecraft.getInstance().player, state.hoveredSlot(), state.carried(),
            false, controller, context.verbosity());
        var nativeRules = nativeRules();
        @SuppressWarnings("unchecked") var container = (ContextualDomainImpl<ContainerContext>) (Object) ContextualDomains.INSTANCE.container();
        var nativeState = container.calculateState(nativeContext, nativeRules.factDependencies());
        nativeState.facts().forEach(sink::contributeFact);
        nativeState.items().forEach(sink::contributeItem);
    }

    private static RuleEngine<GuideRule.Key, GuideRule> rules(ControllerEntity controller) {
        List<GuideRule> rules = new ArrayList<>();
        for (Definition definition : DEFINITIONS) rules.add(new GuideRule(definition.binding(), definition.location(),
            new ContextualPredicate.Fact(id("guide/" + definition.fact())), definitionText(definition, controller)));
        for (GuideRule rule : nativeRules().rules()) {
            if (conflictsWithClose(controller, rule.binding())) continue;
            var condition = new ContextualPredicate.Compound(Optional.of(Set.of(rule.predicate(), new ContextualPredicate.Fact(ORDINARY_SIDE))), Optional.empty(), Optional.empty());
            rules.add(new GuideRule(rule.binding(), rule.location(), condition, compactNativeText(rule.text())));
        }
        return new RuleEngine<>(rules);
    }

    private static Component definitionText(Definition definition, ControllerEntity controller) {
        String variant = "", text = definition.text();
        if (definition.fact().equals("cycle_windows") || definition.fact().equals("rotate_recipes")) {
            InputBinding cycle = WINDOW_CYCLE.onOrNull(controller);
            List<Identifier> inputs = cycle == null ? List.of() : cycle.boundInput().getRelevantInputs();
            boolean previous = inputs.contains(GamepadInputs.LEFT_SHOULDER_BUTTON), next = inputs.contains(GamepadInputs.RIGHT_SHOULDER_BUTTON);
            if (previous != next) {
                if (definition.fact().equals("cycle_windows")) {
                    variant = previous ? ".previous" : ".next";
                    text = previous ? "Previous" : "Next";
                } else {
                    variant = previous ? ".counterclockwise" : ".clockwise";
                    text = previous ? "Rotate left" : "Rotate right";
                }
            }
        }
        return Component.translatableWithFallback("salts_inventory_controlify.guide." + definition.fact() + variant, text);
    }

    private static Component compactNativeText(Component original) {
        if (!(original.getContents() instanceof TranslatableContents contents)) return original;
        String key = contents.getKey();
        if (!key.startsWith("controlify.guide.container.")) return original;
        String action = key.substring("controlify.guide.container.".length());
        String label = switch (action) {
            case "exit" -> "Close"; case "place_all" -> "Place all"; case "place_one", "take_one" -> "Place one";
            case "swap" -> "Swap"; case "drop" -> "Drop"; case "take" -> "Take";
            case "quick_move" -> "Move"; case "take_from_bundle" -> "Bundle"; case "take_half" -> "Take half";
            default -> null;
        };
        return label == null ? original : Component.translatableWithFallback("salts_inventory_controlify.guide.item." + action, label);
    }

    private static RuleEngine<GuideRule.Key, GuideRule> nativeRules() {
        return RuleSetManager.GUIDE_RULES.getRuleEngine(Minecraft.getInstance().level, CONTAINER_DOMAIN).orElseGet(() -> new RuleEngine<>(List.of()));
    }

    private static boolean conflictsWithClose(ControllerEntity controller, InputBindingSupplier binding) {
        if (binding instanceof GlyphSupplier glyph) {
            List<Identifier> physical = glyph.inputs.apply(controller);
            return physical.size() == 1 && Objects.equals(physical.getFirst(), closePhysicalInput(controller));
        }
        return sameBinding(controller, binding, ControlifyBindings.GUI_BACK);
    }
    private static boolean sameBinding(ControllerEntity controller, InputBindingSupplier first, InputBindingSupplier second) {
        InputBinding a = first.onOrNull(controller), b = second.onOrNull(controller);
        if (a == null || b == null || a.isUnbound() || b.isUnbound()) return false;
        Input firstInput = a.boundInput(), secondInput = b.boundInput();
        // Native inputs retain their semantic equality. Only a single fixed physical input
        // can compare metadata with a real binding; composite glyph lists are not actions.
        boolean singleFixedInput = firstInput instanceof GlyphInput && firstInput.getRelevantInputs().size() == 1
            || secondInput instanceof GlyphInput && secondInput.getRelevantInputs().size() == 1;
        return Objects.equals(firstInput, secondInput) || singleFixedInput
            && firstInput.getRelevantInputs().equals(secondInput.getRelevantInputs());
    }
    private static Definition row(String fact, InputBindingSupplier binding, GuideLocation location, String text) {
        return new Definition(fact, binding, location, text);
    }
    private static Identifier id(String path) { return Identifier.fromNamespaceAndPath("salts_inventory_controlify", path); }
    private static InputBindingSupplier fixed(String name, Identifier... inputs) { return new GlyphSupplier(name, controller -> List.of(inputs)); }
    private static InputBindingSupplier composite(String name, InputBindingSupplier... bindings) {
        return new GlyphSupplier(name, controller -> Arrays.stream(bindings).map(binding -> binding.onOrNull(controller)).filter(Objects::nonNull)
            .filter(binding -> !binding.isUnbound()).flatMap(binding -> binding.boundInput().getRelevantInputs().stream()).distinct().toList());
    }

    private static Identifier closePhysicalInput(ControllerEntity controller) {
        InputBinding close = ControlifyBindings.GUI_BACK.onOrNull(controller);
        if (close == null || close.isUnbound()) return null;
        List<Identifier> inputs = close.boundInput().getRelevantInputs();
        return inputs.size() == 1 ? inputs.getFirst() : null;
    }

    /** Unregistered metadata bindings cannot be remapped or persisted independently of real inputs. */
    private static final class GlyphSupplier implements InputBindingSupplier {
        private record Cached(List<Identifier> inputs, InputBinding binding) { }
        private final Identifier id;
        private final Function<ControllerEntity, List<Identifier>> inputs;
        private final Map<ControllerEntity, Cached> cache = new WeakHashMap<>();
        private GlyphSupplier(String name, Function<ControllerEntity, List<Identifier>> inputs) { this.id = id("guide_input/" + name); this.inputs = inputs; }
        @Override public InputBinding onOrNull(ControllerEntity controller) {
            Identifier blocked = closePhysicalInput(controller);
            List<Identifier> actual = inputs.apply(controller).stream().filter(input -> !Objects.equals(input, blocked)).toList();
            if (actual.isEmpty()) return null;
            Cached cached = cache.get(controller);
            if (cached == null || !cached.inputs().equals(actual)) {
                Input metadata = new GlyphInput(plainStickInputs(actual));
                InputBinding binding = new InputBindingImpl(null, controller.info().type().namespace(), id, Component.empty(), Component.empty(),
                    Component.empty(), () -> metadata, Set.of(), false);
                cached = new Cached(actual, binding); cache.put(controller, cached);
            }
            return cached.binding();
        }
        @Override public Identifier bindId() { return id; }
        @Override public Component inputGlyph() {
            return ControlifyApi.get().getCurrentController().map(controller -> onOrNull(controller)).map(InputBinding::inputGlyph).orElseGet(Component::empty);
        }
    }

    /** Native undirected joystick glyphs; metadata never turns these display IDs into button actions. */
    private static List<Identifier> plainStickInputs(List<Identifier> physical) {
        List<Identifier> display = new ArrayList<>();
        for (Identifier input : physical) {
            Identifier glyph = input;
            if (input.equals(GamepadInputs.LEFT_STICK_AXIS_UP) || input.equals(GamepadInputs.LEFT_STICK_AXIS_RIGHT)
                    || input.equals(GamepadInputs.LEFT_STICK_AXIS_DOWN) || input.equals(GamepadInputs.LEFT_STICK_AXIS_LEFT))
                glyph = GamepadInputs.LEFT_STICK_BUTTON;
            else if (input.equals(GamepadInputs.RIGHT_STICK_AXIS_UP) || input.equals(GamepadInputs.RIGHT_STICK_AXIS_RIGHT)
                    || input.equals(GamepadInputs.RIGHT_STICK_AXIS_DOWN) || input.equals(GamepadInputs.RIGHT_STICK_AXIS_LEFT))
                glyph = GamepadInputs.RIGHT_STICK_BUTTON;
            if (!display.contains(glyph)) display.add(glyph);
        }
        return List.copyOf(display);
    }
    private record GlyphInput(List<Identifier> inputs) implements Input {
        private GlyphInput { inputs = List.copyOf(inputs); }
        @Override public float state(ControllerStateView state) { return 0; }
        @Override public List<Identifier> getRelevantInputs() { return inputs; }
        // This metadata input is neither registered, sampled, saved nor serialized.
        @Override public InputType<?> type() { return GamepadInputs.getBind(inputs.getFirst()).type(); }
    }
}
