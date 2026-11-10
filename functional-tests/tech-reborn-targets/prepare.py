"""Original publisher/subject bytes in disposable production Fabric profiles."""
from pathlib import Path
import argparse,ast,hashlib,importlib.util,json,os,shutil,subprocess,sys,urllib.request,zipfile
ROOT=Path(__file__).resolve().parents[2]
WORK=ROOT/'build/tech-reborn-all-targets'
def module(name,path):
    spec=importlib.util.spec_from_file_location(name,path);m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m);return m
prep=module('modern_production',ROOT/'tools/prepare-rollout-production-modern.py')
base=prep.base
IDENTITY='''package com.siu_reference;
import java.lang.reflect.Field;import java.util.Set;
final class RuntimeNames{static String className(String n){return n;}static Set<String> names(Class<?> t,String n,String k){return Set.of(n);}static Field field(Class<?> t,String n)throws NoSuchFieldException{return t.getDeclaredField(n);}}
'''
EXTRA=r'''
    private static void trObserve(){
        var values=new LinkedHashMap<String,Object>();var be=player().serverLevel().getBlockEntity(POS);
        for(String name:List.of("getEnergy","getHeat","getCachedHeat","getMaxStoredPower","getCurrentOutput","getCurrentRadius","getDepth","getRange","getLaunchSpeed"))try{values.put(name,String.valueOf(coreCall(be,name)));}catch(Exception absent){}
        try{var rows=new ArrayList<Object>();for(Object holder:(List<?>)coreCall(coreCall(be,"getSlotConfiguration"),"getSlotDetails")){
            var sides=new ArrayList<Object>();for(var side:net.minecraft.core.Direction.values())sides.add(String.valueOf(coreCall(coreCall(coreCall(holder,"getSideDetail",side),"getSlotIO"),"getIoConfig")));
            rows.add(Map.of("input",coreCall(holder,"autoInput"),"output",coreCall(holder,"autoOutput"),"filter",coreCall(holder,"filter"),"priority",coreCall(holder,"getPriority"),"sides",sides));}
            values.put("getSlotConfiguration",rows);}catch(Exception absent){values.put("slotError",absent.toString());}
        try{values.put("getRedstoneConfiguration",G.toJsonTree(coreCall(be,"getRedstoneConfiguration")));}catch(Exception absent){}
        try{Object config=field(be,"fluidConfiguration");values.put("fluid",Map.of("input",coreCall(config,"autoInput"),"output",coreCall(config,"autoOutput")));}catch(Exception absent){}
        write("tr-state.json",values);
    }
'''
legacy=ast.parse((ROOT/'functional-tests/tech-reborn-implemented/prepare.py').read_text())
EXTRA+=ast.literal_eval(next(node.value for node in legacy.body if isinstance(node,ast.Assign)and any(isinstance(t,ast.Name)and t.id=='EXTRA'for t in node.targets)))

def patch(text,mc):
    text=text.replace('case "catalog"->{','case "tr-observe"->work=server(()->{trObserve();return true;});\n            case "catalog"->{',1)
    text=text.replace('case "catalog"->{','case "tr-blueprint"->work=server(()->{trBlueprint();return true;});\n            case "catalog"->{',1)
    needle='row.put("nativeScreen",screen.getClass().getName());'
    text=text.replace(needle,needle+'''if(screen.getClass().getName().startsWith("techreborn."))try{
        Object tab=field(screen,"selectedTab");row.put("trSelectedTab",tab==null?"none":tab.getClass().getSimpleName());
        try{row.put("trValid",trValid(field(screen,"be")));}catch(Exception absent){}
        try{row.put("hologram",field(field(screen,"be"),"renderMultiblock"));}catch(Exception absent){}
        row.put("trUpgrades",field(screen,"upgrades"));try{row.put("cachedHeat",coreCall(field(screen,"be"),"getCachedHeat"));}catch(Exception absent){}
        row.put("sameModel",((AbstractContainerScreen<?>)screen).getMenu()==coreCall(window,"containerMenu"));
        }catch(Exception error){row.put("trObservationError",error.toString());}''',1)
    if mc.startswith('26.'):
        text=text.replace('mc.screen','screen()').replace('new net.minecraft.client.input.CharacterEvent(letter,0)','new net.minecraft.client.input.CharacterEvent(letter)')
        text=text.replace('mc.player.getInventory().selected','mc.player.getInventory().getSelectedSlot()')
        text=text.replace('.save(new net.minecraft.nbt.CompoundTag())','.getComponents()')
        # Core-state's representative merchant records use mapped public members.
        text=text.replace('mc.player.getInventory().getItem(0).getTag()','mc.player.getInventory().getItem(0).getComponents()')
    extra=EXTRA.replace('coreCall(be,"isMultiblockValid")','trValid(be)')
    extra+='''static Object trValid(Object be){try{return coreCall(be,"isMultiblockValid");}catch(Exception modern){return coreCall(be,"isShapeValid");}}'''
    if mc.startswith('26.'):
        extra=extra.replace('player().serverLevel()','((net.minecraft.server.level.ServerLevel)player().level())')
        extra+='''static Map<String,Object> observeWidget(net.minecraft.client.gui.components.AbstractWidget w){return Map.of("type",w.getClass().getName(),"x",w.getX(),"y",w.getY(),"width",w.getWidth(),"height",w.getHeight(),"active",w.active,"visible",w.visible,"message",w.getMessage().getString());}'''
    return text[:text.rfind('}')]+extra+'\n}\n'

