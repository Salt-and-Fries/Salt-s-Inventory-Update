package com.salts_inventory_update.api.client.desktop;

import com.salts_inventory_update.api.integration.DesktopDiagnostics;
import com.salts_inventory_update.integration.DesktopRegistrationScope;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Public client HUD interoperability. Core retains all slot identities, menus and item actions. */
public final class DesktopHudApi {
    private static final Map<String, Registration> PROVIDERS = new LinkedHashMap<>();
    private DesktopHudApi() { }

    /**
     * Register during client initialization. Lower priorities run first; owner breaks ties lexically.
     * Owners are unique namespaced IDs. Close the returned registration to remove only this provider.
     * Failed integration initialization rolls registration back through the normal integration scope.
     */
    public static synchronized RegistrationHandle register(String owner, int priority, DesktopHudLayoutProvider provider) {
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(provider, "provider");
        if (!owner.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")) throw new IllegalArgumentException("HUD owner must be namespaced");
        Registration registration = new Registration(owner, priority, provider);
        if (PROVIDERS.putIfAbsent(owner, registration) != null) {
            throw DesktopRegistrationScope.conflict("HUD provider already registered for " + owner);
        }
        RegistrationHandle handle = () -> { synchronized (DesktopHudApi.class) { PROVIDERS.remove(owner, registration); } };
        DesktopRegistrationScope.onRollback(handle::close);
        return handle;
    }

    /**
     * Resolve one immutable operation snapshot. A failed/null/invalid provider preserves the preceding
     * layout and does not prevent other providers running. Report once per consecutive failure streak;
     * the provider can recover on its next call. Do not cache across rendering/input operations.
     */
    public static DesktopHudLayout resolve(DesktopHudContext context, DesktopHudLayout base) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(base, "base");
        ArrayList<Registration> ordered;
        synchronized (DesktopHudApi.class) { ordered = new ArrayList<>(PROVIDERS.values()); }
        ordered.sort(Comparator.comparingInt((Registration r) -> r.priority).thenComparing(r -> r.owner));
        DesktopHudLayout current = base;
        for (Registration registration : ordered) {
            try {
                current = Objects.requireNonNull(registration.provider.adapt(context, current), "HUD provider returned null");
                registration.failed = false;
            } catch (RuntimeException | LinkageError failure) {
                if (!registration.failed) {
                    registration.failed = true;
                    DesktopDiagnostics.report(DesktopDiagnostics.Level.ERROR, registration.owner,
                        "hud-layout", "HUD provider failed; keeping preceding layout", failure);
                }
            }
        }
        return current;
    }

    @FunctionalInterface
    public interface RegistrationHandle extends AutoCloseable {
        @Override void close();
    }

    private static final class Registration {
        final String owner;
        final int priority;
        final DesktopHudLayoutProvider provider;
        boolean failed;
        Registration(String owner, int priority, DesktopHudLayoutProvider provider) {
            this.owner = owner; this.priority = priority; this.provider = provider;
        }
    }
}
