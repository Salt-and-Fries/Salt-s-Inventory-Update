package com.salts_inventory_update.compat.nativewindows;

import com.salts_inventory_update.api.client.desktop.*;
import com.salts_inventory_update.api.integration.DesktopEnvironment;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.AbstractContainerMenu;

/** Optional native integrations reuse native artwork through the public template API. */
public final class NativeWindowPolish {
    private static final Set<ResourceLocation> REGISTERED = new HashSet<>();

    private NativeWindowPolish() { }

    public static void install(String modId, String menuId, Function<DesktopNativeView, DesktopNativeLayout> layout) {
        install(modId,menuId,layout,true,0);
    }

    public static void install(String modId, String menuId, Function<DesktopNativeView, DesktopNativeLayout> layout,
                               boolean nativeSlots,int reservedTop) {
        installTemplate(modId, menuId, () -> new Template(layout,nativeSlots,reservedTop));
    }

    public static void installTemplate(String modId, String menuId, Supplier<DesktopNativeTemplate<?, ?>> factory) {
        if (!DesktopEnvironment.isModLoaded(modId)) return;
        register(modId, menuId, factory);
        DesktopClientApi.onClientStarted(client -> register(modId, menuId, factory));
    }

    private static synchronized void register(String modId, String menuId,
            Supplier<DesktopNativeTemplate<?, ?>> factory) {
        var menu = ResourceLocation.parse(menuId);
        if (REGISTERED.contains(menu) || !BuiltInRegistries.MENU.containsKey(menu)) return;
        var type = BuiltInRegistries.MENU.get(menu);
        DesktopEnvironment.registerIntegration(modId, () -> DesktopClientApi.registerNativeMatcher(
            ResourceLocation.fromNamespaceAndPath("salts_inventory_compat", "native/" + menu.getNamespace() + "/" + menu.getPath()),
            0, Set.of(type), lookup -> true, lookup -> factory.get()));
        REGISTERED.add(menu);
    }

    private record Template(Function<DesktopNativeView, DesktopNativeLayout> layout,boolean nativeSlots,int reservedTop)
            implements DesktopNativeTemplate<AbstractContainerMenu, Void>, DesktopWindowPresentation {
        @Override public DesktopNativeLayout nativeLayout(DesktopNativeContext<AbstractContainerMenu, Void> context) {
            return layout.apply(context.nativeView());
        }
        @Override public boolean nativeSlotBackground(DesktopNativeContext<AbstractContainerMenu,Void> context,int menuSlotId) {
            return nativeSlots;
        }
        @Override public java.util.List<DesktopWindowRegion> externalRegions(DesktopWindowContext<?,?> context) {
            if(reservedTop==0)return java.util.List.of();
            var view=((DesktopNativeContext<?,?>)context).nativeView();
            return layout.apply(view).regions().stream().filter(DesktopNativeRegion::exterior)
                .map(r->new DesktopWindowRegion(r.x(),-reservedTop,r.source().width(),reservedTop,false,false,true,DesktopHitRole.NONE)).toList();
        }
    }

    /** Conservative fallback for an unrecognized layout; the host already supplies the frame inset. */
    public static DesktopNativeLayout machine(DesktopNativeView view) {
        return DesktopNativeLayout.builder().inherit(view.automaticBounds()).build();
    }

    private static java.util.List<DesktopWindowBounds> anchors(DesktopNativeView view) {
        var all=new java.util.ArrayList<DesktopWindowBounds>();
        view.slots().stream().filter(s->!s.player()&&s.active()).forEach(s->all.add(s.bounds()));
        view.widgets().stream().filter(DesktopNativeView.Widget::visible).forEach(w->all.add(w.bounds()));
        return all;
    }
    private static DesktopWindowBounds frame(DesktopWindowBounds cell,int padding) {
        return new DesktopWindowBounds(cell.x()-padding,cell.y()-padding,cell.width()+padding*2,cell.height()+padding*2);
    }
    private static DesktopNativeLayout body(java.util.List<DesktopWindowBounds> pieces,DesktopNativeView view) {
        return DesktopNativeLayout.builder().keep("machine",DesktopNativeView.union(pieces).orElse(view.automaticBounds()),0,0).build();
    }
    public static DesktopNativeLayout oxidized(DesktopNativeView view) {
        var output=view.slotBounds(2);if(output.isEmpty())return machine(view);
        var pieces=anchors(view);pieces.add(frame(output.get(),4));return body(pieces,view);
    }
    public static DesktopNativeLayout juicer(DesktopNativeView view) {
        var input=view.slotBounds(0);var second=view.slotBounds(1);var buffer=view.slotBounds(3);
        if(input.isEmpty()||second.isEmpty()||buffer.isEmpty())return machine(view);
        var pieces=anchors(view);var a=input.get();var b=second.get();
        // The press handle/base extend beyond the ingredient cells; retain the entire native mechanism.
        pieces.add(new DesktopWindowBounds(a.x()-8,a.y()-8,34,b.y()+b.height()+16-(a.y()-8)));
        pieces.add(frame(buffer.get(),4));return body(pieces,view);
    }

    /** Keep native slot textures while removing the unused native inventory/title bands around a grid. */
    public static DesktopNativeLayout slots(DesktopNativeView view) {
        var cells = view.slots().stream().filter(slot -> !slot.player() && slot.active())
            .map(DesktopNativeView.Slot::bounds).toList();
        var bounds = DesktopNativeView.union(cells).orElse(view.automaticBounds());
        return DesktopNativeLayout.builder().keep("native-slots", bounds, 0, 0).build();
    }

    /** Native hatch animation and input are retained; only the preview's placement and blank gap change. */
    public static DesktopNativeLayout kiln(DesktopNativeView view) {
        var automatic = view.automaticBounds();
        // Brick & Mortar 1.2's preview is a 92px panel above the original image origin.
        // Its height is observed from native rendering, so the original opening animation drives the layout.
        // Before the first native render there is no observed outboard draw yet. Start at the
        // known closed extent, so the first visible body already has its final stable geometry.
        int previewHeight = Math.max(8, -automatic.y());
        if (previewHeight > 64 || view.imageBounds().width() != 176) return machine(view);
        var all=anchors(view);view.slotBounds(2).ifPresent(slot->all.add(frame(slot,4)));
        var body = DesktopNativeView.union(all).orElse(automatic);
        var preview = new DesktopWindowBounds(view.imageBounds().width() - 102, -previewHeight, 92, previewHeight);
        return DesktopNativeLayout.builder()
            .keep("machine", body, 0, 0)
            .exterior("native-preview", preview, Math.max(0, (body.width()+DesktopInsets.SALT.horizontal()-preview.width())/2), -previewHeight)
            .build();
    }
}
