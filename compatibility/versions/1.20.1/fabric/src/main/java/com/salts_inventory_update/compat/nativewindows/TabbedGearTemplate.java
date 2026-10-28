package com.salts_inventory_update.compat.nativewindows;

import com.salts_inventory_update.api.client.desktop.*;
import com.salts_inventory_update.api.client.input.*;
import java.util.List;
import java.util.function.Function;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.ItemStack;

/** Native LibZ tab artwork, supported session transitions and native action textures. */
public class TabbedGearTemplate extends MockupNativeTemplate<TabbedGearTemplate.State> {
    public static final class State {
        boolean ready;
        EditBox name;
        ItemStack last=ItemStack.EMPTY;
    }
    private final boolean second;
    private final boolean rename;
    private final ResourceLocation texture;
    private final DesktopWindowBounds button;
    public TabbedGearTemplate(Function<DesktopNativeView,DesktopNativeLayout> layout,boolean second,boolean rename,
            ResourceLocation texture,DesktopWindowBounds button,Emblem emblem) {
        super(layout,emblem);this.second=second;this.rename=rename;this.texture=texture;this.button=button;
    }
    @Override public State createState(DesktopWindowSetupContext<AbstractContainerMenu> c) {
        var state=new State();
        if(rename){com.salts_inventory_update.compat.tieredz.GearClientMenus.opened(c.model());state.name=new EditBox(c.minecraft().font,0,0,103,12,Component.translatable("container.repair"));
            state.name.setBordered(false);state.name.setMaxLength(50);state.name.setCanLoseFocus(true);state.name.setTextColor(0xFFFFFFFF);}
        return state;
    }
    @Override public void closed(DesktopWindowContext<AbstractContainerMenu,State> c){if(rename)com.salts_inventory_update.compat.tieredz.GearClientMenus.closed(c.model());}
    @Override public DesktopNativeLayout nativeLayout(DesktopNativeContext<AbstractContainerMenu,State> c) {
        var layout=super.nativeLayout(c);
        if(rename&&c.model() instanceof AnvilMenu anvil&&anvil.getCost()>0){
            Component label=anvil.getCost()>=40&&!c.minecraft().player.getAbilities().instabuild
                ?Component.translatable("container.repair.expensive")
                :anvil.getSlot(2).hasItem()?Component.translatable("container.repair.cost",anvil.getCost()):null;
            if(label==null)return layout;
            // Retain the cost's original full background/text bounds, not the unrelated Inventory label.
            int left=Math.max(8,164-c.fontWidth(label));
            int x=layout.regions().stream().filter(r->r.id().equals("items")).mapToInt(DesktopNativeRegion::x).findFirst().orElse(0);
            return layout.toBuilder().keep("cost",new DesktopWindowBounds(left,67,168-left,12),x+left-8,42).build();
        }
        return layout;
    }
    private DesktopNativeContext<AbstractContainerMenu,State> nativeContext(DesktopWindowContext<AbstractContainerMenu,State> c) {
        return (DesktopNativeContext<AbstractContainerMenu,State>)c;
    }
    private DesktopNativeLayout plan(DesktopWindowContext<AbstractContainerMenu,State> c){return nativeLayout(nativeContext(c));}
    private DesktopWindowBounds placement(DesktopWindowContext<AbstractContainerMenu,State> c,DesktopWindowBounds nativeBounds) {
        var placements=plan(c).placements(nativeBounds);if(placements.isEmpty())return null;
        var b=placements.get(0).bounds();return new DesktopWindowBounds(c.contentX()+b.x(),c.contentY()+b.y(),b.width(),b.height());
    }
    private void field(DesktopWindowContext<AbstractContainerMenu,State> c) {
        if(!rename)return;var b=placement(c,new DesktopWindowBounds(62,24,103,12));
        if(b!=null){c.state().name.setX(b.x());c.state().name.setY(b.y());}
    }
    @Override public void prepareLayout(DesktopWindowContext<AbstractContainerMenu,State> c){field(c);}
    @Override public void tick(DesktopWindowContext<AbstractContainerMenu,State> c) {
        if(!rename)return;field(c);c.state().name.tick();var slot=c.menuSlot(0);var stack=slot==null?ItemStack.EMPTY:slot.getItem();
        if(!ItemStack.matches(stack,c.state().last)){
            c.state().last=stack.copy();c.state().name.setValue(stack.isEmpty()?"":stack.getHoverName().getString());
        }
        c.state().name.setEditable(!stack.isEmpty());
    }
    @Override public void customPayload(DesktopWindowContext<AbstractContainerMenu,State> c,ResourceLocation channel,byte[] data) {
        if(channel.equals(GearMenuBridge.READY)&&data.length==1)c.state().ready=data[0]!=0;
    }
    @Override public boolean mouseClicked(DesktopInputContext<AbstractContainerMenu,State> c,MouseButtonEvent e,boolean twice) {
        var l=plan(c);var p=l.exteriorSourceAt(e.x()-c.windowX(),e.y()-c.windowY());
        if(p.isPresent()&&p.get().regionId().equals("tabs")){
            int selected=p.get().x()>=25?1:0;
            if(selected!=(second?1:0))c.sendPayload(GearMenuBridge.ACTION,new byte[]{(byte)selected});
            return true;
        }
        if(rename){field(c);if(c.state().name.mouseClicked(e.x(),e.y(),e.button())){
            // A standalone widget has no vanilla ContainerEventHandler to assign its focus.
            c.state().name.setFocused(true);return true;
        }c.state().name.setFocused(false);}
        if(button!=null){var b=placement(c,button);if(b!=null&&contains(b,e.x(),e.y())){
            if(e.button()==0&&c.state().ready)c.sendPayload(GearMenuBridge.ACTION,new byte[]{2});return true;
        }}return false;
    }
    @Override public boolean keyPressed(DesktopInputContext<AbstractContainerMenu,State> c,KeyEvent e) {
        if(!rename||!c.state().name.isFocused())return false;
        String old=c.state().name.getValue();boolean consumed=c.state().name.keyPressed(e.key(),e.scancode(),e.modifiers());
        if(!old.equals(c.state().name.getValue()))sendRename(c);return consumed;
    }
    @Override public boolean charTyped(DesktopInputContext<AbstractContainerMenu,State> c,CharacterEvent e) {
        if(!rename||!c.state().name.isFocused())return false;
        String old=c.state().name.getValue();boolean consumed=c.state().name.charTyped(e.codepoint(),e.modifiers());
        if(!old.equals(c.state().name.getValue()))sendRename(c);return consumed;
    }
    private void sendRename(DesktopInputContext<AbstractContainerMenu,State> c){
        String name=c.state().name.getValue();var slot=c.menuSlot(0);
        if(slot!=null&&slot.hasItem()&&!slot.getItem().hasCustomHoverName()&&name.equals(slot.getItem().getHoverName().getString()))name="";
        c.sendRename(name);
    }
    @Override public boolean wantsTextInput(DesktopWindowContext<AbstractContainerMenu,State> c){return rename&&c.state().name.canConsumeInput();}
    @Override public List<DesktopWindowBounds> textInputBounds(DesktopWindowContext<AbstractContainerMenu,State> c){
        var b=rename?placement(c,new DesktopWindowBounds(62,24,103,12)):null;return b==null?List.of():List.of(b);
    }
    @Override public void keyboardFocusChanged(DesktopWindowContext<AbstractContainerMenu,State> c,boolean focused){if(rename&&!focused)c.state().name.setFocused(false);}
    @Override public void renderOverlay(DesktopRenderContext<AbstractContainerMenu,State> c) {
        var tabs=plan(c).regions().stream().filter(r->r.exterior()&&r.id().equals("tabs")).findFirst().orElse(null);
        if(tabs==null)return;
        // The native selected LibZ tab is 27px tall: its last five rows join the
        // container's top bevel. Exterior fragments paint behind Salt's frame,
        // so restore this small native connector above that frame, below later windows.
        c.texture(new ResourceLocation("libz","textures/gui/icons.png"),
            c.windowX()+tabs.x()+(second?25:0),c.windowY()-1,second?48:0,22,
            24,5,24,5,256,256);
    }
    @Override public void render(DesktopRenderContext<AbstractContainerMenu,State> c) {
        if(rename){field(c);var n=c.state().name;c.fill(n.getX(),n.getY(),n.getX()+103,n.getY()+12,0xFF4D4735);
            n.render(c.nativeGraphics(),c.mouseX(),c.mouseY(),c.minecraft().getFrameTime());}
        if(button!=null){var b=placement(c,button);if(b!=null){int u=!c.state().ready?212:contains(b,c.mouseX(),c.mouseY())?194:176;
            c.texture(texture,b.x(),b.y(),u,0,18,18,18,18,256,256);}}
    }
    private static boolean contains(DesktopWindowBounds b,double x,double y){return x>=b.x()&&y>=b.y()&&x<b.x()+b.width()&&y<b.y()+b.height();}
}
