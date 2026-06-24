package com.salts_inventory_update.functionaltest;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.MenuType;

import com.salts_inventory_update.SaltsInventoryRuntime;
import com.salts_inventory_update.SaltsInventoryUpdate;
import com.salts_inventory_update.VersionInfo;
import com.salts_inventory_update.api.client.desktop.DesktopWindowLookupContext;
import com.salts_inventory_update.api.desktop.SaltsInventoryDesktopApi;
import com.salts_inventory_update.client.SaltsInventoryConfig;
import com.salts_inventory_update.client.WindowOpeningStyle;
import com.salts_inventory_update.client.WindowedInventoryClient;
import com.salts_inventory_update.inventory.InventoryExpansion;
import com.salts_inventory_update.network.DesktopPackets;

public final class FunctionalTestHarness {
    private static final int START_DELAY_TICKS = 5;
    private static boolean initialized;
    private static boolean completed;
    private static int ticksUntilRun = START_DELAY_TICKS;

    private FunctionalTestHarness() {
    }

    public static synchronized void tryInitialize() {
        if (!isEnabled() || initialized) {
            return;
        }

        initialized = true;
        SaltsInventoryUpdate.LOGGER.info("SIU_FUNCTIONAL_TEST event=initialized mc={} loader={}", VersionInfo.MINECRAFT_VERSION, loaderLabel());
        ClientTickEvents.END_CLIENT_TICK.register(FunctionalTestHarness::tick);
    }

    private static void tick(Minecraft minecraft) {
        if (completed) {
            return;
        }

        if (ticksUntilRun-- > 0) {
            return;
        }

        completed = true;
        ResultRecorder recorder = new ResultRecorder();
        boolean configuredEnabled = SaltsInventoryRuntime.isConfiguredEnabled();
        boolean serverDesktopAvailable = SaltsInventoryRuntime.isServerDesktopAvailable();

        try {
            SaltsInventoryRuntime.setConfiguredEnabled(true);
            SaltsInventoryRuntime.setServerDesktopAvailable(true);

            runTest("runtime-and-keybinds", recorder, FunctionalTestHarness::testRuntimeAndKeybinds);
            runTest("config-normalization", recorder, FunctionalTestHarness::testConfigNormalization);
            runTest("desktop-menu-screens", recorder, FunctionalTestHarness::testDesktopMenuScreens);
            runTest("desktop-api-definitions", recorder, FunctionalTestHarness::testDesktopApiDefinitions);
            runTest("desktop-packets", recorder, FunctionalTestHarness::testDesktopPackets);
            runTest("inventory-expansion", recorder, FunctionalTestHarness::testInventoryExpansion);
        } finally {
            SaltsInventoryRuntime.setConfiguredEnabled(configuredEnabled);
            SaltsInventoryRuntime.setServerDesktopAvailable(serverDesktopAvailable);
        }

        String status = recorder.failed == 0 ? "PASS" : "FAIL";
        SaltsInventoryUpdate.LOGGER.info(
            "SIU_FUNCTIONAL_TEST_SUMMARY status={} passed={} failed={} mc={} loader={}",
            status,
            recorder.passed,
            recorder.failed,
            VersionInfo.MINECRAFT_VERSION,
            loaderLabel()
        );

        if (shouldExit()) {
            requestStop(minecraft);
        }
    }

    private static void testRuntimeAndKeybinds(ResultRecorder recorder) {
        recorder.check("runtime.configured_enabled", SaltsInventoryRuntime.isConfiguredEnabled());
        recorder.check("runtime.server_desktop_available", SaltsInventoryRuntime.isServerDesktopAvailable());
        recorder.check("runtime.enabled", SaltsInventoryRuntime.isEnabled());
        recorder.check("keybind.character_window_registered", WindowedInventoryClient.characterWindowKey() != null);
    }

    private static void testConfigNormalization(ResultRecorder recorder) {
        ConfigSnapshot original = ConfigSnapshot.capture(SaltsInventoryConfig.get());
        try {
            SaltsInventoryConfig.update(config -> {
                config.enableMod = false;
                config.expandableInventory = true;
                config.windowOpeningStyle = "not_a_real_style";
                config.openUnlocked = true;
                config.allowResizing = true;
                config.enableWindowSnapping = false;
                config.resetLockedWindows = false;
                config.enableGhostPins = true;
                config.ghostWindowOpacity = 42.0D;
                config.eHoldCloseAllSeconds = 0.10D;
            });

            SaltsInventoryConfig.ConfigFile normalized = SaltsInventoryConfig.reload();
            recorder.check("config.enable_mod_updates_runtime", !SaltsInventoryRuntime.isConfiguredEnabled());
            recorder.check(
                "config.window_opening_style_normalizes",
                WindowOpeningStyle.parse("not_a_real_style").name().equals(normalized.windowOpeningStyle)
            );
            recorder.check("config.ghost_opacity_clamps_high", normalized.ghostWindowOpacity == 0.90D);
            recorder.check("config.e_hold_seconds_clamps_low", normalized.eHoldCloseAllSeconds == 0.50D);
        } finally {
            SaltsInventoryConfig.update(original::applyTo);
            SaltsInventoryConfig.reload();
        }
    }

