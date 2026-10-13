package com.salts_inventory_update.compat.ae2.mixin;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import org.objectweb.asm.tree.*;

/** Applies bounded, verified instruction deltas to native AE2; no native class is bundled. */
public final class NativeClassPatch {
    private NativeClassPatch() { }

    /** Forge supplies compressed frames to plugins; contracts use expanded frames. */
    private static void expandFrames(ClassNode target) {
        boolean compressed=target.methods.stream().anyMatch(m -> java.util.Arrays.stream(m.instructions.toArray())
                .anyMatch(n -> n instanceof FrameNode f && f.type!=org.objectweb.asm.Opcodes.F_NEW));
        if (!compressed) return;
        var writer=new org.objectweb.asm.ClassWriter(0);target.accept(writer);
        var expanded=new ClassNode();
        new org.objectweb.asm.ClassReader(writer.toByteArray()).accept(expanded,org.objectweb.asm.ClassReader.EXPAND_FRAMES);
        for (var original:target.methods) {
            var normalized=find(expanded,original.name,original.desc);
            var frames=new ArrayList<FrameNode>();
            var labels=new HashMap<Integer,LabelNode>();int position=0;
            for (var n:original.instructions) {
                if (n instanceof FrameNode f) frames.add(f);
                if (n instanceof LabelNode l) labels.putIfAbsent(position,l);
                if (n.getOpcode()>=0) position++;
            }
            var translated=new HashMap<LabelNode,LabelNode>();position=0;
            for (var n:normalized.instructions) {
                if (n instanceof LabelNode l) translated.put(l,labels.get(position));
                if (n.getOpcode()>=0) position++;
            }
            int index=0;
            for (var n:normalized.instructions) if (n instanceof FrameNode f) {
                if (index>=frames.size()) throw new IllegalStateException("Native frame count changed: "+target.name+"."+original.name);
                var destination=frames.get(index++);destination.type=org.objectweb.asm.Opcodes.F_NEW;
                destination.local=frameValues(f.local,translated);destination.stack=frameValues(f.stack,translated);
            }
            if (index!=frames.size()) throw new IllegalStateException("Native frame count changed: "+target.name+"."+original.name);
        }
    }
    private static List<Object> frameValues(List<Object> values,Map<LabelNode,LabelNode> labels) {
        if (values==null) return null;
        var result=new ArrayList<Object>();
        for (Object value:values) {
            if (value instanceof LabelNode label) {
                value=labels.get(label);
                if (value==null) throw new IllegalStateException("Unmapped native uninitialized frame label");
            }
            result.add(value);
        }
        return result;
    }

    private static String string(JsonObject in, String name) {
        return !in.has(name) || in.get(name).isJsonNull() ? null : in.get(name).getAsString();
    }

