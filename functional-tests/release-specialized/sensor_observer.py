"""Read native sensor options and real input geometry; never write its settings."""
import re
from native_item_serialization import storage_item_saves


def enhance(text,mc,named=False):
    anchor='if(kind.equals("regression-world"))return true;';assert text.count(anchor)==1
    command=r'''
        if(kind.equals("regression-sensor-state")){
            Object window=releaseSensorWindow(),definition=field(window,"apiDefinition"),screen=field(field(definition,"initial"),"screen");
            Object nativeMenu=call(screen,"getMenu"),entity=call(nativeMenu,"getEntity"),list=field(screen,"list"),checkbox=field(screen,"invertedCheckBox");
            int left=((Number)field(screen,"leftPos")).intValue(),top=((Number)field(screen,"topPos")).intValue();
            var choices=new ArrayList<Object>();int index=0;for(Object entry:(List<?>)call(list,"children")){
                double y=((Number)call(list,"getRowTop",index)).doubleValue(),bottom=((Number)call(list,"getRowBottom",index)).doubleValue();
                choices.add(Map.of("index",index++,"type",call(call(entry,"getType"),"getName").toString(),
                    "source_x",((Number)call(list,"getRowLeft")).doubleValue()+16-left,"source_y",(y+bottom)/2-top));
            }
            var client=new LinkedHashMap<String,Object>();client.put("menu",nativeMenu.getClass().getName());client.put("screen",screen.getClass().getName());client.put("choices",choices);
            client.put("inverted",call(entity,"isInverted"));client.put("sensingType",call(call(entity,"getSensingType"),"getName").toString());
            client.put("checkbox",Map.of("type",checkbox.getClass().getName(),"message",((net.minecraft.network.chat.Component)call(checkbox,"getMessage")).getString(),
                "active",field(checkbox,"active"),"visible",field(checkbox,"visible")));
            client.put("session",call(field(window,"session"),"sessionId"));client.put("locked",field(window,"locked"));
            client.put("bounds",Map.of("x",field(window,"x"),"y",field(window,"y"),"width",field(window,"width"),"height",field(window,"height")));
            work=server(()->{
                var menus=new ArrayList<AbstractContainerMenu>();menus.add(player().containerMenu);
                com.salts_inventory_update.api.server.desktop.DesktopSessions.hasOpenSessionMatching(player(),m->{if(!menus.contains(m))menus.add(m);return false;});
                var sensors=menus.stream().filter(m->m.getClass().getName().equals("ad_astra_giselle_addon.common.menu.RocketSensorMenu")).toList();
                if(sensors.size()!=1)throw new IllegalStateException("Expected one original sensor menu");Object owner=call(sensors.get(0),"getEntity");
                var inventory=new ArrayList<Object>();for(int i=0;i<player().getInventory().getContainerSize();i++)inventory.add(Map.of("index",i,"item",releaseSensorStack(player().getInventory().getItem(i))));
                var cursors=new ArrayList<Object>();for(var m:menus)cursors.add(Map.of("menu",m.getClass().getName(),"item",releaseSensorStack(m.getCarried())));
                write("release-sensor-state.json",Map.of("client",client,"inverted",call(owner,"isInverted"),"sensingType",call(call(owner,"getSensingType"),"getName").toString(),
                    "signal",call(owner,"getAnalogSignal"),"inventory",inventory,"cursors",cursors));return null;
            });return true;
        }
'''
    helpers=r'''
    static Object releaseSensorWindow(){
        var found=new ArrayList<Object>();Object desktop=h("screen");if(desktop==null)throw new IllegalStateException("No desktop");
        for(Object w:(List<?>)field(desktop,"windows")){Object s=field(w,"session");if(s!=null&&call(s,"menu").getClass().getName().equals("ad_astra_giselle_addon.common.menu.RocketSensorMenu")&&field(w,"apiDefinition")!=null)found.add(w);}
        if(found.size()!=1)throw new IllegalStateException("Expected one sensor extraction, found "+found.size());return found.get(0);
    }
    static String releaseSensorStack(ItemStack value){
        if(value.isEmpty())return "{}";var ops=net.minecraft.resources.RegistryOps.create(net.minecraft.nbt.NbtOps.INSTANCE,player().registryAccess());
        return ItemStack.CODEC.encodeStart(ops,value).result().orElseThrow(()->new IllegalStateException("Native sensor item codec refused serialization")).toString();
    }
'''
    helpers=storage_item_saves(helpers,mc)
    if not named:
        command=re.sub(r'\bh\(','regressionH(',command);command=re.sub(r'\bcall\(','coreCall(',command)
        helpers=re.sub(r'\bh\(','regressionH(',helpers);helpers=re.sub(r'\bcall\(','coreCall(',helpers)
    text=text.replace(anchor,anchor+command,1)
    return text[:text.rfind('}')]+helpers+'\n}\n'
