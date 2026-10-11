package com.salts_inventory_update.platform.loader.api;

import java.nio.file.Path;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import com.salts_inventory_update.api.integration.DesktopIntegrationContext;
import com.salts_inventory_update.integration.DesktopIntegrationLoader.Source;

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

    public List<Source> integrationSources() {
        List<Source> sources = new ArrayList<>();
        for (var mod : net.fabricmc.loader.api.FabricLoader.getInstance().getAllMods()) {
            mod.findPath(DesktopIntegrationContext.DESCRIPTOR_PATH).ifPresent(path ->
                sources.add(new Source(mod.getMetadata().getId(), path)));
        }
        return List.copyOf(sources);
    }
}
