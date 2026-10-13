package com.salts_inventory_update.compat.ae2.qa;

import java.nio.file.Files;
import java.nio.file.Path;
import org.slf4j.LoggerFactory;

/** Propagates deferred native plot setup failures to the gameplay harness. */
final class NativeAtlasSetup {
    private NativeAtlasSetup() { }

    static void run(Path output, Runnable action) {
        try { action.run(); }
        catch (RuntimeException failure) {
            LoggerFactory.getLogger(NativeAtlasSetup.class).error("ATLAS SETUP FAILED", failure);
            try { Files.writeString(output.resolve("failed.txt"), "Atlas setup: " + failure); }
            catch (java.io.IOException writeError) {
                failure.addSuppressed(writeError);
                throw failure;
            }
        }
    }
}
