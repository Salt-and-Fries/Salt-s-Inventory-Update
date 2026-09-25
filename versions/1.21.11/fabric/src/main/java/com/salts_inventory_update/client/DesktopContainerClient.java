package com.salts_inventory_update.client;

import com.salts_inventory_update.platform.fabric.api.client.networking.v1.ClientPlayNetworking;
import java.security.SecureRandom;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.display.RecipeDisplayId;
import org.jspecify.annotations.Nullable;

import com.salts_inventory_update.SaltsInventoryUpdate;
import com.salts_inventory_update.SaltsInventoryRuntime;
import com.salts_inventory_update.debug.DesktopDebug;
import com.salts_inventory_update.inventory.InventoryExpansion;
import com.salts_inventory_update.network.DesktopPackets;
import com.salts_inventory_update.network.DesktopMenuOpenDataPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopButtonPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopBundleSelectPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopCarriedPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopClickPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopCloseSessionPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopCustomPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopDataPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopGhostRecipePayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopJeiTransferPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopMerchantOffersPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopOpenLinkedSourcesPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopOpenSessionPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopPlaceRecipePayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopPickupAllPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopQuickMoveAllPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopSortWindowsPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopQuickMovePayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopDragSlotsPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopHelloPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopReadyPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopHelloAckPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopModePayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopMutationAckPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopPlayerSessionPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopLinkPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopRenamePayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopSessionClosedPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopSessionPinPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopSessionVisibilityPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopSlotPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopSlotReference;
import com.salts_inventory_update.network.DesktopPackets.DesktopSessionReference;
import com.salts_inventory_update.network.DesktopPackets.InventoryExpansionSyncPayload;
import com.salts_inventory_update.network.DesktopPackets.InventorySlotPurchasePayload;
import com.salts_inventory_update.protocol.DesktopConnectionState;
import com.salts_inventory_update.protocol.DesktopProtocol;

public final class DesktopContainerClient {
    private static final int MODE_RESEND_INTERVAL_TICKS = 100;
    private static final int MAX_QUEUED_MUTATIONS = 256;
    private static final int MAX_ANVIL_NAME_LENGTH = 50;
    private static final long MUTATION_TIMEOUT_NANOS = 90_000_000_000L;
    private static final long SUPPORTED_CAPABILITIES = DesktopProtocol.KNOWN_CAPABILITIES
        | DesktopPackets.CAP_MULTI_MENU_GESTURES | DesktopPackets.CAP_SORT_WINDOWS;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final DesktopConnectionState CONNECTION = new DesktopConnectionState();
    private static final Map<Integer, Long> SESSION_TOKENS = new HashMap<>();
    private static final Map<Integer, MenuOpenData> PENDING_MENU_OPEN_DATA = new HashMap<>();
    private static final ArrayDeque<QueuedMutation> MUTATION_QUEUE = new ArrayDeque<>();
    private static @Nullable Object connectionIdentity;
    private static boolean helloSent;
    private static boolean incompatibilityShown;
    private static boolean requestedUiEnabled;
    private static long playerSessionToken;
    private static long modeSequence;
    private static int modeResendTicks;
    private static long nextMutationId = 1L;
    private static @Nullable InFlightMutation inFlightMutation;
    private static boolean mutationQueueBlocked;
    private static java.util.List<String> requestedForcedMenuIds = java.util.List.of();

    private DesktopContainerClient() {
    }

