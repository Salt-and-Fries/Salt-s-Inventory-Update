package com.salts_inventory_update.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.HorseInventoryMenu;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import com.salts_inventory_update.network.DesktopPackets;
import com.salts_inventory_update.network.DesktopPackets.DesktopMerchantOffersPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopOpenSessionPayload;

public final class DesktopContainerSession {
    private final int sessionId;
    private final AbstractContainerMenu menu;
    private final Component title;
    private final String sourceKey;
    private final int specialKind;
    private final int entityId;
    private final int columns;
    private final List<Slot> containerSlots;
    private final int minSlotX;
    private final int minSlotY;
    private final int contentWidth;
    private final int contentHeight;

    private DesktopContainerSession(
        int sessionId,
        AbstractContainerMenu menu,
        Inventory playerInventory,
        Component title,
        String sourceKey,
        int specialKind,
        int entityId,
        int columns
    ) {
        this.sessionId = sessionId;
        this.menu = menu;
        this.title = title;
        this.sourceKey = sourceKey;
        this.specialKind = specialKind;
        this.entityId = entityId;
        this.columns = columns;
        this.containerSlots = findContainerSlots(menu, playerInventory);
        this.minSlotX = minSlotX(this.containerSlots);
        this.minSlotY = minSlotY(this.containerSlots);
        this.contentWidth = contentWidth(this.containerSlots, this.minSlotX);
        this.contentHeight = contentHeight(this.containerSlots, this.minSlotY);
    }

    public static DesktopContainerSession create(Minecraft minecraft, DesktopOpenSessionPayload payload) {
        LocalPlayer player = minecraft.player;
        if (player == null) {
            throw new IllegalStateException("Cannot create container session without a local player");
        }

        AbstractContainerMenu menu = createMenu(minecraft, payload, player);
        List<ItemStack> items = payload.items();
        if (items.size() > menu.slots.size()) {
            items = items.subList(0, menu.slots.size());
        }
        menu.initializeContents(payload.stateId(), items, payload.carried());
        for (int i = 0; i < payload.data().length; i++) {
            try {
                menu.setData(i, payload.data()[i]);
            } catch (IndexOutOfBoundsException ignored) {
                break;
            }
        }

        return new DesktopContainerSession(
            payload.sessionId(),
            menu,
            player.getInventory(),
            payload.title(),
            payload.sourceKey(),
            payload.specialKind(),
            payload.entityId(),
            payload.columns()
        );
    }

    private static AbstractContainerMenu createMenu(Minecraft minecraft, DesktopOpenSessionPayload payload, LocalPlayer player) {
        if (isHorseSpecialKind(payload.specialKind())) {
            Entity entity = minecraft.level == null ? null : minecraft.level.getEntity(payload.entityId());
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
                return menuType.create(payload.sessionId(), player.getInventory());
            }
        }

        throw new IllegalStateException("Unsupported desktop container session " + payload.sessionId());
    }

    private static boolean isHorseSpecialKind(int specialKind) {
        return specialKind == DesktopPackets.SPECIAL_HORSE
            || specialKind == DesktopPackets.SPECIAL_CAMEL
            || specialKind == DesktopPackets.SPECIAL_LLAMA;
    }

    public int sessionId() {
        return this.sessionId;
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
        return isHorseSpecialKind(this.specialKind);
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
        if (slotIndex >= 0 && slotIndex < this.menu.slots.size()) {
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
        for (Slot slot : menu.slots) {
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
