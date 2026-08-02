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

import com.salts_inventory_update.platform.fabric.api.event.lifecycle.v1.ServerTickEvents;
import com.salts_inventory_update.platform.fabric.api.networking.v1.ServerPlayConnectionEvents;
import com.salts_inventory_update.platform.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.HashedStack;
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
import com.salts_inventory_update.network.DesktopPackets;
import com.salts_inventory_update.network.DesktopPackets.DesktopButtonPayload;
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
import com.salts_inventory_update.network.DesktopPackets.DesktopQuickMovePayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopHelloPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopHelloAckPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopReadyPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopModePayload;
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
    private static final int MAX_DORMANT_GHOST_SOURCES = DesktopProtocol.MAX_DORMANT_SOURCES;
    private static final int DORMANT_GHOST_REOPEN_INTERVAL_TICKS = 10;
    private static final int CRAFTER_INPUT_SLOT_COUNT = 9;
    private static final int CRAFTER_SLOT_STATE_ENABLED_FLAG = 16;
    private static final int MERCHANT_RESULT_SLOT = 2;
    private static final int BEACON_EFFECT_ID_MASK = 0xFFFF;
    private static final int BEACON_SECONDARY_EFFECT_SHIFT = 16;
    private static final Map<ServerPlayer, PlayerSessions> PLAYERS = new IdentityHashMap<>();
    private static final Map<ServerPlayer, String> PENDING_USE_TARGETS = new IdentityHashMap<>();
    private static final Set<ServerPlayer> PROTOCOL_REJECTED = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String PLAYER_LINK_NODE = "player";

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
        PlayerSessions sessions = PLAYERS.get(player);
        return sessions != null && sessions.isActive() && sessions.hasCapability(DesktopProtocol.CAP_CUSTOM_WINDOWS);
    }

    public static boolean isGameplayActive(ServerPlayer player) {
        PlayerSessions sessions = PLAYERS.get(player);
        return sessions != null
            && sessions.isGameplayActive()
            && sessions.hasCapability(DesktopProtocol.CAP_INVENTORY_TOPOLOGY);
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
        if (provider == null) {
            DesktopDebug.warn("server capture skipped player={} reason=null-provider", player.getName().getString());
            return OptionalInt.empty();
        }

        PlayerSessions sessions = PLAYERS.get(player);
        if (sessions == null || !sessions.isActive() || !sessions.hasCapability(DesktopProtocol.CAP_CUSTOM_WINDOWS)) {
            return null;
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
            || (payload.capabilities() & ~DesktopProtocol.KNOWN_CAPABILITIES) != 0L) {
            Component reason = Component.literal("Salt's Inventory Update versions do not match. Update the mod on both client and server.");
            DesktopDebug.warn(
                "server desktop handshake rejected player={} protocol={} expected={} capabilities={} required={}",
                player.getName().getString(), payload.protocolVersion(), DesktopProtocol.VERSION,
                payload.capabilities(), DesktopProtocol.KNOWN_CAPABILITIES
            );
            PROTOCOL_REJECTED.add(player);
            player.connection.disconnect(reason);
            return;
        }

        sessions.connectionNonce = nextToken();
        sessions.playerSessionToken = nextToken();
        sessions.capabilities = DesktopProtocol.sanitizeCapabilities(payload.capabilities());
        sessions.forcedMenuIds = validateForcedMenuIds(payload.forcedMenuIds());
        sessions.negotiated = true;
        sessions.uiEnabled = payload.uiEnabled();
        sessions.gameplayEnabled = payload.uiEnabled();
        sessions.lastModeSequence = -1L;
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
        player.connection.disconnect(Component.literal("Salt's Inventory Update 0.1.1 is incompatible. Update the mod on both client and server (protocol 2 required)."));
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
        session.menu.broadcastChanges();
        syncCarried(player, sessions);
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
        if (!mayMutateInventory(player) || !sessions.allowOperation(player, "quick-move", 1.0D)) {
            DesktopDebug.trace("server quick move dropped player={} sourceSession={} reason=player-state", player.getName().getString(), payload.sourceSessionId());
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
                clickMenu(0, player, sessions, source.menu, payload.sourceSlotIndex(), 0, ContainerInput.QUICK_MOVE, ItemStack.EMPTY);
            } finally {
                if (!carriedBeforeQuickMove.isEmpty()) {
                    setSharedCarried(player, sessions, carriedBeforeQuickMove);
                }
            }
            source.menu.broadcastChanges();
            player.inventoryMenu.broadcastChanges();
            sessions.broadcastAll(player);
            syncCarried(player, sessions);
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

        source.menu.broadcastChanges();
        player.inventoryMenu.broadcastChanges();
        sessions.broadcastAll(player);
        syncCarried(player, sessions);
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
                    authorization.menu().sendAllDataToRemote();
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

        targetSession.menu.broadcastChanges();
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
            session.menu.broadcastChanges();
            player.inventoryMenu.broadcastChanges();
            sessions.broadcastAll(player);
            syncCarried(player, sessions);
        } else {
            syncCarried(player, sessions);
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
        session.menu.broadcastChanges();
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
                DesktopDebug.warn("server JEI crafting transfer failed player={} targetSession={} recipe={} reason={}", player.getName().getString(), payload.targetSessionId(), payload.recipeId(), exception.toString());
                syncCarried(player, sessions);
                return;
            } finally {
                if (!committedCarried) {
                    player.inventoryMenu.setCarried(carriedBefore.copy());
                }
                clearDetachedCarried(sessions);
            }
            if (action == RecipeBookMenu.PostPlaceAction.PLACE_GHOST_RECIPE && !recipe.value().display().isEmpty()) {
                send(player, new DesktopGhostRecipePayload(payload.targetSessionId(), recipe.value().display().getFirst()));
            }
            target.menu().broadcastChanges();
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
            return;
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
        target.menu().broadcastChanges();
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
        sessions.beginHandlerCallback();
        try {
            handler.handle(new ServerPayloadContext(player, sessions, session, payload));
        } catch (RuntimeException exception) {
            DesktopDebug.warn(
                "server custom handler failed player={} session={} channel={} reason={}",
                player.getName().getString(),
                payload.sessionId(),
                payload.channel(),
                exception.toString()
            );
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

        if (slotIndex < 0 || slotIndex >= menu.slots.size()) {
            DesktopDebug.trace("server quick move dropped player={} session={} slot={} reason=out-of-range", player.getName().getString(), sessionId, slotIndex);
            return null;
        }

        return new SlotSource(sessionId, menu, menu.slots.get(slotIndex), session);
    }

    private static List<net.minecraft.world.inventory.Slot> quickMoveTargets(ServerPlayer player, PlayerSessions sessions, SlotSource source, DesktopQuickMovePayload payload) {
        if (payload.targetKind() == DesktopPackets.QUICK_TARGET_SESSION) {
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

    private static void clickMenu(int debugId, ServerPlayer player, PlayerSessions sessions, AbstractContainerMenu menu, int slotIndex, int button, ContainerInput input, ItemStack clientCarried) {
        if (!mayMutateInventory(player) || !menu.stillValid(player)) {
            DesktopDebug.trace("server click ignored id={} player={} menu={} reason=invalid-player-or-menu", debugId, player.getName().getString(), menu.containerId);
            return;
        }
        if (slotIndex != AbstractContainerMenu.SLOT_CLICKED_OUTSIDE
            && (slotIndex < 0 || slotIndex >= menu.slots.size())) {
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
                debugId,
                player.getName().getString(),
                menu.containerId,
                slotIndex,
                button,
                input,
                exception.toString()
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
    }

    private static ItemStack serverSlotStack(AbstractContainerMenu menu, int slotIndex) {
        if (slotIndex < 0 || slotIndex >= menu.slots.size()) {
            return ItemStack.EMPTY;
        }

        return menu.slots.get(slotIndex).getItem().copy();
    }

    private static void syncCarried(ServerPlayer player, PlayerSessions sessions) {
        ItemStack carried = player.inventoryMenu.getCarried().copy();
        DesktopDebug.trace("server sync carried player={} stack={}", player.getName().getString(), carried);
        send(player, new DesktopCarriedPayload(sessions.connectionNonce, sessions.playerSessionToken, player.inventoryMenu.getStateId(), carried));
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
            openMenuSession(player, provider, sourceKey, false, false, true);
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

    private static final class PlayerSessions {
        private final LinkedHashMap<Integer, Session> sessions = new LinkedHashMap<>();
        private final LinkedHashMap<String, DormantGhostSource> dormantGhostSources = new LinkedHashMap<>();
        private final LinkedHashMap<String, SourceIdentity> sourceIdentities = new LinkedHashMap<>();
        private final TokenBucket operationRateLimit = new TokenBucket(40.0D, 80.0D, System.nanoTime());
        private final TokenBucket controlRateLimit = new TokenBucket(10.0D, 20.0D, System.nanoTime());
        private final TokenBucket modeRateLimit = new TokenBucket(1.0D, 4.0D, System.nanoTime());
        private final TokenBucket resyncRateLimit = new TokenBucket(1.0D, 2.0D, System.nanoTime());
        private final BoundedLinkGraph<String> links = new BoundedLinkGraph<>();
        private boolean negotiated;
        private boolean uiEnabled;
        private boolean gameplayEnabled;
        private long capabilities;
        private long connectionNonce;
        private long playerSessionToken;
        private long lastModeSequence = -1L;
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

        private boolean hasCapability(long capability) {
            return (this.capabilities & capability) == capability;
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

            session.sourceIdentity = captureBlockSourceIdentity(player, session.sourceKey);
            if (session.sourceIdentity != null) {
                this.rememberSourceIdentity(session.sourceKey, session.sourceIdentity);
            }
            this.sessions.put(session.sessionId, session);
            session.initializeServerHandler(player, this);
            session.menu.setCarried(ItemStack.EMPTY);
            session.menu.setSynchronizer(new SessionSynchronizer(player, session));
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
                session.menu.sendAllDataToRemote();
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
            // The canonical cursor lives on the player menu. A copied cursor on a
            // desktop menu must never be settled again by removed().
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
                    session.menu.broadcastChanges();
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
                session.menu.broadcastChanges();
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
                OptionalInt opened = openMenuSession(player, provider, dormant.sourceKey(), false, true, false);
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
        private @Nullable SourceIdentity sourceIdentity;
        private boolean ghostPinned;
        private boolean visibleToClient = true;
        private @Nullable DesktopServerWindowHandler<AbstractContainerMenu, Object> serverHandler;
        private @Nullable Object serverState;

        private Session(int sessionId, AbstractContainerMenu menu, Component title, int specialKind, int entityId, int columns, int menuTypeId, String sourceKey) {
            this.sessionId = sessionId;
            this.sessionToken = nextToken();
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

    private record SlotSource(int sessionId, AbstractContainerMenu menu, net.minecraft.world.inventory.Slot slot, @Nullable Session session) {
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
                this.sessions.connectionNonce, this.session.sessionId, this.session.sessionToken,
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
                this.sessions.connectionNonce, this.session.sessionId, this.session.sessionToken,
                this.session.menu.getStateId(), channel, data
            ));
        }

        @Override
        public void broadcastChanges() {
            this.sessions.requestHandlerBroadcast(this.player);
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
            PlayerSessions sessions = PLAYERS.get(this.player);
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
