package probe;
import java.nio.file.Path;
import java.util.*;
import com.google.gson.*;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

/** Process-isolated unit fixture for the actual compiled optional native plugin. */
public final class OptionalAbiMain {
    public static boolean server;
    private static String mode;
    private static int requests,bodyRequests;
    public static ClassNode fetch(String name)throws java.io.IOException,ClassNotFoundException {
        requests++;
        if(name.replace('/','.').equals("appeng.core.AppEng")){
            if(mode.equals("absent-cnf"))throw new ClassNotFoundException("declared unit fixture: AE2 absent");
            if(mode.equals("absent-io"))throw new java.io.IOException("declared unit fixture: AE2 entry unavailable");
        }else{
            bodyRequests++;
            if(mode.equals("contract-cnf"))throw new ClassNotFoundException("declared unit fixture: native contract class absent");
            if(mode.equals("contract-io"))throw new java.io.IOException("declared unit fixture: native contract unreadable");
        }
        ClassNode node=new ClassNode();node.version=Opcodes.V17;node.access=Opcodes.ACC_PUBLIC;
        node.name=name.replace('.','/');node.superName="java/lang/Object";
        // Empty original members intentionally violate the packaged native ABI.
        return node;
    }
    private static void require(boolean v,String why){if(!v)throw new AssertionError(why);}
    @SuppressWarnings("unchecked")
    public static void main(String[] args)throws Exception {
        require(args.length==3,"jar/mode/side arguments");mode=args[1];server=args[2].equals("server");
        require(Set.of("absent-cnf","absent-io","contract-cnf","contract-io","contract-shape").contains(mode),"unknown fixture mode");
        Class<?> clazz=Class.forName("com.salts_inventory_update.compat.ae2.mixin.NativeDesktopPatchPlugin");
        Path loaded=Path.of(clazz.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath();
        require(loaded.equals(Path.of(args[0]).toRealPath()),"plugin must come from the exact shipping jar");
        Object plugin=clazz.getConstructor().newInstance();
        var active=clazz.getDeclaredField("active");active.setAccessible(true);active.setBoolean(null,true);
        var patches=clazz.getDeclaredField("patches");patches.setAccessible(true);
        ((Map<String,JsonObject>)patches.get(plugin)).put("fixture.PreviousHook",new JsonObject());
        clazz.getMethod("onLoad",String.class).invoke(plugin,"declared.unit.fixture");
        require(!active.getBoolean(null),"unsupported or absent AE2 must leave hooks inactive");
        var should=clazz.getMethod("shouldApplyMixin",String.class,String.class);
        for(String target:List.of("fixture.PreviousHook","appeng.client.gui.AEBaseScreen","appeng/client/gui/AEBaseScreen"))
            require(Boolean.FALSE.equals(should.invoke(plugin,target,"declared.unit.Mixin")),"hook must stay disabled: "+target);
        var warnings=probe.logging.ProbeProvider.EVENTS.stream().filter(v->v.startsWith("warn:")).toList();
        if(mode.startsWith("absent-")){
            require(requests==1&&bodyRequests==0,"absent optional AE2 must stop before patch verification");
            require(warnings.isEmpty(),"absent optional AE2 should not be reported as an ABI mismatch");
        }else{
            require(bodyRequests>0,"native verification path must actually run");
            require(warnings.size()==1&&warnings.get(0).contains("native ABI"),"unsupported ABI needs original clear warning");
        }
        var result=new LinkedHashMap<String,Object>();result.put("status","passed");result.put("plugin_jar",loaded.toString());
        result.put("mode",mode);result.put("side",args[2]);result.put("provider_requests",requests);result.put("body_requests",bodyRequests);
        result.put("hooks_active",active.getBoolean(null));result.put("remaining_cached_patches",((Map<?,?>)patches.get(plugin)).size());
        result.put("diagnostics",probe.logging.ProbeProvider.EVENTS);
        result.put("scope","Actual shipping plugin bytecode with synthetic Mixin environment/bytecode-provider seams. No Minecraft startup, fallback rendering, gameplay or arbitrary upstream-release acceptance.");
        System.out.println(new Gson().toJson(result));
    }
}
