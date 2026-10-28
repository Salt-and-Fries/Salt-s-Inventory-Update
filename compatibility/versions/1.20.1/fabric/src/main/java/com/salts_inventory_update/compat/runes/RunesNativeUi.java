package com.salts_inventory_update.compat.runes;

import com.salts_inventory_update.compat.nativewindows.*;
import net.minecraft.resources.ResourceLocation;

public final class RunesNativeUi {
    private RunesNativeUi() { }
    public static void initialize() {
        NativeWindowPolish.installTemplate("runes","runes:crafting",()->new MockupNativeTemplate<>(NativeBatchLayouts::runes,
            new MockupNativeTemplate.Emblem(new ResourceLocation("runes","textures/gui/crafting_altar.png"),10,10,38,34)));
    }
}
