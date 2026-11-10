package com.salts_inventory_update.compat.nativewindows;

import com.salts_inventory_update.api.client.desktop.*;
import com.salts_inventory_update.api.client.input.MouseButtonEvent;
import com.salts_inventory_update.api.integration.DesktopEnvironment;
import com.salts_inventory_update.api.server.desktop.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.inventory.AbstractContainerMenu;

/** Window-addressed transport; the original handler still owns payment and eligibility. */
public final class MowzieTradeActions {
    private static final ResourceLocation ACTION=new ResourceLocation("salts_inventory_compat","mowzie/trade");
    private MowzieTradeActions() { }
    private static final java.util.Map<AbstractContainerMenu,DesktopInputContext<AbstractContainerMenu,Void>> SENDERS=new java.util.IdentityHashMap<>();
    public static void forget(AbstractContainerMenu menu){SENDERS.remove(menu);}
    public static boolean send(AbstractContainerMenu menu){
        var c=SENDERS.get(menu);return c!=null&&c.windowHandle().isOpen()&&c.sendPayload(ACTION,new byte[0]);
    }
    public static void initialize() {
        if(!DesktopEnvironment.isModLoaded("mowziesmobs"))return;
        DesktopEnvironment.onServerStarting(server->DesktopEnvironment.registerIntegration("mowziesmobs",()->{
            for(String kind:java.util.List.of("Sculptor","Umvuthi")) {
                var type=NativeMenuSupport.type("mowziesmobs:"+kind.toLowerCase(java.util.Locale.ROOT)+"_trade");
                if(type==null||DesktopServerApi.findPayloadHandler(type,ACTION)!=null)continue;
                DesktopServerApi.registerNativePayload(type,ACTION,c->{
                    if(c.data().length!=0||!c.menu().stillValid(c.player()))return;
                    var owner=(Entity)ForeignMenuAccess.invoke(c.menu(),"get"+kind);
                    if(owner==null||!owner.isAlive()||owner.level()!=c.player().level())return;
                    var previous=c.player().containerMenu;c.player().containerMenu=c.menu();
                    try {
                        var packet=Class.forName("com.bobmowzie.mowziesmobs.server.message.Message"+kind+"Trade");
                        var instance=packet.getConstructor(net.minecraft.world.entity.LivingEntity.class).newInstance(owner);
                        var handler=Class.forName(packet.getName()+"$Handler").getDeclaredMethod("lambda$accept$0",ServerPlayer.class,packet);
                        handler.setAccessible(true);handler.invoke(null,c.player(),instance);
                    }catch(ReflectiveOperationException failure){throw new IllegalStateException("Unsupported native Mowzie trade",failure);}
                    finally { if(c.player().containerMenu==c.menu())c.player().containerMenu=previous; }
                    c.broadcastChanges();
                });
            }
        }));
    }
    public static boolean clicked(DesktopInputContext<AbstractContainerMenu,Void> c,MouseButtonEvent e) {
        if(e.button()!=0)return false;
        var n=(DesktopNativeContext<?,?>)c;var point=n.nativeSourceAt(e.x(),e.y());if(point.isEmpty())return false;
        for(var w:n.nativeView().widgets())if(w.visible()&&w.active()&&DesktopNativeRegion.contains(w.bounds(),point.get().x(),point.get().y())) {
            // Let the original callback update its native button/prediction;
            // only its transport is redirected to this window's lifetime.
            SENDERS.put(c.model(),c);return false;
        }
        return false;
    }
}
