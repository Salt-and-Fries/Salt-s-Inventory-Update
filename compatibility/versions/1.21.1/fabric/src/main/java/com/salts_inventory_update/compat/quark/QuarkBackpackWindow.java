package com.salts_inventory_update.compat.quark;

import com.salts_inventory_update.api.client.desktop.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** A stitched native bag above Inventory. Closing is reserved for its parent's lifetime. */
final class QuarkBackpackWindow implements DesktopWindowDefinition<QuarkBackpackMenu,Void> {
    private static final ResourceLocation ART=ResourceLocation.fromNamespaceAndPath("quark","textures/misc/backpack_gui.png");
    @Override public Component title(DesktopWindowContext<QuarkBackpackMenu,Void> c) { return Component.literal("Backpack"); }
    @Override public DesktopWindowSize defaultSize(DesktopWindowSetupContext<QuarkBackpackMenu> c) { return DesktopWindowSize.of(184,96); }
    @Override public DesktopWindowSize minSize(DesktopWindowContext<QuarkBackpackMenu,Void> c) { return DesktopWindowSize.of(184,96); }
    @Override public DesktopWindowSize maxSize(DesktopWindowContext<QuarkBackpackMenu,Void> c) { return DesktopWindowSize.of(184,96); }
    @Override public DesktopWindowOptions options(DesktopWindowContext<QuarkBackpackMenu,Void> c) { return new DesktopWindowOptions(true,true,c.model().internalClosing,true,true); }
    @Override public void opened(DesktopWindowContext<QuarkBackpackMenu,Void> c) { QuarkBackpackLink.opened(c.model(),c.windowHandle()); }
    @Override public void closed(DesktopWindowContext<QuarkBackpackMenu,Void> c) { QuarkBackpackLink.closed(c.model(),c.windowHandle()); }
    @Override public void render(DesktopRenderContext<QuarkBackpackMenu,Void> c) {
        int x=c.windowX()+7,y=c.windowY()+26;
        c.texture(ART,x,y,4,80,169,61,169,61,256,256);
        for(int i=0;i<27;i++)c.texturelessSlot(i,x+4+(i%9)*18,y+4+(i/9)*18);
    }
    @Override public DesktopSlotHit slotAt(DesktopSlotContext<QuarkBackpackMenu,Void> c,double mx,double my) {
        for(int i=0;i<27;i++) { var hit=c.menuSlotHit(i,c.windowX()+11+(i%9)*18,c.windowY()+30+(i/9)*18,mx,my);if(hit!=null)return hit; }return null;
    }
    @Override public String layoutRevision(DesktopWindowContext<QuarkBackpackMenu,Void> c) { return "quark-inventory-paired-1"; }
}
