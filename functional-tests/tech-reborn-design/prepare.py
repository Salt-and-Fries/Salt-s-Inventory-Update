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


mod.qa_source = source
sys.argv = [sys.argv[0], '--fixture', 'build/tech-reborn-window-design/fixture.json', '--side', 'native', '--run', 'tech-reborn-design-02']
mod.main()
