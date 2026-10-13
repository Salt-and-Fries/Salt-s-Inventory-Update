package com.salts_inventory_update.controlify.input;

import java.util.Objects;

/** Routes only the inputs explicitly replaced by the controller workspace. */
public final class WorkspaceInputRouter {
    private final ControllerWorkspaceActions actions;
    private boolean closing;
    private boolean holdCompleted;
    private long closeStartedMillis;

    public WorkspaceInputRouter(ControllerWorkspaceActions actions) {
        this.actions = Objects.requireNonNull(actions, "actions");
    }

    /** @return whether this tick belongs to radial selection rather than native UI input. */
    public boolean update(ControllerInputFrame frame) {
        if (!actions.workspaceActive() || actions.tuckedAway()) {
            reset();
            return true;
        }

        boolean closeOwnsButtons = closing || frame.closeDown();
        // Complete a short close press before committing an LT release in the same sample.
        if (frame.closeDown()) {
            if (!closing) {
                closing = true;
                holdCompleted = false;
                closeStartedMillis = frame.nowMillis();
            }
            if (!holdCompleted && frame.nowMillis() - closeStartedMillis >= Math.max(1L, actions.closeHoldDurationMillis())) {
                holdCompleted = true;
                actions.closeWorkspace();
                return true;
            }
        } else if (closing) {
            boolean closeWindow = !holdCompleted && actions.radialActive();
            reset();
            if (closeWindow) {
                actions.closeHighlighted();
            }
        }

        boolean radialAtStart = actions.radialActive();
        if (frame.leftTrigger()) {
            if (!actions.radialActive()) {
                actions.enterRadial();
            }
            actions.radialDirection(frame.leftStickX(), frame.leftStickY());
            if (frame.southPressed() && !closeOwnsButtons) {
                actions.swapFocused();
            }
            return true;
        }

        if (radialAtStart) {
            actions.releaseRadial();
            // Consume the release tick too, avoiding a click in the newly selected native UI.
            return true;
        }

        if (frame.leftBumperPressed() && !closeOwnsButtons) {
            actions.cycleSelected(-1);
        }
        if (frame.rightBumperPressed() && !closeOwnsButtons) {
            actions.cycleSelected(1);
        }
        return false;
    }

    public void reset() {
        closing = false;
        holdCompleted = false;
        closeStartedMillis = 0L;
    }

    public float closeProgress(long nowMillis) {
        if (!closing) {
            return 0f;
        }
        return (float) Math.min(1d, Math.max(0d,
                (nowMillis - closeStartedMillis) / (double) Math.max(1L, actions.closeHoldDurationMillis())));
    }
}
