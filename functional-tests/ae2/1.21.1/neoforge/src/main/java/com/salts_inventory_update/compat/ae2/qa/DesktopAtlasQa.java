package com.salts_inventory_update.compat.ae2.qa;

import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.AESubScreen;
import appeng.client.gui.implementations.*;
import appeng.client.gui.me.crafting.*;
import appeng.client.gui.me.items.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import org.slf4j.LoggerFactory;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import appeng.api.config.Actionable;
import appeng.api.ids.AEComponents;
import appeng.api.parts.PartHelper;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.storage.StorageCells;
import appeng.core.definitions.AEBlocks;
import appeng.core.definitions.AEItems;
import appeng.core.definitions.AEParts;
import appeng.me.helpers.BaseActionSource;
import appeng.menu.MenuOpener;
import appeng.menu.implementations.CellWorkbenchMenu;
import appeng.menu.implementations.CondenserMenu;
import appeng.menu.implementations.DriveMenu;
import appeng.menu.implementations.EnergyLevelEmitterMenu;
import appeng.menu.implementations.FormationPlaneMenu;
import appeng.menu.implementations.IOBusMenu;
import appeng.menu.implementations.IOPortMenu;
import appeng.menu.implementations.InscriberMenu;
import appeng.menu.implementations.InterfaceMenu;
import appeng.menu.implementations.MEChestMenu;
import appeng.menu.implementations.MolecularAssemblerMenu;
import appeng.menu.implementations.PatternAccessTermMenu;
import appeng.menu.implementations.PatternProviderMenu;
import appeng.menu.implementations.PriorityMenu;
import appeng.menu.implementations.QNBMenu;
import appeng.menu.implementations.QuartzKnifeMenu;
import appeng.menu.implementations.SetStockAmountMenu;
import appeng.menu.implementations.SkyChestMenu;
import appeng.menu.implementations.SpatialAnchorMenu;
import appeng.menu.implementations.SpatialIOPortMenu;
import appeng.menu.implementations.StorageBusMenu;
import appeng.menu.implementations.StorageLevelEmitterMenu;
import appeng.menu.implementations.VibrationChamberMenu;
import appeng.menu.implementations.WirelessAccessPointMenu;
import appeng.menu.locator.MenuHostLocator;
import appeng.menu.locator.MenuLocators;
import appeng.menu.me.common.MEStorageMenu;
import appeng.menu.me.crafting.CraftAmountMenu;
import appeng.menu.me.crafting.CraftConfirmMenu;
import appeng.menu.me.crafting.CraftingCPUMenu;
import appeng.menu.me.crafting.CraftingStatusMenu;
import appeng.menu.me.items.BasicCellChestMenu;
import appeng.menu.me.items.CraftingTermMenu;
import appeng.menu.me.items.PatternEncodingTermMenu;
import appeng.menu.me.items.WirelessCraftingTermMenu;
import appeng.menu.me.networktool.NetworkStatusMenu;
import appeng.menu.me.networktool.NetworkToolMenu;
import appeng.parts.AEBasePart;
import appeng.server.testworld.Plot;

/** Opt-in screenshot atlas; creates only disposable AE fixtures and uses normal menu openings. */
public final class DesktopAtlasQa {
    private static Object nativeCall(Object target, String name, Class<?>[] types, Object... args) {
        for (Class<?> type = target.getClass(); type != null; type = type.getSuperclass()) {
            try {
                var method = type.getDeclaredMethod(name, types); method.setAccessible(true);
                return method.invoke(target, args);
            } catch (NoSuchMethodException next) { }
            catch (ReflectiveOperationException failure) { throw new IllegalStateException(failure); }
        }
        throw new IllegalStateException("Missing native AE2 QA method " + name);
    }
    private static net.minecraft.client.renderer.Rect2i nativeBounds(AEBaseScreen<?> screen, boolean absolute) {
        return (net.minecraft.client.renderer.Rect2i) nativeCall(screen, "getBounds", new Class<?>[]{boolean.class}, absolute);
    }
    @SuppressWarnings("unchecked")
    private static List<appeng.client.gui.widgets.VerticalButtonBar> nativeToolbars(AEBaseScreen<?> screen) {
        try {
            var field = AEBaseScreen.class.getDeclaredField("widgets"); field.setAccessible(true);
            return (List<appeng.client.gui.widgets.VerticalButtonBar>) nativeCall(field.get(screen), "desktopToolbars", new Class<?>[0]);
        } catch (ReflectiveOperationException failure) { throw new IllegalStateException(failure); }
    }
    private record Entry(String label, MenuType<?> type, Consumer<ServerPlayer> open) {
    }

    private static final List<Entry> ENTRIES = new ArrayList<>();
    private static final AtomicBoolean READY = new AtomicBoolean();
    private static final BlockPos ORIGIN = new BlockPos(0, 80, 0);
    private static final Path OUTPUT = Path.of(System.getProperty("ae2.desktop.qa.output", java.nio.file.Path.of(System.getenv("SIU_QA_OUTPUT"), "atlas").toString()));
    private static int stage;
    private static int ticks;
    private static int index;
    private static long screenshotStart;
    private static DesktopQaHooks.Window captured;

    private record Variant(String label, net.minecraft.client.gui.components.AbstractWidget button, Runnable action) {
        Variant(String label, net.minecraft.client.gui.components.AbstractWidget button) {
            this(label, button, null);
        }

        Variant(String label, Runnable action) {
            this(label, null, action);
        }
    }

    private static List<Variant> variants = List.of();
    private static int variantIndex = -1;
    private static boolean basePrepared;

