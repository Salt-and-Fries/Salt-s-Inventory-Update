"""Excluded observations for native Mine Cells inside SIU; subject release unchanged."""
from pathlib import Path
import importlib.util

ROOT=Path(__file__).resolve().parents[2]
spec=importlib.util.spec_from_file_location('minecells_research',ROOT/'functional-tests/minecells-research/prepare.py')
research=importlib.util.module_from_spec(spec);spec.loader.exec_module(research)
prep=research.prep;original=prep.qa_source

def source(side):
    s=original(side)
    # Disposable preference only: make movement checks independent of the
    # ordinary default which initially locks newly opened windows.
    s=s.replace('config.getClass().getField("enableMod").setBoolean(config,true);',
                'config.getClass().getField("enableMod").setBoolean(config,true);config.getClass().getField("openUnlocked").setBoolean(config,true);')
    anchor='Object raw=coreCall(window,"containerMenu");if(raw instanceof AbstractContainerMenu menu){'
    observe=r'''
        if(menu.getClass().getName().equals("com.github.mim1q.minecells.screen.cellcrafter.CellCrafterScreenHandler")){
            row.put("minecellsContent",Map.of("x",coreCall(window,"contentX"),"y",coreCall(window,"contentY")));
            int left=((Number)field(window,"x")).intValue(),top=((Number)field(window,"y")).intValue(),width=((Number)field(window,"width")).intValue();
            var controls=new LinkedHashMap<String,Object>();
            for(int px=left;px<left+width;px++){
                Object control=coreCall(desktop,"titleBarControlAt",window,(double)px,(double)(top+10));
                if(control!=null)controls.putIfAbsent(control.toString(),Map.of("x",px+1,"y",top+10));
            }
            row.put("minecellsControls",controls);
            var nativeScreen=Class.forName("com.salts_inventory_update.compat.minecells.MineCellsClient").getMethod("screen",AbstractContainerMenu.class).invoke(null,menu);
            var result=new LinkedHashMap<String,Object>();
            Object forge=field(nativeScreen,"forgeButton");result.put("craftEnabled",forge instanceof net.minecraft.client.gui.components.AbstractWidget widget&&widget.active);
            result.put("managementOpen",field(desktop,"popupWindow")==window);
            if(field(desktop,"popupWindow")==window){
                Object popup=coreCall(desktop,"popupRect",window);var popupControls=new LinkedHashMap<String,Object>();
                for(Object rect:(List<?>)coreCall(desktop,"popupControlRects",window,popup))
                    popupControls.put(coreCall(rect,"control").toString(),Map.of("x",((Number)coreCall(rect,"x")).intValue()+5,"y",((Number)coreCall(rect,"y")).intValue()+5));
                result.put("popupControls",popupControls);
            }
            for(String k:List.of("isRecipeListVisible","selectedRecipe","blockPos"))result.put(k,String.valueOf(field(nativeScreen,k)));
            Object list=field(nativeScreen,"recipeList");
            for(String k:List.of("allRecipes","currentRecipes","visibleRecipes")){Object v=field(list,k);result.put(k,v instanceof List<?> l?l.size():String.valueOf(v));}
            for(String k:List.of("selectedCategory","scrollOffset","search","selectedRecipe"))result.put("list-"+k,String.valueOf(field(list,k)));
            var elements=new ArrayList<Object>();minecellsElements(field(field(nativeScreen,"uiAdapter"),"rootComponent"),elements,0);result.put("elements",elements);
            row.put("minecells",result);
        }
    '''
    assert anchor in s;s=s.replace(anchor,anchor+observe,1)
    command=r'''
        case "minecells-caption-pack"->{
            var repository=mc.getResourcePackRepository();var packs=new ArrayList<String>(repository.getSelectedIds());
            packs.remove("file/siu-control-caption-test");
            if(c.get("enabled").getAsBoolean())packs.add("file/siu-control-caption-test");
            repository.setSelected(packs);work=mc.reloadResourcePacks();
        }
        case "minecells-server"->work=server(()->{
            var drops=new ArrayList<Object>();
            for(var e:player().serverLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(POS).inflate(8)))
                drops.add(e.getItem().save(new net.minecraft.nbt.CompoundTag()).toString());
            var inv=new ArrayList<Object>();for(int i=0;i<player().getInventory().getContainerSize();i++)inv.add(Map.of("index",i,"item",player().getInventory().getItem(i).save(new net.minecraft.nbt.CompoundTag()).toString()));
            write("minecells-server.json",Map.of("drops",drops,"inventory",inv));return true;
        });
    '''
    return s.replace('case "snapshot-server"',command+'\n            case "snapshot-server"',1)

prep.qa_source=source
if __name__=='__main__':prep.main()
