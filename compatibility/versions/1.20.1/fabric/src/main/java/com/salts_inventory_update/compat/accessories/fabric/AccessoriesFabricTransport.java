package com.salts_inventory_update.compat.accessories.fabric;

import java.util.UUID;
import com.salts_inventory_update.api.server.desktop.DesktopSessions;
import com.salts_inventory_update.compat.accessories.*;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.resources.ResourceLocation;

/** Loader transport only. All inventory mutations use authenticated Core sessions. */
public final class AccessoriesFabricTransport {
    private static final ResourceLocation OPEN=AccessoriesIntegration.id("accessories/open");
    private AccessoriesFabricTransport() { }
    public static boolean supported(){return FabricLoader.getInstance().getModContainer("accessories").map(m->m.getMetadata().getVersion().getFriendlyString().equals("1.0.0-beta.48+1.20.1")).orElse(false);}
    public static void initialize(){
        ServerPlayNetworking.registerGlobalReceiver(OPEN,(server,player,handler,buf,response)->{
            if(buf.readableBytes()!=16)return;UUID owner=buf.readUUID();server.execute(()->{
                if(DesktopSessions.shouldCapture(player)&&!DesktopSessions.hasOpenSessionMatching(player,m->m instanceof AccessoriesDesktopMenu))AccessoriesIntegration.open(player,owner);
            });
        });
    }
    public static void open(UUID owner){var b=PacketByteBufs.create();b.writeUUID(owner);ClientPlayNetworking.send(OPEN,b);}
}
