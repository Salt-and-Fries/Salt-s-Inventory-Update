"""Excluded observations for focused Ad Astra acceptance, never shipping code."""
from pathlib import Path
import importlib.util,sys
ROOT=Path(__file__).resolve().parents[2]
sys.path.insert(0,str(ROOT/'tools'))
EXTRA=r'''
            case "ad-astra-state", "ad-astra-menu-fill"->work=server(()->{
                try{
                    var menus=new ArrayList<AbstractContainerMenu>();
                    if(coreMode())Class.forName("com.salts_inventory_update.api.server.desktop.DesktopSessions")
                        .getMethod("hasOpenSessionMatching",net.minecraft.world.entity.player.Player.class,java.util.function.Predicate.class)
                        .invoke(null,player(),(java.util.function.Predicate<AbstractContainerMenu>)m->{if(m.getClass().getName().startsWith("earth.terrarium.adastra.")||m.getClass().getName().startsWith("ad_astra_giselle_addon."))menus.add(m);return false;});
                    else menus.add(player().containerMenu);
                    var rows=new ArrayList<Object>();
                    for(var m:menus){
                        if(str(c,"type","").equals("ad-astra-menu-fill")&&m.getClass().getName().endsWith(str(c,"menu",""))){
                            m.getSlot(num(c,"slot",0)).set(stack(c));
                            if(m.getClass().getName().endsWith(".NasaWorkbenchMenu"))coreCall(coreCall(m,"getEntity"),"update");
                            m.broadcastChanges();
                        }
                        var row=new LinkedHashMap<String,Object>();row.put("class",m.getClass().getName());row.put("worldTime",player().level().getGameTime());
                        try{Object owner=coreCall(m,"getEntity");row.put("owner",owner.getClass().getName());
                            if(owner instanceof net.minecraft.world.level.block.entity.BlockEntity be){
                                Object nbt;try{nbt=coreCall(be,"saveWithFullMetadata");}catch(Exception modern){nbt=coreCall(be,"saveWithFullMetadata",coreCall(player(),"registryAccess"));}
                                row.put("nbt",nbt.toString());
                                for(String method:List.of("getRedstoneControl","getSideConfig","getTargetGravity"))try{row.put(method,coreCall(be,method).toString());}catch(Exception absent){}
                            }
                        }catch(Exception absent){}
                        var cells=new ArrayList<Object>();
                        for(int i=0;i<m.slots.size();i++){var s=m.getSlot(i);cells.add(Map.of("index",i,"item",BuiltInRegistries.ITEM.getKey(s.getItem().getItem()).toString(),"count",s.getItem().getCount(),"player",s.container instanceof Inventory,
                            "storage",coreCall(Class.forName("com.salts_inventory_update.internal.desktop.DesktopSortSlots"),"storageSlot",m,s)));}
                        row.put("slots",cells);rows.add(row);
                    }
                    write("ad-astra-state.json",Map.of("menus",rows));return true;
                }catch(Exception error){throw new IllegalStateException(error);}
            });
'''
if __name__=='__main__':
    forge='--forge' in sys.argv
    if forge:sys.argv.remove('--forge')
    script='prepare-rollout-production-forge.py'if forge else'prepare-rollout-production-modern.py'
    spec=importlib.util.spec_from_file_location('ad_astra_production',ROOT/'tools'/script)
    prep=importlib.util.module_from_spec(spec);spec.loader.exec_module(prep)
    source=prep.modern if forge else prep
    old=source.qa_source
    def qa(side):
        path,text=old(side)
        text=text.replace('info.put("controls",controls);','''info.put("controls",controls);
                if(field(desktop,"popupWindow")==window){
                    Object popup=coreCall(desktop,"popupRect",window);var popupControls=new LinkedHashMap<String,Object>();
                    for(Object rect:(List<?>)coreCall(desktop,"popupControlRects",window,popup))popupControls.put(coreCall(rect,"control").toString(),Map.of("x",((Number)coreCall(rect,"x")).intValue()+5,"y",((Number)coreCall(rect,"y")).intValue()+5));
                    info.put("popupControls",popupControls);
                }''',1)
        return path,text.replace('case "fixture"->{',EXTRA+'case "fixture"->{',1)
    source.qa_source=qa
    if '--neo-prepare'in sys.argv:
        source.MC='1.21.1';_,text=qa('siu');text=text.replace('net.minecraftforge.fml.ModList','net.neoforged.fml.ModList')
        out=ROOT/'build/ad-astra-implementation/observer-1.21.1/com/siu_reference';out.mkdir(parents=True,exist_ok=True)
        (out/'NativeUiReference.java').write_text(text,encoding='utf-8')
        (out/'RuntimeNames.java').write_text((ROOT/'build/ssn-implementation/observer-1.21.1/com/siu_reference/RuntimeNames.java').read_text(encoding='utf-8'),encoding='utf-8')
        init=(ROOT/'build/powah-all-targets/native-1.21.1.init.gradle').read_text(encoding='utf-8').replace('build/powah-all-targets/observer-1.21.1','build/ad-astra-implementation/observer-1.21.1')
        (ROOT/'build/ad-astra-implementation/native-1.21.1.init.gradle').write_text(init,encoding='utf-8')
        print('Prepared excluded NeoForge observer')
    else:prep.main()
