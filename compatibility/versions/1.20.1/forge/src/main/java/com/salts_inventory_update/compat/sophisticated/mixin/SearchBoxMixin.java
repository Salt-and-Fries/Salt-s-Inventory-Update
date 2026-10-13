package com.salts_inventory_update.compat.sophisticated.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import com.salts_inventory_update.compat.sophisticated.client.SophisticatedSearchBoxAccess;

import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.TextBox;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Dimension;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Position;

@Mixin(targets = "net.p3pp3rf1y.sophisticatedcore.client.gui.SearchBox", remap = false)
public abstract class SearchBoxMixin extends TextBox implements SophisticatedSearchBoxAccess {
    @Shadow @Final @Mutable private int maximizedX;
    @Shadow @Final @Mutable private int maximizedWidth;
    @Unique private boolean salts_inventory_update$hosted;
    @Unique private int salts_inventory_update$hostedX;

    protected SearchBoxMixin(Position position, Dimension dimension) {
        super(position, dimension);
    }

    @Invoker("isExpandedOrFocused")
    @Override
    public abstract boolean salts_inventory_update$isExpandedOrFocused();

    @Override
    public void salts_inventory_update$setHostedBounds(int x, int maximizedWidth) {
        this.salts_inventory_update$hosted = true;
        this.salts_inventory_update$hostedX = x;
        this.maximizedX = x;
        this.maximizedWidth = Math.max(10, maximizedWidth);
        if (getWidth() > this.maximizedWidth) {
            updateDimensions(this.maximizedWidth, getHeight());
        }
    }

    /** SearchBox normally expands left toward its title. In Salt's compact left cluster it expands right. */
    @ModifyArg(
        method = "renderBg",
        at = @At(
            value = "INVOKE",
            target = "Lnet/p3pp3rf1y/sophisticatedcore/client/gui/SearchBox;setPosition(Lnet/p3pp3rf1y/sophisticatedcore/client/gui/utils/Position;)V"
        ),
        index = 0,
        remap = false
    )
    private Position salts_inventory_update$expandRightInHostedWindow(Position nativePosition) {
        return this.salts_inventory_update$hosted
            ? new Position(this.salts_inventory_update$hostedX, nativePosition.y())
            : nativePosition;
    }
}
