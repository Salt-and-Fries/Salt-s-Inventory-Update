package com.salts_inventory_update.compat.sophisticated.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContext$Item", remap = false)
public interface BackpackContextItemAccessor {
    @Accessor("handlerName")
    String saltsInventoryUpdate$getHandlerName();

    @Accessor("identifier")
    String saltsInventoryUpdate$getIdentifier();
}
