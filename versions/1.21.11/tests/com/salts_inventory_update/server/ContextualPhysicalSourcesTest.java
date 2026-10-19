package com.salts_inventory_update.server;

import com.salts_inventory_update.api.desktop.DesktopMenuBindings;
import java.lang.reflect.*;
import java.util.*;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/** Simulates native wrappers sharing an empty dummy container while owning different storage. */
final class ContextualPhysicalSourcesTest {
    private static Method add;
    private static ServerPlayer player;
    private static MenuType<TestMenu> type;
    private static final SimpleContainer DUMMY=new SimpleContainer(1);
    @BeforeAll static void initialize() throws Exception {
        SharedConstants.tryDetectVersion();Bootstrap.bootStrap();
        Field f=sun.misc.Unsafe.class.getDeclaredField("theUnsafe");f.setAccessible(true);
        player=(ServerPlayer)((sun.misc.Unsafe)f.get(null)).allocateInstance(ServerPlayer.class);
        type=new MenuType<>((id,inventory)->null,FeatureFlags.DEFAULT_FLAGS);
        DesktopMenuBindings.register(type,new DesktopMenuBindings.SlotAccess() {
            @Override public int size(AbstractContainerMenu menu){return menu.slots.size();}
            @Override public boolean isVirtualSlot(AbstractContainerMenu menu,Slot slot){return ((TestMenu)menu).phantom;}
        },false,null,null,new DesktopMenuBindings.GesturePolicy(){
            @Override public Object physicalOwner(AbstractContainerMenu menu,Slot slot){return ((TestMenu)menu).owner;}
            @Override public int physicalIndex(AbstractContainerMenu menu,Slot slot){return ((TestMenu)menu).physicalIndex;}
        });
        add=Arrays.stream(DesktopContainerSessions.class.getDeclaredMethods()).filter(m->m.getName().equals("addContextualRecipeSource")).findFirst().orElseThrow();add.setAccessible(true);
    }
    private static void append(TestMenu menu, Map<Object,Slot> result, Set<Object> excluded) throws Exception {
        add.invoke(null,player,result,menu,menu.slots.getFirst(),excluded);
    }
    private static Object key(Object owner,int index) throws Exception {
        var constructor=Class.forName(DesktopContainerSessions.class.getName()+"$ContextualSourceKey").getDeclaredConstructor(Object.class,int.class);
        constructor.setAccessible(true);return constructor.newInstance(owner,index);
    }
    @Test void distinctOwnersSharingDummyContainerAndIndexRemainSeparate() throws Exception {
        Object first=new EqualOwner(),second=new EqualOwner();Map<Object,Slot> result=new LinkedHashMap<>();
        append(new TestMenu(first,0,false),result,Set.of());append(new TestMenu(second,0,false),result,Set.of());
        var slots=new ArrayList<>(result.values());assertEquals(2,slots.size());assertSame(slots.get(0).container,slots.get(1).container);
    }
    @Test void aliasedViewsOfTheSamePhysicalSlotCountOnce() throws Exception {
        Object owner=new Object();Map<Object,Slot> result=new LinkedHashMap<>();
        append(new TestMenu(owner,0,false),result,Set.of());append(new TestMenu(owner,0,false),result,Set.of());
        assertEquals(1,result.size());
    }
    @Test void registeredPhysicalIndexSeparatesSlotsWithTheSameDummyIndex() throws Exception {
        Object owner=new Object();Map<Object,Slot> result=new LinkedHashMap<>();
        append(new TestMenu(owner,0,false),result,Set.of());append(new TestMenu(owner,1,false),result,Set.of());
        assertEquals(2,result.size());
    }
    @Test void virtualConfigurationSlotsNeverSupplyRealIngredients() throws Exception {
        Map<Object,Slot> result=new LinkedHashMap<>();append(new TestMenu(new Object(),0,true),result,Set.of());assertTrue(result.isEmpty());
    }
    @Test void excludingAnOwnedGridDoesNotHideAnotherDummyBackedInventory() throws Exception {
        Object first=new Object(),second=new Object();Set<Object> excluded=Set.of(key(first,0));
        Map<Object,Slot> result=new LinkedHashMap<>();
        append(new TestMenu(first,0,false),result,excluded);TestMenu other=new TestMenu(second,0,false);append(other,result,excluded);
        assertEquals(List.of(other.slots.getFirst()),new ArrayList<>(result.values()));
    }
    private static final class EqualOwner {
        @Override public boolean equals(Object other){return other instanceof EqualOwner;}
        @Override public int hashCode(){return 1;}
    }
    private static final class TestMenu extends AbstractContainerMenu {
        final Object owner;final int physicalIndex;final boolean phantom;
        TestMenu(Object owner,int index,boolean phantom){super(type,4);this.owner=owner;physicalIndex=index;this.phantom=phantom;
            addSlot(new Slot(DUMMY,0,0,0){private final ItemStack real=new ItemStack(Items.COAL,2);@Override public ItemStack getItem(){return real;}});}
        @Override public boolean stillValid(Player player){return true;}
        @Override public ItemStack quickMoveStack(Player player,int index){return ItemStack.EMPTY;}
    }
}
