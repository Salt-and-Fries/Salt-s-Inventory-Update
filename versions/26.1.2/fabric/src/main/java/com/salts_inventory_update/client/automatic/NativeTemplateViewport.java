package com.salts_inventory_update.client.automatic;

/** A committed content viewport. Empty scrollbar gutters are never reserved. */
public record NativeTemplateViewport(int width,int height,boolean horizontal,boolean vertical) {
    public static final int GUTTER=8;
    public static NativeTemplateViewport resolve(int canvasWidth,int canvasHeight,int availableWidth,int availableHeight,boolean scrollable) {
        boolean horizontal=false,vertical=false;
        int w=Math.max(1,availableWidth),h=Math.max(1,availableHeight);
        if(scrollable)for(int i=0;i<3;i++) {
            horizontal|=canvasWidth>Math.max(1,w-(vertical?GUTTER:0));
            vertical|=canvasHeight>Math.max(1,h-(horizontal?GUTTER:0));
        }
        return new NativeTemplateViewport(Math.max(1,w-(vertical?GUTTER:0)),Math.max(1,h-(horizontal?GUTTER:0)),horizontal,vertical);
    }
    /** Grow into available room before introducing an unnecessary second scrollbar. */
    public static int[] fit(int canvasWidth,int canvasHeight,int maxWidth,int maxHeight) {
        int w=Math.min(canvasWidth,Math.max(1,maxWidth)),h=Math.min(canvasHeight,Math.max(1,maxHeight));
        if(h<canvasHeight)w=Math.min(Math.max(1,maxWidth),canvasWidth+GUTTER);
        if(w<canvasWidth)h=Math.min(Math.max(1,maxHeight),canvasHeight+GUTTER);
        return new int[]{w,h};
    }
}
