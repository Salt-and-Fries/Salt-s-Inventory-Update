package com.salts_inventory_update.api.client.desktop.widget;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import com.salts_inventory_update.api.client.input.CharacterEvent;
import com.salts_inventory_update.api.client.input.KeyEvent;
import com.salts_inventory_update.api.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import com.salts_inventory_update.api.client.desktop.DesktopRenderContext;

/** Per-window native widget routing. Does not create or replace an active Minecraft Screen. */
public final class DesktopWidgetHost {
    private record Entry(String id, AbstractWidget widget, Component label, Runnable cancellation) { }
    private final List<Entry> entries = new ArrayList<>();
    private @Nullable Entry focused;
    private @Nullable Entry captured;
    private boolean hoverActive = true;

    public DesktopWidgetHost add(String id, AbstractWidget widget, Component label) {
        return add(id, widget, label, () -> { });
    }
    /** Custom drag widgets supply a cancellation hook; it must not commit a release action. */
    public DesktopWidgetHost add(String id, AbstractWidget widget, Component label, Runnable cancellation) {
        Objects.requireNonNull(id); Objects.requireNonNull(widget); Objects.requireNonNull(label); Objects.requireNonNull(cancellation);
        if (id.isBlank() || entries.stream().anyMatch(entry -> entry.id().equals(id) || entry.widget() == widget))
            throw new IllegalArgumentException("Duplicate/blank widget id or reused widget");
        entries.add(new Entry(id, widget, label, cancellation));
        return this;
    }
    private void pruneInactive() {
        if (focused != null && (!focused.widget().visible || !focused.widget().active)) clearFocus();
        if (captured != null && (!captured.widget().visible || !captured.widget().active)) cancelPointer();
    }
    public boolean focusNext(boolean forward) {
        pruneInactive();
        List<Entry> available = entries.stream().filter(entry -> entry.widget().visible && entry.widget().active).toList();
        if (available.isEmpty()) { clearFocus(); return false; }
        int index = available.indexOf(focused);
        int next = index < 0 ? (forward ? 0 : available.size() - 1) : Math.floorMod(index + (forward ? 1 : -1), available.size());
        setFocused(available.get(next));
        return true;
    }
    private void setFocused(@Nullable Entry entry) {
        focused = entry;
        for (Entry candidate : entries) candidate.widget().setFocused(candidate == entry);
    }
    public void clearFocus() { setFocused(null); }
    public void hoverActive(boolean active) { hoverActive = active; }
    public @Nullable String focusedId() { return focused == null ? null : focused.id(); }
    public void render(DesktopRenderContext<?, ?> context) {
        pruneInactive();
        for (Entry entry : entries) if (entry.widget().visible) {
            entry.widget().render(context.nativeGraphics(), hoverActive ? context.mouseX() : Integer.MIN_VALUE,
                hoverActive ? context.mouseY() : Integer.MIN_VALUE, 0);
        }
    }
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        pruneInactive();
        for (int index = entries.size() - 1; index >= 0; index--) {
            Entry entry = entries.get(index);
            var widget = entry.widget();
            if (widget.visible && widget.active && widget.isMouseOver(event.x(), event.y())) {
                setFocused(entry);
                if (widget.mouseClicked(event.x(), event.y(), event.button())) { captured = entry; return true; }
                return false;
            }
        }
        clearFocus();
        return false;
    }
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        pruneInactive();
        if (captured == null) return false;
        var widget = captured.widget();
        return widget.mouseDragged(event.x(), event.y(), event.button(), dx, dy);
    }
    public boolean mouseReleased(MouseButtonEvent event) {
        pruneInactive();
        Entry entry = captured;
        captured = null;
        if (entry == null) return false;
        var widget = entry.widget();
        return widget.mouseReleased(event.x(), event.y(), event.button());
    }
    public void cancelPointer() {
        Entry entry = captured;
        captured = null;
        if (entry != null) entry.cancellation().run();
    }
    public boolean keyPressed(KeyEvent event) {
        pruneInactive();
        if (focused == null) return false;
        var widget = focused.widget();
        return widget.keyPressed(event.key(), event.scancode(), event.modifiers());
    }
    public boolean keyReleased(KeyEvent event) {
        pruneInactive();
        if (focused == null) return false;
        var widget = focused.widget();
        return widget.keyReleased(event.key(), event.scancode(), event.modifiers());
    }
    public boolean charTyped(CharacterEvent event) {
        pruneInactive();
        if (focused == null) return false;
        var widget = focused.widget();
        return widget.charTyped(event.codepoint(), event.modifiers());
    }
    public void narrate(NarrationElementOutput output) {
        pruneInactive();
        if (focused != null && focused.widget().visible && focused.widget().active) {
            var nested = output.nest();
            focused.widget().updateNarration(nested);
            nested.add(net.minecraft.client.gui.narration.NarratedElementType.HINT, focused.label());
        }
    }
}
