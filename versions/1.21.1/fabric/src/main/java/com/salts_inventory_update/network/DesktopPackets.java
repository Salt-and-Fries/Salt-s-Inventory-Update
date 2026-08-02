package com.salts_inventory_update.network;

import java.util.ArrayList;
import java.util.List;

import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;

import com.salts_inventory_update.platform.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffers;

import com.salts_inventory_update.SaltsInventoryUpdate;
import com.salts_inventory_update.protocol.DesktopProtocol;

public final class DesktopPackets {
    private static final int MAX_IDENTIFIER_LENGTH = 256;
    private static final int MAX_INPUT_NAME_LENGTH = 32;
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
    public static final int LINK_ACTION_LINK = 0;
    public static final int LINK_ACTION_UNLINK = 1;
    public static final int LINK_ACTION_CLEAR_ORIGIN = 2;

    private DesktopPackets() {
    }

    public static void registerPayloadTypes() {
        PayloadTypeRegistry.playC2S().register(DesktopHelloPayload.TYPE, DesktopHelloPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(DesktopModePayload.TYPE, DesktopModePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(DesktopReadyPayload.TYPE, DesktopReadyPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(DesktopClickPayload.TYPE, DesktopClickPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(DesktopQuickMovePayload.TYPE, DesktopQuickMovePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(DesktopButtonPayload.TYPE, DesktopButtonPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(DesktopPlaceRecipePayload.TYPE, DesktopPlaceRecipePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(DesktopJeiTransferPayload.TYPE, DesktopJeiTransferPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(DesktopRenamePayload.TYPE, DesktopRenamePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(DesktopCloseSessionPayload.TYPE, DesktopCloseSessionPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(DesktopSessionPinPayload.TYPE, DesktopSessionPinPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(DesktopSessionVisibilityPayload.TYPE, DesktopSessionVisibilityPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(DesktopLinkSessionsPayload.TYPE, DesktopLinkSessionsPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(DesktopOpenLinkedSourcesPayload.TYPE, DesktopOpenLinkedSourcesPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(DesktopCustomPayload.TYPE, DesktopCustomPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(DesktopCarriedPayload.TYPE, DesktopCarriedPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(InventorySlotPurchasePayload.TYPE, InventorySlotPurchasePayload.CODEC);

        PayloadTypeRegistry.playS2C().register(DesktopHelloAckPayload.TYPE, DesktopHelloAckPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(DesktopOpenSessionPayload.TYPE, DesktopOpenSessionPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(DesktopSlotPayload.TYPE, DesktopSlotPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(DesktopDataPayload.TYPE, DesktopDataPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(DesktopCarriedPayload.TYPE, DesktopCarriedPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(DesktopSessionClosedPayload.TYPE, DesktopSessionClosedPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(DesktopSessionVisibilityPayload.TYPE, DesktopSessionVisibilityPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(DesktopMerchantOffersPayload.TYPE, DesktopMerchantOffersPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(DesktopCustomPayload.TYPE, DesktopCustomPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(DesktopGhostRecipePayload.TYPE, DesktopGhostRecipePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(InventoryExpansionSyncPayload.TYPE, InventoryExpansionSyncPayload.CODEC);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(SaltsInventoryUpdate.MOD_ID, path);
    }

    public static int menuTypeId(MenuType<?> menuType) {
        return BuiltInRegistries.MENU.getId(menuType);
    }

    public static MenuType<?> menuTypeById(int id) {
        return BuiltInRegistries.MENU.byId(id);
    }

    private static void writeItemList(RegistryFriendlyByteBuf buf, List<ItemStack> stacks, int maxSize, String label) {
        if (stacks.size() > maxSize) {
            throw new EncoderException(label + " item list is too large: " + stacks.size());
        }
        buf.writeVarInt(stacks.size());
        for (ItemStack stack : stacks) {
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, stack);
        }
    }

    private static List<ItemStack> readItemList(RegistryFriendlyByteBuf buf, int maxSize, String label) {
        int size = buf.readVarInt();
        if (size < 0 || size > maxSize || size > buf.readableBytes()) {
            throw new DecoderException(label + " item list has an invalid size: " + size);
        }
        List<ItemStack> stacks = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            stacks.add(ItemStack.OPTIONAL_STREAM_CODEC.decode(buf));
        }
        return stacks;
    }

    private static void writeLimitedStringList(RegistryFriendlyByteBuf buf, List<String> values, int maxSize, int maxLength) {
        if (values.size() > maxSize) {
            throw new IllegalArgumentException("Desktop string list is too large: " + values.size());
        }
        buf.writeVarInt(values.size());
        for (String value : values) {
            buf.writeUtf(value, maxLength);
        }
    }

    private static List<String> readLimitedStringList(RegistryFriendlyByteBuf buf, int maxSize, int maxLength) {
        int size = buf.readVarInt();
        if (size < 0 || size > maxSize) {
            throw new IllegalArgumentException("Desktop string list is too large: " + size);
        }
        List<String> values = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            values.add(buf.readUtf(maxLength));
        }
        return values;
    }

    private static List<String> validatedForcedMenuIds(List<String> values) {
        if (values == null || values.size() > DesktopProtocol.MAX_FORCED_MENU_IDS) {
            throw new IllegalArgumentException("Invalid forced menu ID list");
        }
        java.util.LinkedHashSet<String> unique = new java.util.LinkedHashSet<>();
        for (String value : values) {
            if (value == null || value.isBlank() || value.length() > DesktopProtocol.MAX_IDENTIFIER_LENGTH || !unique.add(value)) {
                throw new IllegalArgumentException("Invalid forced menu ID");
            }
        }
        return List.copyOf(unique);
    }

    public record MutationStamp(long connectionNonce, long sessionNonce, int expectedStateId) {
        private MutationStamp(RegistryFriendlyByteBuf buf) {
            this(buf.readLong(), buf.readLong(), buf.readVarInt());
        }

        private void write(RegistryFriendlyByteBuf buf) {
            buf.writeLong(this.connectionNonce);
            buf.writeLong(this.sessionNonce);
            buf.writeVarInt(this.expectedStateId);
        }
    }

    public record InventorySlotPurchasePayload(MutationStamp authorization) implements CustomPacketPayload {
        public static final Type<InventorySlotPurchasePayload> TYPE = new Type<>(id("inventory_slot_purchase"));
        public static final StreamCodec<RegistryFriendlyByteBuf, InventorySlotPurchasePayload> CODEC = CustomPacketPayload.codec(
            InventorySlotPurchasePayload::write,
            InventorySlotPurchasePayload::new
        );

        private InventorySlotPurchasePayload(RegistryFriendlyByteBuf buf) {
            this(new MutationStamp(buf));
        }

        private void write(RegistryFriendlyByteBuf buf) {
            this.authorization.write(buf);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record InventoryExpansionSyncPayload(long connectionNonce, long playerMenuNonce, int slotCount, List<ItemStack> items) implements CustomPacketPayload {
        public static final Type<InventoryExpansionSyncPayload> TYPE = new Type<>(id("inventory_expansion_sync"));
        public static final StreamCodec<RegistryFriendlyByteBuf, InventoryExpansionSyncPayload> CODEC = CustomPacketPayload.codec(
            InventoryExpansionSyncPayload::write,
            InventoryExpansionSyncPayload::new
        );

        private InventoryExpansionSyncPayload(RegistryFriendlyByteBuf buf) {
            this(buf.readLong(), buf.readLong(), buf.readVarInt(), readItemList(buf, DesktopProtocol.MAX_EXPANSION_SLOTS, "Inventory expansion"));
        }

        public InventoryExpansionSyncPayload {
            if (connectionNonce == 0L || playerMenuNonce == 0L
                || slotCount < 0 || slotCount > DesktopProtocol.MAX_EXPANSION_SLOTS || items.size() != slotCount) {
                throw new IllegalArgumentException("Invalid inventory expansion snapshot: slots=" + slotCount + ", items=" + items.size());
            }
            items = List.copyOf(items);
        }

        private void write(RegistryFriendlyByteBuf buf) {
            buf.writeLong(this.connectionNonce);
            buf.writeLong(this.playerMenuNonce);
            buf.writeVarInt(this.slotCount);
            writeItemList(buf, this.items, DesktopProtocol.MAX_EXPANSION_SLOTS, "Inventory expansion");
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
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

    public record DesktopClickPayload(MutationStamp authorization, int debugId, int sessionId, int slotIndex, int button, String inputName, ItemStack clientCarried) implements CustomPacketPayload {
        public static final Type<DesktopClickPayload> TYPE = new Type<>(id("desktop_click"));
        public static final StreamCodec<RegistryFriendlyByteBuf, DesktopClickPayload> CODEC = CustomPacketPayload.codec(
            DesktopClickPayload::write,
            DesktopClickPayload::new
        );

        private DesktopClickPayload(RegistryFriendlyByteBuf buf) {
            this(
                new MutationStamp(buf),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readUtf(MAX_INPUT_NAME_LENGTH),
                ItemStack.OPTIONAL_STREAM_CODEC.decode(buf)
            );
        }

        private void write(RegistryFriendlyByteBuf buf) {
            this.authorization.write(buf);
            buf.writeVarInt(this.debugId);
            buf.writeVarInt(this.sessionId);
            buf.writeVarInt(this.slotIndex);
            buf.writeVarInt(this.button);
            buf.writeUtf(this.inputName, MAX_INPUT_NAME_LENGTH);
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, this.clientCarried);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record DesktopQuickMovePayload(MutationStamp sourceAuthorization, MutationStamp targetAuthorization, int sourceSessionId, int sourceSlotIndex, int targetKind, int targetSessionId) implements CustomPacketPayload {
        public static final Type<DesktopQuickMovePayload> TYPE = new Type<>(id("desktop_quick_move"));
        public static final StreamCodec<RegistryFriendlyByteBuf, DesktopQuickMovePayload> CODEC = CustomPacketPayload.codec(
            DesktopQuickMovePayload::write,
            DesktopQuickMovePayload::new
        );

        private DesktopQuickMovePayload(RegistryFriendlyByteBuf buf) {
            this(new MutationStamp(buf), new MutationStamp(buf), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt());
        }

        private void write(RegistryFriendlyByteBuf buf) {
            this.sourceAuthorization.write(buf);
            this.targetAuthorization.write(buf);
            buf.writeVarInt(this.sourceSessionId);
            buf.writeVarInt(this.sourceSlotIndex);
            buf.writeVarInt(this.targetKind);
            buf.writeVarInt(this.targetSessionId);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record DesktopHelloPayload(int protocolVersion, long clientNonce, long capabilities, boolean uiEnabled, List<String> forcedMenuIds) implements CustomPacketPayload {
        public static final Type<DesktopHelloPayload> TYPE = new Type<>(id("desktop_hello"));
        public static final StreamCodec<RegistryFriendlyByteBuf, DesktopHelloPayload> CODEC = CustomPacketPayload.codec(
            DesktopHelloPayload::write,
            DesktopHelloPayload::new
        );

        private DesktopHelloPayload(RegistryFriendlyByteBuf buf) {
            this(buf.readVarInt(), buf.readLong(), buf.readVarLong(), buf.readBoolean(), readLimitedStringList(buf, DesktopProtocol.MAX_FORCED_MENU_IDS, DesktopProtocol.MAX_IDENTIFIER_LENGTH));
        }

        public DesktopHelloPayload {
            forcedMenuIds = validatedForcedMenuIds(forcedMenuIds);
        }

        private void write(RegistryFriendlyByteBuf buf) {
            buf.writeVarInt(this.protocolVersion);
            buf.writeLong(this.clientNonce);
            buf.writeVarLong(this.capabilities);
            buf.writeBoolean(this.uiEnabled);
            writeLimitedStringList(buf, this.forcedMenuIds, DesktopProtocol.MAX_FORCED_MENU_IDS, DesktopProtocol.MAX_IDENTIFIER_LENGTH);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record DesktopHelloAckPayload(int protocolVersion, long echoedClientNonce, long connectionNonce, long capabilities, boolean uiEnabled) implements CustomPacketPayload {
        public static final Type<DesktopHelloAckPayload> TYPE = new Type<>(id("desktop_hello_ack"));
        public static final StreamCodec<RegistryFriendlyByteBuf, DesktopHelloAckPayload> CODEC = CustomPacketPayload.codec(
            DesktopHelloAckPayload::write,
            DesktopHelloAckPayload::new
        );

        private DesktopHelloAckPayload(RegistryFriendlyByteBuf buf) {
            this(buf.readVarInt(), buf.readLong(), buf.readLong(), buf.readVarLong(), buf.readBoolean());
        }

        private void write(RegistryFriendlyByteBuf buf) {
            buf.writeVarInt(this.protocolVersion);
            buf.writeLong(this.echoedClientNonce);
            buf.writeLong(this.connectionNonce);
            buf.writeVarLong(this.capabilities);
            buf.writeBoolean(this.uiEnabled);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record DesktopModePayload(long connectionNonce, long sequence, boolean uiEnabled, List<String> forcedMenuIds) implements CustomPacketPayload {
        public static final Type<DesktopModePayload> TYPE = new Type<>(id("desktop_mode"));
        public static final StreamCodec<RegistryFriendlyByteBuf, DesktopModePayload> CODEC = CustomPacketPayload.codec(
            DesktopModePayload::write,
            DesktopModePayload::new
        );

        private DesktopModePayload(RegistryFriendlyByteBuf buf) {
            this(buf.readLong(), buf.readVarLong(), buf.readBoolean(), readLimitedStringList(buf, DesktopProtocol.MAX_FORCED_MENU_IDS, DesktopProtocol.MAX_IDENTIFIER_LENGTH));
        }

        public DesktopModePayload {
            forcedMenuIds = validatedForcedMenuIds(forcedMenuIds);
        }

        private void write(RegistryFriendlyByteBuf buf) {
            buf.writeLong(this.connectionNonce);
            buf.writeVarLong(this.sequence);
            buf.writeBoolean(this.uiEnabled);
            writeLimitedStringList(buf, this.forcedMenuIds, DesktopProtocol.MAX_FORCED_MENU_IDS, DesktopProtocol.MAX_IDENTIFIER_LENGTH);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record DesktopButtonPayload(MutationStamp authorization, int sessionId, int buttonId) implements CustomPacketPayload {
        public static final Type<DesktopButtonPayload> TYPE = new Type<>(id("desktop_button"));
        public static final StreamCodec<RegistryFriendlyByteBuf, DesktopButtonPayload> CODEC = CustomPacketPayload.codec(
            DesktopButtonPayload::write,
            DesktopButtonPayload::new
        );

        private DesktopButtonPayload(RegistryFriendlyByteBuf buf) {
            this(new MutationStamp(buf), buf.readVarInt(), buf.readVarInt());
        }

        private void write(RegistryFriendlyByteBuf buf) {
            this.authorization.write(buf);
            buf.writeVarInt(this.sessionId);
            buf.writeVarInt(this.buttonId);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record DesktopPlaceRecipePayload(MutationStamp authorization, int sessionId, ResourceLocation recipeId, boolean useMaxItems) implements CustomPacketPayload {
        public static final Type<DesktopPlaceRecipePayload> TYPE = new Type<>(id("desktop_place_recipe"));
        public static final StreamCodec<RegistryFriendlyByteBuf, DesktopPlaceRecipePayload> CODEC = CustomPacketPayload.codec(
            DesktopPlaceRecipePayload::write,
            DesktopPlaceRecipePayload::new
        );

        private DesktopPlaceRecipePayload(RegistryFriendlyByteBuf buf) {
            this(new MutationStamp(buf), buf.readVarInt(), buf.readResourceLocation(), buf.readBoolean());
        }

        private void write(RegistryFriendlyByteBuf buf) {
            this.authorization.write(buf);
            buf.writeVarInt(this.sessionId);
            buf.writeResourceLocation(this.recipeId);
            buf.writeBoolean(this.useMaxItems);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record DesktopJeiTransferPayload(MutationStamp authorization, int targetSessionId, ResourceLocation recipeId, boolean maxTransfer) implements CustomPacketPayload {
        public static final Type<DesktopJeiTransferPayload> TYPE = new Type<>(id("desktop_jei_transfer"));
        public static final StreamCodec<RegistryFriendlyByteBuf, DesktopJeiTransferPayload> CODEC = CustomPacketPayload.codec(
            DesktopJeiTransferPayload::write,
            DesktopJeiTransferPayload::new
        );

        private DesktopJeiTransferPayload(RegistryFriendlyByteBuf buf) {
            this(
                new MutationStamp(buf),
                buf.readVarInt(),
                buf.readResourceLocation(),
                buf.readBoolean()
            );
        }

        private void write(RegistryFriendlyByteBuf buf) {
            this.authorization.write(buf);
            buf.writeVarInt(this.targetSessionId);
            buf.writeResourceLocation(this.recipeId);
            buf.writeBoolean(this.maxTransfer);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record DesktopRenamePayload(MutationStamp authorization, int sessionId, String name) implements CustomPacketPayload {
        public static final Type<DesktopRenamePayload> TYPE = new Type<>(id("desktop_rename"));
        public static final StreamCodec<RegistryFriendlyByteBuf, DesktopRenamePayload> CODEC = CustomPacketPayload.codec(
            DesktopRenamePayload::write,
            DesktopRenamePayload::new
        );

        private DesktopRenamePayload(RegistryFriendlyByteBuf buf) {
            this(new MutationStamp(buf), buf.readVarInt(), buf.readUtf(50));
        }

        private void write(RegistryFriendlyByteBuf buf) {
            this.authorization.write(buf);
            buf.writeVarInt(this.sessionId);
            buf.writeUtf(this.name, 50);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record DesktopCustomPayload(MutationStamp authorization, int sessionId, ResourceLocation channel, byte[] data) implements CustomPacketPayload {
        public static final Type<DesktopCustomPayload> TYPE = new Type<>(id("desktop_custom"));
        public static final StreamCodec<RegistryFriendlyByteBuf, DesktopCustomPayload> CODEC = CustomPacketPayload.codec(
            DesktopCustomPayload::write,
            DesktopCustomPayload::new
        );

        private DesktopCustomPayload(RegistryFriendlyByteBuf buf) {
            this(
                new MutationStamp(buf),
                buf.readVarInt(),
                ResourceLocation.parse(buf.readUtf(MAX_IDENTIFIER_LENGTH)),
                buf.readByteArray(DesktopProtocol.MAX_CUSTOM_DATA_BYTES)
            );
        }

        public DesktopCustomPayload(int sessionId, ResourceLocation channel, byte[] data) {
            this(new MutationStamp(0L, 0L, 0), sessionId, channel, data);
        }

        public DesktopCustomPayload {
            if (channel == null || channel.toString().length() > MAX_IDENTIFIER_LENGTH) {
                throw new IllegalArgumentException("Invalid desktop custom payload channel");
            }
            if (data == null || data.length > DesktopProtocol.MAX_CUSTOM_DATA_BYTES) {
                throw new IllegalArgumentException("Desktop custom payload is too large: " + (data == null ? -1 : data.length));
            }
            data = data.clone();
        }

        @Override
        public byte[] data() {
            return this.data.clone();
        }

        private void write(RegistryFriendlyByteBuf buf) {
            this.authorization.write(buf);
            buf.writeVarInt(this.sessionId);
            buf.writeUtf(this.channel.toString(), MAX_IDENTIFIER_LENGTH);
            buf.writeByteArray(this.data);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record DesktopCloseSessionPayload(MutationStamp authorization, int sessionId) implements CustomPacketPayload {
        public static final Type<DesktopCloseSessionPayload> TYPE = new Type<>(id("desktop_close_session"));
        public static final StreamCodec<RegistryFriendlyByteBuf, DesktopCloseSessionPayload> CODEC = CustomPacketPayload.codec(
            DesktopCloseSessionPayload::write,
            DesktopCloseSessionPayload::new
        );

        private DesktopCloseSessionPayload(RegistryFriendlyByteBuf buf) {
            this(new MutationStamp(buf), buf.readVarInt());
        }

        private void write(RegistryFriendlyByteBuf buf) {
            this.authorization.write(buf);
            buf.writeVarInt(this.sessionId);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record DesktopSessionPinPayload(MutationStamp authorization, int sessionId, int pinMode) implements CustomPacketPayload {
        public static final Type<DesktopSessionPinPayload> TYPE = new Type<>(id("desktop_session_pin"));
        public static final StreamCodec<RegistryFriendlyByteBuf, DesktopSessionPinPayload> CODEC = CustomPacketPayload.codec(
            DesktopSessionPinPayload::write,
            DesktopSessionPinPayload::new
        );

        private DesktopSessionPinPayload(RegistryFriendlyByteBuf buf) {
            this(new MutationStamp(buf), buf.readVarInt(), buf.readVarInt());
        }

        private void write(RegistryFriendlyByteBuf buf) {
            this.authorization.write(buf);
            buf.writeVarInt(this.sessionId);
            buf.writeVarInt(this.pinMode);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record DesktopSessionVisibilityPayload(MutationStamp authorization, int sessionId, boolean visible) implements CustomPacketPayload {
        public static final Type<DesktopSessionVisibilityPayload> TYPE = new Type<>(id("desktop_session_visibility"));
        public static final StreamCodec<RegistryFriendlyByteBuf, DesktopSessionVisibilityPayload> CODEC = CustomPacketPayload.codec(
            DesktopSessionVisibilityPayload::write,
            DesktopSessionVisibilityPayload::new
        );

        private DesktopSessionVisibilityPayload(RegistryFriendlyByteBuf buf) {
            this(new MutationStamp(buf), buf.readVarInt(), buf.readBoolean());
        }

        public DesktopSessionVisibilityPayload(int sessionId, boolean visible) {
            this(new MutationStamp(0L, 0L, 0), sessionId, visible);
        }

        private void write(RegistryFriendlyByteBuf buf) {
            this.authorization.write(buf);
            buf.writeVarInt(this.sessionId);
            buf.writeBoolean(this.visible);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record DesktopLinkSessionsPayload(MutationStamp originAuthorization, MutationStamp targetAuthorization, int action) implements CustomPacketPayload {
        public static final Type<DesktopLinkSessionsPayload> TYPE = new Type<>(id("desktop_link_sessions"));
        public static final StreamCodec<RegistryFriendlyByteBuf, DesktopLinkSessionsPayload> CODEC = CustomPacketPayload.codec(
            DesktopLinkSessionsPayload::write,
            DesktopLinkSessionsPayload::new
        );

        private DesktopLinkSessionsPayload(RegistryFriendlyByteBuf buf) {
            this(new MutationStamp(buf), new MutationStamp(buf), buf.readVarInt());
        }

        public DesktopLinkSessionsPayload {
            if (action < LINK_ACTION_LINK || action > LINK_ACTION_CLEAR_ORIGIN) {
                throw new IllegalArgumentException("Invalid desktop link action: " + action);
            }
        }

        private void write(RegistryFriendlyByteBuf buf) {
            this.originAuthorization.write(buf);
            this.targetAuthorization.write(buf);
            buf.writeVarInt(this.action);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record DesktopOpenLinkedSourcesPayload(MutationStamp authorization) implements CustomPacketPayload {
        public static final Type<DesktopOpenLinkedSourcesPayload> TYPE = new Type<>(id("desktop_open_linked_sources"));
        public static final StreamCodec<RegistryFriendlyByteBuf, DesktopOpenLinkedSourcesPayload> CODEC = CustomPacketPayload.codec(
            DesktopOpenLinkedSourcesPayload::write,
            DesktopOpenLinkedSourcesPayload::new
        );

        private DesktopOpenLinkedSourcesPayload(RegistryFriendlyByteBuf buf) {
            this(new MutationStamp(buf));
        }

        private void write(RegistryFriendlyByteBuf buf) {
            this.authorization.write(buf);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record DesktopOpenSessionPayload(
        long connectionNonce,
        int sessionId,
        long sessionNonce,
        int menuTypeId,
        int specialKind,
        int entityId,
        int columns,
        int stateId,
        boolean visible,
        boolean transferSupported,
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
                buf.readLong(),
                buf.readVarInt(),
                buf.readLong(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readBoolean(),
                buf.readBoolean(),
                buf.readUtf(DesktopProtocol.MAX_SOURCE_TEXT_LENGTH),
                ComponentSerialization.STREAM_CODEC.decode(buf),
                readItemList(buf, DesktopProtocol.MAX_OPEN_SESSION_ITEMS, "Desktop session"),
                ItemStack.OPTIONAL_STREAM_CODEC.decode(buf),
                buf.readVarIntArray(DesktopProtocol.MAX_MENU_DATA_VALUES)
            );
        }

        public DesktopOpenSessionPayload {
            if (sourceKey == null || sourceKey.length() > DesktopProtocol.MAX_SOURCE_TEXT_LENGTH
                || items.size() > DesktopProtocol.MAX_OPEN_SESSION_ITEMS
                || data.length > DesktopProtocol.MAX_MENU_DATA_VALUES) {
                throw new IllegalArgumentException("Desktop session snapshot is too large: items=" + items.size() + ", data=" + data.length);
            }
            items = List.copyOf(items);
            data = data.clone();
        }

        @Override
        public int[] data() {
            return this.data.clone();
        }

        private void write(RegistryFriendlyByteBuf buf) {
            buf.writeLong(this.connectionNonce);
            buf.writeVarInt(this.sessionId);
            buf.writeLong(this.sessionNonce);
            buf.writeVarInt(this.menuTypeId);
            buf.writeVarInt(this.specialKind);
            buf.writeVarInt(this.entityId);
            buf.writeVarInt(this.columns);
            buf.writeVarInt(this.stateId);
            buf.writeBoolean(this.visible);
            buf.writeBoolean(this.transferSupported);
            buf.writeUtf(this.sourceKey, DesktopProtocol.MAX_SOURCE_TEXT_LENGTH);
            ComponentSerialization.STREAM_CODEC.encode(buf, this.title);
            writeItemList(buf, this.items, DesktopProtocol.MAX_OPEN_SESSION_ITEMS, "Desktop session");
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, this.carried);
            buf.writeVarIntArray(this.data);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record DesktopSlotPayload(MutationStamp authorization, int sessionId, int slotIndex, int stateId, ItemStack stack) implements CustomPacketPayload {
        public static final Type<DesktopSlotPayload> TYPE = new Type<>(id("desktop_slot"));
        public static final StreamCodec<RegistryFriendlyByteBuf, DesktopSlotPayload> CODEC = CustomPacketPayload.codec(
            DesktopSlotPayload::write,
            DesktopSlotPayload::new
        );

        private DesktopSlotPayload(RegistryFriendlyByteBuf buf) {
            this(
                new MutationStamp(buf),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt(),
                ItemStack.OPTIONAL_STREAM_CODEC.decode(buf)
            );
        }

        private void write(RegistryFriendlyByteBuf buf) {
            this.authorization.write(buf);
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

    public record DesktopDataPayload(MutationStamp authorization, int sessionId, int dataSlot, int value) implements CustomPacketPayload {
        public static final Type<DesktopDataPayload> TYPE = new Type<>(id("desktop_data"));
        public static final StreamCodec<RegistryFriendlyByteBuf, DesktopDataPayload> CODEC = CustomPacketPayload.codec(
            DesktopDataPayload::write,
            DesktopDataPayload::new
        );

        private DesktopDataPayload(RegistryFriendlyByteBuf buf) {
            this(new MutationStamp(buf), buf.readVarInt(), buf.readVarInt(), buf.readVarInt());
        }

        private void write(RegistryFriendlyByteBuf buf) {
            this.authorization.write(buf);
            buf.writeVarInt(this.sessionId);
            buf.writeVarInt(this.dataSlot);
            buf.writeVarInt(this.value);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record DesktopCarriedPayload(MutationStamp authorization, ItemStack carried) implements CustomPacketPayload {
        public static final Type<DesktopCarriedPayload> TYPE = new Type<>(id("desktop_carried"));
        public static final StreamCodec<RegistryFriendlyByteBuf, DesktopCarriedPayload> CODEC = CustomPacketPayload.codec(
            DesktopCarriedPayload::write,
            DesktopCarriedPayload::new
        );

        private DesktopCarriedPayload(RegistryFriendlyByteBuf buf) {
            this(new MutationStamp(buf), ItemStack.OPTIONAL_STREAM_CODEC.decode(buf));
        }

        public DesktopCarriedPayload(ItemStack carried) {
            this(new MutationStamp(0L, 0L, 0), carried);
        }

        private void write(RegistryFriendlyByteBuf buf) {
            this.authorization.write(buf);
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, this.carried);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record DesktopGhostRecipePayload(MutationStamp authorization, int sessionId, ResourceLocation recipeId) implements CustomPacketPayload {
        public static final Type<DesktopGhostRecipePayload> TYPE = new Type<>(id("desktop_ghost_recipe"));
        public static final StreamCodec<RegistryFriendlyByteBuf, DesktopGhostRecipePayload> CODEC = CustomPacketPayload.codec(
            DesktopGhostRecipePayload::write,
            DesktopGhostRecipePayload::new
        );

        private DesktopGhostRecipePayload(RegistryFriendlyByteBuf buf) {
            this(new MutationStamp(buf), buf.readVarInt(), buf.readResourceLocation());
        }

        private void write(RegistryFriendlyByteBuf buf) {
            this.authorization.write(buf);
            buf.writeVarInt(this.sessionId);
            buf.writeResourceLocation(this.recipeId);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record DesktopSessionClosedPayload(MutationStamp authorization, int sessionId) implements CustomPacketPayload {
        public static final Type<DesktopSessionClosedPayload> TYPE = new Type<>(id("desktop_session_closed"));
        public static final StreamCodec<RegistryFriendlyByteBuf, DesktopSessionClosedPayload> CODEC = CustomPacketPayload.codec(
            DesktopSessionClosedPayload::write,
            DesktopSessionClosedPayload::new
        );

        private DesktopSessionClosedPayload(RegistryFriendlyByteBuf buf) {
            this(new MutationStamp(buf), buf.readVarInt());
        }

        private void write(RegistryFriendlyByteBuf buf) {
            this.authorization.write(buf);
            buf.writeVarInt(this.sessionId);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record DesktopMerchantOffersPayload(
        MutationStamp authorization,
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
                new MutationStamp(buf),
                buf.readVarInt(),
                MerchantOffers.STREAM_CODEC.decode(buf),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readBoolean(),
                buf.readBoolean()
            );
        }

        private void write(RegistryFriendlyByteBuf buf) {
            this.authorization.write(buf);
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