    public static void initializeNetworking() {
        ClientPlayNetworking.registerGlobalReceiver(DesktopHelloAckPayload.TYPE, (payload, context) -> {
            if (payload.playerSessionToken() == 0L
                || (payload.capabilities() & DesktopPackets.CAP_MULTI_MENU_GESTURES) == 0L
                || !CONNECTION.acknowledge(
                payload.protocolVersion(),
                payload.clientNonce(),
                payload.connectionNonce(),
                payload.capabilities(),
                payload.uiEnabled(),
                SUPPORTED_CAPABILITIES
            )) {
                rejectIncompatible(context.client(), "The server returned an invalid desktop protocol handshake.");
                return;
            }
            playerSessionToken = payload.playerSessionToken();
            requestedUiEnabled = payload.uiEnabled();
            modeSequence = -1L;
            modeResendTicks = 0;
            SaltsInventoryRuntime.setServerDesktopAvailable(true);
            if (context.client().player != null) {
                InventoryExpansion.appendMissingMenuSlots(context.client().player.inventoryMenu, context.client().player);
            }
            DesktopDebug.log("client desktop handshake accepted protocol={} capabilities={} ui={}", payload.protocolVersion(), payload.capabilities(), payload.uiEnabled());
        });
        ClientPlayNetworking.registerGlobalReceiver(DesktopPlayerSessionPayload.TYPE, (payload, context) -> {
            if (payload.playerSessionToken() != 0L
                && CONNECTION.authorizes(payload.connectionNonce(), DesktopProtocol.CAP_INVENTORY_TOPOLOGY, false)) {
                playerSessionToken = payload.playerSessionToken();
                SESSION_TOKENS.clear();
                PENDING_MENU_OPEN_DATA.clear();
                resetMutationQueue("player-session-rotated");
                boolean desktopVisible = context.client().screen instanceof InventoryDesktopScreen;
                InventoryDesktopScreen.reset(context.client());
                if (desktopVisible) {
                    context.client().setScreen(null);
                }
            }
        });
        ClientPlayNetworking.registerGlobalReceiver(DesktopOpenSessionPayload.TYPE, (payload, context) -> {
            if (!CONNECTION.authorizes(CONNECTION.connectionNonce(), DesktopProtocol.CAP_CUSTOM_WINDOWS, true)
                || payload.sessionToken() == 0L) {
                DesktopDebug.trace("client payload open ignored session={} reason=inactive-or-invalid", payload.sessionId());
                return;
            }
            Long previousToken = SESSION_TOKENS.get(payload.sessionId());
            if (previousToken != null && previousToken.longValue() != payload.sessionToken()) {
                rejectIncompatible(context.client(), "The server reused an active desktop session identifier.");
                return;
            }
            MenuOpenData menuOpenData = PENDING_MENU_OPEN_DATA.remove(payload.sessionId());
            if (menuOpenData != null
                && (menuOpenData.sessionToken != payload.sessionToken() || menuOpenData.menuTypeId != payload.menuTypeId())) {
                rejectIncompatible(context.client(), "The server sent mismatched desktop menu opening data.");
                return;
            }
            MenuType<?> incomingMenuType = payload.specialKind() == DesktopPackets.SPECIAL_GENERIC
                ? DesktopPackets.menuTypeById(payload.menuTypeId())
                : null;
            if (incomingMenuType != null && DesktopMenuFactories.hasFactory(incomingMenuType) && menuOpenData == null) {
                rejectIncompatible(context.client(), "The server omitted required desktop menu opening data.");
                return;
            }
            boolean replacesKnownSession = menuOpenData != null
                && menuOpenData.replacesSessionId > DesktopPackets.PLAYER_MENU_SESSION
                && SESSION_TOKENS.containsKey(menuOpenData.replacesSessionId);
            if (previousToken == null
                && !replacesKnownSession
                && SESSION_TOKENS.size() >= DesktopProtocol.MAX_DESKTOP_SESSIONS) {
                rejectIncompatible(context.client(), "The server exceeded the desktop session limit.");
                return;
            }
            SESSION_TOKENS.put(payload.sessionId(), payload.sessionToken());
            boolean diagnostic = isCamelOrLlamaSpecial(payload.specialKind());
            if (diagnostic) {
                mountDiag(
                    "client_payload_open_start session={} special={} entityId={} columns={} visible={} title={} source={} items={} data={} playerPresent={} levelPresent={}",
                    payload.sessionId(),
                    payload.specialKind(),
                    payload.entityId(),
                    payload.columns(),
                    payload.visible(),
                    payload.title().getString(),
                    payload.sourceKey(),
                    payload.items().size(),
                    payload.data().length,
                    context.client().player != null,
                    context.client().level != null
                );
            }
            DesktopDebug.log("client payload open session={} title={} type={} special={}", payload.sessionId(), payload.title().getString(), payload.menuTypeId(), payload.specialKind());
            DesktopContainerSession session = null;
            boolean sessionHandedOff = false;
            try {
                session = DesktopContainerSession.create(
                    context.client(),
                    payload,
                    menuOpenData == null ? new byte[0] : menuOpenData.data,
                    menuOpenData == null ? -1 : menuOpenData.replacesSessionId
                );
                if (diagnostic) {
                    mountDiag(
                        "client_payload_open_created session={} special={} menu={} slots={} containerSlots={}",
                        payload.sessionId(),
                        payload.specialKind(),
                        session.menu().getClass().getName(),
                        session.menu().slots.size(),
                        session.containerSlots().size()
                    );
                }
                InventoryDesktopScreen.openOrAddSession(context.client(), session, payload.visible());
                sessionHandedOff = true;
                if (diagnostic) {
                    mountDiag("client_payload_open_added session={} special={}", payload.sessionId(), payload.specialKind());
                }
            } catch (RuntimeException exception) {
                if (!sessionHandedOff && session != null) {
                    session.close();
                }
                SaltsInventoryUpdate.LOGGER.error(
                    "[desktop] Failed to reconstruct desktop session {} ({})",
                    payload.sessionId(),
                    payload.title().getString(),
                    exception
                );
                if (previousToken == null) {
                    SESSION_TOKENS.remove(payload.sessionId());
                }
                if (diagnostic) {
                    mountDiag("client_payload_open_failed session={} special={} reason={}", payload.sessionId(), payload.specialKind(), exception.toString());
                }
                rejectIncompatible(context.client(), "The server sent an invalid desktop session snapshot.");
            }
        });
        ClientPlayNetworking.registerGlobalReceiver(DesktopSlotPayload.TYPE, (payload, context) -> {
            InventoryDesktopScreen screen = InventoryDesktopScreen.current(context.client());
            if (screen != null) {
                screen.updateSessionSlot(payload.sessionId(), payload.slotIndex(), payload.stateId(), payload.stack());
            }
        });
        ClientPlayNetworking.registerGlobalReceiver(DesktopDataPayload.TYPE, (payload, context) -> {
            InventoryDesktopScreen screen = InventoryDesktopScreen.current(context.client());
            if (screen != null) {
                screen.updateSessionData(payload.sessionId(), payload.dataSlot(), payload.value());
            }
        });
        ClientPlayNetworking.registerGlobalReceiver(DesktopCarriedPayload.TYPE, (payload, context) -> {
            if (!CONNECTION.authorizes(payload.connectionNonce(), 0L, false)
                || payload.playerSessionToken() != playerSessionToken) {
                return;
            }
            DesktopDebug.trace("client payload carried stack={}", payload.carried());
            InventoryDesktopScreen screen = InventoryDesktopScreen.current(context.client());
            if (screen != null) {
                screen.setSharedCarried(payload.carried());
            } else if (context.client().player != null) {
                context.client().player.inventoryMenu.setCarried(payload.carried().copy());
            }
        });
        ClientPlayNetworking.registerGlobalReceiver(DesktopMutationAckPayload.TYPE, (payload, context) ->
            acceptMutationAck(payload)
        );
        ClientPlayNetworking.registerGlobalReceiver(DesktopSessionClosedPayload.TYPE, (payload, context) -> {
            SESSION_TOKENS.remove(payload.sessionId());
            PENDING_MENU_OPEN_DATA.remove(payload.sessionId());
            discardQueuedMutationsForSession(payload.sessionId(), "session-closed");
            InventoryDesktopScreen screen = InventoryDesktopScreen.current(context.client());
            if (screen != null) {
                screen.removeSession(payload.sessionId());
            }
        });
        ClientPlayNetworking.registerGlobalReceiver(DesktopSessionVisibilityPayload.TYPE, (payload, context) -> {
            if (!validInboundSession(payload.connectionNonce(), payload.sessionId(), payload.sessionToken())) {
                return;
            }
            InventoryDesktopScreen screen = InventoryDesktopScreen.current(context.client());
            if (screen != null) {
                screen.setSessionVisible(payload.sessionId(), payload.visible());
            }
        });
        ClientPlayNetworking.registerGlobalReceiver(DesktopMerchantOffersPayload.TYPE, (payload, context) -> {
            InventoryDesktopScreen screen = InventoryDesktopScreen.current(context.client());
            if (screen != null) {
                screen.applyMerchantOffers(payload);
            }
        });
        ClientPlayNetworking.registerGlobalReceiver(DesktopCustomPayload.TYPE, (payload, context) -> {
            if (payload.playerSessionToken() != playerSessionToken
                || !validInboundSession(payload.connectionNonce(), payload.sessionId(), payload.sessionToken())) {
                return;
            }
            InventoryDesktopScreen screen = InventoryDesktopScreen.current(context.client());
            if (screen != null) {
                screen.applyCustomPayload(payload);
            }
        });
        ClientPlayNetworking.registerGlobalReceiver(DesktopGhostRecipePayload.TYPE, (payload, context) -> {
            InventoryDesktopScreen screen = InventoryDesktopScreen.current(context.client());
            if (screen != null) {
                screen.applyGhostRecipe(payload);
            }
        });
        ClientPlayNetworking.registerGlobalReceiver(InventoryExpansionSyncPayload.TYPE, (payload, context) -> {
            if (!isTopologyNegotiated()) {
                DesktopDebug.trace("client payload inventory expansion ignored reason=topology-unnegotiated");
                return;
            }
            DesktopDebug.trace("client payload inventory expansion slots={} stacks={}", payload.slotCount(), payload.items().size());
            if (context.client().player != null) {
                int slotCount = InventoryExpansion.clampSlotCount(payload.slotCount());
                InventoryExpansion.access(context.client().player).salts_inventory_update$setExtraSlotCount(slotCount);
                InventoryExpansion.access(context.client().player).salts_inventory_update$getExtraInventory().loadSnapshot(slotCount, payload.items());
                InventoryExpansion.appendMissingMenuSlots(context.client().player.inventoryMenu, context.client().player);
                InventoryDesktopScreen screen = InventoryDesktopScreen.current(context.client());
                if (screen != null) {
                    screen.refreshInventoryWindowLayout();
                }
            }
        });
    }

