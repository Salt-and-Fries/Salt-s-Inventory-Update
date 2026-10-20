package com.salts_inventory_update.controlify;

import com.salts_inventory_update.api.client.desktop.*;
import com.salts_inventory_update.controlify.config.ControlifyConfig;
import com.salts_inventory_update.controlify.input.*;
import com.salts_inventory_update.controlify.model.*;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Owns only presentation and inputs; Core continues to own every window and container session. */
public final class SaltsControlifyClient implements DesktopWorkspaceHost, RadialWorkspaceActions {
    private static final Logger LOGGER = LoggerFactory.getLogger("salts_inventory_controlify");
    private static SaltsControlifyClient instance;
    private final ControlifyConfig config;
    private final RecipeFilterSettings recipeFilters;
    private final WorkspaceCraftFeedback craftFeedback = new WorkspaceCraftFeedback();
    private DesktopWorkspace desktop;
    private RadialWorkspace<UUID> model;
    private RadialInventoryScreen screen;
    private final WorkspaceAnimator<UUID> animator = new WorkspaceAnimator<>();
    private final WorkspacePresentationLayout layout = new WorkspacePresentationLayout();
    private final Map<UUID, DesktopWorkspaceWindow> windows = new LinkedHashMap<>();
    private long tuckedAt;
    private WorkspaceLayout<UUID> lastTuckedPreviewFrame;
    private int lastTuckedPreviewWidth, lastTuckedPreviewHeight;
    private boolean renderingTuckedPreview;
    private boolean synchronizing;
    private boolean controllerPresentationActive;

    private SaltsControlifyClient(ControlifyConfig config) {
        this.config = config;
        Path path = Minecraft.getInstance().gameDirectory.toPath().resolve("config/salts_inventory_controlify_recipes.properties");
        RecipeFilterSettings loaded;
        try { loaded = RecipeFilterSettings.load(path); }
        catch (IOException failure) {
            LOGGER.warn("Could not read recipe-wheel preferences; using All", failure);
            loaded = new RecipeFilterSettings(path);
        }
        recipeFilters = loaded;
    }

    RecipeFilter recipeFilter() { return recipeFilters.filter(); }
    RecipeFilter cycleRecipeFilter() {
        try { recipeFilters.set(recipeFilters.filter().next()); }
        catch (IOException failure) { LOGGER.warn("Could not save recipe-wheel filter", failure); }
        return recipeFilters.filter();
    }

    public static void initialize() {
        if (instance != null) return;
        try {
            Path path = Minecraft.getInstance().gameDirectory.toPath().resolve("config/salts_inventory_controlify.properties");
            instance = new SaltsControlifyClient(ControlifyConfig.load(path));
        } catch (IOException failure) {
            throw new IllegalStateException("Could not load controller inventory configuration", failure);
        }
        ControlifyInputBridge.initialize(instance, RadialInventoryScreen.class);
        WorkspaceUiSounds.initialize();
        DesktopClientApi.setWorkspaceHost(instance);
        DesktopClientApi.onClientTick(instance::tick);
        DesktopClientApi.onClientTick(WorkspaceUiSounds::tick);
    }

    @Override public boolean enabled() {
        return WorkspaceSearchKeyboard.activeFor(screen) || ControlifyInputBridge.usingController();
    }

