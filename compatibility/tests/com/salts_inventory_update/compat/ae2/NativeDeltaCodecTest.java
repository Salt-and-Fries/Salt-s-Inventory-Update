package com.salts_inventory_update.compat.ae2;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.salts_inventory_update.compat.ae2.mixin.NativeClassPatch;
import com.salts_inventory_update.compat.ae2.mixin.NativeInstructionCodec;
import java.util.HashMap;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ConstantDynamic;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import static org.junit.jupiter.api.Assertions.*;

final class NativeDeltaCodecTest {
    @Test void forgeCompressedFramesVerifyAgainstExpandedContractsWithoutReplacingNativeInstructions() {
        var writer=new org.objectweb.asm.ClassWriter(org.objectweb.asm.ClassWriter.COMPUTE_FRAMES);
        writer.visit(Opcodes.V17,Opcodes.ACC_PUBLIC,"example/Native",null,"java/lang/Object",null);
        var code=writer.visitMethod(Opcodes.ACC_PUBLIC|Opcodes.ACC_STATIC,"value","(I)I",null,null);
        code.visitCode();var positive=new org.objectweb.asm.Label();
        code.visitVarInsn(Opcodes.ILOAD,0);code.visitJumpInsn(Opcodes.IFNE,positive);
        code.visitInsn(Opcodes.ICONST_0);code.visitInsn(Opcodes.IRETURN);
        code.visitLabel(positive);code.visitVarInsn(Opcodes.ILOAD,0);code.visitInsn(Opcodes.IRETURN);
        code.visitMaxs(0,0);code.visitEnd();writer.visitEnd();
        var expected=new ClassNode();var actual=new ClassNode();
        new org.objectweb.asm.ClassReader(writer.toByteArray()).accept(expected,org.objectweb.asm.ClassReader.EXPAND_FRAMES);
        new org.objectweb.asm.ClassReader(writer.toByteArray()).accept(actual,0);
        var instructions=java.util.Arrays.stream(actual.methods.get(0).instructions.toArray())
                .filter(n -> n.getOpcode()>=0).toArray(AbstractInsnNode[]::new);
        var patch=incrementPatch(expected);
        patch.getAsJsonArray("changes").get(0).getAsJsonObject().add("edits",new JsonArray());
        NativeClassPatch.apply(actual,patch);
        assertEquals(NativeClassPatch.method(expected.methods.get(0)).get("instructions"),
                NativeClassPatch.method(actual.methods.get(0)).get("instructions"));
        var retained=java.util.Arrays.stream(actual.methods.get(0).instructions.toArray())
                .filter(n -> n.getOpcode()>=0).toArray(AbstractInsnNode[]::new);
        assertEquals(instructions.length,retained.length);
        for (int i=0;i<instructions.length;i++) assertSame(instructions[i],retained[i]);
    }

    @Test void nativeConstantsPreserveTypesAndFloatingPointBits() {
        Object[] values = {null, "native", true, (byte)2, (short)3, 'c', 23, 4L, -0.0F, Float.NaN, Double.NEGATIVE_INFINITY,
                Type.getType("Ljava/lang/String;"), new Handle(Opcodes.H_INVOKESTATIC,"example/Native","run","()V",false)};
        for (Object value : values) {
            Object decoded = NativeInstructionCodec.constant(NativeInstructionCodec.constant(value));
            if (value instanceof Float a) assertEquals(Float.floatToRawIntBits(a),Float.floatToRawIntBits((Float)decoded));
            else if (value instanceof Double a) assertEquals(Double.doubleToRawLongBits(a),Double.doubleToRawLongBits((Double)decoded));
            else assertEquals(value,decoded);
        }
    }

    @Test void dynamicConstantsRetainTheirBootstrapAndArguments() {
        var bootstrap = new Handle(Opcodes.H_INVOKESTATIC,"example/Native","bootstrap","()Ljava/lang/Object;",false);
        var constant = new ConstantDynamic("value","Ljava/lang/Object;",bootstrap,"key",5L);
        assertEquals(constant,NativeInstructionCodec.constant(NativeInstructionCodec.constant(constant)));
    }

