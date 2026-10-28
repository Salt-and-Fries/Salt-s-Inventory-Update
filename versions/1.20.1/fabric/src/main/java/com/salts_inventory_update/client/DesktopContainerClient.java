package com.salts_inventory_update.client;

import com.salts_inventory_update.api.client.desktop.DesktopMenuFactories;
import com.salts_inventory_update.api.desktop.DesktopMenuBindings;

import com.salts_inventory_update.platform.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import java.security.SecureRandom;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import org.jetbrains.annotations.Nullable;

import com.salts_inventory_update.SaltsInventoryRuntime;
import com.salts_inventory_update.debug.DesktopDebug;
import com.salts_inventory_update.inventory.InventoryExpansion;
import com.salts_inventory_update.network.DesktopPackets;
import com.salts_inventory_update.network.DesktopMenuOpenDataPayload;
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
import com.salts_inventory_update.network.DesktopPackets.DesktopFinishWorkspacePayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopCustomPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopDataPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopGhostRecipePayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopJeiTransferPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopMerchantOffersPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopOpenLinkedSourcesPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopOpenSessionPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopPlaceRecipePayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopQuickMovePayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopHelloAckPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopHelloPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopModePayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopSessionAuthorizationPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopRenamePayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopSessionClosedPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopSessionVisibilityPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopSourceLinkPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopSlotPayload;
import com.salts_inventory_update.network.DesktopPackets.InventoryExpansionSyncPayload;
import com.salts_inventory_update.network.DesktopPackets.InventorySlotPurchasePayload;
import com.salts_inventory_update.protocol.DesktopConnectionState;
import com.salts_inventory_update.protocol.DesktopProtocol;

public final class DesktopContainerClient {
    private static boolean automaticNativeReady;
    private static final Map<ResourceLocation, ClientPlayNetworking.PlayChannelHandler> AUTOMATIC_RECEIVERS = new LinkedHashMap<>();
    private static Object lastDesktopLevel;
    private static final int MODE_REFRESH_INTERVAL_TICKS = 100;
    private static final long SUPPORTED_CAPABILITIES = DesktopProtocol.KNOWN_CAPABILITIES | DesktopPackets.CAP_MULTI_MENU_GESTURES | DesktopPackets.CAP_SORT_WINDOWS | DesktopPackets.CAP_CLICK_SEQUENCES | DesktopPackets.CAP_SORT_INVENTORY;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final DesktopConnectionState CONNECTION_STATE = new DesktopConnectionState();
    private static final Map<Integer, Long> SESSION_TOKENS = new LinkedHashMap<>();
    private static final Map<Integer, Integer> SESSION_STATE_IDS = new LinkedHashMap<>();
    private static final Map<Integer, Long> PENDING_SOURCE_GRANTS = new LinkedHashMap<>();
    private static final Map<String, Long> SOURCE_GRANTS = new LinkedHashMap<>();
    private static final Map<String, Integer> SOURCE_SESSIONS = new LinkedHashMap<>();
    private static final Map<Integer, String> SESSION_SOURCE_KEYS = new LinkedHashMap<>();
    private static final Map<Integer, MenuOpenData> PENDING_MENU_OPEN_DATA = new LinkedHashMap<>();
    private static final Map<Integer, SessionSnapshotIdentity> SESSION_SNAPSHOTS = new LinkedHashMap<>();
    private static final Map<Integer,DesktopPackets.DesktopRecipeHoverStatePayload> RECIPE_HOVER_STATES=new LinkedHashMap<>();
    private static int recipeHoverSession=-1, recipeHoverHeartbeat;
    private static long recipeHoverEpoch;
    private static ResourceLocation recipeHoverId;
    private static long clientNonce;
    private static long playerMenuToken;
    private static long modeSequence;
    private static Boolean lastModeSent;
    private static List<String> lastForcedMenuIdsSent;
    private static List<String> lastSupportedMenuIdsSent;
    private static int modeRefreshTicks;
    private static net.minecraft.world.entity.player.Player topologyPlayer;
    private static Object connectionIdentity;
    private static boolean incompatibilityNotified;
    private static boolean protocolRejected;
    private static int channelProbeLogs;
    private static int readyProbeLogs;

    private DesktopContainerClient() {
    }

