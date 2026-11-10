package com.salts_inventory_update.compat.alloyforgery;

import com.salts_inventory_update.compat.nativewindows.ForeignMenuAccess;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;

/** Access only to the optional publisher screen and its own Owo surface. */
public final class AlloyScreens {
    private static final Map<AbstractContainerMenu, WeakReference<AbstractContainerScreen<?>>> SCREENS = new WeakHashMap<>();
    private AlloyScreens() { }

    public static void remember(AbstractContainerMenu menu, AbstractContainerScreen<?> screen) {
        SCREENS.put(menu, new WeakReference<>(screen));
    }

    public static void prepare(AbstractContainerMenu menu) {
        var reference = SCREENS.get(menu);
        Object root = field(field(reference == null ? null : reference.get(), "uiAdapter"), "rootComponent");
        if (root == null) return;
        try {
            Class<?> surface = Class.forName("io.wispforest.owo.ui.core.Surface");
            // XML surfaces may be composite wrappers rather than the static
            // VANILLA_TRANSLUCENT singleton. Only this hosted Forge root is
            // cleared; the original machine textures and child surfaces remain.
            ForeignMenuAccess.invoke(root, "surface", new Class<?>[]{surface}, surface.getField("BLANK").get(null));
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException("Alloy Forge native backdrop unavailable", failure);
        }
    }

    private static Object field(Object owner, String name) {
        if (owner == null) return null;
        for (Class<?> type = owner.getClass(); type != null; type = type.getSuperclass()) try {
            var field = type.getDeclaredField(name);
            field.setAccessible(true);
            return field.get(owner);
        } catch (NoSuchFieldException absent) {
            // The UI adapter and root component belong to different Owo bases.
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException(name, failure);
        }
        return null;
    }
}
