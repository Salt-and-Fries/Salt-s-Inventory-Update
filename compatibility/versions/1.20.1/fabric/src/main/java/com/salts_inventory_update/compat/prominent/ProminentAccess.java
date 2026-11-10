package com.salts_inventory_update.compat.prominent;

import com.salts_inventory_update.compat.nativewindows.ForeignMenuAccess;
import java.lang.ref.WeakReference;
import java.util.*;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;

/** Access only to the optional mod/owo objects owned by this integration. */
public final class ProminentAccess {
    private static final Map<AbstractContainerMenu,WeakReference<AbstractContainerScreen<?>>> SCREENS=Collections.synchronizedMap(new WeakHashMap<>());
    private static final Set<Object> PREPARED=Collections.newSetFromMap(new WeakHashMap<>());
    private static final Map<Object,Long> ARMOR_SIZES=new WeakHashMap<>();
    private ProminentAccess() { }
    static void remember(AbstractContainerMenu menu,AbstractContainerScreen<?> screen){SCREENS.put(menu,new WeakReference<>(screen));}
    public static AbstractContainerScreen<?> screen(AbstractContainerMenu menu){var ref=SCREENS.get(menu);return ref==null?null:ref.get();}
    static void forget(AbstractContainerMenu menu){SCREENS.remove(menu);}
    public static Object field(Object owner,String name){
        if(owner==null)return null;
        for(Class<?> t=owner.getClass();t!=null;t=t.getSuperclass())try{var f=t.getDeclaredField(name);f.setAccessible(true);return f.get(owner);}
        catch(NoSuchFieldException next){}catch(ReflectiveOperationException failure){throw new IllegalStateException(name,failure);}
        return null;
    }
    static void attach(AbstractContainerMenu menu,Player player){
        ForeignMenuAccess.invoke(menu,"owo$attachToPlayer",new Class<?>[]{Player.class},player);
    }
    static void prepare(AbstractContainerMenu menu){
        var screen=screen(menu);
        Object adapter=field(screen,"uiAdapter"),root=field(adapter,"rootComponent");
        if(root==null)return;
        try{
            if(PREPARED.add(root)){
                Class<?> surface=Class.forName("io.wispforest.owo.ui.core.Surface");
                ForeignMenuAccess.invoke(root,"surface",new Class<?>[]{surface},surface.getField("BLANK").get(null));
            }
            if(!menu.getClass().getName().equals(ProminentWindows.Kind.ARMOR.menu))return;
            long size=((long)screen.width<<32)|Integer.toUnsignedLong(screen.height);
            if(Objects.equals(ARMOR_SIZES.get(root),size))return;
            // Upstream computes these two absolute positions only in build().
            // Owo resizes the root later without rebuilding those components.
            Class<?> positioning=Class.forName("io.wispforest.owo.ui.core.Positioning");
            for(Object child:(List<?>)ForeignMenuAccess.invoke(root,"children")){
                boolean craft=child.getClass().getName().equals(ProminentWindows.Kind.ARMOR.screen+"$CraftButton");
                boolean pieces=child.getClass().getName().equals("io.wispforest.owo.ui.container.FlowLayout")
                    &&(int)ForeignMenuAccess.invoke(child,"width")==160&&(int)ForeignMenuAccess.invoke(child,"height")==128;
                if(!craft&&!pieces)continue;
                Object position=positioning.getMethod("absolute",int.class,int.class).invoke(null,screen.width/2-80,screen.height/2-(pieces?112:0));
                ForeignMenuAccess.invoke(child,"positioning",new Class<?>[]{positioning},position);
            }
            ARMOR_SIZES.put(root,size);
        }catch(ReflectiveOperationException failure){throw new IllegalStateException("Native forge surface",failure);}
    }
    static int cinders(Player player){
        try{return (int)Class.forName("elocindev.prominentbase.data.cinders.CindersHelper").getMethod("getCinders",Player.class).invoke(null,player);}
        catch(ReflectiveOperationException failure){throw new IllegalStateException("Native forge currency",failure);}
    }
}
