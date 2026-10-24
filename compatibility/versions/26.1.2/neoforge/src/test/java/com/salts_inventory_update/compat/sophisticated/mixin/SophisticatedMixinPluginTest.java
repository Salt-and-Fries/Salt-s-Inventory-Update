package com.salts_inventory_update.compat.sophisticated.mixin;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;

class SophisticatedMixinPluginTest {
    private static ClassNode target(String descriptor) {
        ClassNode node = new ClassNode();
        node.fields.add(new FieldNode(Opcodes.ACC_PRIVATE, "resultListPositionDimensions", descriptor, null, null));
        return node;
    }

    @Test void olderPairLayoutSelectsOnlyThePairHook() {
        ClassNode node = target("Lcom/mojang/datafixers/util/Pair;");
        assertTrue(SophisticatedMixinPlugin.matchesCraftingBounds(node, "Lcom/mojang/datafixers/util/Pair;"));
        assertFalse(SophisticatedMixinPlugin.matchesCraftingBounds(node, "Lnet/minecraft/util/Tuple;"));
    }

    @Test void newerTupleLayoutSelectsOnlyTheTupleHook() {
        ClassNode node = target("Lnet/minecraft/util/Tuple;");
        assertTrue(SophisticatedMixinPlugin.matchesCraftingBounds(node, "Lnet/minecraft/util/Tuple;"));
        assertFalse(SophisticatedMixinPlugin.matchesCraftingBounds(node, "Lcom/mojang/datafixers/util/Pair;"));
    }

    @Test void unsupportedOrMissingLayoutsFailExplicitly() {
        assertThrows(IllegalStateException.class, () -> SophisticatedMixinPlugin.matchesCraftingBounds(target("Ljava/lang/Object;"), "Lnet/minecraft/util/Tuple;"));
        assertThrows(IllegalStateException.class, () -> SophisticatedMixinPlugin.matchesCraftingBounds(new ClassNode(), "Lnet/minecraft/util/Tuple;"));
    }
}
