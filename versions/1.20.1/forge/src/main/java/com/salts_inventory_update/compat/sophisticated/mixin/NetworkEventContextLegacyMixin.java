package com.salts_inventory_update.compat.sophisticated.mixin;

import java.util.concurrent.CompletableFuture;

import net.minecraftforge.network.NetworkEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.salts_inventory_update.compat.sophisticated.SophisticatedLegacyMessageBridge;

@Mixin(value = NetworkEvent.Context.class, remap = false)
public abstract class NetworkEventContextLegacyMixin {
    @Inject(method = "enqueueWork", at = @At("HEAD"), cancellable = true)
    private void salts_inventory_update$runDetachedMessageImmediately(
        Runnable action,
        CallbackInfoReturnable<CompletableFuture<Void>> cir
    ) {
        if (SophisticatedLegacyMessageBridge.handlingMessage()) {
            cir.setReturnValue(SophisticatedLegacyMessageBridge.runImmediately(action));
        }
    }
}
