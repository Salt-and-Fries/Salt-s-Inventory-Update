package com.salts_inventory_update.controlify.qa;

import com.google.gson.GsonBuilder;
import com.salts_inventory_update.controlify.SaltsControlifyClient;
import com.salts_inventory_update.controlify.RadialInventoryScreen;
import com.salts_inventory_update.controlify.NativeCursorWarpGuard;
import com.salts_inventory_update.controlify.WorkspaceSearchKeyboard;
import com.salts_inventory_update.api.client.desktop.DesktopWorkspaceWindow;
import com.salts_inventory_update.api.client.desktop.DesktopClientApi;
import com.salts_inventory_update.api.client.desktop.DesktopWindowHandle;
import net.minecraft.network.chat.Component;
import com.salts_inventory_update.client.DesktopContainerClient;
import com.salts_inventory_update.client.DesktopContainerSession;
import com.salts_inventory_update.client.InventoryDesktopScreen;
import com.salts_inventory_update.client.SaltsInventoryConfig;
import com.salts_inventory_update.controlify.model.RadialWorkspace;
import com.salts_inventory_update.controlify.model.WorkspaceLayout;
import com.salts_inventory_update.controlify.model.RadialLayout;
import com.salts_inventory_update.controlify.model.WindowSize;
import com.salts_inventory_update.controlify.input.ControlifyInputBridge;
import dev.isxander.controlify.Controlify;
import dev.isxander.controlify.InputMode;
import dev.isxander.controlify.api.event.ControlifyEvents;
import dev.isxander.controlify.api.vmousesnapping.SnapPoint;
import dev.isxander.controlify.bindings.ControlifyBindings;
import dev.isxander.controlify.bindings.input.ButtonInput;
import dev.isxander.controlify.controller.ControllerEntity;
import dev.isxander.controlify.controller.id.ControllerType;
import dev.isxander.controlify.controller.impl.ControllerStateImpl;
import dev.isxander.controlify.controller.info.*;
import dev.isxander.controlify.controller.input.GamepadInputs;
import dev.isxander.controlify.controller.input.InputComponent;
import dev.isxander.controlify.controllermanager.UniqueControllerID;
import dev.isxander.controlify.driver.Driver;
import dev.isxander.controlify.screenop.ScreenProcessorProvider;
import dev.isxander.controlify.utils.log.ControlifyLogger;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.Difficulty;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.FurnaceMenu;
import net.minecraft.world.inventory.LoomMenu;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import com.salts_inventory_update.server.DesktopContainerSessions;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import org.joml.Vector2d;
import org.slf4j.LoggerFactory;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * QA-only integrated-server scenario. A synthetic Driver is attached to the actual Controlify
 * manager; Controlify itself samples the state, updates bindings and drives the real screen.
 * Reflection accesses test observations and attaches that Driver. No production test hooks exist.
 */
public final class ControlifyGameplayHarness {
    private record Step(String name, Runnable start, BooleanSupplier ready, Runnable verify, int settleTicks) { }
    private record QaId(String value) implements UniqueControllerID { }
    private record SessionIdentity(DesktopContainerSession object, long token) { }
    private static final ArrayDeque<Step> steps = new ArrayDeque<>();
    private static final List<Map<String, Object>> results = new ArrayList<>();
    private static final TestDriver driver = new TestDriver();
    private static Minecraft mc;
    private static QaWorkspaceOwner owner;
    private static ControllerEntity controller;
    private static Step active;
    private static CompletableFuture<?> serverTask;
    private static CompletableFuture<Void> nativeControllerInit;
    private static int settle = -1;
    private static long started, stepStarted;
    private static boolean finished;
    private static Path output;
    private static BlockPos chestPos, barrelPos, tablePos, furnacePos, loomPos;
    private static UUID chestWindow, barrelWindow, previousSelected;
    private static UUID tableWindow, furnaceWindow, loomWindow;
    private static Set<UUID> preservedWindows;
    private static Map<Integer, SessionIdentity> preservedSessions;
    private static net.minecraft.world.phys.Vec3 positionBefore;
    private static long heldAt, firstClosedAt;
    private static int furnaceXpAfterSession;
    private static int cursorBeforeX;
    private static final int[] GEAR_RECIPE_INPUTS = {1, 3, 4, 5, 6, 7, 8, 9};
    private static final ResourceLocation QA_VIEW_ID = new ResourceLocation("salts_inventory_controlify_qa", "client_view");
    private static final QaClientView.Model viewA = new QaClientView.Model(), viewB = new QaClientView.Model();
    private static DesktopWindowHandle viewAHandle, viewBHandle;
    private static Runnable pointerFixture;
    private static QaClientView.Model parityView;
    private static double probeMouseX,probeMouseY;
    private static boolean probeMousePending,probeMouseForwarded;

    private ControlifyGameplayHarness() { }
    public static boolean tracingKeyboard(){return active!=null&&active.name().contains("keyboard");}
    public static boolean tracingCursorWarp(){return active!=null&&active.name().startsWith("parity-api-");}
    public static boolean tracingRecipeWheel(){return active!=null&&(active.name().contains("wheel")||active.name().contains("cooking"));}
    public static void observeNativeMouseMotion(long window,double x,double y){
        if(probeMousePending&&window==mc.getWindow().getWindow()
            &&Math.abs(x-probeMouseX)<1&&Math.abs(y-probeMouseY)<1
            &&!Controlify.instance().virtualMouseHandler().isVirtualMouseEnabled())probeMouseForwarded=true;
    }

    public static void initialize() {
        seedQaControlifySettings();
        output = Path.of(System.getProperty("salts_inventory_controlify.qaOutput", "controlify-qa")).toAbsolutePath();
        try {
            Files.createDirectories(output.resolve("screenshots"));
            Files.deleteIfExists(output.resolve("results.json"));
            try (var previousCaptures = Files.list(output.resolve("screenshots"))) {
                for (Path capture : previousCaptures.filter(path -> path.getFileName().toString().endsWith(".png")).toList()) Files.delete(capture);
            }
        }
        catch (Exception failure) { throw new IllegalStateException(failure); }
        started = System.currentTimeMillis();
        DesktopClientApi.registerView(QA_VIEW_ID, QaClientView.Model.class, new QaClientView());
        ControlifyEvents.INPUT_MODE_CHANGED.register(event -> {
            System.out.println("CONTROLIFY_QA_INPUT_MODE " + event.mode() + " step=" + (active == null ? "between" : active.name()));
            if (controller != null && !event.mode().isController()) new Throwable("QA input-mode change source").printStackTrace();
        });
        ControlifyEvents.CONTROLLER_STATE_UPDATE.register(event->traceKeyboardController("before",event.controller()));
        ControlifyEvents.CONTROLLER_STATE_UPDATE.register(event->{
            if(pointerFixture==null||event.controller()!=controller||!(mc.screen instanceof RadialInventoryScreen screen))return;
            var input=controller.input().orElseThrow();
            var binding=com.salts_inventory_update.controlify.ContextualRecipeBinding.OPEN_WHEEL.on(controller);
            float threshold=input.config().config().buttonActivationThreshold;
            if(binding.boundInput().state(input.stateNow())<=threshold||binding.boundInput().state(input.stateThen())>threshold)return;
            Runnable prepare=pointerFixture;pointerFixture=null;prepare.run();
            System.out.println("CONTROLIFY_QA_CONTEXT_POINTER canOpen="+screen.controllerCanOpenContextual()+" cursor="+Controlify.instance().virtualMouseHandler().getCurrentX(1f)+","+Controlify.instance().virtualMouseHandler().getCurrentY(1f));
        });
        ControlifyEvents.ACTIVE_CONTROLLER_TICKED.register(event->traceKeyboardController("after",event.controller()));
        ControlifyEvents.VIRTUAL_MOUSE_TOGGLED.register(event->{
            if(!tracingCursorWarp())return;
            System.out.println("CONTROLIFY_QA_WARP_VM "+event+" knownEcho="+NativeCursorWarpGuard.knownWarpCallback());
            new Throwable("QA actual native cursor toggle source").printStackTrace();
        });
        scenarios();
        QaNativePointerFixture.register(inputController -> {
            if (pointerFixture == null || inputController != controller) return;
            if (!ControlifyBindings.INV_QUICK_MOVE.on(inputController).justPressed()
                    && !ControlifyBindings.INV_SELECT.on(inputController).justPressed()
                    && !ControlifyBindings.VMOUSE_LCLICK.on(inputController).justPressed()) return;
            Runnable preparePointer = pointerFixture;
            pointerFixture = null;
            preparePointer.run();
        });
        QaRuntime.registerTick(ControlifyGameplayHarness::tick);
        System.out.println("CONTROLIFY_QA_START output=" + output);
    }

