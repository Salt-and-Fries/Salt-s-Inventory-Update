package com.salts_inventory_update.compat.raised;

import com.salts_inventory_update.api.client.desktop.DesktopHudApi;
import dev.yurisuika.raised.api.RaisedApi;

/** Optional client adapter. Every interaction with Salt's is through its supported public API. */
public final class RaisedHudIntegration {
    private RaisedHudIntegration() { }

    public static void initialize() {
        DesktopHudApi.register("salts_inventory_compat:raised", 0, (context, layout) ->
            layout.translated(RaisedApi.getX("minecraft:hotbar"), RaisedApi.getY("minecraft:hotbar")));
    }
}
