package com.salts_inventory_update.controlify.qa;

import com.salts_inventory_update.api.client.desktop.DesktopWorkspaceWindow;
import com.salts_inventory_update.controlify.RadialInventoryScreen;
import dev.isxander.controlify.Controlify;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Observes native extraction. Host SDL motion is isolated only during the disposable QA controller session. */
public final class QaFeatureProbe {
    public static long tabFrames, tooltipCalls, cursorCalls;
    public static Map<UUID, String> tabNames = Map.of();
    public record TabIcon(net.minecraft.world.item.ItemStack stack, double top, double bottom, double iconTop) { }
    public static final Map<UUID, TabIcon> tabIcons = new LinkedHashMap<>();
    public static boolean renderingTabs;
    public static int tabTextDraws;
    public static int tabCount, tooltipX, tooltipY, tooltipCursorX, tooltipCursorY;
    public static float cursorScaleX, cursorScaleY, cursorExpectedScale;
    public static int cursorGuiX, cursorGuiY;
    private static int extractingTabs;
    private static java.util.List<UUID> extractingTabOrder = java.util.List.of();
    private static final java.util.List<String> sortRequests = new java.util.ArrayList<>();
    private static final java.util.List<String> creativeActions = new java.util.ArrayList<>();
    private static final java.util.List<String> navigationCalls = new java.util.ArrayList<>();
    private static final java.util.ArrayDeque<String> virtualMouseLifecycle = new java.util.ArrayDeque<>();
    private static long virtualMouseLifecycleEvents;
    private static long isolatedHostMotionEvents;
    private static final java.util.ArrayDeque<Map<String, Object>> isolatedHostMotion = new java.util.ArrayDeque<>();
    public static String scenario = "";
    public static long navigationRequests, navigationNeutralSamples;
    private static int navigationBeforeX, navigationBeforeY;
    public static long categoryFrames, creativeTabFrames;
    public static int categoryCount, selectedCategoryCount, activeCreativeTabCount, hoveredCreativeTabCount, hoveredActiveCreativeTabCount;
    private static int extractingCategories, extractingSelectedCategories, extractingActiveTabs, extractingHoveredTabs, extractingHoveredActiveTabs;
    public static net.minecraft.world.item.crafting.display.RecipeDisplayId renderedRecipeId;
    public static long recipePreviewFrames;
    public static long vanillaBookSpritesContextualEnabled, vanillaBookSpritesLegacyOptOut;
    private static java.lang.reflect.Field coreContextualPresentation;
    private static final java.util.List<String> unexpectedVanillaBookSprites = new java.util.ArrayList<>();

