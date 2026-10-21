package com.salts_inventory_update.compat.sophisticated.mixin;

import java.util.OptionalInt;
import java.util.function.Consumer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Desc;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.salts_inventory_update.api.server.desktop.DesktopSessions;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.p3pp3rf1y.sophisticatedcore.extensions.entity.SophisticatedPlayer;

/** Captures the Fabric port's own extended opening data through the public desktop API. */
@Mixin(SophisticatedPlayer.class)
public interface ServerPlayerOpeningDataMixin {
    @Inject(target = @Desc(value = "sophisticatedCore_openMenu", args = {MenuProvider.class, Consumer.class}, ret = OptionalInt.class), at = @At("HEAD"), cancellable = true)
    private void salts_inventory_update$openDesktopMenuWithData(MenuProvider provider,
            Consumer<RegistryFriendlyByteBuf> writer, CallbackInfoReturnable<OptionalInt> cir) {
        if (!((Object) this instanceof ServerPlayer player) || !DesktopSessions.shouldCapture(player)) return;
        OptionalInt session = DesktopSessions.openMenuSession(player, provider, writer);
        if (session != null) cir.setReturnValue(session);
        else if (DesktopSessions.hasActiveSessionTransition(player)) cir.setReturnValue(OptionalInt.empty());
    }
}
