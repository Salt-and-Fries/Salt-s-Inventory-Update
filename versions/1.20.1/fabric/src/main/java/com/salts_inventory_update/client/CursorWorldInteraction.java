package com.salts_inventory_update.client;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
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
        return startAttack(minecraft);
    }

    public static void continueAttackAtCursor(Minecraft minecraft, double mouseX, double mouseY) {
        updateHitResultAtCursor(minecraft, mouseX, mouseY);
        continueAttack(minecraft);
    }

    public static boolean startAttackAtCrosshair(Minecraft minecraft) {
        return startAttack(minecraft);
    }

    public static void continueAttackAtCrosshair(Minecraft minecraft) {
        continueAttack(minecraft);
    }

    public static void stopAttack(Minecraft minecraft) {
        if (minecraft.gameMode != null) {
            minecraft.gameMode.stopDestroyBlock();
        }
    }

    public static void useAtCursor(Minecraft minecraft, double mouseX, double mouseY) {
        updateHitResultAtCursor(minecraft, mouseX, mouseY);
        ((MinecraftInvoker) minecraft).salts_inventory_update$startUseItem();
    }

    public static void continueUseAtCursor(Minecraft minecraft, double mouseX, double mouseY) {
        updateHitResultAtCursor(minecraft, mouseX, mouseY);
        if (shouldRepeatUse(minecraft)) {
            ((MinecraftInvoker) minecraft).salts_inventory_update$startUseItem();
        }
    }

    public static void useAtCrosshair(Minecraft minecraft) {
        ((MinecraftInvoker) minecraft).salts_inventory_update$startUseItem();
    }

    public static void continueUseAtCrosshair(Minecraft minecraft) {
        if (shouldRepeatUse(minecraft)) {
            ((MinecraftInvoker) minecraft).salts_inventory_update$startUseItem();
        }
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
        Vec3 origin = camera.getPosition();
        Vec3 direction = cursorDirection(minecraft, camera, mouseX, mouseY);
        double blockRange = 4.5D;
        double entityRange = 3.0D;
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
            EntitySelector.CAN_BE_COLLIDED_WITH,
            closestDistance
        );

        if (entityHit != null && entityHit.getLocation().closerThan(origin, entityRange)) {
            return entityHit;
        }

        return blockHit;
    }

    private static boolean shouldRepeatUse(Minecraft minecraft) {
        if (minecraft.player == null || minecraft.level == null || minecraft.gameMode == null) {
            return false;
        }
        if (minecraft.player.isUsingItem() || minecraft.gameMode.isDestroying()) {
            return false;
        }
        if (((MinecraftInvoker) minecraft).salts_inventory_update$getRightClickDelay() > 0) {
            return false;
        }
        if (minecraft.hitResult instanceof EntityHitResult) {
            return false;
        }
        if (minecraft.hitResult instanceof BlockHitResult blockHit && blockHit.getType() == HitResult.Type.BLOCK) {
            MenuProvider provider = minecraft.level.getBlockState(blockHit.getBlockPos()).getMenuProvider(minecraft.level, blockHit.getBlockPos());
            return provider == null;
        }

        return true;
    }

    private static boolean startAttack(Minecraft minecraft) {
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null || minecraft.gameMode == null || minecraft.hitResult == null) {
            return false;
        }
        if (player.isHandsBusy()) {
            return false;
        }

        if (player.isSpectator()) {
            return true;
        }

        ItemStack stack = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (!stack.isItemEnabled(minecraft.level.enabledFeatures())) {
            return false;
        }

        if (minecraft.hitResult instanceof EntityHitResult entityHit) {
            minecraft.gameMode.attack(player, entityHit.getEntity());
            player.swing(InteractionHand.MAIN_HAND);
            return true;
        }

        if (minecraft.hitResult instanceof BlockHitResult blockHit && blockHit.getType() == HitResult.Type.BLOCK) {
            BlockPos pos = blockHit.getBlockPos();
            BlockState state = minecraft.level.getBlockState(pos);
            if (!state.isAir()) {
                minecraft.gameMode.startDestroyBlock(pos, blockHit.getDirection());
                player.swing(InteractionHand.MAIN_HAND);
                return true;
            }
        }

        return false;
    }

    private static void continueAttack(Minecraft minecraft) {
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null || minecraft.gameMode == null) {
            return;
        }
        if (player.isUsingItem()) {
            return;
        }

        ItemStack stack = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (minecraft.hitResult instanceof BlockHitResult blockHit && blockHit.getType() == HitResult.Type.BLOCK) {
            BlockPos pos = blockHit.getBlockPos();
            BlockState state = minecraft.level.getBlockState(pos);
            if (!state.isAir()) {
                if (minecraft.gameMode.continueDestroyBlock(pos, blockHit.getDirection())) {
                    minecraft.level.addDestroyBlockEffect(pos, state);
                    player.swing(InteractionHand.MAIN_HAND);
                }
                return;
            }
        }

        minecraft.gameMode.stopDestroyBlock();
    }

    private static Vec3 cursorDirection(Minecraft minecraft, Camera camera, double mouseX, double mouseY) {
        int width = minecraft.getWindow().getGuiScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();
        float x = width <= 0 ? 0.0F : (float) (mouseX / width * 2.0D - 1.0D);
        float y = height <= 0 ? 0.0F : (float) (1.0D - mouseY / height * 2.0D);
        return camera.getNearPlane().getPointOnPlane(x, y).normalize();
    }
}
