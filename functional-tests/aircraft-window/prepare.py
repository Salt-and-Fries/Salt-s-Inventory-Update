"""Scoped Aircraft fixture observations and ordinary public desktop quick-move input."""
from pathlib import Path
import importlib.util
ROOT=Path(__file__).resolve().parents[2]
spec=importlib.util.spec_from_file_location('production_prepare',ROOT/'tools/prepare-production-fabric-audit.py')
prep=importlib.util.module_from_spec(spec);spec.loader.exec_module(prep);original=prep.qa_source
EXTRA=r'''
            case "aircraft-handle-move"->{
                try{
                    var api=Class.forName("com.salts_inventory_update.api.client.desktop.DesktopClientApi");var workspace=((java.util.Optional<?>)api.getMethod("currentWorkspace").invoke(null)).orElseThrow();
                    Object window=((List<?>)coreCall(workspace,"windows")).stream().filter(w->coreCall(w,"title").toString().equals(str(c,"title",""))).findFirst().orElseThrow();
                    Object handle=coreCall(window,"handle");write("aircraft-handle.json",Map.of("moved",coreCall(handle,"moveTo",c.get("x").getAsInt(),c.get("y").getAsInt())));
                }catch(Exception e){throw new IllegalStateException(e);}
            }
            case "aircraft-state"->work=server(()->{
                var rows=new ArrayList<Object>();
                for(var entity:player().serverLevel().getAllEntities())if(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getNamespace().equals("immersive_aircraft")){
                    var row=new LinkedHashMap<String,Object>();row.put("entity",BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString());row.put("id",entity.getId());row.put("nbt",entity.saveWithoutId(new net.minecraft.nbt.CompoundTag()).toString());
                    try{var inventory=(Container)coreCall(entity,"getInventory");var items=new ArrayList<String>();for(int i=0;i<inventory.getContainerSize();i++)items.add(inventory.getItem(i).save(new net.minecraft.nbt.CompoundTag()).toString());row.put("items",items);}catch(Exception absent){}
                    try{row.put("fuelUtilization",coreCall(entity,"getFuelUtilization"));}catch(Exception absent){}rows.add(row);
                }write("aircraft-state.json",rows);return true;
            });
            case "aircraft-quick"->{
                try{
                    var api=Class.forName("com.salts_inventory_update.api.client.desktop.DesktopClientApi");var workspace=((java.util.Optional<?>)api.getMethod("currentWorkspace").invoke(null)).orElseThrow();
                    Object window=((List<?>)coreCall(workspace,"windows")).stream().filter(w->coreCall(w,"title").toString().equals(str(c,"title",""))).findFirst().orElseThrow();
                    Object handle=coreCall(window,"handle");coreCall(workspace,"clickAt",coreCall(handle,"instanceId"),c.get("x").getAsDouble(),c.get("y").getAsDouble(),0,net.minecraft.world.inventory.ClickType.QUICK_MOVE);
                }catch(Exception e){throw new IllegalStateException(e);}
            }
'''
def enhance(text):
    s=text.replace('case "snapshot-server"',EXTRA+'\n            case "snapshot-server"',1)
    anchor='Object raw=coreCall(window,"containerMenu");if(raw instanceof AbstractContainerMenu menu){'
    observe=r'''
        if(menu.getClass().getName().endsWith(".AircraftDesktopMenu")){
            row.put("aircraftTitle",coreCall(window,"debugName"));
            Object handle=coreCall(desktop,"apiWindowHandle",window);var capabilities=new LinkedHashMap<String,Object>();
            Class<?> actions=Class.forName("com.salts_inventory_update.api.client.desktop.DesktopWindowAction");
            for(Object action:actions.getEnumConstants())capabilities.put(action.toString(),coreCall(handle,"available",action));
            row.put("aircraftActions",capabilities);row.put("aircraftPopup",field(desktop,"popupWindow")==window);
            row.put("aircraftZone",field(menu,"zone").toString());row.put("aircraftBodyWidth",field(menu,"bodyWidth"));row.put("aircraftBodyHeight",field(menu,"bodyHeight"));row.put("aircraftContentY",coreCall(window,"contentY"));
            var aircraftCells=new ArrayList<Object>();for(Object cell:(List<?>)field(menu,"cells")){
                Slot slot=(Slot)field(cell,"slot");aircraftCells.add(Map.of("slot",slot.index,"sourceIndex",field(cell,"nativeIndex"),"kind",field(cell,"kind"),"x",field(cell,"x"),"y",field(cell,"y"),"nativeX",field(cell,"nativeX"),"nativeY",field(cell,"nativeY")));
            }row.put("aircraftCells",aircraftCells);
        }
    '''
    assert anchor in s
    return s.replace(anchor,anchor+observe,1)

def source(side):
    return enhance(original(side))
prep.qa_source=source
if __name__=='__main__':prep.main()
