"""Compile only the excluded observer; run untouched publisher/subject releases."""
from pathlib import Path
from native_input_geometry import correct_block_hits
import argparse, importlib.util, json, sys, re, hashlib, shutil, subprocess, zipfile
ROOT = Path(__file__).resolve().parents[2]
def module(name, path):
    spec = importlib.util.spec_from_file_location(name, path); m = importlib.util.module_from_spec(spec); spec.loader.exec_module(m); return m
prep = module('regression_production', ROOT / 'tools/prepare-rollout-production-modern.py')
# Cache the excluded observer's name lookup. Publisher and SIU bytecode are
# untouched; repeated observations should not repeatedly walk interface trees.
prep.RESOLVER = prep.RESOLVER.replace('    static Set<String> names(Class<?> type,String name,String kind){', '''
    private static final java.util.concurrent.ConcurrentMap<String,Set<String>> LOOKUPS=new java.util.concurrent.ConcurrentHashMap<>();
    static Set<String> names(Class<?> type,String name,String kind){return LOOKUPS.computeIfAbsent(type.getName()+"/"+kind+"/"+name,key->resolveNames(type,name,kind));}
    static Set<String> resolveNames(Class<?> type,String name,String kind){''')

def api_release(mc, desired):
    """Resolve only the fixture's exact platform pin, never a newer substitute."""
    candidates = [ROOT / f'build/all-target-rollout/metadata/fabric-api-{mc}-batch-versions.json',
                  ROOT / 'output/ad-astra-implemented/fabric-api-releases.json',
                  ROOT / f'build/tech-reborn-all-targets/{mc}/fabric-api-releases.json']
    for path in candidates:
        if not path.exists(): continue
        data = json.loads(path.read_text())
        if not isinstance(data, list): continue
        match = next((r for r in data if r.get('version_number') == desired), None)
        if match: return match
    from urllib.parse import quote
    cache = ROOT / 'build/project-everything-regression' / ('fabric-api-' + desired.replace('+', '_') + '.json')
    prep.base.fetch('https://api.modrinth.com/v2/project/P7dR8mSH/version/' + quote(desired, safe=''), cache)
    release = json.loads(cache.read_text())
    assert release['version_number'] == desired and mc in release['game_versions']
    return release

