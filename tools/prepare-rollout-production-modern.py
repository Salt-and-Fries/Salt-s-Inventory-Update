"""Run 1.20.1 or 1.21.11 Fabric with original release JARs and an excluded observer.

Only the observation classes are remapped. This avoids development remapping
conflicts in publisher JARs and keeps Fabric's ordinary production class loader.
"""
from pathlib import Path
import argparse,hashlib,importlib.util,json,os,re,shutil,subprocess,zipfile

ROOT=Path(__file__).resolve().parents[1]
spec=importlib.util.spec_from_file_location('production_reference',ROOT/'tools/prepare-rollout-production-fabric.py')
base=importlib.util.module_from_spec(spec);spec.loader.exec_module(base)
MC='1.21.11'
base.CACHE=ROOT/'build/all-target-rollout/production-cache-12111'
base.MC_CACHE=Path.home()/'.gradle/caches/fabric-loom'/MC
CACHE=base.CACHE
MAPPINGS=base.MC_CACHE/'loom.mappings.1_21_11.layered+hash.2198-v2/mappings.tiny'

def retain_identical_input(staged,family,file,filename):
    """A publisher-pinned runtime API and the platform API can be one file."""
    previous=next((r for r in staged if r['filename']==filename),None)
    if previous is None:return False
    assert base.digest(file,'sha512')==previous['sha512'], 'Conflicting staged binaries named '+filename
    roles=previous.setdefault('also_families',[previous['family']])
    if family not in roles:roles.append(family)
    previous.setdefault('identical_sources',[]).append({'family':family,'file':str(file.resolve())})
    if family=='runtime-api':previous['family']=family
    return True

RESOLVER=r'''package com.siu_reference;
import com.google.gson.*;
import java.io.*;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
/** Names used by this excluded observer's reflection, never by subject mods. */
final class RuntimeNames {
    private static final JsonObject DATA;
    static {
        try(var stream=RuntimeNames.class.getResourceAsStream("/runtime-names.json")){
            DATA=JsonParser.parseReader(new InputStreamReader(Objects.requireNonNull(stream),StandardCharsets.UTF_8)).getAsJsonObject();
        }catch(IOException error){throw new ExceptionInInitializerError(error);}
    }
    static String className(String name){var value=DATA.getAsJsonObject("classes").get(name);return value==null?name:value.getAsString();}
    static Set<String> names(Class<?> type,String name,String kind){
        var result=new LinkedHashSet<String>();result.add(name);collect(type,name,kind,result,new HashSet<>());return result;
    }
    private static void collect(Class<?> type,String name,String kind,Set<String> out,Set<Class<?>> seen){
        if(type==null||!seen.add(type))return;
        var owner=DATA.getAsJsonObject("members").getAsJsonObject(type.getName());
        if(owner!=null){var values=owner.getAsJsonObject(kind).getAsJsonArray(name);if(values!=null)for(var value:values)out.add(value.getAsString());}
        collect(type.getSuperclass(),name,kind,out,seen);for(var face:type.getInterfaces())collect(face,name,kind,out,seen);
    }
    static Field field(Class<?> type,String name)throws NoSuchFieldException{
        for(String candidate:names(type,name,"fields"))try{return type.getDeclaredField(candidate);}catch(NoSuchFieldException absent){}
        throw new NoSuchFieldException(type.getName()+"."+name);
    }
}
'''

def runtime_names():
    lines=MAPPINGS.read_text().splitlines();namespaces=lines[0].split('\t')[3:]
    named=namespaces.index('named');runtime=namespaces.index('intermediary')
    data={'classes':{},'members':{}};owner=None
    for line in lines[1:]:
        parts=line.split('\t')
        if parts[0]=='c':
            owner=parts[1+runtime].replace('/','.')
            data['classes'][parts[1+named].replace('/','.')]=owner
            data['members'][owner]={'fields':{},'methods':{}}
        elif owner and len(parts)>4 and parts[0]=='' and parts[1] in ('f','m'):
            group=data['members'][owner]['fields' if parts[1]=='f' else 'methods']
            values=group.setdefault(parts[3+named],[]);value=parts[3+runtime]
            if value not in values:values.append(value)
    return json.dumps(data,separators=(',',':'))

