package com.salts_inventory_update.compat.ae2.mixin;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonParser;
import com.google.gson.JsonObject;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.tree.ClassNode;

class NativeDesktopPatchPluginTest {
    @Test
    @SuppressWarnings("unchecked")
    void remappedAndNamedTargetsApplyTheSameVerifiedBridge() throws Exception {
        var plugin = new NativeDesktopPatchPlugin();
        var patchesField = NativeDesktopPatchPlugin.class.getDeclaredField("patches");
        patchesField.setAccessible(true);
        var patches = (Map<String, JsonObject>) patchesField.get(plugin);
        var patch = JsonParser.parseString("""
                {"class":"fixture/NativeBuilder","fields":[],"changes":[],"methods":[],
                 "interfaces":{"original":[],"added":["fixture/DesktopBuilder"]}}
                """).getAsJsonObject();
        patches.put("fixture.NativeBuilder", patch);
        var activeField = NativeDesktopPatchPlugin.class.getDeclaredField("active");
        activeField.setAccessible(true);
        boolean previous = activeField.getBoolean(null);
        try {
            activeField.setBoolean(null, true);
            for (String target : new String[]{"fixture.NativeBuilder", "fixture/NativeBuilder"}) {
                assertTrue(plugin.shouldApplyMixin(target, "fixture.Hook"), target);
                var node = new ClassNode();
                node.name = "fixture/NativeBuilder";
                plugin.preApply(target, node, "fixture.Hook", null);
                assertEquals(java.util.List.of("fixture/DesktopBuilder"), node.interfaces);
            }
            assertFalse(plugin.shouldApplyMixin("fixture.Unverified", "fixture.Hook"));
            activeField.setBoolean(null, false);
            assertFalse(plugin.shouldApplyMixin("fixture/NativeBuilder", "fixture.Hook"));
        } finally {
            activeField.setBoolean(null, previous);
        }
    }
}
