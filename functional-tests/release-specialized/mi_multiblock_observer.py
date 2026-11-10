"""Read native multiblock blueprints, recipe status and backing hatches only."""
import re
from mi_storage_observer import enhance as storage_observations


def enhance(text,mc,named=False):
    text=storage_observations(text,mc,named)
    anchor='if(kind.equals("regression-world"))return true;';assert text.count(anchor)==1
    command=r'''
        if(kind.equals("regression-mi-multiblock-state")){
            var clients=new ArrayList<Object>();Object desktop=h("screen");
            if(desktop==null)throw new IllegalStateException("No native multiblock desktop");
            for(Object window:(List<?>)field(desktop,"windows")){
                if(field(window,"session")==null||field(window,"apiState")==null)continue;
                Object screen=field(field(window,"apiState"),"screen");var menu=(AbstractContainerMenu)call(screen,"getMenu");
                if(!menu.getClass().getName().startsWith("aztech.modern_industrialization."))continue;
                for(Object component:(Iterable<?>)field(menu,"components")){
                    if(!component.getClass().getSimpleName().equals("CraftingMultiblockGuiClient"))continue;
                    var data=new LinkedHashMap<String,Object>();CLIENT_DATA
                    clients.add(data);
                }
            }
            if(clients.size()!=1)throw new IllegalStateException("Expected one original multiblock status component");
            work=server(()->{
                var be=player().serverLevel().getBlockEntity(POS);if(be==null)throw new IllegalStateException("Missing controller");
                Object matcher=field(be,"shapeMatcher"),crafter=field(be,"crafter");
                if(matcher==null)throw new IllegalStateException("Native ticks have not initialized the matcher");
                var blueprint=new ArrayList<Object>();var flags=(Map<?,?>)field(matcher,"hatchFlags");
                for(var entry:((Map<?,?>)field(matcher,"simpleMembers")).entrySet()){
                    var at=(BlockPos)entry.getKey();var state=(net.minecraft.world.level.block.state.BlockState)call(entry.getValue(),"getPreviewState");
                    var allowed=new ArrayList<String>();Object flag=flags.get(at);
                    ALLOWED_TYPES
                    blueprint.add(Map.of("x",at.getX(),"y",at.getY(),"z",at.getZ(),"block",BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString(),"allows",allowed));
                }
                var pieces=new ArrayList<Object>();
                if(c.has("positions"))for(var element:c.getAsJsonArray("positions")){
                    var v=element.getAsJsonObject();var at=new BlockPos(num(v,"x",8),num(v,"y",65),num(v,"z",8));
                    if(at.distSqr(POS)>256)throw new IllegalStateException("Observation outside fixture");
                    var piece=player().serverLevel().getBlockEntity(at);if(piece==null)throw new IllegalStateException("Missing original hatch");
                    pieces.add(Map.of("x",at.getX(),"y",at.getY(),"z",at.getZ(),"nbt",PIECE_SAVE));
                }
                var result=new LinkedHashMap<String,Object>();result.put("client",clients.get(0));result.put("blueprint",blueprint);result.put("pieces",pieces);
                result.put("shapeValid",call(field(be,"shapeValid"),"isShapeValid"));
                result.put("active",call(crafter,"hasActiveRecipe"));result.put("progress",call(crafter,"getProgress"));
                result.put("controller",SAVE);result.put("operatingState",field(be,"operatingState").toString());
                write("release-mi-multiblock-state.json",result);return null;
            });return true;
        }
'''
    if mc=='1.21.1':
        allowed='if(flag!=null)for(Object type:(Iterable<?>)call(flag,"values"))allowed.add(call(type,"id").toString());'
        client='''Object nativeData=field(component,"data");data.put("shapeValid",call(nativeData,"isShapeValid"));
                    var active=(java.util.Optional<?>)call(nativeData,"activeRecipe");data.put("active",active.isPresent());
                    if(active.isPresent())data.put("progress",call(active.get(),"progress"));'''
    else:
        assert mc=='1.20.1'
        allowed='''if(flag!=null)for(var method:flag.getClass().getMethods())if(method.getName().equals("allows")&&method.getParameterCount()==1)
                        for(Object type:method.getParameterTypes()[0].getEnumConstants())if(Boolean.TRUE.equals(call(flag,"allows",type)))allowed.add(type.toString());'''
        client='''data.put("shapeValid",field(component,"isShapeValid"));data.put("active",field(component,"hasActiveRecipe"));
                    if(Boolean.TRUE.equals(field(component,"hasActiveRecipe")))data.put("progress",field(component,"progress"));'''
    command=command.replace('CLIENT_DATA',client).replace('ALLOWED_TYPES',allowed)
    for name,var in [('PIECE_SAVE','piece'),('SAVE','be')]:
        command=command.replace(name,var+'.saveWithFullMetadata('+('player().registryAccess()'if mc=='1.21.1'else'')+').toString()')
    # ShapeValidComponent exposes a public boolean in both exact publishers.
    command=command.replace('call(field(be,"shapeValid"),"isShapeValid")','field(field(be,"shapeValid"),"shapeValid")')
    if named:command=re.sub(r'\bPOS\b','origin()',command)
    else:
        command=re.sub(r'\bh\(','regressionH(',command);command=re.sub(r'\bcall\(','coreCall(',command)
    return text.replace(anchor,anchor+command,1)
