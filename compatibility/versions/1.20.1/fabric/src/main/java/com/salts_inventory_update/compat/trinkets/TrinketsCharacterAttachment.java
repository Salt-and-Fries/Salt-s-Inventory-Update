package com.salts_inventory_update.compat.trinkets;

import java.util.ArrayList;
import java.util.List;
import com.mojang.blaze3d.systems.RenderSystem;
import com.salts_inventory_update.api.client.desktop.*;
import dev.emi.trinkets.*;
import dev.emi.trinkets.api.*;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;

/** Native Trinkets hover state and artwork attached to the real, player-backed Character window. */
public final class TrinketsCharacterAttachment implements DesktopBuiltinAttachment, TrinketScreen {
    private static final ResourceLocation MORE = new ResourceLocation("trinkets", "textures/gui/more_slots.png");
    private static final ResourceLocation BLANK = new ResourceLocation("trinkets", "textures/gui/blank_back.png");
    private DesktopWindowContext<AbstractContainerMenu, Void> context;
    private Slot focused;
    private List<Slot> topology = List.of();

    private boolean bind(DesktopWindowContext<AbstractContainerMenu, Void> next) {
        context = next;
        if (!(next.model() instanceof TrinketPlayerScreenHandler)) return false;
        if (TrinketScreenManager.currentScreen != this) TrinketScreenManager.init(this);
        if (!topology.equals(next.model().slots)) {
            topology = List.copyOf(next.model().slots);
            // Upstream rebuilds Slot objects even for ordinary content sync. Keep the native
            // hover selection by logical group/type, otherwise clicking a secondary slot closes it.
            String group = TrinketsClient.activeGroup == null ? null : TrinketsClient.activeGroup.getName();
            String type = TrinketsClient.activeType == null ? null : TrinketsClient.activeType.getName();
            var groups = TrinketsApi.getPlayerSlots(next.minecraft().player);
            TrinketsClient.activeGroup = group == null ? null : groups.get(group);
            TrinketsClient.activeType = TrinketsClient.activeGroup == null || type == null ? null
                : trinkets$getHandler().trinkets$getSlotTypes(TrinketsClient.activeGroup).stream().filter(t -> t.getName().equals(type)).findFirst().orElse(null);
            TrinketScreenManager.init(this);
        }
        return true;
    }
    private void clearSelection() {
        TrinketScreenManager.removeSelections();
        TrinketsClient.activeType = null; TrinketsClient.quickMoveType = null;
        TrinketsClient.quickMoveTimer = 0;
        TrinketScreenManager.init(this);
    }
    @Override public void opened(DesktopWindowContext<AbstractContainerMenu, Void> c) { bind(c); }
    @Override public void suspended(DesktopWindowContext<AbstractContainerMenu, Void> c) {
        if (TrinketScreenManager.currentScreen == this) {
            clearSelection(); TrinketScreenManager.currentScreen = null;
        }
    }
    @Override public void closed(DesktopWindowContext<AbstractContainerMenu, Void> c) {
        if (TrinketScreenManager.currentScreen == this) {
            clearSelection(); TrinketScreenManager.currentScreen = null;
        }
        context = null; focused = null; topology = List.of();
    }
    @Override public void tick(DesktopWindowContext<AbstractContainerMenu, Void> c) {
        if (bind(c)) TrinketScreenManager.tick();
    }
    @Override public boolean requestsBuiltinSlot(int menuSlotId) { return menuSlotId == 45; }
    private boolean visible(Slot slot) {
        if (!(slot instanceof TrinketSlot ts) || !slot.isActive()) return false;
        // Native recipe-book occlusion also applies to this replacement screen.
        return !(slot.x < 0 && context.recipeBookVisible());
    }
    private Slot findFocused(double x, double y) {
        for (Slot slot : context.model().slots) if (visible(slot)
                && (TrinketsClient.activeGroup == null || ((TrinketSlot) slot).isTrinketFocused())
                && inside(x, y, context.contentX() + slot.x - 1, context.contentY() + slot.y - 1, 18, 18)) return slot;
        for (int id : new int[] {5, 6, 7, 8, 45}) {
            if (TrinketsClient.activeGroup != null && TrinketsClient.activeGroup.getSlotId() != id) continue;
            var bounds = context.builtinSlotBounds(id);
            if (bounds.isPresent() && inside(x, y, bounds.get().x() - 1, bounds.get().y() - 1, 18, 18)) return context.menuSlot(id);
        }
        return null;
    }
    private void hover(double x, double y) {
        focused = findFocused(x, y);
        TrinketScreenManager.update((float) x, (float) y);
        focused = findFocused(x, y);
    }
    @Override public void render(DesktopRenderContext<AbstractContainerMenu, Void> c) {
        if (!bind(c)) return;
        hover(c.mouseX(), c.mouseY());
        var graphics = c.nativeGraphics();
        graphics.pose().pushPose();
        try {
            graphics.pose().translate(c.contentX(), c.contentY(), 0);
            TrinketScreenManager.drawExtraGroups(graphics);
        } finally {
            graphics.pose().popPose();
            RenderSystem.enableDepthTest();
        }
        // Match native Trinkets' background/foreground distinction. A permanently
        // visible slot from another group must not paint over the expanded popup.
        c.rawGraphics().nextStratum();
        renderSlotBackgrounds(c, false);
        c.rawGraphics().nextStratum();
        renderSlotContents(c, false);
        c.rawGraphics().nextStratum();
        graphics.pose().pushPose();
        try {
            graphics.pose().translate(c.contentX(), c.contentY(), 0);
            // Native drawGroup adds 305. Rebase it into this owner's current Core stratum,
            // so later desktop windows still occlude the panel and its items.
            graphics.pose().translate(0, 0, -305);
            TrinketScreenManager.drawActiveGroup(graphics);
        } finally {
            graphics.pose().popPose();
            RenderSystem.enableDepthTest();
        }
        c.rawGraphics().nextStratum();
        renderSlotBackgrounds(c, true);
        c.rawGraphics().nextStratum();
        renderSlotContents(c, true);
        if (TrinketsClient.activeGroup != null) {
            int anchor = TrinketsClient.activeGroup.getSlotId();
            c.builtinSlotBounds(anchor).ifPresent(bounds -> c.texturelessSlot(anchor, bounds.x(), bounds.y()));
        }
    }
    private void renderSlotBackgrounds(DesktopRenderContext<AbstractContainerMenu, Void> c, boolean popup) {
        for (Slot slot : c.model().slots) if (visible(slot) && ((TrinketSlot) slot).isTrinketFocused() == popup) {
            TrinketSlot ts = (TrinketSlot) slot;
            int x = c.contentX() + slot.x, y = c.contentY() + slot.y;
            if (!ts.isTrinketFocused()) c.texture(MORE, x - 1, y - 1, 4, 4, 18, 18, 18, 18, 256, 256);
            ResourceLocation back = slot.hasItem() || ts.getBackgroundIdentifier() == null ? BLANK : ts.getBackgroundIdentifier();
            c.texture(back, x, y, 0, 0, 16, 16, 16, 16, 16, 16);
        }
    }
    private void renderSlotContents(DesktopRenderContext<AbstractContainerMenu, Void> c, boolean popup) {
        for (Slot slot : c.model().slots) if (visible(slot) && ((TrinketSlot) slot).isTrinketFocused() == popup)
            c.texturelessSlot(c.menuSlotId(slot), c.contentX() + slot.x, c.contentY() + slot.y);
    }
    @Override public DesktopSlotHit slotAt(DesktopSlotContext<AbstractContainerMenu, Void> c, double x, double y) {
        if (!bind(c)) return null;
        Slot slot = findFocused(x, y);
        if (slot instanceof TrinketSlot) return c.menuSlotHit(c.menuSlotId(slot), c.contentX() + slot.x, c.contentY() + slot.y, x, y);
        return null;
    }
    @Override public boolean occludes(DesktopWindowContext<AbstractContainerMenu, Void> c, double x, double y) {
        if (!bind(c) || TrinketsClient.activeGroup == null) return false;
        // The native anchor remains usable. Empty panel pixels own their hit area as well.
        int id = TrinketsClient.activeGroup.getSlotId();
        var anchor = c.builtinSlotBounds(id);
        if (anchor.isPresent() && inside(x, y, anchor.get().x() - 1, anchor.get().y() - 1, 18, 18)) return false;
        return regions(c).stream().anyMatch(r -> r.contains(c.windowX(), c.windowY(), x, y));
    }
    @Override public List<DesktopWindowBounds> placementBounds(DesktopWindowContext<AbstractContainerMenu, Void> c) {
        int ox = c.contentX() - c.windowX(), oy = c.contentY() - c.windowY();
        return c.model().slots.stream().filter(slot -> slot instanceof TrinketSlot)
            .map(slot -> new DesktopWindowBounds(ox + slot.x - 5, oy + slot.y - 5, 26, 26)).toList();
    }
    @Override public List<DesktopWindowRegion> regions(DesktopWindowContext<AbstractContainerMenu, Void> c) {
        if (!bind(c)) return List.of();
        List<DesktopWindowRegion> result = new ArrayList<>();
        int ox = c.contentX() - c.windowX(), oy = c.contentY() - c.windowY();
        for (Slot slot : c.model().slots) if (visible(slot))
            result.add(DesktopWindowRegion.visualAndInteractive(ox + slot.x - 1, oy + slot.y - 1, 18, 18));
        if (TrinketsClient.activeGroup != null) {
            Rect2i r = TrinketScreenManager.currentBounds;
            result.add(DesktopWindowRegion.transientVisualAndInteractive(ox + r.getX(), oy + r.getY(), r.getWidth(), r.getHeight()));
            if (TrinketsClient.activeType != null) {
                r = TrinketScreenManager.typeBounds;
                result.add(DesktopWindowRegion.transientVisualAndInteractive(ox + r.getX(), oy + r.getY(), r.getWidth(), r.getHeight()));
            }
        }
        return List.copyOf(result);
    }
    private static boolean inside(double x, double y, int left, int top, int width, int height) {
        return x >= left && x < left + width && y >= top && y < top + height;
    }
    @Override public TrinketPlayerScreenHandler trinkets$getHandler() { return (TrinketPlayerScreenHandler) context.model(); }
    @Override public Rect2i trinkets$getGroupRect(SlotGroup group) {
        var point = trinkets$getHandler().trinkets$getGroupPos(group);
        if (point == null) return new Rect2i(0, 0, 0, 0);
        var bounds = context.builtinSlotBounds(group.getSlotId());
        if (bounds.isPresent()) return new Rect2i(bounds.get().x() - context.contentX() - 1, bounds.get().y() - context.contentY() - 1, 17, 17);
        return new Rect2i(point.x() - 1, point.y() - 1, 17, 17);
    }
    @Override public Slot trinkets$getFocusedSlot() { return focused; }
    @Override public int trinkets$getX() { return context.contentX(); }
    @Override public int trinkets$getY() { return context.contentY(); }
    @Override public boolean trinkets$isRecipeBookOpen() { return context.recipeBookVisible(); }
    @Override public void trinkets$updateTrinketSlots() { topology = List.of(); }
}
