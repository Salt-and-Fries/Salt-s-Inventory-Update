package com.salts_inventory_update.controlify.model;

/** A single shared frame places both live windows and the emerging panels in GUI coordinates. */
public final class ContextualPresentationLayout {
    public static final Rect MATERIALS = new Rect(12, 21, 195, 240);
    public static final Rect DESTINATION_AREA = new Rect(12, 275, 195, 154);
    public static final Rect WHEEL = new Rect(208, 21, 344, 408);
    public static final Rect SOURCE_AREA = new Rect(553, 87, 195, 267);
    public record Rect(double x, double y, double width, double height) {
        public Rect {
            if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(width) || !Double.isFinite(height)
                    || width <= 0 || height <= 0) throw new IllegalArgumentException("Invalid presentation rectangle");
        }
        public double centerX() { return x + width / 2; }
        public double bottom() { return y + height; }
    }
    public record Frame(double progress, double canvasX, double canvasY, double canvasScale,
                        Rect container, Rect destination, Rect materials, Rect wheel) { }
    private ContextualPresentationLayout() { }

    public static Frame frame(int width, int height, Rect sourceSide, double progress) {
        return frame(width, height, sourceSide, progress, sourceSide.width, sourceSide.height);
    }
    public static Frame frame(int width, int height, Rect sourceSide, double progress, double nativeWidth, double nativeHeight) {
        return frame(width, height, sourceSide, sourceSide, progress, nativeWidth, nativeHeight, nativeWidth, nativeHeight);
    }
    public static Frame frame(int width, int height, Rect sourceSide, Rect destinationSide, double progress,
            double nativeWidth, double nativeHeight, double destinationWidth, double destinationHeight) {
        if (width <= 0 || height <= 0 || !Double.isFinite(progress) || !Double.isFinite(nativeWidth)
                || !Double.isFinite(nativeHeight) || nativeWidth <= 0 || nativeHeight <= 0
                || !Double.isFinite(destinationWidth) || !Double.isFinite(destinationHeight)
                || destinationWidth <= 0 || destinationHeight <= 0)
            throw new IllegalArgumentException("Invalid presentation viewport");
        double p = Math.max(0, Math.min(1, progress));
        double scale = Math.min(1.5, Math.min(width / 760.0, height / 450.0));
        double x = (width - 760 * scale) / 2, y = (height - 450 * scale) / 2;
        Rect sourceTarget = fit(screen(SOURCE_AREA, x, y, scale), nativeWidth, nativeHeight);
        Rect destinationTarget = fit(screen(DESTINATION_AREA, x, y, scale), destinationWidth, destinationHeight);
        Rect container = mix(sourceSide, sourceTarget, p);
        return new Frame(p, x, y, scale, container, mix(destinationSide, destinationTarget, p),
            emerge(screen(MATERIALS, x, y, scale), container, p, scale),
            emerge(screen(WHEEL, x, y, scale), container, p, scale));
    }
    /** The last displayed pose remains exact even if returning SIDE geometry or guide height changes. */
    public static Frame closing(int width, int height, Rect sideDestination, Frame displayed, double remaining) {
        return closing(width, height, sideDestination, sideDestination, displayed, remaining);
    }
    public static Frame closing(int width, int height, Rect sourceSide, Rect destinationSide, Frame displayed, double remaining) {
        return interpolate(frame(width, height, sourceSide, destinationSide, 0,
            sourceSide.width(), sourceSide.height(), destinationSide.width(), destinationSide.height()), displayed, remaining);
    }
    /** Re-pressing during reversal continues from the actual visible pose rather than a new origin. */
    public static Frame reopening(int width, int height, Rect sideOrigin, double nativeWidth, double nativeHeight,
            Frame displayed, double progress) {
        return reopening(width, height, sideOrigin, sideOrigin, nativeWidth, nativeHeight, nativeWidth, nativeHeight,
            displayed, progress);
    }
    public static Frame reopening(int width, int height, Rect sourceSide, Rect destinationSide,
            double nativeWidth, double nativeHeight, double destinationWidth, double destinationHeight,
            Frame displayed, double progress) {
        return interpolate(displayed, frame(width, height, sourceSide, destinationSide, 1,
            nativeWidth, nativeHeight, destinationWidth, destinationHeight), progress);
    }
    private static Frame interpolate(Frame from, Frame to, double progress) {
        if (!Double.isFinite(progress)) throw new IllegalArgumentException("Invalid presentation progress");
        double p = Math.max(0, Math.min(1, progress));
        if (p == 0) return from;
        if (p == 1) return to;
        return new Frame(from.progress + (to.progress - from.progress) * p,
            from.canvasX + (to.canvasX - from.canvasX) * p,
            from.canvasY + (to.canvasY - from.canvasY) * p,
            from.canvasScale + (to.canvasScale - from.canvasScale) * p,
            mix(from.container, to.container, p), mix(from.destination, to.destination, p),
            mix(from.materials, to.materials, p), mix(from.wheel, to.wheel, p));
    }
    private static Rect fit(Rect area, double width, double height) {
        double scale = Math.min(area.width / width, area.height / height);
        double w = width * scale, h = height * scale;
        return new Rect(area.x + (area.width - w) / 2, area.y + (area.height - h) / 2, w, h);
    }
    private static Rect screen(Rect rect, double x, double y, double scale) {
        return new Rect(x + rect.x * scale, y + rect.y * scale, rect.width * scale, rect.height * scale);
    }
    private static Rect emerge(Rect destination, Rect container, double progress, double canvasScale) {
        Rect origin = new Rect(container.centerX() - destination.width * .075,
            container.bottom() + 12 * canvasScale, destination.width * .15, destination.height * .15);
        return mix(origin, destination, progress);
    }
    private static Rect mix(Rect a, Rect b, double p) {
        if (p == 0) return a;
        if (p == 1) return b;
        return new Rect(a.x + (b.x - a.x) * p, a.y + (b.y - a.y) * p,
            a.width + (b.width - a.width) * p, a.height + (b.height - a.height) * p);
    }
}