    private static void testDesktopMenuScreens(ResultRecorder recorder) throws ReflectiveOperationException {
        Map<MenuType<?>, ?> screens = menuScreenConstructors();
        for (String menuName : expectedMenuNames()) {
            MenuType<?> menuType = menuType(menuName);
            if (menuType == null) {
                continue;
            }

            Object constructor = screens.get(menuType);
            recorder.check("menu_screen." + menuName.toLowerCase(Locale.ROOT), constructor != null);
        }
    }

    private static void testDesktopApiDefinitions(ResultRecorder recorder) {
        for (String menuName : List.of("FURNACE", "BLAST_FURNACE", "SMOKER")) {
            MenuType<?> menuType = menuType(menuName);
            recorder.check("api_definition." + menuName.toLowerCase(Locale.ROOT), menuType != null && hasClientDefinition(menuType));
        }
    }

    private static void testDesktopPackets(ResultRecorder recorder) {
        recorder.check("packets.menu_type_round_trip.furnace", roundTrips(MenuType.FURNACE));
        MenuType<?> crafter = menuType("CRAFTER_3x3");
        if (crafter != null) {
            recorder.check("packets.menu_type_round_trip.crafter", roundTrips(crafter));
        }
        recorder.check("packets.pin_mode_order", DesktopPackets.PIN_MODE_UNPINNED == 0 && DesktopPackets.PIN_MODE_PINNED == 1 && DesktopPackets.PIN_MODE_GHOST_PINNED == 2);
        recorder.check("packets.quick_target_order", DesktopPackets.QUICK_TARGET_DEFAULT == 0 && DesktopPackets.QUICK_TARGET_SESSION == 1 && DesktopPackets.QUICK_TARGET_HOTBAR == 2);
        recorder.check("packets.special_kinds",
            DesktopPackets.SPECIAL_GENERIC == 0
                && DesktopPackets.SPECIAL_HORSE == 1
                && DesktopPackets.SPECIAL_CAMEL > DesktopPackets.SPECIAL_HORSE
                && DesktopPackets.SPECIAL_LLAMA > DesktopPackets.SPECIAL_CAMEL
        );
    }

    private static void testInventoryExpansion(ResultRecorder recorder) {
        recorder.check("inventory_expansion.clamp_low", InventoryExpansion.clampSlotCount(-100) == 0);
        recorder.check("inventory_expansion.clamp_high", InventoryExpansion.clampSlotCount(InventoryExpansion.HARD_MAX_EXTRA_SLOTS + 100) == InventoryExpansion.HARD_MAX_EXTRA_SLOTS);
        recorder.check("inventory_expansion.first_cost", InventoryExpansion.costForNextSlot(0) == 1);
        recorder.check("inventory_expansion.max_cost", InventoryExpansion.costForNextSlot(InventoryExpansion.HARD_MAX_EXTRA_SLOTS) == Integer.MAX_VALUE);
    }

    private static void runTest(String suite, ResultRecorder recorder, TestBody test) {
        try {
            test.run(recorder);
            SaltsInventoryUpdate.LOGGER.info("SIU_FUNCTIONAL_TEST_SUITE suite={} status=DONE", suite);
        } catch (Throwable throwable) {
            recorder.fail(suite + ".exception", throwable.toString());
        }
    }

    private static boolean hasClientDefinition(MenuType<?> menuType) {
        DesktopWindowLookupContext context = new DesktopWindowLookupContext(
            null,
            menuType,
            Component.literal("Functional Test"),
            1,
            "functional-test",
            DesktopPackets.SPECIAL_GENERIC,
            List.of(),
            176,
            166
        );
        return SaltsInventoryDesktopApi.findDefinition(context) != null;
    }

    private static boolean roundTrips(MenuType<?> menuType) {
        return DesktopPackets.menuTypeById(DesktopPackets.menuTypeId(menuType)) == menuType;
    }

