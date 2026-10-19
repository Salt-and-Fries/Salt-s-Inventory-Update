package com.salts_inventory_update.controlify.model;

import java.util.ArrayList;
import java.util.List;

/** Selects source pixels, not rotated artwork: every rectangle retains the original texture UVs. */
public final class TextureSectorMask {
    public static final double HALF_ANGLE = Math.PI / 30;
    public record Strip(int x, int y, int width, int height) { }
    private TextureSectorMask() { }

    public static List<Strip> create(int width, int height, double angle, double halfAngle) {
        if (width <= 0 || height <= 0 || !Double.isFinite(angle) || !Double.isFinite(halfAngle)
                || halfAngle <= 0 || halfAngle > Math.PI) throw new IllegalArgumentException("Invalid texture sector");
        if (halfAngle == Math.PI) return List.of(new Strip(0, 0, width, height));
        double forwardX = Math.sin(angle), forwardY = -Math.cos(angle), cutoff = Math.cos(halfAngle);
        var strips = new ArrayList<Strip>();
        for (int y = 0; y < height; y++) {
            int first = -1;
            double dy = y + .5 - height / 2.0;
            for (int x = 0; x < width; x++) {
                double dx = x + .5 - width / 2.0;
                double length = Math.hypot(dx, dy);
                if (dx * forwardX + dy * forwardY >= length * cutoff) {
                    if (first < 0) first = x;
                } else if (first >= 0) {
                    append(strips, first, y, x - first);
                    first = -1;
                }
            }
            if (first >= 0) append(strips, first, y, width - first);
        }
        return List.copyOf(strips);
    }
    private static void append(List<Strip> strips, int first, int y, int span) {
        // A wide sector can contain two disjoint row spans. They must never be bridged.
        for (int i = strips.size() - 1; i >= 0; i--) {
            var previous = strips.get(i);
            if (previous.y + previous.height < y) break;
            if (previous.x == first && previous.width == span && previous.y + previous.height == y) {
                strips.set(i, new Strip(first, previous.y, span, previous.height + 1));
                return;
            }
        }
        strips.add(new Strip(first, y, span, 1));
    }
}
