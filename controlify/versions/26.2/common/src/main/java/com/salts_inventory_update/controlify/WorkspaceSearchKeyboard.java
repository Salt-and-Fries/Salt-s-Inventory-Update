package com.salts_inventory_update.controlify;

import com.mojang.blaze3d.platform.InputConstants;
import com.salts_inventory_update.api.client.desktop.DesktopWindowBounds;
import com.salts_inventory_update.api.client.desktop.DesktopWorkspace;
import dev.isxander.controlify.Controlify;
import dev.isxander.controlify.InputMode;
import dev.isxander.controlify.api.ControlifyApi;
import dev.isxander.controlify.controller.ControllerEntity;
import dev.isxander.controlify.controller.input.ControllerStateView;
import dev.isxander.controlify.controller.input.HatState;
import dev.isxander.controlify.screenop.ScreenProcessor;
import dev.isxander.controlify.screenop.ScreenProcessorProvider;
import dev.isxander.controlify.screenop.keyboard.InputTarget;
import dev.isxander.controlify.screenop.keyboard.KeyboardLayouts;
import dev.isxander.controlify.screenop.keyboard.KeyboardOverlayScreen;
import dev.isxander.controlify.screenop.keyboard.KeyboardWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Controlify's native keyboard with strict ownership of its underlying workspace. */
public final class WorkspaceSearchKeyboard extends KeyboardOverlayScreen implements ScreenProcessorProvider {
    private static WorkspaceSearchKeyboard owner;
    private static boolean exitAwaitingNeutral;

    private final RadialInventoryScreen parent;
    private final DesktopWorkspace desktop;
    private final UUID window;
    private final ControllerEntity openingController;
    private final InputMode returnInputMode;
    private final Map<Integer, KeyEvent> heldKeys = new HashMap<>();
    private final Set<Integer> heldMouseButtons = new HashSet<>();
    private final Map<Integer, MouseButtonInfo> syntheticMouseButtons = new HashMap<>();
    private ScreenProcessor<WorkspaceSearchKeyboard> processor;
    private boolean openingAwaitingNeutral = true;
    private boolean controllerDispatch;
    private boolean closed;
    private char pendingHighSurrogate;

    private WorkspaceSearchKeyboard(RadialInventoryScreen parent, DesktopWorkspace desktop, UUID window, Target target) {
        super(parent, KeyboardLayouts.simple(), target, WorkspaceSearchKeyboard::positionKeyboard);
        this.parent = parent;
        this.desktop = desktop;
        this.window = window;
        target.keyboard = this;
        this.openingController = ControlifyApi.get().getCurrentController().orElse(null);
        InputMode mode = ControlifyApi.get().currentInputMode();
        this.returnInputMode = mode.isController() ? mode
                : Controlify.instance().config().getSettings().globalSettings().mixedInput ? InputMode.MIXED : InputMode.CONTROLLER;
    }

    /** Bounds are Core's native search-field coordinates, before workspace presentation transforms. */
    public static boolean open(RadialInventoryScreen parent, DesktopWorkspace desktop, UUID window, DesktopWindowBounds searchBounds) {
        Minecraft minecraft = Minecraft.getInstance();
        if (parent == null || desktop == null || window == null || searchBounds == null
                || searchBounds.height() != 12 || searchBounds.width() < 24
                || minecraft.gui.screen() != parent || active() || waitingNeutral() || !desktop.isValid()) return false;
        CreativeSearchTarget search = new CreativeSearchTarget(window, searchBounds);
        if (search.current(desktop) == null) return false;
        return install(parent, desktop, window, () -> search.focus(desktop));
    }

    /** Reopens the already-selected API editor after the overlay changes the native screen. */
    public static boolean openFocused(RadialInventoryScreen parent, DesktopWorkspace desktop, UUID window, double x, double y) {
        if (parent == null || desktop == null || window == null || !Double.isFinite(x) || !Double.isFinite(y)
                || Minecraft.getInstance().gui.screen() != parent || active() || waitingNeutral()
                || !desktop.isValid() || !desktop.textInputActive(window)) return false;
        return install(parent, desktop, window, () -> {
            try {
                desktop.pointer(window, DesktopWorkspace.PointerAction.PRESS, x, y, org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT, 0, 0);
                return desktop.textInputActive(window);
            } finally {
                desktop.pointer(window, DesktopWorkspace.PointerAction.RELEASE, x, y, org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT, 0, 0);
            }
        });
    }

