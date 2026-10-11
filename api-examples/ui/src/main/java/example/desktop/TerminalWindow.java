package example.desktop;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.stream.IntStream;

import com.salts_inventory_update.api.client.desktop.*;
import com.salts_inventory_update.api.client.desktop.widget.*;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/** Virtualized catalog, real slots, tabs, search, nested clips, external panel and modal confirmation. */
public final class TerminalWindow implements DesktopWindowDefinition<TerminalMenu, TerminalWindow.State>, DesktopWindowPresentation {
    public static final class State {
        final DesktopTextBoxState search = new DesktopTextBoxState();
        final DesktopScrollState scroll = new DesktopScrollState();
        int[] rows = IntStream.range(0, TerminalMenu.ROWS).toArray();
        String previousQuery = "";
        int previousTab;
        int tab;
        int selected = -1;
        long confirmedCount;
        boolean inspector;
        boolean confirmation;
        boolean dragging;
        boolean compact;
        float animation;
    }

    @Override public State createState(DesktopWindowSetupContext<TerminalMenu> context) { return new State(); }
    @Override public DesktopWindowSize defaultSize(DesktopWindowSetupContext<TerminalMenu> context) { return DesktopWindowSize.of(310, 242); }
    @Override public DesktopWindowSize minSize(DesktopWindowContext<TerminalMenu, State> context) {
        return context.state().compact ? DesktopWindowSize.of(160, 120) : DesktopWindowSize.of(240, 180);
    }
    @Override public void environmentChanged(DesktopWindowContext<TerminalMenu, State> context, DesktopWindowEnvironment environment) {
        context.state().compact = environment.viewportWidth() < 345 || environment.viewportHeight() < 242;
    }
    @Override public void surfaceUnavailable(DesktopWindowContext<TerminalMenu, State> context) { context.state().compact = true; }
    @Override public DesktopWindowSize desiredWindowSize(DesktopWindowContext<?, ?> context) {
        if (!((State) context.state()).compact) return null;
        return DesktopWindowSize.of(Math.max(160, Math.min(context.windowWidth(), context.environment().viewportWidth() - 35)),
            Math.max(120, Math.min(context.windowHeight(), context.environment().viewportHeight())));
    }
    @Override public DesktopWindowSize maxSize(DesktopWindowContext<TerminalMenu, State> context) { return DesktopWindowSize.of(600, 480); }
    @Override public DesktopResizePolicy resizePolicy(DesktopWindowContext<TerminalMenu, State> context) { return DesktopResizePolicy.FREE; }
    @Override public String layoutRevision(DesktopWindowContext<TerminalMenu, State> context) { return "irregular-2"; }
    @Override public DesktopWindowSurface surface(DesktopWindowContext<TerminalMenu, State> context) {
        int w = context.windowWidth(), h = context.windowHeight();
        return new DesktopWindowSurface(DesktopSurfaceMode.MOD_OWNED, new DesktopInsets(8, 24, 8, 8), false, List.of(
            DesktopWindowRegion.visualAndInteractive(18, 0, w - 36, 16).withRole(DesktopHitRole.MOVE),
            DesktopWindowRegion.visualAndInteractive(0, 16, w, h - 16),
            DesktopWindowRegion.interactive(8, 24, w - 16, 16).withRole(DesktopHitRole.CONTROL),
            DesktopWindowRegion.visualAndInteractive(w, 34, 35, 36).withRole(DesktopHitRole.CONTROL),
            DesktopWindowRegion.interactive(w - 8, h - 8, 8, 8).withRole(DesktopHitRole.RESIZE_SOUTH_EAST)), DesktopViewportPolicy.ADAPT);
    }
    @Override public List<DesktopPopup> popups(DesktopWindowContext<TerminalMenu, State> context) {
        return context.state().confirmation ? List.of(new DesktopPopup("confirm",
            List.of(DesktopWindowRegion.visualAndInteractive(8, 58, context.windowWidth() - 16, 48)),
            true, false, true, false)) : List.of();
    }
    @Override public boolean dismissPopup(DesktopWindowContext<TerminalMenu, State> context, String id, DesktopPopup.Dismissal reason) {
        if (!id.equals("confirm") || !context.state().confirmation) return false;
        context.state().confirmation = false;
        return true;
    }
    @Override public DesktopWindowOptions options(DesktopWindowContext<TerminalMenu, State> context) {
        return new DesktopWindowOptions(true, !context.state().confirmation, true, false, true);
    }

    private void layout(DesktopWindowContext<TerminalMenu, State> context) {
        State state = context.state();
        String query = state.search.text().toLowerCase(Locale.ROOT);
        if (!query.equals(state.previousQuery) || state.tab != state.previousTab) {
            state.rows = IntStream.range(0, TerminalMenu.ROWS)
                .filter(row -> (state.tab == 0 || row % 2 == 0) && context.model().label(row).toLowerCase(Locale.ROOT).contains(query)).toArray();
            state.previousQuery = query;
            state.previousTab = state.tab;
        }
        state.scroll.layout(state.rows.length, 20, Math.max(0, context.contentHeight() - 60));
    }

