package com.salts_inventory_update.controlify.model;

import java.util.ArrayDeque;

/** One bounded, short-lived receipt per programmatic native cursor warp. */
public final class CursorWarpEchoTracker {
    private static final int LIMIT = 8;
    private static final long LIFETIME_NANOS = 200_000_000L;
    private final ArrayDeque<Warp> pending = new ArrayDeque<>();
    public static final class Warp {
        private final Object owner;
        private final long window, created;
        private double x, y;
        private Warp(Object owner, long window, double x, double y, long created) {
            this.owner=owner;this.window=window;this.x=x;this.y=y;this.created=created;
        }
        public void actualPosition(double x, double y) {
            if (Double.isFinite(x) && Double.isFinite(y)) { this.x=x;this.y=y; }
        }
    }
    public Warp record(Object owner, long window, double x, double y, long now) {
        prune(now);
        if (owner == null || window == 0 || !Double.isFinite(x) || !Double.isFinite(y)) return null;
        while (pending.size() >= LIMIT) pending.removeFirst();
        Warp warp=new Warp(owner,window,x,y,now);pending.addLast(warp);return warp;
    }
    public void discard(Warp warp) { pending.remove(warp); }
    public boolean consume(Object owner, long window, double x, double y, long now) {
        prune(now);
        if (owner == null || !Double.isFinite(x) || !Double.isFinite(y)) return false;
        for (var iterator=pending.iterator();iterator.hasNext();) {
            Warp warp=iterator.next();
            if (warp.owner == owner && warp.window == window && warp.x == x && warp.y == y) {
                iterator.remove();return true;
            }
        }
        // A differing physical movement owns the pointer; do not suppress a later return to this position.
        pending.removeIf(warp -> warp.owner == owner && warp.window == window);
        return false;
    }
    private void prune(long now) { pending.removeIf(warp -> now-warp.created < 0 || now-warp.created > LIFETIME_NANOS); }
}
