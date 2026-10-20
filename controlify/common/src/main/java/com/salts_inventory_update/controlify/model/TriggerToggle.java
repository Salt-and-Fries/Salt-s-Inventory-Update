package com.salts_inventory_update.controlify.model;

/** A trigger press toggles a wheel; opening/world-use and modal inputs require a fresh release. */
public final class TriggerToggle {
    private boolean down;
    private boolean active;
    private boolean needsRelease;

    public boolean update(boolean pressed, boolean toggle, boolean allowed) {
        if (!allowed) {
            active = false;
            needsRelease |= pressed;
        }
        if (!pressed) needsRelease = false;
        if (allowed && !needsRelease) {
            if (toggle) {
                if (pressed && !down) active = !active;
            } else active = pressed;
        }
        down = pressed;
        return allowed && active;
    }

    public void reset() { down = active = needsRelease = false; }
    public void cancel(boolean pressed) { active = false; down = pressed; needsRelease = pressed; }
}
