"""Scope block identity around the real game-mode call, after packet validation."""
from pathlib import Path
import re
ROOT=Path(__file__).resolve().parents[2]
METHOD='''
    @org.spongepowered.asm.mixin.injection.Redirect(method = "handleUseItemOn", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/server/level/ServerPlayerGameMode;useItemOn(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/phys/BlockHitResult;)Lnet/minecraft/world/InteractionResult;"))
    private net.minecraft.world.InteractionResult salts_inventory_update$scopedBlockTarget(
        net.minecraft.server.level.ServerPlayerGameMode mode, ServerPlayer player, net.minecraft.world.level.Level level,
        net.minecraft.world.item.ItemStack stack, net.minecraft.world.InteractionHand hand, net.minecraft.world.phys.BlockHitResult hit) {
        return DesktopContainerSessions.withUseTarget(player, hit, () -> mode.useItemOn(player, level, stack, hand, hit));
    }
'''
HELPER='''    /** Only a validated game-mode block interaction may lend its source identity
     * to a menu opener. Packet cancellation and thread handoff happen outside. */
    public static <T> T withUseTarget(ServerPlayer player, BlockHitResult hit, java.util.function.Supplier<T> action) {
        String target = shouldCapture(player) && hit.getType() == HitResult.Type.BLOCK
            ? sourceKeyForBlock(player, hit.getBlockPos()) : null;
        return withPendingUseTarget(player, target, action);
    }

    private static <T> T withPendingUseTarget(ServerPlayer player, @Nullable String target, java.util.function.Supplier<T> action) {
        var identity = IDENTITY;
        String previous = PENDING_USE_TARGETS.get(identity);
        if (target == null) PENDING_USE_TARGETS.remove(identity);
        else PENDING_USE_TARGETS.put(identity, target);
        try { return action.get(); }
        finally {
            if (previous == null) PENDING_USE_TARGETS.remove(identity);
            else PENDING_USE_TARGETS.put(identity, previous);
        }
    }

'''
TEST='''package com.salts_inventory_update.server;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.Supplier;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

class ScopedUseTargetTest {
    private static sun.misc.Unsafe unsafe;
    private ServerPlayer first,second;
    @BeforeAll static void bootstrap()throws Exception{
        SharedConstants.tryDetectVersion();Bootstrap.bootStrap();
        var field=sun.misc.Unsafe.class.getDeclaredField("theUnsafe");field.setAccessible(true);unsafe=(sun.misc.Unsafe)field.get(null);
    }
    private static ServerPlayer player()throws Exception{
        var p=(ServerPlayer)unsafe.allocateInstance(ServerPlayer.class);
        p.connection=(ServerGamePacketListenerImpl)unsafe.allocateInstance(ServerGamePacketListenerImpl.class);return p;
    }
    @BeforeEach void setup()throws Exception{first=player();second=player();}
    @SuppressWarnings("unchecked") private static Map<Object,String> targets()throws Exception{
        var field=DesktopContainerSessions.class.getDeclaredField("PENDING_USE_TARGETS");field.setAccessible(true);return (Map<Object,String>)field.get(null);
    }
    private static String pending(ServerPlayer p)throws Exception{
        var values=targets();return values.get(values.containsKey(p)?p:p.connection);
    }
    private static Object scoped(ServerPlayer p,String key,Supplier<?> action){
        try{var m=DesktopContainerSessions.class.getDeclaredMethod("withPendingUseTarget",ServerPlayer.class,String.class,Supplier.class);m.setAccessible(true);return m.invoke(null,p,key,action);}
        catch(InvocationTargetException e){throw (RuntimeException)e.getCause();}
        catch(ReflectiveOperationException e){throw new AssertionError(e);}
    }
    private static String read(ServerPlayer p){try{return pending(p);}catch(Exception e){throw new AssertionError(e);}}
    @AfterEach void cleanup()throws Exception{for(var p:List.of(first,second)){targets().remove(p);targets().remove(p.connection);}}
    @Test void successfulCallbackCannotLeakItsBlockToTheNextItemMenu(){
        assertEquals("native-result",scoped(first,"block:first",()->{assertEquals("block:first",read(first));return "native-result";}));
        assertNull(read(first));
        scoped(first,null,()->{assertNull(read(first));return null;});
    }
    @Test void nativeExceptionAlwaysReleasesThePendingTarget(){
        var expected=new IllegalStateException("native opener failed");
        assertSame(expected,assertThrows(IllegalStateException.class,()->scoped(first,"block:first",()->{throw expected;})));
        assertNull(read(first));
    }
    @Test void nestedOpenRestoresTheOuterSourceAndThenClearsIt(){
        scoped(first,"block:outer",()->{scoped(first,"block:inner",()->{assertEquals("block:inner",read(first));return null;});assertEquals("block:outer",read(first));return null;});
        assertNull(read(first));
    }
    @Test void_twoPlayersAndAnUncapturedNestedActionCannotBorrowEachOthersSource(){
        scoped(first,"block:first",()->{scoped(second,"block:second",()->{assertEquals("block:first",read(first));assertEquals("block:second",read(second));return null;});
            scoped(first,null,()->{assertNull(read(first));return null;});assertEquals("block:first",read(first));assertNull(read(second));return null;});
        assertNull(read(first));assertNull(read(second));
    }
}
'''.replace('void_twoPlayers','void twoPlayers')
for mc in ['1.20.1','1.21.1','1.21.11','26.1.2','26.2','26.3']:
    base=ROOT/f'versions/{mc}/fabric/src'
    path=base/'main/java/com/salts_inventory_update/mixin/server/ServerGamePacketListenerImplMixin.java';text=path.read_text()
    if 'scopedBlockTarget' not in text:
        text=re.sub(r'    @Inject\(method = "handleUseItemOn", at = @At\("RETURN"\)\)\n    private void salts_inventory_update\$clearDesktopUseTarget\([^\n]+\n        DesktopContainerSessions.clearUseTarget\(this.player\);\n    }\n','',text)
        if mc=='1.20.1':text=text.replace('        DesktopContainerSessions.captureUseTarget(this.player, packet.getHitResult());\n','')
        else:
            text=re.sub(r'    @Inject\(method = "handleUseItemOn", at = @At\("HEAD"\)\)\n    private void salts_inventory_update\$captureDesktopUseTarget\([^\n]+\n        DesktopContainerSessions.captureUseTarget\(this.player, packet\.(?:getHitResult|hitResult)\(\)\);\n    }\n','',text)
        assert 'captureUseTarget(this.player' not in text and 'clearUseTarget(this.player' not in text,path
        text=text.rstrip()[:-1]+METHOD+'}\n';path.write_text(text)
    path=base/'main/java/com/salts_inventory_update/server/DesktopContainerSessions.java';text=path.read_text()
    if 'withPendingUseTarget(' not in text:
        anchor='    public static void captureUseTarget('
        assert text.count(anchor)==1,path
        text=text.replace(anchor,HELPER.replace('IDENTITY','player.connection' if mc=='1.20.1' else 'player')+anchor)
        path.write_text(text)
    path=base/'test/java/com/salts_inventory_update/server/ScopedUseTargetTest.java';path.parent.mkdir(parents=True,exist_ok=True);path.write_text(TEST)
