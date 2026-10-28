package com.salts_inventory_update.compat.additionz;

import com.salts_inventory_update.api.integration.DesktopEnvironment;
import com.salts_inventory_update.api.client.desktop.DesktopClientApi;
import com.salts_inventory_update.compat.nativewindows.*;

public final class AdditionZIntegration {
    private AdditionZIntegration() { }
    public static void initializeCommon() {
        DesktopEnvironment.onServerStarting(server->DesktopEnvironment.registerIntegration("additionz",()->
            NativeMenuSupport.support(NativeMenuSupport.type("additionz:fletching"),"net.additionz.misc.FletchingScreenHandler",false)));
    }
    public static void initializeClient() {
        // AdditionZ 1.4 moved this menu out of Minecraft's namespace.
        NativeMenuSupport.bindNative(NativeMenuSupport.type("additionz:fletching"),false);
        DesktopClientApi.onClientStarted(client->NativeMenuSupport.bindNative(NativeMenuSupport.type("additionz:fletching"),false));
        NativeWindowPolish.installTemplate("additionz","additionz:fletching",()->new MockupNativeTemplate<>(NativeBatchLayouts::fletching));
        NativeWindowPolish.installTemplate("additionz","additionz:chunk_loader",()->new MockupNativeTemplate<>(NativeBatchLayouts::chunkLoader));
    }
}
