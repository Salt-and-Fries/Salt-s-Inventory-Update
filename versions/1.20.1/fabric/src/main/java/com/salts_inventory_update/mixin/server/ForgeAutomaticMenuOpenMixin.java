package com.salts_inventory_update.mixin.server;

import java.util.function.Consumer;
import com.salts_inventory_update.server.DesktopContainerSessions;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

/** Only adopts non-API menus after Forge constructs them and writes its opening data. */
@Pseudo
@Mixin(targets = "net.minecraftforge.network.NetworkHooks", remap = false)
public abstract class ForgeAutomaticMenuOpenMixin {
    @Inject(method = "openScreen(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/world/MenuProvider;Ljava/util/function/Consumer;)V",
        at = @At(value = "INVOKE_ASSIGN", target = "Lnet/minecraft/world/MenuProvider;createMenu(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/entity/player/Player;)Lnet/minecraft/world/inventory/AbstractContainerMenu;", remap = true),
        cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD, require = 0)
    private static void salts_inventory_update$extendedOpen(ServerPlayer player, MenuProvider provider,
            Consumer<FriendlyByteBuf> writer, CallbackInfo ci, int id, FriendlyByteBuf extra,
            FriendlyByteBuf output, AbstractContainerMenu menu) {
        if (menu == null || !DesktopContainerSessions.shouldCapture(player)) return;
        // Forge has already copied extra into output, advancing its reader index.
        // Keep the complete written payload, including block positions and state.
        byte[] data = new byte[extra.writerIndex()]; extra.getBytes(0, data);
        if (DesktopContainerSessions.captureConstructedAutomaticMenu(player, provider, menu, data)) {
            extra.release(); output.release();
            ci.cancel();
        }
    }
}
