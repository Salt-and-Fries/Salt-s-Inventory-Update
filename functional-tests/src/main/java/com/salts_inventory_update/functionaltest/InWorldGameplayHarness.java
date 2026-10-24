package com.salts_inventory_update.functionaltest;

import com.google.gson.GsonBuilder;
import com.salts_inventory_update.SaltsInventoryUpdate;
import com.salts_inventory_update.VersionInfo;
import com.salts_inventory_update.client.InventoryDesktopScreen;
import com.salts_inventory_update.client.DesktopContainerClient;
import com.salts_inventory_update.client.DesktopContainerSession;
import com.salts_inventory_update.client.SaltsInventoryConfig;
import com.salts_inventory_update.api.client.desktop.*;
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
    private static Runnable clearQaToasts=()->{};
    private static com.salts_inventory_update.api.client.recipebrowser.RecipeBrowserAccess tooltipProbeOriginal;
    private static final java.util.concurrent.atomic.AtomicInteger TOOLTIP_PROBE_DRAWS=new java.util.concurrent.atomic.AtomicInteger();
    private static final java.util.concurrent.atomic.AtomicInteger PENDING_CAPTURES=new java.util.concurrent.atomic.AtomicInteger();
    private static final Map<String,Consumer<Path>> CAPTURE_CHECKS=new java.util.concurrent.ConcurrentHashMap<>();
    private static final String SCENARIO = Optional.ofNullable(System.getenv("SIU_QA_SCENARIO")).orElse("core");
    private static final boolean TOMS_PICKUP_ONLY = "true".equalsIgnoreCase(System.getenv("SIU_QA_TOMS_PICKUP_ONLY"));
    private static final boolean MOUSE_TWEAKS_ONLY = "true".equalsIgnoreCase(System.getenv("SIU_QA_MOUSE_TWEAKS_ONLY"));
    private static final boolean MT_ABSENT = "true".equalsIgnoreCase(System.getenv("SIU_QA_MOUSE_TWEAKS_ABSENT"));
    private static final boolean MANUAL_MOUSE_TWEAKS = "true".equalsIgnoreCase(System.getenv("SIU_QA_MANUAL_MOUSE_TWEAKS"));
    private static final boolean HUD_MOVEMENT_ONLY = "true".equalsIgnoreCase(System.getenv("SIU_QA_HUD_MOVEMENT_ONLY"));
    private static final boolean HUD_VISIBILITY_ONLY = "true".equalsIgnoreCase(System.getenv("SIU_QA_HUD_VISIBILITY_ONLY"));
    private static final boolean RAISED_ONLY = "true".equalsIgnoreCase(System.getenv("SIU_QA_RAISED_ONLY"));
    private static final boolean SPATIAL_ONLY = "true".equalsIgnoreCase(System.getenv("SIU_QA_SPATIAL_ONLY"));
    private static final boolean SPATIAL_REPORT = "true".equalsIgnoreCase(System.getenv("SIU_QA_SPATIAL_REPORT"));
    private static final boolean WINDOW_WALLS_ONLY = "true".equalsIgnoreCase(System.getenv("SIU_QA_WINDOW_WALLS_ONLY"));
    private static final boolean INVENTORY_SORT_ONLY = "true".equalsIgnoreCase(System.getenv("SIU_QA_INVENTORY_SORT_ONLY"));
    private static final boolean FULL_CORE = "true".equalsIgnoreCase(System.getenv("SIU_QA_FULL_CORE"));
    private static final String ACCESSORY = Optional.ofNullable(System.getenv("SIU_QA_ACCESSORY")).orElse("");
    private static final boolean WINDOW_CONTROLS_ONLY = INVENTORY_SORT_ONLY || WINDOW_WALLS_ONLY || SPATIAL_ONLY || HUD_MOVEMENT_ONLY || HUD_VISIBILITY_ONLY || RAISED_ONLY || MOUSE_TWEAKS_ONLY || "true".equalsIgnoreCase(System.getenv("SIU_QA_WINDOW_CONTROLS_ONLY"));
    private static final boolean KEYBINDS_ONLY = "true".equalsIgnoreCase(System.getenv("SIU_QA_KEYBINDS_ONLY"));
    private static final boolean BROWSER_SEARCH_ONLY = "true".equalsIgnoreCase(System.getenv("SIU_QA_BROWSER_SEARCH_ONLY"));

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
        if (!WINDOW_CONTROLS_ONLY && !Boolean.parseBoolean(System.getenv("SIU_QA_INTEGRATION_ONLY"))) {
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
        }
        if (!WINDOW_CONTROLS_ONLY) addIntegrations();
        if (!TOMS_PICKUP_ONLY && !WINDOW_CONTROLS_ONLY) {
        add("resource-pack-reload", () -> {
            closeWindows(); task=(CompletableFuture<?>)call(mc,"reloadResourcePacks");
        }, () -> taskDone() && overlay()==null,
            () -> check("desktop-ready-after-resource-reload",DesktopContainerClient.canUseServerSessions()), 15);
        add("inventory-after-resource-reload", () -> InventoryDesktopScreen.openOrToggleInventory(mc),
            () -> windowKind("INVENTORY")!=null, () -> capture("inventory-after-resource-reload"), 10);
        add("final-state-and-save", () -> { closeWindows(); server(() -> { player().inventoryMenu.setCarried(ItemStack.EMPTY); return null; }); },
            InWorldGameplayHarness::taskDone, () -> capture("final-world"), 10);
        }
        }
        if ("true".equalsIgnoreCase(System.getenv("SIU_QA_CHEST_CLOSE_ONLY"))) {
            // Keep only world creation, handshake and the disposable block fixture.
            while (STEPS.size() > 3) STEPS.removeLast();
            addChestCloseRegression();
        }
        if (WINDOW_CONTROLS_ONLY) {
            while (STEPS.size() > 3) STEPS.removeLast();
            if (INVENTORY_SORT_ONLY) addInventorySortRegression();
            else if (WINDOW_WALLS_ONLY) {
                addWindowWallsRegression();
                if("true".equalsIgnoreCase(System.getenv("SIU_QA_SPATIAL_WITH_REGRESSION"))&&wallsSpatial()) addSpatialRegression();
            }
            else if (SPATIAL_ONLY) {
                if("true".equalsIgnoreCase(System.getenv("SIU_QA_SPATIAL_COMPLETE"))) {
                    addSpatialTransitions();
                    addWindowWallsRegression();
                    addSpatialRegression();
                }
                else if("true".equalsIgnoreCase(System.getenv("SIU_QA_SPATIAL_TRANSITIONS"))) {
                    addSpatialTransitions();
                    if("true".equalsIgnoreCase(System.getenv("SIU_QA_SPATIAL_WITH_REGRESSION")))addSpatialRegression();
                }
                else if("true".equalsIgnoreCase(System.getenv("SIU_QA_SPATIAL_HOTBAR_PROBE"))) addSpatialHotbarProbe();
                else if("true".equalsIgnoreCase(System.getenv("SIU_QA_SPATIAL_FINDINGS"))) addSpatialFindingsRegression();
                else addSpatialRegression();
            }
            else if (HUD_MOVEMENT_ONLY || HUD_VISIBILITY_ONLY) addHudVisibilityRegression();
            else if (RAISED_ONLY) addRaisedHudRegression();
            else if (MANUAL_MOUSE_TWEAKS) addManualMouseTweaksFixture();
            else if (MOUSE_TWEAKS_ONLY) addMouseTweaksRegression();
            else if (KEYBINDS_ONLY) addKeybindsRegression();
            else addWindowControlsRegression();
        }
        if ("true".equalsIgnoreCase(System.getenv("SIU_QA_MT_WHEEL_ONLY"))) {
            var allowed=java.util.Set.of("world-create-and-connect","desktop-handshake","world-fixture","mt-configure","fixture-mt-chest","open-mt-chest","mt-api-detection","mt-seed-native","mt-lmb-collect","mt-seed-shift-lmb","mt-shift-lmb","mt-seed-wheel","mt-wheel-push","mt-wheel-pull","mt-wheel-fractional","mt-wheel-disabled","mt-final-cleanup");
            STEPS.removeIf(step -> !allowed.contains(step.name()));
        }
        if (BROWSER_SEARCH_ONLY) {
            while (STEPS.size() > 3) STEPS.removeLast();
            addAncientRemnantsSearchRegression();
        }
        if(SPATIAL_ONLY && "true".equalsIgnoreCase(System.getenv("SIU_QA_SPATIAL_CONTROLS_ONLY"))) {
            var setup=Set.of("world-create-and-connect","desktop-handshake","world-fixture","spatial-configure","fixture-spatial-chest","open-spatial-chest","spatial-inventory-window","spatial-third-person-panel","spatial-hidden-selection","spatial-close");
            STEPS.removeIf(s->!setup.contains(s.name())&&!s.name().startsWith("spatial-movement-")&&!s.name().startsWith("spatial-gameplay-")&&!s.name().startsWith("spatial-rebound-")&&!s.name().startsWith("spatial-hold-"));
        }
        if(SPATIAL_REPORT && "true".equalsIgnoreCase(System.getenv("SIU_QA_SPATIAL_SCALE_ONLY"))) {
            var allowed=Set.of("world-create-and-connect","desktop-handshake","world-fixture","spatial-configure","fixture-spatial-chest","open-spatial-chest","spatial-inventory-window","spatial-third-person-panel","spatial-report-gui-scale-three","spatial-report-gui-scale-two","spatial-close");
            STEPS.removeIf(s->!allowed.contains(s.name()));
        }
        if (!ACCESSORY.isEmpty()) {
            while (STEPS.size() > 3) STEPS.removeLast();
            if (ACCESSORY.equals("curios")) addCuriosRegression();
            else if (ACCESSORY.equals("trinkets")) addTrinketsRegression();
            else throw new IllegalArgumentException("Unknown native accessory fixture");
        }
        if (FULL_CORE) {
            if (!SCENARIO.equals("core") || WINDOW_CONTROLS_ONLY)
                throw new IllegalStateException("Complete Core QA requires the unrestricted standalone Core scenario");
            // Execute the existing feature regressions after the complete native-menu suite.
            // Each group restores its own fixture; no tests are selected out of the full run.
            addChestCloseRegression();
            addWindowControlsRegression();
            addKeybindsRegression();
            addInventorySortRegression();
            addHudVisibilityRegression();
            addWindowWallsRegression();
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

    private static DesktopWindowHandle accessoryCharacter() {
        return DesktopClientApi.currentWorkspace().orElseThrow().windows().stream()
            .filter(w -> w.kind()==DesktopWorkspaceWindow.Kind.CHARACTER).findFirst().orElseThrow().handle();
    }
    private static void accessoryOpen() {
        InventoryDesktopScreen.openOrToggleInventory(mc);
        InventoryDesktopScreen.openOrToggleCharacter(mc);
        accessoryCharacter().moveTo(20,200);
        DesktopClientApi.currentWorkspace().orElseThrow().windows().stream()
            .filter(w -> w.kind()==DesktopWorkspaceWindow.Kind.PLAYER_INVENTORY)
            .forEach(w -> w.handle().moveTo(330,250));
    }
    private static void accessoryPlayerClick(int slot,int button) {
        var p=slotPoint(0,slot,windowKind("INVENTORY"));
        spatialMove(p[0]*mc.getWindow().getGuiScale(),p[1]*mc.getWindow().getGuiScale(),0,0);
        mouse("mouseClicked",p[0],p[1],button,0,0);mouse("mouseReleased",p[0],p[1],button,0,0);
    }
    private static void accessorySeed() {
        player().getInventory().clearContent();player().inventoryMenu.setCarried(ItemStack.EMPTY);
        player().getInventory().setItem(9,new ItemStack(Items.DIAMOND,4));
        player().getInventory().setItem(10,new ItemStack(Items.IRON_INGOT,3));
        player().inventoryMenu.broadcastFullState();mtSync();
    }
    private static Object curiosInventory(Object p) {
        Object maybe=call(ClassFor("top.theillusivec4.curios.api.CuriosApi"),"getCuriosInventory",p);
        if(!(maybe instanceof Optional<?>))maybe=call(maybe,"resolve");
        return ((Optional<?>)maybe).orElseThrow();
    }
    private static Object curiosGroup(Object p) {
        return ((Optional<?>)call(curiosInventory(p),"getStacksHandler","ring")).orElseThrow();
    }
    private static DesktopContainerSession curiosSession() {
        return sessions().stream().filter(s -> s.menu().getClass().getSimpleName().equals("CuriosDesktopMenu")).findFirst().orElse(null);
    }
    private static void curiosActivate() {
        activeSession=curiosSession();activeWindow=windowSession(activeSession.sessionId());
    }
    private static int curiosFirstSlot() {
        Object slot=((List<?>)call(currentSession().menu(),"displayedSlots")).get(0);
        return (slot instanceof Slot nativeSlot ? nativeSlot : (Slot)field(slot,"nativeSlot")).index;
    }
    private static void curiosCapacity(int count) {
        Object group=curiosGroup(player());
        String modifierClass="net.minecraft.world.entity.ai.attributes.AttributeModifier";
        Object id,modifier;
        if(VersionInfo.MINECRAFT_VERSION.equals("1.20.1")) {
            id=UUID.fromString("f773e7bb-a3e6-4e6d-b1b1-ac19cfc3bd52");
            modifier=construct(ClassFor(modifierClass),id,"Salts disposable capacity QA",(double)count-8,
                enumValue(modifierClass+"$Operation","ADDITION"));
        } else {
            Class<?> identifier;
            try{identifier=ClassFor("net.minecraft.resources.Identifier");}
            catch(IllegalStateException legacy){identifier=ClassFor("net.minecraft.resources.ResourceLocation");}
            id=call(identifier,"fromNamespaceAndPath","salts_gameplay_qa","capacity");
            modifier=construct(ClassFor(modifierClass),id,(double)count-8,enumValue(modifierClass+"$Operation","ADD_VALUE"));
        }
        call(group,"removeModifier",id);
        if(count!=8)call(group,"addPermanentModifier",modifier);
        call(group,"update");
    }
    private static Object curiosContext() {
        return call(screen(),"apiContext",windowSession(currentSession().sessionId()),null,Integer.MIN_VALUE,Integer.MIN_VALUE);
    }
    private static Object curiosRightmostCell() {
        Object w=windowSession(currentSession().sessionId()),definition=field(w,"apiDefinition"),context=curiosContext();
        return ((List<?>)call(definition,"cells",context)).stream()
            .filter(cell -> (Boolean)call(call(cell,"slot"),"canToggleRender"))
            .max(Comparator.comparingInt(cell -> ((Number)call(cell,"x")).intValue())).orElseThrow();
    }
    private static void curiosToggleRightmost() {
        Object cell=curiosRightmostCell();
        Object nativeSlot=call(cell,"slot");
        double x=((Number)call(cell,"x")).intValue()+19,y=((Number)call(cell,"y")).intValue()+2;
        Object w=windowSession(currentSession().sessionId());
        record("curios-toggle-dispatch","INFO","group="+field(nativeSlot,"group")+", index="+field(nativeSlot,"accessoryIndex")
            +", visibleBefore="+call(nativeSlot,"getRenderStatus")+", transportReady="+DesktopClientApi.canDispatchOptimisticMutationNow()
            +", windowHit="+(call(screen(),"windowAt",x,y)==w)+", hostScrollbar="+call(screen(),"storageScrollbarContains",w,x,y)
            +", titleControl="+call(screen(),"titleBarControlAt",w,x,y));
        clickAt(x,y);
    }
    private static boolean curiosRightmostVisible() {
        return (Boolean)call(call(curiosRightmostCell(),"slot"),"getRenderStatus");
    }
    private static void curiosVisibilityChecks(String label) {
        add(label+"-geometry",()->{},()->true,()->{
            Object w=windowSession(currentSession().sessionId()),cell=curiosRightmostCell();
            int right=((Number)call(cell,"x")).intValue()+20;
            Object layout=call(field(w,"apiDefinition"),"layout",curiosContext());
            int columns=((Number)call(layout,"columns")).intValue();
            check(label+"-native-toggle-present",(Boolean)call(call(cell,"slot"),"canToggleRender"));
            check(label+"-rightmost-column",((Number)call(cell,"x")).intValue()==number(w,"x")+8+(columns-1)*18);
            int clip=number(w,"x")+6+((Number)call(layout,"viewportWidth",number(w,"width"))).intValue();
            check(label+"-all-eight-pixels-inside-clip",right<=clip);
            capture(label+"-before");
        },4);
        add(label+"-last-pixel-click",InWorldGameplayHarness::curiosToggleRightmost,
            ()->!curiosRightmostVisible(),()->{check(label+"-no-item-pickup",carried()==0);capture(label+"-off");},8);
        add(label+"-restore",InWorldGameplayHarness::curiosToggleRightmost,
            InWorldGameplayHarness::curiosRightmostVisible,()->{},8);
    }
    private static void addCuriosRegression() {
        add("curios-native-fixture",()->{
            closeWindows();SaltsInventoryConfig.update(c -> {c.allowResizing=true;c.returnHotbarToInventory=false;c.persistentWindows=false;});
            server(()->{accessorySeed();check("curios-native-types",((Map<?,?>)call(curiosInventory(player()),"getCurios")).size()==11);return null;});
        },InWorldGameplayHarness::taskDone,()->{},8);
        add("curios-open-character",InWorldGameplayHarness::accessoryOpen,()->windowKind("CHARACTER")!=null,()->{},8);
        add("curios-open-native-panel",()->dispatchMovementKey("KEY_G","PRESS"),
            ()->curiosSession()!=null,()->{curiosActivate();capture("curios-equipment-all-types");},12);
        add("curios-shortcut-repeat",()->dispatchMovementKey("KEY_G","REPEAT"),()->true,()->{
            check("curios-shortcut-repeat-does-not-close",curiosSession()!=null);dispatchMovementKey("KEY_G","RELEASE");
        },8);
        add("curios-joined-layout",()->{},()->true,()->{
            var owner=accessoryCharacter().state();Object panel=windowSession(currentSession().sessionId());
            check("curios-equal-width",number(panel,"width")==owner.width());
            check("curios-four-pixel-gap",number(panel,"y")+number(panel,"height")+4==owner.y());
            check("curios-cosmetics-present",(Boolean)call(currentSession().menu(),"hasCosmetics"));
            check("curios-all-native-types-displayed",((List<?>)call(currentSession().menu(),"displayedSlots")).size()==35);
        },4);
        curiosVisibilityChecks("curios-rightmost-toggle");
        add("curios-player-pickup",()->accessoryPlayerClick(9,left()),()->carried()==4,()->{},5);
        add("curios-native-equipment-place",()->clickSlot(curiosFirstSlot(),left()),()->carried()==0,()->{
            check("curios-native-slot-received",currentSession().menu().getSlot(curiosFirstSlot()).getItem().is(Items.DIAMOND));capture("curios-equipment-item");
        },6);
        add("curios-native-equipment-pickup",()->clickSlot(curiosFirstSlot(),left()),()->carried()==4,()->{},5);
        add("curios-player-return",()->accessoryPlayerClick(9,left()),()->carried()==0,()->{},5);
        add("curios-cosmetic-tab",()->{
            Object w=windowSession(currentSession().sessionId());clickAt(number(w,"x")+number(w,"width")-22,number(w,"y")-12);
        },()->(Boolean)field(currentSession().menu(),"cosmetics"),()->capture("curios-cosmetics"),8);
        add("curios-cosmetic-pickup",()->accessoryPlayerClick(9,left()),()->carried()==4,()->{},4);
        add("curios-cosmetic-place",()->clickSlot(curiosFirstSlot(),left()),()->carried()==0,()->capture("curios-cosmetic-item"),6);
        add("curios-cosmetic-take",()->clickSlot(curiosFirstSlot(),left()),()->carried()==4,()->{},5);
        add("curios-cosmetic-return",()->accessoryPlayerClick(9,left()),()->carried()==0,()->{},5);
        add("curios-return-equipment-tab",()->{
            Object w=windowSession(currentSession().sessionId());clickAt(number(w,"x")+number(w,"width")-22,number(w,"y")-12);
        },()->!(Boolean)field(currentSession().menu(),"cosmetics"),()->{},8);
        add("curios-native-grow",()->server(()->{curiosCapacity(100);return null;}),
            ()->taskDone() && curiosSession()!=null && curiosSession().menu().slots.size()==254,()->{curiosActivate();capture("curios-overflow");},12);
        add("curios-small-viewport",()->{
            mc.options.guiScale().set(3);resizeHudGui();
        },()->true,()->capture("curios-overflow-small-viewport"),12);
        curiosVisibilityChecks("curios-scrolling-rightmost-toggle");
        add("curios-scroll-bottom",()->{
            Object w=windowSession(currentSession().sessionId());
            scroll((double)number(w,"x")+20,(double)number(w,"y")+20,-200D);
        },()->true,()->{
            Object state=call(curiosContext(),"state");
            check("curios-scroll-offset-positive",number(state,"scrollRow")>0);capture("curios-overflow-scrolled");
        },6);
        add("curios-joined-four-walls",()->{
            if(accessoryCharacter().state().locked()) accessoryCharacter().request(DesktopWindowAction.UNLOCK);
            check("curios-left-move-accepted",accessoryCharacter().moveTo(-999,-999));
        },()->true,()->{
            Object w=windowSession(currentSession().sessionId());
            check("curios-top-tab-on-screen",number(w,"y")>=23);
            check("curios-left-on-screen",number(w,"x")>=0);
        },6);
        add("curios-joined-bottom-right",()->{
            check("curios-right-move-accepted",accessoryCharacter().moveTo(99999,99999));
        },()->{
            var owner=accessoryCharacter().state();
            return owner.x()+owner.width()==screen().width&&owner.y()+owner.height()==screen().height;
        },()->{
            var owner=accessoryCharacter().state();
            check("curios-reaches-right-wall",owner.x()+owner.width()==screen().width);
            check("curios-reaches-bottom-wall",owner.y()+owner.height()==screen().height);
            check("curios-right-on-screen",owner.x()+owner.width()<=screen().width);
            check("curios-bottom-on-screen",owner.y()+owner.height()<=screen().height);capture("curios-joined-bottom-right");
        },6);
        add("curios-native-shrink",()->server(()->{
            var obsolete=serverMenu();
            Object removed=((List<?>)call(obsolete,"displayedSlots")).stream()
                .max(Comparator.comparingInt(slot->number(slot,"accessoryIndex"))).orElseThrow();
            Slot removedSlot=removed instanceof Slot nativeSlot?nativeSlot:(Slot)field(removed,"nativeSlot");
            check("curios-stale-click-fixture-index",number(removed,"accessoryIndex")==99);
            var reserved=player().getInventory().removeItemNoUpdate(9);
            check("curios-stale-click-fixture-stack",reserved.is(Items.DIAMOND)&&reserved.getCount()==4);
            player().inventoryMenu.setCarried(reserved.copy());obsolete.setCarried(reserved.copy());
            curiosCapacity(8);
            try {
                // The action was queued before capacity changed; process it before
                // the public lifecycle tick replaces this obsolete menu instance.
                Class<?> packet=ClassFor("com.salts_inventory_update.network.DesktopPackets$DesktopClickPayload");
                Class<?> dispatcher=ClassFor("com.salts_inventory_update.server.DesktopContainerSessions");
                Object payload;
                if(packet.getRecordComponents().length==6) {
                    payload=construct(packet,0,activeSession.sessionId(),removedSlot.index,0,"PICKUP",reserved.copy());
                } else {
                    Object sessions=call(dispatcher,"sessions",player());
                    Object session=((Map<?,?>)field(sessions,"sessions")).get(activeSession.sessionId());
                    long nonce=((Number)field(sessions,"connectionNonce")).longValue();
                    long playerToken=((Number)field(sessions,"playerSessionToken")).longValue();
                    long token=((Number)field(session,"sessionToken")).longValue();
                    long mutation=((Number)call(sessions,"nextMutationId")).longValue();
                    // A rejected authorization would never exercise the native menu guard.
                    if(call(dispatcher,"authorizeMenu",player(),sessions,nonce,activeSession.sessionId(),token,
                        obsolete.getStateId(),0L,"queued-click-qa")==null) {
                        throw new IllegalStateException("Queued-click fixture was not authorized");
                    }
                    payload=construct(packet,nonce,mutation,playerToken,activeSession.sessionId(),token,
                        obsolete.getStateId(),0,removedSlot.index,0,"PICKUP",reserved.copy());
                }
                call(dispatcher,"click",player(),payload);
                check("curios-late-click-rejected-safely",true);
            } catch(RuntimeException failure) {
                Throwable cause=failure;
                while(cause.getCause()!=null)cause=cause.getCause();
                record("curios-late-click-rejected-safely","FAIL",cause.toString());
                SaltsInventoryUpdate.LOGGER.error("Curios queued-click regression failed",failure);
            }
            check("curios-late-click-cursor-preserved",ItemStack.matches(reserved,player().inventoryMenu.getCarried()));
            // Restore only the disposable fixture after recording the actual outcome.
            player().getInventory().setItem(9,reserved);
            player().inventoryMenu.setCarried(ItemStack.EMPTY);obsolete.setCarried(ItemStack.EMPTY);
            return null;
        }),
            ()->taskDone() && curiosSession()!=null && curiosSession().menu().slots.size()==70,()->{curiosActivate();capture("curios-shrunk");},10);
        add("curios-close-owner",()->accessoryCharacter().close(),()->curiosSession()==null,()->check("curios-owner-closes-panel",curiosSession()==null),8);
        add("curios-final-conservation",()->server(()->{
            check("curios-diamond-conservation",player().getInventory().getItem(9).getCount()==4);
            check("curios-other-stack-preserved",player().getInventory().getItem(10).getCount()==3);
            check("curios-cursor-empty",player().inventoryMenu.getCarried().isEmpty());return null;
        }),InWorldGameplayHarness::taskDone,InWorldGameplayHarness::closeWindows,5);
    }
    private static void addTrinketsRegression() {
        add("trinkets-native-seed",()->{
            closeWindows();SaltsInventoryConfig.update(c->{c.returnHotbarToInventory=false;c.persistentWindows=false;});
            server(()->{call(ClassFor("com.salts_inventory_update.qa.trinkets.TrinketsQa"),"seed",player());mtSync();return null;});
        },InWorldGameplayHarness::taskDone,()->{},10);
        add("trinkets-open-character",InWorldGameplayHarness::accessoryOpen,()->windowKind("CHARACTER")!=null,()->capture("trinkets-character"),12);
        add("trinkets-offhand-always-present",()->{},()->true,()->{
            var p=slotPoint(0,45,windowKind("CHARACTER"));check("trinkets-real-offhand-hit",p.length==2);
            check("trinkets-native-slots-appended",mc.player.inventoryMenu.slots.size()>48);
        },4);
        add("trinkets-offhand-item-seed",()->server(()->{player().inventoryMenu.getSlot(45).set(new ItemStack(Items.IRON_INGOT,4));player().inventoryMenu.broadcastFullState();mtSync();return null;}),
            ()->taskDone() && mc.player.inventoryMenu.getSlot(45).getItem().getCount()==4,()->{},6);
        add("trinkets-offhand-pickup",()->{var p=slotPoint(0,45,windowKind("CHARACTER"));clickAt(p[0],p[1]);},()->carried()==4,()->{},5);
        add("trinkets-offhand-return",()->{var p=slotPoint(0,45,windowKind("CHARACTER"));clickAt(p[0],p[1]);},()->carried()==0,()->capture("trinkets-real-offhand"),5);
        add("trinkets-open-offhand-group",InWorldGameplayHarness::trinketsHoverOffhand,
            ()->field(ClassFor("dev.emi.trinkets.TrinketsClient"),"activeGroup")!=null,()->capture("trinkets-offhand-group"),12);
        add("trinkets-equip-pickup",()->accessoryPlayerClick(9,left()),()->carried()==2,()->{},4);
        add("trinkets-hover-with-cursor",InWorldGameplayHarness::trinketsHoverOffhand,()->true,()->{},10);
        add("trinkets-native-equip",()->trinketsClickRing(0,right()),()->carried()==1,
            ()->capture("trinkets-native-ring-equipped"),8);
        add("trinkets-return-remainder",()->accessoryPlayerClick(9,left()),()->carried()==0,()->{},5);
        add("trinkets-invalid-pickup",()->accessoryPlayerClick(11,left()),()->carried()==4,()->{},4);
        add("trinkets-invalid-hover",InWorldGameplayHarness::trinketsHoverOffhand,()->true,()->{},8);
        add("trinkets-invalid-expand-ring",()->{
            var p=slotPoint(0,trinketsRing(0),windowKind("CHARACTER"));
            spatialMove(p[0]*mc.getWindow().getGuiScale(),p[1]*mc.getWindow().getGuiScale(),0,0);
        },()->field(ClassFor("dev.emi.trinkets.TrinketsClient"),"activeType")!=null,()->capture("trinkets-ring-popup"),10);
        add("trinkets-invalid-rejected",()->trinketsClickRing(1,left()),()->true,()->{
            check("trinkets-native-validation",carried()==4 && mc.player.inventoryMenu.getSlot(trinketsRing(1)).getItem().isEmpty());
        },10);
        add("trinkets-invalid-return",()->accessoryPlayerClick(11,left()),()->carried()==0,()->{},5);
        add("trinkets-unequip-hover",InWorldGameplayHarness::trinketsHoverOffhand,()->true,()->{},8);
        add("trinkets-native-unequip",()->trinketsClickRing(0,left()),()->carried()==1,()->{},6);
        add("trinkets-unequip-return",()->accessoryPlayerClick(9,left()),()->carried()==0,()->{},5);
        add("trinkets-lock-pickup",()->accessoryPlayerClick(12,left()),()->carried()==1,()->{},4);
        add("trinkets-lock-hover",InWorldGameplayHarness::trinketsHoverOffhand,()->true,()->{},8);
        add("trinkets-lock-equip",()->trinketsClickRing(0,left()),()->carried()==0,()->{},105);
        add("trinkets-lock-try-pickup",()->trinketsClickRing(0,left()),()->true,()->{
            check("trinkets-unequip-restriction",carried()==0 && mc.player.inventoryMenu.getSlot(trinketsRing(0)).getItem().is(Items.GOLD_INGOT));
            capture("trinkets-native-locked-ring");
        },10);
        add("trinkets-native-rebuild",()->server(()->{
            for(int i=0;i<3;i++)call(player().inventoryMenu,"trinkets$updateTrinketSlots",true);
            com.salts_inventory_update.inventory.InventoryExpansion.syncToClient(player());player().inventoryMenu.broadcastFullState();mtSync();
            call(ClassFor("com.salts_inventory_update.qa.trinkets.TrinketsQa"),"report",player());return null;
        }),InWorldGameplayHarness::taskDone,()->check("trinkets-client-expanded-tail-retained",mc.player.inventoryMenu.slots.size()>=50),10);
        add("trinkets-native-grow",()->server(()->{
            mc.getSingleplayerServer().getCommands().performPrefixedCommand(player().createCommandSourceStack(),"trinketsqa grow");
            return null;
        }),()->taskDone() && mc.player.inventoryMenu.slots.stream().anyMatch(s ->
            trinketsSlotKey(s).equals("trinkets:equipment/hand/ring/3")),
            ()->capture("trinkets-native-capacity-grown"),12);
        add("trinkets-native-final-state",()->server(()->{
            call(ClassFor("com.salts_inventory_update.qa.trinkets.TrinketsQa"),"report",player());
            check("trinkets-diamond-conservation",player().getInventory().getItem(9).getCount()==2);
            check("trinkets-invalid-stack-preserved",player().getInventory().getItem(11).getCount()==4);
            check("trinkets-real-offhand-preserved",player().inventoryMenu.getSlot(45).getItem().getCount()==4);
            check("trinkets-cursor-empty",player().inventoryMenu.getCarried().isEmpty());
            try{
                String events=Files.readString(Path.of(System.getProperty("salts.trinkets.qa"),"events.log"));
                check("trinkets-native-equip-callback",events.contains("EQUIP "));
                check("trinkets-native-unequip-callback",events.contains("UNEQUIP "));
                check("trinkets-native-tick-callback",events.contains("NATIVE_TICK "));
            }catch(java.io.IOException failure){throw new IllegalStateException(failure);}
            return null;
        }),InWorldGameplayHarness::taskDone,()->{},8);
        add("trinkets-close-reopen",InWorldGameplayHarness::closeWindows,
            ()->nativeScreen()==null && (screen()==null || !screen().hasWindows()),()->{},5);
        add("trinkets-reopen",InWorldGameplayHarness::accessoryOpen,()->windowKind("CHARACTER")!=null,()->capture("trinkets-reopened"),10);
        add("trinkets-cleanup",InWorldGameplayHarness::closeWindows,()->sessions().isEmpty(),()->{},5);
    }
    private static int trinketsRing(int index) {
        String key="trinkets:equipment/offhand/ring/"+index;
        for(Slot slot:mc.player.inventoryMenu.slots)
            if(key.equals(trinketsSlotKey(slot)))return slot.index;
        throw new IllegalStateException("Missing native slot "+key);
    }
    private static String trinketsSlotKey(Slot slot) {
        return call(ClassFor("com.salts_inventory_update.api.desktop.DesktopPlayerMenuApi"),"slotKey",slot).toString();
    }
    private static void trinketsHoverOffhand() {
        var p=slotPoint(0,45,windowKind("CHARACTER"));
        spatialMove(p[0]*mc.getWindow().getGuiScale(),p[1]*mc.getWindow().getGuiScale(),0,0);
        call(screen(),"mouseMoved",p[0],p[1]);
    }
    private static void trinketsClickRing(int index,int button) {
        var p=slotPoint(0,trinketsRing(index),windowKind("CHARACTER"));
        spatialMove(p[0]*mc.getWindow().getGuiScale(),p[1]*mc.getWindow().getGuiScale(),0,0);
        mouse("mouseClicked",p[0],p[1],button,0,0);mouse("mouseReleased",p[0],p[1],button,0,0);
    }

    private static void addInventorySortRegression() {
        addContainer("chest", "minecraft:chest", 0);
        add("inner-sort-seed-chest", () -> server(() -> {
            var menu = serverMenu();
            for (int i = 0; i < 27; i++) menu.getSlot(i).set(ItemStack.EMPTY);
            var items = List.of(Items.DIRT, Items.IRON_INGOT, Items.DIAMOND, Items.IRON_INGOT, Items.GRAVEL,
                Items.OAK_PLANKS, Items.BONE, Items.APPLE, Items.IRON_INGOT, Items.DIAMOND_SWORD);
            int[] counts = {32,40,6,40,16,12,4,3,5,1};
            for (int i = 0; i < items.size(); i++) menu.getSlot(i).set(new ItemStack(items.get(i), counts[i]));
            var name = net.minecraft.network.chat.Component.literal("Keepsake");
            if (VersionInfo.MINECRAFT_VERSION.equals("1.20.1")) call(menu.getSlot(8).getItem(), "setHoverName", name);
            else call(menu.getSlot(8).getItem(),"set",field(ClassFor("net.minecraft.core.component.DataComponents"),"CUSTOM_NAME"), name);
            menu.getSlot(9).getItem().setDamageValue(31);
            Object sharpness=field(ClassFor("net.minecraft.world.item.enchantment.Enchantments"),"SHARPNESS");
            if (!VersionInfo.MINECRAFT_VERSION.equals("1.20.1")) {
                boolean legacyRegistry=VersionInfo.MINECRAFT_VERSION.equals("1.21.1");
                Object enchantments=call(call(player(),"registryAccess"),legacyRegistry?"registryOrThrow":"lookupOrThrow",
                    field(ClassFor("net.minecraft.core.registries.Registries"),"ENCHANTMENT"));
                sharpness=call(enchantments,legacyRegistry?"getHolderOrThrow":"getOrThrow",sharpness);
            }
            call(menu.getSlot(9).getItem(),"enchant",sharpness,3);
            menu.broadcastChanges(); return null;
        }), () -> taskDone() && activeSession.menu().getSlot(0).getItem().is(Items.DIRT),
            () -> capture("inner-sort-chest-before"), 8);
        add("inner-sort-chest-click", () -> {
            Object w = windowSession(activeSession.sessionId());
            checkSortButtonNeighbours(w);
            clickControl("SORT_INVENTORY");
        }, () -> activeSession.menu().getSlot(0).getItem().is(Items.DIAMOND), () -> {
            capture("inner-sort-chest-after");
            var actual = server(() -> {
                var m = serverMenu();
                check("inner-sort-chest-total", m.slots.subList(0,27).stream().map(Slot::getItem).mapToInt(ItemStack::getCount).sum() == 159);
                check("inner-sort-gems-first", m.getSlot(0).getItem().is(Items.DIAMOND));
                check("inner-sort-merged-ingots", m.getSlot(1).getItem().getCount() == 64 && m.getSlot(2).getItem().getCount() == 16);
                check("inner-sort-name-preserved", "Keepsake".equals(m.getSlot(3).getItem().getHoverName().getString()));
                check("inner-sort-damage-preserved", m.getSlot(4).getItem().is(Items.DIAMOND_SWORD) && m.getSlot(4).getItem().getDamageValue() == 31);
                if (VersionInfo.MINECRAFT_VERSION.equals("1.20.1")) {
                    Map<?,?> enchantments=(Map<?,?>)call(ClassFor("net.minecraft.world.item.enchantment.EnchantmentHelper"),"getEnchantments",m.getSlot(4).getItem());
                    check("inner-sort-enchantment-preserved",enchantments.values().stream().anyMatch(level -> ((Number)level).intValue()==3));
                } else {
                    Object enchantmentData=call(m.getSlot(4).getItem(),"getEnchantments");
                    check("inner-sort-enchantment-preserved", ((Collection<?>)call(enchantmentData,"keySet")).stream()
                        .anyMatch(e -> ((Number)call(enchantmentData,"getLevel",e)).intValue()==3));
                }
                check("inner-sort-supplies-before-building", m.getSlot(5).getItem().is(Items.APPLE) && m.getSlot(7).getItem().is(Items.OAK_PLANKS));
                check("inner-sort-terrain-last", m.getSlot(8).getItem().is(Items.DIRT) && m.getSlot(9).getItem().is(Items.GRAVEL));
                return m.slots.subList(0,27).stream().map(s -> s.getItem().copy()).toList();
            });
            addFirst("inner-sort-server-confirm", () -> {}, actual::isDone, () -> {
                List<ItemStack> before = actual.join();
                clickControl("SORT_INVENTORY");
                addFirst("inner-sort-idempotent", () -> server(() -> {
                    for (int i = 0; i < before.size(); i++) check("inner-sort-repeat-slot-"+i, ItemStack.matches(before.get(i), serverMenu().getSlot(i).getItem()));
                    return null;
                }), InWorldGameplayHarness::taskDone, () -> {}, 6);
            }, 8);
        }, 10);
        add("inner-sort-compact-controls", () -> {
            Object w = windowSession(activeSession.sessionId()); beforeWidth=number(w,"width");
            double x=number(w,"x")+beforeWidth-2,y=number(w,"y")+number(w,"height")-2;
            mouse("mouseClicked",x,y,left(),0,0);mouse("mouseDragged",x-90,y,left(),-90,0);mouse("mouseReleased",x-90,y,left(),0,0);
        }, () -> true, () -> {
            Object w = windowSession(activeSession.sessionId());
            var rects=(List<?>)call(call(screen(),"titleBarLayout",w),"controls");
            check("inner-sort-compact-more",rects.stream().anyMatch(r -> call(r,"control").toString().equals("ELLIPSIS")));
            checkSortButtonNeighbours(w); capture("inner-sort-compact-popup");
        }, 5);
        add("inner-sort-compact-seed", () -> server(() -> {
            var menu=serverMenu();var first=menu.getSlot(0).getItem().copy();
            menu.getSlot(0).set(menu.getSlot(9).getItem().copy());menu.getSlot(9).set(first);
            menu.broadcastFullState();mtSync();return null;
        }), () -> taskDone() && activeSession.menu().getSlot(0).getItem().is(Items.GRAVEL), () -> {}, 5);
        add("inner-sort-compact-click", () -> clickControl("SORT_INVENTORY"),
            () -> activeSession.menu().getSlot(0).getItem().is(Items.DIAMOND), () -> {
                check("inner-sort-compact-reorders",activeSession.menu().getSlot(0).getItem().getCount()==6);
                // Overflow actions retain the management menu, just like its pin/focus controls.
                check("inner-sort-compact-menu-remains-usable",field(screen(),"popupWindow")==windowSession(activeSession.sessionId()));
                clickControl("ELLIPSIS");
                check("inner-sort-compact-menu-dismisses",field(screen(),"popupWindow")==null);
            }, 5);
        add("inner-sort-restore-width", () -> {
            Object w=windowSession(activeSession.sessionId()); int delta=beforeWidth-number(w,"width");
            double x=number(w,"x")+number(w,"width")-2,y=number(w,"y")+number(w,"height")-2;
            mouse("mouseClicked",x,y,left(),0,0);mouse("mouseDragged",x+delta,y,left(),delta,0);mouse("mouseReleased",x+delta,y,left(),0,0);
        }, () -> true, () -> {}, 5);
        add("inner-sort-player-seed", () -> server(() -> {
            var p = player(); p.getInventory().clearContent();
            p.getInventory().setItem(0,new ItemStack(Items.DIAMOND_SWORD)); p.getInventory().setItem(8,new ItemStack(Items.APPLE,3));
            p.getInventory().setItem(9,new ItemStack(Items.DIRT,13)); p.getInventory().setItem(10,new ItemStack(Items.IRON_INGOT,40));
            p.getInventory().setItem(11,new ItemStack(Items.DIAMOND,4)); p.getInventory().setItem(12,new ItemStack(Items.IRON_INGOT,40));
            p.inventoryMenu.getSlot(5).set(new ItemStack(Items.DIAMOND_HELMET));
            p.inventoryMenu.getSlot(45).set(new ItemStack(Items.TORCH,9));
            p.inventoryMenu.broadcastFullState(); mtSync(); return null;
        }), InWorldGameplayHarness::taskDone, () -> {}, 8);
        add("inner-sort-player-open", () -> {if(windowKind("INVENTORY")==null)InventoryDesktopScreen.openOrToggleInventory(mc);},
            () -> windowKind("INVENTORY") != null && mc.player.getInventory().getItem(9).is(Items.DIRT), () -> capture("inner-sort-player-before"), 8);
        add("inner-sort-player-click", () -> {
            Object w = windowKind("INVENTORY"); checkSortButtonNeighbours(w); clickWindowControl(w,"SORT_INVENTORY");
        }, () -> mc.player.getInventory().getItem(9).is(Items.DIAMOND), () -> {
            capture("inner-sort-player-after");
            server(() -> {
                var p = player(); check("inner-sort-player-gems-first",p.getInventory().getItem(9).is(Items.DIAMOND));
                check("inner-sort-player-merge",p.getInventory().getItem(10).getCount()==64 && p.getInventory().getItem(11).getCount()==16);
                check("inner-sort-hotbar-untouched",p.getInventory().getItem(0).is(Items.DIAMOND_SWORD) && p.getInventory().getItem(8).getCount()==3);
                check("inner-sort-equipment-untouched",p.inventoryMenu.getSlot(5).getItem().is(Items.DIAMOND_HELMET) && p.inventoryMenu.getSlot(45).getItem().getCount()==9);
                check("inner-sort-other-window-untouched",serverMenu().getSlot(0).getItem().getCount()==6);
                return null;
            });
        }, 10);
        add("inner-sort-player-server-confirm", () -> {}, InWorldGameplayHarness::taskDone, () -> {}, 3);
        add("outer-sort-still-transfers", () -> clickWindowControl(windowKind("INVENTORY"),"SORT_WINDOWS"),
            () -> mc.player.getInventory().getItem(9).isEmpty(), () -> server(() -> {
                check("outer-sort-still-works",serverMenu().slots.subList(0,27).stream().map(Slot::getItem)
                    .filter(s -> s.is(Items.DIAMOND)).mapToInt(ItemStack::getCount).sum()==10);
                check("outer-sort-hotbar-preserved",player().getInventory().getItem(0).is(Items.DIAMOND_SWORD)); return null;
            }), 10);
        add("outer-sort-server-confirm", () -> {}, InWorldGameplayHarness::taskDone, () -> {}, 3);
        addContainer("inner-sort-furnace","minecraft:furnace",0);
        STEPS.removeLast(); // Inspect this processor before the helper closes it.
        add("inner-sort-processors-excluded", () -> {}, () -> true, () -> {
            Object w = windowSession(activeSession.sessionId());
            check("inner-sort-no-furnace-button", !Boolean.TRUE.equals(call(screen(),"canSortInventoryWindow",w)));
            capture("inner-sort-furnace-outer-only");
        }, 5);
        add("inner-sort-close", InWorldGameplayHarness::closeWindows, () -> sessions().isEmpty(), () -> {}, 5);
    }

    private static void checkSortButtonNeighbours(Object window) {
        List<?> controls = (List<?>)call(call(screen(),"titleBarLayout",window),"controls");
        List<String> names = controls.stream().map(c -> call(c,"control").toString()).toList();
        if (!names.contains("SORT_INVENTORY")) {
            if (field(screen(),"popupWindow") != window) clickWindowControl(window,"ELLIPSIS");
            check("inner-sort-popup-is-open",field(screen(),"popupWindow") == window);
            controls = (List<?>)call(screen(),"popupControlRects",window,call(screen(),"popupRect",window));
            names = controls.stream().map(c -> call(c,"control").toString()).toList();
        }
        check("inner-sort-button-next-to-outer",names.indexOf("SORT_INVENTORY") == names.indexOf("SORT_WINDOWS")+1 && names.contains("SORT_WINDOWS"));
        Object kind = enumValue("com.salts_inventory_update.client.InventoryDesktopScreen$WindowControl","SORT_INVENTORY");
        check("inner-sort-old-minimize-texture",((Number)call(screen(),"controlTextureColumn",window,kind)).intValue()==1);
    }

    private static Object spatialConfig() { return field(ClassFor("org.tastytrash.spatialGUI.SpatialGUI"), "config"); }
    private record SpatialAction(long at, Runnable action) { }
    private static final List<SpatialAction> spatialActions=new ArrayList<>();
    private static void spatialLater(long delay,Runnable action) {spatialActions.add(new SpatialAction(System.currentTimeMillis()+delay,action));}
    private static void spatialAlt() {dispatchMovementKey("KEY_LALT","PRESS");dispatchMovementKey("KEY_LALT","RELEASE");}
    private static void spatialTransitionCase(String label,Runnable start) {
        add(label,()->{
            call(ClassFor("com.salts_inventory_update.functionaltest.SpatialTransitionProbe"),"begin");
            start.run();
            spatialLater(180,()->capture(label+"-during"));
        },()->true,()->{
            @SuppressWarnings("unchecked") var samples=(List<double[]>)call(ClassFor("com.salts_inventory_update.functionaltest.SpatialTransitionProbe"),"finish");
            double maxJump=0,maxRate=0;int discontinuities=0,closingFrames=0;
            for(int i=0;i<samples.size();i++){
                var s=samples.get(i);if(s[1]==2)closingFrames++;
                for(int k=4;k<s.length;k++)if(!Double.isFinite(s[k]))throw new IllegalStateException("Non-finite projected panel");
                if(i==0)continue;var previous=samples.get(i-1);double delta=0;
                for(int k=4;k<s.length;k++)delta=Math.max(delta,Math.abs(s[k]-previous[k]));
                double seconds=Math.max(.001,s[0]-previous[0]);maxJump=Math.max(maxJump,delta);maxRate=Math.max(maxRate,delta/seconds);
                if(delta>Math.max(.06,seconds*8))discontinuities++;
            }
            try{Files.writeString(output.resolve(label+"-frames.json"),new GsonBuilder().setPrettyPrinting().create().toJson(samples));}catch(Exception failure){throw new IllegalStateException(failure);}
            record(label+"-measurement","INFO","frames="+samples.size()+", closing="+closingFrames+", largest corner step="+maxJump+", max NDC/sec="+maxRate+", discontinuities="+discontinuities);
            check(label+"-rendered",samples.size()>5);
            check(label+"-continuous",discontinuities==0);
            if(label.endsWith("alt-enter")||label.equals("transitions-hold-down")){
                var point=spatialMapped(mc.getWindow().getScreenWidth()*.05,mc.getWindow().getScreenHeight()*.5);
                check(label+"-normal-screen-alignment",Math.abs(point[0]-mc.getWindow().getGuiScaledWidth()*.05)<2&&Math.abs(point[1]-mc.getWindow().getGuiScaledHeight()*.5)<2);
            }
            if(label.endsWith("close-held"))check(label+"-camera-stays-at-eye",samples.stream().filter(s->s[0]>samples.get(0)[0]+.6).allMatch(s->s[2]<.05));
            capture(label+"-settled");
        },24);
    }
    private static void addSpatialTransitions() {
        add("transitions-configure",()->{
            closeWindows();SaltsInventoryConfig.update(c->{c.hasSeenInstructionsWindow=true;c.persistentWindows=false;c.returnHotbarToInventory=true;c.toggleMovementControl=true;});
            setMovementBinding("KEY_LALT");spatialSet("enabled",true);spatialSet("transitionDurationMs",350);
            spatialSet("enableCloseAnimation",true);spatialSet("enableScaleAnimation",true);spatialSet("enableFadeAnimation",true);
            spatialSet("disableThirdPersonParallax",true);spatialSet("disableFirstPersonParallax",true);
            spatialSet("overrideFov",true);spatialSet("targetFov",85);spatialSet("useCrosshairForFirstPerson",false);
        },()->true,()->{},3);
        for(String presentation:List.of("third","first","curved")) {
            add("transitions-"+presentation+"-configure",()->{
                spatialSet("firstPersonModeInventory",!presentation.equals("third"));spatialSet("firstPersonModeContainers",!presentation.equals("third"));
                spatialSet("curvedScreenEnabled",presentation.equals("curved"));
            },()->true,()->{},3);
            spatialTransitionCase("transitions-"+presentation+"-open",()->InventoryDesktopScreen.openOrToggleInventory(mc));
            spatialTransitionCase("transitions-"+presentation+"-alt-enter",InWorldGameplayHarness::spatialAlt);
            spatialTransitionCase("transitions-"+presentation+"-alt-release",()->{
                mc.player.setYRot(mc.player.getYRot()+38);spatialAlt();
            });
            spatialTransitionCase("transitions-"+presentation+"-reverse-alt",()->{
                spatialAlt();spatialLater(100,()->{spatialAlt();spatialLater(100,()->{spatialAlt();spatialLater(100,InWorldGameplayHarness::spatialAlt);});});
            });
            spatialTransitionCase("transitions-"+presentation+"-close",InWorldGameplayHarness::closeWindows);
            spatialTransitionCase("transitions-"+presentation+"-interrupt-open",()->{
                InventoryDesktopScreen.openOrToggleInventory(mc);spatialLater(120,InWorldGameplayHarness::closeWindows);
            });
            spatialTransitionCase("transitions-"+presentation+"-reopen-during-close",()->{
                InventoryDesktopScreen.openOrToggleInventory(mc);spatialLater(180,()->{
                    closeWindows();spatialLater(100,()->InventoryDesktopScreen.openOrToggleInventory(mc));
                });
            });
            spatialTransitionCase("transitions-"+presentation+"-close-held",()->{spatialAlt();spatialLater(500,InWorldGameplayHarness::closeWindows);});
        }
        add("transitions-hold-configure",()->{
            SaltsInventoryConfig.update(c->{c.toggleMovementControl=false;c.returnHotbarToInventory=false;});
            spatialSet("firstPersonModeInventory",false);spatialSet("curvedScreenEnabled",false);
        },()->true,()->{},3);
        spatialTransitionCase("transitions-detached-open",()->InventoryDesktopScreen.openOrToggleInventory(mc));
        spatialTransitionCase("transitions-hold-down",()->spatialHeldInputKey=nativeInputAction("KEY_LALT"));
        spatialTransitionCase("transitions-hold-release",()->spatialHeldInputKey=-1);
        spatialTransitionCase("transitions-close-last-window",()->screen().workspaceAccess().windows().get(0).handle().close());
        add("transitions-persistent-configure",()->SaltsInventoryConfig.update(c->{c.persistentWindows=true;c.returnHotbarToInventory=true;}),()->true,()->{},3);
        spatialTransitionCase("transitions-persistent-open",()->InventoryDesktopScreen.openOrToggleInventory(mc));
        spatialTransitionCase("transitions-persistent-hide",()->InventoryDesktopScreen.openOrToggleInventory(mc));
        spatialTransitionCase("transitions-persistent-restore",()->InventoryDesktopScreen.openOrToggleInventory(mc));
        spatialTransitionCase("transitions-persistent-cleanup",InWorldGameplayHarness::closeWindows);
        add("transitions-container-configure",()->{spatialSet("firstPersonModeContainers",false);spatialSet("curvedScreenEnabled",false);},()->true,()->{},3);
        addContainer("transitions-chest","minecraft:chest",0);STEPS.removeLast();
        spatialTransitionCase("transitions-container-add-inventory",()->InventoryDesktopScreen.openOrToggleInventory(mc));
        add("transitions-render-hit-agreement",()->{var p=slotPoint(0);spatialClick(p[0],p[1]);},()->carried()==32,()->assertContainer("transitions-server-pickup",0,0,32),3);
        add("transitions-render-hit-place",()->{var p=slotPoint(1);spatialClick(p[0],p[1]);},()->carried()==0&&currentSession().menu().getSlot(1).getItem().getCount()==32,()->assertContainer("transitions-server-place",1,32,0),3);
        spatialTransitionCase("transitions-container-close-all",InWorldGameplayHarness::closeWindows);
    }
    private static Object spatialRenderer() { return call(ClassFor("org.tastytrash.spatialGUI.client.SpatialGUIClient"), "renderer"); }
    private static boolean spatialActive() { return (Boolean)call(ClassFor("com.salts_inventory_update.spatial.SpatialGuiIntegration"), "active"); }
    private static void spatialSet(String name, Object value) {
        try { spatialConfig().getClass().getField(name).set(spatialConfig(),value); }
        catch(ReflectiveOperationException e){throw new IllegalStateException(e);}
    }
    private static Object spatialCamera() {
        try{return call(mc.gameRenderer,"mainCamera");}catch(IllegalArgumentException legacy){return call(mc.gameRenderer,"getMainCamera");}
    }
    private static net.minecraft.world.phys.Vec3 spatialCameraPosition() {
        try{return (net.minecraft.world.phys.Vec3)call(spatialCamera(),"position");}
        catch(IllegalArgumentException legacy){return (net.minecraft.world.phys.Vec3)call(spatialCamera(),"getPosition");}
    }
    private static void spatialMove(double x,double y,double dx,double dy) {
        try{call(mc.mouseHandler,"onMove",nativeWindowHandle(),x,y,dx,dy);}
        catch(IllegalArgumentException legacy){
            boolean relative=screen()!=null&&screen().isCameraControlActive();
            double previousX=((Number)field(mc.mouseHandler,"xpos")).doubleValue();
            call(mc.mouseHandler,"onMove",nativeWindowHandle(),relative?((Number)field(mc.mouseHandler,"xpos")).doubleValue()+dx:x,
                relative?((Number)field(mc.mouseHandler,"ypos")).doubleValue()+dy:y);
            if(relative&&dx!=0)record("spatial-legacy-relative-input","INFO","xBefore="+previousX+", xAfter="+field(mc.mouseHandler,"xpos")+", requestedDx="+dx+", yaw="+mc.player.getYRot()+", handoff="+field(mc.mouseHandler,"salts_inventory_update$desktopHandoffFrames"));
        }
    }
    private static double[] spatialMapped(double x,double y) {
        Object r=spatialRenderer(), inventory=call(r,"getInventoryRenderer"), quad=call(inventory,"getQuadBasis"), curve=call(inventory,"getCylinderBasis");
        Object point=curve==null
            ? call(ClassFor("org.tastytrash.spatialGUI.util.RenderUtil"),"getInventoryMousePositionRay",x,y,quad,call(call(r,"getTargetManager"),"getInventoryTarget"))
            : call(ClassFor("org.tastytrash.spatialGUI.util.RenderUtil"),"getInventoryMousePositionRayCurved",x,y,curve);
        if(point==null)return new double[]{-10000,-10000};
        double scale=((Number)call(spatialConfig(),"getEffectiveGuiScale")).doubleValue();
        double canvas=1;
        try {canvas=((Number)call(call(ClassFor("com.salts_inventory_update.spatial.SpatialDesktopCanvas"),"get"),"scale")).doubleValue();}catch(RuntimeException oldArtifact){}
        return new double[]{((Number)field(point,"x")).doubleValue()/scale/canvas,((Number)field(point,"y")).doubleValue()/scale/canvas};
    }
    private static double spatialProjectionError(double[] point,double x,double y) {
        // Published upstream mapping clamps misses to a texture edge. Those plateaus are not
        // inverse-projection seeds: a solver can otherwise optimize y while stuck at x=0.
        double width=mc.getWindow().getGuiScaledWidth(),height=mc.getWindow().getGuiScaledHeight();
        if(point[0]<=0||point[1]<=0||point[0]>=width||point[1]>=height)return Double.POSITIVE_INFINITY;
        return Math.hypot(point[0]-x,point[1]-y);
    }
    /** Invert the published plane/curve projection, then dispatch ordinary native mouse callbacks. */
    private static void spatialPoint(double targetX,double targetY) {
        double width=mc.getWindow().getScreenWidth(),height=mc.getWindow().getScreenHeight();
        double bx=width/2,by=height/2,best=Double.MAX_VALUE;
        for(int iy=0;iy<=20;iy++)for(int ix=0;ix<=24;ix++) {
            double x=width*ix/24,y=height*iy/20;var p=spatialMapped(x,y);double error=spatialProjectionError(p,targetX,targetY);
            if(error<best){best=error;bx=x;by=y;}
        }
        for(double delta=width/24;delta>.05;delta/=2) {
            boolean improved;
            do {improved=false;
                for(int dy=-1;dy<=1;dy++)for(int dx=-1;dx<=1;dx++) {
                    double x=bx+dx*delta,y=by+dy*delta;if(x<0||y<0||x>width||y>height)continue;var p=spatialMapped(x,y);double error=spatialProjectionError(p,targetX,targetY);
                    if(error<best-.0001){best=error;bx=x;by=y;improved=true;}
                }
            }while(improved);
        }
        if(best>2)throw new IllegalStateException("Projected target outside panel: "+targetX+","+targetY+" error="+best);
        spatialMove(bx,by,0,0);
    }
    private static void spatialClick(double x,double y) {
        spatialPoint(x,y);
        spatialMouseButton(left(),"PRESS");spatialMouseButton(left(),"RELEASE");
    }
    private static void spatialMouseButton(int button,String action) {
        try{call(mc.mouseHandler,"onPress",nativeWindowHandle(),button,nativeInputAction(action),0);}
        catch(IllegalArgumentException modern){call(mc.mouseHandler,"onButton",nativeWindowHandle(),construct(ClassFor("net.minecraft.client.input.MouseButtonInfo"),button,0),nativeInputAction(action));}
    }
    private static net.minecraft.world.phys.Vec3 spatialWorldTarget;
    private static BlockPos spatialWorldBlock, spatialWall;
    private static double spatialYawBefore;
    private static boolean spatialNativeLookApplied;
    public static int spatialHeldInputKey=-1;
    private static boolean spatialManualHoldConfigured,spatialManualHoldSeen,spatialManualHoldReleased;
    private static void spatialObserveManualHold() {
        if(!SPATIAL_ONLY||!Files.exists(output.resolve("hold-mode.request"))||screen()==null)return;
        if(!spatialManualHoldConfigured){
            setMovementBinding("KEY_LALT");SaltsInventoryConfig.update(c->c.toggleMovementControl=false);spatialManualHoldConfigured=true;
        }
        if(screen().isCameraControlActive()&&!spatialManualHoldSeen){
            spatialManualHoldSeen=true;check("spatial-physical-hold-activates",true);capture("spatial-physical-hold-active");
        }
        if(spatialManualHoldSeen&&!screen().isCameraControlActive()&&!spatialManualHoldReleased){
            spatialManualHoldReleased=true;check("spatial-physical-hold-release-restores-desktop",true);capture("spatial-physical-hold-released");
            try{Files.writeString(output.resolve("hold-mode-passed.json"),"{\"activated\":true,\"released\":true}");}catch(Exception e){throw new IllegalStateException(e);}
        }
    }
    private static void spatialAimAt(net.minecraft.world.phys.Vec3 target) {
        Object camera=spatialCamera();
        var direction=target.subtract((net.minecraft.world.phys.Vec3)spatialCameraPosition()).normalize();
        double width=mc.getWindow().getScreenWidth(),height=mc.getWindow().getScreenHeight();
        double bx=width/2,by=height/2,best=Double.MAX_VALUE;
        for(double delta=width/2;delta>.03;delta/=2) {
            boolean improved;
            do {improved=false;
                for(int dy=-1;dy<=1;dy++)for(int dx=-1;dx<=1;dx++) {
                    double x=bx+dx*delta,y=by+dy*delta;
                    if(x<0||x>width||y<0||y>height)continue;
                    float[] ray=(float[])call(ClassFor("org.tastytrash.spatialGUI.util.RenderUtil"),"buildCameraRayDirection",x,y);
                    if(ray==null)continue;
                    double error=Math.pow(ray[0]-direction.x,2)+Math.pow(ray[1]-direction.y,2)+Math.pow(ray[2]-direction.z,2);
                    if(error<best-1e-12){best=error;bx=x;by=y;improved=true;}
                }
            }while(improved);
        }
        check("spatial-world-ray-on-monitor",best<.00001);spatialMove(bx,by,0,0);
    }
    private static Object spatialPick() {return call(ClassFor("com.salts_inventory_update.spatial.SpatialGuiIntegration"),"pickWorld");}
    private static void spatialWorldClick() {dispatchMovementMouse("PRESS");dispatchMovementMouse("RELEASE");}
    private static void addSpatialWorldRegression() {
        add("spatial-world-fixture",()->{
            mc.player.setYRot(0);mc.player.setXRot(0);
            spatialSet("useCrosshairForFirstPerson",false);spatialSet("firstPersonModeInventory",false);spatialSet("firstPersonModeContainers",false);spatialSet("disableThirdPersonParallax",true);
            for(var w:screen().workspaceAccess().windows())w.handle().moveTo(8,8);
            server(()->{
                var p=player();spatialWorldBlock=p.blockPosition().offset(1,0,2);
                serverLevel().setBlock(spatialWorldBlock,(net.minecraft.world.level.block.state.BlockState)call(registryValue("BLOCK","minecraft:barrel"),"defaultBlockState"),3);
                spatialWorldTarget=new net.minecraft.world.phys.Vec3(spatialWorldBlock.getX()+.5,spatialWorldBlock.getY()+.5,spatialWorldBlock.getZ()+.5);return null;
            });
        },InWorldGameplayHarness::taskDone,()->{},20);
        add("spatial-world-physical-target",()->spatialAimAt(spatialWorldTarget),()->true,()->{
            spatialAimAt(spatialWorldTarget);
            Object hit=spatialPick();
            record("spatial-world-target-diagnostic","INFO","hit="+call(hit,"getType")+" at="+call(hit,"getLocation")+" target="+spatialWorldBlock);
            check("spatial-world-target-correct-block",hit instanceof net.minecraft.world.phys.BlockHitResult b&&b.getType()==net.minecraft.world.phys.HitResult.Type.BLOCK&&b.getBlockPos().equals(spatialWorldBlock));
            var camera=(net.minecraft.world.phys.Vec3)spatialCameraPosition();
            var eye=mc.player.getEyePosition();
            for(int x=mc.player.blockPosition().getX()-1;x<=spatialWorldBlock.getX()+1&&spatialWall==null;x++)
                for(int y=mc.player.blockPosition().getY();y<=mc.player.blockPosition().getY()+2&&spatialWall==null;y++)
                    for(int z=mc.player.blockPosition().getZ();z<=spatialWorldBlock.getZ()&&spatialWall==null;z++) {
                        BlockPos pos=new BlockPos(x,y,z);if(pos.equals(spatialWorldBlock)||pos.equals(mc.player.blockPosition())||pos.equals(mc.player.blockPosition().above()))continue;
                        var box=new net.minecraft.world.phys.AABB(x,y,z,x+1,y+1,z+1);
                        var location=((net.minecraft.world.phys.HitResult)hit).getLocation();
                        if(box.clip(eye,location).isPresent()&&box.clip(camera,location).isEmpty())spatialWall=pos;
                    }
            check("spatial-camera-can-see-around-player-obstruction",spatialWall!=null);
        },3);
        add("spatial-world-block-player-sight",()->server(()->{
            serverLevel().setBlock(spatialWall,(net.minecraft.world.level.block.state.BlockState)call(registryValue("BLOCK","minecraft:stone"),"defaultBlockState"),3);return null;
        }),InWorldGameplayHarness::taskDone,()->{
            spatialAimAt(spatialWorldTarget);Object hit=spatialPick();
            check("spatial-world-obstruction-respected",!(hit instanceof net.minecraft.world.phys.BlockHitResult b&&b.getType()==net.minecraft.world.phys.HitResult.Type.BLOCK&&b.getBlockPos().equals(spatialWorldBlock)));
            spatialWorldClick();
        },8);
        add("spatial-world-blocked-no-menu",()->{},()->true,()->check("spatial-world-obstructed-menu-stays-closed",sessions().size()==1),8);
        add("spatial-world-clear-sight",()->server(()->{
            serverLevel().setBlock(spatialWall,(net.minecraft.world.level.block.state.BlockState)call(registryValue("BLOCK","minecraft:air"),"defaultBlockState"),3);return null;
        }),InWorldGameplayHarness::taskDone,()->{spatialAimAt(spatialWorldTarget);spatialWorldClick();},8);
        add("spatial-world-native-open",()->{},()->sessions().size()==2,()->{
            check("spatial-world-container-opens-in-existing-panel",call(spatialRenderer(),"getHookedScreen")==screen());capture("spatial-world-opened-barrel");
            spatialAimAt(spatialWorldTarget.add(0,0,12));
            Object hit=spatialPick();
            check("spatial-world-distant-point-not-interactable",hit instanceof net.minecraft.world.phys.BlockHitResult b&&b.getType()==net.minecraft.world.phys.HitResult.Type.MISS);
        },8);
    }
    private static void addSpatialRegression() {
        boolean absent="true".equalsIgnoreCase(System.getenv("SIU_QA_SPATIAL_ABSENT"));
        add("spatial-configure",()->{
            closeWindows();SaltsInventoryConfig.update(c->{c.hasSeenInstructionsWindow=true;c.expandableInventory=false;c.persistentWindows=false;c.returnHotbarToInventory=true;c.toggleMovementControl=true;c.enableWindowSnapping=false;});
            setMovementBinding("KEY_LALT");
            if(!absent){spatialSet("enabled",true);spatialSet("firstPersonModeInventory",false);spatialSet("firstPersonModeContainers",false);spatialSet("useCrosshairForFirstPerson",false);}
        },()->true,()->check("spatial-mod-presence",(loadedMods().containsKey("spatial-gui")||loadedMods().containsKey("spatial_gui"))!=absent),3);
        addContainer("spatial-chest","minecraft:chest",0);STEPS.removeLast();
        add("spatial-inventory-window",()->InventoryDesktopScreen.openOrToggleInventory(mc),()->windowKind("INVENTORY")!=null,()->{
            for(var w:screen().workspaceAccess().windows())w.handle().moveTo(w.kind()==DesktopWorkspaceWindow.Kind.PLAYER_INVENTORY?240:8,30);
            check("spatial-two-windows-one-desktop",screen().workspaceAccess().windows().size()==2);
        },12);
        if(absent){
            add("spatial-absent-normal-input",()->clickSlot(0,left()),()->carried()==32,()->{assertContainer("absent-server-pickup",0,0,32);capture("spatial-absent-normal-desktop");},5);
            add("spatial-absent-return",InWorldGameplayHarness::storeInPlayer,()->carried()==0,()->{},4);
            add("spatial-absent-cleanup",InWorldGameplayHarness::closeWindows,()->sessions().isEmpty(),()->{},5);return;
        }
        add("spatial-third-person-panel",()->{},InWorldGameplayHarness::spatialActive,()->{
            check("spatial-captures-existing-desktop",call(spatialRenderer(),"getHookedScreen")==screen());
            check("spatial-third-person-mode",!(Boolean)call(ClassFor("org.tastytrash.spatialGUI.client.SpatialGUIClient"),"getEffectiveFirstPersonMode"));
            capture("spatial-third-person-desktop");
        },20);
        add("spatial-native-projected-pickup",()->{var p=slotPoint(0);spatialClick(p[0],p[1]);},()->carried()==32,()->{
            assertContainer("spatial-server-pickup",0,0,32);capture("spatial-carried-item");
        },5);
        add("spatial-native-projected-place",()->{var p=slotPoint(1);spatialClick(p[0],p[1]);},()->carried()==0&&currentSession().menu().getSlot(1).getItem().getCount()==32,()->{
            assertContainer("spatial-server-place",1,32,0);
        },5);
        add("spatial-hidden-selection",()->{
            spatialMove(0,0,0,0);beforeExtra=hudSelected(mc.player.getInventory());
            dispatchMovementKey("KEY_2","PRESS");dispatchMovementKey("KEY_2","RELEASE");
            call(mc.mouseHandler,"onScroll",nativeWindowHandle(),0D,-1D);
        },()->true,()->check("spatial-hidden-hotbar-selection-locked",hudSelected(mc.player.getInventory())==beforeExtra),4);
        add("spatial-movement-enter",()->{dispatchMovementKey("KEY_LALT","PRESS");dispatchMovementKey("KEY_LALT","RELEASE");},()->screen().isCameraControlActive(),()->{
            check("spatial-movement-restores-hud",!com.salts_inventory_update.client.WindowedInventoryClient.shouldHideHotbar());
            check("spatial-movement-normal-perspective",call(mc.options,"getCameraType")==call(mc.options,"salts$perspective"));
            check("spatial-movement-keeps-windows",screen().workspaceAccess().windows().size()==2);
            check("spatial-movement-cursor-grabbed",(Boolean)field(mc.mouseHandler,"mouseGrabbed"));
            capture("spatial-movement-gameplay-camera");
            check("spatial-gameplay-camera-at-player-eye",((net.minecraft.world.phys.Vec3)spatialCameraPosition()).distanceTo(mc.player.getEyePosition())<.01);
            spatialYawBefore=mc.player.getYRot();spatialMove(640,450,0,0);spatialMove(640,450,240,0);
            try{call(mc.mouseHandler,"turnPlayer");}catch(IllegalArgumentException newer){call(mc.mouseHandler,"turnPlayer",.016D);}
            spatialNativeLookApplied=Math.abs(mc.player.getYRot()-spatialYawBefore)>1;
            record("spatial-native-mouse-diagnostic","INFO","active="+call(mc,"isWindowActive")+", grabbed="+field(mc.mouseHandler,"mouseGrabbed")+", dx="+field(mc.mouseHandler,"accumulatedDX")+", ignoreFirst="+field(mc.mouseHandler,"ignoreFirstMove")+", yawBefore="+spatialYawBefore+", yawAfter="+mc.player.getYRot());
            dispatchMovementKey("KEY_2","PRESS");dispatchMovementKey("KEY_2","RELEASE");
            check("spatial-gameplay-number-selects-held-slot",hudSelected(mc.player.getInventory())==1);
            call(mc.mouseHandler,"onScroll",nativeWindowHandle(),0D,-1D);
            check("spatial-gameplay-wheel-selects-held-slot",hudSelected(mc.player.getInventory())==2);
        },20);
        add("spatial-gameplay-mouse-turn",()->{},()->true,()->{
            record("spatial-native-mouse-settled","INFO","yaw="+mc.player.getYRot()+", before="+spatialYawBefore);
            check("spatial-gameplay-mouse-turns-player",spatialNativeLookApplied&&Math.abs(mc.player.getYRot()-spatialYawBefore)>1);
        },5);
        add("spatial-movement-exit",()->{dispatchMovementKey("KEY_LALT","PRESS");dispatchMovementKey("KEY_LALT","RELEASE");},()->!screen().isCameraControlActive(),()->{
            check("spatial-movement-hides-hud-again",com.salts_inventory_update.client.WindowedInventoryClient.shouldHideHotbar());
            check("spatial-movement-keeps-session",sessions().size()==1);capture("spatial-movement-desktop-restored");
        },20);
        add("spatial-rebound-ignores-alt",()->{setMovementBinding("KEY_P");dispatchMovementKey("KEY_LALT","PRESS");dispatchMovementKey("KEY_LALT","RELEASE");},()->true,
            ()->check("spatial-old-binding-inactive",!screen().isCameraControlActive()),4);
        add("spatial-rebound-p-enters",()->{dispatchMovementKey("KEY_P","PRESS");dispatchMovementKey("KEY_P","RELEASE");},()->screen().isCameraControlActive(),
            ()->check("spatial-rebound-gameplay-camera",call(mc.options,"getCameraType")==call(mc.options,"salts$perspective")),8);
        add("spatial-rebound-p-exits",()->{dispatchMovementKey("KEY_P","PRESS");dispatchMovementKey("KEY_P","RELEASE");},()->!screen().isCameraControlActive(),
            ()->{setMovementBinding("KEY_LALT");SaltsInventoryConfig.update(c->c.toggleMovementControl=false);},8);
        add("spatial-hold-unheld",()->{},()->true,()->check("spatial-hold-mode-unheld-no-latch",!screen().isCameraControlActive()),4);
        add("spatial-hold-poll-down",()->spatialHeldInputKey=nativeInputAction("KEY_LALT"),()->screen().isCameraControlActive(),()->{
            check("spatial-held-alt-normal-gameplay-camera",call(mc.options,"getCameraType")==call(mc.options,"salts$perspective"));
            check("spatial-held-alt-restores-hud",!com.salts_inventory_update.client.WindowedInventoryClient.shouldHideHotbar());
        },8);
        add("spatial-hold-poll-up",()->spatialHeldInputKey=-1,()->!screen().isCameraControlActive(),()->check("spatial-held-alt-release-returns-desktop",spatialActive()),8);
        add("spatial-hold-poll-remapped",()->{setMovementBinding("KEY_P");spatialHeldInputKey=nativeInputAction("KEY_P");},()->screen().isCameraControlActive(),()->check("spatial-held-remapped-key-works",true),8);
        add("spatial-hold-poll-remapped-up",()->spatialHeldInputKey=-1,()->!screen().isCameraControlActive(),()->setMovementBinding("KEY_LALT"),8);
        add("spatial-disabled",()->spatialSet("enabled",false),()->!spatialActive(),()->{
            check("spatial-disabled-desktop-retained",screen().hasWindows());capture("spatial-disabled-normal-desktop");
        },15);
        add("spatial-reenabled",()->spatialSet("enabled",true),InWorldGameplayHarness::spatialActive,()->capture("spatial-reenabled"),15);
        add("spatial-first-person-mouse",()->{spatialSet("firstPersonModeInventory",true);spatialSet("firstPersonModeContainers",true);},
            ()->(Boolean)call(ClassFor("org.tastytrash.spatialGUI.client.SpatialGUIClient"),"getEffectiveFirstPersonMode"),()->capture("spatial-first-person-mouse"),20);
        add("spatial-first-person-curved",()->spatialSet("curvedScreenEnabled",true),()->call(call(spatialRenderer(),"getInventoryRenderer"),"getCylinderBasis")!=null,()->{
            var p=slotPoint(1);spatialClick(p[0],p[1]);capture("spatial-first-person-curved");
        },15);
        add("spatial-curved-return",()->{},()->carried()==32,()->{var p=slotPoint(1);spatialClick(p[0],p[1]);},5);
        add("spatial-first-person-crosshair",()->spatialSet("useCrosshairForFirstPerson",true),()->(Boolean)call(ClassFor("org.tastytrash.spatialGUI.render.SpatialGUIRenderer"),"isCrosshairModeActive"),()->{
            check("spatial-crosshair-visible",!com.salts_inventory_update.client.WindowedInventoryClient.shouldHideCrosshair());capture("spatial-first-person-crosshair");
        },15);
        addSpatialWorldRegression();
        add("spatial-detached-hotbar",()->{SaltsInventoryConfig.update(c->c.returnHotbarToInventory=false);screen().refreshInventoryWindowLayout(true);},()->true,()->{
            check("spatial-detached-hotbar-option-preserved",!screen().usesInventoryWindowHotbar());capture("spatial-detached-hotbar-on-panel");
        },15);
        add("spatial-outside-drop-seed",()->server(()->{
            player().inventoryMenu.setCarried(new ItemStack(Items.DIRT,2));mtSync();return null;
        }),()->taskDone()&&carried()==2,()->{},5);
        add("spatial-outside-drop-one",()->{
            spatialMove(0,0,0,0);
            check("spatial-outside-panel-detected",(Boolean)call(ClassFor("com.salts_inventory_update.spatial.SpatialGuiIntegration"),"outsidePanel"));
            check("spatial-no-clamped-edge-slot",call(screen(),"slotAt",0D,0D)==null);
            spatialWorldClick();
        },()->carried()==1,()->{
            var state=server(()->player().inventoryMenu.getCarried().getCount());
            addFirst("spatial-outside-drop-authoritative",()->{},state::isDone,()->check("spatial-outside-drop-only-one",state.join()==1),3);
            check("spatial-outside-drop-does-not-open-world-menu",sessions().size()==2);
        },5);
        add("spatial-outside-drop-rest",()->{
            spatialMove(0,0,0,0);
            spatialMouseButton(left(),"PRESS");spatialMouseButton(left(),"RELEASE");
        },()->carried()==0,()->{},5);
        if(SPATIAL_REPORT) addSpatialVisualEdges();
        if("true".equalsIgnoreCase(System.getenv("SIU_QA_SPATIAL_MANUAL")))
            add("spatial-manual-review",()->{spatialSet("useCrosshairForFirstPerson",false);spatialSet("firstPersonModeInventory",false);spatialSet("firstPersonModeContainers",false);setMovementBinding("KEY_LALT");SaltsInventoryConfig.update(c->c.toggleMovementControl=true);},()->Files.exists(output.resolve("continue")),()->{},10);
        add("spatial-close",InWorldGameplayHarness::closeWindows,()->sessions().isEmpty()&&nativeScreen()==null,()->{
            check("spatial-closed-no-mouse-grab-owner",!screen().isCameraControlActive());capture("spatial-closed-gameplay");
        },20);
    }

    private static boolean findingsHudOnScreen;
    private static Map<UUID,DesktopWindowBounds> wallsBefore;
    private static final List<Double> wallsTransitionSamples=new ArrayList<>();
    private static final List<Double> wallsCameraSamples=new ArrayList<>();
    private static boolean wallsObserveTransition;
    private static DesktopWindowHandle wallsApiHandle;
    private static int wallsApiResizes;
    private static int wallsApiResizeViewport;
    private static void wallsApiFixture() {
        Class<?> api=ClassFor("com.salts_inventory_update.api.client.desktop.DesktopClientApi");
        Class<?> idType=Arrays.stream(api.getMethods()).filter(m->m.getName().equals("registerView")).findFirst().orElseThrow().getParameterTypes()[0];
        Object id;
        try{id=call(idType,"parse","salts_inventory_update:qa_walls");}
        catch(IllegalArgumentException legacy){id=construct(idType,"salts_inventory_update","qa_walls");}
        Class<?> definition=ClassFor("com.salts_inventory_update.api.client.desktop.DesktopWindowDefinition");
        Object instance=Proxy.newProxyInstance(definition.getClassLoader(),new Class<?>[]{definition},(proxy,method,args)->{
            if(method.getName().equals("minSize"))return DesktopWindowSize.of(100,80);
            if(method.getName().equals("defaultSize"))return DesktopWindowSize.of(200,120);
            if(method.getName().equals("resizePolicy"))return DesktopResizePolicy.FREE;
            if(method.getName().equals("resized")){wallsApiResizes++;wallsApiResizeViewport=((Number)call(call(args[0],"environment"),"viewportWidth")).intValue();return null;}
            if(method.getName().equals("toString"))return "WindowWallsPublicApiFixture";
            if(method.isDefault())return InvocationHandler.invokeDefault(proxy,method,args);
            throw new IllegalStateException(method.toString());
        });
        call(api,"registerView",id,Object.class,instance);
        wallsApiHandle=(DesktopWindowHandle)((Optional<?>)call(api,"openView",id,"qa",new Object(),net.minecraft.network.chat.Component.literal("API viewport QA"))).orElseThrow();
        check("walls-api-resize-accepted",wallsApiHandle.resizeTo(mc.getWindow().getGuiScaledWidth()-20,mc.getWindow().getGuiScaledHeight()-20));
        wallsApiResizes=0;
    }

    private static boolean wallsSpatial() {return SPATIAL_ONLY&&!"true".equalsIgnoreCase(System.getenv("SIU_QA_SPATIAL_ABSENT"));}
    private static void wallsCheck(String label) {
        int width=mc.getWindow().getGuiScaledWidth(),height=mc.getWindow().getGuiScaledHeight();
        for(var window:screen().workspaceAccess().windows()) {
            var b=screen().workspaceAccess().presentationBounds(window.handle().instanceId());
            record(label+"-geometry","INFO",window.kind()+" "+b+" viewport="+width+"x"+height);
            check(label+"-"+window.kind(),b.x()>=0&&b.y()>=0&&b.x()+b.width()<=width&&b.y()+b.height()<=height);
        }
        if(wallsSpatial()) {
            var canvas=call(ClassFor("com.salts_inventory_update.spatial.SpatialDesktopCanvas"),"get");
            check(label+"-fixed-canvas",((Number)call(canvas,"width")).doubleValue()==width&&((Number)call(canvas,"height")).doubleValue()==height&&((Number)call(canvas,"scale")).doubleValue()==1);
        }
    }
    private static Map<UUID,DesktopWindowBounds> wallsGeometry() {
        Map<UUID,DesktopWindowBounds> result=new LinkedHashMap<>();
        for(var w:screen().workspaceAccess().windows()) {
            var s=w.handle().state();result.put(w.handle().instanceId(),new DesktopWindowBounds(s.x(),s.y(),s.width(),s.height()));
        }
        return result;
    }
    private static void wallsDrag(int dx,int dy) {
        var s=screen().workspaceAccess().windows().stream().filter(w->w.kind()==DesktopWorkspaceWindow.Kind.PLAYER_INVENTORY).findFirst().orElseThrow().handle().state();
        double x=s.x()+12,y=s.y()+6;
        mouse("mouseClicked",x,y,left(),0,0);
        mouse("mouseDragged",x+dx,y+dy,left(),dx,dy);
        mouse("mouseReleased",x+dx,y+dy,left(),0,0);
    }
    private static void addWindowWallsRegression() {
        add("walls-configure",()->{
            closeWindows();SaltsInventoryConfig.update(c->{c.hasSeenInstructionsWindow=true;c.returnHotbarToInventory=true;c.persistentWindows=false;c.enableWindowSnapping=false;c.toggleMovementControl=false;});
            if(wallsSpatial()){spatialSet("enabled",true);spatialSet("firstPersonModeInventory",false);spatialSet("firstPersonModeContainers",false);spatialSet("transitionDurationMs",350);}
            InventoryDesktopScreen.openOrToggleInventory(mc);
        },()->windowKind("INVENTORY")!=null&&(!wallsSpatial()||spatialActive()),()->wallsCheck("walls-open"),25);
        int[][] edges={{-10000,0},{0,-10000},{10000,0},{0,10000}};
        for(int i=0;i<edges.length;i++) {
            final int side=i;
            add("walls-drag-"+side,()->wallsDrag(edges[side][0],edges[side][1]),()->true,()->{
                wallsCheck("walls-drag-"+side);
                var b=wallsGeometry().values().iterator().next();
                check("walls-touch-"+side,switch(side){case 0->b.x()==0;case 1->b.y()==0;case 2->b.x()+b.width()==mc.getWindow().getGuiScaledWidth();default->b.y()+b.height()==mc.getWindow().getGuiScaledHeight();});
            },3);
        }
        add("walls-repeated-drag",()->{for(int i=0;i<12;i++)wallsDrag(10000,10000);},()->true,()->{wallsCheck("walls-repeated");capture("walls-bottom-right");},5);
        add("walls-resize",()->{
            var w=screen().workspaceAccess().windows().get(0);w.handle().moveTo(30,30);w.handle().resizeTo(10000,10000);
        },()->true,()->{wallsCheck("walls-resize");capture("walls-resize");},5);
        add("walls-api-fixture",InWorldGameplayHarness::wallsApiFixture,()->wallsApiHandle!=null&&wallsApiHandle.isOpen(),()->wallsCheck("walls-api-open"),4);
        add("walls-scale-three",()->{mc.options.guiScale().set(3);resizeHudGui();},()->true,()->{wallsCheck("walls-scale-three");check("walls-api-viewport-resize-notified",wallsApiResizes>0);check("walls-api-resize-current-environment",wallsApiResizeViewport==mc.getWindow().getGuiScaledWidth());capture("walls-scale-three");},8);
        add("walls-scale-two",()->{mc.options.guiScale().set(2);resizeHudGui();},()->true,()->{wallsCheck("walls-scale-two");wallsApiHandle.close();},8);
        if(wallsSpatial()) {
            add("walls-prepare-hold",()->{
                var w=screen().workspaceAccess().windows().get(0);w.handle().resizeTo(178,110);wallsDrag(-10000,0);
                wallsBefore=wallsGeometry();setMovementBinding("KEY_LALT");
                wallsTransitionSamples.clear();wallsCameraSamples.clear();wallsObserveTransition=true;spatialHeldInputKey=nativeInputAction("KEY_LALT");
            },()->screen().isCameraControlActive(),()->{},1);
            add("walls-hold-settled",()->{},()->((Number)call(ClassFor("com.salts_inventory_update.spatial.SpatialMovementTransition"),"blend")).doubleValue()>.999,()->{
                check("walls-hold-animated",wallsTransitionSamples.stream().anyMatch(v->v>.05&&v<.95));
                check("walls-camera-travels-to-eye",wallsCameraSamples.stream().filter(v->v>.1).distinct().count()>3&&wallsCameraSamples.get(wallsCameraSamples.size()-1)<.03);
                check("walls-hold-geometry",wallsBefore.equals(wallsGeometry()));wallsCheck("walls-hold");
                var aligned=spatialMapped(mc.getWindow().getScreenWidth()*.05,mc.getWindow().getScreenHeight()*.5);
                check("walls-normal-panel-pixel-alignment",Math.abs(aligned[0]-mc.getWindow().getGuiScaledWidth()*.05)<2&&Math.abs(aligned[1]-mc.getWindow().getGuiScaledHeight()*.5)<2);
                spatialYawBefore=mc.player.getYRot();spatialMove(640,450,0,0);spatialMove(640,450,170,35);
                capture("walls-hold-normal");
            },12);
            add("walls-hold-native-look",()->spatialMove(640,450,90,-20),()->true,()->{
                check("walls-held-mouse-turns-player",Math.abs(mc.player.getYRot()-spatialYawBefore)>1);
                check("walls-held-camera-at-eye",((net.minecraft.world.phys.Vec3)spatialCameraPosition()).distanceTo(mc.player.getEyePosition())<.03);
                check("walls-held-geometry-stable",wallsBefore.equals(wallsGeometry()));
                record("walls-hold-animation-samples","INFO",wallsTransitionSamples.toString());
                record("walls-hold-camera-samples","INFO",wallsCameraSamples.toString());
                wallsTransitionSamples.clear();wallsCameraSamples.clear();spatialHeldInputKey=-1;
            },8);
            add("walls-hold-return",()->{},()->!screen().isCameraControlActive()&&((Number)call(ClassFor("com.salts_inventory_update.spatial.SpatialMovementTransition"),"blend")).doubleValue()<.001,()->{
                check("walls-return-animated",wallsTransitionSamples.stream().anyMatch(v->v>.05&&v<.95));
                check("walls-camera-travels-from-eye",wallsCameraSamples.stream().filter(v->v>.1).distinct().count()>3&&wallsCameraSamples.get(wallsCameraSamples.size()-1)>.5);
                check("walls-return-geometry",wallsBefore.equals(wallsGeometry()));wallsCheck("walls-return");capture("walls-return-spatial");
                wallsObserveTransition=false;
            },12);
            if("true".equalsIgnoreCase(System.getenv("SIU_QA_SPATIAL_MANUAL"))&&!"true".equalsIgnoreCase(System.getenv("SIU_QA_SPATIAL_WITH_REGRESSION")))
                add("walls-manual",()->SaltsInventoryConfig.update(c->c.toggleMovementControl=true),()->Files.exists(output.resolve("continue")),()->{},10);
        }
        add("walls-cleanup",InWorldGameplayHarness::closeWindows,()->sessions().isEmpty(),()->{},5);
    }
    private static void addSpatialHotbarProbe() {
        add("findings-hud-config",()->{closeWindows();SaltsInventoryConfig.update(c->{c.hasSeenInstructionsWindow=true;c.expandableInventory=false;c.returnHotbarToInventory=false;c.toggleMovementControl=true;});spatialSet("firstPersonModeInventory",false);spatialSet("firstPersonModeContainers",false);},()->true,()->{},3);
        addContainer("findings-hud-chest","minecraft:chest",0);STEPS.removeLast();
        spatialFinding("hud-probe-baseline",()->{InventoryDesktopScreen.openOrToggleInventory(mc);for(var w:screen().workspaceAccess().windows())w.handle().moveTo(w.kind()==DesktopWorkspaceWindow.Kind.PLAYER_INVENTORY?260:30,110);spatialMove(640,450,0,0);});
        add("findings-hud-pickup",()->{var p=slotPoint(0);spatialClick(p[0],p[1]);},()->carried()==32,()->{},8);
        spatialFinding("hud-probe-hover",()->{var p=hudPoint(2);spatialPoint(p[0],p[1]);});
        add("findings-hud-place",()->{
            var p=hudPoint(2);spatialPoint(p[0],p[1]);var util=ClassFor("org.tastytrash.spatialGUI.util.MouseHandlerUtil");
            double x=((Number)call(util,"getSourceX")).doubleValue(),y=((Number)call(util,"getSourceY")).doubleValue();
            findingsHudOnScreen=x>=0&&y>=0&&x<mc.getWindow().getScreenWidth()&&y<mc.getWindow().getScreenHeight();
            record("finding-hud-target-reachable",findingsHudOnScreen?"PASS":"INFO","physical target="+x+","+y+"; monitor="+mc.getWindow().getScreenWidth()+","+mc.getWindow().getScreenHeight());
            if(findingsHudOnScreen)spatialClick(p[0],p[1]);
        },()->!findingsHudOnScreen||carried()==0,()->{
            capture("finding-hud-probe-result");
            if(findingsHudOnScreen){var state=server(()->player().getInventory().getItem(2).getCount());addFirst("findings-hud-server",()->{},state::isDone,()->check("finding-hud-server-stack32",state.join()==32),3);}
        },15);
        add("findings-hud-close",InWorldGameplayHarness::closeWindows,()->sessions().isEmpty(),()->{},10);
    }
    private static void spatialFinding(String name,Runnable action) {
        add("finding-"+name,action,InWorldGameplayHarness::spatialActive,()->{
            capture("finding-"+name);
            var info=new LinkedHashMap<String,Object>();
            info.put("frame",List.of(mc.getWindow().getScreenWidth(),mc.getWindow().getScreenHeight()));
            info.put("gui",List.of(mc.getWindow().getGuiScaledWidth(),mc.getWindow().getGuiScaledHeight()));
            info.put("windows",screen().workspaceAccess().windows().stream().map(w->w.handle().state()).toList());
            Object camera=spatialCamera();
            info.put("camera",List.of(call(camera,"yRot"),call(camera,"xRot")));
            info.put("carried",carried());info.put("movement",screen().isCameraControlActive());
            try {
                var fit=field(ClassFor("com.salts_inventory_update.spatial.SpatialPanelPresentation"),"lastFit");
                var bounds=call(fit,"bounds");var available=call(fit,"available");
                info.put("fitBounds",bounds);info.put("fitAvailable",available);
                double epsilon=.003;
                check("fit-contained-"+name,((Number)call(bounds,"left")).doubleValue()>=((Number)call(available,"left")).doubleValue()-epsilon
                    &&((Number)call(bounds,"right")).doubleValue()<=((Number)call(available,"right")).doubleValue()+epsilon
                    &&((Number)call(bounds,"bottom")).doubleValue()>=((Number)call(available,"bottom")).doubleValue()-epsilon
                    &&((Number)call(bounds,"top")).doubleValue()<=((Number)call(available,"top")).doubleValue()+epsilon);
                var canvas=call(ClassFor("com.salts_inventory_update.spatial.SpatialDesktopCanvas"),"get");info.put("canvas",canvas);
            } catch(RuntimeException oldArtifact) {if("true".equalsIgnoreCase(System.getenv("SIU_QA_SPATIAL_FIXES")))throw oldArtifact;}
            var config=new LinkedHashMap<String,Object>();
            for(String key:List.of("firstPersonModeInventory","firstPersonModeContainers","curvedScreenEnabled","useCrosshairForFirstPerson","disableFirstPersonParallax","disableThirdPersonParallax","firstPersonScreenScale","screenScale","hideHud","hideHotbar")) {
                try{config.put(key,field(spatialConfig(),key));}catch(IllegalArgumentException missing){}
            }
            info.put("spatial",config);
            try{Files.writeString(output.resolve("finding-"+name+".json"),new GsonBuilder().setPrettyPrinting().create().toJson(info));}catch(Exception e){throw new IllegalStateException(e);}
        },30);
    }
    private static void spatialArrange(int x,int y) {
        var w=screen().workspaceAccess().windows().stream().filter(v->v.kind()==DesktopWorkspaceWindow.Kind.PLAYER_INVENTORY).findFirst().orElseThrow();w.handle().moveTo(x,y);
    }
    private static void addSpatialFindingsRegression() {
        add("findings-configure",()->{
            closeWindows();SaltsInventoryConfig.update(c->{c.hasSeenInstructionsWindow=true;c.expandableInventory=false;c.persistentWindows=false;c.returnHotbarToInventory=false;c.toggleMovementControl=true;c.enableWindowSnapping=false;});
            setMovementBinding("KEY_LALT");spatialSet("enabled",true);spatialSet("firstPersonModeInventory",false);spatialSet("firstPersonModeContainers",false);spatialSet("useCrosshairForFirstPerson",false);
        },()->true,()->{},3);
        addContainer("findings-chest","minecraft:chest",0);STEPS.removeLast();
        spatialFinding("baseline",()->{InventoryDesktopScreen.openOrToggleInventory(mc);for(var w:screen().workspaceAccess().windows())w.handle().moveTo(w.kind()==DesktopWorkspaceWindow.Kind.PLAYER_INVENTORY?260:30,110);spatialMove(640,450,0,0);});
        spatialFinding("left-mid",()->{screen().workspaceAccess().windows().stream().filter(w->w.kind()==DesktopWorkspaceWindow.Kind.OTHER).findFirst().orElseThrow().handle().moveTo(0,210);});
        spatialFinding("left-bottom",()->{screen().workspaceAccess().windows().stream().filter(w->w.kind()==DesktopWorkspaceWindow.Kind.OTHER).findFirst().orElseThrow().handle().moveTo(0,290);});
        spatialFinding("right-mid",()->{var w=screen().workspaceAccess().windows().stream().filter(v->v.kind()==DesktopWorkspaceWindow.Kind.PLAYER_INVENTORY).findFirst().orElseThrow();spatialArrange(mc.getWindow().getGuiScaledWidth()-w.handle().state().width(),110);});
        spatialFinding("bottom-right",()->{var s=screen().workspaceAccess().windows().stream().filter(v->v.kind()==DesktopWorkspaceWindow.Kind.PLAYER_INVENTORY).findFirst().orElseThrow().handle().state();spatialArrange(mc.getWindow().getGuiScaledWidth()-s.width(),mc.getWindow().getGuiScaledHeight()-s.height());});
        spatialFinding("large-window",()->{var w=screen().workspaceAccess().windows().stream().filter(v->v.kind()==DesktopWorkspaceWindow.Kind.PLAYER_INVENTORY).findFirst().orElseThrow();w.handle().resizeTo(370,280);w.handle().moveTo(200,100);});
        spatialFinding("carry-layout",()->{var w=screen().workspaceAccess().windows().stream().filter(v->v.kind()==DesktopWorkspaceWindow.Kind.PLAYER_INVENTORY).findFirst().orElseThrow();w.handle().resizeTo(230,170);w.handle().moveTo(280,110);screen().workspaceAccess().windows().stream().filter(v->v.kind()==DesktopWorkspaceWindow.Kind.OTHER).findFirst().orElseThrow().handle().moveTo(30,130);});
        add("findings-pickup",()->{var p=slotPoint(0);spatialClick(p[0],p[1]);},()->carried()==32,()->check("finding-real-server-menu-pickup",true),8);
        spatialFinding("carry-interior",()->spatialPoint(200,210));
        spatialFinding("carry-title",()->spatialPoint(75,115));
        spatialFinding("carry-panel-left",()->spatialPoint(1,210));
        spatialFinding("carry-outside",()->spatialMove(0,0,0,0));
        add("findings-return-stack",()->{var p=slotPoint(0);spatialClick(p[0],p[1]);},()->carried()==0,()->assertContainer("finding-stack-returned",0,32,0),8);
        spatialFinding("hotbar-scale-three",()->{mc.options.guiScale().set(3);resizeHudGui();});
        spatialFinding("hotbar-scale-two",()->{mc.options.guiScale().set(2);resizeHudGui();});
        spatialFinding("first-mouse-center",()->{spatialSet("firstPersonModeInventory",true);spatialSet("firstPersonModeContainers",true);spatialMove(640,450,0,0);});
        spatialFinding("first-mouse-corner",()->spatialMove(0,0,0,0));
        spatialFinding("first-no-parallax",()->spatialSet("disableFirstPersonParallax",true));
        spatialFinding("first-curved",()->{spatialSet("curvedScreenEnabled",true);spatialMove(640,450,0,0);});
        spatialFinding("first-crosshair",()->spatialSet("useCrosshairForFirstPerson",true));
        spatialFinding("third-no-parallax",()->{spatialSet("curvedScreenEnabled",false);spatialSet("useCrosshairForFirstPerson",false);spatialSet("firstPersonModeInventory",false);spatialSet("firstPersonModeContainers",false);spatialSet("disableThirdPersonParallax",true);});
        spatialFinding("wide-frame",()->{call(mc.getWindow(),"setWindowed",1280,720);resizeHudGui();});
        spatialFinding("tall-frame",()->{call(mc.getWindow(),"setWindowed",1000,900);resizeHudGui();});
        spatialFinding("restored-frame",()->{call(mc.getWindow(),"setWindowed",1280,900);resizeHudGui();});
        spatialFinding("gameplay-camera",()->{dispatchMovementKey("KEY_LALT","PRESS");dispatchMovementKey("KEY_LALT","RELEASE");});
        spatialFinding("desktop-return",()->{dispatchMovementKey("KEY_LALT","PRESS");dispatchMovementKey("KEY_LALT","RELEASE");});
        if("true".equalsIgnoreCase(System.getenv("SIU_QA_SPATIAL_MANUAL"))) add("spatial-manual-review",()->{},()->Files.exists(output.resolve("continue")),()->{},10);
        add("findings-close",InWorldGameplayHarness::closeWindows,()->sessions().isEmpty(),()->capture("finding-closed"),15);
    }

    /** Extra visual evidence only; production JARs and normal focused profiles are unchanged. */
    private static void addSpatialVisualEdges() {
        add("spatial-report-overlap",()->{
            spatialSet("useCrosshairForFirstPerson",false);spatialSet("curvedScreenEnabled",false);
            spatialSet("firstPersonModeInventory",false);spatialSet("firstPersonModeContainers",false);
            SaltsInventoryConfig.update(c->c.returnHotbarToInventory=true);screen().refreshInventoryWindowLayout(true);
            int i=0;for(var w:screen().workspaceAccess().windows())w.handle().moveTo(18+90*i,35+24*i++);
        },InWorldGameplayHarness::spatialActive,()->check("report-three-windows-one-panel",screen().workspaceAccess().windows().size()==3),25);
        add("spatial-report-front-window",()->{
            var first=screen().workspaceAccess().windows().get(0);check("report-window-focus-request",first.handle().focus());
        },()->true,()->{},12);
        add("spatial-report-resized-window",()->{
            var w=screen().workspaceAccess().windows().stream().filter(v->v.kind()==DesktopWorkspaceWindow.Kind.PLAYER_INVENTORY).findFirst().orElseThrow();
            var s=w.handle().state();check("report-resize-request",w.handle().resizeTo(s.width()+50,s.height()+25));w.handle().moveTo(260,75);
        },()->true,()->{},20);
        add("spatial-report-panel-edge",()->{
            var w=screen().workspaceAccess().windows().get(0);var s=w.handle().state();
            w.handle().moveTo(mc.getWindow().getGuiScaledWidth()-s.width(),mc.getWindow().getGuiScaledHeight()-s.height());spatialMove(0,0,0,0);
        },()->true,()->check("report-outside-edge-no-slot",call(screen(),"slotAt",0D,0D)==null),15);
        add("spatial-report-explicit-exclusion",()->spatialSet("disabledScreens",new ArrayList<>(List.of(screen().getClass().getName()))),()->!spatialActive(),()->check("report-exclusion-keeps-windows",screen().workspaceAccess().windows().size()==3),25);
        add("spatial-report-exclusion-restored",()->spatialSet("disabledScreens",new ArrayList<String>()),InWorldGameplayHarness::spatialActive,()->{},25);
        add("spatial-report-containers-disabled",()->spatialSet("containers",false),()->!spatialActive(),()->{},25);
        add("spatial-report-containers-restored",()->spatialSet("containers",true),InWorldGameplayHarness::spatialActive,()->{},25);
        add("spatial-report-perspective-exclusion",()->spatialSet("disableFirstPerson",true),()->!spatialActive(),()->{},25);
        add("spatial-report-perspective-restored",()->spatialSet("disableFirstPerson",false),InWorldGameplayHarness::spatialActive,()->{},25);
        add("spatial-report-single-player-window",()->{closeWindows();InventoryDesktopScreen.openOrToggleInventory(mc);},()->sessions().isEmpty()&&screen().workspaceAccess().windows().size()==1&&spatialActive(),()->{},25);
        add("spatial-report-inventory-disabled",()->spatialSet("inventory",false),()->!spatialActive(),()->{},25);
        add("spatial-report-inventory-restored",()->spatialSet("inventory",true),InWorldGameplayHarness::spatialActive,()->{},25);
        add("spatial-report-autodetect-first",()->spatialSet("autoDetectCameraMode",true),()->(Boolean)call(ClassFor("org.tastytrash.spatialGUI.client.SpatialGUIClient"),"getEffectiveFirstPersonMode"),()->{},25);
        add("spatial-report-autodetect-third",()->call(mc.options,"setCameraType",enumValue("net.minecraft.client.CameraType","THIRD_PERSON_BACK")),()->!(Boolean)call(ClassFor("org.tastytrash.spatialGUI.client.SpatialGUIClient"),"getEffectiveFirstPersonMode"),()->{},25);
        add("spatial-report-third-exclusion",()->spatialSet("disableThirdPerson",true),()->!spatialActive(),()->{},25);
        add("spatial-report-third-restored",()->{spatialSet("disableThirdPerson",false);spatialSet("autoDetectCameraMode",false);call(mc.options,"setCameraType",enumValue("net.minecraft.client.CameraType","FIRST_PERSON"));},InWorldGameplayHarness::spatialActive,()->{},25);
        add("spatial-report-gui-scale-three",()->{mc.options.guiScale().set(3);resizeHudGui();},InWorldGameplayHarness::spatialActive,()->check("report-rescale-retains-inventory",windowKind("INVENTORY")!=null),25);
        add("spatial-report-gui-scale-two",()->{mc.options.guiScale().set(2);resizeHudGui();},InWorldGameplayHarness::spatialActive,()->{},25);
        add("spatial-report-hide-hud",()->{SaltsInventoryConfig.update(c->c.returnHotbarToInventory=false);screen().refreshInventoryWindowLayout(true);spatialSet("hideHud",true);},()->true,()->check("report-spatial-hide-hud-respected",(Boolean)call(ClassFor("org.tastytrash.spatialGUI.client.SpatialGUIClient"),"shouldHideHud")),25);
        add("spatial-report-hide-hotbar",()->{spatialSet("hideHud",false);spatialSet("hideHotbar",true);},()->true,()->check("report-spatial-hide-hotbar-respected",(Boolean)call(ClassFor("org.tastytrash.spatialGUI.client.SpatialGUIClient"),"shouldHideHotbar")),25);
        add("spatial-report-hud-restored",()->{spatialSet("hideHotbar",false);SaltsInventoryConfig.update(c->c.returnHotbarToInventory=true);screen().refreshInventoryWindowLayout(true);},()->true,()->{},25);
        addContainer("report-manual-chest","minecraft:chest",0);STEPS.removeLast();
        add("spatial-report-manual-layout",()->{InventoryDesktopScreen.openOrToggleInventory(mc);for(var w:screen().workspaceAccess().windows())w.handle().moveTo(w.kind()==DesktopWorkspaceWindow.Kind.PLAYER_INVENTORY?240:8,35);},InWorldGameplayHarness::spatialActive,()->{},25);
    }

    private static boolean mtShift;
    private static com.salts_inventory_update.api.client.recipebrowser.RecipeBrowserTab searchItemTab;
    private static int searchCodexVariants;
    private static ItemStack searchItem(com.salts_inventory_update.api.client.recipebrowser.RecipeBrowserEntry entry) {
        Object value=entry.ingredient();
        if(value instanceof ItemStack stack)return stack;
        try {Object nested=call(value,"getValue");return nested instanceof ItemStack stack?stack:ItemStack.EMPTY;}
        catch(RuntimeException ignored){return ItemStack.EMPTY;}
    }
    private static void addAncientRemnantsSearchRegression() {
        add("ancient-search-configure", () -> SaltsInventoryConfig.update(c -> c.hasSeenInstructionsWindow=true),
            () -> true, () -> {
                check("ancient-remnants-loaded",com.salts_inventory_update.api.integration.DesktopEnvironment.isModLoaded("ancient_remnants"));
                check("compatibility-loaded",com.salts_inventory_update.api.integration.DesktopEnvironment.isModLoaded("salts_inventory_compat"));
            }, 2);
        add("ancient-search-open", () -> { closeWindows(); InventoryDesktopScreen.openOrToggleJei(mc); },
            () -> windowKind("JEI") != null, () -> {
                var access=com.salts_inventory_update.api.client.recipebrowser.RecipeBrowserBridge.access();
                String expected=SCENARIO.equals("browser-rei")?"roughlyenoughitems":SCENARIO.substring("browser-".length());
                check("search-provider-available",access.isAvailable());
                check("search-provider-selected",expected.equals(access.source().id()));
                var tabs=access.tabs();
                searchItemTab=tabs.stream().filter(t->t.uid().toLowerCase(Locale.ROOT).contains("item") && t.kind()==com.salts_inventory_update.api.client.recipebrowser.RecipeBrowserTabKind.INGREDIENTS).findFirst().orElseThrow();
                Object w=windowKind("JEI");
                try { var selected=w.getClass().getDeclaredField("jeiSelectedTabUid"); selected.setAccessible(true); selected.set(w,searchItemTab.uid()); } catch(ReflectiveOperationException e) { throw new IllegalStateException(e); }
                call(screen(),"setJeiSearchText",w,"");
                var entries=access.filteredEntries(searchItemTab);
                check("search-baseline-populated",!entries.isEmpty());
                var codices=entries.stream().filter(e->{var stack=searchItem(e);return stack!=null&&!stack.isEmpty()&&stack.getItem()==registryValue("ITEM","ancient_remnants:monolith_codex");}).toList();
                check("search-baseline-codex-present",!codices.isEmpty());
                searchCodexVariants=codices.size();
                for(var codex:codices)check("codex-world-tooltip",!access.tooltip(codex).isEmpty());
                record("codex-variants","PASS",Integer.toString(codices.size()));
                capture("ancient-search-before");
            }, 15);
        // The pinned JEI builds use $ for tooltips and # for tags; REI uses # for tooltips.
        String tooltipQuery=SCENARIO.equals("browser-jei")?"$r":"#r";
        String codexTooltipQuery=SCENARIO.equals("browser-jei")?"$pages":"#codex";
        for(String query:List.of("r",tooltipQuery,"monolith",codexTooltipQuery,"@ancient_remnants","crafting table","salts_no_matching_item_921")) {
            String name="ancient-query-"+query.replaceAll("[^a-zA-Z0-9]","-");
            add(name, () -> call(screen(),"setJeiSearchText",windowKind("JEI"),query),
                () -> true, () -> {
                    var access=com.salts_inventory_update.api.client.recipebrowser.RecipeBrowserBridge.access();
                    var entries=access.filteredEntries(searchItemTab);
                    if(query.startsWith("salts_no_"))check("search-no-results",entries.isEmpty());
                    else check("search-results-"+query,!entries.isEmpty());
                    if(query.equals("monolith")||query.equals(codexTooltipQuery)||query.equals("@ancient_remnants"))
                        check("search-keeps-codex-"+query,entries.stream().filter(e->{var s=searchItem(e);return s!=null&&!s.isEmpty()&&s.getItem()==registryValue("ITEM","ancient_remnants:monolith_codex");}).count()==searchCodexVariants);
                    capture(name);
                }, 12);
        }
        add("ancient-search-clear",()->call(screen(),"setJeiSearchText",windowKind("JEI"),""),()->true,()->{
            var access=com.salts_inventory_update.api.client.recipebrowser.RecipeBrowserBridge.access();
            check("search-clear-restores-entries",!access.filteredEntries(searchItemTab).isEmpty());capture("ancient-search-cleared");
            closeWindows();
        }, 10);
    }
    public static boolean mouseTweaksShiftHeld() {return mtShift;}
    /** Setup and read-only server observation only. All subsequent gestures come from Computer Use. */
    private static void addManualMouseTweaksFixture() {
        add("manual-configure", () -> SaltsInventoryConfig.update(c -> {
            c.hasSeenInstructionsWindow=true;c.expandableInventory=false;c.openUnlocked=true;
            c.allowResizing=true;c.enableWindowSnapping=false;
        }), () -> true, () -> {}, 2);
        add("manual-blocks-and-items", () -> server(() -> {
            var level=serverLevel();
            for(int i=0;i<4;i++) {
                String block=i==1?"minecraft:barrel":i==3?"minecraft:crafting_table":"minecraft:chest";
                BlockPos pos=origin.offset((i%2)*2,0,(i/2)*2);
                Object value=registryValue("BLOCK",block);
                level.setBlock(pos,(net.minecraft.world.level.block.state.BlockState)call(value,"defaultBlockState"),3);
                if(level.getBlockEntity(pos) instanceof Container c) {
                    c.clearContent();c.setItem(0,new ItemStack(Items.STONE,i==0?32:20));
                    c.setItem(1,new ItemStack(Items.STONE,16));c.setItem(2,new ItemStack(Items.DIRT,10));
                    c.setItem(3,new ItemStack(Items.STONE,4));
                }
            }
            var inventory=player().getInventory();inventory.clearContent();
            inventory.setItem(9,new ItemStack(Items.STONE,24));inventory.setItem(10,new ItemStack(Items.STONE,8));
            inventory.setItem(11,new ItemStack(Items.DIRT,12));inventory.setItem(12,new ItemStack(Items.OAK_PLANKS,16));
            inventory.setItem(13,new ItemStack(Items.IRON_INGOT,3));
            player().inventoryMenu.setCarried(ItemStack.EMPTY);mtSync();return null;
        }), InWorldGameplayHarness::taskDone, () -> {}, 8);
        for(int index=0;index<4;index++) {
            final int i=index;
            if(i==2)add("manual-register-api",InWorldGameplayHarness::mtRegisterWindow,()->true,()->{},2);
            add("manual-open-"+i, () -> server(() -> {
                var level=serverLevel();BlockPos pos=origin.offset((i%2)*2,0,(i/2)*2);
                player().openMenu((net.minecraft.world.MenuProvider)call(level.getBlockState(pos),"getMenuProvider",level,pos));
                if(i==3 && "true".equalsIgnoreCase(System.getenv("SIU_QA_MANUAL_CRAFTING_READY"))) {
                    player().getInventory().clearContent();
                    player().getInventory().setItem(9,new ItemStack(Items.STONE,20));
                    player().getInventory().setItem(10,new ItemStack(Items.DIRT,12));
                    player().getInventory().setItem(11,new ItemStack(Items.OAK_PLANKS,12));
                    player().getInventory().setItem(12,new ItemStack(Items.IRON_INGOT,3));
                    Object owner=call(ClassFor("com.salts_inventory_update.server.DesktopContainerSessions"),"sessions",player());
                    AbstractContainerMenu crafting=((Map<?,?>)field(owner,"sessions")).values().stream()
                        .map(session->(AbstractContainerMenu)field(session,"menu"))
                        .filter(menu->menu instanceof net.minecraft.world.inventory.CraftingMenu).findFirst().orElseThrow();
                    for(int slot:List.of(1,2,4,5))crafting.getSlot(slot).set(new ItemStack(Items.OAK_PLANKS,1));
                    crafting.broadcastFullState();mtSync();
                }
                return null;
            }), () -> taskDone() && sessions().size()>=i+1, () -> {},8);
        }
        add("manual-layout",()->{
            InventoryDesktopScreen.openOrToggleInventory(mc);
            int[][] locations={{8,8},{328,8},{328,205},{8,205}};
            for(int i=0;i<4;i++) {
                Object w=windowSession(sessions().get(i).sessionId());
                try {for(String axis:List.of("x","y")) {var f=w.getClass().getDeclaredField(axis);f.setAccessible(true);f.setInt(w,locations[i][axis.equals("x")?0:1]);}}
                catch(ReflectiveOperationException e){throw new IllegalStateException(e);}
            }
        },()->windowKind("INVENTORY")!=null,()->{
            try{Files.writeString(output.resolve("manual-ready.json"),new GsonBuilder().setPrettyPrinting().create().toJson(Map.of("minecraft",VersionInfo.MINECRAFT_VERSION,"mods",loadedMods(),"core",SaltsInventoryUpdate.class.getProtectionDomain().getCodeSource().getLocation().toString())));}
            catch(Exception e){throw new IllegalStateException(e);}
        },5);
    }
    private static List<String> manualContents(Container container) {
        List<String> items=new ArrayList<>();
        for(int i=0;i<container.getContainerSize();i++) {
            ItemStack stack=container.getItem(i);
            if(!stack.isEmpty())items.add(i+":"+stack.getItem()+"="+stack.getCount());
        }
        return items;
    }
    private static void manualMouseTweaksObserve() {
        if(elapsedTicks%20!=0)return;
        mc.getSingleplayerServer().execute(()->{
            try {
                Map<String,Object> snapshot=new LinkedHashMap<>();
                snapshot.put("time",System.currentTimeMillis());snapshot.put("player",manualContents(player().getInventory()));
                snapshot.put("cursor",player().inventoryMenu.getCarried().toString());
                for(int i=0;i<3;i++)if(serverLevel().getBlockEntity(origin.offset((i%2)*2,0,(i/2)*2)) instanceof Container c)snapshot.put("container"+i,manualContents(c));
                Files.writeString(output.resolve("manual-state.json"),new GsonBuilder().setPrettyPrinting().create().toJson(snapshot));
            }catch(Exception e){SaltsInventoryUpdate.LOGGER.error("Manual inventory observation failed",e);}
        });
    }
    private static DesktopHudApi.RegistrationHandle raisedQaRegistration;
    private static String raisedOriginalConfig;
    private static final boolean RAISED_ABSENT = "true".equalsIgnoreCase(System.getenv("SIU_QA_RAISED_ABSENT"));

    private static void resizeHudGui() {
        try { call(mc, "resizeGui"); } catch (IllegalArgumentException legacy) { call(mc, "resizeDisplay"); }
    }
    private static DesktopHudLayout hudLayout() { return (DesktopHudLayout) call(screen(), "detachedHudLayout"); }
    private static DesktopHudSlot hudSlot(int index) {
        return hudLayout().slots().stream().filter(slot -> index == 9
            ? slot.kind() == DesktopHudSlot.Kind.OFFHAND
            : slot.kind() == DesktopHudSlot.Kind.HOTBAR && slot.index() == index).findFirst().orElseThrow();
    }
    private static double[] hudPoint(int index) {
        var slot = hudSlot(index); return new double[] {slot.transform().screenX(8), slot.transform().screenY(8)};
    }
    private static void clickHud(int index) { var point = hudPoint(index); clickAt(point[0], point[1]); }
    private static void raisedConfig(int x, int y, boolean groups) {
        if (RAISED_ABSENT) return;
        try {
            Path path = ((java.io.File) field(ClassFor("dev.yurisuika.raised.config.Config"), "file")).toPath();
            if (raisedOriginalConfig == null) raisedOriginalConfig = Files.readString(path);
            String configured = groups ? "{\"Default\":{\"offset\":{\"x\":0,\"y\":2},\"layers\":{\"minecraft:hotbar\":{\"position\":\"BOTTOM\"}}},\"Additional\":{\"offset\":{\"x\":" + x + ",\"y\":" + y + "},\"layers\":{\"minecraft:hotbar\":{\"position\":\"BOTTOM_LEFT\"}}}}" : "{}";
            Files.writeString(path, "{\"groups\":" + configured + ",\"settings\":{\"selectionIndicator\":\"AUTO\"}}");
            call(ClassFor("dev.yurisuika.raised.config.Config"), "load");
        } catch (Exception failure) { throw new IllegalStateException(failure); }
    }
    private static void verifyHud(String label, int dx, int dy) {
        var window = mc.getWindow();
        for (int index = 0; index < 10; index++) {
            var geometry = hudSlot(index);
            int baseX = window.getGuiScaledWidth() / 2 - 88 + (index == 9 ? -29 : index * 20);
            int baseY = window.getGuiScaledHeight() - 19;
            check(label + "-geometry-" + index, geometry.transform().x() == baseX + dx && geometry.transform().y() == baseY + dy);
            var point = hudPoint(index);
            Object hit = call(screen(), "detachedHudSlotAt", point[0], point[1]);
            check(label + "-hit-" + index, hit != null && ((Slot) call(hit, "slot")).getContainerSlot() == (index == 9 ? 40 : index));
            check(label + "-world-ownership-" + index, (Boolean) call(screen(), "isPointOverInteractiveUi", point[0], point[1]));
            Object proxy = call(screen(), "mouseTweaksSlotAt", point[0], point[1]);
            check(label + "-mouse-tweaks-target-" + index, proxy instanceof Slot target
                && target.x == geometry.transform().pixelX(0) && target.y == geometry.transform().pixelY(0));
        }
    }
    private static void addHudVisibilityRegression() {
        add("hud-visibility-configure", () -> {
            closeWindows();
            SaltsInventoryConfig.update(config -> {
                config.persistentWindows = false; config.hasSeenInstructionsWindow = true;
                config.returnHotbarToInventory = false;
            });
            mc.options.guiScale().set(2); resizeHudGui();
            server(() -> {
                command("gamemode survival"); command("gamerule naturalRegeneration false");
                command("item replace entity @p armor.chest with minecraft:iron_chestplate");
                command("experience set @p 7 levels"); command("experience add @p 3 points");
                player().setHealth(14.0F); player().getFoodData().setFoodLevel(14);
                return null;
            });
        }, InWorldGameplayHarness::taskDone, () -> {}, 10);
        add("hud-open-option-off", () -> InventoryDesktopScreen.openOrToggleInventory(mc),
            () -> windowKind("INVENTORY") != null, () -> {
                for (var window : screen().workspaceAccess().windows()) window.handle().moveTo(8, 8);
            }, 3);
        add("hud-visible-option-off", () -> {}, () -> true, () -> {
            check("hud-option-off-visible", !com.salts_inventory_update.client.WindowedInventoryClient.shouldHideHotbar());
            capture("hud-option-off");
        }, 8);
        add("hud-hidden-option-on", () -> {
            SaltsInventoryConfig.update(config -> config.returnHotbarToInventory = true);
            screen().refreshInventoryWindowLayout(false);
        },
            () -> true, () -> {
                check("hud-option-on-hidden", com.salts_inventory_update.client.WindowedInventoryClient.shouldHideHotbar());
                capture("hud-option-on");
            }, 8);
        add("hud-visible-toggle-back", () -> {
            SaltsInventoryConfig.update(config -> config.returnHotbarToInventory = false);
            screen().refreshInventoryWindowLayout(true);
        },
            () -> true, () -> {
                check("hud-toggle-back-visible", !com.salts_inventory_update.client.WindowedInventoryClient.shouldHideHotbar());
                capture("hud-toggle-back");
            }, 8);
        add("hud-closed-option-on", () -> {
            SaltsInventoryConfig.update(config -> config.returnHotbarToInventory = true); closeWindows();
        }, () -> sessions().isEmpty() && !(nativeScreen() instanceof InventoryDesktopScreen), () -> {
            check("hud-closed-visible", !com.salts_inventory_update.client.WindowedInventoryClient.shouldHideHotbar());
            capture("hud-closed");
        }, 8);
        add("hud-reopen-option-on", () -> InventoryDesktopScreen.openOrToggleInventory(mc),
            () -> windowKind("INVENTORY") != null, () -> {
                check("hud-reopen-hidden", com.salts_inventory_update.client.WindowedInventoryClient.shouldHideHotbar());
                capture("hud-reopen");
            }, 8);
        if (HUD_MOVEMENT_ONLY || FULL_CORE) addHudMovementRegression();
        add("hud-visibility-cleanup", InWorldGameplayHarness::closeWindows, () -> sessions().isEmpty(), () -> {}, 3);
    }

    private static void addHudMovementRegression() {
        add("hud-movement-seed", () -> {
            SaltsInventoryConfig.update(config -> config.toggleMovementControl = true);
            setMovementBinding("KEY_LALT");
            com.salts_inventory_update.client.WindowedInventoryClient.resetMovementControl();
            SaltsInventoryConfig.update(config -> config.returnHotbarToInventory = false);
            call(screen(),"selectHotbarSlot",0);
            SaltsInventoryConfig.update(config -> config.returnHotbarToInventory = true);
            server(() -> {
                for (int i = 0; i < 9; i++) player().getInventory().setItem(i, new ItemStack(Items.DIRT, i + 1));
                player().getInventory().setItem(9, new ItemStack(Items.SAND, 3));
                player().inventoryMenu.broadcastFullState();
                mtSync();
                return null;
            });
        }, () -> taskDone() && mc.player.getInventory().getItem(9).is(Items.SAND), () -> {
            check("hud-initial-selected-slot", hudSelected(mc.player.getInventory()) == 0);
        }, 5);
        add("hud-hidden-selection-blocked", () -> {
            hudPointerOutside();
            call(mc.mouseHandler, "onScroll", nativeWindowHandle(), 0D, -1D);
            check("hud-hidden-wheel-forward-blocked", hudSelected(mc.player.getInventory()) == 0);
            call(mc.mouseHandler, "onScroll", nativeWindowHandle(), 0D, 1D);
            check("hud-hidden-wheel-backward-blocked", hudSelected(mc.player.getInventory()) == 0);
            for (int i = 1; i <= 9; i++) {
                dispatchMovementKey("KEY_" + i, "PRESS"); dispatchMovementKey("KEY_" + i, "RELEASE");
                check("hud-hidden-number-" + i + "-blocked", hudSelected(mc.player.getInventory()) == 0);
            }
        }, () -> true, () -> {
            check("hud-hidden-wheel-blocked", hudSelected(mc.player.getInventory()) == 0);
            hudServerSelected("hidden-selection", 0);
            dispatchMovementKey("KEY_LALT", "PRESS"); dispatchMovementKey("KEY_LALT", "RELEASE");
        }, 5);
        add("hud-movement-toggle-visible", () -> {}, () -> screen().isCameraControlActive(), () -> {
            check("hud-toggle-shows-native-hud", !com.salts_inventory_update.client.WindowedInventoryClient.shouldHideHotbar());
            capture("hud-movement-active");
            call(mc.mouseHandler, "onScroll", nativeWindowHandle(), 0D, -1D);
            check("hud-movement-wheel-selects", hudSelected(mc.player.getInventory()) == 1);
            // Put the grabbed cursor over a real inventory slot: this must select, rather than swap.
            var point = slotPoint(0, 9, windowKind("INVENTORY")); hudPointerAt(point[0], point[1]);
            check("hud-grabbed-cursor-over-slot", call(screen(), "hoveredSlotForKeyboardInput") != null);
            dispatchMovementKey("KEY_3", "PRESS"); dispatchMovementKey("KEY_3", "RELEASE");
            check("hud-movement-number-selects", hudSelected(mc.player.getInventory()) == 2);
        }, 5);
        add("hud-movement-authoritative", () -> {}, () -> true, () -> {
            hudServerSelected("movement-selection", 2);
            check("hud-movement-keeps-window-slot", mc.player.getInventory().getItem(9).is(Items.SAND));
            check("hud-movement-keeps-hotbar-stack", mc.player.getInventory().getItem(2).is(Items.DIRT));
            dispatchMovementKey("KEY_LALT", "PRESS"); dispatchMovementKey("KEY_LALT", "RELEASE");
        }, 5);
        add("hud-movement-release-locks", () -> {}, () -> !screen().isCameraControlActive(), () -> {
            check("hud-toggle-off-hides-native-hud", com.salts_inventory_update.client.WindowedInventoryClient.shouldHideHotbar());
            hudPointerOutside();
            call(mc.mouseHandler, "onScroll", nativeWindowHandle(), 0D, -1D);
            dispatchMovementKey("KEY_9", "PRESS"); dispatchMovementKey("KEY_9", "RELEASE");
            check("hud-return-keeps-new-selection", hudSelected(mc.player.getInventory()) == 2);
            capture("hud-movement-inactive");
            setMovementBinding("KEY_P");
            dispatchMovementKey("KEY_LALT", "PRESS"); dispatchMovementKey("KEY_LALT", "RELEASE");
        }, 5);
        add("hud-movement-old-binding-ignored", () -> {}, () -> true, () -> {
            check("hud-old-alt-keeps-hidden", !screen().isCameraControlActive()
                && com.salts_inventory_update.client.WindowedInventoryClient.shouldHideHotbar());
            dispatchMovementKey("KEY_P", "PRESS"); dispatchMovementKey("KEY_P", "RELEASE");
        }, 4);
        add("hud-movement-rebound-visible", () -> {}, () -> screen().isCameraControlActive(), () -> {
            check("hud-rebound-p-shows-hud", !com.salts_inventory_update.client.WindowedInventoryClient.shouldHideHotbar());
            hudPointerOutside();
            dispatchMovementKey("KEY_7", "PRESS"); dispatchMovementKey("KEY_7", "RELEASE");
            check("hud-rebound-number-selects", hudSelected(mc.player.getInventory()) == 6);
            capture("hud-movement-rebound");
        }, 4);
        add("hud-movement-mode-change", () -> SaltsInventoryConfig.update(config -> config.toggleMovementControl = false),
            () -> !screen().isCameraControlActive(), () -> {
                check("hud-hold-mode-without-held-key-hides", com.salts_inventory_update.client.WindowedInventoryClient.shouldHideHotbar());
                hudPointerOutside();
                dispatchMovementKey("KEY_1", "PRESS"); dispatchMovementKey("KEY_1", "RELEASE");
                check("hud-mode-change-locks-selection", hudSelected(mc.player.getInventory()) == 6);
                hudServerSelected("mode-change-selection", 6);
            }, 4);
        add("hud-movement-option-off", () -> {
            SaltsInventoryConfig.update(config -> config.returnHotbarToInventory = false);
            screen().refreshInventoryWindowLayout(true);
        }, () -> true, () -> {
            hudPointerOutside();
            call(mc.mouseHandler, "onScroll", nativeWindowHandle(), 0D, -1D);
            check("hud-option-off-wheel-selects", hudSelected(mc.player.getInventory()) == 7);
            dispatchMovementKey("KEY_9", "PRESS"); dispatchMovementKey("KEY_9", "RELEASE");
            check("hud-option-off-number-selects", hudSelected(mc.player.getInventory()) == 8);
            hudServerSelected("option-off-selection", 8);
        }, 4);
        add("hud-movement-close-reset", () -> {
            SaltsInventoryConfig.update(config -> { config.returnHotbarToInventory = true; config.toggleMovementControl = true; });
            dispatchMovementKey("KEY_P", "PRESS"); dispatchMovementKey("KEY_P", "RELEASE");
        }, () -> screen().isCameraControlActive(), InWorldGameplayHarness::closeWindows, 3);
        add("hud-movement-reopen-reset", () -> InventoryDesktopScreen.openOrToggleInventory(mc),
            () -> windowKind("INVENTORY") != null && !screen().isCameraControlActive(), () -> {
                check("hud-reopen-toggle-cleared", com.salts_inventory_update.client.WindowedInventoryClient.shouldHideHotbar());
                hudPointerOutside();
                dispatchMovementKey("KEY_1", "PRESS"); dispatchMovementKey("KEY_1", "RELEASE");
                check("hud-reopen-selection-stays-locked", hudSelected(mc.player.getInventory()) == 8);
                capture("hud-movement-reopen");
                setMovementBinding("KEY_LALT");
                SaltsInventoryConfig.update(config -> config.toggleMovementControl = false);
            }, 4);
    }

    private static int hudSelected(Object inventory) {
        try { return ((Number) call(inventory, "getSelectedSlot")).intValue(); }
        catch (IllegalArgumentException legacy) { return ((Number) field(inventory, "selected")).intValue(); }
    }

    private static void hudServerSelected(String label, int expected) {
        addFirst("hud-server-" + label, () -> task = server(() -> hudSelected(player().getInventory())),
            InWorldGameplayHarness::taskDone,
            () -> check("hud-server-" + label, ((Number) task.join()).intValue() == expected), 3);
    }

    private static void hudPointerOutside() {
        hudPointerAt(mc.getWindow().getGuiScaledWidth() - 4, 4);
    }

    private static void hudPointerAt(double x, double y) {
        Object window = mc.getWindow();
        try {
            Field px = mc.mouseHandler.getClass().getDeclaredField("xpos"), py = mc.mouseHandler.getClass().getDeclaredField("ypos");
            px.setAccessible(true); py.setAccessible(true);
            px.setDouble(mc.mouseHandler, x * ((Number) call(window, "getScreenWidth")).doubleValue() / mc.getWindow().getGuiScaledWidth());
            py.setDouble(mc.mouseHandler, y * ((Number) call(window, "getScreenHeight")).doubleValue() / mc.getWindow().getGuiScaledHeight());
        } catch (ReflectiveOperationException failure) { throw new IllegalStateException(failure); }
    }

    private static void addRaisedHudRegression() {
        add("raised-configure", () -> {
            closeWindows();
            SaltsInventoryConfig.update(config -> {
                config.persistentWindows = false; config.hasSeenInstructionsWindow = true;
                config.returnHotbarToInventory = false; config.expandableInventory = false;
            });
            server(() -> { command("gamemode survival"); return null; });
        }, InWorldGameplayHarness::taskDone, () -> {
            check("raised-presence", com.salts_inventory_update.api.integration.DesktopEnvironment.isModLoaded("raised") != RAISED_ABSENT);
        }, 5);
        addContainer("raised-chest", "minecraft:chest", 0); STEPS.removeLast();
        add("raised-player-window", () -> InventoryDesktopScreen.openOrToggleInventory(mc),
            () -> windowKind("INVENTORY") != null, () -> {
                for (var window : screen().workspaceAccess().windows()) {
                    window.handle().moveTo(window.kind() == DesktopWorkspaceWindow.Kind.PLAYER_INVENTORY ? 230 : 8, 8);
                }
                raisedConfig(0, 0, true);
            }, 3);
        add("raised-default", () -> {}, () -> true, () -> {
            verifyHud("default", 0, RAISED_ABSENT ? 0 : -2); capture("raised-default");
        }, 3);
        add("raised-live-groups", () -> raisedConfig(100, 40, true), () -> true, () -> {
            verifyHud("live", RAISED_ABSENT ? 0 : 100, RAISED_ABSENT ? 0 : -42);
            if (!RAISED_ABSENT) {
                double oldX = mc.getWindow().getGuiScaledWidth() / 2 - 80, oldY = mc.getWindow().getGuiScaledHeight() - 11;
                check("old-hud-location-inactive", call(screen(), "detachedHudSlotAt", oldX, oldY) == null);
            }
            capture("raised-live-groups");
        }, 3);
        add("raised-gui-scale", () -> {
            mc.options.guiScale().set(3); resizeHudGui();
        }, () -> mc.getWindow().getGuiScale() == 3, () -> {
            verifyHud("gui-scale", RAISED_ABSENT ? 0 : 100, RAISED_ABSENT ? 0 : -42);
            capture("raised-gui-scale-3");
        }, 3);
        add("raised-gui-scale-restore", () -> {
            mc.options.guiScale().set(2); resizeHudGui();
        }, () -> mc.getWindow().getGuiScale() == 2, () -> {}, 3);
        add("raised-seed", () -> server(() -> {
            player().getInventory().clearContent();
            player().getInventory().setItem(0, new ItemStack(Items.DIRT, 12));
            player().getInventory().setItem(40, new ItemStack(Items.IRON_INGOT, 3));
            ((Container) serverLevel().getBlockEntity(origin)).clearContent();
            return null;
        }), () -> taskDone() && mc.player.getInventory().getItem(0).getCount() == 12, () -> {}, 4);
        add("raised-hotbar-pickup", () -> clickHud(0), () -> carried() == 12,
            () -> check("hotbar-pickup-real-cursor", mc.player.getInventory().getItem(0).isEmpty()), 3);
        add("raised-window-place", () -> clickSlot(0, left()), () -> carried() == 0 && currentSession().menu().getSlot(0).getItem().getCount() == 12,
            () -> { assertContainer("hud-to-window-server", 0, 12, 0); capture("raised-hotbar-to-window"); }, 3);
        add("raised-offhand-pickup", () -> clickHud(9), () -> carried() == 3, () -> {}, 3);
        add("raised-offhand-return", () -> clickHud(9), () -> carried() == 0,
            () -> check("offhand-return", mc.player.getInventory().getItem(40).getCount() == 3), 3);
        add("raised-drag-pickup", () -> clickSlot(0, left()), () -> carried() == 12, () -> {}, 3);
        add("raised-drag-between-hotbar-slots", () -> {
            var a = hudPoint(6); var b = hudPoint(7);
            mouse("mouseClicked", a[0], a[1], left(), 0, 0);
            mouse("mouseDragged", b[0], b[1], left(), b[0] - a[0], b[1] - a[1]);
            mouse("mouseReleased", b[0], b[1], left(), 0, 0);
        }, () -> carried() == 0 && mc.player.getInventory().getItem(6).getCount() == 6
            && mc.player.getInventory().getItem(7).getCount() == 6, () -> {
                var counts = server(() -> List.of(player().getInventory().getItem(6).getCount(), player().getInventory().getItem(7).getCount()));
                addFirst("raised-drag-server", () -> {}, counts::isDone,
                    () -> check("raised-drag-conservation", counts.join().equals(List.of(6, 6))), 3);
                capture("raised-drag-distribution");
            }, 3);
        add("raised-wheel-seed", () -> server(() -> {
            player().getInventory().setItem(0, new ItemStack(Items.DIRT, 4));
            ((Container)serverLevel().getBlockEntity(origin)).setItem(0, new ItemStack(Items.DIRT, 12));
            return null;
        }), () -> taskDone() && mc.player.getInventory().getItem(0).getCount() == 4, () -> {}, 3);
        add("raised-wheel", () -> {
            if (com.salts_inventory_update.api.integration.DesktopEnvironment.isModLoaded("mousetweaks")) {
                var point = hudPoint(0); scroll(point[0], point[1], 2);
            }
        }, () -> !com.salts_inventory_update.api.integration.DesktopEnvironment.isModLoaded("mousetweaks")
            || mc.player.getInventory().getItem(0).getCount() == 6, () -> {
                if (com.salts_inventory_update.api.integration.DesktopEnvironment.isModLoaded("mousetweaks")) {
                    var counts = server(() -> List.of(player().getInventory().getItem(0).getCount(), ((Container)serverLevel().getBlockEntity(origin)).getItem(0).getCount(), player().inventoryMenu.getCarried().getCount()));
                    addFirst("raised-wheel-server", () -> {}, counts::isDone,
                        () -> check("raised-wheel-conservation", counts.join().equals(List.of(6, 10, 0))), 3);
                } else record("raised-wheel", "SKIP", "Mouse Tweaks absent from this focused fixture");
            }, 3);
        add("raised-custom-layout", () -> raisedQaRegistration = DesktopHudApi.register("qa:layout", 100,
            (context, layout) -> layout.mapSlots(slot -> slot.kind() == DesktopHudSlot.Kind.HOTBAR && slot.index() == 0
                ? slot.withTransform(new DesktopTransform(context.viewportWidth() - 50, 60, 1.5))
                : slot)), () -> true, () -> {
            var point = hudPoint(0);
            Object hit = call(screen(), "slotAt", point[0], point[1]);
            check("custom-scale-input", hit != null && ((Slot)call(hit, "slot")).getContainerSlot() == 0);
            capture("raised-custom-layout");
        }, 3);
        add("raised-hidden-slot", () -> {
            raisedQaRegistration.close();
            raisedQaRegistration = DesktopHudApi.register("qa:layout", 100,
                (context, layout) -> layout.mapSlots(slot -> slot.kind() == DesktopHudSlot.Kind.OFFHAND ? slot.withVisibility(false) : slot));
        }, () -> true, () -> {
            var point = hudPoint(9);
            check("hidden-slot-no-input", call(screen(), "detachedHudSlotAt", point[0], point[1]) == null);
        }, 3);
        add("raised-window-hosted", () -> {
            raisedQaRegistration.close(); raisedQaRegistration = null;
            SaltsInventoryConfig.update(config -> config.returnHotbarToInventory = true);
        }, () -> (Boolean)call(screen(), "usesInventoryWindowHotbar"), () -> {
            var point = hudPoint(0);
            check("window-hosted-no-detached-target", call(screen(), "detachedHudSlotAt", point[0], point[1]) == null);
            capture("raised-window-hosted");
        }, 3);
        add("raised-live-zero", () -> {
            SaltsInventoryConfig.update(config -> config.returnHotbarToInventory = false);
            raisedConfig(0, 0, false);
        }, () -> true, () -> verifyHud("zero", 0, 0), 3);
        add("raised-cleanup", () -> {
            if (raisedOriginalConfig != null) {
                try { Files.writeString(((java.io.File)field(ClassFor("dev.yurisuika.raised.config.Config"), "file")).toPath(), raisedOriginalConfig); }
                catch(Exception failure) { throw new IllegalStateException(failure); }
                call(ClassFor("dev.yurisuika.raised.config.Config"), "load");
            }
            closeWindows();
        }, () -> sessions().isEmpty(), () -> check("hud-cleanup-no-session", sessions().isEmpty()), 3);
    }

    private static void addMouseTweaksRegression() {
        add("mt-configure", () -> SaltsInventoryConfig.update(c -> {
            c.hasSeenInstructionsWindow=true;c.expandableInventory=false;c.openUnlocked=true;
        }), () -> true, () -> {
            check("mouse-tweaks-installed-as-requested", com.salts_inventory_update.api.integration.DesktopEnvironment.isModLoaded("mousetweaks") != MT_ABSENT);
            check("compatibility-absent", !com.salts_inventory_update.api.integration.DesktopEnvironment.isModLoaded("salts_inventory_compat"));
        }, 2);
        addContainer("mt-chest", "minecraft:chest", 0); STEPS.removeLast();
        add("mt-api-detection", () -> {}, () -> true, () -> {
            boolean adapted;
            try { adapted=ClassFor("yalter.mousetweaks.api.IMTModGuiContainer3Ex").isInstance(screen()); }
            catch (Throwable absent) { adapted=false; }
            check("optional-screen-adapter", adapted != MT_ABSENT);
        }, 2);
        if (!MT_ABSENT) add("mt-first-event-paused", () -> {
            mtSetScreenField("cameraControl",true);
            try {mtEvent("mouseClicked",0,left());mtEvent("mouseReleased",0,left());}
            finally {mtSetScreenField("cameraControl",false);}
        }, () -> true, () -> check("camera-mode-input-resumes", !(Boolean)call(screen(),"mouseTweaksDisabled")),3);
        mtSeed("native", 32,16,0,0);
        if (MT_ABSENT) {
            add("standalone-normal-pickup", () -> clickSlot(0,left()), () -> carried()==32,
                () -> mtAuthoritative("standalone-normal-pickup",0,16,0,32), 5);
            add("standalone-normal-return", () -> clickSlot(0,left()), () -> carried()==0,
                () -> mtAuthoritative("standalone-normal-return",32,16,0,0), 5);
            add("standalone-shift-transfer", () -> {
                mtShift=true;
                try {clickSlot(0,left());} finally {mtShift=false;}
            }, () -> currentSession().menu().getSlot(0).getItem().isEmpty() && DesktopContainerClient.canDispatchOptimisticMutationNow(),
                () -> {
                    mtAuthoritative("standalone-shift-transfer",0,16,32,0);
                    var hotbar=server(() -> java.util.stream.IntStream.range(0,9).map(i -> player().getInventory().getItem(i).getCount()).sum());
                    addFirst("standalone-core-hotbar-target",()->{},hotbar::isDone,()->check("standalone-shift-keeps-core-hotbar-destination",hotbar.join()==32),3);
                },5);
            add("standalone-cleanup", InWorldGameplayHarness::closeWindows, () -> sessions().isEmpty(), () -> {}, 3);
            return;
        }
        add("mt-lmb-collect", () -> {
            mtEvent("mouseClicked",0,left()); mtEvent("mouseDragged",1,left()); mtEvent("mouseReleased",1,left());
        }, () -> carried()==48 && currentSession().menu().getSlot(1).getItem().isEmpty(),
            () -> { mtAuthoritative("native-lmb-collect",0,0,0,48);capture("native-lmb-collect"); }, 5);
        mtSeed("shift-lmb",32,16,0,0);
        add("mt-shift-lmb", () -> {
            record("shift-client-states","INFO","player="+mc.player.inventoryMenu.getStateId()+" chest="+currentSession().menu().getStateId());
            server(() -> {record("shift-server-states","INFO","player="+player().inventoryMenu.getStateId()+" chest="+serverMenu().getStateId());return null;});
            mtShift=true;
            try {mtEvent("mouseClicked",0,left());mtEvent("mouseDragged",1,left());mtEvent("mouseReleased",1,left());}
            finally {mtShift=false;}
        }, () -> currentSession().menu().getSlot(0).getItem().isEmpty() && currentSession().menu().getSlot(1).getItem().isEmpty(),
            () -> mtAuthoritative("native-shift-lmb",0,0,48,0),5);
        mtSeed("rmb",0,0,0,12);
        add("mt-rmb-revisit", () -> {
            mtEvent("mouseClicked",0,right());mtEvent("mouseDragged",1,right());mtEvent("mouseDragged",0,right());mtEvent("mouseReleased",0,right());
        }, () -> carried()==9 && currentSession().menu().getSlot(0).getItem().getCount()==2,
            () -> mtAuthoritative("native-rmb-revisit",2,1,0,9), 5);
        mtSeed("rmb-last-item",0,5,0,1);
        add("mt-rmb-last-item", () -> {
            mtEvent("mouseClicked",0,right());mtEvent("mouseDragged",1,right());mtEvent("mouseReleased",1,right());
        }, () -> true, () -> mtAuthoritative("rmb-stops-on-empty-cursor",1,5,0,0),6);
        mtSeed("rmb-disabled",0,0,0,12);
        add("mt-rmb-disabled", () -> {
            mtConfig("rmbTweak",false);
            mtEvent("mouseClicked",0,right());mtEvent("mouseDragged",1,right());mtEvent("mouseReleased",1,right());
        }, () -> carried()==10, () -> {mtAuthoritative("native-rmb-disabled",1,1,0,10);mtConfig("rmbTweak",true);},5);
        mtSeed("lmb-disabled",32,16,0,0);
        add("mt-lmb-disabled", () -> {
            mtConfig("lmbTweakWithItem",false);
            mtEvent("mouseClicked",0,left());mtEvent("mouseDragged",1,left());mtEvent("mouseReleased",1,left());
        }, () -> carried()==32, () -> {mtAuthoritative("native-lmb-disabled",0,16,0,32);mtConfig("lmbTweakWithItem",true);},5);
        mtSeed("wheel",32,0,0,0);
        add("mt-popup-blocks-transfer", () -> {
            mtSetScreenField("popupWindow",activeWindow);
            var p=slotPoint(0);scroll(p[0],p[1],-3);
        }, () -> true, () -> {
            mtSetScreenField("popupWindow",null);mtAuthoritative("popup-keeps-inventory",32,0,0,0);
        },5);
        add("mt-wheel-push", () -> { var p=slotPoint(0);
            call(screen(),"mouseTweaksSlotAt",p[0],p[1]);
            var slots=(List<Slot>)call(screen(),"mouseTweaksSlots");
            Slot emptyPlayer=slots.stream().filter(slot -> slot.container==mc.player.getInventory() && slot.getItem().isEmpty()).findFirst().orElseThrow();
            Slot raw=((Slot)call(field(emptyPlayer,"hit"),"slot"));
            record("wheel-settings","INFO","scaling="+field(field(ClassFor("yalter.mousetweaks.Main"),"config"),"scrollItemScaling")+" slots="+slots.size()+" emptyRawCapacity="+raw.getMaxStackSize(ItemStack.EMPTY)+" adapterCapacity="+emptyPlayer.getMaxStackSize(ItemStack.EMPTY));
            scroll(p[0],p[1],-3);
            record("wheel-local-state","INFO","cursor="+carried()+" pending="+call(DesktopContainerClient.class,"hasPendingSlotMutations"));
        },
            () -> currentSession().menu().getSlot(0).getItem().getCount()==29 && carried()==0,
            () -> mtAuthoritative("native-wheel-push-closed-player",29,0,3,0), 5);
        add("mt-wheel-pull", () -> { var p=slotPoint(0);scroll(p[0],p[1],2); },
            () -> currentSession().menu().getSlot(0).getItem().getCount()==31 && carried()==0,
            () -> mtAuthoritative("native-wheel-pull",31,0,1,0), 5);
        add("mt-wheel-fractional", () -> { var p=slotPoint(0);scroll(p[0],p[1],-.4);scroll(p[0],p[1],-.6); },
            () -> currentSession().menu().getSlot(0).getItem().getCount()==30 && carried()==0,
            () -> mtAuthoritative("native-wheel-fractional",30,0,2,0), 5);
        add("mt-wheel-disabled", () -> {
            try { field(ClassFor("yalter.mousetweaks.Main"),"config").getClass().getField("wheelTweak").setBoolean(field(ClassFor("yalter.mousetweaks.Main"),"config"),false); }
            catch(ReflectiveOperationException e){throw new IllegalStateException(e);}
            var p=slotPoint(0);scroll(p[0],p[1],-2);
        }, () -> true, () -> {
            mtAuthoritative("upstream-wheel-config-respected",30,0,2,0);
            try { field(ClassFor("yalter.mousetweaks.Main"),"config").getClass().getField("wheelTweak").setBoolean(field(ClassFor("yalter.mousetweaks.Main"),"config"),true); }
            catch(ReflectiveOperationException e){throw new IllegalStateException(e);}
        }, 5);
        add("mt-close-native", InWorldGameplayHarness::closeWindows, () -> sessions().isEmpty(), () -> {},3);
        add("mt-register-api-window", InWorldGameplayHarness::mtRegisterWindow, () -> true, () -> {},3);
        addContainer("mt-api-chest", "minecraft:chest",0);STEPS.removeLast();
        mtSeed("api",32,16,0,0);
        add("mt-api-geometry", () -> {}, () -> true, () -> {
            var p=slotPoint(0); Object proxy=call(screen(),"mouseTweaksSlotAt",p[0],p[1]);
            check("api-window-custom-layout", field(activeWindow,"apiDefinition")!=null && proxy instanceof Slot && ((Slot)proxy).x==(int)p[0]-8);
            check("api-window-slots-enumerated", ((List<?>)call(screen(),"mouseTweaksSlots")).size()>=27);
            capture("api-custom-layout");
        },4);
        add("mt-api-lmb", () -> {
            mtEvent("mouseClicked",0,left());mtEvent("mouseDragged",1,left());mtEvent("mouseReleased",1,left());
        }, () -> carried()==48, () -> mtAuthoritative("api-lmb-collect",0,0,0,48),5);
        mtSeed("api-wheel",32,0,0,0);
        add("mt-api-wheel", () -> {var p=slotPoint(0);scroll(p[0],p[1],-2);},
            () -> currentSession().menu().getSlot(0).getItem().getCount()==30 && carried()==0,
            () -> {mtAuthoritative("api-wheel",30,0,2,0);capture("api-wheel-transfer");},5);
        mtSeed("api-virtual",0,0,0,12);
        add("mt-api-virtual-ignore", () -> {
            var p=slotPoint(2);Object slot=call(screen(),"mouseTweaksSlotAt",p[0],p[1]);
            check("api-virtual-slot-ignored",(Boolean)call(screen(),"mouseTweaksIgnored",slot));
            mtEvent("mouseClicked",0,right());mtEvent("mouseDragged",1,right());mtEvent("mouseDragged",2,right());mtEvent("mouseReleased",2,right());
        }, () -> carried()==10, () -> mtAuthoritative("api-virtual-excluded",1,1,0,10),5);
        add("mt-close-api", InWorldGameplayHarness::closeWindows, () -> sessions().isEmpty(), () -> {},3);
        addContainer("mt-crafting", "minecraft:crafting_table",0);STEPS.removeLast();
        add("mt-craft-seed", () -> server(() -> {
            player().getInventory().clearContent();player().inventoryMenu.setCarried(ItemStack.EMPTY);
            player().getInventory().setItem(9,new ItemStack(Items.STONE,20));
            var menu=serverMenu();for(int i:List.of(1,2,4,5))menu.getSlot(i).set(new ItemStack(Items.OAK_PLANKS,1));
            menu.broadcastFullState();mtSync();return null;
        }), () -> taskDone() && currentSession().menu().getSlot(0).getItem().is(Items.CRAFTING_TABLE), () -> {},5);
        add("mt-craft-wheel", () -> {
            var p=slotPoint(0); Object proxy=call(screen(),"mouseTweaksSlotAt",p[0],p[1]);
            record("mt-craft-diagnostics","INFO","proxy="+proxy+" ignored="+(proxy==null?"null":call(screen(),"mouseTweaksIgnored",proxy))+" output="+(proxy==null?"null":call(screen(),"mouseTweaksCraftingOutput",proxy))+" slots="+((List<?>)call(screen(),"mouseTweaksSlots")).size()+" disabled="+call(screen(),"mouseTweaksDisabled"));
            scroll(p[0],p[1],-3);
            record("mt-craft-after-scroll","INFO","result="+currentSession().menu().getSlot(0).getItem()+" pending="+call(DesktopContainerClient.class,"hasPendingSlotMutations"));
        },
            () -> true,
            () -> {
                var actual=server(() -> {
                    var menu=serverMenu();return List.of(1,2,4,5).stream().allMatch(i -> menu.getSlot(i).getItem().isEmpty())
                        && mtCount(player().getInventory(),Items.CRAFTING_TABLE)==1
                        && mtCount(player().getInventory(),Items.STONE)==20 && player().inventoryMenu.getCarried().isEmpty();
                });
                addFirst("mt-craft-authoritative",()->{},actual::isDone,()->check("craft-output-consumed-once",actual.join()),3);
                capture("native-crafting-wheel");
            },6);
        if(VersionInfo.MINECRAFT_VERSION.equals("1.20.1")) add("mt-batch-authorization", () -> server(() -> {
            Object sessions=call(ClassFor("com.salts_inventory_update.server.DesktopContainerSessions"),"sessions",player());
            long token=((Number)field(sessions,"playerMenuToken")).longValue();
            int state=player().inventoryMenu.getStateId();
            Class<?> ref=ClassFor("com.salts_inventory_update.network.DesktopPackets$DesktopSlotReference");
            Class<?> action=ClassFor("com.salts_inventory_update.network.DesktopPackets$DesktopClickAction");
            Object valid=construct(action,construct(ref,0,token,state,9),0,false);
            Object invalid=construct(action,construct(ref,0,token^12345L,state,10),0,false);
            Object sequence=construct(ClassFor("com.salts_inventory_update.network.DesktopPackets$DesktopClickSequencePayload"),((Number)field(sessions,"lastClickSequence")).longValue()+1,List.of(valid,invalid));
            call(ClassFor("com.salts_inventory_update.server.DesktopContainerSessions"),"clickSequence",player(),sequence);
            return player().inventoryMenu.getCarried().isEmpty() && mtCount(player().getInventory(),Items.CRAFTING_TABLE)==1;
        }), InWorldGameplayHarness::taskDone, () -> check("batch-invalid-source-rejected-before-first-click",(Boolean)task.join()),5);
        add("mt-final-cleanup", InWorldGameplayHarness::closeWindows, () -> sessions().isEmpty(), () -> {},3);
    }
    private static void mtSetScreenField(String name,Object value) {
        try {var field=screen().getClass().getDeclaredField(name);field.setAccessible(true);field.set(screen(),value);}
        catch(ReflectiveOperationException failure){throw new IllegalStateException(failure);}
    }
    private static void mtSync() {
        // Fixture writes happen outside Core's packet handlers; flush the final player revision too.
        Object sessions=call(ClassFor("com.salts_inventory_update.server.DesktopContainerSessions"),"sessions",player());
        call(sessions,"broadcastAll",player());
        call(ClassFor("com.salts_inventory_update.server.DesktopContainerSessions"),"syncPlayerMenu",player());
        call(ClassFor("com.salts_inventory_update.server.DesktopContainerSessions"),"syncCarried",player(),sessions);
    }
    private static void mtConfig(String name,boolean enabled) {
        try {Object config=field(ClassFor("yalter.mousetweaks.Main"),"config");config.getClass().getField(name).setBoolean(config,enabled);}
        catch(ReflectiveOperationException error){throw new IllegalStateException(error);}
    }
    private static int mtCount(Container c,net.minecraft.world.item.Item item) {
        int n=0;for(int i=0;i<c.getContainerSize();i++)if(c.getItem(i).is(item))n+=c.getItem(i).getCount();return n;
    }
    private static void mtSeed(String name,int a,int b,int storage,int cursor) {
        add("mt-seed-"+name, () -> server(() -> {
            var menu=serverMenu();for(int i=0;i<27;i++)menu.getSlot(i).set(ItemStack.EMPTY);
            player().getInventory().clearContent();
            menu.getSlot(0).set(new ItemStack(Items.STONE,a));menu.getSlot(1).set(new ItemStack(Items.STONE,b));
            if(storage>0)player().getInventory().setItem(9,new ItemStack(Items.STONE,storage));
            player().inventoryMenu.setCarried(cursor==0?ItemStack.EMPTY:new ItemStack(Items.STONE,cursor));
            menu.setCarried(player().inventoryMenu.getCarried());menu.broadcastFullState();mtSync();return null;
        }), () -> taskDone() && carried()==cursor && currentSession().menu().getSlot(0).getItem().getCount()==a
            && currentSession().menu().getSlot(1).getItem().getCount()==b && DesktopContainerClient.canDispatchOptimisticMutationNow(), () -> {},6);
    }
    private static void mtAuthoritative(String name,int a,int b,int storage,int cursor) {
        var actual=server(() -> List.of(serverMenu().getSlot(0).getItem().getCount(),serverMenu().getSlot(1).getItem().getCount(),
            mtCount(player().getInventory(),Items.STONE),player().inventoryMenu.getCarried().getCount()));
        addFirst(name+"-server",()->{},actual::isDone,()->record(name+"-server-conservation",actual.join().equals(List.of(a,b,storage,cursor))?"PASS":"FAIL","actual="+actual.join()+" expected="+List.of(a,b,storage,cursor)),3);
    }
    private static void mtEvent(String event,int slot,int nativeButton) {
        var p=slotPoint(slot);call(screen(),"mouseMoved",p[0],p[1]);
        Object mtButton=enumValue("yalter.mousetweaks.MouseButton",nativeButton==right()?"RIGHT":"LEFT");
        String method=switch(event){case "mouseClicked"->"onMouseClicked";case "mouseReleased"->"onMouseReleased";default->"onMouseDrag";};
        if(!(Boolean)call(ClassFor("yalter.mousetweaks.Main"),method,screen(),p[0],p[1],mtButton))mouse(event,p[0],p[1],nativeButton,1,0);
    }
    private static void mtRegisterWindow() {
        Class<?> definition=ClassFor("com.salts_inventory_update.api.client.desktop.DesktopWindowDefinition");
        Object instance=Proxy.newProxyInstance(definition.getClassLoader(),new Class<?>[]{definition},(proxy,method,args)->{
            if(method.getName().equals("slotAt")) {
                Object context=args[0];double x=(Double)args[1],y=(Double)args[2];
                for(int i=0;i<27;i++) {
                    Object hit=call(context,"menuSlotHit",i,((Number)call(context,"contentX")).intValue()+12+i%9*18,
                        ((Number)call(context,"contentY")).intValue()+14+i/9*18,x,y);
                    if(hit!=null)return hit;
                }return null;
            }
            if(method.getName().equals("render")) {
                Object c=args[0];for(int i=0;i<27;i++)call(c,"slot",i,((Number)call(c,"contentX")).intValue()+12+i%9*18,((Number)call(c,"contentY")).intValue()+14+i/9*18);
                return null;
            }
            if(method.getName().equals("defaultSize") || method.getName().equals("minSize"))return call(ClassFor("com.salts_inventory_update.api.client.desktop.DesktopWindowSize"),"of",200,115);
            if(method.getName().equals("title"))return net.minecraft.network.chat.Component.literal("API Mouse Tweaks QA");
            if(method.isDefault())return InvocationHandler.invokeDefault(proxy,method,args);
            if(method.getName().equals("toString"))return "MouseTweaksPublicApiFixture";
            throw new IllegalStateException(method.toString());
        });
        Object menuType=field(ClassFor("net.minecraft.world.inventory.MenuType"),"GENERIC_9x3");
        call(ClassFor("com.salts_inventory_update.api.client.desktop.DesktopClientApi"),"replaceWindow",menuType,instance);
        Class<?> access=ClassFor("com.salts_inventory_update.api.desktop.DesktopMenuBindings$SlotAccess");
        Object bindings=Proxy.newProxyInstance(access.getClassLoader(),new Class<?>[]{access},(proxy,method,args)->{
            if(method.getName().equals("size"))return ((AbstractContainerMenu)args[0]).slots.size();
            if(method.getName().equals("isVirtualSlot"))return ((AbstractContainerMenu)args[0]).getSlot(2)==args[1];
            if(method.isDefault())return InvocationHandler.invokeDefault(proxy,method,args);
            if(method.getName().equals("toString"))return "MouseTweaksVirtualFixture";
            throw new IllegalStateException(method.toString());
        });
        call(ClassFor("com.salts_inventory_update.api.desktop.DesktopMenuBindings"),"register",menuType,bindings,false);
    }

    private static void addWindowControlsRegression() {
        add("window-controls-configure", () -> {
            closeWindows();
            SaltsInventoryConfig.update(config -> {
                config.persistentWindows = false;
                config.hasSeenInstructionsWindow = true;
                config.toggleMovementControl = false;
            });
        }, () -> true, () -> {}, 2);
        for (String kind : List.of("character", "crafting_table", "furnace")) {
            for (int attempt = 0; attempt < 2; attempt++) {
                String label = kind + (attempt == 0 ? "-first" : "-reopened");
                if (kind.equals("character")) {
                    add(label + "-open", () -> { closeWindows(); InventoryDesktopScreen.openOrToggleCharacter(mc); },
                        () -> windowKind("CHARACTER") != null, () -> activeWindow = windowKind("CHARACTER"), 4);
                } else {
                    String fixture = "recipe-" + kind;
                    addContainer(fixture, "minecraft:" + kind, 0);
                    // Retain only native opening, without crafting/smelting probes or auto-close.
                    if (!STEPS.removeLast().name().equals("close-" + fixture))
                        throw new IllegalStateException("Expected container fixture close step");
                }
                add(label + "-recipe-click", () -> {
                    if (!kind.equals("character")) activeWindow = windowSession(currentSession().sessionId());
                    check(label + "-book-initially-not-displayed", call(screen(), "visibleRecipeBook", activeWindow) == null);
                    Object rect = call(screen(), "recipeBookButtonRect", activeWindow);
                    clickAt(((Number) call(rect, "x")).doubleValue() + 6, ((Number) call(rect, "y")).doubleValue() + 6);
                }, () -> true, () -> {
                    check(label + "-first-click-opens-book", call(screen(), "visibleRecipeBook", activeWindow) != null);
                    Object rect = call(screen(), "recipeBookButtonRect", activeWindow);
                    double x = ((Number) call(rect, "x")).doubleValue() + 6;
                    double y = ((Number) call(rect, "y")).doubleValue() + 6;
                    clickAt(x, y);
                    check(label + "-second-click-closes-book", call(screen(), "visibleRecipeBook", activeWindow) == null);
                    clickAt(x, y);
                    check(label + "-third-click-opens-book", call(screen(), "visibleRecipeBook", activeWindow) != null);
                    if (label.endsWith("reopened")) capture(label + "-recipe-book");
                }, 3);
                // Close while the native player's recipe-book preference is still open.
                add(label + "-close-open-book", InWorldGameplayHarness::closeWindows, () -> nativeScreen() == null, () -> {}, 3);
            }
        }
        add("movement-controls-native-menu", () -> {
            String pkg = VersionInfo.MINECRAFT_VERSION.equals("1.20.1") ? "controls" : "options.controls";
            setNativeScreen(construct(ClassFor("net.minecraft.client.gui.screens." + pkg + ".ControlsScreen"), null, mc.options));
        }, () -> nativeScreen() != null, () -> {
            Object mode = findWidget(nativeScreen(), "Window Movement: Hold");
            check("controls-hold-default", mode != null);
            capture("minecraft-controls-hold");
            pressUiButton(mode);
            check("controls-saves-toggle", SaltsInventoryConfig.get().toggleMovementControl);
            SaltsInventoryConfig.reload();
            check("controls-toggle-survives-reload", SaltsInventoryConfig.get().toggleMovementControl);
        }, 4);
        add("movement-controls-mod-settings", () -> setNativeScreen(
            com.salts_inventory_update.client.WindowedInventoryClient.createConfigScreen(null)), () -> true, () -> {
            Object mode = configControl("movement_control_mode");
            check("config-reflects-controls-toggle", call(call(mode, "getMessage"), "getString").equals("Toggle"));
            capture("mod-settings-toggle");
            pressUiButton(mode);
            check("config-saves-hold", !SaltsInventoryConfig.get().toggleMovementControl);
            pressUiButton(configControl("movement_control_key"));
            check("config-opens-native-keybinds", nativeScreen().getClass().getSimpleName().equals("KeyBindsScreen"));
        }, 4);
        add("movement-controls-menu-sync", () -> {
            String pkg = VersionInfo.MINECRAFT_VERSION.equals("1.20.1") ? "controls" : "options.controls";
            setNativeScreen(construct(ClassFor("net.minecraft.client.gui.screens." + pkg + ".ControlsScreen"), null, mc.options));
        }, () -> true, () -> {
            check("controls-reflects-config-hold", findWidget(nativeScreen(), "Window Movement: Hold") != null);
            check("controls-removes-extra-key-button", findWidget(nativeScreen(), "Window Movement Key") == null);
            pressUiButton(findWidget(nativeScreen(), net.minecraft.network.chat.Component.translatable("controls.keybinds").getString()));
            check("controls-opens-native-keybinds", nativeScreen().getClass().getSimpleName().equals("KeyBindsScreen"));
        }, 3);
        add("movement-toggle-workspace", () -> {
            setNativeScreen(null);
            SaltsInventoryConfig.update(config -> config.toggleMovementControl = true);
            // A registered but inactive companion must leave regular window controls available.
            com.salts_inventory_update.api.client.desktop.DesktopClientApi.setWorkspaceHost(
                new com.salts_inventory_update.api.client.desktop.DesktopWorkspaceHost() {
                    public boolean enabled() { return false; }
                    public void opened(com.salts_inventory_update.api.client.desktop.DesktopWorkspace workspace) { }
                });
            InventoryDesktopScreen.openOrToggleCharacter(mc);
        }, () -> nativeScreen() instanceof InventoryDesktopScreen, () -> {
            check("movement-binding-default-alt", call(com.salts_inventory_update.client.WindowedInventoryClient.mouseFocusKey(), "isDefault").equals(true));
            dispatchMovementKey("KEY_LALT", "PRESS");
            dispatchMovementKey("KEY_LALT", "RELEASE");
        }, 3);
        add("movement-toggle-latches", () -> {}, () -> true, () -> {
            check("short-alt-tap-latches", screen().isCameraControlActive());
            dispatchMovementKey("KEY_LALT", "REPEAT");
        }, 3);
        add("movement-repeat-ignored", () -> {}, () -> true, () -> {
            check("repeat-does-not-toggle-off", screen().isCameraControlActive());
            dispatchMovementKey("KEY_LALT", "PRESS");
            dispatchMovementKey("KEY_LALT", "RELEASE");
        }, 3);
        add("movement-second-press", () -> {}, () -> true, () -> {
            check("second-tap-restores-cursor", !screen().isCameraControlActive());
            setMovementBinding("KEY_P");
            dispatchMovementKey("KEY_LALT", "PRESS");
            dispatchMovementKey("KEY_LALT", "RELEASE");
        }, 3);
        add("movement-old-key-ignored", () -> {}, () -> true, () -> {
            check("alt-ignored-after-rebind", !screen().isCameraControlActive());
            dispatchMovementKey("KEY_P", "PRESS");
            dispatchMovementKey("KEY_P", "RELEASE");
        }, 3);
        add("movement-rebound-key", () -> {}, () -> true, () -> {
            check("rebound-key-latches", screen().isCameraControlActive());
            capture("rebound-movement-control-active");
            closeWindows();
        }, 3);
        add("movement-close-resets", () -> InventoryDesktopScreen.openOrToggleCharacter(mc), () -> nativeScreen() instanceof InventoryDesktopScreen,
            () -> check("closed-workspace-does-not-retain-toggle", !screen().isCameraControlActive()), 4);
        add("movement-mouse-binding", () -> {
            Object binding = com.salts_inventory_update.client.WindowedInventoryClient.mouseFocusKey();
            call(binding, "setKey", call(enumValue("com.mojang.blaze3d.platform.InputConstants$Type", "MOUSE"), "getOrCreate", right()));
            call(ClassFor("net.minecraft.client.KeyMapping"), "resetMapping");
            dispatchMovementMouse("PRESS"); dispatchMovementMouse("RELEASE");
        }, () -> true, () -> check("mouse-binding-latches", screen().isCameraControlActive()), 4);
        add("movement-menu-clears-toggle", () -> setNativeScreen(
                com.salts_inventory_update.client.WindowedInventoryClient.createConfigScreen(null)),
            () -> true, () -> {}, 3);
        add("movement-menu-return", () -> {
            setNativeScreen(screen());
        }, () -> nativeScreen() instanceof InventoryDesktopScreen,
            () -> check("menu-return-resets-toggle", !screen().isCameraControlActive()), 4);
        add("movement-controls-cleanup", () -> {
            closeWindows();
            com.salts_inventory_update.api.client.desktop.DesktopClientApi.setWorkspaceHost(null);
            Object binding = com.salts_inventory_update.client.WindowedInventoryClient.mouseFocusKey();
            call(binding, "setKey", call(binding, "getDefaultKey"));
            call(ClassFor("net.minecraft.client.KeyMapping"), "resetMapping");
            SaltsInventoryConfig.update(config -> config.toggleMovementControl = false);
        }, () -> true, () -> {}, 2);
    }

    private static void addKeybindsRegression() {
        add("keybinds-configure", () -> {
            closeWindows();
            SaltsInventoryConfig.update(config -> {
                config.persistentWindows = false;
                config.toggleMovementControl = false;
                config.eHoldCloseAllSeconds = 0.5D;
                config.hasSeenInstructionsWindow = true;
            });
            String pkg = VersionInfo.MINECRAFT_VERSION.equals("1.20.1") ? "controls" : "options.controls";
            setNativeScreen(construct(ClassFor("net.minecraft.client.gui.screens." + pkg + ".ControlsScreen"), null, mc.options));
        }, () -> nativeScreen() != null, () -> {
            check("controls-keeps-mode-selector", findWidget(nativeScreen(), "Window Movement: Hold") != null);
            check("controls-no-extra-key-button", findWidget(nativeScreen(), "Window Movement Key") == null);
            capture("controls-mode-selector-only");
            pressUiButton(findWidget(nativeScreen(), "Window Movement: Hold"));
            check("controls-selector-still-saves-mode", SaltsInventoryConfig.get().toggleMovementControl);
            pressUiButton(findWidget(nativeScreen(), net.minecraft.network.chat.Component.translatable("controls.keybinds").getString()));
        }, 4);
        add("native-keybinds-section", () -> {}, () -> nativeScreen().getClass().getSimpleName().equals("KeyBindsScreen"), () -> {
            Object category = call(com.salts_inventory_update.client.WindowedInventoryClient.mouseFocusKey(), "getCategory");
            for (Object binding : List.of(
                com.salts_inventory_update.client.WindowedInventoryClient.characterWindowKey(),
                com.salts_inventory_update.client.WindowedInventoryClient.jeiWindowKey(),
                com.salts_inventory_update.client.WindowedInventoryClient.mouseFocusKey())) {
                check("mod-binding-category-" + call(binding, "getName"), category.equals(call(binding, "getCategory")));
                check("native-binding-registered-" + call(binding, "getName"),
                    Arrays.asList((Object[]) field(mc.options, "keyMappings")).contains(binding));
            }
            check("vanilla-inventory-keeps-its-own-category", !category.equals(call(mc.options.keyInventory, "getCategory")));
            Object list = field(nativeScreen(), "keyBindsList");
            Object section = null;
            for (Object entry : (List<?>) call(list, "children")) {
                if (!entry.getClass().getSimpleName().equals("CategoryEntry")) continue;
                Object label;
                try { label = field(entry, "name"); }
                catch (IllegalArgumentException modern) { label = call(field(entry, "categoryName"), "getMessage"); }
                if (call(label, "getString").equals("Salt's Inventory Update")) { section = entry; break; }
            }
            check("native-keybinds-salt-section-present", section != null);
            call(list, "setScrollAmount", 100000D);
        }, 4);
        add("native-keybinds-section-capture", () -> {}, () -> true,
            () -> capture("salts-inventory-keybinds-section"), 4);
        add("rebind-vanilla-inventory-keyboard", () -> {
            setNativeScreen(null);
            int key = ((Number) field(ClassFor("com.mojang.blaze3d.platform.InputConstants"), "KEY_I")).intValue();
            String type = VersionInfo.MINECRAFT_VERSION.equals("26.3") ? "KEYBOARD" : "KEYSYM";
            call(mc.options.keyInventory, "setKey", call(enumValue("com.mojang.blaze3d.platform.InputConstants$Type", type), "getOrCreate", key));
            call(ClassFor("net.minecraft.client.KeyMapping"), "resetMapping");
            InventoryDesktopScreen.openOrToggleCharacter(mc);
        }, () -> nativeScreen() instanceof InventoryDesktopScreen, () -> {
            check("inventory-binding-reads-rebound-key", com.salts_inventory_update.client.WindowedInventoryClient.inventoryKeyName().equals("I"));
            checkInventoryHoldHelpBinding();
            dispatchMovementKey("KEY_E", "PRESS");
        }, 3);
        add("old-inventory-key-no-longer-closes", () -> {}, () -> true, () -> {
            check("old-e-does-not-close", nativeScreen() instanceof InventoryDesktopScreen && screen().hasWindows());
            dispatchMovementKey("KEY_E", "RELEASE");
            dispatchMovementKey("KEY_I", "PRESS");
        }, 5);
        add("rebound-inventory-key-hold", () -> {}, () -> nativeScreen() == null, () -> {
            check("rebound-i-hold-closes-all", !screen().hasWindows());
            dispatchMovementKey("KEY_I", "RELEASE");
        }, 2);
        add("rebound-key-release-stays-closed", () -> {}, () -> true,
            () -> check("rebound-i-release-does-not-reopen", nativeScreen() == null), 2);
        add("rebind-vanilla-inventory-mouse", () -> {
            call(mc.options.keyInventory, "setKey", call(enumValue("com.mojang.blaze3d.platform.InputConstants$Type", "MOUSE"), "getOrCreate", right()));
            call(ClassFor("net.minecraft.client.KeyMapping"), "resetMapping");
            InventoryDesktopScreen.openOrToggleCharacter(mc);
        }, () -> nativeScreen() instanceof InventoryDesktopScreen, () -> {
            checkInventoryHoldHelpBinding();
            dispatchMovementMouse("PRESS");
        }, 3);
        add("rebound-inventory-mouse-hold", () -> {}, () -> nativeScreen() == null, () -> {
            check("rebound-mouse-hold-closes-all", !screen().hasWindows());
            dispatchMovementMouse("RELEASE");
        }, 2);
        add("rebound-mouse-release-stays-closed", () -> {}, () -> true,
            () -> check("rebound-mouse-release-does-not-reopen", nativeScreen() == null), 2);
        add("rebound-inventory-mouse-tap", () -> {
            dispatchMovementMouse("PRESS"); dispatchMovementMouse("RELEASE");
        }, () -> nativeScreen() instanceof InventoryDesktopScreen,
            () -> check("rebound-mouse-tap-opens-inventory", windowKind("INVENTORY") != null), 3);
        add("keybinds-cleanup", () -> {
            closeWindows();
            call(mc.options.keyInventory, "setKey", call(mc.options.keyInventory, "getDefaultKey"));
            call(ClassFor("net.minecraft.client.KeyMapping"), "resetMapping");
            SaltsInventoryConfig.update(config -> config.toggleMovementControl = false);
        }, () -> true, () -> {}, 2);
    }

    private static void checkInventoryHoldHelpBinding() {
        Object line = call(ClassFor("com.salts_inventory_update.client.InventoryDesktopScreen$InstructionsLine"), "inventoryHold", "QA");
        check("help-reflects-inventory-binding", call(line, "resolvedBinds").equals(
            List.of("Hold " + com.salts_inventory_update.client.WindowedInventoryClient.inventoryKeyName())));
    }

    private static Object configControl(String id) {
        for (Object row : (List<?>) field(nativeScreen(), "rows"))
            if (field(row, "id").equals(id)) return field(row, "control");
        throw new IllegalArgumentException("Missing config control: " + id);
    }

    private static void pressUiButton(Object button) {
        try { call(button, "onPress"); }
        catch (IllegalArgumentException modern) {
            int space = ((Number) field(ClassFor("com.mojang.blaze3d.platform.InputConstants"), "KEY_SPACE")).intValue();
            call(button, "onPress", construct(ClassFor("net.minecraft.client.input.KeyEvent"), space, 0, 0));
        }
    }

    private static Object findWidget(Object root, String text) {
        try { if (call(call(root, "getMessage"), "getString").equals(text)) return root; }
        catch (IllegalArgumentException noMessage) { }
        try {
            for (Object child : (List<?>) call(root, "children")) {
                Object found = findWidget(child, text);
                if (found != null) return found;
            }
        } catch (IllegalArgumentException noChildren) { }
        return null;
    }

    private static void setMovementBinding(String name) {
        int code = ((Number) field(ClassFor("com.mojang.blaze3d.platform.InputConstants"), name)).intValue();
        String type = VersionInfo.MINECRAFT_VERSION.equals("26.3") ? "KEYBOARD" : "KEYSYM";
        call(com.salts_inventory_update.client.WindowedInventoryClient.mouseFocusKey(), "setKey",
            call(enumValue("com.mojang.blaze3d.platform.InputConstants$Type", type), "getOrCreate", code));
        call(ClassFor("net.minecraft.client.KeyMapping"), "resetMapping");
    }

    private static long nativeWindowHandle() {
        Object window = call(mc, "getWindow");
        try { return ((Number) call(window, "handle")).longValue(); }
        catch (IllegalArgumentException legacy) { return ((Number) call(window, "getWindow")).longValue(); }
    }

    private static int nativeInputAction(String name) {
        try { return ((Number) field(ClassFor("com.mojang.blaze3d.platform.InputConstants"), name)).intValue(); }
        catch (IllegalArgumentException legacy) { return name.equals("PRESS") ? 1 : name.equals("REPEAT") ? 2 : 0; }
    }

    private static void dispatchMovementKey(String name, String action) {
        int key = ((Number) field(ClassFor("com.mojang.blaze3d.platform.InputConstants"), name)).intValue();
        Object keyboard = field(mc, "keyboardHandler");
        try { call(keyboard, "keyPress", nativeWindowHandle(), key, 0, nativeInputAction(action), 0); }
        catch (IllegalArgumentException modern) {
            call(keyboard, "keyPress", nativeWindowHandle(), nativeInputAction(action),
                construct(ClassFor("net.minecraft.client.input.KeyEvent"), key, 0, 0));
        }
    }

    private static void dispatchMovementMouse(String action) {
        try { call(mc.mouseHandler, "onPress", nativeWindowHandle(), right(), nativeInputAction(action), 0); }
        catch (IllegalArgumentException modern) {
            call(mc.mouseHandler, "onButton", nativeWindowHandle(),
                construct(ClassFor("net.minecraft.client.input.MouseButtonInfo"), right(), 0), nativeInputAction(action));
        }
    }

    private static void addChestCloseRegression() {
        add("removed-window-api", () -> {}, () -> true, () -> {
            for (String type : List.of("DesktopWindowContext", "DesktopWindowDefinition", "DesktopWindowState", "DesktopWindowOptions")) {
                Class<?> api = ClassFor("com.salts_inventory_update.api.client.desktop." + type);
                check("removed-api-" + type, Arrays.stream(api.getMethods()).noneMatch(method ->
                    List.of("minimized", "minimizable", "ghosted", "unghosted", "ghostable").contains(method.getName())));
            }
            for (String removed : List.of("enableGhostPins", "minimizableWindows", "ghostWindowOpacity")) {
                check("removed-setting-" + removed, Arrays.stream(SaltsInventoryConfig.ConfigFile.class.getFields())
                    .noneMatch(option -> option.getName().equals(removed)));
            }
        }, 1);
        add("removed-window-settings-screen", () -> setNativeScreen(
                com.salts_inventory_update.client.WindowedInventoryClient.createConfigScreen(null)),
            () -> nativeScreen() instanceof com.salts_inventory_update.client.SaltsInventoryConfigScreen, () -> {
                List<String> ids = ((List<?>) field(nativeScreen(), "rows")).stream()
                    .map(row -> field(row, "id").toString()).toList();
                check("config-keeps-persistent-windows", ids.contains("persistent_windows"));
                check("config-removes-obsolete-controls", Collections.disjoint(ids,
                    List.of("enable_ghost_pins", "minimizable_windows", "ghost_window_opacity")));
                capture("window-settings-without-retired-features");
            }, 5);
        add("removed-window-settings-close", () -> setNativeScreen(null), () -> nativeScreen() == null, () -> {}, 2);
        add("native-opener-balanced-lifecycle", () -> server(InWorldGameplayHarness::nativeOpenerLifecycle),
            InWorldGameplayHarness::taskDone, () -> {
                List<Integer> counts = (List<Integer>) task.join();
                record("native-opener-hide-restore-final-close-balanced", counts.equals(List.of(1, 1, 2, 2, 2, 3, 3, 32))
                    ? "PASS" : "FAIL", counts.toString());
            }, 2);
        for (boolean persistent : List.of(false, true)) {
            for (String gesture : List.of("hold-e", "hold-escape", "tap-escape")) {
                String label = "chest-" + (persistent ? "persistent" : "ordinary") + "-" + gesture;
                add(label + "-configure", () -> {
                    closeWindows();
                    call(ClassFor("com.salts_inventory_update.client.InventoryKeyHoldController"), "reset");
                    SaltsInventoryConfig.update(config -> {
                        config.persistentWindows = persistent;
                        config.eHoldCloseAllSeconds = 0.5D;
                        config.hasSeenInstructionsWindow = true;
                    });
                    server(() -> {
                        command("tp @a " + (origin.getX() + 0.5D) + " " + origin.getY()
                            + " " + (origin.getZ() + 3.5D) + " 180 20");
                        return null;
                    });
                }, InWorldGameplayHarness::taskDone, () -> {}, 5);
                addContainer("chest", "minecraft:chest", 0);
                add(label + "-open-state", () -> server(InWorldGameplayHarness::chestCloseSnapshot),
                    InWorldGameplayHarness::taskDone, () -> {
                        List<Integer> snapshot = (List<Integer>) task.join();
                        record(label + "-native-openers-before", snapshot.get(0) > 0 ? "PASS" : "FAIL", snapshot.toString());
                        record(label + "-native-lid-before", chestLidOpenness() > 0.9F ? "PASS" : "FAIL", "openness=" + chestLidOpenness());
                        capture(label + "-open");
                    }, 20);
                long[] pressedAt = new long[1];
                String key = gesture.equals("hold-e") ? "KEY_E" : "KEY_ESCAPE";
                add(label + "-gesture", () -> {
                    pressedAt[0] = System.currentTimeMillis();
                    dispatchChestCloseKey(key, "PRESS");
                }, () -> System.currentTimeMillis() - pressedAt[0] >= (gesture.startsWith("hold") ? 900L : 80L),
                    () -> {
                        if (gesture.startsWith("hold")) {
                            check(label + "-closed-before-key-release", nativeScreen() == null);
                        }
                        dispatchChestCloseKey(key, "RELEASE");
                    }, 0);
                // Let the native opener recheck, block event and lid animation settle.
                add(label + "-animation-settle", () -> {}, () -> true, () -> {}, 45);
                add(label + "-closed-state", () -> server(InWorldGameplayHarness::chestCloseSnapshot),
                    InWorldGameplayHarness::taskDone, () -> {
                        List<Integer> snapshot = (List<Integer>) task.join();
                        float openness = chestLidOpenness();
                        check(label + "-desktop-dismissed", nativeScreen() == null);
                        record(label + "-native-openers-zero", snapshot.get(0) == 0 ? "PASS" : "FAIL", snapshot.toString());
                        record(label + "-native-lid-closed", openness <= 0.01F ? "PASS" : "FAIL", "openness=" + openness);
                        check(label + "-contents-preserved", snapshot.get(1) == 32);
                        capture(label + "-after-close");
                    }, 2);
                if (persistent && !gesture.equals("hold-escape")) {
                    add(label + "-restore", () -> InventoryDesktopScreen.openOrToggleInventory(mc),
                        () -> nativeScreen() instanceof InventoryDesktopScreen && !sessions().isEmpty(), () -> {
                            check(label + "-window-restored", windowKind("CONTAINER") != null
                                || sessions().stream().anyMatch(session -> windowSession(session.sessionId()) != null));
                            capture(label + "-restored");
                        }, 20);
                    add(label + "-restored-native-state", () -> server(InWorldGameplayHarness::chestCloseSnapshot),
                        InWorldGameplayHarness::taskDone, () -> {
                            List<Integer> snapshot = (List<Integer>) task.join();
                            record(label + "-restored-native-viewer", snapshot.get(0) == 1 ? "PASS" : "FAIL", snapshot.toString());
                            record(label + "-restored-native-lid", chestLidOpenness() > 0.9F ? "PASS" : "FAIL", "openness=" + chestLidOpenness());
                            check(label + "-restored-contents-preserved", snapshot.get(1) == 32);
                        }, 5);
                }
                add(label + "-cleanup", InWorldGameplayHarness::closeWindows, () -> sessions().isEmpty(), () -> {}, 15);
            }
        }
    }

    private static List<Integer> nativeOpenerLifecycle() {
        var backing = new net.minecraft.world.SimpleContainer(27);
        backing.setItem(0, new ItemStack(Items.DIRT, 32));
        var opened = new java.util.concurrent.atomic.AtomicInteger();
        var closed = new java.util.concurrent.atomic.AtomicInteger();
        Container container = (Container) Proxy.newProxyInstance(Container.class.getClassLoader(), new Class<?>[]{Container.class},
            (proxy, method, args) -> {
                if (method.getName().equals("startOpen")) { opened.incrementAndGet(); return null; }
                if (method.getName().equals("stopOpen")) { closed.incrementAndGet(); return null; }
                return method.invoke(backing, args);
            });
        Object type = field(ClassFor("net.minecraft.world.inventory.MenuType"), "GENERIC_9x3");
        Object menu = construct(ClassFor("net.minecraft.world.inventory.ChestMenu"), type, 50001, player().getInventory(), container, 3);
        int initiallyOpened = opened.get();
        call(menu, "salts_inventory_update$setOpenerVisible", player(), false);
        call(menu, "salts_inventory_update$setOpenerVisible", player(), false);
        int hiddenClosed = closed.get();
        call(menu, "salts_inventory_update$setOpenerVisible", player(), true);
        call(menu, "salts_inventory_update$setOpenerVisible", player(), true);
        int restoredOpened = opened.get();
        call(menu, "salts_inventory_update$setOpenerVisible", player(), false);
        call(menu, "removed", player());
        int hiddenFinalClosed = closed.get();
        call(menu, "salts_inventory_update$setOpenerVisible", player(), true);
        int disposedOpened = opened.get();
        Object visible = construct(ClassFor("net.minecraft.world.inventory.ChestMenu"), type, 50002, player().getInventory(), container, 3);
        call(visible, "removed", player());
        call(visible, "removed", player());
        return List.of(initiallyOpened, hiddenClosed, restoredOpened, hiddenFinalClosed, disposedOpened,
            opened.get(), closed.get(), backing.getItem(0).getCount());
    }

    private static List<Integer> chestCloseSnapshot() {
        Object chest = serverLevel().getBlockEntity(origin);
        int openers = ((Number) call(field(chest, "openersCounter"), "getOpenerCount")).intValue();
        return List.of(openers, ((Container) chest).getItem(0).getCount());
    }

    private static float chestLidOpenness() {
        Object chest = mc.level.getBlockEntity(origin);
        return ((Number) call(chest, "getOpenNess", 0.0F)).floatValue();
    }

    private static void dispatchChestCloseKey(String name, String actionName) {
        Class<?> input = ClassFor("com.mojang.blaze3d.platform.InputConstants");
        int key = ((Number) field(input, name)).intValue();
        int action;
        try { action = ((Number) field(input, actionName)).intValue(); }
        catch (IllegalArgumentException legacy) { action = actionName.equals("PRESS") ? 1 : 0; }
        Class<?> controller = ClassFor("com.salts_inventory_update.client.InventoryKeyHoldController");
        Method handler = Arrays.stream(controller.getMethods())
            .filter(method -> method.getName().equals("handleInventoryKeyAction")).findFirst().orElseThrow();
        Object event = construct(handler.getParameterTypes()[2], key, 0, 0);
        boolean handled = (Boolean) call(controller, "handleInventoryKeyAction", mc, action, event);
        // Same fallback as KeyboardHandler when the hold controller declines a key.
        if (!handled && nativeScreen() instanceof InventoryDesktopScreen) {
            String method = actionName.equals("PRESS") ? "keyPressed" : "keyReleased";
            try { call(screen(), method, key, 0, 0); }
            catch (IllegalArgumentException modern) { call(screen(), method, event); }
        }
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
                // Sophisticated chests build a different provider during real block use, including opening data.
                if(provider!=null && !label.startsWith("sophisticated-")) p.openMenu((net.minecraft.world.MenuProvider)provider);
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
            if(label.equals("toms-level_emitter")||label.equals("toms-inventory_cable_connector")) {
                Object context=call(screen(),"apiContext",activeWindow,null,0,0);
                int rightExtent=label.equals("toms-level_emitter")?132:164,bottomExtent=label.equals("toms-level_emitter")?60:130;
                check(label+"-controls-inside-frame",((Number)call(context,"contentX")).doubleValue()+8+rightExtent<=number(activeWindow,"x")+number(activeWindow,"width")-4
                    && ((Number)call(context,"contentY")).doubleValue()+8+bottomExtent<=number(activeWindow,"y")+number(activeWindow,"height")-4);
                if(label.equals("toms-level_emitter"))registerEmitterPixelChecks(context,label);
            }
            capture(label);
        }, label.equals("toms-inventory_cable_connector")?100:12);
        if(label.equals("crafting_table")) addCrafting();
        if(label.equals("furnace")) addSmelting();
        if(label.equals("toms-storage_terminal") || TOMS_PICKUP_ONLY && label.equals("toms-crafting_terminal"))
            addTerminalInteractions(TOMS_PICKUP_ONLY ? label : "terminal");
        if(VersionInfo.MINECRAFT_VERSION.equals("1.20.1")) {
            if(label.equals("toms-inventory_cable_connector_filtered"))addTomLegacyConnectorControls();
            if(label.equals("toms-level_emitter"))addTomLegacyEmitterControls();
        }
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
            String expectedMod=switch(SCENARIO) {
                case "browser-jei" -> "jei";case "browser-rei" -> "roughlyenoughitems";case "browser-emi" -> "emi";
                case "toms" -> "toms_storage";case "sophisticated" -> "sophisticatedcore";case "ae2","ae2-atlas" -> "ae2";default -> null;
            };
            if(expectedMod!=null) {
                boolean present=com.salts_inventory_update.api.integration.DesktopEnvironment.isModLoaded(expectedMod);
                check("requested-upstream-mod-present",present);
                if(!present){finish();return;}
            }
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
            if (TOMS_PICKUP_ONLY) {
                for (String id : List.of("storage_terminal", "crafting_terminal"))
                    addContainer("toms-" + id, "toms_storage:" + id, 0);
                return;
            }
            boolean legacy=VersionInfo.MINECRAFT_VERSION.equals("1.20.1");
            for(String id:legacy?List.of("storage_terminal","crafting_terminal","level_emitter","inventory_cable_connector_filtered"):List.of("storage_terminal","crafting_terminal","level_emitter","filing_cabinet"))
                addContainer("toms-"+id,"toms_storage:"+id,0);
            for(String id:legacy?List.of("item_filter","tag_item_filter"):List.of("item_filter","tag_item_filter","inventory_configurator"))addItemIntegration("toms-"+id,"toms_storage:"+id);
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
            check(label+"-api-definition",field(activeWindow,"apiDefinition")!=null);
            if(label.equals("toms-tag_item_filter")) {
                Object context=call(screen(),"apiContext",activeWindow,null,0,0);
                check("tag-filter-panels-inside-frame",((Number)call(context,"contentX")).doubleValue()+8+184<=number(activeWindow,"x")+number(activeWindow,"width")-4
                    && ((Number)call(context,"contentY")).doubleValue()+8+64<=number(activeWindow,"y")+number(activeWindow,"height")-4);
            }
            capture(label);
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
        },()->{
            check("crafting-tab-hosted-position-hook",ClassFor("com.salts_inventory_update.compat.sophisticated.client.SophisticatedHostedPositionAccess").isInstance(sophisticatedCraftingTab()));
            capture("backpack-crafting-upgrade-panel");
        },12);
        if(VersionInfo.MINECRAFT_VERSION.equals("26.1.2")) addSophisticatedPopupGeometry();
    }
    private static Object sophisticatedCraftingTab(){
        Object state=field(windowSession(activeSession.sessionId()),"apiState");
        Object tabs=call(call(field(state,"access"),"components"),"sideTabs");
        return ((List<?>)call(tabs,"children")).stream().filter(t->t.getClass().getSimpleName().contains("CraftingUpgradeTab")).findFirst().orElseThrow();
    }
    @SuppressWarnings("unchecked")
    private static void addSophisticatedPopupGeometry(){
        var previous=new java.util.concurrent.atomic.AtomicReference<List<ItemStack>>();
        add("crafting-result-popup-preview-fixture",()->{
            // UI-only native preview data: no recipes, inventory stacks or server outputs are fabricated.
            List<ItemStack> results=(List<ItemStack>)call(call(sophisticatedCraftingTab(),"getContainer"),"getMatchedCraftingResults");
            previous.set(List.copyOf(results));results.clear();
            results.add(new ItemStack(Items.DIAMOND));results.add(new ItemStack(Items.IRON_INGOT));
        },()->((List<?>)call(call(sophisticatedCraftingTab(),"getContainer"),"getMatchedCraftingResults")).size()==2,()->{},4);
        add("crafting-result-popup-open",()->{
            Object button=field(sophisticatedCraftingTab(),"selectResultButton");
            clickAt(((Number)call(button,"getX")).doubleValue()+4,((Number)call(button,"getY")).doubleValue()+4);
        },()->Boolean.TRUE.equals(field(sophisticatedCraftingTab(),"resultSelectionShown"))
            && field(sophisticatedCraftingTab(),"resultListPositionDimensions")!=null
            && ((List<?>)field(sophisticatedCraftingTab(),"resultChoicePositions")).size()==2,()->{
            Object tab=sophisticatedCraftingTab();Object bounds=field(tab,"resultListPositionDimensions");
            Object point=call(bounds,bounds.getClass().getName().equals("net.minecraft.util.Tuple")?"getA":"getFirst");
            int x=((Number)call(point,"x")).intValue(),y=((Number)call(point,"y")).intValue();
            List<?> positions=(List<?>)field(tab,"resultChoicePositions");
            List<int[]> original=positions.stream().map(p->new int[]{((Number)call(p,"x")).intValue(),((Number)call(p,"y")).intValue()}).toList();
            int dx=83,dy=47;
            call(tab,"salts_inventory_update$translateHostedPosition",dx,dy);
            Object moved=field(tab,"resultListPositionDimensions");
            Object movedPoint=call(moved,moved.getClass().getName().equals("net.minecraft.util.Tuple")?"getA":"getFirst");
            check("crafting-popup-native-bounds-translated",((Number)call(movedPoint,"x")).intValue()==x+dx&&((Number)call(movedPoint,"y")).intValue()==y+dy);
            for(int i=0;i<positions.size();i++){
                Object choice=positions.get(i);
                check("crafting-popup-native-choice-translated-"+i,((Number)call(choice,"x")).intValue()==original.get(i)[0]+dx&&((Number)call(choice,"y")).intValue()==original.get(i)[1]+dy);
            }
            check("crafting-popup-native-hit-follows-translation",((Optional<?>)call(tab,"getResultChoiceHovered",original.get(0)[0]+dx+8,original.get(0)[1]+dy+8)).map(value->value.equals(0)).orElse(false));
            call(tab,"salts_inventory_update$translateHostedPosition",-dx,-dy);
            check("crafting-popup-native-hit-restored",((Optional<?>)call(tab,"getResultChoiceHovered",original.get(0)[0]+8,original.get(0)[1]+8)).map(value->value.equals(0)).orElse(false));
            capture("crafting-result-popup-before-window-move");
        },10);
        add("crafting-result-popup-window-drag",()->{
            Object window=windowSession(activeSession.sessionId());beforeX=number(window,"x");beforeY=number(window,"y");
            double x=beforeX+24,y=beforeY+5;
            mouse("mouseClicked",x,y,left(),0,0);mouse("mouseDragged",x+24,y+12,left(),24,12);mouse("mouseReleased",x+24,y+12,left(),0,0);
        },()->number(windowSession(activeSession.sessionId()),"x")==beforeX+24&&number(windowSession(activeSession.sessionId()),"y")==beforeY+12,()->{
            check("crafting-popup-remains-open-after-window-drag",Boolean.TRUE.equals(field(sophisticatedCraftingTab(),"resultSelectionShown")));
            capture("crafting-result-popup-after-window-move");
        },10);
        add("crafting-result-popup-preview-cleanup",()->{
            List<ItemStack> results=(List<ItemStack>)call(call(sophisticatedCraftingTab(),"getContainer"),"getMatchedCraftingResults");
            results.clear();results.addAll(previous.get());
        },()->!Boolean.TRUE.equals(field(sophisticatedCraftingTab(),"resultSelectionShown")),()->{},4);
    }
    private static void addTomLegacyConnectorControls() {
        var expected=new java.util.concurrent.atomic.AtomicBoolean();
        add("legacy-connector-allowlist-click",()->{
            check("legacy-connector-nine-filter-slots",currentSession().menu().slots.stream().filter(slot->!(slot.container instanceof net.minecraft.world.entity.player.Inventory)).count()==9);
            expected.set(!(Boolean)field(currentSession().menu(),"allowList"));
            Object context=call(screen(),"apiContext",windowSession(activeSession.sessionId()),null,0,0);
            clickAt(((Number)call(context,"contentX")).doubleValue()+16,((Number)call(context,"contentY")).doubleValue()+16);
        },()->((Boolean)field(currentSession().menu(),"allowList"))==expected.get(),()->{
            var actual=server(()->call(field(serverMenu(),"te"),"isAllowList"));
            addFirst("legacy-connector-authoritative",()->{},actual::isDone,()->check("legacy-connector-native-filter-updated",actual.join().equals(expected.get())),3);
            capture("toms-legacy-connector-filter-updated");
        },8);
    }
    private static void addTomLegacyEmitterControls() {
        var expected=new java.util.concurrent.atomic.AtomicInteger();
        add("legacy-emitter-increase-click",()->{
            Object state=field(windowSession(activeSession.sessionId()),"apiState");expected.set(((Number)field(state,"count")).intValue()+1);
            Object context=call(screen(),"apiContext",windowSession(activeSession.sessionId()),null,0,0);
            clickAt(((Number)call(context,"contentX")).doubleValue()+92,((Number)call(context,"contentY")).doubleValue()+59);
        },()->((Number)field(field(windowSession(activeSession.sessionId()),"apiState"),"count")).intValue()==expected.get(),()->{
            var actual=server(()->call(field(serverMenu(),"te"),"getCount"));
            addFirst("legacy-emitter-authoritative",()->{},actual::isDone,()->check("legacy-emitter-native-count-updated",actual.join().equals(expected.get())),3);
            capture("toms-legacy-emitter-updated");
        },12);
    }

    private static void addTerminalInteractions(String prefix){
        add(prefix+"-network-snapshot",()->{},()->!((List<?>)field(field(windowSession(activeSession.sessionId()),"apiState"),"entries")).isEmpty(),
            ()->{check(prefix+"-two-item-types",((List<?>)field(field(windowSession(activeSession.sessionId()),"apiState"),"entries")).size()==2);capture(TOMS_PICKUP_ONLY?prefix+"-connected":"toms-connected-terminal");},5);
        add(prefix+"-withdraw-click",()->{
            Object w=windowSession(activeSession.sessionId());Object c=call(screen(),"apiContext",w,null,0,0);
            clickAt(((Number)call(c,"contentX")).doubleValue()+8,((Number)call(c,"contentY")).doubleValue()+27);
        },()->carried()==32 || TOMS_PICKUP_ONLY,()->{
            var value=server(()->{
                Container c=(Container)serverLevel().getBlockEntity(networkStoragePos);int n=0;
                for(int i=0;i<c.getContainerSize();i++)n+=c.getItem(i).getCount();return List.of(n,player().inventoryMenu.getCarried().getCount());
            });addFirst(prefix+"-authoritative-withdraw",()->{},value::isDone,()->{
                record(prefix+"-withdraw-conservation",value.join().equals(List.of(32,32))?"PASS":"FAIL","storage/canonical cursor="+value.join());
                check(prefix+"-client-cursor-has-stack",carried()==32);
                capture(prefix+"-picked-up");
                if (TOMS_PICKUP_ONLY && (!value.join().equals(List.of(32,32)) || carried()!=32)) STEPS.clear();
            },3);
        },TOMS_PICKUP_ONLY?20:8);
        add(prefix+"-deposit-click",()->{
            Object w=windowSession(activeSession.sessionId());Object c=call(screen(),"apiContext",w,null,0,0);
            clickAt(((Number)call(c,"contentX")).doubleValue()+8,((Number)call(c,"contentY")).doubleValue()+27);
        },()->carried()==0,()->{
            var value=server(()->{
                Container c=(Container)serverLevel().getBlockEntity(networkStoragePos);int n=0;
                for(int i=0;i<c.getContainerSize();i++)n+=c.getItem(i).getCount();return n;
            });addFirst(prefix+"-authoritative-deposit",()->{},value::isDone,()->check(prefix+"-deposit-conservation",value.join()==64),3);
            capture(TOMS_PICKUP_ONLY?prefix+"-deposit":"toms-terminal-deposit");
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
                String expected=SCENARIO.equals("browser-rei")?"roughlyenoughitems":SCENARIO.substring("browser-".length());
                check("requested-provider-active",expected.equals(access.source().id()));
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
        if (VersionInfo.MINECRAFT_VERSION.equals("1.20.1")) addPublicTooltipApiProbe();
        addRecipeBrowserTransfer();
        add("recipe-browser-close",InWorldGameplayHarness::closeWindows,()->true,()->{},3);
        if(com.salts_inventory_update.api.integration.DesktopEnvironment.isModLoaded("modmenu")) {
            add("modmenu-config-factory",()->{
                Object plugin=construct(ClassFor("com.salts_inventory_update.compat.modmenu.SaltsInventoryModMenu"));
                Object factory=call(plugin,"getModConfigScreenFactory");
                setNativeScreen(call(factory,"create",(Object)null));
            },()->nativeScreen()!=null,()->capture("modmenu-configuration"),8);
            add("modmenu-config-close",()->call(nativeScreen(),"onClose"),()->nativeScreen()==null,()->{},3);
        }
    }

    /** Exercises the public legacy native-component tooltip API during a real recipe render. */
    private static void addPublicTooltipApiProbe() {
        add("public-tooltip-api-probe",()->{
            tooltipProbeOriginal=com.salts_inventory_update.api.client.recipebrowser.RecipeBrowserBridge.access();
            TOOLTIP_PROBE_DRAWS.set(0);
            var api=com.salts_inventory_update.api.client.recipebrowser.RecipeBrowserAccess.class;
            var probe=(com.salts_inventory_update.api.client.recipebrowser.RecipeBrowserAccess)Proxy.newProxyInstance(
                api.getClassLoader(),new Class<?>[]{api},(proxy,method,args)->{
                    Object result;
                    try { result=method.invoke(tooltipProbeOriginal,args); }
                    catch(InvocationTargetException failure) { throw failure.getCause(); }
                    if(method.getName().equals("renderRecipe")) {
                        Object component=call(ClassFor("net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent"),"create",
                            net.minecraft.network.chat.Component.literal("Public API tooltip components").getVisualOrderText());
                        call(args[0],"setClientTooltipForNextFrame",mc.font,List.of(component),24,80);
                        TOOLTIP_PROBE_DRAWS.incrementAndGet();
                    }
                    return result;
                });
            com.salts_inventory_update.api.client.recipebrowser.RecipeBrowserBridge.install(tooltipProbeOriginal.source(),probe);
        },()->TOOLTIP_PROBE_DRAWS.get()>0,()->{
            check("native-tooltip-invoker-resolves",TOOLTIP_PROBE_DRAWS.get()>0);
            capture("public-api-tooltip-probe");
        },4);
        add("public-tooltip-api-restore",()->
            com.salts_inventory_update.api.client.recipebrowser.RecipeBrowserBridge.install(tooltipProbeOriginal.source(),tooltipProbeOriginal),
            ()->true,()->{},2);
    }

    /** Uses a visible provider transfer button, then checks native server crafting and the shared cursor. */
    private static void addRecipeBrowserTransfer() {
        add("recipe-transfer-fixture", () -> {
            closeWindows();
            server(() -> {
                var player=player();var level=serverLevel(player);
                player.getInventory().clearContent();player.getInventory().setItem(0,new ItemStack(Items.OAK_PLANKS,4));
                player.inventoryMenu.setCarried(ItemStack.EMPTY);player.inventoryMenu.broadcastFullState();
                level.setBlock(origin,net.minecraft.world.level.block.Blocks.CRAFTING_TABLE.defaultBlockState(),3);
                player.openMenu((net.minecraft.world.MenuProvider)call(level.getBlockState(origin),"getMenuProvider",level,origin));
                return null;
            });
        }, () -> taskDone() && sessions().stream().anyMatch(s -> s.menu() instanceof net.minecraft.world.inventory.CraftingMenu), () -> {
            activeSession=sessions().stream().filter(s -> s.menu() instanceof net.minecraft.world.inventory.CraftingMenu).findFirst().orElseThrow();
            moveQaWindow(windowSession(activeSession.sessionId()),12,50);
        },12);
        add("recipe-transfer-open-browser", () -> {
            InventoryDesktopScreen.openOrToggleJei(mc);
            var access=com.salts_inventory_update.api.client.recipebrowser.RecipeBrowserBridge.access();
            access.setFilterText("");
            Object window=windowKind("JEI");
            call(screen(),"enterJeiRecipeView",window,access.entryForItemStack(new ItemStack(Items.CRAFTING_TABLE)),
                com.salts_inventory_update.api.client.recipebrowser.RecipeBrowserMode.RECIPES);
            moveQaWindow(window,Math.max(8,number(screen(),"width")-number(window,"width")-8),20);
        }, () -> windowKind("JEI")!=null, () -> capture("recipe-transfer-ready"),12);
        add("recipe-transfer-click", () -> {
            Object window=windowKind("JEI");
            Object category=call(screen(),"selectedRecipeBrowserCategory",window);
            var access=com.salts_inventory_update.api.client.recipebrowser.RecipeBrowserBridge.access();
            for(Object placement:(List<?>)call(screen(),"visibleJeiRecipePlacements",window,category)) {
                var recipe=(com.salts_inventory_update.api.client.recipebrowser.RecipeBrowserRecipe)call(placement,"recipe");
                var plan=access.recipeTransferPlan(recipe,currentSession().menu());
                if(plan==null)continue;
                Object rect=call(screen(),"jeiRecipeTransferButtonRect",placement,plan);
                double x=((Number)call(rect,"x")).doubleValue()+((Number)call(rect,"width")).doubleValue()/2;
                double y=((Number)call(rect,"y")).doubleValue()+((Number)call(rect,"height")).doubleValue()/2;
                Object hit=call(screen(),"jeiRecipeTransferButtonAt",window,x,y);
                if(hit==null || !Boolean.TRUE.equals(call(call(hit,"availability"),"craftable")))continue;
                check("provider-transfer-plan-present",!plan.requirements().isEmpty());
                clickAt(x,y);return;
            }
            throw new IllegalStateException("No craftable provider transfer button for four oak planks");
        }, () -> currentSession().menu().getSlot(0).getItem().is(Items.CRAFTING_TABLE) && DesktopContainerClient.canDispatchOptimisticMutationNow(), () -> {
            var state=server(() -> {
                var menu=serverMenu();int grid=0,inventory=0;
                for(int i=1;i<=9;i++)grid+=menu.getSlot(i).getItem().getCount();
                for(int i=0;i<player().getInventory().getContainerSize();i++)
                    if(player().getInventory().getItem(i).is(Items.OAK_PLANKS))inventory+=player().getInventory().getItem(i).getCount();
                return List.of(grid,inventory,menu.getSlot(0).getItem().is(Items.CRAFTING_TABLE)?1:0);
            });
            addFirst("recipe-transfer-authoritative",()->{},state::isDone,()->{
                check("recipe-transfer-server-grid-and-conservation",state.join().equals(List.of(4,0,1)));
                capture("recipe-transferred");
            },3);
        },12);
        add("recipe-transfer-close-browser",()->InventoryDesktopScreen.openOrToggleJei(mc),()->windowKind("JEI")==null,()->{},3);
        add("recipe-transfer-craft-result",()->clickSlot(0,left()),()->carried()==1,()->{
            var state=server(()->{
                var menu=serverMenu();boolean empty=true;
                for(int i=1;i<=9;i++)empty &= menu.getSlot(i).getItem().isEmpty();
                return empty && player().inventoryMenu.getCarried().is(Items.CRAFTING_TABLE);
            });
            addFirst("recipe-transfer-crafted-authoritative",()->{},state::isDone,()->check("recipe-transfer-crafted-on-server",state.join()),3);
        },8);
        add("recipe-transfer-store-output",InWorldGameplayHarness::storeInPlayer,()->carried()==0,()->capture("recipe-transfer-complete"),8);
    }

    private static void moveQaWindow(Object window,int targetX,int targetY) {
        double x=number(window,"x")+25,y=number(window,"y")+5;
        double dx=targetX-number(window,"x"),dy=targetY-number(window,"y");
        mouse("mouseClicked",x,y,left(),0,0);
        mouse("mouseDragged",x+dx,y+dy,left(),dx,dy);
        mouse("mouseReleased",x+dx,y+dy,left(),0,0);
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
        add("window-unpin",()->clickControl("PIN"),()->field(windowSession(activeSession.sessionId()),"pinMode").toString().equals("UNPINNED"),()->{},5);
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
        for(var action:new ArrayList<>(spatialActions))if(System.currentTimeMillis()>=action.at()){
            spatialActions.remove(action);action.action().run();
        }
        spatialObserveManualHold();
        if(wallsObserveTransition) {
            wallsTransitionSamples.add(((Number)call(ClassFor("com.salts_inventory_update.spatial.SpatialMovementTransition"),"blend")).doubleValue());
            wallsCameraSamples.add(((net.minecraft.world.phys.Vec3)spatialCameraPosition()).distanceTo(mc.player.getEyePosition()));
        }
        clearQaToasts.run();
        if(Files.exists(output.resolve("abort.request"))){record("suite-aborted","FAIL","Explicit QA abort requested");finish();return;}
        if(elapsedTicks<15) return;
        // Creating a world while the initial splash overlay is still present leaves
        // Minecraft's synchronous world-load loop waiting for an unrendered overlay.
        if(active==null && mc.player==null && overlay()!=null) return;
        if(!MANUAL_MOUSE_TWEAKS && System.currentTimeMillis()-startedAt>20*60*1000L){record("suite-timeout","FAIL","20-minute deadline exceeded");finish();return;}
        try {
            if(active==null) {
                if(STEPS.isEmpty()){
                    if(MANUAL_MOUSE_TWEAKS){
                        manualMouseTweaksObserve();
                        if(Files.exists(output.resolve("manual-finish.request")))finish();
                        return;
                    }
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
                if (WINDOW_CONTROLS_ONLY) {
                    // Yield after changing screens so framebuffer evidence captures a rendered UI.
                    stepStarted = System.currentTimeMillis();
                    return;
                }
            }
            if (WINDOW_CONTROLS_ONLY && System.currentTimeMillis() - stepStarted < 500L) return;
            boolean ready=active.ready().getAsBoolean();
            if(ready) {
                if(settle<0) settle=active.settleTicks();
                if(settle--<=0) {
                    if(SPATIAL_REPORT && active.name().startsWith("spatial-"))capture("report-"+active.name());
                    active.verify().run();record(active.name(),"PASS","completed in-world stage");active=null;
                }
            } else {
                settle=-1;
                if(System.currentTimeMillis()-stepStarted>((active.name().equals("spatial-manual-review")||active.name().equals("walls-manual")) ? 1800000L : MOUSE_TWEAKS_ONLY && active.name().startsWith("mt-") ? 8000L : 45000L)) throw new IllegalStateException("Stage timed out; screen="+(nativeScreen()==null?"none":nativeScreen().getClass().getName())+", sessions="+sessions().size()+", carried="+carried());
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
        mc.options.renderDistance().set(2);
        mc.options.simulationDistance().set(5);
        try {call(mc.getWindow(),"setWindowed",1280,SCENARIO.equals("ae2-atlas") ? 960 : 900);} catch(Throwable ignored){}
        mc.options.guiScale().set(2);
        try { call(mc,"resizeDisplay"); } catch(IllegalArgumentException ignored) { }
        try { call(call(mc,"getTutorial"),"setStep",enumValue("net.minecraft.client.tutorial.TutorialSteps","NONE")); } catch(Throwable ignored) { }
        for(String getter:List.of("getToasts","getToastManager")) {
            try {
                Object manager=call(mc,getter);Method clear=manager.getClass().getMethod("clear");
                clearQaToasts=()->{try{clear.invoke(manager);}catch(ReflectiveOperationException failure){throw new IllegalStateException(failure);}};
                break;
            } catch(ReflectiveOperationException|IllegalArgumentException ignored) { }
        }
        SaltsInventoryConfig.update(config->{config.enableMod=true;config.openUnlocked=true;config.allowResizing=true;config.expandableInventory=true;if(MANUAL_MOUSE_TWEAKS)config.hasSeenInstructionsWindow=true;});
        if (SCENARIO.equals("ae2-atlas")) SaltsInventoryConfig.update(config -> {
            config.openUnlocked=false;  config.expandableInventory=false;
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
        clickWindowControl(windowSession(activeSession.sessionId()),name);
    }
    private static void clickWindowControl(Object w,String name){
        Object layout=call(screen(),"titleBarLayout",w);
        List<?> rects=(List<?>)call(layout,"controls");Object selected=null;
        for(Object rect:rects) if(call(rect,"control").toString().equals(name))selected=rect;
        if(selected==null){
            for(Object rect:rects)if(field(screen(),"popupWindow") != w && call(rect,"control").toString().equals("ELLIPSIS")){
                double x=((Number)call(rect,"x")).doubleValue()+4,y=((Number)call(rect,"y")).doubleValue()+4;
                mouse("mouseClicked",x,y,left(),0,0);mouse("mouseReleased",x,y,left(),0,0);
            }
            check("overflow-popup-open-before-"+name,field(screen(),"popupWindow") == w);
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
            Object info=construct(ClassFor("net.minecraft.client.input.MouseButtonInfo"),button,mtShift?1:0);
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
        Consumer<Path> verify=CAPTURE_CHECKS.remove(label);if(verify!=null)verify.accept(p);
        Map<String,Object> result=new LinkedHashMap<>();result.put("name",label+"-framebuffer");result.put("status","CAPTURED");result.put("path",p.toString());RESULTS.add(result);
    }
    private static void registerEmitterPixelChecks(Object context,String label){
        int x=((Number)call(context,"contentX")).intValue()+8,y=((Number)call(context,"contentY")).intValue()+8;
        int guiWidth=((Number)call(mc.getWindow(),"getGuiScaledWidth")).intValue();
        CAPTURE_CHECKS.put(label,path->{
            try{
                var image=javax.imageio.ImageIO.read(path.toFile());
                double scale=image.getWidth()/(double)guiWidth;
                assertTextPixels(image,scale,"toms-emitter-label-visible",x+28,y+10,84,9,false);
                assertTextPixels(image,scale,"toms-emitter-count-visible",x+32,y+28,50,9,true);
                for(int i=0;i<4;i++)assertTextPixels(image,scale,"toms-emitter-adjustment-"+i+"-visible",x+31+i*24,y+49,16,9,true);
            }catch(java.io.IOException failure){throw new IllegalStateException("Cannot inspect emitter framebuffer",failure);}
        });
    }
    private static void assertTextPixels(java.awt.image.BufferedImage image,double scale,String name,int x,int y,int width,int height,boolean light){
        int pixels=0;
        for(int py=Math.max(0,(int)Math.floor(y*scale));py<Math.min(image.getHeight(),(int)Math.ceil((y+height)*scale));py++)
            for(int px=Math.max(0,(int)Math.floor(x*scale));px<Math.min(image.getWidth(),(int)Math.ceil((x+width)*scale));px++){
                int rgb=image.getRGB(px,py),r=(rgb>>16)&255,g=(rgb>>8)&255,b=rgb&255;
                if(light?r>225&&g>225&&b>225:r<100&&g<100&&b<100)pixels++;
            }
        record(name,pixels>8?"PASS":"FAIL","visible text pixels="+pixels);
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
        report.put("completeCoreFeatureSuite", FULL_CORE);
        if(!ACCESSORY.isEmpty())report.put("nativeAccessoryFixture",ACCESSORY);
        if ("true".equalsIgnoreCase(System.getenv("SIU_QA_CHEST_CLOSE_ONLY"))) report.put("scope", "chest-close-issue-15");
        if (TOMS_PICKUP_ONLY) report.put("scope", "toms-pickup-issue-17");
        if (WINDOW_CONTROLS_ONLY) report.put("scope", "recipe-book-and-movement-controls");
        if (INVENTORY_SORT_ONLY) report.put("scope", "in-place-inventory-sorting");
        if (MOUSE_TWEAKS_ONLY) report.put("scope","mouse-tweaks-core-only");
        if (MANUAL_MOUSE_TWEAKS) report.put("scope","mouse-tweaks-computer-use-fixture");
        if (KEYBINDS_ONLY) report.put("scope", "key-bindings-and-inventory-hold");
        if (BROWSER_SEARCH_ONLY) report.put("scope", "ancient-remnants-browser-search");
        if (RAISED_ONLY) report.put("scope", "raised-hud-interoperability");
        if (HUD_VISIBILITY_ONLY) report.put("scope", "window-hosted-hud-visibility");
        if (HUD_MOVEMENT_ONLY) report.put("scope", "window-hosted-hud-movement");
        if (SPATIAL_ONLY) report.put("scope", "spatial-gui-"+VersionInfo.MINECRAFT_VERSION+"-"+System.getenv("SIU_FUNCTIONAL_TEST_LOADER"));
        Map<String,String> mods=loadedMods();report.put("loadedMods",mods);
        if(mods.containsKey("salts_inventory_compat"))report.put("compatibilityCodeSource",ClassFor("com.salts_inventory_update.compat.CompatibilityBootstrap").getProtectionDomain().getCodeSource().getLocation().toString());
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
    private static Object registryValue(String registry,String id){if(VersionInfo.MINECRAFT_VERSION.equals("1.20.1")&&id.startsWith("toms_storage:")&&!id.startsWith("toms_storage:ts."))id=id.replace("toms_storage:","toms_storage:ts.");Object values=field(ClassFor("net.minecraft.core.registries.BuiltInRegistries"),registry);for(Object value:(Iterable<?>)values)if(id.equals(call(values,"getKey",value).toString()))return value;return null;}
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
