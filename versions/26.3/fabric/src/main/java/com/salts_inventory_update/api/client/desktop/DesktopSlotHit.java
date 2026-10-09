package com.salts_inventory_update.api.client.desktop;

public record DesktopSlotHit(int menuSlotId, int x, int y) {
    public DesktopSlotHit {
        if (menuSlotId < -1) {
            throw new IllegalArgumentException("menuSlotId must be a valid menu slot or SLOT_CLICKED_OUTSIDE");
        }
    }

    public static DesktopSlotHit of(int menuSlotId, int x, int y) {
        return new DesktopSlotHit(menuSlotId, x, y);
    }
}
