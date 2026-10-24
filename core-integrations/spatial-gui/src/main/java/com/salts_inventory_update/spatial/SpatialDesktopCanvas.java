package com.salts_inventory_update.spatial;

import com.salts_inventory_update.client.InventoryDesktopScreen;
import com.salts_inventory_update.api.client.desktop.DesktopHudLayout;
import net.minecraft.client.Minecraft;

/** A fixed desktop with the same four walls in spatial and normal presentation. */
public final class SpatialDesktopCanvas {
    private SpatialDesktopCanvas() { }
    public record Canvas(double width,double height,double scale,double dockX,double dockY) { }
    private static boolean resolving;
    private static Canvas current=new Canvas(1,1,1,0,0);
    public static Canvas get() {
        if(resolving)return current;
        var mc=Minecraft.getInstance();var viewport=mc.getWindow();
        double w=viewport.getGuiScaledWidth(),h=viewport.getGuiScaledHeight();
        var d=SpatialGuiIntegration.desktop();
        if(d==null||!SpatialGuiIntegration.active())return new Canvas(w,h,1,0,0);
        resolving=true;
        try {
            current=new Canvas(w,h,1,0,0);
            double clusterBottom=0,minX=w,maxX=0;
            var workspace=d.workspaceAccess();
            for(var window:workspace.windows()) {
                if(!window.handle().state().visible())continue;
                var bounds=workspace.presentationBounds(window.handle().instanceId());
                minX=Math.min(minX,bounds.x());maxX=Math.max(maxX,bounds.x()+bounds.width());clusterBottom=Math.max(clusterBottom,bounds.y()+bounds.height());
            }
            // A compact dock follows the cluster's lower edge, with room above for survival indicators.
            double dockCenter=Math.clamp((minX+maxX)/2,112,Math.max(112,w-100));
            double itemY=Math.clamp(clusterBottom+55,60,Math.max(60,h-23));
            current=new Canvas(w,h,1,dockCenter-w/2,itemY-(h-19));
            return current;
        } finally { resolving=false; }
    }
    public static DesktopHudLayout dock(DesktopHudLayout base) {
        if(!SpatialGuiIntegration.active()||SpatialGuiIntegration.movement())return base;
        var canvas=get();return base.translated(canvas.dockX,canvas.dockY);
    }
}
