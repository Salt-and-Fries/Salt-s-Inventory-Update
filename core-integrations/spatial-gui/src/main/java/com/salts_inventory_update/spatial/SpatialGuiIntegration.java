package com.salts_inventory_update.spatial;

import com.salts_inventory_update.SaltsInventoryRuntime;
import com.salts_inventory_update.api.client.desktop.DesktopWorkspaceWindow;
import com.salts_inventory_update.client.InventoryDesktopScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;
import org.tastytrash.spatialGUI.client.SpatialGUIClient;
import org.tastytrash.spatialGUI.SpatialGUI;
import org.tastytrash.spatialGUI.util.RenderUtil;
import org.tastytrash.spatialGUI.util.MouseHandlerUtil;

/** Loaded only by the optional client mixins. No registration, packets or persistent config mutations. */
public final class SpatialGuiIntegration {
    public static boolean cameraPass;
    public static boolean mousePass;
    private SpatialGuiIntegration() { }

    public static InventoryDesktopScreen desktop() {
        var mc = Minecraft.getInstance();
        return mc.gui != null && mc.gui.screen() instanceof InventoryDesktopScreen d ? d : null;
    }
    public static boolean containerWorkspace(InventoryDesktopScreen d) {
        return d.workspaceAccess().windows().stream().anyMatch(w -> w.kind() == DesktopWorkspaceWindow.Kind.OTHER);
    }
    public static boolean eligible(Screen screen) {
        if (!(screen instanceof InventoryDesktopScreen d) || d.isHotbarOnly()
                || !SaltsInventoryRuntime.isEnabled() || Minecraft.getInstance().level == null) return false;
        var c = SpatialGUI.config;
        if (c == null || !SpatialGUIClient.isEnabled() || c.disabledScreens.contains(screen.getClass().getName())) return false;
        // Query the stored gameplay perspective, not Spatial GUI's temporary camera override.
        boolean first = ((SpatialOptionsAccess) Minecraft.getInstance().options).salts$perspective().isFirstPerson();
        if ((first && c.disableFirstPerson) || (!first && c.disableThirdPerson)) return false;
        return containerWorkspace(d) ? c.containers : c.inventory;
    }
    public static boolean active() {
        var d = desktop();
        var r = SpatialGUIClient.renderer();
        return d != null && eligible(d) && r != null && r.getHookedScreen() == d;
    }
    public static boolean movement() { var d = desktop(); return d != null && d.isCameraControlActive(); }
    public static boolean gameplay() { return movement() && active(); }
    public static boolean overPanel() {
        var r = SpatialGUIClient.renderer();
        if (r == null) return false;
        var inventory = r.getInventoryRenderer();
        var quad = inventory.getQuadBasis();
        if (quad == null) return false;
        var curve = inventory.getCylinderBasis();
        var point = curve != null
            ? RenderUtil.getInventoryMousePositionRayCurved(MouseHandlerUtil.getSourceX(), MouseHandlerUtil.getSourceY(), curve)
            : RenderUtil.getInventoryMousePositionRay(MouseHandlerUtil.getSourceX(), MouseHandlerUtil.getSourceY(), quad, r.getTargetManager().getInventoryTarget());
        // Published Spatial GUI 1.8 clamps misses to the texture edges. Reject those edge sentinels.
        var w = Minecraft.getInstance().getWindow();
        return point != null && point.x > 0 && point.y > 0 && point.x < w.getWidth() && point.y < w.getHeight();
    }
    public static boolean outsidePanel() { return active() && !movement() && !overPanel(); }
    public static void refresh() {
        var d = desktop();
        if (d == null) return;
        var r = SpatialGUIClient.renderer();
        if (eligible(d) && r != null && r.getHookedScreen() != d) r.hookScreen(d);
    }

    /** Aim through the displayed world, then enforce reach and visibility from the actual player. */
    public static HitResult pickWorld() {
        var mc = Minecraft.getInstance();
        var p = mc.player;
        var camera = mc.gameRenderer.mainCamera();
        Vec3 origin = camera.position();
        Vec3 direction = worldDirection(MouseHandlerUtil.getSourceX(), MouseHandlerUtil.getSourceY());
        Vec3 eye = p.getEyePosition();
        double reach = Math.max(p.blockInteractionRange(), p.entityInteractionRange());
        Vec3 end = origin.add(direction.scale(origin.distanceTo(eye) + reach));
        var block = mc.level.clip(new ClipContext(origin, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, p));
        double distance = block.getType() == HitResult.Type.MISS ? origin.distanceToSqr(end) : origin.distanceToSqr(block.getLocation());
        var entity = ProjectileUtil.getEntityHitResult(p, origin, end,
            new AABB(origin, end).inflate(1), e -> e != p && EntitySelector.CAN_BE_PICKED.test(e), distance);
        if (entity != null && reachable(entity.getLocation(), p.entityInteractionRange(), null)) return entity;
        if (block.getType() == HitResult.Type.BLOCK && reachable(block.getLocation(), p.blockInteractionRange(), block)) return block;
        return BlockHitResult.miss(end, net.minecraft.core.Direction.getApproximateNearest(direction), net.minecraft.core.BlockPos.containing(end));
    }
    /** Spatial GUI changes camera Euler angles after vanilla caches its near-plane vectors. */
    public static Vec3 worldDirection(double physicalX, double physicalY) {
        var mc = Minecraft.getInstance(); var camera = mc.gameRenderer.mainCamera();
        double yaw = Math.toRadians(camera.yRot()), pitch = Math.toRadians(camera.xRot());
        Vec3 forward = new Vec3(-Math.sin(yaw)*Math.cos(pitch), -Math.sin(pitch), Math.cos(yaw)*Math.cos(pitch));
        Vec3 right = new Vec3(-Math.cos(yaw), 0, -Math.sin(yaw));
        Vec3 up = right.cross(forward);
        double height = Math.max(1, mc.getWindow().getScreenHeight()), width = Math.max(1, mc.getWindow().getScreenWidth());
        double tangent = Math.tan(Math.toRadians(camera.getFov())/2);
        return forward.add(right.scale((physicalX/width*2-1)*tangent*width/height))
            .add(up.scale((1-physicalY/height*2)*tangent)).normalize();
    }
    private static boolean reachable(Vec3 point, double reach, BlockHitResult target) {
        var mc = Minecraft.getInstance(); var p = mc.player;
        if (p.getEyePosition().distanceToSqr(point) > reach * reach + 0.0001) return false;
        var obstruction = mc.level.clip(new ClipContext(p.getEyePosition(), point,
            ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, p));
        return obstruction.getType() == HitResult.Type.MISS || (target != null && obstruction.getBlockPos().equals(target.getBlockPos()));
    }
}
