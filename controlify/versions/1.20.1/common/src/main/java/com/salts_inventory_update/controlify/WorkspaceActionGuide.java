package com.salts_inventory_update.controlify;

import com.salts_inventory_update.api.client.desktop.DesktopContextualMenu;
import com.salts_inventory_update.api.client.gui.GuiGraphicsExtractor;
import com.salts_inventory_update.controlify.input.WorkspaceContainerScreen;
import com.salts_inventory_update.controlify.input.ControlifyInputBridge;
import com.salts_inventory_update.controlify.model.CompactGuideLayout;
import dev.isxander.controlify.Controlify;
import dev.isxander.controlify.api.ControlifyApi;
import dev.isxander.controlify.api.bind.InputBinding;
import dev.isxander.controlify.api.bind.InputBindingSupplier;
import dev.isxander.controlify.bindings.ControlifyBindings;
import dev.isxander.controlify.bindings.InputBindingImpl;
import dev.isxander.controlify.bindings.input.Input;
import dev.isxander.controlify.bindings.input.InputType;
import dev.isxander.controlify.controller.ControllerEntity;
import dev.isxander.controlify.controller.input.ControllerStateView;
import dev.isxander.controlify.controller.input.GamepadInputs;
import dev.isxander.controlify.font.BindingFontHelper;
import java.util.*;
import java.util.function.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Native Controlify 2.x glyphs arranged in the compact workspace action guide. */
public final class WorkspaceActionGuide {
    private enum Location { LEFT, RIGHT }
    private static final String WHOLE_DPAD="salts_inventory_controlify:whole_dpad";
    private record Definition(String fact, InputBindingSupplier binding, Location location, String text) { }
    private record Entry(Component glyph, Component label, int width, int height) { }
    private record Reservation(Mode mode,int width,int height,int band) { }
    private static final Map<WorkspaceContainerScreen,Reservation> reservations=new WeakHashMap<>();
    public enum Mode { SIDE, LT_RADIAL, RECIPE, COOKING, CREATIVE_TABS }
    public record State(Mode mode, DesktopContextualMenu.Kind quickAction, boolean highlightedWindow, boolean tuckHighlighted,
                        boolean closableHighlight, boolean sortableFocus, boolean creativeCatalogItem, boolean scrollableHover,
                        boolean canConfirmContextual, Slot hoveredSlot, ItemStack carried, boolean creativeHotbarDelete,
                        boolean creativeSearchAvailable) {
        public State(Mode mode, DesktopContextualMenu.Kind quickAction, boolean highlightedWindow, boolean tuckHighlighted,
                     boolean closableHighlight, boolean sortableFocus, boolean creativeCatalogItem, boolean scrollableHover,
                     boolean canConfirmContextual, Slot hoveredSlot, ItemStack carried, boolean creativeHotbarDelete) {
            this(mode, quickAction, highlightedWindow, tuckHighlighted, closableHighlight, sortableFocus, creativeCatalogItem,
                scrollableHover, canConfirmContextual, hoveredSlot, carried, creativeHotbarDelete, false);
        }
        public State(Mode mode, DesktopContextualMenu.Kind quickAction, boolean highlightedWindow, boolean tuckHighlighted,
                     boolean closableHighlight, boolean sortableFocus, boolean creativeCatalogItem, boolean scrollableHover,
                     boolean canConfirmContextual, Slot hoveredSlot, ItemStack carried) {
            this(mode, quickAction, highlightedWindow, tuckHighlighted, closableHighlight, sortableFocus, creativeCatalogItem,
                scrollableHover, canConfirmContextual, hoveredSlot, carried, false);
        }
        public State { carried = carried == null ? ItemStack.EMPTY : carried; }
    }
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
    private static InputBindingSupplier CREATIVE_CLEAR;
    private static List<Definition> DEFINITIONS;

