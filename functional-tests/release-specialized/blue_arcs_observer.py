"""Read actual Blue Skies four-cell player capability and original hosted screen."""
import re


def enhance(text,mc,named=False):
    assert mc=='1.20.1'and not named
    anchor='if(kind.equals("regression-world"))return true;';assert text.count(anchor)==1
    command=r'''
        if(kind.equals("regression-blue-arcs-state")){
            var clients=new ArrayList<Object>();Object desktop=h("screen");if(desktop==null)throw new IllegalStateException("No Arcs desktop");
            for(Object window:(List<?>)field(desktop,"windows")){
                if(field(window,"session")==null||field(window,"apiDefinition")==null)continue;
                Object definition=field(window,"apiDefinition");if(!definition.getClass().getName().endsWith(".NativeTemplateWindow"))continue;
                Object screen=field(field(definition,"initial"),"screen");if(!screen.getClass().getName().equals("com.legacy.blue_skies.client.gui.screen.ArcsScreen"))continue;
                clients.add(Map.of("session",call(field(window,"session"),"sessionId"),"screen",screen.getClass().getName(),"menu",call(screen,"getMenu").getClass().getName(),"locked",field(window,"locked"),"bounds",Map.of("x",field(window,"x"),"y",field(window,"y"),"width",field(window,"width"),"height",field(window,"height"))));
            }
            if(clients.size()!=1)throw new IllegalStateException("Expected one actual original Arcs screen");
            work=server(()->{
                net.minecraft.world.Container inventory=releaseBlueArcInventory();var items=new ArrayList<Object>();for(int i=0;i<inventory.getContainerSize();i++)items.add(regressionStack(inventory.getItem(i)));
                Object state=call(com.salts_inventory_update.server.DesktopContainerSessions.class,"sessions",player());var servers=new ArrayList<Object>();
                for(var entry:((Map<?,?>)field(state,"sessions")).entrySet()){
                    var menu=(AbstractContainerMenu)field(entry.getValue(),"menu");if(!menu.getClass().getName().equals("com.legacy.blue_skies.client.gui.menu.ArcsMenu"))continue;
                    var slots=new ArrayList<Object>();for(int i=0;i<menu.slots.size();i++){
                        Slot slot=menu.slots.get(i);if(slot.getClass().getName().equals("com.legacy.blue_skies.client.gui.menu.ArcSlot"))slots.add(Map.of("index",i,"containerIndex",slot.getContainerSlot(),"sameCapability",slot.container==inventory,"item",regressionStack(slot.getItem())));
                    }
                    servers.add(Map.of("session",entry.getKey(),"menu",menu.getClass().getName(),"slots",slots));
                }
                write("release-blue-arcs-state.json",Map.of("client",clients,"server",servers,"capability",items));return null;
            });return true;
        }
'''
    helpers=r'''
    static net.minecraft.world.Container releaseBlueArcInventory(){
        try{
            Object capability=Class.forName("com.legacy.blue_skies.capability.SkiesPlayer").getField("INSTANCE").get(null);
            Object optional=call(call(player(),"getCapability",capability),"resolve");Object nativePlayer=((java.util.Optional<?>)optional).orElseThrow(()->new IllegalStateException("Actual Blue Skies player capability absent"));
            return(net.minecraft.world.Container)call(nativePlayer,"getArcInventory");
        }catch(ReflectiveOperationException error){throw new IllegalStateException("Cannot read native Arcs capability",error);}
    }
'''
    command=re.sub(r'\bh\(','regressionH(',command);command=re.sub(r'\bcall\(','coreCall(',command);helpers=re.sub(r'\bcall\(','coreCall(',helpers)
    text=text.replace(anchor,anchor+command,1);return text[:text.rfind('}')]+helpers+'\n}\n'