def qa_source(side):
    source=(ROOT/'functional-tests/remaining-ui-audit/native/java/com/siu_reference/NativeUiReference.java'
            if MC in ['1.20.1','1.21.1'] else ROOT/f'build/all-target-rollout/native-observers/java-{MC}/com/siu_reference/NativeUiReference.java')
    text=source.read_text()
    forbidden='for(String id:List.of("salts_inventory_update","salts_inventory_compat","salts_inventory_controlify"))'
    assert forbidden in text
    text=text.replace(forbidden,'for(String id:coreMode()?List.of("salts_inventory_controlify"):List.of("salts_inventory_update","salts_inventory_compat","salts_inventory_controlify"))')
    text=text.replace('mc.options.pauseOnLostFocus=false;','configureCore();mc.options.pauseOnLostFocus=false;',1)
    text=text.replace('static Map<String,Object> state(){','static Map<String,Object> state(){\n        if(coreMode())return coreState();',1)
    text=text.replace('mc.player.closeContainer();','closeCore();mc.player.closeContainer();')
    text=text.replace('"siu_loaded",false','"siu_loaded",coreMode()')
    text=text.replace('Class.forName(name)','Class.forName(RuntimeNames.className(name))')
    text=text.replace('Field f=t.getDeclaredField(name);','Field f=RuntimeNames.field(t,name);')
    text=text.replace('method.getName().equals(name)','RuntimeNames.names(type,name,"methods").contains(method.getName())')
    extra=base.EXTRA_JAVA
    if MC=='1.21.11':
        extra=extra.replace('.save(new net.minecraft.nbt.CompoundTag())','.getComponents()')
        extra=extra.replace('mc.player.getInventory().selected','mc.player.getInventory().getSelectedSlot()')
    extra=extra.replace('type.getDeclaredField(name)','RuntimeNames.field(type,name)')
    extra=extra.replace('m.getName().equals(name)','RuntimeNames.names(c,name,"methods").contains(m.getName())')
    extra=extra.replace('method.getName().equals(name)','RuntimeNames.names(type,name,"methods").contains(method.getName())')
    extra=extra.replace('row.put("template",templateName);','row.put("templateClass",templateName);row.put("template",G.toJsonTree(field(definition,"plan")));')
    extra=extra.replace('out.put("allWindows",windows);','out.put("windows",windows);out.put("allWindows",windows);')
    extra=extra.replace('if(definition!=null)try{row.put("composition",',
        'if(definition!=null)try{row.put("layoutError",field(definition,"layoutError"));row.put("nativeSlots",G.toJsonTree(coreCall(field(definition,"view"),"slots")));}catch(Exception ignored){}\n                if(definition!=null)try{row.put("composition",',1)
    assert 'return values;' in extra
    extra=extra.replace('return values;',
        'if(screen instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> nativeContainer)for(String name:List.of("getPageIndex","getPageCount","getRowCount"))try{values.put(name,coreCall(nativeContainer.getMenu(),name));}catch(Exception ignored){}return values;',1)
    extra=extra.replace('out.put("worldLoaded",mc.player!=null&&mc.level!=null);','out.put("worldLoaded",mc.player!=null&&mc.level!=null);if(mc.player!=null)out.put("playerPosition",mc.player.position().toString());')
    extra=extra.replace('return out;\n    }\n    static void auditWidgets',
        'if(mc.screen!=null&&windows.isEmpty()){var widgets=new ArrayList<Object>();auditWidgets(mc.screen,widgets,0);out.put("widgets",widgets);}\n        return out;\n    }\n    static void auditWidgets',1)
    text=text[:text.rfind('}')]+extra+'\n}\n'
    text=text.replace('case "command"->','case "server-command", "command"->')
    text=text.replace('case "open"->','case "client-use-block", "open"->')
    text=text.replace('case "use-item"->','case "client-use-item", "use-item"->')
    text=text.replace('case "use-entity"->','case "client-use-entity", "use-entity"->')
    text=text.replace('case "inventory"->{setScreen(new InventoryScreen(mc.player));}',
        'case "inventory"->{if(coreMode()){try{Class.forName("com.salts_inventory_update.api.client.desktop.DesktopClientApi").getMethod("openPlayerWorkspace").invoke(null);}catch(ReflectiveOperationException error){throw new IllegalStateException(error);}}else setScreen(new InventoryScreen(mc.player));}')
    if MC in ['1.20.1','1.21.1']:
        text=text.replace('case "snapshot-server"', '''case "mouse-press"->{coreCall(mc.mouseHandler,"onPress",mc.getWindow().getWindow(),num(c,"button",0),1,num(c,"modifiers",0));}
            case "mouse-release"->{coreCall(mc.mouseHandler,"onPress",mc.getWindow().getWindow(),num(c,"button",0),0,num(c,"modifiers",0));}
            case "snapshot-server"''',1)
        move='case "move"->{mc.screen.mouseMoved('
        assert move in text
        text=text.replace(move,'''case "move"->{try{
                var mouseX=RuntimeNames.field(mc.mouseHandler.getClass(),"xpos");mouseX.setAccessible(true);
                var mouseY=RuntimeNames.field(mc.mouseHandler.getClass(),"ypos");mouseY.setAccessible(true);
                mouseX.setDouble(mc.mouseHandler,c.get("x").getAsDouble()*mc.getWindow().getWidth()/mc.getWindow().getGuiScaledWidth());
                mouseY.setDouble(mc.mouseHandler,c.get("y").getAsDouble()*mc.getWindow().getHeight()/mc.getWindow().getGuiScaledHeight());
            }catch(ReflectiveOperationException error){throw new IllegalStateException(error);}mc.screen.mouseMoved(''',1)
        text=text.replace('case "inventory"->{mc.setScreen(new InventoryScreen(mc.player));}',
            'case "inventory"->{if(coreMode()){try{Class.forName("com.salts_inventory_update.api.client.desktop.DesktopClientApi").getMethod("openPlayerWorkspace").invoke(null);}catch(ReflectiveOperationException error){throw new IllegalStateException(error);}}else mc.setScreen(new InventoryScreen(mc.player));}')
        text=text.replace('case "move"->','case "xy-click"->{mc.screen.mouseClicked(c.get("x").getAsDouble(),c.get("y").getAsDouble(),num(c,"button",0));mc.screen.mouseReleased(c.get("x").getAsDouble(),c.get("y").getAsDouble(),num(c,"button",0));}\n            case "move"->')
    else:
        text=text.replace('case "move"->', '''case "text"->{for(char letter:str(c,"text","").toCharArray())screen().charTyped(new net.minecraft.client.input.CharacterEvent(letter,0));}
            case "snapshot-server"->work=server(()->{
                var inventory=new ArrayList<Object>();
                for(int i=0;i<player().getInventory().getContainerSize();i++){
                    var item=player().getInventory().getItem(i);
                    inventory.add(Map.of("index",i,"item",G.toJson(Map.of("id",BuiltInRegistries.ITEM.getKey(item.getItem()).toString(),"count",item.getCount()))));
                }
                var carried=player().containerMenu.getCarried();
                write("server-state.json",Map.of("inventory",inventory,"menus",List.of(Map.of("carried",G.toJson(Map.of("id",BuiltInRegistries.ITEM.getKey(carried.getItem()).toString(),"count",carried.getCount()))))));
                return true;
            });
            case "key"->screen().keyPressed(new net.minecraft.client.input.KeyEvent(num(c,"key",256),0,num(c,"modifiers",0)));
            case "drag"->screen().mouseDragged(new net.minecraft.client.input.MouseButtonEvent(c.get("x").getAsDouble(),c.get("y").getAsDouble(),new net.minecraft.client.input.MouseButtonInfo(num(c,"button",0),0)),c.get("dx").getAsDouble(),c.get("dy").getAsDouble());
            case "xy-click"->{var event=new net.minecraft.client.input.MouseButtonEvent(c.get("x").getAsDouble(),c.get("y").getAsDouble(),new net.minecraft.client.input.MouseButtonInfo(num(c,"button",0),0));screen().mouseClicked(event,false);screen().mouseReleased(event);}
            case "move"->''')
    if MC=='1.21.1':
        text=text.replace('createWorldDimensions());','createWorldDimensions(),new TitleScreen());')
        text=text.replace('new ResourceLocation(s)','ResourceLocation.parse(s)')
        text=text.replace('stack.setTag(net.minecraft.nbt.TagParser.parseTag(c.get("snbt").getAsString()))','stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.of(net.minecraft.nbt.TagParser.parseTag(c.get("snbt").getAsString())))')
        text=text.replace('.saveWithFullMetadata()', '.saveWithFullMetadata(mc.level.registryAccess())')
        text=text.replace('.save(new net.minecraft.nbt.CompoundTag())','.saveOptional(mc.level.registryAccess())')
        start=text.index('case "snapshot-server"->work=server(')
        end=text.index('case "slot"',start)
        text=text[:start]+text[start:end].replace('mc.level.registryAccess()','player().registryAccess()')+text[end:]
        text=text.replace('c.get("y").getAsDouble(),c.get("amount").getAsDouble())','c.get("y").getAsDouble(),0,c.get("amount").getAsDouble())')
        text=text.replace('inventory=coreCall(handler,"getInventory")','inventory=coreCall(handler,"getContainer")')
    return source,text

