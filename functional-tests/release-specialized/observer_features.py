"""Read-only extra observations for the native-feature release replays."""


def enhance(text,mc):
    assert mc in ['1.20.1','1.21.1']
    anchor='Object raw=coreCall(window,"containerMenu");if(raw instanceof AbstractContainerMenu menu){'
    assert text.count(anchor)==1
    command=r'''
            case "specialized-shift"->{
                if(!"1".equals(System.getProperty("siu.release.qa.shift.matches")))throw new IllegalStateException("Synthetic Shift input fixture is not installed with its one reviewed native query");
                boolean down=c.get("down").getAsBoolean();System.setProperty("siu.release.qa.shift",Boolean.toString(down));
                boolean observed=com.mojang.blaze3d.platform.InputConstants.isKeyDown(mc.getWindow().getWindow(),340);
                write("specialized-shift-state.json",Map.of("requested",down,"observed",observed,
                    "nativeInputClass",System.getProperty("siu.release.qa.shift.target"),"matchedQueries",1));
                if(observed!=down)throw new IllegalStateException("Synthetic Shift query did not match requested state; physical input may be interfering");
            }
'''
    assert 'case "snapshot-server"'in text
    text=text.replace('case "snapshot-server"',command+'\n            case "snapshot-server"',1)
    extra=r'''
                    if(menu.getClass().getName().equals("earth.terrarium.chipped.common.menus.WorkbenchMenu")){
                        var observed=new LinkedHashMap<String,Object>();
                        Object local=field(window,"apiState");
                        for(String key:List.of("selected","mode","picker","pending","scroll","pickerScroll","eligible"))observed.put(key,G.toJsonTree(field(local,key)));
                        Object search=field(local,"search");observed.put("search",coreCall(search,"getValue"));
                        observed.put("searchFocused",coreCall(search,"isFocused"));
                        var nativeResults=new ArrayList<Object>();
                        for(Object item:(List<?>)coreCall(menu,"results"))nativeResults.add(specializedStack((ItemStack)item));
                        observed.put("results",nativeResults);
                        observed.put("chosen",specializedStack((ItemStack)coreCall(menu,"chosenStack")));
                        row.put("chipped",observed);
                    }
'''
    text=text.replace(anchor,anchor+extra,1)
    save='stack.save(new net.minecraft.nbt.CompoundTag())' if mc=='1.20.1' else 'stack.saveOptional(mc.level.registryAccess())'
    helper='''
    static Map<String,Object> specializedStack(ItemStack stack){
        return Map.of("id",BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(),"count",stack.getCount(),"data",SPECIALIZED_SAVE.toString());
    }
'''.replace('SPECIALIZED_SAVE',save)
    return text[:text.rfind('}')]+helper+'\n}\n'
