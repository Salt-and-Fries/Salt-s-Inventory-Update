package com.salts_inventory_update.compat.sophisticated;

import java.util.List;

import com.salts_inventory_update.client.DesktopContainerClient;
import com.salts_inventory_update.compat.sophisticated.mixin.SlotSuppliedHandlerAccessor;
import com.salts_inventory_update.internal.desktop.DesktopMenuSlots;
import com.salts_inventory_update.network.DesktopMenuOpenDataPayload;
import com.salts_inventory_update.platform.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.SlotItemHandler;
import net.p3pp3rf1y.sophisticatedcore.common.gui.SettingsContainerMenu;
import net.p3pp3rf1y.sophisticatedcore.common.gui.SlotSuppliedHandler;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase.StorageUpgradeSlot;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageInventorySlot;

/** Shared Core adapters installed only after at least one supported Sophisticated mod is present. */
public final class SophisticatedCoreCompat {
    private static final DesktopMenuSlots.GesturePolicy MAIN_MENU_GESTURE_POLICY = new DesktopMenuSlots.GesturePolicy() {
        @Override
        public boolean canQuickCraft(AbstractContainerMenu menu, net.minecraft.world.inventory.Slot slot, ItemStack carried) {
            return StorageContainerMenuBase.canItemQuickReplace(slot, carried);
        }

        @Override
        public int quickCraftPlaceCount(
            AbstractContainerMenu menu,
            net.minecraft.world.inventory.Slot slot,
            int targetCount,
            int quickCraftType,
            ItemStack carried
        ) {
            return StorageContainerMenuBase.getQuickCraftPlaceCount(slot, targetCount, quickCraftType, carried);
        }

        @Override
        public int quickCraftMaxStackSize(AbstractContainerMenu menu, net.minecraft.world.inventory.Slot slot, ItemStack carried) {
            return slot instanceof StorageInventorySlot
                ? slot.getMaxStackSize(carried)
                : Math.min(carried.getMaxStackSize(), slot.getMaxStackSize(carried));
        }

        @Override
        public boolean allowsPickupAll(AbstractContainerMenu menu, ItemStack carried, net.minecraft.world.inventory.Slot slot) {
            if (!(menu instanceof StorageContainerMenuBase<?> storage)) {
                return true;
            }
            return storage.getSlotUpgradeContainer(slot)
                .map(container -> container.allowsPickupAll(slot))
                .orElse(true);
        }

        @Override
        public int repeatedQuickMoveLimit(AbstractContainerMenu menu, net.minecraft.world.inventory.Slot slot, ItemStack stack) {
            if (!(menu instanceof StorageContainerMenuBase<?> storage)) {
                return Integer.MAX_VALUE;
            }
            return storage.getSlotUpgradeContainer(slot)
                .map(container -> container.getRepeatedQuickMoveLimit(slot, stack))
                .orElse(Integer.MAX_VALUE);
        }

        @Override
        public boolean useNativeQuickMove(AbstractContainerMenu menu) {
            return menu instanceof StorageContainerMenuBase<?>;
        }

        @Override
        public boolean allowsTargetedQuickMoveSource(
            AbstractContainerMenu menu,
            net.minecraft.world.inventory.Slot slot
        ) {
            if (!(menu instanceof StorageContainerMenuBase<?> storage)) {
                return true;
            }
            return !(slot instanceof StorageUpgradeSlot) && storage.getSlotUpgradeContainer(slot).isEmpty();
        }

        @Override
        public Object physicalOwner(AbstractContainerMenu menu, net.minecraft.world.inventory.Slot slot) {
            return sophisticatedPhysicalOwner(menu, slot);
        }

        @Override
        public int physicalIndex(AbstractContainerMenu menu, net.minecraft.world.inventory.Slot slot) {
            return sophisticatedPhysicalIndex(slot);
        }
    };
    private static final DesktopMenuSlots.GesturePolicy SETTINGS_MENU_GESTURE_POLICY = new DesktopMenuSlots.GesturePolicy() {
        @Override
        public boolean canQuickCraft(AbstractContainerMenu menu, net.minecraft.world.inventory.Slot slot, ItemStack carried) {
            return false;
        }

        @Override
        public int quickCraftPlaceCount(
            AbstractContainerMenu menu,
            net.minecraft.world.inventory.Slot slot,
            int targetCount,
            int quickCraftType,
            ItemStack carried
        ) {
            return 0;
        }

        @Override
        public int quickCraftMaxStackSize(AbstractContainerMenu menu, net.minecraft.world.inventory.Slot slot, ItemStack carried) {
            return 0;
        }

        @Override
        public boolean allowsPickupAll(AbstractContainerMenu menu, ItemStack carried, net.minecraft.world.inventory.Slot slot) {
            return false;
        }

        @Override
        public boolean allowsPickupAllAnchor(
            AbstractContainerMenu menu,
            ItemStack carried,
            net.minecraft.world.inventory.Slot slot
        ) {
            return false;
        }

        @Override
        public int repeatedQuickMoveLimit(AbstractContainerMenu menu, net.minecraft.world.inventory.Slot slot, ItemStack stack) {
            return 0;
        }

        @Override
        public boolean allowsTargetedQuickMoveSource(
            AbstractContainerMenu menu,
            net.minecraft.world.inventory.Slot slot
        ) {
            return false;
        }

        @Override
        public Object physicalOwner(AbstractContainerMenu menu, net.minecraft.world.inventory.Slot slot) {
            return sophisticatedPhysicalOwner(menu, slot);
        }

        @Override
        public int physicalIndex(AbstractContainerMenu menu, net.minecraft.world.inventory.Slot slot) {
            return sophisticatedPhysicalIndex(slot);
        }
    };

