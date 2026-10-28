package com.salts_inventory_update.api.client.desktop;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** A piece of a native screen. Source coordinates are relative to its original menu origin. */
public record DesktopNativeRegion(String id, DesktopWindowBounds source, int x, int y,
                                  boolean interactive, List<DesktopWindowBounds> cutouts, boolean exterior) {
    public DesktopNativeRegion {
        Objects.requireNonNull(id); Objects.requireNonNull(source);
        if (id.isBlank() || (!exterior && (x < 0 || y < 0))) throw new IllegalArgumentException("Invalid native region");
        Math.addExact(x, source.width()); Math.addExact(y, source.height());
        cutouts = List.copyOf(cutouts);
        if (cutouts.size() > 64) throw new IllegalArgumentException("Too many native cutouts");
    }
    /** Retained constructor: ordinary regions use nonnegative canvas coordinates. */
    public DesktopNativeRegion(String id, DesktopWindowBounds source, int x, int y,
                               boolean interactive, List<DesktopWindowBounds> cutouts) {
        this(id, source, x, y, interactive, cutouts, false);
    }
    public DesktopNativeRegion(String id, DesktopWindowBounds source, int x, int y) {
        this(id, source, x, y, true, List.of());
    }
    public DesktopNativeRegion at(int x, int y) { return new DesktopNativeRegion(id, source, x, y, interactive, cutouts, exterior); }
    public DesktopNativeRegion cut(DesktopWindowBounds bounds) {
        var next = new ArrayList<>(cutouts); next.add(bounds);
        return new DesktopNativeRegion(id, source, x, y, interactive, next, exterior);
    }
    public boolean contains(double sourceX, double sourceY) {
        return contains(source, sourceX, sourceY) && cutouts.stream().noneMatch(b -> contains(b, sourceX, sourceY));
    }
    public boolean covers(DesktopWindowBounds bounds) {
        return bounds.x() >= source.x() && bounds.y() >= source.y()
            && bounds.x() + bounds.width() <= source.x() + source.width()
            && bounds.y() + bounds.height() <= source.y() + source.height()
            && cutouts.stream().noneMatch(b -> intersects(b, bounds));
    }
    public List<DesktopWindowBounds> pieces() {
        List<DesktopWindowBounds> result = List.of(source);
        for (var hole : cutouts) {
            var next = new ArrayList<DesktopWindowBounds>();
            for (var piece : result) subtract(piece, hole, next);
            result = next;
            if (result.size() > 1024) throw new IllegalArgumentException("Native cutouts are too complex");
        }
        return List.copyOf(result);
    }
    public static boolean contains(DesktopWindowBounds b, double x, double y) {
        return x >= b.x() && y >= b.y() && x < b.x() + b.width() && y < b.y() + b.height();
    }
    public static boolean intersects(DesktopWindowBounds a, DesktopWindowBounds b) {
        return a.x() < b.x()+b.width() && a.x()+a.width() > b.x()
            && a.y() < b.y()+b.height() && a.y()+a.height() > b.y();
    }
    private static void subtract(DesktopWindowBounds a, DesktopWindowBounds b, List<DesktopWindowBounds> out) {
        if (!intersects(a,b)) { out.add(a); return; }
        int l=Math.max(a.x(),b.x()), t=Math.max(a.y(),b.y());
        int r=Math.min(a.x()+a.width(),b.x()+b.width()), d=Math.min(a.y()+a.height(),b.y()+b.height());
        if(t>a.y())out.add(new DesktopWindowBounds(a.x(),a.y(),a.width(),t-a.y()));
        if(d<a.y()+a.height())out.add(new DesktopWindowBounds(a.x(),d,a.width(),a.y()+a.height()-d));
        if(l>a.x())out.add(new DesktopWindowBounds(a.x(),t,l-a.x(),d-t));
        if(r<a.x()+a.width())out.add(new DesktopWindowBounds(r,t,a.x()+a.width()-r,d-t));
    }
}