    public static void tick(Minecraft minecraft) {
        Object currentConnection = minecraft.getConnection();
        if (minecraft.player == null || minecraft.level == null) {
            if (currentConnection == null) {
                resetConnection();
                connectionIdentity = null;
            }
            SaltsInventoryRuntime.setServerDesktopAvailable(true);
            return;
        }

        if (connectionIdentity != currentConnection) {
            resetConnection();
            connectionIdentity = currentConnection;
        }

        boolean remoteServer = minecraft.getCurrentServer() != null && minecraft.getSingleplayerServer() == null;
        boolean helloChannel = canSendType(DesktopHelloPayload.TYPE);
        boolean legacyReadyChannel = canSendType(DesktopReadyPayload.TYPE);
        boolean saltChannel = helloChannel || legacyReadyChannel || canSendType(DesktopClickPayload.TYPE);
        if (!helloChannel) {
            SaltsInventoryRuntime.setServerDesktopAvailable(!remoteServer);
            if (remoteServer && saltChannel) {
                rejectIncompatible(
                    minecraft,
                    legacyReadyChannel
                        ? "Salt's Inventory Update is incompatible. Update the mod on both client and server."
                        : "Salt's Inventory Update versions do not match. Update the mod on both client and server."
                );
            }
            return;
        }

        if (!helloSent) {
            long clientNonce = nextToken();
            CONNECTION.begin(clientNonce, minecraft.level.getGameTime());
            requestedUiEnabled = SaltsInventoryRuntime.isConfiguredEnabled();
            requestedForcedMenuIds = forcedMenuIds();
            helloSent = send(
                new DesktopHelloPayload(DesktopProtocol.VERSION, clientNonce, SUPPORTED_CAPABILITIES, requestedUiEnabled, requestedForcedMenuIds),
                "hello"
            );
            if (helloSent) {
                DesktopDebug.log("client desktop hello sent protocol={} capabilities={} ui={}", DesktopProtocol.VERSION, SUPPORTED_CAPABILITIES, requestedUiEnabled);
            }
        }

        if (CONNECTION.expireIfNecessary(minecraft.level.getGameTime(), true)) {
            rejectIncompatible(minecraft, "The server did not complete the Salt's Inventory Update handshake. Update the mod on both sides.");
        }

        boolean negotiated = CONNECTION.isNegotiated();
        SaltsInventoryRuntime.setServerDesktopAvailable(!remoteServer || negotiated);
        if (!negotiated) {
            return;
        }

        tickMutationQueue();

        boolean desiredUi = SaltsInventoryRuntime.isConfiguredEnabled();
        java.util.List<String> desiredForcedMenuIds = forcedMenuIds();
        boolean periodicResend = ++modeResendTicks >= MODE_RESEND_INTERVAL_TICKS;
        if (desiredUi != requestedUiEnabled || !desiredForcedMenuIds.equals(requestedForcedMenuIds) || periodicResend) {
            long nextSequence = ++modeSequence;
            long nonce = CONNECTION.connectionNonce();
            if (send(new DesktopModePayload(nonce, nextSequence, desiredUi, desiredForcedMenuIds), "mode")) {
                CONNECTION.updateMode(nonce, nextSequence, desiredUi);
                requestedUiEnabled = desiredUi;
                requestedForcedMenuIds = desiredForcedMenuIds;
                modeResendTicks = 0;
            }
        }
        if (!desiredUi && minecraft.screen instanceof InventoryDesktopScreen) {
            discardPendingMutations("desktop-disabled");
            minecraft.setScreen(null);
        }
    }

    /** Receiver entry used by the NeoForge-only Sophisticated packet registration. */
    public static void acceptMenuOpenData(DesktopMenuOpenDataPayload payload, Minecraft minecraft) {
        if (!CONNECTION.authorizes(payload.connectionNonce(), DesktopProtocol.CAP_CUSTOM_WINDOWS, true)
            || payload.sessionToken() == 0L) {
            DesktopDebug.trace("client opening data ignored session={} reason=inactive-or-invalid", payload.sessionId());
            return;
        }
        if (PENDING_MENU_OPEN_DATA.size() >= DesktopProtocol.MAX_DESKTOP_SESSIONS
            && !PENDING_MENU_OPEN_DATA.containsKey(payload.sessionId())) {
            rejectIncompatible(minecraft, "The server exceeded the pending desktop opening-data limit.");
            return;
        }
        PENDING_MENU_OPEN_DATA.put(
            payload.sessionId(),
            new MenuOpenData(payload.sessionToken(), payload.menuTypeId(), payload.replacesSessionId(), payload.data())
        );
        DesktopDebug.trace(
            "client opening data accepted session={} menuType={} replaces={} bytes={}",
            payload.sessionId(),
            payload.menuTypeId(),
            payload.replacesSessionId(),
            payload.data().length
        );
    }

    public static boolean canSendDesktopPackets() {
        return isGameplayActive() && canSendType(DesktopClickPayload.TYPE);
    }

    public static boolean canUseServerSessions() {
        return CONNECTION.authorizes(CONNECTION.connectionNonce(), DesktopProtocol.CAP_CUSTOM_WINDOWS, true)
            && canUseServerSessionsRaw();
    }

    private static boolean canUseServerSessionsRaw() {
        try {
            return ClientPlayNetworking.canSend(DesktopHelloPayload.TYPE)
                && ClientPlayNetworking.canSend(DesktopModePayload.TYPE)
                && ClientPlayNetworking.canSend(DesktopClickPayload.TYPE)
                && ClientPlayNetworking.canSend(DesktopDragSlotsPayload.TYPE)
                && ClientPlayNetworking.canSend(DesktopPickupAllPayload.TYPE)
                && ClientPlayNetworking.canSend(DesktopQuickMoveAllPayload.TYPE)
                && ClientPlayNetworking.canSend(DesktopBundleSelectPayload.TYPE)
                && ClientPlayNetworking.canSend(DesktopQuickMovePayload.TYPE)
                && ClientPlayNetworking.canSend(DesktopButtonPayload.TYPE)
                && ClientPlayNetworking.canSend(DesktopPlaceRecipePayload.TYPE)
                && ClientPlayNetworking.canSend(DesktopJeiTransferPayload.TYPE)
                && ClientPlayNetworking.canSend(DesktopRenamePayload.TYPE)
                && ClientPlayNetworking.canSend(DesktopCloseSessionPayload.TYPE)
                && ClientPlayNetworking.canSend(DesktopSessionPinPayload.TYPE)
                && ClientPlayNetworking.canSend(DesktopSessionVisibilityPayload.TYPE)
                && ClientPlayNetworking.canSend(DesktopOpenLinkedSourcesPayload.TYPE)
                && ClientPlayNetworking.canSend(DesktopLinkPayload.TYPE)
                && ClientPlayNetworking.canSend(DesktopCustomPayload.TYPE)
                && ClientPlayNetworking.canSend(DesktopCarriedPayload.TYPE)
                && ClientPlayNetworking.canSend(InventorySlotPurchasePayload.TYPE);
        } catch (IllegalStateException | IllegalArgumentException ignored) {
            return false;
        }
    }

