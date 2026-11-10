package com.salts_inventory_update.compat.ssn;

import com.salts_inventory_update.api.client.desktop.*;
import java.util.*;

/** Approved native working rectangles; the player inventory is always separate. */
public final class SsnLayouts {
    private SsnLayouts() { }
    public static final String BASE="com.lothrazar.storagenetwork.";
    public enum Kind {
        INVENTORY("inventory","block.inventory.ContainerNetworkInventory","block.inventory.ScreenNetworkInventory",4,4,174,172,0,true,false),
        REQUEST("request","block.request.ContainerNetworkCraftingTable","block.request.ScreenNetworkTable",4,4,174,166,10,true,false),
        EXPANDED("request_expanded","block.expand.ContainerNetworkInventoryExpanded","block.expand.ScreenNetworkInventoryExpanded",4,4,466,449,10,true,false),
        REMOTE("inventory_remote","item.remote.ContainerNetworkRemote","item.remote.ScreenNetworkRemote",4,4,174,172,0,true,true),
        CRAFTING_REMOTE("crafting_remote","item.remote.ContainerNetworkCraftingRemote","item.remote.ScreenNetworkCraftingRemote",4,4,174,166,10,true,true),
        EXPANDED_REMOTE("expanded_remote","item.remote.ContainerNetworkExpandedRemote","item.remote.ScreenNetworkExpandedRemote",4,4,466,449,10,true,true),
        IMPORT("import_filter_kabel","block.cable.inputfilter.ContainerCableImportFilter","block.cable.inputfilter.ScreenCableImportFilter",4,4,172,82,4,false,false),
        EXPORT("export_kabel","block.cable.export.ContainerCableExportFilter","block.cable.export.ScreenCableExportFilter",4,4,172,82,4,false,false),
        LINK("filter_kabel","block.cable.linkfilter.ContainerCableFilter","block.cable.linkfilter.ScreenCableFilter",4,4,172,72,0,false,false),
        COLLECTION("collector","block.collection.ContainerCollectionFilter","block.collection.ScreenCollectionFilter",7,31,170,71,0,false,false),
        PROCESS("process_kabel","block.cable.processing.ContainerCableProcess","block.cable.processing.ScreenCableProcess",4,4,172,112,0,false,false),
        CRADLE("cradle","block.cradle.ContainerCradle","block.cradle.ScreenCradle",60,16,114,70,9,false,false),
        RECEIVER("receiver","block.receiver.ContainerNetworkReceiver","block.receiver.ScreenNetworkReceiver",7,17,169,69,1,false,false);
        public final String path,menu,screen;public final DesktopWindowBounds body;public final int slots;public final boolean network,remote;
        Kind(String path,String menu,String screen,int x,int y,int right,int bottom,int slots,boolean network,boolean remote){
            this.path=path;this.menu=BASE+menu;this.screen=BASE+screen;body=new DesktopWindowBounds(x,y,right-x,bottom-y);this.slots=slots;this.network=network;this.remote=remote;
        }
        public boolean crafting(){return network&&slots==10;}
        public boolean expanded(){return this==EXPANDED||this==EXPANDED_REMOTE;}
        public String titleKey(){return (remote?"item.":"block.")+"storagenetwork."+path;}
    }
    public static int width(Kind kind,int titleWidth){return Math.max(kind.body.width()+14,titleWidth+49);}
    public static DesktopNativeLayout layout(Kind kind,int titleWidth){
        int width=width(kind,titleWidth),x=(width-14-kind.body.width())/2;
        var b=DesktopNativeLayout.builder().canvas(width-14,kind.body.height()).scrollable(true)
            .keep("native-working-panel",kind.body,x,0);
        if(kind.network&&!kind.expanded()){
            // The narrow autofocus toggle occupies the original right frame.
            // Keep it complete while excluding the redundant dark frame column.
            int y=kind.crafting()?94:158;
            b.hide(new DesktopWindowBounds(173,kind.body.y(),1,kind.body.height()));
            b.move("autofocus-toggle",new DesktopWindowBounds(168,y,6,11),x+164,y-kind.body.y());
        }
        return b.build();
    }
}
