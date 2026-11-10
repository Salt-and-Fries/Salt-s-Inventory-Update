package com.salts_inventory_update.compat.accessories;

import java.lang.reflect.*;
import java.util.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Optional upstream ABI. Vanilla methods are called through their mapped Java types. */
final class AccessoriesAccess {
    private AccessoriesAccess() { }
    static Object call(Object owner,String name,Object...args){
        Class<?> type=owner instanceof Class<?> c?c:owner.getClass();
        for(var m:type.getMethods())if(m.getName().equals(name)&&m.getParameterCount()==args.length&&(!(owner instanceof Class<?>)||Modifier.isStatic(m.getModifiers())))try{m.trySetAccessible();return m.invoke(owner instanceof Class<?>?null:owner,args);}
        catch(IllegalArgumentException wrongOverload){}catch(ReflectiveOperationException failure){throw new IllegalStateException("Accessories "+name,failure);}
        throw new IllegalStateException("Accessories method missing: "+name);
    }
    static Class<?> type(String name){try{return Class.forName("io.wispforest.accessories."+name);}catch(ClassNotFoundException e){throw new IllegalStateException(e);}}
    static Object capability(Player player){return call(type("api.AccessoriesCapability"),"get",player);}
    @SuppressWarnings("unchecked") static Map<String,Object> containers(Player player){Object cap=capability(player);return cap==null?Map.of():(Map<String,Object>)call(cap,"getContainers");}
    static Object reference(Player player,String name,int index){return call(type("api.slot.SlotReference"),"of",player,name,index);}
    static boolean insert(Player player,String name,int index,ItemStack stack){return (Boolean)call(type("api.AccessoriesAPI"),"canInsertIntoSlot",stack,reference(player,name,index));}
    static boolean remove(Player player,String name,int index,ItemStack stack){return (Boolean)call(type("api.AccessoriesAPI"),"canUnequip",stack,reference(player,name,index));}
    static int maximum(ItemStack stack){return ((Number)call(call(type("api.AccessoriesAPI"),"getOrDefaultAccessory",stack),"maxStackSize",stack)).intValue();}
    static Slot nativeSlot(Object container,int index,boolean cosmetic,java.util.function.Function<Object,Boolean> active){
        try{
            Object slot=type("client.gui.AccessoriesInternalSlot").getConstructor(int.class,type("api.AccessoriesContainer"),boolean.class,int.class,int.class,int.class)
                .newInstance(index,container,cosmetic,index,0,0);
            call(slot,"isActive",active);call(slot,"isAccessible",active);return (Slot)slot;
        }catch(ReflectiveOperationException failure){throw new IllegalStateException("Accessories native slot construction",failure);}
    }
}
