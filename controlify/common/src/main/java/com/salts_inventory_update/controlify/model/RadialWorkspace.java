package com.salts_inventory_update.controlify.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Controller workspace state, independent of Minecraft and Controlify. Window ids refer to the
 * existing core windows: a presentation change never creates or destroys their backing containers.
 */
public final class RadialWorkspace<K> {
    public enum Mode { SIDE_BY_SIDE, RADIAL, TUCKED_AWAY }

    /** Sixty degrees around screen-down are exclusively the tuck-away action. */
    public static final double TUCK_HALF_ANGLE = Math.PI / 6.0;
    private static final double TAU = Math.PI * 2.0;

    private final K playerInventory;
    private final K character;
    private final Map<K, Boolean> windows = new LinkedHashMap<>();
    private List<K> radialPresentationOrder = List.of();
    private Mode mode = Mode.SIDE_BY_SIDE;
    private boolean active;
    private K focused;
    private K selected;
    private K highlighted;
    private boolean tuckHighlighted;
    private long closeHeldMillis;
    private boolean closeHoldFired;

    public RadialWorkspace(K playerInventory, K character) {
        this.playerInventory = Objects.requireNonNull(playerInventory, "playerInventory");
        this.character = Objects.requireNonNull(character, "character");
        if (playerInventory.equals(character)) {
            throw new IllegalArgumentException("Player inventory and character must be different windows");
        }
        windows.put(playerInventory, false);
        windows.put(character, false);
        focused = playerInventory;
        selected = character;
    }

    /** Adds an already existing core window without changing the current presentation. */
    public void register(K id, boolean closable) {
        Objects.requireNonNull(id, "id");
        windows.put(id, !isPermanent(id) && closable);
    }

    public void addAndOpen(K id, boolean closable) {
        register(id, closable);
        open(id);
    }

    /** Applies the specified default focus, except when gameplay has the workspace tucked away. */
    public void open(K id) {
        requireWindow(id);
        if (active && mode == Mode.TUCKED_AWAY) {
            return;
        }
        active = true;
        mode = Mode.SIDE_BY_SIDE;
        focused = playerInventory;
        selected = id.equals(focused) ? firstOther(focused) : id;
        highlighted = null;
        tuckHighlighted = false;
        resetCloseHold();
    }

    public boolean beginRadial() {
        if (!active || mode != Mode.SIDE_BY_SIDE) {
            return false;
        }
        mode = Mode.RADIAL;
        highlighted = selected;
        tuckHighlighted = false;
        return true;
    }

