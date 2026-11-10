package com.salts_inventory_update.server;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import static org.junit.jupiter.api.Assertions.*;

/** Prevent an absent optional loader overload falling back to vanilla openMenu. */
class OptionalMenuHookDescriptorTest {
    private static Object value(AnnotationNode annotation, String key) {
        for (int i = 0; i < annotation.values.size(); i += 2)
            if (key.equals(annotation.values.get(i))) return annotation.values.get(i + 1);
        return null;
    }
    @Test void optionalExtendedMenuHookUsesAnExactDescriptorWithoutPermissiveStringRemapping() throws Exception {
        try (var input = getClass().getResourceAsStream("/com/salts_inventory_update/mixin/server/ServerPlayerMixin.class")) {
            assertNotNull(input);
            var type = new ClassNode(); new ClassReader(input).accept(type, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG);
            var hook = type.methods.stream().filter(m -> m.name.endsWith("$extendedDesktopMenu")).findFirst();
            if (com.salts_inventory_update.VersionInfo.MINECRAFT_VERSION.equals("1.20.1")) {
                assertTrue(hook.isEmpty(), "Legacy Fabric does not expose the NeoForge overload"); return;
            }
            assertTrue(hook.isPresent());
            var inject = hook.orElseThrow().visibleAnnotations.stream()
                .filter(a -> a.desc.equals("Lorg/spongepowered/asm/mixin/injection/Inject;")).findFirst().orElseThrow();
            assertNull(value(inject, "method"), "String selectors allow unsafe descriptor fallback");
            assertEquals(false, value(inject, "remap")); assertEquals(0, value(inject, "require"));
            var targets = (List<?>) value(inject, "target"); assertEquals(1, targets.size());
            var target = (AnnotationNode) targets.get(0);
            assertEquals("openMenu", value(target, "value"));
            assertEquals(List.of(Type.getObjectType("net/minecraft/world/MenuProvider"),
                Type.getObjectType("java/util/function/Consumer")), value(target, "args"));
            assertEquals(Type.getObjectType("java/util/OptionalInt"), value(target, "ret"));
        }
    }
}