ADAPTER = r'''
    static void regressionRecord(Object result){
        try(var out=new java.util.zip.GZIPOutputStream(Files.newOutputStream(OUT.resolve("observations.jsonl.gz"),StandardOpenOption.CREATE,StandardOpenOption.APPEND))){out.write((new Gson().toJson(result)+"\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));}
        catch(java.io.IOException e){throw new IllegalStateException(e);}
        history.add(result);if(history.size()>16)history.remove(0);write("observations.json",history);
    }
    static int regressionActiveSession;
    static List<?> regressionSessions(){Object d=regressionDesktop();return d==null?List.of():(List<?>)field(d,"sessions");}
    static Object regressionDesktop(){return com.salts_inventory_update.client.InventoryDesktopScreen.current(mc);}
    static void regressionFocus(Object w){coreCall(regressionDesktop(),"bringToFront",w);coreCall(regressionDesktop(),"setFocusedWindow",w);}
    static Object regressionWindow(int id,String kind){
        for(Object w:(List<?>)field(regressionDesktop(),"windows")){
            if(kind!=null&&field(w,"kind").toString().equals(kind))return w;
            Object session=field(w,"session");if(session!=null&&((Number)coreCall(session,"sessionId")).intValue()==id)return w;
        }return null;
    }
    static Object regressionH(String name,Object...args){
        return switch(name){
            case "screen"->regressionDesktop();
            case "nativeScreen"->mc.screen;
            case "serverLevel"->player().serverLevel();
            case "dispatchMovementMouse"->{regressionWorldUseMouse((String)args[0]);yield null;}
            case "left"->regressionLeft();
            case "windowKind"->regressionWindow(-1,(String)args[0]);
            case "windowSession"->regressionWindow(((Number)args[0]).intValue(),null);
            case "slotPoint"->regressionPoint(((Number)args[0]).intValue(),((Number)args[1]).intValue(),args[2]);
            case "mouse"->{regressionMouse((String)args[0],(double)args[1],(double)args[2],(int)args[3],(double)args[4],(double)args[5]);yield null;}
            case "scroll"->{Object d=regressionDesktop();double x=((Number)args[0]).doubleValue(),y=((Number)args[1]).doubleValue(),amount=((Number)args[2]).doubleValue();try{coreCall(d,"mouseScrolled",x,y,0D,amount);}catch(IllegalArgumentException legacy){coreCall(d,"mouseScrolled",x,y,amount);}yield null;}
            case "clickSlot"->{var p=regressionPoint(regressionActiveSession,((Number)args[0]).intValue(),regressionWindow(regressionActiveSession,null));regressionMouse("mouseClicked",p[0],p[1],(int)args[1],0,0);regressionMouse("mouseReleased",p[0],p[1],(int)args[1],0,0);yield null;}
            case "clickWindowControl"->{regressionControl(args[0],(String)args[1]);yield null;}
            default->throw new IllegalArgumentException(name);
        };
    }
    static int regressionLeft(){try{return ((Number)field(Class.forName(RuntimeNames.className("com.mojang.blaze3d.platform.InputConstants")),"MOUSE_BUTTON_LEFT")).intValue();}catch(Exception old){return 0;}}
    static void regressionWorldUseMouse(String action){
        Object window=mc.getWindow();long handle;
        try{handle=((Number)coreCall(window,"handle")).longValue();}catch(IllegalArgumentException old){handle=((Number)coreCall(window,"getWindow")).longValue();}
        int button;try{button=((Number)field(Class.forName(RuntimeNames.className("com.mojang.blaze3d.platform.InputConstants")),"MOUSE_BUTTON_RIGHT")).intValue();}catch(Exception old){button=1;}
        int phase=action.equals("PRESS")?1:0;
        try{coreCall(mc.mouseHandler,"onPress",handle,button,phase,0);}
        catch(IllegalArgumentException modern){coreCall(mc.mouseHandler,"onButton",handle,regressionConstruct("net.minecraft.client.input.MouseButtonInfo",button,0),phase);}
    }
    static Object regressionConstruct(String name,Object...args){try{Class<?> type=Class.forName(RuntimeNames.className(name));for(var ctor:type.getDeclaredConstructors())if(ctor.getParameterCount()==args.length)try{ctor.setAccessible(true);return ctor.newInstance(args);}catch(IllegalArgumentException mismatch){}throw new IllegalArgumentException(name);}catch(ReflectiveOperationException e){throw new IllegalStateException(e);}}
    static void regressionMouse(String name,double x,double y,int button,double dx,double dy){
        Object d=regressionDesktop();coreCall(d,"mouseMoved",x,y);
        try{if(name.equals("mouseDragged"))coreCall(d,name,x,y,button,dx,dy);else coreCall(d,name,x,y,button);}
        catch(IllegalArgumentException modern){Object info=regressionConstruct("net.minecraft.client.input.MouseButtonInfo",button,0);Object event=regressionConstruct("net.minecraft.client.input.MouseButtonEvent",x,y,info);if(name.equals("mouseDragged"))coreCall(d,name,event,dx,dy);else if(name.equals("mouseClicked"))coreCall(d,name,event,false);else coreCall(d,name,event);}
    }
    static double[] regressionPoint(int id,int slot,Object w){
        try {
            int x=((Number)field(w,"x")).intValue(),y=((Number)field(w,"y")).intValue(),width=((Number)field(w,"width")).intValue(),height=((Number)field(w,"height")).intValue();
            Object desktop=regressionDesktop();var lookup=desktop.getClass().getDeclaredMethod("slotAt",double.class,double.class);lookup.setAccessible(true);
            Method session=null,index=null;
            for(int py=y-40;py<y+height+40;py+=4)for(int px=x-80;px<x+width+120;px+=4){
                Object hit=lookup.invoke(desktop,(double)px,(double)py);if(hit==null)continue;
                if(session==null){session=hit.getClass().getDeclaredMethod("sessionId");index=hit.getClass().getDeclaredMethod("slotId");session.setAccessible(true);index.setAccessible(true);}
                if(((Number)session.invoke(hit)).intValue()==id&&((Number)index.invoke(hit)).intValue()==slot)return new double[]{((Number)coreCall(hit,"x")).doubleValue()+8,((Number)coreCall(hit,"y")).doubleValue()+8};
            }
            throw new IllegalStateException("Visible physical slot not found: "+id+"/"+slot);
        }catch(ReflectiveOperationException error){throw new IllegalStateException(error);}
    }
    static void regressionControl(Object w,String name){
        Object d=regressionDesktop(),layout=coreCall(d,"titleBarLayout",w),chosen=null;
        for(Object rect:(List<?>)coreCall(layout,"controls"))if(coreCall(rect,"control").toString().equals(name))chosen=rect;
        if(chosen==null){for(Object rect:(List<?>)coreCall(layout,"controls"))if(coreCall(rect,"control").toString().equals("ELLIPSIS")){double x=((Number)coreCall(rect,"x")).doubleValue()+4,y=((Number)coreCall(rect,"y")).doubleValue()+4;regressionMouse("mouseClicked",x,y,regressionLeft(),0,0);regressionMouse("mouseReleased",x,y,regressionLeft(),0,0);}for(Object rect:(List<?>)coreCall(d,"popupControlRects",w,coreCall(d,"popupRect",w)))if(coreCall(rect,"control").toString().equals(name))chosen=rect;}
        if(chosen==null)throw new IllegalStateException("Control not exposed: "+name);
        double x=((Number)coreCall(chosen,"x")).doubleValue()+((Number)coreCall(chosen,"width")).doubleValue()/2,y=((Number)coreCall(chosen,"y")).doubleValue()+((Number)coreCall(chosen,"height")).doubleValue()/2;
        regressionMouse("mouseClicked",x,y,regressionLeft(),0,0);regressionMouse("mouseReleased",x,y,regressionLeft(),0,0);
    }
    static Map<String,Object> regressionItem(ItemStack s){return Map.of("item",BuiltInRegistries.ITEM.getKey(s.getItem()).toString(),"count",s.getCount());}
'''

