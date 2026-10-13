package com.salts_inventory_update.compat.ae2.nativeview;

import java.util.function.Consumer;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.EventPriority;

/** Loader event bridge for the fork's keyboard focus on desktop header controls. */
public final class LegacyHeaderKeyboard {
    @FunctionalInterface public interface KeyHandler {
        boolean pressed(Screen screen, int key, int scan, int modifiers);
    }
    private LegacyHeaderKeyboard() { }
    public static void install(KeyHandler keys, Consumer<Screen> mouse) {
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST,
                (ScreenEvent.KeyPressed.Pre event) -> {
                    if (keys.pressed(event.getScreen(),event.getKeyCode(),event.getScanCode(),event.getModifiers()))
                        event.setCanceled(true);
                });
        MinecraftForge.EVENT_BUS.addListener((ScreenEvent.MouseButtonPressed.Pre event) -> mouse.accept(event.getScreen()));
    }
}
