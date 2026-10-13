package com.salts_inventory_update.client;

import com.salts_inventory_update.api.client.recipebrowser.*;
import com.salts_inventory_update.api.integration.DesktopEnvironment;
import java.lang.reflect.Proxy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class DesktopRecipeProviderTest {
    @AfterEach void reset() { RecipeBrowserBridge.clear(); }
    private RecipeBrowserAccess provider(RecipeBrowserSource source, boolean available) {
        return (RecipeBrowserAccess) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{RecipeBrowserAccess.class}, (proxy, method, args) -> {
            return switch (method.getName()) {
                case "source" -> source;
                case "isAvailable" -> available;
                default -> throw new UnsupportedOperationException(method.getName());
            };
        });
    }

    @Test void standaloneCoreHasANoopBrowserWithoutAnyThirdPartyMod() {
        RecipeBrowserBridge.clear();
        assertFalse(RecipeBrowserBridge.access().isAvailable());
        assertTrue(RecipeBrowserBridge.access().tabs().isEmpty());
    }

    @Test void externalProvidersUsePriorityAndRemovalRestoresTheNextAvailableBrowser() {
        var low = new RecipeBrowserSource("external:low", "Low", 5);
        var high = new RecipeBrowserSource("external:high", "High", 50);
        var disabled = new RecipeBrowserSource("external:disabled", "Disabled", 100);
        var first = provider(low, true);
        var second = provider(high, true);
        RecipeBrowserBridge.install(low, first);
        RecipeBrowserBridge.install(high, second);
        RecipeBrowserBridge.install(disabled, provider(disabled, false));
        assertSame(second, RecipeBrowserBridge.access());
        RecipeBrowserBridge.clear(high, provider(high, true));
        assertSame(second, RecipeBrowserBridge.access(), "A stale provider cannot unregister its replacement");
        RecipeBrowserBridge.clear(high, second);
        assertSame(first, RecipeBrowserBridge.access());
    }

    @Test void failedIntegrationRollsBackItsBrowserAndPreservesAnotherProvider() {
        var existing = new RecipeBrowserSource("external:stable", "Stable", 10);
        var replacement = new RecipeBrowserSource("external:failed", "Failed", 20);
        var stable = provider(existing, true);
        RecipeBrowserBridge.install(existing, stable);
        assertThrows(IllegalStateException.class, () -> DesktopEnvironment.registerIntegration("external:failed", () -> {
            RecipeBrowserBridge.install(replacement, provider(replacement, true));
            throw new IllegalStateException("registration failed");
        }));
        assertSame(stable, RecipeBrowserBridge.access());
    }

    @Test void failingAvailabilityCannotHideAHealthyProvider() {
        var healthySource = new RecipeBrowserSource("external:healthy", "Healthy", 5);
        var brokenSource = new RecipeBrowserSource("external:broken", "Broken", 100);
        var healthy = provider(healthySource, true);
        var broken = (RecipeBrowserAccess) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{RecipeBrowserAccess.class}, (proxy, method, args) -> {
            if (method.getName().equals("source")) return brokenSource;
            throw new LinkageError("optional dependency changed");
        });
        RecipeBrowserBridge.install(healthySource, healthy);
        assertDoesNotThrow(() -> RecipeBrowserBridge.install(brokenSource, broken));
        assertSame(healthy, RecipeBrowserBridge.access());
    }
}