def patch(source):
    fragment = (ROOT / 'functional-tests/project-everything-regression/observer-fragment.java.txt').read_text()
    fragment = fragment.replace('settle=', 'delay=').replace('sessions()', 'regressionSessions()')
    fragment = re.sub(r'\bh\(', 'regressionH(', fragment)
    fragment = re.sub(r'\bcall\(', 'coreCall(', fragment)
    fragment = fragment.replace('set(H,"activeSession",session);', 'regressionActiveSession=num(c,"session",-1);')
    fragment = fragment.replace('origin()', 'POS').replace('((ServerLevel)regressionH("serverLevel"))', 'player().serverLevel()').replace('level().setBlock(pos,state,3);', 'player().serverLevel().setBlock(pos,state,3);')
    fragment = fragment.replace('Class.forName("net.minecraft.resources.Identifier")', 'Class.forName(RuntimeNames.className("net.minecraft.resources.Identifier"))').replace('Class.forName("net.minecraft.resources.ResourceLocation")', 'Class.forName(RuntimeNames.className("net.minecraft.resources.ResourceLocation"))')
    fragment = fragment.replace('InventoryDesktopScreen.openOrToggleInventory(mc)', 'com.salts_inventory_update.client.InventoryDesktopScreen.openOrToggleInventory(mc)')
    fragment = fragment.replace('item(s.getItem())', 'regressionItem(s.getItem())').replace('item(player().getInventory().getItem(i))', 'regressionItem(player().getInventory().getItem(i))').replace('item(player().inventoryMenu.getCarried())', 'regressionItem(player().inventoryMenu.getCarried())')
    # Hook three test-only commands into the original observer protocol.
    extra = '''
        if(str(c,"type","").equals("seed-player")){work=server(()->{for(var el:c.getAsJsonArray("items")){var v=el.getAsJsonObject();player().getInventory().setItem(num(v,"slot",9),stack(v));}player().inventoryMenu.broadcastFullState();return true;});return;}
        if(str(c,"type","").equals("regression-move")){Object w=regressionManagedWindow(num(c,"session",-1));regressionFocus(w);double x=((Number)field(w,"x")).doubleValue()+8,y=((Number)field(w,"y")).doubleValue()+7,dx=num(c,"dx",-50),dy=num(c,"dy",30);regressionMouse("mouseClicked",x,y,regressionLeft(),0,0);regressionMouse("mouseDragged",x+dx,y+dy,regressionLeft(),dx,dy);regressionMouse("mouseReleased",x+dx,y+dy,regressionLeft(),0,0);return;}
        if(regressionCommand(c))return;
    '''
    at = source.index('case "catalog"'); start = source.rfind('switch', 0, at)
    source = source[:start] + extra + source[start:]
    source = source.replace('history.add(reply);write("observations.json",history);', 'regressionRecord(reply);')
    source = source.replace('reply.put("client",state());', 'reply.put("client",str(pending,"type","").startsWith("regression-")&&!str(pending,"type","").equals("regression-move")?Map.of("regression",true):state());')
    source = source.replace('Object definition=field(window,"apiDefinition");', 'row.put("sessionId",coreCall(field(window,"session"),"sessionId"));row.put("locked",field(window,"locked"));Object definition=field(window,"apiDefinition");', 1)
    return correct_block_hits(source[:source.rfind('}')] + fragment + ADAPTER + '\n}\n')

