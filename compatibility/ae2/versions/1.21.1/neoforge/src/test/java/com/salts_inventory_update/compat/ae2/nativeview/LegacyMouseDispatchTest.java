package com.salts_inventory_update.compat.ae2.nativeview;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.salts_inventory_update.api.client.input.MouseButtonEvent;
import com.salts_inventory_update.compat.ae2.mixin.NativeClassPatch;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import net.minecraft.client.gui.components.events.GuiEventListener;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

/** Ensures the shipped hosted-click bridge reaches native terminal overrides. */
public class LegacyMouseDispatchTest implements Opcodes {
    public static class NativeTerminal implements GuiEventListener {
        MouseButtonEvent received;
        @Override public void setFocused(boolean focused) { }
        @Override public boolean isFocused() { return false; }
        @Override public boolean mouseClicked(double x, double y, int button) {
            received = new MouseButtonEvent(x, y, button); return true;
        }
        public boolean desktopMouseClicked(MouseButtonEvent event, boolean doubleClick) {
            throw new AssertionError("The public bridge bypassed the native terminal override");
        }
    }

    private static final class FixtureLoader extends ClassLoader {
        FixtureLoader() { super(LegacyMouseDispatchTest.class.getClassLoader()); }
        Class<?> define(ClassNode node) {
            var writer = new ClassWriter(ClassWriter.COMPUTE_MAXS); node.accept(writer);
            byte[] bytes = writer.toByteArray(); return defineClass(node.name.replace('/', '.'), bytes, 0, bytes.length);
        }
    }

    @Test void hostedMouseEntryUsesOriginalNativeSubclassHandler() throws Exception {
        JsonObject bridge = null;
        try (var stream = getClass().getResourceAsStream("/ae2-native-patches/appeng/client/gui/AEBaseScreen.json")) {
            assertNotNull(stream);
            var patch = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            for (var value : patch.getAsJsonArray("methods")) {
                if (value.getAsJsonObject().get("name").getAsString().equals("siu$desktopMouseClicked"))
                    bridge = value.getAsJsonObject();
            }
        }
        assertNotNull(bridge);
        var node = new ClassNode(); node.name = "ae2regression/HostedNativeTerminal";
        node.superName = Type.getInternalName(NativeTerminal.class); node.version = V17; node.access = ACC_PUBLIC | ACC_SUPER;
        var constructor = new MethodNode(ACC_PUBLIC, "<init>", "()V", null, null);
        constructor.instructions.add(new VarInsnNode(ALOAD, 0));
        constructor.instructions.add(new MethodInsnNode(INVOKESPECIAL, node.superName, "<init>", "()V", false));
        constructor.instructions.add(new InsnNode(RETURN)); node.methods.add(constructor);
        var patch = new JsonObject(); patch.addProperty("class", node.name);
        patch.add("changes", new JsonArray()); patch.add("fields", new JsonArray());
        var methods = new JsonArray(); methods.add(bridge); patch.add("methods", methods); NativeClassPatch.apply(node, patch);
        var type = new FixtureLoader().define(node); var terminal = (NativeTerminal) type.getConstructor().newInstance();
        var event = new MouseButtonEvent(14.5, 38.25, 1);
        assertEquals(true, type.getMethod("siu$desktopMouseClicked", MouseButtonEvent.class, boolean.class)
                .invoke(terminal, event, true));
        assertEquals(event, terminal.received);
    }
}