    public static boolean isGameplayActive() {
        return SaltsInventoryRuntime.isConfiguredEnabled()
            && CONNECTION.authorizes(CONNECTION.connectionNonce(), DesktopPackets.CAP_MULTI_MENU_GESTURES, true);
    }

    public static boolean isTopologyNegotiated() {
        return CONNECTION.authorizes(
            CONNECTION.connectionNonce(),
            DesktopProtocol.CAP_INVENTORY_TOPOLOGY,
            false
        );
    }

    public static boolean clickSlot(int debugId, int sessionId, int slotIndex, int button, ClickType input, ItemStack clientCarried) {
        if (input == null || clientCarried == null || sessionAuth(sessionId) == null) {
            return false;
        }
        return queueMutation("click", Set.of(sessionId), mutationId -> {
            SessionAuth auth = sessionAuth(sessionId);
            ItemStack carried = currentClientCarried();
            if (auth == null || carried == null) {
                return null;
            }
            DesktopDebug.trace(
                "client dispatch mutation={} click id={} session={} slot={} button={} input={} clientCarried={}",
                mutationId, debugId, sessionId, slotIndex, button, input, carried
            );
            return new DesktopClickPayload(
                CONNECTION.connectionNonce(), mutationId, playerSessionToken, sessionId, auth.token(), auth.stateId(),
                debugId, slotIndex, button, input.name(), carried
            );
        });
    }

    public static boolean dragSlots(int quickCraftType, List<SlotTarget> slots) {
        List<SlotTarget> targets = validatedSlotTargets(slots, 1, DesktopPackets.MAX_GESTURE_SLOTS);
        if (targets == null || quickCraftType < 0 || quickCraftType > 2) {
            return false;
        }
        return queueMutation("drag-slots", sessionIds(targets), mutationId -> {
            List<DesktopSlotReference> references = authenticatedSlotReferences(targets, 1, DesktopPackets.MAX_GESTURE_SLOTS);
            if (references == null) {
                return null;
            }
            DesktopDebug.trace("client dispatch mutation={} drag type={} slots={}", mutationId, quickCraftType, references.size());
            return new DesktopDragSlotsPayload(
                CONNECTION.connectionNonce(), mutationId, playerSessionToken, quickCraftType, references
            );
        });
    }

    public static boolean pickupAll(int anchorSessionId, int anchorSlotIndex, int button, List<Integer> sourceSessions) {
        if (anchorSessionId < DesktopPackets.PLAYER_MENU_SESSION
            || anchorSlotIndex < 0
            || anchorSlotIndex >= DesktopProtocol.MAX_OPEN_SESSION_ITEMS
            || (button != 0 && button != 1)
            || sourceSessions == null
            || sourceSessions.isEmpty()
            || sourceSessions.size() > DesktopProtocol.MAX_DESKTOP_SESSIONS + 1) {
            return false;
        }
        LinkedHashSet<Integer> uniqueSessions = new LinkedHashSet<>();
        for (Integer sessionId : sourceSessions) {
            if (sessionId == null || sessionId < DesktopPackets.PLAYER_MENU_SESSION || !uniqueSessions.add(sessionId)) {
                return false;
            }
            if (sessionAuth(sessionId) == null) {
                return false;
            }
        }
        if (!uniqueSessions.contains(anchorSessionId)) {
            return false;
        }
        List<Integer> logicalSources = List.copyOf(uniqueSessions);
        return queueMutation("pickup-all", Set.copyOf(uniqueSessions), mutationId -> {
            List<DesktopSessionReference> sources = authenticatedSessionReferences(logicalSources);
            if (sources == null) {
                return null;
            }
            DesktopDebug.trace(
                "client dispatch mutation={} pickup all anchorSession={} anchorSlot={} button={} sessions={}",
                mutationId, anchorSessionId, anchorSlotIndex, button, sources.size()
            );
            return new DesktopPickupAllPayload(
                CONNECTION.connectionNonce(), mutationId, playerSessionToken,
                anchorSessionId, anchorSlotIndex, button, sources
            );
        });
    }

    public static boolean canSortWindows() {
        return CONNECTION.authorizes(CONNECTION.connectionNonce(), DesktopPackets.CAP_SORT_WINDOWS, true);
    }

    public static boolean sortWindows(int sourceId, List<Integer> destinations, int focusedId, boolean shift) {
        if (!canSortWindows() || destinations.isEmpty()) return false;
        List<Integer> ids = List.copyOf(new LinkedHashSet<>(destinations));
        LinkedHashSet<Integer> involved = new LinkedHashSet<>(ids);
        involved.add(sourceId);
        return queueMutation("sort-windows", Set.copyOf(involved), mutationId -> {
            List<DesktopSessionReference> targets = authenticatedSessionReferences(ids);
            List<DesktopSessionReference> source = authenticatedSessionReferences(List.of(sourceId));
            if (targets == null || source == null) return null;
            return new DesktopSortWindowsPayload(CONNECTION.connectionNonce(), mutationId,
                playerSessionToken, source.get(0), targets, focusedId, shift);
        });
    }

    public static boolean quickMoveSlots(List<SlotTarget> sources, int targetKind, int targetSessionId) {
        if (targetKind < DesktopPackets.QUICK_TARGET_DEFAULT || targetKind > DesktopPackets.QUICK_TARGET_HOTBAR) {
            return false;
        }
        List<SlotTarget> logicalSources = validatedSlotTargets(sources, 1, DesktopPackets.MAX_GESTURE_SLOTS);
        int resolvedTargetSessionId = targetKind == DesktopPackets.QUICK_TARGET_SESSION
            ? targetSessionId
            : DesktopPackets.PLAYER_MENU_SESSION;
        if (logicalSources == null || sessionAuth(resolvedTargetSessionId) == null) {
            return false;
        }
        LinkedHashSet<Integer> involvedSessions = new LinkedHashSet<>(sessionIds(logicalSources));
        involvedSessions.add(resolvedTargetSessionId);
        return queueMutation("quick-move-all", Set.copyOf(involvedSessions), mutationId -> {
            List<DesktopSlotReference> references = authenticatedSlotReferences(
                logicalSources, 1, DesktopPackets.MAX_GESTURE_SLOTS
            );
            SessionAuth target = sessionAuth(resolvedTargetSessionId);
            if (references == null || target == null) {
                return null;
            }
            DesktopDebug.trace(
                "client dispatch mutation={} quick move all sources={} targetKind={} targetSession={}",
                mutationId, references.size(), targetKind, resolvedTargetSessionId
            );
            return new DesktopQuickMoveAllPayload(
                CONNECTION.connectionNonce(), mutationId, playerSessionToken, references, targetKind,
                new DesktopSessionReference(resolvedTargetSessionId, target.token(), target.stateId())
            );
        });
    }