def main():
    p = argparse.ArgumentParser(); p.add_argument('--minecraft', required=True); p.add_argument('--loader',default='fabric',choices=['fabric','forge']); p.add_argument('--fixture', type=Path, required=True); p.add_argument('--run', required=True); p.add_argument('--boundary-observer', action='store_true', help='Add read-only Aether/Aircraft observations for the API-boundary regression'); p.add_argument('--release-toms',action='store_true'); a = p.parse_args()
    work = ROOT / 'build/project-everything-regression/production'; work.mkdir(parents=True, exist_ok=True)
    original = prep.qa_source
    def qa(side):
        path, text = original(side)
        if a.boundary_observer:
            assert a.minecraft in ['1.20.1','1.21.1'], 'Reviewed boundary observer mappings'
            aether = module('boundary_aether_observer', ROOT/'functional-tests/aether-windows/prepare.py')
            text = aether.enhance(text)
            if a.minecraft == '1.20.1':
                aircraft = module('boundary_aircraft_observer', ROOT/'functional-tests/aircraft-window/prepare.py')
                text = aircraft.enhance(text)
            else:
                text = text.replace('.save(new net.minecraft.nbt.CompoundTag())','.saveOptional(player().registryAccess())').replace('.saveWithFullMetadata()','.saveWithFullMetadata(player().registryAccess())')
        text = patch(text)
        if a.release_toms:
            assert a.minecraft=='26.1.2' and a.loader=='fabric'
            text=text.replace('if(kind.equals("regression-world"))return true;',
                'if(kind.equals("regression-world"))return true;\n        if(kind.equals("regression-toms-state")){releaseTomsObserve(c);return true;}\n        if(kind.equals("regression-toms-network")){releaseTomsNetworkFixture();return true;}')
            text=text[:text.rfind('}')] + Path(__file__).with_name('release-toms-observer.java.inc').read_text() + '\n}\n'
        text = text.replace("REGRESSION_STACK_SIGNATURE", "String.valueOf(s.copyWithCount(1).save(new net.minecraft.nbt.CompoundTag()))" if a.minecraft == "1.20.1" else "regressionItemSignature(s)")
        text = text.replace('REGRESSION_ITEM_DATA_FORMAT', 'native-item-stack-save' if a.minecraft == '1.20.1' else 'native-item-codec')
        text = text.replace('REGRESSION_SELECT_EMPTY_HAND', 'mc.player.getInventory().setSelectedSlot(emptySlot);' if a.minecraft not in ['1.20.1','1.21.1'] else 'mc.player.getInventory().selected=emptySlot;')
        text = text.replace('REGRESSION_SET_SERVER_SELECTED', 'player().getInventory().setSelectedSlot(0);' if a.minecraft not in ['1.20.1','1.21.1'] else 'player().getInventory().selected=0;')
        text=text.replace('REGRESSION_SEND_SHIFT_PACKET',
            'mc.player.connection.send(new net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket(mc.player,down?net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket.Action.PRESS_SHIFT_KEY:net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket.Action.RELEASE_SHIFT_KEY));'
            if a.minecraft in ['1.20.1','1.21.1'] else
            'mc.player.connection.send(new net.minecraft.network.protocol.game.ServerboundPlayerInputPacket(new net.minecraft.world.entity.player.Input(false,false,false,false,false,down,false)));')
        native_click='coreCall(s,"mouseClicked",x,y,button);coreCall(s,"mouseReleased",x,y,button);' if a.minecraft in ['1.20.1','1.21.1'] else 'var event=new net.minecraft.client.input.MouseButtonEvent(x,y,new net.minecraft.client.input.MouseButtonInfo(button,0));coreCall(s,"mouseClicked",event,false);coreCall(s,"mouseReleased",event);'
        text=text.replace('REGRESSION_NATIVE_CLICK',native_click)
        text=text.replace('REGRESSION_NATIVE_KEY','coreCall(s,"keyPressed",key,0,modifiers);' if a.minecraft in ['1.20.1','1.21.1'] else 'coreCall(s,"keyPressed",new net.minecraft.client.input.KeyEvent(key,'+('num(c,"keycode",0)' if a.minecraft=='26.3' else '0')+',modifiers));')
        text=text.replace('REGRESSION_DESKTOP_ESCAPE','coreCall(desktop,"keyPressed",256,0,0);' if a.minecraft in ['1.20.1','1.21.1'] else 'coreCall(desktop,"keyPressed",new net.minecraft.client.input.KeyEvent('+('41' if a.minecraft=='26.3' else '256')+',0,0));')
        text=text.replace('REGRESSION_USE_ENTITY_AT','mc.gameMode.'+('interact' if a.minecraft.startswith('26.') else 'interactAt')+'(mc.player,entity,new net.minecraft.world.phys.EntityHitResult(entity),net.minecraft.world.InteractionHand.MAIN_HAND);')
        text=text.replace('REGRESSION_USE_ENTITY','mc.gameMode.interact(mc.player,entity,'+('new net.minecraft.world.phys.EntityHitResult(entity),' if a.minecraft.startswith('26.') else '')+'net.minecraft.world.InteractionHand.MAIN_HAND);')
        if a.minecraft=='26.3':
            text=text.replace('new net.minecraft.client.input.KeyEvent(num(c,"key",256),0,num(c,"modifiers",0))',
                              'new net.minecraft.client.input.KeyEvent(num(c,"key",41),num(c,"keycode",0),num(c,"modifiers",0))')
        parse = 'parseTag' if a.minecraft in ['1.20.1', '1.21.1'] else 'parseCompoundFully'
        tag = 'net.minecraft.nbt.TagParser.' + parse + '(c.get("snbt").getAsString())'
        legacy_data='if(c.has("native_decree")&&c.get("native_decree").getAsBoolean())s.getOrCreateTag().merge('+tag+');else s.setTag('+tag+');'
        apply = legacy_data if a.minecraft == '1.20.1' else 's.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.of(' + tag + '));'
        text = text.replace('REGRESSION_APPLY_STACK_DATA', apply)
        decree='io.ejekta.bountiful.content.'+('' if a.minecraft=='1.20.1' else 'item.')+'DecreeItem'
        text=text.replace('REGRESSION_INITIALIZE_DECREE','s=(ItemStack)coreCall(Class.forName("'+decree+'").getField("Companion").get(null),'+('"create","farmer"' if a.minecraft=='1.20.1' else '"createWithAllDecrees"')+');')

        if a.minecraft.startswith('26.'):
            text = text.replace('mc.screen', 'screen()').replace('new net.minecraft.client.input.CharacterEvent(letter,0)', 'new net.minecraft.client.input.CharacterEvent(letter)')
            text = text.replace('mc.player.getInventory().selected', 'mc.player.getInventory().getSelectedSlot()')
            text = text.replace('.save(new net.minecraft.nbt.CompoundTag())', '.getComponents()')
            text = text.replace('mc.player.getInventory().getItem(0).getTag()', 'mc.player.getInventory().getItem(0).getComponents()')
        if a.minecraft not in ['1.20.1','1.21.1']:
            text = text.replace('player().serverLevel()', '((net.minecraft.server.level.ServerLevel)player().level())')
            text = text[:text.rfind('}')] + 'static Map<String,Object> observeWidget(net.minecraft.client.gui.components.AbstractWidget w){return Map.of("type",w.getClass().getName(),"x",w.getX(),"y",w.getY(),"width",w.getWidth(),"height",w.getHeight(),"active",w.active,"visible",w.visible,"message",w.getMessage().getString());}\n}\n' 
        return path, text
    prep.qa_source = qa
    original_read = prep.base.read
    def exact_platform(path):
        if Path(path).name == f'fabric-api-{a.minecraft}-batch-versions.json':
            fixture = json.loads(a.fixture.read_text())
            return [api_release(a.minecraft, fixture['runtime']['fabric_api'])]
        return original_read(path)
    prep.base.read = exact_platform
    details = next((ROOT / 'build/all-target-rollout/gameplay').glob(a.minecraft + '-' + a.loader + '-project-everything-full-01/launch-details.json'))
    data = json.loads(details.read_text()); data['classpath'].append(str(ROOT / f'versions/{a.minecraft}/fabric/build/classes/java/main'))
    compiler_cp = work / (a.minecraft + '-' + a.loader + '-classpath.json'); compiler_cp.write_text(json.dumps(data))
    if a.loader=='forge':
        assert a.minecraft=='1.20.1';prep.MC=a.minecraft
        sys.path.insert(0,str(ROOT/'tools'))
        forge=module('regression_forge',ROOT/'tools/prepare-rollout-production-forge.py')
        forge.modern.qa_source=qa;forge.modern.RESOLVER=prep.RESOLVER
        data['runtimeClasspath']=data['classpath'];compiler_cp=work/(a.minecraft+'-forge-classpath.json');compiler_cp.write_text(json.dumps(data))
        sys.argv=[sys.argv[0],'--fixture',str(a.fixture),'--side','siu','--run',a.run,'--classpath-from',str(compiler_cp),
            '--core-jar',str(ROOT/f'dist/core/salts_inventory_update-{a.minecraft}-Forge-0.1.6.jar'),
            '--compat-jar',str(ROOT/f'dist/compatibility/salts_inventory_compat-{a.minecraft}-Forge-0.1.3.jar')]
        return forge.main()
    if a.minecraft.startswith('26.'):
        return prepare_named(a.minecraft, a.fixture, a.run, compiler_cp, qa)
    sys.argv = [sys.argv[0], '--minecraft', a.minecraft, '--fixture', str(a.fixture), '--side', 'siu', '--run', a.run,
        '--classpath-from', str(compiler_cp), '--core-jar', str(ROOT / f'dist/core/salts_inventory_update-{a.minecraft}-Fabric-0.1.6.jar'),
        '--compat-jar', str(ROOT / f'dist/compatibility/salts_inventory_compat-{a.minecraft}-Fabric-0.1.3.jar'), '--heap', '4g']
    prep.main()

