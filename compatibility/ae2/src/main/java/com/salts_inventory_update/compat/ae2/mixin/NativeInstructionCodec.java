package com.salts_inventory_update.compat.ae2.mixin;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import org.objectweb.asm.ConstantDynamic;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

/** Lossless encoding of only the native instructions added by an integration patch. */
public final class NativeInstructionCodec {
    private NativeInstructionCodec() { }

    public static Map<LabelNode, String> labels(MethodNode method, String prefix) {
        Map<LabelNode, String> result = new IdentityHashMap<>();
        for (var node : method.instructions) {
            if (node instanceof LabelNode label) result.put(label, prefix + result.size());
        }
        return result;
    }

    public static JsonElement constant(Object value) {
        var out = new JsonObject();
        if (value == null) {
            out.addProperty("kind", "null");
        } else if (value instanceof Float number) {
            out.addProperty("kind", "floatBits"); out.addProperty("value", Float.floatToRawIntBits(number));
        } else if (value instanceof Double number) {
            out.addProperty("kind", "doubleBits"); out.addProperty("value", Double.doubleToRawLongBits(number));
        } else if (value instanceof Type type) {
            out.addProperty("kind", "type"); out.addProperty("value", type.getDescriptor());
        } else if (value instanceof Handle handle) {
            out.addProperty("kind", "handle"); out.addProperty("tag", handle.getTag());
            out.addProperty("owner", handle.getOwner()); out.addProperty("name", handle.getName());
            out.addProperty("desc", handle.getDesc()); out.addProperty("interface", handle.isInterface());
        } else if (value instanceof ConstantDynamic dynamic) {
            out.addProperty("kind", "dynamic"); out.addProperty("name", dynamic.getName());
            out.addProperty("desc", dynamic.getDescriptor()); out.add("bootstrap", constant(dynamic.getBootstrapMethod()));
            var args = new JsonArray();
            for (int i = 0; i < dynamic.getBootstrapMethodArgumentCount(); i++) args.add(constant(dynamic.getBootstrapMethodArgument(i)));
            out.add("args", args);
        } else {
            out.addProperty("kind", value instanceof String ? "string" : value.getClass().getSimpleName());
            if (value instanceof Number number) out.addProperty("value", number);
            else out.addProperty("value", value.toString());
        }
        return out;
    }

    public static Object constant(JsonElement element) {
        var in = element.getAsJsonObject();
        return switch (in.get("kind").getAsString()) {
            case "null" -> null;
            case "floatBits" -> Float.intBitsToFloat(in.get("value").getAsInt());
            case "doubleBits" -> Double.longBitsToDouble(in.get("value").getAsLong());
            case "type" -> Type.getType(in.get("value").getAsString());
            case "handle" -> new Handle(in.get("tag").getAsInt(), in.get("owner").getAsString(),
                    in.get("name").getAsString(), in.get("desc").getAsString(), in.get("interface").getAsBoolean());
            case "dynamic" -> new ConstantDynamic(in.get("name").getAsString(), in.get("desc").getAsString(),
                    (Handle) constant(in.get("bootstrap")), constants(in.getAsJsonArray("args")));
            case "Integer" -> in.get("value").getAsInt();
            case "Boolean" -> in.get("value").getAsBoolean();
            case "Byte" -> in.get("value").getAsByte();
            case "Short" -> in.get("value").getAsShort();
            case "Character" -> in.get("value").getAsString().charAt(0);
            case "Long" -> in.get("value").getAsLong();
            case "Float" -> in.get("value").getAsFloat();
            case "Double" -> in.get("value").getAsDouble();
            case "string" -> in.get("value").getAsString();
            default -> throw new IllegalArgumentException("Unsupported native constant " + in);
        };
    }

    /** Runtime annotations include native synchronization contracts, not just tooling hints. */
    public static JsonArray annotations(java.util.List<AnnotationNode> annotations) {
        var out = new JsonArray();
        if (annotations != null) for (var annotation : annotations) out.add(annotation(annotation));
        return out;
    }

    private static JsonObject annotation(AnnotationNode annotation) {
        var out = new JsonObject(); out.addProperty("desc", annotation.desc);
        var values = new JsonArray();
        if (annotation.values != null) for (int i = 0; i < annotation.values.size(); i += 2) {
            var value = new JsonObject(); value.addProperty("name", (String) annotation.values.get(i));
            value.add("value", annotationValue(annotation.values.get(i+1))); values.add(value);
        }
        out.add("values", values); return out;
    }

