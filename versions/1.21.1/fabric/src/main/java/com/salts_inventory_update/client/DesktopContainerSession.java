package com.salts_inventory_update.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.camel.Camel;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.animal.horse.Llama;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.HorseInventoryMenu;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import com.salts_inventory_update.debug.DesktopDebug;
import com.salts_inventory_update.internal.desktop.DesktopItemSourceLocks;
import com.salts_inventory_update.internal.desktop.DesktopMenuSlots;
import com.salts_inventory_update.network.DesktopPackets;
import com.salts_inventory_update.network.DesktopPackets.DesktopMerchantOffersPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopOpenSessionPayload;

public final class DesktopContainerSession {
    private final int sessionId;
    private final long sessionToken;
    private final AbstractContainerMenu menu;
    private final Component title;
    private final String sourceKey;
    private final int specialKind;
    private final int entityId;
    private final int columns;
    private final boolean recipeTransferSupported;
    private final int replacesSessionId;
    private final List<Slot> containerSlots;
    private final int minSlotX;
    private final int minSlotY;
    private final int contentWidth;
    private final int contentHeight;
    private final DesktopItemSourceLocks.Lease sourceLock;
    private boolean closed;

    private DesktopContainerSession(
        int sessionId,
        long sessionToken,
        AbstractContainerMenu menu,
        LocalPlayer player,
        Component title,
        String sourceKey,
        int specialKind,
        int entityId,
        int columns,
        boolean recipeTransferSupported,
        int replacesSessionId
    ) {
        this.sessionId = sessionId;
        this.sessionToken = sessionToken;
        this.menu = menu;
        this.title = title;
        this.sourceKey = sourceKey;
        this.specialKind = specialKind;
        this.entityId = entityId;
        this.columns = columns;
        this.recipeTransferSupported = recipeTransferSupported;
        this.replacesSessionId = replacesSessionId;
        Inventory playerInventory = player.getInventory();
        this.containerSlots = findContainerSlots(menu, playerInventory);
        this.minSlotX = minSlotX(this.containerSlots);
        this.minSlotY = minSlotY(this.containerSlots);
        this.contentWidth = contentWidth(this.containerSlots, this.minSlotX);
        this.contentHeight = contentHeight(this.containerSlots, this.minSlotY);
        this.sourceLock = DesktopItemSourceLocks.acquire(player, menu);
    }

    public static DesktopContainerSession create(Minecraft minecraft, DesktopOpenSessionPayload payload) {
        return create(minecraft, payload, new byte[0], -1);
    }

    public static DesktopContainerSession create(
        Minecraft minecraft,
        DesktopOpenSessionPayload payload,
        byte[] openingData,
        int replacesSessionId
    ) {
        LocalPlayer player = minecraft.player;
        if (player == null) {
            throw new IllegalStateException("Cannot create container session without a local player");
        }

        AbstractContainerMenu menu = createMenu(minecraft, payload, player, openingData);
        List<ItemStack> items = payload.items();
        DesktopMenuSlots.prepareSnapshot(menu, items);
        int logicalSlots = DesktopMenuSlots.size(menu);
        if (items.size() != logicalSlots) {
            throw new IllegalArgumentException(
                "Desktop session slot snapshot mismatch: session="
                    + payload.sessionId()
                    + ", expected="
                    + logicalSlots
                    + ", actual="
                    + items.size()
            );
        }
        menu.initializeContents(payload.stateId(), items, payload.carried());
        int initializedLogicalSlots = DesktopMenuSlots.size(menu);
        if (items.size() != initializedLogicalSlots) {
            throw new IllegalArgumentException(
                "Desktop session slot snapshot mismatch after initialization: session="
                    + payload.sessionId()
                    + ", expected="
                    + initializedLogicalSlots
                    + ", actual="
                    + items.size()
            );
        }
        int[] data = payload.data();
        for (int i = 0; i < data.length; i++) {
            try {
                menu.setData(i, data[i]);
            } catch (IndexOutOfBoundsException ignored) {
                break;
            }
        }

        return new DesktopContainerSession(
            payload.sessionId(),
            payload.sessionToken(),
            menu,
            player,
            payload.title(),
            payload.sourceKey(),
            payload.specialKind(),
            payload.entityId(),
            payload.columns(),
            payload.recipeTransferSupported(),
            replacesSessionId
        );
    }

    public void close() {
        if (this.closed) {
            return;
        }
        this.closed = true;
        this.sourceLock.close();
    }

    private static AbstractContainerMenu createMenu(
        Minecraft minecraft,
        DesktopOpenSessionPayload payload,
        LocalPlayer player,
        byte[] openingData
    ) {
        if (isHorseSpecialKind(payload.specialKind())) {
            Entity entity = minecraft.level == null ? null : minecraft.level.getEntity(payload.entityId());
            if (isCamelOrLlamaSpecial(payload.specialKind())) {
                mountDiag(
                    "client_create_horse_lookup session={} special={} entityId={} levelPresent={} entityPresent={} entityType={} entityClass={} abstractHorse={} camel={} llama={}",
                    payload.sessionId(),
                    payload.specialKind(),
                    payload.entityId(),
                    minecraft.level != null,
                    entity != null,
                    entity == null ? "null" : BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()),
                    entity == null ? "null" : entity.getClass().getName(),
                    entity instanceof AbstractHorse,
                    entity instanceof Camel,
                    entity instanceof Llama
                );
            }
            if (entity instanceof AbstractHorse horse) {
                return new HorseInventoryMenu(
                    payload.sessionId(),
                    player.getInventory(),
                    new SimpleContainer(2 + payload.columns() * 3),
                    horse,
                    payload.columns()
                );
            }
        } else {
            MenuType<?> menuType = DesktopPackets.menuTypeById(payload.menuTypeId());
            if (menuType != null) {
                AbstractContainerMenu adapted = DesktopMenuFactories.create(
                    menuType,
                    minecraft,
                    player,
                    payload.sessionId(),
                    openingData
                );
                if (adapted != null) {
                    return adapted;
                }
                return menuType.create(payload.sessionId(), player.getInventory());
            }
        }

