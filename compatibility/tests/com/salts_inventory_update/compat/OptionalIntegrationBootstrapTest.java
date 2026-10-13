package com.salts_inventory_update.compat;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Real extension bootstrap and core transaction API, with optional providers selected explicitly. */
final class OptionalIntegrationBootstrapTest {
    public static final class PresentProvider {
        static int registrations;
        public static void initialize() { registrations++; }
    }
    public static final class FailedProvider {
        public static void initialize() { throw new LinkageError("unsupported optional target"); }
    }

    @Test void absentTargetNeverResolvesItsMissingClasses() {
        assertDoesNotThrow(() -> CompatibilityBootstrap.initialize("absent", "missing.OptionalProvider", "initialize", id -> false));
    }

    @Test void presentTargetInitializesNormally() {
        int before = PresentProvider.registrations;
        CompatibilityBootstrap.initialize("present", PresentProvider.class.getName(), "initialize", id -> true);
        assertEquals(before + 1, PresentProvider.registrations);
    }

    @Test void failureOfOneTargetDoesNotDisableAnUnrelatedTarget() {
        assertDoesNotThrow(() -> CompatibilityBootstrap.initialize("failed", FailedProvider.class.getName(), "initialize", id -> true));
        assertDoesNotThrow(() -> CompatibilityBootstrap.initialize("unsupported", "missing.UnsupportedProvider", "initialize", id -> true));
        int before = PresentProvider.registrations;
        CompatibilityBootstrap.initialize("unrelated", PresentProvider.class.getName(), "initialize", id -> true);
        assertEquals(before + 1, PresentProvider.registrations);
    }
}