    public static boolean selectBundleItem(int sessionId, int slotIndex, int selectedItemIndex) {
        if (slotIndex < 0
            || slotIndex >= DesktopProtocol.MAX_OPEN_SESSION_ITEMS
            || selectedItemIndex < -1
            || selectedItemIndex > DesktopPackets.MAX_BUNDLE_SELECTION_INDEX) {
            return false;
        }
        if (sessionAuth(sessionId) == null || !canDispatchOptimisticMutationNow()) {
            return false;
        }
        boolean queued = queueMutation("bundle-select", Set.of(sessionId), mutationId -> {
            SessionAuth auth = sessionAuth(sessionId);
            if (auth == null) {
                return null;
            }
            DesktopDebug.trace(
                "client dispatch mutation={} bundle select session={} slot={} selected={}",
                mutationId, sessionId, slotIndex, selectedItemIndex
            );
            return new DesktopBundleSelectPayload(
                CONNECTION.connectionNonce(), mutationId, playerSessionToken,
                new DesktopSlotReference(sessionId, auth.token(), auth.stateId(), slotIndex),
                selectedItemIndex
            );
        });
        return immediateMutationDispatched("bundle-select", queued);
    }

    public static boolean quickMoveSlot(int sourceSessionId, int sourceSlotIndex, int targetKind, int targetSessionId) {
        if (targetKind < DesktopPackets.QUICK_TARGET_DEFAULT || targetKind > DesktopPackets.QUICK_TARGET_HOTBAR) {
            return false;
        }
        int resolvedTargetSessionId = targetKind == DesktopPackets.QUICK_TARGET_SESSION
            ? targetSessionId
            : DesktopPackets.PLAYER_MENU_SESSION;
        if (sessionAuth(sourceSessionId) == null || sessionAuth(resolvedTargetSessionId) == null) {
            return false;
        }
        LinkedHashSet<Integer> involvedSessions = new LinkedHashSet<>();
        involvedSessions.add(sourceSessionId);
        involvedSessions.add(resolvedTargetSessionId);
        return queueMutation("quick-move", Set.copyOf(involvedSessions), mutationId -> {
            SessionAuth source = sessionAuth(sourceSessionId);
            SessionAuth target = sessionAuth(resolvedTargetSessionId);
            if (source == null || target == null) {
                return null;
            }
            DesktopDebug.trace(
                "client dispatch mutation={} quick move sourceSession={} sourceSlot={} targetKind={} targetSession={}",
                mutationId, sourceSessionId, sourceSlotIndex, targetKind, targetSessionId
            );
            return new DesktopQuickMovePayload(
                CONNECTION.connectionNonce(), mutationId, playerSessionToken,
                sourceSessionId, source.token(), source.stateId(), sourceSlotIndex,
                targetKind, targetSessionId, target.token(), target.stateId()
            );
        });
    }

    public static boolean clickButton(int sessionId, int buttonId) {
        if (sessionAuth(sessionId) == null || !canDispatchOptimisticMutationNow()) {
            return false;
        }
        boolean queued = queueMutation("button", Set.of(sessionId), mutationId -> {
            SessionAuth auth = sessionAuth(sessionId);
            if (auth == null) {
                return null;
            }
            DesktopDebug.trace("client dispatch mutation={} button session={} button={}", mutationId, sessionId, buttonId);
            return new DesktopButtonPayload(
                CONNECTION.connectionNonce(), mutationId, playerSessionToken,
                sessionId, auth.token(), auth.stateId(), buttonId
            );
        });
        return immediateMutationDispatched("button", queued);
    }

    public static boolean placeRecipe(int sessionId, RecipeDisplayId recipeId, boolean useMaxItems) {
        if (recipeId == null || sessionAuth(sessionId) == null) {
            return false;
        }
        return queueMutation("recipe-place", Set.of(sessionId), mutationId -> {
            SessionAuth auth = sessionAuth(sessionId);
            if (auth == null
                || !CONNECTION.authorizes(CONNECTION.connectionNonce(), DesktopProtocol.CAP_RECIPE_TRANSFER, true)) {
                return null;
            }
            DesktopDebug.trace(
                "client dispatch mutation={} recipe place session={} recipe={} useMax={}",
                mutationId, sessionId, recipeId, useMaxItems
            );
            return new DesktopPlaceRecipePayload(
                CONNECTION.connectionNonce(), mutationId, playerSessionToken,
                sessionId, auth.token(), auth.stateId(), recipeId, useMaxItems
            );
        });
    }

    public static boolean transferJeiRecipe(int targetSessionId, Identifier recipeId, boolean maxTransfer) {
        if (recipeId == null
            || sessionAuth(targetSessionId) == null
            || !CONNECTION.authorizes(CONNECTION.connectionNonce(), DesktopProtocol.CAP_RECIPE_TRANSFER, true)) {
            return false;
        }
        return queueMutation("jei-transfer", Set.of(targetSessionId), mutationId -> {
            SessionAuth auth = sessionAuth(targetSessionId);
            if (auth == null
                || !CONNECTION.authorizes(CONNECTION.connectionNonce(), DesktopProtocol.CAP_RECIPE_TRANSFER, true)) {
                return null;
            }
            DesktopDebug.trace(
                "client dispatch mutation={} JEI transfer targetSession={} recipe={} max={}",
                mutationId, targetSessionId, recipeId, maxTransfer
            );
            return new DesktopJeiTransferPayload(
                CONNECTION.connectionNonce(), mutationId, playerSessionToken,
                targetSessionId, auth.token(), auth.stateId(), recipeId, maxTransfer
            );
        });
    }

    public static boolean purchaseInventorySlot() {
        if (sessionAuth(DesktopPackets.PLAYER_MENU_SESSION) == null) {
            return false;
        }
        return queueMutation("inventory-slot-purchase", Set.of(DesktopPackets.PLAYER_MENU_SESSION), mutationId -> {
            SessionAuth auth = sessionAuth(DesktopPackets.PLAYER_MENU_SESSION);
            if (auth == null
                || !CONNECTION.authorizes(CONNECTION.connectionNonce(), DesktopProtocol.CAP_INVENTORY_TOPOLOGY, true)) {
                return null;
            }
            DesktopDebug.trace("client dispatch mutation={} inventory slot purchase", mutationId);
            return new InventorySlotPurchasePayload(
                CONNECTION.connectionNonce(), mutationId, auth.token(), auth.stateId()
            );
        });
    }

