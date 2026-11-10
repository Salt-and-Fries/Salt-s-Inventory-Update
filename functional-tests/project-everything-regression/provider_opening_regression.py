"""Install a regression for NeoForge provider-owned opening bytes before extra data."""
from pathlib import Path
import argparse
ROOT=Path(__file__).resolve().parents[2]
VERSIONS=['1.21.1','1.21.11','26.1.2','26.2','26.3']
HELPER='''    /** NeoForge writes provider data before the caller's extra opening bytes. */
    public static Consumer<net.minecraft.network.RegistryFriendlyByteBuf> providerOpeningWriter(
        MenuProvider provider, AbstractContainerMenu menu, Consumer<net.minecraft.network.RegistryFriendlyByteBuf> extra) {
        try {
            Method nativeWriter=provider.getClass().getMethod("writeClientSideData",AbstractContainerMenu.class,net.minecraft.network.RegistryFriendlyByteBuf.class);
            return buffer->{invoke(nativeWriter,provider,menu,buffer);if(extra!=null)extra.accept(buffer);};
        } catch(NoSuchMethodException absentOnFabric) { return extra; }
    }
'''
TEST='''package com.salts_inventory_update.internal.desktop;
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
}
'''
def main():
    p=argparse.ArgumentParser();p.add_argument('--fix',action='store_true');a=p.parse_args()
    for mc in VERSIONS:
        folder=ROOT/f'versions/{mc}/fabric/src'
        source=folder/'main/java/com/salts_inventory_update/internal/desktop/AutomaticContainerSupport.java'
        text=source.read_text()
        if 'providerOpeningWriter(' not in text:
            stub=HELPER if a.fix else HELPER[:HELPER.index('        try {')]+'        return extra;\n    }\n'
            text=text.replace('    @SuppressWarnings({"rawtypes","unchecked"})\n    public static AbstractContainerMenu create(',stub+'    @SuppressWarnings({"rawtypes","unchecked"})\n    public static AbstractContainerMenu create(')
        elif a.fix:
            start=text.index('    /** NeoForge writes provider data')
            end=text.index('    @SuppressWarnings({"rawtypes","unchecked"})',start)
            text=text[:start]+HELPER+text[end:]
        assert 'providerOpeningWriter(' in text,mc
        source.write_text(text)
        if a.fix:
            source=folder/'main/java/com/salts_inventory_update/server/DesktopContainerSessions.java';text=source.read_text()
            anchor='        byte[] openingData;'
            line='        if(automaticNative(player,sessions,menu))openingDataWriter=com.salts_inventory_update.internal.desktop.AutomaticContainerSupport.providerOpeningWriter(provider,menu,openingDataWriter);\n'
            assert text.count(anchor)==1,mc
            if line not in text:text=text.replace(anchor,line+anchor)
            # Native fallback calls the loader itself, which writes provider
            # bytes. Save the caller writer rather than composing those twice.
            saved='        Consumer<RegistryFriendlyByteBuf> nativeOpeningDataWriter=openingDataWriter;\n'
            if saved not in text:text=text.replace(line,saved+line)
            text=text.replace('session.nativeProvider=provider;session.nativeWriter=openingDataWriter;',
                              'session.nativeProvider=provider;session.nativeWriter=nativeOpeningDataWriter;')
            source.write_text(text)
        target=folder/'test/java/com/salts_inventory_update/internal/desktop/NativeProviderOpeningDataTest.java';target.parent.mkdir(parents=True,exist_ok=True);target.write_text(TEST)
if __name__=='__main__':main()
