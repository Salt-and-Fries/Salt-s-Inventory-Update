package com.salts_inventory_update.compat.nativewindows;

import com.salts_inventory_update.api.client.desktop.DesktopInputContext;
import java.nio.charset.StandardCharsets;
import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.world.inventory.AbstractContainerMenu;

/** Keep the native selection callback; address its packet to the owning window. */
public final class GallerySelectionActions {
    private static final Map<AbstractContainerMenu,DesktopInputContext<AbstractContainerMenu,Void>> SENDERS=new IdentityHashMap<>();
    private GallerySelectionActions() { }
    public static void remember(DesktopInputContext<AbstractContainerMenu,Void> context) {
        if(context.model() instanceof GalleryItemSources.Owner)SENDERS.put(context.model(),context);
    }
    public static void forget(AbstractContainerMenu menu){SENDERS.remove(menu);}
    public static boolean send(AbstractContainerMenu menu) {
        var context=SENDERS.get(menu);if(context==null)return false;
        // A retired or temporarily unavailable hosted session must not fall through
        // to a packet addressed to another active native menu.
        if(context.windowHandle().isOpen()) {
            var selected=ForeignMenuAccess.invoke(menu,"getSelectedPainting");
            if(selected instanceof Holder<?> holder)holder.unwrapKey().ifPresent(key->
                context.sendPayload(GalleryItemSources.SELECTION,key.location().toString().getBytes(StandardCharsets.UTF_8)));
        }
        return true;
    }
}
