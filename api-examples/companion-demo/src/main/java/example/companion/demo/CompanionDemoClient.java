package example.companion.demo;

import example.companion.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.fml.ModList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;

/** Vanilla fallback screens use the exact same consumer textures and native GUI-pixel sizes. No Salt imports. */
public final class CompanionDemoClient {
    public static void initialize(IEventBus modBus) {
        if (Boolean.getBoolean("companion.demo.qa")) {
            if (ModList.get().isLoaded("salts_inventory_update")) {
                try { Class.forName("example.companion.demo.CompanionNativeAcceptance").getMethod("initialize").invoke(null); }
                catch (ReflectiveOperationException failure) { throw new IllegalStateException(failure); }
            }
            NeoForge.EVENT_BUS.addListener(CompanionDemoClient::qaTick);
        }
        modBus.addListener((RegisterMenuScreensEvent event) -> {
            event.<CompanionMainMenu, OriginalScreen<CompanionMainMenu>>register(CompanionMenus.main(), (menu, inventory, title) -> new OriginalScreen<>(menu, inventory, title,
                Identifier.parse("companion_example:textures/gui/machine.png"), 64));
            event.<ToolInventoryMenu, OriginalScreen<ToolInventoryMenu>>register(CompanionMenus.tool(), (menu, inventory, title) -> new OriginalScreen<>(menu, inventory, title,
                Identifier.parse("companion_example:textures/gui/tool.png"), 52));
        });
    }
    private static boolean worldStarted;
    private static int joinedTicks;
    private static int startupTicks;
    private static void qaTick(ClientTickEvent.Post event) {
        Minecraft client = Minecraft.getInstance();
        if (++startupTicks % 100 == 0 && client.player == null) {
            System.out.println("COMPANION NATIVE STARTUP: screen=" + (client.gui.screen() == null ? "none" : client.gui.screen().getClass().getSimpleName())
                + ", overlay=" + (client.gui.overlay() == null ? "none" : client.gui.overlay().getClass().getSimpleName()));
        }
        if (!worldStarted && client.gui.screen() instanceof TitleScreen && client.gui.overlay() == null) {
            worldStarted = true;
            client.createWorldOpenFlows().createFreshLevel("CompanionAPI6QA", new LevelSettings("Companion API6 QA",
                GameType.SURVIVAL, new LevelSettings.DifficultySettings(net.minecraft.world.Difficulty.PEACEFUL, false, false), true, WorldDataConfiguration.DEFAULT),
                new WorldOptions(42, false, false), WorldPresets::createTestWorldDimensions, client.gui.screen());
        }
        CompanionUiHooks.observer.frame();
        if (client.player == null || client.level == null || client.gui.overlay() != null) return;
        if (++joinedTicks == 100) open(client, 1);
        if (joinedTicks == 140 && ModList.get().isLoaded("salts_inventory_update")) open(client, 2);
        if (!ModList.get().isLoaded("salts_inventory_update") && joinedTicks == 160) {
            if (!(client.gui.screen() instanceof OriginalScreen<?>)) throw new IllegalStateException("Absent Salt did not retain the original screen");
            System.out.println("COMPANION ABSENT CLIENT PASS: normal original screen and inventory synchronization");
            net.minecraft.client.Screenshot.grab(client, false);
        }
        if (!ModList.get().isLoaded("salts_inventory_update") && joinedTicks == 190) {
            System.out.println("COMPANION ABSENT CLIENT COMPLETE"); client.stop();
        }
    }
    static void open(Minecraft client, int number) {
        var server = client.getSingleplayerServer();
        if (server == null) throw new IllegalStateException("This initial fixture run needs an integrated server");
        var id = client.player.getUUID();
        server.execute(() -> CompanionDemoMod.open(server.getPlayerList().getPlayer(id), number));
    }
    private static final class OriginalScreen<T extends AbstractContainerMenu> extends AbstractContainerScreen<T> {
        private final Identifier artwork;
        private final int artworkHeight;
        private OriginalScreen(T menu, Inventory inventory, Component title, Identifier artwork, int artworkHeight) {
            super(menu, inventory, title, 176, 148);
            this.artwork = artwork; this.artworkHeight = artworkHeight;
        }
        @Override public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float ticks) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, artwork, leftPos, topPos, 0, 0, 176, artworkHeight, 176, artworkHeight);
            graphics.fill(leftPos, topPos + 64, leftPos + 176, topPos + 148, 0xFFBCC8BF);
            super.extractContents(graphics, mouseX, mouseY, ticks);
        }
    }
}
