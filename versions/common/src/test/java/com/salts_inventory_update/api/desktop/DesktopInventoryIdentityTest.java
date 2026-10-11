package com.salts_inventory_update.api.desktop;

import java.util.UUID;
import java.util.HashMap;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DesktopInventoryIdentityTest {
    @Test void referenceIdentityDeduplicatesActualSourcesRatherThanEqualValues() {
        Object first = new String("same title");
        Object second = new String("same title");
        var one = DesktopInventoryIdentity.reference(first);
        var anotherOwner = DesktopInventoryIdentity.reference(first);
        var otherTool = DesktopInventoryIdentity.reference(second);
        var inventories = new HashMap<DesktopInventoryIdentity, Object>();
        inventories.put(one, first);
        assertSame(first, inventories.get(anotherOwner));
        assertNotEquals(one, otherTool);
        assertNull(inventories.get(otherTool));
    }
    @Test void persistentIdentitySurvivesWrapperReconstructionAndSeparatesToolsAndNamespaces() {
        UUID id = UUID.randomUUID();
        var first = DesktopInventoryIdentity.persistent("example:network_tool", id);
        assertEquals(first, DesktopInventoryIdentity.persistent("example:network_tool", UUID.fromString(id.toString())));
        assertNotEquals(first, DesktopInventoryIdentity.persistent("example:network_tool", UUID.randomUUID()));
        assertNotEquals(first, DesktopInventoryIdentity.persistent("other:inventory", id));
        assertNotEquals(first, DesktopInventoryIdentity.reference(id));
    }
    @Test void sourceIdentityRejectsMissingAndUnboundedIdentityData() {
        assertThrows(NullPointerException.class, () -> DesktopInventoryIdentity.reference(null));
        assertThrows(IllegalArgumentException.class, () -> DesktopInventoryIdentity.persistent(" ", UUID.randomUUID()));
        assertThrows(IllegalArgumentException.class, () -> DesktopInventoryIdentity.persistent("x".repeat(129), UUID.randomUUID()));
        assertThrows(NullPointerException.class, () -> DesktopInventoryIdentity.persistent("x", null));
    }
}
