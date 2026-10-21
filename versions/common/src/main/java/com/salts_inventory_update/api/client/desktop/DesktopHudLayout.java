package com.salts_inventory_update.api.client.desktop;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.function.UnaryOperator;

/**
 * Immutable detached HUD geometry: all nine hotbar slots and one offhand slot, each exactly once.
 * List order is paint order; pointer lookup traverses it in reverse. Hide slots instead of removing them.
 * Window-hosted hotbars and character slots do not use this layout.
 */
public record DesktopHudLayout(List<DesktopHudSlot> slots) {
    public DesktopHudLayout {
        slots = List.copyOf(slots);
        var keys = new HashSet<String>();
        for (DesktopHudSlot slot : slots) {
            if (!keys.add(slot.kind() + ":" + slot.index())) throw new IllegalArgumentException("Duplicate HUD slot");
        }
        if (slots.size() != 10) throw new IllegalArgumentException("A HUD layout must contain nine hotbar slots and offhand");
    }

    /** The existing vanilla-spaced item origins; offhandGap is the gap between 18-unit slot targets. */
    public static DesktopHudLayout standard(int width, int height, int offhandGap) {
        if (width < 0 || height < 0 || offhandGap < 0) throw new IllegalArgumentException("Invalid HUD dimensions");
        var slots = new ArrayList<DesktopHudSlot>();
        int firstX = width / 2 - 88, y = height - 19;
        slots.add(new DesktopHudSlot(DesktopHudSlot.Kind.OFFHAND, 0,
            new DesktopTransform(firstX - 18.0 - offhandGap, y, 1), true, true));
        for (int index = 0; index < 9; index++) slots.add(new DesktopHudSlot(DesktopHudSlot.Kind.HOTBAR, index,
            new DesktopTransform(firstX + index * 20, y, 1), true, true));
        return new DesktopHudLayout(slots);
    }

    public DesktopHudLayout mapSlots(UnaryOperator<DesktopHudSlot> mapper) {
        Objects.requireNonNull(mapper, "mapper");
        return new DesktopHudLayout(slots.stream().map(mapper).toList());
    }

    /** Transforms every slot around the screen origin. Use mapSlots for independent placement/scale. */
    public DesktopHudLayout transformed(DesktopTransform transform) {
        Objects.requireNonNull(transform, "transform");
        return mapSlots(slot -> slot.transformed(transform));
    }

    public DesktopHudLayout translated(double x, double y) { return transformed(new DesktopTransform(x, y, 1)); }
}
