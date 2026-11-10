package com.salts_inventory_update.compat.accessories;

import java.util.*;
import com.salts_inventory_update.compat.equipment.EquipmentPanelSelection;
import static com.salts_inventory_update.compat.equipment.EquipmentPanelSelection.Kind.ACCESSORIES;
import com.salts_inventory_update.api.client.desktop.*;
import com.salts_inventory_update.api.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.AbstractContainerMenu;

/** Curios-style Character launcher and child lifetime. */
public final class AccessoriesClient {
    private AccessoriesClient() { }
    static void initialize(){
        DesktopMenuFactories.register(AccessoriesIntegration.MENU,(mc,p,id,data)->{
            var b=new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.wrappedBuffer(data));
            try{var menu=new AccessoriesDesktopMenu(id,p.getInventory(),b);if(b.isReadable())throw new IllegalArgumentException("Trailing Accessories opening data");return menu;}finally{b.release();}
        });
        DesktopClientApi.registerWindow(AccessoriesIntegration.MENU,new AccessoriesPanel());
        DesktopClientApi.onClientTick(mc->{if(mc.player==null)EquipmentPanelSelection.DESKTOP.clear();});
        DesktopClientApi.registerBuiltinAttachment(AccessoriesIntegration.id("accessories_button"),DesktopWorkspaceWindow.Kind.CHARACTER,Button::new);
    }
    public static boolean openFromNative(){
        if(AccessoriesIntegration.MENU==null||!DesktopClientApi.isDesktopAvailable())return false;
        var workspace=DesktopClientApi.currentWorkspace().orElse(null);
        // A retained workspace can exist while its screen is dismissed. Ensure
        // Character before the inventory toggle so an existing inventory alone
        // cannot toggle away the last visible window during this reopen.
        if(workspace!=null)workspace.ensureWindow(DesktopWorkspaceWindow.Kind.CHARACTER);
        if(workspace==null||!workspace.isNativeScreenActive()){
            DesktopClientApi.openPlayerWorkspace();workspace=DesktopClientApi.currentWorkspace().orElse(null);
        }
        if(workspace==null)return false;toggle(workspace.ensureWindow(DesktopWorkspaceWindow.Kind.CHARACTER));return true;
    }
    static void toggle(DesktopWindowHandle owner){
        if(!owner.isOpen()||!DesktopClientApi.isDesktopAvailable())return;
        if(!EquipmentPanelSelection.DESKTOP.request(owner.instanceId(),ACCESSORIES,System.nanoTime()))return;
        com.salts_inventory_update.compat.aether.AetherEquipmentClient.closeAttached(owner.instanceId());
        try{Class.forName("com.salts_inventory_update.compat.accessories.AccessoriesTransport").getMethod("open",UUID.class).invoke(null,owner.instanceId());}catch(ReflectiveOperationException failure){throw new IllegalStateException(failure);}
    }
    static void opened(UUID owner,DesktopWindowHandle panel){if(!com.salts_inventory_update.compat.aether.AetherEquipmentClient.generalOpened(owner,panel)||!EquipmentPanelSelection.DESKTOP.accept(owner,ACCESSORIES,panel))panel.close();}
    static void closed(UUID owner,DesktopWindowHandle panel){EquipmentPanelSelection.DESKTOP.closed(owner,ACCESSORIES,panel);}
    public static void closeAttached(UUID owner){EquipmentPanelSelection.DESKTOP.close(owner,ACCESSORIES);}
    private static final class Button implements DesktopBuiltinAttachment {
        private static final int Y=9,SIZE=10;
        private int x(DesktopWindowContext<?,?> c){return c.contentX()+(com.salts_inventory_update.compat.aether.AetherEquipmentClient.active(c.windowHandle().instanceId())?45:64);}
        private boolean contains(DesktopWindowContext<?,?> c,double x,double y){return x>=x(c)&&x<x(c)+SIZE&&y>=c.contentY()+Y&&y<c.contentY()+Y+SIZE;}
        @Override public void render(DesktopRenderContext<AbstractContainerMenu,Void> c){String suffix=contains(c,c.mouseX(),c.mouseY())?"_highlighted":"";c.texture(new ResourceLocation("accessories","textures/gui/sprites/widget/8x8/button"+suffix+".png"),x(c),c.contentY()+Y,0,0,SIZE,SIZE,8,8,8,8);}
        @Override public boolean mouseClicked(DesktopInputContext<AbstractContainerMenu,Void> c,MouseButtonEvent e,boolean twice){if(!contains(c,e.x(),e.y()))return false;if(e.button()==0)toggle(c.windowHandle());return true;}
        @Override public boolean appendTooltip(DesktopRenderContext<AbstractContainerMenu,Void> c,int x,int y){if(!contains(c,x,y))return false;c.tooltip(Component.translatable("accessories.open.screen"),x,y);return true;}
        @Override public List<DesktopWindowRegion> regions(DesktopWindowContext<AbstractContainerMenu,Void> c){return List.of(DesktopWindowRegion.interactive(x(c)-c.windowX(),c.contentY()-c.windowY()+Y,SIZE,SIZE).withRole(DesktopHitRole.CONTROL));}
    }
}
