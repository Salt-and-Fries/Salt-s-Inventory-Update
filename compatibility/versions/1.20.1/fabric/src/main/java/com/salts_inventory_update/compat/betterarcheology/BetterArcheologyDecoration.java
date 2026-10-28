package com.salts_inventory_update.compat.betterarcheology;

import java.util.List;
import com.salts_inventory_update.api.client.desktop.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.AbstractContainerMenu;

/** Parchment chrome; Core still owns every committed control and its input. */
final class BetterArcheologyDecoration implements DesktopWindowDecoration<AbstractContainerMenu,Void> {
    static final BetterArcheologyDecoration INSTANCE=new BetterArcheologyDecoration();
    private static final ResourceLocation FRAME=new ResourceLocation("salts_inventory_compat","textures/gui/better_archeology_window.png");
    private static final int INK=0xFF59452F;

    @Override public void render(DesktopRenderContext<AbstractContainerMenu,Void> c,List<DesktopControlState> controls){
        c.windowNineSlice(FRAME,c.windowX(),c.windowY(),c.windowWidth(),c.windowHeight());
        int titleRight=controls.stream().filter(control->control.control()!=DesktopWindowControl.RESIZE_GRIP)
                .mapToInt(control->control.bounds().x()-3).min().orElse(c.windowX()+c.windowWidth()-6);
        String title=c.trimToWidth(c.originalTitle().getString(),Math.max(0,titleRight-c.windowX()-6));
        c.text(title,c.windowX()+6,c.windowY()+5,INK,false);
        for(var control:controls)paintControl(c,control);
    }

    private static void paintControl(DesktopRenderContext<?,?> c,DesktopControlState control){
        var b=control.bounds();int x=b.x(),y=b.y(),w=b.width(),h=b.height();
        if(control.control()==DesktopWindowControl.RESIZE_GRIP){
            for(int step=0;step<3;step++)c.fill(x+w-2-step*2,y+h-1,x+w-1-step*2,y+h,0xFFAD9168);
            return;
        }
        int fill=!control.enabled()?0xFFE0D3B7:control.pressed()?0xFFC9AE7F:
                control.hovered()||control.keyboardFocused()?0xFFFFE5AB:0xFFF2DFB7;
        c.fill(x,y,x+w,y+h,0xFF92764F);
        c.fill(x+1,y+1,x+w-1,y+h-1,fill);
        if(!control.pressed()){
            c.fill(x+1,y+1,x+w-1,y+2,0xFFFFF9E6);
            c.fill(x+1,y+2,x+2,y+h-1,0xFFFFF9E6);
        }
        String[] glyph=switch(control.control()){
            case CLOSE->new String[]{"#...#",".#.#.","..#..",".#.#.","#...#"};
            case ELLIPSIS->new String[]{".....",".....","#.#.#",".....","....."};
            case FOCUS->new String[]{"##.##","#...#","..#..","#...#","##.##"};
            case PIN->new String[]{".###.","..#..",".###.","..#..","..#.."};
            case LOCK->new String[]{".###.",".#.#.","#####","#.#.#","#####"};
            case LINK->new String[]{".##..","#..#.",".#.#.",".#..#","..##."};
            case SORT_WINDOWS->new String[]{"....#","..###","###.#","#.#..","#...."};
            case SORT_INVENTORY->new String[]{"###.#","##..#","#...#","...##","....#"};
            default->new String[]{".....",".....",".....",".....","....."};
        };
        int ink=!control.enabled()?0xFFAE9A79:control.selected()?0xFF49724F:INK;
        for(int row=0;row<glyph.length;row++)for(int col=0;col<glyph[row].length();col++)
            if(glyph[row].charAt(col)=='#')c.fill(x+(w-5)/2+col,y+(h-5)/2+row,x+(w-5)/2+col+1,y+(h-5)/2+row+1,ink);
    }
}
