package com.salts_inventory_update.api.client.desktop.widget;

/** Per-panel pixel scrolling and uniform-row virtualization, independent of Minecraft widgets. */
public final class DesktopScrollState {
    private int rows;
    private int rowHeight = 1;
    private int viewportHeight;
    private double offset;

    public void layout(int rows, int rowHeight, int viewportHeight) {
        if (rows < 0 || rowHeight <= 0 || viewportHeight < 0) {
            throw new IllegalArgumentException("Invalid scroll layout");
        }
        this.rows = rows;
        this.rowHeight = rowHeight;
        this.viewportHeight = viewportHeight;
        this.offset(this.offset);
    }

    public double offset() { return this.offset; }

    public double maxOffset() { return Math.max(0.0, (double) this.rows * this.rowHeight - this.viewportHeight); }

    public void offset(double pixels) {
        if (!Double.isFinite(pixels)) throw new IllegalArgumentException("Scroll offset must be finite");
        this.offset = Math.max(0.0, Math.min(pixels, this.maxOffset()));
    }

    public boolean scroll(double pixels) {
        double previous = this.offset;
        this.offset(previous + pixels);
        return this.offset != previous;
    }

    /** End is exclusive. Overscan is optional; rows remain bounded by the actual model. */
    public VisibleRows visibleRows(int overscan) {
        if (overscan < 0) throw new IllegalArgumentException("Negative overscan");
        if (this.viewportHeight == 0 || this.rows == 0) return new VisibleRows(0, 0);
        long first = (long) Math.floor(this.offset / this.rowHeight);
        long end = (long) Math.ceil((this.offset + this.viewportHeight) / this.rowHeight);
        return new VisibleRows((int) Math.max(0, first - overscan), (int) Math.min(this.rows, end + overscan));
    }

    public double rowY(int row) {
        if (row < 0 || row >= this.rows) throw new IndexOutOfBoundsException(row);
        return (double) row * this.rowHeight - this.offset;
    }

    /** -1 means outside the clipped viewport or outside the current model. */
    public int rowAt(double localY) {
        if (!Double.isFinite(localY) || localY < 0 || localY >= this.viewportHeight) return -1;
        double row = Math.floor((localY + this.offset) / this.rowHeight);
        return row < this.rows ? (int) row : -1;
    }

    public record VisibleRows(int first, int endExclusive) { }
}
