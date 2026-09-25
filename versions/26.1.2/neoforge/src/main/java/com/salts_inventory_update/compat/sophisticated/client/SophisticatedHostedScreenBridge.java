package com.salts_inventory_update.compat.sophisticated.client;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import com.salts_inventory_update.api.client.desktop.DesktopInputContext;
import com.salts_inventory_update.client.InternalDesktopRenderContext.QuickCraftPreview;
import com.salts_inventory_update.compat.sophisticated.mixin.AbstractContainerScreenAccessor;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;

/** Thread-confined routing scope used while an official Sophisticated screen is hosted by Salt. */
public final class SophisticatedHostedScreenBridge {
    private static final ThreadLocal<@Nullable DesktopInputContext<?, ?>> INPUT = new ThreadLocal<>();
    private static final ThreadLocal<@Nullable RenderScope> RENDERING = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> SALT_TAB_BACKGROUNDS = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> HOSTED_QUICK_CRAFT_PREVIEW = new ThreadLocal<>();

    private SophisticatedHostedScreenBridge() {
    }

    public static <T> T withInput(DesktopInputContext<?, ?> context, java.util.function.Supplier<T> action) {
        DesktopInputContext<?, ?> previous = INPUT.get();
        INPUT.set(context);
        try {
            return action.get();
        } finally {
            if (previous == null) {
                INPUT.remove();
            } else {
                INPUT.set(previous);
            }
        }
    }

    public static void withRendering(int tooltipOffsetX, int tooltipOffsetY, Runnable action) {
        RenderScope previous = RENDERING.get();
        RENDERING.set(new RenderScope(tooltipOffsetX, tooltipOffsetY));
        try {
            action.run();
        } finally {
            if (previous == null) {
                RENDERING.remove();
            } else {
                RENDERING.set(previous);
            }
        }
    }

    public static boolean isRendering() {
        return RENDERING.get() != null;
    }

    /** Replaces Sophisticated's tab plate while retaining its native icon and panel children. */
    public static void withSaltTabBackgrounds(Runnable action) {
        Boolean previous = SALT_TAB_BACKGROUNDS.get();
        SALT_TAB_BACKGROUNDS.set(Boolean.TRUE);
        try {
            action.run();
        } finally {
            if (previous == null) {
                SALT_TAB_BACKGROUNDS.remove();
            } else {
                SALT_TAB_BACKGROUNDS.set(previous);
            }
        }
    }

    public static boolean useSaltTabBackgrounds() {
        return Boolean.TRUE.equals(SALT_TAB_BACKGROUNDS.get());
    }

    /** Applies Salt's cross-window drag preview to native slot extraction without retaining native input state. */
    public static void withQuickCraftPreview(
        AbstractContainerScreen<?> screen,
        @Nullable QuickCraftPreview preview,
        Runnable action
    ) {
        if (preview == null) {
            action.run();
            return;
        }

        AbstractContainerScreenAccessor access = (AbstractContainerScreenAccessor) screen;
        Set<Slot> quickCraftSlots = access.salts_inventory_update$quickCraftSlots();
        List<Slot> previousSlots = List.copyOf(quickCraftSlots);
        boolean previousQuickCrafting = access.salts_inventory_update$isQuickCrafting();
        int previousType = access.salts_inventory_update$quickCraftingType();
        int previousRemainder = access.salts_inventory_update$quickCraftingRemainder();
        Boolean previousHostedPreview = HOSTED_QUICK_CRAFT_PREVIEW.get();
        quickCraftSlots.clear();
        quickCraftSlots.addAll(preview.targetSlots());
        access.salts_inventory_update$setQuickCrafting(true);
        access.salts_inventory_update$setQuickCraftingType(preview.type());
        HOSTED_QUICK_CRAFT_PREVIEW.set(Boolean.TRUE);
        try {
            action.run();
        } finally {
            quickCraftSlots.clear();
            quickCraftSlots.addAll(previousSlots);
            access.salts_inventory_update$setQuickCrafting(previousQuickCrafting);
            access.salts_inventory_update$setQuickCraftingType(previousType);
            access.salts_inventory_update$setQuickCraftingRemainder(previousRemainder);
            if (previousHostedPreview == null) {
                HOSTED_QUICK_CRAFT_PREVIEW.remove();
            } else {
                HOSTED_QUICK_CRAFT_PREVIEW.set(previousHostedPreview);
            }
        }
    }

