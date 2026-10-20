package com.salts_inventory_update.controlify.input;

import dev.isxander.controlify.api.ControlifyApi;
import dev.isxander.controlify.Controlify;
import dev.isxander.controlify.InputMode;
import dev.isxander.controlify.api.bind.InputBinding;
import dev.isxander.controlify.api.event.ControlifyEvents;
import dev.isxander.controlify.bindings.ControlifyBindings;
import dev.isxander.controlify.controller.ControllerEntity;
import dev.isxander.controlify.controller.input.ControllerStateView;
import dev.isxander.controlify.controller.input.GamepadInputs;
import dev.isxander.controlify.controller.input.InputComponent;
import dev.isxander.controlify.screenop.ScreenProcessorProvider;
import dev.isxander.controlify.screenop.ScreenProcessor;
import dev.isxander.controlify.screenop.compat.vanilla.AbstractContainerScreenProcessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import com.salts_inventory_update.controlify.model.RadialStickDirection;
import com.salts_inventory_update.controlify.ContextualRecipeBinding;
import com.salts_inventory_update.controlify.WorkspaceUiSounds;
import com.salts_inventory_update.controlify.CreativeSearchBinding;
import com.salts_inventory_update.controlify.WorkspaceSearchKeyboard;

import java.util.Objects;

/** Typed adapter for the published Controlify 3.5.3 API. */
public final class ControlifyInputBridge {
    private static RadialWorkspaceActions actions;
    private static RadialSortInputRouter router;
    private static String lastControllerUid;
    private static final com.salts_inventory_update.controlify.model.TriggerToggle windowTrigger = new com.salts_inventory_update.controlify.model.TriggerToggle();
    private static final com.salts_inventory_update.controlify.model.TriggerToggle recipeTrigger = new com.salts_inventory_update.controlify.model.TriggerToggle();

    private ControlifyInputBridge() {
    }

    public static <M extends AbstractContainerMenu, T extends AbstractContainerScreen<M> & WorkspaceContainerScreen> void initialize(
            RadialWorkspaceActions workspace, Class<T> screenClass) {
        if (actions != null) {
            throw new IllegalStateException("Controlify companion inputs already initialized");
        }
        actions = Objects.requireNonNull(workspace, "workspace");
        ContextualRecipeBinding.initialize();
        CreativeSearchBinding.initialize();
        router = new RadialSortInputRouter(workspace);
        ScreenProcessorProvider.registerProvider(screenClass, screen -> new WorkspaceScreenProcessor<>(screen));

        // Runs after bindings receive the new state but BEFORE Controlify's gameplay handler.
        // Restoring here prevents that handler from constructing a second InventoryScreen.
        ControlifyEvents.CONTROLLER_STATE_UPDATE.register(event -> restoreBeforeGameplayInput(event.controller()));
        ControlifyEvents.CONTROLLER_DISCONNECTED.register(event -> {
            if (event.controller().uid().equals(lastControllerUid)) {
                reset();
            }
        });
        ControlifyEvents.INPUT_MODE_CHANGED.register(event -> {
            if (!event.mode().isController()) {
                reset();
            }
        });
    }

    /** Container mixins override the registry provider, so the workspace supplies this explicitly. */
    public static <M extends AbstractContainerMenu, T extends AbstractContainerScreen<M> & WorkspaceContainerScreen>
    ScreenProcessor<T> createScreenProcessor(T screen) {
        return new WorkspaceScreenProcessor<>(screen);
    }

    public static void hoveredSlotChanged(AbstractContainerScreen<?> screen, Slot current, Slot previous) {
        if (current != previous && ScreenProcessorProvider.provide(screen) instanceof AbstractContainerScreenProcessor<?> processor) {
            processor.onHoveredSlotChanged(current, previous);
            if (current != null && screen instanceof WorkspaceContainerScreen && usingController())
                WorkspaceUiSounds.play(WorkspaceUiSounds.Action.HOVER);
        }
    }

    public static boolean usingController() {
        return ControlifyApi.get().currentInputMode().isController()
                && ControlifyApi.get().getCurrentController().isPresent();
    }