    public static void tick(Minecraft mc) {
        try {
            if (stage == 0) {
                stage = 1;
                Files.createDirectories(OUTPUT);
                Files.writeString(OUTPUT.resolve("manifest.csv"),
                        "index,label,menu,x,y,width,height,guiWidth,guiHeight,file,visualX,visualY,visualWidth,visualHeight,bodyX,bodyY,bodyWidth,bodyHeight\n");
                mc.options.guiScale().set(Integer.getInteger("ae2.desktop.qa.scale", 2));
                mc.resizeDisplay();
                mc.options.pauseOnLostFocus = false;
                mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);
                var id = mc.player.getUUID();
                mc.getSingleplayerServer()
                        .execute(() -> setup(mc.getSingleplayerServer().getPlayerList().getPlayer(id)));
                return;
            }
            int interval = index == 0 && stage < 4 ? 40 : Integer.getInteger("ae2.desktop.qa.interval", 40);
            if (!READY.get() || stage >= 100 || ++ticks < interval)
                return;
            ticks = 0;
            if (stage == 1) {
                mc.setScreen(null);
                KeyMapping.click(mc.options.keyInventory.getKey());
                stage = 2;
            } else if (stage == 2) {
                // Normal inventory routing establishes the optional desktop capability handshake.
                stage = 3;
            } else if (stage == 3) {
                if (index >= ENTRIES.size()) {
                    LoggerFactory.getLogger(DesktopAtlasQa.class).info("ATLAS COMPLETE: {} registered container images",
                            index);
                    stage = 100;
                    Files.writeString(OUTPUT.resolve("completed.txt"), "Registered containers: " + index + "\n");
                    if (Boolean.getBoolean("ae2.desktop.qa.companion")) {
                        throw new IllegalStateException("Companion interaction suite is a separate QA profile");
                    } else if (Boolean.getBoolean("ae2.desktop.qa.quit"))
                        mc.stop();
                    return;
                }
                DesktopQaHooks.closeAll.run();
                if (mc.screen instanceof AEBaseScreen<?>) {
                    mc.setScreen(null);
                }
                // A client close and an integrated-server menu opening have separate queues.
                // Wait for last-owner disposal before starting an isolated catalog fixture.
                stage = 31;
            } else if (stage == 31) {
                if (!DesktopQaHooks.WINDOWS.isEmpty() || !DesktopQaHooks.COMPANIONS.isEmpty()) {
                    return;
                }
                var entry = ENTRIES.get(index);
                var id = mc.player.getUUID();
                mc.getSingleplayerServer().execute(() -> {
                    try {
                        entry.open.accept(mc.getSingleplayerServer().getPlayerList().getPlayer(id));
                    } catch (Exception error) {
                        LoggerFactory.getLogger(DesktopAtlasQa.class).error("ATLAS OPEN FAILED {}", entry.label, error);
                        try { Files.writeString(OUTPUT.resolve("failed.txt"), "Opening " + entry.label + ": " + error); }
                        catch (java.io.IOException writeError) { error.addSuppressed(writeError); }
                    }
                });
                stage = 4;
            } else if (stage == 4) {
                var entry = ENTRIES.get(index);
                captured = DesktopQaHooks.WINDOWS.values().stream()
                        .filter(w -> w.content().getMenu().getType() == entry.type).findFirst().orElse(null);
                if (captured == null && mc.screen instanceof AEBaseScreen<?> ordinary) {
                    var bounds = nativeBounds(ordinary, true);
                    captured = new DesktopQaHooks.Window(bounds.getX(), bounds.getY(), bounds.getWidth(),
                            bounds.getHeight(), 0, 0, ordinary);
                }
                if (captured == null || captured.content().getMenu().getType() != entry.type) {
                    throw new IllegalStateException("No container view for " + entry.label);
                }
                if (captured.content().hasDesktopSharedToolbox()
                        && !DesktopQaHooks.COMPANION_READY
                                .contains(toolboxContent(captured.content()).getMenu().desktopSyncId())) {
                    return;
                }
                if (captured.content().isDesktopHosted() && variantIndex < 0 && !basePrepared) {
                    basePrepared = true;
                    for (var child : List.copyOf(captured.content().children())) {
                        if (child instanceof appeng.client.gui.widgets.TabButton tab && tab.visible && tab.isSelected()
                                && List.of("Upgrades", "View Cells", "Toolbox")
                                        .contains(tab.getMessage().getString())) {
                            click(mc, tab.getX() + 5, tab.getY() + 5);
                        }
                    }
                    return;
                }
                if (!captured.content().isDesktopHosted())
                    throw new IllegalStateException("Atlas requires an API-hosted desktop window for " + entry.label);
                if (variantIndex < 0 && "true".equals(System.getenv("SIU_AE2_CPU_LIFECYCLE_QA"))
                        && captured.content() instanceof CraftingCPUScreen<?> cpu
                        && !NativeCpuLifecycleQa.verify(mc, cpu, ORIGIN, OUTPUT, (x, y) -> click(mc, x, y),
                                nativeCpu -> nativeCpu.craftingLogic.setJobSuspended(true), () -> { }))
                    return;
                verifyCapture(mc, captured);
                screenshotStart = System.currentTimeMillis();
                mc.gui.getChat().clearMessages(true);
                Screenshot.grab(mc.gameDirectory, mc.getMainRenderTarget(), mc.gui.getChat()::addMessage);
                stage = 5;
            } else if (stage == 5) {
                var entry = ENTRIES.get(index);
                Path latest;
                try (var stream = Files.list(Path.of("screenshots"))) {
                    latest = stream.filter(p -> p.toString().endsWith(".png"))
                            .filter(p -> modified(p) >= screenshotStart)
                            .max(Comparator.comparingLong(DesktopAtlasQa::modified)).orElseThrow();
                }
                var label = entry.label + (variantIndex < 0 ? "" : "-" + variants.get(variantIndex).label);
                var filename = String.format("%02d-%s.png", index + 1, label);
                Files.copy(latest, OUTPUT.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
                Files.writeString(OUTPUT.resolve(filename.replace(".png", ".geometry.json")),
                        appeng.client.gui.style.ScreenStyle.GSON.toJson(captured.content().desktopFidelityGeometry()));
                Files.writeString(OUTPUT.resolve(filename.replace(".png", ".controls.json")),
                        appeng.client.gui.style.ScreenStyle.GSON.toJson(DesktopQaHooks.MANAGEMENT_CONTROLS
                                .getOrDefault(captured.content().getMenu().desktopSyncId(), List.of())));
                var visual = visualBounds(captured);
                var body = captured.content().isDesktopHosted() ? captured.content().desktopContentBounds()
                        : nativeBounds(captured.content(), true);
                Files.writeString(OUTPUT.resolve("manifest.csv"),
                        String.format("%d,%s,%s,%d,%d,%d,%d,%d,%d,%s,%d,%d,%d,%d,%d,%d,%d,%d\n",
                                index + 1, label, BuiltInRegistries.MENU.getKey(entry.type), captured.x(), captured.y(),
                                captured.width(), captured.height(), mc.getWindow().getGuiScaledWidth(),
                                mc.getWindow().getGuiScaledHeight(), filename,
                                visual.getX(), visual.getY(), visual.getWidth(), visual.getHeight(),
                                body.getX(), body.getY(), body.getWidth(), body.getHeight()),
                        StandardOpenOption.APPEND);
                LoggerFactory.getLogger(DesktopAtlasQa.class).info("ATLAS CAPTURE {} {}", index + 1, label);
                if (variantIndex < 0 && Boolean.getBoolean("ae2.desktop.qa.variants")) {
                    variants = variants(captured);
                }
                variantIndex++;
                if (variantIndex < variants.size()) {
                    if (variants.get(variantIndex).action != null) {
                        variants.get(variantIndex).action.run();
                        if (variants.get(variantIndex).button == null) {
                            stage = 4;
                            return;
                        }
                    }
                    var button = variants.get(variantIndex).button;
                    if (variants.get(variantIndex).label.startsWith("panel-")
                            && button instanceof appeng.client.gui.widgets.TabButton tab && tab.isSelected()) {
                        stage = 4;
                        return;
                    }
                    mc.screen.mouseClicked(button.getX() + 5, button.getY() + 5, 0);
                    mc.screen.mouseReleased(button.getX() + 5, button.getY() + 5, 0);
                    stage = 4;
                    return;
                }
                variants = List.of();
                variantIndex = -1;
                index++;
                basePrepared = false;
                stage = 3;
            }
        } catch (Exception error) {
            LoggerFactory.getLogger(DesktopAtlasQa.class).error("ATLAS FAILED at {} stage {}", index, stage, error);
            Screenshot.grab(mc.gameDirectory, mc.getMainRenderTarget(), mc.gui.getChat()::addMessage);
            stage = 100;
            try {
                Files.writeString(OUTPUT.resolve("failed.txt"), error.toString());
            } catch (java.io.IOException writeError) {
                error.addSuppressed(writeError);
            }
            if (Boolean.getBoolean("ae2.desktop.qa.quit"))
                mc.stop();
        }
    }

    private static net.minecraft.client.renderer.Rect2i visualBounds(DesktopQaHooks.Window window) {
        int x = window.x(), y = window.y(), right = x + window.width(), bottom = y + window.height();
        var regions = new ArrayList<>(window.content().desktopExternalBounds());
        var ownerSession = DesktopQaHooks.SESSION_IDS.get(window.content().getMenu().desktopSyncId());
        if (ownerSession != null) {
            DesktopQaHooks.COMPANIONS.forEach((id, companion) -> {
                if (DesktopQaHooks.COMPANION_OWNERS.getOrDefault(id, List.of()).contains(ownerSession)) {
                    regions.add(new net.minecraft.client.renderer.Rect2i(companion.x(), companion.y(),
                            companion.width(), companion.height()));
                }
            });
        }
        if (!window.content().isDesktopHosted()) {
            for (var child : window.content().children()) {
                if (child instanceof net.minecraft.client.gui.components.AbstractWidget button && button.visible) {
                    regions.add(new net.minecraft.client.renderer.Rect2i(button.getX(), button.getY(),
                            button.getWidth(), button.getHeight()));
                }
            }
            for (var slot : window.content().getMenu().slots) {
                if (slot.isActive() && slot.x >= 0
                        && (!(slot instanceof appeng.menu.slot.AppEngSlot ae) || ae.isSlotEnabled())) {
                    regions.add(new net.minecraft.client.renderer.Rect2i(window.x() + slot.x - 2,
                            window.y() + slot.y - 2, 20, 20));
                }
            }
        }
        for (var region : regions) {
            x = Math.min(x, region.getX());
            y = Math.min(y, region.getY());
            right = Math.max(right, region.getX() + region.getWidth());
            bottom = Math.max(bottom, region.getY() + region.getHeight());
        }
        return new net.minecraft.client.renderer.Rect2i(x - 3, y - 3, right - x + 6, bottom - y + 6);
    }

