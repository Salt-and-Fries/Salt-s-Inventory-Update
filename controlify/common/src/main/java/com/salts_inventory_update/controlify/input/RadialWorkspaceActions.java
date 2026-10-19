package com.salts_inventory_update.controlify.input;

/** Actions introduced for the 26.3 controller workspace. */
public interface RadialWorkspaceActions extends ControllerWorkspaceActions {
    /** Uses Core's ordinary all-open-window sort, with its Shift modifier when forced. */
    boolean sortFocused(boolean force);
}
