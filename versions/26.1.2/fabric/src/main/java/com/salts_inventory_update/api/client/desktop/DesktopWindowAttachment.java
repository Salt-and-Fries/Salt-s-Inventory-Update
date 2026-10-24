package com.salts_inventory_update.api.client.desktop;

import java.util.Objects;
import java.util.UUID;

/**
 * Joins a real menu window above another window. The child matches the owner's width;
 * Core fits their combined bounds, shrinks only the child and disposes it with its owner.
 * preferredHeight is the complete child body; topProtrusion reserves external tabs.
 * The definition must adapt its contents to the committed height (for example by scrolling).
 * One direct child is supported per owner. Nested/cyclic attachments are rejected.
 */
public record DesktopWindowAttachment(UUID owner, int preferredHeight, int topProtrusion, int gap) {
    public DesktopWindowAttachment {
        Objects.requireNonNull(owner);
        if (preferredHeight < 1 || topProtrusion < 0 || gap < 0)
            throw new IllegalArgumentException("Invalid attached-window geometry");
    }
}
