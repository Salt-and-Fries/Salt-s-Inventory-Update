package com.salts_inventory_update.controlify.qa;

import com.google.gson.GsonBuilder;
import com.salts_inventory_update.controlify.SaltsControlifyClient;
import com.salts_inventory_update.controlify.RadialInventoryScreen;
import com.salts_inventory_update.api.client.desktop.DesktopWorkspaceWindow;
import com.salts_inventory_update.api.client.desktop.DesktopClientApi;
import com.salts_inventory_update.api.client.desktop.DesktopWindowHandle;
import com.salts_inventory_update.api.client.desktop.DesktopWindowBounds;
import net.minecraft.network.chat.Component;
import com.salts_inventory_update.client.DesktopContainerClient;
import com.salts_inventory_update.client.DesktopContainerSession;
import com.salts_inventory_update.client.InventoryDesktopScreen;
import com.salts_inventory_update.client.SaltsInventoryConfig;
import com.salts_inventory_update.controlify.model.RadialWorkspace;
import com.salts_inventory_update.controlify.model.WorkspaceLayout;
import com.salts_inventory_update.controlify.model.RadialLayout;
import com.salts_inventory_update.controlify.model.WorkspacePresentationLayout;
import com.salts_inventory_update.controlify.model.WindowSize;
import com.salts_inventory_update.controlify.input.ControlifyInputBridge;
import dev.isxander.controlify.Controlify;
import dev.isxander.controlify.InputMode;
import dev.isxander.controlify.api.event.ControlifyEvents;
import dev.isxander.controlify.api.vmousesnapping.SnapPoint;
import dev.isxander.controlify.bindings.ControlifyBindings;
import dev.isxander.controlify.bindings.input.ButtonInput;
import dev.isxander.controlify.config.settings.profile.ProfileSettings;
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
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeButton;
import net.minecraft.client.gui.screens.recipebook.RecipeBookPage;
import net.minecraft.client.gui.screens.recipebook.OverlayRecipeComponent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.Difficulty;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.FurnaceMenu;
import net.minecraft.world.inventory.LoomMenu;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
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
    private static int settle = -1;
    private static long started, stepStarted;
    private static boolean finished;
    private static Path output;
    private static BlockPos chestPos, barrelPos, tablePos, furnacePos, loomPos, sortTargetPos;
    private static UUID chestWindow, barrelWindow, previousSelected;
    private static UUID tableWindow, furnaceWindow, loomWindow;
    private static UUID sortTargetWindow;
    private static Set<UUID> preservedWindows;
    private static Map<Integer, SessionIdentity> preservedSessions;
    private static net.minecraft.world.phys.Vec3 positionBefore;
    private static long heldAt, firstClosedAt;
    private static int furnaceXpAfterSession;
    private static int cursorBeforeX;
    private static double nativeUiMasterVolumeBefore, nativeUiVolumeBefore;
    private static long nativeUiSoundsBefore;
    private static double tuckSelectedSize, tuckNormalSize;
    private static UUID tuckAlternateWindow;
    private static DesktopWindowBounds guideClosedBounds;
    private static AbstractWidget guideWidget;
    private static boolean guideFilterBefore;
    private static int expectedSnapX, expectedSnapY;
    private static long observedTooltipAt, observedCursorAt, observedTabsAt, sortPressAt;
    private static long vanillaBookLegacyBefore;
    private static double guideRenderedCursorScale;
    private static Runnable adaptiveCursorFixture;
    private static UUID adaptiveCursorWindow;
    private static int adaptiveCursorX, adaptiveCursorY;
    private static int guidePageBefore;
    private static Object expectedChoiceRecipe;
    private static volatile String sortServerCounts = "";
    private static String lastRecordedSortCounts = "";
    private static final int[] GEAR_RECIPE_INPUTS = {1, 3, 4, 5, 6, 7, 8, 9};
    private static final Identifier QA_VIEW_ID = Identifier.fromNamespaceAndPath("salts_inventory_controlify_qa", "client_view");
    private static final QaClientView.Model viewA = new QaClientView.Model(), viewB = new QaClientView.Model();
    private static DesktopWindowHandle viewAHandle, viewBHandle, tallGuideHandle;
    private static final QaClientView.Model tallGuideModel = new QaClientView.Model();
    private static UUID tallGuidePreviousFocus, tallGuidePreviousSelected;
    private static Runnable pointerFixture;
    private static final java.util.concurrent.atomic.AtomicInteger pendingCaptures = new java.util.concurrent.atomic.AtomicInteger();
    private static volatile Throwable captureFailure;
    private static BlockPos creativeNavigationBrewingPos;
    private static UUID creativeNavigationBrewingWindow;
    private static Item creativeNavigationItem;
    private static int creativeNavigationScrollBefore, creativeNavigationTabIndex;
    private static Identifier creativeNavigationTabId;
    private static Identifier creativeNavigationNativeActiveBefore;
    private static long creativeNavigationScrollStarted;
    private static int creativeNavigationCursorX, creativeNavigationCursorY;
    private static volatile String creativeNavigationServerObservation = "";
    private static long creativeNavigationNeutralBefore, creativeNavigationDispatchBefore;
    private static BlockPos contextualIngredientPos;
    private static UUID contextualIngredientWindow;
    private static double contextualCarouselBefore, contextualCarouselStopped;
    private static long contextualCarouselStarted;
    private static int contextualCarouselAnchor;
    private static int contextualAlphabetIndex;
    private static CraftingMenu contextualClosedMenu;
    private static int contextualHoverXpBefore;
    private static final Map<Slot,ItemStack> contextualFullStorageBefore=new LinkedHashMap<>();
    private static int contextualInterruptSlot;
    private static long contextualRejectionsBefore,contextualLeaseStarted;
    private static long contextualLeaseTick,contextualLeaseTouched;
    private static DesktopContainerSession contextualStaleSession;
    private static int contextualOwnedIronBeforeClear,contextualOwnedBedrockBeforeClear;
    private static long contextualRejectedEpoch;
    private static long nativeGuideBefore;
    private static long contextualClosingBefore,contextualReleaseBefore,contextualFrameBefore;
    private static boolean contextualAnimationsBefore;
    private static UUID contextualReleaseNeighbor;
    private static com.salts_inventory_update.controlify.model.ContextualPresentationLayout.Rect contextualSourceBefore;
    private static boolean nativeHudHiddenBefore;
    private static Component contextualDefaultGlyph;
    private static final BlockPos[] contextualCookingPositions = new BlockPos[3];
    private static final UUID[] contextualCookingWindows = new UUID[3];
    private static final int[] contextualCookingSessions = new int[3];

    private ControlifyGameplayHarness() { }

    public static void initialize() {
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
        scenarios();
        QaNativePointerFixture.register(inputController -> {
            if (pointerFixture == null || inputController != controller) return;
            if (!ControlifyBindings.INV_QUICK_MOVE.on(inputController).justPressed()
                    && !ControlifyBindings.INV_SELECT.on(inputController).justPressed()
                    && !ControlifyBindings.VMOUSE_LCLICK.on(inputController).justPressed()
                    && !ControlifyBindings.VMOUSE_RCLICK.on(inputController).justPressed()) return;
            Runnable preparePointer = pointerFixture;
            pointerFixture = null;
            preparePointer.run();
            if (active != null && active.name().contains("creative")) creativeNavigationObserveClient("native-pointer-fixture-edge");
        });
        QaRuntime.registerTick(ControlifyGameplayHarness::tick);
        System.out.println("CONTROLIFY_QA_START output=" + output);
    }

    private static void scenarios() {
        add("disposable-flat-world", ControlifyGameplayHarness::createWorld,
                () -> mc.player != null && mc.level != null && mc.getSingleplayerServer() != null, () -> {}, 35);
        add("actual-controlify-driver", ControlifyGameplayHarness::installController,
                () -> Controlify.instance().getCurrentController().orElse(null) == controller,
                () -> check("controller-mode", Controlify.instance().currentInputMode().isController()), 5);
        add("salt-server-handshake", () -> {}, DesktopContainerClient::canUseServerSessions, () -> {}, 10);
        add("server-container-fixtures", () -> server(() -> {
            ServerPlayer player = player();
            chestPos = player.blockPosition().offset(2, 0, 0);
            barrelPos = chestPos.offset(2, 0, 0);
            tablePos = chestPos.offset(0, 0, 2); furnacePos = chestPos.offset(2, 0, 2); loomPos = chestPos.offset(4, 0, 2);
            sortTargetPos = chestPos.offset(5, 0, 0);
            var level = player.level();
            for (int x = -3; x <= 6; x++) for (int z = -3; z <= 3; z++) {
                level.setBlock(chestPos.offset(x, -1, z), Blocks.STONE.defaultBlockState(), 3);
                for (int y = 0; y < 5; y++) level.setBlock(chestPos.offset(x, y, z), Blocks.AIR.defaultBlockState(), 3);
            }
            level.setBlock(chestPos, Blocks.CHEST.defaultBlockState(), 3);
            level.setBlock(barrelPos, Blocks.BARREL.defaultBlockState(), 3);
            level.setBlock(tablePos, Blocks.CRAFTING_TABLE.defaultBlockState(), 3);
            level.setBlock(furnacePos, Blocks.FURNACE.defaultBlockState(), 3);
            level.setBlock(loomPos, Blocks.LOOM.defaultBlockState(), 3);
            level.setBlock(sortTargetPos, Blocks.CHEST.defaultBlockState(), 3);
            ((Container) level.getBlockEntity(chestPos)).setItem(0, new ItemStack(Items.DIAMOND, 16));
            ((Container) level.getBlockEntity(barrelPos)).setItem(0, new ItemStack(Items.EMERALD, 7));
            player.getInventory().clearContent(); player.inventoryMenu.setCarried(ItemStack.EMPTY);
            mc.getSingleplayerServer().getCommands().performPrefixedCommand(
                    mc.getSingleplayerServer().createCommandSourceStack().withSuppressedOutput(), "time set noon");
            mc.getSingleplayerServer().getCommands().performPrefixedCommand(
                    mc.getSingleplayerServer().createCommandSourceStack().withSuppressedOutput(), "recipe give @a *");
            return null;
        }), ControlifyGameplayHarness::serverDone, () -> {}, 10);
        add("first-workspace-native-USE-position", () -> server(() -> {
            player().teleportTo(chestPos.getX() + 0.5, chestPos.getY(), chestPos.getZ() - 2.0);
            return null;
        }), ControlifyGameplayHarness::serverDone, () -> { driver.useTarget = chestPos; aimCameraAt(chestPos); }, 8);
        add("first-workspace-native-USE-camera", () -> {}, () -> cameraTargets(chestPos), () -> {}, 4);
        add("first-ever-LT-opens-workspace", () -> driver.trigger = 1,
                () -> owner.workspaceActive() && mc.gui.screen() instanceof RadialInventoryScreen
                        && owner.windows().values().stream().anyMatch(w -> w.kind() == DesktopWorkspaceWindow.Kind.OTHER),
                () -> {
                    check("first-controller-sample-retains-opening-LT-latch", !owner.radialActive());
                    verifyNativeGuide("opening-release-first", "SIDE", "Release opening trigger");
                    check("native-guide-opening-LT-suppresses-hold", !QaNativeGuideProbe.workspace.label("Hold: window wheel"));
                }, 12);
        add("first-workspace-opening-trigger-release", () -> { driver.trigger = 0; driver.useTarget = null; },
                () -> !owner.radialActive(), () -> {}, 4);
        observeNativeGuide("opening-release-cleared", "SIDE", "Hold: window wheel");
        button("first-workspace-side-B-cleanup", GamepadInputs.EAST_BUTTON,
                () -> check("first-workspace-B-cleanly-returns-gameplay", !owner.workspaceActive() && mc.gui.screen() == null));
        button("initial-Y-opens-player-workspace", GamepadInputs.NORTH_BUTTON, () -> {
            check("ordinary-inventory-uses-controller-workspace", owner.workspaceActive() && mc.gui.screen() instanceof RadialInventoryScreen);
            check("initial-player-and-character-only", owner.windows().values().stream().noneMatch(w -> w.kind() == DesktopWorkspaceWindow.Kind.OTHER));
            check("actual-workspace-input-processor", ScreenProcessorProvider.provide(mc.gui.screen()).getClass().getSimpleName().equals("WorkspaceScreenProcessor"));
            verifyCompactInventory();
        });
        addGuideScenarios();
        openContainer("open-chest-default-pair", () -> chestPos, () -> {
            chestWindow = newestOther();
            check("inventory-focused-by-default", owner.model().focused().equals(owner.model().playerInventory()));
            check("new-chest-selected", owner.model().selected().equals(chestWindow));
            check("mandatory-inventory-character", owner.windows().values().stream().filter(w -> w.kind() != DesktopWorkspaceWindow.Kind.OTHER).count() == 2);
            check("forced-hud-offhand", (Boolean) field(InventoryDesktopScreen.current(mc), "workspaceHud"));
            capture("01-side-by-side-default");
        });
        add("hover-native-chest-tooltip-and-cursor", () -> {
            observedTooltipAt = QaFeatureProbe.tooltipCalls; observedCursorAt = QaFeatureProbe.cursorCalls;
            pointAtItem(chestWindow, Items.DIAMOND);
        }, () -> {
            if (!nativePointerReady()) return false;
            pointAtItem(chestWindow, Items.DIAMOND);
            return QaFeatureProbe.tooltipCalls > observedTooltipAt && QaFeatureProbe.cursorCalls > observedCursorAt;
        }, () -> {
            check("deferred-item-tooltip-at-screen-cursor", Math.abs(QaFeatureProbe.tooltipX - QaFeatureProbe.tooltipCursorX) <= 2
                    && Math.abs(QaFeatureProbe.tooltipY - QaFeatureProbe.tooltipCursorY) <= 2);
            check("stock-cursor-native-scale-matches-window", Math.abs(QaFeatureProbe.cursorScaleX - 0.5 * QaFeatureProbe.cursorExpectedScale) < 0.001
                    && Math.abs(QaFeatureProbe.cursorScaleY - 0.5 * QaFeatureProbe.cursorExpectedScale) < 0.001);
            check("stock-cursor-independent-chest-slot-scale", Math.abs(QaFeatureProbe.cursorScaleX
                    - 0.5 * renderedLayout().windows().get(chestWindow).scale()) < 0.001);
            capture("01b-screen-tooltip-and-adaptive-cursor");
        }, 8);
        nativeCursorNeutralReady("native-guide-ordinary-chest", () -> pointAtItem(chestWindow, Items.DIAMOND));
        observeNativeGuide("ordinary-chest", "SIDE", "Close all windows", "Hold: window wheel", "Previous / next window", "Move cursor", "Navigate slots / controls");
        add("native-guide-retains-native-item-actions", () -> {}, () -> true, () -> {
            check("native-guide-native-item-actions-and-glyphs", QaNativeGuideProbe.workspace.glyph("Take", ControlifyBindings.INV_SELECT.on(controller).inputGlyph())
                && QaNativeGuideProbe.workspace.glyph("Quick Move", ControlifyBindings.INV_QUICK_MOVE.on(controller).inputGlyph()));
            check("compact-native-guide-short-horizontal-items", QaNativeGuideProbe.workspace.horizontal()
                && QaNativeGuideProbe.compactNativeTextDraws > 0 && QaNativeGuideProbe.compactViolations.isEmpty());
            var leftGlyph = Controlify.instance().inputFontMapper().getComponentFromInputs(controller.info().type().namespace(),List.of(GamepadInputs.LEFT_STICK_BUTTON));
            check("compact-native-cursor-uses-one-plain-stick-glyph",QaNativeGuideProbe.workspace.glyph("Move cursor",leftGlyph)
                && leftGlyph.getString().codePointCount(0,leftGlyph.getString().length())==1);
            capture("37-compact-guide-native-item-actions");
        }, 2);
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
        observeNativeGuide("radial-protected", "LT_RADIAL", "Release: show highlighted window", "Focus highlighted window", "Choose window / tuck away");
        add("textured-tuck-normal-frame", () -> {}, () -> QaVisualFeatureProbe.tuck != null && QaVisualFeatureProbe.tuck.frame() == 0, () -> {
            check("textured-tuck-normal-frame-native-blit", QaVisualFeatureProbe.tuck.size() > 0 && QaVisualFeatureProbe.violations().isEmpty());
            tuckNormalSize = QaVisualFeatureProbe.tuck.size();
            capture("38-textured-tuck-normal");
        }, 2);
        add("native-guide-protected-hides-close", () -> {}, () -> true, () ->
            check("native-guide-protected-highlight-hides-close", !QaNativeGuideProbe.workspace.label("Close highlighted window")
                && !QaNativeGuideProbe.workspace.label("Close all windows")), 1);
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
                            && mc.gui.screen() instanceof RadialInventoryScreen && owner.workspaceActive());
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
        add("tuck-bottom-option", () -> {
            controller.settings().generic.guide.showIngameGuide = true;
            controller.settings().generic.guide.ingameGuideBottom = false;
            driver.stick(0, 1);
        }, () -> owner.model().tuckHighlighted(),
                () -> capture("05-tuck-option-highlight"), 3);
        observeNativeGuide("radial-tuck", "LT_RADIAL", "Release: tuck away", "Choose window / tuck away");
        add("textured-tuck-selected-frame", () -> {}, () -> QaVisualFeatureProbe.tuck != null && QaVisualFeatureProbe.tuck.frame() == 1, () -> {
            check("textured-tuck-selected-frame-native-blit", QaVisualFeatureProbe.tuck.size() > tuckNormalSize*1.2
                && QaVisualFeatureProbe.violations().isEmpty());
            check("textured-tuck-both-atlas-frames-decoded-distinct", QaVisualFeatureProbe.textures().size() == 1
                && QaVisualFeatureProbe.textures().values().stream().allMatch(texture -> ((List<?>)texture.get("decodedFrameSha256")).size()==2
                    && !((List<?>)texture.get("decodedFrameSha256")).getFirst().equals(((List<?>)texture.get("decodedFrameSha256")).getLast())));
            check("textured-tuck-intermediate-animation-observed", QaVisualFeatureProbe.tuckIntermediateFrames > 0);
            capture("38b-textured-tuck-selected");
            tuckSelectedSize = QaVisualFeatureProbe.tuck.size();
        }, 2);
        add("textured-tuck-native-highlight-shrinks",() -> {
            tuckAlternateWindow = owner.model().radialOrder().getFirst(); aimWindow(tuckAlternateWindow);
        },() -> owner.model().highlighted().filter(tuckAlternateWindow::equals).isPresent() && QaVisualFeatureProbe.tuck!=null
            && QaVisualFeatureProbe.tuck.placement().highlight()>.02 && QaVisualFeatureProbe.tuck.placement().highlight()<.98
            && QaVisualFeatureProbe.tuck.size()<tuckSelectedSize-.1,() -> {
            check("textured-tuck-native-highlight-shrinks-smoothly",QaVisualFeatureProbe.tuck.size()>0
                && QaVisualFeatureProbe.violations().isEmpty()); capture("38c-textured-tuck-shrink-transition");
        },0);
        add("textured-tuck-native-highlight-returns",() -> driver.stick(0,1),
            () -> owner.model().tuckHighlighted() && QaVisualFeatureProbe.tuck!=null && QaVisualFeatureProbe.tuck.frame()==1,
            () -> check("textured-tuck-native-selection-returns-before-release",QaVisualFeatureProbe.tuck.size()>tuckNormalSize*1.2),4);
        add("tuck-keeps-lifetimes", () -> {
            preservedWindows = Set.copyOf(owner.windows().keySet()); preservedSessions = sessions();
            driver.trigger = 0; driver.stick(0, 0);
        }, () -> owner.tuckedAway() && mc.gui.screen() == null, () -> {
            verifyPreserved(); capture("06-tucked-gameplay");
        }, 8);
        add("persistent-tucked-tabs-after-animation", () -> observedTabsAt = QaFeatureProbe.tabFrames, () -> true, () -> {
            verifyTuckedTabs("persistent-tucked-tabs");
            check("tabs-rendered-after-transition", QaFeatureProbe.tabFrames > observedTabsAt);
            capture("06b-persistent-tucked-tabs");
        }, 15);
        add("persistent-tabs-with-disabled-animations", () -> {
            setAnimations(false); observedTabsAt = QaFeatureProbe.tabFrames;
        }, () -> true, () -> {
            verifyTuckedTabs("disabled-animation-tabs");
            check("disabled-animation-tabs-rendered", QaFeatureProbe.tabFrames > observedTabsAt);
            capture("06c-disabled-animation-tucked-tabs"); setAnimations(true);
        }, 8);
        addTuckedNativeGuideScenarios();
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
            check("new-window-keeps-gameplay", owner.tuckedAway() && mc.gui.screen() == null);
            verifyPreserved();
            check("barrel-added-existing-workspace", !preservedWindows.contains(barrelWindow) && owner.windows().containsKey(barrelWindow));
            preservedWindows = Set.copyOf(owner.windows().keySet()); preservedSessions = sessions();
            verifyTuckedTabs("new-container-tucked-tab"); capture("06d-tucked-tabs-new-barrel");
        }, 12);
        button("Y-restores-same-workspace", GamepadInputs.NORTH_BUTTON, () -> {
            check("restored-side-mode", !owner.tuckedAway() && !owner.radialActive() && mc.gui.screen() instanceof RadialInventoryScreen);
            verifyPreserved(); capture("07-restored-existing-windows");
        });
        add("seed-chest-second-transfer", () -> server(() -> {
            ((Container) player().level().getBlockEntity(chestPos)).setItem(0, new ItemStack(Items.DIAMOND, 32));
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
        addRadialSortScenarios();
        add("disable-animations-config", () -> {
            check("queued-native-fractional-vertices", WorkspaceOpacityProbe.fractionalCalls() > 0);
            check("queued-native-opacity-survives-extraction", WorkspaceOpacityProbe.deferredFractionalCalls() > 0);
            check("queued-native-ordinary-colors", WorkspaceOpacityProbe.ordinaryCalls() > 0);
            check("queued-native-premultiplied-item-entity-colors", WorkspaceOpacityProbe.premultipliedCalls() > 0);
            check("queued-opacity-scope-restored", com.salts_inventory_update.controlify.render.WorkspaceOpacity.current() == 1);
            record("queued-opacity-native-probe", "OBSERVED", WorkspaceOpacityProbe.snapshot().toString());
            try { owner.config().setAnimationsEnabled(false); } catch (Exception failure) { throw new IllegalStateException(failure); }
            driver.trigger = 1; aimWindow(barrelWindow);
        }, () -> owner.radialActive(), () -> {
            WorkspaceLayout<UUID> current = renderedLayout();
            Map<UUID, WindowSize> sizes = new LinkedHashMap<>();
            owner.windows().forEach((id, window) -> {
                var bounds = owner.desktop().presentationBounds(id);
                sizes.put(id, new WindowSize(bounds.width(), bounds.height()));
            });
            WorkspaceLayout<UUID> target = new WorkspacePresentationLayout().compute(owner.model(), sizes,
                    QaNativeGuideProbe.layoutWidth, QaNativeGuideProbe.layoutHeight, 1);
            check("disabled-animation-immediate-target", current.equals(target));
            check("native-guide-layout-viewport-is-observed-content-area", QaNativeGuideProbe.layoutFrames > 0
                && QaNativeGuideProbe.layoutWidth == mc.getWindow().getGuiScaledWidth()
                && QaNativeGuideProbe.layoutHeight > 0 && QaNativeGuideProbe.layoutHeight < mc.getWindow().getGuiScaledHeight());
            capture("09-animations-disabled-radial");
        }, 1);
        observeNativeGuide("radial-closable-sort", "LT_RADIAL", "Close highlighted window", "Release: show highlighted window",
            "Focus highlighted window", "Choose window / tuck away", "Sort focused; double tap: force");
        add("B-closes-highlighted-barrel", () -> { heldAt = System.nanoTime(); driver.buttons.add(GamepadInputs.EAST_BUTTON); },
            () -> !owner.windows().containsKey(barrelWindow), () -> {
                check("only-highlighted-window-closed", owner.windows().containsKey(chestWindow)
                    && owner.windows().containsKey(owner.model().playerInventory()) && owner.windows().containsKey(owner.model().character()));
                check("radial-B-closes-highlighted-immediately", (System.nanoTime() - heldAt) / 1_000_000 < 500);
                preservedWindows = Set.copyOf(owner.windows().keySet()); preservedSessions = sessions();
            }, 0);
        add("keep-B-held-after-highlighted-close", () -> {}, () -> System.nanoTime() - heldAt >= 1_500_000_000L, () -> {
            check("radial-held-B-after-closable-does-not-close-all", owner.radialActive() && mc.gui.screen() instanceof RadialInventoryScreen
                && owner.windows().keySet().equals(preservedWindows));
            verifyPreserved(); driver.buttons.remove(GamepadInputs.EAST_BUTTON);
        }, 0);
        add("release-radial-before-protected-hold", () -> { driver.trigger = 0; driver.stick(0, 0); }, () -> !owner.radialActive(), () -> {}, 2);
        add("configured-close-SOUTH-owns-A", () -> {
            ControlifyBindings.GUI_BACK.on(controller).setBoundInput(new ButtonInput(GamepadInputs.SOUTH_BUTTON));
            driver.trigger = 1; aimWindow(owner.model().playerInventory());
        }, () -> owner.radialActive(), () -> {}, 2);
        observeNativeGuide("rebound-close-protected", "LT_RADIAL", "Release: show highlighted window", "Choose window / tuck away");
        add("native-guide-close-owned-focus-hidden", () -> {}, () -> true, () ->
            check("native-guide-close-owned-focus-action-hidden", !QaNativeGuideProbe.workspace.label("Focus highlighted window")
                && !QaNativeGuideProbe.workspace.label("Close all windows")), 2);
        add("held-B-keeps-protected-radial-window", () -> {
            heldAt = System.nanoTime(); preservedWindows = Set.copyOf(owner.windows().keySet()); preservedSessions = sessions();
            driver.buttons.add(GamepadInputs.SOUTH_BUTTON);
        }, () -> System.nanoTime() - heldAt >= 1_500_000_000L, () -> {
            check("radial-held-B-does-not-close-all", owner.radialActive() && mc.gui.screen() instanceof RadialInventoryScreen
                && owner.windows().containsKey(chestWindow));
            check("radial-held-B-protects-mandatory-windows", owner.windows().containsKey(owner.model().playerInventory())
                && owner.windows().containsKey(owner.model().character()));
            check("radial-rebound-close-owns-A", owner.model().focused().equals(chestWindow));
            verifyPreserved(); driver.buttons.clear();
        }, 0);
        add("release-protected-radial-hold", () -> { driver.trigger = 0; driver.stick(0, 0); }, () -> !owner.radialActive(), () -> {}, 3);
        button("side-B-closes-all-after-protected-radial-hold", GamepadInputs.SOUTH_BUTTON, () -> {
            check("side-close-all-returned-gameplay", !owner.workspaceActive() && mc.gui.screen() == null);
            check("close-all-keeps-only-permanent", owner.windows().values().stream().noneMatch(w -> w.kind() == DesktopWorkspaceWindow.Kind.OTHER));
            check("close-all-server-sessions-disposed", sessions().values().stream().noneMatch(i -> i.object().title().getString().equals("Chest")));
            capture("10-close-all-gameplay");
        });
        add("native-guide-normal-gameplay-after-close", () -> nativeGuideBefore = QaNativeGuideProbe.gameplayFrames,
            () -> QaNativeGuideProbe.gameplayFrames > nativeGuideBefore && QaNativeGuideProbe.gameplay != null
                && QaNativeGuideProbe.gameplay.label("Open Inventory"), () -> {
                var frame = QaNativeGuideProbe.gameplay;
                check("native-guide-outside-tuck-restores-native-pose-and-inventory", frame.bothColumns() && !frame.bottomAligned()
                    && Math.abs(frame.parentY()) < .01 && frame.tuckedInset() == 0 && frame.top() >= 0
                    && !frame.label("Restore windows") && frame.glyph("Open Inventory", ControlifyBindings.INVENTORY.on(controller).inputGlyph()));
                capture("35d-native-gameplay-restored-position");
                controller.settings().generic.guide.showIngameGuide = false;
            }, 3);
        add("position-first-reopen-world-interaction", () -> server(() -> {
            player().teleportTo(chestPos.getX() + 0.5, chestPos.getY(), chestPos.getZ() - 2.0);
            return null;
        }), ControlifyGameplayHarness::serverDone, () -> { driver.useTarget = chestPos; aimCameraAt(chestPos); }, 8);
        add("first-reopen-camera-ready", () -> {}, () -> cameraTargets(chestPos), () -> {}, 5);
        add("first-native-LT-reopens-after-close", () -> driver.trigger = 1,
                () -> owner.workspaceActive() && mc.gui.screen() instanceof RadialInventoryScreen
                        && owner.windows().values().stream().anyMatch(w -> w.kind() == DesktopWorkspaceWindow.Kind.OTHER), () -> {
            check("first-world-interaction-reopens-after-close", owner.model().selected().equals(newestOther()));
            check("opening-held-LT-stays-side", !owner.radialActive());
            check("reopen-default-focus", owner.model().focused().equals(owner.model().playerInventory()));
        }, 12);
        add("opening-LT-released-before-radial", () -> { driver.trigger = 0; driver.useTarget = null; }, () -> true,
                () -> check("opening-LT-release-retains-side", !owner.radialActive()), 4);
        add("fresh-LT-can-enter-radial", () -> driver.trigger = 1, () -> owner.radialActive(),
                () -> check("fresh-LT-after-opening-enters-radial", owner.radialActive()), 4);
        add("return-side-before-immediate-close", () -> driver.trigger = 0, () -> !owner.radialActive(), () -> {}, 4);
        add("press-close-all-side", () -> {
            heldAt = System.nanoTime(); firstClosedAt = 0; driver.buttons.add(GamepadInputs.SOUTH_BUTTON);
        }, () -> {
            boolean closed = !owner.workspaceActive() && mc.gui.screen() == null;
            if (closed && firstClosedAt == 0) firstClosedAt = System.nanoTime();
            return closed;
        }, () -> {
            driver.buttons.clear();
            long elapsed = (firstClosedAt - heldAt) / 1_000_000;
            check("side-B-closes-immediately", elapsed < 500);
            check("side-B-returned-gameplay", mc.gui.screen() == null);
        }, 4);
        button("Y-opens-workspace-after-close-all", GamepadInputs.NORTH_BUTTON,
                () -> check("ordinary-Y-after-close", owner.workspaceActive() && mc.gui.screen() instanceof RadialInventoryScreen));
        addNativeResultScenarios();
    }

    private static void addGuideScenarios() {
        add("guide-toggle-is-rigid-snap-option", () -> {
            // The native guide remains a supported Core presentation when a host opts out
            // of the alternate book. Keep its real compatibility checks in this explicit phase.
            vanillaBookLegacyBefore = QaFeatureProbe.vanillaBookSpritesLegacyOptOut;
            owner.desktop().setContextualRecipePresentation(false);
            guideClosedBounds = owner.desktop().presentationBounds(owner.model().character());
            var toggle = owner.desktop().visibleControls(owner.model().character()).getFirst();
            verifyControlSnaps(owner.model().character(), "closed-guide-toggle");
            // The result slot is a real neighboring controller target above the native toggle.
            // Its decorative offset must not prevent moving down into that control.
            setDpadExpected(owner.model().character(), toggle);
            pointAtNativeSlot(owner.model().character(), slot -> slot.index == 0);
        }, () -> true, () -> {}, 3);
        nativeCursorNeutralReady("native-DPAD-selects-crafting-guide-toggle", () -> {
            var toggle = owner.desktop().visibleControls(owner.model().character()).getFirst();
            setDpadExpected(owner.model().character(), toggle);
            pointAtNativeSlot(owner.model().character(), slot -> slot.index == 0);
            record("guide-native-result-source-before-DPAD", "OBSERVED", dpadDiagnostics());
        });
        add("native-DPAD-selects-crafting-guide-toggle-press", () -> {
            creativeNavigationDispatchBefore = QaFeatureProbe.navigationRequests;
            driver.buttons.add(GamepadInputs.DPAD_DOWN_BUTTON);
        }, () -> QaFeatureProbe.navigationRequests > creativeNavigationDispatchBefore,
            () -> driver.buttons.remove(GamepadInputs.DPAD_DOWN_BUTTON), 0);
        add("native-DPAD-selects-crafting-guide-toggle", () -> driver.buttons.remove(GamepadInputs.DPAD_DOWN_BUTTON),
            () -> true, () -> verifyDpadTarget("native-DPAD-guide-toggle"), 3);
        nativePointerClick("native-A-opens-crafting-guide", GamepadInputs.SOUTH_BUTTON,
            () -> pointAtControl(owner.model().character(), owner.desktop().visibleControls(owner.model().character()).getFirst()), () ->
                check("guide-opened-through-native-controller-click", guideBook().isVisible()));
        add("expanded-guide-resizes-controller-pane", () -> {}, () -> true, () -> {
            var expanded = owner.desktop().presentationBounds(owner.model().character());
            check("guide-visual-bounds-expanded", expanded.width() > guideClosedBounds.width() || expanded.height() > guideClosedBounds.height());
            verifyDisplayedPanes("guide-expanded-fitted-panes");
            verifyControlSnaps(owner.model().character(), "expanded-guide-controls");
            check("guide-has-unlocked-recipe-controls", guideRecipeButtons().stream().anyMatch(widget -> widget.visible && widget.active));
            guideWidget = (AbstractWidget) field(guideBook(), "filterButton");
            guideFilterBefore = ((CycleButton<Boolean>) guideWidget).getValue();
            prepareDpadTarget(owner.model().character(), widgetBounds(guideWidget));
            capture("00a-open-crafting-guide-fitted-pair");
        }, 12);
        nativeCursorNeutralReady("actual-cursor-hover-open-guide-control", () -> {});
        add("actual-cursor-hover-open-guide-control", () -> prepareAdaptiveCursor(owner.model().character(), () ->
            pointAtControl(owner.model().character(), widgetBounds((AbstractWidget) field(guideBook(), "filterButton")))),
            ControlifyGameplayHarness::adaptiveCursorReady, () -> {
            guideRenderedCursorScale = renderedLayout().windows().get(owner.model().character()).scale();
            check("actual-native-cursor-uses-guide-pane-scale", Math.abs(QaFeatureProbe.cursorScaleX - 0.5 * guideRenderedCursorScale) < 0.001
                    && Math.abs(QaFeatureProbe.cursorScaleY - 0.5 * guideRenderedCursorScale) < 0.001);
            capture("00a1-adaptive-cursor-over-guide");
        }, 5);
        nativeCursorNeutralReady("actual-cursor-hover-compact-inventory-slot", () -> {});
        add("actual-cursor-hover-compact-inventory-slot", () -> prepareAdaptiveCursor(owner.model().playerInventory(), () ->
            pointAtNativeSlot(owner.model().playerInventory(), slot -> slot.container == mc.player.getInventory() && slot.getContainerSlot() >= 9)),
            ControlifyGameplayHarness::adaptiveCursorReady, () -> {
            double inventoryScale = renderedLayout().windows().get(owner.model().playerInventory()).scale();
            check("guide-and-inventory-require-distinct-cursor-scales", Math.abs(inventoryScale - guideRenderedCursorScale) > 0.05);
            check("actual-native-cursor-changes-to-inventory-slot-scale", Math.abs(QaFeatureProbe.cursorScaleX - 0.5 * inventoryScale) < 0.001
                    && Math.abs(QaFeatureProbe.cursorScaleY - 0.5 * inventoryScale) < 0.001);
            capture("00a2-adaptive-cursor-over-inventory");
            prepareDpadTarget(owner.model().character(), widgetBounds((AbstractWidget) field(guideBook(), "filterButton")));
        }, 5);
        button("native-DPAD-selects-guide-filter", GamepadInputs.DPAD_RIGHT_BUTTON,
                () -> verifyDpadTarget("native-DPAD-guide-filter"));
        nativePointerClick("native-A-toggles-guide-filter", GamepadInputs.SOUTH_BUTTON,
            () -> pointAtControl(owner.model().character(), widgetBounds((AbstractWidget) field(guideBook(), "filterButton"))), () ->
                check("native-controller-guide-filter-changed", ((CycleButton<Boolean>) field(guideBook(), "filterButton")).getValue() != guideFilterBefore));
        // Restore the unfiltered book so the unlocked noncraftable recipe families remain visible.
        add("position-guide-filter-for-restore", () -> pointAtControl(owner.model().character(), widgetBounds((AbstractWidget) field(guideBook(), "filterButton"))),
                () -> true, () -> {}, 3);
        nativePointerClick("native-A-restores-guide-filter", GamepadInputs.SOUTH_BUTTON,
            () -> pointAtControl(owner.model().character(), widgetBounds((AbstractWidget) field(guideBook(), "filterButton"))), () ->
                check("native-controller-guide-filter-restored", ((CycleButton<Boolean>) field(guideBook(), "filterButton")).getValue() == guideFilterBefore));
        add("prepare-native-guide-search-navigation", () -> {
            guideWidget = (AbstractWidget) field(guideBook(), "searchBox");
            // Approach the wide search control from a real neighboring category control.
            // This exercises the same category-to-search navigation available to a player.
            setDpadExpected(owner.model().character(), widgetBounds(guideWidget));
            var category = ((List<AbstractWidget>) field(guideBook(), "tabButtons")).getFirst();
            pointAtControl(owner.model().character(), widgetBounds(category));
            record("native-guide-search-start", "OBSERVED", dpadDiagnostics());
        }, () -> true, () -> {}, 3);
        button("native-DPAD-selects-guide-search", GamepadInputs.DPAD_RIGHT_BUTTON,
                () -> verifyDpadTarget("native-DPAD-guide-search"));
        add("prepare-native-guide-category-navigation", () -> {
            Object selected = field(guideBook(), "selectedTab");
            guideWidget = ((List<AbstractWidget>) field(guideBook(), "tabButtons")).stream()
                    .filter(widget -> widget.visible && widget.active && widget != selected).findFirst().orElseThrow();
            prepareDpadTarget(owner.model().character(), widgetBounds(guideWidget));
        }, () -> true, () -> {}, 3);
        button("native-DPAD-selects-guide-category", GamepadInputs.DPAD_RIGHT_BUTTON, () -> verifyDpadTarget("native-DPAD-guide-category"));
        nativePointerClick("native-A-selects-guide-category", GamepadInputs.SOUTH_BUTTON,
            () -> pointAtControl(owner.model().character(), widgetBounds(guideWidget)),
                () -> check("actual-native-guide-category-selected", field(guideBook(), "selectedTab") == guideWidget));
        add("prepare-restore-guide-search-category", () -> {
            guideWidget = ((List<AbstractWidget>) field(guideBook(), "tabButtons")).getFirst();
            pointAtControl(owner.model().character(), widgetBounds(guideWidget));
        }, () -> true, () -> {}, 3);
        nativePointerClick("native-A-restores-guide-search-category", GamepadInputs.SOUTH_BUTTON,
            () -> pointAtControl(owner.model().character(), widgetBounds(guideWidget)),
                () -> check("actual-native-guide-search-category-restored", field(guideBook(), "selectedTab") == guideWidget));
        add("prepare-native-guide-forward-page", () -> {
            check("unlocked-guide-has-multiple-recipe-pages", (Integer) field(guidePage(), "totalPages") > 1);
            guidePageBefore = (Integer) field(guidePage(), "currentPage");
            guideWidget = (AbstractWidget) field(guidePage(), "forwardButton");
            check("native-guide-forward-page-control-enabled", guideWidget.visible && guideWidget.active);
            prepareDpadTarget(owner.model().character(), widgetBounds(guideWidget));
        }, () -> true, () -> {}, 3);
        button("native-DPAD-selects-guide-next-page", GamepadInputs.DPAD_RIGHT_BUTTON, () -> verifyDpadTarget("native-DPAD-guide-next-page"));
        nativePointerClick("native-A-advances-guide-page", GamepadInputs.SOUTH_BUTTON,
            () -> pointAtControl(owner.model().character(), widgetBounds(guideWidget)),
                () -> check("actual-native-guide-page-advanced", (Integer) field(guidePage(), "currentPage") == guidePageBefore + 1));
        add("prepare-native-guide-back-page", () -> {
            guideWidget = (AbstractWidget) field(guidePage(), "backButton");
            prepareDpadTarget(owner.model().character(), widgetBounds(guideWidget));
        }, () -> true, () -> {}, 3);
        button("native-DPAD-selects-guide-previous-page", GamepadInputs.DPAD_RIGHT_BUTTON, () -> verifyDpadTarget("native-DPAD-guide-previous-page"));
        nativePointerClick("native-A-returns-guide-page", GamepadInputs.SOUTH_BUTTON,
            () -> pointAtControl(owner.model().character(), widgetBounds(guideWidget)),
                () -> check("actual-native-guide-page-restored", (Integer) field(guidePage(), "currentPage") == guidePageBefore));
        add("position-guide-recipe-family", () -> {
            guideWidget = guideRecipeButtons().stream().filter(button -> button.visible && button.active
                    && button.getCollection().getRecipes().size() > 1).findFirst().orElseThrow(() -> new IllegalStateException("Unlocked multi-choice recipe family missing"));
            prepareDpadTarget(owner.model().character(), widgetBounds(guideWidget));
            ControlifyBindings.VMOUSE_RCLICK.on(controller).setBoundInput(new ButtonInput(GamepadInputs.MISC_2_BUTTON));
        }, () -> true, () -> {}, 3);
        button("native-DPAD-selects-guide-recipe", GamepadInputs.DPAD_RIGHT_BUTTON,
                () -> verifyDpadTarget("native-DPAD-guide-recipe"));
        nativePointerClick("native-controller-opens-expanded-recipe-choices", GamepadInputs.MISC_2_BUTTON,
            () -> pointAtControl(owner.model().character(), widgetBounds(guideWidget)), () ->
                check("native-guide-choice-overlay-open", guideOverlay().isVisible()));
        add("expanded-guide-choices-have-rigid-snap-options", () -> {}, () -> true, () -> {
            verifyControlSnaps(owner.model().character(), "guide-choice-controls");
            verifyDisplayedPanes("guide-choice-fitted-panes");
            List<AbstractWidget> choices = guideChoiceButtons();
            check("actual-expanded-recipe-choices-populated", choices.size() > 1);
            guideWidget = choices.getFirst();
            expectedChoiceRecipe = Objects.requireNonNull(field(guideWidget, "recipe"));
            prepareDpadTarget(owner.model().character(), widgetBounds(guideWidget));
            capture("00b-expanded-guide-choice-controls");
        }, 12);
        button("native-DPAD-selects-expanded-recipe-choice", GamepadInputs.DPAD_RIGHT_BUTTON,
                () -> verifyDpadTarget("native-DPAD-expanded-guide-choice"));
        nativePointerClick("native-A-selects-expanded-recipe-choice", GamepadInputs.SOUTH_BUTTON,
            () -> pointAtControl(owner.model().character(), widgetBounds(guideWidget)), () -> {
            check("native-recipe-choice-click-consumed", expectedChoiceRecipe.equals(guideOverlay().getLastRecipeClicked())
                    && expectedChoiceRecipe.equals(guidePage().getLastClickedRecipe()));
            check("native-expanded-choice-reaches-recipe-placement", expectedChoiceRecipe.equals(field(guideBook(), "lastRecipe"))
                    && expectedChoiceRecipe.equals(field(guideBook(), "lastPlacedRecipe")));
        });
        // Native 26.3 keeps the choice panel open after a successful selection. An outside
        // press dismisses it before the ordinary guide toggle can receive another click.
        add("position-native-outside-expanded-choice", ControlifyGameplayHarness::pointOutsideExpandedGuideChoice, () -> true, () -> {}, 3);
        nativePointerClick("native-A-dismisses-expanded-choice-outside", GamepadInputs.SOUTH_BUTTON,
            ControlifyGameplayHarness::pointOutsideExpandedGuideChoice, () ->
                check("native-expanded-choice-outside-dismissed", !guideOverlay().isVisible() && guideBook().isVisible()));
        add("position-guide-toggle-to-close", () -> pointAtControl(owner.model().character(), owner.desktop().visibleControls(owner.model().character()).getFirst()),
                () -> true, () -> {}, 3);
        nativePointerClick("native-A-closes-crafting-guide", GamepadInputs.SOUTH_BUTTON,
            () -> pointAtControl(owner.model().character(), owner.desktop().visibleControls(owner.model().character()).getFirst()), () ->
                check("guide-native-toggle-closed", !guideBook().isVisible()));
        add("closed-guide-restores-controller-pane-size", () -> {}, () -> true, () -> {
            check("guide-closed-original-visual-size", owner.desktop().presentationBounds(owner.model().character()).equals(guideClosedBounds));
            verifyDisplayedPanes("guide-closed-fitted-panes"); verifyCompactInventory();
            check("legacy-native-guide-compatible-with-contextual-opt-out", !guideBook().isVisible());
            check("legacy-opt-out-renders-native-vanilla-book-toggle", QaFeatureProbe.vanillaBookSpritesLegacyOptOut > vanillaBookLegacyBefore);
            capture("00c-compact-inventory-guide-closed");
            owner.desktop().setContextualRecipePresentation(true);
        }, 12);
    }

    private static void addRadialSortScenarios() {
        openContainer("open-empty-third-sort-target", () -> sortTargetPos, () -> sortTargetWindow = newestOther());
        add("seed-normal-matching-sort", () -> server(() -> {
            Container source = (Container) player().level().getBlockEntity(chestPos);
            source.setItem(0, new ItemStack(Items.GOLD_INGOT, 6));
            source.setItem(1, new ItemStack(Items.EMERALD, 9));
            source.setItem(2, new ItemStack(Items.GRAVEL, 4));
            player().getInventory().setItem(8, new ItemStack(Items.GOLD_INGOT, 1));
            player().inventoryMenu.broadcastChanges();
            return count(barrelPos, Items.EMERALD) == 7 && count(sortTargetPos, Items.GRAVEL) == 0;
        }), ControlifyGameplayHarness::serverDone, () -> check("sort-fixture-authentic-containers", (Boolean) serverTask.join()), 10);
        focusPair("sort-middle-chest-empty-selected", () -> chestWindow, () -> sortTargetWindow);
        add("hold-LT-for-middle-container-sort", () -> {
            driver.trigger = 1; aimWindow(owner.model().character());
        }, () -> owner.radialActive(), () -> check("sort-source-is-middle-focused-chest", owner.model().focused().equals(chestWindow)
                && owner.model().selected().equals(sortTargetWindow)), 4);
        button("native-LT-Y-normal-all-open-sort", GamepadInputs.NORTH_BUTTON, () ->
                check("Y-sort-retains-radial-middle-source", owner.radialActive() && owner.model().focused().equals(chestWindow)));
        // The regular Core sort button targets open storage containers, not player inventory.
        assertServer("normal-sort-matches-beyond-displayed-pair", () -> count(chestPos, Items.GOLD_INGOT) == 6
                && inventoryCount(Items.GOLD_INGOT) == 1 && count(chestPos, Items.EMERALD) == 0
                && count(barrelPos, Items.EMERALD) == 16 && count(chestPos, Items.GRAVEL) == 4
                && count(sortTargetPos, Items.GOLD_INGOT) == 0 && count(sortTargetPos, Items.EMERALD) == 0
                && count(sortTargetPos, Items.GRAVEL) == 0);
        add("separate-double-sort-from-first-tap", () -> {}, () -> true, () -> {}, 9);
        add("double-Y-first-real-press", () -> {
            sortPressAt = System.nanoTime(); driver.buttons.add(GamepadInputs.NORTH_BUTTON);
        }, () -> true, () -> {}, 0);
        add("double-Y-real-release-edge", () -> driver.buttons.remove(GamepadInputs.NORTH_BUTTON), () -> true, () -> {}, 0);
        add("double-Y-second-real-press", () -> {
            check("double-Y-native-edge-within-threshold", (System.nanoTime() - sortPressAt) / 1_000_000 <= 350);
            driver.buttons.add(GamepadInputs.NORTH_BUTTON);
        }, () -> true, () -> {}, 0);
        add("double-Y-release-after-force", () -> driver.buttons.remove(GamepadInputs.NORTH_BUTTON), () -> true, () ->
                check("double-Y-retains-focused-middle", owner.radialActive() && owner.model().focused().equals(chestWindow)), 3);
        // Native force sort first prefers shared item tags. Gold shares tags with the barrel's
        // emeralds/diamonds; tagless gravel reaches the empty/focused third storage instead.
        assertServer("force-sort-moves-unmatched-to-empty-open-container-conserves-all", () -> count(chestPos, Items.GRAVEL) == 0
                && count(sortTargetPos, Items.GRAVEL) == 4 && inventoryCount(Items.GOLD_INGOT) == 1
                && count(barrelPos, Items.GOLD_INGOT) == 6 && count(sortTargetPos, Items.GOLD_INGOT) == 0
                && count(barrelPos, Items.EMERALD) == 16 && count(chestPos, Items.GOLD_INGOT) == 0
                && count(chestPos, Items.EMERALD) == 0 && inventoryCount(Items.GRAVEL) == 0 && count(barrelPos, Items.GRAVEL) == 0);
        add("sort-native-radial-capture", () -> {}, () -> true, () -> capture("08b-native-radial-sort-normal-and-force"), 3);
        add("release-LT-after-native-sort", () -> { driver.trigger = 0; driver.stick(0, 0); }, () -> !owner.radialActive(), () -> {}, 6);
    }

    private static void verifyCompactInventory() {
        UUID id = owner.model().playerInventory();
        var state = owner.windows().get(id).handle().state();
        int storageBottom = Integer.MIN_VALUE, hotbarTop = Integer.MAX_VALUE, storageCount = 0, hotbarCount = 0;
        for (var hit : owner.desktop().visibleSlots(id)) {
            Slot slot = owner.desktop().hoveredSlot(id, hit.x() + 8, hit.y() + 8);
            if (slot == null || slot.container != mc.player.getInventory()) continue;
            if (slot.getContainerSlot() < 9) { hotbarTop = Math.min(hotbarTop, hit.y()); hotbarCount++; }
            else if (slot.getContainerSlot() < 36) { storageBottom = Math.max(storageBottom, hit.y() + 16); storageCount++; }
        }
        check("workspace-all-player-storage-and-hotbar-visible", storageCount == 27 && hotbarCount == 9);
        check("workspace-hotbar-gap-is-compact", hotbarTop >= storageBottom && hotbarTop - storageBottom <= 8);
        check("workspace-hotbar-inside-compact-inventory", hotbarTop + 16 <= state.y() + state.height() && state.height() < 160);
    }

    private static void verifyDisplayedPanes(String name) {
        var frame = renderedLayout();
        var first = frame.windows().get(owner.model().focused()); var second = frame.windows().get(owner.model().selected());
        var a = owner.desktop().presentationBounds(owner.model().focused()); var b = owner.desktop().presentationBounds(owner.model().selected());
        check(name + "-bounded", first.x() >= -0.01 && first.y() >= -0.01 && second.x() >= -0.01 && second.y() >= -0.01
                && first.x() + a.width() * first.scale() <= mc.getWindow().getGuiScaledWidth() + 0.01
                && second.x() + b.width() * second.scale() <= mc.getWindow().getGuiScaledWidth() + 0.01
                && first.y() + a.height() * first.scale() <= mc.getWindow().getGuiScaledHeight() + 0.01
                && second.y() + b.height() * second.scale() <= mc.getWindow().getGuiScaledHeight() + 0.01);
        check(name + "-separate", first.x() + a.width() * first.scale() <= second.x() + 0.01);
    }

    private static void verifyTuckedTabs(String name) {
        check(name + "-actual-gameplay-row", owner.tuckedAway() && mc.gui.screen() == null && QaFeatureProbe.tabCount == owner.windows().size());
        check(name + "-every-live-window-name", QaFeatureProbe.tabNames.size() == owner.windows().size()
                && owner.windows().entrySet().stream().allMatch(entry -> entry.getValue().title().equals(QaFeatureProbe.tabNames.get(entry.getKey()))));
        check(name + "-permanent-inventory-and-character", QaFeatureProbe.tabNames.containsKey(owner.model().playerInventory())
                && QaFeatureProbe.tabNames.containsKey(owner.model().character()));
    }

    private static void setAnimations(boolean enabled) {
        try { owner.config().setAnimationsEnabled(enabled); } catch (Exception failure) { throw new IllegalStateException(failure); }
    }
    private static Object nativeWindow(UUID id) {
        for (Object window : (List<?>) field(InventoryDesktopScreen.current(mc), "windows"))
            if (id.equals(field(window, "apiInstanceId"))) return window;
        throw new IllegalStateException("Native window absent " + id);
    }
    private static RecipeBookComponent<?> guideBook() {
        return Objects.requireNonNull((RecipeBookComponent<?>) field(nativeWindow(owner.model().character()), "recipeBook"));
    }
    private static RecipeBookPage guidePage() { return (RecipeBookPage) field(guideBook(), "recipeBookPage"); }
    private static OverlayRecipeComponent guideOverlay() { return (OverlayRecipeComponent) field(guidePage(), "overlay"); }
    @SuppressWarnings("unchecked") private static List<RecipeButton> guideRecipeButtons() { return (List<RecipeButton>) field(guidePage(), "buttons"); }
    @SuppressWarnings("unchecked") private static List<AbstractWidget> guideChoiceButtons() { return ((List<AbstractWidget>) field(guideOverlay(), "recipeButtons"))
            .stream().filter(widget -> widget.visible && widget.active).toList(); }
    private static DesktopWindowBounds widgetBounds(AbstractWidget widget) {
        return new DesktopWindowBounds(widget.getX(), widget.getY(), widget.getWidth(), widget.getHeight());
    }
    private static void verifyControlSnaps(UUID id, String name) {
        List<SnapPoint> snaps = new ArrayList<>();
        ((RadialInventoryScreen) mc.gui.screen()).controlify$collectSnapPoints(snaps::add);
        var bounds = owner.desktop().presentationBounds(id); var placement = renderedLayout().windows().get(id);
        var controls = owner.desktop().visibleControls(id);
        check(name + "-native-controls-exist", !controls.isEmpty());
        for (int index = 0; index < controls.size(); index++) {
            var control = controls.get(index);
            int x = (int) Math.round(placement.x() + (control.x() + control.width() / 2.0 - bounds.x()) * placement.scale());
            int y = (int) Math.round(placement.y() + (control.y() + control.height() / 2.0 - bounds.y()) * placement.scale());
            check(name + "-rigid-control-" + index, snaps.stream().anyMatch(snap -> snap.position().x() == x && snap.position().y() == y));
        }
    }
    private static void prepareDpadTarget(UUID id, DesktopWindowBounds control) {
        var bounds = owner.desktop().presentationBounds(id); var placement = renderedLayout().windows().get(id);
        setDpadExpected(id, control);
        // SnapPoint.range uses the smaller dimension, whereas logical hit testing uses the
        // actual rectangle. Stay outside its full width, including the 1px rounding tolerance.
        int halfWidth = Math.max(1, (int) Math.ceil(control.width() * placement.scale() / 2.0));
        snapCursor(expectedSnapX - halfWidth - 3, expectedSnapY);
    }
    private static void nativeCursorNeutralReady(String name, Runnable prepareSource) {
        add(name + "-native-cursor-ready", () -> {
            driver.buttons.remove(GamepadInputs.DPAD_DOWN_BUTTON); driver.buttons.remove(GamepadInputs.MISC_1_BUTTON);
            driver.buttons.remove(GamepadInputs.SOUTH_BUTTON); driver.buttons.remove(GamepadInputs.MISC_2_BUTTON); driver.stick(0, 0);
            creativeNavigationNeutralBefore = QaFeatureProbe.navigationNeutralSamples;
            Controlify.instance().virtualMouseHandler().enableVirtualMouse();
        }, () -> {
            var mouse = Controlify.instance().virtualMouseHandler();
            if (!mouse.isVirtualMouseEnabled()) {
                mouse.enableVirtualMouse(); creativeNavigationNeutralBefore = QaFeatureProbe.navigationNeutralSamples; return false;
            }
            var quickMove = ControlifyBindings.INV_QUICK_MOVE.on(controller);
            var select = ControlifyBindings.INV_SELECT.on(controller);
            var leftClick = ControlifyBindings.VMOUSE_LCLICK.on(controller);
            var rightClick = ControlifyBindings.VMOUSE_RCLICK.on(controller);
            return nativePointerReady() && !quickMove.digitalNow() && !quickMove.digitalPrev()
                && !select.digitalNow() && !select.digitalPrev() && !leftClick.digitalNow() && !leftClick.digitalPrev()
                && !rightClick.digitalNow() && !rightClick.digitalPrev()
                && QaFeatureProbe.navigationNeutralSamples > creativeNavigationNeutralBefore;
        }, () -> { check(name + "-enabled-neutral-native-sample", nativePointerReady()); prepareSource.run(); }, 0);
    }
    private static void prepareAdaptiveCursor(UUID id, Runnable fixture) {
        adaptiveCursorWindow = id; adaptiveCursorFixture = fixture;
        observedCursorAt = QaFeatureProbe.cursorCalls; fixture.run();
        var mouse = Controlify.instance().virtualMouseHandler(); var window = mc.getWindow();
        adaptiveCursorX = (int) Math.round(mouse.getCurrentX(1) * (double) window.getGuiScaledWidth() / window.getScreenWidth());
        adaptiveCursorY = (int) Math.round(mouse.getCurrentY(1) * (double) window.getGuiScaledHeight() / window.getScreenHeight());
    }
    private static boolean adaptiveCursorReady() {
        var mouse = Controlify.instance().virtualMouseHandler(); var window = mc.getWindow();
        int x = (int) Math.round(mouse.getCurrentX(1) * (double) window.getGuiScaledWidth() / window.getScreenWidth());
        int y = (int) Math.round(mouse.getCurrentY(1) * (double) window.getGuiScaledHeight() / window.getScreenHeight());
        if (!nativePointerReady() || x != adaptiveCursorX || y != adaptiveCursorY) {
            mouse.enableVirtualMouse(); prepareAdaptiveCursor(adaptiveCursorWindow, adaptiveCursorFixture); return false;
        }
        // Match a real render sample to the prepared target pane and its native scale selection.
        // The actual pose transform remains the subject of the original exact assertions.
        double targetPaneScale = renderedLayout().windows().get(adaptiveCursorWindow).scale();
        return QaFeatureProbe.cursorCalls > observedCursorAt && QaFeatureProbe.cursorGuiX == adaptiveCursorX && QaFeatureProbe.cursorGuiY == adaptiveCursorY
            && Math.abs(QaFeatureProbe.cursorExpectedScale - targetPaneScale) < .001;
    }
    private static void setDpadExpected(UUID id, DesktopWindowBounds control) {
        var bounds = owner.desktop().presentationBounds(id); var placement = renderedLayout().windows().get(id);
        expectedSnapX = (int) Math.round(placement.x() + (control.x() + control.width() / 2.0 - bounds.x()) * placement.scale());
        expectedSnapY = (int) Math.round(placement.y() + (control.y() + control.height() / 2.0 - bounds.y()) * placement.scale());
    }
    private static void verifyDpadTarget(String name) {
        var mouse = Controlify.instance().virtualMouseHandler(); var window = mc.getWindow();
        int x = (int) Math.round(mouse.getCurrentX(0) * (double) window.getGuiScaledWidth() / window.getScreenWidth());
        int y = (int) Math.round(mouse.getCurrentY(0) * (double) window.getGuiScaledHeight() / window.getScreenHeight());
        boolean reached = Math.abs(x - expectedSnapX) <= 1 && Math.abs(y - expectedSnapY) <= 1;
        if (!reached) record(name + "-snap-diagnostics", "OBSERVED", dpadDiagnostics());
        check(name, reached);
    }
    private static String dpadDiagnostics() {
        var mouse = Controlify.instance().virtualMouseHandler(); var window = mc.getWindow();
        double xScale = (double) window.getGuiScaledWidth() / window.getScreenWidth();
        double yScale = (double) window.getGuiScaledHeight() / window.getScreenHeight();
        List<SnapPoint> collected = new ArrayList<>();
        ((RadialInventoryScreen) mc.gui.screen()).controlify$collectSnapPoints(collected::add);
        return "expectedGUI=" + expectedSnapX + "," + expectedSnapY
                + ", currentGUI=" + mouse.getCurrentX(0) * xScale + "," + mouse.getCurrentY(0) * yScale
                + ", targetGUI=" + mouse.getCurrentX(1) * xScale + "," + mouse.getCurrentY(1) * yScale
                + ", lastNativeSnap=" + field(mouse, "lastSnappedPoint")
                + ", cachedNativeSnaps=" + field(mouse, "snapPoints")
                + ", liveCollectedSnaps=" + collected;
    }
    private static void pointAtControl(UUID id, DesktopWindowBounds control) {
        var bounds = owner.desktop().presentationBounds(id); var placement = renderedLayout().windows().get(id);
        snapCursor((int) Math.round(placement.x() + (control.x() + control.width() / 2.0 - bounds.x()) * placement.scale()),
                (int) Math.round(placement.y() + (control.y() + control.height() / 2.0 - bounds.y()) * placement.scale()));
    }
    private static void snapCursor(int x, int y) {
        Controlify.instance().virtualMouseHandler().snapToPoint(new SnapPoint(x, y, 1),
                new Vector2d((double) mc.getWindow().getGuiScaledWidth() / mc.getWindow().getScreenWidth(),
                        (double) mc.getWindow().getGuiScaledHeight() / mc.getWindow().getScreenHeight()));
        ((RadialInventoryScreen) mc.gui.screen()).mouseMoved(x, y);
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
            var furnace = (AbstractFurnaceBlockEntity) player().level().getBlockEntity(furnacePos);
            furnace.setItem(2, new ItemStack(Items.IRON_INGOT, 8));
            var recipe = mc.getSingleplayerServer().getRecipeManager().byKey(net.minecraft.resources.ResourceKey.create(Registries.RECIPE, Identifier.withDefaultNamespace("iron_ingot_from_smelting_raw_iron"))).orElseThrow();
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
            var furnace = (AbstractFurnaceBlockEntity) player().level().getBlockEntity(furnacePos);
            furnace.setItem(2, new ItemStack(Items.IRON_INGOT, 8));
            var recipe = mc.getSingleplayerServer().getRecipeManager().byKey(net.minecraft.resources.ResourceKey.create(Registries.RECIPE, Identifier.withDefaultNamespace("iron_ingot_from_smelting_raw_iron"))).orElseThrow();
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
        addTallNativeGuideScenarios();
        addGhostLayoutScenarios();
        addContextualScenarios();
        addCharacterContextualScenarios();
        addCookingContextualScenarios();
        addCreativeNavigationScenarios();
        add("contextual-presentation-native-book-sprite-regression", () -> {}, () -> true, () -> {
            record("native-vanilla-book-sprite-observation", "OBSERVED", "contextualEnabled="
                + QaFeatureProbe.vanillaBookSpritesContextualEnabled + ",legacyOptOut=" + QaFeatureProbe.vanillaBookSpritesLegacyOptOut);
            check("contextual-presentation-hides-vanilla-book-sprites-including-furnace-ghosts",
                QaFeatureProbe.vanillaBookSpritesContextualEnabled == 0 && QaNativeGuideProbe.workspaceFrames > 0);
            check("native-guide-removed-pane-book-never-drawn", QaNativeGuideProbe.rawPaneBookDraws == 0 && QaNativeGuideProbe.workspaceFrames > 0);
            check("native-guide-old-manual-controls-never-drawn", QaNativeGuideProbe.manualControlTextDraws == 0
                && QaFeatureProbe.recipePreviewFrames > 0 && QaFeatureProbe.creativeTabFrames > 0);
            check("native-guide-contextual-surfaces-clear-bottom-lists", QaNativeGuideProbe.overlayFrames > 0
                && QaNativeGuideProbe.overlayOverlapViolations.isEmpty());
        }, 3);
    }

    private static com.salts_inventory_update.controlify.ContextualRadialOverlay contextualOverlay() {
        return (com.salts_inventory_update.controlify.ContextualRadialOverlay) field(mc.gui.screen(), "contextual");
    }
    private static com.salts_inventory_update.api.client.desktop.DesktopContextualMenu contextualSnapshot() {
        return (com.salts_inventory_update.api.client.desktop.DesktopContextualMenu) field(contextualOverlay(), "snapshot");
    }
    @SuppressWarnings("unchecked")
    private static List<com.salts_inventory_update.api.client.desktop.DesktopContextualMenu.Recipe> contextualEntries() {
        return (List<com.salts_inventory_update.api.client.desktop.DesktopContextualMenu.Recipe>) field(contextualOverlay(),"indexedEntries");
    }
    private static void aimContextualRecipe(Item output, boolean craftable) {
        var entries = contextualEntries();
        int index = -1;
        for (int i = 0; i < entries.size(); i++) if (entries.get(i).output().is(output) && entries.get(i).craftable() == craftable) { index = i; break; }
        if (index < 0) throw new IllegalStateException("Contextual recipe missing: " + output + " craftable=" + craftable);
        double outerAngle = com.salts_inventory_update.controlify.model.ContextualWheelSelection.outerMarkerAngle(index, entries.size());
        driver.rightX = (float) Math.sin(outerAngle); driver.rightY = (float) -Math.cos(outerAngle);
        driver.stick(0, 0);
    }
    private static boolean contextualRecipeReady(Item output, boolean craftable) {
        var entries = contextualEntries();
        int displayed = contextualSelection().displayedSelected(entries.size());
        return displayed >= 0 && displayed < entries.size() && entries.get(displayed).output().is(output)
            && entries.get(displayed).craftable() == craftable && entries.get(displayed).id().equals(QaFeatureProbe.renderedRecipeId);
    }
    private static void verifyContextualRecipe(Item output, boolean craftable) {
        var entries = contextualEntries();
        var selection = (com.salts_inventory_update.controlify.model.ContextualWheelSelection) field(contextualOverlay(), "selection");
        int selected = selection.selected(entries.size());
        int expected = -1;
        for (int i = 0; i < entries.size(); i++) if (entries.get(i).output().is(output) && entries.get(i).craftable() == craftable) { expected = i; break; }
        var input = controller.input().orElseThrow();
        var processed = input.stateNow();
        var raw = input.rawStateNow();
        float actualRightX = processed.getAxisState(GamepadInputs.RIGHT_STICK_AXIS_RIGHT) - processed.getAxisState(GamepadInputs.RIGHT_STICK_AXIS_LEFT);
        float actualRightY = processed.getAxisState(GamepadInputs.RIGHT_STICK_AXIS_DOWN) - processed.getAxisState(GamepadInputs.RIGHT_STICK_AXIS_UP);
        float actualLeftX = processed.getAxisState(GamepadInputs.LEFT_STICK_AXIS_RIGHT) - processed.getAxisState(GamepadInputs.LEFT_STICK_AXIS_LEFT);
        float actualLeftY = processed.getAxisState(GamepadInputs.LEFT_STICK_AXIS_DOWN) - processed.getAxisState(GamepadInputs.LEFT_STICK_AXIS_UP);
        record("contextual-wheel-native-stick-observation-" + output, "OBSERVED", "recipeCount=" + entries.size()
            + ", expectedIndex=" + expected + ", selectedIndex=" + selected
            + ", continuousPosition=" + selection.position() + ", visibleStart=" + selection.visibleStart(entries.size()) + ", visibleSize=" + selection.visibleSize(entries.size())
            + ", expectedRecipe=" + (expected >= 0 ? entries.get(expected).id() + ":" + entries.get(expected).output() : "missing")
            + ", actualRecipe=" + (selected >= 0 ? entries.get(selected).id() + ":" + entries.get(selected).output() : "missing")
            + ", commandedRight=" + driver.rightX + "," + driver.rightY + ", commandedLeft=" + driver.x + "," + driver.y
            + ", processedRight=" + actualRightX + "," + actualRightY + ", processedLeft=" + actualLeftX + "," + actualLeftY
            + ", processedOuterRecipe=" + com.salts_inventory_update.controlify.model.ContextualWheelSelection.sector(actualRightX, actualRightY, entries.size())
            + ", rawRight=" + (raw.getAxisState(GamepadInputs.RIGHT_STICK_AXIS_RIGHT) - raw.getAxisState(GamepadInputs.RIGHT_STICK_AXIS_LEFT))
                + "," + (raw.getAxisState(GamepadInputs.RIGHT_STICK_AXIS_DOWN) - raw.getAxisState(GamepadInputs.RIGHT_STICK_AXIS_UP))
            + ", rawLeft=" + (raw.getAxisState(GamepadInputs.LEFT_STICK_AXIS_RIGHT) - raw.getAxisState(GamepadInputs.LEFT_STICK_AXIS_LEFT))
                + "," + (raw.getAxisState(GamepadInputs.LEFT_STICK_AXIS_DOWN) - raw.getAxisState(GamepadInputs.LEFT_STICK_AXIS_UP)));
        check("contextual-dual-stick-real-recipe-selected-" + output, selected >= 0 && entries.get(selected).output().is(output)
            && entries.get(selected).craftable() == craftable);
        check("contextual-visible-recipe-preview-matches-selection-" + output, contextualRecipeReady(output, craftable));
        driver.rightX = 0; driver.rightY = 0; driver.stick(0, 0);
    }
    private static int contextualGridCount(Item item) {
        return ((CraftingMenu) serverMenu(CraftingMenu.class)).getInputGridSlots().stream()
            .filter(slot -> slot.getItem().is(item)).mapToInt(slot -> slot.getItem().getCount()).sum();
    }
    private static int contextualLogCount() {
        return inventoryCount(Items.OAK_LOG) + contextualGridCount(Items.OAK_LOG) + count(contextualIngredientPos, Items.OAK_LOG);
    }
    private static void addContextualScenarios() {
        add("contextual-all-open-ingredient-container-fixture", () -> server(() -> {
            contextualIngredientPos = barrelPos.offset(0, 0, 5);
            var level = player().level();
            level.setBlock(contextualIngredientPos.below(), Blocks.STONE.defaultBlockState(), 3);
            level.setBlock(contextualIngredientPos, Blocks.CHEST.defaultBlockState(), 3);
            return null;
        }), ControlifyGameplayHarness::serverDone, () -> {}, 2);
        openContainer("contextual-open-ingredient-container", () -> contextualIngredientPos,
            () -> contextualIngredientWindow = newestOther());
        focusPair("contextual-crafting-barrel-pair", () -> tableWindow, () -> barrelWindow);
        add("contextual-native-ingredient-fixture", () -> server(() -> {
            player().getInventory().clearContent(); player().inventoryMenu.setCarried(ItemStack.EMPTY);
            CraftingMenu crafting = (CraftingMenu) serverMenu(CraftingMenu.class);
            for (Slot slot : crafting.getInputGridSlots()) slot.set(ItemStack.EMPTY);
            crafting.getInputGridSlots().getFirst().set(new ItemStack(Items.BIRCH_LOG));
            Container barrel = (Container) player().level().getBlockEntity(barrelPos);
            barrel.clearContent();
            Container ingredients = (Container) player().level().getBlockEntity(contextualIngredientPos);
            ingredients.clearContent(); ingredients.setItem(0, new ItemStack(Items.OAK_LOG, 2));
            crafting.broadcastChanges(); player().inventoryMenu.broadcastChanges();
            return inventoryCount(Items.OAK_LOG) == 0 && contextualGridCount(Items.OAK_LOG) == 0
                && count(contextualIngredientPos, Items.OAK_LOG) == 2 && count(barrelPos, Items.OAK_PLANKS) == 0;
        }), ControlifyGameplayHarness::serverDone, () -> {
            check("contextual-authentic-fixture", (Boolean) serverTask.join());
            check("contextual-ingredient-container-open-but-not-displayed", owner.windows().containsKey(contextualIngredientWindow)
                && !owner.model().focused().equals(contextualIngredientWindow) && !owner.model().selected().equals(contextualIngredientWindow));
        }, 12);
        addContextualBookHintScenarios();
        add("contextual-animated-presentation-fixture",() -> {
            contextualAnimationsBefore=owner.config().animationsEnabled();
            driver.rightTrigger=0;driver.rightX=driver.rightY=0;driver.stick(0,0);
            setAnimations(true);
        },() -> nativePointerReady() && !contextualOverlay().presenting(),() -> {
            check("contextual-transition-fixture-explicitly-enables-animations",owner.config().animationsEnabled());
        },6);
        add("contextual-point-at-crafting-native-slot", () -> pointAtNativeSlot(tableWindow, slot -> slot.index == 1),
            ControlifyGameplayHarness::nativePointerReady, () -> {}, 2);
        add("contextual-held-RT-opens-native-crafting-wheel", () -> {
            contextualFrameBefore=QaContextualPresentationProbe.frames;
            cursorBeforeX = Controlify.instance().virtualMouseHandler().getCurrentX(0);
            var placement=renderedLayout().windows().get(tableWindow);var bounds=owner.desktop().presentationBounds(tableWindow);
            contextualSourceBefore=new com.salts_inventory_update.controlify.model.ContextualPresentationLayout.Rect(
                placement.x(),placement.y(),bounds.width()*placement.scale(),bounds.height()*placement.scale());
            driver.rightTrigger = 1;
        }, () -> mc.gui.screen() instanceof RadialInventoryScreen screen && screen.contextualRadialActive()
            && QaContextualPresentationProbe.frames>contextualFrameBefore && QaContextualPresentationProbe.last().held()
            && QaContextualPresentationProbe.currentLive(tableWindow), () -> {
            check("contextual-source-is-hovered-crafting-table", contextualOverlay().source().equals(tableWindow));
            check("contextual-native-crafting-content", contextualOverlay().kind() == com.salts_inventory_update.api.client.desktop.DesktopContextualMenu.Kind.CRAFTING);
            check("contextual-all-unlocked-recipes-in-continuous-carousel", contextualSnapshot().recipes().size() > 100);
            check("contextual-wheel-real-native-ingredient-count", contextualSnapshot().recipes().stream().filter(recipe -> recipe.output().is(Items.OAK_PLANKS) && recipe.craftable())
                .anyMatch(recipe -> recipe.requirements().stream().anyMatch(ingredient -> ingredient.available() == 2 && ingredient.required() == 1)));
            check("contextual-textured-opening-shares-native-source-frame",QaContextualPresentationProbe.last().held()
                && QaContextualPresentationProbe.last().frame().progress()<1 && QaContextualPresentationProbe.openingFrames>0);
            check("contextual-opening-starts-at-actual-side-source",contextualOverlay().originalSourceRect().equals(contextualSourceBefore));
            capture("41a-textured-wheel-opening-live-source");
        }, 0);
        assertServer("contextual-enter-clears-input-without-granting-derived-result",() -> contextualGridCount(Items.BIRCH_LOG)==0
            && inventoryCount(Items.BIRCH_LOG)+count(barrelPos,Items.BIRCH_LOG)+count(contextualIngredientPos,Items.BIRCH_LOG)==1
            && inventoryCount(Items.BIRCH_PLANKS)+count(barrelPos,Items.BIRCH_PLANKS)+count(contextualIngredientPos,Items.BIRCH_PLANKS)==0);
        addContextualPresentationScenarios();
        button("contextual-DPAD-right-category", GamepadInputs.DPAD_RIGHT_BUTTON,
            () -> {
                check("contextual-category-next-native-binding", (Integer) field(contextualOverlay(), "category") == 1);
                check("contextual-category-row-renders-every-category", QaFeatureProbe.categoryCount == contextualSnapshot().categories().size()
                    && QaFeatureProbe.selectedCategoryCount == 1);
                capture("31a-selected-category-header-row");
            });
        button("contextual-DPAD-left-category", GamepadInputs.DPAD_LEFT_BUTTON,
            () -> check("contextual-category-restored-native-binding", (Integer) field(contextualOverlay(), "category") == 0));
        button("contextual-DPAD-up-craftable-filter", GamepadInputs.DPAD_UP_BUTTON,
            () -> check("contextual-craftable-filter-enabled-native-binding", (Boolean) field(contextualOverlay(), "craftableOnly")));
        button("contextual-DPAD-up-all-filter", GamepadInputs.DPAD_UP_BUTTON,
            () -> check("contextual-craftable-filter-disabled-native-binding", !(Boolean) field(contextualOverlay(), "craftableOnly")));
        addContextualAlphabetScenarios();
        addNativeUiMuteScenarios();
        addContextualCarouselScenarios();
        add("contextual-dual-stick-select-planks", () -> aimContextualRecipe(Items.OAK_PLANKS, true), () -> contextualRecipeReady(Items.OAK_PLANKS, true), () -> {
            verifyContextualRecipe(Items.OAK_PLANKS, true); capture("14-contextual-crafting-dual-wheel-ingredients-grid");
        }, 4);
        assertServer("contextual-hover-physically-stages-from-hidden-open-container",() -> contextualGridCount(Items.OAK_LOG)==1
            && count(contextualIngredientPos,Items.OAK_LOG)==1 && inventoryCount(Items.OAK_LOG)==0 && contextualLogCount()==2);
        observeNativeGuide("craftable-recipe", "RECIPE", "Close all windows", "Release: return to pair", "Craft into opposite window",
            "Browse recipes", "Choose recipe", "Hold: rotate recipes", "Change recipe category", "Toggle craftable recipes");
        add("compact-native-recipe-plain-stick-glyphs",() -> {},() -> true,() -> {
            var leftGlyph = Controlify.instance().inputFontMapper().getComponentFromInputs(controller.info().type().namespace(),List.of(GamepadInputs.LEFT_STICK_BUTTON));
            var rightGlyph = Controlify.instance().inputFontMapper().getComponentFromInputs(controller.info().type().namespace(),List.of(GamepadInputs.RIGHT_STICK_BUTTON));
            check("compact-native-recipe-sticks-have-one-undirected-glyph-each",QaNativeGuideProbe.workspace.glyph("Choose recipe",leftGlyph)
                && QaNativeGuideProbe.workspace.glyph("Browse recipes",rightGlyph)
                && leftGlyph.getString().codePointCount(0,leftGlyph.getString().length())==1
                && rightGlyph.getString().codePointCount(0,rightGlyph.getString().length())==1);
            capture("37c-compact-guide-recipe-plain-sticks");
        },2);
        button("contextual-native-A-crafts-to-opposite-barrel", GamepadInputs.SOUTH_BUTTON, () -> {});
        assertServer("contextual-one-craft-ingredients-output-cursor-conserved", () ->
            count(barrelPos, Items.OAK_PLANKS) == 4 && inventoryCount(Items.OAK_PLANKS) == 0
                && contextualLogCount() == 1
                && player().inventoryMenu.getCarried().isEmpty());
        add("contextual-native-destination-displays-crafted-planks",() -> {},
            () -> nativeContextualDestinationHas(barrelWindow,Items.OAK_PLANKS,4),() -> {
            check("contextual-destination-real-native-planks-slot",nativeContextualDestinationHas(barrelWindow,Items.OAK_PLANKS,4));
            capture("42-native-destination-crafted-planks");
        },2);
        add("contextual-missing-recipe-select", () -> aimContextualRecipe(Items.IRON_CHESTPLATE, false), () -> contextualRecipeReady(Items.IRON_CHESTPLATE, false),
            () -> verifyContextualRecipe(Items.IRON_CHESTPLATE, false), 5);
        assertServer("contextual-hover-change-restores-unused-exact-origin",() -> contextualGridCount(Items.OAK_LOG)==0
            && ((Container)player().level().getBlockEntity(contextualIngredientPos)).getItem(0).is(Items.OAK_LOG)
            && ((Container)player().level().getBlockEntity(contextualIngredientPos)).getItem(0).getCount()==1);
        add("contextual-native-missing-ghosts-visible",() -> {},() -> owner.desktop().recipeHoverGhosts(tableWindow).size()==8
            && QaContextualPresentationProbe.ghostNativeItems>0,() -> {
            check("contextual-missing-ghosts-red-translucent-over-real-native-inputs",QaContextualPresentationProbe.ghostFrames>0
                && QaContextualPresentationProbe.ghostNativeItems==QaContextualPresentationProbe.ghostNativeRedWashes
                && owner.desktop().recipeHoverGhosts(tableWindow).size()==8
                && owner.desktop().recipeHoverGhosts(tableWindow).stream().allMatch(ghost -> ghost.item().is(Items.IRON_INGOT))
                && owner.desktop().recipeHoverGhosts(tableWindow).stream().map(ghost -> ghost.slotIndex()).collect(java.util.stream.Collectors.toSet())
                    .equals(Arrays.stream(GEAR_RECIPE_INPUTS).boxed().collect(java.util.stream.Collectors.toSet()))
                && QaContextualPresentationProbe.violations().isEmpty());
            capture("43-native-red-translucent-missing-ingredients");
        },2);
        observeNativeGuide("unavailable-recipe", "RECIPE", "Browse recipes", "Choose recipe", "Release: return to pair");
        add("native-guide-unavailable-recipe-no-confirm", () -> {}, () -> true, () ->
            check("native-guide-unavailable-recipe-hides-confirm", !QaNativeGuideProbe.workspace.label("Craft into opposite window")), 2);
        button("contextual-A-rejects-missing-ingredients", GamepadInputs.SOUTH_BUTTON,
            () -> check("contextual-missing-ingredients-status", field(contextualOverlay(), "status").equals("Missing ingredients")));
        assertServer("contextual-noncraftable-no-inventory-mutation", () -> count(barrelPos, Items.OAK_PLANKS) == 4
            && count(barrelPos, Items.IRON_CHESTPLATE) == 0 && contextualLogCount() == 1);
        add("contextual-fill-exact-opposite-destination", () -> server(() -> {
            Container barrel = (Container) player().level().getBlockEntity(barrelPos);
            for (int i = 0; i < barrel.getContainerSize(); i++) barrel.setItem(i, new ItemStack(Items.BEDROCK, 64));
            return null;
        }), ControlifyGameplayHarness::serverDone, () -> {}, 12);
        add("contextual-full-destination-reselect-planks", () -> aimContextualRecipe(Items.OAK_PLANKS, true), () -> contextualRecipeReady(Items.OAK_PLANKS, true),
            () -> verifyContextualRecipe(Items.OAK_PLANKS, true), 4);
        button("contextual-native-A-full-destination-reject", GamepadInputs.SOUTH_BUTTON,
            () -> check("contextual-full-destination-status", field(contextualOverlay(), "status").equals("Destination unavailable")));
        add("contextual-full-rejection-network-settle", () -> {}, () -> true, () -> {}, 12);
        add("workspace-ui-rejection-sound-snapshot",() -> nativeUiSoundsBefore=QaVisualFeatureProbe.acceptedSoundCount(),() -> true,() -> {},1);
        button("workspace-ui-native-identical-full-rejection",GamepadInputs.SOUTH_BUTTON,() ->
            check("workspace-ui-identical-rejection-is-quiet",QaVisualFeatureProbe.acceptedSoundCount()==nativeUiSoundsBefore
                && field(contextualOverlay(),"status").equals("Destination unavailable")));
        assertServer("contextual-full-destination-no-consumption-no-leak", () ->
            count(barrelPos, Items.OAK_PLANKS) == 0 && inventoryCount(Items.OAK_PLANKS) == 0
                && contextualLogCount() == 1
                && player().inventoryMenu.getCarried().isEmpty());
        add("contextual-release-RT-restores-pair", () -> { driver.rightTrigger = 0; driver.stick(0, 0); driver.rightX = 0; driver.rightY = 0; },
            () -> mc.gui.screen() instanceof RadialInventoryScreen screen && !screen.contextualRadialActive(), () -> {
                check("contextual-release-same-side-pair", owner.model().focused().equals(tableWindow) && owner.model().selected().equals(barrelWindow));
                check("contextual-release-native-cursor-preserved", Math.abs(Controlify.instance().virtualMouseHandler().getCurrentX(0) - cursorBeforeX) <= 2);
            }, 4);
        assertServer("contextual-release-restores-unused-exact-origin",() -> contextualGridCount(Items.OAK_LOG)==0
            && ((Container)player().level().getBlockEntity(contextualIngredientPos)).getItem(0).is(Items.OAK_LOG)
            && ((Container)player().level().getBlockEntity(contextualIngredientPos)).getItem(0).getCount()==1);
        add("contextual-native-remainder-fixture", () -> server(() -> {
            player().getInventory().clearContent();
            CraftingMenu crafting = (CraftingMenu) serverMenu(CraftingMenu.class);
            for (Slot slot : crafting.getInputGridSlots()) slot.set(ItemStack.EMPTY);
            ((Container) player().level().getBlockEntity(barrelPos)).clearContent();
            for (int i = 9; i < 12; i++) player().getInventory().setItem(i, new ItemStack(Items.MILK_BUCKET));
            player().getInventory().setItem(12, new ItemStack(Items.WHEAT));
            Container origins=(Container)player().level().getBlockEntity(contextualIngredientPos);origins.clearContent();
            origins.setItem(3,new ItemStack(Items.WHEAT,2));origins.setItem(4,new ItemStack(Items.SUGAR));
            ((Container)player().level().getBlockEntity(barrelPos)).setItem(0,new ItemStack(Items.SUGAR));
            player().getInventory().setItem(14, new ItemStack(Items.EGG));
            crafting.broadcastChanges(); player().inventoryMenu.broadcastChanges(); return null;
        }), ControlifyGameplayHarness::serverDone, () -> {}, 12);
        add("contextual-cake-reopen-RT", () -> { pointAtNativeSlot(tableWindow, slot -> slot.index == 1); driver.rightTrigger = 1; },
            () -> mc.gui.screen() instanceof RadialInventoryScreen screen && screen.contextualRadialActive(), () -> {}, 4);
        add("contextual-dual-stick-select-cake", () -> aimContextualRecipe(Items.CAKE, true), () -> contextualRecipeReady(Items.CAKE, true), () -> {
            verifyContextualRecipe(Items.CAKE, true); capture("14b-contextual-crafting-native-cake-remainders");
        }, 4);
        assertServer("contextual-hover-shapes-available-ingredients-across-three-open-sources",() -> {
            var slots=((CraftingMenu)serverMenu(CraftingMenu.class)).getInputGridSlots();
            Item[] shape={Items.MILK_BUCKET,Items.MILK_BUCKET,Items.MILK_BUCKET,Items.SUGAR,Items.EGG,Items.SUGAR,Items.WHEAT,Items.WHEAT,Items.WHEAT};
            for(int i=0;i<shape.length;i++) if(!slots.get(i).getItem().is(shape[i]) || slots.get(i).getItem().getCount()!=1) return false;
            return inventoryCount(Items.MILK_BUCKET)==0 && inventoryCount(Items.WHEAT)==0 && inventoryCount(Items.EGG)==0
                && count(contextualIngredientPos,Items.WHEAT)==0 && count(contextualIngredientPos,Items.SUGAR)==0 && count(barrelPos,Items.SUGAR)==0;
        });
        button("contextual-native-A-crafts-cake-with-remainders", GamepadInputs.SOUTH_BUTTON, () -> {});
        assertServer("contextual-native-cake-output-remainders-conserved", () -> count(barrelPos, Items.CAKE) == 1
            && inventoryCount(Items.CAKE) == 0 && inventoryCount(Items.BUCKET) + contextualGridCount(Items.BUCKET) == 3
            && inventoryCount(Items.MILK_BUCKET) + contextualGridCount(Items.MILK_BUCKET) == 0
            && inventoryCount(Items.WHEAT) + contextualGridCount(Items.WHEAT) == 0
            && inventoryCount(Items.SUGAR) + contextualGridCount(Items.SUGAR) == 0
            && inventoryCount(Items.EGG) + contextualGridCount(Items.EGG) == 0 && player().inventoryMenu.getCarried().isEmpty()
            && count(contextualIngredientPos,Items.WHEAT)==0 && count(contextualIngredientPos,Items.SUGAR)==0 && count(barrelPos,Items.SUGAR)==0);
        add("contextual-native-destination-displays-crafted-cake",() -> {},
            () -> nativeContextualDestinationHas(barrelWindow,Items.CAKE,1),() -> {
            check("contextual-destination-real-native-cake-slot",nativeContextualDestinationHas(barrelWindow,Items.CAKE,1));
            capture("42b-native-destination-crafted-cake");
        },2);
        add("contextual-final-release-RT", () -> { driver.rightTrigger = 0; driver.rightX = 0; driver.rightY = 0; driver.stick(0, 0); },
            () -> mc.gui.screen() instanceof RadialInventoryScreen screen && !screen.contextualRadialActive(),
            () -> capture("14c-contextual-crafted-output-in-opposite-container"), 4);
        addContextualHoverLifecycleScenarios();
    }

    /** Counts real owned stacks once even when several native menus expose the same inventory. */
    @SuppressWarnings("unchecked")
    private static int contextualOwnedOriginLogs() {
        return contextualOwnedCount(Items.OAK_LOG,contextualOriginData());
    }
    private static net.minecraft.world.item.component.CustomData contextualOriginData() {
        var tag=new net.minecraft.nbt.CompoundTag();tag.putString("qa_hover_origin","exact-source-slot");
        return net.minecraft.world.item.component.CustomData.of(tag);
    }
    @SuppressWarnings("unchecked")
    private static int contextualOwnedCount(Item expected,net.minecraft.world.item.component.CustomData data) {
        var seen=new IdentityHashMap<Container,Set<Integer>>();var menus=new ArrayList<AbstractContainerMenu>();
        menus.add(player().inventoryMenu);
        Object state=((Map<ServerPlayer,?>)field(DesktopContainerSessions.class,"PLAYERS")).get(player());
        for(Object session:((Map<Integer,?>)field(state,"sessions")).values()) menus.add((AbstractContainerMenu)field(session,"menu"));
        int total=0;
        for(var menu:menus) for(var slot:menu.slots) {
            if(menu instanceof net.minecraft.world.inventory.AbstractCraftingMenu && slot.index==0) continue;
            if(!seen.computeIfAbsent(slot.container,ignored -> new HashSet<>()).add(slot.getContainerSlot())) continue;
            var item=slot.getItem();
            if(item.is(expected) && (data==null || data.equals(item.get(DataComponents.CUSTOM_DATA)))) total+=item.getCount();
        }
        return total;
    }
    private static String contextualOwnedLocations(Item expected) {
        var seen=new IdentityHashMap<Container,Set<Integer>>();var menus=new ArrayList<AbstractContainerMenu>();menus.add(player().inventoryMenu);
        Object state=((Map<?,?>)field(DesktopContainerSessions.class,"PLAYERS")).get(player());
        for(Object session:((Map<?,?>)field(state,"sessions")).values()) menus.add((AbstractContainerMenu)field(session,"menu"));
        var locations=new ArrayList<String>();
        for(var menu:menus) for(var slot:menu.slots) {
            if(menu instanceof net.minecraft.world.inventory.AbstractCraftingMenu && slot.index==0) continue;
            if(!seen.computeIfAbsent(slot.container,ignored -> new HashSet<>()).add(slot.getContainerSlot()) || !slot.getItem().is(expected)) continue;
            locations.add(menu.getClass().getSimpleName()+"[menuSlot="+slot.index+",physical="+slot.getContainerSlot()+",count="+slot.getItem().getCount()+"]");
        }
        return locations.toString();
    }
    private static void seedContextualHoverOrigin() {
        player().getInventory().clearContent();player().inventoryMenu.setCarried(ItemStack.EMPTY);
        for(var slot:((CraftingMenu)serverMenu(CraftingMenu.class)).getInputGridSlots()) slot.set(ItemStack.EMPTY);
        ((Container)player().level().getBlockEntity(barrelPos)).clearContent();
        var origin=(Container)player().level().getBlockEntity(contextualIngredientPos);origin.clearContent();
        var log=new ItemStack(Items.OAK_LOG);log.set(DataComponents.CUSTOM_DATA,contextualOriginData());origin.setItem(7,log);
        serverMenu(CraftingMenu.class).broadcastChanges();player().inventoryMenu.broadcastChanges();
    }
    private static boolean contextualExactOriginRestored() {
        var original=((Container)player().level().getBlockEntity(contextualIngredientPos)).getItem(7);
        return original.is(Items.OAK_LOG) && original.getCount()==1
            && contextualOriginData().equals(original.get(DataComponents.CUSTOM_DATA)) && contextualOwnedOriginLogs()==1;
    }
    private static void openContextualHoverOrigin(String name) {
        add(name+"-RT-open",() -> { pointAtNativeSlot(tableWindow,slot -> slot.index==1);driver.rightTrigger=1; },
            () -> contextualOverlay().active(),() -> {},3);
        add(name+"-select-planks",() -> aimContextualRecipe(Items.OAK_PLANKS,true),() -> contextualRecipeReady(Items.OAK_PLANKS,true),
            () -> { driver.rightX=driver.rightY=0; },3);
        assertServer(name+"-physical-native-input",() -> contextualGridCount(Items.OAK_LOG)==1
            && ((Container)player().level().getBlockEntity(contextualIngredientPos)).getItem(7).isEmpty() && contextualOwnedOriginLogs()==1);
    }
    private static Map<String,Object> fillContextualStorage(boolean seedInitialInput) {
        contextualFullStorageBefore.clear();var menus=new ArrayList<AbstractContainerMenu>();menus.add(player().inventoryMenu);
        Object state=((Map<?,?>)field(DesktopContainerSessions.class,"PLAYERS")).get(player());
        for(Object session:((Map<?,?>)field(state,"sessions")).values()) menus.add((AbstractContainerMenu)field(session,"menu"));
        var source=(CraftingMenu)serverMenu(CraftingMenu.class);var seen=new IdentityHashMap<Container,Set<Integer>>();
        for(var menu:menus) for(var slot:menu.slots) {
            if(menu instanceof net.minecraft.world.inventory.AbstractCraftingMenu && slot.index==0
                || menu==source && source.getInputGridSlots().contains(slot)) continue;
            if(!slot.mayPlace(new ItemStack(Items.BEDROCK)) || !seen.computeIfAbsent(slot.container,ignored -> new HashSet<>()).add(slot.getContainerSlot())) continue;
            contextualFullStorageBefore.put(slot,slot.getItem().copy());slot.set(new ItemStack(Items.BEDROCK,64));
        }
        if(seedInitialInput) {
            for(var slot:source.getInputGridSlots()) slot.set(ItemStack.EMPTY);
            source.getInputGridSlots().getFirst().set(new ItemStack(Items.BIRCH_LOG));
        }
        List<Slot> eligible;
        try {
            var method=DesktopContainerSessions.class.getDeclaredMethod("contextualRecipeSourceSlots",ServerPlayer.class,state.getClass(),Set.class);method.setAccessible(true);
            @SuppressWarnings("unchecked") var actual=(List<Slot>)method.invoke(null,player(),state,new HashSet<>(source.getInputGridSlots()));eligible=actual;
        } catch(ReflectiveOperationException error) {throw new IllegalStateException(error);}
        boolean full=!eligible.isEmpty() && eligible.stream().allMatch(slot -> slot.getItem().is(Items.BEDROCK) && slot.getItem().getCount()==64);
        String capacity="eligibleSlots="+eligible.size()+",full="+full+",nonFull="+eligible.stream()
            .filter(slot -> !slot.getItem().is(Items.BEDROCK) || slot.getItem().getCount()!=64)
            .map(slot -> "menu="+menus.stream().filter(menu -> menu.slots.contains(slot)).map(menu -> menu.getClass().getSimpleName()).toList()
                +",slot="+slot.index+",physical="+slot.getContainerSlot()+",item="+slot.getItem()).toList();
        for(var menu:menus) menu.broadcastChanges();return Map.of("full",full,"capacity",capacity);
    }
    private static void restoreContextualStorageFixture() {
        contextualFullStorageBefore.forEach((slot,item) -> slot.set(item.copy()));contextualFullStorageBefore.clear();
        serverMenu(CraftingMenu.class).broadcastChanges();player().inventoryMenu.broadcastChanges();
    }
    private static List<net.minecraft.world.entity.item.ItemEntity> contextualOriginDropEntities() {
        return player().level().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,player().getBoundingBox().inflate(16),
            entity -> entity.getItem().is(Items.OAK_LOG) && contextualOriginData().equals(entity.getItem().get(DataComponents.CUSTOM_DATA)));
    }
    private static void addContextualHoverLifecycleScenarios() {
        add("contextual-hover-origin-fixture",() -> server(() -> { seedContextualHoverOrigin();return null; }),ControlifyGameplayHarness::serverDone,() -> {},10);
        openContextualHoverOrigin("contextual-hover-LT-interruption");
        add("contextual-hover-LT-interruption-native",() -> {driver.rightTrigger=0;driver.trigger=1;aimWindow(barrelWindow);},
            () -> owner.radialActive() && !contextualOverlay().presenting(),() -> {},2);
        assertServer("contextual-hover-LT-interruption-restores-exact-components-and-origin",() -> contextualGridCount(Items.OAK_LOG)==0 && contextualExactOriginRestored());
        add("contextual-hover-LT-interruption-release",() -> {driver.trigger=driver.rightTrigger=0;driver.stick(0,0);},
            () -> !owner.radialActive(),() -> {},5);
        openContextualHoverOrigin("contextual-hover-full-origin");
        add("contextual-hover-fill-original-container",() -> server(() -> {
            var origin=(Container)player().level().getBlockEntity(contextualIngredientPos);
            for(int i=0;i<origin.getContainerSize();i++) origin.setItem(i,new ItemStack(Items.BEDROCK,64));return null;
        }),ControlifyGameplayHarness::serverDone,() -> {},4);
        add("contextual-hover-full-origin-change",() -> aimContextualRecipe(Items.IRON_CHESTPLATE,false),
            () -> contextualRecipeReady(Items.IRON_CHESTPLATE,false),() -> {driver.rightX=driver.rightY=0;},3);
        assertServer("contextual-hover-full-origin-safe-fallback-without-loss-or-duplication",() -> contextualGridCount(Items.OAK_LOG)==0
            && contextualOwnedOriginLogs()==1 && count(contextualIngredientPos,Items.BEDROCK)==27*64 && count(contextualIngredientPos,Items.OAK_LOG)==0
            && player().inventoryMenu.getCarried().isEmpty());
        add("contextual-hover-full-origin-release",() -> {driver.rightTrigger=0;driver.stick(0,0);},() -> !contextualOverlay().presenting(),() -> {},5);
        add("contextual-hover-all-storage-full-return-fixture",() -> server(() -> {seedContextualHoverOrigin();return null;}),ControlifyGameplayHarness::serverDone,() -> {},10);
        openContextualHoverOrigin("contextual-hover-all-storage-full-return");
        add("contextual-hover-fills-every-return-destination",() -> server(() -> fillContextualStorage(false)),ControlifyGameplayHarness::serverDone,() -> {
            var preflight=(Map<?,?>)serverTask.join();record("contextual-full-return-actual-eligible-capacity","OBSERVED",preflight.get("capacity").toString());
            check("contextual-full-return-fixture-covers-all-actual-eligible-slots",Boolean.TRUE.equals(preflight.get("full")));
        },2);
        add("contextual-hover-all-storage-full-native-release",() -> {driver.rightTrigger=0;driver.rightX=driver.rightY=0;driver.stick(0,0);},
            () -> !contextualOverlay().active(),() -> {},2);
        assertServer("contextual-hover-all-full-return-preserves-one-native-item-entity",() -> {
            int owned=contextualOwnedOriginLogs();var drops=contextualOriginDropEntities();int dropped=drops.stream().mapToInt(entity -> entity.getItem().getCount()).sum();
            return contextualGridCount(Items.OAK_LOG)==0 && owned==0 && drops.size()==1 && dropped==1 && owned+dropped==1
                && player().inventoryMenu.getCarried().isEmpty();
        });
        add("contextual-hover-all-full-return-disposable-fixture-cleanup",() -> server(() -> {
            restoreContextualStorageFixture();contextualOriginDropEntities().forEach(net.minecraft.world.entity.Entity::discard);return null;
        }),ControlifyGameplayHarness::serverDone,() -> {},10);
        add("contextual-hover-source-close-fixture",() -> server(() -> {seedContextualHoverOrigin();contextualClosedMenu=(CraftingMenu)serverMenu(CraftingMenu.class);return null;}),
            ControlifyGameplayHarness::serverDone,() -> {},10);
        openContextualHoverOrigin("contextual-hover-source-close");
        add("contextual-hover-closes-real-native-source",() -> check("contextual-hover-source-native-handle-close",owner.windows().get(tableWindow).handle().close()),
            () -> !owner.windows().containsKey(tableWindow) && !contextualOverlay().presenting(),() -> {
                driver.rightTrigger=0;driver.rightX=driver.rightY=0;driver.stick(0,0);
            },4);
        assertServer("contextual-hover-source-close-restores-before-native-menu-disposal",() -> contextualExactOriginRestored()
            && contextualClosedMenu.getInputGridSlots().stream().allMatch(slot -> slot.getItem().isEmpty()));
        openContainer("contextual-hover-reopens-crafting-after-disposal",() -> tablePos,() -> tableWindow=newestOther());
        focusPair("contextual-hover-full-storage-pair",() -> tableWindow,() -> barrelWindow);
        add("contextual-hover-full-storage-fixture",() -> server(() -> fillContextualStorage(true)),ControlifyGameplayHarness::serverDone,() -> {
            var preflight=(Map<?,?>)serverTask.join();record("contextual-full-storage-actual-eligible-capacity","OBSERVED",preflight.get("capacity").toString());
            check("contextual-full-storage-fixture-covers-all-actual-eligible-slots",Boolean.TRUE.equals(preflight.get("full")));
        },10);
        add("contextual-hover-full-storage-native-RT",() -> {
            contextualRejectionsBefore=QaContextualPresentationProbe.recipeRejectionReads;pointAtNativeSlot(tableWindow,slot -> slot.index==1);
            var session=contextualTableSession();var screen=(RadialInventoryScreen)mc.gui.screen();
            record("contextual-full-storage-native-open-observation","OBSERVED","source="+tableWindow+",session="+session.sessionId()+",state="+session.menu().getStateId()
                +",hovered="+screen.controllerHoveredSlot()+",carried="+owner.desktop().carriedStack()+",requiresRelease="+field(screen,"contextualRequiresRelease")
                +",epoch="+field(DesktopContainerClient.class,"recipeHoverEpoch")+",rejected="+owner.desktop().recipeHoverRejected(tableWindow));
            driver.rightTrigger=1;
        },() -> QaContextualPresentationProbe.recipeRejectionReads>contextualRejectionsBefore && !contextualOverlay().active(),() -> {
            contextualRejectedEpoch=((Number)field(DesktopContainerClient.class,"recipeHoverEpoch")).longValue();driver.rightX=driver.rightY=0;driver.stick(0,0);
            check("contextual-full-storage-rejected-preview-closes-native-wheel",!contextualOverlay().active());
        },0);
        add("contextual-full-storage-rejection-holds-RT-without-reopening",() -> {},() -> true,() -> {
            check("contextual-full-storage-held-RT-does-not-repeat-BEGIN",!contextualOverlay().active()
                && ((Number)field(DesktopContainerClient.class,"recipeHoverEpoch")).longValue()==contextualRejectedEpoch);
            driver.rightTrigger=0;
        },20);
        assertServer("contextual-full-storage-rejected-preview-preserves-input-and-derived-output",() -> {
            var crafting=(CraftingMenu)serverMenu(CraftingMenu.class);
            return contextualGridCount(Items.BIRCH_LOG)==1 && crafting.getSlot(0).getItem().is(Items.BIRCH_PLANKS)
                && crafting.getSlot(0).getItem().getCount()==4 && player().inventoryMenu.getCarried().isEmpty()
                && contextualFullStorageBefore.keySet().stream().allMatch(slot -> slot.getItem().is(Items.BEDROCK) && slot.getItem().getCount()==64);
        });
        add("contextual-hover-full-storage-fixture-restore",() -> server(() -> {
            restoreContextualStorageFixture();
            for(var slot:((CraftingMenu)serverMenu(CraftingMenu.class)).getInputGridSlots()) slot.set(ItemStack.EMPTY);
            serverMenu(CraftingMenu.class).broadcastChanges();player().inventoryMenu.broadcastChanges();return null;
        }),ControlifyGameplayHarness::serverDone,() -> {},10);
        addContextualHoverRecoveryScenarios();
    }

    private static DesktopContainerSession contextualTableSession() {
        return (DesktopContainerSession)field(nativeWindow(tableWindow),"session");
    }
    private static void addContextualHoverRecoveryScenarios() {
        add("contextual-hover-interrupting-click-fixture",() -> server(() -> {
            seedContextualHoverOrigin();player().getInventory().setItem(9,new ItemStack(Items.DIAMOND,5));
            serverMenu(CraftingMenu.class).broadcastChanges();player().inventoryMenu.broadcastChanges();return null;
        }),ControlifyGameplayHarness::serverDone,() -> {},10);
        openContextualHoverOrigin("contextual-hover-interrupting-click");
        add("contextual-hover-genuinely-stale-click-packet",() -> {
            var session=contextualTableSession();contextualStaleSession=session;var menu=session.menu();contextualInterruptSlot=menu.slots.stream().filter(slot -> slot.getItem().is(Items.DIAMOND)).findFirst().orElseThrow().index;
            QaHoverPacketProbe.staleNextClick();
            check("contextual-hover-stale-click-queued",DesktopContainerClient.clickSlot(8999,session.sessionId(),contextualInterruptSlot,0,
                net.minecraft.world.inventory.ContainerInput.PICKUP,ItemStack.EMPTY));
        },QaHoverPacketProbe::staleClickSent,() -> record("contextual-hover-stale-click-state-observation","OBSERVED",QaHoverPacketProbe.snapshot().toString()),6);
        assertServer("contextual-hover-genuinely-stale-click-rejected-before-preview-cleanup",() -> contextualGridCount(Items.OAK_LOG)==1
            && ((Container)player().level().getBlockEntity(contextualIngredientPos)).getItem(7).isEmpty() && contextualOwnedOriginLogs()==1
            && player().inventoryMenu.getCarried().isEmpty() && inventoryCount(Items.DIAMOND)==5);
        add("contextual-hover-stale-click-client-remains-active",() -> {},() -> DesktopContainerClient.recipeHoverActive(contextualTableSession().sessionId()),() -> {
            check("contextual-hover-genuinely-stale-click-retains-active-native-wheel",contextualOverlay().active()
                && DesktopContainerClient.recipeHoverActive(contextualTableSession().sessionId()));
            check("contextual-hover-stale-resync-retains-native-window-and-side-pair",contextualTableSession()==contextualStaleSession
                && owner.windows().containsKey(tableWindow) && owner.model().focused().equals(tableWindow) && owner.model().selected().equals(barrelWindow));
        },2);
        add("contextual-hover-first-authentic-click-packet",() -> {
            var session=contextualTableSession();contextualInterruptSlot=session.menu().slots.stream().filter(slot -> slot.getItem().is(Items.DIAMOND)).findFirst().orElseThrow().index;
            record("contextual-hover-click-authentic-pre-cleanup-state","OBSERVED","session="+session.sessionId()+",stateId="+session.menu().getStateId()+",slot="+contextualInterruptSlot);
            check("contextual-hover-first-click-queued",DesktopContainerClient.clickSlot(9001,session.sessionId(),contextualInterruptSlot,0,
                net.minecraft.world.inventory.ContainerInput.PICKUP,ItemStack.EMPTY));driver.rightTrigger=0;
        },() -> true,() -> {},2);
        assertServer("contextual-hover-first-interrupting-click-applies-and-restores",() -> player().inventoryMenu.getCarried().is(Items.DIAMOND)
            && player().inventoryMenu.getCarried().getCount()==5 && contextualGridCount(Items.OAK_LOG)==0 && contextualExactOriginRestored());
        add("contextual-hover-click-return-cursor",() -> {
            var session=contextualTableSession();DesktopContainerClient.clickSlot(9002,session.sessionId(),contextualInterruptSlot,0,
                net.minecraft.world.inventory.ContainerInput.PICKUP,owner.desktop().carriedStack());
        },() -> true,() -> {},2);
        assertServer("contextual-hover-interrupting-click-cursor-recovered",() -> player().inventoryMenu.getCarried().isEmpty() && inventoryCount(Items.DIAMOND)==5);
        add("contextual-hover-interrupting-quickmove-fixture",() -> server(() -> {
            seedContextualHoverOrigin();player().getInventory().setItem(9,new ItemStack(Items.DIAMOND,7));
            serverMenu(CraftingMenu.class).broadcastChanges();player().inventoryMenu.broadcastChanges();return null;
        }),ControlifyGameplayHarness::serverDone,() -> {},10);
        openContextualHoverOrigin("contextual-hover-interrupting-quickmove");
        add("contextual-hover-first-authentic-quickmove-packet",() -> {
            var session=contextualTableSession();int slot=session.menu().slots.stream().filter(value -> value.getItem().is(Items.DIAMOND)).findFirst().orElseThrow().index;
            var target=(DesktopContainerSession)field(nativeWindow(barrelWindow),"session");
            record("contextual-hover-quickmove-authentic-pre-cleanup-state","OBSERVED","session="+session.sessionId()+",stateId="+session.menu().getStateId()+",slot="+slot);
            QaHoverPacketProbe.observeNextQuickMove();
            check("contextual-hover-first-quickmove-queued",DesktopContainerClient.quickMoveSlot(session.sessionId(),slot,
                com.salts_inventory_update.network.DesktopPackets.QUICK_TARGET_SESSION,target.sessionId()));driver.rightTrigger=0;
        },() -> true,() -> {},2);
        assertServer("contextual-hover-first-interrupting-quickmove-applies-and-restores",() -> inventoryCount(Items.DIAMOND)==0 && count(barrelPos,Items.DIAMOND)==7
            && contextualGridCount(Items.OAK_LOG)==0 && contextualExactOriginRestored() && player().inventoryMenu.getCarried().isEmpty());
        add("contextual-hover-stale-BEGIN-fixture",() -> server(() -> {seedContextualHoverOrigin();return null;}),ControlifyGameplayHarness::serverDone,() -> {},10);
        add("contextual-hover-stale-BEGIN-native-RT",() -> {
            QaHoverPacketProbe.staleNextBegin();
            contextualRejectionsBefore=QaContextualPresentationProbe.recipeRejectionReads;pointAtNativeSlot(tableWindow,slot -> slot.index==1);driver.rightTrigger=1;
        },() -> QaHoverPacketProbe.staleBeginSent() && QaContextualPresentationProbe.recipeRejectionReads>contextualRejectionsBefore && !contextualOverlay().active(),() -> {
            record("contextual-hover-stale-BEGIN-state-observation","OBSERVED",QaHoverPacketProbe.snapshot().toString());
            contextualRejectedEpoch=((Number)field(DesktopContainerClient.class,"recipeHoverEpoch")).longValue();
            check("contextual-hover-stale-BEGIN-rejected-inactive-and-closes-wheel",QaContextualPresentationProbe.recipeRejectionReads>contextualRejectionsBefore && !contextualOverlay().active());
        },0);
        add("contextual-hover-stale-BEGIN-hold-without-reopening",() -> {},() -> true,() -> {
            check("contextual-hover-stale-BEGIN-held-RT-does-not-repeat-BEGIN",!contextualOverlay().active()
                && ((Number)field(DesktopContainerClient.class,"recipeHoverEpoch")).longValue()==contextualRejectedEpoch);
            driver.rightTrigger=0;
        },20);
        assertServer("contextual-hover-stale-BEGIN-does-not-mutate-owned-items",() -> contextualGridCount(Items.OAK_LOG)==0 && contextualExactOriginRestored());
        add("contextual-hover-stale-BEGIN-neutral-before-retry",() -> {driver.rightTrigger=0;driver.stick(0,0);},() -> !contextualOverlay().presenting(),() -> {},6);
        openContextualHoverOrigin("contextual-hover-stale-BEGIN-safe-native-retry");
        add("contextual-hover-real-lease-expiry-without-heartbeat",() -> {
            setField(DesktopContainerClient.class,"recipeHoverHeartbeat",Integer.MIN_VALUE);
            contextualLeaseStarted=System.nanoTime();contextualRejectionsBefore=QaContextualPresentationProbe.recipeRejectionReads;
            driver.rightX=driver.rightY=0;driver.stick(0,0);
            server(() -> {
                Object state=((Map<?,?>)field(DesktopContainerSessions.class,"PLAYERS")).get(player());
                return Map.of("tick",((Number)field(state,"lifecycleTicks")).longValue(),
                    "touched",((Number)field(field(state,"recipeHover"),"touchedTick")).longValue());
            });
        },() -> serverDone() && QaContextualPresentationProbe.recipeRejectionReads>contextualRejectionsBefore && !contextualOverlay().active(),() -> {
            var baseline=(Map<?,?>)serverTask.join();contextualLeaseTick=((Number)baseline.get("tick")).longValue();
            contextualLeaseTouched=((Number)baseline.get("touched")).longValue();
            driver.rightTrigger=0;
            record("contextual-hover-native-lease-time-observation","OBSERVED","startTick="+contextualLeaseTick+",touchedTick="+contextualLeaseTouched
                +",wallElapsedMs="+(System.nanoTime()-contextualLeaseStarted)/1_000_000L);
            check("contextual-hover-real-server-lease-expiry-closes-wheel",QaContextualPresentationProbe.recipeRejectionReads>contextualRejectionsBefore && !contextualOverlay().active());
        },0);
        assertServer("contextual-hover-expired-lease-restores-exact-origin-on-native-tick",() -> {
            Object state=((Map<?,?>)field(DesktopContainerSessions.class,"PLAYERS")).get(player());
            long tick=((Number)field(state,"lifecycleTicks")).longValue();
            boolean passed=contextualGridCount(Items.OAK_LOG)==0 && contextualExactOriginRestored()
                && field(state,"recipeHover")==null && tick-contextualLeaseTouched>60;
            if(passed) QaHoverPacketProbe.leaseObserved(contextualLeaseTick,contextualLeaseTouched,tick);return passed;
        });
    }

    private static com.salts_inventory_update.controlify.model.ContextualWheelSelection contextualSelection() {
        return (com.salts_inventory_update.controlify.model.ContextualWheelSelection) field(contextualOverlay(), "selection");
    }
    private static void aimContextualIndex(double index) {
        int count = contextualEntries().size();
        double angle = index * Math.PI * 2 / count;
        driver.rightX = (float) Math.sin(angle); driver.rightY = (float) -Math.cos(angle); driver.stick(0, 0);
    }
    private static void addContextualPresentationScenarios() {
        add("contextual-textured-popout-settled",() -> {},() -> QaContextualPresentationProbe.currentLive(tableWindow)
            && QaContextualPresentationProbe.currentDestination(barrelWindow) && QaContextualPresentationProbe.settled(),() -> {
            var frame=QaContextualPresentationProbe.last().frame();
            check("contextual-settled-materials-wheel-live-container-order",frame.materials().x()+frame.materials().width()<frame.wheel().x()
                && frame.wheel().x()+frame.wheel().width()<frame.container().x());
            check("contextual-native-container-uses-moving-input-pose",QaContextualPresentationProbe.sourcePoseValid());
            check("contextual-body-text-readable-on-native-window-chrome",QaContextualPresentationProbe.textContrastValid());
            check("contextual-live-destination-below-ingredients",frame.destination().y()>=frame.materials().bottom()
                && frame.destination().x()+frame.destination().width()<frame.wheel().x()
                && QaContextualPresentationProbe.currentDestination(barrelWindow));
            check("contextual-panels-use-two-actual-Core-native-frames",QaContextualPresentationProbe.nativeFrameCalls==2*QaContextualPresentationProbe.frames
                && QaContextualPresentationProbe.nativeFrameCoreCalls==QaContextualPresentationProbe.nativeFrameCalls
                && QaContextualPresentationProbe.nativeFrameTextureBlits==9*QaContextualPresentationProbe.nativeFrameCalls);
            check("contextual-procedural-guard-positive-control",QaContextualPresentationProbe.controlCalls>0
                && QaContextualPresentationProbe.proceduralViolations().isEmpty());
            capture("41b-textured-wheel-settled-native-container");
        },0);
        add("contextual-release-and-native-A-same-sample",() -> {
            contextualReleaseBefore=QaContextualPresentationProbe.releaseActions;
            contextualClosingBefore=QaContextualPresentationProbe.closingFrames;
            contextualReleaseNeighbor=ghostCycleNeighbor(-1);
            QaContextualPresentationProbe.armRelease(tableWindow,1);
            pointerFixture=() -> pointAtNativeSlot(tableWindow,slot -> slot.index==1);
            driver.rightTrigger=0;driver.buttons.add(GamepadInputs.SOUTH_BUTTON);
            driver.buttons.add(GamepadInputs.LEFT_SHOULDER_BUTTON);
        },() -> QaContextualPresentationProbe.releaseActions>contextualReleaseBefore,() -> {
            driver.buttons.remove(GamepadInputs.SOUTH_BUTTON);
            driver.buttons.remove(GamepadInputs.LEFT_SHOULDER_BUTTON);
            check("contextual-same-release-native-A-addresses-moving-slot",pointerFixture==null
                && !contextualOverlay().active() && contextualOverlay().presenting()
                && ((RadialInventoryScreen)mc.gui.screen()).controllerHoveredSlot()!=null
                && ((RadialInventoryScreen)mc.gui.screen()).controllerHoveredSlot().index==1);
            check("contextual-same-release-native-cursor-still-enabled",Controlify.instance().virtualMouseHandler().isVirtualMouseEnabled());
            check("contextual-same-release-LB-selects-exact-neighbor",owner.model().selected().equals(contextualReleaseNeighbor)
                && owner.model().focused().equals(tableWindow));
        },0);
        add("contextual-reverse-popout-visible",() -> driver.buttons.remove(GamepadInputs.SOUTH_BUTTON),
            () -> QaContextualPresentationProbe.closingFrames>contextualClosingBefore && contextualOverlay().presenting()
                && QaContextualPresentationProbe.last().frame().progress()>0 && QaContextualPresentationProbe.last().frame().progress()<1,() -> {
            check("contextual-release-reverses-shared-live-frame",!QaContextualPresentationProbe.last().held()
                && QaContextualPresentationProbe.currentLive(tableWindow));
            capture("41c-textured-wheel-reverse-live-container");
            driver.trigger=1;aimWindow(barrelWindow);
        },0);
        add("contextual-LT-supersedes-reversing-RT",() -> {},() -> owner.radialActive() && !contextualOverlay().presenting(),() -> {
            check("contextual-LT-replaces-reverse-without-stale-overlay",owner.radialActive() && !contextualOverlay().presenting());
            driver.trigger=0;driver.stick(0,0);
        },0);
        add("contextual-LT-return-after-reverse",() -> {},() -> !owner.radialActive(),() -> {},3);
        // The source remains focused, so it is intentionally absent from the radial order.
        // Restore only the opposite pane through the actual LT selection/release path.
        selectPair("contextual-visual-test-restores-crafting-barrel",() -> barrelWindow);
        nativeCursorNeutralReady("contextual-popout-reopen-ready",() -> pointAtNativeSlot(tableWindow,slot -> slot.index==1));
        add("contextual-popout-reopens-for-functional-tests",() -> {
            cursorBeforeX=Controlify.instance().virtualMouseHandler().getCurrentX(0);driver.rightTrigger=1;
        },() -> contextualOverlay().active() && QaContextualPresentationProbe.currentLive(tableWindow)
            && QaContextualPresentationProbe.settled(),() -> check("contextual-popout-reopen-retains-original-side-pair",
                owner.model().focused().equals(tableWindow) && owner.model().selected().equals(barrelWindow)),0);
        add("contextual-transition-fixture-restores-config",() -> setAnimations(contextualAnimationsBefore),
            () -> contextualOverlay().active() && QaContextualPresentationProbe.settled(),() -> {
            check("contextual-transition-fixture-restores-prior-animation-setting",owner.config().animationsEnabled()==contextualAnimationsBefore);
            check("contextual-transition-config-restore-keeps-live-native-pair",owner.model().focused().equals(tableWindow)
                && owner.model().selected().equals(barrelWindow) && QaContextualPresentationProbe.currentLive(tableWindow));
        },2);
    }

    private static boolean nativeContextualDestinationHas(UUID id,Item item,int count) {
        String itemId=net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item).toString();
        return contextualOverlay().presenting() && QaContextualPresentationProbe.currentDestination(id)
            && QaContextualPresentationProbe.destinationSlots().stream().filter(slot -> slot.item().equals(itemId)).mapToInt(slot -> slot.count()).sum()==count;
    }
    private static void addContextualCarouselScenarios() {
        add("contextual-native-individual-carousel-source", () -> {
            contextualCarouselAnchor = Math.min(50, contextualSnapshot().recipes().size() - 3);
            aimContextualIndex(contextualCarouselAnchor);
        }, () -> contextualSelection().displayedSelected(contextualSnapshot().recipes().size()) == contextualCarouselAnchor, () -> {
            check("contextual-right-stick-addresses-individual-recipe", contextualSelection().selected(contextualSnapshot().recipes().size()) == contextualCarouselAnchor);
        }, 3);
        add("contextual-native-adjacent-carousel-recipe", () -> aimContextualIndex(contextualCarouselAnchor + 1),
            () -> contextualSelection().displayedSelected(contextualSnapshot().recipes().size()) == contextualCarouselAnchor + 1, () -> {
            check("contextual-right-stick-moves-one-adjacent-recipe", contextualSelection().selected(contextualSnapshot().recipes().size()) == contextualCarouselAnchor + 1);
        }, 3);
        add("contextual-native-fractional-carousel-position", () -> aimContextualIndex(contextualCarouselAnchor + 1.375), () -> true, () -> {
            double fraction = contextualSelection().position() - Math.floor(contextualSelection().position());
            check("contextual-right-stick-continuous-fractional-position", fraction > .2 && fraction < .55);
            int top=contextualSelection().topSlot(contextualEntries().size());
            check("contextual-carousel-visible-adjacent-window", contextualSelection().visibleIndex(contextualEntries().size(),top) == contextualCarouselAnchor + 1
                && contextualSelection().visibleIndex(contextualEntries().size(),top+1) == contextualCarouselAnchor + 2);
            capture("31-continuous-recipe-carousel-category-row");
        }, 5);
        add("contextual-native-left-stick-selects-visible-card", () -> {
            double angle = contextualSelection().displayedCardAngle(contextualSelection().topSlot(contextualEntries().size())+1, contextualEntries().size());
            driver.stick((float) Math.sin(angle), (float) -Math.cos(angle));
        }, () -> contextualSelection().displayedSelected(contextualSnapshot().recipes().size())
            == Math.floorMod(contextualCarouselAnchor + 2, contextualSnapshot().recipes().size()), () -> {
            check("contextual-left-stick-selects-displayed-adjacent-card", contextualSelection().displayedSelected(contextualSnapshot().recipes().size()) == contextualCarouselAnchor + 2);
            driver.stick(0, 0);
        }, 2);
        add("contextual-native-recipe-stick-return-ready",() -> { driver.rightX=driver.rightY=0;driver.stick(0,0); },() -> true,() -> {},3);
        add("contextual-native-recipe-stick-return-anchor",() -> aimContextualIndex(contextualCarouselAnchor+1),
            () -> contextualSelection().displayedSelected(contextualEntries().size())==contextualCarouselAnchor+1,
            () -> contextualCarouselStopped=contextualSelection().position(),3);
        add("contextual-native-stick-springback-outer",() -> {
            aimContextualIndex(contextualCarouselAnchor+31);driver.rightX*=.90f;driver.rightY*=.90f;
        },() -> true,() -> check("contextual-right-stick-inward-return-does-not-shift-wheel",Math.abs(contextualSelection().position()-contextualCarouselStopped)<.0001),3);
        add("contextual-native-stick-large-deadzone",() -> {
            aimContextualIndex(contextualCarouselAnchor+51);driver.rightX*=.70f;driver.rightY*=.70f;
            double angle=contextualSelection().displayedCardAngle(0,contextualEntries().size());
            driver.stick((float)Math.sin(angle)*.70f,(float)-Math.cos(angle)*.70f);
        },() -> true,() -> {
            check("contextual-recipe-large-deadzone-retains-outer-and-inner-selection",Math.abs(contextualSelection().position()-contextualCarouselStopped)<.0001
                && contextualSelection().displayedSelected(contextualEntries().size())==contextualCarouselAnchor+1);
            driver.rightX=driver.rightY=0;driver.stick(0,0);
        },3);
        add("contextual-native-inner-stick-return-anchor",() -> {
            double angle=contextualSelection().displayedCardAngle(contextualSelection().topSlot(contextualEntries().size())+1,contextualEntries().size());
            driver.stick((float)Math.sin(angle),(float)-Math.cos(angle));
        },() -> contextualSelection().displayedSelected(contextualEntries().size())==contextualCarouselAnchor+2,() -> {},3);
        add("contextual-native-inner-stick-inward-return",() -> {
            double angle=contextualSelection().displayedCardAngle(0,contextualEntries().size());
            driver.stick((float)Math.sin(angle)*.90f,(float)-Math.cos(angle)*.90f);
        },() -> true,() -> {
            check("contextual-left-stick-inward-return-does-not-shift-selection",contextualSelection().displayedSelected(contextualEntries().size())==contextualCarouselAnchor+2
                && Math.abs(contextualSelection().position()-contextualCarouselStopped)<.0001);
            driver.stick(0,0);
        },3);
        add("contextual-held-RB-rotates-carousel", () -> {
            aimContextualIndex(contextualSelection().position());
            contextualCarouselBefore = contextualSelection().position(); contextualCarouselStarted = System.nanoTime();
            driver.buttons.add(GamepadInputs.RIGHT_SHOULDER_BUTTON);
        }, () -> System.nanoTime() - contextualCarouselStarted >= 1_150_000_000L, () -> {
            driver.buttons.remove(GamepadInputs.RIGHT_SHOULDER_BUTTON);
            double elapsed = (System.nanoTime() - contextualCarouselStarted) / 1_000_000_000.0;
            double rate = (contextualSelection().position() - contextualCarouselBefore) / elapsed;
            check("contextual-held-RB-five-recipes-per-second", rate >= 4.3 && rate <= 5.7);
            check("contextual-held-bumpers-keep-side-pair", owner.model().focused().equals(tableWindow) && owner.model().selected().equals(barrelWindow));
            record("contextual-held-RB-native-rate", "OBSERVED", "recipesPerSecond=" + rate + ",elapsed=" + elapsed);
            capture("31b-held-RB-fine-carousel-motion");
        }, 0);
        add("contextual-native-RB-release-sampled", () -> {}, () -> !controller.input().orElseThrow().rawStateNow()
            .isButtonDown(GamepadInputs.RIGHT_SHOULDER_BUTTON), () -> contextualCarouselStopped = contextualSelection().position(), 1);
        add("contextual-fixed-right-stick-retains-bumper-offset", () -> {}, () -> true, () -> {
            check("contextual-fixed-right-stick-preserves-fine-offset", Math.abs(contextualSelection().position() - contextualCarouselStopped) < .03);
        }, 4);
        add("contextual-held-LB-rotates-carousel-back", () -> {
            contextualCarouselBefore = contextualSelection().position(); contextualCarouselStarted = System.nanoTime();
            driver.buttons.add(GamepadInputs.LEFT_SHOULDER_BUTTON);
        }, () -> System.nanoTime() - contextualCarouselStarted >= 1_150_000_000L, () -> {
            driver.buttons.remove(GamepadInputs.LEFT_SHOULDER_BUTTON);
            double elapsed = (System.nanoTime() - contextualCarouselStarted) / 1_000_000_000.0;
            double rate = (contextualCarouselBefore - contextualSelection().position()) / elapsed;
            check("contextual-held-LB-five-recipes-per-second", rate >= 4.3 && rate <= 5.7);
            record("contextual-held-LB-native-rate", "OBSERVED", "recipesPerSecond=" + rate + ",elapsed=" + elapsed);
            driver.rightX = 0; driver.rightY = 0; driver.stick(0, 0);
        }, 0);
        add("contextual-held-bumper-release-neutral", () -> {}, () -> true, () -> {}, 4);
        add("workspace-ui-neutral-wheel-remains-quiet", () -> nativeUiSoundsBefore = QaVisualFeatureProbe.acceptedSoundCount(), () -> true, () ->
            check("workspace-ui-neutral-wheel-no-repeating-sounds", QaVisualFeatureProbe.acceptedSoundCount()==nativeUiSoundsBefore), 8);
    }

    @SuppressWarnings("unchecked")
    private static void addContextualAlphabetScenarios() {
        add("contextual-alphabetical-native-catalog-and-markers", () -> {}, () -> QaVisualFeatureProbe.alphabetFrames > 0, () -> {
            var catalog = (com.salts_inventory_update.controlify.model.AlphabeticalRecipeIndex.Catalog<com.salts_inventory_update.api.client.desktop.DesktopContextualMenu.Recipe>)
                field(contextualOverlay(),"alphabetCatalog");
            var entries = catalog.entries();
            var collator = java.text.Collator.getInstance(java.util.Locale.forLanguageTag(mc.getLanguageManager().getSelected().replace('_','-')));
            collator.setStrength(java.text.Collator.SECONDARY);
            boolean ordered = true;
            for (int i=1;i<entries.size();i++) {
                int order = collator.compare(entries.get(i-1).displayName(),entries.get(i).displayName());
                ordered &= order < 0 || order == 0 && entries.get(i-1).stableKey().compareTo(entries.get(i).stableKey()) <= 0;
            }
            check("alphabet-native-catalog-is-localized-name-order", ordered && entries.size()>100);
            check("alphabet-catalog-retains-every-native-recipe-id", entries.size()==contextualSnapshot().recipes().size()
                && entries.stream().map(entry -> entry.value().id()).collect(java.util.stream.Collectors.toSet())
                .equals(contextualSnapshot().recipes().stream().map(com.salts_inventory_update.api.client.desktop.DesktopContextualMenu.Recipe::id).collect(java.util.stream.Collectors.toSet())));
            var expected = new LinkedHashMap<String,Integer>();
            for (int i=0;i<entries.size();i++) {
                String folded = java.text.Normalizer.normalize(entries.get(i).displayName().strip(),java.text.Normalizer.Form.NFKD).toUpperCase(java.util.Locale.ROOT);
                if (!folded.isEmpty() && folded.charAt(0)>='A' && folded.charAt(0)<='Z') expected.putIfAbsent(folded.substring(0,1),i);
            }
            var actual = new LinkedHashMap<String,Integer>(); catalog.groups().forEach(group -> actual.put(group.letter(),group.firstIndex()));
            check("alphabet-markers-only-present-initials-at-first-recipe", actual.equals(expected) && actual.size()>=10 && actual.size()<=26);
            check("alphabet-actual-native-labels-match-current-catalog", QaVisualFeatureProbe.alphabetGroups().equals(catalog.groups())
                && QaVisualFeatureProbe.alphabetNativeTextDraws > 0 && QaVisualFeatureProbe.violations().isEmpty());
            check("alphabet-actual-labels-separated-and-content-bounded",QaVisualFeatureProbe.alphabetBounds().size()==catalog.groups().size()
                && QaVisualFeatureProbe.alphabetOverlapViolations.isEmpty() && QaVisualFeatureProbe.alphabetBoundsViolations.isEmpty());
            check("alphabet-anchor-first-recipe-native-ticks-preserved",QaVisualFeatureProbe.alphabetAnchorTicks==QaVisualFeatureProbe.alphabetMarkers
                && QaVisualFeatureProbe.alphabetAnchorTicks>0 && QaVisualFeatureProbe.violations().isEmpty());
            capture("40-alphabetic-recipe-groups");
            capture("40b-alphabet-labels-separated-and-anchored");
        },3);
        add("contextual-alphabet-aim-first-letter",() -> {
            var catalog=(com.salts_inventory_update.controlify.model.AlphabeticalRecipeIndex.Catalog<?>)field(contextualOverlay(),"alphabetCatalog");
            contextualAlphabetIndex=catalog.groups().getFirst().firstIndex();aimContextualIndex(contextualAlphabetIndex);
        },() -> contextualSelection().displayedSelected(contextualEntries().size())==contextualAlphabetIndex,() -> {
            int top=contextualSelection().topSlot(contextualEntries().size());
            check("alphabet-aim-first-letter-displays-first-recipe-at-top",contextualSelection().visibleIndex(contextualEntries().size(),top)==contextualAlphabetIndex
                && Math.abs(contextualSelection().displayedCardAngle(top,contextualEntries().size()))<.0001
                && QaFeatureProbe.renderedRecipeId.equals(contextualEntries().get(contextualAlphabetIndex).id()));
        },4);
        add("contextual-alphabet-aim-middle-letter",() -> {
            var catalog=(com.salts_inventory_update.controlify.model.AlphabeticalRecipeIndex.Catalog<?>)field(contextualOverlay(),"alphabetCatalog");
            contextualAlphabetIndex=catalog.groups().get(catalog.groups().size()/2).firstIndex();aimContextualIndex(contextualAlphabetIndex);
        },() -> contextualSelection().displayedSelected(contextualEntries().size())==contextualAlphabetIndex,() -> {
            int top=contextualSelection().topSlot(contextualEntries().size());
            check("alphabet-aim-middle-letter-displays-first-recipe-at-top",contextualSelection().visibleIndex(contextualEntries().size(),top)==contextualAlphabetIndex
                && Math.abs(contextualSelection().displayedCardAngle(top,contextualEntries().size()))<.0001
                && QaFeatureProbe.renderedRecipeId.equals(contextualEntries().get(contextualAlphabetIndex).id()));
            check("alphabet-native-labels-use-inside-and-outside-lanes",QaVisualFeatureProbe.alphabetInsideLabels>0
                && QaVisualFeatureProbe.alphabetOutsideLabels>0 && QaVisualFeatureProbe.violations().isEmpty());
            capture("44-letter-selection-centers-first-recipe");
            driver.rightX=driver.rightY=0;
        },4);
    }

    private static void addNativeUiMuteScenarios() {
        add("workspace-ui-mute-native-source", () -> {
            nativeUiSoundsBefore = QaVisualFeatureProbe.acceptedSoundCount();
            mc.options.getSoundSourceOptionInstance(net.minecraft.sounds.SoundSource.UI).set(0.0);
        }, () -> true, () -> {},2);
        button("workspace-ui-muted-native-filter",GamepadInputs.DPAD_UP_BUTTON,() -> {
            check("workspace-ui-muted-native-action-still-operates",(Boolean)field(contextualOverlay(),"craftableOnly"));
            check("workspace-ui-native-source-mute-suppresses-play",QaVisualFeatureProbe.acceptedSoundCount()==nativeUiSoundsBefore);
        });
        button("workspace-ui-muted-native-filter-return",GamepadInputs.DPAD_UP_BUTTON,() ->
            check("workspace-ui-muted-filter-restores-native-category",!(Boolean)field(contextualOverlay(),"craftableOnly")));
        add("workspace-ui-mute-native-master",() -> {
            mc.options.getSoundSourceOptionInstance(net.minecraft.sounds.SoundSource.UI).set(1.0);
            mc.options.getSoundSourceOptionInstance(net.minecraft.sounds.SoundSource.MASTER).set(0.0);
        },() -> true,() -> {},2);
        button("workspace-ui-master-muted-native-filter",GamepadInputs.DPAD_UP_BUTTON,() ->
            check("workspace-ui-native-master-mute-suppresses-play",QaVisualFeatureProbe.acceptedSoundCount()==nativeUiSoundsBefore));
        button("workspace-ui-master-muted-filter-return",GamepadInputs.DPAD_UP_BUTTON,() ->
            check("workspace-ui-master-muted-filter-restores-native-category",!(Boolean)field(contextualOverlay(),"craftableOnly")));
        add("workspace-ui-normal-volume-restored",() -> mc.options.getSoundSourceOptionInstance(net.minecraft.sounds.SoundSource.MASTER).set(1.0),
            () -> true,() -> check("workspace-ui-mute-respects-native-preferences",QaVisualFeatureProbe.rejectedSoundCount()>=4),2);
    }

    private static boolean nativeGuideReady(String mode, String... labels) {
        var frame = QaNativeGuideProbe.workspace;
        return frame != null && frame.sequence() > nativeGuideBefore && frame.mode().equals(mode)
            && Arrays.stream(labels).allMatch(frame::label);
    }
    private static Component nativeGuideGlyph(String label) {
        String metadata = switch (label) {
            case "Hold: window wheel", "Release opening trigger", "Release: show highlighted window", "Release: tuck away" -> "WINDOW_WHEEL";
            case "Previous / next window", "Hold: rotate recipes" -> "WINDOW_CYCLE";
            case "Choose window / tuck away", "Choose recipe", "Choose creative tab" -> "LEFT_STICK";
            case "Browse recipes" -> "RIGHT_STICK";
            case "Scroll list" -> "SCROLL";
            case "Change recipe category", "Previous / next creative tab" -> "CATEGORY";
            case "Toggle craftable recipes" -> "FILTER";
            case "Navigate slots / controls", "Navigate / scroll rows" -> "NAVIGATION";
            case "Move cursor" -> "CURSOR";
            default -> "";
        };
        if (!metadata.isEmpty()) return ((dev.isxander.controlify.api.bind.InputBindingSupplier)
            field(com.salts_inventory_update.controlify.WorkspaceActionGuide.class, metadata)).on(controller).inputGlyph();
        return switch (label) {
            case "Close all windows", "Close highlighted window" -> ControlifyBindings.GUI_BACK.on(controller).inputGlyph();
            case "Focus highlighted window", "Craft into opposite window", "Load cooking ingredients",
                 "Select creative tab", "Add one to hotbar" -> ControlifyBindings.INV_SELECT.on(controller).inputGlyph();
            case "Sort focused; double tap: force", "Add stack to hotbar", "Restore windows" -> ControlifyBindings.INVENTORY.on(controller).inputGlyph();
            default -> com.salts_inventory_update.controlify.ContextualRecipeBinding.OPEN_WHEEL.on(controller).inputGlyph();
        };
    }
    private static void verifyNativeGuide(String name, String mode, String... labels) {
        var frame = Objects.requireNonNull(QaNativeGuideProbe.workspace, "Actual native workspace guide");
        check("native-guide-" + name + "-labels-and-glyphs", frame.domain().equals("salts_inventory_controlify:workspace")
            && frame.mode().equals(mode) && frame.bottomAligned() && frame.compact() && frame.horizontal()
            && Arrays.stream(labels).allMatch(label -> frame.glyph(label, nativeGuideGlyph(label))));
        check("native-guide-" + name + "-short-horizontal-bounded-entries", frame.draws().stream()
            .allMatch(draw -> !draw.label().isEmpty() && draw.label().split("\\s+").length <= 2)
            && QaNativeGuideProbe.compactViolations.isEmpty());
        if (mode.equals("SIDE") || mode.equals("LT_RADIAL")) {
            var placements = renderedLayout();
            boolean mainPanesClear = true;
            for (var id : new LinkedHashSet<>(List.of(owner.model().focused(), owner.model().selected()))) {
                var placement = placements.windows().get(id);
                if (placement == null || placement.opacity() <= .005) continue;
                var bounds = owner.desktop().presentationBounds(id);
                boolean clear = placement.y() + bounds.height() * placement.scale() <= frame.top() + .5;
                mainPanesClear &= clear;
                check("native-guide-" + name + "-main-pane-clear-bottom-" + id, clear);
            }
            check("native-guide-main-panes-clear-bottom-lists", mainPanesClear);
        }
        record("native-guide-" + name + "-actual-render", "OBSERVED", frame.toString());
    }
    private static void observeNativeGuide(String name, String mode, String... labels) {
        add("native-guide-" + name + "-fresh-render", () -> nativeGuideBefore = QaNativeGuideProbe.frames,
            () -> nativeGuideReady(mode, labels), () -> verifyNativeGuide(name, mode, labels), 2);
    }
    private static void addContextualBookHintScenarios() {
        nativeCursorNeutralReady("native-guide-supported-table", () -> pointAtNativeSlot(tableWindow, slot -> slot.index == 1));
        observeNativeGuide("supported-table", "SIDE", "Close all windows", "Hold: window wheel", "Hold: recipe wheel",
            "Previous / next window", "Move cursor", "Navigate slots / controls");
        add("native-guide-supported-table-capture", () -> {}, () -> true, () -> capture("32-native-guide-supported-table"), 1);
        nativeCursorNeutralReady("native-guide-table-result", () -> pointAtNativeSlot(tableWindow, slot -> slot.index == 0));
        observeNativeGuide("table-result", "SIDE", "Hold: recipe wheel");
        add("contextual-result-slot-RT-opens-wheel", () -> driver.rightTrigger = 1,
            () -> mc.gui.screen() instanceof RadialInventoryScreen screen && screen.contextualRadialActive(), () -> {
                check("contextual-result-slot-activates-hovered-wheel", contextualOverlay().source().equals(tableWindow));
                driver.rightTrigger = 0;
            }, 3);
        add("contextual-result-slot-RT-release-sampled", () -> {},
            () -> mc.gui.screen() instanceof RadialInventoryScreen screen && !screen.contextualRadialActive(), () -> {}, 3);
        nativeCursorNeutralReady("native-guide-table-background", () -> {
            var state = owner.windows().get(tableWindow).handle().state();
            pointAtControl(tableWindow, new DesktopWindowBounds(state.x()+6, state.y()+6, 4, 4));
        });
        observeNativeGuide("table-background", "SIDE", "Hold: recipe wheel");
        add("contextual-background-RT-opens-wheel", () -> driver.rightTrigger = 1,
            () -> mc.gui.screen() instanceof RadialInventoryScreen screen && screen.contextualRadialActive(), () -> {
                check("contextual-background-activates-hovered-wheel", contextualOverlay().source().equals(tableWindow));
                driver.rightTrigger = 0;
            }, 3);
        add("contextual-background-RT-release-sampled", () -> {},
            () -> mc.gui.screen() instanceof RadialInventoryScreen screen && !screen.contextualRadialActive(), () -> {}, 3);
        nativeCursorNeutralReady("native-guide-unsupported-barrel", () -> pointAtNativeSlot(barrelWindow, slot -> slot.index == 0));
        observeNativeGuide("unsupported-barrel", "SIDE", "Hold: window wheel", "Navigate slots / controls");
        add("native-guide-off-supported-pane", () -> {}, () -> true, () -> {
            var frame = QaNativeGuideProbe.workspace;
            check("native-guide-quick-action-hidden-off-supported-pane", !frame.label("Hold: recipe wheel")
                && !frame.label("Hold: cooking wheel") && !frame.label("Hold: creative tab wheel"));
            capture("32b-native-guide-off-supported-pane");
        }, 2);
        add("contextual-unsupported-pane-RT-does-not-open-wheel", () -> driver.rightTrigger = 1,
            () -> controller.input().orElseThrow().rawStateNow().getAxisState(GamepadInputs.RIGHT_TRIGGER_AXIS) >= .9f, () -> {
                check("contextual-unsupported-pane-does-not-activate-wheel", mc.gui.screen() instanceof RadialInventoryScreen screen
                    && !screen.contextualRadialActive()); driver.rightTrigger = 0;
            }, 4);
        add("contextual-unsupported-pane-RT-release-sampled", () -> {},
            () -> controller.input().orElseThrow().rawStateNow().getAxisState(GamepadInputs.RIGHT_TRIGGER_AXIS) < .1f, () -> {}, 2);
        add("contextual-rebind-wheel-action-native-glyph", () -> {
            var binding = com.salts_inventory_update.controlify.ContextualRecipeBinding.OPEN_WHEEL.on(controller);
            contextualDefaultGlyph = binding.inputGlyph(); binding.setBoundInput(new ButtonInput(GamepadInputs.MISC_3_BUTTON));
        }, () -> true, () -> {}, 2);
        nativeCursorNeutralReady("native-guide-rebound-table", () -> pointAtNativeSlot(tableWindow, slot -> slot.index == 1));
        observeNativeGuide("rebound-table", "SIDE", "Hold: recipe wheel");
        add("contextual-rebound-native-guide-glyph", () -> {}, () -> true, () -> {
            var binding = com.salts_inventory_update.controlify.ContextualRecipeBinding.OPEN_WHEEL.on(controller);
            check("contextual-rebound-native-guide-actual-binding-glyph", QaNativeGuideProbe.workspace.glyph("Hold: recipe wheel", binding.inputGlyph())
                && !binding.inputGlyph().equals(contextualDefaultGlyph));
            capture("32c-native-guide-rebound-recipe-action");
        }, 2);
        add("contextual-old-RT-does-not-open-rebound-wheel", () -> driver.rightTrigger = 1,
            () -> controller.input().orElseThrow().rawStateNow().getAxisState(GamepadInputs.RIGHT_TRIGGER_AXIS) >= .9f, () -> {
                check("contextual-rebound-old-RT-no-longer-opens-wheel", mc.gui.screen() instanceof RadialInventoryScreen screen
                    && !screen.contextualRadialActive()); driver.rightTrigger = 0;
            }, 4);
        add("contextual-rebound-old-RT-release-sampled", () -> {},
            () -> controller.input().orElseThrow().rawStateNow().getAxisState(GamepadInputs.RIGHT_TRIGGER_AXIS) < .1f, () -> {}, 2);
        add("contextual-native-rebound-action-opens-wheel", () -> driver.buttons.add(GamepadInputs.MISC_3_BUTTON),
            () -> mc.gui.screen() instanceof RadialInventoryScreen screen && screen.contextualRadialActive(), () -> {
                check("contextual-rebound-action-opens-hovered-wheel", contextualOverlay().source().equals(tableWindow));
            }, 2);
        observeNativeGuide("rebound-recipe-wheel", "RECIPE", "Release: return to pair", "Browse recipes", "Choose recipe",
            "Hold: rotate recipes", "Change recipe category", "Toggle craftable recipes");
        add("contextual-native-rebound-action-release", () -> driver.buttons.remove(GamepadInputs.MISC_3_BUTTON),
            () -> mc.gui.screen() instanceof RadialInventoryScreen screen && !screen.contextualRadialActive(), () -> {
                var binding = com.salts_inventory_update.controlify.ContextualRecipeBinding.OPEN_WHEEL.on(controller);
                binding.setBoundInput(binding.defaultInput());
            }, 3);
        add("native-guide-screen-preference-off", () -> {
            controller.settings().generic.guide.showScreenGuides = false; nativeGuideBefore = QaNativeGuideProbe.workspaceFrames;
        }, () -> true, () -> {
            check("native-guide-screen-preference-off-no-render", QaNativeGuideProbe.workspaceFrames == nativeGuideBefore);
            controller.settings().generic.guide.showScreenGuides = true;
        }, 6);
        observeNativeGuide("screen-preference-restored", "SIDE", "Hold: recipe wheel");
        add("compact-guide-constrained-viewport",() -> {
            mc.getWindow().setWindowed(800,600); nativeGuideBefore = QaNativeGuideProbe.frames;
        },() -> mc.getWindow().getGuiScaledWidth()==400 && mc.getWindow().getGuiScaledHeight()==300
            && QaNativeGuideProbe.compactWidth==400 && QaNativeGuideProbe.compactHeight==300
            && QaNativeGuideProbe.workspace.sequence()>nativeGuideBefore,() -> {
            check("compact-guide-constrained-two-rows-small-band",QaNativeGuideProbe.compactRows<=2
                && QaNativeGuideProbe.compactReservedHeight<=60 && QaNativeGuideProbe.workspace.horizontal()
                && QaNativeGuideProbe.compactViolations.isEmpty());
            capture("37b-compact-guide-constrained-viewport");
        },4);
        add("compact-guide-standard-viewport-restored",() -> mc.getWindow().setWindowed(1280,900),
            () -> mc.getWindow().getGuiScaledWidth()==640 && mc.getWindow().getGuiScaledHeight()==450
                && QaNativeGuideProbe.compactWidth==640 && QaNativeGuideProbe.compactHeight==450,() ->
                check("compact-guide-resize-preserves-native-pair",owner.model().focused().equals(tableWindow)
                    && owner.model().selected().equals(barrelWindow) && QaNativeGuideProbe.compactViolations.isEmpty()),4);
    }

    private static boolean nativeGameplayGuideReady(boolean bottom) {
        var frame = QaNativeGuideProbe.gameplay;
        return frame != null && QaNativeGuideProbe.gameplayFrames > nativeGuideBefore && frame.bottomAligned() == bottom
            && frame.bothColumns() && frame.label("Restore windows");
    }
    private static void addTuckedNativeGuideScenarios() {
        add("native-guide-tucked-top-both-columns", () -> nativeGuideBefore = QaNativeGuideProbe.gameplayFrames,
            () -> nativeGameplayGuideReady(false), () -> {
                var frame = QaNativeGuideProbe.gameplay;
                check("native-guide-tucked-top-columns-clear-tabs-and-previews", frame.domain().equals("controlify:in_game")
                    && frame.tuckedInset() > 0 && Math.abs(frame.parentY() - frame.tuckedInset()) < .01
                    && frame.draws().stream().allMatch(draw -> draw.top() >= Math.max(QaNativeGuideProbe.tabsBottom, frame.tuckedInset()))
                    && QaNativeGuideProbe.overlapViolations.isEmpty());
                check("native-guide-tucked-restore-uses-inventory-glyph", frame.glyph("Restore windows", ControlifyBindings.INVENTORY.on(controller).inputGlyph())
                    && !frame.label("Open Inventory"));
                check("native-guide-tucked-transition-columns-clear-preview", QaNativeGuideProbe.tuckedTransitionFrames > 0
                    && QaNativeGuideProbe.overlapViolations.isEmpty());
                record("native-guide-tucked-top-actual-render", "OBSERVED", frame.toString() + ",tabsBottom=" + QaNativeGuideProbe.tabsBottom);
                capture("35-tucked-native-gameplay-top");
            }, 3);
        add("native-guide-tucked-bottom-preference", () -> {
            controller.settings().generic.guide.ingameGuideBottom = true; nativeGuideBefore = QaNativeGuideProbe.gameplayFrames;
        }, () -> nativeGameplayGuideReady(true), () -> {
            var frame = QaNativeGuideProbe.gameplay;
            check("native-guide-bottom-preference-keeps-original-pose", Math.abs(frame.parentY()) < .01
                && frame.draws().stream().allMatch(draw -> draw.top() > mc.getWindow().getGuiScaledHeight()/2.0));
            capture("35b-tucked-native-gameplay-bottom");
        }, 3);
        add("native-guide-tucked-preference-off", () -> {
            controller.settings().generic.guide.showIngameGuide = false; nativeGuideBefore = QaNativeGuideProbe.gameplayFrames;
        }, () -> true, () -> {
            check("native-guide-gameplay-preference-off-no-render", QaNativeGuideProbe.gameplayFrames == nativeGuideBefore);
            controller.settings().generic.guide.showIngameGuide = true;
            controller.settings().generic.guide.ingameGuideBottom = false;
        }, 6);
        add("native-guide-hideGui-still-gates-native-HUD", () -> {
            nativeHudHiddenBefore = mc.gui.hud.isHidden();
            if (!nativeHudHiddenBefore) mc.gui.hud.toggle();
            nativeGuideBefore = QaNativeGuideProbe.gameplayFrames;
        }, () -> true, () -> {
            check("native-guide-hidden-HUD-no-render", QaNativeGuideProbe.gameplayFrames == nativeGuideBefore);
            if (mc.gui.hud.isHidden() != nativeHudHiddenBefore) mc.gui.hud.toggle();
        }, 6);
        add("native-guide-tucked-top-restored-after-preferences", () -> nativeGuideBefore = QaNativeGuideProbe.gameplayFrames,
            () -> nativeGameplayGuideReady(false), () -> {
                check("native-guide-tucked-preferences-restored-native-render", QaNativeGuideProbe.gameplay.tuckedInset() > 0
                    && QaNativeGuideProbe.overlapViolations.isEmpty());
                capture("35c-tucked-native-guide-preferences-restored");
            }, 3);
    }

    private static int characterGridCount(Item item) {
        int count = 0;
        for (int slot = 1; slot <= 4; slot++) if (player().inventoryMenu.getSlot(slot).getItem().is(item))
            count += player().inventoryMenu.getSlot(slot).getItem().getCount();
        return count;
    }
    private static void addCharacterContextualScenarios() {
        focusPair("contextual-character-inventory-pair", () -> owner.model().character(), () -> owner.model().playerInventory());
        add("contextual-character-open-only-ingredient-fixture", () -> server(() -> {
            player().getInventory().clearContent(); player().inventoryMenu.setCarried(ItemStack.EMPTY);
            for (int slot = 1; slot <= 4; slot++) player().inventoryMenu.getSlot(slot).set(ItemStack.EMPTY);
            Container ingredients = (Container) player().level().getBlockEntity(contextualIngredientPos);
            ingredients.clearContent(); ingredients.setItem(0, new ItemStack(Items.OAK_LOG));
            player().inventoryMenu.broadcastChanges(); return null;
        }), ControlifyGameplayHarness::serverDone, () -> {}, 12);
        add("contextual-character-native-RT-opens-2x2-wheel", () -> {
            Controlify.instance().virtualMouseHandler().enableVirtualMouse();
            pointAtNativeSlot(owner.model().character(), slot -> slot.index == 1);
            driver.rightTrigger = 1;
        }, () -> mc.gui.screen() instanceof RadialInventoryScreen screen && screen.contextualRadialActive(), () -> {
            check("contextual-character-native-2x2-station", contextualOverlay().source().equals(owner.model().character())
                && contextualSnapshot().station().gridWidth() == 2 && contextualSnapshot().station().gridHeight() == 2);
            check("contextual-character-excludes-3x3-recipes", contextualSnapshot().recipes().stream()
                .allMatch(recipe -> recipe.gridWidth() <= 2 && recipe.gridHeight() <= 2));
            check("contextual-character-open-container-ingredient-count", contextualSnapshot().recipes().stream()
                .filter(recipe -> recipe.output().is(Items.OAK_PLANKS) && recipe.craftable())
                .anyMatch(recipe -> recipe.requirements().stream().anyMatch(ingredient -> ingredient.available() == 1)));
        }, 4);
        add("contextual-character-select-visible-planks", () -> aimContextualRecipe(Items.OAK_PLANKS, true),
            () -> contextualRecipeReady(Items.OAK_PLANKS, true), () -> {
                verifyContextualRecipe(Items.OAK_PLANKS, true); capture("33-character-native-2x2-recipe-wheel");
            }, 2);
        button("contextual-character-native-A-crafts-to-inventory", GamepadInputs.SOUTH_BUTTON, () -> {});
        assertServer("contextual-character-native-craft-output-conserved", () -> inventoryCount(Items.OAK_PLANKS) == 4
            && inventoryCount(Items.OAK_LOG) + characterGridCount(Items.OAK_LOG) + count(contextualIngredientPos, Items.OAK_LOG) == 0
            && player().inventoryMenu.getCarried().isEmpty());
        add("contextual-character-native-destination-displays-planks",() -> {},
            () -> nativeContextualDestinationHas(owner.model().playerInventory(),Items.OAK_PLANKS,4),() -> {
            check("contextual-character-real-native-destination-slot",nativeContextualDestinationHas(owner.model().playerInventory(),Items.OAK_PLANKS,4));
            capture("42c-character-native-destination-output");
        },2);
        add("contextual-character-release-RT", () -> { driver.rightTrigger = 0; driver.stick(0, 0); driver.rightX = driver.rightY = 0; },
            () -> mc.gui.screen() instanceof RadialInventoryScreen screen && !screen.contextualRadialActive(), () -> {}, 4);
    }

    @SuppressWarnings("unchecked")
    private static net.minecraft.world.inventory.AbstractFurnaceMenu contextualServerCookingMenu(int index) {
        Object state = ((Map<ServerPlayer, ?>) field(DesktopContainerSessions.class, "PLAYERS")).get(player());
        Object session = ((Map<Integer, ?>) field(state, "sessions")).get(contextualCookingSessions[index]);
        return (net.minecraft.world.inventory.AbstractFurnaceMenu) field(Objects.requireNonNull(session, "Authenticated cooking session"), "menu");
    }

    private static void addCookingContextualScenarios() {
        add("contextual-native-cooking-block-fixture", () -> server(() -> {
            var blocks = new net.minecraft.world.level.block.Block[] {Blocks.FURNACE, Blocks.BLAST_FURNACE, Blocks.SMOKER};
            for (int i = 0; i < blocks.length; i++) {
                contextualCookingPositions[i] = contextualIngredientPos.offset(-3 + i * 2, 0, -2);
                var level = player().level();
                level.setBlock(contextualCookingPositions[i].below(), Blocks.STONE.defaultBlockState(), 3);
                level.setBlock(contextualCookingPositions[i], blocks[i].defaultBlockState(), 3);
            }
            player().getInventory().clearContent(); player().inventoryMenu.setCarried(ItemStack.EMPTY);
            for (int slot = 1; slot <= 4; slot++) player().inventoryMenu.getSlot(slot).set(ItemStack.EMPTY);
            Container ingredients = (Container) player().level().getBlockEntity(contextualIngredientPos);
            ingredients.clearContent();
            ingredients.setItem(0, new ItemStack(Items.RAW_IRON, 2));
            ingredients.setItem(1, new ItemStack(Items.BEEF)); ingredients.setItem(2, new ItemStack(Items.COAL, 3));
            player().inventoryMenu.broadcastChanges(); return null;
        }), ControlifyGameplayHarness::serverDone, () -> {}, 12);
        String[] names = {"furnace", "blast-furnace", "smoker"};
        for (int i = 0; i < names.length; i++) {
            final int index = i;
            openContainer("contextual-open-native-" + names[i], () -> contextualCookingPositions[index], () -> {
                contextualCookingWindows[index] = newestOther();
                contextualCookingSessions[index] = ((DesktopContainerSession) field(nativeWindow(contextualCookingWindows[index]), "session")).sessionId();
            });
        }
        for (int i = 0; i < names.length; i++) addCookingVariantScenarios(names[i], i, i == 2 ? Items.BEEF : Items.RAW_IRON,
            i == 2 ? Items.COOKED_BEEF : Items.IRON_INGOT);
        addCookingHoverLifecycleScenarios();
    }

    private static void addCookingVariantScenarios(String name, int index, Item input, Item outputItem) {
        focusPair("contextual-" + name + "-inventory-pair", () -> contextualCookingWindows[index], () -> owner.model().playerInventory());
        nativeCursorNeutralReady("native-guide-cooking-hover-" + name, () -> pointAtNativeSlot(contextualCookingWindows[index], slot -> slot.index == 2));
        observeNativeGuide("cooking-hover-" + name, "SIDE", "Hold: cooking wheel");
        add("contextual-" + name + "-native-RT-opens-wheel", () -> {
            Controlify.instance().virtualMouseHandler().enableVirtualMouse();
            pointAtNativeSlot(contextualCookingWindows[index], slot -> slot.index == 0);
            driver.rightTrigger = 1;
        }, () -> mc.gui.screen() instanceof RadialInventoryScreen screen && screen.contextualRadialActive(), () -> {
            check("contextual-" + name + "-native-cooking-wheel", contextualOverlay().source().equals(contextualCookingWindows[index])
                && contextualSnapshot().kind() == com.salts_inventory_update.api.client.desktop.DesktopContextualMenu.Kind.COOKING);
            check("contextual-" + name + "-native-empty-input-output-preview", contextualSnapshot().station().input().isEmpty()
                && contextualSnapshot().station().output().isEmpty());
            check("contextual-" + name + "-open-only-input-count", contextualSnapshot().recipes().stream()
                .filter(recipe -> recipe.output().is(outputItem) && recipe.craftable())
                .anyMatch(recipe -> recipe.requirements().stream().anyMatch(ingredient -> ingredient.alternatives().stream().anyMatch(stack -> stack.is(input))
                    && ingredient.available() == (index == 0 ? 2 : 1))));
        }, 4);
        add("contextual-" + name + "-select-visible-cooking-recipe", () -> aimContextualRecipe(outputItem, true),
            () -> contextualRecipeReady(outputItem, true), () -> {
                verifyContextualRecipe(outputItem, true); capture("34-" + name + "-native-cooking-wheel");
            }, 2);
        assertServer("contextual-"+name+"-hover-stages-input-without-taking-fuel",() -> {
            var menu=contextualServerCookingMenu(index);
            return menu.getSlot(0).getItem().is(input) && menu.getSlot(0).getItem().getCount()==1
                && menu.getSlot(1).getItem().isEmpty() && count(contextualIngredientPos,Items.COAL)==3-index
                && count(contextualIngredientPos,input)==(index==0?1:0);
        });
        observeNativeGuide("cooking-" + name, "COOKING", "Close all windows", "Release: return to pair", "Load cooking ingredients",
            "Browse recipes", "Choose recipe", "Hold: rotate recipes", "Change recipe category", "Toggle craftable recipes");
        button("contextual-" + name + "-native-A-loads-input-fuel", GamepadInputs.SOUTH_BUTTON, () -> {});
        assertServer("contextual-" + name + "-native-input-fuel-loaded-from-open-container", () -> {
            var menu = contextualServerCookingMenu(index);
            return menu.getSlot(0).getItem().is(input) && menu.getSlot(0).getItem().getCount() == 1
                && menu.getSlot(2).getItem().isEmpty() && menu.isLit()
                && count(contextualIngredientPos, input) == (index == 0 ? 1 : 0)
                && count(contextualIngredientPos, Items.COAL) == 2 - index;
        });
        add("contextual-" + name + "-native-live-station-preview", () -> {}, () -> contextualSnapshot().station().input().is(input)
            && contextualSnapshot().station().litProgress() > 0 && contextualSnapshot().station().burnProgress() > 0
            && QaContextualPresentationProbe.currentLive(contextualCookingWindows[index]) && QaContextualPresentationProbe.cooking()!=null
            && QaContextualPresentationProbe.cooking().burn()>0 && QaContextualPresentationProbe.cooking().lit()>0
            && QaContextualPresentationProbe.cookingWidgetsCurrent(), () -> {
            check("contextual-" + name + "-real-furnace-progress-preview", contextualSnapshot().station().input().is(input)
                && contextualSnapshot().station().output().isEmpty() && contextualSnapshot().station().burnProgress() > 0);
            var live=QaContextualPresentationProbe.cooking();
            check("contextual-"+name+"-actual-live-native-menu-input-progress",live.input().equals(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(input).toString())
                && live.inputCount()==1 && live.outputCount()==0 && live.burn()>0 && live.lit()>0
                && QaContextualPresentationProbe.cookingWidgetsCurrent()
                && QaContextualPresentationProbe.slots().stream().anyMatch(slot -> slot.index()==0 && slot.item().equals(live.input()) && slot.count()==1));
            capture("34b-" + name + "-live-native-progress");
        }, 2);
        assertServer("contextual-" + name + "-native-processing-produces-output", () -> {
            var menu = contextualServerCookingMenu(index);
            return menu.getSlot(0).getItem().isEmpty() && menu.getSlot(2).getItem().is(outputItem)
                && menu.getSlot(2).getItem().getCount() == 1 && count(contextualIngredientPos, Items.COAL) == 2 - index;
        });
        add("contextual-" + name + "-native-output-preview", () -> {}, () -> contextualSnapshot().station().output().is(outputItem)
            && QaContextualPresentationProbe.currentLive(contextualCookingWindows[index]) && QaContextualPresentationProbe.cooking()!=null
            && QaContextualPresentationProbe.cooking().outputCount()==1, () -> {
            check("contextual-" + name + "-actual-output-preview", contextualSnapshot().station().output().getCount() == 1);
            String outputId=net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(outputItem).toString();
            check("contextual-"+name+"-actual-live-native-menu-output-slot",QaContextualPresentationProbe.cooking().output().equals(outputId)
                && QaContextualPresentationProbe.slots().stream().anyMatch(slot -> slot.index()==2 && slot.item().equals(outputId) && slot.count()==1));
            capture("34c-" + name + "-processed-native-output"); driver.rightTrigger = 0;
        }, 2);
        add("contextual-" + name + "-release-wheel", () -> { driver.rightTrigger = 0; driver.rightX = driver.rightY = 0; driver.stick(0, 0); },
            () -> mc.gui.screen() instanceof RadialInventoryScreen screen && !screen.contextualRadialActive(), () -> {}, 3);
        assertServer("contextual-"+name+"-consumed-hover-input-is-not-restored",() -> {
            var menu=contextualServerCookingMenu(index);
            return menu.getSlot(0).getItem().isEmpty() && menu.getSlot(2).getItem().is(outputItem)
                && menu.getSlot(2).getItem().getCount()==1 && count(contextualIngredientPos,input)==(index==0?1:0);
        });
    }

    private static void addCookingHoverLifecycleScenarios() {
        focusPair("contextual-cooking-hover-lifecycle-pair",() -> contextualCookingWindows[0],() -> owner.model().playerInventory());
        add("contextual-cooking-hover-clear-native-fixture",() -> server(() -> {
            contextualHoverXpBefore=player().totalExperience;player().getInventory().clearContent();
            var menu=contextualServerCookingMenu(0);menu.getSlot(0).set(new ItemStack(Items.BEDROCK,2));
            menu.getSlot(1).set(new ItemStack(Items.COAL,5));menu.getSlot(2).set(new ItemStack(Items.IRON_INGOT,8));
            var furnace=(AbstractFurnaceBlockEntity)player().level().getBlockEntity(contextualCookingPositions[0]);
            var recipe=mc.getSingleplayerServer().getRecipeManager().byKey(net.minecraft.resources.ResourceKey.create(Registries.RECIPE,
                Identifier.withDefaultNamespace("iron_ingot_from_smelting_raw_iron"))).orElseThrow();
            for(int i=0;i<8;i++) furnace.setRecipeUsed(recipe);
            var origin=(Container)player().level().getBlockEntity(contextualIngredientPos);origin.clearContent();origin.setItem(6,new ItemStack(Items.RAW_IRON));
            contextualOwnedIronBeforeClear=contextualOwnedCount(Items.IRON_INGOT,null);contextualOwnedBedrockBeforeClear=contextualOwnedCount(Items.BEDROCK,null);
            menu.broadcastChanges();player().inventoryMenu.broadcastChanges();return Map.of("sourceInput",menu.getSlot(0).getItem().toString(),
                "sourceOutput",menu.getSlot(2).getItem().toString(),"sourceFuel",menu.getSlot(1).getItem().toString(),
                "ownedIron",contextualOwnedIronBeforeClear,"ownedBedrock",contextualOwnedBedrockBeforeClear,
                "ironLocations",contextualOwnedLocations(Items.IRON_INGOT),"bedrockLocations",contextualOwnedLocations(Items.BEDROCK));
        }),ControlifyGameplayHarness::serverDone,() -> record("contextual-cooking-entry-owned-baseline","OBSERVED",serverTask.join().toString()),10);
        add("contextual-cooking-hover-clear-native-RT",() -> {
            pointAtNativeSlot(contextualCookingWindows[0],slot -> slot.index==0);driver.rightTrigger=1;
        },() -> contextualOverlay().active(),() -> {},3);
        assertServer("contextual-cooking-enter-clears-owned-input-output-and-preserves-fuel",() -> {
            var menu=contextualServerCookingMenu(0);
            int iron=contextualOwnedCount(Items.IRON_INGOT,null),bedrock=contextualOwnedCount(Items.BEDROCK,null);
            boolean passed=menu.getSlot(0).getItem().isEmpty() && menu.getSlot(2).getItem().isEmpty()
                && menu.getSlot(1).getItem().is(Items.COAL) && menu.getSlot(1).getItem().getCount()==5
                && contextualOwnedBedrockBeforeClear>=2 && bedrock==contextualOwnedBedrockBeforeClear
                && contextualOwnedIronBeforeClear>=8 && iron==contextualOwnedIronBeforeClear;
            if(passed) QaHoverPacketProbe.cookingEntryObserved(contextualOwnedIronBeforeClear,iron,contextualOwnedBedrockBeforeClear,bedrock,
                menu.getSlot(0).getItem().getCount(),menu.getSlot(2).getItem().getCount(),menu.getSlot(1).getItem().getCount(),
                contextualOwnedLocations(Items.IRON_INGOT),contextualOwnedLocations(Items.BEDROCK));return passed;
        });
        add("contextual-cooking-hover-output-XP-settle",() -> {},() -> true,() -> {},55);
        assertServer("contextual-cooking-cleared-real-output-retains-native-XP-callback",() -> player().totalExperience>contextualHoverXpBefore);
        add("contextual-cooking-hover-refill-native-select",() -> aimContextualRecipe(Items.IRON_INGOT,true),
            () -> contextualRecipeReady(Items.IRON_INGOT,true),() -> {driver.rightX=driver.rightY=0;},3);
        assertServer("contextual-cooking-hover-borrows-input-from-exact-origin",() -> {
            var menu=contextualServerCookingMenu(0);
            return menu.getSlot(0).getItem().is(Items.RAW_IRON) && menu.getSlot(0).getItem().getCount()==1
                && ((Container)player().level().getBlockEntity(contextualIngredientPos)).getItem(6).isEmpty()
                && menu.getSlot(1).getItem().is(Items.COAL) && menu.getSlot(1).getItem().getCount()==5;
        });
        assertServer("contextual-cooking-hover-native-consumption-before-refill",() -> {
            var menu=contextualServerCookingMenu(0);return menu.getSlot(0).getItem().isEmpty()
                && menu.getSlot(2).getItem().is(Items.IRON_INGOT) && menu.getSlot(2).getItem().getCount()==1;
        });
        add("contextual-cooking-hover-hopper-refill-fixture",() -> server(() -> {
            contextualServerCookingMenu(0).getSlot(0).set(new ItemStack(Items.RAW_IRON));contextualServerCookingMenu(0).broadcastChanges();return null;
        }),ControlifyGameplayHarness::serverDone,() -> {driver.rightTrigger=0;},0);
        add("contextual-cooking-hover-refill-release",() -> {driver.rightTrigger=0;driver.stick(0,0);},() -> !contextualOverlay().active(),() -> {},2);
        assertServer("contextual-cooking-consumed-unit-not-returned-as-same-variant-refill",() -> {
            var menu=contextualServerCookingMenu(0);
            return ((Container)player().level().getBlockEntity(contextualIngredientPos)).getItem(6).isEmpty()
                && menu.getSlot(0).getItem().is(Items.RAW_IRON) && menu.getSlot(0).getItem().getCount()==1
                && menu.getSlot(2).getItem().is(Items.IRON_INGOT) && menu.getSlot(2).getItem().getCount()==1
                && contextualOwnedCount(Items.RAW_IRON,null)==1;
        });
    }

    private static void addLoomResultScenarios() {
        openContainer("open-native-loom", () -> loomPos, () -> loomWindow = newestOther());
        add("seed-native-loom-pattern", () -> server(() -> {
            LoomMenu menu = (LoomMenu) serverMenu(LoomMenu.class);
            menu.getSlot(0).set(new ItemStack(Items.BANNER.white(), 2));
            menu.getSlot(1).set(new ItemStack(Items.DYE.red(), 2));
            boolean selected = menu.clickMenuButton(player(), 0);
            menu.broadcastChanges();
            return selected && menu.getResultSlot().getItem().is(Items.BANNER.white())
                    && menu.getResultSlot().getItem().getOrDefault(DataComponents.BANNER_PATTERNS, BannerPatternLayers.EMPTY).layers().size() == 1;
        }), ControlifyGameplayHarness::serverDone, () -> check("loom-authentic-pattern-result", (Boolean) serverTask.join()), 10);
        focusPair("loom-to-real-container", () -> loomWindow, () -> barrelWindow);
        quickMoveSlot("loom-session-result-slot-three", () -> loomWindow, 3);
        assertServer("loom-native-inputs-consumed-once-output-replenished", () -> {
            LoomMenu menu = (LoomMenu) serverMenu(LoomMenu.class);
            return menu.getSlot(0).getItem().getCount() == 1 && menu.getSlot(1).getItem().getCount() == 1
                    && menu.getResultSlot().getItem().is(Items.BANNER.white())
                    && count(barrelPos, Items.BANNER.white()) == 1 && inventoryCount(Items.BANNER.white()) == 0;
        });
        add("loom-replenished-result-capture", () -> {}, () -> true, () -> capture("11b-native-loom-replenished-result"), 3);
        selectPair("loom-to-inventory", () -> owner.model().playerInventory());
        quickMoveSlot("loom-storage-result-slot-three", () -> loomWindow, 3);
        assertServer("loom-final-output-conserved-no-extra-input-consumption", () -> {
            LoomMenu menu = (LoomMenu) serverMenu(LoomMenu.class);
            return menu.getSlot(0).getItem().isEmpty() && menu.getSlot(1).getItem().isEmpty()
                    && menu.getResultSlot().getItem().isEmpty() && count(barrelPos, Items.BANNER.white()) == 1
                    && inventoryCount(Items.BANNER.white()) == 1 && inventoryCount(Items.DYE.red()) == 0;
        });
        add("loom-final-result-capture", () -> {}, () -> true, () -> capture("11c-native-loom-result-inventory"), 3);
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
        }, () -> owner.tuckedAway() && mc.gui.screen() == null, () -> {
            verifyPreserved();
            check("UI-state-survives-tuck", viewAHandle.isOpen() && viewA.statesCreated == 1
                    && viewA.clicks == 1 && viewA.externalClicks == 1 && viewA.closed == 0);
            verifyTuckedTabs("client-UI-disabled-animation-tab"); capture("12d-tucked-client-UI-tabs");
        }, 12);
        add("client-only-view-open-while-tucked", () ->
                viewBHandle = DesktopClientApi.openView(QA_VIEW_ID, "tucked", viewB, Component.literal("QA UI added while tucked")).orElseThrow(),
                () -> owner.windows().containsKey(viewBHandle.instanceId()), () -> {
                    check("UI-open-does-not-restore-tucked", owner.tuckedAway() && mc.gui.screen() == null);
                    verifyPreserved();
                    check("both-client-UI-lifetimes-alive", viewAHandle.isOpen() && viewBHandle.isOpen()
                            && viewA.statesCreated == 1 && viewB.statesCreated == 1 && viewA.closed == 0 && viewB.closed == 0);
                    preservedWindows = Set.copyOf(owner.windows().keySet()); preservedSessions = sessions();
                    verifyTuckedTabs("new-client-UI-tucked-tab"); capture("12e-new-client-UI-tucked-tab");
                }, 6);
        button("restore-tucked-client-UI", GamepadInputs.NORTH_BUTTON, () -> {
            check("UI-restored-native-workspace", mc.gui.screen() instanceof RadialInventoryScreen && !owner.tuckedAway());
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
        add("tuck-after-client-UI-close", () -> { driver.trigger = 1; driver.stick(0, 1); },
                () -> owner.model().tuckHighlighted(), () -> {}, 3);
        add("closed-client-UI-tab-removed", () -> { driver.trigger = 0; driver.stick(0, 0); },
                () -> owner.tuckedAway() && mc.gui.screen() == null, () -> {
                    verifyTuckedTabs("closed-client-UI-tab-removed");
                    check("closed-client-UI-name-disappeared", !QaFeatureProbe.tabNames.containsKey(viewBHandle.instanceId())
                            && QaFeatureProbe.tabNames.containsKey(viewAHandle.instanceId()) && viewA.statesCreated == 1 && viewB.closed == 1);
                    capture("13b-tucked-tabs-after-window-close");
                }, 12);
        button("restore-after-final-tab-check", GamepadInputs.NORTH_BUTTON,
                () -> check("final-tabs-do-not-recreate-workspace", mc.gui.screen() instanceof RadialInventoryScreen && viewA.statesCreated == 1));
    }

    private static void addTallNativeGuideScenarios() {
        add("native-guide-open-tall-client-window", () -> {
            tallGuidePreviousFocus = owner.model().focused(); tallGuidePreviousSelected = owner.model().selected();
            tallGuideModel.height = 600;
            tallGuideHandle = DesktopClientApi.openView(QA_VIEW_ID, "guide-tall", tallGuideModel,
                Component.literal("Tall native-guide clearance QA")).orElseThrow();
        }, () -> tallGuideHandle.isOpen(), () -> {
            check("native-guide-tall-fixture-is-actually-tall", tallGuideHandle.state().height() >= 300);
        }, 4);
        focusPair("native-guide-tall-main-pair", () -> tallGuideHandle.instanceId(), () -> owner.model().playerInventory());
        nativeCursorNeutralReady("native-guide-tall-window", () -> {
            var state = tallGuideHandle.state();
            pointAtControl(tallGuideHandle.instanceId(), new DesktopWindowBounds(state.x()+20, state.y()+20, 4, 4));
        });
        observeNativeGuide("tall-window", "SIDE", "Close all windows", "Hold: window wheel", "Move cursor", "Navigate slots / controls");
        add("native-guide-tall-bottom-band-capture", () -> {}, () -> true, () -> capture("36-tall-window-native-bottom-guide"), 2);
        focusPair("native-guide-restore-pre-tall-pair", () -> tallGuidePreviousFocus, () -> tallGuidePreviousSelected);
        add("native-guide-tall-close-highlight", () -> { driver.trigger = 1; aimWindow(tallGuideHandle.instanceId()); },
            () -> owner.radialActive() && owner.model().highlighted().filter(tallGuideHandle.instanceId()::equals).isPresent(), () -> {}, 2);
        button("native-guide-tall-close-native-B", GamepadInputs.EAST_BUTTON, () ->
            check("native-guide-tall-window-native-close-isolated", !tallGuideHandle.isOpen() && tallGuideModel.closed == 1
                && viewAHandle.isOpen() && owner.windows().containsKey(owner.model().playerInventory())));
        add("native-guide-tall-close-release-wheel", () -> { driver.trigger = 0; driver.stick(0, 0); },
            () -> !owner.radialActive(), () -> {}, 3);
        add("native-guide-tall-cleanup-preserves-saved-focus", () -> {}, () -> true, () ->
            check("native-guide-tall-cleanup-preserves-saved-source", owner.model().focused().equals(tallGuidePreviousFocus)
                && owner.windows().containsKey(tallGuidePreviousFocus) && owner.windows().containsKey(tallGuidePreviousSelected)), 2);
        // The saved source is already focused and therefore absent from the radial order.
        // Restore only the side target through the genuine LT selection/release path.
        selectPair("native-guide-tall-final-pair-restore", () -> tallGuidePreviousSelected);
        add("native-guide-tall-cleanup-restored-exact-pair", () -> {}, () -> true, () ->
            check("native-guide-tall-cleanup-restores-exact-pair", owner.model().focused().equals(tallGuidePreviousFocus)
                && owner.model().selected().equals(tallGuidePreviousSelected)), 2);
    }

    private static WorkspaceLayout<UUID> ghostAnimationBefore, ghostAnimationTarget;
    private static UUID ghostOriginalSelection, ghostExpectedBumperSelection, ghostAnimationOld, ghostAnimationIncoming;
    private static int ghostAnimationDirection;

    private static void addGhostLayoutScenarios() {
        add("native-ghost-neighbor-preview-layout", () -> {
            driver.buttons.clear(); driver.trigger = 0; driver.rightTrigger = 0;
            driver.stick(0, 0); driver.rightX = 0; driver.rightY = 0;
            setAnimations(false);
        }, () -> mc.gui.screen() instanceof RadialInventoryScreen && !owner.radialActive() && !owner.tuckedAway(), () -> {
            check("ghost-fixture-has-many-native-and-client-windows", owner.model().radialOrder().size() >= 5
                    && owner.windows().containsKey(tableWindow) && owner.windows().containsKey(furnaceWindow)
                    && owner.windows().containsKey(loomWindow) && viewAHandle.isOpen());
            ghostOriginalSelection = owner.model().selected();
            verifyNativeGhostLayout("native-ghost-layout");
            capture("14-ghost-neighbor-previews");
            capture("39-associated-item-icons-side-previews");
            ghostExpectedBumperSelection = ghostCycleNeighbor(-1);
        }, 4);
        button("native-LB-selects-nearest-upper-preview", GamepadInputs.LEFT_SHOULDER_BUTTON, () -> {
            check("native-LB-target-is-first-upper-ghost", owner.model().selected().equals(ghostExpectedBumperSelection));
            verifyNativeGhostLayout("native-ghost-layout-after-LB");
            ghostExpectedBumperSelection = ghostCycleNeighbor(1);
        });
        button("native-RB-selects-nearest-lower-preview", GamepadInputs.RIGHT_SHOULDER_BUTTON, () -> {
            check("native-RB-target-is-first-lower-ghost", owner.model().selected().equals(ghostExpectedBumperSelection));
            check("native-LB-RB-preview-order-reverses", owner.model().selected().equals(ghostOriginalSelection));
            verifyNativeGhostLayout("native-ghost-layout-after-RB");
        });
        addGhostAnimationScenario("native-RB-ghost-push-animation", GamepadInputs.RIGHT_SHOULDER_BUTTON, 1,
                "14a-RB-neighbor-animation");
        addGhostAnimationScenario("native-LB-ghost-push-animation", GamepadInputs.LEFT_SHOULDER_BUTTON, -1,
                "14b-LB-neighbor-animation");
        add("native-ghost-animation-config-restored", () -> setAnimations(false), () -> true, () -> {
            check("ghost-animation-restores-disabled-config", !owner.config().animationsEnabled());
            check("ghost-animation-round-trip-keeps-selection", owner.model().selected().equals(ghostOriginalSelection));
        }, 2);
        add("native-enlarged-radial-focus-and-selection", () -> {
            driver.trigger = 1; aimWindow(barrelWindow);
        }, () -> owner.radialActive() && owner.model().highlighted().filter(barrelWindow::equals).isPresent(), () -> {
            var frame = renderedLayout();
            UUID highlighted = owner.model().highlighted().orElseThrow();
            double ordinaryDiagonal = owner.model().radialOrder().stream().filter(id -> !id.equals(highlighted))
                    .mapToDouble(id -> ghostRenderedDiagonal(frame, id)).max().orElseThrow();
            check("radial-highlight-much-larger-than-thumbnails", ghostRenderedDiagonal(frame, highlighted) > ordinaryDiagonal * 2.4);
            check("radial-focus-much-larger-than-thumbnails", ghostRenderedDiagonal(frame, owner.model().focused()) > ordinaryDiagonal * 2.4);
            check("radial-highlight-fully-opaque-and-outlined", frame.windows().get(highlighted).opacity() == 1
                    && frame.windows().get(highlighted).highlight() == 1);
            check("radial-expanded-focus-and-highlight-do-not-overlap", ghostPanesSeparate(frame, owner.model().focused(), highlighted));
            check("active-LT-focus-and-highlight-render-full-native-menus", QaVisualFeatureProbe.nativePanes().equals(Set.of(owner.model().focused(),highlighted)));
            Set<UUID> expectedIcons = new LinkedHashSet<>(frame.windows().keySet());
            expectedIcons.remove(owner.model().focused()); expectedIcons.remove(highlighted);
            expectedIcons.removeIf(id -> frame.windows().get(id).opacity() <= .005);
            check("inactive-LT-previews-render-associated-native-items", QaVisualFeatureProbe.previews().keySet().equals(expectedIcons)
                && QaVisualFeatureProbe.previews().values().stream().allMatch(preview -> !preview.active() && preview.nativeItemDraws()==1)
                && QaVisualFeatureProbe.violations().isEmpty());
            frame.windows().keySet().forEach(id -> check("radial-expanded-pane-bounded-" + id, ghostPaneBounded(frame, id)));
            capture("14c-enlarged-radial-selection");
            capture("39b-associated-item-icons-LT-wheel");
        }, 4);
        add("native-return-side-after-enlarged-radial", () -> { driver.trigger = 0; driver.stick(0, 0); },
                () -> !owner.radialActive(), () -> verifyNativeGhostLayout("native-ghost-layout-after-radial"), 5);
    }

    private static UUID ghostCycleNeighbor(int direction) {
        List<UUID> order = owner.model().radialOrder();
        return order.get(Math.floorMod(order.indexOf(owner.model().selected()) + direction, order.size()));
    }

    private static void verifyNativeGhostLayout(String name) {
        var frame = renderedLayout();
        UUID selected = owner.model().selected();
        UUID focused = owner.model().focused();
        var selection = frame.windows().get(selected);
        List<UUID> order = owner.model().radialOrder();
        int index = order.indexOf(selected);
        Map<UUID, Integer> expected = new LinkedHashMap<>();
        for (int depth = 1; depth <= Math.min(2, order.size() - 1); depth++) for (int direction : new int[]{-1, 1}) {
            UUID id = order.get(Math.floorMod(index + direction * depth, order.size()));
            if (!id.equals(selected)) expected.putIfAbsent(id, direction * depth);
        }
        Set<UUID> visibleGhosts = new LinkedHashSet<>();
        frame.windows().forEach((id, placement) -> {
            if (!id.equals(selected) && !id.equals(focused) && placement.opacity() > 0.005) visibleGhosts.add(id);
        });
        check(name + "-exact-unique-cycle-neighbors", visibleGhosts.equals(expected.keySet()) && visibleGhosts.size() == Math.min(4, order.size() - 1));
        check(name + "-primary-pair-opaque", selection.opacity() == 1 && frame.windows().get(focused).opacity() == 1);
        check("active-SIDE-pair-render-full-native-menus", QaVisualFeatureProbe.nativePanes().equals(Set.of(focused,selected)));
        check("inactive-SIDE-previews-render-associated-native-items", QaVisualFeatureProbe.previews().keySet().equals(visibleGhosts)
            && QaVisualFeatureProbe.previews().values().stream().allMatch(preview -> !preview.active() && preview.nativeItemDraws()==1)
            && QaVisualFeatureProbe.violations().isEmpty());
        double largestGhost = visibleGhosts.stream().mapToDouble(id -> ghostRenderedDiagonal(frame, id)).max().orElse(0);
        check(name + "-selected-much-larger-than-previews", ghostRenderedDiagonal(frame, selected) > largestGhost * 2);
        check(name + "-focused-much-larger-than-previews", ghostRenderedDiagonal(frame, focused) > largestGhost * 2);
        var screen = (RadialInventoryScreen) mc.gui.screen();
        List<SnapPoint> snaps = new ArrayList<>();
        screen.controlify$collectSnapPoints(snaps::add);
        double selectedCenterY = ghostCenterY(frame, selected);
        for (var entry : expected.entrySet()) {
            UUID id = entry.getKey();
            int depth = Math.abs(entry.getValue());
            var placement = frame.windows().get(id);
            check(name + "-ghost-direction-" + entry.getValue(), Integer.signum(entry.getValue()) * (ghostCenterY(frame, id) - selectedCenterY) > 0);
            check(name + "-ghost-subdued-" + entry.getValue(), placement.opacity() > 0 && placement.opacity() < 0.4);
            check(name + "-ghost-bounded-" + entry.getValue(), ghostPaneBounded(frame, id));
            if (depth == 2) {
                UUID nearest = order.get(Math.floorMod(index + Integer.signum(entry.getValue()), order.size()));
                check(name + "-second-ghost-smaller-" + entry.getValue(), ghostRenderedDiagonal(frame, id) < ghostRenderedDiagonal(frame, nearest));
                check(name + "-second-ghost-fainter-" + entry.getValue(), placement.opacity() < frame.windows().get(nearest).opacity());
            }
            var bounds = owner.desktop().presentationBounds(id);
            var slots = owner.desktop().visibleSlots(id);
            double x = slots.isEmpty() ? bounds.x() + bounds.width() / 2.0 : slots.getFirst().x() + 8;
            double y = slots.isEmpty() ? bounds.y() + bounds.height() / 2.0 : slots.getFirst().y() + 8;
            int px = (int) Math.round(placement.x() + (x - bounds.x()) * placement.scale());
            int py = (int) Math.round(placement.y() + (y - bounds.y()) * placement.scale());
            snapCursor(px, py);
            check(name + "-ghost-cannot-hover-window-" + entry.getValue(), field(screen, "hoveredWindow") == null && screen.controllerHoveredSlot() == null);
            for (var slot : slots) {
                int sx = (int) Math.round(placement.x() + (slot.x() + 8 - bounds.x()) * placement.scale());
                int sy = (int) Math.round(placement.y() + (slot.y() + 8 - bounds.y()) * placement.scale());
                check(name + "-ghost-excluded-from-native-snap-" + entry.getValue() + "-" + slot.menuSlotId(),
                        snaps.stream().noneMatch(snap -> snap.position().x() == sx && snap.position().y() == sy));
            }
        }
        List<UUID> visible = frame.windows().entrySet().stream().filter(entry -> entry.getValue().opacity() > 0.005).map(Map.Entry::getKey).toList();
        for (int i = 0; i < visible.size(); i++) for (int j = i + 1; j < visible.size(); j++)
            check(name + "-visible-panes-separate-" + i + "-" + j, ghostPanesSeparate(frame, visible.get(i), visible.get(j)));
    }

    private static void addGhostAnimationScenario(String name, Identifier button, int direction, String captureName) {
        add(name + "-intermediate", () -> {
            setAnimations(true);
            ghostAnimationBefore = renderedLayout(); ghostAnimationTarget = null;
            ghostAnimationOld = owner.model().selected(); ghostAnimationIncoming = ghostCycleNeighbor(direction);
            ghostAnimationDirection = direction;
            driver.buttons.add(button);
        }, () -> {
            if (!owner.model().selected().equals(ghostAnimationIncoming)) return false;
            if (ghostAnimationTarget == null) {
                Map<UUID, WindowSize> sizes = new LinkedHashMap<>();
                owner.windows().forEach((id, window) -> {
                    var bounds = owner.desktop().presentationBounds(id);
                    sizes.put(id, new WindowSize(bounds.width(), bounds.height()));
                });
                ghostAnimationTarget = new WorkspacePresentationLayout().compute(owner.model(), sizes,
                        QaNativeGuideProbe.layoutWidth, QaNativeGuideProbe.layoutHeight, 1);
            }
            var current = renderedLayout().windows().get(ghostAnimationIncoming);
            var origin = ghostAnimationBefore.windows().get(ghostAnimationIncoming);
            var target = ghostAnimationTarget.windows().get(ghostAnimationIncoming);
            double progress = (current.scale() - origin.scale()) / (target.scale() - origin.scale());
            return progress > 0.15 && progress < 0.85;
        }, () -> {
            var frame = renderedLayout();
            var incoming = frame.windows().get(ghostAnimationIncoming);
            var outgoing = frame.windows().get(ghostAnimationOld);
            check(name + "-incoming-grows-before-settling", incoming.scale() > ghostAnimationBefore.windows().get(ghostAnimationIncoming).scale()
                    && incoming.scale() < ghostAnimationTarget.windows().get(ghostAnimationIncoming).scale());
            check(name + "-incoming-fades-to-solid", incoming.opacity() > ghostAnimationBefore.windows().get(ghostAnimationIncoming).opacity() && incoming.opacity() < 1);
            check(name + "-outgoing-shrinks-and-fades", outgoing.scale() < ghostAnimationBefore.windows().get(ghostAnimationOld).scale()
                    && outgoing.scale() > ghostAnimationTarget.windows().get(ghostAnimationOld).scale()
                    && outgoing.opacity() < 1 && outgoing.opacity() > ghostAnimationTarget.windows().get(ghostAnimationOld).opacity());
            check(name + "-incoming-moves-from-correct-preview", ghostAnimationDirection
                    * (ghostCenterY(frame, ghostAnimationIncoming) - ghostCenterY(ghostAnimationBefore, ghostAnimationIncoming)) < 0);
            check(name + "-outgoing-pushed-in-opposite-direction", ghostAnimationDirection
                    * (ghostCenterY(frame, ghostAnimationOld) - ghostCenterY(ghostAnimationBefore, ghostAnimationOld)) < 0);
            record(name + "-native-animation-frame", "OBSERVED", "before=" + ghostAnimationBefore.windows()
                    + "; intermediate=" + frame.windows() + "; target=" + ghostAnimationTarget.windows());
            capture(captureName); driver.buttons.remove(button);
        }, 0);
        add(name + "-settled", () -> {}, () -> ghostAnimationTarget.equals(renderedLayout()), () -> {
            check(name + "-settles-on-preview-destination", owner.model().selected().equals(ghostAnimationIncoming));
            verifyNativeGhostLayout(name + "-settled-layout");
        }, 2);
    }

    private static double ghostRenderedDiagonal(WorkspaceLayout<UUID> frame, UUID id) {
        var bounds = owner.desktop().presentationBounds(id);
        return Math.hypot(bounds.width(), bounds.height()) * frame.windows().get(id).scale();
    }
    private static double ghostCenterY(WorkspaceLayout<UUID> frame, UUID id) {
        var placement = frame.windows().get(id);
        return placement.y() + owner.desktop().presentationBounds(id).height() * placement.scale() / 2.0;
    }
    private static boolean ghostPaneBounded(WorkspaceLayout<UUID> frame, UUID id) {
        var placement = frame.windows().get(id); var bounds = owner.desktop().presentationBounds(id);
        return placement.x() >= -0.01 && placement.y() >= -0.01
                && placement.x() + bounds.width() * placement.scale() <= mc.getWindow().getGuiScaledWidth() + 0.01
                && placement.y() + bounds.height() * placement.scale() <= mc.getWindow().getGuiScaledHeight() + 0.01;
    }
    private static boolean ghostPanesSeparate(WorkspaceLayout<UUID> frame, UUID a, UUID b) {
        var ap = frame.windows().get(a); var ab = owner.desktop().presentationBounds(a);
        var bp = frame.windows().get(b); var bb = owner.desktop().presentationBounds(b);
        return ap.x() + ab.width() * ap.scale() <= bp.x() + 0.01
                || bp.x() + bb.width() * bp.scale() <= ap.x() + 0.01
                || ap.y() + ab.height() * ap.scale() <= bp.y() + 0.01
                || bp.y() + bb.height() * bp.scale() <= ap.y() + 0.01;
    }

    private static void prepareUiCursor(DesktopWindowHandle handle, QaClientView.Model model, boolean external) {
        pointAtUiButton(handle, model, external);
        pointerFixture = () -> pointAtUiButton(handle, model, external);
    }
    private static void pointOutsideUi(DesktopWindowHandle handle) {
        var nativeBounds = handle.state();
        var bounds = owner.desktop().presentationBounds(handle.instanceId());
        var placement = renderedLayout().windows().get(handle.instanceId());
        int localX = nativeBounds.x() + nativeBounds.width() / 2, localY = nativeBounds.y() - 12;
        check("modal-owner-retains-outside-pointer", localY < nativeBounds.y()
                && owner.desktop().containsPointer(handle.instanceId(), localX, localY));
        int x = (int) Math.round(placement.x() + (localX - bounds.x()) * placement.scale());
        int y = (int) Math.round(placement.y() + (localY - bounds.y()) * placement.scale());
        var scale = new Vector2d((double) mc.getWindow().getGuiScaledWidth() / mc.getWindow().getScreenWidth(),
                (double) mc.getWindow().getGuiScaledHeight() / mc.getWindow().getScreenHeight());
        Controlify.instance().virtualMouseHandler().snapToPoint(new SnapPoint(x, y, 1), scale);
        ((RadialInventoryScreen) mc.gui.screen()).mouseMoved(x, y);
        check("modal-outside-hover-has-no-item-slot", ((RadialInventoryScreen) mc.gui.screen()).controllerHoveredSlot() == null);
    }
    private static void pointAtUiButton(DesktopWindowHandle handle, QaClientView.Model model, boolean external) {
        var nativeBounds = handle.state();
        var bounds = owner.desktop().presentationBounds(handle.instanceId());
        var placement = renderedLayout().windows().get(handle.instanceId());
        int localX = (external ? model.externalX : model.buttonX) + 60;
        int localY = (external ? model.externalY : model.buttonY) + 12;
        if (external) check("external-UI-hit-outside-base", localY >= nativeBounds.y() + nativeBounds.height()
                && owner.desktop().containsPointer(handle.instanceId(), localX, localY));
        int x = (int) Math.round(placement.x() + (localX - bounds.x()) * placement.scale());
        int y = (int) Math.round(placement.y() + (localY - bounds.y()) * placement.scale());
        var scale = new Vector2d((double) mc.getWindow().getGuiScaledWidth() / mc.getWindow().getScreenWidth(),
                (double) mc.getWindow().getGuiScaledHeight() / mc.getWindow().getScreenHeight());
        Controlify.instance().virtualMouseHandler().snapToPoint(new SnapPoint(x, y, 1), scale);
        ((RadialInventoryScreen) mc.gui.screen()).mouseMoved(x, y);
        check("generic-UI-hover-has-no-item-slot", ((RadialInventoryScreen) mc.gui.screen()).controllerHoveredSlot() == null);
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
        nativeCursorNeutralReady(name, () -> {
            pointAtItem(window.get(), item);
            observeQuickMovePointer(name, window.get(), item, "neutral-ready-source");
        });
        add(name, () -> {
            pointAtItem(window.get(), item);
            pointerFixture = () -> {
                pointAtItem(window.get(), item);
                observeQuickMovePointer(name, window.get(), item, "native-binding-edge");
                Slot hovered = ((RadialInventoryScreen) mc.gui.screen()).controllerHoveredSlot();
                check(name + "-native-binding-edge-hovers-source", hovered != null && hovered.getItem().is(item));
            };
            driver.buttons.add(GamepadInputs.MISC_1_BUTTON);
        }, () -> pointerFixture == null, () -> {
            check(name + "-native-pointer-prepared-at-binding-edge", pointerFixture == null);
            observeQuickMovePointer(name, window.get(), item, "after-native-action");
            driver.buttons.clear();
        }, 3);
        add(name + "-network-settle", () -> {}, () -> true, () -> {}, 8);
    }
    private static void observeQuickMovePointer(String name, UUID windowId, Item item, String phase) {
        var mouse = Controlify.instance().virtualMouseHandler();
        Slot hovered = ((RadialInventoryScreen) mc.gui.screen()).controllerHoveredSlot();
        var binding = ControlifyBindings.INV_QUICK_MOVE.on(controller);
        record(name + "-pointer-" + phase, "OBSERVED", "sourceWindow=" + windowId + ",sourceItem=" + item
            + ",nativeCursorEnabled=" + mouse.isVirtualMouseEnabled() + ",cursorPhysical=" + mouse.getCurrentX(1) + "," + mouse.getCurrentY(1)
            + ",nativeHovered=" + (hovered == null ? "none" : hovered.index + ":" + hovered.getItem())
            + ",quickPressed=" + binding.justPressed() + ",quickNow=" + binding.digitalNow() + ",quickPrevious=" + binding.digitalPrev()
            + ",physicalQuickNow=" + controller.input().orElseThrow().rawStateNow().isButtonDown(GamepadInputs.MISC_1_BUTTON)
            + ",pointerFixturePending=" + (pointerFixture != null));
    }
    private static void quickMoveSlot(String name, Supplier<UUID> window, int slotId) {
        add(name + "-native-pointer-ready", () -> {}, ControlifyGameplayHarness::nativePointerReady,
                () -> check(name + "-native-cursor-ready", nativePointerReady()), 2);
        add(name, () -> {
            pointAtNativeSlot(window.get(), slot -> slot.index == slotId && slot.hasItem());
            pointerFixture = () -> pointAtNativeSlot(window.get(), slot -> slot.index == slotId && slot.hasItem());
            driver.buttons.add(GamepadInputs.MISC_1_BUTTON);
        }, () -> pointerFixture == null, () -> {
            check(name + "-native-pointer-prepared-at-binding-edge", pointerFixture == null);
            driver.buttons.clear();
        }, 3);
        add(name + "-network-settle", () -> {}, () -> true, () -> {}, 8);
    }
    private static boolean nativePointerReady() {
        var screen = mc.gui.screen();
        return screen instanceof RadialInventoryScreen && owner.workspaceActive() && !owner.radialActive()
                && !owner.tuckedAway() && Controlify.instance().virtualMouseHandler().isVirtualMouseEnabled()
                && Controlify.instance().getCurrentController().orElse(null) == controller
                && ScreenProcessorProvider.provide(screen).screen == screen;
    }
    @SuppressWarnings("unchecked")
    private static AbstractContainerMenu serverMenu(Class<? extends AbstractContainerMenu> type) {
        Object state = ((Map<ServerPlayer, ?>) field(DesktopContainerSessions.class, "PLAYERS")).get(player());
        for (Object session : ((Map<Integer, ?>) field(state, "sessions")).values()) {
            AbstractContainerMenu menu = (AbstractContainerMenu) field(session, "menu");
            if (type.isInstance(menu)) return menu;
        }
        throw new IllegalStateException("Captured server menu missing: " + type.getSimpleName());
    }

    private static void createWorld() {
        mc.options.pauseOnLostFocus = false; mc.options.guiScale().set(2);
        mc.getWindow().setWindowed(1280, 900);
        SaltsInventoryConfig.update(config -> {
            config.enableMod = true; config.hasSeenInstructionsWindow = true; config.eHoldCloseAllSeconds = 1;
            config.returnHotbarToInventory = false; config.persistentWindows = false;
        });
        LevelSettings settings = new LevelSettings("Controller disposable QA", GameType.SURVIVAL,
                new LevelSettings.DifficultySettings(Difficulty.PEACEFUL, false, false), true, WorldDataConfiguration.DEFAULT);
        mc.createWorldOpenFlows().createFreshLevel("salts-controller-qa-" + System.currentTimeMillis(), settings,
                new WorldOptions(424242L, false, false), registry ->
                        registry.lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.FLAT).value().createWorldDimensions(), null);
    }

    @SuppressWarnings("unchecked")
    private static void installController() {
        WorkspaceOpacityProbe.reset();
        owner = new QaWorkspaceOwner((SaltsControlifyClient) field(SaltsControlifyClient.class, "instance"));
        QaVisualFeatureProbe.install(owner);
        QaContextualPresentationProbe.install(owner);
        try { owner.config().setAnimationsEnabled(true); } catch (Exception failure) { throw new IllegalStateException(failure); }
        Controlify controlify = Controlify.instance();
        nativeUiMasterVolumeBefore = mc.options.getSoundSourceOptionInstance(net.minecraft.sounds.SoundSource.MASTER).get();
        nativeUiVolumeBefore = mc.options.getSoundSourceOptionInstance(net.minecraft.sounds.SoundSource.UI).get();
        mc.options.getSoundSourceOptionInstance(net.minecraft.sounds.SoundSource.MASTER).set(1.0);
        mc.options.getSoundSourceOptionInstance(net.minecraft.sounds.SoundSource.UI).set(1.0);
        Object manager = controlify.getControllerManager().orElseThrow();
        var globals = controlify.config().getSettings().globalSettings();
        // Windows can deliver native cursor events while this synthetic controller runs.
        // The supported mixed mode keeps those OS events from changing the workspace host.
        globals.mixedInput = true; globals.outOfFocusInput = true; globals.showSplitscreenAd = false;
        ProfileSettings profile = ProfileSettings.createDefault();
        profile.generic.guide.showIngameGuide = false;
        profile.generic.guide.verbosity = dev.isxander.controlify.api.contextual.GuideVerbosity.FULL;
        controller = new ControllerEntity(new ControllerInfo(new QaId("qa-controller"), ControllerType.DEFAULT, Optional.empty()),
                driver, profile, ProfileSettings.createDefault(),
                ControlifyLogger.createMasterLogger(LoggerFactory.getLogger("Controlify QA driver")));
        ((Map<String, ControllerEntity>) field(manager, "controllersByUid")).put(controller.uid(), controller);
        controlify.setCurrentController(controller, true); controlify.setInputMode(InputMode.MIXED);
        check("real-input-bindings-created", controller.input().orElseThrow().getAllBindings().size() > 20);
        check("disposable-QA-host-motion-isolation-active", isolateHostMouseMotion());
    }

    /** Prevent the desktop mouse from competing with the installed disposable test controller. */
    public static boolean isolateHostMouseMotion() {
        return !finished && controller != null && mc != null && mc.player != null
            && mc.player.level() != null && Controlify.instance().getCurrentController().orElse(null) == controller;
    }

    private static final class TestDriver implements Driver {
        final Set<Identifier> buttons = new HashSet<>();
        float trigger, rightTrigger, x, y, rightX, rightY, walk;
        BlockPos useTarget;

        @Override public void addComponents(ControllerEntity controller) {
            controller.setComponent(new UIDComponent("salts-controller-qa-only"));
            controller.setComponent(new GUIDComponent("salts-qa"));
            controller.setComponent(new DriverNameComponent("Disposable QA virtual controller"));
            controller.setComponent(new InputComponent(controller, 40, 12, 0, true, GamepadInputs.DEADZONE_GROUPS, "default"));
        }
        @Override public void update(ControllerEntity controller, boolean outOfFocus) {
            if (useTarget != null && mc.gui.screen() == null) {
                // Keep the genuine camera ray aimed while vanilla handles the configured USE binding.
                aimCameraAt(useTarget);
            }
            ControllerStateImpl state = new ControllerStateImpl();
            buttons.forEach(button -> state.setButton(button, true));
            state.setAxis(GamepadInputs.LEFT_TRIGGER_AXIS, trigger);
            state.setAxis(GamepadInputs.RIGHT_TRIGGER_AXIS, rightTrigger);
            state.setAxis(GamepadInputs.LEFT_STICK_AXIS_RIGHT, Math.max(0, x));
            state.setAxis(GamepadInputs.LEFT_STICK_AXIS_LEFT, Math.max(0, -x));
            state.setAxis(GamepadInputs.LEFT_STICK_AXIS_DOWN, Math.max(0, y));
            state.setAxis(GamepadInputs.LEFT_STICK_AXIS_UP, Math.max(Math.max(0, -y), walk));
            state.setAxis(GamepadInputs.RIGHT_STICK_AXIS_RIGHT, Math.max(0, rightX));
            state.setAxis(GamepadInputs.RIGHT_STICK_AXIS_LEFT, Math.max(0, -rightX));
            state.setAxis(GamepadInputs.RIGHT_STICK_AXIS_DOWN, Math.max(0, rightY));
            state.setAxis(GamepadInputs.RIGHT_STICK_AXIS_UP, Math.max(0, -rightY));
            controller.input().orElseThrow().pushState(state);
        }
        void stick(float x, float y) { this.x = x; this.y = y; }
    }

    @SuppressWarnings("unchecked")
    private static WorkspaceLayout<UUID> renderedLayout() {
        return Objects.requireNonNull((WorkspaceLayout<UUID>) field(mc.gui.screen(), "placements"), "Rendered controller layout");
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
                    ((RadialInventoryScreen) mc.gui.screen()).mouseMoved(px, py);
                    check("real-hovered-item-slot", ((RadialInventoryScreen) mc.gui.screen()).controllerHoveredSlot() == slot);
                    return;
                }
            }
        throw new IllegalStateException("Visible native slot target missing in " + id);
    }

    private static void button(String name, Identifier button, Runnable verify) {
        add(name + "-press", () -> driver.buttons.add(button), () -> true, () -> {}, 2);
        add(name, () -> driver.buttons.remove(button), () -> true, verify, 3);
    }
    private static void nativePointerClick(String name, Identifier button, Runnable preparePointer, Runnable verify) {
        nativeCursorNeutralReady(name, preparePointer);
        add(name + "-press", () -> {
            preparePointer.run();
            pointerFixture = () -> {
                preparePointer.run();
                var mouse = Controlify.instance().virtualMouseHandler();
                record(name + "-native-click-pointer", "OBSERVED", "button=" + button + ",enabled=" + mouse.isVirtualMouseEnabled()
                    + ",cursorPhysical=" + mouse.getCurrentX(1) + "," + mouse.getCurrentY(1)
                    + ",hovered=" + ((RadialInventoryScreen) mc.gui.screen()).controllerHoveredSlot()
                    + ",leftPressed=" + ControlifyBindings.VMOUSE_LCLICK.on(controller).justPressed()
                    + ",rightPressed=" + ControlifyBindings.VMOUSE_RCLICK.on(controller).justPressed());
            };
            driver.buttons.add(button);
        }, () -> pointerFixture == null, () -> {
            check(name + "-native-pointer-prepared-at-binding-edge", pointerFixture == null);
            driver.buttons.remove(button);
        }, 0);
        add(name, () -> driver.buttons.remove(button), () -> true, verify, 3);
    }
    private static void pointOutsideExpandedGuideChoice() {
        Object window = nativeWindow(owner.model().character());
        pointAtControl(owner.model().character(), new DesktopWindowBounds(
            (Integer) field(window, "recipeBookX") + 2, (Integer) field(window, "recipeBookY") + 2, 2, 2));
    }

    private static void openContainer(String name, Supplier<BlockPos> pos, Runnable verify) {
        add(name, () -> server(() -> {
            ServerPlayer player = player();
            var level = player.level();
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
        for (DesktopContainerSession session : (List<DesktopContainerSession>) field(desktop, "sessions"))
            result.put(session.sessionId(), new SessionIdentity(session, session.sessionToken()));
        return result;
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
        Container container = (Container) player().level().getBlockEntity(position);
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
    private static void assertServer(String name, BooleanSupplier condition) {
        add(name, () -> {
            lastRecordedSortCounts = "";
            server(() -> observeServerCondition(name, condition));
        }, () -> {
            if (!serverDone()) return false;
            if (name.contains("sort") && !sortServerCounts.equals(lastRecordedSortCounts)) {
                lastRecordedSortCounts = sortServerCounts;
                record(name + "-actual-server-counts", "OBSERVED", sortServerCounts + "; actualClientSortRequests=" + QaFeatureProbe.sortRequests());
            }
            if ((Boolean) serverTask.join()) return true;
            // Network dispatch and the integrated server tick can trail a client frame.
            // Observe until the authentic server reaches the required result, with the
            // scenario's normal timeout still rejecting a result that never arrives.
            server(() -> observeServerCondition(name, condition));
            return false;
        }, () -> check(name, (Boolean) serverTask.join()), 2);
    }
    private static boolean observeServerCondition(String name, BooleanSupplier condition) {
        boolean passed = condition.getAsBoolean();
        if (name.startsWith("creative-")) {
            String hotbar = java.util.stream.IntStream.range(0, 9).mapToObj(i -> i + ":" + player().getInventory().getItem(i))
                .collect(java.util.stream.Collectors.joining(";"));
            String observation = "condition=" + name + ",passed=" + passed + ",expectedItem=" + creativeNavigationItem
                + ",expectedItemCount=" + inventoryCount(creativeNavigationItem) + ",creative=" + player().isCreative()
                + ",hotbar=" + hotbar + ",nativeCarried=" + player().inventoryMenu.getCarried();
            if (!observation.equals(creativeNavigationServerObservation)) {
                creativeNavigationServerObservation = observation;
                System.out.println("CONTROLIFY_QA_CREATIVE_SERVER " + observation);
            }
        }
        if (name.contains("sort")) sortServerCounts = "sourceGold=" + count(chestPos, Items.GOLD_INGOT)
                + ",sourceEmerald=" + count(chestPos, Items.EMERALD) + ",sourceGravel=" + count(chestPos, Items.GRAVEL)
                + ",unseenBarrelEmerald=" + count(barrelPos, Items.EMERALD)
                + ",unseenBarrelGold=" + count(barrelPos, Items.GOLD_INGOT)
                + ",selectedChestGold=" + count(sortTargetPos, Items.GOLD_INGOT)
                + ",selectedChestEmerald=" + count(sortTargetPos, Items.EMERALD)
                + ",selectedChestGravel=" + count(sortTargetPos, Items.GRAVEL)
                + ",playerGold=" + inventoryCount(Items.GOLD_INGOT) + ",playerGravel=" + inventoryCount(Items.GRAVEL);
        if(name.startsWith("contextual-hover-") && !passed) {
            String observation="condition="+name+",gridLogs="+contextualGridCount(Items.OAK_LOG)
                +",exactOrigin="+contextualExactOriginRestored()+",ownedOriginLogs="+contextualOwnedOriginLogs()
                +",diamonds="+inventoryCount(Items.DIAMOND)+",carried="+player().inventoryMenu.getCarried()
                +",packets="+QaHoverPacketProbe.snapshot();
            if(!observation.equals(lastRecordedSortCounts)) {lastRecordedSortCounts=observation;System.out.println("CONTROLIFY_QA_HOVER_SERVER "+observation);}
        }
        if(name.startsWith("contextual-cooking-") && !passed) {
            var menu=contextualServerCookingMenu(0);
            String observation="condition="+name+",input="+menu.getSlot(0).getItem()+",fuel="+menu.getSlot(1).getItem()+",output="+menu.getSlot(2).getItem()
                +",XP="+player().totalExperience+",beforeXP="+contextualHoverXpBefore+",bedrock="+contextualOwnedCount(Items.BEDROCK,null)+"/"+contextualOwnedBedrockBeforeClear
                +",iron="+contextualOwnedCount(Items.IRON_INGOT,null)+"/"+contextualOwnedIronBeforeClear+",bedrockLocations="+contextualOwnedLocations(Items.BEDROCK)
                +",ironLocations="+contextualOwnedLocations(Items.IRON_INGOT)+",rawIronLocations="+contextualOwnedLocations(Items.RAW_IRON);
            if(!observation.equals(lastRecordedSortCounts)) {lastRecordedSortCounts=observation;System.out.println("CONTROLIFY_QA_COOKING_SERVER "+observation);}
        }
        return passed;
    }
    private static <T> void server(Supplier<T> action) {
        CompletableFuture<T> future = new CompletableFuture<>(); serverTask = future;
        mc.getSingleplayerServer().execute(() -> {
            try { future.complete(action.get()); } catch (Throwable failure) { future.completeExceptionally(failure); }
        });
    }
    private static boolean serverDone() { if (serverTask.isCompletedExceptionally()) serverTask.join(); return serverTask.isDone(); }
    private static ServerPlayer player() { return mc.getSingleplayerServer().getPlayerList().getPlayers().get(0); }

    private static void addCreativeNavigationScenarios() {
        add("controller-navigation-brewing-fixture", () -> server(() -> {
            creativeNavigationBrewingPos = tablePos.offset(0, 0, 4);
            var level = player().level();
            level.setBlock(creativeNavigationBrewingPos.below(), Blocks.STONE.defaultBlockState(), 3);
            level.setBlock(creativeNavigationBrewingPos, Blocks.BREWING_STAND.defaultBlockState(), 3);
            return null;
        }), ControlifyGameplayHarness::serverDone, () -> {}, 2);
        openContainer("controller-navigation-open-brewing", () -> creativeNavigationBrewingPos, () -> {
            creativeNavigationBrewingWindow = newestOther();
            pointAtNativeSlot(creativeNavigationBrewingWindow, slot -> slot.index == 0);
            var middle = owner.desktop().visibleSlots(creativeNavigationBrewingWindow).stream().filter(slot -> slot.menuSlotId() == 1).findFirst().orElseThrow();
            setDpadExpected(creativeNavigationBrewingWindow, new DesktopWindowBounds(middle.x(), middle.y(), 16, 16));
        });
        nativeBrewingDpad("native-DPAD-brewing-left-to-middle", 0, 1, () -> verifyDpadTarget("brewing-staggered-middle-bottle-reached"));
        add("controller-navigation-brewing-right-target", () -> {
            var right = owner.desktop().visibleSlots(creativeNavigationBrewingWindow).stream().filter(slot -> slot.menuSlotId() == 2).findFirst().orElseThrow();
            setDpadExpected(creativeNavigationBrewingWindow, new DesktopWindowBounds(right.x(), right.y(), 16, 16));
        }, () -> true, () -> {}, 0);
        nativeBrewingDpad("native-DPAD-brewing-middle-to-right", 1, 2, () -> {
            verifyDpadTarget("brewing-next-bottle-reached"); capture("27-logical-brewing-navigation");
        });
        button("controller-navigation-close-before-creative", GamepadInputs.EAST_BUTTON, () -> check("creative-stage-side-B-closed-immediately", mc.gui.screen() == null && !owner.workspaceActive()));
        add("controller-navigation-server-creative-mode", () -> server(() -> {
            player().setGameMode(GameType.CREATIVE);
            player().getInventory().clearContent(); player().inventoryMenu.setCarried(ItemStack.EMPTY);
            player().inventoryMenu.broadcastChanges();
            return null;
        }), () -> serverDone() && mc.player.isCreative(), () -> {}, 8);
        button("controller-navigation-Y-opens-creative-workspace", GamepadInputs.NORTH_BUTTON, () -> {
            check("creative-native-workspace-available", nativePointerReady());
            var creative = owner.desktop().contextualMenu(owner.model().playerInventory());
            check("creative-tab-snapshot-exists", creative.kind() == com.salts_inventory_update.api.client.desktop.DesktopContextualMenu.Kind.CREATIVE && creative.tabs().size() >= 2);
            creativeNavigationTabId = creative.tabs().stream().filter(tab -> tab.id().equals(Identifier.withDefaultNamespace("building_blocks"))).findFirst().orElseThrow().id();
            check("creative-building-tab-fixture", owner.desktop().selectContextualTab(owner.model().playerInventory(), creativeNavigationTabId));
        });
        add("creative-catalog-native-pointer-ready", () -> {}, ControlifyGameplayHarness::nativePointerReady, () -> {
            var tab = (net.minecraft.world.item.CreativeModeTab) field(creativeNavigationWindow(), "creativeSelectedTab");
            creativeNavigationItem = tab.getDisplayItems().iterator().next().getItem();
            creativeNavigationPointCell(0, 0);
            pointerFixture = () -> creativeNavigationPointCell(0, 0);
            check("creative-catalog-has-rigid-items-and-tabs", owner.desktop().controllerTargets(owner.model().playerInventory()).stream().anyMatch(t -> t.group().equals("creative-catalog"))
                && owner.desktop().controllerTargets(owner.model().playerInventory()).stream().anyMatch(t -> t.group().equals("creative-top-tabs")));
            creativeNavigationObserveClient("before-native-A");
        }, 3);
        observeNativeGuide("creative-catalog", "SIDE", "Close all windows", "Hold: window wheel", "Hold: creative tab wheel",
            "Add one to hotbar", "Add stack to hotbar", "Previous / next window", "Move cursor", "Navigate / scroll rows", "Scroll list");
        button("native-creative-A-adds-one-hotbar", GamepadInputs.SOUTH_BUTTON, () -> {
            creativeNavigationObserveClient("after-native-A");
            capture("28-creative-controller-catalog");
        });
        assertServer("creative-A-one-item-no-carried-stack", () -> inventoryCount(creativeNavigationItem) == 1 && player().inventoryMenu.getCarried().isEmpty()
            && player().getInventory().getItem(0).is(creativeNavigationItem) && player().getInventory().getItem(0).getCount() == 1);
        add("creative-Y-stack-pointer", () -> creativeNavigationPointCell(0, 0), ControlifyGameplayHarness::nativePointerReady, () -> {}, 3);
        button("native-creative-Y-adds-stack-hotbar", GamepadInputs.NORTH_BUTTON, () -> {
            creativeNavigationObserveClient("after-native-Y");
            check("creative-Y-does-not-close-workspace", nativePointerReady());
        });
        assertServer("creative-Y-legal-stack-no-carried-item", () -> inventoryCount(creativeNavigationItem) == 1 + new ItemStack(creativeNavigationItem).getMaxStackSize()
            && player().inventoryMenu.getCarried().isEmpty()
            && player().getInventory().getItem(0).getCount() == new ItemStack(creativeNavigationItem).getMaxStackSize()
            && player().getInventory().getItem(1).getCount() == 1);
        add("native-guide-creative-stack-rebind", () -> {
            contextualDefaultGlyph = ControlifyBindings.INVENTORY.on(controller).inputGlyph();
            ControlifyBindings.INVENTORY.on(controller).setBoundInput(new ButtonInput(GamepadInputs.MISC_3_BUTTON));
        }, () -> true, () -> {}, 2);
        nativeCursorNeutralReady("native-guide-creative-stack-rebound", () -> creativeNavigationPointCell(0, 0));
        observeNativeGuide("creative-stack-rebound", "SIDE", "Add one to hotbar", "Add stack to hotbar", "Hold: creative tab wheel");
        add("native-guide-creative-stack-rebound-glyph", () -> {}, () -> true, () ->
            check("native-guide-creative-stack-actual-rebound-glyph", !ControlifyBindings.INVENTORY.on(controller).inputGlyph().equals(contextualDefaultGlyph)
                && QaNativeGuideProbe.workspace.glyph("Add stack to hotbar", ControlifyBindings.INVENTORY.on(controller).inputGlyph())), 2);
        button("native-creative-old-Y-does-not-add-stack", GamepadInputs.NORTH_BUTTON, () ->
            check("creative-rebound-old-Y-retains-workspace", nativePointerReady()));
        assertServer("creative-rebound-old-Y-no-hotbar-mutation", () -> inventoryCount(creativeNavigationItem) == 1 + new ItemStack(creativeNavigationItem).getMaxStackSize()
            && player().inventoryMenu.getCarried().isEmpty());
        nativeCursorNeutralReady("creative-remapped-stack-ready", () -> creativeNavigationPointCell(0, 0));
        button("native-creative-rebound-inventory-adds-stack", GamepadInputs.MISC_3_BUTTON, () -> {});
        assertServer("creative-rebound-inventory-action-adds-legal-stack", () -> inventoryCount(creativeNavigationItem) == 1 + 2 * new ItemStack(creativeNavigationItem).getMaxStackSize()
            && player().inventoryMenu.getCarried().isEmpty());
        add("native-guide-creative-stack-binding-restore", () -> {
            var binding = ControlifyBindings.INVENTORY.on(controller); binding.setBoundInput(binding.defaultInput());
        }, () -> true, () -> {}, 3);
        add("native-guide-creative-select-rebind", () -> {
            contextualDefaultGlyph = ControlifyBindings.INV_SELECT.on(controller).inputGlyph();
            ControlifyBindings.INV_SELECT.on(controller).setBoundInput(new ButtonInput(GamepadInputs.MISC_2_BUTTON));
            check("creative-remap-keeps-native-left-click-SOUTH", ControlifyBindings.VMOUSE_LCLICK.on(controller).boundInput()
                .equals(new ButtonInput(GamepadInputs.SOUTH_BUTTON)));
            check("creative-remap-keeps-native-shift-click-NORTH", ControlifyBindings.VMOUSE_SHIFT_CLICK.on(controller).boundInput()
                .equals(new ButtonInput(GamepadInputs.NORTH_BUTTON)));
        }, () -> true, () -> {}, 2);
        nativeCursorNeutralReady("native-guide-creative-select-rebound", () -> creativeNavigationPointCell(0, 0));
        observeNativeGuide("creative-select-rebound", "SIDE", "Add one to hotbar", "Add stack to hotbar", "Hold: creative tab wheel");
        add("native-guide-creative-select-rebound-glyph", () -> {}, () -> true, () ->
            check("native-guide-creative-select-actual-rebound-glyph", !ControlifyBindings.INV_SELECT.on(controller).inputGlyph().equals(contextualDefaultGlyph)
                && QaNativeGuideProbe.workspace.glyph("Add one to hotbar", ControlifyBindings.INV_SELECT.on(controller).inputGlyph())), 2);
        button("native-creative-old-SOUTH-does-not-add-one", GamepadInputs.SOUTH_BUTTON, () -> {
            creativeNavigationObserveClient("after-rebound-old-SOUTH");
            check("creative-rebound-old-SOUTH-retains-workspace", nativePointerReady());
        });
        assertServer("creative-rebound-old-SOUTH-no-hotbar-mutation", () -> inventoryCount(creativeNavigationItem) == 1 + 2 * new ItemStack(creativeNavigationItem).getMaxStackSize()
            && player().inventoryMenu.getCarried().isEmpty());
        nativeCursorNeutralReady("creative-remapped-select-ready", () -> creativeNavigationPointCell(0, 0));
        button("native-creative-rebound-select-adds-one", GamepadInputs.MISC_2_BUTTON, () ->
            creativeNavigationObserveClient("after-rebound-native-select"));
        assertServer("creative-rebound-select-action-adds-exactly-one", () -> inventoryCount(creativeNavigationItem) == 2 + 2 * new ItemStack(creativeNavigationItem).getMaxStackSize()
            && player().inventoryMenu.getCarried().isEmpty());
        add("native-guide-creative-select-binding-restore", () -> {
            var binding = ControlifyBindings.INV_SELECT.on(controller); binding.setBoundInput(binding.defaultInput());
        }, () -> true, () -> {}, 3);
        add("creative-full-hotbar-fixture", () -> server(() -> {
            for (int i = 0; i < 9; i++) player().getInventory().setItem(i, new ItemStack(Items.BARRIER, 64));
            player().inventoryMenu.broadcastChanges(); return null;
        }), ControlifyGameplayHarness::serverDone, () -> {
            creativeNavigationPointCell(0, 0); pointerFixture = () -> creativeNavigationPointCell(0, 0);
        }, 8);
        button("native-creative-full-hotbar-A", GamepadInputs.SOUTH_BUTTON, () -> creativeNavigationObserveClient("after-native-full-hotbar-A"));
        assertServer("creative-full-hotbar-preserves-existing-stacks", () -> inventoryCount(Items.BARRIER) == 576 && inventoryCount(creativeNavigationItem) == 0
            && player().inventoryMenu.getCarried().isEmpty());
        add("creative-DPAD-top-tab-source", () -> {
            var tabs = owner.desktop().controllerTargets(owner.model().playerInventory()).stream().filter(t -> t.group().equals("creative-top-tabs"))
                .sorted(Comparator.comparingInt(com.salts_inventory_update.api.client.desktop.DesktopControllerTarget::column)).toList();
            check("creative-visible-top-tabs", tabs.size() >= 2);
            var first = tabs.get(0); var next = tabs.get(1);
            pointAtControl(owner.model().playerInventory(), new DesktopWindowBounds(first.x(), first.y(), first.width(), first.height()));
            setDpadExpected(owner.model().playerInventory(), new DesktopWindowBounds(next.x(), next.y(), next.width(), next.height()));
        }, () -> true, () -> {}, 2);
        button("native-DPAD-creative-tabs", GamepadInputs.DPAD_RIGHT_BUTTON, () -> verifyDpadTarget("creative-rigid-next-tab-reached"));
        add("creative-DPAD-bottom-row-source", () -> {
            owner.desktop().selectContextualTab(owner.model().playerInventory(), creativeNavigationTabId);
            creativeNavigationPointCell(4, 0);
            creativeNavigationScrollBefore = (Integer) field(creativeNavigationWindow(), "creativeScrollRow");
            creativeNavigationCursorX = Controlify.instance().virtualMouseHandler().getCurrentX(1f);
            creativeNavigationCursorY = Controlify.instance().virtualMouseHandler().getCurrentY(1f);
        }, () -> true, () -> {}, 3);
        button("native-DPAD-creative-scroll-bottom", GamepadInputs.DPAD_DOWN_BUTTON, () -> {
            check("creative-DPAD-scrolls-one-row-before-hotbar", (Integer) field(creativeNavigationWindow(), "creativeScrollRow") == creativeNavigationScrollBefore + 1);
            check("creative-DPAD-scroll-keeps-cursor-in-catalog", Controlify.instance().virtualMouseHandler().getCurrentX(1f) == creativeNavigationCursorX
                && Controlify.instance().virtualMouseHandler().getCurrentY(1f) == creativeNavigationCursorY);
        });
        add("creative-right-stick-deadzone", () -> {
            creativeNavigationPointCell(4, 0);
            creativeNavigationScrollBefore = (Integer) field(creativeNavigationWindow(), "creativeScrollRow");
            driver.rightY = 0.4f;
        }, () -> true, () -> {
            driver.rightY = 0;
            check("creative-right-stick-drift-does-not-scroll", (Integer) field(creativeNavigationWindow(), "creativeScrollRow") == creativeNavigationScrollBefore);
        }, 5);
        add("creative-right-stick-controlled-scroll", () -> {
            creativeNavigationScrollStarted = System.nanoTime();
            driver.rightY = 1;
        }, () -> System.nanoTime() - creativeNavigationScrollStarted >= 450_000_000L, () -> {
            driver.rightY = 0;
            int rows = (Integer) field(creativeNavigationWindow(), "creativeScrollRow") - creativeNavigationScrollBefore;
            check("creative-right-stick-scroll-rate-controlled", rows >= 1 && rows <= 3);
            capture("29-creative-row-scroll");
        }, 0);
        add("creative-RT-tab-wheel-open", () -> {
            creativeNavigationPointCell(0, 0); driver.rightTrigger = 1;
        }, () -> ((RadialInventoryScreen) mc.gui.screen()).contextualRadialActive(), () -> {
            Object overlay = field(mc.gui.screen(), "contextual");
            check("creative-RT-wheel-kind", field(overlay, "snapshot") instanceof com.salts_inventory_update.api.client.desktop.DesktopContextualMenu content
                && content.kind() == com.salts_inventory_update.api.client.desktop.DesktopContextualMenu.Kind.CREATIVE);
            var content = owner.desktop().contextualMenu(owner.model().playerInventory());
            creativeNavigationNativeActiveBefore = content.tabs().stream().filter(tab -> tab.selected()).findFirst().orElseThrow().id();
            creativeNavigationTabIndex = 0;
            while (content.tabs().get(creativeNavigationTabIndex).selected()) creativeNavigationTabIndex++;
            check("creative-RT-active-native-tab-rendered", QaFeatureProbe.activeCreativeTabCount == 1);
            double angle = (creativeNavigationTabIndex + 0.5) * Math.PI * 2 / content.tabs().size();
            driver.rightX = (float) Math.sin(angle); driver.rightY = (float) -Math.cos(angle);
            driver.stick(0.7f, 0);
            creativeNavigationCursorX = Controlify.instance().virtualMouseHandler().getCurrentX(1f);
            creativeNavigationCursorY = Controlify.instance().virtualMouseHandler().getCurrentY(1f);
        }, 3);
        add("creative-RT-wheel-native-stick-selection", () -> {}, () -> true, () -> {
            check("creative-RT-stick-selected-tab", (Integer) field(field(mc.gui.screen(), "contextual"), "creativeIndex") == creativeNavigationTabIndex);
            check("creative-RT-freezes-underlying-cursor", Controlify.instance().virtualMouseHandler().getCurrentX(1f) == creativeNavigationCursorX
                && Controlify.instance().virtualMouseHandler().getCurrentY(1f) == creativeNavigationCursorY);
            check("creative-RT-active-highlight-distinct-from-hover", QaFeatureProbe.activeCreativeTabCount == 1
                && QaFeatureProbe.hoveredCreativeTabCount == 1 && QaFeatureProbe.hoveredActiveCreativeTabCount == 0
                && owner.desktop().contextualMenu(owner.model().playerInventory()).tabs().stream()
                    .anyMatch(tab -> tab.id().equals(creativeNavigationNativeActiveBefore) && tab.selected()));
            driver.rightX = 0; driver.rightY = 0; driver.stick(0, 0);
        }, 3);
        observeNativeGuide("creative-tabs", "CREATIVE_TABS", "Close all windows", "Release: return to pair", "Select creative tab",
            "Choose creative tab", "Previous / next creative tab");
        add("native-guide-creative-both-stick-glyphs", () -> {}, () -> true, () ->
            check("native-guide-creative-tab-both-sticks-glyphs", QaNativeGuideProbe.workspace.glyph("Choose creative tab",
                ((dev.isxander.controlify.api.bind.InputBindingSupplier) field(com.salts_inventory_update.controlify.WorkspaceActionGuide.class,
                    "RIGHT_STICK")).on(controller).inputGlyph())), 2);
        button("creative-RT-wheel-DPAD-next-tab", GamepadInputs.DPAD_RIGHT_BUTTON, () -> {
            var content = owner.desktop().contextualMenu(owner.model().playerInventory());
            creativeNavigationTabIndex = (creativeNavigationTabIndex + 1) % content.tabs().size();
            creativeNavigationTabId = content.tabs().get(creativeNavigationTabIndex).id();
            check("creative-RT-DPAD-selected-tab", (Integer) field(field(mc.gui.screen(), "contextual"), "creativeIndex") == creativeNavigationTabIndex);
        });
        button("creative-RT-wheel-A-select-tab", GamepadInputs.SOUTH_BUTTON, () -> {
            check("creative-RT-native-A-selected-real-tab", owner.desktop().contextualMenu(owner.model().playerInventory()).tabs().stream()
                .anyMatch(tab -> tab.id().equals(creativeNavigationTabId) && tab.selected()));
            check("creative-RT-active-highlight-follows-native-selection", QaFeatureProbe.activeCreativeTabCount == 1
                && QaFeatureProbe.hoveredActiveCreativeTabCount == 1);
            check("creative-RT-no-tab-selected-status", field(field(mc.gui.screen(), "contextual"), "status").equals(""));
            capture("30-creative-tab-radial");
        });
        add("workspace-ui-active-tab-sound-snapshot",() -> nativeUiSoundsBefore=QaVisualFeatureProbe.acceptedSoundCount(),() -> true,() -> {},2);
        button("workspace-ui-native-already-active-tab-confirm",GamepadInputs.SOUTH_BUTTON,() ->
            check("workspace-ui-already-active-tab-is-quiet",QaVisualFeatureProbe.acceptedSoundCount()==nativeUiSoundsBefore
                && QaFeatureProbe.activeCreativeTabCount==1));
        add("creative-RT-release-returns-catalog", () -> driver.rightTrigger = 0,
            () -> !((RadialInventoryScreen) mc.gui.screen()).contextualRadialActive(), () -> check("creative-RT-release-retains-side-workspace", nativePointerReady()), 4);
        add("workspace-ui-actual-native-sounds-bounded",() -> {},() -> true,() -> {
            var sounds = QaVisualFeatureProbe.sounds();
            var kinds = sounds.stream().map(row -> (String)row.get("action")).collect(java.util.stream.Collectors.toSet());
            check("workspace-ui-appropriate-native-actions-have-subtle-sounds",kinds.containsAll(Set.of("OPEN","CLOSE","CYCLE","TUCK","RESTORE",
                "NAVIGATE","SCROLL","CAROUSEL","CATEGORY","FILTER","CONFIRM","TAB","SORT","REJECT")));
            long previous = Long.MIN_VALUE;
            var previousKinds = new LinkedHashMap<String,Long>();
            boolean rateBounded = true;
            for (var row : sounds) {
                long at = ((Number)row.get("atNanos")).longValue(); String kind = (String)row.get("action");
                if (previous!=Long.MIN_VALUE) rateBounded &= at-previous>=43_000_000L;
                Long prior = previousKinds.put(kind,at);
                if (prior!=null) rateBounded &= at-prior >= (com.salts_inventory_update.controlify.WorkspaceUiSounds.Action.valueOf(kind).intervalMillis()-2)*1_000_000L;
                previous=at;
            }
            check("workspace-ui-native-sounds-global-and-action-rate-limited",rateBounded && !sounds.isEmpty());
            check("workspace-ui-native-sound-source-volume-pitch-constrained",QaVisualFeatureProbe.violations().isEmpty());
            check("compact-native-guides-all-modes-with-plain-stick-glyphs",QaNativeGuideProbe.compactGuideFrames>0
                && QaNativeGuideProbe.compactViolations.isEmpty());
            var elements=com.salts_inventory_update.controlify.render.ContextualRadialAssets.Element.values();
            check("contextual-every-custom-shape-is-native-decoded-PNG",Arrays.stream(elements).allMatch(element ->
                QaContextualPresentationProbe.blits().getOrDefault(element.name(),0L)>0
                    && QaContextualPresentationProbe.textures().containsKey(com.salts_inventory_update.controlify.render.ContextualRadialAssets.textureId(element).toString())));
            check("contextual-card-tab-category-letter-texture-states-rendered",Set.of("RECIPE_CARD","UNAVAILABLE_RECIPE_CARD","HOVERED_CARD",
                "CATEGORY","SELECTED_CATEGORY","CREATIVE_TAB","ACTIVE_CREATIVE_TAB","HOVERED_CREATIVE_TAB","ALPHABET_LABEL","HIGHLIGHTED_ALPHABET_LABEL")
                .stream().allMatch(element -> QaContextualPresentationProbe.blits().getOrDefault(element,0L)>0));
            check("contextual-textured-ring-highlight-fixed-UV-sector-tracks-selection",QaContextualPresentationProbe.ringAlignedFrames>0
                && QaContextualPresentationProbe.ringSectorChanges>0 && QaContextualPresentationProbe.ringRegionBlits>0
                && QaContextualPresentationProbe.ringCoverageFrames>0 && QaContextualPresentationProbe.violations().isEmpty());
            check("contextual-overlay-no-procedural-shape-fallback",QaContextualPresentationProbe.controlCalls>0
                && QaContextualPresentationProbe.proceduralViolations().isEmpty());
            check("contextual-recipe-cards-native-layer-below-opening",QaContextualPresentationProbe.recipeGateFrames>0
                && QaContextualPresentationProbe.recipeGateCardDraws>0 && QaContextualPresentationProbe.recipeGateStratumFrames>0
                && QaContextualPresentationProbe.violations().isEmpty());
            check("contextual-navigation-native-layer-above-opening",QaContextualPresentationProbe.recipeNavigationStratumFrames>0
                && QaContextualPresentationProbe.recipeNavigationDrawsTotal>0 && QaContextualPresentationProbe.violations().isEmpty());
            check("contextual-world-dimming-preserved-through-all-wheel-modes",QaContextualPresentationProbe.backdropFrames>0
                && QaContextualPresentationProbe.violations().isEmpty());
            check("contextual-source-and-destination-native-pair-rendered",QaContextualPresentationProbe.liveDestinationFrames==QaContextualPresentationProbe.liveFrames
                && QaContextualPresentationProbe.nativeDestinationCalls==QaContextualPresentationProbe.liveDestinationFrames
                && QaContextualPresentationProbe.nativeDestinationSlots>0 && QaContextualPresentationProbe.nativeDestinationItems>0);
            check("contextual-live-source-transitions-and-native-draws-valid",QaContextualPresentationProbe.openingFrames>0
                && QaContextualPresentationProbe.settledFrames>0 && QaContextualPresentationProbe.closingFrames>0
                && QaContextualPresentationProbe.nativeSlots>0 && QaContextualPresentationProbe.nativeItems>0
                && QaContextualPresentationProbe.violations().isEmpty());
        },2);
    }

    private static void nativeBrewingDpad(String name, int sourceSlot, int targetSlot, Runnable verify) {
        add(name + "-native-cursor-ready", () -> {
            driver.buttons.remove(GamepadInputs.DPAD_RIGHT_BUTTON);
            driver.stick(0, 0);
            creativeNavigationNeutralBefore = QaFeatureProbe.navigationNeutralSamples;
            Controlify.instance().virtualMouseHandler().enableVirtualMouse();
        }, () -> {
            var mouse = Controlify.instance().virtualMouseHandler();
            if (!mouse.isVirtualMouseEnabled()) {
                mouse.enableVirtualMouse();
                creativeNavigationNeutralBefore = QaFeatureProbe.navigationNeutralSamples;
                return false;
            }
            return nativePointerReady() && QaFeatureProbe.navigationNeutralSamples > creativeNavigationNeutralBefore;
        }, () -> {
            // Set up a real source slot only after the enabled cursor has consumed a neutral sample.
            // The press itself continues through Controlify's genuine D-pad binding and repeat handler.
            pointAtNativeSlot(creativeNavigationBrewingWindow, slot -> slot.index == sourceSlot);
            var target = owner.desktop().visibleSlots(creativeNavigationBrewingWindow).stream()
                .filter(slot -> slot.menuSlotId() == targetSlot).findFirst().orElseThrow();
            setDpadExpected(creativeNavigationBrewingWindow, new DesktopWindowBounds(target.x(), target.y(), 16, 16));
            check(name + "-neutral-enabled-native-cursor", nativePointerReady());
        }, 0);
        add(name + "-press", () -> {
            creativeNavigationDispatchBefore = QaFeatureProbe.navigationRequests;
            driver.buttons.add(GamepadInputs.DPAD_RIGHT_BUTTON);
        }, () -> QaFeatureProbe.navigationRequests > creativeNavigationDispatchBefore,
            () -> driver.buttons.remove(GamepadInputs.DPAD_RIGHT_BUTTON), 0);
        add(name, () -> driver.buttons.remove(GamepadInputs.DPAD_RIGHT_BUTTON), () -> true, verify, 3);
    }

    private static void creativeNavigationObserveClient(String phase) {
        var screen = mc.gui.screen();
        int expectedCount = 0;
        for (int i = 0; i < mc.player.getInventory().getContainerSize(); i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (stack.is(creativeNavigationItem)) expectedCount += stack.getCount();
        }
        String hotbar = java.util.stream.IntStream.range(0, 9).mapToObj(i -> i + ":" + mc.player.getInventory().getItem(i))
            .collect(java.util.stream.Collectors.joining(";"));
        record("creative-client-observation-" + phase, "OBSERVED", "expectedItem=" + creativeNavigationItem
            + ",expectedItemCount=" + expectedCount + ",creative=" + mc.player.isCreative() + ",hotbar=" + hotbar
            + ",nativeCarried=" + mc.player.inventoryMenu.getCarried()
            + ",coreSharedCarried=" + field(InventoryDesktopScreen.current(mc), "sharedCarried")
            + ",hoveredWindow=" + field(screen, "hoveredWindow")
            + ",nativePointer=" + field(screen, "nativeMouseX") + "," + field(screen, "nativeMouseY")
            + ",nativeHoveredSlot=" + ((RadialInventoryScreen) screen).controllerHoveredSlot()
            + ",cursorPhysical=" + Controlify.instance().virtualMouseHandler().getCurrentX(1f) + "," + Controlify.instance().virtualMouseHandler().getCurrentY(1f)
            + ",pointerFixturePending=" + (pointerFixture != null)
            + ",primaryMouseConstant=" + com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT);
    }

    private static Object creativeNavigationWindow() {
        List<?> windows = (List<?>) field(InventoryDesktopScreen.current(mc), "windows");
        return windows.stream().filter(window -> field(window, "apiInstanceId").equals(owner.model().playerInventory())).findFirst().orElseThrow();
    }

    private static void creativeNavigationPointCell(int row, int column) {
        var target = owner.desktop().controllerTargets(owner.model().playerInventory()).stream()
            .filter(t -> t.group().equals("creative-catalog") && t.row() == row && t.column() == column).findFirst().orElseThrow();
        pointAtControl(owner.model().playerInventory(), new DesktopWindowBounds(target.x(), target.y(), target.width(), target.height()));
    }

    private static void add(String name, Runnable start, BooleanSupplier ready, Runnable verify, int settleTicks) {
        steps.addLast(new Step(name, start, ready, verify, settleTicks));
    }
    private static void tick(Minecraft minecraft) {
        if (finished) return; mc = minecraft;
        if (active == null && mc.player == null && mc.gui.overlay() != null) return;
        try {
            if (captureFailure != null) throw new IllegalStateException("Deferred screenshot failed", captureFailure);
            if (System.currentTimeMillis() - started > 8 * 60_000) throw new IllegalStateException("QA eight-minute deadline");
            if (active == null) {
                if (steps.isEmpty()) { if (pendingCaptures.get() == 0) finish(); return; }
                active = steps.removeFirst(); stepStarted = System.currentTimeMillis(); settle = -1;
                QaFeatureProbe.scenario = active.name();
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
        pendingCaptures.incrementAndGet();
        Screenshot.takeScreenshot(mc.gameRenderer.mainRenderTarget(), image -> mc.execute(() -> {
            try (image) {
                Path path = output.resolve("screenshots").resolve(label + ".png");
                image.writeToFile(path);
                record(label, "CAPTURED", path.toString());
            } catch (Exception failure) { captureFailure = failure; }
            finally { pendingCaptures.decrementAndGet(); }
        }));
    }
    private static void check(String name, boolean condition) {
        if (!condition) throw new IllegalStateException("Assertion failed: " + name + "; " + controllerState());
        record(name, "PASS", "assertion true");
    }
    private static String controllerState() {
        return "screen=" + (mc.gui.screen() == null ? "gameplay" : mc.gui.screen().getClass().getSimpleName())
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
        if (controller != null) {
            mc.options.getSoundSourceOptionInstance(net.minecraft.sounds.SoundSource.MASTER).set(nativeUiMasterVolumeBefore);
            mc.options.getSoundSourceOptionInstance(net.minecraft.sounds.SoundSource.UI).set(nativeUiVolumeBefore);
        }
        long failures = results.stream().filter(result -> result.get("status").equals("FAIL")).count();
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("scenario", "controlify-controller-world"); report.put("failures", failures); report.put("results", results);
        report.put("elapsedMs", System.currentTimeMillis() - started);
        report.put("coreCodeSource", InventoryDesktopScreen.class.getProtectionDomain().getCodeSource().getLocation().toString());
        report.put("companionCodeSource", SaltsControlifyClient.class.getProtectionDomain().getCodeSource().getLocation().toString());
        report.put("controlifyCodeSource", Controlify.class.getProtectionDomain().getCodeSource().getLocation().toString());
        report.put("loadedMods", QaRuntime.loadedMods());
        report.put("queuedOpacityProbe", WorkspaceOpacityProbe.snapshot());
        report.put("feature26Probe", QaFeatureProbe.snapshot());
        report.put("javaRuntime", Map.of("version", System.getProperty("java.runtime.version"),
                "vendor", System.getProperty("java.vendor"), "home", System.getProperty("java.home"),
                "vmArguments", java.lang.management.ManagementFactory.getRuntimeMXBean().getInputArguments()));
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
    private static void setField(Object target,String name,Object value) {
        Class<?> type=target instanceof Class<?> clazz?clazz:target.getClass();
        for(Class<?> current=type;current!=null;current=current.getSuperclass()) {
            try {Field field=current.getDeclaredField(name);field.setAccessible(true);field.set(target instanceof Class<?>?null:target,value);return;}
            catch(NoSuchFieldException ignored) {} catch(Exception failure) {throw new IllegalStateException(failure);}
        }
        throw new IllegalArgumentException(type+"."+name);
    }
}