    /** Stick coordinates use screen coordinates: positive y points down. Deadzone retains selection. */
    public void pointStick(double x, double y, double deadzone) {
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(deadzone)
                || deadzone < 0.0 || deadzone > 1.0) {
            throw new IllegalArgumentException("Stick coordinates must be finite and deadzone within [0, 1]");
        }
        if (!active || mode != Mode.RADIAL) {
            return;
        }
        double magnitude = Math.hypot(x, y);
        if (magnitude <= deadzone) {
            return;
        }
        if (y / magnitude >= Math.cos(TUCK_HALF_ANGLE)) {
            highlighted = null;
            tuckHighlighted = true;
            return;
        }
        double angle = Math.atan2(y, x);
        List<K> order = radialOrder();
        double best = -Double.MAX_VALUE;
        K closest = null;
        for (int i = 0; i < order.size(); i++) {
            double similarity = Math.cos(angle - radialAngle(i, order.size()));
            if (similarity > best) {
                closest = order.get(i);
                best = similarity;
            }
        }
        highlighted = closest;
        tuckHighlighted = false;
    }

    /** Committing the dedicated bottom action hides the same workspace without removing windows. */
    public boolean releaseRadial() {
        if (!active || mode != Mode.RADIAL) {
            return false;
        }
        if (tuckHighlighted) {
            mode = Mode.TUCKED_AWAY;
            resetCloseHold();
        } else {
            if (highlighted != null && windows.containsKey(highlighted) && !focused.equals(highlighted)) {
                selected = highlighted;
            }
            mode = Mode.SIDE_BY_SIDE;
        }
        highlighted = null;
        tuckHighlighted = false;
        return true;
    }

    public boolean restore() {
        if (!active || mode != Mode.TUCKED_AWAY) {
            return false;
        }
        mode = Mode.SIDE_BY_SIDE;
        resetCloseHold();
        return true;
    }

    /** Positive moves clockwise; the center window is never a selected-window candidate. */
    public boolean cycleSelected(int direction) {
        if (!active || mode != Mode.SIDE_BY_SIDE || direction == 0) {
            return false;
        }
        List<K> order = radialOrder();
        int index = order.indexOf(selected);
        selected = order.get(Math.floorMod(index + Integer.signum(direction), order.size()));
        return true;
    }

    public boolean swapHighlighted() {
        if (!active || mode != Mode.RADIAL || tuckHighlighted || highlighted == null
                || focused.equals(highlighted) || !windows.containsKey(highlighted)) {
            return false;
        }
        K previous = focused;
        // Exchange the two visual positions as well as their roles. Otherwise the
        // unchanged stick points at a different icon on the very next input sample.
        List<K> order = presentationOrder();
        Collections.swap(order, order.indexOf(previous), order.indexOf(highlighted));
        radialPresentationOrder = List.copyOf(order);
        focused = highlighted;
        selected = previous;
        highlighted = previous;
        return true;
    }

    /** Returns the existing window id which the caller should close through the core API. */
    public Optional<K> closeHighlighted() {
        if (!active || mode != Mode.RADIAL || tuckHighlighted || highlighted == null
                || !closable(highlighted)) {
            return Optional.empty();
        }
        K closed = highlighted;
        remove(closed);
        return Optional.of(closed);
    }

    /** Removes a window closed by the core, protecting the two mandatory windows. */
    public boolean remove(K id) {
        if (isPermanent(id) || windows.remove(id) == null) {
            return false;
        }
        repairSelection();
        return true;
    }

    /** Reconciles external closures without reordering or reopening any surviving windows. */
    public void retain(Collection<K> available) {
        Objects.requireNonNull(available, "available");
        windows.keySet().removeIf(id -> !isPermanent(id) && !available.contains(id));
        repairSelection();
    }

    /**
     * Accumulates input-frame elapsed time. Supply the regular mod's hold threshold, in milliseconds.
     * The action fires once per press, in either inventory mode, and never during tucked gameplay.
     */
    public List<K> updateCloseHold(boolean held, long elapsedMillis, long thresholdMillis) {
        if (elapsedMillis < 0 || thresholdMillis <= 0) {
            throw new IllegalArgumentException("Elapsed time must be nonnegative and threshold positive");
        }
        if (!held || !active || mode == Mode.TUCKED_AWAY) {
            resetCloseHold();
            return List.of();
        }
        if (closeHoldFired) {
            return List.of();
        }
        closeHeldMillis = elapsedMillis >= thresholdMillis - Math.min(closeHeldMillis, thresholdMillis)
                ? thresholdMillis : closeHeldMillis + elapsedMillis;
        if (closeHeldMillis < thresholdMillis) {
            return List.of();
        }
        closeHoldFired = true;
        return closeAll();
    }

    /** Leaves permanent windows registered but deactivates the UI and returns control to gameplay. */
    public List<K> closeAll() {
        List<K> closed = new ArrayList<>();
        windows.forEach((id, closable) -> { if (closable) closed.add(id); });
        closed.forEach(windows::remove);
        active = false;
        mode = Mode.SIDE_BY_SIDE;
        focused = playerInventory;
        selected = firstOther(focused);
        highlighted = null;
        tuckHighlighted = false;
        return List.copyOf(closed);
    }

    /** Called when controller ownership ends, preserving windows for the regular desktop UI. */
    public void deactivate() {
        active = false;
        mode = Mode.SIDE_BY_SIDE;
        highlighted = null;
        tuckHighlighted = false;
        resetCloseHold();
    }

    public Mode mode() { return mode; }
    public boolean active() { return active; }
    public boolean gameplayControls() { return !active || mode == Mode.TUCKED_AWAY; }
    public K focused() { return focused; }
    public K selected() { return selected; }
    public K playerInventory() { return playerInventory; }
    public K character() { return character; }
    public Optional<K> highlighted() { return Optional.ofNullable(highlighted); }
    public boolean tuckHighlighted() { return tuckHighlighted; }
    public boolean isPermanent(K id) { return playerInventory.equals(id) || character.equals(id); }
    public boolean closable(K id) { return Boolean.TRUE.equals(windows.get(id)); }
    public List<K> windows() { return List.copyOf(windows.keySet()); }

    public List<K> radialOrder() {
        List<K> order = presentationOrder();
        order.remove(focused);
        return Collections.unmodifiableList(order);
    }

    private List<K> presentationOrder() {
        List<K> order = new ArrayList<>(windows.size());
        for (K id : radialPresentationOrder) if (windows.containsKey(id)) order.add(id);
        for (K id : windows.keySet()) if (!order.contains(id)) order.add(id);
        return order;
    }

    /** Angle in screen coordinates, shared with the renderer to avoid visual/navigation drift. */
    public static double radialAngle(int index, int count) {
        if (count <= 0 || index < 0 || index >= count) {
            throw new IllegalArgumentException("Radial index must identify an existing window");
        }
        double start = Math.PI / 2.0 + TUCK_HALF_ANGLE;
        double available = TAU - 2.0 * TUCK_HALF_ANGLE;
        return start + available * (index + 0.5) / count;
    }

    private void repairSelection() {
        if (!windows.containsKey(focused)) {
            focused = playerInventory;
        }
        if (!windows.containsKey(selected) || focused.equals(selected)) {
            selected = firstOther(focused);
        }
        if (highlighted != null && (!windows.containsKey(highlighted) || focused.equals(highlighted))) {
            highlighted = selected;
        }
    }

    private K firstOther(K id) {
        return windows.keySet().stream().filter(candidate -> !candidate.equals(id)).findFirst().orElseThrow();
    }

    private void requireWindow(K id) {
        if (!windows.containsKey(id)) {
            throw new IllegalArgumentException("Window must be registered before opening: " + id);
        }
    }

    private void resetCloseHold() {
        closeHeldMillis = 0;
        closeHoldFired = false;
    }
}
