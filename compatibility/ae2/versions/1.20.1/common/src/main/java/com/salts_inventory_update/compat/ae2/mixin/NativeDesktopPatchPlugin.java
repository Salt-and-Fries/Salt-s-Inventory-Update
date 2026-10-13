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
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.service.MixinService;

/** Verifies the entire optional native contract before applying desktop additions. */
public final class NativeDesktopPatchPlugin implements IMixinConfigPlugin {
    private static boolean active;
    private final Map<String,JsonObject> patches=new HashMap<>();
    public static boolean active() { return active; }
    private static JsonObject read(String path) throws IOException {
        var stream=NativeDesktopPatchPlugin.class.getClassLoader().getResourceAsStream(path);
        if (stream==null) throw new IOException("Missing AE2 native contract: "+path);
        try (var reader=new InputStreamReader(stream,StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }
    @Override public void onLoad(String mixinPackage) {
        active=false;
        try { MixinService.getService().getBytecodeProvider().getClassNode("appeng.core.AppEng"); }
        catch (ClassNotFoundException|IOException absent) { return; }
        Throwable failure=null;
        for (String namespace:List.of("ae2-native-patches","ae2-native-patches-runtime")) {
            try {
                var manifest=read(namespace+"/manifest.json");
                boolean complete=manifest.has("clientPortComplete")&&manifest.get("clientPortComplete").getAsBoolean();
                if (!complete) {
                    if (!Boolean.parseBoolean(System.getenv("SIU_AE2_LEGACY_QA"))) return;
                    if (!manifest.get("pointerPortComplete").getAsBoolean()
                            ||!manifest.get("terminalKeyboardPortComplete").getAsBoolean()) return;
                }
                boolean client=MixinEnvironment.getCurrentEnvironment().getSide()!=MixinEnvironment.Side.SERVER;
                Map<String,JsonObject> verified=new HashMap<>();
                for (var entry:manifest.getAsJsonArray("classes")) {
                    String name=entry.getAsJsonObject().get("class").getAsString();
                    if (!client&&(name.startsWith("appeng/client/")||name.startsWith("appeng/init/client/"))) continue;
                    var patch=read(namespace+"/"+name+".json");
                    var raw=MixinService.getService().getBytecodeProvider().getClassNode(name.replace('/','.'));
                    var writer=new ClassWriter(0);raw.accept(writer);
                    var original=new ClassNode();new ClassReader(writer.toByteArray()).accept(original,ClassReader.EXPAND_FRAMES);
                    NativeClassPatch.verify(original,patch);verified.put(name.replace('/','.'),patch);
                }
                patches.putAll(verified);active=true;
                LoggerFactory.getLogger(getClass()).info("Verified {} AE2 desktop hooks in {}",patches.size(),namespace);
                return;
            } catch (IOException|ClassNotFoundException|RuntimeException unsupported) { failure=unsupported; }
        }
        LoggerFactory.getLogger(getClass()).warn("AE2 desktop hooks do not match the installed native ABI",failure);
    }
    @Override public String getRefMapperConfig() { return null; }
    @Override public boolean shouldApplyMixin(String target,String mixin) { return active&&patches.containsKey(target); }
    @Override public void acceptTargets(Set<String> mine,Set<String> others) { }
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String name,ClassNode target,String mixin,IMixinInfo info) {
        try { NativeClassPatch.apply(target,patches.get(name)); }
        catch (RuntimeException failure) {
            String output=System.getenv("SIU_QA_OUTPUT");
            if (output!=null) try {
                var evidence=new com.google.gson.JsonArray();
                for (var method:target.methods) evidence.add(NativeClassPatch.method(method));
                java.nio.file.Files.writeString(java.nio.file.Path.of(output,"native-hook-error.json"),evidence.toString());
            } catch (IOException ignored) { }
            throw failure;
        }
    }
    @Override public void postApply(String name,ClassNode target,String mixin,IMixinInfo info) { }
}
