package com.salts_inventory_update.compat.create;

import com.salts_inventory_update.api.desktop.DesktopItemSources;
import com.salts_inventory_update.api.server.desktop.DesktopSessions;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Native owner identity shared by source deduplication and held-item protection. */
final class CreateOwners {
    private CreateOwners() { }
    static Object field(Object owner,String name) {
        if(owner==null)return null;
        for(var t=owner.getClass();t!=null;t=t.getSuperclass())try {
            var f=t.getDeclaredField(name);f.setAccessible(true);return f.get(owner);
        }catch(NoSuchFieldException absent){}catch(ReflectiveOperationException failure){throw new IllegalStateException(name,failure);}
        return null;
    }
    static ItemStack item(AbstractContainerMenu menu) {
        if(field(menu,"contentHolder") instanceof ItemStack stack)return stack;
        if(field(field(menu,"data"),"stack") instanceof ItemStack stack)return stack;
        try {Object stack=menu.getClass().getMethod("getStackToRender").invoke(menu);return stack instanceof ItemStack s?s:ItemStack.EMPTY;}
        catch(NoSuchMethodException absent){return ItemStack.EMPTY;}
        catch(ReflectiveOperationException failure){throw new IllegalStateException("Create item owner",failure);}
    }
    static DesktopItemSources.Source itemSource(Player player,AbstractContainerMenu menu) {
        var stack=item(menu);if(stack.isEmpty())return null;
        int selected=player.getInventory().selected;
        int index=-1;
        if(ItemStack.isSameItemSameTags(player.getInventory().getItem(selected),stack))index=selected;
        else for(int i=0;i<player.getInventory().getContainerSize();i++)if(player.getInventory().getItem(i)==stack){index=i;break;}
        if(index<0)return null;
        var item=stack.getItem();int count=stack.getCount();
        return new DesktopItemSources.Source(index,"create-native:"+player.getUUID()+":"+index,stack,s->s.is(item)&&s.getCount()==count);
    }
    static String source(ServerPlayer player,AbstractContainerMenu menu,byte[] ignored) {
        Object holder=field(menu,"contentHolder");
        if(field(menu,"conductor") instanceof Entity conductor)return entityKey(player,conductor);
        if(holder instanceof BlockEntity be)return DesktopSessions.blockSourceKey(player,be.getBlockPos());
        if(field(holder,"this$0") instanceof Entity entity)return entityKey(player,entity);
        var item=itemSource(player,menu);return item==null?null:"automatic-item:"+player.getUUID()+":"+item.inventorySlot();
    }
    private static String entityKey(ServerPlayer player,Entity entity){return "entity:"+player.level().dimension().location()+":"+entity.getUUID();}
}
