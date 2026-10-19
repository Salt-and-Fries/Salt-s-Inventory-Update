package com.salts_inventory_update.internal.desktop;

/** Parses a read-only source address without guessing from a window's localized title. */
public final class WorkspacePreviewSource {
    public record Position(int x, int y, int z) { }
    private WorkspacePreviewSource() { }

    public static Position blockPosition(String key, String dimension) {
        if (key == null || dimension == null) return null;
        String blockPrefix = "block:" + dimension + ":";
        String chestPrefix = "chest:" + dimension + ":";
        String coordinates;
        if (key.startsWith(blockPrefix)) coordinates = key.substring(blockPrefix.length());
        else if (key.startsWith(chestPrefix)) coordinates = key.substring(chestPrefix.length()).split("\\|", -1)[0];
        else return null;
        String[] parts = coordinates.split(",", -1);
        if (parts.length != 3) return null;
        try {
            return new Position(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
        } catch (NumberFormatException invalidAddress) {
            return null;
        }
    }
}
