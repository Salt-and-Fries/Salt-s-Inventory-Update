package com.salts_inventory_update.compat.alloyforgery.mixin;

import com.salts_inventory_update.compat.alloyforgery.AlloyScreens;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets="wraith.alloyforgery.client.AlloyForgeScreen", remap=false)
abstract class AlloyForgeScreenCaptureMixin extends AbstractContainerScreen<AbstractContainerMenu> {
    protected AlloyForgeScreenCaptureMixin(AbstractContainerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Inject(method="<init>", at=@At("RETURN"))
    private void salts$rememberNativeSurface(CallbackInfo ci) {
        AlloyScreens.remember(getMenu(), this);
    }
}
