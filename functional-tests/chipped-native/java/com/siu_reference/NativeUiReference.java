package com.siu_reference;

import com.google.gson.*;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.tutorial.TutorialSteps;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.*;

/** Excluded native-only capture driver. No SIU classes or runtime dependency. */
public final class NativeUiReference {
    static final Gson G=new GsonBuilder().setPrettyPrinting().create();
    static final Path OUT=Path.of(System.getenv("NATIVE_REFERENCE_OUTPUT"));
    static final BlockPos POS=new BlockPos(8,65,8);
    static Minecraft mc;
    static int stage,delay;
    static long last=-1;
    static JsonObject pending;
    static CompletableFuture<?> work;
    static boolean stopping;
    static final List<Object> history=new ArrayList<>();

    public static void tick(){
        if(stopping)return;
        try{
            mc=Minecraft.getInstance();
            if(stage==0){
                if(!(mc.screen instanceof TitleScreen)||mc.getOverlay()!=null)return;
                for(String id:List.of("salts_inventory_update","salts_inventory_compat","salts_inventory_controlify"))
                    if(loaded(id))throw new IllegalStateException("SIU must be absent: "+id);
                mc.options.pauseOnLostFocus=false;mc.options.renderDistance().set(2);mc.options.simulationDistance().set(5);
                mc.options.guiScale().set(2);mc.getWindow().setWindowed(1280,900);mc.resizeDisplay();
                mc.getTutorial().setStep(TutorialSteps.NONE);
                var settings=new LevelSettings("Native UI reference",GameType.CREATIVE,false,Difficulty.PEACEFUL,true,new GameRules(),WorldDataConfiguration.DEFAULT);
                mc.createWorldOpenFlows().createFreshLevel("native-reference-"+System.currentTimeMillis(),settings,new WorldOptions(424242L,false,false),
                    registries->registries.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions());
                stage=1;return;
            }
            if(stage==1){
                if(mc.player==null||mc.getSingleplayerServer()==null||mc.level==null)return;
                work=server(()->{command("fill 0 64 0 20 64 20 minecraft:stone");command("time set day");command("weather clear");command("gamerule doDaylightCycle false");command("gamerule doMobSpawning false");player().connection.teleport(8.5,65,12.5,180,8);return true;});
                stage=2;delay=70;return;
            }
            if(stage==2){if(!work.isDone())return;work.join();if(delay-->0)return;mc.setScreen(null);
                write("ready.json",Map.of("ready",true,"siu_loaded",false,"loader",System.getenv("NATIVE_REFERENCE_LOADER"),"origin","8,65,8"));stage=3;}
            if(pending==null){
                Path file=OUT.resolve("command.json");if(!Files.isRegularFile(file))return;
                var c=JsonParser.parseString(Files.readString(file)).getAsJsonObject();long sequence=c.get("sequence").getAsLong();if(sequence<=last)return;
                pending=c;last=sequence;delay=num(c,"ticks",20);work=null;act(c);return;
            }
            if(work!=null){if(!work.isDone())return;work.join();work=null;}
            if(delay-->0)return;
            var reply=new LinkedHashMap<String,Object>();reply.put("sequence",last);reply.put("command",G.fromJson(pending,Map.class));reply.put("client",state());
            reply.put("server",server(()->{var m=player().containerMenu;var slots=new ArrayList<Object>();for(int i=0;i<m.slots.size();i++){var stack=m.getSlot(i).getItem();slots.add(Map.of("index",i,"item",BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(),"count",stack.getCount()));}var cursor=m.getCarried();return Map.of("slots",slots,"cursor",Map.of("item",BuiltInRegistries.ITEM.getKey(cursor.getItem()).toString(),"count",cursor.getCount()));}).join());
            if(pending.has("capture"))capture(pending.get("capture").getAsString());
            history.add(reply);write("observations.json",history);write("reply.json",reply);pending=null;
        }catch(Throwable error){
            error.printStackTrace();var r=Map.of("sequence",last,"error",error.toString());write("reply.json",r);write("last-error.json",r);pending=null;work=null;
            if(stage<3){stopping=true;mc.stop();}
        }
    }
    static int num(JsonObject c,String k,int fallback){return c.has(k)?c.get(k).getAsInt():fallback;}
    static String str(JsonObject c,String k,String fallback){return c.has(k)?c.get(k).getAsString():fallback;}
    static ResourceLocation id(String s){return new ResourceLocation(s);}
    static ServerPlayer player(){return mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);}
    static CompletableFuture<?> server(Supplier<?> action){return mc.getSingleplayerServer().submit(action::get);}
    static void command(String s){var server=mc.getSingleplayerServer();server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(),s);}
    static ItemStack stack(JsonObject c){return new ItemStack(BuiltInRegistries.ITEM.get(id(str(c,"item","minecraft:diamond"))),num(c,"count",1));}
    static boolean loaded(String id){
        try{var f=Class.forName("net.fabricmc.loader.api.FabricLoader");return (Boolean)f.getMethod("isModLoaded",String.class).invoke(f.getMethod("getInstance").invoke(null),id);}
        catch(ClassNotFoundException forge){try{var f=Class.forName("net.minecraftforge.fml.ModList");return (Boolean)f.getMethod("isLoaded",String.class).invoke(f.getMethod("get").invoke(null),id);}catch(Exception e){throw new IllegalStateException(e);}}
        catch(Exception e){throw new IllegalStateException(e);}
    }
    static Object field(Object o,String name){
        for(Class<?> t=o.getClass();t!=null;t=t.getSuperclass())try{Field f=t.getDeclaredField(name);f.setAccessible(true);return f.get(o);}catch(NoSuchFieldException ignored){}catch(Exception e){throw new IllegalStateException(e);}
        throw new IllegalArgumentException(name);
    }
    static void act(JsonObject c){
        switch(str(c,"type","capture")){
            case "catalog"->{
                var blocks=new ArrayList<String>();var items=new ArrayList<String>();var menus=new ArrayList<String>();
                Set<String> namespaces=Set.of("extended_drawers","extendeddrawersaddon","graveyard","betterend","runes","deep_aether","tiered","smitherz","additionz","chipped");
                BuiltInRegistries.BLOCK.keySet().stream().filter(x->namespaces.contains(x.getNamespace())).forEach(x->blocks.add(x.toString()));
                BuiltInRegistries.ITEM.keySet().stream().filter(x->namespaces.contains(x.getNamespace())).forEach(x->items.add(x.toString()));
                BuiltInRegistries.MENU.keySet().stream().filter(x->namespaces.contains(x.getNamespace())).forEach(x->menus.add(x.toString()));
                write("catalog.json",Map.of("blocks",blocks,"items",items,"menus",menus));
            }
            case "fixture"->{
                mc.player.closeContainer();mc.setScreen(null);
                work=server(()->{player().getInventory().clearContent();player().setShiftKeyDown(false);
                    var level=player().serverLevel();level.removeBlock(POS,false);level.setBlockAndUpdate(POS,BuiltInRegistries.BLOCK.get(id(str(c,"block","minecraft:chest"))).defaultBlockState());
                    player().connection.teleport(8.5,65,12.5,180,8);return true;});
            }
            case "open"->{
                boolean shift=c.has("shift")&&c.get("shift").getAsBoolean();mc.player.setShiftKeyDown(shift);
                work=server(()->{player().setShiftKeyDown(shift);return true;});
                // Use the original block interaction on both sides, preserving its native packet/factory.
                mc.gameMode.useItemOn(mc.player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(POS),Direction.UP,POS,false));
            }
            case "provider"->work=server(()->{var level=player().serverLevel();var provider=level.getBlockState(POS).getMenuProvider(level,POS);if(provider==null&&level.getBlockEntity(POS) instanceof MenuProvider m)provider=m;if(provider==null)throw new IllegalStateException("No provider");player().openMenu(provider);return true;});
            case "held"->work=server(()->{player().getInventory().setItem(player().getInventory().selected,stack(c));player().inventoryMenu.broadcastChanges();return true;});
            case "seed"->work=server(()->{var menu=player().containerMenu;menu.getSlot(num(c,"slot",0)).set(stack(c));menu.broadcastChanges();return true;});
            case "command"->work=server(()->{command(c.get("command").getAsString());return true;});
            case "click"->{mc.screen.mouseClicked(c.get("x").getAsDouble(),c.get("y").getAsDouble(),num(c,"button",0));}
            case "slot"->{mc.gameMode.handleInventoryMouseClick(mc.player.containerMenu.containerId,num(c,"slot",0),num(c,"button",0),ClickType.PICKUP,mc.player);}
            case "scroll"->{mc.screen.mouseScrolled(c.get("x").getAsDouble(),c.get("y").getAsDouble(),c.get("amount").getAsDouble());}
            case "key"->{mc.screen.keyPressed(num(c,"key",0),0,num(c,"modifiers",0));}
            case "text"->{for(char ch:str(c,"text","").toCharArray())mc.screen.charTyped(ch,0);}
            case "inventory"->{mc.setScreen(new InventoryScreen(mc.player));}
            case "close"->{mc.player.closeContainer();mc.player.setShiftKeyDown(false);mc.setScreen(null);}
            case "capture", "wait"->{}
            case "finish"->{write("results.json",Map.of("siu_loaded",false,"observations",history.size(),"scope","native UI screenshot capture only"));stopping=true;mc.stop();}
            default->throw new IllegalArgumentException("Unknown capture command");
        }
    }
    static Map<String,Object> state(){
        var out=new LinkedHashMap<String,Object>();out.put("screen",mc.screen==null?"none":mc.screen.getClass().getName());out.put("guiWidth",mc.getWindow().getGuiScaledWidth());out.put("guiHeight",mc.getWindow().getGuiScaledHeight());out.put("siu_loaded",false);
        if(mc.screen instanceof AbstractContainerScreen<?> screen){
            out.put("title",screen.getTitle().getString());out.put("bounds",Map.of("x",field(screen,"leftPos"),"y",field(screen,"topPos"),"width",field(screen,"imageWidth"),"height",field(screen,"imageHeight")));
            var rows=new ArrayList<Object>();for(int i=0;i<screen.getMenu().slots.size();i++){var s=screen.getMenu().getSlot(i);rows.add(Map.of("index",i,"x",s.x,"y",s.y,"player",s.container instanceof Inventory,"item",BuiltInRegistries.ITEM.getKey(s.getItem().getItem()).toString(),"count",s.getItem().getCount()));}out.put("slots",rows);
        }
        if(mc.screen!=null){var children=new ArrayList<Object>();for(var child:mc.screen.children())if(child instanceof net.minecraft.client.gui.components.AbstractWidget widget)children.add(Map.of("type",child.getClass().getName(),"x",widget.getX(),"y",widget.getY(),"width",widget.getWidth(),"height",widget.getHeight(),"message",widget.getMessage().getString()));out.put("widgets",children);}
        return out;
    }
    static void capture(String name)throws Exception{
        if(!name.matches("[a-z0-9-]+"))throw new IllegalArgumentException("Invalid capture name");
        mc.getToasts().clear();var file=OUT.resolve("screenshots/"+name+".png");Files.createDirectories(file.getParent());
        try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){image.writeToFile(file);}
    }
    static void write(String name,Object value){try{Files.createDirectories(OUT);Files.writeString(OUT.resolve(name),G.toJson(value));}catch(Exception e){throw new IllegalStateException(e);}}
}
