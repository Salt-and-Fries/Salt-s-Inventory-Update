package com.salts_inventory_update.compat.modernindustrialization;

import com.salts_inventory_update.api.client.desktop.*;
import java.util.*;
import net.minecraft.world.inventory.AbstractContainerMenu;

/** A single coordinate map owns native pixels, complete cells and input. */
public final class ModernIndustrializationLayouts {
    private ModernIndustrializationLayouts() { }
    public static DesktopNativeLayout layout(DesktopNativeView view,AbstractContainerMenu menu,int titleWidth){
        boolean machine=view.screenType().endsWith(".MachineScreen"),itemPipe=view.screenType().endsWith(".ItemPipeScreen");
        int imageWidth=view.imageBounds().width();
        int playerTop=view.slots().stream().filter(DesktopNativeView.Slot::player).mapToInt(s->s.bounds().y()+1).min().orElse(view.imageBounds().height()-82);
        int top=14,bottom=playerTop-13;
        var mainCells=view.slots().stream().filter(s->!s.player()&&s.active()&&s.bounds().x()>=4&&s.bounds().x()+s.bounds().width()<=imageWidth-4).map(DesktopNativeView.Slot::bounds).toList();
        for(var cell:mainCells)bottom=Math.max(bottom,cell.y()+cell.height()+1);
        int bodyLeft=4,bodyRight=imageWidth-4;
        if(machine){
            var artwork=new ArrayList<DesktopWindowBounds>(mainCells);
            // Always include every component's full drawable extent, even
            // when inactive (for example the gunpowder overclock indicator).
            ModernIndustrializationContents.components(menu,imageWidth,playerTop).forEach(p->artwork.add(p.bounds()));
            view.widgets().stream().filter(ModernIndustrializationLayouts::machineWidget).filter(w->w.visible()&&w.bounds().x()>=4&&w.bounds().x()+w.bounds().width()<=imageWidth-4&&w.bounds().y()>4)
                .map(DesktopNativeView.Widget::bounds).forEach(artwork::add);
            var area=DesktopNativeView.union(artwork);
            if(area.isPresent()){
                var content=area.get();bodyLeft=Math.max(4,content.x()-6);bodyRight=Math.min(imageWidth-4,content.x()+content.width()+6);
                top=Math.max(4,content.y()-6);bottom=Math.max(content.y()+content.height(),Math.min(playerTop-1,content.y()+content.height()+6));
            }else{bodyLeft=imageWidth/2-16;bodyRight=bodyLeft+32;top=24;bottom=40;}
        }
        int bodyWidth=bodyRight-bodyLeft,canvasWidth=machine?Math.max(bodyWidth,Math.min(128,titleWidth+30)):Math.max(bodyWidth,titleWidth+30),dx=(canvasWidth-bodyWidth)/2;
        var header=view.widgets().stream().filter(ModernIndustrializationLayouts::machineWidget).filter(w->w.visible()&&w.bounds().width()==20&&w.bounds().height()==20
            &&w.bounds().x()>=0&&w.bounds().x()<imageWidth&&w.bounds().y()<=4).sorted(Comparator.comparingInt(w->w.bounds().x())).toList();
        int utility=machine&&!header.isEmpty()?23:0,warning=itemPipe?15:0;
        var builder=DesktopNativeLayout.builder().canvas(canvasWidth,utility+warning+bottom-top);
        builder.keep("native-working-body",new DesktopWindowBounds(bodyLeft,top,bodyWidth,bottom-top),dx,utility+warning);
        if(bottom>playerTop-12) {
            // A low machine cell may share Y with the old Inventory label.
            // Hide only its left label band, never the machine cell rim.
            int labelEnd=mainCells.stream().filter(b->b.y()+b.height()>playerTop-12).mapToInt(DesktopWindowBounds::x).min().orElse(60);
            if(!machine&&labelEnd>6)builder.hide(new DesktopWindowBounds(6,playerTop-12,Math.min(56,labelEnd-6),10));
        }
        for(int i=0;i<header.size()&&machine;i++){
            var original=header.get(i).bounds();
            // Only the native widget source moves above the image. This keeps
            // the first cell row free from the old 20px button overlap.
            builder.keep("native-machine-action/"+i,new DesktopWindowBounds(original.x(),-24,20,20),canvasWidth-header.size()*22+i*22,0);
        }
        if(itemPipe)builder.keep("empty-filter-warning",new DesktopWindowBounds(55,5,91,9),dx+4,0);
        if(machine){
            for(boolean left:new boolean[]{true,false}){
                var anchors=new ArrayList<DesktopWindowBounds>();
                view.slots().stream().filter(s->!s.player()&&s.active()).map(DesktopNativeView.Slot::bounds)
                    .filter(b->left?b.x()<0:b.x()>=imageWidth).forEach(anchors::add);
                view.widgets().stream().filter(ModernIndustrializationLayouts::machineWidget).filter(DesktopNativeView.Widget::visible).map(DesktopNativeView.Widget::bounds)
                    .filter(b->left?b.x()<0:b.x()>=imageWidth).forEach(anchors::add);
                var area=DesktopNativeView.union(anchors);
                if(area.isPresent()){
                    int sx=left?Math.min(-32,area.get().x()-8):imageWidth;
                    int width=left?-sx:Math.max(32,area.get().x()+area.get().width()+8-imageWidth);
                    // Shape selection expands its native tray into text rows
                    // and paired arrows. Retain the whole expanded frame too.
                    int h=Math.max(34,area.get().y()+area.get().height()+(!left||width>32?8:7)-10);
                    var source=new DesktopWindowBounds(sx,10,width,h);
                    var region=new DesktopNativeRegion(left?"native-shape-tray":"native-io-tray",source,left?1+sx:canvasWidth+11,22,true,List.of(),true);
                    // Replace the open inner edge with an opening beside the
                    // parent and a full rim below it, painted behind the frame.
                    region=region.cut(new DesktopWindowBounds(left?sx+width-4:sx,14,4,h-4));
                    builder.region(region);
                }
            }
        }
        return builder.build();
    }
    // FTB adds this global sidebar to every container screen. Its screen-space
    // bounds do not belong to the machine body, header or attached native trays.
    private static boolean machineWidget(DesktopNativeView.Widget widget){
        return !widget.type().equals("dev.ftb.mods.ftblibrary.sidebar.SidebarGroupGuiButton");
    }
}
