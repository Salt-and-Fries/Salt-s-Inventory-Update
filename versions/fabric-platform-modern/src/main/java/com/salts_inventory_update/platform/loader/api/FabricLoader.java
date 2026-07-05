package com.salts_inventory_update.platform.loader.api;

import java.nio.file.Path;

public final class FabricLoader {
    private static final FabricLoader INSTANCE = new FabricLoader();

    private FabricLoader() {
    }

    public static FabricLoader getInstance() {
        return INSTANCE;
    }

    public Path getConfigDir() {
        return net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir();
    }

    public boolean isModLoaded(String modId) {
        return net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded(modId);
    }
}
