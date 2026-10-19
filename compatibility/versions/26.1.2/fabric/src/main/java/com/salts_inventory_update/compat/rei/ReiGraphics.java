package com.salts_inventory_update.compat.rei;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import me.shedaniel.rei.api.client.gui.compat.GuiGraphics;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;

/** REI's 26.3 copy constructor shares the render queue but resets its pose and clipping. */
final class ReiGraphics {
    private static final Field SCISSOR_STACK;
    private static final Method PEEK;
    static {
        try {
            SCISSOR_STACK=GuiGraphicsExtractor.class.getDeclaredField("scissorStack");
            SCISSOR_STACK.setAccessible(true);
            PEEK=SCISSOR_STACK.getType().getDeclaredMethod("peek");
            PEEK.setAccessible(true);
        } catch(ReflectiveOperationException failure) {throw new ExceptionInInitializerError(failure);}
    }
    private ReiGraphics() { }
    static GuiGraphics adapt(GuiGraphicsExtractor source) {
        var result=GuiGraphics.of(source);
        if(result==source) return result;
        try {
            var clip=(ScreenRectangle)PEEK.invoke(SCISSOR_STACK.get(source));
            // The inherited clip is already in screen coordinates; install it before the pose.
            if(clip!=null) result.enableScissor(clip.left(),clip.top(),clip.right(),clip.bottom());
            result.pose().set(source.pose());
            return result;
        } catch(ReflectiveOperationException failure) {throw new IllegalStateException("Cannot retain native REI clipping",failure);}
    }
}
