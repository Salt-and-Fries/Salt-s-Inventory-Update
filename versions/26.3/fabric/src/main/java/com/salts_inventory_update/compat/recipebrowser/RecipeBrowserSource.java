package com.salts_inventory_update.compat.recipebrowser;

public enum RecipeBrowserSource {
    JEI("JEI", 10),
    REI("REI", 20),
    EMI("EMI", 30);

    private final String displayName;
    private final int priority;

    RecipeBrowserSource(String displayName, int priority) {
        this.displayName = displayName;
        this.priority = priority;
    }

    public String displayName() {
        return this.displayName;
    }

    int priority() {
        return this.priority;
    }
}
