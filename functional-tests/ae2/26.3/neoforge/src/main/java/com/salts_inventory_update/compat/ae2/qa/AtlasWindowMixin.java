package com.salts_inventory_update.compat.ae2.qa.mixin;

import appeng.menu.AEBaseMenu;
import com.salts_inventory_update.api.client.desktop.DesktopRenderContext;
import com.salts_inventory_update.api.client.desktop.DesktopWindowContext;
import com.salts_inventory_update.compat.ae2.nativeview.AEWindow;
import com.salts_inventory_update.compat.ae2.nativeview.NativeAtlasHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.salts_inventory_update.api.client.desktop.DesktopWindowSetupContext;
import com.salts_inventory_update.api.client.desktop.DesktopWindowSize;

/** Opt-in instrumentation of the extension's own window; no core implementation access. */
@Mixin(value = AEWindow.class, remap = false)
public abstract class AtlasWindowMixin {
    @Inject(method = "defaultSize", at = @At("RETURN"))
    private void defaultSize(DesktopWindowSetupContext<AEBaseMenu> context, CallbackInfoReturnable<DesktopWindowSize> result) {
        if (context.model() instanceof appeng.menu.me.items.PatternEncodingTermMenu)
            org.slf4j.LoggerFactory.getLogger(AtlasWindowMixin.class).info("ATLAS DEFAULT {}", result.getReturnValue());
    }
    @Inject(method = "desiredWindowSize", at = @At("RETURN"))
    private void desiredSize(DesktopWindowContext<?, ?> context, CallbackInfoReturnable<DesktopWindowSize> result) {
        if (context.model() instanceof appeng.menu.me.items.PatternEncodingTermMenu && result.getReturnValue() != null)
            org.slf4j.LoggerFactory.getLogger(AtlasWindowMixin.class).info("ATLAS REQUEST window={}x{} content={}x{} result={}",
                    context.windowWidth(), context.windowHeight(), context.contentWidth(), context.contentHeight(), result.getReturnValue());
    }
    @Inject(method = "render", at = @At("HEAD"))
    private void beforeRender(DesktopRenderContext<AEBaseMenu, AEWindow.State> context, CallbackInfo ci) { NativeAtlasHooks.beforeRender(context); }
    @Inject(method = "render", at = @At("RETURN"))
    private void afterRender(DesktopRenderContext<AEBaseMenu, AEWindow.State> context, CallbackInfo ci) { NativeAtlasHooks.afterRender(context); }
    @Inject(method = "closed", at = @At("RETURN"))
    private void closed(DesktopWindowContext<AEBaseMenu, AEWindow.State> context, CallbackInfo ci) { NativeAtlasHooks.closed(context); }
}
