package com.salts_inventory_update.api.client.desktop;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;

import com.salts_inventory_update.integration.DesktopRegistrationScope;

import org.jspecify.annotations.Nullable;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;


public final class DesktopClientApi {

    private static DesktopWorkspaceHost workspaceHost;

    /** Installs one client-thread presentation owner. Pass null to release it. */
    public static void setWorkspaceHost(DesktopWorkspaceHost host) { workspaceHost = host; }

    public static DesktopWorkspaceHost workspaceHost() { return workspaceHost; }

    /** Opens the existing player workspace through the same native inventory lifecycle. */
    public static void openPlayerWorkspace() {
        com.salts_inventory_update.client.InventoryDesktopScreen.openOrToggleInventory(net.minecraft.client.Minecraft.getInstance());
    }

    public static void onClientTick(java.util.function.Consumer<net.minecraft.client.Minecraft> action) {
        com.salts_inventory_update.platform.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(action::accept);
    }

    public static java.util.Optional<DesktopWorkspace> currentWorkspace() {
        var screen = com.salts_inventory_update.client.InventoryDesktopScreen.current(net.minecraft.client.Minecraft.getInstance());
        return screen == null ? java.util.Optional.empty() : java.util.Optional.of(screen.workspaceAccess());
    }

    private static final Map<MenuType<?>, DesktopWindowDefinition<?, ?>> DEFINITIONS = new LinkedHashMap<>();
    private static final Map<ResourceLocation, ViewRegistration<?, ?>> VIEWS = new LinkedHashMap<>();
    private static final List<PredicateRegistration> PREDICATES = new ArrayList<>();

    /** Readiness for an optimistic mutation. Pending acknowledgements must settle first. */
    public static boolean canDispatchOptimisticMutationNow() {
        return com.salts_inventory_update.client.DesktopContainerClient.canDispatchOptimisticMutationNow();
    }
    public static net.minecraft.client.gui.screens.Screen createConfigScreen(net.minecraft.client.gui.screens.Screen parent) {
        return com.salts_inventory_update.client.WindowedInventoryClient.createConfigScreen(parent);
    }
    public static void onClientStarted(java.util.function.Consumer<net.minecraft.client.Minecraft> action) {
        com.salts_inventory_update.platform.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents.CLIENT_STARTED.register(action::accept);
    }

    private static final Map<ResourceLocation, DesktopHelpPage> HELP_PAGES = new LinkedHashMap<>();
    public static synchronized void registerHelpPage(ResourceLocation id, DesktopHelpPage page) {
        Objects.requireNonNull(id); Objects.requireNonNull(page);
        if (HELP_PAGES.putIfAbsent(id, page) != null) throw DesktopRegistrationScope.conflict("Help page already registered for " + id);
        DesktopRegistrationScope.onRollback(() -> { synchronized (DesktopClientApi.class) { HELP_PAGES.remove(id, page); } });
    }
    public static synchronized List<DesktopHelpPage> helpPages() { return List.copyOf(HELP_PAGES.values()); }

    private DesktopClientApi() {
    }

    public static synchronized <T extends AbstractContainerMenu, S> void registerWindow(MenuType<T> menuType, DesktopWindowDefinition<T, S> definition) {
        Objects.requireNonNull(menuType, "menuType");
        Objects.requireNonNull(definition, "definition");
        if (DEFINITIONS.containsKey(menuType)) {
            throw DesktopRegistrationScope.conflict("Desktop window definition already registered for " + menuType);
        }
        DesktopWindowDefinition<?, ?> previous = DEFINITIONS.put(menuType, definition);
        DesktopRegistrationScope.onRollback(() -> {
            synchronized (DesktopClientApi.class) {
                if (DEFINITIONS.get(menuType) != definition) return;
                if (previous == null) DEFINITIONS.remove(menuType);
                else DEFINITIONS.put(menuType, previous);
            }
        });
    }

    public static synchronized <T extends AbstractContainerMenu, S> void replaceWindow(MenuType<T> menuType, DesktopWindowDefinition<T, S> definition) {
        Objects.requireNonNull(menuType, "menuType");
        Objects.requireNonNull(definition, "definition");
        DesktopWindowDefinition<?, ?> previous = DEFINITIONS.put(menuType, definition);
        DesktopRegistrationScope.onRollback(() -> {
            synchronized (DesktopClientApi.class) {
                if (DEFINITIONS.get(menuType) != definition) return;
                if (previous == null) DEFINITIONS.remove(menuType);
                else DEFINITIONS.put(menuType, previous);
            }
        });
    }

