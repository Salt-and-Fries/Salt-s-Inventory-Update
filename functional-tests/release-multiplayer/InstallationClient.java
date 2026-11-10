package com.siu_release_remote;

import com.google.gson.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import java.nio.file.*;
import java.util.*;
import java.util.function.Consumer;

/** Optional-side installation checks. No reference to Core is required to load this client. */
public final class InstallationClient implements ClientModInitializer {
    /** Explicit obsolete-wire peer; the real Core client is absent in this mode. */
    private record MismatchHello(int protocolVersion) implements CustomPacketPayload {
        static final Type<MismatchHello> TYPE=new Type<>(Identifier.fromNamespaceAndPath("salts_inventory_update","desktop_hello"));
        static final StreamCodec<RegistryFriendlyByteBuf,MismatchHello> CODEC=CustomPacketPayload.codec(MismatchHello::write,MismatchHello::read);
        private void write(RegistryFriendlyByteBuf buf){buf.writeVarInt(protocolVersion);buf.writeLong(123456789L);buf.writeVarLong(16L);buf.writeBoolean(true);buf.writeVarInt(0);buf.writeVarInt(0);}
        private static MismatchHello read(RegistryFriendlyByteBuf buf){int version=buf.readVarInt();buf.readLong();buf.readVarLong();buf.readBoolean();if(buf.readVarInt()!=0||buf.readVarInt()!=0)throw new IllegalArgumentException("QA hello requires empty valid menu lists");return new MismatchHello(version);}
        @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    private static final Gson JSON=new GsonBuilder().setPrettyPrinting().create();
    private static final Path OUT=Path.of(System.getenv("SIU_REMOTE_OUTPUT"));
    private static Minecraft mc;private static int age,stage,settle;private static long last=-1;private static JsonObject pending;
    private static void write(String name,Object value)throws Exception {
        AtomicObservation.write(OUT.resolve(name),JSON.toJson(value));
    }
    private static boolean corePresent(){try{Class.forName("com.salts_inventory_update.client.DesktopContainerClient");return true;}catch(ClassNotFoundException absent){return false;}}
    private static boolean sessions()throws Exception {return corePresent()&&(boolean)Class.forName("com.salts_inventory_update.client.DesktopContainerClient").getMethod("canUseServerSessions").invoke(null);}
    private static void preferences()throws Exception {
        if(!corePresent())return;
        Consumer<Object> update=value->{try{for(String key:List.of("enableMod","hasSeenInstructionsWindow","returnHotbarToInventory"))value.getClass().getField(key).setBoolean(value,true);value.getClass().getField("expandableInventory").setBoolean(value,false);}catch(Exception error){throw new IllegalStateException(error);}};
        Class.forName("com.salts_inventory_update.client.SaltsInventoryConfig").getMethod("update",Consumer.class).invoke(null,update);
    }
    private static Object stack(ItemStack item){return Map.of("item",BuiltInRegistries.ITEM.getKey(item.getItem()).toString(),"count",item.getCount());}
    private static int observed(Object value,String name)throws Exception {
        for(Class<?> type=value.getClass();type!=null;type=type.getSuperclass())try{var field=type.getDeclaredField(name);field.setAccessible(true);return field.getInt(value);}catch(NoSuchFieldException absent){}
        throw new IllegalStateException("Native geometry missing: "+name);
    }
    private static Object state()throws Exception {
        var value=new LinkedHashMap<String,Object>();value.put("corePresent",corePresent());value.put("serverSessions",sessions());
        value.put("connected",mc.player!=null&&mc.level!=null);value.put("screen",mc.gui.screen()==null?"none":mc.gui.screen().getClass().getName());
        value.put("screenText",mc.gui.screen()==null?"":mc.gui.screen().getNarrationMessage().getString());
        if(mc.player!=null){value.put("cursor",stack(mc.player.containerMenu.getCarried()));var rows=new ArrayList<Object>();
            for(int i=0;i<36;i++)rows.add(Map.of("index",i,"stack",stack(mc.player.getInventory().getItem(i))));value.put("inventory",rows);}
        if(mc.gui.screen() instanceof AbstractContainerScreen<?> screen){value.put("nativeMenu",screen.getMenu().getClass().getName());value.put("nativeSlotCount",screen.getMenu().slots.size());}
        return value;
    }
    @Override public void onInitializeClient(){
        if("protocol-mismatch".equals(System.getenv("SIU_INSTALL_MODE")))PayloadTypeRegistry.serverboundPlay().register(MismatchHello.TYPE,MismatchHello.CODEC);
        ClientTickEvents.END_CLIENT_TICK.register(client->{try{tick(client);}catch(Throwable failure){failure.printStackTrace();try{write("failure.json",Map.of("error",failure.toString(),"sequence",last));}catch(Exception nested){nested.printStackTrace();}client.stop();}});}
    private static void tick(Minecraft client)throws Exception {
        mc=client;age++;
        if(stage==0){if(age<60||!(mc.gui.screen() instanceof TitleScreen)||mc.gui.overlay()!=null)return;
            mc.options.pauseOnLostFocus=false;mc.options.renderDistance().set(2);mc.options.simulationDistance().set(5);preferences();
            String address=System.getenv("SIU_REMOTE_SERVER");ConnectScreen.startConnecting(new TitleScreen(),mc,ServerAddress.parseString(address),new ServerData("Installation QA",address,ServerData.Type.OTHER),false,null);stage=1;return;}
        if(stage==1){if(age%20==0)write("login-state.json",state());if(mc.player==null||mc.level==null||mc.gui.screen()!=null)return;
            write("ready.json",Map.of("dedicated",mc.getSingleplayerServer()==null));stage=2;return;}
        if(pending==null){Path path=OUT.resolve("command.json");if(!Files.isRegularFile(path))return;
            var command=JsonParser.parseString(Files.readString(path)).getAsJsonObject();long sequence=command.get("sequence").getAsLong();if(sequence<=last)return;
            last=sequence;pending=command;settle=command.has("ticks")?command.get("ticks").getAsInt():16;
            switch(command.get("type").getAsString()){
                case "state" -> {}
                case "use-block" -> {var pos=new BlockPos(8,65,8);mc.gameMode.useItemOn(mc.player,InteractionHand.MAIN_HAND,new BlockHitResult(new Vec3(8.5,66,8.5),Direction.UP,pos,false));}
                case "native-slot" -> {if(!(mc.gui.screen() instanceof AbstractContainerScreen<?> screen))throw new IllegalStateException("Expected native container");
                    var slot=screen.getMenu().getSlot(command.get("slot").getAsInt());double x=observed(screen,"leftPos")+slot.x+8,y=observed(screen,"topPos")+slot.y+8;
                    var event=new MouseButtonEvent(x,y,new MouseButtonInfo(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT,0));screen.mouseMoved(x,y);screen.mouseClicked(event,false);screen.mouseReleased(event);}
                case "close" -> {mc.gui.screen().onClose();}
                case "bad-protocol" -> {
                    if(corePresent())throw new IllegalStateException("Version mismatch must be the first hello from a peer without automatic Core negotiation");
                    ClientPlayNetworking.send(new MismatchHello(4));}
                case "finish" -> {mc.stop();return;}
                default -> throw new IllegalArgumentException(command.toString());
            }return;
        }
        if(settle-->0)return;write("reply.json",Map.of("sequence",last,"state",state()));pending=null;
    }
}
