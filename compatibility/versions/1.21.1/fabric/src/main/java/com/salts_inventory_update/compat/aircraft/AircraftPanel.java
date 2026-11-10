package com.salts_inventory_update.compat.aircraft;

import java.util.*;
import com.salts_inventory_update.api.client.desktop.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import static com.salts_inventory_update.compat.aircraft.AircraftAccess.*;

/** Native 22-pixel equipment art, complete boiler, and original cargo ordering. */
final class AircraftPanel implements DesktopWindowDefinition<AircraftDesktopMenu,AircraftPanel.State> {
    static final class State {int orphanTicks;}
    private static final ResourceLocation TEXTURE=ResourceLocation.fromNamespaceAndPath("immersive_aircraft","textures/gui/container/inventory.png");
    private static final ResourceLocation CONTROLS=ResourceLocation.fromNamespaceAndPath("salts_inventory_update","textures/gui/window_controls.png");
    private static final DesktopChromeMetrics WING_CHROME=new DesktopChromeMetrics(16,6,5,11,11,1,4,4,0);
    @Override public State createState(DesktopWindowSetupContext<AircraftDesktopMenu> c){return new State();}
    @Override public DesktopWindowSize defaultSize(DesktopWindowSetupContext<AircraftDesktopMenu> c){return DesktopWindowSize.of(c.model().cargoWing()?Math.max(c.model().width,c.minecraft().font.width("Starboard")+52):c.model().width,c.model().height);}
    @Override public DesktopWindowSize minSize(DesktopWindowContext<AircraftDesktopMenu,State> c){return DesktopWindowSize.of(c.model().cargoWing()?Math.max(c.model().width,c.fontWidth("Starboard")+52):c.model().width,c.model().height);}
    // Docked panes keep their author-defined placement. Salt's lock action resets
    // a pane to its standalone placement, so it does not apply to this group.
    @Override public boolean managementControlAllowed(DesktopWindowContext<AircraftDesktopMenu,State> c, DesktopWindowControl control) {
        return !c.model().cargoWing() || control == DesktopWindowControl.SORT_INVENTORY
            || control == DesktopWindowControl.SORT_WINDOWS || control == DesktopWindowControl.FOCUS
            || control == DesktopWindowControl.LOCK && c.model().followingParent();
    }
    @Override public DesktopWindowOptions options(DesktopWindowContext<AircraftDesktopMenu,State> c){return c.model().cargoWing()?new DesktopWindowOptions(c.model().followingParent,false,false,c.model().followingParent,false):new DesktopWindowOptions(true,true,true,false,true);}
    @Override public DesktopWindowDecoration<AircraftDesktopMenu,State> decoration(DesktopWindowContext<AircraftDesktopMenu,State> c){
        if(!c.model().cargoWing())return null;
        return new DesktopWindowDecoration<>(){
            @Override public DesktopChromeMetrics metrics(){return WING_CHROME;}
            @Override public void render(DesktopRenderContext<AircraftDesktopMenu,State> r,List<DesktopControlState> controls){
                r.windowNineSlice(DesktopThemeAssets.WINDOW,r.windowX(),r.windowY(),r.windowWidth(),r.windowHeight());
                r.text(r.originalTitle(),r.windowX()+6,r.windowY()+5,0xff404040,false);
                for(var state:controls){
                    int column=switch(state.control()){case SORT_INVENTORY->1;case SORT_WINDOWS->10;case FOCUS->0;default->-1;};
                    if(column<0)continue;var b=state.bounds();
                    r.texture(CONTROLS,b.x(),b.y(),column*11,(state.pressed()||state.selected()?2:state.hovered()||state.keyboardFocused()?1:0)*11,b.width(),b.height(),11,11,132,33);
                }
            }
        };
    }
    @Override public void opened(DesktopWindowContext<AircraftDesktopMenu,State> c){AircraftClient.opened(c);}
    @Override public void closed(DesktopWindowContext<AircraftDesktopMenu,State> c){AircraftClient.closed(c);}
    @Override public void tick(DesktopWindowContext<AircraftDesktopMenu,State> c){AircraftClient.tick(c);}
    private int x(DesktopWindowContext<AircraftDesktopMenu,State> c,AircraftDesktopMenu.Cell cell){return c.windowX()+(c.windowWidth()-c.model().bodyWidth)/2+cell.x;}
    private int y(DesktopWindowContext<AircraftDesktopMenu,State> c,AircraftDesktopMenu.Cell cell){return c.contentY()+cell.y;}
    private void texture(DesktopRenderContext<AircraftDesktopMenu,State> c,int x,int y,int u,int v,int w,int h){c.texture(TEXTURE,x,y,u,v,w,h,w,h,512,256);}
    @Override public void render(DesktopRenderContext<AircraftDesktopMenu,State> c){
        for(var cell:c.model().cells){int x=x(c,cell),y=y(c,cell);
            if(cell.kind.equals("inventory"))texture(c,x-1,y-1,284,0,18,18);
            else if(cell.kind.equals("boiler")){
                texture(c,x-4,y-18,318,0,24,39);
                if(((Number)call(c.model().vehicle,"getFuelUtilization")).floatValue()>0)texture(c,x-4,y-18,348,0,24,39);
            }else{
                int v=cell.slot.hasItem()?0:switch(cell.kind){case "weapon"->22;case "upgrade"->44;case "banner"->66;case "dye"->88;case "booster"->110;default->0;};
                texture(c,x-3,y-3,262,v,22,22);
            }
            c.texturelessSlot(cell.slot.index,x,y);
        }
    }
    @Override public DesktopSlotHit slotAt(DesktopSlotContext<AircraftDesktopMenu,State> c,double x,double y){for(var cell:c.model().cells){var hit=c.menuSlotHit(cell.slot.index,x(c,cell),y(c,cell),x,y);if(hit!=null)return hit;}return null;}
    @Override public boolean appendTooltip(DesktopRenderContext<AircraftDesktopMenu,State> c,int x,int y){
        for(var cell:c.model().cells)if(x>=x(c,cell)&&x<x(c,cell)+16&&y>=y(c,cell)&&y<y(c,cell)+16){
            if(cell.slot.hasItem())c.tooltip(cell.slot.getItem(),x,y);
            else {var tooltip=(Optional<?>)call(cell.description,"getToolTip");if(tooltip.isPresent()){@SuppressWarnings("unchecked")var lines=(List<Component>)tooltip.get();c.tooltip(lines,x,y);}}
            return true;
        }return false;
    }
    @Override public String layoutRevision(DesktopWindowContext<AircraftDesktopMenu,State> c){return "aircraft-parent-owned-cargo-3";}
}