    @Test void labelsFramesAndSwitchesRoundTripWithoutDefiningNativeClasses() {
        var method = new MethodNode(Opcodes.ACC_PUBLIC|Opcodes.ACC_STATIC,"value","(I)I",null,null);
        var start = new LabelNode(); var end = new LabelNode();
        method.instructions.add(start);
        method.instructions.add(new VarInsnNode(Opcodes.ILOAD,0));
        method.instructions.add(new LookupSwitchInsnNode(end,new int[]{2},new LabelNode[]{end}));
        method.instructions.add(end);
        method.instructions.add(new FrameNode(Opcodes.F_CHOP,1,null,0,null));
        method.instructions.add(new InsnNode(Opcodes.ICONST_0));
        method.instructions.add(new InsnNode(Opcodes.IRETURN));
        var encoded = NativeInstructionCodec.instructions(method,NativeInstructionCodec.labels(method,"o"));
        var reconstructed = new MethodNode(); var labels = new HashMap<String,LabelNode>();
        for (var instruction : encoded) reconstructed.instructions.add(NativeInstructionCodec.instruction(instruction.getAsJsonObject(),labels));
        assertEquals(encoded,NativeInstructionCodec.instructions(reconstructed,NativeInstructionCodec.labels(reconstructed,"o")));
    }

    private static ClassNode nativeClass() {
        var type = new ClassNode(); type.name="example/Native";
        var method = new MethodNode(Opcodes.ACC_PUBLIC|Opcodes.ACC_STATIC,"value","(I)I",null,null);
        method.instructions.add(new VarInsnNode(Opcodes.ILOAD,0));
        method.instructions.add(new InsnNode(Opcodes.IRETURN));
        method.maxLocals=1; method.maxStack=1;
        type.methods.add(method); return type;
    }

    private static JsonObject incrementPatch(ClassNode type) {
        var patch = new JsonObject(); patch.addProperty("class",type.name);
        patch.add("fields",new JsonArray()); patch.add("methods",new JsonArray());
        var change = new JsonObject(); change.addProperty("name","value"); change.addProperty("desc","(I)I");
        change.addProperty("original",NativeClassPatch.fingerprint(NativeClassPatch.method(type.methods.get(0)).get("instructions")));
        change.addProperty("maxStack",2); change.addProperty("maxLocals",1); change.add("handlers",new JsonArray());
        var edit = new JsonObject(); edit.addProperty("from",1); edit.addProperty("to",1);
        var additions = new JsonArray();
        additions.add(NativeInstructionCodec.instruction(new InsnNode(Opcodes.ICONST_1),new HashMap<>()));
        additions.add(NativeInstructionCodec.instruction(new InsnNode(Opcodes.IADD),new HashMap<>()));
        edit.add("insert",additions); var edits = new JsonArray(); edits.add(edit); change.add("edits",edits);
        var changes = new JsonArray(); changes.add(change); patch.add("changes",changes); return patch;
    }

    @Test void integrationDeltaRetainsTheOriginalNativeInstructionObjects() {
        var type = nativeClass(); var load = type.methods.get(0).instructions.getFirst();
        var returnInstruction = type.methods.get(0).instructions.getLast();
        NativeClassPatch.apply(type,incrementPatch(type));
        assertSame(load,type.methods.get(0).instructions.getFirst());
        assertSame(returnInstruction,type.methods.get(0).instructions.getLast());
        assertEquals(4,type.methods.get(0).instructions.size());
    }

    @Test void unsupportedNativeCodeIsRejectedBeforeAnyMutation() {
        var type = nativeClass(); var patch = incrementPatch(type);
        type.methods.get(0).instructions.insert(new InsnNode(Opcodes.NOP));
        var before = NativeClassPatch.method(type.methods.get(0));
        assertThrows(IllegalStateException.class,()->NativeClassPatch.apply(type,patch));
        assertEquals(before,NativeClassPatch.method(type.methods.get(0)));
    }

    @Test void undefinedBranchSwitchAndFrameLabelsCannotBeSerialized() {
        var absent = new LabelNode();
        AbstractInsnNode[] references = {
            new JumpInsnNode(Opcodes.GOTO, absent),
            new TableSwitchInsnNode(0, 0, absent, absent),
            new LookupSwitchInsnNode(absent, new int[] {1}, new LabelNode[] {absent}),
            new FrameNode(Opcodes.F_NEW, 1, new Object[] {absent}, 0, new Object[0])
        };
        for (var reference : references) {
            var method = new MethodNode(); method.instructions.add(reference);
            var error = assertThrows(IllegalArgumentException.class,
                () -> NativeInstructionCodec.instructions(method, NativeInstructionCodec.labels(method,"o")));
            assertTrue(error.getMessage().contains("undefined label"));
        }
    }

