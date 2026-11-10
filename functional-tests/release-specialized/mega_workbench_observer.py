"""Read the actual portable-workbench menus, component contents and controls."""


def enhance(text,mc,named=False):
    assert mc=='1.21.1'and named,'Only the exact supported MEGA4.11 publisher is reviewed'
    anchor='if(kind.equals("regression-world"))return true;';assert text.count(anchor)==1
    command=r'''
        if(kind.equals("regression-mega-workbench-state")){
            var clients=new ArrayList<Object>();Object desktop=h("screen");if(desktop==null)throw new IllegalStateException("No native workbench desktop");
            for(Object window:(List<?>)field(desktop,"windows")){
                if(field(window,"session")==null||field(window,"apiState")==null)continue;
                Object content;try{content=field(field(window,"apiState"),"content");}catch(IllegalArgumentException otherAdapter){continue;}
                if(content==null||!content.getClass().getName().equals("gripe._90.megacells.client.screen.PortableCellWorkbenchScreen"))continue;
                var menu=(AbstractContainerMenu)call(content,"getMenu");var row=new LinkedHashMap<String,Object>();
                row.put("session",call(field(window,"session"),"sessionId"));row.put("screen",content.getClass().getName());row.put("menu",menu.getClass().getName());
                row.put("bounds",Map.of("x",field(window,"x"),"y",field(window,"y"),"width",field(window,"width"),"height",field(window,"height")));row.put("locked",field(window,"locked"));
                row.put("left",call(content,"getGuiLeft"));row.put("top",call(content,"getGuiTop"));row.put("native",releaseMegaWorkbench(menu));
                var widgets=new ArrayList<Object>();
                for(Object value:(Iterable<?>)call(content,"children")){
                    if(!(value instanceof net.minecraft.client.gui.components.AbstractWidget widget))continue;
                    var control=new LinkedHashMap<String,Object>();control.put("type",value.getClass().getName());control.put("message",widget.getMessage().getString());
                    control.put("x",widget.getX());control.put("y",widget.getY());control.put("width",widget.getWidth());control.put("height",widget.getHeight());control.put("visible",widget.visible);control.put("active",widget.active);
                    for(Class<?> type=value.getClass();type!=null;type=type.getSuperclass())if(type.getName().equals("appeng.client.gui.widgets.IconButton")){
                        Object icon=call(value,"getIcon");
                        if(icon!=null)control.put("icon",icon.toString());
                        else if(value.getClass().getName().equals("gripe._90.megacells.client.screen.CompressionCutoffButton"))control.put("itemIcon",regressionStack((ItemStack)field(value,"item")));
                        else throw new IllegalStateException("Unexpected native null workbench icon: "+value.getClass().getName());
                        var tips=new ArrayList<String>();for(Object line:(Iterable<?>)call(value,"getTooltipMessage"))tips.add(((net.minecraft.network.chat.Component)line).getString());control.put("tooltips",tips);break;
                    }
                    if(value.getClass().getName().equals("appeng.client.gui.widgets.ToggleButton"))control.put("selected",field(value,"state"));
                    if(value.getClass().getName().equals("appeng.client.gui.widgets.SettingToggleButton"))control.put("currentValue",call(value,"getCurrentValue").toString());
                    widgets.add(control);
                }
                row.put("widgets",widgets);var slots=new ArrayList<Object>();
                for(Object value:(Iterable<?>)call(content,"desktopSlots")){
                    var slot=(net.minecraft.world.inventory.Slot)value;if(slot.container instanceof net.minecraft.world.entity.player.Inventory)continue;
                    slots.add(Map.of("index",menu.slots.indexOf(slot),"containerIndex",slot.getContainerSlot(),"type",slot.getClass().getName(),"x",slot.x,"y",slot.y,"visible",call(content,"desktopSlotVisible",slot),"active",slot.isActive(),"item",regressionStack(slot.getItem())));
                }
                row.put("slots",slots);clients.add(row);
            }
            if(clients.size()!=1)throw new IllegalStateException("Expected one actual portable workbench");
            work=server(()->{
                var servers=new ArrayList<Object>();Object state=call(com.salts_inventory_update.server.DesktopContainerSessions.class,"sessions",player());
                for(var entry:((Map<?,?>)field(state,"sessions")).entrySet()){
                    Object menu=field(entry.getValue(),"menu");if(!menu.getClass().getName().equals("gripe._90.megacells.menu.PortableCellWorkbenchMenu"))continue;
                    servers.add(Map.of("session",entry.getKey(),"menu",menu.getClass().getName(),"native",releaseMegaWorkbench(menu)));
                }
                write("release-mega-workbench-state.json",Map.of("client",clients,"server",servers));return null;
            });return true;
        }
'''
    helpers=r'''
    static Object releaseMegaGetter(Object value,String name){
        try{var method=value.getClass().getMethod(name);method.setAccessible(true);return method.invoke(value);}
        catch(ReflectiveOperationException error){throw new IllegalStateException("Could not read native workbench getter "+name,error);}
    }
    static Map<String,Object> releaseMegaGeneric(Object value){
        if(value==null)return Map.of("empty",true);Object key=call(value,"what"),amount=call(value,"amount");
        return Map.of("empty",false,"item",regressionStack((ItemStack)call(key,"toStack")),"amount",amount,"amountJavaType",amount.getClass().getName());
    }
    static List<Object> releaseMegaConfig(Object inventory){
        var values=new ArrayList<Object>();for(int i=0;i<((Number)call(inventory,"size")).intValue();i++)values.add(releaseMegaGeneric(call(inventory,"getStack",i)));return values;
    }
    static Map<String,Object> releaseMegaWorkbench(Object menu){
        try{
        Object host=call(menu,"getHost");ItemStack source=(ItemStack)call(host,"getItemStack"),cell=(ItemStack)call(menu,"getWorkbenchItem");
        var result=new LinkedHashMap<String,Object>();result.put("source",regressionStack(source));result.put("sourceSlot",call(host,"getPlayerInventorySlot"));result.put("cell",regressionStack(cell));
        result.put("sourceMutable",releaseMegaMutable(source,List.of("minecraft:container","ae2:exported_config_inv","ae2:exported_settings")));
        result.put("cellMutable",releaseMegaMutable(cell,List.of("ae2:storage_cell_config_inv","ae2:storage_cell_fuzzy_mode","ae2:upgrades")));
        result.put("copyMode",call(menu,"getCopyMode").toString());result.put("fuzzyMode",call(menu,"getFuzzyMode").toString());result.put("config",releaseMegaConfig(call(menu,"getConfigInventory")));
        var upgrades=new ArrayList<Object>();Object inv=call(menu,"getUpgrades");for(int i=0;i<((Number)releaseMegaGetter(inv,"size")).intValue();i++)upgrades.add(regressionStack((ItemStack)call(inv,"getStackInSlot",i)));result.put("upgrades",upgrades);
        Object contained=source.get(net.minecraft.core.component.DataComponents.CONTAINER);var items=new ArrayList<Object>();if(contained!=null)for(Object item:((java.util.stream.Stream<?>)call(contained,"stream")).toList())items.add(regressionStack((ItemStack)item));result.put("sourceContained",items);
        var cfgType=(net.minecraft.core.component.DataComponentType)Class.forName("appeng.api.ids.AEComponents").getField("EXPORTED_CONFIG_INV").get(null);
        var sourceConfig=new ArrayList<Object>();Object encoded=source.get(cfgType);if(encoded!=null)for(Object value:(List<?>)encoded)sourceConfig.add(releaseMegaGeneric(value));result.put("sourceConfig",sourceConfig);
        var settingType=(net.minecraft.core.component.DataComponentType)Class.forName("appeng.api.ids.AEComponents").getField("EXPORTED_SETTINGS").get(null);
        result.put("sourceSettings",source.getOrDefault(settingType,Map.of()));
        if(cell.isEmpty()){result.put("cellConfig",List.of());result.put("cellUpgrades",List.of());result.put("cellFuzzy","none");}
        else{
            Object nativeCell=call(host,"getCell");result.put("cellConfig",releaseMegaConfig(call(nativeCell,"getConfigInventory",cell)));result.put("cellFuzzy",call(nativeCell,"getFuzzyMode",cell).toString());
            Object actual=call(nativeCell,"getUpgrades",cell);var values=new ArrayList<Object>();for(int i=0;i<((Number)releaseMegaGetter(actual,"size")).intValue();i++)values.add(regressionStack((ItemStack)call(actual,"getStackInSlot",i)));result.put("cellUpgrades",values);
        }
        return result;
        }catch(ReflectiveOperationException error){throw new IllegalStateException("Could not read actual workbench components",error);}
    }
    static Map<String,String> releaseMegaMutable(ItemStack stack,List<String> names){
        var result=new LinkedHashMap<String,String>();var ops=net.minecraft.resources.RegistryOps.create(net.minecraft.nbt.NbtOps.INSTANCE,player().registryAccess());
        for(var entry:stack.getComponentsPatch().entrySet()){
            String name=net.minecraft.core.registries.BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(entry.getKey()).toString();if(!names.contains(name))continue;
            if(entry.getValue().isEmpty())throw new IllegalStateException("Unexpected removed native workbench component "+name);
            com.mojang.serialization.Codec codec=entry.getKey().codec();if(codec==null)throw new IllegalStateException("No persistent workbench component codec");
            Object encoded=codec.encodeStart(ops,entry.getValue().get()).getOrThrow();result.put(name,encoded.toString());
        }
        return result;
    }
'''
    text=text.replace(anchor,anchor+command,1)
    return text[:text.rfind('}')]+helpers+'\n}\n'
