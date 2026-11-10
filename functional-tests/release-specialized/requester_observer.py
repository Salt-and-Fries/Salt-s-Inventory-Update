"""Read original Requester controls, references, trackers and world buffers only."""
import re
from ae2_crafting_observer import enhance as crafting_observations


def enhance(text,mc,named=False):
    text=crafting_observations(text,mc,named)
    anchor='if(kind.equals("regression-world"))return true;';assert text.count(anchor)==1
    command=r'''
        if(kind.equals("regression-requester-state")){
            Object desktop=h("screen");if(desktop==null)throw new IllegalStateException("No original Requester desktop");var clients=new ArrayList<Object>();
            for(Object window:(List<?>)field(desktop,"windows")){
                if(field(window,"session")==null||field(window,"apiState")==null)continue;
                Object content;try{content=field(field(window,"apiState"),"content");}catch(IllegalArgumentException otherAdapter){continue;}
                if(content==null||!List.of("com.almostreliable.merequester.client.RequesterScreen","com.almostreliable.merequester.client.RequesterTerminalScreen").contains(content.getClass().getName()))continue;
                var menu=(AbstractContainerMenu)call(content,"getMenu");boolean terminal=content.getClass().getSimpleName().equals("RequesterTerminalScreen");
                var row=new LinkedHashMap<String,Object>();row.put("session",call(field(window,"session"),"sessionId"));row.put("screen",content.getClass().getName());row.put("menu",menu.getClass().getName());
                row.put("bounds",Map.of("x",field(window,"x"),"y",field(window,"y"),"width",field(window,"width"),"height",field(window,"height")));row.put("locked",field(window,"locked"));
                row.put("left",call(content,"getGuiLeft"));row.put("top",call(content,"getGuiTop"));row.put("lines",((List<?>)field(content,"lines")).size());
                var references=new ArrayList<Object>();
                if(terminal){
                    for(Object reference:((Map<?,?>)field(content,"byId")).values())references.add(releaseRequesterReference(reference));
                    row.put("search",call(field(content,"searchField"),"getValue"));
                    row.put("matches",((List<?>)field(content,"requesterNames")).size());
                }else{Object reference=field(content,"requesterReference");if(reference==null)throw new IllegalStateException("Original Requester reference not synchronized");references.add(releaseRequesterReference(reference));}
                row.put("references",references);var rows=new ArrayList<Object>();
                for(Object widget:(List<?>)field(content,"requestWidgets")){
                    int index=((Number)field(widget,"index")).intValue();Object request=call(content,"getTargetRequest",index);var controls=new LinkedHashMap<String,Object>();
                    for(String name:List.of("stateBox","amountField","batchField","submitButton","statusDisplay"))controls.put(name,releaseRequesterWidget(field(widget,name)));
                    rows.add(Map.of("displayIndex",index,"request",releaseRequesterRequest(request),"controls",controls));
                }
                row.put("rows",rows);var slots=new ArrayList<Object>();
                for(Object value:(Iterable<?>)call(content,"desktopSlots")){
                    var slot=(net.minecraft.world.inventory.Slot)value;if(!slot.getClass().getName().equals("com.almostreliable.merequester.client.RequestSlot"))continue;
                    Object reference=field(slot,"requesterReference");
                    slots.add(Map.of("index",field(slot,"slot"),"containerIndex",slot.getContainerSlot(),"requesterId",call(reference,"getRequesterId"),"x",slot.x,"y",slot.y,
                        "visible",call(content,"desktopSlotVisible",slot),"item",regressionStack(slot.getItem()),"type",slot.getClass().getName()));
                }
                row.put("slots",slots);var widgets=new ArrayList<Object>();for(Object child:(Iterable<?>)call(content,"children"))if(child instanceof net.minecraft.client.gui.components.AbstractWidget)widgets.add(releaseRequesterWidget(child));row.put("widgets",widgets);clients.add(row);
            }
            if(clients.isEmpty())throw new IllegalStateException("No actual Requester screen observed");
            work=server(()->{
                var level=(net.minecraft.server.level.ServerLevel)h("serverLevel");Object host=level.getBlockEntity(POS.offset(0,1,-1));
                if(host==null||!host.getClass().getName().equals("com.almostreliable.merequester.requester.RequesterBlockEntity"))throw new IllegalStateException("Native Requester block missing");
                Object manager=call(host,"MANAGER_GETTER");var requests=releaseRequesterManager(manager);var status=new ArrayList<Object>();
                Object states=field(host,"requestStatus");for(int i=0;i<java.lang.reflect.Array.getLength(states);i++){
                    Object nativeState=java.lang.reflect.Array.get(states,i),type=call(nativeState,"type");status.add(Map.of("index",i,"type",type.toString(),"clientType",call(type,"translateToClient").toString()));
                }
                var buffers=new ArrayList<Object>();Object storages=field(call(host,"getStorageManager"),"storages");
                for(int i=0;i<java.lang.reflect.Array.getLength(storages);i++){
                    Object storage=java.lang.reflect.Array.get(storages,i);if(storage==null){buffers.add(Map.of("index",i,"empty",true));continue;}
                    var buffer=new LinkedHashMap<String,Object>();buffer.put("index",i);buffer.put("empty",false);Object key=field(storage,"key");buffer.put("key",releaseRequesterKey(key));
                    for(String name:List.of("knownAmount","bufferAmount","pendingAmount","totalAmount")){Object amount=field(storage,name);buffer.put(name,Map.of("value",amount,"javaType",amount.getClass().getName()));}buffers.add(buffer);
                }
                Object state=call(com.salts_inventory_update.server.DesktopContainerSessions.class,"sessions",player());var servers=new ArrayList<Object>();
                for(var entry:((Map<?,?>)field(state,"sessions")).entrySet()){
                    Object menu=field(entry.getValue(),"menu");boolean terminal=menu.getClass().getName().equals("com.almostreliable.merequester.terminal.RequesterTerminalMenu");
                    if(!terminal&&!menu.getClass().getName().equals("com.almostreliable.merequester.requester.RequesterMenu"))continue;
                    var trackers=new ArrayList<Object>();var values=new ArrayList<Object>();
                    if(terminal)values.addAll(((Map<?,?>)field(menu,"byId")).values());else{Object tracker=field(menu,"requestTracker");if(tracker==null)throw new IllegalStateException("Native Requester tracker missing");values.add(tracker);}
                    for(Object tracker:values){Object serverManager=call(tracker,"getServer");trackers.add(Map.of("id",call(tracker,"getId"),"name",call(tracker,"getName"),"sameBackingManager",serverManager==manager,"requests",releaseRequesterManager(serverManager),"cache",releaseRequesterManager(call(tracker,"getClient"))));}
                    servers.add(Map.of("session",entry.getKey(),"menu",menu.getClass().getName(),"trackers",trackers));
                }
                write("release-requester-state.json",Map.of("host",host.getClass().getName(),"requests",requests,"status",status,"buffers",buffers,"client",clients,"server",servers));return null;
            });return true;
        }
'''.replace('MANAGER_GETTER','getRequests'if mc=='1.20.1'else'getRequestManager')
    helpers=r'''
    static Map<String,Object> releaseRequesterKey(Object key){return key==null?Map.of("empty",true):Map.of("empty",false,"item",regressionStack((ItemStack)call(key,"toStack")));}
    static Map<String,Object> releaseRequesterRequest(Object request){
        if(request==null)return Map.of("empty",true);Object amount=call(request,"getAmount"),batch=call(request,"getBatch");
        return Map.of("empty",false,"index",call(request,"getIndex"),"enabled",call(request,"getState"),"key",releaseRequesterKey(call(request,"getKey")),
            "amount",amount,"amountJavaType",amount.getClass().getName(),"batch",batch,"batchJavaType",batch.getClass().getName(),"status",call(request,"getClientStatus").toString());
    }
    static List<Object> releaseRequesterManager(Object manager){
        var result=new ArrayList<Object>();for(int i=0;i<((Number)call(manager,"size")).intValue();i++)result.add(releaseRequesterRequest(call(manager,"get",i)));return result;
    }
    static Map<String,Object> releaseRequesterReference(Object reference){return Map.of("id",call(reference,"getRequesterId"),"name",call(reference,"getDisplayName"),"requests",releaseRequesterManager(call(reference,"MANAGER_GETTER")));}
    static Map<String,Object> releaseRequesterWidget(Object value){
        var widget=(net.minecraft.client.gui.components.AbstractWidget)value;var result=new LinkedHashMap<String,Object>();
        result.put("type",value.getClass().getName());result.put("message",widget.getMessage().getString());result.put("x",widget.getX());result.put("y",widget.getY());result.put("width",widget.getWidth());result.put("height",widget.getHeight());result.put("visible",widget.visible);result.put("active",widget.active);
        if(value instanceof net.minecraft.client.gui.components.EditBox edit)result.put("value",edit.getValue());
        if(value.getClass().getName().equals("com.almostreliable.merequester.client.widgets.NumberField")){
            var amount=(java.util.OptionalLong)call(value,"getLongValue");result.put("numericPresent",amount.isPresent());if(amount.isPresent())result.put("numeric",amount.getAsLong());
        }
        if(value.getClass().getName().equals("com.almostreliable.merequester.client.widgets.StateBox"))result.put("selected",call(value,"isSelected"));
        if(value.getClass().getName().equals("com.almostreliable.merequester.client.widgets.StatusDisplay"))result.put("status",field(value,"status").toString());return result;
    }
'''.replace('MANAGER_GETTER','getRequests'if mc=='1.20.1'else'getRequestManager')
    if named:command=re.sub(r'\bPOS\b','origin()',command)
    else:
        command=re.sub(r'\bh\(','regressionH(',command)
        command=re.sub(r'\bcall\(','coreCall(',command);helpers=re.sub(r'\bcall\(','coreCall(',helpers)
    text=text.replace(anchor,anchor+command,1)
    return text[:text.rfind('}')]+helpers+'\n}\n'
