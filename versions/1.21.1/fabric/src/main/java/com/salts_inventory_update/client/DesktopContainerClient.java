package com.salts_inventory_update.client;

import com.salts_inventory_update.platform.fabric.api.client.networking.v1.ClientPlayNetworking;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.security.SecureRandom;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;

import com.salts_inventory_update.SaltsInventoryRuntime;
import com.salts_inventory_update.debug.DesktopDebug;
import com.salts_inventory_update.inventory.InventoryExpansion;
import com.salts_inventory_update.network.DesktopPackets;
import com.salts_inventory_update.network.DesktopPackets.DesktopButtonPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopCarriedPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopClickPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopCloseSessionPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopCustomPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopDataPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopGhostRecipePayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopHelloAckPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopHelloPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopJeiTransferPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopLinkSessionsPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopMerchantOffersPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopModePayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopOpenLinkedSourcesPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopOpenSessionPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopPlaceRecipePayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopQuickMovePayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopReadyPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopRenamePayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopSessionClosedPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopSessionPinPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopSessionVisibilityPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopSlotPayload;
import com.salts_inventory_update.network.DesktopPackets.InventoryExpansionSyncPayload;
import com.salts_inventory_update.network.DesktopPackets.InventorySlotPurchasePayload;
import com.salts_inventory_update.network.DesktopPackets.MutationStamp;
import com.salts_inventory_update.protocol.DesktopConnectionState;
import com.salts_inventory_update.protocol.DesktopProtocol;

public final class DesktopContainerClient {
    private static final int MODE_RESEND_INTERVAL_TICKS = 100;
    private static final DesktopConnectionState CONNECTION = new DesktopConnectionState();
    private static final SecureRandom NONCE_RANDOM = new SecureRandom();
    private static boolean helloSent;
    private static boolean lastRequestedUiEnabled;
    private static List<String> lastRequestedForcedMenuIds = List.of();
    private static long clientTicks;
    private static long modeSequence;
    private static long lastModeSendTick;
    private static long playerMenuNonce;
    private static Object lastConnection;
    private static final Map<Integer, SessionStamp> SESSION_STAMPS = new HashMap<>();
    private static boolean incompatibilityNotified;
    private static boolean availabilityLogged;
    private static boolean lastHadPlayer;
    private static boolean lastDesktopAvailable;
    private static boolean lastRuntimeEnabled;
    private static boolean lastRemoteServer;

    private DesktopContainerClient() {
    }

