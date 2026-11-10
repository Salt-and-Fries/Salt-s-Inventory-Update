package com.salts_inventory_update.compat.ae2.nativeview;

import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.WidgetContainer;
import appeng.client.gui.style.WidgetStyle;
import java.util.Map;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.renderer.Rect2i;

/** ME Requester creates its row controls after AE2's ordinary style population. */
public final class AENativeWidgets {
    private AENativeWidgets() { }
    /** Called when the addon creates a row, before desktop placement changes its origin. */
    @SuppressWarnings("unchecked")
    public static void remember(Object nativeScreen,String name,Object nativeWidget) {
        if(!com.salts_inventory_update.compat.ae2.mixin.NativeDesktopPatchPlugin.active())return;
        var screen=(AEBaseScreen<?>)nativeScreen;var w=(AbstractWidget)nativeWidget;
        if(!screen.isDesktopHosted())return;
        var styles=(Map<String,WidgetStyle>)field(screen.getStyle(),"widgets");
        var style=new WidgetStyle();style.setLeft(w.getX()-screen.getGuiLeft());style.setTop(w.getY()-screen.getGuiTop());
        style.setWidth(w.getWidth());style.setHeight(w.getHeight());styles.put(name,style);
    }
    @SuppressWarnings("unchecked")
    public static void retainRuntimeRows(WidgetContainer container,Rect2i bounds,AEBaseScreen<?> screen) {
        if(!screen.getClass().getName().startsWith("com.almostreliable.merequester."))return;
        var widgets=(Map<String,AbstractWidget>)field(container,"widgets");
        var styles=(Map<String,WidgetStyle>)field(screen.getStyle(),"widgets");
        for(var entry:widgets.entrySet())if(!styles.containsKey(entry.getKey())){
            var w=entry.getValue();var style=new WidgetStyle();
            style.setLeft(w.getX()-screen.getGuiLeft());style.setTop(w.getY()-screen.getGuiTop());
            style.setWidth(w.getWidth());style.setHeight(w.getHeight());styles.put(entry.getKey(),style);
        }
    }
    private static Object field(Object owner,String name){
        for(Class<?> t=owner.getClass();t!=null;t=t.getSuperclass())try{
            var field=t.getDeclaredField(name);field.setAccessible(true);return field.get(owner);
        }catch(NoSuchFieldException next){}catch(ReflectiveOperationException failure){throw new IllegalStateException(failure);}
        throw new IllegalStateException("Native AE widget field changed: "+name);
    }
}
