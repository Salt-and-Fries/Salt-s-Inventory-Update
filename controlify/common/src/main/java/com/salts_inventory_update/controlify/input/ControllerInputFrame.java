package com.salts_inventory_update.controlify.input;

/** One deadzone-adjusted Controlify input sample. */
public record ControllerInputFrame(
        boolean leftTrigger,
        float leftStickX,
        float leftStickY,
        boolean leftBumperPressed,
        boolean rightBumperPressed,
        boolean southPressed,
        boolean closeDown,
        long nowMillis
) {
}