    private static void scenarios() {
        add("disposable-flat-world", ControlifyGameplayHarness::createWorld,
                () -> mc.player != null && mc.level != null && mc.getSingleplayerServer() != null, () -> {}, 35);
        var firstRunHost = new com.salts_inventory_update.api.client.desktop.DesktopWorkspaceHost[1];
        add("first-world-controller-onboarding", () -> {}, () -> true, () -> {
            check("first-world-alternative-host-installed", DesktopClientApi.workspaceHost() != null);
            check("first-world-default-tutorial-not-opened", DesktopClientApi.currentWorkspace()
                    .filter(com.salts_inventory_update.api.client.desktop.DesktopWorkspace::isNativeScreenActive).isEmpty());
            check("first-world-tutorial-preference-preserved", !SaltsInventoryConfig.get().hasSeenInstructionsWindow);
            capture("first-world-without-desktop-tutorial");
        }, 20);
        add("first-world-default-help-without-host", () -> {
            firstRunHost[0] = DesktopClientApi.workspaceHost();
            DesktopClientApi.setWorkspaceHost(null);
        }, () -> SaltsInventoryConfig.get().hasSeenInstructionsWindow && DesktopClientApi.currentWorkspace()
                .filter(com.salts_inventory_update.api.client.desktop.DesktopWorkspace::isNativeScreenActive).isPresent(), () -> {
            check("default-tutorial-still-opens-without-alternative-host", true);
            DesktopClientApi.currentWorkspace().ifPresent(com.salts_inventory_update.api.client.desktop.DesktopWorkspace::closeClosableWindows);
            mc.setScreen(null);
            DesktopClientApi.setWorkspaceHost(firstRunHost[0]);
            SaltsInventoryConfig.update(config -> config.hasSeenInstructionsWindow = false);
        }, 4);
        add("first-world-manual-help-with-host",
                com.salts_inventory_update.client.WindowedInventoryClient::scheduleInstructionsWindowOpen,
                () -> SaltsInventoryConfig.get().hasSeenInstructionsWindow && DesktopClientApi.currentWorkspace()
                        .filter(com.salts_inventory_update.api.client.desktop.DesktopWorkspace::isNativeScreenActive).isPresent(), () -> {
            check("manual-help-still-opens-with-alternative-host", true);
            capture("manual-desktop-help-remains-available");
            DesktopClientApi.currentWorkspace().ifPresent(com.salts_inventory_update.api.client.desktop.DesktopWorkspace::closeClosableWindows);
            mc.setScreen(null);
        }, 4);
        add("native-controlify-manager-readiness", () -> {
            Controlify controlify = Controlify.instance();
            var globals = controlify.config().globalSettings();
            // Controlify 2.0.4 quiet mode probes physical devices before creating its manager.
            // Complete its supported setup path for this synthetic-driver fixture, choosing
            // the normal GLFW backend rather than vibration-native onboarding.
            globals.quietMode = false;
            globals.vibrationOnboarded = true;
            globals.loadVibrationNatives = false;
            nativeControllerInit = controlify.finishControlifyInit();
        }, () -> {
            if (nativeControllerInit.isCompletedExceptionally()) nativeControllerInit.join();
            return nativeControllerInit.isDone() && Controlify.instance().getControllerManager().isPresent();
        }, () -> check("actual-native-controller-manager-ready", Controlify.instance().getControllerManager().isPresent()), 3);
        add("actual-controlify-driver", ControlifyGameplayHarness::installController,
                () -> Controlify.instance().getCurrentController().orElse(null) == controller,
                () -> check("controller-mode", Controlify.instance().currentInputMode().isController()), 5);
        add("salt-server-handshake", () -> {}, DesktopContainerClient::canUseServerSessions, () -> {}, 10);
        add("server-container-fixtures", () -> server(() -> {
            ServerPlayer player = player();
            chestPos = player.blockPosition().offset(2, 0, 0);
            barrelPos = chestPos.offset(2, 0, 0);
            tablePos = chestPos.offset(0, 0, 2); furnacePos = chestPos.offset(2, 0, 2); loomPos = chestPos.offset(4, 0, 2);
            var level = player.serverLevel();
            for (int x = -3; x <= 6; x++) for (int z = -3; z <= 3; z++) {
                level.setBlock(chestPos.offset(x, -1, z), Blocks.STONE.defaultBlockState(), 3);
                for (int y = 0; y < 5; y++) level.setBlock(chestPos.offset(x, y, z), Blocks.AIR.defaultBlockState(), 3);
            }
            level.setBlock(chestPos, Blocks.CHEST.defaultBlockState(), 3);
            level.setBlock(barrelPos, Blocks.BARREL.defaultBlockState(), 3);
            level.setBlock(tablePos, Blocks.CRAFTING_TABLE.defaultBlockState(), 3);
            level.setBlock(furnacePos, Blocks.FURNACE.defaultBlockState(), 3);
            level.setBlock(loomPos, Blocks.LOOM.defaultBlockState(), 3);
            ((Container) level.getBlockEntity(chestPos)).setItem(0, new ItemStack(Items.DIAMOND, 16));
            ((Container) level.getBlockEntity(barrelPos)).setItem(0, new ItemStack(Items.EMERALD, 7));
            player.getInventory().clearContent(); player.inventoryMenu.setCarried(ItemStack.EMPTY);
            mc.getSingleplayerServer().getCommands().performPrefixedCommand(
                    mc.getSingleplayerServer().createCommandSourceStack().withSuppressedOutput(), "time set noon");
            return null;
        }), ControlifyGameplayHarness::serverDone, () -> {}, 10);
        if(java.util.Set.of("feature-parity", "playtest-adapters").contains(System.getProperty("salts_inventory_controlify.qaScope", ""))){
            add("parity-native-bindings-fixture",()->{
                ControlifyBindings.INV_QUICK_MOVE.on(controller).setBoundInput(new ButtonInput(GamepadInputs.MISC_1_BUTTON));
                ControlifyBindings.GUI_BACK.on(controller).setBoundInput(new ButtonInput(GamepadInputs.EAST_BUTTON));
            },()->true,()->{},2);
            addFeatureParityScenarios();
            return;
        }
        button("initial-Y-opens-player-workspace", GamepadInputs.NORTH_BUTTON, () -> {
            check("ordinary-inventory-uses-controller-workspace", owner.workspaceActive() && mc.screen instanceof RadialInventoryScreen);
            check("initial-player-and-character-only", owner.windows().values().stream().noneMatch(w -> w.kind() == DesktopWorkspaceWindow.Kind.OTHER));
            check("actual-workspace-input-processor", ScreenProcessorProvider.provide(mc.screen).getClass().getSimpleName().equals("WorkspaceScreenProcessor"));
        });
        openContainer("open-chest-default-pair", () -> chestPos, () -> {
            chestWindow = newestOther();
            check("inventory-focused-by-default", owner.model().focused().equals(owner.model().playerInventory()));
            check("new-chest-selected", owner.model().selected().equals(chestWindow));
            check("mandatory-inventory-character", owner.windows().values().stream().filter(w -> w.kind() != DesktopWorkspaceWindow.Kind.OTHER).count() == 2);
            check("forced-hud-offhand", (Boolean) field(InventoryDesktopScreen.current(mc), "workspaceHud"));
            capture("01-side-by-side-default");
        });
        add("native-left-stick-cursor", () -> {
            pointAtItem(chestWindow, Items.DIAMOND);
            cursorBeforeX = Controlify.instance().virtualMouseHandler().getCurrentX(1f);
            driver.stick(0.65f, 0);
        }, () -> true, () -> {
            driver.stick(0, 0);
            check("stock-cursor-moved-right", Controlify.instance().virtualMouseHandler().getCurrentX(1f) > cursorBeforeX + 1);
        }, 5);
        add("native-cursor-stick-release-snap", () -> {}, () -> true,
                () -> cursorBeforeX = Controlify.instance().virtualMouseHandler().getCurrentX(1f), 4);
        button("native-DPAD-snap-right", GamepadInputs.DPAD_RIGHT_BUTTON,
                () -> check("stock-DPAD-transformed-slot-snap", Controlify.instance().virtualMouseHandler().getCurrentX(1f) > cursorBeforeX));
        add("side-mode-blocks-movement", () -> {
            positionBefore = mc.player.position(); driver.walk = 1;
        }, () -> true, () -> {
            driver.walk = 0;
            check("no-world-movement-side", mc.player.position().distanceTo(positionBefore) < 0.05);
        }, 12);
        add("configured-native-quickmove", () -> {
            ControlifyBindings.INV_QUICK_MOVE.on(controller).setBoundInput(new ButtonInput(GamepadInputs.MISC_1_BUTTON));
            pointAtItem(chestWindow, Items.DIAMOND);
            pointerFixture = () -> pointAtItem(chestWindow, Items.DIAMOND);
            driver.buttons.add(GamepadInputs.MISC_1_BUTTON);
        }, () -> true, () -> { driver.buttons.clear(); }, 3);
        assertServer("quickmove-chest-to-player-exact", () ->
                count(chestPos, Items.DIAMOND) == 0 && inventoryCount(Items.DIAMOND) == 16
                        && count(barrelPos, Items.DIAMOND) == 0);
        add("enter-radial-highlight", () -> { driver.trigger = 1; aimWindow(owner.model().character()); },
                () -> owner.radialActive(), () -> {
                    check("stock-cursor-hidden-in-radial", ControlifyInputBridge.suppressVirtualMouseRendering());
                    check("joystick-highlight-character", owner.model().highlighted().orElseThrow().equals(owner.model().character()));
                    capture("02-radial-highlight");
                }, 6);
        add("radial-blocks-movement", () -> positionBefore = mc.player.position(), () -> true,
                () -> check("no-world-movement-radial", mc.player.position().distanceTo(positionBefore) < 0.05), 6);
        button("swap-focus-with-A", GamepadInputs.SOUTH_BUTTON, () -> {
            check("character-now-focused", owner.model().focused().equals(owner.model().character()));
            check("inventory-still-registered", owner.windows().containsKey(owner.model().playerInventory()));
            capture("03-swapped-focus-radial");
        });
        add("release-LT-commits-highlight", () -> { driver.trigger = 0; driver.stick(0, 0); },
                () -> !owner.radialActive(), () -> {
                    check("stock-cursor-restored-on-release", Controlify.instance().virtualMouseHandler().isVirtualMouseEnabled());
                    check("release-keeps-controller-workspace", Controlify.instance().currentInputMode().isController()
                            && mc.screen instanceof RadialInventoryScreen && owner.workspaceActive());
                    capture("04-swapped-side-by-side");
                }, 6);
        add("remember-selected-before-bumper", () -> previousSelected = owner.model().selected(), () -> true, () -> {}, 0);
        button("RB-clockwise-selected", GamepadInputs.RIGHT_SHOULDER_BUTTON,
                () -> check("RB-changed-only-selected", !owner.model().selected().equals(previousSelected)
                        && owner.model().focused().equals(owner.model().character())));
        button("LB-counterclockwise-selected", GamepadInputs.LEFT_SHOULDER_BUTTON,
                () -> check("LB-reverses-selection", owner.model().selected().equals(previousSelected)));
        add("highlight-player-protected", () -> { driver.trigger = 1; aimWindow(owner.model().playerInventory()); },
                () -> owner.radialActive(), () -> {}, 3);
        button("B-keeps-player-inventory", GamepadInputs.EAST_BUTTON,
                () -> check("inventory-cannot-close", owner.windows().containsKey(owner.model().playerInventory())));
        button("swap-player-back-to-center", GamepadInputs.SOUTH_BUTTON,
                () -> check("inventory-focused-again", owner.model().focused().equals(owner.model().playerInventory())));
        add("highlight-character-protected", () -> aimWindow(owner.model().character()), () -> true, () -> {}, 3);
        button("B-keeps-character", GamepadInputs.EAST_BUTTON,
                () -> check("character-cannot-close", owner.windows().containsKey(owner.model().character())));
        add("tuck-bottom-option", () -> driver.stick(0, 1), () -> owner.model().tuckHighlighted(),
                () -> capture("05-tuck-option-highlight"), 3);
        add("tuck-keeps-lifetimes", () -> {
            preservedWindows = Set.copyOf(owner.windows().keySet()); preservedSessions = sessions();
            driver.trigger = 0; driver.stick(0, 0);
        }, () -> owner.tuckedAway() && mc.screen == null, () -> {
            verifyPreserved(); capture("06-tucked-gameplay");
        }, 8);
        add("tucked-restores-movement", () -> {
            positionBefore = mc.player.position(); driver.walk = 1;
        }, () -> true, () -> {
            driver.walk = 0;
            check("actual-world-movement-tucked", mc.player.position().distanceTo(positionBefore) > 0.1);
        }, 8);
        add("place-player-for-real-world-USE", () -> server(() -> {
            player().teleportTo(barrelPos.getX() + 0.5, barrelPos.getY(), barrelPos.getZ() - 2.0);
            return null;
        }), ControlifyGameplayHarness::serverDone, () -> {
            driver.useTarget = barrelPos;
            aimCameraAt(barrelPos);
        }, 8);
        add("settle-real-camera-ray", () -> {}, () -> cameraTargets(barrelPos), () -> {
            check("camera-targets-real-barrel", mc.hitResult instanceof BlockHitResult block && block.getBlockPos().equals(barrelPos));
        }, 8);
        add("native-USE-barrel-while-tucked-press", () -> {
            driver.useTarget = barrelPos; driver.trigger = 1;
        }, () -> true, () -> record("native-USE-state", "OBSERVED", "mode=" + Controlify.instance().currentInputMode()
                + ",tucked=" + owner.tuckedAway() + ",use=" + ControlifyBindings.USE.on(controller).digitalNow()
                + ",key=" + mc.options.keyUse.isDown() + ",hit=" + mc.hitResult), 2);
        add("native-USE-barrel-open-while-tucked", () -> {
            driver.useTarget = null; driver.trigger = 0;
        }, () -> owner.windows().size() > preservedWindows.size(), () -> {
            barrelWindow = newestOther();
            check("new-window-keeps-gameplay", owner.tuckedAway() && mc.screen == null);
            verifyPreserved();
            check("barrel-added-existing-workspace", !preservedWindows.contains(barrelWindow) && owner.windows().containsKey(barrelWindow));
            preservedWindows = Set.copyOf(owner.windows().keySet()); preservedSessions = sessions();
        }, 12);
        button("Y-restores-same-workspace", GamepadInputs.NORTH_BUTTON, () -> {
            check("restored-side-mode", !owner.tuckedAway() && !owner.radialActive() && mc.screen instanceof RadialInventoryScreen);
            verifyPreserved(); capture("07-restored-existing-windows");
        });
        add("seed-chest-second-transfer", () -> server(() -> {
            ((Container) player().serverLevel().getBlockEntity(chestPos)).setItem(0, new ItemStack(Items.DIAMOND, 32));
            return null;
        }), ControlifyGameplayHarness::serverDone, () -> {}, 10);
        add("radial-focus-chest", () -> { driver.trigger = 1; aimWindow(chestWindow); }, () -> owner.radialActive(), () -> {}, 3);
        button("A-focus-chest", GamepadInputs.SOUTH_BUTTON, () -> check("chest-focused", owner.model().focused().equals(chestWindow)));
        add("select-barrel-by-direction", () -> aimWindow(barrelWindow), () -> true, () -> {}, 3);
        add("commit-chest-barrel-pair", () -> { driver.trigger = 0; driver.stick(0, 0); }, () -> !owner.radialActive(),
                () -> {
                    check("two-container-pair", owner.model().focused().equals(chestWindow) && owner.model().selected().equals(barrelWindow));
                    capture("08-two-containers-pair");
                }, 8);
        add("native-quickmove-to-displayed-barrel", () -> {
            pointAtItem(chestWindow, Items.DIAMOND);
            pointerFixture = () -> pointAtItem(chestWindow, Items.DIAMOND);
            driver.buttons.add(GamepadInputs.MISC_1_BUTTON);
        }, () -> true, () -> driver.buttons.clear(), 3);
        assertServer("server-exact-pair-and-conservation", () -> count(chestPos, Items.DIAMOND) == 0
                && count(barrelPos, Items.DIAMOND) == 32 && inventoryCount(Items.DIAMOND) == 16);
        add("disable-animations-config", () -> {
            try { owner.config().setAnimationsEnabled(false); } catch (Exception failure) { throw new IllegalStateException(failure); }
            driver.trigger = 1; aimWindow(barrelWindow);
        }, () -> owner.radialActive(), () -> {
            WorkspaceLayout<UUID> current = owner.animatedLayout(mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
            Map<UUID, WindowSize> sizes = new LinkedHashMap<>();
            owner.windows().forEach((id, window) -> sizes.put(id,
                    new WindowSize(owner.desktop().presentationBounds(id).width(), owner.desktop().presentationBounds(id).height())));
            WorkspaceLayout<UUID> target = new com.salts_inventory_update.controlify.model.WorkspacePresentationLayout().compute(owner.model(), sizes,
                    mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight(), 1);
            check("disabled-animation-immediate-target", current.equals(target));
            capture("09-animations-disabled-radial");
        }, 1);
        button("B-closes-highlighted-barrel", GamepadInputs.EAST_BUTTON,
                () -> check("only-highlighted-window-closed", !owner.windows().containsKey(barrelWindow)
                        && owner.windows().containsKey(chestWindow) && owner.windows().containsKey(owner.model().playerInventory())));
        add("release-radial-before-hold", () -> { driver.trigger = 0; driver.stick(0, 0); }, () -> !owner.radialActive(), () -> {}, 2);
        add("configured-close-SOUTH-owns-A", () -> {
            ControlifyBindings.GUI_BACK.on(controller).setBoundInput(new ButtonInput(GamepadInputs.SOUTH_BUTTON));
            driver.trigger = 1; aimWindow(owner.model().playerInventory());
        }, () -> owner.radialActive(), () -> {}, 2);
        add("protected-close-press-in-radial", () -> {
            heldAt=System.nanoTime();driver.buttons.add(GamepadInputs.SOUTH_BUTTON);
        }, () -> System.nanoTime()-heldAt > 120_000_000L, () -> {
            check("radial-protected-close-keeps-workspace",owner.workspaceActive() && owner.radialActive()
                && owner.windows().containsKey(chestWindow));driver.buttons.clear();driver.trigger=0;driver.stick(0,0);
        }, 3);
        add("return-to-side-before-immediate-close",() -> {},() -> !owner.radialActive(),() -> {},4);
        add("side-close-all-immediate", () -> {
            heldAt=System.nanoTime();firstClosedAt=0;driver.buttons.add(GamepadInputs.SOUTH_BUTTON);
        }, ControlifyGameplayHarness::observeClosed, () -> {
            driver.buttons.clear();driver.trigger=0;driver.stick(0,0);
            check("side-close-without-hold",(firstClosedAt-heldAt)/1_000_000 < owner.closeHoldDurationMillis());
            check("close-all-keeps-only-permanent",owner.windows().values().stream().noneMatch(w -> w.kind()==DesktopWorkspaceWindow.Kind.OTHER));
            check("close-all-server-sessions-disposed",sessions().values().stream().noneMatch(i -> i.object().title().getString().equals("Chest")));
            capture("10-close-all-gameplay");
        }, 3);
        openContainer("reopen-chest-for-side-hold", () -> chestPos, () -> check("reopen-default-focus", owner.model().focused().equals(owner.model().playerInventory())));
        add("hold-close-all-side", () -> {
            heldAt = System.nanoTime(); firstClosedAt = 0; driver.buttons.add(GamepadInputs.SOUTH_BUTTON);
        }, ControlifyGameplayHarness::observeClosed, () -> {
            driver.buttons.clear();
            check("side-second-close-immediate", (firstClosedAt - heldAt) / 1_000_000 < owner.closeHoldDurationMillis());
            check("side-hold-returned-gameplay", mc.screen == null);
        }, 4);
        button("Y-opens-workspace-after-close-all", GamepadInputs.NORTH_BUTTON,
                () -> check("ordinary-Y-after-close", owner.workspaceActive() && mc.screen instanceof RadialInventoryScreen));
        addNativeResultScenarios();
        addFeatureParityScenarios();
    }

    private static void traceKeyboardController(String phase,ControllerEntity inputController){
        if(inputController!=controller)return;
        var input=inputController.input().orElseThrow();var back=ControlifyBindings.GUI_BACK.on(inputController);
        if(active!=null&&active.name().startsWith("parity-api-")
            &&(input.stateNow().isButtonDown(GamepadInputs.SOUTH_BUTTON)||input.stateThen().isButtonDown(GamepadInputs.SOUTH_BUTTON))){
            var mouse=Controlify.instance().virtualMouseHandler();
            System.out.println("CONTROLIFY_QA_NATIVE_A "+phase+" step="+active.name()+" "+controllerState()
                +" vmouse="+mouse.isVirtualMouseEnabled()+" required="+mouse.requiresVirtualMouse()
                +" select="+ControlifyBindings.INV_SELECT.on(inputController).justPressed()
                +" click="+ControlifyBindings.VMOUSE_LCLICK.on(inputController).justPressed()
                +" pointerPending="+(pointerFixture!=null)+" virtualClicks="+(parityView==null?-1:parityView.virtualClicks)
                +" keyboard="+WorkspaceSearchKeyboard.active()+" waiting="+WorkspaceSearchKeyboard.waitingNeutral());
        }
        if(!tracingKeyboard())return;
        if(back.boundInput().state(input.stateNow())==0&&back.boundInput().state(input.stateThen())==0)return;
        System.out.println("CONTROLIFY_QA_KEYBOARD_INPUT "+phase+" "+controllerState()+" active="+owner.workspaceActive()
            +" keyboard="+com.salts_inventory_update.controlify.WorkspaceSearchKeyboard.active()
            +" waiting="+com.salts_inventory_update.controlify.WorkspaceSearchKeyboard.waitingNeutral()
            +" vmouse="+Controlify.instance().virtualMouseHandler().isVirtualMouseEnabled()
            +" back="+back.justPressed()+" release="+back.guiPressed().get());
    }

    private static void addNativeResultScenarios() {
        // Clear the close/select overlap for the remaining native result-slot checks.
        add("restore-default-close-for-results", () ->
                ControlifyBindings.GUI_BACK.on(controller).setBoundInput(new ButtonInput(GamepadInputs.EAST_BUTTON)), () -> true, () -> {}, 2);
        openContainer("open-crafting-result", () -> tablePos, () -> tableWindow = newestOther());
        add("seed-authenticated-crafting-recipe", () -> server(() -> {
            AbstractContainerMenu menu = serverMenu(CraftingMenu.class);
            // Character includes its crafting grid and offhand. Fill these real destinations so
            // the result must enter the chest equipment slot, and non-equipment output is rejected.
            for (int slot : new int[]{1, 2, 3, 4, 45}) player().inventoryMenu.getSlot(slot).set(new ItemStack(Items.BEDROCK, 64));
            player().inventoryMenu.broadcastChanges();
            for (int slot : GEAR_RECIPE_INPUTS) menu.getSlot(slot).set(new ItemStack(Items.IRON_INGOT, 2));
            menu.broadcastChanges();
            return menu.getSlot(0).getItem().is(Items.IRON_CHESTPLATE);
        }), ControlifyGameplayHarness::serverDone, () -> check("real-recipe-result-created", (Boolean) serverTask.join()), 10);
        focusPair("crafting-to-character", () -> tableWindow, () -> owner.model().character());
        quickMove("crafting-equipment-result", () -> tableWindow, Items.IRON_CHESTPLATE);
        assertServer("crafting-onTake-consumes-once-and-replenishes", () -> {
            AbstractContainerMenu menu = serverMenu(CraftingMenu.class);
            return menu.getSlot(0).getItem().is(Items.IRON_CHESTPLATE)
                    && player().inventoryMenu.getSlot(6).getItem().is(Items.IRON_CHESTPLATE)
                    && inventoryCount(Items.IRON_CHESTPLATE) == 1
                    && Arrays.stream(GEAR_RECIPE_INPUTS).allMatch(slot -> menu.getSlot(slot).getItem().getCount() == 1);
        });
        quickMove("full-equipment-rejects-second-crafting-result", () -> tableWindow, Items.IRON_CHESTPLATE);
        assertServer("crafting-full-character-no-consumption-or-leak", () -> {
            AbstractContainerMenu menu = serverMenu(CraftingMenu.class);
            return inventoryCount(Items.IRON_CHESTPLATE) == 1 && menu.getSlot(0).getItem().is(Items.IRON_CHESTPLATE)
                    && Arrays.stream(GEAR_RECIPE_INPUTS).allMatch(slot -> menu.getSlot(slot).getItem().getCount() == 1);
        });
        selectPair("crafting-to-inventory", () -> owner.model().playerInventory());
        quickMove("remaining-crafting-result-to-player", () -> tableWindow, Items.IRON_CHESTPLATE);
        assertServer("crafting-native-storage-output-conserved", () -> {
            AbstractContainerMenu menu = serverMenu(CraftingMenu.class);
            return menu.getSlot(0).getItem().isEmpty() && inventoryCount(Items.IRON_CHESTPLATE) == 2
                    && Arrays.stream(GEAR_RECIPE_INPUTS).allMatch(slot -> menu.getSlot(slot).getItem().isEmpty());
        });
        add("crafting-final-capture", () -> {}, () -> true, () -> capture("11a-native-crafting-result"), 2);
        openContainer("open-furnace-result", () -> furnacePos, () -> furnaceWindow = newestOther());
        add("seed-real-furnace-result-xp-record", () -> server(() -> {
            var furnace = (AbstractFurnaceBlockEntity) player().serverLevel().getBlockEntity(furnacePos);
            furnace.setItem(2, new ItemStack(Items.IRON_INGOT, 8));
            var recipe = player().serverLevel().getRecipeManager().byKey(new ResourceLocation("iron_ingot_from_smelting_raw_iron")).orElseThrow();
            for (int i = 0; i < 8; i++) furnace.setRecipeUsed(recipe);
            serverMenu(FurnaceMenu.class).broadcastChanges(); return null;
        }), ControlifyGameplayHarness::serverDone, () -> {}, 10);
        focusPair("furnace-to-character", () -> furnaceWindow, () -> owner.model().character());
        quickMove("furnace-equipment-reject", () -> furnaceWindow, Items.IRON_INGOT);
        assertServer("furnace-rejected-output-and-xp-preserved", () -> count(furnacePos, Items.IRON_INGOT) == 8
                && inventoryCount(Items.IRON_INGOT) == 0 && player().totalExperience == 0);
        openContainer("reopen-barrel-for-result-destination", () -> barrelPos, () -> barrelWindow = newestOther());
        focusPair("furnace-to-real-container", () -> furnaceWindow, () -> barrelWindow);
        quickMove("furnace-generic-session-result-take", () -> furnaceWindow, Items.IRON_INGOT);
        add("settle-session-furnace-XP", () -> {}, () -> true, () -> {}, 55);
        assertServer("furnace-session-output-conservation-and-XP", () -> {
            furnaceXpAfterSession = player().totalExperience;
            return count(furnacePos, Items.IRON_INGOT) == 0 && count(barrelPos, Items.IRON_INGOT) == 8
                    && inventoryCount(Items.IRON_INGOT) == 0 && furnaceXpAfterSession > 0;
        });
        add("second-furnace-completed-batch", () -> server(() -> {
            var furnace = (AbstractFurnaceBlockEntity) player().serverLevel().getBlockEntity(furnacePos);
            furnace.setItem(2, new ItemStack(Items.IRON_INGOT, 8));
            var recipe = player().serverLevel().getRecipeManager().byKey(new ResourceLocation("iron_ingot_from_smelting_raw_iron")).orElseThrow();
            for (int i = 0; i < 8; i++) furnace.setRecipeUsed(recipe);
            serverMenu(FurnaceMenu.class).broadcastChanges(); return null;
        }), ControlifyGameplayHarness::serverDone, () -> {}, 10);
        selectPair("furnace-to-inventory", () -> owner.model().playerInventory());
        quickMove("furnace-native-result-take", () -> furnaceWindow, Items.IRON_INGOT);
        add("settle-furnace-experience-orbs", () -> {}, () -> true, () -> {}, 55);
        assertServer("furnace-result-conservation-and-XP", () -> count(furnacePos, Items.IRON_INGOT) == 0
                && inventoryCount(Items.IRON_INGOT) == 8 && count(barrelPos, Items.IRON_INGOT) == 8
                && player().totalExperience > furnaceXpAfterSession);
        add("final-native-result-capture", () -> {}, () -> true, () -> capture("11-native-result-inventory"), 5);
        addLoomResultScenarios();
        addClientOnlyScenarios();
    }

    private static void addLoomResultScenarios() {
        openContainer("open-native-loom", () -> loomPos, () -> loomWindow = newestOther());
        add("seed-native-loom-pattern", () -> server(() -> {
            LoomMenu menu = (LoomMenu) serverMenu(LoomMenu.class);
            menu.getSlot(0).set(new ItemStack(Items.WHITE_BANNER, 2));
            menu.getSlot(1).set(new ItemStack(Items.RED_DYE, 2));
            boolean selected = menu.clickMenuButton(player(), 0);
            menu.broadcastChanges();
            return selected && menu.getResultSlot().getItem().is(Items.WHITE_BANNER)
                    && bannerPatterns(menu.getResultSlot().getItem()) == 1;
        }), ControlifyGameplayHarness::serverDone, () -> check("loom-authentic-pattern-result", (Boolean) serverTask.join()), 10);
        focusPair("loom-to-real-container", () -> loomWindow, () -> barrelWindow);
        quickMoveSlot("loom-session-result-slot-three", () -> loomWindow, 3);
        assertServer("loom-native-inputs-consumed-once-output-replenished", () -> {
            LoomMenu menu = (LoomMenu) serverMenu(LoomMenu.class);
            return menu.getSlot(0).getItem().getCount() == 1 && menu.getSlot(1).getItem().getCount() == 1
                    && menu.getResultSlot().getItem().is(Items.WHITE_BANNER)
                    && count(barrelPos, Items.WHITE_BANNER) == 1 && inventoryCount(Items.WHITE_BANNER) == 0;
        });
        add("loom-replenished-result-capture", () -> {}, () -> true, () -> capture("11b-native-loom-replenished-result"), 3);
        selectPair("loom-to-inventory", () -> owner.model().playerInventory());
        quickMoveSlot("loom-storage-result-slot-three", () -> loomWindow, 3);
        assertServer("loom-final-output-conserved-no-extra-input-consumption", () -> {
            LoomMenu menu = (LoomMenu) serverMenu(LoomMenu.class);
            return menu.getSlot(0).getItem().isEmpty() && menu.getSlot(1).getItem().isEmpty()
                    && menu.getResultSlot().getItem().isEmpty() && count(barrelPos, Items.WHITE_BANNER) == 1
                    && inventoryCount(Items.WHITE_BANNER) == 1 && inventoryCount(Items.RED_DYE) == 0;
        });
        add("loom-final-result-capture", () -> {}, () -> true, () -> capture("11c-native-loom-result-inventory"), 3);
    }

    private static int bannerPatterns(ItemStack stack) {
        var tag = stack.getTagElement("BlockEntityTag");
        return tag == null ? 0 : tag.getList("Patterns", 10).size();
    }

    private static void addClientOnlyScenarios() {
        add("client-only-view-default-open", () ->
                viewAHandle = DesktopClientApi.openView(QA_VIEW_ID, "active", viewA, Component.literal("QA client-only UI")).orElseThrow(),
                () -> owner.windows().containsKey(viewAHandle.instanceId()), () -> {
                    check("UI-open-default-player-focus", owner.model().focused().equals(owner.model().playerInventory())
                            && owner.model().selected().equals(viewAHandle.instanceId()));
                    check("UI-state-created-once", viewA.statesCreated == 1 && viewA.opened == 1 && viewA.closed == 0);
                    capture("12-client-only-UI-pair");
                }, 8);
        add("position-native-UI-cursor", () -> prepareUiCursor(viewAHandle, viewA, false), () -> true, () -> {}, 2);
        button("stock-VMOUSE-click-client-UI", GamepadInputs.SOUTH_BUTTON,
                () -> check("client-only-UI-button-clicked", viewA.clicks == 1));
        add("position-external-UI-cursor", () -> prepareUiCursor(viewAHandle, viewA, true), () -> true, () -> {}, 2);
        button("stock-VMOUSE-click-protruding-UI", GamepadInputs.SOUTH_BUTTON, () -> {
            check("protruding-client-UI-button-clicked", viewA.externalClicks == 1 && viewA.clicks == 1);
            capture("12b-external-client-UI-button");
        });
        add("open-native-client-UI-modal-popup", () -> viewA.modalPopup = true, () -> true, () -> {}, 6);
        add("position-modal-outside-cursor", () -> {
            pointOutsideUi(viewAHandle);
            pointerFixture = () -> pointOutsideUi(viewAHandle);
        }, () -> true, () -> {}, 2);
        button("stock-VMOUSE-dismiss-modal-popup-outside", GamepadInputs.SOUTH_BUTTON, () -> {
            check("modal-outside-PRESS-dismissed-once", !viewA.modalPopup && viewA.popupDismissals == 1
                    && viewA.clicks == 1 && viewA.externalClicks == 1);
            capture("12c-native-modal-outside-dismissal");
        });
        add("UI-tuck-option", () -> { driver.trigger = 1; driver.stick(0, 1); },
                () -> owner.model().tuckHighlighted(), () -> {}, 2);
        add("UI-tuck-preserves-state", () -> {
            preservedWindows = Set.copyOf(owner.windows().keySet()); preservedSessions = sessions();
            driver.trigger = 0; driver.stick(0, 0);
        }, () -> owner.tuckedAway() && mc.screen == null, () -> {
            verifyPreserved();
            check("UI-state-survives-tuck", viewAHandle.isOpen() && viewA.statesCreated == 1
                    && viewA.clicks == 1 && viewA.externalClicks == 1 && viewA.closed == 0);
        }, 6);
        add("client-only-view-open-while-tucked", () ->
                viewBHandle = DesktopClientApi.openView(QA_VIEW_ID, "tucked", viewB, Component.literal("QA UI added while tucked")).orElseThrow(),
                () -> owner.windows().containsKey(viewBHandle.instanceId()), () -> {
                    check("UI-open-does-not-restore-tucked", owner.tuckedAway() && mc.screen == null);
                    verifyPreserved();
                    check("both-client-UI-lifetimes-alive", viewAHandle.isOpen() && viewBHandle.isOpen()
                            && viewA.statesCreated == 1 && viewB.statesCreated == 1 && viewA.closed == 0 && viewB.closed == 0);
                    preservedWindows = Set.copyOf(owner.windows().keySet()); preservedSessions = sessions();
                }, 6);
        button("restore-tucked-client-UI", GamepadInputs.NORTH_BUTTON, () -> {
            check("UI-restored-native-workspace", mc.screen instanceof RadialInventoryScreen && !owner.tuckedAway());
            verifyPreserved();
            check("UI-state-identities-not-recreated", viewA.statesCreated == 1 && viewB.statesCreated == 1 && viewA.clicks == 1);
        });
        selectPair("select-new-tucked-UI", () -> viewBHandle.instanceId());
        add("position-new-UI-button", () -> prepareUiCursor(viewBHandle, viewB, false), () -> true, () -> {}, 2);
        button("stock-VMOUSE-click-new-tucked-UI", GamepadInputs.SOUTH_BUTTON,
                () -> check("new-UI-click-is-isolated", viewB.clicks == 1 && viewA.clicks == 1));
        add("highlight-client-UI-for-close", () -> { driver.trigger = 1; aimWindow(viewBHandle.instanceId()); },
                () -> owner.radialActive(), () -> {}, 2);
        button("close-client-only-UI-with-B", GamepadInputs.EAST_BUTTON,
                () -> check("client-UI-close-callback-once", !viewBHandle.isOpen() && viewB.closed == 1 && viewA.closed == 0));
        add("finish-client-UI-workspace", () -> { driver.trigger = 0; driver.stick(0, 0); }, () -> !owner.radialActive(),
                () -> capture("13-client-only-UI-after-close"), 5);
    }

    private static void prepareUiCursor(DesktopWindowHandle handle, QaClientView.Model model, boolean external) {
        pointAtUiButton(handle, model, external);
        pointerFixture = () -> pointAtUiButton(handle, model, external);
    }
    private static void pointOutsideUi(DesktopWindowHandle handle) {
        var bounds = handle.state();
        var placement = renderedLayout().windows().get(handle.instanceId());
        int localX = bounds.x() + bounds.width() / 2, localY = bounds.y() - 12;
        check("modal-owner-retains-outside-pointer", localY < bounds.y()
                && owner.desktop().containsPointer(handle.instanceId(), localX, localY));
        int x = (int) Math.round(placement.x() + (localX - bounds.x()) * placement.scale());
        int y = (int) Math.round(placement.y() + (localY - bounds.y()) * placement.scale());
        var scale = new Vector2d((double) mc.getWindow().getGuiScaledWidth() / mc.getWindow().getScreenWidth(),
                (double) mc.getWindow().getGuiScaledHeight() / mc.getWindow().getScreenHeight());
        Controlify.instance().virtualMouseHandler().snapToPoint(new SnapPoint(x, y, 1), scale);
        ((RadialInventoryScreen) mc.screen).mouseMoved(x, y);
        check("modal-outside-hover-has-no-item-slot", ((RadialInventoryScreen) mc.screen).controllerHoveredSlot() == null);
    }
    private static void pointAtUiButton(DesktopWindowHandle handle, QaClientView.Model model, boolean external) {
        var bounds = handle.state();
        var placement = renderedLayout().windows().get(handle.instanceId());
        int localX = (external ? model.externalX : model.buttonX) + 60;
        int localY = (external ? model.externalY : model.buttonY) + 12;
        if (external) check("external-UI-hit-outside-base", localY >= bounds.y() + bounds.height()
                && owner.desktop().containsPointer(handle.instanceId(), localX, localY));
        int x = (int) Math.round(placement.x() + (localX - bounds.x()) * placement.scale());
        int y = (int) Math.round(placement.y() + (localY - bounds.y()) * placement.scale());
        var scale = new Vector2d((double) mc.getWindow().getGuiScaledWidth() / mc.getWindow().getScreenWidth(),
                (double) mc.getWindow().getGuiScaledHeight() / mc.getWindow().getScreenHeight());
        Controlify.instance().virtualMouseHandler().snapToPoint(new SnapPoint(x, y, 1), scale);
        ((RadialInventoryScreen) mc.screen).mouseMoved(x, y);
        check("generic-UI-hover-has-no-item-slot", ((RadialInventoryScreen) mc.screen).controllerHoveredSlot() == null);
    }

    private static void focusPair(String name, Supplier<UUID> source, Supplier<UUID> target) {
        add(name + "-radial", () -> { driver.trigger = 1; aimWindow(source.get()); }, () -> owner.radialActive(), () -> {}, 3);
        button(name + "-focus-A", GamepadInputs.SOUTH_BUTTON, () -> check(name + "-source-focused", owner.model().focused().equals(source.get())));
        selectPair(name, target);
    }
    private static void selectPair(String name, Supplier<UUID> target) {
        add(name + "-highlight", () -> { driver.trigger = 1; aimWindow(target.get()); },
                () -> owner.radialActive() && owner.model().highlighted().filter(target.get()::equals).isPresent(),
                () -> check(name + "-highlighted", owner.model().highlighted().filter(target.get()::equals).isPresent()), 3);
        add(name + "-commit", () -> { driver.trigger = 0; driver.stick(0, 0); }, () -> !owner.radialActive(),
                () -> check(name + "-selected", owner.model().selected().equals(target.get())), 8);
    }
    private static void quickMove(String name, Supplier<UUID> window, Item item) {
        add(name, () -> {
            pointAtItem(window.get(), item);
            pointerFixture = () -> pointAtItem(window.get(), item);
            driver.buttons.add(GamepadInputs.MISC_1_BUTTON);
        },
                () -> true, () -> driver.buttons.clear(), 3);
        add(name + "-network-settle", () -> {}, () -> true, () -> {}, 8);
    }
    private static void quickMoveSlot(String name, Supplier<UUID> window, int slotId) {
        add(name, () -> {
            pointAtNativeSlot(window.get(), slot -> slot.index == slotId && slot.hasItem());
            pointerFixture = () -> pointAtNativeSlot(window.get(), slot -> slot.index == slotId && slot.hasItem());
            driver.buttons.add(GamepadInputs.MISC_1_BUTTON);
        }, () -> true, () -> driver.buttons.clear(), 3);
        add(name + "-network-settle", () -> {}, () -> true, () -> {}, 8);
    }
    @SuppressWarnings("unchecked")
    private static AbstractContainerMenu serverMenu(Class<? extends AbstractContainerMenu> type) {
        Object state = ((Map<net.minecraft.server.network.ServerGamePacketListenerImpl, ?>) field(DesktopContainerSessions.class, "PLAYERS"))
                .get(player().connection);
        Objects.requireNonNull(state, "Authenticated server sessions for the real player connection");
        for (Object session : ((Map<Integer, ?>) field(state, "sessions")).values()) {
            AbstractContainerMenu menu = (AbstractContainerMenu) field(session, "menu");
            if (type.isInstance(menu)) return menu;
        }
        throw new IllegalStateException("Captured server menu missing: " + type.getSimpleName());
    }

    private static void createWorld() {
        seedQaControlifySettings();
        mc.options.pauseOnLostFocus = false; mc.options.guiScale().set(2);
        mc.getWindow().setWindowed(1280, 900);
        SaltsInventoryConfig.update(config -> {
            config.enableMod = true; config.hasSeenInstructionsWindow = false; config.eHoldCloseAllSeconds = 1;
            config.returnHotbarToInventory = false; config.persistentWindows = false;
        });
        LevelSettings settings = new LevelSettings("Controller disposable QA", GameType.SURVIVAL, false,
                Difficulty.PEACEFUL, true, new GameRules(), WorldDataConfiguration.DEFAULT);
        mc.createWorldOpenFlows().createFreshLevel("salts-controller-qa-" + System.currentTimeMillis(), settings,
                new WorldOptions(424242L, false, false), registry ->
                        registry.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions());
    }

    @SuppressWarnings("unchecked")
    private static void installController() {
        owner = new QaWorkspaceOwner((SaltsControlifyClient) field(SaltsControlifyClient.class, "instance"));
        try { owner.config().setAnimationsEnabled(true); } catch (Exception failure) { throw new IllegalStateException(failure); }
        Controlify controlify = Controlify.instance();
        Object manager = controlify.getControllerManager().orElseThrow();
        var globals = controlify.config().globalSettings();
        // Supported mixed input prevents operating-system mouse events from taking ownership.
        globals.outOfFocusInput = true; globals.quietMode = false;
        controller = new ControllerEntity(new ControllerInfo(new QaId("qa-controller"), ControllerType.DEFAULT, Optional.empty()),
                driver, ControlifyLogger.createMasterLogger(LoggerFactory.getLogger("Controlify QA driver")));
        controller.genericConfig().config().showIngameGuide = false;
        controller.genericConfig().config().dontShowControllerSubmission = true;
        var inputConfig = controller.input().orElseThrow().config().config();
        inputConfig.mixedInput = true;
        inputConfig.deadzonesCalibrated = true;
        inputConfig.delayedCalibration = false;
        ((Map<String, ControllerEntity>) field(manager, "controllersByUid")).put(controller.uid(), controller);
        controlify.setCurrentController(controller, true); controlify.setInputMode(InputMode.MIXED);
        check("real-input-bindings-created", controller.input().orElseThrow().getAllBindings().size() > 20);
    }

    private static final class TestDriver implements Driver {
        final Set<ResourceLocation> buttons = new HashSet<>();
        float trigger, x, y, walk;
        float rightTrigger,rightX,rightY;
        BlockPos useTarget;

        @Override public void addComponents(ControllerEntity controller) {
            controller.setComponent(new UIDComponent("salts-controller-qa-only"));
            controller.setComponent(new GUIDComponent("salts-qa"));
            controller.setComponent(new DriverNameComponent("Disposable QA virtual controller"));
            controller.setComponent(new InputComponent(controller, 40, 12, 0, true, GamepadInputs.DEADZONE_GROUPS, "default"));
        }
        @Override public void update(ControllerEntity controller, boolean outOfFocus) {
            if (useTarget != null && mc.screen == null) {
                // Keep the genuine camera ray aimed while vanilla handles the configured USE binding.
                aimCameraAt(useTarget);
            }
            ControllerStateImpl state = new ControllerStateImpl();
            buttons.forEach(button -> state.setButton(button, true));
            state.setAxis(GamepadInputs.LEFT_TRIGGER_AXIS, trigger);
            state.setAxis(GamepadInputs.RIGHT_TRIGGER_AXIS,rightTrigger);
            state.setAxis(GamepadInputs.RIGHT_STICK_AXIS_RIGHT,Math.max(0,rightX));
            state.setAxis(GamepadInputs.RIGHT_STICK_AXIS_LEFT,Math.max(0,-rightX));
            state.setAxis(GamepadInputs.RIGHT_STICK_AXIS_DOWN,Math.max(0,rightY));
            state.setAxis(GamepadInputs.RIGHT_STICK_AXIS_UP,Math.max(0,-rightY));
            state.setAxis(GamepadInputs.LEFT_STICK_AXIS_RIGHT, Math.max(0, x));
            state.setAxis(GamepadInputs.LEFT_STICK_AXIS_LEFT, Math.max(0, -x));
            state.setAxis(GamepadInputs.LEFT_STICK_AXIS_DOWN, Math.max(0, y));
            state.setAxis(GamepadInputs.LEFT_STICK_AXIS_UP, Math.max(Math.max(0, -y), walk));
            controller.input().orElseThrow().pushState(state);
        }
        void stick(float x, float y) { this.x = x; this.y = y; }
    }

    @SuppressWarnings("unchecked")
    private static WorkspaceLayout<UUID> renderedLayout() {
        return Objects.requireNonNull((WorkspaceLayout<UUID>) field(mc.screen, "placements"), "Rendered controller layout");
    }

    private static boolean cameraTargets(BlockPos target) {
        return mc.hitResult instanceof BlockHitResult block && block.getBlockPos().equals(target);
    }
    private static void aimCameraAt(BlockPos target) {
        if (mc.player == null) return;
        Vec3 direction = Vec3.atCenterOf(target).subtract(mc.player.getEyePosition());
        mc.player.setYRot((float) Math.toDegrees(Math.atan2(-direction.x, direction.z)));
        mc.player.setXRot((float) -Math.toDegrees(Math.atan2(direction.y, Math.hypot(direction.x, direction.z))));
    }

    private static void aimWindow(UUID id) {
        List<UUID> order = owner.model().radialOrder();
        double angle = RadialWorkspace.radialAngle(order.indexOf(id), order.size());
        driver.stick((float) Math.cos(angle), (float) Math.sin(angle));
    }

    private static void pointAtItem(UUID id, Item item) {
        pointAtNativeSlot(id, slot -> slot.getItem().is(item));
    }
    private static void pointAtNativeSlot(UUID id, java.util.function.Predicate<Slot> target) {
        var window = owner.windows().get(id);
        var bounds = owner.desktop().presentationBounds(id);
        var placement = renderedLayout().windows().get(id);
        for (var hit : owner.desktop().visibleSlots(id)) {
                int x = hit.x() + 8, y = hit.y() + 8;
                Slot slot = owner.desktop().hoveredSlot(id, x, y);
                if (slot != null && target.test(slot)) {
                    int px = (int) Math.round(placement.x() + (x - bounds.x()) * placement.scale());
                    int py = (int) Math.round(placement.y() + (y - bounds.y()) * placement.scale());
                    double scaleX = (double) mc.getWindow().getGuiScaledWidth() / mc.getWindow().getScreenWidth();
                    double scaleY = (double) mc.getWindow().getGuiScaledHeight() / mc.getWindow().getScreenHeight();
                    Controlify.instance().virtualMouseHandler().snapToPoint(new SnapPoint(px, py, 1), new Vector2d(scaleX, scaleY));
                    ((RadialInventoryScreen) mc.screen).mouseMoved(px, py);
                    check("real-hovered-item-slot", ((RadialInventoryScreen) mc.screen).controllerHoveredSlot() != null);
                    return;
                }
            }
        throw new IllegalStateException("Visible native slot target missing in " + id);
    }

    private static void button(String name, ResourceLocation button, Runnable verify) {
        add(name + "-press", () -> {
            // An authored pointer fixture prepares the next click, but a D-pad move
            // deliberately establishes a new native cursor position before that click.
            if(button.equals(GamepadInputs.DPAD_UP_BUTTON)||button.equals(GamepadInputs.DPAD_RIGHT_BUTTON)
                    ||button.equals(GamepadInputs.DPAD_DOWN_BUTTON)||button.equals(GamepadInputs.DPAD_LEFT_BUTTON))pointerFixture=null;
            driver.buttons.add(button);
        }, () -> true, () -> {}, 2);
        add(name, () -> driver.buttons.remove(button), () -> true, verify, 3);
    }

    private static void openContainer(String name, Supplier<BlockPos> pos, Runnable verify) {
        add(name, () -> server(() -> {
            ServerPlayer player = player();
            var level = player.serverLevel();
            player.openMenu(Objects.requireNonNull(level.getBlockState(pos.get()).getMenuProvider(level, pos.get()))); return null;
        }), () -> serverDone() && owner.workspaceActive() && owner.windows().values().stream()
                .anyMatch(w -> w.kind() == DesktopWorkspaceWindow.Kind.OTHER), verify, 12);
    }

    private static UUID newestOther() {
        UUID result = null;
        for (var window : owner.windows().values()) if (window.kind() == DesktopWorkspaceWindow.Kind.OTHER) result = window.handle().instanceId();
        return Objects.requireNonNull(result, "Container window");
    }

    @SuppressWarnings("unchecked")
    private static Map<Integer, SessionIdentity> sessions() {
        InventoryDesktopScreen desktop = InventoryDesktopScreen.current(mc);
        if (desktop == null) return Map.of();
        Map<Integer, SessionIdentity> result = new LinkedHashMap<>();
        Map<Integer, Long> tokens = (Map<Integer, Long>) field(DesktopContainerClient.class, "SESSION_TOKENS");
        for (DesktopContainerSession session : (List<DesktopContainerSession>) field(desktop, "sessions")) {
            long token = tokens.getOrDefault(session.sessionId(), 0L);
            if (token == 0L) throw new IllegalStateException("Live session has no client authorization: " + session.sessionId());
            result.put(session.sessionId(), new SessionIdentity(session, token));
        }
        return result;
    }

    private static void seedQaControlifySettings() {
        // Supported settings in the disposable QA profile: the real GLFW manager can sample
        // the synthetic driver without downloading SDL or presenting native onboarding.
        var config = Controlify.instance().config();
        var globals = config.globalSettings();
        globals.vibrationOnboarded = true;
        globals.loadVibrationNatives = false;
        globals.outOfFocusInput = true;
        globals.quietMode = true;
        config.setDirty();
        config.save();
    }

    private static void verifyPreserved() {
        check("window-lifetimes-preserved", owner.windows().keySet().containsAll(preservedWindows));
        Map<Integer, SessionIdentity> current = sessions();
        for (var entry : preservedSessions.entrySet()) {
            SessionIdentity actual = current.get(entry.getKey());
            check("authenticated-session-preserved-" + entry.getKey(), actual != null
                    && actual.object() == entry.getValue().object() && actual.token() == entry.getValue().token());
        }
    }

    private static int count(BlockPos position, Item item) {
        Container container = (Container) player().serverLevel().getBlockEntity(position);
        int total = 0;
        for (int i = 0; i < container.getContainerSize(); i++) if (container.getItem(i).is(item)) total += container.getItem(i).getCount();
        return total;
    }
    private static int inventoryCount(Item item) {
        Container inventory = player().getInventory(); int total = 0;
        for (int i = 0; i < inventory.getContainerSize(); i++) if (inventory.getItem(i).is(item)) total += inventory.getItem(i).getCount();
        if (player().inventoryMenu.getCarried().is(item)) total += player().inventoryMenu.getCarried().getCount();
        return total;
    }
    private static boolean observeClosed() {
        if (owner.radialActive() && !owner.model().focused().equals(chestWindow)) {
            throw new IllegalStateException("Rebound SOUTH close button also swapped focus");
        }
        boolean closed = !owner.workspaceActive() && mc.screen == null;
        if (closed && firstClosedAt == 0) firstClosedAt = System.nanoTime();
        return closed;
    }
    private static void assertServer(String name, BooleanSupplier condition) {
        add(name, () -> server(condition::getAsBoolean), () -> {
            if (!serverDone()) return false;
            if ((Boolean) serverTask.join()) return true;
            // Network dispatch and the integrated server tick can trail a client frame.
            // Observe until the authentic server reaches the required result, with the
            // scenario's normal timeout still rejecting a result that never arrives.
            server(condition::getAsBoolean);
            return false;
        }, () -> check(name, (Boolean) serverTask.join()), 2);
    }
    private static <T> void server(Supplier<T> action) {
        CompletableFuture<T> future = new CompletableFuture<>(); serverTask = future;
        mc.getSingleplayerServer().execute(() -> {
            try { future.complete(action.get()); } catch (Throwable failure) { future.completeExceptionally(failure); }
        });
    }
    private static boolean serverDone() { if (serverTask.isCompletedExceptionally()) serverTask.join(); return serverTask.isDone(); }
    private static ServerPlayer player() { return mc.getSingleplayerServer().getPlayerList().getPlayers().get(0); }

    private static void add(String name, Runnable start, BooleanSupplier ready, Runnable verify, int settleTicks) {
        steps.addLast(new Step(name, start, ready, verify, settleTicks));
    }
    private static void tick(Minecraft minecraft) {
        if (finished) return; mc = minecraft;
        if (active == null && mc.player == null && mc.getOverlay() != null) return;
        try {
            if (System.currentTimeMillis() - started > 8 * 60_000) throw new IllegalStateException("QA eight-minute deadline");
            if (active == null) {
                if (steps.isEmpty()) { finish(); return; }
                active = steps.removeFirst(); stepStarted = System.currentTimeMillis(); settle = -1;
                System.out.println("CONTROLIFY_QA_STEP " + active.name()); active.start().run();
            }
            if (active.ready().getAsBoolean()) {
                if (settle < 0) settle = active.settleTicks();
                if (settle-- == 0) {
                    active.verify().run(); record(active.name(), "PASS", "actual controller and integrated-server stage"); active = null;
                }
            } else if (System.currentTimeMillis() - stepStarted > 45_000) {
                throw new IllegalStateException("Stage timeout; " + controllerState());
            }
        } catch (Throwable failure) {
            failure.printStackTrace(); record(active == null ? "suite" : active.name(), "FAIL", failure.toString());
            try { capture("failure"); } catch (Throwable ignored) { }
            finish();
        }
    }
    private static void capture(String label) {
        try (var image = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
            Path path = output.resolve("screenshots").resolve(label + ".png"); image.writeToFile(path);
            record(label, "CAPTURED", path.toString());
        } catch (Exception failure) { throw new IllegalStateException(failure); }
    }
    private static void check(String name, boolean condition) {
        if (!condition) throw new IllegalStateException("Assertion failed: " + name + "; " + controllerState());
        record(name, "PASS", "assertion true");
    }
    private static String controllerState() {
        return "screen=" + (mc.screen == null ? "gameplay" : mc.screen.getClass().getSimpleName())
                + ", inputMode=" + Controlify.instance().currentInputMode()
                + ", workspaceMode=" + (owner == null || owner.model() == null ? "none" : owner.model().mode())
                + ", focused=" + (owner == null || owner.model() == null ? "none" : owner.model().focused())
                + ", selected=" + (owner == null || owner.model() == null ? "none" : owner.model().selected())
                + ", highlighted=" + (owner == null || owner.model() == null ? "none" : owner.model().highlighted());
    }
    private static void record(String name, String status, String details) {
        results.add(new LinkedHashMap<>(Map.of("name", name, "status", status, "details", details,
                "elapsedMs", System.currentTimeMillis() - started)));
        System.out.println("CONTROLIFY_QA_CHECK " + name + " " + status + " " + details);
    }
    private static void finish() {
        finished = true; driver.buttons.clear(); driver.trigger = 0; driver.stick(0, 0); driver.walk = 0;
        long failures = results.stream().filter(result -> result.get("status").equals("FAIL")).count();
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("scenario", "controlify-controller-world"); report.put("failures", failures); report.put("results", results);
        report.put("elapsedMs", System.currentTimeMillis() - started);
        report.put("coreCodeSource", InventoryDesktopScreen.class.getProtectionDomain().getCodeSource().getLocation().toString());
        report.put("companionCodeSource", SaltsControlifyClient.class.getProtectionDomain().getCodeSource().getLocation().toString());
        report.put("controlifyCodeSource", Controlify.class.getProtectionDomain().getCodeSource().getLocation().toString());
        report.put("loadedMods", QaRuntime.loadedMods());
        try { Files.writeString(output.resolve("results.json"), new GsonBuilder().setPrettyPrinting().create().toJson(report)); }
        catch (Exception failure) { failure.printStackTrace(); }
        System.out.println("CONTROLIFY_QA_SUMMARY " + (failures == 0 ? "PASS" : "FAIL") + " records=" + results.size());
        mc.stop();
    }
    private static Object field(Object target, String name) {
        Class<?> type = target instanceof Class<?> clazz ? clazz : target.getClass();
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            try { Field field = current.getDeclaredField(name); field.setAccessible(true); return field.get(target instanceof Class<?> ? null : target); }
            catch (NoSuchFieldException ignored) { } catch (Exception failure) { throw new IllegalStateException(failure); }
        }
        throw new IllegalArgumentException(type + "." + name);
    }

