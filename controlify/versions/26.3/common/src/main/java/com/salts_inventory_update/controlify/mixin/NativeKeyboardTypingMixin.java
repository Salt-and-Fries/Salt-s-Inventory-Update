package com.salts_inventory_update.controlify.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import dev.isxander.controlify.screenop.keyboard.InputTarget;
import dev.isxander.controlify.screenop.keyboard.KeyboardOverlayScreen;
import dev.isxander.controlify.screenop.compat.vanilla.EditBoxComponentProcessor;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Native keyboards must accept physical typing too, including when no world is loaded. */
@Mixin(value = KeyboardOverlayScreen.class, remap = false)
public abstract class NativeKeyboardTypingMixin extends Screen {
    @Shadow @Final private InputTarget inputTarget;
    @Shadow @Final private Screen backgroundScreen;
    protected NativeKeyboardTypingMixin(Component title) { super(title); }

    @Inject(method = "init", at = @At("RETURN"), remap = false)
    private void saltsControlify$restoreAfterOpen(CallbackInfo callback) { saltsControlify$restoreEditorFocus(); }

    @Inject(method = "onClose", at = @At("RETURN"), remap = false)
    private void saltsControlify$restoreAfterClose(CallbackInfo callback) {
        // Clicking another background control may have deliberately moved focus.
        if (backgroundScreen.getFocused() == null) saltsControlify$restoreEditorFocus();
    }

    @Unique private void saltsControlify$restoreEditorFocus() {
        InputTarget target = inputTarget;
        for (int depth = 0; depth < 8 && target instanceof InputTarget.Delegated; depth++)
            target = ((DelegatedInputTargetAccessor) target).saltsControlify$target();
        if (!(target instanceof EditBoxComponentProcessor.EditBoxInputTarget editor)) return;
        // Gui.setScreen clears the outgoing focus. Restore only the actual editor still
        // owned by the native background, rather than focusing a detached or replaced field.
        if (saltsControlify$focus(backgroundScreen, editor.editBox())) editor.editBox().setFocused(true);
    }

    @Unique private static boolean saltsControlify$focus(ContainerEventHandler owner, Object editor) {
        for (var child : owner.children()) {
            if (child == editor || child instanceof ContainerEventHandler nested && saltsControlify$focus(nested, editor)) {
                owner.setFocused(child);
                return true;
            }
        }
        return false;
    }

    @Override public boolean charTyped(CharacterEvent event) {
        if (!inputTarget.supportsCharInput()) return super.charTyped(event);
        boolean accepted = false;
        for (char character : Character.toChars(event.codepoint()))
            accepted |= inputTarget.acceptChar(character, 0);
        return accepted;
    }

    @Override public boolean keyPressed(KeyEvent event) {
        int key = event.key();
        boolean editing = key == InputConstants.KEY_BACKSPACE || key == InputConstants.KEY_DELETE
                || key == InputConstants.KEY_LEFT || key == InputConstants.KEY_RIGHT
                || key == InputConstants.KEY_HOME || key == InputConstants.KEY_END
                || event.isSelectAll() || event.isCopy() || event.isPaste() || event.isCut();
        if (editing && inputTarget.supportsKeyCodeInput()
                && inputTarget.acceptKeyCode(key, event.keycode(), event.modifiers())) return true;
        return super.keyPressed(event);
    }
}
