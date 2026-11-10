package com.salts_inventory_update.mixin.client;

import com.salts_inventory_update.client.automatic.AutomaticNativeScope;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Native checkmarks close their own detached lifetime, preserving other windows. */
@Mixin(LocalPlayer.class)
public abstract class NativeSessionCloseMixin {
    @Inject(method="closeContainer",at=@At("HEAD"),cancellable=true)
    private void salts$closeHostedNativeMenu(CallbackInfo ci) {
        var mc = net.minecraft.client.Minecraft.getInstance();
        if (com.salts_inventory_update.client.NativeContainerView.isActive(mc)) {
            com.salts_inventory_update.client.NativeContainerView.restore(mc); ci.cancel(); return;
        }
        var scope=AutomaticNativeScope.current();
        if(scope!=null&&scope.input()!=null){scope.input().windowHandle().close();ci.cancel();}
    }
}