def prepare_named(mc, fixture_path, run, cp_path, qa, side='siu'):
    assert side in {'siu','native'}
    base = prep.base; fixture = json.loads(fixture_path.read_text()); prep.MC = mc
    folder = ROOT / 'build/project-everything-regression/production' / (mc if side=='siu' else mc+'-native-control'); folder.mkdir(exist_ok=True)
    prep.CACHE = folder; base.CACHE = folder; base.MC_CACHE = Path.home() / '.gradle/caches/fabric-loom' / mc
    _, text = qa(side); source = folder / 'observer/com/siu_reference'; source.mkdir(parents=True, exist_ok=True)
    (source / 'NativeUiReference.java').write_text(text, encoding='utf-8')
    (source / 'RuntimeNames.java').write_text('package com.siu_reference;import java.lang.reflect.Field;import java.util.Set;final class RuntimeNames{static String className(String n){return n;}static Set<String> names(Class<?> t,String n,String k){return Set.of(n);}static Field field(Class<?> t,String n)throws NoSuchFieldException{return t.getDeclaredField(n);}}')
    (source / 'NativeReferenceFabric.java').write_text('package com.siu_reference;public final class NativeReferenceFabric implements net.fabricmc.api.ClientModInitializer{public void onInitializeClient(){net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(mc->NativeUiReference.tick());}}')
    java = Path('C:/Program Files/Java/jdk-25.0.3/bin/java.exe'); classes = folder / 'observer/classes'; classes.mkdir(exist_ok=True)
    cp = json.loads(cp_path.read_text())['classpath']
    args = ['--release', '25', '-proc:none', '-encoding', 'UTF-8', '-cp', ';'.join(cp), '-d', str(classes), *map(str, source.glob('*.java'))]
    argfile = folder / 'observer/javac.args'; argfile.write_text('\n'.join(map(base.java_argument, args)))
    with (folder / 'observer/compile.log').open('w') as log: subprocess.run([str(java.with_name('javac.exe')), '@' + str(argfile.resolve())], stdout=log, stderr=subprocess.STDOUT, check=True)
    qa_jar = folder / 'observer/native-ui-reference.jar'
    with zipfile.ZipFile(qa_jar, 'w', zipfile.ZIP_DEFLATED) as jar:
        for file in classes.rglob('*.class'): jar.write(file, file.relative_to(classes).as_posix())
        jar.writestr('fabric.mod.json', json.dumps({'schemaVersion': 1, 'id': 'native_ui_reference', 'version': '1.0.0', 'environment': 'client', 'entrypoints': {'client': ['com.siu_reference.NativeReferenceFabric']}, 'depends': {'minecraft': mc, 'fabric-api': '*'}}))
    name = f'{mc}-fabric-{side}-{run}'; output = ROOT / 'build/remaining-ui-audit/gameplay' / name
    runtime = Path.home() / '.cache/siu-regression-runtime' / name
    assert not output.exists() and not runtime.exists(); output.mkdir(); (runtime / 'mods').mkdir(parents=True)
    info = base.read(base.MC_CACHE / 'mojang_minecraft_info.json'); loader = fixture['runtime'].get('fabric_loader', '0.19.5'); profile_path = folder / f'fabric-loader-{loader}.json'
    base.fetch(f'https://meta.fabricmc.net/v2/versions/loader/{mc}/{loader}/profile/json', profile_path); profile = base.read(profile_path)
    cp = [base.library(r, True) for r in profile['libraries']] + [base.library(r) for r in info['libraries'] if base.allowed(r) and 'artifact' in r.get('downloads', {})]
    client = base.MC_CACHE / 'minecraft-client.jar'; assert base.digest(client, 'sha1') == info['downloads']['client']['sha1']; cp.append(client)
    assets = Path.home() / '.gradle/caches/fabric-loom/assets'; index = f'regression-{mc}-' + info['assetIndex']['id']; base.fetch(info['assetIndex']['url'], assets / 'indexes' / (index + '.json'), info['assetIndex']['sha1'])
    api = api_release(mc, fixture['runtime']['fabric_api']); file = next(f for f in api['files'] if f['primary'])
    api_jar = base.fetch(file['url'], folder / file['filename'], file['hashes']['sha512'], 'sha512')
    inputs = [('runtime-api', api_jar, api_jar.name), ('qa', qa_jar, qa_jar.name)]
    # Inspection copies of nested jars remain nested. Explicit dependency roots
    # (for example rendering APIs) must also be staged when they lack a Maven coordinate.
    roots = [r for r in fixture['artifacts'] if r.get('coordinate') or r.get('stage_as_root', False)]
    inputs += [('upstream', ROOT / r['path'], r['filename']) for r in roots]
    for r in fixture['artifacts']:
        if r in roots: assert base.digest(ROOT / r['path'], 'sha512') == r['sha512']
    if side=='siu':
        inputs += [('core', ROOT / f'dist/core/salts_inventory_update-{mc}-Fabric-0.1.6.jar', f'salts_inventory_update-{mc}-Fabric-0.1.6.jar'), ('compatibility', ROOT / f'dist/compatibility/salts_inventory_compat-{mc}-Fabric-0.1.3.jar', f'salts_inventory_compat-{mc}-Fabric-0.1.3.jar')]
    staged = []
    for family, file, filename in inputs:
        if prep.retain_identical_input(staged,family,file,filename):continue
        shutil.copy2(file, runtime / 'mods' / filename); staged.append({'family': family, 'file': str(file.resolve()), 'filename': filename, 'sha256': base.digest(file), 'sha512': base.digest(file, 'sha512')})
    (runtime / 'options.txt').write_text('tutorialStep:none\nrenderDistance:2\nsimulationDistance:5\nguiScale:2\npauseOnLostFocus:false\nautoJump:false\n')
    wover = next((r['filename'] for r in staged if 'worldweaver-' in r['filename'].lower()), None)
    if wover:
        version = re.search(r'worldweaver-(.+)\.jar', wover, re.I).group(1)
        config = runtime / 'config/wover/client.json'; config.parent.mkdir(parents=True, exist_ok=True)
        config.write_text(json.dumps({'create_version': version, 'modify_version': version,
            'internal': {'did_present_welcome_screen': True}, 'general': {'check_for_new_versions': False, 'prefere_modrinth': False, 'force_betterx_world_type': True}, 'loading': {'disable_experimental_warning': False}}, indent=2))
        (output / 'offline-reference-settings.json').write_text(json.dumps({'scope': 'Disposable profile only; original publisher bytes retained', 'worldweaver': 'First-run setup completed, optional online version checks disabled; inventory defaults unchanged.'}, indent=2))
    args = ['-Xms512m', '-Xmx4g', '-XX:ActiveProcessorCount=4', '--enable-native-access=ALL-UNNAMED', '--sun-misc-unsafe-memory-access=allow', '-Dfabric.development=false', '-Dfabric.noGui=true', '-Dfabric.gameJarPath=' + str(client), '-cp', ';'.join(map(str, cp)), profile['mainClass'], '--username', 'UIAudit', '--version', mc, '--gameDir', str(runtime), '--assetsDir', str(assets), '--assetIndex', index, '--uuid', '14ea713fe12d3d8bb7b6baf52d52d598', '--accessToken', '0', '--userType', 'legacy', '--versionType', 'release', '--width', '1280', '--height', '900']
    argfile = output / 'java.args'; argfile.write_text('\n'.join(map(base.java_argument, args)))
    launch = {'java': str(java), 'arguments': str(argfile), 'cwd': str(runtime), 'environment': {'NATIVE_REFERENCE_OUTPUT': str(output), 'NATIVE_REFERENCE_LOADER': 'fabric', 'SIU_AUDIT_CORE': str(side=='siu').lower()}, 'log': str(output / 'launch.log')}
    (output / 'launch.json').write_text(json.dumps(launch, indent=2)); (output / 'staged-artifacts.json').write_text(json.dumps({'minecraft': mc, 'loader': 'fabric', 'siu_loaded':side=='siu', 'runtime': 'original named production jars; excluded observer only', 'artifacts': staged}, indent=2))
    print('Prepared', output)

if __name__ == '__main__': main()
