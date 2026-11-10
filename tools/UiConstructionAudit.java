import com.google.gson.GsonBuilder;
import java.nio.file.*;
import java.io.*;
import java.util.*;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/** Excluded source-scope audit. Does not load or modify publisher classes. */
public final class UiConstructionAudit {
    public static void main(String[] args) throws Exception {
        var targets=args.length>2?new LinkedHashSet<>(Arrays.asList(Arrays.copyOfRange(args,2,args.length))):Set.of("appeng/client/gui/AESubScreen","appeng/client/gui/implementations/UpgradeableScreen");
        var result=new TreeMap<String,Map<String,Object>>();
        for(String target:targets){var row=new LinkedHashMap<String,Object>();row.put("present",false);
            for(String field:List.of("allocations","constructor_handles","class_literals","named_strings","subclasses","super_constructor_calls"))row.put(field,new ArrayList<String>());
            result.put(target,row);
        }
        var classes=new LinkedHashMap<String,byte[]>();
        readArchive(Files.readAllBytes(Path.of(args[0])),classes);
        for(var bytes:classes.values()){
                var node=new ClassNode();new ClassReader(bytes).accept(node,ClassReader.SKIP_DEBUG|ClassReader.SKIP_FRAMES);
                if(targets.contains(node.name)){result.get(node.name).put("present",true);result.get(node.name).put("access",node.access);result.get(node.name).put("superclass",node.superName);}
                if(targets.contains(node.superName))add(result,node.superName,"subclasses",node.name);
                for(var method:node.methods)for(var insn:method.instructions){
                    String site=node.name+"."+method.name+method.desc;
                    if(insn instanceof TypeInsnNode type&&type.getOpcode()==Opcodes.NEW&&targets.contains(type.desc))add(result,type.desc,"allocations",site);
                    if(insn instanceof MethodInsnNode invoke&&invoke.name.equals("<init>")&&targets.contains(invoke.owner))add(result,invoke.owner,"super_constructor_calls",site+" -> "+invoke.desc);
                    if(insn instanceof InvokeDynamicInsnNode dynamic)for(Object value:dynamic.bsmArgs)handle(result,targets,value,site);
                    if(insn instanceof LdcInsnNode constant){
                        Object value=constant.cst;handle(result,targets,value,site);
                        if(value instanceof Type type&&type.getSort()==Type.OBJECT&&targets.contains(type.getInternalName()))add(result,type.getInternalName(),"class_literals",site);
                        if(value instanceof String name)for(String target:targets)if(name.equals(target)||name.equals(target.replace('/','.')))add(result,target,"named_strings",site);
                    }
                }
        }
        Files.writeString(Path.of(args[1]),new GsonBuilder().setPrettyPrinting().create().toJson(result));
    }
    private static void readArchive(byte[] bytes,Map<String,byte[]> classes)throws IOException{
        try(var archive=new ZipInputStream(new ByteArrayInputStream(bytes))){
            java.util.zip.ZipEntry entry;
            while((entry=archive.getNextEntry())!=null){
                if(entry.getName().endsWith(".class"))classes.putIfAbsent(entry.getName(),archive.readAllBytes());
                else if(entry.getName().endsWith(".jar"))readArchive(archive.readAllBytes(),classes);
            }
        }
    }
    private static void handle(Map<String,Map<String,Object>> result,Set<String> targets,Object value,String site){
        if(value instanceof Handle handle&&handle.getTag()==Opcodes.H_NEWINVOKESPECIAL&&targets.contains(handle.getOwner()))add(result,handle.getOwner(),"constructor_handles",site);
    }
    @SuppressWarnings("unchecked")
    private static void add(Map<String,Map<String,Object>> result,String target,String field,String site){((List<String>)result.get(target).get(field)).add(site);}
}
