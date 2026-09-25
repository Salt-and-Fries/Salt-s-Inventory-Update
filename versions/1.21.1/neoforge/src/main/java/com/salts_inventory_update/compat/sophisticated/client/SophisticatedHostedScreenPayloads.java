package com.salts_inventory_update.compat.sophisticated.client;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.AbstractContainerMenu;

public final class SophisticatedHostedScreenPayloads {
    private SophisticatedHostedScreenPayloads() {
    }

    public static void apply(Minecraft minecraft, AbstractContainerMenu menu, ResourceLocation channel, byte[] data) {
        String namespace = channel.getNamespace();
        String className = switch (namespace) {
            case "sophisticatedcore" -> "com.salts_inventory_update.compat.sophisticated.client.SophisticatedCoreClientState";
            case "sophisticatedbackpacks" -> "com.salts_inventory_update.compat.sophisticated.client.SophisticatedBackpackClientState";
            default -> "";
        };
        if (className.isEmpty()) {
            return;
        }
        try {
            Class<?> type = Class.forName(className);
            Method method = type.getMethod("apply", Minecraft.class, AbstractContainerMenu.class, ResourceLocation.class, byte[].class);
            method.invoke(null, minecraft, menu, channel, data);
        } catch (ReflectiveOperationException exception) {
            Throwable cause = exception instanceof InvocationTargetException invocation && invocation.getCause() != null
                ? invocation.getCause()
                : exception;
            throw new IllegalStateException("Failed to apply hosted Sophisticated payload " + channel, cause);
        }
    }
}
