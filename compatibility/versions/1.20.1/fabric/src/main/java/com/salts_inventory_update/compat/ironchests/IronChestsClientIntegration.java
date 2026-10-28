package com.salts_inventory_update.compat.ironchests;

import com.salts_inventory_update.api.client.desktop.*;
import com.salts_inventory_update.api.client.desktop.widget.DesktopWidgets;
import com.salts_inventory_update.api.client.input.MouseButtonEvent;
import com.salts_inventory_update.api.integration.DesktopEnvironment;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;

/** A storage view using only Salt's supported window, slot, input and theme APIs. */
public final class IronChestsClientIntegration {
    private static boolean initialized;
    private static final java.util.Set<MenuType<?>> registered = new java.util.HashSet<>();
    private IronChestsClientIntegration() { }

    public static synchronized void initialize() {
        if (initialized || !DesktopEnvironment.isModLoaded("ironchests")) return;
        initialized = true;
        register();
        DesktopClientApi.onClientStarted(client -> register());
    }

    private static void register() {
        var pending = new java.util.ArrayList<MenuType<?>>();
        DesktopEnvironment.registerIntegration("ironchests", () -> IronChestsIntegration.forEachMenu((type, shape) -> {
            IronChestsIntegration.registerStorage(type, shape);
            if (registered.contains(type)) return;
            DesktopClientApi.registerWindow(type, new StorageWindow(shape));
            pending.add(type);
        }));
        registered.addAll(pending);
    }

    static final class State { int row; boolean dragging; }

    private record StorageWindow(IronChestsIntegration.Shape shape) implements DesktopWindowDefinition<AbstractContainerMenu, State> {
        @Override public State createState(DesktopWindowSetupContext<AbstractContainerMenu> context) { return new State(); }
        @Override public String layoutRevision(DesktopWindowContext<AbstractContainerMenu, State> context) { return "ironchests-grid-1"; }
        @Override public DesktopWindowSize defaultSize(DesktopWindowSetupContext<AbstractContainerMenu> context) {
            return DesktopWindowSize.of(Math.max(178, shape.columns() * 18 + 16), shape.rows() * 18 + 32);
        }
        @Override public DesktopWindowSize minSize(DesktopWindowContext<AbstractContainerMenu, State> context) { return DesktopWindowSize.of(98, 50); }
        @Override public DesktopResizePolicy resizePolicy(DesktopWindowContext<AbstractContainerMenu, State> context) { return DesktopResizePolicy.FREE; }
        @Override public void loadLocalState(DesktopWindowContext<AbstractContainerMenu, State> context, CompoundTag tag) { context.state().row = Math.max(0, tag.getInt("scrollRow")); }
        @Override public void saveLocalState(DesktopWindowContext<AbstractContainerMenu, State> context, CompoundTag tag) { tag.putInt("scrollRow", layout(context).clamp(context.state().row)); }

        private IronChestGrid.Layout layout(DesktopWindowContext<AbstractContainerMenu, State> c) {
            return IronChestGrid.layout(c.containerSlots().size(), c.contentWidth(), c.contentHeight());
        }
        @Override public void resized(DesktopWindowContext<AbstractContainerMenu, State> context) { context.state().row = layout(context).clamp(context.state().row); }
        @Override public void render(DesktopRenderContext<AbstractContainerMenu, State> c) {
            var grid = layout(c);
            int row = grid.clamp(c.state().row), first = grid.firstSlot(row);
            for (int i = first; i < grid.endSlot(row, c.containerSlots().size()); i++) {
                c.slot(c.containerSlot(i), c.contentX() + grid.gridX() + (i - first) % grid.columns() * 18 + 1,
                    c.contentY() + (i - first) / grid.columns() * 18 + 1);
            }
            if (grid.maxScroll() > 0) DesktopWidgets.renderScrollbar(c, c.contentX() + grid.scrollX(), c.contentY(), grid.height(), row, grid.maxScroll());
        }
        @Override public DesktopSlotHit slotAt(DesktopSlotContext<AbstractContainerMenu, State> c, double x, double y) {
            var grid = layout(c);
            int first = grid.firstSlot(c.state().row);
            for (int i = first; i < grid.endSlot(c.state().row, c.containerSlots().size()); i++) {
                var hit = c.containerSlotHit(i, c.contentX() + grid.gridX() + (i - first) % grid.columns() * 18 + 1,
                    c.contentY() + (i - first) / grid.columns() * 18 + 1, x, y);
                if (hit != null) return hit;
            }
            return null;
        }
        @Override public List<DesktopScrollRegion> controllerScrollRegions(DesktopWindowContext<AbstractContainerMenu, State> c) {
            var grid = layout(c); int row = grid.clamp(c.state().row);
            return grid.maxScroll() == 0 ? List.of() : List.of(new DesktopScrollRegion(c.contentX(), c.contentY(), c.contentWidth(), grid.height(), 18, row > 0, row < grid.maxScroll()));
        }
        @Override public boolean mouseScrolled(DesktopInputContext<AbstractContainerMenu, State> c, double x, double y, double sx, double sy) {
            var grid = layout(c);
            if (grid.maxScroll() == 0 || sy == 0 || !DesktopWidgets.contains(x, y, c.contentX(), c.contentY(), c.contentWidth(), c.contentHeight())) return false;
            c.state().row = DesktopWidgets.scrollByWheel(c.state().row, grid.maxScroll(), sy);
            return true;
        }
        @Override public boolean mouseClicked(DesktopInputContext<AbstractContainerMenu, State> c, MouseButtonEvent e, boolean twice) {
            var grid = layout(c);
            if (e.button() != 0 || grid.maxScroll() == 0 || !DesktopWidgets.contains(e.x(), e.y(), c.contentX() + grid.scrollX(), c.contentY(), 14, grid.height())) return false;
            c.state().dragging = true;
            scrollTo(c, e.y());
            return true;
        }
        @Override public boolean mouseDragged(DesktopInputContext<AbstractContainerMenu, State> c, MouseButtonEvent e, double dx, double dy) {
            if (!c.state().dragging || e.button() != 0) return false;
            scrollTo(c, e.y()); return true;
        }
        @Override public boolean mouseReleased(DesktopInputContext<AbstractContainerMenu, State> c, MouseButtonEvent e) {
            if (!c.state().dragging || e.button() != 0) return false;
            c.state().dragging = false; return true;
        }
        private void scrollTo(DesktopInputContext<AbstractContainerMenu, State> c, double y) {
            var grid = layout(c);
            double track = Math.max(1, grid.height() - 2 - DesktopWidgets.SCROLLBAR_THUMB_HEIGHT);
            c.state().row = grid.clamp((int)Math.round((y - c.contentY() - 1 - DesktopWidgets.SCROLLBAR_THUMB_HEIGHT / 2.0) / track * grid.maxScroll()));
        }
    }
}
