package com.salts_inventory_update.internal.desktop;
import java.util.function.Consumer;
import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NativeProviderOpeningDataTest {
    static class Ordinary implements MenuProvider {
        public Component getDisplayName(){return null;}
        public AbstractContainerMenu createMenu(int id,Inventory inventory,Player player){return null;}
    }
    static class Extended extends Ordinary {
        int calls;
        public void writeClientSideData(AbstractContainerMenu menu,RegistryFriendlyByteBuf buffer){calls++;buffer.writeLong(0x1020304050607080L);}
    }
    static class Broken extends Ordinary {
        public void writeClientSideData(AbstractContainerMenu menu,RegistryFriendlyByteBuf buffer){throw new IllegalArgumentException("invalid provider state");}
    }
    private RegistryFriendlyByteBuf buffer(){return new RegistryFriendlyByteBuf(Unpooled.buffer(),RegistryAccess.EMPTY);}
    @Test void ordinaryFabricProviderKeepsExactExtraWriter(){
        Consumer<RegistryFriendlyByteBuf> extra=b->b.writeInt(42);
        assertSame(extra,AutomaticContainerSupport.providerOpeningWriter(new Ordinary(),null,extra));
        assertNull(AutomaticContainerSupport.providerOpeningWriter(new Ordinary(),null,null));
    }
    @Test void providerBytesPrecedeCallerBytesExactlyOnce(){
        var provider=new Extended();var writer=AutomaticContainerSupport.providerOpeningWriter(provider,null,b->b.writeInt(42));
        assertEquals(0,provider.calls,"Construction must not run the writer before encoding");
        var b=buffer();try{writer.accept(b);assertEquals(1,provider.calls);assertEquals(0x1020304050607080L,b.readLong());assertEquals(42,b.readInt());assertFalse(b.isReadable());}finally{b.release();}
    }
    @Test void providerStillWritesWhenCallerHasNoExtraBytes(){
        var b=buffer();try{AutomaticContainerSupport.providerOpeningWriter(new Extended(),null,null).accept(b);assertEquals(0x1020304050607080L,b.readLong());assertFalse(b.isReadable());}finally{b.release();}
    }
    @Test void providerFailureCannotBecomeAnEmptySuccessfulPayload(){
        var b=buffer();try{var failure=assertThrows(IllegalStateException.class,()->AutomaticContainerSupport.providerOpeningWriter(new Broken(),null,null).accept(b));assertInstanceOf(IllegalArgumentException.class,failure.getCause());}finally{b.release();}
    }
    @Test void registeredFactoryKeepsItsExactPayloadWithoutProviderBytes(){
        var provider=new Extended();var b=buffer();
        try{
            AutomaticContainerSupport.captureOpeningWriter(provider,null,null,data->{data.writeLong(123L);data.writeInt(4);},true).accept(b);
            assertEquals(0,provider.calls);assertEquals(12,b.readableBytes());assertEquals(123L,b.readLong());assertEquals(4,b.readInt());assertFalse(b.isReadable());
        }finally{b.release();}
    }
    @Test void explicitCapturedWriterRetainsExistingPriorityForRegisteredFactory(){
        var provider=new Extended();Consumer<RegistryFriendlyByteBuf> captured=data->data.writeInt(42);
        assertSame(captured,AutomaticContainerSupport.captureOpeningWriter(provider,null,captured,data->data.writeLong(99L),false));
        assertEquals(0,provider.calls);
    }

    @Test void nativeFaceFragmentCannotReplaceRegisteredBlockPositionAndFace(){
        var provider=new Extended();var b=buffer();
        try{
            AutomaticContainerSupport.captureOpeningWriter(provider,null,data->data.writeInt(4),
                data->{data.writeLong(123L);data.writeInt(4);},true).accept(b);
            assertEquals(0,provider.calls);assertEquals(12,b.readableBytes());
            assertEquals(123L,b.readLong());assertEquals(4,b.readInt());assertFalse(b.isReadable());
        }finally{b.release();}
    }
    @Test void explicitApiPayloadWithoutRegistryIsAlreadyComplete(){
        var provider=new Extended();var b=buffer();
        try{
            AutomaticContainerSupport.captureOpeningWriter(provider,null,data->data.writeInt(42),null,false).accept(b);
            assertEquals(0,provider.calls);assertEquals(4,b.readableBytes());assertEquals(42,b.readInt());assertFalse(b.isReadable());
        }finally{b.release();}
    }
    @Test void explicitIntegrationNativeCaptureUsesCompleteRegisteredPayload(){
        var b=buffer();try{
            AutomaticContainerSupport.registeredOpeningWriter(data->data.writeInt(4),data->{data.writeLong(123L);data.writeInt(4);},true).accept(b);
            assertEquals(12,b.readableBytes());assertEquals(123L,b.readLong());assertEquals(4,b.readInt());assertFalse(b.isReadable());
        }finally{b.release();}
    }
    @Test void explicitIntegrationApiCallerStillOwnsItsCompletePayload(){
        Consumer<RegistryFriendlyByteBuf> explicit=data->data.writeInt(42);
        assertSame(explicit,AutomaticContainerSupport.registeredOpeningWriter(explicit,data->data.writeLong(99L),false));
    }
}
