package com.salts_inventory_update.controlify.render;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.List;
import com.salts_inventory_update.controlify.model.TextureSectorMask;
import com.salts_inventory_update.api.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.ResourceLocation;

/** Required texture contract for the 26.3 contextual wheels; every surface is resource-pack editable. */
public final class ContextualRadialAssets {
    public enum Element {
        BACKDROP(760, 450),
        RECIPE_CARD(32, 32), UNAVAILABLE_RECIPE_CARD(32, 32), HOVERED_CARD(32, 32),
        CATEGORY(32, 32), SELECTED_CATEGORY(32, 32),
        CREATIVE_TAB(32, 32), HOVERED_CREATIVE_TAB(32, 32), ACTIVE_CREATIVE_TAB(32, 32),
        WHEEL_RING(288, 288), WHEEL_RING_HIGHLIGHT(288, 288),
        CAROUSEL_OPENING(160, 90), ALPHABET_LABEL(10, 12), HIGHLIGHTED_ALPHABET_LABEL(10, 12),
        ALPHABET_ANCHOR(2, 2), ALPHABET_LEADER(1, 1);

        private final int width, height;
        Element(int width, int height) { this.width = width; this.height = height; }
        public int width() { return width; }
        public int height() { return height; }
    }
    public record Sprite(ResourceLocation texture, int width, int height) {
        public Sprite {
            Objects.requireNonNull(texture, "texture");
            if (width <= 0 || height <= 0) throw new IllegalArgumentException("Sprite dimensions must be positive");
        }
    }
    public record Theme(Map<Element, Sprite> sprites, int panel, int border, int card, int missing,
                        int hover, int activeTab, int ring, int text, int mutedText) {
        public Theme {
            sprites = Map.copyOf(sprites);
            for (Element element : Element.values()) {
                if (!sprites.containsKey(element)) throw new IllegalArgumentException("Missing contextual texture: " + element);
            }
        }
    }
    public static final Theme DEFAULT = new Theme(defaultSprites(), 0xBB0B1016, 0xFF41505D, 0xFF1E3138,
            0xFF422C32, 0xFF4B3600, 0xFF8DE7BB, 0xFF5B5544, 0xFF29251D, 0xFF403A2F);
    private static Theme theme = DEFAULT;
    private static List<TextureSectorMask.Strip> highlightStrips = List.of();
    private static double highlightAngle = Double.NaN;
    private static double highlightHalfAngle = Double.NaN;
    private static int highlightWidth, highlightHeight;
    private ContextualRadialAssets() { }
    public static Theme theme() { return theme; }
    public static void setTheme(Theme value) { theme = Objects.requireNonNull(value); }
    public static ResourceLocation textureId(Element element) {
        return ResourceLocation.fromNamespaceAndPath("salts_inventory_controlify", "textures/gui/contextual/wheel/"
                + element.name().toLowerCase(Locale.ROOT) + ".png");
    }
    private static Map<Element, Sprite> defaultSprites() {
        return Arrays.stream(Element.values()).collect(Collectors.toUnmodifiableMap(
                element -> element, element -> new Sprite(textureId(element), element.width(), element.height())));
    }
    public static void draw(GuiGraphicsExtractor graphics, Element element, int x, int y, int width, int height, int color) {
        Sprite sprite = theme.sprites().get(Objects.requireNonNull(element));
        try (var tint=WorkspaceImmediateTint.enter(graphics,color)) {
            graphics.blit(null, sprite.texture(), x, y, 0, 0,
                width, height, sprite.width(), sprite.height(), sprite.width(), sprite.height(), color);
        }
    }
    public static void drawRegion(GuiGraphicsExtractor graphics, Element element, int x, int y, int width, int height,
            int sourceX, int sourceY, int sourceWidth, int sourceHeight, int color) {
        Sprite sprite = theme.sprites().get(Objects.requireNonNull(element));
        try (var tint=WorkspaceImmediateTint.enter(graphics,color)) {
            graphics.blit(null, sprite.texture(), x, y, sourceX, sourceY,
                width, height, sourceWidth, sourceHeight, sprite.width(), sprite.height(), color);
        }
    }
    /** Crop a small aimed section of the full ring without rotating its authored pixels or lighting. */
    public static void drawRingHighlight(GuiGraphicsExtractor graphics, int x, int y, double angle) {
        drawRingHighlight(graphics, x, y, angle, TextureSectorMask.HALF_ANGLE);
    }
    public static void drawRingHighlight(GuiGraphicsExtractor graphics, int x, int y, double angle, double halfAngle) {
        Sprite sprite = theme.sprites().get(Element.WHEEL_RING_HIGHLIGHT);
        if (highlightAngle != angle || highlightHalfAngle != halfAngle || highlightWidth != sprite.width() || highlightHeight != sprite.height()) {
            highlightStrips = TextureSectorMask.create(sprite.width(), sprite.height(), angle, halfAngle);
            highlightAngle = angle; highlightHalfAngle = halfAngle; highlightWidth = sprite.width(); highlightHeight = sprite.height();
        }
        double scaleX = Element.WHEEL_RING.width() / (double) sprite.width();
        double scaleY = Element.WHEEL_RING.height() / (double) sprite.height();
        for (var strip : highlightStrips) {
            int left = (int)Math.round(strip.x() * scaleX), top = (int)Math.round(strip.y() * scaleY);
            int right = (int)Math.round((strip.x() + strip.width()) * scaleX);
            int bottom = (int)Math.round((strip.y() + strip.height()) * scaleY);
            if (right > left && bottom > top) drawRegion(graphics, Element.WHEEL_RING_HIGHLIGHT,
                x + left, y + top, right - left, bottom - top, strip.x(), strip.y(), strip.width(), strip.height(), 0xFFFFFFFF);
        }
    }
    /** Compatibility return value: shipped textures are always drawn; missing files use native resource diagnostics. */
    public static boolean texture(GuiGraphicsExtractor graphics, Element element, int x, int y, int width, int height) {
        draw(graphics, element, x, y, width, height, 0xFFFFFFFF);
        return true;
    }
    public static void surface(GuiGraphicsExtractor graphics, Element element, int x, int y, int width, int height, int border, int fill) {
        draw(graphics, element, x, y, width, height, 0xFFFFFFFF);
    }
}