    /** Native bindings read Minecraft options during class initialization; wait for a live workspace. */
    private static void ensureBindings() {
        if (DEFINITIONS != null) return;
        NAVIGATION = composite("navigation", ControlifyBindings.VMOUSE_SNAP_UP,
            ControlifyBindings.VMOUSE_SNAP_RIGHT, ControlifyBindings.VMOUSE_SNAP_DOWN, ControlifyBindings.VMOUSE_SNAP_LEFT);
        CURSOR = composite("cursor", ControlifyBindings.VMOUSE_MOVE_UP, ControlifyBindings.VMOUSE_MOVE_RIGHT,
            ControlifyBindings.VMOUSE_MOVE_DOWN, ControlifyBindings.VMOUSE_MOVE_LEFT);
        CREATIVE_CLEAR = composite("creative_clear", ControlifyBindings.INV_SELECT, ControlifyBindings.INVENTORY);
        DEFINITIONS = List.of(
        row("close_all", ControlifyBindings.GUI_BACK, Location.LEFT, "Close all"),
        row("close_window", ControlifyBindings.GUI_BACK, Location.LEFT, "Close"),
        row("hold_windows", WINDOW_WHEEL, Location.LEFT, "Windows"),
        row("release_opening", WINDOW_WHEEL, Location.LEFT, "Release"),
        row("release_window", WINDOW_WHEEL, Location.LEFT, "Show"),
        row("release_tuck", WINDOW_WHEEL, Location.LEFT, "Tuck"),
        row("release_empty", WINDOW_WHEEL, Location.LEFT, "Return"),
        row("focus_window", ControlifyBindings.INV_SELECT, Location.LEFT, "Focus"),
        row("choose_window", LEFT_STICK, Location.RIGHT, "Choose"),
        row("cycle_windows", WINDOW_CYCLE, Location.RIGHT, "Cycle"),
        row("sort_focus", ControlifyBindings.INVENTORY, Location.RIGHT, "Sort/Force"),
        row("move_cursor", CURSOR, Location.RIGHT, "Cursor"),
        row("navigate", NAVIGATION, Location.RIGHT, "Navigate"),
        row("navigate_scroll", NAVIGATION, Location.RIGHT, "Navigate"),
        row("scroll", SCROLL, Location.RIGHT, "Scroll"),
        row("hold_recipe", ContextualRecipeBinding.OPEN_WHEEL, Location.LEFT, "Recipes"),
        row("hold_cooking", ContextualRecipeBinding.OPEN_WHEEL, Location.LEFT, "Cooking"),
        row("hold_tabs", ContextualRecipeBinding.OPEN_WHEEL, Location.LEFT, "Tabs"),
        row("return_pair", ContextualRecipeBinding.OPEN_WHEEL, Location.LEFT, "Return"),
        row("craft", ControlifyBindings.INV_SELECT, Location.LEFT, "Craft"),
        row("craft_stack", ControlifyBindings.INV_QUICK_MOVE, Location.LEFT, "Craft Stack"),
        row("prepare_cooking", ControlifyBindings.INV_SELECT, Location.LEFT, "Load"),
        row("select_tab", ControlifyBindings.INV_SELECT, Location.LEFT, "Select"),
        row("creative_one", ControlifyBindings.INV_SELECT, Location.LEFT, "Add one"),
        row("creative_stack", ControlifyBindings.INVENTORY, Location.LEFT, "Add stack"),
        row("creative_clear", CREATIVE_CLEAR, Location.LEFT, "Clear"),
        row("creative_delete_hotbar", ControlifyBindings.INV_QUICK_MOVE, Location.RIGHT, "Delete"),
        row("creative_search", CreativeSearchBinding.SEARCH, Location.LEFT, "Search"),
        row("browse_recipes", RIGHT_STICK, Location.RIGHT, "Browse"),
        row("choose_recipe", LEFT_STICK, Location.RIGHT, "Choose"),
        row("rotate_recipes", WINDOW_CYCLE, Location.RIGHT, "Rotate"),
        row("categories", CATEGORY, Location.RIGHT, "Category"),
        row("craftable", FILTER, Location.RIGHT, "Filter"),
        row("tab_stick", LEFT_STICK, Location.RIGHT, "Choose"),
        row("tab_right_stick", RIGHT_STICK, Location.RIGHT, "Choose"),
        row("tab_dpad", CATEGORY, Location.RIGHT, "Cycle")
        );
    }

