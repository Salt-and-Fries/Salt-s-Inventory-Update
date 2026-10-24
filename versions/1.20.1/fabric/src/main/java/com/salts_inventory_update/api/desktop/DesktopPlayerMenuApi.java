package com.salts_inventory_update.api.desktop;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Composable rules for native player-menu equipment supplied by another mod. Register on both sides. */
public final class DesktopPlayerMenuApi {
    private static final Map<ResourceLocation, Equipment> EQUIPMENT = new LinkedHashMap<>();
    private static final Map<InventoryMenu, List<TopologySlot>> REBUILDS = java.util.Collections.synchronizedMap(new java.util.WeakHashMap<>());
    private DesktopPlayerMenuApi() { }

    private static List<TopologySlot> topology(InventoryMenu menu) {
        List<TopologySlot> result = new ArrayList<>();
        for (int i = 0; i < menu.slots.size(); i++) {
            Slot slot = menu.slots.get(i);
            String key = slotKey(slot);
            result.add(new TopologySlot(key.isEmpty() ? "native/" + i : key, slot.container, slot.getContainerSlot()));
        }
        return List.copyOf(result);
    }

    public static synchronized void registerEquipment(ResourceLocation id, Equipment equipment) {
        Objects.requireNonNull(id); Objects.requireNonNull(equipment);
        if (EQUIPMENT.putIfAbsent(id, equipment) != null)
            throw com.salts_inventory_update.integration.DesktopRegistrationScope.conflict("Player equipment already registered: " + id);
        com.salts_inventory_update.integration.DesktopRegistrationScope.onRollback(() -> EQUIPMENT.remove(id, equipment));
    }

    private static synchronized List<Equipment> providers() { return List.copyOf(EQUIPMENT.values()); }
    public static boolean hasDynamicEquipment() { return !providers().isEmpty(); }

    /** Native equipment validates placement/removal itself; hidden hover state is not server authority. */
    public static boolean owns(Slot slot) { return providers().stream().anyMatch(p -> p.owns(slot)); }

    public static boolean serverSlotActive(Slot slot) { return owns(slot) || slot.isActive(); }

    /** Stable within a connection across native menu rebuilds; provider IDs prevent collisions. */
    public static synchronized String slotKey(Slot slot) {
        for (var entry : EQUIPMENT.entrySet()) if (entry.getValue().owns(slot))
            return entry.getKey() + "/" + entry.getValue().slotKey(slot);
        if (com.salts_inventory_update.inventory.InventoryExpansion.isExtraSlot(slot))
            return "salts_inventory_update:expanded/" + slot.getContainerSlot();
        return "";
    }

    public static List<Slot> quickMoveTargets(Player player, ItemStack stack) {
        List<Slot> result = new ArrayList<>();
        for (Equipment provider : providers()) for (Slot slot : provider.quickMoveTargets(player, stack)) {
            if (slot != null && provider.owns(slot) && player.inventoryMenu.slots.contains(slot)
                    && slot.mayPlace(stack) && !result.contains(slot)) result.add(slot);
        }
        return List.copyOf(result);
    }

    /**
     * Bracket a native equipment-slot rebuild. Core detaches only its own expanded slots,
     * then appends them after the native provider has rebuilt its real slots and tracking lists.
     * Call in a finally block when the provider can throw. Never call from a render pass.
     */
    public static void beforeSlotRebuild(InventoryMenu menu) {
        REBUILDS.put(menu, topology(menu));
        com.salts_inventory_update.inventory.InventoryExpansion.detachMenuSlots(menu);
    }
    public static void afterSlotRebuild(InventoryMenu menu, Player player) {
        com.salts_inventory_update.inventory.InventoryExpansion.appendMissingMenuSlots(menu, player);
        List<TopologySlot> previous = REBUILDS.remove(menu);
        if (previous != null && previous.equals(topology(menu))) return;
        if (player.level().isClientSide && player.inventoryMenu == menu)
            com.salts_inventory_update.client.InventoryDesktopScreen.playerMenuRebuilt();
        else if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer && player.inventoryMenu == menu)
            com.salts_inventory_update.server.DesktopContainerSessions.playerMenuRebuilt(serverPlayer);
    }

    private record TopologySlot(String key, Object inventory, int index) {
        @Override public boolean equals(Object other) {
            return other instanceof TopologySlot slot && key.equals(slot.key) && inventory == slot.inventory && index == slot.index;
        }
        @Override public int hashCode() { return 31 * key.hashCode() + System.identityHashCode(inventory) + index; }
    }

    public interface Equipment {
        boolean owns(Slot slot);
        String slotKey(Slot slot);
        /** Must apply native quick-move predicates as well as ordinary insertion predicates. */
        List<Slot> quickMoveTargets(Player player, ItemStack stack);
    }
}
