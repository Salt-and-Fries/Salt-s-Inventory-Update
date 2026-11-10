"""Declared player-only recipe ingredient plus pure native crafting observations."""
import re
from ae2_child_observer import enhance as child_observations


def enhance(text,mc,named=False):
    text=child_observations(text,mc,named)
    anchor='if(kind.equals("regression-world"))return true;';assert text.count(anchor)==1
    command=r'''
        if(kind.equals("regression-ae2-crafting-fixture")){
            work=server(()->{
                var level=(net.minecraft.server.level.ServerLevel)h("serverLevel");
                Object sessions=call(com.salts_inventory_update.server.DesktopContainerSessions.class,"sessions",player());
                if(!((Map<?,?>)field(sessions,"sessions")).isEmpty()||!player().getInventory().getItem(9).isEmpty())throw new IllegalStateException("Crafting ingredient must precede all menus in empty player9");
                Object provider=level.getBlockEntity(POS.offset(1,0,0));
                if(provider==null||!provider.getClass().getName().equals("appeng.blockentity.crafting.PatternProviderBlockEntity"))throw new IllegalStateException("Original provider missing");
                Object inventory=call(call(provider,"getLogic"),"getPatternInv");
                for(int i=0;i<((Number)call(inventory,"size")).intValue();i++)if(!((ItemStack)call(inventory,"getStackInSlot",i)).isEmpty())throw new IllegalStateException("Fixture must not populate provider");
                try{
                    var loader=provider.getClass().getClassLoader();Class<?> helper=loader.loadClass("appeng.server.testplots.CraftingPatternHelper");
                    var pattern=(ItemStack)call(helper,"encodeShapelessCraftingRecipe",level,(Object)new ItemStack[]{new ItemStack(net.minecraft.world.item.Items.OAK_LOG)});
                    Object decoded=call(loader.loadClass("appeng.api.crafting.PatternDetailsHelper"),"decodePattern",pattern,level);
                    if(decoded==null)throw new IllegalStateException("Native world recipe did not encode");
                    player().getInventory().setItem(9,pattern);player().inventoryMenu.broadcastFullState();
                    write("release-ae2-crafting-fixture.json",Map.of("pattern",regressionStack(pattern),"decoded",releaseCraftingPattern(decoded),"scope","Native world recipe creates one PLAYER ingredient only. Machines and storage remain empty until real pointer input."));
                }catch(ClassNotFoundException error){throw new IllegalStateException(error);}return null;
            });return true;
        }
        if(kind.equals("regression-ae2-crafting-state")){
            Object desktop=h("screen");if(desktop==null)throw new IllegalStateException("No original crafting desktop");var clients=new ArrayList<Object>();
            for(Object window:(List<?>)field(desktop,"windows")){
                if(field(window,"session")==null||field(window,"apiState")==null)continue;
                Object content;try{content=field(field(window,"apiState"),"content");}catch(IllegalArgumentException otherAdapter){continue;}
                if(content==null||!content.getClass().getName().startsWith("appeng.client.gui."))continue;
                var menu=(AbstractContainerMenu)call(content,"getMenu");var row=new LinkedHashMap<String,Object>();
                row.put("session",call(field(window,"session"),"sessionId"));row.put("screen",content.getClass().getName());row.put("menu",menu.getClass().getName());
                row.put("left",call(content,"getGuiLeft"));row.put("top",call(content,"getGuiTop"));row.put("slots",releaseCraftingSlots(content,menu));
                if(menu.getClass().getSimpleName().equals("CraftConfirmMenu")){
                    row.put("summary",releaseCraftingSummary(call(menu,"getPlan")));row.put("error",releaseCraftingError(menu));
                    if(content.getClass().getName().equals("appeng.client.gui.me.crafting.CraftErrorScreen"))row.put("sameParentMenu",call(call(content,"getParent"),"getMenu")==menu);
                }
                if(menu.getClass().getSimpleName().equals("CraftAmountMenu"))row.put("what",releaseCraftingStack(call(menu,"getWhatToCraft")));
                clients.add(row);
            }
            work=server(()->{
                var level=(net.minecraft.server.level.ServerLevel)h("serverLevel");Object provider=level.getBlockEntity(POS.offset(1,0,0)),drive=level.getBlockEntity(POS.offset(-1,0,0));
                if(provider==null||drive==null)throw new IllegalStateException("Original crafting world sources missing");
                Object grid=releaseCraftingPublic(call(provider,"getMainNode"),"getGrid");if(grid==null)throw new IllegalStateException("Native crafting grid missing");
                Object state=call(com.salts_inventory_update.server.DesktopContainerSessions.class,"sessions",player());var servers=new ArrayList<Object>();var mirrors=new ArrayList<Object>();
                for(var entry:((Map<?,?>)field(state,"sessions")).entrySet()){
                    var menu=(AbstractContainerMenu)field(entry.getValue(),"menu");String simple=menu.getClass().getSimpleName();var row=new LinkedHashMap<String,Object>();
                    row.put("session",entry.getKey());row.put("menu",menu.getClass().getName());
                    if(simple.equals("CraftConfirmMenu")){
                        row.put("summary",releaseCraftingSummary(call(menu,"getPlan")));row.put("plan",releaseCraftingPlan(field(menu,"result")));row.put("error",releaseCraftingError(menu));
                    }
                    if(simple.equals("CraftAmountMenu")){
                        row.put("what",releaseCraftingStack(call(menu,"getWhatToCraft")));
                        var slot=(net.minecraft.world.inventory.Slot)field(menu,"craftingItem");
                        mirrors.add(Map.of("session",entry.getKey(),"slot",menu.slots.indexOf(slot),"type",slot.getClass().getName(),"raw",regressionStack(slot.getItem()),"mayPlace",slot.mayPlace(new ItemStack(net.minecraft.world.item.Items.STONE)),"mayPickup",slot.mayPickup(player())));
                    }
                    servers.add(row);
                }
                var cpus=new ArrayList<Object>();for(Object cpu:(Iterable<?>)call(releaseCraftingPublic(grid,"getCraftingService"),"getCpus"))cpus.add(Map.of("busy",call(cpu,"isBusy"),"active",call(cpu,"getJobStatus")!=null,"bytes",call(cpu,"getAvailableStorage"),"coProcessors",call(cpu,"getCoProcessors")));
                Object storage=call(releaseCraftingPublic(grid,"getStorageService"),"getInventory");
                write("release-ae2-crafting-state.json",Map.of("client",clients,"server",servers,"mirrors",mirrors,
                    "provider",releaseCraftingInventory(call(call(provider,"getLogic"),"getPatternInv")),"drive",releaseCraftingInventory(call(drive,"getInternalInventory")),
                    "stored",releaseCraftingStorage(storage),"powered",call(drive,"isPowered"),"cpus",cpus));return null;
            });return true;
        }
'''
    helpers=r'''
    static Object releaseCraftingPublic(Object receiver,String name){
        try{var method=receiver.getClass().getMethod(name);method.setAccessible(true);return method.invoke(receiver);}
        catch(ReflectiveOperationException error){throw new IllegalStateException("Native public getter "+name,error);}
    }
    static Map<String,Object> releaseCraftingStack(Object stack){
        if(stack==null)return Map.of("empty",true);Object amount=call(stack,"amount");
        return Map.of("empty",false,"item",regressionStack((ItemStack)call(call(stack,"what"),"toStack")),"amount",amount,"amountJavaType",amount.getClass().getName());
    }
    static List<Object> releaseCraftingInventory(Object inventory){
        var result=new ArrayList<Object>();for(int i=0;i<((Number)call(inventory,"size")).intValue();i++)result.add(regressionStack((ItemStack)call(inventory,"getStackInSlot",i)));return result;
    }
    static List<Object> releaseCraftingCounter(Object counter){
        var result=new ArrayList<Object>();for(Object entry:(Iterable<?>)counter){Object key=call(entry,"getKey"),amount=call(entry,"getLongValue");
            result.add(Map.of("item",regressionStack((ItemStack)call(key,"toStack")),"amount",amount,"amountJavaType",amount.getClass().getName()));}return result;
    }
    static List<Object> releaseCraftingStorage(Object storage){
        try{var read=storage.getClass().getMethod("getAvailableStacks");read.setAccessible(true);return releaseCraftingCounter(read.invoke(storage));}
        catch(ReflectiveOperationException error){throw new IllegalStateException(error);}
    }
    static Map<String,Object> releaseCraftingPattern(Object decoded){
        Object ingredients=call(decoded,"getInputs"),outputs=call(decoded,"getOutputs");var inputs=new ArrayList<Object>();var results=new ArrayList<Object>();
        for(int i=0;i<java.lang.reflect.Array.getLength(ingredients);i++){
            Object ingredient=java.lang.reflect.Array.get(ingredients,i),possible=call(ingredient,"getPossibleInputs");var alternatives=new ArrayList<Object>();
            for(int j=0;j<java.lang.reflect.Array.getLength(possible);j++)alternatives.add(releaseCraftingStack(java.lang.reflect.Array.get(possible,j)));
            inputs.add(Map.of("alternatives",alternatives,"multiplier",call(ingredient,"getMultiplier")));
        }
        for(int i=0;i<java.lang.reflect.Array.getLength(outputs);i++)results.add(releaseCraftingStack(java.lang.reflect.Array.get(outputs,i)));
        return Map.of("type",decoded.getClass().getName(),"inputs",inputs,"outputs",results);
    }
    static Map<String,Object> releaseCraftingSummary(Object summary){
        if(summary==null)return Map.of("ready",false);var entries=new ArrayList<Object>();
        for(Object entry:(Iterable<?>)call(summary,"getEntries"))entries.add(Map.of("item",regressionStack((ItemStack)call(call(entry,"getWhat"),"toStack")),"missing",call(entry,"getMissingAmount"),"stored",call(entry,"getStoredAmount"),"craft",call(entry,"getCraftAmount")));
        return Map.of("ready",true,"bytes",call(summary,"getUsedBytes"),"simulation",call(summary,"isSimulation"),"entries",entries);
    }
    static Map<String,Object> releaseCraftingPlan(Object plan){
        if(plan==null)return Map.of("ready",false);var patterns=new ArrayList<Object>();
        for(var entry:((Map<?,?>)call(plan,"patternTimes")).entrySet())patterns.add(Map.of("pattern",releaseCraftingPattern(entry.getKey()),"times",entry.getValue()));
        return Map.of("ready",true,"output",releaseCraftingStack(call(plan,"finalOutput")),"bytes",call(plan,"bytes"),"simulation",call(plan,"simulation"),
            "used",releaseCraftingCounter(call(plan,"usedItems")),"missing",releaseCraftingCounter(call(plan,"missingItems")),"emitted",releaseCraftingCounter(call(plan,"emittedItems")),"patterns",patterns);
    }
    static String releaseCraftingError(Object menu){
        Object result=call(field(menu,"submitError"),"result");if(result==null)return "none";Object code=call(result,"errorCode");return code==null?"none":code.toString();
    }
    static List<Object> releaseCraftingSlots(Object content,AbstractContainerMenu menu){
        var result=new ArrayList<Object>();
        for(Object value:(Iterable<?>)call(content,"desktopSlots")){
            var slot=(net.minecraft.world.inventory.Slot)value;var row=new LinkedHashMap<String,Object>();row.put("type",slot.getClass().getName());
            row.put("index",menu.slots.indexOf(slot));row.put("containerIndex",slot.getContainerSlot());row.put("x",slot.x);row.put("y",slot.y);row.put("visible",call(content,"desktopSlotVisible",slot));row.put("item",regressionStack(slot.getItem()));
            if(slot.getClass().getName().equals("appeng.client.gui.me.common.RepoSlot"))row.put("craftable",call(slot,"isCraftable"));result.add(row);
        }return result;
    }
'''
    if named:command=re.sub(r'\bPOS\b','origin()',command)
    else:
        command=re.sub(r'\bh\(','regressionH(',command)
        command=re.sub(r'\bcall\(','coreCall(',command);helpers=re.sub(r'\bcall\(','coreCall(',helpers)
    text=text.replace(anchor,anchor+command,1)
    return text[:text.rfind('}')]+helpers+'\n}\n'
