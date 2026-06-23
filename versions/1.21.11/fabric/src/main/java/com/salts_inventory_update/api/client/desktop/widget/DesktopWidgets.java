package com.salts_inventory_update.api.client.desktop.widget;

import java.util.List;

import org.lwjgl.glfw.GLFW;

import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import com.salts_inventory_update.api.client.desktop.DesktopInputContext;
import com.salts_inventory_update.api.client.desktop.DesktopRenderContext;
import com.salts_inventory_update.api.client.desktop.DesktopWindowContext;

public final class DesktopWidgets {
    public static final int SLOT_SIZE = 18;
    public static final int SEARCH_BAR_HEIGHT = 12;
    public static final int SCROLLBAR_BACKGROUND_WIDTH = 14;
    public static final int SCROLLBAR_THUMB_WIDTH = 12;
    public static final int SCROLLBAR_THUMB_HEIGHT = 15;
    public static final Identifier SEARCH_BAR_TEXTURE = Identifier.fromNamespaceAndPath("salts_inventory_update", "textures/gui/search_bar.png");
    public static final Identifier SCROLLBAR_BACKGROUND_TEXTURE = Identifier.fromNamespaceAndPath("salts_inventory_update", "textures/gui/scroll_bar_behind.png");
    public static final Identifier SCROLLER_SPRITE = Identifier.parse("container/creative_inventory/scroller");
    public static final Identifier SCROLLER_DISABLED_SPRITE = Identifier.parse("container/creative_inventory/scroller_disabled");

    private static final int SEARCH_TEXTURE_WIDTH = 3;
    private static final int SEARCH_TEXTURE_HEIGHT = 12;

    private DesktopWidgets() {
    }

    public static void renderTextBox(DesktopRenderContext<?, ?> context, DesktopTextBoxState state, int x, int y, int width) {
        renderTextBox(context, state.text(), state.focused(), x, y, width);
    }

    public static void renderTextBox(DesktopRenderContext<?, ?> context, String text, boolean focused, int x, int y, int width) {
        int clampedWidth = Math.max(2, width);
        context.texture(SEARCH_BAR_TEXTURE, x, y, 0, 0, 1, SEARCH_BAR_HEIGHT, 1, SEARCH_BAR_HEIGHT, SEARCH_TEXTURE_WIDTH, SEARCH_TEXTURE_HEIGHT);
        if (clampedWidth > 2) {
            context.texture(SEARCH_BAR_TEXTURE, x + 1, y, 1, 0, clampedWidth - 2, SEARCH_BAR_HEIGHT, 1, SEARCH_BAR_HEIGHT, SEARCH_TEXTURE_WIDTH, SEARCH_TEXTURE_HEIGHT);
        }
        context.texture(SEARCH_BAR_TEXTURE, x + clampedWidth - 1, y, 2, 0, 1, SEARCH_BAR_HEIGHT, 1, SEARCH_BAR_HEIGHT, SEARCH_TEXTURE_WIDTH, SEARCH_TEXTURE_HEIGHT);
        String visible = context.trimToWidth(text, Math.max(0, clampedWidth - 8));
        context.text(visible, x + 4, y + 2, 0xFFFFFFFF, false);
        if (focused && (System.currentTimeMillis() / 300L) % 2L == 0L) {
            int cursorX = x + 4 + context.fontWidth(visible);
            context.fill(cursorX, y + 2, cursorX + 1, y + 10, 0xFFFFFFFF);
        }
    }

    public static boolean clickTextBox(DesktopTextBoxState state, MouseButtonEvent event, int x, int y, int width) {
        if (event.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return false;
        }

        boolean focused = contains(event.x(), event.y(), x, y, width, SEARCH_BAR_HEIGHT);
        state.focused(focused);
        return focused;
    }

    public static boolean keyPressedTextBox(DesktopTextBoxState state, KeyEvent event) {
        if (!state.focused()) {
            return false;
        }

        if (event.key() == GLFW.GLFW_KEY_ESCAPE || event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_KP_ENTER) {
            state.focused(false);
            return true;
        }
        if (event.key() == GLFW.GLFW_KEY_BACKSPACE && !state.text().isEmpty()) {
            state.text(state.text().substring(0, state.text().length() - 1));
            return true;
        }
        if (event.key() == GLFW.GLFW_KEY_DELETE) {
            state.text("");
            return true;
        }
        return true;
    }

    public static boolean charTypedTextBox(DesktopTextBoxState state, CharacterEvent event) {
        if (!state.focused() || !event.isAllowedChatCharacter()) {
            return false;
        }

        state.text(state.text() + event.codepointAsString());
        return true;
    }

    public static void renderIconButton(DesktopRenderContext<?, ?> context, Identifier icon, int x, int y, int size, boolean active, boolean hovered) {
        int fill = active ? 0xFF63B75B : hovered ? 0xFFB6B6B6 : 0xFF9D9D9D;
        int border = hovered ? 0xFF202020 : 0xFF4A4A4A;
        context.fill(x, y, x + size, y + size, border);
        context.fill(x + 1, y + 1, x + size - 1, y + size - 1, fill);
        context.sprite(icon, x + 2, y + 2, Math.max(1, size - 4), Math.max(1, size - 4));
    }

