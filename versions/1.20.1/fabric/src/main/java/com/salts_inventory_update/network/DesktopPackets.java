package com.salts_inventory_update.network;

import java.util.ArrayList;
import java.util.List;

import io.netty.buffer.Unpooled;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffers;

import com.salts_inventory_update.SaltsInventoryUpdate;

public final class DesktopPackets {
    private static final int CUSTOM_PAYLOAD_MAX_BYTES = 32 * 1024;
    public static final int PLAYER_MENU_SESSION = 0;
    public static final int SPECIAL_GENERIC = 0;
    public static final int SPECIAL_HORSE = 1;
    public static final int SPECIAL_CAMEL = 2;
    public static final int SPECIAL_LLAMA = 3;
    public static final int QUICK_TARGET_DEFAULT = 0;
    public static final int QUICK_TARGET_SESSION = 1;
    public static final int QUICK_TARGET_HOTBAR = 2;
    public static final int PIN_MODE_UNPINNED = 0;
    public static final int PIN_MODE_PINNED = 1;
    public static final int PIN_MODE_GHOST_PINNED = 2;

    private DesktopPackets() {
    }

    public static void registerPayloadTypes() {
    }

    public static ResourceLocation id(String path) {
        return new ResourceLocation(SaltsInventoryUpdate.MOD_ID, path);
    }

    public static int menuTypeId(MenuType<?> menuType) {
        return BuiltInRegistries.MENU.getId(menuType);
    }

    public static MenuType<?> menuTypeById(int id) {
        return BuiltInRegistries.MENU.byId(id);
    }