    public static void initializeNetworking() {
        register(com.salts_inventory_update.network.AutomaticNativeReady.TYPE,
            com.salts_inventory_update.network.AutomaticNativeReady::new, payload -> {
                if (payload.acknowledged() && payload.nonce() == CONNECTION_STATE.connectionNonce()
                    && CONNECTION_STATE.isNegotiated()) automaticNativeReady = true;
            });
        ClientPlayNetworking.registerGlobalReceiver(com.salts_inventory_update.network.AutomaticNativeEnvelope.TYPE,
            (client, listener, buffer, sender) -> {
                try {
                    var envelope = new com.salts_inventory_update.network.AutomaticNativeEnvelope(buffer);
                    var receiver = AUTOMATIC_RECEIVERS.get(envelope.innerType());
                    if (buffer.isReadable() || receiver == null) throw new IllegalArgumentException("Unknown native envelope");
                    // Preserve packet order with the ready acknowledgement on the client
                    // thread. Netty may receive both before the acknowledgement is applied.
                    client.execute(() -> {
                        if (client.getConnection() != listener) return;
                        if (!automaticNativeReady || envelope.nonce() != CONNECTION_STATE.connectionNonce()) {
                            rejectServerProtocol("unnegotiated automatic native envelope"); return;
                        }
                        FriendlyByteBuf body = new FriendlyByteBuf(io.netty.buffer.Unpooled.wrappedBuffer(envelope.body()));
                        try {
                            com.salts_inventory_update.network.AutomaticCountCodec.restore(envelope.counts(), () -> {
                                receiver.receive(client, listener, body, sender); return null;
                            });
                        } catch (RuntimeException failure) { rejectServerProtocol("malformed automatic native envelope"); }
                        finally { body.release(); }
                    });
                } catch (RuntimeException failure) {
                    client.execute(() -> rejectServerProtocol("malformed automatic native envelope"));
                }
            });
        register(DesktopPackets.DesktopClickSequenceAckPayload.TYPE, DesktopPackets.DesktopClickSequenceAckPayload::new, DesktopContainerClient::acknowledgeClickSequence);
        register(DesktopPackets.DesktopRecipeHoverStatePayload.TYPE,DesktopPackets.DesktopRecipeHoverStatePayload::new,payload->{
            if(payload.connectionNonce()!=CONNECTION_STATE.connectionNonce()||payload.epoch()!=recipeHoverEpoch||payload.sessionId()!=recipeHoverSession)return;
            RECIPE_HOVER_STATES.put(payload.sessionId(),payload);
        });
        register(DesktopMenuOpenDataPayload.TYPE, DesktopMenuOpenDataPayload::new,
            payload -> acceptMenuOpenData(payload, Minecraft.getInstance()));
        DesktopDebug.probe("client desktop networking initialize start");
        register(DesktopHelloAckPayload.TYPE, DesktopHelloAckPayload::new, DesktopContainerClient::handleHelloAck);
        register(DesktopSessionAuthorizationPayload.TYPE, DesktopSessionAuthorizationPayload::new, payload -> {
            if (payload.sessionId() < DesktopPackets.PLAYER_MENU_SESSION || payload.sessionToken() == 0L) {
                DesktopDebug.warn("client session authorization rejected session={} token={}", payload.sessionId(), payload.sessionToken());
                return;
            }
            if (payload.sessionId() == DesktopPackets.PLAYER_MENU_SESSION) {
                playerMenuToken = payload.sessionToken();
                return;
            }
            if (!hasCapability(DesktopProtocol.CAP_CUSTOM_WINDOWS, true)) {
                rejectServerProtocol("desktop session authorization without custom-window capability");
                return;
            }
            if (!SESSION_TOKENS.containsKey(payload.sessionId())
                && SESSION_TOKENS.size() >= DesktopProtocol.MAX_DESKTOP_SESSIONS) {
                rejectServerProtocol("too many desktop session authorizations");
                return;
            }
            SESSION_TOKENS.put(payload.sessionId(), payload.sessionToken());
            if (payload.sourceGrantToken() != 0L) {
                PENDING_SOURCE_GRANTS.put(payload.sessionId(), payload.sourceGrantToken());
            }
        });
        register(DesktopOpenSessionPayload.TYPE, DesktopOpenSessionPayload::new, payload -> {
            if (!hasCapability(DesktopProtocol.CAP_CUSTOM_WINDOWS, true)) {
                rejectServerProtocol("desktop session opened without custom-window capability");
                return;
            }
            if (!SaltsInventoryRuntime.isEnabled()) {
                DesktopDebug.trace("client payload open ignored session={} reason=runtime-disabled", payload.sessionId());
                return;
            }
            if (!SESSION_TOKENS.containsKey(payload.sessionId())) {
                rejectServerProtocol("desktop session opened without authorization");
                return;
            }
            DesktopDebug.log("client payload open session={} title={} type={} special={}", payload.sessionId(), payload.title().getString(), payload.menuTypeId(), payload.specialKind());
            SESSION_STATE_IDS.put(payload.sessionId(), payload.stateId());
            Long sourceGrant = PENDING_SOURCE_GRANTS.remove(payload.sessionId());
            if (sourceGrant != null && !payload.sourceKey().isBlank()) {
                SOURCE_GRANTS.put(payload.sourceKey(), sourceGrant);
                while (SOURCE_GRANTS.size() > DesktopProtocol.MAX_LINK_NODES) {
                    SOURCE_GRANTS.remove(SOURCE_GRANTS.keySet().iterator().next());
                }
            }
            String previousSourceKey = SESSION_SOURCE_KEYS.get(payload.sessionId());
            if (previousSourceKey != null && !previousSourceKey.equals(payload.sourceKey())) {
                SOURCE_SESSIONS.remove(previousSourceKey, payload.sessionId());
            }
            if (!payload.sourceKey().isBlank()) {
                SOURCE_SESSIONS.put(payload.sourceKey(), payload.sessionId());
                SESSION_SOURCE_KEYS.put(payload.sessionId(), payload.sourceKey());
            } else {
                SESSION_SOURCE_KEYS.remove(payload.sessionId());
            }
            MenuOpenData openingData = PENDING_MENU_OPEN_DATA.remove(payload.sessionId());
            long authorizedToken = SESSION_TOKENS.getOrDefault(payload.sessionId(), 0L);
            if (openingData != null && (openingData.sessionToken() != authorizedToken || openingData.menuTypeId() != payload.menuTypeId())) {
                rejectServerProtocol("desktop menu opening data did not match its session");
                return;
            }
            MenuType<?> menuType = DesktopPackets.menuTypeById(payload.menuTypeId());
            if (menuType != null && DesktopMenuFactories.hasFactory(menuType) && openingData == null) {
                rejectServerProtocol("desktop menu opening data was missing");
                return;
            }
            Minecraft client = Minecraft.getInstance();
            if (resynchronizeSession(client, payload, authorizedToken, openingData)) return;
            SESSION_SNAPSHOTS.remove(payload.sessionId());
            DesktopContainerSession session;
            try {
                session = DesktopContainerSession.create(client, payload,
                    openingData == null ? new byte[0] : openingData.data(),
                    openingData == null ? -1 : openingData.replacesSessionId());
            } catch (RuntimeException failure) {
                if (payload.specialKind() != DesktopPackets.SPECIAL_AUTOMATIC_NATIVE) throw failure;
                DesktopDebug.warn("client native reconstruction declined session={} reason={}", payload.sessionId(), failure.toString());
                declineAutomaticWindow(payload.sessionId());
                return;
            }
            if (openingData != null) session.setCompanionOwners(openingData.companionOwnerIds());
            InventoryDesktopScreen.openOrAddSession(client, session, payload.visible());
            if (InventoryDesktopScreen.sessionForNetworking(client, payload.sessionId()) == session)
                SESSION_SNAPSHOTS.put(payload.sessionId(), SessionSnapshotIdentity.capture(session.menu(), payload,
                    authorizedToken, openingData == null ? new byte[0] : openingData.data()));
        });
        register(DesktopSlotPayload.TYPE, DesktopSlotPayload::new, payload -> {
            if (payload.sessionId() > DesktopPackets.PLAYER_MENU_SESSION) {
                SESSION_STATE_IDS.put(payload.sessionId(), payload.stateId());
            }
            InventoryDesktopScreen screen = InventoryDesktopScreen.current(Minecraft.getInstance());
            if (screen != null) {
                screen.updateSessionSlot(payload.sessionId(), payload.slotIndex(), payload.stateId(), payload.stack());
            } else if (payload.sessionId() == DesktopPackets.PLAYER_MENU_SESSION) {
                Minecraft minecraft = Minecraft.getInstance();
                if (minecraft.player != null) {
                    InventoryExpansion.ensurePlayerMenuCanReadSlotCount(minecraft.player, payload.slotIndex() + 1);
                    if (payload.slotIndex() >= 0 && payload.slotIndex() < minecraft.player.inventoryMenu.slots.size()) {
                        minecraft.player.inventoryMenu.setItem(payload.slotIndex(), payload.stateId(), payload.stack());
                    }
                }
            }
        });
        register(DesktopDataPayload.TYPE, DesktopDataPayload::new, payload -> {
            InventoryDesktopScreen screen = InventoryDesktopScreen.current(Minecraft.getInstance());
            if (screen != null) {
                screen.updateSessionData(payload.sessionId(), payload.dataSlot(), payload.value());
            }
        });
        register(DesktopCarriedPayload.TYPE, DesktopCarriedPayload::new, payload -> {
            DesktopDebug.trace("client payload carried stack={}", payload.carried());
            Minecraft client = Minecraft.getInstance();
            InventoryDesktopScreen screen = InventoryDesktopScreen.current(client);
            if (screen != null) {
                screen.setSharedCarried(payload.carried());
            } else if (client.player != null) {
                client.player.inventoryMenu.setCarried(payload.carried().copy());
            }
        });
        register(DesktopSessionClosedPayload.TYPE, DesktopSessionClosedPayload::new, payload -> {
            SESSION_TOKENS.remove(payload.sessionId());
            SESSION_SNAPSHOTS.remove(payload.sessionId());
            SESSION_STATE_IDS.remove(payload.sessionId());
            PENDING_SOURCE_GRANTS.remove(payload.sessionId());
            String sourceKey = SESSION_SOURCE_KEYS.remove(payload.sessionId());
            if (sourceKey != null) {
                SOURCE_SESSIONS.remove(sourceKey, payload.sessionId());
            }
            InventoryDesktopScreen screen = InventoryDesktopScreen.current(Minecraft.getInstance());
            if (screen != null) {
                screen.removeSession(payload.sessionId());
            }
        });
        register(DesktopSessionVisibilityPayload.TYPE, DesktopSessionVisibilityPayload::new, payload -> {
            InventoryDesktopScreen screen = InventoryDesktopScreen.current(Minecraft.getInstance());
            if (screen != null) {
                screen.setSessionVisible(payload.sessionId(), payload.visible());
            }
        });
        register(DesktopMerchantOffersPayload.TYPE, DesktopMerchantOffersPayload::new, payload -> {
            InventoryDesktopScreen screen = InventoryDesktopScreen.current(Minecraft.getInstance());
            if (screen != null) {
                screen.applyMerchantOffers(payload);
            }
        });
        register(DesktopCustomPayload.TYPE, DesktopCustomPayload::new, payload -> {
            InventoryDesktopScreen screen = InventoryDesktopScreen.current(Minecraft.getInstance());
            if (screen != null) {
                screen.applyCustomPayload(payload);
            }
        });
        register(DesktopGhostRecipePayload.TYPE, DesktopGhostRecipePayload::new, payload -> {
            InventoryDesktopScreen screen = InventoryDesktopScreen.current(Minecraft.getInstance());
            if (screen != null) {
                screen.applyGhostRecipe(payload);
            }
        });
        register(DesktopPackets.DesktopPlayerSlotPayload.TYPE, DesktopPackets.DesktopPlayerSlotPayload::new, payload -> {
            var client = Minecraft.getInstance();
            if (client.player == null) return;
            var menu = client.player.inventoryMenu;
            for (int i = 46; i < menu.slots.size(); i++) {
                if (payload.key().equals(com.salts_inventory_update.api.desktop.DesktopPlayerMenuApi.slotKey(menu.slots.get(i)))) {
                    menu.setItem(i, payload.stateId(), payload.stack());
                    break;
                }
            }
        });
        register(InventoryExpansionSyncPayload.TYPE, InventoryExpansionSyncPayload::new, payload -> {
            DesktopDebug.trace("client payload inventory expansion slots={} stacks={}", payload.slotCount(), payload.items().size());
            Minecraft client = Minecraft.getInstance();
            if (client.player != null && hasCapability(DesktopProtocol.CAP_INVENTORY_TOPOLOGY, false)) {
                InventoryExpansion.setTopologyEnabled(client.player, true);
                InventoryExpansion.setGameplayEnabled(client.player, CONNECTION_STATE.isUiEnabled());
                int slotCount = InventoryExpansion.clampSlotCount(payload.slotCount());
                InventoryExpansion.access(client.player).salts_inventory_update$setExtraSlotCount(slotCount);
                InventoryExpansion.access(client.player).salts_inventory_update$getExtraInventory().loadSnapshot(slotCount, payload.items());
                InventoryExpansion.appendMissingMenuSlots(client.player.inventoryMenu, client.player);
                InventoryDesktopScreen screen = InventoryDesktopScreen.current(client);
                if (screen != null) {
                    screen.refreshInventoryWindowLayout();
                }
            }
        });
        DesktopDebug.probe("client desktop networking initialize complete");
    }

