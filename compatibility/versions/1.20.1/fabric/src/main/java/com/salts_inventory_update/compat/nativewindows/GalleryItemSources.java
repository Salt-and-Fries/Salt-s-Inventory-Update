package com.salts_inventory_update.compat.nativewindows;

import com.salts_inventory_update.api.desktop.DesktopItemSources;
import com.salts_inventory_update.api.integration.DesktopEnvironment;
import com.salts_inventory_update.api.server.desktop.*;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

/** Protect the actual hand-owned painting while its native selector is detached. */
public final class GalleryItemSources {
    public static final net.minecraft.resources.ResourceLocation SELECTION=new net.minecraft.resources.ResourceLocation("salts_inventory_compat","gallery/select");
    private static boolean initialized;
    private static final Set<MenuType<?>> BOUND=Collections.newSetFromMap(new IdentityHashMap<>());
    private GalleryItemSources() { }
    public interface Owner {
        int desktopGallerySourceSlot();
        ItemStack desktopGallerySourceStack();
        void desktopGallerySourceHosted();
    }
    static synchronized void initialize() {
        if(initialized||!DesktopEnvironment.isModLoaded("gallery"))return;
        initialized=true;install();
        DesktopEnvironment.onServerStarting(server->install());
    }
    private static synchronized void install() {
        var type=NativeMenuSupport.type("gallery:painting_selector");
        if(type==null||!BOUND.add(type))return;
        NativeMenuSupport.bindNative(type,false);
        DesktopServerApi.registerNativePayload(type,SELECTION,context->{
            var menu=context.menu();var data=context.data();
            if(!(menu instanceof Owner)||!menu.getClass().getName().equals(
                    "com.teamabnormals.gallery.common.inventory.PaintingSelectorMenu")
                    ||data.length==0||data.length>256||!menu.stillValid(context.player()))return;
            var id=net.minecraft.resources.ResourceLocation.tryParse(new String(data,java.nio.charset.StandardCharsets.UTF_8));
            if(id==null)return;
            var paintings=(java.util.List<?>)ForeignMenuAccess.invoke(menu,"getAllPaintings");
            boolean allowed=paintings.stream().anyMatch(value->value instanceof net.minecraft.core.Holder<?> holder
                &&holder.unwrapKey().map(key->key.location().equals(id)).orElse(false));
            if(!allowed)return;
            ForeignMenuAccess.invoke(menu,"setSelectedPainting",new Class<?>[]{net.minecraft.resources.ResourceLocation.class},id);
            context.broadcastChanges();
        });
        DesktopItemSources.register(type,(player,menu)->{
            if(!(menu instanceof Owner owner))return null;
            int slot=owner.desktopGallerySourceSlot();var source=owner.desktopGallerySourceStack();
            if(slot<0||slot>=player.getInventory().getContainerSize()||source.isEmpty()
                    ||player.getInventory().getItem(slot)!=source)return null;
            return new DesktopItemSources.Source(slot,"gallery:"+player.getUUID()+":"+slot,source);
        });
        DesktopServerApi.registerWindow(type,new DesktopServerWindowHandler<AbstractContainerMenu,Void>() {
            @Override public boolean supports(net.minecraft.server.level.ServerPlayer player,AbstractContainerMenu menu) {
                return menu instanceof Owner&&menu.getClass().getName().equals(
                    "com.teamabnormals.gallery.common.inventory.PaintingSelectorMenu");
            }
            @Override public void opened(DesktopServerSessionContext<AbstractContainerMenu,Void> context) {
                ((Owner)context.menu()).desktopGallerySourceHosted();
            }
        });
    }
}