    private SophisticatedCoreCompat() {
    }

    public static void initializeCommon() {
    }

    public static void initializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(DesktopMenuOpenDataPayload.TYPE, (client, listener, buffer, sender) -> {
            DesktopMenuOpenDataPayload payload = new DesktopMenuOpenDataPayload(buffer);
            client.execute(() -> DesktopContainerClient.acceptMenuOpenData(payload, client));
        });
    }

    public static void registerMainMenu(MenuType<?> menuType) {
        com.salts_inventory_update.internal.desktop.DesktopSortSlots.registerStorage(menuType,
            (menu, slot) -> MAIN_MENU_GESTURE_POLICY.allowsTargetedQuickMoveSource(menu, slot));
        SophisticatedLegacyMessageBridge.register(menuType);
        DesktopMenuSlots.register(
            menuType,
            menu -> ((StorageContainerMenuBase<?>) menu).getTotalSlotsNumber(),
            true,
            null,
            SophisticatedCoreCompat::prepareMainMenuSnapshot,
            MAIN_MENU_GESTURE_POLICY
        );
    }

    public static void registerSettingsMenu(MenuType<?> menuType) {
        SophisticatedLegacyMessageBridge.register(menuType);
        DesktopMenuSlots.register(
            menuType,
            menu -> ((SettingsContainerMenu<?>) menu).getNumberOfSlots(),
            true,
            null,
            null,
            SETTINGS_MENU_GESTURE_POLICY
        );
    }

    /**
     * Sophisticated keeps upgrade slots outside {@code menu.slots}. Upgrade-provided controls add their own slots
     * only after the client wrapper sees the installed upgrade, so seed the physical upgrade segment first. Panel
     * slots follow those physical slots in the authenticated logical snapshot and materialize from this seed.
     */
    public static void prepareMainMenuSnapshot(AbstractContainerMenu menu, List<ItemStack> items) {
        if (!(menu instanceof StorageContainerMenuBase<?> storage)
            || items.size() <= storage.getTotalSlotsNumber()) {
            return;
        }

        int upgradeSlotCount = storage.getNumberOfUpgradeSlots();
        int snapshotUpgradeStart = storage.getInventorySlotsSize();
        if (upgradeSlotCount <= 0 || items.size() < snapshotUpgradeStart + upgradeSlotCount) {
            return;
        }

        int seedSize = snapshotUpgradeStart + upgradeSlotCount;
        List<ItemStack> seed = items.subList(0, seedSize).stream().map(ItemStack::copy).toList();
        storage.initializeContents(storage.getStateId(), seed, storage.getCarried().copy());
    }

    public static DesktopMenuSlots.GesturePolicy mainMenuGesturePolicy() {
        return MAIN_MENU_GESTURE_POLICY;
    }

    public static DesktopMenuSlots.GesturePolicy settingsMenuGesturePolicy() {
        return SETTINGS_MENU_GESTURE_POLICY;
    }

    private static Object sophisticatedPhysicalOwner(
        AbstractContainerMenu menu,
        net.minecraft.world.inventory.Slot slot
    ) {
        if (slot instanceof SlotItemHandler itemHandlerSlot) {
            return itemHandlerSlot.getItemHandler();
        }
        return slot.container;
    }

    private static int sophisticatedPhysicalIndex(net.minecraft.world.inventory.Slot slot) {
        if (slot instanceof SlotSuppliedHandler supplied) {
            // SlotSuppliedHandler deliberately passes zero to Slot and keeps the real index here.
            return ((SlotSuppliedHandlerAccessor) supplied).salts_inventory_update$slot();
        }
        return slot.getContainerSlot();
    }

}