    @Override public void tick(DesktopWindowContext<TerminalMenu, State> context) {
        context.state().animation = (context.state().animation + 0.04F) % 1;
    }

    @Override public void render(DesktopRenderContext<TerminalMenu, State> context) {
        layout(context);
        State state = context.state();
        int x = context.contentX(), y = context.contentY(), width = context.contentWidth();
        context.fill(context.windowX(), context.windowY() + 16, context.windowX() + context.windowWidth(), context.windowY() + context.windowHeight(), 0xFF273A45);
        context.fill(context.windowX() + 18, context.windowY(), context.windowX() + context.windowWidth() - 18, context.windowY() + 16, 0xFF365F73);
        context.text("Catalog", context.windowX() + 24, context.windowY() + 4, 0xFFFFFFFF, false);
        DesktopWidgets.renderTextButton(context, Component.literal("All"), context.windowX() + context.windowWidth(), context.windowY() + 34, 35, 18, state.tab == 0, false);
        DesktopWidgets.renderTextButton(context, Component.literal("Even"), context.windowX() + context.windowWidth(), context.windowY() + 52, 35, 18, state.tab == 1, false);
        DesktopWidgets.renderTextBox(context, state.search, x, y, width - 55);
        DesktopWidgets.renderTextButton(context, Component.literal(state.tab == 0 ? "All" : "Even"), x + width - 50, y, 48, 12, state.tab != 0, false);
        int panelY = y + 18, panelHeight = Math.max(0, context.contentHeight() - 60);
        context.withClip(x, panelY, width, panelHeight, () -> {
            context.fill(x, panelY, x + width, panelY + panelHeight, 0xFF252B33);
            var visible = state.scroll.visibleRows(1);
            for (int i = visible.first(); i < visible.endExclusive(); i++) {
                int row = state.rows[i], rowY = panelY + (int) Math.floor(state.scroll.rowY(i));
                if (row == state.selected) context.fill(x, rowY, x + width, rowY + 20, 0xFF365D80);
                // Nested clips keep icons/labels inside the row's own bounds too.
                context.withClip(x, rowY, width, 20, () -> {
                    context.virtualItem(context.model().icon(row), context.model().count(row), x + 2, rowY + 1);
                    context.text(context.model().label(row), x + 24, rowY + 5, 0xFFFFFFFF, false);
                });
            }
        });
        for (int id = 0; id < 3; id++) OriginalSlotArtwork.draw(context, id, x + id * 20, y + context.contentHeight() - 20);
        DesktopWidgets.renderTextButton(context, Component.literal("Inspect"), x + width - 66, y + context.contentHeight() - 20, 64, 18, state.inspector, false);
    }

    @Override public void renderOverlay(DesktopRenderContext<TerminalMenu, State> context) {
        State state = context.state();
        if (state.inspector && !state.compact) {
            int x = context.windowX() + context.windowWidth() + 35, y = context.windowY() + 24;
            context.fill(x, y, x + 90, y + 52, 0xFF29333E);
            context.text("Row " + state.selected, x + 4, y + 5, 0xFFFFFFFF, false);
            context.text("Count " + state.confirmedCount, x + 4, y + 18, 0xFFFFFFFF, false);
            DesktopWidgets.renderTextButton(context, Component.literal("Select"), x + 4, y + 32, 80, 16, false, false);
        }
        if (state.confirmation) {
            int x = context.contentX(), y = context.contentY() + 34;
            context.fill(x, y, x + context.contentWidth(), y + 48, 0xFF18202D);
            context.text("Confirm row " + state.selected + "?", x + 6, y + 6, 0xFFFFFFFF, false);
            DesktopWidgets.renderTextButton(context, Component.literal("Confirm"), x + 6, y + 24, 90, 18, true, false);
        }
    }

    @Override public List<DesktopWindowRegion> externalRegions(DesktopWindowContext<?, ?> context) {
        State state = (State) context.state();
        return state.inspector && !state.compact
            ? List.of(DesktopWindowRegion.visualAndInteractive(context.windowWidth() + 35, 24, 90, 52)) : List.of();
    }

    private int rowAt(DesktopWindowContext<TerminalMenu, State> context, double x, double y) {
        layout(context);
        if (x < context.contentX() || x >= context.contentX() + context.contentWidth()) return -1;
        int index = context.state().scroll.rowAt(y - context.contentY() - 18);
        return index < 0 ? -1 : context.state().rows[index];
    }

