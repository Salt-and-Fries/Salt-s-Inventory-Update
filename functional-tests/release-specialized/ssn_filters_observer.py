"""Read original SSN filter state and geometry without mutating native menus."""
import re
from native_item_serialization import storage_item_saves


def enhance(text, mc, named=False):
    assert mc in ['1.20.1', '1.21.1', '26.1.2']
    anchor='if(kind.equals("regression-world"))return true;'
    assert text.count(anchor)==1
    command=r'''
        if(kind.equals("regression-ssn-filters-state")){
            Object window=releaseSsnFilterWindow(),definition=field(window,"apiDefinition"),screen=field(field(definition,"initial"),"screen");
            var nativeMenu=(AbstractContainerMenu)call(screen,"getMenu");
            int left=((Number)field(screen,"leftPos")).intValue(),top=((Number)field(screen,"topPos")).intValue();
            var client=new LinkedHashMap<String,Object>();client.put("menu",nativeMenu.getClass().getName());client.put("screen",screen.getClass().getName());
            client.put("settings",releaseSsnFilterSettings(nativeMenu));
            var buttons=new LinkedHashMap<String,Object>();
            if(nativeMenu.getClass().getSimpleName().equals("ContainerCableFilter"))for(String name:List.of("btnPlus","btnMinus","btnAllowIgn","btnImport")){
                Object button=field(screen,name);buttons.put(name,Map.of("x",((Number)call(button,"getX")).intValue()+((Number)call(button,"getWidth")).intValue()/2-left,
                    "y",((Number)call(button,"getY")).intValue()+((Number)call(button,"getHeight")).intValue()/2-top,"active",field(button,"active"),"visible",field(button,"visible")));
            }
            client.put("buttons",buttons);client.put("session",call(field(window,"session"),"sessionId"));client.put("locked",field(window,"locked"));
            client.put("bounds",Map.of("x",field(window,"x"),"y",field(window,"y"),"width",field(window,"width"),"height",field(window,"height")));
            work=server(()->{
                var menus=new ArrayList<AbstractContainerMenu>();menus.add(player().inventoryMenu);
                if(player().containerMenu!=player().inventoryMenu)menus.add(player().containerMenu);
                com.salts_inventory_update.api.server.desktop.DesktopSessions.hasOpenSessionMatching(player(),m->{if(!menus.contains(m))menus.add(m);return false;});
                var filters=menus.stream().filter(m->releaseSsnFilterMenu(m)).toList();
                if(filters.size()!=1)throw new IllegalStateException("Expected one original SSN filter menu: "+filters.size());
                var inventory=new ArrayList<Object>();for(int i=0;i<player().getInventory().getContainerSize();i++)inventory.add(Map.of("index",i,"item",releaseSsnFilterStack(player().getInventory().getItem(i))));
                var cursors=new ArrayList<Object>();for(var m:menus)cursors.add(Map.of("menu",m.getClass().getName(),"item",releaseSsnFilterStack(m.getCarried())));
                write("release-ssn-filters-state.json",Map.of("client",client,"settings",releaseSsnFilterSettings(filters.get(0)),"inventory",inventory,"cursors",cursors));return null;
            });return true;
        }
'''
    helpers=r'''
    static boolean releaseSsnFilterMenu(AbstractContainerMenu menu){
        return List.of("com.lothrazar.storagenetwork.block.cable.linkfilter.ContainerCableFilter",
            "com.lothrazar.storagenetwork.block.collection.ContainerCollectionFilter").contains(menu.getClass().getName());
    }
    static Object releaseSsnFilterWindow(){
        var found=new ArrayList<Object>();Object desktop=h("screen");if(desktop==null)throw new IllegalStateException("No desktop");
        for(Object w:(List<?>)field(desktop,"windows")){Object session=field(w,"session");if(session!=null&&releaseSsnFilterMenu((AbstractContainerMenu)call(session,"menu"))&&field(w,"apiDefinition")!=null)found.add(w);}
        if(found.size()!=1)throw new IllegalStateException("Expected one SSN filter extraction: "+found.size());return found.get(0);
    }
    static Map<String,Object> releaseSsnFilterSettings(AbstractContainerMenu menu){
        Object cap=field(menu,"cap"),filter=call(cap,"getFilter");var result=new LinkedHashMap<String,Object>();
        var ghosts=new ArrayList<String>();int size=((Number)call(filter,"getSlots")).intValue();
        for(int i=0;i<size;i++)ghosts.add(releaseSsnFilterStack((ItemStack)call(filter,"getStackInSlot",i)));
        result.put("ghosts",ghosts);
        if(menu.getClass().getSimpleName().equals("ContainerCableFilter")){
            result.put("priority",call(cap,"getPriority"));result.put("allowList",field(filter,"isAllowList"));
        }
        return result;
    }
    static String releaseSsnFilterStack(ItemStack value){
        if(value.isEmpty())return "{}";var ops=net.minecraft.resources.RegistryOps.create(net.minecraft.nbt.NbtOps.INSTANCE,player().registryAccess());
        return ItemStack.CODEC.encodeStart(ops,value).result().orElseThrow(()->new IllegalStateException("Native SSN filter item codec refused serialization")).toString();
    }
'''
    helpers=storage_item_saves(helpers,mc)
    if not named:
        command=re.sub(r'\bh\(', 'regressionH(',command);command=re.sub(r'\bcall\(', 'coreCall(',command)
        helpers=re.sub(r'\bh\(', 'regressionH(',helpers);helpers=re.sub(r'\bcall\(', 'coreCall(',helpers)
    text=text.replace(anchor,anchor+command,1)
    return text[:text.rfind('}')]+helpers+'\n}\n'
