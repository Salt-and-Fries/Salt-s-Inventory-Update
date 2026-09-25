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
import java.security.SecureRandom;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

import io.netty.buffer.Unpooled;

import com.salts_inventory_update.platform.fabric.api.event.lifecycle.v1.ServerTickEvents;
import com.salts_inventory_update.platform.fabric.api.networking.v1.ServerPlayConnectionEvents;
import com.salts_inventory_update.platform.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.HashedStack;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.CompoundContainer;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.animal.camel.Camel;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.animal.equine.Llama;
import net.minecraft.world.entity.animal.nautilus.AbstractNautilus;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.AbstractCraftingMenu;
import net.minecraft.world.inventory.AbstractFurnaceMenu;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.BeaconMenu;
import net.minecraft.world.inventory.CartographyTableMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.ContainerSynchronizer;
import net.minecraft.world.inventory.CrafterMenu;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.inventory.HorseInventoryMenu;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.NautilusInventoryMenu;
import net.minecraft.world.inventory.RemoteSlot;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.StonecutterMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.CraftingRecipe;
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

import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

import com.salts_inventory_update.api.server.desktop.DesktopServerApi;
import com.salts_inventory_update.api.server.desktop.DesktopServerPayloadContext;
import com.salts_inventory_update.api.server.desktop.DesktopServerPayloadHandler;
import com.salts_inventory_update.api.server.desktop.DesktopServerSessionContext;
import com.salts_inventory_update.api.server.desktop.DesktopServerWindowHandler;
import com.salts_inventory_update.api.server.desktop.DesktopTransferDecision;
import com.salts_inventory_update.api.server.desktop.DesktopTransferRequest;
import com.salts_inventory_update.api.server.desktop.DesktopTransferValidators;
import com.salts_inventory_update.compat.toms_storage.TomsStorageCompat;
import com.salts_inventory_update.debug.DesktopDebug;
import com.salts_inventory_update.inventory.InventoryExpansion;
import com.salts_inventory_update.internal.desktop.DesktopItemSourceLocks;
import com.salts_inventory_update.internal.desktop.DesktopMenuSlots;
import com.salts_inventory_update.internal.desktop.DesktopSortSlots;
import com.salts_inventory_update.internal.desktop.DesktopSessionSynchronizer;
import com.salts_inventory_update.internal.desktop.DesktopSynchronizerOverride;
import com.salts_inventory_update.network.DesktopMenuOpenDataPayload;
import com.salts_inventory_update.network.DesktopPackets;
import com.salts_inventory_update.network.DesktopPackets.DesktopButtonPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopBundleSelectPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopCarriedPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopClickPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopCloseSessionPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopCustomPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopDataPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopMerchantOffersPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopOpenLinkedSourcesPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopOpenSessionPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopGhostRecipePayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopJeiTransferPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopPlaceRecipePayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopDragSlotsPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopPickupAllPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopQuickMovePayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopQuickMoveAllPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopSortWindowsPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopSessionReference;
import com.salts_inventory_update.network.DesktopPackets.DesktopSlotReference;
import com.salts_inventory_update.network.DesktopPackets.DesktopHelloPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopHelloAckPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopReadyPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopModePayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopMutationAckPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopPlayerSessionPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopLinkPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopRenamePayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopSessionClosedPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopSessionPinPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopSessionVisibilityPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopSlotPayload;
import com.salts_inventory_update.network.DesktopPackets.InventorySlotPurchasePayload;
import com.salts_inventory_update.protocol.DesktopProtocol;
import com.salts_inventory_update.protocol.BoundedLinkGraph;
import com.salts_inventory_update.protocol.BoundedTransferPlanner;
import com.salts_inventory_update.protocol.TokenBucket;
import net.minecraft.world.item.crafting.Recipe;

public final class DesktopContainerSessions {
    private static final int MAX_SESSIONS = DesktopProtocol.MAX_DESKTOP_SESSIONS;
    private static final long SUPPORTED_CAPABILITIES = DesktopProtocol.KNOWN_CAPABILITIES
        | DesktopPackets.CAP_MULTI_MENU_GESTURES | DesktopPackets.CAP_SORT_WINDOWS;
    private static final int MAX_DORMANT_GHOST_SOURCES = DesktopProtocol.MAX_DORMANT_SOURCES;
    private static final int DORMANT_GHOST_REOPEN_INTERVAL_TICKS = 10;
    private static final int CRAFTER_INPUT_SLOT_COUNT = 9;
    private static final int CRAFTER_SLOT_STATE_ENABLED_FLAG = 16;
    private static final int MERCHANT_RESULT_SLOT = 2;
    private static final int BEACON_EFFECT_ID_MASK = 0xFFFF;
    private static final int BEACON_SECONDARY_EFFECT_SHIFT = 16;
    private static final int MAX_PICKUP_ALL_LOGICAL_SLOTS = 65_536;
    private static final int PICKUP_ALL_SLOTS_PER_RATE_TOKEN = 1_024;
    private static final int MAX_QUICK_MOVE_TRANSACTION_WORK = 1_000_000;
    private static final Map<ServerPlayer, PlayerSessions> PLAYERS = new IdentityHashMap<>();
    private static final Map<ServerPlayer, String> PENDING_USE_TARGETS = new IdentityHashMap<>();
    private static final Set<ServerPlayer> PROTOCOL_REJECTED = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String PLAYER_LINK_NODE = "player";
    private static final ThreadLocal<SessionTransition> ACTIVE_TRANSITION = new ThreadLocal<>();
    private static final ThreadLocal<NetworkSession> ACTIVE_NETWORK_SESSION = new ThreadLocal<>();

    private DesktopContainerSessions() {
    }

