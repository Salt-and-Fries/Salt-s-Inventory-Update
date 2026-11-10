package com.salts_inventory_update.compat.aether;

import java.lang.reflect.*;
import java.util.*;
import com.salts_inventory_update.api.integration.DesktopEnvironment;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.*;

/** Optional native ABI only; mapped Minecraft methods are called through their Java types. */
public final class AetherAccess {
    private AetherAccess() { }
    public static boolean fabric(){return DesktopEnvironment.isModLoaded("fabricloader");}
    static Class<?> type(String name){try{return Class.forName(name);}catch(ClassNotFoundException e){throw new IllegalStateException(e);}}
    static Object field(Object owner,String name){
        for(Class<?> t=owner instanceof Class<?> c?c:owner.getClass();t!=null;t=t.getSuperclass())try{var f=t.getDeclaredField(name);f.setAccessible(true);return f.get(owner instanceof Class<?>?null:owner);}catch(NoSuchFieldException absent){}catch(ReflectiveOperationException e){throw new IllegalStateException(name,e);}
        throw new IllegalStateException("Missing Aether field "+name);
    }
    static Object call(Object owner,String name,Object...args){
        for(var m:(owner instanceof Class<?> c?c:owner.getClass()).getMethods())if(m.getName().equals(name)&&m.getParameterCount()==args.length)try{m.trySetAccessible();return m.invoke(owner instanceof Class<?>?null:owner,args);}catch(IllegalArgumentException overload){}catch(ReflectiveOperationException e){throw new IllegalStateException(name,e);}
        throw new IllegalStateException("Missing Aether method "+name);
    }
    static AbstractContainerMenu menu(String name,int id,Inventory inventory){try{return (AbstractContainerMenu)type(name).getConstructor(int.class,Inventory.class).newInstance(id,inventory);}catch(ReflectiveOperationException e){throw new IllegalStateException("Aether menu "+name,e);}}
    static List<Slot> equipment(Inventory inventory){
        var menu=menu("com.aetherteam.aether.inventory.menu.AccessoriesMenu",0,inventory);
        return menu.slots.stream().filter(s->s.index>=46).toList();
    }
    static String identifier(Slot slot){return fabric()?(String)call(call(field(slot,"accessoriesContainer"),"slotType"),"name"):(String)call(slot,"getIdentifier");}
    static Object backing(Slot slot){return fabric()?call(field(slot,"accessoriesContainer"),"getAccessories"):call(slot,"getItemHandler");}
    static boolean toggleable(Slot slot){return fabric()||(Boolean)call(slot,"canToggleRender");}
    static boolean render(Slot slot){return fabric()?(Boolean)call(field(slot,"accessoriesContainer"),"shouldRender",slot.getContainerSlot()):(Boolean)call(slot,"getRenderStatus");}
    static void toggle(Slot slot,net.minecraft.server.level.ServerPlayer player){
        if(!toggleable(slot))return;
        if(fabric())try{
            Object packet=type("io.wispforest.accessories.networking.server.SyncCosmeticToggle").getConstructor(Integer.class,String.class,int.class).newInstance(null,identifier(slot),slot.getContainerSlot());call(packet,"handle",player);
        }catch(ReflectiveOperationException e){throw new IllegalStateException(e);}
        else call(type("com.salts_inventory_update.compat.aether.forge.AetherForgeTransport"),"toggle",slot,player);
    }
    static List<Object> topology(List<Slot> slots){var out=new ArrayList<Object>();for(var slot:slots){out.add(identifier(slot));out.add(slot.getContainerSlot());out.add(backing(slot));}return out;}
    static String loader(){return fabric()?"fabric.AetherFabricTransport":"forge.AetherForgeTransport";}
    static void transport(String method,Object...args){call(type("com.salts_inventory_update.compat.aether."+loader()),method,args);}
}
