package com.salts_inventory_update.compat.nativewindows;

import com.google.gson.Gson;
import com.salts_inventory_update.api.client.desktop.*;
import com.salts_inventory_update.api.integration.DesktopEnvironment;
import com.salts_inventory_update.api.server.desktop.*;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;

/** Reviewed live compositions. Original menus, renderers and server actions remain authoritative. */
public final class ReviewedNativeWindows {
    private static final String CATALOG = "/salts_inventory_compat/native-windows/project-everything.json";
    private static final Set<String> REGISTERED = new HashSet<>();
    private static final Set<String> NATIVE_ONLY = Set.of("exposure:album", "exposure:lectern_album",
        "waystones:waystone_selection", "waystones:sharestone_selection",
        "waystones:warp_scroll_selection", "waystones:warp_stone_selection",
        "waystones:portstone_selection", "waystones:admin_selection",
        "waystones:inventory_selection", "waystones:waystone");
    private static Profile[] profiles;
    private ReviewedNativeWindows() { }

    public static void initializeCommon() {
        EasyAnvilsActions.initialize();
        ExposureRenameActions.initialize();
        GalleryItemSources.initialize();
        SpawnInventoryBridge.initializeCommon();
        ReviewedEntityInventories.initializeCommon();
        SupplementariesTrading.initializeCommon();
        MowzieInventories.initializeCommon();
        MowzieTradeActions.initialize();
        SmallShipsActions.initialize();
        DesktopEnvironment.onServerStarting(server -> {
            var nativeTypes = new HashSet<>(NATIVE_ONLY);
            // Polymer's MicroUi presents category/action icons in chest cells;
            // it is a native browser, not a physical storage container.
            if (DesktopEnvironment.isModLoaded("polymer-core"))
                for (int rows = 1; rows <= 6; rows++) nativeTypes.add("minecraft:generic_9x" + rows);
            for (String id : nativeTypes) {
                var type = NativeMenuSupport.type(id);
                if (type != null && DesktopServerApi.findWindowHandler(type) == null)
                    DesktopServerApi.registerWindow(type, new DesktopServerWindowHandler<AbstractContainerMenu,Void>() {
                        @Override public boolean supports(net.minecraft.server.level.ServerPlayer player, AbstractContainerMenu menu) {
                            var key = BuiltInRegistries.MENU.getKey(menu.getType());
                            return key != null && !NATIVE_ONLY.contains(key.toString())
                                && !menu.getClass().getName().equals("eu.pb4.polymer.core.impl.ui.MicroUi$InternalScreenHandler");
                        }
                    });
            }
        });
    }
    public static void initializeClient() {
        EasyAnvilsActions.install();
        ExposureRenameActions.install();
        DesktopClientApi.onClientStarted(client->{EasyAnvilsActions.install();ExposureRenameActions.install();});
        SpawnInventoryBridge.initializeClient();
        ReviewedEntityInventories.initializeClient();
        SupplementariesTrading.initializeClient();
        MowzieInventories.initializeClient();
        // Safe uses the vanilla chest screen with a custom menu type. Match
        // the exact menu, not Minecraft's mapping-dependent screen class name.
        NativeWindowPolish.install("supplementaries","supplementaries:safe",NativeWindowPolish::slots);
        // Visual Workbench owns a separate menu type even though its screen is
        // vanilla crafting. Give it an explicit composition rather than bars.
        NativeWindowPolish.install("visualworkbench","visualworkbench:crafting",NativeBatchLayouts::visualWorkbench);
        NativeWindowPolish.install("sophisticatedstorage","sophisticatedstorage:decoration_table",NativeMachineLayouts::decorationTable);
        NativeWindowPolish.install("niftycarts","niftycarts:seed_drill",NativeWindowPolish::slots);
        for(String menu:List.of("chest_four_rows","chest_eight_rows","chest_quad"))
            NativeWindowPolish.install("niftycarts","niftycarts:"+menu,NativeWindowPolish::slots);
        NativeWindowPolish.install("waystones","waystones:waystone_modifiers",NativeWindowPolish::waystoneModifiers);
        install();
        DesktopClientApi.onClientStarted(client -> install());
    }
    private static synchronized Profile[] catalog() {
        if (profiles == null) {
            try (var stream = ReviewedNativeWindows.class.getResourceAsStream(CATALOG)) {
                if (stream == null) throw new IllegalStateException("Missing reviewed native-window catalog");
                profiles = new Gson().fromJson(new InputStreamReader(stream, StandardCharsets.UTF_8), Profile[].class);
            } catch (java.io.IOException failure) { throw new IllegalStateException(failure); }
        }
        return profiles;
    }
    private static synchronized void install() {
        var selected = Arrays.stream(catalog()).filter(p -> Arrays.stream(p.mods).anyMatch(DesktopEnvironment::isModLoaded)).toList();
        for (MenuType<?> type : BuiltInRegistries.MENU) {
            var id = BuiltInRegistries.MENU.getKey(type);
            // Includes native vanilla-shaped inventories such as the Safe, which
            // need opening bytes but deliberately keep SIU's existing layout.
            if(id!=null&&!NATIVE_ONLY.contains(id.toString())&&!id.toString().equals("supplementaries:red_merchant")
                &&DesktopEnvironment.isModLoaded("moonlight"))MoonlightMenuFactories.register(type);
            if (id == null || REGISTERED.contains(id.toString()) || NATIVE_ONLY.contains(id.toString()) || id.toString().equals("supplementaries:red_merchant")) continue;
            var matching = selected.stream().filter(p -> accepts(p,id)).toList();
            if (matching.isEmpty()) continue;
            // Existing direct definitions (including AE2's authenticated native host) win.
            DesktopEnvironment.registerIntegration(matching.get(0).mods[0], () -> {
                DesktopClientApi.registerNativeMatcher(new ResourceLocation("salts_inventory_compat", "reviewed/"+id.getNamespace()+"/"+id.getPath()),
                    -100, Set.of(type), lookup -> true, lookup -> new Template(matching));
            });
            REGISTERED.add(id.toString());
        }
    }

