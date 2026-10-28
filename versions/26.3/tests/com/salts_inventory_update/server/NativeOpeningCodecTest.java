package com.salts_inventory_update.server;

import com.salts_inventory_update.internal.desktop.AutomaticContainerSupport;
import io.netty.buffer.Unpooled;
import java.util.function.Consumer;
import net.minecraft.SharedConstants;
import net.minecraft.core.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.chat.Component;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/** Actual opening bytes must be decoded before invoking an erased generic factory. */
class NativeOpeningCodecTest {
    static final BlockPos SOURCE=new BlockPos(14,65,-12);
    static final StreamCodec<RegistryFriendlyByteBuf,BlockPos> CODEC=StreamCodec.of(
        (buffer,value)->buffer.writeBlockPos(value),buffer -> buffer.readBlockPos());
    @BeforeAll static void setup(){SharedConstants.tryDetectVersion();Bootstrap.bootStrap();}
    static byte[] opening(MenuType<?> type) {
        Consumer<RegistryFriendlyByteBuf> writer=AutomaticContainerSupport.openingWriter(new Provider(),null,type);
        assertNotNull(writer);var buffer=new RegistryFriendlyByteBuf(Unpooled.buffer(),RegistryAccess.EMPTY);
        try{writer.accept(buffer);byte[] bytes=new byte[buffer.readableBytes()];buffer.readBytes(bytes);return bytes;}
        finally{buffer.release();}
    }
    @Test void oldAndNewPublicCodecGettersRoundTripTypedOpeningData() {
        for(var type:new MenuType<?>[]{new LegacyType(),new ModernType()}) {
            var menu=(ProbeMenu)AutomaticContainerSupport.create(type,7,null,RegistryAccess.EMPTY,opening(type));
            assertEquals(SOURCE,menu.source);assertEquals(7,menu.containerId);
        }
    }
    @Test void trailingOpeningDataIsRejected() {
        var type=new ModernType();var data=opening(type);var extra=java.util.Arrays.copyOf(data,data.length+1);
        assertThrows(IllegalArgumentException.class,()->AutomaticContainerSupport.create(type,7,null,RegistryAccess.EMPTY,extra));
    }
    @Test void erasedObjectFactoryIsNeverTreatedAsARawByteBufferFactory() {
        var menu=(ProbeMenu)AutomaticContainerSupport.create(new UnknownType(),7,null,RegistryAccess.EMPTY,new byte[0]);
        assertEquals(BlockPos.ZERO,menu.source);
    }
    public static class BaseType extends MenuType<ProbeMenu> {
        BaseType(){super((id,inventory)->new ProbeMenu(id,BlockPos.ZERO),FeatureFlags.DEFAULT_FLAGS);}
        public ProbeMenu create(int id,Inventory inventory,BlockPos value){return new ProbeMenu(id,value);}
    }
    public static final class LegacyType extends BaseType {
        public StreamCodec<RegistryFriendlyByteBuf,BlockPos> getPacketCodec(){return CODEC;}
    }
    public static final class ModernType extends BaseType {
        public StreamCodec<RegistryFriendlyByteBuf,BlockPos> getStreamCodec(){return CODEC;}
    }
    public static final class UnknownType extends MenuType<ProbeMenu> {
        UnknownType(){super((id,inventory)->new ProbeMenu(id,BlockPos.ZERO),FeatureFlags.DEFAULT_FLAGS);}
        public ProbeMenu create(int id,Inventory inventory,Object value){throw new AssertionError("Erased data is not raw opening bytes");}
    }
    public static final class Provider implements MenuProvider {
        public BlockPos getScreenOpeningData(ServerPlayer player){return SOURCE;}
        @Override public Component getDisplayName(){return Component.literal("Probe");}
        @Override public AbstractContainerMenu createMenu(int id,Inventory inventory,Player player){return new ProbeMenu(id,SOURCE);}
    }
    static final class ProbeMenu extends AbstractContainerMenu {
        final BlockPos source;
        ProbeMenu(int id,BlockPos source){super(null,id);this.source=source;}
        @Override public boolean stillValid(Player player){return true;}
        @Override public ItemStack quickMoveStack(Player player,int index){return ItemStack.EMPTY;}
    }
}
