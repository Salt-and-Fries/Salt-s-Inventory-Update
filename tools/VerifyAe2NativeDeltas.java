import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.salts_inventory_update.compat.ae2.mixin.NativeClassPatch;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipFile;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.tree.ClassNode;

/** Proves every extracted delta reproduces the fork while retaining upstream instructions. */
public final class VerifyAe2NativeDeltas {
    private static JsonObject expectedField(String owner, org.objectweb.asm.tree.FieldNode field, boolean sessionCpuState) {
        var value = NativeClassPatch.field(field);
        if (sessionCpuState && owner.equals("appeng/menu/me/crafting/CraftingCPUMenu")
                && java.util.Set.of("cpuStatusReady", "cpuAvailable", "hasActiveJob", "cpuStatusSerial").contains(field.name)) {
            // These desktop-only values use the public session payload instead of
            // extending ordinary AE2's GuiSync schema. All other metadata stays strict.
            for (String kind : java.util.List.of("visibleAnnotations", "invisibleAnnotations")) {
                var filtered = new com.google.gson.JsonArray();
                for (var annotation : value.getAsJsonArray(kind))
                    if (!annotation.getAsJsonObject().get("desc").getAsString().equals("Lappeng/menu/guisync/GuiSync;"))
                        filtered.add(annotation);
                value.add(kind, filtered);
            }
        }
        return value;
    }
    public static void main(String[] args) throws Exception {
        boolean sessionCpuState = args.length == 4 && args[3].equals("--session-cpu-state");
        if (args.length != 3 && !sessionCpuState) throw new IllegalArgumentException("Expected upstream JAR, reference JAR, patches, and optional --session-cpu-state");
        Path folder = Path.of(args[2]);
        var manifest = JsonParser.parseString(Files.readString(folder.resolve("manifest.json"))).getAsJsonObject();
        Map<String, String> mappings = new HashMap<>();
        manifest.getAsJsonObject("mappings").entrySet().forEach(entry -> mappings.put(entry.getKey(),entry.getValue().getAsString()));
        var remapper = new Remapper() {
            @Override public String map(String name) {
                for (var entry : mappings.entrySet()) {
                    if (name.equals(entry.getKey()) || name.startsWith(entry.getKey()+"$")) return entry.getValue()+name.substring(entry.getKey().length());
                }
                return name;
            }
        };
        int checked = 0;
        try (var upstream = new ZipFile(args[0]); var fork = new ZipFile(args[1])) {
            for (var item : manifest.getAsJsonArray("classes")) {
                var entry = item.getAsJsonObject();
                if (entry.has("newClass")) continue;
                String name = entry.get("class").getAsString();
                var original = new ClassNode();
                new ClassReader(upstream.getInputStream(upstream.getEntry(name+".class")).readAllBytes()).accept(original, ClassReader.EXPAND_FRAMES);
                var expected = new ClassNode();
                new ClassReader(fork.getInputStream(fork.getEntry(name+".class")).readAllBytes()).accept(new ClassRemapper(expected, remapper), ClassReader.EXPAND_FRAMES);
                var patch = JsonParser.parseString(Files.readString(folder.resolve(name+".json"))).getAsJsonObject();
                Map<String, JsonObject> originalFields = new HashMap<>();
                for (var field : original.fields) originalFields.put(field.name, NativeClassPatch.field(field));
                NativeClassPatch.apply(original, patch);
                if (!original.interfaces.equals(expected.interfaces) || !original.superName.equals(expected.superName))
                    throw new IllegalStateException("Delta class contracts differ from fork: " + name);
                Map<String, JsonObject> fields = new HashMap<>();
                for (var field : original.fields) fields.put(field.name, NativeClassPatch.field(field));
                for (var field : expected.fields) {
                    var actual = fields.remove(field.name);
                    if (!expectedField(name, field, sessionCpuState).equals(actual))
                        throw new IllegalStateException("Delta field metadata differs from fork: "+name+"."+field.name);
                }
                // The source cleanup removed a few unused native constants. Leave
                // those original fields intact so other AE2 consumers retain their ABI.
                for (var extra : fields.entrySet()) if (!extra.getValue().equals(originalFields.get(extra.getKey())))
                    throw new IllegalStateException("Unexpected changed native field: "+name+"."+extra.getKey());
                Map<String, JsonObject> methods = new HashMap<>();
                for (var method : original.methods) methods.put(method.name+method.desc, NativeClassPatch.method(method));
                for (var method : expected.methods) {
                    var actual = methods.remove(method.name+method.desc);
                    var desired = NativeClassPatch.method(method);
                    if (actual == null || !actual.get("instructions").equals(desired.get("instructions"))
                            || !actual.get("handlers").equals(desired.get("handlers"))
                            || !actual.get("visibleAnnotations").equals(desired.get("visibleAnnotations"))
                            || !actual.get("invisibleAnnotations").equals(desired.get("invisibleAnnotations"))) {
                        Path diagnostics = Path.of("build", "ae2-port", "fork-delta-diagnostics");
                        Files.createDirectories(diagnostics);
                        Files.writeString(diagnostics.resolve("actual-method.json"),String.valueOf(actual));
                        Files.writeString(diagnostics.resolve("expected-method.json"),desired.toString());
                        throw new IllegalStateException("Delta differs from fork: "+name+"."+method.name+method.desc);
                    }
                }
                if (!methods.isEmpty()) throw new IllegalStateException("Unexpected methods: "+name+methods.keySet());
                checked++;
            }
        }
        System.out.println("Verified "+checked+" native class deltas against unmodified upstream and the reference fork");
        if (sessionCpuState) System.out.println("Desktop CPU field metadata compared with public session transport; ordinary GuiSync annotations remain excluded");
    }
}