def prepare_qa(side,classpath_file):
    source,text=qa_source(side);aliases=runtime_names()
    identity=hashlib.sha256((text+RESOLVER+aliases).encode()).hexdigest()[:16]
    folder=CACHE/'qa'/identity;mapped=folder/'audit-intermediary.jar'
    if mapped.exists():return mapped
    sources=folder/'source/com/siu_reference';sources.mkdir(parents=True,exist_ok=True)
    (sources/'NativeUiReference.java').write_text(text)
    (sources/'RuntimeNames.java').write_text(RESOLVER)
    (sources/'NativeReferenceFabric.java').write_text('''package com.siu_reference;
public final class NativeReferenceFabric implements net.fabricmc.api.ClientModInitializer {
 public void onInitializeClient(){net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(mc->NativeUiReference.tick());}
}''')
    cp=base.read(classpath_file)['classpath'];classes=folder/'classes';classes.mkdir(exist_ok=True)
    args=['--release','17' if MC=='1.20.1' else '21','-proc:none','-encoding','UTF-8','-cp',';'.join(cp),'-d',str(classes),*map(str,sources.glob('*.java'))]
    argfile=folder/'javac.args';argfile.write_text('\n'.join(map(base.java_argument,args)))
    with (folder/'compile.log').open('w') as log:
        subprocess.run([str(base.JAVA.with_name('javac.exe')),'@'+str(argfile.resolve())],stdout=log,stderr=subprocess.STDOUT,check=True)
    original=folder/'audit-named.jar'
    with zipfile.ZipFile(original,'w',zipfile.ZIP_DEFLATED) as jar:
        for path in classes.rglob('*.class'):jar.write(path,path.relative_to(classes).as_posix())
        jar.writestr('runtime-names.json',aliases)
        jar.writestr('fabric.mod.json',json.dumps({'schemaVersion':1,'id':'native_ui_reference','version':'1.0.0','environment':'client','entrypoints':{'client':['com.siu_reference.NativeReferenceFabric']},'depends':{'minecraft':MC,'fabric-api':'*'}}))
    tiny=next(p for p in (base.GRADLE/'net.fabricmc/tiny-remapper/0.14.1').glob('*/*.jar') if not p.name.endswith('-sources.jar'))
    mapping=next(p for p in (base.GRADLE/'net.fabricmc/mapping-io/0.8.0').glob('*/*.jar') if not p.name.endswith('-sources.jar'))
    remapper_cp=[str(tiny),str(mapping)]+[s for s in cp if 'org.ow2.asm' in s]
    minecraft=next(s for s in cp if Path(s).name.startswith('minecraft-merged-'))
    with (folder/'remap.log').open('w') as log:
        subprocess.run([str(base.JAVA),'-cp',';'.join(remapper_cp),'net.fabricmc.tinyremapper.Main',str(original),str(mapped),str(MAPPINGS),'named','intermediary',minecraft],stdout=log,stderr=subprocess.STDOUT,check=True)
    (folder/'lineage.json').write_text(json.dumps({'scope':'Only excluded observation classes remapped; subject publisher/Core/Compatibility bytes untouched','source':str(source),'source_sha256':base.digest(source),'named_qa_sha256':base.digest(original),'intermediary_qa_sha256':base.digest(mapped),'mappings_sha256':base.digest(MAPPINGS)},indent=2))
    return mapped

