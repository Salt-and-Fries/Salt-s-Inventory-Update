package com.salts_inventory_update.api.client.desktop;

import net.minecraft.resources.ResourceLocation;

/** Supported skin textures for external window renderers.
 * Use DesktopRenderContext.windowNineSlice with WINDOW. Other constants
 * retain the published atlas layout of this API target.
 */
public final class DesktopThemeAssets {
    public static final ResourceLocation WINDOW = texture("window.png");
    public static final ResourceLocation CONTAINER_WIDGETS = texture("container_widgets.png");
    public static final ResourceLocation SCROLLBAR_BACKGROUND = texture("scroll_bar_behind.png");
    public static final ResourceLocation SEARCH_BAR = texture("search_bar.png");
    private DesktopThemeAssets() { }
    private static ResourceLocation texture(String file) {
        String path = "textures/gui/" + file;
        return java.util.Objects.requireNonNull(ResourceLocation.tryParse("salts_inventory_update:" + path));
    }
}
