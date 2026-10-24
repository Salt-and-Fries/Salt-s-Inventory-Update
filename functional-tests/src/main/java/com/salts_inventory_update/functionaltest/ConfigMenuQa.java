package com.salts_inventory_update.functionaltest;

import com.google.gson.GsonBuilder;
import com.salts_inventory_update.VersionInfo;
import com.salts_inventory_update.platform.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;

/** Opt-in menu-only regression: no world creation or complete gameplay suite. */
public final class ConfigMenuQa {
    private static final List<Map<String,Object>> results=new ArrayList<>();
    private static final String loader=System.getenv("SIU_FUNCTIONAL_TEST_LOADER");
    private static final Path output=Path.of(System.getenv("SIU_QA_OUTPUT"));
    private static boolean started,finished;
    private static int phase,ticks,pending;
    private static Object parent,container,factory;
    public static synchronized void initialize(){
        if(started)return;started=true;
        ClientTickEvents.END_CLIENT_TICK.register(ConfigMenuQa::tick);
    }
    private static void tick(Minecraft mc){
        if(finished)return;
        try{
            Object screen=current(mc);
            if(phase==0){
                if(screen==null||overlay(mc)!=null)return;
                if(++ticks<20)return;
                boolean fabric=loader.equals("fabric");
                if(fabric&&"true".equals(System.getenv("SIU_QA_CONFIG_WITHOUT_MODMENU"))){
                    check("core-started-without-modmenu",!com.salts_inventory_update.api.integration.DesktopEnvironment.isModLoaded("modmenu"));
                    parent=screen;
                }else if(fabric){
                    boolean available;
                    try{available=(Boolean)call(type("com.terraformersmc.modmenu.ModMenu"),"hasConfigScreen","salts_inventory_update");}
                    catch(IllegalArgumentException legacy){available=call(type("com.terraformersmc.modmenu.ModMenu"),"getConfigScreen","salts_inventory_update",screen)!=null;}
                    if("true".equals(System.getenv("SIU_QA_CONFIG_EXPECT_MISSING"))){
                        check("reproduced-missing-core-factory",!available);finish(mc);return;
                    }
                    check("modmenu-core-factory-available",available);
                    parent=construct(type("com.terraformersmc.modmenu.gui.ModsScreen"),screen);
                }else{
                    String prefix=loader.equals("forge")?"net.minecraftforge":"net.neoforged";
                    container=((Optional<?>)call(call(type(prefix+".fml.ModList"),"get"),"getModContainerById","salts_inventory_update")).orElseThrow();
                    Class<?> factoryType=type(loader.equals("forge")?"net.minecraftforge.client.ConfigScreenHandler$ConfigScreenFactory":"net.neoforged.neoforge.client.gui.IConfigScreenFactory");
                    factory=((Optional<?>)call(container,"getCustomExtension",factoryType)).orElseThrow();
                    check("native-core-factory-registered",factory!=null);
                    String modList=loader.equals("forge")?"net.minecraftforge.client.gui.ModListScreen":
                        VersionInfo.MINECRAFT_VERSION.equals("26.2")||VersionInfo.MINECRAFT_VERSION.equals("26.3")?"net.neoforged.neoforge.client.gui.modlist.ModListScreen":"net.neoforged.neoforge.client.gui.ModListScreen";
                    try{parent=construct(type(modList),screen);}
                    catch(IllegalArgumentException modern){parent=call(type(modList),"create",screen);}
                }
                set(mc,parent);next();
            }else if(++ticks>=12){
                switch(phase){
                    case 1,4 -> {open(mc);next();}
                    case 2,5 -> {
                        check("config-open-"+phase,current(mc).getClass().getName().equals("com.salts_inventory_update.client.SaltsInventoryConfigScreen"));
                        check("parent-retained-"+phase,field(current(mc),"previousScreen")==parent);
                        check("config-controls-rendered-"+phase,((List<?>)call(current(mc),"children")).size()>5);
                        capture(mc,"config-open-"+phase);
                        call(current(mc),"onClose");next();
                    }
                    case 3 -> {check("first-close-returns-to-mod-list",current(mc)==parent);next();}
                    case 6 -> {check("reopen-close-returns-to-mod-list",current(mc)==parent);if(pending==0)finish(mc);}
                    default -> throw new IllegalStateException("Unexpected phase "+phase);
                }
            }
        }catch(Throwable failure){record("menu-flow","FAIL",failure.toString());finish(mc);}
    }
    private static void next(){phase++;ticks=0;}
    private static void open(Minecraft mc){
        if(loader.equals("fabric")){
            if("true".equals(System.getenv("SIU_QA_CONFIG_WITHOUT_MODMENU"))){set(mc,com.salts_inventory_update.api.client.desktop.DesktopClientApi.createConfigScreen((net.minecraft.client.gui.screens.Screen)parent));return;}
            try{call(parent,"safelyOpenConfigScreen","salts_inventory_update");}
            catch(IllegalArgumentException legacy){set(mc,call(type("com.terraformersmc.modmenu.ModMenu"),"getConfigScreen","salts_inventory_update",parent));}
        }
        else if(loader.equals("forge")){
            @SuppressWarnings("unchecked") var function=(java.util.function.BiFunction<Object,Object,Object>)call(factory,"screenFunction");
            set(mc,function.apply(mc,parent));
        }else set(mc,call(factory,"createScreen",container,parent));
    }
    private static Object current(Minecraft mc){try{return call(field(mc,"gui"),"screen");}catch(IllegalArgumentException old){return field(mc,"screen");}}
    private static Object overlay(Minecraft mc){try{return call(mc,"getOverlay");}catch(IllegalArgumentException modern){return call(field(mc,"gui"),"overlay");}}
    private static Object renderTarget(Minecraft mc){try{return call(mc,"getMainRenderTarget");}catch(IllegalArgumentException modern){return call(field(mc,"gameRenderer"),"mainRenderTarget");}}
    private static void set(Minecraft mc,Object screen){try{call(field(mc,"gui"),"setScreen",screen);}catch(IllegalArgumentException old){call(mc,"setScreen",screen);}}
    private static void check(String name,boolean value){record(name,value?"PASS":"FAIL","assertion "+value);if(!value)throw new IllegalStateException(name);}
    private static void record(String name,String status,String detail){results.add(Map.of("name",name,"status",status,"details",detail));}
    private static void finish(Minecraft mc){
        if(finished)return;finished=true;
        try{Files.createDirectories(output);Files.writeString(output.resolve("results.json"),new GsonBuilder().setPrettyPrinting().create().toJson(Map.of("scope","config-menu","minecraft",VersionInfo.MINECRAFT_VERSION,"loader",loader,"failures",results.stream().filter(r->r.get("status").equals("FAIL")).count(),"results",results,"coreCodeSource",com.salts_inventory_update.SaltsInventoryUpdate.class.getProtectionDomain().getCodeSource().getLocation().toString())));}
        catch(Exception failure){throw new IllegalStateException(failure);}
        call(mc,"stop");
    }
    private static void capture(Minecraft mc,String name){
        Path file=output.resolve(name+".png");
        Consumer<Object> write=image->{try{Files.createDirectories(output);try{call(image,"writeToFile",file);}catch(IllegalArgumentException old){call(image,"writeToFile",file.toFile());}}catch(Exception failure){record(name,"FAIL",failure.toString());}finally{call(image,"close");}};
        try{write.accept(call(type("net.minecraft.client.Screenshot"),"takeScreenshot",renderTarget(mc)));}
        catch(IllegalArgumentException modern){pending++;call(type("net.minecraft.client.Screenshot"),"takeScreenshot",renderTarget(mc),(Consumer<Object>)image->{try{write.accept(image);}finally{pending--;}});}
    }
    private static Class<?> type(String name){try{return Class.forName(name);}catch(Exception failure){throw new IllegalStateException(failure);}}
    private static Object construct(Class<?> type,Object...args){for(Constructor<?> c:type.getDeclaredConstructors())if(matches(c.getParameterTypes(),args))try{c.setAccessible(true);return c.newInstance(args);}catch(ReflectiveOperationException failure){throw new IllegalStateException(failure);}throw new IllegalArgumentException("Constructor: "+type);}
    private static Object field(Object target,String name){for(Class<?> c=target.getClass();c!=null;c=c.getSuperclass())try{Field f=c.getDeclaredField(name);f.setAccessible(true);return f.get(target);}catch(NoSuchFieldException ignored){}catch(ReflectiveOperationException failure){throw new IllegalStateException(failure);}throw new IllegalArgumentException("Field: "+name);}
    private static Object call(Object target,String name,Object...args){Class<?> cls=target instanceof Class<?> t?t:target.getClass();for(Class<?> c=cls;c!=null;c=c.getSuperclass())for(Method m:c.getDeclaredMethods())if(m.getName().equals(name)&&matches(m.getParameterTypes(),args))try{m.setAccessible(true);return m.invoke(target instanceof Class<?>?null:target,args);}catch(InvocationTargetException failure){throw new IllegalStateException(failure.getCause());}catch(ReflectiveOperationException failure){throw new IllegalStateException(failure);}for(Method m:cls.getMethods())if(m.getName().equals(name)&&matches(m.getParameterTypes(),args))try{m.setAccessible(true);return m.invoke(target instanceof Class<?>?null:target,args);}catch(ReflectiveOperationException failure){throw new IllegalStateException(failure);}throw new IllegalArgumentException("Method: "+cls+"."+name);}
    private static boolean matches(Class<?>[] types,Object[] args){if(types.length!=args.length)return false;for(int i=0;i<types.length;i++)if(args[i]!=null&&!types[i].isInstance(args[i]))return false;return true;}
}
