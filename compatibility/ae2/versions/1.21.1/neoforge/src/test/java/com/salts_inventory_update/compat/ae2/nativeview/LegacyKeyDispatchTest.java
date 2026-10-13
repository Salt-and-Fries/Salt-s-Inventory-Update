package com.salts_inventory_update.compat.ae2.nativeview;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.salts_inventory_update.api.client.input.KeyEvent;
import com.salts_inventory_update.compat.ae2.mixin.NativeClassPatch;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

/** Executes the shipped terminal bridges, including their superclass dispatch. */
public class LegacyKeyDispatchTest implements Opcodes {
    private static final String PARENT = "ae2keyregression/NativeParent";
    private static final String BASE = "ae2keyregression/NativeBase";
    private static final String TERMINAL = "ae2keyregression/NativeTerminal";
    private static final String EVENT = Type.getInternalName(KeyEvent.class);
    private static final String RECORDER = Type.getInternalName(LegacyKeyDispatchTest.class);
    private static final String TYPED = "(L" + EVENT + ";)Z";
    private static String route;
    private static KeyEvent received;
    private static int calls;

    public static boolean capture(String value, KeyEvent event) {
        route = value; received = event; calls++; return true;
    }
    public static boolean captureRaw(String value, int key, int scan, int modifiers) {
        return capture(value, new KeyEvent(key, scan, modifiers));
    }

    private static JsonObject productionBridge(String owner, String name, String descriptor) throws Exception {
        try (var stream = LegacyKeyDispatchTest.class.getResourceAsStream("/ae2-native-patches/" + owner + ".json")) {
            assertNotNull(stream);
            var patch = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            for (var entry : patch.getAsJsonArray("methods")) {
                var method = entry.getAsJsonObject();
                if (method.get("name").getAsString().equals(name) && method.get("desc").getAsString().equals(descriptor))
                    return JsonParser.parseString(method.toString()
                            .replace("appeng/client/gui/me/common/MEStorageScreen", TERMINAL)
                            .replace("appeng/client/gui/AEBaseScreen", BASE)
                            .replace("net/minecraft/client/gui/screens/inventory/AbstractContainerScreen", PARENT)).getAsJsonObject();
            }
        }
        throw new IllegalStateException("Missing production key bridge: " + owner + "." + name);
    }

    private static ClassNode type(String name, String parent) {
        var node = new ClassNode(); node.name = name; node.superName = parent;
        node.version = V17; node.access = ACC_PUBLIC | ACC_SUPER;
        var ctor = new MethodNode(ACC_PUBLIC, "<init>", "()V", null, null);
        ctor.instructions.add(new VarInsnNode(ALOAD, 0));
        ctor.instructions.add(new MethodInsnNode(INVOKESPECIAL, parent, "<init>", "()V", false));
        ctor.instructions.add(new InsnNode(RETURN)); node.methods.add(ctor); return node;
    }

    private static void add(ClassNode node, JsonObject method) {
        var patch = new JsonObject(); patch.addProperty("class", node.name);
        patch.add("fields", new JsonArray()); patch.add("changes", new JsonArray());
        var methods = new JsonArray(); methods.add(method); patch.add("methods", methods); NativeClassPatch.apply(node, patch);
    }

    private static MethodNode rawCapture(String label) {
        var method = new MethodNode(ACC_PUBLIC, "keyPressed", "(III)Z", null, null);
        method.instructions.add(new LdcInsnNode(label));
        for (int i = 1; i <= 3; i++) method.instructions.add(new VarInsnNode(ILOAD, i));
        method.instructions.add(new MethodInsnNode(INVOKESTATIC, RECORDER, "captureRaw", "(Ljava/lang/String;III)Z", false));
        method.instructions.add(new InsnNode(IRETURN)); return method;
    }

    private static final class FixtureLoader extends ClassLoader {
        FixtureLoader() { super(LegacyKeyDispatchTest.class.getClassLoader()); }
        Class<?> define(ClassNode node) {
            var writer = new ClassWriter(ClassWriter.COMPUTE_MAXS); node.accept(writer);
            byte[] bytes = writer.toByteArray(); return defineClass(node.name.replace('/', '.'), bytes, 0, bytes.length);
        }
    }

    private static Class<?> terminalType() throws Exception {
        var parent = type(PARENT, "java/lang/Object"); parent.methods.add(rawCapture("parent"));
        var base = type(BASE, PARENT); base.methods.add(rawCapture("native"));
        base.fields.add(new FieldNode(ACC_PUBLIC, "hosted", "Z", null, null));
        var hosted = new MethodNode(ACC_PUBLIC, "isDesktopHosted", "()Z", null, null);
        hosted.instructions.add(new VarInsnNode(ALOAD, 0)); hosted.instructions.add(new FieldInsnNode(GETFIELD, BASE, "hosted", "Z"));
        hosted.instructions.add(new InsnNode(IRETURN)); base.methods.add(hosted);
        var desktop = new MethodNode(ACC_PUBLIC, "keyPressed", TYPED, null, null);
        desktop.instructions.add(new LdcInsnNode("desktop")); desktop.instructions.add(new VarInsnNode(ALOAD, 1));
        desktop.instructions.add(new MethodInsnNode(INVOKESTATIC, RECORDER, "capture", "(Ljava/lang/String;L" + EVENT + ";)Z", false));
        desktop.instructions.add(new InsnNode(IRETURN)); base.methods.add(desktop);
        add(base, productionBridge("appeng/client/gui/AEBaseScreen", "siu$desktopKeyPressed", "(III)Z"));
        var parentBridge = productionBridge("appeng/client/gui/AEBaseScreen", "siu$nativeKeyPressed", TYPED);
        parentBridge.addProperty("name", "nativeParent"); parentBridge.addProperty("access", ACC_PUBLIC); add(base, parentBridge);
        var terminal = type(TERMINAL, BASE);
        add(terminal, productionBridge("appeng/client/gui/me/common/MEStorageScreen", "keyPressed", TYPED));
        var raw = new MethodNode(ACC_PUBLIC, "keyPressed", "(III)Z", null, null);
        raw.instructions.add(new VarInsnNode(ALOAD, 0));
        for (int i = 1; i <= 3; i++) raw.instructions.add(new VarInsnNode(ILOAD, i));
        raw.instructions.add(new MethodInsnNode(INVOKESPECIAL, BASE, "siu$desktopKeyPressed", "(III)Z", false));
        raw.instructions.add(new InsnNode(IRETURN)); terminal.methods.add(raw);
        var loader = new FixtureLoader(); loader.define(parent); loader.define(base); return loader.define(terminal);
    }

    @Test void terminalDispatchRunsOnceAndPreservesNativeKeyValues() throws Exception {
        var type = terminalType(); var event = new KeyEvent(257, 777, 3);
        for (boolean hosted : new boolean[] {true, false}) {
            var screen = type.getConstructor().newInstance(); type.getField("hosted").setBoolean(screen, hosted); calls = 0;
            assertEquals(true, type.getMethod("keyPressed", KeyEvent.class).invoke(screen, event));
            assertEquals(hosted ? "desktop" : "native", route); assertEquals(event, received); assertEquals(1, calls);
        }
    }

    @Test void explicitSuperclassBridgeBypassesNativeOverrides() throws Exception {
        var type = terminalType(); var event = new KeyEvent(65, 19, 2); calls = 0;
        assertEquals(true, type.getMethod("nativeParent", KeyEvent.class).invoke(type.getConstructor().newInstance(), event));
        assertEquals("parent", route); assertEquals(event, received); assertEquals(1, calls);
    }
}
