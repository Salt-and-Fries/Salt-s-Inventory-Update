package example.companion.demo;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import example.companion.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.minecraft.world.inventory.ContainerInput;
import static net.minecraft.commands.Commands.literal;

/** Ordinary mod startup contains no Salt references. This exact jar also runs without Salt. */
@Mod("companion_example")
public final class CompanionDemoMod {
    private static final Map<UUID, ToolInventorySource[]> TOOLS = new HashMap<>();
    public CompanionDemoMod(IEventBus modBus) {
        modBus.addListener(CompanionDemoMod::menus);
        NeoForge.EVENT_BUS.addListener(CompanionDemoMod::commands);
        NeoForge.EVENT_BUS.addListener(CompanionDemoMod::serverAcceptance);
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent event) -> {
            var tools = TOOLS.remove(event.getEntity().getUUID());
            if (tools != null) for (var tool : tools) tool.invalidate();
        });
        if (FMLEnvironment.getDist() == Dist.CLIENT) {
            try { Class.forName("example.companion.demo.CompanionDemoClient").getMethod("initialize", IEventBus.class).invoke(null, modBus); }
            catch (ReflectiveOperationException failure) { throw new IllegalStateException(failure); }
        }
    }
    private static void serverAcceptance(ServerStartedEvent event) {
        if (!Boolean.getBoolean("companion.demo.serverQA")) return;
        var server = event.getServer();
        try {
            var player = FakePlayerFactory.getMinecraft(server.overworld());
            var source = tools(player)[0];
            var menu = new ToolInventoryMenu(100, player.getInventory(), source);
            if (net.neoforged.fml.ModList.get().isLoaded("salts_inventory_update")) {
                Class.forName("example.companion.demo.CompanionDedicatedAcceptance").getMethod("verify", ServerPlayer.class, ToolInventoryMenu.class).invoke(null, player, menu);
            }
            check(menu.stillValid(player) && menu.slots.get(0).container == source.inventory, "real backing inventory and source authorization");
            menu.clicked(0, 0, ContainerInput.PICKUP, player);
            check(menu.getCarried().getCount() == 16 && source.inventory.getItem(0).isEmpty(), "server pickup");
            menu.clicked(7, 0, ContainerInput.PICKUP, player);
            check(menu.getCarried().getCount() == 15 && source.inventory.getItem(7).getCount() == 1, "server slot capacity");
            menu.clicked(8, 0, ContainerInput.PICKUP, player);
            check(menu.getCarried().getCount() == 15 && source.inventory.getItem(8).isEmpty(), "server read-only slot");
            menu.clicked(1, 0, ContainerInput.PICKUP, player);
            menu.clicked(1, 0, ContainerInput.QUICK_MOVE, player);
            check(source.inventory.getItem(1).isEmpty() && inspect(player).playerCards() == 15, "server quick move");
            source.invalidate();
            check(!menu.stillValid(player) && !menu.slots.get(7).mayPickup(player), "invalid source revokes permission");
            menu.setCarried(ItemStack.EMPTY); menu.removed(player);
            check(source.inventory.getItem(7).getCount() == 1, "menu teardown retains the backing inventory");
            System.out.println("COMPANION DEDICATED COMPLETE");
        } catch (Exception failure) {
            System.out.println("COMPANION DEDICATED FAILED: " + failure); failure.printStackTrace();
        } finally { server.halt(false); }
    }
    private static void check(boolean condition, String invariant) {
        if (!condition) throw new IllegalStateException(invariant);
        System.out.println("COMPANION DEDICATED PASS: " + invariant);
    }
    private static void menus(RegisterEvent event) {
        if (!event.getRegistryKey().equals(Registries.MENU)) return;
        MenuType<CompanionMainMenu> main = new MenuType<>((id, inventory) ->
            new CompanionMainMenu(id, inventory, ToolInventorySource.clientReplica(new UUID(0, 0)), 1), FeatureFlags.DEFAULT_FLAGS);
        MenuType<ToolInventoryMenu> tool = new MenuType<>((id, inventory) ->
            new ToolInventoryMenu(id, inventory, ToolInventorySource.clientReplica(new UUID(0, 0))), FeatureFlags.DEFAULT_FLAGS);
        event.register(Registries.MENU, helper -> {
            helper.register(Identifier.parse("companion_example:main"), main);
            helper.register(Identifier.parse("companion_example:tool"), tool);
        });
        CompanionMenus.bind(main, tool);
    }
    private static void commands(RegisterCommandsEvent event) {
        event.getDispatcher().register(literal("companiondemo")
            .then(literal("main1").executes(context -> open(context.getSource().getPlayerOrException(), 1)))
            .then(literal("main2").executes(context -> open(context.getSource().getPlayerOrException(), 2)))
            .then(literal("other").executes(context -> open(context.getSource().getPlayerOrException(), 3)))
            .then(literal("tool1").executes(context -> open(context.getSource().getPlayerOrException(), 4)))
            .then(literal("tool2").executes(context -> open(context.getSource().getPlayerOrException(), 5)))
            .then(literal("invalidate").executes(context -> {
                tools(context.getSource().getPlayerOrException())[0].invalidate(); return 1;
            }))
            .then(literal("replace").executes(context -> {
                var player = context.getSource().getPlayerOrException();
                player.getInventory().setItem(0, new ItemStack(Items.STICK)); return 1;
            })));
    }
    public static int open(ServerPlayer player, int number) {
        var tools = tools(player);
        ToolInventorySource source = tools[number == 3 || number == 5 ? 1 : 0];
        if (number >= 4) {
            return player.openMenu(new SimpleMenuProvider((id, inventory, owner) -> new ToolInventoryMenu(id, inventory, source),
                Component.literal("Tool inventory"))).isPresent() ? 1 : 0;
        }
        return player.openMenu(new SimpleMenuProvider((id, inventory, owner) -> new CompanionMainMenu(id, inventory, source, number),
            Component.literal("Machine " + number))).isPresent() ? 1 : 0;
    }
    public record InventorySnapshot(int[] first, int[] second, int cursor, int playerCards) {
        public InventorySnapshot { first = first.clone(); second = second.clone(); }
        @Override public int[] first() { return first.clone(); }
        @Override public int[] second() { return second.clone(); }
        public int total() { return java.util.Arrays.stream(first).sum() + java.util.Arrays.stream(second).sum() + cursor + playerCards; }
    }
    /** Read on the server main thread; return detached counts to native acceptance. */
    public static InventorySnapshot inspect(ServerPlayer player) {
        var tools = tools(player);
        int[] first = new int[9], second = new int[9];
        for (int i = 0; i < 9; i++) { first[i] = tools[0].inventory.getItem(i).getCount(); second[i] = tools[1].inventory.getItem(i).getCount(); }
        int cards = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            var stack = player.getInventory().getItem(i);
            if (stack.is(Items.REDSTONE) || stack.is(Items.PAPER)) cards += stack.getCount();
        }
        return new InventorySnapshot(first, second, player.inventoryMenu.getCarried().getCount(), cards);
    }
    public static void invalidate(ServerPlayer player, int tool) { tools(player)[tool].invalidate(); }
    public static void replace(ServerPlayer player) { player.getInventory().setItem(0, new ItemStack(Items.STICK)); }
    public static void rebind(ServerPlayer player) {
        var tools = tools(player);
        tools[0] = new ToolInventorySource(UUID.randomUUID(), player, 0, tools[0].inventory);
    }
    private static ToolInventorySource[] tools(ServerPlayer player) {
        return TOOLS.computeIfAbsent(player.getUUID(), id -> {
            player.getInventory().setItem(0, new ItemStack(Items.STICK));
            player.getInventory().setItem(1, new ItemStack(Items.BLAZE_ROD));
            var one = new SimpleContainer(9); var two = new SimpleContainer(9);
            one.setItem(0, new ItemStack(Items.REDSTONE, 16));
            two.setItem(0, new ItemStack(Items.PAPER, 12));
            return new ToolInventorySource[]{new ToolInventorySource(UUID.randomUUID(), player, 0, one),
                new ToolInventorySource(UUID.randomUUID(), player, 1, two)};
        });
    }
}