    private static boolean install(RadialInventoryScreen parent, DesktopWorkspace desktop, UUID window,
                                   java.util.function.BooleanSupplier focus) {
        Minecraft minecraft = Minecraft.getInstance();
        WorkspaceSearchKeyboard keyboard = new WorkspaceSearchKeyboard(parent, desktop, window, new Target());
        owner = keyboard;
        minecraft.gui.setScreen(keyboard);
        // Screen removal may cancel Core's editor focus. Focus the real field only after installing
        // the overlay; all subsequent editing goes through Core's existing public text handlers.
        boolean focused = false;
        try {
            focused = focus.getAsBoolean();
            return focused && minecraft.gui.screen() == keyboard;
        } finally {
            if (!focused && minecraft.gui.screen() == keyboard) keyboard.onClose();
        }
    }

    public static boolean active() {
        return owner != null && !owner.closed && Minecraft.getInstance().gui.screen() == owner;
    }

    public static boolean activeFor(Screen parent) {
        return active() && owner.parent == parent;
    }

    /** Drops only this parent's retained modal input state during workspace teardown. */
    public static void resetFor(Screen parent) {
        WorkspaceSearchKeyboard keyboard = owner;
        if (keyboard == null || keyboard.parent != parent) return;
        keyboard.closed = true;
        try {
            keyboard.releaseTargetKeys();
            keyboard.releaseSyntheticMouseButtons();
        }
        finally {
            keyboard.heldKeys.clear();
            keyboard.heldMouseButtons.clear();
            keyboard.syntheticMouseButtons.clear();
            owner = null;
            exitAwaitingNeutral = false;
        }
    }

    /** Read-only: held physical keyboard/mouse inputs outlive the controller's neutral frame. */
    public static boolean waitingNeutral() {
        return exitAwaitingNeutral || owner != null && owner.closed
                && (!owner.heldKeys.isEmpty() || !owner.heldMouseButtons.isEmpty());
    }

    /** Consume the release sample as well, so native justReleased actions cannot leak after close. */
    public static boolean suppressWorkspaceInput(ControllerStateView current, float threshold) {
        if (active()) return true;
        if (exitAwaitingNeutral) {
            if (neutral(current, threshold)) exitAwaitingNeutral = false;
            return true;
        }
        return waitingNeutral();
    }

    public static boolean suppressWorkspaceKey(KeyEvent event, boolean release) {
        if (active()) return true;
        if (owner == null) return false;
        boolean consumed = waitingNeutral() || owner.heldKeys.containsKey(event.key());
        if (release) owner.heldKeys.remove(event.key());
        else if (consumed && !owner.controllerDispatch) owner.heldKeys.put(event.key(), event);
        return consumed;
    }

    public static boolean suppressWorkspaceMouse(MouseButtonEvent event, boolean release) {
        if (active()) return true;
        if (owner == null) return false;
        boolean consumed = waitingNeutral() || owner.heldMouseButtons.contains(event.button());
        if (release) {
            owner.syntheticMouseButtons.remove(event.button());
            if (!owner.controllerDispatch) owner.heldMouseButtons.remove(event.button());
        } else if (consumed) owner.trackMousePress(event);
        return consumed;
    }

    @Override public ScreenProcessor<WorkspaceSearchKeyboard> screenProcessor() {
        if (processor == null) processor = new ScreenProcessor<>(this) {
            @Override public boolean tryOpenKeyboard(ControllerEntity controller,
                    net.minecraft.client.gui.components.events.GuiEventListener component) {
                return false; // Refocusing a hosted EditBox must not install a second overlay.
            }
            @Override public void onControllerUpdate(ControllerEntity controller) {
                if (!ownsTarget()) return;
                var input = controller.input().orElseThrow();
                if (openingAwaitingNeutral) {
                    if (neutral(input.stateNow(), input.settings().buttonActivationThreshold)
                            && neutral(input.stateThen(), input.settings().buttonActivationThreshold))
                        openingAwaitingNeutral = false;
                    return;
                }
                controllerDispatch = true;
                try { super.onControllerUpdate(controller); }
                finally {
                    try {
                        if (closed) releaseSyntheticMouseButtons();
                    } finally { controllerDispatch = false; }
                }
            }
        };
        return processor;
    }

