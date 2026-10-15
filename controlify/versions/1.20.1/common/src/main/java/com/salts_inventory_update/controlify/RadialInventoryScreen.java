package com.salts_inventory_update.controlify;

import com.mojang.blaze3d.systems.RenderSystem;
import com.salts_inventory_update.api.client.desktop.*;
import com.salts_inventory_update.controlify.input.*;
import com.salts_inventory_update.controlify.model.*;
import dev.isxander.controlify.screenop.ScreenProcessor;
import dev.isxander.controlify.screenop.ScreenProcessorProvider;
import dev.isxander.controlify.api.vmousesnapping.ISnapBehaviour;
import dev.isxander.controlify.api.vmousesnapping.SnapPoint;
import java.util.*;
import net.minecraft.client.gui.GuiGraphics;
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

    @Override public ScreenProcessor<RadialInventoryScreen> screenProcessor() {
        if (controllerProcessor == null) controllerProcessor = ControlifyInputBridge.createScreenProcessor(this);
        return controllerProcessor;
    }

    RadialInventoryScreen(SaltsControlifyClient owner, InventoryMenu menu, Inventory inventory) {
        super(menu, inventory, Component.literal("Inventory"));
        this.owner = owner;
    }

    @Override protected void init() {
        imageWidth = width; imageHeight = height; leftPos = 0; topPos = 0;
        owner.desktop().initialize(width, height);
    }
    @Override protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) { }
    @Override public void renderBackground(GuiGraphics graphics) { }
    @Override public boolean isPauseScreen() { return false; }
    @Override public boolean shouldCloseOnEsc() { return false; }
    @Override public void onClose() { /* Configured back-button tap/hold is routed by ControlifyInputBridge. */ }
    @Override public void removed() { owner.desktop().cancelInput(); capturedWindow = null; hoveredSlot = null; }
    @Override public void containerTick() { owner.desktop().tick(); }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0x66000000);
        renderWorkspace(graphics, mouseX, mouseY, partialTick);
        if (!owner.radialActive()) owner.desktop().renderCarried(graphics, mouseX, mouseY);
        if (!owner.radialActive()) for (var renderable : ((dev.isxander.controlify.mixins.feature.screenop.ScreenAccessor) (Object) this).getRenderables())
            renderable.render(graphics, mouseX, mouseY, partialTick);
        float progress = ControlifyInputBridge.closeProgress();
        if (progress > 0.25f) {
            int x = width / 2 - 52;
            graphics.fill(x, 8, x + 104, 27, 0xDD101010);
            graphics.fill(x, 25, x + Math.round(104 * progress), 27, 0xFFFFD84A);
            graphics.drawCenteredString(font, Component.literal("Closing All"), width / 2, 13, 0xFFFFFFFF);
        }
    }

    void renderWorkspace(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        placements = owner.animatedLayout(graphics.guiWidth(), graphics.guiHeight());
        updateHover(mouseX, mouseY);
        for (var entry : placements.windows().entrySet()) {
            if (owner.model().mode() == RadialWorkspace.Mode.SIDE_BY_SIDE
                    && !entry.getKey().equals(owner.model().focused()) && !entry.getKey().equals(owner.model().selected())) continue;
            var window = owner.windows().get(entry.getKey());
            var placement = entry.getValue();
            if (window == null || placement.opacity() < 0.005) continue;
            var bounds = window.handle().state();
            boolean hovered = !owner.radialActive() && entry.getKey().equals(hoveredWindow);
            graphics.flush();
            RenderSystem.setShaderColor(1, 1, 1, (float) placement.opacity());
            graphics.pose().pushPose();
            try (var transform = com.salts_inventory_update.controlify.render.WorkspaceRenderTransform.enter()) {
                graphics.pose().translate(placement.x(), placement.y(), 0);
                graphics.pose().scale((float) placement.scale(), (float) placement.scale(), 1);
                graphics.pose().translate(-bounds.x(), -bounds.y(), 0);
                owner.desktop().renderWindow(entry.getKey(), graphics,
                        hovered ? (int) nativeMouseX : Integer.MIN_VALUE,
                        hovered ? (int) nativeMouseY : Integer.MIN_VALUE, partialTick);
                graphics.flush();
            } finally {
                graphics.pose().popPose();
                RenderSystem.setShaderColor(1, 1, 1, 1);
            }
            if (placement.highlight() > 0.001) {
                int alpha = (int) (255 * placement.highlight());
                graphics.renderOutline((int) placement.x() - 2, (int) placement.y() - 2,
                    (int) Math.ceil(bounds.width() * placement.scale()) + 4,
                    (int) Math.ceil(bounds.height() * placement.scale()) + 4, alpha << 24 | 0xFFD84A);
            }
        }
        var tuck = placements.tuck();
        if (tuck.opacity() > 0.01) {
            int x = (int) tuck.x(), y = (int) tuck.y();
            graphics.fill(x - 46, y - 11, x + 46, y + 11,
                    (int) (210 * tuck.opacity()) << 24 | (tuck.highlight() > 0.5 ? 0x8A6910 : 0x202020));
            graphics.drawCenteredString(font, Component.literal("Tuck Away"), x, y - 4,
                    (int) (255 * tuck.opacity()) << 24 | 0xFFFFFF);
        }
    }

    private void updateHover(double x, double y) {
        Slot previousSlot = hoveredSlot;
        hoveredWindow = null; hoveredSlot = null;
        if (placements == null || owner.radialActive() || owner.tuckedAway()) return;
        // Only the displayed pair receives inputs, even while other previews animate out.
        for (UUID id : List.of(owner.model().selected(), owner.model().focused())) {
            var placement = placements.windows().get(id);
            var window = owner.windows().get(id);
            if (placement == null || window == null || placement.opacity() < 0.1) continue;
            var bounds = window.handle().state();
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

    /** Controlify 2.0.4 uses a returned set; the native Forge port uses a collector. */
    public Set<SnapPoint> getSnapPoints() {
        Set<SnapPoint> points = new LinkedHashSet<>();
        collectWorkspaceSnapPoints(points::add);
        return points;
    }
    public void controlify$collectSnapPoints(java.util.function.Consumer<SnapPoint> consumer) {
        collectWorkspaceSnapPoints(consumer);
    }
    private void collectWorkspaceSnapPoints(java.util.function.Consumer<SnapPoint> consumer) {
        if (owner == null || placements == null || owner.radialActive() || owner.tuckedAway()) return;
        for (UUID id : List.of(owner.model().focused(), owner.model().selected())) {
            var window = owner.windows().get(id);
            var placement = placements.windows().get(id);
            if (window == null || placement == null || placement.opacity() < 0.1) continue;
            var bounds = window.handle().state();
            for (var slot : owner.desktop().visibleSlots(id)) {
                int x = (int) Math.round(placement.x() + (slot.x() + 8 - bounds.x()) * placement.scale());
                int y = (int) Math.round(placement.y() + (slot.y() + 8 - bounds.y()) * placement.scale());
                consumer.accept(new SnapPoint(x, y, Math.max(1, (int) Math.round(17 * placement.scale()))));
            }
        }
    }

    @Override public Slot controllerHoveredSlot() { return hoveredSlot; }
    @Override public void controllerClickSlot(Slot slot, int slotId, int button, ClickType type) {
        if (owner.radialActive()) return;
        if (slot == null && slotId == -999) owner.desktop().dropCarried(button);
        else if (hoveredWindow != null) owner.desktop().clickAt(hoveredWindow, nativeMouseX, nativeMouseY, button, type);
    }
    @Override protected void slotClicked(Slot slot, int slotId, int button, ClickType type) { controllerClickSlot(slot, slotId, button, type); }

    private boolean pointer(DesktopWorkspace.PointerAction action, double x, double y, int button, double dx, double dy) {
        if (owner.radialActive() || owner.tuckedAway()) return true;
        updateHover(x, y);
        UUID id = action == DesktopWorkspace.PointerAction.DRAG || action == DesktopWorkspace.PointerAction.RELEASE ? capturedWindow : hoveredWindow;
        if (id == null) {
            if (action == DesktopWorkspace.PointerAction.PRESS && placements != null) {
                for (UUID displayed : List.of(owner.model().selected(), owner.model().focused())) {
                    var placement = placements.windows().get(displayed);
                    var window = owner.windows().get(displayed);
                    if (placement == null || window == null || placement.opacity() < 0.1) continue;
                    var bounds = window.handle().state();
                    owner.desktop().dismissOutsidePointer(displayed,
                            (x - placement.x()) / placement.scale() + bounds.x(),
                            (y - placement.y()) / placement.scale() + bounds.y());
                }
            }
            return true; // Empty workspace never sends world interactions.
        }
        var placement = placements.windows().get(id);
        var window = owner.windows().get(id);
        if (placement == null || window == null) return true;
        var bounds = window.handle().state();
        double localX = (x - placement.x()) / placement.scale() + bounds.x();
        double localY = (y - placement.y()) / placement.scale() + bounds.y();
        if (action == DesktopWorkspace.PointerAction.PRESS) capturedWindow = id;
        boolean result = owner.desktop().pointer(id, action, localX, localY, button, dx / placement.scale(), dy / placement.scale());
        if (action == DesktopWorkspace.PointerAction.RELEASE) capturedWindow = null;
        return result;
    }
    @Override public boolean mouseClicked(double x, double y, int button) { return pointer(DesktopWorkspace.PointerAction.PRESS, x, y, button, 0, 0); }
    @Override public boolean mouseReleased(double x, double y, int button) { return pointer(DesktopWorkspace.PointerAction.RELEASE, x, y, button, 0, 0); }
    @Override public boolean mouseDragged(double x, double y, int button, double dx, double dy) { return pointer(DesktopWorkspace.PointerAction.DRAG, x, y, button, dx, dy); }
    @Override public void mouseMoved(double x, double y) { pointer(DesktopWorkspace.PointerAction.MOVE, x, y, 0, 0, 0); }
    @Override public boolean mouseScrolled(double x, double y, double dy) { return pointer(DesktopWorkspace.PointerAction.SCROLL, x, y, 0, 0, dy); }
    @Override public boolean keyPressed(int key, int scanCode, int modifiers) { return owner.desktop().keyPressed(key, scanCode, modifiers); }
    @Override public boolean keyReleased(int key, int scanCode, int modifiers) { return owner.desktop().keyReleased(key, scanCode, modifiers); }
    @Override public boolean charTyped(char character, int modifiers) { return owner.desktop().charTyped(character, modifiers); }
}
