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
    static int left(Object screen){return ((com.salts_inventory_update.compat.nativewindows.mixin.NativeScreenGeometryAccess)screen).salts_inventory_compat$left();}
    static int top(Object screen){return ((com.salts_inventory_update.compat.nativewindows.mixin.NativeScreenGeometryAccess)screen).salts_inventory_compat$top();}
    static int number(Object o,String method){return ((Number)call(o,method)).intValue();}
    static int integer(Object o,String field){return ((Number)field(o,field)).intValue();}
    private static final java.util.Map<String,net.minecraft.core.component.DataComponentType<Object>> COMPONENTS=new java.util.HashMap<>();
    @SuppressWarnings("unchecked")
    static net.minecraft.core.component.DataComponentType<Object> component(String name){
        return COMPONENTS.computeIfAbsent(name,key->{try{
            Object value=Class.forName("com.tiviacz.travelersbackpack.init.ModDataComponents").getField(key).get(null);
            if(value instanceof java.util.function.Supplier<?> supplier)value=supplier.get();
            return (net.minecraft.core.component.DataComponentType<Object>)value;
        }catch(ReflectiveOperationException failure){throw new IllegalStateException("Traveler's component "+key,failure);}});
    }
    static ItemStack presentation(ItemStack stack){
        ItemStack copy=stack.copy();copy.remove(component("BACKPACK_CONTAINER"));copy.remove(component("TOOLS_CONTAINER"));return copy;
    }
    static void retainContents(ItemStack copy,ItemStack old){
        for(String name:java.util.List.of("BACKPACK_CONTAINER","TOOLS_CONTAINER")){
            var type=component(name);Object value=old.get(type);if(value!=null)copy.set(type,value);else copy.remove(type);
        }
    }
    static boolean structureChanged(ItemStack old,ItemStack next){
        for(String name:java.util.List.of("UPGRADES","TIER","STORAGE_SLOTS","TOOL_SLOTS","UPGRADE_SLOTS","SHOW_TOOL_SLOTS","SHOW_MORE_BUTTONS","TAB_OPEN"))
            if(!java.util.Objects.equals(old.get(component(name)),next.get(component(name))))return true;
        return false;
    }
    static Object wrapper(AbstractContainerMenu menu){return ForeignMenuAccess.invoke(menu,"getWrapper");}
    static ItemStack stack(AbstractContainerMenu menu){return (ItemStack)call(wrapper(menu),"getBackpackStack");}
    static boolean settings(AbstractContainerMenu m){return m.getClass().getSimpleName().equals("BackpackSettingsMenu");}
}
