package com.salts_inventory_update.api.client.desktop;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;

/** Client reconstruction for menus that require opening data. Register from a client integration provider. */
public final class DesktopMenuFactories {
    private static final Map<MenuType<?>, Factory> FACTORIES = new LinkedHashMap<>();

    private DesktopMenuFactories() {
    }

    public static synchronized void register(MenuType<?> menuType, Factory factory) {
        Objects.requireNonNull(menuType, "menuType");
        Objects.requireNonNull(factory, "factory");
        if (FACTORIES.putIfAbsent(menuType, factory) != null) {
            throw com.salts_inventory_update.integration.DesktopRegistrationScope.conflict("Menu factory already registered for " + menuType);
        }
        com.salts_inventory_update.integration.DesktopRegistrationScope.onRollback(() -> {
            synchronized (DesktopMenuFactories.class) { FACTORIES.remove(menuType, factory); }
        });
    }

    public static synchronized @Nullable AbstractContainerMenu create(
        MenuType<?> menuType,
        Minecraft minecraft,
        LocalPlayer player,
        int containerId,
        byte[] openingData
    ) {
        Factory factory = FACTORIES.get(menuType);
        return factory == null ? null : factory.create(minecraft, player, containerId, openingData.clone());
    }

    public static synchronized boolean hasFactory(MenuType<?> menuType) {
        return FACTORIES.containsKey(menuType);
    }

    @FunctionalInterface
    public interface Factory {
        AbstractContainerMenu create(Minecraft minecraft, LocalPlayer player, int containerId, byte[] openingData);
    }
}
