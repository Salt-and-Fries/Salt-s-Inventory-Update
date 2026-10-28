package example.nativewindow;

import com.salts_inventory_update.api.client.desktop.*;
import com.salts_inventory_update.api.client.input.MouseButtonEvent;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;

/** API-only example, excluded from production. No Core implementation classes are imported. */
public final class FurnaceNativeTemplate<T extends AbstractContainerMenu> implements DesktopNativeTemplate<T,FurnaceNativeTemplate.State> {
    public static final class State { public boolean details; }
    public static <T extends AbstractContainerMenu> void register(MenuType<T> menuType){DesktopClientApi.registerWindow(menuType,new FurnaceNativeTemplate<T>()); }
    @Override public State createState(DesktopWindowSetupContext<T> c){return new State();}
    @Override public DesktopNativeLayout nativeLayout(DesktopNativeContext<T,State> c){
        var nativeView=c.nativeView();var body=nativeView.automaticBounds();
        var layout=DesktopNativeLayout.builder().inherit(body).padding(4).canvas(body.width()+8,body.height()+38);
        var output=nativeView.slotBounds(2).orElseThrow();
        var artwork=new DesktopWindowBounds(output.x()-4,output.y()-4,output.width()+8,output.height()+8);
        layout.move("output",artwork,body.width()-artwork.width(),0);
        nativeView.widgets().stream().filter(w->w.type().endsWith("ImageButton")&&w.visible()).findFirst()
            .ifPresent(w->layout.move("recipe-button",w.bounds(),0,0));
        return layout.build();
    }
    @Override public void renderNativeUnderlay(DesktopRenderContext<T,State> c){
        c.fill(c.contentX(),c.contentY(),c.contentX()+c.contentWidth(),c.contentY()+c.contentHeight(),0xFFC6C6C6);
    }
    @Override public void render(DesktopRenderContext<T,State> c){
        int x=c.contentX()+4,y=c.contentY()+c.contentHeight()-22;
        c.fill(x,y,x+82,y+16,0xFF3E5962);c.text(c.state().details?"Hide details":"Details",x+4,y+4,0xFFFFFFFF,false);
        if(c.state().details)c.text("Native template",x+87,y+4,0xFF12333A,false);
    }
    @Override public boolean mouseClicked(DesktopInputContext<T,State> c,MouseButtonEvent e,boolean twice){
        int x=c.contentX()+4,y=c.contentY()+c.contentHeight()-22;
        if(e.button()==0&&e.x()>=x&&e.x()<x+82&&e.y()>=y&&e.y()<y+16){c.state().details=!c.state().details;return true;}return false;
    }
}
