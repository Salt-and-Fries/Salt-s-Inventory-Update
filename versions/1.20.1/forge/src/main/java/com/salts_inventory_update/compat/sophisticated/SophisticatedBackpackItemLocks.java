package com.salts_inventory_update.compat.sophisticated;

import java.util.UUID;

import com.salts_inventory_update.internal.desktop.DesktopItemSourceLocks;
import com.salts_inventory_update.compat.sophisticated.mixin.BackpackContextItemAccessor;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContainer;
import net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContext;
import net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackSettingsContainerMenu;
import net.p3pp3rf1y.sophisticatedbackpacks.util.PlayerInventoryHandler;
import net.p3pp3rf1y.sophisticatedbackpacks.util.PlayerInventoryProvider;

/** Locks the exact vanilla inventory, offhand, or worn Backpack while its detached session is live. */
final class SophisticatedBackpackItemLocks {
    private SophisticatedBackpackItemLocks() {
    }

    static void register(MenuType<?> backpack, MenuType<?> settings) {
        DesktopItemSourceLocks.register(backpack, SophisticatedBackpackItemLocks::resolve);
        DesktopItemSourceLocks.register(settings, SophisticatedBackpackItemLocks::resolve);
    }

    private static DesktopItemSourceLocks.Source resolve(Player player, AbstractContainerMenu menu) {
        BackpackContext context;
        if (menu instanceof BackpackContainer backpackContainer) {
            context = backpackContainer.getBackpackContext();
        } else if (menu instanceof BackpackSettingsContainerMenu settingsContainer) {
            context = settingsContainer.getBackpackContext();
        } else {
            return null;
        }

        if (context.getType() != BackpackContext.ContextType.ITEM_BACKPACK
            || !(context instanceof BackpackContextItemAccessor itemContext)) {
            return null;
        }

        String handlerName = itemContext.saltsInventoryUpdate$getHandlerName();
        int handlerSlot = context.getBackpackSlotIndex();
        int inventorySlot = switch (handlerName) {
            case PlayerInventoryProvider.MAIN_INVENTORY -> handlerSlot;
            case PlayerInventoryProvider.OFFHAND_INVENTORY -> handlerSlot == 0 ? Inventory.SLOT_OFFHAND : -1;
            case PlayerInventoryProvider.ARMOR_INVENTORY -> handlerSlot == 0
                ? EquipmentSlot.CHEST.getIndex(Inventory.INVENTORY_SIZE)
                : -1;
            default -> -1;
        };
        if (inventorySlot < 0 || inventorySlot >= player.getInventory().getContainerSize()) {
            return null;
        }
        PlayerInventoryHandler handler = PlayerInventoryProvider.get()
            .getPlayerInventoryHandler(handlerName)
            .orElse(null);
        ItemStack source = player.getInventory().getItem(inventorySlot);
        ItemStack contextSource = handler == null
            ? ItemStack.EMPTY
            : handler.getStackInSlot(player, itemContext.saltsInventoryUpdate$getResourceLocation(), handlerSlot);
        if (source.isEmpty() || contextSource.isEmpty() || source != contextSource) {
            return null;
        }

        UUID contentsUuid = context.getBackpackWrapper(player).getContentsUuid().orElse(null);
        if (contentsUuid == null) {
            return null;
        }
        return new DesktopItemSourceLocks.Source(
            inventorySlot,
            contentsUuid.toString(),
            source,
            stack -> matchesContentsUuid(stack, contentsUuid)
        );
    }

    private static boolean matchesContentsUuid(ItemStack stack, UUID contentsUuid) {
        return !stack.isEmpty()
            && new BackpackWrapper(stack).getContentsUuid().filter(contentsUuid::equals).isPresent();
    }
}
