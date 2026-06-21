package com.salts_inventory_update.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.display.RecipeDisplayId;

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
import com.salts_inventory_update.network.DesktopPackets.DesktopMerchantOffersPayload;
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

public final class DesktopContainerClient {
    private static boolean readySent;

    private DesktopContainerClient() {
    }

    public static void initializeNetworking() {
        ClientPlayNetworking.registerGlobalReceiver(DesktopOpenSessionPayload.TYPE, (payload, context) -> {
            DesktopDebug.log("client payload open session={} title={} type={} special={}", payload.sessionId(), payload.title().getString(), payload.menuTypeId(), payload.specialKind());
            InventoryDesktopScreen.openOrAddSession(context.client(), DesktopContainerSession.create(context.client(), payload), payload.visible());
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
            DesktopDebug.trace("client payload carried stack={}", payload.carried());
            InventoryDesktopScreen screen = InventoryDesktopScreen.current(context.client());
            if (screen != null) {
                screen.setSharedCarried(payload.carried());
            } else if (context.client().player != null) {
                context.client().player.inventoryMenu.setCarried(payload.carried().copy());
            }
        });
        ClientPlayNetworking.registerGlobalReceiver(DesktopSessionClosedPayload.TYPE, (payload, context) -> {
            InventoryDesktopScreen screen = InventoryDesktopScreen.current(context.client());
            if (screen != null) {
                screen.removeSession(payload.sessionId());
            }
        });
        ClientPlayNetworking.registerGlobalReceiver(DesktopSessionVisibilityPayload.TYPE, (payload, context) -> {
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
        if (minecraft.player == null || minecraft.level == null) {
            readySent = false;
            return;
        }

        if (!readySent) {
            if (send(new DesktopReadyPayload(true), "ready")) {
                DesktopDebug.log("client desktop ready send");
                readySent = true;
            }
        }
    }

    public static boolean canSendDesktopPackets() {
        try {
            return ClientPlayNetworking.canSend(DesktopClickPayload.TYPE);
        } catch (IllegalStateException | IllegalArgumentException ignored) {
            return false;
        }
    }

    public static boolean canUseServerSessions() {
        try {
            return ClientPlayNetworking.canSend(DesktopReadyPayload.TYPE)
                && ClientPlayNetworking.canSend(DesktopClickPayload.TYPE)
                && ClientPlayNetworking.canSend(DesktopQuickMovePayload.TYPE)
                && ClientPlayNetworking.canSend(DesktopButtonPayload.TYPE)
                && ClientPlayNetworking.canSend(DesktopPlaceRecipePayload.TYPE)
                && ClientPlayNetworking.canSend(DesktopRenamePayload.TYPE)
                && ClientPlayNetworking.canSend(DesktopCloseSessionPayload.TYPE)
                && ClientPlayNetworking.canSend(DesktopSessionPinPayload.TYPE)
                && ClientPlayNetworking.canSend(DesktopSessionVisibilityPayload.TYPE)
                && ClientPlayNetworking.canSend(DesktopCustomPayload.TYPE)
                && ClientPlayNetworking.canSend(DesktopCarriedPayload.TYPE)
                && ClientPlayNetworking.canSend(InventorySlotPurchasePayload.TYPE);
        } catch (IllegalStateException | IllegalArgumentException ignored) {
            return false;
        }
    }

    public static boolean clickSlot(int debugId, int sessionId, int slotIndex, int button, ContainerInput input, ItemStack clientCarried) {
        DesktopDebug.trace("client send click id={} session={} slot={} button={} input={} clientCarried={}", debugId, sessionId, slotIndex, button, input, clientCarried);
        return send(new DesktopClickPayload(debugId, sessionId, slotIndex, button, input.name(), clientCarried.copy()), "click");
    }

    public static boolean quickMoveSlot(int sourceSessionId, int sourceSlotIndex, int targetKind, int targetSessionId) {
        DesktopDebug.trace(
            "client send quick move sourceSession={} sourceSlot={} targetKind={} targetSession={}",
            sourceSessionId,
            sourceSlotIndex,
            targetKind,
            targetSessionId
        );
        return send(new DesktopQuickMovePayload(sourceSessionId, sourceSlotIndex, targetKind, targetSessionId), "quick-move");
    }

    public static boolean clickButton(int sessionId, int buttonId) {
        DesktopDebug.trace("client send button session={} button={}", sessionId, buttonId);
        return send(new DesktopButtonPayload(sessionId, buttonId), "button");
    }

    public static boolean placeRecipe(int sessionId, RecipeDisplayId recipeId, boolean useMaxItems) {
        DesktopDebug.trace("client send recipe place session={} recipe={} useMax={}", sessionId, recipeId, useMaxItems);
        return send(new DesktopPlaceRecipePayload(sessionId, recipeId, useMaxItems), "recipe-place");
    }

    public static boolean purchaseInventorySlot() {
        DesktopDebug.trace("client send inventory slot purchase");
        return send(new InventorySlotPurchasePayload(), "inventory-slot-purchase");
    }

    public static boolean renameAnvil(int sessionId, String name) {
        DesktopDebug.trace("client send rename session={} name={}", sessionId, name);
        return send(new DesktopRenamePayload(sessionId, name), "rename");
    }

    public static boolean sendCustomPayload(int sessionId, Identifier channel, byte[] data) {
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

    public static void setSessionPinMode(int sessionId, PinMode pinMode) {
        DesktopDebug.trace("client send pin session={} pin={}", sessionId, pinMode);
        send(new DesktopSessionPinPayload(sessionId, pinModeToPacket(pinMode)), "pin");
    }

    public static void setSessionVisible(int sessionId, boolean visible) {
        DesktopDebug.trace("client send visibility session={} visible={}", sessionId, visible);
        send(new DesktopSessionVisibilityPayload(sessionId, visible), "visibility");
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
}
