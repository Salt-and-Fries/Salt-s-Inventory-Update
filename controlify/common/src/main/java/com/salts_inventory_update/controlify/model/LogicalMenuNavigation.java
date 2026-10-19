package com.salts_inventory_update.controlify.model;

import java.util.Comparator;
import java.util.List;

/** Uses authored menu cells before geometric fallback between separate panels/windows. */
public final class LogicalMenuNavigation {
    private LogicalMenuNavigation() { }

    public record Target(String group, int row, int column, double x, double y, double width, double height) { }
    public enum Direction { LEFT, RIGHT, UP, DOWN }

    public static Target next(List<Target> targets, double x, double y, Direction direction) {
        if (targets.isEmpty()) return null;
        Target source = targets.stream().min(Comparator.comparingDouble(t -> distance(t, x, y))).orElseThrow();
        boolean horizontal = direction == Direction.LEFT || direction == Direction.RIGHT;
        int sign = direction == Direction.LEFT || direction == Direction.UP ? -1 : 1;
        if (Math.abs(source.x() - x) > source.width() / 2 + 1 || Math.abs(source.y() - y) > source.height() / 2 + 1) {
            // Empty-space presses acquire a target before advancing its logical row/column.
            return targets.stream().filter(t -> horizontal ? (t.x() - x) * sign > 1 : (t.y() - y) * sign > 1)
                .min(Comparator.comparingDouble(t -> {
                    double forward = horizontal ? Math.abs(t.x() - x) : Math.abs(t.y() - y);
                    double cross = horizontal ? Math.abs(t.y() - y) : Math.abs(t.x() - x);
                    return forward + cross * 3;
                })).orElse(source);
        }
        // Adjacent cells on the same logical line win even when their rendered centers are staggered.
        Target sameLine = targets.stream().filter(t -> t != source && t.group().equals(source.group()))
            .filter(t -> horizontal ? t.row() == source.row() && (t.column() - source.column()) * sign > 0
                : t.column() == source.column() && (t.row() - source.row()) * sign > 0)
            .min(Comparator.comparingInt(t -> horizontal ? Math.abs(t.column() - source.column()) : Math.abs(t.row() - source.row())))
            .orElse(null);
        if (sameLine != null) return sameLine;
        // Rows with fewer cells retain the nearest logical column (and vice versa).
        Target sameGroup = targets.stream().filter(t -> t != source && t.group().equals(source.group()))
            .filter(t -> horizontal ? (t.column() - source.column()) * sign > 0 : (t.row() - source.row()) * sign > 0)
            .min(Comparator.comparingDouble(t -> horizontal
                ? Math.abs(t.column() - source.column()) * 100 + Math.abs(t.row() - source.row()) * 30
                : Math.abs(t.row() - source.row()) * 100 + Math.abs(t.column() - source.column()) * 30))
            .orElse(null);
        if (sameGroup != null) return sameGroup;
        Target outside = targets.stream().filter(t -> t != source)
            .filter(t -> horizontal ? (t.x() - source.x()) * sign > 1 : (t.y() - source.y()) * sign > 1)
            .min(Comparator.comparingDouble(t -> {
                double forward = horizontal ? Math.abs(t.x() - source.x()) : Math.abs(t.y() - source.y());
                double cross = horizontal ? Math.abs(t.y() - source.y()) : Math.abs(t.x() - source.x());
                return forward + cross * 3;
            })).orElse(null);
        return outside == null ? source : outside;
    }

    private static double distance(Target t, double x, double y) {
        double dx = Math.max(0, Math.abs(t.x() - x) - t.width() / 2);
        double dy = Math.max(0, Math.abs(t.y() - y) - t.height() / 2);
        // Tiny center term deterministically distinguishes overlapping/boundary rectangles.
        return dx * dx + dy * dy + 0.0001 * ((t.x() - x) * (t.x() - x) + (t.y() - y) * (t.y() - y));
    }
}
