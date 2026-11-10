package com.salts_inventory_update.compat.create;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.renderer.Rect2i;
import com.salts_inventory_update.api.client.desktop.DesktopWindowBounds;
import com.salts_inventory_update.api.client.desktop.DesktopNativeView;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;

/** Access to upstream-owned state only. No Core implementation or synthetic menus. */
public final class CreateNativeAccess {
    private static final Map<AbstractContainerMenu,WeakReference<AbstractContainerScreen<?>>> SCREENS=new WeakHashMap<>();
    private static final Map<Object,Integer> HEIGHTS=new WeakHashMap<>();
    private static final Map<Object,String> STOCK_CLOSES=new WeakHashMap<>();
    private CreateNativeAccess() { }
    public static void register(AbstractContainerScreen<?> screen) { SCREENS.put(screen.getMenu(),new WeakReference<>(screen)); }
    static AbstractContainerScreen<?> screen(AbstractContainerMenu menu) { var ref=SCREENS.get(menu);return ref==null?null:ref.get(); }
    static void forget(AbstractContainerMenu menu) { var screen=screen(menu);SCREENS.remove(menu);if(screen!=null)HEIGHTS.remove(screen); }
    static void closeStock(com.salts_inventory_update.api.client.desktop.DesktopWindowContext<AbstractContainerMenu,?> c){
        var screen=screen(c.model());
        if(!(field(screen,"addressBox") instanceof EditBox address)
            ||!(field(screen,"hiddenCategories") instanceof Set<?> hidden)
            ||!(field(c.model(),"contentHolder") instanceof net.minecraft.world.level.block.entity.BlockEntity owner))return;
        var indices=new ArrayList<Integer>();for(Object value:hidden){
            if(!(value instanceof Integer index))throw new IllegalStateException("Unsupported native category index");indices.add(index);
        }
        Collections.sort(indices);
        var buffer=new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        try{
            buffer.writeBlockPos(owner.getBlockPos());buffer.writeUtf(address.getValue(),32767);buffer.writeVarInt(indices.size());
            for(int index:indices)buffer.writeVarInt(index);
            byte[] data=new byte[buffer.readableBytes()];buffer.readBytes(data);
            String value=Base64.getEncoder().encodeToString(data);
            if(value.length()>32767)throw new IllegalArgumentException("Native stock settings exceed text limit");
            STOCK_CLOSES.put(screen,value);
        }finally{buffer.release();}
    }
    public static void flushStockClose(Object screen){
        String value=STOCK_CLOSES.get(screen);if(value==null)return;
        var connection=net.minecraft.client.Minecraft.getInstance().getConnection();
        if(connection!=null)connection.send(new net.minecraft.network.protocol.game.ServerboundRenameItemPacket(value));
    }
    public static boolean suppressStockClose(Object screen){return STOCK_CLOSES.containsKey(screen);}
    public static void finishStockClose(Object screen){STOCK_CLOSES.remove(screen);}
    public static int compactHeight(Object screen,int original) { return Math.min(original,HEIGHTS.getOrDefault(screen,original)); }
    static void prepare(AbstractContainerMenu menu,net.minecraft.client.Minecraft mc) {
        var screen=screen(menu);
        if(screen!=null&&screen.getClass().getName().endsWith(".PackagePortScreen")&&field(screen,"addressBox") instanceof EditBox box)box.setWidth(110);
        if(screen!=null&&screen.getClass().getName().endsWith(".SchematicTableScreen")
            &&field(screen,"schematicsArea")!=null&&field(screen,"schematicsLabel") instanceof GuiEventListener label
            &&!screen.children().contains(label)){
            // Upstream's refresh-from-empty path creates the selector but omits
            // its filename label. Rebuild the original screen so both native
            // widgets are registered together, exactly as on a normal reopen.
            screen.resize(mc,mc.getWindow().getGuiScaledWidth(),mc.getWindow().getGuiScaledHeight());
        }
        if(screen!=null&&screen.getClass().getName().endsWith(".SchematicTableScreen")
            &&field(screen,"schematicsArea") instanceof AbstractWidget selector){
            Object label=field(screen,"schematicsLabel");
            if(field(label,"text") instanceof net.minecraft.network.chat.Component text
                &&mc.font.width(text)>selector.getWidth()-12){
                // Keep long display names inside the native field. Uploads still
                // use the selector's original, complete filename and file data.
                invoke(label,"setTextAndTrim",text,true,selector.getWidth()-12);
            }
        }
    }
    public static boolean textFocused(Screen screen) { return focused(screen,0); }
    private static boolean focused(GuiEventListener listener,int depth) {
        if(depth>8)return false;
        if(listener instanceof EditBox box)return box.visible&&box.isFocused();
        if(listener instanceof ContainerEventHandler parent) {
            var child=parent.getFocused();return child!=null&&focused(child,depth+1);
        }
        return false;
    }
    static Object field(Object owner,String name) {
        if(owner==null)return null;
        for(Class<?> t=owner.getClass();t!=null;t=t.getSuperclass())try {
            Field f=t.getDeclaredField(name);f.setAccessible(true);return f.get(owner);
        }catch(NoSuchFieldException absent){}catch(ReflectiveOperationException failure){throw new IllegalStateException(name,failure);}
        return null;
    }
    static Object invoke(Object owner,String name,Object...args) {
        if(owner==null)return null;
        for(Class<?> t=owner.getClass();t!=null;t=t.getSuperclass())for(Method m:t.getDeclaredMethods())if(m.getName().equals(name)&&m.getParameterCount()==args.length)try {
            m.setAccessible(true);return m.invoke(owner,args);
        }catch(IllegalArgumentException wrongOverload){}catch(ReflectiveOperationException failure){throw new IllegalStateException(name,failure);}
        return null;
    }
    static boolean editing(AbstractContainerMenu menu) {
        var s=screen(menu);return field(s,"editingItem")!=null||field(s,"editingDestination")!=null||field(s,"editingCondition")!=null;
    }
    static List<?> list(Object owner,String field) { var v=field(owner,field);return v instanceof List<?> l?l:List.of(); }
    static DesktopWindowBounds destinationPopup(Object owner,AbstractContainerScreen<?> screen,DesktopNativeView view) {
        if(!(owner instanceof CommandSuggestions)||screen==null)return null;
        // Minecraft maps private member names differently on each loader. The
        // original suggestions object and its original rectangle are identified
        // by type, so paint and pointer routing use exactly the same live bounds.
        try {
            for(Class<?> type=owner.getClass();type!=null;type=type.getSuperclass())for(var f:type.getDeclaredFields()) {
                if(f.getType().getEnclosingClass()!=CommandSuggestions.class)continue;
                f.setAccessible(true);Object popup=f.get(owner);if(popup==null)continue;
                for(var r:popup.getClass().getDeclaredFields())if(r.getType()==Rect2i.class) {
                    r.setAccessible(true);Rect2i bounds=(Rect2i)r.get(popup);if(bounds==null)return null;
                    // Pair the original widget's public screen coordinates with
                    // the public native view's relative coordinates. This also
                    // works when a loader renames the screen's origin getters.
                    var anchor=screen.children().stream().filter(AbstractWidget.class::isInstance).map(AbstractWidget.class::cast)
                        .filter(w->w.getWidth()>0&&w.getHeight()>0).findFirst().orElse(null);
                    if(anchor==null||view.widgets().isEmpty())return null;
                    var relative=view.widgets().get(0).bounds();int x=anchor.getX()-relative.x(),y=anchor.getY()-relative.y();
                    return new DesktopWindowBounds(bounds.getX()-x-1,bounds.getY()-y-1,bounds.getWidth()+2,bounds.getHeight()+2);
                }
            }
        }catch(ReflectiveOperationException failure){throw new IllegalStateException("Native destination suggestions",failure);}
        return null;
    }
}
