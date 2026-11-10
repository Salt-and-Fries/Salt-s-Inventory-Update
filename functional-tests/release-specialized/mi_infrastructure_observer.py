"""Read original pump progress and tank data/blueprint without changing them."""
import re
from mi_storage_observer import enhance as storage_observations


def enhance(text,mc,named=False):
    assert mc in {'1.20.1','1.21.1'}
    text=storage_observations(text,mc,named);anchor='if(kind.equals("regression-world"))return true;';assert text.count(anchor)==1
    command=r'''
        if(kind.equals("regression-mi-infrastructure-state")){
            var clients=new ArrayList<Object>();Object desktop=h("screen");
            if(desktop==null)throw new IllegalStateException("No native MI infrastructure window");
            for(Object window:(List<?>)field(desktop,"windows")){
                if(field(window,"session")==null||field(window,"apiState")==null)continue;
                Object screen=field(field(window,"apiState"),"screen");var menu=(AbstractContainerMenu)call(screen,"getMenu");
                if(!menu.getClass().getName().startsWith("aztech.modern_industrialization."))continue;
                var components=new LinkedHashMap<String,Object>();
                for(Object component:(Iterable<?>)field(menu,"components")){
                    String name=component.getClass().getSimpleName();
                    if(name.equals("ProgressBarClient"))components.put("progress",PROGRESS);
                    if(name.equals("LargeTankFluidDisplayClient")){
                        Object data=TANK_DATA;
                        var fluid=(net.minecraft.world.level.material.Fluid)call(field(data,"fluid"),"getFluid");
                        components.put("tank",Map.of("fluid",BuiltInRegistries.FLUID.getKey(fluid).toString(),"amount",field(data,"amount"),"capacity",field(data,"capacity")));
                    }
                }
                clients.add(Map.of("menu",menu.getClass().getName(),"components",components));
            }
            if(clients.size()!=1)throw new IllegalStateException("Expected one native MI infrastructure panel");
            work=server(()->{
                var be=player().serverLevel().getBlockEntity(POS);if(be==null)throw new IllegalStateException("Native infrastructure source missing");
                var result=new LinkedHashMap<String,Object>();result.put("client",clients.get(0));result.put("fixture",SAVE);result.put("blockEntity",be.getClass().getName());
                if(be.getClass().getSimpleName().equals("LargeTankMultiblockBlockEntity")){
                    result.put("shapeValid",call(be,"isShapeValid"));Object matcher=field(be,"shapeMatcher");
                    if(matcher==null)throw new IllegalStateException("Original tank has not initialized its normal shape matcher");
                    var blueprint=new ArrayList<Object>();
                    for(var entry:((Map<?,?>)field(matcher,"simpleMembers")).entrySet()){
                        var position=(BlockPos)entry.getKey();
                        var state=(net.minecraft.world.level.block.state.BlockState)call(entry.getValue(),"getPreviewState");
                        blueprint.add(Map.of("x",position.getX(),"y",position.getY(),"z",position.getZ(),"block",BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString()));
                    }
                    result.put("blueprint",blueprint);
                }
                write("release-mi-infrastructure-state.json",result);return null;
            });return true;
        }
'''
    modern=mc=='1.21.1'
    command=command.replace('PROGRESS','field(component,"data")'if modern else'field(component,"progress")')
    command=command.replace('TANK_DATA','field(component,"data")'if modern else'field(component,"fluidData")')
    command=command.replace('SAVE','be.saveWithFullMetadata(player().registryAccess()).toString()'if modern else'be.saveWithFullMetadata().toString()')
    if not named:
        command=re.sub(r'\bh\(','regressionH(',command);command=re.sub(r'\bcall\(','coreCall(',command)
    else:command=re.sub(r'\bPOS\b','origin()',command)
    return text.replace(anchor,anchor+command,1)
