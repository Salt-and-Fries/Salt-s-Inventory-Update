package com.salts_inventory_update.network;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.netty.buffer.Unpooled;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffers;

import com.salts_inventory_update.SaltsInventoryUpdate;
import com.salts_inventory_update.protocol.DesktopProtocol;

public final class DesktopPackets {
    public static final int NETWORK_PROTOCOL_VERSION = DesktopProtocol.VERSION;
    public static final ResourceLocation LEGACY_READY_TYPE = new ResourceLocation(SaltsInventoryUpdate.MOD_ID, "desktop_ready");
    private static final int CUSTOM_PAYLOAD_MAX_BYTES = DesktopProtocol.MAX_CUSTOM_DATA_BYTES;
    private static final int ITEM_LIST_MAX_SIZE = DesktopProtocol.MAX_OPEN_SESSION_ITEMS;
    private static final int DATA_LIST_MAX_SIZE = DesktopProtocol.MAX_MENU_DATA_VALUES;
    private static final int MENU_SOURCE_KEY_MAX_LENGTH = DesktopProtocol.MAX_SOURCE_TEXT_LENGTH;
    private static final int CLICK_INPUT_MAX_LENGTH = 32;
    private static final int IDENTIFIER_MAX_LENGTH = 128;
    private static final int HORSE_MENU_MAX_COLUMNS = DesktopProtocol.MAX_MOUNT_COLUMNS;
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
    public static final int MAX_GESTURE_SLOTS = 256;
    public static final long CAP_MULTI_MENU_GESTURES = 1L << 4;
    public static final long CAP_SORT_WINDOWS = 1L << 5;

    private DesktopPackets() {
    }

    public static void registerPayloadTypes() {
    }

    public static ResourceLocation id(String path) {
        return new ResourceLocation(SaltsInventoryUpdate.MOD_ID, "v" + NETWORK_PROTOCOL_VERSION + "/" + path);
    }

    public static int menuTypeId(MenuType<?> menuType) {
        return BuiltInRegistries.MENU.getId(menuType);
    }

    public static MenuType<?> menuTypeById(int id) {
        return BuiltInRegistries.MENU.byId(id);
    }

