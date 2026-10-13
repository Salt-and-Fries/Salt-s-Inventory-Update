package com.salts_inventory_update.compat.ae2.nativeview;

import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Small desktop-only artwork fallback; installed AE2 textures remain untouched. */
public final class LegacyCpuArtwork {
    @FunctionalInterface public interface Brush {
        void fill(int x0, int y0, int x1, int y1, int argb);
    }
    private record Rectangle(int x, int y, int width, int height, int argb) { }
    private record Region(int width, int height, List<Rectangle> rectangles) { }
    private static final Map<String, Region> REGIONS = load();
    private LegacyCpuArtwork() { }

    private static Map<String, Region> load() {
        var stream = LegacyCpuArtwork.class.getResourceAsStream("/ae2-legacy-cpu-art.json");
        if (stream == null) throw new IllegalStateException("Missing desktop CPU artwork");
        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            var regions = JsonParser.parseReader(reader).getAsJsonObject().getAsJsonObject("regions");
            Map<String, Region> result = new HashMap<>();
            for (var entry : regions.entrySet()) {
                var value = entry.getValue().getAsJsonObject();
                var rectangles = new java.util.ArrayList<Rectangle>();
                for (var item : value.getAsJsonArray("rectangles")) {
                    var a = item.getAsJsonArray();
                    rectangles.add(new Rectangle(a.get(0).getAsInt(), a.get(1).getAsInt(),
                            a.get(2).getAsInt(), a.get(3).getAsInt(), a.get(4).getAsInt()));
                }
                result.put(entry.getKey(), new Region(value.get("width").getAsInt(),
                        value.get("height").getAsInt(), List.copyOf(rectangles)));
            }
            return Map.copyOf(result);
        } catch (java.io.IOException failure) {
            throw new IllegalStateException("Cannot read desktop CPU artwork", failure);
        }
    }

    public static void paint(String name, Brush graphics, int x, int y) {
        var region = region(name);
        slice(region, graphics, 0, region.width, x, y);
    }

    /** The same three-pixel cap and repeated center used by the fork's narrow selector. */
    public static void button(Brush graphics, int x, int y, int width, boolean selected) {
        if (width < 6) return;
        var region = region(selected ? "rowSelected" : "row");
        slice(region, graphics, 0, 3, x, y);
        for (int dx = 3; dx < width - 3;) {
            int part = Math.min(region.width - 6, width - 3 - dx);
            slice(region, graphics, 3, part, x + dx, y);
            dx += part;
        }
        slice(region, graphics, region.width - 3, 3, x + width - 3, y);
    }

    private static Region region(String name) {
        var result = REGIONS.get(name);
        if (result == null) throw new IllegalArgumentException("Unknown desktop CPU artwork: " + name);
        return result;
    }
    private static void slice(Region region, Brush graphics, int sourceX, int width, int x, int y) {
        for (var r : region.rectangles) {
            int left = Math.max(sourceX, r.x), right = Math.min(sourceX + width, r.x + r.width);
            if (right > left) graphics.fill(x + left - sourceX, y + r.y,
                    x + right - sourceX, y + r.y + r.height, r.argb);
        }
    }
}
