"""Prepare the normal production client with scoped, excluded observation/fixture hooks."""
import importlib.util
from pathlib import Path

ROOT=Path(__file__).resolve().parents[2]
spec=importlib.util.spec_from_file_location('production_prepare',ROOT/'tools/prepare-production-fabric-audit.py')
prep=importlib.util.module_from_spec(spec);spec.loader.exec_module(prep)
original=prep.qa_source
EXTRA=r'''
            case "accessories-character"->{
                try{
                Class<?> api=Class.forName("com.salts_inventory_update.api.client.desktop.DesktopClientApi");
                Object existing=((java.util.Optional<?>)api.getMethod("currentWorkspace").invoke(null)).orElse(null);
                if(existing==null||!(Boolean)coreCall(existing,"isNativeScreenActive"))api.getMethod("openPlayerWorkspace").invoke(null);
                Object workspace=((java.util.Optional<?>)api.getMethod("currentWorkspace").invoke(null)).orElseThrow();
                Object kind=java.util.Arrays.stream(Class.forName("com.salts_inventory_update.api.client.desktop.DesktopWorkspaceWindow$Kind").getEnumConstants()).filter(v->v.toString().equals("CHARACTER")).findFirst().orElseThrow();
                coreCall(workspace,"ensureWindow",kind);
                }catch(Exception e){throw new IllegalStateException(e);}
            }
            case "accessories-capacity"->work=server(()->{
                try{
                    Object cap=coreCall(Class.forName("io.wispforest.accessories.api.AccessoriesCapability"),"get",player());
                    Object container=((Map<?,?>)coreCall(cap,"getContainers")).get(str(c,"slot","ring"));
                    var id=java.util.UUID.fromString("d5b51430-0450-4c66-bb4c-51a090812340");coreCall(container,"removeModifier",id);
                    int amount=num(c,"amount",0);if(amount!=0)coreCall(container,"addTransientModifier",new net.minecraft.world.entity.ai.attributes.AttributeModifier(id,"SIU focused fixture",amount,net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADDITION));
                    coreCall(container,"update");return true;
                }catch(Exception e){throw new IllegalStateException(e);}
            });
            case "accessories-state"->work=server(()->{
                try{
                    Object cap=coreCall(Class.forName("io.wispforest.accessories.api.AccessoriesCapability"),"get",player());
                    var result=new LinkedHashMap<String,Object>();
                    for(var entry:((Map<?,?>)coreCall(cap,"getContainers")).entrySet()){
                        Object container=entry.getValue();var normal=(Container)coreCall(container,"getAccessories");var cosmetic=(Container)coreCall(container,"getCosmeticAccessories");
                        var real=new ArrayList<String>();var visual=new ArrayList<String>();
                        for(int i=0;i<normal.getContainerSize();i++)real.add(normal.getItem(i).save(new net.minecraft.nbt.CompoundTag()).toString());
                        for(int i=0;i<cosmetic.getContainerSize();i++)visual.add(cosmetic.getItem(i).save(new net.minecraft.nbt.CompoundTag()).toString());
                        result.put(entry.getKey().toString(),Map.of("normal",real,"cosmetic",visual,"render",coreCall(container,"renderOptions")));
                    }result.put("_player",Map.of("maxHealth",player().getMaxHealth()));write("accessories-state.json",result);return true;
                }catch(Exception e){throw new IllegalStateException(e);}
            });
'''
def source(side):
    text=original(side).replace('case "snapshot-server"',EXTRA+'\n            case "snapshot-server"',1)
    anchor='Object raw=coreCall(window,"containerMenu");if(raw instanceof AbstractContainerMenu menu){'
    observation=r'''
        if(menu.getClass().getName().endsWith(".AccessoriesDesktopMenu")){
            row.put("accessoriesScroll",field(field(window,"apiState"),"scroll"));
            var accessoryCells=new ArrayList<Object>();for(Object cell:(List<?>)field(menu,"cells")){
                Object group=field(cell,"group");Slot slot=(Slot)field(cell,"slot");
                accessoryCells.add(Map.of("slot",slot.index,"name",coreCall(group,"name"),"index",field(cell,"index"),"cosmetic",field(cell,"cosmetic"),"render",field(cell,"render")));
            }row.put("accessoriesCells",accessoryCells);
        }
    '''
    assert anchor in text
    return text.replace(anchor,anchor+observation,1)
prep.qa_source=source
if __name__=='__main__':prep.main()