    @Override public void opened(DesktopWorkspace workspace) {
        if (synchronizing) return;
        synchronizing = true;
        try {
            boolean changed = desktop == null || !desktop.isValid();
            desktop = workspace;
            UUID requested = desktop.windows().stream().filter(w -> w.handle().state().inventoryFocused()
                    || w.handle().state().keyboardFocused()).map(w -> w.handle().instanceId()).reduce((a, b) -> b).orElse(null);
            desktop.forceInventoryHud(true);
            desktop.setContextualRecipePresentation(true);
            desktop.setCharacterEffectsPresentation(true);
            controllerPresentationActive = true;
            desktop.initialize(Minecraft.getInstance().getWindow().getGuiScaledWidth(), Minecraft.getInstance().getWindow().getGuiScaledHeight());
            Set<UUID> before = new LinkedHashSet<>(windows.keySet());
            var inventory = desktop.ensureWindow(DesktopWorkspaceWindow.Kind.PLAYER_INVENTORY);
            var character = desktop.ensureWindow(DesktopWorkspaceWindow.Kind.CHARACTER);
            desktop.protectWindow(inventory.instanceId());
            desktop.protectWindow(character.instanceId());
            if (changed || model == null || !model.windows().contains(inventory.instanceId())) {
                craftFeedback.reset(); WorkspaceUiSounds.reset();
                model = new RadialWorkspace<>(inventory.instanceId(), character.instanceId());
                animator.reset(); windows.clear(); before.clear(); screen = null;
            }
            syncWindows();
            UUID newest = requested != null && windows.containsKey(requested) ? requested : inventory.instanceId();
            for (var entry : windows.entrySet()) {
                if (!before.contains(entry.getKey()) && entry.getValue().kind() == DesktopWorkspaceWindow.Kind.OTHER) newest = entry.getKey();
            }
            if (!tuckedAway()) {
                model.open(newest);
                ControlifyInputBridge.workspaceOpened();
                if (Minecraft.getInstance().screen != screen) WorkspaceUiSounds.play(WorkspaceUiSounds.Action.OPEN);
                show();
            }
            updateTransferPair();
        } finally { synchronizing = false; }
    }

    private void syncWindows() {
        List<DesktopWorkspaceWindow> snapshot = desktop.windows();
        windows.clear();
        for (var window : snapshot) {
            if (!window.handle().state().visible()) desktop.restoreWindow(window.handle().instanceId());
            windows.put(window.handle().instanceId(), window);
            model.register(window.handle().instanceId(), window.closable());
        }
        model.retain(windows.keySet());
    }

    private void tick(Minecraft minecraft) {
        if (desktop == null) {
            if (enabled()) DesktopClientApi.currentWorkspace().filter(DesktopWorkspace::isNativeScreenActive).ifPresent(this::opened);
            return;
        }
        if (!desktop.isValid() || minecraft.player == null) {
            craftFeedback.reset(); WorkspaceUiSounds.reset();
            desktop.setContextualRecipePresentation(false);
            desktop.setCharacterEffectsPresentation(false);
            controllerPresentationActive = false;
            if (minecraft.screen == screen || WorkspaceSearchKeyboard.activeFor(screen)) minecraft.setScreen(null);
            WorkspaceSearchKeyboard.resetFor(screen);
            desktop = null; model = null; windows.clear(); screen = null; animator.reset();
            ControlifyInputBridge.reset();
            return;
        }
        if (!enabled()) {
            WorkspaceSearchKeyboard.resetFor(screen);
            craftFeedback.reset(); WorkspaceUiSounds.reset();
            desktop.forceInventoryHud(false);
            desktop.setContextualRecipePresentation(false);
            desktop.setCharacterEffectsPresentation(false);
            controllerPresentationActive = false;
            if (model != null && model.active()) {
                boolean wasVisible = !tuckedAway();
                model.deactivate();
                if (minecraft.screen == screen) minecraft.setScreen(null);
                if (wasVisible) windows.values().stream().filter(w -> w.handle().instanceId().equals(model.focused())).findFirst().ifPresent(w -> w.handle().focus());
            }
            return;
        }
        if (!controllerPresentationActive) {
            desktop.setContextualRecipePresentation(true);
            desktop.setCharacterEffectsPresentation(true);
            controllerPresentationActive = true;
        }
        if (!model.active() && desktop.isNativeScreenActive()) opened(desktop);
        syncWindows();
        craftFeedback.tick(desktop);
        if (model.active() && minecraft.screen == null && !tuckedAway()) show();
        if (tuckedAway()) desktop.tick();
        updateTransferPair();
    }

    private void show() {
        Minecraft minecraft = Minecraft.getInstance();
        if (screen == null) screen = new RadialInventoryScreen(this, minecraft.player.inventoryMenu, minecraft.player.getInventory());
        if (minecraft.screen != screen) minecraft.setScreen(screen);
    }

