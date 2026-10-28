package com.salts_inventory_update.client.automatic;

import java.util.List;

/** Pure geometry inference for conventional upper-container/lower-player layouts. */
public record AutomaticContainerLayout(int x, int y, int width, int height) {
    public record Cell(int x, int y, boolean player) { }
    public record Control(int x, int y, int width, int height) { }

    /** Expand laterally to retain native side borders without changing the vertical cut. */
    public AutomaticContainerLayout withNativeSides(int imageWidth) {
        int left = Math.min(0, x), right = Math.max(imageWidth, x + width);
        return new AutomaticContainerLayout(left, y, right - left, height);
    }

    /** Conventional inventory backing starts above its label/first slot row.
     * Keep real upper controls and complete cells, but omit the lower inventory's border. */
    public AutomaticContainerLayout automaticBody(int imageWidth, List<Cell> cells, List<Control> controls) {
        int playerTop = cells.stream().filter(Cell::player).mapToInt(Cell::y).min().orElse(Integer.MAX_VALUE);
        int requiredBottom = cells.stream().filter(c -> !c.player()).mapToInt(c -> c.y() + 19).max().orElse(y);
        for (Control c : controls) {
            if (c.width() > 0 && c.height() > 0 && c.y() + c.height() <= playerTop - 2)
                requiredBottom = Math.max(requiredBottom, c.y() + c.height());
        }
        int bottom = Math.min(y + height, Math.max(requiredBottom, playerTop - 16));
        var sides = withNativeSides(imageWidth);
        return new AutomaticContainerLayout(sides.x(), y, sides.width(), Math.max(1, bottom - y));
    }

    public static AutomaticContainerLayout infer(int imageWidth, int imageHeight,
                                                 List<Cell> cells, List<Control> controls) {
        if (imageWidth < 32 || imageHeight < 32 || imageWidth > 2048 || imageHeight > 2048) return null;
        int playerTop = cells.stream().filter(Cell::player).mapToInt(Cell::y).min().orElse(Integer.MAX_VALUE);
        List<Cell> machine = cells.stream().filter(c -> !c.player() && c.x() >= 0 && c.y() >= 0).toList();
        if (machine.isEmpty() || playerTop == Integer.MAX_VALUE) return null;
        int machineTop = machine.stream().mapToInt(Cell::y).min().orElseThrow();
        int machineBottom = machine.stream().mapToInt(c -> c.y() + 17).max().orElseThrow();
        // The lower inventory is a semantic exclusion, not a list-index convention.
        if (machineBottom > playerTop - 2) return null;
        int left = 7, right = imageWidth - 7;
        int top = Math.max(0, Math.min(16, machineTop - 1));
        int bottom = Math.max(machineBottom + 2, playerTop - 10);
        for (Cell c : machine) { left = Math.min(left, c.x() - 1); right = Math.max(right, c.x() + 17); }
        for (Control c : controls) {
            if (c.width() <= 0 || c.height() <= 0 || c.y() + c.height() > playerTop - 2) continue;
            left = Math.min(left, c.x()); right = Math.max(right, c.x() + c.width());
            top = Math.min(top, c.y()); bottom = Math.max(bottom, c.y() + c.height());
        }
        if (right <= left || bottom <= top || bottom > imageHeight || right - left > 2048) return null;
        return new AutomaticContainerLayout(left, top, right - left, bottom - top);
    }
}
