package com.salts_inventory_update.compat.equipment;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Predicate;
import com.salts_inventory_update.api.client.desktop.DesktopWindowHandle;

/** One selected equipment view per Character, including requests still in flight. */
public final class EquipmentPanelSelection<P> {
    public enum Kind { CURIOS, ACCESSORIES }
    public static final EquipmentPanelSelection<DesktopWindowHandle> DESKTOP =
        new EquipmentPanelSelection<>(DesktopWindowHandle::isOpen, DesktopWindowHandle::close);
    private static final long RETRY_NANOS = 2_000_000_000L;
    private final Map<UUID, Selection<P>> selections = new HashMap<>();
    private final Predicate<P> isOpen;
    private final Consumer<P> close;

    public EquipmentPanelSelection(Predicate<P> isOpen, Consumer<P> close) {
        this.isOpen = isOpen;
        this.close = close;
    }

    /** Returns true only when the caller should send a new menu-open request. */
    public boolean request(UUID owner, Kind kind, long now) {
        Selection<P> previous = selections.get(owner);
        if (previous != null && previous.kind == kind) {
            if (previous.panel != null && isOpen.test(previous.panel)) {
                close(owner, kind);
                return false;
            }
            if (previous.panel == null && now - previous.requestedAt < RETRY_NANOS) return false;
        }
        // Select first: closing the old window can synchronously call closed().
        selections.put(owner, new Selection<>(kind, now));
        if (previous != null && previous.panel != null && isOpen.test(previous.panel)) close.accept(previous.panel);
        return true;
    }

    /** A delayed response must never replace the view selected more recently. */
    public boolean accept(UUID owner, Kind kind, P panel) {
        Selection<P> selection = selections.get(owner);
        if (selection == null || selection.kind != kind) return false;
        P previous = selection.panel;
        selection.panel = panel;
        if (previous != null && previous != panel && isOpen.test(previous)) close.accept(previous);
        return true;
    }

    public void closed(UUID owner, Kind kind, P panel) {
        Selection<P> selection = selections.get(owner);
        if (selection != null && selection.kind == kind && selection.panel == panel) selections.remove(owner);
    }

    public void close(UUID owner, Kind kind) {
        Selection<P> selection = selections.get(owner);
        if (selection == null || selection.kind != kind) return;
        selections.remove(owner);
        if (selection.panel != null && isOpen.test(selection.panel)) close.accept(selection.panel);
    }

    public void clear() { selections.clear(); }

    private static final class Selection<P> {
        final Kind kind;
        final long requestedAt;
        P panel;
        Selection(Kind kind, long requestedAt) { this.kind = kind; this.requestedAt = requestedAt; }
    }
}
