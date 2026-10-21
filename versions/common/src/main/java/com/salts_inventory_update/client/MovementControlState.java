package com.salts_inventory_update.client;

/** A toggle belongs to the current window workspace, never to a menu or a world. */
public final class MovementControlState {
    private boolean toggled;
    private boolean toggleMode;

    public void press(boolean toggleMode) {
        this.selectMode(toggleMode);
        if (toggleMode) this.toggled = !this.toggled;
    }

    public boolean active(boolean held, boolean toggleMode, boolean eligible) {
        this.selectMode(toggleMode);
        if (!eligible) {
            this.reset();
            return false;
        }
        return toggleMode ? this.toggled : held;
    }

    public void reset() {
        this.toggled = false;
    }

    private void selectMode(boolean toggleMode) {
        if (this.toggleMode != toggleMode) this.reset();
        this.toggleMode = toggleMode;
    }
}
