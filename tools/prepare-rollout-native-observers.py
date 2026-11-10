"""Generate excluded, SIU-free native observers for newer Minecraft clients."""
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
BASE=ROOT/'functional-tests/remaining-ui-audit/native/java-1.21.1/com/siu_reference/NativeUiReference.java'
source=BASE.read_text()
harness=(ROOT/'functional-tests/src/main/java/com/salts_inventory_update/functionaltest/InWorldGameplayHarness.java').read_text()
start=harness.index('        String name="siu-qa-"')
end=harness.index('\n    }',start)
world='    static void createWorld(){\n'+harness[start:end].replace('siu-qa-','native-reference-')+'\n    }\n'
helpers=r'''
    static Class<?> ClassFor(String name){try{return Class.forName(name);}catch(ClassNotFoundException e){throw new IllegalStateException(e);}}
    @SuppressWarnings({"unchecked","rawtypes"}) static Object enumValue(String type,String name){return Enum.valueOf((Class)ClassFor(type),name);}
    static Object construct(Class<?> type,Object...args){
        for(var ctor:type.getDeclaredConstructors())if(ctor.getParameterCount()==args.length)try{ctor.setAccessible(true);return ctor.newInstance(args);}
        catch(IllegalArgumentException mismatch){}catch(ReflectiveOperationException e){throw new IllegalStateException(e);}
        throw new IllegalStateException("Native constructor unavailable: "+type);
    }
    static Object call(Object owner,String name,Object...args){
        Class<?> type=owner instanceof Class<?> t?t:owner.getClass();
        var methods=new ArrayList<Method>(List.of(type.getMethods()));
        for(Class<?> t=type;t!=null;t=t.getSuperclass())methods.addAll(List.of(t.getDeclaredMethods()));
        for(var method:methods)if(method.getName().equals(name)&&method.getParameterCount()==args.length)try{method.setAccessible(true);return method.invoke(owner instanceof Class<?>?null:owner,args);}
        catch(IllegalArgumentException mismatch){}catch(ReflectiveOperationException e){throw new IllegalStateException(e);}
        throw new IllegalArgumentException("Native method unavailable: "+type+"."+name);
    }
    static net.minecraft.client.gui.screens.Screen screen(){
        try{return (net.minecraft.client.gui.screens.Screen)field(mc,"screen");}
        catch(IllegalArgumentException moved){return (net.minecraft.client.gui.screens.Screen)call(field(mc,"gui"),"screen");}
    }
    static void setScreen(net.minecraft.client.gui.screens.Screen screen){
        try{call(mc,"setScreen",screen);}catch(IllegalArgumentException moved){call(field(mc,"gui"),"setScreen",screen);}
    }
    static Object overlay(){try{return call(mc,"getOverlay");}catch(IllegalArgumentException moved){return call(field(mc,"gui"),"overlay");}}
    static Object renderTarget(){try{return call(mc,"getMainRenderTarget");}catch(IllegalArgumentException moved){return call(field(mc,"gameRenderer"),"mainRenderTarget");}}
    static net.minecraft.server.level.ServerLevel serverLevel(){try{return (net.minecraft.server.level.ServerLevel)call(player(),"serverLevel");}catch(IllegalArgumentException moved){return (net.minecraft.server.level.ServerLevel)call(player(),"level");}}
    static void resizeGui(){try{call(mc,"resizeDisplay");}catch(IllegalArgumentException moved){call(mc,"resizeGui");}}
    static void clearCaptureMessages(){
        for(String name:List.of("getToasts","getToastManager"))try{call(call(mc,name),"clear");break;}catch(IllegalArgumentException ignored){}
        Object gui=field(mc,"gui");try{call(call(gui,"getChat"),"clearMessages",false);}catch(IllegalArgumentException moved){try{call(call(field(gui,"hud"),"getChat"),"clearMessages",false);}catch(IllegalArgumentException ignored){}}
    }
    static void writeImage(Object image,Path path){
        try{call(image,"writeToFile",path);}finally{call(image,"close");}
    }
'''
for version in ['1.21.11','26.1.2','26.2','26.3']:
    text=source.replace('ResourceLocation','Identifier')
    text=text.replace('import java.util.function.Supplier;','import java.util.function.Supplier;\nimport java.util.function.Function;')
    a=text.index('                var settings=new LevelSettings(');b=text.index('                stage=1;return;',a)
    text=text[:a]+'                createWorld();\n'+text[b:]
    text=text.replace('for(Class<?> t=o.getClass();','for(Class<?> t=o instanceof Class<?> c?c:o.getClass();').replace('return f.get(o);','return f.get(o instanceof Class<?>?null:o);')
    text=text.replace('player().connection.teleport(8.5,65,12.5,180,8);','command("tp @a 8.5 65 12.5 180 8");')
    text=text.replace('mc.screen','screen()').replace('mc.setScreen(','setScreen(')
    text=text.replace('mc.getOverlay()','overlay()').replace('mc.resizeDisplay()','resizeGui()').replace('mc.getMainRenderTarget()','renderTarget()').replace('player().serverLevel()','serverLevel()')
    for registry in ['ITEM','BLOCK']:
        text=text.replace('BuiltInRegistries.'+registry+'.get(','BuiltInRegistries.'+registry+'.getValue(')
    text=text.replace('player().getInventory().selected','player().getInventory().getSelectedSlot()')
    for assignment in ['mc.player.input.shiftKeyDown=down;','mc.player.input.shiftKeyDown=shift;','mc.player.input.shiftKeyDown=false;']:
        text=text.replace(assignment,'')
    # Deliberately omit the old forced-provider diagnostic from screenshot runs.
    text='\n'.join(line for line in text.splitlines() if 'case "provider"->' not in line and 'case "slot"->' not in line)+'\n'
    button='(num(c,"button",0)==0?1:num(c,"button",0)==1?3:2)' if version=='26.3' else 'num(c,"button",0)'
    event='new net.minecraft.client.input.MouseButtonEvent(c.get("x").getAsDouble(),c.get("y").getAsDouble(),new net.minecraft.client.input.MouseButtonInfo('+button+',0))'
    text=text.replace('screen().mouseClicked(c.get("x").getAsDouble(),c.get("y").getAsDouble(),num(c,"button",0))','screen().mouseClicked('+event+',false)')
    text=text.replace('case "capture", "wait"->{}',f'''case "release"->screen().mouseReleased({event});
            case "move"->screen().mouseMoved(c.get("x").getAsDouble(),c.get("y").getAsDouble());
            case "scroll"->screen().mouseScrolled(c.get("x").getAsDouble(),c.get("y").getAsDouble(),0,c.get("amount").getAsDouble());
            case "capture"->clearCaptureMessages();
            case "wait"->{{}}''')
    if version.startswith('26.'):
        text=text.replace('var result=mc.gameMode.interactAt(mc.player,entity,hit,InteractionHand.MAIN_HAND);\n                if(!result.consumesAction())mc.gameMode.interact(mc.player,entity,InteractionHand.MAIN_HAND);','mc.gameMode.interact(mc.player,entity,hit,InteractionHand.MAIN_HAND);')
        text=text.replace('mc.gameMode.interact(mc.player,entity,InteractionHand.MAIN_HAND);','mc.gameMode.interact(mc.player,entity,new EntityHitResult(entity),InteractionHand.MAIN_HAND);')
    text=text.replace('        mc.getToasts().clear();var file=', '        clearCaptureMessages();var file=')
    text=text.replace('        try(var image=Screenshot.takeScreenshot(renderTarget())){image.writeToFile(file);}',r'''        try{Object image=call(Screenshot.class,"takeScreenshot",renderTarget());writeImage(image,file);}
        catch(IllegalArgumentException asynchronous){call(Screenshot.class,"takeScreenshot",renderTarget(),(java.util.function.Consumer<Object>)image->writeImage(image,file));}''')
    text=text[:text.rfind('}')]+world+helpers+'}\n'
    path=ROOT/'build/all-target-rollout/native-observers'/('java-'+version)/'com/siu_reference/NativeUiReference.java'
    path.parent.mkdir(parents=True,exist_ok=True);path.write_text(text)
    print('Prepared SIU-free observer',version)

