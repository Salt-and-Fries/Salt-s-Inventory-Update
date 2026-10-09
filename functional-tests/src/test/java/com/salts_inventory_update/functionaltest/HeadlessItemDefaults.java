package com.salts_inventory_update.functionaltest;

import java.lang.reflect.Method;
import java.util.List;

import net.minecraft.world.item.Items;

/** Supplies the stack-capacity defaults that 26.x normally binds while loading a world. */
public final class HeadlessItemDefaults {
    private HeadlessItemDefaults() { }

    public static void bindStackCapacity() throws Exception {
        try {
            Class<?> mapType = Class.forName("net.minecraft.core.component.DataComponentMap");
            Object builder = mapType.getMethod("builder").invoke(null);
            Object key = Class.forName("net.minecraft.core.component.DataComponents").getField("MAX_STACK_SIZE").get(null);
            Method set = java.util.Arrays.stream(builder.getClass().getMethods())
                .filter(m -> m.getName().equals("set") && m.getParameterCount() == 2).findFirst().orElseThrow();
            set.invoke(builder, key, 64);
            Object components = builder.getClass().getMethod("build").invoke(builder);
            for (var item : List.of(Items.DIRT, Items.SAND)) {
                Object holder = item.builtInRegistryHolder();
                try {
                    holder.getClass().getMethod("bindComponents", mapType).invoke(holder, components);
                } catch (NoSuchMethodException ignored) {
                    // Older versions already have their item defaults after bootstrap.
                }
            }
        } catch (ClassNotFoundException ignored) {
            // 1.20.1 uses NBT instead of data components.
        }
    }
}
