package com.salts_inventory_update.compat.ironchests;

import com.salts_inventory_update.api.desktop.DesktopMenuBindings;
import com.salts_inventory_update.api.integration.DesktopEnvironment;
import com.salts_inventory_update.api.server.desktop.DesktopServerApi;
import com.salts_inventory_update.api.server.desktop.DesktopServerWindowHandler;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;

/** Iron Chests: Restocked 5.x. Native menus and native Slot permissions remain authoritative. */
public final class IronChestsIntegration {
    private static boolean serverHook;
    private static final Map<String, Shape> SHAPES = shapes();

    private IronChestsIntegration() { }

    public record Shape(int columns, int rows) {
        public int slots() { return columns * rows; }
    }

    private static Map<String, Shape> shapes() {
        Map<String, Shape> result = new LinkedHashMap<>();
        result.put("dirt_chest", new Shape(1, 1));
        String[] tiers = {"copper", "iron", "gold", "diamond", "crystal", "obsidian", "netherite"};
        int[] columns = {9, 9, 11, 12, 12, 12, 13};
        int[] rows = {4, 6, 6, 6, 6, 6, 7};
        for (int i = 0; i < tiers.length; i++) {
            result.put(tiers[i] + "_chest", new Shape(columns[i], rows[i]));
            result.put(tiers[i] + "_barrel", new Shape(columns[i], rows[i]));
        }
        return java.util.Collections.unmodifiableMap(result);
    }

    static void forEachMenu(BiConsumer<MenuType<AbstractContainerMenu>, Shape> action) {
        SHAPES.forEach((name, shape) -> {
            ResourceLocation id = new ResourceLocation("ironchests", name);
            if (!BuiltInRegistries.MENU.containsKey(id)) return;
            @SuppressWarnings("unchecked")
            MenuType<AbstractContainerMenu> type = (MenuType<AbstractContainerMenu>) BuiltInRegistries.MENU.get(id);
            action.accept(type, shape);
        });
    }

    public static synchronized void initializeCommon() {
        if (serverHook || !DesktopEnvironment.isModLoaded("ironchests")) return;
        serverHook = true;
        // Forge's deferred menu registry is ready by this event. This also supports dedicated servers.
        DesktopEnvironment.onServerStarting(server -> DesktopEnvironment.registerIntegration("ironchests", () ->
            forEachMenu((type, shape) -> {
                registerStorage(type, shape);
                DesktopServerWindowHandler<?, ?> existing = DesktopServerApi.findWindowHandler(type);
                if (existing instanceof StorageHandler) return;
                DesktopServerApi.registerWindow(type, new StorageHandler(shape));
            })));
    }

    static void registerStorage(MenuType<?> type, Shape shape) {
        DesktopMenuBindings.registerStorage(type, (menu, slot) ->
            slot.index >= 0 && slot.index < shape.slots() && !(slot.container instanceof Inventory));
    }

    private record StorageHandler(Shape shape) implements DesktopServerWindowHandler<AbstractContainerMenu, Void> {
        @Override public boolean supports(ServerPlayer player, AbstractContainerMenu menu) {
            // Fail back to the native screen if an upstream update changes this inventory contract.
            if (menu.slots.size() != shape.slots() + Inventory.INVENTORY_SIZE) return false;
            for (int i = 0; i < menu.slots.size(); i++) {
                if ((menu.getSlot(i).container == player.getInventory()) != (i >= shape.slots())) return false;
            }
            return true;
        }
    }
}
