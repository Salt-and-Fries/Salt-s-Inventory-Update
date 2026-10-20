package com.salts_inventory_update.controlify.mixin;

import com.salts_inventory_update.controlify.WorkspaceControlsScreen;
import java.util.Objects;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.OptionsSubScreen;
import net.minecraft.client.gui.screens.controls.ControlsScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The companion adds one entry to native control options, without modifying another mod. */
@Mixin(ControlsScreen.class)
public abstract class WorkspaceControlsOptionsMixin extends Screen {
    @Unique private Button saltsControlify$controlsButton;

    protected WorkspaceControlsOptionsMixin(Component title) { super(title); }

    @Inject(method = "init", at = @At("TAIL"))
    private void saltsControlify$addControlsSettings(CallbackInfo callback) {
        if (((Object) this).getClass() != ControlsScreen.class) return;
        saltsControlify$controlsButton = addRenderableWidget(Button.builder(
            Component.translatable("salts_inventory_controlify.controls.entry"),
            button -> minecraft.setScreen(new WorkspaceControlsScreen(this))).width(150).build());
        saltsControlify$placeControlsSettings();
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void saltsControlify$repositionControlsSettings(CallbackInfo callback) {
        if (((Object) this).getClass() == ControlsScreen.class) saltsControlify$placeControlsSettings();
    }

    @Unique private void saltsControlify$placeControlsSettings() {
        if (saltsControlify$controlsButton == null || !children().contains(saltsControlify$controlsButton)) return;
        Button done = children().stream().filter(child -> child instanceof Button)
            .map(child -> (Button) child).filter(button -> button != saltsControlify$controlsButton
                && Objects.equals(button.getMessage(), CommonComponents.GUI_DONE)).findFirst().orElse(null);
        // Leave a differently structured footer alone instead of covering another mod's controls.
        if (done == null || children().stream().filter(child -> child instanceof Button)
            .map(child -> (Button) child).anyMatch(button -> button != done && button != saltsControlify$controlsButton
                && button.visible && button.getY() < (done.getY()+done.getHeight()) && (button.getY()+button.getHeight()) > done.getY())) {
            saltsControlify$controlsButton.visible = false;
            saltsControlify$controlsButton.active = false;
            return;
        }
        int total = Math.min(310, Math.max(0, width - 20));
        int each = (total - 6) / 2;
        if (each < 50) {
            saltsControlify$controlsButton.visible = false;
            saltsControlify$controlsButton.active = false;
            return;
        }
        int left = (width - total) / 2;
        done.setWidth(each);
        done.setX(left);
        saltsControlify$controlsButton.setWidth(each);
        saltsControlify$controlsButton.setX(left + each + 6);
        saltsControlify$controlsButton.setY(done.getY());

        saltsControlify$controlsButton.visible = true;
        saltsControlify$controlsButton.active = true;
    }
}
