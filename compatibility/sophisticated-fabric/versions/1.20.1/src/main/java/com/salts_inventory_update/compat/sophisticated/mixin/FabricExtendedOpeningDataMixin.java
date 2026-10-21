package com.salts_inventory_update.compat.sophisticated.mixin;

import java.util.OptionalInt;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import com.salts_inventory_update.api.server.desktop.DesktopSessions;

/** Preserves the native helper factory's opening bytes before Minecraft's ordinary open-menu path. */
@Mixin(value = ServerPlayer.class, priority = 1100)
public abstract class FabricExtendedOpeningDataMixin {
    @Inject(method = "openMenu", at = @At("HEAD"), cancellable = true)
    private void salts_inventory_update$openNativeExtendedFactory(MenuProvider provider,
            CallbackInfoReturnable<OptionalInt> cir) {
        if (!(provider instanceof ExtendedScreenHandlerFactory factory)
                || !provider.getClass().getName().startsWith("net.p3pp3rf1y.sophisticatedcore.")) return;
        ServerPlayer player=(ServerPlayer)(Object)this;
        if (!DesktopSessions.shouldCapture(player)) return;
        OptionalInt session=DesktopSessions.openMenuSession(player,provider,
            buffer -> factory.writeScreenOpeningData(player,buffer));
        if (session!=null) cir.setReturnValue(session);
        else if (DesktopSessions.hasActiveSessionTransition(player)) cir.setReturnValue(OptionalInt.empty());
    }
}
