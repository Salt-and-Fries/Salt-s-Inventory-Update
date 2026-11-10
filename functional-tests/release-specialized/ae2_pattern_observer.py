"""Native pattern fixture creation and separate read-only pattern observations.

The fixture command creates one encoded item in an empty PLAYER inventory
cell before any menu is entered. It never fills a provider, client record,
screen or expected result. The observation command never writes native state.
"""
import re
from ae2_child_observer import enhance as child_observations


def enhance(text,mc,named=False):
    text=child_observations(text,mc,named)
    anchor='if(kind.equals("regression-world"))return true;';assert text.count(anchor)==1
    command=r'''
        if(kind.equals("regression-ae2-pattern-fixture")){
            work=server(()->{
                var level=(net.minecraft.server.level.ServerLevel)h("serverLevel");var host=level.getBlockEntity(POS.offset(-1,0,0));
                if(host==null||!host.getClass().getName().equals("appeng.blockentity.crafting.PatternProviderBlockEntity"))throw new IllegalStateException("Original pattern provider missing");
                Object sessions=call(com.salts_inventory_update.server.DesktopContainerSessions.class,"sessions",player());
                if(!((Map<?,?>)field(sessions,"sessions")).isEmpty())throw new IllegalStateException("Pattern fixture must precede native menu entry");
                Object inventory=call(call(host,"getLogic"),"getPatternInv");
                for(int i=0;i<((Number)call(inventory,"size")).intValue();i++)if(!((ItemStack)call(inventory,"getStackInSlot",i)).isEmpty())throw new IllegalStateException("Provider fixture must start empty");
                if(!player().getInventory().getItem(9).isEmpty())throw new IllegalStateException("Player fixture cell is not empty");
                try{
                    var loader=host.getClass().getClassLoader();Class<?> key=loader.loadClass("appeng.api.stacks.AEItemKey"),base=loader.loadClass("appeng.api.stacks.AEKey");
                    Class<?> generic=loader.loadClass("appeng.api.stacks.GenericStack"),details=loader.loadClass("appeng.api.crafting.PatternDetailsHelper");
                    Object stone=call(key,"of",new ItemStack(net.minecraft.world.item.Items.STONE)),diamond=call(key,"of",new ItemStack(net.minecraft.world.item.Items.DIAMOND));
                    Object input=generic.getConstructor(base,long.class).newInstance(stone,1L),output=generic.getConstructor(base,long.class).newInstance(diamond,1L);
                    ENCODER_ARGUMENTS
                    var encoded=(ItemStack)call(details,"encodeProcessingPattern",inputs,outputs);
                    Object decoded=call(details,"decodePattern",encoded,level);if(decoded==null)throw new IllegalStateException("Native encoder did not produce a valid pattern");
                    Object sourceInputs=call(decoded,"getInputs");if(java.lang.reflect.Array.getLength(sourceInputs)!=1)throw new IllegalStateException("Native pattern input arity");
                    Object sourceInput=java.lang.reflect.Array.get(sourceInputs,0),possible=call(sourceInput,"getPossibleInputs");
                    if(java.lang.reflect.Array.getLength(possible)!=1)throw new IllegalStateException("Native pattern input alternatives");
                    Object primary=call(decoded,"getPrimaryOutput");
                    player().getInventory().setItem(9,encoded);player().inventoryMenu.broadcastFullState();
                    write("release-ae2-pattern-fixture.json",Map.of("item",regressionStack(encoded),"decodedInput",releasePatternIngredient(java.lang.reflect.Array.get(possible,0)),
                        "inputMultiplier",call(sourceInput,"getMultiplier"),"decodedOutput",releasePatternIngredient(primary),
                        "scope","Declared pre-entry player ingredient only; actual provider remains empty until native pointer input"));
                }catch(ReflectiveOperationException error){throw new IllegalStateException(error);}
                return null;
            });return true;
        }
        if(kind.equals("regression-ae2-pattern-state")){
            Object desktop=h("screen");if(desktop==null)throw new IllegalStateException("No actual AE2 desktop");
            var clients=new ArrayList<Object>();
            for(Object window:(List<?>)field(desktop,"windows")){
                if(field(window,"session")==null||field(window,"apiState")==null)continue;
                Object content;try{content=field(field(window,"apiState"),"content");}catch(IllegalArgumentException otherAdapter){continue;}
                if(content==null||!content.getClass().getName().startsWith("appeng.client.gui."))continue;
                var menu=(AbstractContainerMenu)call(content,"getMenu");String simple=menu.getClass().getSimpleName();
                if(!simple.equals("PatternProviderMenu")&&!simple.equals("PatternAccessTermMenu"))continue;
                var row=new LinkedHashMap<String,Object>();row.put("session",call(field(window,"session"),"sessionId"));row.put("menu",menu.getClass().getName());
                row.put("left",call(content,"getGuiLeft"));row.put("top",call(content,"getGuiTop"));
                var slots=new ArrayList<Object>();
                for(int i=0;i<menu.slots.size();i++){
                    var slot=menu.slots.get(i);String type=slot.getClass().getName();
                    if(!type.equals("appeng.client.gui.me.patternaccess.PatternSlot")&&!type.equals("appeng.menu.slot.RestrictedInputSlot"))continue;
                    var cell=new LinkedHashMap<String,Object>();cell.put("menuIndex",i);cell.put("type",type);cell.put("x",slot.x);cell.put("y",slot.y);
                    cell.put("containerIndex",slot.getContainerSlot());cell.put("item",regressionStack(slot.getItem()));cell.put("visible",call(content,"desktopSlotVisible",slot));
                    if(simple.equals("PatternAccessTermMenu"))cell.put("serverId",call(call(slot,"getMachineInv"),"getServerId"));slots.add(cell);
                }
                row.put("slots",slots);
                var settings=new ArrayList<Object>();
                for(Object widget:(Iterable<?>)call(content,"children"))for(Class<?> type=widget.getClass();type!=null;type=type.getSuperclass()){
                    if(!type.getName().equals("appeng.client.gui.widgets.SettingToggleButton"))continue;
                    var button=(net.minecraft.client.gui.components.AbstractWidget)widget;
                    settings.add(Map.of("x",button.getX(),"y",button.getY(),"width",button.getWidth(),"height",button.getHeight(),
                        "visible",button.visible,"active",button.active,"currentValue",call(widget,"getCurrentValue").toString()));break;
                }
                row.put("settings",settings);
                if(simple.equals("PatternAccessTermMenu")){
                    var records=new ArrayList<Object>();
                    for(Object record:((Map<?,?>)field(content,"byId")).values())records.add(Map.of("serverId",call(record,"getServerId"),
                        "searchName",call(record,"getSearchName"),"items",releasePatternInventory(call(record,"getInventory"))));
                    row.put("records",records);row.put("rows",((List<?>)field(content,"rows")).size());row.put("visibleRows",field(content,"visibleRows"));
                    row.put("shownProviders",call(menu,"getShownProviders").toString());row.put("search",call(field(content,"searchField"),"getValue"));
                }
                clients.add(row);
            }
            work=server(()->{
                var level=(net.minecraft.server.level.ServerLevel)h("serverLevel");var host=level.getBlockEntity(POS.offset(-1,0,0));
                if(host==null||!host.getClass().getName().equals("appeng.blockentity.crafting.PatternProviderBlockEntity"))throw new IllegalStateException("Native provider disappeared");
                Object state=call(com.salts_inventory_update.server.DesktopContainerSessions.class,"sessions",player());var servers=new ArrayList<Object>();
                for(var entry:((Map<?,?>)field(state,"sessions")).entrySet()){
                    Object menu=field(entry.getValue(),"menu");if(!menu.getClass().getSimpleName().equals("PatternAccessTermMenu"))continue;
                    var records=new ArrayList<Object>();for(Object tracker:((Map<?,?>)field(menu,"byId")).values())records.add(Map.of("serverId",field(tracker,"serverId"),"items",releasePatternInventory(field(tracker,"server"))));
                    servers.add(Map.of("session",entry.getKey(),"menu",menu.getClass().getName(),"records",records,"shownProviders",call(menu,"getShownProviders").toString()));
                }
                write("release-ae2-pattern-state.json",Map.of("client",clients,"server",servers,
                    "provider",releasePatternInventory(call(call(host,"getLogic"),"getPatternInv")),"providerVisible",call(host,"isVisibleInTerminal")));return null;
            });return true;
        }
'''
    helpers=r'''
    static List<Object> releasePatternInventory(Object inventory){
        var result=new ArrayList<Object>();for(int i=0;i<((Number)call(inventory,"size")).intValue();i++)result.add(regressionStack((ItemStack)call(inventory,"getStackInSlot",i)));return result;
    }
    static Map<String,Object> releasePatternIngredient(Object stack){
        Object what=call(stack,"what"),amount=call(stack,"amount");return Map.of("id",call(what,"getId").toString(),"amount",amount,"javaType",amount.getClass().getName());
    }
'''
    args='Object inputs=List.of(input),outputs=List.of(output);'
    if mc=='1.20.1':args='Object inputs=java.lang.reflect.Array.newInstance(generic,1),outputs=java.lang.reflect.Array.newInstance(generic,1);java.lang.reflect.Array.set(inputs,0,input);java.lang.reflect.Array.set(outputs,0,output);'
    command=command.replace('ENCODER_ARGUMENTS',args)
    if named:command=re.sub(r'\bPOS\b','origin()',command)
    else:
        command=re.sub(r'\bh\(','regressionH(',command)
        command=re.sub(r'\bcall\(','coreCall(',command);helpers=re.sub(r'\bcall\(','coreCall(',helpers)
    text=text.replace(anchor,anchor+command,1)
    return text[:text.rfind('}')]+helpers+'\n}\n'
