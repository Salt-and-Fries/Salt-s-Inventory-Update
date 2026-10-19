package com.salts_inventory_update.controlify.input;

import java.util.Objects;

/** Adds the configured inventory-button sort gesture to the 26.3 input route. */
public final class RadialSortInputRouter {
    public static final long DOUBLE_TAP_MILLIS = 350L;

    private final RadialWorkspaceActions actions;
    private final WorkspaceInputRouter workspace;
    private long lastSortTapMillis = Long.MIN_VALUE;
    private boolean inventoryOwned;
    private boolean closePreviouslyDown;
    private boolean sideCloseOwned;
    private boolean triggerNeedsRelease;
    private boolean openingTriggerSampleProvided;

    public RadialSortInputRouter(RadialWorkspaceActions actions) {
        this.actions = Objects.requireNonNull(actions, "actions");
        this.workspace = new WorkspaceInputRouter(actions);
    }

    /** Input levels come from the player's configured bindings, including their rebound buttons. */
    public boolean update(ControllerInputFrame frame, boolean inventoryDown, boolean inventoryPreviouslyDown) {
        boolean closeOwnsButtons = frame.closeDown() || closePreviouslyDown;
        boolean closePressed = frame.closeDown() && !closePreviouslyDown;
        closePreviouslyDown = frame.closeDown();
        if (!openingTriggerSampleProvided && !frame.leftTrigger()) triggerNeedsRelease = false;
        openingTriggerSampleProvided = false;
        if (triggerNeedsRelease) frame = new ControllerInputFrame(false, frame.leftStickX(), frame.leftStickY(),
                frame.leftBumperPressed(), frame.rightBumperPressed(), frame.southPressed(), frame.closeDown(), frame.nowMillis());

        // A side-by-side back press closes immediately. Own the whole gesture, including
        // release, so a rebound back button cannot also move an item or reopen a screen.
        if (sideCloseOwned) {
            if (!frame.closeDown()) sideCloseOwned = false;
            workspace.reset(); resetSort();
            return true;
        }
        if (closePressed && actions.workspaceActive() && !actions.tuckedAway() && !actions.radialActive()) {
            sideCloseOwned = true;
            workspace.reset(); resetSort();
            actions.closeWorkspace();
            return true;
        }
        // This version has no timed close gesture. Never forward back to the shared
        // legacy hold router; keep rebound back inputs from also swapping/cycling.
        ControllerInputFrame navigation = new ControllerInputFrame(frame.leftTrigger(), frame.leftStickX(), frame.leftStickY(),
                !closeOwnsButtons && frame.leftBumperPressed(), !closeOwnsButtons && frame.rightBumperPressed(),
                !closeOwnsButtons && frame.southPressed(), false, frame.nowMillis());
        // A back edge and LT release can arrive together. Close the highlighted pane
        // before the release commits a side-by-side selection.
        boolean closeBeforeRelease = closePressed && !frame.leftTrigger() && actions.workspaceActive()
                && !actions.tuckedAway() && actions.radialActive();
        if (closeBeforeRelease) actions.closeHighlighted();
        boolean consumed = workspace.update(navigation);
        if (closePressed && !closeBeforeRelease && actions.workspaceActive() && !actions.tuckedAway() && actions.radialActive()) {
            actions.closeHighlighted();
        }
        if (!actions.workspaceActive() || actions.tuckedAway()) {
            resetSort();
            return consumed || closeOwnsButtons;
        }

        if (frame.leftTrigger() && actions.radialActive() && !closeOwnsButtons) {
            if (inventoryDown && !inventoryPreviouslyDown) {
                long now = frame.nowMillis();
                boolean force = lastSortTapMillis != Long.MIN_VALUE && now >= lastSortTapMillis
                        && now - lastSortTapMillis <= DOUBLE_TAP_MILLIS;
                // Remember a first tap even when the ordinary sort moves nothing: force-sort can
                // still move unmatched items on the second tap using Core's destination rules.
                actions.sortFocused(force);
                lastSortTapMillis = force ? Long.MIN_VALUE : now;
                inventoryOwned = true;
            }
        } else {
            lastSortTapMillis = Long.MIN_VALUE;
        }

        // Own the release too, including an LT release while inventory remains held. Native
        // Controlify must not interpret the same gesture as an inventory toggle or a UI action.
        boolean sortOwnsButtons = inventoryOwned;
        if (!inventoryDown) inventoryOwned = false;
        return consumed || sortOwnsButtons || closeOwnsButtons;
    }

    public float closeProgress(long nowMillis) {
        return 0f;
    }

    public void reset() {
        workspace.reset();
        resetSort();
        closePreviouslyDown = false;
        sideCloseOwned = false;
        triggerNeedsRelease = false;
        openingTriggerSampleProvided = false;
    }

    /** An opening world-use trigger must be released before it can select workspace windows. */
    public void workspaceOpened(boolean leftTriggerHeld) {
        workspace.reset(); resetSort();
        triggerNeedsRelease = leftTriggerHeld;
    }

    /** Read the physical opening trigger before contextual input masks the navigation frame. */
    public void observeOpeningTrigger(boolean leftTriggerHeld) {
        openingTriggerSampleProvided = true;
        if (!leftTriggerHeld) triggerNeedsRelease = false;
    }

    public boolean awaitingOpeningTriggerRelease() { return triggerNeedsRelease; }

    private void resetSort() {
        lastSortTapMillis = Long.MIN_VALUE;
        inventoryOwned = false;
    }
}