    private static void addFeatureParityScenarios() {
        QaFeatureParity.append(new QaFeatureParity.Harness() {
            public void step(String n,Runnable s,BooleanSupplier r,Runnable v,int t) {
                if(n.equals("parity-api-view-close"))addNativeCursorCallbackProbe();
                add(n,s,r,v,t);
            }
            public void button(String n,String k,Runnable v) {ControlifyGameplayHarness.button(n,parityButton(k),v);}
            public void check(String n,boolean v) {ControlifyGameplayHarness.check(n,v);}
            public void capture(String n) {ControlifyGameplayHarness.capture(n);}
            public QaWorkspaceOwner owner() {return owner;}
            public boolean creative() {return mc.player!=null && mc.player.isCreative();}
            public net.minecraft.client.gui.screens.Screen screen() {return mc.screen;}
            public void neutral() {pointerFixture=null;driver.buttons.clear();driver.trigger=driver.rightTrigger=driver.rightX=driver.rightY=0;driver.stick(0,0);}
            public void leftTrigger(float v) {driver.trigger=v;}
            public void rightTrigger(float v) {driver.rightTrigger=v;}
            public void rightStick(float x,float y) {driver.rightX=x;driver.rightY=y;}
            public void holdButton(String n,boolean down) {if(down)driver.buttons.add(parityButton(n));else driver.buttons.remove(parityButton(n));}
            public void aimWindow(UUID id) {ControlifyGameplayHarness.aimWindow(id);}
            public void point(UUID id,double x,double y) {parityPoint(id,x,y);pointerFixture=() -> parityPoint(id,x,y);}
            public void openContainer(String n,String which) {
                ControlifyGameplayHarness.openContainer(n,() -> which.equals("table")?tablePos:which.equals("furnace")?furnacePos:chestPos,
                    () -> {if(which.equals("table"))tableWindow=newestOther();else if(which.equals("furnace"))furnaceWindow=newestOther();else chestWindow=newestOther();});
            }
            public UUID window(String name) {return switch(name) {case "table" -> tableWindow;case "furnace" -> furnaceWindow;case "chest" -> chestWindow;case "inventory" -> owner.model().playerInventory();default -> throw new IllegalArgumentException(name);};}
            public void server(Runnable r) {ControlifyGameplayHarness.server(() -> {r.run();return null;});}
            public boolean serverDone() {return ControlifyGameplayHarness.serverDone();}
            public ServerPlayer player() {return ControlifyGameplayHarness.player();}
            public Container storage() {return (Container)player().serverLevel().getBlockEntity(chestPos);}
            public AbstractContainerMenu menu(Class<? extends AbstractContainerMenu> c) {return serverMenu(c);}
            public void awardRecipes() {player().awardRecipes(mc.getSingleplayerServer().getRecipeManager().getRecipes());}
            public DesktopWindowHandle openView(QaClientView.Model m) {parityView=m;return DesktopClientApi.openView(QA_VIEW_ID,"feature-parity",m,Component.literal("API navigation and keyboard")).orElseThrow();}
        });
    }
    private static void addNativeCursorCallbackProbe(){
        add("parity-known-native-warp-callback",()->{},()->true,()->{
            check("parity-native-warp-echo-observed",((Number)field(NativeCursorWarpGuard.class,"matchedCallbacks")).longValue()>0);
            check("parity-native-warp-retains-controller-cursor",Controlify.instance().virtualMouseHandler().isVirtualMouseEnabled());
        },3);
        add("parity-unscoped-native-cursor-callback",()->{
            pointerFixture=null;
            probeMouseX=mc.mouseHandler.xpos()+17;probeMouseY=mc.mouseHandler.ypos()+13;
            probeMousePending=true;probeMouseForwarded=false;
            // Outside an owned transition: this native callback must retain ordinary mouse priority.
            org.lwjgl.glfw.GLFW.glfwSetCursorPos(mc.getWindow().getWindow(),probeMouseX,probeMouseY);
        },()->probeMouseForwarded,()->{
            probeMousePending=false;
            check("parity-different-native-mouse-motion-forwarded",probeMouseForwarded);
            check("parity-different-native-mouse-motion-reveals-cursor",!Controlify.instance().virtualMouseHandler().isVirtualMouseEnabled());
        },3);
        add("parity-restore-controller-after-mouse-probe",()->{
            NativeCursorWarpGuard.keyboardTransition((RadialInventoryScreen)mc.screen,()->Controlify.instance().virtualMouseHandler().onScreenChanged());
        },()->Controlify.instance().virtualMouseHandler().isVirtualMouseEnabled(),()->{},4);
    }
    private static ResourceLocation parityButton(String name) {
        try {return (ResourceLocation)GamepadInputs.class.getField(name).get(null);}
        catch(ReflectiveOperationException failure) {throw new IllegalStateException(failure);}
    }
    private static void parityPoint(UUID id,double localX,double localY) {
        var bounds=owner.desktop().presentationBounds(id);var p=renderedLayout().windows().get(id);
        int x=(int)Math.round(p.x()+(localX-bounds.x())*p.scale());
        int y=(int)Math.round(p.y()+(localY-bounds.y())*p.scale());
        var scale=new Vector2d((double)mc.getWindow().getGuiScaledWidth()/mc.getWindow().getScreenWidth(),
            (double)mc.getWindow().getGuiScaledHeight()/mc.getWindow().getScreenHeight());
        Controlify.instance().virtualMouseHandler().snapToPoint(new SnapPoint(x,y,1),scale);
        ((RadialInventoryScreen)mc.screen).mouseMoved(x,y);
    }
}
