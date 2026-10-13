package com.salts_inventory_update.compat.sophisticated.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

import com.salts_inventory_update.compat.sophisticated.client.SophisticatedInventoryControlPositionAccess;

import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Position;

/**
 * Tank and battery columns keep their location in immutable private fields rather than in a WidgetBase.
 * Both target classes have this exact field layout in Sophisticated Core 26.2-1.4.101.2276.
 */
@Mixin(
    targets = {
        "net.p3pp3rf1y.sophisticatedcore.upgrades.tank.TankInventoryControl",
        "net.p3pp3rf1y.sophisticatedcore.upgrades.battery.BatteryInventoryControl"
    },
    remap = false
)
public abstract class PositionedInventoryControlMixin implements SophisticatedInventoryControlPositionAccess {
    @Shadow(remap = false) @Final @Mutable private Position pos;
    @Shadow(remap = false) @Final private int height;

    @Override
    public Position salts_inventory_update$getPosition() {
        return pos;
    }

    @Override
    public void salts_inventory_update$setPosition(Position position) {
        pos = position;
    }

    @Override
    public int salts_inventory_update$getHeight() {
        return height;
    }
}
