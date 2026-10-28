package com.salts_inventory_update.compat.nativewindows;

import java.lang.reflect.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.*;

/** Reflection is limited to the optional upstream mod's own classes, never Salt internals. */
public final class ForeignMenuAccess {
    private ForeignMenuAccess() { }
    public static Object invoke(Object target,String name) {
        return invoke(target,name,new Class<?>[0]);
    }
    public static Object invoke(Object target,String name,Class<?>[] parameters,Object... arguments) {
        for(Class<?> type=target.getClass();type!=null;type=type.getSuperclass())try{
            var method=type.getDeclaredMethod(name,parameters);method.setAccessible(true);return method.invoke(target,arguments);
        }catch(NoSuchMethodException ignored){}catch(ReflectiveOperationException failure){throw new IllegalStateException(name,failure);}
        throw new IllegalStateException("Upstream method missing: "+target.getClass().getName()+"."+name);
    }
    public static boolean flag(Object target,String name) {
        try{var field=target.getClass().getDeclaredField(name);field.setAccessible(true);return field.getBoolean(target);}
        catch(ReflectiveOperationException failure){throw new IllegalStateException(name,failure);}
    }
    public static BlockPos position(AbstractContainerMenu menu) {
        Object value=invoke(menu,"getPos");return value instanceof BlockPos pos?pos:null;
    }
    public static AbstractContainerMenu create(String name,int id,Inventory inventory,ContainerLevelAccess access) {
        try{
            if(name.endsWith("SmitherScreenHandler")) {
                var config=Class.forName("net.smitherz.init.ConfigInit").getField("CONFIG").get(null);
                int count=config.getClass().getField("maxGemSlots").getInt(config);
                return (AbstractContainerMenu)Class.forName(name).getConstructor(int.class,Inventory.class,ContainerLevelAccess.class,int.class)
                    .newInstance(id,inventory,access,count);
            }
            return (AbstractContainerMenu)Class.forName(name).getConstructor(int.class,Inventory.class,ContainerLevelAccess.class)
            .newInstance(id,inventory,access);}
        catch(ReflectiveOperationException failure){throw new IllegalStateException("Upstream menu construction failed: "+name,failure);}
    }
}
