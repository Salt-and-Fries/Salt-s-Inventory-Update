package com.salts_inventory_update.compat.nativewindows;

import com.salts_inventory_update.api.client.desktop.*;
import java.lang.reflect.Method;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Botania's alpha renderer bypasses GuiGraphics' capture pose. Reuse it on the
 * final API surface, preserving Botania's slot assignment and original alpha. */
final class ReviewedPouchFlowers {
    private static Method flowerForSlot,renderAlpha;
    private ReviewedPouchFlowers() { }
    static void render(DesktopRenderContext<AbstractContainerMenu,Void> c,DesktopNativeLayout layout) {
        try {
            boolean colored=((DesktopNativeContext<?,?>)c).nativeView().screenType().endsWith(".ColoredContentsPouchScreen");
            if(flowerForSlot==null&&!colored) {
                flowerForSlot=Class.forName("vazkii.botania.common.item.FlowerPouchItem").getMethod("getFlowerForSlot",int.class);
            }
            if(renderAlpha==null){
                renderAlpha=Class.forName("vazkii.botania.client.core.helper.RenderHelper").getMethod("renderGuiItemAlpha",
                    ItemStack.class,int.class,int.class,int.class,ItemRenderer.class);
            }
            c.nativeGraphics().flush();
            for(var cell:((DesktopNativeContext<?,?>)c).nativeView().slots())if(!cell.player()&&cell.active()) {
                var slot=c.model().getSlot(cell.menuSlotId());
                if(slot.hasItem())continue;
                Item item=colored?coloredItem(c.model(),cell.menuSlotId()):(Item)flowerForSlot.invoke(null,slot.getContainerSlot());
                if(item==null)continue;
                var flower=new ItemStack(item);
                for(var place:layout.placements(cell.bounds())) {
                    var b=place.bounds();
                    renderAlpha.invoke(null,flower,c.contentX()+b.x()+1,c.contentY()+b.y()+1,95,c.minecraft().getItemRenderer());
                }
            }
        }catch(ReflectiveOperationException failure){throw new IllegalStateException("Botania flower guide renderer unavailable",failure);}
    }
    @SuppressWarnings("unchecked")
    private static Item coloredItem(AbstractContainerMenu menu,int index)throws ReflectiveOperationException{
        Object pouch=ForeignMenuAccess.invoke(menu,"getPouch");
        var types=(java.util.List<net.minecraft.tags.TagKey<Item>>)Class.forName("vazkii.botania.common.item.ColoredContentsPouchItem")
            .getMethod("getStoredItemTypes",ItemStack.class).invoke(null,pouch);
        java.util.List<?> colors;
        try(var stream=(java.util.stream.Stream<?>)Class.forName("vazkii.botania.common.helper.ColorHelper").getMethod("supportedColors").invoke(null)){colors=stream.toList();}
        if(colors.isEmpty()||index<0||index/colors.size()>=types.size())return null;
        Object color=colors.get(index%colors.size());Class<?> colored=Class.forName("vazkii.botania.api.internal.Colored");
        var getColor=colored.getMethod("getColor");
        var candidates=net.minecraft.core.registries.BuiltInRegistries.ITEM.getTag(types.get(index/colors.size()));
        if(candidates.isEmpty())return null;
        for(var holder:candidates.get())if(colored.isInstance(holder.value())&&java.util.Objects.equals(getColor.invoke(holder.value()),color))return holder.value();
        return null;
    }
}