    private static boolean neutral(ControllerStateView state, float threshold) {
        if (state == null) return true;
        for (var button : state.getButtons()) if (state.isButtonDown(button)) return false;
        // These states already include the configured stick deadzones. Require resting axes,
        // rather than merely falling below a digital button's activation threshold.
        float restingTolerance = Math.min(0.01f, Math.max(0.0001f, threshold));
        for (var axis : state.getAxes())
            if (Math.abs(state.getAxisState(axis) - state.getAxisResting(axis)) > restingTolerance) return false;
        for (var hat : state.getHats()) if (state.getHatState(hat) != HatState.CENTERED) return false;
        return true;
    }

    /** Native positioning owns keyboard sizing; use the lower viewport independently of Core coordinates. */
    private static ScreenRectangle positionKeyboard(int width, int height) {
        int padding = Math.max(0, Math.min(8, height / 40));
        return KeyboardOverlayScreen.aboveOrBelowWidgetPositioner(
                Math.max(1, width * 9 / 10), Math.max(1, height * 2 / 5), padding,
                () -> new ScreenRectangle(0, height, width, 0)).positionKeyboard(width, height);
    }

    private KeyboardWidget nativeKeyboard() {
        return children().stream().filter(KeyboardWidget.class::isInstance)
                .map(KeyboardWidget.class::cast).findFirst().orElse(null);
    }

