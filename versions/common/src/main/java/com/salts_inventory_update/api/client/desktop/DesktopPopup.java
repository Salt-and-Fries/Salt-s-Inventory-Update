package com.salts_inventory_update.api.client.desktop;

import java.util.List;
import java.util.Objects;

/** Owner-relative popup geometry. Painting and model state belong to the integration. */
public record DesktopPopup(String id, List<DesktopWindowRegion> regions, boolean modal,
                           boolean dismissOnOutside, boolean dismissOnEscape, boolean dismissOnFocusLoss) {
    public enum Dismissal { OUTSIDE_PRESS, ESCAPE, FOCUS_LOST }
    public DesktopPopup {
        Objects.requireNonNull(id);
        if (id.isBlank()) throw new IllegalArgumentException("Popup id is blank");
        regions = regions.stream().map(region -> region.withRole(DesktopHitRole.POPUP).transientRegion()).toList();
    }
    public boolean contains(int ownerX, int ownerY, double mouseX, double mouseY) {
        return regions.stream().anyMatch(region -> region.contains(ownerX, ownerY, mouseX, mouseY));
    }
}
