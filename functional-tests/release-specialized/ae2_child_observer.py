"""Observe original AE2 child-screen widgets and native server/menu values.

This helper never creates a local view, plan or status, invokes an action
handler, or changes a menu. Actual pointer/key input belongs to the feature
driver; existing full-item/cursor ledgers remain separate observations.
"""
import re


def enhance(text,mc,named=False):
    anchor='if(kind.equals("regression-world"))return true;';assert text.count(anchor)==1
    command=r'''
        if(kind.equals("regression-ae2-child-state")){
            var clients=new ArrayList<Object>();Object desktop=h("screen");
            if(desktop==null)throw new IllegalStateException("No AE2 desktop");
            for(Object window:(List<?>)field(desktop,"windows")){
                if(field(window,"session")==null||field(window,"apiState")==null)continue;
                Object state=field(window,"apiState"),content;
                try{content=field(state,"content");}catch(IllegalArgumentException otherAdapter){continue;}
                if(content==null||!content.getClass().getName().startsWith("appeng.client.gui."))continue;
                var widgets=new ArrayList<Object>();
                for(Object child:(Iterable<?>)call(content,"children")){
                    if(!(child instanceof net.minecraft.client.gui.components.AbstractWidget widget))continue;
                    var data=new LinkedHashMap<String,Object>();
                    data.put("type",child.getClass().getName());data.put("message",widget.getMessage().getString());
                    data.put("x",widget.getX());data.put("y",widget.getY());data.put("width",widget.getWidth());data.put("height",widget.getHeight());
                    data.put("visible",widget.visible);data.put("active",widget.active);
                    if(child instanceof net.minecraft.client.gui.components.EditBox edit)data.put("value",edit.getValue());
                    for(Class<?> type=child.getClass();type!=null;type=type.getSuperclass()){
                        if(type.getName().equals("appeng.client.gui.widgets.TabButton")){
                            data.put("icon",String.valueOf(field(child,"icon")));var tooltips=new ArrayList<String>();
                            for(Object line:(Iterable<?>)call(child,"getTooltipMessage"))tooltips.add(((net.minecraft.network.chat.Component)line).getString());
                            data.put("tooltips",tooltips);break;
                        }
                        if(!type.getName().equals("appeng.client.gui.widgets.IconButton"))continue;
                        data.put("icon",call(child,"getIcon").toString());var tooltips=new ArrayList<String>();
                        for(Object line:(Iterable<?>)call(child,"getTooltipMessage"))tooltips.add(((net.minecraft.network.chat.Component)line).getString());
                        data.put("tooltips",tooltips);break;
                    }
                    if(child.getClass().getName().equals("appeng.client.gui.widgets.SettingToggleButton"))
                        data.put("currentValue",call(child,"getCurrentValue").toString());
                    widgets.add(data);
                }
                Object menu=call(content,"getMenu");var row=new LinkedHashMap<String,Object>();
                row.put("session",call(field(window,"session"),"sessionId"));row.put("screen",content.getClass().getName());
                row.put("bounds",Map.of("x",field(window,"x"),"y",field(window,"y"),"width",field(window,"width"),"height",field(window,"height")));
                row.put("locked",field(window,"locked"));
                row.put("menu",menu.getClass().getName());row.put("values",releaseAeChildValues(menu));row.put("widgets",widgets);clients.add(row);
            }
            if(clients.isEmpty())throw new IllegalStateException("No original AE2 screen observed");
            work=server(()->{
                Object state=call(com.salts_inventory_update.server.DesktopContainerSessions.class,"sessions",player());
                var servers=new ArrayList<Object>();
                for(var entry:((Map<?,?>)field(state,"sessions")).entrySet()){
                    Object menu=field(entry.getValue(),"menu");if(!menu.getClass().getName().startsWith("appeng.menu."))continue;
                    servers.add(Map.of("session",entry.getKey(),"menu",menu.getClass().getName(),"values",releaseAeChildValues(menu)));
                }
                write("release-ae2-child-state.json",Map.of("client",clients,"server",servers));return null;
            });return true;
        }
'''
    helpers=r'''
    static Map<String,Object> releaseAeChildValues(Object menu){
        var values=new LinkedHashMap<String,Object>();String name=menu.getClass().getSimpleName();
        List<String> getters=switch(name){
            case "PriorityMenu"->List.of("getPriorityValue");
            case "SpatialAnchorMenu"->List.of("getOverlayMode");
            case "EnergyLevelEmitterMenu"->List.of("getReportingValue","getRedStoneMode");
            case "SetStockAmountMenu"->List.of("getInitialAmount","getMaxAmount","getWhatToStock");
            case "CraftAmountMenu"->List.of("getWhatToCraft");
            case "CraftConfirmMenu"->List.of("hasNoCPU","getCpuAvailableBytes","getCpuCoProcessors","getName","isAutoStart");
            default->List.of();
        };
        for(String getter:getters){
            Object value=call(menu,getter);var observed=new LinkedHashMap<String,Object>();
            observed.put("javaType",value==null?"null":value.getClass().getName());
            observed.put("value",value==null?null:value instanceof Number||value instanceof Boolean?value:value.toString());
            values.put(getter,observed);
        }
        if(name.equals("PriorityMenu")){
            Object host=call(menu,"getHost");Object priority=call(host,"getPriority");
            values.put("nativeHostPriority",Map.of("javaType",priority.getClass().getName(),"value",priority));
        }
        if(name.equals("SpatialAnchorMenu"))for(String key:List.of("powerConsumption","loadedChunks","allLoadedWorlds","allLoadedChunks","allWorlds","allChunks")){
            Object value=field(menu,key);values.put(key,Map.of("javaType",value.getClass().getName(),"value",value));
        }
        return values;
    }
'''
    if not named:
        command=re.sub(r'\bh\(','regressionH(',command)
        command=re.sub(r'\bcall\(','coreCall(',command);helpers=re.sub(r'\bcall\(','coreCall(',helpers)
    text=text.replace(anchor,anchor+command,1)
    return text[:text.rfind('}')]+helpers+'\n}\n'
