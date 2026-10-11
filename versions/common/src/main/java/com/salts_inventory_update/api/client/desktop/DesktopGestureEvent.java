package com.salts_inventory_update.api.client.desktop;

import java.util.Objects;

/** Gesture lifecycle, separate from whether its eventual server mutation succeeds. */
public record DesktopGestureEvent(long id, Outcome outcome, String reason) {
    public enum Outcome { STARTED, UPDATED, COMPLETED, CANCELLED }
    public DesktopGestureEvent { Objects.requireNonNull(outcome); Objects.requireNonNull(reason); }
}