    private static void verifyCapture(Minecraft mc, DesktopQaHooks.Window window) {
        var content = window.content();
        var body = content.desktopContentBounds();
        var external = content.desktopExternalBounds();
        var geometry = content.desktopFidelityGeometry();
        var title = geometry.get("text/dialog_title");
        var controls = DesktopQaHooks.MANAGEMENT_CONTROLS.getOrDefault(content.getMenu().desktopSyncId(), List.of());
        var kinds = controls.stream().map(DesktopQaHooks.Control::kind).toList();
        if (content.usesAeDesktopLayout()) {
            for (var toolbar : nativeToolbars(content)) {
                var columns = toolbar.desktopBackingColumns();
                for (var button : toolbar.buttons()) {
                    if (!button.visible)
                        continue;
                    var paint = appeng.client.gui.widgets.VerticalButtonBar.desktopPaintBounds(button);
                    int px = paint.getX() - content.getGuiLeft(), py = paint.getY() - content.getGuiTop();
                    if (columns.stream().noneMatch(column -> px >= column.getX() + 3
                            && px + paint.getWidth() <= column.getX() + column.getWidth() - 3
                            && py >= column.getY() + 3
                            && py + paint.getHeight() <= column.getY() + column.getHeight() - 4)) {
                        throw new IllegalStateException("Toolbar backing does not surround its complete hovered bevel");
                    }
                }
                for (int c = 1; c < columns.size(); c++) {
                    if (columns.get(c).getX() + columns.get(c).getWidth() != columns.get(c - 1).getX())
                        throw new IllegalStateException("Toolbar columns must form a connected outline");
                }
            }
            if (content instanceof appeng.client.gui.implementations.WirelessAccessPointScreen) {
                var position = content.getStyle().getWidget("linkPanel").resolve(nativeBounds(content, false));
                if (body.getY() + position.getY()
                        + content.getStyle().getImage("linkPanel").getSrcHeight() != body.getY() + body.getHeight())
                    throw new IllegalStateException(
                            "Wireless Access Point housing must share the main footer baseline");
            }
        }
        if (variantIndex < 0 && content instanceof appeng.client.gui.me.crafting.CraftingCPUScreen<?> cpu
                && !cpu.getMenu().cpuStatusReady) {
            throw new IllegalStateException("Initial crafting CPU state did not arrive through the desktop session");
        }
        if (content instanceof appeng.client.gui.me.crafting.CraftingStatusScreen statusScreen) {
            var list = statusScreen.cpuSelectionBounds();
            boolean narrow = mc.getWindow().getGuiScaledWidth() < 400;
            if (narrow && (list.getX() < 0 || list.getY() < 18
                    || list.getX() + list.getWidth() > body.getWidth() || list.getY() + list.getHeight() > 84)) {
                throw new IllegalStateException("Narrow CPU selector must fit above the job table");
            }
            var heading = content.getStyle().getText().get("cpuListTitle");
            var pos = heading.getPosition().resolve(nativeBounds(content, false));
            var headingBounds = new net.minecraft.client.renderer.Rect2i(content.getGuiLeft() + pos.getX(),
                    content.getGuiTop() + pos.getY(), mc.font.width(heading.getText()), mc.font.lineHeight);
            for (var widget : content.children()) {
                if (widget instanceof appeng.client.gui.widgets.OpenGuideButton help
                        && help.getX() < headingBounds.getX() + headingBounds.getWidth()
                        && help.getX() + help.getWidth() > headingBounds.getX()
                        && help.getY() < headingBounds.getY() + headingBounds.getHeight()
                        && help.getY() + help.getHeight() > headingBounds.getY()) {
                    throw new IllegalStateException("Help must leave the complete CPU heading clear");
                }
            }
        }
        if (content instanceof appeng.client.gui.me.items.PatternEncodingTermScreen<?>) {
            for (var child : content.children()) {
                if (child instanceof appeng.client.gui.widgets.TabButton tab && tab.visible
                        && tab.getWidth() == 22 && tab.getX() - content.getGuiLeft() == 173
                        && tab.getY() + tab.getHeight() > body.getY() + body.getHeight()) {
                    throw new IllegalStateException("Encoding tab ends beyond its final footer");
                }
            }
        }
        if (content instanceof appeng.client.gui.me.common.MEStorageScreen<?>) {
            for (var slot : content.getMenu().getSlots(appeng.menu.SlotSemantics.UPGRADE)) {
                if (content.desktopSlotVisible(slot) && slot.y + 16 + 5 > body.getHeight())
                    throw new IllegalStateException("Upgrade housing extends below the catalog minimum height");
            }
        }
        if (content instanceof appeng.client.gui.implementations.DriveScreen
                && controls.stream().anyMatch(control -> control.y() - content.getGuiTop() != 4)) {
            throw new IllegalStateException("Drive controls need the standard top gutter above the complete grid");
        }
        if (content instanceof appeng.client.gui.me.crafting.CraftErrorScreen
                && (content.desktopManagementTitle().getString().isEmpty() || title == null || title[2] <= 0)) {
            throw new IllegalStateException("Crafting errors must retain a painted and managed title");
        }
        if (kinds.contains("ELLIPSIS")) {
            if (!kinds.equals(List.of("ELLIPSIS", "CLOSE")))
                throw new IllegalStateException("Compact controls must be exactly More, Close: " + kinds);
        } else if (!kinds.isEmpty()) {
            var order = List.of("SORT_WINDOWS", "FOCUS", "PIN", "LOCK", "LINK", "MINIMIZE", "CLOSE");
            if (!order.stream().filter(kinds::contains).toList().equals(kinds)
                    || !kinds.containsAll(List.of("FOCUS", "PIN", "LOCK", "LINK", "CLOSE")))
                throw new IllegalStateException(
                        "Expanded controls must be the complete permitted native row: " + kinds);
        }
        for (int index = 0; index < controls.size(); index++) {
            var control = controls.get(index);
            if (control.width() != 11 || control.height() != 11
                    || index > 0 && (control.x() - controls.get(index - 1).x() != 14
                            || control.y() != controls.get(index - 1).y()))
                throw new IllegalStateException("Controls must retain original dimensions and three-pixel gaps");
            if (title != null && index == 0 && title[0] + title[2] + 6 > control.x() - content.getGuiLeft())
                throw new IllegalStateException("Title lacks its six-pixel control separation");
            for (var slot : content.desktopSlots()) {
                int x = content.getGuiLeft() + slot.x - 1, y = content.getGuiTop() + slot.y - 1;
                if (content.desktopSlotVisible(slot) && control.x() < x + 18
                        && control.x() + control.width() > x && control.y() < y + 18
                        && control.y() + control.height() > y) {
                    throw new IllegalStateException("Window control covers native slot " + slot.index);
                }
            }
            if (content instanceof appeng.client.gui.me.crafting.CraftErrorScreen
                    && control.y() + control.height() > content.getGuiTop()
                            + content.getStyle().getText().get("errorText").getPosition()
                                    .resolve(nativeBounds(content, false)).getY()) {
                throw new IllegalStateException("Window control crosses the error message row");
            }
        }
        if (content.hasDesktopSharedToolbox()) {
            var companion = toolboxContent(content);
            if (content.getMenu().getSlots(appeng.menu.SlotSemantics.TOOLBOX).stream()
                    .anyMatch(content::desktopSlotVisible)
                    || companion.getMenu().getSlots(appeng.menu.SlotSemantics.STORAGE).size() != 9) {
                throw new IllegalStateException("Toolbox must be shown exactly once through its real companion menu");
            }
            for (var slot : companion.getMenu().getSlots(appeng.menu.SlotSemantics.STORAGE)) {
                if (!companion.desktopSlotVisible(slot) || slot.x < 4 || slot.y < 18
                        || slot.x + 16 > companion.desktopContentBounds().getWidth() - 4
                        || slot.y + 16 > companion.desktopContentBounds().getHeight() - 4) {
                    throw new IllegalStateException("Companion slot crosses its native frame");
                }
            }
        }
        if (content.isDesktopHosted() && title != null) {
            for (var widget : content.children()) {
                if (widget instanceof net.minecraft.client.gui.components.EditBox field && field.visible) {
                    int x = field.getX() - content.getGuiLeft();
                    int y = field.getY() - content.getGuiTop();
                    if (title[0] < x + field.getWidth() && title[0] + title[2] > x
                            && title[1] < y + field.getHeight() && title[1] + title[3] > y) {
                        throw new IllegalStateException("Title overlaps an editor in " + content.getMenu().getType());
                    }
                }
            }
        }
        for (var slot : content.desktopSlots()) {
            if (!content.desktopSlotVisible(slot)) {
                continue;
            }
            int x = content.getGuiLeft() + slot.x;
            int y = content.getGuiTop() + slot.y;
            if (content.desktopExternalSlot(slot)) {
                if (external.stream().noneMatch(r -> x >= r.getX() && y >= r.getY()
                        && x + 16 <= r.getX() + r.getWidth() && y + 16 <= r.getY() + r.getHeight())) {
                    throw new IllegalStateException("Visible attachment slot has no external region: " + slot.index);
                }
            } else if (content.isDesktopHosted() && content.desktopInventoryCut() > 0
                    && y + 16 > body.getY() + body.getHeight() - 4) {
                throw new IllegalStateException("Closing border crosses visible slot artwork: " + slot.index);
            } else if (x < body.getX() || y < body.getY() || x + 16 > body.getX() + body.getWidth()
                    || y + 16 > body.getY() + body.getHeight()) {
                throw new IllegalStateException(
                        "Visible body slot crosses its clip: " + slot.index + " at " + slot.x + "," + slot.y);
            }
        }
        for (var rect : external) {
            if (rect.getX() < 0 || rect.getY() < 0
                    || rect.getX() + rect.getWidth() > mc.getWindow().getGuiScaledWidth()
                    || rect.getY() + rect.getHeight() > mc.getWindow().getGuiScaledHeight()) {
                throw new IllegalStateException("External control is outside the desktop: " + rect.getX() + ","
                        + rect.getY() + " " + rect.getWidth() + "x" + rect.getHeight());
            }
        }
        if (variantIndex >= 0) {
            var variant = variants.get(variantIndex);
            if (variant.label.equals("network-large-scroll")
                    && content instanceof appeng.client.gui.me.networktool.NetworkStatusScreen network
                    && network.desktopMachineScroll() != 1) {
                throw new IllegalStateException("Wider network cells must still reach every device group");
            }
            if (variant.label.startsWith("status-")
                    && content instanceof appeng.client.gui.me.crafting.CraftingCPUScreen<?> cpu) {
                String expected = switch (variant.label) {
                    case "status-waiting" -> "waitingCraftingStatus";
                    case "status-unavailable" -> "noCraftingCpu";
                    case "status-active" -> "";
                    default -> "noCraftingJob";
                };
                if (!cpu.desktopCraftingMessageKey().equals(expected))
                    throw new IllegalStateException(
                            "Crafting state message does not describe its synchronized fields: " + variant.label);
            }
            if (variant.label.equals("cpu-select-last") && content.getMenu() instanceof CraftingStatusMenu status
                    && status.getSelectedCpuSerial() != status.cpuList.cpus().getLast().serial()) {
                throw new IllegalStateException("The final CPU row is not reachable through the visible selector");
            }
            if (variant.label.equals("cpu-return-first") && content.getMenu() instanceof CraftingStatusMenu status
                    && status.getSelectedCpuSerial() != status.cpuList.cpus().getFirst().serial()) {
                throw new IllegalStateException("CPU selection cannot return to the first row");
            }
            if (variant.label.equals("readable-scroll")
                    && content instanceof appeng.client.gui.me.crafting.CraftConfirmScreen plan
                    && plan.desktopPlanScroll() != 2) {
                throw new IllegalStateException("Readable plan rows must still reach the final entries");
            }
            if (variant.label.endsWith("-minimized")
                    && !DesktopQaHooks.windowSummary.apply(content.getMenu().desktopSyncId())
                            .contains("minimized=true")) {
                throw new IllegalStateException("Error view did not minimize through its permitted manager action");
            }
            if (variant.label.endsWith("-restored")
                    && DesktopQaHooks.windowSummary.apply(content.getMenu().desktopSyncId())
                            .contains("minimized=true")) {
                throw new IllegalStateException("Error view did not restore through its permitted manager action");
            }
            if (variant.label.equals("toolbox-return")) {
                var slot = toolboxContent(content).getMenu().getSlots(content.hasDesktopSharedToolbox()
                        ? appeng.menu.SlotSemantics.STORAGE
                        : appeng.menu.SlotSemantics.TOOLBOX).getFirst();
                if (!mc.player.inventoryMenu.getCarried().isEmpty()
                        || !slot.getItem().is(AEItems.CAPACITY_CARD.asItem())
                        || slot.getItem().getCount() != 4) {
                    throw new IllegalStateException(
                            "Toolbox return did not conserve its four cards and clear the shared cursor");
                }
            }
            if (variant.button instanceof appeng.client.gui.widgets.TabButton tab && !tab.isSelected()) {
                throw new IllegalStateException("Native click did not select " + variant.label);
            }
            if (variant.label.equals("settings")
                    && !(content instanceof appeng.client.gui.me.common.TerminalSettingsScreen<?>)) {
                throw new IllegalStateException("Native settings click did not open its local view");
            }
        }
        LoggerFactory.getLogger(DesktopAtlasQa.class).info("ATLAS GEOMETRY VERIFIED {} {}",
                content.getMenu().getType(), variantIndex < 0 ? "base" : variants.get(variantIndex).label);
    }

