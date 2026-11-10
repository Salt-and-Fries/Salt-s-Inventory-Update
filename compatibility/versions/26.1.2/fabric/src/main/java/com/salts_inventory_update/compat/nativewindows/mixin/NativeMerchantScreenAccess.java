package com.salts_inventory_update.compat.nativewindows.mixin;

import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Native merchant selection state, independent of an upstream mod's accessors. */
@Mixin(MerchantScreen.class)
public interface NativeMerchantScreenAccess {
    @Accessor("shopItem") int salts_inventory_compat$selectedOffer();
    @Accessor("shopItem") void salts_inventory_compat$selectedOffer(int value);
    @Accessor("scrollOff") int salts_inventory_compat$scrollOffset();
    @Accessor("scrollOff") void salts_inventory_compat$scrollOffset(int value);
}
