package com.salts_inventory_update.compat.emi;

import java.lang.reflect.Constructor;

import com.salts_inventory_update.compat.CompatibilityLog;
import com.salts_inventory_update.api.client.recipebrowser.RecipeBrowserAccess;
import com.salts_inventory_update.api.client.recipebrowser.RecipeBrowserBridge;
import com.salts_inventory_update.api.client.recipebrowser.RecipeBrowserSource;
import com.salts_inventory_update.api.integration.DesktopEnvironment;

public final class EmiRecipeBrowserBootstrap {
    private static final String EMI_MOD_ID = "emi";
    private static final String ACCESS_CLASS =
        "com.salts_inventory_update.compat.emi.RuntimeEmiRecipeBrowserAccess";
    private static boolean initialized;

    private EmiRecipeBrowserBootstrap() {
    }

    public static synchronized void initialize() {
        if (initialized || !DesktopEnvironment.isModLoaded(EMI_MOD_ID)) {
            return;
        }
        initialized = true;

        try {
            Class<?> accessClass = Class.forName(ACCESS_CLASS);
            Constructor<?> constructor = accessClass.getDeclaredConstructor();
            constructor.setAccessible(true);
            Object candidate = constructor.newInstance();
            if (candidate instanceof RecipeBrowserAccess access) {
                RecipeBrowserBridge.install(com.salts_inventory_update.compat.recipebrowser.RecipeBrowserProviders.EMI, access);
            }
        } catch (ReflectiveOperationException | LinkageError exception) {
            CompatibilityLog.LOGGER.warn("Unable to initialize EMI recipe-browser support.", exception);
        }
    }
}
