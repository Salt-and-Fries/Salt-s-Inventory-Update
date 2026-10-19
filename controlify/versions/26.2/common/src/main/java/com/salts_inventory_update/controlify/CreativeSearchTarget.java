package com.salts_inventory_update.controlify;

import com.mojang.blaze3d.platform.InputConstants;
import com.salts_inventory_update.api.client.desktop.DesktopContextualMenu;
import com.salts_inventory_update.api.client.desktop.DesktopWindowBounds;
import com.salts_inventory_update.api.client.desktop.DesktopWorkspace;
import com.salts_inventory_update.api.client.desktop.DesktopWorkspaceWindow;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.CreativeModeTabs;

/** Native search geometry obtained solely through Salt's public workspace API. */
public record CreativeSearchTarget(UUID window, DesktopWindowBounds field) {
    public CreativeSearchTarget {
        Objects.requireNonNull(window, "window");
        Objects.requireNonNull(field, "field");
        if (field.height() != 12 || field.width() < 24)
            throw new IllegalArgumentException("Not a native creative search field");
    }

    /** Returns null unless the selected native Search tab exposes exactly one search-sized control. */
    public static CreativeSearchTarget find(DesktopWorkspace desktop, DesktopWorkspaceWindow window) {
        if (desktop == null || window == null || window.handle() == null
                || !desktop.isValid() || !window.handle().isOpen()) return null;
        UUID id = window.handle().instanceId();
        // This check runs for guide/hover updates too. Asking for a crafting/furnace
        // snapshot here would enumerate the entire recipe book every rendered frame.
        var hint = desktop.contextualBookHint(id);
        if (hint == null || hint.kind() != DesktopContextualMenu.Kind.CREATIVE) return null;
        var menu = desktop.contextualMenu(id);
        if (menu == null || menu.kind() != DesktopContextualMenu.Kind.CREATIVE) return null;
        var searchId = BuiltInRegistries.CREATIVE_MODE_TAB.getKey(CreativeModeTabs.searchTab());
        if (searchId == null || menu.tabs().stream().noneMatch(tab -> tab.selected() && searchId.equals(tab.id()))) return null;

        // Native public controls provide the actual field rectangle: 12px high,
        // at least 24px wide. Page buttons are only 11px wide; inventory expanders
        // do not belong to the Search tab. No private title/window offsets are copied.
        DesktopWindowBounds field = null;
        for (var control : desktop.visibleControls(id)) {
            if (control.height() != 12 || control.width() < 24) continue;
            if (field != null) return null; // New/ambiguous controls must never receive a guessed click.
            field = control;
        }
        return field == null ? null : new CreativeSearchTarget(id, field);
    }

    /** Coordinates are native Core pixels, before the companion's presentation transform. */
    public boolean contains(double nativeX, double nativeY) {
        return Double.isFinite(nativeX) && Double.isFinite(nativeY)
            && nativeX >= field.x() && nativeX < field.x() + field.width()
            && nativeY >= field.y() && nativeY < field.y() + field.height();
    }

    /** Refreshes eligibility and geometry after resizing, tab changes, or screen transitions. */
    public CreativeSearchTarget current(DesktopWorkspace desktop) {
        if (desktop == null || !desktop.isValid()) return null;
        for (var candidate : desktop.windows()) {
            if (candidate.handle() != null && window.equals(candidate.handle().instanceId())) return find(desktop, candidate);
        }
        return null;
    }

    /** Focuses the real field without changing its query, moving the virtual mouse, or selecting a tab. */
    public boolean focus(DesktopWorkspace desktop) {
        var current = current(desktop);
        if (current == null) return false;
        double x = current.field.x() + current.field.width() / 2.0;
        double y = current.field.y() + current.field.height() / 2.0;
        try {
            return desktop.pointer(window, DesktopWorkspace.PointerAction.PRESS, x, y,
                org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT, 0, 0);
        } finally {
            desktop.pointer(window, DesktopWorkspace.PointerAction.RELEASE, x, y,
                org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT, 0, 0);
        }
    }
}
