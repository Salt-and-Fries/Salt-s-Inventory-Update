package com.salts_inventory_update.compat.nativewindows.mixin;

import com.salts_inventory_update.compat.create.CreatePackets;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Preserve the native requester ghost-rack/final-configuration save order. */
@Pseudo
@Mixin(targets="com.simibubi.create.foundation.gui.menu.MenuBase",remap=false)
public abstract class CreateMenuFinalSaveMixin {
    @Inject(method={"removed","method_7595","m_6877_"},at=@At("TAIL"),remap=false,require=0)
    private void salts$afterOriginalSave(Player player,CallbackInfo ci) {
        if(player instanceof net.minecraft.server.level.ServerPlayer)
            CreatePackets.afterMenuRemoved((AbstractContainerMenu)(Object)this);
    }
}