        throw new IllegalStateException(
            "Unsupported desktop container session "
                + payload.sessionId()
                + " special="
                + payload.specialKind()
                + " entity="
                + describeEntity(minecraft, payload.entityId())
        );
    }

    private static boolean isHorseSpecialKind(int specialKind) {
        return specialKind == DesktopPackets.SPECIAL_HORSE
            || specialKind == DesktopPackets.SPECIAL_CAMEL
            || specialKind == DesktopPackets.SPECIAL_LLAMA;
    }

    private static boolean isCamelOrLlamaSpecial(int specialKind) {
        return specialKind == DesktopPackets.SPECIAL_CAMEL || specialKind == DesktopPackets.SPECIAL_LLAMA;
    }

    private static String describeEntity(Minecraft minecraft, int entityId) {
        Entity entity = minecraft.level == null ? null : minecraft.level.getEntity(entityId);
        if (entity == null) {
            return "null";
        }
        return BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()) + "/" + entity.getClass().getName();
    }

    private static void mountDiag(String message, Object... args) {
        DesktopDebug.detail("SIU_MOUNT_DIAG " + message, args);
    }

    public int sessionId() {
        return this.sessionId;
    }

    public long sessionToken() {
        return this.sessionToken;
    }

    public boolean recipeTransferSupported() {
        return this.recipeTransferSupported;
    }

    public int replacesSessionId() {
        return this.replacesSessionId;
    }

    public AbstractContainerMenu menu() {
        return this.menu;
    }

    public Component title() {
        return this.title;
    }

    public String sourceKey() {
        return this.sourceKey;
    }

    public int specialKind() {
        return this.specialKind;
    }

    public int entityId() {
        return this.entityId;
    }

    public int columns() {
        return this.columns;
    }

    public boolean isMountSession() {
        return isHorseSpecialKind(this.specialKind) || this.specialKind == DesktopPackets.SPECIAL_NAUTILUS;
    }

    public LivingEntity mountEntity(Minecraft minecraft) {
        Entity entity = minecraft.level == null ? null : minecraft.level.getEntity(this.entityId);
        return entity instanceof LivingEntity livingEntity ? livingEntity : null;
    }

    public List<Slot> containerSlots() {
        return this.containerSlots;
    }

    public int minSlotX() {
        return this.minSlotX;
    }

    public int minSlotY() {
        return this.minSlotY;
    }

    public int contentWidth() {
        return this.contentWidth;
    }

    public int contentHeight() {
        return this.contentHeight;
    }

    public void updateSlot(int slotIndex, int stateId, ItemStack stack) {
        if (slotIndex >= 0 && slotIndex < DesktopMenuSlots.size(this.menu)) {
            this.menu.setItem(slotIndex, stateId, stack);
        }
    }

    public void updateData(int dataSlot, int value) {
        try {
            this.menu.setData(dataSlot, value);
        } catch (IndexOutOfBoundsException ignored) {
        }
    }

    public void setCarried(ItemStack carried) {
        this.menu.setCarried(carried.copy());
    }

    public void applyMerchantOffers(DesktopMerchantOffersPayload payload) {
        if (this.menu instanceof MerchantMenu merchantMenu) {
            merchantMenu.setOffers(payload.offers());
            merchantMenu.setMerchantLevel(payload.villagerLevel());
            merchantMenu.setXp(payload.villagerXp());
            merchantMenu.setShowProgressBar(payload.showProgress());
            merchantMenu.setCanRestock(payload.canRestock());
        }
    }

    private static List<Slot> findContainerSlots(AbstractContainerMenu menu, Inventory playerInventory) {
        List<Slot> slots = new ArrayList<>();
        for (Slot slot : DesktopMenuSlots.all(menu)) {
            if (slot.container != playerInventory) {
                slots.add(slot);
            }
        }
        return List.copyOf(slots);
    }

    private static int minSlotX(List<Slot> slots) {
        int min = Integer.MAX_VALUE;
        for (Slot slot : slots) {
            min = Math.min(min, slot.x);
        }
        return min == Integer.MAX_VALUE ? 0 : min;
    }

    private static int minSlotY(List<Slot> slots) {
        int min = Integer.MAX_VALUE;
        for (Slot slot : slots) {
            min = Math.min(min, slot.y);
        }
        return min == Integer.MAX_VALUE ? 0 : min;
    }

    private static int contentWidth(List<Slot> slots, int minSlotX) {
        int max = 0;
        for (Slot slot : slots) {
            max = Math.max(max, slot.x - minSlotX + InventoryDesktopScreen.SLOT_SIZE);
        }
        return max;
    }

    private static int contentHeight(List<Slot> slots, int minSlotY) {
        int max = 0;
        for (Slot slot : slots) {
            max = Math.max(max, slot.y - minSlotY + InventoryDesktopScreen.SLOT_SIZE);
        }
        return max;
    }
}
