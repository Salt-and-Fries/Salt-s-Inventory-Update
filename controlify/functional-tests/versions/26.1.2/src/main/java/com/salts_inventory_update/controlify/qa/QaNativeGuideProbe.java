package com.salts_inventory_update.controlify.qa;

import com.salts_inventory_update.controlify.RadialInventoryScreen;
import com.salts_inventory_update.controlify.SaltsControlifyClient;
import dev.isxander.controlify.gui.guide.GuideInstanceImpl;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/** QA-only observations inside the pinned Controlify renderer, never substitutes guide rendering. */
public final class QaNativeGuideProbe {
    public record Draw(Component text, String label, boolean right, double left, double top, double rightEdge, double bottom) { }
    public record Frame(long sequence, String domain, String mode, boolean gameplay, boolean bottomAligned,
                        float parentY, int tuckedInset, boolean compact, List<Draw> draws) {
        public boolean label(String label) { return draws.stream().anyMatch(draw -> matches(draw, label)); }
        public boolean glyph(String label, Component expected) {
            return draws.stream().anyMatch(draw -> matches(draw, label) && contains(draw.text(), expected));
        }
        public boolean wholeDpad(String label) {
            return draws.stream().anyMatch(draw -> matches(draw, label) && nativeSymbols(draw.text()) == 1 && wholeDpadSymbols(draw.text()) == 1);
        }
        public boolean adjacentButtons(String label, int count) {
            return draws.stream().anyMatch(draw -> matches(draw, label) && nativeSymbols(draw.text()) == count && validNativeSpacing(draw.text()));
        }
        public boolean bothColumns() { return draws.stream().anyMatch(Draw::right) && draws.stream().anyMatch(draw -> !draw.right()); }
        public boolean horizontal() {
            return compact && draws.size() > 1 && java.util.stream.IntStream.range(1, draws.size())
                .anyMatch(i -> Math.abs(draws.get(i).top() - draws.get(i-1).top()) < .25 && draws.get(i).left() > draws.get(i-1).left());
        }
        public double top() { return draws.stream().mapToDouble(Draw::top).min().orElse(Double.POSITIVE_INFINITY); }
    }
    // Existing action checks retain their semantic identities while asserting the newly rendered short labels.
    private static final Map<String, String> SHORT_LABELS = Map.ofEntries(
        Map.entry("Close all windows", "Close all"), Map.entry("Close highlighted window", "Close"),
        Map.entry("Hold: window wheel", "Windows"), Map.entry("Release opening trigger", "Release"),
        Map.entry("Release: show highlighted window", "Show"), Map.entry("Release: tuck away", "Tuck"),
        Map.entry("Release: return to pair", "Return"), Map.entry("Focus highlighted window", "Focus"),
        Map.entry("Choose window / tuck away", "Choose"), Map.entry("Previous / next window", "Cycle"),
        Map.entry("Sort focused; double tap: force", "Sort/Force"), Map.entry("Move cursor", "Cursor"),
        Map.entry("Navigate slots / controls", "Navigate"), Map.entry("Navigate / scroll rows", "Navigate"),
        Map.entry("Scroll list", "Scroll"), Map.entry("Hold: recipe wheel", "Recipes"),
        Map.entry("Hold: cooking wheel", "Cooking"), Map.entry("Hold: creative tab wheel", "Tabs"),
        Map.entry("Craft into opposite window", "Craft"), Map.entry("Craft stack into opposite window", "Craft Stack"), Map.entry("Load cooking ingredients", "Load"),
        Map.entry("Select creative tab", "Select"), Map.entry("Add one to hotbar", "Add one"),
        Map.entry("Add stack to hotbar", "Add stack"), Map.entry("Browse recipes", "Browse"),
        Map.entry("Choose recipe", "Choose"), Map.entry("Hold: rotate recipes", "Rotate"),
        Map.entry("Change recipe category", "Category"), Map.entry("Toggle craftable recipes", "Filter"),
        Map.entry("Choose creative tab", "Choose"), Map.entry("Previous / next creative tab", "Cycle"),
        Map.entry("Previous window", "Previous"), Map.entry("Next window", "Next"),
        Map.entry("Hold: rotate counterclockwise", "Rotate left"), Map.entry("Hold: rotate clockwise", "Rotate right"),
        Map.entry("Restore windows", "Restore"), Map.entry("Quick Move", "Move"));
    private static boolean matches(Draw draw, String expected) {
        String actual = SHORT_LABELS.getOrDefault(expected, expected);
        return draw.label().isEmpty() ? draw.text().getString().contains(actual) : draw.label().equals(actual);
    }
    private static final List<Draw> current = new ArrayList<>();
    private static final Map<Component, Integer> rowHeights = new LinkedHashMap<>();
    private static final Map<String, Long> modeFrames = new LinkedHashMap<>();
    private static final List<String> unexpected = new ArrayList<>();
    public static long frames, workspaceFrames, gameplayFrames, rawPaneBookDraws, manualControlTextDraws;
    public static long compactGuideFrames, compactEntryDraws, compactNativeTextDraws;
    public static long wholeDpadEntries, adjacentButtonEntries;
    public static long compactLayoutFrames;
    public static int compactRows, compactReservedHeight, compactWidth, compactHeight;
    public static final List<String> compactViolations = new ArrayList<>();
    public static long tuckedTopFrames, tuckedBottomFrames, tuckedTransitionFrames;
    public static final List<String> overlapViolations = new ArrayList<>();
    public static final List<String> overlayOverlapViolations = new ArrayList<>();
    public static long overlayFrames;
    public static double overlayBottom;
    private static boolean overlayScope;
    private static String overlayMode = "";
    public static Frame workspace, gameplay;
    public static double tabsBottom;
    public static int layoutWidth, layoutHeight;
    public static long layoutFrames;
    private static boolean extracting, right, gameplayScope, compactScope;
    private static String domain, mode;
    private static boolean bottom;
    private static float parentY;
    private static int inset;
    private static int settledInset;

