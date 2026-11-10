package com.salts_inventory_update.server;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

final class PlayerMenuClickSnapshotsTest {
    private PlayerMenuClickSnapshots history;
    private List<Slot> slots;
    private ItemStack cursor;
    private static final long NOW=1_000_000_000L;

    @BeforeAll static void bootstrap() throws Exception {
        SharedConstants.tryDetectVersion();Bootstrap.bootStrap();
        com.salts_inventory_update.functionaltest.HeadlessItemDefaults.bindStackCapacity();
    }
    @BeforeEach void setup(){
        history=new PlayerMenuClickSnapshots();SimpleContainer contents=new SimpleContainer(3);
        contents.setItem(0,new ItemStack(Items.DIRT));mark(contents.getItem(0),4);
        slots=List.of(new Slot(contents,0,0,0),new Slot(contents,1,0,0),new Slot(contents,2,0,0));
        cursor=new ItemStack(Items.SAND,3);remember(10);
    }
    private static void mark(ItemStack stack,int value) {
        try {
            try {
                Object tag=ItemStack.class.getMethod("getOrCreateTag").invoke(stack);
                tag.getClass().getMethod("putInt",String.class,int.class).invoke(tag,"siu_charge",value);
            } catch (NoSuchMethodException componentsEra) {
                Object key=Class.forName("net.minecraft.core.component.DataComponents").getField("CUSTOM_NAME").get(null);
                Class<?> type=Class.forName("net.minecraft.core.component.DataComponentType");
                ItemStack.class.getMethod("set",type,Object.class).invoke(stack,key,net.minecraft.network.chat.Component.literal("charge:"+value));
            }
        } catch (ReflectiveOperationException error) {throw new AssertionError(error);}
    }
    private void remember(int revision){history.remember(revision,slots,cursor,NOW);}
    private boolean permits(){return history.permits(10,1,slots,cursor,cursor.copy(),s->s==slots.get(0),NOW+100_000_000L);}
    @ParameterizedTest @ValueSource(booleans={true,false})
    void aPublishedCursorChangeGetsItsOwnNativeRevisionBeforeSnapshotting(boolean nativeCursorAdvancesRevision) throws Exception {
        var menu=new net.minecraft.world.inventory.AbstractContainerMenu(null,0) {
            { for(Slot slot:PlayerMenuClickSnapshotsTest.this.slots) this.addSlot(slot); }
            @Override public boolean stillValid(net.minecraft.world.entity.player.Player player){return true;}
            @Override public ItemStack quickMoveStack(net.minecraft.world.entity.player.Player player,int slot){return ItemStack.EMPTY;}
        };
        var setter=java.util.Arrays.stream(net.minecraft.world.inventory.AbstractContainerMenu.class.getMethods())
            .filter(method->method.getName().equals("setSynchronizer")).findFirst().orElseThrow();
        Object synchronizer=java.lang.reflect.Proxy.newProxyInstance(setter.getParameterTypes()[0].getClassLoader(),
            setter.getParameterTypes(),(proxy,method,args)->{
                if(method.getName().equals("createSlot")){
                    ItemStack[] last={ItemStack.EMPTY};
                    return java.lang.reflect.Proxy.newProxyInstance(method.getReturnType().getClassLoader(),
                        new Class<?>[]{method.getReturnType()},(tracker,operation,values)->{
                            if(operation.getName().equals("force")){last[0]=((ItemStack)values[0]).copy();return null;}
                            if(operation.getName().equals("matches"))return ItemStack.matches(last[0],(ItemStack)values[0]);
                            throw new AssertionError("Unexpected native tracker operation: "+operation);
                        });
                }
                if((nativeCursorAdvancesRevision&&method.getName().equals("sendCarriedChange"))||method.getName().equals("sendSlotChange")
                        ||method.getName().equals("sendInitialData")) menu.incrementStateId();
                return null;
            });
        setter.invoke(menu,synchronizer);
        var published=new PlayerMenuClickSnapshots();
        published.rememberPublished(menu,NOW);
        int before=menu.getStateId();
        published.rememberPublished(menu,NOW);
        assertEquals(before,menu.getStateId(),"An unchanged publication must not allocate another revision");
        menu.setCarried(cursor.copy());
        published.rememberPublished(menu,NOW+1);
        int revision=menu.getStateId();
        assertNotEquals(before,revision,"A new cursor cannot reuse an already published immutable revision");
        mark(slots.get(0).getItem(),5);
        assertTrue(published.permits(revision,1,menu.slots,menu.getCarried(),cursor.copy(),
            slot->slot==slots.get(0),NOW+2));
        assertFalse(published.permits(before,1,menu.slots,menu.getCarried(),cursor.copy(),
            slot->slot==slots.get(0),NOW+2));
        slots.get(1).set(new ItemStack(Items.DIRT));
        assertFalse(published.permits(revision,1,menu.slots,menu.getCarried(),cursor.copy(),slot->true,NOW+2));
    }
    @Test void lockedSourceMetadataCanTickWhileReturningTheUnchangedCursor(){
        mark(slots.get(0).getItem(),5);assertTrue(permits());
    }
    @Test void changedClickedContentsStillReject(){
        slots.get(1).set(new ItemStack(Items.DIRT));assertFalse(permits());
    }
    @Test void changedOtherEditableContentsStillReject(){
        slots.get(2).set(new ItemStack(Items.SAND));assertFalse(permits());
    }
    @Test void changedOtherEditableMetadataStillReject(){
        slots.get(2).set(new ItemStack(Items.DIRT));remember(11);
        mark(slots.get(2).getItem(),8);
        assertFalse(history.permits(11,1,slots,cursor,cursor.copy(),s->s==slots.get(0),NOW+1));
    }
    @Test void changedAuthoritativeCursorStillReject(){cursor.setCount(2);assertFalse(permits());}
    @Test void changedClientCursorStillReject(){
        assertFalse(history.permits(10,1,slots,cursor,new ItemStack(Items.SAND,2),s->true,NOW+1));
    }
    @Test void cursorMetadataMustRemainExact(){
        cursor=new ItemStack(Items.DIRT);remember(11);mark(cursor,3);
        assertFalse(history.permits(11,1,slots,cursor,cursor.copy(),s->true,NOW+1));
    }
    @Test void lockedSourceCountChangeStillReject(){slots.get(0).getItem().setCount(2);assertFalse(permits());}
    @Test void lockedSourceTypeChangeStillReject(){slots.get(0).set(new ItemStack(Items.SAND));assertFalse(permits());}
    @Test void clickedLockedSourceMetadataStillReject(){
        mark(slots.get(0).getItem(),5);
        assertFalse(history.permits(10,0,slots,cursor,cursor.copy(),s->true,NOW+1));
    }
    @Test void sourceMustStillBeLocked(){
        mark(slots.get(0).getItem(),5);
        assertFalse(history.permits(10,1,slots,cursor,cursor.copy(),s->false,NOW+1));
    }
    @Test void expiredAndUnknownRevisionsStillReject(){
        assertFalse(history.permits(10,1,slots,cursor,cursor.copy(),s->true,NOW+2_000_000_001L));
        assertFalse(history.permits(99,1,slots,cursor,cursor.copy(),s->true,NOW+1));
    }
    @Test void equalItemsInAReplacedSlotTopologyStillReject(){
        List<Slot> replacement=new ArrayList<>(slots);replacement.set(1,new Slot(new SimpleContainer(1),0,0,0));
        assertFalse(history.permits(10,1,replacement,cursor,cursor.copy(),s->true,NOW+1));
    }
    @Test void aRevisionCannotBeOverwrittenWithAnUnpublishedEditableChange(){
        slots.get(1).set(new ItemStack(Items.DIRT));remember(10);assertFalse(permits());
    }
    @Test void oldRevisionsAreBoundedAndConnectionResetClearsThem(){
        for(int i=11;i<=42;i++)remember(i);
        assertFalse(permits());assertTrue(history.permits(42,1,slots,cursor,cursor.copy(),s->true,NOW+1));
        history.clear();assertFalse(history.permits(42,1,slots,cursor,cursor.copy(),s->true,NOW+1));
    }
    @Test void outOfRangeOrResizedInventoryStillReject(){
        assertFalse(history.permits(10,-1,slots,cursor,cursor.copy(),s->true,NOW+1));
        assertFalse(history.permits(10,3,slots,cursor,cursor.copy(),s->true,NOW+1));
        assertFalse(history.permits(10,1,slots.subList(0,2),cursor,cursor.copy(),s->true,NOW+1));
    }
}
