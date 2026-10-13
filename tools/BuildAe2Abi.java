import com.google.gson.JsonParser;
import com.salts_inventory_update.compat.ae2.mixin.NativeClassPatch;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.BasicVerifier;

/** Compile-only declarations for native methods introduced by optional AE2 patches. */
public final class BuildAe2Abi {
    public static void main(String[] args) throws Exception {
        Path patches = Path.of(args[1]);
        try (var upstream = new ZipFile(args[0]); var output = new ZipOutputStream(Files.newOutputStream(Path.of(args[2])))) {
            var entries = upstream.entries();
            while (entries.hasMoreElements()) {
                var entry = entries.nextElement();
                if (!entry.getName().startsWith("appeng/") || !entry.getName().endsWith(".class")) continue;
                var node = new ClassNode();
                new ClassReader(upstream.getInputStream(entry).readAllBytes()).accept(node, ClassReader.EXPAND_FRAMES);
                Path patch = patches.resolve(node.name+".json");
                if (Files.exists(patch)) {
                    NativeClassPatch.apply(node, JsonParser.parseString(Files.readString(patch)).getAsJsonObject());
                    for (var method : node.methods) if ((method.access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_NATIVE)) == 0) {
                        try { new Analyzer<>(new BasicVerifier()).analyze(node.name, method); }
                        catch (org.objectweb.asm.tree.analysis.AnalyzerException failure) {
                            throw new IllegalStateException("Invalid native integration method: " + node.name + "." + method.name + method.desc, failure);
                        }
                    }
                }
                for (var method : node.methods) {
                    if ((method.access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_NATIVE)) != 0) continue;
                    method.instructions = new InsnList();
                    method.instructions.add(new InsnNode(Opcodes.ACONST_NULL));
                    method.instructions.add(new InsnNode(Opcodes.ATHROW));
                    method.tryCatchBlocks.clear(); method.localVariables = null;
                    method.visibleLocalVariableAnnotations = null; method.invisibleLocalVariableAnnotations = null;
                    method.maxStack = 1;
                    method.maxLocals = (method.access & Opcodes.ACC_STATIC) == 0 ? 1 : 0;
                    for (Type type : Type.getArgumentTypes(method.desc)) method.maxLocals += type.getSize();
                }
                var writer = new ClassWriter(0); node.accept(writer);
                output.putNextEntry(new ZipEntry(entry.getName())); output.write(writer.toByteArray()); output.closeEntry();
            }
        }
        System.out.println("Created declaration-only AE2 ABI; no native implementation or resources included");
    }
}
