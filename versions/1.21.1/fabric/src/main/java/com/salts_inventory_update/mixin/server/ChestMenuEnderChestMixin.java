package com.salts_inventory_update.mixin.server;

import com.salts_inventory_update.internal.desktop.DesktopEnderChestMenu;
import com.salts_inventory_update.mixin.accessor.PlayerEnderChestContainerAccessor;
import com.salts_inventory_update.server.DesktopContainerSessions;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.PlayerEnderChestContainer;
import net.minecraft.world.level.block.entity.EnderChestBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Bind native open/close/validity to the actual ender chest, not the last chest opened. */
@Mixin(ChestMenu.class)
public abstract class ChestMenuEnderChestMixin implements DesktopEnderChestMenu {
    @Unique private EnderChestBlockEntity salts_inventory_update$enderChest;
    @Unique private boolean salts_inventory_update$enderClosed;

    @Inject(method = "<init>(Lnet/minecraft/world/inventory/MenuType;ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/Container;I)V", at = @At("TAIL"))
    private void salts_inventory_update$bindEnderChest(MenuType<?> type, int id, Inventory inventory,
                                                       Container container, int rows, CallbackInfo ci) {
        if (container instanceof PlayerEnderChestContainer ender
            && inventory.player instanceof ServerPlayer player
            && DesktopContainerSessions.shouldCapture(player)) {
            this.salts_inventory_update$enderChest =
                ((PlayerEnderChestContainerAccessor) ender).salts_inventory_update$activeChest();
            if (this.salts_inventory_update$enderChest != null) {
                // startOpen already ran exactly once in the native constructor. Its global pointer
                // must not represent a detached window or override a later vanilla menu.
                ender.setActiveChest(null);
            }
        }
    }

    @Redirect(method = "stillValid", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/Container;stillValid(Lnet/minecraft/world/entity/player/Player;)Z"))
    private boolean salts_inventory_update$validateBoundChest(Container container, Player player) {
        return this.salts_inventory_update$enderChest == null ? container.stillValid(player)
            : !this.salts_inventory_update$enderClosed && this.salts_inventory_update$enderChest.stillValid(player);
    }

    @Redirect(method = "removed", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/Container;stopOpen(Lnet/minecraft/world/entity/player/Player;)V"))
    private void salts_inventory_update$closeBoundChest(Container container, Player player) {
        if (this.salts_inventory_update$enderChest == null) {
            container.stopOpen(player);
        } else if (!this.salts_inventory_update$enderClosed) {
            this.salts_inventory_update$enderClosed = true;
            this.salts_inventory_update$enderChest.stopOpen(player);
        }
    }

    @Override
    public boolean salts_inventory_update$ownsEnderChest(EnderChestBlockEntity chest) {
        return !this.salts_inventory_update$enderClosed && this.salts_inventory_update$enderChest == chest;
    }
}