    private QaFeatureProbe() { }
    public static void isolatedHostMotion(org.lwjgl.sdl.SDL_Event event) {
        var motion = event.motion();
        var observation = new LinkedHashMap<String, Object>();
        observation.put("scenario", scenario); observation.put("type", event.type());
        observation.put("windowId", motion.windowID()); observation.put("which", motion.which());
        observation.put("timestamp", motion.timestamp()); observation.put("x", motion.x()); observation.put("y", motion.y());
        observation.put("xrel", motion.xrel()); observation.put("yrel", motion.yrel());
        observation.put("syntheticControllerCurrent", true); observation.put("worldPlayerLoaded", true);
        observation.put("unfinishedRun", true);
        isolatedHostMotionEvents++;
        if (isolatedHostMotion.size() == 64) isolatedHostMotion.removeFirst();
        isolatedHostMotion.addLast(Map.copyOf(observation));
        if (isolatedHostMotionEvents <= 64) System.out.println("CONTROLIFY_QA_ISOLATED_HOST_SDL_MOTION " + observation);
    }
    public static void beginTabs(Map<UUID, DesktopWorkspaceWindow> windows) {
        var names = new LinkedHashMap<UUID, String>();
        windows.forEach((id, window) -> names.put(id, window.title()));
        tabNames = Map.copyOf(names); extractingTabOrder = java.util.List.copyOf(windows.keySet());
        extractingTabs = 0; tabIcons.clear(); tabTextDraws = 0; renderingTabs = true;
    }
    public static void tab() { extractingTabs++; }
    public static void finishTabs() { tabCount = extractingTabs; tabFrames++; renderingTabs = false; }
    public static void tabIcon(GuiGraphicsExtractor graphics, net.minecraft.world.item.ItemStack stack) {
        if (!renderingTabs) return;
        UUID window = extractingTabOrder.get(extractingTabs - 1);
        var pose = graphics.pose();
        tabIcons.put(window, new TabIcon(stack.copy(), pose.m21(), pose.m21() + 30 * pose.m11(), pose.m21() + 8 * pose.m11()));
    }
    public static void tooltip(int x, int y) {
        if (!(Minecraft.getInstance().gui.screen() instanceof RadialInventoryScreen)) return;
        var mouse = Controlify.instance().virtualMouseHandler();
        tooltipX = x; tooltipY = y;
        var window = Minecraft.getInstance().getWindow();
        tooltipCursorX = (int) Math.round(mouse.getCurrentX(0) * (double) window.getGuiScaledWidth() / window.getScreenWidth());
        tooltipCursorY = (int) Math.round(mouse.getCurrentY(0) * (double) window.getGuiScaledHeight() / window.getScreenHeight()); tooltipCalls++;
    }
    public static void cursor(GuiGraphicsExtractor graphics) {
        if (!(Minecraft.getInstance().gui.screen() instanceof RadialInventoryScreen screen)) return;
        var mouse = Controlify.instance().virtualMouseHandler();
        cursorScaleX = graphics.pose().m00(); cursorScaleY = graphics.pose().m11();
        var window = Minecraft.getInstance().getWindow();
        cursorGuiX = (int) Math.round(mouse.getCurrentX(0) * (double) window.getGuiScaledWidth() / window.getScreenWidth());
        cursorGuiY = (int) Math.round(mouse.getCurrentY(0) * (double) window.getGuiScaledHeight() / window.getScreenHeight());
        cursorExpectedScale = screen.controllerCursorScale(mouse.getCurrentX(0) * (double) window.getGuiScaledWidth() / window.getScreenWidth(),
                mouse.getCurrentY(0) * (double) window.getGuiScaledHeight() / window.getScreenHeight()); cursorCalls++;
    }
    public static void sortRequest(int source, java.util.List<Integer> destinations, int focused, boolean force, boolean sent) {
        String request = "sourceSession=" + source + ",allOpenDestinationSessions=" + destinations
                + ",nativeFocusedSession=" + focused + ",force=" + force + ",sent=" + sent;
        sortRequests.add(request);
        System.out.println("CONTROLIFY_QA_NATIVE_SORT_REQUEST " + request);
    }
    public static java.util.List<String> sortRequests() { return java.util.List.copyOf(sortRequests); }
    public static void creativeAction(String phase, net.minecraft.world.item.ItemStack source, boolean fullStack) {
        var minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;
        String hotbar = java.util.stream.IntStream.range(0, 9).mapToObj(i -> i + ":" + minecraft.player.getInventory().getItem(i)).collect(java.util.stream.Collectors.joining(";"));
        String action = "phase=" + phase + ",source=" + source + ",fullStack=" + fullStack
            + ",creative=" + minecraft.player.isCreative() + ",hotbar=" + hotbar
            + ",nativeCarried=" + minecraft.player.inventoryMenu.getCarried();
        var controller = dev.isxander.controlify.api.ControlifyApi.get().getCurrentController().orElse(null);
        if (controller != null) {
            var select = dev.isxander.controlify.bindings.ControlifyBindings.INV_SELECT.on(controller);
            var inventory = dev.isxander.controlify.bindings.ControlifyBindings.INVENTORY.on(controller);
            var primary = dev.isxander.controlify.bindings.ControlifyBindings.VMOUSE_LCLICK.on(controller);
            var shifted = dev.isxander.controlify.bindings.ControlifyBindings.VMOUSE_SHIFT_CLICK.on(controller);
            action += ",configuredSelect=" + select.boundInput() + ",selectPressed=" + select.justPressed()
                + ",configuredInventory=" + inventory.boundInput() + ",inventoryPressed=" + inventory.justPressed()
                + ",nativeLeftClick=" + primary.boundInput() + ",leftPressed=" + primary.justPressed()
                + ",nativeShiftClick=" + shifted.boundInput() + ",shiftPressed=" + shifted.justPressed();
        }
        creativeActions.add(action);
        System.out.println("CONTROLIFY_QA_NATIVE_CREATIVE_ACTION " + action);
    }
    public static void navigation(String phase, dev.isxander.controlify.controller.ControllerEntity controller,
                                  dev.isxander.controlify.virtualmouse.VirtualMouseHandler mouse, String direction) {
        boolean guide = scenario.startsWith("native-DPAD-selects-crafting-guide-toggle") || scenario.startsWith("actual-cursor-hover-");
        boolean radialRelease = scenario.equals("release-LT-commits-highlight") || scenario.equals("enter-radial-highlight")
            || scenario.startsWith("swap-focus-with-A");
        if (!scenario.contains("brewing") && !guide && !scenario.endsWith("-native-cursor-ready") && !radialRelease) return;
        var input = controller.input().orElseThrow();
        var button = guide ? dev.isxander.controlify.controller.input.GamepadInputs.DPAD_DOWN_BUTTON
            : dev.isxander.controlify.controller.input.GamepadInputs.DPAD_RIGHT_BUTTON;
        var binding = (guide ? dev.isxander.controlify.bindings.ControlifyBindings.VMOUSE_SNAP_DOWN
            : dev.isxander.controlify.bindings.ControlifyBindings.VMOUSE_SNAP_RIGHT).on(controller);
        if (phase.equals("input-before")) {
            navigationBeforeX = mouse.getCurrentX(1);
            navigationBeforeY = mouse.getCurrentY(1);
        } else if (phase.equals("input-after")) {
            // The production HEAD navigation injection cancels before other snap observers.
            // A target change inside the genuine native handler observes its dispatch reliably.
            if (mouse.isVirtualMouseEnabled() && binding.digitalNow()
                    && input.rawStateNow().isButtonDown(button)
                    && (mouse.getCurrentX(1) != navigationBeforeX || mouse.getCurrentY(1) != navigationBeforeY)) navigationRequests++;
            if (mouse.isVirtualMouseEnabled() && !binding.digitalNow() && !binding.digitalPrev()
                    && !input.rawStateNow().isButtonDown(button)
                    && !input.rawStateThen().isButtonDown(button)) navigationNeutralSamples++;
        }
        String call = "scenario=" + scenario + ",phase=" + phase + ",direction=" + direction
            + ",rawNow=" + System.identityHashCode(input.rawStateNow()) + ",rawThen=" + System.identityHashCode(input.rawStateThen())
            + ",button=" + button + ",digitalNow=" + binding.digitalNow() + ",digitalPrevious=" + binding.digitalPrev() + ",justPressed=" + binding.justPressed()
            + ",driverButton=" + input.rawStateNow().isButtonDown(button)
            + ",enabled=" + mouse.isVirtualMouseEnabled() + ",cursor=" + mouse.getCurrentX(0) + "," + mouse.getCurrentY(0)
            + ",target=" + mouse.getCurrentX(1) + "," + mouse.getCurrentY(1) + ",requests=" + navigationRequests
            + ",neutralSamples=" + navigationNeutralSamples;
        navigationCalls.add(call);
        System.out.println("CONTROLIFY_QA_NATIVE_NAVIGATION " + call);
    }
    public static void virtualMouseLifecycle(String phase, dev.isxander.controlify.virtualmouse.VirtualMouseHandler mouse, String eventMode) {
        if (scenario.isEmpty()) return;
        var actual = Minecraft.getInstance().gui.screen();
        var nativeEventScreen = dev.isxander.controlify.utils.MinecraftUtil.getScreen();
        String cause = StackWalker.getInstance().walk(frames -> frames.skip(1).limit(10)
            .map(frame -> frame.getClassName() + "." + frame.getMethodName() + ":" + frame.getLineNumber())
            .collect(java.util.stream.Collectors.joining(" <- ")));
        String call = "scenario=" + scenario + ",phase=" + phase + ",eventMode=" + eventMode
            + ",currentMode=" + Controlify.instance().currentInputMode() + ",enabled=" + mouse.isVirtualMouseEnabled()
            + ",requiresVirtualMouse=" + mouse.requiresVirtualMouse()
            + ",eventScreen=" + (nativeEventScreen == null ? "null" : nativeEventScreen.getClass().getName())
            + ",actualGuiScreen=" + (actual == null ? "null" : actual.getClass().getName()) + ",cause=" + cause;
        virtualMouseLifecycleEvents++;
        if (virtualMouseLifecycle.size() == 256) virtualMouseLifecycle.removeFirst();
        virtualMouseLifecycle.addLast(call);
        if (virtualMouseLifecycleEvents <= 512) System.out.println("CONTROLIFY_QA_NATIVE_VM_LIFECYCLE " + call);
    }
    public static void beginCategoryRow() { extractingCategories = extractingSelectedCategories = 0; }
    public static void categoryTile(boolean selected) { extractingCategories++; if (selected) extractingSelectedCategories++; }
    public static void finishCategoryRow() { categoryCount = extractingCategories; selectedCategoryCount = extractingSelectedCategories; categoryFrames++; }
    public static void beginCreativeTabs() { extractingActiveTabs = extractingHoveredTabs = extractingHoveredActiveTabs = 0; }
    public static void creativeTab(boolean hovered, boolean active) {
        if (active) extractingActiveTabs++; if (hovered) extractingHoveredTabs++; if (hovered && active) extractingHoveredActiveTabs++;
    }
    public static void finishCreativeTabs() {
        activeCreativeTabCount = extractingActiveTabs; hoveredCreativeTabCount = extractingHoveredTabs;
        hoveredActiveCreativeTabCount = extractingHoveredActiveTabs; creativeTabFrames++;
    }
    public static void recipePreview(com.salts_inventory_update.api.client.desktop.DesktopContextualMenu.Recipe recipe) {
        renderedRecipeId = recipe.id(); recipePreviewFrames++;
    }
    public static void vanillaBookSprite(net.minecraft.resources.Identifier sprite) {
        if (!sprite.getNamespace().equals("minecraft") || !(sprite.getPath().equals("recipe_book/button")
                || sprite.getPath().equals("recipe_book/button_highlighted"))) return;
        var minecraft = Minecraft.getInstance();
        if (!(minecraft.gui.screen() instanceof RadialInventoryScreen)) return;
        var desktop = com.salts_inventory_update.client.InventoryDesktopScreen.current(minecraft);
        if (desktop == null) return;
        try {
            if (coreContextualPresentation == null) {
                coreContextualPresentation = com.salts_inventory_update.client.InventoryDesktopScreen.class
                    .getDeclaredField("contextualRecipePresentation");
                coreContextualPresentation.setAccessible(true);
            }
            if (coreContextualPresentation.getBoolean(desktop)) {
                vanillaBookSpritesContextualEnabled++;
                if (unexpectedVanillaBookSprites.size() < 32) unexpectedVanillaBookSprites.add("scenario=" + scenario + ",sprite=" + sprite);
            } else vanillaBookSpritesLegacyOptOut++;
        } catch (ReflectiveOperationException failure) { throw new IllegalStateException("Observe Core recipe presentation mode", failure); }
    }
    public static Map<String, Object> snapshot() {
        var result = new LinkedHashMap<String, Object>(Map.of("tabFrames", tabFrames, "lastTabCount", tabCount, "lastTabNames", tabNames.toString(),
                "tooltipCalls", tooltipCalls, "lastTooltip", tooltipX + "," + tooltipY,
                "lastTooltipCursor", tooltipCursorX + "," + tooltipCursorY,
                "cursorCalls", cursorCalls, "lastCursorScale", cursorScaleX + "," + cursorScaleY,
                "lastCursorExpectedScale", cursorExpectedScale, "nativeSortRequests", sortRequests()));
        result.put("nativeCreativeActions", java.util.List.copyOf(creativeActions));
        result.put("nativeNavigationCalls", java.util.List.copyOf(navigationCalls));
        result.put("nativeNavigationRequests", navigationRequests);
        result.put("nativeVirtualMouseLifecycleEvents", virtualMouseLifecycleEvents);
        result.put("nativeVirtualMouseLifecycle", java.util.List.copyOf(virtualMouseLifecycle));
        result.put("hostSdlMotionIsolation", Map.of("policy", "disposable-synthetic-controller-motion-only",
            "scope", "unfinished-run/current-installed-synthetic-controller/loaded-world-player",
            "nativeDirectPointerAndControllerUnchanged", true, "buttonsKeysWheelsForwarded", true,
            "blockedMotionEvents", isolatedHostMotionEvents, "events", java.util.List.copyOf(isolatedHostMotion)));
        result.put("nativeCategoryFrames", categoryFrames);
        result.put("nativeCategoryCount", categoryCount);
        result.put("nativeSelectedCategoryCount", selectedCategoryCount);
        result.put("nativeCreativeTabFrames", creativeTabFrames);
        result.put("nativeActiveCreativeTabCount", activeCreativeTabCount);
        result.put("nativeRecipePreviewFrames", recipePreviewFrames);
        result.put("nativeVanillaBookSpritesContextualEnabled", vanillaBookSpritesContextualEnabled);
        result.put("nativeVanillaBookSpritesLegacyOptOut", vanillaBookSpritesLegacyOptOut);
        result.put("lastNativeCursorGui", cursorGuiX + "," + cursorGuiY);
        result.put("unexpectedVanillaBookSprites", java.util.List.copyOf(unexpectedVanillaBookSprites));
        result.putAll(QaNativeGuideProbe.snapshot());
        result.putAll(QaVisualFeatureProbe.snapshot());
        result.putAll(QaContextualPresentationProbe.snapshot());
        result.putAll(QaHoverPacketProbe.snapshot());
        return Map.copyOf(result);
    }
}
