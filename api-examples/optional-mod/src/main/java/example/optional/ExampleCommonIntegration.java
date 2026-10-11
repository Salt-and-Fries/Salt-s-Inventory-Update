package example.optional;

import com.salts_inventory_update.api.integration.DesktopIntegration;
import com.salts_inventory_update.api.integration.DesktopIntegrationContext;

public final class ExampleCommonIntegration implements DesktopIntegration {
    @Override
    public void register(DesktopIntegrationContext context) {
        System.setProperty("salts.example.common", context.modId() + ":" + context.phase());
    }
}
