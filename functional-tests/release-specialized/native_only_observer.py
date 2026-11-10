"""Observe native screens and issue ordinary native screen input only.

No target Screen/MenuProvider is constructed. World/player setup belongs to
explicit operator commands in the driver; these input hooks call the actual
current Screen, because the older desktop-only mouse helper cannot target it.
"""
import re
from native_item_serialization import storage_item_saves


def enhance(text,mc,named=False):
    assert mc in ['1.20.1','1.21.1']
    anchor='if(kind.equals("regression-world"))return true;';assert text.count(anchor)==1
    command=r'''
        if(kind.equals("regression-native-only-state")){
            var client=new LinkedHashMap<String,Object>();var widgets=new ArrayList<Object>();
            client.put("screen",mc.screen==null?"none":mc.screen.getClass().getName());
            client.put("guiWidth",mc.getWindow().getGuiScaledWidth());client.put("guiHeight",mc.getWindow().getGuiScaledHeight());
            client.put("containerScreen",mc.screen instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?>);
            client.put("desktopActive",com.salts_inventory_update.api.client.desktop.DesktopClientApi.currentWorkspace().map(w->w.isNativeScreenActive()).orElse(false));
            client.put("fields",mc.screen==null?Map.of():releaseNativeOnlyFields(mc.screen));
            if(mc.screen!=null)releaseNativeOnlyWidgets(mc.screen,widgets,Collections.newSetFromMap(new IdentityHashMap<>()),0);
            client.put("widgets",widgets);
            work=server(()->{
                var sessions=new ArrayList<String>();
                com.salts_inventory_update.api.server.desktop.DesktopSessions.hasOpenSessionMatching(player(),m->{sessions.add(m.getClass().getName());return false;});
                var inventory=new ArrayList<Object>();for(int i=0;i<player().getInventory().getContainerSize();i++)inventory.add(Map.of("index",i,"item",releaseNativeOnlyStack(player().getInventory().getItem(i))));
                var pos=new net.minecraft.core.BlockPos(8,65,8);var be=level().getBlockEntity(pos);String entity="{}";
                for(var e:level().getAllEntities())if(e.getTags().contains("release_native_stand")){entity=e.saveWithoutId(new net.minecraft.nbt.CompoundTag()).toString();break;}
                write("release-native-only-state.json",Map.of("client",client,"sessions",sessions,"inventory",inventory,
                    "cursor",releaseNativeOnlyStack(player().inventoryMenu.getCarried()),"nativeCursor",releaseNativeOnlyStack(player().containerMenu.getCarried()),"held",releaseNativeOnlyStack(player().getMainHandItem()),
                    "blockState",level().getBlockState(pos).toString(),"blockEntity",be==null?"{}":be.saveWithFullMetadata(player().registryAccess()).toString(),
                    "armorStand",entity));return null;
            });return true;
        }
        if(kind.equals("regression-native-only-tap")){
            if(mc.screen==null)throw new IllegalStateException("No original screen for native input");
            double x=c.get("x").getAsDouble(),y=c.get("y").getAsDouble();int button=num(c,"button",0);
            mc.screen.mouseMoved(x,y);mc.screen.mouseClicked(x,y,button);if(mc.screen!=null)mc.screen.mouseReleased(x,y,button);return true;
        }
        if(kind.equals("regression-native-only-key")){
            if(mc.screen==null)throw new IllegalStateException("No original screen for native key");
            mc.screen.keyPressed(num(c,"key",256),0,num(c,"modifiers",0));return true;
        }
        if(kind.equals("regression-native-only-text")){
            if(mc.screen==null)throw new IllegalStateException("No original screen for native text");
            for(char value:str(c,"text","").toCharArray())mc.screen.charTyped(value,0);return true;
        }
        if(kind.equals("regression-native-only-block-use")){
            if(!(mc.hitResult instanceof net.minecraft.world.phys.BlockHitResult hit)||hit.getType()!=net.minecraft.world.phys.HitResult.Type.BLOCK
                ||!hit.getBlockPos().equals(new net.minecraft.core.BlockPos(8,65,8)))throw new IllegalStateException("Actual player ray must hit native test block");
            mc.gameMode.useItemOn(mc.player,net.minecraft.world.InteractionHand.MAIN_HAND,hit);return true;
        }
'''
    helpers=r'''
    static void releaseNativeOnlyWidgets(Object node,List<Object> out,Set<Object> seen,int depth){
        if(depth>12||!seen.add(node))return;
        if(node instanceof net.minecraft.client.gui.components.AbstractWidget w){
            var row=new LinkedHashMap<String,Object>();row.put("type",w.getClass().getName());row.put("message",w.getMessage().getString());
            row.put("x",w.getX());row.put("y",w.getY());row.put("width",w.getWidth());row.put("height",w.getHeight());row.put("active",w.active);row.put("visible",w.visible);
            row.put("editBox",w instanceof net.minecraft.client.gui.components.EditBox);
            if(w instanceof net.minecraft.client.gui.components.EditBox e)row.put("value",e.getValue());
            row.put("fields",releaseNativeOnlyFields(w));out.add(row);
        }
        if(node instanceof net.minecraft.client.gui.components.events.ContainerEventHandler parent)for(Object child:parent.children())releaseNativeOnlyWidgets(child,out,seen,depth+1);
    }
    static Map<String,Object> releaseNativeOnlyFields(Object screen){
        var result=new TreeMap<String,Object>();
        for(Class<?> type=screen.getClass();type!=null&&!type.getName().startsWith("net.minecraft.");type=type.getSuperclass())for(var f:type.getDeclaredFields()){
            if(java.lang.reflect.Modifier.isStatic(f.getModifiers())||f.isSynthetic())continue;
            try{f.trySetAccessible();Object value=f.get(screen);if(value instanceof Number||value instanceof String||value instanceof Boolean||value instanceof Enum<?>||value instanceof net.minecraft.network.chat.Component)result.put(type.getName()+"#"+f.getName(),value instanceof net.minecraft.network.chat.Component component?component.getString():value instanceof Enum<?>?value.toString():value);}
            catch(IllegalAccessException refused){throw new IllegalStateException("Native screen state is unreadable",refused);}
        }return result;
    }
    static String releaseNativeOnlyStack(ItemStack value){
        if(value.isEmpty())return "{}";var ops=net.minecraft.resources.RegistryOps.create(net.minecraft.nbt.NbtOps.INSTANCE,player().registryAccess());
        return ItemStack.CODEC.encodeStart(ops,value).result().orElseThrow(()->new IllegalStateException("Native-only item serialization failed")).toString();
    }
'''
    command=command.replace('level()','player().serverLevel()')
    if mc=='1.20.1':command=command.replace('be.saveWithFullMetadata(player().registryAccess())','be.saveWithFullMetadata()')
    helpers=storage_item_saves(helpers,mc)
    if not named:
        command=re.sub(r'\bcall\(','coreCall(',command);helpers=re.sub(r'\bcall\(','coreCall(',helpers)
    text=text.replace(anchor,anchor+command,1)
    return text[:text.rfind('}')]+helpers+'\n}\n'
