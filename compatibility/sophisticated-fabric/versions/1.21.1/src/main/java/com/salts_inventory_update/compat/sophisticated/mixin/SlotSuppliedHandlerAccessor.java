package com.salts_inventory_update.compat.sophisticated.mixin;

import net.p3pp3rf1y.sophisticatedcore.common.gui.SlotSuppliedHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SlotSuppliedHandler.class)
public interface SlotSuppliedHandlerAccessor {
    @Accessor("slot")
    int salts_inventory_update$slot();
}