    static final class Region { int[] source, at; int[][] cutouts; }
    static boolean accepts(Profile profile,ResourceLocation id) {
        return id.toString().equals(profile.menu)
            || profile.menus!=null&&Arrays.asList(profile.menus).contains(id.toString());
    }
    static final class Profile {
        String id, screen, menu, title;
        String[] mods, menus, screens;
        Region[] regions;
        int[] canvas, color;
        int[][] clean;
        boolean recipeBook, machineUtility, playerSlots, darkFrame;
    }
    static DesktopWindowBounds box(int[] r) { return new DesktopWindowBounds(r[0],r[1],r[2]-r[0],r[3]-r[1]); }
    static boolean covers(DesktopNativeLayout layout, DesktopWindowBounds bounds) { return !layout.placements(bounds).isEmpty(); }

    static DesktopNativeLayout compose(Profile p, DesktopNativeView view) {
        if(p.id.equals("bf-warp-plate")&&(view.screenType().endsWith(".WaystoneContainerScreen")||view.screenType().endsWith(".WarpPlateScreen")))return NativeWindowPolish.waystoneModifiers(view);
        if("net.satisfy.vinery.client.gui.FermentationBarrelGui".equals(p.screen)) {
            var fermentation=fermentationLayout(view);
            if(fermentation!=null)return fermentation;
        }
        if(p.screen!=null&&p.screen.equals("com.talhanation.smallships.client.gui.screens.inventory.ShipContainerScreen"))return shipLayout(view);
        if(p.screen!=null&&p.screen.endsWith(".VariableSizeContainerScreen"))return NativeWindowPolish.slots(view);
        var b = DesktopNativeLayout.builder().canvas(p.canvas[0],p.canvas[1]);
        for (int i=0;i<p.regions.length;i++) {
            var r=p.regions[i];var region=new DesktopNativeRegion("body/"+i,box(r.source),r.at[0],r.at[1]);
            if(r.cutouts!=null)for(int[] hole:r.cutouts)region=region.cut(box(hole));
            b.region(region);
        }
        // The native guide is render-only, so widget discovery cannot retain it.
        // Its hover coordinates still go through the ordinary native region map.
        if(p.id.equals("minecraft-anvil")&&view.screenType().contains("easyanvils"))
            b.keep("formatting-guide",new DesktopWindowBounds(155,6,15,12),115,0);
        for (int[] hole:p.clean) b.hide(box(hole));
        // The native options strip can contain real menu slots as well as
        // buttons. Map it before finding unplaced slots so each stays here.
        if(p.machineUtility)b.exterior("machine-options",
            new DesktopWindowBounds(0,-34,view.imageBounds().width(),34),0,-36);
        var base=b.build();
        // Newly visible mount cargo/upgrade sockets are real state, never disposable whitespace.
        var missing=view.slots().stream().filter(s->!s.player()&&s.active()&&!covers(base,s.bounds())).toList();
        if (!missing.isEmpty()) {
            var bounds=DesktopNativeView.union(missing.stream().map(DesktopNativeView.Slot::bounds).toList()).orElseThrow();
            // A changed native geometry must remain usable, rather than painting half a cell.
            for(var slot:missing)b.hide(slot.bounds());
            b.keep("additional-owned-slots",bounds,base.width()+6,0);
        }
        var result=b.build();
        // Native controls outside a body chunk (search, capacity, machine configuration) retain
        // a single complete paint and hit region. Invisible widgets do not reserve empty columns.
        int extraY=0, extraX=result.width()+6;
        for(var w:view.widgets()) {
            if(!w.visible()||covers(result,w.bounds()))continue;
            var r=w.bounds();
            var image=view.imageBounds();
            if(!DesktopNativeRegion.intersects(r,new DesktopWindowBounds(-40,-40,image.width()+80,image.height()+80)))continue;
            // Never reintroduce an unrelated player-inventory sorting widget.
            int playerTop=view.slots().stream().filter(DesktopNativeView.Slot::player).mapToInt(s->s.bounds().y()).min().orElse(Integer.MAX_VALUE);
            if(r.y()>=playerTop&& !p.id.contains("wands"))continue;
            boolean partial=result.regions().stream().anyMatch(a->DesktopNativeRegion.intersects(a.source(),r));
            if(partial)b.hide(r);
            if(p.screen!=null&&p.screen.endsWith(".GuiUmvuthiTrade") && r.width()>100) {
                // After the first payment the native UI replaces the input row
                // with a wider Replenish button. Keep it in that row rather
                // than adding an empty extra column beside the portrait.
                b.keep("native-replenish",r,64,47);result=b.build();continue;
            }
            if(p.machineUtility&&r.y()<0) {
                b.exterior("utility/"+w.id(),r,6+extraY,-r.height());extraY+=r.width()+2;
            } else {
                b.keep("widget/"+w.id(),r,extraX,extraY);extraY+=r.height()+3;
            }
            result=b.build();
        }
        // DoAPI's live recipe drawer is painted by the source screen rather than a child widget.
        if(p.recipeBook&&view.automaticBounds().x()<0) {
            var a=view.automaticBounds();int width=-a.x();
            b.exterior("recipe-drawer",new DesktopWindowBounds(a.x(),Math.min(0,a.y()),width,
                Math.max(view.imageBounds().height(),a.height())), -width-3,20);
        }
        result=b.build();
        for(var slot:view.slots())if(!slot.player()&&slot.active()&&result.placements(slot.bounds()).size()!=1)
            return NativeWindowPolish.machine(view);
        return result;
    }