    private static <P extends DesktopPacket> void register(ResourceLocation id, Function<FriendlyByteBuf, P> decoder, Consumer<P> handler) {
        DesktopDebug.probe("client desktop payload receiver register id={}", id);
        ClientPlayNetworking.PlayChannelHandler receiver = (client, listener, buf, sender) -> {
            try {
                P payload = decoder.apply(buf);
                com.salts_inventory_update.network.AutomaticCountCodec.requireComplete();
                if (buf.isReadable()) {
                    throw new IllegalArgumentException("Trailing bytes in desktop payload " + id + ": " + buf.readableBytes());
                }
                client.execute(() -> {
                    try {
                        if (protocolRejected || client.getConnection() != listener) {
                            return;
                        }
                        if (!id.equals(DesktopHelloAckPayload.TYPE) && !CONNECTION_STATE.isNegotiated()) {
                            rejectServerProtocol("desktop payload received before negotiation");
                            return;
                        }
                        handler.accept(payload);
                    } catch (RuntimeException exception) {
                        rejectServerProtocol("invalid " + id + " payload: " + exception.getClass().getSimpleName());
                    }
                });
            } catch (RuntimeException exception) {
                client.execute(() -> {
                    if (client.getConnection() == listener) {
                        rejectServerProtocol("malformed " + id + " payload: " + exception.getClass().getSimpleName());
                    }
                });
            }
        };
        AUTOMATIC_RECEIVERS.put(id, receiver);
        ClientPlayNetworking.registerGlobalReceiver(id, receiver);
    }

    private static void rejectServerProtocol(String reason) {
        if (protocolRejected) {
            return;
        }
        protocolRejected = true;
        Minecraft minecraft = Minecraft.getInstance();
        DesktopDebug.warn("client desktop protocol connection rejected reason={}", reason);
        if (minecraft.getConnection() != null) {
            minecraft.getConnection().getConnection().disconnect(Component.literal(
                "Salt's Inventory Update network protocol error."
            ));
        }
    }

    private static void handleHelloAck(DesktopHelloAckPayload payload) {
        boolean accepted = payload.accepted()
            && payload.playerMenuToken() != 0L
            && CONNECTION_STATE.acknowledge(
                payload.protocolVersion(),
                payload.echoedClientNonce(),
                payload.connectionNonce(),
                payload.capabilities(),
                payload.uiEnabled(),
                SUPPORTED_CAPABILITIES
            );
        if (!accepted) {
            CONNECTION_STATE.markIncompatible();
            SaltsInventoryRuntime.setServerDesktopAvailable(false);
            notifyIncompatible(Minecraft.getInstance(), "the server rejected desktop protocol v" + DesktopProtocol.VERSION);
            DesktopDebug.warn(
                "client desktop negotiation rejected protocol={} echoedNonce={} accepted={}",
                payload.protocolVersion(),
                payload.echoedClientNonce(),
                payload.accepted()
            );
            return;
        }

        playerMenuToken = payload.playerMenuToken();
        lastModeSent = payload.uiEnabled();
        modeRefreshTicks = 0;
        if (topologyPlayer != null) {
            boolean topologyEnabled = hasCapability(DesktopProtocol.CAP_INVENTORY_TOPOLOGY, false);
            InventoryExpansion.setTopologyEnabled(topologyPlayer, topologyEnabled);
            InventoryExpansion.setGameplayEnabled(
                topologyPlayer,
                topologyEnabled && hasCapability(DesktopProtocol.CAP_INVENTORY_TOPOLOGY, true)
            );
            if (topologyEnabled) {
                InventoryExpansion.appendMissingMenuSlots(topologyPlayer.inventoryMenu, topologyPlayer);
            }
        }
        SaltsInventoryRuntime.setServerDesktopAvailable(true);
        if (ClientPlayNetworking.canSend(com.salts_inventory_update.network.AutomaticNativeReady.TYPE)) {
            sendRaw(new com.salts_inventory_update.network.AutomaticNativeReady(CONNECTION_STATE.connectionNonce(), false), "automatic-native-ready");
            refreshNativeTemplates();
        }
        DesktopDebug.log(
            "client desktop negotiation active capabilities={} uiEnabled={}",
            payload.capabilities(),
            payload.uiEnabled()
        );
    }

    private static void resetConnection() {
        automaticNativeReady = false;
        resetClickSequences();
        if (topologyPlayer != null) {
            InventoryExpansion.setGameplayEnabled(topologyPlayer, false);
            InventoryExpansion.setTopologyEnabled(topologyPlayer, false);
        }
        CONNECTION_STATE.reset();
        SESSION_TOKENS.clear();
        SESSION_SNAPSHOTS.clear();
        SESSION_STATE_IDS.clear();
        PENDING_SOURCE_GRANTS.clear();
        PENDING_MENU_OPEN_DATA.clear();
        SOURCE_GRANTS.clear();
        SOURCE_SESSIONS.clear();
        SESSION_SOURCE_KEYS.clear();
        clientNonce = 0L;
        playerMenuToken = 0L;
        recipeHoverSession=-1;recipeHoverId=null;RECIPE_HOVER_STATES.clear();
        modeSequence = 0L;
        lastModeSent = null;
        lastForcedMenuIdsSent = null;
        lastSupportedMenuIdsSent = null;
        modeRefreshTicks = 0;
        topologyPlayer = null;
        connectionIdentity = null;
        incompatibilityNotified = false;
        protocolRejected = false;
    }

    private static long nextNonce() {
        long nonce;
        do {
            nonce = SECURE_RANDOM.nextLong();
        } while (nonce == 0L);
        return nonce;
    }

