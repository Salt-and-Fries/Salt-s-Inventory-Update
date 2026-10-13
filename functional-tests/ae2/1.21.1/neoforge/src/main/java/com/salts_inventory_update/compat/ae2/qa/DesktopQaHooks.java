package com.salts_inventory_update.compat.ae2.qa;

import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.implementations.*;
import appeng.client.gui.me.crafting.*;
import appeng.client.gui.me.items.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.BooleanSupplier;

/** Optional provider installs this only during a QA launch. Ordinary code has no Salt linkage. */
public final class DesktopQaHooks {
    public static BooleanSupplier openProbe = () -> false;
    public static Runnable closeAll = () -> {
    };
    public static java.util.function.Consumer<UUID> closeWindow = id -> {
    };

    public static java.util.function.Consumer<UUID> unlockWindow = id -> {
    };

    public record Window(int x, int y, int width, int height, int inputX, int inputY, AEBaseScreen<?> content) {
    }

    public static final Map<UUID, Window> WINDOWS = new HashMap<>();
    public static final Map<UUID, Window> COMPANIONS = new HashMap<>();
    /** Controlled UI-state captures, opt-in only; never dispatched as server actions. */
    public static final Map<UUID, Runnable> VISUAL_FIXTURES = new HashMap<>();
    public static final java.util.Set<UUID> COMPANION_READY = new java.util.HashSet<>();
    public static final Map<UUID, java.util.List<Integer>> COMPANION_OWNERS = new HashMap<>();
    public static final Map<UUID, Integer> SESSION_IDS = new HashMap<>();

    public record Control(String kind, int x, int y, int width, int height,
            boolean enabled, boolean selected, boolean keyboardFocused) {
    }

    public static final Map<UUID, java.util.List<Control>> MANAGEMENT_CONTROLS = new HashMap<>();
    public static java.util.function.BiFunction<UUID, String, Boolean> windowAction = (id, action) -> false;
    public static java.util.function.BiFunction<UUID, String, Boolean> windowAvailable = (id, action) -> false;
    public static java.util.function.Predicate<UUID> windowVisible = id -> false;
    public static java.util.function.Function<UUID, String> windowSummary = id -> "unavailable";
    public static UUID keyboardOwner;
    public static final Map<UUID, Integer> POINTER_CANCELLATIONS = new HashMap<>();
    public static final Map<UUID, String> RESIZE_DIAGNOSTICS = new HashMap<>();

    public record Environment(long revision, int width, int height, double scale) {
    }

    public static final Map<UUID, Environment> ENVIRONMENTS = new HashMap<>();

    public static final class PopupState {
        public boolean open = true;
        public boolean closed;
        public int characters;
        public int dismissals;
        public String dismissal;
    }

    public static PopupState popupState;
    public static Runnable openModalProbe = () -> {
    };
    public static Runnable closeModalProbe = () -> {
    };

    private DesktopQaHooks() {
    }
}
