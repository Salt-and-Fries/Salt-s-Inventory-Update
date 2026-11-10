package com.salts_inventory_update.compat.aether.fabric;
import java.util.UUID;
import com.salts_inventory_update.compat.aether.*;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.resources.ResourceLocation;

public final class AetherFabricTransport {
    private static final ResourceLocation OPEN=new ResourceLocation("salts_inventory_compat","aether/open");
    private AetherFabricTransport() { }
    public static void initialize(){ServerPlayNetworking.registerGlobalReceiver(OPEN,(server,player,handler,buffer,response)->{if(buffer.readableBytes()!=16)return;UUID owner=buffer.readUUID();server.execute(()->AetherEquipment.receive(player,owner));});}
    public static void open(UUID owner){var b=PacketByteBufs.create();b.writeUUID(owner);ClientPlayNetworking.send(OPEN,b);}
}
