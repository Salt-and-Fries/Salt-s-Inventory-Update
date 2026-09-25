package com.salts_inventory_update.server;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;
import java.security.SecureRandom;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;

import io.netty.buffer.Unpooled;

import com.salts_inventory_update.platform.fabric.api.event.lifecycle.v1.ServerTickEvents;
import com.salts_inventory_update.platform.fabric.api.networking.v1.ServerPlayConnectionEvents;
import com.salts_inventory_update.platform.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.CompoundContainer;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.animal.camel.Camel;
import net.minecraft.world.entity.animal.horse.AbstractChestedHorse;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.animal.horse.Llama;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.AbstractFurnaceMenu;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.BeaconMenu;
import net.minecraft.world.inventory.CartographyTableMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerSynchronizer;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.inventory.HorseInventoryMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.StonecutterMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import net.minecraft.resources.ResourceLocation;

import com.salts_inventory_update.api.server.desktop.DesktopServerApi;
import com.salts_inventory_update.api.server.desktop.DesktopServerPayloadContext;
import com.salts_inventory_update.api.server.desktop.DesktopServerPayloadHandler;
import com.salts_inventory_update.api.server.desktop.DesktopServerSessionContext;
import com.salts_inventory_update.api.server.desktop.DesktopServerWindowHandler;
import com.salts_inventory_update.api.server.desktop.DesktopTransferDecision;
import com.salts_inventory_update.api.server.desktop.DesktopTransferRequest;
import com.salts_inventory_update.api.server.desktop.DesktopTransferRequirement;
import com.salts_inventory_update.api.server.desktop.DesktopTransferValidators;
import com.salts_inventory_update.compat.toms_storage.TomsStorageCompat;
import com.salts_inventory_update.debug.DesktopDebug;
import com.salts_inventory_update.inventory.InventoryExpansion;
import com.salts_inventory_update.inventory.InventoryExpansionAccess;
import com.salts_inventory_update.network.DesktopPackets;
import com.salts_inventory_update.network.DesktopMenuOpenDataPayload;
import com.salts_inventory_update.internal.desktop.DesktopMenuSlots;
import com.salts_inventory_update.internal.desktop.DesktopSortSlots;
import com.salts_inventory_update.internal.desktop.DesktopItemSourceLocks;
import com.salts_inventory_update.internal.desktop.DesktopSessionSynchronizer;
import com.salts_inventory_update.internal.desktop.DesktopSynchronizerOverride;
import com.salts_inventory_update.network.DesktopPackets.DesktopPacket;
import com.salts_inventory_update.network.DesktopPackets.DesktopAuthenticatedPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopButtonPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopCarriedPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopClickPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopDragSlotsPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopPickupAllPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopQuickMoveAllPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopSortWindowsPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopSlotReference;
import com.salts_inventory_update.network.DesktopPackets.DesktopSessionReference;
import com.salts_inventory_update.network.DesktopPackets.DesktopCloseSessionPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopCustomPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopDataPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopMerchantOffersPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopOpenLinkedSourcesPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopOpenSessionPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopGhostRecipePayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopJeiTransferPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopPlaceRecipePayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopQuickMovePayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopHelloAckPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopHelloPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopModePayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopSessionAuthorizationPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopRenamePayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopSessionClosedPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopSessionPinPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopSessionVisibilityPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopSlotPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopSourceLinkPayload;
import com.salts_inventory_update.network.DesktopPackets.InventorySlotPurchasePayload;
import com.salts_inventory_update.protocol.DesktopProtocol;
import com.salts_inventory_update.protocol.DesktopConnectionState;
import com.salts_inventory_update.protocol.BoundedTransferPlanner;
import com.salts_inventory_update.protocol.BoundedLinkGraph;
import com.salts_inventory_update.protocol.TokenBucket;

public final class DesktopContainerSessions {
    private static final long SUPPORTED_CAPABILITIES = DesktopProtocol.KNOWN_CAPABILITIES | DesktopPackets.CAP_MULTI_MENU_GESTURES | DesktopPackets.CAP_SORT_WINDOWS;
    private static final int MAX_SESSIONS = DesktopProtocol.MAX_DESKTOP_SESSIONS;
    private static final int MAX_SOURCE_GRANTS = DesktopProtocol.MAX_LINK_NODES;
    private static final int MAX_DORMANT_GHOST_SOURCES = DesktopProtocol.MAX_DORMANT_SOURCES;
    private static final int JEI_TRANSFER_MAX_CRAFTS = DesktopProtocol.MAX_TRANSFER_CRAFTS;
    private static final int JEI_TRANSFER_SEARCH_BUDGET = DesktopProtocol.MAX_TRANSFER_ATTEMPTS;
    private static final int DORMANT_GHOST_REOPEN_INTERVAL_TICKS = DesktopProtocol.DORMANT_PROBE_INTERVAL_TICKS;
    private static final int CRAFTER_INPUT_SLOT_COUNT = 9;
    private static final int CRAFTER_SLOT_STATE_ENABLED_FLAG = 16;
    private static final int FURNACE_RESULT_SLOT = 2;
    private static final int ANVIL_RESULT_SLOT = 2;
    private static final int CARTOGRAPHY_RESULT_SLOT = 2;
    private static final int GRINDSTONE_RESULT_SLOT = 2;
    private static final int MERCHANT_RESULT_SLOT = 2;
    private static final int SMITHING_RESULT_SLOT = 3;
    private static final int STONECUTTER_RESULT_SLOT = 1;
    private static final int BEACON_EFFECT_ID_MASK = 0xFFFF;
    private static final int BEACON_SECONDARY_EFFECT_SHIFT = 16;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final Map<ServerGamePacketListenerImpl, PlayerSessions> PLAYERS = new IdentityHashMap<>();
    private static final Map<ServerGamePacketListenerImpl, String> PENDING_USE_TARGETS = new IdentityHashMap<>();
    private static final Set<ServerGamePacketListenerImpl> PROTOCOL_REJECTED = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
    private static final Map<ResourceLocation, MutationRegistration<?>> MUTATIONS = new HashMap<>();
    private static final int MAX_QUICK_MOVE_TRANSACTION_WORK = 1_000_000;
    private static final int NO_SESSION = Integer.MIN_VALUE;
    private static final ThreadLocal<Consumer<FriendlyByteBuf>> OPENING_DATA_WRITER = new ThreadLocal<>();
    private static final ThreadLocal<NetworkSession> ACTIVE_NETWORK_SESSION = new ThreadLocal<>();
    private static final ThreadLocal<SessionTransition> ACTIVE_TRANSITION = new ThreadLocal<>();
    private static int shouldCaptureProbeLogs;

    private DesktopContainerSessions() {
    }

