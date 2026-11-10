package com.salts_inventory_update.compat.enchanted;

import com.salts_inventory_update.api.client.desktop.*;
import com.salts_inventory_update.api.desktop.DesktopMenuBindings;
import com.salts_inventory_update.api.integration.DesktopEnvironment;
import com.salts_inventory_update.api.server.desktop.DesktopServerApi;
import com.salts_inventory_update.compat.nativewindows.NativeMenuSupport;
import io.netty.buffer.Unpooled;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Reviewed Enchanted 4.x artwork; upstream menus retain their slot and recipe logic. */
public final class EnchantedWindows {
    enum Kind {
        OVEN("witch_oven", "WitchOvenMenu", 150, 90, box(50,15,75,56)),
        WHEEL("spinning_wheel", "SpinningWheelMenu", 164, 80, box(27,20,122,45)),
        DISTILLERY("distillery", "DistilleryMenu", 148, 111, box(29,5,117,77)),
        SHELF("poppet_shelf", "PoppetShelfMenu", 136, 58, box(45,16,86,21)),
        ALTAR("altar", "AltarMenu", 160, 64, box(20,32,136,24));

        final String path, menu;
        final int width, height;
        final DesktopWindowBounds body;
        Kind(String path,String menu,int width,int height,DesktopWindowBounds body) {
            this.path=path;this.menu="net.favouriteless.enchanted.common.menus."+menu;
            this.width=width;this.height=height;this.body=body;
        }
    }
    private static final Set<Kind> BOUND=new HashSet<>(),CLIENT=new HashSet<>();
    private static final DesktopInsets INSETS=new DesktopInsets(7,26,7,8);
    private EnchantedWindows() { }

    public static void initializeCommon() { bind();DesktopEnvironment.onServerStarting(server->bind()); }
    public static void initializeClient() { install();DesktopClientApi.onClientStarted(mc->{bind();install();}); }

    private static synchronized void bind() {
        for(var kind:Kind.values()) {
            var type=NativeMenuSupport.type("enchanted:"+kind.path);
            if(type==null||!BOUND.add(kind))continue;
            NativeMenuSupport.support(type,kind.menu,true);
            // Processing slots have distinct ingredient, jar, fuel and result roles.
            DesktopMenuBindings.registerStorage(type,(menu,slot)->kind==Kind.SHELF&&!(slot.container instanceof Inventory));
            if(DesktopServerApi.findOpeningDataWriter(type)==null)
                DesktopServerApi.registerOpeningData(type,(player,menu,data)->data.writeBlockPos(ownerPosition(kind,menu)));
        }
    }

    private static BlockPos ownerPosition(Kind kind,AbstractContainerMenu menu) {
        try {
            if(kind==Kind.SHELF||kind==Kind.ALTAR) {
                // 4.x keeps a ContainerLevelAccess rather than the old public
                // altar owner / shelf-inventory position accessor.
                var field=menu.getClass().getDeclaredField(kind==Kind.SHELF?"containerAccess":"canInteractWithCallable");
                field.setAccessible(true);
                return ((ContainerLevelAccess)field.get(menu)).evaluate((level,pos)->pos).orElseThrow();
            }
            var owner=(BlockEntity)menu.getClass().getMethod("getBlockEntity").invoke(menu);
            return owner.getBlockPos();
        }catch(ReflectiveOperationException failure) { throw new IllegalStateException("Enchanted menu owner changed: "+kind.path,failure); }
    }

