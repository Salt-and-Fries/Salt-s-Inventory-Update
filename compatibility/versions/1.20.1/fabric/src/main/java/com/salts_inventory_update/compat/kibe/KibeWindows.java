package com.salts_inventory_update.compat.kibe;

import com.salts_inventory_update.api.desktop.DesktopMenuBindings;
import com.salts_inventory_update.api.desktop.DesktopItemSources;
import com.salts_inventory_update.api.integration.DesktopEnvironment;
import com.salts_inventory_update.compat.nativewindows.NativeMenuSupport;
import java.util.*;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import com.salts_inventory_update.api.server.desktop.DesktopSessions;

/** Kibe retains its actual ExtendedScreenHandlerFactory opening data and menus.
 * SIU's native transport already carries those bytes and tracks held sources.
 * Explicit client templates replace only the visual composition, not that path. */
public final class KibeWindows {
    public enum Kind {
        COOLER("cooler",100,56,79,17,18,18,Theme.COOLER),
        HELD_COOLER("cooler_item",100,56,79,17,18,18,Theme.COOLER),
        BIG_TORCH("big_torch",176,72,7,18,162,36,Theme.GRAY),
        GENERATOR("generator",176,108,7,17,162,72,Theme.GRAY),
        BREAKER("breaker",112,90,61,17,54,54,Theme.GRAY),
        PLACER("placer",112,90,61,17,54,54,Theme.GRAY),
        DRAWBRIDGE("drawbridge",142,58,79,17,72,18,Theme.DRAWBRIDGE),
        WITHER_BUILDER("wither_builder",132,90,61,17,54,54,Theme.GRAY),
        VACUUM("vacuum_hopper",148,90,43,17,90,54,Theme.TEAL),
        CHEST("entangled_chest",176,108,7,17,162,54,Theme.TEAL),
        BAG("entangled_bag",176,108,7,17,162,54,Theme.TEAL),
        TRASH("trash_can",128,58,79,35,18,18,Theme.TRASH),
        POCKET_TRASH("pocket_trash_can",144,58,79,35,18,18,Theme.TRASH);

        final String path;
        final int width,height,sx,sy,bw,bh;
        final Theme theme;
        Kind(String path,int width,int height,int sx,int sy,int bw,int bh,Theme theme) {
            this.path=path;this.width=width;this.height=height;this.sx=sx;this.sy=sy;this.bw=bw;this.bh=bh;this.theme=theme;
        }
        boolean runes(){return this==CHEST||this==BAG;}
        List<String> paths(){
            if(this!=GENERATOR)return List.of(path);
            var paths=new ArrayList<String>();
            for(String block:List.of("cobblestone","basalt"))for(int tier=1;tier<=5;tier++)paths.add(block+"_generator_mk"+tier);
            return List.copyOf(paths);
        }
    }
    enum Theme { GRAY,COOLER,TEAL,DRAWBRIDGE,TRASH }
    private static final Set<String> BOUND=new HashSet<>();
    static java.util.function.BooleanSupplier clientCoolerOpen=()->false;
    private KibeWindows() { }
    public static void initializeCommon(){bind();DesktopEnvironment.onServerStarting(server->bind());}
    /** Upstream pauses all carried Coolers while its item menu is open.
     * A detached session must keep the same guard on both logical sides. */
    public static boolean coolerOpen(Player player){
        return player.level().isClientSide?clientCoolerOpen.getAsBoolean():DesktopSessions.hasOpenSessionMatching(player,
            menu->menu.getClass().getName().equals("io.github.lucaargolo.kibe.items.cooler.CoolerBlockItemScreenHandler"));
    }
    static synchronized void bind(){
        for(var kind:Kind.values())for(String path:kind.paths()){
            var type=NativeMenuSupport.type("kibe:"+path);
            if(type==null||!BOUND.add(path))continue;
            if(kind==Kind.HELD_COOLER)DesktopItemSources.register(type,(player,menu)->{
                try{
                    var hand=(InteractionHand)menu.getClass().getMethod("getHand").invoke(menu);
                    int slot=hand==InteractionHand.OFF_HAND?40:player.getInventory().selected;
                    var backing=player.getInventory().getItem(slot);
                    if(backing.isEmpty())return null;
                    var item=backing.getItem();
                    // Kibe rewrites food NBT through its own menu. Protect the
                    // held source without treating those valid writes as a
                    // different item or allowing a cross-window move of it.
                    return new DesktopItemSources.Source(slot,"kibe:cooler:"+player.getUUID()+":"+slot,
                        backing,stack->stack.is(item));
                }catch(ReflectiveOperationException failure){throw new IllegalStateException("Kibe Cooler hand",failure);}
            });
            // Keep disposal, cover, ingredients and XP conversion out of storage
            // sorting. Only the homogeneous storage groups can be reordered.
            DesktopMenuBindings.registerStorage(type,(menu,slot)->{
                if(slot.container instanceof Inventory)return false;
                return switch(kind){
                    case CHEST,BAG,GENERATOR,BREAKER,PLACER->true;
                    case VACUUM->{int index=menu.slots.indexOf(slot);yield index>=2&&index<11;}
                    default->false;
                };
            });
        }
    }
}
