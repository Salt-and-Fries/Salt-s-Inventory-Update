"""Read actual Accessories capability inventories and paired rendered cells.

The sole additional action is a real pointer click on a rendered visibility
control. No capability, menu, native validator or handler is assigned or called
to manufacture the result of a tested operation.
"""
import re
from native_item_serialization import storage_item_saves


def enhance(text,mc,named=False):
    assert mc in ['1.20.1','1.21.1']
    anchor='if(kind.equals("regression-world"))return true;'
    assert text.count(anchor)==1
    command=r'''
        if(kind.equals("regression-accessories-equipment-state")){
            Object desktop=h("screen");var panels=new ArrayList<Object>();
            if(desktop!=null)for(Object w:(List<?>)field(desktop,"windows")){
                Object session=field(w,"session"),definition=field(w,"apiDefinition");
                if(session==null||definition==null||!call(session,"menu").getClass().getName().endsWith(".AccessoriesDesktopMenu"))continue;
                Object context=call(desktop,"apiContext",w,null,0,0);var visible=new ArrayList<Object>();
                for(Object displayed:(List<?>)call(definition,"cells",context)){
                    Object model=field(displayed,"model"),group=field(model,"group");Slot slot=(Slot)field(model,"slot");
                    visible.add(Map.of("slot",slot.index,"group",call(group,"name"),"index",field(model,"index"),
                        "cosmetic",field(model,"cosmetic"),"render",field(model,"render"),"x",field(displayed,"x"),"y",field(displayed,"y")));
                }
                panels.add(Map.of("session",call(session,"sessionId"),"cells",visible,"bounds",releaseAccessoryBounds(w)));
            }
            Object character=desktop==null?null:h("windowKind","CHARACTER");
            var client=new LinkedHashMap<String,Object>();client.put("panels",panels);
            client.put("character",character==null?null:releaseAccessoryBounds(character));
            client.put("characterLocked",character==null?null:field(character,"locked"));
            work=server(()->{
                Object capability=releaseAccessoryCapability();
                if(capability==null)throw new IllegalStateException("No actual Accessories capability");
                var groups=new TreeMap<String,Object>();
                for(var entry:((Map<?,?>)call(capability,"getContainers")).entrySet()){
                    Object container=entry.getValue();Container normal=(Container)call(container,"getAccessories"),cosmetic=(Container)call(container,"getCosmeticAccessories");
                    var real=new ArrayList<String>();var visual=new ArrayList<String>();
                    for(int i=0;i<normal.getContainerSize();i++)real.add(releaseAccessoryStack(normal.getItem(i)));
                    for(int i=0;i<cosmetic.getContainerSize();i++)visual.add(releaseAccessoryStack(cosmetic.getItem(i)));
                    groups.put(entry.getKey().toString(),Map.of("normal",real,"cosmetic",visual,"render",new ArrayList<>((List<?>)call(container,"renderOptions"))));
                }
                var menus=new ArrayList<AbstractContainerMenu>();menus.add(player().inventoryMenu);
                if(player().containerMenu!=player().inventoryMenu)menus.add(player().containerMenu);
                com.salts_inventory_update.api.server.desktop.DesktopSessions.hasOpenSessionMatching(player(),m->{if(!menus.contains(m))menus.add(m);return false;});
                var sources=new ArrayList<String>();var cursors=new ArrayList<Object>();
                for(var menu:menus){sources.add(menu.getClass().getName());cursors.add(Map.of("menu",menu.getClass().getName(),"item",releaseAccessoryStack(menu.getCarried())));}
                var inventory=new ArrayList<Object>();for(int i=0;i<player().getInventory().getContainerSize();i++)inventory.add(Map.of("index",i,"item",releaseAccessoryStack(player().getInventory().getItem(i))));
                write("release-accessories-equipment-state.json",Map.of("client",client,"groups",groups,"inventory",inventory,
                    "cursors",cursors,"canonicalCursor",releaseAccessoryStack(player().inventoryMenu.getCarried()),"sources",sources));return null;
            });return true;
        }
        if(kind.equals("regression-accessories-visibility-tap")){
            Object desktop=h("screen"),window=h("windowSession",num(c,"session",-1));
            if(window==null)throw new IllegalStateException("Accessories window missing");
            Object definition=field(window,"apiDefinition"),context=call(desktop,"apiContext",window,null,0,0),found=null;
            for(Object cell:(List<?>)call(definition,"cells",context)){
                Object model=field(cell,"model");if(((Slot)field(model,"slot")).index==num(c,"slot",-1)&&!(Boolean)field(model,"cosmetic"))found=cell;
            }
            if(found==null)throw new IllegalStateException("No actual visible normal cell");
            double x=((Number)field(found,"x")).doubleValue()+15,y=((Number)field(found,"y")).doubleValue();
            call(desktop,"bringToFront",window);call(desktop,"setFocusedWindow",window);
            h("mouse","mouseClicked",x,y,0,0D,0D);h("mouse","mouseReleased",x,y,0,0D,0D);return true;
        }
'''
    helpers=r'''
    static Object releaseAccessoryCapability(){
        try{return call(Class.forName("io.wispforest.accessories.api.AccessoriesCapability"),"get",player());}
        catch(ClassNotFoundException missing){throw new IllegalStateException("Native Accessories capability is absent",missing);}
    }
    static Map<String,Object> releaseAccessoryBounds(Object window){return Map.of("x",field(window,"x"),"y",field(window,"y"),"width",field(window,"width"),"height",field(window,"height"));}
    static String releaseAccessoryStack(ItemStack value){
        if(value.isEmpty())return "{}";var ops=net.minecraft.resources.RegistryOps.create(net.minecraft.nbt.NbtOps.INSTANCE,player().registryAccess());
        return ItemStack.CODEC.encodeStart(ops,value).result().orElseThrow(()->new IllegalStateException("Native Accessories item serialization failed")).toString();
    }
'''
    helpers=storage_item_saves(helpers,mc)
    if not named:
        command=re.sub(r'\bh\(','regressionH(',command);command=re.sub(r'\bcall\(','coreCall(',command)
        helpers=re.sub(r'\bh\(','regressionH(',helpers);helpers=re.sub(r'\bcall\(','coreCall(',helpers)
    text=text.replace(anchor,anchor+command,1)
    return text[:text.rfind('}')]+helpers+'\n}\n'
