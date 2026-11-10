"""Read native guard entity flags, exact equipment and actual rendered buttons."""
import re
from native_item_serialization import storage_item_saves


def enhance(text,mc,named=False):
    anchor='if(kind.equals("regression-world"))return true;';assert text.count(anchor)==1
    command=r'''
        if(kind.equals("regression-guard-state")){
            Object window=releaseGuardWindow(),definition=field(window,"apiDefinition"),screen=field(field(definition,"initial"),"screen");
            Object menu=call(screen,"getMenu"),guard=releaseGuardOwner(menu);var buttons=new ArrayList<Object>();
            int ordinal=0;for(Object child:(List<?>)call(screen,"children"))if(child.getClass().getName().endsWith("$GuardGuiButton")){
                buttons.add(Map.of("ordinal",ordinal++,"type",child.getClass().getName(),"message",((net.minecraft.network.chat.Component)call(child,"getMessage")).getString(),
                    "follow",field(child,"isFollowButton"),"active",field(child,"active"),"visible",field(child,"visible")));
            }
            var client=new LinkedHashMap<String,Object>();client.put("buttons",buttons);client.put("following",call(guard,"isFollowing"));client.put("patrolling",call(guard,"isPatrolling"));
            client.put("screen",screen.getClass().getName());client.put("menu",menu.getClass().getName());client.put("session",call(field(window,"session"),"sessionId"));
            client.put("bounds",Map.of("x",field(window,"x"),"y",field(window,"y"),"width",field(window,"width"),"height",field(window,"height")));client.put("locked",field(window,"locked"));
            work=server(()->{
                var menus=new ArrayList<AbstractContainerMenu>();menus.add(player().containerMenu);
                com.salts_inventory_update.api.server.desktop.DesktopSessions.hasOpenSessionMatching(player(),m->{if(!menus.contains(m))menus.add(m);return false;});
                var guards=menus.stream().filter(m->releaseGuardMenu(m.getClass().getName())).toList();
                if(guards.size()!=1)throw new IllegalStateException("Expected one native guard menu");
                var nativeMenu=guards.get(0);Object owner=releaseGuardOwner(nativeMenu);var cells=new ArrayList<Object>();
                for(int i=0;i<nativeMenu.slots.size();i++)if(nativeMenu.getSlot(i).container!=player().getInventory())cells.add(Map.of("index",i,"item",releaseGuardStack(nativeMenu.getSlot(i).getItem())));
                var inventory=new ArrayList<Object>();for(int i=0;i<player().getInventory().getContainerSize();i++)inventory.add(Map.of("index",i,"item",releaseGuardStack(player().getInventory().getItem(i))));
                var cursors=new ArrayList<Object>();for(var m:menus)cursors.add(Map.of("menu",m.getClass().getName(),"item",releaseGuardStack(m.getCarried())));
                write("release-guard-state.json",Map.of("client",client,"following",call(owner,"isFollowing"),"patrolling",call(owner,"isPatrolling"),
                    "patrolPos",String.valueOf(call(owner,"getPatrolPos")),"cells",cells,"inventory",inventory,"cursors",cursors));return null;
            });return true;
        }
'''
    helpers=r'''
    static boolean releaseGuardMenu(String name){return name.equals("dev.sterner.guardvillagers.common.screenhandler.GuardVillagerScreenHandler")||name.equals("tallestegg.guardvillagers.entities.GuardContainer")||name.equals("tallestegg.guardvillagers.common.entities.GuardContainer");}
    static Object releaseGuardOwner(Object menu){return field(menu,menu.getClass().getName().startsWith("dev.sterner.")?"guardEntity":"guard");}
    static Object releaseGuardWindow(){
        var found=new ArrayList<Object>();Object desktop=h("screen");if(desktop==null)throw new IllegalStateException("No desktop");
        for(Object w:(List<?>)field(desktop,"windows")){Object s=field(w,"session");if(s!=null&&releaseGuardMenu(call(s,"menu").getClass().getName())&&field(w,"apiDefinition")!=null)found.add(w);}
        if(found.size()!=1)throw new IllegalStateException("Expected one native guard extraction, found "+found.size());return found.get(0);
    }
    static String releaseGuardStack(ItemStack value){
        if(value.isEmpty())return "{}";var ops=net.minecraft.resources.RegistryOps.create(net.minecraft.nbt.NbtOps.INSTANCE,player().registryAccess());
        return ItemStack.CODEC.encodeStart(ops,value).result().orElseThrow(()->new IllegalStateException("Native guard item codec refused serialization")).toString();
    }
'''
    helpers=storage_item_saves(helpers,mc)
    if not named:
        command=re.sub(r'\bh\(','regressionH(',command);command=re.sub(r'\bcall\(','coreCall(',command)
        helpers=re.sub(r'\bh\(','regressionH(',helpers);helpers=re.sub(r'\bcall\(','coreCall(',helpers)
    text=text.replace(anchor,anchor+command,1)
    return text[:text.rfind('}')]+helpers+'\n}\n'
