"""Excluded SSN observation fixture; shipping mods are copied without modification."""
from pathlib import Path
import importlib.util,sys
ROOT=Path(__file__).resolve().parents[1]

def load(name,path):
    spec=importlib.util.spec_from_file_location(name,ROOT/path)
    module=importlib.util.module_from_spec(spec);spec.loader.exec_module(module);return module

VALUES=r'''
        if(screen.getClass().getName().startsWith("com.lothrazar.storagenetwork."))try{
            for(String key:List.of("getSort","getDownwards","getAutoFocus","getFullStackCraft"))try{values.put(key,String.valueOf(coreCall(screen,key)));}catch(Exception absent){}
            try{
                Object network=coreCall(screen,"getNetwork");
                for(String key:List.of("page","maxPage","lines","columns"))try{values.put(key,field(network,key));}catch(Exception absent){}
                Object search=field(network,"searchBar");values.put("search",coreCall(search,"getValue"));values.put("searchFocused",coreCall(search,"isFocused"));
                values.put("searchBounds",Map.of("x",coreCall(search,"getX"),"y",coreCall(search,"getY"),"width",coreCall(search,"getWidth"),"height",coreCall(search,"getHeight")));
                var entries=new ArrayList<Object>();for(Object entry:(java.util.Collection<?>)field(network,"stacks")){
                    if(entry instanceof ItemStack stack)entries.add(Map.of("item",BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(),"count",stack.getCount()));
                    else entries.add(String.valueOf(entry));
                }values.put("stacks",entries);
            }catch(Exception absent){values.put("networkProbe",absent.toString());}
        }catch(Exception error){values.put("ssnObserverError",error.toString());}
'''

SERVER=r'''
        if(menu.getClass().getName().startsWith("com.lothrazar.storagenetwork.")){
            try{
                Object cap=field(menu,"cap");
                for(String method:List.of("getPriority","needsRedstone","isOperationMode"))try{out.put(method,coreCall(cap,method));}catch(Exception absent){}
                for(String method:List.of("getFilter","getFilters","getFiltersOut"))try{
                    Object filter=coreCall(cap,method);try{out.put(method+"AllowList",field(filter,"isAllowList"));}catch(Exception absent){}int size=((Number)coreCall(filter,"getSlots")).intValue();var items=new ArrayList<Object>();
                    for(int i=0;i<size;i++){var s=(ItemStack)coreCall(filter,"getStackInSlot",i);items.add(Map.of("item",BuiltInRegistries.ITEM.getKey(s.getItem()).toString(),"count",s.getCount()));}out.put(method,items);
                }catch(Exception absent){}
                for(String key:List.of("operationType","operationLimit"))try{out.put(key,field(cap,key));}catch(Exception absent){}
            }catch(Exception absent){}
            try{Object model=coreCall(field(menu,"tile"),"getProcessModel");for(String key:List.of("getCount","isAlwaysActive","getInputFace","getOutputFace","getStatus"))try{out.put(key,String.valueOf(coreCall(model,key)));}catch(Exception absent){}}catch(Exception absent){}
            return out;
        }
'''

def patch(text,named):
    design=load('ssn_design',Path('tools/prepare-ssn-design.py'))
    if 'case "move"->screen().mouseMoved('in text:
        text=text.replace('case "move"->screen().mouseMoved(','case "move"->{try{var px=mc.mouseHandler.getClass().getDeclaredField("xpos");px.setAccessible(true);var py=mc.mouseHandler.getClass().getDeclaredField("ypos");py.setAccessible(true);px.setDouble(mc.mouseHandler,c.get("x").getAsDouble()*mc.getWindow().getGuiScale());py.setDouble(mc.mouseHandler,c.get("y").getAsDouble()*mc.getWindow().getGuiScale());}catch(Exception e){throw new IllegalStateException(e);}screen().mouseMoved(')
        text=text.replace('screen().mouseMoved(c.get("x").getAsDouble(),c.get("y").getAsDouble());','screen().mouseMoved(c.get("x").getAsDouble(),c.get("y").getAsDouble());}')
        # The design helper only needs to add its disposable network fixture.
        text=text.replace('case "fixture"->{',design.EXTRA+'case "fixture"->{',1)
    else:
        text=design.patch(text,named)
    needle='static Map<String,Object> nativeValues(Object screen){'
    assert needle in text
    start=text.index(needle);end=text.index('return values;',start)
    text=text[:end]+VALUES+text[end:]
    needle='if(!menu.getClass().getName().startsWith("com.tom.storagemod."))return out;'
    if needle in text:text=text.replace(needle,SERVER+needle)
    return text

