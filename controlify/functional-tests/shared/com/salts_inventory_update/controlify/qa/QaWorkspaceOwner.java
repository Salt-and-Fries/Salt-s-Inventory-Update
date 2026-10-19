package com.salts_inventory_update.controlify.qa;

import com.salts_inventory_update.api.client.desktop.DesktopWorkspace;
import com.salts_inventory_update.api.client.desktop.DesktopWorkspaceWindow;
import com.salts_inventory_update.controlify.SaltsControlifyClient;
import com.salts_inventory_update.controlify.config.ControlifyConfig;
import com.salts_inventory_update.controlify.model.RadialWorkspace;
import com.salts_inventory_update.controlify.model.WorkspaceLayout;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;

/** QA-only observations of existing package-private state; no production test hooks. */
public final class QaWorkspaceOwner {
    private final SaltsControlifyClient value;
    public QaWorkspaceOwner(SaltsControlifyClient value) { this.value = value; }
    public DesktopWorkspace desktop() { return invoke("desktop"); }
    public Map<UUID, DesktopWorkspaceWindow> windows() { return invoke("windows"); }
    public RadialWorkspace<UUID> model() { return invoke("model"); }
    public ControlifyConfig config() { return invoke("config"); }
    public WorkspaceLayout<UUID> animatedLayout(int width, int height) {
        return invoke("animatedLayout", new Class<?>[]{int.class, int.class}, width, height);
    }
    public boolean workspaceActive() { return value.workspaceActive(); }
    public boolean radialActive() { return value.radialActive(); }
    public boolean tuckedAway() { return value.tuckedAway(); }
    public long closeHoldDurationMillis() { return value.closeHoldDurationMillis(); }
    private <T> T invoke(String name) { return invoke(name, new Class<?>[0]); }
    @SuppressWarnings("unchecked")
    private <T> T invoke(String name, Class<?>[] parameterTypes, Object... arguments) {
        try {
            Method method = SaltsControlifyClient.class.getDeclaredMethod(name, parameterTypes);
            method.setAccessible(true);
            return (T) method.invoke(value, arguments);
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException("Cannot observe companion method " + name, failure);
        }
    }
}
