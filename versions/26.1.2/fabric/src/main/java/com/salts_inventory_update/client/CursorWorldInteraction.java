package com.salts_inventory_update.client;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import com.salts_inventory_update.mixin.client.MinecraftInvoker;

public final class CursorWorldInteraction {
    private CursorWorldInteraction() {
    }

    public static boolean startAttackAtCursor(Minecraft minecraft, double mouseX, double mouseY) {
        updateHitResultAtCursor(minecraft, mouseX, mouseY);
        return ((MinecraftInvoker) minecraft).salts_inventory_update$startAttack();
    }

    public static void continueAttackAtCursor(Minecraft minecraft, double mouseX, double mouseY) {
        updateHitResultAtCursor(minecraft, mouseX, mouseY);
        ((MinecraftInvoker) minecraft).salts_inventory_update$continueAttack(true);
    }

    public static boolean startAttackAtCrosshair(Minecraft minecraft) {
        return ((MinecraftInvoker) minecraft).salts_inventory_update$startAttack();
    }

    public static void continueAttackAtCrosshair(Minecraft minecraft) {
        ((MinecraftInvoker) minecraft).salts_inventory_update$continueAttack(true);
    }

    public static void stopAttack(Minecraft minecraft) {
        ((MinecraftInvoker) minecraft).salts_inventory_update$continueAttack(false);
    }

    public static void useAtCursor(Minecraft minecraft, double mouseX, double mouseY) {
        updateHitResultAtCursor(minecraft, mouseX, mouseY);
        ((MinecraftInvoker) minecraft).salts_inventory_update$startUseItem();
    }

    public static void useAtCrosshair(Minecraft minecraft) {
        ((MinecraftInvoker) minecraft).salts_inventory_update$startUseItem();
    }

    public static void updateHitResultAtCursor(Minecraft minecraft, double mouseX, double mouseY) {
        HitResult hitResult = pickAtCursor(minecraft, mouseX, mouseY);
        minecraft.hitResult = hitResult;
        minecraft.crosshairPickEntity = hitResult instanceof EntityHitResult entityHit ? entityHit.getEntity() : null;
    }

    private static HitResult pickAtCursor(Minecraft minecraft, double mouseX, double mouseY) {
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null) {
            return minecraft.hitResult;
        }

        Entity cameraEntity = minecraft.getCameraEntity();
        if (cameraEntity == null) {
            cameraEntity = player;
        }

        Camera camera = minecraft.gameRenderer.getMainCamera();
        Vec3 origin = camera.position();
        Vec3 direction = cursorDirection(minecraft, camera, mouseX, mouseY);
        double blockRange = player.blockInteractionRange();
        double entityRange = player.entityInteractionRange();
        double maxRange = Math.max(blockRange, entityRange);
        Vec3 blockEnd = origin.add(direction.scale(blockRange));
        BlockHitResult blockHit = minecraft.level.clip(new ClipContext(
            origin,
            blockEnd,
            ClipContext.Block.OUTLINE,
            ClipContext.Fluid.NONE,
            cameraEntity
        ));

        double closestDistance = blockHit.getLocation().distanceToSqr(origin);
        if (blockHit.getType() == HitResult.Type.MISS) {
            closestDistance = maxRange * maxRange;
        }

        Vec3 entityEnd = origin.add(direction.scale(maxRange));
        AABB searchBox = cameraEntity.getBoundingBox().expandTowards(direction.scale(maxRange)).inflate(1.0D);
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
            cameraEntity,
            origin,
            entityEnd,
            searchBox,
            EntitySelector.CAN_BE_PICKED,
            closestDistance
        );

        if (entityHit != null && entityHit.getLocation().closerThan(origin, entityRange)) {
            return entityHit;
        }

        return blockHit;
    }

    private static Vec3 cursorDirection(Minecraft minecraft, Camera camera, double mouseX, double mouseY) {
        int width = minecraft.getWindow().getGuiScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();
        float x = width <= 0 ? 0.0F : (float) (mouseX / width * 2.0D - 1.0D);
        float y = height <= 0 ? 0.0F : (float) (1.0D - mouseY / height * 2.0D);
        float fov = minecraft.options.fov().get();
        return camera.getNearPlane(fov).getPointOnPlane(x, y).normalize();
    }
}
