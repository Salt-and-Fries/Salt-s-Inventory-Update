package com.salts_inventory_update.controlify.model;

/** Retains a stick's mapped physical angle while respecting its configured deadzone magnitude. */
public record RadialStickDirection(float x, float y) {
    public static RadialStickDirection from(float rawX, float rawY, float processedX, float processedY) {
        double rawMagnitude = Math.hypot(rawX, rawY);
        double magnitude = Math.hypot(processedX, processedY);
        if (!Double.isFinite(rawMagnitude) || !Double.isFinite(magnitude) || rawMagnitude == 0 || magnitude == 0)
            return new RadialStickDirection(0, 0);
        double factor = magnitude / rawMagnitude;
        return new RadialStickDirection((float) (rawX * factor), (float) (rawY * factor));
    }
}
