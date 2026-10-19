package com.salts_inventory_update.internal.desktop;

import java.util.Arrays;

/** Maximum-cardinality ingredient matching, including recipes which lack some ingredients. */
public final class PartialRecipeAllocation {
    private PartialRecipeAllocation() { }

    /** Returns the supplying source index per ingredient, or -1 for a missing ingredient. */
    public static int[] allocate(boolean[][] accepts, int[] sourceCounts) {
        int ingredients = accepts.length;
        int maximumSources = com.salts_inventory_update.protocol.DesktopProtocol.MAX_OPEN_SESSION_ITEMS
            * (com.salts_inventory_update.protocol.DesktopProtocol.MAX_DESKTOP_SESSIONS + 1);
        if (ingredients > 9 || sourceCounts.length > maximumSources) throw new IllegalArgumentException("Unbounded recipe allocation");
        for (boolean[] row : accepts) if (row.length != sourceCounts.length) throw new IllegalArgumentException("Invalid compatibility matrix");
        int total = 0;
        for (int count : sourceCounts) {
            if (count < 0) throw new IllegalArgumentException("Negative ingredient supply");
            total += Math.min(count, ingredients);
        }
        int[] units = new int[total];
        int position = 0;
        for (int source = 0; source < sourceCounts.length; source++)
            for (int unit = 0; unit < Math.min(sourceCounts[source], ingredients); unit++) units[position++] = source;
        int[] owner = new int[total]; Arrays.fill(owner, -1);
        for (int ingredient = 0; ingredient < ingredients; ingredient++)
            augment(ingredient, accepts, units, owner, new boolean[total]);
        int[] allocation = new int[ingredients]; Arrays.fill(allocation, -1);
        for (int unit = 0; unit < total; unit++) if (owner[unit] >= 0) allocation[owner[unit]] = units[unit];
        return allocation;
    }

    private static boolean augment(int ingredient, boolean[][] accepts, int[] units, int[] owner, boolean[] seen) {
        for (int unit = 0; unit < units.length; unit++) {
            if (seen[unit] || !accepts[ingredient][units[unit]]) continue;
            seen[unit] = true;
            if (owner[unit] < 0 || augment(owner[unit], accepts, units, owner, seen)) {
                owner[unit] = ingredient; return true;
            }
        }
        return false;
    }
}
