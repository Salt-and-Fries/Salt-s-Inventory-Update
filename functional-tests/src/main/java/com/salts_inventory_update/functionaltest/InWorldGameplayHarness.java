package com.salts_inventory_update.functionaltest;

import com.google.gson.GsonBuilder;
import com.salts_inventory_update.SaltsInventoryUpdate;
import com.salts_inventory_update.VersionInfo;
import com.salts_inventory_update.client.InventoryDesktopScreen;
import com.salts_inventory_update.client.DesktopContainerClient;
import com.salts_inventory_update.client.DesktopContainerSession;
import com.salts_inventory_update.client.SaltsInventoryConfig;
import com.salts_inventory_update.network.DesktopPackets;
import com.salts_inventory_update.platform.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.*;

/** Opt-in in-world QA. Uses disposable worlds, real menu capture, GUI callbacks,
 * authenticated packets, authoritative server assertions and game framebuffer captures.
 * This class and all test instrumentation are excluded from production artifacts.
 */
public final class InWorldGameplayHarness {
    private record Step(String name, Runnable start, BooleanSupplier ready, Runnable verify, int settleTicks) { }
    private static final Deque<Step> STEPS = new ArrayDeque<>();
    private static final List<Map<String,Object>> RESULTS = Collections.synchronizedList(new ArrayList<>());
    private static Step active;
    private static boolean initialized;
    private static volatile boolean finished, worldConnected, clientReady;
    private static long startedAt, stepStarted;
    private static int elapsedTicks, settle;
    private static Path output;
    private static Minecraft mc;
    private static CompletableFuture<?> task;
    private static BlockPos origin, networkStoragePos;
    private static Object activeWindow;
    private static DesktopContainerSession activeSession;
    private static int clicks;
    private static int beforeX, beforeY, beforeWidth, beforeHeight;
    private static int beforeExtra;
    private static final java.util.concurrent.atomic.AtomicInteger PENDING_CAPTURES=new java.util.concurrent.atomic.AtomicInteger();
    private static final String SCENARIO = Optional.ofNullable(System.getenv("SIU_QA_SCENARIO")).orElse("core");

    private InWorldGameplayHarness() { }
    public static synchronized void initialize() {
        if (initialized) return;
        initialized = true;
        output = Path.of(Optional.ofNullable(System.getenv("SIU_QA_OUTPUT")).orElse("gameplay-qa")).toAbsolutePath();
        try { Files.createDirectories(output.resolve("screenshots")); } catch (Exception e) { throw new IllegalStateException(e); }
        startedAt = System.currentTimeMillis();
        com.salts_inventory_update.api.integration.DesktopDiagnostics.listen(event -> {
            if(event.level()==com.salts_inventory_update.api.integration.DesktopDiagnostics.Level.ERROR)
                record("api-error-"+event.operation(),"FAIL",event.owner()+": "+event.message());
        });
        add("world-create-and-connect", InWorldGameplayHarness::createWorld,
            () -> mc.player != null && mc.level != null && mc.getSingleplayerServer() != null,
            () -> {worldConnected=true;check("integrated-server-connected", mc.player != null);}, 40);
        add("desktop-handshake", () -> setNativeScreen(null), DesktopContainerClient::canUseServerSessions,
            () -> check("authenticated-desktop-ready", DesktopContainerClient.canUseServerSessions()), 15);
        if (SCENARIO.equals("ae2-atlas")) {
            System.setProperty("ae2.desktop.qa.variants", "true");
            System.setProperty("ae2.desktop.qa.populated", "true");
            // The rectangular-toolbar reference atlas has 90 views. Expanded
            // historical visual probes add other views and use a separate run.
            System.setProperty("ae2.desktop.qa.visualFixes", "false");
            System.setProperty("ae2.desktop.qa.quit", "false");
        } else {
        add("world-fixture", () -> server(() -> {
            var p = player(); origin = p.blockPosition().offset(2, 0, 0);
            var level = serverLevel(p);
            Object stone = field(ClassFor("net.minecraft.world.level.block.Blocks"), "STONE");
            Object air = field(ClassFor("net.minecraft.world.level.block.Blocks"), "AIR");
            for (int x=-1; x<9; x++) for (int z=-2; z<3; z++) {
                level.setBlock(origin.offset(x,-1,z), (net.minecraft.world.level.block.state.BlockState)call(stone,"defaultBlockState"),3);
                for(int y=0;y<5;y++) level.setBlock(origin.offset(x,y,z),(net.minecraft.world.level.block.state.BlockState)call(air,"defaultBlockState"),3);
            }
            p.getInventory().clearContent(); p.inventoryMenu.setCarried(ItemStack.EMPTY);
            command("gamerule doDaylightCycle false"); command("time set noon"); command("weather clear");
            return null;
        }), InWorldGameplayHarness::taskDone, () -> check("disposable-fixture-ready", origin != null), 10);
        addLocalWindows();
        addExpansion();
        addContainer("chest", "minecraft:chest", 0);
        addChestInteractions();
        for (String block : List.of("trapped_chest","barrel","ender_chest","shulker_box","hopper","dispenser","dropper",
            "furnace","blast_furnace","smoker","brewing_stand","crafting_table","enchanting_table","anvil","grindstone",
            "loom","smithing_table","stonecutter","cartography_table","beacon","crafter")) {
            if (block.equals("crafter") && VersionInfo.MINECRAFT_VERSION.equals("1.20.1")) continue;
            addContainer(block, "minecraft:"+block, 0);
        }
        for(String mount:List.of("horse","donkey","llama","camel"))addMount(mount);
        addIntegrations();
        add("resource-pack-reload", () -> {
            closeWindows(); task=(CompletableFuture<?>)call(mc,"reloadResourcePacks");
        }, () -> taskDone() && overlay()==null,
            () -> check("desktop-ready-after-resource-reload",DesktopContainerClient.canUseServerSessions()), 15);
        add("inventory-after-resource-reload", () -> InventoryDesktopScreen.openOrToggleInventory(mc),
            () -> windowKind("INVENTORY")!=null, () -> capture("inventory-after-resource-reload"), 10);
        add("final-state-and-save", () -> { closeWindows(); server(() -> { player().inventoryMenu.setCarried(ItemStack.EMPTY); return null; }); },
            InWorldGameplayHarness::taskDone, () -> capture("final-world"), 10);
        }
        com.salts_inventory_update.api.client.desktop.DesktopClientApi.onClientStarted(client->clientReady=true);
        ClientTickEvents.END_CLIENT_TICK.register(InWorldGameplayHarness::tick);
        Thread watchdog=new Thread(()->{
            while(!finished){
                try{Thread.sleep(1000L);}catch(InterruptedException e){return;}
                if(mc!=null&&worldConnected&&mc.player==null&&!finished)mc.execute(()->{
                    if(!finished&&mc.player==null){record("unexpected-disconnect","FAIL","Client disconnected during gameplay QA");finish();}
                });
            }
        },"SIU gameplay QA disconnect watchdog");watchdog.setDaemon(true);watchdog.start();
        SaltsInventoryUpdate.LOGGER.info("SIU_GAMEPLAY_START scenario={} mc={} output={}",SCENARIO,VersionInfo.MINECRAFT_VERSION,output);
    }

    private static void addLocalWindows() {
        add("inventory-ui", () -> InventoryDesktopScreen.openOrToggleInventory(mc), () -> screen()!=null,
            () -> { check("inventory-window-present", windowKind("INVENTORY")!=null); capture("inventory"); }, 10);
        add("character-ui", () -> InventoryDesktopScreen.openOrToggleCharacter(mc), () -> windowKind("CHARACTER")!=null,
            () -> capture("character"), 10);
        add("creative-mode", () -> { closeWindows(); server(() -> {command("gamemode creative @a");return null;}); },
            () -> taskDone() && infiniteItems(), () -> {}, 5);
        add("creative-ui", () -> InventoryDesktopScreen.openOrToggleCreative(mc), () -> windowKind("CREATIVE")!=null,
            () -> capture("creative"), 10);
        add("instructions-ui", () -> InventoryDesktopScreen.openInstructions(mc), () -> windowKind("INSTRUCTIONS")!=null,
            () -> capture("instructions"), 10);
        add("configuration-ui", () -> {
            closeWindows(); setNativeScreen(com.salts_inventory_update.client.WindowedInventoryClient.createConfigScreen(null));
        }, () -> nativeScreen()!=null, () -> capture("configuration"), 10);
        add("configuration-close", () -> setNativeScreen(null), () -> true, () -> {}, 2);
        add("survival-mode", () -> server(() -> {command("gamemode survival @a");return null;}),
            () -> taskDone() && !infiniteItems(), () -> {}, 5);
    }