    private static List<Variant> variants(DesktopQaHooks.Window window) {
        var result = new ArrayList<Variant>();
        if (Boolean.getBoolean("ae2.desktop.qa.visualFixes")
                && window.content() instanceof appeng.client.gui.me.networktool.NetworkStatusScreen network) {
            var fixture = largeNetworkFixture(Minecraft.getInstance());
            var id = network.getMenu().desktopSyncId();
            result.add(new Variant("network-large-counts", () -> DesktopQaHooks.VISUAL_FIXTURES.put(id,
                    () -> network.processServerUpdate(fixture))));
            result.add(new Variant("network-large-scroll", () -> Minecraft.getInstance().screen.mouseScrolled(
                    network.getGuiLeft() + 40, network.getGuiTop() + 75, 0, -1)));
        }
        if (Boolean.getBoolean("ae2.desktop.qa.visualFixes")
                && window.content() instanceof appeng.client.gui.me.crafting.CraftingStatusScreen status) {
            result.add(new Variant("cpu-scroll-last", () -> {
                var rect = status.cpuSelectionBounds();
                for (int step = 0; step < status.getMenu().cpuList.cpus().size(); step++) {
                    Minecraft.getInstance().screen.mouseScrolled(status.getGuiLeft() + rect.getX() + 10,
                            status.getGuiTop() + rect.getY() + 25, 0, -1);
                }
            }));
            result.add(new Variant("cpu-select-last", () -> {
                var rect = status.cpuSelectionBounds();
                int count = Math.min(status.cpuVisibleRows(), status.getMenu().cpuList.cpus().size());
                int rowOffset = Minecraft.getInstance().getWindow().getGuiScaledWidth() < 400 ? 0 : 19;
                click(Minecraft.getInstance(), status.getGuiLeft() + rect.getX() + 15,
                        status.getGuiTop() + rect.getY() + rowOffset + (count - 1) * 23 + 5);
            }));
            result.add(new Variant("cpu-return-first", () -> {
                var rect = status.cpuSelectionBounds();
                var mc = Minecraft.getInstance();
                for (int step = 0; step < status.getMenu().cpuList.cpus().size(); step++) {
                    mc.screen.mouseScrolled(status.getGuiLeft() + rect.getX() + 10,
                            status.getGuiTop() + rect.getY() + 25, 0, 1);
                }
                int rowOffset = mc.getWindow().getGuiScaledWidth() < 400 ? 0 : 19;
                click(mc, status.getGuiLeft() + rect.getX() + 15, status.getGuiTop() + rect.getY() + rowOffset + 5);
            }));
        }
        if (Boolean.getBoolean("ae2.desktop.qa.visualFixes")
                && window.content() instanceof appeng.client.gui.me.crafting.CraftingCPUScreen<?> cpu) {
            var model = cpu.getMenu();
            var id = model.desktopSyncId();
            boolean ready = model.cpuStatusReady, available = model.cpuAvailable, active = model.hasActiveJob;
            int serial = model.cpuStatusSerial;
            result.add(new Variant("status-waiting", () -> DesktopQaHooks.VISUAL_FIXTURES.put(id, () -> {
                model.cpuStatusReady = false;
                model.hasActiveJob = true;
                cpu.postUpdate(appeng.menu.me.crafting.CraftingStatus.EMPTY);
            })));
            result.add(new Variant("status-unavailable", () -> DesktopQaHooks.VISUAL_FIXTURES.put(id, () -> {
                model.cpuStatusReady = true;
                model.cpuAvailable = false;
                model.hasActiveJob = false;
                model.cpuStatusSerial = model instanceof CraftingStatusMenu status ? status.getSelectedCpuSerial() : 0;
                cpu.postUpdate(appeng.menu.me.crafting.CraftingStatus.EMPTY);
            })));
            result.add(new Variant("status-active", () -> DesktopQaHooks.VISUAL_FIXTURES.put(id, () -> {
                model.cpuStatusReady = true;
                model.cpuAvailable = true;
                model.hasActiveJob = true;
                model.cpuStatusSerial = model instanceof CraftingStatusMenu status ? status.getSelectedCpuSerial() : 0;
                cpu.postUpdate(new appeng.menu.me.crafting.CraftingStatus(true, 1_000_000_000L, 7, 10, List.of(
                        new appeng.menu.me.crafting.CraftingStatusEntry(1, AEItemKey.of(Items.IRON_INGOT), 1000, 24,
                                64),
                        new appeng.menu.me.crafting.CraftingStatusEntry(2, AEItemKey.of(Items.REDSTONE), 128, 0, 0)),
                        false));
            })));
            result.add(new Variant("status-resynced", () -> {
                DesktopQaHooks.VISUAL_FIXTURES.remove(id);
                model.cpuStatusReady = ready;
                model.cpuAvailable = available;
                model.hasActiveJob = active;
                model.cpuStatusSerial = serial;
                cpu.postUpdate(appeng.menu.me.crafting.CraftingStatus.EMPTY);
            }));
        }
        if (!window.content().getStyle().isDesktopRestyled()
                && window.content() instanceof appeng.client.gui.implementations.CellWorkbenchScreen
                && Boolean.getBoolean("ae2.desktop.qa.populated")) {
            result.add(new Variant("toolbox-pickup", () -> clickToolboxSlot(false)));
            result.add(new Variant("toolbox-return", () -> clickToolboxSlot(true)));
        }
        for (var child : window.content().children()) {
            if (child instanceof appeng.client.gui.widgets.TabButton tab && tab.visible) {
                String name = tab.getMessage().getString();
                if (List.of("Upgrades", "View Cells", "Toolbox").contains(name)) {
                    result.add(new Variant("panel-" + name.toLowerCase().replace(' ', '-'), tab));
                    if (name.equals("Toolbox")
                            && window.content() instanceof appeng.client.gui.implementations.CellWorkbenchScreen
                            && Boolean.getBoolean("ae2.desktop.qa.populated")) {
                        result.add(new Variant("toolbox-pickup", () -> clickToolboxSlot(false)));
                        result.add(new Variant("toolbox-return", () -> clickToolboxSlot(true)));
                    }
                }
            }
        }
        window.content().children().stream().filter(appeng.client.gui.widgets.TabButton.class::isInstance)
                .map(appeng.client.gui.widgets.TabButton.class::cast)
                .filter(tab -> tab.visible
                        && window.content() instanceof appeng.client.gui.me.items.PatternEncodingTermScreen
                        && tab.getMessage().getString().endsWith("Patterns"))
                .sorted(window.content().usesAeDesktopLayout()
                        || window.content() instanceof appeng.client.gui.me.items.PatternEncodingTermScreen
                                ? Comparator.comparingInt(t -> t.getY())
                                : Comparator.comparingInt(t -> t.getX()))
                .forEach(tab -> result
                        .add(new Variant("mode-" + tab.getMessage().getString().toLowerCase().replace(' ', '-'), tab,
                                Boolean.getBoolean("ae2.desktop.qa.populated")
                                        ? () -> populateEncodingMode(tab.getMessage().getString())
                                        : null)));
        window.content().children().stream().filter(appeng.client.gui.widgets.ActionButton.class::isInstance)
                .map(appeng.client.gui.widgets.ActionButton.class::cast)
                .filter(button -> button.visible && button.getMessage().getString()
                        .equals(appeng.core.localization.ButtonToolTips.TerminalSettings.text().getString()))
                .findFirst().ifPresent(button -> {
                    result.add(new Variant("settings", button));
                    result.add(new Variant("settings-return", DesktopAtlasQa::returnLocalView));
                });
        window.content().children().stream().filter(appeng.client.gui.widgets.KeyTypeSelectionButton.class::isInstance)
                .map(appeng.client.gui.widgets.KeyTypeSelectionButton.class::cast).filter(button -> button.visible)
                .findFirst().ifPresent(button -> {
                    result.add(new Variant("key-types", button));
                    result.add(new Variant("key-types-return", DesktopAtlasQa::returnLocalView));
                });
        if (window.content() instanceof appeng.client.gui.me.items.PatternEncodingTermScreen<?> parent) {
            result.add(new Variant("processing-amount", () -> parent.switchToScreen(
                    new appeng.client.gui.me.items.SetProcessingPatternAmountScreen<>(parent,
                            new appeng.api.stacks.GenericStack(AEItemKey.of(Items.IRON_INGOT), 32), ignored -> {
                            }))));
            result.add(new Variant("processing-amount-return", DesktopAtlasQa::returnLocalView));
        }
        if (window.content() instanceof appeng.client.gui.me.crafting.CraftConfirmScreen parent) {
            if (Boolean.getBoolean("ae2.desktop.qa.visualFixes")) {
                var original = parent.getMenu().getPlan();
                result.add(new Variant("readable-mixed", () -> parent.getMenu().setPlan(
                        new appeng.menu.me.crafting.CraftingPlanSummary(65536, true, List.of(
                                new appeng.menu.me.crafting.CraftingPlanSummaryEntry(AEItemKey.of(Items.IRON_INGOT),
                                        1234567, 2000000, 876543),
                                new appeng.menu.me.crafting.CraftingPlanSummaryEntry(AEItemKey.of(Items.GOLD_INGOT),
                                        123, 3000, 12000),
                                new appeng.menu.me.crafting.CraftingPlanSummaryEntry(AEItemKey.of(Items.DIAMOND), 0, 32,
                                        64),
                                new appeng.menu.me.crafting.CraftingPlanSummaryEntry(AEItemKey.of(Items.REDSTONE), 64,
                                        0, 0),
                                new appeng.menu.me.crafting.CraftingPlanSummaryEntry(
                                        AEFluidKey.of(net.minecraft.world.level.material.Fluids.WATER), 10000000,
                                        20000000, 0))))));
                result.add(new Variant("readable-scroll", () -> {
                    for (int step = 0; step < 2; step++)
                        Minecraft.getInstance().screen
                                .mouseScrolled(parent.getGuiLeft() + 40, parent.getGuiTop() + 45, 0, -1);
                }));
                result.add(new Variant("readable-return", () -> parent.getMenu().setPlan(original)));
            }
            for (var code : appeng.api.networking.crafting.CraftingSubmitErrorCode.values()) {
                Object details = switch (code) {
                    case MISSING_INGREDIENT -> new appeng.api.stacks.GenericStack(AEItemKey.of(Items.IRON_INGOT), 64);
                    case NO_SUITABLE_CPU_FOUND -> new appeng.api.networking.crafting.UnsuitableCpus(1, 2, 3, 4);
                    default -> null;
                };
                result.add(new Variant("error-" + code.name().toLowerCase(), () -> parent.switchToScreen(
                        new appeng.client.gui.me.crafting.CraftErrorScreen(parent, code, details))));
                if (Boolean.getBoolean("ae2.desktop.qa.visualFixes")
                        && DesktopQaHooks.windowAvailable.apply(parent.getMenu().desktopSyncId(), "MINIMIZE")) {
                    result.add(new Variant("error-" + code.name().toLowerCase() + "-minimized", () -> {
                        if (!DesktopQaHooks.windowAction.apply(parent.getMenu().desktopSyncId(), "MINIMIZE"))
                            throw new IllegalStateException("Minimize acceptance needs its desktop setting enabled");
                    }));
                    result.add(new Variant("error-" + code.name().toLowerCase() + "-restored",
                            () -> DesktopQaHooks.windowAction.apply(parent.getMenu().desktopSyncId(), "RESTORE")));
                }
                result.add(
                        new Variant("error-" + code.name().toLowerCase() + "-return", DesktopAtlasQa::returnLocalView));
            }
        }
        return result;
    }

