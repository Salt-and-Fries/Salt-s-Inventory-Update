package com.salts_inventory_update.inventory;

import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import com.salts_inventory_update.mixin.accessor.AbstractContainerMenuAccessor;
import com.salts_inventory_update.network.DesktopPackets.InventoryExpansionSyncPayload;
import com.salts_inventory_update.SaltsInventoryRuntime;

public final class InventoryExpansion {
    public static final int VANILLA_MAIN_START = 9;
    public static final int VANILLA_MAIN_END = 36;
    public static final int HOTBAR_START = 0;
    public static final int HOTBAR_END = 9;
    public static final int VANILLA_PLAYER_MENU_SLOTS = 46;
    public static final int HARD_MAX_EXTRA_SLOTS = 4096;

    private static final String EXTRA_SLOT_COUNT_KEY = "salts_inventory_update_extra_slot_count";
    private static final String EXTRA_INVENTORY_KEY = "salts_inventory_update_extra_inventory";
    private static final int EXTRA_MENU_SLOT_X = 8;
    private static final int EXTRA_MENU_SLOT_Y = 142;

    private InventoryExpansion() {
    }

    public static InventoryExpansionAccess access(net.minecraft.world.entity.player.Player player) {
        return (InventoryExpansionAccess) player;
    }

    public static int costForNextSlot(net.minecraft.world.entity.player.Player player) {
        return costForNextSlot(access(player).salts_inventory_update$getExtraSlotCount());
    }

    public static int costForNextSlot(int currentExtraSlotCount) {
        int clampedCount = clampSlotCount(currentExtraSlotCount);
        return clampedCount >= HARD_MAX_EXTRA_SLOTS ? Integer.MAX_VALUE : clampedCount + 1;
    }

    public static int clampSlotCount(int slotCount) {
        return Math.max(0, Math.min(slotCount, HARD_MAX_EXTRA_SLOTS));
    }

    public static boolean isExtraSlot(Slot slot) {
        return SaltsInventoryRuntime.isEnabled() && slot instanceof InventoryExpansionSlot;
    }

    public static boolean isMainInventorySlot(net.minecraft.world.entity.player.Player player, Slot slot) {
        return slot.container == player.getInventory()
            && slot.getContainerSlot() >= VANILLA_MAIN_START
            && slot.getContainerSlot() < VANILLA_MAIN_END
            || isExtraSlot(slot);
    }

    public static int storageOrder(Slot slot) {
        if (isExtraSlot(slot)) {
            return VANILLA_MAIN_END + slot.getContainerSlot();
        }
        return slot.getContainerSlot();
    }

    public static void appendMissingMenuSlots(InventoryMenu menu, net.minecraft.world.entity.player.Player player) {
        if (!SaltsInventoryRuntime.isEnabled()) {
            return;
        }

        PlayerExtraInventory extraInventory = access(player).salts_inventory_update$getExtraInventory();
        int existing = 0;
        for (Slot slot : menu.slots) {
            if (slot instanceof InventoryExpansionSlot && slot.container == extraInventory) {
                existing++;
            }
        }

        for (int i = existing; i < extraInventory.getContainerSize(); i++) {
            int x = EXTRA_MENU_SLOT_X + i % 9 * 18;
            int y = EXTRA_MENU_SLOT_Y + i / 9 * 18;
            ((AbstractContainerMenuAccessor) menu).salts_inventory_update$invokeAddSlot(new InventoryExpansionSlot(extraInventory, i, x, y));
        }
    }

    public static boolean insertIntoExtra(net.minecraft.world.entity.player.Player player, ItemStack stack) {
        if (!SaltsInventoryRuntime.isEnabled()) {
            return false;
        }

        return access(player).salts_inventory_update$getExtraInventory().insert(stack);
    }

    public static void ensurePlayerMenuCanReadSlotCount(net.minecraft.world.entity.player.Player player, int packetSlotCount) {
        if (!SaltsInventoryRuntime.isEnabled()) {
            return;
        }

        if (packetSlotCount <= player.inventoryMenu.slots.size()) {
            return;
        }

        int expectedExtraSlots = clampSlotCount(packetSlotCount - VANILLA_PLAYER_MENU_SLOTS);
        InventoryExpansionAccess access = access(player);
        if (expectedExtraSlots > access.salts_inventory_update$getExtraSlotCount()) {
            access.salts_inventory_update$setExtraSlotCount(expectedExtraSlots);
        } else {
            appendMissingMenuSlots(player.inventoryMenu, player);
        }
    }