    public static void tick(Minecraft minecraft) {
        tickClickSequences();
        tickRecipeHover();
        if (lastDesktopLevel != null && lastDesktopLevel != minecraft.level) {
            InventoryDesktopScreen.reset(minecraft);
            // Dimension travel ends window lifetimes, not the authenticated connection.
            // Keep the player token and monotonic mutation sequence, including any sent acknowledgement.
            SESSION_TOKENS.clear();
            SESSION_SNAPSHOTS.clear();
            PENDING_MENU_OPEN_DATA.clear();
            SESSION_STATE_IDS.clear();
            PENDING_SOURCE_GRANTS.clear();
            SOURCE_GRANTS.clear();
            SOURCE_SESSIONS.clear();
            SESSION_SOURCE_KEYS.clear();
        }
        lastDesktopLevel = minecraft.level;
        Object currentConnection = minecraft.getConnection();
        if (currentConnection == null) {
            if (connectionIdentity != null) {
                resetConnection();
            }
            SaltsInventoryRuntime.setServerDesktopAvailable(false);
            return;
        }
        if (connectionIdentity != currentConnection) {
            resetConnection();
            connectionIdentity = currentConnection;
        }
        if (minecraft.player == null || minecraft.level == null) {
            SaltsInventoryRuntime.setServerDesktopAvailable(false);
            return;
        }
        if (topologyPlayer != minecraft.player) {
            if (topologyPlayer != null) {
                InventoryExpansion.setTopologyEnabled(topologyPlayer, false);
            }
            topologyPlayer = minecraft.player;
            boolean topologyEnabled = hasCapability(DesktopProtocol.CAP_INVENTORY_TOPOLOGY, false);
            InventoryExpansion.setTopologyEnabled(topologyPlayer, topologyEnabled);
            InventoryExpansion.setGameplayEnabled(topologyPlayer, hasCapability(DesktopProtocol.CAP_INVENTORY_TOPOLOGY, true));
            if (topologyEnabled) {
                InventoryExpansion.appendMissingMenuSlots(topologyPlayer.inventoryMenu, topologyPlayer);
            }
        }

        boolean channelsAvailable = canUseServerSessionsRaw();
        boolean saltChannelDetected = hasAnyNegotiationChannel();
        boolean legacyReadyDetected = hasLegacyReadyChannel();
        if (legacyReadyDetected && !channelsAvailable) {
            rejectLegacyConnection(minecraft);
            CONNECTION_STATE.markIncompatible();
        }
        if (!channelsAvailable && saltChannelDetected) {
            CONNECTION_STATE.markIncompatible();
            notifyIncompatible(minecraft, "the server exposes an incomplete desktop protocol");
        }
        if (clientNonce == 0L && channelsAvailable) {
            clientNonce = nextNonce();
            CONNECTION_STATE.begin(clientNonce, minecraft.level.getGameTime());
            List<String> forcedMenuIds = configuredForcedMenuIds();
            List<String> supportedMenuIds = com.salts_inventory_update.client.automatic.AutomaticNativeWindow.advertisedMenuIds(
                com.salts_inventory_update.api.client.desktop.DesktopClientApi.supportedMenuIds());
            boolean sent = sendRaw(new DesktopHelloPayload(
                DesktopProtocol.VERSION,
                clientNonce,
                SUPPORTED_CAPABILITIES,
                SaltsInventoryRuntime.isConfiguredEnabled(),
                forcedMenuIds, supportedMenuIds
            ), "hello");
            if (!sent) {
                CONNECTION_STATE.reset();
                clientNonce = 0L;
            } else {
                lastForcedMenuIdsSent = forcedMenuIds;
                lastSupportedMenuIdsSent = supportedMenuIds;
            }
        }
        boolean expired = CONNECTION_STATE.expireIfNecessary(minecraft.level.getGameTime(), channelsAvailable);
        if (expired && channelsAvailable && CONNECTION_STATE.phase() == DesktopConnectionState.Phase.INCOMPATIBLE) {
            notifyIncompatible(minecraft, "the server did not complete the desktop protocol handshake");
        }
        boolean desktopAvailable = CONNECTION_STATE.isUiEnabled();
        SaltsInventoryRuntime.setServerDesktopAvailable(desktopAvailable);
        if (readyProbeLogs < 24) {
            readyProbeLogs++;
            DesktopDebug.probe(
                "client negotiation tick player={} runtime={} channels={} desktopAvailable={} phase={} screen={}",
                minecraft.player.getName().getString(),
                SaltsInventoryRuntime.isEnabled(),
                channelsAvailable,
                desktopAvailable,
                CONNECTION_STATE.phase(),
                minecraft.screen == null ? "null" : minecraft.screen.getClass().getName()
            );
        }

        if (CONNECTION_STATE.isNegotiated()) {
            boolean topologyEnabled = hasCapability(DesktopProtocol.CAP_INVENTORY_TOPOLOGY, false);
            InventoryExpansion.setTopologyEnabled(minecraft.player, topologyEnabled);
            boolean desiredMode = SaltsInventoryRuntime.isConfiguredEnabled();
            List<String> forcedMenuIds = configuredForcedMenuIds();
            List<String> supportedMenuIds = com.salts_inventory_update.client.automatic.AutomaticNativeWindow.advertisedMenuIds(
                com.salts_inventory_update.api.client.desktop.DesktopClientApi.supportedMenuIds());
            modeRefreshTicks++;
            InventoryExpansion.setGameplayEnabled(
                minecraft.player,
                desiredMode && hasCapability(DesktopProtocol.CAP_INVENTORY_TOPOLOGY, true)
            );
            if (lastModeSent == null
                || lastModeSent.booleanValue() != desiredMode
                || !forcedMenuIds.equals(lastForcedMenuIdsSent)
                || !supportedMenuIds.equals(lastSupportedMenuIdsSent)
                || modeRefreshTicks >= MODE_REFRESH_INTERVAL_TICKS) {
                long sequence = modeSequence++;
                DesktopModePayload mode = new DesktopModePayload(
                    CONNECTION_STATE.connectionNonce(),
                    sequence,
                    desiredMode,
                    forcedMenuIds, supportedMenuIds
                );
                if (sendRaw(mode, desiredMode ? "mode-enabled" : "mode-disabled")
                    && CONNECTION_STATE.updateMode(CONNECTION_STATE.connectionNonce(), sequence, desiredMode)) {
                    lastModeSent = desiredMode;
                    lastForcedMenuIdsSent = forcedMenuIds;
                lastSupportedMenuIdsSent = supportedMenuIds;
                    modeRefreshTicks = 0;
                    InventoryExpansion.setGameplayEnabled(
                        minecraft.player,
                        desiredMode && hasCapability(DesktopProtocol.CAP_INVENTORY_TOPOLOGY, true)
                    );
                }
            }
        } else {
            InventoryExpansion.setTopologyEnabled(minecraft.player, false);
            InventoryExpansion.setGameplayEnabled(minecraft.player, false);
        }
    }

    private static List<String> configuredForcedMenuIds() {
        List<String> configured = SaltsInventoryConfig.get().forcedContainerWindows;
        if (configured.isEmpty()) {
            return List.of();
        }

        return configured.stream()
            .filter(id -> id != null && !id.isBlank() && id.length() <= DesktopProtocol.MAX_IDENTIFIER_LENGTH)
            .limit(DesktopProtocol.MAX_FORCED_MENU_IDS)
            .toList();
    }

    public static boolean canSendDesktopPackets() {
        if (!SaltsInventoryRuntime.isEnabled()) {
            return false;
        }

        try {
            return CONNECTION_STATE.authorizes(CONNECTION_STATE.connectionNonce(), 0L, true)
                && ClientPlayNetworking.canSend(DesktopAuthenticatedPayload.TYPE);
        } catch (IllegalStateException | IllegalArgumentException ignored) {
            return false;
        }
    }

    public static boolean canUseServerSessions() {
        if (!SaltsInventoryRuntime.isEnabled()) {
            return false;
        }

        return CONNECTION_STATE.authorizes(
            CONNECTION_STATE.connectionNonce(),
            DesktopProtocol.CAP_CUSTOM_WINDOWS,
            true
        );
    }

    private record SequenceClick(int sessionId,int slotIndex,int button,boolean quickMove,int targetKind,int targetSessionId) { }
    private static final java.util.ArrayDeque<List<SequenceClick>> CLICK_SEQUENCES = new java.util.ArrayDeque<>();
    private static @Nullable List<SequenceClick> collectedClicks;
    private static long nextClickSequence = 1, inFlightClickSequence, clickSequenceSentAt, slotMutationDiscardEpoch;
    private static boolean clickSequencesBlocked;

    public static boolean supportsClickSequences() {
        return hasCapability(DesktopPackets.CAP_CLICK_SEQUENCES, true) && !clickSequencesBlocked;
    }
    public static boolean hasPendingSlotMutations() {
        return !clickSequencesBlocked && (inFlightClickSequence != 0 || !CLICK_SEQUENCES.isEmpty());
    }
    public static long slotMutationDiscardEpoch() { return slotMutationDiscardEpoch; }
    public static void beginClickSequence() { collectedClicks = new ArrayList<>(); }
    public static void endClickSequence() {
        List<SequenceClick> collected = collectedClicks; collectedClicks = null;
        if (collected == null || collected.isEmpty()) return;
        if (!supportsClickSequences() || collected.size() > DesktopPackets.MAX_GESTURE_SLOTS || CLICK_SEQUENCES.size() >= 128) {
            slotMutationDiscardEpoch++; return;
        }
        CLICK_SEQUENCES.addLast(List.copyOf(collected));
        dispatchClickSequence();
    }
    private static boolean touchesPlayerMenu(List<SequenceClick> clicks) {
        return clicks.stream().anyMatch(click -> click.sessionId() == DesktopPackets.PLAYER_MENU_SESSION
            || click.targetKind() >= 0 && click.targetSessionId() == DesktopPackets.PLAYER_MENU_SESSION);
    }
    public static void discardQueuedPlayerMenuMutations() {
        // Drop complete gestures. Keep the in-flight acknowledgement and sequence numbering intact.
        boolean discarded = CLICK_SEQUENCES.removeIf(DesktopContainerClient::touchesPlayerMenu);
        if (collectedClicks != null && touchesPlayerMenu(collectedClicks)) { collectedClicks = null; discarded = true; }
        if (discarded) slotMutationDiscardEpoch++;
    }
    private static void resetClickSequences() {
        CLICK_SEQUENCES.clear(); collectedClicks = null; inFlightClickSequence = 0;
        nextClickSequence = 1; clickSequencesBlocked = false; slotMutationDiscardEpoch++;
    }
    private static void tickClickSequences() {
        if (inFlightClickSequence != 0 && System.nanoTime() - clickSequenceSentAt > 10_000_000_000L && !clickSequencesBlocked) {
            CLICK_SEQUENCES.clear(); clickSequencesBlocked = true; slotMutationDiscardEpoch++;
            DesktopDebug.warn("client click sequence paused reason=ack-timeout");
        }
    }
    private static void dispatchClickSequence() {
        if (inFlightClickSequence != 0 || CLICK_SEQUENCES.isEmpty() || !supportsClickSequences()) return;
        List<SequenceClick> clicks = CLICK_SEQUENCES.removeFirst();
        List<DesktopPackets.DesktopClickAction> actions = new ArrayList<>();
        try {
            for (SequenceClick click : clicks) actions.add(new DesktopPackets.DesktopClickAction(
                new DesktopSlotReference(click.sessionId(), tokenFor(click.sessionId()), stateIdFor(click.sessionId()), click.slotIndex()),
                click.button(),click.quickMove(),click.targetKind(),click.targetKind()>=0?new DesktopSessionReference(click.targetSessionId(),tokenFor(click.targetSessionId()),stateIdFor(click.targetSessionId())):null));
        } catch (IllegalArgumentException unavailable) {
            CLICK_SEQUENCES.clear(); slotMutationDiscardEpoch++; return;
        }
        inFlightClickSequence = nextClickSequence++; clickSequenceSentAt = System.nanoTime();
        if (!send(new DesktopPackets.DesktopClickSequencePayload(inFlightClickSequence, actions), "click-sequence")) {
            CLICK_SEQUENCES.clear(); inFlightClickSequence = 0; clickSequencesBlocked = true; slotMutationDiscardEpoch++;
        }
    }
    private static void acknowledgeClickSequence(DesktopPackets.DesktopClickSequenceAckPayload ack) {
        if (ack.connectionNonce() != CONNECTION_STATE.connectionNonce() || ack.sequence() != inFlightClickSequence) return;
        inFlightClickSequence = 0;
        if (!ack.accepted()) { CLICK_SEQUENCES.clear(); slotMutationDiscardEpoch++; }
        dispatchClickSequence();
    }

