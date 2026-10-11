package example.desktop;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import com.salts_inventory_update.api.integration.DesktopIntegration;
import com.salts_inventory_update.api.integration.DesktopIntegrationContext;
import com.salts_inventory_update.api.server.desktop.DesktopServerApi;
import com.salts_inventory_update.api.server.desktop.DesktopServerWindowHandler;
import net.minecraft.resources.Identifier;

/** Discovered only by Salt. Never referenced by the ordinary mod's entrypoints. */
public final class ExampleCommonIntegration implements DesktopIntegration {
    public static final Identifier SELECT = Identifier.parse("developer_examples:select");
    public static final Identifier RESULT = Identifier.parse("developer_examples:selection_result");
    @Override public void register(DesktopIntegrationContext context) {
        DesktopServerApi.registerWindow(ExampleMenus.machine(), new MachineTransferHandler());
        DesktopServerApi.registerWindow(ExampleMenus.terminal(), new DesktopServerWindowHandler<TerminalMenu, Void>() { });
        DesktopServerApi.registerOpeningData(ExampleMenus.terminal(), (player, menu, buffer) -> buffer.writeVarInt(menu.catalogSeed));
        DesktopServerApi.registerPayload(ExampleMenus.terminal(), SELECT, request -> {
            // Salt authenticated this session and checked menu validity. Enforce mod-specific rules too.
            byte[] data = request.data();
            if (data.length == 0 || data.length > 8 || !request.menu().stillValid(request.player())) { request.reject("Invalid selection request"); return; }
            String value = new String(data, StandardCharsets.UTF_8);
            if (!value.matches("[0-9]{1,5}")) { request.reject("Selection must be a catalog row id"); return; }
            int row = Integer.parseInt(value);
            if (row >= TerminalMenu.ROWS) { request.reject("Catalog row is absent"); return; }
            request.menu().selectedRow = row;
            request.sendToClient(RESULT, ByteBuffer.allocate(12).putInt(row).putLong(request.menu().count(row)).array());
            // A real withdrawal would validate permissions/counts against the mod's backing storage here.
            // Displayed icons never become client-authored stacks or real Slot instances.
            request.broadcastChanges();
        });
    }
}
