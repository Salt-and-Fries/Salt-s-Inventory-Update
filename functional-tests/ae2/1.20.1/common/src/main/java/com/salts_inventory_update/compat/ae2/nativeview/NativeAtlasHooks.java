package com.salts_inventory_update.compat.ae2.nativeview;

import appeng.menu.AEBaseMenu;
import appeng.menu.SlotSemantics;
import com.salts_inventory_update.api.client.desktop.*;
import com.salts_inventory_update.compat.ae2.qa.DesktopQaHooks;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Test-only observation of extension-owned state and public API window handles. */
public final class NativeAtlasHooks {
    private static final Map<UUID, AEWindow.State> OPEN = new HashMap<>();
    private static final Map<UUID, String> LAST_SIZING = new HashMap<>();
    static {
        DesktopQaHooks.closeAll = () -> List.copyOf(OPEN.values()).stream()
                .filter(state -> !state.companion).forEach(state -> state.handle.close());
        DesktopQaHooks.closeWindow = id -> { var s = OPEN.get(id); if (s != null) s.handle.close(); };
        DesktopQaHooks.unlockWindow = id -> { var s = OPEN.get(id); if (s != null) s.handle.request(DesktopWindowAction.UNLOCK); };
        DesktopQaHooks.windowAction = (id, action) -> { var s = OPEN.get(id); return s != null && s.handle.request(DesktopWindowAction.valueOf(action)); };
        DesktopQaHooks.windowAvailable = (id, action) -> { var s = OPEN.get(id); return s != null && s.handle.available(DesktopWindowAction.valueOf(action)); };
        DesktopQaHooks.windowVisible = id -> { var s = OPEN.get(id); return s != null && s.handle.state().visible(); };
        DesktopQaHooks.windowSummary = id -> { var s = OPEN.get(id); return s == null ? "disposed" : s.handle.state().toString(); };
    }
    private NativeAtlasHooks() { }
    public static void beforeRender(DesktopRenderContext<AEBaseMenu, AEWindow.State> context) {
        var fixture = DesktopQaHooks.VISUAL_FIXTURES.get(context.model().desktopSyncId());
        if (fixture != null) fixture.run();
    }
    public static void afterRender(DesktopRenderContext<AEBaseMenu, AEWindow.State> context) {
        var state = context.state(); var content = state.content; var id = context.model().desktopSyncId();
        if (context.model() instanceof appeng.menu.me.items.PatternEncodingTermMenu) {
            String sizing = content.getClass().getName()+" window="+context.windowWidth()+"x"+context.windowHeight()
                    +" content="+context.contentWidth()+"x"+context.contentHeight()
                    +" initial="+java.util.Arrays.toString(appeng.init.client.InitScreens.desktopDefaultContentSize(context.model()))
                    +" minimum="+java.util.Arrays.toString(content.desktopPreferredSize())
                    +" requested="+java.util.Arrays.toString(content.desktopRequestedSize());
            var terminal = content.getStyle().getTerminalStyle();
            if (terminal != null) sizing += " cut="+content.desktopInventoryCut()+" header="+terminal.getHeaderHeight()
                    +" bottom="+terminal.getBottom().getSrcHeight()+" row="+terminal.getRow().getSrcHeight()
                    +" snap176="+java.util.Arrays.toString(content.desktopSnapSize(195,176));
            if (!sizing.equals(LAST_SIZING.put(id, sizing))) org.slf4j.LoggerFactory.getLogger(NativeAtlasHooks.class).info("ATLAS SIZING {}", sizing);
        }
        OPEN.entrySet().removeIf(entry -> {
            if (!entry.getValue().handle.isOpen() || !entry.getKey().equals(entry.getValue().model.desktopSyncId())) {
                remove(entry.getKey()); return true;
            }
            return false;
        });
        OPEN.put(id, state);
        var inputs = context.model().getSlots(SlotSemantics.MACHINE_INPUT);
        var window = new DesktopQaHooks.Window(context.windowX(), context.windowY(), context.windowWidth(), context.windowHeight(),
                inputs.isEmpty() ? -1 : content.getGuiLeft()+inputs.get(0).x+8,
                inputs.isEmpty() ? -1 : content.getGuiTop()+inputs.get(0).y+8, content);
        (context.companion() ? DesktopQaHooks.COMPANIONS : DesktopQaHooks.WINDOWS).put(id, window);
        DesktopQaHooks.SESSION_IDS.put(id, context.sessionId());
        if (context.companion()) {
            DesktopQaHooks.COMPANION_OWNERS.put(id, context.companionOwnerSessionIds());
            if (!state.pendingCompanionDock && ++state.readyRenderFrames >= 2) DesktopQaHooks.COMPANION_READY.add(id);
        }
        DesktopQaHooks.MANAGEMENT_CONTROLS.put(id, state.managerControls.stream()
                .filter(control -> control.control() != DesktopWindowControl.RESIZE_GRIP)
                .map(control -> new DesktopQaHooks.Control(control.control().name(), control.bounds().x(), control.bounds().y(),
                        control.bounds().width(), control.bounds().height(), control.enabled(), control.selected(), control.keyboardFocused())).toList());
    }
    private static void remove(UUID id) {
        DesktopQaHooks.WINDOWS.remove(id); DesktopQaHooks.COMPANIONS.remove(id);
        DesktopQaHooks.COMPANION_READY.remove(id); DesktopQaHooks.COMPANION_OWNERS.remove(id);
        DesktopQaHooks.SESSION_IDS.remove(id); DesktopQaHooks.MANAGEMENT_CONTROLS.remove(id);
        DesktopQaHooks.VISUAL_FIXTURES.remove(id);
    }
    public static void closed(DesktopWindowContext<AEBaseMenu, AEWindow.State> context) {
        var id = context.model().desktopSyncId(); OPEN.remove(id); remove(id);
    }
}