    public static boolean canDispatchOptimisticMutationNow() {
        return canSendDesktopPackets() && !hasPendingSlotMutations();
    }

    private static boolean resynchronizeSession(Minecraft minecraft, DesktopOpenSessionPayload payload,
                                                long authorizedToken, @Nullable MenuOpenData opening) {
        InventoryDesktopScreen screen = InventoryDesktopScreen.current(minecraft);
        DesktopContainerSession existing = InventoryDesktopScreen.sessionForNetworking(minecraft, payload.sessionId());
        SessionSnapshotIdentity identity = SESSION_SNAPSHOTS.get(payload.sessionId());
        byte[] data = opening == null ? new byte[0] : opening.data();
        int replacement = opening == null ? -1 : opening.replacesSessionId();
        if (screen == null || minecraft.screen == null || existing == null || identity == null
            || !identity.matches(existing.menu(), payload, authorizedToken, data, replacement)) return false;
        // Stale-state recovery is another snapshot of the same authenticated lifetime.
        // Replacing its real window here would interrupt an otherwise valid recipe preview.
        identity.apply(payload);
        if (!identity.matches(existing.menu(), payload, authorizedToken, data, replacement)) return false;
        if (opening != null && existing.isCompanion()) screen.updateCompanionOwners(payload.sessionId(), opening.companionOwnerIds());
        screen.setSharedCarried(payload.carried());
        DesktopDebug.trace("client session resynchronized in place session={} state={}", payload.sessionId(), payload.stateId());
        return true;
    }

    public static void acceptMenuOpenData(DesktopMenuOpenDataPayload payload, Minecraft client) {
        if (!CONNECTION_STATE.authorizes(payload.connectionNonce(), DesktopProtocol.CAP_CUSTOM_WINDOWS, true)) {
            return;
        }
        Long sessionToken = SESSION_TOKENS.get(payload.sessionId());
        if (sessionToken == null || sessionToken.longValue() != payload.sessionToken()) {
            rejectServerProtocol("desktop menu opening data was not authorized");
            return;
        }
        if (payload.metadataOnly()) {
            Long token = SESSION_TOKENS.get(payload.sessionId());
            if (token == null || token.longValue() != payload.sessionToken()) return;
            InventoryDesktopScreen screen = InventoryDesktopScreen.current(client);
            if (screen != null) screen.updateCompanionOwners(payload.sessionId(), payload.companionOwnerIds());
            return;
        }
        PENDING_MENU_OPEN_DATA.put(payload.sessionId(), new MenuOpenData(
            payload.sessionToken(), payload.menuTypeId(), payload.replacesSessionId(), payload.data(), payload.companionOwnerIds()
        ));
    }

    private static boolean canUseServerSessionsRaw() {
        try {
            boolean hello = ClientPlayNetworking.canSend(DesktopHelloPayload.TYPE);
            boolean mode = ClientPlayNetworking.canSend(DesktopModePayload.TYPE);
            boolean authenticated = ClientPlayNetworking.canSend(DesktopAuthenticatedPayload.TYPE);
            boolean result = hello && mode && authenticated;
            if (channelProbeLogs < 16) {
                channelProbeLogs++;
                DesktopDebug.probe(
                    "client server negotiation channel probe result={} hello={} mode={} authenticated={}",
                    result,
                    hello,
                    mode,
                    authenticated
                );
            }
            return result;
        } catch (IllegalStateException | IllegalArgumentException ignored) {
            if (channelProbeLogs < 16) {
                channelProbeLogs++;
                DesktopDebug.probe("client server session channel probe failed reason={}", ignored.toString());
            }
            return false;
        }
    }

    private static boolean hasAnyNegotiationChannel() {
        try {
            return ClientPlayNetworking.canSend(DesktopHelloPayload.TYPE)
                || ClientPlayNetworking.canSend(DesktopModePayload.TYPE)
                || ClientPlayNetworking.canSend(DesktopAuthenticatedPayload.TYPE)
                || ClientPlayNetworking.canSend(DesktopPackets.LEGACY_READY_TYPE);
        } catch (IllegalStateException | IllegalArgumentException ignored) {
            return false;
        }
    }

    private static boolean hasLegacyReadyChannel() {
        try {
            return ClientPlayNetworking.canSend(DesktopPackets.LEGACY_READY_TYPE);
        } catch (IllegalStateException | IllegalArgumentException ignored) {
            return false;
        }
    }

    private static void rejectLegacyConnection(Minecraft minecraft) {
        Component message = Component.literal(
            "Salts Inventory Update versions are incompatible. Update the mod on both client and server."
        );
        notifyIncompatible(minecraft, "the server uses the legacy desktop protocol; update the mod on both sides");
        if (minecraft.getConnection() != null) {
            minecraft.getConnection().getConnection().disconnect(message);
        }
    }

    private static void notifyIncompatible(Minecraft minecraft, String reason) {
        if (incompatibilityNotified || minecraft.player == null) {
            return;
        }
        incompatibilityNotified = true;
        minecraft.player.displayClientMessage(
            Component.literal("Salts Inventory Update is disabled for this connection: " + reason + "."),
            false
        );
    }

    public static boolean clickSlot(int debugId, int sessionId, int slotIndex, int button, ClickType input, ItemStack clientCarried) {
        if (collectedClicks != null) {collectedClicks.add(new SequenceClick(sessionId,slotIndex,button,input==ClickType.QUICK_MOVE,-1,0));return true;}
        DesktopDebug.trace("client send click id={} session={} slot={} button={} input={} clientCarried={}", debugId, sessionId, slotIndex, button, input, clientCarried);
        return send(new DesktopClickPayload(debugId, sessionId, slotIndex, button, input.name(), clientCarried.copy()), "click");
    }

    public static boolean quickMoveSlot(int sourceSessionId, int sourceSlotIndex, int targetKind, int targetSessionId) {
        if (targetKind < DesktopPackets.QUICK_TARGET_DEFAULT || targetKind > DesktopPackets.QUICK_TARGET_PLAYER_STORAGE) {
            DesktopDebug.trace(
                "client quick move skipped sourceSession={} sourceSlot={} targetKind={} reason=invalid-target-kind",
                sourceSessionId,
                sourceSlotIndex,
                targetKind
            );
            return false;
        }
        if (targetKind == DesktopPackets.QUICK_TARGET_SESSION
            && (targetSessionId == sourceSessionId || targetSessionId == DesktopPackets.PLAYER_MENU_SESSION)) {
            DesktopDebug.trace(
                "client quick move skipped sourceSession={} targetSession={} reason=invalid-target-session",
                sourceSessionId,
                targetSessionId
            );
            return false;
        }
        DesktopDebug.trace(
            "client send quick move sourceSession={} sourceSlot={} targetKind={} targetSession={}",
            sourceSessionId,
            sourceSlotIndex,
            targetKind,
            targetSessionId
        );
        if (supportsClickSequences()) {
            if (CLICK_SEQUENCES.size() >= 128) return false;
            CLICK_SEQUENCES.addLast(List.of(new SequenceClick(sourceSessionId,sourceSlotIndex,0,true,targetKind,
                targetKind == DesktopPackets.QUICK_TARGET_SESSION ? targetSessionId : DesktopPackets.PLAYER_MENU_SESSION)));
            dispatchClickSequence(); return true;
        }
        return send(new DesktopQuickMovePayload(sourceSessionId, sourceSlotIndex, targetKind, targetSessionId), "quick-move");
    }

    public record SlotTarget(int sessionId, int slotIndex) {
    }

