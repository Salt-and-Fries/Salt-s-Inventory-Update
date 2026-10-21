package com.salts_inventory_update.platform;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import com.mojang.blaze3d.platform.InputConstants;
import com.salts_inventory_update.SaltsInventoryRuntime;
import com.salts_inventory_update.client.InventoryDesktopScreen;
import com.salts_inventory_update.client.WindowedInventoryClient;
import com.salts_inventory_update.debug.DesktopDebug;

import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.phys.Vec2;
import com.salts_inventory_update.platform.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import com.salts_inventory_update.platform.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import com.salts_inventory_update.platform.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import com.salts_inventory_update.platform.fabric.api.client.keybinding.v1.KeyBindingHelper;
import com.salts_inventory_update.platform.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.common.NeoForge;

public final class NeoForgeClientPlatform {
    private static final int MOVEMENT_LOG_INTERVAL = 20;

    private static boolean initialized;
    private static Field keyPressesField;
    private static Constructor<?> keyPressesConstructor;
    private static Field moveVectorField;
    private static Field minecraftScreenField;
    private static Field minecraftGuiField;
    private static Method guiScreenMethod;
    private static Method windowHandleMethod;
    private static Method legacyRawKeyMethod;
    private static Method sdlRawKeyMethod;
    private static boolean movementInputWarningLogged;
    private static boolean recordMovementShapeLogged;
    private static boolean legacyMovementShapeLogged;
    private static boolean recordMovementUnavailableLogged;
    private static int movementInputEvents;
    private static int movementInputPatches;
    private static int movementInputSkips;
    private static String lastMovementInputSummary = "";
    private static String lastMovementInputSkipSummary = "";

    private NeoForgeClientPlatform() {
    }

    public static void initialize(IEventBus modBus) {
        if (initialized) {
            return;
        }
        initialized = true;

        registerResourceReloadListener(modBus);
        modBus.addListener(NeoForgeClientPlatform::onRegisterKeyMappings);
        modBus.addListener(KeyMappingHelper::onRegisterKeyMappings);
        modBus.addListener(NeoForgeClientPlatform::onClientSetup);
        NeoForge.EVENT_BUS.addListener(ClientTickEvents::onStartClientTick);
        NeoForge.EVENT_BUS.addListener(ClientTickEvents::onEndClientTick);
        NeoForge.EVENT_BUS.addListener(ClientCommandRegistrationCallback.EVENT::onRegisterClientCommands);
        NeoForge.EVENT_BUS.addListener(NeoForgeClientPlatform::onMovementInputUpdate);
        NeoForge.EVENT_BUS.addListener(NeoForgeClientPlatform::onHudLayer);
        registerConfigScreen();
    }