    static boolean update(ControllerEntity controller) {
        if (!controller.uid().equals(lastControllerUid)) {
            router.reset();
            cancelWheelToggles();
            lastControllerUid = controller.uid();
        }
        InputComponent input = controller.input().orElseThrow();
        ControllerStateView current = input.stateNow();
        ControllerStateView previous = input.stateThen();
        float threshold = input.settings().buttonActivationThreshold;
        if (WorkspaceSearchKeyboard.suppressWorkspaceInput(current, threshold)) {
            cancelWheelToggles();
            return true;
        }
        InputBinding close = ControlifyBindings.GUI_BACK.on(controller);
        InputBinding inventory = ControlifyBindings.INVENTORY.on(controller);
        var nativeScreen = Minecraft.getInstance().screen;
        var workspaceScreen = nativeScreen instanceof WorkspaceContainerScreen screen ? screen : null;
        boolean contextual = workspaceScreen != null && workspaceScreen.contextualRadialActive();
        boolean leftTriggerHeld = current.getAxisState(GamepadInputs.LEFT_TRIGGER_AXIS) > threshold;
        router.observeOpeningTrigger(leftTriggerHeld);
        InputBinding recipe = ContextualRecipeBinding.OPEN_WHEEL.on(controller);
        boolean openingRecipeBlocked = router.awaitingOpeningTriggerRelease()
                && recipe.boundInput().getRelevantInputs().contains(GamepadInputs.LEFT_TRIGGER_AXIS);
        boolean recipeHeld = recipeTrigger.update(recipe.boundInput().state(current) > threshold,
                actions.toggleRecipeWheel(), !openingRecipeBlocked && workspaceScreen != null
                    && (contextual || !actions.radialActive() && workspaceScreen.controllerCanOpenContextual()));
        boolean windowWheelHeld = windowTrigger.update(leftTriggerHeld, actions.toggleWindowWheel(),
                !recipeHeld && actions.workspaceActive() && !actions.tuckedAway() && !router.awaitingOpeningTriggerRelease());
        if (contextual && !recipeHeld) {
            // Hand the release sample back before workspace routing too, so fresh bumpers/LT
            // and native inventory actions all apply in SIDE mode immediately.
            workspaceScreen.controllerContextualInput(false, 0, 0, 0, 0,
                    false, false, false, false, false, false, false);
            contextual = false;
        }
        boolean recipeOwns = contextual || recipeHeld && workspaceScreen != null && workspaceScreen.controllerCanOpenContextual();
        InputBinding select = ControlifyBindings.INV_SELECT.on(controller);
        InputBinding quickMove = ControlifyBindings.INV_QUICK_MOVE.on(controller);
        boolean craftStackPressed = quickMove.boundInput().state(current) > threshold && quickMove.boundInput().state(previous) <= threshold;
        boolean confirmPressed = select.boundInput().state(current) > threshold && select.boundInput().state(previous) <= threshold;
        boolean closeOwns = close.boundInput().state(current) > threshold || close.boundInput().state(previous) > threshold;
        if (!closeOwns && !contextual && !actions.radialActive() && workspaceScreen != null) {
            InputBinding search = CreativeSearchBinding.SEARCH.on(controller);
            boolean openingSearchBlocked = router.awaitingOpeningTriggerRelease()
                && search.boundInput().getRelevantInputs().contains(GamepadInputs.LEFT_TRIGGER_AXIS);
            boolean searchPressed = !openingSearchBlocked && search.boundInput().state(current) > threshold
                && search.boundInput().state(previous) <= threshold;
            if (searchPressed && workspaceScreen.controllerCreativeSearch(false)) return true;
            if (confirmPressed && workspaceScreen.controllerCreativeSearch(true)) return true;
        }
        boolean consumed = router.update(new ControllerInputFrame(
                !recipeOwns && windowWheelHeld,
                current.getAxisState(GamepadInputs.LEFT_STICK_AXIS_RIGHT) - current.getAxisState(GamepadInputs.LEFT_STICK_AXIS_LEFT),
                current.getAxisState(GamepadInputs.LEFT_STICK_AXIS_DOWN) - current.getAxisState(GamepadInputs.LEFT_STICK_AXIS_UP),
                !recipeOwns && justPressed(current, previous, GamepadInputs.LEFT_SHOULDER_BUTTON),
                !recipeOwns && justPressed(current, previous, GamepadInputs.RIGHT_SHOULDER_BUTTON),
                !contextual && confirmPressed,
                close.boundInput().state(current) > threshold,
                System.nanoTime() / 1_000_000L
        ), inventory.boundInput().state(current) > threshold,
                inventory.boundInput().state(previous) > threshold);
        if (consumed || workspaceScreen == null || Minecraft.getInstance().screen != nativeScreen) return consumed;
        // Independent axis deadzones distort angles and can make dense wheel sectors unreachable.
        // rawStateNow is already device-mapped; retain its angle with the configured processed magnitude.
        ControllerStateView raw = input.rawStateNow();
        var right = RadialStickDirection.from(
                raw.getAxisState(GamepadInputs.RIGHT_STICK_AXIS_RIGHT) - raw.getAxisState(GamepadInputs.RIGHT_STICK_AXIS_LEFT),
                raw.getAxisState(GamepadInputs.RIGHT_STICK_AXIS_DOWN) - raw.getAxisState(GamepadInputs.RIGHT_STICK_AXIS_UP),
                current.getAxisState(GamepadInputs.RIGHT_STICK_AXIS_RIGHT) - current.getAxisState(GamepadInputs.RIGHT_STICK_AXIS_LEFT),
                current.getAxisState(GamepadInputs.RIGHT_STICK_AXIS_DOWN) - current.getAxisState(GamepadInputs.RIGHT_STICK_AXIS_UP));
        var left = RadialStickDirection.from(
                raw.getAxisState(GamepadInputs.LEFT_STICK_AXIS_RIGHT) - raw.getAxisState(GamepadInputs.LEFT_STICK_AXIS_LEFT),
                raw.getAxisState(GamepadInputs.LEFT_STICK_AXIS_DOWN) - raw.getAxisState(GamepadInputs.LEFT_STICK_AXIS_UP),
                current.getAxisState(GamepadInputs.LEFT_STICK_AXIS_RIGHT) - current.getAxisState(GamepadInputs.LEFT_STICK_AXIS_LEFT),
                current.getAxisState(GamepadInputs.LEFT_STICK_AXIS_DOWN) - current.getAxisState(GamepadInputs.LEFT_STICK_AXIS_UP));
        if (workspaceScreen.controllerContextualInput(
                recipeHeld,
                right.x(), right.y(), left.x(), left.y(),
                justPressed(current, previous, GamepadInputs.DPAD_LEFT_BUTTON),
                justPressed(current, previous, GamepadInputs.DPAD_RIGHT_BUTTON),
                justPressed(current, previous, GamepadInputs.DPAD_UP_BUTTON), confirmPressed,
                current.isButtonDown(GamepadInputs.LEFT_SHOULDER_BUTTON),
                current.isButtonDown(GamepadInputs.RIGHT_SHOULDER_BUTTON), craftStackPressed)) return true;
        // Native Quick Move and cursor-drop can share Y and both run in one native sample.
        // A creative hotbar deletion must own that sample, preserving any carried stack.
        if (craftStackPressed && workspaceScreen.controllerCreativeDeleteHotbar()) return true;
        // Catalog actions use the same configured bindings as their native guide rows.
        // The full-stack action wins when both bindings share an input or fire together.
        if (inventory.boundInput().state(current) > threshold && inventory.boundInput().state(previous) <= threshold
                && workspaceScreen.controllerCreativeHotbar(true)) return true;
        return confirmPressed && workspaceScreen.controllerCreativeHotbar(false);
    }

