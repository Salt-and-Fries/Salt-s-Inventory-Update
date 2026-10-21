package com.salts_inventory_update.compat.ae2.nativeview;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.salts_inventory_update.api.client.desktop.DesktopChromeMetrics;
import com.salts_inventory_update.api.client.desktop.DesktopControlState;
import com.salts_inventory_update.api.client.desktop.DesktopHitRole;
import com.salts_inventory_update.api.client.desktop.DesktopInputContext;
import com.salts_inventory_update.api.client.desktop.DesktopInsets;
import com.salts_inventory_update.api.client.desktop.DesktopRenderContext;
import com.salts_inventory_update.api.client.desktop.DesktopResizePolicy;
import com.salts_inventory_update.api.client.desktop.DesktopSlotContext;
import com.salts_inventory_update.api.client.desktop.DesktopSlotHit;
import com.salts_inventory_update.api.client.desktop.DesktopSurfaceMode;
import com.salts_inventory_update.api.client.desktop.DesktopViewportPolicy;
import com.salts_inventory_update.api.client.desktop.DesktopWindowContext;
import com.salts_inventory_update.api.client.desktop.DesktopWindowDecoration;
import com.salts_inventory_update.api.client.desktop.DesktopWindowDefinition;
import com.salts_inventory_update.api.client.desktop.DesktopWindowHandle;
import com.salts_inventory_update.api.client.desktop.DesktopWindowPresentation;
import com.salts_inventory_update.api.client.desktop.DesktopWindowRegion;
import com.salts_inventory_update.api.client.desktop.DesktopWindowSetupContext;
import com.salts_inventory_update.api.client.desktop.DesktopWindowSize;
import com.salts_inventory_update.api.client.desktop.DesktopWindowSurface;

import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MenuType;

import appeng.client.AEClientboundPacketHandler;
import appeng.client.InitScreens;
import appeng.client.gui.AEBaseScreen;
import com.salts_inventory_update.compat.ae2.nativeview.NativeContentActions;
import com.salts_inventory_update.compat.ae2.nativeview.NativeContentRegistry;
import appeng.menu.AEBaseMenu;
import com.salts_inventory_update.compat.ae2.nativeview.NativeMenuPacketScope;
import appeng.menu.SlotSemantics;
import appeng.menu.slot.CraftingTermSlot;
import appeng.menu.slot.FakeSlot;

