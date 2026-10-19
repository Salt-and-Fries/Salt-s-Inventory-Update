package com.salts_inventory_update.internal.desktop;

import static org.junit.jupiter.api.Assertions.*;
import com.salts_inventory_update.api.desktop.DesktopMenuBindings;
import java.util.List;
import java.util.Map;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

final class ContextualRecipeSourceIdentityTest {
    @BeforeAll static void bootstrap(){SharedConstants.tryDetectVersion();Bootstrap.bootStrap();}
    private static final SimpleContainer SHARED_DUMMY = new SimpleContainer(1);
    private static final class Menu extends AbstractContainerMenu {
        final Object physicalOwner;
        final boolean virtual;
        Menu(MenuType<?> type,Object owner,boolean virtual){
            super(type,1);physicalOwner=owner;this.virtual=virtual;
            addSlot(new Slot(SHARED_DUMMY,0,0,0));
        }
        @Override public boolean stillValid(Player player){return true;}
        @Override public ItemStack quickMoveStack(Player player,int slot){return ItemStack.EMPTY;}
        ContextualRecipeSources.OwnedSlot source(){return new ContextualRecipeSources.OwnedSlot(this,getSlot(0));}
    }
    private static MenuType<Menu> type(){
        MenuType<Menu> type=new MenuType<>((id,inventory)->null,FeatureFlags.DEFAULT_FLAGS);
        DesktopMenuBindings.register(type,new DesktopMenuBindings.SlotAccess(){
            @Override public int size(AbstractContainerMenu menu){return menu.slots.size();}
            @Override public boolean isVirtualSlot(AbstractContainerMenu menu,Slot slot){return ((Menu)menu).virtual;}
        },false,null,null,new DesktopMenuBindings.GesturePolicy(){
            @Override public Object physicalOwner(AbstractContainerMenu menu,Slot slot){return ((Menu)menu).physicalOwner;}
            @Override public int physicalIndex(AbstractContainerMenu menu,Slot slot){return 7;}
        });
        return type;
    }
    @Test void distinctNativeOwnersSharingDummyContainerBothContribute(){
        MenuType<Menu> type=type();
        // Equal values are still distinct backing owners; source identity must use ==.
        Menu first=new Menu(type,new String("owner"),false),second=new Menu(type,new String("owner"),false);
        assertSame(first.getSlot(0).container,second.getSlot(0).container);
        assertEquals(List.of(first.getSlot(0),second.getSlot(0)),
            ContextualRecipeSources.uniquePhysical(List.of(first.source(),second.source()),List.of()));
    }
    @Test void aliasedViewsContributeOnceAndPhysicalExclusionAppliesAcrossViews(){
        MenuType<Menu> type=type();Object owner=new Object();
        Menu first=new Menu(type,owner,false),alias=new Menu(type,owner,false),other=new Menu(type,new Object(),false);
        assertEquals(List.of(first.getSlot(0),other.getSlot(0)),
            ContextualRecipeSources.uniquePhysical(List.of(first.source(),alias.source(),other.source()),List.of()));
        assertEquals(List.of(other.getSlot(0)),
            ContextualRecipeSources.uniquePhysical(List.of(first.source(),other.source()),List.of(alias.source())));
    }
    @Test void declaredVirtualSlotNeverContributesToPhysicalRecipePool(){
        MenuType<Menu> type=type();
        Menu virtual=new Menu(type,new Object(),true),real=new Menu(type,new Object(),false);
        assertEquals(List.of(real.getSlot(0)),
            ContextualRecipeSources.uniquePhysical(List.of(virtual.source(),real.source()),List.of()));
    }
    @Test void outputCapacityMatchesRealOwnerAndRejectsVirtualDestinations(){
        MenuType<Menu> type=type();Object owner=new String("owner");
        Menu source=new Menu(type,owner,false),alias=new Menu(type,owner,false),distinct=new Menu(type,new String("owner"),false);
        Menu virtual=new Menu(type,new Object(),true);
        // All native Slot.container values are the same; only the true alias was freed.
        SHARED_DUMMY.setItem(0,new ItemStack(Items.COBBLESTONE,64));
        var consumed=Map.of(ContextualRecipeSources.key(source,source.getSlot(0)),ItemStack.EMPTY);
        try{
            assertEquals(64,ContextualRecipeSources.outputCapacity(alias,alias.getSlot(0),new ItemStack(Items.OAK_PLANKS),consumed));
            assertEquals(0,ContextualRecipeSources.outputCapacity(distinct,distinct.getSlot(0),new ItemStack(Items.OAK_PLANKS),consumed));
            assertEquals(0,ContextualRecipeSources.outputCapacity(virtual,virtual.getSlot(0),new ItemStack(Items.OAK_PLANKS),Map.of()));
        }finally{SHARED_DUMMY.clearContent();}
    }
}
