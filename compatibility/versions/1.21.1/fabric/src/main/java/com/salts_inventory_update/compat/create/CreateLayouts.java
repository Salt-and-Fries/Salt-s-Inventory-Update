package com.salts_inventory_update.compat.create;

import com.salts_inventory_update.api.client.desktop.*;
import java.util.*;
import net.minecraft.world.inventory.AbstractContainerMenu;

/** Preserve assembled native panels. One map owns both painting and input. */
final class CreateLayouts {
    static DesktopWindowBounds box(int x,int y,int w,int h){return new DesktopWindowBounds(x,y,w,h);}
    static final class Plan {
        final DesktopNativeView v;final DesktopNativeLayout.Builder b=DesktopNativeLayout.builder();
        final Set<String> handled=new HashSet<>();int w,h;
        Plan(DesktopNativeView v,int w,int h){this.v=v;this.w=w;this.h=h;}
        Plan keep(String id,DesktopWindowBounds source,int x,int y){b.keep(id,source,x,y);return this;}
        Plan keep(String id,int sx,int sy,int sw,int sh,int x,int y){return keep(id,box(sx,sy,sw,sh),x,y);}
        Plan widget(DesktopNativeView.Widget a,int x,int y){b.move(a.id(),a.bounds(),x,y);handled.add(a.id());return this;}
        Plan widget(int sx,int sy,int x,int y){
            v.widgets().stream().filter(a->a.visible()&&Math.abs(a.bounds().x()-sx)<=1&&Math.abs(a.bounds().y()-sy)<=1)
                .findFirst().ifPresent(a->widget(a,x,y));return this;
        }
        Plan preview(int sx,int sy,int sw,int sh){
            // Preserve Create's outboard, original-scale model without widening
            // the working panel or adding a differently colored picture card.
            var region=new DesktopNativeRegion("native-companion",box(sx,sy,sw,sh),w+22,24+Math.max(0,h-sh),false,List.of(),true);
            var player=v.slots().stream().filter(DesktopNativeView.Slot::player).filter(s->s.bounds().x()>=0&&s.bounds().y()>=0).toList();
            if(!player.isEmpty()){
                int px=player.stream().mapToInt(s->s.bounds().x()).min().orElseThrow()-7;
                int py=player.stream().mapToInt(s->s.bounds().y()).min().orElseThrow()-17;
                region=region.cut(box(px,py,176,108));
            }
            b.region(region);return this;
        }
        DesktopNativeLayout finish(){
            var layout=b.canvas(w,h).build();int extra=0;
            for(var s:v.slots())if(!s.player()&&s.active()&&s.bounds().x()>=0&&s.bounds().y()>=0&&layout.placements(s.bounds()).isEmpty()){
                b.hide(s.bounds()).keep("extra-cell/"+s.menuSlotId(),s.bounds(),w+5,extra);extra+=s.bounds().height()+3;layout=b.build();
            }
            int playerTop=v.slots().stream().filter(DesktopNativeView.Slot::player).filter(s->s.bounds().y()>=0).mapToInt(s->s.bounds().y()).min().orElse(Integer.MAX_VALUE);
            for(var a:v.widgets())if(a.visible()&&!handled.contains(a.id())&&a.bounds().y()<playerTop&&layout.placements(a.bounds()).isEmpty()){
                b.hide(a.bounds()).keep("extra-"+a.id(),a.bounds(),w+5,extra);extra+=a.bounds().height()+3;layout=b.build();
            }
            return b.build();
        }
    }
    private static Plan panel(DesktopNativeView v,int sx,int sy,int w,int h){return new Plan(v,w,h).keep("native-panel",sx,sy,w,h,0,0);}
    static DesktopNativeLayout build(CreateWindows.Kind kind,DesktopNativeView v,AbstractContainerMenu menu,CreateWindows.State state){
        var screen=CreateNativeAccess.screen(menu);
        return switch(kind){
            case TOOLBOX->nativeToolbox(v);
            case MOUNTED_TOOLBOX->nativeToolbox(v);
            case SCHEMATICANNON->cannon(v,state);
            case SCHEMATIC_TABLE->table(v);
            case BLUEPRINT->blueprint(v);
            case FROGPORT->frogport(v,screen,state);
            case REQUESTER->nativePanel(v,3,14,232,106,235,35,85,105).finish();
            case CATEGORIES->categories(v,screen,state);
            case STOCK->stock(v);
            case GAUGE->nativePanel(v,-5,14,184,74,179,30,80,90).finish();
            case FILTER->nativePanel(v,0,14,214,85,214,0,96,110).finish();
            case ATTRIBUTE->attribute(v,screen,state);
            case PACKAGE_FILTER->nativePanel(v,0,14,218,65,218,0,96,100).finish();
            case CONTROLLER->nativePanel(v,0,14,179,95,179,25,100,100).finish();
            case SCHEDULE->schedule(v,screen,state);
            case AMMO->ammo(v);
            case TIMED_FUZE,PROXIMITY_FUZE,INERTIA_FUZE,IMPACT_FUZE->nativePanel(v,0,14,179,69,179,16,96,100).finish();
        };
    }
    static DesktopNativeLayout toolbox(DesktopNativeView v){
        // The original inventory overlaps the left of the native footer.
        // Preserve the entire ring; restore that footer from its original atlas.
        var p=panel(v,30,14,188,133);p.h=157;p.widget(185,147,155,133);
        return p.preview(218,100,110,130).finish();
    }
    private static Plan nativePanel(DesktopNativeView v,int sx,int sy,int w,int h,int px,int py,int pw,int ph){
        var p=panel(v,sx,sy,w,h).preview(px,py,pw,ph);
        p.b.moveRegion("native-companion",px-sx,py).scrollable(false);
        return p;
    }
    static DesktopNativeLayout nativeToolbox(DesktopNativeView v){
        // The player's inventory covers part of the native footer. Keep its
        // original atlas restoration and the original confirm-button offset.
        var p=nativePanel(v,30,14,188,133,218,100,110,130);
        p.h=157;p.widget(185,147,155,133);
        return p.finish();
    }
    static DesktopNativeLayout cannon(DesktopNativeView v,CreateWindows.State state){
        state.settings=v.widgets().stream().anyMatch(w->w.visible()&&w.bounds().x()==33&&w.bounds().y()==111);
        // Fuel caps, printer, indicators, playback and inline settings stay together.
        var p=panel(v,0,SchematicannonChrome.BODY_TOP,213,141-SchematicannonChrome.BODY_TOP)
            .preview(213,25,100,145);
        // Keep the companion at its native offset from the title strip as well.
        p.b.moveRegion("native-companion",213,27).scrollable(false);
        return p.finish();
    }
    static DesktopNativeLayout table(DesktopNativeView v){
        var p=nativePanel(v,0,14,214,71,224,15,80,90);
        for(var a:v.widgets())if(a.visible()&&a.bounds().x()==206&&a.bounds().width()==18){
            // Its native refresh control is deliberately outside the frame.
            // Exclude its partial overlap before mapping the complete button.
            p.b.hide(a.bounds()).exterior("native-refresh",a.bounds(),a.bounds().x(),a.bounds().y());
            p.handled.add(a.id());
        }
        return p.finish();
    }
    static DesktopNativeLayout blueprint(DesktopNativeView v){
        // Complete arrow and native overlapping result pair, without reslicing.
        return nativePanel(v,0,14,179,95,179,45,90,100).finish();
    }
    static DesktopNativeLayout frogport(DesktopNativeView v,Object screen,CreateWindows.State state){
        var address=v.widgets().stream().filter(w->w.bounds().height()==10&&w.bounds().y()<0).findFirst().orElse(null);
        state.renaming=CreateNativeAccess.field(screen,"addressBox") instanceof net.minecraft.client.gui.components.EditBox box&&box.isFocused();
        int header=Boolean.FALSE.equals(CreateNativeAccess.field(screen,"frogMode"))?24:17;
        var p=panel(v,0,0,220,82).preview(220,0,96,105);
        p.b.moveRegion("native-companion",220,header).scrollable(false);
        if(address!=null){p.handled.add(address.id());state.nameSource=address.bounds();
            p.b.exterior("name-input",address.bounds(),12,header-11);
        }
        return p.finish();
    }
    static DesktopNativeLayout categories(DesktopNativeView v,Object screen,CreateWindows.State state){
        state.editing=CreateNativeAccess.field(screen,"editingItem")!=null;
        var p=state.editing?panel(v,0,13,200,71):panel(v,0,18,200,113);
        if(state.editing){
            // Native atlas transparency must not retain the modal dimmer.
            p.b.hide(box(0,13,1,39)).hide(box(191,13,9,39)).hide(box(192,52,8,32));
        }else{p.preview(200,75,80,90);p.b.moveRegion("native-companion",200,75);}
        p.b.scrollable(false);
        return p.finish();
    }
    static DesktopNativeLayout stock(DesktopNativeView v){
        int height=v.imageBounds().height();var p=panel(v,-15,14,256,height-14);
        // Transparent atlas margin beside the board must not retain a sliver
        // of the original outboard stockkeeper model. Keep the complete curl.
        p.b.hide(box(-15,36,18,height-36));
        p.b.region(new DesktopNativeRegion("native-companion",box(-75,height-140,78,145),-60,height-140,false,List.of(),true)).scrollable(false);
        return p.finish();
    }
    static DesktopNativeLayout attribute(DesktopNativeView v,Object screen,CreateWindows.State state){
        state.ruleCount=Math.max(0,CreateNativeAccess.list(screen,"selectedAttributes").size()-1);
        // The flat parchment fits before x=320; the remaining old capture
        // margin was transparent. Keep the full native frame at small GUI sizes.
        return nativePanel(v,0,14,241,71,241,0,79,110).finish();
    }
    static DesktopNativeLayout schedule(DesktopNativeView v,Object screen,CreateWindows.State state){
        state.editing=CreateNativeAccess.field(screen,"editingDestination")!=null||CreateNativeAccess.field(screen,"editingCondition")!=null;
        if(!state.editing)return nativePanel(v,0,14,256,212,256,140,64,95).finish();
        // Native editor wings and rails. Its bottom seven pixels overlap the
        // original inventory, so those pixels are restored from the editor atlas.
        var p=panel(v,-2,54,256,68);p.h=75;p.b.scrollable(false);
        // The original modal gradient sits behind the editor. Exclude only
        // transparent atlas edge pixels so it cannot become a rectangular card.
        for(int[] b:new int[][]{{-2,54,3,1},{251,54,3,1},{-2,55,4,59},{250,55,4,59},{-2,114,3,8},{251,114,3,8}})
            p.b.hide(box(b[0],b[1],b[2],b[3]));
        var popup=CreateNativeAccess.destinationPopup(CreateNativeAccess.field(screen,"destinationSuggestions"),screen instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> s?s:null,v);
        if(popup!=null)p.b.hide(popup).exterior("destination-suggestions",popup,popup.x()+2,popup.y()-40);
        return p.finish();
    }
    static DesktopNativeLayout ammo(DesktopNativeView v){
        int width=v.imageBounds().width();return nativePanel(v,0,14,width,69,width,20,100,110).finish();
    }
}
