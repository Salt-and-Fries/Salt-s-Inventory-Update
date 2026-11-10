package com.salts_inventory_update.compat.aether;

import java.nio.ByteBuffer;
import java.util.*;
import com.salts_inventory_update.api.client.desktop.*;
import com.salts_inventory_update.api.client.input.MouseButtonEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.AbstractContainerMenu;

/** Aether is a Character subview. Its authenticated equipment session has no window. */
public final class AetherEquipmentClient {
    private static final Set<UUID> WANTED=new HashSet<>();
    private static final Map<UUID,DesktopWindowHandle> OWNERS=new HashMap<>();
    private static final Map<UUID,Binding> BINDINGS=new HashMap<>();
    private static final ResourceLocation CLOUD=new ResourceLocation("aether","textures/gui/inventory/button/accessories_button.png");
    private static final ResourceLocation BODY=new ResourceLocation("aether","textures/gui/inventory/accessories.png");
    private static final ResourceLocation SKINS=new ResourceLocation("aether","textures/gui/perks/skins/skins_button.png");
    private static final ResourceLocation CUSTOM=new ResourceLocation("aether","textures/gui/perks/customization/customization_button.png");
    private static final class Binding {
        final DesktopEmbeddedSession<AetherEquipmentMenu> session;final AetherEquipmentMenu menu;byte[] render=new byte[0];
        Binding(DesktopEmbeddedSession<AetherEquipmentMenu> s){session=s;menu=s.menu();}
    }
    private AetherEquipmentClient() { }
    static void initialize(){
        if(AetherEquipment.MENU==null)return;
        if(AetherAccess.fabric())AetherAccess.type("io.wispforest.accessories.client.gui.ToggleButton");
        DesktopMenuFactories.register(AetherEquipment.MENU,(mc,p,id,data)->{
            var b=new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.wrappedBuffer(data));
            try{var menu=new AetherEquipmentMenu(id,p.getInventory(),b);if(b.isReadable())throw new IllegalArgumentException("Aether equipment opening data");return menu;}finally{b.release();}
        });
        DesktopClientApi.registerEmbeddedSession(AetherEquipment.MENU,new DesktopEmbeddedSessionHandler<AetherEquipmentMenu>() {
            @Override public boolean opened(DesktopEmbeddedSession<AetherEquipmentMenu> session){return accept(session);}
            @Override public void closed(int id){sessionClosed(id);}
            @Override public void customPayload(DesktopEmbeddedSession<AetherEquipmentMenu> session,ResourceLocation channel,byte[] data){payload(session.sessionId(),channel,data);}
        });
        DesktopClientApi.registerBuiltinAttachment(AetherWindows.id("aether/cloud"),DesktopWorkspaceWindow.Kind.CHARACTER,Cloud::new);
        DesktopClientApi.onClientTick(mc->{if(mc.player==null){WANTED.clear();OWNERS.clear();BINDINGS.clear();}});
    }
    public static boolean openFromNative(){
        var mc=Minecraft.getInstance();if(AetherEquipment.MENU==null||mc.player==null||mc.player.isSpectator()||!DesktopClientApi.isDesktopAvailable())return false;
        var workspace=DesktopClientApi.currentWorkspace().orElse(null);
        if(workspace!=null)workspace.ensureWindow(DesktopWorkspaceWindow.Kind.CHARACTER);
        if(workspace==null||!workspace.isNativeScreenActive()){DesktopClientApi.openPlayerWorkspace();workspace=DesktopClientApi.currentWorkspace().orElse(null);}
        if(workspace==null)return false;toggle(workspace.ensureWindow(DesktopWorkspaceWindow.Kind.CHARACTER));return true;
    }
    static void toggle(DesktopWindowHandle owner){
        UUID id=owner.instanceId();if(WANTED.contains(id)){closeAttached(id);return;}
        if(!owner.isOpen()||!DesktopClientApi.isDesktopAvailable())return;
        owner.cancelInput();OWNERS.put(id,owner);
        com.salts_inventory_update.compat.accessories.AccessoriesClient.closeAttached(id);
        if(!AetherAccess.fabric())AetherAccess.call(AetherAccess.type("com.salts_inventory_update.compat.curios.CuriosCompatBootstrap"),"closeAttached",id);
        WANTED.add(id);AetherAccess.transport("open",id);
    }
    public static boolean active(UUID owner){return WANTED.contains(owner)&&BINDINGS.containsKey(owner);}
    public static boolean accept(DesktopEmbeddedSession<AetherEquipmentMenu> session){
        var menu=session.menu();var owner=OWNERS.get(menu.ownerWindow);
        if(!WANTED.contains(menu.ownerWindow)||owner==null||!owner.isOpen())return false;
        owner.cancelInput();BINDINGS.put(menu.ownerWindow,new Binding(session));return true;
    }
    public static void closeAttached(UUID owner){
        WANTED.remove(owner);var handle=OWNERS.get(owner);if(handle!=null&&handle.isOpen())handle.cancelInput();
        var b=BINDINGS.remove(owner);if(b!=null)b.session.close();
    }
    public static void sessionClosed(int id){
        for(var entry:List.copyOf(BINDINGS.entrySet()))if(entry.getValue().session.sessionId()==id){WANTED.remove(entry.getKey());BINDINGS.remove(entry.getKey());}
    }
    public static boolean generalOpened(UUID owner,DesktopWindowHandle panel){if(WANTED.contains(owner)){panel.close();return false;}return true;}
    public static boolean payload(int id,ResourceLocation channel,byte[] bytes){
        for(var b:BINDINGS.values())if(b.session.sessionId()==id){
            if(channel.equals(AetherEquipment.STATE)&&bytes.length==b.menu.cells.size()){
                for(byte value:bytes)if(value!=0&&value!=1)return true;b.render=bytes.clone();
            }return true;
        }return false;
    }
    public static AetherEquipmentMenu menu(UUID owner){var b=BINDINGS.get(owner);return b==null?null:b.menu;}
    public static int sessionId(UUID owner){var b=BINDINGS.get(owner);return b==null?-1:b.session.sessionId();}
    public static int generalButtonX(DesktopWindowContext<?,?> c){return c.contentX()+(active(c.windowHandle().instanceId())?9:27);}
    public static DesktopSlotHit playerSlotAt(DesktopWindowContext<AbstractContainerMenu,Void> c,double x,double y){
        if(!active(c.windowHandle().instanceId())||toggleAt(c,x,y)>=0)return null;
        for(int id:new int[]{5,6,7,8,45,1,2,3,4,0}){
            var r=AetherCharacterLayout.playerSlot(id);int sx=c.contentX()+r.x(),sy=c.contentY()+r.y();
            if(inside(x,y,sx-1,sy-1,18,18)&&id<c.model().slots.size())return DesktopSlotHit.of(id,sx,sy);
        }return null;
    }
    public static DesktopSessionSlotHit equipmentSlotAt(DesktopWindowContext<AbstractContainerMenu,Void> c,double x,double y){
        var b=BINDINGS.get(c.windowHandle().instanceId());if(b==null||!b.session.isOpen()||toggleAt(c,x,y)>=0)return null;
        for(int i=0;i<b.menu.cells.size();i++){
            var cell=b.menu.cells.get(i);var r=AetherCharacterLayout.equipment(cell.x(),cell.y());int sx=c.contentX()+r.x(),sy=c.contentY()+r.y();
            if(inside(x,y,sx-1,sy-1,18,18))return new DesktopSessionSlotHit(b.session.sessionId(),DesktopSlotHit.of(i,sx,sy));
        }return null;
    }
    public static void renderCharacter(DesktopRenderContext<AbstractContainerMenu,Void> c){
        var b=BINDINGS.get(c.windowHandle().instanceId());if(b==null)return;int x=c.contentX(),y=c.contentY();
        // Only native working artwork, leaving the existing Salt frame untouched.
        c.texture(BODY,x+7,y+7,7,7,165,73,165,73,256,256);
        for(int id:new int[]{5,6,7,8,45,1,2,3,4,0}){var r=AetherCharacterLayout.playerSlot(id);c.slot(id,x+r.x(),y+r.y());}
        for(int i=0;i<b.menu.cells.size();i++){var cell=b.menu.cells.get(i);c.slot(b.menu.slots.get(i),x+77+cell.x(),y+8+cell.y());}
        c.text(Component.translatable("container.crafting"),x+116,y+2,0xff404040,false);
        if(c.minecraft().player!=null)c.withClip(x+8,y+7,51,72,()->{
            int mx=c.mouseX()==Integer.MIN_VALUE?x+33:c.mouseX(),my=c.mouseY()==Integer.MIN_VALUE?y+42:c.mouseY();
            net.minecraft.client.gui.screens.inventory.InventoryScreen.renderEntityInInventoryFollowsMouse(c.nativeGraphics(),x+33,y+75,30,(float)(x+33-mx),(float)(y+25-my),c.minecraft().player);
        });
        c.rawGraphics().nextStratum();
        for(int i=0;i<b.menu.cells.size();i++){
            var cell=b.menu.cells.get(i);if(!cell.toggle())continue;int tx=x+88+cell.x(),ty=y+5+cell.y();boolean shown=b.render.length<=i||b.render[i]==1;
            if(AetherAccess.fabric()){
                String sprite=!shown?"button_disabled":inside(c.mouseX(),c.mouseY(),tx,ty,5,5)?"button_highlighted":"button";
                AetherAccess.call(AetherAccess.type("io.wispforest.accessories.client.GuiGraphicsUtils"),"blitSpriteBatched",c.nativeGraphics(),new ResourceLocation("accessories","widget/"+sprite),tx,ty,5,5);
            }else c.texture(new ResourceLocation("curios","textures/gui/inventory.png"),tx,ty,shown?75:83,0,8,8,8,8,256,256);
        }
    }
    private static int toggleAt(DesktopWindowContext<?,?> c,double x,double y){
        var b=BINDINGS.get(c.windowHandle().instanceId());if(b==null)return -1;
        for(int i=0;i<b.menu.cells.size();i++){var cell=b.menu.cells.get(i);if(cell.toggle()&&inside(x,y,c.contentX()+88+cell.x(),c.contentY()+5+cell.y(),AetherAccess.fabric()?5:8,AetherAccess.fabric()?5:8))return i;}return -1;
    }
    private static boolean inside(double x,double y,int bx,int by,int w,int h){return x>=bx&&x<bx+w&&y>=by&&y<by+h;}
    private static boolean customization(){
        Object user=AetherAccess.call(AetherAccess.type("com.aetherteam.nitrogen.api.users.UserData$Client"),"getClientUser");if(user==null)return false;var perk=AetherAccess.type("com.aetherteam.aether.perk.PerkUtil");
        return ((java.util.function.Predicate<Object>)AetherAccess.call(perk,"hasDeveloperGlow")).test(user)||((java.util.function.Predicate<Object>)AetherAccess.call(perk,"hasHalo")).test(user);
    }
    private static final class Cloud implements DesktopBuiltinAttachment {
        @Override public boolean replacesContent(DesktopWindowContext<AbstractContainerMenu,Void> c){return active(c.windowHandle().instanceId());}
        @Override public void renderContent(DesktopRenderContext<AbstractContainerMenu,Void> c){renderCharacter(c);}
        @Override public DesktopSlotHit slotAt(DesktopSlotContext<AbstractContainerMenu,Void> c,double x,double y){return playerSlotAt(c,x,y);}
        @Override public DesktopSessionSlotHit embeddedSlotAt(DesktopSlotContext<AbstractContainerMenu,Void> c,double x,double y){return equipmentSlotAt(c,x,y);}
        @Override public boolean occludes(DesktopWindowContext<AbstractContainerMenu,Void> c,double x,double y){
            return active(c.windowHandle().instanceId())&&inside(x,y,c.contentX(),c.contentY(),c.contentWidth(),c.contentHeight());
        }
        @Override public Optional<DesktopWindowBounds> builtinSlotBounds(DesktopWindowContext<AbstractContainerMenu,Void> c,int id){
            if(!active(c.windowHandle().instanceId())||!(id>=0&&id<=8||id==45))return Optional.empty();
            var r=AetherCharacterLayout.playerSlot(id);return Optional.of(new DesktopWindowBounds(c.contentX()+r.x(),c.contentY()+r.y(),16,16));
        }
        @Override public Optional<DesktopWindowBounds> recipeBookButtonBounds(DesktopWindowContext<AbstractContainerMenu,Void> c){
            return active(c.windowHandle().instanceId())?Optional.of(new DesktopWindowBounds(c.contentX()+134,c.contentY()+61,20,18)):Optional.empty();
        }

        @Override public void opened(DesktopWindowContext<AbstractContainerMenu,Void> c){OWNERS.put(c.windowHandle().instanceId(),c.windowHandle());}
        @Override public void suspended(DesktopWindowContext<AbstractContainerMenu,Void> c){if(!c.visible())closeAttached(c.windowHandle().instanceId());}
        @Override public void closed(DesktopWindowContext<AbstractContainerMenu,Void> c){closeAttached(c.windowHandle().instanceId());OWNERS.remove(c.windowHandle().instanceId());}
        private int x(DesktopWindowContext<?,?> c){return c.contentX()+(active(c.windowHandle().instanceId())?9:27);}
        private int y(DesktopWindowContext<?,?> c){return c.contentY()+(active(c.windowHandle().instanceId())?68:Math.max(0,c.contentHeight()-12));}
        private boolean cloud(DesktopWindowContext<?,?> c,double x,double y){return inside(x,y,x(c),y(c),12,8);}
        private int iconX(DesktopWindowContext<?,?> c,boolean custom){return c.windowX()>=22?c.windowX()-22:c.windowX()+(custom?24:2);}
        private int iconY(DesktopWindowContext<?,?> c,boolean custom){return c.windowX()>=22?c.contentY()+(custom?24:2):c.windowY()-22;}
        private boolean perk(DesktopWindowContext<?,?> c,double x,double y,boolean custom){return active(c.windowHandle().instanceId())&&(!custom||customization())&&inside(x,y,iconX(c,custom),iconY(c,custom),20,20);}
        @Override public void render(DesktopRenderContext<AbstractContainerMenu,Void> c){
            c.texture(CLOUD,x(c),y(c),0,cloud(c,c.mouseX(),c.mouseY())?8:0,12,8,12,8,12,16);
            if(active(c.windowHandle().instanceId())){
                c.texture(SKINS,iconX(c,false),iconY(c,false),0,perk(c,c.mouseX(),c.mouseY(),false)?20:0,20,20,20,20,20,40);
                if(customization())c.texture(CUSTOM,iconX(c,true),iconY(c,true),0,perk(c,c.mouseX(),c.mouseY(),true)?20:0,20,20,20,20,20,40);
            }
        }
        @Override public boolean mouseClicked(DesktopInputContext<AbstractContainerMenu,Void> c,MouseButtonEvent e,boolean twice){
            if(cloud(c,e.x(),e.y())){if(e.button()==0)toggle(c.windowHandle());return true;}
            if(perk(c,e.x(),e.y(),false)||perk(c,e.x(),e.y(),true)){
                if(e.button()==0&&c.carriedStack().isEmpty())try{String name=perk(c,e.x(),e.y(),false)?"MoaSkinsScreen":"AetherCustomizationsScreen";var mc=Minecraft.getInstance();mc.setScreen((Screen)AetherAccess.type("com.aetherteam.aether.client.gui.screen.perks."+name).getConstructor(Screen.class).newInstance(mc.screen));}catch(ReflectiveOperationException failure){throw new IllegalStateException(failure);}return true;
            }
            int i=toggleAt(c,e.x(),e.y());if(i<0)return false;if(e.button()==0){var binding=BINDINGS.get(c.windowHandle().instanceId());if(binding!=null)binding.session.sendPayload(AetherEquipment.VISIBILITY,ByteBuffer.allocate(4).putInt(i).array());}return true;
        }
        @Override public boolean appendTooltip(DesktopRenderContext<AbstractContainerMenu,Void> c,int x,int y){
            if(cloud(c,x,y)){c.tooltip(Component.literal(active(c.windowHandle().instanceId())?"Normal Character":"Aether Equipment"),x,y);return true;}
            if(perk(c,x,y,false)){c.tooltip(Component.translatable("gui.aether.accessories.skins_button"),x,y);return true;}
            if(perk(c,x,y,true)){c.tooltip(Component.translatable("gui.aether.accessories.customization_button"),x,y);return true;}
            if(toggleAt(c,x,y)>=0){c.tooltip(Component.literal("Toggle accessory visibility"),x,y);return true;}return false;
        }
        @Override public List<DesktopWindowRegion> regions(DesktopWindowContext<AbstractContainerMenu,Void> c){
            var r=new ArrayList<DesktopWindowRegion>();r.add(DesktopWindowRegion.interactive(x(c)-c.windowX(),y(c)-c.windowY(),12,8).withRole(DesktopHitRole.CONTROL));
            if(active(c.windowHandle().instanceId())){
                var binding=BINDINGS.get(c.windowHandle().instanceId());
                for(var cell:binding.menu.cells)if(cell.toggle())r.add(DesktopWindowRegion.interactive(
                    c.contentX()+88+cell.x()-c.windowX(),c.contentY()+5+cell.y()-c.windowY(),5,5).withRole(DesktopHitRole.CONTROL));
                r.add(DesktopWindowRegion.visualAndInteractive(iconX(c,false)-c.windowX(),iconY(c,false)-c.windowY(),20,20).withRole(DesktopHitRole.CONTROL));
                if(customization())r.add(DesktopWindowRegion.visualAndInteractive(iconX(c,true)-c.windowX(),iconY(c,true)-c.windowY(),20,20).withRole(DesktopHitRole.CONTROL));
            }return List.copyOf(r);
        }
    }
}