    public static void renderTextButton(DesktopRenderContext<?, ?> context, Component text, int x, int y, int width, int height, boolean active, boolean hovered) {
        int fill = active ? 0xFF63B75B : hovered ? 0xFFB6B6B6 : 0xFF9D9D9D;
        int border = hovered ? 0xFF202020 : 0xFF4A4A4A;
        context.fill(x, y, x + width, y + height, border);
        context.fill(x + 1, y + 1, x + width - 1, y + height - 1, fill);
        String visible = context.trimToWidth(text.getString(), Math.max(0, width - 4));
        context.text(visible, x + Math.max(2, (width - context.fontWidth(visible)) / 2), y + Math.max(2, (height - 8) / 2), active ? 0xFF103D12 : 0xFF303030, false);
    }

    public static void renderScrollbar(DesktopRenderContext<?, ?> context, int x, int y, int height, int scroll, int maxScroll) {
        renderScrollbarBackground(context, x, y, height);
        boolean scrollable = maxScroll > 0;
        int track = Math.max(0, height - 2 - SCROLLBAR_THUMB_HEIGHT);
        int offset = scrollable ? Math.round((float) clamp(scroll, 0, maxScroll) / (float) maxScroll * track) : 0;
        context.sprite(scrollable ? SCROLLER_SPRITE : SCROLLER_DISABLED_SPRITE, x + 1, y + 1 + offset, SCROLLBAR_THUMB_WIDTH, SCROLLBAR_THUMB_HEIGHT);
    }

    public static void renderScrollbarBackground(DesktopRenderContext<?, ?> context, int x, int y, int height) {
        int clampedHeight = Math.max(3, height);
        context.texture(SCROLLBAR_BACKGROUND_TEXTURE, x, y, 0, 0, SCROLLBAR_BACKGROUND_WIDTH, 1, SCROLLBAR_BACKGROUND_WIDTH, 1, SCROLLBAR_BACKGROUND_WIDTH, 3);
        if (clampedHeight > 2) {
            context.texture(SCROLLBAR_BACKGROUND_TEXTURE, x, y + 1, 0, 1, SCROLLBAR_BACKGROUND_WIDTH, clampedHeight - 2, SCROLLBAR_BACKGROUND_WIDTH, 1, SCROLLBAR_BACKGROUND_WIDTH, 3);
        }
        context.texture(SCROLLBAR_BACKGROUND_TEXTURE, x, y + clampedHeight - 1, 0, 2, SCROLLBAR_BACKGROUND_WIDTH, 1, SCROLLBAR_BACKGROUND_WIDTH, 1, SCROLLBAR_BACKGROUND_WIDTH, 3);
    }

    public static int scrollByWheel(int scroll, int maxScroll, double scrollY) {
        if (maxScroll <= 0 || scrollY == 0.0D) {
            return clamp(scroll, 0, maxScroll);
        }

        return clamp(scroll + (scrollY < 0.0D ? 1 : -1), 0, maxScroll);
    }

    public static void renderDropdown(DesktopRenderContext<?, ?> context, int x, int y, int width, List<Component> labels, List<Component> states, int hoveredIndex) {
        int rowHeight = 18;
        int padding = 6;
        int height = padding * 2 + labels.size() * rowHeight;
        context.windowNineSlice(Identifier.fromNamespaceAndPath("salts_inventory_update", "textures/gui/window.png"), x, y, width, height);
        for (int i = 0; i < labels.size(); i++) {
            int rowY = y + padding + i * rowHeight;
            if (i == hoveredIndex) {
                context.fill(x + padding, rowY, x + width - padding, rowY + rowHeight, 0x22999999);
            }
            Component state = i < states.size() ? states.get(i) : Component.empty();
            int stateWidth = Math.max(34, context.fontWidth(state) + 10);
            int buttonX = x + width - padding - stateWidth;
            context.text(context.trimToWidth(labels.get(i).getString(), buttonX - x - padding * 2), x + padding + 2, rowY + 5, 0xFF303030, false);
            renderTextButton(context, state, buttonX, rowY + 2, stateWidth, 14, "On".equalsIgnoreCase(state.getString()), i == hoveredIndex);
        }
    }

    public static void renderVirtualItemGrid(
        DesktopRenderContext<?, ?> context,
        List<DesktopVirtualItem> items,
        int firstIndex,
        int x,
        int y,
        int columns,
        int rows
    ) {
        int max = Math.min(items.size(), firstIndex + columns * rows);
        for (int i = firstIndex; i < max; i++) {
            int visual = i - firstIndex;
            DesktopVirtualItem item = items.get(i);
            context.virtualItem(item.stack(), item.count(), x + visual % columns * SLOT_SIZE, y + visual / columns * SLOT_SIZE);
        }
    }

    public static int virtualItemIndexAt(double mouseX, double mouseY, int x, int y, int columns, int rows, int firstIndex, int itemCount) {
        if (!contains(mouseX, mouseY, x, y, columns * SLOT_SIZE, rows * SLOT_SIZE)) {
            return -1;
        }
        int column = (int) ((mouseX - x) / SLOT_SIZE);
        int row = (int) ((mouseY - y) / SLOT_SIZE);
        int index = firstIndex + row * columns + column;
        return index >= 0 && index < itemCount ? index : -1;
    }

    public static boolean contains(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;
    }

    public static int rowsForCount(int count, int columns) {
        return count <= 0 ? 0 : (count + Math.max(1, columns) - 1) / Math.max(1, columns);
    }

    public static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    public static boolean wantsTextInput(DesktopTextBoxState state) {
        return state.focused();
    }

    public static void unfocus(DesktopTextBoxState state) {
        state.focused(false);
    }
}
