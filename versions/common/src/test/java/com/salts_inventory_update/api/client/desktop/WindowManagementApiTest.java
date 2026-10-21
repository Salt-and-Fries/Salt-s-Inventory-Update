package com.salts_inventory_update.api.client.desktop;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

final class WindowManagementApiTest {
    @Test void optionsAndSnapshotsExposeOnlySupportedWindowStates() {
        assertEquals(List.of("movable", "pinnable", "closable", "lockable", "managementMenu"),
            Arrays.stream(DesktopWindowOptions.class.getRecordComponents()).map(component -> component.getName()).toList());
        assertEquals(List.of("instanceId", "open", "x", "y", "width", "height", "pinned", "locked",
                "keyboardFocused", "inventoryFocused", "visible"),
            Arrays.stream(DesktopWindowState.class.getRecordComponents()).map(component -> component.getName()).toList());
        assertEquals(List.of(3, 5), Arrays.stream(DesktopWindowOptions.class.getConstructors())
            .map(constructor -> constructor.getParameterCount()).sorted().toList());
    }

    @Test void actionsAndControlsContainNoRemovedCommands() {
        assertEquals(List.of("FOCUS", "USER_CLOSE", "PIN", "UNPIN", "MOVE", "RESIZE", "LOCK", "UNLOCK", "MANAGEMENT_MENU"),
            Arrays.stream(DesktopWindowAction.values()).map(Enum::name).toList());
        assertEquals(List.of("FOCUS", "CLOSE", "ELLIPSIS", "LINK", "LOCK", "PIN", "SORT_WINDOWS", "RESIZE_GRIP"),
            Arrays.stream(DesktopWindowControl.values()).map(Enum::name).toList());
    }
}
