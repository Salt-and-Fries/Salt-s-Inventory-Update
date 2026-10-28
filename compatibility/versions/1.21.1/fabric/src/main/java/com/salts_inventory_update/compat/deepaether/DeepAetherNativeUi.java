package com.salts_inventory_update.compat.deepaether;

import com.salts_inventory_update.compat.nativewindows.*;

public final class DeepAetherNativeUi {
    private DeepAetherNativeUi() { }
    public static void initialize() {
        NativeWindowPolish.installTemplate("deep_aether","deep_aether:combiner",()->new MockupNativeTemplate<>(NativeBatchLayouts::combiner));
        NativeWindowPolish.installTemplate("deep_aether","deep_aether:combiner_menu",()->new MockupNativeTemplate<>(NativeBatchLayouts::combiner));
    }
}
