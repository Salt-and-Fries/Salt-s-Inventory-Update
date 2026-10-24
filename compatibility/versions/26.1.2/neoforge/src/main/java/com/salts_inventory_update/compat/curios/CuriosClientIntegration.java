package com.salts_inventory_update.compat.curios;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import io.netty.buffer.Unpooled;
import com.mojang.blaze3d.platform.InputConstants;
import com.salts_inventory_update.api.client.desktop.*;
import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.common.NeoForge;
import top.theillusivec4.curios.client.CuriosKeyMappings;

public final class CuriosClientIntegration {
    static final Identifier ART = Identifier.fromNamespaceAndPath("curios", "textures/gui/curios/inventory.png");
    private static final Map<UUID, DesktopWindowHandle> PANELS = new HashMap<>();
    private static final Map<UUID, Long> PENDING = new HashMap<>();
    private static int heldDesktopKey = -1;
    private CuriosClientIntegration() { }
    public static void initialize() {
        DesktopMenuFactories.register(CuriosIntegration.MENU.get(), (minecraft, player, id, data) -> {
            RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(data), player.registryAccess());
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
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, (ScreenEvent.KeyPressed.Post event) -> {
            var mc = Minecraft.getInstance();
            var workspace = DesktopClientApi.currentWorkspace().orElse(null);
            if (mc.player == null || !mc.isWindowActive() || !DesktopClientApi.isDesktopAvailable()
                || workspace == null || !workspace.isNativeScreenActive() || mc.screen != event.getScreen()
                || !CuriosKeyMappings.OPEN_CURIOS_INVENTORY.matches(event.getKeyEvent())) return;
            // Screen events include repeats. One physical press must toggle exactly once.
            if (heldDesktopKey != event.getKeyCode()) {
                heldDesktopKey = event.getKeyCode();
                toggle(workspace.ensureWindow(DesktopWorkspaceWindow.Kind.CHARACTER));
            }
            event.setCanceled(true);
        });
        NeoForge.EVENT_BUS.addListener((InputEvent.Key event) -> {
            if (event.getAction() == InputConstants.RELEASE && event.getKey() == heldDesktopKey) heldDesktopKey = -1;
            else if (event.getAction() == InputConstants.PRESS && Minecraft.getInstance().screen == null
                && CuriosKeyMappings.OPEN_CURIOS_INVENTORY.matches(event.getKeyEvent())) heldDesktopKey = event.getKey();
        });
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, (ClientTickEvent.Post event) -> {
            var mc = Minecraft.getInstance();
            if (mc.player == null || !mc.isWindowActive()) { heldDesktopKey = -1; return; }
            if (!DesktopClientApi.isDesktopAvailable() || mc.screen != null) return;
            var workspace = DesktopClientApi.currentWorkspace().orElse(null);
            if (!CuriosKeyMappings.OPEN_CURIOS_INVENTORY.consumeClick()) return;
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
        ClientPacketDistributor.sendToServer(new CuriosIntegration.Open(owner.instanceId()));
    }
    static void opened(UUID owner, DesktopWindowHandle panel) { PENDING.remove(owner); PANELS.put(owner, panel); }
    static void closed(UUID owner, DesktopWindowHandle panel) { PANELS.remove(owner, panel); }
}