    public static void initialize() {
        DesktopDebug.log("server desktop session networking initialized");
        DesktopDebug.probe("server desktop session networking initialize start");
        ServerPlayNetworking.registerGlobalReceiver(DesktopPackets.LEGACY_READY_TYPE, (server, player, networkHandler, buf, sender) -> {
            try {
                buf.readBoolean();
                if (buf.isReadable()) {
                    throw new IllegalArgumentException("Trailing bytes in legacy desktop-ready payload: " + buf.readableBytes());
                }
                rejectProtocolConnection(
                    server,
                    player,
                    DesktopPackets.LEGACY_READY_TYPE,
                    "legacy desktop protocol",
                    Component.literal("Salts Inventory Update versions are incompatible. Update the mod on both client and server.")
                );
            } catch (RuntimeException | StackOverflowError exception) {
                rejectMalformedPayload(server, player, DesktopPackets.LEGACY_READY_TYPE, exception);
            }
        });
        register(DesktopHelloPayload.TYPE, DesktopHelloPayload::new, DesktopContainerSessions::hello);
        register(DesktopModePayload.TYPE, DesktopModePayload::new, DesktopContainerSessions::mode);
        register(DesktopAuthenticatedPayload.TYPE, DesktopAuthenticatedPayload::new, DesktopContainerSessions::authenticated);
        registerMutation(DesktopClickPayload.TYPE, DesktopClickPayload::new, 0L, DesktopClickPayload::sessionId, null, DesktopContainerSessions::click);
        registerMutation(DesktopDragSlotsPayload.TYPE, DesktopDragSlotsPayload::new, DesktopPackets.CAP_MULTI_MENU_GESTURES, null, null, DesktopContainerSessions::dragSlots);
        registerMutation(DesktopPickupAllPayload.TYPE, DesktopPickupAllPayload::new, DesktopPackets.CAP_MULTI_MENU_GESTURES, null, null, DesktopContainerSessions::pickupAll);
        registerMutation(DesktopSortWindowsPayload.TYPE, DesktopSortWindowsPayload::new, DesktopPackets.CAP_SORT_WINDOWS, null, null, DesktopContainerSessions::sortWindows);
        registerMutation(DesktopQuickMoveAllPayload.TYPE, DesktopQuickMoveAllPayload::new, DesktopPackets.CAP_MULTI_MENU_GESTURES, null, null, DesktopContainerSessions::quickMoveAll);
        registerMutation(DesktopQuickMovePayload.TYPE, DesktopQuickMovePayload::new, 0L, DesktopQuickMovePayload::sourceSessionId, DesktopContainerSessions::quickMoveTargetSession, DesktopContainerSessions::quickMove);
        registerMutation(DesktopButtonPayload.TYPE, DesktopButtonPayload::new, 0L, DesktopButtonPayload::sessionId, null, DesktopContainerSessions::button);
        registerMutation(DesktopPlaceRecipePayload.TYPE, DesktopPlaceRecipePayload::new, DesktopProtocol.CAP_RECIPE_TRANSFER, DesktopPlaceRecipePayload::sessionId, null, DesktopContainerSessions::placeRecipe);
        registerMutation(DesktopJeiTransferPayload.TYPE, DesktopJeiTransferPayload::new, DesktopProtocol.CAP_RECIPE_TRANSFER, DesktopJeiTransferPayload::targetSessionId, null, DesktopContainerSessions::transferJeiRecipe);
        registerMutation(DesktopRenamePayload.TYPE, DesktopRenamePayload::new, 0L, DesktopRenamePayload::sessionId, null, DesktopContainerSessions::rename);
        registerMutation(DesktopCloseSessionPayload.TYPE, DesktopCloseSessionPayload::new, 0L, DesktopCloseSessionPayload::sessionId, null, DesktopContainerSessions::closeSessionFromClient);
        registerMutation(DesktopSessionPinPayload.TYPE, DesktopSessionPinPayload::new, DesktopProtocol.CAP_LINK_GRAPH, DesktopSessionPinPayload::sessionId, null, DesktopContainerSessions::setSessionPin);
        registerMutation(DesktopSessionVisibilityPayload.TYPE, DesktopSessionVisibilityPayload::new, DesktopProtocol.CAP_LINK_GRAPH, DesktopSessionVisibilityPayload::sessionId, null, DesktopContainerSessions::setSessionVisibility);
        registerMutation(DesktopSourceLinkPayload.TYPE, DesktopSourceLinkPayload::new, DesktopProtocol.CAP_LINK_GRAPH, DesktopSourceLinkPayload::firstSessionId, DesktopSourceLinkPayload::secondSessionId, DesktopContainerSessions::setSourceLink);
        registerMutation(DesktopOpenLinkedSourcesPayload.TYPE, DesktopOpenLinkedSourcesPayload::new, DesktopProtocol.CAP_LINK_GRAPH | DesktopProtocol.CAP_CUSTOM_WINDOWS, null, null, DesktopContainerSessions::openLinkedSources);
        registerMutation(DesktopCustomPayload.TYPE, DesktopCustomPayload::new, DesktopProtocol.CAP_CUSTOM_WINDOWS, DesktopCustomPayload::sessionId, null, DesktopContainerSessions::customPayload);
        registerMutation(DesktopCarriedPayload.TYPE, DesktopCarriedPayload::new, 0L, payload -> DesktopPackets.PLAYER_MENU_SESSION, null, DesktopContainerSessions::carried);
        registerMutation(InventorySlotPurchasePayload.TYPE, InventorySlotPurchasePayload::new, 0L, payload -> DesktopPackets.PLAYER_MENU_SESSION, null, (player, payload) -> InventoryExpansion.tryPurchase(player));
        ServerTickEvents.END_SERVER_TICK.register(DesktopContainerSessions::tick);
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> disconnect(handler.player));
        DesktopDebug.probe("server desktop session networking initialize complete");
    }

    private static <P extends DesktopPacket> void register(ResourceLocation id, Function<FriendlyByteBuf, P> decoder, BiConsumer<ServerPlayer, P> handler) {
        DesktopDebug.probe("server desktop payload receiver register id={}", id);
        ServerPlayNetworking.registerGlobalReceiver(id, (server, player, networkHandler, buf, sender) -> {
            try {
                P payload = decoder.apply(buf);
                if (buf.isReadable()) {
                    throw new IllegalArgumentException("Trailing bytes in desktop payload " + id + ": " + buf.readableBytes());
                }
                server.execute(() -> {
                    try {
                        handler.accept(player, payload);
                    } catch (RuntimeException exception) {
                        DesktopDebug.warn(
                            "server desktop payload handler rejected player={} type={} reason={}",
                            player.getName().getString(),
                            id,
                            exception.toString()
                        );
                    }
                });
            } catch (RuntimeException | StackOverflowError exception) {
                rejectMalformedPayload(server, player, id, exception);
            }
        });
    }

    private static void rejectMalformedPayload(
        MinecraftServer server,
        ServerPlayer player,
        ResourceLocation id,
        Throwable exception
    ) {
        rejectProtocolConnection(server, player, id, exception.getClass().getSimpleName());
    }

    private static void rejectProtocolConnection(
        MinecraftServer server,
        ServerPlayer player,
        ResourceLocation id,
        String reason
    ) {
        rejectProtocolConnection(
            server,
            player,
            id,
            reason,
            Component.literal("Salt's Inventory Update network protocol error.")
        );
    }

    private static void rejectProtocolConnection(
        MinecraftServer server,
        ServerPlayer player,
        ResourceLocation id,
        String reason,
        Component disconnectMessage
    ) {
        server.execute(() -> {
            PENDING_USE_TARGETS.remove(player.connection);
            if (player.connection == null || !PROTOCOL_REJECTED.add(player.connection)) {
                return;
            }
            DesktopDebug.warn(
                "server desktop protocol connection rejected player={} type={} reason={}",
                player.getName().getString(),
                id,
                reason
            );
            player.connection.disconnect(disconnectMessage);
        });
    }

    private static <P extends DesktopPacket> void registerMutation(
        ResourceLocation id,
        Function<FriendlyByteBuf, P> decoder,
        long operationCapabilities,
        @Nullable ToIntFunction<P> primarySession,
        @Nullable ToIntFunction<P> secondarySession,
        BiConsumer<ServerPlayer, P> handler
    ) {
        MUTATIONS.put(id, new MutationRegistration<>(decoder, operationCapabilities, primarySession, secondarySession, handler));
    }

    private static void hello(ServerPlayer player, DesktopHelloPayload payload) {
        PENDING_USE_TARGETS.remove(player.connection);
        PlayerSessions existing = existingSessions(player);
        if (existing != null && existing.connectionState.phase() != DesktopConnectionState.Phase.UNNEGOTIATED) {
            MinecraftServer server = player.getServer();
            if (server != null) {
                rejectProtocolConnection(server, player, DesktopHelloPayload.TYPE, "duplicate hello");
            }
            return;
        }
        PlayerSessions sessions = sessions(player);
        sessions.resetForHello(player);
        long selectedCapabilities = payload.capabilities() & SUPPORTED_CAPABILITIES;
        boolean compatible = payload.protocolVersion() == DesktopProtocol.VERSION
            && payload.clientNonce() != 0L;
        if (!compatible) {
            sessions.connectionState.markIncompatible();
            InventoryExpansion.setTopologyEnabled(player, false);
            InventoryExpansion.setGameplayEnabled(player, false);
            send(player, new DesktopHelloAckPayload(
                DesktopProtocol.VERSION,
                payload.clientNonce(),
                0L,
                0L,
                0L,
                false,
                false
            ));
            return;
        }

        sessions.setForcedMenuIds(payload.forcedMenuIds());
        long connectionNonce = nextNonce();
        sessions.playerMenuToken = nextNonce();
        boolean uiEnabled = payload.uiEnabled();
        try {
            sessions.connectionState.begin(payload.clientNonce(), player.serverLevel().getGameTime());
        } catch (IllegalArgumentException | ArithmeticException exception) {
            sessions.connectionState.markIncompatible();
            return;
        }
        boolean accepted = sessions.connectionState.acknowledge(
            DesktopProtocol.VERSION,
            payload.clientNonce(),
            connectionNonce,
            selectedCapabilities,
            uiEnabled,
            SUPPORTED_CAPABILITIES
        );
        if (!accepted) {
            sessions.connectionState.markIncompatible();
            return;
        }

        boolean topologyEnabled = (selectedCapabilities & DesktopProtocol.CAP_INVENTORY_TOPOLOGY) != 0L;
        InventoryExpansion.setTopologyEnabled(player, topologyEnabled);
        InventoryExpansion.setGameplayEnabled(player, topologyEnabled && uiEnabled);
        if (topologyEnabled) {
            InventoryExpansion.appendMissingMenuSlots(player.inventoryMenu, player);
        }
        send(player, new DesktopHelloAckPayload(
            DesktopProtocol.VERSION,
            payload.clientNonce(),
            connectionNonce,
            selectedCapabilities,
            sessions.playerMenuToken,
            uiEnabled,
            true
        ));
        if (topologyEnabled) {
            InventoryExpansion.syncToClient(player);
        }
        DesktopDebug.log("server desktop negotiation accepted player={} capabilities={} uiEnabled={}", player.getName().getString(), selectedCapabilities, uiEnabled);
    }

    private static void mode(ServerPlayer player, DesktopModePayload payload) {
        PlayerSessions sessions = existingSessions(player);
        if (sessions == null || !sessions.modeRate.tryConsume(System.nanoTime())) {
            return;
        }
        boolean uiEnabled = payload.uiEnabled();
        if (!sessions.connectionState.updateMode(payload.connectionNonce(), payload.sequence(), uiEnabled)) {
            DesktopDebug.trace("server desktop mode rejected player={} sequence={}", player.getName().getString(), payload.sequence());
            return;
        }
        sessions.setForcedMenuIds(payload.forcedMenuIds());
        boolean topologyEnabled = sessions.hasCapability(DesktopProtocol.CAP_INVENTORY_TOPOLOGY, false);
        InventoryExpansion.setTopologyEnabled(player, topologyEnabled);
        if (!uiEnabled) {
            PENDING_USE_TARGETS.remove(player.connection);
            // The connection phase already rejects mutations. Keep expansion insertion active
            // until removed() has returned every transient menu input to the player.
            sessions.closeAll(player, true);
            InventoryExpansion.setGameplayEnabled(player, false);
        } else {
            InventoryExpansion.setGameplayEnabled(player, topologyEnabled);
            if (topologyEnabled) {
                InventoryExpansion.appendMissingMenuSlots(player.inventoryMenu, player);
                InventoryExpansion.syncToClient(player);
            }
        }
    }

    private static void authenticated(ServerPlayer player, DesktopAuthenticatedPayload payload) {
        PlayerSessions sessions = existingSessions(player);
        MutationRegistration<?> registration = MUTATIONS.get(payload.innerType());
        if (sessions == null || registration == null) {
            DesktopDebug.warn("server authenticated payload rejected player={} type={} reason=unknown-or-unnegotiated", player.getName().getString(), payload.innerType());
            return;
        }
        if (!sessions.isReady()
            || !sessions.connectionState.authorizes(payload.connectionNonce(), registration.operationCapabilities, true)
            || !sessions.operationRate.tryConsume(System.nanoTime())) {
            DesktopDebug.warn("server authenticated payload rejected player={} type={} reason=authorization-or-rate", player.getName().getString(), payload.innerType());
            return;
        }
        registration.dispatch(player, sessions, payload);
    }

    private static int quickMoveTargetSession(DesktopQuickMovePayload payload) {
        if (payload.targetKind() < DesktopPackets.QUICK_TARGET_DEFAULT
            || payload.targetKind() > DesktopPackets.QUICK_TARGET_HOTBAR) {
            throw new IllegalArgumentException("Invalid quick move target kind: " + payload.targetKind());
        }
        return payload.targetKind() == DesktopPackets.QUICK_TARGET_SESSION
            ? payload.targetSessionId()
            : DesktopPackets.PLAYER_MENU_SESSION;
    }

    private static long capabilityForSession(int sessionId) {
        return sessionId == DesktopPackets.PLAYER_MENU_SESSION
            ? DesktopProtocol.CAP_INVENTORY_TOPOLOGY
            : DesktopProtocol.CAP_CUSTOM_WINDOWS;
    }

    private static long nextNonce() {
        long nonce;
        do {
            nonce = SECURE_RANDOM.nextLong();
        } while (nonce == 0L);
        return nonce;
    }

    public static boolean shouldCapture(ServerPlayer player) {
        PlayerSessions sessions = existingSessions(player);
        boolean ready = sessions != null && sessions.canOpenSessions();
        probeShouldCapture(player, ready, false, ready ? "session-ready" : "explicit-ready-required");
        return ready;
    }

    public static void captureUseTarget(ServerPlayer player, BlockHitResult hitResult) {
        PlayerSessions sessions = existingSessions(player);
        if (sessions == null || !sessions.canOpenSessions()) {
            PENDING_USE_TARGETS.remove(player.connection);
            return;
        }
        if (hitResult.getType() != HitResult.Type.BLOCK) {
            return;
        }

        BlockPos position = hitResult.getBlockPos();
        if (!player.serverLevel().isInWorldBounds(position) || !player.serverLevel().hasChunkAt(position)) {
            PENDING_USE_TARGETS.remove(player.connection);
            return;
        }
        String sourceKey = sourceKeyForBlock(player, position);
        PENDING_USE_TARGETS.put(player.connection, sourceKey);
        DesktopDebug.trace("server use target player={} key={}", player.getName().getString(), sourceKey);
    }

    public static void clearUseTarget(ServerPlayer player) {
        PENDING_USE_TARGETS.remove(player.connection);
    }

    public static boolean hasOpenSessionForContainer(Player player, Container container) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return false;
        }

        PlayerSessions sessions = existingSessions(serverPlayer);
        if (sessions == null || !sessions.canOpenSessions()) {
            return false;
        }

        for (Session session : sessions.sessions.values()) {
            Container sessionContainer = containerForMenu(session.menu);
            if (sessionContainer != null && containsContainer(sessionContainer, container)) {
                DesktopDebug.trace(
                    "server chest opener owned by desktop player={} session={} title={}",
                    serverPlayer.getName().getString(),
                    session.sessionId,
                    session.title.getString()
                );
                return true;
            }
        }

        return false;
    }

    public static boolean hasOpenSessionMatching(Player player, Predicate<AbstractContainerMenu> predicate) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return false;
        }
        PlayerSessions sessions = existingSessions(serverPlayer);
        if (sessions == null || !sessions.canOpenSessions()) {
            return false;
        }
        for (Session session : sessions.sessions.values()) {
            if (predicate.test(session.menu)) {
                return true;
            }
        }
        return false;
    }

    public static @Nullable OptionalInt openMenuSession(ServerPlayer player, MenuProvider provider) {
        return openMenuSession(player, provider, null, true, false, true);
    }

    public static @Nullable OptionalInt openMenuSession(ServerPlayer player, MenuProvider provider, Consumer<FriendlyByteBuf> openingDataWriter) {
        if (OPENING_DATA_WRITER.get() != null) {
            return null;
        }
        OPENING_DATA_WRITER.set(openingDataWriter);
        try {
            return openMenuSession(player, provider);
        } finally {
            OPENING_DATA_WRITER.remove();
        }
    }

    public static boolean hasActiveSessionTransition(ServerPlayer player) {
        SessionTransition transition = ACTIVE_TRANSITION.get();
        return transition != null && transition.player() == player;
    }

    public static String blockSourceKey(ServerPlayer player, BlockPos pos) {
        return sourceKeyForBlock(player, pos);
    }

    public static int activeNetworkSessionId(ServerPlayer player) {
        NetworkSession active = ACTIVE_NETWORK_SESSION.get();
        return active != null && active.player() == player ? active.session().sessionId : -1;
    }

    public static boolean sendSessionPayload(ServerPlayer player, int sessionId, ResourceLocation channel, byte[] data) {
        PlayerSessions sessions = existingSessions(player);
        Session session = sessions == null ? null : sessions.sessions.get(sessionId);
        if (session == null || data == null || data.length > DesktopProtocol.MAX_CUSTOM_DATA_BYTES) {
            return false;
        }
        send(player, new DesktopCustomPayload(session.sessionId, channel, data));
        return true;
    }

    public static void withSessionTransition(ServerPlayer player, int sessionId, Runnable action) {
        PlayerSessions sessions = existingSessions(player);
        Session oldSession = sessions == null ? null : sessions.sessions.get(sessionId);
        if (oldSession == null || ACTIVE_TRANSITION.get() != null) {
            return;
        }
        AbstractContainerMenu previousMenu = player.containerMenu;
        ACTIVE_TRANSITION.set(new SessionTransition(player, oldSession));
        player.containerMenu = oldSession.menu;
        try {
            withNetworkSession(player, oldSession, action);
        } finally {
            player.containerMenu = previousMenu;
            ACTIVE_TRANSITION.remove();
        }
    }

    private static void withNetworkSession(ServerPlayer player, Session session, Runnable action) {
        NetworkSession previous = ACTIVE_NETWORK_SESSION.get();
        ACTIVE_NETWORK_SESSION.set(new NetworkSession(player, session));
        try {
            action.run();
        } finally {
            if (previous == null) {
                ACTIVE_NETWORK_SESSION.remove();
            } else {
                ACTIVE_NETWORK_SESSION.set(previous);
            }
        }
    }

    private static @Nullable OptionalInt openMenuSession(
        ServerPlayer player,
        MenuProvider provider,
        @Nullable String forcedSourceKey,
        boolean toggleExisting,
        boolean ghostPinned,
        boolean visibleToClient
    ) {
        if (provider == null) {
            DesktopDebug.warn("server capture skipped player={} reason=null-provider", player.getName().getString());
            return OptionalInt.empty();
        }

        PlayerSessions sessions = existingSessions(player);
        if (sessions == null || !sessions.canOpenSessions()) {
            PENDING_USE_TARGETS.remove(player.connection);
            return OptionalInt.empty();
        }
        SessionTransition transition = ACTIVE_TRANSITION.get();
        if (transition != null && transition.player() == player) {
            forcedSourceKey = transition.oldSession().sourceKey;
            toggleExisting = false;
            ghostPinned = transition.oldSession().ghostPinned;
            visibleToClient = transition.oldSession().visibleToClient;
        } else {
            transition = null;
        }
        byte[] openingData;
        Consumer<FriendlyByteBuf> openingDataWriter = OPENING_DATA_WRITER.get();
        if (openingDataWriter == null) {
            openingData = new byte[0];
        } else {
            FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
            try {
                openingDataWriter.accept(buffer);
                int length = buffer.readableBytes();
                if (length <= 0 || length > DesktopProtocol.MAX_CUSTOM_DATA_BYTES) {
                    throw new IllegalArgumentException("Opening data length out of bounds: " + length);
                }
                openingData = new byte[length];
                buffer.getBytes(buffer.readerIndex(), openingData);
            } finally {
                buffer.release();
            }
        }
        String sourceKey = forcedSourceKey == null ? sourceKeyForProvider(player, provider) : forcedSourceKey;
        if (sourceKey == null) {
            sourceKey = PENDING_USE_TARGETS.get(player.connection);
        }

        if (toggleExisting && sessions.closeBySourceKey(player, sourceKey, true)) {
            DesktopDebug.log("server toggle close player={} source={} title={}", player.getName().getString(), sourceKey, provider.getDisplayName().getString());
            return OptionalInt.empty();
        }

        AbstractContainerMenu menu = provider.createMenu(nextSessionId(player), player.getInventory(), player);
        if (menu == null) {
            DesktopDebug.warn("server capture skipped player={} title={} reason=null-menu", player.getName().getString(), provider.getDisplayName().getString());
            return OptionalInt.empty();
        }

        if (!isDesktopSupportedMenu(player, menu)) {
            ResourceLocation menuKey = BuiltInRegistries.MENU.getKey(menu.getType());
            DesktopDebug.log(
                "server capture skipped player={} title={} menu={} menuType={} source={} reason=unsupported-menu-vanilla-fallback",
                player.getName().getString(),
                provider.getDisplayName().getString(),
                menuKey,
                menu.getType(),
                sourceKey
            );
            menu.removed(player);
            return null;
        }
        if (DesktopMenuSlots.requiresOpeningData(menu)
            && (openingData.length == 0 || !ServerPlayNetworking.canSend(player, DesktopMenuOpenDataPayload.TYPE))) {
            menu.removed(player);
            return null;
        }
        if (sourceKey == null) {
            sourceKey = DesktopMenuSlots.sourceKey(player, menu, openingData);
        }
        if (!menu.stillValid(player)) {
            menu.setCarried(ItemStack.EMPTY);
            menu.removed(player);
            DesktopDebug.warn("server capture skipped player={} title={} source={} reason=invalid-menu", player.getName().getString(), provider.getDisplayName().getString(), sourceKey);
            return OptionalInt.empty();
        }

        Session session = new Session(
            menu.containerId,
            menu,
            provider.getDisplayName(),
            DesktopPackets.SPECIAL_GENERIC,
            -1,
            0,
            DesktopPackets.menuTypeId(menu.getType()),
            sourceKey == null ? "" : sourceKey,
            openingData,
            transition == null ? -1 : transition.oldSession().sessionId
        );
        session.ghostPinned = ghostPinned;
        session.visibleToClient = visibleToClient;
        if (isBlockBackedSourceKey(session.sourceKey)) {
            if (forcedSourceKey == null) {
                session.sourceGrantToken = sessions.authorizeSource(player, session.sourceKey);
            } else if (sessions.isSourceAuthorized(player, session.sourceKey)) {
                session.sourceGrantToken = sessions.sourceGrantToken(session.sourceKey);
            } else {
                menu.setCarried(ItemStack.EMPTY);
                menu.removed(player);
                DesktopDebug.warn("server capture skipped player={} source={} reason=expired-source-grant", player.getName().getString(), session.sourceKey);
                return OptionalInt.empty();
            }
        }
        if (transition == null) {
            sessions.add(player, session);
        } else if (!sessions.replace(player, transition.oldSession(), session)) {
            menu.setCarried(ItemStack.EMPTY);
            menu.removed(player);
            return OptionalInt.empty();
        }
        DesktopDebug.log(
            "server capture menu player={} session={} container={} type={} title={} source={} ghostPinned={} visible={}",
            player.getName().getString(),
            session.sessionId,
            menu.containerId,
            session.menuTypeId,
            provider.getDisplayName().getString(),
            session.sourceKey,
            session.ghostPinned,
            session.visibleToClient
        );
        return OptionalInt.of(session.sessionId);
    }

    public static void openHorseSession(ServerPlayer player, AbstractHorse horse, Container container) {
        PlayerSessions sessions = existingSessions(player);
        if (sessions == null || !sessions.canOpenSessions()) {
            return;
        }
        String sourceKey = sourceKeyForEntity(player, horse.getUUID());
        if (sessions.closeBySourceKey(player, sourceKey, true)) {
            DesktopDebug.log("server toggle close horse player={} source={}", player.getName().getString(), sourceKey);
            return;
        }

        int columns = horse instanceof AbstractChestedHorse chestedHorse && chestedHorse.hasChest()
            ? chestedHorse.getInventoryColumns()
            : 0;
        int specialKind = horseSpecialKind(horse);
        int sessionId = nextSessionId(player);
        HorseInventoryMenu menu = new HorseInventoryMenu(sessionId, player.getInventory(), container, horse);
        sessions.add(player, new Session(
            sessionId,
            menu,
            horse.getDisplayName(),
            specialKind,
            horse.getId(),
            columns,
            -1,
            sourceKey
        ));
        DesktopDebug.log("server capture horse player={} session={} entity={} kind={} columns={}", player.getName().getString(), sessionId, horse.getId(), specialKind, columns);
    }

    private static int horseSpecialKind(AbstractHorse horse) {
        if (horse instanceof Camel) {
            return DesktopPackets.SPECIAL_CAMEL;
        }
        if (horse instanceof Llama) {
            return DesktopPackets.SPECIAL_LLAMA;
        }
        return DesktopPackets.SPECIAL_HORSE;
    }

    private static boolean isDesktopSupportedMenu(ServerPlayer player, AbstractContainerMenu menu) {
        MenuType<?> type = menu.getType();
        ResourceLocation key = BuiltInRegistries.MENU.getKey(type);
        PlayerSessions sessions = existingSessions(player);
        if (key != null && sessions != null && sessions.forcedMenuIds.contains(key)) {
            DesktopDebug.log("server capture force-enabled menu={}", key);
            return true;
        }

        if (isKnownVanillaDesktopMenu(type)) {
            return true;
        }

        if (DesktopServerApi.hasWindowSupport(type)) {
            return true;
        }

        return false;
    }

    private static boolean isKnownVanillaDesktopMenu(MenuType<?> type) {
        return type == MenuType.GENERIC_9x1
            || type == MenuType.GENERIC_9x2
            || type == MenuType.GENERIC_9x3
            || type == MenuType.GENERIC_9x4
            || type == MenuType.GENERIC_9x5
            || type == MenuType.GENERIC_9x6
            || type == MenuType.GENERIC_3x3
            || type == MenuType.ANVIL
            || type == MenuType.BEACON
            || type == MenuType.BLAST_FURNACE
            || type == MenuType.BREWING_STAND
            || type == MenuType.CRAFTING
            || type == MenuType.ENCHANTMENT
            || type == MenuType.FURNACE
            || type == MenuType.GRINDSTONE
            || type == MenuType.HOPPER
            || type == MenuType.LOOM
            || type == MenuType.MERCHANT
            || type == MenuType.SHULKER_BOX
            || type == MenuType.SMITHING
            || type == MenuType.SMOKER
            || type == MenuType.CARTOGRAPHY_TABLE
            || type == MenuType.STONECUTTER;
    }

    public static boolean sendMerchantOffers(ServerPlayer player, int containerId, MerchantOffers offers, int villagerLevel, int villagerXp, boolean showProgress, boolean canRestock) {
        PlayerSessions sessions = existingSessions(player);
        if (sessions == null || !sessions.canOpenSessions() || !sessions.sessions.containsKey(containerId)) {
            return false;
        }

        if (sessions.sessions.get(containerId).menu instanceof MerchantMenu merchantMenu) {
            merchantMenu.setOffers(offers);
            merchantMenu.setMerchantLevel(villagerLevel);
            merchantMenu.setXp(villagerXp);
            merchantMenu.setShowProgressBar(showProgress);
            merchantMenu.setCanRestock(canRestock);
        }

        send(player, new DesktopMerchantOffersPayload(containerId, offers, villagerLevel, villagerXp, showProgress, canRestock));
        DesktopDebug.trace("server merchant offers player={} session={}", player.getName().getString(), containerId);
        return true;
    }

    private static void probeShouldCapture(ServerPlayer player, boolean result, boolean canSendOpen, String reason) {
        if (shouldCaptureProbeLogs >= 48 && !DesktopDebug.traceEnabled()) {
            return;
        }
        shouldCaptureProbeLogs++;
        PlayerSessions sessions = existingSessions(player);
        DesktopDebug.probe(
            "server shouldCapture player={} result={} reason={} negotiatedUi={} sessionsKnown={} sessionReady={} canSendOpen={} hasExpansionAccess={} playerClass={}",
            player.getName().getString(),
            result,
            reason,
            sessions != null && sessions.connectionState.isUiEnabled(),
            sessions != null,
            sessions != null && sessions.isReady(),
            canSendOpen,
            player instanceof InventoryExpansionAccess,
            player.getClass().getName()
        );
    }

    private static void disconnect(ServerPlayer player) {
        PlayerSessions sessions = PLAYERS.remove(player.connection);
        PENDING_USE_TARGETS.remove(player.connection);
        PROTOCOL_REJECTED.remove(player.connection);
        if (sessions != null) {
            DesktopDebug.log("server disconnect close player={} sessions={}", player.getName().getString(), sessions.sessions.size());
            sessions.closeAll(player, false);
        }
        InventoryExpansion.setGameplayEnabled(player, false);
        InventoryExpansion.setTopologyEnabled(player, false);
    }

    private static void tick(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            PlayerSessions sessions = existingSessions(player);
            if (sessions != null && sessions.isReady()) {
                sessions.tick(player);
            }
        }
    }

    private static void click(ServerPlayer player, DesktopClickPayload payload) {
        PlayerSessions sessions = existingSessions(player);
        if (sessions == null || !sessions.isReady()) {
            DesktopDebug.trace("server click dropped player={} session={} reason=not-ready", player.getName().getString(), payload.sessionId());
            return;
        }

        ClickType input;
        try {
            input = ClickType.valueOf(payload.inputName());
        } catch (IllegalArgumentException exception) {
            DesktopDebug.warn("server click dropped player={} session={} reason=bad-input input={}", player.getName().getString(), payload.sessionId(), payload.inputName());
            return;
        }

        if (payload.sessionId() == DesktopPackets.PLAYER_MENU_SESSION) {
            DesktopDebug.trace("server click player-menu id={} player={} slot={} button={} input={} clientCarried={}", payload.debugId(), player.getName().getString(), payload.slotIndex(), payload.button(), input, payload.clientCarried());
            clickMenu(payload.debugId(), player, sessions, player.inventoryMenu, payload.slotIndex(), payload.button(), input, payload.clientCarried());
            sessions.broadcastAll(player);
            return;
        }

        Session session = sessions.sessions.get(payload.sessionId());
        if (session == null) {
            DesktopDebug.trace("server click dropped player={} session={} reason=missing-session", player.getName().getString(), payload.sessionId());
            return;
        }
        if (!session.visibleToClient) {
            DesktopDebug.trace("server click dropped player={} session={} reason=hidden", player.getName().getString(), payload.sessionId());
            return;
        }

        DesktopDebug.trace("server click session id={} player={} session={} slot={} button={} input={} clientCarried={}", payload.debugId(), player.getName().getString(), payload.sessionId(), payload.slotIndex(), payload.button(), input, payload.clientCarried());
        clickMenu(payload.debugId(), player, sessions, session.menu, payload.slotIndex(), payload.button(), input, payload.clientCarried());
        sessions.broadcastAll(player);
    }

    private static void carried(ServerPlayer player, DesktopCarriedPayload payload) {
        PlayerSessions sessions = existingSessions(player);
        if (sessions == null || !sessions.isReady()) {
            DesktopDebug.trace("server carried dropped player={} reason=not-ready stack={}", player.getName().getString(), payload.carried());
            return;
        }

        if (!player.getAbilities().instabuild) {
            DesktopDebug.trace("server carried dropped player={} reason=not-creative stack={} serverCarried={}", player.getName().getString(), payload.carried(), player.inventoryMenu.getCarried());
            syncCarried(player, sessions);
            return;
        }

        setSharedCarried(player, sessions, payload.carried());
        DesktopDebug.trace("server carried sync player={} stack={}", player.getName().getString(), player.inventoryMenu.getCarried());
        syncCarried(player, sessions);
    }

    private static void dragSlots(ServerPlayer player, DesktopDragSlotsPayload payload) {
        PlayerSessions sessions = existingSessions(player);
        if (sessions == null || !sessions.isReady() || player.isSpectator()) return;
        ItemStack carried = player.inventoryMenu.getCarried().copy();
        if (carried.isEmpty() || DesktopItemSourceLocks.matchesLockedSource(player, carried)) return;
        List<GestureSlot> targets = new ArrayList<>();
        IdentityHashMap<Object, Set<Integer>> physical = new IdentityHashMap<>();
        for (DesktopSlotReference reference : payload.slots()) {
            AbstractContainerMenu menu = authorizedMenu(player, sessions, reference.sessionId(), reference.sessionToken(), reference.stateId());
            Slot slot = menu == null ? null : DesktopMenuSlots.slot(menu, reference.slotIndex());
            if (slot == null || DesktopItemSourceLocks.isSlotLocked(player, slot) || !slot.isActive()
                || !slot.mayPlace(carried) || !menu.canDragTo(slot) || !DesktopMenuSlots.canQuickCraft(menu, slot, carried)) return;
            Object owner = DesktopMenuSlots.physicalOwner(menu, slot);
            int index = DesktopMenuSlots.physicalIndex(menu, slot);
            if (!physical.computeIfAbsent(owner, ignored -> new HashSet<>()).add(index)) return;
            targets.add(new GestureSlot(menu, slot));
        }
        int remaining = carried.getCount();
        for (GestureSlot target : targets) {
            Slot slot = target.slot();
            ItemStack existing = slot.getItem();
            int existingCount = existing.isEmpty() ? 0 : existing.getCount();
            int placement = DesktopMenuSlots.quickCraftPlaceCount(target.menu(), slot, targets.size(), payload.quickCraftType(), carried);
            int maximum = DesktopMenuSlots.quickCraftMaxStackSize(target.menu(), slot, carried);
            int inserted = Math.max(0, Math.min(maximum - existingCount, placement));
            if (payload.quickCraftType() != AbstractContainerMenu.QUICKCRAFT_TYPE_CLONE) inserted = Math.min(inserted, remaining);
            if (inserted <= 0) continue;
            ItemStack result = carried.copy();
            result.setCount(existingCount + inserted);
            slot.set(result);
            slot.setChanged();
            if (payload.quickCraftType() != AbstractContainerMenu.QUICKCRAFT_TYPE_CLONE) remaining -= inserted;
        }
        if (payload.quickCraftType() != AbstractContainerMenu.QUICKCRAFT_TYPE_CLONE) {
            ItemStack remainder = carried.copy();
            remainder.setCount(remaining);
            setSharedCarried(player, sessions, remainder);
        }
        sessions.broadcastAll(player);
    }

    private static void pickupAll(ServerPlayer player, DesktopPickupAllPayload payload) {
        PlayerSessions sessions = existingSessions(player);
        if (sessions == null || !sessions.isReady() || player.isSpectator()) return;
        ItemStack carried = player.inventoryMenu.getCarried().copy();
        if (carried.isEmpty() || DesktopItemSourceLocks.matchesLockedSource(player, carried)) return;
        List<AbstractContainerMenu> menus = new ArrayList<>();
        for (DesktopSessionReference reference : payload.sources()) {
            AbstractContainerMenu menu = authorizedMenu(player, sessions, reference.sessionId(), reference.sessionToken(), reference.stateId());
            if (menu == null) return;
            menus.add(menu);
        }
        for (int pass = 0; pass < 2 && carried.getCount() < carried.getMaxStackSize(); pass++) {
            for (AbstractContainerMenu menu : menus) {
                List<Slot> slots = new ArrayList<>(DesktopMenuSlots.all(menu));
                if (payload.button() != 0) java.util.Collections.reverse(slots);
                for (Slot slot : slots) {
                    if (carried.getCount() >= carried.getMaxStackSize()) break;
                    ItemStack source = slot.getItem();
                    if (DesktopItemSourceLocks.isSlotLocked(player, slot) || !slot.isActive() || source.isEmpty()
                        || !slot.mayPickup(player) || !ItemStack.isSameItemSameTags(carried, source)
                        || !DesktopMenuSlots.allowsPickupAll(menu, carried, slot)) continue;
                    if (pass == 0 && source.getCount() == source.getMaxStackSize()) continue;
                    int amount = Math.min(source.getCount(), carried.getMaxStackSize() - carried.getCount());
                    ItemStack taken = slot.safeTake(source.getCount(), amount, player);
                    if (!taken.isEmpty() && ItemStack.isSameItemSameTags(carried, taken)) carried.grow(taken.getCount());
                }
            }
        }
        setSharedCarried(player, sessions, carried);
        sessions.broadcastAll(player);
    }

    private record SortDestination(SessionAuthorization authorization, List<Slot> slots, List<ItemStack> contents) {}

    private static void sortWindows(ServerPlayer player, DesktopSortWindowsPayload payload) {
        PlayerSessions sessions = existingSessions(player);
        if (sessions == null || !sessions.isReady() || player.isSpectator() || !player.inventoryMenu.getCarried().isEmpty()) return;
        var reference = payload.source();
        SessionAuthorization source = authorizeSortMenu(player, sessions, reference);
        if (source == null) return;
        AbstractContainerMenu sourceMenu = source.menu();
        boolean inventory = source.sessionId() == DesktopPackets.PLAYER_MENU_SESSION;
        if (!inventory && !DesktopSortSlots.isSource(sourceMenu)) return;
        List<Slot> sourceSlots = inventory ? mainInventorySlots(player) : containerSlots(sourceMenu, player).stream()
            .filter(slot -> DesktopSortSlots.sourceSlot(sourceMenu, slot)).toList();
        Set<Object> owners = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
        for (Slot slot : sourceSlots) owners.add(DesktopMenuSlots.physicalOwner(sourceMenu, slot));
        List<SortDestination> destinations = new ArrayList<>();
        Set<String> sourceKeys = new HashSet<>();
        if (source.session() != null && !source.session().sourceKey.isBlank()) sourceKeys.add(source.session().sourceKey);
        for (var targetRef : payload.destinations()) {
            if (targetRef.sessionId() == source.sessionId() || targetRef.sessionId() == DesktopPackets.PLAYER_MENU_SESSION) continue;
            SessionAuthorization target = authorizeSortMenu(player, sessions, targetRef);
            if (target == null) { resyncSortWindows(player, sessions); return; }
            if (!DesktopSortSlots.isStorage(target.menu())) continue;
            String key = target.session().sourceKey;
            if (!key.isBlank() && !sourceKeys.add(key)) continue;
            List<Slot> slots = containerSlots(target.menu(), player).stream()
                .filter(slot -> DesktopSortSlots.storageSlot(target.menu(), slot))
                .filter(slot -> slot.isActive()).toList();
            if (slots.isEmpty() || slots.stream().anyMatch(slot -> owners.contains(DesktopMenuSlots.physicalOwner(target.menu(), slot)))) continue;
            for (Slot slot : slots) owners.add(DesktopMenuSlots.physicalOwner(target.menu(), slot));
            destinations.add(new SortDestination(target, slots, slots.stream().map(slot -> slot.getItem().copy()).toList()));
        }
        // Capture sources as well: output callbacks must not introduce new work into this action.
        List<SlotSource> sources = new ArrayList<>();
        List<ItemStack> snapshots = new ArrayList<>();
        IdentityHashMap<Object, Set<Integer>> seen = new IdentityHashMap<>();
        for (Slot slot : sourceSlots) {
            if (!slot.hasItem() || !slot.isActive() || !slot.mayPickup(player)
                || DesktopItemSourceLocks.isSlotLocked(player, slot)
                || !DesktopMenuSlots.allowsTargetedQuickMoveSource(sourceMenu, slot)
                || DesktopMenuSlots.repeatedQuickMoveLimit(sourceMenu, slot, slot.getItem()) <= 0
                || !rememberPhysicalSlot(seen, sourceMenu, slot)) continue;
            sources.add(new SlotSource(source.sessionId(), sourceMenu, slot, source.session()));
            snapshots.add(slot.getItem().copy());
        }
        try {
            executeSortTransfers(player, sessions, sources, snapshots, destinations, payload.focusedSessionId(), payload.shift());
        } finally {
            sessions.broadcastAll(player);
        }
    }

    private static void executeSortTransfers(ServerPlayer player, PlayerSessions sessions, List<SlotSource> sources,
        List<ItemStack> snapshots, List<SortDestination> destinations, int focusedSessionId, boolean shift) {
        QuickMoveWorkBudget budget = new QuickMoveWorkBudget(MAX_QUICK_MOVE_TRANSACTION_WORK);
        for (int index = 0; index < sources.size(); index++) {
            SlotSource slotSource = sources.get(index);
            ItemStack original = snapshots.get(index);
            if (!ItemStack.matches(original, slotSource.slot.getItem())) break;
            Set<SortDestination> attempted = new HashSet<>();
            for (int priority = 0; priority < (shift ? 4 : 1); priority++) {
                for (SortDestination target : destinations) {
                    if (!slotSource.slot.hasItem()) break;
                    boolean matches = switch (priority) {
                        case 0 -> target.contents().stream().anyMatch(stack -> !stack.isEmpty() && ItemStack.isSameItem(original, stack));
                        case 1 -> target.contents().stream().anyMatch(stack -> !stack.isEmpty()
                            && original.getTags().anyMatch(stack::is));
                        case 2 -> target.authorization().sessionId() == focusedSessionId;
                        default -> target.contents().stream().allMatch(ItemStack::isEmpty);
                    };
                    if (!matches || !attempted.add(target)) continue;
                    if (!slotSource.menu.stillValid(player) || !target.authorization().menu().stillValid(player)) continue;
                    List<Slot> unlocked = target.slots().stream()
                        .filter(slot -> !DesktopItemSourceLocks.isSlotLocked(player, slot)).toList();
                    moveSortStack(player, sessions, slotSource, target.authorization().menu(), unlocked, budget);
                }
            }
        }
    }

    /** Keep transaction validation bounded even for very large modded storage grids. */
    private static void moveSortStack(ServerPlayer player, PlayerSessions sessions, SlotSource source,
        AbstractContainerMenu targetMenu, List<Slot> slots, QuickMoveWorkBudget budget) {
        List<Slot> candidates = new ArrayList<>();
        ItemStack moving = source.slot.getItem();
        for (Slot slot : slots) {
            if (slot.hasItem() && ItemStack.isSameItemSameTags(moving, slot.getItem())) candidates.add(slot);
        }
        for (Slot slot : slots) if (!slot.hasItem()) candidates.add(slot);
        int index = 0;
        while (source.slot.hasItem() && index < candidates.size()) {
            List<Slot> batch = new ArrayList<>();
            long capacity = 0;
            ItemStack current = source.slot.getItem();
            while (index < candidates.size() && batch.size() < 16 && capacity < current.getCount()) {
                Slot slot = candidates.get(index++);
                if (!slot.isActive() || !slot.mayPlace(current)) continue;
                int room = slot.getMaxStackSize(current) - slot.getItem().getCount();
                if (room <= 0) continue;
                batch.add(slot);
                capacity += room;
            }
            if (!batch.isEmpty()) moveSlotStack(player, sessions, source, targetMenu, batch, budget, true);
        }
    }

    private static @Nullable SessionAuthorization authorizeSortMenu(
        ServerPlayer player, PlayerSessions sessions, DesktopSessionReference reference
    ) {
        AbstractContainerMenu menu = authorizedMenu(player, sessions, reference.sessionId(), reference.sessionToken(), reference.stateId());
        return menu == null ? null : new SessionAuthorization(reference.sessionId(), menu, sessions.sessions.get(reference.sessionId()), "");
    }

    private static void resyncSortWindows(ServerPlayer player, PlayerSessions sessions) {
        player.inventoryMenu.sendAllDataToRemote();
        for (Session session : sessions.sessions.values()) {
            if (session.visibleToClient) withNetworkSession(player, session, session.menu::sendAllDataToRemote);
        }
        syncCarried(player, sessions);
    }

    private static void quickMoveAll(ServerPlayer player, DesktopQuickMoveAllPayload payload) {
        PlayerSessions sessions = existingSessions(player);
        if (sessions == null || !sessions.isReady() || player.isSpectator()) return;
        AbstractContainerMenu target = authorizedMenu(player, sessions, payload.target().sessionId(), payload.target().sessionToken(), payload.target().stateId());
        if (target == null) return;
        for (DesktopSlotReference source : payload.sources()) {
            AbstractContainerMenu menu = authorizedMenu(player, sessions, source.sessionId(), source.sessionToken(), source.stateId());
            Slot slot = menu == null ? null : DesktopMenuSlots.slot(menu, source.slotIndex());
            if (slot == null || DesktopItemSourceLocks.isSlotLocked(player, slot)) return;
        }
        for (DesktopSlotReference source : payload.sources()) {
            quickMove(player, new DesktopQuickMovePayload(source.sessionId(), source.slotIndex(), payload.targetKind(), payload.target().sessionId()));
        }
    }

    private static @Nullable AbstractContainerMenu authorizedMenu(
        ServerPlayer player,
        PlayerSessions sessions,
        int sessionId,
        long token,
        int stateId
    ) {
        if (sessionId == DesktopPackets.PLAYER_MENU_SESSION) {
            return token == sessions.playerMenuToken && stateId == player.inventoryMenu.getStateId() ? player.inventoryMenu : null;
        }
        Session session = sessions.sessions.get(sessionId);
        return session != null && session.sessionToken == token && session.menu.getStateId() == stateId
            && session.visibleToClient && session.menu.stillValid(player) ? session.menu : null;
    }

    private record GestureSlot(AbstractContainerMenu menu, Slot slot) {
    }

    private static void quickMove(ServerPlayer player, DesktopQuickMovePayload payload) {
        PlayerSessions sessions = existingSessions(player);
        if (sessions == null || !sessions.isReady()) {
            DesktopDebug.trace("server quick move dropped player={} sourceSession={} reason=not-ready", player.getName().getString(), payload.sourceSessionId());
            return;
        }

        SlotSource source = resolveSlot(player, sessions, payload.sourceSessionId(), payload.sourceSlotIndex());
        if (source == null) {
            return;
        }

        if (payload.targetKind() == DesktopPackets.QUICK_TARGET_SESSION) {
            if (payload.targetSessionId() == source.sessionId) {
                DesktopDebug.trace(
                    "server quick move dropped player={} sourceSession={} targetSession={} reason=same-session",
                    player.getName().getString(),
                    payload.sourceSessionId(),
                    payload.targetSessionId()
                );
                return;
            }
            Session targetSession = sessions.sessions.get(payload.targetSessionId());
            if (targetSession == null || !targetSession.visibleToClient) {
                DesktopDebug.trace(
                    "server quick move dropped player={} sourceSession={} targetSession={} reason=invalid-target-session",
                    player.getName().getString(),
                    payload.sourceSessionId(),
                    payload.targetSessionId()
                );
                return;
            }
            if (!targetSession.menu.stillValid(player)) {
                DesktopDebug.log(
                    "server quick move invalid target player={} sourceSession={} targetSession={} title={}",
                    player.getName().getString(),
                    payload.sourceSessionId(),
                    payload.targetSessionId(),
                    targetSession.title.getString()
                );
                sessions.close(player, targetSession.sessionId, true);
                return;
            }
        }

        ItemStack carriedBeforeQuickMove = player.inventoryMenu.getCarried().copy();
        if (!carriedBeforeQuickMove.isEmpty()) {
            DesktopDebug.trace(
                "server quick move treating carried as empty player={} sourceSession={} sourceSlot={} carried={}",
                player.getName().getString(),
                payload.sourceSessionId(),
                payload.sourceSlotIndex(),
                carriedBeforeQuickMove
            );
        }

        if (payload.targetKind() != DesktopPackets.QUICK_TARGET_SESSION
            && isVanillaResultSource(source, payload.sourceSlotIndex())) {
            DesktopDebug.trace(
                "server quick move vanilla result player={} sourceSession={} sourceSlot={} menu={}",
                player.getName().getString(),
                payload.sourceSessionId(),
                payload.sourceSlotIndex(),
                source.menu.getClass().getSimpleName()
            );
            boolean restoreCarried = !carriedBeforeQuickMove.isEmpty();
            if (restoreCarried) {
                setSharedCarried(player, sessions, ItemStack.EMPTY);
            }
            try {
                clickMenu(0, player, sessions, source.menu, payload.sourceSlotIndex(), 0, ClickType.QUICK_MOVE, ItemStack.EMPTY);
            } finally {
                if (restoreCarried) {
                    setSharedCarried(player, sessions, carriedBeforeQuickMove);
                }
            }
            sessions.broadcastAll(player);
            return;
        }

        if (payload.targetKind() == DesktopPackets.QUICK_TARGET_SESSION && payload.targetSessionId() != source.sessionId) {
            Session targetSession = sessions.sessions.get(payload.targetSessionId());
            if (quickMoveIntoTomStorageTerminal(player, sessions, source, targetSession)) {
                return;
            }
        }

        List<net.minecraft.world.inventory.Slot> targets = quickMoveTargets(player, sessions, source, payload);
        if (targets.isEmpty()) {
            DesktopDebug.trace("server quick move dropped player={} sourceSession={} sourceSlot={} reason=no-targets", player.getName().getString(), payload.sourceSessionId(), payload.sourceSlotIndex());
            return;
        }

        boolean moved = moveSlotStack(player, source.slot, targets);
        DesktopDebug.trace(
            "server quick move player={} sourceSession={} sourceSlot={} targetKind={} targetSession={} moved={}",
            player.getName().getString(),
            payload.sourceSessionId(),
            payload.sourceSlotIndex(),
            payload.targetKind(),
            payload.targetSessionId(),
            moved
        );
        if (!moved) {
            return;
        }

        sessions.broadcastAll(player);
    }

    public static void playerDying(ServerPlayer player) {
        closeForPlayerReplacement(player);
    }

    public static void preparePlayerRestore(ServerPlayer oldPlayer, boolean keepContents) {
        boolean returnedInputs = closeForPlayerReplacement(oldPlayer);
        if (returnedInputs && !keepContents) {
            // Normally die() has already closed the menus before vanilla dropAll(). This is the
            // fallback for replacement paths that do not pass through die().
            oldPlayer.getInventory().dropAll();
        }
    }

    private static boolean closeForPlayerReplacement(ServerPlayer player) {
        PENDING_USE_TARGETS.remove(player.connection);
        PlayerSessions sessions = existingSessions(player);
        if (sessions == null) {
            return false;
        }
        sessions.replacingPlayer = true;
        boolean returnedInputs = !sessions.sessions.isEmpty();
        sessions.closeAll(player, true);
        return returnedInputs;
    }

    public static void playerRestored(ServerPlayer oldPlayer, ServerPlayer newPlayer) {
        PlayerSessions sessions = PLAYERS.remove(oldPlayer.connection);
        PENDING_USE_TARGETS.remove(oldPlayer.connection);
        PENDING_USE_TARGETS.remove(newPlayer.connection);
        if (sessions == null) {
            return;
        }
        sessions.replacingPlayer = false;
        sessions.nextSessionId = 1;
        sessions.dormantGhostProbeTicks = 0;
        PLAYERS.put(newPlayer.connection, sessions);
        if (sessions.connectionState.isNegotiated()) {
            sessions.playerMenuToken = nextNonce();
            send(newPlayer, new DesktopSessionAuthorizationPayload(
                DesktopPackets.PLAYER_MENU_SESSION,
                sessions.playerMenuToken,
                0L
            ));
        } else {
            sessions.playerMenuToken = 0L;
        }
    }

    private static boolean quickMoveIntoTomStorageTerminal(ServerPlayer player, PlayerSessions sessions, SlotSource source, @Nullable Session targetSession) {
        @Nullable MenuType<?> targetMenuType = targetSession == null ? null : targetSession.menuType();
        if (targetSession == null
            || !targetSession.visibleToClient
            || !targetSession.menu.stillValid(player)
            || targetMenuType == null
            || !TomsStorageCompat.isTerminal(targetMenuType)
            || !isPlayerInventorySlot(player, source.slot)
            || !source.slot.hasItem()) {
            return false;
        }

        int sourceContainerSlot = source.slot.getContainerSlot();
        int targetSlotIndex = -1;
        for (int i = 0; i < targetSession.menu.slots.size(); i++) {
            net.minecraft.world.inventory.Slot slot = targetSession.menu.slots.get(i);
            if (isPlayerInventorySlot(player, slot) && slot.getContainerSlot() == sourceContainerSlot) {
                targetSlotIndex = i;
                break;
            }
        }
        if (targetSlotIndex < 0) {
            DesktopDebug.trace(
                "server quick move tom terminal dropped player={} sourceSlot={} targetSession={} reason=no-matching-player-slot",
                player.getName().getString(),
                sourceContainerSlot,
                targetSession.sessionId
            );
            return false;
        }

        ItemStack carriedBeforeQuickMove = player.inventoryMenu.getCarried().copy();
        boolean restoreCarried = !carriedBeforeQuickMove.isEmpty();
        if (restoreCarried) {
            setSharedCarried(player, sessions, ItemStack.EMPTY);
        }
        ItemStack before = targetSession.menu.slots.get(targetSlotIndex).getItem().copy();
        try {
            targetSession.menu.quickMoveStack(player, targetSlotIndex);
        } finally {
            if (restoreCarried) {
                setSharedCarried(player, sessions, carriedBeforeQuickMove);
            }
        }
        ItemStack after = targetSession.menu.slots.get(targetSlotIndex).getItem();
        boolean moved = !ItemStack.matches(before, after);
        DesktopDebug.trace(
            "server quick move tom terminal player={} sourceSession={} sourceSlot={} targetSession={} targetSlot={} moved={} before={} after={}",
            player.getName().getString(),
            source.sessionId,
            source.slot.getContainerSlot(),
            targetSession.sessionId,
            targetSlotIndex,
            moved,
            before,
            after
        );
        if (!moved) {
            return false;
        }

        sessions.broadcastAll(player);
        return true;
    }

    private static boolean isVanillaResultSource(SlotSource source, int slotIndex) {
        if (source.menu instanceof RecipeBookMenu<?> recipeMenu && source.menu.slots.indexOf(source.slot) == recipeMenu.getResultSlotIndex()) {
            return true;
        }
        if (source.menu instanceof AbstractFurnaceMenu) {
            return slotIndex == FURNACE_RESULT_SLOT;
        }
        if (source.menu instanceof AnvilMenu) {
            return slotIndex == ANVIL_RESULT_SLOT;
        }
        if (source.menu instanceof CartographyTableMenu) {
            return slotIndex == CARTOGRAPHY_RESULT_SLOT;
        }
        if (source.menu instanceof GrindstoneMenu) {
            return slotIndex == GRINDSTONE_RESULT_SLOT;
        }
        if (source.menu instanceof MerchantMenu) {
            return slotIndex == MERCHANT_RESULT_SLOT;
        }
        if (source.menu instanceof SmithingMenu) {
            return slotIndex == SMITHING_RESULT_SLOT;
        }
        if (source.menu instanceof StonecutterMenu) {
            return slotIndex == STONECUTTER_RESULT_SLOT;
        }
        return false;
    }

    private static void button(ServerPlayer player, DesktopButtonPayload payload) {
        PlayerSessions sessions = existingSessions(player);
        if (sessions == null || !sessions.isReady()) {
            DesktopDebug.trace("server button dropped player={} session={} button={} reason=not-ready", player.getName().getString(), payload.sessionId(), payload.buttonId());
            return;
        }

        Session session = sessions.sessions.get(payload.sessionId());
        if (session == null) {
            DesktopDebug.trace("server button dropped player={} session={} button={} reason=missing-session", player.getName().getString(), payload.sessionId(), payload.buttonId());
            return;
        }
        if (!session.visibleToClient) {
            DesktopDebug.trace("server button dropped player={} session={} button={} reason=hidden", player.getName().getString(), payload.sessionId(), payload.buttonId());
            return;
        }

        if (!session.menu.stillValid(player)) {
            DesktopDebug.log("server button invalid player={} session={} title={}", player.getName().getString(), session.sessionId, session.title.getString());
            sessions.close(player, session.sessionId, true);
            return;
        }

        boolean clicked = withCanonicalCarried(player, sessions, session.menu, () -> {
            if (session.menu instanceof BeaconMenu beaconMenu) {
                return applyBeaconButton(beaconMenu, payload.buttonId());
            }
            if (session.menu instanceof MerchantMenu merchantMenu) {
                boolean validSelection = payload.buttonId() >= 0 && payload.buttonId() < merchantMenu.getOffers().size();
                if (validSelection) {
                    merchantMenu.setSelectionHint(payload.buttonId());
                    merchantMenu.tryMoveItems(payload.buttonId());
                }
                return validSelection;
            }
            return session.menu.clickMenuButton(player, payload.buttonId());
        });

        DesktopDebug.trace(
            "server button player={} session={} button={} clicked={}",
            player.getName().getString(),
            payload.sessionId(),
            payload.buttonId(),
            clicked
        );
        if (clicked) {
            sessions.broadcastAll(player);
        } else {
            syncCarried(player, sessions);
        }
    }

    private static void placeRecipe(ServerPlayer player, DesktopPlaceRecipePayload payload) {
        PlayerSessions sessions = existingSessions(player);
        if (sessions == null || !sessions.isReady()) {
            DesktopDebug.trace("server recipe place dropped player={} session={} recipe={} reason=not-ready", player.getName().getString(), payload.sessionId(), payload.recipeId());
            return;
        }

        Session session = sessions.sessions.get(payload.sessionId());
        if (session == null) {
            DesktopDebug.trace("server recipe place dropped player={} session={} recipe={} reason=missing-session", player.getName().getString(), payload.sessionId(), payload.recipeId());
            return;
        }
        if (!session.visibleToClient) {
            DesktopDebug.trace("server recipe place dropped player={} session={} recipe={} reason=hidden", player.getName().getString(), payload.sessionId(), payload.recipeId());
            return;
        }
        if (player.isSpectator()) {
            DesktopDebug.trace("server recipe place dropped player={} session={} recipe={} reason=spectator", player.getName().getString(), payload.sessionId(), payload.recipeId());
            return;
        }
        if (!session.menu.stillValid(player)) {
            DesktopDebug.log("server recipe place invalid player={} session={} title={}", player.getName().getString(), session.sessionId, session.title.getString());
            sessions.close(player, session.sessionId, true);
            return;
        }
        if (!(session.menu instanceof RecipeBookMenu recipeBookMenu)) {
            DesktopDebug.trace("server recipe place dropped player={} session={} recipe={} menu={} reason=not-recipe-menu", player.getName().getString(), payload.sessionId(), payload.recipeId(), session.menuTypeDescription());
            return;
        }

        MinecraftServer server = player.level().getServer();
        if (server == null) {
            DesktopDebug.trace("server recipe place dropped player={} session={} recipe={} reason=no-server", player.getName().getString(), payload.sessionId(), payload.recipeId());
            return;
        }

        Recipe<?> recipe = server.getRecipeManager().byKey(payload.recipeId()).orElse(null);
        if (recipe == null) {
            DesktopDebug.trace("server recipe place dropped player={} session={} recipe={} reason=missing-recipe", player.getName().getString(), payload.sessionId(), payload.recipeId());
            return;
        }

        if (!player.getRecipeBook().contains(recipe)) {
            DesktopDebug.trace("server recipe place dropped player={} session={} recipe={} recipeKey={} reason=not-unlocked", player.getName().getString(), payload.sessionId(), payload.recipeId(), recipe.getId());
            return;
        }
        if (recipe.isIncomplete()) {
            DesktopDebug.trace("server recipe place dropped player={} session={} recipe={} recipeKey={} reason=incomplete", player.getName().getString(), payload.sessionId(), payload.recipeId(), recipe.getId());
            return;
        }

        boolean canCraft = player.isCreative() || canCraftRecipe(recipeBookMenu, player, recipe);
        withCanonicalCarried(player, sessions, session.menu, () -> {
            recipeBookMenu.handlePlacement(
                payload.useMaxItems(),
                recipe,
                player
            );
            return null;
        });

        DesktopDebug.trace(
            "server recipe place player={} session={} recipe={} recipeKey={} useMax={} canCraft={} carried={}",
            player.getName().getString(),
            payload.sessionId(),
            payload.recipeId(),
            recipe.getId(),
            payload.useMaxItems(),
            canCraft,
            player.inventoryMenu.getCarried()
        );

        if (!canCraft) {
            send(player, new DesktopGhostRecipePayload(session.sessionId, recipe.getId()));
        }
        sessions.broadcastAll(player);
    }

    private static void transferJeiRecipe(ServerPlayer player, DesktopJeiTransferPayload payload) {
        PlayerSessions sessions = existingSessions(player);
        if (sessions == null || !sessions.isReady()) {
            DesktopDebug.trace("server JEI transfer dropped player={} targetSession={} reason=not-ready", player.getName().getString(), payload.targetSessionId());
            return;
        }
        if (player.isSpectator()) {
            DesktopDebug.trace("server JEI transfer dropped player={} targetSession={} reason=spectator", player.getName().getString(), payload.targetSessionId());
            return;
        }
        if (!sessions.allowJeiTransfer(player)) {
            DesktopDebug.trace("server JEI transfer dropped player={} targetSession={} reason=rate-limit", player.getName().getString(), payload.targetSessionId());
            return;
        }
        if (!player.inventoryMenu.getCarried().isEmpty()) {
            DesktopDebug.trace("server JEI transfer dropped player={} targetSession={} reason=carried carried={}", player.getName().getString(), payload.targetSessionId(), player.inventoryMenu.getCarried());
            syncCarried(player, sessions);
            return;
        }

        JeiTransferTarget target = resolveJeiTransferTarget(player, sessions, payload.targetSessionId());
        if (target == null) {
            return;
        }

        MinecraftServer server = player.getServer();
        Recipe<?> recipe = server == null ? null : server.getRecipeManager().byKey(payload.recipeId()).orElse(null);
        if (recipe == null) {
            DesktopDebug.trace("server JEI transfer dropped player={} targetSession={} recipe={} reason=missing-recipe", player.getName().getString(), payload.targetSessionId(), payload.recipeId());
            return;
        }

        ValidatedJeiTransfer validated = validateJeiTransfer(player, sessions, target, recipe, payload.maxTransfer());
        if (validated == null) {
            DesktopDebug.trace("server JEI transfer dropped player={} targetSession={} recipe={} reason=unsupported-or-denied", player.getName().getString(), payload.targetSessionId(), payload.recipeId());
            return;
        }

        List<Slot> recipeSlots = validated.recipeSlots();
        List<JeiTransferRequirement> requirements = validated.requirements();
        Set<JeiTransferSourceKey> recipeSlotKeys = new HashSet<>();
        for (Slot recipeSlot : recipeSlots) {
            recipeSlotKeys.add(new JeiTransferSourceKey(recipeSlot.container, recipeSlot.getContainerSlot()));
        }
        List<Slot> sourceSlots = jeiTransferSourceSlots(player, sessions, recipeSlotKeys);
        JeiTransferSimulation simulation = simulateJeiTransfer(
            player,
            recipeSlots,
            requirements,
            sourceSlots,
            payload.maxTransfer(),
            validated.maximumCrafts()
        );
        if (simulation == null) {
            DesktopDebug.trace("server JEI transfer dropped player={} targetSession={} recipe={} reason=simulation-failed", player.getName().getString(), payload.targetSessionId(), payload.recipeId());
            return;
        }

        applyJeiTransferSimulation(simulation);
        sessions.broadcastAll(player);
        DesktopDebug.trace(
            "server JEI transfer player={} targetSession={} recipe={} recipeSlots={} requirements={} sources={} max={}",
            player.getName().getString(),
            payload.targetSessionId(),
            payload.recipeId(),
            recipeSlots.size(),
            requirements.size(),
            sourceSlots.size(),
            payload.maxTransfer()
        );
    }

    private static @Nullable JeiTransferTarget resolveJeiTransferTarget(ServerPlayer player, PlayerSessions sessions, int targetSessionId) {
        if (targetSessionId == DesktopPackets.PLAYER_MENU_SESSION) {
            return new JeiTransferTarget(targetSessionId, player.inventoryMenu, null);
        }

        Session session = sessions.sessions.get(targetSessionId);
        if (session == null) {
            DesktopDebug.trace("server JEI transfer dropped player={} targetSession={} reason=missing-session", player.getName().getString(), targetSessionId);
            return null;
        }
        if (!session.visibleToClient) {
            DesktopDebug.trace("server JEI transfer dropped player={} targetSession={} reason=hidden", player.getName().getString(), targetSessionId);
            return null;
        }
        if (!session.menu.stillValid(player)) {
            DesktopDebug.log("server JEI transfer invalid player={} session={} title={}", player.getName().getString(), session.sessionId, session.title.getString());
            sessions.close(player, session.sessionId, true);
            return null;
        }
        return new JeiTransferTarget(targetSessionId, session.menu, session);
    }

    private static @Nullable ValidatedJeiTransfer validateJeiTransfer(
        ServerPlayer player,
        PlayerSessions sessions,
        JeiTransferTarget target,
        Recipe<?> recipe,
        boolean maxTransfer
    ) {
        if (isVanillaCraftingTransferMenu(target.menu()) && recipe instanceof CraftingRecipe craftingRecipe) {
            return validateVanillaCraftingTransfer(target.menu(), craftingRecipe);
        }

        Session session = target.session();
        if (session == null || session.serverHandler == null || !DesktopTransferValidators.supports(session.serverHandler)) {
            return null;
        }

        DesktopTransferDecision decision;
        try {
            decision = sessions.invokeServerHandlerCallback(
                player,
                () -> DesktopTransferValidators.validate(
                    session.serverHandler,
                    new DesktopTransferRequest<>(new ServerSessionContext(player, sessions, session), recipe, maxTransfer)
                )
            );
        } catch (RuntimeException exception) {
            session.quarantineServerHandler(player, "transfer-validation", exception);
            return null;
        }
        if (!decision.allowed()) {
            DesktopDebug.trace(
                "server transfer validator rejected player={} session={} recipe={} status={} reason={}",
                player.getName().getString(),
                session.sessionId,
                recipe.getId(),
                decision.status(),
                decision.reason()
            );
            return null;
        }

        List<Slot> destinationSlots = resolveJeiTransferRecipeSlots(target.menu(), decision.destinationSlots());
        if (destinationSlots == null || destinationSlots.isEmpty()) {
            return null;
        }
        Map<Integer, Slot> destinationsById = new HashMap<>();
        for (int destinationId : decision.destinationSlots()) {
            destinationsById.put(destinationId, target.menu().slots.get(destinationId));
        }
        List<JeiTransferRequirement> requirements = new ArrayList<>(decision.requirements().size());
        int inputIndex = 0;
        for (DesktopTransferRequirement requirement : decision.requirements()) {
            Slot targetSlot = destinationsById.get(requirement.targetSlotId());
            if (targetSlot == null) {
                return null;
            }
            List<ItemStack> alternatives = normalizeTransferAlternatives(
                targetSlot,
                requirement.alternatives(),
                requirement.count()
            );
            if (alternatives == null) {
                return null;
            }
            requirements.add(new JeiTransferRequirement(inputIndex++, requirement.targetSlotId(), targetSlot, requirement.count(), alternatives));
        }
        sortTransferRequirements(requirements);
        return new ValidatedJeiTransfer(destinationSlots, requirements, decision.maximumCrafts());
    }

    private static boolean isVanillaCraftingTransferMenu(AbstractContainerMenu menu) {
        return menu instanceof InventoryMenu || menu instanceof CraftingMenu;
    }

    private static @Nullable ValidatedJeiTransfer validateVanillaCraftingTransfer(
        AbstractContainerMenu menu,
        CraftingRecipe recipe
    ) {
        if (!(menu instanceof RecipeBookMenu<?> recipeMenu)) {
            return null;
        }
        int gridWidth = recipeMenu.getGridWidth();
        int gridHeight = recipeMenu.getGridHeight();
        if (gridWidth <= 0 || gridHeight <= 0 || !recipe.canCraftInDimensions(gridWidth, gridHeight)) {
            return null;
        }
        int gridSize;
        try {
            gridSize = Math.multiplyExact(gridWidth, gridHeight);
        } catch (ArithmeticException exception) {
            return null;
        }
        int firstGridSlot = recipeMenu.getResultSlotIndex() + 1;
        if (firstGridSlot < 0 || firstGridSlot + gridSize > menu.slots.size()) {
            return null;
        }
        List<Slot> gridSlots = new ArrayList<>(gridSize);
        for (int index = 0; index < gridSize; index++) {
            Slot slot = menu.slots.get(firstGridSlot + index);
            if (!slot.isActive()) {
                return null;
            }
            gridSlots.add(slot);
        }

        List<Ingredient> ingredients = recipe.getIngredients();
        List<JeiTransferRequirement> requirements = new ArrayList<>();
        if (recipe instanceof ShapedRecipe shapedRecipe) {
            int recipeWidth = shapedRecipe.getWidth();
            int recipeHeight = shapedRecipe.getHeight();
            if (recipeWidth <= 0 || recipeHeight <= 0 || recipeWidth > gridWidth || recipeHeight > gridHeight
                || ingredients.size() < recipeWidth * recipeHeight) {
                return null;
            }
            int xOffset = recipeWidth < gridWidth / 2.0F ? (gridWidth - recipeWidth) / 2 : 0;
            int yOffset = recipeHeight < gridHeight / 2.0F ? (gridHeight - recipeHeight) / 2 : 0;
            for (int y = 0; y < recipeHeight; y++) {
                for (int x = 0; x < recipeWidth; x++) {
                    int ingredientIndex = y * recipeWidth + x;
                    Ingredient ingredient = ingredients.get(ingredientIndex);
                    if (ingredient.isEmpty()) {
                        continue;
                    }
                    Slot targetSlot = gridSlots.get((y + yOffset) * gridWidth + x + xOffset);
                    List<ItemStack> alternatives = normalizeTransferAlternatives(targetSlot, List.of(ingredient.getItems()), 1);
                    if (alternatives == null) {
                        return null;
                    }
                    int targetMenuSlotId = firstGridSlot + (y + yOffset) * gridWidth + x + xOffset;
                    requirements.add(new JeiTransferRequirement(ingredientIndex, targetMenuSlotId, targetSlot, 1, alternatives));
                }
            }
        } else {
            int gridIndex = 0;
            for (int ingredientIndex = 0; ingredientIndex < ingredients.size(); ingredientIndex++) {
                Ingredient ingredient = ingredients.get(ingredientIndex);
                if (ingredient.isEmpty()) {
                    continue;
                }
                if (gridIndex >= gridSlots.size()) {
                    return null;
                }
                Slot targetSlot = gridSlots.get(gridIndex++);
                List<ItemStack> alternatives = normalizeTransferAlternatives(targetSlot, List.of(ingredient.getItems()), 1);
                if (alternatives == null) {
                    return null;
                }
                int targetMenuSlotId = firstGridSlot + gridIndex - 1;
                requirements.add(new JeiTransferRequirement(ingredientIndex, targetMenuSlotId, targetSlot, 1, alternatives));
            }
        }
        if (requirements.isEmpty() || requirements.size() > DesktopProtocol.MAX_TRANSFER_REQUIREMENTS) {
            return null;
        }
        int totalAlternatives = requirements.stream().mapToInt(value -> value.alternatives().size()).sum();
        if (totalAlternatives > DesktopProtocol.MAX_TRANSFER_ALTERNATIVES) {
            return null;
        }
        sortTransferRequirements(requirements);
        return new ValidatedJeiTransfer(gridSlots, requirements, JEI_TRANSFER_MAX_CRAFTS);
    }

    private static @Nullable List<ItemStack> normalizeTransferAlternatives(Slot targetSlot, List<ItemStack> rawAlternatives, int count) {
        if (count <= 0 || rawAlternatives.isEmpty() || rawAlternatives.size() > DesktopProtocol.MAX_ALTERNATIVES_PER_REQUIREMENT) {
            return null;
        }
        List<ItemStack> alternatives = new ArrayList<>();
        for (ItemStack raw : rawAlternatives) {
            if (raw == null || raw.isEmpty()) {
                continue;
            }
            ItemStack alternative = raw.copyWithCount(count);
            int limit = Math.min(alternative.getMaxStackSize(), targetSlot.getMaxStackSize(alternative));
            if (!targetSlot.mayPlace(alternative) || count > limit) {
                continue;
            }
            boolean duplicate = alternatives.stream().anyMatch(existing -> ItemStack.isSameItemSameTags(existing, alternative));
            if (!duplicate) {
                alternatives.add(alternative);
            }
        }
        return alternatives.isEmpty() || alternatives.size() > DesktopProtocol.MAX_ALTERNATIVES_PER_REQUIREMENT
            ? null
            : List.copyOf(alternatives);
    }

    private static void sortTransferRequirements(List<JeiTransferRequirement> requirements) {
        requirements.sort(Comparator
            .comparingInt((JeiTransferRequirement requirement) -> requirement.alternatives().size())
            .thenComparingInt(JeiTransferRequirement::inputIndex)
            .thenComparingInt(JeiTransferRequirement::targetMenuSlotId));
    }

    private static @Nullable List<Slot> resolveJeiTransferRecipeSlots(AbstractContainerMenu menu, List<Integer> slotIds) {
        if (slotIds.isEmpty()) {
            return null;
        }
        List<Slot> slots = new ArrayList<>(slotIds.size());
        Set<Integer> seen = new HashSet<>();
        for (int slotId : slotIds) {
            if (!seen.add(slotId) || slotId < 0 || slotId >= menu.slots.size()) {
                return null;
            }
            Slot slot = menu.slots.get(slotId);
            if (!slot.isActive()) {
                return null;
            }
            slots.add(slot);
        }
        return slots;
    }

    private static List<Slot> jeiTransferSourceSlots(
        ServerPlayer player,
        PlayerSessions sessions,
        Set<JeiTransferSourceKey> targetRecipeSlots
    ) {
        Map<JeiTransferSourceKey, Slot> slots = new LinkedHashMap<>();
        for (Slot slot : player.inventoryMenu.slots) {
            addJeiTransferSourceSlot(player, slots, slot, targetRecipeSlots);
        }
        for (Session session : sessions.sessions.values()) {
            if (!session.visibleToClient || !session.menu.stillValid(player)) {
                continue;
            }
            for (Slot slot : session.menu.slots) {
                addJeiTransferSourceSlot(player, slots, slot, targetRecipeSlots);
            }
        }
        return new ArrayList<>(slots.values());
    }

    private static void addJeiTransferSourceSlot(
        ServerPlayer player,
        Map<JeiTransferSourceKey, Slot> slots,
        Slot slot,
        Set<JeiTransferSourceKey> targetRecipeSlots
    ) {
        JeiTransferSourceKey sourceKey = new JeiTransferSourceKey(slot.container, slot.getContainerSlot());
        if (targetRecipeSlots.contains(sourceKey)) {
            return;
        }
        if (!isJeiTransferSourceSlot(player, slot)) {
            return;
        }
        slots.putIfAbsent(sourceKey, slot);
    }

    private static boolean isJeiTransferSourceSlot(ServerPlayer player, Slot slot) {
        if (!slot.isActive() || !slot.hasItem() || !slot.mayPickup(player)) {
            return false;
        }
        if (slot.container == player.getInventory()) {
            int containerSlot = slot.getContainerSlot();
            return containerSlot >= 0 && containerSlot < net.minecraft.world.entity.player.Inventory.INVENTORY_SIZE;
        }
        return slot.mayPlace(slot.getItem());
    }

    private static @Nullable JeiTransferSimulation simulateJeiTransfer(
        ServerPlayer player,
        List<Slot> recipeSlots,
        List<JeiTransferRequirement> requirements,
        List<Slot> sourceSlots,
        boolean maxTransfer,
        int maximumCrafts
    ) {
        if (maximumCrafts < 1 || maximumCrafts > JEI_TRANSFER_MAX_CRAFTS
            || requirements.isEmpty() || requirements.size() > DesktopProtocol.MAX_TRANSFER_REQUIREMENTS) {
            return null;
        }
        Map<Slot, ItemStack> sourceStacks = new LinkedHashMap<>();
        for (Slot sourceSlot : sourceSlots) {
            sourceStacks.put(sourceSlot, sourceSlot.getItem().copy());
        }

        Map<Slot, ItemStack> targetStacks = compatibleJeiTransferTargetStacks(player, recipeSlots, requirements);
        if (targetStacks == null) {
            for (Slot recipeSlot : recipeSlots) {
                ItemStack stack = recipeSlot.getItem();
                if (stack.isEmpty()) {
                    continue;
                }
                if (!recipeSlot.mayPickup(player)) {
                    return null;
                }
                ItemStack moving = stack.copy();
                if (!insertJeiTransferStack(sourceSlots, sourceStacks, moving)) {
                    return null;
                }
            }

            targetStacks = new LinkedHashMap<>();
            for (Slot recipeSlot : recipeSlots) {
                targetStacks.put(recipeSlot, ItemStack.EMPTY);
            }
        }

        Map<TransferStackKey, Integer> supply = new LinkedHashMap<>();
        Map<TransferStackKey, ItemStack> prototypes = new LinkedHashMap<>();
        for (ItemStack stack : sourceStacks.values()) {
            if (stack.isEmpty()) {
                continue;
            }
            TransferStackKey key = TransferStackKey.of(stack);
            try {
                supply.merge(key, stack.getCount(), Math::addExact);
            } catch (ArithmeticException exception) {
                return null;
            }
            prototypes.putIfAbsent(key, stack.copyWithCount(1));
        }

        List<BoundedTransferPlanner.Requirement<TransferStackKey>> plannerRequirements = new ArrayList<>(requirements.size());
        int capacityCrafts = maximumCrafts;
        int totalAlternatives = 0;
        for (JeiTransferRequirement requirement : requirements) {
            List<TransferStackKey> alternatives = new ArrayList<>();
            Map<TransferStackKey, Integer> maximumUnitsByAlternative = new LinkedHashMap<>();
            int largestLimit = 0;
            for (ItemStack alternative : requirement.alternatives()) {
                TransferStackKey key = TransferStackKey.of(alternative);
                if (!alternatives.contains(key)) {
                    alternatives.add(key);
                    prototypes.putIfAbsent(key, alternative.copyWithCount(1));
                }
                int limit = Math.min(alternative.getMaxStackSize(), requirement.targetSlot().getMaxStackSize(alternative));
                maximumUnitsByAlternative.merge(key, limit, Math::max);
                largestLimit = Math.max(largestLimit, limit);
            }
            totalAlternatives += alternatives.size();
            if (alternatives.isEmpty() || alternatives.size() > DesktopProtocol.MAX_ALTERNATIVES_PER_REQUIREMENT
                || totalAlternatives > DesktopProtocol.MAX_TRANSFER_ALTERNATIVES) {
                return null;
            }
            ItemStack existing = targetStacks.getOrDefault(requirement.targetSlot(), ItemStack.EMPTY);
            if (!existing.isEmpty()) {
                TransferStackKey existingKey = TransferStackKey.of(existing);
                alternatives.removeIf(key -> !key.equals(existingKey));
                if (alternatives.isEmpty()) {
                    return null;
                }
                largestLimit = Math.min(existing.getMaxStackSize(), requirement.targetSlot().getMaxStackSize(existing));
                maximumUnitsByAlternative.keySet().removeIf(key -> !key.equals(existingKey));
            }
            capacityCrafts = Math.min(capacityCrafts, largestLimit / requirement.unitsPerCraft());
            plannerRequirements.add(new BoundedTransferPlanner.Requirement<>(
                requirement.targetMenuSlotId(),
                requirement.unitsPerCraft(),
                existing.getCount(),
                alternatives,
                maximumUnitsByAlternative
            ));
        }
        if (capacityCrafts < 1) {
            return null;
        }

        BoundedTransferPlanner<TransferStackKey> planner = new BoundedTransferPlanner<>(TransferStackKey.COMPARATOR);
        Optional<BoundedTransferPlanner.Plan<TransferStackKey>> planned = maxTransfer
            ? planner.planMaximum(supply, plannerRequirements, capacityCrafts)
            : planner.planExact(supply, plannerRequirements, 1);
        if (planned.isEmpty() || planned.get().attempts() > JEI_TRANSFER_SEARCH_BUDGET) {
            return null;
        }
        BoundedTransferPlanner.Plan<TransferStackKey> plan = planned.get();
        int crafts = plan.crafts();

        Map<Integer, JeiTransferRequirement> requirementsByTarget = new HashMap<>();
        for (JeiTransferRequirement requirement : requirements) {
            requirementsByTarget.put(requirement.targetMenuSlotId(), requirement);
        }
        for (BoundedTransferPlanner.Allocation<TransferStackKey> allocation : plan.allocations()) {
            JeiTransferRequirement requirement = requirementsByTarget.get(allocation.targetId());
            if (requirement == null || allocation.units().size() != 1) {
                // A single menu slot cannot represent mixed ingredient variants.
                return null;
            }
            Map.Entry<TransferStackKey, Integer> selected = allocation.units().entrySet().iterator().next();
            int totalUnits;
            try {
                totalUnits = Math.multiplyExact(requirement.unitsPerCraft(), crafts);
            } catch (ArithmeticException exception) {
                return null;
            }
            ItemStack existing = targetStacks.getOrDefault(requirement.targetSlot(), ItemStack.EMPTY);
            int deficit = Math.max(0, totalUnits - existing.getCount());
            if (selected.getValue() < deficit || !consumeTransferUnits(sourceStacks, selected.getKey(), deficit)) {
                return null;
            }
            ItemStack prototype = prototypes.get(selected.getKey());
            if (prototype == null) {
                return null;
            }
            int newCount = existing.getCount() + deficit;
            ItemStack result = existing.isEmpty() ? prototype.copyWithCount(newCount) : existing.copyWithCount(newCount);
            int limit = Math.min(result.getMaxStackSize(), requirement.targetSlot().getMaxStackSize(result));
            if (!requirement.targetSlot().mayPlace(result) || newCount > limit) {
                return null;
            }
            targetStacks.put(requirement.targetSlot(), result);
        }

        return new JeiTransferSimulation(sourceStacks, targetStacks);
    }

    private static boolean consumeTransferUnits(Map<Slot, ItemStack> sourceStacks, TransferStackKey key, int count) {
        if (count == 0) {
            return true;
        }
        int remaining = count;
        for (ItemStack stack : sourceStacks.values()) {
            if (stack.isEmpty() || !key.equals(TransferStackKey.of(stack))) {
                continue;
            }
            int consumed = Math.min(stack.getCount(), remaining);
            stack.shrink(consumed);
            remaining -= consumed;
            if (remaining == 0) {
                return true;
            }
        }
        return false;
    }

    private static @Nullable Map<Slot, ItemStack> compatibleJeiTransferTargetStacks(ServerPlayer player, List<Slot> recipeSlots, List<JeiTransferRequirement> requirements) {
        Map<Slot, JeiTransferRequirement> requirementsBySlot = new HashMap<>();
        for (JeiTransferRequirement requirement : requirements) {
            requirementsBySlot.put(requirement.targetSlot(), requirement);
        }

        Map<Slot, ItemStack> targetStacks = new LinkedHashMap<>();
        for (Slot recipeSlot : recipeSlots) {
            ItemStack stack = recipeSlot.getItem();
            JeiTransferRequirement requirement = requirementsBySlot.get(recipeSlot);
            if (requirement == null) {
                if (!stack.isEmpty()) {
                    return null;
                }
                targetStacks.put(recipeSlot, ItemStack.EMPTY);
                continue;
            }

            if (stack.isEmpty()) {
                targetStacks.put(recipeSlot, ItemStack.EMPTY);
                continue;
            }
            if (!recipeSlot.mayPickup(player) || !recipeSlot.mayPlace(stack) || !matchesJeiTransferAlternative(stack, requirement.alternatives())) {
                return null;
            }

            int limit = Math.min(stack.getMaxStackSize(), recipeSlot.getMaxStackSize(stack));
            if (stack.getCount() > limit) {
                return null;
            }
            targetStacks.put(recipeSlot, stack.copy());
        }
        return targetStacks;
    }

    private static boolean matchesJeiTransferAlternative(ItemStack stack, List<ItemStack> alternatives) {
        for (ItemStack alternative : alternatives) {
            if (ItemStack.isSameItemSameTags(stack, alternative)) {
                return true;
            }
        }
        return false;
    }

    private static boolean insertJeiTransferStack(List<Slot> sourceSlots, Map<Slot, ItemStack> sourceStacks, ItemStack moving) {
        if (moving.isEmpty()) {
            return true;
        }

        if (moving.isStackable()) {
            for (Slot slot : sourceSlots) {
                if (moving.isEmpty()) {
                    return true;
                }
                ItemStack existing = sourceStacks.getOrDefault(slot, ItemStack.EMPTY);
                if (existing.isEmpty() || !ItemStack.isSameItemSameTags(existing, moving) || !slot.mayPlace(moving)) {
                    continue;
                }
                int limit = Math.min(existing.getMaxStackSize(), slot.getMaxStackSize(existing));
                int moved = Math.min(moving.getCount(), Math.max(0, limit - existing.getCount()));
                if (moved <= 0) {
                    continue;
                }
                existing.grow(moved);
                moving.shrink(moved);
            }
        }

        for (Slot slot : sourceSlots) {
            if (moving.isEmpty()) {
                return true;
            }
            ItemStack existing = sourceStacks.getOrDefault(slot, ItemStack.EMPTY);
            if (!existing.isEmpty() || !slot.mayPlace(moving)) {
                continue;
            }
            int moved = Math.min(moving.getCount(), Math.min(moving.getMaxStackSize(), slot.getMaxStackSize(moving)));
            if (moved <= 0) {
                continue;
            }
            sourceStacks.put(slot, moving.copyWithCount(moved));
            moving.shrink(moved);
        }
        return moving.isEmpty();
    }

    private static void applyJeiTransferSimulation(JeiTransferSimulation simulation) {
        for (Map.Entry<Slot, ItemStack> entry : simulation.sourceStacks().entrySet()) {
            setJeiTransferSlotStack(entry.getKey(), entry.getValue());
        }
        for (Map.Entry<Slot, ItemStack> entry : simulation.targetStacks().entrySet()) {
            setJeiTransferSlotStack(entry.getKey(), entry.getValue());
        }
    }

    private static void setJeiTransferSlotStack(Slot slot, ItemStack stack) {
        ItemStack current = slot.getItem();
        if (ItemStack.matches(current, stack)) {
            return;
        }
        slot.setByPlayer(stack.copy());
        slot.setChanged();
    }

    private static boolean canCraftRecipe(RecipeBookMenu recipeBookMenu, ServerPlayer player, Recipe<?> recipe) {
        StackedContents contents = new StackedContents();
        player.getInventory().fillStackedContents(contents);
        recipeBookMenu.fillCraftSlotsStackedContents(contents);
        return contents.canCraft(recipe, null);
    }

    private static void rename(ServerPlayer player, DesktopRenamePayload payload) {
        PlayerSessions sessions = existingSessions(player);
        if (sessions == null || !sessions.isReady()) {
            DesktopDebug.trace("server rename dropped player={} session={} reason=not-ready", player.getName().getString(), payload.sessionId());
            return;
        }

        Session session = sessions.sessions.get(payload.sessionId());
        if (session == null) {
            DesktopDebug.trace("server rename dropped player={} session={} reason=missing-session", player.getName().getString(), payload.sessionId());
            return;
        }
        if (!session.visibleToClient) {
            DesktopDebug.trace("server rename dropped player={} session={} reason=hidden", player.getName().getString(), payload.sessionId());
            return;
        }

        if (!session.menu.stillValid(player)) {
            DesktopDebug.log("server rename invalid player={} session={} title={}", player.getName().getString(), session.sessionId, session.title.getString());
            sessions.close(player, session.sessionId, true);
            return;
        }

        if (!(session.menu instanceof AnvilMenu anvilMenu)) {
            DesktopDebug.trace("server rename dropped player={} session={} reason=not-anvil", player.getName().getString(), payload.sessionId());
            return;
        }

        boolean changed = anvilMenu.setItemName(payload.name());
        DesktopDebug.trace("server rename player={} session={} changed={} name={}", player.getName().getString(), payload.sessionId(), changed, payload.name());
        if (changed) {
            anvilMenu.broadcastChanges();
            player.inventoryMenu.broadcastChanges();
            sessions.broadcastAll(player);
        }
    }

    private static void customPayload(ServerPlayer player, DesktopCustomPayload payload) {
        PlayerSessions sessions = existingSessions(player);
        if (sessions == null || !sessions.isReady()) {
            DesktopDebug.trace("server custom dropped player={} session={} channel={} reason=not-ready", player.getName().getString(), payload.sessionId(), payload.channel());
            return;
        }

        Session session = sessions.sessions.get(payload.sessionId());
        if (session == null) {
            DesktopDebug.trace("server custom dropped player={} session={} channel={} reason=missing-session", player.getName().getString(), payload.sessionId(), payload.channel());
            return;
        }
        if (!session.visibleToClient) {
            DesktopDebug.trace("server custom dropped player={} session={} channel={} reason=hidden", player.getName().getString(), payload.sessionId(), payload.channel());
            return;
        }

        if (!session.menu.stillValid(player)) {
            DesktopDebug.log("server custom invalid player={} session={} title={}", player.getName().getString(), session.sessionId, session.title.getString());
            sessions.close(player, session.sessionId, true);
            return;
        }

        @Nullable MenuType<?> menuType = session.menuType();
        if (menuType == null) {
            DesktopDebug.trace("server custom dropped player={} session={} channel={} reason=no-menu-type", player.getName().getString(), payload.sessionId(), payload.channel());
            return;
        }

        DesktopServerPayloadHandler<AbstractContainerMenu> handler = DesktopServerApi.findPayloadHandler(menuType, payload.channel());
        if (handler == null) {
            DesktopDebug.warn(
                "server custom dropped player={} session={} channel={} menu={} reason=no-handler",
                player.getName().getString(),
                payload.sessionId(),
                payload.channel(),
                menuType
            );
            return;
        }

        DesktopDebug.trace(
            "server custom player={} session={} channel={} bytes={}",
            player.getName().getString(),
            payload.sessionId(),
            payload.channel(),
            payload.dataLength()
        );
        try {
            sessions.invokeServerHandlerCallback(
                player,
                () -> withCanonicalCarried(player, sessions, session.menu, () -> {
                    handler.handle(new ServerPayloadContext(player, sessions, session, payload));
                    return null;
                })
            );
        } catch (RuntimeException exception) {
            DesktopDebug.warn(
                "server custom handler failed player={} session={} channel={} reason={}",
                player.getName().getString(),
                payload.sessionId(),
                payload.channel(),
                exception.toString()
            );
        }
    }

    private static boolean applyBeaconButton(BeaconMenu menu, int buttonId) {
        int primaryId = buttonId & BEACON_EFFECT_ID_MASK;
        int secondaryId = buttonId >>> BEACON_SECONDARY_EFFECT_SHIFT & BEACON_EFFECT_ID_MASK;
        MobEffect primary = MobEffect.byId(primaryId);
        MobEffect secondary = MobEffect.byId(secondaryId);
        if (!menu.hasPayment() || !canSelectBeaconPrimary(menu, primary) || !canSelectBeaconSecondary(menu, primary, secondary)) {
            return false;
        }

        menu.updateEffects(Optional.of(primary), Optional.ofNullable(secondary));
        return true;
    }

    private static boolean canSelectBeaconPrimary(BeaconMenu menu, @Nullable MobEffect effect) {
        if (effect == null) {
            return false;
        }

        int unlockedTiers = Math.min(menu.getLevels(), Math.min(3, BeaconBlockEntity.BEACON_EFFECTS.length));
        for (int tier = 0; tier < unlockedTiers; tier++) {
            if (beaconTierContains(tier, effect)) {
                return true;
            }
        }
        return false;
    }

    private static boolean canSelectBeaconSecondary(BeaconMenu menu, MobEffect primary, @Nullable MobEffect secondary) {
        if (secondary == null) {
            return true;
        }
        if (menu.getLevels() < 4) {
            return false;
        }
        if (primary.equals(secondary)) {
            return true;
        }
        return BeaconBlockEntity.BEACON_EFFECTS.length > 3 && beaconTierContains(3, secondary);
    }

    private static boolean beaconTierContains(int tier, MobEffect effect) {
        if (tier < 0 || tier >= BeaconBlockEntity.BEACON_EFFECTS.length) {
            return false;
        }
        for (MobEffect candidate : BeaconBlockEntity.BEACON_EFFECTS[tier]) {
            if (candidate == effect) {
                return true;
            }
        }
        return false;
    }

    private static @Nullable SlotSource resolveSlot(ServerPlayer player, PlayerSessions sessions, int sessionId, int slotIndex) {
        AbstractContainerMenu menu;
        Session session = null;
        if (sessionId == DesktopPackets.PLAYER_MENU_SESSION) {
            menu = player.inventoryMenu;
        } else {
            session = sessions.sessions.get(sessionId);
            if (session == null) {
                DesktopDebug.trace("server quick move dropped player={} session={} reason=missing-session", player.getName().getString(), sessionId);
                return null;
            }
            if (!session.visibleToClient) {
                DesktopDebug.trace("server quick move dropped player={} session={} reason=hidden", player.getName().getString(), sessionId);
                return null;
            }
            if (!session.menu.stillValid(player)) {
                DesktopDebug.log("server quick move invalid player={} session={} title={}", player.getName().getString(), session.sessionId, session.title.getString());
                sessions.close(player, session.sessionId, true);
                return null;
            }
            menu = session.menu;
        }

        Slot slot = DesktopMenuSlots.slot(menu, slotIndex);
        if (slot == null) {
            DesktopDebug.trace("server quick move dropped player={} session={} slot={} reason=out-of-range", player.getName().getString(), sessionId, slotIndex);
            return null;
        }

        return new SlotSource(sessionId, menu, slot, session);
    }

    private static List<net.minecraft.world.inventory.Slot> quickMoveTargets(ServerPlayer player, PlayerSessions sessions, SlotSource source, DesktopQuickMovePayload payload) {
        if (payload.targetKind() == DesktopPackets.QUICK_TARGET_SESSION) {
            if (payload.targetSessionId() == source.sessionId) {
                return List.of();
            }
            Session targetSession = sessions.sessions.get(payload.targetSessionId());
            if (targetSession != null && targetSession.visibleToClient && targetSession.menu.stillValid(player)) {
                return containerSlots(targetSession.menu, player);
            }
            return List.of();
        }

        if (payload.targetKind() == DesktopPackets.QUICK_TARGET_HOTBAR && !isPlayerInventorySlot(player, source.slot)) {
            return hotbarSlots(player);
        }

        return defaultPlayerTargets(player, source.slot);
    }

    private static List<net.minecraft.world.inventory.Slot> defaultPlayerTargets(ServerPlayer player, net.minecraft.world.inventory.Slot sourceSlot) {
        boolean sourceIsPlayerInventory = isPlayerInventorySlot(player, sourceSlot);
        boolean sourceIsMainInventory = InventoryExpansion.isMainInventorySlot(player, sourceSlot);
        int sourceContainerSlot = sourceSlot.getContainerSlot();
        if (sourceIsPlayerInventory && sourceContainerSlot >= 0 && sourceContainerSlot < 9) {
            return mainInventorySlots(player);
        }
        if (sourceIsMainInventory) {
            return hotbarSlots(player);
        }

        List<net.minecraft.world.inventory.Slot> slots = new ArrayList<>();
        slots.addAll(mainInventorySlots(player));
        slots.addAll(hotbarSlots(player));
        return slots;
    }

    private static boolean moveSlotStack(
        ServerPlayer player, PlayerSessions sessions, SlotSource source, AbstractContainerMenu targetMenu,
        List<Slot> targets, QuickMoveWorkBudget workBudget, boolean sortExtraction
    ) {
        Slot sourceSlot = source.slot;
        if (!sourceSlot.isActive() || !sourceSlot.hasItem() || !sourceSlot.mayPickup(player)) {
            return false;
        }

        ItemStack sourceBefore = sourceSlot.getItem().copy();
        ItemStack moving = sourceBefore.copy();
        ItemStack carriedBefore = player.inventoryMenu.getCarried().copy();
        List<Slot> uniqueTargets = uniqueQuickMoveTargets(source, targetMenu, targets);
        // Reserve the complete worst-case validation cost before the first slot callback. Budget
        // exhaustion must never happen after safeInsert/setByPlayer/onTake side effects that a
        // rollback cannot necessarily reverse (stats, upgrade work, external handlers, etc.).
        int targetCount = uniqueTargets.size();
        long validationPasses = 3L * targetCount + 2L;
        workBudget.consume(Math.max(1, targetCount) + validationPasses * (targetCount + 1L));
        List<QuickMoveTargetPlan> plans = new ArrayList<>(uniqueTargets.size());
        for (Slot target : uniqueTargets) {
            plans.add(new QuickMoveTargetPlan(targetMenu, target, target.getItem()));
        }

        try {
            int moved = insertIntoMatchingSlots(
                player, sessions, source, targetMenu, plans, moving, carriedBefore
            );
            moved += insertIntoEmptySlots(
                player, sessions, source, targetMenu, plans, moving, carriedBefore
            );
            // Even a target that reports a zero insertion may have run arbitrary container
            // callbacks. Validate the complete source/target set before treating this as a no-op.
            verifyQuickMoveTransaction(player, carriedBefore, source, moving, plans);
            if (moved <= 0) {
                return false;
            }

            ItemStack remainder = moving.copy();
            final int extractedCount = moved;
            withMenuNetworkSession(player, sessions, source.menu, () -> {
                if (sortExtraction) {
                    sourceSlot.onQuickCraft(remainder, sourceBefore);
                    sourceSlot.onTake(player, sourceBefore.copyWithCount(extractedCount));
                } else {
                    sourceSlot.onTake(player, remainder);
                }
            });
            verifyQuickMoveTransaction(player, carriedBefore, source, moving, plans);
            return true;
        } catch (RuntimeException exception) {
            try {
                rollbackQuickMoveTransaction(
                    player, sessions, source, sourceBefore, carriedBefore, targetMenu, plans
                );
            } catch (RuntimeException rollbackException) {
                exception.addSuppressed(rollbackException);
            }
            throw exception;
        }
    }

    private static void verifyQuickMoveTransaction(
        ServerPlayer player,
        ItemStack expectedCarried,
        SlotSource source,
        ItemStack expectedSource,
        List<QuickMoveTargetPlan> plans
    ) {
        IdentityHashMap<Object, Set<Integer>> physicalSlots = new IdentityHashMap<>();
        if (!matchesPhysicalSlot(
                source.menu, source.slot, source.physicalOwner, source.physicalIndex
            )
            || !rememberPhysicalSlot(physicalSlots, source.physicalOwner, source.physicalIndex)) {
            throw new IllegalStateException("Quick-move source physical identity changed");
        }
        if (!ItemStack.matches(expectedCarried, player.inventoryMenu.getCarried())) {
            throw new IllegalStateException("Quick-move callback changed the canonical cursor");
        }
        if (!ItemStack.matches(expectedSource, source.slot.getItem())) {
            throw new IllegalStateException("Quick-move source changed after its verified commit");
        }
        for (QuickMoveTargetPlan plan : plans) {
            if (!matchesPhysicalSlot(
                    plan.menu(), plan.slot(), plan.physicalOwner(), plan.physicalIndex()
                )
                || !rememberPhysicalSlot(
                    physicalSlots, plan.physicalOwner(), plan.physicalIndex()
                )
                || !ItemStack.matches(plan.expected(), plan.slot().getItem())) {
                throw new IllegalStateException("Quick-move target changed after its verified commit");
            }
        }
    }

    private static List<Slot> uniqueQuickMoveTargets(
        SlotSource source,
        AbstractContainerMenu targetMenu,
        List<Slot> targets
    ) {
        IdentityHashMap<Object, Set<Integer>> physicalSlots = new IdentityHashMap<>();
        if (!matchesPhysicalSlot(source.menu, source.slot, source.physicalOwner, source.physicalIndex)
            || !rememberPhysicalSlot(physicalSlots, source.physicalOwner, source.physicalIndex)) {
            return List.of();
        }
        List<Slot> unique = new ArrayList<>(targets.size());
        for (Slot target : targets) {
            if (rememberPhysicalSlot(physicalSlots, targetMenu, target)) {
                unique.add(target);
            }
        }
        return unique;
    }

    private static int insertIntoMatchingSlots(
        ServerPlayer player,
        PlayerSessions sessions,
        SlotSource source,
        AbstractContainerMenu targetMenu,
        List<QuickMoveTargetPlan> targets,
        ItemStack moving,
        ItemStack carriedBefore
    ) {
        if (!moving.isStackable()) {
            return 0;
        }

        int moved = 0;
        for (QuickMoveTargetPlan plan : targets) {
            if (moving.isEmpty()) {
                break;
            }
            Slot target = plan.slot();
            if (!ItemStack.matches(plan.expected(), target.getItem())) {
                throw new IllegalStateException("Quick-move target changed before matching-slot insertion");
            }
            if (!target.isActive() || !target.hasItem()) {
                continue;
            }
            if (!ItemStack.isSameItemSameTags(moving, target.getItem())) {
                continue;
            }
            if (!target.mayPlace(moving)
                || target.getItem().getCount() >= Math.max(0, target.getMaxStackSize(moving))) {
                continue;
            }
            moved += insertQuickMoveTarget(
                player, sessions, source, targetMenu, plan, moving, carriedBefore, targets
            );
        }
        return moved;
    }

    private static int insertIntoEmptySlots(
        ServerPlayer player,
        PlayerSessions sessions,
        SlotSource source,
        AbstractContainerMenu targetMenu,
        List<QuickMoveTargetPlan> targets,
        ItemStack moving,
        ItemStack carriedBefore
    ) {
        int moved = 0;
        for (QuickMoveTargetPlan plan : targets) {
            if (moving.isEmpty()) {
                break;
            }
            Slot target = plan.slot();
            if (!ItemStack.matches(plan.expected(), target.getItem())) {
                throw new IllegalStateException("Quick-move target changed before empty-slot insertion");
            }
            if (!target.isActive() || target.hasItem()) {
                continue;
            }
            if (!target.mayPlace(moving) || target.getMaxStackSize(moving) <= 0) {
                continue;
            }
            moved += insertQuickMoveTarget(
                player, sessions, source, targetMenu, plan, moving, carriedBefore, targets
            );
        }
        return moved;
    }

    private static int insertQuickMoveTarget(
        ServerPlayer player,
        PlayerSessions sessions,
        SlotSource source,
        AbstractContainerMenu targetMenu,
        QuickMoveTargetPlan plan,
        ItemStack moving,
        ItemStack carriedBefore,
        List<QuickMoveTargetPlan> plans
    ) {
        verifyQuickMoveTransaction(player, carriedBefore, source, moving, plans);
        Slot target = plan.slot();
        ItemStack sourceBefore = source.slot.getItem().copy();
        ItemStack targetBefore = plan.expected();
        if (DesktopItemSourceLocks.isSlotLocked(player, source.slot)
            || !source.slot.isActive()
            || !source.slot.hasItem()
            || !source.slot.mayPickup(player)
            || !ItemStack.matches(sourceBefore, moving)) {
            throw new IllegalStateException("Quick-move source changed or became ineligible before target commit");
        }
        if (DesktopItemSourceLocks.isSlotLocked(player, target)
            || !target.isActive()
           ) {
            return 0;
        }

        if (!ItemStack.matches(targetBefore, target.getItem())) {
            throw new IllegalStateException("Quick-move target changed before target commit");
        }

        ItemStack attempt = sourceBefore.copy();
        ItemStack[] returnedRemainder = {null};
        withMenuNetworkSession(player, sessions, targetMenu, () ->
            returnedRemainder[0] = target.safeInsert(attempt)
        );
        ItemStack remainder = returnedRemainder[0];
        if (remainder == null
            || !remainder.isEmpty() && !ItemStack.isSameItemSameTags(sourceBefore, remainder)
            || remainder.getCount() < 0
            || remainder.getCount() > sourceBefore.getCount()) {
            throw new IllegalStateException("Quick-move target returned an invalid remainder");
        }
        int inserted = sourceBefore.getCount() - remainder.getCount();
        if (inserted <= 0) {
            if (!ItemStack.matches(targetBefore, target.getItem())) {
                throw new IllegalStateException("Quick-move target changed without consuming an item");
            }
            verifyQuickMoveTransaction(player, carriedBefore, source, moving, plans);
            return 0;
        }

        ItemStack expectedTarget = targetBefore.isEmpty()
            ? sourceBefore.copyWithCount(inserted)
            : targetBefore.copyWithCount(saturatingAdd(targetBefore.getCount(), inserted));
        if (!ItemStack.matches(expectedTarget, target.getItem())) {
            throw new IllegalStateException("Quick-move target gain could not be verified");
        }
        if (!ItemStack.matches(sourceBefore, source.slot.getItem())) {
            throw new IllegalStateException("Quick-move source changed during target insertion");
        }
        plan.expect(expectedTarget);
        verifyQuickMoveTransaction(player, carriedBefore, source, moving, plans);

        ItemStack sourceAfter = sourceBefore.copy();
        sourceAfter.shrink(inserted);
        withMenuNetworkSession(player, sessions, source.menu, () ->
            source.slot.setByPlayer(sourceAfter.copy())
        );
        // The shared remainder is updated only after both physical slots have committed.
        moving.setCount(sourceAfter.getCount());
        verifyQuickMoveTransaction(player, carriedBefore, source, moving, plans);
        return inserted;
    }

    private static void rollbackQuickMoveTransaction(
        ServerPlayer player,
        PlayerSessions sessions,
        SlotSource source,
        ItemStack sourceBefore,
        ItemStack carriedBefore,
        AbstractContainerMenu targetMenu,
        List<QuickMoveTargetPlan> plans
    ) {
        RuntimeException rollbackFailure = null;
        boolean ambiguousPhysicalSlots = false;
        IdentityHashMap<Object, Set<Integer>> restoredPhysicalSlots = new IdentityHashMap<>();
        // Restore every credited side before replenishing the source. A callback may rewrite a
        // different target, so the aggregate is measured only after all target callbacks return.
        for (int index = plans.size() - 1; index >= 0; index--) {
            QuickMoveTargetPlan plan = plans.get(index);
            try {
                if (!matchesPhysicalSlot(
                        plan.menu(), plan.slot(), plan.physicalOwner(), plan.physicalIndex()
                    )
                    || !rememberPhysicalSlot(
                        restoredPhysicalSlots, plan.physicalOwner(), plan.physicalIndex()
                    )) {
                    ambiguousPhysicalSlots = true;
                    continue;
                }
                if (!ItemStack.matches(plan.before(), plan.slot().getItem())) {
                    restoreSlot(player, sessions, plan.menu(), plan.slot(), plan.before(), "quick-move target");
                }
            } catch (RuntimeException exception) {
                ambiguousPhysicalSlots = true;
                rollbackFailure = appendSuppressed(rollbackFailure, exception);
            }
        }

        int credited = aggregateQuickMoveCredit(plans, sourceBefore);
        ItemStack sourceRollback = expectedQuickMoveSourceForCredit(sourceBefore, credited);
        if (sourceRollback == null) {
            // A target now has an unmeasurable or excessive credit. Emptying the source is the
            // only fail-closed choice: restoring it could duplicate whatever the callback retained.
            sourceRollback = ItemStack.EMPTY;
            DesktopDebug.warn(
                "server quick-move rollback found ambiguous aggregate target credit player={} targets={}",
                player.getName().getString(),
                plans.size()
            );
        }
        try {
            if (!matchesPhysicalSlot(
                    source.menu, source.slot, source.physicalOwner, source.physicalIndex
                )) {
                ambiguousPhysicalSlots = true;
            } else if (!ItemStack.matches(sourceRollback, source.slot.getItem())) {
                restoreSlot(player, sessions, source.menu, source.slot, sourceRollback, "quick-move source");
            }
        } catch (RuntimeException exception) {
            rollbackFailure = appendSuppressed(rollbackFailure, exception);
        }

        // Restoring the source invokes another arbitrary callback which may rewrite any target.
        // Reconcile once more against the new aggregate, then verify the complete transaction.
        int finalCredit = aggregateQuickMoveCredit(plans, sourceBefore);
        ItemStack reconciledSource = expectedQuickMoveSourceForCredit(sourceBefore, finalCredit);
        if (reconciledSource == null) {
            reconciledSource = ItemStack.EMPTY;
        }
        try {
            if (!matchesPhysicalSlot(
                    source.menu, source.slot, source.physicalOwner, source.physicalIndex
                )) {
                ambiguousPhysicalSlots = true;
            } else if (!ItemStack.matches(reconciledSource, source.slot.getItem())) {
                restoreSlot(player, sessions, source.menu, source.slot, reconciledSource, "quick-move reconciled source");
            }
        } catch (RuntimeException exception) {
            rollbackFailure = appendSuppressed(rollbackFailure, exception);
        }

        try {
            if (!ItemStack.matches(carriedBefore, player.inventoryMenu.getCarried())) {
                setSharedCarried(player, sessions, carriedBefore);
            }
            if (!ItemStack.matches(carriedBefore, player.inventoryMenu.getCarried())) {
                throw new IllegalStateException("Quick-move rollback could not restore the canonical cursor");
            }
        } catch (RuntimeException exception) {
            rollbackFailure = appendSuppressed(rollbackFailure, exception);
        }

        int verifiedCredit = aggregateQuickMoveCredit(plans, sourceBefore);
        ItemStack verifiedSource = expectedQuickMoveSourceForCredit(sourceBefore, verifiedCredit);
        if (verifiedSource == null
            || !matchesPhysicalSlot(
                source.menu, source.slot, source.physicalOwner, source.physicalIndex
            )
            || !ItemStack.matches(verifiedSource, source.slot.getItem())) {
            rollbackFailure = appendSuppressed(
                rollbackFailure,
                new IllegalStateException("Quick-move rollback could not establish a balanced source/targets transaction")
            );
        } else if (verifiedCredit != 0) {
            rollbackFailure = appendSuppressed(
                rollbackFailure,
                new IllegalStateException("Quick-move rollback retained a balanced partial transfer")
            );
        }
        if (ambiguousPhysicalSlots) {
            rollbackFailure = appendSuppressed(
                rollbackFailure,
                new IllegalStateException("Quick-move rollback encountered a rebound or aliased physical slot")
            );
        }
        if (rollbackFailure != null) {
            throw rollbackFailure;
        }
    }

    private static int aggregateQuickMoveCredit(List<QuickMoveTargetPlan> plans, ItemStack sourceBefore) {
        int credited = 0;
        IdentityHashMap<Object, Set<Integer>> physicalSlots = new IdentityHashMap<>();
        for (QuickMoveTargetPlan plan : plans) {
            int targetCredit;
            try {
                if (!matchesPhysicalSlot(
                        plan.menu(), plan.slot(), plan.physicalOwner(), plan.physicalIndex()
                    )
                    || !rememberPhysicalSlot(
                        physicalSlots, plan.physicalOwner(), plan.physicalIndex()
                    )) {
                    return -1;
                }
                targetCredit = observedTargetGain(plan.before(), plan.slot().getItem(), sourceBefore);
            } catch (RuntimeException exception) {
                return -1;
            }
            if (targetCredit < 0 || targetCredit > sourceBefore.getCount() - credited) {
                return -1;
            }
            credited += targetCredit;
        }
        return credited;
    }

    private static @Nullable ItemStack expectedQuickMoveSourceForCredit(ItemStack sourceBefore, int credited) {
        if (credited < 0 || credited > sourceBefore.getCount()) {
            return null;
        }
        ItemStack expected = sourceBefore.copy();
        expected.shrink(credited);
        return expected;
    }

    /** Returns the observable credit relative to a snapshot, or {@code -1} when it cannot be reconciled safely. */
    private static int observedTargetGain(ItemStack before, ItemStack current, ItemStack insertedType) {
        if (ItemStack.matches(before, current)) {
            return 0;
        }
        if (current.isEmpty() || !ItemStack.isSameItemSameTags(insertedType, current)) {
            return -1;
        }
        if (before.isEmpty()) {
            return current.getCount();
        }
        if (!ItemStack.isSameItemSameTags(before, current) || current.getCount() < before.getCount()) {
            return -1;
        }
        return current.getCount() - before.getCount();
    }

    private static void restoreSlot(
        ServerPlayer player,
        PlayerSessions sessions,
        AbstractContainerMenu menu,
        Slot slot,
        ItemStack snapshot,
        String description
    ) {
        ItemStack replaced = slot.getItem().copy();
        withMenuNetworkSession(player, sessions, menu, () ->
            slot.setByPlayer(snapshot.copy())
        );
        if (!ItemStack.matches(snapshot, slot.getItem())) {
            throw new IllegalStateException(description + " rollback could not be verified");
        }
    }

    private static void withMenuNetworkSession(
        ServerPlayer player,
        PlayerSessions sessions,
        AbstractContainerMenu menu,
        Runnable action
    ) {
        for (Session session : sessions.sessions.values()) {
            if (session.menu == menu) {
                withNetworkSession(player, session, action);
                return;
            }
        }
        action.run();
    }

    private static boolean matchesPhysicalSlot(
        AbstractContainerMenu menu,
        Slot slot,
        Object expectedOwner,
        int expectedIndex
    ) {
        return DesktopMenuSlots.physicalOwner(menu, slot) == expectedOwner
            && DesktopMenuSlots.physicalIndex(menu, slot) == expectedIndex;
    }

    private static int saturatingAdd(int first, int second) {
        long result = (long) first + second;
        return result >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) Math.max(0L, result);
    }

    private static RuntimeException appendSuppressed(
        @Nullable RuntimeException aggregate,
        RuntimeException exception
    ) {
        if (aggregate == null) {
            return exception;
        }
        aggregate.addSuppressed(exception);
        return aggregate;
    }

    private static final class QuickMoveTargetPlan {
        private final AbstractContainerMenu menu;
        private final Slot slot;
        private final Object physicalOwner;
        private final int physicalIndex;
        private final ItemStack before;
        private ItemStack expected;

        private QuickMoveTargetPlan(AbstractContainerMenu menu, Slot slot, ItemStack before) {
            this.menu = menu;
            this.slot = slot;
            this.physicalOwner = DesktopMenuSlots.physicalOwner(menu, slot);
            this.physicalIndex = DesktopMenuSlots.physicalIndex(menu, slot);
            this.before = before.copy();
            this.expected = before.copy();
        }

        private Slot slot() {
            return this.slot;
        }

        private AbstractContainerMenu menu() {
            return this.menu;
        }

        private Object physicalOwner() {
            return this.physicalOwner;
        }

        private int physicalIndex() {
            return this.physicalIndex;
        }

        private ItemStack before() {
            return this.before.copy();
        }

        private ItemStack expected() {
            return this.expected.copy();
        }

        private void expect(ItemStack stack) {
            this.expected = stack.copy();
        }
    }

    private static final class QuickMoveWorkBudget {
        private int remaining;

        private QuickMoveWorkBudget(int maximum) {
            this.remaining = Math.max(0, maximum);
        }

        private void consume(long work) {
            if (work < 0 || work > this.remaining) {
                throw new IllegalStateException("Quick-move transaction validation budget exceeded");
            }
            this.remaining -= (int) work;
        }
    }

    private record SessionAuthorization(
        int sessionId,
        AbstractContainerMenu menu,
        @Nullable Session session,
        String linkNode
    ) {
    }

    private static boolean rememberPhysicalSlot(
        IdentityHashMap<Object, Set<Integer>> slots,
        AbstractContainerMenu menu,
        Slot slot
    ) {
        Object owner = DesktopMenuSlots.physicalOwner(menu, slot);
        int index = DesktopMenuSlots.physicalIndex(menu, slot);
        return rememberPhysicalSlot(slots, owner, index);
    }

    private static boolean rememberPhysicalSlot(
        IdentityHashMap<Object, Set<Integer>> slots,
        @Nullable Object owner,
        int index
    ) {
        return owner != null && index >= 0 && slots.computeIfAbsent(owner, ignored -> new HashSet<>()).add(index);
    }

    private static boolean moveSlotStack(ServerPlayer player, net.minecraft.world.inventory.Slot sourceSlot, List<net.minecraft.world.inventory.Slot> targets) {
        if (!sourceSlot.isActive() || !sourceSlot.hasItem() || !sourceSlot.mayPickup(player)) {
            return false;
        }

        ItemStack sourceStack = sourceSlot.getItem();
        ItemStack original = sourceStack.copy();
        ItemStack moving = sourceStack.copy();
        int originalCount = moving.getCount();
        insertIntoMatchingSlots(sourceSlot, targets, moving);
        insertIntoEmptySlots(sourceSlot, targets, moving);

        int moved = originalCount - moving.getCount();
        if (moved <= 0) {
            return false;
        }

        ItemStack taken = original.copyWithCount(moved);
        sourceStack.shrink(moved);
        if (sourceStack.isEmpty()) {
            sourceSlot.setByPlayer(ItemStack.EMPTY);
        } else {
            sourceSlot.setChanged();
        }
        sourceSlot.onTake(player, taken);
        return true;
    }

    private static void insertIntoMatchingSlots(net.minecraft.world.inventory.Slot sourceSlot, List<net.minecraft.world.inventory.Slot> targets, ItemStack moving) {
        if (!moving.isStackable()) {
            return;
        }

        for (net.minecraft.world.inventory.Slot target : targets) {
            if (moving.isEmpty()) {
                return;
            }
            if (sameBackingSlot(target, sourceSlot) || !target.isActive() || !target.hasItem()) {
                continue;
            }
            if (!ItemStack.isSameItemSameTags(moving, target.getItem())) {
                continue;
            }
            target.safeInsert(moving);
        }
    }

    private static void insertIntoEmptySlots(net.minecraft.world.inventory.Slot sourceSlot, List<net.minecraft.world.inventory.Slot> targets, ItemStack moving) {
        for (net.minecraft.world.inventory.Slot target : targets) {
            if (moving.isEmpty()) {
                return;
            }
            if (sameBackingSlot(target, sourceSlot) || !target.isActive() || target.hasItem()) {
                continue;
            }
            target.safeInsert(moving);
        }
    }

    private static boolean sameBackingSlot(Slot first, Slot second) {
        return first.container == second.container
            && first.getContainerSlot() == second.getContainerSlot();
    }

    private static List<net.minecraft.world.inventory.Slot> containerSlots(AbstractContainerMenu menu, ServerPlayer player) {
        List<net.minecraft.world.inventory.Slot> slots = new ArrayList<>();
        for (net.minecraft.world.inventory.Slot slot : menu.slots) {
            if (!isPlayerInventorySlot(player, slot) && !InventoryExpansion.isExtraSlot(slot)) {
                slots.add(slot);
            }
        }
        return slots;
    }

    private static List<net.minecraft.world.inventory.Slot> mainInventorySlots(ServerPlayer player) {
        List<net.minecraft.world.inventory.Slot> slots = new ArrayList<>();
        for (net.minecraft.world.inventory.Slot slot : player.inventoryMenu.slots) {
            if (InventoryExpansion.isMainInventorySlot(player, slot)) {
                slots.add(slot);
            }
        }
        slots.sort(Comparator.comparingInt(InventoryExpansion::storageOrder));
        return slots;
    }

    private static List<net.minecraft.world.inventory.Slot> hotbarSlots(ServerPlayer player) {
        List<net.minecraft.world.inventory.Slot> slots = new ArrayList<>();
        for (net.minecraft.world.inventory.Slot slot : player.inventoryMenu.slots) {
            if (isPlayerInventorySlot(player, slot) && slot.getContainerSlot() >= 0 && slot.getContainerSlot() < 9) {
                slots.add(slot);
            }
        }
        return slots;
    }

    private static boolean isPlayerInventorySlot(ServerPlayer player, net.minecraft.world.inventory.Slot slot) {
        return slot.container == player.getInventory();
    }

    private static @Nullable Container containerForMenu(AbstractContainerMenu menu) {
        if (menu instanceof ChestMenu chestMenu) {
            return chestMenu.getContainer();
        }

        return null;
    }

    private static boolean containsContainer(Container owner, Container target) {
        return owner == target || owner instanceof CompoundContainer compoundContainer && compoundContainer.contains(target);
    }

    private static void clickMenu(int debugId, ServerPlayer player, PlayerSessions sessions, AbstractContainerMenu menu, int slotIndex, int button, ClickType input, ItemStack clientCarried) {
        if (slotIndex != AbstractContainerMenu.SLOT_CLICKED_OUTSIDE
            && DesktopMenuSlots.slot(menu, slotIndex) == null) {
            DesktopDebug.trace("server click ignored id={} player={} menu={} slot={} reason=out-of-range", debugId, player.getName().getString(), menu.containerId, slotIndex);
            return;
        }

        ItemStack slotBefore = serverSlotStack(menu, slotIndex);
        ItemStack carriedBefore = player.inventoryMenu.getCarried().copy();
        ItemStack menuCarriedBefore = menu.getCarried().copy();
        ItemStack effectiveCarried = player.getAbilities().instabuild ? clientCarried.copy() : player.inventoryMenu.getCarried().copy();
        DesktopDebug.trace(
            "server click before id={} player={} menu={} slot={} button={} input={} slotBefore={} sessionsCarried={} menuCarried={} clientCarried={} effectiveCarried={} creative={}",
            debugId,
            player.getName().getString(),
            menu.containerId,
            slotIndex,
            button,
            input,
            slotBefore,
            carriedBefore,
            menuCarriedBefore,
            clientCarried,
            effectiveCarried,
            player.getAbilities().instabuild
        );
        DesktopDebug.probe(
            "server click before id={} player={} menu={} slot={} button={} input={} slotBefore={} sessionsCarried={} menuCarried={} clientCarried={} effectiveCarried={} creative={}",
            debugId,
            player.getName().getString(),
            menu.containerId,
            slotIndex,
            button,
            input,
            slotBefore,
            carriedBefore,
            menuCarriedBefore,
            clientCarried,
            effectiveCarried,
            player.getAbilities().instabuild
        );
        withMenuCarried(player, sessions, menu, effectiveCarried, () -> {
            menu.clicked(slotIndex, button, input, player);
            return null;
        });
        DesktopDebug.trace(
            "server click after id={} player={} menu={} slot={} slotAfter={} sessionsCarried={} playerMenuCarried={}",
            debugId,
            player.getName().getString(),
            menu.containerId,
            slotIndex,
            serverSlotStack(menu, slotIndex),
            player.inventoryMenu.getCarried(),
            player.inventoryMenu.getCarried()
        );
        DesktopDebug.probe(
            "server click after id={} player={} menu={} slot={} input={} slotAfter={} sessionsCarried={} playerMenuCarried={}",
            debugId,
            player.getName().getString(),
            menu.containerId,
            slotIndex,
            input,
            serverSlotStack(menu, slotIndex),
            player.inventoryMenu.getCarried(),
            player.inventoryMenu.getCarried()
        );
    }

    private static ItemStack serverSlotStack(AbstractContainerMenu menu, int slotIndex) {
        Slot slot = DesktopMenuSlots.slot(menu, slotIndex);
        if (slot == null) {
            return ItemStack.EMPTY;
        }

        return slot.getItem().copy();
    }

    private static void syncCarried(ServerPlayer player, PlayerSessions sessions) {
        ItemStack canonical = player.inventoryMenu.getCarried().copy();
        for (Session session : sessions.sessions.values()) {
            if (!ItemStack.matches(canonical, session.menu.getCarried())) {
                session.menu.setCarried(canonical.copy());
            }
        }
        syncPlayerMenuState(player);
        DesktopDebug.trace("server sync carried player={} stack={}", player.getName().getString(), canonical);
        send(player, new DesktopCarriedPayload(canonical));
    }

    private static void syncPlayerMenuState(ServerPlayer player) {
        if (player.inventoryMenu.slots.isEmpty()) {
            return;
        }

        // A single unchanged slot acknowledges the new state before the cursor becomes interactive.
        int slotIndex = Math.min(9, player.inventoryMenu.slots.size() - 1);
        Slot slot = player.inventoryMenu.getSlot(slotIndex);
        send(player, new DesktopSlotPayload(
            DesktopPackets.PLAYER_MENU_SESSION,
            slotIndex,
            player.inventoryMenu.getStateId(),
            slot.getItem().copy()
        ));
    }

    private static void syncPlayerMenu(ServerPlayer player) {
        InventoryExpansion.appendMissingMenuSlots(player.inventoryMenu, player);
        int stateId = player.inventoryMenu.getStateId();
        for (int slotIndex = 0; slotIndex < player.inventoryMenu.slots.size(); slotIndex++) {
            Slot slot = player.inventoryMenu.slots.get(slotIndex);
            send(player, new DesktopSlotPayload(
                DesktopPackets.PLAYER_MENU_SESSION,
                slotIndex,
                stateId,
                slot.getItem().copy()
            ));
        }
        DesktopDebug.trace("server sync player-menu player={} slots={}", player.getName().getString(), player.inventoryMenu.slots.size());
    }

    private static void setSharedCarried(ServerPlayer player, PlayerSessions sessions, ItemStack stack) {
        ItemStack carried = stack.copy();
        player.inventoryMenu.setCarried(carried.copy());
        for (Session session : sessions.sessions.values()) {
            session.menu.setCarried(carried.copy());
        }
    }

    public static void syncCraftingResult(ServerPlayer player, CraftingMenu menu) {
        PlayerSessions sessions = existingSessions(player);
        if (sessions == null || !sessions.canOpenSessions()) {
            return;
        }

        for (Session session : sessions.sessions.values()) {
            if (session.menu == menu && session.visibleToClient) {
                int slotIndex = menu.getResultSlotIndex();
                Slot resultSlot = menu.getSlot(slotIndex);
                send(player, new DesktopSlotPayload(
                    session.sessionId,
                    slotIndex,
                    menu.getStateId(),
                    resultSlot.getItem().copy()
                ));
                return;
            }
        }
    }

    private static <T> T withCanonicalCarried(
        ServerPlayer player,
        PlayerSessions sessions,
        AbstractContainerMenu menu,
        Supplier<T> operation
    ) {
        return withMenuCarried(player, sessions, menu, player.inventoryMenu.getCarried(), operation);
    }

    private static <T> T withMenuCarried(
        ServerPlayer player,
        PlayerSessions sessions,
        AbstractContainerMenu menu,
        ItemStack initialCarried,
        Supplier<T> operation
    ) {
        ItemStack canonicalBefore = player.inventoryMenu.getCarried().copy();
        menu.setCarried(initialCarried.copy());
        try {
            T result = operation.get();
            setSharedCarried(player, sessions, menu.getCarried());
            return result;
        } catch (RuntimeException | Error exception) {
            setSharedCarried(player, sessions, canonicalBefore);
            throw exception;
        }
    }

    private static void syncMerchantOffers(ServerPlayer player, Session session) {
        if (!(session.menu instanceof MerchantMenu merchantMenu)) {
            return;
        }

        send(player, new DesktopMerchantOffersPayload(
            session.sessionId,
            merchantMenu.getOffers(),
            merchantMenu.getTraderLevel(),
            merchantMenu.getTraderXp(),
            merchantMenu.showProgressBar(),
            merchantMenu.canRestock()
        ));
    }

    private static void closeSession(ServerPlayer player, int sessionId, boolean notifyClient) {
        PlayerSessions sessions = existingSessions(player);
        if (sessions != null) {
            DesktopDebug.log("server close request player={} session={} notify={}", player.getName().getString(), sessionId, notifyClient);
            sessions.close(player, sessionId, notifyClient);
        }
    }

    private static void closeSessionFromClient(ServerPlayer player, DesktopCloseSessionPayload payload) {
        PlayerSessions sessions = existingSessions(player);
        if (sessions == null || !sessions.controlRate.tryConsume(System.nanoTime())) {
            return;
        }
        closeSession(player, payload.sessionId(), true);
    }

    private static void setSessionPin(ServerPlayer player, DesktopSessionPinPayload payload) {
        PlayerSessions sessions = existingSessions(player);
        if (sessions == null || !sessions.isReady() || !sessions.controlRate.tryConsume(System.nanoTime())) {
            DesktopDebug.trace("server pin dropped player={} session={} reason=not-ready", player.getName().getString(), payload.sessionId());
            return;
        }

        Session session = sessions.sessions.get(payload.sessionId());
        if (session == null) {
            DesktopDebug.trace("server pin dropped player={} session={} reason=missing-session", player.getName().getString(), payload.sessionId());
            return;
        }

        session.ghostPinned = payload.pinMode() == DesktopPackets.PIN_MODE_GHOST_PINNED;
        DesktopDebug.trace("server pin player={} session={} ghostPinned={}", player.getName().getString(), payload.sessionId(), session.ghostPinned);
        session.dispatchPinChanged(player, sessions);
    }

    private static void setSessionVisibility(ServerPlayer player, DesktopSessionVisibilityPayload payload) {
        PlayerSessions sessions = existingSessions(player);
        if (sessions == null || !sessions.isReady() || !sessions.controlRate.tryConsume(System.nanoTime())) {
            DesktopDebug.trace("server visibility dropped player={} session={} reason=not-ready", player.getName().getString(), payload.sessionId());
            return;
        }
        if (payload.visible() && !sessions.snapshotRate.tryConsume(System.nanoTime())) {
            DesktopDebug.trace("server visibility dropped player={} session={} reason=snapshot-rate", player.getName().getString(), payload.sessionId());
            return;
        }

        Session session = sessions.sessions.get(payload.sessionId());
        if (session == null) {
            DesktopDebug.trace("server visibility dropped player={} session={} reason=missing-session", player.getName().getString(), payload.sessionId());
            return;
        }

        if (!session.menu.stillValid(player)) {
            DesktopDebug.log("server visibility invalid player={} session={} title={}", player.getName().getString(), session.sessionId, session.title.getString());
            sessions.rememberDormantGhost(player, session, "visibility-invalid");
            sessions.close(player, session.sessionId, true);
            return;
        }

        sessions.setVisible(player, session, payload.visible(), true);
    }

    private static void setSourceLink(ServerPlayer player, DesktopSourceLinkPayload payload) {
        PlayerSessions sessions = existingSessions(player);
        if (sessions == null || !sessions.isReady() || player.isSpectator()
            || !sessions.controlRate.tryConsume(System.nanoTime())) {
            return;
        }
        SourceGrant firstGrant = sessions.sourceGrant(player, payload.firstSourceGrantToken());
        SourceGrant secondGrant = sessions.sourceGrant(player, payload.secondSourceGrantToken());
        if (firstGrant == null || secondGrant == null) {
            DesktopDebug.warn("server source link rejected player={} reason=invalid-grant", player.getName().getString());
            return;
        }
        Session firstSession = sessions.sessions.get(payload.firstSessionId());
        Session secondSession = sessions.sessions.get(payload.secondSessionId());
        if (firstSession == null || secondSession == null
            || !firstSession.sourceKey.equals(firstGrant.sourceKey())
            || !secondSession.sourceKey.equals(secondGrant.sourceKey())) {
            DesktopDebug.warn("server source link rejected player={} reason=session-grant-mismatch", player.getName().getString());
            return;
        }
        if (payload.linked()) {
            if (!firstSession.visibleToClient || !secondSession.visibleToClient
                || !firstSession.menu.stillValid(player) || !secondSession.menu.stillValid(player)) {
                DesktopDebug.warn("server source link rejected player={} reason=sources-not-open", player.getName().getString());
                return;
            }
            if (!sessions.sourceLinks.link(payload.firstSourceGrantToken(), payload.secondSourceGrantToken())) {
                DesktopDebug.warn("server source link rejected player={} reason=graph-limit", player.getName().getString());
            }
        } else {
            sessions.sourceLinks.unlink(payload.firstSourceGrantToken(), payload.secondSourceGrantToken());
        }
    }

    private static void openLinkedSources(ServerPlayer player, DesktopOpenLinkedSourcesPayload payload) {
        PlayerSessions sessions = existingSessions(player);
        if (sessions == null || !sessions.isReady()) {
            DesktopDebug.trace("server linked open dropped player={} reason=not-ready", player.getName().getString());
            return;
        }
        if (player.isSpectator()) {
            DesktopDebug.trace("server linked open dropped player={} reason=spectator", player.getName().getString());
            return;
        }
        if (!sessions.allowLinkedOpen(player)) {
            DesktopDebug.trace("server linked open dropped player={} reason=rate-limit", player.getName().getString());
            return;
        }

        SourceGrant originGrant = sessions.sourceGrant(player, payload.originSourceGrantToken());
        if (originGrant == null) {
            DesktopDebug.warn("server linked open skipped player={} reason=invalid-origin-grant", player.getName().getString());
            return;
        }
        List<Long> connectedGrants = sessions.sourceLinks.connectedComponent(
            payload.originSourceGrantToken(),
            DesktopProtocol.MAX_LINK_NODES
        );
        if (connectedGrants.size() <= 1) {
            DesktopDebug.trace("server linked open skipped player={} reason=no-server-linked-component", player.getName().getString());
            return;
        }

        for (long sourceGrantToken : connectedGrants) {
            SourceGrant grant = sessions.sourceGrant(player, sourceGrantToken);
            if (grant == null) {
                DesktopDebug.warn("server linked open skipped player={} grant={} reason=not-server-authorized", player.getName().getString(), sourceGrantToken);
                continue;
            }
            String sourceKey = grant.sourceKey();

            Session existing = sessions.sessionForSourceKey(sourceKey);
            if (existing != null) {
                if (!existing.menu.stillValid(player)) {
                    sessions.rememberDormantGhost(player, existing, "linked-open-invalid");
                    sessions.close(player, existing.sessionId, true);
                    continue;
                }
                if (!existing.visibleToClient) {
                    sessions.setVisible(player, existing, true, true);
                }
                continue;
            }

            MenuProvider provider = providerForDormantGhost(player, sourceKey);
            if (provider == null) {
                DesktopDebug.trace("server linked open skipped player={} source={} reason=unavailable", player.getName().getString(), sourceKey);
                continue;
            }

            DesktopDebug.log("server linked open player={} source={} title={}", player.getName().getString(), sourceKey, provider.getDisplayName().getString());
            openMenuSession(player, provider, sourceKey, false, false, true);
        }
    }

    private static int nextSessionId(ServerPlayer player) {
        PlayerSessions sessions = sessions(player);
        for (int attempt = 0; attempt <= MAX_SESSIONS; attempt++) {
            int next = sessions.nextSessionId;
            sessions.nextSessionId = next <= 0 || next == Integer.MAX_VALUE ? 1 : next + 1;
            if (next > 0 && !sessions.sessions.containsKey(next)) {
                return next;
            }
        }
        throw new IllegalStateException("Unable to allocate a desktop session id");
    }

    private static PlayerSessions sessions(ServerPlayer player) {
        return PLAYERS.computeIfAbsent(player.connection, connection -> new PlayerSessions());
    }

    private static @Nullable PlayerSessions existingSessions(ServerPlayer player) {
        return PLAYERS.get(player.connection);
    }

    private static <K, V> void trimOldest(LinkedHashMap<K, V> values, int maximumSize) {
        while (values.size() > maximumSize) {
            Iterator<K> iterator = values.keySet().iterator();
            if (!iterator.hasNext()) {
                return;
            }
            iterator.next();
            iterator.remove();
        }
    }

    private static void send(ServerPlayer player, DesktopPacket payload) {
        if (ServerPlayNetworking.canSend(player, payload.id())) {
            ServerPlayNetworking.send(player, payload.id(), DesktopPackets.toBuffer(payload));
        }
    }

    private static final class PlayerSessions {
        private final LinkedHashMap<Integer, Session> sessions = new LinkedHashMap<>();
        private final LinkedHashMap<String, DormantGhostSource> dormantGhostSources = new LinkedHashMap<>();
        private final LinkedHashMap<Long, SourceGrant> sourceGrants = new LinkedHashMap<>();
        private final Map<String, Long> sourceGrantTokensByKey = new HashMap<>();
        private final BoundedLinkGraph<Long> sourceLinks = new BoundedLinkGraph<>();
        private final DesktopConnectionState connectionState = new DesktopConnectionState();
        private Set<ResourceLocation> forcedMenuIds = Set.of();
        private int nextSessionId = 1;
        private int dormantGhostProbeTicks;
        private long playerMenuToken;
        private boolean replacingPlayer;
        private int serverHandlerCallbackDepth;
        private boolean serverHandlerBroadcastPending;
        private final TokenBucket operationRate = new TokenBucket(40.0D, 80.0D, System.nanoTime());
        private final TokenBucket resyncRate = new TokenBucket(1.0D, 1.0D, System.nanoTime());
        private final TokenBucket controlRate = new TokenBucket(8.0D, DesktopProtocol.MAX_DESKTOP_SESSIONS, System.nanoTime());
        private final TokenBucket snapshotRate = new TokenBucket(1.0D, 2.0D, System.nanoTime());
        private final TokenBucket modeRate = new TokenBucket(2.0D, 4.0D, System.nanoTime());
        private final TokenBucket linkedOpenRate = new TokenBucket(4.0D, 2.0D, System.nanoTime());
        private final TokenBucket jeiTransferRate = new TokenBucket(4.0D, 2.0D, System.nanoTime());

        private boolean isReady() {
            return !this.replacingPlayer && this.connectionState.isUiEnabled();
        }

        private boolean hasCapability(long capability, boolean requireUi) {
            return !this.replacingPlayer && this.connectionState.authorizes(
                this.connectionState.connectionNonce(),
                capability,
                requireUi
            );
        }

        private boolean canOpenSessions() {
            return this.hasCapability(DesktopProtocol.CAP_CUSTOM_WINDOWS, true);
        }

        private boolean authorizesSession(int sessionId, long sessionToken) {
            if (sessionToken == 0L) {
                return false;
            }
            if (sessionId == DesktopPackets.PLAYER_MENU_SESSION) {
                return sessionToken == this.playerMenuToken;
            }
            Session session = this.sessions.get(sessionId);
            return session != null && session.sessionToken == sessionToken;
        }

        private boolean sessionStateMatches(ServerPlayer player, int sessionId, int expectedStateId) {
            if (expectedStateId < 0) {
                return false;
            }
            if (sessionId == DesktopPackets.PLAYER_MENU_SESSION) {
                return player.inventoryMenu.getStateId() == expectedStateId;
            }
            Session session = this.sessions.get(sessionId);
            return session != null && session.menu.getStateId() == expectedStateId;
        }

        private void resyncSession(ServerPlayer player, int sessionId) {
            if (sessionId == DesktopPackets.PLAYER_MENU_SESSION) {
                syncPlayerMenu(player);
                syncCarried(player, this);
                return;
            }
            Session session = this.sessions.get(sessionId);
            if (session != null) {
                session.menu.sendAllDataToRemote();
                syncCarried(player, this);
            }
        }

        private void resetForHello(ServerPlayer player) {
            this.closeAll(player, true);
            this.connectionState.reset();
            this.playerMenuToken = 0L;
            this.forcedMenuIds = Set.of();
            this.replacingPlayer = false;
            this.nextSessionId = 1;
            this.dormantGhostProbeTicks = 0;
        }

        private void setForcedMenuIds(List<String> requestedIds) {
            HashSet<ResourceLocation> validated = new HashSet<>();
            for (String requestedId : requestedIds) {
                if (validated.size() >= DesktopProtocol.MAX_FORCED_MENU_IDS) {
                    break;
                }
                ResourceLocation id = ResourceLocation.tryParse(requestedId);
                if (id != null && BuiltInRegistries.MENU.containsKey(id)) {
                    validated.add(id);
                }
            }
            this.forcedMenuIds = Set.copyOf(validated);
        }

        private void add(ServerPlayer player, Session session) {
            while (this.sessions.size() >= MAX_SESSIONS) {
                Iterator<Integer> iterator = this.sessions.keySet().iterator();
                if (!iterator.hasNext()) {
                    break;
                }
                Integer sessionId = iterator.next();
                DesktopDebug.log("server session cap close player={} session={}", player.getName().getString(), sessionId);
                this.close(player, sessionId, true);
            }

            this.initialize(player, session);
        }

        private boolean replace(ServerPlayer player, Session oldSession, Session replacement) {
            if (this.sessions.get(oldSession.sessionId) != oldSession || !oldSession.sourceKey.equals(replacement.sourceKey)) {
                return false;
            }
            this.initialize(player, replacement);
            this.close(player, oldSession.sessionId, true);
            return true;
        }

        private void initialize(ServerPlayer player, Session session) {
            this.sessions.put(session.sessionId, session);
            session.initializeServerHandler(player, this);
            session.menu.setCarried(player.inventoryMenu.getCarried().copy());
            SessionSynchronizer synchronizer = new SessionSynchronizer(player, session);
            send(player, new DesktopSessionAuthorizationPayload(session.sessionId, session.sessionToken, session.sourceGrantToken));
            withNetworkSession(player, session, () -> DesktopSynchronizerOverride.run(
                synchronizer,
                () -> session.menu.setSynchronizer(synchronizer)
            ));
            DesktopDebug.log("server session initial sync requested player={} session={} state={} slots={}", player.getName().getString(), session.sessionId, session.menu.getStateId(), session.menu.slots.size());
            DesktopDebug.log("server session add player={} session={} title={} count={}", player.getName().getString(), session.sessionId, session.title.getString(), this.sessions.size());
            session.dispatchOpened(player, this);
        }

        private boolean closeBySourceKey(ServerPlayer player, @Nullable String sourceKey, boolean notifyClient) {
            if (sourceKey == null || sourceKey.isEmpty()) {
                return false;
            }

            boolean handled = false;
            for (Session session : List.copyOf(this.sessions.values())) {
                if (sourceKey.equals(session.sourceKey)) {
                    if (session.ghostPinned) {
                        this.setVisible(player, session, !session.visibleToClient, notifyClient);
                    } else {
                        this.close(player, session.sessionId, notifyClient);
                    }
                    handled = true;
                }
            }

            return handled;
        }

        private void setVisible(ServerPlayer player, Session session, boolean visible, boolean notifyClient) {
            if (session.visibleToClient == visible) {
                return;
            }

            session.visibleToClient = visible;
            DesktopDebug.log("server session visibility player={} session={} title={} visible={} notify={}", player.getName().getString(), session.sessionId, session.title.getString(), visible, notifyClient);
            if (notifyClient) {
                send(player, new DesktopSessionVisibilityPayload(session.sessionId, visible));
            }
            if (visible) {
                session.menu.sendAllDataToRemote();
                syncCarried(player, this);
            }
            session.dispatchVisibilityChanged(player, this);
        }

        private void close(ServerPlayer player, int sessionId, boolean notifyClient) {
            Session session = this.sessions.remove(sessionId);
            if (session == null) {
                DesktopDebug.trace("server close ignored player={} session={} reason=missing", player.getName().getString(), sessionId);
                return;
            }

            DesktopDebug.log("server session close player={} session={} title={} notify={}", player.getName().getString(), sessionId, session.title.getString(), notifyClient);
            session.dispatchClosed(player, this);
            // Session menus mirror the cursor owned by player.inventoryMenu.
            // removed() returns a carried stack, so never let a mirror reach it.
            session.menu.setCarried(ItemStack.EMPTY);
            session.menu.removed(player);
            if (notifyClient) {
                send(player, new DesktopSessionClosedPayload(sessionId));
            }
        }

        private void closeAll(ServerPlayer player, boolean notifyClient) {
            for (Integer sessionId : List.copyOf(this.sessions.keySet())) {
                this.close(player, sessionId, notifyClient);
            }
            this.dormantGhostSources.clear();
            this.sourceGrants.clear();
            this.sourceGrantTokensByKey.clear();
            this.sourceLinks.clear();
        }

        private void tick(ServerPlayer player) {
            for (Session session : List.copyOf(this.sessions.values())) {
                if (!session.menu.stillValid(player)) {
                    DesktopDebug.log("server session invalid player={} session={} title={}", player.getName().getString(), session.sessionId, session.title.getString());
                    this.rememberDormantGhost(player, session, "invalid");
                    this.close(player, session.sessionId, true);
                } else {
                    session.menu.broadcastChanges();
                    session.dispatchTick(player, this);
                }
            }
            this.reopenDormantGhosts(player);
        }

        private void broadcastAll(ServerPlayer player) {
            for (Session session : List.copyOf(this.sessions.values())) {
                session.menu.broadcastChanges();
            }
            player.inventoryMenu.broadcastChanges();
            syncCarried(player, this);
        }

        private <T> T invokeServerHandlerCallback(ServerPlayer player, Supplier<T> callback) {
            this.serverHandlerCallbackDepth++;
            try {
                return callback.get();
            } finally {
                this.serverHandlerCallbackDepth--;
                this.flushRequestedBroadcastChanges(player);
            }
        }

        private void requestBroadcastChanges(ServerPlayer player) {
            // The detached menu is not a cursor authority. withCanonicalCarried commits a
            // successful callback before this request flushes; the flush always syncs from
            // player.inventoryMenu instead of snapshotting the addon's menu-carried value.
            this.serverHandlerBroadcastPending = true;
            this.flushRequestedBroadcastChanges(player);
        }

        private void flushRequestedBroadcastChanges(ServerPlayer player) {
            if (this.serverHandlerCallbackDepth != 0 || !this.serverHandlerBroadcastPending) {
                return;
            }
            this.serverHandlerBroadcastPending = false;
            this.broadcastAll(player);
        }

        private void rememberDormantGhost(ServerPlayer player, Session session, String reason) {
            if (!session.ghostPinned || !isBlockBackedSourceKey(session.sourceKey) || !this.isSourceAuthorized(player, session.sourceKey)) {
                return;
            }

            this.dormantGhostSources.put(
                session.sourceKey,
                new DormantGhostSource(session.sourceKey, player.serverLevel().getGameTime() + DesktopProtocol.DORMANT_SOURCE_TTL_TICKS)
            );
            trimOldest(this.dormantGhostSources, MAX_DORMANT_GHOST_SOURCES);
            DesktopDebug.log("server dormant ghost remember source={} session={} title={} reason={}", session.sourceKey, session.sessionId, session.title.getString(), reason);
        }

        private void reopenDormantGhosts(ServerPlayer player) {
            if (this.dormantGhostSources.isEmpty()) {
                return;
            }

            this.dormantGhostProbeTicks++;
            if (this.dormantGhostProbeTicks % DORMANT_GHOST_REOPEN_INTERVAL_TICKS != 0) {
                return;
            }

            for (DormantGhostSource dormant : List.copyOf(this.dormantGhostSources.values())) {
                if (player.serverLevel().getGameTime() > dormant.expiresAtTick()) {
                    this.dormantGhostSources.remove(dormant.sourceKey());
                    continue;
                }
                if (this.hasSessionForSourceKey(dormant.sourceKey())) {
                    this.dormantGhostSources.remove(dormant.sourceKey());
                    continue;
                }
                if (!this.isSourceAuthorized(player, dormant.sourceKey())) {
                    this.dormantGhostSources.remove(dormant.sourceKey());
                    continue;
                }

                MenuProvider provider = providerForDormantGhost(player, dormant.sourceKey());
                if (provider == null) {
                    continue;
                }

                this.dormantGhostSources.remove(dormant.sourceKey());
                DesktopDebug.log("server dormant ghost reopen player={} source={} title={}", player.getName().getString(), dormant.sourceKey(), provider.getDisplayName().getString());
                openMenuSession(player, provider, dormant.sourceKey(), false, true, false);
            }
        }

        private boolean allowLinkedOpen(ServerPlayer player) {
            return this.linkedOpenRate.tryConsume(System.nanoTime());
        }

        private boolean allowJeiTransfer(ServerPlayer player) {
            return this.jeiTransferRate.tryConsume(System.nanoTime());
        }

        private long authorizeSource(ServerPlayer player, String sourceKey) {
            SourceGrant grant = SourceGrant.capture(player, sourceKey);
            if (grant == null) {
                return 0L;
            }
            Long previousToken = this.sourceGrantTokensByKey.get(sourceKey);
            long token = previousToken == null || previousToken == 0L ? nextNonce() : previousToken;
            this.sourceGrants.remove(token);
            this.sourceGrants.put(token, grant);
            this.sourceGrantTokensByKey.put(sourceKey, token);
            this.trimSourceGrants();
            return token;
        }

        private boolean isSourceAuthorized(ServerPlayer player, String sourceKey) {
            Long token = this.sourceGrantTokensByKey.get(sourceKey);
            return token != null && this.sourceGrant(player, token) != null;
        }

        private long sourceGrantToken(String sourceKey) {
            return this.sourceGrantTokensByKey.getOrDefault(sourceKey, 0L);
        }

        private @Nullable SourceGrant sourceGrant(ServerPlayer player, long token) {
            if (token == 0L) {
                return null;
            }
            SourceGrant grant = this.sourceGrants.get(token);
            if (grant == null || !grant.matches(player)) {
                this.removeSourceGrant(token, grant);
                return null;
            }
            return grant;
        }

        private void trimSourceGrants() {
            while (this.sourceGrants.size() > MAX_SOURCE_GRANTS) {
                Iterator<Map.Entry<Long, SourceGrant>> iterator = this.sourceGrants.entrySet().iterator();
                if (!iterator.hasNext()) {
                    return;
                }
                Map.Entry<Long, SourceGrant> oldest = iterator.next();
                iterator.remove();
                this.sourceGrantTokensByKey.remove(oldest.getValue().sourceKey(), oldest.getKey());
                this.removeSourceLinkNode(oldest.getKey());
            }
        }

        private void removeSourceGrant(long token, @Nullable SourceGrant grant) {
            this.sourceGrants.remove(token);
            this.removeSourceLinkNode(token);
            if (grant != null) {
                this.sourceGrantTokensByKey.remove(grant.sourceKey(), token);
            }
        }

        private void removeSourceLinkNode(long token) {
            Set<Long> neighbors = this.sourceLinks.snapshot().get(token);
            if (neighbors == null) {
                return;
            }
            for (long neighbor : List.copyOf(neighbors)) {
                this.sourceLinks.unlink(token, neighbor);
            }
        }

        private boolean hasSessionForSourceKey(String sourceKey) {
            return this.sessionForSourceKey(sourceKey) != null;
        }

        private @Nullable Session sessionForSourceKey(String sourceKey) {
            for (Session session : this.sessions.values()) {
                if (sourceKey.equals(session.sourceKey)) {
                    return session;
                }
            }
            return null;
        }
    }

    private static final class Session {
        private final int sessionId;
        private final long sessionToken = nextNonce();
        private final AbstractContainerMenu menu;
        private final Component title;
        private final int specialKind;
        private final int entityId;
        private final int columns;
        private final int menuTypeId;
        private final String sourceKey;
        private final byte[] openingData;
        private final int replacesSessionId;
        private boolean initialSnapshotSent;
        private long sourceGrantToken;
        private boolean ghostPinned;
        private boolean visibleToClient = true;
        private @Nullable DesktopServerWindowHandler<AbstractContainerMenu, Object> serverHandler;
        private @Nullable Object serverState;

        private Session(int sessionId, AbstractContainerMenu menu, Component title, int specialKind, int entityId, int columns, int menuTypeId, String sourceKey) {
            this(sessionId, menu, title, specialKind, entityId, columns, menuTypeId, sourceKey, new byte[0], -1);
        }

        private Session(int sessionId, AbstractContainerMenu menu, Component title, int specialKind, int entityId, int columns, int menuTypeId, String sourceKey, byte[] openingData, int replacesSessionId) {
            this.sessionId = sessionId;
            this.menu = menu;
            this.title = title;
            this.specialKind = specialKind;
            this.entityId = entityId;
            this.columns = columns;
            this.menuTypeId = menuTypeId;
            this.sourceKey = sourceKey;
            this.openingData = openingData.clone();
            this.replacesSessionId = replacesSessionId;
        }

        private @Nullable MenuType<?> menuType() {
            if (this.menuTypeId < 0) {
                return null;
            }
            try {
                return this.menu.getType();
            } catch (UnsupportedOperationException exception) {
                return null;
            }
        }

        private String menuTypeDescription() {
            MenuType<?> menuType = this.menuType();
            return menuType == null ? "special:" + this.specialKind : String.valueOf(menuType);
        }

        private boolean transferSupported() {
            if (isVanillaCraftingTransferMenu(this.menu)) {
                return true;
            }
            return this.serverHandler != null && DesktopTransferValidators.supports(this.serverHandler);
        }

        private void initializeServerHandler(ServerPlayer player, PlayerSessions sessions) {
            MenuType<?> menuType = this.menuType();
            if (menuType == null) {
                return;
            }

            this.serverHandler = DesktopServerApi.findWindowHandler(menuType);
            if (this.serverHandler == null) {
                return;
            }

            DesktopServerWindowHandler<AbstractContainerMenu, Object> handler = this.serverHandler;
            try {
                sessions.invokeServerHandlerCallback(player, () -> {
                    this.serverState = withCanonicalCarried(
                        player,
                        sessions,
                        this.menu,
                        () -> handler.createState(new ServerSessionContext(player, sessions, this))
                    );
                    return null;
                });
            } catch (RuntimeException exception) {
                this.quarantineServerHandler(player, "create-state", exception);
            }
        }

        private void dispatchOpened(ServerPlayer player, PlayerSessions sessions) {
            if (this.serverHandler == null) {
                return;
            }
            DesktopServerWindowHandler<AbstractContainerMenu, Object> handler = this.serverHandler;
            try {
                sessions.invokeServerHandlerCallback(
                    player,
                    () -> withCanonicalCarried(player, sessions, this.menu, () -> {
                        handler.opened(new ServerSessionContext(player, sessions, this));
                        return null;
                    })
                );
            } catch (RuntimeException exception) {
                this.quarantineServerHandler(player, "opened", exception);
            }
        }

        private void dispatchTick(ServerPlayer player, PlayerSessions sessions) {
            if (this.serverHandler == null) {
                return;
            }
            DesktopServerWindowHandler<AbstractContainerMenu, Object> handler = this.serverHandler;
            try {
                sessions.invokeServerHandlerCallback(
                    player,
                    () -> withCanonicalCarried(player, sessions, this.menu, () -> {
                        handler.tick(new ServerSessionContext(player, sessions, this));
                        return null;
                    })
                );
            } catch (RuntimeException exception) {
                this.quarantineServerHandler(player, "tick", exception);
            }
        }

        private void dispatchClosed(ServerPlayer player, PlayerSessions sessions) {
            if (this.serverHandler == null) {
                return;
            }
            DesktopServerWindowHandler<AbstractContainerMenu, Object> handler = this.serverHandler;
            try {
                sessions.invokeServerHandlerCallback(
                    player,
                    () -> withCanonicalCarried(player, sessions, this.menu, () -> {
                        handler.closed(new ServerSessionContext(player, sessions, this));
                        return null;
                    })
                );
            } catch (RuntimeException exception) {
                this.quarantineServerHandler(player, "closed", exception);
            }
        }

        private void dispatchVisibilityChanged(ServerPlayer player, PlayerSessions sessions) {
            if (this.serverHandler == null) {
                return;
            }
            DesktopServerWindowHandler<AbstractContainerMenu, Object> handler = this.serverHandler;
            try {
                sessions.invokeServerHandlerCallback(
                    player,
                    () -> withCanonicalCarried(player, sessions, this.menu, () -> {
                        handler.visibilityChanged(new ServerSessionContext(player, sessions, this), this.visibleToClient);
                        return null;
                    })
                );
            } catch (RuntimeException exception) {
                this.quarantineServerHandler(player, "visibility", exception);
            }
        }

        private void dispatchPinChanged(ServerPlayer player, PlayerSessions sessions) {
            if (this.serverHandler == null) {
                return;
            }
            DesktopServerWindowHandler<AbstractContainerMenu, Object> handler = this.serverHandler;
            try {
                sessions.invokeServerHandlerCallback(
                    player,
                    () -> withCanonicalCarried(player, sessions, this.menu, () -> {
                        handler.pinChanged(new ServerSessionContext(player, sessions, this), this.ghostPinned);
                        return null;
                    })
                );
            } catch (RuntimeException exception) {
                this.quarantineServerHandler(player, "pin", exception);
            }
        }

        private void quarantineServerHandler(ServerPlayer player, String callback, RuntimeException exception) {
            DesktopDebug.warn(
                "server window handler quarantined player={} session={} title={} callback={} reason={}",
                player.getName().getString(),
                this.sessionId,
                this.title.getString(),
                callback,
                exception.toString()
            );
            this.serverHandler = null;
            this.serverState = null;
        }
    }

    private record SlotSource(
        int sessionId,
        AbstractContainerMenu menu,
        net.minecraft.world.inventory.Slot slot,
        @Nullable Session session,
        Object physicalOwner,
        int physicalIndex
    ) {
        private SlotSource(
            int sessionId,
            AbstractContainerMenu menu,
            net.minecraft.world.inventory.Slot slot,
            @Nullable Session session
        ) {
            this(
                sessionId,
                menu,
                slot,
                session,
                DesktopMenuSlots.physicalOwner(menu, slot),
                DesktopMenuSlots.physicalIndex(menu, slot)
            );
        }
    }

    private record NetworkSession(ServerPlayer player, Session session) {
    }

    private record SessionTransition(ServerPlayer player, Session oldSession) {
    }

    private record JeiTransferTarget(int sessionId, AbstractContainerMenu menu, @Nullable Session session) {
    }

    private record ValidatedJeiTransfer(List<Slot> recipeSlots, List<JeiTransferRequirement> requirements, int maximumCrafts) {
        private ValidatedJeiTransfer {
            recipeSlots = List.copyOf(recipeSlots);
            requirements = List.copyOf(requirements);
        }
    }

    private record JeiTransferRequirement(int inputIndex, int targetMenuSlotId, Slot targetSlot, int unitsPerCraft, List<ItemStack> alternatives) {
    }

    private record JeiTransferSimulation(Map<Slot, ItemStack> sourceStacks, Map<Slot, ItemStack> targetStacks) {
    }

    private record JeiTransferSourceKey(Container container, int containerSlot) {
        @Override
        public boolean equals(Object other) {
            return other instanceof JeiTransferSourceKey key
                && this.container == key.container
                && this.containerSlot == key.containerSlot;
        }

        @Override
        public int hashCode() {
            return 31 * System.identityHashCode(this.container) + this.containerSlot;
        }
    }

    private record TransferStackKey(Item item, @Nullable CompoundTag tag) {
        private static final Comparator<TransferStackKey> COMPARATOR = Comparator
            .comparingInt((TransferStackKey key) -> BuiltInRegistries.ITEM.getId(key.item()))
            // NBT equality/hashCode is authoritative. Text is only a stable ordering tie-breaker.
            .thenComparing(key -> key.tag() == null ? "" : key.tag().toString());

        private static TransferStackKey of(ItemStack stack) {
            return new TransferStackKey(
                stack.getItem(),
                stack.hasTag() ? stack.getTag().copy() : null
            );
        }
    }

    private record DormantGhostSource(String sourceKey, long expiresAtTick) {
    }

    private static final class MutationRegistration<P extends DesktopPacket> {
        private final Function<FriendlyByteBuf, P> decoder;
        private final long operationCapabilities;
        private final @Nullable ToIntFunction<P> primarySession;
        private final @Nullable ToIntFunction<P> secondarySession;
        private final BiConsumer<ServerPlayer, P> handler;

        private MutationRegistration(
            Function<FriendlyByteBuf, P> decoder,
            long operationCapabilities,
            @Nullable ToIntFunction<P> primarySession,
            @Nullable ToIntFunction<P> secondarySession,
            BiConsumer<ServerPlayer, P> handler
        ) {
            this.decoder = decoder;
            this.operationCapabilities = operationCapabilities;
            this.primarySession = primarySession;
            this.secondarySession = secondarySession;
            this.handler = handler;
        }

        private void dispatch(ServerPlayer player, PlayerSessions sessions, DesktopAuthenticatedPayload envelope) {
            byte[] data = envelope.data();
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.wrappedBuffer(data));
            try {
                P payload = this.decoder.apply(buf);
                if (buf.isReadable()) {
                    throw new IllegalArgumentException("Trailing mutation bytes: " + buf.readableBytes());
                }
                boolean hasPrimary = this.primarySession != null;
                boolean hasSecondary = this.secondarySession != null;
                int primary = hasPrimary ? this.primarySession.applyAsInt(payload) : NO_SESSION;
                int secondary = hasSecondary ? this.secondarySession.applyAsInt(payload) : NO_SESSION;
                long requiredCapabilities = this.operationCapabilities;
                if (hasPrimary) {
                    requiredCapabilities |= capabilityForSession(primary);
                }
                if (hasSecondary) {
                    requiredCapabilities |= capabilityForSession(secondary);
                }
                if (!sessions.connectionState.authorizes(envelope.connectionNonce(), requiredCapabilities, true)) {
                    DesktopDebug.warn("server mutation rejected player={} type={} reason=capability", player.getName().getString(), envelope.innerType());
                    return;
                }
                boolean primaryAuthorized = !hasPrimary || sessions.authorizesSession(primary, envelope.primarySessionToken());
                boolean secondaryAuthorized = !hasSecondary || sessions.authorizesSession(secondary, envelope.secondarySessionToken());
                if (!primaryAuthorized || !secondaryAuthorized) {
                    DesktopDebug.warn("server mutation rejected player={} type={} reason=session-token", player.getName().getString(), envelope.innerType());
                    return;
                }
                boolean primaryCurrent = !hasPrimary || sessions.sessionStateMatches(player, primary, envelope.primaryStateId());
                boolean secondaryCurrent = !hasSecondary || sessions.sessionStateMatches(player, secondary, envelope.secondaryStateId());
                if (!primaryCurrent || !secondaryCurrent) {
                    DesktopDebug.trace("server mutation rejected player={} type={} reason=stale-state", player.getName().getString(), envelope.innerType());
                    if (sessions.resyncRate.tryConsume(System.nanoTime())) {
                        if (!primaryCurrent && hasPrimary) {
                            sessions.resyncSession(player, primary);
                        }
                        if (!secondaryCurrent && hasSecondary && secondary != primary) {
                            sessions.resyncSession(player, secondary);
                        }
                    }
                    return;
                }
                this.handler.accept(player, payload);
            } catch (RuntimeException | StackOverflowError exception) {
                DesktopDebug.warn("server mutation rejected player={} type={} reason={}", player.getName().getString(), envelope.innerType(), exception.toString());
            } finally {
                buf.release();
            }
        }
    }

    private record SourceGrant(String sourceKey, List<SourceBackingIdentity> backing) {
        private static @Nullable SourceGrant capture(ServerPlayer player, String sourceKey) {
            SourceKey parsed = SourceKey.parse(sourceKey);
            if (parsed == null || !parsed.dimension().equals(player.level().dimension().location().toString())) {
                return null;
            }
            ServerLevel level = player.serverLevel();
            List<SourceBackingIdentity> backing = new ArrayList<>(parsed.positions().size());
            for (BlockPos pos : parsed.positions()) {
                if (!level.isInWorldBounds(pos) || !level.hasChunkAt(pos)) {
                    return null;
                }
                BlockState state = level.getBlockState(pos);
                BlockEntity blockEntity = level.getBlockEntity(pos);
                backing.add(new SourceBackingIdentity(
                    BuiltInRegistries.BLOCK.getKey(state.getBlock()),
                    blockEntity == null ? null : BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(blockEntity.getType()),
                    blockEntity == null ? null : new WeakReference<>(blockEntity)
                ));
            }
            return new SourceGrant(sourceKey, List.copyOf(backing));
        }

        private boolean matches(ServerPlayer player) {
            SourceKey parsed = SourceKey.parse(this.sourceKey);
            if (parsed == null
                || !parsed.dimension().equals(player.level().dimension().location().toString())
                || parsed.positions().size() != this.backing.size()) {
                return false;
            }
            ServerLevel level = player.serverLevel();
            for (int index = 0; index < parsed.positions().size(); index++) {
                BlockPos pos = parsed.positions().get(index);
                if (!level.isInWorldBounds(pos) || !level.hasChunkAt(pos)) {
                    return false;
                }
                SourceBackingIdentity expected = this.backing.get(index);
                BlockEntity blockEntity = level.getBlockEntity(pos);
                ResourceLocation blockEntityTypeId = blockEntity == null
                    ? null
                    : BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(blockEntity.getType());
                if (!expected.blockId().equals(BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock()))
                    || (expected.blockEntityTypeId() == null
                        ? blockEntityTypeId != null
                        : !expected.blockEntityTypeId().equals(blockEntityTypeId))
                    || (expected.blockEntity() == null
                        ? blockEntity != null
                        : expected.blockEntity().get() != blockEntity)) {
                    return false;
                }
            }
            return true;
        }
    }

    private record SourceBackingIdentity(
        ResourceLocation blockId,
        @Nullable ResourceLocation blockEntityTypeId,
        @Nullable WeakReference<BlockEntity> blockEntity
    ) {
    }

    private record ServerSessionContext(
        ServerPlayer player,
        PlayerSessions sessions,
        Session session
    ) implements DesktopServerSessionContext<AbstractContainerMenu, Object> {
        @Override
        public AbstractContainerMenu menu() {
            return this.session.menu;
        }

        @Override
        public int sessionId() {
            return this.session.sessionId;
        }

        @Override
        public String sourceKey() {
            return this.session.sourceKey;
        }

        @Override
        public boolean visible() {
            return this.session.visibleToClient;
        }

        @Override
        public boolean ghostPinned() {
            return this.session.ghostPinned;
        }

        @Override
        public Object state() {
            return this.session.serverState;
        }

        @Override
        public void sendToClient(ResourceLocation channel, byte[] data) {
            send(this.player, new DesktopCustomPayload(this.session.sessionId, channel, data));
        }

        @Override
        public void broadcastChanges() {
            this.sessions.requestBroadcastChanges(this.player);
        }
    }

    private record ServerPayloadContext(
        ServerPlayer player,
        PlayerSessions sessions,
        Session session,
        DesktopCustomPayload payload
    ) implements DesktopServerPayloadContext<AbstractContainerMenu> {
        @Override
        public AbstractContainerMenu menu() {
            return this.session.menu;
        }

        @Override
        public int sessionId() {
            return this.session.sessionId;
        }

        @Override
        public String sourceKey() {
            return this.session.sourceKey;
        }

        @Override
        public boolean visible() {
            return this.session.visibleToClient;
        }

        @Override
        public boolean ghostPinned() {
            return this.session.ghostPinned;
        }

        @Override
        public Object state() {
            return this.session.serverState;
        }

        @Override
        public ResourceLocation channel() {
            return this.payload.channel();
        }

        @Override
        public byte[] data() {
            return this.payload.data();
        }

        @Override
        public void sendToClient(ResourceLocation channel, byte[] data) {
            send(this.player, new DesktopCustomPayload(this.session.sessionId, channel, data));
        }

        @Override
        public void broadcastChanges() {
            this.sessions.requestBroadcastChanges(this.player);
        }
    }

    private static final class SessionSynchronizer implements ContainerSynchronizer, DesktopSessionSynchronizer {
        private final ServerPlayer player;
        private final Session session;

        private SessionSynchronizer(ServerPlayer player, Session session) {
            this.player = player;
            this.session = session;
        }

        @Override
        public void sendInitialData(AbstractContainerMenu menu, NonNullList<ItemStack> stacks, ItemStack carried, int[] dataSlots) {
            DesktopDebug.log("server send initial player={} session={} title={} slots={} data={}", this.player.getName().getString(), this.session.sessionId, this.session.title.getString(), stacks.size(), dataSlots.length);
            ItemStack canonicalCarried = this.canonicalCarried(menu);
            PlayerSessions sessions = existingSessions(this.player);
            if (!this.session.initialSnapshotSent && sessions != null && this.session.openingData.length > 0) {
                send(this.player, new DesktopMenuOpenDataPayload(
                    sessions.connectionState.connectionNonce(),
                    this.session.sessionId,
                    this.session.sessionToken,
                    this.session.menuTypeId,
                    this.session.replacesSessionId,
                    this.session.openingData
                ));
            }
            send(this.player, new DesktopOpenSessionPayload(
                this.session.sessionId,
                this.session.menuTypeId,
                this.session.specialKind,
                this.session.entityId,
                this.session.columns,
                menu.getStateId(),
                this.session.visibleToClient,
                this.session.transferSupported(),
                this.session.sourceKey,
                this.session.title,
                stacks,
                canonicalCarried,
                dataSlots
            ));
            this.session.initialSnapshotSent = true;
            send(this.player, new DesktopCarriedPayload(canonicalCarried));
            syncMerchantOffers(this.player, this.session);
        }

        @Override
        public void sendSlotChange(AbstractContainerMenu menu, int slot, ItemStack stack) {
            DesktopDebug.trace("server send slot player={} session={} slot={} stack={}", this.player.getName().getString(), this.session.sessionId, slot, stack);
            send(this.player, new DesktopSlotPayload(this.session.sessionId, slot, menu.getStateId(), stack.copy()));
        }

        @Override
        public void sendCarriedChange(AbstractContainerMenu menu, ItemStack stack) {
            ItemStack canonicalCarried = this.canonicalCarried(menu);
            DesktopDebug.trace("server send carried player={} session={} stack={}", this.player.getName().getString(), this.session.sessionId, canonicalCarried);
            send(this.player, new DesktopCarriedPayload(canonicalCarried));
        }

        private ItemStack canonicalCarried(AbstractContainerMenu menu) {
            ItemStack canonical = this.player.inventoryMenu.getCarried().copy();
            if (!ItemStack.matches(canonical, menu.getCarried())) {
                menu.setCarried(canonical.copy());
            }
            return canonical;
        }

        @Override
        public void sendDataChange(AbstractContainerMenu menu, int dataSlotIndex, int value) {
            DesktopDebug.trace("server send data player={} session={} data={} value={}", this.player.getName().getString(), this.session.sessionId, dataSlotIndex, value);
            send(this.player, new DesktopDataPayload(this.session.sessionId, dataSlotIndex, value));
        }
    }

    private static @Nullable String sourceKeyForProvider(ServerPlayer player, MenuProvider provider) {
        if (provider instanceof BlockEntity blockEntity) {
            return sourceKeyForBlock(player, blockEntity.getBlockPos());
        }

        if (provider instanceof net.minecraft.world.entity.Entity entity) {
            return sourceKeyForEntity(player, entity.getUUID());
        }

        return null;
    }

    private static boolean isBlockBackedSourceKey(String sourceKey) {
        return sourceKey.startsWith("block:") || sourceKey.startsWith("chest:");
    }

    private static @Nullable MenuProvider providerForDormantGhost(ServerPlayer player, String sourceKey) {
        SourceKey source = SourceKey.parse(sourceKey);
        if (source == null || !source.dimension().equals(player.level().dimension().location().toString())) {
            return null;
        }

        ServerLevel level = player.serverLevel();
        for (BlockPos pos : source.positions()) {
            if (!level.isInWorldBounds(pos) || !level.hasChunkAt(pos) || !level.mayInteract(player, pos)) {
                return null;
            }
        }
        for (BlockPos pos : source.positions()) {
            if (!canReachDormantSource(player, level, pos)) {
                continue;
            }

            if (!sourceKey.equals(sourceKeyForBlock(player, pos))) {
                continue;
            }

            MenuProvider provider = level.getBlockState(pos).getMenuProvider(level, pos);
            if (provider != null) {
                return provider;
            }
        }

        return null;
    }

    private static boolean canReachDormantSource(ServerPlayer player, ServerLevel level, BlockPos pos) {
        if (!level.isInWorldBounds(pos) || !level.hasChunkAt(pos) || !level.mayInteract(player, pos)) {
            return false;
        }
        Vec3 target = Vec3.atCenterOf(pos);
        double range = blockInteractionReach(player);
        if (!Double.isFinite(range) || range < 0.0D) {
            return false;
        }
        Vec3 eye = player.getEyePosition();
        if (eye.distanceToSqr(target) > range * range) {
            return false;
        }

        BlockHitResult hit = level.clip(new ClipContext(
            eye,
            target,
            ClipContext.Block.OUTLINE,
            ClipContext.Fluid.NONE,
            player
        ));
        return hit.getType() == HitResult.Type.BLOCK && hit.getBlockPos().equals(pos);
    }

    private static double blockInteractionReach(ServerPlayer player) {
        net.minecraft.world.entity.ai.attributes.Attribute blockReach = BuiltInRegistries.ATTRIBUTE.getOptional(
            new ResourceLocation("forge", "block_reach")
        ).orElse(null);
        if (blockReach != null) {
            net.minecraft.world.entity.ai.attributes.AttributeInstance instance = player.getAttribute(blockReach);
            if (instance != null) {
                return instance.getValue();
            }
        }
        // 1.20.1 has no vanilla reach attribute; these are the game-mode reach values.
        return player.isCreative() ? 5.0D : 4.5D;
    }

    private record SourceKey(String kind, String dimension, List<BlockPos> positions) {
        private static @Nullable SourceKey parse(String sourceKey) {
            int firstColon = sourceKey.indexOf(':');
            int lastColon = sourceKey.lastIndexOf(':');
            if (firstColon <= 0 || lastColon <= firstColon) {
                return null;
            }

            String kind = sourceKey.substring(0, firstColon);
            if (!kind.equals("block") && !kind.equals("chest")) {
                return null;
            }

            String dimension = sourceKey.substring(firstColon + 1, lastColon);
            String positionsPart = sourceKey.substring(lastColon + 1);
            List<BlockPos> positions = new ArrayList<>();
            for (String positionPart : positionsPart.split("\\|")) {
                BlockPos pos = parseBlockPos(positionPart);
                if (pos == null) {
                    return null;
                }
                positions.add(pos);
            }
            int expectedPositions = kind.equals("chest") ? 2 : 1;
            return positions.size() == expectedPositions ? new SourceKey(kind, dimension, List.copyOf(positions)) : null;
        }
    }

    private static @Nullable BlockPos parseBlockPos(String value) {
        String[] parts = value.split(",");
        if (parts.length != 3) {
            return null;
        }

        try {
            return new BlockPos(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private static String sourceKeyForEntity(ServerPlayer player, UUID entityUuid) {
        return "entity:" + player.level().dimension().location() + ":" + entityUuid;
    }

    private static String sourceKeyForBlock(ServerPlayer player, BlockPos pos) {
        String dimension = player.level().dimension().location().toString();
        BlockState state = player.level().getBlockState(pos);
        if (state.getBlock() instanceof ChestBlock && state.hasProperty(ChestBlock.TYPE)) {
            ChestType chestType = state.getValue(ChestBlock.TYPE);
            if (chestType != ChestType.SINGLE) {
                BlockPos connectedPos = pos.relative(ChestBlock.getConnectedDirection(state));
                String first = blockPosKey(pos);
                String second = blockPosKey(connectedPos);
                if (first.compareTo(second) > 0) {
                    String swap = first;
                    first = second;
                    second = swap;
                }
                return "chest:" + dimension + ":" + first + "|" + second;
            }
        }

        return "block:" + dimension + ":" + blockPosKey(pos);
    }

    private static String blockPosKey(BlockPos pos) {
        return pos.getX() + "," + pos.getY() + "," + pos.getZ();
    }
}
