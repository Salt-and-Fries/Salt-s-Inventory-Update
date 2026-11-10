package com.salts_inventory_update.compat.nativewindows;
import java.util.function.*;
import com.salts_inventory_update.api.integration.DesktopEnvironment;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.*;
/** Loader-owned registry timing, kept entirely in the optional extension. */
public final class NativeMenuRegistration {
    private NativeMenuRegistration() { }
    public static <T extends AbstractContainerMenu> void register(String path,Supplier<MenuType<T>> factory,Consumer<MenuType<T>> bind){
        if(DesktopEnvironment.isModLoaded("fabricloader")){
            var type=Registry.register(BuiltInRegistries.MENU,new ResourceLocation("salts_inventory_compat",path),factory.get());bind.accept(type);return;
        }
        try {Class.forName("com.salts_inventory_update.compat.platform.LoaderMenus")
            .getMethod("register",String.class,Supplier.class,Consumer.class).invoke(null,path,factory,bind);}
        catch(ReflectiveOperationException failure){throw new IllegalStateException("Native menu registration",failure);}
    }
}
