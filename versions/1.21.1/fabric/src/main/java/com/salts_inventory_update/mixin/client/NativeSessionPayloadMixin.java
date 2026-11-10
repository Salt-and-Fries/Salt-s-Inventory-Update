package com.salts_inventory_update.mixin.client;

import com.salts_inventory_update.api.server.desktop.DesktopServerApi;
import com.salts_inventory_update.client.automatic.AutomaticNativeScope;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Only explicitly registered native-template channels enter the authenticated
 * session transport. Ordinary mod packets and ordinary API windows stay native. */
@Mixin(Connection.class)
public abstract class NativeSessionPayloadMixin {
    @Inject(method="send(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketSendListener;Z)V",at=@At("HEAD"),cancellable=true)
    private void salts$nativeSessionPayload(Packet<?> packet,PacketSendListener listener,boolean flush,CallbackInfo ci) {
        var scope=AutomaticNativeScope.current();
        if(scope==null||scope.input()==null||!(packet instanceof ServerboundCustomPayloadPacket payload))return;
        var menu=scope.screen().getMenu();
        if(DesktopServerApi.findPayloadHandler(menu.getType(),payload.payload().type().id())==null)return;
        var mc=net.minecraft.client.Minecraft.getInstance();
        if(mc.level==null)return;
        var buffer=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),mc.level.registryAccess());
        byte[] data;
        try {
            ServerboundCustomPayloadPacket.STREAM_CODEC.encode(buffer,payload);
            buffer.readResourceLocation();
            int size=buffer.readableBytes();
            if(size>com.salts_inventory_update.api.desktop.DesktopPayloadLimits.MAX_CUSTOM_DATA_BYTES)
                throw new IllegalArgumentException("Native session payload exceeds desktop limit");
            data=new byte[size];buffer.readBytes(data);
        } finally {buffer.release();}
        // Never fall through to a different currently active vanilla menu if
        // the authenticated action is refused or the lifetime is closing.
        // Native removed() saves configuration while the owning window is
        // already being disposed. Its authenticated session is still live until
        // closeSession is sent; the ordinary input availability gate is too late.
        com.salts_inventory_update.client.DesktopContainerClient.sendCustomPayload(scope.input().sessionId(),payload.payload().type().id(),data);ci.cancel();
    }
    /** Route vanilla rename only for a native menu that explicitly registers it. */
    @Inject(method="send(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketSendListener;Z)V",at=@At("HEAD"),cancellable=true)
    private void salts$nativeRename(Packet<?> packet,PacketSendListener listener,boolean flush,CallbackInfo ci) {
        if(!(packet instanceof net.minecraft.network.protocol.game.ServerboundRenameItemPacket rename))return;
        var scope=AutomaticNativeScope.current();
        if(scope==null||scope.input()==null)return;
        var channel=net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("minecraft","rename_item");
        if(DesktopServerApi.findPayloadHandler(scope.screen().getMenu().getType(),channel)==null)return;
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
        com.salts_inventory_update.client.DesktopContainerClient.sendCustomPayload(scope.input().sessionId(),channel,data);
        ci.cancel();
    }

}