    private static appeng.menu.me.networktool.NetworkStatus largeNetworkFixture(Minecraft mc) {
        var data = new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),
                mc.player.registryAccess(), net.neoforged.neoforge.network.connection.ConnectionType.NEOFORGE);
        try {
            data.writeDouble(1);
            data.writeDouble(2);
            data.writeDouble(65536);
            data.writeDouble(131072);
            data.writeDouble(1);
            data.writeVarInt(16);
            var displays = List.of(Items.STONE, Items.IRON_INGOT, Items.GOLD_INGOT, Items.DIAMOND, Items.EMERALD,
                    Items.REDSTONE, Items.GLASS, Items.QUARTZ, Items.COAL, Items.COPPER_INGOT, Items.LAPIS_LAZULI,
                    Items.NETHERITE_INGOT, Items.OBSIDIAN, Items.ACACIA_LOG, Items.CARROT);
            int[] counts = { Integer.MAX_VALUE, 999999, 12345, 10000, 9999, 1200, 999, 64, 32, 16, 5, 4, 3, 2, 1 };
            data.writeVarInt(displays.size());
            for (int i = 0; i < displays.size(); i++) {
                AEItemKey.of(displays.get(i)).writeToPacket(data);
                data.writeBoolean(i == displays.size() - 1);
                data.writeDouble(0.5);
                data.writeDouble(0);
                data.writeVarInt(counts[i]);
            }
            return appeng.menu.me.networktool.NetworkStatus.read(data);
        } finally {
            data.release();
        }
    }

    private static void click(Minecraft mc, double x, double y) {
        mc.screen.mouseClicked(x, y, 0);
        mc.screen.mouseReleased(x, y, 0);
    }

    private static void clickToolboxSlot(boolean returning) {
        var mc = Minecraft.getInstance();
        var parent = captured.content();
        var content = toolboxContent(parent);
        var slot = content.getMenu().getSlots(parent.hasDesktopSharedToolbox()
                ? appeng.menu.SlotSemantics.STORAGE
                : appeng.menu.SlotSemantics.TOOLBOX).getFirst();
        var cursor = content.isDesktopHosted() ? mc.player.inventoryMenu.getCarried()
                : content.getMenu().getCarried();
        if (returning && (!cursor.is(AEItems.CAPACITY_CARD.asItem())
                || cursor.getCount() != 4 || slot.hasItem())) {
            throw new IllegalStateException(
                    "Toolbox pickup did not transfer its four capacity cards to the active cursor");
        }
        click(mc, content.getGuiLeft() + slot.x + 8, content.getGuiTop() + slot.y + 8);
    }

    private static AEBaseScreen<?> toolboxContent(AEBaseScreen<?> parent) {
        if (!parent.hasDesktopSharedToolbox()) {
            return parent;
        }
        int owner = DesktopQaHooks.SESSION_IDS.get(parent.getMenu().desktopSyncId());
        return DesktopQaHooks.COMPANIONS.entrySet().stream()
                .filter(entry -> DesktopQaHooks.COMPANION_OWNERS.getOrDefault(entry.getKey(), List.of())
                        .contains(owner))
                .findFirst().orElseThrow(() -> new IllegalStateException("Shared Toolbox has no actual client menu"))
                .getValue().content();
    }

    private static void returnLocalView() {
        if (!(captured.content() instanceof AESubScreen<?, ?> view)) {
            throw new IllegalStateException("Expected a local view before returning");
        }
        nativeCall(view, "returnToParent", new Class<?>[0]);
    }

    private static long modified(Path path) {
        try {
            return Files.getLastModifiedTime(path).toMillis();
        } catch (Exception ignored) {
            return 0;
        }
    }

    private static void setup(ServerPlayer player) {
        var level = player.serverLevel();
        player.setGameMode(GameType.SURVIVAL);
        player.getInventory().clearContent();
        var toolbox = AEItems.NETWORK_TOOL.stack();
        var cards = appeng.items.tools.NetworkToolItem.getInventory(toolbox);
        cards.setItemDirect(0, AEItems.CAPACITY_CARD.stack(4));
        cards.setItemDirect(1, AEItems.FUZZY_CARD.stack());
        cards.setItemDirect(2, AEItems.REDSTONE_CARD.stack());
        player.getInventory().setItem(1, toolbox);
        player.inventoryMenu.broadcastChanges();
        player.teleportTo(level, -1.5, 80, -3.5, Set.of(), 0, 15);
        var plot = new Plot(ResourceLocation.fromNamespaceAndPath("ae2", "desktop_atlas_qa"));
        plot.block("[-5,20] [-1,5] [-5,10]", net.minecraft.world.level.block.Blocks.AIR);
        plot.block("[-5,20] -1 [-5,10]", net.minecraft.world.level.block.Blocks.STONE);
        plot.creativeEnergyCell("0 0 1");
        plot.block("1 0 1", AEBlocks.CONTROLLER);
        plot.cable("[0,15] 0 0", AEParts.SMART_DENSE_CABLE);
        // Four small terminal branches preserve their actual normal part/menu hosts.
        var terminals = List.of(AEParts.TERMINAL, AEParts.CRAFTING_TERMINAL,
                AEParts.PATTERN_ENCODING_TERMINAL, AEParts.PATTERN_ACCESS_TERMINAL);
        for (int x = 0; x < 4; x++) {
            plot.cable(x + " 1 0", AEParts.GLASS_CABLE);
            plot.part(x + " 1 0", Direction.NORTH, terminals.get(x));
        }
        plot.drive(new BlockPos(2, 0, 1)).addItemCell64k().and().addItemCell64k();
        plot.block("3 0 1", AEBlocks.PATTERN_PROVIDER);
        plot.block("3 1 1", AEBlocks.MOLECULAR_ASSEMBLER);
        plot.block("4 0 1", AEBlocks.CRAFTING_STORAGE_64K);
        if (Boolean.getBoolean("ae2.desktop.qa.visualFixes")) {
            for (int cpuX : List.of(8, 10, 12, 14))
                plot.block(cpuX + " 0 1", AEBlocks.CRAFTING_STORAGE_64K);
        }
        plot.cable("5 1 0", AEParts.GLASS_CABLE);
        var accessPoint = AEBlocks.WIRELESS_ACCESS_POINT.block();
        plot.blockState("5 1 1", accessPoint.getOrientationStrategy()
                .setFacing(accessPoint.defaultBlockState(), Direction.SOUTH));
        plot.block("6 0 1", AEBlocks.ME_CHEST);
        plot.block("7 0 1", AEBlocks.INTERFACE);
        var blocks = List.of(AEBlocks.QUANTUM_LINK, AEBlocks.SKY_STONE_CHEST, AEBlocks.VIBRATION_CHAMBER,
                AEBlocks.CONDENSER, AEBlocks.IO_PORT, AEBlocks.SPATIAL_IO_PORT, AEBlocks.INSCRIBER,
                AEBlocks.CELL_WORKBENCH, AEBlocks.SPATIAL_ANCHOR);
        for (int x = 0; x < blocks.size(); x++)
            plot.block(x + " 0 4", blocks.get(x));
        var parts = List.of(AEParts.EXPORT_BUS, AEParts.IMPORT_BUS, AEParts.STORAGE_BUS,
                AEParts.FORMATION_PLANE, AEParts.LEVEL_EMITTER, AEParts.ENERGY_LEVEL_EMITTER);
        for (int x = 0; x < parts.size(); x++) {
            plot.cable((x + 8) + " 1 0", AEParts.GLASS_CABLE);
            plot.part((x + 8) + " 1 0", Direction.NORTH, parts.get(x));
        }
        plot.afterGridInitAt(new BlockPos(2, 0, 1), (grid, node) -> NativeAtlasSetup.run(OUTPUT, () -> {
            var storage = grid.getStorageService().getInventory();
            var source = new BaseActionSource();
            for (var item : List.of(Items.ACACIA_LOG, Items.OAK_LOG, Items.STONE, Items.COBBLESTONE,
                    Items.IRON_INGOT, Items.GOLD_INGOT, Items.DIAMOND, Items.REDSTONE, Items.GLASS,
                    Items.QUARTZ, Items.COAL, Items.COPPER_INGOT)) {
                storage.insert(AEItemKey.of(item), 128, Actionable.MODULATE, source);
            }
            var provider = (appeng.blockentity.crafting.PatternProviderBlockEntity) level
                    .getBlockEntity(ORIGIN.offset(3, 0, 1));
            provider.getLogic().getPatternInv().addItems(appeng.server.testplots.CraftingPatternHelper
                    .encodeShapelessCraftingRecipe(level, Items.ACACIA_LOG.getDefaultInstance()));
            var crafting = (appeng.parts.reporting.CraftingTerminalPart) PartHelper.getPart(level,
                    ORIGIN.offset(1, 1, 0), Direction.NORTH);
            crafting.getSubInventory(appeng.parts.reporting.CraftingTerminalPart.INV_CRAFTING)
                    .setItemDirect(0, Items.ACACIA_LOG.getDefaultInstance());
            var chest = (appeng.blockentity.storage.MEChestBlockEntity) level.getBlockEntity(ORIGIN.offset(6, 0, 1));
            chest.setCell(AEItems.ITEM_CELL_64K.stack());
            chest.getInventory().insert(AEItemKey.of(Items.DIAMOND), 64, Actionable.MODULATE, source);
            var inscriber = (appeng.blockentity.misc.InscriberBlockEntity) level.getBlockEntity(ORIGIN.offset(6, 0, 4));
            inscriber.getInternalInventory().setItemDirect(0, AEItems.SILICON_PRESS.stack());
            inscriber.getInternalInventory().setItemDirect(2, AEItems.SILICON.stack(16));
            if (Boolean.getBoolean("ae2.desktop.qa.populated")) {
                populateFixtures(player);
            }
            buildEntries();
            var only = System.getProperty("ae2.desktop.qa.only", "");
            if (!only.isBlank()) {
                var selected = Set.of(only.split(","));
                ENTRIES.removeIf(entry -> !selected.contains(entry.label));
            }
            READY.set(true);
            LoggerFactory.getLogger(DesktopAtlasQa.class).info("ATLAS FIXTURE READY {}", ENTRIES.size());
        }));
        plot.build(level, player, ORIGIN);
    }

    private static void populateFixtures(ServerPlayer player) {
        var level = player.serverLevel();
        for (int x : List.of(8, 9, 10, 11)) {
            var part = PartHelper.getPart(level, ORIGIN.offset(x, 1, 0), Direction.NORTH);
            if (part instanceof appeng.api.upgrades.IUpgradeableObject upgraded) {
                upgraded.getUpgrades().addItems(AEItems.CAPACITY_CARD.stack(2));
                upgraded.getUpgrades().addItems(AEItems.FUZZY_CARD.stack());
                upgraded.getUpgrades().addItems(AEItems.REDSTONE_CARD.stack());
            }
            if (part instanceof appeng.helpers.IConfigInvHost configured) {
                var items = List.of(Items.IRON_INGOT, Items.GOLD_INGOT, Items.REDSTONE, Items.DIAMOND);
                for (int n = 0; n < items.size(); n++) {
                    configured.getConfig().setStack(n,
                            new appeng.api.stacks.GenericStack(AEItemKey.of(items.get(n)), 1));
                }
            }
        }
        var workbench = (appeng.blockentity.misc.CellWorkbenchBlockEntity) level.getBlockEntity(ORIGIN.offset(7, 0, 4));
        workbench.getSubInventory(appeng.api.inventories.ISegmentedInventory.CELLS).setItemDirect(0,
                AEItems.ITEM_CELL_64K.stack());
        workbench.getConfig().setStack(0, new appeng.api.stacks.GenericStack(AEItemKey.of(Items.IRON_INGOT), 1));
        workbench.getConfig().setStack(1, new appeng.api.stacks.GenericStack(AEItemKey.of(Items.GOLD_INGOT), 1));
        var iface = (appeng.helpers.InterfaceLogicHost) level.getBlockEntity(ORIGIN.offset(7, 0, 1));
        iface.getInterfaceLogic().getConfig().setStack(0,
                new appeng.api.stacks.GenericStack(AEItemKey.of(Items.IRON_INGOT), 64));
        iface.getInterfaceLogic().getConfig().setStack(1,
                new appeng.api.stacks.GenericStack(AEItemKey.of(Items.REDSTONE), 32));
        var patterns = (appeng.parts.encoding.PatternEncodingTerminalPart) PartHelper.getPart(level,
                ORIGIN.offset(2, 1, 0), Direction.NORTH);
        patterns.getLogic().getBlankPatternInv().setItemDirect(0, AEItems.BLANK_PATTERN.stack(8));
        patterns.getLogic().getEncodedInputInv().setStack(0,
                new appeng.api.stacks.GenericStack(AEItemKey.of(Items.ACACIA_LOG), 1));
    }

    private static void populateEncodingMode(String label) {
        var mc = Minecraft.getInstance();
        mc.getSingleplayerServer().execute(() -> {
            var logic = ((appeng.parts.encoding.PatternEncodingTerminalPart) PartHelper.getPart(
                    mc.getSingleplayerServer().overworld(),
                    ORIGIN.offset(2, 1, 0), Direction.NORTH)).getLogic();
            logic.getEncodedInputInv().clear();
            logic.getEncodedOutputInv().clear();
            var input = label.contains("Stonecutting") ? Items.STONE
                    : label.contains("Smithing")
                            ? Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE
                            : Items.ACACIA_LOG;
            logic.getEncodedInputInv().setStack(0, new appeng.api.stacks.GenericStack(AEItemKey.of(input), 1));
            if (label.contains("Smithing")) {
                logic.getEncodedInputInv().setStack(1,
                        new appeng.api.stacks.GenericStack(AEItemKey.of(Items.DIAMOND_SWORD), 1));
                logic.getEncodedInputInv().setStack(2,
                        new appeng.api.stacks.GenericStack(AEItemKey.of(Items.NETHERITE_INGOT), 1));
            } else if (label.contains("Processing")) {
                logic.getEncodedOutputInv().setStack(0,
                        new appeng.api.stacks.GenericStack(AEItemKey.of(Items.ACACIA_PLANKS), 4));
            }
        });
    }

    private static MenuHostLocator block(ServerPlayer p, int x, int y, int z) {
        var pos = ORIGIN.offset(x, y, z);
        p.teleportTo(p.serverLevel(), pos.getX() - 1.5, 80, pos.getZ() - 2.5, Set.of(), 0, 15);
        return MenuLocators.forBlockEntity(p.level().getBlockEntity(pos));
    }

    private static MenuHostLocator part(ServerPlayer p, int x) {
        p.teleportTo(p.serverLevel(), x - 1.5, 80, -2.5, Set.of(), 0, 15);
        return MenuLocators
                .forPart((AEBasePart) PartHelper.getPart(p.level(), ORIGIN.offset(x, 1, 0), Direction.NORTH));
    }

    private static void b(String label, MenuType<?> type, int x, int y, int z) {
        ENTRIES.add(new Entry(label, type, p -> MenuOpener.open(type, p, block(p, x, y, z))));
    }

    private static void t(String label, MenuType<?> type, int x) {
        ENTRIES.add(new Entry(label, type, p -> MenuOpener.open(type, p, part(p, x))));
    }

    private static void buildEntries() {
        b("quantum-network-bridge", QNBMenu.TYPE, 0, 0, 4);
        b("sky-stone-chest", SkyChestMenu.TYPE, 1, 0, 4);
        b("me-chest", MEChestMenu.TYPE, 6, 0, 1);
        b("wireless-access-point", WirelessAccessPointMenu.TYPE, 5, 1, 1);
        ENTRIES.add(new Entry("network-status-tool", NetworkStatusMenu.NETWORK_TOOL_TYPE, p -> {
            block(p, 1, 0, 1);
            p.getInventory().setItem(0, AEItems.NETWORK_TOOL.stack());
            p.getInventory().selected = 0;
            p.inventoryMenu.broadcastChanges();
            var hit = new BlockHitResult(Vec3.atCenterOf(ORIGIN.offset(1, 0, 1)), Direction.NORTH,
                    ORIGIN.offset(1, 0, 1), false);
            MenuOpener.open(NetworkStatusMenu.NETWORK_TOOL_TYPE, p,
                    MenuLocators.forItemUseContext(new UseOnContext(p, InteractionHand.MAIN_HAND, hit)));
        }));
        b("network-status-controller", NetworkStatusMenu.CONTROLLER_TYPE, 1, 0, 1);
        b("crafting-cpu", CraftingCPUMenu.TYPE, 4, 0, 1);
        item("network-tool", NetworkToolMenu.TYPE, AEItems.NETWORK_TOOL.stack());
        item("quartz-cutting-knife", QuartzKnifeMenu.TYPE, AEItems.CERTUS_QUARTZ_KNIFE.stack());
        b("me-drive", DriveMenu.TYPE, 2, 0, 1);
        b("vibration-chamber", VibrationChamberMenu.TYPE, 2, 0, 4);
        b("matter-condenser", CondenserMenu.TYPE, 3, 0, 4);
        b("me-interface", InterfaceMenu.TYPE, 7, 0, 1);
        t("export-bus", IOBusMenu.EXPORT_TYPE, 8);
        t("import-bus", IOBusMenu.IMPORT_TYPE, 9);
        b("me-io-port", IOPortMenu.TYPE, 4, 0, 4);
        t("storage-bus", StorageBusMenu.TYPE, 10);
        ENTRIES.add(new Entry("stock-amount", SetStockAmountMenu.TYPE, p -> SetStockAmountMenu.open(p,
                block(p, 7, 0, 1), 0, AEItemKey.of(Items.IRON_INGOT), 64)));
        t("formation-plane", FormationPlaneMenu.TYPE, 11);
        b("priority", PriorityMenu.TYPE, 6, 0, 1);
        t("storage-level-emitter", StorageLevelEmitterMenu.TYPE, 12);
        t("energy-level-emitter", EnergyLevelEmitterMenu.TYPE, 13);
        b("spatial-io-port", SpatialIOPortMenu.TYPE, 5, 0, 4);
        b("inscriber", InscriberMenu.TYPE, 6, 0, 4);
        b("cell-workbench", CellWorkbenchMenu.TYPE, 7, 0, 4);
        b("pattern-provider", PatternProviderMenu.TYPE, 3, 0, 1);
        b("molecular-assembler", MolecularAssemblerMenu.TYPE, 3, 1, 1);
        ENTRIES.add(new Entry("craft-amount", CraftAmountMenu.TYPE, p -> CraftAmountMenu.open(p,
                part(p, 0), AEItemKey.of(Items.ACACIA_PLANKS), 8)));
        ENTRIES.add(new Entry("craft-confirm", CraftConfirmMenu.TYPE, p -> {
            var menu = MenuOpener.openAndGetMenu(CraftConfirmMenu.TYPE, p, part(p, 0));
            menu.planJob(AEItemKey.of(Items.ACACIA_PLANKS), 8,
                    appeng.api.networking.crafting.CalculationStrategy.REPORT_MISSING_ITEMS);
        }));
        t("crafting-status", CraftingStatusMenu.TYPE, 0);
        b("spatial-anchor", SpatialAnchorMenu.TYPE, 8, 0, 4);
        t("me-terminal", MEStorageMenu.TYPE, 0);
        b("me-chest-storage", BasicCellChestMenu.TYPE, 6, 0, 1);
        item("portable-item-cell", MEStorageMenu.PORTABLE_ITEM_CELL_TYPE,
                charged(AEItems.PORTABLE_ITEM_CELL64K.stack(), false, false));
        item("portable-fluid-cell", MEStorageMenu.PORTABLE_FLUID_CELL_TYPE,
                charged(AEItems.PORTABLE_FLUID_CELL64K.stack(), true, false));
        item("wireless-terminal", MEStorageMenu.WIRELESS_TYPE, charged(AEItems.WIRELESS_TERMINAL.stack(), false, true));
        t("crafting-terminal", CraftingTermMenu.TYPE, 1);
        item("wireless-crafting-terminal", WirelessCraftingTermMenu.TYPE,
                charged(AEItems.WIRELESS_CRAFTING_TERMINAL.stack(), false, true));
        t("pattern-encoding-terminal", PatternEncodingTermMenu.TYPE, 2);
        t("pattern-access-terminal", PatternAccessTermMenu.TYPE, 3);
        String focus = System.getenv("SIU_AE2_ATLAS_FOCUS");
        if (focus != null) {
            ENTRIES.removeIf(entry -> !BuiltInRegistries.MENU.getKey(entry.type).toString().equals("ae2:"+focus));
            if (ENTRIES.isEmpty()) throw new IllegalArgumentException("Unknown atlas menu " + focus);
        }
    }

    private static net.minecraft.world.item.ItemStack charged(net.minecraft.world.item.ItemStack stack, boolean fluid,
            boolean wireless) {
        ((appeng.items.tools.powered.powersink.AEBasePoweredItem) stack.getItem()).injectAEPower(stack, 1000000,
                Actionable.MODULATE);
        if (wireless)
            stack.set(AEComponents.WIRELESS_LINK_TARGET,
                    GlobalPos.of(net.minecraft.world.level.Level.OVERWORLD, ORIGIN.offset(5, 1, 1)));
        else {
            var inventory = StorageCells.getCellInventory(stack, null);
            inventory.insert(
                    fluid ? AEFluidKey.of(net.minecraft.world.level.material.Fluids.WATER)
                            : AEItemKey.of(Items.IRON_INGOT),
                    fluid ? 8000 : 128, Actionable.MODULATE, new BaseActionSource());
        }
        return stack;
    }

    @SuppressWarnings("unchecked")
    private static void item(String label, MenuType<?> type, net.minecraft.world.item.ItemStack stack) {
        ENTRIES.add(new Entry(label, type, p -> {
            p.teleportTo(p.serverLevel(), 4, 80, -2, Set.of(), 0, 15);
            p.getInventory().setItem(0, stack.copy());
            p.inventoryMenu.broadcastChanges();
            var menu = MenuOpener.openAndGetMenu((MenuType<appeng.menu.AEBaseMenu>) type, p,
                    MenuLocators.forInventorySlot(0));
            if (menu instanceof MEStorageMenu storage) {
                LoggerFactory.getLogger(DesktopAtlasQa.class).info(
                        "ATLAS ITEM STORAGE {} connected={} entries={} link={}",
                        label, storage.getHost().getLinkStatus().connected(),
                        storage.getHost().getInventory().getAvailableStacks().size(),
                        p.getInventory().getItem(0).get(AEComponents.WIRELESS_LINK_TARGET));
            }
        }));
    }
}
