"""Read original MI virtual item/fluid cells and their native coordinates."""
import re


def enhance(text,mc,named=False):
    assert mc in {'1.20.1','1.21.1'}
    anchor='if(kind.equals("regression-world"))return true;';assert text.count(anchor)==1
    command=r'''
        if(kind.equals("regression-mi-storage-state")){
            var values=new ArrayList<Object>();Object desktop=h("screen");
            if(desktop==null)throw new IllegalStateException("No desktop for native MI storage");
            for(Object window:(List<?>)field(desktop,"windows")){
                Object session=field(window,"session");if(session==null||field(window,"apiState")==null)continue;
                Object screen=field(field(window,"apiState"),"screen");
                var menu=(AbstractContainerMenu)call(screen,"getMenu");
                if(!menu.getClass().getName().startsWith("aztech.modern_industrialization."))continue;
                Object inventory=field(menu,"inventory");var cells=new LinkedHashMap<String,Object>();
                var controls=new LinkedHashMap<String,Object>();controls.put("lockingMode",field(menu,"lockingMode"));
                controls.put("hasLockButton",field(field(menu,"guiParams"),"lockButton"));
                var extraction=new ArrayList<Object>();
                for(Object component:(Iterable<?>)field(menu,"components"))if(component.getClass().getSimpleName().equals("AutoExtractClient")){
                    var option=new LinkedHashMap<String,Object>();option.put("type",component.getClass().getName());
                    EXTRACTION_STATE
                    extraction.add(option);
                }
                controls.put("autoExtract",extraction);
                for(String kindName:List.of("Item","Fluid")){
                    var stacks=(List<?>)call(inventory,"get"+kindName+"Stacks");
                    Object positions=field(inventory,kindName.toLowerCase()+"Positions");
                    var entries=new ArrayList<Object>();
                    for(int index=0;index<stacks.size();index++){
                        Object cell=stacks.get(index);Object nbt=SERIALIZE;
                        entries.add(Map.of("index",index,"x",call(positions,"getX",index),"y",call(positions,"getY",index),"nbt",nbt.toString()));
                    }
                    cells.put(kindName.toLowerCase()+"s",entries);
                }
                values.add(Map.of("menu",menu.getClass().getName(),"screen",screen.getClass().getName(),"cells",cells,"controls",controls));
            }
            if(values.size()!=1)throw new IllegalStateException("Expected one native MI storage menu, got "+values.size());
            write("release-mi-storage-state.json",values.get(0));return true;
        }
'''
    command=command.replace('SERIALIZE','call(cell,"toNbt")'if mc=='1.20.1'else'call(cell,"toNbt",mc.player.registryAccess())')
    command=command.replace('EXTRACTION_STATE',r'''
                    boolean[] status=(boolean[])field(component,"extractStatus");
                    if(status.length!=2)throw new IllegalStateException("Unexpected native extraction status shape");
                    option.put("hasExtractItems",field(component,"hasExtractItems"));option.put("hasExtractFluids",field(component,"hasExtractFluids"));
                    option.put("extractItems",status[0]);option.put("extractFluids",status[1]);
    '''if mc=='1.20.1'else r'''
                    Object params=field(component,"params"),data=field(component,"data");
                    option.put("hasExtractItems",call(params,"hasExtractItems"));option.put("hasExtractFluids",call(params,"hasExtractFluids"));
                    option.put("extractItems",call(data,"extractItems"));option.put("extractFluids",call(data,"extractFluids"));
    ''')
    if not named:
        command=re.sub(r'\bh\(','regressionH(',command);command=re.sub(r'\bcall\(','coreCall(',command)
    return text.replace(anchor,anchor+command,1)
