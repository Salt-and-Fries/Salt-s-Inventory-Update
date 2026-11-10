package com.salts_inventory_update.compat.techreborn;

import com.salts_inventory_update.api.client.desktop.*;
import java.lang.ref.WeakReference;
import java.util.*;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;

/** Geometry changes only while the original publisher screen is hosted by SIU. */
public final class TechRebornScreens {
    private static final Map<AbstractContainerMenu,WeakReference<AbstractContainerScreen<?>>> SCREENS=new WeakHashMap<>();
    private static final Set<AbstractContainerMenu> OPEN=Collections.newSetFromMap(new WeakHashMap<>());
    private static final ThreadLocal<AbstractContainerMenu> CONSTRUCTING=new ThreadLocal<>();
    private static final ThreadLocal<DesktopInputContext<?,?>> INPUT=new ThreadLocal<>();
    private TechRebornScreens() { }
    static boolean key(DesktopInputContext<?,?> c,Object tab,int key,int scan,int modifiers){INPUT.set(c);try{return(Boolean)RebornAccess.call(tab,"keyPress",new net.minecraft.client.input.KeyEvent(key,scan,modifiers));}finally{INPUT.remove();}}
    public static boolean send(net.minecraft.network.protocol.common.custom.CustomPacketPayload packet){var c=INPUT.get();if(c==null)return false;
        return c.sendPayload(packet.type().id(),com.salts_inventory_update.compat.nativewindows.NativePayloads.bytes(packet,c.minecraft().player.registryAccess()));
    }
    public static AbstractContainerMenu constructionMenu(AbstractContainerMenu original){var menu=CONSTRUCTING.get();return menu==null?original:menu;}
    public static AbstractContainerScreen<?> create(AbstractContainerMenu menu,net.minecraft.world.entity.player.Inventory inventory,net.minecraft.network.chat.Component title){
        try{
            Class<?> types=RebornAccess.type("techreborn.client.ClientGuiType");Object factory=null;
            for(var field:types.getFields())if(java.lang.reflect.Modifier.isStatic(field.getModifiers())&&field.getType()==types){
                Object record=field.get(null),guiType=RebornAccess.call(record,"guiType");
                if(RebornAccess.call(guiType,"getScreenHandlerType")==menu.getType()){factory=RebornAccess.call(record,"guiFactory");break;}
            }
            if(factory==null)throw new IllegalStateException("Missing original Reborn screen factory");
            CONSTRUCTING.set(menu);
            try{return(AbstractContainerScreen<?>)RebornAccess.call(factory,"create",menu.containerId,inventory.player,TechRebornWindows.block(menu));}finally{CONSTRUCTING.remove();}
        }catch(ReflectiveOperationException failure){throw new IllegalStateException("Tech Reborn native screen factory",failure);}
    }
    public static void remember(AbstractContainerScreen<?> screen){SCREENS.put(screen.getMenu(),new WeakReference<>(screen));}
    static Object screen(AbstractContainerMenu menu){var ref=SCREENS.get(menu);return ref==null?null:ref.get();}
    static void open(AbstractContainerMenu menu){OPEN.add(menu);}
    public static boolean hosted(AbstractContainerMenu menu){return OPEN.contains(menu);}
    static void close(AbstractContainerMenu menu){OPEN.remove(menu);SCREENS.remove(menu);}
    public static boolean capturing(){return DesktopNativeRenderTarget.current().isPresent();}
    static Object tab(AbstractContainerMenu menu){return RebornAccess.field(screen(menu),"selectedTab");}
    static String tabName(AbstractContainerMenu menu){Object tab=tab(menu);return tab==null?"":tab.getClass().getSimpleName();}
    static boolean upgrades(AbstractContainerMenu menu){
        Object screen=screen(menu);if(screen==null)return false;
        return Boolean.TRUE.equals(RebornAccess.call(screen,"tryAddUpgrades"))&&Boolean.TRUE.equals(RebornAccess.call(TechRebornWindows.block(menu),"canBeUpgraded"));
    }
    static int tabs(AbstractContainerMenu menu){Object tabs=RebornAccess.call(screen(menu),"getTabs");return tabs instanceof List<?> list?list.size():0;}
    static Object popup(AbstractContainerMenu menu){
        Object tab=tab(menu);if(tab==null)return null;
        return RebornAccess.field(tab,tab.getClass().getSimpleName().equals("FluidConfigGui")?"fluidConfigElement":"selectedSlot");
    }
    static void prepare(AbstractContainerMenu menu){
        OPEN.add(menu);Object screen=screen(menu),popup=popup(menu);if(screen==null||popup==null)return;
        // Reborn draws its inspector in the foreground's translated coordinates.
        // Move the whole native element tree, including every hit box, outside
        // the machine. The native compositor then attaches it as one panel.
        Object children=RebornAccess.field(popup,"elements");
        if(children instanceof List<?> list)list.removeIf(child->child.getClass().getSimpleName().equals("ButtonElement"));
        int left=(Integer)RebornAccess.call(screen,"getGuiLeft"),top=(Integer)RebornAccess.call(screen,"getGuiTop");
        int dx=230-left-(Integer)RebornAccess.call(popup,"getX"),dy=29-top-(Integer)RebornAccess.call(popup,"getY");
        if(dx!=0||dy!=0)translate(popup,dx,dy);
    }
    static void drawFaces(DesktopRenderContext<AbstractContainerMenu,?> c){
        Object element=face(popup(c.model()));if(element==null)return;
        int x=c.windowX()+c.windowWidth()+42,y=c.windowY()+50;
        int mouseX=c.mouseX()-x+204,mouseY=c.mouseY()-y+4;
        c.withClip(x,y,75,85,()->
            c.withTransform(x-204,y-4,1,()->RebornAccess.call(element,"draw",c.nativeGraphics(),screen(c.model()),mouseX,mouseY)));
    }
    private static Object face(Object element){
        if(element==null)return null;
        if(RebornAccess.type("reborncore.client.gui.config.elements.AbstractConfigPopupElement").isInstance(element))return element;
        Object children=RebornAccess.field(element,"elements");if(children instanceof List<?> list)for(Object child:list){Object found=face(child);if(found!=null)return found;}
        return null;
    }
    private static void translate(Object element,int dx,int dy){
        RebornAccess.set(element,"x",(Integer)RebornAccess.call(element,"getX")+dx);
        RebornAccess.set(element,"y",(Integer)RebornAccess.call(element,"getY")+dy);
        Object children=RebornAccess.field(element,"elements");if(children instanceof List<?> list)for(Object child:list)translate(child,dx,dy);
    }
}
