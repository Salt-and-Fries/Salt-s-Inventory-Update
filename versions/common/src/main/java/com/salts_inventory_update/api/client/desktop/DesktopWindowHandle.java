package com.salts_inventory_update.api.client.desktop;

import java.util.UUID;

/** A specific window lifetime. Operations use the client thread, except thread-safe schedule. */
public interface DesktopWindowHandle {
    UUID instanceId();
    boolean isOpen();
    /** Returns false when closed or when another modal window owns focus. */
    boolean focus();
    /** Permanently disposes an independent instance. Shared companions reject this; release server ownership instead. */
    boolean close();

    DesktopWindowState state();

    boolean available(DesktopWindowAction action);

    /** MOVE/RESIZE require their parameterized methods and return false here. */
    boolean request(DesktopWindowAction action);

    boolean moveTo(int x, int y);

    boolean resizeTo(int width, int height);

    /** Cancel local pointer/slot gestures before replacing an internal subview. Does not undo sent actions. */
    boolean cancelInput();

    /** Enqueue on the client thread; a closed lifetime suppresses the callback. */
    boolean schedule(Runnable action);
}
