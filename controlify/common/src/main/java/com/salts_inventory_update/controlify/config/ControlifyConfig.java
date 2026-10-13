package com.salts_inventory_update.controlify.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** Configuration belonging exclusively to the standalone controller companion. */
public final class ControlifyConfig {
    private final Path path;
    private boolean animationsEnabled = true;

    private ControlifyConfig(Path path) {
        this.path = path;
    }

    public static ControlifyConfig load(Path path) throws IOException {
        ControlifyConfig config = new ControlifyConfig(path);
        if (Files.isRegularFile(path)) {
            Properties properties = new Properties();
            try (InputStream input = Files.newInputStream(path)) {
                properties.load(input);
            }
            // Only an explicit false disables animations; missing/invalid values keep the default.
            config.animationsEnabled = !"false".equalsIgnoreCase(properties.getProperty("animations", "true").trim());
        } else {
            config.save();
        }
        return config;
    }

    public boolean animationsEnabled() {
        return animationsEnabled;
    }

    public void setAnimationsEnabled(boolean enabled) throws IOException {
        animationsEnabled = enabled;
        save();
    }

    public void save() throws IOException {
        Path parent = path.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Properties properties = new Properties();
        properties.setProperty("animations", Boolean.toString(animationsEnabled));
        try (OutputStream output = Files.newOutputStream(path)) {
            properties.store(output, "Salt's Inventory Update - Controlify companion");
        }
    }
}
