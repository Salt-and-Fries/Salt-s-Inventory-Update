package com.salts_inventory_update.compat.ae2.mixin;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonParser;
import com.google.gson.JsonObject;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.tree.ClassNode;

class NativeDesktopPatchPluginTest {
    private static void inputMembers(com.google.gson.JsonElement node,java.util.Set<String> names){
        if(node.isJsonArray()){for(var child:node.getAsJsonArray())inputMembers(child,names);}
        else if(node.isJsonObject()){
            var o=node.getAsJsonObject();if(o.has("owner")&&o.has("name")&&o.get("owner").getAsString().startsWith("com/salts_inventory_update/api/client/input/"))names.add(o.get("owner").getAsString()+"."+o.get("name").getAsString());
            for(var e:o.entrySet())inputMembers(e.getValue(),names);
        }
    }
    @Test void productionInputRecordsKeepTheirPublicAccessors() throws Exception {
        var named=new java.util.HashSet<String>();var runtime=new java.util.HashSet<String>();
        for(var prefix:new String[]{"ae2-native-patches","ae2-native-patches-runtime"})try(var stream=getClass().getResourceAsStream("/"+prefix+"/appeng/client/gui/AEBaseScreen.json")){
            inputMembers(JsonParser.parseReader(new java.io.InputStreamReader(stream)),prefix.endsWith("runtime")?runtime:named);
        }
        assertFalse(named.isEmpty());assertEquals(named,runtime,"Minecraft member-name fallback must not rename public SIU input records");
    }
    @Test void supportedAddonFactoriesHaveDesktopStyleDocuments() throws Exception {
        for(var name:new String[]{"megacells/cell_dock","megacells/mega_interface","megacells/mega_pattern_provider","requester","requester_terminal"})
            try(var stream=getClass().getResourceAsStream("/assets/ae2/screens/desktop/"+name+".json")){
                assertNotNull(stream,name);
                var style=JsonParser.parseReader(new java.io.InputStreamReader(stream)).getAsJsonObject();
                assertTrue(style.getAsJsonObject("desktop").get("height").getAsInt()>0,name);
            }
    }
    @Test
    void productionKeyboardBridgeUsesIntermediaryKeyType() throws Exception {
        try (var stream = getClass().getResourceAsStream("/ae2-native-patches-runtime/appeng/client/gui/AEBaseScreen.json")) {
            assertNotNull(stream);
            var text = new String(stream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            assertFalse(text.contains("com/mojang/blaze3d/platform/InputConstants$Key"));
            assertTrue(text.contains("(Lnet/minecraft/class_304;Lnet/minecraft/class_3675$class_306;)Z"));
            assertFalse(text.contains("\"name\":\"getFocused\""),"Inherited Minecraft calls also need runtime member names");
            assertTrue(text.contains("\"name\":\"method_25399\""));
        }
    }
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
