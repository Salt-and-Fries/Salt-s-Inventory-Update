"""Read native zero-item Tech Reborn panels; never mutate their settings."""
import re
from native_item_serialization import storage_item_saves


def enhance(text,mc,named=False):
    anchor='if(kind.equals("regression-world"))return true;';assert text.count(anchor)==1
    command=r'''
        if(kind.equals("regression-techreborn-status")){
            Object window=releaseTrStatusWindow(),definition=field(window,"apiDefinition"),screen=field(field(definition,"initial"),"screen");
            AbstractContainerMenu menu=(AbstractContainerMenu)call(screen,"getMenu");Object entity=call(menu,"getBlockEntity");
            var pos=((net.minecraft.world.level.block.entity.BlockEntity)entity).getBlockPos();
            var client=new LinkedHashMap<String,Object>();client.put("native",releaseTrStatusValues(entity));
            client.put("menu",menu.getClass().getName());client.put("screen",screen.getClass().getName());
            client.put("session",call(field(window,"session"),"sessionId"));client.put("locked",field(window,"locked"));
            client.put("bounds",Map.of("x",field(window,"x"),"y",field(window,"y"),"width",field(window,"width"),"height",field(window,"height")));
            int left=((Number)field(screen,"leftPos")).intValue(),top=((Number)field(screen,"topPos")).intValue();
            var buttons=new ArrayList<Map<String,Object>>();
            for(Object child:(List<?>)call(screen,"children"))if(child.getClass().getSimpleName().equals("GuiButtonUpDown")){
                var w=(net.minecraft.client.gui.components.AbstractWidget)child;
                buttons.add(Map.of("type",child.getClass().getName(),"source_x",w.getX()+w.getWidth()/2.0-left,
                    "source_y",w.getY()+w.getHeight()/2.0-top,"active",w.active,"visible",w.visible));
            }
            buttons.sort(java.util.Comparator.comparingDouble(v->((Number)v.get("source_x")).doubleValue()));client.put("buttons",buttons);
            work=server(()->{
                var menus=new ArrayList<AbstractContainerMenu>();menus.add(player().inventoryMenu);
                if(!menus.contains(player().containerMenu))menus.add(player().containerMenu);
                com.salts_inventory_update.api.server.desktop.DesktopSessions.hasOpenSessionMatching(player(),m->{if(!menus.contains(m))menus.add(m);return false;});
                var found=menus.stream().filter(m->m.getClass().getName().equals("reborncore.common.screen.BuiltScreenHandler")
                    &&((net.minecraft.world.level.block.entity.BlockEntity)call(m,"getBlockEntity")).getBlockPos().equals(pos)).toList();
                if(found.size()!=1)throw new IllegalStateException("Expected one original Tech Reborn source menu");
                var original=found.get(0);Object owner=call(original,"getBlockEntity");
                var inventory=new ArrayList<Object>();for(int i=0;i<player().getInventory().getContainerSize();i++)inventory.add(Map.of("index",i,"item",releaseTrStatusStack(player().getInventory().getItem(i))));
                var cursors=new ArrayList<Object>();for(var m:menus)cursors.add(Map.of("menu",m.getClass().getName(),"item",releaseTrStatusStack(m.getCarried())));
                write("release-techreborn-status.json",Map.of("client",client,"native",releaseTrStatusValues(owner),"inventory",inventory,"cursors",cursors,
                    "position",Map.of("x",pos.getX(),"y",pos.getY(),"z",pos.getZ()),
                    "physical_slots",original.slots.stream().filter(s->s.container!=player().getInventory()).count()));return null;
            });return true;
        }
'''
    helpers=r'''
    static Object releaseTrStatusWindow(){
        var found=new ArrayList<Object>();Object desktop=h("screen");if(desktop==null)throw new IllegalStateException("No desktop");
        for(Object w:(List<?>)field(desktop,"windows")){Object s=field(w,"session");if(s!=null&&call(s,"menu").getClass().getName().equals("reborncore.common.screen.BuiltScreenHandler")&&field(w,"apiDefinition")!=null)found.add(w);}
        if(found.size()!=1)throw new IllegalStateException("Expected one native Tech Reborn window, found "+found.size());return found.get(0);
    }
    static Map<String,Object> releaseTrStatusValues(Object entity){
        var out=new LinkedHashMap<String,Object>();String name=entity.getClass().getSimpleName();
        out.put("type",entity.getClass().getName());out.put("energy",call(entity,"getEnergy"));out.put("capacity",call(entity,"getMaxStoredPower"));
        if(name.equals("LaunchpadBlockEntity")){
            out.put("selection",call(entity,"getSelection"));out.put("minimum",0);
            try{out.put("maximum",entity.getClass().getField("MAX_SELECTION").get(null));}
            catch(ReflectiveOperationException error){throw new IllegalStateException(error);}
        }else if(name.equals("PlayerDetectorBlockEntity")){
            out.put("selection",call(entity,"getCurrentRadius"));out.put("minimum",1);
            try{Object config=Class.forName("techreborn.config.TechRebornConfig").getField("playerDetectorMaxRadius").get(null);
                out.put("maximum",config instanceof Number?config:call(config,"get"));
            }catch(ReflectiveOperationException error){throw new IllegalStateException(error);}
        }else if(name.equals("SolarPanelBlockEntity")){
            out.put("generating",call(entity,"isGenerating"));out.put("rate",call(entity,"getGenerationRate"));
        }else if(!name.equals("ElevatorBlockEntity"))throw new IllegalStateException("Unexpected native status panel "+name);
        return out;
    }
    static String releaseTrStatusStack(ItemStack value){
        if(value.isEmpty())return "{}";var ops=net.minecraft.resources.RegistryOps.create(net.minecraft.nbt.NbtOps.INSTANCE,player().registryAccess());
        return ItemStack.CODEC.encodeStart(ops,value).result().orElseThrow(()->new IllegalStateException("Native Tech Reborn item codec refused serialization")).toString();
    }
'''
    helpers=storage_item_saves(helpers,mc)
    if not named:
        command=re.sub(r'\bh\(','regressionH(',command);command=re.sub(r'\bcall\(','coreCall(',command)
        helpers=re.sub(r'\bh\(','regressionH(',helpers);helpers=re.sub(r'\bcall\(','coreCall(',helpers)
    text=text.replace(anchor,anchor+command,1)
    return text[:text.rfind('}')]+helpers+'\n}\n'
