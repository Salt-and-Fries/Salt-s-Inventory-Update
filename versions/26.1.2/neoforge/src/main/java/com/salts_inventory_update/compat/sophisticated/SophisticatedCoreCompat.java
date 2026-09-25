package com.salts_inventory_update.compat.sophisticated;

import java.lang.reflect.Proxy;
import java.util.List;

import com.salts_inventory_update.api.server.desktop.DesktopServerPayloadContext;
import com.salts_inventory_update.client.DesktopContainerClient;
import com.salts_inventory_update.internal.desktop.DesktopItemSourceLocks;
import com.salts_inventory_update.internal.desktop.DesktopMenuSlots;
import com.salts_inventory_update.network.DesktopMenuOpenDataPayload;
import com.salts_inventory_update.platform.fabric.api.client.networking.v1.ClientPlayNetworking;
import com.salts_inventory_update.server.DesktopContainerSessions;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.transfer.item.ResourceHandlerSlot;
import net.neoforged.neoforge.world.inventory.StackCopySlot;
import net.p3pp3rf1y.sophisticatedcore.common.gui.ISyncedContainer;
import net.p3pp3rf1y.sophisticatedcore.common.gui.SettingsContainerMenu;
import net.p3pp3rf1y.sophisticatedcore.common.gui.SlotSuppliedHandler;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase.StorageUpgradeSlot;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageInventorySlot;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeContainerBase;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.SetGhostSlotPayload;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.common.SetMemorySlotPayload;
import net.p3pp3rf1y.sophisticatedcore.network.SyncContainerClientDataPayload;
import net.p3pp3rf1y.sophisticatedcore.network.TransferFullSlotPayload;
import net.p3pp3rf1y.sophisticatedcore.network.TransferItemsPayload;
import net.p3pp3rf1y.sophisticatedcore.upgrades.tank.TankClickPayload;
import net.p3pp3rf1y.sophisticatedcore.upgrades.tank.TankUpgradeContainer;

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
        ClientPlayNetworking.registerGlobalReceiver(DesktopMenuOpenDataPayload.TYPE, (payload, context) ->
            DesktopContainerClient.acceptMenuOpenData(payload, context.client())
        );
    }

    public static void registerMainMenu(MenuType<?> menuType) {
        com.salts_inventory_update.internal.desktop.DesktopSortSlots.registerStorage(menuType,
            (menu, slot) -> MAIN_MENU_GESTURE_POLICY.allowsTargetedQuickMoveSource(menu, slot));
        DesktopMenuSlots.register(
            menuType,
            menu -> ((StorageContainerMenuBase<?>) menu).getTotalSlotsNumber(),
            true,
            null,
            SophisticatedCoreCompat::prepareMainMenuSnapshot,
            MAIN_MENU_GESTURE_POLICY
        );
        registerSync(menuType);
        register(menuType, TransferItemsPayload.TYPE.id(), TransferItemsPayload.STREAM_CODEC, (context, payload) -> {
            // The native transfer-to-storage helper walks the entire player inventory directly. It does not know
            // that an item backpack may be the live source of this detached menu, so refuse that bulk operation
            // while any such source is locked rather than allowing the open backpack to be moved into itself.
            if (!payload.transferToInventory()
                && DesktopItemSourceLocks.hasLockedSourceInRange(context.player(), 9, Inventory.INVENTORY_SIZE)) {
                context.broadcastChanges();
                return;
            }
            runOfficial(context, () -> TransferItemsPayload.handlePayload(payload, payloadContext(context.player())));
        });
        register(menuType, TransferFullSlotPayload.TYPE.id(), TransferFullSlotPayload.STREAM_CODEC, (context, payload) ->
            runForValidUnlockedSlot(context, payload.slotId(), () ->
                TransferFullSlotPayload.handlePayload(payload, payloadContext(context.player()))
            )
        );
        register(menuType, SetGhostSlotPayload.TYPE.id(), SetGhostSlotPayload.STREAM_CODEC, (context, payload) ->
            runForValidSlot(context, payload.slotNumber(), () ->
                SetGhostSlotPayload.handlePayload(payload, payloadContext(context.player()))
            )
        );
        register(menuType, TankClickPayload.TYPE.id(), TankClickPayload.STREAM_CODEC, SophisticatedCoreCompat::handleTankClick);
    }

    public static void registerSettingsMenu(MenuType<?> menuType) {
        DesktopMenuSlots.register(
            menuType,
            menu -> ((SettingsContainerMenu<?>) menu).getNumberOfSlots(),
            true,
            null,
            null,
            SETTINGS_MENU_GESTURE_POLICY
        );
        registerSync(menuType);
        register(menuType, SetMemorySlotPayload.TYPE.id(), SetMemorySlotPayload.STREAM_CODEC, (context, payload) ->
            runForValidSlot(context, payload.slotNumber(), () ->
                SetMemorySlotPayload.handlePayload(payload, payloadContext(context.player()))
            )
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
        if (slot instanceof SlotSuppliedHandler supplied) {
            return supplied.getResourceHandler();
        }
        if (slot instanceof ResourceHandlerSlot resourceSlot) {
            return resourceSlot.getResourceHandler();
        }
        if (slot instanceof StorageUpgradeSlot upgradeSlot) {
            return upgradeSlot.getResourceHandler();
        }
        // StackCopySlot's vanilla container is one shared zero-sized sentinel.  If a future
        // Sophisticated slot does not expose its resource handler, scope it to this menu.
        return slot instanceof StackCopySlot ? menu : slot.container;
    }

    private static int sophisticatedPhysicalIndex(net.minecraft.world.inventory.Slot slot) {
        if (slot instanceof SlotSuppliedHandler supplied) {
            // SlotSuppliedHandler deliberately passes zero to Slot and keeps the real index here.
            return supplied.slot;
        }
        return slot.getContainerSlot();
    }

    private static void registerSync(MenuType<?> menuType) {
        register(menuType, SyncContainerClientDataPayload.TYPE.id(), SyncContainerClientDataPayload.STREAM_CODEC, (context, payload) -> {
            if (payload.data() == null) {
                return;
            }
            runOfficial(context, () -> {
                if (context.menu() instanceof ISyncedContainer synced) {
                    synced.handlePacket(payload.data());
                } else if (context.menu() instanceof SettingsContainerMenu<?> settings) {
                    settings.handlePacket(payload.data());
                }
            });
        });
    }

    private static void handleTankClick(DesktopServerPayloadContext<AbstractContainerMenu> context, TankClickPayload payload) {
        if (!(context.menu() instanceof StorageContainerMenuBase<?> storage)) {
            return;
        }
        DesktopContainerSessions.withSessionTransition(context.player(), context.sessionId(), () -> {
            UpgradeContainerBase<?, ?> upgrade = storage.getUpgradeContainers().get(payload.upgradeSlot());
            if (!(upgrade instanceof TankUpgradeContainer tank)) {
                return;
            }
            ItemStack cursor = context.player().inventoryMenu.getCarried().copy();
            if (cursor.getCount() > 1) {
                return;
            }
            tank.getUpgradeWrapper().interactWithCursorStack(cursor, stack ->
                context.player().inventoryMenu.setCarried(stack.copy())
            );
        });
        context.broadcastChanges();
    }

    private static void runOfficial(DesktopServerPayloadContext<AbstractContainerMenu> context, Runnable action) {
        DesktopContainerSessions.withSessionTransition(context.player(), context.sessionId(), action);
        context.broadcastChanges();
    }

    private static void runForValidSlot(
        DesktopServerPayloadContext<AbstractContainerMenu> context,
        int slotId,
        Runnable action
    ) {
        if (slotId < 0 || slotId >= DesktopMenuSlots.size(context.menu())) {
            return;
        }
        runOfficial(context, action);
    }

    private static void runForValidUnlockedSlot(
        DesktopServerPayloadContext<AbstractContainerMenu> context,
        int slotId,
        Runnable action
    ) {
        if (slotId < 0 || slotId >= DesktopMenuSlots.size(context.menu())) {
            return;
        }
        if (DesktopItemSourceLocks.isSlotLocked(context.player(), DesktopMenuSlots.slot(context.menu(), slotId))) {
            context.broadcastChanges();
            return;
        }
        runOfficial(context, action);
    }

    private static IPayloadContext payloadContext(ServerPlayer player) {
        return (IPayloadContext) Proxy.newProxyInstance(
            SophisticatedCoreCompat.class.getClassLoader(),
            new Class<?>[] {IPayloadContext.class},
            (proxy, method, args) -> switch (method.getName()) {
                case "player" -> player;
                case "toString" -> "SaltDetachedSophisticatedPayloadContext[" + player.getGameProfile().name() + "]";
                case "hashCode" -> System.identityHashCode(proxy);
                case "equals" -> proxy == args[0];
                default -> throw new UnsupportedOperationException("Unsupported detached payload context operation " + method.getName());
            }
        );
    }

    private static <P> void register(
        MenuType<?> menuType,
        net.minecraft.resources.Identifier channel,
        StreamCodec<? super RegistryFriendlyByteBuf, P> codec,
        com.salts_inventory_update.api.server.desktop.DesktopTypedServerPayloadHandler<AbstractContainerMenu, P> handler
    ) {
        SophisticatedPayloadRegistration.register(menuType, channel, codec, handler);
    }

    public static IPayloadContext officialPayloadContext(ServerPlayer player) {
        return payloadContext(player);
    }

    public static void runSessionAction(DesktopServerPayloadContext<AbstractContainerMenu> context, Runnable action) {
        runOfficial(context, action);
    }
}
