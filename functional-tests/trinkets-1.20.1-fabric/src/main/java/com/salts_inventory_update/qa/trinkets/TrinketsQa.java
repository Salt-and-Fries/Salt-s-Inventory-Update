package com.salts_inventory_update.qa.trinkets;

import java.nio.file.*;
import java.util.*;
import com.google.gson.GsonBuilder;
import com.salts_inventory_update.api.desktop.DesktopPlayerMenuApi;
import com.salts_inventory_update.inventory.*;
import com.salts_inventory_update.mixin.accessor.AbstractContainerMenuAccessor;
import dev.emi.trinkets.TrinketPlayerScreenHandler;
import dev.emi.trinkets.api.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.commands.Commands;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

/** Disposable, opt-in native accessory fixture and server-side observations. */
public final class TrinketsQa implements ModInitializer {
    private static final Path OUTPUT = Path.of(System.getProperty("salts.trinkets.qa"));
    private static int ticks;
    private static void log(String event) {
        try { Files.createDirectories(OUTPUT); Files.writeString(OUTPUT.resolve("events.log"), event + "\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND); }
        catch (Exception failure) { throw new IllegalStateException(failure); }
    }
    @Override public void onInitialize() {
        for (Item item : List.of(Items.DIAMOND, Items.EMERALD, Items.GOLD_INGOT)) TrinketsApi.registerTrinket(item, new Trinket() {
            @Override public void onEquip(ItemStack stack, SlotReference ref, LivingEntity entity) {
                if (!entity.level().isClientSide) log("EQUIP " + stack + " " + ref.inventory().getSlotType().getGroup() + "/" + ref.inventory().getSlotType().getName() + "/" + ref.index());
            }
            @Override public void onUnequip(ItemStack stack, SlotReference ref, LivingEntity entity) {
                if (!entity.level().isClientSide) log("UNEQUIP " + stack + " " + ref.inventory().getSlotType().getGroup() + "/" + ref.inventory().getSlotType().getName() + "/" + ref.index());
            }
            @Override public boolean canUnequip(ItemStack stack, SlotReference ref, LivingEntity entity) { return !stack.is(Items.GOLD_INGOT); }
            @Override public void tick(ItemStack stack, SlotReference ref, LivingEntity entity) {
                if (!entity.level().isClientSide && entity.tickCount % 100 == 0) log("NATIVE_TICK " + stack + " " + ref.inventory().getSlotType().getGroup() + "/" + ref.inventory().getSlotType().getName());
            }
        });
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            handler.player.setGameMode(GameType.SURVIVAL);
            log("JOIN " + handler.player.getUUID());
        });
        CommandRegistrationCallback.EVENT.register((dispatcher, registries, env) -> dispatcher.register(
            Commands.literal("trinketsqa").requires(s -> s.hasPermission(2))
                .then(Commands.literal("seed").executes(c -> { seed(c.getSource().getPlayerOrException()); return 1; }))
                .then(Commands.literal("report").executes(c -> { report(c.getSource().getPlayerOrException()); return 1; }))
                .then(Commands.literal("rebuild").executes(c -> {
                    var p = c.getSource().getPlayerOrException();
                    for (int i = 0; i < 3; i++) ((TrinketPlayerScreenHandler) p.inventoryMenu).trinkets$updateTrinketSlots(true);
                    InventoryExpansion.syncToClient(p); p.inventoryMenu.broadcastFullState(); report(p); log("REBUILD_PASS"); return 1;
                }))
                .then(Commands.literal("grow").executes(c -> {
                    var p = c.getSource().getPlayerOrException();
                    var inv = TrinketsApi.getTrinketComponent(p).orElseThrow().getInventory().get("hand").get("ring");
                    inv.addModifier(new AttributeModifier(UUID.fromString("74c46498-6190-4092-8834-377aa0c58213"), "salts_trinkets_qa:grow", 1, AttributeModifier.Operation.ADDITION));
                    inv.markUpdate(); log("GROW_REQUESTED"); return 1;
                }))));
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (++ticks % 40 == 0) for (ServerPlayer player : server.getPlayerList().getPlayers()) report(player);
        });
    }
    private static void seed(ServerPlayer p) {
        p.getInventory().clearContent();
        p.inventoryMenu.setCarried(ItemStack.EMPTY);
        TrinketsApi.getTrinketComponent(p).orElseThrow().getInventory().values().forEach(g -> g.values().forEach(TrinketInventory::clearContent));
        p.getInventory().setItem(9, new ItemStack(Items.DIAMOND, 2));
        p.getInventory().setItem(10, new ItemStack(Items.EMERALD, 2));
        p.getInventory().setItem(11, new ItemStack(Items.IRON_INGOT, 4));
        p.getInventory().setItem(12, new ItemStack(Items.GOLD_INGOT));
        ((InventoryExpansionAccess) p).salts_inventory_update$setExtraSlotCount(2);
        InventoryExpansion.appendMissingMenuSlots(p.inventoryMenu, p); InventoryExpansion.syncToClient(p);
        p.inventoryMenu.broadcastFullState(); report(p); log("SEEDED");
    }
    private static void report(ServerPlayer p) {
        List<Map<String, Object>> slots = new ArrayList<>(); Set<String> keys = new HashSet<>(); int extras = 0;
        for (int i = 0; i < p.inventoryMenu.slots.size(); i++) {
            Slot slot = p.inventoryMenu.slots.get(i);
            if (slot.index != i) throw new AssertionError("Stale native slot index " + i + ":" + slot.index);
            String key = DesktopPlayerMenuApi.slotKey(slot);
            if (!key.isEmpty() && !keys.add(key)) throw new AssertionError("Duplicate slot identity " + key);
            if (InventoryExpansion.isExtraSlot(slot)) extras++;
            slots.add(Map.of("id", i, "key", key, "item", slot.getItem().toString()));
        }
        var access = (AbstractContainerMenuAccessor) p.inventoryMenu;
        if (access.salts_inventory_update$getLastSlots().size() != slots.size()
                || access.salts_inventory_update$getRemoteSlots().size() != slots.size()) throw new AssertionError("Native tracker size mismatch");
        if (extras != ((InventoryExpansionAccess) p).salts_inventory_update$getExtraSlotCount()) throw new AssertionError("Expanded slots lost");
        try {
            Files.createDirectories(OUTPUT);
            Files.writeString(OUTPUT.resolve("server-state.json"), new GsonBuilder().setPrettyPrinting().create().toJson(
                Map.of("slots", slots, "extras", extras, "cursor", p.inventoryMenu.getCarried().toString(), "gameMode", p.gameMode.getGameModeForPlayer().getName())));
        } catch (Exception failure) { throw new IllegalStateException(failure); }
    }
}
