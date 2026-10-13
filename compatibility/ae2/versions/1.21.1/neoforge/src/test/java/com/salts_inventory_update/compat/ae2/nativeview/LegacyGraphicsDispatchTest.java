package com.salts_inventory_update.compat.ae2.nativeview;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.salts_inventory_update.compat.ae2.mixin.NativeClassPatch;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

/** Executes the production bridges against a small native-renderer fixture. */
public class LegacyGraphicsDispatchTest implements Opcodes {
    public static final class RawGraphics { }
    public record WrappedGraphics(RawGraphics raw) {
        public static WrappedGraphics wrap(RawGraphics raw) { return new WrappedGraphics(raw); }
        public RawGraphics unwrap() { return raw; }
    }

    private static final String BASE = "ae2regression/NativeBase";
    private static final String SUB = "ae2regression/NativeWorkbench";
    private static final String RAW = Type.getInternalName(RawGraphics.class);
    private static final String WRAPPED = Type.getInternalName(WrappedGraphics.class);
    private static final String RAW_FG = "(L" + RAW + ";IIII)V";
    private static final String API_FG = "(L" + WRAPPED + ";IIII)V";

    private static JsonObject bridge(String nativeOwner, String graphicsType, String fixtureOwner) throws Exception {
        try (var stream = LegacyGraphicsDispatchTest.class.getResourceAsStream("/ae2-native-patches/" + nativeOwner + ".json")) {
            if (stream == null) throw new IllegalStateException("Missing production native patch: " + nativeOwner);
            var patch = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            for (var entry : patch.getAsJsonArray("methods")) {
                var method = entry.getAsJsonObject();
                if (method.get("name").getAsString().equals("drawFG")
                        && method.get("desc").getAsString().equals("(L" + graphicsType + ";IIII)V")) {
                    // Only types change; dispatch opcodes come from the release patch.
                    return JsonParser.parseString(method.toString()
                            .replace(nativeOwner, fixtureOwner)
                            .replace("appeng/client/gui/AEBaseScreen", BASE)
                            .replace("com/salts_inventory_update/api/client/gui/GuiGraphicsExtractor", WRAPPED)
                            .replace("net/minecraft/client/gui/GuiGraphics", RAW)).getAsJsonObject();
                }
            }
        }
        throw new IllegalStateException("Missing production foreground bridge: " + nativeOwner);
    }

    private static ClassNode fixture(String name, String parent) {
        var node = new ClassNode(); node.version = V17; node.access = ACC_PUBLIC | ACC_SUPER;
        node.name = name; node.superName = parent;
        var constructor = new MethodNode(ACC_PUBLIC, "<init>", "()V", null, null);
        constructor.instructions.add(new VarInsnNode(ALOAD, 0));
        constructor.instructions.add(new MethodInsnNode(INVOKESPECIAL, parent, "<init>", "()V", false));
        constructor.instructions.add(new InsnNode(RETURN)); node.methods.add(constructor);
        return node;
    }

    private static void addBridge(ClassNode node, JsonObject method) {
        var patch = new JsonObject(); patch.addProperty("class", node.name);
        patch.add("changes", new JsonArray()); patch.add("fields", new JsonArray());
        var methods = new JsonArray(); methods.add(method); patch.add("methods", methods);
        NativeClassPatch.apply(node, patch);
    }

    private static void increment(MethodNode method, String field) {
        method.instructions.add(new VarInsnNode(ALOAD, 0));
        method.instructions.add(new InsnNode(DUP));
        method.instructions.add(new FieldInsnNode(GETFIELD, BASE, field, "I"));
        method.instructions.add(new InsnNode(ICONST_1));
        method.instructions.add(new InsnNode(IADD));
        method.instructions.add(new FieldInsnNode(PUTFIELD, BASE, field, "I"));
    }

    private static final class FixtureLoader extends ClassLoader {
        FixtureLoader() { super(LegacyGraphicsDispatchTest.class.getClassLoader()); }
        Class<?> define(ClassNode node) {
            var writer = new ClassWriter(ClassWriter.COMPUTE_MAXS); node.accept(writer);
            byte[] bytes = writer.toByteArray(); return defineClass(node.name.replace('/', '.'), bytes, 0, bytes.length);
        }
    }

    @Test void nativeLabelsReachDesktopForegroundAndSuperDoesNotReenterBridge() throws Exception {
        var base = fixture(BASE, "java/lang/Object");
        base.fields.add(new FieldNode(ACC_PUBLIC, "nativeCalls", "I", null, null));
        base.fields.add(new FieldNode(ACC_PUBLIC, "desktopCalls", "I", null, null));
        var nativeForeground = new MethodNode(ACC_PUBLIC, "drawFG", RAW_FG, null, null);
        increment(nativeForeground, "nativeCalls"); nativeForeground.instructions.add(new InsnNode(RETURN));
        base.methods.add(nativeForeground);
        addBridge(base, bridge("appeng/client/gui/AEBaseScreen",
                "com/salts_inventory_update/api/client/gui/GuiGraphicsExtractor", BASE));

        var sub = fixture(SUB, BASE);
        addBridge(sub, bridge("appeng/client/gui/implementations/CellWorkbenchScreen",
                "net/minecraft/client/gui/GuiGraphics", SUB));
        var desktopForeground = new MethodNode(ACC_PUBLIC, "drawFG", API_FG, null, null);
        desktopForeground.instructions.add(new VarInsnNode(ALOAD, 0));
        desktopForeground.instructions.add(new VarInsnNode(ALOAD, 1));
        for (int local = 2; local <= 5; local++) desktopForeground.instructions.add(new VarInsnNode(ILOAD, local));
        desktopForeground.instructions.add(new MethodInsnNode(INVOKESPECIAL, BASE, "drawFG", API_FG, false));
        increment(desktopForeground, "desktopCalls"); desktopForeground.instructions.add(new InsnNode(RETURN));
        sub.methods.add(desktopForeground);

        var loader = new FixtureLoader(); var baseType = loader.define(base); var subType = loader.define(sub);
        var screen = subType.getConstructor().newInstance();
        subType.getMethod("drawFG", RawGraphics.class, int.class, int.class, int.class, int.class)
                .invoke(screen, new RawGraphics(), 1, 2, 3, 4);
        assertEquals(1, baseType.getField("nativeCalls").getInt(screen));
        assertEquals(1, baseType.getField("desktopCalls").getInt(screen));
    }
}