    private static synchronized void install() {
        for(var kind:Kind.values()) {
            var type=NativeMenuSupport.type("enchanted:"+kind.path);
            if(type==null||!CLIENT.add(kind))continue;
            // Enchanted 4.x constructors take BlockPos directly on both loaders.
            DesktopMenuFactories.register(type,(mc,player,id,data)->{
                if(data.length!=8)throw new IllegalArgumentException("Invalid Enchanted opening data");
                var buffer=new FriendlyByteBuf(Unpooled.wrappedBuffer(data));
                try {
                    return (AbstractContainerMenu)Class.forName(kind.menu)
                        .getConstructor(int.class,Inventory.class,BlockPos.class)
                        .newInstance(id,player.getInventory(),buffer.readBlockPos());
                }catch(ReflectiveOperationException failure) { throw new IllegalStateException("Enchanted menu reconstruction failed",failure); }
                finally { buffer.release(); }
            });
            DesktopClientApi.registerNativeMatcher(ResourceLocation.fromNamespaceAndPath("salts_inventory_compat","enchanted/"+kind.path),100,
                Set.of(type),lookup->true,lookup->new Template(kind));
        }
    }

    static DesktopNativeLayout layout(Kind kind,int titleWidth) {
        return layout(kind,titleWidth,50);
    }
    static DesktopNativeLayout layout(Kind kind,int titleWidth,int inventoryTitleWidth) {
        int width=Math.max(kind.width,titleWidth+46),contentWidth=width-INSETS.horizontal();
        int x=(contentWidth-kind.body.width())/2;
        int y=kind==Kind.ALTAR?4:0;
        var region=new DesktopNativeRegion("native-"+kind.path,kind.body,x,y);
        if(kind==Kind.DISTILLERY) {
            // Native outputs start at y=7, above the old title's baseline.
            // Keep their entire column, removing only the original title band.
            for(var clean:labels(titleWidth,inventoryTitleWidth))region=region.cut(clean);
        }
        return DesktopNativeLayout.builder().canvas(contentWidth,kind.height-INSETS.vertical())
            .region(region).scrollable(false).build();
    }

    private static List<DesktopWindowBounds> labels(int titleWidth,int inventoryTitleWidth) {
        return List.of(box((176-titleWidth)/2,6,titleWidth,9),box(8,72,inventoryTitleWidth,9));
    }

    private record Template(Kind kind) implements DesktopNativeTemplate<AbstractContainerMenu,Void> {
        @Override public DesktopNativeLayout nativeLayout(DesktopNativeContext<AbstractContainerMenu,Void> c) {
            return layout(kind,c.fontWidth(c.originalTitle()),c.fontWidth(Component.translatable("container.inventory")));
        }
        @Override public DesktopWindowSurface surface(DesktopWindowContext<AbstractContainerMenu,Void> c) {
            return new DesktopWindowSurface(DesktopSurfaceMode.SALT,INSETS,true,List.of(),DesktopViewportPolicy.CLAMP);
        }
        @Override public void renderNativeUnderlay(DesktopRenderContext<AbstractContainerMenu,Void> c) {
            if(kind!=Kind.DISTILLERY)return;
            // Restore the native pixels behind the removed old title, including
            // resource-pack artwork, without repainting the output slots.
            int textWidth=c.fontWidth(c.originalTitle());
            int width=Math.max(kind.width,textWidth+46)-INSETS.horizontal();
            for(var label:labels(textWidth,c.fontWidth(Component.translatable("container.inventory")))) {
                int sx=Math.max(label.x(),kind.body.x()),sy=Math.max(label.y(),kind.body.y());
                int right=Math.min(label.x()+label.width(),kind.body.x()+kind.body.width());
                int bottom=Math.min(label.y()+label.height(),kind.body.y()+kind.body.height());
                if(right<=sx||bottom<=sy)continue;
                int dx=(width-kind.body.width())/2+sx-kind.body.x();
                c.texture(ResourceLocation.fromNamespaceAndPath("enchanted","textures/gui/menus/distillery.png"),
                    c.contentX()+dx,c.contentY()+sy-kind.body.y(),sx,sy,right-sx,bottom-sy,right-sx,bottom-sy,256,256);
            }
        }
        @Override public String layoutRevision(DesktopWindowContext<AbstractContainerMenu,Void> c) { return "enchanted-reviewed-modern-1:"+kind.path; }
    }

    private static DesktopWindowBounds box(int x,int y,int w,int h) { return new DesktopWindowBounds(x,y,w,h); }
}