    public static boolean dragSlots(int quickCraftType, List<SlotTarget> targets) {
        if (targets.isEmpty() || !hasCapability(DesktopPackets.CAP_MULTI_MENU_GESTURES, true)) {
            return false;
        }
        return send(new DesktopDragSlotsPayload(quickCraftType, targets.stream().map(DesktopContainerClient::slotReference).toList()), "drag-slots");
    }

    public static boolean pickupAll(int anchorSessionId, int anchorSlotIndex, int button, List<Integer> sourceSessions) {
        if (!hasCapability(DesktopPackets.CAP_MULTI_MENU_GESTURES, true)) return false;
        return send(new DesktopPickupAllPayload(
            anchorSessionId,
            anchorSlotIndex,
            button,
            sourceSessions.stream().map(DesktopContainerClient::sessionReference).toList()
        ), "pickup-all");
    }

    public static boolean canSortWindows() {
        return hasCapability(DesktopPackets.CAP_SORT_WINDOWS, true);
    }

    public static boolean sortWindows(int sourceId, List<Integer> destinations, int focusedId, boolean shift) {
        if (!canSortWindows() || destinations.isEmpty() || tokenFor(sourceId) == 0L || stateIdFor(sourceId) < 0) return false;
        List<Integer> ids = destinations.stream().distinct().toList();
        if (ids.stream().anyMatch(id -> tokenFor(id) == 0L || stateIdFor(id) < 0)) return false;
        return send(new DesktopSortWindowsPayload(sessionReference(sourceId),
            ids.stream().map(DesktopContainerClient::sessionReference).toList(), focusedId, shift), "sort-windows");
    }

    public static boolean canSortInventory() {
        return canSortWindows() && hasCapability(DesktopPackets.CAP_SORT_INVENTORY, true);
    }

    public static boolean sortInventory(int sessionId) {
        if (!canSortInventory() || tokenFor(sessionId) == 0L || stateIdFor(sessionId) < 0) return false;
        return send(new DesktopPackets.DesktopSortInventoryPayload(sessionReference(sessionId)), "sort-inventory");
    }

    public static boolean quickMoveSlots(List<SlotTarget> sources, int targetKind, int targetSessionId) {
        if (targetKind < DesktopPackets.QUICK_TARGET_DEFAULT || targetKind > DesktopPackets.QUICK_TARGET_PLAYER_STORAGE) return false;
        if (sources.isEmpty() || !hasCapability(DesktopPackets.CAP_MULTI_MENU_GESTURES, true)) return false;
        int target = targetKind == DesktopPackets.QUICK_TARGET_SESSION ? targetSessionId : DesktopPackets.PLAYER_MENU_SESSION;
        return send(new DesktopQuickMoveAllPayload(
            sources.stream().map(DesktopContainerClient::slotReference).toList(),
            targetKind,
            sessionReference(target)
        ), "quick-move-all");
    }

    private static DesktopSlotReference slotReference(SlotTarget target) {
        return new DesktopSlotReference(target.sessionId(), tokenFor(target.sessionId()), stateIdFor(target.sessionId()), target.slotIndex());
    }

    private static DesktopSessionReference sessionReference(int sessionId) {
        return new DesktopSessionReference(sessionId, tokenFor(sessionId), stateIdFor(sessionId));
    }

    public static boolean clickButton(int sessionId, int buttonId) {
        DesktopDebug.trace("client send button session={} button={}", sessionId, buttonId);
        return send(new DesktopButtonPayload(sessionId, buttonId), "button");
    }

    public static boolean craftToWindow(int source,int target,ResourceLocation recipe){return contextualCraft(source,target,recipe,false);}
    public static boolean craftStackToWindow(int source,int target,ResourceLocation recipe){return contextualCraft(source,target,recipe,true);}
    private static boolean contextualCraft(int source,int target,ResourceLocation recipe,boolean stack){
        return recipe!=null&&tokenFor(source)!=0&&tokenFor(target)!=0&&send(new DesktopPackets.DesktopCraftToWindowPayload(source,target,recipe,stack),"contextual-craft");
    }
    public static boolean prepareCookingRecipe(int source,ResourceLocation recipe){return recipe!=null&&send(new DesktopPackets.DesktopCookingRecipePayload(source,recipe),"contextual-cooking");}
    public static boolean beginRecipeHover(int source){
        if(tokenFor(source)==0||!hasCapability(DesktopProtocol.CAP_RECIPE_TRANSFER,true))return false;
        if(recipeHoverSession>=0)endRecipeHover(recipeHoverSession);
        recipeHoverSession=source;recipeHoverEpoch=recipeHoverEpoch==Long.MAX_VALUE?1:recipeHoverEpoch+1;recipeHoverId=null;recipeHoverHeartbeat=0;RECIPE_HOVER_STATES.remove(source);
        return send(new DesktopPackets.DesktopRecipeHoverPayload(source,recipeHoverEpoch,DesktopPackets.RECIPE_HOVER_BEGIN,null),"recipe-hover-begin");
    }
    private static void tickRecipeHover(){
        if(recipeHoverSession<0)return;
        var state=RECIPE_HOVER_STATES.get(recipeHoverSession);
        boolean sameEpoch=state!=null&&state.epoch()==recipeHoverEpoch;
        // A rejected begin is terminal; the presentation returns to its normal pair.
        if(sameEpoch&&!state.active())return;
        boolean acknowledged=sameEpoch&&state.active();
        boolean current=acknowledged&&java.util.Objects.equals(state.recipeId(),recipeHoverId);
        // The legacy protocol authenticates the native menu state. Clearing the grid or
        // a furnace tick can advance it before the next selection arrives. Retry only
        // until the server acknowledges that selection, retaining all state validation.
        if(++recipeHoverHeartbeat<(current?20:4))return;
        recipeHoverHeartbeat=0;
        int action=acknowledged?DesktopPackets.RECIPE_HOVER_UPDATE:DesktopPackets.RECIPE_HOVER_BEGIN;
        send(new DesktopPackets.DesktopRecipeHoverPayload(recipeHoverSession,recipeHoverEpoch,action,recipeHoverId),"recipe-hover-heartbeat");
    }
    public static boolean updateRecipeHover(int source,ResourceLocation recipe){
        if(recipeHoverSession!=source)return false;recipeHoverId=recipe;recipeHoverHeartbeat=0;
        return send(new DesktopPackets.DesktopRecipeHoverPayload(source,recipeHoverEpoch,DesktopPackets.RECIPE_HOVER_UPDATE,recipe),"recipe-hover-update");
    }
    public static void endRecipeHover(int source){
        if(recipeHoverSession!=source)return;
        long epoch=recipeHoverEpoch;recipeHoverSession=-1;recipeHoverId=null;recipeHoverHeartbeat=0;RECIPE_HOVER_STATES.remove(source);
        send(new DesktopPackets.DesktopRecipeHoverPayload(source,epoch,DesktopPackets.RECIPE_HOVER_END,null),"recipe-hover-end");
    }
    public static List<com.salts_inventory_update.api.client.desktop.DesktopRecipeGhost> recipeHoverGhosts(int source){
        var state=RECIPE_HOVER_STATES.get(source);
        if(state==null||!state.active()||state.epoch()!=recipeHoverEpoch||!java.util.Objects.equals(state.recipeId(),recipeHoverId))return List.of();
        return state.ghosts().stream().map(ghost->new com.salts_inventory_update.api.client.desktop.DesktopRecipeGhost(ghost.slotIndex(),ghost.item())).toList();
    }
    public static boolean recipeHoverMatches(int source,ResourceLocation recipe){var state=RECIPE_HOVER_STATES.get(source);return state!=null&&state.active()&&state.epoch()==recipeHoverEpoch&&recipe!=null&&recipe.equals(state.recipeId());}
    public static boolean recipeHoverActive(int source){var state=RECIPE_HOVER_STATES.get(source);return state!=null&&state.active()&&state.epoch()==recipeHoverEpoch&&source==recipeHoverSession;}
    public static boolean recipeHoverRejected(int source){var state=RECIPE_HOVER_STATES.get(source);return state!=null&&!state.active()&&state.epoch()==recipeHoverEpoch&&source==recipeHoverSession;}

    public static boolean placeRecipe(int sessionId, ResourceLocation recipeId, boolean useMaxItems) {
        DesktopDebug.trace("client send recipe place session={} recipe={} useMax={}", sessionId, recipeId, useMaxItems);
        return send(new DesktopPlaceRecipePayload(sessionId, recipeId, useMaxItems), "recipe-place");
    }

    public static boolean transferJeiRecipe(int targetSessionId, ResourceLocation recipeId, boolean maxTransfer) {
        DesktopDebug.trace("client send JEI transfer targetSession={} recipe={} max={}", targetSessionId, recipeId, maxTransfer);
        return send(new DesktopJeiTransferPayload(targetSessionId, recipeId, maxTransfer), "jei-transfer");
    }

