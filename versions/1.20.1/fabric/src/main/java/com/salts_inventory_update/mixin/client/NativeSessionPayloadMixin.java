package com.salts_inventory_update.mixin.client;

import com.salts_inventory_update.api.server.desktop.DesktopServerApi;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundCustomPayloadPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Only explicitly registered native-template channels enter the authenticated
 * session transport. Ordinary mod packets and ordinary API windows stay native. */
@Mixin(Connection.class)
public abstract class NativeSessionPayloadMixin {
    @Inject(method="send(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketSendListener;)V",at=@At("HEAD"),cancellable=true)
    private void salts$nativeSessionPayload(Packet<?> packet,PacketSendListener listener,CallbackInfo ci) {
        var scope=com.salts_inventory_update.client.NativeContainerView.input();
        if(scope==null||!(packet instanceof ServerboundCustomPayloadPacket payload))return;
        var menu=scope.screen().getMenu();
        var type=com.salts_inventory_update.client.NativeContainerView.registeredMenuType(menu);
        if(type==null||DesktopServerApi.findPayloadHandler(type,payload.getIdentifier())==null)return;
        var buffer=payload.getData();int size=buffer.readableBytes();
        if(size<0||size>com.salts_inventory_update.api.desktop.DesktopPayloadLimits.MAX_CUSTOM_DATA_BYTES)
            throw new IllegalArgumentException("Native session payload exceeds desktop limit");
        byte[] data=new byte[size];buffer.getBytes(buffer.readerIndex(),data);
        // Never fall through to a different currently active vanilla menu if
        // the authenticated action is refused or the lifetime is closing.
        // Native removed() saves configuration while the owning window is
        // already being disposed. Its authenticated session is still live until
        // closeSession is sent; the ordinary input availability gate is too late.
        com.salts_inventory_update.client.DesktopContainerClient.sendCustomPayload(scope.sessionId(),payload.getIdentifier(),data);ci.cancel();
    }
    /** Route vanilla rename only for a native menu that explicitly registers it. */
    @Inject(method="send(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketSendListener;)V",at=@At("HEAD"),cancellable=true)
    private void salts$nativeRename(Packet<?> packet,PacketSendListener listener,CallbackInfo ci) {
        if(!(packet instanceof net.minecraft.network.protocol.game.ServerboundRenameItemPacket rename))return;
        var scope=com.salts_inventory_update.client.NativeContainerView.input();
        if(scope==null)return;
        if (com.salts_inventory_update.client.NativeContainerView.isActive(net.minecraft.client.Minecraft.getInstance())
                && scope.screen().getMenu() instanceof net.minecraft.world.inventory.AnvilMenu) {
            com.salts_inventory_update.client.DesktopContainerClient.renameAnvil(scope.sessionId(), rename.getName());
            ci.cancel(); return;
        }
        var channel=new net.minecraft.resources.ResourceLocation("minecraft","rename_item");
        var type=com.salts_inventory_update.client.NativeContainerView.registeredMenuType(scope.screen().getMenu());
        if(type==null||DesktopServerApi.findPayloadHandler(type,channel)==null)return;
        var buffer=new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        byte[] data;
        try {
            buffer.writeUtf(rename.getName(),32767);
            int size=buffer.readableBytes();
            if(size>com.salts_inventory_update.api.desktop.DesktopPayloadLimits.MAX_CUSTOM_DATA_BYTES)
                throw new IllegalArgumentException("Native rename payload exceeds desktop limit");
            data=new byte[size];buffer.readBytes(data);
        } finally {buffer.release();}
        // A refused/closing addressed action must never fall through to a
        // different ordinary container. The native menu still validates names.
        com.salts_inventory_update.client.DesktopContainerClient.sendCustomPayload(scope.sessionId(),channel,data);
        ci.cancel();
    }

    @Inject(method="send(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketSendListener;)V",at=@At("HEAD"),cancellable=true)
    private void salts$fullNativeActions(Packet<?> packet,PacketSendListener listener,CallbackInfo ci) {
        var mc=net.minecraft.client.Minecraft.getInstance();
        if(!com.salts_inventory_update.client.NativeContainerView.isActive(mc))return;
        var scope=com.salts_inventory_update.client.NativeContainerView.input();
        if(scope==null)return;
        if(packet instanceof net.minecraft.network.protocol.game.ServerboundSelectTradePacket trade) {
            com.salts_inventory_update.client.DesktopContainerClient.clickButton(scope.sessionId(),trade.getItem());ci.cancel();
        } else if(packet instanceof net.minecraft.network.protocol.game.ServerboundSetBeaconPacket beacon) {
            int primary=net.minecraft.world.effect.MobEffect.getIdFromNullable(beacon.getPrimary().orElse(null))&0xFFFF;
            int secondary=net.minecraft.world.effect.MobEffect.getIdFromNullable(beacon.getSecondary().orElse(null))&0xFFFF;
            com.salts_inventory_update.client.DesktopContainerClient.clickButton(scope.sessionId(),primary|(secondary<<16));ci.cancel();
        }
    }
}