    private void updateTransferPair() {
        if (model != null) desktop.transferPair(model.focused(), model.selected());
    }

    WorkspaceLayout<UUID> animatedLayout(int width, int height) {
        Map<UUID, WindowSize> sizes = new LinkedHashMap<>();
        windows.forEach((id, window) -> {
            if (!window.handle().isOpen()) return;
            var bounds = desktop.presentationBounds(id);
            sizes.put(id, new WindowSize(Math.max(1, bounds.width()), Math.max(1, bounds.height())));
        });
        var frame = animator.animate(layout.compute(model, sizes, Math.max(1, width), Math.max(1, height), 1),
                System.nanoTime(), config.animationsEnabled(), 220);
        frame = layout.constrainRadial(model, sizes, frame, Math.max(1, width), Math.max(1, height));
        if (renderingTuckedPreview) {
            lastTuckedPreviewFrame = frame;
            lastTuckedPreviewWidth = width; lastTuckedPreviewHeight = height;
        }
        return frame;
    }

    public static void renderGameplayTransition(GuiGraphicsExtractor graphics, float partialTick) {
        if (instance == null || !instance.enabled() || !instance.tuckedAway()
                || Minecraft.getInstance().screen != null) return;
        if (instance.config.animationsEnabled() && System.nanoTime() - instance.tuckedAt <= 260_000_000L
                && instance.screen != null) {
            instance.renderingTuckedPreview = true;
            try { instance.screen.renderWorkspace(graphics, Integer.MIN_VALUE, Integer.MIN_VALUE, partialTick); }
            finally { instance.renderingTuckedPreview = false; }
        }
        TuckedWindowTabs.render(graphics, instance.windows, instance.desktop);
    }

    /** Retained windows can be restored by the native inventory action during tucked gameplay. */
    public static boolean tuckedWindowsAvailable() {
        return instance != null && instance.enabled() && instance.tuckedAway()
                && Minecraft.getInstance().screen == null;
    }

    /** Clearance in current GUI coordinates, independent of Controlify's optional guide scale. */
    public static int tuckedGuideInset(int width, int height) {
        if (instance == null || !instance.enabled() || !instance.tuckedAway()
                || Minecraft.getInstance().screen != null || width <= 0 || height <= 0) return 0;
        double tabsBottom = TuckedWindowTabs.reservedBottom(width, instance.windows);
        WorkspaceLayout<UUID> departing = null;
        Map<UUID, WindowSize> sizes = new LinkedHashMap<>();
        // Sample the same retargetable animation as the departing panes. A fixed tab-height
        // offset would still cover the native guide while large panes cross the top band.
        if (instance.config.animationsEnabled() && System.nanoTime() - instance.tuckedAt <= 260_000_000L
                && instance.screen != null && instance.desktop != null && instance.desktop.isValid()) {
            instance.windows.forEach((id, window) -> {
                if (!window.handle().isOpen()) return;
                var bounds = instance.desktop.presentationBounds(id);
                sizes.put(id, new WindowSize(Math.max(1, bounds.width()), Math.max(1, bounds.height())));
            });
            departing = instance.animatedLayout(width, height);
        }
        int inset = TuckedGuideReservation.inset(width, height, tabsBottom, departing, sizes);
        // The guide can extract before or after our overlay within a HUD frame. Reserve the
        // last drawn departing geometry too, so a later animation sample cannot undercut it.
        if (departing != null && instance.lastTuckedPreviewWidth == width && instance.lastTuckedPreviewHeight == height)
            inset = Math.max(inset, TuckedGuideReservation.inset(width, height, tabsBottom, instance.lastTuckedPreviewFrame, sizes));
        return inset;
    }

