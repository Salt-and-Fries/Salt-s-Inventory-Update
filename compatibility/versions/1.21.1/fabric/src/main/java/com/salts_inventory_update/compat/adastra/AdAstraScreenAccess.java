package com.salts_inventory_update.compat.adastra;

import com.salts_inventory_update.compat.nativewindows.mixin.AdAstraScreenGeometryAccessor;
import java.lang.ref.WeakReference;
import java.util.*;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import com.salts_inventory_update.api.client.desktop.DesktopWindowBounds;
import net.minecraft.client.gui.components.events.ContainerEventHandler;

/** Upstream-owned widget state only; no Core implementation is accessed. */
public final class AdAstraScreenAccess {
    private static final Map<AbstractContainerMenu,WeakReference<AbstractContainerScreen<?>>> SCREENS=new WeakHashMap<>();
    private AdAstraScreenAccess() { }
    public static void remember(Object screen){
        var nativeScreen=(AbstractContainerScreen<?>)screen;
        SCREENS.put(nativeScreen.getMenu(),new WeakReference<>(nativeScreen));
    }
    static Object screen(AbstractContainerMenu menu){var ref=SCREENS.get(menu);return ref==null?null:ref.get();}
    static Object invoke(Object owner,String name,Object...args){
        if(owner==null)throw new IllegalStateException("Missing native Ad Astra screen");
        for(Class<?> t=owner.getClass();t!=null;t=t.getSuperclass())for(var m:t.getDeclaredMethods())
            if(m.getName().equals(name)&&m.getParameterCount()==args.length)try{m.setAccessible(true);return m.invoke(owner,args);}
            catch(IllegalArgumentException overload){}catch(ReflectiveOperationException error){throw new IllegalStateException(name,error);}
        throw new IllegalStateException("Missing native Ad Astra method "+name);
    }
    private static boolean set(Object owner,String name,int value){
        for(Class<?> t=owner.getClass();t!=null;t=t.getSuperclass())try{
            var f=t.getDeclaredField(name);f.setAccessible(true);boolean changed=f.getInt(owner)!=value;f.setInt(owner,value);return changed;
        }catch(NoSuchFieldException next){}catch(ReflectiveOperationException error){throw new IllegalStateException(name,error);}
        throw new IllegalStateException(name);
    }
    static void prepare(AbstractContainerMenu menu,AdAstraSpec spec){
        Object screen=screen(menu);if(screen==null)return;Object side=invoke(screen,"getSideConfigWidget");
        // These invisible port selectors reject clicks outside Gear mode in
        // upstream isValidClickButton. Reflect that real availability in the
        // public widget snapshot so they do not occlude ordinary item slots.
        boolean configuring=((net.minecraft.client.gui.narration.NarratableEntry)side).isActive();
        for(var child:((AbstractContainerScreen<?>)screen).children())
            if(child instanceof AbstractWidget widget&&child.getClass().getName().endsWith(".SlotWidget"))widget.active=configuring;
        if(!spec.menu.equals("ad_astra_giselle_addon:rocket_sensor"))return;
        int left=((Number)invoke(screen,"leftPos")).intValue(),top=((Number)invoke(screen,"topPos")).intValue();
        boolean changed=set(side,"width",162)|set(side,"height",76)|set(side,"x",left+7)|set(side,"y",top+spec.textureHeight+20);
        ((AdAstraScreenGeometryAccessor)screen).siu$inventoryLabelY(spec.textureHeight+9);
        if(changed)invoke(side,"init");
    }
    static void select(AbstractContainerMenu menu,int x,int y){
        for(Object configuration:(List<?>)invoke(menu,"getConfigurations")){
            if(((Number)invoke(configuration,"x")).intValue()!=x||((Number)invoke(configuration,"y")).intValue()!=y)continue;
            invoke(invoke(screen(menu),"getSideConfigWidget"),"setIndex",((Number)invoke(configuration,"index")).intValue());return;
        }
        throw new IllegalStateException("Unmatched native configuration port");
    }
    static Component configurationTitle(AbstractContainerMenu menu){return (Component)invoke(invoke(screen(menu),"getSideConfigWidget"),"getMessage");}
    static boolean configuring(AbstractContainerMenu menu){
        Object screen=screen(menu);return screen!=null&&((net.minecraft.client.gui.narration.NarratableEntry)invoke(screen,"getSideConfigWidget")).isActive();
    }
    static List<DesktopWindowBounds> configurationCells(AbstractContainerMenu menu){
        Object screen=screen(menu);int left=((Number)invoke(screen,"leftPos")).intValue(),top=((Number)invoke(screen,"topPos")).intValue();
        var cells=new ArrayList<DesktopWindowBounds>();
        for(var child:((ContainerEventHandler)invoke(screen,"getSideConfigWidget")).children())
            if(child instanceof AbstractWidget w&&w.visible&&w.active)cells.add(new DesktopWindowBounds(w.getX()-left,w.getY()-top,w.getWidth(),w.getHeight()));
        return List.copyOf(cells);
    }
}
