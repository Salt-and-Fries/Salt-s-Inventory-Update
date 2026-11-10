"""Observe original gem ownership, projected sockets and native tool properties.

No RNG, recipe, menu callback, item result or configuration is changed here.
"""
import re
from native_item_serialization import storage_item_saves


def enhance(text,mc,named=False):
    anchor='if(kind.equals("regression-world"))return true;';assert text.count(anchor)==1
    command=r'''
        if(kind.equals("regression-smitherz-state")){
            Object window=releaseSmitherWindow(),screen=field(field(window,"apiState"),"screen");var nativeMenu=(AbstractContainerMenu)call(screen,"getMenu");
            var client=new LinkedHashMap<String,Object>();client.put("menu",nativeMenu.getClass().getName());client.put("screen",screen.getClass().getName());
            client.put("session",call(field(window,"session"),"sessionId"));client.put("locked",field(window,"locked"));
            client.put("bounds",Map.of("x",field(window,"x"),"y",field(window,"y"),"width",field(window,"width"),"height",field(window,"height")));
            var buttons=new ArrayList<Object>();for(Object widget:(List<?>)call(screen,"children"))if(widget.getClass().getSimpleName().equals("SmitherButton"))
                buttons.add(Map.of("type",widget.getClass().getName(),"message",((net.minecraft.network.chat.Component)call(widget,"getMessage")).getString(),"disabled",field(widget,"disabled"),"active",field(widget,"active"),"visible",field(widget,"visible")));
            client.put("buttons",buttons);
            work=server(()->{
                var menus=new ArrayList<AbstractContainerMenu>();menus.add(player().containerMenu);
                com.salts_inventory_update.api.server.desktop.DesktopSessions.hasOpenSessionMatching(player(),m->{if(!menus.contains(m))menus.add(m);return false;});
                var found=menus.stream().filter(m->releaseSmitherMenu(m)).toList();if(found.size()!=1)throw new IllegalStateException("Expected one original SmitherZ menu");var menu=found.get(0);
                var cells=new ArrayList<Object>();for(int i=0;i<menu.slots.size();i++)if(menu.getSlot(i).container!=player().getInventory()){
                    Slot slot=menu.getSlot(i);boolean gem=slot.getClass().getName().equals("net.smitherz.screen.widget.GemSlot");
                    var cell=new LinkedHashMap<String,Object>();cell.put("index",i);cell.put("item",releaseSmitherStack(slot.getItem()));cell.put("active",slot.isActive());cell.put("gemSlot",gem);cell.put("linked",gem&&Boolean.TRUE.equals(call(slot,"isLinked")));cells.add(cell);
                }
                var inventory=new ArrayList<Object>();for(int i=0;i<player().getInventory().getContainerSize();i++)inventory.add(Map.of("index",i,"item",releaseSmitherStack(player().getInventory().getItem(i))));
                var cursors=new ArrayList<Object>();for(var m:menus)cursors.add(Map.of("menu",m.getClass().getName(),"item",releaseSmitherStack(m.getCarried())));
                ItemStack tool=menu.getSlot(1).getItem();var details=new LinkedHashMap<String,Object>();
                details.put("client",client);details.put("cells",cells);details.put("inventory",inventory);details.put("cursors",cursors);
                details.put("gems",releaseSmitherGems(menu.getSlot(0).getItem()));
                details.put("tool",Map.of("damageable",tool.isDamageableItem(),"damage",tool.getDamageValue(),"maxDamage",tool.getMaxDamage()));
                var pending=new ArrayList<String>();if(menu.getClass().getName().endsWith(".GrinderScreenHandler"))for(Object value:(List<?>)field(menu,"unlinkedItemStacks"))pending.add(releaseSmitherStack((ItemStack)value));
                details.put("pendingReturnedGems",pending);write("release-smitherz-state.json",details);return null;
            });return true;
        }
'''
    helpers=r'''
    static boolean releaseSmitherMenu(Object menu){String name=menu.getClass().getName();return name.equals("net.smitherz.screen.SmitherScreenHandler")||name.equals("net.smitherz.screen.GrinderScreenHandler");}
    static Object releaseSmitherWindow(){
        var found=new ArrayList<Object>();Object desktop=h("screen");if(desktop==null)throw new IllegalStateException("No desktop");
        for(Object w:(List<?>)field(desktop,"windows")){Object s=field(w,"session");if(s!=null&&releaseSmitherMenu(call(s,"menu"))&&field(w,"apiState")!=null)found.add(w);}
        if(found.size()!=1)throw new IllegalStateException("Expected one original SmitherZ window: "+found.size());return found.get(0);
    }
    static java.util.List<String> releaseSmitherGems(ItemStack equipment){
        var result=new ArrayList<String>();if(equipment.isEmpty())return result;
        try{for(Object gem:((java.util.stream.Stream<?>)call(Class.forName("net.smitherz.util.UpgradeHelper"),"getGemStacks",equipment)).toList())result.add(releaseSmitherStack((ItemStack)gem));}
        catch(ClassNotFoundException e){throw new IllegalStateException(e);}return result;
    }
    static String releaseSmitherStack(ItemStack value){
        if(value.isEmpty())return "{}";var ops=net.minecraft.resources.RegistryOps.create(net.minecraft.nbt.NbtOps.INSTANCE,player().registryAccess());
        return ItemStack.CODEC.encodeStart(ops,value).result().orElseThrow(()->new IllegalStateException("SmitherZ stack codec refused serialization")).toString();
    }
'''
    helpers=storage_item_saves(helpers,mc)
    if not named:
        command=re.sub(r'\bh\(','regressionH(',command);command=re.sub(r'\bcall\(','coreCall(',command)
        helpers=re.sub(r'\bh\(','regressionH(',helpers);helpers=re.sub(r'\bcall\(','coreCall(',helpers)
    text=text.replace(anchor,anchor+command,1)
    return text[:text.rfind('}')]+helpers+'\n}\n'
