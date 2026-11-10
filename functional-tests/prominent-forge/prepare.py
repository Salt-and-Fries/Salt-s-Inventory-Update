"""Excluded, read-only observations for the two native Prominent forge menus."""
from pathlib import Path
import importlib.util

ROOT=Path(__file__).resolve().parents[2]
spec=importlib.util.spec_from_file_location('production_prepare',ROOT/'tools/prepare-production-fabric-audit.py')
prep=importlib.util.module_from_spec(spec);spec.loader.exec_module(prep);original=prep.qa_source

def source(side):
    s=original(side)
    anchor='Object raw=coreCall(window,"containerMenu");if(raw instanceof AbstractContainerMenu menu){'
    observe=r'''
        if(menu.getClass().getName().startsWith("elocindev.prominentbase.client.gui.PrimordialForge")){
            row.put("forgeContent",Map.of("x",coreCall(window,"contentX"),"y",coreCall(window,"contentY")));
            var screen=Class.forName("com.salts_inventory_update.compat.prominent.ProminentAccess").getMethod("screen",AbstractContainerMenu.class).invoke(null,menu);
            var values=new LinkedHashMap<String,Object>();
            for(String name:List.of("offset","targetOffset","selectedSlot","hoveredSlot","craftingConditions","displayedStage","selectedSet"))try{values.put(name,String.valueOf(field(screen,name)));}catch(Exception absent){}
            try{values.put("armorName",String.valueOf(field(field(screen,"selectedSet"),"name")));}catch(Exception absent){}
            try{values.put("rootSurface",coreCall(field(field(screen,"uiAdapter"),"rootComponent"),"surface").toString());}catch(Exception absent){}
            row.put("forgeValues",values);
        }
    '''
    assert anchor in s;s=s.replace(anchor,anchor+observe,1)
    command=r'''
            case "prominent-state"->work=server(()->{
                var result=new LinkedHashMap<String,Object>();
                for(String name:List.of("primordialforge.PrimordialForgeProgressComponent","cinders.CindersComponent")){
                    try{
                        var cls=Class.forName("elocindev.prominentbase.data."+name);var key=cls.getField("KEY").get(null);var value=coreCall(key,"get",player());
                        var nbt=new net.minecraft.nbt.CompoundTag();coreCall(value,"writeToNbt",nbt);result.put(name,nbt.toString());
                        if(name.startsWith("primordialforge")){
                            Object stage=coreCall(value,"getNextStage");result.put("nextStage",String.valueOf(stage));
                        }
                    }catch(Exception error){result.put(name,error.toString());}
                }
                var inv=new ArrayList<Object>();for(int i=0;i<player().getInventory().getContainerSize();i++)inv.add(Map.of("index",i,"item",player().getInventory().getItem(i).save(new net.minecraft.nbt.CompoundTag()).toString()));result.put("inventory",inv);
                write("prominent-state.json",result);return true;
            });
    '''
    assert 'case "snapshot-server"' in s
    s=s.replace('case "snapshot-server"',command+'\n            case "snapshot-server"',1)
    # Native HoverableButtonContainer reads its render-time hover flag before
    # accepting a click. Simulate pointer position as well as the screen event;
    # calling mouseMoved alone leaves Minecraft rendering the old mouse point.
    pointer=r'''
            case "pointer"->{
                try{
                double x=c.get("x").getAsDouble(),y=c.get("y").getAsDouble();
                var xf=mc.mouseHandler.getClass().getDeclaredField("field_1795");xf.setAccessible(true);
                var yf=mc.mouseHandler.getClass().getDeclaredField("field_1794");yf.setAccessible(true);
                xf.setDouble(mc.mouseHandler,x*mc.getWindow().getScreenWidth()/mc.getWindow().getGuiScaledWidth());
                yf.setDouble(mc.mouseHandler,y*mc.getWindow().getScreenHeight()/mc.getWindow().getGuiScaledHeight());
                if(mc.screen!=null)mc.screen.mouseMoved(x,y);
                }catch(ReflectiveOperationException error){throw new IllegalStateException(error);}
            }
    '''
    return s.replace('case "snapshot-server"',pointer+'\n            case "snapshot-server"',1)

prep.qa_source=source
if __name__=='__main__':prep.main()
