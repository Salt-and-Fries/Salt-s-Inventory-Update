package com.salts_inventory_update.compat.simplyswords;

/** Geometry of the reviewed 1.70.2 forge. Coordinates remain native, including the animation corridor. */
public final class SimplySwordsLayout {
    public static final int BODY_TOP=16, BODY_WIDTH=200, BODY_HEIGHT=58, VISIBLE_SLOTS=11;
    private SimplySwordsLayout() { }
    public static int slotX(int slot) {
        if(slot<0||slot>=VISIBLE_SLOTS)throw new IllegalArgumentException("Not a visible forge slot");
        return slot==0?92:slot==1?33:slot==2?151:29+(slot-3)*18;
    }
    public static int slotY(int slot) {
        if(slot<0||slot>=VISIBLE_SLOTS)throw new IllegalArgumentException("Not a visible forge slot");
        return slot<3?22:56;
    }
    /** Keep original gold tablet artwork while replacing only the lower inventory bag lip. ABGR native pixels. */
    public static boolean keepTabletPixel(int x,int abgr) {
        int red=abgr&255,green=abgr>>>8&255,blue=abgr>>>16&255;
        boolean cell=false;
        for(int i=0;i<8;i++)if(x>=29+18*i&&x<45+18*i){cell=true;break;}
        return cell&&red>110&&green>65&&red*10>blue*13;
    }
}
