package com.salts_inventory_update.controlify.qa;

import appeng.api.config.Actionable;
import appeng.api.parts.PartHelper;
import appeng.api.stacks.AEItemKey;
import appeng.api.storage.MEStorage;
import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.me.common.RepoSlot;
import appeng.core.definitions.AEParts;
import appeng.me.helpers.BaseActionSource;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocators;
import appeng.menu.me.common.MEStorageMenu;
import appeng.parts.AEBasePart;
import appeng.server.testworld.Plot;
import com.salts_inventory_update.api.client.desktop.DesktopWindowBounds;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import java.util.List;

/** Genuine powered storage network in the disposable QA world; no client inventory state is fabricated. */
public final class QaAe2Network {
    private static BlockPos origin;
    private static MEStorage inventory;
    public static volatile boolean ready;
    public static void setup(ServerPlayer player) {
        ready=false;origin=player.blockPosition().offset(3,0,3);
        var plot=new Plot(Identifier.fromNamespaceAndPath("ae2","salts_controller_network_qa"));
        plot.block("[-1,3] -1 [-1,3]",Blocks.STONE);
        plot.creativeEnergyCell("0 0 1");
        plot.cable("0 0 0",AEParts.GLASS_CABLE);plot.part("0 0 0",Direction.NORTH,AEParts.TERMINAL);
        plot.drive(new BlockPos(1,0,1)).addItemCell64k().and().addItemCell64k().and().addItemCell64k();
        plot.afterGridInitAt(new BlockPos(1,0,1),(grid,node) -> {
            inventory=grid.getStorageService().getInventory();var source=new BaseActionSource();
            BuiltInRegistries.ITEM.stream().filter(item -> item!=Items.AIR && item!=Items.DIAMOND).limit(128)
                .forEach(item -> inventory.insert(AEItemKey.of(item),1,Actionable.MODULATE,source));
            inventory.insert(AEItemKey.of(Items.DIAMOND),128,Actionable.MODULATE,source);ready=true;
        });
        plot.build(player.level(),player,origin);
    }
    public static boolean clientReady() {
        return ready && Minecraft.getInstance().level!=null
            && PartHelper.getPart(Minecraft.getInstance().level,origin,Direction.NORTH)!=null;
    }
    public static void open(ServerPlayer player) {
        MenuOpener.open(MEStorageMenu.TYPE,player,MenuLocators.forPart((AEBasePart)PartHelper.getPart(player.level(),origin,Direction.NORTH)));
    }
    public static long diamonds() {return inventory.extract(AEItemKey.of(Items.DIAMOND),Long.MAX_VALUE,Actionable.SIMULATE,new BaseActionSource());}
    public static List<DesktopWindowBounds> entries(Object value) {
        var content=(AEBaseScreen<?>)value;
        return content.desktopSlots().stream().filter(slot -> slot instanceof RepoSlot && slot.hasItem() && content.desktopSlotVisible(slot))
            .map(slot -> new DesktopWindowBounds(content.getGuiLeft()+slot.x-1,content.getGuiTop()+slot.y-1,18,18)).toList();
    }
    public static DesktopWindowBounds diamondEntry(Object value) {
        var content=(AEBaseScreen<?>)value;
        return content.desktopSlots().stream().filter(slot -> slot instanceof RepoSlot && slot.getItem().is(Items.DIAMOND) && content.desktopSlotVisible(slot))
            .map(slot -> new DesktopWindowBounds(content.getGuiLeft()+slot.x-1,content.getGuiTop()+slot.y-1,18,18)).findFirst().orElse(null);
    }
    public static boolean filteredForDiamonds(Object value) {
        var content=(AEBaseScreen<?>)value;
        return diamondEntry(value)!=null && content.children().stream().anyMatch(widget -> widget instanceof net.minecraft.client.gui.components.EditBox text && text.getValue().equals("diamond"))
            && content.desktopSlots().stream().filter(slot -> slot instanceof RepoSlot && slot.hasItem() && content.desktopSlotVisible(slot))
                .allMatch(slot -> BuiltInRegistries.ITEM.getKey(slot.getItem().getItem()).getPath().contains("diamond"));
    }
    public static int scroll(Object value) {
        try {
            var field=appeng.client.gui.me.common.MEStorageScreen.class.getDeclaredField("scrollbar");field.setAccessible(true);
            return ((appeng.client.gui.widgets.Scrollbar)field.get(value)).getCurrentScroll();
        } catch(ReflectiveOperationException failure) {throw new IllegalStateException(failure);}
    }
}
