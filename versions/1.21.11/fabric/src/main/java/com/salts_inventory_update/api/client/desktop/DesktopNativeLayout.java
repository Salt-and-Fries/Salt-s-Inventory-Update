package com.salts_inventory_update.api.client.desktop;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Immutable native composition, committed once for painting, slots and pointer routing. */
public record DesktopNativeLayout(List<DesktopNativeRegion> regions, int padding,
                                  int minimumWidth, int minimumHeight, boolean scrollable) {
    public DesktopNativeLayout {
        regions = List.copyOf(regions);
        if (regions.isEmpty() || regions.size() > 64 || padding < 0 || padding > 256
            || minimumWidth < 0 || minimumHeight < 0 || minimumWidth > 4096 || minimumHeight > 4096)
            throw new IllegalArgumentException("Invalid native layout");
        if (regions.stream().map(DesktopNativeRegion::id).distinct().count() != regions.size())
            throw new IllegalArgumentException("Duplicate native region id");
        for(var r:regions) {
            if(Math.abs((long)r.source().x())>4096 || Math.abs((long)r.source().y())>4096
                || Math.abs((long)r.x())>4096 || Math.abs((long)r.y())>4096
                || r.source().width()>4096 || r.source().height()>4096
                || (long)r.x()+r.source().width()+2L*padding>4096
                || (long)r.y()+r.source().height()+2L*padding>4096)
                throw new IllegalArgumentException("Native region exceeds layout limits");
            r.pieces();
        }
    }
    public record Point(String regionId, double x, double y) { }
    public record Placement(String regionId, DesktopWindowBounds bounds, boolean interactive, boolean exterior) {
        public Placement(String regionId, DesktopWindowBounds bounds, boolean interactive) { this(regionId,bounds,interactive,false); }
    }
    public int width() { return Math.max(minimumWidth, regions.stream().filter(r -> !r.exterior()).mapToInt(r -> r.x()+r.source().width()).max().orElse(1)+padding*2); }
    public int height() { return Math.max(minimumHeight, regions.stream().filter(r -> !r.exterior()).mapToInt(r -> r.y()+r.source().height()).max().orElse(1)+padding*2); }
    public Optional<Point> sourceAt(double x, double y) {
        for(int i=regions.size()-1;i>=0;i--) {
            var r=regions.get(i);if(r.exterior())continue;double sx=x-padding-r.x()+r.source().x(),sy=y-padding-r.y()+r.source().y();
            if(r.interactive()&&r.contains(sx,sy))return Optional.of(new Point(r.id(),sx,sy));
        }
        return Optional.empty();
    }
    /** Exterior destinations are window-local, do not scroll and do not enlarge the body canvas. */
    public Optional<Point> exteriorSourceAt(double windowX,double windowY) {
        for(int i=regions.size()-1;i>=0;i--) {
            var r=regions.get(i);if(!r.exterior()||!r.interactive())continue;
            double sx=windowX-r.x()+r.source().x(),sy=windowY-r.y()+r.source().y();
            if(r.contains(sx,sy))return Optional.of(new Point(r.id(),sx,sy));
        }
        return Optional.empty();
    }
    public List<Placement> placements(DesktopWindowBounds nativeBounds) {
        var result=new ArrayList<Placement>();
        for(var r:regions)if(r.covers(nativeBounds))result.add(new Placement(r.id(),
            new DesktopWindowBounds((r.exterior()?0:padding)+r.x()+nativeBounds.x()-r.source().x(),(r.exterior()?0:padding)+r.y()+nativeBounds.y()-r.source().y(),nativeBounds.width(),nativeBounds.height()),r.interactive(),r.exterior()));
        return List.copyOf(result);
    }
    public static DesktopNativeLayout automatic(DesktopWindowBounds bounds) { return builder().keep("automatic",bounds,0,0).build(); }
    public static Builder builder() { return new Builder(); }
    public Builder toBuilder() { return new Builder(this); }
    public static final class Builder {
        private final List<DesktopNativeRegion> regions=new ArrayList<>();
        private int padding,width,height; private boolean scroll=true;
        private Builder() { }
        private Builder(DesktopNativeLayout layout) {
            regions.addAll(layout.regions());padding=layout.padding();width=layout.minimumWidth();height=layout.minimumHeight();scroll=layout.scrollable();
        }
        public Builder inherit(DesktopWindowBounds automaticBounds) { return keep("automatic",automaticBounds,0,0); }
        public Builder keep(String id, DesktopWindowBounds source, int x, int y) { return region(new DesktopNativeRegion(id,source,x,y)); }
        /** Retains native pixels outside the outer frame, painted behind it with original input mapping. */
        public Builder exterior(String id, DesktopWindowBounds source, int windowX, int windowY) {
            return region(new DesktopNativeRegion(id,source,windowX,windowY,true,List.of(),true));
        }
        public Builder region(DesktopNativeRegion region) { regions.add(region);return this; }
        public Builder padding(int pixels) { padding=pixels;return this; }
        public Builder canvas(int width,int height) { this.width=width;this.height=height;return this; }
        public Builder scrollable(boolean scroll) { this.scroll=scroll;return this; }
        public Builder hide(DesktopWindowBounds source) {
            for(int i=0;i<regions.size();i++)if(DesktopNativeRegion.intersects(regions.get(i).source(),source))regions.set(i,regions.get(i).cut(source));
            return this;
        }
        /** Excludes the original pixels and interaction before adding their new location. */
        public Builder move(String id, DesktopWindowBounds source,int x,int y) { return hide(source).keep(id,source,x,y); }
        public Builder moveRegion(String id,int x,int y) {
            for(int i=0;i<regions.size();i++)if(regions.get(i).id().equals(id)){regions.set(i,regions.get(i).at(x,y));return this;}
            throw new IllegalArgumentException("Unknown native region: "+id);
        }
        public DesktopNativeLayout build() { return new DesktopNativeLayout(regions,padding,width,height,scroll); }
    }
}
