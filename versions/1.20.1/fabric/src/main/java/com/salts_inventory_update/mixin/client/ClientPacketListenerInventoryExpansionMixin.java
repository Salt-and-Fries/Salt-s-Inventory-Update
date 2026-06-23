package com.salts_inventory_update.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;

import com.salts_inventory_update.inventory.InventoryExpansion;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerInventoryExpansionMixin {
    @Inject(method = "handleContainerContent", at = @At("HEAD"))
    private void salts_inventory_update$ensureExpansionSlotsForContentPacket(ClientboundContainerSetContentPacket packet, CallbackInfo ci) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && packet.getContainerId() == 0) {
            InventoryExpansion.ensurePlayerMenuCanReadSlotCount(player, packet.getItems().size());
        }
    }

    @Inject(method = "handleContainerSetSlot", at = @At("HEAD"))
    private void salts_inventory_update$ensureExpansionSlotsForSlotPacket(ClientboundContainerSetSlotPacket packet, CallbackInfo ci) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && packet.getContainerId() == 0 && packet.getSlot() >= 0) {
            InventoryExpansion.ensurePlayerMenuCanReadSlotCount(player, packet.getSlot() + 1);
        }
    }
}
