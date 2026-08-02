package com.salts_inventory_update.compat.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.ResourceLocation;

import com.salts_inventory_update.SaltsInventoryUpdate;
import com.salts_inventory_update.compat.recipebrowser.RecipeBrowserBridge;
import com.salts_inventory_update.compat.recipebrowser.RecipeBrowserSource;

@JeiPlugin
public final class SaltsJeiPlugin implements IModPlugin {
    private static final ResourceLocation UID = new ResourceLocation(SaltsInventoryUpdate.MOD_ID, "jei_window");
    private RuntimeRecipeBrowserAccess access;

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        this.access = new RuntimeRecipeBrowserAccess(jeiRuntime);
        RecipeBrowserBridge.install(RecipeBrowserSource.JEI, this.access);
    }

    @Override
    public void onRuntimeUnavailable() {
        if (this.access != null) {
            RecipeBrowserBridge.clear(RecipeBrowserSource.JEI, this.access);
            this.access = null;
        }
    }
}
