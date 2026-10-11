package example.companion;

import com.salts_inventory_update.api.desktop.DesktopInventoryIdentity;
import com.salts_inventory_update.api.integration.DesktopIntegration;
import com.salts_inventory_update.api.integration.DesktopIntegrationContext;
import com.salts_inventory_update.api.server.desktop.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleMenuProvider;

/** Optional common provider, loaded by descriptor only when Salt is installed. No client imports. */
public final class CompanionCommonIntegration implements DesktopIntegration {
    public static final class ParentState { private DesktopCompanionLease lease; }
    private static final java.util.List<DesktopCompanionLease> QA_LEASES = new java.util.ArrayList<>();
    /** Native fixture diagnostics; call only on the server main thread. */
    public static int openQaLeases() { return (int) QA_LEASES.stream().filter(DesktopCompanionLease::isOpen).count(); }
    @Override public void register(DesktopIntegrationContext context) {
        DesktopServerApi.registerWindow(CompanionMenus.main(), new DesktopServerWindowHandler<CompanionMainMenu, ParentState>() {
            @Override public ParentState createState(DesktopServerSessionContext<CompanionMainMenu, ParentState> parent) { return new ParentState(); }
            @Override public void opened(DesktopServerSessionContext<CompanionMainMenu, ParentState> parent) {
                acquire(parent);
            }
            @Override public void tick(DesktopServerSessionContext<CompanionMainMenu, ParentState> parent) {
                // Idempotent: an eligible new tool can appear later; the old captured source guard expires safely.
                acquire(parent);
            }
            @Override public void closed(DesktopServerSessionContext<CompanionMainMenu, ParentState> parent) {
                var lease = parent.state().lease;
                if (lease == null) return;
                if (Boolean.getBoolean("companion.demo.qa") && lease.isOpen()) throw new IllegalStateException("Parent retained an ownership after disposal");
                lease.close(); lease.close(); // Harmless even after manager-owned automatic release.
                if (Boolean.getBoolean("companion.demo.qa")) System.out.println("COMPANION SERVER PASS: automatic ownership release and repeated close");
            }
            private void acquire(DesktopServerSessionContext<CompanionMainMenu, ParentState> parent) {
                ToolInventorySource tool = parent.menu().tool;
                var acquired = parent.acquireCompanion(new DesktopCompanionSpec(
                    DesktopInventoryIdentity.reference(tool.inventory),
                    new SimpleMenuProvider((id, inventory, player) -> new ToolInventoryMenu(id, inventory, tool),
                        Component.literal("Network Tool upgrades")),
                    player -> tool.authorized(player)));
                acquired.ifPresent(lease -> {
                    var previous = parent.state().lease;
                    if (Boolean.getBoolean("companion.demo.qa") && previous != null && previous.isOpen() && previous != lease) {
                        throw new IllegalStateException("Repeated acquisition replaced the ownership reference");
                    }
                    if (lease != previous && Boolean.getBoolean("companion.demo.qa")) QA_LEASES.add(lease);
                    parent.state().lease = lease;
                });
                // Salt releases this parent's reference automatically on close, even if closed() throws.
            }
        });
        DesktopServerApi.registerWindow(CompanionMenus.tool(), new DesktopServerWindowHandler<ToolInventoryMenu, Void>() { });
        DesktopServerApi.registerOpeningData(CompanionMenus.main(), (player, menu, buffer) -> {
            buffer.writeUUID(menu.tool.id); buffer.writeVarInt(menu.machineNumber);
        });
        DesktopServerApi.registerOpeningData(CompanionMenus.tool(), (player, menu, buffer) -> buffer.writeUUID(menu.tool.id));
    }
}
