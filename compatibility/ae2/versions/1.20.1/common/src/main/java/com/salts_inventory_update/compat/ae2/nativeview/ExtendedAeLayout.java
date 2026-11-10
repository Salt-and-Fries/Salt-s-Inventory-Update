package com.salts_inventory_update.compat.ae2.nativeview;

import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.style.BackgroundGenerator;
import com.salts_inventory_update.api.client.desktop.DesktopInsets;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/** Version-local native composition; the original menu/widgets still own every action. */
public final class ExtendedAeLayout {
    private ExtendedAeLayout() { }
    /** Give directly-created native controls stable AE2 desktop registrations. */
    public static void prepare(AEBaseScreen<?> screen){
        if(!ExtendedAePackets.supports(screen))return;
        String name=screen.getClass().getSimpleName();
        if(name.equals("GuiPatternModifier")){
            int i=0;for(Object b:(java.util.List<?>)ExtendedAePackets.field(screen,"multiBtns"))retain(screen,"siu_multi"+i++,b,0,0);
            retain(screen,"siu_replace",ExtendedAePackets.field(screen,"replace"),0,0);
            retain(screen,"siu_clone",ExtendedAePackets.field(screen,"clone"),0,0);
        }else if(name.equals("GuiWirelessConnector")){
            retain(screen,"siu_status",ExtendedAePackets.field(screen,"statusIcon"),152,-4);
            Object highlight=ExtendedAePackets.optionalField(screen,"highlight");
            if(highlight!=null)retain(screen,"siu_highlight",highlight,152,24);
            retain(screen,"siu_remote",ExtendedAePackets.field(screen,"remote"),24,76);
        }else if(name.equals("GuiWirelessHub")){
            for(String field:new String[]{"remotes","highlightBtn","disconnectBtn"}){
                int i=0;for(Object b:(Object[])ExtendedAePackets.field(screen,field))retain(screen,"siu_"+field+i++,b,0,0);
            }
        }
    }
    @SuppressWarnings("unchecked") private static void retain(AEBaseScreen<?> screen,String name,Object widget,int x,int y){
        var container=(appeng.client.gui.WidgetContainer)ExtendedAePackets.field(screen,"widgets");
        if(((java.util.Map<?,?>)ExtendedAePackets.field(container,"widgets")).get(name)==widget)return;
        var styles=(java.util.Map<String,appeng.client.gui.style.WidgetStyle>)ExtendedAePackets.field(screen.getStyle(),"widgets");
        var style=new appeng.client.gui.style.WidgetStyle();style.setLeft(x);style.setTop(y);
        styles.put(name,style);
        if(widget instanceof net.minecraft.client.gui.components.AbstractWidget button){style.setWidth(button.getWidth());style.setHeight(button.getHeight());container.add(name,button);}
        else if(widget instanceof appeng.client.gui.ICompositeWidget composite)container.add(name,composite);
        else throw new IllegalStateException("Unsupported Extended AE control "+widget.getClass().getName());
    }
    public static boolean fixed(AEBaseScreen<?> screen){return ExtendedAePackets.supports(screen)&&screen.getStyle().getTerminalStyle()==null;}
    public static int inset(Object owner){
        if(!ExtendedAePackets.supports(owner))return 0;
        String n=owner.getClass().getSimpleName();
        return n.contains("PatternTerminal")||n.contains("WirelessExPAT")||n.contains("UWirelessExPAT")||n.contains("ExCraftingTerminal")||n.contains("WirelessExCT")||n.equals("SetAmount")?0:10;
    }
    public static DesktopInsets insets(AEBaseScreen<?> screen){return new DesktopInsets(0,inset(screen),0,0);}
    public static boolean background(AEBaseScreen<?> screen,GuiGraphics g,int mouseX,int mouseY,float delta){
        if(!fixed(screen))return false;
        var body=screen.desktopContentBounds();int x=body.getX(),y=body.getY(),w=body.getWidth(),h=body.getHeight(),top=inset(screen);
        BackgroundGenerator.draw(w,h+top,g,x,y-top);
        int extra=screen.getClass().getSimpleName().equals("GuiPatternModifier")?16:0;
        int first=screen.getClass().getSimpleName().contains("PatternTerminal")||screen.getClass().getSimpleName().contains("WirelessExPAT")?23:14;
        int bottom=screen.getClass().getSimpleName().equals("GuiAssemblerMatrix")?104:h-4;
        // Extra frame height belongs to the upgrade tray; never expose the old player inventory.
        if(screen.getClass().getSimpleName().equals("GuiExIOPort")||screen.getClass().getSimpleName().equals("GuiModExportBus"))
            bottom=Math.min(bottom,screen.getStyle().getBackground().getSrcHeight()-screen.desktopInventoryCut()-4);
        g.enableScissor(x+4,y+first+extra,x+w-4,y+bottom);
        try{screen.drawBG(g,x,y+extra,mouseX,mouseY,delta);}finally{g.disableScissor();}
        return true;
    }
    public static boolean patternLabels(Object owner,GuiGraphics g){
        if(!(owner instanceof AEBaseScreen<?> screen)||!screen.isDesktopHosted()||!screen.getClass().getSimpleName().equals("GuiPatternModifier"))return false;
        int page=((Number)ExtendedAePackets.field(screen.getMenu(),"page")).intValue();String mode=page==0?"multiply":page==1?"replace":"clone";
        var font=Minecraft.getInstance().font;g.drawString(font,Component.translatable("gui.siu.extendedae.mode."+mode),8,14,0x404040,false);
        if(page==2){String namespace=screen.getClass().getName().startsWith("com.github.")?"extendedae":"expatternprovider";
            g.drawString(font,Component.translatable("gui."+namespace+".pattern_modifier.target"),52,41,0x404040,false);
            g.drawString(font,Component.translatable("gui."+namespace+".pattern_modifier.blank"),52,73,0x404040,false);
        }
        return true;
    }
    /** These controls are created directly by the addon, outside AE2's named-widget table. */
    public static void layout(AEBaseScreen<?> screen){
        if(!ExtendedAePackets.supports(screen)||!screen.isDesktopHosted())return;
        int x=screen.getGuiLeft(),y=screen.getGuiTop();String name=screen.getClass().getSimpleName();
        restoreRaisedTabs(screen);
        if(name.contains("PatternTerminal")||name.contains("WirelessExPAT")||name.contains("UWirelessExPAT")){
            int rows=Math.max(2,(screen.desktopContentBounds().getHeight()-57)/18);
            if(((Number)ExtendedAePackets.field(screen,"visibleRows")).intValue()!=rows){
                setField(screen,"visibleRows",rows);ExtendedAePackets.invoke(screen,"resetScrollbar");
            }
        }
        if(name.equals("GuiPatternModifier")){
            Object multi=ExtendedAePackets.field(screen,"multiBtns");int[]positions={7,37,67,97,130};int i=0;
            for(Object button:(java.util.List<?>)multi)position(button,x+positions[i++],y+35);
            position(ExtendedAePackets.field(screen,"replace"),x+120,y+35);
            position(ExtendedAePackets.field(screen,"clone"),x+79,y+51);
        }else if(name.equals("GuiWirelessConnector")){
            retain(screen,"siu_highlight",ExtendedAePackets.field(screen,"highlight"),152,24);
            position(ExtendedAePackets.field(screen,"statusIcon"),x+152,y-4);
            position(ExtendedAePackets.field(screen,"highlight"),x+152,y+24);
            Object remote=ExtendedAePackets.field(screen,"remote");position(remote,x+24,y+76);ExtendedAePackets.invoke(remote,"refreshBounds");
        }else if(name.equals("GuiWirelessHub")){
            Object[]remotes=(Object[])ExtendedAePackets.field(screen,"remotes"),highlight=(Object[])ExtendedAePackets.field(screen,"highlightBtn"),disconnect=(Object[])ExtendedAePackets.field(screen,"disconnectBtn");
            for(int i=0;i<remotes.length;i++){int px=x+(i<4?37:120),py=y+44+i%4*28;position(remotes[i],px,py);position(highlight[i],px-18,py);position(disconnect[i],px+18,py);}
        }
    }
    /** A native corner tab is positioned against the outer frame, not its inset work area. */
    @SuppressWarnings("unchecked") private static void restoreRaisedTabs(AEBaseScreen<?> screen){
        int lift=inset(screen);if(lift==0)return;
        var container=ExtendedAePackets.field(screen,"widgets");
        var widgets=(java.util.Map<String,net.minecraft.client.gui.components.AbstractWidget>)ExtendedAePackets.field(container,"widgets");
        var styles=(java.util.Map<String,appeng.client.gui.style.WidgetStyle>)ExtendedAePackets.field(screen.getStyle(),"widgets");
        for(var entry:widgets.entrySet()){
            var widget=entry.getValue();var style=styles.get(entry.getKey());
            if(widget instanceof appeng.client.gui.widgets.TabButton&&style!=null&&style.getTop()!=null&&style.getTop()<0
                    &&widget.getX()>=screen.getGuiLeft()+screen.desktopContentBounds().getWidth()/2)
                widget.setY(screen.getGuiTop()+style.getTop()-lift);
        }
    }
    public static boolean virtualSlot(net.minecraft.world.inventory.Slot slot){
        for(Class<?> type=slot.getClass();type!=null;type=type.getSuperclass())
            if(type.getName().endsWith(".AssemblerMatrixSlot")||type.getName().endsWith(".OutputResultSlot"))return true;
        return false;
    }
    /** Addon action slots aren't vanilla pickup slots; invoke their native override. */
    public static boolean clickActionSlot(AEBaseScreen<?> screen,com.salts_inventory_update.api.client.input.MouseButtonEvent event){
        var slot=screen.desktopSlotAt(event.x(),event.y());
        if(slot==null||!virtualSlot(slot)||event.button()<0||event.button()>2)return false;
        var kind=event.button()==2?net.minecraft.world.inventory.ClickType.CLONE:
                net.minecraft.client.gui.screens.Screen.hasShiftDown()?net.minecraft.world.inventory.ClickType.QUICK_MOVE:net.minecraft.world.inventory.ClickType.PICKUP;
        // Match the protected Minecraft override by its descriptor, since its
        // published name differs between Fabric intermediary and Forge SRG.
        for(Class<?> type=screen.getClass();type!=null&&type.getName().contains(".extendedae.");type=type.getSuperclass())
            for(var method:type.getDeclaredMethods())if(method.getReturnType()==void.class&&java.util.Arrays.equals(method.getParameterTypes(),new Class<?>[]{net.minecraft.world.inventory.Slot.class,int.class,int.class,net.minecraft.world.inventory.ClickType.class}))try{
                method.setAccessible(true);method.invoke(screen,slot,slot.index,event.button(),kind);return true;
            }catch(ReflectiveOperationException error){throw new IllegalStateException("Native Extended AE action slot",error);}
        throw new IllegalStateException("Missing Extended AE action-slot override");
    }
    public static void afterForeground(Object owner){
        if(!(owner instanceof AEBaseScreen<?> screen)||!screen.isDesktopHosted())return;
        Object buttons=ExtendedAePackets.optionalField(screen,"highlightBtns");
        if(buttons instanceof java.util.Map<?,?> map){
            var styles=(java.util.Map<?,?>)ExtendedAePackets.field(screen.getStyle(),"widgets");
            for(var entry:map.entrySet()){
                String name="siu_highlight_row_"+entry.getKey();
                if(!styles.containsKey(name)){
                    var button=(net.minecraft.client.gui.components.AbstractWidget)entry.getValue();
                    retain(screen,name,button,button.getX()-screen.getGuiLeft(),button.getY()-screen.getGuiTop());
                }
            }
        }
    }
    private static void setField(Object owner,String name,Object value){for(Class<?> type=owner.getClass();type!=null;type=type.getSuperclass())try{var f=type.getDeclaredField(name);f.setAccessible(true);f.set(owner,value);return;}catch(NoSuchFieldException ignored){}catch(ReflectiveOperationException e){throw new IllegalStateException(e);}throw new IllegalStateException(name);}
    private static void position(Object widget,int x,int y){
        if(widget instanceof net.minecraft.client.gui.components.AbstractWidget nativeWidget) nativeWidget.setPosition(x,y);
        else ExtendedAePackets.invoke(widget,"setPosition",x,y);
    }
}
