package net.fabricmc.fabric.api.client.keymapping.v1;

import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

public final class KeyMappingHelper {
    private static final List<KeyMapping> KEY_MAPPINGS = new ArrayList<>();

    private KeyMappingHelper() {
    }

    public static KeyMapping registerKeyMapping(KeyMapping keyMapping) {
        KEY_MAPPINGS.add(keyMapping);
        return keyMapping;
    }

    public static InputConstants.Key getBoundKeyOf(KeyMapping keyMapping) {
        return keyMapping.getKey();
    }

    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        for (KeyMapping keyMapping : KEY_MAPPINGS) {
            event.register(keyMapping);
        }
    }
}
