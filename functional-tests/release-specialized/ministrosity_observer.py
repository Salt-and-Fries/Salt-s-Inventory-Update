"""Read original pet cargo, owner, cells and cursors without changing them."""
import re
from native_item_serialization import storage_item_saves


def enhance(text,mc,named=False):
    anchor='if(kind.equals("regression-world"))return true;';assert text.count(anchor)==1
    commands=r'''
        if(kind.equals("regression-ministrosity-state")){
            Object window=releaseMiniWindow(),screen=field(field(window,"apiState"),"screen");
            var menu=(AbstractContainerMenu)call(screen,"getMenu");
            var client=new LinkedHashMap<String,Object>();client.put("menu",menu.getClass().getName());client.put("screen",screen.getClass().getName());
            client.put("session",call(field(window,"session"),"sessionId"));client.put("locked",field(window,"locked"));
            client.put("bounds",Map.of("x",field(window,"x"),"y",field(window,"y"),"width",field(window,"width"),"height",field(window,"height")));
            work=server(()->{
                var menus=new ArrayList<AbstractContainerMenu>();menus.add(player().containerMenu);
                com.salts_inventory_update.api.server.desktop.DesktopSessions.hasOpenSessionMatching(player(),m->{if(!menus.contains(m))menus.add(m);return false;});
                var pets=menus.stream().filter(m->m.getClass().getName().equals("com.github.L_Ender.cataclysm.inventory.MinistrostiyMenu")).toList();
                if(pets.size()!=1)throw new IllegalStateException("Expected one original Ministrosity menu");
                var selected=pets.get(0);var cells=new ArrayList<Object>();
                for(int i=0;i<selected.slots.size();i++){Slot slot=selected.getSlot(i);if(slot.container!=player().getInventory())cells.add(Map.of("index",i,"item",releaseMiniStack(slot.getItem())));}
                var inventory=new ArrayList<Object>();for(int i=0;i<player().getInventory().getContainerSize();i++)inventory.add(Map.of("index",i,"item",releaseMiniStack(player().getInventory().getItem(i))));
                var cursors=new ArrayList<Object>();for(var item:menus)cursors.add(Map.of("menu",item.getClass().getName(),"item",releaseMiniStack(item.getCarried())));
                Object owner;
                try{owner=call(Class.forName("com.salts_inventory_update.compat.nativewindows.ReviewedEntityInventories"),"owner",selected);}
                catch(ClassNotFoundException e){throw new IllegalStateException(e);}
                var entity=(net.minecraft.world.entity.Entity)owner;
                write("release-ministrosity-state.json",Map.of("client",client,"owner",entity.getUUID().toString(),"cells",cells,"inventory",inventory,"cursors",cursors));return null;
            });return true;
        }
'''
    helpers=r'''
    static Object releaseMiniWindow(){
        var values=new ArrayList<Object>();Object desktop=h("screen");if(desktop==null)throw new IllegalStateException("No desktop");
        for(Object w:(List<?>)field(desktop,"windows")){Object s=field(w,"session");if(s!=null&&call(s,"menu").getClass().getName().equals("com.github.L_Ender.cataclysm.inventory.MinistrostiyMenu")&&field(w,"apiState")!=null)values.add(w);}
        if(values.size()!=1)throw new IllegalStateException("Expected one companion window, found "+values.size());return values.get(0);
    }
    static String releaseMiniStack(ItemStack value){
        if(value.isEmpty())return "{}";var ops=net.minecraft.resources.RegistryOps.create(net.minecraft.nbt.NbtOps.INSTANCE,player().registryAccess());
        return ItemStack.CODEC.encodeStart(ops,value).result().orElseThrow(()->new IllegalStateException("Native pet stack codec refused serialization")).toString();
    }
'''
    helpers=storage_item_saves(helpers,mc)
    if not named:
        commands=re.sub(r'\bh\(','regressionH(',commands);commands=re.sub(r'\bcall\(','coreCall(',commands)
        helpers=re.sub(r'\bh\(','regressionH(',helpers);helpers=re.sub(r'\bcall\(','coreCall(',helpers)
    text=text.replace(anchor,anchor+commands,1)
    return text[:text.rfind('}')]+helpers+'\n}\n'
