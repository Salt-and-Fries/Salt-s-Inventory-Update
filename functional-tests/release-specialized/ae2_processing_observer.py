"""Read the actual processing-pattern parent/local child and native host config.

No screen creation, setter, callback, inventory write or manufactured pattern.
"""
import re
from ae2_child_observer import enhance as child_observations


def enhance(text,mc,named=False):
    text=child_observations(text,mc,named)
    anchor='if(kind.equals("regression-world"))return true;';assert text.count(anchor)==1
    command=r'''
        if(kind.equals("regression-ae2-processing-state")){
            Object desktop=h("screen");if(desktop==null)throw new IllegalStateException("No native processing desktop");
            var clients=new ArrayList<Object>();
            for(Object window:(List<?>)field(desktop,"windows")){
                if(field(window,"session")==null||field(window,"apiState")==null)continue;
                Object content;try{content=field(field(window,"apiState"),"content");}catch(IllegalArgumentException otherAdapter){continue;}
                if(content==null||!content.getClass().getName().startsWith("appeng.client.gui."))continue;
                Object menu=call(content,"getMenu");if(!menu.getClass().getName().equals("appeng.menu.me.items.PatternEncodingTermMenu"))continue;
                String screen=content.getClass().getName();boolean child=screen.equals("appeng.client.gui.me.items.SetProcessingPatternAmountScreen");
                Object parent=child?call(content,"getParent"):content;
                if(!parent.getClass().getName().equals("appeng.client.gui.me.items.PatternEncodingTermScreen"))throw new IllegalStateException("Unexpected original processing parent");
                var row=new LinkedHashMap<String,Object>();row.put("session",call(field(window,"session"),"sessionId"));row.put("screen",screen);
                row.put("sameParentMenu",call(parent,"getMenu")==menu);row.put("mode",call(menu,"getMode").toString());
                row.put("left",call(content,"getGuiLeft"));row.put("top",call(content,"getGuiTop"));
                row.put("inputs",releaseProcessingSlots(content,menu,call(menu,"getProcessingInputSlots")));
                row.put("outputs",releaseProcessingSlots(content,menu,call(menu,"getProcessingOutputSlots")));
                row.put("blank",releaseProcessingSlot(content,(AbstractContainerMenu)menu,field(menu,"blankPatternSlot")));
                row.put("encoded",releaseProcessingSlot(content,(AbstractContainerMenu)menu,field(menu,"encodedPatternSlot")));
                if(child)row.put("current",releaseProcessingStack(field(content,"currentStack")));
                var modes=new ArrayList<Object>();
                for(var entry:((Map<?,?>)field(parent,"modeTabButtons")).entrySet()){
                    var button=(net.minecraft.client.gui.components.AbstractWidget)entry.getValue();
                    modes.add(Map.of("mode",entry.getKey().toString(),"x",button.getX(),"y",button.getY(),"width",button.getWidth(),"height",button.getHeight(),"visible",button.visible,"active",button.active));
                }
                row.put("modes",modes);clients.add(row);
            }
            work=server(()->{
                Object state=call(com.salts_inventory_update.server.DesktopContainerSessions.class,"sessions",player());var servers=new ArrayList<Object>();
                for(var entry:((Map<?,?>)field(state,"sessions")).entrySet()){
                    Object menu=field(entry.getValue(),"menu");if(!menu.getClass().getName().equals("appeng.menu.me.items.PatternEncodingTermMenu"))continue;
                    Object logic=field(menu,"encodingLogic");ItemStack encoded=((net.minecraft.world.inventory.Slot)field(menu,"encodedPatternSlot")).getItem();
                    Object decoded=null;
                    if(!encoded.isEmpty())try{decoded=call(menu.getClass().getClassLoader().loadClass("appeng.api.crafting.PatternDetailsHelper"),"decodePattern",encoded,(net.minecraft.server.level.ServerLevel)h("serverLevel"));}
                    catch(ClassNotFoundException error){throw new IllegalStateException(error);}
                    var row=new LinkedHashMap<String,Object>();row.put("session",entry.getKey());row.put("mode",call(logic,"getMode").toString());
                    row.put("inputs",releaseProcessingInventory(call(logic,"getEncodedInputInv")));row.put("outputs",releaseProcessingInventory(call(logic,"getEncodedOutputInv")));
                    row.put("blank",regressionStack(((net.minecraft.world.inventory.Slot)field(menu,"blankPatternSlot")).getItem()));row.put("encoded",regressionStack(encoded));
                    if(decoded!=null){
                        var inputs=new ArrayList<Object>();Object ingredients=call(decoded,"getInputs");
                        for(int i=0;i<java.lang.reflect.Array.getLength(ingredients);i++){
                            Object ingredient=java.lang.reflect.Array.get(ingredients,i),possible=call(ingredient,"getPossibleInputs");var alternatives=new ArrayList<Object>();
                            for(int j=0;j<java.lang.reflect.Array.getLength(possible);j++)alternatives.add(releaseProcessingStack(java.lang.reflect.Array.get(possible,j)));
                            inputs.add(Map.of("alternatives",alternatives,"multiplier",call(ingredient,"getMultiplier")));
                        }
                        Object outputs=call(decoded,"getOutputs");var result=new ArrayList<Object>();
                        for(int i=0;i<java.lang.reflect.Array.getLength(outputs);i++)result.add(releaseProcessingStack(java.lang.reflect.Array.get(outputs,i)));
                        row.put("decoded",Map.of("type",decoded.getClass().getName(),"inputs",inputs,"outputs",result));
                    }
                    servers.add(row);
                }
                write("release-ae2-processing-state.json",Map.of("client",clients,"server",servers));return null;
            });return true;
        }
'''
    helpers=r'''
    static Map<String,Object> releaseProcessingStack(Object stack){
        if(stack==null)return Map.of("empty",true);Object amount=call(stack,"amount");
        return Map.of("empty",false,"item",regressionStack((ItemStack)call(call(stack,"what"),"toStack")),"amount",amount,"amountJavaType",amount.getClass().getName());
    }
    static List<Object> releaseProcessingInventory(Object inventory){
        var result=new ArrayList<Object>();for(int i=0;i<((Number)call(inventory,"size")).intValue();i++)result.add(releaseProcessingStack(call(inventory,"getStack",i)));return result;
    }
    static Map<String,Object> releaseProcessingSlot(Object screen,AbstractContainerMenu menu,Object value){
        var slot=(net.minecraft.world.inventory.Slot)value;var row=new LinkedHashMap<String,Object>();
        row.put("index",menu.slots.indexOf(slot));row.put("containerIndex",slot.getContainerSlot());row.put("type",slot.getClass().getName());
        row.put("x",slot.x);row.put("y",slot.y);row.put("visible",call(screen,"desktopSlotVisible",slot));row.put("item",regressionStack(slot.getItem()));
        try{row.put("decoded",releaseProcessingStack(call(screen.getClass().getClassLoader().loadClass("appeng.api.stacks.GenericStack"),"fromItemStack",slot.getItem())));}
        catch(ClassNotFoundException error){throw new IllegalStateException(error);}return row;
    }
    static List<Object> releaseProcessingSlots(Object screen,Object menu,Object slots){
        var result=new ArrayList<Object>();for(int i=0;i<java.lang.reflect.Array.getLength(slots);i++)result.add(releaseProcessingSlot(screen,(AbstractContainerMenu)menu,java.lang.reflect.Array.get(slots,i)));return result;
    }
'''
    if not named:
        command=re.sub(r'\bh\(','regressionH(',command)
        command=re.sub(r'\bcall\(','coreCall(',command);helpers=re.sub(r'\bcall\(','coreCall(',helpers)
    text=text.replace(anchor,anchor+command,1)
    return text[:text.rfind('}')]+helpers+'\n}\n'
