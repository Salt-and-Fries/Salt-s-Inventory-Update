package com.salts_inventory_update.api.client.desktop;

/** UI constraints; resizing is separately controlled by the definition's resize policy. */
public record DesktopWindowOptions(boolean movable, boolean minimizable, boolean pinnable,
                                   boolean ghostable, boolean closable, boolean lockable, boolean managementMenu) {
    public static final DesktopWindowOptions DEFAULT = new DesktopWindowOptions(true, true, true, true, true, true, true);
    public static final DesktopWindowOptions DRAG_ONLY = new DesktopWindowOptions(true, false, false, false, false, false, false);

    /** Ordinary windows retain the existing management policy unless explicitly constrained. */
    public DesktopWindowOptions(boolean movable, boolean minimizable, boolean pinnable, boolean ghostable, boolean closable) {
        this(movable, minimizable, pinnable, ghostable, closable, movable, true);
    }
}
