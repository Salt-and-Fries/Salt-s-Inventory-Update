package com.salts_inventory_update.compat.accessories.forge;
import java.util.UUID;
import com.salts_inventory_update.compat.accessories.*;
import net.minecraftforge.network.*;
import net.minecraftforge.network.simple.SimpleChannel;
public final class AccessoriesForgeTransport {
    private static SimpleChannel channel;
    private AccessoriesForgeTransport() { }
    public static void initialize(){
        channel=NetworkRegistry.newSimpleChannel(AccessoriesIntegration.id("accessories/open"),()->"1","1"::equals,"1"::equals);
        channel.registerMessage(0,Open.class,(packet,buffer)->buffer.writeUUID(packet.owner()),buffer->new Open(buffer.readUUID()),(packet,context)->{
            var c=context.get();c.enqueueWork(()->{var player=c.getSender();if(player!=null)AccessoriesTransport.receive(player,packet.owner());});c.setPacketHandled(true);
        },java.util.Optional.of(NetworkDirection.PLAY_TO_SERVER));
    }
    public static void open(UUID owner){channel.sendToServer(new Open(owner));}
    private record Open(UUID owner) { }
}