/** A native window of AE content and controls. Never renders or installs a container screen. */
public final class AEWindow implements DesktopWindowDefinition<AEBaseMenu, AEWindow.State>, DesktopWindowPresentation {
    @Override public List<com.salts_inventory_update.api.client.desktop.DesktopScrollRegion> controllerScrollRegions(DesktopWindowContext<AEBaseMenu,State> context) {
        var content=context.state().content;
        if(!(content instanceof appeng.client.gui.me.common.MEStorageScreen<?>)) return List.of();
        var bar=((com.salts_inventory_update.compat.ae2.mixin.MEStorageScrollAccessor)content).saltsInventory$scrollbar();
        var range=(com.salts_inventory_update.compat.ae2.mixin.ScrollbarRangeAccessor)bar;
        var slots=content.desktopSlots().stream().filter(slot -> slot instanceof appeng.client.gui.me.common.RepoSlot && content.desktopSlotVisible(slot)).toList();
        if(slots.isEmpty()) return List.of();
        int x=slots.stream().mapToInt(slot -> slot.x).min().orElseThrow(),y=slots.stream().mapToInt(slot -> slot.y).min().orElseThrow();
        int right=slots.stream().mapToInt(slot -> slot.x+16).max().orElseThrow(),bottom=slots.stream().mapToInt(slot -> slot.y+16).max().orElseThrow();
        return List.of(new com.salts_inventory_update.api.client.desktop.DesktopScrollRegion(content.getGuiLeft()+x,content.getGuiTop()+y,right-x,bottom-y,18,
            bar.getCurrentScroll()>range.saltsInventory$minScroll(),bar.getCurrentScroll()<range.saltsInventory$maxScroll(),1.0/Math.max(1,range.saltsInventory$pageSize())));
    }
    @Override public List<com.salts_inventory_update.api.client.desktop.DesktopWindowBounds> textInputBounds(DesktopWindowContext<AEBaseMenu,State> context) {
        var result=new java.util.ArrayList<com.salts_inventory_update.api.client.desktop.DesktopWindowBounds>();
        var pending=new java.util.ArrayDeque<net.minecraft.client.gui.components.events.GuiEventListener>(context.state().content.children());
        var visited=java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<net.minecraft.client.gui.components.events.GuiEventListener,Boolean>());
        while(!pending.isEmpty()) {
            var widget=pending.removeFirst();if(!visited.add(widget)) continue;
            if(widget instanceof net.minecraft.client.gui.components.events.ContainerEventHandler container) pending.addAll(container.children());
            if(widget instanceof net.minecraft.client.gui.components.EditBox text && text.visible && text.active)
                result.add(new com.salts_inventory_update.api.client.desktop.DesktopWindowBounds(text.getX(),text.getY(),text.getWidth(),text.getHeight()));
        }
        return List.copyOf(result);
    }
    private static final Map<UUID, State> OPEN = new HashMap<>();

    public static final class State {
        AEBaseMenu model;
        AEBaseScreen<?> content;
        DesktopWindowHandle handle;
        final Map<Class<?>, int[]> viewSizes = new HashMap<>();
        int contentWidth;
        int contentHeight;
        int mouseX = -10000;
        int mouseY = -10000;
        long resourceRevision = -1;
        int sharedToolboxSession = -1;
        boolean companion;
        String toolboxBadge;
        int sessionId;
        boolean pendingCompanionDock;
        List<Integer> initialOwners = List.of();
        int dockAge;
        int stableDockTicks;
        int[] previousDockOwnerBounds;
        Class<?> dockedOwnerView;
        int dockedOwnerWidth;
        int dockedOwnerHeight;
        boolean userPlaced;
        boolean automaticMove;
        int readyRenderFrames;
        List<DesktopControlState> managerControls = List.of();
        com.salts_inventory_update.api.client.desktop.DesktopWindowControl keyboardControl;
        net.minecraft.client.gui.screens.Screen hostScreen;
        boolean activatingHeaderControl;

        State(AEBaseMenu menu, Component title) {
            bind(menu, title);
        }

        void bind(AEBaseMenu menu, Component title) {
            if (model == menu) {
                return;
            }
            var preferences = new CompoundTag();
            if (model != null) {
                if (model.getType() == menu.getType()) {
                    content.saveDesktopPreferences(preferences);
                }
                NativeContentRegistry.remove(model.desktopSyncId());
                OPEN.remove(model.desktopSyncId());
                model.discardDesktopActions();
                content.desktopClosed();
            }
            model = menu;
            setContent(menu.isDesktopToolboxCompanion()
                    ? new com.salts_inventory_update.compat.ae2.nativeview.ToolboxCompanionScreen(
                            (appeng.menu.me.networktool.NetworkToolMenu) menu, title)
                    : InitScreens.createDesktopContent(menu, menu.getPlayerInventory(), title));
            content.loadDesktopPreferences(preferences);
            NativeContentRegistry.register(menu.desktopSyncId(),
                    packet -> AEClientboundPacketHandler.applyDesktopUpdate(packet, content));
            if (handle != null) {
                OPEN.put(menu.desktopSyncId(), this);
            }
        }

        private void setContent(AEBaseScreen<?> next) {
            if (handle != null && content != null)
                handle.cancelInput();
            if (content != null && contentWidth > 0 && contentHeight > 0) {
                viewSizes.put(content.getClass(), new int[] { contentWidth, contentHeight });
                int[] size = viewSizes.getOrDefault(next.getClass(), next.desktopDefaultSize());
                next.requestDesktopSize(size[0], size[1]);
            }
            next.prepareDesktop();
            next.setDesktopSharedToolbox(sharedToolboxSession >= 0);
            if (content != null) {
                next.refreshDesktopContentLayout();
            }
            next.setDesktopNavigation(this::setContent);
            content = next;

        }
    }

    private final MenuType<AEBaseMenu> type;

    static void installHeaderKeyboard() {
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(
                net.neoforged.bus.api.EventPriority.HIGHEST,
                (net.neoforged.neoforge.client.event.ScreenEvent.KeyPressed.Pre event) -> {
                    var owner = OPEN.values().stream().filter(state -> state.handle != null && state.handle.isOpen()
                            && state.hostScreen == event.getScreen() && state.handle.state().keyboardFocused())
                            .findFirst().orElse(null);
                    if (owner != null
                            && AEManagementControls.keyPressed(owner, event.getKeyEvent(), event.getScreen())) {
                        event.setCanceled(true);
                    }
                });
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(
                (net.neoforged.neoforge.client.event.ScreenEvent.MouseButtonPressed.Pre event) -> OPEN.values()
                        .forEach(state -> {
                            if (state.hostScreen == event.getScreen() && !state.activatingHeaderControl)
                                state.keyboardControl = null;
                        }));
    }

    public AEWindow(MenuType<AEBaseMenu> type) {
        this.type = type;

    }

    @Override
    public State createState(DesktopWindowSetupContext<AEBaseMenu> context) {

        return NativeMenuPacketScope.call(context.model(), () -> new State(context.model(), context.originalTitle()));
    }

    @Override
    public void opened(DesktopWindowContext<AEBaseMenu, State> context) {
        var state = context.state();
        state.bind(context.model(), context.originalTitle());
        state.handle = context.windowHandle();
        state.companion = context.companion();
        state.sessionId = context.sessionId();
        OPEN.put(context.model().desktopSyncId(), state);
        if (context.companion()) {
            int badge = 1;
            while (true) {
                String candidate = Integer.toString(badge++, 36).toUpperCase(java.util.Locale.ROOT);
                if (OPEN.values().stream().noneMatch(other -> candidate.equals(other.toolboxBadge))) {
                    state.toolboxBadge = candidate;
                    state.content.setDesktopToolboxBadge(candidate);
                    break;
                }
            }
            state.initialOwners = context.companionOwnerSessionIds();
            state.pendingCompanionDock = true;
        }
        if (context.model() instanceof appeng.menu.ISubMenu) {
            // A server GUI transition replaces the content for this host. Close the superseded owner
            // through its API handle so returning to the main menu creates a fresh authenticated model.
            OPEN.values().stream().filter(other -> other != state
                    && java.util.Objects.equals(other.model.getLocator(), context.model().getLocator()))
                    .toList().forEach(other -> other.handle.close());
        }
    }

    /** The native AE side panels protrude beyond its body; account for their complete visual bounds. */
    private static boolean dockCompanion(State companion, List<Integer> owners) {
        var parent = OPEN.values().stream().filter(state -> !state.companion
                && owners.contains(state.sessionId)).findFirst().orElse(null);
        if (parent == null || !parent.handle.isOpen() || !companion.handle.isOpen()
                || !parent.handle.state().visible())
            return false;
        // The child opening arrives before the parent's successful-acquisition notification.
        // Wait until the obsolete attachment has actually left the prepared visual geometry.
        if (!parent.content.hasDesktopSharedToolbox()
                || parent.model.getSlots(SlotSemantics.TOOLBOX).stream().anyMatch(parent.content::desktopSlotVisible)) {
            return false;
        }
        var preparedBody = parent.content.desktopFrame();
        var ownerState = parent.handle.state();
        var body = new net.minecraft.client.renderer.Rect2i(ownerState.x(), ownerState.y(),
                ownerState.width(), ownerState.height());
        int left = body.getX(), top = body.getY();
        int right = left + body.getWidth(), bottom = top + body.getHeight();
        for (var rect : parent.content.desktopExternalBounds()) {
            int x = body.getX() + rect.getX() - preparedBody.getX();
            int y = body.getY() + rect.getY() - preparedBody.getY();
            left = Math.min(left, x);
            top = Math.min(top, y);
            right = Math.max(right, x + rect.getWidth());
            bottom = Math.max(bottom, y + rect.getHeight());
        }
        int[] ownerBounds = { left, top, right, bottom };
        companion.dockAge++;
        if (java.util.Arrays.equals(ownerBounds, companion.previousDockOwnerBounds)) {
            companion.stableDockTicks++;
        } else {
            companion.previousDockOwnerBounds = ownerBounds;
            companion.stableDockTicks = 0;
        }
        // Native placement/restoration can move the parent after its first prepared frame.
        // Use its settled outline rather than leaving the accessory at that provisional origin.
        if (companion.dockAge < 10 || companion.stableDockTicks < 5)
            return false;
        var window = net.minecraft.client.Minecraft.getInstance().getWindow();
        int width = 68, height = 80;
        int[][] candidates = { { right + 4, top }, { left - width - 4, top },
                { body.getX(), bottom + 4 }, { body.getX(), top - height - 4 } };
        for (var position : candidates) {
            if (position[0] >= 0 && position[1] >= 0 && position[0] + width <= window.getGuiScaledWidth()
                    && position[1] + height <= window.getGuiScaledHeight()) {
                companion.handle.moveTo(position[0], position[1]);

                rememberDock(companion, parent, body);
                return true;
            }
        }
        companion.handle.moveTo(Math.max(0, Math.min(right + 4, window.getGuiScaledWidth() - width)),
                Math.max(0, Math.min(top, window.getGuiScaledHeight() - height)));
        rememberDock(companion, parent, body);
        return true;
    }

    private static void rememberDock(State companion, State parent, net.minecraft.client.renderer.Rect2i body) {
        companion.dockedOwnerView = parent.content.getClass();
        companion.dockedOwnerWidth = body.getWidth();
        companion.dockedOwnerHeight = body.getHeight();
    }

    @Override
    public void moved(DesktopWindowContext<AEBaseMenu, State> context) {
        var state = context.state();
        if (context.companion() && !state.automaticMove && context.keyboardFocused()) {
            state.userPlaced = true;
            state.pendingCompanionDock = false;
        }
    }

    @Override
    public DesktopWindowSurface surface(DesktopWindowContext<AEBaseMenu, State> context) {
        var content = context.state().content;
        var regions = new java.util.ArrayList<DesktopWindowRegion>();
        int width = context.windowWidth(), height = context.windowHeight();
        for (var segment : content.desktopSurfaceSegments()) {
            regions.add(DesktopWindowRegion.visualAndInteractive(segment.getX(), segment.getY(),
                    segment.getWidth(), segment.getHeight()));
        }
        boolean companion = context.companion() || context.model().isDesktopToolboxCompanion();
        // Keep explicit title movement/menu regions clear of even the largest native row.
        // With zero host insets, a declared MOVE region can intercept managed chrome;
        // reserving all seven control positions avoids relying on overlapping hit roles.
        int moveRight = companion ? width - 4 : width - controlMetrics(context).controlsWidth(7);
        regions.add(DesktopWindowRegion.interactive(4, 2, Math.max(0, moveRight - 4), 12)
                .withRole(DesktopHitRole.MOVE));
        // Original widgets/slots take priority over unused title-space movement and resize edges.
        for (var child : content.children()) {
            if (child instanceof net.minecraft.client.gui.components.AbstractWidget widget && widget.visible) {
                regions.add(DesktopWindowRegion.interactive(widget.getX() - content.getGuiLeft(),
                        widget.getY() - content.getGuiTop(), widget.getWidth(), widget.getHeight())
                        .withRole(DesktopHitRole.CONTROL));
            }
        }
        for (var slot : content.desktopSlots()) {
            if (content.desktopSlotVisible(slot)) {
                regions.add(DesktopWindowRegion.interactive(slot.x - 1, slot.y - 1, 18, 18)
                        .withRole(DesktopHitRole.SLOT));
            }
        }
        if (companion && !content.desktopResizeKind().equals("fixed")) {
            regions.add(DesktopWindowRegion.interactive(0, 0, width, 3).withRole(DesktopHitRole.RESIZE_NORTH));
            regions.add(DesktopWindowRegion.interactive(0, height - 3, width, 3).withRole(DesktopHitRole.RESIZE_SOUTH));
            regions.add(DesktopWindowRegion.interactive(0, 0, 3, height).withRole(DesktopHitRole.RESIZE_WEST));
            regions.add(DesktopWindowRegion.interactive(width - 3, 0, 3, height).withRole(DesktopHitRole.RESIZE_EAST));
            regions.add(DesktopWindowRegion.interactive(0, 0, 6, 6).withRole(DesktopHitRole.RESIZE_NORTH_WEST));
            regions.add(DesktopWindowRegion.interactive(width - 6, 0, 6, 6).withRole(DesktopHitRole.RESIZE_NORTH_EAST));
            regions.add(
                    DesktopWindowRegion.interactive(0, height - 6, 6, 6).withRole(DesktopHitRole.RESIZE_SOUTH_WEST));
            regions.add(DesktopWindowRegion.interactive(width - 8, height - 8, 8, 8)
                    .withRole(DesktopHitRole.RESIZE_SOUTH_EAST));
        }
        return companion
                ? DesktopWindowSurface.modOwned(regions)
                : new DesktopWindowSurface(DesktopSurfaceMode.SALT, DesktopInsets.ZERO, false,
                        regions, DesktopViewportPolicy.PRESERVE);
    }

    @Override
    public Component title(DesktopWindowContext<AEBaseMenu, State> context) {
        return context.state().content.desktopManagementTitle();
    }

    @Override
    public DesktopWindowDecoration<AEBaseMenu, State> decoration(DesktopWindowContext<AEBaseMenu, State> context) {
        if (context.companion() || context.model().isDesktopToolboxCompanion()) {
            return null;
        }
        var metrics = controlMetrics(context);
        return new DesktopWindowDecoration<>() {
            @Override
            public DesktopChromeMetrics metrics() {
                return metrics;
            }

            @Override
            public void render(DesktopRenderContext<AEBaseMenu, State> frame, List<DesktopControlState> controls) {
                // Keep the manager's committed snapshot. Original AE content paints after decoration,
                // so controls are drawn at the end of that content.
                var window = frame.windowHandle().state();
                if (frame.state().hostScreen == null)
                    frame.state().hostScreen = frame.minecraft().gui.screen();
                if (!window.keyboardFocused())
                    frame.state().keyboardControl = null;
                if (frame.state().keyboardControl != null && controls.stream().noneMatch(
                        control -> control.enabled() && control.control() == frame.state().keyboardControl)) {
                    frame.state().keyboardControl = controls.stream().filter(DesktopControlState::enabled)
                            .map(DesktopControlState::control).findFirst().orElse(null);
                }
                var painted = controls.stream()
                        .map(control -> new DesktopControlState(control.control(), control.bounds(),
                                control.hovered(), control.pressed(), control.enabled(), switch (control.control()) {
                                    case PIN -> window.pinned();
                                    case LOCK -> window.locked();

                                    default -> control.selected();
                                }, control.keyboardFocused() || control.control() == frame.state().keyboardControl))
                        .toList();
                frame.state().managerControls = painted;
                {}

            }
        };
    }

    private DesktopChromeMetrics controlMetrics(DesktopWindowContext<AEBaseMenu, State> context) {
        var content = context.state().content;
        var title = content.desktopManagementTitlePosition();
        int right = context.windowWidth();
        int top = Math.max(0, title.getY() - 2);
        for (var child : content.children()) {
            if (child instanceof net.minecraft.client.gui.components.AbstractWidget widget && widget.visible
                    && widget.getY() - content.getGuiTop() < top + 11
                    && widget.getY() + widget.getHeight() - content.getGuiTop() > top
                    && widget.getX() - content.getGuiLeft() > title.getX()) {
                right = Math.min(right, widget.getX() - content.getGuiLeft());
            }
        }
        var slots = content.desktopSlots().stream().filter(content::desktopSlotVisible).toList();
        boolean intersects = false, fitsAbove = true;
        for (var slot : slots) {
            if (slot.x - 1 < right && slot.x + 17 > right - 100) {
                intersects |= slot.y - 1 < top + 11 && slot.y + 17 > top;
                fitsAbove &= !(slot.y - 1 < top + 9 && slot.y + 17 > top - 2);
            }
        }
        // Native Drive/assembler slots can touch the last two pixels of the original title band.
        // Use the clear band just above them instead of overlaying their artwork or shrinking controls.
        if (intersects && fitsAbove && top >= 2)
            top -= 2;
        for (var slot : slots) {
            if (slot.y - 1 < top + 11 && slot.y + 17 > top && slot.x - 1 > title.getX()) {
                right = Math.min(right, slot.x - 1);
            }
        }
        return new DesktopChromeMetrics(19, Math.max(0, title.getX()), Math.max(0, title.getY()),
                11, 11, 3, context.windowWidth() - right + 2, top, 3);
    }

    @Override
    public void prepareLayout(DesktopWindowContext<AEBaseMenu, State> context) {
        layout(context);
    }

    @Override
    public void narrate(DesktopWindowContext<AEBaseMenu, State> context,
            net.minecraft.client.gui.narration.NarrationElementOutput output) {
        output.add(net.minecraft.client.gui.narration.NarratedElementType.TITLE, title(context));
        if (context.state().keyboardControl != null) {
            output.add(net.minecraft.client.gui.narration.NarratedElementType.TITLE,
                    AEManagementControls.label(context.state().keyboardControl, context.windowHandle().state()));
        }
        context.state().content.narrateDesktop(output);
    }

    @Override
    public String layoutRevision(DesktopWindowContext<AEBaseMenu, State> context) {
        return "ae-ui-polish-api5-1";
    }

    @Override
    public DesktopWindowSize defaultSize(DesktopWindowSetupContext<AEBaseMenu> context) {
        if (context.model().isDesktopToolboxCompanion()) {
            return DesktopWindowSize.of(68, 80);
        }
        var size = InitScreens.desktopDefaultContentSize(context.model());
        if (context.model() instanceof appeng.menu.me.crafting.CraftingStatusMenu
                && net.minecraft.client.Minecraft.getInstance().getWindow().getGuiScaledWidth() < 400) {
            size[0] = 238;
        }
        return DesktopWindowSize.of(size[0], size[1]);
    }

    @Override
    public DesktopWindowSize minSize(DesktopWindowContext<AEBaseMenu, State> context) {
        var size = context.state().model == context.model() ? context.state().content.desktopPreferredSize()
                : InitScreens.desktopDefaultContentSize(context.model());
        return DesktopWindowSize.of(size[0], size[1]);
    }

    @Override
    public DesktopResizePolicy resizePolicy(DesktopWindowContext<AEBaseMenu, State> context) {
        return switch (context.state().content.desktopResizeKind()) {
            // AE's catalog is virtual; the adapter applies its own slot-pitch snapping.
            case "grid", "list" -> DesktopResizePolicy.FREE;
            default -> DesktopResizePolicy.FIXED;
        };
    }

    @Override
    public void resized(DesktopWindowContext<AEBaseMenu, State> context) {
        // The manager owns the gesture; the native layout must acknowledge its new dimensions.
        context.state().content.requestDesktopSize(context.contentWidth(), context.contentHeight());
    }

    @Override
    public DesktopWindowSize maxSize(DesktopWindowContext<AEBaseMenu, State> context) {
        var window = net.minecraft.client.Minecraft.getInstance().getWindow();
        return DesktopWindowSize.of(window.getGuiScaledWidth(), window.getGuiScaledHeight());
    }

    @Override
    @SuppressWarnings("unchecked")
    public DesktopWindowSize desiredWindowSize(DesktopWindowContext<?, ?> context) {
        {}
        // Initial placement can move the host after prepareLayout. Measure
        // native protrusions only after updating them to this committed frame.
        layout((DesktopWindowContext<AEBaseMenu, State>) context);
        var content = ((State) context.state()).content;
        var requested = content.desktopRequestedSize();
        if (requested == null && content.desktopResizeKind().equals("fixed")) {
            var preferred = content.desktopPreferredSize();
            if (context.contentWidth() != preferred[0] || context.contentHeight() != preferred[1]) {
                requested = preferred;
            }
        }
        if (requested == null && (content.desktopResizeKind().equals("grid")
                || content.usesAeDesktopLayout() && content.desktopResizeKind().equals("list"))) {
            var snapped = content.desktopSnapSize(context.contentWidth(), context.contentHeight());
            if (snapped[0] != context.contentWidth() || snapped[1] != context.contentHeight()) {
                requested = snapped;
            }
        }
        if (content.desktopResizeKind().equals("grid")
                || content.usesAeDesktopLayout() && content.desktopResizeKind().equals("list")) {
            int top = 0, bottom = 0, left = 0, right = 0;
            var frame = content.desktopFrame();
            for (var region : content.desktopExternalBounds()) {
                top = Math.max(top, frame.getY() - region.getY());
                bottom = Math.max(bottom, region.getY() + region.getHeight() - frame.getY() - frame.getHeight());
                left = Math.max(left, frame.getX() - region.getX());
                right = Math.max(right, region.getX() + region.getWidth() - frame.getX() - frame.getWidth());
            }
            int available = net.minecraft.client.Minecraft.getInstance().getWindow().getGuiScaledHeight()
                    - top - bottom - 48;
            var minimum = content.desktopPreferredSize();
            int width = requested == null ? context.contentWidth() : requested[0];
            int height = requested == null ? context.contentHeight() : requested[1];
            int availableWidth = net.minecraft.client.Minecraft.getInstance().getWindow().getGuiScaledWidth()
                    - left - right - 32;
            width = Math.max(minimum[0], Math.min(width, availableWidth));
            var bounded = content.desktopSnapSize(width, Math.max(minimum[1], Math.min(height, available)));
            if (bounded[0] != context.contentWidth() || bounded[1] != context.contentHeight()) {
                requested = bounded;
            }
        }
        if (requested != null) {

            content.requestDesktopSize(requested[0], requested[1]);
        }
        return requested == null ? null : context.surface().insets().surfaceSize(requested[0], requested[1]);
    }

    private void layout(DesktopWindowContext<AEBaseMenu, State> context) {
        context.state().bind(context.model(), context.originalTitle());
        context.state().contentWidth = context.contentWidth();
        context.state().contentHeight = context.contentHeight();
        var player = net.minecraft.client.Minecraft.getInstance().player;
        if (player != null) {
            context.model().setCarried(player.inventoryMenu.getCarried().copy());
        }
        NativeMenuPacketScope.run(context.model(), () -> context.state().content.layoutDesktop(
                context.contentX(), context.contentY(), context.contentWidth(), context.contentHeight(),
                new net.minecraft.client.renderer.Rect2i(context.windowX(), context.windowY(),
                        context.windowWidth(), context.windowHeight())));
        int firstControl = context.state().managerControls.stream()
                .filter(control -> control
                        .control() != com.salts_inventory_update.api.client.desktop.DesktopWindowControl.RESIZE_GRIP)
                .mapToInt(control -> control.bounds().x() - context.windowX()).min().orElse(Integer.MAX_VALUE);
        context.state().content
                .setDesktopTitleRight(firstControl == Integer.MAX_VALUE ? firstControl : firstControl - 6);
        if (!context.keyboardFocused()) {
            context.state().content.setFocused(null);
        }
    }

    private com.salts_inventory_update.compat.ae2.client.DesktopSlotPresentation gestureAppearance(
            DesktopRenderContext<AEBaseMenu, State> context, net.minecraft.world.inventory.Slot slot) {
        var appearance = context.gesture().slot(context.sessionId(), context.menuSlotId(slot));
        return appearance == null || !appearance.selected() || !appearance.eligible() ? null
                : new com.salts_inventory_update.compat.ae2.client.DesktopSlotPresentation(appearance.displayStack(), appearance.countText(),
                        true);
    }

    @Override
    public void environmentChanged(DesktopWindowContext<AEBaseMenu, State> context,
            com.salts_inventory_update.api.client.desktop.DesktopWindowEnvironment environment) {

        var state = context.state();
        if (state.resourceRevision >= 0 && state.resourceRevision != environment.resourceRevision()) {
            state.content.reloadDesktopResources();
        }
        state.resourceRevision = environment.resourceRevision();
        context.state().content.refreshDesktopContentLayout();
    }

    @Override
    public void render(DesktopRenderContext<AEBaseMenu, State> context) {
        layout(context);
        var content = context.state().content;
        if (!context.companion()) {
            String badge = OPEN.values().stream().filter(other -> other.companion
                    && other.sessionId == context.state().sharedToolboxSession)
                    .map(other -> other.toolboxBadge).findFirst().orElse(null);
            content.setDesktopToolboxBadge(badge);
        }

        if (!content.usesAeDesktopLayout()) {
            context.fill(context.contentX(), context.contentY(), context.contentX() + context.contentWidth(),
                    context.contentY() + context.contentHeight(), content.desktopBackgroundColor());
        }
        context.withClip(context.contentX(), context.contentY(), context.contentWidth(), context.contentHeight(),
                () -> NativeMenuPacketScope.run(context.model(),
                        () -> content.extractDesktopContent(context.nativeGraphics(), context.state().mouseX,
                                context.state().mouseY)));
        if (content.usesAeDesktopLayout()) {
            content.extractDesktopRealSlots(context.nativeGraphics(), false,
                    context.state().mouseX, context.state().mouseY, slot -> gestureAppearance(context, slot));
        } else
            for (int id = 0; id < context.model().slots.size(); id++) {
                var slot = context.model().slots.get(id);
                if (content.desktopSlotVisible(slot)
                        && !content.desktopExternalSlot(slot)) {
                    context.slot(id, content.getGuiLeft() + slot.x, content.getGuiTop() + slot.y);
                }
            }
        if (!content.usesAeDesktopLayout()) {
            content.extractDesktopSlotDecorations(context.nativeGraphics());
        }
        if (!context.companion()) {
            AEManagementControls.render(context, context.state().managerControls);
        }

    }

    @Override
    public void renderOverlay(DesktopRenderContext<AEBaseMenu, State> context) {
        var content = context.state().content;
        content.extractDesktopAttachments(context.nativeGraphics(), context.state().mouseX, context.state().mouseY);
        if (content.usesAeDesktopLayout()) {
            content.extractDesktopRealSlots(context.nativeGraphics(), true,
                    context.state().mouseX, context.state().mouseY, slot -> gestureAppearance(context, slot));
        } else
            for (int id = 0; id < context.model().slots.size(); id++) {
                var slot = context.model().slots.get(id);
                if (content.desktopSlotVisible(slot) && content.desktopExternalSlot(slot)) {
                    context.slot(id, content.getGuiLeft() + slot.x, content.getGuiTop() + slot.y);
                }
            }
        if (!content.usesAeDesktopLayout()) {
            content.extractDesktopSlotDecorations(context.nativeGraphics(), true);
        }

    }

    @Override
    public DesktopSlotHit slotAt(DesktopSlotContext<AEBaseMenu, State> context, double x, double y) {
        layout(context);
        var content = context.state().content;
        var slot = content.desktopSlotAt(x, y);
        if (slot == null || slot instanceof FakeSlot || slot instanceof CraftingTermSlot
                || !context.model().slots.contains(slot)) {
            return null;
        }
        return context.menuSlotHit(slot.index, content.getGuiLeft() + slot.x, content.getGuiTop() + slot.y, x, y);
    }

    private boolean input(DesktopInputContext<AEBaseMenu, State> context, java.util.function.BooleanSupplier callback) {
        layout(context);
        boolean[] handled = { false };
        NativeMenuPacketScope.run(context.model(),
                () -> context.state().content.withDesktopActions(new NativeContentActions() {
                    @Override
                    public boolean clickSlot(int slot, int button, ContainerInput action) {
                        return context.clickSlot(slot, button, action);
                    }

                    @Override
                    public void close() {
                        context.windowHandle().close();
                    }

                    @Override
                    public void switchContent(AEBaseScreen<?> content) {
                        context.state().setContent(content);
                        layout(context);
                    }
                }, () -> handled[0] = callback.getAsBoolean()));
        context.model().flushDesktopActions(context::sendPayload);
        return handled[0];
    }

    @Override
    public boolean mouseClicked(DesktopInputContext<AEBaseMenu, State> context, MouseButtonEvent event,
            boolean doubleClick) {
        if (!context.state().activatingHeaderControl)
            context.state().keyboardControl = null;
        layout(context);

        return input(context, () -> context.state().content.mouseClicked(event, doubleClick));
    }

    @Override
    public boolean mouseDragged(DesktopInputContext<AEBaseMenu, State> context, MouseButtonEvent event, double dx,
            double dy) {
        return input(context, () -> context.state().content.mouseDragged(event, dx, dy));
    }

    @Override
    public boolean mouseReleased(DesktopInputContext<AEBaseMenu, State> context, MouseButtonEvent event) {

        return input(context, () -> context.state().content.mouseReleased(event));
    }

    @Override
    public boolean mouseScrolled(DesktopInputContext<AEBaseMenu, State> context, double x, double y, double sx,
            double sy) {
        return input(context, () -> context.state().content.mouseScrolled(x, y, sx, sy));
    }

    @Override
    public boolean keyPressed(DesktopInputContext<AEBaseMenu, State> context, KeyEvent event) {
        if (AEManagementControls.keyPressed(context, event))
            return true;
        return input(context, () -> context.state().content.keyPressed(event));
    }

    @Override
    public boolean charTyped(DesktopInputContext<AEBaseMenu, State> context, CharacterEvent event) {
        if (context.state().keyboardControl != null)
            return true;
        return input(context, () -> context.state().content.charTyped(event));
    }

    @Override
    public boolean wantsTextInput(DesktopWindowContext<AEBaseMenu, State> context) {
        return context.keyboardFocused() && context.state().keyboardControl == null
                && context.state().content.desktopWantsTextInput();
    }

    @Override
    public void keyboardFocusChanged(DesktopWindowContext<AEBaseMenu, State> context, boolean focused) {

        if (!focused) {
            context.state().keyboardControl = null;
            context.state().content.setFocused(null);
            context.state().content.cancelDesktopPointer();
        }
    }

    @Override
    public void tick(DesktopWindowContext<AEBaseMenu, State> context) {
        context.state().bind(context.model(), context.originalTitle());
        var state = context.state();
        if (context.companion() && !state.pendingCompanionDock) {
            var parent = OPEN.values().stream().filter(owner -> !owner.companion
                    && state.initialOwners.contains(owner.sessionId)).findFirst().orElse(null);
            if (parent != null && parent.handle.state().visible()) {
                var ownerState = parent.handle.state();
                var body = new net.minecraft.client.renderer.Rect2i(ownerState.x(), ownerState.y(),
                        ownerState.width(), ownerState.height());
                if (parent.content.getClass() != state.dockedOwnerView
                        || body.getWidth() != state.dockedOwnerWidth || body.getHeight() != state.dockedOwnerHeight) {
                    var position = state.handle.state();
                    boolean overlaps = position.x() < body.getX() + body.getWidth()
                            && position.x() + 68 > body.getX() && position.y() < body.getY() + body.getHeight()
                            && position.y() + 80 > body.getY();
                    if (!state.userPlaced || overlaps) {
                        state.pendingCompanionDock = true;
                        state.dockAge = state.stableDockTicks = 0;
                        state.previousDockOwnerBounds = null;

                    } else {
                        rememberDock(state, parent, body);
                    }
                }
            }
        }
        state.automaticMove = true;
        try {
            if (state.pendingCompanionDock && dockCompanion(state, state.initialOwners)) {
                state.pendingCompanionDock = false;
                state.readyRenderFrames = 0;
            }
        } finally {
            state.automaticMove = false;
        }

        NativeMenuPacketScope.run(context.model(), () -> context.state().content.containerTick());
    }

    @Override
    public void customPayload(DesktopWindowContext<AEBaseMenu, State> context,
            net.minecraft.resources.Identifier channel, byte[] data) {
        if (channel.equals(com.salts_inventory_update.compat.ae2.server.AE2ServerIntegration.TOOLBOX_CHANNEL)) {
            if (data.length != 4) {
                throw new IllegalArgumentException("Invalid AE toolbox association data");
            }
            var state = context.state();
            state.sharedToolboxSession = java.nio.ByteBuffer.wrap(data).getInt();
            state.content.setDesktopSharedToolbox(state.sharedToolboxSession >= 0);
            context.windowHandle().cancelInput();
        }
        if (channel.toString().equals("ae2:desktop_cpu_status")
                && context.model() instanceof appeng.menu.me.crafting.CraftingCPUMenu cpu) {
            var status = com.salts_inventory_update.compat.ae2.menu.DesktopCpuState.decode(data);
            cpu.cpuStatusReady = status.ready();
            cpu.cpuAvailable = status.available();
            cpu.hasActiveJob = status.activeJob();
            cpu.cpuStatusSerial = status.serial();
        }

    }

    @Override
    public com.salts_inventory_update.api.client.desktop.DesktopWindowOptions options(
            DesktopWindowContext<AEBaseMenu, State> context) {
        return context.companion()
                ? com.salts_inventory_update.api.client.desktop.DesktopWindowOptions.DRAG_ONLY
                : com.salts_inventory_update.api.client.desktop.DesktopWindowOptions.DEFAULT;
    }

    @Override
    public void closed(DesktopWindowContext<AEBaseMenu, State> context) {

        OPEN.remove(context.model().desktopSyncId());
        NativeContentRegistry.remove(context.model().desktopSyncId());
        context.model().discardDesktopActions();
        if (context.state().model == context.model()) {
            context.state().content.desktopClosed();
        }
    }

    @Override
    public void cancelPointer(DesktopWindowContext<?, ?> context) {
        var state = (State) context.state();
        state.content.cancelDesktopPointer();

    }

    @Override
    public List<DesktopWindowRegion> externalRegions(DesktopWindowContext<?, ?> context) {
        var content = ((State) context.state()).content;
        var origin = content.desktopFrame();
        var regions = new java.util.ArrayList<>(content.desktopExternalBounds().stream()
                .map(rect -> DesktopWindowRegion.visualAndInteractive(
                        rect.getX() - origin.getX(), rect.getY() - origin.getY(), rect.getWidth(), rect.getHeight()))
                .toList());
        if (!content.desktopResizeKind().equals("fixed")) {
            regions.add(DesktopWindowRegion.visualAndInteractive(origin.getWidth() - 8, origin.getHeight() - 8, 8, 8));
        }
        return regions;
    }

    @Override
    public boolean appendTooltip(DesktopRenderContext<AEBaseMenu, State> context, int x, int y) {
        context.state().mouseX = x;
        context.state().mouseY = y;
        context.state().content.extractDesktopTooltip(context.nativeGraphics(), x, y);
        return true;
    }

    @Override
    public void updateTooltipState(DesktopWindowContext<?, ?> context, int x, int y, boolean active) {
        var state = (State) context.state();
        state.mouseX = active ? x : -10000;
        state.mouseY = active ? y : -10000;
    }

    @Override
    public boolean keyReleased(DesktopInputContext<AEBaseMenu, State> context, KeyEvent event) {
        return input(context, () -> context.state().content.keyReleased(event));
    }

    @Override
    public void saveLocalState(DesktopWindowContext<AEBaseMenu, State> context, CompoundTag tag) {
        tag.putString("ae2.layoutRevision", "ae-surface-4");
        context.state().content.saveDesktopPreferences(tag);
    }

    @Override
    public void loadLocalState(DesktopWindowContext<AEBaseMenu, State> context, CompoundTag tag) {
        context.state().bind(context.model(), context.originalTitle());
        NativeMenuPacketScope.run(context.model(), () -> context.state().content.loadDesktopPreferences(tag));
        if (context.state().content.usesAeDesktopLayout()
                && !"ae-surface-4".equals(tag.getStringOr("ae2.layoutRevision", ""))) {
            var size = InitScreens.desktopDefaultContentSize(context.model());
            context.state().content.requestDesktopSize(size[0], size[1]);
        }
    }
}
