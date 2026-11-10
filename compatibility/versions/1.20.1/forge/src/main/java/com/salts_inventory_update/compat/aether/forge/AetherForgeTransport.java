package com.salts_inventory_update.compat.aether.forge;
import java.util.*;
import com.salts_inventory_update.compat.aether.AetherEquipment;
import net.minecraftforge.network.*;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.Slot;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.common.inventory.CurioSlot;
import top.theillusivec4.curios.common.network.NetworkHandler;
import top.theillusivec4.curios.common.network.server.sync.SPacketSyncRender;

public final class AetherForgeTransport {
    private static SimpleChannel channel;
    private AetherForgeTransport() { }
    public static void initialize(){
        channel=NetworkRegistry.newSimpleChannel(new ResourceLocation("salts_inventory_compat","aether/open"),()->"1","1"::equals,"1"::equals);
        channel.registerMessage(0,Open.class,(p,b)->b.writeUUID(p.owner),b->new Open(b.readUUID()),(p,supplied)->{var c=supplied.get();c.enqueueWork(()->{if(c.getSender()!=null)AetherEquipment.receive(c.getSender(),p.owner);});c.setPacketHandled(true);},Optional.of(NetworkDirection.PLAY_TO_SERVER));
    }
    public static void open(UUID owner){channel.sendToServer(new Open(owner));}
    public static void toggle(Slot nativeSlot,ServerPlayer player){
        if(!(nativeSlot instanceof CurioSlot slot)||!slot.canToggleRender())return;
        CuriosApi.getCuriosInventory(player).resolve().flatMap(c->c.getStacksHandler(slot.getIdentifier())).ifPresent(group->{
            int i=slot.getContainerSlot();if(i<0||i>=group.getRenders().size())return;
            boolean value=!group.getRenders().get(i);group.getRenders().set(i,value);
            NetworkHandler.INSTANCE.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(()->player),new SPacketSyncRender(player.getId(),slot.getIdentifier(),i,value));
        });
    }
    private record Open(UUID owner) { }
}
