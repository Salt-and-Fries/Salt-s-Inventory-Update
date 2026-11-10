// Excluded instrumentation: records the existing send and authorization path.
import java.lang.instrument.*;
import java.security.ProtectionDomain;
import java.util.jar.JarFile;
import org.objectweb.asm.*;

public final class UiClickProbe {
    private static final String R="siu/qa/clickprobe/UiClickProbeRuntime";
    public static void premain(String args,Instrumentation instrumentation) throws Exception {
        String[] parts=args.split("\\|",2);
        instrumentation.appendToBootstrapClassLoaderSearch(new JarFile(parts[0]));
        Class.forName(R.replace('/','.'),true,null).getMethod("configure",String.class).invoke(null,parts[1]);
        instrumentation.addTransformer(new ClassFileTransformer(){
            public byte[] transform(ClassLoader loader,String name,Class<?> redef,ProtectionDomain domain,byte[] bytes){
                boolean client=name.equals("com/salts_inventory_update/client/DesktopContainerClient");
                boolean server=name.equals("com/salts_inventory_update/server/DesktopContainerSessions$MutationRegistration");
                boolean modern=name.equals("com/salts_inventory_update/server/DesktopContainerSessions");
                if(!client&&!server&&!modern)return null;
                if(modern){boolean[] queued={false};new ClassReader(bytes).accept(new ClassVisitor(Opcodes.ASM9){
                    public MethodVisitor visitMethod(int access,String method,String desc,String signature,String[] exceptions){if(method.equals("handleMutation"))queued[0]=true;return null;}
                },ClassReader.SKIP_CODE|ClassReader.SKIP_DEBUG|ClassReader.SKIP_FRAMES);if(!queued[0])return null;}
                int[] matched={0};ClassWriter writer=new ClassWriter(ClassWriter.COMPUTE_MAXS);
                ClassVisitor visitor=new ClassVisitor(Opcodes.ASM9,writer){
                    public MethodVisitor visitMethod(int access,String method,String descriptor,String signature,String[] exceptions){
                        MethodVisitor next=super.visitMethod(access,method,descriptor,signature,exceptions);
                        if(modern){
                            boolean click=method.equals("click")&&descriptor.contains("DesktopClickPayload;");if(click)matched[0]++;
                            return new MethodVisitor(Opcodes.ASM9,next){
                                public void visitCode(){super.visitCode();if(click){visitVarInsn(Opcodes.ALOAD,0);visitVarInsn(Opcodes.ALOAD,1);visitLdcInsn(Type.getObjectType(name));super.visitMethodInsn(Opcodes.INVOKESTATIC,R,"modernEnter","(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V",false);}}
                                public void visitMethodInsn(int opcode,String owner,String method,String desc,boolean itf){
                                    if(click&&owner.equals(name)&&method.equals("clickMenu"))super.visitMethodInsn(Opcodes.INVOKESTATIC,R,"accepted","()V",false);
                                    if(owner.equals("com/salts_inventory_update/debug/DesktopDebug")&&(method.equals("warn")||method.equals("trace"))&&desc.equals("(Ljava/lang/String;[Ljava/lang/Object;)V")){
                                        super.visitInsn(Opcodes.DUP2);super.visitMethodInsn(Opcodes.INVOKESTATIC,R,"rejected",desc,false);}
                                    super.visitMethodInsn(opcode,owner,method,desc,itf);
                                }
                                public void visitInsn(int opcode){if(click&&(opcode==Opcodes.RETURN||opcode==Opcodes.ATHROW))super.visitMethodInsn(Opcodes.INVOKESTATIC,R,"exit","()V",false);super.visitInsn(opcode);}
                            };
                        }
                        if(client&&method.equals("clickSlot")&&descriptor.startsWith("(IIII")){
                            matched[0]++;
                            return new MethodVisitor(Opcodes.ASM9,next){
                                public void visitCode(){super.visitCode();for(int i=0;i<4;i++)visitVarInsn(Opcodes.ILOAD,i);
                                    visitVarInsn(Opcodes.ALOAD,4);visitVarInsn(Opcodes.ALOAD,5);visitLdcInsn(Type.getObjectType(name));
                                    visitMethodInsn(Opcodes.INVOKESTATIC,R,"client","(IIIILjava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V",false);}
                            };
                        }
                        if(server&&method.equals("dispatch")){
                            matched[0]++;
                            return new MethodVisitor(Opcodes.ASM9,next){
                                public void visitCode(){super.visitCode();for(int i=0;i<4;i++)visitVarInsn(Opcodes.ALOAD,i);
                                    visitMethodInsn(Opcodes.INVOKESTATIC,R,"enter","(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V",false);}
                                public void visitMethodInsn(int opcode,String owner,String method,String desc,boolean itf){
                                    if(owner.equals("java/util/function/BiConsumer")&&method.equals("accept"))super.visitMethodInsn(Opcodes.INVOKESTATIC,R,"accepted","()V",false);
                                    if(owner.equals("com/salts_inventory_update/debug/DesktopDebug")&&(method.equals("warn")||method.equals("trace"))&&desc.equals("(Ljava/lang/String;[Ljava/lang/Object;)V")){
                                        super.visitInsn(Opcodes.DUP2);super.visitMethodInsn(Opcodes.INVOKESTATIC,R,"rejected",desc,false);
                                    }
                                    super.visitMethodInsn(opcode,owner,method,desc,itf);
                                    if(owner.equals("java/util/function/Function")&&method.equals("apply")){
                                        super.visitInsn(Opcodes.DUP);super.visitMethodInsn(Opcodes.INVOKESTATIC,R,"decoded","(Ljava/lang/Object;)V",false);
                                    }
                                }
                                public void visitInsn(int opcode){if(opcode==Opcodes.RETURN||opcode==Opcodes.ATHROW)super.visitMethodInsn(Opcodes.INVOKESTATIC,R,"exit","()V",false);super.visitInsn(opcode);}
                            };
                        }
                        if(client)return new MethodVisitor(Opcodes.ASM9,next){
                            public void visitCode(){super.visitCode();if(method.equals("send")&&descriptor.endsWith("Ljava/lang/String;)Z")){
                                visitVarInsn(Opcodes.ALOAD,0);super.visitMethodInsn(Opcodes.INVOKESTATIC,R,"packet","(Ljava/lang/Object;)V",false);}}
                            public void visitMethodInsn(int opcode,String owner,String method,String desc,boolean itf){
                                if(owner.equals("com/salts_inventory_update/debug/DesktopDebug")&&(method.equals("warn")||method.equals("trace"))&&desc.equals("(Ljava/lang/String;[Ljava/lang/Object;)V")){
                                    super.visitInsn(Opcodes.DUP2);super.visitMethodInsn(Opcodes.INVOKESTATIC,R,"clientLog",desc,false);}
                                super.visitMethodInsn(opcode,owner,method,desc,itf);
                            }
                        };
                        return next;
                    }
                };
                new ClassReader(bytes).accept(visitor,0);
                // The legacy outer class has no modern click payload signature.
                if(modern&&matched[0]==0)return null;
                if(matched[0]!=1)throw new IllegalStateException("Expected one probe method in "+name+", found "+matched[0]);
                System.err.println("SIU_CLICK_PROBE_INSTALLED "+name+" (no original instruction removed)");
                return writer.toByteArray();
            }
        });
    }
}
