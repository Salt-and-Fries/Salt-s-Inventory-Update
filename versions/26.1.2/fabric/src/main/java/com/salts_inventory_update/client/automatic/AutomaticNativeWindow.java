package com.salts_inventory_update.client.automatic;

import java.util.ArrayList;
import java.util.List;
import com.salts_inventory_update.api.client.desktop.*;
import net.minecraft.client.input.*;
import com.salts_inventory_update.client.InventoryDesktopScreen;
import com.salts_inventory_update.debug.DesktopDebug;
import com.salts_inventory_update.mixin.client.AutomaticContainerScreenAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;

/** Internal fallback definition; never registered in, or consulted before, the public API registry. */
public class AutomaticNativeWindow implements DesktopWindowDefinition<AbstractContainerMenu, AutomaticNativeWindow.State>, DesktopWindowPresentation {
    public static final class State {
        final AbstractContainerScreen<?> screen;
        AutomaticContainerLayout layout;
        AutomaticContainerLayout baseLayout, pendingLayout;
        public net.minecraft.client.gui.Font tooltipFont;
        public List<net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent> tooltip;
        public net.minecraft.resources.Identifier tooltipTexture;
        public GuiEventListener recipeButton;
        public boolean recipeManaged;
        int screenWidth, screenHeight;
        boolean nativePointer;
        boolean bodyClip;
        boolean capturing;
        final NativeFrameBuffer buffer = new NativeFrameBuffer();
        State(AbstractContainerScreen<?> screen, AutomaticContainerLayout layout, int width, int height) {
            this.screen = screen; this.layout = layout; this.baseLayout = layout; screenWidth = width; screenHeight = height;
            refreshRecipeButton();
        }
        void refreshRecipeButton() {
            recipeButton = null;
            if (screen instanceof net.minecraft.client.gui.screens.recipebook.RecipeUpdateListener) {
                for (var child : screen.children()) if (child instanceof net.minecraft.client.gui.components.ImageButton
                    && child instanceof com.salts_inventory_update.mixin.client.AutomaticRecipeButtonAccessor access
                    && access.salts_inventory_update$nativeButtonSprites().enabled().getPath().contains("recipe_book")) recipeButton = child;
            }
        }
        void observe(int left, int top, int right, int bottom) {
            var b = pendingLayout == null ? layout : pendingLayout;
            int x = Math.min(left, b.x()), y = Math.min(top, b.y());
            pendingLayout = new AutomaticContainerLayout(x, y, Math.max(right, b.x() + b.width()) - x,
                Math.max(bottom, b.y() + b.height()) - y);
        }
        void beginObservation() { pendingLayout = baseLayout; }
        void endObservation() { if (layout.equals(pendingLayout)) pendingLayout = null; }
    }
    private final State initial;
    protected AutomaticNativeWindow(State initial) { this.initial = initial; }