def main():
    global MC,CACHE,MAPPINGS
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--fixture',type=Path,required=True)
    p.add_argument('--minecraft',choices=['1.20.1','1.21.1','1.21.11'],default='1.21.11')
    p.add_argument('--side',choices=['native','siu'],required=True);p.add_argument('--run',required=True)
    p.add_argument('--classpath-from',type=Path,default=ROOT/'build/all-target-rollout/gameplay/1.21.11-fabric-rollout-inventory-batch-02/launch-details.json')
    p.add_argument('--core-jar',type=Path);p.add_argument('--compat-jar',type=Path)
    p.add_argument('--heap',choices=['2g','4g','6g'],default='2g',help='Disposable test-client heap; subject release bytes remain unchanged')
    p.add_argument('--java',type=Path,default=base.JAVA)
    p.add_argument('--launch',action='store_true');p.add_argument('--plain-anvil-reference',action='store_true');a=p.parse_args()
    MC=a.minecraft;CACHE=ROOT/('build/all-target-rollout/production-cache-'+MC.replace('.',''))
    base.CACHE=CACHE;base.MC_CACHE=Path.home()/'.gradle/caches/fabric-loom'/MC
    MAPPINGS=base.MC_CACHE/f'loom.mappings.{MC.replace(".","_")}.layered+hash.2198-v2/mappings.tiny'
    assert re.fullmatch('[a-z0-9-]+',a.run)
    fixture=base.read(a.fixture);assert (fixture['minecraft'],fixture['loader'])==(MC,'fabric')
    output=ROOT/'build/remaining-ui-audit/gameplay'/f'{MC}-fabric-{a.side}-{a.run}'
    runtime=Path.home()/'.cache/siu-rollout-runtime'/f'{MC}-fabric-{a.side}-{a.run}'
    assert not output.exists() and not runtime.exists(),'Preserve previous evidence; choose a fresh run ID'
    qa=prepare_qa(a.side,a.classpath_from)
    version=base.read(base.MC_CACHE/'minecraft-info.json');loader=fixture.get('runtime',{}).get('fabric_loader','0.19.5')
    profile_path=CACHE/f'fabric-loader-{loader}-{MC}.json'
    base.fetch(f'https://meta.fabricmc.net/v2/versions/loader/{MC}/{loader}/profile/json',profile_path)
    profile=base.read(profile_path);cp=[base.library(r,True) for r in profile['libraries']]
    cp += [base.library(r) for r in version['libraries'] if base.allowed(r) and 'artifact' in r.get('downloads',{})]
    client=base.MC_CACHE/'minecraft-client.jar';assert base.digest(client,'sha1')==version['downloads']['client']['sha1'];cp.append(client)
    assets=Path.home()/'.gradle/caches/fabric-loom/assets';index=f'rollout-{MC}-'+version['assetIndex']['id']
    base.fetch(version['assetIndex']['url'],assets/'indexes'/f'{index}.json',version['assetIndex']['sha1'])
    runtime.mkdir(parents=True);output.mkdir(parents=True);mods=runtime/'mods';mods.mkdir();inputs=[]
    for record in fixture['artifacts']:
        if not record.get('coordinate') and not record.get('stage_as_root', False):continue
        file=ROOT/record['path'];assert base.digest(file,'sha512')==record['sha512'];inputs.append(('upstream',file,record['filename']))
    api_meta=ROOT/f'build/all-target-rollout/metadata/fabric-api-{MC}-batch-versions.json'
    desired=fixture['runtime']['fabric_api'];release=next(r for r in base.read(api_meta) if r['version_number']==desired)
    api=next(r for r in release['files'] if r.get('primary'));file=base.fetch(api['url'],CACHE/api['filename'],api['hashes']['sha512'],'sha512')
    inputs += [('runtime-api',file,api['filename']),('qa',qa,'native-ui-reference.jar')]
    if a.side=='siu':
        for family,file in [('core',a.core_jar),('compatibility',a.compat_jar)]:
            assert file is not None and file.is_file(),f'An explicit built {family} JAR is required'
            inputs.append((family,file,file.name))
    staged=[]
    for family,file,filename in inputs:
        if retain_identical_input(staged,family,file,filename):continue
        shutil.copy2(file,mods/filename);staged.append({'family':family,'file':str(file.resolve()),'filename':filename,'sha256':base.digest(file),'sha512':base.digest(file,'sha512')})
    (runtime/'options.txt').write_text('tutorialStep:none\nrenderDistance:2\nsimulationDistance:5\nguiScale:2\npauseOnLostFocus:false\nautoJump:false\n')
    if MC=='1.20.1' and any('bclib' in record['filename'].lower() for record in staged):
        config=runtime/'config/bclib/client.json';config.parent.mkdir(parents=True,exist_ok=True)
        config.write_text(json.dumps({'version':{'didShowWelcome [default: false]':True,'check [default: true]':False}},indent=2))
        (output/'offline-reference-settings.json').write_text(json.dumps({
            'scope':'Disposable offline reference runtime only; publisher and subject JAR bytes unchanged',
            'bclib':'First-run setup already configured; online update checks disabled. Inventory UI settings retain their defaults.'},indent=2))
    if MC=='1.21.1' and any('worldweaver' in record['filename'].lower() or 'wover' in record['filename'].lower() for record in staged):
        config=runtime/'config/wover/client.json';config.parent.mkdir(parents=True,exist_ok=True)
        config.write_text(json.dumps({'create_version':'21.0.13','modify_version':'21.0.13','internal':{'did_present_welcome_screen':True},'general':{'check_for_new_versions':False,'prefere_modrinth':False,'force_betterx_world_type':True},'loading':{'disable_experimental_warning':False}},indent=2))
        (output/'offline-reference-settings.json').write_text(json.dumps({'scope':'Disposable offline reference profile only; original publisher and shipping JAR bytes unchanged','worldweaver':'The original first-run setup is configured and optional online update checks disabled. Inventory UI settings retain their defaults.'},indent=2))
    if MC=='1.20.1' and any(record['filename'].lower().startswith('supplementaries-') for record in staged):
        config=runtime/'config/supplementaries-client.json';config.parent.mkdir(parents=True,exist_ok=True)
        config.write_text(json.dumps({'general':{'no_amendments_screen':True}},indent=2))
        note=output/'offline-reference-settings.json'
        settings=json.loads(note.read_text()) if note.exists() else {'scope':'Disposable, newly generated reference world only; publisher JAR bytes and inventory UI defaults unchanged'}
        settings['supplementaries']='Suppress the optional Amendments migration reminder before creating the fresh test world; there is no existing world or migrated block data in this profile.'
        note.write_text(json.dumps(settings,indent=2))
    if a.plain_anvil_reference:
        assert a.side=='native'
        config=runtime/'config/easyanvils-server.toml';config.parent.mkdir(exist_ok=True)
        config.write_text('[miscellaneous]\nrenaming_supports_formatting=false\n')
        (output/'native-easyanvils-reference-setting.json').write_text(json.dumps({'publisher_option':'renaming_supports_formatting','value':False,'reason':'The default publisher formatting guide crashes on a boxed Character without SIU; unchanged native reference uses its documented option.'},indent=2))
    argv=['-Xms512m','-Xmx'+a.heap,'-XX:ActiveProcessorCount=2','-Dfabric.development=false','-Dfabric.noGui=true','-Dfabric.gameJarPath='+str(client),
          '-Djava.library.path='+str(runtime/'natives'),'-Dorg.lwjgl.system.SharedLibraryExtractPath='+str(runtime/'natives'),'-cp',';'.join(map(str,cp)),profile['mainClass'],
          '--username','UIAudit','--version',MC,'--gameDir',str(runtime),'--assetsDir',str(assets),'--assetIndex',index,
          '--uuid','14ea713fe12d3d8bb7b6baf52d52d598','--accessToken','0','--userType','legacy','--versionType','release','--width','1280','--height','900']
    argfile=output/'java.args';argfile.write_text('\n'.join(map(base.java_argument,argv)))
    environment={'NATIVE_REFERENCE_OUTPUT':str(output),'NATIVE_REFERENCE_LOADER':'fabric','SIU_AUDIT_CORE':str(a.side=='siu').lower()}
    launch={'java':str(a.java),'arguments':str(argfile.resolve()),'cwd':str(runtime),'environment':environment,'log':str(output/'launch.log')}
    (output/'launch.json').write_text(json.dumps(launch,indent=2))
    (output/'staged-artifacts.json').write_text(json.dumps({'minecraft':MC,'loader':'fabric','runtime':'production intermediary; original publisher and subject JAR bytes','siu_loaded':a.side=='siu','artifacts':staged},indent=2))
    if a.launch:
        with (output/'launch.log').open('w') as log:
            process=subprocess.Popen([str(a.java),'@'+str(argfile.resolve())],cwd=runtime,env={**os.environ,**environment},stdout=log,stderr=subprocess.STDOUT,creationflags=subprocess.CREATE_NO_WINDOW)
        (output/'process.json').write_text(json.dumps({'pid':process.pid,'launch':launch},indent=2));print('STARTED',process.pid,str(output))
    else:print('PREPARED',str(output))

if __name__=='__main__':main()
