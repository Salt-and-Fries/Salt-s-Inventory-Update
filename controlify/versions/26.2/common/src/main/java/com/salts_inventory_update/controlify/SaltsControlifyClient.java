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
public final class SaltsControlifyClient implements DesktopWorkspaceHost, ControllerWorkspaceActions {
    private static final Logger LOGGER = LoggerFactory.getLogger("salts_inventory_controlify");
    private static SaltsControlifyClient instance;
    private final ControlifyConfig config;
    private DesktopWorkspace desktop;
    private RadialWorkspace<UUID> model;
    private RadialInventoryScreen screen;
    private final WorkspaceAnimator<UUID> animator = new WorkspaceAnimator<>();
    private final RadialLayout layout = new RadialLayout();
    private final Map<UUID, DesktopWorkspaceWindow> windows = new LinkedHashMap<>();
    private long tuckedAt;
    private boolean synchronizing;

    private SaltsControlifyClient(ControlifyConfig config) { this.config = config; }

    public static void initialize() {
        if (instance != null) return;
        try {
            Path path = Minecraft.getInstance().gameDirectory.toPath().resolve("config/salts_inventory_controlify.properties");
            instance = new SaltsControlifyClient(ControlifyConfig.load(path));
        } catch (IOException failure) {
            throw new IllegalStateException("Could not load controller inventory configuration", failure);
        }
        ControlifyInputBridge.initialize(instance, RadialInventoryScreen.class);
        DesktopClientApi.setWorkspaceHost(instance);
        DesktopClientApi.onClientTick(instance::tick);
    }

    @Override public boolean enabled() { return ControlifyInputBridge.usingController(); }

    @Override public void opened(DesktopWorkspace workspace) {
        if (synchronizing) return;
        synchronizing = true;
        try {
            boolean changed = desktop == null || !desktop.isValid();
            desktop = workspace;
            UUID requested = desktop.windows().stream().filter(w -> w.handle().state().inventoryFocused()
                    || w.handle().state().keyboardFocused()).map(w -> w.handle().instanceId()).reduce((a, b) -> b).orElse(null);
            desktop.forceInventoryHud(true);
            desktop.initialize(Minecraft.getInstance().getWindow().getGuiScaledWidth(), Minecraft.getInstance().getWindow().getGuiScaledHeight());
            Set<UUID> before = new LinkedHashSet<>(windows.keySet());
            var inventory = desktop.ensureWindow(DesktopWorkspaceWindow.Kind.PLAYER_INVENTORY);
            var character = desktop.ensureWindow(DesktopWorkspaceWindow.Kind.CHARACTER);
            desktop.protectWindow(inventory.instanceId());
            desktop.protectWindow(character.instanceId());
            if (changed || model == null || !model.windows().contains(inventory.instanceId())) {
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
            if (minecraft.gui.screen() == screen) minecraft.gui.setScreen(null);
            desktop = null; model = null; windows.clear(); screen = null; animator.reset();
            ControlifyInputBridge.reset();
            return;
        }
        if (!enabled()) {
            desktop.forceInventoryHud(false);
            if (model != null && model.active()) {
                boolean wasVisible = !tuckedAway();
                model.deactivate();
                if (minecraft.gui.screen() == screen) minecraft.gui.setScreen(null);
                if (wasVisible) windows.values().stream().filter(w -> w.handle().instanceId().equals(model.focused())).findFirst().ifPresent(w -> w.handle().focus());
            }
            return;
        }
        if (!model.active() && desktop.isNativeScreenActive()) opened(desktop);
        syncWindows();
        if (model.active() && minecraft.gui.screen() == null && !tuckedAway()) show();
        if (tuckedAway()) desktop.tick();
        updateTransferPair();
    }

    private void show() {
        Minecraft minecraft = Minecraft.getInstance();
        if (screen == null) screen = new RadialInventoryScreen(this, minecraft.player.inventoryMenu, minecraft.player.getInventory());
        if (minecraft.gui.screen() != screen) minecraft.gui.setScreen(screen);
    }

    private void updateTransferPair() {
        if (model != null) desktop.transferPair(model.focused(), model.selected());
    }

    WorkspaceLayout<UUID> animatedLayout(int width, int height) {
        Map<UUID, WindowSize> sizes = new LinkedHashMap<>();
        windows.forEach((id, window) -> {
            var state = window.handle().state();
            sizes.put(id, new WindowSize(Math.max(1, state.width()), Math.max(1, state.height())));
        });
        return animator.animate(layout.compute(model, sizes, Math.max(1, width), Math.max(1, height), 1),
                System.nanoTime(), config.animationsEnabled(), 220);
    }

    public static void renderGameplayTransition(GuiGraphicsExtractor graphics, float partialTick) {
        if (instance == null || !instance.tuckedAway() || Minecraft.getInstance().gui.screen() != null
                || !instance.config.animationsEnabled() || System.nanoTime() - instance.tuckedAt > 260_000_000L) return;
        if (instance.screen != null) instance.screen.renderWorkspace(graphics, Integer.MIN_VALUE, Integer.MIN_VALUE, partialTick);
    }

    DesktopWorkspace desktop() { return desktop; }
    Map<UUID, DesktopWorkspaceWindow> windows() { return Collections.unmodifiableMap(windows); }
    RadialWorkspace<UUID> model() { return model; }
    ControlifyConfig config() { return config; }

    @Override public boolean workspaceActive() { return model != null && model.active(); }
    @Override public boolean radialActive() { return workspaceActive() && model.mode() == RadialWorkspace.Mode.RADIAL; }
    @Override public boolean tuckedAway() { return workspaceActive() && model.mode() == RadialWorkspace.Mode.TUCKED_AWAY; }
    @Override public void enterRadial() { if (model.beginRadial()) desktop.cancelInput(); }
    @Override public void radialDirection(float x, float y) { model.pointStick(x, y, 0.18); }
    @Override public void releaseRadial() {
        model.releaseRadial(); updateTransferPair();
        if (tuckedAway()) {
            tuckedAt = System.nanoTime();
            desktop.cancelInput();
            Minecraft.getInstance().gui.setScreen(null);
        }
    }
    @Override public void cycleSelected(int steps) { if (model.cycleSelected(steps)) { desktop.cancelInput(); updateTransferPair(); } }
    @Override public void swapFocused() { if (model.swapHighlighted()) { desktop.cancelInput(); updateTransferPair(); } }
    @Override public void closeHighlighted() {
        model.highlighted().filter(model::closable).ifPresent(id -> {
            var window = windows.get(id);
            if (window != null && window.handle().available(DesktopWindowAction.USER_CLOSE)
                    && window.handle().close() && !window.handle().isOpen()) model.remove(id);
        });
        syncWindows(); updateTransferPair();
    }
    @Override public void closeWorkspace() {
        desktop.closeClosableWindows(); model.closeAll(); syncWindows();
        Minecraft.getInstance().gui.setScreen(null);
    }
    @Override public void restoreWorkspace() {
        if (model != null && model.restore()) { desktop.cancelInput(); show(); }
        else DesktopClientApi.openPlayerWorkspace();
    }
    @Override public long closeHoldDurationMillis() { return desktop == null ? 500 : desktop.closeAllHoldMillis(); }
}