    public static boolean renameAnvil(int sessionId, String name) {
        if (name == null
            || name.length() > MAX_ANVIL_NAME_LENGTH
            || sessionAuth(sessionId) == null
            || !canDispatchOptimisticMutationNow()) {
            return false;
        }
        boolean queued = queueMutation("rename", Set.of(sessionId), mutationId -> {
            SessionAuth auth = sessionAuth(sessionId);
            if (auth == null) {
                return null;
            }
            DesktopDebug.trace("client dispatch mutation={} rename session={} name={}", mutationId, sessionId, name);
            return new DesktopRenamePayload(
                CONNECTION.connectionNonce(), mutationId, playerSessionToken,
                sessionId, auth.token(), auth.stateId(), name
            );
        });
        return immediateMutationDispatched("rename", queued);
    }

    public static boolean sendCustomPayload(int sessionId, Identifier channel, byte[] data) {
        if (channel == null
            || channel.toString().length() > DesktopProtocol.MAX_IDENTIFIER_LENGTH
            || data == null
            || data.length > DesktopProtocol.MAX_CUSTOM_DATA_BYTES
            || sessionAuth(sessionId) == null
            || !canDispatchOptimisticMutationNow()) {
            return false;
        }
        byte[] intentData = data.clone();
        boolean queued = queueMutation("custom", Set.of(sessionId), mutationId -> {
            SessionAuth auth = sessionAuth(sessionId);
            if (auth == null) {
                return null;
            }
            DesktopDebug.trace(
                "client dispatch mutation={} custom session={} channel={} bytes={}",
                mutationId, sessionId, channel, intentData.length
            );
            return new DesktopCustomPayload(
                CONNECTION.connectionNonce(), mutationId, playerSessionToken,
                sessionId, auth.token(), auth.stateId(), channel, intentData
            );
        });
        return immediateMutationDispatched("custom", queued);
    }

    public static boolean syncCarried(ItemStack carried) {
        if (carried == null
            || sessionAuth(DesktopPackets.PLAYER_MENU_SESSION) == null
            || !canDispatchOptimisticMutationNow()) {
            return false;
        }
        ItemStack intentCarried = carried.copy();
        boolean queued = queueMutation("carried", Set.of(DesktopPackets.PLAYER_MENU_SESSION), mutationId -> {
            SessionAuth auth = sessionAuth(DesktopPackets.PLAYER_MENU_SESSION);
            if (auth == null) {
                return null;
            }
            DesktopDebug.trace("client dispatch mutation={} carried stack={}", mutationId, intentCarried);
            return new DesktopCarriedPayload(
                CONNECTION.connectionNonce(), mutationId, auth.token(), auth.stateId(), intentCarried
            );
        });
        return immediateMutationDispatched("carried", queued);
    }

    /** Whether a mutation intent can enter the authenticated mutation lane. */
    public static boolean canAcceptMutationIntent() {
        return isGameplayActive()
            && !mutationQueueBlocked
            && MUTATION_QUEUE.size() < MAX_QUEUED_MUTATIONS;
    }

    /**
     * Native/optimistic controls must not change opaque local state while another mutation owns the
     * lane. Unlike slot gestures, that state cannot be reconstructed safely if a queued intent is
     * later discarded due to a timeout, closed session, or rejected sequence.
     */
    public static boolean canDispatchOptimisticMutationNow() {
        return canAcceptMutationIntent()
            && inFlightMutation == null
            && MUTATION_QUEUE.isEmpty();
    }

    private static boolean immediateMutationDispatched(String label, boolean queued) {
        InFlightMutation inFlight = inFlightMutation;
        return queued
            && !mutationQueueBlocked
            && inFlight != null
            && label.equals(inFlight.label());
    }

    public static void closeSession(int sessionId) {
        SessionAuth auth = sessionAuth(sessionId);
        if (auth == null) {
            return;
        }
        DesktopDebug.log("client send close session={}", sessionId);
        send(new DesktopCloseSessionPayload(CONNECTION.connectionNonce(), sessionId, auth.token()), "close");
    }

    public static void setSessionPinMode(int sessionId, PinMode pinMode) {
        SessionAuth auth = sessionAuth(sessionId);
        if (auth == null) {
            return;
        }
        DesktopDebug.trace("client send pin session={} pin={}", sessionId, pinMode);
        send(new DesktopSessionPinPayload(CONNECTION.connectionNonce(), sessionId, auth.token(), pinModeToPacket(pinMode)), "pin");
    }

    public static void setSessionVisible(int sessionId, boolean visible) {
        SessionAuth auth = sessionAuth(sessionId);
        if (auth == null) {
            return;
        }
        DesktopDebug.trace("client send visibility session={} visible={}", sessionId, visible);
        send(new DesktopSessionVisibilityPayload(CONNECTION.connectionNonce(), sessionId, auth.token(), visible), "visibility");
    }

    public static void openLinkedSources(int originSessionId) {
        SessionAuth auth = sessionAuth(originSessionId);
        if (auth == null) {
            return;
        }
        DesktopDebug.trace("client send linked sources origin={}", originSessionId);
        send(new DesktopOpenLinkedSourcesPayload(CONNECTION.connectionNonce(), originSessionId, auth.token()), "open-linked-sources");
    }

    public static void linkSessions(int firstSessionId, int secondSessionId) {
        SessionAuth first = sessionAuth(firstSessionId);
        SessionAuth second = sessionAuth(secondSessionId);
        if (first == null || second == null) {
            return;
        }
        send(new DesktopLinkPayload(CONNECTION.connectionNonce(), firstSessionId, first.token(), secondSessionId, second.token(), DesktopLinkPayload.ACTION_LINK), "link");
    }

    public static void detachSession(int sessionId) {
        SessionAuth auth = sessionAuth(sessionId);
        if (auth == null) {
            return;
        }
        send(new DesktopLinkPayload(CONNECTION.connectionNonce(), sessionId, auth.token(), sessionId, auth.token(), DesktopLinkPayload.ACTION_DETACH), "detach");
    }

    public static void unlinkSessions(int firstSessionId, int secondSessionId) {
        SessionAuth first = sessionAuth(firstSessionId);
        SessionAuth second = sessionAuth(secondSessionId);
        if (first == null || second == null) {
            return;
        }
        send(new DesktopLinkPayload(CONNECTION.connectionNonce(), firstSessionId, first.token(), secondSessionId, second.token(), DesktopLinkPayload.ACTION_DETACH), "unlink");
    }

    private static int pinModeToPacket(PinMode pinMode) {
        return switch (pinMode) {
            case UNPINNED -> DesktopPackets.PIN_MODE_UNPINNED;
            case PINNED -> DesktopPackets.PIN_MODE_PINNED;
            case GHOST_PINNED -> DesktopPackets.PIN_MODE_GHOST_PINNED;
        };
    }

