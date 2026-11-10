"""Read native interface config/storage and the inaccessible amount-dialog copy."""
import re
from ae2_child_observer import enhance as child_observations


def enhance(text,mc,named=False):
    text=child_observations(text,mc,named)
    anchor='if(kind.equals("regression-world"))return true;';assert text.count(anchor)==1
    command=r'''
        if(kind.equals("regression-ae2-stock-state")){
            var clients=new ArrayList<Object>();Object desktop=h("screen");
            if(desktop==null)throw new IllegalStateException("No native interface desktop");
            for(Object window:(List<?>)field(desktop,"windows")){
                if(field(window,"session")==null||field(window,"apiState")==null)continue;
                Object content;try{content=field(field(window,"apiState"),"content");}catch(IllegalArgumentException otherAdapter){continue;}
                if(content==null||!content.getClass().getName().equals("appeng.client.gui.implementations.InterfaceScreen"))continue;
                var menu=(AbstractContainerMenu)call(content,"getMenu");var cells=new ArrayList<Object>();
                for(var slot:menu.slots)if(slot.getClass().getName().equals("appeng.menu.slot.FakeSlot"))cells.add(Map.of(
                    "index",slot.getContainerSlot(),"x",slot.x,"y",slot.y,"visible",call(content,"desktopSlotVisible",slot),"decoded",releaseStockFromItem(slot.getItem(),content)));
                clients.add(Map.of("session",call(field(window,"session"),"sessionId"),"left",call(content,"getGuiLeft"),"top",call(content,"getGuiTop"),"cells",cells));
            }
            work=server(()->{
                var level=(net.minecraft.server.level.ServerLevel)h("serverLevel");Object host=level.getBlockEntity(POS);
                if(host==null)throw new IllegalStateException("Native interface source missing");Object logic=call(host,"getInterfaceLogic");
                var config=releaseStockInventory(call(logic,"getConfig"));var storage=releaseStockInventory(call(logic,"getStorage"));
                Object state=call(com.salts_inventory_update.server.DesktopContainerSessions.class,"sessions",player());var mirrors=new ArrayList<Object>();
                for(var entry:((Map<?,?>)field(state,"sessions")).entrySet()){
                    var menu=(AbstractContainerMenu)field(entry.getValue(),"menu");if(!menu.getClass().getName().equals("appeng.menu.implementations.SetStockAmountMenu"))continue;
                    for(int i=0;i<menu.slots.size();i++){
                        var slot=menu.slots.get(i);if(!slot.getClass().getName().equals("appeng.menu.slot.InaccessibleSlot"))continue;
                        mirrors.add(Map.of("session",entry.getKey(),"slot",i,"raw",regressionStack(slot.getItem()),"decoded",releaseStockFromItem(slot.getItem(),host),
                            "mayPlace",slot.mayPlace(new ItemStack(net.minecraft.world.item.Items.STONE)),"mayPickup",slot.mayPickup(player())));
                    }
                }
                write("release-ae2-stock-state.json",Map.of("host",host.getClass().getName(),"config",config,"storage",storage,"mirrors",mirrors,"client",clients));return null;
            });return true;
        }
'''
    helpers=r'''
    static Map<String,Object> releaseStockStack(Object stack){
        if(stack==null)return Map.of("empty",true);Object key=call(stack,"what"),amount=call(stack,"amount");
        return Map.of("empty",false,"item",regressionStack((ItemStack)call(key,"toStack")),"amount",amount,"amountJavaType",amount.getClass().getName());
    }
    static List<Object> releaseStockInventory(Object inventory){
        var result=new ArrayList<Object>();for(int i=0;i<((Number)call(inventory,"size")).intValue();i++)result.add(releaseStockStack(call(inventory,"getStack",i)));return result;
    }
    static Map<String,Object> releaseStockFromItem(ItemStack item,Object host){
        try{return releaseStockStack(call(host.getClass().getClassLoader().loadClass("appeng.api.stacks.GenericStack"),"fromItemStack",item));}
        catch(ClassNotFoundException error){throw new IllegalStateException(error);}
    }
'''
    if named:command=re.sub(r'\bPOS\b','origin()',command)
    else:
        command=re.sub(r'\bh\(','regressionH(',command)
        command=re.sub(r'\bcall\(','coreCall(',command);helpers=re.sub(r'\bcall\(','coreCall(',helpers)
    text=text.replace(anchor,anchor+command,1)
    return text[:text.rfind('}')]+helpers+'\n}\n'
