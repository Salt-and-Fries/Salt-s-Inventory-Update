package com.salts_inventory_update.compat.sophisticated.mixin;

import com.salts_inventory_update.internal.desktop.DesktopItemSourceLocks;

import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundPickItemPacket;
import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerItemLockMixin {
    private static final String ENSURE_SERVER_THREAD =
        "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread"
            + "(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;"
            + "Lnet/minecraft/server/level/ServerLevel;)V";

    @Shadow
    public ServerPlayer player;

    @Inject(method = "handleContainerClick", at = @At(value = "INVOKE", target = ENSURE_SERVER_THREAD, shift = At.Shift.AFTER), cancellable = true)
    private void saltsInventoryUpdate$rejectLockedContainerClick(ServerboundContainerClickPacket packet, CallbackInfo ci) {
        if (this.player.containerMenu.containerId == packet.getContainerId()
            && DesktopItemSourceLocks.shouldRejectClick(
                this.player,
                this.player.containerMenu,
                packet.getSlotNum(),
                packet.getButtonNum(),
                packet.getClickType(),
                this.player.containerMenu.getCarried()
            )) {
            this.player.containerMenu.sendAllDataToRemote();
            this.player.inventoryMenu.broadcastChanges();
            ci.cancel();
        }
    }

    @Inject(method = "handlePlayerAction", at = @At(value = "INVOKE", target = ENSURE_SERVER_THREAD, shift = At.Shift.AFTER), cancellable = true)
    private void saltsInventoryUpdate$rejectLockedPlayerAction(ServerboundPlayerActionPacket packet, CallbackInfo ci) {
        ServerboundPlayerActionPacket.Action action = packet.getAction();
        boolean dropsSelected = action == ServerboundPlayerActionPacket.Action.DROP_ITEM
            || action == ServerboundPlayerActionPacket.Action.DROP_ALL_ITEMS;
        boolean swapsHands = action == ServerboundPlayerActionPacket.Action.SWAP_ITEM_WITH_OFFHAND;
        if (dropsSelected && DesktopItemSourceLocks.isSelectedSlotLocked(this.player)
            || swapsHands && (DesktopItemSourceLocks.isSelectedSlotLocked(this.player)
                || DesktopItemSourceLocks.isOffhandSlotLocked(this.player))) {
            this.player.inventoryMenu.sendAllDataToRemote();
            ci.cancel();
        }
    }

    @Inject(method = "handleUseItem", at = @At(value = "INVOKE", target = ENSURE_SERVER_THREAD, shift = At.Shift.AFTER), cancellable = true)
    private void saltsInventoryUpdate$rejectLockedUse(ServerboundUseItemPacket packet, CallbackInfo ci) {
        if (DesktopItemSourceLocks.isHandLocked(this.player, packet.getHand())) {
            ci.cancel();
        }
    }

    @Inject(method = "handleUseItemOn", at = @At(value = "INVOKE", target = ENSURE_SERVER_THREAD, shift = At.Shift.AFTER), cancellable = true)
    private void saltsInventoryUpdate$rejectLockedUseOn(ServerboundUseItemOnPacket packet, CallbackInfo ci) {
        if (DesktopItemSourceLocks.isHandLocked(this.player, packet.getHand())) {
            ci.cancel();
        }
    }

    @Inject(method = "handleInteract", at = @At(value = "INVOKE", target = ENSURE_SERVER_THREAD, shift = At.Shift.AFTER), cancellable = true)
    private void saltsInventoryUpdate$rejectLockedInteract(ServerboundInteractPacket packet, CallbackInfo ci) {
        final boolean[] locked = {false};
        packet.dispatch(new ServerboundInteractPacket.Handler() {
            @Override
            public void onInteraction(net.minecraft.world.InteractionHand hand) {
                locked[0] = DesktopItemSourceLocks.isHandLocked(ServerGamePacketListenerItemLockMixin.this.player, hand);
            }

            @Override
            public void onInteraction(net.minecraft.world.InteractionHand hand, net.minecraft.world.phys.Vec3 location) {
                locked[0] = DesktopItemSourceLocks.isHandLocked(ServerGamePacketListenerItemLockMixin.this.player, hand);
            }

            @Override
            public void onAttack() {
            }
        });
        if (locked[0]) {
            ci.cancel();
        }
    }

    @Inject(method = "handleSetCreativeModeSlot", at = @At(value = "INVOKE", target = ENSURE_SERVER_THREAD, shift = At.Shift.AFTER), cancellable = true)
    private void saltsInventoryUpdate$rejectLockedCreativeSlot(ServerboundSetCreativeModeSlotPacket packet, CallbackInfo ci) {
        boolean lockedTarget = packet.getSlotNum() >= 0
            && packet.getSlotNum() < this.player.inventoryMenu.slots.size()
            && DesktopItemSourceLocks.isSlotLocked(this.player, this.player.inventoryMenu.getSlot(packet.getSlotNum()));
        if (lockedTarget || packet.getSlotNum() < 0 && DesktopItemSourceLocks.matchesLockedSource(this.player, packet.getItem())) {
            this.player.inventoryMenu.sendAllDataToRemote();
            ci.cancel();
        }
    }

    @Inject(method = "handlePickItem", at = @At(value = "INVOKE", target = ENSURE_SERVER_THREAD, shift = At.Shift.AFTER), cancellable = true)
    private void saltsInventoryUpdate$rejectLockedPick(ServerboundPickItemPacket packet, CallbackInfo ci) {
        if (DesktopItemSourceLocks.isSelectedSlotLocked(this.player)
            || DesktopItemSourceLocks.isInventorySlotLocked(this.player, packet.getSlot())) {
            this.player.inventoryMenu.sendAllDataToRemote();
            ci.cancel();
        }
    }
}
