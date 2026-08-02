package com.salts_inventory_update.client;

import com.salts_inventory_update.platform.fabric.api.client.networking.v1.ClientPlayNetworking;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.display.RecipeDisplayId;
import org.jspecify.annotations.Nullable;

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
import com.salts_inventory_update.network.DesktopPackets.DesktopJeiTransferPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopMerchantOffersPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopOpenLinkedSourcesPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopOpenSessionPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopPlaceRecipePayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopQuickMovePayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopHelloPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopReadyPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopHelloAckPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopModePayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopPlayerSessionPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopLinkPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopRenamePayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopSessionClosedPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopSessionPinPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopSessionVisibilityPayload;
import com.salts_inventory_update.network.DesktopPackets.DesktopSlotPayload;
import com.salts_inventory_update.network.DesktopPackets.InventoryExpansionSyncPayload;
import com.salts_inventory_update.network.DesktopPackets.InventorySlotPurchasePayload;
import com.salts_inventory_update.protocol.DesktopConnectionState;
import com.salts_inventory_update.protocol.DesktopProtocol;

public final class DesktopContainerClient {
    private static final int MODE_RESEND_INTERVAL_TICKS = 100;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final DesktopConnectionState CONNECTION = new DesktopConnectionState();
    private static final Map<Integer, Long> SESSION_TOKENS = new HashMap<>();
    private static @Nullable Object connectionIdentity;
    private static boolean helloSent;
    private static boolean incompatibilityShown;
    private static boolean requestedUiEnabled;
    private static long playerSessionToken;
    private static long modeSequence;
    private static int modeResendTicks;
    private static java.util.List<String> requestedForcedMenuIds = java.util.List.of();

    private DesktopContainerClient() {
    }

