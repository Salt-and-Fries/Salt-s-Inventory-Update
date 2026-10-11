package com.salts_inventory_update.platform.loader.api;

import java.nio.file.Path;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import com.salts_inventory_update.api.integration.DesktopIntegrationContext;
import com.salts_inventory_update.integration.DesktopIntegrationLoader.Source;

import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLPaths;

public final class FabricLoader {
    private static final FabricLoader INSTANCE = new FabricLoader();

    private FabricLoader() {
    }

    public static FabricLoader getInstance() {
        return INSTANCE;
    }

    public Path getConfigDir() {
        return FMLPaths.CONFIGDIR.get();
    }

    public boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    public List<Source> integrationSources() {
        List<Source> sources = new ArrayList<>();
        for (var mod : ModList.get().getMods()) {
            try {
                com.salts_inventory_update.integration.DesktopModFileResources.find(mod.getModId(),
                    mod.getOwningFile().getFile(), net.neoforged.neoforgespi.locating.IModFile.class)
                    .ifPresent(sources::add);
            } catch (ReflectiveOperationException failure) {
                com.salts_inventory_update.api.integration.DesktopDiagnostics.report(
                    com.salts_inventory_update.api.integration.DesktopDiagnostics.Level.ERROR,
                    mod.getModId(), "discovery", "Cannot inspect mod resources", failure);
            }
        }
        return List.copyOf(sources);
    }
}