    /** Vinery's later 1.4.x release keeps four bottle outputs underneath its pipes.
     * The same class in the older release paints a two-by-two aging barrel. */
    static DesktopNativeLayout fermentationLayout(DesktopNativeView view) {
        var cells=view.slots().stream().filter(s->!s.player()&&s.active()).map(DesktopNativeView.Slot::bounds).toList();
        if(cells.size()!=6)return null;
        int bottom=cells.stream().mapToInt(DesktopWindowBounds::y).max().orElseThrow();
        if(cells.stream().filter(b->b.y()==bottom).count()!=4)return null;
        var bounds=DesktopNativeView.union(cells).orElseThrow();
        var artwork=new DesktopWindowBounds(bounds.x()-2,bounds.y()-2,bounds.width()+4,bounds.height()+4);
        var layout=DesktopNativeLayout.builder().keep("fermentation-machine",artwork,0,0);
        int playerTop=view.slots().stream().filter(DesktopNativeView.Slot::player).mapToInt(s->s.bounds().y()).min().orElse(Integer.MAX_VALUE);
        if(playerTop!=Integer.MAX_VALUE) {
            int bottlesLeft=cells.stream().filter(b->b.y()==bottom).mapToInt(DesktopWindowBounds::x).min().orElseThrow();
            // The lower bottle row shares native Y coordinates with the
            // Inventory label at its left. Keep bottles, omit that label.
            layout.hide(new DesktopWindowBounds(0,playerTop-12,bottlesLeft,12));
        }
        return layout.build();
    }

    static DesktopNativeLayout shipLayout(DesktopNativeView view) {
        var cargo=DesktopNativeView.union(view.slots().stream().filter(s->!s.player()&&s.active()).map(DesktopNativeView.Slot::bounds).toList());
        if(cargo.isEmpty())return NativeWindowPolish.machine(view);
        var grid=cargo.get();
        var b=DesktopNativeLayout.builder().keep("cargo",grid,0,0)
            .keep("statistics",new DesktopWindowBounds(177,17,79,108),grid.width()+6,0);
        int pagerY=grid.y()+grid.height();
        b.keep("pager",new DesktopWindowBounds(114,pagerY,56,12),Math.max(0,grid.width()-56),grid.height()+4);
        return b.build();
    }

