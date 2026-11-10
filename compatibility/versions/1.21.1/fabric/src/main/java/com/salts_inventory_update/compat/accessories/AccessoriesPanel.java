package com.salts_inventory_update.compat.accessories;

import java.util.*;
import java.nio.ByteBuffer;
import com.salts_inventory_update.api.client.desktop.*;
import com.salts_inventory_update.api.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** Salt frame joined above Character, with every real/cosmetic pair kept together. */
final class AccessoriesPanel implements DesktopWindowDefinition<AccessoriesDesktopMenu,AccessoriesPanel.State> {
    // Slot frames are 18 pixels wide: one pixel within a pair, five between pairs.
    private static final int COSMETIC_OFFSET=19,PAIR_PITCH=42,PAIR_GAP=5;
    private static int columns(int width,boolean scrolling){return Math.max(1,(width-(scrolling?23:12)+PAIR_GAP)/PAIR_PITCH);}
    static final class State {int scroll;boolean dragging;}
    private record Layout(int columns,int rows,int visible,int maxScroll,boolean scrolling) { }
    private record Cell(AccessoriesDesktopMenu.Cell model,int x,int y) { }
    @Override public State createState(DesktopWindowSetupContext<AccessoriesDesktopMenu> c){return new State();}
    @Override public DesktopWindowOptions options(DesktopWindowContext<AccessoriesDesktopMenu,State> c){return new DesktopWindowOptions(false,false,true,false,false);}
    private static int preferred(int width,int pairs){return 14+20*Math.max(1,(pairs+columns(width,false)-1)/columns(width,false));}
    @Override public DesktopWindowSize defaultSize(DesktopWindowSetupContext<AccessoriesDesktopMenu> c){return DesktopWindowSize.of(176,preferred(176,c.model().cells.size()/2));}
    @Override public DesktopWindowSize minSize(DesktopWindowContext<AccessoriesDesktopMenu,State> c){return DesktopWindowSize.of(54,1);}
    @Override public DesktopWindowAttachment attachment(DesktopWindowContext<AccessoriesDesktopMenu,State> c){
        int width=DesktopClientApi.currentWorkspace().stream().flatMap(w->w.windows().stream()).filter(w->w.handle().instanceId().equals(c.model().ownerWindow)).findFirst().map(w->w.handle().state().width()).orElse(c.windowWidth());
        return new DesktopWindowAttachment(c.model().ownerWindow,preferred(width,c.model().cells.size()/2),0,4);
    }
    @Override public DesktopWindowSurface surface(DesktopWindowContext<AccessoriesDesktopMenu,State> c){return DesktopWindowSurface.modOwned(List.of(DesktopWindowRegion.visualAndInteractive(0,0,c.windowWidth(),c.windowHeight())));}
    @Override public void opened(DesktopWindowContext<AccessoriesDesktopMenu,State> c){AccessoriesClient.opened(c.model().ownerWindow,c.windowHandle());}
    @Override public void closed(DesktopWindowContext<AccessoriesDesktopMenu,State> c){AccessoriesClient.closed(c.model().ownerWindow,c.windowHandle());}
    private Layout layout(DesktopWindowContext<AccessoriesDesktopMenu,State> c){
        int pairs=c.model().cells.size()/2,visible=Math.max(1,(c.windowHeight()-14)/20),columns=columns(c.windowWidth(),false);
        boolean scroll=(pairs+columns-1)/columns>visible;if(scroll)columns=columns(c.windowWidth(),true);
        int rows=Math.max(1,(pairs+columns-1)/columns),max=Math.max(0,rows-visible);c.state().scroll=Math.max(0,Math.min(max,c.state().scroll));
        return new Layout(columns,rows,Math.min(rows,visible),max,scroll);
    }
    private List<Cell> cells(DesktopWindowContext<AccessoriesDesktopMenu,State> c){
        var l=layout(c);var out=new ArrayList<Cell>();int first=c.state().scroll*l.columns,end=Math.min(c.model().cells.size()/2,first+l.visible*l.columns);
        for(int pair=first;pair<end;pair++){int local=pair-first,x=c.windowX()+7+(local%l.columns)*PAIR_PITCH,y=c.windowY()+8+(local/l.columns)*20;out.add(new Cell(c.model().cells.get(pair*2),x,y));out.add(new Cell(c.model().cells.get(pair*2+1),x+COSMETIC_OFFSET,y));}return out;
    }
    private static boolean inside(double x,double y,int bx,int by,int w,int h){return x>=bx&&x<bx+w&&y>=by&&y<by+h;}
    private Cell visibility(DesktopWindowContext<AccessoriesDesktopMenu,State> c,double x,double y){for(Cell cell:cells(c))if(!cell.model.cosmetic&&inside(x,y,cell.x+13,cell.y-2,5,5))return cell;return null;}
    private boolean bar(DesktopWindowContext<AccessoriesDesktopMenu,State> c,double x,double y){return layout(c).scrolling&&inside(x,y,c.windowX()+c.windowWidth()-15,c.windowY()+7,8,Math.max(1,c.windowHeight()-14));}
    private boolean viewport(DesktopWindowContext<AccessoriesDesktopMenu,State> c,double x,double y){return inside(x,y,c.windowX()+6,c.windowY()+6,c.windowWidth()-(layout(c).scrolling?23:12),Math.max(1,c.windowHeight()-12));}
    @Override public void render(DesktopRenderContext<AccessoriesDesktopMenu,State> c){
        var l=layout(c);c.windowNineSlice(DesktopThemeAssets.WINDOW,c.windowX(),c.windowY(),c.windowWidth(),c.windowHeight());
        if(c.model().cells.isEmpty()){c.text("No accessory slots",c.windowX()+8,c.windowY()+10,0xff404040,false);return;}
        c.withClip(c.windowX()+6,c.windowY()+6,Math.max(1,c.windowWidth()-(l.scrolling?23:12)),Math.max(1,c.windowHeight()-12),()->{
            for(Cell cell:cells(c))c.slot(cell.model.slot.index,cell.x,cell.y);
            c.rawGraphics().nextStratum();
            for(Cell cell:cells(c))if(!cell.model.cosmetic){int x=cell.x+13,y=cell.y-2;c.fill(x,y,x+5,y+5,0xff373737);c.fill(x+1,y+1,x+4,y+4,cell.model.render?0xffdadada:0xff6a6a6a);}
        });
        if(l.scrolling){int x=c.windowX()+c.windowWidth()-15,y=c.windowY()+7,height=Math.max(1,c.windowHeight()-14),thumb=Math.max(8,height*l.visible/l.rows),top=y+(height-thumb)*c.state().scroll/Math.max(1,l.maxScroll);c.fill(x,y,x+8,y+height,0xff555555);c.fill(x+1,top,x+7,top+thumb,0xffc6c6c6);c.fill(x+1,top,x+7,top+1,0xffffffff);}
    }
    @Override public DesktopSlotHit slotAt(DesktopSlotContext<AccessoriesDesktopMenu,State> c,double x,double y){
        if(!viewport(c,x,y)||bar(c,x,y)||visibility(c,x,y)!=null)return null;
        for(Cell cell:cells(c)){var hit=c.menuSlotHit(cell.model.slot.index,cell.x,cell.y,x,y);if(hit!=null)return hit;}return null;
    }
    @Override public boolean mouseClicked(DesktopInputContext<AccessoriesDesktopMenu,State> c,MouseButtonEvent e,boolean twice){
        if(viewport(c,e.x(),e.y())){var cell=visibility(c,e.x(),e.y());if(cell!=null){if(e.button()==0)c.sendPayload(AccessoriesIntegration.VISIBILITY,ByteBuffer.allocate(4).putInt(cell.model.slot.index).array());return true;}}
        if(bar(c,e.x(),e.y())){if(e.button()==0){c.state().dragging=true;drag(c,e.y());}return true;}return false;
    }
    private void drag(DesktopWindowContext<AccessoriesDesktopMenu,State> c,double y){var l=layout(c);int h=Math.max(1,c.windowHeight()-14),thumb=Math.max(8,h*l.visible/l.rows);double f=(y-c.windowY()-7-thumb/2.0)/Math.max(1,h-thumb);c.state().scroll=Math.max(0,Math.min(l.maxScroll,(int)Math.round(f*l.maxScroll)));}
    @Override public boolean mouseDragged(DesktopInputContext<AccessoriesDesktopMenu,State> c,MouseButtonEvent e,double dx,double dy){if(!c.state().dragging)return false;drag(c,e.y());return true;}
    @Override public boolean mouseReleased(DesktopInputContext<AccessoriesDesktopMenu,State> c,MouseButtonEvent e){boolean dragging=c.state().dragging;c.state().dragging=false;return dragging;}
    @Override public boolean mouseScrolled(DesktopInputContext<AccessoriesDesktopMenu,State> c,double x,double y,double dx,double dy){var l=layout(c);if(!l.scrolling||dy==0||!inside(x,y,c.windowX(),c.windowY(),c.windowWidth(),c.windowHeight()))return false;c.windowHandle().cancelInput();c.state().scroll=Math.max(0,Math.min(l.maxScroll,c.state().scroll+(dy<0?1:-1)));return true;}
    @Override public List<DesktopScrollRegion> controllerScrollRegions(DesktopWindowContext<AccessoriesDesktopMenu,State> c){var l=layout(c);return l.scrolling?List.of(new DesktopScrollRegion(c.windowX()+7,c.windowY()+7,Math.max(1,c.windowWidth()-25),Math.max(1,l.visible*20),20,c.state().scroll>0,c.state().scroll<l.maxScroll)):List.of();}
    @Override public boolean appendTooltip(DesktopRenderContext<AccessoriesDesktopMenu,State> c,int x,int y){
        if(!viewport(c,x,y))return false;var toggle=visibility(c,x,y);
        if(toggle!=null){c.tooltip(Component.translatable("accessories.display.toggle."+(toggle.model.render?"hide":"show")),x,y);return true;}
        for(Cell cell:cells(c))if(inside(x,y,cell.x-1,cell.y-1,18,18)){
            if(!cell.model.slot.getItem().isEmpty())c.tooltip(cell.model.slot.getItem(),x,y);
            else c.tooltip(Component.translatable("accessories."+(cell.model.cosmetic?"cosmetic_":"")+"slot.tooltip.singular").append(Component.translatable(cell.model.group.translation())),x,y);return true;
        }return false;
    }
    @Override public void customPayload(DesktopWindowContext<AccessoriesDesktopMenu,State> c,net.minecraft.resources.ResourceLocation channel,byte[] data){if(!channel.equals(AccessoriesIntegration.STATE)||data.length!=c.model().cells.size())return;for(int i=0;i<data.length;i++){if(data[i]!=0&&data[i]!=1)return;}for(int i=0;i<data.length;i++)c.model().cells.get(i).render=data[i]==1;}
    @Override public String layoutRevision(DesktopWindowContext<AccessoriesDesktopMenu,State> c){return "accessories-character-paired-2";}
}
