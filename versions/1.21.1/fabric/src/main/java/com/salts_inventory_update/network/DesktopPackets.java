package com.salts_inventory_update.network;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
    private static final int MAX_ITEM_LIST_SIZE = DesktopProtocol.MAX_OPEN_SESSION_ITEMS;
    private static final int MAX_IDENTIFIER_LENGTH = DesktopProtocol.MAX_IDENTIFIER_LENGTH;
    private static final int MAX_INPUT_NAME_LENGTH = 32;
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
    /** 26.2-local capability for authenticated, atomic multi-menu item gestures. */
    public static final long CAP_MULTI_MENU_GESTURES = 1L << 4;
    public static final long CAP_SORT_WINDOWS = 1L << 5;
    public static final int MAX_GESTURE_SLOTS = 512;
    public static final int MAX_BUNDLE_SELECTION_INDEX = DesktopProtocol.MAX_OPEN_SESSION_ITEMS - 1;
    private static final int LINKED_SOURCE_MAX_KEY_LENGTH = DesktopProtocol.MAX_SOURCE_TEXT_LENGTH;

    private DesktopPackets() {
    }

    public static void registerPayloadTypes() {
        PayloadTypeRegistry.serverboundPlay().register(DesktopHelloPayload.TYPE, DesktopHelloPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(DesktopReadyPayload.TYPE, DesktopReadyPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(DesktopModePayload.TYPE, DesktopModePayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(DesktopClickPayload.TYPE, DesktopClickPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(DesktopDragSlotsPayload.TYPE, DesktopDragSlotsPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(DesktopPickupAllPayload.TYPE, DesktopPickupAllPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(DesktopQuickMoveAllPayload.TYPE, DesktopQuickMoveAllPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(DesktopSortWindowsPayload.TYPE, DesktopSortWindowsPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(DesktopBundleSelectPayload.TYPE, DesktopBundleSelectPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(DesktopQuickMovePayload.TYPE, DesktopQuickMovePayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(DesktopButtonPayload.TYPE, DesktopButtonPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(DesktopPlaceRecipePayload.TYPE, DesktopPlaceRecipePayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(DesktopJeiTransferPayload.TYPE, DesktopJeiTransferPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(DesktopRenamePayload.TYPE, DesktopRenamePayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(DesktopCloseSessionPayload.TYPE, DesktopCloseSessionPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(DesktopSessionPinPayload.TYPE, DesktopSessionPinPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(DesktopSessionVisibilityPayload.TYPE, DesktopSessionVisibilityPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(DesktopOpenLinkedSourcesPayload.TYPE, DesktopOpenLinkedSourcesPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(DesktopLinkPayload.TYPE, DesktopLinkPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(DesktopCustomPayload.TYPE, DesktopCustomPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(DesktopCarriedPayload.TYPE, DesktopCarriedPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(InventorySlotPurchasePayload.TYPE, InventorySlotPurchasePayload.CODEC);

        PayloadTypeRegistry.clientboundPlay().register(DesktopHelloAckPayload.TYPE, DesktopHelloAckPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(DesktopPlayerSessionPayload.TYPE, DesktopPlayerSessionPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(DesktopOpenSessionPayload.TYPE, DesktopOpenSessionPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(DesktopSlotPayload.TYPE, DesktopSlotPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(DesktopDataPayload.TYPE, DesktopDataPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(DesktopCarriedPayload.TYPE, DesktopCarriedPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(DesktopMutationAckPayload.TYPE, DesktopMutationAckPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(DesktopSessionClosedPayload.TYPE, DesktopSessionClosedPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(DesktopSessionVisibilityPayload.TYPE, DesktopSessionVisibilityPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(DesktopMerchantOffersPayload.TYPE, DesktopMerchantOffersPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(DesktopCustomPayload.TYPE, DesktopCustomPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(DesktopGhostRecipePayload.TYPE, DesktopGhostRecipePayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(InventoryExpansionSyncPayload.TYPE, InventoryExpansionSyncPayload.CODEC);
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

    private static void writeItemList(RegistryFriendlyByteBuf buf, List<ItemStack> stacks) {
        if (stacks.size() > MAX_ITEM_LIST_SIZE) {
            throw new IllegalArgumentException("Desktop item list is too large: " + stacks.size());
        }
        buf.writeVarInt(stacks.size());
        for (ItemStack stack : stacks) {
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, stack);
        }
    }

    private static List<ItemStack> readItemList(RegistryFriendlyByteBuf buf) {
        int size = buf.readVarInt();
        if (size < 0 || size > MAX_ITEM_LIST_SIZE) {
            throw new IllegalArgumentException("Desktop item list is too large: " + size);
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

    private static void writeSlotReference(RegistryFriendlyByteBuf buf, DesktopSlotReference reference) {
        buf.writeVarInt(reference.sessionId());
        buf.writeLong(reference.sessionToken());
        buf.writeVarInt(reference.stateId());
        buf.writeVarInt(reference.slotIndex());
    }

    private static DesktopSlotReference readSlotReference(RegistryFriendlyByteBuf buf) {
        return new DesktopSlotReference(buf.readVarInt(), buf.readLong(), buf.readVarInt(), buf.readVarInt());
    }

    private static void writeSessionReference(RegistryFriendlyByteBuf buf, DesktopSessionReference reference) {
        buf.writeVarInt(reference.sessionId());
        buf.writeLong(reference.sessionToken());
        buf.writeVarInt(reference.stateId());
    }

    private static DesktopSessionReference readSessionReference(RegistryFriendlyByteBuf buf) {
        return new DesktopSessionReference(buf.readVarInt(), buf.readLong(), buf.readVarInt());
    }

    private static void writeSlotReferences(RegistryFriendlyByteBuf buf, List<DesktopSlotReference> references, int minimum, int maximum) {
        requireListSize("desktop gesture slots", references, minimum, maximum);
        buf.writeVarInt(references.size());
        for (DesktopSlotReference reference : references) {
            writeSlotReference(buf, reference);
        }
    }

    private static List<DesktopSlotReference> readSlotReferences(RegistryFriendlyByteBuf buf, int minimum, int maximum) {
        int size = readBoundedListSize(buf, "desktop gesture slots", minimum, maximum);
        List<DesktopSlotReference> references = new ArrayList<>(size);
        for (int index = 0; index < size; index++) {
            references.add(readSlotReference(buf));
        }
        return references;
    }

    private static void writeSessionReferences(RegistryFriendlyByteBuf buf, List<DesktopSessionReference> references, int minimum, int maximum) {
        requireListSize("desktop gesture sessions", references, minimum, maximum);
        buf.writeVarInt(references.size());
        for (DesktopSessionReference reference : references) {
            writeSessionReference(buf, reference);
        }
    }

    private static List<DesktopSessionReference> readSessionReferences(RegistryFriendlyByteBuf buf, int minimum, int maximum) {
        int size = readBoundedListSize(buf, "desktop gesture sessions", minimum, maximum);
        List<DesktopSessionReference> references = new ArrayList<>(size);
        for (int index = 0; index < size; index++) {
            references.add(readSessionReference(buf));
        }
        return references;
    }

    private static int readBoundedListSize(RegistryFriendlyByteBuf buf, String name, int minimum, int maximum) {
        int size = buf.readVarInt();
        requireListSize(name, size, minimum, maximum);
        return size;
    }

    private static void requireListSize(String name, List<?> values, int minimum, int maximum) {
        if (values == null) {
            throw new IllegalArgumentException(name + " cannot be null");
        }
        requireListSize(name, values.size(), minimum, maximum);
    }

    private static void requireListSize(String name, int size, int minimum, int maximum) {
        if (size < minimum || size > maximum) {
            throw new IllegalArgumentException(name + " must contain between " + minimum + " and " + maximum + " entries, got " + size);
        }
    }

    private static void requireUniqueSlots(String name, List<DesktopSlotReference> references) {
        Set<Long> identities = new HashSet<>();
        for (DesktopSlotReference reference : references) {
            long identity = ((long) reference.sessionId() << 32) ^ (reference.slotIndex() & 0xffffffffL);
            if (!identities.add(identity)) {
                throw new IllegalArgumentException(name + " contains a duplicate slot reference");
            }
        }
    }

    private static void requireUniqueSessions(String name, List<DesktopSessionReference> references) {
        Set<Integer> identities = new HashSet<>();
        for (DesktopSessionReference reference : references) {
            if (!identities.add(reference.sessionId())) {
                throw new IllegalArgumentException(name + " contains a duplicate session reference");
            }
        }
    }

    public record InventorySlotPurchasePayload(long connectionNonce, long mutationId, long playerSessionToken, int stateId) implements CustomPacketPayload {
        public static final Type<InventorySlotPurchasePayload> TYPE = new Type<>(id("inventory_slot_purchase"));
        public static final StreamCodec<RegistryFriendlyByteBuf, InventorySlotPurchasePayload> CODEC = CustomPacketPayload.codec(
            InventorySlotPurchasePayload::write,
            InventorySlotPurchasePayload::new
        );

        private InventorySlotPurchasePayload(RegistryFriendlyByteBuf buf) {
            this(buf.readLong(), buf.readVarLong(), buf.readLong(), buf.readVarInt());
        }

        public InventorySlotPurchasePayload {
            if (mutationId <= 0L || playerSessionToken == 0L) {
                throw new IllegalArgumentException("Invalid inventory slot purchase mutation");
            }
        }

        private void write(RegistryFriendlyByteBuf buf) {
            buf.writeLong(this.connectionNonce);
            buf.writeVarLong(this.mutationId);
            buf.writeLong(this.playerSessionToken);
            buf.writeVarInt(this.stateId);
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

        public InventoryExpansionSyncPayload {
            if (slotCount < 0 || slotCount > DesktopProtocol.MAX_EXPANSION_SLOTS || items.size() != slotCount) {
                throw new IllegalArgumentException("Invalid inventory expansion snapshot: slots=" + slotCount + ", items=" + items.size());
            }
            items = items.stream().map(ItemStack::copy).toList();
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

    public record DesktopHelloPayload(int protocolVersion, long clientNonce, long capabilities, boolean uiEnabled, List<String> forcedMenuIds) implements CustomPacketPayload {
        public static final Type<DesktopHelloPayload> TYPE = new Type<>(id("desktop_hello"));
        public static final StreamCodec<RegistryFriendlyByteBuf, DesktopHelloPayload> CODEC = CustomPacketPayload.codec(DesktopHelloPayload::write, DesktopHelloPayload::new);

        private DesktopHelloPayload(RegistryFriendlyByteBuf buf) {
            this(buf.readVarInt(), buf.readLong(), buf.readVarLong(), buf.readBoolean(), readLimitedStringList(buf, DesktopProtocol.MAX_FORCED_MENU_IDS, MAX_IDENTIFIER_LENGTH));
        }

        public DesktopHelloPayload {
            forcedMenuIds = List.copyOf(forcedMenuIds);
        }

        private void write(RegistryFriendlyByteBuf buf) {
            buf.writeVarInt(this.protocolVersion);
            buf.writeLong(this.clientNonce);
            buf.writeVarLong(this.capabilities);
            buf.writeBoolean(this.uiEnabled);
            writeLimitedStringList(buf, this.forcedMenuIds, DesktopProtocol.MAX_FORCED_MENU_IDS, MAX_IDENTIFIER_LENGTH);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record DesktopHelloAckPayload(int protocolVersion, long clientNonce, long connectionNonce, long capabilities, boolean uiEnabled, long playerSessionToken) implements CustomPacketPayload {
        public static final Type<DesktopHelloAckPayload> TYPE = new Type<>(id("desktop_hello_ack"));
        public static final StreamCodec<RegistryFriendlyByteBuf, DesktopHelloAckPayload> CODEC = CustomPacketPayload.codec(DesktopHelloAckPayload::write, DesktopHelloAckPayload::new);

        private DesktopHelloAckPayload(RegistryFriendlyByteBuf buf) {
            this(buf.readVarInt(), buf.readLong(), buf.readLong(), buf.readVarLong(), buf.readBoolean(), buf.readLong());
        }

        private void write(RegistryFriendlyByteBuf buf) {
            buf.writeVarInt(this.protocolVersion);
            buf.writeLong(this.clientNonce);
            buf.writeLong(this.connectionNonce);
            buf.writeVarLong(this.capabilities);
            buf.writeBoolean(this.uiEnabled);
            buf.writeLong(this.playerSessionToken);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Protocol-v1 sentinel, retained only so a legacy peer gets a clear rejection. */
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

    public record DesktopModePayload(long connectionNonce, long sequence, boolean uiEnabled, List<String> forcedMenuIds) implements CustomPacketPayload {
        public static final Type<DesktopModePayload> TYPE = new Type<>(id("desktop_mode"));
        public static final StreamCodec<RegistryFriendlyByteBuf, DesktopModePayload> CODEC = CustomPacketPayload.codec(DesktopModePayload::write, DesktopModePayload::new);

        private DesktopModePayload(RegistryFriendlyByteBuf buf) {
            this(buf.readLong(), buf.readVarLong(), buf.readBoolean(), readLimitedStringList(buf, DesktopProtocol.MAX_FORCED_MENU_IDS, MAX_IDENTIFIER_LENGTH));
        }

        public DesktopModePayload {
            forcedMenuIds = List.copyOf(forcedMenuIds);
        }

        private void write(RegistryFriendlyByteBuf buf) {
            buf.writeLong(this.connectionNonce);
            buf.writeVarLong(this.sequence);
            buf.writeBoolean(this.uiEnabled);
            writeLimitedStringList(buf, this.forcedMenuIds, DesktopProtocol.MAX_FORCED_MENU_IDS, MAX_IDENTIFIER_LENGTH);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record DesktopPlayerSessionPayload(long connectionNonce, long playerSessionToken) implements CustomPacketPayload {
        public static final Type<DesktopPlayerSessionPayload> TYPE = new Type<>(id("desktop_player_session"));
        public static final StreamCodec<RegistryFriendlyByteBuf, DesktopPlayerSessionPayload> CODEC = CustomPacketPayload.codec(DesktopPlayerSessionPayload::write, DesktopPlayerSessionPayload::new);

        private DesktopPlayerSessionPayload(RegistryFriendlyByteBuf buf) {
            this(buf.readLong(), buf.readLong());
        }

        private void write(RegistryFriendlyByteBuf buf) {
            buf.writeLong(this.connectionNonce);
            buf.writeLong(this.playerSessionToken);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record DesktopSlotReference(int sessionId, long sessionToken, int stateId, int slotIndex) {
        public DesktopSlotReference {
            if (sessionId < PLAYER_MENU_SESSION || sessionToken == 0L || stateId < 0 || slotIndex < 0 || slotIndex >= MAX_ITEM_LIST_SIZE) {
                throw new IllegalArgumentException("Invalid desktop slot reference");
            }
        }
    }

    public record DesktopSessionReference(int sessionId, long sessionToken, int stateId) {
        public DesktopSessionReference {
            if (sessionId < PLAYER_MENU_SESSION || sessionToken == 0L || stateId < 0) {
                throw new IllegalArgumentException("Invalid desktop session reference");
            }
        }
    }

    public record DesktopDragSlotsPayload(long connectionNonce, long mutationId, long playerSessionToken, int quickCraftType, List<DesktopSlotReference> slots) implements CustomPacketPayload {
        public static final Type<DesktopDragSlotsPayload> TYPE = new Type<>(id("desktop_drag_slots"));
        public static final StreamCodec<RegistryFriendlyByteBuf, DesktopDragSlotsPayload> CODEC = CustomPacketPayload.codec(
            DesktopDragSlotsPayload::write,
            DesktopDragSlotsPayload::new
        );

        private DesktopDragSlotsPayload(RegistryFriendlyByteBuf buf) {
            this(buf.readLong(), buf.readVarLong(), buf.readLong(), buf.readVarInt(), readSlotReferences(buf, 1, MAX_GESTURE_SLOTS));
        }

        public DesktopDragSlotsPayload {
            if (mutationId <= 0L || playerSessionToken == 0L || quickCraftType < 0 || quickCraftType > 2) {
                throw new IllegalArgumentException("Invalid desktop drag type: " + quickCraftType);
            }
            requireListSize("desktop drag slots", slots, 1, MAX_GESTURE_SLOTS);
            slots = List.copyOf(slots);
            requireUniqueSlots("desktop drag slots", slots);
        }

        private void write(RegistryFriendlyByteBuf buf) {
            buf.writeLong(this.connectionNonce);
            buf.writeVarLong(this.mutationId);
            buf.writeLong(this.playerSessionToken);
            buf.writeVarInt(this.quickCraftType);
            writeSlotReferences(buf, this.slots, 1, MAX_GESTURE_SLOTS);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record DesktopPickupAllPayload(
        long connectionNonce,
        long mutationId,
        long playerSessionToken,
        int anchorSessionId,
        int anchorSlotIndex,
        int button,
        List<DesktopSessionReference> sources
    ) implements CustomPacketPayload {
        public static final Type<DesktopPickupAllPayload> TYPE = new Type<>(id("desktop_pickup_all"));
        public static final StreamCodec<RegistryFriendlyByteBuf, DesktopPickupAllPayload> CODEC = CustomPacketPayload.codec(
            DesktopPickupAllPayload::write,
            DesktopPickupAllPayload::new
        );

        private DesktopPickupAllPayload(RegistryFriendlyByteBuf buf) {
            this(
                buf.readLong(),
                buf.readVarLong(),
                buf.readLong(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt(),
                readSessionReferences(buf, 1, DesktopProtocol.MAX_DESKTOP_SESSIONS + 1)
            );
        }

        public DesktopPickupAllPayload {
            if (mutationId <= 0L || playerSessionToken == 0L || anchorSessionId < PLAYER_MENU_SESSION || anchorSlotIndex < 0 || anchorSlotIndex >= MAX_ITEM_LIST_SIZE || (button != 0 && button != 1)) {
                throw new IllegalArgumentException("Invalid desktop pickup-all request");
            }
            requireListSize("desktop pickup-all sessions", sources, 1, DesktopProtocol.MAX_DESKTOP_SESSIONS + 1);
            sources = List.copyOf(sources);
            requireUniqueSessions("desktop pickup-all sessions", sources);
            if (sources.stream().noneMatch(source -> source.sessionId() == anchorSessionId)) {
                throw new IllegalArgumentException("Desktop pickup-all sources do not include the anchor session");
            }
        }

        private void write(RegistryFriendlyByteBuf buf) {
            buf.writeLong(this.connectionNonce);
            buf.writeVarLong(this.mutationId);
            buf.writeLong(this.playerSessionToken);
            buf.writeVarInt(this.anchorSessionId);
            buf.writeVarInt(this.anchorSlotIndex);
            buf.writeVarInt(this.button);
            writeSessionReferences(buf, this.sources, 1, DesktopProtocol.MAX_DESKTOP_SESSIONS + 1);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record DesktopSortWindowsPayload(
        long connectionNonce, long mutationId, long playerSessionToken,
        DesktopSessionReference source, List<DesktopSessionReference> destinations,
        int focusedSessionId, boolean shift
    ) implements CustomPacketPayload {
        public static final Type<DesktopSortWindowsPayload> TYPE = new Type<>(id("desktop_sort_windows"));
        public static final StreamCodec<RegistryFriendlyByteBuf, DesktopSortWindowsPayload> CODEC =
            CustomPacketPayload.codec(DesktopSortWindowsPayload::write, DesktopSortWindowsPayload::new);
        private DesktopSortWindowsPayload(RegistryFriendlyByteBuf buf) {
            this(buf.readLong(), buf.readVarLong(), buf.readLong(), readSessionReference(buf),
                readSessionReferences(buf, 1, DesktopProtocol.MAX_DESKTOP_SESSIONS), buf.readVarInt(), buf.readBoolean());
        }
        public DesktopSortWindowsPayload {
            if (mutationId <= 0 || playerSessionToken == 0 || source == null) throw new IllegalArgumentException("Invalid sort request");
            requireListSize("sort destinations", destinations, 1, DesktopProtocol.MAX_DESKTOP_SESSIONS);
            destinations = List.copyOf(destinations);
            if (destinations.stream().map(DesktopSessionReference::sessionId).distinct().count() != destinations.size())
                throw new IllegalArgumentException("Duplicate sort destinations");
        }
        private void write(RegistryFriendlyByteBuf buf) {
            buf.writeLong(connectionNonce); buf.writeVarLong(mutationId); buf.writeLong(playerSessionToken);
            writeSessionReference(buf, source);
            writeSessionReferences(buf, destinations, 1, DesktopProtocol.MAX_DESKTOP_SESSIONS);
            buf.writeVarInt(focusedSessionId); buf.writeBoolean(shift);
        }
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record DesktopQuickMoveAllPayload(
        long connectionNonce,
        long mutationId,
        long playerSessionToken,
        List<DesktopSlotReference> sources,
        int targetKind,
        DesktopSessionReference target
    ) implements CustomPacketPayload {
        public static final Type<DesktopQuickMoveAllPayload> TYPE = new Type<>(id("desktop_quick_move_all"));
        public static final StreamCodec<RegistryFriendlyByteBuf, DesktopQuickMoveAllPayload> CODEC = CustomPacketPayload.codec(
            DesktopQuickMoveAllPayload::write,
            DesktopQuickMoveAllPayload::new
        );

        private DesktopQuickMoveAllPayload(RegistryFriendlyByteBuf buf) {
            this(
                buf.readLong(),
                buf.readVarLong(),
                buf.readLong(),
                readSlotReferences(buf, 1, MAX_GESTURE_SLOTS),
                buf.readVarInt(),
                readSessionReference(buf)
            );
        }

        public DesktopQuickMoveAllPayload {
            if (mutationId <= 0L || playerSessionToken == 0L || targetKind < QUICK_TARGET_DEFAULT || targetKind > QUICK_TARGET_HOTBAR || target == null) {
                throw new IllegalArgumentException("Invalid desktop quick-move-all target");
            }
            requireListSize("desktop quick-move-all slots", sources, 1, MAX_GESTURE_SLOTS);
            sources = List.copyOf(sources);
            requireUniqueSlots("desktop quick-move-all slots", sources);
        }

        private void write(RegistryFriendlyByteBuf buf) {
            buf.writeLong(this.connectionNonce);
            buf.writeVarLong(this.mutationId);
            buf.writeLong(this.playerSessionToken);
            writeSlotReferences(buf, this.sources, 1, MAX_GESTURE_SLOTS);
            buf.writeVarInt(this.targetKind);
            writeSessionReference(buf, this.target);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record DesktopBundleSelectPayload(long connectionNonce, long mutationId, long playerSessionToken, DesktopSlotReference target, int selectedItemIndex) implements CustomPacketPayload {
        public static final Type<DesktopBundleSelectPayload> TYPE = new Type<>(id("desktop_bundle_select"));
        public static final StreamCodec<RegistryFriendlyByteBuf, DesktopBundleSelectPayload> CODEC = CustomPacketPayload.codec(
            DesktopBundleSelectPayload::write,
            DesktopBundleSelectPayload::new
        );

        private DesktopBundleSelectPayload(RegistryFriendlyByteBuf buf) {
            this(buf.readLong(), buf.readVarLong(), buf.readLong(), readSlotReference(buf), buf.readVarInt());
        }

        public DesktopBundleSelectPayload {
            if (mutationId <= 0L || playerSessionToken == 0L || target == null || selectedItemIndex < -1 || selectedItemIndex > MAX_BUNDLE_SELECTION_INDEX) {
                throw new IllegalArgumentException("Invalid desktop bundle selection: " + selectedItemIndex);
            }
        }

        private void write(RegistryFriendlyByteBuf buf) {
            buf.writeLong(this.connectionNonce);
            buf.writeVarLong(this.mutationId);
            buf.writeLong(this.playerSessionToken);
            writeSlotReference(buf, this.target);
            buf.writeVarInt(this.selectedItemIndex);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record DesktopClickPayload(long connectionNonce, long mutationId, long playerSessionToken, int sessionId, long sessionToken, int stateId, int debugId, int slotIndex, int button, String inputName, ItemStack clientCarried) implements CustomPacketPayload {
        public static final Type<DesktopClickPayload> TYPE = new Type<>(id("desktop_click"));
        public static final StreamCodec<RegistryFriendlyByteBuf, DesktopClickPayload> CODEC = CustomPacketPayload.codec(
            DesktopClickPayload::write,
            DesktopClickPayload::new
        );

        private DesktopClickPayload(RegistryFriendlyByteBuf buf) {
            this(
                buf.readLong(),
                buf.readVarLong(),
                buf.readLong(),
                buf.readVarInt(),
                buf.readLong(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readUtf(MAX_INPUT_NAME_LENGTH),
                ItemStack.OPTIONAL_STREAM_CODEC.decode(buf)
            );
        }

        public DesktopClickPayload {
            if (mutationId <= 0L || playerSessionToken == 0L || inputName == null || inputName.isBlank() || inputName.length() > MAX_INPUT_NAME_LENGTH) {
                throw new IllegalArgumentException("Invalid desktop click input name");
            }
            clientCarried = clientCarried.copy();
        }

        private void write(RegistryFriendlyByteBuf buf) {
            buf.writeLong(this.connectionNonce);
            buf.writeVarLong(this.mutationId);
            buf.writeLong(this.playerSessionToken);
            buf.writeVarInt(this.sessionId);
            buf.writeLong(this.sessionToken);
            buf.writeVarInt(this.stateId);
            buf.writeVarInt(this.debugId);
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

    public record DesktopQuickMovePayload(long connectionNonce, long mutationId, long playerSessionToken, int sourceSessionId, long sourceSessionToken, int sourceStateId, int sourceSlotIndex, int targetKind, int targetSessionId, long targetSessionToken, int targetStateId) implements CustomPacketPayload {
        public static final Type<DesktopQuickMovePayload> TYPE = new Type<>(id("desktop_quick_move"));
        public static final StreamCodec<RegistryFriendlyByteBuf, DesktopQuickMovePayload> CODEC = CustomPacketPayload.codec(
            DesktopQuickMovePayload::write,
            DesktopQuickMovePayload::new
        );

        private DesktopQuickMovePayload(RegistryFriendlyByteBuf buf) {
            this(buf.readLong(), buf.readVarLong(), buf.readLong(), buf.readVarInt(), buf.readLong(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readLong(), buf.readVarInt());
        }

        public DesktopQuickMovePayload {
            if (mutationId <= 0L || playerSessionToken == 0L) {
                throw new IllegalArgumentException("Invalid desktop quick-move mutation id");
            }
        }

        private void write(RegistryFriendlyByteBuf buf) {
            buf.writeLong(this.connectionNonce);
            buf.writeVarLong(this.mutationId);
            buf.writeLong(this.playerSessionToken);
            buf.writeVarInt(this.sourceSessionId);
            buf.writeLong(this.sourceSessionToken);
            buf.writeVarInt(this.sourceStateId);
            buf.writeVarInt(this.sourceSlotIndex);
            buf.writeVarInt(this.targetKind);
            buf.writeVarInt(this.targetSessionId);
            buf.writeLong(this.targetSessionToken);
            buf.writeVarInt(this.targetStateId);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record DesktopButtonPayload(long connectionNonce, long mutationId, long playerSessionToken, int sessionId, long sessionToken, int stateId, int buttonId) implements CustomPacketPayload {
        public static final Type<DesktopButtonPayload> TYPE = new Type<>(id("desktop_button"));
        public static final StreamCodec<RegistryFriendlyByteBuf, DesktopButtonPayload> CODEC = CustomPacketPayload.codec(
            DesktopButtonPayload::write,
            DesktopButtonPayload::new
        );

        private DesktopButtonPayload(RegistryFriendlyByteBuf buf) {
            this(buf.readLong(), buf.readVarLong(), buf.readLong(), buf.readVarInt(), buf.readLong(), buf.readVarInt(), buf.readVarInt());
        }

        public DesktopButtonPayload {
            if (mutationId <= 0L || playerSessionToken == 0L) {
                throw new IllegalArgumentException("Invalid desktop button mutation");
            }
        }

        private void write(RegistryFriendlyByteBuf buf) {
            buf.writeLong(this.connectionNonce);
            buf.writeVarLong(this.mutationId);
            buf.writeLong(this.playerSessionToken);
            buf.writeVarInt(this.sessionId);
            buf.writeLong(this.sessionToken);
            buf.writeVarInt(this.stateId);
            buf.writeVarInt(this.buttonId);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record DesktopPlaceRecipePayload(long connectionNonce, long mutationId, long playerSessionToken, int sessionId, long sessionToken, int stateId, ResourceLocation recipeId, boolean useMaxItems) implements CustomPacketPayload {
        public static final Type<DesktopPlaceRecipePayload> TYPE = new Type<>(id("desktop_place_recipe"));
        public static final StreamCodec<RegistryFriendlyByteBuf, DesktopPlaceRecipePayload> CODEC = CustomPacketPayload.codec(
            DesktopPlaceRecipePayload::write,
            DesktopPlaceRecipePayload::new
        );

        private DesktopPlaceRecipePayload(RegistryFriendlyByteBuf buf) {
            this(buf.readLong(), buf.readVarLong(), buf.readLong(), buf.readVarInt(), buf.readLong(), buf.readVarInt(), buf.readResourceLocation(), buf.readBoolean());
        }

        public DesktopPlaceRecipePayload {
            if (mutationId <= 0L || playerSessionToken == 0L || recipeId == null) {
                throw new IllegalArgumentException("Invalid desktop recipe placement mutation");
            }
        }

        private void write(RegistryFriendlyByteBuf buf) {
            buf.writeLong(this.connectionNonce);
            buf.writeVarLong(this.mutationId);
            buf.writeLong(this.playerSessionToken);
            buf.writeVarInt(this.sessionId);
            buf.writeLong(this.sessionToken);
            buf.writeVarInt(this.stateId);
            buf.writeResourceLocation(this.recipeId);
            buf.writeBoolean(this.useMaxItems);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record DesktopJeiTransferPayload(long connectionNonce, long mutationId, long playerSessionToken, int targetSessionId, long targetSessionToken, int targetStateId, ResourceLocation recipeId, boolean maxTransfer) implements CustomPacketPayload {
        public static final Type<DesktopJeiTransferPayload> TYPE = new Type<>(id("desktop_jei_transfer"));
        public static final StreamCodec<RegistryFriendlyByteBuf, DesktopJeiTransferPayload> CODEC = CustomPacketPayload.codec(
            DesktopJeiTransferPayload::write,
            DesktopJeiTransferPayload::new
        );

        public DesktopJeiTransferPayload {
            if (mutationId <= 0L
                || playerSessionToken == 0L
                || targetSessionId < PLAYER_MENU_SESSION
                || targetSessionToken == 0L
                || targetStateId < 0
                || recipeId == null
                || recipeId.toString().length() > MAX_IDENTIFIER_LENGTH) {
                throw new IllegalArgumentException("Invalid desktop recipe id");
            }
        }

        private DesktopJeiTransferPayload(RegistryFriendlyByteBuf buf) {
            this(
                buf.readLong(),
                buf.readVarLong(),
                buf.readLong(),
                buf.readVarInt(),
                buf.readLong(),
                buf.readVarInt(),
                ResourceLocation.parse(buf.readUtf(MAX_IDENTIFIER_LENGTH)),
                buf.readBoolean()
            );
        }

        private void write(RegistryFriendlyByteBuf buf) {
            buf.writeLong(this.connectionNonce);
            buf.writeVarLong(this.mutationId);
            buf.writeLong(this.playerSessionToken);
            buf.writeVarInt(this.targetSessionId);
            buf.writeLong(this.targetSessionToken);
            buf.writeVarInt(this.targetStateId);
            buf.writeUtf(this.recipeId.toString(), MAX_IDENTIFIER_LENGTH);
            buf.writeBoolean(this.maxTransfer);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record DesktopRenamePayload(long connectionNonce, long mutationId, long playerSessionToken, int sessionId, long sessionToken, int stateId, String name) implements CustomPacketPayload {
        public static final Type<DesktopRenamePayload> TYPE = new Type<>(id("desktop_rename"));
        public static final StreamCodec<RegistryFriendlyByteBuf, DesktopRenamePayload> CODEC = CustomPacketPayload.codec(
            DesktopRenamePayload::write,
            DesktopRenamePayload::new
        );

        private DesktopRenamePayload(RegistryFriendlyByteBuf buf) {
            this(buf.readLong(), buf.readVarLong(), buf.readLong(), buf.readVarInt(), buf.readLong(), buf.readVarInt(), buf.readUtf(50));
        }

        public DesktopRenamePayload {
            if (mutationId <= 0L || playerSessionToken == 0L || name == null) {
                throw new IllegalArgumentException("Invalid desktop rename mutation");
            }
        }

        private void write(RegistryFriendlyByteBuf buf) {
            buf.writeLong(this.connectionNonce);
            buf.writeVarLong(this.mutationId);
            buf.writeLong(this.playerSessionToken);
            buf.writeVarInt(this.sessionId);
            buf.writeLong(this.sessionToken);
            buf.writeVarInt(this.stateId);
            buf.writeUtf(this.name, 50);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record DesktopCustomPayload(long connectionNonce, long mutationId, long playerSessionToken, int sessionId, long sessionToken, int stateId, ResourceLocation channel, byte[] data) implements CustomPacketPayload {
        public static final Type<DesktopCustomPayload> TYPE = new Type<>(id("desktop_custom"));
        public static final StreamCodec<RegistryFriendlyByteBuf, DesktopCustomPayload> CODEC = CustomPacketPayload.codec(
            DesktopCustomPayload::write,
            DesktopCustomPayload::new
        );

        private DesktopCustomPayload(RegistryFriendlyByteBuf buf) {
            this(
                buf.readLong(),
                buf.readVarLong(),
                buf.readLong(),
                buf.readVarInt(),
                buf.readLong(),
                buf.readVarInt(),
                ResourceLocation.parse(buf.readUtf(MAX_IDENTIFIER_LENGTH)),
                buf.readByteArray(DesktopProtocol.MAX_CUSTOM_DATA_BYTES)
            );
        }

        public DesktopCustomPayload {
            if (mutationId < 0L || (mutationId > 0L && playerSessionToken == 0L)
                || channel == null || channel.toString().length() > MAX_IDENTIFIER_LENGTH) {
                throw new IllegalArgumentException("Invalid desktop custom payload channel");
            }
            if (data == null || data.length > DesktopProtocol.MAX_CUSTOM_DATA_BYTES) {
                throw new IllegalArgumentException("Desktop custom payload is too large: " + (data == null ? -1 : data.length));
            }
            data = data.clone();
        }

        public DesktopCustomPayload(int sessionId, ResourceLocation channel, byte[] data) {
            this(0L, 0L, 0L, sessionId, 0L, 0, channel, data);
        }

        @Override
        public byte[] data() {
            return this.data.clone();
        }

        private void write(RegistryFriendlyByteBuf buf) {
            buf.writeLong(this.connectionNonce);
            buf.writeVarLong(this.mutationId);
            buf.writeLong(this.playerSessionToken);
            buf.writeVarInt(this.sessionId);
            buf.writeLong(this.sessionToken);
            buf.writeVarInt(this.stateId);
            buf.writeUtf(this.channel.toString(), MAX_IDENTIFIER_LENGTH);
            buf.writeByteArray(this.data);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record DesktopCloseSessionPayload(long connectionNonce, int sessionId, long sessionToken) implements CustomPacketPayload {
        public static final Type<DesktopCloseSessionPayload> TYPE = new Type<>(id("desktop_close_session"));
        public static final StreamCodec<RegistryFriendlyByteBuf, DesktopCloseSessionPayload> CODEC = CustomPacketPayload.codec(
            DesktopCloseSessionPayload::write,
            DesktopCloseSessionPayload::new
        );

        private DesktopCloseSessionPayload(RegistryFriendlyByteBuf buf) {
            this(buf.readLong(), buf.readVarInt(), buf.readLong());
        }

        private void write(RegistryFriendlyByteBuf buf) {
            buf.writeLong(this.connectionNonce);
            buf.writeVarInt(this.sessionId);
            buf.writeLong(this.sessionToken);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record DesktopSessionPinPayload(long connectionNonce, int sessionId, long sessionToken, int pinMode) implements CustomPacketPayload {
        public static final Type<DesktopSessionPinPayload> TYPE = new Type<>(id("desktop_session_pin"));
        public static final StreamCodec<RegistryFriendlyByteBuf, DesktopSessionPinPayload> CODEC = CustomPacketPayload.codec(
            DesktopSessionPinPayload::write,
            DesktopSessionPinPayload::new
        );

        private DesktopSessionPinPayload(RegistryFriendlyByteBuf buf) {
            this(buf.readLong(), buf.readVarInt(), buf.readLong(), buf.readVarInt());
        }

        private void write(RegistryFriendlyByteBuf buf) {
            buf.writeLong(this.connectionNonce);
            buf.writeVarInt(this.sessionId);
            buf.writeLong(this.sessionToken);
            buf.writeVarInt(this.pinMode);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record DesktopSessionVisibilityPayload(long connectionNonce, int sessionId, long sessionToken, boolean visible) implements CustomPacketPayload {
        public static final Type<DesktopSessionVisibilityPayload> TYPE = new Type<>(id("desktop_session_visibility"));
        public static final StreamCodec<RegistryFriendlyByteBuf, DesktopSessionVisibilityPayload> CODEC = CustomPacketPayload.codec(
            DesktopSessionVisibilityPayload::write,
            DesktopSessionVisibilityPayload::new
        );

        private DesktopSessionVisibilityPayload(RegistryFriendlyByteBuf buf) {
            this(buf.readLong(), buf.readVarInt(), buf.readLong(), buf.readBoolean());
        }

        private void write(RegistryFriendlyByteBuf buf) {
            buf.writeLong(this.connectionNonce);
            buf.writeVarInt(this.sessionId);
            buf.writeLong(this.sessionToken);
            buf.writeBoolean(this.visible);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record DesktopOpenLinkedSourcesPayload(long connectionNonce, int originSessionId, long originSessionToken) implements CustomPacketPayload {
        public static final Type<DesktopOpenLinkedSourcesPayload> TYPE = new Type<>(id("desktop_open_linked_sources"));
        public static final StreamCodec<RegistryFriendlyByteBuf, DesktopOpenLinkedSourcesPayload> CODEC = CustomPacketPayload.codec(
            DesktopOpenLinkedSourcesPayload::write,
            DesktopOpenLinkedSourcesPayload::new
        );

        private DesktopOpenLinkedSourcesPayload(RegistryFriendlyByteBuf buf) {
            this(buf.readLong(), buf.readVarInt(), buf.readLong());
        }

        private void write(RegistryFriendlyByteBuf buf) {
            buf.writeLong(this.connectionNonce);
            buf.writeVarInt(this.originSessionId);
            buf.writeLong(this.originSessionToken);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record DesktopLinkPayload(long connectionNonce, int firstSessionId, long firstSessionToken, int secondSessionId, long secondSessionToken, int action) implements CustomPacketPayload {
        public static final int ACTION_LINK = 1;
        public static final int ACTION_DETACH = 2;
        public static final Type<DesktopLinkPayload> TYPE = new Type<>(id("desktop_link"));
        public static final StreamCodec<RegistryFriendlyByteBuf, DesktopLinkPayload> CODEC = CustomPacketPayload.codec(DesktopLinkPayload::write, DesktopLinkPayload::new);

        private DesktopLinkPayload(RegistryFriendlyByteBuf buf) {
            this(buf.readLong(), buf.readVarInt(), buf.readLong(), buf.readVarInt(), buf.readLong(), buf.readVarInt());
        }

        public DesktopLinkPayload {
            if (action != ACTION_LINK && action != ACTION_DETACH) {
                throw new IllegalArgumentException("Invalid desktop link action: " + action);
            }
        }

        private void write(RegistryFriendlyByteBuf buf) {
            buf.writeLong(this.connectionNonce);
            buf.writeVarInt(this.firstSessionId);
            buf.writeLong(this.firstSessionToken);
            buf.writeVarInt(this.secondSessionId);
            buf.writeLong(this.secondSessionToken);
            buf.writeVarInt(this.action);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record DesktopOpenSessionPayload(
        int sessionId,
        long sessionToken,
        int menuTypeId,
        int specialKind,
        int entityId,
        int columns,
        int stateId,
        boolean visible,
        boolean recipeTransferSupported,
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
                buf.readLong(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readBoolean(),
                buf.readBoolean(),
                buf.readUtf(LINKED_SOURCE_MAX_KEY_LENGTH),
                ComponentSerialization.STREAM_CODEC.decode(buf),
                readItemList(buf),
                ItemStack.OPTIONAL_STREAM_CODEC.decode(buf),
                buf.readVarIntArray(DesktopProtocol.MAX_MENU_DATA_VALUES)
            );
        }

        public DesktopOpenSessionPayload {
            boolean generic = specialKind == SPECIAL_GENERIC;
            if (sessionId <= PLAYER_MENU_SESSION
                || sessionToken == 0L
                || stateId < 0
                || specialKind < SPECIAL_GENERIC
                || specialKind > SPECIAL_LLAMA
                || columns < 0
                || columns > DesktopProtocol.MAX_MOUNT_COLUMNS
                || sourceKey == null
                || sourceKey.length() > LINKED_SOURCE_MAX_KEY_LENGTH
                || title == null
                || title.getString().length() > LINKED_SOURCE_MAX_KEY_LENGTH
                || items == null
                || items.size() > MAX_ITEM_LIST_SIZE
                || carried == null
                || data == null
                || data.length > DesktopProtocol.MAX_MENU_DATA_VALUES
                || (generic && (menuTypeId < 0 || menuTypeById(menuTypeId) == null || entityId != -1 || columns != 0))
                || (!generic && (menuTypeId != -1 || entityId < 0))) {
                throw new IllegalArgumentException("Invalid desktop open-session snapshot");
            }
            items = items.stream().map(ItemStack::copy).toList();
            carried = carried.copy();
            data = data.clone();
        }

        @Override
        public int[] data() {
            return this.data.clone();
        }

        private void write(RegistryFriendlyByteBuf buf) {
            buf.writeVarInt(this.sessionId);
            buf.writeLong(this.sessionToken);
            buf.writeVarInt(this.menuTypeId);
            buf.writeVarInt(this.specialKind);
            buf.writeVarInt(this.entityId);
            buf.writeVarInt(this.columns);
            buf.writeVarInt(this.stateId);
            buf.writeBoolean(this.visible);
            buf.writeBoolean(this.recipeTransferSupported);
            buf.writeUtf(this.sourceKey, LINKED_SOURCE_MAX_KEY_LENGTH);
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

    public record DesktopCarriedPayload(long connectionNonce, long mutationId, long playerSessionToken, int stateId, ItemStack carried) implements CustomPacketPayload {
        public static final Type<DesktopCarriedPayload> TYPE = new Type<>(id("desktop_carried"));
        public static final StreamCodec<RegistryFriendlyByteBuf, DesktopCarriedPayload> CODEC = CustomPacketPayload.codec(
            DesktopCarriedPayload::write,
            DesktopCarriedPayload::new
        );

        private DesktopCarriedPayload(RegistryFriendlyByteBuf buf) {
            this(buf.readLong(), buf.readVarLong(), buf.readLong(), buf.readVarInt(), ItemStack.OPTIONAL_STREAM_CODEC.decode(buf));
        }

        public DesktopCarriedPayload {
            if (mutationId < 0L || (mutationId > 0L && playerSessionToken == 0L)) {
                throw new IllegalArgumentException("Invalid desktop carried mutation id");
            }
            carried = carried.copy();
        }

        private void write(RegistryFriendlyByteBuf buf) {
            buf.writeLong(this.connectionNonce);
            buf.writeVarLong(this.mutationId);
            buf.writeLong(this.playerSessionToken);
            buf.writeVarInt(this.stateId);
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, this.carried);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Releases the single shared-cursor mutation lane after all preceding authoritative syncs. */
    public record DesktopMutationAckPayload(
        long connectionNonce,
        long playerSessionToken,
        long mutationId,
        boolean sequenceAccepted
    ) implements CustomPacketPayload {
        public static final Type<DesktopMutationAckPayload> TYPE = new Type<>(id("desktop_mutation_ack"));
        public static final StreamCodec<RegistryFriendlyByteBuf, DesktopMutationAckPayload> CODEC = CustomPacketPayload.codec(
            DesktopMutationAckPayload::write,
            DesktopMutationAckPayload::new
        );

        private DesktopMutationAckPayload(RegistryFriendlyByteBuf buf) {
            this(buf.readLong(), buf.readLong(), buf.readVarLong(), buf.readBoolean());
        }

        public DesktopMutationAckPayload {
            if (connectionNonce == 0L || playerSessionToken == 0L || mutationId <= 0L) {
                throw new IllegalArgumentException("Invalid desktop mutation acknowledgement");
            }
        }

        private void write(RegistryFriendlyByteBuf buf) {
            buf.writeLong(this.connectionNonce);
            buf.writeLong(this.playerSessionToken);
            buf.writeVarLong(this.mutationId);
            buf.writeBoolean(this.sequenceAccepted);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record DesktopGhostRecipePayload(int sessionId, ResourceLocation recipeId) implements CustomPacketPayload {
        public static final Type<DesktopGhostRecipePayload> TYPE = new Type<>(id("desktop_ghost_recipe"));
        public static final StreamCodec<RegistryFriendlyByteBuf, DesktopGhostRecipePayload> CODEC = CustomPacketPayload.codec(
            DesktopGhostRecipePayload::write,
            DesktopGhostRecipePayload::new
        );

        private DesktopGhostRecipePayload(RegistryFriendlyByteBuf buf) {
            this(buf.readVarInt(), buf.readResourceLocation());
        }

        private void write(RegistryFriendlyByteBuf buf) {
            buf.writeVarInt(this.sessionId);
            buf.writeResourceLocation(this.recipeId);
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
