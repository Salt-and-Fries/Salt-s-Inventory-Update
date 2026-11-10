package com.salts_inventory_update.compat.create;

import com.salts_inventory_update.api.client.desktop.*;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** Original Create title strips, with manager-owned controls in the same hit rectangles. */
enum CreateNativeFrame {
    TOOLBOX("toolbox",0,0,188,5841956),
    TABLE("schematics",10,139,214,5263440),
    BLUEPRINT("curiosities_2",0,109,179,0xFFFFFF),
    FILTER("filters",0,0,214,3158064),
    ATTRIBUTE("filters",0,99,241,5841956),
    PORT("create","frogport_and_mailbox",0,0,214,17,4013128,6,false),
    POSTBOX("create","frogport_and_mailbox",0,23,214,24,4013128,13,false),
    REQUESTER("requester",16,16,232,4013128),
    CATEGORIES("create","stock_keeper_categories",32,0,192,18,4013128,4,false),
    STOCK("stock_keeper",0,0,256,7424576),
    GAUGE("requester",16,160,184,4013128),
    PACKAGE_FILTER("filters_2",0,0,218,4013128),
    CONTROLLER("curiosities_2",0,0,179,5841956),
    SCHEDULE("schedule",0,0,256,5263440),
    SCHEDULE_EDITOR("schedule_2",0,0,256,5263440),
    AMMO("createbigcannons","backgrounds1",0,168,179,14,0xFFFFFF,3,true),
    CREATIVE_AMMO("createbigcannons","backgrounds2",0,0,199,14,5513551,3,false),
    TIMED_FUZE("createbigcannons","backgrounds1",0,0,179,14,0xFFFFFF,3,true),
    PROXIMITY_FUZE("createbigcannons","backgrounds1",0,83,179,14,0xFFFFFF,3,true);

    static final int TITLE_HEIGHT=14;
    static final DesktopWindowSurface SURFACE=new DesktopWindowSurface(DesktopSurfaceMode.SALT,
        new DesktopInsets(0,TITLE_HEIGHT,0,0),true,List.of(),DesktopViewportPolicy.CLAMP);
    static final DesktopChromeMetrics METRICS=new DesktopChromeMetrics(TITLE_HEIGHT,6,4,11,11,3,10,2,0);
    final ResourceLocation texture;final int u,v,width,height,color,textY;final boolean shadow;
    CreateNativeFrame(String asset,int u,int v,int width,int color){
        this("create",asset,u,v,width,TITLE_HEIGHT,color,4,false);
    }
    CreateNativeFrame(String namespace,String asset,int u,int v,int width,int height,int color,int textY,boolean shadow){
        texture=new ResourceLocation(namespace,"textures/gui/"+asset+".png");
        this.u=u;this.v=v;this.width=width;this.height=height;this.color=color;this.textY=textY;this.shadow=shadow;
    }
    static CreateNativeFrame of(CreateWindows.Kind kind){return switch(kind){
        case TOOLBOX,MOUNTED_TOOLBOX->TOOLBOX;case SCHEMATIC_TABLE->TABLE;case BLUEPRINT->BLUEPRINT;
        case FILTER->FILTER;case ATTRIBUTE->ATTRIBUTE;case FROGPORT->PORT;case REQUESTER->REQUESTER;
        case CATEGORIES->CATEGORIES;case STOCK->STOCK;case GAUGE->GAUGE;case PACKAGE_FILTER->PACKAGE_FILTER;
        case CONTROLLER->CONTROLLER;case SCHEDULE->SCHEDULE;case AMMO->AMMO;
        case TIMED_FUZE,INERTIA_FUZE,IMPACT_FUZE->TIMED_FUZE;case PROXIMITY_FUZE->PROXIMITY_FUZE;
        case SCHEMATICANNON->null;
    };}
    static CreateNativeFrame of(CreateWindows.Kind kind,CreateWindows.State state,net.minecraft.world.inventory.AbstractContainerMenu menu){
        if(kind==CreateWindows.Kind.FROGPORT)return Boolean.FALSE.equals(CreateNativeAccess.field(CreateNativeAccess.screen(menu),"frogMode"))?POSTBOX:PORT;
        if(kind==CreateWindows.Kind.SCHEDULE&&state!=null&&state.editing)return SCHEDULE_EDITOR;
        if(kind==CreateWindows.Kind.AMMO&&Boolean.TRUE.equals(CreateNativeAccess.invoke(menu,"isCreativeContainer")))return CREATIVE_AMMO;
        return of(kind);
    }
    DesktopWindowSurface surface(){return new DesktopWindowSurface(DesktopSurfaceMode.SALT,new DesktopInsets(0,height,0,0),true,List.of(),DesktopViewportPolicy.CLAMP);}
    DesktopChromeMetrics metrics(){
        // The editable address and pencil own the left part of port headers.
        // Reserve that area when the manager selects its compact controls menu.
        int reserve=this==PORT||this==POSTBOX?124:this==STOCK?24:this==SCHEDULE?20:6;
        // The schedule atlas has 14px transparent margins beside its title strip.
        int rightInset=this==SCHEDULE?24:this==STOCK?18:10;
        return new DesktopChromeMetrics(height,reserve,textY,11,11,3,rightInset,Math.max(2,height-13),0);
    }
    private static String title(DesktopRenderContext<?,?> c,String text,int width){
        if(c.minecraft().font.width(text)<=width)return text;
        return c.trimToWidth(text,Math.max(0,width-c.minecraft().font.width("...")))+"...";
    }
    void paint(DesktopRenderContext<?,?> c,List<DesktopControlState> controls,Component title){
        int x=c.windowX(),y=c.windowY();
        // Includes the exact native bevel and transparent margin above the footer point.
        c.texture(texture,x,y,u,v,width,height,width,height,256,256);
        int right=controls.stream().filter(s->s.control()!=DesktopWindowControl.RESIZE_GRIP)
            .mapToInt(s->s.bounds().x()-3).min().orElse(x+width-14);
        if((this==PORT||this==POSTBOX)&&c.state() instanceof CreateWindows.State state){
            if(state.renaming&&CreateNativeAccess.field(CreateNativeAccess.screen((net.minecraft.world.inventory.AbstractContainerMenu)c.model()),"addressBox") instanceof net.minecraft.client.gui.components.EditBox box){
                c.withClip(x+12,y+height-11,110,10,()->{
                    var g=c.nativeGraphics();g.pose().pushPose();
                    try{g.pose().translate(x+12-box.getX(),y+height-11-box.getY(),0);box.render(g,-10000,-10000,0);}
                    finally{g.pose().popPose();}
                });
            }else c.text(title(c,title.getString(),106),x+12,y+textY,0xFF000000|color,false);
            c.texture(texture,x+124,y+height-14,230,3,13,13,13,13,256,256);
        }else{
            var metrics=metrics();
            c.text(title(c,title.getString(),Math.max(0,right-x-metrics.titleLeftPadding())),
                x+metrics.titleLeftPadding(),y+textY,0xFF000000|color,shadow);
        }
        CreateChrome.controls(c,controls);
    }
}