    private static void onHudLayer(RenderGuiLayerEvent.Pre event) {
        if (!WindowedInventoryClient.shouldHideHotbar() || !event.getName().getNamespace().equals("minecraft")) {
            return;
        }
        // Layer identifiers span the legacy XP/jump bars and the newer contextual bar.
        switch (event.getName().getPath()) {
            case "hotbar", "player_health", "armor_level", "food_level", "air_level", "vehicle_health",
                "jump_meter", "experience_bar", "experience_level", "contextual_info_bar_background",
                "contextual_info_bar", "selected_item_name", "spectator_tooltip" -> event.setCanceled(true);
            default -> { }
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void registerResourceReloadListener(IEventBus modBus) {
        // NeoForge changed the event name and added keyed listeners after 1.21.1.
        // Register on the mod bus before Minecraft freezes the reload listener list.
        try {
            Class eventType;
            try {
                eventType = Class.forName("net.neoforged.neoforge.client.event.AddClientReloadListenersEvent");
            } catch (ClassNotFoundException legacy) {
                eventType = Class.forName("net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent");
            }
            modBus.addListener(eventType, event -> {
                net.minecraft.server.packs.resources.ResourceManagerReloadListener listener = manager ->
                    Minecraft.getInstance().execute(InventoryDesktopScreen::resourcesReloaded);
                try {
                    for (Method method : event.getClass().getMethods()) {
                        if (method.getName().equals("addListener") && method.getParameterCount() == 2) {
                            method.invoke(event, WindowedInventoryClient.id("desktop_resources"), listener);
                            return;
                        }
                        if (method.getName().equals("registerReloadListener") && method.getParameterCount() == 1) {
                            method.invoke(event, listener);
                            return;
                        }
                    }
                    throw new IllegalStateException("Unsupported NeoForge resource reload event " + event.getClass().getName());
                } catch (ReflectiveOperationException error) {
                    throw new IllegalStateException("Could not register desktop resource reload listener", error);
                }
            });
        } catch (ClassNotFoundException error) {
            throw new IllegalStateException("NeoForge client resource reload event is unavailable", error);
        }
    }

    private static void registerConfigScreen() {
        ModLoadingContext.get().registerExtensionPoint(
            IConfigScreenFactory.class,
            () -> (container, modListScreen) -> WindowedInventoryClient.createConfigScreen(modListScreen)
        );
    }

    private static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        WindowedInventoryClient.registerKeyMappings();
        KeyBindingHelper.onRegisterKeyMappings(event);
    }

    private static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            WindowedInventoryClient.initialize();
            ClientLifecycleEvents.fireStarted(Minecraft.getInstance());
        });
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

        Screen screen = activeScreen(minecraft);
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
            if (applyRecordMovementInput(movementInput, forward, backward, left, right, jump, shift, sprint, leftImpulse, forwardImpulse)) {
                logMovementPatch(desktop, movementInput, enabled, allowShift, forward, backward, left, right, jump, shift, sprint, leftImpulse, forwardImpulse, "record");
                return;
            }

            setField(movementInput, "up", forward);
            setField(movementInput, "down", backward);
            setField(movementInput, "left", left);
            setField(movementInput, "right", right);
            setField(movementInput, "jumping", jump);
            setField(movementInput, "shiftKeyDown", shift);
            setField(movementInput, "forwardImpulse", forwardImpulse);
            setField(movementInput, "leftImpulse", leftImpulse);
            logMovementPatch(desktop, movementInput, enabled, allowShift, forward, backward, left, right, jump, shift, sprint, leftImpulse, forwardImpulse, "legacy");
        } catch (ReflectiveOperationException | RuntimeException exception) {
            if (!movementInputWarningLogged) {
                movementInputWarningLogged = true;
                WindowedInventoryClient.syncMovementKeys(minecraft, false);
                DesktopDebug.warn(
                    "NeoForge movement input sync failed input={} screen={} enabled={} allowShift={} reason={}",
                    className(movementInput),
                    className(desktop),
                    enabled,
                    allowShift,
                    exception.toString()
                );
                com.salts_inventory_update.SaltsInventoryUpdate.LOGGER.warn("NeoForge movement input sync failed", exception);
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
            InputConstants.Key key = KeyMappingHelper.getBoundKeyOf(keyMapping);
            if (key.getValue() != InputConstants.UNKNOWN.getValue()) {
                if ("KEYBOARD".equals(key.getType().name())) {
                    if (sdlRawKeyMethod == null) {
                        sdlRawKeyMethod = InputConstants.class.getMethod("isKeyDown", int.class);
                    }
                    down = (boolean) sdlRawKeyMethod.invoke(null, key.getValue());
                } else if ("KEYSYM".equals(key.getType().name())) {
                    if (legacyRawKeyMethod == null) {
                        legacyRawKeyMethod = Class.forName("org.lwjgl.glfw.GLFW").getMethod("glfwGetKey", long.class, int.class);
                    }
                    down = ((int) legacyRawKeyMethod.invoke(null, windowHandle(minecraft), key.getValue())) != 0;
                }
            }
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            DesktopDebug.trace("NeoForge movement raw key lookup failed mapping={} reason={}", keyMapping.getName(), ignored.toString());
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
            DesktopDebug.trace("NeoForge movement window handle lookup failed reason={}", exception.toString());
        }
        return 0L;
    }

    private static Screen activeScreen(Minecraft minecraft) {
        try {
            Field screenField = minecraftScreenField();
            if (screenField != null) {
                Object screen = screenField.get(minecraft);
                if (screen instanceof Screen typed) {
                    return typed;
                }
            }

            Field guiField = minecraftGuiField();
            if (guiField != null) {
                Object gui = guiField.get(minecraft);
                Method screenMethod = guiScreenMethod(gui.getClass());
                Object screen = screenMethod.invoke(gui);
                if (screen instanceof Screen typed) {
                    return typed;
                }
            }
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            DesktopDebug.trace("NeoForge movement active screen lookup failed reason={}", ignored.toString());
        }
        return null;
    }

    private static Field minecraftScreenField() {
        if (minecraftScreenField == null) {
            minecraftScreenField = findField(Minecraft.class, "screen");
        }
        return minecraftScreenField;
    }

    private static Field minecraftGuiField() {
        if (minecraftGuiField == null) {
            minecraftGuiField = findField(Minecraft.class, "gui");
        }
        return minecraftGuiField;
    }

    private static Method guiScreenMethod(Class<?> type) throws NoSuchMethodException {
        if (guiScreenMethod == null) {
            guiScreenMethod = type.getMethod("screen");
        }
        return guiScreenMethod;
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

    private static boolean applyRecordMovementInput(
        Object movementInput,
        boolean forward,
        boolean backward,
        boolean left,
        boolean right,
        boolean jump,
        boolean shift,
        boolean sprint,
        float leftImpulse,
        float forwardImpulse
    ) throws ReflectiveOperationException {
        Field keyPresses = keyPressesField(movementInput.getClass());
        if (keyPresses == null) {
            if (!recordMovementUnavailableLogged) {
                recordMovementUnavailableLogged = true;
                DesktopDebug.log("NeoForge movement input record fields unavailable input={} fallback=legacy", className(movementInput));
            }
            return false;
        }

        Constructor<?> constructor = keyPressesConstructor(keyPresses.getType());
        Object value = constructor.newInstance(forward, backward, left, right, jump, shift, sprint);
        keyPresses.set(movementInput, value);

        Field moveVector = moveVectorField(movementInput.getClass());
        if (moveVector != null) {
            moveVector.set(movementInput, new Vec2(leftImpulse, forwardImpulse).normalized());
        }
        if (!recordMovementShapeLogged) {
            recordMovementShapeLogged = true;
            DesktopDebug.log(
                "NeoForge movement input record fields detected input={} keyPresses={} moveVector={}",
                className(movementInput),
                keyPresses.getType().getName(),
                moveVector != null
            );
        }
        return true;
    }

    private static Field keyPressesField(Class<?> type) {
        if (keyPressesField == null) {
            keyPressesField = findField(type, "keyPresses");
        }
        return keyPressesField;
    }

    private static Constructor<?> keyPressesConstructor(Class<?> type) throws NoSuchMethodException {
        if (keyPressesConstructor == null) {
            keyPressesConstructor = type.getConstructor(
                boolean.class,
                boolean.class,
                boolean.class,
                boolean.class,
                boolean.class,
                boolean.class,
                boolean.class
            );
        }
        return keyPressesConstructor;
    }

    private static Field moveVectorField(Class<?> type) {
        if (moveVectorField == null) {
            moveVectorField = findField(type, "moveVector");
        }
        return moveVectorField;
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
        float forwardImpulse,
        String shape
    ) {
        movementInputPatches++;
        if (!DesktopDebug.enabled()) {
            return;
        }

        if ("legacy".equals(shape) && !legacyMovementShapeLogged) {
            legacyMovementShapeLogged = true;
            DesktopDebug.log("NeoForge movement input legacy fields patched input={}", className(movementInput));
        }

        String summary = shape
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
                "NeoForge movement input apply event={} patch={} shape={} input={} windows={} hotbarOnly={} textInput={} camera={} enabled={} allowShift={} keys=[forward:{} backward:{} left:{} right:{} jump:{} shift:{} sprint:{}] impulses=[left:{} forward:{}]",
                movementInputEvents,
                movementInputPatches,
                shape,
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
                "NeoForge movement input skip event={} skip={} reason={} runtime={} input={} screen={}",
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