    public static boolean purchaseInventorySlot() {
        DesktopDebug.trace("client send inventory slot purchase");
        return send(new InventorySlotPurchasePayload(), "inventory-slot-purchase");
    }

    public static boolean renameAnvil(int sessionId, String name) {
        DesktopDebug.trace("client send rename session={} name={}", sessionId, name);
        return send(new DesktopRenamePayload(sessionId, name), "rename");
    }

    public static boolean sendCustomPayload(int sessionId, ResourceLocation channel, byte[] data) {
        DesktopDebug.trace("client send custom session={} channel={} bytes={}", sessionId, channel, data.length);
        return send(new DesktopCustomPayload(sessionId, channel, data), "custom");
    }

    public static boolean syncCarried(ItemStack carried) {
        DesktopDebug.trace("client send carried stack={}", carried);
        return send(new DesktopCarriedPayload(carried.copy()), "carried");
    }

    public static void closeSession(int sessionId) {
        DesktopDebug.log("client send close session={}", sessionId);
        send(new DesktopCloseSessionPayload(sessionId), "close");
    }

    public static void finishWorkspace() {
        DesktopDebug.log("client send close session={}", DesktopPackets.PLAYER_MENU_SESSION);
        send(new DesktopFinishWorkspacePayload(DesktopPackets.PLAYER_MENU_SESSION), "finish-workspace");
    }



    public static void setSessionVisible(int sessionId, boolean visible) {
        DesktopDebug.trace("client send visibility session={} visible={}", sessionId, visible);
        send(new DesktopSessionVisibilityPayload(sessionId, visible), "visibility");
    }

    public static void openLinkedSources(String originSourceKey, java.util.List<String> sourceKeys) {
        if (originSourceKey == null || originSourceKey.isBlank() || sourceKeys.isEmpty()) {
            return;
        }
        long originGrant = SOURCE_GRANTS.getOrDefault(originSourceKey, 0L);
        if (originGrant == 0L) {
            DesktopDebug.trace("client linked sources skipped requested={} reason=no-origin-grant", sourceKeys.size());
            return;
        }
        DesktopDebug.trace("client send linked source component requested={} origin={}", sourceKeys.size(), originSourceKey);
        send(new DesktopOpenLinkedSourcesPayload(originGrant), "open-linked-sources");
    }

    public static void setSourcesLinked(String firstSourceKey, String secondSourceKey, boolean linked) {
        long firstGrant = SOURCE_GRANTS.getOrDefault(firstSourceKey, 0L);
        long secondGrant = SOURCE_GRANTS.getOrDefault(secondSourceKey, 0L);
        int firstSession = SOURCE_SESSIONS.getOrDefault(firstSourceKey, NO_SESSION);
        int secondSession = SOURCE_SESSIONS.getOrDefault(secondSourceKey, NO_SESSION);
        if (firstGrant == 0L || secondGrant == 0L || firstGrant == secondGrant
            || firstSession == NO_SESSION || secondSession == NO_SESSION || firstSession == secondSession) {
            DesktopDebug.trace("client source link skipped linked={} reason=missing-grant", linked);
            return;
        }
        send(
            new DesktopSourceLinkPayload(firstSession, firstGrant, secondSession, secondGrant, linked),
            linked ? "source-link" : "source-unlink"
        );
    }



    public static void declineAutomaticWindow(int sessionId) {
        if (!automaticNativeReady) return;
        long token = SESSION_TOKENS.getOrDefault(sessionId, 0L);
        if (token != 0L) sendRaw(new com.salts_inventory_update.network.AutomaticNativeFallback(
            CONNECTION_STATE.connectionNonce(), sessionId, token), "automatic-native-fallback");
    }

    public static void refreshNativeTemplates() {
        if(!CONNECTION_STATE.isNegotiated()||!ClientPlayNetworking.canSend(com.salts_inventory_update.network.NativeTemplateCapabilities.TYPE))return;
        var ids=new java.util.LinkedHashSet<>(com.salts_inventory_update.api.client.desktop.DesktopClientApi.nativeTemplateMenuIds());
        ids.addAll(com.salts_inventory_update.client.automatic.NativeProfileFiles.menuIds());
        sendRaw(new com.salts_inventory_update.network.NativeTemplateCapabilities(CONNECTION_STATE.connectionNonce(),List.copyOf(ids)),"native-template-capabilities");
    }

    private static boolean send(DesktopPacket payload, String label) {
        if (!SaltsInventoryRuntime.isEnabled()) {
            DesktopDebug.trace("client desktop packet skipped label={} type={} reason=runtime-disabled", label, payload.id());
            return false;
        }

        AuthTokens tokens = authTokens(payload);
        if (tokens == null) {
            DesktopDebug.warn("client desktop packet skipped label={} type={} reason=no-auth-policy", label, payload.id());
            return false;
        }
        if (!CONNECTION_STATE.authorizes(
            CONNECTION_STATE.connectionNonce(),
            requiredCapability(payload, tokens),
            true
        )) {
            DesktopDebug.trace("client desktop packet skipped label={} type={} reason=unauthorized", label, payload.id());
            return false;
        }
        if ((tokens.primarySessionId() != NO_SESSION && (tokens.primaryToken() == 0L || tokens.primaryStateId() < 0))
            || (tokens.secondarySessionId() != NO_SESSION && (tokens.secondaryToken() == 0L || tokens.secondaryStateId() < 0))) {
            DesktopDebug.trace("client desktop packet skipped label={} type={} reason=missing-session-token", label, payload.id());
            return false;
        }

        com.salts_inventory_update.network.AutomaticCountCodec.Encoded nativeCounts = automaticNativeReady
            && payload instanceof DesktopClickPayload click && click.clientCarried().getCount() > 127
            ? com.salts_inventory_update.network.AutomaticCountCodec.encode(payload) : null;
        FriendlyByteBuf inner = nativeCounts == null ? DesktopPackets.toBuffer(payload)
            : new FriendlyByteBuf(io.netty.buffer.Unpooled.wrappedBuffer(nativeCounts.body()));
        try {
            byte[] data = new byte[inner.readableBytes()];
            inner.readBytes(data);
            DesktopAuthenticatedPayload authenticated = new DesktopAuthenticatedPayload(
                CONNECTION_STATE.connectionNonce(),
                tokens.primaryToken(),
                tokens.secondaryToken(),
                tokens.primaryStateId(),
                tokens.secondaryStateId(),
                payload.id(),
                data
            );
            if (nativeCounts != null) {
                FriendlyByteBuf encoded = DesktopPackets.toBuffer(authenticated);
                try {
                    byte[] body = new byte[encoded.readableBytes()]; encoded.readBytes(body);
                    return sendRaw(new com.salts_inventory_update.network.AutomaticNativeEnvelope(
                        CONNECTION_STATE.connectionNonce(), authenticated.id(), body, nativeCounts.counts()), label);
                } finally { encoded.release(); }
            }
            return sendRaw(authenticated, label);
        } finally {
            inner.release();
        }
    }

    private static boolean sendRaw(DesktopPacket payload, String label) {
        FriendlyByteBuf encoded = DesktopPackets.toBuffer(payload);
        try {
            ClientPlayNetworking.send(payload.id(), encoded);
            return true;
        } catch (IllegalStateException | IllegalArgumentException exception) {
            if (encoded.refCnt() > 0) {
                encoded.release();
            }
            DesktopDebug.warn("client desktop packet failed label={} type={} reason={}", label, payload.id(), exception.toString());
            return false;
        }
    }

    private static final int NO_SESSION = Integer.MIN_VALUE;

    private static boolean hasCapability(long capability, boolean requireUi) {
        return CONNECTION_STATE.authorizes(CONNECTION_STATE.connectionNonce(), capability, requireUi);
    }

