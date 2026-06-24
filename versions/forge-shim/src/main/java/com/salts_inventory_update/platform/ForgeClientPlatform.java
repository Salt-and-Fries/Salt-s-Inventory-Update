package com.salts_inventory_update.platform;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import com.mojang.blaze3d.platform.InputConstants;
import com.salts_inventory_update.SaltsInventoryRuntime;
import com.salts_inventory_update.client.InventoryDesktopScreen;
import com.salts_inventory_update.client.WindowedInventoryClient;
import com.salts_inventory_update.debug.DesktopDebug;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.screens.Screen;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.lwjgl.glfw.GLFW;

public final class ForgeClientPlatform {
    private static final int MOVEMENT_LOG_INTERVAL = 20;

    private static boolean initialized;
    private static boolean movementInputWarningLogged;
    private static Method windowHandleMethod;
    private static int movementInputEvents;
    private static int movementInputPatches;
    private static int movementInputSkips;
    private static String lastMovementInputSummary = "";
    private static String lastMovementInputSkipSummary = "";

    private ForgeClientPlatform() {
    }

    public static void initialize(IEventBus modBus) {
        if (initialized) {
            DesktopDebug.log("Forge client platform already initialized");
            return;
        }
        initialized = true;

        DesktopDebug.log("Forge client platform initializing");
        modBus.addListener(KeyBindingHelper::onRegisterKeyMappings);
        modBus.addListener(ForgeClientPlatform::onClientSetup);
        modBus.addListener(ClientLifecycleEvents::onClientSetup);
        MinecraftForge.EVENT_BUS.addListener(ClientTickEvents::onClientTick);
        MinecraftForge.EVENT_BUS.addListener(ClientCommandRegistrationCallback.EVENT::onRegisterClientCommands);
        MinecraftForge.EVENT_BUS.addListener(ForgeClientPlatform::onMovementInputUpdate);
        DesktopDebug.log("Forge client platform listeners registered");
    }

    private static void onClientSetup(FMLClientSetupEvent event) {
        DesktopDebug.log("Forge client setup enqueue WindowedInventoryClient.initialize");
        event.enqueueWork(WindowedInventoryClient::initialize);
    }

    private static void onMovementInputUpdate(MovementInputUpdateEvent event) {
        movementInputEvents++;
        syncDesktopMovementInput(Minecraft.getInstance(), event.getInput());
    }

    private static void syncDesktopMovementInput(Minecraft minecraft, Object movementInput) {
        if (!SaltsInventoryRuntime.isEnabled()) {
            traceMovementSkip("runtime-disabled", movementInput, null);
            return;
        }

        if (movementInput == null) {
            traceMovementSkip("null-input", null, null);
            return;
        }

        Screen screen = minecraft.screen;
        if (!(screen instanceof InventoryDesktopScreen desktop)) {
            traceMovementSkip("screen-not-desktop", movementInput, screen);
            return;
        }

        if (!desktop.hasWindows() && !desktop.isHotbarOnly()) {
            traceMovementSkip("desktop-inactive", movementInput, screen);
            return;
        }

        boolean enabled = !desktop.isTextInputActive();
        boolean allowShift = !desktop.hasWindows() || desktop.isCameraControlActive();
        WindowedInventoryClient.syncMovementKeys(minecraft, enabled, allowShift);
        applyMovementInput(minecraft, desktop, movementInput, enabled, allowShift);
    }

    private static void applyMovementInput(Minecraft minecraft, InventoryDesktopScreen desktop, Object movementInput, boolean enabled, boolean allowShift) {
        Options options = minecraft.options;
        boolean forward = isDown(minecraft, options.keyUp, enabled);
        boolean backward = isDown(minecraft, options.keyDown, enabled);
        boolean left = isDown(minecraft, options.keyLeft, enabled);
        boolean right = isDown(minecraft, options.keyRight, enabled);
        boolean jump = isDown(minecraft, options.keyJump, enabled);
        boolean shift = isDown(minecraft, options.keyShift, enabled && allowShift);
        boolean sprint = isDown(minecraft, options.keySprint, enabled);
        float forwardImpulse = forward == backward ? 0.0F : (forward ? 1.0F : -1.0F);
        float leftImpulse = left == right ? 0.0F : (left ? 1.0F : -1.0F);

        try {
            setField(movementInput, "up", forward);
            setField(movementInput, "down", backward);
            setField(movementInput, "left", left);
            setField(movementInput, "right", right);
            setField(movementInput, "jumping", jump);
            setField(movementInput, "shiftKeyDown", shift);
            setField(movementInput, "forwardImpulse", forwardImpulse);
            setField(movementInput, "leftImpulse", leftImpulse);
            logMovementPatch(desktop, movementInput, enabled, allowShift, forward, backward, left, right, jump, shift, sprint, leftImpulse, forwardImpulse);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            if (!movementInputWarningLogged) {
                movementInputWarningLogged = true;
                WindowedInventoryClient.syncMovementKeys(minecraft, false);
                DesktopDebug.warn(
                    "Forge movement input sync failed input={} screen={} enabled={} allowShift={} reason={}",
                    className(movementInput),
                    className(desktop),
                    enabled,
                    allowShift,
                    exception.toString()
                );
                com.salts_inventory_update.SaltsInventoryUpdate.LOGGER.warn("Forge movement input sync failed", exception);
            }
        }
    }

