"""Read original wireless Requester models and Survival/native energy state."""
from requester_observer import enhance as requester_observations

SCREEN='com.almostreliable.merequester.compat.wtlib.ReqWirelessTerminalScreen'
MENU='com.almostreliable.merequester.compat.wtlib.ReqWirelessTerminalMenu'


def enhance(text,mc,named=False):
    assert mc in ['1.21.1','26.1.2']and named
    text=requester_observations(text,mc,named)
    replacements={
        'List.of("com.almostreliable.merequester.client.RequesterScreen","com.almostreliable.merequester.client.RequesterTerminalScreen").contains(content.getClass().getName())':'List.of("com.almostreliable.merequester.client.RequesterScreen","com.almostreliable.merequester.client.RequesterTerminalScreen","'+SCREEN+'").contains(content.getClass().getName())',
        'boolean terminal=content.getClass().getSimpleName().equals("RequesterTerminalScreen");':'boolean terminal=content.getClass().getSimpleName().equals("RequesterTerminalScreen")||content.getClass().getName().equals("'+SCREEN+'");',
        'boolean terminal=menu.getClass().getName().equals("com.almostreliable.merequester.terminal.RequesterTerminalMenu");':'boolean terminal=List.of("com.almostreliable.merequester.terminal.RequesterTerminalMenu","'+MENU+'").contains(menu.getClass().getName());',
    }
    # Broaden only the exact original observation filters/terminal subtype
    # classification. No existing native screen/model or input path is replaced.
    for old,new in replacements.items():assert text.count(old)==1; text=text.replace(old,new,1)
    anchor='if(kind.equals("regression-world"))return true;';assert text.count(anchor)==1
    command=r'''
        if(kind.equals("regression-requester-wireless-energy")){
            work=server(()->{
                var rows=new ArrayList<Object>();Object state=call(com.salts_inventory_update.server.DesktopContainerSessions.class,"sessions",player());
                for(var entry:((Map<?,?>)field(state,"sessions")).entrySet()){
                    Object menu=field(entry.getValue(),"menu");if(!menu.getClass().getName().equals("com.almostreliable.merequester.compat.wtlib.ReqWirelessTerminalMenu"))continue;
                    Object host=field(menu,"wrMenuHost");ItemStack source=(ItemStack)call(host,"getItemStack");Object stored=call(source.getItem(),"getAECurrentPower",source),drain=call(host,"getPowerDrainPerTick");
                    Object point=field(host,"currentAccessPoint");if(!(point instanceof net.minecraft.world.level.block.entity.BlockEntity block))throw new IllegalStateException("No actual native wireless access point");var pos=block.getBlockPos();
                    var row=new LinkedHashMap<String,Object>();row.put("session",entry.getKey());row.put("source",regressionStack(source));row.put("sourceSlot",call(host,"getPlayerInventorySlot"));row.put("sameSource",source==player().getInventory().getItem(0));
                    row.put("stored",stored);row.put("storedJavaType",stored.getClass().getName());row.put("drain",drain);row.put("drainJavaType",drain.getClass().getName());row.put("remainingEnergyTicks",field(host,"remainingEnergyTicks"));
                    row.put("creative",player().isCreative());row.put("quantumConnected",releaseRequesterWirelessPublic(field(host,"quantumStatus"),"connected"));row.put("accessPoint",List.of(pos.getX(),pos.getY(),pos.getZ()));
                    row.put("position",List.of(player().getX(),player().getY(),player().getZ()));row.put("gameTime",player().level().getGameTime());rows.add(row);
                }
                if(rows.size()!=1)throw new IllegalStateException("Expected one actual wireless Requester power host");write("release-requester-wireless-energy.json",Map.of("server",rows));return null;
            });return true;
        }
'''
    helpers=r'''
    static Object releaseRequesterWirelessPublic(Object value,String name){
        try{var method=value.getClass().getMethod(name);method.setAccessible(true);return method.invoke(value);}
        catch(ReflectiveOperationException error){throw new IllegalStateException("Could not read original wireless status "+name,error);}
    }
'''
    text=text.replace(anchor,anchor+command,1);return text[:text.rfind('}')]+helpers+'\n}\n'
