"""Production Zenith integration observer; no observer code enters release JARs."""
from pathlib import Path
import importlib.util

ROOT=Path(__file__).resolve().parents[1]
spec=importlib.util.spec_from_file_location('zenith_design',ROOT/'tools/prepare-zenith-design.py')
design=importlib.util.module_from_spec(spec);spec.loader.exec_module(design)


def source(side):
    text=design.source(side)
    needle='static Map<String,Object> nativeValues(Object screen){\n        var values=new LinkedHashMap<String,Object>();'
    replacement=needle+'''
        if(screen.getClass().getName().startsWith("dev.shadowsoffire.apotheosis."))try{
            var menu=((net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?>)screen).getMenu();
            if(menu instanceof net.minecraft.world.inventory.EnchantmentMenu ench){
                values.put("costs",ench.costs);values.put("enchantClue",ench.enchantClue);
                Object stats=field(menu,"stats");for(String key:List.of("eterna","quanta","arcana","clues"))values.put(key,coreCall(stats,key));
                values.put("receivedClues",String.valueOf(field(screen,"clues")));
            }
            if(screen.getClass().getName().endsWith("EnchLibraryScreen")){
                values.put("storedEnchantments",coreCall(menu,"getNumStoredEnchants"));
                values.put("scrollOffs",field(screen,"scrollOffs"));values.put("startIndex",field(screen,"startIndex"));
                values.put("libraryRows",((java.util.List<?>)field(screen,"data")).size());
                for(var child:((net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?>)screen).children())if(child instanceof net.minecraft.client.gui.components.EditBox box)values.put("search",box.getValue());
            }
            if(screen.getClass().getName().endsWith("AugmentingScreen")){
                values.put("affixes",((java.util.List<?>)field(screen,"currentItemAffixes")).size());
                values.put("selectedAffix",coreCall(screen,"getSelectedAffix"));
                values.put("dropdownOpen",coreCall(field(screen,"list"),"isOpen"));
            }
        }catch(Exception failure){values.put("zenithObserverError",failure.toString());}
'''
    assert needle in text
    text=text.replace(needle,replacement)
    needle='write("server-state.json",Map.of('
    replacement='''
                var zenithRows=new ArrayList<Object>();
                for(var menu:menus)if(menu.getClass().getName().endsWith("ApothEnchantmentMenu")){
                    var value=new LinkedHashMap<String,Object>();Object stats=field(menu,"stats");
                    for(String key:List.of("eterna","quanta","arcana","clues"))value.put(key,coreCall(stats,key));
                    try{
                        var cls=Class.forName("com.salts_inventory_update.compat.zenith.ZenithWindows");var f=cls.getDeclaredField("ENCHANTING");f.setAccessible(true);
                        Object owner=((java.util.Map<?,?>)f.get(null)).get(menu);
                        if(owner!=null){value.put("cachedStats",field(owner,"stats")!=null);value.put("cachedClues",((java.util.Map<?,?>)field(owner,"clues")).size());value.put("dirty",field(owner,"dirty"));}
                    }catch(ReflectiveOperationException error){value.put("observerError",error.toString());}
                    zenithRows.add(value);
                }
                write("zenith-server-state.json",zenithRows);
                write("server-state.json",Map.of('''
    assert needle in text
    return text.replace(needle,replacement)


if __name__=='__main__':
    design.prep.qa_source=source;design.prep.main()
