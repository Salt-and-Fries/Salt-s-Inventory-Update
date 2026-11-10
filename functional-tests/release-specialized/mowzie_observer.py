"""Read-only native offer/effect and rendered input-region observations.

No trade, progression, payment, callback or packet implementation is replaced.
The driver uses the real desktop mouse path at the observed extraction regions.
"""


def enhance(text, mc):
    assert mc in ['1.20.1', '1.21.1']
    anchor = 'Object raw=coreCall(window,"containerMenu");if(raw instanceof AbstractContainerMenu menu){'
    assert text.count(anchor) == 1
    observation = r'''
                    if(menu.getClass().getName().startsWith("com.bobmowzie.mowziesmobs.server.inventory.Container")){
                        Object context=coreCall(desktop,"apiContext",window,null,0,0);
                        var regions=new ArrayList<Object>();Object plan=coreCall(definition,"layout");
                        for(Object region:(List<?>)coreCall(plan,"regions")){
                            Object source=coreCall(region,"source");
                            regions.add(Map.of("interactive",coreCall(region,"interactive"),
                                "x",coreCall(definition,"regionX",context,region),"y",coreCall(definition,"regionY",context,region),
                                "source",Map.of("x",coreCall(source,"x"),"y",coreCall(source,"y"),
                                    "width",coreCall(source,"width"),"height",coreCall(source,"height"))));
                        }row.put("mowzieInputRegions",regions);
                    }
'''
    text = text.replace(anchor, anchor + observation, 1)
    anchor = 'case "snapshot-server"'; assert text.count(anchor) == 1
    command = r'''
            case "mowzie-native-state"->work=server(()->{
                var data=new LinkedHashMap<String,Object>();var values=new ArrayList<Object>();
                for(var entity:player().serverLevel().getAllEntities()){
                    String name=entity.getClass().getName();
                    if(!name.startsWith("com.bobmowzie.mowziesmobs.server.entity."))continue;
                    var row=new LinkedHashMap<String,Object>();
                    row.put("type",BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString());
                    for(String method:List.of("getCustomer","isTesting","isTestObstructed","isOfferingTrade"))try{
                        Object value=coreCall(entity,method);
                        row.put(method,value instanceof net.minecraft.world.entity.player.Player p?p.getUUID().equals(player().getUUID()):value);
                    }catch(IllegalArgumentException absent){}
                    if(name.endsWith(".EntitySculptor")||name.endsWith(".EntityUmvuthi")){
                        ItemStack desired=(ItemStack)coreCall(entity,"getDesires");row.put("desired",MOWZIE_SAVE_DESIRED.toString());
                        row.put("desiredMaxCount",desired.getMaxStackSize());
                    }
                    if(name.endsWith(".EntityUmvuthanaMinion")){
                        Object offer=coreCall(entity,"getOfferingTrade");
                        if(offer!=null){
                            ItemStack input=(ItemStack)coreCall(offer,"getInput"),output=(ItemStack)coreCall(offer,"getOutput");
                            row.put("input",MOWZIE_SAVE_INPUT.toString());row.put("output",MOWZIE_SAVE_OUTPUT.toString());
                            row.put("inputMaxCount",input.getMaxStackSize());
                        }
                    }values.add(row);
                }
                var effects=new ArrayList<String>();
                for(var effect:player().getActiveEffects())effects.add(BuiltInRegistries.MOB_EFFECT.getKey(MOWZIE_EFFECT).toString());
                data.put("entities",values);data.put("effects",effects);write("mowzie-native-state.json",data);return true;
            });
'''
    for name in ['desired', 'input', 'output']:
        save = name + ('.save(new net.minecraft.nbt.CompoundTag())' if mc == '1.20.1'
                       else '.saveOptional(player().registryAccess())')
        command = command.replace('MOWZIE_SAVE_' + name.upper(), save)
    command = command.replace('MOWZIE_EFFECT', 'effect.getEffect()' if mc == '1.20.1' else 'effect.getEffect().value()')
    return text.replace(anchor, command + '\n            ' + anchor, 1)
