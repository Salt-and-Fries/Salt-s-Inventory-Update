"""Read actual packet-delivered AE2 status and the backing grid independently.

Only public native snapshot/getter calls and field reads occur here. In
particular the client status, menu grid, native config and export result are
never assigned or manufactured by this observer.
"""
import re
from ae2_child_observer import enhance as child_observations


def enhance(text,mc,named=False):
    text=child_observations(text,mc,named)
    anchor='if(kind.equals("regression-world"))return true;';assert text.count(anchor)==1
    command=r'''
        if(kind.equals("regression-ae2-network-state")){
            Object desktop=h("screen");if(desktop==null)throw new IllegalStateException("No original desktop");
            var clients=new ArrayList<Object>();Class<?> statusType=null;
            for(Object window:(List<?>)field(desktop,"windows")){
                if(field(window,"session")==null||field(window,"apiState")==null)continue;
                Object content;try{content=field(field(window,"apiState"),"content");}catch(IllegalArgumentException otherAdapter){continue;}
                if(content==null||!content.getClass().getName().equals("appeng.client.gui.me.networktool.NetworkStatusScreen"))continue;
                Object menu=call(content,"getMenu"),status=field(content,"status");statusType=status.getClass();
                var row=new LinkedHashMap<String,Object>();row.put("session",call(field(window,"session"),"sessionId"));
                row.put("menu",menu.getClass().getName());row.put("screen",content.getClass().getName());row.put("status",releaseAeNetworkValues(status));
                row.put("scroll",call(field(content,"scrollbar"),"getCurrentScroll"));
                try{Class<?> configType=content.getClass().getClassLoader().loadClass("appeng.core.AEConfig");
                    row.put("selectedEnergyUnit",call(call(configType,"instance"),"getSelectedEnergyUnit").toString());
                }catch(ClassNotFoundException error){throw new IllegalStateException(error);}
                OPTIONAL_EXPORT
                clients.add(row);
            }
            if(clients.isEmpty()||statusType==null)throw new IllegalStateException("No actual network status");
            final Class<?> actualStatusType=statusType;
            work=server(()->{
                Object state=call(com.salts_inventory_update.server.DesktopContainerSessions.class,"sessions",player());
                var servers=new ArrayList<Object>();
                for(var entry:((Map<?,?>)field(state,"sessions")).entrySet()){
                    Object menu=field(entry.getValue(),"menu");if(!menu.getClass().getName().equals("appeng.menu.me.networktool.NetworkStatusMenu"))continue;
                    Object grid=field(menu,"grid");if(grid==null)throw new IllegalStateException("Native status menu has no grid");
                    var row=new LinkedHashMap<String,Object>();row.put("session",entry.getKey());row.put("menu",menu.getClass().getName());
                    row.put("status",releaseAeNetworkValues(call(actualStatusType,"fromGrid",grid)));
                    OPTIONAL_METADATA
                    servers.add(row);
                }
                write("release-ae2-network-state.json",Map.of("client",clients,"server",servers));return null;
            });return true;
        }
'''
    helpers=r'''
    static Map<String,Object> releaseAeNetworkValues(Object status){
        var values=new LinkedHashMap<String,Object>();
        for(String getter:List.of("getAveragePowerInjection","getAveragePowerUsage","getStoredPower","getMaxStoredPower","getChannelPower","getChannelsUsed")){
            Object value=call(status,getter);values.put(getter,Map.of("javaType",value.getClass().getName(),"value",value));
        }
        var groups=new ArrayList<Object>();
        for(Object group:(Iterable<?>)call(status,"getGroupedMachines")){
            Object display=call(group,"getDisplay");var row=new LinkedHashMap<String,Object>();
            row.put("display",regressionStack((ItemStack)call(display,"toStack")));row.put("item",call(display,"getId").toString());
            for(String getter:List.of("getCount","isMissingChannel","getIdlePowerUsage","getPowerGenerationCapacity")){
                Object value=call(group,getter);row.put(getter,Map.of("javaType",value.getClass().getName(),"value",value));
            }
            groups.add(row);
        }
        values.put("groups",groups);return values;
    }
'''
    command=command.replace('OPTIONAL_EXPORT','row.put("canExportGrid",call(menu,"canExportGrid"));'if mc!='1.20.1'else'row.put("exportUnavailableOnPinnedRelease",true);')
    command=command.replace('OPTIONAL_METADATA','row.put("gridSerial",call(grid,"getSerialNumber"));row.put("serverDirectory",call(call(h("serverLevel"),"getServer"),"getServerDirectory").toString());'if mc!='1.20.1'else'')
    if not named:
        command=re.sub(r'\bh\(','regressionH(',command)
        command=re.sub(r'\bcall\(','coreCall(',command);helpers=re.sub(r'\bcall\(','coreCall(',helpers)
    text=text.replace(anchor,anchor+command,1)
    return text[:text.rfind('}')]+helpers+'\n}\n'