    public static synchronized void registerMatcher(
        ResourceLocation id,
        int priority,
        Set<MenuType<?>> menuTypes,
        DesktopWindowMatcher matcher,
        DesktopWindowDefinitionFactory factory
    ) {
        Objects.requireNonNull(id, "id");
        menuTypes = Set.copyOf(menuTypes);
        if (menuTypes.isEmpty()) throw new IllegalArgumentException("A matcher must declare its supported menu types");
        Objects.requireNonNull(matcher, "matcher");
        Objects.requireNonNull(factory, "factory");
        if (PREDICATES.stream().anyMatch(existing -> existing.id.equals(id))) {
            throw DesktopRegistrationScope.conflict("Desktop matcher already registered for " + id);
        }
        PredicateRegistration registration = new PredicateRegistration(id, priority, menuTypes, matcher, factory);
        PREDICATES.add(registration);
        DesktopRegistrationScope.onRollback(() -> {
            synchronized (DesktopClientApi.class) { PREDICATES.remove(registration); }
        });
        PREDICATES.sort(Comparator.comparingInt(PredicateRegistration::priority).reversed());
    }

    public static synchronized @Nullable DesktopWindowDefinition<?, ?> findDefinition(DesktopWindowLookupContext context) {
        DesktopWindowDefinition<?, ?> direct = DEFINITIONS.get(context.menuType());
        if (direct != null) {
            return direct;
        }

        for (PredicateRegistration registration : PREDICATES) {
            if (registration.menuTypes.contains(context.menuType()) && registration.matcher.matches(context)) {
                return registration.factory.create(context);
            }
        }

        return null;
    }

    /** Menu capabilities announced to the server. Matchers must cover all declared menu instances. */
    public static synchronized List<String> supportedMenuIds() {
        Set<MenuType<?>> types = new java.util.HashSet<>(DEFINITIONS.keySet());
        for (PredicateRegistration registration : PREDICATES) types.addAll(registration.menuTypes);
        List<String> ids = types.stream().map(BuiltInRegistries.MENU::getKey)
            .filter(Objects::nonNull).map(Object::toString).distinct().sorted().toList();
        com.salts_inventory_update.protocol.DesktopProtocol.requireCount("supported menu ids", ids.size(),
            com.salts_inventory_update.protocol.DesktopProtocol.MAX_SUPPORTED_MENU_IDS);
        return ids;
    }

    /** Registers a client-only view. The model can be any mod-owned object; no menu is created. */
    public static synchronized <M, S> void registerView(ResourceLocation id, Class<M> modelType, DesktopWindowDefinition<M, S> definition) {
        Objects.requireNonNull(id, "id");
        ViewRegistration<M, S> registration = new ViewRegistration<>(Objects.requireNonNull(modelType), Objects.requireNonNull(definition));
        if (VIEWS.putIfAbsent(id, registration) != null) {
            throw DesktopRegistrationScope.conflict("Client view already registered for " + id);
        }
        DesktopRegistrationScope.onRollback(() -> {
            synchronized (DesktopClientApi.class) { VIEWS.remove(id, registration); }
        });
    }

    /** Reusing an instance key focuses that instance. Different keys create independent windows. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static java.util.Optional<DesktopWindowHandle> openView(ResourceLocation id, String instanceKey, Object model,
                                                                  net.minecraft.network.chat.Component title) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(instanceKey, "instanceKey");
        Objects.requireNonNull(title, "title");
        if (instanceKey.isBlank()) throw new IllegalArgumentException("View instanceKey is blank");
        ViewRegistration registration;
        synchronized (DesktopClientApi.class) { registration = VIEWS.get(id); }
        if (registration == null) {
            com.salts_inventory_update.api.integration.DesktopDiagnostics.report(
                com.salts_inventory_update.api.integration.DesktopDiagnostics.Level.WARNING,
                id.toString(), "open-view", "No client view registered", null);
            return java.util.Optional.empty();
        }
        if (model != null && !registration.modelType().isInstance(model)) {
            throw new IllegalArgumentException("View " + id + " requires model " + registration.modelType().getName());
        }
        String key = "view:" + id + ":" + instanceKey;
        return java.util.Optional.ofNullable(com.salts_inventory_update.client.InventoryDesktopScreen.openApiView(
            net.minecraft.client.Minecraft.getInstance(), key, model, title, registration.definition()));
    }

    private record ViewRegistration<M, S>(Class<M> modelType, DesktopWindowDefinition<M, S> definition) { }

    private record PredicateRegistration(
        ResourceLocation id,
        int priority,
        Set<MenuType<?>> menuTypes,
        DesktopWindowMatcher matcher,
        DesktopWindowDefinitionFactory factory
    ) {
    }
}
