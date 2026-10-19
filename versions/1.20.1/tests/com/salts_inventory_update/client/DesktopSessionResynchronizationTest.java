package com.salts_inventory_update.client;

import com.salts_inventory_update.api.client.desktop.DesktopWindowHandle;
import com.salts_inventory_update.internal.desktop.DesktopItemSourceLocks;
import com.salts_inventory_update.network.DesktopPackets.DesktopOpenSessionPayload;
import java.lang.reflect.*;
import java.util.*;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/** Native snapshot recovery must preserve a real API window only within its authenticated lifetime. */
final class DesktopSessionResynchronizationTest {
    private static sun.misc.Unsafe unsafe;
    private static Class<?> identityType,openingType,windowType;
    private Object previousSingleton;
    private final List<Integer> snapshots=new ArrayList<>();
    @BeforeAll static void bootstrap() throws Exception {
        SharedConstants.tryDetectVersion();Bootstrap.bootStrap();
        Field access=sun.misc.Unsafe.class.getDeclaredField("theUnsafe");access.setAccessible(true);unsafe=(sun.misc.Unsafe)access.get(null);
        identityType=Class.forName(DesktopContainerClient.class.getName()+"$SessionSnapshotIdentity");
        openingType=Class.forName(DesktopContainerClient.class.getName()+"$MenuOpenData");
        windowType=Class.forName(InventoryDesktopScreen.class.getName()+"$InventoryWindow");
    }
    @BeforeEach void saveSingleton() throws Exception {previousSingleton=field(InventoryDesktopScreen.class,"singleton");}
    @AfterEach void restoreSingleton() throws Exception {
        set(InventoryDesktopScreen.class,"singleton",previousSingleton);
        for(int id:snapshots)cache().remove(id);
    }
    @Test void sameLifetimeSnapshotRetainsPublicHandleNativeMenuAndPreview() throws Exception {
        Fixture fixture=fixture();
        var update=payload(29,new ItemStack(Items.OAK_PLANKS,12),17);
        assertTrue(resynchronize(fixture,update,771L,new byte[0],-1));
        assertSame(fixture.session,InventoryDesktopScreen.sessionForNetworking(fixture.minecraft,1));
        assertSame(fixture.menu,fixture.session.menu());
        assertTrue(fixture.handle.isOpen());assertEquals(fixture.id,fixture.handle.instanceId());
        assertEquals(fixture.id,field(fixture.screen,"contextualHoverSource"));
        assertEquals(1,field(fixture.screen,"contextualHoverSession"));
        assertEquals(29,fixture.menu.getStateId());assertEquals(12,fixture.menu.getSlot(0).getItem().getCount());
        assertEquals(17,fixture.menu.data.get());
    }
    @Test void newAuthorizationOrReplacementCannotRetainOldWindowAndRealClosureStillClosesHandle() throws Exception {
        Fixture fixture=fixture();var update=payload(3,new ItemStack(Items.STICK,2),8);
        assertFalse(resynchronize(fixture,update,772L,new byte[0],-1));
        assertFalse(resynchronize(fixture,update,771L,new byte[0],2));
        assertTrue(fixture.handle.isOpen());assertTrue(fixture.menu.getSlot(0).getItem().is(Items.COBBLESTONE));
        // This is the same window disposal path used by a real session replacement/removal.
        Method close=InventoryDesktopScreen.class.getDeclaredMethod("apiClosed",windowType);close.setAccessible(true);close.invoke(fixture.screen,fixture.window);
        fixture.session.close();
        assertFalse(fixture.handle.isOpen());assertTrue((Boolean)field(fixture.session,"closed"));
        assertEquals(0,((List<?>)field(fixture.screen,"windows")).size());
    }
    @Test void nativeTopologyAndOpeningDataChangesFallBackWithoutApplyingSnapshot() throws Exception {
        Fixture fixture=fixture();var update=payload(8,new ItemStack(Items.STICK,2),11);
        assertFalse(resynchronize(fixture,update,771L,new byte[]{4},-1));
        Slot original=fixture.menu.getSlot(0);
        fixture.menu.slots.set(0,new Slot(original.container,0,original.x,original.y));
        assertFalse(resynchronize(fixture,update,771L,new byte[0],-1));
        fixture.menu.slots.set(0,original);
        Menu replacement=new Menu();set(fixture.session,"menu",replacement);
        assertFalse(resynchronize(fixture,update,771L,new byte[0],-1));
        assertEquals(0,fixture.menu.getStateId());assertTrue(original.getItem().is(Items.COBBLESTONE));
    }
    @Test void changedNativeMetadataAndZeroTokenNeverBecomeSameLifetimeSnapshots() throws Exception {
        Fixture fixture=fixture();var original=payload(2,new ItemStack(Items.STICK,2),1);
        assertFalse(resynchronize(fixture,original,0L,new byte[0],-1));
        for(DesktopOpenSessionPayload changed:List.of(
            new DesktopOpenSessionPayload(1,1,0,0,0,2,true,true,"source",Component.literal("Session"),original.items(),ItemStack.EMPTY,original.data()),
            new DesktopOpenSessionPayload(1,0,0,0,0,2,false,true,"source",Component.literal("Session"),original.items(),ItemStack.EMPTY,original.data()),
            new DesktopOpenSessionPayload(1,0,0,0,0,2,true,false,"source",Component.literal("Session"),original.items(),ItemStack.EMPTY,original.data()),
            new DesktopOpenSessionPayload(1,0,0,0,0,2,true,true,"different",Component.literal("Session"),original.items(),ItemStack.EMPTY,original.data()),
            new DesktopOpenSessionPayload(1,0,0,0,0,2,true,true,"source",Component.literal("Changed"),original.items(),ItemStack.EMPTY,original.data()),
            new DesktopOpenSessionPayload(1,0,0,0,0,2,true,true,"source",Component.literal("Session"),original.items(),ItemStack.EMPTY,new int[2])
        ))assertFalse(resynchronize(fixture,changed,771L,new byte[0],-1));
        assertTrue(fixture.handle.isOpen());assertTrue(fixture.menu.getSlot(0).getItem().is(Items.COBBLESTONE));
    }
    private Fixture fixture() throws Exception {
        Menu menu=new Menu();menu.getSlot(0).set(new ItemStack(Items.COBBLESTONE,3));
        DesktopContainerSession session=(DesktopContainerSession)unsafe.allocateInstance(DesktopContainerSession.class);
        set(session,"sessionId",1);set(session,"menu",menu);set(session,"companionOwnerIds",new int[0]);
        set(session,"sourceLock",DesktopItemSourceLocks.acquire(null,menu));
        Minecraft minecraft=(Minecraft)unsafe.allocateInstance(Minecraft.class);
        LocalPlayer player=(LocalPlayer)unsafe.allocateInstance(LocalPlayer.class);
        InventoryMenu playerMenu=(InventoryMenu)unsafe.allocateInstance(InventoryMenu.class);
        set(playerMenu,"slots",NonNullList.create());playerMenu.setCarried(ItemStack.EMPTY);set(player,"inventoryMenu",playerMenu);minecraft.player=player;
        InventoryDesktopScreen screen=(InventoryDesktopScreen)unsafe.allocateInstance(InventoryDesktopScreen.class);
        set(screen,"minecraft",minecraft);set(screen,"owner",player);set(screen,"sharedCarried",ItemStack.EMPTY);
        set(screen,"sessions",new ArrayList<>(List.of(session)));set(screen,"windows",new ArrayList<>());
        minecraft.screen=screen;set(InventoryDesktopScreen.class,"singleton",screen);
        Object window=unsafe.allocateInstance(windowType);UUID id=UUID.randomUUID();
        set(window,"apiInstanceId",id);set(window,"session",session);set(window,"legacyContainerSlots",List.of());
        ((List<Object>)field(screen,"windows")).add(window);
        Method handleMethod=InventoryDesktopScreen.class.getDeclaredMethod("apiWindowHandle",windowType);handleMethod.setAccessible(true);
        DesktopWindowHandle handle=(DesktopWindowHandle)handleMethod.invoke(screen,window);
        set(screen,"contextualHoverSource",id);set(screen,"contextualHoverSession",1);
        Method capture=identityType.getDeclaredMethod("capture",AbstractContainerMenu.class,DesktopOpenSessionPayload.class,long.class,byte[].class);capture.setAccessible(true);
        cache().put(1,capture.invoke(null,menu,payload(0,new ItemStack(Items.COBBLESTONE,3),0),771L,new byte[0]));snapshots.add(1);
        return new Fixture(minecraft,screen,session,menu,window,handle,id);
    }
    private static boolean resynchronize(Fixture fixture,DesktopOpenSessionPayload payload,long token,byte[] data,int replacement) throws Exception {
        Constructor<?> constructor=openingType.getDeclaredConstructor(long.class,int.class,int.class,byte[].class,int[].class);constructor.setAccessible(true);
        Object opening=constructor.newInstance(token,payload.menuTypeId(),replacement,data,new int[0]);
        Method method=DesktopContainerClient.class.getDeclaredMethod("resynchronizeSession",Minecraft.class,DesktopOpenSessionPayload.class,long.class,openingType);method.setAccessible(true);
        return (Boolean)method.invoke(null,fixture.minecraft,payload,token,opening);
    }
    private static DesktopOpenSessionPayload payload(int state,ItemStack item,int data){return new DesktopOpenSessionPayload(1,0,0,0,0,state,true,true,"source",Component.literal("Session"),List.of(item),ItemStack.EMPTY,new int[]{data});}
    @SuppressWarnings("unchecked") private static Map<Integer,Object> cache() throws Exception{return (Map<Integer,Object>)field(DesktopContainerClient.class,"SESSION_SNAPSHOTS");}
    private static Object field(Object target,String name) throws Exception {Field field=find(target,name);return field.get(target instanceof Class<?>?null:target);}
    private static void set(Object target,String name,Object value) throws Exception {find(target,name).set(target instanceof Class<?>?null:target,value);}
    private static Field find(Object target,String name) throws Exception {
        for(Class<?> type=target instanceof Class<?> value?value:target.getClass();type!=null;type=type.getSuperclass())try{Field field=type.getDeclaredField(name);field.setAccessible(true);return field;}catch(NoSuchFieldException ignored){}
        throw new NoSuchFieldException(name);
    }
    private record Fixture(Minecraft minecraft,InventoryDesktopScreen screen,DesktopContainerSession session,Menu menu,Object window,DesktopWindowHandle handle,UUID id){}
    private static final class Menu extends AbstractContainerMenu {
        final DataSlot data=DataSlot.standalone();
        Menu(){super(MenuType.GENERIC_9x1,1);addSlot(new Slot(new SimpleContainer(1),0,4,5));addDataSlot(data);}
        @Override public boolean stillValid(Player player){return true;}
        @Override public ItemStack quickMoveStack(Player player,int index){return ItemStack.EMPTY;}
    }
}
