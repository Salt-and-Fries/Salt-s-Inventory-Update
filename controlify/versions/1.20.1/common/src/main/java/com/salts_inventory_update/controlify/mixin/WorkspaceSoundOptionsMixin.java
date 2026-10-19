package com.salts_inventory_update.controlify.mixin;

import com.salts_inventory_update.controlify.WorkspaceSoundSettingsScreen;
import java.util.Objects;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.OptionsSubScreen;
import net.minecraft.client.gui.screens.SoundOptionsScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The companion adds one entry to native sound options, without modifying another mod. */
@Mixin(SoundOptionsScreen.class)
public abstract class WorkspaceSoundOptionsMixin extends Screen {
    @Unique private Button saltsControlify$soundsButton;

    protected WorkspaceSoundOptionsMixin(Component title) { super(title); }

    @Inject(method = "init", at = @At("TAIL"))
    private void saltsControlify$addSoundSettings(CallbackInfo callback) {
        if (((Object) this).getClass() != SoundOptionsScreen.class) return;
        saltsControlify$soundsButton = addRenderableWidget(Button.builder(
            Component.translatable("salts_inventory_controlify.sounds.entry"),
            button -> minecraft.setScreen(new WorkspaceSoundSettingsScreen(this))).width(150).build());
        saltsControlify$placeSoundSettings();
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void saltsControlify$repositionSoundSettings(CallbackInfo callback) {
        if (((Object) this).getClass() == SoundOptionsScreen.class) saltsControlify$placeSoundSettings();
    }

    @Unique private void saltsControlify$placeSoundSettings() {
        if (saltsControlify$soundsButton == null || !children().contains(saltsControlify$soundsButton)) return;
        Button done = children().stream().filter(child -> child instanceof Button)
            .map(child -> (Button) child).filter(button -> button != saltsControlify$soundsButton
                && Objects.equals(button.getMessage(), CommonComponents.GUI_DONE)).findFirst().orElse(null);
        // Leave a differently structured footer alone instead of covering another mod's controls.
        if (done == null || children().stream().filter(child -> child instanceof Button)
            .map(child -> (Button) child).anyMatch(button -> button != done && button != saltsControlify$soundsButton
                && button.visible && button.getY() < (done.getY()+done.getHeight()) && (button.getY()+button.getHeight()) > done.getY())) {
            saltsControlify$soundsButton.visible = false;
            saltsControlify$soundsButton.active = false;
            return;
        }
        int total = Math.min(310, Math.max(0, width - 20));
        int each = (total - 6) / 2;
        if (each < 50) {
            saltsControlify$soundsButton.visible = false;
            saltsControlify$soundsButton.active = false;
            return;
        }
        int left = (width - total) / 2;
        done.setWidth(each);
        done.setX(left);
        saltsControlify$soundsButton.setWidth(each);
        saltsControlify$soundsButton.setX(left + each + 6);
        saltsControlify$soundsButton.setY(done.getY());

        saltsControlify$soundsButton.visible = true;
        saltsControlify$soundsButton.active = true;
    }
}
