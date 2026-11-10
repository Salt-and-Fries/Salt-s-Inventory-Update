"""Observe the real item-backed cutoff control without constructing a cell store."""
from mega_workbench_observer import enhance as workbench_observations


def enhance(text,mc,named=False):
    text=workbench_observations(text,mc,named)
    old='result.put("cellMutable",releaseMegaMutable(cell,List.of("ae2:storage_cell_config_inv","ae2:storage_cell_fuzzy_mode","ae2:upgrades")));'
    new='result.put("cellMutable",releaseMegaMutable(cell,List.of("ae2:storage_cell_config_inv","ae2:storage_cell_fuzzy_mode","ae2:upgrades","megacells:compression_cutoff")));result.put("bulk",releaseMegaBulk(menu,cell));'
    assert text.count(old)==1;text=text.replace(old,new,1)
    helpers=r'''
    static Map<String,Object> releaseMegaBulk(Object menu,ItemStack cell)throws ReflectiveOperationException{
        var result=new LinkedHashMap<String,Object>();if(cell.isEmpty())return result;
        if(!net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(cell.getItem()).toString().equals("megacells:bulk_item_cell"))throw new IllegalStateException("Unexpected native bulk cell");
        Class<?> components=Class.forName("gripe._90.megacells.definition.MEGAComponents");
        for(String name:List.of("BULK_CELL_ITEM","BULK_CELL_UNIT_COUNT","BULK_CELL_UNIT_FACTOR","BULK_CELL_COMPRESSION_CUTOFF")){
            var type=(net.minecraft.core.component.DataComponentType)components.getField(name).get(null);Object value=cell.get(type);
            if(value!=null)result.put(name,Map.of("javaType",value.getClass().getName(),"value",value.toString()));
        }
        // getChain only looks up recipe definitions and memoizes their key lookup.
        // BulkCellInventory/StorageCells factories can mutate persisted cell data
        // in their constructors and MUST NOT be called by this observer.
        Object host=call(menu,"getHost"),nativeCell=call(host,"getCell"),config=call(nativeCell,"getConfigInventory",cell),key=call(config,"getKey",0);
        Object chain=call(Class.forName("gripe._90.megacells.misc.CompressionService"),"getChain",key);var variants=new ArrayList<Object>();
        for(int i=0;i<((Number)call(chain,"size")).intValue();i++){
            ItemStack item=(ItemStack)call(chain,"getItem",i);Object itemKey=call(Class.forName("appeng.api.stacks.AEItemKey"),"of",item);
            variants.add(Map.of("item",regressionStack(item),"unitFactor",call(chain,"unitFactor",itemKey).toString()));
        }
        result.put("variants",variants);return result;
    }
'''
    return text[:text.rfind('}')]+helpers+'\n}\n'
