package com.salts_inventory_update.controlify.model;

/** Continuous recipe carousel; individual cards enter and leave through its bottom opening. */
public final class ContextualWheelSelection {
    public static final int VISIBLE_RECIPES = 10;
    /** Compatibility constant for the former ten-card page size. Recipes are no longer grouped. */
    public static final int GROUP_SIZE = VISIBLE_RECIPES;
    public static final double RECIPES_PER_SECOND = 5.0;
    public static final double STICK_ACTIVATION = .78, STICK_RELEASE = .64;
    public static final double ARC_START = Math.toRadians(210), ARC_SWEEP = Math.toRadians(300);
    private static final double TAU = Math.PI * 2, STICK_CHANGE_THRESHOLD = 0.008, MIN_CARD_OPACITY = .001;
    private double position, renderedPosition, lastRightAngle;
    private boolean rightEngaged, rightReturning, leftEngaged, leftReturning;
    private double previousRightMagnitude, previousLeftMagnitude;
    private int inner = -1, lastCount;
    private long updatedAt = Long.MIN_VALUE, renderedAt = Long.MIN_VALUE;
    public record Card(int recipeIndex, double angle, double opacity, double scale) { }

    public void reset() {
        position = renderedPosition = 0; inner = -1; lastCount = 0;
        rightEngaged = rightReturning = leftEngaged = leftReturning = false;
        previousRightMagnitude = previousLeftMagnitude = 0;
        updatedAt = renderedAt = Long.MIN_VALUE;
    }
    /** Keep the chosen recipe when localized names or available entries reorder the catalog. */
    public void reanchor(int recipeIndex, int count) {
        if (count <= 0) { reset(); return; }
        if (recipeIndex < 0 || recipeIndex >= count) throw new IndexOutOfBoundsException(recipeIndex);
        position = renderedPosition = recipeIndex; inner = topSlot(count); lastCount = count;
        updatedAt = renderedAt = Long.MIN_VALUE;
        // Retain the last observed stick direction: an unchanged held stick is not a new choice.
    }
    public double position() { return normalized(position, lastCount); }
    public int visibleSize(int count) { return Math.min(VISIBLE_RECIPES, Math.max(0, count)); }
    public int visibleStart(int count) { return count <= 0 ? 0 : Math.floorMod((int) Math.floor(position) - topSlot(count), count); }
    public int visibleIndex(int count, int slot) {
        return count <= 0 || slot < 0 || slot >= visibleSize(count) ? -1 : Math.floorMod(visibleStart(count) + slot, count);
    }
    public int selected(int count) { return visibleIndex(count, inner < 0 ? topSlot(count) : Math.min(inner, Math.max(0, visibleSize(count) - 1))); }
    public int displayedStart(int count) { return count <= 0 ? 0 : Math.floorMod((int) Math.floor(renderedPosition) - topSlot(count), count); }
    public int displayedSelected(int count) {
        if (count <= 0) return -1;
        int slot = inner < 0 ? topSlot(count) : Math.min(inner, visibleSize(count) - 1);
        // The outgoing edge may fall below the renderer's visibility cutoff between input
        // samples. Keep preview/confirmation on its next visible neighbor, without moving
        // the carousel or changing the player's continuous fine position.
        if (cardOpacity(slot, visibleSize(count), renderedPosition) <= MIN_CARD_OPACITY) slot++;
        return Math.floorMod(displayedStart(count) + slot, count);
    }
    public double displayedCardAngle(int slot, int count) { return cardAngle(slot, visibleSize(count), renderedPosition); }
    /** Legacy observations now refer to the rolling visible range, never a fixed group. */
    public int groupStart(int count) { return visibleStart(count); }
    public int groupSize(int count) { return visibleSize(count); }
    public int groupCount(int count) { return Math.max(0, count); }
    public static double outerMarkerAngle(int index, int count) {
        if (index < 0 || index >= count) throw new IndexOutOfBoundsException(index);
        return index * TAU / count;
    }
    public double positionAngle(int count) { return count <= 0 ? 0 : normalized(position, count) * TAU / count; }
    public double highlightHalfAngle(int count) { return count <= 0 ? 0 : Math.PI * visibleSize(count) / count; }
    public int topSlot(int count) { return visibleSize(count) / 2; }
    public double cardAngle(int slot, int count) { return cardAngle(slot, visibleSize(count), position); }
    public void update(int count, float rightX, float rightY, float leftX, float leftY) {
        update(count, rightX, rightY, leftX, leftY, false, false, System.nanoTime());
    }
    public void update(int count, float rightX, float rightY, float leftX, float leftY,
                       boolean rotateLeftHeld, boolean rotateRightHeld, long nowNanos) {
        if (count < 0) throw new IllegalArgumentException("Recipe count must be nonnegative");
        if (!Float.isFinite(rightX) || !Float.isFinite(rightY) || !Float.isFinite(leftX) || !Float.isFinite(leftY))
            throw new IllegalArgumentException("Stick coordinates must be finite");
        if (lastCount != count) {
            position = normalized(position, count); renderedPosition = position;
            inner = inner < 0 ? topSlot(count) : Math.min(inner, Math.max(0, visibleSize(count) - 1)); lastCount = count;
        }
        double elapsed = updatedAt == Long.MIN_VALUE ? 0 : Math.max(0, (nowNanos - updatedAt) / 1_000_000_000.0);
        updatedAt = nowNanos;
        if (count == 0) { reset(); updatedAt = nowNanos; return; }
        double rightMagnitude = Math.hypot(rightX, rightY), leftMagnitude = Math.hypot(leftX, leftY);
        if (rightMagnitude < STICK_RELEASE) { rightEngaged = rightReturning = false; }
        else {
            if (rightEngaged && rightMagnitude < previousRightMagnitude - .08 && rightMagnitude < .95) rightReturning = true;
            if (rightMagnitude > previousRightMagnitude + .08) rightReturning = false;
        }
        if (rightMagnitude >= STICK_ACTIVATION && !rightReturning) {
            double angle = stickAngle(rightX, rightY);
            // Fixed stick samples are observations, not repeated commands. Bumper fine-positioning
            // remains stable until the stick is deliberately moved or freshly deflected.
            if (!rightEngaged || Math.abs(shortestDelta(angle, lastRightAngle, TAU)) >= Math.min(STICK_CHANGE_THRESHOLD, TAU / count * .2)) {
                double target = normalized(angle / TAU * count, count);
                position += shortestDelta(target, normalized(position, count), count);
                // Device/processed float normalization can move a dense marker center by a few
                // hundred-thousandths of a recipe; preserve its integer identity without pages.
                if (Math.abs(position - Math.rint(position)) < .0001) position = Math.rint(position);
                inner = topSlot(count); lastRightAngle = angle;
            }
            rightEngaged = true;
        }
        int rotation = (rotateRightHeld ? 1 : 0) - (rotateLeftHeld ? 1 : 0);
        position += rotation * elapsed * RECIPES_PER_SECOND;
        if (leftMagnitude < STICK_RELEASE) { leftEngaged = leftReturning = false; }
        else {
            if (leftEngaged && leftMagnitude < previousLeftMagnitude - .08 && leftMagnitude < .95) leftReturning = true;
            if (leftMagnitude > previousLeftMagnitude + .08) leftReturning = false;
        }
        if (leftMagnitude >= STICK_ACTIVATION && !leftReturning) {
            leftEngaged = true;
            double angle = stickAngle(leftX, leftY), best = -Double.MAX_VALUE;
            for (int slot = 0; slot < visibleSize(count); slot++) {
                if (cardOpacity(slot, visibleSize(count), renderedPosition) <= MIN_CARD_OPACITY) continue;
                double similarity = Math.cos(angle - displayedCardAngle(slot, count));
                if (similarity > best) { best = similarity; inner = slot; }
            }
        }
        previousRightMagnitude = rightMagnitude; previousLeftMagnitude = leftMagnitude;
    }
    /** Overscan cards fade through the opening, preserving fractional movement between recipes. */
    public java.util.List<Card> cards(int count, long nowNanos, boolean animationsEnabled) {
        if (count <= 0) return java.util.List.of();
        if (count == 1) return java.util.List.of(new Card(0, 0, 1, 1));
        advancePresentation(nowNanos, animationsEnabled);
        int size = visibleSize(count), base = (int) Math.floor(renderedPosition) - topSlot(count);
        java.util.List<Card> cards = new java.util.ArrayList<>();
        for (int slot = -1; slot <= size; slot++) {
            double opacity = cardOpacity(slot, size, renderedPosition);
            if (opacity <= MIN_CARD_OPACITY) continue;
            cards.add(new Card(Math.floorMod(base + slot, count), cardAngle(slot, size, renderedPosition), opacity, .78 + .22 * opacity));
        }
        return java.util.List.copyOf(cards);
    }
    public void advancePresentation(long nowNanos, boolean animationsEnabled) {
        double elapsed = renderedAt == Long.MIN_VALUE ? 0 : Math.max(0, (nowNanos - renderedAt) / 1_000_000_000.0);
        renderedAt = nowNanos;
        if (!animationsEnabled) renderedPosition = position;
        else renderedPosition += (position - renderedPosition) * (1 - Math.exp(-elapsed * 22));
        if (Math.abs(position - renderedPosition) < .00001) renderedPosition = position;
    }
    public static int sector(float x, float y, int sectors) {
        return sectors <= 0 ? -1 : Math.min(sectors - 1, (int) Math.floor(stickAngle(x, y) / TAU * sectors));
    }
    private static double cardAngle(int slot, int visibleSize, double position) {
        if (visibleSize <= 1) return 0;
        double fraction = position - Math.floor(position);
        double distance = slot - visibleSize / 2 - fraction;
        int sideCount = distance < 0 ? visibleSize / 2 : visibleSize - visibleSize / 2 - 1;
        double offset = Math.abs(distance), angle;
        if (sideCount == 0) angle = Math.min(1, offset) * Math.PI;
        else if (offset <= sideCount) angle = offset / sideCount * Math.toRadians(135);
        else angle = Math.toRadians(135) + Math.min(1, offset - sideCount) * Math.toRadians(45);
        // The top recipe stays exactly north. Both ends approach the authored doorway
        // at135 degrees and reach the hidden bottom seam before wrapping, even in tiny catalogs.
        return Math.copySign(angle, -distance);
    }
    private static double cardOpacity(int slot, int visibleSize, double position) {
        double fraction = position - Math.floor(position);
        double distance = slot + .5 - fraction;
        return Math.max(0, Math.min(1, Math.min(distance + .5, visibleSize + .5 - distance)));
    }
    private static double stickAngle(float x, float y) {
        double angle = Math.atan2(y, x) + Math.PI / 2;
        return angle < 0 ? angle + TAU : angle;
    }
    private static double normalized(double value, int count) { return count <= 0 ? 0 : value - Math.floor(value / count) * count; }
    private static double shortestDelta(double target, double current, double period) {
        double value = target - current;
        return value - Math.floor((value + period / 2) / period) * period;
    }
}
