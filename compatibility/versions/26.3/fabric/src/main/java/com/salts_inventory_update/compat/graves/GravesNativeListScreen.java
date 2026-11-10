package com.salts_inventory_update.compat.graves;

import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;

/** Registers the native selection screen, which deliberately is not an SIU window. */
final class GravesNativeListScreen {
    private GravesNativeListScreen() { }
    static void register(MenuType<ChestMenu> type) {
        try {
            // Fabric widens this vanilla method; shared compilation also sees
            // un-widened loader classes. Resolve its structural signature so
            // production-obfuscated method names do not need string guesses.
            for (var method : MenuScreens.class.getDeclaredMethods()) {
                var parameters=method.getParameterTypes();
                if (!Modifier.isStatic(method.getModifiers()) || method.getReturnType()!=void.class
                        || parameters.length!=2 || parameters[0]!=MenuType.class || !parameters[1].isInterface()) continue;
                Object factory=Proxy.newProxyInstance(MenuScreens.class.getClassLoader(),new Class<?>[]{parameters[1]},(proxy,call,args)->{
                    if (call.isDefault()) return java.lang.reflect.InvocationHandler.invokeDefault(proxy,call,args);
                    if (call.getDeclaringClass()==Object.class) return switch(call.getName()) {
                        case "toString"->"Universal Graves native list screen";
                        case "hashCode"->System.identityHashCode(proxy);
                        case "equals"->proxy==args[0];default->throw new IllegalStateException(call.toString());
                    };
                    if (args==null || args.length!=3) throw new IllegalStateException("Unexpected native screen constructor: "+call);
                    return new ContainerScreen((ChestMenu)args[0],(Inventory)args[1],(Component)args[2]);
                });
                method.setAccessible(true);method.invoke(null,type,factory);return;
            }
            throw new IllegalStateException("Native menu screen registration unavailable");
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException("Universal Graves native selection screen",failure);
        }
    }
}
