package com.salts_inventory_update.compat.nativewindows;

import com.salts_inventory_update.api.client.desktop.*;
import java.util.List;
import java.util.function.Function;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.AbstractContainerMenu;

/** Approved compact sizing and optional original workstation emblem in the title bar. */
public class MockupNativeTemplate<S> extends CompactNativeTemplate<S> {
    public record Emblem(ResourceLocation texture,int x,int y,int width,int height) { }
    private final Emblem emblem;
    public MockupNativeTemplate(Function<DesktopNativeView,DesktopNativeLayout> layout) { this(layout,null); }
    public MockupNativeTemplate(Function<DesktopNativeView,DesktopNativeLayout> layout,Emblem emblem) {
        super(layout);this.emblem=emblem;
    }
    @Override public DesktopNativeLayout nativeLayout(DesktopNativeContext<AbstractContainerMenu,S> c) {
        // 6px title inset + 6px gap + two 11px controls / 3px gaps + 2px right inset.
        // Remove the 12px body insets because this value is a content width.
        return NativeBatchLayouts.center(super.nativeLayout(c),c.fontWidth(c.originalTitle())+30+(emblem==null?0:14));
    }
    @Override public DesktopWindowDecoration<AbstractContainerMenu,S> decoration(DesktopWindowContext<AbstractContainerMenu,S> c) {
        return emblem==null?null:this::paintChrome;
    }
    private void paintChrome(DesktopRenderContext<AbstractContainerMenu,S> c,List<DesktopControlState> controls) {
        c.windowNineSlice(new ResourceLocation("salts_inventory_update","textures/gui/window.png"),c.windowX(),c.windowY(),c.windowWidth(),c.windowHeight());
        c.texture(emblem.texture(),c.windowX()+6,c.windowY()+3,emblem.x(),emblem.y(),12,12,emblem.width(),emblem.height(),256,256);
        int right=controls.stream().filter(s->s.control()!=DesktopWindowControl.RESIZE_GRIP).mapToInt(s->s.bounds().x()-3)
            .min().orElse(c.windowX()+c.windowWidth()-6);
        c.text(c.trimToWidth(c.originalTitle().getString(),Math.max(0,right-c.windowX()-20)),c.windowX()+20,c.windowY()+5,0xFF404040,false);
        var texture=new ResourceLocation("salts_inventory_update","textures/gui/window_controls.png");
        for(var state:controls) {
            int column=switch(state.control()) {
                case FOCUS->0;case SORT_INVENTORY->1;case CLOSE->2;case ELLIPSIS->3;
                case LOCK->5;case PIN->6;case LINK->9;case SORT_WINDOWS->10;default->-1;
            };
            var b=state.bounds();
            if(column>=0)c.texture(texture,b.x(),b.y(),column*11,(state.pressed()||state.selected()?2:state.hovered()||state.keyboardFocused()?1:0)*11,
                b.width(),b.height(),11,11,132,33);
        }
    }
}
