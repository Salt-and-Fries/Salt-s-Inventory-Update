package com.salts_inventory_update.compat.modernindustrialization;

import com.salts_inventory_update.api.client.desktop.*;
import com.salts_inventory_update.api.desktop.DesktopMenuBindings;
import com.salts_inventory_update.api.integration.DesktopEnvironment;
import java.util.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;

/** Native MI machines and pipe editors, inside the approved SIU frames. */
public final class ModernIndustrializationWindows {
    private static final Set<String> BOUND=new HashSet<>(),INSTALLED=new HashSet<>();
    private ModernIndustrializationWindows() { }
    public static void initializeCommon(){bind();DesktopEnvironment.onServerStarting(server->bind());}
    public static void initializeClient(){install();DesktopClientApi.onClientStarted(client->{bind();install();});}
    private static boolean supported(String path){return Set.of("machine","forge_hammer","item_pipe","fluid_pipe").contains(path);}
    @SuppressWarnings({"unchecked","rawtypes"}) private static synchronized void bind(){
        for(var type:BuiltInRegistries.MENU){
            var id=BuiltInRegistries.MENU.getKey(type);
            if(id==null||!id.getNamespace().equals("modern_industrialization")||!supported(id.getPath())||!BOUND.add(id.toString()))continue;
            DesktopMenuBindings.register(type,new DesktopMenuBindings.SlotAccess(){
                @Override public int size(AbstractContainerMenu menu){return menu.slots.size();}
                @Override public boolean isVirtualSlot(AbstractContainerMenu menu,Slot slot){
                    String name=slot.getClass().getName();
                    return name.endsWith("$FilterSlot")||name.endsWith("$ConfigurableFluidSlot");
                }
            },!id.getPath().equals("forge_hammer"),null,null,new DesktopMenuBindings.GesturePolicy(){
                @Override public boolean useNativeQuickMove(AbstractContainerMenu menu){return true;}
                @Override public boolean allowsTargetedQuickMoveSource(AbstractContainerMenu menu,Slot slot){return false;}
            });
            // Recipe inputs, locked/capacity-limited cells and pipe filters are
            // native machine configuration, not a sortable chest inventory.
            DesktopMenuBindings.registerStorage(type,(menu,slot)->false);
            ModernIndustrializationPackets.register((net.minecraft.world.inventory.MenuType)type,id.getPath());
        }
    }
    private static synchronized void install(){
        for(var type:BuiltInRegistries.MENU){
            var id=BuiltInRegistries.MENU.getKey(type);
            if(id==null||!id.getNamespace().equals("modern_industrialization")||!supported(id.getPath())||!INSTALLED.add(id.toString()))continue;
            DesktopClientApi.registerNativeMatcher(new ResourceLocation("salts_inventory_compat","modern-industrialization/"+id.getPath()),
                100,Set.of(type),lookup->true,lookup->new Template());
        }
    }
    private static final class State {DesktopNativeLayout layout;}
    private static final class Template implements DesktopNativeTemplate<AbstractContainerMenu,State>,DesktopWindowPresentation {
        @Override public State createState(DesktopWindowSetupContext<AbstractContainerMenu> c){return new State();}
        @Override public DesktopWindowSurface surface(DesktopWindowContext<AbstractContainerMenu,State> c){
            return new DesktopWindowSurface(DesktopSurfaceMode.SALT,new DesktopInsets(6,22,6,6),true,List.of(),DesktopViewportPolicy.CLAMP);
        }
        @Override public void opened(DesktopWindowContext<AbstractContainerMenu,State> c){ModernIndustrializationClient.open(c.model());}
        @Override public void closed(DesktopWindowContext<AbstractContainerMenu,State> c){ModernIndustrializationClient.close(c.model());}
        @Override public void prepareLayout(DesktopWindowContext<AbstractContainerMenu,State> c){ModernIndustrializationClient.prepare(c.model());}
        @Override public DesktopNativeLayout nativeLayout(DesktopNativeContext<AbstractContainerMenu,State> c){
            return c.state().layout=ModernIndustrializationLayouts.layout(c.nativeView(),c.model(),c.fontWidth(c.originalTitle()));
        }
        @Override public void renderBehindFrame(DesktopRenderContext<?,?> c){if(c.state() instanceof State state)ModernIndustrializationPanels.paint(c,state.layout);}
        @Override public List<DesktopWindowRegion> externalRegions(DesktopWindowContext<?,?> c){
            if(!(c.state() instanceof State state)||state.layout==null)return List.of();
            return state.layout.regions().stream().filter(DesktopNativeRegion::exterior).map(r->DesktopWindowRegion.visual(
                r.id().equals("native-shape-tray")?r.x()+r.source().width()-4:r.x(),r.y()+4,4,r.source().height()-4)).toList();
        }
        @Override public void renderOverlay(DesktopRenderContext<AbstractContainerMenu,State> c){
            if(c.fontWidth(c.originalTitle())>c.windowWidth()-42&&c.mouseX()>=c.windowX()+6&&c.mouseX()<c.windowX()+c.windowWidth()-34
                &&c.mouseY()>=c.windowY()+3&&c.mouseY()<c.windowY()+18)c.tooltip(c.originalTitle(),c.mouseX(),c.mouseY());
        }
        @Override public String layoutRevision(DesktopWindowContext<AbstractContainerMenu,State> c){return "mi-native-2";}
    }
}
