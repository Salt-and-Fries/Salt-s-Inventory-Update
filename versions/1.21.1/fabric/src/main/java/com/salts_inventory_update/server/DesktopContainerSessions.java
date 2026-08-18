package com.salts_inventory_update.server;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;
import java.security.SecureRandom;

import com.salts_inventory_update.platform.fabric.api.event.lifecycle.v1.ServerTickEvents;
import com.salts_inventory_update.platform.fabric.api.networking.v1.ServerPlayConnectionEvents;
import com.salts_inventory_update.platform.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
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
import net.minecraft.world.inventory.CrafterMenu;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.inventory.HorseInventoryMenu;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.StonecutterMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
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
import com.salts_inventory_update.api.server.desktop.DesktopTransferValidators;
import com.salts_inventory_update.api.server.desktop.DesktopTransferDecision;
import com.salts_inventory_update.api.server.desktop.DesktopTransferRequest;
import com.salts_inventory_update.api.server.desktop.DesktopTransferRequirement;
import com.salts_inventory_update.compat.toms_storage.TomsStorageCompat;
import com.salts_inventory_update.debug.DesktopDebug;
import com.salts_inventory_update.inventory.InventoryExpansion;
import com.salts_inventory_update.network.DesktopPackets;
import com.salts_inventory_update.network.DesktopPackets.DesktopButtonPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopCarriedPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopClickPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopCloseSessionPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopCustomPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopDataPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopHelloAckPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopHelloPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopMerchantOffersPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopModePayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopOpenLinkedSourcesPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopOpenSessionPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopGhostRecipePayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopJeiTransferPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopLinkSessionsPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopPlaceRecipePayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopQuickMovePayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopReadyPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopRenamePayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopSessionClosedPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopSessionPinPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopSessionVisibilityPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopSlotPayload;
import com.salts_inventory_update.network.DesktopPackets.InventorySlotPurchasePayload;
import com.salts_inventory_update.network.DesktopPackets.MutationStamp;
import com.salts_inventory_update.protocol.DesktopConnectionState;
import com.salts_inventory_update.protocol.BoundedLinkGraph;
import com.salts_inventory_update.protocol.BoundedTransferPlanner;
import com.salts_inventory_update.protocol.DesktopProtocol;
import com.salts_inventory_update.protocol.TokenBucket;

public final class DesktopContainerSessions {
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
    private static final Map<UUID, PlayerSessions> PLAYERS = new LinkedHashMap<>();
    private static final SecureRandom NONCE_RANDOM = new SecureRandom();
    private static final Map<ServerPlayer, DesktopConnectionState> CONNECTIONS = new LinkedHashMap<>();
    private static final Set<ServerPlayer> PROTOCOL_REJECTIONS = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
    private static final Map<UUID, String> PENDING_USE_TARGETS = new LinkedHashMap<>();

    private DesktopContainerSessions() {
    }

