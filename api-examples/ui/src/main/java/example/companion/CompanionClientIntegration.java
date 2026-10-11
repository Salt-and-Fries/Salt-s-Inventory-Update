package example.companion;

import com.salts_inventory_update.api.integration.*;
import com.salts_inventory_update.api.client.desktop.*;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;

/** Optional client provider. Ordinary mod startup never references this class. */
public final class CompanionClientIntegration implements DesktopIntegration {
    @Override public void register(DesktopIntegrationContext context) {
        DesktopClientApi.registerWindow(CompanionMenus.main(), new CompanionMainWindow());
        DesktopClientApi.registerWindow(CompanionMenus.tool(), new ToolInventoryWindow());
        DesktopMenuFactories.register(CompanionMenus.main(), (minecraft, player, id, data) -> {
            var buffer = new FriendlyByteBuf(Unpooled.wrappedBuffer(data));
            try {
                var source = ToolInventorySource.clientReplica(buffer.readUUID());
                int number = buffer.readVarInt();
                if (buffer.readableBytes() != 0) throw new IllegalArgumentException("Trailing main opening data");
                return new CompanionMainMenu(id, player.getInventory(), source, number);
            } finally { buffer.release(); }
        });
        DesktopMenuFactories.register(CompanionMenus.tool(), (minecraft, player, id, data) -> {
            var buffer = new FriendlyByteBuf(Unpooled.wrappedBuffer(data));
            try {
                var source = ToolInventorySource.clientReplica(buffer.readUUID());
                if (buffer.readableBytes() != 0) throw new IllegalArgumentException("Trailing tool opening data");
                return new ToolInventoryMenu(id, player.getInventory(), source);
            } finally { buffer.release(); }
        });
    }
}
