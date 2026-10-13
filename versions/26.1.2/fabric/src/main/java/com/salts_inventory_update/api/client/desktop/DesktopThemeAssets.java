package com.salts_inventory_update.api.client.desktop;

import net.minecraft.resources.Identifier;

/** Supported skin textures for external window renderers.
 * Use DesktopRenderContext.windowNineSlice with WINDOW. Other constants
 * retain the published atlas layout of this API target.
 */
public final class DesktopThemeAssets {
    public static final Identifier WINDOW = texture("window.png");
    public static final Identifier CONTAINER_WIDGETS = texture("container_widgets.png");
    public static final Identifier SCROLLBAR_BACKGROUND = texture("scroll_bar_behind.png");
    public static final Identifier SEARCH_BAR = texture("search_bar.png");
    private DesktopThemeAssets() { }
    private static Identifier texture(String file) {
        String path = "textures/gui/" + file;
        return java.util.Objects.requireNonNull(Identifier.fromNamespaceAndPath("salts_inventory_update", path));
    }
}
