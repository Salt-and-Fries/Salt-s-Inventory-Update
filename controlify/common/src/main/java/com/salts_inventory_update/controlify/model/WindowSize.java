package com.salts_inventory_update.controlify.model;

/** Unscaled GUI dimensions supplied by the existing core window. */
public record WindowSize(double width, double height) {
    public WindowSize {
        if (!Double.isFinite(width) || !Double.isFinite(height) || width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Window dimensions must be finite and positive");
        }
    }
}
