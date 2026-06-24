package com.salts_inventory_update.mixin.server;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.salts_inventory_update.server.DesktopContainerSessions;

@Mixin(targets = "net.minecraft.world.level.block.entity.ChestBlockEntity$1")
public abstract class ChestOpenersCounterMixin {
    @Unique
    private static Field salts_inventory_update$containerField;

    @Inject(method = "isOwnContainer", at = @At("HEAD"), cancellable = true)
    private void salts_inventory_update$desktopSessionOwnsChest(Player player, CallbackInfoReturnable<Boolean> cir) {
        Container container = salts_inventory_update$container(this);
        if (container != null && DesktopContainerSessions.hasOpenSessionForContainer(player, container)) {
            cir.setReturnValue(true);
        }
    }

    @Unique
    private static Container salts_inventory_update$container(Object opener) {
        try {
            Field field = salts_inventory_update$containerField;
            if (field == null) {
                field = salts_inventory_update$findContainerField(opener.getClass());
                salts_inventory_update$containerField = field;
            }
            Object value = field.get(opener);
            return value instanceof Container container ? container : null;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            return null;
        }
    }

    @Unique
    private static Field salts_inventory_update$findContainerField(Class<?> type) throws NoSuchFieldException {
        for (Field field : type.getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers()) && Container.class.isAssignableFrom(field.getType())) {
                field.setAccessible(true);
                return field;
            }
        }
        throw new NoSuchFieldException(type.getName());
    }
}
