package com.salts_inventory_update.compat.ae2;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import static org.junit.jupiter.api.Assertions.*;

/** Mixin validates target kind before the optional plugin can filter it. */
class LegacyPacketMixinTargetsTest {
    @Test void nativePacketClassMixinNeverDeclaresTheSharedNetworkHandlerInterface() throws Exception {
        String path = "/com/salts_inventory_update/compat/ae2/extended/mixin/ExtendedAeNativePacketsMixin.class";
        try (var input = getClass().getResourceAsStream(path)) {
            assertNotNull(input);
            var node = new ClassNode();
            new ClassReader(input).accept(node, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG);
            var mixin = node.invisibleAnnotations.stream()
                .filter(a -> a.desc.equals("Lorg/spongepowered/asm/mixin/Mixin;")).findFirst().orElseThrow();
            List<String> targets = new ArrayList<>();
            for (int i = 0; i < mixin.values.size(); i += 2)
                if (mixin.values.get(i).equals("targets"))
                    for (Object target : (List<?>) mixin.values.get(i + 1)) targets.add((String) target);
            assertEquals(List.of("appeng.core.sync.network.ClientNetworkHandler"), targets);
        }
    }
}