    public static void initializeNetworking() {
        ClientPlayNetworking.registerGlobalReceiver(DesktopHelloAckPayload.TYPE, (payload, context) -> {
            Minecraft client = context.client();
            client.execute(() -> {
                boolean accepted = CONNECTION.acknowledge(
                    payload.protocolVersion(),
                    payload.echoedClientNonce(),
                    payload.connectionNonce(),
                    payload.capabilities(),
                    payload.uiEnabled()
                );
                SaltsInventoryRuntime.setServerDesktopAvailable(accepted);
                SaltsInventoryRuntime.setServerDesktopCapabilities(accepted ? payload.capabilities() : 0L);
                DesktopDebug.log("client desktop hello ack accepted={} protocol={} capabilities={}", accepted, payload.protocolVersion(), payload.capabilities());
            });
        });
        ClientPlayNetworking.registerGlobalReceiver(DesktopOpenSessionPayload.TYPE, (payload, context) -> {
            Minecraft client = context.client();
            client.execute(() -> {
                if (!CONNECTION.authorizes(payload.connectionNonce(), DesktopProtocol.CAP_CUSTOM_WINDOWS, true)) {
                    return;
                }
                if (!SESSION_STAMPS.containsKey(payload.sessionId())
                    && SESSION_STAMPS.size() >= DesktopProtocol.MAX_DESKTOP_SESSIONS) {
                    DesktopDebug.warn("client payload open rejected session={} reason=session-cap", payload.sessionId());
                    return;
                }
                SESSION_STAMPS.put(payload.sessionId(), new SessionStamp(payload.sessionNonce(), payload.stateId()));
                if (!SaltsInventoryRuntime.isEnabled()) {
                    DesktopDebug.log(
                        "client payload open ignored session={} title={} reason=runtime-disabled configured={} desktopAvailable={} screen={}",
                        payload.sessionId(),
                        payload.title().getString(),
                        SaltsInventoryRuntime.isConfiguredEnabled(),
                        SaltsInventoryRuntime.isServerDesktopAvailable(),
                        currentScreenName(client)
                    );
                    return;
                }
                DesktopDebug.log(
                    "client payload open session={} title={} type={} special={} visible={} state={} items={} data={} carried={} screen={} playerMenu={}",
                    payload.sessionId(),
                    payload.title().getString(),
                    payload.menuTypeId(),
                    payload.specialKind(),
                    payload.visible(),
                    payload.stateId(),
                    payload.items().size(),
                    payload.data().length,
                    payload.carried(),
                    currentScreenName(client),
                    client.player == null ? -1 : client.player.inventoryMenu.containerId
                );
                InventoryDesktopScreen.openOrAddSession(client, DesktopContainerSession.create(client, payload), payload.visible());
                InventoryDesktopScreen screen = InventoryDesktopScreen.current(client);
                DesktopDebug.log(
                    "client payload open applied session={} currentScreen={} desktopScreen={}",
                    payload.sessionId(),
                    currentScreenName(client),
                    screen != null
                );
            });
        });
        ClientPlayNetworking.registerGlobalReceiver(DesktopSlotPayload.TYPE, (payload, context) -> {
            Minecraft client = context.client();
            client.execute(() -> {
                if (!acceptsServerStamp(payload.sessionId(), payload.authorization())) {
                    return;
                }
                InventoryDesktopScreen screen = InventoryDesktopScreen.current(client);
                if (screen != null) {
                    screen.updateSessionSlot(payload.sessionId(), payload.slotIndex(), payload.stateId(), payload.stack());
                }
                SESSION_STAMPS.computeIfPresent(payload.sessionId(), (ignored, stamp) -> new SessionStamp(stamp.sessionNonce(), payload.stateId()));
            });
        });
        ClientPlayNetworking.registerGlobalReceiver(DesktopDataPayload.TYPE, (payload, context) -> {
            Minecraft client = context.client();
            client.execute(() -> {
                if (!acceptsServerStamp(payload.sessionId(), payload.authorization())) {
                    return;
                }
                InventoryDesktopScreen screen = InventoryDesktopScreen.current(client);
                if (screen != null) {
                    screen.updateSessionData(payload.sessionId(), payload.dataSlot(), payload.value());
                }
            });
        });
        ClientPlayNetworking.registerGlobalReceiver(DesktopCarriedPayload.TYPE, (payload, context) -> {
            Minecraft client = context.client();
            client.execute(() -> {
                if (!acceptsServerStamp(DesktopPackets.PLAYER_MENU_SESSION, payload.authorization())) {
                    return;
                }
                DesktopDebug.trace("client payload carried stack={}", payload.carried());
                InventoryDesktopScreen screen = InventoryDesktopScreen.current(client);
                if (screen != null) {
                    screen.setSharedCarried(payload.carried());
                } else if (client.player != null) {
                    client.player.inventoryMenu.setCarried(payload.carried().copy());
                }
            });
        });
        ClientPlayNetworking.registerGlobalReceiver(DesktopSessionClosedPayload.TYPE, (payload, context) -> {
            Minecraft client = context.client();
            client.execute(() -> {
                if (!acceptsServerStamp(payload.sessionId(), payload.authorization())) {
                    return;
                }
                InventoryDesktopScreen screen = InventoryDesktopScreen.current(client);
                if (screen != null) {
                    screen.removeSession(payload.sessionId());
                }
                SESSION_STAMPS.remove(payload.sessionId());
            });
        });
        ClientPlayNetworking.registerGlobalReceiver(DesktopSessionVisibilityPayload.TYPE, (payload, context) -> {
            Minecraft client = context.client();
            client.execute(() -> {
                if (!acceptsServerStamp(payload.sessionId(), payload.authorization())) {
                    return;
                }
                InventoryDesktopScreen screen = InventoryDesktopScreen.current(client);
                if (screen != null) {
                    screen.setSessionVisible(payload.sessionId(), payload.visible());
                }
            });
        });
        ClientPlayNetworking.registerGlobalReceiver(DesktopMerchantOffersPayload.TYPE, (payload, context) -> {
            Minecraft client = context.client();
            client.execute(() -> {
                if (!acceptsServerStamp(payload.sessionId(), payload.authorization())) {
                    return;
                }
                InventoryDesktopScreen screen = InventoryDesktopScreen.current(client);
                if (screen != null) {
                    screen.applyMerchantOffers(payload);
                }
            });
        });
        ClientPlayNetworking.registerGlobalReceiver(DesktopCustomPayload.TYPE, (payload, context) -> {
            Minecraft client = context.client();
            client.execute(() -> {
                if (!acceptsServerStamp(payload.sessionId(), payload.authorization())) {
                    return;
                }
                InventoryDesktopScreen screen = InventoryDesktopScreen.current(client);
                if (screen != null) {
                    screen.applyCustomPayload(payload);
                }
            });
        });
        ClientPlayNetworking.registerGlobalReceiver(DesktopGhostRecipePayload.TYPE, (payload, context) -> {
            Minecraft client = context.client();
            client.execute(() -> {
                if (!acceptsServerStamp(payload.sessionId(), payload.authorization())) {
                    return;
                }
                InventoryDesktopScreen screen = InventoryDesktopScreen.current(client);
                if (screen != null) {
                    screen.applyGhostRecipe(payload);
                }
            });
        });
        ClientPlayNetworking.registerGlobalReceiver(InventoryExpansionSyncPayload.TYPE, (payload, context) -> {
            Minecraft client = context.client();
            client.execute(() -> {
                if (!CONNECTION.authorizes(payload.connectionNonce(), DesktopProtocol.CAP_INVENTORY_TOPOLOGY, false)
                    || payload.playerMenuNonce() == 0L) {
                    return;
                }
                playerMenuNonce = payload.playerMenuNonce();
                SaltsInventoryRuntime.setServerDesktopAvailable(true);
                DesktopDebug.trace("client payload inventory expansion slots={} stacks={}", payload.slotCount(), payload.items().size());
                if (client.player != null) {
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
        });
    }

    public static void tick(Minecraft minecraft) {
        clientTicks++;
        Object connection = minecraft.getConnection();
        if (connection != lastConnection) {
            resetConnectionState();
            lastConnection = connection;
        }
        if (minecraft.player == null || minecraft.level == null) {
            logAvailability(minecraft, false, false, false);
            resetConnectionState();
            SaltsInventoryRuntime.setServerDesktopAvailable(true);
            return;
        }

        boolean remoteServer = minecraft.getCurrentServer() != null && minecraft.getSingleplayerServer() == null;
        boolean helloChannelPresent = canSendRaw(DesktopHelloPayload.TYPE);
        boolean legacyChannelPresent = canSendRaw(DesktopReadyPayload.TYPE);
        if (!helloSent) {
            if (helloChannelPresent) {
                long clientNonce = nonzeroNonce();
                CONNECTION.begin(clientNonce, clientTicks);
                helloSent = sendRaw(new DesktopHelloPayload(
                    DesktopProtocol.VERSION,
                    clientNonce,
                    DesktopProtocol.KNOWN_CAPABILITIES,
                    SaltsInventoryRuntime.isConfiguredEnabled(),
                    SaltsInventoryConfig.forcedContainerWindowIds()
                ), "hello");
                lastRequestedUiEnabled = SaltsInventoryRuntime.isConfiguredEnabled();
                lastRequestedForcedMenuIds = SaltsInventoryConfig.forcedContainerWindowIds();
            } else if (legacyChannelPresent) {
                CONNECTION.markIncompatible();
            }
        }
        CONNECTION.expireIfNecessary(clientTicks, helloChannelPresent || legacyChannelPresent);
        if (CONNECTION.phase() == DesktopConnectionState.Phase.INCOMPATIBLE
            && (helloChannelPresent || legacyChannelPresent)
            && !incompatibilityNotified) {
            minecraft.player.displayClientMessage(
                Component.literal("Salt's Inventory Update protocol mismatch. Update the mod on both client and server (protocol 2 required)."),
                false
            );
            incompatibilityNotified = true;
        }
        boolean desktopAvailable = CONNECTION.isNegotiated();
        SaltsInventoryRuntime.setServerDesktopAvailable(desktopAvailable);
        SaltsInventoryRuntime.setServerDesktopCapabilities(desktopAvailable ? CONNECTION.capabilities() : 0L);
        logAvailability(minecraft, true, remoteServer, desktopAvailable);
        boolean requestedUiEnabled = SaltsInventoryRuntime.isConfiguredEnabled();
        List<String> requestedForcedMenuIds = SaltsInventoryConfig.forcedContainerWindowIds();
        if (CONNECTION.isNegotiated()
            && (requestedUiEnabled != lastRequestedUiEnabled
                || !requestedForcedMenuIds.equals(lastRequestedForcedMenuIds)
                || clientTicks - lastModeSendTick >= MODE_RESEND_INTERVAL_TICKS)) {
            long sequence = ++modeSequence;
            if (sendRaw(new DesktopModePayload(CONNECTION.connectionNonce(), sequence, requestedUiEnabled, requestedForcedMenuIds), "mode")) {
                CONNECTION.updateMode(CONNECTION.connectionNonce(), sequence, requestedUiEnabled);
                lastRequestedUiEnabled = requestedUiEnabled;
                lastRequestedForcedMenuIds = requestedForcedMenuIds;
                lastModeSendTick = clientTicks;
                if (!requestedUiEnabled) {
                    SESSION_STAMPS.clear();
                }
            }
        }
    }

    public static boolean canSendDesktopPackets() {
        if (!SaltsInventoryRuntime.isEnabled()) {
            return false;
        }

        try {
            return CONNECTION.authorizes(CONNECTION.connectionNonce(), 0L, true)
                && ClientPlayNetworking.canSend(DesktopClickPayload.TYPE);
        } catch (IllegalStateException | IllegalArgumentException ignored) {
            return false;
        }
    }

    public static boolean canUseServerSessions() {
        if (!SaltsInventoryRuntime.isEnabled()) {
            return false;
        }

        return CONNECTION.authorizes(CONNECTION.connectionNonce(), DesktopProtocol.CAP_CUSTOM_WINDOWS, true)
            && canUseServerSessionsRaw();
    }

    private static boolean canUseServerSessionsRaw() {
        try {
            return ClientPlayNetworking.canSend(DesktopHelloPayload.TYPE)
                && ClientPlayNetworking.canSend(DesktopModePayload.TYPE)
                && ClientPlayNetworking.canSend(DesktopClickPayload.TYPE)
                && ClientPlayNetworking.canSend(DesktopQuickMovePayload.TYPE)
                && ClientPlayNetworking.canSend(DesktopButtonPayload.TYPE)
                && ClientPlayNetworking.canSend(DesktopRenamePayload.TYPE)
                && ClientPlayNetworking.canSend(DesktopCloseSessionPayload.TYPE)
                && ClientPlayNetworking.canSend(DesktopSessionPinPayload.TYPE)
                && ClientPlayNetworking.canSend(DesktopSessionVisibilityPayload.TYPE)
                && ClientPlayNetworking.canSend(DesktopCustomPayload.TYPE)
                && ClientPlayNetworking.canSend(DesktopCarriedPayload.TYPE);
        } catch (IllegalStateException | IllegalArgumentException ignored) {
            return false;
        }
    }

    public static boolean clickSlot(int debugId, int sessionId, int slotIndex, int button, ClickType input, ItemStack clientCarried) {
        DesktopDebug.trace("client send click id={} session={} slot={} button={} input={} clientCarried={}", debugId, sessionId, slotIndex, button, input, clientCarried);
        return send(new DesktopClickPayload(mutationStamp(sessionId), debugId, sessionId, slotIndex, button, input.name(), clientCarried.copy()), "click", capabilityForSession(sessionId));
    }

    public static boolean quickMoveSlot(int sourceSessionId, int sourceSlotIndex, int targetKind, int targetSessionId) {
        DesktopDebug.trace(
            "client send quick move sourceSession={} sourceSlot={} targetKind={} targetSession={}",
            sourceSessionId,
            sourceSlotIndex,
            targetKind,
            targetSessionId
        );
        int authorizedTarget = targetKind == DesktopPackets.QUICK_TARGET_SESSION ? targetSessionId : DesktopPackets.PLAYER_MENU_SESSION;
        return send(
            new DesktopQuickMovePayload(mutationStamp(sourceSessionId), mutationStamp(authorizedTarget), sourceSessionId, sourceSlotIndex, targetKind, targetSessionId),
            "quick-move",
            capabilityForSession(sourceSessionId) | capabilityForSession(authorizedTarget)
        );
    }

    public static boolean clickButton(int sessionId, int buttonId) {
        DesktopDebug.trace("client send button session={} button={}", sessionId, buttonId);
        return send(new DesktopButtonPayload(mutationStamp(sessionId), sessionId, buttonId), "button", DesktopProtocol.CAP_CUSTOM_WINDOWS);
    }

    public static boolean placeRecipe(int sessionId, ResourceLocation recipeId, boolean useMaxItems) {
        DesktopDebug.trace("client send recipe place session={} recipe={} useMax={}", sessionId, recipeId, useMaxItems);
        return send(new DesktopPlaceRecipePayload(mutationStamp(sessionId), sessionId, recipeId, useMaxItems), "recipe-place", capabilityForSession(sessionId) | DesktopProtocol.CAP_RECIPE_TRANSFER);
    }

    public static boolean transferJeiRecipe(int targetSessionId, ResourceLocation recipeId, boolean maxTransfer) {
        DesktopDebug.trace("client send recipe transfer targetSession={} recipe={} max={}", targetSessionId, recipeId, maxTransfer);
        return send(new DesktopJeiTransferPayload(mutationStamp(targetSessionId), targetSessionId, recipeId, maxTransfer), "recipe-transfer", capabilityForSession(targetSessionId) | DesktopProtocol.CAP_RECIPE_TRANSFER);
    }

    public static boolean purchaseInventorySlot() {
        DesktopDebug.trace("client send inventory slot purchase");
        return send(new InventorySlotPurchasePayload(mutationStamp(DesktopPackets.PLAYER_MENU_SESSION)), "inventory-slot-purchase", DesktopProtocol.CAP_INVENTORY_TOPOLOGY);
    }

    public static boolean renameAnvil(int sessionId, String name) {
        DesktopDebug.trace("client send rename session={} name={}", sessionId, name);
        return send(new DesktopRenamePayload(mutationStamp(sessionId), sessionId, name), "rename", DesktopProtocol.CAP_CUSTOM_WINDOWS);
    }

    public static boolean sendCustomPayload(int sessionId, ResourceLocation channel, byte[] data) {
        DesktopDebug.trace("client send custom session={} channel={} bytes={}", sessionId, channel, data.length);
        return send(new DesktopCustomPayload(mutationStamp(sessionId), sessionId, channel, data), "custom", DesktopProtocol.CAP_CUSTOM_WINDOWS);
    }

    public static boolean syncCarried(ItemStack carried) {
        DesktopDebug.trace("client send carried stack={}", carried);
        return send(new DesktopCarriedPayload(mutationStamp(DesktopPackets.PLAYER_MENU_SESSION), carried.copy()), "carried");
    }

    public static void closeSession(int sessionId) {
        DesktopDebug.log("client send close session={}", sessionId);
        send(new DesktopCloseSessionPayload(mutationStamp(sessionId), sessionId), "close", DesktopProtocol.CAP_CUSTOM_WINDOWS);
    }

    public static void setSessionPinMode(int sessionId, PinMode pinMode) {
        DesktopDebug.trace("client send pin session={} pin={}", sessionId, pinMode);
        send(new DesktopSessionPinPayload(mutationStamp(sessionId), sessionId, pinModeToPacket(pinMode)), "pin", DesktopProtocol.CAP_CUSTOM_WINDOWS);
    }

    public static void setSessionVisible(int sessionId, boolean visible) {
        DesktopDebug.trace("client send visibility session={} visible={}", sessionId, visible);
        send(new DesktopSessionVisibilityPayload(mutationStamp(sessionId), sessionId, visible), "visibility", DesktopProtocol.CAP_CUSTOM_WINDOWS);
    }

    public static void setSessionsLinked(int originSessionId, int targetSessionId, boolean linked) {
        int action = linked ? DesktopPackets.LINK_ACTION_LINK : DesktopPackets.LINK_ACTION_UNLINK;
        send(new DesktopLinkSessionsPayload(mutationStamp(originSessionId), mutationStamp(targetSessionId), action), "link-sessions", DesktopProtocol.CAP_CUSTOM_WINDOWS | DesktopProtocol.CAP_LINK_GRAPH);
    }

    public static void clearSessionLinks(int originSessionId) {
        send(new DesktopLinkSessionsPayload(
            mutationStamp(originSessionId),
            new MutationStamp(CONNECTION.connectionNonce(), 0L, -1),
            DesktopPackets.LINK_ACTION_CLEAR_ORIGIN
        ), "clear-session-links", DesktopProtocol.CAP_CUSTOM_WINDOWS | DesktopProtocol.CAP_LINK_GRAPH);
    }

    public static void openLinkedSources(int originSessionId) {
        send(new DesktopOpenLinkedSourcesPayload(mutationStamp(originSessionId)), "open-linked-sources", DesktopProtocol.CAP_CUSTOM_WINDOWS | DesktopProtocol.CAP_LINK_GRAPH);
    }

    private static int pinModeToPacket(PinMode pinMode) {
        return switch (pinMode) {
            case UNPINNED -> DesktopPackets.PIN_MODE_UNPINNED;
            case PINNED -> DesktopPackets.PIN_MODE_PINNED;
            case GHOST_PINNED -> DesktopPackets.PIN_MODE_GHOST_PINNED;
        };
    }

    private static boolean send(net.minecraft.network.protocol.common.custom.CustomPacketPayload payload, String label) {
        return send(payload, label, 0L);
    }

    private static boolean send(net.minecraft.network.protocol.common.custom.CustomPacketPayload payload, String label, long requiredCapabilities) {
        if (!SaltsInventoryRuntime.isEnabled()) {
            DesktopDebug.trace("client desktop packet skipped label={} type={} reason=runtime-disabled", label, payload.type().id());
            return false;
        }
        if (!CONNECTION.authorizes(CONNECTION.connectionNonce(), requiredCapabilities, true)) {
            DesktopDebug.trace("client desktop packet skipped label={} type={} reason=unauthorized", label, payload.type().id());
            return false;
        }
        return sendRaw(payload, label);
    }

    private static long capabilityForSession(int sessionId) {
        return sessionId == DesktopPackets.PLAYER_MENU_SESSION
            ? DesktopProtocol.CAP_INVENTORY_TOPOLOGY
            : DesktopProtocol.CAP_CUSTOM_WINDOWS;
    }

    private static boolean sendRaw(net.minecraft.network.protocol.common.custom.CustomPacketPayload payload, String label) {
        try {
            ClientPlayNetworking.send(payload);
            return true;
        } catch (IllegalStateException | IllegalArgumentException exception) {
            DesktopDebug.warn("client desktop packet failed label={} type={} reason={}", label, payload.type().id(), exception.toString());
            return false;
        }
    }

    private static boolean canSendRaw(net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<?> type) {
        try {
            return ClientPlayNetworking.canSend(type);
        } catch (IllegalStateException | IllegalArgumentException ignored) {
            return false;
        }
    }

    private static long nonzeroNonce() {
        long nonce;
        do {
            nonce = NONCE_RANDOM.nextLong();
        } while (nonce == 0L);
        return nonce;
    }

    private static void resetConnectionState() {
        CONNECTION.reset();
        SaltsInventoryRuntime.setServerDesktopCapabilities(0L);
        helloSent = false;
        modeSequence = 0L;
        lastModeSendTick = 0L;
        playerMenuNonce = 0L;
        lastRequestedUiEnabled = SaltsInventoryRuntime.isConfiguredEnabled();
        lastRequestedForcedMenuIds = List.of();
        SESSION_STAMPS.clear();
        incompatibilityNotified = false;
    }

    private static MutationStamp mutationStamp(int sessionId) {
        Minecraft minecraft = Minecraft.getInstance();
        if (sessionId == DesktopPackets.PLAYER_MENU_SESSION) {
            int stateId = minecraft.player == null ? -1 : minecraft.player.inventoryMenu.getStateId();
            return new MutationStamp(CONNECTION.connectionNonce(), playerMenuNonce, stateId);
        }
        SessionStamp stamp = SESSION_STAMPS.get(sessionId);
        return stamp == null
            ? new MutationStamp(CONNECTION.connectionNonce(), 0L, -1)
            : new MutationStamp(CONNECTION.connectionNonce(), stamp.sessionNonce(), stamp.stateId());
    }

    private static boolean acceptsServerStamp(int sessionId, MutationStamp stamp) {
        if (!CONNECTION.authorizes(stamp.connectionNonce(), capabilityForSession(sessionId), false)) {
            return false;
        }
        if (sessionId == DesktopPackets.PLAYER_MENU_SESSION) {
            return playerMenuNonce != 0L && stamp.sessionNonce() == playerMenuNonce;
        }
        SessionStamp known = SESSION_STAMPS.get(sessionId);
        return known != null && stamp.sessionNonce() == known.sessionNonce();
    }

    private record SessionStamp(long sessionNonce, int stateId) {
    }

    private static void logAvailability(Minecraft minecraft, boolean hasPlayer, boolean remoteServer, boolean desktopAvailable) {
        boolean runtimeEnabled = SaltsInventoryRuntime.isEnabled();
        if (!availabilityLogged
            || lastHadPlayer != hasPlayer
            || lastRemoteServer != remoteServer
            || lastDesktopAvailable != desktopAvailable
            || lastRuntimeEnabled != runtimeEnabled) {
            DesktopDebug.log(
                "client desktop availability player={} level={} remoteServer={} desktopAvailable={} configured={} runtimeEnabled={} readySent={} screen={}",
                minecraft.player != null,
                minecraft.level != null,
                remoteServer,
                desktopAvailable,
                SaltsInventoryRuntime.isConfiguredEnabled(),
                runtimeEnabled,
                helloSent,
                currentScreenName(minecraft)
            );
            availabilityLogged = true;
            lastHadPlayer = hasPlayer;
            lastRemoteServer = remoteServer;
            lastDesktopAvailable = desktopAvailable;
            lastRuntimeEnabled = runtimeEnabled;
        }
    }

    private static String currentScreenName(Minecraft minecraft) {
        return minecraft.screen == null ? "none" : minecraft.screen.getClass().getName();
    }
}
