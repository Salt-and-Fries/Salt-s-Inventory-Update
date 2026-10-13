import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.salts_inventory_update.compat.ae2.mixin.NativeClassPatch;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipFile;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

/** Build-time native ABI inventory. Output is intermediate evidence, never a production class. */
public final class Ae2ClassInventory {
    public static void main(String[] args) throws Exception {
        Path output = Path.of(args[1]);
        Files.createDirectories(output);
        try (var jar = new ZipFile(args[0])) {
            var entries = jar.entries();
            while (entries.hasMoreElements()) {
                var entry = entries.nextElement();
                if (!entry.getName().startsWith("appeng/") || !entry.getName().endsWith(".class")) continue;
                var node = new ClassNode();
                new ClassReader(jar.getInputStream(entry).readAllBytes()).accept(node, ClassReader.EXPAND_FRAMES);
                Path path = output.resolve(node.name + ".json");
                boolean metadataOnly = args.length > 2 && args[2].equals("--metadata-only");
                if (metadataOnly && !Files.exists(path)) continue;
                var result = metadataOnly ? JsonParser.parseString(Files.readString(path)).getAsJsonObject() : new JsonObject();
                result.addProperty("class", node.name); result.addProperty("access", node.access);
                result.addProperty("superName", node.superName);
                var interfaces = new JsonArray(); node.interfaces.forEach(interfaces::add); result.add("interfaces", interfaces);
                if (metadataOnly) { Files.writeString(path, new GsonBuilder().disableHtmlEscaping().create().toJson(result)); continue; }
                var fields = new JsonArray(); for (var field : node.fields) fields.add(NativeClassPatch.field(field)); result.add("fields", fields);
                var methods = new JsonArray(); for (var method : node.methods) methods.add(NativeClassPatch.method(method)); result.add("methods", methods);
                Files.createDirectories(path.getParent());
                Files.writeString(path, new GsonBuilder().disableHtmlEscaping().create().toJson(result));
            }
        }
    }
}
