package com.salts_inventory_update.compat.ae2.mixin;

import appeng.items.contents.NetworkToolMenuHost;
import com.salts_inventory_update.compat.ae2.menu.DesktopToolboxAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Accessor;

@Pseudo
@Mixin(targets = "appeng.menu.ToolboxMenu", remap = false)
public interface ToolboxMenuAccessor extends DesktopToolboxAccess {
    @Override
    @Accessor("inv")
    NetworkToolMenuHost<?> salts$toolboxHost();
}