    private WorkspaceActionGuide() { }
    public static void update(WorkspaceContainerScreen screen,ControllerEntity controller) { ensureBindings(); }
    private static Map<String,Boolean> facts(State state,ControllerEntity controller) {
        boolean side = state.mode() == Mode.SIDE, windowWheel = state.mode() == Mode.LT_RADIAL;
        boolean recipes = state.mode() == Mode.RECIPE || state.mode() == Mode.COOKING;
        boolean tabs = state.mode() == Mode.CREATIVE_TABS;
        boolean select = !conflictsWithClose(controller, ControlifyBindings.INV_SELECT);
        boolean inventory = !conflictsWithClose(controller, ControlifyBindings.INVENTORY);
        boolean quickMove = !conflictsWithClose(controller, ControlifyBindings.INV_QUICK_MOVE);
        boolean contextual = !conflictsWithClose(controller, ContextualRecipeBinding.OPEN_WHEEL);
        boolean opening = ControlifyInputBridge.awaitingOpeningTriggerRelease();
        InputBinding search = CreativeSearchBinding.SEARCH.onOrNull(controller);
        boolean searchReady = side && state.creativeSearchAvailable() && search != null && !search.isUnbound()
            && !conflictsWithClose(controller, CreativeSearchBinding.SEARCH)
            && !(opening && search.boundInput().getRelevantInputs().contains(GamepadInputs.LEFT_TRIGGER_AXIS));
        boolean contextualOwnsWindowTrigger = state.quickAction() != DesktopContextualMenu.Kind.NONE && contextual
            && sameBinding(controller, ContextualRecipeBinding.OPEN_WHEEL, WINDOW_WHEEL);
        Map<String, Boolean> facts = new HashMap<>();
        facts.put("creative_search", searchReady);
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
        facts.put("craft", state.mode() == Mode.RECIPE && select);
        facts.put("craft_stack", state.mode() == Mode.RECIPE && quickMove && !sameBinding(controller, ControlifyBindings.INV_SELECT, ControlifyBindings.INV_QUICK_MOVE));
        facts.put("prepare_cooking", state.mode() == Mode.COOKING && state.canConfirmContextual() && select);
        facts.put("select_tab", tabs && state.canConfirmContextual() && select);
        boolean carriedCatalog = state.creativeCatalogItem() && !state.carried().isEmpty();
        boolean distinctCatalogBindings = !sameBinding(controller, ControlifyBindings.INV_SELECT, ControlifyBindings.INVENTORY);
        facts.put("creative_one", side && state.creativeCatalogItem() && !carriedCatalog && select && distinctCatalogBindings);
        facts.put("creative_stack", side && state.creativeCatalogItem() && !carriedCatalog && inventory);
        facts.put("creative_clear", side && carriedCatalog && (select || inventory));
        facts.put("creative_delete_hotbar", side && state.creativeHotbarDelete() && quickMove);
        facts.put("browse_recipes", recipes);
        facts.put("choose_recipe", recipes);
        facts.put("rotate_recipes", recipes);
        facts.put("categories", recipes);
        facts.put("craftable", recipes);
        facts.put("tab_stick", tabs);
        facts.put("tab_right_stick", tabs);
        facts.put("tab_dpad", tabs);
        return facts;
    }
    private static List<Entry> entries(WorkspaceContainerScreen screen,ControllerEntity controller) {
        ensureBindings();
        State state=screen.controllerGuideState();
        if(state==null) return List.of();
        Map<String,Boolean> facts=facts(state,controller);
        List<Entry> result=new ArrayList<>();
        for(Definition definition:DEFINITIONS) if(facts.getOrDefault(definition.fact(),false)) {
            InputBinding binding=definition.binding().onOrNull(controller);
            if(binding!=null && !binding.isUnbound()) add(result,displayGlyph(binding.inputIcon(),controller),definition.text());
        }
        if(state.mode()==Mode.SIDE && !state.creativeCatalogItem()) {
            if(state.hoveredSlot()!=null) {
                addNative(result,controller,ControlifyBindings.INV_SELECT,state.carried().isEmpty()?"Take":"Place");
                if(!state.creativeHotbarDelete()) addNative(result,controller,ControlifyBindings.INV_QUICK_MOVE,"Move");
                addNative(result,controller,ControlifyBindings.INV_TAKE_HALF,"Split");
            } else if(!state.carried().isEmpty()) addNative(result,controller,ControlifyBindings.DROP_INVENTORY,"Drop");
        }
        return List.copyOf(result);
    }
    private static void addNative(List<Entry> entries,ControllerEntity controller,InputBindingSupplier supplier,String label) {
        InputBinding binding=supplier.onOrNull(controller);
        if(binding!=null && !binding.isUnbound() && !conflictsWithClose(controller,supplier))
            add(entries,displayGlyph(binding.inputIcon(),controller),label);
    }
    private static void add(List<Entry> entries,Component glyph,String label) {
        var font=Minecraft.getInstance().font;
        Component text=Component.literal(label);
        entries.add(new Entry(glyph,text,font.width(glyph)+4+font.width(text),Math.max(10,BindingFontHelper.getComponentHeight(font,glyph))));
    }
    private static ControllerEntity controller(WorkspaceContainerScreen screen) {
        if(Minecraft.getInstance().screen!=screen || !ControlifyApi.get().currentInputMode().isController()) return null;
        ControllerEntity controller=ControlifyApi.get().getCurrentController().orElse(null);
        return controller!=null && controller.genericConfig().config().showScreenGuides?controller:null;
    }
    public static int reservedBottomSpace(WorkspaceContainerScreen screen) {
        if(screen instanceof net.minecraft.client.gui.screens.Screen nativeScreen && WorkspaceSearchKeyboard.activeFor(nativeScreen)) {
            Reservation saved=reservations.get(screen);return saved==null?0:saved.band();
        }
        ControllerEntity controller=controller(screen);if(controller==null)return 0;
        var minecraft=Minecraft.getInstance();int width=minecraft.getWindow().getGuiScaledWidth(),height=minecraft.getWindow().getGuiScaledHeight();
        var entries=entries(screen,controller);
        var layout=layout(entries,width,height,controller);
        State state=screen.controllerGuideState();Reservation previous=reservations.get(screen);int band=layout.reservedHeight();
        if(previous!=null && previous.mode()==state.mode() && previous.width()==width && previous.height()==height)band=Math.max(band,previous.band());
        reservations.put(screen,new Reservation(state.mode(),width,height,band));return band;
    }
    private static CompactGuideLayout.Layout layout(List<Entry> entries,int width,int height,ControllerEntity controller) {
        var minecraft=Minecraft.getInstance();var window=minecraft.getWindow();
        int requested=-1;
        double scale=1;
        if(requested>0)scale=window.calculateScale(requested,minecraft.isEnforceUnicode())/(double)window.getGuiScale();
        return CompactGuideLayout.compute(entries.stream().map(entry->new CompactGuideLayout.Size(entry.width(),entry.height())).toList(),Math.max(1,width),Math.max(1,height),Math.max(.1,scale));
    }
    public static void render(WorkspaceContainerScreen screen,GuiGraphicsExtractor graphics,int mouseX,int mouseY,float partialTick) {
        ControllerEntity controller=controller(screen);if(controller==null)return;
        List<Entry> entries=entries(screen,controller);var layout=layout(entries,graphics.guiWidth(),graphics.guiHeight(),controller);
        graphics.nextStratum();var font=Minecraft.getInstance().font;
        for(var placement:layout.entries()) {
            Entry entry=entries.get(placement.index());
            graphics.fill((int)placement.x()-1,(int)placement.y()-1,(int)Math.ceil(placement.x()+placement.width())+1,(int)Math.ceil(placement.y()+placement.height())+1,0x70000000);
            graphics.pose().pushMatrix();
            try { graphics.pose().translate((float)placement.x(),(float)placement.y());graphics.pose().scale((float)layout.scale(),(float)layout.scale());
                graphics.text(font,Component.empty().append(entry.glyph()).append(" ").append(entry.label()),0,0,0xFFFFFFFF,true);
            } finally {graphics.pose().popMatrix();}
        }
    }
    private static boolean conflictsWithClose(ControllerEntity controller, InputBindingSupplier binding) {
        if (binding instanceof GlyphSupplier glyph) {
            List<ResourceLocation> physical = glyph.inputs.apply(controller);
            return physical.size() == 1 && Objects.equals(physical.get(0), closePhysicalInput(controller));
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
    private static Definition row(String fact, InputBindingSupplier binding, Location location, String text) {
        return new Definition(fact, binding, location, text);
    }
    private static ResourceLocation id(String path) { return new ResourceLocation("salts_inventory_controlify", path); }
    private static InputBindingSupplier fixed(String name, ResourceLocation... inputs) { return new GlyphSupplier(name, controller -> List.of(inputs)); }
    private static InputBindingSupplier composite(String name, InputBindingSupplier... bindings) {
        return new GlyphSupplier(name, controller -> Arrays.stream(bindings).map(binding -> binding.onOrNull(controller)).filter(Objects::nonNull)
            .filter(binding -> !binding.isUnbound()).flatMap(binding -> binding.boundInput().getRelevantInputs().stream()).distinct().toList());
    }

    private static ResourceLocation closePhysicalInput(ControllerEntity controller) {
        InputBinding close = ControlifyBindings.GUI_BACK.onOrNull(controller);
        if (close == null || close.isUnbound()) return null;
        List<ResourceLocation> inputs = close.boundInput().getRelevantInputs();
        return inputs.size() == 1 ? inputs.get(0) : null;
    }

    /** Unregistered metadata bindings cannot be remapped or persisted independently of real inputs. */
    private static final class GlyphSupplier implements InputBindingSupplier {
        private record Cached(List<ResourceLocation> inputs, InputBinding binding) { }
        private final ResourceLocation id;
        private final Function<ControllerEntity, List<ResourceLocation>> inputs;
        private final Map<ControllerEntity, Cached> cache = new WeakHashMap<>();
        private GlyphSupplier(String name, Function<ControllerEntity, List<ResourceLocation>> inputs) { this.id = id("guide_input/" + name); this.inputs = inputs; }
        @Override public InputBinding onOrNull(ControllerEntity controller) {
            ResourceLocation blocked = closePhysicalInput(controller);
            List<ResourceLocation> actual = inputs.apply(controller).stream().filter(input -> !Objects.equals(input, blocked)).toList();
            if (actual.isEmpty()) return null;
            Cached cached = cache.get(controller);
            if (cached == null || !cached.inputs().equals(actual)) {
                Input metadata = new GlyphInput(plainStickInputs(actual));
                InputBinding binding = new InputBindingImpl(controller, id, Component.empty(), Component.empty(),
                    Component.empty(), () -> metadata, Set.of(), null) {
                    @Override public Component inputIcon() { return displayGlyph(super.inputIcon(), controller); }
                };
                cached = new Cached(actual, binding); cache.put(controller, cached);
            }
            return cached.binding();
        }
        @Override public ResourceLocation bindId() { return id; }
        public Component inputIcon() {
            return ControlifyApi.get().getCurrentController().map(controller -> onOrNull(controller)).map(InputBinding::inputIcon).orElseGet(Component::empty);
        }
    }

    /** Native bitmap fonts contain only input symbols: their literal '+' separator has no glyph.
     * Keep each input in its mapped font and put ordinary spacing in the default font. */
    private static Component displayGlyph(Component original, ControllerEntity controller) {
        List<Component> parts = new ArrayList<>();
        original.visit((style, text) -> {
            if (text.isEmpty()) return Optional.empty();
            if (WHOLE_DPAD.equals(style.getInsertion())) parts.add(Component.literal(text).withStyle(style));
            else for (String part : text.split("\\+", -1))
                if (!part.isEmpty() && part.chars().anyMatch(code -> code != 32)) parts.add(Component.literal(part).withStyle(style));
            return Optional.empty();
        }, Style.EMPTY);
        List<Component> dpad = controller == null ? List.of() : List.of(
            nativeInputGlyph(controller, GamepadInputs.DPAD_UP_BUTTON), nativeInputGlyph(controller, GamepadInputs.DPAD_DOWN_BUTTON),
            nativeInputGlyph(controller, GamepadInputs.DPAD_LEFT_BUTTON), nativeInputGlyph(controller, GamepadInputs.DPAD_RIGHT_BUTTON));
        boolean whole = !dpad.isEmpty() && dpad.stream().allMatch(direction -> parts.stream().anyMatch(part -> sameGlyph(part, direction)));
        var result = Component.empty();
        boolean addedDpad = false;
        for (Component part : parts) {
            boolean dpadPart = whole && dpad.stream().anyMatch(direction -> sameGlyph(part, direction));
            if (dpadPart && addedDpad) continue;
            if (!result.getSiblings().isEmpty()) result.append(" ");
            if (dpadPart) {
                result.append(wholeDpadGlyph(GamepadInputs.DPAD_UP_BUTTON).withStyle(style -> style.withInsertion(WHOLE_DPAD)));
                addedDpad = true;
            } else result.append(part);
        }
        return result;
    }
    /** The default family supplies a complete cross; PlayStation direction glyphs are separate buttons. */
    private static net.minecraft.network.chat.MutableComponent wholeDpadGlyph(ResourceLocation input) {
        return singleInputGlyph(new ResourceLocation("controlify", "default"), input).copy();
    }
    private static Component nativeInputGlyph(ControllerEntity controller, ResourceLocation input) {
        return singleInputGlyph(controller.info().type().namespace(), input);
    }
    private static Component singleInputGlyph(ResourceLocation namespace, ResourceLocation input) {
        Component mapped = Controlify.instance().inputFontMapper().getComponentFromInputs(namespace, List.of(input));
        // The pinned mapper wraps each styled input in an empty component. Compare and
        // mark the actual glyph leaf with its inherited font, not that unstyled wrapper.
        Optional<Component> leaf = mapped.visit((style, text) -> text.isEmpty() ? Optional.empty()
            : Optional.of(Component.literal(text).withStyle(style)), Style.EMPTY);
        return leaf.orElseGet(Component::empty);
    }
    private static boolean sameGlyph(Component first, Component second) {
        return first.getString().equals(second.getString()) && Objects.equals(first.getStyle().getFont(), second.getStyle().getFont());
    }

    /** Native undirected joystick glyphs; metadata never turns these display IDs into button actions. */
    private static List<ResourceLocation> plainStickInputs(List<ResourceLocation> physical) {
        List<ResourceLocation> display = new ArrayList<>();
        for (ResourceLocation input : physical) {
            ResourceLocation glyph = input;
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
    private record GlyphInput(List<ResourceLocation> inputs) implements Input {
        private GlyphInput { inputs = List.copyOf(inputs); }
        @Override public float state(ControllerStateView state) { return 0; }
        @Override public List<ResourceLocation> getRelevantInputs() { return inputs; }
        // This metadata input is neither registered, sampled, saved nor serialized.
        @Override public InputType<?> type() { return GamepadInputs.getBind(inputs.get(0)).type(); }
    }
}
