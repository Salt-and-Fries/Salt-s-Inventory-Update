package com.salts_inventory_update.compat.quark.forge;

import java.util.*;
import com.salts_inventory_update.compat.quark.QuarkWindows;
import net.minecraftforge.network.*;
import net.minecraftforge.network.simple.SimpleChannel;

/** Opening requests can only address the sender's equipped bag. */
public final class QuarkForgeTransport {
    private static SimpleChannel channel;
    private QuarkForgeTransport() { }
    public static void initialize() {
        channel=NetworkRegistry.newSimpleChannel(QuarkWindows.id("quark/backpack_open"),()->"1","1"::equals,"1"::equals);
        channel.registerMessage(0,Open.class,(p,b)->b.writeUUID(p.owner),b->new Open(b.readUUID()),(p,ctx)->{
            var c=ctx.get();c.enqueueWork(()->{if(c.getSender()!=null)QuarkWindows.openBackpack(c.getSender(),p.owner);});c.setPacketHandled(true);
        },Optional.of(NetworkDirection.PLAY_TO_SERVER));
    }
    public static void open(UUID owner) { channel.sendToServer(new Open(owner)); }
    private record Open(UUID owner) { }
}