    @Override public DesktopSlotHit slotAt(DesktopSlotContext<TerminalMenu, State> context, double x, double y) {
        if (context.state().confirmation) return null;
        for (int id = 0; id < 3; id++) {
            DesktopSlotHit hit = context.menuSlotHit(id, context.contentX() + id * 20, context.contentY() + context.contentHeight() - 20, x, y);
            if (hit != null) return hit;
        }
        return null;
    }

    @Override public boolean mouseClicked(DesktopInputContext<TerminalMenu, State> context, MouseButtonEvent event, boolean doubleClick) {
        State state = context.state();
        int x = context.contentX(), y = context.contentY(), width = context.contentWidth();
        if (state.confirmation) {
            if (DesktopWidgets.contains(event.x(), event.y(), x + 6, y + 58, 90, 18) && state.selected >= 0) {
                context.sendPayload(ExampleCommonIntegration.SELECT, Integer.toString(state.selected).getBytes(StandardCharsets.UTF_8));
                state.confirmation = false;
            }
            return true;
        }
        if (DesktopWidgets.contains(event.x(), event.y(), context.windowX() + context.windowWidth(), context.windowY() + 34, 35, 36)) {
            state.tab = event.y() < context.windowY() + 52 ? 0 : 1;
            return true;
        }
        if (DesktopWidgets.clickTextBox(state.search, event, x, y, width - 55)) return true;
        if (DesktopWidgets.contains(event.x(), event.y(), x + width - 50, y, 48, 12)) { state.tab = 1 - state.tab; return true; }
        if (DesktopWidgets.contains(event.x(), event.y(), x + width - 66, y + context.contentHeight() - 20, 64, 18)) {
            state.inspector = !state.inspector;
            return true;
        }
        if (state.inspector && !state.compact && DesktopWidgets.contains(event.x(), event.y(), context.windowX() + context.windowWidth() + 39, context.windowY() + 56, 80, 16)) {
            state.confirmation = state.selected >= 0;
            return true;
        }
        int row = rowAt(context, event.x(), event.y());
        if (row >= 0) { state.selected = row; state.dragging = true; return true; }
        return false;
    }

    @Override public boolean mouseDragged(DesktopInputContext<TerminalMenu, State> context, MouseButtonEvent event, double dx, double dy) {
        if (!context.state().dragging) return false;
        int row = rowAt(context, event.x(), event.y());
        if (row >= 0) context.state().selected = row;
        return true;
    }
    @Override public boolean mouseReleased(DesktopInputContext<TerminalMenu, State> context, MouseButtonEvent event) {
        boolean handled = context.state().dragging;
        context.state().dragging = false;
        return handled;
    }
    @Override public void cancelPointer(DesktopWindowContext<?, ?> context) { ((State) context.state()).dragging = false; }
    @Override public boolean mouseScrolled(DesktopInputContext<TerminalMenu, State> context, double x, double y, double sx, double sy) {
        if (rowAt(context, x, y) < 0) return false;
        context.state().scroll.scroll(-sy * 20);
        return true;
    }
    @Override public boolean keyPressed(DesktopInputContext<TerminalMenu, State> context, KeyEvent event) {
        if (event.isEscape() && context.state().confirmation) { context.state().confirmation = false; return true; }
        return DesktopWidgets.keyPressedTextBox(context.state().search, event, 64);
    }
    @Override public boolean charTyped(DesktopInputContext<TerminalMenu, State> context, CharacterEvent event) {
        return DesktopWidgets.charTypedTextBox(context.state().search, event, 64);
    }
    @Override public boolean wantsTextInput(DesktopWindowContext<TerminalMenu, State> context) { return context.state().search.focused(); }
    @Override public void keyboardFocusChanged(DesktopWindowContext<TerminalMenu, State> context, boolean focused) {
        if (!focused) context.state().search.focused(false);
    }
    @Override public boolean appendTooltip(DesktopRenderContext<TerminalMenu, State> context, int x, int y) {
        int row = rowAt(context, x, y);
        if (row < 0) return false;
        context.tooltip(List.of(Component.literal(context.model().label(row)), Component.literal("Stored: " + context.model().count(row))), x, y);
        return true;
    }
    @Override public void customPayload(DesktopWindowContext<TerminalMenu, State> context, Identifier channel, byte[] data) {
        if (!channel.equals(ExampleCommonIntegration.RESULT) || data.length != 12) return;
        var buffer = ByteBuffer.wrap(data);
        int row = buffer.getInt();
        long count = buffer.getLong();
        if (row < 0 || row >= TerminalMenu.ROWS || count < 0) return;
        context.state().selected = row;
        context.state().confirmedCount = count;
    }
    @Override public void saveLocalState(DesktopWindowContext<TerminalMenu, State> context, CompoundTag tag) { tag.putString("query", context.state().search.text()); }
    @Override public void loadLocalState(DesktopWindowContext<TerminalMenu, State> context, CompoundTag tag) { context.state().search.text(tag.getStringOr("query", "")); }
}
