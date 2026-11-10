package com.salts_inventory_update.compat.aircraft;

import java.lang.reflect.*;
import java.util.*;
import net.minecraft.world.entity.Entity;

/** Optional upstream ABI, limited to the audited Aircraft release. */
final class AircraftAccess {
    private AircraftAccess() { }
    static Object call(Object owner,String name,Object...args){
        Class<?> type=owner instanceof Class<?> c?c:owner.getClass();
        for(var m:type.getMethods())if(m.getName().equals(name)&&m.getParameterCount()==args.length&&(!(owner instanceof Class<?>)||Modifier.isStatic(m.getModifiers())))try{m.trySetAccessible();return m.invoke(owner instanceof Class<?>?null:owner,args);}
        catch(IllegalArgumentException overload){}catch(ReflectiveOperationException failure){throw new IllegalStateException("Aircraft "+name,failure);}
        throw new IllegalStateException("Aircraft method missing: "+name);
    }
    static int number(Object owner,String name){return ((Number)call(owner,name)).intValue();}
    static boolean supported(){
        try {
            var vehicle=Class.forName("immersive_aircraft.entity.InventoryVehicleEntity");
            vehicle.getMethod("getInventory");var description=vehicle.getMethod("getInventoryDescription").getReturnType();
            description.getMethod("getSlots");
            var slot=Class.forName("immersive_aircraft.entity.inventory.slots.SlotDescription");
            for(String name:List.of("type","index","x","y"))slot.getMethod(name);
            return true;
        }catch(ReflectiveOperationException unsupported){return false;}
    }
    @SuppressWarnings("unchecked") static List<Object> descriptions(Entity vehicle){return (List<Object>)call(call(vehicle,"getInventoryDescription"),"getSlots");}
}
