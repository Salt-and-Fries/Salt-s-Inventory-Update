package com.salts_inventory_update.network;

import java.util.ArrayList;
import java.util.List;

import com.salts_inventory_update.platform.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.RecipeDisplayId;
import net.minecraft.world.item.trading.MerchantOffers;

import com.salts_inventory_update.SaltsInventoryUpdate;

public final class DesktopPackets {
    private static final int CUSTOM_PAYLOAD_MAX_BYTES = 32 * 1024;
    public static final int PLAYER_MENU_SESSION = 0;
    public static final int SPECIAL_GENERIC = 0;
    public static final int SPECIAL_HORSE = 1;
    public static final int SPECIAL_NAUTILUS = 2;
    public static final int SPECIAL_CAMEL = 3;
    public static final int SPECIAL_LLAMA = 4;
    public static final int QUICK_TARGET_DEFAULT = 0;
    public static final int QUICK_TARGET_SESSION = 1;
    public static final int QUICK_TARGET_HOTBAR = 2;
    public static final int PIN_MODE_UNPINNED = 0;
    public static final int PIN_MODE_PINNED = 1;
    public static final int PIN_MODE_GHOST_PINNED = 2;
    private static final int JEI_TRANSFER_MAX_RECIPE_SLOTS = 128;
    private static final int JEI_TRANSFER_MAX_REQUIREMENTS = 128;
    private static final int JEI_TRANSFER_MAX_ALTERNATIVES = 128;
    private static final int LINKED_SOURCE_MAX_KEYS = 64;
    private static final int LINKED_SOURCE_MAX_KEY_LENGTH = 512;

    private DesktopPackets() {
    }

