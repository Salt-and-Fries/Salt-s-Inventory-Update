package com.salts_inventory_update.platform.loader.api;

import java.nio.file.Path;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import com.salts_inventory_update.api.integration.DesktopIntegrationContext;
import com.salts_inventory_update.integration.DesktopIntegrationLoader.Source;

import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLPaths;

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
            Path path = mod.getOwningFile().getFile().findResource(DesktopIntegrationContext.DESCRIPTOR_PATH);
            if (Files.isRegularFile(path)) sources.add(new Source(mod.getModId(), path));
        }
        return List.copyOf(sources);
    }
}