def main():
    p=argparse.ArgumentParser();p.add_argument('minecraft');p.add_argument('run');p.add_argument('--native',action='store_true');a=p.parse_args();mc=a.minecraft
    target=next(r for r in json.loads((ROOT/'output/tech-reborn-all-targets/targets.json').read_text())if r['minecraft']==mc)
    folder=WORK/mc;folder.mkdir(exist_ok=True)
    api_meta=folder/'fabric-api-releases.json'
    if not api_meta.exists():base.fetch('https://api.modrinth.com/v2/project/fabric-api/version',api_meta)
    api=next(r for r in base.read(api_meta)if mc in r['game_versions']and 'fabric'in r['loaders']and r['version_type']=='release')
    file=next(f for f in api['files']if f['primary']);api_jar=base.fetch(file['url'],folder/file['filename'],file['hashes']['sha512'],'sha512')
    fixture={'minecraft':mc,'loader':'fabric','runtime':{'fabric_loader':'0.19.5','fabric_api':api['version_number']},'artifacts':[{'coordinate':'maven.modrinth:techreborn:'+target['techreborn']['id'],'path':target['techreborn_jar'],'filename':Path(target['techreborn_jar']).name,'sha512':hashlib.sha512(Path(target['techreborn_jar']).read_bytes()).hexdigest(),'mod_ids':['techreborn']}]}
    fixture_path=folder/'fixture.json';fixture_path.write_text(json.dumps(fixture,indent=2))
    prep.MC=mc;prep.CACHE=folder/'qa-cache';base.CACHE=prep.CACHE
    base.MC_CACHE=Path.home()/'.gradle/caches/fabric-loom'/mc
    qa_classpath=next((ROOT/'build/all-target-rollout/gameplay').glob(mc+'-fabric-*/launch-details.json'))
    old=prep.qa_source
    def qa(side):
        path,text=old(side);return path,patch(text,mc)
    prep.qa_source=qa
    if mc=='1.21.1':
        original_read=base.read
        def read(path):
            if str(path).endswith('fabric-api-1.21.1-batch-versions.json'):return [api]
            return original_read(path)
        base.read=read
        sys.argv=[sys.argv[0],'--fixture',str(fixture_path),'--minecraft',mc,'--side','native'if a.native else'siu','--run',a.run,'--classpath-from',str(qa_classpath),'--core-jar',str(ROOT/f'dist/core/salts_inventory_update-{mc}-Fabric-0.1.6.jar'),'--compat-jar',str(ROOT/f'compatibility/versions/{mc}/fabric/build/libs/salts_inventory_compat-{mc}-Fabric-0.1.3.jar')]
        prep.main();return
    # Minecraft 26.x is already named. Only the excluded observer is compiled;
    # original Minecraft, upstream, Core and Compatibility bytes stay untouched.
    _,text=qa('native'if a.native else'siu');source=folder/'observer/com/siu_reference';source.mkdir(exist_ok=True,parents=True)
    (source/'NativeUiReference.java').write_text(text,encoding='utf-8');(source/'RuntimeNames.java').write_text(IDENTITY)
    (source/'NativeReferenceFabric.java').write_text('package com.siu_reference;public final class NativeReferenceFabric implements net.fabricmc.api.ClientModInitializer{public void onInitializeClient(){net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(mc->NativeUiReference.tick());}}')
    java=Path('C:/Program Files/Java/jdk-25.0.3/bin/java.exe');classes=folder/'observer/classes';classes.mkdir(exist_ok=True)
    cp=base.read(qa_classpath)['classpath'];args=['--release','25','-proc:none','-encoding','UTF-8','-cp',';'.join(cp),'-d',str(classes),*map(str,source.glob('*.java'))]
    argfile=folder/'observer/javac.args';argfile.write_text('\n'.join(map(base.java_argument,args)))
    with (folder/'observer/compile.log').open('w')as log:subprocess.run([str(java.with_name('javac.exe')),'@'+str(argfile.resolve())],stdout=log,stderr=subprocess.STDOUT,check=True)
    qa_jar=folder/'observer/native-ui-reference.jar'
    with zipfile.ZipFile(qa_jar,'w',zipfile.ZIP_DEFLATED)as z:
        for f in classes.rglob('*.class'):z.write(f,f.relative_to(classes).as_posix())
        z.writestr('fabric.mod.json',json.dumps({'schemaVersion':1,'id':'native_ui_reference','version':'1.0.0','environment':'client','entrypoints':{'client':['com.siu_reference.NativeReferenceFabric']},'depends':{'minecraft':mc,'fabric-api':'*'}}))
    side='native'if a.native else'siu';name=f'{mc}-fabric-{side}-{a.run}'
    output=ROOT/'build/remaining-ui-audit/gameplay'/name;runtime=ROOT/'build/remaining-ui-audit/production-runtime'/name
    assert not output.exists()and not runtime.exists();output.mkdir();(runtime/'mods').mkdir(parents=True)
    info=base.read(base.MC_CACHE/'mojang_minecraft_info.json');loader='0.19.5';profile_path=folder/f'fabric-loader-{loader}.json'
    base.fetch(f'https://meta.fabricmc.net/v2/versions/loader/{mc}/{loader}/profile/json',profile_path);profile=base.read(profile_path)
    cp=[base.library(r,True)for r in profile['libraries']]+[base.library(r)for r in info['libraries']if base.allowed(r)and'artifact'in r.get('downloads',{})]
    client=base.MC_CACHE/'minecraft-client.jar';assert base.digest(client,'sha1')==info['downloads']['client']['sha1'];cp.append(client)
    assets=Path.home()/'.gradle/caches/fabric-loom/assets';index=f'techreborn-{mc}-'+info['assetIndex']['id'];base.fetch(info['assetIndex']['url'],assets/'indexes'/f'{index}.json',info['assetIndex']['sha1'])
    inputs=[('upstream',Path(target['techreborn_jar'])),('runtime-api',api_jar),('qa',qa_jar)]
    if not a.native:
        core=ROOT/(f'versions/{mc}/fabric/build/libs/salts_inventory_update-{mc}-Fabric-0.1.6.jar'if mc in ('26.2','26.3')else f'dist/core/salts_inventory_update-{mc}-Fabric-0.1.6.jar')
        inputs += [('core',core),('compatibility',ROOT/f'compatibility/versions/{mc}/fabric/build/libs/salts_inventory_compat-{mc}-Fabric-0.1.3.jar')]
    staged=[]
    for family,f in inputs:
        shutil.copy2(f,runtime/'mods'/f.name);staged.append({'family':family,'file':str(f.resolve()),'filename':f.name,'sha256':base.digest(f),'sha512':base.digest(f,'sha512')})
    (runtime/'options.txt').write_text('tutorialStep:none\nrenderDistance:2\nsimulationDistance:5\nguiScale:2\npauseOnLostFocus:false\nautoJump:false\n')
    argv=['-Xms512m','-Xmx3g','-XX:ActiveProcessorCount=4','--enable-native-access=ALL-UNNAMED','--sun-misc-unsafe-memory-access=allow','-Dfabric.development=false','-Dfabric.noGui=true','-Dfabric.gameJarPath='+str(client),'-cp',';'.join(map(str,cp)),profile['mainClass'],'--username','UIAudit','--version',mc,'--gameDir',str(runtime),'--assetsDir',str(assets),'--assetIndex',index,'--uuid','14ea713fe12d3d8bb7b6baf52d52d598','--accessToken','0','--userType','legacy','--versionType','release','--width','1280','--height','900']
    arguments=output/'java.args';arguments.write_text('\n'.join(map(base.java_argument,argv)))
    launch={'java':str(java),'arguments':str(arguments),'cwd':str(runtime),'environment':{'NATIVE_REFERENCE_OUTPUT':str(output),'NATIVE_REFERENCE_LOADER':'fabric','SIU_AUDIT_CORE':str(not a.native).lower()},'log':str(output/'launch.log')}
    (output/'launch.json').write_text(json.dumps(launch,indent=2));(output/'staged-artifacts.json').write_text(json.dumps({'minecraft':mc,'loader':'fabric','artifacts':staged,'siu_loaded':not a.native},indent=2))
    print('Prepared',output)

if __name__=='__main__':main()
