package com.salts_inventory_update.controlify.model;

/** The recipe-wheel filter; partial ownership does not require a complete recipe. */
public enum RecipeFilter {
    ALL("all"), CONTAINS_PARTS("contains_parts"), CRAFTABLE("craftable");

    private final String key;
    RecipeFilter(String key) { this.key = key; }
    public String key() { return key; }
    public RecipeFilter next() {
        return switch (this) { case ALL -> CONTAINS_PARTS; case CONTAINS_PARTS -> CRAFTABLE; case CRAFTABLE -> ALL; };
    }
    public boolean includes(boolean craftable, boolean hasIngredient) {
        return switch (this) { case ALL -> true; case CONTAINS_PARTS -> hasIngredient; case CRAFTABLE -> craftable; };
    }
    public static RecipeFilter parse(String key) {
        for (RecipeFilter filter : values()) if (filter.key.equalsIgnoreCase(key == null ? "" : key.trim())) return filter;
        return ALL;
    }
}