    public static void initializeNetworking() {
        ClientPlayNetworking.registerGlobalReceiver(DesktopHelloAckPayload.TYPE, (payload, context) -> {
            if (payload.playerSessionToken() == 0L || !CONNECTION.acknowledge(
                payload.protocolVersion(),
                payload.clientNonce(),
                payload.connectionNonce(),
                payload.capabilities(),
                payload.uiEnabled()
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
            if (previousToken == null && SESSION_TOKENS.size() >= DesktopProtocol.MAX_DESKTOP_SESSIONS) {
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
            try {
                DesktopContainerSession session = DesktopContainerSession.create(context.client(), payload);
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
                if (diagnostic) {
                    mountDiag("client_payload_open_added session={} special={}", payload.sessionId(), payload.specialKind());
                }
            } catch (RuntimeException exception) {
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
        ClientPlayNetworking.registerGlobalReceiver(DesktopSessionClosedPayload.TYPE, (payload, context) -> {
            SESSION_TOKENS.remove(payload.sessionId());
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
            if (!validInboundSession(payload.connectionNonce(), payload.sessionId(), payload.sessionToken())) {
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
                        ? "Salt's Inventory Update 0.1.1 is incompatible. Update the mod on both client and server (protocol 2 required)."
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
                new DesktopHelloPayload(DesktopProtocol.VERSION, clientNonce, DesktopProtocol.KNOWN_CAPABILITIES, requestedUiEnabled, requestedForcedMenuIds),
                "hello"
            );
            if (helloSent) {
                DesktopDebug.log("client desktop hello sent protocol={} capabilities={} ui={}", DesktopProtocol.VERSION, DesktopProtocol.KNOWN_CAPABILITIES, requestedUiEnabled);
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
        if (!desiredUi && minecraft.gui.screen() instanceof InventoryDesktopScreen) {
            minecraft.gui.setScreen(null);
        }
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
            && CONNECTION.authorizes(CONNECTION.connectionNonce(), 0L, true);
    }

    public static boolean isTopologyNegotiated() {
        return CONNECTION.authorizes(
            CONNECTION.connectionNonce(),
            DesktopProtocol.CAP_INVENTORY_TOPOLOGY,
            false
        );
    }

    public static boolean clickSlot(int debugId, int sessionId, int slotIndex, int button, ContainerInput input, ItemStack clientCarried) {
        SessionAuth auth = sessionAuth(sessionId);
        if (auth == null) {
            return false;
        }
        DesktopDebug.trace("client send click id={} session={} slot={} button={} input={} clientCarried={}", debugId, sessionId, slotIndex, button, input, clientCarried);
        return send(new DesktopClickPayload(CONNECTION.connectionNonce(), sessionId, auth.token(), auth.stateId(), debugId, slotIndex, button, input.name(), clientCarried.copy()), "click");
    }

    public static boolean quickMoveSlot(int sourceSessionId, int sourceSlotIndex, int targetKind, int targetSessionId) {
        SessionAuth source = sessionAuth(sourceSessionId);
        SessionAuth target = sessionAuth(targetKind == DesktopPackets.QUICK_TARGET_SESSION ? targetSessionId : DesktopPackets.PLAYER_MENU_SESSION);
        if (source == null || target == null) {
            return false;
        }
        DesktopDebug.trace(
            "client send quick move sourceSession={} sourceSlot={} targetKind={} targetSession={}",
            sourceSessionId,
            sourceSlotIndex,
            targetKind,
            targetSessionId
        );
        return send(new DesktopQuickMovePayload(
            CONNECTION.connectionNonce(), sourceSessionId, source.token(), source.stateId(), sourceSlotIndex,
            targetKind, targetSessionId, target.token(), target.stateId()
        ), "quick-move");
    }

    public static boolean clickButton(int sessionId, int buttonId) {
        SessionAuth auth = sessionAuth(sessionId);
        if (auth == null) {
            return false;
        }
        DesktopDebug.trace("client send button session={} button={}", sessionId, buttonId);
        return send(new DesktopButtonPayload(CONNECTION.connectionNonce(), sessionId, auth.token(), auth.stateId(), buttonId), "button");
    }

    public static boolean placeRecipe(int sessionId, RecipeDisplayId recipeId, boolean useMaxItems) {
        SessionAuth auth = sessionAuth(sessionId);
        if (auth == null) {
            return false;
        }
        DesktopDebug.trace("client send recipe place session={} recipe={} useMax={}", sessionId, recipeId, useMaxItems);
        return send(new DesktopPlaceRecipePayload(CONNECTION.connectionNonce(), sessionId, auth.token(), auth.stateId(), recipeId, useMaxItems), "recipe-place");
    }

    public static boolean transferJeiRecipe(int targetSessionId, Identifier recipeId, boolean maxTransfer) {
        SessionAuth auth = sessionAuth(targetSessionId);
        if (auth == null || !CONNECTION.authorizes(CONNECTION.connectionNonce(), DesktopProtocol.CAP_RECIPE_TRANSFER, true)) {
            return false;
        }
        DesktopDebug.trace("client send JEI transfer targetSession={} recipe={} max={}", targetSessionId, recipeId, maxTransfer);
        return send(new DesktopJeiTransferPayload(CONNECTION.connectionNonce(), targetSessionId, auth.token(), auth.stateId(), recipeId, maxTransfer), "jei-transfer");
    }

    public static boolean purchaseInventorySlot() {
        SessionAuth auth = sessionAuth(DesktopPackets.PLAYER_MENU_SESSION);
        if (auth == null) {
            return false;
        }
        DesktopDebug.trace("client send inventory slot purchase");
        return send(new InventorySlotPurchasePayload(CONNECTION.connectionNonce(), auth.token(), auth.stateId()), "inventory-slot-purchase");
    }

    public static boolean renameAnvil(int sessionId, String name) {
        SessionAuth auth = sessionAuth(sessionId);
        if (auth == null) {
            return false;
        }
        DesktopDebug.trace("client send rename session={} name={}", sessionId, name);
        return send(new DesktopRenamePayload(CONNECTION.connectionNonce(), sessionId, auth.token(), auth.stateId(), name), "rename");
    }

    public static boolean sendCustomPayload(int sessionId, Identifier channel, byte[] data) {
        SessionAuth auth = sessionAuth(sessionId);
        if (auth == null) {
            return false;
        }
        DesktopDebug.trace("client send custom session={} channel={} bytes={}", sessionId, channel, data.length);
        return send(new DesktopCustomPayload(CONNECTION.connectionNonce(), sessionId, auth.token(), auth.stateId(), channel, data), "custom");
    }

    public static boolean syncCarried(ItemStack carried) {
        SessionAuth auth = sessionAuth(DesktopPackets.PLAYER_MENU_SESSION);
        if (auth == null) {
            return false;
        }
        DesktopDebug.trace("client send carried stack={}", carried);
        return send(new DesktopCarriedPayload(CONNECTION.connectionNonce(), auth.token(), auth.stateId(), carried.copy()), "carried");
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
        CONNECTION.reset();
        SESSION_TOKENS.clear();
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

    private record SessionAuth(long token, int stateId) {
    }
}
