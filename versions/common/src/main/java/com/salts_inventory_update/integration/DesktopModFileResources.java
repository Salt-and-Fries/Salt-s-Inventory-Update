package com.salts_inventory_update.integration;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import com.salts_inventory_update.api.integration.DesktopIntegrationContext;
import com.salts_inventory_update.integration.DesktopIntegrationLoader.Source;

/** Bridges the different mod-file resource SPIs in the currently supported NeoForge versions. */
public final class DesktopModFileResources {
    private DesktopModFileResources() { }

    public static Optional<Source> find(String modId, Object modFile, Class<?> fileInterface)
        throws ReflectiveOperationException {
        String resource = DesktopIntegrationContext.DESCRIPTOR_PATH;
        Method contents;
        try {
            contents = fileInterface.getMethod("getContents");
        } catch (NoSuchMethodException olderSpi) {
            Path path = (Path) fileInterface.getMethod("findResource", String[].class)
                .invoke(modFile, (Object) new String[]{resource});
            return Files.isRegularFile(path) ? Optional.of(new Source(modId, path)) : Optional.empty();
        }
        Object jar = contents.invoke(modFile);
        Class<?> jarInterface = contents.getReturnType();
        if (!(boolean) jarInterface.getMethod("containsFile", String.class).invoke(jar, resource)) {
            return Optional.empty();
        }
        Method openFile = jarInterface.getMethod("openFile", String.class);
        return Optional.of(new Source(modId, () -> {
            try {
                return (InputStream) openFile.invoke(jar, resource);
            } catch (ReflectiveOperationException failure) {
                throw new IOException("Cannot read integration descriptor from " + modId, failure);
            }
        }));
    }
}
