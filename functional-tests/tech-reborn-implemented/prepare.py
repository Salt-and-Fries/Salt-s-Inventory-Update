"""SIU-free reference client; the observer reads native structure blueprints."""
from pathlib import Path
import importlib.util, sys

ROOT = Path(__file__).resolve().parents[2]
spec = importlib.util.spec_from_file_location('reference', ROOT/'tools/prepare-production-fabric-audit.py')
mod = importlib.util.module_from_spec(spec)
spec.loader.exec_module(mod)
original = mod.qa_source

EXTRA = '''
    private static void trBlueprint(){
        try{
            Object be=player().serverLevel().getBlockEntity(POS);
            Class<?> writerType=Class.forName("reborncore.common.blockentity.MultiblockWriter");
            var blocks=new ArrayList<Object>();
            Object writer=Proxy.newProxyInstance(writerType.getClassLoader(),new Class<?>[]{writerType},(proxy,method,args)->{
                if(method.isDefault())return java.lang.reflect.InvocationHandler.invokeDefault(proxy,method,args);
                if(method.getName().equals("add")&&args.length==5){
                    var state=(net.minecraft.world.level.block.state.BlockState)args[4];
                    String serialized=state.toString().replace("Block{","").replace("}","");
                    blocks.add(Map.of("x",args[0],"y",args[1],"z",args[2],"state",serialized));
                    return proxy;
                }
                throw new IllegalArgumentException(method.toString());
            });
            var facing=(net.minecraft.core.Direction)coreCall(be,"getFacing");
            Object rotated=writerType.getMethod("rotate",net.minecraft.core.Direction.class).invoke(writer,facing.getOpposite());
            coreCall(be,"writeMultiblock",rotated);
            write("blueprint.json",Map.of("blocks",blocks,"valid",coreCall(be,"isMultiblockValid"),"facing",facing.toString(),"source","Native machine writeMultiblock; read only"));
        }catch(Exception error){throw new IllegalStateException(error);}
    }
'''


def source(side):
    s = original(side)
    s=s.replace('case "catalog"->{','case "tr-observe"->work=server(()->{trObserve();return true;});\n            case "catalog"->{',1)
    needle = 'row.put("nativeScreen",screen.getClass().getName());'
    s=s.replace(needle,needle+'''if(screen.getClass().getName().startsWith("techreborn."))try{
        Object tab=field(screen,"selectedTab");row.put("trSelectedTab",tab==null?"none":tab.getClass().getSimpleName());
        try{row.put("trValid",coreCall(field(screen,"be"),"isMultiblockValid"));}catch(Exception unsupported){}
        row.put("trUpgrades",field(screen,"upgrades"));try{row.put("cachedHeat",coreCall(field(screen,"be"),"getCachedHeat"));}catch(Exception absent){}
        row.put("sameModel",((net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?>)screen).getMenu()==coreCall(window,"containerMenu"));
        }catch(Exception error){row.put("trObservationError",error.toString());}''',1)
    needle = 'case "catalog"->{'
    assert needle in s
    s = s.replace(needle, 'case "tr-blueprint"->work=server(()->{trBlueprint();return true;});\n            '+needle)
    needle = 'out.put("title",screen.getTitle().getString());'
    assert needle in s
    s = s.replace(needle, needle+'''if(screen.getClass().getName().startsWith("techreborn."))try{
        Object selected=field(screen,"selectedTab");out.put("trSelectedTab",selected==null?"none":selected.getClass().getSimpleName());
        out.put("trValid",coreCall(field(screen,"be"),"isMultiblockValid"));
        }catch(Exception error){out.put("trObservationError",error.toString());}''', 1)
    i = s.rfind('\n}')
    return s[:i]+EXTRA+s[i:]


EXTRA += '''
    private static void trObserve(){
        var values=new LinkedHashMap<String,Object>();var be=player().serverLevel().getBlockEntity(POS);
        values.put("nbt",be.saveWithFullMetadata().toString());
        for(String name:List.of("getEnergy","getHeat","getCachedHeat","getMaxStoredPower","getOutputEnergy","getCurrentOutput","getCurrentRadius","getRadius","getDepth","getRange","getLaunchSpeed","getSlotConfiguration","getRedstoneConfiguration"))try{
            Object value=coreCall(be,name);values.put(name,String.valueOf(value));
            if(name.equals("getSlotConfiguration")||name.equals("getRedstoneConfiguration"))try{values.put(name+"Nbt",String.valueOf(coreCall(value,"write")));}catch(Exception absent){}
        }catch(Exception absent){}
        write("tr-state.json",values);
    }
'''
mod.qa_source = source
sys.argv = [sys.argv[0], '--fixture', 'build/tech-reborn-window-design/fixture.json', '--side', 'integrated', '--run', 'tech-reborn-implemented-10', '--core-jar', 'dist/core/salts_inventory_update-1.20.1-Fabric-0.1.6.jar', '--compat-jar', 'compatibility/versions/1.20.1/fabric/build/libs/salts_inventory_compat-1.20.1-Fabric-0.1.3.jar']
mod.main()