    DesktopWorkspace desktop() { return desktop; }
    Map<UUID, DesktopWorkspaceWindow> windows() { return Collections.unmodifiableMap(windows); }
    RadialWorkspace<UUID> model() { return model; }
    ControlifyConfig config() { return config; }
    public static ControlifyConfig preferences() { return instance.config; }
    @Override public boolean toggleWindowWheel() { return config.toggleWindowWheel(); }
    @Override public boolean toggleRecipeWheel() { return config.toggleRecipeWheel(); }
    WorkspaceCraftFeedback craftFeedback() { return craftFeedback; }

    @Override public boolean workspaceActive() { return model != null && model.active(); }
    @Override public boolean radialActive() { return workspaceActive() && model.mode() == RadialWorkspace.Mode.RADIAL; }
    @Override public boolean tuckedAway() { return workspaceActive() && model.mode() == RadialWorkspace.Mode.TUCKED_AWAY; }
    @Override public void enterRadial() {
        if (model.beginRadial()) { desktop.cancelInput(); WorkspaceUiSounds.play(WorkspaceUiSounds.Action.OPEN); }
    }
    @Override public void radialDirection(float x, float y) {
        var previous = model.highlighted();
        boolean tuck = model.tuckHighlighted();
        model.pointStick(x, y, 0.18);
        if (!previous.equals(model.highlighted()) || tuck != model.tuckHighlighted())
            WorkspaceUiSounds.play(WorkspaceUiSounds.Action.FOCUS);
    }
    @Override public void releaseRadial() {
        UUID previous = model.selected();
        model.releaseRadial(); updateTransferPair();
        if (tuckedAway()) {
            craftFeedback.reset();
            WorkspaceUiSounds.play(WorkspaceUiSounds.Action.TUCK);
            tuckedAt = System.nanoTime();
            lastTuckedPreviewFrame = null;
            desktop.cancelInput();
            Minecraft.getInstance().setScreen(null);
        } else if (!Objects.equals(previous, model.selected())) WorkspaceUiSounds.play(WorkspaceUiSounds.Action.CYCLE);
    }
    @Override public void cycleSelected(int steps) {
        if (model.cycleSelected(steps)) { desktop.cancelInput(); updateTransferPair(); WorkspaceUiSounds.play(WorkspaceUiSounds.Action.CYCLE); }
    }
    @Override public void swapFocused() {
        if (model.swapHighlighted()) { desktop.cancelInput(); updateTransferPair(); WorkspaceUiSounds.play(WorkspaceUiSounds.Action.CONFIRM); }
    }
    @Override public boolean sortFocused(boolean force) {
        boolean sorted = radialActive() && desktop != null && desktop.sortWindow(model.focused(), force);
        if (sorted) WorkspaceUiSounds.play(WorkspaceUiSounds.Action.SORT);
        return sorted;
    }
    @Override public void closeHighlighted() {
        model.highlighted().filter(model::closable).ifPresent(id -> {
            var window = windows.get(id);
            if (window != null && window.handle().available(DesktopWindowAction.USER_CLOSE)
                    && window.handle().close() && !window.handle().isOpen()) {
                model.remove(id); WorkspaceUiSounds.play(WorkspaceUiSounds.Action.CLOSE);
            }
        });
        syncWindows(); updateTransferPair();
    }
    @Override public void closeWorkspace() {
        boolean wasActive = workspaceActive();
        craftFeedback.reset(); WorkspaceUiSounds.reset();
        desktop.closeClosableWindows(); model.closeAll(); syncWindows();
        desktop.cancelInput();
        desktop.transferPair(null, null);
        if (screen != null) screen.resetControllerState();
        animator.reset();
        Minecraft.getInstance().setScreen(null);
        if (wasActive) WorkspaceUiSounds.play(WorkspaceUiSounds.Action.CLOSE);
    }
    @Override public void restoreWorkspace() {
        if (model != null && model.restore()) {
            desktop.cancelInput(); ControlifyInputBridge.workspaceOpened(); show();
            WorkspaceUiSounds.play(WorkspaceUiSounds.Action.RESTORE);
        }
        else DesktopClientApi.openPlayerWorkspace();
    }
    @Override public long closeHoldDurationMillis() { return desktop == null ? 500 : desktop.closeAllHoldMillis(); }
}
