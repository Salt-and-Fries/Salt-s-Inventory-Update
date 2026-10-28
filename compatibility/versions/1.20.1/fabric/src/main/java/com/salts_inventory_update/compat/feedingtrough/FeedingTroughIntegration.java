package com.salts_inventory_update.compat.feedingtrough;

import com.salts_inventory_update.api.desktop.DesktopMenuBindings;
import com.salts_inventory_update.api.integration.DesktopEnvironment;
import com.salts_inventory_update.api.server.desktop.DesktopServerApi;
import com.salts_inventory_update.api.server.desktop.DesktopServerWindowHandler;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;

/** Leaves animal feeding, stored XP and all inventory permissions to the native mod. */
public final class FeedingTroughIntegration {
    static final String MOD_ID = "animal_feeding_trough";
    private static boolean serverHook;
    private FeedingTroughIntegration() { }

    @SuppressWarnings("unchecked")
    static MenuType<AbstractContainerMenu> menuType() {
        var id = new ResourceLocation(MOD_ID, "feeding_trough");
        return BuiltInRegistries.MENU.containsKey(id)
            ? (MenuType<AbstractContainerMenu>) BuiltInRegistries.MENU.get(id) : null;
    }

    public static synchronized void initializeCommon() {
        if (serverHook || !DesktopEnvironment.isModLoaded(MOD_ID)) return;
        serverHook = true;
        DesktopEnvironment.onServerStarting(server -> DesktopEnvironment.registerIntegration(MOD_ID, () -> {
            var type = menuType();
            if (type == null) return;
            registerStorage(type);
            DesktopServerWindowHandler<?, ?> existing = DesktopServerApi.findWindowHandler(type);
            if (!(existing instanceof TroughHandler)) DesktopServerApi.registerWindow(type, new TroughHandler());
        }));
    }

    static void registerStorage(MenuType<?> type) {
        DesktopMenuBindings.registerStorage(type, (menu, slot) ->
            slot.index == 0 && !(slot.container instanceof Inventory));
    }

    private static final class TroughHandler implements DesktopServerWindowHandler<AbstractContainerMenu, Void> {
        @Override public boolean supports(ServerPlayer player, AbstractContainerMenu menu) {
            // An unknown upstream layout keeps its native screen instead of being partially captured.
            if (menu.slots.size() != 1 + Inventory.INVENTORY_SIZE) return false;
            for (int i = 0; i < menu.slots.size(); i++) {
                if ((menu.getSlot(i).container == player.getInventory()) != (i > 0)) return false;
            }
            return true;
        }
    }
}