    public static List<String> advertisedMenuIds(List<String> apiIds) {
        var result = new java.util.LinkedHashSet<>(apiIds);
        for (MenuType<?> type : BuiltInRegistries.MENU) {
            var id = BuiltInRegistries.MENU.getKey(type);
            if (result.size() < com.salts_inventory_update.protocol.DesktopProtocol.MAX_SUPPORTED_MENU_IDS
                && id != null && !id.getNamespace().equals("minecraft") && InventoryDesktopScreen.nativeContainerConstructor(type) != null)
                result.add(id.toString());
        }
        return List.copyOf(result);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static AutomaticNativeWindow create(Minecraft minecraft, AbstractContainerMenu menu, Component title) {
        return create(minecraft, menu, title, null);
    }
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static AutomaticNativeWindow create(Minecraft minecraft, AbstractContainerMenu menu, Component title, DesktopNativeTemplate template) {
        MenuScreens.ScreenConstructor factory = InventoryDesktopScreen.nativeContainerConstructor(menu.getType());
        if (factory == null || minecraft.player == null) return null;
        try {
            Object created = factory.create(menu, minecraft.player.getInventory(), title);
            if (!(created instanceof AbstractContainerScreen<?> screen)) return null;
            if (template == null) template = NativeProfileFiles.find(menu.getType(), screen.getClass().getName());
            int width = minecraft.getWindow().getGuiScaledWidth(), height = minecraft.getWindow().getGuiScaledHeight();
            screen.init(width, height);
            AutomaticContainerLayout layout = layout(minecraft, screen, template == null);
            if (layout == null && template != null) {
                var bounds = DesktopNativeView.union(menu.slots.stream().filter(s -> s.container != minecraft.player.getInventory() && s.isActive())
                    .map(s -> new DesktopWindowBounds(s.x-1,s.y-1,18,18)).toList()).orElse(null);
                if (bounds != null) layout = new AutomaticContainerLayout(bounds.x(),bounds.y(),bounds.width(),bounds.height());
            }
            if (layout == null) { screen.removed(); return null; }
            com.salts_inventory_update.internal.desktop.AutomaticContainerSupport.inferStorage(menu, minecraft.player.getInventory());
            State state = new State(screen, layout, width, height);
            return template == null ? new AutomaticNativeWindow(state) : new NativeTemplateWindow(state, template);
        } catch (RuntimeException failure) {
            DesktopDebug.warn("automatic native screen declined menu={} reason={}", menu.getType(), failure.toString());
            return null;
        }
    }

    private static AutomaticContainerLayout layout(Minecraft mc, AbstractContainerScreen<?> screen, boolean bars) {
        var access = (AutomaticContainerScreenAccessor) screen;
        var cells = new ArrayList<AutomaticContainerLayout.Cell>();
        for (Slot slot : screen.getMenu().slots)
            if (slot.isActive()) cells.add(new AutomaticContainerLayout.Cell(slot.x, slot.y, slot.container == mc.player.getInventory()));
        var controls = new ArrayList<AutomaticContainerLayout.Control>();
        for (GuiEventListener child : screen.children()) if (child instanceof AbstractWidget w)
            controls.add(new AutomaticContainerLayout.Control(w.getX() - access.salts_inventory_update$nativeLeft(),
                w.getY() - access.salts_inventory_update$nativeTop(), w.getWidth(), w.getHeight()));
        var inferred = AutomaticContainerLayout.infer(access.salts_inventory_update$nativeWidth(), access.salts_inventory_update$nativeHeight(), cells, controls);
        if (inferred == null) return null;
        int machineBottom = cells.stream().filter(cell -> !cell.player()).mapToInt(cell -> cell.y() + 19).max().orElse(inferred.y());
        int labelY = access.salts_inventory_update$nativeInventoryLabelY();
        int bottom = inferred.y() + inferred.height();
        if (!bars && labelY >= machineBottom && labelY < bottom) bottom = labelY;
        var body = new AutomaticContainerLayout(inferred.x(), inferred.y(), inferred.width(), bottom - inferred.y());
        // Keep the original side artwork only for the unconfigured fallback. API
        // templates retain the original inferred crop and author-defined regions.
        return bars ? body.automaticBody(access.salts_inventory_update$nativeWidth(), cells, controls) : body;
    }

    protected boolean usesAutomaticBars() { return true; }
    private DesktopInsets hostInsets() { return usesAutomaticBars() ? AutomaticWindowBars.INSETS : DesktopInsets.SALT; }
    @Override public DesktopWindowSurface surface(DesktopWindowContext<AbstractContainerMenu, State> c) {
        return usesAutomaticBars() ? AutomaticWindowBars.SURFACE : DesktopWindowSurface.SALT;
    }
    @Override public DesktopWindowDecoration<AbstractContainerMenu, State> decoration(DesktopWindowContext<AbstractContainerMenu, State> c) {
        return usesAutomaticBars() ? AutomaticWindowBars::render : null;
    }
    @Override public State createState(DesktopWindowSetupContext<AbstractContainerMenu> context) {
        com.salts_inventory_update.internal.desktop.AutomaticContainerSources.bindClient(context.minecraft().player, context.model(), context.sourceKey());
        return initial;
    }
    @Override public DesktopWindowSize defaultSize(DesktopWindowSetupContext<AbstractContainerMenu> context) {
        return hostInsets().surfaceSize(initial.layout.width(), initial.layout.height());
    }
    @Override public DesktopWindowSize minSize(DesktopWindowContext<AbstractContainerMenu, State> c) {
        return hostInsets().surfaceSize(c.state().layout.width(), c.state().layout.height());
    }
    @Override public DesktopWindowSize maxSize(DesktopWindowContext<AbstractContainerMenu, State> c) { return minSize(c); }
    @Override public DesktopResizePolicy resizePolicy(DesktopWindowContext<AbstractContainerMenu, State> c) { return DesktopResizePolicy.FIXED; }
    @Override public String layoutRevision(DesktopWindowContext<AbstractContainerMenu, State> c) { return "automatic-native-bars-2"; }
    @Override public void tick(DesktopWindowContext<AbstractContainerMenu, State> c) {
        State s = c.state();
        int width = c.minecraft().getWindow().getGuiScaledWidth(), height = c.minecraft().getWindow().getGuiScaledHeight();
        if (width != s.screenWidth || height != s.screenHeight) {
            s.screen.resize(width, height); s.screenWidth = width; s.screenHeight = height;
            s.refreshRecipeButton();
            AutomaticContainerLayout next = layout(c.minecraft(), s.screen, usesAutomaticBars());
            if (next != null) { s.layout = next; s.baseLayout = next; s.pendingLayout = null; }
        }
        AutomaticNativeScope.call(frame(c, null, null), () -> { s.screen.tick(); return null; });
    }
    @Override public void closed(DesktopWindowContext<AbstractContainerMenu, State> c) { c.state().buffer.close(); c.state().screen.removed(); }
    @Override public void prepareLayout(DesktopWindowContext<AbstractContainerMenu, State> c) {
        if (c.state().pendingLayout != null) { c.state().layout = c.state().pendingLayout; c.state().pendingLayout = null; }
    }
    @Override public DesktopWindowSize desiredWindowSize(DesktopWindowContext<?, ?> c) {
        var s = (State)c.state(); var l = s.pendingLayout == null ? s.layout : s.pendingLayout;
        return hostInsets().surfaceSize(l.width(), l.height());
    }
    @Override public void cancelPointer(DesktopWindowContext<?, ?> c) {
        State s = (State)c.state(); s.nativePointer = false; s.screen.setDragging(false);
    }
    @Override public void keyboardFocusChanged(DesktopWindowContext<AbstractContainerMenu, State> c, boolean focused) {
        if (!focused) { c.state().screen.setFocused(null); c.state().screen.setDragging(false); c.state().nativePointer = false; }
    }

    private AutomaticNativeScope.Frame frame(DesktopWindowContext<AbstractContainerMenu, State> c,
                                             DesktopRenderContext<?, ?> render, DesktopInputContext<?, ?> input) {
        var a = (AutomaticContainerScreenAccessor) c.state().screen;
        var layout = c.state().layout;
        return new AutomaticNativeScope.Frame(c.state().screen, c.state(), render, input, layout,
            c.contentX() - layout.x() - a.salts_inventory_update$nativeLeft(),
            c.contentY() - layout.y() - a.salts_inventory_update$nativeTop());
    }
    @Override public void render(DesktopRenderContext<AbstractContainerMenu, State> c) {
        c.state().tooltip = null; c.state().tooltipFont = null;
        c.state().beginObservation();
        var f = frame(c, c, null); var g = c.nativeGraphics();
        c.withClip(c.contentX(), c.contentY(), Math.min(c.contentWidth(), c.state().layout.width()), Math.min(c.contentHeight(), c.state().layout.height()), () -> {
            g.pose().pushMatrix();
            try {
                g.pose().translate(f.offsetX(), f.offsetY());
                AutomaticNativeScope.call(f, () -> {
                    c.state().screen.extractBackground(g, c.mouseX() - f.offsetX(), c.mouseY() - f.offsetY(), c.minecraft().getDeltaTracker().getGameTimeDeltaPartialTick(false));
                    c.state().screen.extractRenderState(g, c.mouseX() - f.offsetX(), c.mouseY() - f.offsetY(), c.minecraft().getDeltaTracker().getGameTimeDeltaPartialTick(false));
                    return null;
                });

            } finally {
                c.state().endObservation();
                AutomaticNativeScope.finishTextureClip(f, g); g.pose().popMatrix();
            }
        });
    }
    @Override public boolean appendTooltip(DesktopRenderContext<AbstractContainerMenu, State> c, int x, int y) {
        State s = c.state(); if (s.tooltip == null || s.tooltip.isEmpty()) return false;
        c.nativeGraphics().tooltip(s.tooltipFont,s.tooltip,x,y,net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner.INSTANCE,s.tooltipTexture);
        return true;
    }
    @Override public net.minecraft.client.gui.screens.recipebook.RecipeBookComponent createRecipeBook(DesktopWindowContext<AbstractContainerMenu, State> c) {
        if (c.state().screen instanceof com.salts_inventory_update.mixin.client.AutomaticRecipeScreenAccessor provider) {
            c.state().recipeManaged = true; return provider.salts_inventory_update$recipeBook();
        }
        return null;
    }
    @Override public DesktopSlotHit slotAt(DesktopSlotContext<AbstractContainerMenu, State> c, double x, double y) {
        var l = c.state().layout;
        for (Slot slot : c.containerSlots()) {
            int sx = c.contentX() + slot.x - l.x(), sy = c.contentY() + slot.y - l.y();
            if (slot.isActive() && x >= sx - 1 && x < sx + 17 && y >= sy - 1 && y < sy + 17)
                return DesktopSlotHit.of(c.menuSlotId(slot), sx, sy);
        }
        return null;
    }
    private boolean insideNativeBody(DesktopWindowContext<AbstractContainerMenu, State> c, double x, double y) {
        return x >= c.contentX() && y >= c.contentY()
            && x < c.contentX() + Math.min(c.contentWidth(), c.state().layout.width())
            && y < c.contentY() + Math.min(c.contentHeight(), c.state().layout.height());
    }
    @Override public boolean mouseClicked(DesktopInputContext<AbstractContainerMenu, State> c, MouseButtonEvent e, boolean twice) {
        // Blank caps/gutters must not activate cropped-out native widgets or player slots.
        // Explicit API templates continue to own their complete input mapping.
        if (usesAutomaticBars() && !insideNativeBody(c, e.x(), e.y())) return true;
        var l = c.state().layout;
        for (Slot slot : c.containerSlots()) {
            int x = c.contentX() + slot.x - l.x(), y = c.contentY() + slot.y - l.y();
            if (slot.isActive() && e.x() >= x - 1 && e.x() < x + 17 && e.y() >= y - 1 && e.y() < y + 17) return false;
        }
        var f = frame(c, null, c);
        boolean consumed = AutomaticNativeScope.call(f, () -> c.state().screen.mouseClicked(new MouseButtonEvent(e.x()-f.offsetX(),e.y()-f.offsetY(),e.buttonInfo()), twice));
        c.state().nativePointer = consumed; return consumed;
    }
    @Override public boolean mouseReleased(DesktopInputContext<AbstractContainerMenu, State> c, MouseButtonEvent e) {
        if (!c.state().nativePointer) return false;
        c.state().nativePointer = false;
        var f = frame(c, null, c);
        return AutomaticNativeScope.call(f, () -> c.state().screen.mouseReleased(new MouseButtonEvent(e.x()-f.offsetX(),e.y()-f.offsetY(),e.buttonInfo())));
    }
    @Override public boolean mouseDragged(DesktopInputContext<AbstractContainerMenu, State> c, MouseButtonEvent e, double dx, double dy) {
        if (!c.state().nativePointer) return false;
        var f = frame(c, null, c);
        return AutomaticNativeScope.call(f, () -> c.state().screen.mouseDragged(new MouseButtonEvent(e.x()-f.offsetX(),e.y()-f.offsetY(),e.buttonInfo()), dx, dy));
    }
    @Override public boolean mouseScrolled(DesktopInputContext<AbstractContainerMenu, State> c, double x, double y, double sx, double sy) {
        if (usesAutomaticBars() && !insideNativeBody(c, x, y)) return true;
        var f = frame(c, null, c);
        return AutomaticNativeScope.call(f, () -> c.state().screen.mouseScrolled(x - f.offsetX(), y - f.offsetY(), sx, sy));
    }
    @Override public boolean keyPressed(DesktopInputContext<AbstractContainerMenu, State> c, KeyEvent e) {
        var focused = c.state().screen.getFocused();
        return focused != null && AutomaticNativeScope.call(frame(c, null, c),
            () -> focused.keyPressed(e));
    }
    @Override public boolean charTyped(DesktopInputContext<AbstractContainerMenu, State> c, CharacterEvent e) {
        var focused = c.state().screen.getFocused();
        return focused != null && AutomaticNativeScope.call(frame(c, null, c),
            () -> focused.charTyped(e));
    }
    @Override public boolean wantsTextInput(DesktopWindowContext<AbstractContainerMenu, State> c) {
        return c.state().screen.getFocused() instanceof net.minecraft.client.gui.components.EditBox box && box.isFocused();
    }
}
