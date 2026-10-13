package com.salts_inventory_update.compat.ae2.nativeview;

import com.google.gson.JsonObject;

/** Align only unchanged AE2 15 defaults in an independent desktop style document. */
public final class LegacyDesktopStyle {
    private LegacyDesktopStyle() { }

    public static void prepare(JsonObject document) {
        if (!document.has("desktop") || !document.has("widgets")) return;
        var widgets = document.getAsJsonObject("widgets");
        var desktop = document.getAsJsonObject("desktop");
        if (desktop.has("inventoryCut")) {
            for (int index = 0; index < 4; index++) {
                String key = "modeTabButton" + index;
                if (!widgets.has(key)) continue;
                var tab = widgets.getAsJsonObject(key);
                if (isValue(tab, "left", 173) && isValue(tab, "bottom", 170 - index * 20)
                        && isValue(tab, "width", 20) && isValue(tab, "height", index == 3 ? 22 : 20)) {
                    // Native horizontal-tab art is 22 pixels square. A 21-pixel
                    // pitch shares the joining bevel without clipping the icon.
                    tab.addProperty("width", 22);
                    tab.addProperty("height", 22);
                    tab.addProperty("bottom", desktop.get("inventoryCut").getAsInt() + 22 + (3 - index) * 21);
                }
            }
        }
        if (!widgets.has("openPriority")) return;
        var priority = widgets.getAsJsonObject("openPriority");
        boolean nativeSize = (!priority.has("width") && !priority.has("height"))
                || (isValue(priority, "width", 16) && isValue(priority, "height", 16));
        if (!isValue(priority, "left", 154) || !isValue(priority, "top", 0) || !nativeSize) return;
        // These are the reference's priority-tab bounds. Native screen documents
        // and resource-pack overrides retain their original values.
        priority.addProperty("left", 152);
        priority.addProperty("top", -5);
        priority.addProperty("width", 20);
        priority.addProperty("height", 20);
    }

    private static boolean isValue(JsonObject object, String name, int value) {
        return object.has(name) && object.get(name).getAsInt() == value;
    }
}
