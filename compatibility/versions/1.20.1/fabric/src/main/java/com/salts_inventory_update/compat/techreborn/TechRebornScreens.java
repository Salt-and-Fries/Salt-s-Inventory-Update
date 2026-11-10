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
    static boolean key(DesktopInputContext<?,?> c,Object tab,int key,int scan,int modifiers){INPUT.set(c);try{return(Boolean)RebornAccess.call(tab,"keyPress",key,scan,modifiers);}finally{INPUT.remove();}}
    public static boolean send(Object packet){var c=INPUT.get();if(c==null)return false;
        var data=(net.minecraft.network.FriendlyByteBuf)RebornAccess.call(packet,"packetByteBuf");
        try{byte[] bytes=new byte[data.readableBytes()];data.getBytes(data.readerIndex(),bytes);c.sendPayload((net.minecraft.resources.ResourceLocation)RebornAccess.call(packet,"channel"),bytes);}finally{data.release();}return true;
    }
    public static AbstractContainerMenu constructionMenu(AbstractContainerMenu original){var menu=CONSTRUCTING.get();return menu==null?original:menu;}
    public static AbstractContainerScreen<?> create(AbstractContainerMenu menu,net.minecraft.world.entity.player.Inventory inventory,net.minecraft.network.chat.Component title){
        try{
            Object types=RebornAccess.type("techreborn.client.ClientGuiType").getField("TYPES").get(null);
            Object record=((Map<?,?>)types).get(net.minecraft.core.registries.BuiltInRegistries.MENU.getKey(menu.getType()));
            Object factory=RebornAccess.call(record,"guiFactory");
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
    static int tabs(AbstractContainerMenu menu){Object tabs=RebornAccess.field(screen(menu),"tabs");return tabs instanceof List<?> list?list.size():0;}
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
    private static void translate(Object element,int dx,int dy){
        RebornAccess.set(element,"x",(Integer)RebornAccess.call(element,"getX")+dx);
        RebornAccess.set(element,"y",(Integer)RebornAccess.call(element,"getY")+dy);
        Object children=RebornAccess.field(element,"elements");if(children instanceof List<?> list)for(Object child:list)translate(child,dx,dy);
    }
}
