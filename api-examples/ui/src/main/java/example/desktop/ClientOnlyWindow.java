package example.desktop;

import com.salts_inventory_update.api.client.desktop.*;
import com.salts_inventory_update.api.client.desktop.widget.DesktopWidgetHost;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** Native widgets belong to each instance; no active Screen or menu is replaced. */
public class ClientOnlyWindow implements DesktopWindowDefinition<PreferencesModel, ClientOnlyWindow.State>, DesktopWindowPresentation {
    public record State(EditBox edit, Button reset, DesktopWidgetHost host) { }
    @Override public State createState(DesktopWindowSetupContext<PreferencesModel> context) {
        var edit = new EditBox(context.minecraft().font, 0, 0, 180, 20, Component.literal("Caption"));
        edit.setMaxLength(64);
        edit.setValue(context.model().caption);
        edit.setResponder(value -> context.model().caption = value);
        var reset = Button.builder(Component.literal("Reset"), button -> edit.setValue(""))
            .bounds(0, 0, 80, 20).build();
        var host = new DesktopWidgetHost().add("caption", edit, Component.literal("Window caption"))
            .add("reset", reset, Component.literal("Clear the caption"));
        return new State(edit, reset, host);
    }
    @Override public DesktopWindowSize defaultSize(DesktopWindowSetupContext<PreferencesModel> context) { return DesktopWindowSize.of(212, 112); }
    @Override public DesktopWindowOptions options(DesktopWindowContext<PreferencesModel, State> context) {
        return new DesktopWindowOptions(true, true, true, false, true);
    }
    @Override public void prepareLayout(DesktopWindowContext<PreferencesModel, State> context) {
        context.state().edit().setX(context.contentX() + 4);
        context.state().edit().setY(context.contentY() + 10);
        context.state().reset().setX(context.contentX() + 4);
        context.state().reset().setY(context.contentY() + 40);
    }
    @Override public void render(DesktopRenderContext<PreferencesModel, State> context) { context.state().host().render(context); }
    @Override public boolean mouseClicked(DesktopInputContext<PreferencesModel, State> context, MouseButtonEvent event, boolean doubleClick) { return context.state().host().mouseClicked(event, doubleClick); }
    @Override public boolean mouseDragged(DesktopInputContext<PreferencesModel, State> context, MouseButtonEvent event, double dx, double dy) { return context.state().host().mouseDragged(event, dx, dy); }
    @Override public boolean mouseReleased(DesktopInputContext<PreferencesModel, State> context, MouseButtonEvent event) { return context.state().host().mouseReleased(event); }
    @Override public boolean keyPressed(DesktopInputContext<PreferencesModel, State> context, KeyEvent event) { return context.state().host().keyPressed(event); }
    @Override public boolean keyReleased(DesktopInputContext<PreferencesModel, State> context, KeyEvent event) { return context.state().host().keyReleased(event); }
    @Override public boolean charTyped(DesktopInputContext<PreferencesModel, State> context, CharacterEvent event) { return context.state().host().charTyped(event); }
    @Override public boolean focusNext(DesktopWindowContext<PreferencesModel, State> context, boolean forward) { return context.state().host().focusNext(forward); }
    @Override public boolean wantsTextInput(DesktopWindowContext<PreferencesModel, State> context) { return context.state().edit().isFocused(); }
    @Override public void keyboardFocusChanged(DesktopWindowContext<PreferencesModel, State> context, boolean focused) { if (!focused) context.state().host().clearFocus(); }
    @Override public void narrate(DesktopWindowContext<PreferencesModel, State> context, NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE, title(context));
        context.state().host().narrate(output);
    }
    @Override public void cancelPointer(DesktopWindowContext<?, ?> context) { ((State) context.state()).host().cancelPointer(); }
    @Override public void updateTooltipState(DesktopWindowContext<?, ?> context, int x, int y, boolean active) { ((State) context.state()).host().hoverActive(active); }
}
