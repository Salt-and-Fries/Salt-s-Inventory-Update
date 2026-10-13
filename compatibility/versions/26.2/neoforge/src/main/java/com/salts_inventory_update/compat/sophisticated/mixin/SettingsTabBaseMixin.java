package com.salts_inventory_update.compat.sophisticated.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.salts_inventory_update.compat.sophisticated.client.SophisticatedSettingsTabChildrenAccess;

import net.p3pp3rf1y.sophisticatedcore.client.gui.SettingsTabBase;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.WidgetBase;

@Mixin(value = SettingsTabBase.class, remap = false)
public abstract class SettingsTabBaseMixin implements SophisticatedSettingsTabChildrenAccess {
    @Shadow @Final private List<WidgetBase> hideableChildren;

    @Override
    public List<WidgetBase> salts_inventory_update$hideableChildren() {
        return hideableChildren;
    }
}
