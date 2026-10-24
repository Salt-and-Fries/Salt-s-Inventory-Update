package com.salts_inventory_update.compat.trinkets;

import java.util.List;
import com.salts_inventory_update.api.desktop.DesktopPlayerMenuApi;
import com.salts_inventory_update.api.client.desktop.*;
import dev.emi.trinkets.TrinketSlot;
import dev.emi.trinkets.api.*;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.Slot;

/** All native Trinkets dependencies stay in this optional consumer of Core's public API. */
public final class TrinketsIntegration {
    private TrinketsIntegration() { }
    public static boolean supported() {
        return FabricLoader.getInstance().getModContainer("trinkets")
            .map(m -> m.getMetadata().getVersion().getFriendlyString().equals("3.7.2")).orElse(false);
    }
    public static void initializeCommon() {
        if (!supported()) return;
        DesktopPlayerMenuApi.registerEquipment(new ResourceLocation("trinkets", "equipment"), new DesktopPlayerMenuApi.Equipment() {
            @Override public boolean owns(Slot slot) { return slot instanceof TrinketSlot && slot.container instanceof TrinketInventory; }
            @Override public String slotKey(Slot slot) {
                var type = ((TrinketSlot) slot).getType();
                return type.getGroup() + "/" + type.getName() + "/" + slot.getContainerSlot();
            }
            @Override public List<Slot> quickMoveTargets(net.minecraft.world.entity.player.Player player, net.minecraft.world.item.ItemStack stack) {
                return player.inventoryMenu.slots.stream().filter(this::owns).filter(slot -> {
                    var type = ((TrinketSlot) slot).getType();
                    return slot.mayPlace(stack) && TrinketsApi.evaluatePredicateSet(type.getQuickMovePredicates(), stack,
                        new SlotReference((TrinketInventory) slot.container, slot.getContainerSlot()), player);
                }).toList();
            }
        });
    }
    public static void initializeClient() {
        if (!supported()) return;
        DesktopClientApi.registerBuiltinAttachment(new ResourceLocation("trinkets", "character"),
            DesktopWorkspaceWindow.Kind.CHARACTER, TrinketsCharacterAttachment::new);
    }
}
