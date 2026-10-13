package com.salts_inventory_update.compat.ae2.menu;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.salts_inventory_update.compat.ae2.mixin.NativeClassPatch;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

/** Executes the shipped construction bridge; only native parameter types are replaced by fixture types. */
public class DesktopBuilderBridgeTest implements Opcodes {
    private static final String BUILDER = "ae2builderregression/NativeBuilder";
    private static final String DESC = "(ILjava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;";

    private static Class<?> fixture() throws Exception {
        JsonObject bridge = null;
        try (var stream = DesktopBuilderBridgeTest.class.getResourceAsStream(
                "/ae2-native-patches/appeng/menu/implementations/MenuTypeBuilder.json")) {
            assertNotNull(stream);
            var patch = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            for (var entry : patch.getAsJsonArray("methods")) {
                if (entry.getAsJsonObject().get("name").getAsString().equals("createDesktopMenu")) {
                    bridge = JsonParser.parseString(entry.toString()
                            .replace("appeng/menu/implementations/MenuTypeBuilder", BUILDER)
                            .replace("net/minecraft/world/entity/player/Inventory", "java/lang/Object")
                            .replace("net/minecraft/network/FriendlyByteBuf", "java/lang/Object")
                            .replace("appeng/menu/AEBaseMenu", "java/lang/Object")).getAsJsonObject();
                }
            }
        }
        assertNotNull(bridge);
        var node = new ClassNode(); node.name = BUILDER; node.superName = "java/lang/Object";
        node.version = V17; node.access = ACC_PUBLIC | ACC_SUPER;
        node.fields.add(new FieldNode(ACC_PUBLIC | ACC_STATIC, "factory", "Ljava/lang/Runnable;", null, null));
        var ctor = new MethodNode(ACC_PUBLIC, "<init>", "()V", null, null);
        ctor.instructions.add(new VarInsnNode(ALOAD, 0));
        ctor.instructions.add(new MethodInsnNode(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false));
        ctor.instructions.add(new InsnNode(RETURN)); node.methods.add(ctor);
        var nativeFactory = new MethodNode(ACC_PRIVATE, "fromNetwork", DESC, null, null);
        nativeFactory.instructions.add(new FieldInsnNode(GETSTATIC, BUILDER, "factory", "Ljava/lang/Runnable;"));
        nativeFactory.instructions.add(new MethodInsnNode(INVOKEINTERFACE, "java/lang/Runnable", "run", "()V", true));
        nativeFactory.instructions.add(new InsnNode(ACONST_NULL));
        nativeFactory.instructions.add(new InsnNode(ARETURN)); node.methods.add(nativeFactory);
        var patch = new JsonObject(); patch.addProperty("class", BUILDER);
        patch.add("fields", new JsonArray()); patch.add("changes", new JsonArray());
        var methods = new JsonArray(); methods.add(bridge); patch.add("methods", methods);
        NativeClassPatch.apply(node, patch);
        var writer = new ClassWriter(ClassWriter.COMPUTE_MAXS); node.accept(writer);
        byte[] bytes = writer.toByteArray();
        return new ClassLoader(DesktopBuilderBridgeTest.class.getClassLoader()) {
            Class<?> define() { return defineClass(BUILDER.replace('/', '.'), bytes, 0, bytes.length); }
        }.define();
    }

    private static Object construct(Class<?> type, Runnable factory) throws Exception {
        type.getField("factory").set(null, factory);
        return type.getMethod("createDesktopMenu", int.class, Object.class, Object.class)
                .invoke(type.getConstructor().newInstance(), 7, null, null);
    }

    @Test void nativeFactoryRunsInsideDetachedScopeAndSuccessRestoresIt() throws Exception {
        assertFalse(DesktopMenuConstructionScope.detached());
        assertNull(construct(fixture(), () -> assertTrue(DesktopMenuConstructionScope.detached())));
        assertFalse(DesktopMenuConstructionScope.detached());
    }

    @Test void nativeFactoryFailureRestoresScopeBeforePropagating() throws Exception {
        var type = fixture(); var failure = new IllegalStateException("missing native host");
        var thrown = assertThrows(InvocationTargetException.class,
                () -> construct(type, () -> { assertTrue(DesktopMenuConstructionScope.detached()); throw failure; }));
        assertSame(failure, thrown.getCause());
        assertFalse(DesktopMenuConstructionScope.detached());
    }

    @Test void nestedConstructionRestoresItsOuterOwner() throws Exception {
        int previous = DesktopMenuConstructionScope.begin();
        try {
            assertNull(construct(fixture(), () -> assertTrue(DesktopMenuConstructionScope.detached())));
            assertTrue(DesktopMenuConstructionScope.detached());
        } finally {
            DesktopMenuConstructionScope.restore(previous);
        }
        assertFalse(DesktopMenuConstructionScope.detached());
    }
}