    public static void initialize() {
        DesktopDebug.log("server desktop session networking initialized");
        ServerPlayNetworking.registerGlobalReceiver(DesktopHelloPayload.TYPE, (payload, context) ->
            context.server().execute(() -> hello(context.player(), payload))
        );
        ServerPlayNetworking.registerGlobalReceiver(DesktopReadyPayload.TYPE, (payload, context) ->
            context.server().execute(() -> rejectLegacyReady(context.player()))
        );
        ServerPlayNetworking.registerGlobalReceiver(DesktopModePayload.TYPE, (payload, context) ->
            context.server().execute(() -> setMode(context.player(), payload))
        );
        ServerPlayNetworking.registerGlobalReceiver(DesktopClickPayload.TYPE, (payload, context) ->
            context.server().execute(() -> handleMutation(
                context.player(), payload.connectionNonce(), payload.playerSessionToken(), payload.mutationId(), "click",
                () -> click(context.player(), payload)
            ))
        );
        ServerPlayNetworking.registerGlobalReceiver(DesktopDragSlotsPayload.TYPE, (payload, context) ->
            context.server().execute(() -> handleMutation(
                context.player(), payload.connectionNonce(), payload.playerSessionToken(), payload.mutationId(), "drag-slots",
                proportionalBatchCost(payload.slots().size(), 8),
                () -> dragSlots(context.player(), payload)
            ))
        );
        ServerPlayNetworking.registerGlobalReceiver(DesktopPickupAllPayload.TYPE, (payload, context) ->
            context.server().execute(() -> handleMutation(
                context.player(), payload.connectionNonce(), payload.playerSessionToken(), payload.mutationId(), "pickup-all",
                proportionalBatchCost(payload.sources().size(), 2),
                () -> pickupAll(context.player(), payload)
            ))
        );
        ServerPlayNetworking.registerGlobalReceiver(DesktopSortWindowsPayload.TYPE, (payload, context) ->
            context.server().execute(() -> handleMutation(
                context.player(), payload.connectionNonce(), payload.playerSessionToken(), payload.mutationId(), "sort-windows",
                proportionalBatchCost(payload.destinations().size(), 8),
                () -> sortWindows(context.player(), payload)
            ))
        );
        ServerPlayNetworking.registerGlobalReceiver(DesktopQuickMoveAllPayload.TYPE, (payload, context) ->
            context.server().execute(() -> handleMutation(
                context.player(), payload.connectionNonce(), payload.playerSessionToken(), payload.mutationId(), "quick-move-all",
                proportionalBatchCost(payload.sources().size(), 8),
                () -> quickMoveAll(context.player(), payload)
            ))
        );
        ServerPlayNetworking.registerGlobalReceiver(DesktopBundleSelectPayload.TYPE, (payload, context) ->
            context.server().execute(() -> handleMutation(
                context.player(), payload.connectionNonce(), payload.playerSessionToken(), payload.mutationId(), "bundle-select",
                () -> selectBundleItem(context.player(), payload)
            ))
        );
        ServerPlayNetworking.registerGlobalReceiver(DesktopQuickMovePayload.TYPE, (payload, context) ->
            context.server().execute(() -> handleMutation(
                context.player(), payload.connectionNonce(), payload.playerSessionToken(), payload.mutationId(), "quick-move",
                () -> quickMove(context.player(), payload)
            ))
        );
        ServerPlayNetworking.registerGlobalReceiver(DesktopButtonPayload.TYPE, (payload, context) ->
            context.server().execute(() -> handleMutation(
                context.player(), payload.connectionNonce(), payload.playerSessionToken(), payload.mutationId(), "button",
                () -> button(context.player(), payload)
            ))
        );
        ServerPlayNetworking.registerGlobalReceiver(DesktopPlaceRecipePayload.TYPE, (payload, context) ->
            context.server().execute(() -> handleMutation(
                context.player(), payload.connectionNonce(), payload.playerSessionToken(), payload.mutationId(), "recipe-place",
                () -> placeRecipe(context.player(), payload)
            ))
        );
        ServerPlayNetworking.registerGlobalReceiver(DesktopJeiTransferPayload.TYPE, (payload, context) ->
            context.server().execute(() -> handleMutation(
                context.player(), payload.connectionNonce(), payload.playerSessionToken(), payload.mutationId(), "jei-transfer",
                () -> transferJeiRecipe(context.player(), payload)
            ))
        );
        ServerPlayNetworking.registerGlobalReceiver(DesktopRenamePayload.TYPE, (payload, context) ->
            context.server().execute(() -> handleMutation(
                context.player(), payload.connectionNonce(), payload.playerSessionToken(), payload.mutationId(), "rename",
                () -> rename(context.player(), payload)
            ))
        );
        ServerPlayNetworking.registerGlobalReceiver(DesktopCloseSessionPayload.TYPE, (payload, context) ->
            context.server().execute(() -> closeSession(context.player(), payload))
        );
        ServerPlayNetworking.registerGlobalReceiver(DesktopSessionPinPayload.TYPE, (payload, context) ->
            context.server().execute(() -> setSessionPin(context.player(), payload))
        );
        ServerPlayNetworking.registerGlobalReceiver(DesktopSessionVisibilityPayload.TYPE, (payload, context) ->
            context.server().execute(() -> setSessionVisibility(context.player(), payload))
        );
        ServerPlayNetworking.registerGlobalReceiver(DesktopOpenLinkedSourcesPayload.TYPE, (payload, context) ->
            context.server().execute(() -> openLinkedSources(context.player(), payload))
        );
        ServerPlayNetworking.registerGlobalReceiver(DesktopLinkPayload.TYPE, (payload, context) ->
            context.server().execute(() -> updateLink(context.player(), payload))
        );
        ServerPlayNetworking.registerGlobalReceiver(DesktopCustomPayload.TYPE, (payload, context) ->
            context.server().execute(() -> handleMutation(
                context.player(), payload.connectionNonce(), payload.playerSessionToken(), payload.mutationId(), "custom",
                () -> customPayload(context.player(), payload)
            ))
        );
        ServerPlayNetworking.registerGlobalReceiver(DesktopCarriedPayload.TYPE, (payload, context) ->
            context.server().execute(() -> handleMutation(
                context.player(), payload.connectionNonce(), payload.playerSessionToken(), payload.mutationId(), "carried",
                () -> carried(context.player(), payload)
            ))
        );
        ServerPlayNetworking.registerGlobalReceiver(InventorySlotPurchasePayload.TYPE, (payload, context) ->
            context.server().execute(() -> handleMutation(
                context.player(), payload.connectionNonce(), payload.playerSessionToken(), payload.mutationId(), "inventory-slot-purchase",
                () -> purchaseInventorySlot(context.player(), payload)
            ))
        );
        ServerTickEvents.END_SERVER_TICK.register(DesktopContainerSessions::tick);
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> disconnect(handler.player));
    }

    public static boolean shouldCapture(ServerPlayer player) {
        PlayerSessions sessions = PLAYERS.get(player);
        return sessions != null && sessions.isActive() && sessions.hasCapability(DesktopProtocol.CAP_CUSTOM_WINDOWS);
    }

    public static boolean isGameplayActive(ServerPlayer player) {
        PlayerSessions sessions = PLAYERS.get(player);
        return sessions != null
            && sessions.isGameplayActive()
            && sessions.hasCapability(DesktopProtocol.CAP_INVENTORY_TOPOLOGY | DesktopPackets.CAP_MULTI_MENU_GESTURES);
    }

    public static boolean isTopologyNegotiated(ServerPlayer player) {
        PlayerSessions sessions = PLAYERS.get(player);
        return sessions != null && sessions.negotiated && sessions.hasCapability(DesktopProtocol.CAP_INVENTORY_TOPOLOGY);
    }

    public static void transferPlayerState(ServerPlayer target, ServerPlayer source) {
        if (target == source) {
            return;
        }
        PlayerSessions previous = PLAYERS.get(source);
        PENDING_USE_TARGETS.remove(source);
        if (previous == null) {
            return;
        }
        previous.closeAll(source, false);
        PLAYERS.remove(source);
        PlayerSessions replacement = new PlayerSessions();
        replacement.negotiated = previous.negotiated;
        replacement.uiEnabled = previous.uiEnabled;
        replacement.gameplayEnabled = previous.gameplayEnabled;
        replacement.capabilities = previous.capabilities;
        replacement.connectionNonce = previous.connectionNonce;
        replacement.playerSessionToken = nextToken();
        // The rotated player token is a new mutation epoch; the client resets to id 1 with it.
        replacement.lastMutationId = 0L;
        replacement.lastModeSequence = previous.lastModeSequence;
        replacement.forcedMenuIds = previous.forcedMenuIds;
        PLAYERS.put(target, replacement);
        send(target, new DesktopPlayerSessionPayload(replacement.connectionNonce, replacement.playerSessionToken));
    }

    public static void preparePlayerStateTransfer(ServerPlayer source) {
        PlayerSessions sessions = PLAYERS.get(source);
        if (sessions != null) {
            sessions.closeAll(source, false);
        }
    }

    public static void captureUseTarget(ServerPlayer player, BlockHitResult hitResult) {
        if (!shouldCapture(player)) {
            return;
        }

        if (hitResult.getType() != HitResult.Type.BLOCK) {
            return;
        }

        String sourceKey = sourceKeyForBlock(player, hitResult.getBlockPos());
        PENDING_USE_TARGETS.put(player, sourceKey);
        DesktopDebug.trace("server use target player={} key={}", player.getName().getString(), sourceKey);
    }

    public static void clearUseTarget(ServerPlayer player) {
        PENDING_USE_TARGETS.remove(player);
    }

    public static boolean hasOpenSessionForContainer(Player player, Container container) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return false;
        }

        PlayerSessions sessions = PLAYERS.get(serverPlayer);
        if (sessions == null || !sessions.isActive() || !sessions.hasCapability(DesktopProtocol.CAP_CUSTOM_WINDOWS)) {
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

    /**
     * Internal compatibility hook for container opener counters that identify an owner from the
     * player's active menu. Salt menus are detached, so optional integrations can use the mod's
     * own ownership predicate against every live desktop menu without exposing session state.
     */
    public static boolean hasOpenSessionMatching(Player player, Predicate<AbstractContainerMenu> menuPredicate) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return false;
        }

        PlayerSessions sessions = PLAYERS.get(serverPlayer);
        if (sessions == null || !sessions.isActive() || !sessions.hasCapability(DesktopProtocol.CAP_CUSTOM_WINDOWS)) {
            return false;
        }

        for (Session session : sessions.sessions.values()) {
            if (menuPredicate.test(session.menu)) {
                return true;
            }
        }

        return false;
    }

    public static @Nullable OptionalInt openMenuSession(ServerPlayer player, MenuProvider provider) {
        return openMenuSession(player, provider, null, true, false, true, null);
    }

    public static @Nullable OptionalInt openMenuSession(
        ServerPlayer player,
        MenuProvider provider,
        Consumer<RegistryFriendlyByteBuf> openingDataWriter
    ) {
        return openMenuSession(player, provider, null, true, false, true, openingDataWriter);
    }

    private static @Nullable OptionalInt openMenuSession(
        ServerPlayer player,
        MenuProvider provider,
        @Nullable String forcedSourceKey,
        boolean toggleExisting,
        boolean ghostPinned,
        boolean visibleToClient,
        @Nullable Consumer<RegistryFriendlyByteBuf> openingDataWriter
    ) {
        if (provider == null) {
            DesktopDebug.warn("server capture skipped player={} reason=null-provider", player.getName().getString());
            return OptionalInt.empty();
        }

        PlayerSessions sessions = PLAYERS.get(player);
        if (sessions == null || !sessions.isActive() || !sessions.hasCapability(DesktopProtocol.CAP_CUSTOM_WINDOWS)) {
            return null;
        }
        SessionTransition transition = ACTIVE_TRANSITION.get();
        if (transition != null && transition.player != player) {
            transition = null;
        }
        if (transition != null) {
            forcedSourceKey = transition.oldSession.sourceKey;
            toggleExisting = false;
            ghostPinned = transition.oldSession.ghostPinned;
            visibleToClient = transition.oldSession.visibleToClient;
        }

        String sourceKey = forcedSourceKey == null ? sourceKeyForProvider(player, provider) : forcedSourceKey;
        if (sourceKey == null) {
            sourceKey = PENDING_USE_TARGETS.get(player);
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

        if (!isDesktopSupportedMenu(sessions, menu)) {
            Identifier menuKey = BuiltInRegistries.MENU.getKey(menu.getType());
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

        byte[] openingData;
        try {
            openingData = encodeOpeningData(player, openingDataWriter);
        } catch (RuntimeException exception) {
            DesktopDebug.warn(
                "server capture skipped player={} title={} reason=invalid-opening-data detail={}",
                player.getName().getString(),
                provider.getDisplayName().getString(),
                exception.toString()
            );
            menu.removed(player);
            return null;
        }
        if (DesktopMenuSlots.requiresOpeningData(menu)) {
            if (openingData.length == 0 || !ServerPlayNetworking.canSend(player, DesktopMenuOpenDataPayload.TYPE)) {
                DesktopDebug.log(
                    "server capture skipped player={} title={} menu={} reason=opening-data-transport-unavailable",
                    player.getName().getString(),
                    provider.getDisplayName().getString(),
                    BuiltInRegistries.MENU.getKey(menu.getType())
                );
                menu.removed(player);
                return null;
            }
        }
        if (sourceKey == null) {
            sourceKey = DesktopMenuSlots.sourceKey(player, menu, openingData);
        }
        if (toggleExisting && sessions.closeBySourceKey(player, sourceKey, true)) {
            menu.setCarried(ItemStack.EMPTY);
            menu.removed(player);
            DesktopDebug.log("server toggle close player={} source={} title={}", player.getName().getString(), sourceKey, provider.getDisplayName().getString());
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
            transition == null ? -1 : transition.oldSession.sessionId
        );
        session.ghostPinned = ghostPinned;
        session.visibleToClient = visibleToClient;
        if (transition == null) {
            sessions.add(player, session);
        } else if (!sessions.replace(player, transition.oldSession, session)) {
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

    private static byte[] encodeOpeningData(
        ServerPlayer player,
        @Nullable Consumer<RegistryFriendlyByteBuf> openingDataWriter
    ) {
        if (openingDataWriter == null) {
            return new byte[0];
        }
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), player.registryAccess());
        try {
            openingDataWriter.accept(buffer);
            int length = buffer.readableBytes();
            if (length <= 0 || length > DesktopProtocol.MAX_CUSTOM_DATA_BYTES) {
                throw new IllegalArgumentException("Opening data length out of bounds: " + length);
            }
            byte[] result = new byte[length];
            buffer.getBytes(buffer.readerIndex(), result);
            return result;
        } finally {
            buffer.release();
        }
    }

    /** Runs a supported third-party navigation action against its detached Salt menu. */
    public static void withSessionTransition(ServerPlayer player, int sessionId, Runnable action) {
        PlayerSessions sessions = PLAYERS.get(player);
        Session oldSession = sessions == null ? null : sessions.sessions.get(sessionId);
        if (oldSession == null || !oldSession.menu.stillValid(player) || ACTIVE_TRANSITION.get() != null) {
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

    public static int activeNetworkSessionId(ServerPlayer player) {
        NetworkSession active = ACTIVE_NETWORK_SESSION.get();
        return active != null && active.player == player ? active.session.sessionId : -1;
    }

    public static boolean hasActiveSessionTransition(ServerPlayer player) {
        SessionTransition transition = ACTIVE_TRANSITION.get();
        return transition != null && transition.player == player;
    }

    public static String blockSourceKey(ServerPlayer player, BlockPos pos) {
        return sourceKeyForBlock(player, pos);
    }

    public static boolean sendSessionPayload(ServerPlayer player, int sessionId, Identifier channel, byte[] data) {
        PlayerSessions sessions = PLAYERS.get(player);
        Session session = sessions == null ? null : sessions.sessions.get(sessionId);
        if (session == null || data == null || data.length > DesktopProtocol.MAX_CUSTOM_DATA_BYTES) {
            return false;
        }
        send(player, new DesktopCustomPayload(
            sessions.connectionNonce,
            0L,
            sessions.playerSessionToken,
            session.sessionId,
            session.sessionToken,
            session.menu.getStateId(),
            channel,
            data
        ));
        return true;
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

    public static void openHorseSession(ServerPlayer player, AbstractHorse horse, Container container) {
        PlayerSessions sessions = PLAYERS.get(player);
        if (sessions == null || !sessions.isActive() || !sessions.hasCapability(DesktopProtocol.CAP_CUSTOM_WINDOWS)) {
            return;
        }
        String sourceKey = sourceKeyForEntity(player, horse);
        int columns = horse.getInventoryColumns();
        int specialKind = horseSpecialKind(horse);
        if (isCamelOrLlama(horse)) {
            mountDiag(
                "server_openHorse_start player={} entityId={} entityType={} entityClass={} special={} source={} columns={} containerClass={} containerSize={} sessions={} ready={}",
                player.getName().getString(),
                horse.getId(),
                BuiltInRegistries.ENTITY_TYPE.getKey(horse.getType()),
                horse.getClass().getName(),
                specialKind,
                sourceKey,
                columns,
                container.getClass().getName(),
                container.getContainerSize(),
                sessions.sessions.size(),
                sessions.isActive()
            );
        }
        if (sessions.closeBySourceKey(player, sourceKey, true)) {
            DesktopDebug.log("server toggle close horse player={} source={}", player.getName().getString(), sourceKey);
            if (isCamelOrLlama(horse)) {
                mountDiag("server_openHorse_toggled_closed player={} entityId={} source={}", player.getName().getString(), horse.getId(), sourceKey);
            }
            return;
        }

        int sessionId = nextSessionId(player);
        HorseInventoryMenu menu = new HorseInventoryMenu(sessionId, player.getInventory(), container, horse, columns);
        if (isCamelOrLlama(horse)) {
            mountDiag(
                "server_openHorse_menu_created player={} session={} entityId={} special={} menuClass={} menuSlots={} stillValid={}",
                player.getName().getString(),
                sessionId,
                horse.getId(),
                specialKind,
                menu.getClass().getName(),
                menu.slots.size(),
                menu.stillValid(player)
            );
        }
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
        if (isCamelOrLlama(horse)) {
            mountDiag("server_openHorse_session_added player={} session={} entityId={} special={} source={}", player.getName().getString(), sessionId, horse.getId(), specialKind, sourceKey);
        }
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

    private static boolean isCamelOrLlama(AbstractHorse horse) {
        return horse instanceof Camel || horse instanceof Llama;
    }

    private static boolean isCamelOrLlamaSpecial(int specialKind) {
        return specialKind == DesktopPackets.SPECIAL_CAMEL || specialKind == DesktopPackets.SPECIAL_LLAMA;
    }

    private static void mountDiag(String message, Object... args) {
        DesktopDebug.detail("SIU_MOUNT_DIAG " + message, args);
    }

    public static void openNautilusSession(ServerPlayer player, AbstractNautilus nautilus, Container container) {
        PlayerSessions sessions = PLAYERS.get(player);
        if (sessions == null || !sessions.isActive() || !sessions.hasCapability(DesktopProtocol.CAP_CUSTOM_WINDOWS)) {
            return;
        }
        String sourceKey = sourceKeyForEntity(player, nautilus);
        if (sessions.closeBySourceKey(player, sourceKey, true)) {
            DesktopDebug.log("server toggle close nautilus player={} source={}", player.getName().getString(), sourceKey);
            return;
        }

        int columns = nautilus.getInventoryColumns();
        int sessionId = nextSessionId(player);
        NautilusInventoryMenu menu = new NautilusInventoryMenu(sessionId, player.getInventory(), container, nautilus, columns);
        sessions.add(player, new Session(
            sessionId,
            menu,
            nautilus.getDisplayName(),
            DesktopPackets.SPECIAL_NAUTILUS,
            nautilus.getId(),
            columns,
            -1,
            sourceKey
        ));
        DesktopDebug.log("server capture nautilus player={} session={} entity={} columns={}", player.getName().getString(), sessionId, nautilus.getId(), columns);
    }

    private static boolean isDesktopSupportedMenu(PlayerSessions sessions, AbstractContainerMenu menu) {
        MenuType<?> type = menu.getType();
        Identifier key = BuiltInRegistries.MENU.getKey(type);
        if (key != null && sessions.forcedMenuIds.contains(key.toString())) {
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
            || type == MenuType.CRAFTER_3x3
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
        PlayerSessions sessions = PLAYERS.get(player);
        if (sessions == null
            || !sessions.isActive()
            || !sessions.hasCapability(DesktopProtocol.CAP_CUSTOM_WINDOWS)
            || !sessions.sessions.containsKey(containerId)) {
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

    private static void hello(ServerPlayer player, DesktopHelloPayload payload) {
        if (PROTOCOL_REJECTED.contains(player)) {
            return;
        }
        PlayerSessions sessions = sessions(player);
        if (sessions.negotiated) {
            PENDING_USE_TARGETS.remove(player);
            sessions.closeAll(player, false);
            sessions.uiEnabled = false;
            sessions.gameplayEnabled = false;
            sessions.negotiated = false;
            sessions.links.clear();
            PROTOCOL_REJECTED.add(player);
            player.connection.disconnect(Component.literal("Salt's Inventory Update received a repeated desktop handshake."));
            return;
        }
        PENDING_USE_TARGETS.remove(player);
        if (payload.protocolVersion() != DesktopProtocol.VERSION
            || payload.clientNonce() == 0L
            || (payload.capabilities() & ~SUPPORTED_CAPABILITIES) != 0L
            || (payload.capabilities() & DesktopPackets.CAP_MULTI_MENU_GESTURES) == 0L) {
            Component reason = Component.literal("Salt's Inventory Update versions do not match. Update the mod on both client and server.");
            DesktopDebug.warn(
                "server desktop handshake rejected player={} protocol={} expected={} capabilities={} required={}",
                player.getName().getString(), payload.protocolVersion(), DesktopProtocol.VERSION,
                payload.capabilities(), SUPPORTED_CAPABILITIES
            );
            PROTOCOL_REJECTED.add(player);
            player.connection.disconnect(reason);
            return;
        }

        sessions.connectionNonce = nextToken();
        sessions.playerSessionToken = nextToken();
        sessions.capabilities = payload.capabilities() & SUPPORTED_CAPABILITIES;
        sessions.forcedMenuIds = validateForcedMenuIds(payload.forcedMenuIds());
        sessions.negotiated = true;
        sessions.uiEnabled = payload.uiEnabled();
        sessions.gameplayEnabled = payload.uiEnabled();
        sessions.lastModeSequence = -1L;
        sessions.lastMutationId = 0L;
        sessions.closeAll(player, false);
        sessions.links.clear();
        send(player, new DesktopHelloAckPayload(
            DesktopProtocol.VERSION,
            payload.clientNonce(),
            sessions.connectionNonce,
            sessions.capabilities,
            sessions.uiEnabled,
            sessions.playerSessionToken
        ));
        InventoryExpansion.appendMissingMenuSlots(player.inventoryMenu, player);
        InventoryExpansion.syncToClient(player);
        DesktopDebug.log("server desktop handshake accepted player={} ui={} capabilities={}", player.getName().getString(), sessions.uiEnabled, sessions.capabilities);
    }

    private static void setMode(ServerPlayer player, DesktopModePayload payload) {
        PlayerSessions sessions = PLAYERS.get(player);
        if (sessions == null
            || !sessions.negotiated
            || payload.connectionNonce() != sessions.connectionNonce
            || payload.sequence() <= sessions.lastModeSequence
            || !sessions.modeRateLimit.tryConsume(1.0D, System.nanoTime())) {
            DesktopDebug.trace("server desktop mode dropped player={} sequence={} reason=unauthorized-or-stale", player.getName().getString(), payload.sequence());
            return;
        }
        sessions.lastModeSequence = payload.sequence();
        sessions.forcedMenuIds = validateForcedMenuIds(payload.forcedMenuIds());
        if (!payload.uiEnabled()) {
            PENDING_USE_TARGETS.remove(player);
            sessions.uiEnabled = false;
            sessions.closeAll(player, true);
            sessions.links.clear();
            sessions.gameplayEnabled = false;
        } else {
            sessions.gameplayEnabled = true;
            sessions.uiEnabled = true;
        }
        DesktopDebug.log("server desktop mode player={} ui={} sequence={}", player.getName().getString(), sessions.uiEnabled, sessions.lastModeSequence);
    }

    private static void rejectLegacyReady(ServerPlayer player) {
        if (!PROTOCOL_REJECTED.add(player)) {
            return;
        }
        PENDING_USE_TARGETS.remove(player);
        PlayerSessions sessions = PLAYERS.get(player);
        if (sessions != null) {
            sessions.closeAll(player, false);
            sessions.uiEnabled = false;
            sessions.gameplayEnabled = false;
            sessions.negotiated = false;
            sessions.links.clear();
        }
        DesktopDebug.warn("server rejected legacy desktop-ready packet player={} expectedProtocol={}", player.getName().getString(), DesktopProtocol.VERSION);
        player.connection.disconnect(Component.literal("Salt's Inventory Update is incompatible. Update the mod on both client and server."));
    }

    private static void disconnect(ServerPlayer player) {
        PlayerSessions sessions = PLAYERS.get(player);
        PENDING_USE_TARGETS.remove(player);
        if (sessions != null) {
            DesktopDebug.log("server disconnect close player={} sessions={}", player.getName().getString(), sessions.sessions.size());
            sessions.closeAll(player, false);
        }
        PLAYERS.remove(player);
        PROTOCOL_REJECTED.remove(player);
    }

    private static void tick(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
        PlayerSessions sessions = PLAYERS.get(player);
            if (sessions != null && sessions.isActive()) {
                sessions.tick(player);
            }
        }
    }

    private static void handleMutation(
        ServerPlayer player,
        long connectionNonce,
        long playerSessionToken,
        long mutationId,
        String operation,
        Runnable mutation
    ) {
        handleMutation(player, connectionNonce, playerSessionToken, mutationId, operation, 0.0D, mutation);
    }

    private static void handleMutation(
        ServerPlayer player,
        long connectionNonce,
        long playerSessionToken,
        long mutationId,
        String operation,
        double preAuthorizationCost,
        Runnable mutation
    ) {
        PlayerSessions sessions = PLAYERS.get(player);
        if (sessions == null
            || !sessions.authorizesMutationConnection(connectionNonce, playerSessionToken)) {
            DesktopDebug.trace(
                "server mutation dropped player={} id={} operation={} reason=connection-or-player-auth",
                player.getName().getString(), mutationId, operation
            );
            return;
        }

        MutationSequence sequence = sessions.acceptMutation(mutationId);
        boolean sequenceAccepted = sequence != MutationSequence.REJECTED;
        boolean acknowledge = sequence == MutationSequence.NEXT
            || sessions.allowMutationReplay(player, operation);
        try {
            if (sequence == MutationSequence.NEXT) {
                if (preAuthorizationCost <= 0.0D
                    || sessions.allowOperation(player, operation + "-preauthorize", preAuthorizationCost)) {
                    mutation.run();
                }
            } else if (sequence == MutationSequence.DUPLICATE) {
                DesktopDebug.trace(
                    "server mutation duplicate player={} id={} operation={}",
                    player.getName().getString(), mutationId, operation
                );
            } else {
                if (acknowledge) {
                    DesktopDebug.warn(
                        "server mutation sequence rejected player={} id={} expected={} operation={}",
                        player.getName().getString(), mutationId, sessions.nextMutationId(), operation
                    );
                    // Immediate-only optimistic controls may already have changed opaque client
                    // state. Keep the negative acknowledgement responsive, but use the stricter
                    // resync bucket for the comparatively expensive all-session snapshot.
                    if (sessions.allowResync(player, operation + "-sequence")) {
                        resyncGesture(player, sessions);
                    }
                } else {
                    DesktopDebug.trace(
                        "server mutation sequence rejected without reply player={} id={} expected={} operation={}",
                        player.getName().getString(), mutationId, sessions.nextMutationId(), operation
                    );
                }
            }
        } catch (RuntimeException exception) {
            DesktopDebug.warn(
                "server mutation failed player={} id={} operation={} reason={}",
                player.getName().getString(), mutationId, operation, exception.toString()
            );
            recoverGesture(player, sessions, operation + "-mutation", exception);
        } finally {
            if (acknowledge) {
                finishMutation(player, sessions, mutationId, sequenceAccepted);
            }
        }
    }

    private static double proportionalBatchCost(int count, int entriesPerAdditionalToken) {
        return 1.0D + Math.max(0, count - 1) / entriesPerAdditionalToken;
    }

    private static void finishMutation(
        ServerPlayer player,
        PlayerSessions sessions,
        long mutationId,
        boolean sequenceAccepted
    ) {
        // Every handler emits its authoritative sync (or recovery sync) before returning.
        // The networking layer preserves that send order, so this acknowledgement releases
        // the shared-cursor lane only after those earlier updates have been enqueued.
        if (mutationId > 0L) {
            send(player, new DesktopMutationAckPayload(
                sessions.connectionNonce,
                sessions.playerSessionToken,
                mutationId,
                sequenceAccepted
            ));
        }
    }

    private static void click(ServerPlayer player, DesktopClickPayload payload) {
        PlayerSessions sessions = PLAYERS.get(player);
        if (sessions == null) {
            DesktopDebug.trace("server click dropped player={} session={} reason=not-ready", player.getName().getString(), payload.sessionId());
            return;
        }
        SessionAuthorization authorization = authorizeMenu(
            player, sessions, payload.connectionNonce(), payload.sessionId(), payload.sessionToken(),
            payload.stateId(), 0L, "click"
        );
        if (authorization == null) {
            return;
        }

        ContainerInput input;
        try {
            input = ContainerInput.valueOf(payload.inputName());
        } catch (IllegalArgumentException exception) {
            DesktopDebug.warn("server click dropped player={} session={} reason=bad-input input={}", player.getName().getString(), payload.sessionId(), payload.inputName());
            return;
        }
        if (!mayMutateInventory(player) || !sessions.allowOperation(player, "click", 1.0D)) {
            DesktopDebug.trace("server click dropped player={} session={} reason=player-state", player.getName().getString(), payload.sessionId());
            return;
        }

        if (authorization.session() == null) {
            DesktopDebug.trace("server click player-menu id={} player={} slot={} button={} input={} clientCarried={}", payload.debugId(), player.getName().getString(), payload.slotIndex(), payload.button(), input, payload.clientCarried());
            clickMenu(payload.debugId(), player, sessions, authorization.menu(), payload.slotIndex(), payload.button(), input, payload.clientCarried());
            player.inventoryMenu.broadcastChanges();
            sessions.broadcastAll(player);
            syncCarried(player, sessions);
            return;
        }

        Session session = authorization.session();
        DesktopDebug.trace("server click session id={} player={} session={} slot={} button={} input={} clientCarried={}", payload.debugId(), player.getName().getString(), payload.sessionId(), payload.slotIndex(), payload.button(), input, payload.clientCarried());
        clickMenu(payload.debugId(), player, sessions, session.menu, payload.slotIndex(), payload.button(), input, payload.clientCarried());
        sessions.broadcastAll(player);
        syncCarried(player, sessions);
    }

    private static void dragSlots(ServerPlayer player, DesktopDragSlotsPayload payload) {
        PlayerSessions sessions = PLAYERS.get(player);
        if (sessions == null || !mayMutateInventory(player)) {
            return;
        }
        int quickCraftType = payload.quickCraftType();
        if (quickCraftType == AbstractContainerMenu.QUICKCRAFT_TYPE_CLONE && !player.hasInfiniteMaterials()) {
            DesktopDebug.trace("server drag slots dropped player={} reason=invalid-clone", player.getName().getString());
            return;
        }

        List<AuthorizedSlot> targets = authorizeGestureSlots(player, sessions, payload.connectionNonce(), payload.slots(), "drag-slots");
        if (targets == null) {
            return;
        }
        ItemStack carried = player.inventoryMenu.getCarried().copy();
        if (carried.isEmpty() || DesktopItemSourceLocks.matchesLockedSource(player, carried)) {
            resyncGesture(player, sessions);
            return;
        }

        List<DragTargetPlan> plans = new ArrayList<>(targets.size());
        try {
            IdentityHashMap<Object, Set<Integer>> physicalSlots = new IdentityHashMap<>();
            int remaining = carried.getCount();
            int targetCount = targets.size();
            for (AuthorizedSlot target : targets) {
                Slot slot = target.slot();
                Object physicalOwner = DesktopMenuSlots.physicalOwner(target.authorization().menu(), slot);
                int physicalIndex = DesktopMenuSlots.physicalIndex(target.authorization().menu(), slot);
                if (!rememberPhysicalSlot(physicalSlots, physicalOwner, physicalIndex)
                    || DesktopItemSourceLocks.isSlotLocked(player, slot)
                    || !slot.isActive()
                    || slot.isFake()
                    || !slot.mayPlace(carried)
                    || !target.authorization().menu().canDragTo(slot)
                    || !DesktopMenuSlots.canQuickCraft(target.authorization().menu(), slot, carried)) {
                    DesktopDebug.trace(
                        "server drag slots dropped player={} session={} slot={} reason=invalid-target",
                        player.getName().getString(),
                        target.reference().sessionId(),
                        target.reference().slotIndex()
                    );
                    resyncGesture(player, sessions);
                    return;
                }
                ItemStack before = slot.getItem().copy();
                int existingCount = before.isEmpty() ? 0 : before.getCount();
                int placement = DesktopMenuSlots.quickCraftPlaceCount(
                    target.authorization().menu(), slot, targetCount, quickCraftType, carried
                );
                int maximum = DesktopMenuSlots.quickCraftMaxStackSize(target.authorization().menu(), slot, carried);
                int newCount = Math.min(maximum, saturatingAdd(existingCount, placement));
                int inserted = Math.max(0, newCount - existingCount);
                if (quickCraftType != AbstractContainerMenu.QUICKCRAFT_TYPE_CLONE) {
                    inserted = Math.min(inserted, Math.max(0, remaining));
                    newCount = existingCount + inserted;
                    remaining -= inserted;
                }
                // Snapshot every authorized physical target, including zero-capacity targets. A
                // callback from a different slot must not be able to rewrite an omitted target.
                ItemStack after = inserted > 0 ? carried.copyWithCount(newCount) : before.copy();
                plans.add(new DragTargetPlan(
                    target.authorization().menu(), slot, physicalOwner, physicalIndex, before, after
                ));
            }
        } catch (RuntimeException exception) {
            recoverGesture(player, sessions, "drag-slots-validate", exception);
            return;
        }

        int insertedTotal = plans.stream()
            .mapToInt(plan -> plan.after().getCount() - plan.before().getCount())
            .sum();
        ItemStack carriedAfter = carried.copy();
        if (quickCraftType != AbstractContainerMenu.QUICKCRAFT_TYPE_CLONE) {
            carriedAfter.shrink(insertedTotal);
        }
        if (!runGestureMutation(player, sessions, "drag-slots", () -> {
            if (!ItemStack.matches(carried, player.inventoryMenu.getCarried())) {
                throw new IllegalStateException("Drag cursor changed before commit");
            }
            for (DragTargetPlan plan : plans) {
                verifyDragPhysicalTargets(plans);
                if (DesktopItemSourceLocks.isSlotLocked(player, plan.slot())
                    || !plan.slot().isActive()
                    || plan.slot().isFake()
                    || !plan.slot().mayPlace(carried)
                    || !plan.menu().canDragTo(plan.slot())
                    || !DesktopMenuSlots.canQuickCraft(plan.menu(), plan.slot(), carried)
                    || !ItemStack.matches(plan.before(), plan.slot().getItem())) {
                    throw new IllegalStateException("Drag target changed or became ineligible before commit");
                }
                if (ItemStack.matches(plan.before(), plan.after())) {
                    continue;
                }
                withMenuNetworkSession(player, sessions, plan.menu(), () ->
                    plan.slot().setByPlayer(plan.after().copy(), plan.before().copy())
                );
                if (!ItemStack.matches(plan.after(), plan.slot().getItem())) {
                    throw new IllegalStateException("Drag target did not accept the verified stack");
                }
                if (!ItemStack.matches(carried, player.inventoryMenu.getCarried())) {
                    throw new IllegalStateException("Drag target callback changed the canonical cursor");
                }
            }

            // A later custom-slot callback can mutate an earlier target (or an earlier callback can
            // mutate a target that has not run yet). Verify the complete pre-snapshotted target set
            // as one transaction before committing the shared cursor.
            verifyDragTargets(plans);
            if (!ItemStack.matches(carried, player.inventoryMenu.getCarried())) {
                throw new IllegalStateException("Drag cursor changed before remainder commit");
            }

            setSharedCarried(player, sessions, carriedAfter);
            if (!ItemStack.matches(carriedAfter, player.inventoryMenu.getCarried())) {
                throw new IllegalStateException("Drag cursor did not accept the verified remainder");
            }
            verifyDragTargets(plans);
            broadcastGesture(player, sessions);
        }, () -> rollbackDrag(
            player,
            sessions,
            plans,
            carried,
            quickCraftType == AbstractContainerMenu.QUICKCRAFT_TYPE_CLONE
        ))) {
            return;
        }
        DesktopDebug.trace(
            "server drag slots player={} type={} targets={} carriedBefore={} carriedAfter={}",
            player.getName().getString(), quickCraftType, targets.size(), carried, carriedAfter
        );
    }

    private static void verifyDragTargets(List<DragTargetPlan> plans) {
        verifyDragPhysicalTargets(plans);
        for (DragTargetPlan plan : plans) {
            if (!ItemStack.matches(plan.after(), plan.slot().getItem())) {
                throw new IllegalStateException("Drag target transaction changed after its verified commit");
            }
        }
    }

    private static void verifyDragPhysicalTargets(List<DragTargetPlan> plans) {
        IdentityHashMap<Object, Set<Integer>> physicalSlots = new IdentityHashMap<>();
        for (DragTargetPlan plan : plans) {
            if (!matchesPhysicalSlot(plan.menu(), plan.slot(), plan.physicalOwner(), plan.physicalIndex())
                || !rememberPhysicalSlot(physicalSlots, plan.physicalOwner(), plan.physicalIndex())) {
                throw new IllegalStateException("Drag target physical identity changed or became aliased");
            }
        }
    }

    private static void pickupAll(ServerPlayer player, DesktopPickupAllPayload payload) {
        PlayerSessions sessions = PLAYERS.get(player);
        if (sessions == null || !mayMutateInventory(player)) {
            return;
        }
        LinkedHashMap<Integer, SessionAuthorization> sources = authorizeGestureSessions(
            player, sessions, payload.connectionNonce(), payload.sources(), "pickup-all"
        );
        if (sources == null) {
            return;
        }
        int totalLogicalSlots = 0;
        try {
            for (SessionAuthorization source : sources.values()) {
                int logicalSlots = DesktopMenuSlots.size(source.menu());
                if (logicalSlots < 0 || logicalSlots > MAX_PICKUP_ALL_LOGICAL_SLOTS - totalLogicalSlots) {
                    DesktopDebug.trace(
                        "server pickup all dropped player={} reason=logical-scan-limit slots={}",
                        player.getName().getString(), totalLogicalSlots + (long) logicalSlots
                    );
                    resyncGesture(player, sessions);
                    return;
                }
                totalLogicalSlots += logicalSlots;
            }
        } catch (RuntimeException exception) {
            recoverGesture(player, sessions, "pickup-all-size", exception);
            return;
        }
        double supplementalScanCost = Math.max(0, totalLogicalSlots - 1) / (double) PICKUP_ALL_SLOTS_PER_RATE_TOKEN;
        if (supplementalScanCost > 0.0D
            && !sessions.allowOperation(player, "pickup-all-logical-scan", Math.min(64.0D, supplementalScanCost))) {
            return;
        }
        SessionAuthorization anchorAuthorization = sources.get(payload.anchorSessionId());
        Slot anchor = anchorAuthorization == null
            ? null
            : DesktopMenuSlots.slot(anchorAuthorization.menu(), payload.anchorSlotIndex());
        ItemStack carried = player.inventoryMenu.getCarried().copy();
        if (anchor == null
            || carried.isEmpty()
            || anchor.hasItem() && anchor.mayPickup(player)
            || !DesktopMenuSlots.allowsPickupAllAnchor(anchorAuthorization.menu(), carried, anchor)
            || DesktopItemSourceLocks.matchesLockedSource(player, carried)) {
            resyncGesture(player, sessions);
            return;
        }

        List<PickupSource> candidates = new ArrayList<>();
        try {
            IdentityHashMap<Object, Set<Integer>> physicalSlots = new IdentityHashMap<>();
            for (SessionAuthorization source : sources.values()) {
                List<Slot> slots = source.session() == null
                    ? DesktopMenuSlots.all(source.menu())
                    : containerSlots(source.menu(), player);
                if (payload.button() != 0) {
                    slots = new ArrayList<>(slots);
                    java.util.Collections.reverse(slots);
                }
                for (Slot slot : slots) {
                    // An ineligible read-only alias (for example a settings view) must not hide an
                    // eligible alias to the same resource handler that appears later in the scan.
                    if (DesktopItemSourceLocks.isSlotLocked(player, slot)
                        || !slot.isActive()
                        || slot.isFake()
                        || !slot.hasItem()
                        || !slot.mayPickup(player)
                        || !DesktopMenuSlots.canQuickCraft(source.menu(), slot, carried)
                        || !DesktopMenuSlots.allowsPickupAll(source.menu(), carried, slot)) {
                        continue;
                    }
                    Object physicalOwner = DesktopMenuSlots.physicalOwner(source.menu(), slot);
                    int physicalIndex = DesktopMenuSlots.physicalIndex(source.menu(), slot);
                    if (rememberPhysicalSlot(physicalSlots, physicalOwner, physicalIndex)) {
                        candidates.add(new PickupSource(
                            source.menu(), slot, physicalOwner, physicalIndex
                        ));
                    }
                }
            }
        } catch (RuntimeException exception) {
            recoverGesture(player, sessions, "pickup-all-validate", exception);
            return;
        }

        ItemStack carriedBeforePickup = carried.copy();
        int before = carried.getCount();
        List<PickupTargetPlan> plans = new ArrayList<>(candidates.size());
        IdentityHashMap<Slot, PickupTargetPlan> plansBySlot = new IdentityHashMap<>();
        try {
            // Snapshot every physically unique candidate before invoking the first arbitrary slot
            // callback. Lazy snapshots would allow one source callback to rewrite a later source
            // and have that rewritten value incorrectly accepted as the gesture's starting state.
            for (PickupSource candidate : candidates) {
                PickupTargetPlan plan = new PickupTargetPlan(
                    candidate.menu(), candidate.slot(), candidate.physicalOwner(),
                    candidate.physicalIndex(), candidate.slot().getItem()
                );
                plans.add(plan);
                plansBySlot.put(candidate.slot(), plan);
            }
        } catch (RuntimeException exception) {
            recoverGesture(player, sessions, "pickup-all-snapshot", exception);
            return;
        }
        if (!runGestureMutation(player, sessions, "pickup-all", () -> {
            if (!ItemStack.matches(carriedBeforePickup, player.inventoryMenu.getCarried())) {
                throw new IllegalStateException("Pickup-all cursor changed before commit");
            }
            for (int pass = 0; pass < 2 && carried.getCount() < carried.getMaxStackSize(); pass++) {
                for (PickupSource candidate : candidates) {
                    Slot slot = candidate.slot();
                    PickupTargetPlan plan = plansBySlot.get(slot);
                    if (carried.getCount() >= carried.getMaxStackSize()) {
                        break;
                    }
                    if (plan == null
                        || !matchesPhysicalSlot(
                            plan.menu(), slot, plan.physicalOwner(), plan.physicalIndex()
                        )
                        || !ItemStack.matches(plan.expected(), slot.getItem())) {
                        throw new IllegalStateException("Pickup-all source changed before its commit");
                    }
                    if (DesktopItemSourceLocks.isSlotLocked(player, slot)
                        || !slot.isActive()
                        || slot.isFake()
                        || !slot.hasItem()
                        || !slot.mayPickup(player)
                        || !ItemStack.isSameItemSameComponents(carried, slot.getItem())
                        || !DesktopMenuSlots.canQuickCraft(candidate.menu(), slot, carried)
                        || !DesktopMenuSlots.allowsPickupAll(candidate.menu(), carried, slot)) {
                        continue;
                    }
                    ItemStack sourceBefore = plan.expected();
                    if (pass == 0 && sourceBefore.getCount() == sourceBefore.getMaxStackSize()) {
                        continue;
                    }
                    int requested = Math.min(
                        sourceBefore.getCount(),
                        carried.getMaxStackSize() - carried.getCount()
                    );
                    if (requested <= 0) {
                        continue;
                    }
                    ItemStack[] removed = {ItemStack.EMPTY};
                    withMenuNetworkSession(player, sessions, candidate.menu(), () ->
                        removed[0] = slot.safeTake(sourceBefore.getCount(), requested, player)
                    );
                    ItemStack taken = removed[0];
                    if (taken == null
                        || taken.getCount() < 0
                        || taken.getCount() > requested
                        || !taken.isEmpty() && !ItemStack.isSameItemSameComponents(carried, taken)) {
                        throw new IllegalStateException("Pickup-all source returned an invalid stack");
                    }
                    ItemStack expectedSource = sourceBefore.copy();
                    expectedSource.shrink(taken.getCount());
                    if (!ItemStack.matches(expectedSource, slot.getItem())) {
                        throw new IllegalStateException("Pickup-all source removal could not be verified");
                    }
                    plan.expect(expectedSource);
                    if (!ItemStack.matches(carriedBeforePickup, player.inventoryMenu.getCarried())) {
                        throw new IllegalStateException("Pickup-all source callback changed the canonical cursor");
                    }
                    carried.grow(taken.getCount());
                }
            }

            verifyPickupSources(plans);

            if (carried.getCount() != before) {
                setSharedCarried(player, sessions, carried);
                if (!ItemStack.matches(carried, player.inventoryMenu.getCarried())) {
                    throw new IllegalStateException("Pickup-all cursor did not accept the verified collection");
                }
                verifyPickupSources(plans);
                broadcastGesture(player, sessions);
            } else {
                syncCarried(player, sessions);
            }
        }, () -> rollbackPickupAll(player, sessions, plans, carriedBeforePickup))) {
            return;
        }

        if (carried.getCount() != before) {
            DesktopDebug.trace(
                "server pickup all player={} sessions={} candidates={} collected={} carried={}",
                player.getName().getString(), sources.size(), candidates.size(), carried.getCount() - before, carried
            );
        }
    }

    private static void verifyPickupSources(List<PickupTargetPlan> plans) {
        IdentityHashMap<Object, Set<Integer>> physicalSlots = new IdentityHashMap<>();
        for (PickupTargetPlan plan : plans) {
            if (!matchesPhysicalSlot(
                    plan.menu(), plan.slot(), plan.physicalOwner(), plan.physicalIndex()
                )
                || !rememberPhysicalSlot(physicalSlots, plan.physicalOwner(), plan.physicalIndex())
                || !ItemStack.matches(plan.expected(), plan.slot().getItem())) {
                throw new IllegalStateException("Pickup-all source transaction changed after its verified removal");
            }
        }
    }

    private record SortDestination(SessionAuthorization authorization, List<Slot> slots, List<ItemStack> contents) {}

    private static void sortWindows(ServerPlayer player, DesktopSortWindowsPayload payload) {
        PlayerSessions sessions = PLAYERS.get(player);
        if (sessions == null || !mayMutateInventory(player) || !player.inventoryMenu.getCarried().isEmpty()) return;
        var reference = payload.source();
        SessionAuthorization source = authorizeMenu(player, sessions, payload.connectionNonce(),
            reference.sessionId(), reference.sessionToken(), reference.stateId(), DesktopPackets.CAP_SORT_WINDOWS, "sort-source");
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
            SessionAuthorization target = authorizeMenu(player, sessions, payload.connectionNonce(),
                targetRef.sessionId(), targetRef.sessionToken(), targetRef.stateId(), DesktopPackets.CAP_SORT_WINDOWS, "sort-target");
            if (target == null) { resyncGesture(player, sessions); return; }
            if (!DesktopSortSlots.isStorage(target.menu())) continue;
            String key = target.session().sourceKey;
            if (!key.isBlank() && !sourceKeys.add(key)) continue;
            List<Slot> slots = containerSlots(target.menu(), player).stream()
                .filter(slot -> DesktopSortSlots.storageSlot(target.menu(), slot))
                .filter(slot -> slot.isActive() && !slot.isFake()).toList();
            if (slots.isEmpty() || slots.stream().anyMatch(slot -> owners.contains(DesktopMenuSlots.physicalOwner(target.menu(), slot)))) continue;
            for (Slot slot : slots) owners.add(DesktopMenuSlots.physicalOwner(target.menu(), slot));
            destinations.add(new SortDestination(target, slots, slots.stream().map(slot -> slot.getItem().copy()).toList()));
        }
        // Capture sources as well: output callbacks must not introduce new work into this action.
        List<SlotSource> sources = new ArrayList<>();
        List<ItemStack> snapshots = new ArrayList<>();
        IdentityHashMap<Object, Set<Integer>> seen = new IdentityHashMap<>();
        for (Slot slot : sourceSlots) {
            if (!slot.hasItem() || !slot.isActive() || slot.isFake() || !slot.mayPickup(player)
                || DesktopItemSourceLocks.isSlotLocked(player, slot)
                || !DesktopMenuSlots.allowsTargetedQuickMoveSource(sourceMenu, slot)
                || DesktopMenuSlots.repeatedQuickMoveLimit(sourceMenu, slot, slot.getItem()) <= 0
                || !rememberPhysicalSlot(seen, sourceMenu, slot)) continue;
            sources.add(new SlotSource(source.sessionId(), sourceMenu, slot, source.session()));
            snapshots.add(slot.getItem().copy());
        }
        runGestureMutation(player, sessions, "sort-windows", () -> {
            executeSortTransfers(player, sessions, sources, snapshots, destinations, payload.focusedSessionId(), payload.shift());
            broadcastGesture(player, sessions);
        });
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
                            && original.tags().anyMatch(stack::is));
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
            if (slot.hasItem() && ItemStack.isSameItemSameComponents(moving, slot.getItem())) candidates.add(slot);
        }
        for (Slot slot : slots) if (!slot.hasItem()) candidates.add(slot);
        int index = 0;
        while (source.slot.hasItem() && index < candidates.size()) {
            List<Slot> batch = new ArrayList<>();
            long capacity = 0;
            ItemStack current = source.slot.getItem();
            while (index < candidates.size() && batch.size() < 16 && capacity < current.getCount()) {
                Slot slot = candidates.get(index++);
                if (!slot.isActive() || slot.isFake() || !slot.mayPlace(current)) continue;
                int room = slot.getMaxStackSize(current) - slot.getItem().getCount();
                if (room <= 0) continue;
                batch.add(slot);
                capacity += room;
            }
            if (!batch.isEmpty()) moveSlotStack(player, sessions, source, targetMenu, batch, budget, true);
        }
    }

    private static void quickMoveAll(ServerPlayer player, DesktopQuickMoveAllPayload payload) {
        PlayerSessions sessions = PLAYERS.get(player);
        if (sessions == null || !mayMutateInventory(player)) {
            return;
        }
        if (payload.targetKind() < DesktopPackets.QUICK_TARGET_DEFAULT
            || payload.targetKind() > DesktopPackets.QUICK_TARGET_HOTBAR) {
            return;
        }
        List<AuthorizedSlot> sources = authorizeGestureSlots(
            player, sessions, payload.connectionNonce(), payload.sources(), "quick-move-all"
        );
        SessionAuthorization target = authorizeMenu(
            player,
            sessions,
            payload.connectionNonce(),
            payload.target().sessionId(),
            payload.target().sessionToken(),
            payload.target().stateId(),
            DesktopPackets.CAP_MULTI_MENU_GESTURES,
            "quick-move-all-target"
        );
        if (sources == null || target == null) {
            return;
        }
        int expectedTargetSession = payload.targetKind() == DesktopPackets.QUICK_TARGET_SESSION
            ? payload.target().sessionId()
            : DesktopPackets.PLAYER_MENU_SESSION;
        int sourceSession = sources.get(0).reference().sessionId();
        AbstractContainerMenu sourceMenu = sources.get(0).authorization().menu();
        Slot firstSlot = sources.get(0).slot();
        ItemStack match = firstSlot.getItem().copy();
        if (payload.target().sessionId() != expectedTargetSession
            || payload.targetKind() == DesktopPackets.QUICK_TARGET_SESSION && expectedTargetSession == sourceSession
            || match.isEmpty()
            || !player.inventoryMenu.getCarried().isEmpty()) {
            resyncGesture(player, sessions);
            return;
        }

        List<QuickMoveSourceSnapshot> sourceSnapshots = new ArrayList<>(sources.size());
        try {
            Object firstPhysicalOwner = DesktopMenuSlots.physicalOwner(sourceMenu, firstSlot);
            IdentityHashMap<Object, Set<Integer>> physicalSlots = new IdentityHashMap<>();
            for (AuthorizedSlot source : sources) {
                Slot slot = source.slot();
                Object physicalOwner = DesktopMenuSlots.physicalOwner(source.authorization().menu(), slot);
                int physicalIndex = DesktopMenuSlots.physicalIndex(source.authorization().menu(), slot);
                if (source.reference().sessionId() != sourceSession
                    || physicalOwner != firstPhysicalOwner
                    || !rememberPhysicalSlot(physicalSlots, physicalOwner, physicalIndex)
                    || DesktopItemSourceLocks.isSlotLocked(player, slot)
                    || !slot.isActive()
                    || slot.isFake()
                    || !slot.hasItem()
                    || !slot.mayPickup(player)
                    || !allowsQuickMoveRouting(source.authorization().menu(), slot, payload.targetKind())
                    || DesktopMenuSlots.repeatedQuickMoveLimit(
                        source.authorization().menu(), slot, slot.getItem()
                    ) <= 0
                    || !ItemStack.isSameItemSameComponents(match, slot.getItem())) {
                    resyncGesture(player, sessions);
                    return;
                }
                sourceSnapshots.add(new QuickMoveSourceSnapshot(
                    source.reference(), source.authorization(), physicalOwner, physicalIndex, slot.getItem().copy()
                ));
            }
        } catch (RuntimeException exception) {
            recoverGesture(player, sessions, "quick-move-all-validate", exception);
            return;
        }

        boolean[] moved = {false};
        boolean[] nativeClickAttempted = {false};
        boolean[] staleSource = {false};
        QuickMoveWorkBudget quickMoveWork = new QuickMoveWorkBudget(MAX_QUICK_MOVE_TRANSACTION_WORK);
        boolean nativeQuickMove = payload.targetKind() == DesktopPackets.QUICK_TARGET_DEFAULT
            && DesktopMenuSlots.useNativeQuickMove(sourceMenu);
        if (!runGestureMutation(player, sessions, "quick-move-all", () -> {
            for (QuickMoveSourceSnapshot source : sourceSnapshots) {
                Slot currentSlot = DesktopMenuSlots.slot(sourceMenu, source.reference().slotIndex());
                if (currentSlot == null
                    || DesktopMenuSlots.physicalOwner(sourceMenu, currentSlot) != source.physicalOwner()
                    || DesktopMenuSlots.physicalIndex(sourceMenu, currentSlot) != source.physicalIndex()
                    || DesktopItemSourceLocks.isSlotLocked(player, currentSlot)
                    || !currentSlot.isActive()
                    || currentSlot.isFake()
                    || !currentSlot.hasItem()
                    || !currentSlot.mayPickup(player)
                    || !allowsQuickMoveRouting(sourceMenu, currentSlot, payload.targetKind())
                    || DesktopMenuSlots.repeatedQuickMoveLimit(sourceMenu, currentSlot, currentSlot.getItem()) <= 0
                    || !ItemStack.matches(source.stack(), currentSlot.getItem())) {
                    staleSource[0] = true;
                    break;
                }

                ItemStack before = currentSlot.getItem().copy();
                SlotSource slotSource = new SlotSource(
                    source.reference().sessionId(), sourceMenu, currentSlot, source.authorization().session()
                );
                int nativeTargetSlot = payload.targetKind() == DesktopPackets.QUICK_TARGET_SESSION
                    && isPlayerInventorySlot(player, currentSlot)
                    && DesktopMenuSlots.useNativeQuickMove(target.menu())
                        ? physicalSlotIndexInMenu(sourceMenu, currentSlot, target.menu())
                        : -1;
                if (nativeTargetSlot >= 0) {
                    clickGestureMenu(player, sessions, target.menu(), nativeTargetSlot);
                    nativeClickAttempted[0] = true;
                    moved[0] |= !ItemStack.matches(before, currentSlot.getItem());
                    continue;
                }
                if (payload.targetKind() != DesktopPackets.QUICK_TARGET_SESSION
                    && isVanillaResultSource(slotSource, source.reference().slotIndex())) {
                    clickGestureMenu(player, sessions, sourceMenu, source.reference().slotIndex());
                    nativeClickAttempted[0] = true;
                    moved[0] |= !ItemStack.matches(before, currentSlot.getItem());
                    continue;
                }
                if (nativeQuickMove) {
                    // StorageContainerMenuBase.clicked performs its upgrade bookkeeping and owns
                    // repeated crafting/block-converter quick moves. Calling quickMoveStack skips both.
                    clickGestureMenu(player, sessions, sourceMenu, source.reference().slotIndex());
                    nativeClickAttempted[0] = true;
                    moved[0] |= !ItemStack.matches(before, currentSlot.getItem());
                    continue;
                }
                List<Slot> targets = new ArrayList<>(quickMoveTargets(
                    player, sessions, slotSource, payload.targetKind(), payload.target().sessionId()
                ));
                targets.removeIf(slot -> DesktopItemSourceLocks.isSlotLocked(player, slot));
                moved[0] |= moveSlotStack(
                    player, sessions, slotSource, target.menu(), targets, quickMoveWork
                );
            }

            if (staleSource[0]) {
                resyncGesture(player, sessions);
            } else if (moved[0] || nativeClickAttempted[0]) {
                broadcastGesture(player, sessions);
            } else {
                syncCarried(player, sessions);
            }
        })) {
            return;
        }

        if (staleSource[0]) {
            DesktopDebug.trace(
                "server quick move all stopped player={} sourceSession={} reason=source-changed-during-batch",
                player.getName().getString(), sourceSession
            );
            return;
        }

        if (moved[0]) {
            DesktopDebug.trace(
                "server quick move all player={} sourceSession={} sources={} targetKind={} targetSession={}",
                player.getName().getString(), sourceSession, sources.size(), payload.targetKind(), payload.target().sessionId()
            );
        }
    }

    private static void selectBundleItem(ServerPlayer player, DesktopBundleSelectPayload payload) {
        PlayerSessions sessions = PLAYERS.get(player);
        if (sessions == null) {
            return;
        }
        if (!mayMutateInventory(player)) {
            resyncGesture(player, sessions);
            return;
        }
        DesktopSlotReference reference = payload.target();
        SessionAuthorization authorization = authorizeMenu(
            player,
            sessions,
            payload.connectionNonce(),
            reference.sessionId(),
            reference.sessionToken(),
            reference.stateId(),
            DesktopPackets.CAP_MULTI_MENU_GESTURES,
            "bundle-select"
        );
        Slot slot = authorization == null ? null : DesktopMenuSlots.slot(authorization.menu(), reference.slotIndex());
        if (authorization == null) {
            return;
        }
        if (!sessions.allowOperation(player, "bundle-select", 1.0D)) {
            resyncGesture(player, sessions);
            return;
        }
        if (slot == null
            || DesktopItemSourceLocks.isSlotLocked(player, slot)
            || !slot.isActive()
            || slot.isFake()
            || !slot.hasItem()
            || !slot.getItem().is(net.minecraft.tags.ItemTags.BUNDLES)) {
            resyncGesture(player, sessions);
            return;
        }
        if (!runGestureMutation(player, sessions, "bundle-select", () -> {
            withMenuNetworkSession(player, sessions, authorization.menu(), () -> {
                net.minecraft.world.item.BundleItem.toggleSelectedItem(slot.getItem(), payload.selectedItemIndex());
                // StackCopySlot and similar transfer slots commit mutations from their cached copy here.
                slot.setChanged();
            });
            // The client applies the selection highlight optimistically. Publish the authoritative
            // component to every alias before ACK releases the following click or scroll intent.
            broadcastGesture(player, sessions);
        })) {
            return;
        }
        DesktopDebug.trace(
            "server bundle select player={} session={} slot={} selected={}",
            player.getName().getString(), reference.sessionId(), reference.slotIndex(), payload.selectedItemIndex()
        );
    }

    private static @Nullable List<AuthorizedSlot> authorizeGestureSlots(
        ServerPlayer player,
        PlayerSessions sessions,
        long connectionNonce,
        List<DesktopSlotReference> references,
        String operation
    ) {
        LinkedHashMap<Integer, SessionAuthorization> authorizations = new LinkedHashMap<>();
        LinkedHashMap<Integer, DesktopSlotReference> sessionReferences = new LinkedHashMap<>();
        List<AuthorizedSlot> slots = new ArrayList<>(references.size());
        for (DesktopSlotReference reference : references) {
            DesktopSlotReference existing = sessionReferences.get(reference.sessionId());
            if (existing != null
                && (existing.sessionToken() != reference.sessionToken() || existing.stateId() != reference.stateId())) {
                DesktopDebug.trace("server {} dropped player={} session={} reason=inconsistent-auth", operation, player.getName().getString(), reference.sessionId());
                return null;
            }
            SessionAuthorization authorization = authorizations.get(reference.sessionId());
            if (authorization == null) {
                authorization = authorizeMenu(
                    player, sessions, connectionNonce, reference.sessionId(), reference.sessionToken(),
                    reference.stateId(), DesktopPackets.CAP_MULTI_MENU_GESTURES, operation
                );
                if (authorization == null) {
                    return null;
                }
                authorizations.put(reference.sessionId(), authorization);
                sessionReferences.put(reference.sessionId(), reference);
            }
            Slot slot = DesktopMenuSlots.slot(authorization.menu(), reference.slotIndex());
            if (slot == null) {
                DesktopDebug.trace("server {} dropped player={} session={} slot={} reason=out-of-range", operation, player.getName().getString(), reference.sessionId(), reference.slotIndex());
                return null;
            }
            slots.add(new AuthorizedSlot(reference, authorization, slot));
        }
        return slots;
    }

    private static @Nullable LinkedHashMap<Integer, SessionAuthorization> authorizeGestureSessions(
        ServerPlayer player,
        PlayerSessions sessions,
        long connectionNonce,
        List<DesktopSessionReference> references,
        String operation
    ) {
        LinkedHashMap<Integer, SessionAuthorization> authorizations = new LinkedHashMap<>();
        for (DesktopSessionReference reference : references) {
            SessionAuthorization authorization = authorizeMenu(
                player, sessions, connectionNonce, reference.sessionId(), reference.sessionToken(),
                reference.stateId(), DesktopPackets.CAP_MULTI_MENU_GESTURES, operation
            );
            if (authorization == null) {
                return null;
            }
            authorizations.put(reference.sessionId(), authorization);
        }
        return authorizations;
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

    private static void broadcastGesture(ServerPlayer player, PlayerSessions sessions) {
        sessions.broadcastAll(player);
        syncCarried(player, sessions);
    }

    private static void resyncGesture(ServerPlayer player, PlayerSessions sessions) {
        try {
            syncPlayerMenu(player);
        } catch (RuntimeException exception) {
            DesktopDebug.warn(
                "server gesture player-menu resync failed player={} reason={}",
                player.getName().getString(),
                exception.toString()
            );
        }
        for (Session session : List.copyOf(sessions.sessions.values())) {
            try {
                withNetworkSession(player, session, session.menu::sendAllDataToRemote);
            } catch (RuntimeException exception) {
                DesktopDebug.warn(
                    "server gesture session resync failed player={} session={} reason={}",
                    player.getName().getString(),
                    session.sessionId,
                    exception.toString()
                );
            }
        }
        try {
            syncCarried(player, sessions);
        } catch (RuntimeException exception) {
            DesktopDebug.warn(
                "server gesture carried resync failed player={} reason={}",
                player.getName().getString(),
                exception.toString()
            );
        }
    }

    private static boolean runGestureMutation(
        ServerPlayer player,
        PlayerSessions sessions,
        String operation,
        Runnable mutation
    ) {
        return runGestureMutation(player, sessions, operation, mutation, () -> {
        });
    }

    private static boolean runGestureMutation(
        ServerPlayer player,
        PlayerSessions sessions,
        String operation,
        Runnable mutation,
        Runnable rollback
    ) {
        try {
            mutation.run();
            return true;
        } catch (RuntimeException exception) {
            try {
                rollback.run();
            } catch (RuntimeException rollbackException) {
                exception.addSuppressed(rollbackException);
                DesktopDebug.warn(
                    "server {} rollback failed player={} reason={}",
                    operation,
                    player.getName().getString(),
                    rollbackException.toString()
                );
            }
            recoverGesture(player, sessions, operation, exception);
            return false;
        }
    }

    private static void rollbackDrag(
        ServerPlayer player,
        PlayerSessions sessions,
        List<DragTargetPlan> plans,
        ItemStack carriedBefore,
        boolean clone
    ) {
        RuntimeException rollbackFailure = null;
        boolean ambiguousPhysicalTargets = false;
        IdentityHashMap<Object, Set<Integer>> restoredPhysicalSlots = new IdentityHashMap<>();
        for (int index = plans.size() - 1; index >= 0; index--) {
            DragTargetPlan plan = plans.get(index);
            try {
                if (!matchesPhysicalSlot(plan.menu(), plan.slot(), plan.physicalOwner(), plan.physicalIndex())
                    || !rememberPhysicalSlot(
                        restoredPhysicalSlots, plan.physicalOwner(), plan.physicalIndex()
                    )) {
                    ambiguousPhysicalTargets = true;
                    continue;
                }
            } catch (RuntimeException exception) {
                ambiguousPhysicalTargets = true;
                rollbackFailure = appendSuppressed(rollbackFailure, exception);
                continue;
            }
            if (ItemStack.matches(plan.before(), plan.slot().getItem())) {
                continue;
            }
            try {
                restoreSlot(player, sessions, plan.menu(), plan.slot(), plan.before(), "drag target");
            } catch (RuntimeException exception) {
                rollbackFailure = appendSuppressed(rollbackFailure, exception);
            }
        }
        // Recheck after every callback: restoring a later custom slot may mutate one restored earlier.
        int unrevertedGain = 0;
        boolean unknownGain = ambiguousPhysicalTargets;
        IdentityHashMap<Object, Set<Integer>> measuredPhysicalSlots = new IdentityHashMap<>();
        for (DragTargetPlan plan : plans) {
            try {
                if (!matchesPhysicalSlot(plan.menu(), plan.slot(), plan.physicalOwner(), plan.physicalIndex())
                    || !rememberPhysicalSlot(
                        measuredPhysicalSlots, plan.physicalOwner(), plan.physicalIndex()
                    )) {
                    unknownGain = true;
                    continue;
                }
            } catch (RuntimeException exception) {
                unknownGain = true;
                rollbackFailure = appendSuppressed(rollbackFailure, exception);
                continue;
            }
            int gain = observedTargetGain(plan.before(), plan.slot().getItem(), carriedBefore);
            if (gain < 0) {
                unknownGain = true;
            } else {
                unrevertedGain = saturatingAdd(unrevertedGain, gain);
            }
        }
        try {
            ItemStack reconciledCarried = carriedBefore.copy();
            if (!clone) {
                reconciledCarried.shrink(unknownGain ? carriedBefore.getCount() : unrevertedGain);
            }
            setSharedCarried(player, sessions, reconciledCarried);
            if (!ItemStack.matches(reconciledCarried, player.inventoryMenu.getCarried())) {
                throw new IllegalStateException("Drag cursor rollback could not be verified");
            }
        } catch (RuntimeException exception) {
            rollbackFailure = appendSuppressed(rollbackFailure, exception);
        }
        if (ambiguousPhysicalTargets) {
            rollbackFailure = appendSuppressed(
                rollbackFailure,
                new IllegalStateException("Drag rollback encountered a rebound or aliased physical target")
            );
        }
        if (rollbackFailure != null) {
            throw rollbackFailure;
        }
    }

    private static void rollbackPickupAll(
        ServerPlayer player,
        PlayerSessions sessions,
        List<PickupTargetPlan> touched,
        ItemStack carriedBefore
    ) {
        RuntimeException rollbackFailure = null;
        boolean ambiguousPhysicalSources = false;
        IdentityHashMap<Object, Set<Integer>> restoredPhysicalSlots = new IdentityHashMap<>();
        for (int index = touched.size() - 1; index >= 0; index--) {
            PickupTargetPlan plan = touched.get(index);
            try {
                if (!matchesPhysicalSlot(
                        plan.menu(), plan.slot(), plan.physicalOwner(), plan.physicalIndex()
                    )
                    || !rememberPhysicalSlot(
                        restoredPhysicalSlots, plan.physicalOwner(), plan.physicalIndex()
                    )) {
                    ambiguousPhysicalSources = true;
                    continue;
                }
            } catch (RuntimeException exception) {
                ambiguousPhysicalSources = true;
                rollbackFailure = appendSuppressed(rollbackFailure, exception);
                continue;
            }
            if (ItemStack.matches(plan.before(), plan.slot().getItem())) {
                continue;
            }
            try {
                restoreSlot(player, sessions, plan.menu(), plan.slot(), plan.before(), "pickup-all source");
            } catch (RuntimeException exception) {
                rollbackFailure = appendSuppressed(rollbackFailure, exception);
            }
        }
        // A later slot callback may touch an earlier slot. Measure each unique source only after all
        // restore callbacks have completed so overlapping snapshots cannot duplicate cursor credit.
        int unrevertedLoss = 0;
        boolean unknownLoss = ambiguousPhysicalSources;
        IdentityHashMap<Object, Set<Integer>> measuredPhysicalSlots = new IdentityHashMap<>();
        for (PickupTargetPlan plan : touched) {
            try {
                if (!matchesPhysicalSlot(
                        plan.menu(), plan.slot(), plan.physicalOwner(), plan.physicalIndex()
                    )
                    || !rememberPhysicalSlot(
                        measuredPhysicalSlots, plan.physicalOwner(), plan.physicalIndex()
                    )) {
                    unknownLoss = true;
                    continue;
                }
            } catch (RuntimeException exception) {
                unknownLoss = true;
                rollbackFailure = appendSuppressed(rollbackFailure, exception);
                continue;
            }
            int loss = observedSourceLoss(plan.before(), plan.slot().getItem());
            if (loss < 0) {
                unknownLoss = true;
            } else {
                unrevertedLoss = saturatingAdd(unrevertedLoss, loss);
            }
        }
        try {
            ItemStack reconciledCarried = carriedBefore.copy();
            if (unrevertedLoss > 0) {
                int available = Math.max(0, reconciledCarried.getMaxStackSize() - reconciledCarried.getCount());
                reconciledCarried.grow(Math.min(available, unrevertedLoss));
            }
            if (unknownLoss) {
                DesktopDebug.warn(
                    "server pickup-all rollback left an ambiguous custom source player={}; proven losses were preserved",
                    player.getName().getString()
                );
            }
            setSharedCarried(player, sessions, reconciledCarried);
            if (!ItemStack.matches(reconciledCarried, player.inventoryMenu.getCarried())) {
                throw new IllegalStateException("Pickup-all cursor rollback could not be verified");
            }
        } catch (RuntimeException exception) {
            rollbackFailure = appendSuppressed(rollbackFailure, exception);
        }
        if (ambiguousPhysicalSources) {
            rollbackFailure = appendSuppressed(
                rollbackFailure,
                new IllegalStateException("Pickup-all rollback encountered a rebound or aliased physical source")
            );
        }
        if (rollbackFailure != null) {
            throw rollbackFailure;
        }
    }

    /** Returns the observable loss relative to a source snapshot, or {@code -1} when it is ambiguous. */
    private static int observedSourceLoss(ItemStack before, ItemStack current) {
        if (ItemStack.matches(before, current)) {
            return 0;
        }
        if (before.isEmpty()) {
            return -1;
        }
        if (current.isEmpty()) {
            return before.getCount();
        }
        if (!ItemStack.isSameItemSameComponents(before, current) || current.getCount() > before.getCount()) {
            return -1;
        }
        return before.getCount() - current.getCount();
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

    private static void recoverGesture(
        ServerPlayer player,
        PlayerSessions sessions,
        String operation,
        RuntimeException exception
    ) {
        DesktopDebug.warn(
            "server {} failed player={} reason={}",
            operation,
            player.getName().getString(),
            exception.toString()
        );
        try {
            clearDetachedCarried(sessions);
        } catch (RuntimeException clearException) {
            DesktopDebug.warn(
                "server {} detached cursor reset failed player={} reason={}",
                operation,
                player.getName().getString(),
                clearException.toString()
            );
        }
        resyncGesture(player, sessions);
    }

    /** Invokes QUICK_MOVE through the owning menu so mod overrides retain their bookkeeping. */
    private static void clickGestureMenu(
        ServerPlayer player,
        PlayerSessions sessions,
        AbstractContainerMenu menu,
        int slotIndex
    ) {
        ItemStack carriedBefore = player.inventoryMenu.getCarried().copy();
        boolean committedCarried = false;
        try {
            menu.setCarried(carriedBefore.copy());
            withMenuNetworkSession(player, sessions, menu, () ->
                menu.clicked(slotIndex, 0, ContainerInput.QUICK_MOVE, player)
            );
            player.inventoryMenu.setCarried(menu.getCarried().copy());
            committedCarried = true;
        } finally {
            if (!committedCarried) {
                player.inventoryMenu.setCarried(carriedBefore);
            }
            clearDetachedCarried(sessions);
        }
    }

    private static void clickQuickMoveMenu(
        ServerPlayer player,
        PlayerSessions sessions,
        AbstractContainerMenu menu,
        int slotIndex,
        ItemStack carriedBefore
    ) {
        if (!carriedBefore.isEmpty()) {
            setSharedCarried(player, sessions, ItemStack.EMPTY);
        }
        try {
            clickGestureMenu(player, sessions, menu, slotIndex);
        } finally {
            if (!carriedBefore.isEmpty()) {
                setSharedCarried(player, sessions, carriedBefore);
            }
        }
    }

    private static void carried(ServerPlayer player, DesktopCarriedPayload payload) {
        PlayerSessions sessions = PLAYERS.get(player);
        if (sessions == null || authorizeMenu(
            player, sessions, payload.connectionNonce(), DesktopPackets.PLAYER_MENU_SESSION,
            payload.playerSessionToken(), payload.stateId(), 0L, "carried"
        ) == null) {
            DesktopDebug.trace("server carried dropped player={} reason=not-ready stack={}", player.getName().getString(), payload.carried());
            return;
        }
        if (!mayMutateInventory(player) || !sessions.allowOperation(player, "carried", 1.0D)) {
            syncCarried(player, sessions);
            return;
        }

        if (!player.hasInfiniteMaterials()) {
            DesktopDebug.trace("server carried dropped player={} reason=not-creative stack={} serverCarried={}", player.getName().getString(), payload.carried(), player.inventoryMenu.getCarried());
            syncCarried(player, sessions);
            return;
        }

        ItemStack carried = payload.carried().copy();
        player.inventoryMenu.setCarried(carried.copy());
        for (Session session : sessions.sessions.values()) {
            session.menu.setCarried(ItemStack.EMPTY);
        }
        DesktopDebug.trace("server carried sync player={} stack={}", player.getName().getString(), carried);
        syncCarried(player, sessions);
    }

    private static void quickMove(ServerPlayer player, DesktopQuickMovePayload payload) {
        PlayerSessions sessions = PLAYERS.get(player);
        if (sessions == null) {
            DesktopDebug.trace("server quick move dropped player={} sourceSession={} reason=not-ready", player.getName().getString(), payload.sourceSessionId());
            return;
        }
        if (payload.targetKind() < DesktopPackets.QUICK_TARGET_DEFAULT
            || payload.targetKind() > DesktopPackets.QUICK_TARGET_HOTBAR
            || payload.targetKind() == DesktopPackets.QUICK_TARGET_SESSION
                && payload.targetSessionId() == payload.sourceSessionId()) {
            DesktopDebug.trace("server quick move dropped player={} sourceSession={} targetKind={} targetSession={} reason=invalid-target", player.getName().getString(), payload.sourceSessionId(), payload.targetKind(), payload.targetSessionId());
            return;
        }

        SessionAuthorization sourceAuthorization = authorizeMenu(
            player, sessions, payload.connectionNonce(), payload.sourceSessionId(), payload.sourceSessionToken(),
            payload.sourceStateId(), 0L, "quick-move-source"
        );
        if (sourceAuthorization == null) {
            return;
        }
        int authorizedTargetId = payload.targetKind() == DesktopPackets.QUICK_TARGET_SESSION
            ? payload.targetSessionId()
            : DesktopPackets.PLAYER_MENU_SESSION;
        SessionAuthorization targetAuthorization = authorizeMenu(
            player, sessions, payload.connectionNonce(), authorizedTargetId, payload.targetSessionToken(),
            payload.targetStateId(), 0L, "quick-move-target"
        );
        if (targetAuthorization == null) {
            return;
        }

        SlotSource source = resolveSlot(player, sessions, payload.sourceSessionId(), payload.sourceSlotIndex());
        if (source == null) {
            return;
        }
        if (!allowsQuickMoveRouting(source.menu, source.slot, payload.targetKind())) {
            DesktopDebug.trace(
                "server quick move dropped player={} sourceSession={} sourceSlot={} reason=source-disallows-target",
                player.getName().getString(), payload.sourceSessionId(), payload.sourceSlotIndex()
            );
            resyncGesture(player, sessions);
            return;
        }
        if (DesktopItemSourceLocks.isSlotLocked(player, source.slot)) {
            DesktopDebug.trace("server quick move dropped player={} sourceSession={} sourceSlot={} reason=locked-item-source", player.getName().getString(), payload.sourceSessionId(), payload.sourceSlotIndex());
            syncPlayerMenu(player);
            return;
        }
        if (!mayMutateInventory(player) || !sessions.allowOperation(player, "quick-move", 1.0D)) {
            DesktopDebug.trace("server quick move dropped player={} sourceSession={} reason=player-state", player.getName().getString(), payload.sourceSessionId());
            resyncGesture(player, sessions);
            return;
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
            runGestureMutation(player, sessions, "quick-move-result", () -> {
                clickQuickMoveMenu(player, sessions, source.menu, payload.sourceSlotIndex(), carriedBeforeQuickMove);
                broadcastGesture(player, sessions);
            });
            return;
        }

        if (payload.targetKind() == DesktopPackets.QUICK_TARGET_SESSION && payload.targetSessionId() != source.sessionId) {
            Session targetSession = sessions.sessions.get(payload.targetSessionId());
            if (quickMoveIntoTomStorageTerminal(player, sessions, source, targetSession)) {
                return;
            }
        }

        int nativeTargetSlot = payload.targetKind() == DesktopPackets.QUICK_TARGET_SESSION
            && isPlayerInventorySlot(player, source.slot)
            && DesktopMenuSlots.useNativeQuickMove(targetAuthorization.menu())
                ? physicalSlotIndexInMenu(source.menu, source.slot, targetAuthorization.menu())
                : -1;
        if (nativeTargetSlot >= 0) {
            runGestureMutation(player, sessions, "quick-move-native-target", () -> {
                clickQuickMoveMenu(player, sessions, targetAuthorization.menu(), nativeTargetSlot, carriedBeforeQuickMove);
                broadcastGesture(player, sessions);
            });
            return;
        }

        if (payload.targetKind() == DesktopPackets.QUICK_TARGET_DEFAULT
            && DesktopMenuSlots.useNativeQuickMove(source.menu)) {
            runGestureMutation(player, sessions, "quick-move-native", () -> {
                clickQuickMoveMenu(player, sessions, source.menu, payload.sourceSlotIndex(), carriedBeforeQuickMove);
                broadcastGesture(player, sessions);
            });
            return;
        }

        List<net.minecraft.world.inventory.Slot> targets = new ArrayList<>(quickMoveTargets(player, sessions, source, payload));
        targets.removeIf(slot -> DesktopItemSourceLocks.isSlotLocked(player, slot));
        if (targets.isEmpty()) {
            DesktopDebug.trace("server quick move dropped player={} sourceSession={} sourceSlot={} reason=no-targets", player.getName().getString(), payload.sourceSessionId(), payload.sourceSlotIndex());
            return;
        }

        boolean[] moved = {false};
        QuickMoveWorkBudget quickMoveWork = new QuickMoveWorkBudget(MAX_QUICK_MOVE_TRANSACTION_WORK);
        if (!runGestureMutation(player, sessions, "quick-move", () -> {
            moved[0] = moveSlotStack(
                player, sessions, source, targetAuthorization.menu(), targets, quickMoveWork
            );
            if (moved[0]) {
                broadcastGesture(player, sessions);
            }
        })) {
            return;
        }
        DesktopDebug.trace(
            "server quick move player={} sourceSession={} sourceSlot={} targetKind={} targetSession={} moved={}",
            player.getName().getString(),
            payload.sourceSessionId(),
            payload.sourceSlotIndex(),
            payload.targetKind(),
            payload.targetSessionId(),
            moved[0]
        );
        if (!moved[0]) {
            return;
        }
    }

    private static boolean mayMutateInventory(ServerPlayer player) {
        return player.isAlive() && !player.isSpectator();
    }

    private static @Nullable SessionAuthorization authorizeSession(
        ServerPlayer player,
        PlayerSessions sessions,
        long connectionNonce,
        int sessionId,
        long sessionToken,
        long capability,
        boolean requireVisible,
        String operation
    ) {
        long requiredCapabilities = capability | capabilityForSession(sessionId);
        if (!sessions.authorizes(connectionNonce, requiredCapabilities)) {
            DesktopDebug.trace("server {} dropped player={} reason=connection-auth", operation, player.getName().getString());
            return null;
        }
        if (sessionId == DesktopPackets.PLAYER_MENU_SESSION) {
            if (sessionToken != sessions.playerSessionToken) {
                DesktopDebug.trace("server {} dropped player={} reason=player-token", operation, player.getName().getString());
                return null;
            }
            return new SessionAuthorization(sessionId, player.inventoryMenu, null, PLAYER_LINK_NODE);
        }
        Session session = sessions.sessions.get(sessionId);
        if (session == null || session.sessionToken != sessionToken || (requireVisible && !session.visibleToClient)) {
            DesktopDebug.trace("server {} dropped player={} session={} reason=session-auth", operation, player.getName().getString(), sessionId);
            return null;
        }
        if (!session.menu.stillValid(player)) {
            sessions.close(player, sessionId, true);
            return null;
        }
        String linkNode = session.sourceKey.isBlank() ? "session:" + session.sessionId : session.sourceKey;
        return new SessionAuthorization(sessionId, session.menu, session, linkNode);
    }

    private static boolean preauthorizesSession(
        PlayerSessions sessions,
        long connectionNonce,
        int sessionId,
        long sessionToken,
        long capability,
        boolean requireVisible
    ) {
        long requiredCapabilities = capability | capabilityForSession(sessionId);
        if (!sessions.authorizes(connectionNonce, requiredCapabilities)) {
            return false;
        }
        if (sessionId == DesktopPackets.PLAYER_MENU_SESSION) {
            return sessionToken == sessions.playerSessionToken;
        }
        Session session = sessions.sessions.get(sessionId);
        return session != null
            && session.sessionToken == sessionToken
            && (!requireVisible || session.visibleToClient);
    }

    private static long capabilityForSession(int sessionId) {
        return sessionId == DesktopPackets.PLAYER_MENU_SESSION
            ? DesktopProtocol.CAP_INVENTORY_TOPOLOGY
            : DesktopProtocol.CAP_CUSTOM_WINDOWS;
    }

    private static @Nullable SessionAuthorization authorizeMenu(
        ServerPlayer player,
        PlayerSessions sessions,
        long connectionNonce,
        int sessionId,
        long sessionToken,
        int expectedStateId,
        long capability,
        String operation
    ) {
        SessionAuthorization authorization = authorizeSession(
            player, sessions, connectionNonce, sessionId, sessionToken, capability, true, operation
        );
        if (authorization == null) {
            return null;
        }
        if (authorization.menu().getStateId() != expectedStateId) {
            DesktopDebug.trace(
                "server {} dropped player={} session={} reason=state-id expected={} actual={}",
                operation, player.getName().getString(), sessionId, expectedStateId, authorization.menu().getStateId()
            );
            if (sessions.allowResync(player, operation)) {
                if (authorization.session() == null) {
                    syncPlayerMenu(player);
                } else {
                    withNetworkSession(player, authorization.session(), authorization.menu()::sendAllDataToRemote);
                }
                syncCarried(player, sessions);
            }
            return null;
        }
        return authorization;
    }

    private static void purchaseInventorySlot(ServerPlayer player, InventorySlotPurchasePayload payload) {
        PlayerSessions sessions = PLAYERS.get(player);
        if (sessions == null
            || authorizeMenu(
                player, sessions, payload.connectionNonce(), DesktopPackets.PLAYER_MENU_SESSION,
                payload.playerSessionToken(), payload.stateId(), DesktopProtocol.CAP_INVENTORY_TOPOLOGY,
                "inventory-slot-purchase"
            ) == null
            || !mayMutateInventory(player)
            || !sessions.allowOperation(player, "inventory-slot-purchase", 1.0D)) {
            DesktopDebug.trace("server inventory slot purchase dropped player={} reason=unauthorized", player.getName().getString());
            return;
        }
        InventoryExpansion.tryPurchase(player);
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
        if (!carriedBeforeQuickMove.isEmpty()) {
            setSharedCarried(player, sessions, ItemStack.EMPTY);
        }
        ItemStack before = targetSession.menu.slots.get(targetSlotIndex).getItem().copy();
        try {
            targetSession.menu.quickMoveStack(player, targetSlotIndex);
        } finally {
            if (!carriedBeforeQuickMove.isEmpty()) {
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

        withNetworkSession(player, targetSession, targetSession.menu::broadcastChanges);
        player.inventoryMenu.broadcastChanges();
        sessions.broadcastAll(player);
        syncCarried(player, sessions);
        return true;
    }

    private static boolean isVanillaResultSource(SlotSource source, int slotIndex) {
        if (source.menu instanceof AbstractCraftingMenu craftingMenu) {
            return source.slot == craftingMenu.getResultSlot();
        }
        if (source.menu instanceof AbstractFurnaceMenu) {
            return slotIndex == AbstractFurnaceMenu.RESULT_SLOT;
        }
        if (source.menu instanceof AnvilMenu) {
            return slotIndex == AnvilMenu.RESULT_SLOT;
        }
        if (source.menu instanceof CartographyTableMenu) {
            return slotIndex == CartographyTableMenu.RESULT_SLOT;
        }
        if (source.menu instanceof GrindstoneMenu) {
            return slotIndex == GrindstoneMenu.RESULT_SLOT;
        }
        if (source.menu instanceof MerchantMenu) {
            return slotIndex == MERCHANT_RESULT_SLOT;
        }
        if (source.menu instanceof SmithingMenu) {
            return slotIndex == SmithingMenu.RESULT_SLOT;
        }
        if (source.menu instanceof StonecutterMenu) {
            return slotIndex == StonecutterMenu.RESULT_SLOT;
        }
        return false;
    }

    private static void button(ServerPlayer player, DesktopButtonPayload payload) {
        PlayerSessions sessions = PLAYERS.get(player);
        if (sessions == null) {
            DesktopDebug.trace("server button dropped player={} session={} button={} reason=not-ready", player.getName().getString(), payload.sessionId(), payload.buttonId());
            return;
        }
        SessionAuthorization authorization = authorizeMenu(
            player, sessions, payload.connectionNonce(), payload.sessionId(), payload.sessionToken(),
            payload.stateId(), 0L, "button"
        );
        Session session = authorization == null ? null : authorization.session();
        if (session == null) {
            return;
        }
        if (!mayMutateInventory(player) || !sessions.allowOperation(player, "button", 1.0D)) {
            DesktopDebug.trace("server button dropped player={} session={} reason=player-state", player.getName().getString(), payload.sessionId());
            resyncGesture(player, sessions);
            return;
        }

        ItemStack carriedBefore = player.inventoryMenu.getCarried().copy();
        boolean committedCarried = false;
        boolean clicked;
        try {
            session.menu.setCarried(carriedBefore.copy());
            if (session.menu instanceof CrafterMenu crafterMenu) {
                int slotId = payload.buttonId() & ~CRAFTER_SLOT_STATE_ENABLED_FLAG;
                boolean enabled = (payload.buttonId() & CRAFTER_SLOT_STATE_ENABLED_FLAG) != 0;
                clicked = slotId >= 0 && slotId < CRAFTER_INPUT_SLOT_COUNT;
                if (clicked) {
                    Slot slot = crafterMenu.getSlot(slotId);
                    clicked = enabled || (!slot.hasItem() && crafterMenu.getCarried().isEmpty());
                    if (clicked) {
                        crafterMenu.setSlotState(slotId, enabled);
                    }
                }
            } else if (session.menu instanceof BeaconMenu beaconMenu) {
                clicked = applyBeaconButton(beaconMenu, payload.buttonId());
            } else if (session.menu instanceof MerchantMenu merchantMenu) {
                clicked = payload.buttonId() >= 0 && payload.buttonId() < merchantMenu.getOffers().size();
                if (clicked) {
                    merchantMenu.setSelectionHint(payload.buttonId());
                    merchantMenu.tryMoveItems(payload.buttonId());
                }
            } else {
                clicked = session.menu.clickMenuButton(player, payload.buttonId());
            }
            player.inventoryMenu.setCarried(session.menu.getCarried().copy());
            committedCarried = true;
        } catch (RuntimeException exception) {
            player.inventoryMenu.setCarried(carriedBefore.copy());
            throw exception;
        } finally {
            if (!committedCarried) {
                player.inventoryMenu.setCarried(carriedBefore.copy());
            }
            clearDetachedCarried(sessions);
        }

        DesktopDebug.trace(
            "server button player={} session={} button={} clicked={}",
            player.getName().getString(),
            payload.sessionId(),
            payload.buttonId(),
            clicked
        );
        if (clicked) {
            broadcastGesture(player, sessions);
        } else {
            resyncGesture(player, sessions);
        }
    }

    private static void placeRecipe(ServerPlayer player, DesktopPlaceRecipePayload payload) {
        PlayerSessions sessions = PLAYERS.get(player);
        if (sessions == null) {
            DesktopDebug.trace("server recipe place dropped player={} session={} recipe={} reason=not-ready", player.getName().getString(), payload.sessionId(), payload.recipeId());
            return;
        }
        SessionAuthorization authorization = authorizeMenu(
            player, sessions, payload.connectionNonce(), payload.sessionId(), payload.sessionToken(),
            payload.stateId(), DesktopProtocol.CAP_RECIPE_TRANSFER, "recipe-place"
        );
        Session session = authorization == null ? null : authorization.session();
        if (session == null) {
            return;
        }
        if (!mayMutateInventory(player) || !sessions.allowOperation(player, "recipe-place", 2.0D)) {
            DesktopDebug.trace("server recipe place dropped player={} session={} recipe={} reason=player-state", player.getName().getString(), payload.sessionId(), payload.recipeId());
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

        RecipeManager.ServerDisplayInfo displayInfo = server.getRecipeManager().getRecipeFromDisplay(payload.recipeId());
        if (displayInfo == null) {
            DesktopDebug.trace("server recipe place dropped player={} session={} recipe={} reason=missing-display", player.getName().getString(), payload.sessionId(), payload.recipeId());
            return;
        }

        RecipeHolder<?> recipe = displayInfo.parent();
        if (!player.getRecipeBook().contains(recipe.id())) {
            DesktopDebug.trace("server recipe place dropped player={} session={} recipe={} recipeKey={} reason=not-unlocked", player.getName().getString(), payload.sessionId(), payload.recipeId(), recipe.id());
            return;
        }
        if (recipe.value().placementInfo().isImpossibleToPlace()) {
            DesktopDebug.trace("server recipe place dropped player={} session={} recipe={} recipeKey={} reason=impossible", player.getName().getString(), payload.sessionId(), payload.recipeId(), recipe.id());
            return;
        }
        if (DesktopItemSourceLocks.anyLockedSourceMatches(
            player,
            stack -> recipe.value().placementInfo().ingredients().stream().anyMatch(ingredient -> ingredient.test(stack))
        )) {
            DesktopDebug.trace("server recipe place dropped player={} session={} recipe={} reason=locked-item-source", player.getName().getString(), payload.sessionId(), payload.recipeId());
            syncPlayerMenu(player);
            return;
        }

        ItemStack carriedBefore = player.inventoryMenu.getCarried().copy();
        boolean committedCarried = false;
        RecipeBookMenu.PostPlaceAction action;
        try {
            session.menu.setCarried(carriedBefore.copy());
            action = recipeBookMenu.handlePlacement(
                payload.useMaxItems(),
                player.isCreative(),
                recipe,
                player.level(),
                player.getInventory()
            );
            player.inventoryMenu.setCarried(session.menu.getCarried().copy());
            committedCarried = true;
        } catch (RuntimeException exception) {
            player.inventoryMenu.setCarried(carriedBefore.copy());
            throw exception;
        } finally {
            if (!committedCarried) {
                player.inventoryMenu.setCarried(carriedBefore.copy());
            }
            clearDetachedCarried(sessions);
        }

        DesktopDebug.trace(
            "server recipe place player={} session={} recipe={} recipeKey={} useMax={} action={} carried={}",
            player.getName().getString(),
            payload.sessionId(),
            payload.recipeId(),
            recipe.id(),
            payload.useMaxItems(),
            action,
            player.inventoryMenu.getCarried()
        );

        if (action == RecipeBookMenu.PostPlaceAction.PLACE_GHOST_RECIPE) {
            send(player, new DesktopGhostRecipePayload(session.sessionId, displayInfo.display().display()));
        }
        withNetworkSession(player, session, session.menu::broadcastChanges);
        player.inventoryMenu.broadcastChanges();
        sessions.broadcastAll(player);
        syncCarried(player, sessions);
    }

    private static void transferJeiRecipe(ServerPlayer player, DesktopJeiTransferPayload payload) {
        PlayerSessions sessions = PLAYERS.get(player);
        if (sessions == null) {
            DesktopDebug.trace("server JEI transfer dropped player={} targetSession={} reason=not-ready", player.getName().getString(), payload.targetSessionId());
            return;
        }
        SessionAuthorization authorization = authorizeMenu(
            player, sessions, payload.connectionNonce(), payload.targetSessionId(), payload.targetSessionToken(),
            payload.targetStateId(), DesktopProtocol.CAP_RECIPE_TRANSFER, "jei-transfer"
        );
        if (authorization == null) {
            return;
        }
        if (!mayMutateInventory(player) || !sessions.allowOperation(player, "jei-transfer", 8.0D)) {
            DesktopDebug.trace("server JEI transfer dropped player={} targetSession={} reason=player-state", player.getName().getString(), payload.targetSessionId());
            return;
        }
        if (!player.inventoryMenu.getCarried().isEmpty()) {
            DesktopDebug.trace("server JEI transfer dropped player={} targetSession={} reason=carried carried={}", player.getName().getString(), payload.targetSessionId(), player.inventoryMenu.getCarried());
            syncCarried(player, sessions);
            return;
        }

        MinecraftServer server = player.level().getServer();
        if (server == null) {
            return;
        }
        ResourceKey<Recipe<?>> recipeKey = ResourceKey.create(Registries.RECIPE, payload.recipeId());
        RecipeHolder<?> recipe = server.getRecipeManager().byKey(recipeKey).orElse(null);
        if (recipe == null || recipe.value().placementInfo().isImpossibleToPlace()) {
            DesktopDebug.trace("server JEI transfer dropped player={} recipe={} reason=unknown-recipe", player.getName().getString(), payload.recipeId());
            return;
        }
        if (DesktopItemSourceLocks.anyLockedSourceMatches(
            player,
            stack -> recipe.value().placementInfo().ingredients().stream().anyMatch(ingredient -> ingredient.test(stack))
        )) {
            DesktopDebug.trace("server JEI transfer dropped player={} targetSession={} recipe={} reason=locked-item-source", player.getName().getString(), payload.targetSessionId(), payload.recipeId());
            syncPlayerMenu(player);
            return;
        }

        JeiTransferTarget target = new JeiTransferTarget(payload.targetSessionId(), authorization.menu(), authorization.session());
        if (target.menu() instanceof AbstractCraftingMenu craftingMenu) {
            if (!(recipe.value() instanceof CraftingRecipe)) {
                DesktopDebug.trace("server JEI transfer dropped player={} targetSession={} recipe={} reason=not-crafting-recipe", player.getName().getString(), payload.targetSessionId(), payload.recipeId());
                return;
            }
            ItemStack carriedBefore = player.inventoryMenu.getCarried().copy();
            boolean committedCarried = false;
            RecipeBookMenu.PostPlaceAction action;
            try {
                target.menu().setCarried(carriedBefore.copy());
                action = craftingMenu.handlePlacement(
                    payload.maxTransfer(),
                    player.isCreative(),
                    recipe,
                    player.level(),
                    player.getInventory()
                );
                player.inventoryMenu.setCarried(target.menu().getCarried().copy());
                committedCarried = true;
            } catch (RuntimeException exception) {
                player.inventoryMenu.setCarried(carriedBefore.copy());
                throw exception;
            } finally {
                if (!committedCarried) {
                    player.inventoryMenu.setCarried(carriedBefore.copy());
                }
                clearDetachedCarried(sessions);
            }
            if (action == RecipeBookMenu.PostPlaceAction.PLACE_GHOST_RECIPE && !recipe.value().display().isEmpty()) {
                send(player, new DesktopGhostRecipePayload(payload.targetSessionId(), recipe.value().display().getFirst()));
            }
            withMenuNetworkSession(player, sessions, target.menu(), target.menu()::broadcastChanges);
            player.inventoryMenu.broadcastChanges();
            sessions.broadcastAll(player);
            syncCarried(player, sessions);
            return;
        }

        Session session = target.session();
        if (session == null || session.serverHandler == null || !DesktopTransferValidators.supports(session.serverHandler)) {
            DesktopDebug.trace("server JEI transfer dropped player={} targetSession={} reason=unsupported-custom-menu", player.getName().getString(), payload.targetSessionId());
            return;
        }
        DesktopTransferDecision decision;
        sessions.beginHandlerCallback();
        try {
            decision = DesktopTransferValidators.validate(
                session.serverHandler,
                new DesktopTransferRequest<>(new ServerSessionContext(player, sessions, session), recipe.value(), payload.maxTransfer())
            );
        } catch (RuntimeException exception) {
            session.quarantineServerHandler(player, "transfer-validation", exception);
            // The validator receives a live session context and may have requested or performed a
            // mutation before failing. Let the outer mutation boundary recover and resync before ACK.
            throw exception;
        } finally {
            sessions.endHandlerCallback(player);
        }
        if (!decision.allowed()) {
            return;
        }
        List<Slot> recipeSlots = resolveJeiTransferRecipeSlots(target.menu(), decision.destinationSlots());
        List<JeiTransferRequirement> requirements = resolveCustomTransferRequirements(target.menu(), decision.requirements());
        int maximumCrafts = decision.maximumCrafts();

        if (recipeSlots == null || recipeSlots.isEmpty() || requirements == null || requirements.isEmpty()) {
            DesktopDebug.trace("server JEI transfer dropped player={} targetSession={} reason=bad-server-transfer-plan", player.getName().getString(), payload.targetSessionId());
            return;
        }

        Set<Slot> recipeSlotSet = new HashSet<>();
        for (Slot slot : recipeSlots) {
            recipeSlotSet.add(slot);
        }

        List<Slot> sourceSlots = jeiTransferSourceSlots(player, sessions, payload.targetSessionId(), recipeSlotSet);
        JeiTransferSimulation simulation = simulateJeiTransfer(player, recipeSlots, requirements, sourceSlots, payload.maxTransfer(), maximumCrafts);
        if (simulation == null) {
            DesktopDebug.trace("server JEI transfer dropped player={} targetSession={} reason=simulation-failed", player.getName().getString(), payload.targetSessionId());
            return;
        }

        applyJeiTransferSimulation(simulation);
        withMenuNetworkSession(player, sessions, target.menu(), target.menu()::broadcastChanges);
        if (target.session() == null) {
            player.inventoryMenu.broadcastChanges();
        }
        sessions.broadcastAll(player);
        syncCarried(player, sessions);
        DesktopDebug.trace(
            "server JEI transfer player={} targetSession={} recipeSlots={} requirements={} sources={} max={}",
            player.getName().getString(),
            payload.targetSessionId(),
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
            if (!slot.isActive() || slot.isFake()) {
                return null;
            }
            slots.add(slot);
        }
        return slots;
    }

    private static @Nullable List<JeiTransferRequirement> resolveCustomTransferRequirements(
        AbstractContainerMenu menu,
        List<com.salts_inventory_update.api.server.desktop.DesktopTransferRequirement> approvedRequirements
    ) {
        List<JeiTransferRequirement> requirements = new ArrayList<>(approvedRequirements.size());
        Set<Integer> targetSlots = new HashSet<>();
        int inputIndex = 0;
        for (com.salts_inventory_update.api.server.desktop.DesktopTransferRequirement approved : approvedRequirements) {
            int targetSlotId = approved.targetSlotId();
            if (targetSlotId < 0 || targetSlotId >= menu.slots.size() || !targetSlots.add(targetSlotId)) {
                return null;
            }
            Slot target = menu.slots.get(targetSlotId);
            List<ItemStack> alternatives = approved.alternatives().stream()
                .filter(stack -> !stack.isEmpty() && target.mayPlace(stack))
                .map(stack -> stack.copyWithCount(approved.count()))
                .toList();
            if (alternatives.isEmpty()) {
                return null;
            }
            requirements.add(new JeiTransferRequirement(inputIndex++, target, alternatives));
        }
        return requirements;
    }

    private static List<Slot> jeiTransferSourceSlots(ServerPlayer player, PlayerSessions sessions, int targetSessionId, Set<Slot> targetRecipeSlots) {
        Map<JeiTransferSourceKey, Slot> slots = new LinkedHashMap<>();
        for (Slot slot : player.inventoryMenu.slots) {
            addJeiTransferSourceSlot(player, slots, slot, DesktopPackets.PLAYER_MENU_SESSION, targetSessionId, targetRecipeSlots);
        }
        for (Session session : sessions.sessions.values()) {
            if (!session.visibleToClient || !session.menu.stillValid(player)) {
                continue;
            }
            for (Slot slot : session.menu.slots) {
                addJeiTransferSourceSlot(player, slots, slot, session.sessionId, targetSessionId, targetRecipeSlots);
            }
        }
        return new ArrayList<>(slots.values());
    }

    private static void addJeiTransferSourceSlot(ServerPlayer player, Map<JeiTransferSourceKey, Slot> slots, Slot slot, int sourceSessionId, int targetSessionId, Set<Slot> targetRecipeSlots) {
        if (sourceSessionId == targetSessionId && targetRecipeSlots.contains(slot)) {
            return;
        }
        if (!isJeiTransferSourceSlot(player, slot)) {
            return;
        }
        if (DesktopItemSourceLocks.isSlotLocked(player, slot)) {
            return;
        }
        slots.putIfAbsent(new JeiTransferSourceKey(slot.container, slot.getContainerSlot()), slot);
    }

    private static boolean isJeiTransferSourceSlot(ServerPlayer player, Slot slot) {
        if (!slot.isActive() || slot.isFake() || !slot.hasItem() || !slot.mayPickup(player)) {
            return false;
        }
        if (slot.container == player.getInventory()) {
            int containerSlot = slot.getContainerSlot();
            return containerSlot >= 0 && containerSlot < net.minecraft.world.entity.player.Inventory.INVENTORY_SIZE;
        }
        return slot.mayPlace(slot.getItem());
    }

    private static @Nullable JeiTransferSimulation simulateJeiTransfer(ServerPlayer player, List<Slot> recipeSlots, List<JeiTransferRequirement> requirements, List<Slot> sourceSlots, boolean maxTransfer, int maximumCrafts) {
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

        Map<StackVariant, Integer> supply = new LinkedHashMap<>();
        for (ItemStack stack : sourceStacks.values()) {
            if (!stack.isEmpty()) {
                try {
                    supply.merge(new StackVariant(stack), stack.getCount(), Math::addExact);
                } catch (ArithmeticException exception) {
                    return null;
                }
            }
        }

        int craftLimit = Math.max(1, Math.min(maximumCrafts, DesktopProtocol.MAX_TRANSFER_CRAFTS));
        List<BoundedTransferPlanner.Requirement<StackVariant>> plannerRequirements = new ArrayList<>(requirements.size());
        for (JeiTransferRequirement requirement : requirements) {
            int unitsPerCraft = requirement.alternatives().getFirst().getCount();
            if (unitsPerCraft <= 0 || requirement.alternatives().stream().anyMatch(stack -> stack.getCount() != unitsPerCraft)) {
                return null;
            }
            List<StackVariant> alternatives = requirement.alternatives().stream().map(StackVariant::new).distinct().toList();
            ItemStack existing = targetStacks.getOrDefault(requirement.targetSlot(), ItemStack.EMPTY);
            List<StackVariant> compatibleAlternatives = existing.isEmpty()
                ? alternatives
                : alternatives.stream().filter(variant -> variant.matches(existing)).toList();
            if (compatibleAlternatives.isEmpty()) {
                return null;
            }
            Map<StackVariant, Integer> maximumUnits = new LinkedHashMap<>();
            for (StackVariant alternative : compatibleAlternatives) {
                ItemStack stack = alternative.stack();
                maximumUnits.put(
                    alternative,
                    Math.min(stack.getMaxStackSize(), requirement.targetSlot().getMaxStackSize(stack))
                );
            }
            plannerRequirements.add(new BoundedTransferPlanner.Requirement<>(
                requirement.targetSlot().index,
                unitsPerCraft,
                existing.getCount(),
                compatibleAlternatives,
                maximumUnits
            ));
        }

        Optional<BoundedTransferPlanner.Plan<StackVariant>> planned;
        try {
            BoundedTransferPlanner<StackVariant> planner = new BoundedTransferPlanner<>(StackVariant.COMPARATOR);
            planned = maxTransfer
                ? planner.planMaximum(supply, plannerRequirements, craftLimit)
                : planner.planExact(supply, plannerRequirements, 1);
        } catch (IllegalArgumentException | ArithmeticException exception) {
            return null;
        }
        if (planned.isEmpty()) {
            return null;
        }

        Map<Integer, JeiTransferRequirement> requirementsByMenuSlot = new HashMap<>();
        for (JeiTransferRequirement requirement : requirements) {
            requirementsByMenuSlot.put(requirement.targetSlot().index, requirement);
        }
        Map<Slot, ItemStack> plannedSources = copyJeiTransferStacks(sourceStacks);
        Map<Slot, ItemStack> plannedTargets = copyJeiTransferStacks(targetStacks);
        for (BoundedTransferPlanner.Allocation<StackVariant> allocation : planned.get().allocations()) {
            if (allocation.units().size() != 1) {
                return null;
            }
            JeiTransferRequirement requirement = requirementsByMenuSlot.get(allocation.targetId());
            if (requirement == null) {
                return null;
            }
            Map.Entry<StackVariant, Integer> selected = allocation.units().entrySet().iterator().next();
            if (!consumeVariant(plannedSources, selected.getKey(), selected.getValue())) {
                return null;
            }
            ItemStack existing = plannedTargets.getOrDefault(requirement.targetSlot(), ItemStack.EMPTY);
            ItemStack selectedStack = selected.getKey().stack();
            int limit = Math.min(selectedStack.getMaxStackSize(), requirement.targetSlot().getMaxStackSize(selectedStack));
            if (!existing.isEmpty() && !ItemStack.isSameItemSameComponents(existing, selectedStack)
                || existing.getCount() + selected.getValue() > limit) {
                return null;
            }
            if (existing.isEmpty()) {
                plannedTargets.put(requirement.targetSlot(), selectedStack.copyWithCount(selected.getValue()));
            } else {
                existing.grow(selected.getValue());
            }
        }
        return new JeiTransferSimulation(plannedSources, plannedTargets);
    }

    private static boolean consumeVariant(Map<Slot, ItemStack> sourceStacks, StackVariant variant, int amount) {
        if (amount <= 0) {
            return amount == 0;
        }
        int remaining = amount;
        for (ItemStack stack : sourceStacks.values()) {
            if (stack.isEmpty() || !variant.matches(stack)) {
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
            if (ItemStack.isSameItemSameComponents(stack, alternative)) {
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
                if (existing.isEmpty() || !ItemStack.isSameItemSameComponents(existing, moving) || !slot.mayPlace(moving)) {
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

    private static Map<Slot, ItemStack> copyJeiTransferStacks(Map<Slot, ItemStack> stacks) {
        Map<Slot, ItemStack> copy = new LinkedHashMap<>();
        for (Map.Entry<Slot, ItemStack> entry : stacks.entrySet()) {
            copy.put(entry.getKey(), entry.getValue().copy());
        }
        return copy;
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
        ItemStack before = current.copy();
        slot.setByPlayer(stack.copy(), before);
        slot.setChanged();
    }

    private static void rename(ServerPlayer player, DesktopRenamePayload payload) {
        PlayerSessions sessions = PLAYERS.get(player);
        if (sessions == null) {
            DesktopDebug.trace("server rename dropped player={} session={} reason=not-ready", player.getName().getString(), payload.sessionId());
            return;
        }
        SessionAuthorization authorization = authorizeMenu(
            player, sessions, payload.connectionNonce(), payload.sessionId(), payload.sessionToken(),
            payload.stateId(), 0L, "rename"
        );
        Session session = authorization == null ? null : authorization.session();
        if (session == null) {
            return;
        }
        if (!mayMutateInventory(player) || !sessions.allowOperation(player, "rename", 1.0D)) {
            DesktopDebug.trace("server rename dropped player={} session={} reason=player-state", player.getName().getString(), payload.sessionId());
            resyncGesture(player, sessions);
            return;
        }

        if (!(session.menu instanceof AnvilMenu anvilMenu)) {
            DesktopDebug.trace("server rename dropped player={} session={} reason=not-anvil", player.getName().getString(), payload.sessionId());
            resyncGesture(player, sessions);
            return;
        }

        boolean changed = anvilMenu.setItemName(payload.name());
        DesktopDebug.trace("server rename player={} session={} changed={} name={}", player.getName().getString(), payload.sessionId(), changed, payload.name());
        if (changed) {
            broadcastGesture(player, sessions);
        } else {
            resyncGesture(player, sessions);
        }
    }

    private static void customPayload(ServerPlayer player, DesktopCustomPayload payload) {
        PlayerSessions sessions = PLAYERS.get(player);
        if (sessions == null) {
            DesktopDebug.trace("server custom dropped player={} session={} channel={} reason=not-ready", player.getName().getString(), payload.sessionId(), payload.channel());
            return;
        }
        SessionAuthorization authorization = authorizeMenu(
            player, sessions, payload.connectionNonce(), payload.sessionId(), payload.sessionToken(),
            payload.stateId(), DesktopProtocol.CAP_CUSTOM_WINDOWS, "custom"
        );
        Session session = authorization == null ? null : authorization.session();
        if (session == null) {
            return;
        }
        if (!mayMutateInventory(player) || !sessions.allowOperation(player, "custom", 2.0D)) {
            DesktopDebug.trace("server custom dropped player={} session={} reason=player-state", player.getName().getString(), payload.sessionId());
            resyncGesture(player, sessions);
            return;
        }

        @Nullable MenuType<?> menuType = session.menuType();
        if (menuType == null) {
            DesktopDebug.trace("server custom dropped player={} session={} channel={} reason=no-menu-type", player.getName().getString(), payload.sessionId(), payload.channel());
            resyncGesture(player, sessions);
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
            resyncGesture(player, sessions);
            return;
        }

        DesktopDebug.trace(
            "server custom player={} session={} channel={} bytes={}",
            player.getName().getString(),
            payload.sessionId(),
            payload.channel(),
            payload.data().length
        );
        sessions.beginHandlerCallback();
        try {
            withNetworkSession(player, session, () -> handler.handle(new ServerPayloadContext(player, sessions, session, payload)));
        } catch (RuntimeException exception) {
            throw exception;
        } finally {
            sessions.endHandlerCallback(player);
        }
    }

    private static boolean applyBeaconButton(BeaconMenu menu, int buttonId) {
        int primaryId = buttonId & BEACON_EFFECT_ID_MASK;
        int secondaryId = buttonId >>> BEACON_SECONDARY_EFFECT_SHIFT & BEACON_EFFECT_ID_MASK;
        Holder<MobEffect> primary = BeaconMenu.decodeEffect(primaryId);
        Holder<MobEffect> secondary = BeaconMenu.decodeEffect(secondaryId);
        if (!menu.hasPayment() || !canSelectBeaconPrimary(menu, primary) || !canSelectBeaconSecondary(menu, primary, secondary)) {
            return false;
        }

        menu.updateEffects(Optional.of(primary), Optional.ofNullable(secondary));
        return true;
    }

    private static boolean canSelectBeaconPrimary(BeaconMenu menu, @Nullable Holder<MobEffect> effect) {
        if (effect == null) {
            return false;
        }

        int unlockedTiers = Math.min(menu.getLevels(), Math.min(3, BeaconBlockEntity.BEACON_EFFECTS.size()));
        for (int tier = 0; tier < unlockedTiers; tier++) {
            if (BeaconBlockEntity.BEACON_EFFECTS.get(tier).contains(effect)) {
                return true;
            }
        }
        return false;
    }

    private static boolean canSelectBeaconSecondary(BeaconMenu menu, Holder<MobEffect> primary, @Nullable Holder<MobEffect> secondary) {
        if (secondary == null) {
            return true;
        }
        if (menu.getLevels() < 4) {
            return false;
        }
        if (primary.equals(secondary)) {
            return true;
        }
        return BeaconBlockEntity.BEACON_EFFECTS.size() > 3 && BeaconBlockEntity.BEACON_EFFECTS.get(3).contains(secondary);
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
        return quickMoveTargets(player, sessions, source, payload.targetKind(), payload.targetSessionId());
    }

    private static List<net.minecraft.world.inventory.Slot> quickMoveTargets(
        ServerPlayer player,
        PlayerSessions sessions,
        SlotSource source,
        int targetKind,
        int targetSessionId
    ) {
        if (targetKind == DesktopPackets.QUICK_TARGET_SESSION) {
            Session targetSession = sessions.sessions.get(targetSessionId);
            if (targetSession != null && targetSession.visibleToClient && targetSession.menu.stillValid(player)) {
                return containerSlots(targetSession.menu, player);
            }
            return List.of();
        }

        if (targetKind == DesktopPackets.QUICK_TARGET_HOTBAR && !isPlayerInventorySlot(player, source.slot)) {
            return hotbarSlots(player);
        }

        return defaultPlayerTargets(player, source.slot);
    }

    /**
     * A menu may either opt a slot into Salt's explicit destination routing, or retain its native
     * DEFAULT quick-move implementation. Slots which opt into neither (notably settings/memory
     * displays) are never fed through the generic transfer path.
     */
    private static boolean allowsQuickMoveRouting(AbstractContainerMenu menu, Slot slot, int targetKind) {
        return DesktopMenuSlots.allowsTargetedQuickMoveSource(menu, slot)
            || targetKind == DesktopPackets.QUICK_TARGET_DEFAULT && DesktopMenuSlots.useNativeQuickMove(menu);
    }

    /** Finds the target menu's logical slot that wraps the exact same physical source slot. */
    private static int physicalSlotIndexInMenu(
        AbstractContainerMenu sourceMenu,
        Slot sourceSlot,
        AbstractContainerMenu targetMenu
    ) {
        int size = DesktopMenuSlots.size(targetMenu);
        for (int slotIndex = 0; slotIndex < size; slotIndex++) {
            Slot candidate = DesktopMenuSlots.slot(targetMenu, slotIndex);
            if (candidate != null
                && DesktopMenuSlots.isSamePhysicalSlot(sourceMenu, sourceSlot, targetMenu, candidate)) {
                return slotIndex;
            }
        }
        return -1;
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
        ServerPlayer player,
        PlayerSessions sessions,
        SlotSource source,
        AbstractContainerMenu targetMenu,
        List<net.minecraft.world.inventory.Slot> targets,
        QuickMoveWorkBudget workBudget
    ) {
        return moveSlotStack(player, sessions, source, targetMenu, targets, workBudget, false);
    }

    private static boolean moveSlotStack(
        ServerPlayer player, PlayerSessions sessions, SlotSource source, AbstractContainerMenu targetMenu,
        List<Slot> targets, QuickMoveWorkBudget workBudget, boolean sortExtraction
    ) {
        Slot sourceSlot = source.slot;
        if (!sourceSlot.isActive() || sourceSlot.isFake() || !sourceSlot.hasItem() || !sourceSlot.mayPickup(player)) {
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
            if (!target.isActive() || target.isFake() || !target.hasItem()) {
                continue;
            }
            if (!ItemStack.isSameItemSameComponents(moving, target.getItem())) {
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
            if (!target.isActive() || target.isFake() || target.hasItem()) {
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
            || source.slot.isFake()
            || !source.slot.hasItem()
            || !source.slot.mayPickup(player)
            || !ItemStack.matches(sourceBefore, moving)) {
            throw new IllegalStateException("Quick-move source changed or became ineligible before target commit");
        }
        if (DesktopItemSourceLocks.isSlotLocked(player, target)
            || !target.isActive()
            || target.isFake()) {
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
            || !remainder.isEmpty() && !ItemStack.isSameItemSameComponents(sourceBefore, remainder)
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
            source.slot.setByPlayer(sourceAfter.copy(), sourceBefore.copy())
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
        if (current.isEmpty() || !ItemStack.isSameItemSameComponents(insertedType, current)) {
            return -1;
        }
        if (before.isEmpty()) {
            return current.getCount();
        }
        if (!ItemStack.isSameItemSameComponents(before, current) || current.getCount() < before.getCount()) {
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
            slot.setByPlayer(snapshot.copy(), replaced)
        );
        if (!ItemStack.matches(snapshot, slot.getItem())) {
            throw new IllegalStateException(description + " rollback could not be verified");
        }
    }

    private static List<net.minecraft.world.inventory.Slot> containerSlots(AbstractContainerMenu menu, ServerPlayer player) {
        List<net.minecraft.world.inventory.Slot> slots = new ArrayList<>();
        for (net.minecraft.world.inventory.Slot slot : DesktopMenuSlots.all(menu)) {
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

    private static void clickMenu(int debugId, ServerPlayer player, PlayerSessions sessions, AbstractContainerMenu menu, int slotIndex, int button, ContainerInput input, ItemStack clientCarried) {
        if (!mayMutateInventory(player) || !menu.stillValid(player)) {
            DesktopDebug.trace("server click ignored id={} player={} menu={} reason=invalid-player-or-menu", debugId, player.getName().getString(), menu.containerId);
            return;
        }
        if (slotIndex != AbstractContainerMenu.SLOT_CLICKED_OUTSIDE
            && DesktopMenuSlots.slot(menu, slotIndex) == null) {
            DesktopDebug.trace("server click ignored id={} player={} menu={} slot={} reason=out-of-range", debugId, player.getName().getString(), menu.containerId, slotIndex);
            return;
        }

        ItemStack slotBefore = serverSlotStack(menu, slotIndex);
        ItemStack carriedBefore = player.inventoryMenu.getCarried().copy();
        ItemStack menuCarriedBefore = menu.getCarried().copy();
        ItemStack effectiveCarried = player.hasInfiniteMaterials() ? clientCarried.copy() : player.inventoryMenu.getCarried().copy();
        if (DesktopItemSourceLocks.shouldRejectClick(player, menu, slotIndex, button, input, effectiveCarried)) {
            DesktopDebug.trace(
                "server click ignored id={} player={} menu={} slot={} button={} input={} reason=locked-item-source",
                debugId,
                player.getName().getString(),
                menu.containerId,
                slotIndex,
                button,
                input
            );
            syncPlayerMenu(player);
            syncCarried(player, sessions);
            return;
        }
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
            player.hasInfiniteMaterials()
        );
        boolean committedCarried = false;
        try {
            menu.setCarried(effectiveCarried);
            withMenuNetworkSession(player, sessions, menu, () -> menu.clicked(slotIndex, button, input, player));
            player.inventoryMenu.setCarried(menu.getCarried().copy());
            committedCarried = true;
        } catch (RuntimeException exception) {
            player.inventoryMenu.setCarried(carriedBefore.copy());
            throw exception;
        } finally {
            if (!committedCarried) {
                player.inventoryMenu.setCarried(carriedBefore.copy());
            }
            clearDetachedCarried(sessions);
        }
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
    }

    private static ItemStack serverSlotStack(AbstractContainerMenu menu, int slotIndex) {
        Slot slot = DesktopMenuSlots.slot(menu, slotIndex);
        if (slot == null) {
            return ItemStack.EMPTY;
        }

        return slot.getItem().copy();
    }

    private static void syncCarried(ServerPlayer player, PlayerSessions sessions) {
        ItemStack carried = player.inventoryMenu.getCarried().copy();
        DesktopDebug.trace("server sync carried player={} stack={}", player.getName().getString(), carried);
        send(player, new DesktopCarriedPayload(
            sessions.connectionNonce, 0L, sessions.playerSessionToken, player.inventoryMenu.getStateId(), carried
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
        clearDetachedCarried(sessions);
    }

    private static void clearDetachedCarried(PlayerSessions sessions) {
        for (Session session : sessions.sessions.values()) {
            session.menu.setCarried(ItemStack.EMPTY);
        }
    }

    public static void syncCraftingResult(ServerPlayer player, CraftingMenu menu) {
        PlayerSessions sessions = PLAYERS.get(player);
        if (sessions == null || !sessions.isActive() || !sessions.hasCapability(DesktopProtocol.CAP_CUSTOM_WINDOWS)) {
            return;
        }

        for (Session session : sessions.sessions.values()) {
            if (session.menu == menu && session.visibleToClient) {
                syncCraftingResultSlot(player, session);
                return;
            }
        }
    }

    private static void syncCraftingResultSlot(ServerPlayer player, Session session) {
        if (!(session.menu instanceof AbstractCraftingMenu craftingMenu)) {
            return;
        }

        Slot resultSlot = craftingMenu.getResultSlot();
        int slotIndex = session.menu.slots.indexOf(resultSlot);
        if (slotIndex < 0) {
            return;
        }

        send(player, new DesktopSlotPayload(
            session.sessionId,
            slotIndex,
            session.menu.getStateId(),
            resultSlot.getItem().copy()
        ));
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

    private static void closeSession(ServerPlayer player, DesktopCloseSessionPayload payload) {
        PlayerSessions sessions = PLAYERS.get(player);
        if (sessions == null
            || !preauthorizesSession(sessions, payload.connectionNonce(), payload.sessionId(), payload.sessionToken(), 0L, false)
            || !sessions.allowControl(player, "close", 1.0D)
            || authorizeSession(
                player, sessions, payload.connectionNonce(), payload.sessionId(), payload.sessionToken(), 0L, false, "close"
            ) == null) {
            return;
        }
        DesktopDebug.log("server close request player={} session={}", player.getName().getString(), payload.sessionId());
        sessions.close(player, payload.sessionId(), true);
    }

    private static void setSessionPin(ServerPlayer player, DesktopSessionPinPayload payload) {
        PlayerSessions sessions = PLAYERS.get(player);
        if (sessions == null) {
            DesktopDebug.trace("server pin dropped player={} session={} reason=not-ready", player.getName().getString(), payload.sessionId());
            return;
        }

        if (payload.pinMode() < DesktopPackets.PIN_MODE_UNPINNED
            || payload.pinMode() > DesktopPackets.PIN_MODE_GHOST_PINNED
            || !preauthorizesSession(sessions, payload.connectionNonce(), payload.sessionId(), payload.sessionToken(), 0L, false)
            || !sessions.allowControl(player, "pin", 1.0D)) {
            return;
        }
        SessionAuthorization authorization = authorizeSession(
            player, sessions, payload.connectionNonce(), payload.sessionId(), payload.sessionToken(), 0L, false, "pin"
        );
        Session session = authorization == null ? null : authorization.session();
        if (session == null) {
            return;
        }

        session.ghostPinned = payload.pinMode() == DesktopPackets.PIN_MODE_GHOST_PINNED;
        DesktopDebug.trace("server pin player={} session={} ghostPinned={}", player.getName().getString(), payload.sessionId(), session.ghostPinned);
        session.dispatchPinChanged(player, sessions);
    }

    private static void setSessionVisibility(ServerPlayer player, DesktopSessionVisibilityPayload payload) {
        PlayerSessions sessions = PLAYERS.get(player);
        if (sessions == null) {
            DesktopDebug.trace("server visibility dropped player={} session={} reason=not-ready", player.getName().getString(), payload.sessionId());
            return;
        }

        if (!preauthorizesSession(sessions, payload.connectionNonce(), payload.sessionId(), payload.sessionToken(), 0L, false)
            || !sessions.allowControl(player, "visibility", 1.0D)) {
            return;
        }
        SessionAuthorization authorization = authorizeSession(
            player, sessions, payload.connectionNonce(), payload.sessionId(), payload.sessionToken(), 0L, false, "visibility"
        );
        Session session = authorization == null ? null : authorization.session();
        if (session == null) {
            return;
        }
        sessions.setVisible(player, session, payload.visible(), true);
    }

    private static void openLinkedSources(ServerPlayer player, DesktopOpenLinkedSourcesPayload payload) {
        PlayerSessions sessions = PLAYERS.get(player);
        if (sessions == null) {
            DesktopDebug.trace("server linked open dropped player={} reason=not-ready", player.getName().getString());
            return;
        }
        if (!preauthorizesSession(
            sessions, payload.connectionNonce(), payload.originSessionId(), payload.originSessionToken(),
            DesktopProtocol.CAP_LINK_GRAPH | DesktopProtocol.CAP_CUSTOM_WINDOWS, false
        ) || !sessions.allowControl(player, "open-linked", 2.0D)) {
            return;
        }
        SessionAuthorization origin = authorizeSession(
            player, sessions, payload.connectionNonce(), payload.originSessionId(), payload.originSessionToken(),
            DesktopProtocol.CAP_LINK_GRAPH | DesktopProtocol.CAP_CUSTOM_WINDOWS, false, "open-linked"
        );
        if (origin == null) {
            return;
        }

        for (String sourceKey : sessions.links.connectedComponent(origin.linkNode(), MAX_DORMANT_GHOST_SOURCES)) {
            if (sourceKey == null || sourceKey.isBlank() || !isBlockBackedSourceKey(sourceKey)) {
                DesktopDebug.trace("server linked open skipped player={} source={} reason=unsupported-source", player.getName().getString(), sourceKey);
                continue;
            }

            Session existing = sessions.sessionForSourceKey(sourceKey);
            if (existing != null) {
                if (!existing.visibleToClient) {
                    sessions.setVisible(player, existing, true, true);
                }
                continue;
            }

            SourceIdentity sourceIdentity = sessions.sourceIdentities.get(sourceKey);
            if (sourceIdentity == null) {
                DesktopDebug.trace("server linked open skipped player={} source={} reason=missing-source-identity", player.getName().getString(), sourceKey);
                continue;
            }
            MenuProvider provider = providerForDormantGhost(player, sourceKey, sourceIdentity);
            if (provider == null) {
                DesktopDebug.trace("server linked open skipped player={} source={} reason=unavailable", player.getName().getString(), sourceKey);
                continue;
            }

            DesktopDebug.log("server linked open player={} source={} title={}", player.getName().getString(), sourceKey, provider.getDisplayName().getString());
            openMenuSession(player, provider, sourceKey, false, false, true, null);
        }
    }

    private static void updateLink(ServerPlayer player, DesktopLinkPayload payload) {
        PlayerSessions sessions = PLAYERS.get(player);
        if (sessions == null) {
            return;
        }
        if (!preauthorizesSession(
            sessions, payload.connectionNonce(), payload.firstSessionId(), payload.firstSessionToken(),
            DesktopProtocol.CAP_LINK_GRAPH, false
        ) || !preauthorizesSession(
            sessions, payload.connectionNonce(), payload.secondSessionId(), payload.secondSessionToken(),
            DesktopProtocol.CAP_LINK_GRAPH, false
        ) || !sessions.allowControl(player, "link", 2.0D)) {
            return;
        }
        SessionAuthorization first = authorizeSession(
            player, sessions, payload.connectionNonce(), payload.firstSessionId(), payload.firstSessionToken(),
            DesktopProtocol.CAP_LINK_GRAPH, false, "link-first"
        );
        if (first == null) {
            return;
        }
        if (payload.action() == DesktopLinkPayload.ACTION_DETACH) {
            if (!first.linkNode().equals(secondLinkNodeCandidate(sessions, payload))) {
                SessionAuthorization second = authorizeSession(
                    player, sessions, payload.connectionNonce(), payload.secondSessionId(), payload.secondSessionToken(),
                    DesktopProtocol.CAP_LINK_GRAPH, false, "unlink-second"
                );
                if (second != null) {
                    sessions.links.unlink(first.linkNode(), second.linkNode());
                }
                return;
            }
            Set<String> neighbors = sessions.links.snapshot().get(first.linkNode());
            if (neighbors != null) {
                for (String neighbor : List.copyOf(neighbors)) {
                    sessions.links.unlink(first.linkNode(), neighbor);
                }
            }
            return;
        }
        SessionAuthorization second = authorizeSession(
            player, sessions, payload.connectionNonce(), payload.secondSessionId(), payload.secondSessionToken(),
            DesktopProtocol.CAP_LINK_GRAPH, false, "link-second"
        );
        if (second != null) {
            sessions.links.link(first.linkNode(), second.linkNode());
        }
    }

    private static String secondLinkNodeCandidate(PlayerSessions sessions, DesktopLinkPayload payload) {
        if (payload.secondSessionId() == DesktopPackets.PLAYER_MENU_SESSION
            && payload.secondSessionToken() == sessions.playerSessionToken) {
            return PLAYER_LINK_NODE;
        }
        Session second = sessions.sessions.get(payload.secondSessionId());
        if (second == null || second.sessionToken != payload.secondSessionToken()) {
            return "";
        }
        return second.sourceKey.isBlank() ? "session:" + second.sessionId : second.sourceKey;
    }

    private static int nextSessionId(ServerPlayer player) {
        PlayerSessions sessions = sessions(player);
        for (int attempt = 0; attempt <= MAX_SESSIONS; attempt++) {
            int candidate = sessions.nextSessionId <= 0 ? 1 : sessions.nextSessionId;
            sessions.nextSessionId = candidate == Integer.MAX_VALUE ? 1 : candidate + 1;
            if (!sessions.sessions.containsKey(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("No free desktop session identifier");
    }

    private static PlayerSessions sessions(ServerPlayer player) {
        return PLAYERS.computeIfAbsent(player, ignored -> new PlayerSessions());
    }

    private static void send(ServerPlayer player, CustomPacketPayload payload) {
        boolean canSend = ServerPlayNetworking.canSend(player, payload.type());
        if (payload instanceof DesktopOpenSessionPayload openPayload && isCamelOrLlamaSpecial(openPayload.specialKind())) {
            mountDiag(
                "server_send_open player={} session={} special={} entityId={} columns={} visible={} source={} items={} data={} canSend={}",
                player.getName().getString(),
                openPayload.sessionId(),
                openPayload.specialKind(),
                openPayload.entityId(),
                openPayload.columns(),
                openPayload.visible(),
                openPayload.sourceKey(),
                openPayload.items().size(),
                openPayload.data().length,
                canSend
            );
        }
        if (canSend) {
            ServerPlayNetworking.send(player, payload);
        }
    }

    private enum MutationSequence {
        NEXT,
        DUPLICATE,
        REJECTED
    }

    private static final class PlayerSessions {
        private final LinkedHashMap<Integer, Session> sessions = new LinkedHashMap<>();
        private final LinkedHashMap<String, DormantGhostSource> dormantGhostSources = new LinkedHashMap<>();
        private final LinkedHashMap<String, SourceIdentity> sourceIdentities = new LinkedHashMap<>();
        private final TokenBucket operationRateLimit = new TokenBucket(40.0D, 80.0D, System.nanoTime());
        private final TokenBucket controlRateLimit = new TokenBucket(10.0D, 20.0D, System.nanoTime());
        private final TokenBucket modeRateLimit = new TokenBucket(1.0D, 4.0D, System.nanoTime());
        private final TokenBucket resyncRateLimit = new TokenBucket(1.0D, 2.0D, System.nanoTime());
        private final TokenBucket mutationReplayRateLimit = new TokenBucket(20.0D, 40.0D, System.nanoTime());
        private final BoundedLinkGraph<String> links = new BoundedLinkGraph<>();
        private boolean negotiated;
        private boolean uiEnabled;
        private boolean gameplayEnabled;
        private long capabilities;
        private long connectionNonce;
        private long playerSessionToken;
        private long lastModeSequence = -1L;
        private long lastMutationId;
        private Set<String> forcedMenuIds = Set.of();
        private int nextSessionId = 1;
        private int dormantGhostProbeTicks;
        private long lifecycleTicks;
        private int handlerCallbackDepth;
        private boolean handlerBroadcastPending;

        private boolean isActive() {
            return this.negotiated && this.uiEnabled;
        }

        private boolean isGameplayActive() {
            return this.negotiated && this.gameplayEnabled;
        }

        private boolean authorizes(long nonce, long capability) {
            return this.isActive() && nonce != 0L && nonce == this.connectionNonce && this.hasCapability(capability);
        }

        private boolean authorizesMutationConnection(long nonce, long token) {
            return this.negotiated
                && nonce != 0L
                && nonce == this.connectionNonce
                && token != 0L
                && token == this.playerSessionToken
                && this.hasCapability(DesktopPackets.CAP_MULTI_MENU_GESTURES);
        }

        private boolean hasCapability(long capability) {
            return (this.capabilities & capability) == capability;
        }

        private MutationSequence acceptMutation(long mutationId) {
            if (mutationId <= 0L) {
                return MutationSequence.REJECTED;
            }
            if (this.lastMutationId > 0L && mutationId == this.lastMutationId) {
                return MutationSequence.DUPLICATE;
            }
            if (this.lastMutationId == Long.MAX_VALUE || mutationId != this.lastMutationId + 1L) {
                return MutationSequence.REJECTED;
            }
            this.lastMutationId = mutationId;
            return MutationSequence.NEXT;
        }

        private long nextMutationId() {
            return this.lastMutationId == Long.MAX_VALUE ? -1L : this.lastMutationId + 1L;
        }

        private boolean allowOperation(ServerPlayer player, String operation, double cost) {
            boolean allowed = this.operationRateLimit.tryConsume(cost, System.nanoTime());
            if (!allowed) {
                DesktopDebug.trace("server desktop operation rate-limited player={} operation={}", player.getName().getString(), operation);
            }
            return allowed;
        }

        private boolean allowControl(ServerPlayer player, String operation, double cost) {
            boolean allowed = this.controlRateLimit.tryConsume(cost, System.nanoTime());
            if (!allowed) {
                DesktopDebug.trace("server desktop control rate-limited player={} operation={}", player.getName().getString(), operation);
            }
            return allowed;
        }

        private boolean allowResync(ServerPlayer player, String operation) {
            boolean allowed = this.resyncRateLimit.tryConsume(1.0D, System.nanoTime());
            if (!allowed) {
                DesktopDebug.trace("server desktop resync rate-limited player={} operation={}", player.getName().getString(), operation);
            }
            return allowed;
        }

        private boolean allowMutationReplay(ServerPlayer player, String operation) {
            boolean allowed = this.mutationReplayRateLimit.tryConsume(1.0D, System.nanoTime());
            if (!allowed) {
                DesktopDebug.trace(
                    "server mutation replay acknowledgement rate-limited player={} operation={}",
                    player.getName().getString(),
                    operation
                );
            }
            return allowed;
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
            if (this.sessions.get(oldSession.sessionId) != oldSession
                || !oldSession.sourceKey.equals(replacement.sourceKey)) {
                return false;
            }

            try {
                this.initialize(player, replacement);
            } catch (RuntimeException exception) {
                this.close(player, replacement.sessionId, true);
                DesktopDebug.warn(
                    "server session transition failed player={} oldSession={} newSession={} reason={}",
                    player.getName().getString(),
                    oldSession.sessionId,
                    replacement.sessionId,
                    exception.toString()
                );
                return false;
            }
            this.close(player, oldSession.sessionId, true);
            DesktopDebug.log(
                "server session transition player={} oldSession={} newSession={} source={}",
                player.getName().getString(),
                oldSession.sessionId,
                replacement.sessionId,
                replacement.sourceKey
            );
            return true;
        }

        private void initialize(ServerPlayer player, Session session) {
            session.sourceIdentity = captureBlockSourceIdentity(player, session.sourceKey);
            if (session.sourceIdentity != null) {
                this.rememberSourceIdentity(session.sourceKey, session.sourceIdentity);
            }
            session.sourceLock = DesktopItemSourceLocks.acquire(player, session.menu);
            this.sessions.put(session.sessionId, session);
            session.initializeServerHandler(player, this);
            session.menu.setCarried(ItemStack.EMPTY);
            SessionSynchronizer synchronizer = new SessionSynchronizer(player, session);
            withNetworkSession(player, session, () -> DesktopSynchronizerOverride.run(
                synchronizer,
                () -> session.menu.setSynchronizer(synchronizer)
            ));
            syncMerchantOffers(player, session);
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
                    if (!session.visibleToClient) {
                        this.setVisible(player, session, true, notifyClient);
                    } else if (session.ghostPinned) {
                        this.setVisible(player, session, !session.visibleToClient, notifyClient);
                    } else {
                        this.close(player, session.sessionId, notifyClient);
                    }
                    handled = true;
                }
            }

            return handled;
        }

        private void rememberSourceIdentity(String sourceKey, SourceIdentity identity) {
            this.sourceIdentities.remove(sourceKey);
            while (this.sourceIdentities.size() >= DesktopProtocol.MAX_LINK_NODES) {
                Iterator<String> iterator = this.sourceIdentities.keySet().iterator();
                if (!iterator.hasNext()) {
                    break;
                }
                iterator.next();
                iterator.remove();
            }
            this.sourceIdentities.put(sourceKey, identity);
        }

        private void setVisible(ServerPlayer player, Session session, boolean visible, boolean notifyClient) {
            if (session.visibleToClient == visible) {
                return;
            }

            if (visible && !canRestoreHiddenSession(player, session)) {
                DesktopDebug.log("server session restore rejected player={} session={} title={} source={}", player.getName().getString(), session.sessionId, session.title.getString(), session.sourceKey);
                this.close(player, session.sessionId, notifyClient);
                return;
            }

            session.visibleToClient = visible;
            DesktopDebug.log("server session visibility player={} session={} title={} visible={} notify={}", player.getName().getString(), session.sessionId, session.title.getString(), visible, notifyClient);
            if (notifyClient) {
                send(player, new DesktopSessionVisibilityPayload(this.connectionNonce, session.sessionId, session.sessionToken, visible));
            }
            if (visible) {
                withNetworkSession(player, session, session.menu::sendAllDataToRemote);
                syncMerchantOffers(player, session);
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
            try {
                session.dispatchClosed(player, this);
                // The canonical cursor lives on the player menu. A copied cursor on a
                // desktop menu must never be settled again by removed().
                session.menu.setCarried(ItemStack.EMPTY);
                session.menu.removed(player);
            } finally {
                session.releaseSourceLock();
            }
            if (notifyClient) {
                send(player, new DesktopSessionClosedPayload(sessionId));
            }
        }

        private void closeAll(ServerPlayer player, boolean notifyClient) {
            for (Integer sessionId : List.copyOf(this.sessions.keySet())) {
                this.close(player, sessionId, notifyClient);
            }
            this.dormantGhostSources.clear();
            this.sourceIdentities.clear();
        }

        private void tick(ServerPlayer player) {
            this.lifecycleTicks++;
            for (Session session : List.copyOf(this.sessions.values())) {
                if (!session.menu.stillValid(player)) {
                    DesktopDebug.log("server session invalid player={} session={} title={}", player.getName().getString(), session.sessionId, session.title.getString());
                    if (isCamelOrLlamaSpecial(session.specialKind)) {
                        mountDiag(
                            "server_session_invalid player={} session={} special={} entityId={} title={} visible={} source={}",
                            player.getName().getString(),
                            session.sessionId,
                            session.specialKind,
                            session.entityId,
                            session.title.getString(),
                            session.visibleToClient,
                            session.sourceKey
                        );
                    }
                    this.rememberDormantGhost(session, "invalid");
                    this.close(player, session.sessionId, true);
                } else {
                    withNetworkSession(player, session, session.menu::broadcastChanges);
                    session.dispatchTick(player, this);
                }
            }
            this.reopenDormantGhosts(player);
        }

        private void beginHandlerCallback() {
            this.handlerCallbackDepth++;
        }

        private void endHandlerCallback(ServerPlayer player) {
            if (this.handlerCallbackDepth <= 0) {
                throw new IllegalStateException("Unbalanced desktop handler callback");
            }
            this.handlerCallbackDepth--;
            this.flushHandlerBroadcast(player);
        }

        private void requestHandlerBroadcast(ServerPlayer player) {
            this.handlerBroadcastPending = true;
            this.flushHandlerBroadcast(player);
        }

        private void flushHandlerBroadcast(ServerPlayer player) {
            if (this.handlerCallbackDepth != 0 || !this.handlerBroadcastPending) {
                return;
            }
            this.handlerBroadcastPending = false;
            this.broadcastAll(player);
            syncCarried(player, this);
        }

        private void broadcastAll(ServerPlayer player) {
            for (Session session : this.sessions.values()) {
                withNetworkSession(player, session, session.menu::broadcastChanges);
            }
            player.inventoryMenu.broadcastChanges();
        }

        private void rememberDormantGhost(Session session, String reason) {
            if (!session.ghostPinned || !isBlockBackedSourceKey(session.sourceKey) || session.sourceIdentity == null) {
                return;
            }

            this.purgeExpiredDormantGhosts();
            this.dormantGhostSources.remove(session.sourceKey);
            while (this.dormantGhostSources.size() >= MAX_DORMANT_GHOST_SOURCES) {
                Iterator<String> iterator = this.dormantGhostSources.keySet().iterator();
                if (!iterator.hasNext()) {
                    break;
                }
                iterator.next();
                iterator.remove();
            }
            this.dormantGhostSources.put(session.sourceKey, new DormantGhostSource(
                session.sourceKey,
                session.sourceIdentity,
                this.lifecycleTicks + DesktopProtocol.DORMANT_SOURCE_TTL_TICKS
            ));
            DesktopDebug.log("server dormant ghost remember source={} session={} title={} reason={}", session.sourceKey, session.sessionId, session.title.getString(), reason);
        }

        private void reopenDormantGhosts(ServerPlayer player) {
            if (this.dormantGhostSources.isEmpty()) {
                return;
            }

            this.purgeExpiredDormantGhosts();
            this.dormantGhostProbeTicks++;
            if (this.dormantGhostProbeTicks % DORMANT_GHOST_REOPEN_INTERVAL_TICKS != 0) {
                return;
            }

            for (DormantGhostSource dormant : List.copyOf(this.dormantGhostSources.values())) {
                if (this.hasSessionForSourceKey(dormant.sourceKey())) {
                    this.dormantGhostSources.remove(dormant.sourceKey());
                    continue;
                }

                MenuProvider provider = providerForDormantGhost(player, dormant.sourceKey(), dormant.sourceIdentity());
                if (provider == null) {
                    continue;
                }

                DesktopDebug.log("server dormant ghost reopen player={} source={} title={}", player.getName().getString(), dormant.sourceKey(), provider.getDisplayName().getString());
                OptionalInt opened = openMenuSession(player, provider, dormant.sourceKey(), false, true, false, null);
                if (opened != null && opened.isPresent()) {
                    this.dormantGhostSources.remove(dormant.sourceKey());
                }
            }
        }

        private void purgeExpiredDormantGhosts() {
            this.dormantGhostSources.values().removeIf(dormant -> dormant.expiresAtTick() <= this.lifecycleTicks);
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
        private final long sessionToken;
        private final AbstractContainerMenu menu;
        private final Component title;
        private final int specialKind;
        private final int entityId;
        private final int columns;
        private final int menuTypeId;
        private final String sourceKey;
        private final byte[] openingData;
        private final int replacesSessionId;
        private @Nullable SourceIdentity sourceIdentity;
        private boolean ghostPinned;
        private boolean visibleToClient = true;
        private @Nullable DesktopServerWindowHandler<AbstractContainerMenu, Object> serverHandler;
        private @Nullable Object serverState;
        private DesktopItemSourceLocks.Lease sourceLock;
        private boolean initialSnapshotSent;

        private Session(int sessionId, AbstractContainerMenu menu, Component title, int specialKind, int entityId, int columns, int menuTypeId, String sourceKey) {
            this(sessionId, menu, title, specialKind, entityId, columns, menuTypeId, sourceKey, new byte[0], -1);
        }

        private Session(
            int sessionId,
            AbstractContainerMenu menu,
            Component title,
            int specialKind,
            int entityId,
            int columns,
            int menuTypeId,
            String sourceKey,
            byte[] openingData,
            int replacesSessionId
        ) {
            this.sessionId = sessionId;
            this.sessionToken = nextToken();
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

        private void releaseSourceLock() {
            if (this.sourceLock == null) {
                return;
            }
            this.sourceLock.close();
            this.sourceLock = null;
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

            sessions.beginHandlerCallback();
            try {
                this.serverState = this.serverHandler.createState(new ServerSessionContext(player, sessions, this));
            } catch (RuntimeException exception) {
                this.quarantineServerHandler(player, "create-state", exception);
            } finally {
                sessions.endHandlerCallback(player);
            }
        }

        private boolean recipeTransferSupported() {
            return this.menu instanceof AbstractCraftingMenu
                || this.serverHandler != null && DesktopTransferValidators.supports(this.serverHandler);
        }

        private void dispatchOpened(ServerPlayer player, PlayerSessions sessions) {
            if (this.serverHandler == null) {
                return;
            }
            sessions.beginHandlerCallback();
            try {
                this.serverHandler.opened(new ServerSessionContext(player, sessions, this));
            } catch (RuntimeException exception) {
                this.quarantineServerHandler(player, "opened", exception);
            } finally {
                sessions.endHandlerCallback(player);
            }
        }

        private void dispatchTick(ServerPlayer player, PlayerSessions sessions) {
            if (this.serverHandler == null) {
                return;
            }
            sessions.beginHandlerCallback();
            try {
                this.serverHandler.tick(new ServerSessionContext(player, sessions, this));
            } catch (RuntimeException exception) {
                this.quarantineServerHandler(player, "tick", exception);
            } finally {
                sessions.endHandlerCallback(player);
            }
        }

        private void dispatchClosed(ServerPlayer player, PlayerSessions sessions) {
            if (this.serverHandler == null) {
                return;
            }
            sessions.beginHandlerCallback();
            try {
                this.serverHandler.closed(new ServerSessionContext(player, sessions, this));
            } catch (RuntimeException exception) {
                this.quarantineServerHandler(player, "closed", exception);
            } finally {
                sessions.endHandlerCallback(player);
            }
        }

        private void dispatchVisibilityChanged(ServerPlayer player, PlayerSessions sessions) {
            if (this.serverHandler == null) {
                return;
            }
            sessions.beginHandlerCallback();
            try {
                this.serverHandler.visibilityChanged(new ServerSessionContext(player, sessions, this), this.visibleToClient);
            } catch (RuntimeException exception) {
                this.quarantineServerHandler(player, "visibility", exception);
            } finally {
                sessions.endHandlerCallback(player);
            }
        }

        private void dispatchPinChanged(ServerPlayer player, PlayerSessions sessions) {
            if (this.serverHandler == null) {
                return;
            }
            sessions.beginHandlerCallback();
            try {
                this.serverHandler.pinChanged(new ServerSessionContext(player, sessions, this), this.ghostPinned);
            } catch (RuntimeException exception) {
                this.quarantineServerHandler(player, "pin", exception);
            } finally {
                sessions.endHandlerCallback(player);
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

    private record AuthorizedSlot(
        DesktopSlotReference reference,
        SessionAuthorization authorization,
        Slot slot
    ) {
    }

    private record PickupSource(
        AbstractContainerMenu menu,
        Slot slot,
        Object physicalOwner,
        int physicalIndex
    ) {
    }

    private static final class PickupTargetPlan {
        private final AbstractContainerMenu menu;
        private final Slot slot;
        private final Object physicalOwner;
        private final int physicalIndex;
        private final ItemStack before;
        private ItemStack expected;

        private PickupTargetPlan(
            AbstractContainerMenu menu,
            Slot slot,
            Object physicalOwner,
            int physicalIndex,
            ItemStack before
        ) {
            this.menu = menu;
            this.slot = slot;
            this.physicalOwner = physicalOwner;
            this.physicalIndex = physicalIndex;
            this.before = before.copy();
            this.expected = before.copy();
        }

        private AbstractContainerMenu menu() {
            return this.menu;
        }

        private Slot slot() {
            return this.slot;
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

    private record DragTargetPlan(
        AbstractContainerMenu menu,
        Slot slot,
        Object physicalOwner,
        int physicalIndex,
        ItemStack before,
        ItemStack after
    ) {
        private DragTargetPlan {
            before = before.copy();
            after = after.copy();
        }
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

    /** Hard cap for callback-aware validation work shared by one quick-move mutation. */
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

    private record QuickMoveSourceSnapshot(
        DesktopSlotReference reference,
        SessionAuthorization authorization,
        Object physicalOwner,
        int physicalIndex,
        ItemStack stack
    ) {
        private QuickMoveSourceSnapshot {
            stack = stack.copy();
        }
    }

    private record SessionAuthorization(
        int sessionId,
        AbstractContainerMenu menu,
        @Nullable Session session,
        String linkNode
    ) {
    }

    private record JeiTransferTarget(int sessionId, AbstractContainerMenu menu, @Nullable Session session) {
    }

    private record JeiTransferRequirement(int inputIndex, Slot targetSlot, List<ItemStack> alternatives) {
    }

    private record JeiTransferSimulation(Map<Slot, ItemStack> sourceStacks, Map<Slot, ItemStack> targetStacks) {
    }

    private static final class StackVariant {
        private static final Comparator<StackVariant> COMPARATOR = Comparator
            .comparing((StackVariant variant) -> BuiltInRegistries.ITEM.getKey(variant.stack.getItem()).toString())
            .thenComparingInt(variant -> ItemStack.hashItemAndComponents(variant.stack))
            .thenComparing(variant -> variant.stack.toString());

        private final ItemStack stack;

        private StackVariant(ItemStack stack) {
            this.stack = stack.copyWithCount(1);
        }

        private ItemStack stack() {
            return this.stack;
        }

        private boolean matches(ItemStack other) {
            return ItemStack.isSameItemSameComponents(this.stack, other);
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof StackVariant variant && this.matches(variant.stack);
        }

        @Override
        public int hashCode() {
            return ItemStack.hashItemAndComponents(this.stack);
        }
    }

    private record JeiTransferSourceKey(Container container, int containerSlot) {
    }

    private record SourceIdentity(List<SourceBackingIdentity> backing) {
        private SourceIdentity {
            backing = List.copyOf(backing);
        }

        private boolean matches(ServerLevel level, SourceKey source) {
            if (this.backing.size() != source.positions().size()) {
                return false;
            }
            for (int index = 0; index < this.backing.size(); index++) {
                BlockPos pos = source.positions().get(index);
                BlockState state = level.getBlockState(pos);
                BlockEntity blockEntity = level.getBlockEntity(pos);
                SourceBackingIdentity expected = this.backing.get(index);
                String blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
                String blockEntityTypeId = blockEntity == null
                    ? ""
                    : String.valueOf(BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(blockEntity.getType()));
                if (!expected.blockId().equals(blockId)
                    || !expected.blockEntityTypeId().equals(blockEntityTypeId)
                    || (expected.blockEntity() == null ? blockEntity != null : expected.blockEntity().get() != blockEntity)) {
                    return false;
                }
            }
            return true;
        }
    }

    private record SourceBackingIdentity(
        String blockId,
        String blockEntityTypeId,
        @Nullable WeakReference<BlockEntity> blockEntity
    ) {
    }

    private record DormantGhostSource(String sourceKey, SourceIdentity sourceIdentity, long expiresAtTick) {
    }

    private record SessionTransition(ServerPlayer player, Session oldSession) {
    }

    private record NetworkSession(ServerPlayer player, Session session) {
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
        public void sendToClient(Identifier channel, byte[] data) {
            send(this.player, new DesktopCustomPayload(
                this.sessions.connectionNonce, 0L, this.sessions.playerSessionToken,
                this.session.sessionId, this.session.sessionToken,
                this.session.menu.getStateId(), channel, data
            ));
        }

        @Override
        public void broadcastChanges() {
            this.sessions.requestHandlerBroadcast(this.player);
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
        public Identifier channel() {
            return this.payload.channel();
        }

        @Override
        public byte[] data() {
            return this.payload.data();
        }

        @Override
        public void sendToClient(Identifier channel, byte[] data) {
            send(this.player, new DesktopCustomPayload(
                this.sessions.connectionNonce, 0L, this.sessions.playerSessionToken,
                this.session.sessionId, this.session.sessionToken,
                this.session.menu.getStateId(), channel, data
            ));
        }

        @Override
        public void broadcastChanges() {
            this.sessions.requestHandlerBroadcast(this.player);
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
        public void sendInitialData(AbstractContainerMenu menu, java.util.List<ItemStack> stacks, ItemStack carried, int[] dataSlots) {
            DesktopDebug.log("server send initial player={} session={} title={} slots={} data={}", this.player.getName().getString(), this.session.sessionId, this.session.title.getString(), stacks.size(), dataSlots.length);
            if (isCamelOrLlamaSpecial(this.session.specialKind)) {
                mountDiag(
                    "server_initial_data player={} session={} special={} entityId={} columns={} visible={} source={} menuSlots={} stacks={} data={} carried={}",
                    this.player.getName().getString(),
                    this.session.sessionId,
                    this.session.specialKind,
                    this.session.entityId,
                    this.session.columns,
                    this.session.visibleToClient,
                    this.session.sourceKey,
                    menu.slots.size(),
                    stacks.size(),
                    dataSlots.length,
                    carried
                );
            }
            PlayerSessions sessions = PLAYERS.get(this.player);
            int replacesSessionId = this.session.initialSnapshotSent ? -1 : this.session.replacesSessionId;
            if (sessions != null && this.session.openingData.length > 0) {
                send(this.player, new DesktopMenuOpenDataPayload(
                    sessions.connectionNonce,
                    this.session.sessionId,
                    this.session.sessionToken,
                    this.session.menuTypeId,
                    replacesSessionId,
                    this.session.openingData
                ));
            }
            send(this.player, new DesktopOpenSessionPayload(
                this.session.sessionId,
                this.session.sessionToken,
                this.session.menuTypeId,
                this.session.specialKind,
                this.session.entityId,
                this.session.columns,
                menu.getStateId(),
                this.session.visibleToClient,
                this.session.recipeTransferSupported(),
                this.session.sourceKey,
                this.session.title,
                stacks,
                this.player.inventoryMenu.getCarried().copy(),
                dataSlots
            ));
            this.session.initialSnapshotSent = true;
            if (sessions != null) {
                syncCarried(this.player, sessions);
            }
        }

        @Override
        public void sendSlotChange(AbstractContainerMenu menu, int slot, ItemStack stack) {
            DesktopDebug.trace("server send slot player={} session={} slot={} stack={}", this.player.getName().getString(), this.session.sessionId, slot, stack);
            send(this.player, new DesktopSlotPayload(this.session.sessionId, slot, menu.getStateId(), stack.copy()));
        }

        @Override
        public void sendCarriedChange(AbstractContainerMenu menu, ItemStack stack) {
            ItemStack canonicalCarried = this.player.inventoryMenu.getCarried().copy();
            menu.setCarried(ItemStack.EMPTY);
            menu.setRemoteCarried(HashedStack.EMPTY);
            PlayerSessions sessions = PLAYERS.get(this.player);
            if (sessions != null) {
                syncCarried(this.player, sessions);
            }
            DesktopDebug.trace(
                "server ignored detached carried player={} session={} reported={} canonical={}",
                this.player.getName().getString(),
                this.session.sessionId,
                stack,
                canonicalCarried
            );
        }

        @Override
        public void sendDataChange(AbstractContainerMenu menu, int dataSlotIndex, int value) {
            DesktopDebug.trace("server send data player={} session={} data={} value={}", this.player.getName().getString(), this.session.sessionId, dataSlotIndex, value);
            send(this.player, new DesktopDataPayload(this.session.sessionId, dataSlotIndex, value));
        }

        @Override
        public RemoteSlot createSlot() {
            return new TrackingRemoteSlot();
        }
    }

    private static final class TrackingRemoteSlot implements RemoteSlot {
        private ItemStack stack = ItemStack.EMPTY;

        @Override
        public void force(ItemStack stack) {
            this.stack = stack.copy();
        }

        @Override
        public void receive(HashedStack stack) {
            this.stack = ItemStack.EMPTY;
        }

        @Override
        public boolean matches(ItemStack stack) {
            return ItemStack.matches(this.stack, stack);
        }
    }

    private static @Nullable String sourceKeyForProvider(ServerPlayer player, MenuProvider provider) {
        if (provider instanceof BlockEntity blockEntity) {
            return sourceKeyForBlock(player, blockEntity.getBlockPos());
        }

        if (provider instanceof net.minecraft.world.entity.Entity entity) {
            return sourceKeyForEntity(player, entity);
        }

        return null;
    }

    private static boolean isBlockBackedSourceKey(String sourceKey) {
        return sourceKey.startsWith("block:") || sourceKey.startsWith("chest:");
    }

    private static @Nullable MenuProvider providerForDormantGhost(ServerPlayer player, String sourceKey, @Nullable SourceIdentity expectedIdentity) {
        SourceKey source = SourceKey.parse(sourceKey);
        if (source == null
            || expectedIdentity == null
            || !source.dimension().equals(player.level().dimension().identifier().toString())) {
            return null;
        }

        ServerLevel level = player.level();
        if (!prevalidateSourcePositions(player, level, source)
            || !expectedIdentity.matches(level, source)) {
            return null;
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

    private static boolean canRestoreHiddenSession(ServerPlayer player, Session session) {
        if (!session.menu.stillValid(player)) {
            return false;
        }
        if (isBlockBackedSourceKey(session.sourceKey)) {
            return providerForDormantGhost(player, session.sourceKey, session.sourceIdentity) != null;
        }
        return true;
    }

    private static @Nullable SourceIdentity captureBlockSourceIdentity(ServerPlayer player, String sourceKey) {
        SourceKey source = SourceKey.parse(sourceKey);
        if (source == null || !source.dimension().equals(player.level().dimension().identifier().toString())) {
            return null;
        }
        ServerLevel level = player.level();
        return prevalidateSourcePositions(player, level, source) ? captureLoadedSourceIdentity(level, source) : null;
    }

    private static boolean prevalidateSourcePositions(ServerPlayer player, ServerLevel level, SourceKey source) {
        for (BlockPos pos : source.positions()) {
            if (!level.isInWorldBounds(pos)
                || !level.hasChunkAt(pos)
                || !level.isLoaded(pos)
                || !level.mayInteract(player, pos)) {
                return false;
            }
        }
        return true;
    }

    private static SourceIdentity captureLoadedSourceIdentity(ServerLevel level, SourceKey source) {
        List<SourceBackingIdentity> backing = new ArrayList<>(source.positions().size());
        for (BlockPos pos : source.positions()) {
            BlockState state = level.getBlockState(pos);
            BlockEntity blockEntity = level.getBlockEntity(pos);
            String blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
            String blockEntityTypeId = blockEntity == null
                ? ""
                : String.valueOf(BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(blockEntity.getType()));
            backing.add(new SourceBackingIdentity(
                blockId,
                blockEntityTypeId,
                blockEntity == null ? null : new WeakReference<>(blockEntity)
            ));
        }
        return new SourceIdentity(List.copyOf(backing));
    }

    private static boolean canReachDormantSource(ServerPlayer player, ServerLevel level, BlockPos pos) {
        Vec3 target = Vec3.atCenterOf(pos);
        Vec3 eye = player.getEyePosition();
        double range = player.blockInteractionRange();
        if (!Double.isFinite(range) || range < 0.0D) {
            return false;
        }
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
        return hit.getType() == HitResult.Type.MISS || hit.getBlockPos().equals(pos);
    }

    private record SourceKey(String kind, String dimension, List<BlockPos> positions) {
        private static @Nullable SourceKey parse(String sourceKey) {
            if (sourceKey == null || sourceKey.isBlank() || sourceKey.length() > DesktopProtocol.MAX_SOURCE_TEXT_LENGTH) {
                return null;
            }
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
                if (positions.size() >= 2) {
                    return null;
                }
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

    private static String sourceKeyForEntity(ServerPlayer player, net.minecraft.world.entity.Entity entity) {
        return "entity:" + player.level().dimension().identifier() + ":" + entity.getUUID();
    }

    private static String sourceKeyForBlock(ServerPlayer player, BlockPos pos) {
        String dimension = player.level().dimension().identifier().toString();
        BlockState state = player.level().getBlockState(pos);
        if (state.getBlock() instanceof ChestBlock && state.hasProperty(ChestBlock.TYPE)) {
            ChestType chestType = state.getValue(ChestBlock.TYPE);
            if (chestType != ChestType.SINGLE) {
                BlockPos connectedPos = ChestBlock.getConnectedBlockPos(pos, state);
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

    private static long nextToken() {
        long token;
        do {
            token = SECURE_RANDOM.nextLong();
        } while (token == 0L);
        return token;
    }

    private static Set<String> validateForcedMenuIds(List<String> rawIds) {
        Set<String> validated = new HashSet<>();
        for (String raw : rawIds) {
            if (raw == null || raw.isBlank() || raw.length() > DesktopProtocol.MAX_IDENTIFIER_LENGTH) {
                continue;
            }
            try {
                Identifier id = Identifier.parse(raw);
                if (BuiltInRegistries.MENU.containsKey(id)) {
                    validated.add(id.toString());
                }
            } catch (RuntimeException ignored) {
                // Invalid client configuration never becomes server authority.
            }
        }
        return Set.copyOf(validated);
    }
}