    /** True only while Salt asks the official renderer to draw its live cross-window drag preview. */
    public static boolean isHostedQuickCraftPreview() {
        return Boolean.TRUE.equals(HOSTED_QUICK_CRAFT_PREVIEW.get());
    }

    public static void setHoveredSlot(AbstractContainerScreen<?> screen, @Nullable Slot slot) {
        ((AbstractContainerScreenAccessor) screen).salts_inventory_update$setHoveredSlot(slot);
    }

    /** Clears native drag bookkeeping without invoking the native release path. */
    public static void cancelQuickCraft(AbstractContainerScreen<?> screen) {
        AbstractContainerScreenAccessor access = (AbstractContainerScreenAccessor) screen;
        access.salts_inventory_update$quickCraftSlots().clear();
        access.salts_inventory_update$setQuickCrafting(false);
        access.salts_inventory_update$setQuickCraftingType(0);
        access.salts_inventory_update$setQuickCraftingRemainder(0);
        access.salts_inventory_update$setSkipNextRelease(false);
    }

    public static int translateTooltipX(int nativeX) {
        RenderScope scope = RENDERING.get();
        return scope == null ? nativeX : nativeX + scope.tooltipOffsetX;
    }

    public static int translateTooltipY(int nativeY) {
        RenderScope scope = RENDERING.get();
        return scope == null ? nativeY : nativeY + scope.tooltipOffsetY;
    }

    public static boolean clickSlot(int slotId, int button, ContainerInput input) {
        DesktopInputContext<?, ?> context = INPUT.get();
        return context != null && context.clickSlot(slotId, button, input);
    }

    public static boolean sendToServer(CustomPacketPayload first, CustomPacketPayload[] additional) {
        DesktopInputContext<?, ?> context = INPUT.get();
        if (context == null || !canRoute(first)) {
            return false;
        }
        for (CustomPacketPayload payload : additional) {
            if (!canRoute(payload)) {
                return false;
            }
        }
        route(context, first);
        for (CustomPacketPayload payload : additional) {
            route(context, payload);
        }
        // A recognized hosted packet must never fall through to NeoForge's active vanilla menu,
        // even if Salt's authenticated mutation lane is temporarily unavailable.
        return true;
    }

    private static boolean canRoute(CustomPacketPayload payload) {
        String id = payload.type().id().toString();
        return id.equals("sophisticatedcore:sync_container_client_data")
            || id.equals("sophisticatedcore:transfer_items")
            || id.equals("sophisticatedcore:transfer_full_slot")
            || id.equals("sophisticatedcore:set_ghost_slot")
            || id.equals("sophisticatedcore:set_memory_slot")
            || id.equals("sophisticatedcore:tank_click")
            || id.equals("sophisticatedbackpacks:backpack_open")
            || id.equals("sophisticatedbackpacks:mob_catcher_release")
            || id.equals("sophisticatedstorage:open_storage_inventory");
    }

    private static boolean route(DesktopInputContext<?, ?> context, CustomPacketPayload payload) {
        String namespace = payload.type().id().getNamespace();
        String className = switch (namespace) {
            case "sophisticatedcore" -> "com.salts_inventory_update.compat.sophisticated.client.SophisticatedCoreClientPackets";
            case "sophisticatedbackpacks" -> "com.salts_inventory_update.compat.sophisticated.client.SophisticatedBackpackClientPackets";
            case "sophisticatedstorage" -> "com.salts_inventory_update.compat.sophisticated.client.SophisticatedStorageClientPackets";
            default -> "";
        };
        if (className.isEmpty()) {
            return false;
        }
        try {
            Class<?> type = Class.forName(className);
            Method method = type.getMethod("send", DesktopInputContext.class, CustomPacketPayload.class);
            return (boolean) method.invoke(null, context, payload);
        } catch (ReflectiveOperationException exception) {
            Throwable cause = exception instanceof InvocationTargetException invocation && invocation.getCause() != null
                ? invocation.getCause()
                : exception;
            throw new IllegalStateException("Failed to route hosted Sophisticated payload " + payload.type().id(), cause);
        }
    }

    private record RenderScope(int tooltipOffsetX, int tooltipOffsetY) {
    }
}
