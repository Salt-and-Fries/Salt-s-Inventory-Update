package com.salts_inventory_update.controlify.input;

/** The controller adapter's small, loader-independent boundary to the workspace. */
public interface ControllerWorkspaceActions {
    boolean workspaceActive();

    boolean radialActive();

    boolean tuckedAway();

    void enterRadial();

    /** Direction uses screen coordinates: positive X is right; positive Y is down. */
    void radialDirection(float x, float y);

    void releaseRadial();

    /** Positive steps run clockwise; negative steps run counterclockwise. */
    void cycleSelected(int steps);

    void swapFocused();

    void closeHighlighted();

    void closeWorkspace();

    /** Restores a tucked workspace, or opens the player workspace for an ordinary inventory press. */
    void restoreWorkspace();

    /** The value of Salt's existing inventory-key hold-to-close duration. */
    long closeHoldDurationMillis();
}
