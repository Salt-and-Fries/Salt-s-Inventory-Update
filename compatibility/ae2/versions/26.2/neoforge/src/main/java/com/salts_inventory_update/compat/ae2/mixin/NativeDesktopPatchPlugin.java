package com.salts_inventory_update.compat.ae2.mixin;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.objectweb.asm.tree.ClassNode;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.service.MixinService;

/**
 * Loads only the fork's integration deltas. Every original native method is
 * verified before any hook is enabled. Missing or unsupported AE2 keeps its
 * ordinary UI; unrelated integrations continue to initialize independently.
 */
public final class NativeDesktopPatchPlugin implements IMixinConfigPlugin {
    private static boolean active;
    private final Map<String, JsonObject> patches = new HashMap<>();
    public static boolean active() { return active; }

    private static JsonObject read(String path) throws IOException {
        var stream = NativeDesktopPatchPlugin.class.getClassLoader().getResourceAsStream(path);
        if (stream == null) throw new IOException("Missing AE2 integration patch: " + path);
        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    @Override
    public void onLoad(String mixinPackage) {
        active = false;
        try {
            MixinService.getService().getBytecodeProvider().getClassNode("appeng.core.AppEng");
        } catch (ClassNotFoundException | IOException absent) {
            return;
        }
        try {
            boolean client = MixinEnvironment.getCurrentEnvironment().getSide() != MixinEnvironment.Side.SERVER;
            var manifest = read("ae2-native-patches/manifest.json");
            for (var item : manifest.getAsJsonArray("classes")) {
                String name = item.getAsJsonObject().get("class").getAsString();
                if (!client && name.startsWith("appeng/client/")) continue;
                JsonObject patch = read("ae2-native-patches/" + name + ".json");
                ClassNode raw = MixinService.getService().getBytecodeProvider().getClassNode(name.replace('/', '.'));
                var writer = new org.objectweb.asm.ClassWriter(0);
                raw.accept(writer);
                ClassNode original = new ClassNode();
                new org.objectweb.asm.ClassReader(writer.toByteArray()).accept(original, org.objectweb.asm.ClassReader.EXPAND_FRAMES);
                NativeClassPatch.verify(original, patch);
                patches.put(name.replace('/', '.'), patch);
            }
            active = true;
            LoggerFactory.getLogger(NativeDesktopPatchPlugin.class).info("Verified {} native AE2 desktop hooks", patches.size());
        } catch (IOException | ClassNotFoundException | RuntimeException unsupported) {
            patches.clear();
            LoggerFactory.getLogger(NativeDesktopPatchPlugin.class).warn("AE2 desktop hooks do not match the installed native ABI", unsupported);
        }
    }

    @Override public String getRefMapperConfig() { return null; }
    @Override public boolean shouldApplyMixin(String targetClassName, String mixinClassName) { return active && patches.containsKey(targetClassName); }
    @Override public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) { }
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String name, ClassNode target, String mixinClassName, IMixinInfo info) {
        try { NativeClassPatch.apply(target, patches.get(name)); }
        catch (RuntimeException failure) {
            LoggerFactory.getLogger(NativeDesktopPatchPlugin.class).error("Native AE2 hook failed for {}", name, failure);
            String output = System.getenv("SIU_QA_OUTPUT");
            if (output != null) try {
                var evidence = new com.google.gson.JsonArray();
                for (var method : target.methods) evidence.add(NativeClassPatch.method(method));
                java.nio.file.Files.writeString(java.nio.file.Path.of(output, "native-hook-error.json"), evidence.toString());
            } catch (IOException ignored) { }
            throw failure;
        }
    }
    @Override public void postApply(String name, ClassNode target, String mixinClassName, IMixinInfo info) { }
}