    public static void initialize() {
        DesktopDebug.log("server desktop session networking initialized");
        ServerPlayNetworking.registerGlobalReceiver(DesktopHelloPayload.TYPE, (payload, context) ->
            context.server().execute(() -> hello(context.player(), payload))
        );
        ServerPlayNetworking.registerGlobalReceiver(DesktopModePayload.TYPE, (payload, context) ->
            context.server().execute(() -> setMode(context.player(), payload))
        );
        ServerPlayNetworking.registerGlobalReceiver(DesktopReadyPayload.TYPE, (payload, context) ->
            context.server().execute(() -> rejectLegacyReady(context.player()))
        );
        ServerPlayNetworking.registerGlobalReceiver(DesktopClickPayload.TYPE, (payload, context) ->
            context.server().execute(() -> click(context.player(), payload))
        );
        ServerPlayNetworking.registerGlobalReceiver(DesktopQuickMovePayload.TYPE, (payload, context) ->
            context.server().execute(() -> quickMove(context.player(), payload))
        );
        ServerPlayNetworking.registerGlobalReceiver(DesktopButtonPayload.TYPE, (payload, context) ->
            context.server().execute(() -> button(context.player(), payload))
        );
        ServerPlayNetworking.registerGlobalReceiver(DesktopPlaceRecipePayload.TYPE, (payload, context) ->
            context.server().execute(() -> placeRecipe(context.player(), payload))
        );
        ServerPlayNetworking.registerGlobalReceiver(DesktopJeiTransferPayload.TYPE, (payload, context) ->
            context.server().execute(() -> transferJeiRecipe(context.player(), payload))
        );
        ServerPlayNetworking.registerGlobalReceiver(DesktopRenamePayload.TYPE, (payload, context) ->
            context.server().execute(() -> rename(context.player(), payload))
        );
        ServerPlayNetworking.registerGlobalReceiver(DesktopCloseSessionPayload.TYPE, (payload, context) ->
            context.server().execute(() -> closeSession(context.player(), payload, true))
        );
        ServerPlayNetworking.registerGlobalReceiver(DesktopSessionPinPayload.TYPE, (payload, context) ->
            context.server().execute(() -> setSessionPin(context.player(), payload))
        );
        ServerPlayNetworking.registerGlobalReceiver(DesktopSessionVisibilityPayload.TYPE, (payload, context) ->
            context.server().execute(() -> setSessionVisibility(context.player(), payload))
        );
        ServerPlayNetworking.registerGlobalReceiver(DesktopLinkSessionsPayload.TYPE, (payload, context) ->
            context.server().execute(() -> linkSessions(context.player(), payload))
        );
        ServerPlayNetworking.registerGlobalReceiver(DesktopOpenLinkedSourcesPayload.TYPE, (payload, context) ->
            context.server().execute(() -> openLinkedSources(context.player(), payload))
        );
        ServerPlayNetworking.registerGlobalReceiver(DesktopCustomPayload.TYPE, (payload, context) ->
            context.server().execute(() -> customPayload(context.player(), payload))
        );
        ServerPlayNetworking.registerGlobalReceiver(DesktopCarriedPayload.TYPE, (payload, context) ->
            context.server().execute(() -> carried(context.player(), payload))
        );
        ServerPlayNetworking.registerGlobalReceiver(InventorySlotPurchasePayload.TYPE, (payload, context) ->
            context.server().execute(() -> purchaseInventorySlot(context.player(), payload))
        );
        ServerTickEvents.END_SERVER_TICK.register(DesktopContainerSessions::tick);
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> disconnect(handler.player));
    }

    public static boolean shouldCapture(ServerPlayer player) {
        PlayerSessions sessions = PLAYERS.get(player.getUUID());
        return sessions != null && sessions.ready && canUseCustomWindows(player);
    }

    public static boolean isPlayerActive(ServerPlayer player) {
        return authorizesFeature(player, DesktopProtocol.CAP_INVENTORY_TOPOLOGY, true);
    }

    public static boolean isPlayerNegotiated(ServerPlayer player) {
        return authorizesFeature(player, DesktopProtocol.CAP_INVENTORY_TOPOLOGY, false);
    }

    private static boolean canUseCustomWindows(ServerPlayer player) {
        return authorizesFeature(player, DesktopProtocol.CAP_CUSTOM_WINDOWS, true);
    }

    private static boolean authorizesFeature(ServerPlayer player, long capability, boolean requireUi) {
        DesktopConnectionState connection = CONNECTIONS.get(player);
        PlayerSessions sessions = PLAYERS.get(player.getUUID());
        boolean effectiveRequireUi = requireUi && (sessions == null || !sessions.closingForModeDisable);
        return connection != null
            && connection.authorizes(connection.connectionNonce(), capability, effectiveRequireUi);
    }

    public static long connectionNonceFor(ServerPlayer player) {
        DesktopConnectionState connection = CONNECTIONS.get(player);
        return connection == null ? 0L : connection.connectionNonce();
    }

    public static long playerMenuNonceFor(ServerPlayer player) {
        PlayerSessions sessions = PLAYERS.get(player.getUUID());
        return sessions == null ? 0L : sessions.playerMenuNonce;
    }

    public static void captureUseTarget(ServerPlayer player, BlockHitResult hitResult) {
        if (!canUseCustomWindows(player)) {
            return;
        }

        if (hitResult.getType() != HitResult.Type.BLOCK) {
            return;
        }

        String sourceKey = sourceKeyForBlock(player, hitResult.getBlockPos());
        PENDING_USE_TARGETS.put(player.getUUID(), sourceKey);
        DesktopDebug.log("server use target player={} key={}", player.getName().getString(), sourceKey);
    }

    public static void clearUseTarget(ServerPlayer player) {
        PENDING_USE_TARGETS.remove(player.getUUID());
    }

    public static boolean hasOpenSessionForContainer(Player player, Container container) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return false;
        }

        PlayerSessions sessions = PLAYERS.get(serverPlayer.getUUID());
        if (sessions == null || !sessions.ready || !canUseCustomWindows(serverPlayer)) {
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

    public static @Nullable OptionalInt openMenuSession(ServerPlayer player, MenuProvider provider) {
        return openMenuSession(player, provider, null, true, false, true);
    }

    private static @Nullable OptionalInt openMenuSession(
        ServerPlayer player,
        MenuProvider provider,
        @Nullable String forcedSourceKey,
        boolean toggleExisting,
        boolean ghostPinned,
        boolean visibleToClient
    ) {
        if (!canUseCustomWindows(player)) {
            return OptionalInt.empty();
        }
        if (provider == null) {
            DesktopDebug.warn("server capture skipped player={} reason=null-provider", player.getName().getString());
            return OptionalInt.empty();
        }

        PlayerSessions sessions = sessions(player);
        String providerSourceKey = forcedSourceKey == null ? sourceKeyForProvider(player, provider) : forcedSourceKey;
        String pendingSourceKey = PENDING_USE_TARGETS.get(player.getUUID());
        String sourceKey = providerSourceKey;
        if (sourceKey == null) {
            sourceKey = pendingSourceKey;
        }
        if (sourceKey != null && isBlockBackedSourceKey(sourceKey) && forcedSourceKey != null
            && !sessions.isSourceAuthorized(player, sourceKey)) {
                DesktopDebug.warn("server capture skipped player={} source={} reason=invalid-source-grant", player.getName().getString(), sourceKey);
                return OptionalInt.empty();
        }

        DesktopDebug.log(
            "server capture start player={} title={} provider={} providerSource={} pendingSource={} chosenSource={} toggle={} ready={} sessions={}",
            player.getName().getString(),
            provider.getDisplayName().getString(),
            provider.getClass().getName(),
            providerSourceKey,
            pendingSourceKey,
            sourceKey,
            toggleExisting,
            sessions.ready,
            sessions.sessions.size()
        );

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
        if (sourceKey != null && isBlockBackedSourceKey(sourceKey) && forcedSourceKey == null
            && !sessions.authorizeSource(player, sourceKey)) {
            menu.setCarried(ItemStack.EMPTY);
            menu.removed(player);
            DesktopDebug.warn("server capture skipped player={} source={} reason=invalid-source-grant", player.getName().getString(), sourceKey);
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
            sourceKey == null ? "" : sourceKey
        );
        session.ghostPinned = ghostPinned;
        session.visibleToClient = visibleToClient;
        sessions.add(player, session);
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
        if (!canUseCustomWindows(player)) {
            return;
        }
        PlayerSessions sessions = sessions(player);
        String sourceKey = sourceKeyForEntity(player, horse.getUUID());
        if (sessions.closeBySourceKey(player, sourceKey, true)) {
            DesktopDebug.log("server toggle close horse player={} source={}", player.getName().getString(), sourceKey);
            return;
        }

        int columns = horse.getInventoryColumns();
        int specialKind = horseSpecialKind(horse);
        int sessionId = nextSessionId(player);
        HorseInventoryMenu menu = new HorseInventoryMenu(sessionId, player.getInventory(), container, horse, columns);
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
        PlayerSessions sessions = PLAYERS.get(player.getUUID());
        if (key != null && sessions != null && sessions.forcedMenuIds.contains(key.toString())) {
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
        PlayerSessions sessions = PLAYERS.get(player.getUUID());
        if (sessions == null || !sessions.ready || !canUseCustomWindows(player) || !sessions.sessions.containsKey(containerId)) {
            return false;
        }

        if (sessions.sessions.get(containerId).menu instanceof MerchantMenu merchantMenu) {
            merchantMenu.setOffers(offers);
            merchantMenu.setMerchantLevel(villagerLevel);
            merchantMenu.setXp(villagerXp);
            merchantMenu.setShowProgressBar(showProgress);
            merchantMenu.setCanRestock(canRestock);
        }

        Session session = sessions.sessions.get(containerId);
        send(player, new DesktopMerchantOffersPayload(outboundStamp(player, session.sessionNonce, session.menu.getStateId()), containerId, offers, villagerLevel, villagerXp, showProgress, canRestock));
        DesktopDebug.trace("server merchant offers player={} session={}", player.getName().getString(), containerId);
        return true;
    }

    private static void hello(ServerPlayer player, DesktopHelloPayload payload) {
        if (PROTOCOL_REJECTIONS.contains(player)) {
            return;
        }
        PlayerSessions sessions = sessions(player);
        DesktopConnectionState connection = CONNECTIONS.computeIfAbsent(player, ignored -> new DesktopConnectionState());
        PENDING_USE_TARGETS.remove(player.getUUID());
        if (connection.phase() != DesktopConnectionState.Phase.UNNEGOTIATED) {
            PROTOCOL_REJECTIONS.add(player);
            sessions.ready = false;
            sessions.closingForModeDisable = true;
            try {
                sessions.closeAll(player, false);
            } finally {
                sessions.closingForModeDisable = false;
            }
            connection.markIncompatible();
            sessions.linkGraph.clear();
            sessions.sourceGrants.clear();
            player.connection.disconnect(Component.literal("Salt's Inventory Update received a repeated desktop handshake."));
            return;
        }
        sessions.closeAll(player, false);
        if (payload.protocolVersion() != DesktopProtocol.VERSION
            || payload.clientNonce() == 0L
            || (payload.capabilities() & ~DesktopProtocol.KNOWN_CAPABILITIES) != 0L) {
            PROTOCOL_REJECTIONS.add(player);
            connection.markIncompatible();
            sessions.ready = false;
            DesktopDebug.warn("server desktop hello rejected player={} protocol={} capabilities={}", player.getName().getString(), payload.protocolVersion(), payload.capabilities());
            player.connection.disconnect(Component.literal("Salt's Inventory Update protocol mismatch. Update the mod on both client and server (protocol 2 required)."));
            return;
        }

        long connectionNonce = nonzeroNonce();
        long capabilities = DesktopProtocol.sanitizeCapabilities(payload.capabilities());
        boolean uiEnabled = payload.uiEnabled();
        connection.begin(payload.clientNonce(), 0L);
        if (!connection.acknowledge(DesktopProtocol.VERSION, payload.clientNonce(), connectionNonce, capabilities, uiEnabled)) {
            sessions.ready = false;
            return;
        }
        sessions.ready = connection.isUiEnabled();
        sessions.playerMenuNonce = nonzeroNonce();
        sessions.forcedMenuIds = validateForcedMenuIds(payload.forcedMenuIds());
        send(player, new DesktopHelloAckPayload(DesktopProtocol.VERSION, payload.clientNonce(), connectionNonce, capabilities, uiEnabled));
        InventoryExpansion.appendMissingMenuSlots(player.inventoryMenu, player);
        InventoryExpansion.syncToClient(player);
        DesktopDebug.log("server desktop hello accepted player={} ui={} capabilities={}", player.getName().getString(), uiEnabled, capabilities);
    }

    private static void setMode(ServerPlayer player, DesktopModePayload payload) {
        DesktopConnectionState connection = CONNECTIONS.get(player);
        PlayerSessions sessions = PLAYERS.get(player.getUUID());
        if (connection == null || sessions == null
            || !connection.authorizes(payload.connectionNonce(), 0L, false)
            || !sessions.modeBucket.tryConsume(System.nanoTime())
            || !connection.updateMode(payload.connectionNonce(), payload.sequence(), payload.uiEnabled())) {
            return;
        }
        sessions.ready = connection.isUiEnabled();
        sessions.forcedMenuIds = validateForcedMenuIds(payload.forcedMenuIds());
        if (!sessions.ready) {
            PENDING_USE_TARGETS.remove(player.getUUID());
            sessions.closingForModeDisable = true;
            try {
                sessions.closeAll(player, false);
            } finally {
                sessions.closingForModeDisable = false;
            }
            InventoryExpansion.appendMissingMenuSlots(player.inventoryMenu, player);
            InventoryExpansion.syncToClient(player);
        } else {
            InventoryExpansion.appendMissingMenuSlots(player.inventoryMenu, player);
            InventoryExpansion.syncToClient(player);
        }
    }

    private static void rejectLegacyReady(ServerPlayer player) {
        if (!PROTOCOL_REJECTIONS.add(player)) {
            return;
        }
        PENDING_USE_TARGETS.remove(player.getUUID());
        CONNECTIONS.computeIfAbsent(player, ignored -> new DesktopConnectionState()).markIncompatible();
        PlayerSessions sessions = PLAYERS.get(player.getUUID());
        if (sessions != null) {
            sessions.ready = false;
            sessions.closeAll(player, false);
        }
        DesktopDebug.warn("server rejected legacy desktop-ready packet player={} expectedProtocol={}", player.getName().getString(), DesktopProtocol.VERSION);
        player.connection.disconnect(Component.literal("Salt's Inventory Update 0.1.1 is incompatible. Update the mod on both client and server (protocol 2 required)."));
    }

    public static void beforePlayerReplacement(ServerPlayer player) {
        PlayerSessions sessions = PLAYERS.get(player.getUUID());
        if (sessions != null) {
            sessions.closeAll(player, true);
            sessions.linkGraph.clear();
            sessions.dormantGhostSources.clear();
            sessions.sourceGrants.clear();
        }
        PENDING_USE_TARGETS.remove(player.getUUID());
    }

    public static void afterPlayerReplacement(ServerPlayer player, ServerPlayer oldPlayer) {
        DesktopConnectionState connection = CONNECTIONS.remove(oldPlayer);
        if (connection != null) {
            CONNECTIONS.put(player, connection);
        }
        PlayerSessions sessions = PLAYERS.get(player.getUUID());
        if (sessions != null) {
            sessions.playerMenuNonce = nonzeroNonce();
            sessions.ready = connection != null && connection.isUiEnabled();
        }
    }

    private static void disconnect(ServerPlayer player) {
        PlayerSessions sessions = PLAYERS.get(player.getUUID());
        if (sessions != null) {
            DesktopDebug.log("server disconnect close player={} sessions={}", player.getName().getString(), sessions.sessions.size());
            sessions.closeAll(player, false);
        }
        PLAYERS.remove(player.getUUID());
        CONNECTIONS.remove(player);
        PROTOCOL_REJECTIONS.remove(player);
        PENDING_USE_TARGETS.remove(player.getUUID());
    }

    private static void tick(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            PlayerSessions sessions = PLAYERS.get(player.getUUID());
            if (sessions != null && sessions.ready && canUseCustomWindows(player)) {
                sessions.tick(player);
            }
        }
    }

    private static long nonzeroNonce() {
        long nonce;
        do {
            nonce = NONCE_RANDOM.nextLong();
        } while (nonce == 0L);
        return nonce;
    }

    private static Set<String> validateForcedMenuIds(List<String> requested) {
        java.util.LinkedHashSet<String> validated = new java.util.LinkedHashSet<>();
        for (String value : requested) {
            ResourceLocation id = ResourceLocation.tryParse(value);
            if (id != null && BuiltInRegistries.MENU.get(id) != null) {
                validated.add(id.toString());
            }
        }
        return Set.copyOf(validated);
    }

    private static long connectionNonce(ServerPlayer player) {
        DesktopConnectionState connection = CONNECTIONS.get(player);
        return connection == null ? 0L : connection.connectionNonce();
    }

    private static boolean authorizeMutation(ServerPlayer player, PlayerSessions sessions, int sessionId, MutationStamp stamp) {
        return authorizeMutation(player, sessions, sessionId, stamp, capabilityForSession(sessionId));
    }

    private static boolean authorizeMutation(ServerPlayer player, PlayerSessions sessions, int sessionId, MutationStamp stamp, long requiredCapabilities) {
        DesktopConnectionState connection = CONNECTIONS.get(player);
        if (connection == null
            || !connection.authorizes(stamp.connectionNonce(), requiredCapabilities, true)
            || !player.isAlive()
            || player.isRemoved()
            || player.isSpectator()) {
            return false;
        }
        if (!sessions.mutationBucket.tryConsume(System.nanoTime())) {
            resyncAuthoritative(player, sessions, sessionId);
            return false;
        }

        AbstractContainerMenu menu;
        long sessionNonce;
        if (sessionId == DesktopPackets.PLAYER_MENU_SESSION) {
            menu = player.inventoryMenu;
            sessionNonce = sessions.playerMenuNonce;
        } else {
            Session session = sessions.sessions.get(sessionId);
            if (session == null || !session.visibleToClient || !session.menu.stillValid(player)) {
                return false;
            }
            menu = session.menu;
            sessionNonce = session.sessionNonce;
        }
        if (stamp.sessionNonce() != sessionNonce || stamp.expectedStateId() != menu.getStateId()) {
            resyncAuthoritative(player, sessions, sessionId);
            return false;
        }
        setSharedCarried(player, sessions, player.inventoryMenu.getCarried());
        return true;
    }

    private static boolean authorizeSession(ServerPlayer player, PlayerSessions sessions, int sessionId, MutationStamp stamp, boolean requireVisible) {
        return authorizeSession(player, sessions, sessionId, stamp, requireVisible, DesktopProtocol.CAP_CUSTOM_WINDOWS);
    }

    private static boolean authorizeSession(ServerPlayer player, PlayerSessions sessions, int sessionId, MutationStamp stamp, boolean requireVisible, long requiredCapabilities) {
        DesktopConnectionState connection = CONNECTIONS.get(player);
        if (connection == null
            || !connection.authorizes(stamp.connectionNonce(), requiredCapabilities, true)
            || !player.isAlive()
            || player.isRemoved()
            || player.isSpectator()) {
            return false;
        }
        if (!sessions.controlBucket.tryConsume(System.nanoTime())) {
            return false;
        }
        Session session = sessions.sessions.get(sessionId);
        if (session == null || (requireVisible && (!session.visibleToClient || !session.menu.stillValid(player)))) {
            return false;
        }
        if (stamp.sessionNonce() != session.sessionNonce) {
            resyncAuthoritative(player, sessions, sessionId);
            return false;
        }
        return true;
    }

    private static long capabilityForSession(int sessionId) {
        return sessionId == DesktopPackets.PLAYER_MENU_SESSION
            ? DesktopProtocol.CAP_INVENTORY_TOPOLOGY
            : DesktopProtocol.CAP_CUSTOM_WINDOWS;
    }

    private static void resyncAuthoritative(ServerPlayer player, PlayerSessions sessions, int sessionId) {
        if (!sessions.resyncBucket.tryConsume(System.nanoTime())) {
            return;
        }
        if (sessionId == DesktopPackets.PLAYER_MENU_SESSION) {
            InventoryExpansion.syncToClient(player);
            syncPlayerMenu(player);
            syncCarried(player, sessions);
            return;
        }
        Session session = sessions.sessions.get(sessionId);
        if (session == null || !session.visibleToClient) {
            return;
        }
        session.menu.sendAllDataToRemote();
        syncCraftingResultSlot(player, session);
        syncMerchantOffers(player, session);
        syncCarried(player, sessions);
    }

    private static void purchaseInventorySlot(ServerPlayer player, InventorySlotPurchasePayload payload) {
        PlayerSessions sessions = PLAYERS.get(player.getUUID());
        if (sessions != null && authorizeMutation(player, sessions, DesktopPackets.PLAYER_MENU_SESSION, payload.authorization())) {
            InventoryExpansion.tryPurchase(player);
        }
    }

    private static void click(ServerPlayer player, DesktopClickPayload payload) {
        PlayerSessions sessions = PLAYERS.get(player.getUUID());
        if (sessions == null || !sessions.ready || !authorizeMutation(player, sessions, payload.sessionId(), payload.authorization())) {
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
        PlayerSessions sessions = PLAYERS.get(player.getUUID());
        if (sessions == null || !sessions.ready || !authorizeMutation(player, sessions, DesktopPackets.PLAYER_MENU_SESSION, payload.authorization())) {
            DesktopDebug.trace("server carried dropped player={} reason=not-ready stack={}", player.getName().getString(), payload.carried());
            return;
        }

        if (!player.hasInfiniteMaterials()) {
            DesktopDebug.trace("server carried dropped player={} reason=not-creative stack={} serverCarried={}", player.getName().getString(), payload.carried(), player.inventoryMenu.getCarried());
            syncCarried(player, sessions);
            return;
        }

        setSharedCarried(player, sessions, payload.carried());
        DesktopDebug.trace("server carried sync player={} stack={}", player.getName().getString(), player.inventoryMenu.getCarried());
        syncCarried(player, sessions);
    }

    private static void quickMove(ServerPlayer player, DesktopQuickMovePayload payload) {
        PlayerSessions sessions = PLAYERS.get(player.getUUID());
        if (sessions == null || !sessions.ready) {
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
        int authorizedTarget = payload.targetKind() == DesktopPackets.QUICK_TARGET_SESSION
            ? payload.targetSessionId()
            : DesktopPackets.PLAYER_MENU_SESSION;
        if (!authorizeMutation(player, sessions, payload.sourceSessionId(), payload.sourceAuthorization())
            || !authorizeMutation(player, sessions, authorizedTarget, payload.targetAuthorization())) {
            DesktopDebug.trace("server quick move dropped player={} sourceSession={} reason=not-ready", player.getName().getString(), payload.sourceSessionId());
            return;
        }

        SlotSource source = resolveSlot(player, sessions, payload.sourceSessionId(), payload.sourceSlotIndex());
        if (source == null) {
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
            if (!carriedBeforeQuickMove.isEmpty()) {
                setSharedCarried(player, sessions, ItemStack.EMPTY);
            }
            try {
                clickMenu(0, player, sessions, source.menu, payload.sourceSlotIndex(), 0, ClickType.QUICK_MOVE, ItemStack.EMPTY);
            } finally {
                if (!carriedBeforeQuickMove.isEmpty()) {
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

        sessions.broadcastAll(player);
        return true;
    }

    private static boolean isVanillaResultSource(SlotSource source, int slotIndex) {
        if (source.menu instanceof RecipeBookMenu<?, ?> recipeMenu && source.menu.slots.indexOf(source.slot) == recipeMenu.getResultSlotIndex()) {
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
        PlayerSessions sessions = PLAYERS.get(player.getUUID());
        if (sessions == null || !sessions.ready || !authorizeMutation(player, sessions, payload.sessionId(), payload.authorization())) {
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
            DesktopDebug.warn("server button failed player={} session={} button={} reason={}", player.getName().getString(), payload.sessionId(), payload.buttonId(), exception.toString());
            syncCarried(player, sessions);
            return;
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
            sessions.broadcastAll(player);
        } else {
            syncCarried(player, sessions);
        }
    }

    private static void placeRecipe(ServerPlayer player, DesktopPlaceRecipePayload payload) {
        PlayerSessions sessions = PLAYERS.get(player.getUUID());
        if (sessions == null || !sessions.ready || !authorizeMutation(
            player, sessions, payload.sessionId(), payload.authorization(), capabilityForSession(payload.sessionId()) | DesktopProtocol.CAP_RECIPE_TRANSFER
        )) {
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

        RecipeHolder<?> recipe = server.getRecipeManager().byKey(payload.recipeId()).orElse(null);
        if (recipe == null) {
            DesktopDebug.trace("server recipe place dropped player={} session={} recipe={} reason=missing-recipe", player.getName().getString(), payload.sessionId(), payload.recipeId());
            return;
        }

        if (!player.getRecipeBook().contains(recipe)) {
            DesktopDebug.trace("server recipe place dropped player={} session={} recipe={} recipeKey={} reason=not-unlocked", player.getName().getString(), payload.sessionId(), payload.recipeId(), recipe.id());
            return;
        }
        if (recipe.value().isIncomplete()) {
            DesktopDebug.trace("server recipe place dropped player={} session={} recipe={} recipeKey={} reason=incomplete", player.getName().getString(), payload.sessionId(), payload.recipeId(), recipe.id());
            return;
        }

        boolean canCraft = player.isCreative() || canCraftRecipe(recipeBookMenu, player, recipe);
        ItemStack carriedBefore = player.inventoryMenu.getCarried().copy();
        boolean committedCarried = false;
        try {
            session.menu.setCarried(carriedBefore.copy());
            recipeBookMenu.handlePlacement(payload.useMaxItems(), recipe, player);
            player.inventoryMenu.setCarried(session.menu.getCarried().copy());
            committedCarried = true;
        } catch (RuntimeException exception) {
            player.inventoryMenu.setCarried(carriedBefore.copy());
            DesktopDebug.warn("server recipe place failed player={} session={} recipe={} reason={}", player.getName().getString(), payload.sessionId(), payload.recipeId(), exception.toString());
            syncCarried(player, sessions);
            return;
        } finally {
            if (!committedCarried) {
                player.inventoryMenu.setCarried(carriedBefore.copy());
            }
            clearDetachedCarried(sessions);
        }

        DesktopDebug.trace(
            "server recipe place player={} session={} recipe={} recipeKey={} useMax={} canCraft={} carried={}",
            player.getName().getString(),
            payload.sessionId(),
            payload.recipeId(),
            recipe.id(),
            payload.useMaxItems(),
            canCraft,
            player.inventoryMenu.getCarried()
        );

        if (!canCraft) {
            send(player, new DesktopGhostRecipePayload(outboundStamp(player, session.sessionNonce, session.menu.getStateId()), session.sessionId, recipe.id()));
        }
        sessions.broadcastAll(player);
    }

    private static void transferJeiRecipe(ServerPlayer player, DesktopJeiTransferPayload payload) {
        PlayerSessions sessions = PLAYERS.get(player.getUUID());
        if (sessions == null || !sessions.ready || !authorizeMutation(
            player, sessions, payload.targetSessionId(), payload.authorization(), capabilityForSession(payload.targetSessionId()) | DesktopProtocol.CAP_RECIPE_TRANSFER
        )) {
            DesktopDebug.trace("server JEI transfer dropped player={} targetSession={} reason=not-ready", player.getName().getString(), payload.targetSessionId());
            return;
        }
        if (!sessions.expensiveBucket.tryConsume(System.nanoTime())) {
            return;
        }
        if (player.isSpectator()) {
            DesktopDebug.trace("server JEI transfer dropped player={} targetSession={} reason=spectator", player.getName().getString(), payload.targetSessionId());
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

        MinecraftServer server = player.level().getServer();
        RecipeHolder<?> recipe = server == null ? null : server.getRecipeManager().byKey(payload.recipeId()).orElse(null);
        if (recipe == null || recipe.value().isIncomplete()) {
            DesktopDebug.trace("server JEI transfer dropped player={} recipe={} reason=unresolved-recipe", player.getName().getString(), payload.recipeId());
            return;
        }

        if (target.menu() instanceof RecipeBookMenu recipeBookMenu) {
            ItemStack carriedBefore = player.inventoryMenu.getCarried().copy();
            boolean committedCarried = false;
            try {
                target.menu().setCarried(carriedBefore.copy());
                recipeBookMenu.handlePlacement(payload.maxTransfer(), recipe, player);
                player.inventoryMenu.setCarried(target.menu().getCarried().copy());
                committedCarried = true;
            } catch (RuntimeException exception) {
                player.inventoryMenu.setCarried(carriedBefore.copy());
                DesktopDebug.warn("server JEI recipe-book transfer failed player={} targetSession={} recipe={} reason={}", player.getName().getString(), payload.targetSessionId(), payload.recipeId(), exception.toString());
                syncCarried(player, sessions);
                return;
            } finally {
                if (!committedCarried) {
                    player.inventoryMenu.setCarried(carriedBefore.copy());
                }
                clearDetachedCarried(sessions);
            }
            sessions.broadcastAll(player);
            return;
        }

        Session targetSession = target.session();
        if (targetSession == null || targetSession.serverHandler == null
            || !DesktopTransferValidators.supports(targetSession.serverHandler)) {
            DesktopDebug.trace("server JEI transfer dropped player={} targetSession={} reason=unsupported", player.getName().getString(), payload.targetSessionId());
            return;
        }
        DesktopTransferDecision decision;
        boolean validationCompleted = false;
        sessions.enterCallback(player, targetSession);
        try {
            decision = DesktopTransferValidators.validate(
                targetSession.serverHandler,
                new DesktopTransferRequest<>(new ServerSessionContext(player, sessions, targetSession), recipe.value(), payload.maxTransfer())
            );
            validationCompleted = true;
        } catch (RuntimeException exception) {
            targetSession.quarantineServerHandler(player, "validate-transfer", exception);
            return;
        } finally {
            sessions.exitCallback(player, validationCompleted);
        }
        if (!decision.allowed()) {
            return;
        }

        List<Slot> recipeSlots = resolveJeiTransferRecipeSlots(target.menu(), decision.destinationSlots());
        if (recipeSlots == null || recipeSlots.isEmpty()) {
            DesktopDebug.trace("server JEI transfer dropped player={} targetSession={} reason=bad-recipe-slots", player.getName().getString(), payload.targetSessionId());
            return;
        }

        Map<Integer, Slot> recipeSlotsById = new HashMap<>();
        Set<Slot> recipeSlotSet = new HashSet<>();
        for (int i = 0; i < recipeSlots.size(); i++) {
            Slot slot = recipeSlots.get(i);
            recipeSlotsById.put(decision.destinationSlots().get(i), slot);
            recipeSlotSet.add(slot);
        }

        List<JeiTransferRequirement> requirements = resolveApprovedTransferRequirements(decision.requirements(), recipeSlotsById);
        if (requirements == null || requirements.isEmpty()) {
            DesktopDebug.trace("server JEI transfer dropped player={} targetSession={} reason=bad-requirements", player.getName().getString(), payload.targetSessionId());
            return;
        }

        List<Slot> sourceSlots = jeiTransferSourceSlots(player, sessions, payload.targetSessionId(), recipeSlotSet);
        JeiTransferSimulation simulation = simulateJeiTransfer(player, recipeSlots, requirements, sourceSlots, payload.maxTransfer(), decision.maximumCrafts());
        if (simulation == null) {
            DesktopDebug.trace("server JEI transfer dropped player={} targetSession={} reason=simulation-failed", player.getName().getString(), payload.targetSessionId());
            return;
        }

        applyJeiTransferSimulation(simulation);
        sessions.broadcastAll(player);
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

    private static @Nullable List<JeiTransferRequirement> resolveApprovedTransferRequirements(
        List<DesktopTransferRequirement> approved,
        Map<Integer, Slot> recipeSlotsById
    ) {
        List<JeiTransferRequirement> requirements = new ArrayList<>(approved.size());
        int inputIndex = 0;
        for (DesktopTransferRequirement requirement : approved) {
            Slot target = recipeSlotsById.get(requirement.targetSlotId());
            if (target == null) {
                return null;
            }
            List<ItemStack> alternatives = requirement.alternatives().stream()
                .filter(stack -> !stack.isEmpty())
                .map(stack -> stack.copyWithCount(requirement.count()))
                .toList();
            if (alternatives.isEmpty()) {
                return null;
            }
            requirements.add(new JeiTransferRequirement(inputIndex++, requirement.targetSlotId(), target, alternatives));
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

        return planBoundedTransfer(sourceStacks, requirements, targetStacks, maxTransfer, maximumCrafts);
    }

    private static @Nullable JeiTransferSimulation planBoundedTransfer(
        Map<Slot, ItemStack> sourceStacks,
        List<JeiTransferRequirement> requirements,
        Map<Slot, ItemStack> targetStacks,
        boolean maximum,
        int maximumCrafts
    ) {
        Map<TransferStackKey, Integer> supply = new LinkedHashMap<>();
        Map<TransferStackKey, ItemStack> representatives = new LinkedHashMap<>();
        for (ItemStack stack : sourceStacks.values()) {
            if (stack.isEmpty()) {
                continue;
            }
            TransferStackKey key = transferStackKey(stack);
            supply.merge(key, stack.getCount(), Math::addExact);
            representatives.putIfAbsent(key, stack.copyWithCount(1));
        }

        List<BoundedTransferPlanner.Requirement<TransferStackKey>> plannedRequirements = new ArrayList<>(requirements.size());
        Map<Integer, Slot> targets = new HashMap<>();
        for (JeiTransferRequirement requirement : requirements) {
            int units = requirement.alternatives().get(0).getCount();
            List<TransferStackKey> alternatives = new ArrayList<>();
            Map<TransferStackKey, Integer> maximumUnits = new LinkedHashMap<>();
            Slot target = requirement.targetSlot();
            ItemStack existing = targetStacks.getOrDefault(target, ItemStack.EMPTY);
            int initialUnits = existing.getCount();
            List<ItemStack> allowedStacks = existing.isEmpty()
                ? requirement.alternatives()
                : List.of(existing.copyWithCount(units));
            for (ItemStack alternative : allowedStacks) {
                if (alternative.getCount() != units) {
                    return null;
                }
                TransferStackKey key = transferStackKey(alternative);
                alternatives.add(key);
                ItemStack representative = alternative.copyWithCount(1);
                representatives.putIfAbsent(key, representative);
                maximumUnits.put(key, Math.min(representative.getMaxStackSize(), target.getMaxStackSize(representative)));
            }
            if (targets.put(requirement.targetMenuSlotId(), target) != null) {
                return null;
            }
            plannedRequirements.add(new BoundedTransferPlanner.Requirement<>(
                requirement.targetMenuSlotId(), units, initialUnits, alternatives, maximumUnits
            ));
        }

        BoundedTransferPlanner<TransferStackKey> planner = new BoundedTransferPlanner<>(Comparator.comparing(TransferStackKey::sortKey));
        int craftLimit = Math.min(maximumCrafts, DesktopProtocol.MAX_TRANSFER_CRAFTS);
        if (craftLimit <= 0) {
            return null;
        }
        Optional<BoundedTransferPlanner.Plan<TransferStackKey>> planned = maximum
            ? planner.planMaximum(supply, plannedRequirements, craftLimit)
            : planner.planExact(supply, plannedRequirements, 1);
        if (planned.isEmpty()) {
            return null;
        }
        BoundedTransferPlanner.Plan<TransferStackKey> plan = planned.get();

        Map<Slot, ItemStack> sources = copyJeiTransferStacks(sourceStacks);
        Map<Slot, ItemStack> outputs = copyJeiTransferStacks(targetStacks);
        for (BoundedTransferPlanner.Allocation<TransferStackKey> allocation : plan.allocations()) {
            if (allocation.units().size() != 1) {
                return null;
            }
            Map.Entry<TransferStackKey, Integer> selected = allocation.units().entrySet().iterator().next();
            ItemStack representative = representatives.get(selected.getKey());
            Slot target = targets.get(allocation.targetId());
            if (representative == null || target == null || !consumeTransferUnits(sources, selected.getKey(), selected.getValue())) {
                return null;
            }
            ItemStack existing = outputs.getOrDefault(target, ItemStack.EMPTY);
            if (!existing.isEmpty() && !ItemStack.isSameItemSameComponents(existing, representative)) {
                return null;
            }
            ItemStack result = existing.isEmpty() ? representative.copyWithCount(selected.getValue()) : existing.copy();
            if (!existing.isEmpty()) {
                result.grow(selected.getValue());
            }
            if (!target.mayPlace(result) || result.getCount() > Math.min(result.getMaxStackSize(), target.getMaxStackSize(result))) {
                return null;
            }
            outputs.put(target, result);
        }
        return new JeiTransferSimulation(sources, outputs);
    }

    private static boolean consumeTransferUnits(Map<Slot, ItemStack> sources, TransferStackKey key, int units) {
        int remaining = units;
        for (ItemStack stack : sources.values()) {
            if (remaining <= 0) {
                break;
            }
            if (stack.isEmpty() || !transferStackKey(stack).equals(key)) {
                continue;
            }
            int consumed = Math.min(stack.getCount(), remaining);
            stack.shrink(consumed);
            remaining -= consumed;
        }
        return remaining == 0;
    }

    private static TransferStackKey transferStackKey(ItemStack stack) {
        return new TransferStackKey(stack.getItem(), stack.getComponentsPatch());
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

    private static boolean canCraftRecipe(RecipeBookMenu recipeBookMenu, ServerPlayer player, RecipeHolder<?> recipe) {
        StackedContents contents = new StackedContents();
        player.getInventory().fillStackedContents(contents);
        recipeBookMenu.fillCraftSlotsStackedContents(contents);
        return contents.canCraft(recipe.value(), null);
    }

    private static void rename(ServerPlayer player, DesktopRenamePayload payload) {
        PlayerSessions sessions = PLAYERS.get(player.getUUID());
        if (sessions == null || !sessions.ready || !authorizeMutation(player, sessions, payload.sessionId(), payload.authorization())) {
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
            sessions.broadcastAll(player);
        }
    }

    private static void customPayload(ServerPlayer player, DesktopCustomPayload payload) {
        PlayerSessions sessions = PLAYERS.get(player.getUUID());
        if (sessions == null || !sessions.ready || !authorizeMutation(player, sessions, payload.sessionId(), payload.authorization())) {
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
        if (session.serverCallbacksQuarantined) {
            DesktopDebug.trace("server custom dropped player={} session={} channel={} reason=handler-quarantined", player.getName().getString(), payload.sessionId(), payload.channel());
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
            payload.data().length
        );
        boolean completed = false;
        sessions.enterCallback(player, session);
        try {
            handler.handle(new ServerPayloadContext(player, sessions, session, payload));
            completed = true;
        } catch (RuntimeException exception) {
            session.quarantineServerHandler(player, "custom-payload:" + payload.channel(), exception);
        } finally {
            sessions.exitCallback(player, completed);
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

        if (slotIndex < 0 || slotIndex >= menu.slots.size()) {
            DesktopDebug.trace("server quick move dropped player={} session={} slot={} reason=out-of-range", player.getName().getString(), sessionId, slotIndex);
            return null;
        }

        return new SlotSource(sessionId, menu, menu.slots.get(slotIndex), session);
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

    private static boolean moveSlotStack(ServerPlayer player, net.minecraft.world.inventory.Slot sourceSlot, List<net.minecraft.world.inventory.Slot> targets) {
        if (!sourceSlot.isActive() || sourceSlot.isFake() || !sourceSlot.hasItem() || !sourceSlot.mayPickup(player)) {
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
            sourceSlot.setByPlayer(ItemStack.EMPTY, original);
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
            if (target == sourceSlot || !target.isActive() || target.isFake() || !target.hasItem()) {
                continue;
            }
            if (!ItemStack.isSameItemSameComponents(moving, target.getItem())) {
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
            if (target == sourceSlot || !target.isActive() || target.isFake() || target.hasItem()) {
                continue;
            }
            target.safeInsert(moving);
        }
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
        if (slotIndex != AbstractContainerMenu.SLOT_CLICKED_OUTSIDE && (slotIndex < 0 || slotIndex >= menu.slots.size())) {
            DesktopDebug.trace("server click ignored id={} player={} menu={} slot={} reason=out-of-range", debugId, player.getName().getString(), menu.containerId, slotIndex);
            return;
        }

        ItemStack slotBefore = serverSlotStack(menu, slotIndex);
        ItemStack carriedBefore = player.inventoryMenu.getCarried().copy();
        ItemStack menuCarriedBefore = menu.getCarried().copy();
        ItemStack effectiveCarried = player.hasInfiniteMaterials() ? clientCarried.copy() : player.inventoryMenu.getCarried().copy();
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
            player.hasInfiniteMaterials()
        );
        boolean committedCarried = false;
        try {
            menu.setCarried(effectiveCarried);
            menu.clicked(slotIndex, button, input, player);
            player.inventoryMenu.setCarried(menu.getCarried().copy());
            committedCarried = true;
        } catch (RuntimeException exception) {
            player.inventoryMenu.setCarried(carriedBefore.copy());
            DesktopDebug.warn(
                "server click failed id={} player={} menu={} slot={} button={} input={} reason={}",
                debugId, player.getName().getString(), menu.containerId, slotIndex, button, input, exception.toString()
            );
            syncCarried(player, sessions);
            return;
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
        if (slotIndex < 0 || slotIndex >= menu.slots.size()) {
            return ItemStack.EMPTY;
        }

        return menu.slots.get(slotIndex).getItem().copy();
    }

    private static MutationStamp outboundStamp(ServerPlayer player, long sessionNonce, int stateId) {
        return new MutationStamp(connectionNonceFor(player), sessionNonce, stateId);
    }

    private static void syncCarried(ServerPlayer player, PlayerSessions sessions) {
        ItemStack canonical = player.inventoryMenu.getCarried().copy();
        DesktopDebug.trace("server sync carried player={} stack={}", player.getName().getString(), canonical);
        send(player, new DesktopCarriedPayload(outboundStamp(player, sessions.playerMenuNonce, player.inventoryMenu.getStateId()), canonical));
    }

    private static void syncPlayerMenu(ServerPlayer player) {
        PlayerSessions current = PLAYERS.get(player.getUUID());
        if (current == null) {
            return;
        }
        InventoryExpansion.appendMissingMenuSlots(player.inventoryMenu, player);
        int stateId = player.inventoryMenu.getStateId();
        for (int slotIndex = 0; slotIndex < player.inventoryMenu.slots.size(); slotIndex++) {
            Slot slot = player.inventoryMenu.slots.get(slotIndex);
            send(player, new DesktopSlotPayload(
                outboundStamp(player, current.playerMenuNonce, stateId),
                DesktopPackets.PLAYER_MENU_SESSION,
                slotIndex,
                stateId,
                slot.getItem().copy()
            ));
        }
        DesktopDebug.trace("server sync player-menu player={} slots={}", player.getName().getString(), player.inventoryMenu.slots.size());
    }

    private static void setSharedCarried(ServerPlayer player, PlayerSessions sessions, ItemStack stack) {
        player.inventoryMenu.setCarried(stack.copy());
        clearDetachedCarried(sessions);
    }

    private static void clearDetachedCarried(PlayerSessions sessions) {
        for (Session session : sessions.sessions.values()) {
            session.menu.setCarried(ItemStack.EMPTY);
        }
    }

    public static void syncCraftingResult(ServerPlayer player, CraftingMenu menu) {
        PlayerSessions sessions = PLAYERS.get(player.getUUID());
        if (sessions == null || !sessions.ready || !canUseCustomWindows(player)) {
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
        if (!(session.menu instanceof RecipeBookMenu<?, ?> recipeMenu)) {
            return;
        }

        int slotIndex = recipeMenu.getResultSlotIndex();
        if (slotIndex < 0) {
            return;
        }
        net.minecraft.world.inventory.Slot resultSlot = session.menu.getSlot(slotIndex);

        send(player, new DesktopSlotPayload(outboundStamp(player, session.sessionNonce, session.menu.getStateId()), session.sessionId, slotIndex, session.menu.getStateId(), resultSlot.getItem().copy()));
    }

    private static void syncMerchantOffers(ServerPlayer player, Session session) {
        if (!(session.menu instanceof MerchantMenu merchantMenu)) {
            return;
        }

        send(player, new DesktopMerchantOffersPayload(
            outboundStamp(player, session.sessionNonce, session.menu.getStateId()),
            session.sessionId,
            merchantMenu.getOffers(),
            merchantMenu.getTraderLevel(),
            merchantMenu.getTraderXp(),
            merchantMenu.showProgressBar(),
            merchantMenu.canRestock()
        ));
    }

    private static void closeSession(ServerPlayer player, DesktopCloseSessionPayload payload, boolean notifyClient) {
        PlayerSessions sessions = PLAYERS.get(player.getUUID());
        if (sessions != null && authorizeSession(player, sessions, payload.sessionId(), payload.authorization(), false)) {
            DesktopDebug.log("server close request player={} session={} notify={}", player.getName().getString(), payload.sessionId(), notifyClient);
            sessions.close(player, payload.sessionId(), notifyClient);
        }
    }

    private static void setSessionPin(ServerPlayer player, DesktopSessionPinPayload payload) {
        PlayerSessions sessions = PLAYERS.get(player.getUUID());
        if (sessions == null || !sessions.ready || !authorizeSession(player, sessions, payload.sessionId(), payload.authorization(), false)) {
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
        session.dispatchPinChanged(player);
    }

    private static void setSessionVisibility(ServerPlayer player, DesktopSessionVisibilityPayload payload) {
        PlayerSessions sessions = PLAYERS.get(player.getUUID());
        if (sessions == null || !sessions.ready || !authorizeSession(player, sessions, payload.sessionId(), payload.authorization(), false)) {
            DesktopDebug.trace("server visibility dropped player={} session={} reason=not-ready", player.getName().getString(), payload.sessionId());
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

    private static void linkSessions(ServerPlayer player, DesktopLinkSessionsPayload payload) {
        PlayerSessions sessions = PLAYERS.get(player.getUUID());
        int originSessionId = sessions == null ? -1 : sessionIdForAuthorization(sessions, payload.originAuthorization());
        Session origin = sessions == null ? null : sessions.sessions.get(originSessionId);
        if (sessions == null || !sessions.ready || origin == null || !isBlockBackedSourceKey(origin.sourceKey)
            || !authorizeSession(player, sessions, originSessionId, payload.originAuthorization(), true, DesktopProtocol.CAP_CUSTOM_WINDOWS | DesktopProtocol.CAP_LINK_GRAPH)
            || !sessions.isSourceAuthorized(player, origin.sourceKey)) {
            return;
        }
        if (!sessions.expensiveBucket.tryConsume(System.nanoTime())) {
            return;
        }

        if (payload.action() == DesktopPackets.LINK_ACTION_CLEAR_ORIGIN) {
            for (String target : sessions.linkGraph.snapshot().getOrDefault(origin.sourceKey, Set.of())) {
                sessions.linkGraph.unlink(origin.sourceKey, target);
            }
            return;
        }

        int targetSessionId = sessionIdForAuthorization(sessions, payload.targetAuthorization());
        Session target = sessions.sessions.get(targetSessionId);
        if (target == null || target == origin || !isBlockBackedSourceKey(target.sourceKey)
            || !authorizeSession(player, sessions, targetSessionId, payload.targetAuthorization(), true, DesktopProtocol.CAP_CUSTOM_WINDOWS | DesktopProtocol.CAP_LINK_GRAPH)
            || !sessions.isSourceAuthorized(player, target.sourceKey)) {
            return;
        }
        if (payload.action() == DesktopPackets.LINK_ACTION_LINK) {
            sessions.linkGraph.link(origin.sourceKey, target.sourceKey);
        } else {
            sessions.linkGraph.unlink(origin.sourceKey, target.sourceKey);
        }
    }

    private static void openLinkedSources(ServerPlayer player, DesktopOpenLinkedSourcesPayload payload) {
        PlayerSessions sessions = PLAYERS.get(player.getUUID());
        int originSessionId = sessions == null ? -1 : sessionIdForAuthorization(sessions, payload.authorization());
        Session origin = sessions == null ? null : sessions.sessions.get(originSessionId);
        if (sessions == null || !sessions.ready || origin == null || origin.sourceKey.isBlank()
            || !authorizeSession(player, sessions, originSessionId, payload.authorization(), true, DesktopProtocol.CAP_CUSTOM_WINDOWS | DesktopProtocol.CAP_LINK_GRAPH)
            || !sessions.isSourceAuthorized(player, origin.sourceKey)) {
            DesktopDebug.trace("server linked open dropped player={} reason=not-ready", player.getName().getString());
            return;
        }
        if (!sessions.expensiveBucket.tryConsume(System.nanoTime())) {
            return;
        }

        for (String sourceKey : sessions.linkGraph.connectedComponent(origin.sourceKey, DesktopProtocol.MAX_LINK_NODES)) {
            if (sourceKey.equals(origin.sourceKey) || !isBlockBackedSourceKey(sourceKey)) {
                continue;
            }

            Session existing = sessions.sessionForSourceKey(sourceKey);
            if (existing != null) {
                if (!existing.visibleToClient) {
                    sessions.setVisible(player, existing, true, true);
                }
                continue;
            }

            MenuProvider provider = providerForDormantGhost(player, sessions, sourceKey);
            if (provider == null) {
                DesktopDebug.trace("server linked open skipped player={} source={} reason=unavailable", player.getName().getString(), sourceKey);
                continue;
            }

            DesktopDebug.log("server linked open player={} source={} title={}", player.getName().getString(), sourceKey, provider.getDisplayName().getString());
            openMenuSession(player, provider, sourceKey, false, false, true);
        }
    }

    private static int sessionIdForAuthorization(PlayerSessions sessions, MutationStamp authorization) {
        for (Session session : sessions.sessions.values()) {
            if (session.sessionNonce == authorization.sessionNonce()) {
                return session.sessionId;
            }
        }
        return -1;
    }

    private static int nextSessionId(ServerPlayer player) {
        PlayerSessions sessions = sessions(player);
        for (int attempt = 0; attempt <= DesktopProtocol.MAX_DESKTOP_SESSIONS; attempt++) {
            int candidate = sessions.nextSessionId <= 0 ? 1 : sessions.nextSessionId;
            sessions.nextSessionId = candidate == Integer.MAX_VALUE ? 1 : candidate + 1;
            if (!sessions.sessions.containsKey(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("No free desktop session identifier");
    }

    private static PlayerSessions sessions(ServerPlayer player) {
        return PLAYERS.computeIfAbsent(player.getUUID(), uuid -> new PlayerSessions());
    }

    private static void send(ServerPlayer player, CustomPacketPayload payload) {
        boolean canSend;
        try {
            canSend = ServerPlayNetworking.canSend(player, payload.type());
        } catch (IllegalArgumentException | IllegalStateException exception) {
            DesktopDebug.warn(
                "server packet send failed player={} type={} reason={}",
                player.getName().getString(),
                payload.type().id(),
                exception.toString()
            );
            return;
        }

        if (canSend) {
            DesktopDebug.trace("server packet send player={} type={}", player.getName().getString(), payload.type().id());
            ServerPlayNetworking.send(player, payload);
        } else {
            PlayerSessions sessions = PLAYERS.get(player.getUUID());
            DesktopDebug.warn(
                "server packet not sent player={} type={} reason=client-cannot-receive ready={} sessions={}",
                player.getName().getString(),
                payload.type().id(),
                sessions != null && sessions.ready,
                sessions == null ? 0 : sessions.sessions.size()
            );
        }
    }

    private static final class PlayerSessions {
        private final LinkedHashMap<Integer, Session> sessions = new LinkedHashMap<>();
        private final LinkedHashMap<String, DormantGhostSource> dormantGhostSources = new LinkedHashMap<>();
        private final LinkedHashMap<String, SourceGrant> sourceGrants = new LinkedHashMap<>();
        private final BoundedLinkGraph<String> linkGraph = new BoundedLinkGraph<>();
        private final TokenBucket modeBucket = new TokenBucket(1.0D, 2.0D, System.nanoTime());
        private final TokenBucket mutationBucket = new TokenBucket(40.0D, 80.0D, System.nanoTime());
        private final TokenBucket controlBucket = new TokenBucket(10.0D, 20.0D, System.nanoTime());
        private final TokenBucket expensiveBucket = new TokenBucket(2.0D, 4.0D, System.nanoTime());
        private final TokenBucket resyncBucket = new TokenBucket(1.0D, 2.0D, System.nanoTime());
        private Set<String> forcedMenuIds = Set.of();
        private boolean ready;
        private boolean closingForModeDisable;
        private long playerMenuNonce = nonzeroNonce();
        private int nextSessionId = 1;
        private int dormantGhostProbeTicks;
        private long lifecycleTicks;
        private int callbackDepth;
        private boolean pendingCallbackBroadcast;
        private boolean callbackFailed;
        private ItemStack callbackCarriedBefore = ItemStack.EMPTY;
        private final Set<AbstractContainerMenu> callbackMenus = new HashSet<>();

        private void add(ServerPlayer player, Session session) {
            while (this.sessions.size() >= DesktopProtocol.MAX_DESKTOP_SESSIONS) {
                Iterator<Integer> iterator = this.sessions.keySet().iterator();
                if (!iterator.hasNext()) {
                    break;
                }
                Integer sessionId = iterator.next();
                DesktopDebug.log("server session cap close player={} session={}", player.getName().getString(), sessionId);
                this.close(player, sessionId, true);
            }

            this.sessions.put(session.sessionId, session);
            session.initializeServerHandler(player, this);
            session.menu.setCarried(ItemStack.EMPTY);
            session.menu.setSynchronizer(new SessionSynchronizer(player, session));
            session.menu.sendAllDataToRemote();
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

            if (visible && !canRestoreHiddenSession(player, this, session)) {
                DesktopDebug.trace("server visibility restore rejected player={} session={} source={}", player.getName().getString(), session.sessionId, session.sourceKey);
                this.rememberDormantGhost(player, session, "visibility-invalid");
                this.close(player, session.sessionId, notifyClient);
                return;
            }

            session.visibleToClient = visible;
            DesktopDebug.log("server session visibility player={} session={} title={} visible={} notify={}", player.getName().getString(), session.sessionId, session.title.getString(), visible, notifyClient);
            if (notifyClient) {
                send(player, new DesktopSessionVisibilityPayload(outboundStamp(player, session.sessionNonce, session.menu.getStateId()), session.sessionId, visible));
            }
            if (visible) {
                session.menu.sendAllDataToRemote();
                syncCraftingResultSlot(player, session);
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
            session.dispatchClosed(player, this);
            ItemStack canonicalCarried = player.inventoryMenu.getCarried().copy();
            session.menu.setCarried(ItemStack.EMPTY);
            session.menu.removed(player);
            setSharedCarried(player, this, canonicalCarried);
            if (notifyClient) {
                send(player, new DesktopSessionClosedPayload(outboundStamp(player, session.sessionNonce, session.menu.getStateId()), sessionId));
            }
        }

        private void closeAll(ServerPlayer player, boolean notifyClient) {
            for (Integer sessionId : List.copyOf(this.sessions.keySet())) {
                this.close(player, sessionId, notifyClient);
            }
            this.dormantGhostSources.clear();
        }

        private void tick(ServerPlayer player) {
            this.lifecycleTicks++;
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

        private void enterCallback(ServerPlayer player, Session session) {
            if (this.callbackDepth == 0) {
                this.callbackCarriedBefore = player.inventoryMenu.getCarried().copy();
                this.callbackFailed = false;
            }
            this.callbackDepth++;
            this.callbackMenus.add(session.menu);
            session.menu.setCarried(player.inventoryMenu.getCarried().copy());
        }

        private void requestCallbackBroadcast(Session session) {
            if (this.callbackDepth <= 0 || !this.callbackMenus.contains(session.menu)) {
                return;
            }
            this.pendingCallbackBroadcast = true;
        }

        private void exitCallback(ServerPlayer player, boolean completed) {
            if (this.callbackDepth <= 0) {
                throw new IllegalStateException("Desktop callback depth underflow");
            }
            if (!completed) {
                this.callbackFailed = true;
            }
            if (--this.callbackDepth != 0) {
                return;
            }

            boolean flush = this.pendingCallbackBroadcast || this.callbackFailed;
            player.inventoryMenu.setCarried(this.callbackCarriedBefore.copy());
            for (AbstractContainerMenu menu : this.callbackMenus) {
                menu.setCarried(ItemStack.EMPTY);
            }
            clearDetachedCarried(this);
            this.callbackMenus.clear();
            this.pendingCallbackBroadcast = false;
            this.callbackFailed = false;
            this.callbackCarriedBefore = ItemStack.EMPTY;
            if (flush) {
                this.broadcastAll(player);
            }
        }

        private void rememberDormantGhost(ServerPlayer player, Session session, String reason) {
            if (!session.ghostPinned || !isBlockBackedSourceKey(session.sourceKey) || !this.isSourceAuthorized(player, session.sourceKey)) {
                return;
            }

            this.purgeExpiredDormantGhosts();
            this.dormantGhostSources.remove(session.sourceKey);
            while (this.dormantGhostSources.size() >= DesktopProtocol.MAX_DORMANT_SOURCES) {
                Iterator<String> iterator = this.dormantGhostSources.keySet().iterator();
                if (!iterator.hasNext()) {
                    break;
                }
                iterator.next();
                iterator.remove();
            }
            this.dormantGhostSources.put(session.sourceKey, new DormantGhostSource(
                session.sourceKey,
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
            if (this.dormantGhostProbeTicks % DesktopProtocol.DORMANT_PROBE_INTERVAL_TICKS != 0) {
                return;
            }

            for (DormantGhostSource dormant : List.copyOf(this.dormantGhostSources.values())) {
                if (this.sessions.size() >= DesktopProtocol.MAX_DESKTOP_SESSIONS) {
                    return;
                }
                if (this.hasSessionForSourceKey(dormant.sourceKey())) {
                    this.dormantGhostSources.remove(dormant.sourceKey());
                    continue;
                }

                if (!this.isSourceAuthorized(player, dormant.sourceKey())) {
                    this.dormantGhostSources.remove(dormant.sourceKey());
                    continue;
                }
                MenuProvider provider = providerForDormantGhost(player, this, dormant.sourceKey());
                if (provider == null) {
                    continue;
                }

                DesktopDebug.log("server dormant ghost reopen player={} source={} title={}", player.getName().getString(), dormant.sourceKey(), provider.getDisplayName().getString());
                OptionalInt opened = openMenuSession(player, provider, dormant.sourceKey(), false, true, false);
                if (opened != null && opened.isPresent()) {
                    this.dormantGhostSources.remove(dormant.sourceKey());
                }
            }
        }

        private void purgeExpiredDormantGhosts() {
            this.dormantGhostSources.values().removeIf(dormant -> dormant.expiresAtTick() <= this.lifecycleTicks);
        }

        private boolean authorizeSource(ServerPlayer player, String sourceKey) {
            SourceGrant grant = SourceGrant.capture(player, sourceKey);
            if (grant == null) {
                return false;
            }
            this.sourceGrants.remove(sourceKey);
            this.sourceGrants.put(sourceKey, grant);
            while (this.sourceGrants.size() > DesktopProtocol.MAX_LINK_NODES) {
                Iterator<String> iterator = this.sourceGrants.keySet().iterator();
                if (!iterator.hasNext()) {
                    break;
                }
                String oldest = iterator.next();
                iterator.remove();
                this.removeSourceAuthorization(oldest, false);
            }
            return true;
        }

        private boolean isSourceAuthorized(ServerPlayer player, String sourceKey) {
            SourceGrant grant = this.sourceGrants.get(sourceKey);
            if (grant == null || !grant.matches(player)) {
                this.removeSourceAuthorization(sourceKey, true);
                return false;
            }
            return true;
        }

        private void removeSourceAuthorization(String sourceKey, boolean removeGrant) {
            if (removeGrant) {
                this.sourceGrants.remove(sourceKey);
            }
            this.dormantGhostSources.remove(sourceKey);
            for (String neighbor : this.linkGraph.snapshot().getOrDefault(sourceKey, Set.of())) {
                this.linkGraph.unlink(sourceKey, neighbor);
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
        private final long sessionNonce;
        private final AbstractContainerMenu menu;
        private final Component title;
        private final int specialKind;
        private final int entityId;
        private final int columns;
        private final int menuTypeId;
        private final String sourceKey;
        private boolean ghostPinned;
        private boolean visibleToClient = true;
        private @Nullable DesktopServerWindowHandler<AbstractContainerMenu, Object> serverHandler;
        private @Nullable Object serverState;
        private boolean serverCallbacksQuarantined;

        private Session(int sessionId, AbstractContainerMenu menu, Component title, int specialKind, int entityId, int columns, int menuTypeId, String sourceKey) {
            this.sessionId = sessionId;
            this.sessionNonce = nonzeroNonce();
            this.menu = menu;
            this.title = title;
            this.specialKind = specialKind;
            this.entityId = entityId;
            this.columns = columns;
            this.menuTypeId = menuTypeId;
            this.sourceKey = sourceKey;
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

        private void initializeServerHandler(ServerPlayer player, PlayerSessions sessions) {
            MenuType<?> menuType = this.menuType();
            if (menuType == null) {
                return;
            }

            this.serverHandler = DesktopServerApi.findWindowHandler(menuType);
            if (this.serverHandler == null) {
                return;
            }

            boolean completed = false;
            sessions.enterCallback(player, this);
            try {
                this.serverState = this.serverHandler.createState(new ServerSessionContext(player, sessions, this));
                completed = true;
            } catch (RuntimeException exception) {
                this.quarantineServerHandler(player, "create-state", exception);
            } finally {
                sessions.exitCallback(player, completed);
            }
        }

        private void dispatchOpened(ServerPlayer player, PlayerSessions sessions) {
            if (this.serverHandler == null) {
                return;
            }
            boolean completed = false;
            sessions.enterCallback(player, this);
            try {
                this.serverHandler.opened(new ServerSessionContext(player, sessions, this));
                completed = true;
            } catch (RuntimeException exception) {
                this.quarantineServerHandler(player, "opened", exception);
            } finally {
                sessions.exitCallback(player, completed);
            }
        }

        private void dispatchTick(ServerPlayer player, PlayerSessions sessions) {
            if (this.serverHandler == null) {
                return;
            }
            boolean completed = false;
            sessions.enterCallback(player, this);
            try {
                this.serverHandler.tick(new ServerSessionContext(player, sessions, this));
                completed = true;
            } catch (RuntimeException exception) {
                this.quarantineServerHandler(player, "tick", exception);
            } finally {
                sessions.exitCallback(player, completed);
            }
        }

        private void dispatchClosed(ServerPlayer player, PlayerSessions sessions) {
            if (this.serverHandler == null) {
                return;
            }
            boolean completed = false;
            sessions.enterCallback(player, this);
            try {
                this.serverHandler.closed(new ServerSessionContext(player, sessions, this));
                completed = true;
            } catch (RuntimeException exception) {
                this.quarantineServerHandler(player, "closed", exception);
            } finally {
                sessions.exitCallback(player, completed);
            }
        }

        private void dispatchVisibilityChanged(ServerPlayer player, PlayerSessions sessions) {
            if (this.serverHandler == null) {
                return;
            }
            boolean completed = false;
            sessions.enterCallback(player, this);
            try {
                this.serverHandler.visibilityChanged(new ServerSessionContext(player, sessions, this), this.visibleToClient);
                completed = true;
            } catch (RuntimeException exception) {
                this.quarantineServerHandler(player, "visibility", exception);
            } finally {
                sessions.exitCallback(player, completed);
            }
        }

        private void dispatchPinChanged(ServerPlayer player) {
            if (this.serverHandler == null) {
                return;
            }
            PlayerSessions sessions = sessions(player);
            boolean completed = false;
            sessions.enterCallback(player, this);
            try {
                this.serverHandler.pinChanged(new ServerSessionContext(player, sessions, this), this.ghostPinned);
                completed = true;
            } catch (RuntimeException exception) {
                this.quarantineServerHandler(player, "pin", exception);
            } finally {
                sessions.exitCallback(player, completed);
            }
        }

        private void quarantineServerHandler(ServerPlayer player, String callback, RuntimeException exception) {
            if (!this.serverCallbacksQuarantined) {
                DesktopDebug.warn(
                    "server window handler quarantined player={} session={} title={} callback={} reason={}",
                    player.getName().getString(), this.sessionId, this.title.getString(), callback, exception.toString()
                );
            }
            this.serverCallbacksQuarantined = true;
            this.serverHandler = null;
            this.serverState = null;
        }

        private boolean transferSupported() {
            return this.menu instanceof RecipeBookMenu
                || this.serverHandler != null && DesktopTransferValidators.supports(this.serverHandler);
        }
    }

    private record SlotSource(int sessionId, AbstractContainerMenu menu, net.minecraft.world.inventory.Slot slot, @Nullable Session session) {
    }

    private record JeiTransferTarget(int sessionId, AbstractContainerMenu menu, @Nullable Session session) {
    }

    private record JeiTransferRequirement(int inputIndex, int targetMenuSlotId, Slot targetSlot, List<ItemStack> alternatives) {
    }

    private record JeiTransferSimulation(Map<Slot, ItemStack> sourceStacks, Map<Slot, ItemStack> targetStacks) {
    }

    private record TransferStackKey(net.minecraft.world.item.Item item, net.minecraft.core.component.DataComponentPatch components) {
        private String sortKey() {
            return BuiltInRegistries.ITEM.getKey(this.item) + "|" + this.components;
        }
    }

    private record JeiTransferSourceKey(Container container, int containerSlot) {
    }

    private record DormantGhostSource(String sourceKey, long expiresAtTick) {
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
            send(this.player, new DesktopCustomPayload(outboundStamp(this.player, this.session.sessionNonce, this.session.menu.getStateId()), this.session.sessionId, channel, data));
        }

        @Override
        public void broadcastChanges() {
            this.sessions.requestCallbackBroadcast(this.session);
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
            send(this.player, new DesktopCustomPayload(outboundStamp(this.player, this.session.sessionNonce, this.session.menu.getStateId()), this.session.sessionId, channel, data));
        }

        @Override
        public void broadcastChanges() {
            this.sessions.requestCallbackBroadcast(this.session);
        }
    }

    private static final class SessionSynchronizer implements ContainerSynchronizer {
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
            send(this.player, new DesktopOpenSessionPayload(
                connectionNonce(this.player),
                this.session.sessionId,
                this.session.sessionNonce,
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
            PlayerSessions current = PLAYERS.get(this.player.getUUID());
            if (current != null) {
                send(this.player, new DesktopCarriedPayload(outboundStamp(this.player, current.playerMenuNonce, this.player.inventoryMenu.getStateId()), canonicalCarried));
            }
        }

        @Override
        public void sendSlotChange(AbstractContainerMenu menu, int slot, ItemStack stack) {
            DesktopDebug.trace("server send slot player={} session={} slot={} stack={}", this.player.getName().getString(), this.session.sessionId, slot, stack);
            send(this.player, new DesktopSlotPayload(outboundStamp(this.player, this.session.sessionNonce, menu.getStateId()), this.session.sessionId, slot, menu.getStateId(), stack.copy()));
        }

        @Override
        public void sendCarriedChange(AbstractContainerMenu menu, ItemStack stack) {
            ItemStack canonicalCarried = this.canonicalCarried(menu);
            DesktopDebug.trace("server send carried player={} session={} stack={}", this.player.getName().getString(), this.session.sessionId, canonicalCarried);
            PlayerSessions current = PLAYERS.get(this.player.getUUID());
            if (current != null) {
                send(this.player, new DesktopCarriedPayload(outboundStamp(this.player, current.playerMenuNonce, this.player.inventoryMenu.getStateId()), canonicalCarried));
            }
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
            send(this.player, new DesktopDataPayload(outboundStamp(this.player, this.session.sessionNonce, menu.getStateId()), this.session.sessionId, dataSlotIndex, value));
        }
    }

    private static @Nullable String sourceKeyForProvider(ServerPlayer player, MenuProvider provider) {
        if (provider instanceof BlockEntity blockEntity) {
            return sourceKeyForBlock(player, blockEntity.getBlockPos());
        }

        return null;
    }

    private static boolean isBlockBackedSourceKey(String sourceKey) {
        return sourceKey.startsWith("block:") || sourceKey.startsWith("chest:");
    }

    private static @Nullable MenuProvider providerForDormantGhost(ServerPlayer player, PlayerSessions sessions, String sourceKey) {
        SourceKey source = SourceKey.parse(sourceKey);
        if (source == null
            || !source.dimension().equals(player.level().dimension().location().toString())
            || !sessions.isSourceAuthorized(player, sourceKey)) {
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
        if (!level.isInWorldBounds(pos) || !level.mayInteract(player, pos) || !level.hasChunkAt(pos)) {
            return false;
        }
        Vec3 target = Vec3.atCenterOf(pos);
        double range = player.blockInteractionRange();
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

    private static boolean canRestoreHiddenSession(ServerPlayer player, PlayerSessions sessions, Session session) {
        if (!player.isAlive() || player.isSpectator() || !session.menu.stillValid(player)) {
            return false;
        }
        if (!isBlockBackedSourceKey(session.sourceKey)) {
            return true;
        }
        return providerForDormantGhost(player, sessions, session.sourceKey) != null;
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
            for (String positionPart : positionsPart.split("\\|", -1)) {
                BlockPos pos = parseBlockPos(positionPart);
                if (pos == null) {
                    return null;
                }
                positions.add(pos);
            }

            int expectedPositions = kind.equals("chest") ? 2 : 1;
            return positions.size() == expectedPositions && positions.stream().distinct().count() == expectedPositions
                ? new SourceKey(kind, dimension, List.copyOf(positions))
                : null;
        }
    }

    private static @Nullable BlockPos parseBlockPos(String value) {
        String[] parts = value.split(",", -1);
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

    private record SourceGrant(String sourceKey, List<SourceBackingIdentity> backing) {
        private static @Nullable SourceGrant capture(ServerPlayer player, String sourceKey) {
            SourceKey source = SourceKey.parse(sourceKey);
            if (source == null || !source.dimension().equals(player.level().dimension().location().toString())) {
                return null;
            }
            ServerLevel level = player.serverLevel();
            for (BlockPos pos : source.positions()) {
                if (!level.isInWorldBounds(pos) || !level.hasChunkAt(pos)) {
                    return null;
                }
            }
            List<SourceBackingIdentity> backing = new ArrayList<>(source.positions().size());
            for (BlockPos pos : source.positions()) {
                BlockEntity blockEntity = level.getBlockEntity(pos);
                backing.add(new SourceBackingIdentity(
                    String.valueOf(BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock())),
                    blockEntity == null ? "" : String.valueOf(BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(blockEntity.getType())),
                    blockEntity == null ? null : new WeakReference<>(blockEntity)
                ));
            }
            return new SourceGrant(sourceKey, List.copyOf(backing));
        }

        private boolean matches(ServerPlayer player) {
            SourceKey source = SourceKey.parse(this.sourceKey);
            if (source == null
                || !source.dimension().equals(player.level().dimension().location().toString())
                || source.positions().size() != this.backing.size()) {
                return false;
            }
            ServerLevel level = player.serverLevel();
            for (BlockPos pos : source.positions()) {
                if (!level.isInWorldBounds(pos) || !level.hasChunkAt(pos)) {
                    return false;
                }
            }
            for (int index = 0; index < source.positions().size(); index++) {
                BlockPos pos = source.positions().get(index);
                BlockEntity blockEntity = level.getBlockEntity(pos);
                SourceBackingIdentity expected = this.backing.get(index);
                String blockId = String.valueOf(BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock()));
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
}