    @Test void duplicateLabelDefinitionsAreRejectedBeforeNativeMutation() {
        for (boolean addedMethod : new boolean[] {false, true}) {
            var type = nativeClass(); var patch = incrementPatch(type);
            var before = NativeClassPatch.method(type.methods.get(0));
            var label = new JsonObject(); label.addProperty("type",AbstractInsnNode.LABEL);
            label.addProperty("op",-1); label.addProperty("label","collision");
            JsonArray instructions;
            if (addedMethod) {
                var method = before.deepCopy(); method.addProperty("name","addedValue");
                patch.getAsJsonArray("methods").add(method); instructions = method.getAsJsonArray("instructions");
            } else {
                instructions = patch.getAsJsonArray("changes").get(0).getAsJsonObject()
                        .getAsJsonArray("edits").get(0).getAsJsonObject().getAsJsonArray("insert");
            }
            instructions.add(label); instructions.add(label.deepCopy());
            var error = assertThrows(IllegalArgumentException.class,()->NativeClassPatch.apply(type,patch));
            assertTrue(error.getMessage().contains("duplicate label"));
            assertEquals(1,type.methods.size()); assertTrue(type.fields.isEmpty());
            assertEquals(before,NativeClassPatch.method(type.methods.get(0)));
        }
    }

    @Test void nativeSynchronizationAnnotationsSurvivePatchingAndClassFileSerialization() {
        var type = nativeClass(); type.version=Opcodes.V17; type.access=Opcodes.ACC_PUBLIC; type.superName="java/lang/Object";
        var field = new FieldNode(Opcodes.ACC_PUBLIC,"statusReady","Z",null,null);
        var sync = new AnnotationNode("Lappeng/menu/guisync/GuiSync;"); sync.visit("value",(short)2);
        field.visibleAnnotations = new java.util.ArrayList<>(java.util.List.of(sync));
        var hint = new AnnotationNode("Lexample/Contract;");
        hint.visit("enabled",true); hint.visit("flags",new byte[]{1,2});
        hint.visit("mode",new String[]{"Lexample/Mode;","READY"});
        var nested = new AnnotationNode("Lexample/Nested;"); nested.visit("type",Type.getType("Ljava/lang/String;"));
        hint.visit("nested",nested);
        field.invisibleAnnotations = new java.util.ArrayList<>(java.util.List.of(hint));
        var patch = incrementPatch(type); patch.getAsJsonArray("fields").add(NativeClassPatch.field(field));
        NativeClassPatch.apply(type,patch);
        var writer = new org.objectweb.asm.ClassWriter(0); type.accept(writer);
        var restored = new ClassNode(); new org.objectweb.asm.ClassReader(writer.toByteArray()).accept(restored,0);
        var actual = restored.fields.get(0);
        assertEquals("Lappeng/menu/guisync/GuiSync;",actual.visibleAnnotations.get(0).desc);
        assertEquals(java.util.List.of("value",(short)2),actual.visibleAnnotations.get(0).values);
        assertEquals(NativeClassPatch.field(field),NativeClassPatch.field(actual));
    }

    @Test void interfaceAdditionSurvivesClassFileSerializationAndJvmTypeChecks() {
        var type = nativeClass(); type.version=Opcodes.V17; type.access=Opcodes.ACC_PUBLIC; type.superName="java/lang/Object";
        var patch = incrementPatch(type); var contract = new JsonObject();
        contract.add("original",new JsonArray()); var added = new JsonArray(); added.add("java/io/Serializable");
        contract.add("added",added); patch.add("interfaces",contract);
        NativeClassPatch.apply(type,patch);
        var writer = new org.objectweb.asm.ClassWriter(0); type.accept(writer);
        var loader = new ClassLoader() { Class<?> define(byte[] bytes) { return defineClass(null,bytes,0,bytes.length); } };
        assertTrue(java.io.Serializable.class.isAssignableFrom(loader.define(writer.toByteArray())));
    }

    @Test void mismatchedInterfaceContractIsRejectedBeforeMemberMutation() {
        var type = nativeClass(); type.interfaces.add("java/io/Serializable");
        var patch = incrementPatch(type); var contract = new JsonObject(); contract.add("original",new JsonArray());
        var added = new JsonArray(); added.add("java/lang/Cloneable"); contract.add("added",added); patch.add("interfaces",contract);
        var before = NativeClassPatch.method(type.methods.get(0));
        assertThrows(IllegalStateException.class,()->NativeClassPatch.apply(type,patch));
        assertEquals(java.util.List.of("java/io/Serializable"),type.interfaces);
        assertEquals(before,NativeClassPatch.method(type.methods.get(0)));
    }
}
