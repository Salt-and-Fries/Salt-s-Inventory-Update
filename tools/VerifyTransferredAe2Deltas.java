import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.salts_inventory_update.compat.ae2.mixin.NativeClassPatch;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipFile;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.BasicVerifier;

/** Checks every transferred method and its stack without launching Minecraft. */
public final class VerifyTransferredAe2Deltas {
    private static JsonObject normalized(JsonObject method) {
        for (var handler : method.getAsJsonArray("handlers"))
            if (!handler.getAsJsonObject().has("type")) handler.getAsJsonObject().add("type", com.google.gson.JsonNull.INSTANCE);
        Map<String, String> labels = new HashMap<>();
        for (var instruction : method.getAsJsonArray("instructions")) {
            var node = instruction.getAsJsonObject();
            if (node.get("type").getAsInt() == 8) labels.put(node.get("label").getAsString(), "o"+labels.size());
        }
        return translate(method, labels).getAsJsonObject();
    }
    private static JsonElement translate(JsonElement value, Map<String, String> labels) {
        if (value.isJsonObject()) {
            var result = new JsonObject();
            value.getAsJsonObject().entrySet().forEach(e -> result.add(e.getKey(), translate(e.getValue(), labels)));
            return result;
        }
        if (value.isJsonArray()) {
            var result = new com.google.gson.JsonArray();
            value.getAsJsonArray().forEach(e -> result.add(translate(e, labels)));
            return result;
        }
        if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()) {
            String label = labels.get(value.getAsString());
            if (label != null) return new com.google.gson.JsonPrimitive(label);
        }
        return value;
    }
    public static void main(String[] args) throws Exception {
        Path patches = Path.of(args[1]), expectedFolder = Path.of(args[2]);
        var manifest = JsonParser.parseString(Files.readString(patches.resolve("manifest.json"))).getAsJsonObject();
        int classes = 0, methods = 0;
        try (var upstream = new ZipFile(args[0])) {
            for (var entry : manifest.getAsJsonArray("classes")) {
                String name = entry.getAsJsonObject().get("class").getAsString();
                var node = new ClassNode();
                new ClassReader(upstream.getInputStream(upstream.getEntry(name+".class")).readAllBytes()).accept(node, ClassReader.EXPAND_FRAMES);
                NativeClassPatch.apply(node, JsonParser.parseString(Files.readString(patches.resolve(name+".json"))).getAsJsonObject());
                // Round-trip catches missing labels and malformed frame serialization.
                var writer = new ClassWriter(0); node.accept(writer);
                var roundTrip = new ClassNode(); new ClassReader(writer.toByteArray()).accept(roundTrip, ClassReader.EXPAND_FRAMES);
                var expected = JsonParser.parseString(Files.readString(expectedFolder.resolve(name+".json"))).getAsJsonObject();
                if (expected.has("interfaces")) {
                    var interfaces = new java.util.ArrayList<String>(); expected.getAsJsonArray("interfaces").forEach(x -> interfaces.add(x.getAsString()));
                    if (!roundTrip.interfaces.equals(interfaces)) throw new IllegalStateException("Transferred class interfaces differ: " + name);
                }
                Map<String, JsonObject> expectedFields = new HashMap<>();
                for (var field : expected.getAsJsonArray("fields")) expectedFields.put(field.getAsJsonObject().get("name").getAsString(), field.getAsJsonObject());
                for (var field : roundTrip.fields) {
                    var wanted = expectedFields.remove(field.name); var actual = NativeClassPatch.field(field);
                    if (wanted == null || !actual.get("desc").equals(wanted.get("desc")) || !actual.get("access").equals(wanted.get("access"))
                            || !actual.get("visibleAnnotations").equals(wanted.get("visibleAnnotations"))
                            || !actual.get("invisibleAnnotations").equals(wanted.get("invisibleAnnotations")))
                        throw new IllegalStateException("Transferred field metadata differs: "+name+"."+field.name);
                }
                if (!expectedFields.isEmpty()) throw new IllegalStateException("Missing transferred fields: "+name+expectedFields.keySet());
                Map<String, JsonObject> desired = new HashMap<>();
                for (var method : expected.getAsJsonArray("methods")) {
                    var m = method.getAsJsonObject(); desired.put(m.get("name").getAsString()+m.get("desc").getAsString(), normalized(m));
                }
                for (var method : node.methods) {
                    var wanted = desired.remove(method.name+method.desc);
                    var actual = NativeClassPatch.method(method);
                    if (wanted == null || !actual.get("instructions").equals(wanted.get("instructions")) || !actual.get("handlers").equals(wanted.get("handlers"))) {
                        Files.writeString(expectedFolder.resolve("actual-transferred-method.json"), actual.toString());
                        Files.writeString(expectedFolder.resolve("expected-transferred-method.json"), String.valueOf(wanted));
                        throw new IllegalStateException("Transferred delta differs: "+name+"."+method.name+method.desc);
                    }
                    methods++;
                }
                for (var method : roundTrip.methods) if ((method.access & (Opcodes.ACC_ABSTRACT|Opcodes.ACC_NATIVE)) == 0)
                    new Analyzer<>(new BasicVerifier()).analyze(name, method);
                if (!desired.isEmpty()) throw new IllegalStateException("Missing transferred methods: "+name+desired.keySet());
                classes++;
            }
        }
        System.out.println("Verified "+classes+" transferred native deltas and "+methods+" method stacks");
    }
}
