package com.salts_inventory_update.controlify.model;

import java.util.List;

/** Actual selection must survive any opaque hover artwork supplied by a resource pack. */
public final class CreativeTabLayers {
    public enum Layer { BASE, HOVER, ACTIVE }
    private CreativeTabLayers() { }
    public static List<Layer> ordered(boolean hovered, boolean active) {
        if (active && hovered) return List.of(Layer.BASE, Layer.HOVER, Layer.ACTIVE);
        if (active) return List.of(Layer.BASE, Layer.ACTIVE);
        if (hovered) return List.of(Layer.BASE, Layer.HOVER);
        return List.of(Layer.BASE);
    }
}