    public static FriendlyByteBuf toBuffer(DesktopPacket packet) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        try {
            packet.write(buf);
            return buf;
        } catch (RuntimeException | Error exception) {
            buf.release();
            throw exception;
        }
    }

    private static void writeItemList(FriendlyByteBuf buf, List<ItemStack> stacks) {
        if (stacks.size() > ITEM_LIST_MAX_SIZE) {
            throw new IllegalArgumentException("Desktop item list is too large: " + stacks.size());
        }
        buf.writeVarInt(stacks.size());
        for (ItemStack stack : stacks) {
            buf.writeItem(stack);
        }
    }

    private static List<ItemStack> readItemList(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        if (size < 0 || size > ITEM_LIST_MAX_SIZE) {
            throw new IllegalArgumentException("Desktop item list is too large: " + size);
        }
        List<ItemStack> stacks = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            stacks.add(buf.readItem());
        }
        return stacks;
    }

    private static void writeLimitedStringList(FriendlyByteBuf buf, List<String> values, int maxSize, int maxLength) {
        if (values.size() > maxSize) {
            throw new IllegalArgumentException("Desktop string list is too large: " + values.size());
        }
        buf.writeVarInt(values.size());
        for (String value : values) {
            buf.writeUtf(value, maxLength);
        }
    }

    private static List<String> readLimitedStringList(FriendlyByteBuf buf, int maxSize, int maxLength) {
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

    private static void writeSlotReference(FriendlyByteBuf buf, DesktopSlotReference reference) {
        buf.writeVarInt(reference.sessionId());
        buf.writeLong(reference.sessionToken());
        buf.writeVarInt(reference.stateId());
        buf.writeVarInt(reference.slotIndex());
    }

    private static DesktopSlotReference readSlotReference(FriendlyByteBuf buf) {
        return new DesktopSlotReference(buf.readVarInt(), buf.readLong(), buf.readVarInt(), buf.readVarInt());
    }

    private static void writeSessionReference(FriendlyByteBuf buf, DesktopSessionReference reference) {
        buf.writeVarInt(reference.sessionId());
        buf.writeLong(reference.sessionToken());
        buf.writeVarInt(reference.stateId());
    }

    private static DesktopSessionReference readSessionReference(FriendlyByteBuf buf) {
        return new DesktopSessionReference(buf.readVarInt(), buf.readLong(), buf.readVarInt());
    }

    private static void writeSlotReferences(FriendlyByteBuf buf, List<DesktopSlotReference> references) {
        requireUniqueBounded(references, MAX_GESTURE_SLOTS, reference -> ((long) reference.sessionId() << 32) ^ reference.slotIndex());
        buf.writeVarInt(references.size());
        references.forEach(reference -> writeSlotReference(buf, reference));
    }

    private static List<DesktopSlotReference> readSlotReferences(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        DesktopProtocol.requireCount("desktop gesture slots", size, MAX_GESTURE_SLOTS);
        List<DesktopSlotReference> result = new ArrayList<>(size);
        for (int i = 0; i < size; i++) result.add(readSlotReference(buf));
        requireUniqueBounded(result, MAX_GESTURE_SLOTS, reference -> ((long) reference.sessionId() << 32) ^ reference.slotIndex());
        return List.copyOf(result);
    }

    private static void writeSessionReferences(FriendlyByteBuf buf, List<DesktopSessionReference> references) {
        requireUniqueBounded(references, DesktopProtocol.MAX_DESKTOP_SESSIONS + 1, reference -> (long) reference.sessionId());
        buf.writeVarInt(references.size());
        references.forEach(reference -> writeSessionReference(buf, reference));
    }

    private static List<DesktopSessionReference> readSessionReferences(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        DesktopProtocol.requireCount("desktop gesture sessions", size, DesktopProtocol.MAX_DESKTOP_SESSIONS + 1);
        List<DesktopSessionReference> result = new ArrayList<>(size);
        for (int i = 0; i < size; i++) result.add(readSessionReference(buf));
        requireUniqueBounded(result, DesktopProtocol.MAX_DESKTOP_SESSIONS + 1, reference -> (long) reference.sessionId());
        return List.copyOf(result);
    }

    private static <T> void requireUniqueBounded(List<T> values, int maximum, java.util.function.Function<T, Long> identity) {
        if (values == null || values.isEmpty() || values.size() > maximum) {
            throw new IllegalArgumentException("Invalid desktop gesture list size");
        }
        Set<Long> seen = new HashSet<>();
        for (T value : values) if (!seen.add(identity.apply(value))) throw new IllegalArgumentException("Duplicate desktop gesture target");
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

        public InventoryExpansionSyncPayload {
            DesktopProtocol.requireCount("inventory expansion slots", slotCount, DesktopProtocol.MAX_EXPANSION_SLOTS);
            items = List.copyOf(items);
            if (items.size() != slotCount) {
                throw new IllegalArgumentException("Desktop inventory expansion size mismatch: slots=" + slotCount + ", items=" + items.size());
            }
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

    public record DesktopHelloPayload(
        int protocolVersion,
        long clientNonce,
        long capabilities,
        boolean uiEnabled,
        List<String> forcedMenuIds
    ) implements DesktopPacket {
        public static final ResourceLocation TYPE = DesktopPackets.id("desktop_hello");

        public DesktopHelloPayload(FriendlyByteBuf buf) {
            this(
                buf.readVarInt(),
                buf.readLong(),
                buf.readLong(),
                buf.readBoolean(),
                readLimitedStringList(
                    buf,
                    DesktopProtocol.MAX_FORCED_MENU_IDS,
                    DesktopProtocol.MAX_IDENTIFIER_LENGTH
                )
            );
        }

        public DesktopHelloPayload {
            forcedMenuIds = List.copyOf(forcedMenuIds);
        }

        @Override
        public ResourceLocation id() {
            return TYPE;
        }

        @Override
        public void write(FriendlyByteBuf buf) {
            buf.writeVarInt(this.protocolVersion);
            buf.writeLong(this.clientNonce);
            buf.writeLong(this.capabilities);
            buf.writeBoolean(this.uiEnabled);
            writeLimitedStringList(
                buf,
                this.forcedMenuIds,
                DesktopProtocol.MAX_FORCED_MENU_IDS,
                DesktopProtocol.MAX_IDENTIFIER_LENGTH
            );
        }
    }

    public record DesktopSlotReference(int sessionId, long sessionToken, int stateId, int slotIndex) {
        public DesktopSlotReference {
            if (sessionId < PLAYER_MENU_SESSION || sessionToken == 0L || stateId < 0 || slotIndex < 0 || slotIndex >= ITEM_LIST_MAX_SIZE) {
                throw new IllegalArgumentException("Invalid desktop slot reference");
            }
        }
    }

    public record DesktopSessionReference(int sessionId, long sessionToken, int stateId) {
        public DesktopSessionReference {
            if (sessionId < PLAYER_MENU_SESSION || sessionToken == 0L || stateId < 0) throw new IllegalArgumentException("Invalid desktop session reference");
        }
    }

    public record DesktopDragSlotsPayload(int quickCraftType, List<DesktopSlotReference> slots) implements DesktopPacket {
        public static final ResourceLocation TYPE = DesktopPackets.id("desktop_drag_slots");
        public DesktopDragSlotsPayload(FriendlyByteBuf buf) { this(buf.readVarInt(), readSlotReferences(buf)); }
        public DesktopDragSlotsPayload { if (quickCraftType < 0 || quickCraftType > 2) throw new IllegalArgumentException("Invalid quick craft type"); slots = List.copyOf(slots); requireUniqueBounded(slots, MAX_GESTURE_SLOTS, r -> ((long) r.sessionId() << 32) ^ r.slotIndex()); }
        @Override public ResourceLocation id() { return TYPE; }
        @Override public void write(FriendlyByteBuf buf) { buf.writeVarInt(quickCraftType); writeSlotReferences(buf, slots); }
    }

    public record DesktopPickupAllPayload(int anchorSessionId, int anchorSlotIndex, int button, List<DesktopSessionReference> sources) implements DesktopPacket {
        public static final ResourceLocation TYPE = DesktopPackets.id("desktop_pickup_all");
        public DesktopPickupAllPayload(FriendlyByteBuf buf) { this(buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), readSessionReferences(buf)); }
        public DesktopPickupAllPayload { if (anchorSessionId < 0 || anchorSlotIndex < 0 || (button != 0 && button != 1)) throw new IllegalArgumentException("Invalid pickup all"); sources = List.copyOf(sources); requireUniqueBounded(sources, DesktopProtocol.MAX_DESKTOP_SESSIONS + 1, r -> (long) r.sessionId()); }
        @Override public ResourceLocation id() { return TYPE; }
        @Override public void write(FriendlyByteBuf buf) { buf.writeVarInt(anchorSessionId); buf.writeVarInt(anchorSlotIndex); buf.writeVarInt(button); writeSessionReferences(buf, sources); }
    }

    public record DesktopSortWindowsPayload(
        DesktopSessionReference source, List<DesktopSessionReference> destinations, int focusedSessionId, boolean shift
    ) implements DesktopPacket {
        public static final ResourceLocation TYPE = DesktopPackets.id("desktop_sort_windows");
        public DesktopSortWindowsPayload(FriendlyByteBuf buf) {
            this(readSessionReference(buf), readSessionReferences(buf), buf.readVarInt(), buf.readBoolean());
        }
        public DesktopSortWindowsPayload {
            if (source == null) throw new IllegalArgumentException("Invalid sort source");
            destinations = List.copyOf(destinations);
            requireUniqueBounded(destinations, DesktopProtocol.MAX_DESKTOP_SESSIONS, r -> (long) r.sessionId());
        }
        @Override public ResourceLocation id() { return TYPE; }
        @Override public void write(FriendlyByteBuf buf) {
            writeSessionReference(buf, source); writeSessionReferences(buf, destinations);
            buf.writeVarInt(focusedSessionId); buf.writeBoolean(shift);
        }
    }

    public record DesktopQuickMoveAllPayload(List<DesktopSlotReference> sources, int targetKind, DesktopSessionReference target) implements DesktopPacket {
        public static final ResourceLocation TYPE = DesktopPackets.id("desktop_quick_move_all");
        public DesktopQuickMoveAllPayload(FriendlyByteBuf buf) { this(readSlotReferences(buf), buf.readVarInt(), readSessionReference(buf)); }
        public DesktopQuickMoveAllPayload { if (targetKind < QUICK_TARGET_DEFAULT || targetKind > QUICK_TARGET_HOTBAR || target == null) throw new IllegalArgumentException("Invalid quick move all"); sources = List.copyOf(sources); requireUniqueBounded(sources, MAX_GESTURE_SLOTS, r -> ((long) r.sessionId() << 32) ^ r.slotIndex()); }
        @Override public ResourceLocation id() { return TYPE; }
        @Override public void write(FriendlyByteBuf buf) { writeSlotReferences(buf, sources); buf.writeVarInt(targetKind); writeSessionReference(buf, target); }
    }

    public record DesktopHelloAckPayload(
        int protocolVersion,
        long echoedClientNonce,
        long connectionNonce,
        long capabilities,
        long playerMenuToken,
        boolean uiEnabled,
        boolean accepted
    ) implements DesktopPacket {
        public static final ResourceLocation TYPE = DesktopPackets.id("desktop_hello_ack");

        public DesktopHelloAckPayload(FriendlyByteBuf buf) {
            this(buf.readVarInt(), buf.readLong(), buf.readLong(), buf.readLong(), buf.readLong(), buf.readBoolean(), buf.readBoolean());
        }

        @Override
        public ResourceLocation id() {
            return TYPE;
        }

        @Override
        public void write(FriendlyByteBuf buf) {
            buf.writeVarInt(this.protocolVersion);
            buf.writeLong(this.echoedClientNonce);
            buf.writeLong(this.connectionNonce);
            buf.writeLong(this.capabilities);
            buf.writeLong(this.playerMenuToken);
            buf.writeBoolean(this.uiEnabled);
            buf.writeBoolean(this.accepted);
        }
    }

    public record DesktopModePayload(
        long connectionNonce,
        long sequence,
        boolean uiEnabled,
        List<String> forcedMenuIds
    ) implements DesktopPacket {
        public static final ResourceLocation TYPE = DesktopPackets.id("desktop_mode");

        public DesktopModePayload(FriendlyByteBuf buf) {
            this(
                buf.readLong(),
                buf.readVarLong(),
                buf.readBoolean(),
                readLimitedStringList(
                    buf,
                    DesktopProtocol.MAX_FORCED_MENU_IDS,
                    DesktopProtocol.MAX_IDENTIFIER_LENGTH
                )
            );
        }

        public DesktopModePayload {
            forcedMenuIds = List.copyOf(forcedMenuIds);
        }

        @Override
        public ResourceLocation id() {
            return TYPE;
        }

        @Override
        public void write(FriendlyByteBuf buf) {
            buf.writeLong(this.connectionNonce);
            buf.writeVarLong(this.sequence);
            buf.writeBoolean(this.uiEnabled);
            writeLimitedStringList(
                buf,
                this.forcedMenuIds,
                DesktopProtocol.MAX_FORCED_MENU_IDS,
                DesktopProtocol.MAX_IDENTIFIER_LENGTH
            );
        }
    }

    public record DesktopAuthenticatedPayload(
        long connectionNonce,
        long primarySessionToken,
        long secondarySessionToken,
        int primaryStateId,
        int secondaryStateId,
        ResourceLocation innerType,
        byte[] data
    ) implements DesktopPacket {
        public static final ResourceLocation TYPE = DesktopPackets.id("desktop_authenticated");

        public DesktopAuthenticatedPayload(FriendlyByteBuf buf) {
            this(
                buf.readLong(),
                buf.readLong(),
                buf.readLong(),
                buf.readVarInt(),
                buf.readVarInt(),
                new ResourceLocation(buf.readUtf(128)),
                buf.readByteArray(DesktopProtocol.MAX_ENVELOPE_BYTES)
            );
        }

        public DesktopAuthenticatedPayload {
            if (data.length > DesktopProtocol.MAX_ENVELOPE_BYTES) {
                throw new IllegalArgumentException("Authenticated desktop payload is too large: " + data.length);
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
            buf.writeLong(this.connectionNonce);
            buf.writeLong(this.primarySessionToken);
            buf.writeLong(this.secondarySessionToken);
            buf.writeVarInt(this.primaryStateId);
            buf.writeVarInt(this.secondaryStateId);
            buf.writeUtf(this.innerType.toString(), 128);
            buf.writeByteArray(this.data);
        }
    }

    public record DesktopSessionAuthorizationPayload(int sessionId, long sessionToken, long sourceGrantToken) implements DesktopPacket {
        public static final ResourceLocation TYPE = DesktopPackets.id("desktop_session_authorization");

        public DesktopSessionAuthorizationPayload(FriendlyByteBuf buf) {
            this(buf.readVarInt(), buf.readLong(), buf.readLong());
        }

        @Override
        public ResourceLocation id() {
            return TYPE;
        }

        @Override
        public void write(FriendlyByteBuf buf) {
            buf.writeVarInt(this.sessionId);
            buf.writeLong(this.sessionToken);
            buf.writeLong(this.sourceGrantToken);
        }
    }

    public record DesktopClickPayload(int debugId, int sessionId, int slotIndex, int button, String inputName, ItemStack clientCarried) implements DesktopPacket {
        public static final ResourceLocation TYPE = DesktopPackets.id("desktop_click");

        public DesktopClickPayload(FriendlyByteBuf buf) {
            this(buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readUtf(CLICK_INPUT_MAX_LENGTH), buf.readItem());
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
            buf.writeUtf(this.inputName, CLICK_INPUT_MAX_LENGTH);
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
            this(buf.readVarInt(), new ResourceLocation(buf.readUtf(IDENTIFIER_MAX_LENGTH)), buf.readBoolean());
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

    public record DesktopJeiTransferPayload(int targetSessionId, ResourceLocation recipeId, boolean maxTransfer) implements DesktopPacket {
        public static final ResourceLocation TYPE = DesktopPackets.id("desktop_jei_transfer");

        public DesktopJeiTransferPayload {
            if (recipeId == null) {
                throw new IllegalArgumentException("Desktop JEI transfer recipe id is required");
            }
        }

        public DesktopJeiTransferPayload(FriendlyByteBuf buf) {
            this(
                buf.readVarInt(),
                new ResourceLocation(buf.readUtf(IDENTIFIER_MAX_LENGTH)),
                buf.readBoolean()
            );
        }

        @Override
        public ResourceLocation id() {
            return TYPE;
        }

        @Override
        public void write(FriendlyByteBuf buf) {
            buf.writeVarInt(this.targetSessionId);
            buf.writeResourceLocation(this.recipeId);
            buf.writeBoolean(this.maxTransfer);
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

        public int dataLength() {
            return this.data.length;
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

    public record DesktopSourceLinkPayload(
        int firstSessionId,
        long firstSourceGrantToken,
        int secondSessionId,
        long secondSourceGrantToken,
        boolean linked
    ) implements DesktopPacket {
        public static final ResourceLocation TYPE = DesktopPackets.id("desktop_source_link");

        public DesktopSourceLinkPayload(FriendlyByteBuf buf) {
            this(buf.readVarInt(), buf.readLong(), buf.readVarInt(), buf.readLong(), buf.readBoolean());
        }

        public DesktopSourceLinkPayload {
            if (firstSessionId <= 0 || secondSessionId <= 0 || firstSessionId == secondSessionId
                || firstSourceGrantToken == 0L || secondSourceGrantToken == 0L
                || firstSourceGrantToken == secondSourceGrantToken) {
                throw new IllegalArgumentException("Desktop source-link grants are invalid");
            }
        }

        @Override
        public ResourceLocation id() {
            return TYPE;
        }

        @Override
        public void write(FriendlyByteBuf buf) {
            buf.writeVarInt(this.firstSessionId);
            buf.writeLong(this.firstSourceGrantToken);
            buf.writeVarInt(this.secondSessionId);
            buf.writeLong(this.secondSourceGrantToken);
            buf.writeBoolean(this.linked);
        }
    }

    public record DesktopOpenLinkedSourcesPayload(long originSourceGrantToken) implements DesktopPacket {
        public static final ResourceLocation TYPE = DesktopPackets.id("desktop_open_linked_sources");

        public DesktopOpenLinkedSourcesPayload(FriendlyByteBuf buf) {
            this(buf.readLong());
        }

        public DesktopOpenLinkedSourcesPayload {
            if (originSourceGrantToken == 0L) {
                throw new IllegalArgumentException("Desktop linked-source origin grant is invalid");
            }
        }

        @Override
        public ResourceLocation id() {
            return TYPE;
        }

        @Override
        public void write(FriendlyByteBuf buf) {
            buf.writeLong(this.originSourceGrantToken);
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
        boolean transferSupported,
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
                buf.readBoolean(),
                buf.readUtf(MENU_SOURCE_KEY_MAX_LENGTH),
                buf.readComponent(),
                readItemList(buf),
                buf.readItem(),
                buf.readVarIntArray(DATA_LIST_MAX_SIZE)
            );
        }

        public DesktopOpenSessionPayload {
            if (sessionId <= 0 || columns < 0 || columns > HORSE_MENU_MAX_COLUMNS) {
                throw new IllegalArgumentException("Desktop session header is invalid: session=" + sessionId + ", columns=" + columns);
            }
            if (sourceKey.length() > MENU_SOURCE_KEY_MAX_LENGTH) {
                throw new IllegalArgumentException("Desktop source key is too long: " + sourceKey.length());
            }
            items = List.copyOf(items);
            data = data.clone();
            if (items.size() > ITEM_LIST_MAX_SIZE || data.length > DATA_LIST_MAX_SIZE) {
                throw new IllegalArgumentException("Desktop session state is too large: items=" + items.size() + ", data=" + data.length);
            }
        }

        @Override
        public int[] data() {
            return this.data.clone();
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
            buf.writeBoolean(this.transferSupported);
            buf.writeUtf(this.sourceKey, MENU_SOURCE_KEY_MAX_LENGTH);
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
