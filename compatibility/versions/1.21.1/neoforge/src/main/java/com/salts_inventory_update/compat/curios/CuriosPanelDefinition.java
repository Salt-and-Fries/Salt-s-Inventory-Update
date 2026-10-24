package com.salts_inventory_update.compat.curios;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import com.salts_inventory_update.api.client.desktop.*;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import com.salts_inventory_update.api.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/** Curios contents remain a real detached menu; only their presentation is joined to Character. */
public final class CuriosPanelDefinition implements DesktopWindowDefinition<CuriosDesktopMenu, CuriosPanelDefinition.State> {
    public static final class State { int scrollRow; boolean draggingScrollbar; }
    private record Cell(CuriosDesktopMenu.AccessorySlot slot, int x, int y) { }

    @Override public State createState(DesktopWindowSetupContext<CuriosDesktopMenu> c) { return new State(); }
    @Override public DesktopWindowOptions options(DesktopWindowContext<CuriosDesktopMenu, State> c) {
        return new DesktopWindowOptions(false, false, true, false, false);
    }
    @Override public DesktopWindowSize defaultSize(DesktopWindowSetupContext<CuriosDesktopMenu> c) {
        return DesktopWindowSize.of(176, CuriosPanelLayout.preferredHeight(176, c.model().displayedSlots().size()));
    }
    @Override public DesktopWindowSize minSize(DesktopWindowContext<CuriosDesktopMenu, State> c) { return DesktopWindowSize.of(32, 1); }
    @Override public DesktopWindowAttachment attachment(DesktopWindowContext<CuriosDesktopMenu, State> c) {
        int width = DesktopClientApi.currentWorkspace().stream().flatMap(w -> w.windows().stream())
            .filter(w -> w.handle().instanceId().equals(c.model().ownerWindow)).findFirst()
            .map(w -> w.handle().state().width()).orElse(c.windowWidth());
        return new DesktopWindowAttachment(c.model().ownerWindow,
            CuriosPanelLayout.preferredHeight(width, c.model().displayedSlots().size()), c.model().hasCosmetics() ? 23 : 0, CuriosPanelLayout.WINDOW_GAP);
    }
    @Override public DesktopWindowSurface surface(DesktopWindowContext<CuriosDesktopMenu, State> c) {
        List<DesktopWindowRegion> regions = new ArrayList<>();
        regions.add(DesktopWindowRegion.visualAndInteractive(0, 0, c.windowWidth(), c.windowHeight()));
        if (c.model().hasCosmetics()) regions.add(DesktopWindowRegion.visualAndInteractive(c.windowWidth() - 36, -23, 28, 24));
        return DesktopWindowSurface.modOwned(regions);
    }
    @Override public void opened(DesktopWindowContext<CuriosDesktopMenu, State> c) {
        CuriosClientIntegration.opened(c.model().ownerWindow, c.windowHandle());
    }
    @Override public void closed(DesktopWindowContext<CuriosDesktopMenu, State> c) {
        CuriosClientIntegration.closed(c.model().ownerWindow, c.windowHandle());
    }
    private CuriosPanelLayout layout(DesktopWindowContext<CuriosDesktopMenu, State> c) {
        var layout = CuriosPanelLayout.fit(c.windowWidth(), c.windowHeight(), c.model().displayedSlots().size());
        c.state().scrollRow = Math.max(0, Math.min(c.state().scrollRow, layout.maxScroll()));
        return layout;
    }
    private List<Cell> cells(DesktopWindowContext<CuriosDesktopMenu, State> c) {
        var layout = layout(c);
        List<Cell> result = new ArrayList<>();
        var slots = c.model().displayedSlots();
        int first = c.state().scrollRow * layout.columns();
        int end = Math.min(slots.size(), first + layout.visibleRows() * layout.columns());
        for (int i = first; i < end; i++) {
            int local = i - first;
            result.add(new Cell(slots.get(i), c.windowX() + 8 + local % layout.columns() * 18,
                c.windowY() + 8 + local / layout.columns() * 18));
        }
        return result;
    }
    private static boolean contains(double x, double y, int left, int top, int width, int height) {
        return x >= left && y >= top && x < (double) left + width && y < (double) top + height;
    }
    private boolean tab(DesktopWindowContext<CuriosDesktopMenu, State> c, double x, double y) {
        return c.model().hasCosmetics() && contains(x, y, c.windowX() + c.windowWidth() - 36, c.windowY() - 23, 28, 24);
    }
    private @Nullable Cell visibility(DesktopWindowContext<CuriosDesktopMenu, State> c, double x, double y) {
        if (!contains(x, y, c.windowX() + 6, c.windowY() + 6, layout(c).viewportWidth(c.windowWidth()),
            Math.max(1, c.windowHeight() - 12))) return null;
        for (Cell cell : cells(c)) if (cell.slot.canToggleRender() && contains(x, y, cell.x + 12, cell.y - 1, 8, 8)) return cell;
        return null;
    }
    private boolean scrollbar(DesktopWindowContext<CuriosDesktopMenu, State> c, double x, double y) {
        return layout(c).scrolling() && contains(x, y, c.windowX() + c.windowWidth() - 15, c.windowY() + 7, 8, Math.max(1, c.windowHeight() - 14));
    }
    @Override public void render(DesktopRenderContext<CuriosDesktopMenu, State> c) {
        var layout = layout(c);
        c.windowNineSlice(DesktopThemeAssets.WINDOW, c.windowX(), c.windowY(), c.windowWidth(), c.windowHeight());
        if (c.model().hasCosmetics()) {
            int x = c.windowX() + c.windowWidth() - 36, y = c.windowY() - 23;
            c.texture(CuriosClientIntegration.ART, x, y, 32, 0, 28, 24, 28, 24, 256, 256);
            String sprite = c.model().cosmetics ? "cosmetic_on" : "cosmetic_off";
            if (tab(c, c.mouseX(), c.mouseY())) sprite += "_highlighted";
            c.sprite(ResourceLocation.fromNamespaceAndPath("curios", sprite), x + 4, y + 5, 20, 17);
        }
        if (c.model().displayedSlots().isEmpty()) {
            c.text(Component.literal("No accessory slots"), c.windowX() + 8, c.windowY() + 10, 0xff404040, false);
            return;
        }
        var cells = cells(c);
        c.withClip(c.windowX() + 6, c.windowY() + 6, layout.viewportWidth(c.windowWidth()),
            Math.max(1, c.windowHeight() - 12), () -> {
                for (Cell cell : cells) {
                    c.slotBackground(cell.x, cell.y);
                    if (cell.slot.isCosmetic()) {
                        // Native revamp artwork is translucent; legacy GuiGraphics does not enable blending for blits.
                        c.nativeGraphics().flush();
                        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
                        com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
                        c.texture(CuriosClientIntegration.ART, cell.x - 1, cell.y - 1,
                            32, 50, 18, 18, 18, 18, 256, 256);
                        c.nativeGraphics().flush();
                        com.mojang.blaze3d.systems.RenderSystem.disableBlend();
                    }
                    ItemStack display = cell.slot.displayStack();
                    if (ItemStack.matches(display, cell.slot.getItem())) c.texturelessSlot(((net.minecraft.world.inventory.Slot) cell.slot.nativeSlot).index, cell.x, cell.y);
                    else c.item(display, cell.x, cell.y);
                    var icon = cell.slot.nativeSlot.getNoItemIcon();
                    if (display.isEmpty() && !cell.slot.getItem().isEmpty() && icon != null)
                        c.nativeGraphics().blit(cell.x, cell.y, 0, 16, 16,
                            c.minecraft().getTextureAtlas(icon.getFirst()).apply(icon.getSecond()));
                    if (contains(c.mouseX(), c.mouseY(), cell.x - 1, cell.y - 1, 18, 18)
                        && visibility(c, c.mouseX(), c.mouseY()) == null) c.slotHighlight(cell.x, cell.y);
                }
                c.rawGraphics().nextStratum();
                for (Cell cell : cells) if (cell.slot.canToggleRender())
                    c.texture(CuriosClientIntegration.ART, cell.x + 12, cell.y - 1,
                        cell.slot.getRenderStatus() ? 75 : 83, 0, 8, 8, 8, 8, 256, 256);
            });
        if (layout.scrolling()) {
            int x = c.windowX() + c.windowWidth() - 15, y = c.windowY() + 7, height = c.windowHeight() - 14;
            c.fill(x, y, x + 8, y + height, 0xff555555);
            int thumb = Math.max(8, height * layout.visibleRows() / layout.totalRows());
            int top = y + (height - thumb) * c.state().scrollRow / Math.max(1, layout.maxScroll());
            c.fill(x + 1, top, x + 7, top + thumb, 0xffc6c6c6);
            c.fill(x + 1, top, x + 7, top + 1, 0xffffffff);
            c.fill(x + 6, top + 1, x + 7, top + thumb, 0xff777777);
        }
    }
    @Override public @Nullable DesktopSlotHit slotAt(DesktopSlotContext<CuriosDesktopMenu, State> c, double x, double y) {
        if (tab(c, x, y) || scrollbar(c, x, y) || visibility(c, x, y) != null) return null;
        if (!contains(x, y, c.windowX() + 6, c.windowY() + 6, layout(c).viewportWidth(c.windowWidth()), c.windowHeight() - 12)) return null;
        for (Cell cell : cells(c)) {
            var hit = c.menuSlotHit(((net.minecraft.world.inventory.Slot) cell.slot.nativeSlot).index, cell.x, cell.y, x, y);
            if (hit != null) return hit;
        }
        return null;
    }
    @Override public boolean mouseClicked(DesktopInputContext<CuriosDesktopMenu, State> c, MouseButtonEvent event, boolean doubleClick) {
        if (tab(c, event.x(), event.y())) {
            if (event.button() == 0) {
                c.windowHandle().cancelInput();
                boolean next = !c.model().cosmetics;
                if (c.sendPayload(CuriosIntegration.COSMETICS, new byte[]{(byte) (next ? 1 : 0)})) {
                    c.model().cosmetics = next; c.state().scrollRow = 0;
                }
            }
            return true;
        }
        Cell button = visibility(c, event.x(), event.y());
        if (button != null) {
            if (event.button() == 0) c.sendPayload(CuriosIntegration.VISIBILITY, ByteBuffer.allocate(4).putInt(((net.minecraft.world.inventory.Slot) button.slot.nativeSlot).index).array());
            return true;
        }
        if (scrollbar(c, event.x(), event.y())) {
            if (event.button() == 0) { c.state().draggingScrollbar = true; dragScrollbar(c, event.y()); }
            return true;
        }
        return false;
    }
    private void dragScrollbar(DesktopWindowContext<CuriosDesktopMenu, State> c, double y) {
        var layout = layout(c);
        int height = Math.max(1, c.windowHeight() - 14);
        int thumb = Math.max(8, height * layout.visibleRows() / layout.totalRows());
        double fraction = (y - c.windowY() - 7 - thumb / 2.0) / Math.max(1, height - thumb);
        c.state().scrollRow = Math.max(0, Math.min(layout.maxScroll(), (int) Math.round(fraction * layout.maxScroll())));
    }
    @Override public boolean mouseDragged(DesktopInputContext<CuriosDesktopMenu, State> c, MouseButtonEvent event, double dx, double dy) {
        if (!c.state().draggingScrollbar) return false;
        dragScrollbar(c, event.y()); return true;
    }
    @Override public boolean mouseReleased(DesktopInputContext<CuriosDesktopMenu, State> c, MouseButtonEvent event) {
        boolean handled = c.state().draggingScrollbar; c.state().draggingScrollbar = false; return handled;
    }
    @Override public boolean mouseScrolled(DesktopInputContext<CuriosDesktopMenu, State> c, double x, double y, double dx, double dy) {
        var layout = layout(c);
        if (!layout.scrolling() || dy == 0 || !contains(x, y, c.windowX(), c.windowY(), c.windowWidth(), c.windowHeight())) return false;
        c.windowHandle().cancelInput();
        c.state().scrollRow = Math.max(0, Math.min(layout.maxScroll(), c.state().scrollRow + (dy < 0 ? 1 : -1)));
        return true;
    }
    @Override public List<DesktopScrollRegion> controllerScrollRegions(DesktopWindowContext<CuriosDesktopMenu, State> c) {
        var layout = layout(c);
        if (!layout.scrolling()) return List.of();
        return List.of(new DesktopScrollRegion(c.windowX() + 7, c.windowY() + 7, Math.max(1, c.windowWidth() - 25),
            Math.max(1, layout.visibleRows() * 18), 18, c.state().scrollRow > 0, c.state().scrollRow < layout.maxScroll()));
    }
    @Override public boolean appendTooltip(DesktopRenderContext<CuriosDesktopMenu, State> c, int x, int y) {
        if (tab(c, x, y)) { c.tooltip(Component.translatable("gui.curios.toggle.cosmetics"), x, y); return true; }
        if (visibility(c, x, y) != null) { c.tooltip(Component.translatable("gui.curios.toggle"), x, y); return true; }
        for (Cell cell : cells(c)) if (contains(x, y, cell.x - 1, cell.y - 1, 18, 18)) {
            ItemStack display = cell.slot.displayStack();
            if (display.isEmpty()) c.tooltip(cell.slot.getSlotTooltip(), x, y);
            else {
                var lines = new ArrayList<>(Screen.getTooltipFromItem(c.minecraft(), display));
                if (!cell.slot.isActiveState()) { lines.add(Component.empty()); lines.add(Component.translatable("curios.tooltip.inactive").withStyle(ChatFormatting.RED)); }
                c.nativeGraphics().renderTooltip(c.minecraft().font, lines, display.getTooltipImage(), x, y);
            }
            return true;
        }
        return false;
    }
}
