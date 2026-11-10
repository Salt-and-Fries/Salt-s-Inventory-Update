package com.salts_inventory_update.compat.travelersbackpack;

import com.salts_inventory_update.compat.nativewindows.ForeignMenuAccess;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

/** Access is limited to the optional mod's own stable public methods/fields. */
public final class TravelersAccess {
    private TravelersAccess() { }
    static Object call(Object owner,String name,Object... args){
        for(Class<?> c=owner.getClass();c!=null;c=c.getSuperclass())for(var m:c.getDeclaredMethods())if(m.getName().equals(name)&&m.getParameterCount()==args.length){
            try{m.setAccessible(true);return m.invoke(owner,args);}catch(IllegalArgumentException mismatch){}catch(ReflectiveOperationException failure){throw new IllegalStateException(name,failure);}
        }
        throw new IllegalStateException("Traveler's method missing: "+name);
    }
    public static Object field(Object owner,String name){
        for(Class<?> c=owner.getClass();c!=null;c=c.getSuperclass())try{var f=c.getDeclaredField(name);f.setAccessible(true);return f.get(owner);}
        catch(NoSuchFieldException absent){}catch(ReflectiveOperationException failure){throw new IllegalStateException(name,failure);}
        throw new IllegalStateException("Traveler's field missing: "+name);
    }
    static int number(Object o,String method){return ((Number)call(o,method)).intValue();}
    static int integer(Object o,String field){return ((Number)field(o,field)).intValue();}
    static Object wrapper(AbstractContainerMenu menu){return ForeignMenuAccess.invoke(menu,"getWrapper");}
    static ItemStack stack(AbstractContainerMenu menu){return (ItemStack)call(wrapper(menu),"getBackpackStack");}
    static boolean settings(AbstractContainerMenu m){return m.getClass().getSimpleName().equals("BackpackSettingsMenu");}
}
