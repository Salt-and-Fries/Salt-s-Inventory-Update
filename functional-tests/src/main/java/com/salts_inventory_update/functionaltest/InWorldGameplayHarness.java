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
    private static final boolean WINDOW_CONTROLS_ONLY = HUD_MOVEMENT_ONLY || HUD_VISIBILITY_ONLY || RAISED_ONLY || MOUSE_TWEAKS_ONLY || "true".equalsIgnoreCase(System.getenv("SIU_QA_WINDOW_CONTROLS_ONLY"));
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
            if (HUD_MOVEMENT_ONLY || HUD_VISIBILITY_ONLY) addHudVisibilityRegression();
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
        if (HUD_MOVEMENT_ONLY) addHudMovementRegression();
        add("hud-visibility-cleanup", InWorldGameplayHarness::closeWindows, () -> sessions().isEmpty(), () -> {}, 3);
    }

    private static void addHudMovementRegression() {
        add("hud-movement-seed", () -> {
            SaltsInventoryConfig.update(config -> config.toggleMovementControl = true);
            setMovementBinding("KEY_LALT");
            com.salts_inventory_update.client.WindowedInventoryClient.resetMovementControl();
            server(() -> {
                for (int i = 0; i < 9; i++) player().getInventory().setItem(i, new ItemStack(Items.DIRT, i + 1));
                player().getInventory().setItem(9, new ItemStack(Items.SAND, 3));
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
        },()->capture("backpack-crafting-upgrade-panel"),12);
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
                    active.verify().run();record(active.name(),"PASS","completed in-world stage");active=null;
                }
            } else {
                settle=-1;
                if(System.currentTimeMillis()-stepStarted>(MOUSE_TWEAKS_ONLY && active.name().startsWith("mt-") ? 8000L : 45000L)) throw new IllegalStateException("Stage timed out; screen="+(nativeScreen()==null?"none":nativeScreen().getClass().getName())+", sessions="+sessions().size()+", carried="+carried());
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
        if ("true".equalsIgnoreCase(System.getenv("SIU_QA_CHEST_CLOSE_ONLY"))) report.put("scope", "chest-close-issue-15");
        if (TOMS_PICKUP_ONLY) report.put("scope", "toms-pickup-issue-17");
        if (WINDOW_CONTROLS_ONLY) report.put("scope", "recipe-book-and-movement-controls");
        if (MOUSE_TWEAKS_ONLY) report.put("scope","mouse-tweaks-core-only");
        if (MANUAL_MOUSE_TWEAKS) report.put("scope","mouse-tweaks-computer-use-fixture");
        if (KEYBINDS_ONLY) report.put("scope", "key-bindings-and-inventory-hold");
        if (BROWSER_SEARCH_ONLY) report.put("scope", "ancient-remnants-browser-search");
        if (RAISED_ONLY) report.put("scope", "raised-hud-interoperability");
        if (HUD_VISIBILITY_ONLY) report.put("scope", "window-hosted-hud-visibility");
        if (HUD_MOVEMENT_ONLY) report.put("scope", "window-hosted-hud-movement");
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
