package com.salts_inventory_update.compat.curios;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import io.netty.buffer.Unpooled;
import com.mojang.blaze3d.platform.InputConstants;
import com.salts_inventory_update.api.client.desktop.*;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import top.theillusivec4.curios.client.KeyRegistry;

public final class CuriosClientIntegration {
    static final ResourceLocation ART = new ResourceLocation("curios", "textures/gui/inventory_revamp.png");
    static final ResourceLocation CONTROLS = new ResourceLocation("curios", "textures/gui/inventory.png");
    private static final Map<UUID, DesktopWindowHandle> PANELS = new HashMap<>();
    private static final Map<UUID, Long> PENDING = new HashMap<>();
    private static int heldDesktopKey = -1;
    private CuriosClientIntegration() { }
    public static void initialize() {
        DesktopMenuFactories.register(CuriosIntegration.MENU.get(), (minecraft, player, id, data) -> {
            FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.wrappedBuffer(data));
            try {
                var menu = new CuriosDesktopMenu(id, player.getInventory(), buffer);
                if (buffer.isReadable()) throw new IllegalArgumentException("Trailing Curios opening data");
                return menu;
            } finally { buffer.release(); }
        });
        DesktopClientApi.registerWindow(CuriosIntegration.MENU.get(), new CuriosPanelDefinition());
        DesktopClientApi.registerBuiltinAttachment(CuriosIntegration.id("curios_character_button"),
            DesktopWorkspaceWindow.Kind.CHARACTER, CuriosCharacterButton::new);
        // A GUI does not enqueue gameplay key-mapping clicks. Handle only keys the desktop
        // leaves unconsumed, so typing in search/name fields and modal input retain priority.
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, (ScreenEvent.KeyPressed.Post event) -> {
            var mc = Minecraft.getInstance();
            var workspace = DesktopClientApi.currentWorkspace().orElse(null);
            if (mc.player == null || !mc.isWindowActive() || !DesktopClientApi.isDesktopAvailable()
                || workspace == null || !workspace.isNativeScreenActive() || mc.screen != event.getScreen()
                || !KeyRegistry.openCurios.matches(event.getKeyCode(), event.getScanCode())) return;
            // Screen events include repeats. One physical press must toggle exactly once.
            if (heldDesktopKey != event.getKeyCode()) {
                heldDesktopKey = event.getKeyCode();
                toggle(workspace.ensureWindow(DesktopWorkspaceWindow.Kind.CHARACTER));
            }
            event.setCanceled(true);
        });
        MinecraftForge.EVENT_BUS.addListener((InputEvent.Key event) -> {
            if (event.getAction() == InputConstants.RELEASE && event.getKey() == heldDesktopKey) heldDesktopKey = -1;
            else if (event.getAction() == InputConstants.PRESS && Minecraft.getInstance().screen == null
                && KeyRegistry.openCurios.matches(event.getKey(), event.getScanCode())) heldDesktopKey = event.getKey();
        });
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST, (TickEvent.ClientTickEvent event) -> {
            if (event.phase != TickEvent.Phase.END) return;
            var mc = Minecraft.getInstance();
            if (mc.player == null || !mc.isWindowActive()) { heldDesktopKey = -1; return; }
            if (!DesktopClientApi.isDesktopAvailable() || mc.screen != null) return;
            var workspace = DesktopClientApi.currentWorkspace().orElse(null);
            if (!KeyRegistry.openCurios.consumeClick()) return;
            if (workspace == null) {
                DesktopClientApi.openPlayerWorkspace();
                workspace = DesktopClientApi.currentWorkspace().orElse(null);
            }
            if (workspace != null) toggle(workspace.ensureWindow(DesktopWorkspaceWindow.Kind.CHARACTER));
        });
    }
    static void toggle(DesktopWindowHandle owner) {
        var panel = PANELS.get(owner.instanceId());
        if (panel != null && panel.isOpen()) { panel.close(); return; }
        if (!owner.isOpen() || !DesktopClientApi.isDesktopAvailable()) return;
        long now = System.nanoTime();
        Long pending = PENDING.get(owner.instanceId());
        if (pending != null && now - pending < 2_000_000_000L) return;
        PENDING.put(owner.instanceId(), now);
        CuriosIntegration.network.sendToServer(new CuriosIntegration.Open(owner.instanceId()));
    }
    static void opened(UUID owner, DesktopWindowHandle panel) { PENDING.remove(owner); PANELS.put(owner, panel); }
    static void closed(UUID owner, DesktopWindowHandle panel) { PANELS.remove(owner, panel); }
}
