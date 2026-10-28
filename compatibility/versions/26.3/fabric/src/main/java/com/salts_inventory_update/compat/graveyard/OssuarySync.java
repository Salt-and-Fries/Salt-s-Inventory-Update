package com.salts_inventory_update.compat.graveyard;
import com.salts_inventory_update.api.client.desktop.*;
import com.salts_inventory_update.api.integration.DesktopEnvironment;
import com.salts_inventory_update.api.server.desktop.*;
import com.salts_inventory_update.compat.nativewindows.*;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

/** The upstream recipe packet addresses the foreground menu; detached windows own their own API payload. */
public final class OssuarySync {
    private static final Identifier RECIPES=Identifier.fromNamespaceAndPath("salts_inventory_compat","graveyard/recipes");
    private OssuarySync() { }
    private static final class State { List<ItemStack> sent; }
    @SuppressWarnings("unchecked") private static List<ItemStack> results(AbstractContainerMenu menu){
        return (List<ItemStack>)ForeignMenuAccess.invoke(menu,"getAvailableResults");
    }
    private static boolean equal(List<ItemStack> a,List<ItemStack> b){
        if(a==null||a.size()!=b.size())return false;
        for(int i=0;i<a.size();i++)if(!ItemStack.matches(a.get(i),b.get(i)))return false;
        return true;
    }
    public static void initializeCommon(){
        DesktopEnvironment.onServerStarting(server->DesktopEnvironment.registerIntegration("graveyard",()->{
            for(String id:List.of("graveyard:ossuary_screen_handler","graveyard:ossuary")){
                var type=NativeMenuSupport.type(id);if(type==null)continue;
                NativeMenuSupport.bindNative(type,false);
                if(DesktopServerApi.findWindowHandler(type)!=null)continue;
                DesktopServerApi.registerWindow(type,new DesktopServerWindowHandler<AbstractContainerMenu,State>(){
                    @Override public boolean supports(net.minecraft.server.level.ServerPlayer player,AbstractContainerMenu menu){return menu.getClass().getName().equals("com.lion.graveyard.gui.OssuaryScreenHandler");}
                    @Override public boolean usesNativePlayerInventoryQuickMove(AbstractContainerMenu menu){return true;}
                    @Override public State createState(DesktopServerSessionContext<AbstractContainerMenu,State> c){return new State();}
                    @Override public void tick(DesktopServerSessionContext<AbstractContainerMenu,State> c){
                        var list=results(c.menu());if(equal(c.state().sent,list))return;
                        var b=new RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),c.player().registryAccess());
                        try {
                            b.writeVarInt(list.size());for(var stack:list)ItemStack.STREAM_CODEC.encode(b,stack);
                            byte[] data=new byte[b.readableBytes()];b.readBytes(data);c.sendToClient(RECIPES,data);
                            c.state().sent=list.stream().map(ItemStack::copy).toList();
                        }finally{b.release();}
                    }
                });
            }
        }));
    }
    public static final class Template extends MockupNativeTemplate<Void>{
        public Template(){super(NativeBatchLayouts::ossuary);}
        @Override public void customPayload(DesktopWindowContext<AbstractContainerMenu,Void> c,Identifier channel,byte[] data){
            if(!channel.equals(RECIPES))return;
            var b=new RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.wrappedBuffer(data),c.minecraft().player.registryAccess());
            try {
                int count=b.readVarInt();if(count<0||count>4096)throw new IllegalArgumentException("Invalid ossuary recipe count");
                var list=new java.util.ArrayList<ItemStack>();for(int i=0;i<count;i++)list.add(ItemStack.STREAM_CODEC.decode(b));
                if(b.isReadable())throw new IllegalArgumentException("Invalid ossuary recipe payload");
                ForeignMenuAccess.invoke(c.model(),"setAvailableResults",new Class<?>[]{List.class},list);
            }finally{b.release();}
        }
    }
}
