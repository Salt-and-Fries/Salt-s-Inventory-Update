package com.salts_inventory_update.compat.ae2.nativeview;

import static org.junit.jupiter.api.Assertions.*;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.junit.jupiter.api.Test;

class LegacyKeyBindingTest {
    @Test void keysymBindingsUseTheNativeKeyboardMatcher() {
        var mapping = new KeyMapping("ae2-test-key", InputConstants.Type.KEYSYM, 65, "ae2-test");
        assertTrue(LegacyKeyBinding.isActiveAndMatches(mapping, InputConstants.Type.KEYSYM.getOrCreate(65)));
        assertFalse(LegacyKeyBinding.isActiveAndMatches(mapping, InputConstants.Type.KEYSYM.getOrCreate(66)));
    }
    @Test void scancodeBindingsKeepTheirNativeUnknownKeyCodeContract() {
        var mapping = new KeyMapping("ae2-test-scan", InputConstants.Type.SCANCODE, 30, "ae2-test");
        assertTrue(LegacyKeyBinding.isActiveAndMatches(mapping, InputConstants.Type.SCANCODE.getOrCreate(30)));
        assertFalse(LegacyKeyBinding.isActiveAndMatches(mapping, InputConstants.Type.SCANCODE.getOrCreate(31)));
    }
    @Test void mouseBindingsUseTheNativeMouseMatcher() {
        var mapping = new KeyMapping("ae2-test-mouse", InputConstants.Type.MOUSE, 2, "ae2-test");
        assertTrue(LegacyKeyBinding.isActiveAndMatches(mapping, InputConstants.Type.MOUSE.getOrCreate(2)));
        assertFalse(LegacyKeyBinding.isActiveAndMatches(mapping, InputConstants.Type.MOUSE.getOrCreate(0)));
    }
}
