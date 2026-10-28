package com.salts_inventory_update.client.automatic;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import com.google.gson.GsonBuilder;
import com.salts_inventory_update.api.client.desktop.*;
import com.salts_inventory_update.api.client.input.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/** Explicit developer tool. Editing consumes pointer events; it never performs inventory actions. */
final class NativeWindowInspector {
    static final int HEIGHT=96;
    private static final String[] ACTIONS={"Source","Use","Add","Move","Hide","Padding +","Export","Reset","Exit"};
    final NativeTemplateWindow owner;
    DesktopNativeLayout edited;
    boolean source,move;
    DesktopWindowBounds selection;
    DesktopNativeLayout.Point start;
    int startX,startY,endX,endY;
    String movingRegion,status="Drag a selection; Source shows the original UI.";
    NativeWindowInspector(NativeTemplateWindow owner){this.owner=owner;}
    void start(){owner.inspecting=true;edited=owner.layout();source=false;move=false;selection=null;cancel();}
    DesktopNativeLayout override(DesktopNativeView view){
        if(owner.inspecting&&source){var b=DesktopNativeView.union(List.of(view.imageBounds(),view.automaticBounds())).orElseThrow();return DesktopNativeLayout.builder().keep("source",b,0,0).padding(1).build();}
        return edited;
    }
    List<DesktopWindowRegion> regions(DesktopWindowContext<?,?> c){return List.of(DesktopWindowRegion.visualAndInteractive(0,c.windowHeight()+6,width(c),HEIGHT));}
    private int width(DesktopWindowContext<?,?> c){return Math.max(216,Math.min(360,c.windowWidth()));}
    void render(DesktopRenderContext<?,?> c){
        int x=c.windowX(),y=c.windowY()+c.windowHeight()+6,w=width(c);
        c.fill(x,y,x+w,y+HEIGHT,0xFF162D36);c.text("Native UI inspector",x+5,y+4,0xFF8BE0DA,false);
        int cell=(w-8)/3;
        for(int i=0;i<ACTIONS.length;i++){int bx=x+4+i%3*cell,by=y+17+i/3*18;c.fill(bx,by,bx+cell-2,by+16,0xFF3E5962);c.text(ACTIONS[i],bx+3,by+4,0xFFFFFFFF,false);}
        c.text(c.trimToWidth(owner.layoutError.isEmpty()?status:owner.layoutError,w-10),x+5,y+75,owner.layoutError.isEmpty()?0xFFD0DFE3:0xFFFF9999,false);
        c.text(source?"Source view (read-only)":move?"Move: drag a region":"Selection: drag; Use / Add / Hide",x+5,y+85,0xFF8BE0DA,false);
    }
    void renderSelection(DesktopRenderContext<?,?> c){
        for(var slot:owner.view.slots())if(!slot.player()&&slot.active())for(var p:owner.layout().placements(slot.bounds()))outline(c,p.bounds(),0x7744EAD0);
        for(var widget:owner.view.widgets())if(widget.visible())for(var p:owner.layout().placements(widget.bounds()))outline(c,p.bounds(),0x9989AEFF);
        if(selection!=null)for(var p:owner.layout().placements(selection))outline(c,p.bounds(),0xFFFFFF00);
        if(start!=null&&!move){var b=box(startX,startY,endX,endY);for(var p:owner.layout().placements(b))outline(c,p.bounds(),0xFFFFFF00);}
    }
    private void outline(DesktopRenderContext<?,?> c,DesktopWindowBounds b,int color){
        int x=c.contentX()+b.x()-owner.scrollX,y=c.contentY()+b.y()-owner.scrollY,w=b.width(),h=b.height();
        c.fill(x,y,x+w,y+1,color);c.fill(x,y+h-1,x+w,y+h,color);c.fill(x,y,x+1,y+h,color);c.fill(x+w-1,y,x+w,y+h,color);
    }
    boolean clicked(DesktopInputContext<?,?> c,MouseButtonEvent e){
        int x=c.windowX(),y=c.windowY()+c.windowHeight()+6,w=width(c);
        if(e.x()>=x&&e.x()<x+w&&e.y()>=y&&e.y()<y+HEIGHT){
            int row=(int)(e.y()-y-17)/18,col=(int)(e.x()-x-4)/Math.max(1,(w-8)/3);
            if(e.y()>=y+17&&row>=0&&row<3&&col>=0&&col<3)action(row*3+col,c);return true;
        }
        if(e.x()<c.contentX()||e.y()<c.contentY()||e.x()>=c.contentX()+c.contentWidth()-8||e.y()>=c.contentY()+c.contentHeight()-8)return false;
        start=owner.sourceAt(c,e.x(),e.y()).orElse(null);if(start==null)return true;
        startX=endX=(int)Math.floor(start.x());startY=endY=(int)Math.floor(start.y());movingRegion=move?start.regionId():null;
        return true;
    }
    boolean dragged(DesktopInputContext<?,?> c,MouseButtonEvent e){
        if(start==null)return false;
        var region=owner.layout().regions().stream().filter(r->r.id().equals(start.regionId())).findFirst().orElse(null);
        if(region!=null){endX=(int)Math.floor(e.x()-c.contentX()+owner.scrollX-owner.layout().padding()-region.x()+region.source().x());
            endY=(int)Math.floor(e.y()-c.contentY()+owner.scrollY-owner.layout().padding()-region.y()+region.source().y());}
        return true;
    }
    boolean released(DesktopInputContext<?,?> c,MouseButtonEvent e){
        if(start==null)return false;dragged(c,e);
        if(move&&movingRegion!=null&&!source){var r=edited.regions().stream().filter(a->a.id().equals(movingRegion)).findFirst().orElse(null);
            if(r!=null)try{edited=edited.toBuilder().moveRegion(r.id(),Math.max(0,r.x()+endX-startX),Math.max(0,r.y()+endY-startY)).build();status="Region moved. Export to keep this profile.";}catch(RuntimeException failure){status="Move exceeded layout limits.";}
        }else{selection=box(startX,startY,endX,endY);status="Selected "+selection.width()+" x "+selection.height()+" at "+selection.x()+", "+selection.y();}
        cancel();return true;
    }
    private static DesktopWindowBounds box(int x,int y,int r,int b){return new DesktopWindowBounds(Math.min(x,r),Math.min(y,b),Math.abs(r-x)+1,Math.abs(b-y)+1);}
    private void action(int index,DesktopInputContext<?,?> c){
        if(edited==null&&index!=7)edited=owner.layout();
        try{switch(index){
            case 0->{source=!source;move=false;owner.scrollX=owner.scrollY=0;status=source?"Select any original UI region.":"Composed preview.";}
            case 1->{if(selection==null){status="Select a region first.";break;}edited=DesktopNativeLayout.builder().keep("region/1",selection,0,0).padding(4).build();source=false;owner.scrollX=owner.scrollY=0;status="Selection is the new native base.";}
            case 2->{if(selection==null){status="Select a region first.";break;}edited=edited.toBuilder().keep("region/"+(edited.regions().size()+1),selection,0,Math.max(0,edited.height()-edited.padding()*2)+4).build();source=false;status="Region added below the existing pieces.";}
            case 3->{source=false;move=!move;status="Drag a region to move it.";}
            case 4->{if(selection==null){status="Select a region first.";break;}edited=edited.toBuilder().hide(selection).build();source=false;status="Selection hidden, including its native input.";}
            case 5->{edited=edited.toBuilder().padding(Math.min(256,edited.padding()+2)).build();status="Padding: "+edited.padding()+" GUI pixels.";}
            case 6->export(c);
            case 7->{edited=null;source=false;move=false;selection=null;status="Restored the registered/native layout.";}
            case 8->{owner.inspecting=false;source=false;cancel();}
        }}catch(RuntimeException failure){status="Invalid edit: "+failure.getMessage();}
    }
    private void export(DesktopWindowContext<?,?> c){
        if(!owner.layoutError.isEmpty()){status="Correct the highlighted layout error before exporting.";return;}
        try{
            var menu=BuiltInRegistries.MENU.getKey(owner.initial.screen.getMenu().getType());
            var json=new com.google.gson.JsonObject();json.addProperty("menu",menu.toString());json.addProperty("screenType",owner.view.screenType());
            json.addProperty("minecraft",com.salts_inventory_update.VersionInfo.MINECRAFT_VERSION);json.add("layout",DesktopNativeProfiles.encode(edited==null?owner.layout():edited));
            var folder=c.minecraft().gameDirectory.toPath().resolve("config/salts_inventory_update/native-windows");Files.createDirectories(folder);
            var file=folder.resolve((menu.getNamespace()+"__"+menu.getPath()).replaceAll("[^a-zA-Z0-9._-]","_")+".json");
            Files.writeString(file,new GsonBuilder().setPrettyPrinting().create().toJson(json),StandardCharsets.UTF_8);
            NativeProfileFiles.reload();com.salts_inventory_update.client.DesktopContainerClient.refreshNativeTemplates();
            c.minecraft().player.displayClientMessage(Component.literal("Native UI profile saved: "+file.getFileName()),false);
            status="Exported. Reopen this container to load it.";
        }catch(java.io.IOException failure){status="Export failed: "+failure.getMessage();}
    }
    boolean key(KeyEvent e){if(e.key()==GLFW.GLFW_KEY_ESCAPE){owner.inspecting=false;source=false;cancel();return true;}return false;}
    void cancel(){start=null;movingRegion=null;}
}
