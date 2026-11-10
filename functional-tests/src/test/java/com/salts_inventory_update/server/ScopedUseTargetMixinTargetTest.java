package com.salts_inventory_update.server;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import static org.junit.jupiter.api.Assertions.*;

/** Validate the scoped redirect against the real vanilla call in every mapped version. */
class ScopedUseTargetMixinTargetTest {
    private static ClassNode read(String resource) throws Exception {
        try (var input = ScopedUseTargetMixinTargetTest.class.getResourceAsStream(resource)) {
            assertNotNull(input, resource);
            var type = new ClassNode(); new ClassReader(input).accept(type, ClassReader.SKIP_DEBUG); return type;
        }
    }
    private static Object value(AnnotationNode annotation, String key) {
        for (int i=0;i<annotation.values.size();i+=2) if(key.equals(annotation.values.get(i))) return annotation.values.get(i+1);
        return null;
    }
    @Test void redirectMatchesExactlyOneRealGameModeInvocation() throws Exception {
        var mixin = read("/com/salts_inventory_update/mixin/server/ServerGamePacketListenerImplMixin.class");
        var hook = mixin.methods.stream().filter(m->m.name.endsWith("$scopedBlockTarget")).findFirst().orElseThrow();
        var annotation = hook.visibleAnnotations.stream().filter(a->a.desc.equals("Lorg/spongepowered/asm/mixin/injection/Redirect;")).findFirst().orElseThrow();
        assertEquals(List.of("handleUseItemOn"),value(annotation,"method"));
        var at = (AnnotationNode)value(annotation,"at"); assertEquals("INVOKE",value(at,"value"));
        var expected = value(at,"target");
        var nativeType=read("/net/minecraft/server/network/ServerGamePacketListenerImpl.class");
        long matches=0;
        for(var method:nativeType.methods) if(method.name.equals("handleUseItemOn"))
            for(var instruction:method.instructions) if(instruction instanceof MethodInsnNode call
                && expected.equals("L"+call.owner+";"+call.name+call.desc)) matches++;
        assertEquals(1,matches,"The scoped hook must match the actual upstream call without optional fallback");
    }
    @Test void packetHooksCannotSetOrClearThePendingTargetOutsideTheScope() throws Exception {
        var mixin=read("/com/salts_inventory_update/mixin/server/ServerGamePacketListenerImplMixin.class");
        int scopedCalls=0;
        for(var method:mixin.methods) for(var instruction:method.instructions) if(instruction instanceof MethodInsnNode call
            && call.owner.equals("com/salts_inventory_update/server/DesktopContainerSessions")) {
            assertNotEquals("captureUseTarget",call.name,"Packet HEAD runs before validation and may be cancelled");
            assertNotEquals("clearUseTarget",call.name,"A RETURN injection cannot clean every cancelled path");
            if(call.name.equals("withUseTarget")) scopedCalls++;
        }
        assertEquals(1,scopedCalls);
    }
}
