package com.salts_inventory_update.platform.fabric.api.client.keybinding.v1;

import java.lang.reflect.Method;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;

public final class KeyBindingHelper {
    private KeyBindingHelper() {
    }

    public static KeyMapping registerKeyBinding(KeyMapping keyMapping) {
        invokeRegister("net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper", "registerKeyBinding", keyMapping);
        return keyMapping;
    }

    public static InputConstants.Key getBoundKeyOf(KeyMapping keyMapping) {
        return invokeBoundKey("net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper", keyMapping);
    }

    private static void invokeRegister(String className, String methodName, KeyMapping keyMapping) {
        try {
            Class<?> helper = Class.forName(className);
            Method method = helper.getMethod(methodName, KeyMapping.class);
            method.invoke(null, keyMapping);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to register Fabric key mapping via " + className + "." + methodName, exception);
        }
    }

    private static InputConstants.Key invokeBoundKey(String className, KeyMapping keyMapping) {
        try {
            Class<?> helper = Class.forName(className);
            Method method = helper.getMethod("getBoundKeyOf", KeyMapping.class);
            return (InputConstants.Key) method.invoke(null, keyMapping);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to read Fabric key mapping via " + className + ".getBoundKeyOf", exception);
        }
    }
}
