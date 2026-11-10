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
            if(flowerForSlot==null) {
                flowerForSlot=Class.forName("vazkii.botania.common.item.FlowerPouchItem").getMethod("getFlowerForSlot",int.class);
                renderAlpha=Class.forName("vazkii.botania.client.core.helper.RenderHelper").getMethod("renderGuiItemAlpha",
                    ItemStack.class,int.class,int.class,int.class,ItemRenderer.class);
            }
            c.nativeGraphics().flush();
            for(var cell:((DesktopNativeContext<?,?>)c).nativeView().slots())if(!cell.player()&&cell.active()) {
                var slot=c.model().getSlot(cell.menuSlotId());
                if(slot.hasItem())continue;
                var flower=new ItemStack((Item)flowerForSlot.invoke(null,slot.getContainerSlot()));
                for(var place:layout.placements(cell.bounds())) {
                    var b=place.bounds();
                    renderAlpha.invoke(null,flower,c.contentX()+b.x()+1,c.contentY()+b.y()+1,95,c.minecraft().getItemRenderer());
                }
            }
        }catch(ReflectiveOperationException failure){throw new IllegalStateException("Botania flower guide renderer unavailable",failure);}
    }
}
