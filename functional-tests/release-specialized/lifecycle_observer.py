"""Read actual lifecycle ownership and issue native DeathScreen pointer input."""
from pathlib import Path
import re
from native_item_serialization import storage_item_saves

ROOT=Path(__file__).resolve().parents[2]


def enhance(text,mc,named=False):
    assert mc in ['1.20.1','1.21.1','1.21.11','26.1.2','26.2','26.3']
    source=(ROOT/'versions'/mc/'fabric/src/main/java/com/salts_inventory_update/server/DesktopContainerSessions.java').read_text()
    legacy=mc=='1.20.1'
    fields=['sessions','companions','playerClickSnapshots','sessionWorld','recipeHover','PLAYERS','PENDING_USE_TARGETS']
    fields+=['sourceGrants','sourceGrantTokensByKey','sourceLinks','connectionState','playerMenuToken']if legacy else['sourceIdentities','links','connectionNonce','playerSessionToken']
    assert all(re.search(r'\b'+name+r'\b',source)for name in fields),'Lifecycle schema needs native review'
    anchor='if(kind.equals("regression-world"))return true;';assert text.count(anchor)==1
    command=r'''
        if(kind.equals("regression-lifecycle-state")){
            var actual=(net.minecraft.client.gui.screens.Screen)h("nativeScreen");
            Object desktop=h("screen");
            var client=new LinkedHashMap<String,Object>();client.put("deathScreen",actual instanceof net.minecraft.client.gui.screens.DeathScreen);
            client.put("screen",actual==null?"none":actual.getClass().getName());client.put("sessions",desktop==null?0:((List<?>)field(desktop,"sessions")).size());
            var widgets=new ArrayList<Object>();
            if(actual!=null)for(Object value:actual.children())if(value instanceof net.minecraft.client.gui.components.AbstractWidget widget)
                widgets.add(Map.of("message",widget.getMessage().getString(),"x",widget.getX(),"y",widget.getY(),"width",widget.getWidth(),"height",widget.getHeight(),"active",widget.active,"visible",widget.visible));
            client.put("widgets",widgets);var windows=new ArrayList<Object>();
            if(desktop!=null)for(Object window:(List<?>)field(desktop,"windows"))if(field(window,"session")!=null)
                windows.add(Map.of("session",call(field(window,"session"),"sessionId"),"locked",field(window,"locked"),"bounds",Map.of("x",field(window,"x"),"y",field(window,"y"),"width",field(window,"width"),"height",field(window,"height"))));
            client.put("windows",windows);
            work=server(()->{write("release-lifecycle-state.json",Map.of("client",client,"server",releaseLifecycleState()));return null;});return true;
        }
        if(kind.equals("regression-lifecycle-native-tap")){
            var screen=(net.minecraft.client.gui.screens.Screen)h("nativeScreen");
            if(!(screen instanceof net.minecraft.client.gui.screens.DeathScreen))throw new IllegalStateException("Actual native DeathScreen required");
            double x=c.get("x").getAsDouble(),y=c.get("y").getAsDouble();screen.mouseMoved(x,y);
            LIFECYCLE_NATIVE_TAP
            return true;
        }
'''
    native_tap='screen.mouseClicked(x,y,0);if(h("nativeScreen")==screen)screen.mouseReleased(x,y,0);'
    if mc not in ['1.20.1','1.21.1']:
        native_tap='var event=new net.minecraft.client.input.MouseButtonEvent(x,y,new net.minecraft.client.input.MouseButtonInfo(0,0));screen.mouseClicked(event,false);if(h("nativeScreen")==screen)screen.mouseReleased(event);'
    command=command.replace('LIFECYCLE_NATIVE_TAP',native_tap)
    helpers=r'''
    static java.lang.ref.WeakReference<net.minecraft.server.level.ServerPlayer> releaseLifecycleOriginal;
    static Map<String,Object> releaseLifecycleState(){
        var p=player();if(releaseLifecycleOriginal==null)releaseLifecycleOriginal=new java.lang.ref.WeakReference<>(p);
        Class<?> owner=com.salts_inventory_update.server.DesktopContainerSessions.class;var players=(Map<?,?>)field(owner,"PLAYERS");
        Object state=players.get(LIFECYCLE_PLAYER_KEY);if(state==null)throw new IllegalStateException("Actual negotiated player state absent");
        var result=new LinkedHashMap<String,Object>();result.put("uuid",p.getUUID().toString());result.put("entityId",p.getId());result.put("sameOriginalPlayer",releaseLifecycleOriginal.get()==p);
        result.put("alive",p.isAlive());result.put("dimension",p.level().dimension().toString());result.put("keepInventory",LIFECYCLE_KEEP_INVENTORY);
        result.put("playerStates",players.size());result.put("pendingUseTargets",((Map<?,?>)field(owner,"PENDING_USE_TARGETS")).size());
        result.put("nonce",LIFECYCLE_NONCE);result.put("playerToken",field(state,"LIFECYCLE_PLAYER_TOKEN"));
        result.put("worldMatches",field(state,"sessionWorld")==null||field(state,"sessionWorld")==p.level());
        result.put("snapshots",((Map<?,?>)field(field(state,"playerClickSnapshots"),"snapshots")).size());
        result.put("snapshotLimit",field(ClassForLifecycleSnapshots(),"LIMIT"));result.put("sessionLimit",com.salts_inventory_update.protocol.DesktopProtocol.MAX_DESKTOP_SESSIONS);
        result.put("sourceLimit",com.salts_inventory_update.protocol.DesktopProtocol.MAX_LINK_NODES);
        result.put("graphNodeLimit",com.salts_inventory_update.protocol.DesktopProtocol.MAX_LINK_NODES);result.put("graphEdgeLimit",com.salts_inventory_update.protocol.DesktopProtocol.MAX_LINK_EDGES);
        result.put("sources",((Map<?,?>)field(state,"LIFECYCLE_SOURCES")).size());LIFECYCLE_SOURCE_KEYS
        Object graph=field(state,"LIFECYCLE_GRAPH");result.put("graphNodes",call(graph,"nodeCount"));result.put("graphEdges",call(graph,"edgeCount"));
        result.put("companions",((Map<?,?>)field(state,"companions")).size());result.put("recipeHover",field(state,"recipeHover")!=null);
        var locks=(Map<?,?>)field(com.salts_inventory_update.internal.desktop.DesktopItemSourceLocks.class,"ACTIVE");int lockCount=0;
        for(Object value:locks.values())lockCount+=((Map<?,?>)value).size();result.put("sourceLocks",lockCount);
        result.put("automaticSources",((Map<?,?>)field(com.salts_inventory_update.internal.desktop.AutomaticContainerSources.class,"SOURCES")).size());
        var inventory=new ArrayList<Object>();for(int i=0;i<p.getInventory().getContainerSize();i++)inventory.add(Map.of("index",i,"item",releaseLifecycleStack(p.getInventory().getItem(i))));result.put("inventory",inventory);
        result.put("cursor",releaseLifecycleStack(p.inventoryMenu.getCarried()));var menus=new ArrayList<Object>();
        for(var entry:((Map<?,?>)field(state,"sessions")).entrySet()){
            var menu=(AbstractContainerMenu)field(entry.getValue(),"menu");var slots=new ArrayList<Object>();
            for(int i=0;i<menu.slots.size();i++){
                var slot=menu.getSlot(i);if(slot.container==p.getInventory())continue;
                slots.add(Map.of("index",i,"projection",slot instanceof net.minecraft.world.inventory.ResultSlot||com.salts_inventory_update.api.desktop.DesktopMenuBindings.isVirtualSlot(menu,slot),"item",releaseLifecycleStack(slot.getItem())));
            }
            menus.add(Map.of("session",entry.getKey(),"token",field(entry.getValue(),"sessionToken"),"crafting",menu instanceof net.minecraft.world.inventory.CraftingMenu,"loom",menu instanceof net.minecraft.world.inventory.LoomMenu,"type",menu.getClass().getName(),"slots",slots,"cursor",releaseLifecycleStack(menu.getCarried()),"valid",menu.stillValid(p)));
        }
        result.put("menus",menus);var drops=new ArrayList<Object>();
        for(var world:p.level().getServer().getAllLevels())for(var entity:world.getAllEntities())if(entity instanceof net.minecraft.world.entity.item.ItemEntity item)
            drops.add(Map.of("dimension",world.dimension().toString(),"uuid",item.getUUID().toString(),"item",releaseLifecycleStack(item.getItem())));
        result.put("drops",drops);return result;
    }
    static Class<?> ClassForLifecycleSnapshots(){
        try{return Class.forName("com.salts_inventory_update.server.PlayerMenuClickSnapshots");}
        catch(ClassNotFoundException e){throw new IllegalStateException("Actual snapshot history absent",e);}
    }
    static String releaseLifecycleStack(ItemStack value){
        if(value.isEmpty())return "{}";
        var ops=net.minecraft.resources.RegistryOps.create(net.minecraft.nbt.NbtOps.INSTANCE,player().registryAccess());
        return ItemStack.CODEC.encodeStart(ops,value).result().orElseThrow(()->new IllegalStateException("Native lifecycle item codec refused serialization")).toString();
    }
'''
    replacements={
        'LIFECYCLE_PLAYER_KEY':'p.connection'if legacy else'p',
        'LIFECYCLE_KEEP_INVENTORY':'p.level().getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_KEEPINVENTORY)'if mc in ['1.20.1','1.21.1']else'Boolean.TRUE.equals(p.level().getGameRules().get(net.minecraft.world.level.gamerules.GameRules.KEEP_INVENTORY))',
        'LIFECYCLE_NONCE':'call(field(state,"connectionState"),"connectionNonce")'if legacy else'field(state,"connectionNonce")',
        'LIFECYCLE_PLAYER_TOKEN':'playerMenuToken'if legacy else'playerSessionToken',
        'LIFECYCLE_SOURCES':'sourceGrants'if legacy else'sourceIdentities',
        'LIFECYCLE_SOURCE_KEYS':'result.put("sourceKeys",((Map<?,?>)field(state,"sourceGrantTokensByKey")).size());'if legacy else'result.put("sourceKeys",((Map<?,?>)field(state,"sourceIdentities")).size());',
        'LIFECYCLE_GRAPH':'sourceLinks'if legacy else'links',
    }
    for key,value in replacements.items():assert key in helpers;helpers=helpers.replace(key,value)
    helpers=storage_item_saves(helpers,mc)
    if not named:
        command=re.sub(r'\bh\(','regressionH(',command);command=re.sub(r'\bcall\(','coreCall(',command);helpers=re.sub(r'\bcall\(','coreCall(',helpers)
    text=text.replace(anchor,anchor+command,1);return text[:text.rfind('}')]+helpers+'\n}\n'