    private static JsonObject annotationValue(Object value) {
        var out = new JsonObject();
        if (value instanceof AnnotationNode nested) {
            out.addProperty("kind", "annotation"); out.add("value", annotation(nested));
        } else if (value instanceof String[] enumeration && enumeration.length == 2 && enumeration[0].startsWith("L")) {
            out.addProperty("kind", "enum"); out.addProperty("desc", enumeration[0]); out.addProperty("value", enumeration[1]);
        } else if (value instanceof java.util.List<?> || value.getClass().isArray()) {
            out.addProperty("kind", "array"); var items = new JsonArray();
            if (value instanceof java.util.List<?> list) for (Object item : list) items.add(annotationValue(item));
            else for (int i = 0; i < java.lang.reflect.Array.getLength(value); i++) items.add(annotationValue(java.lang.reflect.Array.get(value, i)));
            out.add("value", items);
        } else {
            out.addProperty("kind", "scalar"); out.add("value", constant(value));
        }
        return out;
    }

    public static java.util.List<AnnotationNode> annotations(JsonArray values) {
        var out = new java.util.ArrayList<AnnotationNode>();
        if (values != null) for (var value : values) out.add(annotation(value.getAsJsonObject()));
        return out.isEmpty() ? null : out;
    }

    private static AnnotationNode annotation(JsonObject input) {
        var out = new AnnotationNode(input.get("desc").getAsString());
        for (var value : input.getAsJsonArray("values")) {
            var entry = value.getAsJsonObject(); out.visit(entry.get("name").getAsString(), annotationValue(entry.getAsJsonObject("value")));
        }
        return out;
    }

    private static Object annotationValue(JsonObject input) {
        return switch (input.get("kind").getAsString()) {
            case "annotation" -> annotation(input.getAsJsonObject("value"));
            case "enum" -> new String[]{input.get("desc").getAsString(), input.get("value").getAsString()};
            case "array" -> {
                var values = new java.util.ArrayList<Object>();
                for (var value : input.getAsJsonArray("value")) values.add(annotationValue(value.getAsJsonObject()));
                yield values;
            }
            case "scalar" -> constant(input.get("value"));
            default -> throw new IllegalArgumentException("Unsupported native annotation value " + input);
        };
    }

    private static Object[] constants(JsonArray input) {
        var out = new Object[input.size()];
        for (int i = 0; i < out.length; i++) out[i] = constant(input.get(i));
        return out;
    }

    private static JsonArray frame(java.util.List<Object> values, Map<LabelNode, String> labels) {
        var out = new JsonArray();
        if (values != null) for (Object value : values) {
            if (value instanceof LabelNode label) {
                var entry = new JsonObject(); entry.addProperty("label", requiredLabel(labels, label)); out.add(entry);
            } else out.add(constant(value));
        }
        return out;
    }

    private static Object[] frame(JsonArray values, Map<String, LabelNode> labels) {
        var out = new Object[values.size()];
        for (int i = 0; i < out.length; i++) {
            var value = values.get(i).getAsJsonObject();
            out[i] = value.has("label") ? label(labels, value.get("label").getAsString()) : constant(value);
        }
        return out;
    }

    public static JsonArray instructions(MethodNode method, Map<LabelNode, String> labels) {
        var out = new JsonArray();
        for (var node : method.instructions) {
            if (node instanceof LineNumberNode) continue;
            out.add(instruction(node, labels));
        }
        return out;
    }

    public static JsonObject instruction(AbstractInsnNode node, Map<LabelNode, String> labels) {
        var out = new JsonObject(); out.addProperty("type", node.getType()); out.addProperty("op", node.getOpcode());
        if (node instanceof IntInsnNode n) out.addProperty("operand", n.operand);
        else if (node instanceof VarInsnNode n) out.addProperty("var", n.var);
        else if (node instanceof TypeInsnNode n) out.addProperty("desc", n.desc);
        else if (node instanceof FieldInsnNode n) {
            out.addProperty("owner", n.owner); out.addProperty("name", n.name); out.addProperty("desc", n.desc);
        } else if (node instanceof MethodInsnNode n) {
            out.addProperty("owner", n.owner); out.addProperty("name", n.name); out.addProperty("desc", n.desc);
            out.addProperty("interface", n.itf);
        } else if (node instanceof InvokeDynamicInsnNode n) {
            out.addProperty("name", n.name); out.addProperty("desc", n.desc); out.add("bootstrap", constant(n.bsm));
            var args = new JsonArray(); for (Object value : n.bsmArgs) args.add(constant(value)); out.add("args", args);
        } else if (node instanceof JumpInsnNode n) out.addProperty("label", requiredLabel(labels, n.label));
        else if (node instanceof LabelNode n) out.addProperty("label", requiredLabel(labels, n));
        else if (node instanceof LdcInsnNode n) out.add("constant", constant(n.cst));
        else if (node instanceof IincInsnNode n) { out.addProperty("var", n.var); out.addProperty("increment", n.incr); }
        else if (node instanceof TableSwitchInsnNode n) {
            out.addProperty("min", n.min); out.addProperty("max", n.max); out.addProperty("default", requiredLabel(labels, n.dflt));
            var targets = new JsonArray(); for (var target : n.labels) targets.add(requiredLabel(labels, target)); out.add("labels", targets);
        } else if (node instanceof LookupSwitchInsnNode n) {
            out.addProperty("default", requiredLabel(labels, n.dflt));
            var keys = new JsonArray(); for (int key : n.keys) keys.add(key); out.add("keys", keys);
            var targets = new JsonArray(); for (var target : n.labels) targets.add(requiredLabel(labels, target)); out.add("labels", targets);
        } else if (node instanceof MultiANewArrayInsnNode n) { out.addProperty("desc", n.desc); out.addProperty("dimensions", n.dims); }
        else if (node instanceof FrameNode n) {
            out.addProperty("frameType", n.type); out.add("locals", frame(n.local, labels)); out.add("stack", frame(n.stack, labels));
        } else if (!(node instanceof InsnNode)) throw new IllegalArgumentException("Unsupported native instruction " + node.getType());
        return out;
    }