    public static FriendlyByteBuf toBuffer(DesktopPacket packet) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        packet.write(buf);
        return buf;
    }

    private static void writeItemList(FriendlyByteBuf buf, List<ItemStack> stacks) {
        buf.writeVarInt(stacks.size());
        for (ItemStack stack : stacks) {
            buf.writeItem(stack);
        }
    }

    private static List<ItemStack> readItemList(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        List<ItemStack> stacks = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            stacks.add(buf.readItem());
        }
        return stacks;
    }

    public interface DesktopPacket {
        ResourceLocation id();

        void write(FriendlyByteBuf buf);
    }

    public record InventorySlotPurchasePayload() implements DesktopPacket {
        public static final ResourceLocation TYPE = DesktopPackets.id("inventory_slot_purchase");

        public InventorySlotPurchasePayload(FriendlyByteBuf buf) {
            this();
        }

        @Override
        public ResourceLocation id() {
            return TYPE;
        }

        @Override
        public void write(FriendlyByteBuf buf) {
        }
    }

    public record InventoryExpansionSyncPayload(int slotCount, List<ItemStack> items) implements DesktopPacket {
        public static final ResourceLocation TYPE = DesktopPackets.id("inventory_expansion_sync");

        public InventoryExpansionSyncPayload(FriendlyByteBuf buf) {
            this(buf.readVarInt(), readItemList(buf));
        }

        @Override
        public ResourceLocation id() {
            return TYPE;
        }

        @Override
        public void write(FriendlyByteBuf buf) {
            buf.writeVarInt(this.slotCount);
            writeItemList(buf, this.items);
        }
    }

    public record DesktopReadyPayload(boolean ready) implements DesktopPacket {
        public static final ResourceLocation TYPE = DesktopPackets.id("desktop_ready");

        public DesktopReadyPayload(FriendlyByteBuf buf) {
            this(buf.readBoolean());
        }

        @Override
        public ResourceLocation id() {
            return TYPE;
        }

        @Override
        public void write(FriendlyByteBuf buf) {
            buf.writeBoolean(this.ready);
        }
    }

    public record DesktopClickPayload(int debugId, int sessionId, int slotIndex, int button, String inputName, ItemStack clientCarried) implements DesktopPacket {
        public static final ResourceLocation TYPE = DesktopPackets.id("desktop_click");

        public DesktopClickPayload(FriendlyByteBuf buf) {
            this(buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readUtf(), buf.readItem());
        }

        @Override
        public ResourceLocation id() {
            return TYPE;
        }

        @Override
        public void write(FriendlyByteBuf buf) {
            buf.writeVarInt(this.debugId);
            buf.writeVarInt(this.sessionId);
            buf.writeVarInt(this.slotIndex);
            buf.writeVarInt(this.button);
            buf.writeUtf(this.inputName);
            buf.writeItem(this.clientCarried);
        }
    }

    public record DesktopQuickMovePayload(int sourceSessionId, int sourceSlotIndex, int targetKind, int targetSessionId) implements DesktopPacket {
        public static final ResourceLocation TYPE = DesktopPackets.id("desktop_quick_move");

        public DesktopQuickMovePayload(FriendlyByteBuf buf) {
            this(buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt());
        }

        @Override
        public ResourceLocation id() {
            return TYPE;
        }

        @Override
        public void write(FriendlyByteBuf buf) {
            buf.writeVarInt(this.sourceSessionId);
            buf.writeVarInt(this.sourceSlotIndex);
            buf.writeVarInt(this.targetKind);
            buf.writeVarInt(this.targetSessionId);
        }
    }

    public record DesktopButtonPayload(int sessionId, int buttonId) implements DesktopPacket {
        public static final ResourceLocation TYPE = DesktopPackets.id("desktop_button");

        public DesktopButtonPayload(FriendlyByteBuf buf) {
            this(buf.readVarInt(), buf.readVarInt());
        }

        @Override
        public ResourceLocation id() {
            return TYPE;
        }

        @Override
        public void write(FriendlyByteBuf buf) {
            buf.writeVarInt(this.sessionId);
            buf.writeVarInt(this.buttonId);
        }
    }

    public record DesktopPlaceRecipePayload(int sessionId, ResourceLocation recipeId, boolean useMaxItems) implements DesktopPacket {
        public static final ResourceLocation TYPE = DesktopPackets.id("desktop_place_recipe");

        public DesktopPlaceRecipePayload(FriendlyByteBuf buf) {
            this(buf.readVarInt(), buf.readResourceLocation(), buf.readBoolean());
        }

        @Override
        public ResourceLocation id() {
            return TYPE;
        }

        @Override
        public void write(FriendlyByteBuf buf) {
            buf.writeVarInt(this.sessionId);
            buf.writeResourceLocation(this.recipeId);
            buf.writeBoolean(this.useMaxItems);
        }
    }

    public record DesktopRenamePayload(int sessionId, String name) implements DesktopPacket {
        public static final ResourceLocation TYPE = DesktopPackets.id("desktop_rename");

        public DesktopRenamePayload(FriendlyByteBuf buf) {
            this(buf.readVarInt(), buf.readUtf(50));
        }

        @Override
        public ResourceLocation id() {
            return TYPE;
        }

        @Override
        public void write(FriendlyByteBuf buf) {
            buf.writeVarInt(this.sessionId);
            buf.writeUtf(this.name, 50);
        }
    }

    public record DesktopCustomPayload(int sessionId, ResourceLocation channel, byte[] data) implements DesktopPacket {
        public static final ResourceLocation TYPE = DesktopPackets.id("desktop_custom");

        public DesktopCustomPayload(FriendlyByteBuf buf) {
            this(buf.readVarInt(), buf.readResourceLocation(), buf.readByteArray(CUSTOM_PAYLOAD_MAX_BYTES));
        }

        public DesktopCustomPayload {
            if (data.length > CUSTOM_PAYLOAD_MAX_BYTES) {
                throw new IllegalArgumentException("Desktop custom payload is too large: " + data.length);
            }
            data = data.clone();
        }

        @Override
        public byte[] data() {
            return this.data.clone();
        }

        @Override
        public ResourceLocation id() {
            return TYPE;
        }

        @Override
        public void write(FriendlyByteBuf buf) {
            buf.writeVarInt(this.sessionId);
            buf.writeResourceLocation(this.channel);
            buf.writeByteArray(this.data);
        }
    }

    public record DesktopCloseSessionPayload(int sessionId) implements DesktopPacket {
        public static final ResourceLocation TYPE = DesktopPackets.id("desktop_close_session");

        public DesktopCloseSessionPayload(FriendlyByteBuf buf) {
            this(buf.readVarInt());
        }

        @Override
        public ResourceLocation id() {
            return TYPE;
        }

        @Override
        public void write(FriendlyByteBuf buf) {
            buf.writeVarInt(this.sessionId);
        }
    }

    public record DesktopSessionPinPayload(int sessionId, int pinMode) implements DesktopPacket {
        public static final ResourceLocation TYPE = DesktopPackets.id("desktop_session_pin");

        public DesktopSessionPinPayload(FriendlyByteBuf buf) {
            this(buf.readVarInt(), buf.readVarInt());
        }

        @Override
        public ResourceLocation id() {
            return TYPE;
        }

        @Override
        public void write(FriendlyByteBuf buf) {
            buf.writeVarInt(this.sessionId);
            buf.writeVarInt(this.pinMode);
        }
    }

    public record DesktopSessionVisibilityPayload(int sessionId, boolean visible) implements DesktopPacket {
        public static final ResourceLocation TYPE = DesktopPackets.id("desktop_session_visibility");

        public DesktopSessionVisibilityPayload(FriendlyByteBuf buf) {
            this(buf.readVarInt(), buf.readBoolean());
        }

        @Override
        public ResourceLocation id() {
            return TYPE;
        }

        @Override
        public void write(FriendlyByteBuf buf) {
            buf.writeVarInt(this.sessionId);
            buf.writeBoolean(this.visible);
        }
    }

    public record DesktopOpenSessionPayload(
        int sessionId,
        int menuTypeId,
        int specialKind,
        int entityId,
        int columns,
        int stateId,
        boolean visible,
        String sourceKey,
        Component title,
        List<ItemStack> items,
        ItemStack carried,
        int[] data
    ) implements DesktopPacket {
        public static final ResourceLocation TYPE = DesktopPackets.id("desktop_open_session");

        public DesktopOpenSessionPayload(FriendlyByteBuf buf) {
            this(
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readBoolean(),
                buf.readUtf(),
                buf.readComponent(),
                readItemList(buf),
                buf.readItem(),
                buf.readVarIntArray()
            );
        }

        @Override
        public ResourceLocation id() {
            return TYPE;
        }

        @Override
        public void write(FriendlyByteBuf buf) {
            buf.writeVarInt(this.sessionId);
            buf.writeVarInt(this.menuTypeId);
            buf.writeVarInt(this.specialKind);
            buf.writeVarInt(this.entityId);
            buf.writeVarInt(this.columns);
            buf.writeVarInt(this.stateId);
            buf.writeBoolean(this.visible);
            buf.writeUtf(this.sourceKey);
            buf.writeComponent(this.title);
            writeItemList(buf, this.items);
            buf.writeItem(this.carried);
            buf.writeVarIntArray(this.data);
        }
    }

    public record DesktopSlotPayload(int sessionId, int slotIndex, int stateId, ItemStack stack) implements DesktopPacket {
        public static final ResourceLocation TYPE = DesktopPackets.id("desktop_slot");

        public DesktopSlotPayload(FriendlyByteBuf buf) {
            this(buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readItem());
        }

        @Override
        public ResourceLocation id() {
            return TYPE;
        }

        @Override
        public void write(FriendlyByteBuf buf) {
            buf.writeVarInt(this.sessionId);
            buf.writeVarInt(this.slotIndex);
            buf.writeVarInt(this.stateId);
            buf.writeItem(this.stack);
        }
    }

    public record DesktopDataPayload(int sessionId, int dataSlot, int value) implements DesktopPacket {
        public static final ResourceLocation TYPE = DesktopPackets.id("desktop_data");

        public DesktopDataPayload(FriendlyByteBuf buf) {
            this(buf.readVarInt(), buf.readVarInt(), buf.readVarInt());
        }

        @Override
        public ResourceLocation id() {
            return TYPE;
        }

        @Override
        public void write(FriendlyByteBuf buf) {
            buf.writeVarInt(this.sessionId);
            buf.writeVarInt(this.dataSlot);
            buf.writeVarInt(this.value);
        }
    }

    public record DesktopCarriedPayload(ItemStack carried) implements DesktopPacket {
        public static final ResourceLocation TYPE = DesktopPackets.id("desktop_carried");

        public DesktopCarriedPayload(FriendlyByteBuf buf) {
            this(buf.readItem());
        }

        @Override
        public ResourceLocation id() {
            return TYPE;
        }

        @Override
        public void write(FriendlyByteBuf buf) {
            buf.writeItem(this.carried);
        }
    }

    public record DesktopGhostRecipePayload(int sessionId, ResourceLocation recipeId) implements DesktopPacket {
        public static final ResourceLocation TYPE = DesktopPackets.id("desktop_ghost_recipe");

        public DesktopGhostRecipePayload(FriendlyByteBuf buf) {
            this(buf.readVarInt(), buf.readResourceLocation());
        }

        @Override
        public ResourceLocation id() {
            return TYPE;
        }

        @Override
        public void write(FriendlyByteBuf buf) {
            buf.writeVarInt(this.sessionId);
            buf.writeResourceLocation(this.recipeId);
        }
    }

    public record DesktopSessionClosedPayload(int sessionId) implements DesktopPacket {
        public static final ResourceLocation TYPE = DesktopPackets.id("desktop_session_closed");

        public DesktopSessionClosedPayload(FriendlyByteBuf buf) {
            this(buf.readVarInt());
        }

        @Override
        public ResourceLocation id() {
            return TYPE;
        }

        @Override
        public void write(FriendlyByteBuf buf) {
            buf.writeVarInt(this.sessionId);
        }
    }

    public record DesktopMerchantOffersPayload(
        int sessionId,
        MerchantOffers offers,
        int villagerLevel,
        int villagerXp,
        boolean showProgress,
        boolean canRestock
    ) implements DesktopPacket {
        public static final ResourceLocation TYPE = DesktopPackets.id("desktop_merchant_offers");

        public DesktopMerchantOffersPayload(FriendlyByteBuf buf) {
            this(buf.readVarInt(), MerchantOffers.createFromStream(buf), buf.readVarInt(), buf.readVarInt(), buf.readBoolean(), buf.readBoolean());
        }

        @Override
        public ResourceLocation id() {
            return TYPE;
        }

        @Override
        public void write(FriendlyByteBuf buf) {
            buf.writeVarInt(this.sessionId);
            this.offers.writeToStream(buf);
            buf.writeVarInt(this.villagerLevel);
            buf.writeVarInt(this.villagerXp);
            buf.writeBoolean(this.showProgress);
            buf.writeBoolean(this.canRestock);
        }
    }
}
