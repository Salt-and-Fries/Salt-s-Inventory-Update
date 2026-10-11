package com.salts_inventory_update.integration;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.function.Predicate;

import com.salts_inventory_update.api.integration.DesktopDiagnostics;
import com.salts_inventory_update.api.integration.DesktopDiagnostics.Level;
import com.salts_inventory_update.api.integration.DesktopIntegration;
import com.salts_inventory_update.api.integration.DesktopIntegrationContext;
import com.salts_inventory_update.api.integration.DesktopIntegrationContext.Phase;

/** Loader-independent discovery. No Minecraft or client references are allowed in this class. */
public final class DesktopIntegrationLoader {
    @FunctionalInterface
    public interface DescriptorInput {
        InputStream open() throws IOException;
    }

    public record Source(String modId, DescriptorInput input) {
        public Source(String modId, Path descriptor) {
            this(modId, () -> Files.newInputStream(descriptor));
        }
    }

    private final Set<String> attempted = new HashSet<>();
    private final Set<String> failedCommon = new HashSet<>();

    public synchronized void load(Collection<Source> sources, Phase phase, ClassLoader classLoader,
                                  Predicate<String> loadedMods) {
        for (Source source : sources.stream().sorted(Comparator.comparing(Source::modId)).toList()) {
            String key = source.modId() + ":" + phase;
            if (!loadedMods.test(source.modId()) || !attempted.add(key)) continue;
            try {
                Properties properties = read(source.input());
                // A jar can contain multiple loader mod records. Only its declared owner runs it.
                String owner = properties.getProperty("modId", "").trim();
                if (owner.isEmpty()) throw new IllegalArgumentException("Integration descriptor needs modId");
                if (!owner.equals(source.modId())) continue;
                int api = Integer.parseInt(properties.getProperty("apiVersion", ""));
                if (api != DesktopIntegrationContext.API_VERSION) {
                    throw new IllegalArgumentException("Provider targets API " + api + "; installed API is "
                        + DesktopIntegrationContext.API_VERSION);
                }
                for (String required : properties.getProperty("requires", "").split(",")) {
                    if (!required.isBlank() && !loadedMods.test(required.trim())) {
                        DesktopDiagnostics.report(Level.INFO, source.modId(), "discovery",
                            "Skipped integration: required mod " + required.trim() + " is absent", null);
                        if (phase == Phase.COMMON) failedCommon.add(source.modId());
                        properties = null;
                        break;
                    }
                }
                if (properties == null) continue;
                if (phase == Phase.CLIENT && failedCommon.contains(source.modId())) {
                    DesktopDiagnostics.report(Level.WARNING, source.modId(), "discovery",
                        "Skipped client provider because common registration failed", null);
                    continue;
                }
                String className = properties.getProperty(phase == Phase.COMMON ? "common" : "client", "").trim();
                if (className.isEmpty()) continue;
                // Resolve only the selected phase. Never even inspect a client class on a server.
                Class<? extends DesktopIntegration> type = Class.forName(className, false, classLoader)
                    .asSubclass(DesktopIntegration.class);
                DesktopRegistrationScope.run(source.modId(), () -> {
                    try {
                        DesktopIntegration provider = type.getDeclaredConstructor().newInstance();
                        provider.register(new DesktopIntegrationContext(source.modId(), phase, loadedMods));
                    } catch (ReflectiveOperationException exception) {
                        throw new IllegalStateException("Could not construct provider " + className, exception);
                    }
                });
                DesktopDiagnostics.report(Level.INFO, source.modId(), "registration",
                    "Registered " + phase + " provider " + className, null);
            } catch (ReflectiveOperationException | IOException | RuntimeException | LinkageError | AssertionError exception) {
                if (phase == Phase.COMMON) failedCommon.add(source.modId());
                DesktopDiagnostics.report(Level.ERROR, source.modId(), "registration",
                    "Failed " + phase + " integration: " + exception, exception);
            }
        }
    }

    private static Properties read(DescriptorInput descriptor) throws IOException {
        Properties result = new Properties();
        try (InputStream input = descriptor.open()) {
            byte[] data = input.readNBytes(16 * 1024 + 1);
            if (data.length > 16 * 1024) throw new IOException("Integration descriptor exceeds 16 KiB");
            result.load(new StringReader(new String(data, StandardCharsets.UTF_8)));
        }
        for (String name : result.stringPropertyNames()) {
            if (!Set.of("modId", "apiVersion", "common", "client", "requires").contains(name)) {
                throw new IOException("Unknown integration descriptor key: " + name);
            }
        }
        return result;
    }
}
