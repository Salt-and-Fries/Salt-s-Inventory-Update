package com.salts_inventory_update.network;

import java.util.ArrayList;
import java.util.List;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffers;

import com.salts_inventory_update.SaltsInventoryUpdate;

public final class DesktopPackets {
    public static final int PLAYER_MENU_SESSION = 0;
    public static final int SPECIAL_GENERIC = 0;
    public static final int SPECIAL_HORSE = 1;
    public static final int SPECIAL_NAUTILUS = 2;

    private DesktopPackets() {
    }

    public static void registerPayloadTypes() {
        PayloadTypeRegistry.serverboundPlay().register(DesktopReadyPayload.TYPE, DesktopReadyPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(DesktopClickPayload.TYPE, DesktopClickPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(DesktopCloseSessionPayload.TYPE, DesktopCloseSessionPayload.CODEC);

        PayloadTypeRegistry.clientboundPlay().register(DesktopOpenSessionPayload.TYPE, DesktopOpenSessionPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(DesktopSlotPayload.TYPE, DesktopSlotPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(DesktopDataPayload.TYPE, DesktopDataPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(DesktopCarriedPayload.TYPE, DesktopCarriedPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(DesktopSessionClosedPayload.TYPE, DesktopSessionClosedPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(DesktopMerchantOffersPayload.TYPE, DesktopMerchantOffersPayload.CODEC);
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(SaltsInventoryUpdate.MOD_ID, path);
    }

    public static int menuTypeId(MenuType<?> menuType) {
        return BuiltInRegistries.MENU.getId(menuType);
    }

    public static MenuType<?> menuTypeById(int id) {
        return BuiltInRegistries.MENU.byId(id);
    }

    private static void writeItemList(RegistryFriendlyByteBuf buf, List<ItemStack> stacks) {
        buf.writeVarInt(stacks.size());
        for (ItemStack stack : stacks) {
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, stack);
        }
    }

    private static List<ItemStack> readItemList(RegistryFriendlyByteBuf buf) {
        int size = buf.readVarInt();
        List<ItemStack> stacks = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            stacks.add(ItemStack.OPTIONAL_STREAM_CODEC.decode(buf));
        }
        return stacks;
    }

    public record DesktopReadyPayload(boolean ready) implements CustomPacketPayload {
        public static final Type<DesktopReadyPayload> TYPE = new Type<>(id("desktop_ready"));
        public static final StreamCodec<RegistryFriendlyByteBuf, DesktopReadyPayload> CODEC = CustomPacketPayload.codec(
            DesktopReadyPayload::write,
            DesktopReadyPayload::new
        );

        private DesktopReadyPayload(RegistryFriendlyByteBuf buf) {
            this(buf.readBoolean());
        }

        private void write(RegistryFriendlyByteBuf buf) {
            buf.writeBoolean(this.ready);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record DesktopClickPayload(int sessionId, int slotIndex, int button, int inputOrdinal) implements CustomPacketPayload {
        public static final Type<DesktopClickPayload> TYPE = new Type<>(id("desktop_click"));
        public static final StreamCodec<RegistryFriendlyByteBuf, DesktopClickPayload> CODEC = CustomPacketPayload.codec(
            DesktopClickPayload::write,
            DesktopClickPayload::new
        );

        private DesktopClickPayload(RegistryFriendlyByteBuf buf) {
            this(buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt());
        }

        private void write(RegistryFriendlyByteBuf buf) {
            buf.writeVarInt(this.sessionId);
            buf.writeVarInt(this.slotIndex);
            buf.writeVarInt(this.button);
            buf.writeVarInt(this.inputOrdinal);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record DesktopCloseSessionPayload(int sessionId) implements CustomPacketPayload {
        public static final Type<DesktopCloseSessionPayload> TYPE = new Type<>(id("desktop_close_session"));
        public static final StreamCodec<RegistryFriendlyByteBuf, DesktopCloseSessionPayload> CODEC = CustomPacketPayload.codec(
            DesktopCloseSessionPayload::write,
            DesktopCloseSessionPayload::new
        );

        private DesktopCloseSessionPayload(RegistryFriendlyByteBuf buf) {
            this(buf.readVarInt());
        }

        private void write(RegistryFriendlyByteBuf buf) {
            buf.writeVarInt(this.sessionId);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record DesktopOpenSessionPayload(
        int sessionId,
        int menuTypeId,
        int specialKind,
        int entityId,
        int columns,
        int stateId,
        String sourceKey,
        Component title,
        List<ItemStack> items,
        ItemStack carried,
        int[] data
    ) implements CustomPacketPayload {
        public static final Type<DesktopOpenSessionPayload> TYPE = new Type<>(id("desktop_open_session"));
        public static final StreamCodec<RegistryFriendlyByteBuf, DesktopOpenSessionPayload> CODEC = CustomPacketPayload.codec(
            DesktopOpenSessionPayload::write,
            DesktopOpenSessionPayload::new
        );

        private DesktopOpenSessionPayload(RegistryFriendlyByteBuf buf) {
            this(
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readUtf(),
                ComponentSerialization.STREAM_CODEC.decode(buf),
                readItemList(buf),
                ItemStack.OPTIONAL_STREAM_CODEC.decode(buf),
                buf.readVarIntArray()
            );
        }

        private void write(RegistryFriendlyByteBuf buf) {
            buf.writeVarInt(this.sessionId);
            buf.writeVarInt(this.menuTypeId);
            buf.writeVarInt(this.specialKind);
            buf.writeVarInt(this.entityId);
            buf.writeVarInt(this.columns);
            buf.writeVarInt(this.stateId);
            buf.writeUtf(this.sourceKey);
            ComponentSerialization.STREAM_CODEC.encode(buf, this.title);
            writeItemList(buf, this.items);
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, this.carried);
            buf.writeVarIntArray(this.data);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record DesktopSlotPayload(int sessionId, int slotIndex, int stateId, ItemStack stack) implements CustomPacketPayload {
        public static final Type<DesktopSlotPayload> TYPE = new Type<>(id("desktop_slot"));
        public static final StreamCodec<RegistryFriendlyByteBuf, DesktopSlotPayload> CODEC = CustomPacketPayload.codec(
            DesktopSlotPayload::write,
            DesktopSlotPayload::new
        );

        private DesktopSlotPayload(RegistryFriendlyByteBuf buf) {
            this(
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt(),
                ItemStack.OPTIONAL_STREAM_CODEC.decode(buf)
            );
        }

        private void write(RegistryFriendlyByteBuf buf) {
            buf.writeVarInt(this.sessionId);
            buf.writeVarInt(this.slotIndex);
            buf.writeVarInt(this.stateId);
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, this.stack);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record DesktopDataPayload(int sessionId, int dataSlot, int value) implements CustomPacketPayload {
        public static final Type<DesktopDataPayload> TYPE = new Type<>(id("desktop_data"));
        public static final StreamCodec<RegistryFriendlyByteBuf, DesktopDataPayload> CODEC = CustomPacketPayload.codec(
            DesktopDataPayload::write,
            DesktopDataPayload::new
        );

        private DesktopDataPayload(RegistryFriendlyByteBuf buf) {
            this(buf.readVarInt(), buf.readVarInt(), buf.readVarInt());
        }

        private void write(RegistryFriendlyByteBuf buf) {
            buf.writeVarInt(this.sessionId);
            buf.writeVarInt(this.dataSlot);
            buf.writeVarInt(this.value);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record DesktopCarriedPayload(ItemStack carried) implements CustomPacketPayload {
        public static final Type<DesktopCarriedPayload> TYPE = new Type<>(id("desktop_carried"));
        public static final StreamCodec<RegistryFriendlyByteBuf, DesktopCarriedPayload> CODEC = CustomPacketPayload.codec(
            DesktopCarriedPayload::write,
            DesktopCarriedPayload::new
        );

        private DesktopCarriedPayload(RegistryFriendlyByteBuf buf) {
            this(ItemStack.OPTIONAL_STREAM_CODEC.decode(buf));
        }

        private void write(RegistryFriendlyByteBuf buf) {
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, this.carried);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record DesktopSessionClosedPayload(int sessionId) implements CustomPacketPayload {
        public static final Type<DesktopSessionClosedPayload> TYPE = new Type<>(id("desktop_session_closed"));
        public static final StreamCodec<RegistryFriendlyByteBuf, DesktopSessionClosedPayload> CODEC = CustomPacketPayload.codec(
            DesktopSessionClosedPayload::write,
            DesktopSessionClosedPayload::new
        );

        private DesktopSessionClosedPayload(RegistryFriendlyByteBuf buf) {
            this(buf.readVarInt());
        }

        private void write(RegistryFriendlyByteBuf buf) {
            buf.writeVarInt(this.sessionId);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record DesktopMerchantOffersPayload(
        int sessionId,
        MerchantOffers offers,
        int villagerLevel,
        int villagerXp,
        boolean showProgress,
        boolean canRestock
    ) implements CustomPacketPayload {
        public static final Type<DesktopMerchantOffersPayload> TYPE = new Type<>(id("desktop_merchant_offers"));
        public static final StreamCodec<RegistryFriendlyByteBuf, DesktopMerchantOffersPayload> CODEC = CustomPacketPayload.codec(
            DesktopMerchantOffersPayload::write,
            DesktopMerchantOffersPayload::new
        );

        private DesktopMerchantOffersPayload(RegistryFriendlyByteBuf buf) {
            this(
                buf.readVarInt(),
                MerchantOffers.STREAM_CODEC.decode(buf),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readBoolean(),
                buf.readBoolean()
            );
        }

        private void write(RegistryFriendlyByteBuf buf) {
            buf.writeVarInt(this.sessionId);
            MerchantOffers.STREAM_CODEC.encode(buf, this.offers);
            buf.writeVarInt(this.villagerLevel);
            buf.writeVarInt(this.villagerXp);
            buf.writeBoolean(this.showProgress);
            buf.writeBoolean(this.canRestock);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
