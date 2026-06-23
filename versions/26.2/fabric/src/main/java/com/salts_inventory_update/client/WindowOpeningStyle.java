package com.salts_inventory_update.client;

public enum WindowOpeningStyle {
    TOP_OUTSIDE,
    AROUND_INVENTORY,
    VINTAGE_STORY;

    public WindowOpeningStyle next() {
        WindowOpeningStyle[] values = values();
        return values[(this.ordinal() + 1) % values.length];
    }

    public static WindowOpeningStyle parse(String value) {
        if (value != null) {
            String normalized = value.trim().replace(' ', '_').replace('-', '_');
            for (WindowOpeningStyle style : values()) {
                if (style.name().equalsIgnoreCase(normalized)) {
                    return style;
                }
            }
        }
        return TOP_OUTSIDE;
    }
}
