package com.salts_inventory_update.api.server.desktop;

import java.util.Objects;
import java.util.function.Predicate;
import com.salts_inventory_update.api.desktop.DesktopInventoryIdentity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;

/** A real menu over one source. Authorization must pin the actual source and its permissions. */
public record DesktopCompanionSpec(DesktopInventoryIdentity identity, MenuProvider provider,
                                   Predicate<ServerPlayer> authorization) {
    public DesktopCompanionSpec {
        Objects.requireNonNull(identity, "identity");
        Objects.requireNonNull(provider, "provider");
        Objects.requireNonNull(authorization, "authorization");
    }
}
