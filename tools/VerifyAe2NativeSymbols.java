import com.google.gson.JsonParser;
import com.salts_inventory_update.compat.ae2.mixin.NativeClassPatch;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.zip.ZipFile;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Handle;
import org.objectweb.asm.ConstantDynamic;
import org.objectweb.asm.tree.*;

/** Resolve integration instruction member references without defining optional game classes. */
public final class VerifyAe2NativeSymbols {
    private static final Map<String, ClassNode> TYPES = new HashMap<>();
    private static void add(byte[] bytes) {
        var node = new ClassNode(); new ClassReader(bytes).accept(node, ClassReader.EXPAND_FRAMES); TYPES.put(node.name, node);
    }
    private static void index(Path path) throws Exception {
        if (Files.isDirectory(path)) {
            try (var paths = Files.walk(path)) { for (var file : paths.filter(p -> p.toString().endsWith(".class")).toList()) add(Files.readAllBytes(file)); }
        } else try (var jar = new ZipFile(path.toFile())) {
            for (var entry : Collections.list(jar.entries())) if (entry.getName().endsWith(".class")) add(jar.getInputStream(entry).readAllBytes());
        }
    }
    private static boolean exists(String owner, String name, String desc, boolean field, Set<String> visited) {
        if (!visited.add(owner)) return false;
        var type = TYPES.get(owner);
        if (type == null) {
            try (var stream = ClassLoader.getSystemResourceAsStream(owner+".class")) {
                if (stream == null) return false; add(stream.readAllBytes()); type = TYPES.get(owner);
            } catch (Exception ignored) { return false; }
        }
        if (field) { for (var member : type.fields) if (member.name.equals(name) && member.desc.equals(desc)) return true; }
        else { for (var member : type.methods) if (member.name.equals(name) && member.desc.equals(desc)) return true; }
        if (name.equals("<init>")) return false;
        if (type.superName != null && exists(type.superName, name, desc, field, visited)) return true;
        for (String parent : type.interfaces) if (exists(parent, name, desc, field, visited)) return true;
        return false;
    }
    private static int check(String owner, String member, String desc, boolean field, String from, Set<String> failures) {
        if (!owner.startsWith("net/minecraft/") && !owner.startsWith("appeng/") && !owner.startsWith("com/mojang/blaze3d/") && !owner.startsWith("com/salts_inventory_update/")) return 0;
        if (owner.startsWith("[")) return 0;
        if (!exists(owner, member, desc, field, new HashSet<>())) failures.add(owner + "." + member + desc + " (from " + from + ")");
        return 1;
    }
    private static void checkOwnerKind(String owner, boolean interfaceCall, String from, Set<String> failures) {
        var type = TYPES.get(owner);
        if (type != null && interfaceCall != ((type.access & org.objectweb.asm.Opcodes.ACC_INTERFACE) != 0))
            failures.add(owner + " invocation owner kind (from " + from + ")");
    }
    public static void main(String[] args) throws Exception {
        Path patches = Path.of(args[0]);
        for (int i = 1; i < args.length; i++) index(Path.of(args[i]));
        var manifest = JsonParser.parseString(Files.readString(patches.resolve("manifest.json"))).getAsJsonObject();
        for (var entry : manifest.getAsJsonArray("classes")) {
            String name = entry.getAsJsonObject().get("class").getAsString();
            NativeClassPatch.apply(TYPES.get(name), JsonParser.parseString(Files.readString(patches.resolve(name+".json"))).getAsJsonObject());
        }
        Set<String> failures = new TreeSet<>(); int checked = 0;
        for (var entry : manifest.getAsJsonArray("classes")) {
            String name = entry.getAsJsonObject().get("class").getAsString();
            for (var method : TYPES.get(name).methods) for (var instruction : method.instructions) {
                String from = name + "." + method.name;
                var constants = new java.util.ArrayDeque<Object>();
                if (instruction instanceof InvokeDynamicInsnNode dynamic) {
                    constants.add(dynamic.bsm); Collections.addAll(constants, dynamic.bsmArgs);
                } else if (instruction instanceof LdcInsnNode constant && constant.cst != null) constants.add(constant.cst);
                while (!constants.isEmpty()) {
                    Object value = constants.removeFirst();
                    if (value instanceof Handle handle) {
                        checked += check(handle.getOwner(), handle.getName(), handle.getDesc(), handle.getTag() <= 4, from + " bootstrap handle", failures);
                        if (handle.getTag() > 4) checkOwnerKind(handle.getOwner(), handle.isInterface(), from + " bootstrap handle", failures);
                    } else if (value instanceof ConstantDynamic dynamic) {
                        constants.add(dynamic.getBootstrapMethod());
                        for (int i = 0; i < dynamic.getBootstrapMethodArgumentCount(); i++) constants.add(dynamic.getBootstrapMethodArgument(i));
                    }
                }
                String owner, member, desc; boolean field;
                if (instruction instanceof MethodInsnNode call) {
                    owner = call.owner; member = call.name; desc = call.desc; field = false;
                    checkOwnerKind(owner, call.itf, from, failures);
                }
                else if (instruction instanceof FieldInsnNode access) { owner = access.owner; member = access.name; desc = access.desc; field = true; }
                else continue;
                checked += check(owner, member, desc, field, from, failures);
            }
        }
        for (String failure : failures) System.out.println("MISSING "+failure);
        if (!failures.isEmpty()) throw new IllegalStateException(failures.size()+" unresolved native integration symbols");
        System.out.println("Resolved "+checked+" native desktop instruction member references");
    }
}