    @Override public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        trackMousePress(event);
        KeyboardWidget keyboard = nativeKeyboard();
        // Calling KeyboardOverlayScreen.mouseClicked would forward misses to the inventory.
        if (!closed && !openingAwaitingNeutral && keyboard != null && keyboard.isMouseOver(event.x(), event.y())) {
            setFocused(keyboard);
            keyboard.mouseClicked(event, doubleClick);
        }
        return true;
    }

    @Override public boolean mouseReleased(MouseButtonEvent event) {
        syntheticMouseButtons.remove(event.button());
        if (!controllerDispatch) heldMouseButtons.remove(event.button());
        KeyboardWidget keyboard = nativeKeyboard();
        if (!closed && keyboard != null) keyboard.mouseReleased(event);
        return true;
    }

    @Override public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
        KeyboardWidget keyboard = nativeKeyboard();
        if (!closed && !openingAwaitingNeutral && keyboard != null) keyboard.mouseDragged(event, deltaX, deltaY);
        return true;
    }

    @Override public boolean mouseScrolled(double x, double y, double deltaX, double deltaY) {
        KeyboardWidget keyboard = nativeKeyboard();
        if (!closed && !openingAwaitingNeutral && keyboard != null && keyboard.isMouseOver(x, y))
            keyboard.mouseScrolled(x, y, deltaX, deltaY);
        return true;
    }

    @Override public void mouseMoved(double x, double y) {
        KeyboardWidget keyboard = nativeKeyboard();
        if (!closed && keyboard != null) keyboard.mouseMoved(x, y);
    }

    @Override public boolean keyPressed(KeyEvent event) {
        if (!controllerDispatch) heldKeys.put(event.key(), event);
        if (!closed && !openingAwaitingNeutral) sendKey(event.key(), event.scancode(), event.modifiers());
        return true;
    }

    @Override public boolean keyReleased(KeyEvent event) {
        heldKeys.remove(event.key());
        if (ownsTarget()) desktop.keyReleased(event.key(), event.scancode(), event.modifiers());
        return true;
    }

    @Override public boolean charTyped(CharacterEvent event) {
        if (ownsTarget() && !openingAwaitingNeutral) desktop.charTyped(event.codepoint(), 0);
        return true;
    }

    private boolean ownsTarget() {
        return !closed && Minecraft.getInstance().gui.screen() == this && desktop.isValid()
                && desktop.windows().stream().anyMatch(candidate -> candidate.handle().instanceId().equals(window)
                && candidate.handle().isOpen());
    }

    private boolean sendKey(int key, int keycode, int modifiers) {
        if (!ownsTarget() || openingAwaitingNeutral) return true;
        if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER || key == org.lwjgl.glfw.GLFW.GLFW_KEY_KP_ENTER || key == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }
        desktop.keyPressed(key, keycode, modifiers);
        return true;
    }

    @Override public void onClose() {
        if (closed || Minecraft.getInstance().gui.screen() != this) return;
        closed = true;
        exitAwaitingNeutral = true;
        releaseTargetKeys();
        var current = ControlifyApi.get().getCurrentController().orElse(null);
        if (current != null && current == openingController && !ControlifyApi.get().currentInputMode().isController())
            ControlifyApi.get().setInputMode(returnInputMode);
        // Native forceSetScreen restores the exact parent without init()/removed().
        super.onClose();
        // A physical Escape/Enter can close between controller updates, when there is no
        // controller-dispatch finally block left to balance an earlier virtual click.
        if (!controllerDispatch) {
            controllerDispatch = true;
            try { releaseSyntheticMouseButtons(); }
            finally { controllerDispatch = false; }
        }
    }

    @Override public void removed() {
        closed = true;
        // A replacement menu/world receives future releases. Retaining this screen's held
        // physical inputs would permanently quarantine an unrelated later workspace.
        try {
            releaseTargetKeys();
            releaseSyntheticMouseButtons();
        }
        finally {
            heldKeys.clear();
            heldMouseButtons.clear();
            syntheticMouseButtons.clear();
            if (owner == this) {
                exitAwaitingNeutral = false;
                owner = null;
            }
        }
    }

    private void releaseTargetKeys() {
        pendingHighSurrogate = 0;
        if (desktop.isValid()) for (KeyEvent event : heldKeys.values())
            desktop.keyReleased(event.key(), event.scancode(), event.modifiers());
    }

    private void trackMousePress(MouseButtonEvent event) {
        if (controllerDispatch) {
            if (!heldMouseButtons.contains(event.button())) syntheticMouseButtons.put(event.button(), event.buttonInfo());
        } else {
            heldMouseButtons.add(event.button());
            syntheticMouseButtons.remove(event.button());
        }
    }

    /** Balance only owned virtual presses, after the complete native callback sequence. */
    private void releaseSyntheticMouseButtons() {
        Minecraft minecraft = Minecraft.getInstance();
        boolean wasControllerDispatch = controllerDispatch;
        controllerDispatch = true;
        try {
            for (MouseButtonInfo button : Map.copyOf(syntheticMouseButtons).values()) {
                Screen current = minecraft.gui.screen();
                if (current != this && !(current == parent && closed && exitAwaitingNeutral)) break;
                syntheticMouseButtons.remove(button.button());
                if (!heldMouseButtons.contains(button.button()))
                    ((com.salts_inventory_update.controlify.mixin.WorkspaceMouseHandlerAccessor) minecraft.mouseHandler).saltsControlify$onButton(minecraft.getWindow().handle(), button, org.lwjgl.glfw.GLFW.GLFW_RELEASE);
            }
        } finally {
            syntheticMouseButtons.clear();
            controllerDispatch = wasControllerDispatch;
        }
    }

    private static final class Target implements InputTarget {
        private WorkspaceSearchKeyboard keyboard;

        @Override public boolean supportsCharInput() { return true; }
        @Override public boolean supportsKeyCodeInput() { return true; }
        @Override public boolean supportsCursorMovement() { return true; }

        @Override public boolean acceptChar(char character, int modifiers) {
            if (!keyboard.ownsTarget() || keyboard.openingAwaitingNeutral) return true;
            if (Character.isHighSurrogate(character)) {
                keyboard.pendingHighSurrogate = character;
                return true;
            }
            int codepoint = character;
            if (Character.isLowSurrogate(character)) {
                if (keyboard.pendingHighSurrogate == 0) return true;
                codepoint = Character.toCodePoint(keyboard.pendingHighSurrogate, character);
            }
            keyboard.pendingHighSurrogate = 0;
            keyboard.desktop.charTyped(codepoint, modifiers);
            return true;
        }

        @Override public boolean acceptKeyCode(int key, int keycode, int modifiers) {
            if (!keyboard.ownsTarget() || keyboard.openingAwaitingNeutral) return true;
            keyboard.sendKey(key, keycode, modifiers);
            if (!keyboard.closed && keyboard.desktop.isValid()) keyboard.desktop.keyReleased(key, keycode, modifiers);
            return true;
        }

        @Override public boolean moveCursor(int amount) {
            if (!keyboard.ownsTarget() || keyboard.openingAwaitingNeutral) return true;
            int key = amount < 0 ? org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT : org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT;
            for (long remaining = Math.abs((long) amount); remaining > 0; remaining--) acceptKeyCode(key, 0, 0);
            return true;
        }
    }
}