init=(ROOT/'functional-tests/remaining-ui-audit/native/qa.init.gradle').read_text()
init=init.replace("if(!((minecraft=='1.20.1' && loader in ['fabric','forge']) || (minecraft=='1.21.1' && loader=='neoforge')) || !(runId ==~ /[a-z0-9-]+/))", "if(!(minecraft in ['1.20.1','1.21.1','1.21.11','26.1.2','26.2','26.3']) || !(loader in ['fabric','forge','neoforge']) || !(runId ==~ /[a-z0-9-]+/))")
init=init.replace("minecraft=='1.20.1'?'functional-tests/remaining-ui-audit/native/java':'functional-tests/remaining-ui-audit/native/java-1.21.1'", "minecraft=='1.20.1'?'functional-tests/remaining-ui-audit/native/java':minecraft=='1.21.1'?'functional-tests/remaining-ui-audit/native/java-1.21.1':\"build/all-target-rollout/native-observers/java-${minecraft}\"")
init=init.replace("depends:[minecraft:'1.20.1','fabric-api':'*']", "depends:[minecraft:minecraft,'fabric-api':'*']")
init=init.replace("minecraft=='1.20.1'?15:48", "minecraft=='1.20.1'?15:minecraft=='1.21.1'?48:999")
init=init.replace("it.name!='runClient'&&it.name!='classes'", "!(it.name in ['runClient','classes','compileJava','processResources'])")
init=init.replace("maxHeapSize='3g'", "maxHeapSize=root.providers.gradleProperty('nativeHeap').getOrElse('3g')")
init=init.replace("if(loader=='fabric')jvmArgs+=['-XX:ActiveProcessorCount=2']", "jvmArgs+=['-XX:ActiveProcessorCount=2']")
init=init.replace("            new File(output,'staged-artifacts.json').text=", """            // Disposable capture worlds acknowledge only the welcome page.
            // Leave the publisher's other client preferences at their defaults.
            if(inputs.any{it.filename.toLowerCase().contains('wover') || it.filename.toLowerCase().contains('worldweaver')}){
                def settings=new File(workingDir,'config/wover/client.json');settings.parentFile.mkdirs()
                settings.text=JsonOutput.toJson([internal:[did_present_welcome_screen:true]])
                new File(output,'welcome-fixture-setting.json').text=JsonOutput.toJson([scope:'Disposable native screenshot fixture only',file:'config/wover/client.json',internal:[did_present_welcome_screen:true]])
            }
            new File(output,'staged-artifacts.json').text=""")
(ROOT/'functional-tests/all-target-rollout/native.init.gradle').write_text(init)