    private static String requiredLabel(Map<LabelNode, String> labels, LabelNode target) {
        String id = labels.get(target);
        if (id == null) throw new IllegalArgumentException("Native instruction references an undefined label");
        return id;
    }

    private static LabelNode label(Map<String, LabelNode> labels, String id) {
        return labels.computeIfAbsent(id, ignored -> new LabelNode());
    }

    public static AbstractInsnNode instruction(JsonObject in, Map<String, LabelNode> labels) {
        int op = in.get("op").getAsInt();
        return switch (in.get("type").getAsInt()) {
            case AbstractInsnNode.INSN -> new InsnNode(op);
            case AbstractInsnNode.INT_INSN -> new IntInsnNode(op, in.get("operand").getAsInt());
            case AbstractInsnNode.VAR_INSN -> new VarInsnNode(op, in.get("var").getAsInt());
            case AbstractInsnNode.TYPE_INSN -> new TypeInsnNode(op, in.get("desc").getAsString());
            case AbstractInsnNode.FIELD_INSN -> new FieldInsnNode(op, in.get("owner").getAsString(), in.get("name").getAsString(), in.get("desc").getAsString());
            case AbstractInsnNode.METHOD_INSN -> new MethodInsnNode(op, in.get("owner").getAsString(), in.get("name").getAsString(), in.get("desc").getAsString(), in.get("interface").getAsBoolean());
            case AbstractInsnNode.INVOKE_DYNAMIC_INSN -> new InvokeDynamicInsnNode(in.get("name").getAsString(), in.get("desc").getAsString(), (Handle) constant(in.get("bootstrap")), constants(in.getAsJsonArray("args")));
            case AbstractInsnNode.JUMP_INSN -> new JumpInsnNode(op, label(labels, in.get("label").getAsString()));
            case AbstractInsnNode.LABEL -> label(labels, in.get("label").getAsString());
            case AbstractInsnNode.LDC_INSN -> new LdcInsnNode(constant(in.get("constant")));
            case AbstractInsnNode.IINC_INSN -> new IincInsnNode(in.get("var").getAsInt(), in.get("increment").getAsInt());
            case AbstractInsnNode.TABLESWITCH_INSN -> new TableSwitchInsnNode(in.get("min").getAsInt(), in.get("max").getAsInt(), label(labels, in.get("default").getAsString()), targets(in.getAsJsonArray("labels"), labels));
            case AbstractInsnNode.LOOKUPSWITCH_INSN -> new LookupSwitchInsnNode(label(labels, in.get("default").getAsString()), keys(in.getAsJsonArray("keys")), targets(in.getAsJsonArray("labels"), labels));
            case AbstractInsnNode.MULTIANEWARRAY_INSN -> new MultiANewArrayInsnNode(in.get("desc").getAsString(), in.get("dimensions").getAsInt());
            case AbstractInsnNode.FRAME -> {
                Object[] locals = frame(in.getAsJsonArray("locals"), labels), stack = frame(in.getAsJsonArray("stack"), labels);
                yield new FrameNode(in.get("frameType").getAsInt(), locals.length, locals, stack.length, stack);
            }
            default -> throw new IllegalArgumentException("Unsupported native instruction " + in);
        };
    }

    private static LabelNode[] targets(JsonArray input, Map<String, LabelNode> labels) {
        var out = new LabelNode[input.size()];
        for (int i = 0; i < out.length; i++) out[i] = label(labels, input.get(i).getAsString());
        return out;
    }

    private static int[] keys(JsonArray input) {
        var out = new int[input.size()]; for (int i = 0; i < out.length; i++) out[i] = input.get(i).getAsInt(); return out;
    }
}
