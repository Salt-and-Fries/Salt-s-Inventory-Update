package com.salts_inventory_update.compat.nativewindows.mixin;

import com.salts_inventory_update.api.desktop.DesktopMerchantAccess;
import com.salts_inventory_update.compat.nativewindows.ForeignMenuAccess;
import com.salts_inventory_update.compat.tradingpost.TradingPostOfferState;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.trading.*;
import org.spongepowered.asm.mixin.*;

/** Core's existing merchant updates must preserve TradingPostOffers and its native owner map. */
@Pseudo
@Mixin(targets="fuzs.tradingpost.common.world.inventory.TradingPostMenu",remap=false)
public abstract class TradingPostMenuMixin implements DesktopMerchantAccess {
    @Override public void desktopSetOffers(MerchantOffers incoming){
        // ArrayList's iterator visits the unfiltered backing list; get(index)
        // on TradingPostOffers deliberately maps filtered display indices.
        var iterator=((MerchantMenu)(Object)this).getOffers().iterator();int index=0;
        while(iterator.hasNext()&&index<incoming.size())((TradingPostOfferState)iterator.next()).salts$tradingPostState(incoming.get(index++));
    }
    @Override public void desktopSetXp(int xp){
        ((Merchant)ForeignMenuAccess.invoke(this,"getTraders")).overrideXp(xp);
    }
    // The native merchant-data packets own these per-villager immutable flags.
    @Override public void desktopSetLevel(int level){ }
    @Override public void desktopSetProgress(boolean show){ }
    @Override public void desktopSetRestock(boolean restock){ }
}
