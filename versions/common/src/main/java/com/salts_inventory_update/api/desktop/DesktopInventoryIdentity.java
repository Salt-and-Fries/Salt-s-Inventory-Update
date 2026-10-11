package com.salts_inventory_update.api.desktop;

import java.util.Objects;
import java.util.UUID;

/** Identity of one actual inventory/source, never a menu type, item type, title or inventory position. */
public final class DesktopInventoryIdentity {
    private final Object reference;
    private final String namespace;
    private final UUID sourceId;

    private DesktopInventoryIdentity(Object reference, String namespace, UUID sourceId) {
        this.reference = reference;
        this.namespace = namespace;
        this.sourceId = sourceId;
    }

    /** Uses object identity (==) of the stable backing inventory or source object. */
    public static DesktopInventoryIdentity reference(Object inventory) {
        return new DesktopInventoryIdentity(Objects.requireNonNull(inventory, "inventory"), null, null);
    }

    /** Uses a server-assigned, unique identity persisted on the actual source. Copies must receive new IDs. */
    public static DesktopInventoryIdentity persistent(String namespace, UUID sourceId) {
        if (Objects.requireNonNull(namespace, "namespace").isBlank() || namespace.length() > 128) {
            throw new IllegalArgumentException("Inventory namespace must contain 1..128 characters");
        }
        return new DesktopInventoryIdentity(null, namespace, Objects.requireNonNull(sourceId, "sourceId"));
    }

    @Override public boolean equals(Object other) {
        if (!(other instanceof DesktopInventoryIdentity identity)) return false;
        return reference != null ? reference == identity.reference
            : identity.reference == null && namespace.equals(identity.namespace) && sourceId.equals(identity.sourceId);
    }

    @Override public int hashCode() {
        return reference != null ? System.identityHashCode(reference) : Objects.hash(namespace, sourceId);
    }
}
