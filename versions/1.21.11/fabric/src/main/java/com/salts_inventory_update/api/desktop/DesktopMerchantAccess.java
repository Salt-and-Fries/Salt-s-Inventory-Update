package com.salts_inventory_update.api.desktop;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.trading.MerchantOffers;

/** Opts a native three-slot merchant into SIU's existing trading window and authenticated actions. */
public interface DesktopMerchantAccess {
    MerchantOffers desktopOffers();
    void desktopSetOffers(MerchantOffers offers);
    void desktopSelect(int index);
    void desktopMovePayment(int index);
    int desktopLevel();
    int desktopXp();
    int desktopFutureXp();
    boolean desktopShowProgress();
    boolean desktopCanRestock();
    void desktopSetLevel(int level);
    void desktopSetXp(int xp);
    void desktopSetProgress(boolean show);
    void desktopSetRestock(boolean restock);
    /** Null keeps the normal villager title/level heading. */
    default Component desktopHeading() { return null; }
    default int desktopHeadingColor() { return 0xFF404040; }
}
