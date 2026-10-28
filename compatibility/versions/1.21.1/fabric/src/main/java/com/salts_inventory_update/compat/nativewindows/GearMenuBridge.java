package com.salts_inventory_update.compat.nativewindows;

import com.salts_inventory_update.api.server.desktop.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.*;

/** Authenticated tab/action bridge. The original menu remains authoritative over recipes and costs. */
public final class GearMenuBridge {
    public static final ResourceLocation ACTION=ResourceLocation.fromNamespaceAndPath("salts_inventory_compat","gear/action");
    public static final ResourceLocation READY=ResourceLocation.fromNamespaceAndPath("salts_inventory_compat","gear/ready");
    private GearMenuBridge() { }
    public static void register(MenuType<AbstractContainerMenu> first,MenuType<AbstractContainerMenu> second,
            String customClass,String firstTitle,String secondTitle,boolean reforge) {
        if(first==null||second==null)return;
        NativeMenuSupport.bindNative(second,false);
        if(DesktopServerApi.findWindowHandler(second)==null)DesktopServerApi.registerWindow(second,new Handler(customClass,reforge));
        install(first,first,second,customClass,firstTitle,secondTitle,reforge);
        install(second,first,second,customClass,firstTitle,secondTitle,reforge);
    }
    private static void install(MenuType<AbstractContainerMenu> type,MenuType<AbstractContainerMenu> first,MenuType<AbstractContainerMenu> second,
            String customClass,String firstTitle,String secondTitle,boolean reforge) {
        if(DesktopServerApi.findPayloadHandler(type,ACTION)!=null)return;
        DesktopServerApi.registerPayload(type,ACTION,c->{
            if(c.data().length!=1||!c.menu().stillValid(c.player()))return;
            int action=c.data()[0];
            if(action==2) {
                if(customClass.endsWith("GrinderScreenHandler")||c.menu().getType()!=second||!ready(c.menu(),reforge))return;
                ForeignMenuAccess.invoke(c.menu(),reforge?"reforge":"smith");c.broadcastChanges();return;
            }
            if(action<0||action>1||(action==0&&type==first)||(action==1&&type==second))return;
            var pos=ForeignMenuAccess.position(c.menu());
            if(pos==null||c.player().distanceToSqr(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5)>64)return;
            var access=ContainerLevelAccess.create(c.player().level(),pos);
            var provider=new SimpleMenuProvider((id,inventory,player)->action==1
                ?ForeignMenuAccess.create(customClass,id,inventory,access)
                :(MenuType<?>)first==MenuType.ANVIL?new AnvilMenu(id,inventory,access)
                :(MenuType<?>)first==MenuType.SMITHING?new SmithingMenu(id,inventory,access):new GrindstoneMenu(id,inventory,access),
                Component.translatable(action==1?secondTitle:firstTitle));
            DesktopSessions.withSessionTransition(c.player(),c.sessionId(),()->c.player().openMenu(provider));
        });
    }
    public static boolean ready(AbstractContainerMenu menu,boolean reforge) {
        if(reforge) {
            return Boolean.TRUE.equals(ForeignMenuAccess.invoke(menu,"isReforgeReady"));
        }
        if(menu.slots.isEmpty()||!menu.getSlot(0).hasItem())return false;
        return !((net.minecraft.world.item.ItemStack)ForeignMenuAccess.invoke(menu,"getUnlickedGem")).isEmpty()
            &&Boolean.TRUE.equals(ForeignMenuAccess.invoke(menu,"canLinkSameGem"));
    }
    private static final class ReadyState {
        Boolean ready;
        java.util.List<net.minecraft.world.item.ItemStack> items=java.util.List.of();
    }
    private record Handler(String menuClass,boolean reforge) implements DesktopServerWindowHandler<AbstractContainerMenu,ReadyState> {
        @Override public boolean supports(net.minecraft.server.level.ServerPlayer player,AbstractContainerMenu menu){return menu.getClass().getName().equals(menuClass);}
        @Override public boolean usesNativePlayerInventoryQuickMove(AbstractContainerMenu menu){return true;}
        @Override public ReadyState createState(DesktopServerSessionContext<AbstractContainerMenu,ReadyState> c){return new ReadyState();}
        @Override public void tick(DesktopServerSessionContext<AbstractContainerMenu,ReadyState> c){
            if(menuClass.endsWith("GrinderScreenHandler"))return;
            var stacks=c.menu().slots.stream().filter(s->s.container!=c.player().getInventory()).map(Slot::getItem).toList();
            boolean changed=stacks.size()!=c.state().items.size();
            if(!changed)for(int i=0;i<stacks.size();i++)if(!net.minecraft.world.item.ItemStack.matches(stacks.get(i),c.state().items.get(i))){changed=true;break;}
            if(!changed&&c.state().ready!=null)return;
            c.state().items=stacks.stream().map(net.minecraft.world.item.ItemStack::copy).toList();
            boolean enabled=ready(c.menu(),reforge);
            if(c.state().ready==null||c.state().ready!=enabled){c.state().ready=enabled;c.sendToClient(READY,new byte[]{(byte)(enabled?1:0)});}
        }
    }
}
