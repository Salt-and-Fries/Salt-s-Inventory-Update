"""Read-only XP/accounting probe for the native Archon pouch scenario."""


def enhance(text, mc):
    assert mc == '1.20.1'
    values_anchor='static Map<String,Object> nativeValues(Object screen){\n        var values=new LinkedHashMap<String,Object>();'
    assert text.count(values_anchor)==1
    readout=r'''
        if(screen.getClass().getName().equals("safro.archon.client.screen.ExperiencePouchScreen")){
            var model=((net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?>)screen).getMenu();
            values.put("getExperience",coreCall(model,"getExperience"));
            values.put("getMaxExperience",coreCall(model,"getMaxExperience"));
        }
'''
    text=text.replace(values_anchor,values_anchor+readout,1)
    anchor = 'case "snapshot-server"'; assert text.count(anchor) == 1
    command = r'''
        case "pouch-resource-state"->work=server(()->{
            try{
                var data=new LinkedHashMap<String,Object>();
                data.put("nativePlayerXp",coreCall(Class.forName("safro.archon.util.XpUtil"),"getPlayerXP",player()));
                data.put("level",player().experienceLevel);data.put("progress",player().experienceProgress);
                var held=player().getMainHandItem();data.put("held",held.save(new net.minecraft.nbt.CompoundTag()).toString());
                if(held.getItem().getClass().getName().equals("safro.archon.item.ExperiencePouchItem"))
                    data.put("capacity",coreCall(held.getItem(),"getMaxXp"));
                write("pouch-resource-state.json",data);return true;
            }catch(ReflectiveOperationException failure){throw new IllegalStateException(failure);}
        });
'''
    return text.replace(anchor, command + '\n' + anchor, 1)
