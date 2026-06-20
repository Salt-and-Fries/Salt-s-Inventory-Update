package com.salts_inventory_update.api.desktop;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;

import com.salts_inventory_update.api.client.desktop.DesktopWindowDefinition;
import com.salts_inventory_update.api.client.desktop.DesktopWindowDefinitionFactory;
import com.salts_inventory_update.api.client.desktop.DesktopWindowLookupContext;
import com.salts_inventory_update.api.client.desktop.DesktopWindowMatcher;
import com.salts_inventory_update.api.server.desktop.DesktopServerApi;
import com.salts_inventory_update.api.server.desktop.DesktopServerPayloadHandler;

public final class SaltsInventoryDesktopApi {
    public static final int CUSTOM_PAYLOAD_MAX_BYTES = 32 * 1024;

    private static final Map<MenuType<?>, DesktopWindowDefinition<?, ?>> DEFINITIONS = new LinkedHashMap<>();
    private static final List<PredicateRegistration> PREDICATES = new ArrayList<>();

    private SaltsInventoryDesktopApi() {
    }

    public static synchronized <T extends AbstractContainerMenu, S> void register(MenuType<T> menuType, DesktopWindowDefinition<T, S> definition) {
        Objects.requireNonNull(menuType, "menuType");
        Objects.requireNonNull(definition, "definition");
        if (DEFINITIONS.containsKey(menuType)) {
            throw new IllegalStateException("Desktop window definition already registered for " + menuType);
        }
        DEFINITIONS.put(menuType, definition);
    }

    public static synchronized <T extends AbstractContainerMenu, S> void replace(MenuType<T> menuType, DesktopWindowDefinition<T, S> definition) {
        Objects.requireNonNull(menuType, "menuType");
        Objects.requireNonNull(definition, "definition");
        DEFINITIONS.put(menuType, definition);
    }

    public static synchronized void registerPredicate(
        Identifier id,
        int priority,
        DesktopWindowMatcher matcher,
        DesktopWindowDefinitionFactory factory
    ) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(matcher, "matcher");
        Objects.requireNonNull(factory, "factory");
        PREDICATES.removeIf(existing -> existing.id.equals(id));
        PREDICATES.add(new PredicateRegistration(id, priority, matcher, factory));
        PREDICATES.sort(Comparator.comparingInt(PredicateRegistration::priority).reversed());
    }

    public static synchronized <T extends AbstractContainerMenu> void registerServerPayload(
        MenuType<T> menuType,
        Identifier channel,
        DesktopServerPayloadHandler<T> handler
    ) {
        Objects.requireNonNull(menuType, "menuType");
        Objects.requireNonNull(channel, "channel");
        Objects.requireNonNull(handler, "handler");
        DesktopServerApi.registerPayload(menuType, channel, handler);
    }

    public static synchronized @Nullable DesktopWindowDefinition<?, ?> findDefinition(DesktopWindowLookupContext context) {
        DesktopWindowDefinition<?, ?> direct = DEFINITIONS.get(context.menuType());
        if (direct != null) {
            return direct;
        }

        for (PredicateRegistration registration : PREDICATES) {
            if (registration.matcher.matches(context)) {
                return registration.factory.create(context);
            }
        }

        return null;
    }

    private record PredicateRegistration(
        Identifier id,
        int priority,
        DesktopWindowMatcher matcher,
        DesktopWindowDefinitionFactory factory
    ) {
    }
}
