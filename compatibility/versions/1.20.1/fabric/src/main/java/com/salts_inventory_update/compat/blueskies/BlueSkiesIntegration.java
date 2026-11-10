package com.salts_inventory_update.compat.blueskies;

import com.salts_inventory_update.api.desktop.DesktopMenuBindings;
import com.salts_inventory_update.api.integration.DesktopEnvironment;
import com.salts_inventory_update.compat.nativewindows.NativeMenuSupport;

/** Original Arcs capability inventory, opened by its publisher's request. */
public final class BlueSkiesIntegration {
    private BlueSkiesIntegration() { }
    public static void initializeCommon() {
        DesktopEnvironment.onServerStarting(server -> {
            var type=NativeMenuSupport.type("blue_skies:arc_inventory");
            if(type==null)return;
            // Its publisher uses an empty Forge opening-data writer. The reviewed
            // native template owns transport; an explicit binding would disable
            // that path and incorrectly require a nonempty payload.
            // These four restricted equipment cells are not general cargo.
            DesktopMenuBindings.registerStorage(type,(menu,slot)->false);
        });
    }
    public static void initializeClient() { BlueSkiesCharacterButton.initialize(); }
}
