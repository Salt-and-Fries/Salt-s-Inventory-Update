// Excluded diagnosis for the exact 1.20.1 Fabric runtime. This adds bounded
// stack logging to the native revision method; it never supplies a menu/click.
import java.lang.instrument.*;
import java.security.ProtectionDomain;
import org.objectweb.asm.*;

public class NativeRevisionAgent {
    static final String STACK="net/minecraft/class_1799",TAG="net/minecraft/class_2487";
    static void append(MethodVisitor v,String text){v.visitLdcInsn(text);v.visitMethodInsn(Opcodes.INVOKEVIRTUAL,"java/lang/StringBuilder","append","(Ljava/lang/String;)Ljava/lang/StringBuilder;",false);}
    static void value(MethodVisitor v,int local,String method,String descriptor,String appendType){v.visitVarInsn(Opcodes.ALOAD,local);v.visitMethodInsn(Opcodes.INVOKEVIRTUAL,STACK,method,descriptor,false);v.visitMethodInsn(Opcodes.INVOKEVIRTUAL,"java/lang/StringBuilder","append","("+appendType+")Ljava/lang/StringBuilder;",false);}
    static void mismatchPrinter(ClassVisitor visitor,String owner){
        MethodVisitor v=visitor.visitMethod(Opcodes.ACC_PRIVATE|Opcodes.ACC_STATIC|Opcodes.ACC_SYNTHETIC,"siu$qaPrintMismatch","(IL"+STACK+";L"+STACK+";)V",null,null);v.visitCode();
        v.visitFieldInsn(Opcodes.GETSTATIC,"java/lang/System","err","Ljava/io/PrintStream;");v.visitTypeInsn(Opcodes.NEW,"java/lang/StringBuilder");v.visitInsn(Opcodes.DUP);v.visitMethodInsn(Opcodes.INVOKESPECIAL,"java/lang/StringBuilder","<init>","()V",false);
        append(v,"SIU QA native cache mismatch slot=");v.visitVarInsn(Opcodes.ILOAD,0);v.visitMethodInsn(Opcodes.INVOKEVIRTUAL,"java/lang/StringBuilder","append","(I)Ljava/lang/StringBuilder;",false);
        for(int local=1;local<=2;local++){
            append(v,local==1?" incoming=":" cached=");v.visitVarInsn(Opcodes.ALOAD,local);v.visitMethodInsn(Opcodes.INVOKEVIRTUAL,"java/lang/StringBuilder","append","(Ljava/lang/Object;)Ljava/lang/StringBuilder;",false);
            append(v," count=");value(v,local,"method_7947","()I","I");append(v," empty=");value(v,local,"method_7960","()Z","Z");
            append(v," tag=");value(v,local,"method_7969","()L"+TAG+";","Ljava/lang/Object;");
        }
        v.visitMethodInsn(Opcodes.INVOKEVIRTUAL,"java/lang/StringBuilder","toString","()Ljava/lang/String;",false);v.visitMethodInsn(Opcodes.INVOKEVIRTUAL,"java/io/PrintStream","println","(Ljava/lang/String;)V",false);v.visitInsn(Opcodes.RETURN);v.visitMaxs(0,0);v.visitEnd();
    }
    public static void premain(String args,Instrumentation instrumentation) {
        instrumentation.addTransformer(new ClassFileTransformer() {
            @Override public byte[] transform(ClassLoader loader,String name,Class<?> redef,ProtectionDomain domain,byte[] original) {
                if(!name.equals("net/minecraft/class_1703"))return null;
                ClassWriter writer=new ClassWriter(ClassWriter.COMPUTE_MAXS);int[] found={0},mismatch={0};
                ClassVisitor visitor=new ClassVisitor(Opcodes.ASM9,writer) {
                    @Override public MethodVisitor visitMethod(int access,String method,String descriptor,String signature,String[] exceptions) {
                        MethodVisitor next=super.visitMethod(access,method,descriptor,signature,exceptions);
                        if(method.equals("method_34253")&&descriptor.equals("(IL"+STACK+";Ljava/util/function/Supplier;)V")){
                            mismatch[0]++;
                            return new MethodVisitor(Opcodes.ASM9,next){
                                void cached(){visitVarInsn(Opcodes.ALOAD,0);visitFieldInsn(Opcodes.GETFIELD,name,"field_29206","Lnet/minecraft/class_2371;");visitVarInsn(Opcodes.ILOAD,1);visitMethodInsn(Opcodes.INVOKEVIRTUAL,"net/minecraft/class_2371","get","(I)Ljava/lang/Object;",false);visitTypeInsn(Opcodes.CHECKCAST,STACK);}
                                @Override public void visitCode(){super.visitCode();Label skip=new Label();
                                    visitVarInsn(Opcodes.ALOAD,0);visitFieldInsn(Opcodes.GETFIELD,name,"field_7763","I");visitJumpInsn(Opcodes.IFNE,skip);
                                    visitVarInsn(Opcodes.ALOAD,0);visitFieldInsn(Opcodes.GETFIELD,name,"field_34024","I");visitIntInsn(Opcodes.SIPUSH,200);visitJumpInsn(Opcodes.IF_ICMPLE,skip);
                                    visitFieldInsn(Opcodes.GETSTATIC,name,"siu$qaMismatchProbeCount","I");visitIntInsn(Opcodes.BIPUSH,16);visitJumpInsn(Opcodes.IF_ICMPGE,skip);
                                    cached();visitVarInsn(Opcodes.ALOAD,2);visitMethodInsn(Opcodes.INVOKESTATIC,STACK,"method_7973","(L"+STACK+";L"+STACK+";)Z",false);visitJumpInsn(Opcodes.IFNE,skip);
                                    visitFieldInsn(Opcodes.GETSTATIC,name,"siu$qaMismatchProbeCount","I");visitInsn(Opcodes.ICONST_1);visitInsn(Opcodes.IADD);visitFieldInsn(Opcodes.PUTSTATIC,name,"siu$qaMismatchProbeCount","I");
                                    visitVarInsn(Opcodes.ILOAD,1);visitVarInsn(Opcodes.ALOAD,2);cached();visitMethodInsn(Opcodes.INVOKESTATIC,name,"siu$qaPrintMismatch","(IL"+STACK+";L"+STACK+";)V",false);
                                    visitLabel(skip);visitFrame(Opcodes.F_SAME,0,null,0,null);
                                }
                            };
                        }
                        if(!method.equals("method_37422")||!descriptor.equals("()I"))return next;
                        found[0]++;
                        return new MethodVisitor(Opcodes.ASM9,next) {
                            @Override public void visitCode() {
                                super.visitCode();Label skip=new Label();
                                visitVarInsn(Opcodes.ALOAD,0);visitFieldInsn(Opcodes.GETFIELD,name,"field_7763","I");visitJumpInsn(Opcodes.IFNE,skip);
                                visitVarInsn(Opcodes.ALOAD,0);visitFieldInsn(Opcodes.GETFIELD,name,"field_34024","I");visitIntInsn(Opcodes.SIPUSH,200);visitJumpInsn(Opcodes.IF_ICMPLE,skip);
                                visitFieldInsn(Opcodes.GETSTATIC,name,"siu$qaRevisionProbeCount","I");visitIntInsn(Opcodes.BIPUSH,32);visitJumpInsn(Opcodes.IF_ICMPGE,skip);
                                visitFieldInsn(Opcodes.GETSTATIC,name,"siu$qaRevisionProbeCount","I");visitInsn(Opcodes.ICONST_1);visitInsn(Opcodes.IADD);visitFieldInsn(Opcodes.PUTSTATIC,name,"siu$qaRevisionProbeCount","I");
                                visitTypeInsn(Opcodes.NEW,"java/lang/Throwable");visitInsn(Opcodes.DUP);visitLdcInsn("SIU QA native player-menu revision caller (diagnostic only)");
                                visitMethodInsn(Opcodes.INVOKESPECIAL,"java/lang/Throwable","<init>","(Ljava/lang/String;)V",false);
                                visitMethodInsn(Opcodes.INVOKEVIRTUAL,"java/lang/Throwable","printStackTrace","()V",false);
                                visitLabel(skip);visitFrame(Opcodes.F_SAME,0,null,0,null);
                            }
                        };
                    }
                    @Override public void visitEnd() {
                        visitField(Opcodes.ACC_PRIVATE|Opcodes.ACC_STATIC|Opcodes.ACC_SYNTHETIC,"siu$qaRevisionProbeCount","I",null,0).visitEnd();
                        if(mismatch[0]>0){visitField(Opcodes.ACC_PRIVATE|Opcodes.ACC_STATIC|Opcodes.ACC_SYNTHETIC,"siu$qaMismatchProbeCount","I",null,0).visitEnd();mismatchPrinter(this,name);}
                        super.visitEnd();
                    }
                };
                new ClassReader(original).accept(visitor,0);
                if(found[0]!=1)throw new IllegalStateException("Expected exactly one native incrementStateId method, found "+found[0]);
                System.err.println("SIU QA instrumented exact native incrementStateId and "+mismatch[0]+" cache comparison method; no shipping jar changed");
                return writer.toByteArray();
            }
        });
    }
}