    private static long requiredCapability(DesktopPacket payload, AuthTokens tokens) {
        if (payload instanceof DesktopPackets.DesktopClickSequencePayload) return DesktopPackets.CAP_CLICK_SEQUENCES;
        if (payload instanceof DesktopPackets.DesktopSortInventoryPayload) return DesktopPackets.CAP_SORT_WINDOWS | DesktopPackets.CAP_SORT_INVENTORY;
        long required = (payload instanceof DesktopSortWindowsPayload || payload instanceof DesktopPackets.DesktopSortInventoryPayload) ? DesktopPackets.CAP_SORT_WINDOWS : 0L;
        if (payload instanceof DesktopPackets.DesktopCraftToWindowPayload || payload instanceof DesktopPackets.DesktopCookingRecipePayload || payload instanceof DesktopPackets.DesktopRecipeHoverPayload || payload instanceof DesktopPlaceRecipePayload || payload instanceof DesktopJeiTransferPayload) {
            required |= DesktopProtocol.CAP_RECIPE_TRANSFER;
        }
        if (payload instanceof DesktopSessionVisibilityPayload
            || payload instanceof DesktopSourceLinkPayload
            || payload instanceof DesktopOpenLinkedSourcesPayload) {
            required |= DesktopProtocol.CAP_LINK_GRAPH;
        }
        if (payload instanceof DesktopCustomPayload) {
            required |= DesktopProtocol.CAP_CUSTOM_WINDOWS;
        }
        if (payload instanceof DesktopDragSlotsPayload || payload instanceof DesktopPickupAllPayload || payload instanceof DesktopQuickMoveAllPayload) {
            required |= DesktopPackets.CAP_MULTI_MENU_GESTURES;
        }
        if (payload instanceof DesktopOpenLinkedSourcesPayload) {
            required |= DesktopProtocol.CAP_CUSTOM_WINDOWS;
        }
        if (tokens.primarySessionId() != NO_SESSION) {
            required |= capabilityForSession(tokens.primarySessionId());
        }
        if (tokens.secondarySessionId() != NO_SESSION) {
            required |= capabilityForSession(tokens.secondarySessionId());
        }
        return required;
    }

    private static long capabilityForSession(int sessionId) {
        return sessionId == DesktopPackets.PLAYER_MENU_SESSION
            ? DesktopProtocol.CAP_INVENTORY_TOPOLOGY
            : DesktopProtocol.CAP_CUSTOM_WINDOWS;
    }

    private static AuthTokens authTokens(DesktopPacket payload) {
        int primary = NO_SESSION;
        int secondary = NO_SESSION;
        if (payload instanceof DesktopPackets.DesktopCraftToWindowPayload value) {
            primary=value.sourceSessionId();secondary=value.targetSessionId();
        } else if(payload instanceof DesktopPackets.DesktopCookingRecipePayload value){primary=value.sessionId();
        } else if(payload instanceof DesktopPackets.DesktopRecipeHoverPayload value){primary=value.sessionId();
        } else if (payload instanceof DesktopClickPayload value) {
            primary = value.sessionId();
        } else if (payload instanceof DesktopQuickMovePayload value) {
            primary = value.sourceSessionId();
            secondary = value.targetKind() == DesktopPackets.QUICK_TARGET_SESSION
                ? value.targetSessionId()
                : DesktopPackets.PLAYER_MENU_SESSION;
        } else if (payload instanceof DesktopButtonPayload value) {
            primary = value.sessionId();
        } else if (payload instanceof DesktopPlaceRecipePayload value) {
            primary = value.sessionId();
        } else if (payload instanceof DesktopJeiTransferPayload value) {
            primary = value.targetSessionId();
        } else if (payload instanceof DesktopRenamePayload value) {
            primary = value.sessionId();
        } else if (payload instanceof DesktopCustomPayload value) {
            primary = value.sessionId();
        } else if (payload instanceof DesktopCloseSessionPayload value) {
            primary = value.sessionId();
        } else if (payload instanceof DesktopFinishWorkspacePayload value) {
            primary = value.sessionId();
        } else if (payload instanceof DesktopSessionVisibilityPayload value) {
            primary = value.sessionId();
        } else if (payload instanceof DesktopSourceLinkPayload value) {
            primary = value.firstSessionId();
            secondary = value.secondSessionId();
        } else if (payload instanceof DesktopCarriedPayload || payload instanceof InventorySlotPurchasePayload) {
            primary = DesktopPackets.PLAYER_MENU_SESSION;
        } else if (payload instanceof DesktopPackets.DesktopClickSequencePayload || payload instanceof DesktopDragSlotsPayload || payload instanceof DesktopPickupAllPayload || payload instanceof DesktopQuickMoveAllPayload || payload instanceof DesktopSortWindowsPayload || payload instanceof DesktopPackets.DesktopSortInventoryPayload) {
            // Every referenced menu carries its own token/state pair and is validated atomically by the server.
        } else if (!(payload instanceof DesktopOpenLinkedSourcesPayload)
        ) {
            return null;
        }
        return new AuthTokens(
            primary,
            tokenFor(primary),
            stateIdFor(primary),
            secondary,
            tokenFor(secondary),
            stateIdFor(secondary)
        );
    }

    private static long tokenFor(int sessionId) {
        if (sessionId == NO_SESSION) {
            return 0L;
        }
        if (sessionId == DesktopPackets.PLAYER_MENU_SESSION) {
            return playerMenuToken;
        }
        return SESSION_TOKENS.getOrDefault(sessionId, 0L);
    }

    private static int stateIdFor(int sessionId) {
        if (sessionId == NO_SESSION) {
            return -1;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (sessionId == DesktopPackets.PLAYER_MENU_SESSION) {
            return minecraft.player == null ? -1 : minecraft.player.inventoryMenu.getStateId();
        }
        return SESSION_STATE_IDS.getOrDefault(sessionId, -1);
    }

    private record AuthTokens(
        int primarySessionId,
        long primaryToken,
        int primaryStateId,
        int secondarySessionId,
        long secondaryToken,
        int secondaryStateId
    ) {
    }

    /** An immutable native opening identity; stack values and revision IDs may change. */
    private record SessionSnapshotIdentity(AbstractContainerMenu menu, int sessionId, long sessionToken,
        int menuTypeId, int specialKind, int entityId, int columns, String sourceKey, Component title,
        boolean visible, boolean transferSupported, int dataCount, byte[] openingData, List<SnapshotSlot> slots) {
        static SessionSnapshotIdentity capture(AbstractContainerMenu menu, DesktopOpenSessionPayload payload,
                                              long sessionToken, byte[] openingData) {
            List<SnapshotSlot> slots = new ArrayList<>();
            for (int i = 0; i < DesktopMenuBindings.size(menu); i++) slots.add(SnapshotSlot.capture(menu, DesktopMenuBindings.slot(menu, i)));
            return new SessionSnapshotIdentity(menu, payload.sessionId(), sessionToken, payload.menuTypeId(),
                payload.specialKind(), payload.entityId(), payload.columns(), payload.sourceKey(), payload.title().copy(),
                payload.visible(), payload.transferSupported(), payload.data().length, openingData.clone(), List.copyOf(slots));
        }
        boolean matches(AbstractContainerMenu currentMenu, DesktopOpenSessionPayload payload, long authorizedToken,
                        byte[] currentOpeningData, int replacesSessionId) {
            if (sessionToken == 0 || menu != currentMenu || sessionId != payload.sessionId() || sessionToken != authorizedToken
                || menuTypeId != payload.menuTypeId() || specialKind != payload.specialKind() || entityId != payload.entityId()
                || columns != payload.columns() || !sourceKey.equals(payload.sourceKey()) || !title.equals(payload.title())
                || visible != payload.visible() || transferSupported != payload.transferSupported() || dataCount != payload.data().length
                || replacesSessionId > DesktopPackets.PLAYER_MENU_SESSION || !java.util.Arrays.equals(openingData, currentOpeningData)
                || slots.size() != payload.items().size() || slots.size() != DesktopMenuBindings.size(menu)) return false;
            for (int i = 0; i < slots.size(); i++) if (!slots.get(i).matches(menu, DesktopMenuBindings.slot(menu, i))) return false;
            return true;
        }
        void apply(DesktopOpenSessionPayload payload) {
            menu.initializeContents(payload.stateId(), payload.items(), payload.carried());
            int[] data = payload.data();
            for (int i = 0; i < data.length; i++) {
                try { menu.setData(i, data[i]); } catch (IndexOutOfBoundsException ignored) { break; }
            }
        }
    }
    private record SnapshotSlot(@Nullable Slot slot, @Nullable Object owner, int physicalIndex, int menuIndex, int x, int y) {
        static SnapshotSlot capture(AbstractContainerMenu menu, @Nullable Slot slot) {
            return slot == null ? new SnapshotSlot(null, null, -1, -1, 0, 0)
                : new SnapshotSlot(slot, DesktopMenuBindings.physicalOwner(menu, slot), DesktopMenuBindings.physicalIndex(menu, slot), slot.index, slot.x, slot.y);
        }
        boolean matches(AbstractContainerMenu menu, @Nullable Slot current) {
            return slot == current && (current == null || owner == DesktopMenuBindings.physicalOwner(menu, current)
                && physicalIndex == DesktopMenuBindings.physicalIndex(menu, current) && menuIndex == current.index && x == current.x && y == current.y);
        }
    }

    private record MenuOpenData(long sessionToken, int menuTypeId, int replacesSessionId, byte[] data, int[] companionOwnerIds) {
        private MenuOpenData {
            data = data.clone();
            companionOwnerIds = companionOwnerIds.clone();
        }

        @Override
        public byte[] data() {
            return data.clone();
        }
    }
}
