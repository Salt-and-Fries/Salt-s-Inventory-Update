package com.salts_inventory_update.api.client.desktop;

import java.util.Objects;

/** An embedded native slot; menu ownership is resolved and checked by Core. */
public record DesktopSessionSlotHit(int sessionId, DesktopSlotHit hit) {
    public DesktopSessionSlotHit {
        if (sessionId <= 0) throw new IllegalArgumentException("Embedded session must be server-owned");
        Objects.requireNonNull(hit);
        if (hit.menuSlotId() < 0) throw new IllegalArgumentException("Embedded hit must identify a real slot");
    }
}
