package com.salts_inventory_update.compat.additionz;

import com.salts_inventory_update.api.integration.DesktopEnvironment;
import com.salts_inventory_update.api.client.desktop.DesktopClientApi;
import com.salts_inventory_update.compat.nativewindows.*;

public final class AdditionZIntegration {
    private AdditionZIntegration() { }
    public static void initializeCommon() {
        DesktopEnvironment.onServerStarting(server->DesktopEnvironment.registerIntegration("additionz",()->
            NativeMenuSupport.support(NativeMenuSupport.type("minecraft:fletching"),"net.additionz.misc.FletchingScreenHandler",false)));
    }
    public static void initializeClient() {
        NativeMenuSupport.bindNative(NativeMenuSupport.type("minecraft:fletching"),false);
        DesktopClientApi.onClientStarted(client->NativeMenuSupport.bindNative(NativeMenuSupport.type("minecraft:fletching"),false));
        NativeWindowPolish.installTemplate("additionz","minecraft:fletching",()->new MockupNativeTemplate<>(NativeBatchLayouts::fletching));
        NativeWindowPolish.installTemplate("additionz","additionz:chunk_loader",()->new MockupNativeTemplate<>(NativeBatchLayouts::chunkLoader));
    }
}