    private QaNativeGuideProbe() { }
    public static void gameplay(boolean value) { gameplayScope = value; }
    public static void beginCompact(GuiGraphicsExtractor graphics, GuideInstanceImpl<?> guide) {
        begin(graphics, guide, true); compactScope = true;
    }
    public static void compactLayout(com.salts_inventory_update.controlify.model.CompactGuideLayout.Layout layout, int width, int height) {
        compactLayoutFrames++; compactRows = layout.rows(); compactReservedHeight = layout.reservedHeight();
        compactWidth = width; compactHeight = height;
        if ((layout.rows()>2 || layout.reservedHeight()>Math.ceil(height*.2) || layout.scale()<=0
                || layout.entries().stream().anyMatch(entry -> entry.x()<-.5 || entry.y()<-.5
                    || entry.x()+entry.width()>width+.5 || entry.y()+entry.height()>height+.5)) && compactViolations.size()<32)
            compactViolations.add("scenario="+QaFeatureProbe.scenario+",compact layout="+layout+",viewport="+width+","+height);
    }
    public static void compactEntry(GuiGraphicsExtractor graphics, Font font, Component glyph, Component label,
                                   int x, int y, int width, int height) {
        if (!extracting || !compactScope) return;
        var pose = graphics.pose();
        double left = pose.m00()*x + pose.m10()*y + pose.m20(), top = pose.m01()*x + pose.m11()*y + pose.m21();
        double rightEdge = pose.m00()*(x+width) + pose.m10()*(y+height) + pose.m20();
        double bottomEdge = pose.m01()*(x+width) + pose.m11()*(y+height) + pose.m21();
        String value = label.getString().strip();
        var draw = new Draw(Component.empty().append(glyph.copy()).append(" ").append(label.copy()), value, false,
            Math.min(left,rightEdge), Math.min(top,bottomEdge), Math.max(left,rightEdge), Math.max(top,bottomEdge));
        current.add(draw); compactEntryDraws++;
        if (wholeDpadSymbols(glyph) == 1) wholeDpadEntries++;
        if (nativeSymbols(glyph) >= 2 && validNativeSpacing(glyph)) adjacentButtonEntries++;
        if (!validNativeSpacing(glyph) && compactViolations.size() < 32)
            compactViolations.add("scenario=" + QaFeatureProbe.scenario + ",unsupported controller-font separator=" + glyph);
        if ((value.isEmpty() || value.split("\\s+").length > 2 || glyph.getString().isEmpty()
                || draw.left() < -.5 || draw.top() < -.5 || draw.rightEdge() > graphics.guiWidth()+.5
                || draw.bottom() > graphics.guiHeight()+.5) && compactViolations.size() < 32)
            compactViolations.add("scenario=" + QaFeatureProbe.scenario + ",entry=" + draw);
    }
    public static void begin(GuiGraphicsExtractor graphics, GuideInstanceImpl<?> guide, boolean bottomAligned) {
        try {
            var field = GuideInstanceImpl.class.getDeclaredField("domain"); field.setAccessible(true);
            Object value = field.get(guide);
            domain = value.getClass().getMethod("id").invoke(value).toString();
        } catch (ReflectiveOperationException failure) { throw new IllegalStateException("Observe actual native guide domain", failure); }
        var minecraft = Minecraft.getInstance();
        mode = minecraft.screen instanceof RadialInventoryScreen screen ? screen.controllerGuideState().mode().name() : "GAMEPLAY";
        bottom = bottomAligned; parentY = graphics.pose().m21();
        inset = pureTuckedReservation(minecraft.getWindow().getGuiScaledWidth(), minecraft.getWindow().getGuiScaledHeight());
        current.clear(); extracting = true;
    }
    public static void column(dev.isxander.controlify.gui.guide.PrecomputedLines lines, boolean rightAligned) {
        right = rightAligned; rowHeights.clear();
        for (var line : lines.lines()) rowHeights.put(line.text(), line.height());
    }
    public static void finish() {
        if (!extracting) return;
        extracting = false;
        var frame = new Frame(++frames, domain, mode, gameplayScope, bottom, parentY, inset, compactScope, List.copyOf(current));
        if (compactScope) compactGuideFrames++;
        if (compactScope && compactViolations.size()<32) for (int a=0;a<current.size();a++) for (int b=a+1;b<current.size();b++) {
            var first = current.get(a); var second = current.get(b);
            if (Math.min(first.rightEdge(),second.rightEdge())-Math.max(first.left(),second.left())>.5
                    && Math.min(first.bottom(),second.bottom())-Math.max(first.top(),second.top())>.5)
                compactViolations.add("scenario="+QaFeatureProbe.scenario+",overlapping compact entries="+first+","+second);
        }
        compactScope = false;
        if (domain.equals("salts_inventory_controlify:workspace")) {
            workspace = frame; workspaceFrames++; modeFrames.merge(mode, 1L, Long::sum);
            if ((mode.equals("RECIPE") || mode.equals("COOKING") || mode.equals("CREATIVE_TABS"))
                    && mode.equals(overlayMode) && frame.top() < overlayBottom && overlayOverlapViolations.size() < 32)
                overlayOverlapViolations.add("overlayBottom=" + overlayBottom + "," + frame);
        } else if (gameplayScope) {
            gameplay = frame; gameplayFrames++;
            if (inset > 0 && !bottom && frame.bothColumns()) {
                tuckedTopFrames++;
                if (parentY > settledInset + 1) tuckedTransitionFrames++;
                if ((parentY + .01 < inset || frame.top() < Math.max(inset, tabsBottom)) && overlapViolations.size() < 32)
                    overlapViolations.add(frame.toString());
            } else if (bottom && SaltsControlifyClient.tuckedWindowsAvailable()) tuckedBottomFrames++;
        }
    }
    public static void text(GuiGraphicsExtractor graphics, Font font, Component text, int x, int y) {
        if (overlayScope) surface(graphics, x, y, font.width(text), font.lineHeight);
        if (extracting && compactScope) {
            compactNativeTextDraws++;
            if (!validNativeSpacing(text) && compactViolations.size() < 32)
                compactViolations.add("scenario=" + QaFeatureProbe.scenario + ",actual native draw has unsupported separator=" + text);
            return;
        }
        if (extracting) {
            var pose = graphics.pose();
            float width = font.width(text), height = rowHeights.getOrDefault(text, font.lineHeight);
            double x1 = pose.m00()*x + pose.m10()*y + pose.m20();
            double y1 = pose.m01()*x + pose.m11()*y + pose.m21();
            double x2 = pose.m00()*(x+width) + pose.m10()*(y+height) + pose.m20();
            double y2 = pose.m01()*(x+width) + pose.m11()*(y+height) + pose.m21();
            current.add(new Draw(text.copy(), "", right, Math.min(x1,x2), Math.min(y1,y2), Math.max(x1,x2), Math.max(y1,y2)));
        } else if (Minecraft.getInstance().screen instanceof RadialInventoryScreen) {
            String value = text.getString();
            if (value.startsWith("Right stick:") || value.startsWith("Left stick:") || value.startsWith("LS:") || value.startsWith("RS:")
                    || value.startsWith("LS / RS") || value.startsWith("D-pad") || value.startsWith("Hold LB")
                    || value.startsWith("A:") || value.equals("One recipe per A press")
                    || value.startsWith("Release ") && value.contains("return")) {
                manualControlTextDraws++; unexpected("manual=" + value);
            }
        }
    }
    public static void rawBook(Identifier texture) {
        if (!(Minecraft.getInstance().screen instanceof RadialInventoryScreen)) return;
        String id = texture.toString();
        if (id.equals("salts_inventory_controlify:textures/gui/contextual/recipe_book.png")
                || id.equals("salts_inventory_controlify:textures/gui/contextual/recipe_book_disabled.png")
                || id.equals("salts_inventory_controlify:generated/contextual_recipe_book_grayscale")
                || id.equals("minecraft:textures/gui/sprites/recipe_book/button.png")) {
            rawPaneBookDraws++; unexpected("paneBook=" + id);
        }
    }
    public static void beginTabs() { tabsBottom = 0; }
    @SuppressWarnings("unchecked")
    private static int pureTuckedReservation(int width, int height) {
        settledInset = 0;
        if (!SaltsControlifyClient.tuckedWindowsAvailable()) return 0;
        try {
            Object owner = read(SaltsControlifyClient.class, "instance");
            var windows = (Map<java.util.UUID, com.salts_inventory_update.api.client.desktop.DesktopWorkspaceWindow>) read(owner, "windows");
            var method = Class.forName("com.salts_inventory_update.controlify.TuckedWindowTabs")
                .getDeclaredMethod("reservedBottom", int.class, Map.class);
            method.setAccessible(true);
            double tabBottom = ((Number) method.invoke(null, width, windows)).doubleValue();
            settledInset = com.salts_inventory_update.controlify.model.TuckedGuideReservation.inset(width, height, tabBottom, null, Map.of());
            com.salts_inventory_update.controlify.model.WorkspaceLayout<?> preview = null;
            var sizes = new LinkedHashMap<java.util.UUID, com.salts_inventory_update.controlify.model.WindowSize>();
            // Read the last geometry actually extracted by the production transition. Never
            // call tuckedGuideInset/animatedLayout: those sample and advance its animator.
            long tuckedAt = ((Number) read(owner, "tuckedAt")).longValue();
            var config = (com.salts_inventory_update.controlify.config.ControlifyConfig) read(owner, "config");
            if (config.animationsEnabled() && System.nanoTime() - tuckedAt <= 260_000_000L
                    && ((Number) read(owner, "lastTuckedPreviewWidth")).intValue() == width
                    && ((Number) read(owner, "lastTuckedPreviewHeight")).intValue() == height) {
                preview = (com.salts_inventory_update.controlify.model.WorkspaceLayout<?>) read(owner, "lastTuckedPreviewFrame");
                var desktop = (com.salts_inventory_update.api.client.desktop.DesktopWorkspace) read(owner, "desktop");
                for (var entry : windows.entrySet()) if (entry.getValue().handle().isOpen()) {
                    var bounds = desktop.presentationBounds(entry.getKey());
                    sizes.put(entry.getKey(), new com.salts_inventory_update.controlify.model.WindowSize(Math.max(1,bounds.width()), Math.max(1,bounds.height())));
                }
            }
            return com.salts_inventory_update.controlify.model.TuckedGuideReservation.inset(width, height, tabBottom, preview, sizes);
        } catch (ReflectiveOperationException failure) { throw new IllegalStateException("Observe last drawn tucked preview geometry", failure); }
    }
    private static Object read(Object target, String name) throws ReflectiveOperationException {
        Class<?> type = target instanceof Class<?> clazz ? clazz : target.getClass();
        var field = type.getDeclaredField(name); field.setAccessible(true);
        return field.get(target instanceof Class<?> ? null : target);
    }
    public static void overlayBegin() {
        overlayScope = true; overlayBottom = 0;
        overlayMode = Minecraft.getInstance().screen instanceof RadialInventoryScreen screen ? screen.controllerGuideState().mode().name() : "";
    }
    public static void overlayEnd() { overlayScope = false; overlayFrames++; }
    public static void surface(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
        if (!overlayScope) return;
        var pose = graphics.pose();
        // The emerging panels are clipped by the actual overlay content-area scissor.
        overlayBottom = Math.max(overlayBottom, QaContextualPresentationProbe.clippedBottom(Math.max(pose.m01()*x + pose.m11()*y + pose.m21(),
            pose.m01()*(x+width) + pose.m11()*(y+height) + pose.m21())));
    }
    public static void tab(GuiGraphicsExtractor graphics, int width, int height) {
        var pose = graphics.pose();
        tabsBottom = Math.max(tabsBottom, Math.max(pose.m21(), pose.m01()*width + pose.m11()*height + pose.m21()));
    }
    private static boolean controllerFont(net.minecraft.network.chat.Style style) {
        return style.getFont().toString().contains("controller/");
    }
    private static int nativeSymbols(Component text) {
        int[] symbols = {0};
        text.visit((style, value) -> {
            if (controllerFont(style)) symbols[0] += value.codePointCount(0, value.length());
            return java.util.Optional.empty();
        }, net.minecraft.network.chat.Style.EMPTY);
        return symbols[0];
    }
    private static int wholeDpadSymbols(Component text) {
        int[] symbols = {0};
        text.visit((style, value) -> {
            if ("salts_inventory_controlify:whole_dpad".equals(style.getInsertion()))
                symbols[0] += value.codePointCount(0, value.length());
            return java.util.Optional.empty();
        }, net.minecraft.network.chat.Style.EMPTY);
        return symbols[0];
    }
    private static boolean validNativeSpacing(Component text) {
        boolean[] valid = {true};
        text.visit((style, value) -> {
            if (controllerFont(style) && (value.indexOf('+') >= 0 || value.indexOf(' ') >= 0)) valid[0] = false;
            return java.util.Optional.empty();
        }, net.minecraft.network.chat.Style.EMPTY);
        return valid[0];
    }
    private static boolean contains(Component actual, Component expected) {
        if (actual.equals(expected)) return true;
        if (!expected.getString().isEmpty() && actual.getString().equals(expected.getString())
                && actual.getStyle().getFont().equals(expected.getStyle().getFont())) return true;
        return actual.getSiblings().stream().anyMatch(sibling -> contains(sibling, expected));
    }
    private static void unexpected(String value) { if (unexpected.size() < 32) unexpected.add("scenario=" + QaFeatureProbe.scenario + "," + value); }
    public static Map<String, Object> snapshot() {
        var result = new LinkedHashMap<String,Object>();
        result.put("nativeWorkspaceGuideFrames", workspaceFrames); result.put("nativeGameplayGuideFrames", gameplayFrames);
        result.put("nativeWorkspaceGuideModeFrames", Map.copyOf(modeFrames)); result.put("removedPaneBookDraws", rawPaneBookDraws);
        result.put("compactGuideFrames", compactGuideFrames); result.put("compactGuideEntryDraws", compactEntryDraws);
        result.put("compactGuideNativeTextDraws", compactNativeTextDraws);
        result.put("nativeWholeDpadGuideEntries", wholeDpadEntries);
        result.put("nativeAdjacentButtonGuideEntries", adjacentButtonEntries); result.put("compactGuideViolations", List.copyOf(compactViolations));
        result.put("compactGuideLayoutFrames",compactLayoutFrames); result.put("compactGuideLastRows",compactRows);
        result.put("compactGuideReservedHeight",compactReservedHeight); result.put("compactGuideViewport",compactWidth+","+compactHeight);
        result.put("nativeTuckedTopGuideFrames", tuckedTopFrames); result.put("nativeTuckedBottomGuideFrames", tuckedBottomFrames);
        result.put("nativeTuckedTransitionGuideFrames", tuckedTransitionFrames); result.put("nativeTuckedGuideOverlapViolations", List.copyOf(overlapViolations));
        result.put("nativeContextualOverlayFrames", overlayFrames); result.put("nativeContextualGuideOverlapViolations", List.copyOf(overlayOverlapViolations));
        result.put("nativeLayoutViewportFrames", layoutFrames); result.put("nativeLayoutContentViewport", layoutWidth + "," + layoutHeight);
        result.put("manualControlTextDraws", manualControlTextDraws); result.put("unexpectedRemovedControlDraws", List.copyOf(unexpected));
        result.put("lastWorkspaceGuide", workspace == null ? "none" : workspace.toString());
        result.put("lastGameplayGuide", gameplay == null ? "none" : gameplay.toString());
        return result;
    }
}
