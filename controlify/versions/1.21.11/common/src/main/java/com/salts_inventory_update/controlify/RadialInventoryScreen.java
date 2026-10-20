package com.salts_inventory_update.controlify;

import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.CharacterEvent;
import com.salts_inventory_update.api.client.desktop.*;
import com.salts_inventory_update.controlify.input.*;
import com.salts_inventory_update.controlify.model.*;
import dev.isxander.controlify.screenop.ScreenProcessor;
import dev.isxander.controlify.screenop.ScreenProcessorProvider;
import dev.isxander.controlify.api.vmousesnapping.ISnapBehaviour;
import dev.isxander.controlify.api.vmousesnapping.SnapPoint;
import java.util.*;
import com.salts_inventory_update.api.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.*;

/** A normal modal container screen for Controlify; all actual slot actions stay in Salt's API. */
public final class RadialInventoryScreen extends AbstractContainerScreen<InventoryMenu>
        implements WorkspaceContainerScreen, ScreenProcessorProvider, ISnapBehaviour {
    private final SaltsControlifyClient owner;
    private WorkspaceLayout<UUID> placements;
    private UUID hoveredWindow;
    private double nativeMouseX, nativeMouseY;
    private UUID capturedWindow;
    private ScreenProcessor<RadialInventoryScreen> controllerProcessor;
    private final ContextualRadialOverlay contextual;
    private final com.salts_inventory_update.controlify.model.WorkspaceSurfaceTransition<UUID> surfaces = new com.salts_inventory_update.controlify.model.WorkspaceSurfaceTransition<>();
    private boolean contextualRequiresRelease;

    @Override public ScreenProcessor<RadialInventoryScreen> screenProcessor() {
        if (controllerProcessor == null) controllerProcessor = ControlifyInputBridge.createScreenProcessor(this);
        return controllerProcessor;
    }

    RadialInventoryScreen(SaltsControlifyClient owner, InventoryMenu menu, Inventory inventory) {
        super(menu, inventory, Component.literal("Inventory"));
        this.owner = owner;
        this.contextual = new ContextualRadialOverlay(owner);
    }

    @Override protected void init() {
        imageWidth = width; imageHeight = height; leftPos = 0; topPos = 0;
        owner.desktop().initialize(width, height);
    }

    @Override protected void renderBg(net.minecraft.client.gui.GuiGraphics graphics, float partialTick, int x, int y) { }
    @Override public void renderBackground(net.minecraft.client.gui.GuiGraphics graphics, int x, int y, float partialTick) { }
    @Override protected boolean hasClickedOutside(double x, double y, int left, int top) {
        return x < 0 || y < 0 || x >= width || y >= height;
    }
    @Override public boolean isPauseScreen() { return false; }
    @Override public boolean shouldCloseOnEsc() { return false; }
    @Override public void onClose() { /* Back input is routed by ControlifyInputBridge. */ }
    @Override public void removed() { owner.desktop().cancelInput(); resetControllerState(); }
    void resetControllerState() {
        capturedWindow = null; hoveredWindow = null; hoveredSlot = null; placements = null;
        contextualRequiresRelease = false;
        contextual.finishImmediately();
        surfaces.reset();
        ControlifyInputBridge.cancelWheelToggles();
    }
    @Override public void containerTick() { owner.desktop().tick(); }

    @Override public void render(net.minecraft.client.gui.GuiGraphics nativeGraphics, int mouseX, int mouseY, float partialTick) {
        var graphics = GuiGraphicsExtractor.wrap(nativeGraphics);
        // The workspace owns world dimming, including both wheel transitions. Texture
        // transparency must never make the world flash brighter behind live windows.
        graphics.fill(0, 0, width, height, 0x66000000);
        renderWorkspace(graphics, mouseX, mouseY, partialTick);
        if (contextual.presenting()) {
            int contentHeight = Math.max(1, height - WorkspaceActionGuide.reservedBottomSpace(this));
            graphics.enableScissor(0, 0, width, contentHeight);
            try {
                contextual.render(graphics, width, contentHeight);
                var frame = contextual.presentationFrame();
                if (frame != null) {
                    renderLiveSource(graphics, contextual.presentationSource(), frame.container(), partialTick);
                    renderLiveDestination(graphics, contextual.presentationDestination(), frame.destination(), partialTick);
                }
            } finally {
                graphics.disableScissor();
            }
        }
        WorkspaceEffectsTray.render(owner, placements, graphics, mouseX, mouseY,
                Math.max(1, height - WorkspaceActionGuide.reservedBottomSpace(this)));
        if (!owner.radialActive() && !contextual.active()) {
            float scale = controllerCursorScale(mouseX, mouseY);
            graphics.pose().pushMatrix();
            try {
                graphics.pose().translate(mouseX, mouseY);
                graphics.pose().scale(scale, scale);
                graphics.pose().translate(-mouseX, -mouseY);
                owner.desktop().renderCarried(graphics, mouseX, mouseY);
            } finally { graphics.pose().popMatrix(); }
        }
        if (!owner.radialActive() && !contextual.active()) for (var renderable : ((dev.isxander.controlify.mixins.feature.screenop.ScreenAccessor) (Object) this).getRenderables())
            renderable.render(graphics.unwrap(), mouseX, mouseY, partialTick);
        WorkspaceActionGuide.render(this, graphics, mouseX, mouseY, partialTick);
    }

    void renderWorkspace(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        boolean wasPresenting = contextual.presenting();
        boolean sourceStillDisplayed = !wasPresenting || contextual.presentationSource().equals(owner.model().focused())
                || contextual.presentationSource().equals(owner.model().selected());
        if (wasPresenting && (!owner.workspaceActive() || owner.radialActive() || owner.tuckedAway()
                || !sourceStillDisplayed))
            contextual.finishImmediately();
        int reserved = owner.tuckedAway() ? 0 : WorkspaceActionGuide.reservedBottomSpace(this);
        int contentHeight = Math.max(1, graphics.guiHeight() - reserved);
        placements = owner.animatedLayout(graphics.guiWidth(), contentHeight);
        if (contextual.presenting()) {
            UUID source = contextual.presentationSource();
            var ordinary = placements.windows().get(source);
            var window = owner.windows().get(source);
            UUID destination = contextual.presentationDestination();
            var destinationOrdinary = placements.windows().get(destination);
            var destinationWindow = owner.windows().get(destination);
            if (ordinary == null || window == null || !window.handle().isOpen() || destinationOrdinary == null
                    || destinationWindow == null || !destinationWindow.handle().isOpen()) {
                contextual.finishImmediately();
            } else {
                contextual.preparePresentation(graphics.guiWidth(), contentHeight, presentationRect(source, ordinary),
                    presentationRect(destination, destinationOrdinary));
                var frame = contextual.presentationFrame();
                if (contextual.presenting() && frame != null) {
                    var shown = new LinkedHashMap<>(placements.windows());
                    var bounds = owner.desktop().presentationBounds(source);
                    var rect = frame.container();
                    shown.put(source, new WindowPlacement(rect.x(), rect.y(), rect.width() / bounds.width(), 1, 0));
                    var destinationBounds = owner.desktop().presentationBounds(destination);
                    var destinationRect = frame.destination();
                    shown.put(destination, new WindowPlacement(destinationRect.x(), destinationRect.y(),
                        destinationRect.width() / destinationBounds.width(), 1, 0));
                    placements = new WorkspaceLayout<>(shown, placements.tuck());
                }
            }
        }
        updateHover(mouseX, mouseY);
        if (reserved > 0) graphics.enableScissor(0, 0, graphics.guiWidth(), contentHeight);
        try {
            renderWorkspaceContents(graphics, partialTick);
        } finally {
            if (reserved > 0) graphics.disableScissor();
        }
    }

    private void renderWorkspaceContents(GuiGraphicsExtractor graphics, float partialTick) {
        surfaces.retain(placements.windows().keySet());
        long surfaceNow = System.nanoTime();
        // Ghosts and outgoing panes render beneath the two active panes.
        var drawOrder = new ArrayList<>(placements.windows().entrySet());
        drawOrder.sort(Comparator.comparingInt(entry -> owner.radialActive()
                ? owner.model().highlighted().filter(entry.getKey()::equals).isPresent() ? 1
                    : entry.getKey().equals(owner.model().focused()) ? 0 : 2
                : entry.getKey().equals(owner.model().focused()) || entry.getKey().equals(owner.model().selected()) ? 1 : 0));
        for (var entry : drawOrder) {
            if (contextual.presenting() && (entry.getKey().equals(contextual.presentationSource())
                    || entry.getKey().equals(contextual.presentationDestination()))) continue;
            var window = owner.windows().get(entry.getKey());
            var placement = entry.getValue();
            if (window == null || !window.handle().isOpen() || placement.opacity() < 0.005) continue;
            var bounds = owner.desktop().presentationBounds(entry.getKey());
            graphics.nextStratum();

            boolean fullSurface = entry.getKey().equals(owner.model().focused())
                    || (owner.radialActive() ? owner.model().highlighted().filter(entry.getKey()::equals).isPresent()
                    : entry.getKey().equals(owner.model().selected()));
            double live = surfaces.value(entry.getKey(), fullSurface, surfaceNow, owner.config().animationsEnabled());
            if (live < .995) {
                com.salts_inventory_update.controlify.render.WorkspacePreviewRenderer.renderWindowIcon(
                        entry.getKey(), owner.desktop().previewIcon(entry.getKey()),
                        placement.withOpacity(placement.opacity() * (1 - live)), bounds, graphics);
            }
            if (live < .005) continue;
            renderNativeWindow(graphics, entry.getKey(), placement.withOpacity(placement.opacity() * live), partialTick);
            if (placement.highlight() > 0.001) {
                int alpha = (int) (255 * placement.highlight());
                graphics.outline((int) placement.x() - 2, (int) placement.y() - 2,
                    (int) Math.ceil(bounds.width() * placement.scale()) + 4,
                    (int) Math.ceil(bounds.height() * placement.scale()) + 4, alpha << 24 | 0xFFD84A);
            }
        }
        var tuck = placements.tuck();
        if (tuck.opacity() > 0.01) {
            com.salts_inventory_update.controlify.render.WorkspacePreviewRenderer.renderTuck(graphics, tuck);
        }
    }

    private ContextualPresentationLayout.Rect presentationRect(UUID id, WindowPlacement placement) {
        var bounds = owner.desktop().presentationBounds(id);
        return new ContextualPresentationLayout.Rect(placement.x(), placement.y(),
                bounds.width() * placement.scale(), bounds.height() * placement.scale());
    }

    /** The moved preview is the live native menu; its rendered pose is also its input placement. */
    private void renderLiveSource(GuiGraphicsExtractor graphics, UUID id,
            ContextualPresentationLayout.Rect rect, float partialTick) {
        var bounds = owner.desktop().presentationBounds(id);
        renderNativeWindow(graphics, id,
                new WindowPlacement(rect.x(), rect.y(), rect.width() / bounds.width(), 1, 0), partialTick);
        renderRecipeHoverGhosts(graphics, id, rect);
    }

    /** Missing ingredients are artwork over real native input slots, never inventory stacks. */
    private void renderRecipeHoverGhosts(GuiGraphicsExtractor graphics, UUID id,
            ContextualPresentationLayout.Rect rect) {
        if (!contextual.active()) return;
        var ghosts = owner.desktop().recipeHoverGhosts(id);
        if (ghosts.isEmpty()) return;
        var slots = owner.desktop().visibleSlots(id);
        var bounds = owner.desktop().presentationBounds(id);
        graphics.pose().pushMatrix();
        try (var opacity = com.salts_inventory_update.controlify.render.WorkspaceOpacity.enter(.55f);
             var transform = com.salts_inventory_update.controlify.render.WorkspaceRenderTransform.enter()) {
            graphics.pose().translate((float) rect.x(), (float) rect.y());
            graphics.pose().scale((float) (rect.width() / bounds.width()), (float) (rect.width() / bounds.width()));
            graphics.pose().translate(-bounds.x(), -bounds.y());
            for (var ghost : ghosts) {
                var hit = slots.stream().filter(slot -> slot.menuSlotId() == ghost.slotIndex()).findFirst().orElse(null);
                if (hit == null || ghost.item().isEmpty()) continue;
                var nativeSlot = owner.desktop().hoveredSlot(id, hit.x() + 8, hit.y() + 8);
                if (nativeSlot == null || nativeSlot.hasItem()) continue;
                graphics.nextStratum();
                graphics.item(ghost.item(), hit.x(), hit.y(), ghost.slotIndex());
                graphics.nextStratum();
                graphics.fill(hit.x(), hit.y(), hit.x() + 16, hit.y() + 16, 0x88EF3535);
            }
        } finally {
            graphics.pose().popMatrix();
        }
    }

    /** Keeps the exact output destination live beneath the ingredients, using its shared input placement. */
    private void renderLiveDestination(GuiGraphicsExtractor graphics, UUID id,
            ContextualPresentationLayout.Rect rect, float partialTick) {
        var bounds = owner.desktop().presentationBounds(id);
        renderNativeWindow(graphics, id,
            new WindowPlacement(rect.x(), rect.y(), rect.width() / bounds.width(), 1, 0), partialTick);
    }

    private void renderNativeWindow(GuiGraphicsExtractor graphics, UUID id, WindowPlacement placement, float partialTick) {
        var bounds = owner.desktop().presentationBounds(id);
        boolean hovered = !contextual.active() && !owner.radialActive() && id.equals(hoveredWindow);
        graphics.nextStratum();
        graphics.pose().pushMatrix();
        try (var opacity = com.salts_inventory_update.controlify.render.WorkspaceOpacity.enter((float) placement.opacity());
             var transform = com.salts_inventory_update.controlify.render.WorkspaceRenderTransform.enter();
             var tooltip = WorkspaceTooltipCoordinates.enter(placement.x(), placement.y(), bounds.x(), bounds.y(), placement.scale())) {
            graphics.pose().translate((float) placement.x(), (float) placement.y());
            graphics.pose().scale((float) placement.scale(), (float) placement.scale());
            graphics.pose().translate(-bounds.x(), -bounds.y());
            owner.desktop().renderWindow(id, graphics,
                    hovered ? (int) nativeMouseX : Integer.MIN_VALUE,
                    hovered ? (int) nativeMouseY : Integer.MIN_VALUE, partialTick);
            graphics.nextStratum();
        } finally {
            graphics.pose().popMatrix();
        }
    }

    private void updateHover(double x, double y) {
        if (WorkspaceSearchKeyboard.activeFor(this) || WorkspaceSearchKeyboard.waitingNeutral()) return;
        Slot previousSlot = hoveredSlot;
        hoveredWindow = null; hoveredSlot = null;
        if (placements == null || owner.radialActive() || owner.tuckedAway() || contextual.active()) return;
        // Only the displayed pair receives inputs, even while other previews animate out.
        for (UUID id : List.of(owner.model().selected(), owner.model().focused())) {
            var placement = placements.windows().get(id);
            var window = owner.windows().get(id);
            if (placement == null || window == null || !window.handle().isOpen() || placement.opacity() < 0.1) continue;
            var bounds = owner.desktop().presentationBounds(id);
            double localX = (x - placement.x()) / placement.scale() + bounds.x();
            double localY = (y - placement.y()) / placement.scale() + bounds.y();
            if (owner.desktop().containsPointer(id, localX, localY)) {
                hoveredWindow = id; nativeMouseX = localX; nativeMouseY = localY;
                hoveredSlot = owner.desktop().hoveredSlot(id, localX, localY);
                ControlifyInputBridge.hoveredSlotChanged(this, hoveredSlot, previousSlot);
                return;
            }
        }
        ControlifyInputBridge.hoveredSlotChanged(this, null, previousSlot);
    }

    @Override public void controlify$collectSnapPoints(java.util.function.Consumer<SnapPoint> consumer) {
        if (owner == null || placements == null || owner.radialActive() || owner.tuckedAway() || contextual.active()) return;
        for (UUID id : List.of(owner.model().focused(), owner.model().selected())) {
            var window = owner.windows().get(id);
            var placement = placements.windows().get(id);
            if (window == null || !window.handle().isOpen() || placement == null || placement.opacity() < 0.1) continue;
            var bounds = owner.desktop().presentationBounds(id);
            for (var control : owner.desktop().controllerTargets(id)) {
                int x = (int) Math.round(placement.x() + (control.x() + control.width() / 2.0 - bounds.x()) * placement.scale());
                int y = (int) Math.round(placement.y() + (control.y() + control.height() / 2.0 - bounds.y()) * placement.scale());
                int range = Math.max(1, (int) Math.round(Math.min(control.width(), control.height()) * placement.scale() / 2.0));
                consumer.accept(new SnapPoint(x, y, range));
            }
        }
    }

    @Override public SnapPoint controllerNavigate(net.minecraft.client.gui.navigation.ScreenDirection direction, double x, double y) {
        if (WorkspaceSearchKeyboard.activeFor(this) || WorkspaceSearchKeyboard.waitingNeutral()) return null;
        if (placements == null || owner.radialActive() || owner.tuckedAway() || contextual.active()) return null;
        var targets = new ArrayList<LogicalMenuNavigation.Target>();
        for (UUID id : List.of(owner.model().focused(), owner.model().selected())) {
            var window = owner.windows().get(id);
            var placement = placements.windows().get(id);
            if (window == null || !window.handle().isOpen() || placement == null || placement.opacity() < 0.1) continue;
            var bounds = owner.desktop().presentationBounds(id);
            for (var target : owner.desktop().controllerTargets(id)) targets.add(new LogicalMenuNavigation.Target(
                    id + "/" + target.group(), target.row(), target.column(),
                    placement.x() + (target.x() + target.width() / 2.0 - bounds.x()) * placement.scale(),
                    placement.y() + (target.y() + target.height() / 2.0 - bounds.y()) * placement.scale(),
                    target.width() * placement.scale(), target.height() * placement.scale()));
        }
        var logicalDirection = direction == net.minecraft.client.gui.navigation.ScreenDirection.LEFT ? LogicalMenuNavigation.Direction.LEFT
                : direction == net.minecraft.client.gui.navigation.ScreenDirection.RIGHT ? LogicalMenuNavigation.Direction.RIGHT
                : direction == net.minecraft.client.gui.navigation.ScreenDirection.UP ? LogicalMenuNavigation.Direction.UP
                : LogicalMenuNavigation.Direction.DOWN;
        var next = LogicalMenuNavigation.next(targets, x, y, logicalDirection);
        if (next != null) WorkspaceUiSounds.play(WorkspaceUiSounds.Action.NAVIGATE);
        return next == null ? null : new SnapPoint((int) Math.round(next.x()), (int) Math.round(next.y()),
                Math.max(1, (int) Math.round(Math.min(next.width(), next.height()) / 2)));
    }

    @Override public boolean controllerScroll(double x, double y, int direction, boolean edgeOnly) {
        if (WorkspaceSearchKeyboard.activeFor(this) || WorkspaceSearchKeyboard.waitingNeutral()) return false;
        if (placements == null || owner.radialActive() || owner.tuckedAway() || contextual.active()) return false;
        updateHover(x, y);
        boolean scrolled = hoveredWindow != null && owner.desktop().scrollController(hoveredWindow, nativeMouseX, nativeMouseY, direction, edgeOnly);
        if (scrolled) WorkspaceUiSounds.play(WorkspaceUiSounds.Action.SCROLL);
        return scrolled;
    }

    private void updateControllerHover() {
        var client = net.minecraft.client.Minecraft.getInstance();
        var mouse = dev.isxander.controlify.Controlify.instance().virtualMouseHandler();
        updateHover(mouse.getCurrentX(1f) * client.getWindow().getGuiScaledWidth() / client.getWindow().getScreenWidth(),
                mouse.getCurrentY(1f) * client.getWindow().getGuiScaledHeight() / client.getWindow().getScreenHeight());
    }

    @Override public boolean controllerCreativeHotbar(boolean stack) {
        if (WorkspaceSearchKeyboard.activeFor(this) || WorkspaceSearchKeyboard.waitingNeutral()) return false;
        if (placements == null || owner.radialActive() || owner.tuckedAway() || contextual.active()) return false;
        updateControllerHover();
        return creativeHotbarAtHover(stack);
    }

    private CreativeSearchTarget hoveredCreativeSearch() {
        if (WorkspaceSearchKeyboard.activeFor(this) || WorkspaceSearchKeyboard.waitingNeutral()) return null;
        if (placements == null || !owner.workspaceActive() || owner.radialActive() || owner.tuckedAway() || contextual.active()) return null;
        return hoveredWindow == null ? null : CreativeSearchTarget.find(owner.desktop(), owner.windows().get(hoveredWindow));
    }

    @Override public boolean controllerCreativeSearchAvailable() {
        updateControllerHover();
        return hoveredCreativeSearch() != null;
    }

    @Override public boolean controllerCreativeSearch(boolean fieldOnly) {
        updateControllerHover();
        return openCreativeSearch(fieldOnly);
    }

    private boolean openCreativeSearch(boolean fieldOnly) {
        var target = hoveredCreativeSearch();
        if (target == null || fieldOnly && !target.contains(nativeMouseX, nativeMouseY)) return false;
        owner.desktop().cancelInput();
        capturedWindow = null;
        hoveredSlot = null;
        WorkspaceSearchKeyboard.open(this, owner.desktop(), target.window(), target.field());
        return true; // An attempted Search action must never fall through into an item click.
    }

    @Override public boolean controllerCreativeDeleteHotbar() {
        if (WorkspaceSearchKeyboard.activeFor(this) || WorkspaceSearchKeyboard.waitingNeutral()) return false;
        if (placements == null || owner.radialActive() || owner.tuckedAway() || contextual.active()) return false;
        updateControllerHover();
        if (hoveredWindow == null || !owner.desktop().creativeHotbarDeleteAt(hoveredWindow, nativeMouseX, nativeMouseY)) return false;
        owner.desktop().clickAt(hoveredWindow, nativeMouseX, nativeMouseY, 0, net.minecraft.world.inventory.ClickType.QUICK_MOVE);
        WorkspaceUiSounds.play(WorkspaceUiSounds.Action.CONFIRM);
        return true;
    }

    private boolean creativeHotbarAtHover(boolean stack) {
        var player = net.minecraft.client.Minecraft.getInstance().player;
        if (hoveredWindow == null || player == null) return false;
        int before = 0;
        for (int slot = 0; slot < 9; slot++) before += player.getInventory().getItem(slot).getCount();
        boolean carriedBefore = !owner.desktop().carriedStack().isEmpty();
        boolean handled = owner.desktop().creativeHotbarAt(hoveredWindow, nativeMouseX, nativeMouseY, stack);
        if (handled) {
            int after = 0;
            for (int slot = 0; slot < 9; slot++) after += player.getInventory().getItem(slot).getCount();
            boolean clearedCursor = carriedBefore && owner.desktop().carriedStack().isEmpty();
            WorkspaceUiSounds.play(after > before || clearedCursor ? WorkspaceUiSounds.Action.CONFIRM : WorkspaceUiSounds.Action.REJECT);
        }
        return handled;
    }

    @Override public boolean contextualRadialActive() { return contextual.active(); }

    @Override public WorkspaceActionGuide.State controllerGuideState() {
        var mode = WorkspaceActionGuide.Mode.SIDE;
        var quick = DesktopContextualMenu.Kind.NONE;
        if (owner == null || owner.desktop() == null || owner.model() == null)
            return new WorkspaceActionGuide.State(mode, quick, false, false, false, false, false, false, false,
                    null, net.minecraft.world.item.ItemStack.EMPTY);
        var desktop = owner.desktop();
        if (contextual.active()) {
            mode = contextual.kind() == DesktopContextualMenu.Kind.CREATIVE ? WorkspaceActionGuide.Mode.CREATIVE_TABS
                    : contextual.kind() == DesktopContextualMenu.Kind.COOKING ? WorkspaceActionGuide.Mode.COOKING
                    : WorkspaceActionGuide.Mode.RECIPE;
            return new WorkspaceActionGuide.State(mode, quick, false, false, false, false, false, false,
                    contextual.canConfirm(), null, desktop.carriedStack());
        }
        if (owner.radialActive()) {
            var highlighted = owner.model().highlighted();
            boolean focusable = highlighted.filter(id -> !id.equals(owner.model().focused()))
                    .filter(owner.windows()::containsKey).isPresent();
            boolean closable = highlighted.filter(owner.model()::closable).map(owner.windows()::get)
                    .filter(window -> window != null && window.handle().available(DesktopWindowAction.USER_CLOSE)).isPresent();
            return new WorkspaceActionGuide.State(WorkspaceActionGuide.Mode.LT_RADIAL, quick, focusable,
                    owner.model().tuckHighlighted(), closable, desktop.canSortWindow(owner.model().focused()),
                    false, false, false, null, desktop.carriedStack());
        }
        updateControllerHover();
        boolean creative = false, scrollable = false, creativeHotbarDelete = false;
        if (hoveredWindow != null) {
            var hint = desktop.contextualBookHint(hoveredWindow);
            if (hint != null) quick = hint.kind();
            creative = desktop.creativeCatalogAt(hoveredWindow, nativeMouseX, nativeMouseY);
            creativeHotbarDelete = desktop.creativeHotbarDeleteAt(hoveredWindow, nativeMouseX, nativeMouseY);
            scrollable = desktop.controllerScrollAvailable(hoveredWindow, nativeMouseX, nativeMouseY);
        }
        return new WorkspaceActionGuide.State(mode, quick, false, false, false, false, creative, scrollable, false,
                hoveredSlot, desktop.carriedStack(), creativeHotbarDelete, hoveredCreativeSearch() != null);
    }

    @Override public boolean controllerCanOpenContextual() {
        if (WorkspaceSearchKeyboard.activeFor(this) || WorkspaceSearchKeyboard.waitingNeutral()) return false;
        if (placements == null || !owner.workspaceActive() || owner.radialActive() || owner.tuckedAway()) return false;
        updateControllerHover();
        if (hoveredWindow == null) return false;
        var hint = owner.desktop().contextualBookHint(hoveredWindow);
        return hint != null;
    }

    @Override public boolean controllerContextualInput(boolean held, float rightX, float rightY, float leftX, float leftY,
            boolean categoryLeft, boolean categoryRight, boolean toggleCraftable, boolean confirm,
            boolean rotateLeftHeld, boolean rotateRightHeld, boolean craftStack) {
        if (WorkspaceSearchKeyboard.activeFor(this) || WorkspaceSearchKeyboard.waitingNeutral()) return false;
        if (!held) contextualRequiresRelease = false;
        if (!held || !owner.workspaceActive() || owner.radialActive() || owner.tuckedAway()) {
            if (!owner.workspaceActive() || owner.radialActive() || owner.tuckedAway()) contextual.finishImmediately();
            else contextual.close();
            // Release hands this same sample back to the native inventory processor.
            return false;
        }
        // A rejected or interrupted opening stays closed until the action is released.
        // This also prevents a rebound Select binding falling through into an item click.
        if (contextualRequiresRelease) return true;
        boolean openedNow = false;
        if (!contextual.active()) {
            updateControllerHover();
            if (hoveredWindow == null) return false;
            var hint = owner.desktop().contextualBookHint(hoveredWindow);
            if (hint == null) return false;
            UUID other = hoveredWindow.equals(owner.model().focused()) ? owner.model().selected() : owner.model().focused();
            if (!contextual.open(hoveredWindow, other)) {
                contextualRequiresRelease = true;
                return true;
            }
            var startPlacement = placements.windows().get(hoveredWindow);
            var destinationPlacement = placements.windows().get(other);
            if (startPlacement != null && destinationPlacement != null) contextual.preparePresentation(width,
                    Math.max(1, height - WorkspaceActionGuide.reservedBottomSpace(this)),
                    presentationRect(hoveredWindow, startPlacement), presentationRect(other, destinationPlacement));
            openedNow = true;
            owner.desktop().cancelInput(); capturedWindow = null; hoveredSlot = null;
        }
        // A rebound hold action may share Select. Opening must never confirm a recipe.
        contextual.input(rightX, rightY, leftX, leftY, categoryLeft, categoryRight, toggleCraftable, confirm && !openedNow,
                rotateLeftHeld, rotateRightHeld, craftStack && !openedNow);
        if (!contextual.active()) contextualRequiresRelease = true;
        return true;
    }

    /** Scale the stock 16px cursor with the hovered window's native 16px slot contents. */
    public float controllerCursorScale(double x, double y) {
        if (placements == null || owner.radialActive() || owner.tuckedAway()) return 1;
        for (UUID id : List.of(owner.model().selected(), owner.model().focused())) {
            var placement = placements.windows().get(id);
            var window = owner.windows().get(id);
            if (placement == null || placement.opacity() < 0.1 || window == null || !window.handle().isOpen()) continue;
            var bounds = owner.desktop().presentationBounds(id);
            double localX = (x - placement.x()) / placement.scale() + bounds.x();
            double localY = (y - placement.y()) / placement.scale() + bounds.y();
            if (owner.desktop().containsPointer(id, localX, localY)) return (float) placement.scale();
        }
        return 1;
    }

    @Override public Slot controllerHoveredSlot() { return hoveredSlot; }
    @Override public void controllerClickSlot(Slot slot, int slotId, int button, ClickType type) {
        if (WorkspaceSearchKeyboard.activeFor(this) || WorkspaceSearchKeyboard.waitingNeutral()) return;
        if (owner.radialActive() || contextual.active()) return;
        if (button == 0 && type == ClickType.PICKUP && controllerCreativeHotbar(false)) return;
        if (slot == null && slotId == -999) owner.desktop().dropCarried(button == 0
                ? 0
                : 1);
        else if (hoveredWindow != null) owner.desktop().clickAt(hoveredWindow, nativeMouseX, nativeMouseY, button, type);
    }
    @Override protected void slotClicked(Slot slot, int slotId, int button, ClickType type) { controllerClickSlot(slot, slotId, button, type); }

    private boolean pointer(DesktopWorkspace.PointerAction action, double x, double y, int button, double dx, double dy) {
        if (WorkspaceSearchKeyboard.activeFor(this) || WorkspaceSearchKeyboard.waitingNeutral()) return true;
        if (owner.radialActive() || owner.tuckedAway() || contextual.active()) return true;
        updateHover(x, y);
        if (action == DesktopWorkspace.PointerAction.PRESS && button == 0
                && ControlifyInputBridge.usingController() && openCreativeSearch(true)) return true;
        if (action == DesktopWorkspace.PointerAction.PRESS && button == 0 && hoveredWindow != null
                && ControlifyInputBridge.usingController()
                && creativeHotbarAtHover(false)) return true;
        UUID id = action == DesktopWorkspace.PointerAction.DRAG || action == DesktopWorkspace.PointerAction.RELEASE ? capturedWindow : hoveredWindow;
        if (id == null) {
            if (action == DesktopWorkspace.PointerAction.PRESS && placements != null) {
                for (UUID displayed : List.of(owner.model().selected(), owner.model().focused())) {
                    var placement = placements.windows().get(displayed);
                    var window = owner.windows().get(displayed);
                    if (placement == null || window == null || !window.handle().isOpen() || placement.opacity() < 0.1) continue;
                    var bounds = owner.desktop().presentationBounds(displayed);
                    owner.desktop().dismissOutsidePointer(displayed,
                            (x - placement.x()) / placement.scale() + bounds.x(),
                            (y - placement.y()) / placement.scale() + bounds.y());
                }
            }
            return true; // Empty workspace never sends world interactions.
        }
        var placement = placements.windows().get(id);
        var window = owner.windows().get(id);
        if (placement == null || window == null || !window.handle().isOpen()) return true;
        var bounds = owner.desktop().presentationBounds(id);
        double localX = (x - placement.x()) / placement.scale() + bounds.x();
        double localY = (y - placement.y()) / placement.scale() + bounds.y();
        if (action == DesktopWorkspace.PointerAction.PRESS) capturedWindow = id;
        boolean keyboardClick=action==DesktopWorkspace.PointerAction.PRESS && button==0
            && ControlifyInputBridge.usingController();
        boolean wasEditing=keyboardClick && owner.desktop().textInputActive(id);
        var editors=keyboardClick?owner.desktop().textInputBounds(id):List.<DesktopWindowBounds>of();
        boolean result = owner.desktop().pointer(id, action, localX, localY, button, dx / placement.scale(), dy / placement.scale());
        if (keyboardClick && com.salts_inventory_update.controlify.model.KeyboardActivation.shouldOpen(wasEditing,owner.desktop().textInputActive(id),
                !editors.isEmpty(),editors.stream().anyMatch(field -> localX>=field.x() && localX<field.x()+field.width()
                    && localY>=field.y() && localY<field.y()+field.height()))) {
            owner.desktop().pointer(id, DesktopWorkspace.PointerAction.RELEASE, localX, localY, button, 0, 0);
            capturedWindow = null;
            hoveredSlot = null;
            WorkspaceSearchKeyboard.openFocused(this, owner.desktop(), id, localX, localY);
            return true;
        }
        if (action == DesktopWorkspace.PointerAction.RELEASE) capturedWindow = null;
        return result;
    }
    @Override public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        return WorkspaceSearchKeyboard.suppressWorkspaceMouse(event, false)
            || pointer(DesktopWorkspace.PointerAction.PRESS, event.x(), event.y(), event.button(), 0, 0);
    }
    @Override public boolean mouseReleased(MouseButtonEvent event) {
        return WorkspaceSearchKeyboard.suppressWorkspaceMouse(event, true)
            || pointer(DesktopWorkspace.PointerAction.RELEASE, event.x(), event.y(), event.button(), 0, 0);
    }
    @Override public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) { return pointer(DesktopWorkspace.PointerAction.DRAG, event.x(), event.y(), event.button(), dx, dy); }
    @Override public void mouseMoved(double x, double y) { pointer(DesktopWorkspace.PointerAction.MOVE, x, y, 0, 0, 0); }
    @Override public boolean mouseScrolled(double x, double y, double dx, double dy) { return pointer(DesktopWorkspace.PointerAction.SCROLL, x, y, 0, dx, dy); }
    @Override public boolean keyPressed(KeyEvent event) {
        return WorkspaceSearchKeyboard.suppressWorkspaceKey(event, false) || WorkspaceSearchKeyboard.activeFor(this)
            || owner.desktop().keyPressed(event.key(), event.scancode(), event.modifiers());
    }
    @Override public boolean keyReleased(KeyEvent event) {
        return WorkspaceSearchKeyboard.suppressWorkspaceKey(event, true) || WorkspaceSearchKeyboard.activeFor(this)
            || owner.desktop().keyReleased(event.key(), event.scancode(), event.modifiers());
    }
    @Override public boolean charTyped(CharacterEvent event) {
        return WorkspaceSearchKeyboard.activeFor(this) || WorkspaceSearchKeyboard.waitingNeutral()
            || owner.desktop().charTyped(event.codepoint(), 0);
    }
}
