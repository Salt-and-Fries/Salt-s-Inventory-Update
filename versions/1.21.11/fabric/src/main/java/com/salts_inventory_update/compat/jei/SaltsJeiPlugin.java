package com.salts_inventory_update.compat.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.Identifier;

import com.salts_inventory_update.SaltsInventoryUpdate;

@JeiPlugin
public final class SaltsJeiPlugin implements IModPlugin {
    private static final Identifier UID = Identifier.fromNamespaceAndPath(SaltsInventoryUpdate.MOD_ID, "jei_window");
    private RuntimeJeiDesktopAccess access;

    @Override
    public Identifier getPluginUid() {
        return UID;
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        this.access = new RuntimeJeiDesktopAccess(jeiRuntime);
        JeiDesktopBridge.install(this.access);
    }

    @Override
    public void onRuntimeUnavailable() {
        if (this.access != null) {
            JeiDesktopBridge.clear(this.access);
            this.access = null;
        }
    }
}