    public static void registerPayloadTypes() {
        PayloadTypeRegistry.serverboundPlay().register(DesktopReadyPayload.TYPE, DesktopReadyPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(DesktopClickPayload.TYPE, DesktopClickPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(DesktopQuickMovePayload.TYPE, DesktopQuickMovePayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(DesktopButtonPayload.TYPE, DesktopButtonPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(DesktopPlaceRecipePayload.TYPE, DesktopPlaceRecipePayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(DesktopJeiTransferPayload.TYPE, DesktopJeiTransferPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(DesktopRenamePayload.TYPE, DesktopRenamePayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(DesktopCloseSessionPayload.TYPE, DesktopCloseSessionPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(DesktopSessionPinPayload.TYPE, DesktopSessionPinPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(DesktopSessionVisibilityPayload.TYPE, DesktopSessionVisibilityPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(DesktopOpenLinkedSourcesPayload.TYPE, DesktopOpenLinkedSourcesPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(DesktopCustomPayload.TYPE, DesktopCustomPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(DesktopCarriedPayload.TYPE, DesktopCarriedPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(InventorySlotPurchasePayload.TYPE, InventorySlotPurchasePayload.CODEC);

        PayloadTypeRegistry.clientboundPlay().register(DesktopOpenSessionPayload.TYPE, DesktopOpenSessionPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(DesktopSlotPayload.TYPE, DesktopSlotPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(DesktopDataPayload.TYPE, DesktopDataPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(DesktopCarriedPayload.TYPE, DesktopCarriedPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(DesktopSessionClosedPayload.TYPE, DesktopSessionClosedPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(DesktopSessionVisibilityPayload.TYPE, DesktopSessionVisibilityPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(DesktopMerchantOffersPayload.TYPE, DesktopMerchantOffersPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(DesktopCustomPayload.TYPE, DesktopCustomPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(DesktopGhostRecipePayload.TYPE, DesktopGhostRecipePayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(InventoryExpansionSyncPayload.TYPE, InventoryExpansionSyncPayload.CODEC);
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

    private static void writeIntList(RegistryFriendlyByteBuf buf, List<Integer> values) {
        buf.writeVarInt(values.size());
        for (int value : values) {
            buf.writeVarInt(value);
        }
    }

    private static List<Integer> readIntList(RegistryFriendlyByteBuf buf, int maxSize) {
        int size = buf.readVarInt();
        if (size < 0 || size > maxSize) {
            throw new IllegalArgumentException("Desktop int list is too large: " + size);
        }
        List<Integer> values = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            values.add(buf.readVarInt());
        }
        return values;
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

    private static void writeLimitedItemList(RegistryFriendlyByteBuf buf, List<ItemStack> stacks) {
        if (stacks.size() > JEI_TRANSFER_MAX_ALTERNATIVES) {
            throw new IllegalArgumentException("Desktop JEI transfer alternatives are too large: " + stacks.size());
        }
        writeItemList(buf, stacks);
    }

    private static List<ItemStack> readLimitedItemList(RegistryFriendlyByteBuf buf) {
        int size = buf.readVarInt();
        if (size < 0 || size > JEI_TRANSFER_MAX_ALTERNATIVES) {
            throw new IllegalArgumentException("Desktop JEI transfer alternatives are too large: " + size);
        }
        List<ItemStack> stacks = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            stacks.add(ItemStack.OPTIONAL_STREAM_CODEC.decode(buf));
        }
        return stacks;
    }

    public record InventorySlotPurchasePayload() implements CustomPacketPayload {
        public static final Type<InventorySlotPurchasePayload> TYPE = new Type<>(id("inventory_slot_purchase"));
        public static final StreamCodec<RegistryFriendlyByteBuf, InventorySlotPurchasePayload> CODEC = CustomPacketPayload.codec(
            InventorySlotPurchasePayload::write,
            InventorySlotPurchasePayload::new
        );

        private InventorySlotPurchasePayload(RegistryFriendlyByteBuf buf) {
            this();
        }

        private void write(RegistryFriendlyByteBuf buf) {
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record InventoryExpansionSyncPayload(int slotCount, List<ItemStack> items) implements CustomPacketPayload {
        public static final Type<InventoryExpansionSyncPayload> TYPE = new Type<>(id("inventory_expansion_sync"));
        public static final StreamCodec<RegistryFriendlyByteBuf, InventoryExpansionSyncPayload> CODEC = CustomPacketPayload.codec(
            InventoryExpansionSyncPayload::write,
            InventoryExpansionSyncPayload::new
        );

        private InventoryExpansionSyncPayload(RegistryFriendlyByteBuf buf) {
            this(buf.readVarInt(), readItemList(buf));
        }

        private void write(RegistryFriendlyByteBuf buf) {
            buf.writeVarInt(this.slotCount);
            writeItemList(buf, this.items);
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

    public record DesktopClickPayload(int debugId, int sessionId, int slotIndex, int button, String inputName, ItemStack clientCarried) implements CustomPacketPayload {
        public static final Type<DesktopClickPayload> TYPE = new Type<>(id("desktop_click"));
        public static final StreamCodec<RegistryFriendlyByteBuf, DesktopClickPayload> CODEC = CustomPacketPayload.codec(
            DesktopClickPayload::write,
            DesktopClickPayload::new
        );

        private DesktopClickPayload(RegistryFriendlyByteBuf buf) {
            this(
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readUtf(),
                ItemStack.OPTIONAL_STREAM_CODEC.decode(buf)
            );
        }

        private void write(RegistryFriendlyByteBuf buf) {
            buf.writeVarInt(this.debugId);
            buf.writeVarInt(this.sessionId);
            buf.writeVarInt(this.slotIndex);
            buf.writeVarInt(this.button);
            buf.writeUtf(this.inputName);
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, this.clientCarried);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record DesktopQuickMovePayload(int sourceSessionId, int sourceSlotIndex, int targetKind, int targetSessionId) implements CustomPacketPayload {
        public static final Type<DesktopQuickMovePayload> TYPE = new Type<>(id("desktop_quick_move"));
        public static final StreamCodec<RegistryFriendlyByteBuf, DesktopQuickMovePayload> CODEC = CustomPacketPayload.codec(
            DesktopQuickMovePayload::write,
            DesktopQuickMovePayload::new
        );

        private DesktopQuickMovePayload(RegistryFriendlyByteBuf buf) {
            this(buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt());
        }

        private void write(RegistryFriendlyByteBuf buf) {
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

    public record DesktopButtonPayload(int sessionId, int buttonId) implements CustomPacketPayload {
        public static final Type<DesktopButtonPayload> TYPE = new Type<>(id("desktop_button"));
        public static final StreamCodec<RegistryFriendlyByteBuf, DesktopButtonPayload> CODEC = CustomPacketPayload.codec(
            DesktopButtonPayload::write,
            DesktopButtonPayload::new
        );

        private DesktopButtonPayload(RegistryFriendlyByteBuf buf) {
            this(buf.readVarInt(), buf.readVarInt());
        }

        private void write(RegistryFriendlyByteBuf buf) {
            buf.writeVarInt(this.sessionId);
            buf.writeVarInt(this.buttonId);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record DesktopPlaceRecipePayload(int sessionId, RecipeDisplayId recipeId, boolean useMaxItems) implements CustomPacketPayload {
        public static final Type<DesktopPlaceRecipePayload> TYPE = new Type<>(id("desktop_place_recipe"));
        public static final StreamCodec<RegistryFriendlyByteBuf, DesktopPlaceRecipePayload> CODEC = CustomPacketPayload.codec(
            DesktopPlaceRecipePayload::write,
            DesktopPlaceRecipePayload::new
        );

        private DesktopPlaceRecipePayload(RegistryFriendlyByteBuf buf) {
            this(buf.readVarInt(), RecipeDisplayId.STREAM_CODEC.decode(buf), buf.readBoolean());
        }

        private void write(RegistryFriendlyByteBuf buf) {
            buf.writeVarInt(this.sessionId);
            RecipeDisplayId.STREAM_CODEC.encode(buf, this.recipeId);
            buf.writeBoolean(this.useMaxItems);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record DesktopJeiTransferRequirement(int inputIndex, int targetSlotId, List<ItemStack> alternatives) {
        public DesktopJeiTransferRequirement {
            alternatives = List.copyOf(alternatives);
        }

        private DesktopJeiTransferRequirement(RegistryFriendlyByteBuf buf) {
            this(buf.readVarInt(), buf.readVarInt(), readLimitedItemList(buf));
        }

        private void write(RegistryFriendlyByteBuf buf) {
            buf.writeVarInt(this.inputIndex);
            buf.writeVarInt(this.targetSlotId);
            writeLimitedItemList(buf, this.alternatives);
        }
    }

    public record DesktopJeiTransferPayload(int targetSessionId, List<Integer> recipeSlotIds, List<DesktopJeiTransferRequirement> requirements, boolean maxTransfer) implements CustomPacketPayload {
        public static final Type<DesktopJeiTransferPayload> TYPE = new Type<>(id("desktop_jei_transfer"));
        public static final StreamCodec<RegistryFriendlyByteBuf, DesktopJeiTransferPayload> CODEC = CustomPacketPayload.codec(
            DesktopJeiTransferPayload::write,
            DesktopJeiTransferPayload::new
        );

        public DesktopJeiTransferPayload {
            recipeSlotIds = List.copyOf(recipeSlotIds);
            requirements = List.copyOf(requirements);
            if (recipeSlotIds.size() > JEI_TRANSFER_MAX_RECIPE_SLOTS) {
                throw new IllegalArgumentException("Desktop JEI transfer recipe slots are too large: " + recipeSlotIds.size());
            }
            if (requirements.size() > JEI_TRANSFER_MAX_REQUIREMENTS) {
                throw new IllegalArgumentException("Desktop JEI transfer requirements are too large: " + requirements.size());
            }
        }

        private DesktopJeiTransferPayload(RegistryFriendlyByteBuf buf) {
            this(
                buf.readVarInt(),
                readIntList(buf, JEI_TRANSFER_MAX_RECIPE_SLOTS),
                readJeiTransferRequirements(buf),
                buf.readBoolean()
            );
        }

        private void write(RegistryFriendlyByteBuf buf) {
            buf.writeVarInt(this.targetSessionId);
            writeIntList(buf, this.recipeSlotIds);
            buf.writeVarInt(this.requirements.size());
            for (DesktopJeiTransferRequirement requirement : this.requirements) {
                requirement.write(buf);
            }
            buf.writeBoolean(this.maxTransfer);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    private static List<DesktopJeiTransferRequirement> readJeiTransferRequirements(RegistryFriendlyByteBuf buf) {
        int size = buf.readVarInt();
        if (size < 0 || size > JEI_TRANSFER_MAX_REQUIREMENTS) {
            throw new IllegalArgumentException("Desktop JEI transfer requirements are too large: " + size);
        }
        List<DesktopJeiTransferRequirement> requirements = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            requirements.add(new DesktopJeiTransferRequirement(buf));
        }
        return requirements;
    }

    public record DesktopRenamePayload(int sessionId, String name) implements CustomPacketPayload {
        public static final Type<DesktopRenamePayload> TYPE = new Type<>(id("desktop_rename"));
        public static final StreamCodec<RegistryFriendlyByteBuf, DesktopRenamePayload> CODEC = CustomPacketPayload.codec(
            DesktopRenamePayload::write,
            DesktopRenamePayload::new
        );

        private DesktopRenamePayload(RegistryFriendlyByteBuf buf) {
            this(buf.readVarInt(), buf.readUtf(50));
        }

        private void write(RegistryFriendlyByteBuf buf) {
            buf.writeVarInt(this.sessionId);
            buf.writeUtf(this.name, 50);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record DesktopCustomPayload(int sessionId, Identifier channel, byte[] data) implements CustomPacketPayload {
        public static final Type<DesktopCustomPayload> TYPE = new Type<>(id("desktop_custom"));
        public static final StreamCodec<RegistryFriendlyByteBuf, DesktopCustomPayload> CODEC = CustomPacketPayload.codec(
            DesktopCustomPayload::write,
            DesktopCustomPayload::new
        );

        private DesktopCustomPayload(RegistryFriendlyByteBuf buf) {
            this(
                buf.readVarInt(),
                Identifier.parse(buf.readUtf()),
                buf.readByteArray(CUSTOM_PAYLOAD_MAX_BYTES)
            );
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

        private void write(RegistryFriendlyByteBuf buf) {
            buf.writeVarInt(this.sessionId);
            buf.writeUtf(this.channel.toString());
            buf.writeByteArray(this.data);
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

    public record DesktopSessionPinPayload(int sessionId, int pinMode) implements CustomPacketPayload {
        public static final Type<DesktopSessionPinPayload> TYPE = new Type<>(id("desktop_session_pin"));
        public static final StreamCodec<RegistryFriendlyByteBuf, DesktopSessionPinPayload> CODEC = CustomPacketPayload.codec(
            DesktopSessionPinPayload::write,
            DesktopSessionPinPayload::new
        );

        private DesktopSessionPinPayload(RegistryFriendlyByteBuf buf) {
            this(buf.readVarInt(), buf.readVarInt());
        }

        private void write(RegistryFriendlyByteBuf buf) {
            buf.writeVarInt(this.sessionId);
            buf.writeVarInt(this.pinMode);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record DesktopSessionVisibilityPayload(int sessionId, boolean visible) implements CustomPacketPayload {
        public static final Type<DesktopSessionVisibilityPayload> TYPE = new Type<>(id("desktop_session_visibility"));
        public static final StreamCodec<RegistryFriendlyByteBuf, DesktopSessionVisibilityPayload> CODEC = CustomPacketPayload.codec(
            DesktopSessionVisibilityPayload::write,
            DesktopSessionVisibilityPayload::new
        );

        private DesktopSessionVisibilityPayload(RegistryFriendlyByteBuf buf) {
            this(buf.readVarInt(), buf.readBoolean());
        }

        private void write(RegistryFriendlyByteBuf buf) {
            buf.writeVarInt(this.sessionId);
            buf.writeBoolean(this.visible);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record DesktopOpenLinkedSourcesPayload(List<String> sourceKeys) implements CustomPacketPayload {
        public static final Type<DesktopOpenLinkedSourcesPayload> TYPE = new Type<>(id("desktop_open_linked_sources"));
        public static final StreamCodec<RegistryFriendlyByteBuf, DesktopOpenLinkedSourcesPayload> CODEC = CustomPacketPayload.codec(
            DesktopOpenLinkedSourcesPayload::write,
            DesktopOpenLinkedSourcesPayload::new
        );

        private DesktopOpenLinkedSourcesPayload(RegistryFriendlyByteBuf buf) {
            this(readLimitedStringList(buf, LINKED_SOURCE_MAX_KEYS, LINKED_SOURCE_MAX_KEY_LENGTH));
        }

        private void write(RegistryFriendlyByteBuf buf) {
            writeLimitedStringList(buf, this.sourceKeys, LINKED_SOURCE_MAX_KEYS, LINKED_SOURCE_MAX_KEY_LENGTH);
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
        boolean visible,
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
                buf.readBoolean(),
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
            buf.writeBoolean(this.visible);
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

    public record DesktopGhostRecipePayload(int sessionId, RecipeDisplay recipeDisplay) implements CustomPacketPayload {
        public static final Type<DesktopGhostRecipePayload> TYPE = new Type<>(id("desktop_ghost_recipe"));
        public static final StreamCodec<RegistryFriendlyByteBuf, DesktopGhostRecipePayload> CODEC = CustomPacketPayload.codec(
            DesktopGhostRecipePayload::write,
            DesktopGhostRecipePayload::new
        );

        private DesktopGhostRecipePayload(RegistryFriendlyByteBuf buf) {
            this(buf.readVarInt(), RecipeDisplay.STREAM_CODEC.decode(buf));
        }

        private void write(RegistryFriendlyByteBuf buf) {
            buf.writeVarInt(this.sessionId);
            RecipeDisplay.STREAM_CODEC.encode(buf, this.recipeDisplay);
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
