"""Observe native tool selection and deliver real extracted-window mouse input.

The publisher owns PlayerWand.player_data and its Tools array. These commands
never assign that data, menu slots, action results, callbacks or providers.
"""
import re
from native_item_serialization import storage_item_saves


def enhance(text, mc, named=False):
    anchor = 'if(kind.equals("regression-world"))return true;'
    assert text.count(anchor) == 1
    commands = r'''
        if(kind.equals("regression-wands-state")){
            Object window=releaseWandsWindow(),desktop=h("screen"),definition=field(window,"apiDefinition");
            Object nativeScreen=field(field(definition,"initial"),"screen"),menu=call(nativeScreen,"getMenu");
            Object clientWand;try{clientWand=field(Class.forName("net.nicguzzo.wands.client.render.ClientRender"),"wand");}
            catch(ClassNotFoundException e){throw new IllegalStateException(e);}
            var client=new LinkedHashMap<String,Object>();client.put("selected",releaseWandsTools(clientWand));
            client.put("menu",menu.getClass().getName());client.put("screen",nativeScreen.getClass().getName());
            client.put("session",call(field(window,"session"),"sessionId"));client.put("locked",field(window,"locked"));
            client.put("bounds",Map.of("x",field(window,"x"),"y",field(window,"y"),"width",field(window,"width"),"height",field(window,"height")));
            work=server(()->{
                Object wand;try{wand=call(Class.forName("net.nicguzzo.wands.wand.PlayerWand"),"get",player());}
                catch(ClassNotFoundException e){throw new IllegalStateException(e);}
                var inventory=new ArrayList<Object>();for(int i=0;i<player().getInventory().getContainerSize();i++)
                    inventory.add(Map.of("index",i,"item",releaseWandsStack(player().getInventory().getItem(i))));
                var menus=new ArrayList<AbstractContainerMenu>();menus.add(player().containerMenu);
                com.salts_inventory_update.api.server.desktop.DesktopSessions.hasOpenSessionMatching(player(),m->{if(!menus.contains(m))menus.add(m);return false;});
                var cursors=new ArrayList<Object>();for(var m:menus)cursors.add(Map.of("menu",m.getClass().getName(),"item",releaseWandsStack(m.getCarried())));
                write("release-wands-state.json",Map.of("client",client,"selected",releaseWandsTools(wand),"inventory",inventory,"cursors",cursors));return null;
            });return true;
        }
        if(kind.equals("regression-wands-tap")){
            Object window=releaseWandsWindow(),desktop=h("screen"),definition=field(window,"apiDefinition");
            Object screen=field(field(definition,"initial"),"screen");
            var menu=(AbstractContainerMenu)call(screen,"getMenu");int index=num(c,"slot",-1);
            if(index<0||index>=36||menu.slots.size()!=36)throw new IllegalArgumentException("Native wand selector cell required");
            Slot cell=menu.getSlot(index);double sx=cell.x+8,sy=cell.y+8;
            Object context=call(desktop,"apiContext",window,null,0,0);
            call(desktop,"bringToFront",window);call(desktop,"setFocusedWindow",window);
            for(Object region:(List<?>)call(call(definition,"layout"),"regions"))if(Boolean.TRUE.equals(call(region,"interactive"))){
                Object source=call(region,"source");double rx=((Number)call(source,"x")).doubleValue(),ry=((Number)call(source,"y")).doubleValue();
                if(sx<rx||sy<ry||sx>=rx+((Number)call(source,"width")).doubleValue()||sy>=ry+((Number)call(source,"height")).doubleValue())continue;
                double x=((Number)call(definition,"regionX",context,region)).doubleValue()+sx-rx,y=((Number)call(definition,"regionY",context,region)).doubleValue()+sy-ry;
                int b=regressionButton(c);h("mouse","mouseClicked",x,y,b,0D,0D);h("mouse","mouseReleased",x,y,b,0D,0D);
                write("release-wands-point.json",Map.of("x",x,"y",y,"slot",index,"button",num(c,"button",0)));return true;
            }
            throw new IllegalStateException("Native selector cell outside interactive extraction");
        }
        if(kind.equals("regression-wands-unlock")){
            Object w=releaseWandsWindow();if(Boolean.TRUE.equals(field(w,"locked")))h("clickWindowControl",w,"LOCK");return true;
        }
'''
    helpers = r'''
    static Object releaseWandsWindow(){
        var found=new ArrayList<Object>();Object desktop=h("screen");if(desktop==null)throw new IllegalStateException("No desktop");
        for(Object w:(List<?>)field(desktop,"windows")){
            Object session=field(w,"session");if(session==null)continue;
            Object menu=call(session,"menu");String name=menu.getClass().getName();
            if(name.equals("net.nicguzzo.wands.menues.WandMenu")||name.equals("net.nicguzzo.wands.menues.WandToolsMenu"))
                if(field(w,"apiDefinition")!=null)found.add(w);
        }
        if(found.size()!=1)throw new IllegalStateException("Expected one native selector extraction, found "+found.size());return found.get(0);
    }
    static int[] releaseWandsTools(Object wand){
        if(wand==null)throw new IllegalStateException("Publisher wand state missing");Object data=field(wand,"player_data");
        if(data==null)throw new IllegalStateException("Publisher player data missing");Object values=call(data,"getIntArray","Tools");
        if(values instanceof java.util.Optional<?> optional)values=optional.orElse(null);
        return values==null?new int[0]:(int[])values;
    }
    static String releaseWandsStack(ItemStack stack){
        if(stack.isEmpty())return "{}";
        var ops=net.minecraft.resources.RegistryOps.create(net.minecraft.nbt.NbtOps.INSTANCE,player().registryAccess());
        return ItemStack.CODEC.encodeStart(ops,stack).result().orElseThrow(()->new IllegalStateException("Native selector item codec refused serialization")).toString();
    }
'''
    helpers=storage_item_saves(helpers,mc)
    if not named:
        commands = re.sub(r'\bh\(', 'regressionH(', commands)
        commands = re.sub(r'\bcall\(', 'coreCall(', commands)
        helpers = re.sub(r'\bh\(', 'regressionH(', helpers)
        helpers = re.sub(r'\bcall\(', 'coreCall(', helpers)
    text = text.replace(anchor, anchor + commands, 1)
    return text[:text.rfind('}')] + helpers + '\n}\n'
