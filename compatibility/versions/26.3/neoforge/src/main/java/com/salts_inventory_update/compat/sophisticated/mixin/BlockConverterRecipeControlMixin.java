package com.salts_inventory_update.compat.sophisticated.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.salts_inventory_update.compat.sophisticated.client.SophisticatedHostedPositionAccess;

import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.Button;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Position;
import net.p3pp3rf1y.sophisticatedcore.upgrades.blockconverter.BlockConverterRecipeControl;

/** Keeps Stonecutter/Sawmill's internally owned browse button aligned with its hosted recipe control. */
@Mixin(value = BlockConverterRecipeControl.class, remap = false)
public abstract class BlockConverterRecipeControlMixin implements SophisticatedHostedPositionAccess {
    @Shadow @Final private Button browseButton;

    @Override
    public void salts_inventory_update$translateHostedPosition(int dx, int dy) {
        if (dx == 0 && dy == 0) {
            return;
        }
        browseButton.setPosition(new Position(browseButton.getX() + dx, browseButton.getY() + dy));
    }
}
