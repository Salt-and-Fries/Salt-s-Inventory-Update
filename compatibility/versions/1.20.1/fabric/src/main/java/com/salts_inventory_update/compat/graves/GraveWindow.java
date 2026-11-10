package com.salts_inventory_update.compat.graves;

import com.salts_inventory_update.api.client.desktop.*;
import com.salts_inventory_update.api.client.input.MouseButtonEvent;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.resources.ResourceLocation;

/** Approved 36-cell storage grid and a separate, frameless native action row. */
final class GraveWindow implements DesktopWindowDefinition<ChestMenu,Void> {
    @Override public DesktopWindowSize defaultSize(DesktopWindowSetupContext<ChestMenu> c){return DesktopWindowSize.of(190,138);}
    @Override public DesktopWindowSize minSize(DesktopWindowContext<ChestMenu,Void> c){return DesktopWindowSize.of(190,138);}
    @Override public DesktopWindowSize maxSize(DesktopWindowContext<ChestMenu,Void> c){return minSize(c);}
    @Override public DesktopResizePolicy resizePolicy(DesktopWindowContext<ChestMenu,Void> c){return DesktopResizePolicy.FIXED;}
    private int x(DesktopWindowContext<?,?> c,int index){return c.contentX()+7+(index%9)*18;}
    private int y(DesktopWindowContext<?,?> c,int index){return c.contentY()+(index<36?1+(index/9)*18:87);}
    @Override public void render(DesktopRenderContext<ChestMenu,Void> c){
        for(int i=0;i<36;i++)c.slot(i,x(c,i),y(c,i));
        c.fill(c.contentX()+6,c.contentY()+78,c.contentX()+168,c.contentY()+79,0xFF7E7E7E);
        for(int i=36;i<45;i++)c.texturelessSlot(i,x(c,i),y(c,i));
    }
    @Override public DesktopSlotHit slotAt(DesktopSlotContext<ChestMenu,Void> c,double mx,double my){
        for(int i=0;i<36;i++)if(c.contains(mx,my,x(c,i)-1,y(c,i)-1,18,18))return DesktopSlotHit.of(i,x(c,i),y(c,i));return null;
    }
    @Override public boolean mouseClicked(DesktopInputContext<ChestMenu,Void> c,MouseButtonEvent e,boolean twice){
        if(e.x()<c.contentX()+6||e.x()>=c.contentX()+168||e.y()<c.contentY()+86||e.y()>=c.contentY()+104)return false;
        int i=36+((int)e.x()-c.contentX()-6)/18;
        if(e.button()<=1&&!c.model().getSlot(i).getItem().isEmpty())c.sendPayload(UniversalGravesWindows.ACTION,new byte[]{(byte)i,(byte)e.button()});
        return true;
    }
    @Override public boolean appendTooltip(DesktopRenderContext<ChestMenu,Void> c,int mx,int my){
        for(int i=36;i<45;i++)if(mx>=x(c,i)-1&&mx<x(c,i)+17&&my>=y(c,i)-1&&my<y(c,i)+17&&!c.model().getSlot(i).getItem().isEmpty()){
            c.tooltip(c.model().getSlot(i).getItem(),mx,my);return true;
        }return false;
    }
    @Override public void customPayload(DesktopWindowContext<ChestMenu,Void> c,ResourceLocation channel,byte[] data){
        if(!channel.equals(UniversalGravesWindows.STATE)||data.length!=16)return;
        var b=java.nio.ByteBuffer.wrap(data);var m=(GraveClientMenu)c.model();m.real=b.getLong();m.take=b.getLong();
    }
    @Override public String layoutRevision(DesktopWindowContext<ChestMenu,Void> c){return "universal-grave-four-rows-native-footer-1";}
}
