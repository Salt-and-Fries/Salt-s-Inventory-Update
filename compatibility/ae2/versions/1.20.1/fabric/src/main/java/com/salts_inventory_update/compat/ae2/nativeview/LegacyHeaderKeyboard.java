package com.salts_inventory_update.compat.ae2.nativeview;

import java.util.function.Consumer;
import net.minecraft.client.gui.screens.Screen;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;

/** Loader event bridge for the fork's keyboard focus on desktop header controls. */
public final class LegacyHeaderKeyboard {
    @FunctionalInterface public interface KeyHandler {
        boolean pressed(Screen screen, int key, int scan, int modifiers);
    }
    private LegacyHeaderKeyboard() { }
    public static void install(KeyHandler keys, Consumer<Screen> mouse) {
        ScreenEvents.AFTER_INIT.register((client,screen,width,height) -> {
            ScreenKeyboardEvents.allowKeyPress(screen).register((target,key,scan,modifiers) ->
                    !keys.pressed(target,key,scan,modifiers));
            ScreenMouseEvents.beforeMouseClick(screen).register((target,x,y,button) -> mouse.accept(target));
        });
    }
}