    @SuppressWarnings("unchecked")
    private static Map<MenuType<?>, ?> menuScreenConstructors() throws ReflectiveOperationException {
        try {
            Class<?> accessor = Class.forName("com.salts_inventory_update.mixin.client.MenuScreensAccessor");
            Method method = accessor.getMethod("salts_inventory_update$getScreens");
            return (Map<MenuType<?>, ?>) method.invoke(null);
        } catch (ClassNotFoundException | IllegalAccessException | NoSuchMethodException exception) {
            return menuScreenConstructorsReflectively();
        } catch (InvocationTargetException exception) {
            return menuScreenConstructorsReflectively();
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<MenuType<?>, ?> menuScreenConstructorsReflectively() throws ReflectiveOperationException {
        for (Field field : MenuScreens.class.getDeclaredFields()) {
            if (Map.class.isAssignableFrom(field.getType())) {
                field.setAccessible(true);
                Object value = field.get(null);
                if (value instanceof Map<?, ?> map) {
                    return (Map<MenuType<?>, ?>) map;
                }
            }
        }

        throw new NoSuchFieldException("Unable to locate MenuScreens constructor map");
    }

    private static List<String> expectedMenuNames() {
        List<String> names = new ArrayList<>(List.of(
            "GENERIC_9x1",
            "GENERIC_9x2",
            "GENERIC_9x3",
            "GENERIC_9x4",
            "GENERIC_9x5",
            "GENERIC_9x6",
            "GENERIC_3x3",
            "ANVIL",
            "BEACON",
            "BLAST_FURNACE",
            "BREWING_STAND",
            "CRAFTING",
            "ENCHANTMENT",
            "FURNACE",
            "GRINDSTONE",
            "HOPPER",
            "LOOM",
            "MERCHANT",
            "SHULKER_BOX",
            "SMITHING",
            "SMOKER",
            "CARTOGRAPHY_TABLE",
            "STONECUTTER"
        ));
        if (menuType("CRAFTER_3x3") != null) {
            names.add("CRAFTER_3x3");
        }
        return names;
    }

    private static MenuType<?> menuType(String name) {
        try {
            Field field = MenuType.class.getField(name);
            return (MenuType<?>) field.get(null);
        } catch (ReflectiveOperationException exception) {
            return null;
        }
    }

    private static void requestStop(Minecraft minecraft) {
        if ("fabric".equalsIgnoreCase(loaderLabel())) {
            requestHardExit();
            return;
        }

        try {
            Method stop = Minecraft.class.getMethod("stop");
            stop.invoke(minecraft);
        } catch (ReflectiveOperationException exception) {
            SaltsInventoryUpdate.LOGGER.warn("SIU_FUNCTIONAL_TEST event=stop_failed reason={}", exception.toString());
            requestHardExit();
        }
    }

    private static void requestHardExit() {
        Thread exitThread = new Thread(() -> {
            try {
                Thread.sleep(500L);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
            Runtime.getRuntime().halt(0);
        }, "SIU Functional Test Exit");
        exitThread.setDaemon(true);
        exitThread.start();
    }

    private static boolean isEnabled() {
        return Boolean.getBoolean("salts_inventory_update.functionalTests")
            || isTruthy(System.getenv("SIU_FUNCTIONAL_TESTS"));
    }

    private static boolean shouldExit() {
        return Boolean.getBoolean("salts_inventory_update.functionalTests.exit")
            || isTruthy(System.getenv("SIU_FUNCTIONAL_TEST_EXIT"));
    }

    private static boolean isTruthy(String value) {
        return value != null && (
            value.equalsIgnoreCase("true")
                || value.equalsIgnoreCase("1")
                || value.equalsIgnoreCase("yes")
                || value.equalsIgnoreCase("on")
        );
    }

    private static String loaderLabel() {
        String loader = System.getProperty("salts_inventory_update.functionalTests.loader");
        if (loader == null || loader.isBlank()) {
            loader = System.getenv("SIU_FUNCTIONAL_TEST_LOADER");
        }
        return loader == null || loader.isBlank() ? "unknown" : loader;
    }

    private interface TestBody {
        void run(ResultRecorder recorder) throws Exception;
    }

    private static final class ResultRecorder {
        private int passed;
        private int failed;

        private void check(String name, boolean condition) {
            if (condition) {
                pass(name);
            } else {
                fail(name, "condition=false");
            }
        }

        private void pass(String name) {
            this.passed++;
            SaltsInventoryUpdate.LOGGER.info("SIU_FUNCTIONAL_TEST test={} status=PASS", name);
        }

        private void fail(String name, String reason) {
            this.failed++;
            SaltsInventoryUpdate.LOGGER.error("SIU_FUNCTIONAL_TEST test={} status=FAIL reason={}", name, reason);
        }
    }

    private record ConfigSnapshot(
        boolean enableMod,
        boolean expandableInventory,
        String windowOpeningStyle,
        boolean openUnlocked,
        boolean allowResizing,
        boolean enableWindowSnapping,
        boolean resetLockedWindows,
        boolean enableGhostPins,
        double ghostWindowOpacity,
        double eHoldCloseAllSeconds
    ) {
        private static ConfigSnapshot capture(SaltsInventoryConfig.ConfigFile config) {
            return new ConfigSnapshot(
                config.enableMod,
                config.expandableInventory,
                config.windowOpeningStyle,
                config.openUnlocked,
                config.allowResizing,
                config.enableWindowSnapping,
                config.resetLockedWindows,
                config.enableGhostPins,
                config.ghostWindowOpacity,
                config.eHoldCloseAllSeconds
            );
        }

        private void applyTo(SaltsInventoryConfig.ConfigFile config) {
            config.enableMod = this.enableMod;
            config.expandableInventory = this.expandableInventory;
            config.windowOpeningStyle = this.windowOpeningStyle;
            config.openUnlocked = this.openUnlocked;
            config.allowResizing = this.allowResizing;
            config.enableWindowSnapping = this.enableWindowSnapping;
            config.resetLockedWindows = this.resetLockedWindows;
            config.enableGhostPins = this.enableGhostPins;
            config.ghostWindowOpacity = this.ghostWindowOpacity;
            config.eHoldCloseAllSeconds = this.eHoldCloseAllSeconds;
        }
    }
}
