package com.salts_inventory_update.compat.nativewindows;

import com.salts_inventory_update.api.client.desktop.*;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.AnvilMenu;

/** Native text is a replacement value, so only its newest unsent value is retained. */
public final class EasyAnvilsRenameClient {
    private static final java.util.Map<AbstractContainerMenu,Pending> OWNERS=new java.util.IdentityHashMap<>();
    private static boolean initialized;
    private static final class Pending {
        DesktopInputContext<?,?> input;
        String name;
        Pending(DesktopInputContext<?,?> input){this.input=input;}
    }
    private EasyAnvilsRenameClient() { }
    static void initialize(){
        if(initialized)return;
        initialized=true;
        DesktopClientApi.onClientTick(client->flush());
    }
    static void bind(DesktopInputContext<?,?> input){
        if(input.model() instanceof AnvilMenu menu&&menu.getClass().getName().startsWith("fuzs.easyanvils."))
            OWNERS.computeIfAbsent(menu,key->new Pending(input)).input=input;
    }
    public static void changed(AbstractContainerMenu menu,String name){
        var pending=OWNERS.get(menu);
        if(pending!=null&&pending.input.windowHandle().isOpen())pending.name=name;
    }
    static void forget(AbstractContainerMenu menu){OWNERS.remove(menu);}
    private static void flush(){
        var iterator=OWNERS.entrySet().iterator();
        while(iterator.hasNext()){
            var pending=iterator.next().getValue();
            if(!pending.input.windowHandle().isOpen()){iterator.remove();continue;}
            if(pending.name==null)continue;
            String name=pending.name;
            var buffer=new FriendlyByteBuf(Unpooled.buffer());
            byte[] bytes;
            try{buffer.writeUtf(name);bytes=new byte[buffer.readableBytes()];buffer.readBytes(bytes);}
            finally{buffer.release();}
            if(pending.input.sendPayload(net.minecraft.resources.Identifier.fromNamespaceAndPath("easyanvils","main/1"),bytes)&&name.equals(pending.name))pending.name=null;
        }
    }
}