    private static boolean queueMutation(String label, Set<Integer> sessionIds, MutationPacketFactory factory) {
        if (!isGameplayActive()) {
            return false;
        }
        if (mutationQueueBlocked) {
            DesktopDebug.warn("client mutation safely dropped label={} reason=queue-blocked", label);
            return true;
        }
        if (MUTATION_QUEUE.size() >= MAX_QUEUED_MUTATIONS) {
            DesktopDebug.warn(
                "client mutation safely dropped label={} queued={} reason=queue-capacity",
                label,
                MUTATION_QUEUE.size()
            );
            return true;
        }
        MUTATION_QUEUE.addLast(new QueuedMutation(label, Set.copyOf(sessionIds), factory));
        dispatchNextMutation();
        return true;
    }

    private static void dispatchNextMutation() {
        if (inFlightMutation != null || mutationQueueBlocked || !isGameplayActive()) {
            return;
        }
        while (!MUTATION_QUEUE.isEmpty()) {
            QueuedMutation queued = MUTATION_QUEUE.removeFirst();
            long mutationId = nextMutationId;
            if (mutationId <= 0L) {
                blockMutationQueue("mutation-id-exhausted", false);
                return;
            }

            CustomPacketPayload payload;
            try {
                payload = queued.factory().create(mutationId);
            } catch (RuntimeException exception) {
                DesktopDebug.warn(
                    "client mutation intent dropped label={} reason={}",
                    queued.label(),
                    exception.toString()
                );
                continue;
            }
            if (payload == null) {
                DesktopDebug.trace("client mutation intent dropped label={} reason=session-invalidated", queued.label());
                continue;
            }

            InFlightMutation inFlight = new InFlightMutation(mutationId, queued.label(), System.nanoTime());
            inFlightMutation = inFlight;
            if (!send(payload, queued.label())) {
                inFlightMutation = null;
                blockMutationQueue("packet-send-failed", false);
                return;
            }
            nextMutationId = mutationId == Long.MAX_VALUE ? Long.MIN_VALUE : mutationId + 1L;
            DesktopDebug.trace(
                "client mutation sent id={} label={} remaining={}",
                mutationId,
                queued.label(),
                MUTATION_QUEUE.size()
            );
            return;
        }
    }

    private static void acceptMutationAck(DesktopMutationAckPayload payload) {
        if (!CONNECTION.authorizes(payload.connectionNonce(), DesktopPackets.CAP_MULTI_MENU_GESTURES, false)
            || payload.playerSessionToken() != playerSessionToken) {
            DesktopDebug.trace("client mutation ack ignored id={} reason=connection-auth", payload.mutationId());
            return;
        }
        InFlightMutation inFlight = inFlightMutation;
        if (inFlight == null) {
            DesktopDebug.trace("client mutation ack ignored id={} reason=no-in-flight", payload.mutationId());
            return;
        }
        if (payload.mutationId() < inFlight.mutationId()) {
            DesktopDebug.trace(
                "client mutation ack ignored id={} expected={} reason=stale",
                payload.mutationId(),
                inFlight.mutationId()
            );
            return;
        }
        if (payload.mutationId() != inFlight.mutationId() || !payload.sequenceAccepted()) {
            DesktopDebug.warn(
                "client mutation queue blocked ack={} expected={} accepted={} reason=sequence",
                payload.mutationId(),
                inFlight.mutationId(),
                payload.sequenceAccepted()
            );
            inFlightMutation = null;
            blockMutationQueue("server-sequence-rejected", false);
            return;
        }

        DesktopDebug.trace(
            "client mutation acknowledged id={} label={} queued={} timedOut={}",
            payload.mutationId(),
            inFlight.label(),
            MUTATION_QUEUE.size(),
            inFlight.timedOut()
        );
        inFlightMutation = null;
        mutationQueueBlocked = false;
        dispatchNextMutation();
    }

    private static void tickMutationQueue() {
        InFlightMutation inFlight = inFlightMutation;
        if (inFlight == null || inFlight.timedOut()
            || System.nanoTime() - inFlight.sentAtNanos() < MUTATION_TIMEOUT_NANOS) {
            return;
        }
        inFlight.markTimedOut();
        discardPendingMutations("ack-timeout");
        mutationQueueBlocked = true;
        DesktopDebug.warn(
            "client mutation queue paused id={} label={} reason=ack-timeout",
            inFlight.mutationId(),
            inFlight.label()
        );
    }

    private static void discardQueuedMutationsForSession(int sessionId, String reason) {
        int before = MUTATION_QUEUE.size();
        MUTATION_QUEUE.removeIf(queued -> queued.sessionIds().contains(sessionId));
        int removed = before - MUTATION_QUEUE.size();
        if (removed > 0) {
            DesktopDebug.trace(
                "client mutation intents discarded session={} count={} reason={}",
                sessionId,
                removed,
                reason
            );
        }
    }

    private static void discardPendingMutations(String reason) {
        if (!MUTATION_QUEUE.isEmpty()) {
            DesktopDebug.trace("client mutation intents discarded count={} reason={}", MUTATION_QUEUE.size(), reason);
            MUTATION_QUEUE.clear();
        }
    }

    private static void blockMutationQueue(String reason, boolean retainInFlight) {
        discardPendingMutations(reason);
        if (!retainInFlight) {
            inFlightMutation = null;
        }
        mutationQueueBlocked = true;
        DesktopDebug.warn("client mutation queue blocked reason={}", reason);
    }

    private static void resetMutationQueue(String reason) {
        boolean hadState = inFlightMutation != null
            || !MUTATION_QUEUE.isEmpty()
            || mutationQueueBlocked
            || nextMutationId != 1L;
        discardPendingMutations(reason);
        inFlightMutation = null;
        mutationQueueBlocked = false;
        nextMutationId = 1L;
        if (hadState) {
            DesktopDebug.trace("client mutation queue reset reason={}", reason);
        }
    }

    private static boolean send(net.minecraft.network.protocol.common.custom.CustomPacketPayload payload, String label) {
        try {
            ClientPlayNetworking.send(payload);
            return true;
        } catch (IllegalStateException | IllegalArgumentException exception) {
            DesktopDebug.warn("client desktop packet failed label={} type={} reason={}", label, payload.type().id(), exception.toString());
            return false;
        }
    }

    private static @Nullable SessionAuth sessionAuth(int sessionId) {
        if (!isGameplayActive()) {
            return null;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return null;
        }
        if (sessionId == DesktopPackets.PLAYER_MENU_SESSION) {
            return playerSessionToken == 0L
                ? null
                : new SessionAuth(playerSessionToken, minecraft.player.inventoryMenu.getStateId());
        }
        DesktopContainerSession session = InventoryDesktopScreen.sessionForNetworking(minecraft, sessionId);
        Long token = SESSION_TOKENS.get(sessionId);
        if (session == null || token == null || token == 0L || token != session.sessionToken()) {
            return null;
        }
        return new SessionAuth(token, session.menu().getStateId());
    }

