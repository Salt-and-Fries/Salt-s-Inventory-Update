package com.salts_inventory_update.compat.bumblezone;

import java.lang.ref.WeakReference;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.AbstractContainerMenu;

/** Routes the upstream list update to its actual hosted menu, including the
 * constructor's update which can arrive just before the desktop open packet. */
public final class BumblezoneFlowerPackets {
    private record Pending(Object packet,long created) { }
    private static final Map<Integer,WeakReference<AbstractContainerMenu>> MENUS=new HashMap<>();
    private static final Map<Integer,Pending> PENDING=new LinkedHashMap<>();
    private static final Map<AbstractContainerMenu,Map<ResourceLocation,Object>> LISTS=new WeakHashMap<>();
    private BumblezoneFlowerPackets() { }
    static void open(AbstractContainerMenu menu) {
        MENUS.put(menu.containerId,new WeakReference<>(menu));
        var pending=PENDING.remove(menu.containerId);
        if(pending!=null&&System.nanoTime()-pending.created()<5_000_000_000L)apply(menu,pending.packet());
    }
    static void close(AbstractContainerMenu menu) {
        var ref=MENUS.get(menu.containerId);
        if(ref!=null&&ref.get()==menu)MENUS.remove(menu.containerId);
        PENDING.remove(menu.containerId);LISTS.remove(menu);
    }
    /** Called on the client thread by the original packet's scheduled handler. */
    public static boolean receive(Object packet) {
        try {
            int id=(Integer)packet.getClass().getMethod("containerId").invoke(packet);
            var player=Minecraft.getInstance().player;if(player==null)return false;
            // Leave an ordinary native menu completely in its upstream handler.
            if(player.containerMenu.containerId==id&&player.containerMenu.getClass().getName().endsWith(".CrystallineFlowerMenu"))return false;
            var ref=MENUS.get(id);var menu=ref==null?null:ref.get();
            if(menu!=null){apply(menu,packet);return true;}
            long now=System.nanoTime();PENDING.values().removeIf(p->now-p.created()>5_000_000_000L);
            if(PENDING.size()>=16)PENDING.remove(PENDING.keySet().iterator().next());
            PENDING.put(id,new Pending(packet,now));return false;
        }catch(ReflectiveOperationException failure){throw new IllegalStateException("Bumblezone enchantment update",failure);}
    }
    private static void apply(AbstractContainerMenu menu,Object packet) {
        try {
            var map=new HashMap<ResourceLocation,Object>();
            for(var skeleton:(List<?>)packet.getClass().getMethod("enchantmentSkeletons").invoke(packet)) {
                var t=skeleton.getClass();map.put(new ResourceLocation((String)t.getField("namespace").get(skeleton),(String)t.getField("path").get(skeleton)),skeleton);
            }
            LISTS.put(menu,map);
            var selected=(ResourceLocation)packet.getClass().getMethod("selectedResourceLocation").invoke(packet);
            if(selected.equals(new ResourceLocation("minecraft","empty"))||!map.containsKey(selected))selected=map.keySet().stream().findFirst().orElse(null);
            menu.getClass().getField("selectedEnchantment").set(menu,selected);restore(menu);
        }catch(ReflectiveOperationException failure){throw new IllegalStateException("Bumblezone enchantment contents",failure);}
    }
    static void restore(AbstractContainerMenu menu) {
        var list=LISTS.get(menu);if(list==null)return;
        try {
            var screen=Class.forName("com.telepathicgrunt.the_bumblezone.client.screens.CrystallineFlowerScreen");
            screen.getField("enchantmentsAvailable").set(null,list);
            screen.getMethod("SortAndAssignAvailableEnchants").invoke(null);
        }catch(ReflectiveOperationException failure){throw new IllegalStateException("Bumblezone enchantment viewport",failure);}
    }
}
