package com.salts_inventory_update.compat.create;

import com.salts_inventory_update.api.client.desktop.*;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** Create's own title strip is the window chrome; the native body supplies every other edge. */
final class SchematicannonChrome {
    // SCHEMATIC_TITLE occupies native y=-2..12. The next unmodified body row is y=13.
    static final int TITLE_HEIGHT=15, BODY_TOP=13;
    static final DesktopWindowSurface SURFACE=new DesktopWindowSurface(DesktopSurfaceMode.SALT,
        new DesktopInsets(0,TITLE_HEIGHT,0,0),true,List.of(),DesktopViewportPolicy.CLAMP);
    static final DesktopChromeMetrics METRICS=new DesktopChromeMetrics(TITLE_HEIGHT,6,4,11,11,3,10,2,0);
    private static final ResourceLocation TEXTURE=ResourceLocation.fromNamespaceAndPath("create","textures/gui/schematics_2.png");
    private SchematicannonChrome() { }

    static void paint(DesktopRenderContext<?,?> c,List<DesktopControlState> controls,Component title) {
        int x=c.windowX(),y=c.windowY();
        // Exact upstream 205x15 strip, including its square corners and bevel.
        // The body's extra eight pixels belong to the pointed footer, not the title.
        c.texture(TEXTURE,x,y,0,0,205,TITLE_HEIGHT,205,TITLE_HEIGHT,256,256);
        int right=controls.stream().filter(s->s.control()!=DesktopWindowControl.RESIZE_GRIP)
            .mapToInt(s->s.bounds().x()-3).min().orElse(x+199);
        c.text(c.trimToWidth(title.getString(),Math.max(0,right-x-METRICS.titleLeftPadding())),
            x+METRICS.titleLeftPadding(),y+METRICS.titleTextY(),0xFF505050,false);
        CreateChrome.controls(c,controls);
    }
}
