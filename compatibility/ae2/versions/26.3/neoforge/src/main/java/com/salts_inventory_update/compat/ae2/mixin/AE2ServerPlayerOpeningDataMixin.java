package com.salts_inventory_update.compat.ae2.mixin;

import com.salts_inventory_update.api.server.desktop.DesktopSessions;
import java.util.OptionalInt;
import java.util.function.Consumer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Captures AE2's native extra-data opener through the same supported API as other mods. */
@Mixin(ServerPlayer.class)
public abstract class AE2ServerPlayerOpeningDataMixin {
    @Inject(method = "openMenu(Lnet/minecraft/world/MenuProvider;Ljava/util/function/Consumer;)Ljava/util/OptionalInt;",
            at = @At("HEAD"), cancellable = true)
    private void salts$openAE2DesktopMenu(MenuProvider provider, Consumer<RegistryFriendlyByteBuf> writer,
            CallbackInfoReturnable<OptionalInt> result) {
        if (!provider.getClass().getName().startsWith("appeng.")) return;
        var player = (ServerPlayer) (Object) this;
        if (!DesktopSessions.shouldCapture(player)) return;
        OptionalInt session = DesktopSessions.openMenuSession(player, provider, writer);
        if (session != null) {
            result.setReturnValue(session);
        } else if (DesktopSessions.hasActiveSessionTransition(player)) {
            result.setReturnValue(OptionalInt.empty());
        }
    }
}
