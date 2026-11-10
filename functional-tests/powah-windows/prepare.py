"""Excluded production-client Powah fixtures; publisher and subject JARs stay intact."""
from pathlib import Path
import importlib.util
import sys

ROOT=Path(__file__).resolve().parents[2]
sys.path.insert(0,str(ROOT/'tools'))
EXTRA=r'''
            case "powah-setup"->{
                closeCore();mc.player.closeContainer();mc.setScreen(null);
                work=server(()->{
                    var p=player();var level=p.serverLevel();p.getInventory().clearContent();
                    for(int x=6;x<=10;x++)for(int y=65;y<=69;y++)for(int z=6;z<=10;z++)level.removeBlock(new BlockPos(x,y,z),false);
                    for(int x=6;x<=10;x++)for(int z=6;z<=10;z++)level.setBlockAndUpdate(new BlockPos(x,64,z),Blocks.STONE.defaultBlockState());
                    String chosen=str(c,"block","powah:energy_cell_basic");
                    if(chosen.contains("reactor")){
                        p.getInventory().setItem(p.getInventory().selected,new ItemStack(BuiltInRegistries.ITEM.get(id(chosen)),36));
                    }else{
                        if(chosen.contains("ender_gate"))level.setBlockAndUpdate(POS.north(),BuiltInRegistries.BLOCK.get(id("powah:energy_cell_basic")).defaultBlockState().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED,false));
                        var block=BuiltInRegistries.BLOCK.get(id(chosen));var state=block.defaultBlockState();
                        if(state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED))state=state.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED,false);
                        if(chosen.contains("ender_gate"))state=state.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING,Direction.NORTH);
                        level.setBlockAndUpdate(POS,state);block.setPlacedBy(level,POS,state,p,new ItemStack(block.asItem()));
                        if(chosen.contains("cable"))level.setBlockAndUpdate(POS.north(),BuiltInRegistries.BLOCK.get(id("powah:energy_cell_basic")).defaultBlockState());
                        if(chosen.contains("thermo"))level.setBlockAndUpdate(POS.below(),Blocks.MAGMA_BLOCK.defaultBlockState());
                    }
                    p.connection.teleport(8.5,65,12.5,180,8);return true;
                });
            }
            case "powah-place-reactor"->{
                mc.gameMode.useItemOn(mc.player,InteractionHand.MAIN_HAND,new BlockHitResult(new Vec3(8.5,65,8.5),Direction.UP,POS.below(),false));
            }
            case "powah-open-face"->{
                String name=str(c,"face","north");Direction side=Direction.byName(name);
                if(side==null)throw new IllegalArgumentException("Unknown fixture face");
                Vec3 hit=Vec3.atCenterOf(POS).add(side.getStepX()*.49,side.getStepY()*.49,side.getStepZ()*.49);
                mc.gameMode.useItemOn(mc.player,InteractionHand.MAIN_HAND,new BlockHitResult(hit,side,POS,false));
            }
            case "powah-set-energy"->work=server(()->{
                var te=player().serverLevel().getBlockEntity(POS);var energy=coreCall(te,"getEnergy");
                long capacity=((Number)coreCall(energy,"getCapacity")).longValue();
                coreCall(energy,"setStored",c.has("value")?c.get("value").getAsLong():capacity/2);
                coreCall(te,"sync");return true;
            });
            case "powah-quick-player"->{
                try{
                var workspace=((Optional<?>)Class.forName("com.salts_inventory_update.api.client.desktop.DesktopClientApi").getMethod("currentWorkspace").invoke(null)).orElseThrow();
                Object inventory=((List<?>)coreCall(workspace,"windows")).stream().filter(w->coreCall(w,"kind").toString().equals("PLAYER_INVENTORY")).findFirst().orElseThrow();
                Object identity=coreCall(coreCall(inventory,"handle"),"instanceId");
                coreCall(workspace,"clickAt",identity,c.get("x").getAsDouble(),c.get("y").getAsDouble(),0,ClickType.QUICK_MOVE);
                }catch(ReflectiveOperationException failure){throw new IllegalStateException(failure);}
            }
            case "powah-state"->work=server(()->{
                var rows=new ArrayList<Object>();var menus=new ArrayList<AbstractContainerMenu>();
                try{Class.forName("com.salts_inventory_update.api.server.desktop.DesktopSessions").getMethod("hasOpenSessionMatching",net.minecraft.world.entity.player.Player.class,java.util.function.Predicate.class)
                    .invoke(null,player(),(java.util.function.Predicate<AbstractContainerMenu>)m->{if(m.getClass().getName().startsWith("owmii.powah.inventory."))menus.add(m);return false;});}
                catch(ClassNotFoundException nativeMode){menus.add(player().containerMenu);}
                catch(ReflectiveOperationException failure){throw new IllegalStateException(failure);}
                for(var m:menus){var row=new LinkedHashMap<String,Object>();row.put("class",m.getClass().getName());row.put("id",m.containerId);
                    row.put("state",powahState(field(m,"te")));try{row.put("face",coreCall(m,"getSide").toString());}catch(Exception absent){}
                    var slots=new ArrayList<Object>();for(int i=0;i<m.slots.size();i++){var s=m.getSlot(i);if(!(s.container instanceof Inventory))slots.add(Map.of("index",i,"item",s.getItem().save(new net.minecraft.nbt.CompoundTag()).toString()));}
                    row.put("slots",slots);rows.add(row);}
                var inv=new ArrayList<Object>();for(int i=0;i<player().getInventory().getContainerSize();i++)inv.add(Map.of("index",i,"item",player().getInventory().getItem(i).save(new net.minecraft.nbt.CompoundTag()).toString()));
                write("powah-state.json",Map.of("menus",rows,"inventory",inv));return true;
            });
'''

HELPERS=r'''
    static Map<String,Object> powahState(Object te){
        var state=new LinkedHashMap<String,Object>();
        for(String name:List.of("getRedstoneMode","getMaxChannels","isGenModeOn","isBuilt"))try{state.put(name,coreCall(te,name).toString());}catch(Exception absent){}
        try{var e=coreCall(te,"getEnergy");state.put("stored",coreCall(e,"getStored"));state.put("capacity",coreCall(e,"getCapacity"));}catch(Exception absent){}
        try{var channel=coreCall(te,"getChannel");state.put("channel",coreCall(channel,"get"));}catch(Exception absent){}
        try{var sides=coreCall(te,"getSideConfig");var values=new ArrayList<String>();for(Direction side:Direction.values())values.add(coreCall(sides,"getType",side).toString());state.put("faces",values);}catch(Exception absent){}
        if(te instanceof net.minecraft.world.level.block.entity.BlockEntity block)state.put("nbt",block.saveWithFullMetadata().toString());
        return state;
    }
'''


def patch(text):
    assert 'case "fixture"->{' in text
    text=text.replace('case "fixture"->{',EXTRA+'case "fixture"->{',1)
    return text[:text.rfind('}')]+HELPERS+'\n}\n'


if __name__=='__main__':
    forge='--forge' in sys.argv
    if forge:sys.argv.remove('--forge')
    name='prepare-rollout-production-forge.py' if forge else 'prepare-rollout-production-modern.py'
    spec=importlib.util.spec_from_file_location('powah_production',ROOT/'tools'/name)
    prep=importlib.util.module_from_spec(spec);spec.loader.exec_module(prep)
    target=prep.modern if forge else prep
    old=target.qa_source
    def qa(side):
        source,text=old(side);return source,patch(text)
    target.qa_source=qa
    prep.main()
