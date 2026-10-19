package com.salts_inventory_update.compat.jei;

import com.salts_inventory_update.api.client.desktop.DesktopWindowBounds;
import java.util.function.BiPredicate;
import org.jspecify.annotations.Nullable;

/** JEI's artwork extent can include a distant background origin; navigate its actual hit area. */
final class RecipeHitBounds {
    private RecipeHitBounds() { }
    static @Nullable DesktopWindowBounds find(DesktopWindowBounds area,BiPredicate<Double,Double> hit) {
        if(area.width()<=0 || area.height()<=0 || (long)area.width()*area.height()>1_048_576) return null;
        for(int step:new int[]{4,1}) for(int y=area.y();y<area.y()+area.height();y+=step) for(int x=area.x();x<area.x()+area.width();x+=step) {
            if(!hit.test(x+0.5,y+0.5)) continue;
            int left=x,right=x,top=y,bottom=y;
            while(left>area.x() && hit.test(left-0.5,y+0.5)) left--;
            while(right<area.x()+area.width()-1 && hit.test(right+1.5,y+0.5)) right++;
            while(top>area.y() && hit.test(x+0.5,top-0.5)) top--;
            while(bottom<area.y()+area.height()-1 && hit.test(x+0.5,bottom+1.5)) bottom++;
            if(!hit.test((left+right+1)/2.0,(top+bottom+1)/2.0)) return new DesktopWindowBounds(x,y,1,1);
            return new DesktopWindowBounds(left,top,right-left+1,bottom-top+1);
        }
        return null;
    }
}
