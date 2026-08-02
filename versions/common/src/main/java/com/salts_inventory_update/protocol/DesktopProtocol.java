package com.salts_inventory_update.protocol;

/** Mapping-independent desktop protocol contract shared by every game version. */
public final class DesktopProtocol {
    public static final int VERSION = 2;
    public static final int HELLO_TIMEOUT_TICKS = 100;

    public static final long CAP_INVENTORY_TOPOLOGY = 1L;
    public static final long CAP_LINK_GRAPH = 1L << 1;
    public static final long CAP_RECIPE_TRANSFER = 1L << 2;
    public static final long CAP_CUSTOM_WINDOWS = 1L << 3;
    public static final long KNOWN_CAPABILITIES = CAP_INVENTORY_TOPOLOGY
        | CAP_LINK_GRAPH
        | CAP_RECIPE_TRANSFER
        | CAP_CUSTOM_WINDOWS;

    public static final int MAX_ENVELOPE_BYTES = 1024 * 1024;
    public static final int MAX_CUSTOM_DATA_BYTES = 32 * 1024;
    public static final int MAX_SOURCE_TEXT_LENGTH = 512;
    public static final int MAX_EXPANSION_SLOTS = 4_096;
    public static final int MAX_DESKTOP_SESSIONS = 16;
    public static final int MAX_OPEN_SESSION_ITEMS = 8_192;
    public static final int MAX_MENU_DATA_VALUES = 4_096;
    public static final int MAX_MOUNT_COLUMNS = 5;
    public static final int MAX_FORCED_MENU_IDS = 256;
    public static final int MAX_IDENTIFIER_LENGTH = 256;

    public static final int MAX_RECIPE_SLOTS = 128;
    public static final int MAX_TRANSFER_REQUIREMENTS = 128;
    public static final int MAX_ALTERNATIVES_PER_REQUIREMENT = 32;
    public static final int MAX_TRANSFER_ALTERNATIVES = 512;
    public static final int MAX_TRANSFER_CRAFTS = 64;
    public static final int MAX_TRANSFER_ATTEMPTS = 100_000;

    public static final int MAX_DORMANT_SOURCES = 16;
    public static final int DORMANT_SOURCE_TTL_TICKS = 6_000;
    public static final int DORMANT_PROBE_INTERVAL_TICKS = 10;
    public static final int MAX_LINK_NODES = 128;
    public static final int MAX_LINK_EDGES = 512;

    public static final int MAX_TOMS_COMPRESSED_BYTES = 30 * 1024;
    public static final int MAX_TOMS_DECOMPRESSED_BYTES = 2 * 1024 * 1024;
    public static final int MAX_TOMS_DEPTH = 64;
    public static final int MAX_TOMS_NODES = 4_096;
    public static final int MAX_TOMS_STRING_BYTES = 8 * 1024;
    public static final int MAX_TOMS_PRIMITIVE_ARRAY_BYTES = 128 * 1024;

    private DesktopProtocol() {
    }

    public static int requireCount(String field, int value, int maximum) {
        if (value < 0 || value > maximum) {
            throw new IllegalArgumentException(field + " must be between 0 and " + maximum + ", got " + value);
        }
        return value;
    }

    public static long sanitizeCapabilities(long capabilities) {
        return capabilities & KNOWN_CAPABILITIES;
    }
}