    private static @Nullable List<DesktopSlotReference> authenticatedSlotReferences(List<SlotTarget> targets, int minimum, int maximum) {
        if (targets == null || targets.size() < minimum || targets.size() > maximum) {
            return null;
        }
        LinkedHashSet<SlotTarget> uniqueTargets = new LinkedHashSet<>();
        List<DesktopSlotReference> references = new ArrayList<>(targets.size());
        for (SlotTarget target : targets) {
            if (target == null || !uniqueTargets.add(target)) {
                return null;
            }
            SessionAuth auth = sessionAuth(target.sessionId());
            if (auth == null) {
                return null;
            }
            references.add(new DesktopSlotReference(target.sessionId(), auth.token(), auth.stateId(), target.slotIndex()));
        }
        return List.copyOf(references);
    }

    private static @Nullable List<SlotTarget> validatedSlotTargets(List<SlotTarget> targets, int minimum, int maximum) {
        if (targets == null || targets.size() < minimum || targets.size() > maximum) {
            return null;
        }
        LinkedHashSet<SlotTarget> uniqueTargets = new LinkedHashSet<>();
        for (SlotTarget target : targets) {
            if (target == null || !uniqueTargets.add(target) || sessionAuth(target.sessionId()) == null) {
                return null;
            }
        }
        return List.copyOf(uniqueTargets);
    }

    private static @Nullable List<DesktopSessionReference> authenticatedSessionReferences(List<Integer> sessionIds) {
        List<DesktopSessionReference> references = new ArrayList<>(sessionIds.size());
        for (Integer sessionId : sessionIds) {
            SessionAuth auth = sessionId == null ? null : sessionAuth(sessionId);
            if (auth == null) {
                return null;
            }
            references.add(new DesktopSessionReference(sessionId, auth.token(), auth.stateId()));
        }
        return List.copyOf(references);
    }

    private static Set<Integer> sessionIds(List<SlotTarget> targets) {
        LinkedHashSet<Integer> sessionIds = new LinkedHashSet<>();
        for (SlotTarget target : targets) {
            sessionIds.add(target.sessionId());
        }
        return Set.copyOf(sessionIds);
    }

    private static @Nullable ItemStack currentClientCarried() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.player == null ? null : minecraft.player.inventoryMenu.getCarried().copy();
    }

    private static boolean validInboundSession(long connectionNonce, int sessionId, long token) {
        if (!CONNECTION.authorizes(connectionNonce, 0L, false)) {
            return false;
        }
        if (sessionId == DesktopPackets.PLAYER_MENU_SESSION) {
            return token != 0L && token == playerSessionToken;
        }
        return token != 0L && token == SESSION_TOKENS.getOrDefault(sessionId, 0L);
    }

    private static boolean canSendType(CustomPacketPayload.Type<?> type) {
        try {
            return ClientPlayNetworking.canSend(type);
        } catch (IllegalStateException | IllegalArgumentException ignored) {
            return false;
        }
    }

    private static long nextToken() {
        long token;
        do {
            token = SECURE_RANDOM.nextLong();
        } while (token == 0L);
        return token;
    }

    private static void resetConnection() {
        resetMutationQueue("connection-reset");
        CONNECTION.reset();
        SESSION_TOKENS.clear();
        PENDING_MENU_OPEN_DATA.clear();
        helloSent = false;
        incompatibilityShown = false;
        requestedUiEnabled = false;
        requestedForcedMenuIds = java.util.List.of();
        playerSessionToken = 0L;
        modeSequence = -1L;
        modeResendTicks = 0;
    }

    private static void rejectIncompatible(Minecraft minecraft, String reason) {
        if (incompatibilityShown) {
            return;
        }
        incompatibilityShown = true;
        blockMutationQueue("protocol-incompatible", false);
        CONNECTION.markIncompatible();
        SaltsInventoryRuntime.setServerDesktopAvailable(false);
        Component message = Component.literal(reason);
        if (minecraft.player != null) {
            minecraft.player.connection.getConnection().disconnect(message);
        }
        DesktopDebug.warn("client desktop protocol rejected reason={}", reason);
    }

    private static java.util.List<String> forcedMenuIds() {
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String raw : SaltsInventoryConfig.get().forcedContainerWindows) {
            if (raw == null || raw.isBlank() || normalized.size() >= DesktopProtocol.MAX_FORCED_MENU_IDS) {
                continue;
            }
            String value = raw.trim();
            if (value.length() > DesktopProtocol.MAX_IDENTIFIER_LENGTH) {
                continue;
            }
            try {
                Identifier.parse(value);
                normalized.add(value);
            } catch (RuntimeException ignored) {
                // Invalid local entries remain local configuration errors and are not sent.
            }
        }
        ArrayList<String> result = new ArrayList<>(normalized);
        result.sort(String::compareTo);
        return java.util.List.copyOf(result);
    }

    private static boolean isCamelOrLlamaSpecial(int specialKind) {
        return specialKind == DesktopPackets.SPECIAL_CAMEL || specialKind == DesktopPackets.SPECIAL_LLAMA;
    }

    private static void mountDiag(String message, Object... args) {
        DesktopDebug.detail("SIU_MOUNT_DIAG " + message, args);
    }

    public record SlotTarget(int sessionId, int slotIndex) {
        public SlotTarget {
            if (sessionId < DesktopPackets.PLAYER_MENU_SESSION
                || slotIndex < 0
                || slotIndex >= DesktopProtocol.MAX_OPEN_SESSION_ITEMS) {
                throw new IllegalArgumentException("Invalid desktop slot target");
            }
        }
    }

    private record SessionAuth(long token, int stateId) {
    }

    @FunctionalInterface
    private interface MutationPacketFactory {
        @Nullable CustomPacketPayload create(long mutationId);
    }

    private record QueuedMutation(String label, Set<Integer> sessionIds, MutationPacketFactory factory) {
    }

    private static final class InFlightMutation {
        private final long mutationId;
        private final String label;
        private final long sentAtNanos;
        private boolean timedOut;

        private InFlightMutation(long mutationId, String label, long sentAtNanos) {
            this.mutationId = mutationId;
            this.label = label;
            this.sentAtNanos = sentAtNanos;
        }

        private long mutationId() {
            return this.mutationId;
        }

        private String label() {
            return this.label;
        }

        private long sentAtNanos() {
            return this.sentAtNanos;
        }

        private boolean timedOut() {
            return this.timedOut;
        }

        private void markTimedOut() {
            this.timedOut = true;
        }
    }

    private record MenuOpenData(long sessionToken, int menuTypeId, int replacesSessionId, byte[] data) {
        private MenuOpenData {
            data = data.clone();
        }
    }
}
