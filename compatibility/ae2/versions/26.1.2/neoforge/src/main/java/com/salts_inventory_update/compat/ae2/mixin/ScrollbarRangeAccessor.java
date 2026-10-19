package com.salts_inventory_update.compat.ae2.mixin;
import appeng.client.gui.widgets.Scrollbar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(value=Scrollbar.class,remap=false)
public interface ScrollbarRangeAccessor {
    @Accessor("minScroll") int saltsInventory$minScroll();
    @Accessor("maxScroll") int saltsInventory$maxScroll();
    @Accessor("pageSize") int saltsInventory$pageSize();
}
