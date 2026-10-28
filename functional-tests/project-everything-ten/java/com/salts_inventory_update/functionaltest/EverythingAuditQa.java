package com.salts_inventory_update.functionaltest;

import com.google.gson.*;
import com.salts_inventory_update.client.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.*;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.*;

/** Excluded observational driver. Never registered as a production integration. */
public final class EverythingAuditQa {
    static final Class<?> H=InWorldGameplayHarness.class;
    static final Gson G=new GsonBuilder().setPrettyPrinting().create();
    static final Path OUT=Path.of(System.getenv("SIU_QA_OUTPUT"));
    static Minecraft mc;
    static JsonObject pending;
    static CompletableFuture<?> work;
    static long last=-1;
    static int settle;
    static boolean probing,ready,completed;
    static String failure="";
    static final List<Map<String,Object>> history=new ArrayList<>();
    public static void initialize(){
        InWorldGameplayHarness.initialize();
        Deque<?> steps=(Deque<?>)field(H,"STEPS");while(steps.size()>3)steps.removeLast();
        h("add","spatial-manual-review",(Runnable)()->{
            mc=Minecraft.getInstance();
            SaltsInventoryConfig.update(c->{c.hasSeenInstructionsWindow=true;c.expandableInventory=false;c.forcedContainerWindows.clear();});
            write("ready.json",Map.of("ready",true,"loader",System.getenv("SIU_FUNCTIONAL_TEST_LOADER")));ready=true;
        },(BooleanSupplier)EverythingAuditQa::tick,(Runnable)()->{},1);
    }
    static Object field(Object o,String name){Class<?> c=o instanceof Class<?> k?k:o.getClass();for(;c!=null;c=c.getSuperclass())try{Field f=c.getDeclaredField(name);f.setAccessible(true);return f.get(o instanceof Class<?>?null:o);}catch(NoSuchFieldException e){}catch(Exception e){throw new RuntimeException(e);}throw new IllegalArgumentException(name);}
    static void set(Object o,String name,Object value){Class<?> c=o instanceof Class<?> k?k:o.getClass();for(;c!=null;c=c.getSuperclass())try{Field f=c.getDeclaredField(name);f.setAccessible(true);f.set(o instanceof Class<?>?null:o,value);return;}catch(NoSuchFieldException e){}catch(Exception e){throw new RuntimeException(e);}throw new IllegalArgumentException(name);}
    static Object h(String name,Object...args){return call(H,name,args);}
    static Object call(Object o,String name,Object...args){Class<?> c=o instanceof Class<?> k?k:o.getClass();for(;c!=null;c=c.getSuperclass())for(Method m:c.getDeclaredMethods())if(m.getName().equals(name)&&m.getParameterCount()==args.length){try{m.setAccessible(true);return m.invoke(o instanceof Class<?>?null:o,args);}catch(IllegalArgumentException e){}catch(InvocationTargetException e){throw new RuntimeException(e.getCause());}catch(Exception e){throw new RuntimeException(e);}}throw new IllegalArgumentException("No method "+name+" on "+o);}
    static void write(String name,Object value){try{Files.createDirectories(OUT);Files.writeString(OUT.resolve(name),G.toJson(value));}catch(Exception e){throw new RuntimeException(e);}}
    static CompletableFuture<?> server(Supplier<?> action){return (CompletableFuture<?>)h("server",action);}
    static ServerPlayer player(){return (ServerPlayer)h("player");}
    static BlockPos origin(){return (BlockPos)field(H,"origin");}
    static List<?> sessions(){return (List<?>)h("sessions");}
    static void selectSession(){if(!sessions().isEmpty())set(H,"activeSession",sessions().get(sessions().size()-1));}
    static AbstractContainerMenu clientMenu(){if(!sessions().isEmpty())return (AbstractContainerMenu)call(sessions().get(sessions().size()-1),"menu");return mc.player.containerMenu;}
    static AbstractContainerMenu serverMenu(){if(!sessions().isEmpty())return (AbstractContainerMenu)h("serverMenu");return player().containerMenu;}
    static String str(JsonObject c,String k,String d){return c.has(k)?c.get(k).getAsString():d;}
    static int num(JsonObject c,String k,int d){return c.has(k)?c.get(k).getAsInt():d;}
    static ItemStack stack(JsonObject c){var stack=new ItemStack(BuiltInRegistries.ITEM.get(new ResourceLocation(str(c,"item","minecraft:dirt"))),num(c,"count",32));if(c.has("nbt"))stack.setTag(parseNbt(c.get("nbt").getAsString()));return stack;}
    static Map<String,Object> item(ItemStack s){return Map.of("item",BuiltInRegistries.ITEM.getKey(s.getItem()).toString(),"count",s.getCount());}
    static List<Map<String,Object>> contents(AbstractContainerMenu m){List<Map<String,Object>> out=new ArrayList<>();for(int i=0;i<m.slots.size();i++){Slot s=m.getSlot(i);Map<String,Object> row=new LinkedHashMap<>(item(s.getItem()));row.put("index",i);row.put("x",s.x);row.put("y",s.y);row.put("max",s.getMaxStackSize());row.put("type",s.getClass().getName());out.add(row);}return out;}
    static Map<String,Object> state(){Map<String,Object> out=new LinkedHashMap<>();out.put("screen",mc.screen==null?"none":mc.screen.getClass().getName());out.put("sessions",sessions().size());out.put("guiWidth",mc.getWindow().getGuiScaledWidth());out.put("guiHeight",mc.getWindow().getGuiScaledHeight());if(mc.screen!=null){out.put("title",mc.screen.getTitle().getString());try{out.put("left",field(mc.screen,"leftPos"));out.put("top",field(mc.screen,"topPos"));}catch(Exception ignored){}List<Object> controls=new ArrayList<>();for(Object c:mc.screen.children())if(c instanceof net.minecraft.client.gui.components.AbstractWidget w)controls.add(Map.of("label",w.getMessage().getString(),"x",w.getX(),"y",w.getY(),"width",w.getWidth(),"height",w.getHeight(),"visible",w.visible,"active",w.active));out.put("widgets",controls);}
        AbstractContainerMenu m=clientMenu();out.put("menu",m.getClass().getName());try{out.put("menuId",BuiltInRegistries.MENU.getKey(m.getType()).toString());}catch(Exception ignored){}out.put("slots",contents(m));out.put("cursor",item(m.getCarried()));if(!sessions().isEmpty()){out.put("desktopCursor",item((ItemStack)field(h("screen"),"sharedCarried")));try{Object w=h("windowSession",call(sessions().get(sessions().size()-1),"sessionId"));out.put("window",Map.of("x",field(w,"x"),"y",field(w,"y"),"width",field(w,"width"),"height",field(w,"height")));}catch(Exception ignored){}}
        var windows=new ArrayList<Map<String,Object>>();for(var session:sessions()){var row=new LinkedHashMap<String,Object>();int id=((Number)call(session,"sessionId")).intValue();row.put("sessionId",id);row.put("sourceKey",call(session,"sourceKey"));row.put("slots",contents((AbstractContainerMenu)call(session,"menu")));try{Object w=h("windowSession",id);row.put("bounds",Map.of("x",field(w,"x"),"y",field(w,"y"),"width",field(w,"width"),"height",field(w,"height")));Object def=field(w,"apiDefinition");row.put("definition",def==null?"none":def.getClass().getName());if(def!=null&&def.getClass().getSimpleName().equals("NativeTemplateWindow")){row.put("templateLayout",call(def,"layout").toString());row.put("layoutError",field(def,"layoutError"));row.put("scrollX",field(def,"scrollX"));row.put("scrollY",field(def,"scrollY"));}Object nativeState=field(w,"apiState");if(nativeState!=null&&nativeState.getClass().getName().contains("AutomaticNativeWindow")){Object nativeScreen=field(nativeState,"screen");var controls=new ArrayList<Object>();for(Object child:(List<?>)call(nativeScreen,"children"))if(child instanceof net.minecraft.client.gui.components.AbstractWidget widget)controls.add(Map.of("x",widget.getX(),"y",widget.getY(),"width",widget.getWidth(),"height",widget.getHeight(),"active",widget.active,"visible",widget.visible,"type",widget.getClass().getName()));row.put("nativeControls",controls);row.put("layout",field(nativeState,"layout").toString());}}catch(Exception e){row.put("observerError",e.toString());}windows.add(row);}out.put("allWindows",windows);
        var desktopWindows=new ArrayList<Map<String,Object>>();Object desktop=InventoryDesktopScreen.current(mc);
        if(desktop!=null)for(Object w:(List<?>)field(desktop,"windows")){
            var row=new LinkedHashMap<String,Object>();row.put("kind",field(w,"kind").toString());
            row.put("bounds",Map.of("x",field(w,"x"),"y",field(w,"y"),"width",field(w,"width"),"height",field(w,"height")));
            row.put("hidden",field(w,"persistentHidden"));Object book=field(w,"recipeBook");
            if(book instanceof net.minecraft.client.gui.screens.recipebook.RecipeBookComponent recipes){
                row.put("recipeVisible",recipes.isVisible());
                if(recipes.isVisible()){
                    int bookWidth=((Number)field(book,"width")).intValue(),bookHeight=((Number)field(book,"height")).intValue();
                    int offset=((Number)field(book,"xOffset")).intValue();
                    row.put("recipeBounds",Map.of("x",(bookWidth-147)/2-offset-30,"y",(bookHeight-166)/2,"width",177,"height",166));
                }
            }
            desktopWindows.add(row);
        }
        out.put("desktopWindows",desktopWindows);return out;}
    static Map<String,Object> authoritative(){selectSession();AbstractContainerMenu m=serverMenu();Map<String,Object> v=new LinkedHashMap<>();v.put("heldNbt",player().getMainHandItem().getTag()==null?"":player().getMainHandItem().getTag().toString());v.put("menu",m.getClass().getName());v.put("slots",contents(m));v.put("cursor",item(m.getCarried()));v.put("inventoryCursor",item(player().inventoryMenu.getCarried()));Object e=player().serverLevel().getBlockEntity(origin());v.put("playerXp",player().totalExperience);v.put("playerPosition",Map.of("x",player().getX(),"y",player().getY(),"z",player().getZ()));var animals=new ArrayList<Map<String,Object>>();for(var animal:player().serverLevel().getEntitiesOfClass(net.minecraft.world.entity.animal.Animal.class,new AABB(origin()).inflate(12))){animals.add(Map.of("type",BuiltInRegistries.ENTITY_TYPE.getKey(animal.getType()).toString(),"age",animal.getAge(),"inLove",animal.isInLove(),"x",animal.getX(),"y",animal.getY(),"z",animal.getZ()));}v.put("animals",animals);if(e instanceof Container container)v.put("sourceItems",contentsAsItems(container));if(e!=null){try{v.put("storedXp",field(e,"storedExp"));}catch(Exception ignored){}v.put("blockEntity",e.getClass().getName());if(e instanceof net.minecraft.world.level.block.entity.BlockEntity entity)v.put("sourceNbt",entity.saveWithFullMetadata().toString());try{v.put("viewers",call(e,"viewers"));}catch(Exception ignored){}v.put("blockState",player().serverLevel().getBlockState(origin()).toString());}return v;}
    static List<Map<String,Object>> contentsAsItems(Container c){var out=new ArrayList<Map<String,Object>>();for(int i=0;i<c.getContainerSize();i++)out.add(item(c.getItem(i)));return out;}
    static void close(){if(mc.screen instanceof InventoryDesktopScreen||!sessions().isEmpty())InventoryDesktopScreen.permanentlyCloseAllOpenWindows(mc);if(mc.player!=null&&mc.player.containerMenu!=mc.player.inventoryMenu)mc.player.closeContainer();mc.setScreen(null);}
    static void click(int slot,int button){selectSession();if(!sessions().isEmpty()){h("clickSlot",slot,button);return;}var s=mc.screen;if(!(s instanceof AbstractContainerScreen<?>))throw new IllegalStateException("Not inventory screen");Slot p=clientMenu().getSlot(slot);double x=((Number)field(s,"leftPos")).intValue()+p.x+8,y=((Number)field(s,"topPos")).intValue()+p.y+8;s.mouseMoved(x,y);s.mouseClicked(x,y,button);s.mouseReleased(x,y,button);}
    static void seedBlock(JsonObject c){var p=player();var level=p.serverLevel();BlockPos pos=origin();for(int x=-4;x<=4;x++)for(int z=-4;z<=4;z++){level.setBlock(pos.offset(x,-1,z),Blocks.STONE.defaultBlockState(),3);for(int h=0;h<4;h++)level.setBlock(pos.offset(x,h,z),Blocks.AIR.defaultBlockState(),3);}level.setBlock(pos.offset(-2,0,0),Blocks.STONE.defaultBlockState(),3);p.teleportTo(level,pos.getX()-1.5,pos.getY()+1,pos.getZ()+.5,-90f,46f);p.setDeltaMovement(Vec3.ZERO);p.inventoryMenu.setCarried(ItemStack.EMPTY);String id=str(c,"block","minecraft:chest");var block=BuiltInRegistries.BLOCK.get(new ResourceLocation(id));if(block==Blocks.AIR&&!id.equals("minecraft:air"))throw new IllegalArgumentException("Missing block "+id);var state=block.defaultBlockState();if(c.has("water")&&c.get("water").getAsBoolean()){for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)level.setBlock(pos.offset(x,0,z),Blocks.WATER.defaultBlockState(),3);if(state.hasProperty(BlockStateProperties.WATERLOGGED))state=state.setValue(BlockStateProperties.WATERLOGGED,true);}level.setBlock(pos,state,3);block.setPlacedBy(level,pos,state,p,new ItemStack(block.asItem()));p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);p.getInventory().clearContent();if(c.has("items")){Object e=level.getBlockEntity(pos);Object storage=e;if(!(storage instanceof Container)){try{storage=call(e,"getInventory");}catch(Exception ignored){try{storage=field(e,"container");}catch(Exception ignored2){}}}for(var el:c.getAsJsonArray("items")){var item=el.getAsJsonObject();if(storage instanceof Container cont)cont.setItem(num(item,"slot",0),stack(item));else call(storage,"setStackInSlot",num(item,"slot",0),stack(item));}}}
    static net.minecraft.nbt.CompoundTag parseNbt(String text){try{return net.minecraft.nbt.TagParser.parseTag(text);}catch(com.mojang.brigadier.exceptions.CommandSyntaxException e){throw new IllegalArgumentException(e);}}
    static void openBlock(){var p=player();var level=p.serverLevel();BlockPos pos=origin();var hit=new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false);level.getBlockState(pos).use(level,p,InteractionHand.MAIN_HAND,hit);}
    static void execute(JsonObject c){String type=str(c,"type","inspect");work=null;failure="";settle=num(c,"ticks",12);selectSession();switch(type){
        case "fixture"->{close();Object desktop=InventoryDesktopScreen.current(mc);if(desktop!=null)set(desktop,"sharedCarried",ItemStack.EMPTY);mc.player.inventoryMenu.setCarried(ItemStack.EMPTY);work=server(()->{seedBlock(c);return null;});}
        case "open"->work=server(()->{openBlock();return null;});
        case "reload-sawmill-config"->{try{call(Class.forName("net.mehvahdjukaar.sawmill.CommonConfigs").getField("SPEC").get(null),"loadFromFile");}catch(ReflectiveOperationException e){throw new RuntimeException(e);}}
        case "catalog"->{String[] mods={"barteringstation","alloy_forgery","seasonsextras","alloyed","farmersdelight","miners_delight","betterarcheology","illagerinvasion","galosphere","sawmill"};Set<String> namespaces=Set.of(mods);write("catalog.json",Map.of("menus",BuiltInRegistries.MENU.keySet().stream().filter(id->namespaces.contains(id.getNamespace())).map(Object::toString).sorted().toList(),"blocks",BuiltInRegistries.BLOCK.keySet().stream().filter(id->namespaces.contains(id.getNamespace())).map(Object::toString).sorted().toList(),"origin",origin().toShortString()));}
        case "key"->mc.screen.keyPressed(num(c,"key",256),0,num(c,"modifiers",0));
        case "text"->{for(char letter:str(c,"text","").toCharArray())mc.screen.charTyped(letter,0);}
        case "block-nbt"->work=server(()->{var entity=player().serverLevel().getBlockEntity(origin());var tag=entity.saveWithFullMetadata();tag.merge(parseNbt(str(c,"nbt","{}")));entity.load(tag);entity.setChanged();player().serverLevel().sendBlockUpdated(origin(),entity.getBlockState(),entity.getBlockState(),3);return null;});
        case "mode"->{close();SaltsInventoryConfig.update(s->{s.forcedContainerWindows.clear();if(c.has("menus"))for(var e:c.getAsJsonArray("menus"))s.forcedContainerWindows.add(e.getAsString());});settle=40;}
        case "enable"->{close();SaltsInventoryConfig.update(s->s.enableMod=c.get("enabled").getAsBoolean());settle=40;}
        case "command"->work=server(()->{h("command",str(c,"command","time set noon"));return null;});
        case "inventory"->InventoryDesktopScreen.openOrToggleInventory(mc);
        case "character"->InventoryDesktopScreen.openOrToggleCharacter(mc);
        case "close"->close();
        case "dismiss-help"->{Object desktop=InventoryDesktopScreen.current(mc);Object help=h("windowKind","INSTRUCTIONS");if(desktop!=null&&help!=null){call(desktop,"setFocusedWindow",help);h("clickWindowControl",help,"CLOSE");}}
        case "click"->click(num(c,"slot",0),num(c,"button",0));
        case "store-in-player"->h("storeInPlayer");
        case "quick"->{if(!sessions().isEmpty())throw new IllegalStateException("Use native callback only for native quick move; desktop case requires desktop shortcut test");mc.gameMode.handleInventoryMouseClick(clientMenu().containerId,num(c,"slot",0),0,ClickType.QUICK_MOVE,mc.player);}
        case "menu-button"->mc.gameMode.handleInventoryButtonClick(clientMenu().containerId,num(c,"button",0));
        case "seed"->work=server(()->{var m=serverMenu();for(var el:c.getAsJsonArray("items")){var s=el.getAsJsonObject();m.getSlot(num(s,"slot",0)).set(stack(s));}m.broadcastFullState();return null;});
        case "seed-player"->work=server(()->{var p=player();for(var el:c.getAsJsonArray("items")){var s=el.getAsJsonObject();p.getInventory().setItem(num(s,"slot",9),stack(s));}p.inventoryMenu.broadcastFullState();return null;});
        case "item-use"->work=server(()->{var p=player();p.setItemInHand(InteractionHand.MAIN_HAND,stack(c));p.getMainHandItem().use(p.serverLevel(),p,InteractionHand.MAIN_HAND);return null;});
        case "item-reopen"->work=server(()->{var p=player();p.getMainHandItem().use(p.serverLevel(),p,InteractionHand.MAIN_HAND);return null;});
        case "sort"->{selectSession();Object w=h("windowSession",call(sessions().get(sessions().size()-1),"sessionId"));h("clickWindowControl",w,"SORT_INVENTORY");}
        case "move"->{selectSession();Object w=h("windowSession",call(sessions().get(sessions().size()-1),"sessionId"));double x=((Number)field(w,"x")).doubleValue()+25,y=((Number)field(w,"y")).doubleValue()+5;h("mouse","mouseClicked",x,y,0,0d,0d);h("mouse","mouseDragged",x+num(c,"dx",70),y+num(c,"dy",30),0,(double)num(c,"dx",70),(double)num(c,"dy",30));h("mouse","mouseReleased",x+num(c,"dx",70),y+num(c,"dy",30),0,0d,0d);}
        case "nearby-container"->work=server(()->{var p=player();var level=p.serverLevel();var pos=origin().offset(2,0,0);level.setBlock(pos,Blocks.CHEST.defaultBlockState(),3);((Container)level.getBlockEntity(pos)).setItem(0,stack(c));level.getBlockState(pos).use(level,p,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false));return null;});
        case "sort-first"->{Object w=h("windowSession",call(sessions().get(0),"sessionId"));h("clickWindowControl",w,"SORT_WINDOWS");}
        case "first-click"->{var session=sessions().get(0);double[] point=(double[])h("slotPoint",call(session,"sessionId"),num(c,"slot",0),h("windowSession",call(session,"sessionId")));h("mouse","mouseClicked",point[0],point[1],num(c,"button",0),0d,0d);h("mouse","mouseReleased",point[0],point[1],num(c,"button",0),0d,0d);}
        case "stale-size"->{Object w=h("windowSession",call(sessions().get(0),"sessionId"));set(w,"width",300);set(w,"height",140);}
        case "crouch-open"->work=server(()->{var p=player();p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);p.setShiftKeyDown(true);try{openBlock();}finally{p.setShiftKeyDown(false);}return null;});
        case "native-reuse-on"->work=server(()->{var p=player();p.setShiftKeyDown(true);try{p.gameMode.useItemOn(p,p.serverLevel(),p.getMainHandItem(),InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(origin()),Direction.UP,origin(),false));}finally{p.setShiftKeyDown(false);}return null;});
        case "native-use-on"->work=server(()->{var p=player();p.setItemInHand(InteractionHand.MAIN_HAND,stack(c));p.setShiftKeyDown(true);try{p.gameMode.useItemOn(p,p.serverLevel(),p.getMainHandItem(),InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(origin()),Direction.UP,origin(),false));}finally{p.setShiftKeyDown(false);}return null;});
        case "resize"->{selectSession();Object w=h("windowSession",call(sessions().get(sessions().size()-1),"sessionId"));double x=((Number)field(w,"x")).doubleValue(),y=((Number)field(w,"y")).doubleValue(),width=((Number)field(w,"width")).doubleValue(),height=((Number)field(w,"height")).doubleValue();h("mouse","mouseClicked",x+width-2,y+height-2,0,0d,0d);h("mouse","mouseDragged",x+num(c,"width",178)-2,y+num(c,"height",86)-2,0,num(c,"width",178)-width,num(c,"height",86)-height);h("mouse","mouseReleased",x+num(c,"width",178)-2,y+num(c,"height",86)-2,0,0d,0d);}
        case "scroll"->{selectSession();Object w=h("windowSession",call(sessions().get(sessions().size()-1),"sessionId"));h("scroll",((Number)field(w,"x")).doubleValue()+15,((Number)field(w,"y")).doubleValue()+35,(double)num(c,"amount",-1));}
        case "quick-desktop"->{selectSession();double[] point=(double[])h("slotPoint",num(c,"slot",0));Object hit=call(h("screen"),"slotAt",point[0],point[1]);call(h("screen"),"quickMoveSlot",hit);}
        case "player-click"->{if(h("windowKind","INVENTORY")==null)InventoryDesktopScreen.openOrToggleInventory(mc);double[] point=(double[])h("slotPoint",0,num(c,"slot",9),h("windowKind","INVENTORY"));h("mouse","mouseClicked",point[0],point[1],num(c,"button",0),0d,0d);h("mouse","mouseReleased",point[0],point[1],num(c,"button",0),0d,0d);}
        case "drag-slots"->{selectSession();double[] prev=null;for(var element:c.getAsJsonArray("slots")){double[] point=(double[])h("slotPoint",element.getAsInt());if(prev==null)h("mouse","mouseClicked",point[0],point[1],0,0d,0d);else h("mouse","mouseDragged",point[0],point[1],0,point[0]-prev[0],point[1]-prev[1]);prev=point;}if(prev!=null)h("mouse","mouseReleased",prev[0],prev[1],0,0d,0d);}
        case "remove-source"->work=server(()->{player().serverLevel().setBlock(origin(),Blocks.AIR.defaultBlockState(),3);return null;});
        case "source-distance"->work=server(()->{var p=player();var pos=origin();p.teleportTo(p.serverLevel(),pos.getX()+num(c,"distance",30),pos.getY()+1,pos.getZ(),0f,0f);return null;});
        case "decline-native"->DesktopContainerClient.declineAutomaticWindow(((Number)call(sessions().get(sessions().size()-1),"sessionId")).intValue());
        case "reject-next-native"->{var menuType=BuiltInRegistries.MENU.get(new ResourceLocation(str(c,"menu","netherchested:nether_chest")));java.util.Map screens=com.salts_inventory_update.mixin.client.MenuScreensAccessor.salts_inventory_update$getScreens();net.minecraft.client.gui.screens.MenuScreens.ScreenConstructor original=(net.minecraft.client.gui.screens.MenuScreens.ScreenConstructor)InventoryDesktopScreen.nativeContainerConstructor(menuType);screens.put(menuType,(net.minecraft.client.gui.screens.MenuScreens.ScreenConstructor)(menu,inventory,title)->{screens.put(menuType,original);Object screen=original.create(menu,inventory,title);set(screen,"imageWidth",0);return (net.minecraft.client.gui.screens.Screen)screen;});}
        case "secondary-block"->work=server(()->{var p=player();var level=p.serverLevel();var pos=origin().offset(2,0,0);var block=BuiltInRegistries.BLOCK.get(new ResourceLocation(str(c,"block","netherchested:nether_chest")));level.setBlock(pos,block.defaultBlockState(),3);block.setPlacedBy(level,pos,level.getBlockState(pos),p,new ItemStack(block.asItem()));if(level.getBlockEntity(pos) instanceof Container cont&&c.has("item"))cont.setItem(0,stack(c));level.getBlockState(pos).use(level,p,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false));return null;});
        case "window-click"->{Object session=sessions().get(num(c,"window",0));double[] pt=(double[])h("slotPoint",call(session,"sessionId"),num(c,"slot",0),h("windowSession",call(session,"sessionId")));h("mouse","mouseClicked",pt[0],pt[1],num(c,"button",0),0d,0d);h("mouse","mouseReleased",pt[0],pt[1],num(c,"button",0),0d,0d);}
        case "gui-scale"->{mc.options.guiScale().set(num(c,"scale",3));mc.resizeDisplay();}
        case "native-inspector"->{InventoryDesktopScreen.current(mc).inspectNativeWindow();}
        case "xy-click"->{h("mouse","mouseClicked",c.get("x").getAsDouble(),c.get("y").getAsDouble(),num(c,"button",0),0d,0d);h("mouse","mouseReleased",c.get("x").getAsDouble(),c.get("y").getAsDouble(),num(c,"button",0),0d,0d);}
        case "xy-drag"->{double x=c.get("x").getAsDouble(),y=c.get("y").getAsDouble(),toX=c.get("toX").getAsDouble(),toY=c.get("toY").getAsDouble();h("mouse","mouseClicked",x,y,0,0d,0d);h("mouse","mouseDragged",toX,toY,0,toX-x,toY-y);h("mouse","mouseReleased",toX,toY,0,0d,0d);}
        case "reload-profiles"->{com.salts_inventory_update.client.automatic.NativeProfileFiles.reload();DesktopContainerClient.refreshNativeTemplates();}
        case "inspect","wait","capture","finish"->{}
        default->throw new IllegalArgumentException(type);
    }}
    static boolean tick(){if(completed)return true;if(!ready)return false;try{
        if(pending==null){Path request=OUT.resolve("command.json");if(!Files.exists(request))return false;var c=JsonParser.parseString(Files.readString(request)).getAsJsonObject();long n=c.get("sequence").getAsLong();if(n<=last)return false;pending=c;last=n;probing=false;try{execute(c);}catch(Throwable e){failure=e.toString();work=null;settle=3;}return false;}
        if(work!=null&&!work.isDone())return false;if(work!=null&&work.isCompletedExceptionally()){try{work.join();}catch(Throwable e){failure=e.toString();}work=null;}
        if(settle-->0)return false;
        if(!probing){probing=true;work=server(EverythingAuditQa::authoritative);return false;}
        Map<String,Object> result=new LinkedHashMap<>();result.put("command",pending);result.put("sequence",last);result.put("error",failure);result.put("client",state());if(work!=null)try{result.put("server",work.join());}catch(Throwable e){result.put("probeError",e.toString());}
        if(pending.has("capture")){String label=pending.get("capture").getAsString();h("capture",label);result.put("screenshot",OUT.resolve("screenshots").resolve(label+".png").toString());}
        history.add(result);write("observations.json",history);write("reply.json",result);boolean finish=str(pending,"type","").equals("finish");pending=null;work=null;completed=finish;return finish;
    }catch(Throwable e){write("driver-error.json",Map.of("sequence",last,"error",e.toString()));pending=null;work=null;return false;}}
}
