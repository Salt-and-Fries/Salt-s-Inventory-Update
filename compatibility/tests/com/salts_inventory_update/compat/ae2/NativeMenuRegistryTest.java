package com.salts_inventory_update.compat.ae2;

import com.salts_inventory_update.compat.ae2.menu.NativeMenuRegistry;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class NativeMenuRegistryTest {
    @Test void nestedNativeBuildCallsPreserveTheSameBuilder() {
        var type = new Object();
        var builder = new Object();
        NativeMenuRegistry.capture(type, builder);
        NativeMenuRegistry.capture(type, builder);
        assertSame(builder, NativeMenuRegistry.builders().get(type));
        assertThrows(IllegalStateException.class, () -> NativeMenuRegistry.capture(type, new Object()));
        assertSame(builder, NativeMenuRegistry.builders().get(type));
    }

    @Test void registrySnapshotsAreImmutableAndIndependentOfLaterRegistrations() {
        var type = new Object();
        var snapshot = NativeMenuRegistry.builders();
        assertThrows(UnsupportedOperationException.class, () -> snapshot.put(type, new Object()));
        NativeMenuRegistry.capture(type, new Object());
        assertFalse(snapshot.containsKey(type));
        assertTrue(NativeMenuRegistry.builders().containsKey(type));
    }
}
