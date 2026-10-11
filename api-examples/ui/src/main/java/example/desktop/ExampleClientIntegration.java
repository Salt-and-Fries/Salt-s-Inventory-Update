package example.desktop;

import com.salts_inventory_update.api.integration.DesktopIntegration;
import com.salts_inventory_update.api.integration.DesktopIntegrationContext;
import com.salts_inventory_update.api.client.desktop.DesktopClientApi;
import com.salts_inventory_update.api.client.desktop.DesktopMenuFactories;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/** Client provider named as a string in metadata, never registered as a normal loader entrypoint. */
public final class ExampleClientIntegration implements DesktopIntegration {
    public static final Identifier PREFERENCES = Identifier.parse("developer_examples:preferences");
    public static final Identifier STYLED_PREFERENCES = Identifier.parse("developer_examples:styled_preferences");
    @Override public void register(DesktopIntegrationContext context) {
        DesktopClientApi.registerWindow(ExampleMenus.machine(), new SimpleMachineWindow());
        DesktopClientApi.registerWindow(ExampleMenus.terminal(), new TerminalWindow());
        DesktopMenuFactories.register(ExampleMenus.terminal(), (minecraft, player, id, data) -> {
            var buffer = new FriendlyByteBuf(Unpooled.wrappedBuffer(data));
            try {
                int seed = buffer.readVarInt();
                if (buffer.readableBytes() != 0) throw new IllegalArgumentException("Unexpected terminal opening data");
                return new TerminalMenu(id, player.getInventory(), seed);
            } finally { buffer.release(); }
        });
        DesktopClientApi.registerView(PREFERENCES, PreferencesModel.class, new ClientOnlyWindow());
        DesktopClientApi.registerView(STYLED_PREFERENCES, PreferencesModel.class, new StyledPreferencesWindow());
        ExampleUiHooks.openStyledPreferences = (key, model) -> DesktopClientApi.openView(STYLED_PREFERENCES, key, model,
            Component.literal("Styled preferences")).isPresent();
        ExampleUiHooks.openPreferences = (key, model) -> DesktopClientApi.openView(PREFERENCES, key, model,
            Component.literal("Preferences")).isPresent();
    }
}
