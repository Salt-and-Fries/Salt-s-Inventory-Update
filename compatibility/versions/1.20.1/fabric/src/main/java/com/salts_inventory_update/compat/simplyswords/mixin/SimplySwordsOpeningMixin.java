package com.salts_inventory_update.compat.simplyswords.mixin;

import com.salts_inventory_update.compat.simplyswords.SimplySwordsIntegration;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets="net.sweenus.simplyswords.block.RunicForgeBlock",remap=false)
abstract class SimplySwordsOpeningMixin {
    @Inject(method={"use","method_9534","m_6227_"},at=@At("HEAD"),cancellable=true,remap=false)
    private void salts$open(BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit,CallbackInfoReturnable<InteractionResult> ci){
        if(player instanceof ServerPlayer server&&SimplySwordsIntegration.open(server,pos))ci.setReturnValue(InteractionResult.CONSUME);
    }
}
