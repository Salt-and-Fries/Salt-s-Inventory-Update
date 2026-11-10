package com.salts_inventory_update.compat.nativewindows.mixin;

import com.salts_inventory_update.compat.tradingpost.TradingPostOfferState;
import net.minecraft.world.item.trading.MerchantOffer;
import org.spongepowered.asm.mixin.*;

@Mixin(MerchantOffer.class)
public abstract class TradingPostOfferStateMixin implements TradingPostOfferState {
    @Shadow private int uses;
    @Shadow private int specialPriceDiff;
    @Shadow private int demand;
    @Override public void salts$tradingPostState(MerchantOffer source){
        var tag=source.createTag();uses=source.getUses();specialPriceDiff=tag.getInt("specialPrice");demand=tag.getInt("demand");
    }
}
