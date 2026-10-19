package com.salts_inventory_update.compat.ae2.mixin;
import appeng.client.gui.me.common.MEStorageScreen;
import appeng.client.gui.widgets.Scrollbar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(value=MEStorageScreen.class,remap=false)
public interface MEStorageScrollAccessor {
    @Accessor("scrollbar") Scrollbar saltsInventory$scrollbar();
}
