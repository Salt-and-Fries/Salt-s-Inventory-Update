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

import java.util.Objects;

/** Typed adapter for the published Controlify 3.5.3 API. */
public final class ControlifyInputBridge {
    private static ControllerWorkspaceActions actions;
    private static WorkspaceInputRouter router;
    private static String lastControllerUid;

    private ControlifyInputBridge() {
    }

    public static <M extends AbstractContainerMenu, T extends AbstractContainerScreen<M> & WorkspaceContainerScreen> void initialize(
            ControllerWorkspaceActions workspace, Class<T> screenClass) {
        if (actions != null) {
            throw new IllegalStateException("Controlify companion inputs already initialized");
        }
        actions = Objects.requireNonNull(workspace, "workspace");
        router = new WorkspaceInputRouter(workspace);
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
        }
    }

    public static boolean usingController() {
        return ControlifyApi.get().currentInputMode().isController()
                && ControlifyApi.get().getCurrentController().isPresent();
    }

    static boolean update(ControllerEntity controller) {
        if (!controller.uid().equals(lastControllerUid)) {
            router.reset();
            lastControllerUid = controller.uid();
        }
        InputComponent input = controller.input().orElseThrow();
        ControllerStateView current = input.stateNow();
        ControllerStateView previous = input.stateThen();
        float threshold = input.settings().buttonActivationThreshold;
        InputBinding close = ControlifyBindings.GUI_BACK.on(controller);
        boolean consumed = router.update(new ControllerInputFrame(
                current.getAxisState(GamepadInputs.LEFT_TRIGGER_AXIS) > threshold,
                current.getAxisState(GamepadInputs.LEFT_STICK_AXIS_RIGHT) - current.getAxisState(GamepadInputs.LEFT_STICK_AXIS_LEFT),
                current.getAxisState(GamepadInputs.LEFT_STICK_AXIS_DOWN) - current.getAxisState(GamepadInputs.LEFT_STICK_AXIS_UP),
                justPressed(current, previous, GamepadInputs.LEFT_SHOULDER_BUTTON),
                justPressed(current, previous, GamepadInputs.RIGHT_SHOULDER_BUTTON),
                justPressed(current, previous, GamepadInputs.SOUTH_BUTTON),
                close.boundInput().state(current) > threshold,
                System.nanoTime() / 1_000_000L
        ));
        return consumed;
    }

    public static float closeProgress() {
        return router == null ? 0f : router.closeProgress(System.nanoTime() / 1_000_000L);
    }

    static boolean radialActive() {
        return actions.radialActive();
    }

    /** Hides only the cursor image; its native state/position survives radial selection. */
    public static boolean suppressVirtualMouseRendering() {
        return actions != null && actions.radialActive()
                && Minecraft.getInstance().screen instanceof WorkspaceContainerScreen;
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

    public static void reset() {
        if (router != null) {
            router.reset();
        }
        lastControllerUid = null;
        if (actions != null && actions.radialActive()) {
            actions.releaseRadial();
        }
    }
}
