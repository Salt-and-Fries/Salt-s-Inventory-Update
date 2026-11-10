package com.salts_inventory_update.compat.aether;
import com.salts_inventory_update.api.client.desktop.DesktopWindowBounds;
/** Native Aether upper inventory, in the existing 176 by 82 Character content. */
public final class AetherCharacterLayout {
    private AetherCharacterLayout() { }
    public static DesktopWindowBounds playerSlot(int id) {
        if(id>=5&&id<=8)return new DesktopWindowBounds(59,8+(id-5)*18,16,16);
        if(id>=1&&id<=4)return new DesktopWindowBounds(116+((id-1)%2)*18,18+((id-1)/2)*18,16,16);
        if(id==0)return new DesktopWindowBounds(154,28,16,16);
        if(id==45)return new DesktopWindowBounds(116,62,16,16);
        return null;
    }
    public static DesktopWindowBounds equipment(int x,int y){return new DesktopWindowBounds(77+x,8+y,16,16);}
}