    private static void addContainer(String label, String blockId, int offset) {
        add("fixture-"+label, () -> {
            closeWindows();
            server(() -> {
                var p=player(); BlockPos pos=origin.offset(offset,0,0);
                Object block=registryValue("BLOCK",blockId);
                if (block==null) throw new IllegalStateException("Missing fixture block "+blockId);
                var level=serverLevel(p);
                level.setBlock(pos,(net.minecraft.world.level.block.state.BlockState)call(block,"defaultBlockState"),3);
                // Preserve native placement initialization (including third-party ownership checks).
                call(block,"setPlacedBy",level,pos,level.getBlockState(pos),p,new ItemStack((net.minecraft.world.item.Item)call(block,"asItem")));
                if(label.equals("toms-inventory_cable_connector")){
                    Object direction=call(level.getBlockState(pos),"getValue",field(ClassFor("com.tom.storagemod.block.InventoryCableConnectorBlock"),"FACING"));
                    BlockPos beaconPos=(BlockPos)call(pos,"relative",direction);
                    Object iron=registryValue("BLOCK","minecraft:iron_block"),beacon=registryValue("BLOCK","minecraft:beacon");
                    for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)level.setBlock(beaconPos.offset(x,-1,z),(net.minecraft.world.level.block.state.BlockState)call(iron,"defaultBlockState"),3);
                    level.setBlock(beaconPos,(net.minecraft.world.level.block.state.BlockState)call(beacon,"defaultBlockState"),3);
                }
                Object entity=level.getBlockEntity(pos);
                if(entity instanceof Container container) {
                    container.clearContent();
                    if(container.getContainerSize()>0) container.setItem(0,new ItemStack(Items.DIRT,32));
                }
                if (label.equals("ae2-sky_stone_chest")) {
                    call(call(entity,"getInternalInventory"),"setItemDirect",0,new ItemStack(Items.DIRT,32));
                }
                return null;
            });
        }, InWorldGameplayHarness::taskDone, () -> {}, label.equals("toms-inventory_cable_connector")?100:8);
        add("open-"+label, () -> {server(() -> {
            var p=player(); var level=serverLevel(p); BlockPos pos=origin.offset(offset,0,0);
            Object entity=level.getBlockEntity(pos);
                Object provider=call(level.getBlockState(pos),"getMenuProvider",level,pos);
                if(provider==null && entity instanceof net.minecraft.world.MenuProvider) provider=entity;
                if(provider!=null) p.openMenu((net.minecraft.world.MenuProvider)provider);
                else {
                    Object vec=construct(ClassFor("net.minecraft.world.phys.Vec3"),pos.getX()+0.5,pos.getY()+0.5,pos.getZ()+0.5);
                    Object hit=construct(ClassFor("net.minecraft.world.phys.BlockHitResult"),vec,enumValue("net.minecraft.core.Direction","UP"),pos,false);
                    try{call(level.getBlockState(pos),"useWithoutItem",level,p,hit);}
                    catch(IllegalArgumentException legacy){call(level.getBlockState(pos),"use",level,p,enumValue("net.minecraft.world.InteractionHand","MAIN_HAND"),hit);}
                }
                return null;
            });
        }, () -> taskDone() && !sessions().isEmpty(), () -> {
            activeSession=sessions().get(sessions().size()-1);
            activeWindow=windowSession(activeSession.sessionId());
            check(label+"-captured-session",activeWindow!=null && currentSession().menu()!=null);
            check(label+"-window-size-positive",((Number)field(activeWindow,"width")).intValue()>0 && ((Number)field(activeWindow,"height")).intValue()>0);
            if(label.startsWith("toms-")||label.startsWith("sophisticated-")||label.startsWith("ae2-"))check(label+"-api-definition",field(activeWindow,"apiDefinition")!=null);
            capture(label);
        }, label.equals("toms-inventory_cable_connector")?100:12);
        if(label.equals("crafting_table")) addCrafting();
        if(label.equals("furnace")) addSmelting();
        if(label.equals("toms-storage_terminal"))addTerminalInteractions();
        if(label.equals("ae2-sky_stone_chest")) {
            add("ae2-native-slot-pickup",()->clickSlot(0,left()),()->carried()==32,()->{
                var actual=server(()->player().inventoryMenu.getCarried().getCount());
                addFirst("ae2-shared-cursor-authoritative",()->{},actual::isDone,
                        ()->check("ae2-cursor-conserved",actual.join().equals(32)),3);
                capture("ae2-sky-chest-pickup");
            },8);
            add("ae2-store-in-player",InWorldGameplayHarness::storeInPlayer,()->carried()==0,()->{},8);
        }
        if(!label.equals("chest")) add("close-"+label, InWorldGameplayHarness::closeWindows,
            () -> sessions().isEmpty(), () -> check(label+"-session-closed",sessions().isEmpty()), 5);
    }

    private static void addExpansion() {
        add("expansion-fund",()->{closeWindows();server(()->{player().giveExperienceLevels(20);return null;});},
            ()->taskDone()&&mc.player.experienceLevel>=20,()->{},8);
        add("expansion-open",()->InventoryDesktopScreen.openOrToggleInventory(mc),()->windowKind("INVENTORY")!=null,()->{},8);
        add("expansion-purchase-click",()->{
            beforeExtra=extraSlots(mc.player);Object w=windowKind("INVENTORY");Object rect=call(screen(),"increaseInventoryButtonRect",w);
            if(rect==null){scroll((double)number(w,"x")+35,(double)number(w,"y")+50,-4D);rect=call(screen(),"increaseInventoryButtonRect",w);}
            if(rect==null)throw new IllegalStateException("Purchase control not visible");
            clickAt(((Number)call(rect,"x")).doubleValue()+8,((Number)call(rect,"y")).doubleValue()+8);
        },()->extraSlots(mc.player)==beforeExtra+1,()->{
            var actual=server(()->List.of(extraSlots(player()),player().experienceLevel));
            addFirst("expansion-server-purchase",()->{},actual::isDone,()->{
                List<Integer> value=(List<Integer>)actual.join();check("expansion-authoritative-count",value.get(0)==beforeExtra+1);
                check("expansion-xp-cost",value.get(1)==20-(beforeExtra+1));capture("expanded-inventory");
            },3);
        },8);
        add("expansion-close-and-reopen",()->{closeWindows();InventoryDesktopScreen.openOrToggleInventory(mc);},
            ()->windowKind("INVENTORY")!=null,()->check("expansion-preserved-reopen",extraSlots(mc.player)==beforeExtra+1),5);
        add("expansion-extra-slot-seed",()->server(()->{
            Object inventory=call(player(),"salts_inventory_update$getExtraInventory");call(inventory,"setItem",beforeExtra,new ItemStack(Items.DIAMOND,16));
            call(ClassFor("com.salts_inventory_update.inventory.InventoryExpansion"),"syncToClient",player());return null;
        }),()->taskDone()&&mc.player.inventoryMenu.getSlot(46+beforeExtra).getItem().getCount()==16,()->{},8);
        add("expansion-extra-slot-pickup",()->{
            Object w=windowKind("INVENTORY");scroll((double)number(w,"x")+35,(double)number(w,"y")+50,-4D);
            var point=slotPoint(0,46+beforeExtra,w);clickAt(point[0],point[1]);
        },()->carried()==16,()->{
            var actual=server(()->player().inventoryMenu.getSlot(46+beforeExtra).getItem().isEmpty()&&player().inventoryMenu.getCarried().getCount()==16);
            addFirst("expansion-extra-authoritative",()->{},actual::isDone,()->check("extra-slot-server-pickup",(Boolean)actual.join()),3);
        },8);
        add("expansion-extra-slot-return",()->{var point=slotPoint(0,46+beforeExtra,windowKind("INVENTORY"));clickAt(point[0],point[1]);},
            ()->carried()==0,()->capture("expanded-slot-interaction"),8);
        add("expansion-close",InWorldGameplayHarness::closeWindows,()->true,()->{},3);
    }
    private static int extraSlots(Object p){return ((Number)call(p,"salts_inventory_update$getExtraSlotCount")).intValue();}
    private static AbstractContainerMenu serverMenu(){
        Object ps=call(ClassFor("com.salts_inventory_update.server.DesktopContainerSessions"),"sessions",player());
        Object session=((Map<?,?>)field(ps,"sessions")).get(activeSession.sessionId());
        return (AbstractContainerMenu)field(session,"menu");
    }
    private static void addCrafting(){
        add("crafting-seed",()->server(()->{
            var menu=serverMenu();for(int i:List.of(1,2,4,5))menu.getSlot(i).set(new ItemStack(Items.OAK_PLANKS,1));
            menu.broadcastFullState();return null;
        }),()->taskDone()&&currentSession().menu().getSlot(0).getItem().is(Items.CRAFTING_TABLE),()->capture("crafting-output"),8);
        add("crafting-take-result",()->clickSlot(0,left()),()->carried()==1,()->{
            var actual=server(()->{
                var menu=serverMenu();return player().inventoryMenu.getCarried().is(Items.CRAFTING_TABLE)
                    &&List.of(1,2,4,5).stream().allMatch(i->menu.getSlot(i).getItem().isEmpty());
            });
            addFirst("crafting-authoritative",()->{},actual::isDone,()->check("crafting-consumes-four-planks",(Boolean)actual.join()),3);
        },8);
        add("crafting-store-result",InWorldGameplayHarness::storeInPlayer,()->carried()==0,()->{},5);
    }
    private static void addSmelting(){
        add("furnace-seed",()->server(()->{
            Container c=(Container)serverLevel().getBlockEntity(origin);c.setItem(0,new ItemStack(Items.IRON_ORE,1));c.setItem(1,new ItemStack(Items.COAL,1));c.setChanged();return null;
        }),InWorldGameplayHarness::taskDone,()->{},5);
        add("furnace-completes-recipe",()->{},()->currentSession().menu().getSlot(2).getItem().is(Items.IRON_INGOT),
            ()->capture("furnace-output"),8);
        add("furnace-take-result",()->clickSlot(2,left()),()->carried()==1,()->{
            var actual=server(()->player().inventoryMenu.getCarried().is(Items.IRON_INGOT));
            addFirst("furnace-authoritative",()->{},actual::isDone,()->check("furnace-server-result",(Boolean)actual.join()),3);
        },6);
        add("furnace-store-result",InWorldGameplayHarness::storeInPlayer,()->carried()==0,()->{},5);
    }
    private static void addIntegrations(){
        if(SCENARIO.equals("core"))return;
        add("extension-loaded",()->{},()->true,()->{
            check("extension-present",com.salts_inventory_update.api.integration.DesktopEnvironment.isModLoaded("salts_inventory_compat"));
            if(SCENARIO.equals("extension-empty")){
                for(String id:List.of("ae2","toms_storage","sophisticatedbackpacks","sophisticatedstorage","jei","emi","roughlyenoughitems","modmenu"))
                    check("optional-absent-"+id,!com.salts_inventory_update.api.integration.DesktopEnvironment.isModLoaded(id));
            }
        },3);
        if(SCENARIO.startsWith("browser-"))addRecipeBrowser();
        if(SCENARIO.equals("ae2")) {
            add("ae2-installed",()->{},()->true,()->check("ae2-present",
                    com.salts_inventory_update.api.integration.DesktopEnvironment.isModLoaded("ae2")),3);
            for(String id:List.of("sky_stone_chest","drive","cell_workbench","inscriber"))
                addContainer("ae2-"+id,"ae2:"+id,0);
        }
        if(SCENARIO.equals("toms")){
            add("toms-connected-storage-fixture",()->server(()->{
                Object terminal=registryValue("BLOCK","toms_storage:storage_terminal");
                Object direction=call(call(terminal,"defaultBlockState"),"getValue",field(ClassFor("com.tom.storagemod.block.AbstractStorageTerminalBlock"),"FACING"));
                networkStoragePos=(BlockPos)call(origin,"relative",direction,2);
                for(int i=1;i<=2;i++){
                    Object b=registryValue("BLOCK",i==1?"toms_storage:inventory_connector":"minecraft:chest");
                    serverLevel().setBlock((BlockPos)call(origin,"relative",direction,i),(net.minecraft.world.level.block.state.BlockState)call(b,"defaultBlockState"),3);
                    BlockPos pos=(BlockPos)call(origin,"relative",direction,i);
                    call(b,"setPlacedBy",serverLevel(),pos,serverLevel().getBlockState(pos),player(),new ItemStack((net.minecraft.world.item.Item)call(b,"asItem")));
                }
                Container c=(Container)serverLevel().getBlockEntity(networkStoragePos);
                c.setItem(0,new ItemStack(Items.DIRT,32));c.setItem(1,new ItemStack(Items.OAK_PLANKS,32));c.setChanged();return null;
            }),InWorldGameplayHarness::taskDone,()->{},40);
            for(String id:List.of("storage_terminal","crafting_terminal","level_emitter","filing_cabinet"))
                addContainer("toms-"+id,"toms_storage:"+id,0);
            for(String id:List.of("item_filter","tag_item_filter","inventory_configurator"))addItemIntegration("toms-"+id,"toms_storage:"+id);
            addContainer("toms-inventory_cable_connector","toms_storage:inventory_cable_connector",0);
        }
        if(SCENARIO.equals("sophisticated")){
            addItemIntegration("sophisticated-backpack","sophisticatedbackpacks:backpack");
            for(String id:List.of("chest","barrel","limited_barrel_1"))addContainer("sophisticated-"+id,"sophisticatedstorage:"+id,0);
        }
    }
    private static void addMount(String id){
        add("open-mount-"+id,()->{
            closeWindows();server(()->{
                Object type=registryValue("ENTITY_TYPE","minecraft:"+id);Object entity;
                try{entity=call(type,"create",serverLevel());}
                catch(IllegalArgumentException modern){entity=call(type,"create",serverLevel(),enumValue("net.minecraft.world.entity.EntitySpawnReason","COMMAND"));}
                call(entity,"setPos",(double)origin.getX(),(double)origin.getY(),(double)origin.getZ());call(entity,"setNoAi",true);
                call(entity,"setTamed",true);if(id.equals("donkey")||id.equals("llama"))call(entity,"setChest",true);
                call(serverLevel(),"addFreshEntity",entity);call(entity,"openCustomInventoryScreen",player());return null;
            });
        },()->taskDone()&&!sessions().isEmpty(),()->{
            activeSession=sessions().get(sessions().size()-1);check(id+"-mount-session",activeSession.isMountSession());capture("mount-"+id);
        },12);
        add("close-mount-"+id,InWorldGameplayHarness::closeWindows,()->sessions().isEmpty(),()->{},5);
    }
    private static void addItemIntegration(String label,String itemId){
        add("seed-"+label,()->{
            closeWindows();server(()->{
                var p=player();Object item=registryValue("ITEM",itemId);
                if(item==null)throw new IllegalStateException("Missing fixture item "+itemId);
                ItemStack stack=new ItemStack((net.minecraft.world.item.Item)item);
                if(label.equals("sophisticated-backpack")){
                    Class<?> wrapperType=ClassFor("net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper");
                    Object wrapper;
                    try { wrapper=call(wrapperType,"fromStack",stack); }
                    catch(IllegalArgumentException legacy) { wrapper=construct(wrapperType,stack); }
                    call(wrapper,"getInventoryHandler");
                    if(((Optional<?>)call(wrapper,"getContentsUuid")).isEmpty())call(wrapper,"setContentsUuid",UUID.randomUUID());
                }
                p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,stack);
                p.inventoryMenu.broadcastFullState();return null;
            });
        },()->taskDone()&&mc.player.getItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND).getItem()==registryValue("ITEM",itemId),()->{},8);
        add("open-"+label,()->{
            server(()->{
                var p=player();Object item=p.getItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND).getItem();
                if(label.equals("toms-inventory_configurator"))call(item,"onRightClick",p,p.getItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND),networkStoragePos,net.minecraft.world.InteractionHand.MAIN_HAND);
                else call(item,"use",serverLevel(p),p,net.minecraft.world.InteractionHand.MAIN_HAND);return null;
            });
        },()->taskDone()&&!sessions().isEmpty(),()->{
            activeSession=sessions().get(sessions().size()-1);activeWindow=windowSession(activeSession.sessionId());
            check(label+"-api-definition",field(activeWindow,"apiDefinition")!=null);capture(label);
        },15);
        if(label.equals("sophisticated-backpack")){
            addDrag("backpack-left-drag",left(),30,List.of(10,10,10));
            addDrag("backpack-right-drag",right(),30,List.of(1,1,1));
            addSophisticatedUpgrade();
            addSophisticatedSettings();
        }
        add("close-"+label,InWorldGameplayHarness::closeWindows,()->sessions().isEmpty(),()->{},5);
    }
    private static void addSophisticatedUpgrade(){
        add("backpack-crafting-upgrade-seed",()->server(()->{
            var item=(net.minecraft.world.item.Item)registryValue("ITEM","sophisticatedbackpacks:crafting_upgrade");
            if(item==null)throw new IllegalStateException("Crafting upgrade fixture missing");
            player().getInventory().setItem(9,new ItemStack(item));player().inventoryMenu.broadcastFullState();return null;
        }),()->taskDone()&&mc.player.inventoryMenu.getSlot(9).getItem().getItem()==registryValue("ITEM","sophisticatedbackpacks:crafting_upgrade"),()->{},8);
        add("backpack-crafting-upgrade-pickup",()->{
            if(windowKind("INVENTORY")==null)InventoryDesktopScreen.openOrToggleInventory(mc);
            Object w=windowKind("INVENTORY");double x=number(w,"x")+22,y=number(w,"y")+5;
            double dx=16-number(w,"x"),dy=260-number(w,"y");
            mouse("mouseClicked",x,y,left(),0,0);mouse("mouseDragged",x+dx,y+dy,left(),dx,dy);mouse("mouseReleased",x+dx,y+dy,left(),0,0);
            var point=slotPoint(0,9,w);clickAt(point[0],point[1]);
        },()->carried()==1,()->{},8);
        add("backpack-crafting-upgrade-install",()->{
            Slot slot=(Slot)((List<?>)field(currentSession().menu(),"upgradeSlots")).get(0);
            int id=com.salts_inventory_update.api.desktop.DesktopMenuBindings.indexOf(currentSession().menu(),slot);clickSlot(id,left());
        },()->carried()==0&&DesktopContainerClient.canDispatchOptimisticMutationNow(),()->{
            check("upgrade-client-logical-slot",!com.salts_inventory_update.api.desktop.DesktopMenuBindings.slot(currentSession().menu(),
                com.salts_inventory_update.api.desktop.DesktopMenuBindings.indexOf(currentSession().menu(),(Slot)((List<?>)field(currentSession().menu(),"upgradeSlots")).get(0))).getItem().isEmpty());
            var actual=server(()->!((Slot)((List<?>)field(serverMenu(),"upgradeSlots")).get(0)).getItem().isEmpty());
            addFirst("backpack-upgrade-authoritative",()->{},actual::isDone,()->check("upgrade-installed-on-server",(Boolean)actual.join()),3);capture("backpack-upgrade-installed");
        },12);
        add("backpack-crafting-upgrade-tab",()->{
            Object state=field(windowSession(activeSession.sessionId()),"apiState");Object tabs=call(call(field(state,"access"),"components"),"sideTabs");
            Object tab=((List<?>)call(tabs,"children")).stream().filter(t->t.getClass().getSimpleName().contains("CraftingUpgradeTab")).findFirst().orElseThrow();
            clickAt(((Number)call(tab,"getX")).doubleValue()+8,((Number)call(tab,"getY")).doubleValue()+8);
        },()->{
            Object state=field(windowSession(activeSession.sessionId()),"apiState");Object tabs=call(call(field(state,"access"),"components"),"sideTabs");
            return ((Optional<?>)call(tabs,"getOpenTab")).isPresent();
        },()->capture("backpack-crafting-upgrade-panel"),12);
    }
    private static void addTerminalInteractions(){
        add("terminal-network-snapshot",()->{},()->!((List<?>)field(field(windowSession(activeSession.sessionId()),"apiState"),"entries")).isEmpty(),
            ()->{check("terminal-two-item-types",((List<?>)field(field(windowSession(activeSession.sessionId()),"apiState"),"entries")).size()==2);capture("toms-connected-terminal");},5);
        add("terminal-withdraw-click",()->{
            Object w=windowSession(activeSession.sessionId());Object c=call(screen(),"apiContext",w,null,0,0);
            clickAt(((Number)call(c,"contentX")).doubleValue()+8,((Number)call(c,"contentY")).doubleValue()+27);
        },()->carried()==32,()->{
            var value=server(()->{
                Container c=(Container)serverLevel().getBlockEntity(networkStoragePos);int n=0;
                for(int i=0;i<c.getContainerSize();i++)n+=c.getItem(i).getCount();return List.of(n,player().inventoryMenu.getCarried().getCount());
            });addFirst("terminal-authoritative-withdraw",()->{},value::isDone,()->check("terminal-withdraw-conservation",value.join().equals(List.of(32,32))),3);
        },8);
        add("terminal-deposit-click",()->{
            Object w=windowSession(activeSession.sessionId());Object c=call(screen(),"apiContext",w,null,0,0);
            clickAt(((Number)call(c,"contentX")).doubleValue()+8,((Number)call(c,"contentY")).doubleValue()+27);
        },()->carried()==0,()->{
            var value=server(()->{
                Container c=(Container)serverLevel().getBlockEntity(networkStoragePos);int n=0;
                for(int i=0;i<c.getContainerSize();i++)n+=c.getItem(i).getCount();return n;
            });addFirst("terminal-authoritative-deposit",()->{},value::isDone,()->check("terminal-deposit-conservation",value.join()==64),3);
            capture("toms-terminal-deposit");
        },8);
    }
    private static void addSophisticatedSettings(){
        add("backpack-source-lock",()->{},()->true,()->{
            var menu=currentSession().menu();
            check("backpack-held-item-cannot-be-picked-up",menu.slots.stream().filter(s->s.getItem().getItem()==registryValue("ITEM","sophisticatedbackpacks:backpack")).anyMatch(s->!s.mayPickup(mc.player)));
        },3);
        add("backpack-settings-click",()->{
            Object state=field(windowSession(activeSession.sessionId()),"apiState");
            Object components=call(field(state,"access"),"components");Object tabs=call(components,"sideTabs");
            Object tab=((List<?>)call(tabs,"children")).stream().filter(t->t.getClass().getSimpleName().equals("StorageSettingsTab")).findFirst().orElseThrow();
            clickAt(((Number)call(tab,"getX")).doubleValue()+8,((Number)call(tab,"getY")).doubleValue()+8);
        },()->sessions().stream().anyMatch(s->s.menu().getClass().getSimpleName().contains("Settings")),()->{
            activeSession=sessions().get(sessions().size()-1);capture("backpack-settings");
            Object state=field(windowSession(activeSession.sessionId()),"apiState");Object tabs=call(call(field(state,"access"),"components"),"sideTabs");
            List<?> children=(List<?>)call(tabs,"children");check("settings-tabs-present",children.size()>=4);
            for(Object child:children){
                if(!child.getClass().getSimpleName().contains("SettingsTab"))continue;
                String name=child.getClass().getSimpleName();
                addFirst("settings-tab-"+name,()->clickAt(((Number)call(child,"getX")).doubleValue()+8,((Number)call(child,"getY")).doubleValue()+8),()->true,()->capture("settings-"+name),5);
            }
        },15);
        add("backpack-settings-back",InWorldGameplayHarness::pressEscape,
            ()->sessions().stream().anyMatch(s->s.menu().getClass().getSimpleName().equals("BackpackContainer")),()->{
                activeSession=sessions().get(sessions().size()-1);check("settings-back-session-preserved",windowSession(activeSession.sessionId())!=null);capture("backpack-returned");
            },10);
    }
    private static void addRecipeBrowser(){
        add("recipe-browser-open",()->{closeWindows();InventoryDesktopScreen.openOrToggleJei(mc);},
            ()->windowKind("JEI")!=null,()->{
                var access=com.salts_inventory_update.api.client.recipebrowser.RecipeBrowserBridge.access();
                check("recipe-provider-available",access.isAvailable());record("recipe-provider","PASS",access.source().id());access.setFilterText("");
            },15);
        add("recipe-browser-item-tab",()->{
            var access=com.salts_inventory_update.api.client.recipebrowser.RecipeBrowserBridge.access();var tabs=access.tabs();Object w=windowKind("JEI");
            int index=-1;for(int i=0;i<tabs.size();i++)if(tabs.get(i).uid().toLowerCase(Locale.ROOT).contains("item")&&tabs.get(i).kind()==com.salts_inventory_update.api.client.recipebrowser.RecipeBrowserTabKind.INGREDIENTS){index=i;break;}
            if(index<0)throw new IllegalStateException("Item ingredient tab not found: "+tabs);
            double x=((Number)call(screen(),"jeiRecipeTopTabsX",w)).doubleValue()+index*(((Number)field(InventoryDesktopScreen.class,"JEI_RECIPE_TAB_SIZE")).intValue()+((Number)field(InventoryDesktopScreen.class,"JEI_RECIPE_TAB_GAP")).intValue())+8;
            double y=((Number)call(screen(),"jeiRecipeTopTabsY",w)).doubleValue()+8;clickAt(x,y);
            check("item-tab-selected",field(w,"jeiSelectedTabUid").equals(tabs.get(index).uid()));
        },()->true,()->{
                var access=com.salts_inventory_update.api.client.recipebrowser.RecipeBrowserBridge.access();capture("recipe-browser");
                access.setFilterText("crafting table");
            },8);
        add("recipe-browser-search",()->{},()->true,()->{
            var access=com.salts_inventory_update.api.client.recipebrowser.RecipeBrowserBridge.access();
            var selected=access.tabs().stream().filter(t->t.uid().equals(field(windowKind("JEI"),"jeiSelectedTabUid"))).findFirst().orElseThrow();
            check("recipe-search-nonempty",!access.filteredEntries(selected).isEmpty());capture("recipe-search");
            var entry=access.entryForItemStack(new ItemStack(Items.CRAFTING_TABLE));
            check("recipe-entry-present",entry!=null);call(screen(),"enterJeiRecipeView",windowKind("JEI"),entry,com.salts_inventory_update.api.client.recipebrowser.RecipeBrowserMode.RECIPES);
        },10);
        add("recipe-view",()->{},()->true,()->{
            var access=com.salts_inventory_update.api.client.recipebrowser.RecipeBrowserBridge.access();
            var entry=access.entryForItemStack(new ItemStack(Items.CRAFTING_TABLE));
            var cats=access.recipeCategories(entry,com.salts_inventory_update.api.client.recipebrowser.RecipeBrowserMode.RECIPES);
            check("recipe-categories",!cats.isEmpty());
            var recipes=access.recipes(entry,com.salts_inventory_update.api.client.recipebrowser.RecipeBrowserMode.RECIPES,cats.get(0));
            check("recipe-results",!recipes.isEmpty());capture("recipe-view");
            var recipe=recipes.get(0);
            if(access.canBookmarkRecipe(recipe)){
                boolean before=access.isRecipeBookmarked(recipe);access.toggleRecipeBookmark(recipe);
                check("recipe-bookmark-toggle",access.isRecipeBookmarked(recipe)!=before);access.toggleRecipeBookmark(recipe);
            }
        },10);
        add("recipe-browser-close",InWorldGameplayHarness::closeWindows,()->true,()->{},3);
        if(com.salts_inventory_update.api.integration.DesktopEnvironment.isModLoaded("modmenu"))
            add("modmenu-config-factory",()->setNativeScreen(com.salts_inventory_update.client.WindowedInventoryClient.createConfigScreen(null)),()->nativeScreen()!=null,()->capture("modmenu-configuration"),8);
    }

    private static void addChestInteractions() {
        add("chest-pickup", () -> clickSlot(0,left()), () -> carried()==32,
            () -> assertContainer("pickup-server",0,0,32), 3);
        add("chest-place", () -> clickSlot(1,left()), () -> carried()==0,
            () -> assertContainer("place-server",1,32,0), 3);
        add("chest-right-split", () -> clickSlot(1,right()), () -> carried()==16,
            () -> assertContainer("right-split-server",1,16,16), 3);
        add("chest-right-place-one", () -> clickSlot(2,right()), () -> carried()==15,
            () -> assertContainer("right-place-server",2,1,15), 3);
        add("chest-merge-remainder", () -> clickSlot(1,left()), () -> carried()==0,
            () -> { assertContainer("merge-server",1,31,0); capture("chest-interactions"); },3);
        addDrag("left-drag",left(),30,List.of(10,10,10));
        addDrag("right-drag",right(),30,List.of(1,1,1));
        addWindowControls();
        add("chest-close",InWorldGameplayHarness::closeWindows,()->sessions().isEmpty(),()->{},5);
    }
    private static void addWindowControls() {
        add("window-move",()->{
            Object w=windowSession(activeSession.sessionId());beforeX=number(w,"x");beforeY=number(w,"y");
            double x=beforeX+25,y=beforeY+5;
            mouse("mouseClicked",x,y,left(),0,0);mouse("mouseDragged",x+60,y+25,left(),60,25);mouse("mouseReleased",x+60,y+25,left(),0,0);
        },()->true,()->{
            Object w=windowSession(activeSession.sessionId());check("window-position-changed",number(w,"x")!=beforeX||number(w,"y")!=beforeY);capture("window-moved");
        },5);
        add("window-resize",()->{
            Object w=windowSession(activeSession.sessionId());beforeWidth=number(w,"width");beforeHeight=number(w,"height");
            double x=number(w,"x")+beforeWidth-2,y=number(w,"y")+beforeHeight-2;
            mouse("mouseClicked",x,y,left(),0,0);mouse("mouseDragged",x-54,y-18,left(),-54,-18);mouse("mouseReleased",x-54,y-18,left(),0,0);
        },()->true,()->{Object w=windowSession(activeSession.sessionId());check("window-size-changed",number(w,"width")!=beforeWidth||number(w,"height")!=beforeHeight);capture("window-resized");},5);
        add("window-lock",()->clickControl("LOCK"),()->Boolean.TRUE.equals(field(windowSession(activeSession.sessionId()),"locked")),()->capture("window-locked"),3);
        add("window-unlock",()->clickControl("LOCK"),()->!Boolean.TRUE.equals(field(windowSession(activeSession.sessionId()),"locked")),()->{},3);
        add("window-pin",()->clickControl("PIN"),()->!field(windowSession(activeSession.sessionId()),"pinMode").toString().equals("UNPINNED"),()->capture("window-pinned"),5);
        add("window-ghost-pin",()->clickControl("PIN"),()->field(windowSession(activeSession.sessionId()),"pinMode").toString().equals("GHOST_PINNED"),()->capture("window-ghost-pinned"),5);
        add("window-unpin",()->clickControl("PIN"),()->field(windowSession(activeSession.sessionId()),"pinMode").toString().equals("UNPINNED"),()->{},5);
        add("window-minimize",()->clickControl("MINIMIZE"),()->Boolean.TRUE.equals(field(windowSession(activeSession.sessionId()),"minimized")),()->capture("window-minimized"),3);
        add("window-restore",()->clickControl("MINIMIZE"),()->!Boolean.TRUE.equals(field(windowSession(activeSession.sessionId()),"minimized")),()->capture("window-restored"),3);
    }
    private static void addDrag(String label,int button,int count,List<Integer> expected) {
        add(label+"-seed",()->server(()->{
            var menu=serverMenu();for(int i=0;i<4;i++)menu.getSlot(i).set(ItemStack.EMPTY);
            menu.getSlot(0).set(new ItemStack(Items.DIRT,count));menu.broadcastFullState();
            player().inventoryMenu.setCarried(ItemStack.EMPTY); return null;
        }),InWorldGameplayHarness::taskDone,()->{},10);
        add(label+"-pickup",()->clickSlot(0,left()),()->carried()==count,()->{},3);
        add(label+"-preview",()->{
            var first=slotPoint(1); mouse("mouseClicked",first[0],first[1],button,0,0);
            double[] previous=first;
            for(int i=2;i<=3;i++) {var next=slotPoint(i); mouse("mouseDragged",next[0],next[1],button,next[0]-previous[0],next[1]-previous[1]);previous=next;}
        },()->field(screen(),"dragDistribution")!=null,()->capture(label+"-preview"),3);
        add(label+"-commit",()->{var p=slotPoint(3);mouse("mouseReleased",p[0],p[1],button,0,0);},
            ()->DesktopContainerClient.canDispatchOptimisticMutationNow(),()->{
                var actual=server(()->{
                    var c=serverMenu();
                    return List.of(c.getSlot(1).getItem().getCount(),c.getSlot(2).getItem().getCount(),c.getSlot(3).getItem().getCount(),player().inventoryMenu.getCarried().getCount());
                });
                addFirst(label+"-authoritative",()->{},actual::isDone,()->{
                    List<Integer> value=(List<Integer>)actual.join();
                    check(label+"-slot-distribution",value.subList(0,3).equals(expected));
                    check(label+"-item-conservation",value.stream().mapToInt(Integer::intValue).sum()==count);
                    capture(label+"-committed");
                },3);
            },8);
        add(label+"-reset-cursor",()->{
            if(carried()>0) clickSlot(0,left());
        },()->carried()==0,()->{},4);
    }

    private static void tick(Minecraft client) {
        if(finished||!clientReady) return;
        mc=client; elapsedTicks++;
        if(Files.exists(output.resolve("abort.request"))){record("suite-aborted","FAIL","Explicit QA abort requested");finish();return;}
        if(elapsedTicks<15) return;
        // Creating a world while the initial splash overlay is still present leaves
        // Minecraft's synchronous world-load loop waiting for an unrendered overlay.
        if(active==null && mc.player==null && overlay()!=null) return;
        if(System.currentTimeMillis()-startedAt>20*60*1000L){record("suite-timeout","FAIL","20-minute deadline exceeded");finish();return;}
        try {
            if(active==null) {
                if(STEPS.isEmpty()){
                    if (SCENARIO.equals("ae2-atlas")) {
                        Path atlas = output.resolve("atlas");
                        if (Files.exists(atlas.resolve("failed.txt"))) {
                            record("ae2-atlas", "FAIL", Files.readString(atlas.resolve("failed.txt"))); finish();
                        } else if (Files.exists(atlas.resolve("completed.txt"))) {
                            if (System.getenv("SIU_AE2_ATLAS_FOCUS") != null) {
                                check("ae2-atlas-focused-container", Files.readString(atlas.resolve("completed.txt")).contains("Registered containers: 1"));
                                finish(); return;
                            }
                            check("ae2-atlas-40-containers", Files.readString(atlas.resolve("completed.txt")).contains("Registered containers: 40"));
                            call(ClassFor("com.salts_inventory_update.compat.ae2.qa.NativeAtlasCoverage"),
                                    "verify", atlas, VersionInfo.MINECRAFT_VERSION);
                            check("ae2-atlas-native-presentations", true);
                            finish();
                        } else call(ClassFor("com.salts_inventory_update.compat.ae2.qa.DesktopAtlasQa"), "tick", mc);
                    } else if(PENDING_CAPTURES.get()==0)finish();
                    return;
                }
                active=STEPS.removeFirst(); stepStarted=System.currentTimeMillis();settle=-1;
                SaltsInventoryUpdate.LOGGER.info("SIU_GAMEPLAY_STEP name={} status=START",active.name());active.start().run();
            }
            boolean ready=active.ready().getAsBoolean();
            if(ready) {
                if(settle<0) settle=active.settleTicks();
                if(settle--<=0) {
                    active.verify().run();record(active.name(),"PASS","completed in-world stage");active=null;
                }
            } else {
                settle=-1;
                if(System.currentTimeMillis()-stepStarted>45000L) throw new IllegalStateException("Stage timed out; screen="+(nativeScreen()==null?"none":nativeScreen().getClass().getName())+", sessions="+sessions().size()+", carried="+carried());
            }
        } catch(Throwable e) {
            boolean requiredHandshakeFailed = active != null && active.name().equals("desktop-handshake");
            SaltsInventoryUpdate.LOGGER.error("SIU_GAMEPLAY_STEP failure "+(active==null?"unknown":active.name()),e);
            record(active==null?"suite":active.name(),"FAIL",e.toString());
            try{capture("failure-"+(active==null?"suite":active.name()));}catch(Throwable ignored){}
            active=null;
            if(mc.player==null || requiredHandshakeFailed || SCENARIO.equals("ae2-atlas")){finish();}
        }
    }
    private static void createWorld() {
        mc.options.pauseOnLostFocus=false;
        try {call(mc.getWindow(),"setWindowed",1280,SCENARIO.equals("ae2-atlas") ? 960 : 900);} catch(Throwable ignored){}
        mc.options.guiScale().set(2);
        SaltsInventoryConfig.update(config->{config.enableMod=true;config.openUnlocked=true;config.allowResizing=true;config.enableGhostPins=true;config.minimizableWindows=true;config.expandableInventory=true;});
        if (SCENARIO.equals("ae2-atlas")) SaltsInventoryConfig.update(config -> {
            config.openUnlocked=false; config.minimizableWindows=false; config.expandableInventory=false;
            config.hasSeenInstructionsWindow=true;
        });
        String name="siu-qa-"+System.currentTimeMillis();
        Object config=field(ClassFor("net.minecraft.world.level.WorldDataConfiguration"),"DEFAULT");
        Object settings;
        try{
            Object difficulty=construct(ClassFor("net.minecraft.world.level.LevelSettings$DifficultySettings"),enumValue("net.minecraft.world.Difficulty","PEACEFUL"),false,false);
            settings=construct(ClassFor("net.minecraft.world.level.LevelSettings"),"SIU disposable QA",enumValue("net.minecraft.world.level.GameType","SURVIVAL"),difficulty,true,config);
        }catch(IllegalStateException|IllegalArgumentException legacy){
            Object rules;
            try { rules=construct(ClassFor("net.minecraft.world.level.GameRules")); }
            catch(IllegalStateException modern) {
                rules=construct(ClassFor("net.minecraft.world.level.gamerules.GameRules"),call(config,"enabledFeatures"));
            }
            settings=construct(ClassFor("net.minecraft.world.level.LevelSettings"),"SIU disposable QA",enumValue("net.minecraft.world.level.GameType","SURVIVAL"),false,enumValue("net.minecraft.world.Difficulty","PEACEFUL"),true,rules,config);
        }
        Object options=construct(ClassFor("net.minecraft.world.level.levelgen.WorldOptions"),424242L,false,false);
        Function<Object,Object> dimensions=registries->{
            try {
                Object key=field(ClassFor("net.minecraft.core.registries.Registries"),"WORLD_PRESET");
                Object lookup;
                try{lookup=call(registries,"lookupOrThrow",key);}catch(Throwable ignored){lookup=call(registries,"registryOrThrow",key);}
                Object preset;
                try{preset=call(call(lookup,"getOrThrow",field(ClassFor("net.minecraft.world.level.levelgen.presets.WorldPresets"),"FLAT")),"value");}
                catch(Throwable ignored){preset=call(lookup,"getOrThrow",field(ClassFor("net.minecraft.world.level.levelgen.presets.WorldPresets"),"FLAT"));}
                return call(preset,"createWorldDimensions");
            } catch(Throwable failure) { return call(ClassFor("net.minecraft.world.level.levelgen.presets.WorldPresets"),"createNormalWorldDimensions",registries); }
        };
        Object flows=call(mc,"createWorldOpenFlows");
        try{call(flows,"createFreshLevel",name,settings,options,dimensions,null);}catch(IllegalArgumentException missing){call(flows,"createFreshLevel",name,settings,options,dimensions);}
    }

    private static <T> CompletableFuture<T> server(Supplier<T> action) {
        CompletableFuture<T> future=new CompletableFuture<>();task=future;
        mc.getSingleplayerServer().execute(()->{try{future.complete(action.get());}catch(Throwable error){future.completeExceptionally(error);}});
        return future;
    }
    private static net.minecraft.server.level.ServerPlayer player(){return mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);}
    private static net.minecraft.server.level.ServerLevel serverLevel(){return serverLevel(player());}
    private static net.minecraft.server.level.ServerLevel serverLevel(Object p){try{return (net.minecraft.server.level.ServerLevel)call(p,"serverLevel");}catch(IllegalArgumentException modern){return (net.minecraft.server.level.ServerLevel)call(p,"level");}}
    private static boolean taskDone(){if(task==null)return false;if(task.isCompletedExceptionally())task.join();return task.isDone();}
    private static void command(String value){var s=mc.getSingleplayerServer();s.getCommands().performPrefixedCommand(s.createCommandSourceStack().withSuppressedOutput(),value);}
    private static InventoryDesktopScreen screen(){return InventoryDesktopScreen.current(mc);}
    private static Object nativeScreen(){try{return field(mc,"screen");}catch(IllegalArgumentException modern){return call(field(mc,"gui"),"screen");}}
    private static void setNativeScreen(Object target){try{call(mc,"setScreen",target);}catch(IllegalArgumentException modern){call(field(mc,"gui"),"setScreen",target);}}
    private static Object overlay(){try{return call(mc,"getOverlay");}catch(IllegalArgumentException modern){return call(field(mc,"gui"),"overlay");}}
    private static Object renderTarget(){try{return call(mc,"getMainRenderTarget");}catch(IllegalArgumentException modern){return call(field(mc,"gameRenderer"),"mainRenderTarget");}}
    private static void scroll(double x,double y,double amount){try{call(screen(),"mouseScrolled",x,y,0D,amount);}catch(IllegalArgumentException legacy){call(screen(),"mouseScrolled",x,y,amount);}}
    private static List<DesktopContainerSession> sessions(){var s=screen();return s==null?List.of():(List<DesktopContainerSession>)field(s,"sessions");}
    private static Object windowKind(String kind){var s=screen();if(s==null)return null;for(Object w:(List<?>)field(s,"windows"))if(field(w,"kind").toString().equals(kind))return w;return null;}
    private static DesktopContainerSession currentSession(){
        if(activeSession==null)throw new IllegalStateException("No active session");
        return sessions().stream().filter(s->s.sessionId()==activeSession.sessionId()).findFirst().orElseThrow();
    }
    private static Object windowSession(int id){var s=screen();if(s==null)return null;for(Object w:(List<?>)field(s,"windows")){Object session=field(w,"session");if(session instanceof DesktopContainerSession ds&&ds.sessionId()==id)return w;}return null;}
    private static boolean infiniteItems(){try{return (Boolean)call(mc.gameMode,"hasInfiniteItems");}catch(IllegalArgumentException modern){return (Boolean)call(mc.player,"hasInfiniteMaterials");}}
    private static int carried(){var s=screen();return s==null?0:((ItemStack)field(s,"sharedCarried")).getCount();}
    private static void closeWindows(){var s=screen();if(s!=null)InventoryDesktopScreen.permanentlyCloseAllOpenWindows(mc);else setNativeScreen(null);}
    private static void clickSlot(int slot,int button){var p=slotPoint(slot);mouse("mouseClicked",p[0],p[1],button,0,0);mouse("mouseReleased",p[0],p[1],button,0,0);}
    private static void clickAt(double x,double y){mouse("mouseClicked",x,y,left(),0,0);mouse("mouseReleased",x,y,left(),0,0);}
    private static int number(Object object,String name){return ((Number)field(object,name)).intValue();}
    private static void clickControl(String name){
        Object w=windowSession(activeSession.sessionId());Object layout=call(screen(),"titleBarLayout",w);
        List<?> rects=(List<?>)call(layout,"controls");Object selected=null;
        for(Object rect:rects) if(call(rect,"control").toString().equals(name))selected=rect;
        if(selected==null){
            for(Object rect:rects)if(call(rect,"control").toString().equals("ELLIPSIS")){
                double x=((Number)call(rect,"x")).doubleValue()+4,y=((Number)call(rect,"y")).doubleValue()+4;
                mouse("mouseClicked",x,y,left(),0,0);mouse("mouseReleased",x,y,left(),0,0);
            }
            for(Object rect:(List<?>)call(screen(),"popupControlRects",w,call(screen(),"popupRect",w)))if(call(rect,"control").toString().equals(name))selected=rect;
        }
        if(selected==null)throw new IllegalStateException("Control not exposed: "+name);
        double x=((Number)call(selected,"x")).doubleValue()+((Number)call(selected,"width")).doubleValue()/2,
            y=((Number)call(selected,"y")).doubleValue()+((Number)call(selected,"height")).doubleValue()/2;
        mouse("mouseClicked",x,y,left(),0,0);mouse("mouseReleased",x,y,left(),0,0);
    }
    private static double[] slotPoint(int slot){
        if(activeSession==null)throw new IllegalStateException("No active desktop session");
        return slotPoint(activeSession.sessionId(),slot,windowSession(activeSession.sessionId()));
    }
    private static void storeInPlayer(){
        if(windowKind("INVENTORY")==null)InventoryDesktopScreen.openOrToggleInventory(mc);
        int empty=-1;for(int i=9;i<36;i++)if(mc.player.inventoryMenu.getSlot(i).getItem().isEmpty()){empty=i;break;}
        if(empty<0)throw new IllegalStateException("Fixture requires an empty player slot");
        var point=slotPoint(0,empty,windowKind("INVENTORY"));clickAt(point[0],point[1]);
    }
    private static double[] slotPoint(int sessionId,int slot,Object w){
        var s=screen();if(s==null||w==null)throw new IllegalStateException("No visible desktop window");
        int x=((Number)field(w,"x")).intValue(),y=((Number)field(w,"y")).intValue(),width=((Number)field(w,"width")).intValue(),height=((Number)field(w,"height")).intValue();
        for(int py=y-40;py<y+height+40;py+=4)for(int px=x-80;px<x+width+120;px+=4){
            Object hit=call(s,"slotAt",(double)px,(double)py);
            if(hit!=null&&((Number)call(hit,"sessionId")).intValue()==sessionId&&((Number)call(hit,"slotId")).intValue()==slot)
                return new double[]{((Number)call(hit,"x")).doubleValue()+8,((Number)call(hit,"y")).doubleValue()+8};
        }
        throw new IllegalStateException("Visible slot not found: "+slot);
    }
    private static void mouse(String name,double x,double y,int button,double dx,double dy){
        Object s=screen();call(s,"mouseMoved",x,y);
        try{
            if(name.equals("mouseDragged"))call(s,name,x,y,button,dx,dy);else call(s,name,x,y,button);
        }catch(IllegalArgumentException modern){
            Object info=construct(ClassFor("net.minecraft.client.input.MouseButtonInfo"),button,0);
            Object event=construct(ClassFor("net.minecraft.client.input.MouseButtonEvent"),x,y,info);
            if(name.equals("mouseDragged"))call(s,name,event,dx,dy);else if(name.equals("mouseClicked"))call(s,name,event,false);else call(s,name,event);
        }
    }
    private static int left(){try{return ((Number)field(ClassFor("com.mojang.blaze3d.platform.InputConstants"),"MOUSE_BUTTON_LEFT")).intValue();}catch(Throwable ignored){return 0;}}
    private static int right(){try{return ((Number)field(ClassFor("com.mojang.blaze3d.platform.InputConstants"),"MOUSE_BUTTON_RIGHT")).intValue();}catch(Throwable ignored){return 1;}}
    private static void pressEscape(){try{call(screen(),"keyPressed",256,0,0);}catch(IllegalArgumentException modern){call(screen(),"keyPressed",construct(ClassFor("net.minecraft.client.input.KeyEvent"),256,0,0));}}
    private static void assertContainer(String name,int index,int count,int cursor){
        var future=server(()->{
            var container=(Container)serverLevel().getBlockEntity(origin);
            return List.of(container.getItem(index).getCount(),player().inventoryMenu.getCarried().getCount());
        });
        addFirst(name,()->{},future::isDone,()->{List<Integer> actual=(List<Integer>)future.join();check(name,actual.equals(List.of(count,cursor)));},2);
    }
    private static void capture(String label){
        try {
            Path p=output.resolve("screenshots").resolve(label.replaceAll("[^A-Za-z0-9._-]","-")+".png");
            try{
                Object image=call(ClassFor("net.minecraft.client.Screenshot"),"takeScreenshot",renderTarget());
                writeCapture(image,p,label);
            }catch(IllegalArgumentException modern){
                PENDING_CAPTURES.incrementAndGet();
                Consumer<Object> callback=image->{try{writeCapture(image,p,label);}catch(Throwable e){record(label+"-framebuffer","FAIL",e.toString());}finally{PENDING_CAPTURES.decrementAndGet();}};
                call(ClassFor("net.minecraft.client.Screenshot"),"takeScreenshot",renderTarget(),callback);
            }
        }catch(Throwable e){throw new IllegalStateException("Framebuffer capture failed",e);}
    }
    private static void writeCapture(Object image,Path p,String label){
        try{try{call(image,"writeToFile",p);}catch(IllegalArgumentException legacy){call(image,"writeToFile",p.toFile());}}
        finally{call(image,"close");}
        Map<String,Object> result=new LinkedHashMap<>();result.put("name",label+"-framebuffer");result.put("status","CAPTURED");result.put("path",p.toString());RESULTS.add(result);
    }
    private static void check(String name,boolean condition){record(name,condition?"PASS":"FAIL",condition?"assertion true":"assertion false");}
    private static void record(String name,String status,String details){
        RESULTS.add(new LinkedHashMap<>(Map.of("name",name,"status",status,"details",details,"elapsedMs",System.currentTimeMillis()-startedAt)));
        SaltsInventoryUpdate.LOGGER.info("SIU_GAMEPLAY_CHECK name={} status={} details={}",name,status,details);
    }
    private static void finish(){
        finished=true;
        long failures=RESULTS.stream().filter(r->"FAIL".equals(r.get("status"))).count();
        Map<String,Object> report=new LinkedHashMap<>();report.put("scenario",SCENARIO);report.put("minecraft",VersionInfo.MINECRAFT_VERSION);report.put("loader",System.getenv("SIU_FUNCTIONAL_TEST_LOADER"));report.put("results",RESULTS);report.put("failures",failures);report.put("elapsedMs",System.currentTimeMillis()-startedAt);
        report.put("coreCodeSource",SaltsInventoryUpdate.class.getProtectionDomain().getCodeSource().getLocation().toString());
        report.put("runDirectory",Path.of("").toAbsolutePath().toString());
        report.put("loadedMods",loadedMods());
        try{Files.writeString(output.resolve("results.json"),new GsonBuilder().setPrettyPrinting().create().toJson(report));}catch(Exception e){SaltsInventoryUpdate.LOGGER.error("Cannot save QA result",e);}
        SaltsInventoryUpdate.LOGGER.info("SIU_GAMEPLAY_SUMMARY status={} failures={} records={} output={}",failures==0?"PASS":"FAIL",failures,RESULTS.size(),output);
        mc.stop();
    }
    private static Map<String,String> loadedMods(){
        Map<String,String> result=new TreeMap<>();
        try{
            Object loader=call(ClassFor("net.fabricmc.loader.api.FabricLoader"),"getInstance");
            for(Object c:(Collection<?>)call(loader,"getAllMods")){Object metadata=call(c,"getMetadata");result.put(call(metadata,"getId").toString(),call(call(metadata,"getVersion"),"getFriendlyString").toString());}
        }catch(Throwable absentFabric){
            try{Object list;try{list=call(ClassFor("net.neoforged.fml.ModList"),"get");}catch(Throwable absentNeo){list=call(ClassFor("net.minecraftforge.fml.ModList"),"get");}
                for(Object m:(List<?>)call(list,"getMods"))result.put(call(m,"getModId").toString(),call(m,"getVersion").toString());
            }catch(Throwable e){result.put("discovery-error",e.toString());}
        }return result;
    }
    private static void add(String n,Runnable s,BooleanSupplier r,Runnable v,int t){STEPS.addLast(new Step(n,s,r,v,t));}
    private static void addFirst(String n,Runnable s,BooleanSupplier r,Runnable v,int t){STEPS.addFirst(new Step(n,s,r,v,t));}
    private static Object registryValue(String registry,String id){Object values=field(ClassFor("net.minecraft.core.registries.BuiltInRegistries"),registry);for(Object value:(Iterable<?>)values)if(id.equals(call(values,"getKey",value).toString()))return value;return null;}
    private static Class<?> ClassFor(String name){try{return Class.forName(name);}catch(Exception e){throw new IllegalStateException(e);}}
    private static Object enumValue(String type,String name){return Enum.valueOf((Class)ClassFor(type),name);}
    private static Object construct(Class<?> type,Object...args){for(Constructor<?> c:type.getDeclaredConstructors())if(matches(c.getParameterTypes(),args)){try{c.setAccessible(true);return c.newInstance(args);}catch(ReflectiveOperationException e){throw new IllegalStateException(e);}}throw new IllegalArgumentException("Constructor not found: "+type);}
    private static Object field(Object target,String name){Class<?> cls=target instanceof Class<?> type?type:target.getClass();for(Class<?> c=cls;c!=null;c=c.getSuperclass())try{Field f=c.getDeclaredField(name);f.setAccessible(true);return f.get(target instanceof Class<?>?null:target);}catch(NoSuchFieldException e){}catch(Exception e){throw new IllegalStateException(e);}throw new IllegalArgumentException("Field not found: "+cls+"."+name);}
    private static Object call(Object target,String name,Object...args){
        Class<?> cls=target instanceof Class<?> type?type:target.getClass();
        for(Class<?> c=cls;c!=null;c=c.getSuperclass())for(Method m:c.getDeclaredMethods())if(m.getName().equals(name)&&matches(m.getParameterTypes(),args))try{m.setAccessible(true);return m.invoke(target instanceof Class<?>?null:target,args);}catch(InvocationTargetException e){throw new IllegalStateException(e.getCause());}catch(Exception e){throw new IllegalStateException(e);}
        for(Method m:cls.getMethods())if(m.getName().equals(name)&&matches(m.getParameterTypes(),args))try{return m.invoke(target instanceof Class<?>?null:target,args);}catch(Exception e){throw new IllegalStateException(e);}
        throw new IllegalArgumentException("Method not found: "+cls+"."+name+"/"+args.length);
    }
    private static boolean matches(Class<?>[] types,Object[] args){if(types.length!=args.length)return false;for(int i=0;i<types.length;i++){if(args[i]==null){if(types[i].isPrimitive())return false;}else if(!box(types[i]).isInstance(args[i]))return false;}return true;}
    private static Class<?> box(Class<?> c){if(!c.isPrimitive())return c;if(c==int.class)return Integer.class;if(c==long.class)return Long.class;if(c==double.class)return Double.class;if(c==float.class)return Float.class;if(c==boolean.class)return Boolean.class;if(c==byte.class)return Byte.class;if(c==short.class)return Short.class;if(c==char.class)return Character.class;return c;}
}
