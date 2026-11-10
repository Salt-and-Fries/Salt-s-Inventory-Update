package com.salts_inventory_update.compat.quark;

import java.lang.reflect.*;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Reflection is restricted to Quark's optional classes, never SIU internals. */
public final class QuarkAccess {
    private QuarkAccess() { }
    public static Class<?> type(String name) {
        try { return Class.forName("org.violetmoon.quark."+name); }
        catch(ClassNotFoundException e) { throw new IllegalStateException(e); }
    }
    public static Object field(Object owner,String name) {
        for(Class<?> t=owner instanceof Class<?> c?c:owner.getClass();t!=null;t=t.getSuperclass())try {
            var f=t.getDeclaredField(name);f.setAccessible(true);return f.get(owner instanceof Class<?>?null:owner);
        } catch(NoSuchFieldException absent) { } catch(ReflectiveOperationException e) { throw new IllegalStateException(name,e); }
        throw new IllegalStateException("Missing Quark field "+name);
    }
    public static Object call(Object owner,String name,Object...args) {
        for(Class<?> t=owner instanceof Class<?> c?c:owner.getClass();t!=null;t=t.getSuperclass())for(var m:t.getDeclaredMethods()) {
            if(!m.getName().equals(name)||m.getParameterCount()!=args.length)continue;
            try { m.setAccessible(true);return m.invoke(owner instanceof Class<?>?null:owner,args); }
            catch(IllegalArgumentException overload) { } catch(ReflectiveOperationException e) { throw new IllegalStateException(name,e); }
        }
        throw new IllegalStateException("Missing Quark method "+name);
    }
    public static boolean wearing(Player p) {
        return p!=null&&p.getItemBySlot(EquipmentSlot.CHEST).getItem().getClass().getName().equals("org.violetmoon.quark.addons.oddities.item.BackpackItem");
    }
    static Object backpackInventory(ItemStack bag) {
        try { return type("base.util.InventoryIIH").getConstructor(ItemStack.class).newInstance(bag); }
        catch(ReflectiveOperationException e) { throw new IllegalStateException(e); }
    }
    static Slot backpackSlot(Object inventory,int index) {
        try {
            return (Slot)type("addons.oddities.inventory.slot.BackpackSlot").getConstructors()[0].newInstance(inventory,index,0,0);
        } catch(ReflectiveOperationException e) { throw new IllegalStateException(e); }
    }
}
