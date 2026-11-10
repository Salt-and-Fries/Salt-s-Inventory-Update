package com.salts_inventory_update.compat.ssn;

import com.salts_inventory_update.compat.ssn.SsnLayouts.Kind;
import java.lang.reflect.*;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;

/** Explicit per-menu action list; publisher codecs and native checks remain authoritative. */
public final class SsnPackets {
    public static final String PREFIX=SsnLayouts.BASE+"network.";
    public static final List<String> SERVER=List.of("CableDataMessage","CableIOMessage","InsertMessage","RequestMessage","ClearRecipeMessage","SettingsSyncMessage","RecipeMessage","CableProcessMessage");
    public static final List<String> CLIENT=List.of("StackRefreshClientMessage","StackResponseClientMessage","RefreshFilterClientMessage","SortClientMessage");
    private SsnPackets() { }
    public static boolean serverMessage(Object message){return message!=null&&SERVER.stream().anyMatch(n->message.getClass().getName().equals(PREFIX+n));}
    public static boolean clientMessage(Object message){return message!=null&&CLIENT.stream().anyMatch(n->message.getClass().getName().equals(PREFIX+n));}
    public static boolean allowed(Kind kind,Object m){return switch(m.getClass().getSimpleName()){
        case "CableDataMessage"->kind==Kind.LINK;
        case "CableIOMessage"->kind==Kind.IMPORT||kind==Kind.EXPORT||kind==Kind.COLLECTION;
        case "InsertMessage","RequestMessage","SettingsSyncMessage"->kind.network;
        case "ClearRecipeMessage","RecipeMessage"->kind.crafting();
        case "CableProcessMessage"->kind==Kind.PROCESS;
        default->false;
    };}
    private static int number(Object m,String key){return((Number)SsnWindows.field(m,key)).intValue();}
    public static void validate(Kind kind,Object m,AbstractContainerMenu menu){
        String name=m.getClass().getSimpleName();
        if(name.equals("CableDataMessage")||name.equals("CableIOMessage")||name.equals("CableProcessMessage")){
            int id=number(m,"id");var type=Arrays.stream(m.getClass().getDeclaredClasses()).filter(Class::isEnum).findFirst().orElseThrow();
            var values=type.getEnumConstants();if(id<0||id>=values.length)throw new IllegalArgumentException("Unknown cable operation");
            String operation=((Enum<?>)values[id]).name();int value=number(m,"value");
            if(operation.startsWith("SAVE_F")&&(value<0||value>=(kind==Kind.PROCESS?9:18)))throw new IllegalArgumentException("Unknown ghost entry");
            if(kind==Kind.COLLECTION&&!operation.equals("SAVE_FITLER"))throw new IllegalArgumentException("Collection filter action");
            if(operation.endsWith("FACE")&&(value<0||value>5))throw new IllegalArgumentException("Unknown face");
            if(operation.equals("SET_COUNT")&&value<0)throw new IllegalArgumentException("Negative processing count");
            if(operation.equals("SYNC_OP")&&(value<0||value>=3))throw new IllegalArgumentException("Unknown comparison");
        }
        if(name.equals("RequestMessage")||name.equals("InsertMessage")){
            int mouse=number(m,"mouseButton");if(mouse<0||mouse>1)throw new IllegalArgumentException("Unsupported mouse button");
        }
        if(name.equals("SettingsSyncMessage")){
            Object target=SsnWindows.optionalField(m,"targetTileEntity");
            if(Boolean.TRUE.equals(target)&&(kind.remote||!SsnWindows.position(menu).equals(SsnWindows.field(m,"pos"))))throw new IllegalArgumentException("Settings target does not own this menu");
        }
    }
    public static Object newRefresh(){try{return Class.forName(PREFIX+"RequestMessage").getConstructor().newInstance();}catch(ReflectiveOperationException e){throw new IllegalStateException(e);}}
    public static byte[] encode(Object m,Player player){return(byte[])transport("encode",m,player);}
    public static Object decode(byte[] bytes,Player player){return transport("decode",bytes,player);}
    public static void receiveServer(ServerPlayer player,Object message){transport("receive",message,player,true);}
    public static void receiveClient(AbstractContainerScreen<?> screen,byte[] bytes){
        var mc=Minecraft.getInstance();if(mc.player==null)return;
        Object message=decode(bytes,mc.player);if(!clientMessage(message))throw new IllegalArgumentException("Wrong SSN update direction");
        var previous=mc.screen;var menu=mc.player.containerMenu;mc.screen=screen;mc.player.containerMenu=screen.getMenu();
        try{transport("receive",message,mc.player,false);}finally{mc.player.containerMenu=menu;mc.screen=previous;}
    }
    private static Object transport(String name,Object...args){
        try{
            var type=Class.forName("com.salts_inventory_update.compat.ssn.SsnTransport");
            for(var m:type.getMethods())if(m.getName().equals(name)&&m.getParameterCount()==args.length)return m.invoke(null,args);
            throw new NoSuchMethodException(name);
        }catch(InvocationTargetException failure){if(failure.getCause()instanceof RuntimeException r)throw r;throw new IllegalStateException(name,failure.getCause());}
        catch(ReflectiveOperationException failure){throw new IllegalStateException("SSN loader transport: "+name,failure);}
    }
}
