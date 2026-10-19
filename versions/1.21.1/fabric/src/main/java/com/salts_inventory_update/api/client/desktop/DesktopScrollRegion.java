package com.salts_inventory_update.api.client.desktop;

/** Current list viewport in native GUI coordinates. Queries must not scroll or mutate the model. */
public record DesktopScrollRegion(int x, int y, int width, int height, int rowHeight,
                                  boolean canScrollUp, boolean canScrollDown, double nativeScrollStep) {
    public DesktopScrollRegion(int x,int y,int width,int height,int rowHeight,boolean canScrollUp,boolean canScrollDown) {
        this(x,y,width,height,rowHeight,canScrollUp,canScrollDown,1);
    }
    public DesktopScrollRegion {
        if (width <= 0 || height <= 0 || rowHeight <= 0 || !Double.isFinite(nativeScrollStep) || nativeScrollStep<=0)
            throw new IllegalArgumentException("Positive scroll geometry and native step required");
    }
    public boolean contains(double px, double py) {
        return px >= x && px < (double)x + width && py >= y && py < (double)y + height;
    }
    public boolean accepts(double px, double py, int direction, boolean edgeOnly) {
        return direction!=0 && contains(px,py) && (direction < 0 ? canScrollUp : canScrollDown)
            && (!edgeOnly || (direction < 0 ? py < (double)y + rowHeight : py >= (double)y + height - rowHeight));
    }
}
