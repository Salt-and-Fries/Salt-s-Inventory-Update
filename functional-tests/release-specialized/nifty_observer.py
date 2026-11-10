"""Read the actual cart extraction regions; no slot or input result override."""
from native_item_serialization import storage_item_saves


def enhance(text, mc):
    assert mc in ['1.20.1', '1.21.1', '1.21.11']
    anchor = 'Object raw=coreCall(window,"containerMenu");if(raw instanceof AbstractContainerMenu menu){'
    assert text.count(anchor) == 1
    observation = r'''
                    if(menu.getClass().getName().startsWith("net.jmb19905.niftycarts.container.")){
                        Object context=coreCall(desktop,"apiContext",window,null,0,0);
                        var regions=new ArrayList<Object>();Object plan=coreCall(definition,"layout");
                        for(Object region:(List<?>)coreCall(plan,"regions")){
                            Object source=coreCall(region,"source");
                            regions.add(Map.of("interactive",coreCall(region,"interactive"),
                                "x",coreCall(definition,"regionX",context,region),"y",coreCall(definition,"regionY",context,region),
                                "source",Map.of("x",coreCall(source,"x"),"y",coreCall(source,"y"),
                                    "width",coreCall(source,"width"),"height",coreCall(source,"height"))));
                        }row.put("niftyInputRegions",regions);
                    }
'''
    text = text.replace(anchor, anchor + observation, 1)
    anchor = 'case "catalog"'; assert text.count(anchor) == 1
    command = r'''
            case "nifty-native-state"->work=server(()->{
                var menus=new ArrayList<AbstractContainerMenu>();menus.add(player().containerMenu);
                try{Class.forName("com.salts_inventory_update.api.server.desktop.DesktopSessions").getMethod("hasOpenSessionMatching",net.minecraft.world.entity.player.Player.class,java.util.function.Predicate.class)
                    .invoke(null,player(),(java.util.function.Predicate<AbstractContainerMenu>)m->{if(!menus.contains(m))menus.add(m);return false;});}
                catch(Exception failure){throw new IllegalStateException(failure);}
                var rows=new ArrayList<Object>();
                for(var menu:menus){var cells=new ArrayList<Object>();
                    for(int i=0;i<menu.slots.size();i++)cells.add(Map.of("slot",i,"item",niftyStack(menu.getSlot(i).getItem())));
                    rows.add(Map.of("class",menu.getClass().getName(),"slots",cells,"carried",niftyStack(menu.getCarried())));
                }
                var inventory=new ArrayList<Object>();
                for(int i=0;i<player().getInventory().getContainerSize();i++)inventory.add(Map.of("index",i,"item",niftyStack(player().getInventory().getItem(i))));
                write("nifty-native-state.json",Map.of("menus",rows,"inventory",inventory));return true;
            });
'''
    text = text.replace(anchor, command + '\n            ' + anchor, 1)
    helper = r'''
    static String niftyStack(ItemStack value){
        if(value.isEmpty())return "{}";
        var ops=net.minecraft.resources.RegistryOps.create(net.minecraft.nbt.NbtOps.INSTANCE,player().registryAccess());
        return ItemStack.CODEC.encodeStart(ops,value).result().orElseThrow(()->new IllegalStateException("Native cart item codec refused serialization")).toString();
    }
'''
    helper=storage_item_saves(helper,mc)
    return text[:text.rfind('}')] + helper + '\n}\n'
