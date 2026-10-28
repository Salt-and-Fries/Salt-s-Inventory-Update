package com.salts_inventory_update.compat.alloyforgery;

import com.salts_inventory_update.api.server.desktop.DesktopSessions;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** The native property bytes travel through the public session-owned transport. */
public final class AlloyPropertyBridge {
    public static final Identifier CHANNEL=Identifier.fromNamespaceAndPath("salts_inventory_compat","alloy_properties");
    private AlloyPropertyBridge() { }
    public static boolean send(ServerPlayer player,CustomPacketPayload payload) {
        if(!payload.type().id().equals(Identifier.fromNamespaceAndPath("owo","sync_menu_properties")))return false;
        if(DesktopSessions.activeNetworkMenu(player).filter(menu->menu.getClass().getName().equals("io.wispforest.alloyforgery.AlloyForgeScreenHandler")).isEmpty())return false;
        try{
            var buffer=(FriendlyByteBuf)payload.getClass().getMethod("payload").invoke(payload);
            byte[] data=new byte[buffer.readableBytes()];buffer.getBytes(buffer.readerIndex(),data);
            boolean sent=DesktopSessions.sendSessionPayload(player,DesktopSessions.activeNetworkSessionId(player),CHANNEL,data);
            if(sent)buffer.release();
            return sent;
        }catch(ReflectiveOperationException failure){throw new IllegalStateException("Alloy property transport failed",failure);}
    }
}
