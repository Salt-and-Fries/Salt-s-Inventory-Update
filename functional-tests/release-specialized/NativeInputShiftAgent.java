import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import org.objectweb.asm.*;

/** Excluded synthetic keyboard fixture. Native menus and SIU are untouched. */
public final class NativeInputShiftAgent {
    public static void premain(String ignored, Instrumentation instrumentation) {
        instrumentation.addTransformer(new ClassFileTransformer() {
            @Override public byte[] transform(ClassLoader loader, String name, Class<?> redef,
                    ProtectionDomain domain, byte[] bytes) {
                if (!name.equals("com/mojang/blaze3d/platform/InputConstants")
                        && !name.equals("net/minecraft/class_3675")) return null;
                int[] matches = {0};
                ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
                new ClassReader(bytes).accept(new ClassVisitor(Opcodes.ASM9, writer) {
                    @Override public MethodVisitor visitMethod(int access, String method, String descriptor,
                            String signature, String[] exceptions) {
                        MethodVisitor next = super.visitMethod(access, method, descriptor, signature, exceptions);
                        if ((access & Opcodes.ACC_STATIC) == 0 || !descriptor.equals("(JI)Z")) return next;
                        matches[0]++;
                        return new MethodVisitor(Opcodes.ASM9, next) {
                            @Override public void visitCode() {
                                super.visitCode();
                                Label force = new Label(), original = new Label();
                                visitVarInsn(Opcodes.ILOAD, 2); visitIntInsn(Opcodes.SIPUSH, 340);
                                visitJumpInsn(Opcodes.IF_ICMPEQ, force);
                                visitVarInsn(Opcodes.ILOAD, 2); visitIntInsn(Opcodes.SIPUSH, 344);
                                visitJumpInsn(Opcodes.IF_ICMPNE, original);
                                visitLabel(force); visitFrame(Opcodes.F_SAME, 0, null, 0, null);
                                visitLdcInsn("siu.release.qa.shift");
                                visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Boolean", "getBoolean", "(Ljava/lang/String;)Z", false);
                                visitJumpInsn(Opcodes.IFEQ, original);
                                visitInsn(Opcodes.ICONST_1); visitInsn(Opcodes.IRETURN);
                                visitLabel(original); visitFrame(Opcodes.F_SAME, 0, null, 0, null);
                            }
                        };
                    }
                }, 0);
                System.setProperty("siu.release.qa.shift.matches", Integer.toString(matches[0]));
                System.setProperty("siu.release.qa.shift.target", name);
                // Refuse an unreviewed native input signature instead of quietly
                // running a Shift scenario without its declared input fixture.
                if (matches[0] != 1) return new byte[] {0};
                return writer.toByteArray();
            }
        });
    }
}
