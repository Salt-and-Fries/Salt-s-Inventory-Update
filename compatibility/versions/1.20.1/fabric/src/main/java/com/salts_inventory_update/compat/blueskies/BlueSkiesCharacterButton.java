package com.salts_inventory_update.compat.blueskies;

import com.salts_inventory_update.api.client.desktop.*;
import com.salts_inventory_update.api.client.input.MouseButtonEvent;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.AbstractContainerMenu;

/** Keep the original Arcs tab reachable beside Character, clear of Curios. */
public final class BlueSkiesCharacterButton implements DesktopBuiltinAttachment {
    private static final int X=-25,Y=24,W=25,H=22;
    private static final ResourceLocation ART=new ResourceLocation("blue_skies","textures/gui/inventory_tabs_simple.png");
    public static void initialize() {
        DesktopClientApi.registerBuiltinAttachment(new ResourceLocation("blue_skies","arcs"),
            DesktopWorkspaceWindow.Kind.CHARACTER,BlueSkiesCharacterButton::new);
    }
    private static boolean contains(DesktopWindowContext<?,?> c,double x,double y) {
        return x>=c.windowX()+X&&x<c.windowX()+X+W&&y>=c.windowY()+Y&&y<c.windowY()+Y+H;
    }
    @Override public void render(DesktopRenderContext<AbstractContainerMenu,Void> c) {
        c.texture(ART,c.windowX()+X,c.windowY()+Y,25,contains(c,c.mouseX(),c.mouseY())?0:22,W,H,W,H,128,128);
    }
    @Override public boolean mouseClicked(DesktopInputContext<AbstractContainerMenu,Void> c,MouseButtonEvent event,boolean twice) {
        if(!contains(c,event.x(),event.y()))return false;
        if(event.button()==0)try {
            // Exactly the request used by the original inventory tab. The mod
            // opens and synchronizes its own player-backed Arcs menu.
            Object request=Class.forName("com.legacy.blue_skies.network.c_to_s.OpenInventoryPacket$Arcs").getConstructor().newInstance();
            Class.forName("com.legacy.blue_skies.network.PacketHandler").getMethod("sendToServer",Object.class).invoke(null,request);
        }catch(ReflectiveOperationException error){throw new IllegalStateException("Blue Skies Arcs inventory request",error);}
        return true;
    }
    @Override public boolean appendTooltip(DesktopRenderContext<AbstractContainerMenu,Void> c,int x,int y) {
        if(!contains(c,x,y))return false;
        c.tooltip(Component.translatable("gui.blue_skies.arc_inventory"),x,y);return true;
    }
    @Override public List<DesktopWindowBounds> placementBounds(DesktopWindowContext<AbstractContainerMenu,Void> c) {
        return List.of(new DesktopWindowBounds(X,Y,W,H));
    }
    @Override public List<DesktopWindowRegion> regions(DesktopWindowContext<AbstractContainerMenu,Void> c) {
        return List.of(DesktopWindowRegion.visualAndInteractive(X,Y,W,H).withRole(DesktopHitRole.CONTROL));
    }
}
