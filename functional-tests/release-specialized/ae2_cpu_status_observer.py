"""Observe real CPU selection and delegate wheel input to the desktop screen."""
from ae2_child_observer import enhance as child_observations


def enhance(text,mc,named=False):
    assert mc=='1.21.1'and named,'This fixture is reviewed only for1.21.1NeoForge'
    text=child_observations(text,mc,named)
    anchor='if(kind.equals("regression-world"))return true;';assert text.count(anchor)==1
    command=r'''
        if(kind.equals("regression-ae2-cpu-wheel")){
            int direction=num(c,"direction",0);
            if(direction!=1&&direction!=-1)throw new IllegalArgumentException("Expected one wheel step");
            Object desktop=h("screen"),window=h("windowSession",num(c,"session",-1));
            if(window==null)throw new IllegalStateException("CPU status window missing");
            Object content=field(field(window,"apiState"),"content");
            if(!content.getClass().getName().equals("appeng.client.gui.me.crafting.CraftingStatusScreen"))
                throw new IllegalStateException("Not the native CPU status screen");
            Object bounds=call(content,"cpuSelectionBounds");
            double x=((Number)call(content,"getGuiLeft")).doubleValue()+((Number)call(bounds,"getX")).doubleValue()+10;
            double y=((Number)call(content,"getGuiTop")).doubleValue()+((Number)call(bounds,"getY")).doubleValue()+25;
            ((net.minecraft.client.gui.screens.Screen)desktop).mouseScrolled(x,y,0,direction);
            return true;
        }
        if(kind.equals("regression-ae2-cpu-status-state")){
            Object desktop=h("screen");if(desktop==null)throw new IllegalStateException("No CPU status desktop");
            var clients=new ArrayList<Object>();
            for(Object window:(List<?>)field(desktop,"windows")){
                if(field(window,"session")==null||field(window,"apiState")==null)continue;
                Object content;try{content=field(field(window,"apiState"),"content");}catch(IllegalArgumentException otherAdapter){continue;}
                if(content==null||!content.getClass().getName().equals("appeng.client.gui.me.crafting.CraftingStatusScreen"))continue;
                Object menu=call(content,"getMenu"),rect=call(content,"cpuSelectionBounds");
                var row=new LinkedHashMap<String,Object>(releaseCpuStatusMenu(menu));
                row.put("session",call(field(window,"session"),"sessionId"));row.put("screen",content.getClass().getName());
                row.put("left",call(content,"getGuiLeft"));row.put("top",call(content,"getGuiTop"));
                row.put("selection",Map.of("x",call(rect,"getX"),"y",call(rect,"getY"),"width",call(rect,"getWidth"),"height",call(rect,"getHeight")));
                row.put("visibleRows",call(content,"cpuVisibleRows"));row.put("scroll",call(field(content,"cpuScrollbar"),"getCurrentScroll"));
                row.put("narrow",net.minecraft.client.Minecraft.getInstance().getWindow().getGuiScaledWidth()<400);
                clients.add(row);
            }
            if(clients.size()!=1)throw new IllegalStateException("Expected one actual CPU status screen");
            work=server(()->{
                var servers=new ArrayList<Object>();
                Object state=call(com.salts_inventory_update.server.DesktopContainerSessions.class,"sessions",player());
                for(var entry:((Map<?,?>)field(state,"sessions")).entrySet()){
                    Object menu=field(entry.getValue(),"menu");
                    if(menu.getClass().getName().equals("appeng.menu.me.crafting.CraftingStatusMenu")){
                        var row=new LinkedHashMap<String,Object>(releaseCpuStatusMenu(menu));
                        row.put("session",entry.getKey());servers.add(row);
                    }
                }
                write("release-ae2-cpu-status-state.json",Map.of("client",clients,"server",servers));return null;
            });return true;
        }
'''
    helpers=r'''
    static Map<String,Object> releaseCpuStatusMenu(Object menu){
        var cpus=new ArrayList<Object>();
        for(Object cpu:(Iterable<?>)call(field(menu,"cpuList"),"cpus"))cpus.add(call(cpu,"serial"));
        return Map.of("menu",menu.getClass().getName(),"selected",call(menu,"getSelectedCpuSerial"),"cpus",cpus,
            "ready",field(menu,"cpuStatusReady"),"available",field(menu,"cpuAvailable"),"active",field(menu,"hasActiveJob"));
    }
'''
    text=text.replace(anchor,anchor+command,1)
    return text[:text.rfind('}')]+helpers+'\n}\n'
