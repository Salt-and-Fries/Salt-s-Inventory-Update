"""Disposable observation only; shipping mods and native subject JAR bytes stay untouched."""
from pathlib import Path
import importlib.util
import sys

ROOT=Path(__file__).resolve().parents[2]
sys.path.insert(0,str(ROOT/'tools'))


def enhance(text):
    extra=r'''
            case "aether-character"->{
                try{
                    Object api=Class.forName("com.salts_inventory_update.api.client.desktop.DesktopClientApi");
                    if(mc.screen==null)((Class<?>)api).getMethod("openPlayerWorkspace").invoke(null);
                    Object workspace=((java.util.Optional<?>)((Class<?>)api).getMethod("currentWorkspace").invoke(null)).orElseThrow();
                    Object kind=java.util.Arrays.stream(Class.forName("com.salts_inventory_update.api.client.desktop.DesktopWorkspaceWindow$Kind").getEnumConstants()).filter(v->v.toString().equals("CHARACTER")).findFirst().orElseThrow();
                    coreCall(workspace,"ensureWindow",kind);
                }catch(Exception e){throw new IllegalStateException(e);}
            }
            case "aether-key"->{net.minecraft.client.KeyMapping.click(com.mojang.blaze3d.platform.InputConstants.getKey(num(c,"key",73),0));}
            case "aether-state"->work=server(()->{
                try{
                    var entries=new ArrayList<Object>();
                    Class<?> access=Class.forName("com.salts_inventory_update.compat.aether.AetherAccess");
                    for(Object value:(List<?>)coreCall(access,"equipment",player().getInventory())){
                        Slot slot=(Slot)value;entries.add(Map.of("name",coreCall(access,"identifier",slot),"index",slot.getContainerSlot(),"render",coreCall(access,"render",slot),"item",slot.getItem().save(new net.minecraft.nbt.CompoundTag()).toString()));
                    }
                    var block=player().serverLevel().getBlockEntity(POS);
                    write("aether-state.json",Map.of("equipment",entries,"block",block==null?"none":block.saveWithFullMetadata().toString()));return true;
                }catch(Exception e){throw new IllegalStateException(e);}
            });
'''
    assert 'case "snapshot-server"' in text
    text=text.replace('case "snapshot-server"',extra+'\n            case "snapshot-server"',1)
    anchor='}workspace.add(info);'
    observation=r'''}
                info.put("instance",field(window,"apiInstanceId").toString());
                Object apiContext=coreCall(desktop,"apiContext",window,null,0,0);
                var builtinControls=new ArrayList<Object>();
                for(Object attachment:(List<?>)field(window,"builtinAttachments")){
                    var regions=new ArrayList<Object>();
                    for(Object region:(List<?>)coreCall(attachment,"regions",apiContext))regions.add(Map.of(
                        "x",coreCall(region,"x"),"y",coreCall(region,"y"),"width",coreCall(region,"width"),"height",coreCall(region,"height")));
                    builtinControls.add(Map.of("type",attachment.getClass().getName(),"regions",regions));
                }
                info.put("builtinControls",builtinControls);
                var controllerApi=Class.forName("com.salts_inventory_update.api.client.desktop.DesktopClientApi");
                Object publicWorkspace=((java.util.Optional<?>)controllerApi.getMethod("currentWorkspace").invoke(null)).orElseThrow();
                info.put("controllerTargets",G.toJsonTree(coreCall(publicWorkspace,"controllerTargets",field(window,"apiInstanceId"))));
                info.put("content",Map.of("x",coreCall(apiContext,"contentX"),"y",coreCall(apiContext,"contentY"),"width",coreCall(apiContext,"contentWidth"),"height",coreCall(apiContext,"contentHeight")));
                info.put("recipeBookVisible",coreCall(apiContext,"recipeBookVisible"));
                try{
                    var aether=Class.forName("com.salts_inventory_update.compat.aether.AetherEquipmentClient");
                    java.util.UUID owner=(java.util.UUID)field(window,"apiInstanceId");
                    boolean active=(Boolean)coreCall(aether,"active",owner);info.put("aetherMode",active);
                    if(active){
                        var equipment=(AbstractContainerMenu)coreCall(aether,"menu",owner);
                        info.put("aetherSession",coreCall(aether,"sessionId",owner));
                        info.put("aetherCells",G.toJsonTree(field(equipment,"cells")));
                        var equipmentItems=new ArrayList<Object>();for(Slot s:equipment.slots)equipmentItems.add(Map.of("index",s.index,"item",BuiltInRegistries.ITEM.getKey(s.getItem().getItem()).toString(),"count",s.getItem().getCount()));
                        info.put("aetherSlots",equipmentItems);
                    }
                }catch(ClassNotFoundException absent){}
                try{Object a=field(window,"apiAttachment");if(a!=null)info.put("attachmentOwner",coreCall(a,"owner").toString());}catch(Exception ignored){}
                workspace.add(info);'''
    assert anchor in text;text=text.replace(anchor,observation,1)
    anchor='Object raw=coreCall(window,"containerMenu");if(raw instanceof AbstractContainerMenu menu){'
    extra=r'''
                    if(menu.getClass().getName().endsWith(".AetherEquipmentMenu")){
                        row.put("aetherCells",G.toJsonTree(field(menu,"cells")));
                        row.put("aetherOwner",field(menu,"ownerWindow").toString());
                        row.put("aetherRender",G.toJsonTree(field(field(window,"apiState"),"render")));
                    }
'''
    assert anchor in text;text=text.replace(anchor,anchor+extra,1)
    # Observable native values only; no changes to recipes, timers or equipment.
    anchor='static Map<String,Object> nativeValues(Object screen){'
    assert anchor in text
    # Existing helper includes many unrelated integrations. Add values into the
    # generic result at its existing initialization, scoped to Aether screens.
    pos=text.index(anchor);tail=text[pos:];needle='var values=new LinkedHashMap<String,Object>();'
    assert needle in tail
    insert=r'''
        if(screen instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> aetherScreen && aetherScreen.getMenu().getClass().getName().startsWith("com.aetherteam.aether.")){
            Object menu=aetherScreen.getMenu();
            for(String method:List.of("isLit","getLitProgress","getBurnDuration","getCookingProgress","getIncubationProgressScaled","isIncubating","getIncubationTimeRemaining","getLoreEntryExists"))try{values.put(method,coreCall(menu,method));}catch(Exception absent){}
            for(String fieldName:List.of("currentPageNumber","pages"))try{Object value=field(screen,fieldName);values.put(fieldName,value instanceof Map<?,?> map?map.size():value);}catch(Exception absent){}
        }
'''
    tail=tail.replace(needle,needle+insert,1);return text[:pos]+tail


if __name__=='__main__':
    loader=sys.argv[1];sys.argv.pop(1)
    script='prepare-production-fabric-audit.py' if loader=='fabric' else 'prepare-rollout-production-forge.py'
    spec=importlib.util.spec_from_file_location('aether_capture_prepare',ROOT/'tools'/script)
    prep=importlib.util.module_from_spec(spec);spec.loader.exec_module(prep)
    if loader=='fabric':
        original=prep.qa_source
        prep.qa_source=lambda side:enhance(original(side))
    else:
        original=prep.modern.qa_source
        def source(side):
            metadata,text=original(side);return metadata,enhance(text)
        prep.modern.qa_source=source
    prep.main()