    public static String fingerprint(JsonElement instructions) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(instructions.toString().getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException impossible) {
            throw new AssertionError(impossible);
        }
    }

    public static JsonObject method(MethodNode method) {
        return method(method, NativeInstructionCodec.labels(method, "o"));
    }

    public static JsonObject method(MethodNode method, Map<LabelNode, String> labels) {
        var out = new JsonObject(); out.addProperty("name", method.name); out.addProperty("desc", method.desc);
        out.addProperty("access", method.access); out.addProperty("signature", method.signature);
        var exceptions = new JsonArray(); if (method.exceptions != null) for (String value : method.exceptions) exceptions.add(value);
        out.add("exceptions", exceptions); out.addProperty("maxStack", method.maxStack); out.addProperty("maxLocals", method.maxLocals);
        out.add("instructions", NativeInstructionCodec.instructions(method, labels));
        var handlers = new JsonArray();
        for (var handler : method.tryCatchBlocks) {
            var entry = new JsonObject(); entry.addProperty("start", labels.get(handler.start));
            entry.addProperty("end", labels.get(handler.end)); entry.addProperty("handler", labels.get(handler.handler));
            entry.addProperty("type", handler.type); handlers.add(entry);
        }
        out.add("handlers", handlers);
        out.add("visibleAnnotations", NativeInstructionCodec.annotations(method.visibleAnnotations));
        out.add("invisibleAnnotations", NativeInstructionCodec.annotations(method.invisibleAnnotations));
        return out;
    }

    private static MethodNode method(JsonObject input) {
        verifyUniqueLabels(input.getAsJsonArray("instructions"));
        var exceptions = input.getAsJsonArray("exceptions");
        String[] names = new String[exceptions.size()]; for (int i = 0; i < names.length; i++) names[i] = exceptions.get(i).getAsString();
        var out = new MethodNode(input.get("access").getAsInt(), string(input, "name"), string(input, "desc"), string(input, "signature"), names);
        var labels = new HashMap<String, LabelNode>();
        for (var instruction : input.getAsJsonArray("instructions")) out.instructions.add(NativeInstructionCodec.instruction(instruction.getAsJsonObject(), labels));
        metadata(out, input, labels);
        out.visibleAnnotations = NativeInstructionCodec.annotations(input.getAsJsonArray("visibleAnnotations"));
        out.invisibleAnnotations = NativeInstructionCodec.annotations(input.getAsJsonArray("invisibleAnnotations"));
        return out;
    }

    private static void metadata(MethodNode method, JsonObject input, Map<String, LabelNode> labels) {
        method.maxStack = input.get("maxStack").getAsInt(); method.maxLocals = input.get("maxLocals").getAsInt();
        method.tryCatchBlocks.clear();
        for (var element : input.getAsJsonArray("handlers")) {
            var handler = element.getAsJsonObject();
            var start = labels.get(string(handler, "start")); var end = labels.get(string(handler, "end"));
            var target = labels.get(string(handler, "handler"));
            if (start == null || end == null || target == null) throw new IllegalArgumentException("Invalid native exception handler");
            method.tryCatchBlocks.add(new TryCatchBlockNode(start, end, target, string(handler, "type")));
        }
        method.localVariables = null;
        method.visibleLocalVariableAnnotations = null;
        method.invisibleLocalVariableAnnotations = null;
    }

    public static JsonObject field(FieldNode field) {
        var out = new JsonObject(); out.addProperty("name", field.name); out.addProperty("desc", field.desc);
        out.addProperty("access", field.access); out.addProperty("signature", field.signature);
        out.add("value", field.value == null ? JsonNull.INSTANCE : NativeInstructionCodec.constant(field.value));
        out.add("visibleAnnotations", NativeInstructionCodec.annotations(field.visibleAnnotations));
        out.add("invisibleAnnotations", NativeInstructionCodec.annotations(field.invisibleAnnotations));
        return out;
    }

    /** Verify every original member before mutation, so unsupported AE2 cannot receive a partial patch. */
    public static void verify(ClassNode target, JsonObject patch) {
        expandFrames(target);
        if (!target.name.equals(string(patch, "class"))) throw new IllegalArgumentException("Native patch target differs");
        if (patch.has("interfaces")) {
            var change = patch.getAsJsonObject("interfaces");
            var expected = new ArrayList<String>(); change.getAsJsonArray("original").forEach(x -> expected.add(x.getAsString()));
            if (!target.interfaces.equals(expected)) throw new IllegalStateException("Unsupported native AE2 interfaces: " + target.name);
            for (var item : change.getAsJsonArray("added")) if (target.interfaces.contains(item.getAsString()))
                throw new IllegalStateException("Native interface addition already exists: " + target.name);
        }
        if (patch.has("fieldChanges")) for (var entry : patch.getAsJsonArray("fieldChanges")) {
            var field = entry.getAsJsonObject();
            boolean matches = target.fields.stream().anyMatch(candidate -> candidate.name.equals(string(field, "name"))
                    && candidate.desc.equals(string(field, "desc")) && candidate.access == field.get("originalAccess").getAsInt());
            if (!matches) throw new IllegalStateException("Unsupported native AE2 field: " + target.name + "." + string(field, "name"));
        }
        for (var entry : patch.getAsJsonArray("changes")) {
            var change = entry.getAsJsonObject();
            MethodNode method = find(target, string(change, "name"), string(change, "desc"));
            String actual = fingerprint(method(method).get("instructions"));
            if (!actual.equals(string(change, "original"))) {
                throw new IllegalStateException("Unsupported native AE2 method: " + target.name + "." + method.name + method.desc);
            }
            var instructions = new ArrayList<JsonElement>();
            method(method).getAsJsonArray("instructions").forEach(instructions::add);
            var edits = change.getAsJsonArray("edits");
            for (int i = edits.size() - 1; i >= 0; i--) {
                var edit = edits.get(i).getAsJsonObject();
                int from = edit.get("from").getAsInt(), to = edit.get("to").getAsInt();
                instructions.subList(from, to).clear();
                var insert = new ArrayList<JsonElement>(); edit.getAsJsonArray("insert").forEach(insert::add);
                instructions.addAll(from, insert);
            }
            var merged = new JsonArray(); instructions.forEach(merged::add); verifyUniqueLabels(merged);
        }
        for (var entry : patch.getAsJsonArray("methods")) verifyUniqueLabels(entry.getAsJsonObject().getAsJsonArray("instructions"));
        if (patch.has("removedMethods")) for (var entry : patch.getAsJsonArray("removedMethods")) {
            var removed = entry.getAsJsonObject();
            var method = find(target, string(removed, "name"), string(removed, "desc"));
            if (!fingerprint(method(method).get("instructions")).equals(string(removed, "original")))
                throw new IllegalStateException("Unsupported removed native method: " + target.name + "." + method.name);
        }
        for (var entry : patch.getAsJsonArray("methods")) {
            var method = entry.getAsJsonObject();
            if (target.methods.stream().anyMatch(candidate -> candidate.name.equals(string(method, "name")) && candidate.desc.equals(string(method, "desc"))))
                throw new IllegalStateException("Native addition already exists: " + target.name + "." + string(method, "name"));
        }
        for (var entry : patch.getAsJsonArray("fields")) {
            var field = entry.getAsJsonObject();
            if (target.fields.stream().anyMatch(candidate -> candidate.name.equals(string(field, "name"))))
                throw new IllegalStateException("Native field already exists: " + target.name + "." + string(field, "name"));
        }
    }

    private static void verifyUniqueLabels(JsonArray instructions) {
        var labels = new java.util.HashSet<String>();
        for (var entry : instructions) {
            var node = entry.getAsJsonObject();
            if (node.get("type").getAsInt() == AbstractInsnNode.LABEL && !labels.add(node.get("label").getAsString()))
                throw new IllegalArgumentException("Native instruction defines a duplicate label: " + node.get("label").getAsString());
        }
    }

    private static MethodNode find(ClassNode target, String name, String descriptor) {
        return target.methods.stream().filter(method -> method.name.equals(name) && method.desc.equals(descriptor))
                .findFirst().orElseThrow(() -> new IllegalStateException("Missing native AE2 method: " + target.name + "." + name + descriptor));
    }

    public static void apply(ClassNode target, JsonObject patch) {
        verify(target, patch);
        if (patch.has("interfaces")) patch.getAsJsonObject("interfaces").getAsJsonArray("added")
                .forEach(x -> target.interfaces.add(x.getAsString()));
        if (patch.has("removedMethods")) for (var entry : patch.getAsJsonArray("removedMethods")) {
            var removed = entry.getAsJsonObject();
            target.methods.remove(find(target, string(removed, "name"), string(removed, "desc")));
        }
        if (patch.has("fieldChanges")) for (var entry : patch.getAsJsonArray("fieldChanges")) {
            var field = entry.getAsJsonObject();
            target.fields.stream().filter(candidate -> candidate.name.equals(string(field, "name"))).findFirst().orElseThrow()
                    .access = field.get("access").getAsInt();
            var changed = target.fields.stream().filter(candidate -> candidate.name.equals(string(field, "name"))).findFirst().orElseThrow();
            if (field.has("visibleAnnotations")) changed.visibleAnnotations = NativeInstructionCodec.annotations(field.getAsJsonArray("visibleAnnotations"));
            if (field.has("invisibleAnnotations")) changed.invisibleAnnotations = NativeInstructionCodec.annotations(field.getAsJsonArray("invisibleAnnotations"));
        }
        for (var entry : patch.getAsJsonArray("changes")) {
            var change = entry.getAsJsonObject(); var method = find(target, string(change, "name"), string(change, "desc"));
            for (var node : method.instructions.toArray()) if (node instanceof LineNumberNode) method.instructions.remove(node);
            var labels = new HashMap<String, LabelNode>();
            NativeInstructionCodec.labels(method, "o").forEach((label, name) -> labels.put(name, label));
            AbstractInsnNode[] original = method.instructions.toArray();
            var edits = change.getAsJsonArray("edits");
            var patched = new InsnList();
            int cursor = 0;
            for (var entryEdit : edits) {
                var edit = entryEdit.getAsJsonObject();
                int from = edit.get("from").getAsInt(), to = edit.get("to").getAsInt();
                if (from < cursor || to < from || to > original.length) throw new IllegalArgumentException("Invalid native edit range");
                while (cursor < from) patched.add(original[cursor++]);
                for (var instruction : edit.getAsJsonArray("insert")) patched.add(NativeInstructionCodec.instruction(instruction.getAsJsonObject(), labels));
                cursor = to;
            }
            while (cursor < original.length) patched.add(original[cursor++]);
            method.instructions = patched;
            method.name = change.has("rename") ? string(change, "rename") : method.name;
            if (change.has("access")) method.access = change.get("access").getAsInt();
            metadata(method, change, labels);
            if (change.has("visibleAnnotations")) method.visibleAnnotations = NativeInstructionCodec.annotations(change.getAsJsonArray("visibleAnnotations"));
            if (change.has("invisibleAnnotations")) method.invisibleAnnotations = NativeInstructionCodec.annotations(change.getAsJsonArray("invisibleAnnotations"));
        }
        for (var entry : patch.getAsJsonArray("fields")) {
            var field = entry.getAsJsonObject();
            var added = new FieldNode(field.get("access").getAsInt(), string(field, "name"), string(field, "desc"), string(field, "signature"),
                    !field.has("value") || field.get("value").isJsonNull() ? null : NativeInstructionCodec.constant(field.get("value")));
            added.visibleAnnotations = NativeInstructionCodec.annotations(field.getAsJsonArray("visibleAnnotations"));
            added.invisibleAnnotations = NativeInstructionCodec.annotations(field.getAsJsonArray("invisibleAnnotations"));
            target.fields.add(added);
        }
        for (var entry : patch.getAsJsonArray("methods")) target.methods.add(method(entry.getAsJsonObject()));
    }
}
