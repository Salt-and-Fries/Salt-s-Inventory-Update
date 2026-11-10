package com.salts_inventory_update.compat.nativewindows.mixin;

import com.salts_inventory_update.api.desktop.DesktopMerchantAccess;
import net.minecraft.world.item.trading.MerchantOffers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Pseudo;

/** Explicit bridges keep the public API names stable across loader remapping. */
@Pseudo
@Mixin(targets="net.mehvahdjukaar.supplementaries.common.inventories.RedMerchantMenu",remap=false)
public abstract class RedMerchantAccessMixin implements DesktopMerchantAccess {
    @Shadow public abstract MerchantOffers getOffers();
    @Shadow public abstract void setOffers(MerchantOffers offers);
    @Shadow public abstract void setSelectionHint(int index);
    @Shadow public abstract void tryMoveItems(int index);
    @Shadow public abstract int getTraderLevel();
    @Shadow public abstract int getTraderXp();
    @Shadow public abstract int getFutureTraderXp();
    @Shadow public abstract boolean showProgressBar();
    @Shadow public abstract boolean canRestock();
    @Shadow public abstract void setMerchantLevel(int level);
    @Shadow public abstract void setXp(int xp);
    @Shadow public abstract void setShowProgressBar(boolean show);
    @Shadow public abstract void setCanRestock(boolean restock);
    public MerchantOffers desktopOffers(){return getOffers();}
    public void desktopSetOffers(MerchantOffers offers){setOffers(offers);}
    public void desktopSelect(int index){setSelectionHint(index);}
    public void desktopMovePayment(int index){tryMoveItems(index);}
    public int desktopLevel(){return getTraderLevel();}
    public int desktopXp(){return getTraderXp();}
    public int desktopFutureXp(){return getFutureTraderXp();}
    public boolean desktopShowProgress(){return showProgressBar();}
    public boolean desktopCanRestock(){return canRestock();}
    public void desktopSetLevel(int level){setMerchantLevel(level);}
    public void desktopSetXp(int xp){setXp(xp);}
    public void desktopSetProgress(boolean show){setShowProgressBar(show);}
    public void desktopSetRestock(boolean restock){setCanRestock(restock);}
    @Override public net.minecraft.network.chat.Component desktopHeading(){return net.minecraft.network.chat.Component.literal("Trade offer");}
    @Override public int desktopHeadingColor(){return 0xFFB52922;}
}