    private static boolean isDown(Minecraft minecraft, KeyMapping keyMapping, boolean enabled) {
        if (!enabled) {
            keyMapping.setDown(false);
            return false;
        }

        boolean down = keyMapping.isDown();
        try {
            InputConstants.Key key = KeyBindingHelper.getBoundKeyOf(keyMapping);
            if (key.getType() == InputConstants.Type.KEYSYM && key.getValue() != InputConstants.UNKNOWN.getValue()) {
                down = GLFW.glfwGetKey(windowHandle(minecraft), key.getValue()) != GLFW.GLFW_RELEASE;
            }
        } catch (RuntimeException ignored) {
            DesktopDebug.trace("Forge movement raw key lookup failed mapping={} reason={}", keyMapping.getName(), ignored.toString());
        }
        keyMapping.setDown(down);
        return down;
    }

    private static long windowHandle(Minecraft minecraft) {
        try {
            Object window = minecraft.getWindow();
            Object value = windowHandleMethod(window.getClass()).invoke(window);
            if (value instanceof Number number) {
                return number.longValue();
            }
        } catch (ReflectiveOperationException | RuntimeException exception) {
            DesktopDebug.trace("Forge movement window handle lookup failed reason={}", exception.toString());
        }
        return 0L;
    }

    private static Method windowHandleMethod(Class<?> type) throws NoSuchMethodException {
        if (windowHandleMethod == null) {
            try {
                windowHandleMethod = type.getMethod("handle");
            } catch (NoSuchMethodException ignored) {
                windowHandleMethod = type.getMethod("getWindow");
            }
        }
        return windowHandleMethod;
    }

    private static void setField(Object target, String name, boolean value) throws ReflectiveOperationException {
        Field field = requireField(target.getClass(), name);
        field.setBoolean(target, value);
    }

    private static void setField(Object target, String name, float value) throws ReflectiveOperationException {
        Field field = requireField(target.getClass(), name);
        field.setFloat(target, value);
    }

    private static Field requireField(Class<?> type, String name) throws NoSuchFieldException {
        Field field = findField(type, name);
        if (field == null) {
            throw new NoSuchFieldException(type.getName() + "." + name);
        }
        return field;
    }

    private static Field findField(Class<?> type, String name) {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            try {
                Field field = current.getDeclaredField(name);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException ignored) {
            }
        }
        return null;
    }

    private static void logMovementPatch(
        InventoryDesktopScreen desktop,
        Object movementInput,
        boolean enabled,
        boolean allowShift,
        boolean forward,
        boolean backward,
        boolean left,
        boolean right,
        boolean jump,
        boolean shift,
        boolean sprint,
        float leftImpulse,
        float forwardImpulse
    ) {
        movementInputPatches++;
        if (!DesktopDebug.enabled()) {
            return;
        }

        String summary = "legacy"
            + "|enabled=" + enabled
            + "|allowShift=" + allowShift
            + "|forward=" + forward
            + "|backward=" + backward
            + "|left=" + left
            + "|right=" + right
            + "|jump=" + jump
            + "|shift=" + shift
            + "|sprint=" + sprint
            + "|windows=" + desktop.hasWindows()
            + "|hotbarOnly=" + desktop.isHotbarOnly()
            + "|textInput=" + desktop.isTextInputActive()
            + "|camera=" + desktop.isCameraControlActive();
        boolean changed = !summary.equals(lastMovementInputSummary);
        if (DesktopDebug.traceEnabled() || changed || movementInputPatches % MOVEMENT_LOG_INTERVAL == 1) {
            lastMovementInputSummary = summary;
            DesktopDebug.log(
                "Forge movement input apply event={} patch={} shape=legacy input={} windows={} hotbarOnly={} textInput={} camera={} enabled={} allowShift={} keys=[forward:{} backward:{} left:{} right:{} jump:{} shift:{} sprint:{}] impulses=[left:{} forward:{}]",
                movementInputEvents,
                movementInputPatches,
                className(movementInput),
                desktop.hasWindows(),
                desktop.isHotbarOnly(),
                desktop.isTextInputActive(),
                desktop.isCameraControlActive(),
                enabled,
                allowShift,
                forward,
                backward,
                left,
                right,
                jump,
                shift,
                sprint,
                leftImpulse,
                forwardImpulse
            );
        }
    }

    private static void traceMovementSkip(String reason, Object movementInput, Screen screen) {
        if (!DesktopDebug.traceEnabled()) {
            return;
        }

        movementInputSkips++;
        String summary = reason + "|input=" + className(movementInput) + "|screen=" + className(screen);
        boolean changed = !summary.equals(lastMovementInputSkipSummary);
        if (changed || movementInputSkips % MOVEMENT_LOG_INTERVAL == 1) {
            lastMovementInputSkipSummary = summary;
            DesktopDebug.trace(
                "Forge movement input skip event={} skip={} reason={} runtime={} input={} screen={}",
                movementInputEvents,
                movementInputSkips,
                reason,
                SaltsInventoryRuntime.isEnabled(),
                className(movementInput),
                className(screen)
            );
        }
    }

    private static String className(Object value) {
        return value == null ? "null" : value.getClass().getName();
    }
}
