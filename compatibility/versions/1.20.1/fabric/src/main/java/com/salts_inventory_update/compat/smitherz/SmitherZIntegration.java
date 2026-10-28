package com.salts_inventory_update.compat.smitherz;

import com.salts_inventory_update.api.integration.DesktopEnvironment;
import com.salts_inventory_update.api.client.desktop.*;
import com.salts_inventory_update.api.desktop.DesktopMenuBindings;
import com.salts_inventory_update.compat.nativewindows.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class SmitherZIntegration {
    private SmitherZIntegration() { }
    private static final ResourceLocation TEXTURE=new ResourceLocation("smitherz","textures/gui/smither_screen.png");
    public static void initializeCommon() {
        DesktopEnvironment.onServerStarting(server->DesktopEnvironment.registerIntegration("smitherz",()->{
            registerBindings();
            GearMenuBridge.register(NativeMenuSupport.type("minecraft:smithing"),NativeMenuSupport.type("smitherz:smither"),
                "net.smitherz.screen.SmitherScreenHandler","container.upgrade","container.link",false);
            GearMenuBridge.register(NativeMenuSupport.type("minecraft:grindstone"),NativeMenuSupport.type("smitherz:grinder"),
                "net.smitherz.screen.GrinderScreenHandler","container.grindstone_title","container.unlink",false);
        }));
    }
    public static void initializeClient() {
        registerBindings();DesktopClientApi.onClientStarted(client->registerBindings());
        var emblem=new MockupNativeTemplate.Emblem(TEXTURE,6,7,40,36);
        NativeWindowPolish.installTemplate("smitherz","minecraft:smithing",()->
            new TabbedGearTemplate(NativeBatchLayouts::smithing,false,false,TEXTURE,null,emblem));
        NativeWindowPolish.installTemplate("smitherz","smitherz:smither",()->
            new LinkingTemplate(emblem));
        NativeWindowPolish.installTemplate("smitherz","minecraft:grindstone",()->
            new TabbedGearTemplate(NativeBatchLayouts::grindstone,false,false,null,null,null));
        NativeWindowPolish.installTemplate("smitherz","smitherz:grinder",()->
            new TabbedGearTemplate(NativeBatchLayouts::unlinking,true,false,null,null,null));
    }
    private static void registerBindings() {
        NativeMenuSupport.bindNative(NativeMenuSupport.type("smitherz:grinder"),false);
        NativeMenuSupport.bindNative(NativeMenuSupport.type("smitherz:smither"),false,new DesktopMenuBindings.SlotAccess(){
            @Override public int size(AbstractContainerMenu menu){return menu.slots.size();}
            @Override public boolean isVirtualSlot(AbstractContainerMenu menu,Slot slot){
                return slot.getClass().getName().equals("net.smitherz.screen.widget.GemSlot")
                    &&Boolean.TRUE.equals(ForeignMenuAccess.invoke(slot,"isLinked"));
            }
        },(menu,items)->{if(!items.isEmpty())refreshSockets(menu,items.get(0));});
    }
    private static boolean upgradeable(ItemStack stack) {
        if(stack.isEmpty())return false;
        try{return Class.forName("net.smitherz.item.Upgradeable").isInstance(stack.getItem());}
        catch(ClassNotFoundException missing){throw new IllegalStateException(missing);}
    }
    private static void refreshSockets(AbstractContainerMenu menu,ItemStack stack) {
        if(upgradeable(stack))ForeignMenuAccess.invoke(menu,"updateGemSlots",new Class<?>[]{ItemStack.class},stack);
        else for(var slot:menu.slots)if(slot.getClass().getName().equals("net.smitherz.screen.widget.GemSlot")){
            ForeignMenuAccess.invoke(slot,"setEnabled",new Class<?>[]{boolean.class},false);
            ForeignMenuAccess.invoke(slot,"setLinked",new Class<?>[]{boolean.class},false);
        }
    }
    private static final class LinkingTemplate extends TabbedGearTemplate {
        private ItemStack last=ItemStack.EMPTY;
        LinkingTemplate(MockupNativeTemplate.Emblem emblem){
            super(NativeBatchLayouts::linking,true,false,TEXTURE,new DesktopWindowBounds(148,18,18,18),emblem);
        }
        @Override public void prepareLayout(DesktopWindowContext<AbstractContainerMenu,State> c){
            var stack=c.model().getSlot(0).getItem();
            if(!ItemStack.matches(last,stack)){refreshSockets(c.model(),stack);last=stack.copy();}
            super.prepareLayout(c);
        }
    }
}