    public static float closeProgress() {
        return router == null ? 0f : router.closeProgress(System.nanoTime() / 1_000_000L);
    }

    public static boolean awaitingOpeningTriggerRelease() {
        return router != null && router.awaitingOpeningTriggerRelease();
    }

    public static void workspaceOpened() {
        if (router == null) return;
        var controller = ControlifyApi.get().getCurrentController().orElse(null);
        // The first container may be opened by USE before a workspace processor has sampled
        // this controller. Preserve the opening latch across that first processor update.
        if (controller != null && !controller.uid().equals(lastControllerUid)) {
            router.reset();
            cancelWheelToggles();
            lastControllerUid = controller.uid();
        }
        boolean held = controller != null && controller.input().map(input ->
                input.stateNow().getAxisState(GamepadInputs.LEFT_TRIGGER_AXIS) > input.settings().buttonActivationThreshold).orElse(false);
        cancelWheelToggles();
        router.workspaceOpened(held);
    }

    static boolean radialActive() {
        return actions.radialActive();
    }

    /** Hides only the cursor image; its native state/position survives radial selection. */
    public static boolean suppressVirtualMouseRendering() {
        return actions != null && Minecraft.getInstance().screen instanceof WorkspaceContainerScreen screen
                && (actions.radialActive() || screen.contextualRadialActive());
    }

    static boolean closeOwnsButtons(ControllerEntity controller) {
        InputComponent input = controller.input().orElseThrow();
        InputBinding close = ControlifyBindings.GUI_BACK.on(controller);
        float threshold = input.settings().buttonActivationThreshold;
        return close.boundInput().state(input.stateNow()) > threshold
                || close.boundInput().state(input.stateThen()) > threshold;
    }

    private static boolean justPressed(ControllerStateView current, ControllerStateView previous,
                                       net.minecraft.resources.Identifier button) {
        return current.isButtonDown(button) && !previous.isButtonDown(button);
    }

    private static void restoreBeforeGameplayInput(ControllerEntity controller) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.gameMode == null) return;
        if (minecraft.screen != null
                || ControlifyApi.get().getCurrentController().orElse(null) != controller) {
            return;
        }
        InputComponent input = controller.input().orElseThrow();
        InputBinding inventory = ControlifyBindings.INVENTORY.on(controller);
        float threshold = input.settings().buttonActivationThreshold;
        if (inventory.boundInput().state(input.stateNow()) > threshold
                && inventory.boundInput().state(input.stateThen()) <= threshold) {
            if (!ControlifyApi.get().currentInputMode().isController()) {
                ControlifyApi.get().setInputMode(Controlify.instance().config().getSettings().globalSettings().mixedInput
                        ? InputMode.MIXED : InputMode.CONTROLLER);
            }
            actions.restoreWorkspace();
        }
    }

    public static void cancelWheelToggles() {
        // Require a neutral sample after screen/modal changes, even if the processor missed an input.
        windowTrigger.cancel(true);
        recipeTrigger.cancel(true);
    }

    public static void reset() {
        cancelWheelToggles();
        WorkspaceUiSounds.reset();
        if (router != null) {
            router.reset();
        }
        lastControllerUid = null;
        if (actions != null && actions.radialActive()) {
            actions.releaseRadial();
        }
    }
}
