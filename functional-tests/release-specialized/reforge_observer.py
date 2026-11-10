"""Observe native reforging readiness, quality and exact costs, never RNG/actions."""
import re
from native_item_serialization import storage_item_saves


def enhance(text,mc,named=False):
    anchor='if(kind.equals("regression-world"))return true;';assert text.count(anchor)==1
    command=r'''
        if(kind.equals("regression-reforge-state")){
            Object window=releaseReforgeWindow(),screen=field(field(window,"apiState"),"screen");var menu=(AbstractContainerMenu)call(screen,"getMenu");
            var client=new LinkedHashMap<String,Object>();client.put("menu",menu.getClass().getName());client.put("screen",screen.getClass().getName());
            client.put("session",call(field(window,"session"),"sessionId"));client.put("locked",field(window,"locked"));
            client.put("bounds",Map.of("x",field(window,"x"),"y",field(window,"y"),"width",field(window,"width"),"height",field(window,"height")));
            var buttons=new ArrayList<Object>();for(Object widget:(List<?>)call(screen,"children"))if(widget.getClass().getSimpleName().equals("ReforgeButton"))
                buttons.add(Map.of("type",widget.getClass().getName(),"message",((net.minecraft.network.chat.Component)call(widget,"getMessage")).getString(),"disabled",field(widget,"disabled"),"active",field(widget,"active"),"visible",field(widget,"visible")));
            client.put("buttons",buttons);
            work=server(()->{
                var menus=new ArrayList<AbstractContainerMenu>();menus.add(player().containerMenu);
                com.salts_inventory_update.api.server.desktop.DesktopSessions.hasOpenSessionMatching(player(),m->{if(!menus.contains(m))menus.add(m);return false;});
                var choices=menus.stream().filter(m->m.getClass().getName().endsWith(".ReforgeScreenHandler")).toList();
                if(choices.size()!=1)throw new IllegalStateException("Expected one original reforge menu");var owner=choices.get(0);
                var cells=new ArrayList<Object>();for(int i=0;i<owner.slots.size();i++)if(owner.getSlot(i).container!=player().getInventory())cells.add(Map.of("index",i,"item",releaseReforgeStack(owner.getSlot(i).getItem())));
                Object quality;var known=new ArrayList<String>();
                try{
                    quality=call(Class.forName("draylar.tiered.api.ModifierUtils"),"RELEASE_QUALITY_GETTER",owner.getSlot(1).getItem());
                    Object definitions=field(Class.forName("draylar.tiered.Tiered"),"ATTRIBUTE_DATA_LOADER");
                    for(Object key:((Map<?,?>)call(definitions,"getItemAttributes")).keySet())known.add(key.toString());
                }catch(ClassNotFoundException e){throw new IllegalStateException(e);}
                var inventory=new ArrayList<Object>();for(int i=0;i<player().getInventory().getContainerSize();i++)inventory.add(Map.of("index",i,"item",releaseReforgeStack(player().getInventory().getItem(i))));
                var cursors=new ArrayList<Object>();for(var m:menus)cursors.add(Map.of("menu",m.getClass().getName(),"item",releaseReforgeStack(m.getCarried())));
                write("release-reforge-state.json",Map.of("client",client,"ready",field(owner,"reforgeReady"),"quality",quality==null?"":quality.toString(),"knownQualities",known,"cells",cells,"inventory",inventory,"cursors",cursors));return null;
            });return true;
        }
'''
    helpers=r'''
    static Object releaseReforgeWindow(){
        var found=new ArrayList<Object>();Object desktop=h("screen");if(desktop==null)throw new IllegalStateException("No desktop");
        for(Object w:(List<?>)field(desktop,"windows")){Object s=field(w,"session");if(s!=null&&call(s,"menu").getClass().getName().endsWith(".ReforgeScreenHandler")&&field(w,"apiState")!=null)found.add(w);}
        if(found.size()!=1)throw new IllegalStateException("Expected one native reforge window: "+found.size());return found.get(0);
    }
    static String releaseReforgeStack(ItemStack value){
        if(value.isEmpty())return "{}";var ops=net.minecraft.resources.RegistryOps.create(net.minecraft.nbt.NbtOps.INSTANCE,player().registryAccess());
        return ItemStack.CODEC.encodeStart(ops,value).result().orElseThrow(()->new IllegalStateException("Reforge item codec refused serialization")).toString();
    }
'''
    command=command.replace('RELEASE_QUALITY_GETTER','getAttributeID'if mc=='1.20.1'else'getAttributeId')
    helpers=storage_item_saves(helpers,mc)
    if not named:
        command=re.sub(r'\bh\(','regressionH(',command);command=re.sub(r'\bcall\(','coreCall(',command)
        helpers=re.sub(r'\bh\(','regressionH(',helpers);helpers=re.sub(r'\bcall\(','coreCall(',helpers)
    text=text.replace(anchor,anchor+command,1)
    return text[:text.rfind('}')]+helpers+'\n}\n'
