package com.salts_inventory_update.compat.platform;
import java.util.function.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.*;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.bus.api.IEventBus;
public final class LoaderMenus {
    private static final DeferredRegister<MenuType<?>> MENUS=DeferredRegister.create(Registries.MENU,"salts_inventory_compat");
    private static IEventBus bus;
    private LoaderMenus() { }
    public static void initialize(IEventBus events){if(bus!=null)return;bus=events;MENUS.register(events);}
    public static IEventBus bus(){if(bus==null)throw new IllegalStateException("Loader event bus is not available");return bus;}
    public static <T extends AbstractContainerMenu> void register(String id,Supplier<MenuType<T>> factory,Consumer<MenuType<T>> bind){
        MENUS.register(id,()->{var type=factory.get();bind.accept(type);return type;});
    }
    public static <T extends AbstractContainerMenu> Supplier<MenuType<T>> registerEntity(String id,Supplier<MenuType<T>> factory){return MENUS.register(id,factory);}
}
