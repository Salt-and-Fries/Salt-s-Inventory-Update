import com.google.gson.JsonParser;
import com.salts_inventory_update.compat.ae2.mixin.NativeClassPatch;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipFile;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LineNumberNode;

/** Proves hosted rendering/input branches retain every original native instruction. */
public final class VerifyAe2HostedBranches {
    public static void main(String[] args) throws Exception {
        int retained = 0;
        try (var jar = new ZipFile(args[0])) {
            for (String widget : List.of("AE2Button", "IconButton", "ProgressBar", "TabButton", "AEBaseScreen")) {
                String name = widget.equals("AEBaseScreen") ? "appeng/client/gui/AEBaseScreen" : "appeng/client/gui/widgets/" + widget;
                String methodName = widget.equals("AEBaseScreen") ? "mouseClicked" : "renderWidget";
                var type = new ClassNode();
                new ClassReader(jar.getInputStream(jar.getEntry(name + ".class")).readAllBytes())
                        .accept(type, ClassReader.EXPAND_FRAMES);
                var method = type.methods.stream().filter(m -> m.name.equals(methodName)).findFirst().orElseThrow();
                var original = java.util.Arrays.stream(method.instructions.toArray())
                        .filter(n -> !(n instanceof LineNumberNode)).toList();
                NativeClassPatch.apply(type, JsonParser.parseString(Files.readString(Path.of(args[1], name + ".json"))).getAsJsonObject());
                var updated = type.methods.stream().filter(m -> m.name.equals(methodName)).findFirst().orElseThrow();
                int cursor = 0;
                for (AbstractInsnNode node : updated.instructions) {
                    if (cursor < original.size() && node == original.get(cursor)) cursor++;
                }
                if (cursor != original.size()) throw new IllegalStateException("Native method was replaced: " + widget + "." + methodName);
                retained += cursor;
            }
        }
        System.out.println("Retained " + retained + " original instruction objects across five native rendering/input methods");
    }
}