    public static void save(net.minecraft.world.entity.player.Player player, ValueOutput output) {
        InventoryExpansionAccess access = access(player);
        int extraSlotCount = clampSlotCount(access.salts_inventory_update$getExtraSlotCount());
        output.putInt(EXTRA_SLOT_COUNT_KEY, extraSlotCount);

        ValueOutput.TypedOutputList<SavedExtraSlot> outputList = output.list(EXTRA_INVENTORY_KEY, SavedExtraSlot.CODEC);
        PlayerExtraInventory extraInventory = access.salts_inventory_update$getExtraInventory();
        for (int slot = 0; slot < extraInventory.getContainerSize(); slot++) {
            ItemStack stack = extraInventory.getItem(slot);
            if (!stack.isEmpty()) {
                outputList.add(new SavedExtraSlot(slot, stack.copy()));
            }
        }
    }

    public static void load(net.minecraft.world.entity.player.Player player, ValueInput input) {
        int extraSlotCount = clampSlotCount(input.getIntOr(EXTRA_SLOT_COUNT_KEY, 0));
        NonNullList<ItemStack> stacks = NonNullList.withSize(extraSlotCount, ItemStack.EMPTY);
        for (SavedExtraSlot savedSlot : input.listOrEmpty(EXTRA_INVENTORY_KEY, SavedExtraSlot.CODEC)) {
            if (savedSlot.slot() >= 0 && savedSlot.slot() < extraSlotCount && !savedSlot.stack().isEmpty()) {
                stacks.set(savedSlot.slot(), savedSlot.stack().copy());
            }
        }

        InventoryExpansionAccess access = access(player);
        access.salts_inventory_update$setExtraSlotCount(extraSlotCount);
        access.salts_inventory_update$getExtraInventory().loadSnapshot(extraSlotCount, stacks);
        appendMissingMenuSlots(player.inventoryMenu, player);
    }

    public static void copyFrom(
        net.minecraft.world.entity.player.Player target,
        net.minecraft.world.entity.player.Player source,
        boolean copyContents
    ) {
        if (!SaltsInventoryRuntime.isEnabled()) {
            return;
        }

        InventoryExpansionAccess sourceAccess = access(source);
        InventoryExpansionAccess targetAccess = access(target);
        targetAccess.salts_inventory_update$setExtraSlotCount(sourceAccess.salts_inventory_update$getExtraSlotCount());
        targetAccess.salts_inventory_update$getExtraInventory().copyFrom(sourceAccess.salts_inventory_update$getExtraInventory(), copyContents);
        appendMissingMenuSlots(target.inventoryMenu, target);
    }

    public static void syncToClient(ServerPlayer player) {
        if (SaltsInventoryRuntime.isEnabled() && ServerPlayNetworking.canSend(player, InventoryExpansionSyncPayload.TYPE)) {
            InventoryExpansionAccess access = access(player);
            ServerPlayNetworking.send(
                player,
                new InventoryExpansionSyncPayload(
                    access.salts_inventory_update$getExtraSlotCount(),
                    access.salts_inventory_update$getExtraInventory().snapshot()
                )
            );
        }
    }

    public static boolean tryPurchase(ServerPlayer player) {
        if (!SaltsInventoryRuntime.isEnabled()) {
            return false;
        }

        InventoryExpansionAccess access = access(player);
        int currentCount = access.salts_inventory_update$getExtraSlotCount();
        int cost = costForNextSlot(currentCount);
        if (cost == Integer.MAX_VALUE || player.experienceLevel < cost) {
            syncToClient(player);
            return false;
        }

        player.giveExperienceLevels(-cost);
        access.salts_inventory_update$setExtraSlotCount(currentCount + 1);
        appendMissingMenuSlots(player.inventoryMenu, player);
        syncToClient(player);
        player.inventoryMenu.broadcastFullState();
        return true;
    }

    public record SavedExtraSlot(int slot, ItemStack stack) {
        public static final Codec<SavedExtraSlot> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("Slot").forGetter(SavedExtraSlot::slot),
            ItemStack.CODEC.fieldOf("Item").forGetter(SavedExtraSlot::stack)
        ).apply(instance, SavedExtraSlot::new));
    }
}