if __name__=='__main__':
    if '--neo' not in sys.argv:
        prep=load('ssn_forge_implementation',Path('tools/prepare-rollout-production-forge.py'))
        old=prep.modern.qa_source
        def qa(side):
            key,source=old(side);return key,patch(source,False)
        prep.modern.qa_source=qa;prep.main()
    else:
        version=sys.argv[sys.argv.index('--neo')+1]
        source=ROOT/f'build/all-target-rollout/native-observers/java-{version}'
        if version=='1.21.1':source=source.with_name('java-1.21.1-current')
        source=source/'com/siu_reference'
        out=ROOT/f'build/ssn-implementation/observer-{version}/com/siu_reference';out.mkdir(parents=True,exist_ok=True)
        if version.startswith('26.'):
            modern=load('ssn_modern',Path('tools/prepare-rollout-production-modern.py'));modern.MC=version
            _,text=modern.qa_source('siu')
            text=text.replace('case "wait"->{}','case "gui-scale"->{mc.options.guiScale().set(num(c,"scale",2));resizeGui();}\n            case "wait"->{}')
            text=text.replace('.save(new net.minecraft.nbt.CompoundTag())','.toString()').replace('.getInventory().selected','.getInventory().getSelectedSlot()')
            text=text.replace('net.minecraftforge.fml.ModList','net.neoforged.fml.ModList').replace('new net.minecraft.client.input.CharacterEvent(letter,0)','new net.minecraft.client.input.CharacterEvent(letter)')
            start=text.index('case "snapshot-server"->');end=text.index('case "key"->',start)
            snapshot=r'''case "snapshot-server"->work=server(()->{
                var menus=new ArrayList<AbstractContainerMenu>();menus.add(player().containerMenu);
                Class.forName("com.salts_inventory_update.api.server.desktop.DesktopSessions").getMethod("hasOpenSessionMatching",net.minecraft.world.entity.player.Player.class,java.util.function.Predicate.class)
                    .invoke(null,player(),(java.util.function.Predicate<AbstractContainerMenu>)m->{if(!menus.contains(m))menus.add(m);return false;});
                var rows=new ArrayList<Object>();for(var menu:menus){var slots=new ArrayList<Object>();for(int i=0;i<menu.slots.size();i++)slots.add(Map.of("slot",i,"item",menu.getSlot(i).getItem().toString()));rows.add(Map.of("class",menu.getClass().getName(),"carried",menu.getCarried().toString(),"slots",slots,"settings",observedSsn(menu)));}
                var inventory=new ArrayList<Object>();for(int i=0;i<player().getInventory().getContainerSize();i++)inventory.add(Map.of("index",i,"item",player().getInventory().getItem(i).toString()));
                write("server-state.json",Map.of("inventory",inventory,"menus",rows,"fixture",G.toJson(rows)));return true;
            });
            '''
            # Reflective reporting is test-only; convert checked reflection errors for Supplier.
            snapshot=snapshot.replace('var menus=new','try{var menus=new').replace('return true;\n            });','return true;}catch(Exception e){throw new IllegalStateException(e);}\n            });')
            text=text[:start]+snapshot+text[end:]
            helper='static Map<String,Object> observedSsn(AbstractContainerMenu menu){var out=new LinkedHashMap<String,Object>();'+SERVER+'return out;}\n'
            text=text[:text.rfind('}')]+helper+text[text.rfind('}'):]
            text=patch(text,True).replace('p.serverLevel()','p.level()')
            text=text.replace('BuiltInRegistries.BLOCK.get(id(e.getValue()))','BuiltInRegistries.BLOCK.getValue(id(e.getValue()))').replace('BuiltInRegistries.ITEM.get(id("minecraft:"+items[i]))','BuiltInRegistries.ITEM.getValue(id("minecraft:"+items[i]))')
            # This modern observer calls its native widget reporter widget().
            text=text.replace('observeWidget(w)','Map.of("type",w.getClass().getName(),"x",w.getX(),"y",w.getY(),"width",w.getWidth(),"height",w.getHeight(),"visible",w.visible,"active",w.active,"message",w.getMessage()==null?"":w.getMessage().getString())')
            (out/'NativeUiReference.java').write_text(text)
            (out/'RuntimeNames.java').write_text((ROOT/'build/all-target-rollout/native-observers/java-1.21.1-current/com/siu_reference/RuntimeNames.java').read_text())
        else:
            (out/'NativeUiReference.java').write_text(patch((source/'NativeUiReference.java').read_text(),True))
            (out/'RuntimeNames.java').write_text((source/'RuntimeNames.java').read_text())
        init=(ROOT/'functional-tests/all-target-rollout/native.init.gradle').read_text()
        line=next(l for l in init.splitlines() if l.strip().startswith('qa.java.srcDir(root.file(enhancedNeo?'))
        init=init.replace(line,f"    qa.java.srcDir(root.file('build/ssn-implementation/observer-{version}'));"+line.split(';',1)[1])
        if version.startswith('26.'):
            init=init.replace("def enhancedNeo = minecraft=='1.21.1' && loader=='neoforge'","def enhancedNeo = minecraft in ['1.21.1','26.1.2'] && loader=='neoforge'")
        (ROOT/f'build/ssn-implementation/native-{version}.init.gradle').write_text(init)
        print('Prepared isolated SSN observer',version)