    private static final class Template implements DesktopNativeTemplate<AbstractContainerMenu,Void> {
        private final List<Profile> candidates;
        Template(List<Profile> candidates) { this.candidates=candidates; }
        private Profile profile(DesktopNativeView view) {
            return candidates.stream().filter(p->(p.screen.equals(view.screenType())||p.screens!=null&&Arrays.asList(p.screens).contains(view.screenType()))).findFirst().orElse(null);
        }
        @Override public DesktopWindowSurface surface(DesktopWindowContext<AbstractContainerMenu,Void> c) {
            return new DesktopWindowSurface(DesktopSurfaceMode.SALT,new DesktopInsets(6,20,6,6),true,List.of(),DesktopViewportPolicy.CLAMP);
        }
        @Override public DesktopWindowDecoration<AbstractContainerMenu,Void> decoration(DesktopWindowContext<AbstractContainerMenu,Void> c) {
            var p=profile(((DesktopNativeContext<?,?>)c).nativeView());
            return p!=null&&p.darkFrame?(render,controls)->ReviewedDarkChrome.paint(render,controls,title(c),p.color):null;
        }
        @Override public DesktopNativeLayout nativeLayout(DesktopNativeContext<AbstractContainerMenu,Void> c) {
            var p=profile(c.nativeView());
            var layout=p==null?NativeWindowPolish.machine(c.nativeView()):compose(p,c.nativeView());
            var centered=NativeBatchLayouts.center(layout,c.fontWidth(title(c))+30);
            if(p!=null&&p.machineUtility)return new DesktopNativeLayout(centered.regions().stream()
                .map(r->r.id().equals("machine-options")?r.at(centered.width()+12-r.source().width(),r.y()):r).toList(),
                centered.padding(),centered.minimumWidth(),centered.minimumHeight(),centered.scrollable());
            return centered;
        }
        @Override public void renderNativeUnderlay(DesktopRenderContext<AbstractContainerMenu,Void> c) {
            var p=profile(((DesktopNativeContext<?,?>)c).nativeView());
            if(p!=null)c.fill(c.contentX(),c.contentY(),c.contentX()+c.contentWidth(),c.contentY()+c.contentHeight(),
                0xFF000000|(p.color[0]<<16)|(p.color[1]<<8)|p.color[2]);
        }
        @Override public void render(DesktopRenderContext<AbstractContainerMenu,Void> c) {
            var context=(DesktopNativeContext<AbstractContainerMenu,Void>)c;
            var p=profile(context.nativeView());
            if(p==null)return;
            if(p.id.equals("item-botania-flower-bag"))ReviewedPouchFlowers.render(c,nativeLayout(context));
            if(p.id.equals("exposure-film-rename")) {
                // The original display slot sits on an external tab. Keep the
                // real slot/input mapping, but present its item without that tab.
                var layout=nativeLayout(context);
                for(var slot:context.nativeView().slots())if(!slot.player())for(var place:layout.placements(slot.bounds())) {
                    var b=place.bounds();int x=c.contentX()+b.x(),y=c.contentY()+b.y();
                    c.fill(x,y,x+b.width(),y+b.height(),0xFFC6C6C6);
                    c.texturelessSlot(slot.menuSlotId(),x+1,y+1);
                }
            }
        }
        @Override public String layoutRevision(DesktopWindowContext<AbstractContainerMenu,Void> c) { return "project-everything-56-3"; }
        @Override public net.minecraft.network.chat.Component title(DesktopWindowContext<AbstractContainerMenu,Void> c) {
            var p=profile(((DesktopNativeContext<?,?>)c).nativeView());
            return p!=null&&c.originalTitle().getString().isBlank()?net.minecraft.network.chat.Component.literal(p.title):c.originalTitle();
        }
        @Override public boolean retainPlayerSlot(DesktopNativeContext<AbstractContainerMenu,Void> c,int id) {
            var p=profile(c.nativeView());return p!=null&&p.playerSlots;
        }
        @Override public void closed(DesktopWindowContext<AbstractContainerMenu,Void> c){MowzieTradeActions.forget(c.model());GallerySelectionActions.forget(c.model());}
        @Override public boolean mouseClicked(DesktopInputContext<AbstractContainerMenu,Void> c,
                com.salts_inventory_update.api.client.input.MouseButtonEvent e,boolean twice) {
            GallerySelectionActions.remember(c);
            var p=profile(((DesktopNativeContext<?,?>)c).nativeView());
            if(p!=null&&(p.screen.endsWith(".GuiSculptorTrade")||p.screen.endsWith(".GuiUmvuthiTrade")))return MowzieTradeActions.clicked(c,e);
            return p!=null&&p.screen.equals("com.talhanation.smallships.client.gui.screens.inventory.ShipContainerScreen")&&SmallShipsActions.clicked(c,e);
        }
    }
}
